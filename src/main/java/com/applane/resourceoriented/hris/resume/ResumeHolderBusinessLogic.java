package com.applane.resourceoriented.hris.resume;

import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class ResumeHolderBusinessLogic implements OperationJob {

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		if (record.has("temp")) {
			return;
		}
		try {
			String operationType = record.getOperationType();
			if (operationType.equals(Updates.Types.INSERT)) {
				Object key = record.getKey();
				updateMrfCode(key);
				record.addUpdate("resume_code", key);
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			throw new RuntimeException("ResumeHolderBusinessLogic >> Trace >> " + trace);
		}

	}

	private void updateMrfCode(Object key) throws Exception {
		JSONObject updateColumn = new JSONObject();
		updateColumn.put(Updates.KEY, key);
		updateColumn.put("resume_code", key);
		updateColumn.put("temp", "Temp");
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, updateColumn);
		updates.put(Data.Update.RESOURCE, "hris_resume_holder");
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		if (record.has("temp")) {
			return;
		}
		// Object hrisResumeHolderEmployeementStatus = record.getValue("hris_resume_holder_employeement_status");
		// Object hrisResumeHolderEmployeementStatusOld = record.getOldValue("hris_resume_holder_employeement_status");

		// if (hrisResumeHolderEmployeementStatus != null && hrisResumeHolderEmployeementStatus instanceof JSONArray) {
		// JSONArray array = (JSONArray) hrisResumeHolderEmployeementStatus;
		// LogUtility.writeError("hrisResumeHolderEmployeementStatus >> " + array);
		// HashMap<Date, Date> employeementStatusMap = new HashMap<Date, Date>();
		// for (int counter = 0; counter < array.length(); counter++) {
		// Date fromDate = Translator.dateValue(array.getJSONObject(counter).opt("from_date"));
		// Date toDate = Translator.dateValue(array.getJSONObject(counter).opt("to_date"));
		// if (fromDate != null && startDate.after(fromDate)) {
		// throw new BusinessLogicException("(Lodging Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
		// }
		// if (toDate != null && endDate.before(toDate)) {
		// throw new BusinessLogicException("(Lodging Details) From Date and To Date of All Details Should Be Between. Start Date:\"" + record.getValue("startdate") + "\" and End Date: \"" + record.getValue("enddate") + "\"");
		// }
		// }
		// }
		// if (hrisResumeHolderEmployeementStatusOld != null && hrisResumeHolderEmployeementStatusOld instanceof JSONArray) {
		// JSONArray array = (JSONArray) hrisResumeHolderEmployeementStatusOld;
		// LogUtility.writeError("hrisResumeHolderEmployeementStatusOld >> " + array);
		// }
	}
}