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
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.google.apphosting.api.DeadlineExceededException;

public class PopulateEmployeeKPIBusinessLogic implements OperationJob {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

		String operationType = record.getOperationType();
		if (operationType.equalsIgnoreCase("insert")) {
			try {
				Object monthId = record.getValue("monthid");
				Object yearId = record.getValue("yearid");
				if (monthId != null && yearId != null) {
					Object branchId = null;
					boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.VARIABLE);
					if (isFreezed) {
						throw new BusinessLogicException("Variable Freezed Please contact Admin Department.");
					}
					JSONObject taskQueueParameters = new JSONObject();
					taskQueueParameters.put("monthid", "" + monthId);
					taskQueueParameters.put("yearid", "" + yearId);
					ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeKPIServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
				}
			} catch (Exception e) {
				LogUtility.writeLog(e.getStackTrace());
			}
		}
	}

	@SuppressWarnings("unused")
	// used to re-populate KPI
	private void rePopulateEmployeeKPI(Object[] selectedkey) throws JSONException {
		JSONArray array = getPopulateKPIRecords(selectedkey[0]);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		Object monthid = null;
		Object yearid = null;
		if (arrayCount > 0) {
			monthid = array.getJSONObject(0).optString("monthid");
			yearid = array.getJSONObject(0).optString("yearid");
			Object branchId = null;
			boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthid, yearid, HRISApplicationConstants.Freez.VARIABLE);
			if (isFreezed) {
				throw new BusinessLogicException("Variable Freezed Please contact Admin Department.");
			}
			JSONObject taskQueueParameters = new JSONObject();
			taskQueueParameters.put("monthid", "" + monthid);
			taskQueueParameters.put("yearid", "" + yearid);
			taskQueueParameters.put("isUpdate", false);
			ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeKPIServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);

		} else {
			LogUtility.writeLog("Populate KPI record not found....");
		}
	}

	@SuppressWarnings("unused")
	// used to re-populate KPI
	private void rePopulateEmployeeKPIAndUpdateAll(Object[] selectedkey) throws JSONException {
		JSONArray array = getPopulateKPIRecords(selectedkey[0]);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		Object monthid = null;
		Object yearid = null;
		if (arrayCount > 0) {
			monthid = array.getJSONObject(0).optString("monthid");
			yearid = array.getJSONObject(0).optString("yearid");
			Object branchId = null;
			boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthid, yearid, HRISApplicationConstants.Freez.VARIABLE);
			if (isFreezed) {
				throw new BusinessLogicException("Variable Freezed Please contact Admin Department.");
			}
			JSONObject taskQueueParameters = new JSONObject();
			taskQueueParameters.put("monthid", "" + monthid);
			taskQueueParameters.put("yearid", "" + yearid);
			taskQueueParameters.put("isUpdate", true);
			ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeKPIServlet", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);

		} else {
			LogUtility.writeLog("Populate KPI record not found....");
		}
	}

	private JSONArray getPopulateKPIRecords(Object key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "populateemployeekpi");
		JSONArray array = new JSONArray();
		array.put("monthid");
		array.put("yearid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("populateemployeekpi");
		return rows;
	}

	public void onAfterUpdate(Record record) {

	}
}
