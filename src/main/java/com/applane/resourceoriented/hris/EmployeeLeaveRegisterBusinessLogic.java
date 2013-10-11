package com.applane.resourceoriented.hris;

import com.applane.assistance.Translator;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeLeaveRegisterBusinessLogic implements OperationJob {

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
	}

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			if (record.has("employeeid.employeecode")) {
				Object employeeId = PunchingUtility.getId("hris_employees", "employeecode", "" + record.getValue("employeeid.employeecode"));
				record.removeUpdate("employeeid.employeecode");
				record.addUpdate("employeeid", employeeId);

				if (record.has("monthid.name")) {
					Object monthId = PunchingUtility.getId("organization_months", "name", "" + record.getValue("monthid.name"));
					if (monthId == null) {
						throw new BusinessLogicException("Month Not Found");
					}
					record.removeUpdate("monthid.name");
					record.addUpdate("monthid", monthId);
				} else {
					throw new BusinessLogicException("Month Not Found");
				}
				if (record.has("yearid.name")) {
					Object yearId = PunchingUtility.getId("organization_years", "name", "" + record.getValue("yearid.name"));
					if (yearId == null) {
						throw new BusinessLogicException("Year Not Found");
					}
					record.removeUpdate("monthid.name");
					record.addUpdate("yearid", yearId);
				} else {
					throw new BusinessLogicException("Year Not Found");
				}
				if (record.has("leavetypeid.name")) {
					Object leaveTypeId = PunchingUtility.getId("leavetypes", "name", "" + record.getValue("leavetypeid.name"));
					if (leaveTypeId == null) {
						throw new BusinessLogicException("Leave Type Not Found");
					}
					record.removeUpdate("leavetypeid.name");
					record.addUpdate("leavetypeid", leaveTypeId);
				} else {
					throw new BusinessLogicException("Leave Type Not Found");
				}
				double openingBalance = Translator.doubleValue(record.getValue("openingbalance"));
				record.addUpdate("leavebalance", openingBalance);
				record.addUpdate("closingbalance", openingBalance);
			}
		} catch (Exception e) {
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "EmployeeLeaveRegisterBusinessLogic >> onBeforeUpdate >> Exception");
		}
	}
}