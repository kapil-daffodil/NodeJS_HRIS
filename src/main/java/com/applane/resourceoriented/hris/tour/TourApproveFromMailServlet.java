package com.applane.resourceoriented.hris.tour;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

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
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class TourApproveFromMailServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			approveTourByMail(req, resp);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(TourApproveFromMailServlet.class.getName(), e);
			LogUtility.writeLog("trace >> " + trace);
		}
	}

	private void approveTourByMail(HttpServletRequest req, HttpServletResponse resp) throws JSONException, Exception {
		Object tourRequestKey = req.getParameter("lr");
		Object organizationName = req.getParameter("orn");
		Object approverId = req.getParameter("apid");
		Object approveReject = req.getParameter("approvereject");
		Object query = req.getParameter("query");
		if (approverId != null) {
			// Object creatorID = PunchingUtility.getId("up_users", "emailid", ApplicationConstants.OWNER);
			Object creatorID = 13652;
			PunchingUtility.setValueInSession("" + organizationName, creatorID, ApplicationConstants.OWNER);
			JSONArray approverEmployeeDetails = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(approverId);
			JSONArray tourRecord = getTourRecord(tourRequestKey);
			PrintWriter out = resp.getWriter();
			if (tourRecord != null && tourRecord.length() > 0) {
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
				int tourStatusId = Translator.integerValue(tourRecord.getJSONObject(0).opt("tourstatusid"));
				if (tourStatusId == HRISConstants.LEAVE_APPROVED) {
					out.println(logoInMail + "Tour Request already approved");
				} else if (tourStatusId == HRISConstants.LEAVE_REJECTED) {
					out.println(logoInMail + "Tour Request already rejected");
				} else if (tourStatusId == HRISConstants.LEAVE_CANCEL) {
					out.println(logoInMail + "Tour Request cancled");
				} else if (approverEmployeeDetails != null && approverEmployeeDetails.length() > 0) {
					String officialEmailId = Translator.stringValue(approverEmployeeDetails.getJSONObject(0).opt("officialemailid"));
					Object approverUserId = PunchingUtility.getId("up_users", "emailid", officialEmailId);
					PunchingUtility.setValueInSession("" + organizationName, approverUserId, officialEmailId);
					if (query != null && (query + "").length() > 0) {
						String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
						insertTourQuery(tourRequestKey, approverId, query, TODAY_DATE);
						out.println("Query successfully sent.");
					} else {
						updateTourRequest(tourRequestKey, approverId, approveReject);
						if (Translator.integerValue(approveReject) == HRISConstants.LEAVE_APPROVED) {
							out.println(logoInMail + "Tour Request has been approved");
						} else {
							out.println(logoInMail + "Tour Request has been rejected");
						}
					}
				}
			} else {
				out.println("This Tour Request may be removed, Please check details.");
			}
		}
	}

	private void insertTourQuery(Object tourRequestKey, Object approverId, Object query, String todayDate) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.TourBeforeApprovalComments.RESOURCE);
		JSONObject row = new JSONObject();
		row.put(HRISApplicationConstants.TourBeforeApprovalComments.TOUR_ID, tourRequestKey);
		row.put(HRISApplicationConstants.TourBeforeApprovalComments.EMPLOYEE_ID, approverId);
		row.put(HRISApplicationConstants.TourBeforeApprovalComments.REMARKS, query);
		row.put(HRISApplicationConstants.TourBeforeApprovalComments.DATE, todayDate);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
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

	private JSONArray getTourRecord(Object tourRequestKey) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_tourrequests");
		columnArray.put(Updates.KEY);
		columnArray.put("tourstatusid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + tourRequestKey);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_tourrequests");
		return rows;
	}

	private void updateTourRequest(Object tourRequestKey, Object approverId, Object approveReject) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_tourrequests");
		JSONObject row = new JSONObject();
		row.put("__key__", tourRequestKey);
		row.put("approverid", approverId);
		row.put("tourstatusid", Translator.integerValue(approveReject));
		// row.put("leavestatusid", HRISConstants.LEAVE_APPROVED);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}
}
