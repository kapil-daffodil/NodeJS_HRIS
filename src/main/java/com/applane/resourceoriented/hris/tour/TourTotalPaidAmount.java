package com.applane.resourceoriented.hris.tour;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic job define on tourexpense, travelexpense, otherexpense, localconveyance and tourlodgingexpense
 */

public class TourTotalPaidAmount implements OperationJob {

	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

		Object tourDetailId = record.getValue("tourdetailid");
		JSONObject paidAmountObject = (JSONObject) record.getValue("paidamount");
		JSONObject paidOldAmountObject = (JSONObject) record.getOldValue("approveamount");
		if (paidAmountObject != null && paidAmountObject != paidOldAmountObject) {
			Number paidAmount = (Number) (paidAmountObject.opt("amount") == null ? 0 : paidAmountObject.opt("amount"));
			JSONObject currencyJsonObject = (JSONObject) paidAmountObject.opt("type");
			// String paidAmountType = currencyJsonObject.optString("currency");
			int paidAmountTypeId = currencyJsonObject.optInt("__key__");
			try {
				JSONObject tourDetailQuery = new JSONObject();
				tourDetailQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
				JSONArray tourDetailColumnArray = new JSONArray();
				tourDetailColumnArray.put("tourrequestid");
				tourDetailQuery.put(Data.Query.COLUMNS, tourDetailColumnArray);
				tourDetailQuery.put(Data.Query.FILTERS, "__key__ = " + tourDetailId);
				JSONObject tourObject;
				tourObject = ResourceEngine.query(tourDetailQuery);
				JSONArray tourDetailArray = tourObject.getJSONArray("hris_tourdetails");
				int tourDetailArrayCount = tourDetailArray == null ? 0 : tourDetailArray.length();
				if (tourDetailArrayCount > 0) {
					Object tourRequestId = tourDetailArray.getJSONObject(0).opt("tourrequestid");
					JSONObject tourRequestQuery = new JSONObject();
					tourRequestQuery.put(Data.Query.RESOURCE, "hris_tourrequests");
					JSONArray tourRequestColumnArray = new JSONArray();
					JSONObject totalApprovedAmountColumnJson = new JSONObject();
					totalApprovedAmountColumnJson.put("expression", "totalpaidamount");
					totalApprovedAmountColumnJson.put("type", "currency");
					tourRequestColumnArray.put(totalApprovedAmountColumnJson);
					tourRequestQuery.put(Data.Query.COLUMNS, tourRequestColumnArray);
					tourRequestQuery.put(Data.Query.FILTERS, "__key__ = " + tourRequestId);
					JSONObject tourRequestObject;
					tourRequestObject = ResourceEngine.query(tourRequestQuery);
					JSONArray tourRequestArray = tourRequestObject.getJSONArray("hris_tourrequests");

					int tourRequestArrayCount = tourRequestArray == null ? 0 : tourRequestArray.length();

					Double totalPaidAmount = 0.0;
					// String totalPaidAmountType = null;

					if (tourRequestArrayCount > 0) {
						JSONObject totalPaidAmountObject = (JSONObject) tourRequestArray.getJSONObject(0).opt("totalpaidamount");
						if (totalPaidAmountObject != null) {
							totalPaidAmount = (Double) totalPaidAmountObject.get("amount");
							// JSONObject totalPsidAmountTypeObject = (JSONObject) totalPaidAmountObject.get("type");
							// totalPaidAmountType = (String) totalPsidAmountTypeObject.get("currency");
							totalPaidAmount = totalPaidAmount + paidAmount.doubleValue();
							;
						} else {
							totalPaidAmount = totalPaidAmount + paidAmount.doubleValue();
							;
						}
						JSONObject updates = new JSONObject();
						updates.put(Data.Query.RESOURCE, "hris_tourrequests");
						JSONObject row = new JSONObject();
						row.put("__key__", tourRequestId);
						row.put("totalpaidamount_amount", totalPaidAmount);
						row.put("totalpaidamount_type", paidAmountTypeId);
						updates.put(Data.Update.UPDATES, row);
						ResourceEngine.update(updates);
					}
				}
			} catch (Exception e) {
				throw new BusinessLogicException("Error while update amount" + e.getMessage());
			}
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
