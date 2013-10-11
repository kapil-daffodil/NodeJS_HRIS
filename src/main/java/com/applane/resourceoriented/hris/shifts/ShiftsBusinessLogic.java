package com.applane.resourceoriented.hris.shifts;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.backend.exception.BusinessLogicException;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HrisUserDefinedFunctions;
import com.applane.resourceoriented.hris.leave.LeaveMail;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class ShiftsBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {

		LogUtility.writeLog("ShiftsBusinessLogic >> onAfterUpdate >> Record Values : " + record.getValues());

		String operationType = record.getOperationType();
		try {
			Object employeeId = record.getValue("employeeid");
			List<String> employeeEmailIdList = new ArrayList<String>();
			JSONArray indirectingReportingToIds = new LeaveMail().getIndirectingReportingToIds(employeeId);
			if (indirectingReportingToIds != null && indirectingReportingToIds.length() > 0) {
				for (int counter = 0; counter < indirectingReportingToIds.length(); counter++) {
					String offMail = indirectingReportingToIds.getJSONObject(counter).optString("indirectreportingto.officialemailid");
					// Object indId = indirectingReportingToIds.getJSONObject(counter).opt("indirectreportingto");
					employeeEmailIdList.add(offMail);
				}
			}
			if (operationType.equals(Updates.Types.INSERT)) {
				int statusId = Translator.integerValue(record.getValue("statusid"));
				if (statusId == HRISConstants.LEAVE_NEW) {
					sendEmailOnNew(employeeEmailIdList, record);
				}
			}
			if (operationType.equals(Updates.Types.UPDATE)) {
				int statusIdNew = Translator.integerValue(record.getNewValue("statusid"));
				int oldStatusId = Translator.integerValue(record.getOldValue("statusid"));
				if (oldStatusId == HRISConstants.LEAVE_NEW && statusIdNew == HRISConstants.LEAVE_NEW) {
					// Object historyid = getHistoryId(shiftRequestId);
					sendEmailOnNew(employeeEmailIdList, record);
				}
				if (oldStatusId == HRISConstants.LEAVE_NEW && statusIdNew == HRISConstants.LEAVE_APPROVED) {
					Object shiftRequestId = record.getKey();
					Object shiftId = record.getValue("shift_id");
					Object fromDate = record.getValue("from_date");
					Object toDate = record.getValue("to_date");
					Object historyid = null;
					insertUpdateRecord(historyid, employeeId, shiftRequestId, shiftId, fromDate, toDate);
					sendEmailOnApprove(employeeEmailIdList, record);
				}
				if ((oldStatusId == HRISConstants.LEAVE_NEW && statusIdNew == HRISConstants.LEAVE_REJECTED) || (oldStatusId == HRISConstants.LEAVE_APPROVED && statusIdNew == HRISConstants.LEAVE_REJECTED)) {
					// if(statusIdNew == HRISConstants.LEAVE_APPROVED && oldStatusId == HRISConstants.LEAVE_REJECTED){
					// delete shift from employee history
					// }
					sendEmailOnReject(employeeEmailIdList, record);
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private void sendEmailOnReject(List<String> employeeEmailIdList, Record record) throws Exception {
		Object shiftName = record.getValue("shift_id.name");
		Object fromDate = record.getValue("from_date");
		Object toDate = record.getValue("to_date");
		Object requestDate = record.getValue("request_date");
		Object reason = record.getValue("reason");
		Object employeeEmail = record.getValue("employeeid.officialemailid");
		Object employeeName = record.getValue("employeeid.name");
		Object employeeCode = record.getValue("employeeid.employeecode");
		Object approverEmail = record.getValue("approverid.officialemailid");
		Object approverName = record.getValue("approverid.name");
		if (approverEmail != null && approverEmail.toString().length() > 0) {
			employeeEmailIdList.add(approverEmail.toString());
		}

		String[] emailsInCC = new String[employeeEmailIdList.size()];
		for (int counter = 0; counter < employeeEmailIdList.size(); counter++) {
			emailsInCC[counter] = employeeEmailIdList.get(counter);
		}
		String title = "Shift Change Request By " + employeeName + "(" + employeeCode + ") on " + requestDate + " Rejected";
		StringBuffer message = new StringBuffer();
		message.append("Dear ").append(approverName).append("<BR /><BR />");
		message.append(employeeName).append(" has requested to change his/her shift to ").append(shiftName).append(" has been rejected.").append("<BR /><BR />");
		message.append("<b>Shift Name:- </b>").append(shiftName).append("<BR />");
		message.append("<b>From Date:- </b>").append(fromDate).append("<BR />");
		message.append("<b>To Date:- </b>").append(toDate).append("<BR />");
		message.append("<b>Reason:- </b>").append(reason).append("<BR /><BR />");
		message.append("Regards").append("<BR />");
		message.append(employeeName);
		PunchingUtility.sendMailsAccToProfitCenters(new String[] { employeeEmail.toString() }, message.toString(), title, emailsInCC);

	}

	private void sendEmailOnApprove(List<String> employeeEmailIdList, Record record) throws Exception {

		Object shiftName = record.getValue("shift_id.name");
		Object fromDate = record.getValue("from_date");
		Object toDate = record.getValue("to_date");
		Object requestDate = record.getValue("request_date");
		Object reason = record.getValue("reason");
		Object employeeEmail = record.getValue("employeeid.officialemailid");
		Object employeeName = record.getValue("employeeid.name");
		Object employeeCode = record.getValue("employeeid.employeecode");
		Object approverEmail = record.getValue("approverid.officialemailid");
		Object approverName = record.getValue("approverid.name");

		if (approverEmail != null && approverEmail.toString().length() > 0) {
			employeeEmailIdList.add(approverEmail.toString());
		}

		String cabNotificationMail = getCabNotificationMail();
		if (cabNotificationMail != null) {
			employeeEmailIdList.add(cabNotificationMail);
		}

		String[] emailsInCC = new String[employeeEmailIdList.size()];
		for (int counter = 0; counter < employeeEmailIdList.size(); counter++) {
			emailsInCC[counter] = employeeEmailIdList.get(counter);
		}

		String shiftRequestOffDays = HrisUserDefinedFunctions.getShiftRequestOffDays(record);

		String title = "Shift Change Request By " + employeeName + "(" + employeeCode + ") on " + requestDate + " Approved";

		StringBuffer message = new StringBuffer();
		message.append("Dear ").append(approverName).append("<BR /><BR />");
		message.append(employeeName).append(" has requested to change his/her shift to ").append(shiftName).append(" has been approved.").append("<BR /><BR />");
		message.append("<b>Shift Name:- </b>").append(shiftName).append("<BR />");
		message.append("<b>From Date:- </b>").append(fromDate).append("<BR />");
		message.append("<b>To Date:- </b>").append(toDate).append("<BR />");
		message.append("<b>Reason:- </b>").append(reason).append("<BR />");
		message.append("<b>Off Days:- </b>").append(shiftRequestOffDays).append("<BR /><BR />");
		message.append("Regards").append("<BR />");
		message.append(employeeName);

		PunchingUtility.sendMailsAccToProfitCenters(new String[] { employeeEmail.toString() }, message.toString(), title, emailsInCC);
	}

	public static String getCabNotificationMail() throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		JSONArray array = new JSONArray();
		array.put("cab_admin_employee_id.officialemailid");
		query.put(Data.Query.COLUMNS, array);
		JSONObject attendanceObject;
		attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_mailconfigurations");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).optString("cab_admin_employee_id.officialemailid", null);
		}
		return null;
	}

	private void insertUpdateRecord(Object historyRequestId, Object employeeId, Object shiftRequestId, Object shiftId, Object fromDate, Object toDate) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_shift_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("from_date", fromDate);
		row.put("to_date", toDate);
		row.put("shift_id", shiftId);
		row.put("shift_request_id", shiftRequestId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private void sendEmailOnNew(List<String> employeeEmailIdList, Record record) throws Exception {

		Object shiftName = record.getValue("shift_id.name");
		Object fromDate = record.getValue("from_date");
		Object toDate = record.getValue("to_date");
		Object requestDate = record.getValue("request_date");
		Object reason = record.getValue("reason");
		Object employeeEmail = record.getValue("employeeid.officialemailid");
		Object employeeName = record.getValue("employeeid.name");
		Object employeeCode = record.getValue("employeeid.employeecode");
		Object approverEmail = record.getValue("approverid.officialemailid");
		Object approverName = record.getValue("approverid.name");

		if (employeeEmail != null && employeeEmail.toString().length() > 0) {
			employeeEmailIdList.add(employeeEmail.toString());
		}

		String shiftRequestOffDays = HrisUserDefinedFunctions.getShiftRequestOffDays(record);

		String[] emailsInCC = new String[employeeEmailIdList.size()];
		for (int counter = 0; counter < employeeEmailIdList.size(); counter++) {
			emailsInCC[counter] = employeeEmailIdList.get(counter);
		}

		String title = "Shift Change Request By " + employeeName + "(" + employeeCode + ") on " + requestDate;

		StringBuffer message = new StringBuffer();
		message.append("Dear ").append(approverName).append("<BR /><BR />");
		message.append(employeeName).append(" has requested to change his/her shift to ").append(shiftName).append("<BR /><BR />");
		message.append("<b>Shift Name:- </b>").append(shiftName).append("<BR />");
		message.append("<b>From Date:- </b>").append(fromDate).append("<BR />");
		message.append("<b>To Date:- </b>").append(toDate).append("<BR />");
		message.append("<b>Reason:- </b>").append(reason).append("<BR />");
		message.append("<b>Off Days:- </b>").append(shiftRequestOffDays).append("<BR /><BR />");
		message.append("Regards").append("<BR />");
		message.append(employeeName);

		PunchingUtility.sendMailsAccToProfitCenters(new String[] { approverEmail.toString() }, message.toString(), title, emailsInCC);
	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			Object fromDate = record.getValue("from_date");
			Object toDate = record.getValue("to_date");
			if (Translator.dateValue(toDate).before(Translator.dateValue(fromDate))) {
				throw new BusinessLogicException("From Date should be greater or equal to To Date.");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

}
