package com.applane.resourceoriented.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class EmployeeCardPunchDataBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
	private boolean isGirnarSoft = false;

	public void onBeforeUpdate(Record record) throws VetoException {
		LogUtility.writeLog("EmployeeCardPunchDataBusinessLogic ------->onBeforeUpdate()");
		String operation = record.getOperationType();
		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		if (organizationId == 10719) {
			isGirnarSoft = true;
		} else {
			isGirnarSoft = false;
		}
		if (operation.equalsIgnoreCase(Updates.Types.INSERT) || operation.equalsIgnoreCase(Updates.Types.UPDATE)) {
			if (operation.equalsIgnoreCase(Updates.Types.UPDATE) && !record.has("inouttime")) {
				return;
			}
			/*
			 * get all record which are come for insert
			 */
			Object employeeId = record.getValue("employeeid");
			Object employeeUserId = record.getValue("employeeuserid");
			Object inOutDate = record.getValue("inoutdate");
			JSONArray cutOffTime = getCutOffTimeFromShiftHistory(employeeId, inOutDate);
			if (cutOffTime == null) {
				cutOffTime = getCutOffTimeFromAssignedShift(employeeId);
			}
			if (cutOffTime == null) {
				cutOffTime = getCutOffTimeFromSchedule(employeeId);
			}
			String cutOffTimeFormatted = getCutOffTimeFormatted(cutOffTime);
			/*
			 * String inOutDateInString = DataTypeUtilities.changeDateInString(inOutDate); LogUtility.writeLog("inOutDateInStrig is : >> " + inOutDateInString);
			 */

			String inOutTime = (String) record.getValue("inouttime");
			if (inOutTime != null && inOutTime.trim().length() > 0) {
				inOutTime = getTimeInFormat(inOutTime);
			}
			Date inOutDateTime = getDateTime(inOutTime, inOutDate);
			record.addUpdate("inoutdatetime", inOutDateTime);
			record.addUpdate("inouttime", inOutTime);
			int inOutStatusId = (Integer) record.getValue("inoutstatusid");
			String inOutGateName = (String) record.getValue("doorname");
			Date punchDate = DataTypeUtilities.checkDateFormat(inOutDate);
			String punchDateInString = EmployeeSalaryGenerationServlet.getDateInString(punchDate);

			/*
			 * check this record is already exist or not
			 */
			try {
				if (!isExist(employeeId, punchDateInString, inOutTime, inOutStatusId, inOutGateName)) {
					updateEmployeeAttendance(employeeId, employeeUserId, inOutDate, cutOffTimeFormatted, inOutTime, inOutStatusId, true);
				} else {
					throw new BusinessLogicException("This punching record already exist.....");
				}
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				LogUtility.writeLog("EmployeeCardPunchDataBusinessLogic >> onBeforeUpdate >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
				throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			}
		}
	}

	public JSONArray getCutOffTimeFromAssignedShift(Object employeeId) {
		try {

			JSONObject query = new JSONObject();
			JSONArray columns = new JSONArray();
			query.put(Data.Query.RESOURCE, "hris_employees");
			columns.put("shiftid.cut_off_time_time");
			columns.put("shiftid.cut_off_time_meridiem");
			columns.put("shiftid.fromtime_time");
			columns.put("shiftid.fromtime_meridiem");
			query.put(Data.Query.COLUMNS, columns);
			String filter = "__key__ = " + employeeId;
			query.put(Data.Query.FILTERS, filter);
			JSONObject data = ResourceEngine.query(query);
			JSONArray rows = data.getJSONArray("hris_employees");
			for (int counter = 0; counter < rows.length(); counter++) {
				JSONObject object = rows.getJSONObject(counter);
				Object time_time = object.opt("shiftid.cut_off_time_time");
				if (time_time == null) {
					return null;
				}
				Object time_meridiem = object.opt("shiftid.cut_off_time_meridiem");
				object.put("cutofftime_time", time_time);
				object.put("cutofftime_meridiem", time_meridiem);
			}
			return (rows == null || rows.length() == 0) ? null : rows;
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeCardPuinchData >> getCutOffTimeFromAssignedShift >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

	}

	public JSONArray getCutOffTimeFromShiftHistory(Object employeeId, Object inOutDate) {
		try {

			JSONObject query = new JSONObject();
			JSONArray columns = new JSONArray();
			query.put(Data.Query.RESOURCE, "hris_employees_shift_history");
			columns.put("shift_id.cut_off_time_time");
			columns.put("shift_id.cut_off_time_meridiem");
			columns.put("shift_id.fromtime_time");
			columns.put("shift_id.fromtime_meridiem");
			query.put(Data.Query.COLUMNS, columns);
			String filter = "employeeid = " + employeeId + " AND from_date<='" + inOutDate + "' AND to_date>='" + inOutDate + "'";
			JSONArray orders = new JSONArray();
			JSONObject orderObject = new JSONObject();
			orderObject.put(Data.Query.Orders.EXPERSSION, "__key__");
			orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
			orders.put(orderObject);
			query.put(Data.Query.ORDERS, orders);
			query.put(Data.Query.FILTERS, filter);
			query.put(Data.Query.MAX_ROWS, 1);
			JSONObject data = ResourceEngine.query(query);
			JSONArray rows = data.getJSONArray("hris_employees_shift_history");
			for (int counter = 0; counter < rows.length(); counter++) {
				JSONObject object = rows.getJSONObject(counter);
				Object time_time = object.opt("shift_id.cut_off_time_time");
				Object time_meridiem = object.opt("shift_id.cut_off_time_meridiem");
				object.put("cutofftime_time", time_time);
				object.put("cutofftime_meridiem", time_meridiem);
			}
			return (rows == null || rows.length() == 0) ? null : rows;
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeCardPuinchData >> getCutOffTimeFromShiftHistory >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

	}

	public void syncEmployeeAttendanceOnInvoke(Object[] keys) {
		try {
			int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			if (organizationId == 10719) {
				isGirnarSoft = true;
			} else {
				isGirnarSoft = false;
			}
			if (keys != null) {
				String keyObject = "";
				for (int counter = 0; counter < keys.length; counter++) {
					if (keyObject.length() > 0) {
						keyObject += "," + keys[counter];
					} else {
						keyObject += keys[counter];
					}
				}
				if (keyObject.length() > 0) {
					JSONArray employeeDetail = getEmployeeDetail(keyObject);
					for (int counter = 0; counter < employeeDetail.length(); counter++) {
						Object employeeId = employeeDetail.getJSONObject(counter).opt("employeeid");
						Object employeeUserId = employeeDetail.getJSONObject(counter).opt("employeeuserid");
						Object inOutDate = employeeDetail.getJSONObject(counter).opt("attendancedate");
						JSONArray cutOffTime = getCutOffTimeFromShiftHistory(employeeId, inOutDate);
						if (cutOffTime == null) {
							cutOffTime = getCutOffTimeFromAssignedShift(employeeId);
						}
						if (cutOffTime == null) {
							cutOffTime = getCutOffTimeFromSchedule(employeeId);
						}
						String cutOffTimeFormatted = getCutOffTimeFormatted(cutOffTime);
						// return;
						updateEmployeeAttendance(employeeId, employeeUserId, inOutDate, cutOffTimeFormatted, "00:00:00", 0, false);
					}
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeCardPunchDataBusinessLogic >> syncEmployeeAttendanceOnInvoke >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private String getCutOffTimeFormatted(JSONArray cutOffTime) {
		try {

			String cutOffTimeFormatted = "";
			if (cutOffTime.length() > 0) {
				String cutTime = "";
				String cutOffMeridiem = "";
				cutTime = "" + cutOffTime.getJSONObject(0).opt("cutofftime_time");
				cutOffMeridiem = "" + cutOffTime.getJSONObject(0).opt("cutofftime_meridiem");
				cutOffTimeFormatted = getCutOffTimeInFormat(cutTime, cutOffMeridiem);
			}
			return cutOffTimeFormatted;
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeCardPunchDataBusinessLogic >> getCutOffTimeFormatted >> cutOffTime (JSONArray[0]) >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
		}
		return "";
	}

	private JSONArray getEmployeeDetail(String key) throws Exception {
		try {
			JSONObject query = new JSONObject();
			JSONArray columns = new JSONArray();
			query.put(Data.Query.RESOURCE, "employeeattendance");
			columns.put("__key__");
			columns.put("attendancedate");
			columns.put("employeeuserid");
			columns.put("employeeid");
			String filter = "__key__ in (" + key + ")";
			query.put(Data.Query.COLUMNS, columns);
			query.put(Data.Query.FILTERS, filter);
			JSONObject data = ResourceEngine.query(query);
			return data.getJSONArray("employeeattendance");

		} catch (Exception e) {
			LogUtility.writeLog("NotebookSubmitIsComplete >> getEmployeeDetail >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private void updateEmployeeAttendance(Object employeeId, Object employeeUserId, Object inOutDate, String cutOffTimeFormatted, String inOutTime, int inOutStatusId, boolean isFromOnUpdate) throws Exception {
		/*
		 * if IOstaus is entry and any record found for entry or if IOstaus is exit and any record found for exit than it is a case of inconsisitent entry For the rest of the cases we have to do following things : a) Insert records in Punchcarddata table b) Update Attandance table to update firstin and lastout time c) Insert into EmployeeBreaks break table with following two conditions :
		 * 
		 * if IOstaus is entry and last record found for exit
		 */
		Date attendanceDateCutOffTime = null;
		if (cutOffTimeFormatted.length() > 0) {
			attendanceDateCutOffTime = getDateTime(cutOffTimeFormatted, inOutDate);
		}
		Date attendanceDateCommingTime = getDateTime(inOutTime, inOutDate);
		boolean previousAccToCutOff = false;

		String inOutDateTemp = Translator.getDateInString(DataTypeUtilities.getBackDate(inOutDate));
		if (isFromOnUpdate && attendanceDateCutOffTime != null && attendanceDateCommingTime.before(attendanceDateCutOffTime)) {
			previousAccToCutOff = true;
		}

		JSONArray employeeAttendanceArray = null;
		if (!previousAccToCutOff) {
			employeeAttendanceArray = getEmployeeAttendanceArray(employeeId, inOutDate);
		} else {
			employeeAttendanceArray = getEmployeeAttendanceArray(employeeId, inOutDateTemp);
		}
		Object employeeAttendanceId = null;
		Object attendanceTypeId = null;
		if (employeeAttendanceArray == null || employeeAttendanceArray.length() == 0) {
			if (!previousAccToCutOff) {
				employeeAttendanceId = insertEmployeeInDailyAttendance(employeeId, inOutDate, employeeUserId);
			} else {
				employeeAttendanceId = insertEmployeeInDailyAttendance(employeeId, inOutDateTemp, employeeUserId);
			}
		} else {
			employeeAttendanceId = employeeAttendanceArray.getJSONObject(0).opt("__key__");
			attendanceTypeId = employeeAttendanceArray.getJSONObject(0).opt("attendancetypeid");

		}
		if (employeeAttendanceId != null) {

			JSONArray inOutStatusArray = getPunchStatus(employeeId, inOutDate);
			HashMap<Long, Object[]> previousDayEmployeeDetailMap = new HashMap<Long, Object[]>();
			HashMap<Long, Object[]> presentDayEmployeeDetailMap = new HashMap<Long, Object[]>();
			ArrayList<Long> previousPunchTimeInOrder = new ArrayList<Long>();
			ArrayList<Long> presentPunchTimeInOrder = new ArrayList<Long>();
			deleteBreaks(employeeId, inOutDate);
			Date cutOfDateTime = null;
			if (cutOffTimeFormatted.length() > 0) {
				cutOfDateTime = getDateTime(cutOffTimeFormatted, inOutDate);
			}
			getPresentPreviousDayEmployeeDetailMaps(inOutStatusArray, previousPunchTimeInOrder, presentPunchTimeInOrder, previousDayEmployeeDetailMap, presentDayEmployeeDetailMap, cutOfDateTime, inOutStatusId, inOutTime, inOutDate, isFromOnUpdate);
			Collections.sort(presentPunchTimeInOrder);
			if (!previousAccToCutOff) {
				updateEmployeeRecords(presentDayEmployeeDetailMap, presentPunchTimeInOrder, employeeAttendanceId, inOutDate, employeeUserId, employeeId, attendanceTypeId);
			}

			if (previousPunchTimeInOrder.size() > 0) {
				inOutDate = DataTypeUtilities.getBackDate("" + inOutDate);
				if (cutOffTimeFormatted.length() > 0) {
					cutOfDateTime = getDateTime(cutOffTimeFormatted, inOutDate);
				}
				employeeAttendanceArray = getEmployeeAttendanceArray(employeeId, inOutDate);
				employeeAttendanceId = null;
				attendanceTypeId = null;
				if (employeeAttendanceArray == null || employeeAttendanceArray.length() == 0) {
					employeeAttendanceId = insertEmployeeInDailyAttendance(employeeId, inOutDate, employeeUserId);
				} else {
					employeeAttendanceId = employeeAttendanceArray.getJSONObject(0).opt("__key__");
					attendanceTypeId = employeeAttendanceArray.getJSONObject(0).opt("attendancetypeid");
				}
				if (employeeAttendanceId != null) {
					inOutStatusArray = getPunchStatus(employeeId, inOutDate);
					deleteBreaks(employeeId, inOutDate);
					getpreviousDayEmployeeDetailMap(inOutStatusArray, previousPunchTimeInOrder, previousDayEmployeeDetailMap, cutOfDateTime, inOutDate);
				}
				Collections.sort(previousPunchTimeInOrder);
				updateEmployeeRecords(previousDayEmployeeDetailMap, previousPunchTimeInOrder, employeeAttendanceId, inOutDate, employeeUserId, employeeId, attendanceTypeId);
			}

		}

	}

	private Object insertEmployeeInDailyAttendance(Object employeeId, Object inOutDate, Object employeeUserId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("attendancedate", inOutDate);
		row.put("employeeid", employeeId);
		row.put("employeeuserid", employeeUserId);
		// row.put("employeeuserid", employeeUserId);
		row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_PRESENT);
		updates.put(Data.Update.UPDATES, row);
		JSONObject object = ResourceEngine.update(updates);
		JSONArray jsonarray = object.getJSONArray("employeeattendance");
		object = jsonarray.getJSONObject(0).getJSONObject("__key__");
		return object.get("actual");
	}

	public String getCutOffTimeInFormat(String cutTime, String cutOffMeridiem) {
		String time = getTimeInFormat(cutTime);
		int hrs = Integer.parseInt((time.split(":"))[0]);
		if ((hrs + 12) < 24 && cutOffMeridiem.equalsIgnoreCase("PM")) {
			hrs += 12;
		} else if (hrs == 12 && cutOffMeridiem.equalsIgnoreCase("AM")) {
			hrs = 0;
		}
		String timeInFormat = ("" + hrs) + ":" + (time.split(":"))[1] + ":" + (time.split(":"))[2];
		return timeInFormat;
	}

	private JSONArray getCutOffTimeFromSchedule(Object employeeId) {
		try {

			JSONObject query = new JSONObject();
			JSONArray columns = new JSONArray();
			query.put(Data.Query.RESOURCE, "hris_employees");
			columns.put("employeescheduleid.cutofftime_time");
			columns.put("employeescheduleid.cutofftime_meridiem");
			query.put(Data.Query.COLUMNS, columns);
			String filter = "__key__ = " + employeeId;
			query.put(Data.Query.FILTERS, filter);
			JSONObject data = ResourceEngine.query(query);
			JSONArray rows = data.getJSONArray("hris_employees");
			for (int counter = 0; counter < rows.length(); counter++) {
				JSONObject object = rows.getJSONObject(counter);
				Object time_time = object.opt("employeescheduleid.cutofftime_time");
				Object time_meridiem = object.opt("employeescheduleid.cutofftime_meridiem");
				object.put("cutofftime_time", time_time);
				object.put("cutofftime_meridiem", time_meridiem);
			}
			return (rows == null || rows.length() == 0) ? null : rows;
		} catch (Exception e) {
			LogUtility.writeLog("NotebookSubmitIsComplete >> getTeachersDailyDiariesStatus >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

	}

	private void getpreviousDayEmployeeDetailMap(JSONArray inOutStatusArray, ArrayList<Long> previousPunchTimeInOrder, HashMap<Long, Object[]> previousDayEmployeeDetailMap, Date cutOffTime, Object inOutDate) throws JSONException {
		long cutOffMiliSecond = cutOffTime.getTime();
		for (int counter = 0; counter < inOutStatusArray.length(); counter++) {
			int inOutStatusId = inOutStatusArray.getJSONObject(counter).optInt("inoutstatusid");
			String lastPunchTime = inOutStatusArray.getJSONObject(counter).opt("inouttime") == null ? "" : inOutStatusArray.getJSONObject(counter).optString("inouttime");
			Object inoutdate = inOutStatusArray.getJSONObject(counter).opt("inoutdate");
			Object[] detail = new Object[3];
			detail[0] = inOutStatusId;
			detail[1] = lastPunchTime;
			detail[2] = inoutdate;
			Date dateTime = getDateTime(lastPunchTime, inoutdate);
			long key = dateTime.getTime();
			if (cutOffMiliSecond < key) {
				previousPunchTimeInOrder.add(key);
				previousDayEmployeeDetailMap.put(key, detail);
			}
		}
	}

	private void updateEmployeeRecords(HashMap<Long, Object[]> dayEmployeeDetailMap, ArrayList<Long> punchTimeInOrder, Object employeeAttendanceId, Object inOutDate, Object employeeUserId, Object employeeId, Object attendanceTypeId) throws Exception {
		Boolean isFirstIn = true;
		boolean inConsistent = true;
		String totalBreak = "00:00:00";
		String totalTimeInOfice = "00:00:00";
		String lastOutTime = "";// "00:00:00";
		long timeInOffice = 0l;
		long timeOutSideOffice = 0l;
		Object lastOutDate = null;
		boolean isOffDaysNotFixed = getIsOffDaysNotFixed(employeeId);

		for (int counter = 1; counter < punchTimeInOrder.size(); counter++) {
			long inOutTimeFromArrayListPresent = punchTimeInOrder.get(counter);
			long inOutTimeFromArrayListPrevious = punchTimeInOrder.get(counter - 1);

			if (isGirnarSoft) {
				inOutTimeFromArrayListPresent = punchTimeInOrder.get(punchTimeInOrder.size() - 1);
				inOutTimeFromArrayListPrevious = punchTimeInOrder.get(0);
				counter = punchTimeInOrder.size();
			}

			Object[] detailPrevious = dayEmployeeDetailMap.get(inOutTimeFromArrayListPrevious);
			int enteryTypePrevious = Integer.parseInt(detailPrevious[0].toString());
			Object[] detailPresent = dayEmployeeDetailMap.get(inOutTimeFromArrayListPresent);
			int enteryTypePresent = Integer.parseInt(detailPresent[0].toString());
			if (inConsistent && enteryTypePrevious == enteryTypePresent) {
				inConsistent = false;
				updateInConsistentEntry(employeeAttendanceId, HRISApplicationConstants.PUNCHING_INCONSISTENT);
			}
			String timePrevious = detailPrevious[1].toString();
			String timePresent = detailPresent[1].toString();

			Date previousDateTime = getDateTime(timePrevious, detailPrevious[2]);
			Date presentDateTime = getDateTime(timePresent, detailPresent[2]);
			if (isFirstIn && enteryTypePrevious == HRISApplicationConstants.PUNCH_ENTRY) {
				isFirstIn = false;
				String standardIOTime1 = getTime(timePrevious, detailPrevious[2]);
				Date standardIODateTime1 = getDateTime(timePrevious, detailPrevious[2]);
				updateFirstInTime(employeeAttendanceId, standardIOTime1, standardIODateTime1, employeeId, attendanceTypeId, isOffDaysNotFixed);
			}
			if (isFirstIn && enteryTypePresent == HRISApplicationConstants.PUNCH_ENTRY) {
				isFirstIn = false;
				String standardIOTime1 = getTime(timePresent, detailPrevious[2]);
				Date standardIODateTime1 = getDateTime(timePresent, detailPrevious[2]);
				updateFirstInTime(employeeAttendanceId, standardIOTime1, standardIODateTime1, employeeId, attendanceTypeId, isOffDaysNotFixed);
			}
			if (enteryTypePrevious == HRISApplicationConstants.PUNCH_EXIT) {
				lastOutTime = timePrevious;
				lastOutDate = detailPrevious[2];
			}
			if (enteryTypePresent == HRISApplicationConstants.PUNCH_EXIT) {
				lastOutTime = timePresent;
				lastOutDate = detailPresent[2];
			}
			if (enteryTypePrevious == HRISApplicationConstants.PUNCH_ENTRY) {
				if (enteryTypePresent == HRISApplicationConstants.PUNCH_EXIT) {
					long timeDifference = Translator.getDifferenceInMS(previousDateTime, presentDateTime);
					timeInOffice = timeInOffice + timeDifference;
				}
			}
			if (enteryTypePrevious == HRISApplicationConstants.PUNCH_EXIT) {
				if (enteryTypePresent == HRISApplicationConstants.PUNCH_ENTRY) {
					long timeDifference = Translator.getDifferenceInMS(previousDateTime, presentDateTime);
					timeOutSideOffice = timeOutSideOffice + timeDifference;
					// LogUtility.writeLog("timeOutSideOffice >> " + timeOutSideOffice + " << timeDifference out >> " + timeDifference);
					String outTime = getTime(timePrevious, detailPrevious[2]);
					String inTime = getTime(timePresent, detailPresent[2]);
					Date standardLastPunchDateTime = getDateTime(timePrevious, detailPrevious[2]);
					Date standardIODateTime1 = getDateTime(timePresent, detailPresent[2]);
					updateEmployeeBreakRecords(employeeId, employeeUserId, inOutDate, outTime, standardLastPunchDateTime, inTime, standardIODateTime1, employeeAttendanceId);
				}
			}
		}
		if (punchTimeInOrder.size() == 1) {
			long inOutTimeFromArrayListPrevious = punchTimeInOrder.get(0);
			Object[] detailPrevious = dayEmployeeDetailMap.get(inOutTimeFromArrayListPrevious);
			int enteryType = Integer.parseInt(detailPrevious[0].toString());
			String time = detailPrevious[1].toString();

			Date dateTime = getDateTime(time, detailPrevious[2]);
			String standardIOTime1 = getTime(time, detailPrevious[2]);
			if (enteryType == HRISApplicationConstants.PUNCH_ENTRY) {
				updateFirstInTime(employeeAttendanceId, standardIOTime1, dateTime, employeeId, attendanceTypeId, isOffDaysNotFixed);
			} else if (enteryType == HRISApplicationConstants.PUNCH_EXIT) {
				updateLastOutTimeSingleRecord(employeeAttendanceId, standardIOTime1, dateTime, employeeId, attendanceTypeId, isOffDaysNotFixed);
			}
		} else {
			if (lastOutDate == null) {
				lastOutDate = inOutDate;
			}
			String standaredLastOut = "";
			Date standardLastOutDateTime = null;
			if (lastOutTime.length() > 0) {
				standaredLastOut = getTime(lastOutTime, lastOutDate);
				standardLastOutDateTime = getDateTime(lastOutTime, lastOutDate);

			}
			totalTimeInOfice = Translator.getTimeFromMillisecond(timeInOffice);
			totalBreak = Translator.getTimeFromMillisecond(timeOutSideOffice);
			if (punchTimeInOrder.size() > 0) {
				updateLastOutTime(employeeAttendanceId, standaredLastOut, standardLastOutDateTime, totalTimeInOfice, totalBreak, employeeId);
			} else {
				updateLastOutTime(employeeAttendanceId, null, null, null, null, employeeId);
			}

			if (inConsistent) {// && timeInOfficeToConsistent > (5 * 60 * 60)) {
				updateInConsistentEntry(employeeAttendanceId, HRISApplicationConstants.PUNCHING_CONSISTENT);
			}
		}

	}

	private boolean getIsOffDaysNotFixed(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeescheduleid.is_off_day_not_fixed");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		if (rows != null && rows.length() > 0) {
			return Translator.booleanValue(rows.getJSONObject(0).opt("employeescheduleid.is_off_day_not_fixed"));
		}
		return false;
	}

	private void getPresentPreviousDayEmployeeDetailMaps(JSONArray inOutStatusArray, ArrayList<Long> previousPunchTimeInOrder, ArrayList<Long> presentPunchTimeInOrder, HashMap<Long, Object[]> previousDayEmployeeDetailMap, HashMap<Long, Object[]> presentDayEmployeeDetailMap, Date cutOffTime, int inOutStatusId, String inOutTime, Object inOutDate, boolean isFromOnUpdate) throws JSONException {
		long cutOffMilliSecond = 0l;
		if (cutOffTime != null) {
			cutOffMilliSecond = cutOffTime.getTime();
		}
		for (int counter = 0; counter < inOutStatusArray.length(); counter++) {
			int inOutStatusId1 = inOutStatusArray.getJSONObject(counter).optInt("inoutstatusid");
			String lastPunchTime = inOutStatusArray.getJSONObject(counter).opt("inouttime") == null ? "" : inOutStatusArray.getJSONObject(counter).optString("inouttime");
			Object inoutdate = inOutStatusArray.getJSONObject(counter).opt("inoutdate");
			Object[] detail = new Object[3];
			detail[0] = inOutStatusId1;
			detail[1] = lastPunchTime;
			detail[2] = inoutdate;
			Date dateTime = getDateTime(lastPunchTime, inoutdate);
			long key = dateTime.getTime();

			if (cutOffTime != null && cutOffMilliSecond >= key) {
				if (!previousPunchTimeInOrder.contains(key)) {
					previousPunchTimeInOrder.add(key);
				}
				previousDayEmployeeDetailMap.put(key, detail);
			} else {
				if (!presentPunchTimeInOrder.contains(key)) {
					presentPunchTimeInOrder.add(key);
				}
				presentDayEmployeeDetailMap.put(key, detail);
			}
		}
		if (isFromOnUpdate) {
			Date presentDateTime = getDateTime(inOutTime, inOutDate);
			long key = presentDateTime.getTime();
			Object[] detail = new Object[3];
			detail[0] = inOutStatusId;
			detail[1] = inOutTime;
			detail[2] = inOutDate;
			if (cutOffTime != null && cutOffMilliSecond >= key) {
				if (!previousPunchTimeInOrder.contains(key)) {
					previousPunchTimeInOrder.add(key);
				}
				previousDayEmployeeDetailMap.put(key, detail);
			} else {
				if (!presentPunchTimeInOrder.contains(key)) {
					presentPunchTimeInOrder.add(key);
				}
				presentDayEmployeeDetailMap.put(key, detail);
			}
		}
	}

	private void deleteBreaks(Object employeeId, Object inOutDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeebreaks");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and punchdate = '" + inOutDate + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeebreaks");

		for (int counter = 0; counter < rows.length(); counter++) {
			Object key = rows.getJSONObject(counter).opt("__key__");
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "employeebreaks");
			JSONObject row = new JSONObject();
			row.put("__key__", key);
			row.put("__type__", "delete");
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		}
	}

	private String getTimeInFormat(String inOutTime) {
		String[] time = inOutTime.split(":");
		try {
			if (time == null || time.length < 1) {
				throw new BusinessLogicException("1) Please Enter In / Out(24 hrs) or Cut Off(12 hrs) Time in Proper Format");
			}
			int hours = Integer.parseInt(time[0]);
			int min = 0;
			if (time.length > 1) {
				min = Integer.parseInt(time[1]);
			} else {
				if (!inOutTime.endsWith(":")) {
					inOutTime += ":";
				}
				inOutTime += "00";
			}
			int sec = 0;
			if (time.length > 2) {
				sec = Integer.parseInt(time[2]);
			} else {
				if (!inOutTime.endsWith(":")) {
					inOutTime += ":";
				}
				inOutTime += "00";
			}
			if (hours < 0 || hours > 24 || min < 0 || min > 60 || sec < 0 || sec > 60) {
				throw new BusinessLogicException("2) Please Enter In / Out(24 hrs) or Cut Off(12 hrs) Time in Range");
			}
		} catch (Exception e) {
			throw new BusinessLogicException("3) Please Enter In / Out(24 hrs) or Cut Off(12 hrs) Time in Proper Format");
		}
		return inOutTime;
	}

	private boolean isExist(Object employeeId, Object inOutDate, String inOutTime, int inOutStatusId, String inOutGateName) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeecardpunchdata");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and inoutdate = '" + inOutDate + "' and inouttime = '" + inOutTime + "' and inoutstatusid = " + inOutStatusId + " and doorname = '" + inOutGateName + "'");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeecardpunchdata");
		if (rows != null && rows.length() > 0) {
			return true;
		} else {
			return false;
		}
	}

	private JSONArray getPunchStatus(Object employeeId, Object inOutDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeecardpunchdata");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("inoutstatusid");
		array.put("inouttime");
		array.put("inoutdate");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and inoutdate = '" + inOutDate + "'");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeecardpunchdata");
		return rows;
	}

	public void onAfterUpdate(Record record) {

	}

	private JSONArray getEmployeeAttendanceArray(Object employeeId, Object inOutDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("firstindatetime");
		array.put("lastoutdatetime");
		array.put("attendancedate");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and attendancedate = '" + inOutDate + "'");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("employeeattendance");
		return rows;
	}

	private void updateInConsistentEntry(Object employeeAttendanceId, int punchingStatus) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceId);
		row.put("punchingtypeid", punchingStatus);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private String getTime(String inOutTime, Object inOutDate) throws ParseException {
		Date date = DataTypeUtilities.checkDateFormat(inOutDate);
		String[] mTimeArray = null;
		GregorianCalendar gc = null;
		String am_pm;
		mTimeArray = inOutTime.split(":");
		gc = new GregorianCalendar();
		gc.setTime(date);
		gc.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(mTimeArray[0]));
		gc.set(GregorianCalendar.MINUTE, Integer.parseInt(mTimeArray[1]));
		gc.set(GregorianCalendar.SECOND, Integer.parseInt(mTimeArray[2]));
		if (gc.get(GregorianCalendar.AM_PM) == 0) {
			am_pm = "AM";
		} else {
			am_pm = "PM";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
		Date dateObj = sdf.parse(inOutTime);
		SimpleDateFormat sdf1 = new SimpleDateFormat("K:mm");
		String finalTime = sdf1.format(dateObj);
		String[] dateObject = finalTime.split(":");

		if (dateObject[0].equals("0") || dateObject[0].equals("00")) {
			finalTime = "12:" + dateObject[1];
		}

		finalTime = finalTime + " " + am_pm;
		return finalTime;
	}

	public Date getDateTime(String mTime, Object IODate) {
		try {
			Date mDate = DataTypeUtilities.checkDateFormat(IODate);
			String[] mTimeArray = null;
			GregorianCalendar gc = null;
			Date finalDate = null;
			mTimeArray = mTime.split(":");
			gc = new GregorianCalendar();
			gc.setTime(mDate);
			gc.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(mTimeArray[0]));
			gc.set(GregorianCalendar.MINUTE, Integer.parseInt(mTimeArray[1]));
			gc.set(GregorianCalendar.SECOND, Integer.parseInt(mTimeArray[2]));
			finalDate = gc.getTime();
			return finalDate;
		} catch (Exception e) {
			LogUtility.writeLog("NotebookSubmitIsComplete >> getTeachersDailyDiariesStatus >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private void updateFirstInTime(Object employeeAttendanceId, String inOutTime, Date standardIODatetime, Object employeeId, Object attendanceTypeId, boolean isOffDaysNotFixed) throws JSONException {
		int[] halfOffDay = { 0 };
		boolean isOffDay = getOffDayToMarkEWD(employeeId, standardIODatetime, halfOffDay);
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceId);
		row.put("firstintime", inOutTime);
		row.put("firstindatetime", standardIODatetime);
		// if (attendanceTypeId != null && Translator.integerValue(attendanceTypeId) != HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
		// row.put("temp", HRISApplicationConstants.TEMP_VALUE_RUN_JOBS);
		// }
		if (isOffDay) {
			if (halfOffDay[0] == 1) {
				row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF);
			} else if (!isOffDaysNotFixed) {
				row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY);
			} else {
				row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_PRESENT);
			}
		} else {
			row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_PRESENT);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static boolean getOffDayToMarkEWD(Object employeeId, Date attDate, int[] halfOffDay) throws JSONException {
		JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeId);
		if (employeeArray != null && employeeArray.length() > 0) {
			Object scheduleId = employeeArray.getJSONObject(0).opt("employeescheduleid");
			Object holidayCalendarId = employeeArray.getJSONObject(0).opt("holidaycalendarid");
			String weekDay = null;
			int DAY_OF_WEEK = 0;
			int WEEK_OF_MONTH = 0;
			if (attDate != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(attDate);
				WEEK_OF_MONTH = cal.get(Calendar.WEEK_OF_MONTH);
				DAY_OF_WEEK = cal.get(Calendar.DAY_OF_WEEK);
				String strDateFormat = "EEEE";
				SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
				weekDay = sdf.format(attDate);
				weekDay = weekDay.toLowerCase();
			} else {
				return false;
			}
			if (scheduleId != null) {
				JSONArray scheduleArray = MarkAttendanceServlet.getScheduleRecord(weekDay, scheduleId);
				int scheduleArrayCount = (scheduleArray == null || scheduleArray.length() == 0) ? 0 : scheduleArray.length();
				Object dayOfWeek = null;
				if (scheduleArrayCount > 0) {
					dayOfWeek = scheduleArray.getJSONObject(0).opt(weekDay);
				}
				boolean isweekday = DataTypeUtilities.booleanValue(dayOfWeek);
				if (!isweekday) {
					return true;
				}
				JSONArray monthlyOffRecord = MarkAttendanceServlet.getMonthlyOffRecords(scheduleId, WEEK_OF_MONTH, DAY_OF_WEEK);
				int monthlyOffRecordCount = monthlyOffRecord == null ? 0 : monthlyOffRecord.length();
				if (monthlyOffRecordCount > 0) {
					if (monthlyOffRecord.getJSONObject(0).opt("off_day_type") != null) {
						halfOffDay[0] = 1;
					}
					return true;
				}
			}
			if (holidayCalendarId != null) {
				String attDateInString = EmployeeSalaryGenerationServlet.getDateInString(attDate);
				int holidayRecordCount = MarkAttendanceServlet.getHolidayRecords(holidayCalendarId, attDateInString);
				if (holidayRecordCount > 0) {
					return true;
				}
			}
		}
		return false;
	}

	private void updateLastOutTimeSingleRecord(Object employeeAttendanceId, String inOutTime, Date standardIODatetime, Object employeeId, Object attendanceTypeId, boolean isOffDaysNotFixed) throws JSONException {
		int[] halfOffDay = { 0 };
		boolean isOffDay = getOffDayToMarkEWD(employeeId, standardIODatetime, halfOffDay);
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceId);
		row.put("lastouttime", inOutTime);
		row.put("lastoutdatetime", standardIODatetime);
		if (isOffDay) {
			if (halfOffDay[0] == 1) {
				row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_HALF_DAY_OFF);
			} else {
				row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY);
			}
		} else {
			row.put("attendancetypeid", HRISApplicationConstants.ATTENDANCE_PRESENT);
		}
		if (attendanceTypeId != null && Translator.integerValue(attendanceTypeId) != HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateLastOutTime(Object employeeAttendanceId, String inOutTime, Date standardIODateTime, String totalTimeInOffice, String totalBreakingTime, Object employeeId) throws JSONException {
		JSONObject row = new JSONObject();
		if (totalTimeInOffice != null) {
			Object[] timeInOfc = totalTimeInOffice.split(":");
			row.put("totaltimeinoffice", "" + timeInOfc[0] + "Hr. " + "" + timeInOfc[1] + "min");
		} else {
			row.put("totaltimeinoffice", JSONObject.NULL);
		}
		if (totalBreakingTime != null) {
			Object[] timeBreak = totalBreakingTime.split(":");
			row.put("totalbreaktime", "" + timeBreak[0] + "Hr. " + "" + timeBreak[1] + " Min.");
		} else {
			row.put("totalbreaktime", JSONObject.NULL);
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		row.put("__key__", employeeAttendanceId);
		row.put("lastouttime", inOutTime == null ? JSONObject.NULL : inOutTime);
		row.put("lastoutdatetime", standardIODateTime == null ? JSONObject.NULL : standardIODateTime);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private void updateEmployeeBreakRecords(Object employeeId, Object employeeUserId, Object inOutDate, String lastPunchtime, Date lastPunchDateTime, String inOutTime, Date IODateTime, Object employeeAttendanceId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeebreaks");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("employeeuserid", employeeUserId == null ? JSONObject.NULL : employeeUserId);
		row.put("punchdate", inOutDate);
		row.put("breakstarttime", lastPunchtime);
		row.put("breakstartdatetime", lastPunchDateTime);
		row.put("breakendtime", inOutTime);
		row.put("breakenddatetime", IODateTime);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
		// String totalBreakingTime = calculateTotalBreakTime(inOutDate, employeeId);
		// if (employeeAttendanceId != null) {
		// updateTotalBreakingTime(employeeAttendanceId, totalBreakingTime);
		// }
	}

	public static String calculateTotalBreakTime(Object atendanceDateObject, Object employeeId) throws JSONException {
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
		int breakArrayCount = (breakArray == null || breakArray.length() == 0) ? 0 : breakArray.length();
		long totalMiliseconds = 0;

		if (breakArrayCount > 0) {
			for (int counter = 0; counter < breakArrayCount; counter++) {
				Object breakStartDateObject = breakArray.getJSONObject(counter).opt("breakstartdatetime");
				Date breakStartDate = DataTypeUtilities.checkDateFormat(breakStartDateObject);
				Object breakEndDateObject = breakArray.getJSONObject(counter).opt("breakenddatetime");
				Date breakEndDate = DataTypeUtilities.checkDateFormat(breakEndDateObject);
				long miliseconds = DataTypeUtilities.calculateMiliseconds(breakStartDate, breakEndDate);
				totalMiliseconds = totalMiliseconds + miliseconds;
			}
			String totalBreakingTime = DataTypeUtilities.dissociateTimeInHrsAndMin(totalMiliseconds);
			return totalBreakingTime;
		} else {
			return "";
		}
	}

	public static void updateTotalBreakingTime(Object employeeAttendanceId, String totalBreakingTime) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeeattendance");
		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceId);
		row.put("totalbreaktime", totalBreakingTime);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}
}