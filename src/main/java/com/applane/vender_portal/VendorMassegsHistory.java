package com.applane.vender_portal;

import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.moduleimpl.shared.constants.ModuleConstants.Udt.Date;
import com.google.apphosting.api.DeadlineExceededException;

public class VendorMassegsHistory implements OperationJob {

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			Object key = record.getKey();
			// Object subject = record.getValue("subject");
			Object messages = null;
			Object messagesSupport = null;
			if (record.getOperationType().equals(Updates.Types.INSERT)) {
				messages = record.getValue("message");
				messagesSupport = record.getValue("message_support");
			} else if (record.getOperationType().equals(Updates.Types.UPDATE)) {
				messages = record.getNewValue("message");
				messagesSupport = record.getNewValue("message_support");

			}
			Object vId = record.getValue("vender_id");
			Object sId = record.getValue("support_id");
			Object vName = record.getValue("vender_id.orgname");
			Object sName = record.getValue("support_id.name");
			Object fromName = "";
			Object toName = "";
			if (messages != null) {
				fromName = vName;
				toName = sName;
			}
			if (messagesSupport != null) {
				fromName = sName;
				toName = vName;
			}
			insertHistory(key, messages, messagesSupport, fromName, toName, vId, sId);
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("VendorMassegsHistory >> Exception Trace >> [" + trace + "]");
		}
	}

	private void insertHistory(Object key, Object messagesVendor, Object messagesSupport, Object fromName, Object toName, Object vId, Object sId) throws Exception {
		JSONObject updateColumns = new JSONObject();
		updateColumns.put("messages_id", key);
		updateColumns.put("vendor_id", vId);
		updateColumns.put("support_id", sId);
		updateColumns.put("from_name", fromName);
		updateColumns.put("to_name", toName);
		updateColumns.put("temp", "temp");
		Object message = "";
		if (messagesVendor != null) {
			message = messagesVendor;
			updateColumns.put("message_vendor", messagesVendor);
		} else if (messagesSupport != null) {
			message = messagesSupport;
			updateColumns.put("message_support", messagesSupport);
		}
		updateColumns.put("message", message);
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, updateColumns);
		updates.put(Data.Update.RESOURCE, "poi_messages_history");
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	public static void populateMailConversationBySupportPerson(Object subject, Object message, Object vendorId, Object senderId, Date date, String category) throws JSONException {
		JSONObject updateColumns = new JSONObject();
		updateColumns.put("subject", subject);
		updateColumns.put("message_support", message);
		updateColumns.put("message", message);
		updateColumns.put("vender_id", vendorId);
		updateColumns.put("support_id", senderId);
		updateColumns.put("category", category);
		updateColumns.put("message_date", date == null ? SystemParameters.getCurrentDate() : date);
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, updateColumns);
		updates.put(Data.Update.RESOURCE, "poi_messages");
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static void populateMailConversationByVendor(Object subject, Object message, Object vendorId, Object senderId, Date date, String category) throws JSONException {
		JSONObject updateColumns = new JSONObject();
		updateColumns.put("subject", subject);
		updateColumns.put("message", message);
		updateColumns.put("vender_id", vendorId);
		updateColumns.put("support_id", senderId);
		updateColumns.put("message_date", date == null ? SystemParameters.getCurrentDate() : date);
		updateColumns.put("category", category);
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, updateColumns);
		updates.put(Data.Update.RESOURCE, "poi_messages");
		LogUtility.writeLog("updates>>" + updates);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		Object messages = null;
		Object messagesSupport = null;
		if (record.getOperationType().equals(Updates.Types.INSERT)) {
			messages = record.getValue("message");
			messagesSupport = record.getValue("message_support");
			if (messages != null) {
				record.addUpdate("Status", "Sent");
			} else if (messagesSupport != null) {
				record.addUpdate("Status", "Received");
			}
		}
	}

}
