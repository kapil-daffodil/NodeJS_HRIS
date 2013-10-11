package com.applane.vender_portal;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class PoiRfq implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			if (record.has("temp")) {
				return;
			}
			LogUtility.writeError("record >> " + record.getValues());
			Object rfqKey = record.getKey();
			Object vendors = record.getValue("vendors_id");
			Object poiRfqLineItems = record.getValue("poi_rfq_line_items");
			if (vendors instanceof JSONArray) {
				JSONArray vendorsArray = ((JSONArray) vendors);
				if (vendorsArray.length() > 0 && poiRfqLineItems instanceof JSONArray) {
					JSONArray lineItemArray = ((JSONArray) poiRfqLineItems);
					for (int vendorCounter = 0; vendorCounter < vendorsArray.length(); vendorCounter++) {
						Object vendorId = vendorsArray.opt(vendorCounter);
						for (int itemCounter = 0; itemCounter < lineItemArray.length(); itemCounter++) {
							Object itemKey = lineItemArray.getJSONObject(itemCounter).opt(Updates.KEY);
							insertVendors(rfqKey, vendorId, itemKey);
						}
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("RFQ After exception Trace >> " + trace);
		}
	}

	private void insertVendors(Object rfqKey, Object vendorId, Object itemKey) throws Exception {
		JSONObject columnUpdate = new JSONObject();
		columnUpdate.put("rfq_id", rfqKey);
		columnUpdate.put("vendor_id", vendorId);
		columnUpdate.put("rfq_line_items_id", itemKey);
		columnUpdate.put("temp", "temp");
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, columnUpdate);
		updates.put(Data.Update.RESOURCE, "poi_rfq_venrors_product_rate");
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {

	}

}
