package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.moduleimpl.SystemParameters;

public class EmployeeDataQueries {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public static boolean isEqual(Date dateFirst, Date dateSecond) {
		long to_date_time = dateSecond.getTime();
		long from_date_time = dateFirst.getTime();
		int length = (int) ((to_date_time / (1000 * 60 * 60 * 24)) - (from_date_time / (1000 * 60 * 60 * 24)));
		if (length == 0) {
			return true;
		}
		return false;
	}

	public static JSONArray getEmployeePresentInfo(Object employeeId, Object fromDateObj, Object toDateObj) throws JSONException, java.text.ParseException {

		String currentDateInStr = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getSystemParameters().getCurrentDateTime());
		Date currentDate = DataTypeUtilities.checkDateFormat(currentDateInStr);

		Date fromDate = DataTypeUtilities.checkDateFormat(fromDateObj);
		Date toDate = DataTypeUtilities.checkDateFormat(toDateObj);

		JSONArray presentArray = new JSONArray();

		if (currentDate.after(toDate) && currentDate.after(fromDate)) {// past
			findPresentInfoFromEmployeeAttendance(presentArray, employeeId, fromDateObj, toDateObj, true);
			if (presentArray != null && presentArray.length() > 0) {
				return presentArray;
			} else {
				return null;
			}
		} else if ((currentDate.after(fromDate) && (currentDate.before(toDate) || isEqual(currentDate, toDate)))) {// present
			findPresentInfoFromEmployeeAttendance(presentArray, employeeId, fromDateObj, currentDateInStr, false);
			Map<String, Object> employeeMap = getEmployeeScheduleAndHolidayCalendar(employeeId);
			if (employeeMap != null && employeeMap.size() > 0) {
				Object employeeScheduleId = employeeMap.get("employeescheduleid");
				Object holidayCalendarId = employeeMap.get("holidaycalendarid");
				if (employeeScheduleId != null && holidayCalendarId != null) {
					presentArray = findPresentInfoFromEmployeeSchedule(presentArray, employeeId, employeeScheduleId, holidayCalendarId, currentDate, toDate);
					if (presentArray != null && presentArray.length() > 0) {
						return presentArray;
					} else {
						return null;
					}
				}
			}
		} else if ((currentDate.before(fromDate) || isEqual(currentDate, fromDate)) && currentDate.before(toDate)) {// future
			Map<String, Object> employeeMap = getEmployeeScheduleAndHolidayCalendar(employeeId);
			if (employeeMap != null && employeeMap.size() > 0) {
				Object employeeScheduleId = employeeMap.get("employeescheduleid");
				Object holidayCalendarId = employeeMap.get("holidaycalendarid");
				if (employeeScheduleId != null && holidayCalendarId != null) {
					presentArray = findPresentInfoFromEmployeeSchedule(presentArray, employeeId, employeeScheduleId, holidayCalendarId, fromDate, toDate);
					if (presentArray != null && presentArray.length() > 0) {
						return presentArray;
					} else {
						return null;
					}
				}
			}
		} else {
		}
		return null;
	}

	private static JSONArray findPresentInfoFromEmployeeSchedule(JSONArray presentArray, Object employeeId, Object employeeScheduleId, Object holidayCalendarId, Date fromDate, Date toDate) throws JSONException {
		long daysBetween = DataTypeUtilities.differenceBetweenDates(fromDate, toDate);

		for (int counter = 0; counter < daysBetween + 1; counter++) {
			Calendar cal2 = new GregorianCalendar();
			cal2.setTime(fromDate);
			cal2.add(Calendar.DATE, counter);
			Date date = cal2.getTime();
			String dateInString = getDateInString(date);
			SimpleDateFormat simpleDateformat = new SimpleDateFormat("EEEE");
			String weekDay = simpleDateformat.format(date);
			String weekDayInLower = weekDay.toLowerCase();
			boolean isWeekDay = EmployeeMonthlyAttendance.getWeekDayFromSchedule(weekDayInLower, employeeScheduleId);
			if (isWeekDay) {
				if (isHolidayCheck(holidayCalendarId, dateInString)) {
				} else {
					JSONObject attandanceObj = new JSONObject();
					attandanceObj.put("employeeid", employeeId);
					attandanceObj.put("date", dateInString);
					attandanceObj.put("time", "9Hr. 00min");
					attandanceObj.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_PRESENT);
					presentArray.put(attandanceObj);
				}
			} else {
			}
		}
		return presentArray;
	}

	private static boolean isHolidayCheck(Object holidayCalendarId, String date) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("holidaydate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate = '" + date + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_holidays");
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			return true;
		} else {
			return false;
		}
	}

	public static String getDateInString(Date date) {
		if (date != null) {
			return new SimpleDateFormat("yyyy-MM-dd").format(date);
		} else {
			return "";
		}
	}

	private static void findPresentInfoFromEmployeeAttendance(JSONArray presentInfo, Object employeeId, Object fromDate, Object toDate, boolean isPast) throws JSONException {
		String filter = "employeeid = " + employeeId + " and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_PRESENT + "," + HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY + "," + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF + "," + HRISApplicationConstants.ATTENDANCE_UNKNOWN + "," + HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME + "," + HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY + "," + HRISApplicationConstants.ATTENDANCE_TOUR + ")" + " and attendancedate >= '" + fromDate;
		if (isPast) {
			filter += "' and  attendancedate <='" + toDate + "'";
		} else {
			filter += "' and  attendancedate <'" + toDate + "'";
		}

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("attendancedate");
		columnArray.put("totaltimeinoffice");
		columnArray.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, filter);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeeattendance");
		int rowCount = rows.length();
		for (int i = 0; i < rowCount; i++) {

			JSONObject row = rows.getJSONObject(i);
			Object firstInDateTime = row.opt("attendancedate");
			Object totalTimeInOffice = row.opt("totaltimeinoffice");
			Object attendanceTypeId = row.opt("attendancetypeid");

			JSONObject attandanceObj = new JSONObject();
			attandanceObj.put("employeeid", employeeId);
			attandanceObj.put("date", firstInDateTime);
			attandanceObj.put("time", totalTimeInOffice);
			attandanceObj.put("attendancetypeid", attendanceTypeId);
			presentInfo.put(attandanceObj);
		}
	}

	private static Map<String, Object> getEmployeeScheduleAndHolidayCalendar(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("employeescheduleid");
		array.put("holidaycalendarid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = '" + employeeId + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Object employeeScheduleId = rows.getJSONObject(0).optLong("employeescheduleid");
			Object holidayCalendarId = rows.getJSONObject(0).optLong("holidaycalendarid");

			Map<String, Object> employeeMap = new HashMap<String, Object>();
			employeeMap.put("employeescheduleid", employeeScheduleId);
			employeeMap.put("holidaycalendarid", holidayCalendarId);

			return employeeMap;
		} else {
			return null;
		}
	}

}
