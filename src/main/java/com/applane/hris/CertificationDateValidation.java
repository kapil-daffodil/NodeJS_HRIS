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
 
public class CertificationDateValidation implements OperationJob{

// job definr on hris_employeecertifications
	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object validFromDateObject = record.getValue("validfrom");
		Object validToDateObject = record.getValue("validto");
		String operation = record.getOperationType();
		SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		java.util.Date validFromDate = null;
		java.util.Date validToDate = null;

		if (operation.equalsIgnoreCase("insert")) {
			if (validFromDateObject != null && validToDateObject != null) {
				if ((validFromDateObject instanceof String) && (validToDateObject instanceof String)) {
					try {
						validFromDate = queryDateFormat.parse("" + validFromDateObject);
						validToDate = queryDateFormat.parse("" + validToDateObject);
					} catch (ParseException e) {
						e.printStackTrace();
						throw new BusinessLogicException(e.getMessage());
					}

					if (validToDate.before(validFromDate)) {
						throw new BusinessLogicException("Certification Valid To Date must be greater than Valid From Date.");
					}
				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			if (validFromDateObject != null && validToDateObject != null) {
				// in case of update we get two formats of date so below handling is required
				// parse start date
				try {
					validFromDate = queryDateFormat.parse("" + validFromDateObject);
				} catch (ParseException e) {
					e.printStackTrace();
					try {
						validFromDate = updateDateFormat.parse("" + validFromDateObject);
					} catch (ParseException e1) {
						throw new RuntimeException("Valid from date is not parsable" + e.getMessage());
					}
				}

				// parse end date
				try {
					validToDate = queryDateFormat.parse("" + validToDateObject);
				} catch (ParseException e) {
					e.printStackTrace();
					try {
						validToDate = updateDateFormat.parse("" + validToDateObject);
					} catch (ParseException e1) {
						throw new RuntimeException("Valid to date is not parsable" + e.getMessage());
					}
				}

				// / compare parsed valid from date and valid to date
				if (validToDate.before(validFromDate)) {
					throw new BusinessLogicException("Certification Valid To Date must be greater than Valid From Date.");
				}
			}
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		
	}

}
