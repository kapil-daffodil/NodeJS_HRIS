package com.applane.resourceoriented.hris;

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
import com.google.apphosting.api.DeadlineExceededException;

public class HolidayBusinessLogic implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		String operationType = record.getOperationType();
		if (operationType.equalsIgnoreCase("insert")) {
			try {
				Object holidayCalendarId = record.getValue("holidaycalendarid");
				Object holidayDate = record.getValue("holidaydate");
				holidayDateValidation(holidayCalendarId, holidayDate);
			} catch (DeadlineExceededException e) {
				throw e;
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Some unknown error occured while insert holiday records.");
			}
		} else if (operationType.equalsIgnoreCase("update")) {
			try {
				Object holidayCalendarId = record.getValue("holidaycalendarid");
				Object holidayDate = record.getValue("holidaydate");
				holidayDateValidation(holidayCalendarId, holidayDate);
			} catch (DeadlineExceededException e) {
				LogUtility.writeLog("First exception block...");
				throw e;
			} catch (BusinessLogicException e) {
				LogUtility.writeLog("Second exception block");
				throw e;
			} catch (Exception e) {
				LogUtility.writeLog("Third Exception block...");
				e.printStackTrace();
				throw new BusinessLogicException("Some unknown error occured while update holiday records.");
			}
		}

	}

	private void holidayDateValidation(Object holidayCalendarId, Object dateObject) throws JSONException {
		Date holidayDate = DataTypeUtilities.checkDateFormat(dateObject);
		JSONArray holidayCalendarArray = getHolidayCalendarRecords(holidayCalendarId);
		int holidayCalendarArrayCount = (holidayCalendarArray == null || holidayCalendarArray.length() == 0) ? 0 : holidayCalendarArray.length();
		if (holidayCalendarArrayCount > 0) {

			Object fromDateObject = holidayCalendarArray.getJSONObject(0).opt("fromdate");
			Date fromDate = DataTypeUtilities.checkDateFormat(fromDateObject);
			Object toDateObject = holidayCalendarArray.getJSONObject(0).opt("todate");
			Date toDate = DataTypeUtilities.checkDateFormat(toDateObject);
			if (holidayDate.before(fromDate) || holidayDate.after(toDate)) {
				throw new BusinessLogicException("Holiday date must be lie between range of calendar dates.");
			}
		}
	}

	private JSONArray getHolidayCalendarRecords(Object holidayCalendarId) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidaycalendars");
		JSONArray columnArray = new JSONArray();
		columnArray.put("fromdate");
		columnArray.put("todate");
		query.put(Data.Query.FILTERS, "__key__ = " + holidayCalendarId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("hris_holidaycalendars");
		return array;
	}

	@Override
	public void onAfterUpdate(Record record) {
		// TODO Auto-generated method stub

	}

}
