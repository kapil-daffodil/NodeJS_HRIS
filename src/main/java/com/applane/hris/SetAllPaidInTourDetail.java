package com.applane.hris;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class SetAllPaidInTourDetail implements OperationJob {
	// job define on hris_tourexpenses,hris_travelexpense, hris_localconveyance, hris_otherexpenses
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();
		Object tourDetailId;
		Object oldPaidObject;
		Object newPaidObject;
		if (operation.equalsIgnoreCase("update")) {
			// get tourdetailid and paid from record
			tourDetailId = record.getValue("tourdetailid");
			oldPaidObject = record.getOldValue("paid");
			newPaidObject = record.getValue("paid");
			boolean isPaidOld = CommunicationUtility.booleanValue(oldPaidObject);
			boolean isPaidNew = CommunicationUtility.booleanValue(newPaidObject);
			if (!isPaidOld && isPaidNew) {
				if (hasAllPaymentDone(tourDetailId)) {
					try {
						JSONObject updates = new JSONObject();
						updates.put(Data.Query.RESOURCE, "hris_tourdetails");
						JSONObject row = new JSONObject();
						row.put("__key__", tourDetailId);
						row.put("allpaid", 1);
						updates.put(Data.Update.UPDATES, row);
						ResourceEngine.update(updates);
					} catch (Exception e) {
						e.printStackTrace();
						throw new BusinessLogicException("Error come while update" + e.getMessage());
					}
				}
			}
		}
	}

	public boolean hasAllPaymentDone(Object tourDetailId) {
		try {
			// check same above for hris_tourexpense
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_tourexpenses");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("paid");
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray tourExpenseArray = object.getJSONArray("hris_tourexpenses");
			int tourExpenseCount = tourExpenseArray == null ? 0 : tourExpenseArray.length();
			if (tourExpenseCount > 0) {
				for (int expenseCounter = 0; expenseCounter < tourExpenseCount; expenseCounter++) {
					// Object tourExpenseId = tourExpenseArray.getJSONObject(expenseCounter).opt("__key__");
					Object paidObject = tourExpenseArray.getJSONObject(expenseCounter).opt("paid");
					boolean paid;
					paid = CommunicationUtility.booleanValue(paidObject);
					if (!paid) {
						return false;
					}
				}
			}
			// end for hris_tourexpenses

			// check same above for hris_tourlodgingexpense
			JSONObject lodgingQuery = new JSONObject();
			lodgingQuery.put(Data.Query.RESOURCE, "hris_tourlodgingexpense");
			JSONArray lodgingExpenseColumns = new JSONArray();
			lodgingExpenseColumns.put("__key__");
			lodgingExpenseColumns.put("paid");
			lodgingQuery.put(Data.Query.COLUMNS, lodgingExpenseColumns);
			lodgingQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
			JSONObject lodgingObject;
			lodgingObject = ResourceEngine.query(lodgingQuery);
			JSONArray lodgingExpenseArray = lodgingObject.getJSONArray("hris_tourlodgingexpense");
			int lodgingExpenseArrayCount = lodgingExpenseArray == null ? 0 : lodgingExpenseArray.length();
			if (lodgingExpenseArrayCount > 0) {
				for (int expenseCounter = 0; expenseCounter < lodgingExpenseArrayCount; expenseCounter++) {
					// Object tourLodgingExpenseId = lodgingExpenseArray.getJSONObject(expenseCounter).opt("__key__");
					Object paidObject = lodgingExpenseArray.getJSONObject(expenseCounter).opt("paid");
					boolean paid;
					paid = CommunicationUtility.booleanValue(paidObject);
					if (!paid) {
						return false;
					}
				}
			}
			// end for hris_tourlodgingexpense

			// check same above for hris_travelexpense

			lodgingQuery = new JSONObject();
			lodgingQuery.put(Data.Query.RESOURCE, "hris_travelexpenses");
			lodgingExpenseColumns = new JSONArray();
			lodgingExpenseColumns.put("__key__");
			lodgingExpenseColumns.put("paid");
			lodgingQuery.put(Data.Query.COLUMNS, lodgingExpenseColumns);
			lodgingQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
			lodgingObject = ResourceEngine.query(lodgingQuery);
			JSONArray travelExpenseArray = lodgingObject.getJSONArray("hris_travelexpenses");
			int travelExpenseCount = travelExpenseArray == null ? 0 : travelExpenseArray.length();
			if (travelExpenseCount > 0) {
				for (int travelExpenseCounter = 0; travelExpenseCounter < travelExpenseCount; travelExpenseCounter++) {
					// Object travelExpenseId = travelExpenseArray.getJSONObject(travelExpenseCounter).opt("__key__");
					Object paidObject = travelExpenseArray.getJSONObject(travelExpenseCounter).opt("paid");
					boolean paid;
					paid = CommunicationUtility.booleanValue(paidObject);
					if (!paid) {
						return false;
					}
				}
			}

			// check same above for hris_localconveyance
			lodgingQuery = new JSONObject();
			lodgingQuery.put(Data.Query.RESOURCE, "hris_localconveyance");
			lodgingExpenseColumns = new JSONArray();
			lodgingExpenseColumns.put("__key__");
			lodgingExpenseColumns.put("paid");
			lodgingQuery.put(Data.Query.COLUMNS, lodgingExpenseColumns);
			lodgingQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
			lodgingObject = ResourceEngine.query(lodgingQuery);
			JSONArray localExpenseArray = lodgingObject.getJSONArray("hris_localconveyance");
			int localExpenseCount = localExpenseArray == null ? 0 : localExpenseArray.length();
			if (travelExpenseCount > 0) {
				for (int localExpenseCounter = 0; localExpenseCounter < localExpenseCount; localExpenseCounter++) {
					// Object localConyenceId = localExpenseArray.getJSONObject(localExpenseCounter).opt("__key__");
					Object paidObject = localExpenseArray.getJSONObject(localExpenseCounter).opt("paid");
					boolean paid;
					paid = CommunicationUtility.booleanValue(paidObject);
					if (!paid) {
						return false;
					}
				}
			}

			// check same above for hris_otherexpenses
			lodgingQuery = new JSONObject();
			lodgingQuery.put(Data.Query.RESOURCE, "hris_otherexpenses");
			lodgingExpenseColumns = new JSONArray();
			lodgingExpenseColumns.put("__key__");
			lodgingExpenseColumns.put("paid");
			lodgingQuery.put(Data.Query.COLUMNS, lodgingExpenseColumns);
			lodgingQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
			lodgingObject = ResourceEngine.query(lodgingQuery);
			JSONArray otherExpenseArray = lodgingObject.getJSONArray("hris_otherexpenses");
			int otherExpenseCount = otherExpenseArray == null ? 0 : otherExpenseArray.length();
			if (travelExpenseCount > 0) {
				for (int otherExpenseCounter = 0; otherExpenseCounter < otherExpenseCount; otherExpenseCounter++) {
					// Object otherExpenseId = otherExpenseArray.getJSONObject(otherExpenseCounter).opt("paid");
					Object paidObject = otherExpenseArray.getJSONObject(otherExpenseCounter).opt("paid");
					boolean paid;
					paid = CommunicationUtility.booleanValue(paidObject);
					if (!paid) {
						return false;
					}
				}
			}

			// if no unpaid record found than mark AllPaid of TourDetail to true
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error come" + e.getMessage());
		}
	}

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

}
