package com.applane.resourceoriented.hris.tour;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class SMSCodeGeneration implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		if (record.has("temp")) {
			return;
		}
		String resourceName = record.getTable().getTableName();
		String CURRENT_ORGANIZATION = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION);
		Object CURRENT_ORGANIZATION_ID = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);

		Object smsCodeObject = record.getOldValue("smscode");

		if ((smsCodeObject == null || ((String) smsCodeObject).length() == 0)) {
			try {
				long generatedSmsCode = generateSmsCode(resourceName, CURRENT_ORGANIZATION_ID, CURRENT_ORGANIZATION);
				record.addUpdate("smscode", generatedSmsCode);
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Error Come while generate SMS Code." + e.getMessage());
			}
		}
	}

	private long generateSmsCode(String resource, Object organizationId, String organization) {

		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_smscodes");
			JSONArray array = new JSONArray();
			array.put("__key__");
			array.put("code");
			query.put(Data.Query.COLUMNS, array);

			/*
			 * JSONObject parameters = new JSONObject(); query.put(Data.Query.FILTERS, "resource= {resourceName} and organization ={organizationName} and academicyearid={academicyearid}"); parameters.put("resourceName", resource); parameters.put("organizationName", organization);
			 */
			JSONObject order = new JSONObject();
			order.put(Data.Query.Orders.EXPERSSION, "code");
			order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
			JSONArray orders = new JSONArray();
			orders.put(order);
			query.put(Data.Query.ORDERS, orders);
			query.put(Data.Query.MAX_ROWS, 1);

			JSONObject smsCodeObject;
			smsCodeObject = ResourceEngine.query(query);
			JSONArray smsCodeArray = smsCodeObject.getJSONArray("organization_smscodes");
			int smsCodeArrayCount = (smsCodeArray == null || smsCodeArray.length() == 0) ? 0 : smsCodeArray.length();
			Object smsCodeId = null;
			long smsCode = 0;
			if (smsCodeArrayCount > 0) {
				smsCodeId = smsCodeArray.getJSONObject(0).opt("__key__");
				smsCode = (Integer) smsCodeArray.getJSONObject(0).opt("code");

				if (smsCodeId != null) {
					smsCode = smsCode + 1;
					JSONObject updates = new JSONObject();
					updates.put(Data.Query.RESOURCE, "organization_smscodes");
					JSONObject row = new JSONObject();
					row.put("code", smsCode);
					row.put("organizationid", organizationId);
					row.put("organizationname", organization);
					row.put("resource", resource);
					updates.put(Data.Update.UPDATES, row);
					ResourceEngine.update(updates);
				}
			} else {
				smsCode = 1;
				JSONObject updates = new JSONObject();
				updates.put(Data.Query.RESOURCE, "organization_smscodes");
				JSONObject row = new JSONObject();
				row.put("code", smsCode);
				row.put("organizationid", organizationId);
				row.put("organizationname", organization);
				row.put("resource", resource);
				updates.put(Data.Update.UPDATES, row);
				ResourceEngine.update(updates);
			}
			return smsCode;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error Come while generate Code." + e.getMessage());
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

}
