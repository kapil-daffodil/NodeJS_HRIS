package com.applane.resourceoriented.hris.shifts;

import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HrisUserDefinedFunctions;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class ShiftChangeByTlBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

		LogUtility.writeLog(getClass().getSimpleName() + " >> onAfterUpdate >> Record Values : " + record.getValues());

		try {
			Object employeeId = record.getValue("employee_id");
			Object employeeName = record.getValue("employee_id.name");
			Object employeeCode = record.getValue("employee_id.employeecode");
			String employeeEmailId = Translator.stringValue(record.getValue("employee_id.officialemailid"));
			Object shiftId = record.getValue("shift_id");
			Object shiftName = record.getValue("shift_id.name");
			Object fromDate = record.getValue("from_date");
			Object toDate = record.getValue("to_date");

			String shiftRequestOffDays = HrisUserDefinedFunctions.getShiftChangeByTLOffDays(record);
			sendMailToCabAdmin(employeeName, employeeCode, shiftName, fromDate, toDate, shiftRequestOffDays, employeeEmailId);

			insertUpdateRecord(employeeId, shiftId, fromDate, toDate);
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

		LogUtility.writeLog(getClass().getSimpleName() + " >> onAfterUpdate End");
	}

	private void insertUpdateRecord(Object employeeId, Object shiftId, Object fromDate, Object toDate) throws Exception {

		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("from_date", fromDate);
		row.put("to_date", toDate);
		row.put("shift_id", shiftId);

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_shift_history");
		updates.put(Data.Update.UPDATES, row);

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public void onBeforeUpdate(Record records) throws VetoException, DeadlineExceededException {

		Object fromDate = records.getValue("from_date");
		Object toDate = records.getValue("to_date");
		if (Translator.dateValue(fromDate).after(Translator.dateValue(toDate))) {
			throw new BusinessLogicException("From Date should be less or equal to To Date.");
		}
	}

	private void sendMailToCabAdmin(Object employeeName, Object employeeCode, Object shiftName, Object fromDate, Object toDate, String shiftRequestOffDays, String employeeEmailId) throws Exception {

		LogUtility.writeLog(getClass().getSimpleName() + " >> sendMailToCabAdmin Begin");

		String[] cabAdminNameAndOfficialEmailId = HrisUserDefinedFunctions.getCabAdminNameAndOfficialEmailId();

		String cabAdminName = cabAdminNameAndOfficialEmailId[0];
		String cabAdminOfficialEmailId = cabAdminNameAndOfficialEmailId[1];

		if (cabAdminOfficialEmailId != null && cabAdminOfficialEmailId.length() > 0) {

			String currentUserEmailId = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));
			JSONObject employeeDetail = HrisUserDefinedFunctions.getEmployeeDetail(currentUserEmailId);

			String currentEmployeeName = "";
			String currentEmployeeCode = "";

			if (employeeDetail != null) {
				currentEmployeeName = employeeDetail.optString(HrisKinds.Employess.NAME);
				currentEmployeeCode = employeeDetail.optString(HrisKinds.Employess.EMPLOYEE_CODE);
			}

			String title = "Shift Change Request By " + currentEmployeeName + "," + currentEmployeeCode;

			StringBuffer message = new StringBuffer();
			message.append("Dear ").append(cabAdminName).append("<br /><br />");
			message.append("<b>").append(currentEmployeeName).append(",").append(currentEmployeeCode).append("</b> has requested to change <b>").append(employeeName).append(",").append(employeeCode).append("</b> shift to ").append(shiftName).append(". The details are as follows<br /><br />");
			message.append("<b>Shift Name:- </b>").append(shiftName).append("<br />");
			message.append("<b>From Date:- </b>").append(fromDate).append("<br />");
			message.append("<b>To Date:- </b>").append(toDate).append("<br />");
			message.append("<b>Off Days:- </b>").append(shiftRequestOffDays).append("<br /><br />");
			message.append("Regards").append("<br />");
			message.append(currentEmployeeName);

			LogUtility.writeLog("Current User Email Id : " + currentUserEmailId + ", Employee Email Id : " + employeeEmailId + ", Cab Admin Email Id : " + cabAdminOfficialEmailId);

			String[] ccEmailIds = { currentUserEmailId, employeeEmailId };
			PunchingUtility.sendMailsAccToProfitCenters(new String[] { cabAdminOfficialEmailId }, message.toString(), title, ccEmailIds);

			LogUtility.writeLog(getClass().getSimpleName() + " >> sendMailToCabAdmin End");
		}
	}
}
