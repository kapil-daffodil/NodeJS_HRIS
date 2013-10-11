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
public class ResignationDateValidation implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		Object resignationDateObject = record.getValue("resignationdate");
		Object relievingDateObject = record.getValue("proposedrelievingdate");
		// String operation = record.getOperationType();
		// SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		Date resignationDate = null;
		Date relievingDate = null;

		if (resignationDateObject != null && relievingDateObject != null) {

			try {
				resignationDate = updateDateFormat.parse("" + resignationDateObject);
				relievingDate = updateDateFormat.parse("" + relievingDateObject);

			} catch (ParseException e) {
				e.printStackTrace();
				throw new BusinessLogicException(e.getMessage());
			}
			if (relievingDate.before(resignationDate)) {
				throw new BusinessLogicException("Relieving Date must be greater than or equal to Resignation Date.");
			}

		}

	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

}
