package com.applane.resourceoriented.hris;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

public class FullAndFinalBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record arg0) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		if (record.has("employeeid.employeecode")) {
			record.removeUpdate("employeeid.employeecode");
		}
		if (record.has("employeeid.designationid")) {
			record.removeUpdate("employeeid.designationid");
		}
		if (record.has("employeeid.designationid.name")) {
			record.removeUpdate("employeeid.designationid.name");
		}
		if (record.has("employeeid.resign_application_date")) {
			record.removeUpdate("employeeid.resign_application_date");
		}
		if (record.has("employeeid.ctc")) {
			record.removeUpdate("employeeid.ctc");
		}
	}

}
