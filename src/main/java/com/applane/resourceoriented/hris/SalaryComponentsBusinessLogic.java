package com.applane.resourceoriented.hris;

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
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.google.apphosting.api.DeadlineExceededException;

public class SalaryComponentsBusinessLogic implements OperationJob {

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		String operationType = record.getOperationType();
		try {
			if (operationType.equals(Updates.Types.INSERT) || operationType.equals(Updates.Types.UPDATE)) {
				Object key = null;
				if (operationType.equals(Updates.Types.UPDATE)) {
					key = record.getKey();
				}
				if (record.has(SalaryComponentKinds.KPI_ID)) {
					Object kpiId = record.getValue(SalaryComponentKinds.KPI_ID);
					if (kpiId != null) {
						JSONArray kpiDuplicate = getSalaryComponentsKPIRecords(key, kpiId);
						if (kpiDuplicate != null && kpiDuplicate.length() > 0) {
							throw new BusinessLogicException("This Kpi Already Assigned to " + kpiDuplicate.getJSONObject(0).opt(SalaryComponentKinds.NAME));
						}
					}
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(SalaryComponentsBusinessLogic.class.getName(), e);
			LogUtility.writeLog("SalaryComponentsBusinessLogic >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown ErrorOccured Please Contace Admin.");
		}
	}

	private JSONArray getSalaryComponentsKPIRecords(Object key, Object kpiId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, SalaryComponentKinds.RESOURCE);
		if (key != null) {
			query.put(Data.Query.FILTERS, Updates.KEY + " != " + key + " and " + SalaryComponentKinds.KPI_ID + " = " + kpiId);
		} else {
			query.put(Data.Query.FILTERS, SalaryComponentKinds.KPI_ID + " = " + kpiId);
		}
		columnArray.put(Updates.KEY);
		columnArray.put(SalaryComponentKinds.NAME);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(SalaryComponentKinds.RESOURCE);
		return rows;
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
