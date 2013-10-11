package com.applane.resourceoriented.hris.reports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeMonthlyAttendance;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.EmployeePerformanceKinds;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeSalarySheetExceptionReportOnMail implements ApplaneTask {

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		JSONObject taskQueueInfo = applaneTaskInfo.getTaskInfo();
		try {
			// HashMap<Integer, Object[]> salaryComponentsMap = (HashMap<Integer, Object[]>) taskQueueInfo.opt("salaryComponentsMap");
			Object pfEmployeeComponentId = taskQueueInfo.opt("pfEmployeeComponentId");
			Object pfEmployerComponentId = taskQueueInfo.opt("pfEmployerComponentId");
			JSONObject ignoreableAndCompulsoryComponentKeys = (JSONObject) taskQueueInfo.opt("ignoreableAndCompulsoryComponentKeys");
			String dateForReport = Translator.stringValue(taskQueueInfo.opt("dateForReport"));
			JSONObject fields = getExtraUsefulDetails(dateForReport);
			JSONArray profitCenters = (JSONArray) taskQueueInfo.opt("profitCenters");
			int counter = (Integer) taskQueueInfo.opt("counter");
			int profitCenterId = Translator.integerValue(profitCenters.getJSONObject(counter).opt(Updates.KEY));
			int organizationCounter = Translator.integerValue(taskQueueInfo.opt(MorningSchdularHRIS.ORGANIZATION_COUNTER));
			Object mailIdArray = profitCenters.getJSONObject(counter).opt("emails");
			String[] emailIds = null;
			if (mailIdArray != null && mailIdArray instanceof JSONArray) {
				emailIds = new String[((JSONArray) mailIdArray).length()];
				for (int counterEmail = 0; counterEmail < ((JSONArray) mailIdArray).length(); counterEmail++) {
					emailIds[counterEmail] = Translator.stringValue(((JSONArray) mailIdArray).getJSONObject(counterEmail).opt("officialemailid"));
				}
			}

			sendExceptionMail(pfEmployeeComponentId, pfEmployerComponentId, ignoreableAndCompulsoryComponentKeys, fields, profitCenterId, emailIds);
			counter++;
			if (counter <= (profitCenters.length() - 1)) {
				initiateTaskQueue(pfEmployeeComponentId, pfEmployerComponentId, ignoreableAndCompulsoryComponentKeys, fields, profitCenters, counter, dateForReport, organizationCounter);
			} else if (organizationCounter != -1) {
				new MorningSchdularHRIS().initiateTaskQueue(organizationCounter, MorningSchdularHRIS.SEND_ATTENDANCE_SUMMARY);
			}

		} catch (Exception e) {
			String exceptionMessage = ExceptionUtils.getExceptionTraceMessage(EmployeeSalarySheetExceptionReportOnMail.class.getName(), e);
			LogUtility.writeLog("putIntoSheet Exception  >> : [" + exceptionMessage + "]");
			throw new RuntimeException(exceptionMessage);
		}
	}

	public static void sendExceptionReportMail(Object date) {
		sendExceptionReportMailByScheduler(date, -1);
	}

	public static void sendExceptionReportMailByScheduler(Object date, int organizationCounter) {
		try {
			String dateForReport = "";
			if (date != null) {
				dateForReport = Translator.stringValue(date);
			}
			JSONArray salaryComponents = new EmployeeSalarySheetReport().getSalaryComponents();
			HashMap<Integer, Object[]> salaryComponentsMap = new HashMap<Integer, Object[]>();

			Object[] ids = putSalaryCpmponentMap(salaryComponents, salaryComponentsMap);
			if (salaryComponentsMap.size() > 0) {
				JSONObject ignoreableAndCompulsoryComponentKeys = getIgnoreAbleComponentKeys(salaryComponentsMap);
				JSONObject fields = getExtraUsefulDetails(dateForReport);

				JSONArray profitCenters = EmployeeSalarySheetReport.getMailIdProfitCenterWise("");
				// JSONArray branches = EmployeeSalarySheetReport.getHrMailIdDetail("branchid = 1");
				int counter = 0;
				initiateTaskQueue(ids[0], ids[1], ignoreableAndCompulsoryComponentKeys, fields, profitCenters, counter, dateForReport, organizationCounter);
			}
		} catch (Exception e) {
			String exceptionMessage = ExceptionUtils.getExceptionTraceMessage(EmployeeSalarySheetExceptionReportOnMail.class.getName(), e);
			LogUtility.writeLog("putIntoSheet Exception  >> : [" + exceptionMessage + "]");
			throw new RuntimeException(exceptionMessage);
		}
	}

	private static void sendExceptionMail(Object pfEmployeeComponentId, Object pfEmployerComponentId, JSONObject ignoreableAndCompulsoryComponentKeys, JSONObject fields, int profitCenterId, String[] emailIds) throws JSONException, Exception {
		JSONArray employeeArray = getEmployeeArray(profitCenterId, fields);
		String[] employeeSalaryComponentIssueForMail = getEmployeeSalaryComponentIssueForMail(employeeArray, ignoreableAndCompulsoryComponentKeys, fields, pfEmployeeComponentId, pfEmployerComponentId);
		if (employeeSalaryComponentIssueForMail[1].length() > 0) {
			String header = employeeSalaryComponentIssueForMail[0];
			String body = employeeSalaryComponentIssueForMail[1];
			String footer = employeeSalaryComponentIssueForMail[2];
			String mailValue = header + body + footer;
			String title = "Exception Report for " + fields.opt("currentMonthName") + " - " + fields.opt("currentYearName");
			String[] toEmailIds = new String[1];
			toEmailIds[0] = ApplicationConstants.USER;
			if (emailIds != null && emailIds.length > 0) {
				PunchingUtility.sendMailsAccToProfitCenters(toEmailIds, mailValue, title, emailIds);
				// PunchingUtility.sendMailsWithoutCC(new String[] { ApplicationConstants.OWNER }, mailValue, title);
			}
		}
	}

	private static void initiateTaskQueue(Object pfEmployeeComponentId, Object pfEmployerComponentId, JSONObject ignoreableAndCompulsoryComponentKeys, JSONObject fields, JSONArray profitCenters, int counter, String dateForReport, int organizationCounter) throws JSONException {
		JSONObject taskQueueInfo = new JSONObject();
		// taskQueueInfo.put("salaryComponentsMap", salaryComponentsMap);
		taskQueueInfo.put("pfEmployeeComponentId", pfEmployeeComponentId);
		taskQueueInfo.put("pfEmployerComponentId", pfEmployerComponentId);
		taskQueueInfo.put("ignoreableAndCompulsoryComponentKeys", ignoreableAndCompulsoryComponentKeys);
		taskQueueInfo.put("fields", fields);
		taskQueueInfo.put("profitCenters", profitCenters);
		taskQueueInfo.put("counter", counter);
		taskQueueInfo.put("dateForReport", dateForReport);
		taskQueueInfo.put(MorningSchdularHRIS.ORGANIZATION_COUNTER, organizationCounter);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.reports.EmployeeSalarySheetExceptionReportOnMail", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueInfo);
	}

	private static String[] getEmployeeSalaryComponentIssueForMail(JSONArray employeeArray, JSONObject ignoreableAndCompulsoryComponentKeys, JSONObject fields, Object pfEmployeeComponentId, Object pfEmployerComponentId) throws Exception {
		String headerForMail = "<table border='1' cellspacing='0'>";
		String employeeSalaryComponentIssueForMail = "";
		String footerForMail = "</table>";

		String todayDateInString = fields.optString("todayDateInString");
		Date todayDate = (Date) fields.opt("todayDate");

		Date sendingDate = (Date) fields.opt("sendingDate");

		Calendar cal = Calendar.getInstance();
		cal.setTime(todayDate);

		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(sendingDate);
		int sendingMonthId = cal1.get(Calendar.MONTH) + 1;

		// LogUtility.writeError("fields >> " + fields + " << sendingMonthId >> " + sendingMonthId);
		// fields = null;

		Date currentMonthFirstDate = (Date) fields.opt("currentMonthFirstDate");
		Date currentMonthLastDate = (Date) fields.opt("currentMonthLastDate");

		long numberOfDaysInMonth = DataTypeUtilities.differenceBetweenDates(currentMonthFirstDate, currentMonthLastDate) + 1;

		Date currentMonthFirstDateTemp = (Date) fields.opt("currentMonthFirstDate");
		Date currentMonthLastDateTemp = (Date) fields.opt("currentMonthLastDate");
		// Date previousMonthFirstDate = (Date)fields.opt("previousMonthFirstDate");
		// Date previousMonthLastDate = (Date)fields.opt("previousMonthLastDate");

		String currentMonthFirstDateInString = fields.optString("currentMonthFirstDateInString");
		String currentMonthLastDateInString = fields.optString("currentMonthLastDateInString");

		String currentMonthFirstDateInStringTemp = fields.optString("currentMonthFirstDateInString");
		String currentMonthLastDateInStringTemp = fields.optString("currentMonthLastDateInString");

		String previousMonthFirstDateInString = fields.optString("previousMonthFirstDateInString");
		String previousMonthLastDateInString = fields.optString("previousMonthLastDateInString");

		JSONArray ignoreableComponentList = (JSONArray) ignoreableAndCompulsoryComponentKeys.opt("ignoreableComponentList");
		JSONArray compulsoryComponentListJSONArray = (JSONArray) ignoreableAndCompulsoryComponentKeys.opt("compulsoryComponentList");
		ArrayList<Integer> compulsoryComponentList = new ArrayList<Integer>();

		// LogUtility.writeLog("pfEmployeeComponentId >> " + pfEmployeeComponentId + " << pfEmployerComponentId >> " + pfEmployerComponentId);
		for (int counter = 0; counter < compulsoryComponentListJSONArray.length(); counter++) {
			compulsoryComponentList.add(compulsoryComponentListJSONArray.getInt(counter));
		}
		for (int counter = 0; counter < employeeArray.length(); counter++) {
			Object employeeId = employeeArray.getJSONObject(counter).opt(Updates.KEY);
			Object employeeName = employeeArray.getJSONObject(counter).opt("name");

			double ctc = DataTypeUtilities.doubleValue(employeeArray.getJSONObject(counter).opt("ctc"));
			Object panno = employeeArray.getJSONObject(counter).opt("panno");
			Object accountno = employeeArray.getJSONObject(counter).opt("accountno");
			Object providentfundno = employeeArray.getJSONObject(counter).opt("providentfundno");
			Object joiningDateObject = employeeArray.getJSONObject(counter).opt("joiningdate");
			Object relievingDateObject = employeeArray.getJSONObject(counter).opt("relievingdate");

			Object scheduleId = employeeArray.getJSONObject(counter).opt("employeescheduleid");
			Object holidayCalendarId = employeeArray.getJSONObject(counter).opt("holidaycalendarid");
			Object deviceId = employeeArray.getJSONObject(counter).opt("deviceid");
			Date joiningDate = null;
			Date relievingDate = null;
			currentMonthFirstDateInString = currentMonthFirstDateInStringTemp;
			currentMonthLastDateInString = currentMonthLastDateInStringTemp;
			currentMonthFirstDate = currentMonthFirstDateTemp;
			currentMonthLastDate = currentMonthLastDateTemp;
			if (joiningDateObject != null) {
				joiningDate = Translator.dateValue(joiningDateObject);
				if (currentMonthFirstDate.before(joiningDate)) {
					currentMonthFirstDate = joiningDate;
					currentMonthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(joiningDate);
				}
			}
			if (relievingDateObject != null) {
				relievingDate = Translator.dateValue(relievingDateObject);
				if (currentMonthLastDate.after(relievingDate)) {
					currentMonthLastDate = relievingDate;
					currentMonthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(relievingDate);
				}
			}
			StringBuilder employeeNameForMail = new StringBuilder();
			StringBuilder mailValue = new StringBuilder();
			employeeNameForMail.append("<tr  bgcolor='99CCCC'>").append("<th align='left'>").append(employeeName).append("</th>").append("</tr>");
			employeeNameForMail.append("<tr>").append("<td>").append("<ol>");
			// if (Translator.integerValue(employeeId) == 5 || Translator.integerValue(employeeId) == 6)
			// LogUtility.writeLog("employeeId >> " + employeeId + " << currentMonthLastDateInString >> " + currentMonthLastDateInString + " << currentMonthFirstDateInString >> " + currentMonthFirstDateInString);
			JSONArray currentEmployeeSalaryComponentsArray = getEmployeeSalaryComponents(currentMonthFirstDateInString, currentMonthLastDateInString, employeeId);
			JSONArray previousEmployeeSalaryComponentsArray = getEmployeeSalaryComponents(previousMonthFirstDateInString, previousMonthLastDateInString, employeeId);
			if (currentEmployeeSalaryComponentsArray == null || currentEmployeeSalaryComponentsArray.length() == 0) {
				mailValue.append("<li>").append("Salary Component Not Defined in Date Range ").append(currentMonthFirstDateInString).append(" to ").append(currentMonthLastDateInString).append("</li>");
			}
			if (ctc > 25000 && panno == null) {
				mailValue.append("<li>").append("CTC is greater then 25000 but PAN number not defined").append("</li>");
			}

			if (accountno == null) {
				mailValue.append("<li>").append("Account number not defined").append("</li>");
			}
			if (deviceId == null) {
				mailValue.append("<li>").append("Punch Card number not defined").append("</li>");
			}

			HashMap<Integer, Object[]> currentComponentMap = new HashMap<Integer, Object[]>();
			boolean hasPFComponent = false;
			boolean hasComponents = false;
			for (int currentComponentCounter = 0; currentComponentCounter < currentEmployeeSalaryComponentsArray.length(); currentComponentCounter++) {
				Integer currentComponentKey = DataTypeUtilities.integerValue(currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("salarycomponentid"));
				String currentComponentName = DataTypeUtilities.stringValue(currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("salarycomponentid.name"));
				Object applicableFromDateObject = currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("applicablefrom");
				Object applicableToDateObject = currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("applicableto");
				Object amount = currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("amount");
				Integer criteriaId = DataTypeUtilities.integerValue(currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("salarycomponentid." + SalaryComponentKinds.CRITERIA_ID));
				Integer performanceTypeId = DataTypeUtilities.integerValue(currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("salarycomponentid." + SalaryComponentKinds.PERFORMANCE_TYPE_ID));
				Object kpiId = currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("salarycomponentid." + SalaryComponentKinds.KPI_ID);
				int paidAfterMonth = (int) Translator.doubleValue(currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("salarycomponentid." + SalaryComponentKinds.PAID_AFTER_MONTH));
				int duemonthId = Translator.integerValue(currentEmployeeSalaryComponentsArray.getJSONObject(currentComponentCounter).opt("duemonthid"));
				if (amount != null && Translator.doubleValue(amount) > 0.0) {
					if (sendingMonthId != duemonthId) {
						mailValue.append("<li>").append("Due Month Not updated for ").append(currentComponentName).append("</li>");
					}
					boolean notIgnoreable = false;
					if (!hasComponents) {
						for (int ignoreComponentCounter = 0; ignoreComponentCounter < ignoreableComponentList.length(); ignoreComponentCounter++) {
							if (ignoreableComponentList.optInt(ignoreComponentCounter) == currentComponentKey) {
								notIgnoreable = true;
							}
						}
					}
					if (!notIgnoreable) {
						hasComponents = true;
					}

					if (DataTypeUtilities.integerValue(pfEmployeeComponentId) == currentComponentKey) {
						hasPFComponent = true;
					}

					if (criteriaId == HRISApplicationConstants.PERFORMANCE_BASED && performanceTypeId == HRISApplicationConstants.TARGET_ACHIEVED && kpiId != null) {
						cal.setTime(currentMonthFirstDateTemp);
						if (paidAfterMonth > 0) {
							cal.add(Calendar.MONTH, (-1 * paidAfterMonth));
						}
						int monthId = cal.get(Calendar.MONTH) + 1;
						long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));

						JSONArray employeePerformance = getEmployeePerformance(employeeId, monthId, yearId, kpiId);
						if (employeePerformance != null && employeePerformance.length() > 0) {
							Object performanceAmount = employeePerformance.getJSONObject(0).opt(EmployeePerformanceKinds.AMOUNT);
							if (performanceAmount == null) {
								mailValue.append("<li>").append("Employee Performance amount not defined for ").append(currentComponentName).append(" for month ").append(EmployeeReleasingSalaryServlet.getMonthName(monthId)).append(" and year ").append(EmployeeReleasingSalaryServlet.getYearName(yearId)).append("</li>");
							}
						} else {
							mailValue.append("<li>").append("Employee Performance KPI not defined for ").append(currentComponentName).append(" for month ").append(EmployeeReleasingSalaryServlet.getMonthName(monthId)).append(" and year ").append(EmployeeReleasingSalaryServlet.getYearName(yearId)).append("</li>");
						}
					}

					if (applicableFromDateObject != null && applicableToDateObject != null && amount != null && Translator.doubleValue(amount) > 0.0) {
						Date applicableFromDate = DataTypeUtilities.checkDateFormat(applicableFromDateObject);
						Date applicableToDate = DataTypeUtilities.checkDateFormat(applicableToDateObject);
						Object[] dateDetails = new Object[3];
						dateDetails[0] = applicableFromDate;
						dateDetails[1] = applicableToDate;
						dateDetails[2] = currentComponentName;
						if (currentComponentMap.containsKey(currentComponentKey)) {
							dateDetails = currentComponentMap.get(currentComponentKey);
							String previousDate = DataTypeUtilities.getBackDate(EmployeeSalaryGenerationServlet.getDateInString(applicableFromDate));

							if (!((Date) dateDetails[1]).equals(Translator.dateValue(previousDate)) && ((Date) dateDetails[1]).before(Translator.dateValue(previousDate))) {
								mailValue.append("<li>").append(currentComponentName).append(" not defined from ").append(DataTypeUtilities.getNextDate(EmployeeSalaryGenerationServlet.getDateInString((Date) dateDetails[1]))).append(" To ").append(previousDate).append("</li>");
							}

							if (((Date) dateDetails[0]).after(applicableFromDate)) {
								dateDetails[0] = applicableFromDate;
							}

							if (((Date) dateDetails[1]).before(applicableToDate)) {
								dateDetails[1] = applicableToDate;
							}
						}
						currentComponentMap.put(currentComponentKey, dateDetails);
					}
				}
			}
			if (!hasComponents) {
				mailValue.append("<li>").append("Salary Component Not Defined in Date Range ").append(currentMonthFirstDateInString).append(" to ").append(currentMonthLastDateInString).append("</li>");
			}

			for (Integer currentComponentKey : currentComponentMap.keySet()) {
				Object[] dateDetails = currentComponentMap.get(currentComponentKey);
				Date applicableFromDate = (Date) dateDetails[0];
				Date applicableToDate = (Date) dateDetails[1];
				String componentName = (String) dateDetails[2];
				if (applicableFromDate.after(currentMonthFirstDate) || applicableToDate.before(currentMonthLastDate)) {
					mailValue.append("<li>").append(componentName).append(" Not Defined for Complete Month in Date Range ").append(currentMonthFirstDateInString).append(" to ").append(currentMonthLastDateInString).append("</li>");
				}
			}

			// LogUtility.writeLog("employeeId >> " + employeeId + " << hasPFComponent >> " + hasPFComponent + " << providentfundno >> " + providentfundno);
			if (hasPFComponent && providentfundno == null) {
				mailValue.append("<li>").append("PF Employee component assigned but PF number not defined").append("</li>");
			}
			if (!hasPFComponent && providentfundno != null) {
				mailValue.append("<li>").append("PF number defined but PF Employee component not assigned").append("</li>");
			}

			if (compulsoryComponentList.size() > 0) {
				for (int previousComponentCounter = 0; previousComponentCounter < previousEmployeeSalaryComponentsArray.length(); previousComponentCounter++) {
					Integer previousComponentKey = DataTypeUtilities.integerValue(previousEmployeeSalaryComponentsArray.getJSONObject(previousComponentCounter).opt("salarycomponentid"));
					double previousAmount = DataTypeUtilities.integerValue(previousEmployeeSalaryComponentsArray.getJSONObject(previousComponentCounter).opt("amount"));
					if (compulsoryComponentList.contains(previousComponentKey) && previousAmount > 0.0) {
						String previousComponentName = DataTypeUtilities.stringValue(previousEmployeeSalaryComponentsArray.getJSONObject(previousComponentCounter).opt("salarycomponentid.name"));
						boolean isContais = false;
						for (int couuentComponentCounter = 0; couuentComponentCounter < currentEmployeeSalaryComponentsArray.length(); couuentComponentCounter++) {
							Integer currentComponentKey = DataTypeUtilities.integerValue(currentEmployeeSalaryComponentsArray.getJSONObject(couuentComponentCounter).opt("salarycomponentid"));

							if (((int) currentComponentKey) == ((int) previousComponentKey)) {
								isContais = true;
							}
						}
						if (!isContais) {
							mailValue.append("<li>").append(previousComponentName).append(" Defined for Previous Month But Not for Current Month in Date Range ").append(currentMonthFirstDateInString).append(" to ").append(currentMonthLastDateInString).append("</li>");
						}
					}
				}
			}
			if (todayDate.before(currentMonthLastDate)) {
				currentMonthLastDateInString = todayDateInString;
				currentMonthLastDate = todayDate;
			}
			numberOfDaysInMonth = DataTypeUtilities.differenceBetweenDates(currentMonthFirstDate, currentMonthLastDate) + 1;
			String filter = "employeeid = " + employeeId + " and attendancedate >= '" + currentMonthFirstDateInString + "' and attendancedate <= '" + currentMonthLastDateInString + "'";// + "' and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_ABSENT + ", " + HRISApplicationConstants.ATTENDANCE_UNKNOWN + ", " + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY + ", " + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF + ")";
			JSONArray employeeAttendanceArray = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);
			double absentDays = 0.0;
			double ewdDays = 0.0;
			double unKnown = 0.0;
			// double salaryDays = 0.0;

			if (numberOfDaysInMonth != employeeAttendanceArray.length()) {
				mailValue.append("<li>").append("Daily Attendance should be for ").append(numberOfDaysInMonth).append(" days but found for  ").append(employeeAttendanceArray.length()).append(" days.</li>");
			}

			for (int innerCounter = 0; innerCounter < employeeAttendanceArray.length(); innerCounter++) {

				int attendanceStatusId = Translator.integerValue(employeeAttendanceArray.getJSONObject(innerCounter).opt("attendance_status"));
				int attendanceTypeId = Translator.integerValue(employeeAttendanceArray.getJSONObject(innerCounter).opt("attendancetypeid"));
				Object paidStatus = employeeAttendanceArray.getJSONObject(innerCounter).opt("paidstatus");
				Object attendanceDate = employeeAttendanceArray.getJSONObject(innerCounter).opt("attendancedate");
				if (attendanceStatusId == 1) {
					mailValue.append("<li>").append("Un-Approved Attendance found for date: ").append(attendanceDate).append(".</li>");
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HOLIDAY) {
					// salaryDays += 1;
				}

				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
					absentDays += 1.0;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
					// salaryDays += 1;
					if (paidStatus == null) {
						ewdDays += 1.0;
						// salaryDays += 1;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					// salaryDays += 1;
					if (paidStatus == null) {
						ewdDays += 0.50;
						// salaryDays += 0.50;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
					unKnown += 1.0;
				}
			}
			if (absentDays > 0.0) {
				mailValue.append("<li>").append(absentDays).append(" Absent Attendance Found From ").append(currentMonthFirstDateInString).append(" to ").append(currentMonthLastDateInString).append("</li>");
			}
			if (ewdDays > 0.0) {
				mailValue.append("<li>").append(ewdDays).append(" EWD Un-Approved Found From ").append(currentMonthFirstDateInString).append(" to ").append(currentMonthLastDateInString).append("</li>");
			}
			if (unKnown > 0.0) {
				mailValue.append("<li>").append(unKnown).append(" Un-Known Attendance Found From ").append(currentMonthFirstDateInString).append(" to ").append(currentMonthLastDateInString).append("</li>");
			}

			JSONArray unApprovesLeaveRequests = getUnApprovesLeaveRequests(employeeId, currentMonthFirstDateInString, currentMonthLastDateInString);
			StringBuilder unApprovedLEaves = new StringBuilder();
			int leaveCounter = 0;
			if (unApprovesLeaveRequests != null && unApprovesLeaveRequests.length() > 0) {
				unApprovedLEaves.append("<li>").append("Un Approve Leave Found:-").append("<ol>");
				for (int leaveRequestCounter = 0; leaveRequestCounter < unApprovesLeaveRequests.length(); leaveRequestCounter++) {
					JSONObject leaveRequestObject = unApprovesLeaveRequests.getJSONObject(leaveRequestCounter);
					String fromDateString = Translator.stringValue(leaveRequestObject.opt("fromdate"));
					String fromDurationType = Translator.stringValue(leaveRequestObject.opt("fromdurationtypeid.name"));
					String toDateString = Translator.stringValue(leaveRequestObject.opt("todate"));
					String toDurationType = Translator.stringValue(leaveRequestObject.opt("todurationtypeid.name"));
					// int leaveType = Translator.integerValue(leaveRequestObject.opt("leavetypeid"));
					// long daysDiff = DataTypeUtilities.differenceBetweenDates(DataTypeUtilities.checkDateFormat(fromDate), DataTypeUtilities.checkDateFormat(toDate));
					filter = "employeeid = " + employeeId + " and attendancedate >= '" + fromDateString + "' and attendancedate <= '" + toDateString + "'";// and leavetypeid in (" + leaveType + ") and paidstatus = null";
					employeeAttendanceArray = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);

					for (int dailyAttendance = 0; dailyAttendance < employeeAttendanceArray.length(); dailyAttendance++) {
						int attendanceType = Translator.integerValue(employeeAttendanceArray.getJSONObject(dailyAttendance).opt("attendancetypeid"));
						Object paidStatus = employeeAttendanceArray.getJSONObject(dailyAttendance).opt("paidstatus");
						if (attendanceType == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceType == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
							leaveCounter++;
						} else if ((attendanceType == HRISApplicationConstants.ATTENDANCE_LEAVE || attendanceType == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceType == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) && paidStatus == null) {
							leaveCounter++;
						}
					}
					if (leaveCounter > 0) {
						unApprovedLEaves.append("<li>").append("From Date:- ").append(fromDateString).append(" From Duration Type:- ").append(fromDurationType).append(", To Date:- ").append(toDateString).append(" To Duration Type:- ").append(toDurationType).append("</li>");
					}
				}
				unApprovedLEaves.append("</ol></li>");
			}
			if (leaveCounter > 0) {
				mailValue.append(unApprovedLEaves);
			}

			HashMap<Date, Integer> offDaysMap = new HashMap<Date, Integer>();
			HashMap<Date, Integer> holidaysMap = new HashMap<Date, Integer>();
			HashMap<Date, Object[]> dailyAttendanceMap = new HashMap<Date, Object[]>();

			new EmployeeMonthlyAttendance().getTotalOffAndHolidaysMaps(currentMonthFirstDate, currentMonthFirstDateInString, currentMonthLastDate, currentMonthLastDateInString, offDaysMap, holidaysMap, dailyAttendanceMap, scheduleId, holidayCalendarId, employeeId);
			List<Date> offDaysList = new ArrayList<Date>();
			for (Date date : offDaysMap.keySet()) {
				if (!offDaysList.contains(date)) {
					offDaysList.add(date);
				}
			}
			for (Date date : holidaysMap.keySet()) {
				if (!offDaysList.contains(date)) {
					offDaysList.add(date);
				}
			}
			Collections.sort(offDaysList);
			for (int offDaysCounter = 0; offDaysCounter < offDaysList.size(); offDaysCounter++) {
				Date attendanceDateOffDay = offDaysList.get(offDaysCounter);
				Calendar dateCal = Calendar.getInstance();
				for (int innerCounter = 0; innerCounter < 7; innerCounter++) {
					dateCal.setTime(attendanceDateOffDay);
					dateCal.add(Calendar.DAY_OF_MONTH, innerCounter);
					Date attendanceDateForCheck = dateCal.getTime();
					if (!offDaysList.contains(attendanceDateForCheck)) {
						Object[] dailyAttendanceDetails = dailyAttendanceMap.get(attendanceDateForCheck);
						if (dailyAttendanceDetails != null) {
							int attendanceTypeId = Translator.integerValue(dailyAttendanceDetails[1]);
							if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
								for (int innerCounterForPrevious = 1; innerCounterForPrevious < 7; innerCounterForPrevious++) {
									dateCal.setTime(attendanceDateOffDay);
									dateCal.add(Calendar.DAY_OF_MONTH, -innerCounterForPrevious);
									Date attendanceDateForCheckForPrevious = dateCal.getTime();
									if (!offDaysList.contains(attendanceDateForCheckForPrevious)) {
										dailyAttendanceDetails = dailyAttendanceMap.get(attendanceDateForCheckForPrevious);
										if (dailyAttendanceDetails != null) {
											attendanceTypeId = Translator.integerValue(dailyAttendanceDetails[1]);
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
												dailyAttendanceDetails = dailyAttendanceMap.get(attendanceDateOffDay);
												if (dailyAttendanceDetails != null) {
													attendanceTypeId = Translator.integerValue(dailyAttendanceDetails[1]);
													if (attendanceTypeId != HRISApplicationConstants.ATTENDANCE_ABSENT && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_LEAVE && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
														mailValue.append("<li>").append(EmployeeSalaryGenerationServlet.getDateInString(attendanceDateOffDay)).append(" is a Sandwich case but not marked.").append("</li>");
													}
												}
											}
											break;
										}
									}
								}
							}
							break;
						}
					}
				}
			}

			StringBuilder footerForEmployee = new StringBuilder();
			footerForEmployee.append("</ol>").append("</td>").append("</tr>");
			if (mailValue.toString().length() > 0) {
				employeeSalaryComponentIssueForMail += employeeNameForMail.toString() + mailValue.toString() + footerForEmployee.toString();
			}
		}
		return new String[] { headerForMail, employeeSalaryComponentIssueForMail, footerForMail };
	}

	private static JSONArray getEmployeePerformance(Object employeeId, int monthId, long yearId, Object kpiId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, EmployeePerformanceKinds.RESOURCE);
		columnArray.put(EmployeePerformanceKinds.AMOUNT);
		query.put(Data.Query.FILTERS, EmployeePerformanceKinds.EMPLOYEE_ID + " = " + employeeId + " and " + EmployeePerformanceKinds.MONTH_ID + " = " + monthId + " and " + EmployeePerformanceKinds.YEAR_ID + " = " + yearId + " and " + EmployeePerformanceKinds.KPI_ID + " = " + kpiId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(EmployeePerformanceKinds.RESOURCE);
	}

	private static JSONArray getUnApprovesLeaveRequests(Object employeeId, String currentMonthFirstDateInString, String currentMonthLastDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_leaverequests");
		columnArray.put(Updates.KEY);
		columnArray.put("fromdate");
		columnArray.put("todate");
		columnArray.put("leavetypeid");
		columnArray.put("fromdurationtypeid.name");
		columnArray.put("todurationtypeid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and fromdate  >= '" + currentMonthFirstDateInString + "' and " + "fromdate  <= '" + currentMonthLastDateInString + "'" + " and leavestatusid = " + HRISConstants.LEAVE_NEW);
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + " and todate  >= '" + currentMonthFirstDateInString + "' and " + "todate  <= '" + currentMonthLastDateInString + "'" + " and leavestatusid = " + HRISConstants.LEAVE_NEW);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_leaverequests");
		String keys = "";
		for (int counter = 0; counter < rows.length(); counter++) {
			Object key = rows.getJSONObject(counter).opt(Updates.KEY);
			if (keys.length() > 0) {
				keys += ",";
			}
			keys += ("" + key);
		}
		String extraFilters = "";
		if (keys.length() > 0) {
			extraFilters = " and " + Updates.KEY + " NOT IN (" + keys + ")";
		}
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and fromdate  <= '" + currentMonthFirstDateInString + "' and " + "todate  >= '" + currentMonthLastDateInString + "'" + extraFilters + " and leavestatusid = " + HRISConstants.LEAVE_NEW);
		query.remove(Data.Query.OPTION_FILTERS);
		JSONArray rows1 = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_leaverequests");
		if (rows1 != null && rows1.length() > 0) {
			for (int counter = 0; counter < rows1.length(); counter++) {
				rows.put(rows1.getJSONObject(counter));
			}
		}

		return rows;
	}

	private static JSONObject getExtraUsefulDetails(String dateForReport) throws ParseException, JSONException {
		String sendingDateString = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		String TODAY_DATE_CURRENT = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());

		if (dateForReport != null && dateForReport.length() > 0) {
			sendingDateString = dateForReport;
		}
		// String TODAY_DATE = "2012-06-20";
		Date sendingDate = DataTypeUtilities.checkDateFormat(sendingDateString);
		Date todayDateCurrent = DataTypeUtilities.checkDateFormat(TODAY_DATE_CURRENT);
		Calendar cal = Calendar.getInstance();
		cal.setTime(sendingDate);
		long currentMonthId = cal.get(Calendar.MONTH) + 1;
		// long currentYearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
		String currentMonthName = EmployeeSalaryGenerationServlet.getMonthName(currentMonthId);
		String currentYearName = String.valueOf(cal.get(Calendar.YEAR));
		Date currentMonthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYearName, currentMonthName);
		Date currentMonthLastDate = DataTypeUtilities.getMonthLastDate(currentMonthFirstDate);
		String currentMonthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthFirstDate);
		String currentMonthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthLastDate);

		cal.setTime(sendingDate);
		cal.add(Calendar.MONTH, -1);

		long previousMonthId = cal.get(Calendar.MONTH) + 1;

		String previousMonthName = EmployeeSalaryGenerationServlet.getMonthName(previousMonthId);
		String previousYearName = String.valueOf(cal.get(Calendar.YEAR));
		Date previousMonthFirstDate = DataTypeUtilities.getMonthFirstDate(previousYearName, previousMonthName);
		Date previousMonthLastDate = DataTypeUtilities.getMonthLastDate(previousMonthFirstDate);
		String previousMonthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(previousMonthFirstDate);
		String previousMonthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(previousMonthLastDate);

		JSONObject fields = new JSONObject();

		fields.put("todayDateInString", TODAY_DATE_CURRENT);
		fields.put("todayDate", todayDateCurrent);
		fields.put("sendingDateInString", sendingDateString);
		fields.put("sendingDate", sendingDate);
		fields.put("currentMonthFirstDateInString", currentMonthFirstDateInString);
		fields.put("currentMonthLastDateInString", currentMonthLastDateInString);
		fields.put("currentMonthFirstDate", currentMonthFirstDate);
		fields.put("currentMonthLastDate", currentMonthLastDate);
		fields.put("currentMonthName", currentMonthName);
		fields.put("currentYearName", currentYearName);

		fields.put("previousMonthFirstDateInString", previousMonthFirstDateInString);
		fields.put("previousMonthLastDateInString", previousMonthLastDateInString);
		fields.put("previousMonthFirstDate", previousMonthFirstDate);
		fields.put("previousMonthLastDate", previousMonthLastDate);

		return fields;
	}

	public static JSONObject getIgnoreAbleComponentKeys(HashMap<Integer, Object[]> salaryComponentsMap) throws JSONException {
		String ignoreAbleComponentKeys = "";
		String compulsoryComponentKeys = "";
		List<Integer> ignoreableComponentList = new ArrayList<Integer>();
		List<Integer> compulsoryComponentList = new ArrayList<Integer>();
		for (Integer salaryComponentId : salaryComponentsMap.keySet()) {
			Object[] details = salaryComponentsMap.get(salaryComponentId);
			int ignoreInExceptionReport = DataTypeUtilities.integerValue(details[0]);
			int isCompulsoryIfDefined = DataTypeUtilities.integerValue(details[1]);
			if ((salaryComponentId + "").length() > 0) {
				if (ignoreInExceptionReport == HRISApplicationConstants.EmployeeDecision.YES) {
					ignoreableComponentList.add(salaryComponentId);
					if (ignoreAbleComponentKeys.length() > 0) {
						ignoreAbleComponentKeys += "," + salaryComponentId;
					} else {
						ignoreAbleComponentKeys += salaryComponentId;
					}
				}

				if (isCompulsoryIfDefined == HRISApplicationConstants.EmployeeDecision.YES) {
					compulsoryComponentList.add(salaryComponentId);
					if (compulsoryComponentKeys.length() > 0) {
						compulsoryComponentKeys += "," + salaryComponentId;
					} else {
						compulsoryComponentKeys += salaryComponentId;
					}
				}
			}
		}
		JSONObject object = new JSONObject();

		object.put("ignoreAbleComponentKeys", ignoreAbleComponentKeys);
		object.put("compulsoryComponentKeys", compulsoryComponentKeys);
		object.put("ignoreableComponentList", new JSONArray(ignoreableComponentList));
		object.put("compulsoryComponentList", new JSONArray(compulsoryComponentList));
		return object;
	}

	private static JSONArray getEmployeeArray(int profitCenterId, JSONObject fields) throws JSONException {
		JSONArray columnArray = new JSONArray();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		columnArray.put(Updates.KEY);
		columnArray.put("name");
		columnArray.put("ctc");
		columnArray.put("panno");
		columnArray.put("joiningdate");
		columnArray.put("accountno");
		columnArray.put("relievingdate");
		columnArray.put("providentfundno");
		columnArray.put("employeescheduleid");
		columnArray.put("holidaycalendarid");
		columnArray.put("deviceid");

		query.put(Data.Query.COLUMNS, columnArray);
		// query.put(Data.Query.FILTERS, "__key__ IN(11,29)");
		// query.put(Data.Query.FILTERS, "branchid = " + branchId + " and __key__ in (130, 226)");
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and profitcenterid = " + profitCenterId + " and joiningdate <= '" + fields.opt("currentMonthLastDateInString") + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and profitcenterid = " + profitCenterId + " and relievingdate >= '" + fields.opt("currentMonthFirstDateInString") + "'");
		// query.put(Data.Query.MAX_ROWS, 50);
		JSONObject resultSet = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = resultSet.getJSONArray("hris_employees");
		return rows;
	}

	public static Object[] putSalaryCpmponentMap(JSONArray salaryComponents, HashMap<Integer, Object[]> salaryComponentsMap) throws JSONException {
		Object pfEmployeeComponentId = null;
		Object pfEmployerComponentId = null;
		for (int counter = 0; counter < salaryComponents.length(); counter++) {
			Integer salaryComponentId = DataTypeUtilities.integerValue(salaryComponents.getJSONObject(counter).opt(Updates.KEY));
			Object ignoreInExceptionReport = DataTypeUtilities.integerValue(salaryComponents.getJSONObject(counter).opt("ignoreinexceptionreport"));
			Object isCompulsoryIfDefined = DataTypeUtilities.integerValue(salaryComponents.getJSONObject(counter).opt("compulsoryifdefined"));
			String name = DataTypeUtilities.stringValue(salaryComponents.getJSONObject(counter).opt("name"));
			Object[] details = { ignoreInExceptionReport, isCompulsoryIfDefined };
			salaryComponentsMap.put(salaryComponentId, details);

			if (name.replaceAll(" ", "").equalsIgnoreCase("PFEmployee")) {
				pfEmployeeComponentId = salaryComponentId;
			}

			if (name.replaceAll(" ", "").equalsIgnoreCase("PFEmployer")) {
				pfEmployerComponentId = salaryComponentId;
			}

		}
		return new Object[] { pfEmployeeComponentId, pfEmployerComponentId };
	}

	public static JSONArray getEmployeeSalaryComponents(String monthFirstDate, String monthLastDate, Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid." + SalaryComponentKinds.CRITERIA_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.PERFORMANCE_TYPE_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.KPI_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.PAID_AFTER_MONTH);
		columnArray.put("duemonthid");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthFirstDate + "' and applicableto >= '" + monthFirstDate + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeId + " and applicablefrom <= '" + monthLastDate + "' and applicableto >= '" + monthLastDate + "'");
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");

		String keys = "";
		for (int counter = 0; counter < rows.length(); counter++) {
			int key = Translator.integerValue(rows.getJSONObject(counter).opt(Updates.KEY));
			if (keys.length() > 0) {
				keys += ("," + key);
			} else {
				keys += ("" + key);
			}
		}
		String filters = "";
		if (keys.length() > 0) {
			filters += (" and " + Updates.KEY + " not in (" + keys + ")");
		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid." + SalaryComponentKinds.NAME);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.CRITERIA_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.PERFORMANCE_TYPE_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.KPI_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.PAID_AFTER_MONTH);
		columnArray.put("duemonthid");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and applicablefrom >= '" + monthFirstDate + "' and applicableto <= '" + monthLastDate + "'" + filters);
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows1 = object.getJSONArray("employeesalarycomponents");

		for (int counter = 0; counter < rows1.length(); counter++) {
			JSONObject object1 = rows1.getJSONObject(counter);
			rows.put(object1);
		}

		return rows;
	}
}
