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
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeBusinessLogic implements OperationJob {
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onBeforeUpdate(Record record) {
	}

	private JSONArray getEmployeeKPI(Object businessFunctionId, Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "keyperformanceindicators");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put("target");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "businessfunctionid = " + businessFunctionId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray kpiArray = object.getJSONArray("keyperformanceindicators");
		return kpiArray;
	}

	private void insertEmployeeKPI(Object businessFunctionId, Object kpiId, Object employeeId, Object target) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeekpi");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("businessfunctionid", businessFunctionId);
		row.put("keyperformanceindicatorid", kpiId);
		if (target != null) {
			row.put("target", target);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static boolean findDuplicateEntry(Object employeeId, Object businessFunctionId, Object kpiId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeekpi");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and businessfunctionid = " + businessFunctionId + " and keyperformanceindicatorid = " + kpiId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray kpiArray = object.getJSONArray("employeekpi");
		int kpiArrayCount = kpiArray == null ? 0 : kpiArray.length();
		if (kpiArrayCount > 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		if (record.getNewValue("temp") == null) {
			String operationType = record.getOperationType();
			if (operationType.equalsIgnoreCase("insert")) {
				Object employeeId = record.getValue("__key__");
				try {
					resolveEmployeeKPIs(employeeId);
				} catch (DeadlineExceededException e) {
					LogUtility.writeLog("First exception block...");
					throw e;
				} catch (BusinessLogicException e) {
					LogUtility.writeLog("Second exception block");
					throw e;
				} catch (Exception e) {
					LogUtility.writeLog("Third Exception block...");
					e.printStackTrace();
					throw new BusinessLogicException("Some unknown error ocuured while insert employee KPI's.");
				}
			} else if (operationType.equalsIgnoreCase("update")) {
				Object employeeId = record.getValue("__key__");
				if (record.has("businessfunctionid")) {
					try {
						resolveEmployeeKPIs(employeeId);
					} catch (DeadlineExceededException e) {
						LogUtility.writeLog("First exception block...");
						throw e;
					} catch (BusinessLogicException e) {
						LogUtility.writeLog("Second exception block");
						throw e;
					} catch (Exception e) {
						LogUtility.writeLog("Third Exception block...");
						e.printStackTrace();
						throw new BusinessLogicException("Some unknown error ocuured while update employee KPI's.");
					}
				}
			}
		}
	}

	private void resolveEmployeeKPIs(Object employeeId) throws JSONException {
		JSONArray businessFunctionArray = getEmployeeBusinessFunctions(employeeId);
		int businessFunctionArrayCount = (businessFunctionArray == null || businessFunctionArray.length() == 0) ? 0 : businessFunctionArray.length();
		if (businessFunctionArrayCount > 0) {
			for (int counter = 0; counter < businessFunctionArrayCount; counter++) {
				Object businessFunctionId = businessFunctionArray.getJSONObject(counter).opt("businessfunctionid");
				Object parentBusinessFunctionId = businessFunctionArray.getJSONObject(counter).opt("businessfunctionid.parentbusinessfunctionid");
				JSONArray businessFunctionKPIArray = getEmployeeKPI(businessFunctionId, employeeId);
				int businessFunctionKPICount = (businessFunctionKPIArray == null || businessFunctionKPIArray.length() == 0) ? 0 : businessFunctionKPIArray.length();
				if (businessFunctionKPICount > 0) {
					for (int counter1 = 0; counter1 < businessFunctionKPICount; counter1++) {
						Object kpiId = businessFunctionKPIArray.getJSONObject(counter1).opt(Updates.KEY);
						Object target = businessFunctionKPIArray.getJSONObject(counter1).opt("target");
						if (!findDuplicateEntry(employeeId, businessFunctionId, kpiId)) {
							insertEmployeeKPI(businessFunctionId, kpiId, employeeId, target);
						}
					}
				}
				if (parentBusinessFunctionId != null) {
					JSONArray parentBusinessFunctionKPIArray = getEmployeeKPI(parentBusinessFunctionId, employeeId);
					int parentBusinessFunctionKPICount = (parentBusinessFunctionKPIArray == null || parentBusinessFunctionKPIArray.length() == 0) ? 0 : parentBusinessFunctionKPIArray.length();
					if (parentBusinessFunctionKPICount > 0) {
						for (int counter1 = 0; counter1 < parentBusinessFunctionKPICount; counter1++) {
							Object parentBusinessFunctionKPIId = parentBusinessFunctionKPIArray.getJSONObject(counter1).opt("__key__");
							Object target = parentBusinessFunctionKPIArray.getJSONObject(counter1).opt("target");
							if (!findDuplicateEntry(employeeId, businessFunctionId, parentBusinessFunctionKPIId)) {
								insertEmployeeKPI(parentBusinessFunctionId, parentBusinessFunctionKPIId, employeeId, target);
							}
						}
					}
				}
			}
		}

	}

	private JSONArray getEmployeeBusinessFunctions(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees__businessfunctionid");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("hris_employees");
		array.put("businessfunctionid");
		array.put("businessfunctionid.parentbusinessfunctionid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "hris_employees = " + employeeId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray businessFunctionArray = object.getJSONArray("hris_employees__businessfunctionid");
		return businessFunctionArray;
	}

	public void syncBusinessFunctionKPI(Object[] selectedkey) throws JSONException {
		int keysCount = selectedkey == null ? 0 : selectedkey.length;
		try {
			for (int counter = 0; counter < keysCount; counter++) {
				resolveEmployeeKPIs(selectedkey[counter]);
			}
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeCardPunchDataBusinessLogic >> syncEmployeeAttendanceOnInvoke >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}
}
