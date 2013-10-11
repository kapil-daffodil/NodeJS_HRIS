package com.applane.resourceoriented.hris;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.google.apphosting.api.DeadlineExceededException;

public class ImportEmployeeValidation implements OperationJob {

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		Object employeeCode = record.getValue("EMPLOYEE_CODE");
		Object department = record.getValue("DEPARTMENT");
		Object holidayCalendar = record.getValue("HOLIDAY_CALENDAR");
		Object schedule = record.getValue("SCHEDULE");
		Object branch = record.getValue("BRANCH");
		Object designation = record.getValue("DESIGNATION");
		Object businessFunction = record.getValue("BUSINESS_FUNCTIONS");

		if (employeeCode == null) {
			throw new BusinessLogicException("Employee Code Can Not Be Blank");
		}

		if (department == null) {
			throw new BusinessLogicException("Employee Department Can Not Be Blank");
		}

		if (holidayCalendar == null) {
			throw new BusinessLogicException("Employee Holiday Calendar Can Not Be Blank");
		}

		if (schedule == null) {
			throw new BusinessLogicException("Employee Schedule Can Not Be Blank");
		}

		if (branch == null) {
			throw new BusinessLogicException("Employee Branch Can Not Be Blank");
		}

		if (designation == null) {
			throw new BusinessLogicException("Employee Designation Can Not Be Blank");
		}

		if (businessFunction == null) {
			throw new BusinessLogicException("Employee Business Function Can Not Be Blank");
		}
	}

}
