package com.applane.resourceoriented.hris;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class EsiCycleMasterBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		Object key = null;
		if (record.getOperationType().equals(Updates.Types.UPDATE)) {
			key = record.getKey();
		}
		try {

			if (record.has("from_date")) {
				Object fromDate = record.getValue("from_date");
				JSONArray esiArray = getEsiArray(fromDate, key);
				if (esiArray != null && esiArray.length() > 0) {
					throw new BusinessLogicException("This Range Already Exist.");
				}
			}
			if (record.has("to_date")) {
				Object toDate = record.getValue("to_date");
				JSONArray esiArray = getEsiArray(toDate, key);
				if (esiArray != null && esiArray.length() > 0) {
					throw new BusinessLogicException("This Range Already Exist.");
				}
			}
			if (record.has("from_date") && record.has("to_date")) {
				Object fromDate = record.getValue("from_date");
				Object toDate = record.getValue("to_date");
				if (Translator.dateValue(fromDate).equals(Translator.dateValue(toDate)) || Translator.dateValue(fromDate).after(Translator.dateValue(toDate))) {
					throw new BusinessLogicException("To Date must be greater than From Date.");
				}
				JSONArray esiArray = getEsiArray(fromDate, toDate, key);
				if (esiArray != null && esiArray.length() > 0) {
					throw new BusinessLogicException("This Range Already Exist.");
				}
			}
			if (record.getOperationType().equals(Updates.Types.UPDATE)) {
				Object fromDate = record.getValue("from_date");
				Object toDate = record.getValue("to_date");
				if (Translator.dateValue(fromDate).equals(Translator.dateValue(toDate)) || Translator.dateValue(fromDate).after(Translator.dateValue(toDate))) {
					throw new BusinessLogicException("To Date must be greater than From Date.");
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("ESI Cycle Exception Trace >> " + trace);
			throw new RuntimeException(trace);
		}
	}

	private JSONArray getEsiArray(Object fromDate, Object toDate, Object key) throws Exception {
		String filter = "";
		if (key != null) {
			filter = " AND __key__ != " + key;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_esi_cycle_master");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "from_date <= '" + fromDate + "' and  to_date >= '" + toDate + "'" + filter);
		query.put(Data.Query.OPTION_FILTERS, "from_date >= '" + fromDate + "' and  to_date <= '" + toDate + "'" + filter);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_esi_cycle_master");
		return rows;
	}

	private JSONArray getEsiArray(Object date, Object key) throws Exception {
		String filter = "";
		if (key != null) {
			filter = " AND __key__ != " + key;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_esi_cycle_master");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "from_date <= '" + date + "' and  to_date >= '" + date + "'" + filter);
		query.put(Data.Query.OPTION_FILTERS, "from_date >= '" + date + "' and  to_date <= '" + date + "'" + filter);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_esi_cycle_master");
		return rows;
	}
}
