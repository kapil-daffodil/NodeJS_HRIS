package com.applane.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class WorkExperienceDateValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object fromDateObject = record.getValue("fromdate");
		Object toDateObject = record.getValue("todate");
		String operation = record.getOperationType();
		SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		Date fromDate = null;
		Date toDate = null;

		if (operation.equalsIgnoreCase("insert")) {
			if (fromDateObject != null && toDateObject != null) {

				try {
					fromDate = queryDateFormat.parse("" + fromDateObject);
					toDate = queryDateFormat.parse("" + toDateObject);
				} catch (ParseException e) {
					throw new BusinessLogicException(e.getMessage());
				}
				if (toDate.before(fromDate)) {
					throw new BusinessLogicException("Experience To Date must be greater than From Date.");
				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			if (fromDateObject != null && toDateObject != null) {
				// in case of update we get two formats of date so below handling is required
				// parse start date
				try {
					fromDate = queryDateFormat.parse("" + fromDateObject);
				} catch (ParseException e) {
					try {
						fromDate = updateDateFormat.parse("" + fromDateObject);
					} catch (ParseException e1) {
						throw new BusinessLogicException("From date is not parsable" + e.getMessage());
					}
				}

				// parse end date
				try {
					toDate = queryDateFormat.parse("" + toDateObject);
				} catch (ParseException e) {
					try {
						toDate = updateDateFormat.parse("" + toDateObject);
					} catch (ParseException e1) {
						throw new BusinessLogicException("To date is not parsable" + e.getMessage());
					}
				}

				// / compare parsed valid from date and valid to date
				if (toDate.before(fromDate)) {
					throw new BusinessLogicException("Experience To Date must be greater than From Date.");
				}
			}
		}
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
