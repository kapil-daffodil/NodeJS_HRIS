package com.applane.resourceoriented.hris;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;

public class CalculateBusinessFunctionAllEmployeesCTC {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void calculateCTCForBusinessFunctions() {
		try {
			JSONArray businessFunctionsArray = getBusinessFunctions();
			List<Integer> list = new ArrayList<Integer>();
			for (int counter = 0; counter < businessFunctionsArray.length(); counter++) {
				int businessFunctionId = Translator.integerValue(businessFunctionsArray.getJSONObject(counter).opt("__key__"));
				JSONArray employeeRecord = getEmployeeRecord(businessFunctionId);
				double totalCTC = 0.0;
				int numberOfEmployees = 0;
				for (int innerCounter = 0; innerCounter < employeeRecord.length(); innerCounter++) {
					int employeeId = Translator.integerValue(employeeRecord.getJSONObject(innerCounter).opt("hris_employees"));
					if (!list.contains(employeeId)) {
						numberOfEmployees++;
						list.add(employeeId);
						double ctc = Translator.doubleValue(employeeRecord.getJSONObject(innerCounter).opt("hris_employees.ctc"));
						totalCTC += ctc;
					}
				}
				updateBusinessFunctionCTC(businessFunctionId, numberOfEmployees, totalCTC);
			}
		} catch (Exception e) {
			LogUtility.writeLog("CalculateBusinessFunctionAllEmployeesCTC calculateCTCForBusinessFunctions Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private void updateBusinessFunctionCTC(int businessFunctionId, int numberOfEmployees, double totalCTC) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "businessfunctions");
		JSONObject row = new JSONObject();
		row.put("__key__", businessFunctionId);
		row.put("totalemployees", numberOfEmployees);
		row.put("ctc", totalCTC);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private JSONArray getEmployeeRecord(int businessFunctionId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees__businessfunctionid");
		JSONArray array = new JSONArray();
		array.put("hris_employees");
		array.put("hris_employees.ctc");
		array.put("businessfunctionid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "businessfunctionid = " + businessFunctionId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray businessFunctionArray = object.getJSONArray("hris_employees__businessfunctionid");
		return businessFunctionArray;
	}

	private JSONArray getBusinessFunctions() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "businessfunctions");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		JSONObject object = ResourceEngine.query(query);
		JSONArray businessFunctionArray = object.getJSONArray("businessfunctions");
		return businessFunctionArray;
	}
}
