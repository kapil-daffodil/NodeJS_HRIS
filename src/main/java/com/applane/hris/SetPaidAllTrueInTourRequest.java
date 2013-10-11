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
 * @category HRIS businesslogic Job define on hris_tourdetails
 */

public class SetPaidAllTrueInTourRequest implements OperationJob {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		// job define on "hris_tourdetails"
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			Object tourDetailId = record.getValue("__key__");
			Object allPaidObject = record.getValue("allpaid");
			boolean allPaid = CommunicationUtility.booleanValue(allPaidObject);
			if (allPaid) {
				try {
					JSONObject tourQuery = new JSONObject();
					tourQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
					JSONArray tourColumnArray = new JSONArray();
					tourColumnArray.put("tourrequestid");
					tourColumnArray.put("allpaid");
					tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
					tourQuery.put(Data.Query.FILTERS, "__key__ = " + tourDetailId);
					JSONObject tourObject;
					tourObject = ResourceEngine.query(tourQuery);
					JSONArray tourDetailArray = tourObject.getJSONArray("hris_tourdetails");
					// int tourDetailArrayCount = tourDetailArray == null ? 0 : tourDetailArray.length();
					Object tourRequestId = tourDetailArray.getJSONObject(0).opt("tourrequestid");
					if (tourRequestId == null) {
						throw new BusinessLogicException("No Tour Request Found for Tour Detail");
					} else {
						tourQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
						JSONArray tourArray = new JSONArray();
						tourArray.put("allpaid");
						tourQuery.put(Data.Query.COLUMNS, tourArray);
						tourQuery.put(Data.Query.FILTERS, "tourrequestid = " + tourRequestId);
						JSONObject object;
						object = ResourceEngine.query(tourQuery);
						JSONArray rows = object.getJSONArray("hris_tourdetails");
						int tourDetailCount = rows == null ? 0 : rows.length();

						for (int tourDetailCounter = 0; tourDetailCounter < tourDetailCount; tourDetailCounter++) {
							Object tourDetailAllPaidObject = rows.getJSONObject(tourDetailCounter).opt("allpaid");
							boolean tourDetailAllPaid = CommunicationUtility.booleanValue(tourDetailAllPaidObject);
							if (!tourDetailAllPaid) {
								return;
							}
						}
						// update paid all of tour request to true
						JSONObject updates = new JSONObject();
						updates.put(Data.Query.RESOURCE, "hris_tourrequests");
						JSONObject row = new JSONObject();
						row.put("__key__", tourRequestId);
						row.put("paidall", 1);
						updates.put(Data.Update.UPDATES, row);
						ResourceEngine.update(updates);
					}
				} catch (Exception e) {
					throw new BusinessLogicException("Error come while update" + e.getMessage());
				}
			}
		}
	}
}
