package com.applane.resourceoriented.hris.cabdelay;

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
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class CabDelayInformationApproveFromMailServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			approveLeaveByMail(req, resp);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(CabDelayInformationApproveFromMailServlet.class.getName(), e);
			LogUtility.writeLog("trace >> " + trace);
		}
	}

	private void approveLeaveByMail(HttpServletRequest req, HttpServletResponse resp) throws JSONException, Exception {
		Object cabDelayRequestKey = req.getParameter("lr");
		Object organizationName = req.getParameter("orn");
		Object approverId = req.getParameter("apid");
		Object approveReject = req.getParameter("approvereject");
		Object apby = req.getParameter("apby");
		Object comments = req.getParameter("comments");
		LogUtility.writeLog("cabDelayRequestKey >> " + cabDelayRequestKey + " << organizationName >> " + organizationName + " << approverId >> " + approverId + " << approveReject >> " + approveReject + " << apby >> " + apby);
		if (approverId != null) {
			// Object creatorID = PunchingUtility.getId("up_users", "emailid", ApplicationConstants.OWNER);
			Object creatorID = 13652;
			PunchingUtility.setValueInSession("" + organizationName, creatorID, ApplicationConstants.OWNER);
			PrintWriter out = resp.getWriter();

			if (approveReject != null) {
				JSONArray approverEmployeeDetails = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(approverId);
				JSONArray calDelayRequestRecord = getCalDelayRequestRecord(cabDelayRequestKey);

				if (calDelayRequestRecord != null && calDelayRequestRecord.length() > 0) {
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
					int calDelayRequestStatusId = 0;

					if (apby != null && ("" + apby).equals("tl")) {
						calDelayRequestStatusId = Translator.integerValue(calDelayRequestRecord.getJSONObject(0).opt("status_id"));
					} else if (apby != null && ("" + apby).equals("admin")) {
						calDelayRequestStatusId = Translator.integerValue(calDelayRequestRecord.getJSONObject(0).opt("approve_by_admin_status_id"));
						int calDelayRequestTlStatusId = Translator.integerValue(calDelayRequestRecord.getJSONObject(0).opt("status_id"));
						if (calDelayRequestTlStatusId == HRISConstants.LEAVE_NEW) {
							out.println(logoInMail + "Cab Delay Information Not Approved By TL");
							return;
						} else if (calDelayRequestTlStatusId == HRISConstants.LEAVE_REJECTED) {
							out.println(logoInMail + "Cab Delay Information Already Rejected By TL");
							return;
						} else if (calDelayRequestTlStatusId == HRISConstants.LEAVE_CANCEL) {
							out.println(logoInMail + "Cab Delay Information Cancled");
							return;
						}
					}

					if (calDelayRequestStatusId == HRISConstants.LEAVE_APPROVED) {
						out.println(logoInMail + "Cab Delay Information Already Approved.");
					} else if (calDelayRequestStatusId == HRISConstants.LEAVE_REJECTED) {
						out.println(logoInMail + "Cab Delay Information Already Rejected.");
					} else if (calDelayRequestStatusId == HRISConstants.LEAVE_CANCEL) {
						out.println(logoInMail + "Cab Delay Information Cancled.");
					} else if (approverEmployeeDetails != null && approverEmployeeDetails.length() > 0) {
						String officialEmailId = Translator.stringValue(approverEmployeeDetails.getJSONObject(0).opt("officialemailid"));
						Object approverUserId = PunchingUtility.getId("up_users", "emailid", officialEmailId);
						PunchingUtility.setValueInSession("" + organizationName, approverUserId, officialEmailId);
						if (apby != null && ("" + apby).equals("tl")) {
							updateCabDelayRequestByTl(cabDelayRequestKey, approverId, approveReject, comments);
						} else if (apby != null && ("" + apby).equals("admin")) {
							updateCabDelayRequestByAdmin(cabDelayRequestKey, approverId, approveReject, comments);
						}
						if (Translator.integerValue(approveReject) == HRISConstants.LEAVE_APPROVED) {
							out.println(logoInMail + "Cab Delay Information Has Been Approved.");
						} else {
							out.println(logoInMail + "Cab Delay Information Has Been Rejected.");
						}
					}
				} else {
					out.println("This Cab Delay Information may be removed, Please check details.");
				}
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

	private JSONArray getCalDelayRequestRecord(Object leaveRequestKey) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_cab_delay_information");
		columnArray.put(Updates.KEY);
		columnArray.put("status_id");
		columnArray.put("approve_by_admin_status_id");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + leaveRequestKey);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_cab_delay_information");
		return rows;
	}

	private void updateCabDelayRequestByAdmin(Object leaveRequestKey, Object approverId, Object approveReject, Object comments) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_cab_delay_information");
		JSONObject row = new JSONObject();
		row.put("__key__", leaveRequestKey);
		if (comments != null)
			row.put("reason", comments);
		row.put("cab_admin_employee_id", approverId);
		row.put("approve_by_admin_status_id", Translator.integerValue(approveReject));
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private void updateCabDelayRequestByTl(Object leaveRequestKey, Object approverId, Object approveReject, Object comments) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_cab_delay_information");
		JSONObject row = new JSONObject();
		row.put("__key__", leaveRequestKey);
		row.put("reportingto_id", approverId);
		if (comments != null)
			row.put("reason", comments);
		row.put("status_id", Translator.integerValue(approveReject));
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}
}