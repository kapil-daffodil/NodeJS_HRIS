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
 * @category HRIS businesslogic Job define on hris_tourexpenses
 */

public class TourExpenseValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

		// Handle Approved Amount validation
		approvedAmontValidation(record);

		// HAndle duplicate entry of same particularid
		duplicateEntry(record);

		// Handle Paid AMount Validation
		paidAmountValidation(record);

	}

	private void paidAmountValidation(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			Object tourExpenseId = record.getValue("__key__");
			JSONObject paidAmountObject = (JSONObject) record.getValue("paidamount");
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
					JSONObject tourExpenseQuery = new JSONObject();
					tourExpenseQuery.put(Data.Query.RESOURCE, "hris_tourexpenses");
					JSONArray tourExpenseColumnArray = new JSONArray();
					JSONObject expenseAmountColumnJson = new JSONObject();
					expenseAmountColumnJson.put("expression", "approveamount");
					expenseAmountColumnJson.put("type", "currency");
					tourExpenseColumnArray.put(expenseAmountColumnJson);
					tourExpenseQuery.put(Data.Query.COLUMNS, tourExpenseColumnArray);
					tourExpenseQuery.put(Data.Query.FILTERS, "__key__ = " + tourExpenseId);
					JSONObject expenseObject;
					expenseObject = ResourceEngine.query(tourExpenseQuery);
					JSONArray tourExpenseArray = expenseObject.getJSONArray("hris_tourexpenses");
					int tourExpenseArrayCount = tourExpenseArray == null ? 0 : tourExpenseArray.length();
					if (tourExpenseArrayCount > 0) {
						JSONObject approveAmountObject = (JSONObject) tourExpenseArray.getJSONObject(0).opt("approveamount");
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
					throw new BusinessLogicException("" + e.getMessage());
				}
				record.addUpdate("paid", 1);
			}
		}
	}

	private void duplicateEntry(Record record) {
		String operation = record.getOperationType();
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

		if (operation.equalsIgnoreCase("insert")) {
			Object tourDetailId = record.getValue("tourdetailid");
			Object tourParticularId = record.getValue("tourparticularid");
			try {
				String particularName = getParticularName(tourParticularId);
				JSONObject tourExpenseQuery = new JSONObject();
				tourExpenseQuery.put(Data.Query.RESOURCE, "hris_tourexpenses");
				JSONArray tourExpenseColumnArray = new JSONArray();
				tourExpenseColumnArray.put("__key__");
				tourExpenseQuery.put(Data.Query.COLUMNS, tourExpenseColumnArray);
				tourExpenseQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and tourparticularid = " + tourParticularId);
				JSONObject object;
				object = ResourceEngine.query(tourExpenseQuery);
				JSONArray tourExpenseArray = object.getJSONArray("hris_tourexpenses");
				int tourExpenseArrayCount = tourExpenseArray == null ? 0 : tourExpenseArray.length();
				if (tourExpenseArrayCount > 0) {
					throw new BusinessLogicException("You have already saved record for " + particularName + ". Only one entry allowed for each particular.");
				}
			} catch (Exception e) {
				throw new BusinessLogicException("" + e.getMessage());
			}
		} else if (operation.equalsIgnoreCase("update")) {
			Object tourDetailId = record.getValue("tourdetailid");
			Object tourParticularId = record.getValue("tourparticularid");
			if (record.has("tourparticularid") && tourParticularId != null) {
				try {
					String particularName = getParticularName(tourParticularId);
					JSONObject tourExpenseQuery = new JSONObject();
					tourExpenseQuery.put(Data.Query.RESOURCE, "hris_tourexpenses");
					JSONArray tourExpenseColumnArray = new JSONArray();
					tourExpenseColumnArray.put("__key__");
					tourExpenseQuery.put(Data.Query.COLUMNS, tourExpenseColumnArray);
					tourExpenseQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and tourparticularid = " + tourParticularId);
					JSONObject object;
					object = ResourceEngine.query(tourExpenseQuery);
					JSONArray tourExpenseArray = object.getJSONArray("hris_tourexpenses");
					int tourExpenseArrayCount = tourExpenseArray == null ? 0 : tourExpenseArray.length();
					if (tourExpenseArrayCount > 0) {
						throw new BusinessLogicException("You have already saved record for " + particularName + ". Only one entry allowed for each particular.");
					}
				} catch (Exception e) {
					throw new BusinessLogicException("" + e.getMessage());
				}
			}
		}
	}

	private void approvedAmontValidation(Record record) {
		Object tourExpenseId = record.getValue("__key__");
		JSONObject approvedAmountObject = (JSONObject) record.getValue("approveamount");
		JSONObject oldApprovedAmountObject = (JSONObject) record.getOldValue("approveamount");
		if (record.has("approveamount")) {
			if (oldApprovedAmountObject != null) {
				throw new BusinessLogicException("You have already enter approved amount.");
			}
		}

		if (approvedAmountObject != null) {
			ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			try {
				Number approvedAmount = (Number) (approvedAmountObject.opt("amount") == null ? 0 : approvedAmountObject.opt("amount"));
				JSONObject currencyJsonObject = (JSONObject) approvedAmountObject.opt("type");
				String approvedAmountType = currencyJsonObject.optString("currency");

				JSONObject tourExpenseQuery = new JSONObject();
				tourExpenseQuery.put(Data.Query.RESOURCE, "hris_tourexpenses");
				JSONArray tourExpenseColumnArray = new JSONArray();
				JSONObject expenseAmountColumnJson = new JSONObject();
				expenseAmountColumnJson.put("expression", "expenseamount");
				expenseAmountColumnJson.put("type", "currency");
				tourExpenseColumnArray.put(expenseAmountColumnJson);
				tourExpenseQuery.put(Data.Query.COLUMNS, tourExpenseColumnArray);
				tourExpenseQuery.put(Data.Query.FILTERS, "__key__ = " + tourExpenseId);
				JSONObject expenseObject;
				expenseObject = ResourceEngine.query(tourExpenseQuery);
				JSONArray tourExpenseArray = expenseObject.getJSONArray("hris_tourexpenses");

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
				throw new BusinessLogicException("" + e.getMessage());
			}
			record.addUpdate("approved", 1);
		}
	}

	private String getParticularName(Object tourParticularId) {
		try {
			ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_tourparticulars");
			JSONArray array = new JSONArray();
			array.put("name");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + tourParticularId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray particularArray = object.getJSONArray("hris_tourparticulars");
			int particularArrayCount = particularArray == null ? 0 : particularArray.length();
			if (particularArrayCount > 0) {
				String particularName = particularArray.getJSONObject(0).optString("name");
				return particularName;
			} else {
				throw new BusinessLogicException("Particular name not found");
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error come while generate particular name" + e.getMessage());
		}
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
	}
}
