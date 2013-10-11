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
 * @category HRIS businesslogic job define on hris_tourexpense, hris_travelexpense, hris_localconeyanceexpenses, hris_otherexpenses and hris_tourlodgingexpenses
 */

public class SetAllPaidFalseInTourRequestAndTourDetail implements OperationJob {

	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {

			Object tourDetailId = record.getValue("tourdetailid");
			// Object ExpenseId = record.getValue("__key__");
			Object approvedObject = record.getValue("approved");
			boolean approved = CommunicationUtility.booleanValue(approvedObject);
			if (approved) {
				try {
					// fire query on hris_tourdetails where tourdetailid = above
					JSONObject tourQuery = new JSONObject();
					tourQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
					JSONArray tourColumnArray = new JSONArray();
					tourColumnArray.put("tourrequestid");
					tourColumnArray.put("tourrequestid.paidall");
					tourColumnArray.put("allpaid");
					tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
					tourQuery.put(Data.Query.FILTERS, "__key__ = " + tourDetailId);
					JSONObject tourObject;
					tourObject = ResourceEngine.query(tourQuery);
					JSONArray tourDetailArray = tourObject.getJSONArray("hris_tourdetails");
					int tourDetailArrayCount = tourDetailArray == null ? 0 : tourDetailArray.length();
					if (tourDetailArrayCount > 0) {
						// check for tour detail paid status
						Object allPaidObject = tourDetailArray.getJSONObject(0).opt("allpaid");
						boolean tourDetailAllPaid = CommunicationUtility.booleanValue(allPaidObject);
						if (tourDetailAllPaid) {
							// than only set it false
							JSONObject updates = new JSONObject();
							updates.put(Data.Query.RESOURCE, "hris_tourdetails");
							JSONObject row = new JSONObject();
							row.put("__key__", tourDetailId);
							row.put("allpaid", 0);
							updates.put(Data.Update.UPDATES, row);
							ResourceEngine.update(updates);
						}

						// check for tour request paid status
						Object tourRequestAllPaidObject = tourDetailArray.getJSONObject(0).opt("tourrequestid.paidall");
						Object tourRequestId = tourDetailArray.getJSONObject(0).opt("tourrequestid");
						boolean tourRequestAllPaid = CommunicationUtility.booleanValue(tourRequestAllPaidObject);
						if (tourRequestAllPaid) {
							// than only set it false
							JSONObject updates = new JSONObject();
							updates.put(Data.Query.RESOURCE, "hris_tourrequests");
							JSONObject row = new JSONObject();
							row.put("__key__", tourRequestId);
							row.put("paidall", 0);
							updates.put(Data.Update.UPDATES, row);
							ResourceEngine.update(updates);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BusinessLogicException("Error come while update" + e.getMessage());
				}
			}

		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
