package com.applane.resourceoriented.hris.reports;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class AbsentReportOnEmailBusinessLogic {
	public void sendAbsentReportOnEmail(Object[] keys) {
		try {
			String todayDateString = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date todayDate = Translator.dateValue(todayDateString);
			Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(todayDate);
			Date monthLastDate = DataTypeUtilities.getMonthLastDate(todayDate);
			String monthFirstDateString = Translator.getDateInString(monthFirstDate);
			String monthLastDateString = Translator.getDateInString(monthLastDate);
			HashMap<String, String> employeeMailValue = new HashMap<String, String>();
			HashMap<String, String> reportingToEmployeeMailValue = new HashMap<String, String>();
			JSONArray employeeAttendanceDetails = getEmployeeDetailsData(monthFirstDateString, monthLastDateString);
			JSONArray mailConfigurationSetup = new MorningSchdularHRIS().getMailConfigurationSetup();
			String logo = "";
			if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
				Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
				if (organizationlogo != null) {
					logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
				}
			}
			String valueToMail = "";
			valueToMail += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
			valueToMail += "<hr></hr><br></br>";
			valueToMail += "<table border='1' cellspacing='0'>";
			valueToMail += "<tr><td bgcolor='99CCCC'>Employee Code</td><td bgcolor='99CCCC'>Employee Name</td><td bgcolor='99CCCC'>Department</td></tr>";
			for (int counter = 0; counter < employeeAttendanceDetails.length(); counter++) {
				JSONObject jsonObject = employeeAttendanceDetails.optJSONObject(counter);
				JSONArray attendanceRows = jsonObject.optJSONArray("employeeattendance");
				if (attendanceRows != null && attendanceRows.length() > 0) {
					StringBuilder value = new StringBuilder();
					String employeeCode = jsonObject.optString("employeecode");
					String employeeMail = jsonObject.optString("officialemailid", "");
					String employeeName = jsonObject.optString("name", "");
					Object department = jsonObject.opt("departmentid.name") == null ? "" : jsonObject.opt("departmentid.name");
					String reportingToEmployeeMail = jsonObject.optString("reportingtoid.officialemailid", "");
					// String reportingToEmployeeName = jsonObject.optString("reportingtoid.name", "");
					value.append("<tr>");
					value.append("<td>&nbsp;").append(employeeCode).append("</td><td>&nbsp;").append(employeeName).append("</td><td>&nbsp;").append(department).append("</td>");
					value.append("</tr><tr valign='top'>");
					value.append("<td>&nbsp;</td><td colspan='2'>&nbsp;<table border='1' cellspacing='0'>");
					value.append("<tr><td bgcolor='99CCCC'>Date</td><td bgcolor='99CCCC'>Attendance Status</td></tr>");
					for (int attendanceCounter = 0; attendanceCounter < attendanceRows.length(); attendanceCounter++) {
						JSONObject attendance = attendanceRows.getJSONObject(attendanceCounter);
						int attendanceTypeId = attendance.optInt("attendancetypeid", 0);
						Object date = attendance.opt("attendancedate");
						String attendanceType = "";
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
							attendanceType = "Absent";
						} else {
							attendanceType = "Half Day Absent";
						}
						value.append("<tr><td>").append(date).append("</td><td>").append(attendanceType).append("</td></tr>");
					}

					value.append("</table></td></tr>");
					employeeMailValue.put(employeeMail, value.toString());
					String reportingValue = value.toString();
					if (reportingToEmployeeMailValue.containsKey(reportingToEmployeeMail)) {
						reportingValue += reportingToEmployeeMailValue.get(reportingToEmployeeMail);
					}
					reportingToEmployeeMailValue.put(reportingToEmployeeMail, reportingValue);
				}
			}
			for (String email : employeeMailValue.keySet()) {
				String value = employeeMailValue.get(email);
				HrisHelper.sendMails(new String[] { email }, (valueToMail + value + "</table>"), "Absent(s) In Month");
				// HrisHelper.sendMails(new String[] { "kapil.dalal@daffodilsw.com" }, (valueToMail + value + "</table>"), "Absent(s) In Month");
			}
			for (String email : reportingToEmployeeMailValue.keySet()) {
				String value = reportingToEmployeeMailValue.get(email);
				HrisHelper.sendMails(new String[] { email }, (valueToMail + value + "</table>"), "Your Team's Absent(s) In Month");
				// HrisHelper.sendMails(new String[] { "kapil.dalal@daffodilsw.com" }, (valueToMail + value + "</table>"), "Your Team's Absent(s) In Month");
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("AbsentReportOnEmailBusinessLogic >> Trace >> " + trace);
		}
	}

	private JSONArray getEmployeeDetailsData(Object fromDate, Object toDate) throws JSONException {
		LogUtility.writeLog("getEmployeeAttendanceReportData >> toDate : " + toDate + ", fromDate : " + fromDate);

		StringBuilder filter = new StringBuilder("employeestatusid = ").append(HRISApplicationConstants.EMPLOYEE_ACTIVE).append(" AND joiningdate<='").append(toDate).append("'");
		StringBuilder optionFilter = new StringBuilder("employeestatusid = ").append(HRISApplicationConstants.EMPLOYEE_INACTIVE);
		optionFilter.append(" AND ");
		optionFilter.append("relievingdate >= '").append(fromDate).append("'");

		JSONObject parameters = null;

		JSONArray columns = new JSONArray();
		columns.put("name");
		columns.put("employeecode");
		columns.put("officialemailid");
		columns.put("departmentid.name");
		columns.put("reportingtoid.name");
		columns.put("reportingtoid.officialemailid");

		JSONObject employeeQuery = new JSONObject();
		employeeQuery.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		employeeQuery.put(Data.Query.CHILDS, populateChildQuery("" + fromDate, "" + toDate));
		employeeQuery.put(Data.Query.COLUMNS, columns);
		// employeeQuery.put(Data.Query.FILTERS, "__key__ IN(1312)");
		employeeQuery.put(Data.Query.FILTERS, filter.toString());
		employeeQuery.put(Data.Query.OPTION_FILTERS, optionFilter.toString());
		employeeQuery.put(Data.Query.PARAMETERS, parameters);

		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(employeeQuery).getJSONArray(HrisKinds.EMPLOYEES);

	}

	private JSONArray populateChildQuery(String fromDate, String toDate) throws JSONException {
		JSONArray columnsRequired = new JSONArray();
		columnsRequired.put("__key__");
		columnsRequired.put("attendancetypeid");
		columnsRequired.put("attendancedate");

		JSONObject attendanceQuery = new JSONObject();
		attendanceQuery.put(Data.Query.RESOURCE, "employeeattendance");
		attendanceQuery.put(Data.Query.COLUMNS, columnsRequired);
		attendanceQuery.put(Data.Query.FILTERS, "attendancedate>='" + fromDate + "' AND attendancedate<='" + toDate + "' AND attendancetypeid IN(" + HRISApplicationConstants.ATTENDANCE_ABSENT + "," + HRISApplicationConstants.ATTENDANCE_HALF_DAY_ABSENT + ")");

		JSONObject child = new JSONObject();
		child.put(Data.Query.Childs.RELATED_COLUMN, "employeeid");
		child.put(Data.Query.Childs.QUERY, attendanceQuery);
		child.put(Data.Query.Childs.ALIAS, "employeeattendance");

		return new JSONArray().put(child);
	}
}