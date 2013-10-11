package com.applane.resourceoriented.hris.reports;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class MonthlyAttendanceOnMail {
	public void sendMonthlyAttendanceReport(Object date) {
		sendMonthlyAttendanceReportFromTaskQueue(true, date);
	}

	public void sendMonthlyAttendanceReportFromTaskQueue(boolean fromInvoke, Object date) {
		try {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date todayDate = Translator.dateValue(TODAY_DATE);
			boolean enterInIfCondition = true;
			if (!fromInvoke) {
				Date firstDayOfCurrentMonth = DataTypeUtilities.getFirstDayOfMonth(todayDate);
				enterInIfCondition = todayDate.equals(firstDayOfCurrentMonth);
			}
			if (enterInIfCondition) {
				JSONArray mailConfigurationSetup = new MorningSchdularHRIS().getMailConfigurationSetup();
				String logo = "";
				if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
					Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
					if (organizationlogo != null) {
						logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
					}
				}
				Calendar cal = Calendar.getInstance();
				if (date != null) {
					cal.setTime(Translator.dateValue(date));
				} else {
					cal.setTime(todayDate);
					cal.add(Calendar.MONTH, -1);
				}
				int monthId = cal.get(Calendar.MONTH) + 1;
				long yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));

				// String monthName = EmployeeSalarySheetReport.getMonthName(monthId);
				// String yearName = EmployeeSalarySheetReport.getYearName(yearId);
				JSONArray monthlyAttendanceArray = getMonthlyAttendanceArray(yearId, monthId);

				HashMap<String, String> mailToReportinHeadMap = new HashMap<String, String>();
				for (int counter = 0; counter < monthlyAttendanceArray.length(); counter++) {
					getMailValueForTL(mailToReportinHeadMap, monthlyAttendanceArray.getJSONObject(counter));
				}

				String valueToMail = "";
				valueToMail += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
				valueToMail += "<hr></hr><br></br>";
				valueToMail += "<table border='1' cellspacing='0'>";
				valueToMail += "<tr><td bgcolor='99CCCC'>Employee Name</td><td bgcolor='99CCCC'>Presents</td><td bgcolor='99CCCC'>Absents</td><td bgcolor='99CCCC'>EWD</td><td bgcolor='99CCCC'>Payable EWD</td><td bgcolor='99CCCC'>Leaves</td><td bgcolor='99CCCC'>Payable Leaves</td><td bgcolor='99CCCC'>Avtual Non Working Days</td></tr>";
				for (String officialMailId : mailToReportinHeadMap.keySet()) {
					String mailValue = mailToReportinHeadMap.get(officialMailId);
					if (mailValue.length() > 0) {
						if (officialMailId == null || officialMailId.length() == 0) {
							officialMailId = "kapil.dalal@daffodilsw.com";
						}
						String[] toMailIds = { officialMailId };
						mailValue = valueToMail + mailValue + "</table>";
						mailValue += "<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
						HrisHelper.sendMails(toMailIds, mailValue, "Monthly Attendance of Team");
					}
				}
			}
			// add task queue
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeMonthlyShortAttendanceReportBusinessLogic >> sendMonthlyShortAttendanceReport >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private void getMailValueForTL(HashMap<String, String> mailToReportinHeadMap, JSONObject attendanceDetailsObject) {
		Object reportingToEmail = attendanceDetailsObject.opt("employeeid.reportingtoid.officialemailid");
		Object employeeName = attendanceDetailsObject.opt("employeeid.name");
		// Object extraworkingdays = attendanceDetailsObject.opt("extraworkingdays");

		Object present = attendanceDetailsObject.opt("presentdays");
		Object absents = attendanceDetailsObject.opt("absents");
		Object extraworkingdays = attendanceDetailsObject.opt("extraworkingdays");

		Object carryextraworkingday = attendanceDetailsObject.opt("carryextraworkingday");
		Object leaves = attendanceDetailsObject.opt("leaves");
		Object payableleaves = attendanceDetailsObject.opt("payableleaves");
		Object actualnonworkingdays = attendanceDetailsObject.opt("actualnonworkingdays");
		String mailValue = "";
		if (mailToReportinHeadMap.containsKey(reportingToEmail)) {
			mailValue = mailToReportinHeadMap.get(reportingToEmail);
		}
		mailValue += "<tr><td>" + employeeName + "</td><td>" + present + "</td><td>" + absents + "</td><td>" + extraworkingdays + "</td><td>" + carryextraworkingday + "</td><td>" + leaves + "</td><td>" + payableleaves + "</td><td>" + actualnonworkingdays + "</td></tr>";
		mailToReportinHeadMap.put("" + reportingToEmail, mailValue);
	}

	private JSONArray getMonthlyAttendanceArray(long yearId, int monthId) throws JSONException {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		// columnArray.put("employeeid.officialemailid");
		columnArray.put("employeeid.reportingtoid");
		columnArray.put("employeeid.reportingtoid.officialemailid");
		columnArray.put("employeeid.leavepolicyid.isextraworkingdayencashable");
		columnArray.put("presentdays");
		columnArray.put("absents");
		columnArray.put("leaves");
		columnArray.put("extraworkingdays");
		columnArray.put("totalworkingdays");
		columnArray.put("carryextraworkingday");
		columnArray.put("holidays");
		columnArray.put("nonworkingdays");
		columnArray.put("nonpayableleaves");
		columnArray.put("nonpaidabsents");
		columnArray.put("actualnonworkingdays");
		columnArray.put("monthlyclosingbalance");
		columnArray.put("paidabsents");
		columnArray.put("payableleaves");
		columnArray.put("yearid");
		columnArray.put("monthid");
		columnArray.put("nonpaidabsents");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "yearid = " + yearId + " and  monthid = " + monthId);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("employeemonthlyattendance");
		return rows;
	}
}