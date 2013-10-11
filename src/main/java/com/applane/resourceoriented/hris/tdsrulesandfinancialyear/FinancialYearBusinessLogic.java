package com.applane.resourceoriented.hris.tdsrulesandfinancialyear;

import org.json.JSONArray;
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
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.google.apphosting.api.DeadlineExceededException;

public class FinancialYearBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record arg0) throws DeadlineExceededException {

	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		String operationType = record.getOperationType();
		try {
			if (operationType.equalsIgnoreCase(Updates.Types.INSERT) || operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
				Object fromDateObject = record.getValue(HRISApplicationConstants.FinancialYear.FROM_DATE);
				Object toDateObject = record.getValue(HRISApplicationConstants.FinancialYear.TO_DATE);
				Object key = null;
				if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
					key = record.getKey();
				}
				if (fromDateObject != null && toDateObject != null) {
					// Date fromDate = Translator.dateValue(fromDateObject);
					// Date toDate = Translator.dateValue(toDateObject);
					String filter = HRISApplicationConstants.FinancialYear.FROM_DATE + " <= '" + fromDateObject + "' and " + HRISApplicationConstants.FinancialYear.TO_DATE + " >= '" + fromDateObject + "'";
					String optionFilter = HRISApplicationConstants.FinancialYear.FROM_DATE + " <= '" + toDateObject + "' and " + HRISApplicationConstants.FinancialYear.TO_DATE + " >= '" + toDateObject + "'";
					String extraFilter = "";
					if (key != null) {
						extraFilter = " and " + Updates.KEY + " != " + key;
					}
					JSONArray financialYearsArray = getFinancialYearsArray(key, filter, optionFilter, extraFilter);
					if (financialYearsArray != null && financialYearsArray.length() > 0) {
						Object fromDateTemp = financialYearsArray.getJSONObject(0).opt(HRISApplicationConstants.FinancialYear.FROM_DATE);
						Object toDateTemp = financialYearsArray.getJSONObject(0).opt(HRISApplicationConstants.FinancialYear.TO_DATE);
						throw new BusinessLogicException("Financial Year Already Defined for date range is: " + fromDateTemp + " to " + toDateTemp);
					}
				} else {
					throw new BusinessLogicException("From Date and To Date can not be null.");
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("FinancialYearBusinessLogic >> Exception >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured Please Contace to Admin.");
		}
	}

	private JSONArray getFinancialYearsArray(Object key, String filter, String optionFilter, String extraFilter) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.FinancialYear.RESOURCE);
		columnArray.put(HRISApplicationConstants.FinancialYear.FROM_DATE);
		columnArray.put(HRISApplicationConstants.FinancialYear.TO_DATE);
		columnArray.put(Updates.KEY);
		query.put(Data.Query.FILTERS, filter + extraFilter);
		query.put(Data.Query.OPTION_FILTERS, optionFilter + extraFilter);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.FinancialYear.RESOURCE);
	}
}
