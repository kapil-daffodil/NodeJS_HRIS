package com.applane.resourceoriented.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServlet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.appengine.api.utils.SystemProperty;

public class EmployeePunchingCron extends HttpServlet {

	private static final long serialVersionUID = -9124876884033910069L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	protected void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
		System.err.println("doGet() Method call..");
		doPost(req, resp);
	}

	protected void doPost(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
		System.err.println("EmployeePunchingCron --->doPost method call....");
		try {
			CurrentState.setCurrentVariable(CurrentSession.CURRENT_TIME_ZONE, -19800000);
			String serverName = SystemProperty.applicationId.get();
			System.err.println("serverName is : >> " + serverName);
			if (serverName.equals(HRISApplicationConstants.ServerName.APPLANE_CRM_HRD)) {
				punchingInOrganizations();
			}
		} catch (Exception e) {
			System.err.println("Error[" + e + "]");
		}
	}

	public void punchingInOrganizations() {
		System.err.println("punchingForAllOrganizations Method call.....");
		try {
			System.err.println("In Try block....");
			Object organizationId = 5;
			Object organizationName = "Daffodil";
			Object applicationId = 777;
			Object applicationName = "hris_services";

			setSession(organizationId, organizationName, applicationId, applicationName);

			String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			System.err.println("currentDate is : >> " + currentDate);

			String backDate = getBackDate(currentDate);

			// String backDate = "2012-02-11";

			System.err.println("backDate is : >> " + backDate);

			JSONArray employeeAttendanceArray = getEmployeeAttedanceRecords(backDate);
			System.err.println("employeeAttendanceArray is : >> " + employeeAttendanceArray);
			int count = (employeeAttendanceArray == null || employeeAttendanceArray.length() == 0) ? 0 : employeeAttendanceArray.length();
			System.err.println("Count is : >> " + count);
			if (count > 0) {
				System.err.println("count is greater than zero...");
				for (int counter = 0; counter < count; counter++) {
					System.err.println("in loop....");

					Object employeeAttendanceId = employeeAttendanceArray.getJSONObject(counter).opt("__key__");
					System.err.println("employeeAttendanceId is : >> " + employeeAttendanceId);

					Object employeeId = employeeAttendanceArray.getJSONObject(counter).opt("employeeid");
					System.err.println("employeeId is : >> " + employeeId);

					Object attendanceDate = employeeAttendanceArray.getJSONObject(counter).opt("attendancedate");
					System.err.println("atendanceDate : >> " + attendanceDate);

					int attendanceTypeId = employeeAttendanceArray.getJSONObject(counter).optInt("attendancetypeid");
					System.err.println("attendanceTypeId is : >> " + attendanceTypeId);

					Object firstInTime = employeeAttendanceArray.getJSONObject(counter).opt("firstintime") == null ? "" : employeeAttendanceArray.getJSONObject(counter).opt("firstintime");
					System.err.println("firstInTime is : >> " + firstInTime);

					Object lastOutTime = employeeAttendanceArray.getJSONObject(counter).opt("lastouttime") == null ? "" : employeeAttendanceArray.getJSONObject(counter).opt("lastouttime");
					System.err.println("lastOutTime is : >> " + lastOutTime);

					Object scheduleId = employeeAttendanceArray.getJSONObject(counter).opt("employeeid.employeescheduleid");
					System.err.println("scheduleId is : >> " + scheduleId);

					Object holidayCalendaId = employeeAttendanceArray.getJSONObject(counter).opt("employeeid.holidaycalendarid");
					System.err.println("holidayCalendarId is : >> " + holidayCalendaId);

					Object firstInDateObject = employeeAttendanceArray.getJSONObject(counter).opt("firstindatetime");
					System.err.println("firstInDateObject is : >> " + firstInDateObject);
					Date firstInDate = null;
					if (firstInDateObject != null) {
						firstInDate = DataTypeUtilities.checkDateFormat(firstInDateObject);
						System.err.println("firstInDate is : >> " + firstInDate);
					}
					Object lastOutDateObject = employeeAttendanceArray.getJSONObject(counter).opt("lastoutdatetime");
					System.err.println("lastOutDateObject is : >> " + lastOutDateObject);

					Date lastOutDate = null;
					if (lastOutDateObject != null) {
						lastOutDate = DataTypeUtilities.checkDateFormat(lastOutDateObject);
						System.err.println("lastOutDate is : >> " + lastOutDate);
					}

					// boolean isFind = findPunchingRecords(attendanceDate, employeeId);
					// System.err.println("isFind va;ue is : >> " + isFind);

					boolean isFind = true;
					if (!isFind && attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
						System.err.println("Come for update attendance type....");
						updateAttendanceType(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_UNKNOWN);
					} else if (firstInDate != null && lastOutDate != null) {
						System.err.println("firstInDate and lastOutDate not equal to null...");
						String totalTimeInOffice = DataTypeUtilities.showDifference(firstInDate, lastOutDate);
						System.err.println("totalTimeInOffice is : >>> " + totalTimeInOffice);

						if (scheduleId != null) {
							System.err.println("scheduleId not equal to null....");
							String weekDay = DataTypeUtilities.getWeekDay(attendanceDate);
							System.err.println("weekDay is : >> " + weekDay);

							int weekNo = DataTypeUtilities.getWeekNo(attendanceDate);
							System.err.println("weekNo is : >> " + weekNo);

							int weekDayNo = DataTypeUtilities.getWeekDayNo(attendanceDate);
							System.err.println("weekDayNo is : >> " + weekDayNo);

							boolean isWeekDay = getWeekDayValueFromSchedule(weekDay, scheduleId);
							System.err.println("isWeekDay value is : >> " + isWeekDay);

							boolean isMonthlyOff = findMonthlyOff(scheduleId, weekNo, weekDayNo);
							System.err.println("isMonthlyOff is : >> " + isMonthlyOff);

							boolean isHoliday = true;
							if (holidayCalendaId != null) {
								System.err.println("holidayCalendaId not equal to null....");
								isHoliday = findHoliday(holidayCalendaId, attendanceDate);
							}

							if (!isWeekDay || !isMonthlyOff || !isHoliday) {
								System.err.println("isWeekDay is false....");
								long miliSeconds = DataTypeUtilities.showTimeDifferenceInMiliSeconds(firstInDate, lastOutDate);
								System.err.println("miliSeconds is : >> " + miliSeconds);

								if (miliSeconds > HRISApplicationConstants.FIVE_HOURS) {
									System.err.println("miliSeconds is greater than zero...");
									updateAttendanceType(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY);
								} else {
									System.err.println("miliSeconds is less than five hours");
									updateAttendanceType(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF);
								}
							}
						} else {
							System.err.println("ScheduleId not found for this employeee [" + employeeId + "]");
						}
						// update totalTimeInOffice
						updateTotalTimeInOffice(employeeAttendanceId, totalTimeInOffice);
					}

					String totalBreakingTime = calculateTotalBreakTime(attendanceDate, employeeId);
					System.err.println("totalBreakingTime is : >> " + totalBreakingTime);

					updateTotalBreakingTime(employeeAttendanceId, totalBreakingTime);
				}
			}
		} catch (Exception e) {
			String traces = ExceptionUtils.getExceptionTraceMessage("EmployeePunchingCron", e);
			LogUtility.writeError(traces);
			ApplaneMail mail = new ApplaneMail();
			StringBuilder builder = new StringBuilder();
			builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
			builder.append("<br><br><b>Exception traces are given below :</b><br><br>").append(traces);
			mail.setMessage("Employee Final Punching Cron Failed", builder.toString(), true);
			try {
				mail.setTo("ajaypal.singh@daffodilsw.com");
				mail.sendAsAdmin();
			} catch (ApplaneMailException e1) {
				System.err.println("Exception Come while send mail in Employee Punching Cron.");
			}
		}
	}

	private boolean findHoliday(Object holidayCalendarId, Object attendanceDate) throws JSONException {
		System.err.println("findHoliday Method call....");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate = '" + attendanceDate + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_holidays");
		System.err.println("array is : >> " + array);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		System.err.println("arrayCount is : >> " + arrayCount);
		if (arrayCount > 0) {
			System.err.println("arrayCount is greater than zero..");
			return false;
		} else {
			System.err.println("arrayCount is less than zero....");
			return true;
		}
	}

	public static boolean findMonthlyOff(Object scheduleId, int weekNo, int weekDayNo) throws JSONException {
		System.err.println("getMonthlyOffRecords Method call...");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_monthlyoff");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.FILTERS, "scheduleid = " + scheduleId + " and weeknoid = " + weekNo + " and weekdayid = " + weekDayNo);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_monthlyoff");
		System.err.println("array is : >> " + array);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		System.err.println("arrayCount is : >> " + arrayCount);
		if (arrayCount > 0) {
			return false;
		} else {
			return true;
		}
	}

	private boolean findPunchingRecords(Object attendanceDate, Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeecardpunchdata");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "inoutdate = '" + attendanceDate + "' and employeeid = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray punchingArray = object.getJSONArray("employeecardpunchdata");
		System.err.println("punchingArray is : >> " + punchingArray);
		int punchingArrayCount = (punchingArray == null || punchingArray.length() == 0) ? 0 : punchingArray.length();
		System.err.println("punchingArrayCount is : > > " + punchingArrayCount);
		if (punchingArrayCount > 0) {
			System.err.println("punchingArrayCount is greater than zero......");
			return true;
		} else {
			System.err.println("punchingArrayCount is not greater than zero....");
			return false;
		}
	}

	private void updateTotalTimeInOffice(Object employeeAttendanceId, String totalTimeInOffice) throws JSONException {
		System.err.println("updateTotalTimeInOffice Method call...");

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceId);
		row.put("totaltimeinoffice", totalTimeInOffice);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
		System.err.println("updateTotalTimeInOffice Successfully....");

	}

	private void updateAttendanceType(Object employeeAttendanceId, int attendanceTypeId) throws JSONException {
		System.err.println("updateAttendanceType Method calll....");
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceId);
		row.put("attendancetypeid", attendanceTypeId);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
		System.err.println("updateAttendanceType Sucessfulyy....");

	}

	public static void updateTotalBreakingTime(Object employeeAttendanceId, String totalBreakingTime) throws JSONException {
		System.err.println("updateTotalBreakingTime method call....");
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceId);
		row.put("totalbreaktime", totalBreakingTime);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
		System.err.println("updateTotalBreakingTime successfully....");
	}

	private static void setSession(Object organizationId, Object organizationName, Object applicationId, Object applicationName) {
		System.err.println("setSession Method call....");
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, organizationId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, organizationName);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID, applicationId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION, applicationName);
		System.err.println("Set values in session successfully...");
	}

	private JSONArray getEmployeeAttedanceRecords(String currentDate) throws JSONException {
		System.err.println("getEmployeeAttedanceRecords Method call...");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeeid");
		array.put("employeeid.employeescheduleid");
		array.put("employeeid.holidaycalendarid");
		array.put("attendancedate");
		array.put("attendancetypeid");
		array.put("firstintime");
		array.put("lastouttime");
		array.put("firstindatetime");
		array.put("lastoutdatetime");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "attendancedate = '" + currentDate + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray attendanceArray = object.getJSONArray("employeeattendance");
		return attendanceArray;
	}

	public static String calculateTotalBreakTime(Object atendanceDateObject, Object employeeId) throws JSONException {
		System.err.println("calculateTotalBreakTime Method calll....");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeebreaks");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("breakstartdatetime");
		array.put("breakenddatetime");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "punchdate = '" + atendanceDateObject + "' and employeeid = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray breakArray = object.getJSONArray("employeebreaks");
		System.err.println("breakArray is : >> " + breakArray);

		int breakArrayCount = (breakArray == null || breakArray.length() == 0) ? 0 : breakArray.length();
		System.err.println("breakArrayCount is : >>> " + breakArrayCount);
		long totalMiliseconds = 0;

		if (breakArrayCount > 0) {
			System.err.println("breakArrayCount is greater nthan zerooo....");
			for (int counter = 0; counter < breakArrayCount; counter++) {
				System.err.println("In loop.....");

				Object employeeBreakId = breakArray.getJSONObject(counter).opt("__key__");
				System.err.println("employeeBreakId is : >> >" + employeeBreakId);

				Object breakStartDateObject = breakArray.getJSONObject(counter).opt("breakstartdatetime");
				System.err.println("breakStartDateObject is  >> " + breakStartDateObject);

				Date breakStartDate = DataTypeUtilities.checkDateFormat(breakStartDateObject);
				System.err.println("breakStartDate is : >> " + breakStartDate);

				Object breakEndDateObject = breakArray.getJSONObject(counter).opt("breakenddatetime");
				System.err.println("breakEndDateObject is : >> " + breakEndDateObject);

				Date breakEndDate = DataTypeUtilities.checkDateFormat(breakEndDateObject);
				System.err.println("breakEndDate is : >> " + breakEndDate);

				long miliseconds = DataTypeUtilities.calculateMiliseconds(breakStartDate, breakEndDate);
				System.err.println("miliseconds is : >> " + miliseconds);

				totalMiliseconds = totalMiliseconds + miliseconds;
				System.err.println("totalMiliseconds is : >> " + totalMiliseconds);
			}
			String totalBreakingTime = DataTypeUtilities.dissociateTimeInHrsAndMin(totalMiliseconds);
			System.err.println("totalBreakingTime is : >> " + totalBreakingTime);
			return totalBreakingTime;
		} else {
			System.err.println("No record found for breaking...");
			return "";
		}
	}

	private String getBackDate(String dateInString) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		Date currentDate = (Date) formatter.parse(dateInString);
		System.err.println("currentDate is : >> " + currentDate);

		Calendar cal = new GregorianCalendar();
		cal.setTime(currentDate);
		cal.add(Calendar.DATE, -1);
		String backDate = formatter.format(cal.getTime());
		System.err.println("backDate is : >> " + backDate);
		return backDate;
	}

	public static boolean getWeekDayValueFromSchedule(String weekDay, Object scheduleId) throws JSONException {
		System.err.println("getScheduleRecord Method call..");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		JSONArray columnArray = new JSONArray();
		columnArray.put(weekDay);
		query.put(Data.Query.FILTERS, "__key__ = " + scheduleId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_schedules");
		System.err.println("array is : >> " + array);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			Object dayOfWeek = array.getJSONObject(0).opt(weekDay);
			boolean isweekday = DataTypeUtilities.booleanValue(dayOfWeek);
			return isweekday;
		}
		return true;
	}
}
