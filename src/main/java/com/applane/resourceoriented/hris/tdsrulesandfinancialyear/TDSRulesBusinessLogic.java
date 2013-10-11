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

public class TDSRulesBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record arg0) throws DeadlineExceededException {

	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		String operationType = record.getOperationType();
		try {
			if (operationType.equalsIgnoreCase(Updates.Types.INSERT) || operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
				Object financialYearId = record.getValue(HRISApplicationConstants.TDSRules.FINANCIAL_YEAR_ID);
				if (financialYearId != null) {
					Object key = null;
					String extraFilter = "";
					if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
						key = record.getKey();
						extraFilter = " and " + Updates.KEY + " != " + key;
					}
					String filter = HRISApplicationConstants.TDSRules.FINANCIAL_YEAR_ID + "=" + financialYearId;
					JSONArray tDSRuleArray = getTDSRuleArray(key, filter, extraFilter);

					if (tDSRuleArray != null && tDSRuleArray.length() > 0) {
						throw new BusinessLogicException("Rule Already Defined For This Financial Year.");
					}
				}
			} else {
				throw new BusinessLogicException("Financial Year Can Not Be Null.");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("TDSRulesBusinessLogic >> Exception >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured Please Contace to Admin.");
		}
	}

	private JSONArray getTDSRuleArray(Object key, String filter, String extraFilter) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.TDSRules.RESOURCE);
		columnArray.put(Updates.KEY);
		query.put(Data.Query.FILTERS, filter + extraFilter);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.TDSRules.RESOURCE);
	}
}
