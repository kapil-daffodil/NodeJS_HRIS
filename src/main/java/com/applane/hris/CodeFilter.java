package com.applane.hris;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.HRISConstants;

public class CodeFilter {

	public Date getMonthFirstDate(Date currentDate) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(currentDate);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			Date firstDayOfMonth = cal.getTime();
			return firstDayOfMonth;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some error occured while get month first." + e.getMessage());
		}
	}

	public Date getMonthLastDate(Date currentDate) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(currentDate);
			int lastDate = cal.getActualMaximum(Calendar.DATE);
			cal.set(Calendar.DAY_OF_MONTH, lastDate);
			Date lastDayofMonth = cal.getTime();
			return lastDayofMonth;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some error occured while get month last date" + e.getMessage());
		}
	}

	public JSONArray getFilterTourRequest(Object employeeid, Object attendanceDateObject) {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONArray filteredTourRequestArray = new JSONArray();

		try {
			if (employeeid != null && attendanceDateObject != null) {
				Date attendanceDate = CommunicationUtility.checkDateFormat(attendanceDateObject);
				Date monthFirstDate = getMonthFirstDate(attendanceDate);
				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				String firstDate = updateDateFormat.format(monthFirstDate);
				Date monthLastDate = getMonthLastDate(attendanceDate);
				String lastDate = updateDateFormat.format(monthLastDate);
				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_tourrequests");
				JSONArray columnArray = new JSONArray();
				columnArray.put("__key__");
				query.put(Data.Query.COLUMNS, columnArray);
				query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and tourstatusid = " + HRISConstants.TOUR_APPROVED + " and startdate >= '" + firstDate + "' and startdate <= '" + lastDate + "'");
				JSONObject object = new JSONObject();
				object = ResourceEngine.query(query);
				JSONArray tourRequestArray = object.getJSONArray("hris_tourrequests");
				int tourRequestCount = (tourRequestArray == null || tourRequestArray.length() == 0) ? 0 : tourRequestArray.length();
				if (tourRequestCount > 0) {
					for (int counter = 0; counter < tourRequestCount; counter++) {
						Object tourRequestId = tourRequestArray.getJSONObject(counter).opt("__key__");
						if (tourRequestId != null) {
							filteredTourRequestArray.put(tourRequestId);
						}
					}
				}
				query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_tourrequests");
				columnArray = new JSONArray();
				columnArray.put("__key__");
				query.put(Data.Query.COLUMNS, columnArray);
				query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and tourstatusid = " + HRISConstants.TOUR_APPROVED + " and enddate >= '" + firstDate + "' and enddate <= '" + lastDate + "'");
				object = ResourceEngine.query(query);
				JSONArray tourArray = object.getJSONArray("hris_tourrequests");
				int tourArrayCount = (tourArray == null || tourArray.length() == 0) ? 0 : tourArray.length();
				if (tourArrayCount > 0) {
					for (int counter = 0; counter < tourArrayCount; counter++) {
						Object tourRequestId = tourArray.getJSONObject(counter).opt("__key__");
						if (tourRequestId != null) {
							filteredTourRequestArray.put(tourRequestId);
						}
					}
				} else { // if no tour request found in this duration
					filteredTourRequestArray.put("tourcode@garbage");
				}
			} else {
				filteredTourRequestArray.put("tourcode@garbage");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some error occured while get Tour Code" + e.getMessage());
		}
		return filteredTourRequestArray;
	}

	public JSONArray getFilterLeaveRequest(Object employeeid, Object attendanceDateObject) {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONArray filteredLeaveRequestArray = new JSONArray();

		try {
			if (employeeid != null && attendanceDateObject != null) {

				Date attendanceDate = CommunicationUtility.checkDateFormat(attendanceDateObject);
				Date monthFirstDate = getMonthFirstDate(attendanceDate);
				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				String firstDate = updateDateFormat.format(monthFirstDate);
				Date monthLastDate = getMonthLastDate(attendanceDate);
				String lastDate = updateDateFormat.format(monthLastDate);
				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_leaverequests");
				JSONArray columnArray = new JSONArray();
				columnArray.put("__key__");
				query.put(Data.Query.COLUMNS, columnArray);
				query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and leavestatusid = " + HRISConstants.LEAVE_APPROVED + " and fromdate >= '" + firstDate + "' and fromdate <= '" + lastDate + "'");
				JSONObject object = new JSONObject();
				object = ResourceEngine.query(query);
				JSONArray leaveRequestArray = object.getJSONArray("hris_leaverequests");
				int leaveRequestCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
				if (leaveRequestCount > 0) {
					for (int counter = 0; counter < leaveRequestCount; counter++) {
						Object leaveRequestId = leaveRequestArray.getJSONObject(counter).opt("__key__");
						if (leaveRequestId != null) {
							filteredLeaveRequestArray.put(leaveRequestId);
						}
					}
				}
				query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_leaverequests");
				columnArray = new JSONArray();
				columnArray.put("__key__");
				query.put(Data.Query.COLUMNS, columnArray);
				query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and leavestatusid = " + HRISConstants.LEAVE_APPROVED + " and todate >= '" + firstDate + "' and todate <= '" + lastDate + "'");
				object = ResourceEngine.query(query);
				JSONArray leaveArray = object.getJSONArray("hris_leaverequests");
				int leaveArrayCount = (leaveArray == null || leaveArray.length() == 0) ? 0 : leaveArray.length();
				if (leaveArrayCount > 0) {
					for (int counter = 0; counter < leaveArrayCount; counter++) {
						Object leaveRequestId = leaveArray.getJSONObject(counter).opt("__key__");
						if (leaveRequestId != null) {
							filteredLeaveRequestArray.put(leaveRequestId);
						}
					}
				} else { // if no leave request found in this duration
					filteredLeaveRequestArray.put("leavecode@garbage");
				}
			} else {
				filteredLeaveRequestArray.put("leavecode@garbage");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some error occured while get leave code" + e.getMessage());
		}
		return filteredLeaveRequestArray;
	}
}
