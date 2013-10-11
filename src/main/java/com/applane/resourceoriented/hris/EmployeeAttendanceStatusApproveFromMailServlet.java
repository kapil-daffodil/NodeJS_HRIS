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
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class EmployeeAttendanceStatusApproveFromMailServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			updateAttendanceStatusByMail(req, resp);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeAttendanceStatusApproveFromMailServlet.class.getName(), e);
			LogUtility.writeLog("trace >> " + trace);
		}
	}

	private void updateAttendanceStatusByMail(HttpServletRequest req, HttpServletResponse resp) throws JSONException, Exception {

		Object employeeAttendanceKey = req.getParameter(HRISConstants.ApproveAttendanceStatus.FormParameterNames.KEY);
		Object organizationName = req.getParameter(HRISConstants.ApproveAttendanceStatus.FormParameterNames.ORGANIZATION);
		Object approverOfficialEmailIdObj = req.getParameter(HRISConstants.ApproveAttendanceStatus.FormParameterNames.APPROVER_OFFICIAL_EMAIL_ID);
		Object approveRejectStatus = req.getParameter(HRISConstants.ApproveAttendanceStatus.FormParameterNames.STATUS);

		if (approveRejectStatus != null) {

			String approverOfficialEmailId = Translator.stringValue(approverOfficialEmailIdObj);
			Object approverUserId = PunchingUtility.getId("up_users", "emailid", approverOfficialEmailId);
			PunchingUtility.setValueInSession("" + organizationName, approverUserId, approverOfficialEmailId);

			updateAttendanceStatus(employeeAttendanceKey, approveRejectStatus);
		}

		PrintWriter out = resp.getWriter();

		JSONArray mailConfigurationSetup = getMailConfigurationSetup();
		String logo = "";
		if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
			Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
			if (organizationlogo != null) {
				logo = new EmployeeSalarySheetReport().getLogo(organizationlogo);
			}
		}
		StringBuilder logoImage = new StringBuilder();
		logoImage.append("<img src='").append(logo).append("' alt='Organization Logo' height='50' width='200'").append(" /><br /><br /><br />");

		if (Translator.integerValue(approveRejectStatus) == HRISConstants.ApproveAttendanceStatus.STATUS_APPROVED) {
			out.println(logoImage + "Attendance Status has been approved.");
		} else {
			out.println(logoImage + "Attendance Status has been rejected.");
		}

		out.close();
		out.flush();
	}

	private void updateAttendanceStatus(Object employeeAttendanceKey, Object approveRejectStatus) throws Exception {

		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEE_ATTENDANCE);

		JSONObject row = new JSONObject();
		row.put("__key__", employeeAttendanceKey);
		row.put(HrisKinds.EmployeeAttendance.ATTENDANCE_STATUS, Translator.integerValue(approveRejectStatus));

		updates.put(Data.Update.UPDATES, row);

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private JSONArray getMailConfigurationSetup() throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(Updates.KEY);
		columns.put("organizationlogo");

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, columns);

		JSONObject employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = employeeObject.getJSONArray(HrisKinds.MAIL_CONFIGURATIONS);

		return employeeArray;
	}
}
