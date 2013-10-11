package com.applane.resourceoriented.hris.leave;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HrisUserDefinedFunctions;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class LeaveApplyForTeamBusinessLogic implements OperationJob {

	public void onAfterUpdate(Record records) throws DeadlineExceededException {
		LogUtility.writeError("after records >> " + records.getValues());
		JSONArray employeeArray = (JSONArray) records.getValue("employeeid");
		for (int counter = 0; counter < employeeArray.length(); counter++) {
			JSONObject employeeIdObject = (JSONObject) employeeArray.opt(counter);
			Object employeeId = employeeIdObject.opt("__key__");
			Object name = employeeIdObject.opt("name");
			Object fromdurationtypeid = records.getValue("fromdurationtypeid");
			Object reason = records.getValue("reason");
			Object todurationtypeid = records.getValue("todurationtypeid");
			Object todate = records.getValue("todate");
			Object requestdate = records.getValue("requestdate");
			Object paidstatus = records.getValue("paidstatus");
			Object fromdate = records.getValue("fromdate");
			Object leavetypeid = records.getValue("leavetypeid");
			try {
				Object approverId = getApproverId();
				insertLeaveRecords(employeeId, fromdurationtypeid, reason, todurationtypeid, todate, requestdate, paidstatus, fromdate, leavetypeid, approverId);
			} catch (Exception e) {
				sendMailInCaseOfException(e, ("Error While Appling Leave For [" + name + "]"));
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				LogUtility.writeError("LeaveApplyForTeamBusinessLogic >> exception trace  >> " + trace);
			}
		}

	}

	private void sendMailInCaseOfException(Exception e, String title) {
		try {
			ApplaneMail mail = new ApplaneMail();
			StringBuilder builder = new StringBuilder();
			builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
			builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(e.getMessage());
			mail.setMessage(title, builder.toString(), true);
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Employee Salary Servlet.");
		}
	}

	private Object getApproverId() throws Exception {
		Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "officialemailid = '" + currentUserEmail + "'");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			Object monthId = rows.getJSONObject(0).opt("__key__");
			return monthId;
		}
		return null;
	}

	private void insertLeaveRecords(Object employeeId, Object fromdurationtypeid, Object reason, Object todurationtypeid, Object todate, Object requestdate, Object paidstatus, Object fromdate, Object leavetypeid, Object approverId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_leaverequests");
		JSONObject row = new JSONObject();

		row.put("employeeuserid", new HrisUserDefinedFunctions().getEmployeeUserId(employeeId));
		row.put("leavestatusid", 1);
		row.put("alternateholiday", 2);
		row.put("employeeid", employeeId);
		row.put("fromdurationtypeid", fromdurationtypeid);
		row.put("reason", reason);
		row.put("todurationtypeid", todurationtypeid);
		row.put("todate", todate);
		row.put("requestdate", requestdate);
		row.put("paidstatus", paidstatus);
		row.put("fromdate", fromdate);
		row.put("leavetypeid", leavetypeid);
		row.put("approverid", approverId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public void onBeforeUpdate(Record records) throws VetoException, DeadlineExceededException {
		LogUtility.writeError("records >> " + records.getValues());
	}
}