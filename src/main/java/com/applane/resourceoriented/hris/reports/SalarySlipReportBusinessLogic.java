package com.applane.resourceoriented.hris.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeMonthlyAttendance;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.ConvertNumberToWordsBusinessLogic;
import com.google.apphosting.api.DeadlineExceededException;

public class SalarySlipReportBusinessLogic implements QueryJob {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onQuery(Query query) throws DeadlineExceededException {
	}

	public void onResult(Result result) throws DeadlineExceededException {
		JSONArray array = result.getRecords();
		if (array != null && array.length() > 0) {
			try {
				Object employeeId = array.getJSONObject(0).opt("employeeid");
				Object monthId = array.getJSONObject(0).opt("monthid");
				Object yearId = array.getJSONObject(0).opt("yearid");
				JSONObject employeePersonalDetail = getEmployeePersonalDetail(array.getJSONObject((array.length() - 1)));
				JSONArray allComponentArray = getAllCompontnts();
				JSONArray configurationSetup = getConfigurationSetup();
				String organizationName = "";
				String organizationAddress = "";
				String organizationLogo = "";
				if (configurationSetup != null && configurationSetup.length() > 0) {
					organizationName = configurationSetup.getJSONObject(0).optString("organizationname");
					organizationAddress = configurationSetup.getJSONObject(0).optString("address");
					if (configurationSetup.getJSONObject(0).optString("organizationlogo") != null) {
						organizationLogo = new EmployeeSalarySheetReport().getLogo(configurationSetup.getJSONObject(0).optString("organizationlogo"));
					}
				}
				JSONArray receivedActualSalaryComponentArray = getEmployeeMonthlySalaryRecords(employeeId, monthId, yearId, allComponentArray);
				JSONObject salarySlip = new JSONObject();
				salarySlip.put("employeePersonalDetail", employeePersonalDetail);
				salarySlip.put("receivedActualSalaryComponentArray", receivedActualSalaryComponentArray.getJSONObject(0).opt("payableComponentDetail"));
				salarySlip.put("deductionSalaryComponentArray", receivedActualSalaryComponentArray.getJSONObject(0).opt("deductAbleComponentDetail"));
				salarySlip.put("totalDeductionAmount", receivedActualSalaryComponentArray.getJSONObject(0).opt("totalDeductionAmount"));
				salarySlip.put("totalActualAmount", receivedActualSalaryComponentArray.getJSONObject(0).opt("totalActualAmount"));
				salarySlip.put("netPayableAmount", receivedActualSalaryComponentArray.getJSONObject(0).opt("netPayableAmount"));
				salarySlip.put("totalPayableAmount", receivedActualSalaryComponentArray.getJSONObject(0).opt("totalPayableAmount"));
				salarySlip.put("netAmountInWords", receivedActualSalaryComponentArray.getJSONObject(0).opt("netAmountInWords"));

				salarySlip.put("organizationName", organizationName);
				salarySlip.put("organizationAddress", organizationAddress);
				salarySlip.put("organizationLogo", organizationLogo);

				array.put(new JSONArray().put(new JSONObject().put("salarySlip", salarySlip)));
			} catch (Exception e) {
				LogUtility.writeLog("SalarySlipReportBusinessLogic  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
				throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			}
		}
	}

	private JSONArray getConfigurationSetup() throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		columnArray.put("organizationname");
		columnArray.put("organizationlogo");
		columnArray.put("address");
		query.put(Data.Query.COLUMNS, columnArray);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_mailconfigurations");
	}

