package com.applane.resourceoriented.hris.reports;

import java.text.DecimalFormat;
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
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.google.apphosting.api.DeadlineExceededException;

public class MonthlyAverageTimeInOfficeBusinessLogic implements QueryJob {

	public void onQuery(Query query) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		JSONArray employeeArray = result.getRecords();
		Query query = result.getQuery();
		JSONObject parameters = query.getParameters();
		String date = "" + parameters.opt("date");
		sendMonthlyShortAttendanceReportFromTaskQueue(employeeArray, date);
	}

	public void sendMonthlyShortAttendanceReportFromTaskQueue(JSONArray employeeArray, String date) {
		try {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date todayDate = Translator.dateValue(TODAY_DATE);
			boolean enterInIfCondition = true;
			if (enterInIfCondition) {

				JSONArray schedules = getSchedules();
				if (schedules.length() > 0) {
					HashMap<Integer, String> scheduleHoursMAP = new HashMap<Integer, String>();
					getScheduleHoursMAP(schedules, scheduleHoursMAP);
					Calendar cal = Calendar.getInstance();
					if (date != null && date.length() > 0) {
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

					// JSONArray employeeArray = getEmployeeArray(TODAY_DATE);

					// String logo = "";
					HashMap<Integer, Object[]> monthlyAttendanceAverageDetails = new HashMap<Integer, Object[]>();
					HashMap<Integer, Object[]> employeeDetailsMap = new HashMap<Integer, Object[]>();
					for (int employeeCounter = 0; employeeCounter < employeeArray.length(); employeeCounter++) {
						Object employeeId = employeeArray.getJSONObject(employeeCounter).opt(Updates.KEY);
						Object scheduleId = employeeArray.getJSONObject(employeeCounter).opt("employeescheduleid");
						String name = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("name"));
						String employeeCode = Translator.stringValue(employeeArray.getJSONObject(employeeCounter).opt("employeecode"));
						JSONArray employeeMonthlyAttendance = getEmployeeMonthlyAttendance(employeeId, firstDateInString, lastDateInString);
						String timeShouleBeInOffice = scheduleId == null ? null : scheduleHoursMAP.get(Translator.integerValue(scheduleId));
						Object[] details = new Object[2];
						details[0] = name;
						details[1] = employeeCode;
						employeeDetailsMap.put(Translator.integerValue(employeeId), details);
						if (timeShouleBeInOffice != null) {
							getShortAttendanceValue(name, employeeMonthlyAttendance, timeShouleBeInOffice, monthlyAttendanceAverageDetails, employeeId);
						}
					}
					JSONArray detailsArray = new JSONArray();
					JSONObject objectDetails = null;
					DecimalFormat df = new DecimalFormat("#");
					for (Integer employeeId : monthlyAttendanceAverageDetails.keySet()) {
						Object[] details = monthlyAttendanceAverageDetails.get(employeeId);
						Object[] employeeDetails = employeeDetailsMap.get(employeeId);

						// details[0] = totalHours; total in month hrs
						// details[1] = totalMinuts; total in month minuts
						// details[2] = totalHoursInOffice; office hrs
						// details[3] = totalMinutsInOffice; office minuts
						// details[4] = totalPresent;
						// details[5] = hoursPerDay; avg hrs
						// details[6] = minutsPerDay; avg minuts
						if (details != null && employeeDetails != null) {
							objectDetails = new JSONObject();
							objectDetails.put("employeeName", employeeDetails[0]);
							objectDetails.put("employeeCode", employeeDetails[1]);
							objectDetails.put("monthName", monthName);
							objectDetails.put("totalHoursInMonth", details[0] == null ? "" : df.format(details[0]));
							objectDetails.put("totalMinutsInMonth", details[1] == null ? "" : df.format(details[1]));
							objectDetails.put("totalHoursInOffice", details[2] == null ? "" : df.format(details[2]));
							objectDetails.put("totalMinutsInOffice", details[3] == null ? "" : df.format(details[3]));
							objectDetails.put("totalPresent", details[4] == null ? "" : df.format(details[4]));
							objectDetails.put("avgHoursPerDay", details[5] == null ? "" : df.format(details[5]));
							objectDetails.put("avgMinutsPerDay", details[6] == null ? "" : df.format(details[6]));
							detailsArray.put(objectDetails);
						}
					}
					employeeArray.put(new JSONObject().put("monthlyAverageTime", detailsArray));
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("MonthlyAverageTimeInOfficeBusinessLogic >> sendMonthlyShortAttendanceReport >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private void getShortAttendanceValue(String name, JSONArray employeeMonthlyAttendance, String timeShouleBeInOffice, HashMap<Integer, Object[]> monthlyAttendanceAverageDetails, Object employeeId) throws Exception {
		double averageInOffice = 0d;
		double totalHours = 0d;
		double totalMinuts = 0d;
		double totalPresent = 0d;
		double totalHoursInOffice = 0d;
		double totalMinutsInOffice = 0d;

		for (int attendanceCounter = 0; attendanceCounter < employeeMonthlyAttendance.length(); attendanceCounter++) {
			// String attendanceDate = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("attendancedate"));
			// String inTime = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("firstintime"));
			// String outTime = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("lastouttime"));
			// String status = Translator.stringValue(employeeMonthlyAttendance.getJSONObject(attendanceCounter).opt("punchingtypeid.name"));
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
			}
		}

		double totalMinutsForAverageCalculationPerDay = (float) (((float) (totalHours * 60) + totalMinuts) / totalPresent);

		totalHours += (int) (totalMinuts / 60);
		totalMinuts = totalMinuts % 60;

		averageInOffice = (totalMinutsForAverageCalculationPerDay / 60);
		int hoursPerDay = (int) averageInOffice;
		int minutsPerDay = (int) ((float) (averageInOffice - hoursPerDay) * 60);

		totalHoursInOffice += (int) (totalMinutsInOffice / 60);
		totalMinutsInOffice = totalMinutsInOffice % 60;

		Object[] details = new Object[7];
		details[0] = totalHours;
		details[1] = totalMinuts;
		details[2] = totalHoursInOffice;
		details[3] = totalMinutsInOffice;
		details[4] = totalPresent;
		details[5] = hoursPerDay;
		details[6] = minutsPerDay;

		monthlyAttendanceAverageDetails.put(Translator.integerValue(employeeId), details);

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
			int key = Translator.integerValue(schedules.getJSONObject(counter).opt("__key__"));
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