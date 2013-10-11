package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeAttendance implements OperationJob {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object attendanceDate = record.getValue("attendancedate");
		Object attendanceTypeId = record.getValue("attendancetypeid");
		Object branchId = record.getValue("branchid");

		Object dayBetween = record.getValue("dayBetween");
		Object counter = record.getValue("counter");
		Object temp = record.getValue("temp");

		record.removeUpdate("dayBetween");
		record.removeUpdate("counter");
		record.removeUpdate("temp");

		Integer defaultAttendanceTypeId = (Integer) attendanceTypeId;
		try {

			if ((temp == null || temp.equals(HRISApplicationConstants.TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE)) && attendanceDate != null && attendanceTypeId != null) {
				Date date = DataTypeUtilities.checkDateFormat(attendanceDate);
				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				String attDate = updateDateFormat.format(date);
				Object takeAttendanceKey = isExists(attDate, branchId);

				if ((temp == null && isExists(attDate, branchId) != null) || (temp != null && !temp.equals(HRISApplicationConstants.TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE))) {
					throw new BusinessLogicException("Attendance for the selected date is already taken.");
				} else if (defaultAttendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || defaultAttendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					throw new BusinessLogicException("Extra working day can't be mark as default attendance.");
				} else if (temp != null && takeAttendanceKey != null && temp.equals(HRISApplicationConstants.TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE)) {
					record.addUpdate("__key__", takeAttendanceKey);
					// record.removeUpdate("attendancedate");
					// record.removeUpdate("attendancetypeid");
					// record.removeUpdate("branchid");
				}

				// ApplaneQueue queue = ApplaneQueue.getApplaneQueue();
				// TaskOptions taskOptions = TaskOptions.Builder.withUrl("/queue/employeeattendance");
				// taskOptions = taskOptions.param("attendancedate", "" + attendanceDate);
				// taskOptions = taskOptions.param("attendanceTypeId", "" + attendanceTypeId);
				// taskOptions = taskOptions.param("branchid", "" + branchId);
				// taskOptions = taskOptions.param("dayBetween", dayBetween == null ? "" : ("" + dayBetween));
				// taskOptions = taskOptions.param("counter", counter == null ? "" : ("" + counter));
				// taskOptions = taskOptions.param("temp", temp == null ? "" : ("" + temp));
				// queue.addTaskToQueue(taskOptions, null, true);

				JSONObject taskQueueParameters = new JSONObject();
				taskQueueParameters.put("attendancedate", "" + attendanceDate);
				taskQueueParameters.put("attendanceTypeId", "" + attendanceTypeId);
				taskQueueParameters.put("branchid", "" + branchId);
				taskQueueParameters.put("dayBetween", dayBetween);
				taskQueueParameters.put("counter", counter);
				taskQueueParameters.put("temp", temp);

				ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.MarkAttendanceServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);

			}
			// } catch (DeadlineExceededException e) {
			// throw e;
			// } catch (BusinessLogicException e) {
			// throw e;
		} catch (Exception e) {
			LogUtility.writeError(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			throw new BusinessLogicException("Some error ocuured while taking attendance.");
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	Object isExists(String date, Object branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takeattendance");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "attendancedate = '" + date + "' and branchid = " + branchId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("takeattendance");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	// using from row action of take attendance
	@SuppressWarnings("unused")
	private void rePopulateDailyAttendance(Object[] selectedkey) throws JSONException {
		JSONArray array = getTakeAttendanceRecord(selectedkey[0]);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			Object attendanceDate = array.getJSONObject(0).opt("attendancedate");
			Object attendanceTypeId = array.getJSONObject(0).opt("attendancetypeid");
			Object branchId = array.getJSONObject(0).opt("branchid");

			// ApplaneQueue queue = ApplaneQueue.getApplaneQueue();
			// TaskOptions taskOptions = TaskOptions.Builder.withUrl("/queue/employeeattendance");
			// taskOptions = taskOptions.param("attendancedate", "" + attendanceDate);
			// taskOptions = taskOptions.param("attendanceTypeId", "" + attendanceTypeId);
			// taskOptions = taskOptions.param("branchid", "" + branchId);
			// queue.addTaskToQueue(taskOptions, null);

			JSONObject taskQueueParameters = new JSONObject();
			taskQueueParameters.put("attendancedate", "" + attendanceDate);
			taskQueueParameters.put("attendanceTypeId", "" + attendanceTypeId);
			taskQueueParameters.put("branchid", "" + branchId);

			ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.MarkAttendanceServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);

		}
	}

	private JSONArray getTakeAttendanceRecord(Object key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takeattendance");
		JSONArray array = new JSONArray();
		array.put("attendancedate");
		array.put("branchid");
		array.put("attendancetypeid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("takeattendance");
		return rows;
	}
}
