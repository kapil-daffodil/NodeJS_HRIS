package com.applane.resourceoriented.hris;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class EmployeeLeaveBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) {
		String operation = record.getOperationType();

		if (operation.equalsIgnoreCase("insert")) {
		} else if (operation.equalsIgnoreCase("update")) {
		}
	}

	public static String getEmployeeEmailId(Object directReportingToID) {
		JSONArray rows = null;
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray columns = new JSONArray();
			columns.put("officialemailid");
			query.put(Data.Query.COLUMNS, columns);
			query.put(Data.Query.FILTERS, "__key__ = " + directReportingToID);
			JSONObject object;
			object = ResourceEngine.query(query);
			rows = object.getJSONArray("hris_employees");
		} catch (Exception e) {
			throw new BusinessLogicException("Exception come while get officialemailid.");
		}
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			String officialEmailId = null;
			try {
				officialEmailId = rows.getJSONObject(0).optString("officialemailid");
			} catch (Exception e) {
				LogUtility.writeLog("Exception while get officialemailid..");
			}
			return officialEmailId;
		} else {
			throw new BusinessLogicException("Some unknown error occured while get employee official email id.");
		}
	}

	public static void rejectedLeaveUpdation(int newStatus, int oldStatus, Record record, Object employeeLeaveId, String approverComment, String currentDate) {
		if (oldStatus == HRISConstants.LEAVE_NEW && newStatus == HRISConstants.LEAVE_REJECTED) {
			record.addUpdate("approveddate", currentDate);
			if (!record.has("approvedbyid")) {
				String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
				int currentUserId = getCurrentUserId(CURRENT_USER_EMAILID);
				record.addUpdate("approvedbyid", currentUserId);
				updateLeaveComments(currentUserId, employeeLeaveId, currentDate, approverComment);
			}
		}
	}

	public static void cancelLeaveUpdation(int newStatus, int oldStatus, Record record, Object employeeLeaveId, String approverComment, String currentDate) {
		if (oldStatus == HRISConstants.LEAVE_NEW && newStatus == HRISConstants.LEAVE_CANCEL) {
			String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
			int currentUserId = getCurrentUserId(CURRENT_USER_EMAILID);
			updateLeaveComments(currentUserId, employeeLeaveId, currentDate, approverComment);
		}
	}

	public static void approvedLeaveUpdation(int newStatus, int oldStatus, Record record, Object employeeLeaveId, String approverComment, String currentDateStr, Object leaveRequestFromDateObject, Object leaveRequestToDateObject, int fromDurationTypeId, int toDurationTypeId, Object employeeId, JSONObject employeeRecord, Object leaveTypeId) throws Exception {
		if ((oldStatus == HRISConstants.LEAVE_NEW || oldStatus == HRISConstants.LEAVE_REJECTED) && newStatus == HRISConstants.LEAVE_APPROVED) {

			Object employeeScheduleId = employeeRecord.opt("employeescheduleid");
			Object holidayCalendarId = employeeRecord.opt("holidaycalendarid");
			Object isSandWichObejct = employeeRecord.opt("leavepolicyid.issandwich");
			boolean isSandWich = DataTypeUtilities.booleanValue(isSandWichObejct);
			if (!record.has("approvedbyid")) {
				String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
				int currentUserId = getCurrentUserId(CURRENT_USER_EMAILID);
				record.addUpdate("approvedbyid", currentUserId);
				record.addUpdate("approveddate", currentDateStr);
				updateLeaveComments(currentUserId, employeeLeaveId, currentDateStr, approverComment);

			}
			markAttendanceInEmployeeAttendance(leaveRequestFromDateObject, leaveRequestToDateObject, fromDurationTypeId, toDurationTypeId, isSandWich, employeeScheduleId, holidayCalendarId, employeeId, leaveTypeId, employeeLeaveId, record);
		}
	}

	public static void updateMonthlyAttendanceWithSandwich(Date date, Object employeeId, double subtractNonWorkingDays) throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		Object monthId = cal.get(Calendar.MONTH) + 1;
		Object yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
		JSONArray employeeMonghlyAttendance = getEmployeeMonghlyAttendance(employeeId, monthId, yearId);
		if (employeeMonghlyAttendance != null && employeeMonghlyAttendance.length() > 0) {
			Object key = employeeMonghlyAttendance.getJSONObject(0).opt("__key__");
			double actualNonWorkingDays = Translator.doubleValue(employeeMonghlyAttendance.getJSONObject(0).opt("actualnonworkingdays"));
			updateEmployeeMonthlyAttendance(key, (actualNonWorkingDays == 0 ? 0 : (actualNonWorkingDays - (subtractNonWorkingDays))));
		}
	}

	private static void updateEmployeeMonthlyAttendance(Object key, double actualNonWorkingDays) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("actualnonworkingdays", actualNonWorkingDays);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private static JSONArray getEmployeeMonghlyAttendance(Object employeeId, Object monthId, Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("actualnonworkingdays");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("employeemonthlyattendance");
		return array;
	}

	public static Long getMonthId(String monthhName) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_months");
			JSONArray array = new JSONArray();
			array.put("__key__");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "name = '" + monthhName + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_months");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				Long monthId = rows.getJSONObject(0).optLong("__key__");
				return monthId;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error while retrieving month id from month name: " + monthhName + " : " + e.getMessage());
		}
	}

	private static void updateLeaveComments(int currentUserId, Object employeeLeaveId, String currentDate, String approverComment) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "leavecomments");
			JSONObject row = new JSONObject();
			row.put("employeeid", currentUserId);
			row.put("leaverequestid", employeeLeaveId);
			row.put("commentdate", currentDate);
			row.put("comment", approverComment);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while update leave comment");
		}
	}

	public static int getCurrentUserId(String CURRENT_USER_EMAILID) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray columns = new JSONArray();
			columns.put("__key__");
			query.put(Data.Query.COLUMNS, columns);
			query.put(Data.Query.FILTERS, "officialemailid = '" + CURRENT_USER_EMAILID + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("hris_employees");
			LogUtility.writeLog("rows IS : >> " + rows);
			int currentUserId = rows.getJSONObject(0).optInt("__key__");
			return currentUserId;
		} catch (Exception e) {
			throw new BusinessLogicException("Exception come while get current userId" + e.getMessage());
		}
	}

	public static Object getReportingToUserId(String emailId) {
		JSONArray userArray;
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "up_users");
			JSONArray array = new JSONArray();
			array.put("__key__");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "emailid= '" + emailId + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			userArray = object.getJSONArray("up_users");
		} catch (Exception e) {
			throw new BusinessLogicException("Sone unknown error occured while get reporting to user Id.");
		}
		int userCount = (userArray == null || userArray.length() == 0) ? 0 : userArray.length();
		if (userCount > 0) {
			Object userId = null;
			try {
				userId = userArray.getJSONObject(0).opt("__key__");
			} catch (Exception e) {
				LogUtility.writeLog("Error occured while get userid.");
			}
			return userId;
		} else {
			return null;
			// throw new BusinessLogicException("Exception come while get reporting to user Id.");
		}
	}

	public static void updateAllUsers(Object employeeLeaveId, Object reportingToUserId) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "hris_leaverequests__approveruserid");
			JSONObject row = new JSONObject();
			row.put("hris_leaverequests", employeeLeaveId);
			row.put("approveruserid", reportingToUserId);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			throw new BusinessLogicException("Exception come while update all approver users.");
		}
	}

	public static void leaveRequestDuplicateValidationInUpdate(Object leaveRequestFromDate, Object leaveRequestToDate, Object employeeId, Object leaveRequestId) {
		JSONArray leaveRequestArray;
		JSONObject leaveQuery;
		JSONArray leaveColumnArray;
		JSONObject leaveOject;
		try {
			leaveQuery = new JSONObject();
			leaveQuery.put(Data.Query.RESOURCE, "hris_leaverequests");
			leaveColumnArray = new JSONArray();
			leaveColumnArray.put("__key__");
			leaveColumnArray.put("fromdate");
			leaveColumnArray.put("todate");
			leaveQuery.put(Data.Query.COLUMNS, leaveColumnArray);
			leaveQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeId + "' and fromdate >= '" + leaveRequestFromDate + "' and  fromdate <= '" + leaveRequestToDate + "' and leavestatusid  NOT IN (" + HRISConstants.LEAVE_CANCEL + "," + HRISConstants.LEAVE_REJECTED + ")" + " and __key__ != " + leaveRequestId));
			JSONArray groupby = new JSONArray();
			groupby.put("__key__");
			leaveQuery.put(Data.Query.GROUPS, groupby);
			leaveOject = ResourceEngine.query(leaveQuery);
			leaveRequestArray = leaveOject.getJSONArray("hris_leaverequests");
			LogUtility.writeLog("leaveRequestArray is : >> " + leaveRequestArray);
		} catch (Exception e) {
			throw new BusinessLogicException("Some wnknown error occured while get leave records.");
		}
		int leaveRequestCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
		LogUtility.writeLog("leaveRequestCount is : >>" + leaveRequestCount);
		if (leaveRequestCount > 0) {
			throw new BusinessLogicException("You have already requested for leave in this duration.");
		} else {
			try {
				leaveQuery = new JSONObject();
				leaveQuery.put(Data.Query.RESOURCE, "hris_leaverequests");
				leaveColumnArray = new JSONArray();
				leaveColumnArray.put("__key__");
				leaveColumnArray.put("fromdate");
				leaveColumnArray.put("todate");
				leaveQuery.put(Data.Query.COLUMNS, leaveColumnArray);
				leaveQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeId + "' and  fromdate <= '" + leaveRequestFromDate + "' and  todate >= '" + leaveRequestFromDate + "' and leavestatusid  NOT IN (" + HRISConstants.LEAVE_CANCEL + "," + HRISConstants.LEAVE_REJECTED + ")" + " and __key__ != " + leaveRequestId));
				JSONArray groupbyArray = new JSONArray();
				groupbyArray.put("__key__");
				leaveQuery.put(Data.Query.GROUPS, groupbyArray);
				leaveOject = ResourceEngine.query(leaveQuery);
				leaveRequestArray = leaveOject.getJSONArray("hris_leaverequests");
			} catch (Exception e) {
				throw new BusinessLogicException("Some unknown error occured while get leave records.");
			}
			LogUtility.writeLog("leaveRequestArray IS : >> " + leaveRequestArray);
			leaveRequestCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
			LogUtility.writeLog("leaveRequestCount is : >>" + leaveRequestCount);
			if (leaveRequestCount > 0) {
				throw new BusinessLogicException("You have already requested for leave in this duration.");
			}
		}
	}

	public static void leaveRequestDuplicateValidation(Object leaveRequestFromDate, Object leaveRequestToDate, Object employeeId) {
		JSONArray leaveRequestArray;
		JSONObject leaveQuery;
		JSONArray leaveColumnArray;
		JSONObject leaveOject;
		try {
			leaveQuery = new JSONObject();
			leaveQuery.put(Data.Query.RESOURCE, "hris_leaverequests");
			leaveColumnArray = new JSONArray();
			leaveColumnArray.put("__key__");
			leaveColumnArray.put("fromdate");
			leaveColumnArray.put("todate");
			leaveQuery.put(Data.Query.COLUMNS, leaveColumnArray);
			leaveQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeId + "' and fromdate >= '" + leaveRequestFromDate + "' and  fromdate <= '" + leaveRequestToDate + "' and leavestatusid NOT IN (" + HRISConstants.LEAVE_CANCEL + "," + HRISConstants.LEAVE_REJECTED + ")"));
			JSONArray groupby = new JSONArray();
			groupby.put("__key__");
			leaveQuery.put(Data.Query.GROUPS, groupby);
			leaveOject = ResourceEngine.query(leaveQuery);
			leaveRequestArray = leaveOject.getJSONArray("hris_leaverequests");
			LogUtility.writeLog("leaveRequestArray is : >> " + leaveRequestArray);
		} catch (Exception e) {
			new TakeAndMarkAttendanceServlet();
			TakeAndMarkAttendanceServlet.sendMailInCaseOfException(e, "Error while leaveRequestDuplicateValidation Testing while get leave records. 563");
			throw new BusinessLogicException("Some wnknown error occured while get leave records.");
		}
		int leaveRequestCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
		LogUtility.writeLog("leaveRequestCount is : >>" + leaveRequestCount);
		if (leaveRequestCount > 0) {
			throw new BusinessLogicException("You have already requested for leave in this duration.");
		} else {
			try {
				leaveQuery = new JSONObject();
				leaveQuery.put(Data.Query.RESOURCE, "hris_leaverequests");
				leaveColumnArray = new JSONArray();
				leaveColumnArray.put("__key__");
				leaveColumnArray.put("fromdate");
				leaveColumnArray.put("todate");
				leaveQuery.put(Data.Query.COLUMNS, leaveColumnArray);
				leaveQuery.put(Data.Query.FILTERS, ("employeeid= '" + employeeId + "' and  fromdate <= '" + leaveRequestFromDate + "' and  todate >= '" + leaveRequestFromDate + "' and leavestatusid  NOT IN (" + HRISConstants.LEAVE_CANCEL + "," + HRISConstants.LEAVE_REJECTED + ")"));
				JSONArray groupbyArray = new JSONArray();
				groupbyArray.put("__key__");
				leaveQuery.put(Data.Query.GROUPS, groupbyArray);
				leaveOject = ResourceEngine.query(leaveQuery);
				leaveRequestArray = leaveOject.getJSONArray("hris_leaverequests");
			} catch (Exception e) {
				TakeAndMarkAttendanceServlet.sendMailInCaseOfException(e, "Error while leaveRequestDuplicateValidation Testing");
				throw new BusinessLogicException("Some unknown error occured while get leave records.");
			}
			LogUtility.writeLog("leaveRequestArray IS : >> " + leaveRequestArray);
			leaveRequestCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
			LogUtility.writeLog("leaveRequestCount is : >>" + leaveRequestCount);
			if (leaveRequestCount > 0) {
				throw new BusinessLogicException("You have already requested for leave in this duration.");
			}
		}
	}

	public static Double getLeaveCount(Object leaveRequestFromDateObject, Object leaveRequestToDateObject, int fromDurationTypeId, int toDurationTypeId, Object employeeId) throws JSONException, NumberFormatException, ParseException {
		Date leaveFromDate = DataTypeUtilities.checkDateFormat(leaveRequestFromDateObject);
		Date leaveToDate = DataTypeUtilities.checkDateFormat(leaveRequestToDateObject);
		JSONArray employeeArray = new JSONArray();
		employeeArray = getEmployeeRecord(employeeId);
		int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
		boolean isSandWich = false;
		Object scheduleId = null;
		Object holidayCalendarId = null;
		if (employeeArrayCount > 0) {
			Object isSandWichObject = employeeArray.getJSONObject(0).optInt("leavepolicyid.issandwich");
			isSandWich = DataTypeUtilities.booleanValue(isSandWichObject);
			scheduleId = employeeArray.getJSONObject(0).optInt("employeescheduleid");
			holidayCalendarId = employeeArray.getJSONObject(0).optInt("holidaycalendarid");
		}

		Long dateDiff = DataTypeUtilities.differenceBetweenDates(leaveFromDate, leaveToDate);
		Double leaveCount = 0.0;
		if (leaveFromDate.equals(leaveToDate)) {
			if (fromDurationTypeId == HRISApplicationConstants.DURATION_FULLDAY) {
				LogUtility.writeLog("DURATION TYPE ID IN IF  : >>" + fromDurationTypeId);
				LogUtility.writeLog("LEAVE COUNT IN IF : >>" + leaveCount);
				leaveCount = leaveCount + 1.0;
			} else {
				leaveCount = leaveCount + 0.5;
			}
		} else if (leaveToDate.after(leaveFromDate) && dateDiff == 1) {
			if (toDurationTypeId != HRISApplicationConstants.DURATION_FULLDAY && fromDurationTypeId != HRISApplicationConstants.DURATION_FULLDAY) {
				leaveCount = leaveCount + 1.0;
			} else if (toDurationTypeId == HRISApplicationConstants.DURATION_FULLDAY && fromDurationTypeId == HRISApplicationConstants.DURATION_FULLDAY) {
				leaveCount = leaveCount + 2.0;
			} else {
				leaveCount = leaveCount + 1.5;
			}
		} else if (leaveToDate.after(leaveFromDate) && dateDiff > 1) {
			if (toDurationTypeId != HRISApplicationConstants.DURATION_FULLDAY && fromDurationTypeId != HRISApplicationConstants.DURATION_FULLDAY) {
				leaveCount = leaveCount + 1.0;
			} else if (toDurationTypeId == HRISApplicationConstants.DURATION_FULLDAY && fromDurationTypeId == HRISApplicationConstants.DURATION_FULLDAY) {
				leaveCount = leaveCount + 2.0;
			} else if (toDurationTypeId == HRISApplicationConstants.DURATION_FIRSTHALF && fromDurationTypeId == HRISApplicationConstants.DURATION_SECONDHALF) {
				leaveCount = leaveCount + 1.0;
			} else {
				leaveCount = leaveCount + 1.5;
			}
			leaveCount = leaveCount + (dateDiff - 1);
		}

		// check if sandwitch policy is true
		if (!isSandWich) {
			String leaveFromDateInStr = getDateInString(leaveFromDate);
			String leaveToDateInStr = getDateInString(leaveToDate);
			String currentMonth = DataTypeUtilities.getCurrentMonth(leaveFromDate);
			Object monthId = EmployeeMonthlyAttendance.getMonthId(currentMonth);
			String currentYear = DataTypeUtilities.getCurrentYear(leaveFromDate);
			long nonWorkingDays = getNonWorkingDays(employeeId, scheduleId, holidayCalendarId, leaveFromDate, leaveToDate, Integer.parseInt(currentYear), (Integer) monthId, leaveFromDateInStr, leaveToDateInStr);
			leaveCount = leaveCount - nonWorkingDays;
		}

		return leaveCount;
	}

	public static long getNonWorkingDays(Object employeeId, Object employeeScheduleId, Object holidayCalendarId, Date startDate, Date endDate, Integer currentYear, Integer monthId, String startDateInString, String endDateInstring) throws JSONException, ParseException {
		List<String> daysOfWeek = new ArrayList<String>();
		daysOfWeek.add(HRISApplicationConstants.Weekdays.MONDAY);
		daysOfWeek.add(HRISApplicationConstants.Weekdays.TUESDAY);
		daysOfWeek.add(HRISApplicationConstants.Weekdays.WEDNESDAY);
		daysOfWeek.add(HRISApplicationConstants.Weekdays.THURSDAY);
		daysOfWeek.add(HRISApplicationConstants.Weekdays.FRIDAY);
		daysOfWeek.add(HRISApplicationConstants.Weekdays.SATURDAY);
		daysOfWeek.add(HRISApplicationConstants.Weekdays.SUNDAY);

		int weekListSize = daysOfWeek.size();
		Set<Object> nonWorkingDayList = new LinkedHashSet<Object>();
		if (employeeScheduleId != null) {
			for (int counter = 0; counter < weekListSize; counter++) {
				boolean isWeekDay = EmployeeMonthlyAttendance.getWeekDayFromSchedule(daysOfWeek.get(counter), employeeScheduleId);
				if (!isWeekDay) {
					long dayBetween = DataTypeUtilities.differenceBetweenDates(startDate, endDate);
					for (int counter1 = 0; counter1 < dayBetween + 1; counter1++) {
						Calendar cal2 = new GregorianCalendar();
						cal2.setTime(startDate);
						cal2.add(Calendar.DATE, counter1);
						Date date = cal2.getTime();

						SimpleDateFormat simpleDateformat = new SimpleDateFormat("EEEE");

						String weekDay = simpleDateformat.format(date);

						String weekDayInLower = weekDay.toLowerCase();

						if (daysOfWeek.get(counter).equalsIgnoreCase(weekDayInLower)) {
							nonWorkingDayList.add(date);
						}
					}
				}
			}

			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_monthlyoff");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("weeknoid");
			columnArray.put("weekdayid");
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, "scheduleid = " + employeeScheduleId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_monthlyoff");
			int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
			if (arrayCount > 0) {
				for (int counter = 0; counter < arrayCount; counter++) {

					Integer weekNoId = (Integer) array.getJSONObject(counter).opt("weeknoid");

					Integer weekDayId = (Integer) array.getJSONObject(counter).opt("weekdayid");

					Date date = EmployeeMonthlyAttendance.getDate(currentYear, monthId, weekNoId, weekDayId);

					String dateInString = EmployeeMonthlyAttendance.getDateInString(date);

					DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

					Date monthlyOffDate = formatter.parse(dateInString);

					long dayBetween = DataTypeUtilities.differenceBetweenDates(startDate, endDate);

					for (int counter1 = 0; counter1 < dayBetween + 1; counter1++) {
						Calendar cal2 = new GregorianCalendar();
						cal2.setTime(startDate);
						cal2.add(Calendar.DATE, counter1);
						Date monthdate = cal2.getTime();

						String monthDateInString = formatter.format(monthdate);

						Date monthlyDate = formatter.parse(monthDateInString);

						if (monthlyDate.equals(monthlyOffDate)) {
							nonWorkingDayList.add(monthlyOffDate);
						}
					}
				}
			} else {
				LogUtility.writeLog("No monthly off records found for this employee [" + employeeId + "]");
			}
		}

		if (holidayCalendarId != null) {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_holidays");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("holidaydate");
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate >= '" + startDateInString + "' and holidaydate <= '" + endDateInstring + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_holidays");
			int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
			if (arrayCount > 0) {
				for (int counter = 0; counter < arrayCount; counter++) {
					// Object holidayId = array.getJSONObject(counter).opt("__key__");
					Object holidayDateObject = array.getJSONObject(counter).opt("holidaydate");
					Date holidayDate = DataTypeUtilities.checkDateFormat(holidayDateObject);
					nonWorkingDayList.add(holidayDate);
				}
			}
		}
		long nonWorkingDaysCount = nonWorkingDayList == null ? 0 : nonWorkingDayList.size();
		return nonWorkingDaysCount;
	}

	public static String getDateInString(Date date) {
		if (date != null) {
			return new SimpleDateFormat("yyyy-MM-dd").format(date);
		} else {
			return "";
		}
	}

	private static JSONArray getEmployeeRecord(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("leavepolicyid.issandwich");
		columnArray.put("employeescheduleid");
		columnArray.put("holidaycalendarid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
		JSONObject leavePolicyObject;
		leavePolicyObject = ResourceEngine.query(query);
		JSONArray array = leavePolicyObject.getJSONArray("hris_employees");
		return array;
		/*
		 * int arrayCount = array == null ? 0 : array.length(); boolean isSandwich; if (arrayCount > 0) { Object isSandwichObject = array.getJSONObject(0).opt("leavepolicyid.issandwich"); isSandwich = DataTypeUtilities.booleanValue(isSandwichObject); return isSandwich; } return false;
		 */
	}

	private static boolean isMonthlyOff(Date date, Object scheduleId, Integer durationtypeId) throws JSONException {

		int DAY_OF_WEEK = 0;
		int WEEK_OF_MONTH = 0;

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		WEEK_OF_MONTH = cal.get(Calendar.WEEK_OF_MONTH);
		DAY_OF_WEEK = cal.get(Calendar.DAY_OF_WEEK);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_monthlyoff");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("off_day_type");
		query.put(Data.Query.FILTERS, "scheduleid = " + scheduleId + " and weeknoid = " + WEEK_OF_MONTH + " and weekdayid = " + DAY_OF_WEEK);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_monthlyoff");
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			String type = array.getJSONObject(0).optString("off_day_type", null);
			if (type != null && type.equals("Half Day") && (durationtypeId == HRISConstants.DURATION_FIRSTHALF || durationtypeId == HRISConstants.DURATION_SECONDHALF)) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	private static boolean isWeekDay(Date date, Object scheduleId) throws JSONException {

		String weekDay = null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		/*
		 * WEEK_OF_MONTH = cal.get(Calendar.WEEK_OF_MONTH); LogUtility.writeLog("WEEK_OF_MONTH is : >> " + WEEK_OF_MONTH);
		 * 
		 * DAY_OF_WEEK = cal.get(Calendar.DAY_OF_WEEK); LogUtility.writeLog("weekDayNo is : >> " + DAY_OF_WEEK);
		 */

		String strDateFormat = "EEEE";
		SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
		weekDay = sdf.format(date);
		weekDay = weekDay.toLowerCase();

		if (scheduleId != null) {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_schedules");
			JSONArray columnArray = new JSONArray();
			columnArray.put(weekDay);
			query.put(Data.Query.FILTERS, "__key__ = " + scheduleId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_schedules");
			int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
			boolean isWeekDay;
			if (arrayCount > 0) {
				Object isWeekDayObject = array.getJSONObject(0).opt(weekDay);
				isWeekDay = DataTypeUtilities.booleanValue(isWeekDayObject);
				return isWeekDay;
			}
		}
		return true;
	}

	public static void leaveRequestValidation(Object leaveRequestFromDateObject, Object leaveRequestToDateObject, Object employeeId, Integer fromDurationtypeId, Integer toDurationtypeId, JSONObject employeeRecord, boolean isOffDaysNotFixed) throws JSONException {
		Date leaveRequestFromDate = null;
		Date leaveRequestToDate = null;

		if (leaveRequestFromDateObject != null && leaveRequestToDateObject != null) {
			leaveRequestFromDate = DataTypeUtilities.checkDateFormat(leaveRequestFromDateObject);
			leaveRequestToDate = DataTypeUtilities.checkDateFormat(leaveRequestToDateObject);
			Object scheduleId = employeeRecord.opt("employeescheduleid");
			Object holidayCalendarId = employeeRecord.opt("holidaycalendarid");

			if (leaveRequestToDate.before(leaveRequestFromDate)) {
				throw new BusinessLogicException("Leave To Date must be greater than or equal to From Date.");
			}

			if (!isOffDaysNotFixed && !isWeekDay(leaveRequestToDate, scheduleId) || isMonthlyOff(leaveRequestToDate, scheduleId, toDurationtypeId)) {
				throw new BusinessLogicException("You can not apply leave on this To date. Because this day is off for you.");
			} else if (!isOffDaysNotFixed && !isWeekDay(leaveRequestFromDate, scheduleId) || isMonthlyOff(leaveRequestFromDate, scheduleId, fromDurationtypeId)) {
				throw new BusinessLogicException("You can not apply leave on this From date. Because this day is off for you.");
			} else if (!isOffDaysNotFixed && !isHoliday(holidayCalendarId, leaveRequestToDateObject)) {
				throw new BusinessLogicException("You can not apply leave on this To date. Because this day is Holiday for you.");
			} else if (!isOffDaysNotFixed && !isHoliday(holidayCalendarId, leaveRequestFromDateObject)) {
				throw new BusinessLogicException("You can not apply leave on this From date. Because this day is Holiday for you.");
			}

			if (leaveRequestFromDate.equals(leaveRequestToDate)) {
				if (!fromDurationtypeId.equals(toDurationtypeId)) {
					throw new BusinessLogicException("For one day leave \"Duration Type\" for both dates should be same.");
				}
			}

			JSONArray leavePolicyArray = getEmployeeLeavePolicyRecords(employeeId);
			int leavePolicyArrayCount = leavePolicyArray == null ? 0 : leavePolicyArray.length();

			if (leavePolicyArrayCount > 0) {

				if (leaveRequestToDate.after(leaveRequestFromDate)) { // && !leaveRequestToDate.equals(leaveRequestFromDate)) {
					if (fromDurationtypeId == HRISApplicationConstants.DURATION_FIRSTHALF && toDurationtypeId == HRISApplicationConstants.DURATION_FIRSTHALF) {
						throw new BusinessLogicException("These should be measured as two different leaves.");
					} else if (fromDurationtypeId == HRISApplicationConstants.DURATION_FULLDAY && toDurationtypeId == HRISApplicationConstants.DURATION_SECONDHALF) {
						throw new BusinessLogicException("These should be measured as two different leaves.");
					} else if (fromDurationtypeId == HRISApplicationConstants.DURATION_FIRSTHALF && toDurationtypeId == HRISApplicationConstants.DURATION_FULLDAY) {
						throw new BusinessLogicException("These should be measured as two different leaves.");
					} else if (fromDurationtypeId == HRISApplicationConstants.DURATION_SECONDHALF && toDurationtypeId == HRISApplicationConstants.DURATION_SECONDHALF) {
						throw new BusinessLogicException("These should be measured as two different leaves.");
					} else if (fromDurationtypeId == HRISApplicationConstants.DURATION_FIRSTHALF && toDurationtypeId == HRISApplicationConstants.DURATION_SECONDHALF) {
						throw new BusinessLogicException("These should be measured as two different leaves.");
					}
				}
			} else {
				throw new BusinessLogicException("No Leave policy defined for you. Kindly consult with your HR.");
			}
		}
	}

	private static boolean isHoliday(Object holidayCalendarId, Object dateObject) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidays");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.FILTERS, "holidaycalendarid = " + holidayCalendarId + " and holidaydate = '" + dateObject + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_holidays");
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			return false;
		} else {
			return true;
		}
	}

	private static JSONArray getEmployeeLeavePolicyRecords(Object employeeId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray array = new JSONArray();
			array.put("leavepolicyid");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__= " + employeeId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray leavePolicyArray = object.getJSONArray("hris_employees");
			return leavePolicyArray;

		} catch (JSONException e) {
			e.printStackTrace();
			throw new BusinessLogicException("" + e.getMessage());
		}
	}

	@Override
	public void onAfterUpdate(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("insert")) {
			Object employeeLeaveId = record.getValue("__key__");
			Object directReportingToId = record.getValue("approverid");
			String directReportingToEmailId = getEmployeeEmailId(directReportingToId);
			Object directReportingUserId = getReportingToUserId(directReportingToEmailId);
			if (directReportingUserId != null) {
				updateAllUsers(employeeLeaveId, directReportingUserId);
			}
			JSONArray inDirectReportingToArray = getEmployeeInDirectReportingTo(employeeLeaveId);
			int inDirectReportingToArrayCount = inDirectReportingToArray == null ? 0 : inDirectReportingToArray.length();
			if (inDirectReportingToArrayCount > 0) {
				for (int counter = 0; counter < inDirectReportingToArrayCount; counter++) {
					Object inDirectReportingToId = null;
					try {
						// Object leaveRequestId = inDirectReportingToArray.getJSONObject(counter).opt("hris_leaverequests");
						inDirectReportingToId = inDirectReportingToArray.getJSONObject(counter).opt("indirectreportingtoid");
					} catch (Exception e) {
						throw new BusinessLogicException("Some unknown error occured while get indirect reportingto.");
					}
					String inDirectReportingToEmailId = getEmployeeEmailId(inDirectReportingToId);
					Object indirectReportingUserId = getReportingToUserId(inDirectReportingToEmailId);
					if (indirectReportingUserId != null) {
						updateAllUsers(employeeLeaveId, indirectReportingUserId);
					}
				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			Object employeeLeaveId = record.getValue("__key__");
			// Object directReportingToId = record.getValue("approverid");
			JSONArray inDirectReportingToArray = getEmployeeInDirectReportingTo(employeeLeaveId);
			int inDirectReportingToArrayCount = inDirectReportingToArray == null ? 0 : inDirectReportingToArray.length();
			if (inDirectReportingToArrayCount > 0) {
				Object inDirectReportingToId;
				Object leaveRequestId;
				for (int counter = 0; counter < inDirectReportingToArrayCount; counter++) {
					try {
						leaveRequestId = inDirectReportingToArray.getJSONObject(counter).opt("hris_leaverequests");
						inDirectReportingToId = inDirectReportingToArray.getJSONObject(counter).opt("indirectreportingtoid");
					} catch (Exception e) {
						throw new BusinessLogicException("Some unknown error occured while get indirect reportingto.");
					}
					if (inDirectReportingToId != null) {
						String inDirectReportingToEmailId = getEmployeeEmailId(inDirectReportingToId);
						Object indirectReportingUserId = getReportingToUserId(inDirectReportingToEmailId);
						boolean duplicateEntry = findDuplicateUserEntry(leaveRequestId, indirectReportingUserId);
						if (!duplicateEntry && indirectReportingUserId != null) {
							updateAllUsers(employeeLeaveId, indirectReportingUserId);
						}
					}
				}
			}
		}
	}

	public static boolean findDuplicateUserEntry(Object leaveRequestId, Object indirectReportingUserId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_leaverequests__approveruserid");
			JSONArray array = new JSONArray();
			array.put("__key__");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "hris_leaverequests = " + leaveRequestId + " and approveruserid = " + indirectReportingUserId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray userArray = object.getJSONArray("hris_leaverequests__approveruserid");
			int userArrayCount = userArray == null ? 0 : userArray.length();
			if (userArrayCount > 0) {
				return true;
			} else {
				return false;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			throw new BusinessLogicException("" + e.getMessage());
		}
	}

	public static JSONArray getEmployeeInDirectReportingTo(Object employeeLeaveId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_leaverequests__indirectreportingtoid");
			JSONArray array = new JSONArray();
			array.put("__key__");
			array.put("hris_leaverequests");
			array.put("indirectreportingtoid");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "hris_leaverequests = " + employeeLeaveId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray reportingArray = object.getJSONArray("hris_leaverequests__indirectreportingtoid");
			return reportingArray;
		} catch (JSONException e) {
			e.printStackTrace();
			throw new BusinessLogicException("" + e.getMessage());
		}
	}

	private static void markAttendanceInEmployeeAttendance(Object fromDateObject, Object toDateObject, int fromDurationTypeId, int toDurationtypeId, boolean isSandwich, Object scheduleId, Object holidayCalendarId, Object employeeId, Object leaveTypeId, Object leaveRequestId, Record record) throws Exception {
		Date leaveFromDate = DataTypeUtilities.checkDateFormat(fromDateObject);
		Date leaveToDate = DataTypeUtilities.checkDateFormat(toDateObject);
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		Date currentDate = Translator.dateValue(TODAY_DATE);
		Date lastDateOfMonth = DataTypeUtilities.getMonthLastDate(currentDate);
		long daysDiff = DataTypeUtilities.differenceBetweenDates(leaveFromDate, leaveToDate);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date nextFromDate = DataTypeUtilities.checkDateFormat(DataTypeUtilities.getNextDate(EmployeeSalaryGenerationServlet.getDateInString(leaveFromDate)));

		Date backToDate = DataTypeUtilities.checkDateFormat(DataTypeUtilities.getBackDate(EmployeeSalaryGenerationServlet.getDateInString(leaveToDate)));
		Object paidStatus = record.getValue("paidstatus");
		int alternateHoliday = Translator.integerValue(record.getValue("alternateholiday"));
		boolean markAttendance = true;
		if (nextFromDate.before(backToDate) || nextFromDate.equals(backToDate)) {
			if (fromDurationTypeId == HRISConstants.DURATION_FIRSTHALF || fromDurationTypeId == HRISConstants.DURATION_SECONDHALF || toDurationtypeId == HRISConstants.DURATION_FIRSTHALF || toDurationtypeId == HRISConstants.DURATION_SECONDHALF) {
				long diff = DataTypeUtilities.differenceBetweenDates(nextFromDate, backToDate) + 1;
				int offDays = EmployeeMonthlyAttendance.getActualNonWorkingDaysForSandWich(employeeId, scheduleId, holidayCalendarId, nextFromDate, backToDate);
				int difference = (int) diff;
				if (difference == offDays) {
					markAttendance = false;
				}
			}
		}
		boolean isDaffodil = false;
		int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		if (currentOrganizationId == 5) {
			isDaffodil = true;
		}
		for (int counter = 0; counter < daysDiff + 1; counter++) {
			Calendar cal2 = new GregorianCalendar();
			cal2.setTime(leaveFromDate);
			cal2.add(Calendar.DATE, counter);
			Date leaveDate = cal2.getTime();
			String leaveDateInString = formatter.format(leaveDate);
			// Date leaveDate = formatter.parse(leaveDateInString);
			if (!leaveDate.after(lastDateOfMonth) || !isDaffodil) {

				Object employeeAttendanceId = getEmployeeAttendanceId(leaveDateInString, employeeId);
				if (alternateHoliday == HRISApplicationConstants.EmployeeDecision.YES) {
					updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_HOLIDAY, null, leaveRequestId, "" + fromDateObject, employeeId, null);
				} else {

					if (leaveDate.equals(leaveFromDate) && (fromDurationTypeId == HRISConstants.DURATION_FIRSTHALF)) {
						// update From date on daily attendance as a Half day leave
						updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE, leaveTypeId, leaveRequestId, leaveDateInString, employeeId, paidStatus);
					} else if (leaveDate.equals(leaveFromDate) && (fromDurationTypeId == HRISConstants.DURATION_SECONDHALF)) {
						// update From date on daily attendance as a Half day leave
						updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE, leaveTypeId, leaveRequestId, leaveDateInString, employeeId, paidStatus);
					} else if (leaveDate.equals(leaveFromDate) && fromDurationTypeId == HRISConstants.DURATION_FULLDAY) {
						// upedate from date on daily attendance as full day leave
						updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_LEAVE, leaveTypeId, leaveRequestId, leaveDateInString, employeeId, paidStatus);
					} else if (leaveDate.equals(leaveToDate) && (toDurationtypeId == HRISConstants.DURATION_FIRSTHALF)) {
						// update to date on daily attendance as half day leave
						updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE, leaveTypeId, leaveRequestId, leaveDateInString, employeeId, paidStatus);
					} else if (leaveDate.equals(leaveToDate) && (toDurationtypeId == HRISConstants.DURATION_SECONDHALF)) {
						// update to date on daily attendance as half day leave
						updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE, leaveTypeId, leaveRequestId, leaveDateInString, employeeId, paidStatus);
					} else if (leaveDate.equals(leaveToDate) && toDurationtypeId == HRISConstants.DURATION_FULLDAY) {
						// update to date on daily attendance as full day leave
						updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_LEAVE, leaveTypeId, leaveRequestId, leaveDateInString, employeeId, paidStatus);
					} else if (markAttendance) {
						updateEmployeeAttendance(employeeAttendanceId, HRISApplicationConstants.ATTENDANCE_LEAVE, leaveTypeId, leaveRequestId, leaveDateInString, employeeId, paidStatus);
					}
				}
			}
		}
	}

	private static void updateEmployeeAttendance(Object employeeAttendanceId, int attendanceTypeId, Object leaveTypeId, Object leaveRequestId, String leaveDateInString, Object employeeId, Object paidStatus) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		if (employeeAttendanceId != null) {
			row.put("__key__", employeeAttendanceId);
		} else {
			row.put("employeeid", employeeId);
			row.put("attendancedate", leaveDateInString);
		}
		row.put("attendancetypeid", attendanceTypeId);
		row.put("attendance_status", 2);
		if (leaveRequestId != null) {
			row.put("employeeleaveid", leaveRequestId);
		}
		if (leaveTypeId != null) {
			row.put("leavetypeid", leaveTypeId);
		}
		if (paidStatus != null) {
			row.put("paidstatus", paidStatus);
		}
		int[] halfOffDay = new int[] { 0 };
		boolean isOffDay = EmployeeCardPunchDataBusinessLogic.getOffDayToMarkEWD(employeeId, Translator.dateValue(leaveDateInString), halfOffDay);
		if (!isOffDay && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_HOLIDAY) {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS_ON_LEAVE_APPROVAL);
		} else {
			row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private static Object getEmployeeAttendanceId(String leaveDate, Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate = '" + leaveDate + "'");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("employeeattendance");
		int arrayCount = array == null ? 0 : array.length();
		Object employeeAttendaceId = null;
		if (arrayCount > 0) {
			employeeAttendaceId = array.getJSONObject(0).opt("__key__");
			return employeeAttendaceId;
		} else {
			return employeeAttendaceId;
		}
	}
}
