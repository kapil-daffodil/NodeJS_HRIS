package com.applane.resourceoriented.hris;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;

public class EmployeeMonthlySalaryBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) {

	}

	@Override
	public void onAfterUpdate(Record record) {
		System.err.println("EmployeeMonthlySalaryBusinessLogic --> onAfterUpdate()");

		String operationtype = record.getOperationType();
		System.err.println("operationtype is : >> " + operationtype);

		if (operationtype.equalsIgnoreCase("insert")) {
			System.err.println("In after insert.....");
			try {
				Object employeeId = record.getValue("employeeid");
				System.err.println("employeeId is : >> " + employeeId);

				Object salaryComponentId = record.getValue("salarycomponentid");
				System.err.println("salaryComponentId is : >> " + salaryComponentId);

				Object monthId = record.getValue("salarymonthid");
				System.err.println("monthid is : >> " + monthId);

				Object yearId = record.getValue("salaryyearid");
				System.err.println("yearId is : >> " + yearId);

				double amount = ((Number) record.getValue("amount")).doubleValue();
				System.err.println("amount is : >>  " + amount);

				updateAmountInSalarySheet(salaryComponentId, amount, employeeId, monthId, yearId);
			} catch (Exception e) {
				System.err.println("in catch block...");
				String message = ExceptionUtils.getExceptionTraceMessage("EmployeeMonthlySalaryBusinessLogic", e);
				System.err.println("message is : >> " + message);
				LogUtility.writeError(message);
				e.printStackTrace();
				throw new BusinessLogicException("Some unknown error occured while update criteria based amount in employee salary sheet.");
			}
		} else if (operationtype.equalsIgnoreCase("update")) {
			System.err.println("In after update.....");
			try {
				Object employeeId = record.getOldValue("employeeid");
				System.err.println("employeeId is : >> " + employeeId);

				Object salaryComponentId = record.getOldValue("salarycomponentid");
				System.err.println("salaryComponentId is : >> " + salaryComponentId);

				Object monthId = record.getOldValue("salarymonthid");
				System.err.println("monthid is : >> " + monthId);

				Object yearId = record.getOldValue("salaryyearid");
				System.err.println("yearId is : >> " + yearId);

				double oldAmount = ((Number) record.getOldValue("amount")).doubleValue();
				System.err.println("oldAmount is : >>  " + oldAmount);

				double newAmount = ((Number) record.getValue("amount")).doubleValue();
				System.err.println("newAmount is : >>  " + newAmount);

				if (record.has("amount")) {
					System.err.println("amount column come for update....");
					updateAmountInSalarySheet(salaryComponentId, newAmount, employeeId, monthId, yearId);
				} else {
					System.err.println("Amount column not come for update....");
				}
			} catch (Exception e) {
				System.err.println("in catch block...");
				String message = ExceptionUtils.getExceptionTraceMessage("EmployeeMonthlySalaryBusinessLogic", e);
				System.err.println("message is : >> " + message);
				LogUtility.writeError(message);
				e.printStackTrace();
				throw new BusinessLogicException("Some unknown error occured while update amount in salary sheet");
			}
		}
	}

	private void updateAmountInSalarySheet(Object salaryComponentId, double amount, Object employeeId, Object monthId, Object yearId) throws JSONException {
		System.err.println("updateAmountInSalarySheet Method call.....");
		if (salaryComponentId != null) {
			System.err.println("salaryComponentId not equal to null....");
			int componentcriteriaId = getComponentCriteria(salaryComponentId);
			System.err.println("componentcriteriaId is : >> " + componentcriteriaId);

			int paymentTypeId = getPaymentTypeId(salaryComponentId);
			System.err.println("paymentTypeId is : >> " + paymentTypeId);

			JSONArray salarySheetArray = getSalarySheetRecords(employeeId, monthId, yearId);
			System.err.println("salarySheetArray is : >> " + salarySheetArray);

			int salarySheetArrayCount = (salarySheetArray == null || salarySheetArray.length() == 0) ? 0 : salarySheetArray.length();
			System.err.println("salarySheetArrayCount is : > " + salarySheetArrayCount);

			if (salarySheetArrayCount > 0) {
				System.err.println("salarySheetArrayCount is greater tahn zero....");

				Object salarySheetId = salarySheetArray.getJSONObject(0).opt("__key__");
				System.err.println("salarySheetId is : >> " + salarySheetId);

				double attendanceBasedAmount = ((Number) (salarySheetArray.getJSONObject(0).opt("attendancebasedamount") == null ? 0.0 : salarySheetArray.getJSONObject(0).optDouble("attendancebasedamount"))).doubleValue();
				System.err.println("attendanceBasedAmount is : >> " + attendanceBasedAmount);

				double performanceBasedAmount = ((Number) (salarySheetArray.getJSONObject(0).opt("performancebasedamount") == null ? 0.0 : salarySheetArray.getJSONObject(0).optDouble("performancebasedamount"))).doubleValue();
				System.err.println("performanceBasedAmount is : >> " + performanceBasedAmount);

				double fixedBasedAmount = ((Number) (salarySheetArray.getJSONObject(0).opt("fixedamount") == null ? 0.0 : salarySheetArray.getJSONObject(0).opt("fixedamount"))).doubleValue();
				System.err.println("fixedBasedAmount is : >> " + fixedBasedAmount);

				double deductionAmount = ((Number) (salarySheetArray.getJSONObject(0).opt("deductionamount") == null ? 0.0 : salarySheetArray.getJSONObject(0).opt("deductionamount"))).doubleValue();
				System.err.println("deductionAmount is : >> " + deductionAmount);

				double payableAmount = ((Number) (salarySheetArray.getJSONObject(0).opt("payableamount") == null ? 0.0 : salarySheetArray.getJSONObject(0).opt("payableamount"))).doubleValue();

				System.err.println("payableAmount is : >> " + payableAmount);

				if (componentcriteriaId == HRISApplicationConstants.FIXED_BASED) {

					if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
						payableAmount = payableAmount + amount;
						fixedBasedAmount = fixedBasedAmount + amount;
						System.err.println("fixedBasedAmount is : >> " + fixedBasedAmount);
						// update payableamount and fixed based amount in employee salary sheet
						updatePayableAndFixedAmount(salarySheetId, payableAmount, fixedBasedAmount);
					} else {
						deductionAmount = deductionAmount + amount;
						// fixedBasedAmount = fixedBasedAmount + amount;
						payableAmount = payableAmount - amount;
						System.err.println("fixedBasedAmount is : >> " + fixedBasedAmount);
						// update deduction amount and fixed amount in employee salary sheet
						updateDeductionAndFixedAmount(salarySheetId, deductionAmount, fixedBasedAmount, payableAmount);
					}
				} else if (componentcriteriaId == HRISApplicationConstants.ATTENDANCE_BASED) {

					if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
						payableAmount = payableAmount + amount;
						attendanceBasedAmount = attendanceBasedAmount + amount;
						// update payable amount and attendance based amount
						updatePayableAndAttendanceAmount(salarySheetId, payableAmount, attendanceBasedAmount);
					} else {
						deductionAmount = deductionAmount + amount;
						// attendanceBasedAmount = attendanceBasedAmount + amount;
						payableAmount = payableAmount - amount;
						// update deduction amount and attendance based amount
						updateDeductionAndAttendanceAmount(salarySheetId, deductionAmount, attendanceBasedAmount, payableAmount);
					}
					System.err.println("attendanceBasedAmount is : >> " + attendanceBasedAmount);
				} else if (componentcriteriaId == HRISApplicationConstants.PERFORMANCE_BASED) {
					if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
						payableAmount = payableAmount + amount;
						performanceBasedAmount = performanceBasedAmount + amount;
						// update payable amount and performance based amount
						updatePayableAndPerformanceAmount(salarySheetId, payableAmount, performanceBasedAmount);
					} else {
						deductionAmount = deductionAmount + amount;
						// performanceBasedAmount = performanceBasedAmount + amount;
						payableAmount = payableAmount - amount;
						// update deductionAmount and performanceBasedAmount in employee salary sheet
						updateDeductionAndPerformanceAmount(salarySheetId, deductionAmount, performanceBasedAmount, payableAmount);
					}
				}
			}
		}
	}

	private void updatePayableAndFixedAmount(Object salarySheetId, double payableAmount, double fixedAmount) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		row.put("__key__", salarySheetId);
		row.put("payableamount", payableAmount);
		row.put("fixedamount", fixedAmount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateDeductionAndFixedAmount(Object salarySheetId, double deductionAmount, double fixedAmount, double payableAmount) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		row.put("__key__", salarySheetId);
		row.put("deductionamount", deductionAmount);
		// row.put("fixedamount", fixedAmount);
		row.put("payableamount", payableAmount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updatePayableAndAttendanceAmount(Object salarySheetId, double payableAmount, double attendanceAmount) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		row.put("__key__", salarySheetId);
		row.put("payableamount", payableAmount);
		row.put("attendancebasedamount", attendanceAmount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateDeductionAndAttendanceAmount(Object salarySheetId, double deductionAmount, double attendanceAmount, double payableAmount) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		row.put("__key__", salarySheetId);
		row.put("payableamount", payableAmount);
		row.put("deductionamount", deductionAmount);
		// row.put("attendancebasedamount", attendanceAmount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updateDeductionAndPerformanceAmount(Object salarySheetId, double deductionAmount, double performanceAmount, double payableAmount) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		row.put("__key__", salarySheetId);
		row.put("deductionamount", deductionAmount);
		// row.put("performancebasedamount", performanceAmount);
		row.put("payableamount", payableAmount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private void updatePayableAndPerformanceAmount(Object salarySheetId, double payableAmount, double performanceAmount) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "salarysheet");
		JSONObject row = new JSONObject();
		row.put("__key__", salarySheetId);
		row.put("payableamount", payableAmount);
		row.put("performancebasedamount", performanceAmount);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private JSONArray getSalarySheetRecords(Object employeeId, Object monthId, Object yearId) throws JSONException {
		System.err.println("getSalarySheetRecords Method call.....");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("attendancebasedamount");
		columnArray.put("performancebasedamount");
		columnArray.put("fixedamount");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarysheet");
		return array;
	}

	private int getComponentCriteria(Object salaryComponentId) throws JSONException {
		System.err.println("getComponentCriteria Method call...");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("componentcriteriaid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + salaryComponentId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarycomponents");
		System.err.println("array is : >> " + array);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			System.err.println("arrayCount is greater than zeroo....");
			Integer componentCriteriaId = array.getJSONObject(0).optInt("componentcriteriaid");
			System.err.println("componentCriteriaId is : >> " + componentCriteriaId);
			return componentCriteriaId;
		} else {
			throw new BusinessLogicException("No componentcriteriaid found for salary component[" + salaryComponentId + "]");
		}
	}

	private int getPaymentTypeId(Object salaryComponentId) throws JSONException {
		System.err.println("getComponentCriteria Method call...");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("paymenttypeid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + salaryComponentId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarycomponents");
		System.err.println("array is : >> " + array);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			System.err.println("arrayCount is greater than zeroo....");
			Integer paymentTypeId = array.getJSONObject(0).optInt("paymenttypeid");
			System.err.println("paymentTypeId is : >> " + paymentTypeId);
			return paymentTypeId;
		} else {
			throw new BusinessLogicException("No Payment Type found for salary component [" + salaryComponentId + "]");
		}
	}
}
