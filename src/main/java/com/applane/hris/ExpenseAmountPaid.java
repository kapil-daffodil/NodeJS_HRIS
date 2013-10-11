package com.applane.hris;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */
public class ExpenseAmountPaid implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();

		if (operation.equalsIgnoreCase("update")) {
			if (record.has("paidamount")) {
				Object paidAmountObject = record.getValue("paidamount");
				if (paidAmountObject != null) {
					record.addUpdate("paid", 1);
				}
			}
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
