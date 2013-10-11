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
import com.applane.hris.HrisKinds;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeKPIBusinessLogic implements OperationJob {

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			String operationType = record.getOperationType();
			if (operationType.equals(Updates.Types.INSERT) || operationType.equals(Updates.Types.UPDATE)) {
				Object employeeId = record.getValue("employeeid");
				Object monthId = record.getValue("performancemonthid");
				Object yearId = record.getValue("performanceyearid");
				Object branchId = getBranchId(employeeId);
				if (branchId != null) {
					boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.VARIABLE);
					if (isFreezed) {
						throw new BusinessLogicException("Variable Freezed Please contact Admin Department.");
					}
				}
			}
		} catch (BusinessLogicException e) {
			throw new BusinessLogicException(e.getMessage());
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeKPIBusinessLogic.class.getName(), e);
			LogUtility.writeLog("EmployeeKPIBusinessLogic >> Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Ocured, Please Contact to Admin.");
		}
	}

	public static Object getBranchId(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		columnArray.put("branchid");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		if (rows.length() > 0) {
			return rows.getJSONObject(0).opt("branchid");
		}
		return null;
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
