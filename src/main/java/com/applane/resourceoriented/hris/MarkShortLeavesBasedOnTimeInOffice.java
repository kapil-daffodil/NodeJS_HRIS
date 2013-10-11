package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneAsyncTaskManager;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.reports.EmployeeDailyShortAttendanceReportBusinessLogic;
import com.applane.resourceoriented.hris.reports.EmployeeMonthlyShortAttendanceReportBusinessLogic;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.google.apphosting.api.DeadlineExceededException;

public class MarkShortLeavesBasedOnTimeInOffice implements ApplaneTask {
	private static final String LIST = "list";
	private static final String COUNTER = "counter";
	private static final String EMPLOYEE_LIST_WITH_SHORT_ATTENDANCE = "employeeListWithShortAttendance";

	public void markShortAttendance() {
		markShortAttendanceFromTaskQueue(-1);
	}

	public void markShortAttendanceFromTaskQueue(int organizationCounter) {
		try {
			EmployeeMonthlyShortAttendanceReportBusinessLogic emsabl = new EmployeeMonthlyShortAttendanceReportBusinessLogic();
			JSONArray schedules = emsabl.getSchedules();
			HashMap<Integer, String> scheduleHoursMAP = new HashMap<Integer, String>();
			emsabl.getScheduleHoursMAP(schedules, scheduleHoursMAP);
			EmployeeDailyShortAttendanceReportBusinessLogic edsabl = new EmployeeDailyShortAttendanceReportBusinessLogic();

			HashMap<Integer, JSONObject> employeeListWithShortAttendance = new HashMap<Integer, JSONObject>();
			// String todayDateString = "2012-09-25";
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			edsabl.sendReport(employeeListWithShortAttendance, scheduleHoursMAP, TODAY_DATE);
			List<Integer> list = new ArrayList<Integer>();
			for (Integer employeeId : employeeListWithShortAttendance.keySet()) {
				list.add(employeeId);
			}
			if (list.size() > 0) {
				initiateTaskQueue(employeeListWithShortAttendance, list, 0, organizationCounter);
			} else if (organizationCounter != -1) {
				new MorningSchdularHRIS().initiateTaskQueue(organizationCounter, MorningSchdularHRIS.NEXT_ORGANIZATION);
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("MarkShortLeavesBasedOnTimeInOffice(markShortAttendance) Trace Exception >> " + trace);
			throw new RuntimeException(trace);
		}
	}

	private void initiateTaskQueue(HashMap<Integer, JSONObject> employeeListWithShortAttendance, List<Integer> list, int counter, int organizationCounter) throws JSONException {
		JSONObject taskQueueInfo = new JSONObject();
		taskQueueInfo.put(LIST, new JSONArray(list));
		taskQueueInfo.put(EMPLOYEE_LIST_WITH_SHORT_ATTENDANCE, new JSONObject(employeeListWithShortAttendance));
		taskQueueInfo.put(COUNTER, counter);
		taskQueueInfo.put(MorningSchdularHRIS.ORGANIZATION_COUNTER, organizationCounter);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.MarkShortLeavesBasedOnTimeInOffice", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueInfo);
	}

	public void initiate(ApplaneTaskInfo applaneTaskQueueInfo) throws DeadlineExceededException {
		JSONObject taskQueueInfo = applaneTaskQueueInfo.getTaskInfo();
		if (taskQueueInfo != null) {
			JSONObject employeeListWithShortAttendance = (JSONObject) taskQueueInfo.opt(EMPLOYEE_LIST_WITH_SHORT_ATTENDANCE);
			int counter = taskQueueInfo.optInt(COUNTER);
			int organizationCounter = taskQueueInfo.optInt(MorningSchdularHRIS.ORGANIZATION_COUNTER);
			JSONArray list = (JSONArray) taskQueueInfo.opt(LIST);
			try {
				if (counter < list.length()) {
					int employeeId = list.optInt(counter);
					JSONObject details = (JSONObject) employeeListWithShortAttendance.opt("" + employeeId);
					if (details != null) {
						int attendanceId = Translator.integerValue(details.opt("key"));
						int leavePolicyId = Translator.integerValue(details.opt("leavePolicyId"));
						String attendanceDateInString = Translator.stringValue(details.opt("attendanceDate"));
						Date attendanceDate = Translator.dateValue(attendanceDateInString);
						Calendar cal = Calendar.getInstance();
						cal.setTime(attendanceDate);

						Date monthFirstDate = DataTypeUtilities.getMonthFirstDate("" + cal.get(Calendar.YEAR), EmployeeSalaryGenerationServlet.getMonthName(cal.get(Calendar.MONTH) + 1));
						Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
						String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
						String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
						JSONArray leaveRule = new EmployeeMonthlyAttendance().getLeaveRuleRecord(leavePolicyId, monthFirstDateInString, monthLastDateInString);
						if (leaveRule != null && leaveRule.length() > 0) {
							int leaveTypeId = Translator.integerValue(leaveRule.getJSONObject(0).opt("leavetypeid"));
							new ShortLeaveBusinessLogic().updateAttendance(attendanceId, leaveTypeId, employeeId, attendanceDateInString);
							ApplaneAsyncTaskManager.initiateTasksSynchronously();
						}
					}

					counter++;
				}
				if (counter < list.length()) {
					taskQueueInfo.put(COUNTER, counter);
					ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.MarkShortLeavesBasedOnTimeInOffice", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueInfo);
				} else if (organizationCounter != -1) {
					new MorningSchdularHRIS().initiateTaskQueue(organizationCounter, MorningSchdularHRIS.NEXT_ORGANIZATION);
				}
			} catch (Exception e) {
				if (counter < list.length()) {
					try {
						taskQueueInfo.put(COUNTER, counter);
					} catch (JSONException e1) {
						String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
						LogUtility.writeLog("MarkShortLeavesBasedOnTimeInOffice(Initiate 1) Trace Exception >> " + trace);
						throw new RuntimeException(trace);
					}
					ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.MarkShortLeavesBasedOnTimeInOffice", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueInfo);
				} else if (organizationCounter != -1) {
					try {
						new MorningSchdularHRIS().initiateTaskQueue(organizationCounter, MorningSchdularHRIS.NEXT_ORGANIZATION);
					} catch (JSONException e1) {
						throw new RuntimeException(e1);
					}
				}
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				throw new RuntimeException(trace);
			}
		}
	}
}
