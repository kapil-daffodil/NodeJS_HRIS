package com.applane.resourceoriented.hris.recruitment;

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
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class ApproveRecruitmentFormByMail extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			approveAdvanceByMail(req, resp);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(ApproveRecruitmentFormByMail.class.getName(), e);
			LogUtility.writeLog("trace >> " + trace);
		}
	}

	private void approveAdvanceByMail(HttpServletRequest req, HttpServletResponse resp) throws JSONException, Exception {
		Object mrfKey = req.getParameter("_mrf_");
		Object organizationName = req.getParameter("orn");
		Object approverMail = req.getParameter("em");
		Object status = req.getParameter("_ap_");
		Object remarks = req.getParameter("rem");
		Object directorEmail = req.getParameter("directorEmail");

		LogUtility.writeError("advanceKey >> " + mrfKey + " << organizationName >> " + organizationName + " << approverMail >> " + approverMail + " << approveBySenior >> " + status + " << approveByDirector >> " + remarks + " << directorEmail >> " + directorEmail);

		if (mrfKey != null && approverMail != null) {
			Object creatorID = PunchingUtility.getId("up_users", "emailid", "" + approverMail);
			// if (creatorID == null) {
			// creatorID = 13652;
			// approverMail = ApplicationConstants.OWNER;
			// }
			PunchingUtility.setValueInSession("" + organizationName, creatorID, "" + approverMail);
			PrintWriter out = resp.getWriter();
			try {
				checkMrfStatusIsCancled(mrfKey);
				updateAdvanceDetails(mrfKey, status, remarks, directorEmail);
			} catch (BusinessLogicException e) {
				out.print(e.getMessage());
				throw e;

			} catch (Exception e) {
				out.print(e.getMessage());
				throw e;
			}
		}
	}

	private void checkMrfStatusIsCancled(Object mrfKey) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfProcess.RESOURCE);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfProcess.STATUS_ID);

		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray mrfArray = employeeObject.getJSONArray(HRISApplicationConstants.RecruitmentMrfProcess.RESOURCE);
		if (mrfArray != null && mrfArray.length() > 0) {
			int statusId = Translator.integerValue(mrfArray.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfProcess.STATUS_ID));
			if (statusId == HRISConstants.LEAVE_CANCEL) {
				throw new BusinessLogicException("This Request Cancled by Requester.");
			}
		} else {
			throw new BusinessLogicException("Record may be removed, Please contact to HR Admin.");
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

	private void updateAdvanceDetails(Object mrfKey, Object status, Object remarks, Object directorEmail) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfProcess.RESOURCE);
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, mrfKey);
		if (status != null) {
			row.put(HRISApplicationConstants.RecruitmentMrfProcess.STATUS_ID, status);
		}
		if (remarks != null) {
			row.put(HRISApplicationConstants.RecruitmentMrfProcess.REMARKS_FOR_HISTORY, remarks);
		}
		if (directorEmail != null) {
			row.put(HRISApplicationConstants.RecruitmentMrfProcess.DIRECTOR_EMAIL, directorEmail);
		}
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}
}
