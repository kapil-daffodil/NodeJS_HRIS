package com.applane.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class VisaDateValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object issueDateObject = record.getValue("dateofissue");
		Object expiryDateObject = record.getValue("dateofexpiry");
		String operation = record.getOperationType();
		SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		java.util.Date issueDate = null;
		java.util.Date expiryDate = null;

		if (operation.equalsIgnoreCase("insert")) {
			if (issueDateObject != null && expiryDateObject != null) {
				if ((issueDateObject instanceof String) && (expiryDateObject instanceof String)) {
					try {
						issueDate = queryDateFormat.parse("" + issueDateObject);
						expiryDate = queryDateFormat.parse("" + expiryDateObject);
					} catch (ParseException e) {
						throw new BusinessLogicException(e.getMessage());
					}

					if (expiryDate.before(issueDate)) {
						throw new BusinessLogicException("Visa Expiry Date must be greater than Issue Date.");
					}
				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			if (issueDateObject != null && expiryDateObject != null) {
				// in case of update we get two formats of date so below handling is required
				// parse start date
				try {
					issueDate = queryDateFormat.parse("" + issueDateObject);
				} catch (ParseException e) {
					try {
						issueDate = updateDateFormat.parse("" + issueDateObject);
					} catch (ParseException e1) {
						throw new RuntimeException("Issue date is not parsable" + e.getMessage());
					}
				}

				// parse end date
				try {
					expiryDate = queryDateFormat.parse("" + expiryDateObject);
				} catch (ParseException e) {
					try {
						expiryDate = updateDateFormat.parse("" + expiryDateObject);
					} catch (ParseException e1) {
						throw new RuntimeException("Expiry date is not parsable" + e.getMessage());
					}
				}

				// / compare parsed valid from date and valid to date
				if (expiryDate.before(issueDate)) {
					throw new BusinessLogicException("Visa Expiry Date must be greater than Issue Date.");
				}
			}
		}
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
