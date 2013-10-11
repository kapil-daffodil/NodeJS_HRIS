package com.applane.resourceoriented.hris.advance.darcl;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class ApproveRejectAdvanceByMailServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			approveAdvanceByMail(req, resp);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(ApproveRejectAdvanceByMailServlet.class.getName(), e);
			LogUtility.writeLog("trace >> " + trace);
		}
	}

	private void approveAdvanceByMail(HttpServletRequest req, HttpServletResponse resp) throws JSONException, Exception {
		Object advanceKey = req.getParameter("advk");
		Object organizationName = req.getParameter("orn");
		Object approverMail = req.getParameter("apm");
		Object approveBySenior = req.getParameter("abs");
		Object approveByDirector = req.getParameter("abd");

		Object approveAmountBySenior = req.getParameter("ambs");
		Object approveAmountByDirector = req.getParameter("ambd");
		Object remarksForHistory = req.getParameter("rfh");
		Object remarksForHistoryDirector = req.getParameter("rfhd");
		Object lastAdvanceTaken = req.getParameter("lat");
		Object balance = req.getParameter("bal");
		Object dateOfLastAdvanceCleared = req.getParameter("dolac");
		Object directorEmail = req.getParameter("directorEmail");
		Object loneRemarks = req.getParameter("l_remarks");
		Object intrestPercentage = req.getParameter("int_per");

		LogUtility.writeError("advanceKey >> " + advanceKey + " << organizationName >> " + organizationName + " << approverMail >> " + approverMail + " << approveBySenior >> " + approveBySenior + " << approveByDirector >> " + approveByDirector + " << approveAmountBySenior >> " + approveAmountBySenior + " << remarksForHistory >> " + remarksForHistory + " << remarksForHistoryDirector >> " + remarksForHistoryDirector + " << lastAdvanceTaken >> " + lastAdvanceTaken + " << balance >> " + balance + " << dateOfLastAdvanceCleared >> " + dateOfLastAdvanceCleared + " << approvermTest >> " + directorEmail + " << loneRemarks >> " + loneRemarks + " << intrestPercentage >> " + intrestPercentage);

		if (advanceKey != null && approverMail != null) {
			Object creatorID = PunchingUtility.getId("up_users", "emailid", "" + approverMail);
			// if (creatorID == null) {
			// creatorID = 13652;
			// approverMail = ApplicationConstants.OWNER;
			// }
			PunchingUtility.setValueInSession("" + organizationName, creatorID, "" + approverMail);
			PrintWriter out = resp.getWriter();
			try {
				updateAdvanceDetails(advanceKey, approveBySenior, approveByDirector, approveAmountBySenior, approveAmountByDirector, remarksForHistory, remarksForHistoryDirector, lastAdvanceTaken, balance, dateOfLastAdvanceCleared, directorEmail, loneRemarks, intrestPercentage);
			} catch (BusinessLogicException e) {
				out.print(e.getMessage());
				throw e;

			} catch (Exception e) {
				throw e;
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

	private void updateAdvanceDetails(Object advanceKey, Object approveBySenior, Object approveByDirector, Object approveAmountBySenior, Object approveAmountByDirector, Object remarksForHistory, Object remarksForHistoryDirector, Object lastAdvanceTaken, Object balance, Object dateOfLastAdvanceCleared, Object directorEmail, Object loneRemarks, Object intrestPercentage) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceAmount.RESOURCE);
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, advanceKey);
		if (approveBySenior != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR, approveBySenior);
		}
		if (approveByDirector != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR, approveByDirector);
		}
		if (approveAmountBySenior != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR, approveAmountBySenior);
		}
		if (approveAmountByDirector != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR, approveAmountByDirector);
		}
		if (remarksForHistory != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY, remarksForHistory);
		}
		if (remarksForHistoryDirector != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR, remarksForHistoryDirector);
		}
		if (lastAdvanceTaken != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP, lastAdvanceTaken);
		}
		if (balance != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP, balance);
		}
		if (dateOfLastAdvanceCleared != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP, dateOfLastAdvanceCleared);
		}
		if (directorEmail != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.DIRECTOR_EMAIL, directorEmail);
		}
		if (loneRemarks != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS, loneRemarks);
		}
		if (intrestPercentage != null) {
			row.put(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE, intrestPercentage);
		}
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}
}
