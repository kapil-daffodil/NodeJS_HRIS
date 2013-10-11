package com.applane.hris;

import java.util.Date;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic job define on hris_employeedocuments
 */

public class EmployeeDocumentsValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object issueDateObject = record.getValue("issuedate");
		Object expiryDateObject = record.getValue("expirydate");
		String operation = record.getOperationType();
		Date issueDate = null;
		Date expiryDate = null;

		if (operation.equalsIgnoreCase("insert")) {
			if (issueDateObject != null && expiryDateObject != null) {
				issueDate = CommunicationUtility.checkDateFormat(issueDateObject);
				expiryDate = CommunicationUtility.checkDateFormat(expiryDateObject);
				if (expiryDate.before(issueDate)) {
					throw new BusinessLogicException("Document expiry date must be greater than OR equal to issue date.");
				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			if (issueDateObject != null && expiryDateObject != null) {
				issueDate = CommunicationUtility.checkDateFormat(issueDateObject);
				expiryDate = CommunicationUtility.checkDateFormat(expiryDateObject);
				// / compare parsed valid from date and valid to date
				if (expiryDate.before(issueDate)) {
					throw new BusinessLogicException("Document expiry date must be greater than OR equal to issue date.");
				}
			}
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}
}
