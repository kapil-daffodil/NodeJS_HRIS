package com.applane.resourceoriented.hris;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;

public class PopulateMonthlyAttendanceBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws VetoException {

		String operationType = record.getOperationType();

		if (operationType.equalsIgnoreCase("insert")) {
			Object monthId = record.getValue("monthid");
			Object yearId = record.getValue("yearid");
			Object branchId = record.getValue("branchid");
			Integer defaultAttendanceTypeId = DataTypeUtilities.integerValue(record.getValue("attendancetypeid"));
			if (monthId != null && yearId != null && branchId != null && defaultAttendanceTypeId != null) {
				try {
					String yearName = EmployeeKPIServlet.getYearName(yearId);
					String monthName = EmployeeKPIServlet.getMonthName(monthId);

					// if (isExist(monthId, yearId, branchId)) {
					// throw new BusinessLogicException("Attendance for this month, year and branch already populated.");
					// }

					Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
					Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
					long dayBetween = (DataTypeUtilities.daysBetween(monthLastDate, monthFirstDate)) + 1;
					// long dayBetween = 16;
					int counter = 0;
					Calendar cal2 = new GregorianCalendar();
					cal2.setTime(monthFirstDate);
					cal2.add(Calendar.DATE, counter);
					Date date = cal2.getTime();
					String dateInString = EmployeeSalaryGenerationServlet.getDateInString(date);
					String fromMonthlyAttendance = HRISApplicationConstants.TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE;

					insertTakeAttendanceRecord(dateInString, branchId, defaultAttendanceTypeId, dayBetween, counter, fromMonthlyAttendance);
				} catch (BusinessLogicException e) {
					throw e;
				} catch (Exception e) {
					LogUtility.writeError(ExceptionUtils.getExceptionTraceMessage(PopulateMonthlyAttendanceBusinessLogic.class.getName(), e));
					throw new BusinessLogicException("Sorry for the inconvinence caused. Unable to update populate attendance.");
				}
			}
		}
	}

	public static void insertTakeAttendanceRecord(String attendanceDate, Object branchId, Integer defaultAttendanceTypeId, long dayBetween, int counter, String fromMonthlyAttendance) throws JSONException {
		boolean runLoop = true;
		while (runLoop) {
			if (counter < dayBetween && !isAlreadyExist(branchId, attendanceDate)) {
				JSONObject updates = new JSONObject();
				updates.put(Data.Query.RESOURCE, "takeattendance");
				JSONObject row = new JSONObject();

				row.put("attendancedate", attendanceDate);
				row.put("branchid", branchId);
				row.put("attendancetypeid", defaultAttendanceTypeId);

				// extra parameters to generate loop for daily attendance
				row.put("dayBetween", dayBetween);
				row.put("counter", counter);
				row.put("temp", fromMonthlyAttendance);

				updates.put(Data.Update.UPDATES, row);
				ResourceEngine.update(updates);
				runLoop = false;
			} else {
				Calendar cal2 = new GregorianCalendar();
				cal2.setTime(DataTypeUtilities.checkDateFormat(attendanceDate));
				cal2.add(Calendar.DATE, 1);
				Date date = cal2.getTime();
				attendanceDate = EmployeeSalaryGenerationServlet.getDateInString(date);
				counter++;
			}
		}
		// if (!isAlreadyExist(branchId, attendanceDate)) {
		// JSONObject updates = new JSONObject();
		// updates.put(Data.Query.RESOURCE, "takeattendance");
		// JSONObject row = new JSONObject();
		//
		// row.put("attendancedate", attendanceDate);
		// row.put("branchid", branchId);
		// row.put("attendancetypeid", defaultAttendanceTypeId);
		//
		// // extra parameters to generate loop for daily attendance
		// row.put("dayBetween", dayBetween);
		// row.put("counter", counter);
		// row.put("temp", fromMonthlyAttendance);
		//
		// updates.put(Data.Update.UPDATES, row);
		// ResourceEngine.update(updates);
		// } else if (counter < dayBetween) {
		// counter++;
		// Calendar cal2 = new GregorianCalendar();
		// cal2.setTime(DataTypeUtilities.checkDateFormat(attendanceDate));
		// cal2.add(Calendar.DATE, 1);
		// Date date = cal2.getTime();
		// String dateInString = EmployeeSalaryGenerationServlet.getDateInString(date);
		// fromMonthlyAttendance = HRISApplicationConstants.TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE;
		//
		// insertTakeAttendanceRecord(dateInString, branchId, defaultAttendanceTypeId, dayBetween, counter, fromMonthlyAttendance);
		// } else {
		// LogUtility.writeLog("Attendance for this date " + attendanceDate + " already exist");
		// }
	}

	private static boolean isAlreadyExist(Object branchId, String attendanceDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "branchid = " + branchId + " and attendancedate = '" + attendanceDate + "'");
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("takeattendance");
		if (rows != null && rows.length() > 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onAfterUpdate(Record record) {

	}

	private static boolean isExist(Object monthId, Object yearId, Object branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "populatemonthlyattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "monthid = " + monthId + " and yearid = " + yearId + " and branchid = " + branchId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("populatemonthlyattendance");
		if (rows != null && rows.length() > 0) {
			return true;
		} else {
			return false;
		}
	}

}
