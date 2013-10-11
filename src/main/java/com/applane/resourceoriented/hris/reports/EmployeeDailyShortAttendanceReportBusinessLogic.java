package com.applane.resourceoriented.hris.reports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class EmployeeDailyShortAttendanceReportBusinessLogic {

	private static final String HEADER = "header";

	public void sendDailyShortAttendanceReport() {
		try {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			EmployeeMonthlyShortAttendanceReportBusinessLogic emsabl = new EmployeeMonthlyShortAttendanceReportBusinessLogic();
			JSONArray schedules = emsabl.getSchedules();
			HashMap<Integer, String> scheduleHoursMAP = new HashMap<Integer, String>();
			emsabl.getScheduleHoursMAP(schedules, scheduleHoursMAP);
			sendReport(null, scheduleHoursMAP, TODAY_DATE);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("EmployeeDailyShortAttendanceReportBusinessLogic >> sendDailyShortAttendanceReport >> Exception  >> : [" + trace + "]");
			// throw new BusinessLogicException("Some Unknown Error Occured, Please Contace To Admin");
			throw new RuntimeException(trace);
		}
	}

	public void sendReport(HashMap<Integer, JSONObject> employeeListWithShortAttendance, HashMap<Integer, String> employeeScheduleTimeInOffice, String TODAY_DATE) throws ParseException, JSONException, Exception {
		String previousDateString = DataTypeUtilities.getBackDate(TODAY_DATE);
		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		JSONArray hrEmails = new JSONArray();
		if (organizationId == 7783) {
			hrEmails = EmployeeSalarySheetReport.getHrMailIdDetailFromBranch("");
		} else {
			hrEmails = EmployeeSalarySheetReport.getHrMailIdDetail("");
		}

		String logo = "";
		JSONArray mailConfigurationSetup = new MorningSchdularHRIS().getMailConfigurationSetup();
		List<Integer> disableDailyShortAttendanceMailBranchWise = new ArrayList<Integer>();
		JSONObject disableRecords = new JSONObject();

		if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
			Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
			if (organizationlogo != null) {
				logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
			}
			Object mailIdArray = mailConfigurationSetup.getJSONObject(0).opt("disable_d_s_att_m_b_w");
			if (mailIdArray != null && mailIdArray instanceof JSONArray) {
				for (int counterEmail = 0; counterEmail < ((JSONArray) mailIdArray).length(); counterEmail++) {
					disableDailyShortAttendanceMailBranchWise.add(Translator.integerValue(((JSONArray) mailIdArray).getJSONObject(counterEmail).opt(Updates.KEY)));
				}
			}
		}
		disableRecords.put("1", disableDailyShortAttendanceMailBranchWise);

		Map<String, String> reportingToMail = new HashMap<String, String>();
		for (int counter = 0; counter < hrEmails.length(); counter++) {
			String timeShouleBeInOffice = "07:00";
			Object branchId = null;
			String hrEmailId = "";
			if (organizationId == 7783) {
				branchId = hrEmails.getJSONObject(counter).opt("__key__");
				JSONArray ids = hrEmails.getJSONObject(counter).optJSONArray("hr_email_ids");
				for (int i = 0; i < ids.length(); i++) {
					Object emailId = ids.getJSONObject(i).opt("officialemailid");
					if (hrEmailId.length() > 0 && emailId != null) {
						hrEmailId += ",";
					}
					if (emailId != null) {
						hrEmailId += emailId;
					}
				}
			} else {
				branchId = hrEmails.getJSONObject(counter).opt("branchid");
				hrEmailId = Translator.stringValue(hrEmails.getJSONObject(counter).opt("employeeid.officialemailid"));
			}

			JSONArray employeeAttendance = getEmployeeAttendance(previousDateString, branchId, employeeScheduleTimeInOffice);
			String mailValue = getShortAttendanceValue(employeeAttendance, timeShouleBeInOffice, employeeListWithShortAttendance, employeeScheduleTimeInOffice, disableRecords, reportingToMail);

			if (mailValue.length() > 0 && hrEmailId.length() > 0 && employeeListWithShortAttendance == null) {
				String valueToMail = "";
				valueToMail += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
				valueToMail += "<hr></hr><br></br>";
				valueToMail += "<table border='1' cellspacing='0'>";

				String[] toMailIds = null;
				if (organizationId == 7783) {
					toMailIds = hrEmailId.split(",");
				} else {
					toMailIds = new String[] { hrEmailId };
				}
				// String[] toMailIds = { ApplicationConstants.OWNER };
				mailValue = valueToMail + mailValue + "</table>";
				mailValue += "<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
				HrisHelper.sendMails(toMailIds, mailValue, "Short Attendance for date " + previousDateString);
			}
		}
		if (reportingToMail != null && reportingToMail.size() > 0) {
			String header = reportingToMail.get(HEADER);
			reportingToMail.remove(HEADER);
			if (reportingToMail.size() > 0) {
				for (String reportingToMailId : reportingToMail.keySet()) {
					String valueToMail = "";
					valueToMail += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
					valueToMail += "<hr></hr><br></br>";
					valueToMail += "<table border='1' cellspacing='0'>";
					if (reportingToMailId == null || reportingToMailId.length() == 0) {
						reportingToMailId = ApplicationConstants.OWNER;
					}
					String[] toMailIds = { reportingToMailId };
					// String[] toMailIds = { ApplicationConstants.OWNER };
					valueToMail = valueToMail + header + reportingToMail.get(reportingToMailId) + "</table>";
					valueToMail += "<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
					HrisHelper.sendMails(toMailIds, valueToMail, "Short Attendance Of Your Team for date " + previousDateString);

				}
			}

		}
	}

	@SuppressWarnings("unchecked")
	private String getShortAttendanceValue(JSONArray employeeAttendance, String timeShouleBeInOffice, HashMap<Integer, JSONObject> employeeListWithShortAttendance, HashMap<Integer, String> employeeScheduleTimeInOffice, JSONObject disableRecords, Map<String, String> reportingToMail) throws Exception {

		String employeeAttendanceValue = "<tr><td bgcolor='99CCCC'>Name</td><td bgcolor='99CCCC'>Date</td><td bgcolor='99CCCC'>In Time</td><td bgcolor='99CCCC'>Out Time</td><td bgcolor='99CCCC'>Total Time In Office</td><td bgcolor='99CCCC'>Status</td></tr>";
		String header = "<tr><td bgcolor='99CCCC'>Name</td><td bgcolor='99CCCC'>Date</td><td bgcolor='99CCCC'>In Time</td><td bgcolor='99CCCC'>Out Time</td><td bgcolor='99CCCC'>Total Time In Office</td><td bgcolor='99CCCC'>Status</td></tr>";
		reportingToMail.put(HEADER, header);

		boolean haveValues = false;
		List<Integer> disableDailyShortAttendanceMailBranchWise = (List<Integer>) disableRecords.opt("1");
		for (int attendanceCounter = 0; attendanceCounter < employeeAttendance.length(); attendanceCounter++) {
			int key = Translator.integerValue(employeeAttendance.getJSONObject(attendanceCounter).opt(Updates.KEY));
			int employeeId = Translator.integerValue(employeeAttendance.getJSONObject(attendanceCounter).opt("employeeid"));
			int branchId = Translator.integerValue(employeeAttendance.getJSONObject(attendanceCounter).opt("employeeid.branchid"));
			int attendancetypeId = Translator.integerValue(employeeAttendance.getJSONObject(attendanceCounter).opt("attendancetypeid"));

			String employeeName = Translator.stringValue(employeeAttendance.getJSONObject(attendanceCounter).opt("employeeid.name"));
			String attendanceDate = Translator.stringValue(employeeAttendance.getJSONObject(attendanceCounter).opt("attendancedate"));
			String inTime = Translator.stringValue(employeeAttendance.getJSONObject(attendanceCounter).opt("firstintime"));
			String outTime = Translator.stringValue(employeeAttendance.getJSONObject(attendanceCounter).opt("lastouttime"));
			String status = Translator.stringValue(employeeAttendance.getJSONObject(attendanceCounter).opt("punchingtypeid.name"));
			String reportingToEmailId = Translator.stringValue(employeeAttendance.getJSONObject(attendanceCounter).opt("employeeid.reportingtoid.officialemailid"));
			int scheduleId = Translator.integerValue(employeeAttendance.getJSONObject(attendanceCounter).opt("employeeid.employeescheduleid"));
			int leavePolicyId = Translator.integerValue(employeeAttendance.getJSONObject(attendanceCounter).opt("employeeid.leavepolicyid"));
			String actualTimeInOffice = employeeAttendance.getJSONObject(attendanceCounter).opt("totaltimeinoffice") == null ? timeShouleBeInOffice : Translator.stringValue(employeeAttendance.getJSONObject(attendanceCounter).opt("totaltimeinoffice"));
			// if (employeeListWithShortAttendance != null && employeeScheduleTimeInOffice != null) {
			if (employeeScheduleTimeInOffice != null) {
				timeShouleBeInOffice = employeeScheduleTimeInOffice.get(scheduleId) == null ? timeShouleBeInOffice : employeeScheduleTimeInOffice.get(scheduleId);
			}
			if (employeeListWithShortAttendance != null && employeeScheduleTimeInOffice == null) {
				timeShouleBeInOffice = "05:00";
			}
			if (actualTimeInOffice != null && timeShouleBeInOffice.length() > 0) {
				String[] timeArray = { "00:00" };
				if (actualTimeInOffice.contains(" ")) {
					timeArray = actualTimeInOffice.split(" ");
				} else {
					timeArray = actualTimeInOffice.split(":");
				}
				String[] hoursMinutsInOffice = timeShouleBeInOffice.split(":");
				if (timeShouleBeInOffice.contains(".")) {
					hoursMinutsInOffice = timeShouleBeInOffice.split(".");
				}
				double hoursShouldBeInOffice = Integer.parseInt(hoursMinutsInOffice[0]);
				double minutsShouldBeInOffice = Integer.parseInt(hoursMinutsInOffice[1]);

				String hours = "";
				String minuts = "";
				for (int timeCounter = 0; timeCounter < timeArray[0].length(); timeCounter++) {
					if (timeArray[0].charAt(timeCounter) >= 48 && timeArray[0].charAt(timeCounter) <= 57) {
						hours += timeArray[0].charAt(timeCounter);
					}
				}

				for (int timeCounter = 0; timeCounter < timeArray[1].length(); timeCounter++) {
					if (timeArray[1].charAt(timeCounter) >= 48 && timeArray[1].charAt(timeCounter) <= 57) {
						minuts += timeArray[1].charAt(timeCounter);
					}
				}
				if ((hoursShouldBeInOffice > Double.parseDouble(hours)) || (hoursShouldBeInOffice == Double.parseDouble(hours) && minutsShouldBeInOffice > Double.parseDouble(minuts))) {
					if (!disableDailyShortAttendanceMailBranchWise.contains(branchId)) {
						if (!haveValues) {
							haveValues = true;
						}
						employeeAttendanceValue += ("<tr><td>&nbsp;" + employeeName + "</td><td>&nbsp;" + attendanceDate + "</td><td>&nbsp;" + inTime + "</td><td>&nbsp;" + outTime + "</td><td>&nbsp;" + actualTimeInOffice + "</td><td>&nbsp;" + status + "</td></tr>");
						String oldValue = "";
						if (reportingToMail.containsKey(reportingToEmailId)) {
							oldValue = reportingToMail.get(reportingToEmailId);
						}
						oldValue += ("<tr><td>&nbsp;" + employeeName + "</td><td>&nbsp;" + attendanceDate + "</td><td>&nbsp;" + inTime + "</td><td>&nbsp;" + outTime + "</td><td>&nbsp;" + actualTimeInOffice + "</td><td>&nbsp;" + status + "</td></tr>");
						reportingToMail.put(reportingToEmailId, oldValue);
					}
					if (employeeListWithShortAttendance != null) {
						JSONObject details = new JSONObject();
						details.put("key", key);
						details.put("leavePolicyId", leavePolicyId);
						details.put("attendanceDate", attendanceDate);
						details.put("attendancetypeId", attendancetypeId);
						employeeListWithShortAttendance.put(employeeId, details);
					}
				}
			}
		}
		if (!haveValues) {
			employeeAttendanceValue = "";
		}
		return employeeAttendanceValue;
	}

	private JSONArray getEmployeeAttendance(String previousDateString, Object branchId, HashMap<Integer, String> employeeScheduleTimeInOffice) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeescheduleid");
		columnArray.put("employeeid.leavepolicyid");
		columnArray.put("employeeid.branchid");
		columnArray.put("employeeid.reportingtoid.officialemailid");
		columnArray.put("punchingtypeid.name");
		columnArray.put("attendancetypeid");
		columnArray.put("attendancedate");
		columnArray.put("firstintime");
		columnArray.put("lastouttime");
		columnArray.put("totaltimeinoffice");
		query.put(Data.Query.COLUMNS, columnArray);
		if (employeeScheduleTimeInOffice == null) {
			query.put(Data.Query.FILTERS, "employeeid.branchid = " + branchId + " and attendancedate = '" + previousDateString + "' and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_PRESENT + " , " + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY + ")");
		} else {
			query.put(Data.Query.FILTERS, "employeeid.branchid = " + branchId + " and attendancedate = '" + previousDateString + "' and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_PRESENT + ")");// , " + HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE + ", " + HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE + ")");
		}
		JSONObject attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray monthlyAttendanceArray = attendanceObject.getJSONArray("employeeattendance");
		return monthlyAttendanceArray;
	}

	public static JSONArray getEmployeeArray(String firstDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("employeecode");
		columnArray.put("employeeid.reportingtoid.officialemailid");
		columnArray.put("employeescheduleid");
		columnArray.put("subbranchid.name");
		columnArray.put("designationid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and relievingdate >= '" + firstDateInString + "'");
		// query.put(Data.Query.MAX_ROWS, 5);
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}
}