	private JSONArray getEmployeeMonthlySalaryRecords(Object employeeId, Object monthId, Object yearId, JSONArray allComponentArray) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.paymenttypeid");
		columnArray.put("salarycomponentid.amountpaytoid");
		columnArray.put("amount");
		columnArray.put("actualamount");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + monthId + " and salaryyearid = " + yearId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("employeemonthlysalarycomponents");
		HashMap<Integer, Object[]> employPayableComponentDetail = new HashMap<Integer, Object[]>();
		HashMap<Integer, Object[]> employDeductableComponentDetail = new HashMap<Integer, Object[]>();
		LinkedHashMap<Integer, String> payableComponentsMap = new LinkedHashMap<Integer, String>();
		LinkedHashMap<Integer, String> deducatableComponentsMap = new LinkedHashMap<Integer, String>();
		for (int counter = 0; counter < allComponentArray.length(); counter++) {
			// salary components like HRA, basic
			String salaryComponent = allComponentArray.getJSONObject(counter).optString("name");
			int salaryComponentId = allComponentArray.getJSONObject(counter).optInt("__key__");
			int paymentTypeId = allComponentArray.getJSONObject(counter).optInt("paymenttypeid");
			double amountPaytoId = allComponentArray.getJSONObject(counter).optInt("amountpaytoid");
			if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
				payableComponentsMap.put(salaryComponentId, salaryComponent);
			}
			if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE || amountPaytoId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
				deducatableComponentsMap.put(salaryComponentId, salaryComponent);
			}
		}
		double totalDeductionAmount = 0;
		double totalActualAmount = 0;
		double totalPayableAmount = 0;
		if (array != null && array.length() > 0) {
			for (int counter = 0; counter < array.length(); counter++) {
				Object[] employComponentArray = new Object[3];
				int salaryComponentId = array.getJSONObject(counter).optInt("salarycomponentid");
				double paymentTypeId = array.getJSONObject(counter).optInt("salarycomponentid.paymenttypeid");
				double amountPaytoId = array.getJSONObject(counter).optInt("salarycomponentid.amountpaytoid");
				String monthlySalaryComponent = array.getJSONObject(counter).optString("salarycomponentid.name");
				double receivedAmount = Translator.doubleValue(array.getJSONObject(counter).opt("amount"));
				double actualAmount = Translator.doubleValue(array.getJSONObject(counter).opt("actualamount"));
				if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
					totalActualAmount += actualAmount;
					totalPayableAmount += receivedAmount;
					if (employPayableComponentDetail.containsKey(salaryComponentId)) {
						employComponentArray = employPayableComponentDetail.get(salaryComponentId); // if component already contain
						double receivedAmountInner = ((Number) employComponentArray[1]).doubleValue();
						double actualAmountInner = ((Number) employComponentArray[2]).doubleValue();
						employComponentArray[1] = receivedAmountInner + (int) (receivedAmount + 0.5);
						employComponentArray[2] = actualAmountInner + (int) (actualAmount + 0.5);
					} else {
						employComponentArray[0] = monthlySalaryComponent;
						employComponentArray[1] = (int) (receivedAmount + 0.5);
						employComponentArray[2] = (int) (actualAmount + 0.5);
					}
					employPayableComponentDetail.put(salaryComponentId, employComponentArray);
				}
				if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE || amountPaytoId == HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
					totalDeductionAmount += receivedAmount;
					if (employDeductableComponentDetail.containsKey(salaryComponentId)) {
						employComponentArray = employDeductableComponentDetail.get(salaryComponentId); // if component already contain
						double receivedAmountInner = ((Number) employComponentArray[1]).doubleValue();
						double actualAmountInner = ((Number) employComponentArray[2]).doubleValue();
						employComponentArray[1] = receivedAmountInner + (int) (receivedAmount + 0.5);
						employComponentArray[2] = actualAmountInner + (int) (actualAmount + 0.5);
					} else {
						employComponentArray[0] = monthlySalaryComponent;
						employComponentArray[1] = (int) (receivedAmount + 0.5);
						employComponentArray[2] = (int) (actualAmount + 0.5);
					}
					employDeductableComponentDetail.put(salaryComponentId, employComponentArray);
				}
			}
		}
		JSONArray payableComponentDetail = new JSONArray();
		for (Integer componentId : payableComponentsMap.keySet()) {
			String componentName = payableComponentsMap.get(componentId);
			Object[] componentArray = employPayableComponentDetail.get(componentId);
			JSONObject detail = new JSONObject();
			if (componentArray != null) {
				detail.put("name", componentName);
				// totalPayableAmount += Double.parseDouble("" + componentArray[1]);
				detail.put("actualAmount", (int) (((Number) componentArray[2]).doubleValue() + 0.5)); // write actual amount
				detail.put("payableAmount", (int) (((Number) componentArray[1]).doubleValue() + 0.5)); // write actual amount
				payableComponentDetail.put(detail);
			}
		}

		JSONArray deductAbleComponentDetail = new JSONArray();
		for (Integer componentId : deducatableComponentsMap.keySet()) {
			String componentName = deducatableComponentsMap.get(componentId);
			Object[] componentArray = employDeductableComponentDetail.get(componentId);
			JSONObject detail = new JSONObject();

			if (componentArray != null) {
				detail.put("name", componentName);
				detail.put("payableAmount", (int) (((Number) componentArray[1]).doubleValue() + 0.5)); // write actual amount
				deductAbleComponentDetail.put(detail);
			}

		}
		int netPayableAmount = (int) ((((Number) (totalPayableAmount - totalDeductionAmount))).doubleValue() + 0.5);
		String netAmountInWords = new ConvertNumberToWordsBusinessLogic().convertNumberToWords(netPayableAmount);
		JSONObject paymentDetail = new JSONObject();
		paymentDetail.put("payableComponentDetail", payableComponentDetail); // array
		paymentDetail.put("totalDeductionAmount", (int) (totalDeductionAmount + 0.5)); // double
		paymentDetail.put("totalActualAmount", (int) (totalActualAmount + 0.5));
		paymentDetail.put("totalPayableAmount", (int) (totalPayableAmount + 0.5));
		paymentDetail.put("netPayableAmount", netPayableAmount);
		paymentDetail.put("netAmountInWords", netAmountInWords);
		paymentDetail.put("deductAbleComponentDetail", deductAbleComponentDetail);
		return new JSONArray().put(paymentDetail);
	}

	private JSONArray getAllCompontnts() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("name");
		columnArray.put("__key__");
		columnArray.put("amountpaytoid");
		columnArray.put("paymenttypeid");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarycomponents");
		return array;
	}

	private JSONObject getEmployeePersonalDetail(JSONObject array) {
		try {
			JSONObject personalDetail = new JSONObject();
			Object employeeId = array.opt("employeeid");
			Object employeeName = array.opt("employeeid.name");
			Object employeeCode = array.opt("employeeid.employeecode");
			Object employeeBranch = array.opt("branchid.name");
			Object employeeDepartment = array.opt("employeeid.departmentid.name");
			Object payMonthId = array.opt("monthid");
			Object payYearId = array.opt("yearid");
			Object payMonth = array.opt("monthid.name");
			Object payYear = array.opt("yearid.name");
			Object employeeDesignation = array.opt("employeeid.designationid.name");
			Object monthlyclosingbalance = array.opt("monthlyclosingbalance");
			// Object balanceleaves = array.opt("employeeid.balanceleaves");

			double dayWorked = Translator.doubleValue((array.opt("present")));
			double nonPayableLeave = Translator.doubleValue(array.opt("nonpayableleaves"));
			double leaves = Translator.doubleValue(array.opt("leaves"));
			double paidAbsents = Translator.doubleValue(array.opt("paidabsents"));
			double nonWorkingDays = Translator.doubleValue(array.opt("nonworkingdays"));
			Object absent = array.opt("absent");
			Object ewd = array.opt("extraworkingday");
			Date monthFirstDate = null;
			if (payMonth != null && ("" + payMonth).length() > 0 && ("" + payYear) != null && ("" + payYear).length() > 0) {
				monthFirstDate = DataTypeUtilities.getMonthFirstDate("" + payYear, "" + payMonth);
			}

			long maxDayInMonth = 0;
			if (monthFirstDate != null) {
				maxDayInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
			}
			JSONArray leaverRegister = new EmployeeMonthlyAttendance().getLeaveRegisterArray(employeeId, payMonthId, payYearId, null);
			double balanceLeaves = 0.0;
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int counter = 0; counter < leaverRegister.length(); counter++) {
				int leaveType = Translator.integerValue(leaverRegister.getJSONObject(counter).opt("leavetypeid"));
				if (!list.contains(leaveType)) {
					list.add(leaveType);
					balanceLeaves += Translator.doubleValue(leaverRegister.getJSONObject(counter).opt("leavebalance"));
				}
			}

			personalDetail.put("employeeName", employeeName);
			personalDetail.put("employeeCode", employeeCode);
			personalDetail.put("employeeBranch", employeeBranch);
			personalDetail.put("employeeDepartment", employeeDepartment);
			personalDetail.put("payMonth", payMonth);
			personalDetail.put("payYear", payYear);
			personalDetail.put("employeeDesignation", employeeDesignation);
			personalDetail.put("maxDayInMonth", maxDayInMonth);
			personalDetail.put("balanceleaves", balanceLeaves);
			personalDetail.put("monthlyclosingbalance", monthlyclosingbalance);
			personalDetail.put("dayWorked", dayWorked);
			personalDetail.put("leaveWithPay", leaves - nonPayableLeave);
			personalDetail.put("nonPayableLeave", nonPayableLeave);
			personalDetail.put("absent", absent);
			personalDetail.put("paidAbsents", paidAbsents);
			personalDetail.put("ewd", ewd);
			if (dayWorked > 0.0) {
				personalDetail.put("totalSalaryDay", dayWorked + (leaves - nonPayableLeave) + nonWorkingDays + DataTypeUtilities.doubleValue(ewd) + paidAbsents);
			} else {
				personalDetail.put("totalSalaryDay", 0.0);
			}

			return personalDetail;
		} catch (Exception e) {
			LogUtility.writeLog("SalarySlipReportBusinessLogic getEmployeePersonalDetail  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

	}

}
