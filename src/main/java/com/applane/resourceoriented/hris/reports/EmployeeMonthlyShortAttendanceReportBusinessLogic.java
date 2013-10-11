package com.applane.resourceoriented.hris.reports;

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
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class EmployeeMonthlyShortAttendanceReportBusinessLogic {
	public void sendMonthlyShortAttendanceReport(Object date) {
		sendMonthlyShortAttendanceReportFromTaskQueue(true, date);
	}

	public void sendMonthlyShortAttendanceReportFromTaskQueue(boolean fromInvoke, Object date) {
		try {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date todayDate = Translator.dateValue(TODAY_DATE);
			Date firstDayOfCurrentMonth = DataTypeUtilities.getFirstDayOfMonth(todayDate);
			boolean enterInIfCondition = true;
			if (!fromInvoke) {
				enterInIfCondition = todayDate.equals(firstDayOfCurrentMonth);
			}
			if (enterInIfCondition) {
				JSONArray mailConfigurationSetup = new MorningSchdularHRIS().getMailConfigurationSetup();
				List<Integer> disableMonthlyShortAttendanceMailBranchWise = new ArrayList<Integer>();
				String logo = "";
				if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
					Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
					if (organizationlogo != null) {
						logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
					}
					Object mailIdArray = mailConfigurationSetup.getJSONObject(0).opt("disable_d_m_s_att_m_b_w");
					if (mailIdArray != null && mailIdArray instanceof JSONArray) {
						for (int counterEmail = 0; counterEmail < ((JSONArray) mailIdArray).length(); counterEmail++) {
							disableMonthlyShortAttendanceMailBranchWise.add(Translator.integerValue(((JSONArray) mailIdArray).getJSONObject(counterEmail).opt(Updates.KEY)));
						}
					}
				}

				JSONArray schedules = getSchedules();
				if (schedules.length() > 0) {
					HashMap<Integer, String> scheduleHoursMAP = new HashMap<Integer, String>();
					getScheduleHoursMAP(schedules, scheduleHoursMAP);
					Calendar cal = Calendar.getInstance();
					if (date != null) {
						cal.setTime(Translator.dateValue(date));
					} else {
						cal.setTime(todayDate);
						cal.add(Calendar.MONTH, -1);
					}
					int monthId = cal.get(Calendar.MONTH) + 1;
					long yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));

					String monthName = EmployeeSalarySheetReport.getMonthName(monthId);
					String yearName = EmployeeSalarySheetReport.getYearName(yearId);
					Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
					Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
					String firstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
					String lastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
					JSONArray employeeArray = getEmployeeArray(TODAY_DATE);
					HashMap<String, String> mailToReportinHeadMap = new HashMap<String, String>();
					HashMap<Integer, String[]> hrEmails = new HashMap<Integer, String[]>();
					HashMap<Integer, String> profitCenterWiseRecords = new HashMap<Integer, String>();
					JSONArray profitCenters = EmployeeSalarySheetReport.getMailIdProfitCenterWise("");
					for (int counter = 0; counter < profitCenters.length(); counter++) {
						int profitCenterId = Translator.integerValue(profitCenters.getJSONObject(counter).opt(Updates.KEY));
						Object mailIdArray = profitCenters.getJSONObject(counter).opt("emails");
						String[] emailIds = null;
						if (mailIdArray != null && mailIdArray instanceof JSONArray) {
							emailIds = new String[((JSONArray) mailIdArray).length()];
							for (int counterEmail = 0; counterEmail < ((JSONArray) mailIdArray).length(); counterEmail++) {
								emailIds[counterEmail] = Translator.stringValue(((JSONArray) mailIdArray).getJSONObject(counterEmail).opt("officialemailid"));
							}
							hrEmails.put(profitCenterId, emailIds);
						}
					}
					for (int employeeCounter = 0; employeeCounter < employeeArray.length(); employeeCounter++) {
						Object employeeId = employeeArray.getJSONObject(employeeCounter).opt(Updates.KEY);
						Object scheduleId = employeeArray.getJSONObject(employeeCounter).opt("employeescheduleid");
						String officialMailId = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("reportingtoid.officialemailid"));
						int branchId = Translator.integerValue(employeeArray.getJSONObject(employeeCounter).opt("branchid"));
						Object profitCenterIdArray = employeeArray.getJSONObject(employeeCounter).opt("profitcenterid");
						// String currentLocation = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("subbranchid.name"));
						// String designation = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("designationid.name"));
						String name = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("name"));
						JSONArray employeeMonthlyAttendance = getEmployeeMonthlyAttendance(employeeId, firstDateInString, lastDateInString);
						String timeShouleBeInOffice = scheduleId == null ? null : scheduleHoursMAP.get(Translator.integerValue(scheduleId));
						if (timeShouleBeInOffice != null) {
							String employeeAttendanceValue = "";
							if (mailToReportinHeadMap.containsKey(officialMailId)) {
								employeeAttendanceValue = mailToReportinHeadMap.get(officialMailId);
							}
							String employeeShortAttendanceDetails = "";
							if (!disableMonthlyShortAttendanceMailBranchWise.contains(branchId)) {
								employeeShortAttendanceDetails = getShortAttendanceValue(name, employeeMonthlyAttendance, timeShouleBeInOffice);
								mailToReportinHeadMap.put(officialMailId, employeeAttendanceValue + employeeShortAttendanceDetails);
							}
							if (profitCenterIdArray != null && profitCenterIdArray instanceof JSONArray) {
								for (int counterEmail = 0; counterEmail < ((JSONArray) profitCenterIdArray).length(); counterEmail++) {
									int profitCenterId = Translator.integerValue(((JSONArray) profitCenterIdArray).getJSONObject(counterEmail).opt(Updates.KEY));
									String value = "";
									if (profitCenterWiseRecords.containsKey(profitCenterId)) {
										value = profitCenterWiseRecords.get(profitCenterId);
									}
									value += employeeShortAttendanceDetails;
									profitCenterWiseRecords.put(profitCenterId, value);
								}
							}
						}
					}
					String valueToMail = "";
					valueToMail += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
					valueToMail += "<hr></hr><br></br>";
					valueToMail += "<table border='1' cellspacing='0'>";
					for (String officialMailId : mailToReportinHeadMap.keySet()) {
						String mailValue = mailToReportinHeadMap.get(officialMailId);
						if (mailValue.length() > 0) {
							if (officialMailId == null || officialMailId.length() == 0) {
								officialMailId = "kapil.dalal@daffodilsw.com";
							}
							String[] toMailIds = { officialMailId };
							mailValue = valueToMail + mailValue + "</table>";
							mailValue += "<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
							HrisHelper.sendMails(toMailIds, mailValue, "Monthly Short Attendance of Team");
						}
					}
					for (Integer profitCenterId : hrEmails.keySet()) {
						String[] mailIds = hrEmails.get(profitCenterId);
						String mailValue = profitCenterWiseRecords.get(profitCenterId);
						if (mailValue != null && mailValue.length() > 0) {
							if (mailIds == null || mailIds.length == 0) {
								mailIds = new String[] { "kapil.dalal@daffodilsw.com" };
							}
							mailValue = valueToMail + mailValue + "</table>";
							mailValue += "<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
							PunchingUtility.sendMailsAccToProfitCenters(new String[] { ApplicationConstants.USER }, mailValue, "Monthly Short Attendance of Team", mailIds);
						}
					}
				}
			}
			// add task queue
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeMonthlyShortAttendanceReportBusinessLogic >> sendMonthlyShortAttendanceReport >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private String getShortAttendanceValue(String name, JSONArray employeeMonthlyAttendance, String timeShouleBeInOffice) throws Exception {
		String employeeAttendanceValue = "";
		String employeeAttendanceHeaderValue = "";
		double averageInOffice = 0d;
		double totalHours = 0d;
		double totalMinuts = 0d;
		double totalPresent = 0d;
		double totalHoursInOffice = 0d;
		double totalMinutsInOffice = 0d;

		for (int attendanceCounter = 0; attendanceCounter < employeeMonthlyAttendance.length(); attendanceCounter++) {
			String attendanceDate = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("attendancedate"));
			String inTime = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("firstintime"));
			String outTime = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("lastouttime"));
			String status = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("punchingtypeid.name"));
			String actualTimeInOffice = employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("totaltimeinoffice") == null ? timeShouleBeInOffice : Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("totaltimeinoffice"));
			int attendanceType = Translator.integerValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("attendancetypeid"));
			// 4Hr. 11min
			// 11Hr. 1min
			if (actualTimeInOffice != null) {
				String[] timeArray = { "00:00" };
				if (actualTimeInOffice.contains(" ")) {
					timeArray = actualTimeInOffice.split(" ");
				} else {
					timeArray = actualTimeInOffice.split(":");
				}
				String[] hoursMinutsInOffice = timeShouleBeInOffice.split(":");
				double hourrShouldBeInOffice = Integer.parseInt(hoursMinutsInOffice[0]);
				double minutsShouldBeInOffice = Integer.parseInt(hoursMinutsInOffice[1]);

				if (attendanceType == HRISApplicationConstants.ATTENDANCE_PRESENT) {
					totalPresent += 1;
				} else {
					totalPresent += 0.50;
					hourrShouldBeInOffice = hourrShouldBeInOffice / 2;
					minutsShouldBeInOffice = minutsShouldBeInOffice / 2;
				}
				totalHoursInOffice += hourrShouldBeInOffice;
				totalMinutsInOffice += minutsShouldBeInOffice;

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
				totalHours += Double.parseDouble(hours);
				totalMinuts += Double.parseDouble(minuts);
				if ((hourrShouldBeInOffice > Double.parseDouble(hours)) || (hourrShouldBeInOffice == Double.parseDouble(hours) && minutsShouldBeInOffice > Double.parseDouble(minuts))) {
					employeeAttendanceValue += ("<tr><td>&nbsp;" + attendanceDate + "</td><td>&nbsp;" + inTime + "</td><td>&nbsp;" + outTime + "</td><td>&nbsp;" + actualTimeInOffice + "</td><td>&nbsp;" + status + "</td></tr>");
				}
			}
		}

		double totalMinutsForAverageCalculationPerDay = (float) (((float) (totalHours * 60) + totalMinuts) / totalPresent);

		totalHours += (float) (totalMinuts / 60);
		totalMinuts = totalMinuts % 60;

		averageInOffice = (totalMinutsForAverageCalculationPerDay / 60);
		int hoursPerDay = (int) averageInOffice;
		int minutsPerDay = (int) ((float) (averageInOffice - hoursPerDay) * 60);

		totalHoursInOffice += (int) (totalMinutsInOffice / 60);
		totalMinutsInOffice = totalMinutsInOffice % 60;

		employeeAttendanceHeaderValue += ("<tr><td colspan='5' bgcolor='12FF01'>&nbsp;" + name + "</td></tr>");
		employeeAttendanceHeaderValue += ("<tr><td bgcolor='99CCCC' colspan='2'>Hours Worked In Month</td><td bgcolor='99CCCC'>Present Days</td><td bgcolor='99CCCC' colspan='2'>Avg. Hours</td></tr>");
		employeeAttendanceHeaderValue += ("<tr><td colspan='2'>&nbsp;" + totalHours + " hr. " + totalMinuts + " mn. / " + totalHoursInOffice + " hr. " + totalMinutsInOffice + " mn." + "</td><td>&nbsp;" + totalPresent + "</td><td colspan='2'>&nbsp;" + hoursPerDay + " hr. " + minutsPerDay + " mn. / Day" + "</td></tr>");
		employeeAttendanceHeaderValue += ("<tr><td bgcolor='99CCCC'>Date</td><td bgcolor='99CCCC'>In Time</td><td bgcolor='99CCCC'>Out Time</td><td bgcolor='99CCCC'>Total Time In Office</td><td bgcolor='99CCCC'>Status</td></tr>");
		employeeAttendanceValue += ("<tr><td colspan='5'>&nbsp;</td></tr>");
		return (employeeAttendanceHeaderValue + employeeAttendanceValue);
	}

	private JSONArray getEmployeeMonthlyAttendance(Object employeeId, String monthFirstDate, String monthLastDate) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("attendancetypeid");
		columnArray.put("attendancedate");
		columnArray.put("firstintime");
		columnArray.put("lastouttime");
		columnArray.put("punchingtypeid.name");
		columnArray.put("totaltimeinoffice");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate >= '" + monthFirstDate + "' and attendancedate <= '" + monthLastDate + "' and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_PRESENT + ", " + HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE + ", " + HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE + ")");
		JSONObject attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray monthlyAttendanceArray = attendanceObject.getJSONArray("employeeattendance");
		return monthlyAttendanceArray;
	}

	public void getScheduleHoursMAP(JSONArray schedules, HashMap<Integer, String> scheduleHoursMAP) throws Exception {
		for (int counter = 0; counter < schedules.length(); counter++) {
			int workingDays = 0;
			int key = Translator.integerValue(schedules.getJSONObject(counter).opt(Updates.KEY));
			Object timeShouldBeInOfficeObject = schedules.getJSONObject(counter).opt("time_should_be_in_office");
			if (timeShouldBeInOfficeObject != null) {
				scheduleHoursMAP.put(key, "" + timeShouldBeInOfficeObject);
			} else {
				if (Translator.booleanValue(schedules.getJSONObject(counter).opt("monday"))) {
					workingDays++;
				}
				if (Translator.booleanValue(schedules.getJSONObject(counter).opt("tuesday"))) {
					workingDays++;
				}
				if (Translator.booleanValue(schedules.getJSONObject(counter).opt("wednesday"))) {
					workingDays++;
				}
				if (Translator.booleanValue(schedules.getJSONObject(counter).opt("thursday"))) {
					workingDays++;
				}
				if (Translator.booleanValue(schedules.getJSONObject(counter).opt("friday"))) {
					workingDays++;
				}
				if (Translator.booleanValue(schedules.getJSONObject(counter).opt("saturday"))) {
					workingDays++;
				}
				if (Translator.booleanValue(schedules.getJSONObject(counter).opt("sunday"))) {
					workingDays++;
				}
				if (workingDays == 5) {
					scheduleHoursMAP.put(key, "09:00");
				}
				if (workingDays == 6) {
					scheduleHoursMAP.put(key, "08:00");
				}
			}
		}
	}

	public static JSONArray getEmployeeArray(String firstDateInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("employeecode");
		columnArray.put("branchid");
		columnArray.put("reportingtoid.officialemailid");
		columnArray.put("employeescheduleid");
		columnArray.put("subbranchid.name");
		columnArray.put("designationid.name");

		JSONArray profitCenterIds = new JSONArray();
		JSONObject offOb = new JSONObject();
		profitCenterIds.put(Updates.KEY);
		offOb.put("profitcenterid", profitCenterIds);
		columnArray.put(offOb);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and relievingdate >= '" + firstDateInString + "'");
		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public JSONArray getSchedules() throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("time_should_be_in_office");
		columnArray.put("monday");
		columnArray.put("tuesday");
		columnArray.put("wednesday");
		columnArray.put("thursday");
		columnArray.put("friday");
		columnArray.put("saturday");
		columnArray.put("sunday");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_schedules");
		return rows;
	}
}