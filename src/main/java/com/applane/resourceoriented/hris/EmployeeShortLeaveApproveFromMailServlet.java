package com.applane.resourceoriented.hris;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class EmployeeShortLeaveApproveFromMailServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			approveLeaveByMail(req, resp);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeShortLeaveApproveFromMailServlet.class.getName(), e);
			LogUtility.writeLog("trace >> " + trace);
		}
	}

	private void approveLeaveByMail(HttpServletRequest req, HttpServletResponse resp) throws JSONException, Exception {
		Object leaveRequestKey = req.getParameter("lr");
		Object organizationName = req.getParameter("orn");
		Object approverId = req.getParameter("apid");
		Object approveReject = req.getParameter("approvereject");
		Object unpaid = req.getParameter("unpaid");
		if (approverId != null) {
			Object creatorID = PunchingUtility.getId("up_users", "emailid", ApplicationConstants.OWNER);
			PunchingUtility.setValueInSession("" + organizationName, creatorID, ApplicationConstants.OWNER);
			JSONArray approverEmployeeDetails = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(approverId);

			JSONArray leaveRecord = getLeaveRecord(leaveRequestKey);
			PrintWriter out = resp.getWriter();
			if (leaveRecord != null && leaveRecord.length() > 0) {
				JSONArray mailConfigurationSetup = getMailConfigurationSetup();
				String logo = "";
				if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
					Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
					if (organizationlogo != null) {
						logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
					}
				}
				StringBuffer logoInMail = new StringBuffer();
				logoInMail.append("<img src='").append(logo).append("' alt='Organization Logo' height='50' width='200'").append(" /><BR /><BR /><BR />");
				int leaveStatusId = Translator.integerValue(leaveRecord.getJSONObject(0).opt("leavestatusid"));
				if (leaveStatusId == HRISConstants.LEAVE_APPROVED) {
					out.println(logoInMail + "Leave Request already approved");
				} else if (leaveStatusId == HRISConstants.LEAVE_REJECTED) {
					out.println(logoInMail + "Leave Request already rejected");
				} else if (leaveStatusId == HRISConstants.LEAVE_CANCEL) {
					out.println(logoInMail + "Leave Request cancled");
				} else

				if (approverEmployeeDetails != null && approverEmployeeDetails.length() > 0) {
					String officialEmailId = Translator.stringValue(approverEmployeeDetails.getJSONObject(0).opt("officialemailid"));
					Object approverUserId = PunchingUtility.getId("up_users", "emailid", officialEmailId);
					PunchingUtility.setValueInSession("" + organizationName, approverUserId, officialEmailId);
					updateShortLeaveRequest(leaveRequestKey, approverId, approveReject, unpaid);
					if (Translator.integerValue(approveReject) == HRISConstants.LEAVE_APPROVED) {
						out.println(logoInMail + "Leave Request has been approved");
					} else {
						out.println(logoInMail + "Leave Request has been rejected");
					}
				}
			} else {
				out.println("This Leave Request record may be removed, Please check details.");
			}
		}
	}

	public JSONArray getMailConfigurationSetup() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("organizationlogo");

		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_mailconfigurations");
		return employeeArray;
	}

	private JSONArray getLeaveRecord(Object leaveRequestKey) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_shortleaves");
		columnArray.put(Updates.KEY);
		columnArray.put("leavestatusid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + leaveRequestKey);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_shortleaves");
		return rows;
	}

	private void updateShortLeaveRequest(Object leaveRequestKey, Object approverId, Object approveReject, Object unpaid) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_shortleaves");
		JSONObject row = new JSONObject();
		row.put("__key__", leaveRequestKey);
		row.put("approverid", approverId);
		row.put("leavestatusid", Translator.integerValue(approveReject));
		row.put("paidstatus", unpaid == null ? JSONObject.NULL : Translator.integerValue(unpaid));
		// if (Translator.integerValue(unpaid) == HRISApplicationConstants.LeavePaymentType.UNPAID) {
		// row.put("approvedleaves", 0.50);
		// }
		// row.put("leavestatusid", HRISConstants.LEAVE_APPROVED);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}
}
