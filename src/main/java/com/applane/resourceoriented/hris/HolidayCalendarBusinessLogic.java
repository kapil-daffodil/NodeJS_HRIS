package com.applane.resourceoriented.hris;

import java.util.Date;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;

public class HolidayCalendarBusinessLogic implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws VetoException {

		String operationType = record.getOperationType();
		if (operationType.equalsIgnoreCase("insert")) {
			Object fromDateObject = record.getValue("fromdate");
			Object toDateObject = record.getValue("todate");
			if (fromDateObject != null && toDateObject != null) {
				holidayCalendarDateValidation(fromDateObject, toDateObject);
			}
		} else if (operationType.equalsIgnoreCase("update")) {
			Object fromDateObject = record.getValue("fromdate");
			Object toDateObject = record.getValue("todate");
			if (fromDateObject != null && toDateObject != null) {
				holidayCalendarDateValidation(fromDateObject, toDateObject);
			}
		}
	}

	private void holidayCalendarDateValidation(Object fromDateObject, Object toDateObject) {
		Date fromDate = DataTypeUtilities.checkDateFormat(fromDateObject);

		Date toDate = DataTypeUtilities.checkDateFormat(toDateObject);
		if (toDate.before(fromDate)) {
			throw new BusinessLogicException("Holiday calendar To Date must be greater than OR equal to From Date.");
		}
	}

	@Override
	public void onAfterUpdate(Record record) {

	}
}
