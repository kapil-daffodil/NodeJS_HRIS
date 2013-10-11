package com.applane.resourceoriented.hris.reports;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class ProbationEndingNotification {

	public void sendNotificationToProfitCenterWise() {
		try {
			sendNotificationProfitCenterWiseInvoke(null);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("ProbationEndingNotification Exception >> " + trace);
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "ProbationEndingNotification Exception");
		}
	}

	public void sendNotificationProfitCenterWiseInvoke(Object date) throws Exception {
		if (date == null) {
			date = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		}
		String todayDate = "" + date;
		Date monthFirstDate = DataTypeUtilities.getFirstDayOfMonth(Translator.dateValue(date));
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(Translator.dateValue(date));
		String monthFirstDateString = Translator.getDateInString(monthFirstDate);
		String monthLastDateString = Translator.getDateInString(monthLastDate);
		String employeeIds = getEmployeeIdsIfProbationAdded(monthFirstDateString, monthLastDateString, todayDate);
		if (employeeIds != null && employeeIds.length() > 0) {
			JSONArray employeeArray = getEmployeeDetails(employeeIds);
			HashMap<Integer, String> employeeValueMap = new HashMap<Integer, String>();
			for (int counter = 0; counter < employeeArray.length(); counter++) {
				JSONObject employeeDetails = employeeArray.getJSONObject(counter);
				JSONArray profitcenters = employeeDetails.optJSONArray("profitcenterid");
				Object employeeName = employeeDetails.opt("name");
				Object departmentName = employeeDetails.opt("departmentid.name");
				Object employeeCode = employeeDetails.opt("employeecode");
				Object joiningDate = employeeDetails.opt("joiningdate");
				Object profitcenterIdObject = null;
				if (profitcenters != null && profitcenters.length() > 0) {
					profitcenterIdObject = profitcenters.optJSONObject(0).opt("__key__");
				}
				if (profitcenterIdObject != null) {
					int profitcenterId = Translator.integerValue(profitcenterIdObject);
					String value = "<tr><td>&nbsp;" + employeeCode + "</td><td>&nbsp;" + employeeName + "</td><td>&nbsp;" + departmentName + "</td><td>&nbsp;" + joiningDate + "</td></tr>";
					if (employeeValueMap.containsKey(profitcenterId)) {
						value += employeeValueMap.get(profitcenterId);
					}
					employeeValueMap.put(profitcenterId, value);
				}
			}
			if (employeeValueMap != null && employeeValueMap.size() > 0) {
				JSONArray mailIdDetailProfitCenterWise = EmployeeSalarySheetReport.getMailIdProfitCenterWise("");
				for (int counter = 0; counter < mailIdDetailProfitCenterWise.length(); counter++) {
					int profitCenterId = Translator.integerValue(mailIdDetailProfitCenterWise.getJSONObject(counter).opt(Updates.KEY));

					Object profitCenterName = mailIdDetailProfitCenterWise.getJSONObject(counter).opt("name");
					Object mailIdArray = mailIdDetailProfitCenterWise.getJSONObject(counter).opt("emails");
					String[] emailIds = null;
					if (mailIdArray != null && mailIdArray instanceof JSONArray) {
						emailIds = new String[((JSONArray) mailIdArray).length()];
						for (int counterEmail = 0; counterEmail < ((JSONArray) mailIdArray).length(); counterEmail++) {
							emailIds[counterEmail] = Translator.stringValue(((JSONArray) mailIdArray).getJSONObject(counterEmail).opt("officialemailid"));
						}
						if (employeeValueMap.containsKey(profitCenterId)) {
							String valueToMail = "<table border='1' cellspacing='0'><tr bgcolor='99CCCC'><td>Employee Code</td><td>Name</td><td>Department Name</td><td>Joining Date</td></tr>";
							valueToMail += employeeValueMap.get(profitCenterId);
							valueToMail += "</table>";
							String title = "Probation Period Over in " + profitCenterName + " Business Unit";
							HrisHelper.sendMails(emailIds, valueToMail, title);
						}
					}
				}
			}
		}
	}

	private JSONArray getEmployeeDetails(String employeeIds) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("name");
		array.put("employeecode");
		array.put("departmentid.name");
		array.put("joiningdate");
		array.put(new JSONObject().put("profitcenterid", new JSONArray().put("__key__")));
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ IN(" + employeeIds + ")");
		JSONObject attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_employees");
		return rows;
	}

	private String getEmployeeIdsIfProbationAdded(String monthFirstDateString, String monthLastDateString, String todayDateString) throws Exception {
		String ids = "";
		Date todayDate = Translator.dateValue(todayDateString);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		JSONArray array = new JSONArray();
		array.put("employeeid");
		array.put("employeeid.joiningdate");
		array.put("probation_period");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid.employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " AND employeeid.joiningdate<='" + monthLastDateString + "' AND probation_period !=null");
		query.put(Data.Query.OPTION_FILTERS, "employeeid.employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " AND employeeid.relievingdate >= '" + monthFirstDateString + "' AND probation_period !=null");
		JSONObject attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_employees_additional_information");

		if (rows != null && rows.length() > 0) {
			Calendar cal = Calendar.getInstance();
			for (int counter = 0; counter < rows.length(); counter++) {
				Object probationPeriodObject = rows.getJSONObject(counter).opt("probation_period");
				int probationPeriod = -1;
				if (probationPeriodObject instanceof Double) {
					probationPeriod = (int) (Translator.doubleValue(probationPeriodObject) == 0.0 ? -1 : (int) Translator.doubleValue(probationPeriodObject));
				} else if (probationPeriodObject instanceof Integer) {
					probationPeriod = Translator.integerValue(probationPeriodObject) == 0 ? -1 : Translator.integerValue(probationPeriodObject);
				}
				Object joiningDateObject = rows.getJSONObject(counter).opt("employeeid.joiningdate");
				if (joiningDateObject != null && probationPeriod != -1) {
					Date joiningDate = Translator.dateValue(joiningDateObject);
					cal.setTime(joiningDate);
					cal.add(Calendar.MONTH, probationPeriod);
					joiningDate = cal.getTime();
					String todayDateStringTemp = Translator.getDateInString(joiningDate);
					joiningDate = Translator.dateValue(todayDateStringTemp);
					if (joiningDate.equals(todayDate)) {
						Object employeeId = rows.getJSONObject(counter).opt("employeeid");
						if (employeeId != null) {
							if (ids.length() > 0) {
								ids += ",";
							}
							ids += "" + employeeId;
						}
					}
				}
			}
		}
		LogUtility.writeError("ids >> " + ids);
		return ids;
	}
}