package com.applane.resourceoriented.hris.freez;

import java.text.SimpleDateFormat;

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
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class FreezUnFreezRecordsBusinessLogic implements OperationJob {

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		String operationType = record.getOperationType();
		try {
			String currentUserEmail = "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
			String organization = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION));
			if (organization.toLowerCase().indexOf("daffodil") != -1) {
				if (!currentUserEmail.equals(ApplicationConstants.OWNER) && !currentUserEmail.equals(ApplicationConstants.USER)) {
					throw new BusinessLogicException("You are not authorized to any kind of changes in Freez Setup.");
				}
			}
			if (operationType.equals(Updates.Types.INSERT)) {

			} else if (operationType.equals(Updates.Types.UPDATE)) {
				Object key = record.getKey();

				String updatedBy = Translator.stringValue(CurrentState.getCurrentState().opt(CurrentSession.CURRENT_USER_EMAIL));

				String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());

				JSONObject newValues = record.getValues();

				JSONObject oldRecords = new JSONObject();
				oldRecords.put("branchid", record.getOldValue("branchid"));
				oldRecords.put("monthid", record.getOldValue("monthid"));
				oldRecords.put("yearid", record.getOldValue("yearid"));
				oldRecords.put("freeztoid", record.getOldValue("freeztoid"));
				oldRecords.put("statusid", record.getOldValue("statusid"));

				insertFreezHistoory(key, updatedBy, TODAY_DATE, newValues, oldRecords);

			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(FreezUnFreezRecordsBusinessLogic.class.getName(), e);
			LogUtility.writeLog("FreezUnFreezRecordsBusinessLogic >> trace > onbefore updae >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured, Please try after some time.");
		}
	}

	private void insertFreezHistoory(Object key, String updatedBy, String todayDateInString, JSONObject newValues, JSONObject oldRecords) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_freezattendancesalaryhistory");
		JSONObject row = new JSONObject();
		row.put("freezattendancesalaryid", key);
		row.put("updatedby", updatedBy);
		row.put("date", todayDateInString);
		row.put("updatedcolumn", "New Values -[" + newValues + "] - Old values -[" + oldRecords + "]");
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
