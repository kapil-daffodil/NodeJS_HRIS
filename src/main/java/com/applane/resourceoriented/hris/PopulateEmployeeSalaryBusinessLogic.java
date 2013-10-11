package com.applane.resourceoriented.hris;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.google.apphosting.api.DeadlineExceededException;

public class PopulateEmployeeSalaryBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws VetoException {
		try {
			Object branchId = record.getValue("branchid");
			Object monthId = record.getValue("monthid");
			Object yearId = record.getValue("yearid");

			if (!isExists(yearId, monthId, branchId)) {
				// JSONObject taskQueueParameters = new JSONObject();
				// taskQueueParameters.put("monthid", monthId);
				// taskQueueParameters.put("yearid", yearId);
				// taskQueueParameters.put("branchid", branchId);
				// ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet", QueueConstantsHRIS.SALARY_PROCESS, taskQueueParameters);
			} else {
				throw new BusinessLogicException("Employees salary for selected branch is already populated.");
			}
		} catch (DeadlineExceededException e) {
			throw e;
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String message = ExceptionUtils.getExceptionTraceMessage("PopulateEmployeeSalaryBusinessLogic", e);
			LogUtility.writeError(message);
			throw new BusinessLogicException("Some unknown error ocuured while taking salary sheet.");
		}
	}

	public void releaseEmployeeSalary(Object[] selectedkey) throws Exception {
		JSONArray array = getTakeSalarySheetRecords(selectedkey[0]);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		Object branchid = null;
		Object monthid = null;
		Object yearid = null;
		Object generateInvoice = false;
		if (arrayCount > 0) {
			branchid = array.getJSONObject(0).opt("branchid");
			monthid = array.getJSONObject(0).opt("monthid");
			yearid = array.getJSONObject(0).opt("yearid");
			generateInvoice = array.getJSONObject(0).opt("make_invoice");
			Object key = array.getJSONObject(0).opt("__key__");

			boolean isGenerating = Translator.booleanValue(array.getJSONObject(0).opt("is_generating"));
			boolean isReleasing = Translator.booleanValue(array.getJSONObject(0).opt("is_releasing"));
			if (isGenerating) {
				throw new BusinessLogicException("Salary Generation is already in progress. please wait...");
			}
			if (isReleasing) {
				throw new BusinessLogicException("Salary Release is already in progress. please wait...");
			}

			boolean isFreezed = getIsFreezedSalary(branchid, monthid, yearid, HRISApplicationConstants.Freez.SALARY);
			if (isFreezed) {
				throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
			}
			boolean updateIsReleasing = true;
			updateReleasingStatus(key, updateIsReleasing);
			JSONObject taskQueueParameters = new JSONObject();
			taskQueueParameters.put("takeSalarySheetKey", key);
			taskQueueParameters.put("monthid", monthid);
			taskQueueParameters.put("yearid", yearid);
			taskQueueParameters.put("branchid", branchid);
			taskQueueParameters.put("generateInvoice", "" + generateInvoice);

			ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet", QueueConstantsHRIS.SALARY_PROCESS, taskQueueParameters);
		} else {
			LogUtility.writeLog("Take salary sheet record not found....");
		}
	}

	public static void updateReleasingStatus(Object key, boolean updateIsReleasing) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("is_releasing", updateIsReleasing);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static boolean getIsFreezedSalary(Object branchid, Object monthid, Object yearid, int freezToId) throws JSONException {
		String filter = "";
		if (branchid != null) {
			filter = " and branchid = " + branchid;
		}
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_freezattendancesalary");
		columnArray.put(Updates.KEY);
		columnArray.put("statusid");
		query.put(Data.Query.FILTERS, "monthid = " + monthid + " and yearid = " + yearid + " and freeztoid = " + freezToId + filter);

		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_freezattendancesalary");
		if (rows.length() > 0) {
			int employeeDecision = Translator.integerValue(rows.getJSONObject(0).opt("statusid"));
			if (employeeDecision == HRISApplicationConstants.EmployeeDecision.YES) {
				return true;
			}
		}
		return false;
	}

	public void regenerateEmployeeSalary(Object[] selectedkey) throws Exception {
		JSONArray array = getTakeSalarySheetRecords(selectedkey[0]);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		Object branchid = null;
		Object monthid = null;
		Object yearid = null;
		if (arrayCount > 0) {
			branchid = array.getJSONObject(0).opt("branchid");
			monthid = array.getJSONObject(0).opt("monthid");
			yearid = array.getJSONObject(0).opt("yearid");
			Object key = array.getJSONObject(0).opt("__key__");
			boolean isGenerating = Translator.booleanValue(array.getJSONObject(0).opt("is_generating"));
			boolean isReleasing = Translator.booleanValue(array.getJSONObject(0).opt("is_releasing"));
			if (isGenerating) {
				throw new BusinessLogicException("Salary Generation is already in progress. please wait...");
			}
			if (isReleasing) {
				throw new BusinessLogicException("Salary Release is already in progress. please wait...");
			}
			boolean isFreezed = getIsFreezedSalary(branchid, monthid, yearid, HRISApplicationConstants.Freez.SALARY);
			if (isFreezed) {
				throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
			}

			boolean updateIsGenerating = true;
			updateGeneratingStatus(key, updateIsGenerating);
			JSONObject taskQueueParameters = new JSONObject();
			taskQueueParameters.put("takeSalarySheetKey", key);
			taskQueueParameters.put("monthid", monthid);
			taskQueueParameters.put("yearid", yearid);
			taskQueueParameters.put("branchid", branchid);
			ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet", QueueConstantsHRIS.SALARY_PROCESS, taskQueueParameters);
		} else {
			LogUtility.writeLog("Take salary sheet record not found....");
		}
	}

	public static void updateGeneratingStatus(Object key, boolean updateIsGenerating) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("is_generating", updateIsGenerating);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public void regenerateEmployeeSalaryPerformanceHead(Object[] selectedkey) throws Exception {
		JSONArray array = getTakeSalarySheetRecords(selectedkey[0]);
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		Object branchid = null;
		Object monthid = null;
		Object yearid = null;
		if (arrayCount > 0) {
			branchid = array.getJSONObject(0).opt("branchid");
			monthid = array.getJSONObject(0).opt("monthid");
			yearid = array.getJSONObject(0).opt("yearid");
			Object key = array.getJSONObject(0).opt("__key__");
			boolean isGenerating = Translator.booleanValue(array.getJSONObject(0).opt("is_generating"));
			boolean isReleasing = Translator.booleanValue(array.getJSONObject(0).opt("is_releasing"));
			if (isGenerating) {
				throw new BusinessLogicException("Salary Generation is already in progress. please wait...");
			}
			if (isReleasing) {
				throw new BusinessLogicException("Salary Release is already in progress. please wait...");
			}

			boolean isFreezed = getIsFreezedSalary(branchid, monthid, yearid, HRISApplicationConstants.Freez.SALARY);
			if (isFreezed) {
				throw new BusinessLogicException("Salary Freezed Please contact Admin Department");
			}

			boolean updateIsGenerating = true;
			updateGeneratingStatus(key, updateIsGenerating);
			JSONObject taskQueueParameters = new JSONObject();
			taskQueueParameters.put("takeSalarySheetKey", key);
			taskQueueParameters.put("monthid", monthid);
			taskQueueParameters.put("yearid", yearid);
			taskQueueParameters.put("branchid", branchid);
			ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeSalaryGenerationPerformanceHead", QueueConstantsHRIS.SALARY_PROCESS, taskQueueParameters);
		} else {
			LogUtility.writeLog("Take salary sheet record not found....");
		}
	}

	private JSONArray getTakeSalarySheetRecords(Object key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("branchid");
		array.put("monthid");
		array.put("yearid");
		array.put("make_invoice");

		array.put("is_generating");
		array.put("is_releasing");

		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("takesalarysheets");
		return rows;
	}

	@Override
	public void onAfterUpdate(Record record) {

	}

	public static JSONArray getActiveEmployeeRecord(Object branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		columnArray.put("incrementduedate");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + " and branchid = " + branchId);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static JSONArray getRelievedEmployeeRecord(Object branchId, String monthFirstDate, String monthLastDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " and branchid = " + branchId + " and relievingdate >= '" + monthFirstDate + "'");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	Boolean isExists(Object yearId, Object monthId, Object branchId) throws JSONException {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "branchid = " + branchId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("takesalarysheets");
		if (rows != null && rows.length() > 0) {
			return true;
		} else {
			return false;
		}
	}
}
