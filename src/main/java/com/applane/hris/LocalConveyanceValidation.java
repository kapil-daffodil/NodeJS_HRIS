package com.applane.hris;

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
 * @category HRIS businesslogic job define on hris_localconveyance
 */

public class LocalConveyanceValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

		// Handle Approived amount validation
		approvedAmontValidation(record);

		// Handle paidAmountValidation
		paidAmountValidation(record);
	}

	private void paidAmountValidation(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			Object localConveyanceExpenseId = record.getValue("__key__");
			JSONObject paidAmountObject = (JSONObject) record.getValue("paidamount");
			if (record.has("paidamount") && paidAmountObject != null) {
				JSONObject oldPaidAmountObject = (JSONObject) record.getOldValue("paidamount");
				if (record.has("paidamount")) {
					if (oldPaidAmountObject != null) {
						throw new BusinessLogicException("You have already enter Paid amount.");
					}
				}
				if (paidAmountObject != null) {
					ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
					try {
						Number paidAmount = (Number) (paidAmountObject.opt("amount") == null ? 0 : paidAmountObject.opt("amount"));
						JSONObject currencyJsonObject = (JSONObject) paidAmountObject.opt("type");
						String paidAmountType = currencyJsonObject.optString("currency");

						JSONObject query = new JSONObject();
						query.put(Data.Query.RESOURCE, "hris_localconveyance");
						JSONArray columns = new JSONArray();
						JSONObject expenseAmountColumnJson = new JSONObject();
						expenseAmountColumnJson.put("expression", "approveamount");
						expenseAmountColumnJson.put("type", "currency");
						columns.put(expenseAmountColumnJson);
						query.put(Data.Query.COLUMNS, columns);
						query.put(Data.Query.FILTERS, "__key__ = " + localConveyanceExpenseId);
						JSONObject object;
						object = ResourceEngine.query(query);
						JSONArray localExpenseArray = object.getJSONArray("hris_localconveyance");

						int localExpenseArrayCount = localExpenseArray == null ? 0 : localExpenseArray.length();
						if (localExpenseArrayCount > 0) {
							JSONObject approveAmountObject = (JSONObject) localExpenseArray.getJSONObject(0).opt("approveamount");
							if (approveAmountObject != null) {
								Number approveAmount = 0.0;
								String approveAmountType = null;
								approveAmount = (Number) approveAmountObject.opt("amount");
								JSONObject approveAmountTypeObject = (JSONObject) approveAmountObject.opt("type");
								approveAmountType = (String) approveAmountTypeObject.opt("currency");
								if (paidAmount.doubleValue() > approveAmount.doubleValue()) {
									throw new BusinessLogicException("Paid Amount must be equal to OR less than Approved Amount.");
								} else if (paidAmount.doubleValue() <= approveAmount.doubleValue() && (!paidAmountType.equalsIgnoreCase(approveAmountType))) {
									throw new BusinessLogicException("Paid Amount Type must be same as Approved Amount Type.");
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new BusinessLogicException("" + e.getMessage());
					}
					record.addUpdate("paid", 1);
				}
			}
		}
	}

	private void approvedAmontValidation(Record record) {
		Object localConveyanceId = record.getValue("__key__");
		JSONObject approvedAmountObject = (JSONObject) record.getValue("approveamount");

		if (record.has("approveamount") && approvedAmountObject != null) {
			JSONObject oldApprovedAmountObject = (JSONObject) record.getOldValue("approveamount");
			if (oldApprovedAmountObject != null) {
				throw new BusinessLogicException("You have already enter Approved Amount.");
			}

			if (approvedAmountObject != null) {
				ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
				try {
					Number approvedAmount = (Number) (approvedAmountObject.opt("amount") == null ? 0 : approvedAmountObject.opt("amount"));
					JSONObject currencyJsonObject = (JSONObject) approvedAmountObject.opt("type");
					String approvedAmountType = currencyJsonObject.optString("currency");

					JSONObject tourExpenseQuery = new JSONObject();
					tourExpenseQuery.put(Data.Query.RESOURCE, "hris_localconveyance");
					JSONArray tourExpenseColumnArray = new JSONArray();
					JSONObject expenseAmountColumnJson = new JSONObject();
					expenseAmountColumnJson.put("expression", "expenseamount");
					expenseAmountColumnJson.put("type", "currency");
					tourExpenseColumnArray.put(expenseAmountColumnJson);
					tourExpenseQuery.put(Data.Query.COLUMNS, tourExpenseColumnArray);
					tourExpenseQuery.put(Data.Query.FILTERS, "__key__ = " + localConveyanceId);
					JSONObject expenseObject;
					expenseObject = ResourceEngine.query(tourExpenseQuery);
					JSONArray tourExpenseArray = expenseObject.getJSONArray("hris_localconveyance");
					int tourExpenseArrayCount = tourExpenseArray == null ? 0 : tourExpenseArray.length();
					if (tourExpenseArrayCount > 0) {
						JSONObject expenseAmountObject = (JSONObject) tourExpenseArray.getJSONObject(0).opt("expenseamount");
						if (expenseAmountObject != null) {
							Number expenseAmount = 0.0;
							String expenseAmountType = null;
							expenseAmount = (Number) expenseAmountObject.opt("amount");
							JSONObject expenseAmountTypeObject = (JSONObject) expenseAmountObject.opt("type");
							expenseAmountType = (String) expenseAmountTypeObject.opt("currency");
							if (approvedAmount.doubleValue() > expenseAmount.doubleValue()) {
								throw new BusinessLogicException("Approved Amount must be equal to OR less than Expense Amount.");
							} else if (approvedAmount.doubleValue() <= expenseAmount.doubleValue() && (!approvedAmountType.equalsIgnoreCase(expenseAmountType))) {
								throw new BusinessLogicException("Approved Amount Type must be same as Expense Amount Type.");
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BusinessLogicException("" + e.getMessage());
				}
				record.addUpdate("approved", 1);
			}
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
	}

}
