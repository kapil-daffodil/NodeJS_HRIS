package com.applane.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */
public class LeavePolicyValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		// Handle leave policy From date and To dates
		try {
			Object updatedLeaveRuleDetails = record.getValue("hris_leaverule");

			JSONArray updatedLeaveRuleArray = new JSONArray();
			if (updatedLeaveRuleDetails != null && updatedLeaveRuleDetails instanceof JSONArray) {
				updatedLeaveRuleArray = (JSONArray) updatedLeaveRuleDetails;
			} else if (updatedLeaveRuleDetails != null) {
				updatedLeaveRuleArray.put(updatedLeaveRuleDetails);
			}

			// leavePolicyDateValidation(record);

			// Handle No. of Leave must be equal to total No. of Leaves
			if (updatedLeaveRuleArray.length() > 0) {
				Object temp = updatedLeaveRuleArray.getJSONObject(0).opt("temp");
				if (temp == null) {
					leavePolicyLeavesValidation(record);
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("Leave Rule Validation onBeforeUpdate Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException(e.getMessage());
			// throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
	}

	public static String dateInString(Date date) {
		if (date != null) {
			return new SimpleDateFormat("dd MMM yyyy").format(date);
		} else {
			return "";
		}
	}

	private void leavePolicyLeavesValidation(Record record) throws JSONException, ParseException {
		Object leavePolicyId = record.getValue("__key__");
		String operation = record.getOperationType();

		if (operation.equalsIgnoreCase("insert")) {
			try {
				Object leaveruleDetails = record.getValue("hris_leaverule");
				JSONArray leaveRuleArray = new JSONArray();
				if (leaveruleDetails != null && leaveruleDetails instanceof JSONArray) {
					leaveRuleArray = (JSONArray) leaveruleDetails;
				} else if (leaveruleDetails != null) {
					leaveRuleArray.put(leaveruleDetails);
				}
				if (leaveRuleArray.length() > 0) {
					for (int counter = 0; counter < leaveRuleArray.length(); counter++) {
						Object fromDateObject = leaveRuleArray.getJSONObject(counter).opt("fromdate");
						Object toDateObject = leaveRuleArray.getJSONObject(counter).opt("todate");
						if (toDateObject != null) {
							Calendar cal1 = Calendar.getInstance();
							Calendar cal2 = Calendar.getInstance();
							cal1.setTime(Translator.dateValue(fromDateObject));
							cal2.setTime(Translator.dateValue(toDateObject));
							if (cal1.after(cal2)) {
								throw new BusinessLogicException("'From Date' Should be Less or Equal to 'To Date'");
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (operation.equalsIgnoreCase("update")) {
			Object updatedLeaveRuleDetails = record.getValue("hris_leaverule");
			Object updatedLeaveRuleDetailsOldValues = record.getOldValue("hris_leaverule");
			JSONArray updatedLeaveRuleArray = new JSONArray();
			if (updatedLeaveRuleDetails != null && updatedLeaveRuleDetails instanceof JSONArray) {
				updatedLeaveRuleArray = (JSONArray) updatedLeaveRuleDetails;
			} else if (updatedLeaveRuleDetails != null) {
				updatedLeaveRuleArray.put(updatedLeaveRuleDetails);
			}
			JSONArray updatedLeaveRuleArrayOldValues = new JSONArray();
			if (updatedLeaveRuleDetailsOldValues != null && updatedLeaveRuleDetailsOldValues instanceof JSONArray) {
				updatedLeaveRuleArrayOldValues = (JSONArray) updatedLeaveRuleDetailsOldValues;
			} else if (updatedLeaveRuleDetailsOldValues != null) {
				updatedLeaveRuleArrayOldValues.put(updatedLeaveRuleDetailsOldValues);
			}

			if (updatedLeaveRuleArray.length() > 0) {
				for (int counter = 0; counter < updatedLeaveRuleArray.length(); counter++) {
					Object __type__ = updatedLeaveRuleArray.getJSONObject(counter).opt("__type__");
					if (__type__ == null || !__type__.toString().equalsIgnoreCase("delete")) {
						Object leaveRuleId = updatedLeaveRuleArray.getJSONObject(counter).opt("__key__");
						Object fromDateObject = updatedLeaveRuleArray.getJSONObject(counter).opt("fromdate");
						if (fromDateObject == null && !updatedLeaveRuleArray.getJSONObject(counter).has("fromdate")) {
							for (int innerCounter = 0; innerCounter < updatedLeaveRuleArrayOldValues.length(); innerCounter++) {
								Object oldLeaveRuleId = updatedLeaveRuleArrayOldValues.getJSONObject(innerCounter).opt("__key__");
								if (oldLeaveRuleId.toString().equals(leaveRuleId.toString())) {
									fromDateObject = updatedLeaveRuleArrayOldValues.getJSONObject(innerCounter).opt("fromdate");
								}
							}
						}

						Date toDateObject = Translator.dateValue(updatedLeaveRuleArray.getJSONObject(counter).opt("todate"));
						if (toDateObject != null) {
							if (Translator.dateValue(fromDateObject).after(toDateObject)) {
								throw new BusinessLogicException("'From Date' Should be Less or Equal to 'To Date'");
							}
						}
						Object leaveTypeId = updatedLeaveRuleArray.getJSONObject(counter).opt("leavetypeid");
						if (leaveTypeId == null && !updatedLeaveRuleArray.getJSONObject(counter).has("leavetypeid")) {
							for (int innerCounter = 0; innerCounter < updatedLeaveRuleArrayOldValues.length(); innerCounter++) {
								Object oldLeaveRuleId = updatedLeaveRuleArrayOldValues.getJSONObject(innerCounter).opt("__key__");
								if (oldLeaveRuleId.toString().equals(leaveRuleId.toString())) {
									leaveTypeId = updatedLeaveRuleArrayOldValues.getJSONObject(innerCounter).opt("leavetypeid");
								}
							}
						}
						JSONArray previousKeyForSameLeaveArray = getPreviousKeyForSameLeaveTypeId(leaveRuleId, leavePolicyId, leaveTypeId);
						if (previousKeyForSameLeaveArray != null && previousKeyForSameLeaveArray.length() > 0) {
							Object fromDateOfPrevious = previousKeyForSameLeaveArray.getJSONObject(0).opt("fromdate");
							Calendar cal1 = Calendar.getInstance();
							Calendar cal2 = Calendar.getInstance();
							cal1.setTime(Translator.dateValue(fromDateObject));
							cal2.setTime(Translator.dateValue(fromDateOfPrevious));
							if (cal1.after(cal2)) {
								updateToDateForPreviousRecord(previousKeyForSameLeaveArray.getJSONObject(0).opt("__key__"), fromDateObject);
							} else {
								throw new BusinessLogicException("'From Date' [" + fromDateObject + "] Should be different from Previous Same Leave Type's From Date");
							}
						}
					}
				}
			}
		}
	}

	private void updateToDateForPreviousRecord(Object previousKeyForSameLeaveTypeId, Object fromDateObject) throws ParseException, JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		String backDate = DataTypeUtilities.getBackDate(fromDateObject.toString());
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONObject row = new JSONObject();
		row.put("__key__", previousKeyForSameLeaveTypeId);
		row.put("todate", backDate);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);

	}

	private JSONArray getPreviousKeyForSameLeaveTypeId(Object leaveRuleId, Object leavePolicyId, Object leaveTypeId) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		String filter = "";
		if (!(leaveRuleId instanceof JSONObject)) {
			filter += " and __key__ != " + leaveRuleId;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("fromdate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyId + " and leavetypeid = " + leaveTypeId + filter);

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "__key__");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		JSONArray orderByExpression = new JSONArray();
		orderByExpression.put(order);
		query.put(Data.Query.ORDERS, orderByExpression);
		query.put(Data.Query.MAX_ROWS, 1);

		JSONObject leaveRuleObject = ResourceEngine.query(query);
		JSONArray leaveRuleArray = leaveRuleObject.getJSONArray("hris_leaverule");
		if (leaveRuleArray.length() > 0) {
			return leaveRuleArray;
		} else {
			return null;
		}
	}

	// private int getKey(Object keyObject) {
	// Integer key = null;
	// try {
	// if (keyObject instanceof JSONObject) {
	// key = ((JSONObject) keyObject).getInt("key");
	// } else {
	// key = (Integer) keyObject;
	// }
	// } catch (Exception e) {
	// LogUtility.writeLog("Erorr while fetching updated No. of Leaves" + e.getMessage());
	// }
	// return key;
	// }

	// private Date addMonthsInDate(Date fromDate) {
	//
	// Calendar cal = Calendar.getInstance();
	// cal.setTime(fromDate);
	// int monthNumber = cal.get(Calendar.MONTH) + 1;
	// cal.add(Calendar.MONTH, 12);
	// Date afterIncreemntDate = cal.getTime();
	// cal.setTime(afterIncreemntDate);
	// cal.add(Calendar.DAY_OF_MONTH, -1);
	//
	// Date finalDate = cal.getTime();
	// return finalDate;
	// }
}
