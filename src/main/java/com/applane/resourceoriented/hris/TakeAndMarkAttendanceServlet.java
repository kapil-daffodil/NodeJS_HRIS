package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneAsyncTaskManager;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.scheduler.NightSchedulerHRIS;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class TakeAndMarkAttendanceServlet implements ApplaneTask {
	/**
	 * 
	 */
	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		JSONObject taskInfo = applaneTaskInfo.getTaskInfo();
		String temp = taskInfo.optString("temp");
		try {
			if (temp != null && temp.endsWith(HRISApplicationConstants.TEMP_VALUE_TO_MARK_ABSENT)) {

				Object organizationCounter = taskInfo.opt(NightSchedulerHRIS.ORGANIZATION_COUNTER);
				String[] organizations = HRISApplicationConstants.ORGANIZATIONS;
				int counter = Integer.valueOf("" + organizationCounter);
				String orgName = organizations[counter];
				String owner = "kapil.dalal@daffodilsw.com";
				Object userId = 13652;
				PunchingUtility.setValueInSession(orgName, userId, owner);

				String previousDate = taskInfo.optString("previousdate");
				String key = taskInfo.optString(Updates.KEY);
				markAbsentForYesterDayAttendanceIfItIsUnknown(previousDate, key, taskInfo);
			}
		} catch (Exception e) {
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "NightSchedulerHRIS Failed While Marking Absent");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	/**
	 * Mark Attendance as Absent for yesterday if it is Unknown
	 * 
	 * @throws JSONException
	 * */

	public void markAbsentForYesterDayAttendanceIfItIsUnknown(String previousDate, String key, JSONObject taskInfo) throws JSONException {
		JSONArray employeeArrayFromDailyAttendance = new JSONArray();
		Object returnKey = null;
		try {
			employeeArrayFromDailyAttendance = getEmployeeArrayFromDailyAttendance(previousDate, key);
			returnKey = updateAttendanceAbsentFromUnknown(employeeArrayFromDailyAttendance);
			ApplaneAsyncTaskManager.initiateTasksSynchronously();
		} catch (Exception e) {
			sendMailInCaseOfException(e, "Exception while calling markAbsentForYesterDayAttendanceIfItIsUnknown.");
		}
		if (employeeArrayFromDailyAttendance.length() > 0 && returnKey != null) {
			initiateTaskQueueToMarkAbsent(previousDate, key, taskInfo);
		} else {
			NightSchedulerHRIS.initiateTaskQueue("" + taskInfo.opt(NightSchedulerHRIS.ORGANIZATION_COUNTER), NightSchedulerHRIS.POPULATE_ATTENDANCE, null, true, null, null);
		}
	}

	private void initiateTaskQueueToMarkAbsent(String previousDate, String key, JSONObject taskInfo) throws JSONException {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("previousdate", "" + previousDate);
		taskQueueParameters.put(Updates.KEY, "" + key);
		taskQueueParameters.put("temp", "" + HRISApplicationConstants.TEMP_VALUE_TO_MARK_ABSENT);
		taskQueueParameters.put(NightSchedulerHRIS.ORGANIZATION_COUNTER, taskInfo.opt(NightSchedulerHRIS.ORGANIZATION_COUNTER));
		taskQueueParameters.put(NightSchedulerHRIS.PURPOSE, taskInfo.opt(NightSchedulerHRIS.PURPOSE));

		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.TakeAndMarkAttendanceServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);

	}

	private Object updateAttendanceAbsentFromUnknown(JSONArray employeeArrayFromDailyAttendance) throws Exception {
		Object returnKey = null;
		for (int counter = 0; counter < employeeArrayFromDailyAttendance.length(); counter++) {
			Object key = employeeArrayFromDailyAttendance.getJSONObject(counter).opt("__key__");
			returnKey = key;
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "employeeattendance");
			JSONObject row = new JSONObject();
			row.put("__key__", key);
			row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_ABSENT);
			updates.put(Data.Update.UPDATES, row);
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
		}
		return returnKey;
	}

	private JSONArray getEmployeeArrayFromDailyAttendance(String previousDate, String key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		String extraFilter = "";
		if (key != null && key.length() > 0) {
			extraFilter = " and " + Updates.KEY + " > " + key;
		}
		query.put(Data.Query.FILTERS, "attendancedate = '" + previousDate + "' and attendancetypeid = " + HRISApplicationConstants.ATTENDANCE_UNKNOWN + " and employeeid.subbranchid != " + HRISApplicationConstants.BRANCH_CLINT_SIDE + extraFilter);
		query.put(Data.Query.MAX_ROWS, 15);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray branchArray = object.getJSONArray("employeeattendance");
		return branchArray;
	}

	public static boolean isExists(Object branchId, Date currentDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "branchid = " + branchId + " and attendancedate = '" + currentDate + "'");
		JSONObject attendanceObject;
		attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = attendanceObject.getJSONArray("takeattendance");
		if (rows != null && rows.length() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public static void insertTakeAttendnace(Object branchId, String currentDate) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "takeattendance");
		JSONObject row = new JSONObject();
		row.put("branchid", branchId);
		row.put("attendancedate", currentDate);
		row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_UNKNOWN);
		row.put("temp", "temp");
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static Object getBranche(Object branchKey) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_branches");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		if (branchKey != null) {
			query.put(Data.Query.FILTERS, "__key__ > " + branchKey);
		}
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray branchArray = object.getJSONArray("hris_branches");
		// sendMailDateAndBranchIdsIsExist(branchArray, "getBranche method");
		return branchArray.length() > 0 ? branchArray.getJSONObject(0).get(Data.Query.KEY) : null;
	}

	public static void sendMailInCaseOfException(Exception e, String title) {
		String traces = ExceptionUtils.getExceptionTraceMessage("Morning Schedular For Attendance Failed Exception >> ", e);
		LogUtility.writeError(traces);
		ApplaneMail mail = new ApplaneMail();
		StringBuilder builder = new StringBuilder();
		builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
		builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(traces);
		mail.setMessage(title, builder.toString(), true);
		try {
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception while calling markAbsentForYesterDayAttendanceIfItIsUnknown.");
		}
	}

	public static String getLeavePolicyId(String leavePolicyId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leavepolicy");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		if (leavePolicyId != null) {
			query.put(Data.Query.FILTERS, "__key__ > " + leavePolicyId);
		}
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray leavePolicyArray = object.getJSONArray("hris_leavepolicy");
		// sendMailDateAndBranchIdsIsExist(leavePolicyArray, "getLeavePolicyId method call");
		return leavePolicyArray.length() > 0 ? Translator.stringValue(leavePolicyArray.getJSONObject(0).get(Data.Query.KEY)) : null;
	}

	public static Object getLeaveRule(Object leavetypeIds, String leavePolicyIdParaeter) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONArray columnArray = new JSONArray();
		columnArray.put("leavetypeid");
		String filters = "";
		if (leavetypeIds != null) {
			filters = " and leavetypeid not in (" + leavetypeIds + ")";
		}
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyIdParaeter + filters);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray leaveRuleArray = object.getJSONArray("hris_leaverule");
		// sendMailDateAndBranchIdsIsExist(leaveRuleArray, "getLeaveRule method call");
		return leaveRuleArray.length() > 0 ? leaveRuleArray.getJSONObject(0).get("leavetypeid") : null;
	}

	public static void invokeMethodForTesting() {
		carryForwardLeaves("" + 7, 1);
	}

	public static void carryForwardLeaves(String leavePolicyIdParameter, Object leaveTypeId) {
		try {
			JSONArray employeeArray = getActiveEmployeeArrayAccToLeavePolicyId(leavePolicyIdParameter);
			if (employeeArray != null && employeeArray.length() > 0) {
				Calendar lapsCalendar = Calendar.getInstance();
				Calendar todayCalendar = Calendar.getInstance();
				JSONArray leaveRulesArray = getLeaveRulesArray(leavePolicyIdParameter, leaveTypeId);
				if (leaveRulesArray != null && leaveRulesArray.length() > 0) {
					Object maxAllowedObject = leaveRulesArray.getJSONObject(0).opt("maxallowed");
					Date lapsDate = Translator.dateValue(leaveRulesArray.getJSONObject(0).opt("lapsdate"));
					if (lapsDate != null && maxAllowedObject != null) {
						boolean isCarryForward = Translator.booleanValue(leaveRulesArray.getJSONObject(0).opt("carryforward"));
						String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
						Date todayDate = Translator.dateValue(TODAY_DATE);
						lapsCalendar.setTime(lapsDate);
						todayCalendar.setTime(todayDate);
						int lapsDayOfMonth = lapsCalendar.get(Calendar.DAY_OF_MONTH);
						int lapsMonth = lapsCalendar.get(Calendar.MONTH);
						int todayDayOfMonth = todayCalendar.get(Calendar.DAY_OF_MONTH);
						int todayMonth = todayCalendar.get(Calendar.MONTH);
						long todayYear = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(todayCalendar.get(Calendar.YEAR)));
						if (lapsDayOfMonth == todayDayOfMonth && lapsMonth == todayMonth) {
							for (int counter = 0; counter < employeeArray.length(); counter++) {
								Object employeeId = employeeArray.getJSONObject(counter).opt("__key__");
								JSONArray leaveRegisterArray = getLeaveRegisterArray(employeeId, leaveTypeId);
								if (leaveRegisterArray != null && leaveRegisterArray.length() > 0) {
									double leaveBalance = Translator.doubleValue(leaveRegisterArray.getJSONObject(0).opt("leavebalance"));
									Object leaveTypeIdFronLeaveRegister = leaveRegisterArray.getJSONObject(0).opt("leavetypeid");
									Object leaveRegisterKey = leaveRegisterArray.getJSONObject(0).opt("__key__");
									if (leaveBalance > 0.0) {
										if (isCarryForward) {
											double maxAllowed = Translator.doubleValue(maxAllowedObject);
											if (maxAllowed < leaveBalance) {
												leaveBalance = leaveBalance - maxAllowed;
												insertRecordIntoLeavesAfterLaps(employeeId, leaveBalance, (todayMonth + 1), todayYear, leaveTypeIdFronLeaveRegister);
												leaveBalance = maxAllowed;
												updateLeaveRegister(leaveRegisterKey, leaveBalance);
											}
										} else {
											insertRecordIntoLeavesAfterLaps(employeeId, leaveBalance, (todayMonth + 1), todayYear, leaveTypeIdFronLeaveRegister);
											leaveBalance = 0.0;
											updateLeaveRegister(leaveRegisterKey, leaveBalance);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "carryForwardLeaves(method in TakeAndMarkAttendance) CARRY_FORWARD_LEAVES_ON_LAPS_DATE Failed");
		}

	}

	private static void updateLeaveRegister(Object leaveRegisterKey, double leaveBalance) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "leaveregister");
		JSONObject row = new JSONObject();
		row.put("__key__", leaveRegisterKey);
		row.put("leavebalance", leaveBalance);
		row.put("closingbalance", leaveBalance);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private static void insertRecordIntoLeavesAfterLaps(Object employeeId, double leaveBalance, int monthId, long yearId, Object leaveTypeIdFronLeaveRegister) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "leavesafterlaps");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("monthid", monthId);
		row.put("yearid", yearId);
		row.put("leavetypeid", leaveTypeIdFronLeaveRegister);
		row.put("numberofleaves", leaveBalance);
		row.put("paidstatusid", HRISApplicationConstants.LEAVE_EWD_UN_PAID);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private static JSONArray getLeaveRulesArray(String leavePolicyIdParameter, Object leaveTypeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("carryforward");
		columnArray.put("maxallowed");
		columnArray.put("lapsdate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyIdParameter + " and leavetypeid = " + leaveTypeId);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_leaverule");
		return employeeArray;
	}

	private static JSONArray getLeaveRegisterArray(Object employeeId, Object leaveTypeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "leaveregister");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("leavebalance");
		columnArray.put("leavetypeid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and leavetypeid = " + leaveTypeId);

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "__key__");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		query.put(Data.Query.ORDERS, new JSONArray().put(order));
		query.put(Data.Query.MAX_ROWS, 1);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("leaveregister");
		return employeeArray;
	}

	private static JSONArray getActiveEmployeeArrayAccToLeavePolicyId(String leavePolicyIdParameter) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyIdParameter + " and employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees");
		return employeeArray;
	}

}
