package com.applane.hris;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class ResignationStatus implements OperationJob {
	// Job Define on "hris_employeeresignations" Resource
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

		Object resignationStatusObject = record.getValue("resignationstatusid");
		if (resignationStatusObject == null) {
			record.addUpdate("resignationstatusid", 1);
		}

	}

}
