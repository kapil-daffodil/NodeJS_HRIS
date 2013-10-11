package com.applane.resourceoriented.hris;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;

public class LeaveCodeBusinessLogic implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) {
		if (record.has("temp")) {
			return;
		}
		Object employeeId = record.getValue("employeeid");
		Object leaveCode = record.getValue("leavecode");
		if (employeeId instanceof JSONObject) {
			employeeId = ((JSONObject) employeeId).opt(Updates.KEY);
		}
		if (employeeId instanceof JSONArray) {
			try {
				employeeId = ((JSONArray) employeeId).length() == 0 ? 0 : ((JSONArray) employeeId).getJSONObject(0).opt(Updates.KEY);
			} catch (JSONException e) {
				throw new BusinessLogicException("Error come while getting Employee ID.");
			}
		}
		boolean isAutoEnquiryNo = checkEnableDisable();
		if (!isAutoEnquiryNo && (leaveCode == null || ((String) leaveCode).length() == 0)) {
			try {
				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_employees");
				JSONArray array = new JSONArray();
				array.put("departmentid.name");
				array.put("departmentid.abbreviations");
				query.put(Data.Query.COLUMNS, array);
				query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
				ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
				JSONObject object = ResourceEngine.query(query);
				JSONArray rows = object.getJSONArray("hris_employees");
				int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
				if (rowCount > 0) {
					String departmentName = rows.getJSONObject(0).optString("departmentid.name") == null ? "" : rows.getJSONObject(0).optString("departmentid.name");
					String departmentAbbreviations = rows.getJSONObject(0).optString("departmentid.abbreviations") == null ? "" : rows.getJSONObject(0).optString("departmentid.abbreviations");
					leaveCode = ApplicationsGenerateCode.generateCode(departmentName, departmentAbbreviations, 1);
					record.addUpdate("leavecode", leaveCode);
				}
			} catch (Exception e) {
				throw new BusinessLogicException("Error come while generate Leave Code.");
			}
		} else if (leaveCode == null) {
			throw new BusinessLogicException("Leave Code can't be blank.");
		}
	}

	@Override
	public void onAfterUpdate(Record record) {
		// TODO Auto-generated method stub

	}

	public boolean checkEnableDisable() {
		try {
			JSONArray array = ApplicationsGenerateCode.getNumberGenerationSchemeDetail(1);
			if (array == null || array.length() == 0) {
				return true;
			}
			return false;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}
