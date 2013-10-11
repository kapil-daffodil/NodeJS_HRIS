package com.applane.hris;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HrisUserDefinedFunctions;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class AssignAssetsValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

		Object assignDateObject = record.getOldValue("assigningdate");
		Object returnDateObject = record.getValue("returndate");
		String operation = record.getOperationType();

		Date assignDate;
		Date returnDate;
		try {
			Object employeeId = record.getValue("employeeid");
			if (employeeId == null) {
				employeeId = record.getValue("employeeid.__key__");
			}
			Object assetId = record.getValue("procurement_assetcategories_id");
			JSONArray assetsArray = getAssetsArray(employeeId, assetId);
			if (operation.equalsIgnoreCase(Updates.Types.INSERT)) {
				int quantity = Translator.integerValue(record.getValue("quantity"));
				if (quantity > 0) {
					for (int counter = 0; counter < assetsArray.length(); counter++) {
						int key = Translator.integerValue(assetsArray.getJSONObject(counter).opt(Updates.KEY));
						int quantityAdded = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantityadded"));
						int quantityRemained = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantityremained"));
						int quantityMoved = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantitymoved"));
						quantityRemained = (quantityAdded - quantityMoved);
						if (quantityRemained >= quantity) {
							quantityRemained -= quantity;
							quantityMoved += quantity;
							quantity = 0;
							updateInventoryQuantity(key, quantityRemained, quantityMoved);
							break;
						} else {
							quantity -= quantityRemained;
							quantityMoved += quantityRemained;
							quantityRemained = 0;
							updateInventoryQuantity(key, quantityRemained, quantityMoved);
						}
					}
				}
			}

			if (operation.equalsIgnoreCase("update")) {
				if (record.has("quantity")) {
					int quantityNew = Translator.integerValue(record.getNewValue("quantity"));
					int quantityOld = Translator.integerValue(record.getOldValue("quantity"));
					if (quantityNew != quantityOld) {
						int quantity = 0;
						if (quantityNew > quantityOld) {
							quantity = quantityNew - quantityOld;
							if (quantity > 0) {
								for (int counter = 0; counter < assetsArray.length(); counter++) {
									int key = Translator.integerValue(assetsArray.getJSONObject(counter).opt(Updates.KEY));
									int quantityAdded = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantityadded"));
									int quantityRemained = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantityremained"));
									int quantityMoved = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantitymoved"));
									quantityRemained = (quantityAdded - quantityMoved);
									if (quantityRemained >= quantity) {
										quantityRemained -= quantity;
										quantityMoved += quantity;
										quantity = 0;
										updateInventoryQuantity(key, quantityRemained, quantityMoved);
										break;
									} else {
										quantity -= quantityRemained;
										quantityMoved += quantityRemained;
										quantityRemained = 0;
										updateInventoryQuantity(key, quantityRemained, quantityMoved);
									}
								}
							}
						} else {
							quantity = quantityOld - quantityNew;
							if (assetsArray != null && assetsArray.length() > 0) {
								for (int counter = 0; counter < assetsArray.length(); counter++) {
									int key = Translator.integerValue(assetsArray.getJSONObject(counter).opt(Updates.KEY));
									// int quantityAdded = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantityadded"));
									int quantityRemained = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantityremained"));
									int quantityMoved = Translator.integerValue(assetsArray.getJSONObject(counter).opt("quantitymoved"));
									if (quantityMoved >= quantity) {
										quantityMoved -= quantity;
										quantityRemained += quantity;
										quantity = 0;
										updateInventoryQuantity(key, quantityRemained, quantityMoved);
										break;
									} else {
										quantity -= quantityMoved;
										quantityRemained += quantityMoved;
										quantityMoved = 0;
										updateInventoryQuantity(key, quantityRemained, quantityMoved);
									}
								}
							}
						}
					}
				}
				if (record.has("returndate")) {
					if (assignDateObject != null && returnDateObject != null) {
						assignDate = CommunicationUtility.checkDateFormat(assignDateObject);
						returnDate = CommunicationUtility.checkDateFormat(returnDateObject);
						if (returnDate.before(assignDate)) {
							throw new BusinessLogicException("Return date must be greater than OR equal to assign date.");
						}
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AssignAssetsValidation Exception >> trace >> " + trace);
			throw new RuntimeException(trace);
		}
	}

	private void updateInventoryQuantity(int key, int quantityRemained, int quantityMoved) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "poi_inventory");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put("quantityremained", quantityRemained);
		row.put("quantitymoved", quantityMoved);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	public static JSONArray getAssetsArray(Object empId, Object assetId) {
		Object branchId = HrisUserDefinedFunctions.getBranchId(empId);
		try {
			if (empId != null) {

				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "poi_inventory");
				JSONArray array = new JSONArray();
				array.put(Updates.KEY);
				array.put("productid");
				array.put("quantityadded");
				array.put("quantityremained");
				array.put("quantitymoved");

				query.put(Data.Query.COLUMNS, array);

				query.put(Data.Query.FILTERS, "quantityadded > 0 AND quantityremained >0 AND locationid = " + branchId + " AND productid = " + assetId);
				query.put(Data.Query.OPTION_FILTERS, "quantityadded > 0 AND quantityremained = null AND locationid = " + branchId + " AND productid = " + assetId);

				JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
				return object.getJSONArray("poi_inventory");
			} else {
				throw new BusinessLogicException("Employee Can not be null.");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(HrisUserDefinedFunctions.class.getName(), e);
			LogUtility.writeLog("HrisUserDefinedFunctions >> getCurrentEmployeeId >> trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured");
		}
	}
}
