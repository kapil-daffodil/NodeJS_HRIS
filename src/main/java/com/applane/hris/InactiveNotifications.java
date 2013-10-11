package com.applane.hris;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */
public class InactiveNotifications implements OperationJob {
	// job define on "hris_sendnotifications"
	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("insert")) {
			record.addUpdate("inactive", 0);
		}

	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

}
