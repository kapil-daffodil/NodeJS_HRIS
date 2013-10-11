package com.applane.resourceoriented.hris.tour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.hris.ApplicationsGenerateCode;
import com.google.apphosting.api.DeadlineExceededException;

public class TourCodeBusinessLogic implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object employeeId = record.getValue("employeeid");
		Object tourCode = record.getValue("tourcode");

		boolean isAutoEnquiryNo = checkEnableDisable();

		if (!isAutoEnquiryNo && (tourCode == null || ((String) tourCode).length() == 0)) {
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
					tourCode = ApplicationsGenerateCode.generateCode(departmentName, departmentAbbreviations, 3);
					record.addUpdate("tourcode", tourCode);
				}
			} catch (Exception e) {
				throw new BusinessLogicException("Error come while generate Tour Code.");
			}
		} else if (tourCode == null) {
			throw new BusinessLogicException("Tour Code can't be blank.");
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	public boolean checkEnableDisable() {
		try {
			JSONArray array = ApplicationsGenerateCode.getNumberGenerationSchemeDetail(3);
			if (array == null || array.length() == 0) {
				return true;
			}
			return false;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}
