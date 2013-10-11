package com.applane.resourceoriented.hris.freez;

import org.json.JSONArray;
import org.json.JSONException;
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
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class CheckEmployeeFreezOrNot implements OperationJob {

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			Object employeeId = record.getValue("employeeid");
			if (employeeId == null) {
				employeeId = record.getValue("employeeid.__key__");
			}
			String filter = "";
			if (employeeId != null) {
				filter = Updates.KEY + "=" + employeeId;
			} else {
				Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
				filter = HrisKinds.Employess.OFFICIAL_EMAIL_ID + "='" + currentUserEmail + "'";
			}

			JSONArray rows = getEmployeeIfFreez(filter);
			if (rows != null && rows.length() > 0) {
				boolean isFreez = false;
				int freeze = Translator.integerValue(rows.getJSONObject(0).opt("contacticonid"));
				if (freeze == HRISApplicationConstants.EmployeeDecision.YES) {
					isFreez = true;
				}
				if (isFreez) {
					throw new BusinessLogicException("EmployeeId Freezed Please Unfreez First Or Contact To HR.");
				}
				if (employeeId == null) {
					employeeId = rows.getJSONObject(0).opt(Updates.KEY);
					record.addUpdate("employeeid", employeeId);
				}
			} else {
				throw new BusinessLogicException("EmployeeId Not found");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("CheckEmployeeFreezOrNot >> Exception Trace >> " + trace);
			throw new RuntimeException("Some Unknown Exception Occure, Please Contace To Admin.");
		}
	}

	private JSONArray getEmployeeIfFreez(Object filter) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		query.put(Data.Query.FILTERS, filter);
		JSONArray columns = new JSONArray();
		columns.put(Updates.KEY);
		columns.put("contacticonid");
		query.put(Data.Query.COLUMNS, columns);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees");
	}

}
