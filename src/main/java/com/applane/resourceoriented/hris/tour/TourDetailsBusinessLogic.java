package com.applane.resourceoriented.hris.tour;

import java.util.Date;

import com.applane.assistance.Translator;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

public class TourDetailsBusinessLogic implements OperationJob {

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {

		Date fromDate = Translator.dateValue(record.getValue("from_date"));
		Date toDate = Translator.dateValue(record.getValue("to_date"));
		if (fromDate != null && toDate != null && fromDate.after(toDate)) {
			throw new BusinessLogicException("From Date Should Be Less Or Equalto To Date.");
		}
		double actualAmount = Translator.doubleValue(record.getValue("actual_amount"));
		double approvedAmount = Translator.doubleValue(record.getValue("approved_amount"));
		if (record.getValue("actual_amount") == null) {
			actualAmount = Translator.doubleValue(record.getValue("amount"));
		}
		if (actualAmount < approvedAmount) {
			throw new BusinessLogicException("Approved Amount can not be exceded Actual Amount");
		}
	}

}
