package com.applane.hris;

import java.text.SimpleDateFormat;
import java.util.Date;

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

public class LodgingExpenseValidation implements OperationJob {

	// Job define on hris_tourlodgingexpense
	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		// HAndle all date validations
		lodgingExpenseDateValidation(record);

		// Handle Approved Amount validation
		approvedAmontValidation(record);

		// Handle Paid AMount validations
		paidAmountValidation(record);

	}

	private void paidAmountValidation(Record record) {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("update")) {
			Object tourLodgingExpenseId = record.getValue("__key__");
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

					JSONObject query = new JSONObject();
					query.put(Data.Query.RESOURCE, "hris_tourlodgingexpense");
					JSONArray columns = new JSONArray();
					JSONObject expenseAmountColumnJson = new JSONObject();
					expenseAmountColumnJson.put("expression", "approveamount");
					expenseAmountColumnJson.put("type", "currency");
					columns.put(expenseAmountColumnJson);
					query.put(Data.Query.COLUMNS, columns);
					query.put(Data.Query.FILTERS, "__key__ = " + tourLodgingExpenseId);
					JSONObject object;
					object = ResourceEngine.query(query);
					JSONArray tourLodgingExpenseArray = object.getJSONArray("hris_tourlodgingexpense");

					int tourLodgingExpenseArrayCount = tourLodgingExpenseArray == null ? 0 : tourLodgingExpenseArray.length();
					if (tourLodgingExpenseArrayCount > 0) {
						JSONObject approveAmountObject = (JSONObject) tourLodgingExpenseArray.getJSONObject(0).opt("approveamount");
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

	private void lodgingExpenseDateValidation(Record record) {
		ApplaneDatabaseEngine resourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

		String operation = record.getOperationType();
		Object checkInObject = record.getValue("checkin");
		Object checkOutObject = record.getValue("checkout");
		Object tourDetailId = record.getValue("tourdetailid");
		// SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		Date fromDate = null;
		Date toDate = null;
		Date checkInDate = null;
		Date checkOutDate = null;

		try {
			JSONObject tourQuery = new JSONObject();
			tourQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
			JSONArray tourColumnArray = new JSONArray();
			tourColumnArray.put("fromdate");
			tourColumnArray.put("todate");
			tourQuery.put(Data.Query.COLUMNS, tourColumnArray);
			tourQuery.put(Data.Query.FILTERS, "__key__= " + tourDetailId);
			JSONObject tourObject;
			tourObject = resourceEngine.query(tourQuery);
			JSONArray tourArray = tourObject.getJSONArray("hris_tourdetails");

			Object fromDateObject = tourArray.getJSONObject(0).opt("fromdate");
			Object toDateObject = tourArray.getJSONObject(0).opt("todate");
			if (fromDateObject != null && toDateObject != null) {
				fromDate = CommunicationUtility.checkDateFormat(fromDateObject);
				toDate = CommunicationUtility.checkDateFormat(toDateObject);
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
			String tourDetailFromDate = dateFormat.format(fromDate);
			String tourDetailToDate = dateFormat.format(toDate);

			if (operation.equalsIgnoreCase("insert")) {

				if (checkInObject != null && checkOutObject != null) {

					checkInDate = CommunicationUtility.checkDateFormat(checkInObject);
					checkOutDate = CommunicationUtility.checkDateFormat(checkOutObject);
					if (checkInDate.before(fromDate)) {
						throw new BusinessLogicException("CheckIn Date must be greater or equal to tour From Date(" + tourDetailFromDate + ").");
					}

					if (toDate.before(checkOutDate)) {
						throw new BusinessLogicException("CheckOut Date must be less or equal to tour To Date (" + tourDetailToDate + ").");
					}

					if (checkOutDate.before(checkInDate)) {
						throw new BusinessLogicException("Check Out Date must be greater or equal to Check In Date.");
					}
				}
			} else if (operation.equalsIgnoreCase("update")) {
				if (checkInObject != null && checkOutObject != null) {
					checkInDate = CommunicationUtility.checkDateFormat(checkInObject);
					checkOutDate = CommunicationUtility.checkDateFormat(checkOutObject);
					if (checkInDate.before(fromDate)) {
						throw new BusinessLogicException("CheckIn Date must be greater or equal to tour From Date(" + tourDetailFromDate + ").");
					}

					if (toDate.before(checkOutDate)) {
						throw new BusinessLogicException("CheckOut Date must be less or equal to tour To Date (" + tourDetailToDate + ").");
					}

					if (checkOutDate.before(checkInDate)) {
						throw new BusinessLogicException("Check Out Date must be greater or equal to Check In Date.");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("" + e.getMessage());
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

				JSONObject tourLodgingExpenseQuery = new JSONObject();
				tourLodgingExpenseQuery.put(Data.Query.RESOURCE, "hris_tourlodgingexpense");
				JSONArray tourExpenseColumnArray = new JSONArray();
				JSONObject expenseAmountColumnJson = new JSONObject();
				expenseAmountColumnJson.put("expression", "expenseamount");
				expenseAmountColumnJson.put("type", "currency");
				tourExpenseColumnArray.put(expenseAmountColumnJson);
				tourLodgingExpenseQuery.put(Data.Query.COLUMNS, tourExpenseColumnArray);
				tourLodgingExpenseQuery.put(Data.Query.FILTERS, "__key__ = " + tourExpenseId);
				JSONObject expenseObject;
				expenseObject = ResourceEngine.query(tourLodgingExpenseQuery);
				JSONArray tourLodgingExpenseArray = expenseObject.getJSONArray("hris_tourlodgingexpense");

				int tourLodgingExpenseArrayCount = tourLodgingExpenseArray == null ? 0 : tourLodgingExpenseArray.length();
				if (tourLodgingExpenseArrayCount > 0) {
					JSONObject expenseAmountObject = (JSONObject) tourLodgingExpenseArray.getJSONObject(0).opt("expenseamount");
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

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
