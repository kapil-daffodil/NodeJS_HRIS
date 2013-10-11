package com.applane.vender_portal;

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

public class RfqVendorRateUpdatesBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		if (record.has("temp")) {
			return;
		}
		try {
			if (record.has("rate") && record.getValue("rate") != null) {
				updateRfqStatus(record.getValue("rfq_id"));
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("RFQ Rate exception Trace >> " + trace);
		}
	}

	private void updateRfqStatus(Object rfqKey) throws Exception {
		JSONObject columnUpdate = new JSONObject();
		columnUpdate.put(Updates.KEY, rfqKey);
		columnUpdate.put("status", 2);
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, columnUpdate);
		updates.put(Data.Update.RESOURCE, "poi_rfq");
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}
}
