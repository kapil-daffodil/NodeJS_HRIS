package com.applane.resourceoriented.hris.dashboard;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.app.shared.constants.View;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.leave.LeaveRequestUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeDashboardQueryBusinessLogic implements QueryJob {

	@Override
	public void onQuery(com.applane.databaseengine.resource.Query arg0) throws DeadlineExceededException {
	};

	@Override
	public void onResult(Result result) throws DeadlineExceededException {
		JSONArray records = result.getRecords();
		if (records != null && records.length() > 0) {
			Object _CurrentAppGroup = CurrentState.getCurrentVariable(CurrentSession.CURRENT_APPGROUP);
			Object _CurrentAppGroupId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_APPGROUP_ID);
			Object _CurrentApplication = CurrentState.getCurrentVariable(CurrentSession.CURRENT_APPLICATION);
			Object _CurrentApplicationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID);
			Object _CurrentOrganization = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION);
			Object _CurrentOrganizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
			Object _CurrentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
			Object _CurrentUserId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_ID);
			try {

				JSONObject loadViewActions = new JSONObject();
				JSONObject view = new JSONObject();
				view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
				// view.put(View.PRIMARY_EXPRESSION, "accountname");
				view.put(View.Action.Types.LoadView.VIEWID, "leaverequest_viewdetail_portal");
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "Enter Leave Request");
				view.put("max_rows", 0);
				loadViewActions.put("leaverequest_viewdetail_portal", view);

				view = new JSONObject();
				view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
				// view.put(View.PRIMARY_EXPRESSION, "accountname");
				view.put(View.Action.Types.LoadView.VIEWID, "shortleaves_panelview_portal_edtiable");
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "Enter Short Leave Request");
				view.put("max_rows", 0);
				loadViewActions.put("shortleaves_panelview_portal_edtiable", view);

				view = new JSONObject();
				view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
				// view.put(View.PRIMARY_EXPRESSION, "accountname");
				view.put(View.Action.Types.LoadView.VIEWID, "newtourrequest_panelview_portal");
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "Enter Tour Request (Domestic)");
				view.put("max_rows", 0);
				loadViewActions.put("newtourrequest_panelview_portal", view);

				view = new JSONObject();
				view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
				// view.put(View.PRIMARY_EXPRESSION, "accountname");
				view.put(View.Action.Types.LoadView.VIEWID, "newtourrequest_panelview_portal_foreign");
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "Enter Tour Request (Foreign)");
				view.put("max_rows", 0);
				loadViewActions.put("newtourrequest_panelview_portal_foreign", view);

				view = new JSONObject();
				view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
				// view.put(View.PRIMARY_EXPRESSION, "accountname");
				view.put(View.Action.Types.LoadView.VIEWID, "request_form_panel");
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "Enter Manpower Requisition Request");
				view.put("max_rows", 0);
				loadViewActions.put("request_form_panel", view);

				view = new JSONObject();
				view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
				// view.put(View.PRIMARY_EXPRESSION, "accountname");
				view.put(View.Action.Types.LoadView.VIEWID, "employee_feedback_employee_details_portal");
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "Enter Feedback On Employee");
				view.put("max_rows", 0);
				loadViewActions.put("employee_feedback_employee_details_portal", view);

				view = new JSONObject();
				view.put(View.Action.TYPE, View.Action.Types.LOADVIEW);
				// view.put(View.PRIMARY_EXPRESSION, "accountname");
				view.put(View.Action.Types.LoadView.VIEWID, "employee_feedback_company_policy_details_portal");
				view.put(View.Action.Types.LoadView.VIEW_LABEL, "Enter Feedback On Policy");
				view.put("max_rows", 0);
				loadViewActions.put("employee_feedback_company_policy_details_portal", view);

				result.addResultInfo("loadviews", loadViewActions);

				String addLeaveRequest = new StringBuffer().append("<a class=\"loadview\" actionid=\"leaverequest_viewdetail_portal\">").append("Enter Leave Request").append("</a>").toString();
				String addShortLeaveRequest = new StringBuffer().append("<a class=\"loadview\" actionid=\"shortleaves_panelview_portal_edtiable\">").append("Enter Short Leave Request").append("</a>").toString();
				String addTourDomesticRequest = new StringBuffer().append("<a class=\"loadview\" actionid=\"newtourrequest_panelview_portal\">").append("Enter Tour Request (Domestic)").append("</a>").toString();
				String addTourForeignRequest = new StringBuffer().append("<a class=\"loadview\" actionid=\"newtourrequest_panelview_portal_foreign\">").append("Enter Tour Request (Foreign)").append("</a>").toString();
				String addMRFRequest = new StringBuffer().append("<a class=\"loadview\" actionid=\"request_form_panel\">").append("Enter Manpower Requisition Request").append("</a>").toString();
				String addFeedbackOnEmployee = new StringBuffer().append("<a class=\"loadview\" actionid=\"employee_feedback_employee_details_portal\">").append("Enter Feedback On Employee").append("</a>").toString();
				String addFeedbackOnCompanyPolicy = new StringBuffer().append("<a class=\"loadview\" actionid=\"employee_feedback_company_policy_details_portal\">").append("Enter Feedback On Policy").append("</a>").toString();

				String addLeaveRequest_Link = "<a href=\"/app?app=" + _CurrentApplication + "&amp;appgroup=" + _CurrentAppGroup + "&amp;appgroupid=" + _CurrentAppGroupId + "&amp;appid=" + _CurrentApplicationId + "&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'leaverequest_viewdetail_portal','source':'insert','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Enter Leave Request  </a>";
				String addShortLeaveRequest_Link = "<a href=\"/app?app=" + _CurrentApplication + "&amp;appgroup=" + _CurrentAppGroup + "&amp;appgroupid=" + _CurrentAppGroupId + "&amp;appid=" + _CurrentApplicationId + "&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'shortleaves_panelview_portal_edtiable','source':'newleave1','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Enter Short Leave Request  </a>";
				String addTourDomesticRequest_Link = "<a href=\"/app?app=" + _CurrentApplication + "&amp;appgroup=" + _CurrentAppGroup + "&amp;appgroupid=" + _CurrentAppGroupId + "&amp;appid=" + _CurrentApplicationId + "&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'newtourrequest_panelview_portal','source':'newtourrequest_portal','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Enter Tour Request (Domestic)  </a>";
				String addTourForeignRequest_Link = "<a href=\"/app?app=" + _CurrentApplication + "&amp;appgroup=" + _CurrentAppGroup + "&amp;appgroupid=" + _CurrentAppGroupId + "&amp;appid=" + _CurrentApplicationId + "&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'newtourrequest_panelview_portal_foreign','source':'new_tour_request_foreign','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Enter Tour Request (Foreign)  </a>";
				String addMRFRequest_Link = "<a href=\"/app?app=" + _CurrentApplication + "&amp;appgroup=" + _CurrentAppGroup + "&amp;appgroupid=" + _CurrentAppGroupId + "&amp;appid=" + _CurrentApplicationId + "&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'request_form_panel','source':'request_form_panel_new','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Enter Manpower Requisition Request  </a>";
				String addFeedbackOnEmployee_Link = "<a href=\"/app?app=" + _CurrentApplication + "&amp;appgroup=" + _CurrentAppGroup + "&amp;appgroupid=" + _CurrentAppGroupId + "&amp;appid=" + _CurrentApplicationId + "&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'employee_feedback_employee_details_portal','source':'employee_feedback_employee_details_portal','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Enter Feedback On Employee  </a>";
				String addFeedbackOnCompanyPolicy_Link = "<a href=\"/app?app=" + _CurrentApplication + "&amp;appgroup=" + _CurrentAppGroup + "&amp;appgroupid=" + _CurrentAppGroupId + "&amp;appid=" + _CurrentApplicationId + "&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'employee_feedback_company_policy_details_portal','source':'employee_feedback_company_policy_details_portal','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Enter Feedback On Policy  </a>";
				String taskManager_Link = "<a href=\"/app?app=task_manager&amp;appgroup=task_manager&amp;appgroupid=1189&amp;appid=1151&amp;org=" + _CurrentOrganization + "&amp;orgid=" + _CurrentOrganizationId + "&amp;user=" + _CurrentUserEmail + "&amp;userid=" + _CurrentUserId + "&amp;viewid={'viewid':'tasks_list_','source':'tasks','max_rows':0}&amp;welcomehelp=false\"  target='_blank'> Task Manager  </a>";

				Object key = records.getJSONObject(0).opt("__key__");
				JSONArray documents = getDocumentsArray(key);

				JSONArray lastThreeDatesList = new JSONArray();
				String todayDate = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
				new LeaveRequestUtility().getLeaveTakenInLestThreMonths(key, todayDate, lastThreeDatesList);
				String threeLeaves = "";
				if (lastThreeDatesList != null && lastThreeDatesList.length() > 0) {
					for (int counter = 0; counter < lastThreeDatesList.length(); counter++) {
						if (threeLeaves.length() > 0) {
							threeLeaves += "<BR />";
						}
						threeLeaves += lastThreeDatesList.opt(counter);
					}
				}
				JSONObject object = records.getJSONObject(0);
				if (object.opt("providentfundno") == null) {
					object.put("providentfundno", "N/A");
				}

				JSONObject linkDetails = new JSONObject();

				linkDetails.put("addLeaveRequest", addLeaveRequest);
				linkDetails.put("addShortLeaveRequest", addShortLeaveRequest);
				linkDetails.put("addTourDomesticRequest", addTourDomesticRequest);
				linkDetails.put("addTourForeignRequest", addTourForeignRequest);
				linkDetails.put("addMRFRequest", addMRFRequest);
				linkDetails.put("addFeedbackOnEmployee", addFeedbackOnEmployee);
				linkDetails.put("addFeedbackOnCompanyPolicy", addFeedbackOnCompanyPolicy);

				linkDetails.put("addLeaveRequest_Link", addLeaveRequest_Link);
				linkDetails.put("addShortLeaveRequest_Link", addShortLeaveRequest_Link);
				linkDetails.put("addTourDomesticRequest_Link", addTourDomesticRequest_Link);
				linkDetails.put("addTourForeignRequest_Link", addTourForeignRequest_Link);
				linkDetails.put("addMRFRequest_Link", addMRFRequest_Link);
				linkDetails.put("addFeedbackOnEmployee_Link", addFeedbackOnEmployee_Link);
				linkDetails.put("addFeedbackOnCompanyPolicy_Link", addFeedbackOnCompanyPolicy_Link);
				linkDetails.put("taskManager_Link", taskManager_Link);

				linkDetails.put("employeeDetails", object);
				if (_CurrentOrganizationId != null && Translator.integerValue(_CurrentOrganizationId) == 10719) {
					object.put("balanceleaves", getLeaveBalance(key, object.opt("relievingdate")));
				}
				linkDetails.put("threeLeaves", threeLeaves);
				linkDetails.put("documents", documents);
				linkDetails.put("hris_employeeexperiences", getEmployeeEmployeementArray(key));
				linkDetails.put("hris_employeeeducations", getEmployeeEducationArray(key));
				JSONArray array = new JSONArray();
				array.put(linkDetails);
				records.put(new JSONObject().put("records", array));
				LogUtility.writeError("array >> " + array);
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				LogUtility.writeError("EmployeeDashboardQueryBusinessLogic >> exception trace >> " + trace);
			}
		}
	}

	private Object getLeaveBalance(Object employeeId, Object relievingDate) throws Exception {
		String dateInString = "";
		if (relievingDate != null) {
			dateInString = "" + relievingDate;
		} else {
			dateInString = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(Translator.dateValue(dateInString));
		int monthId = cal.get(Calendar.MONTH) + 1;
		long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
		JSONArray leaveBalanceArray = getLeaveBalanceArray(employeeId, monthId, yearId);
		if (leaveBalanceArray != null && leaveBalanceArray.length() > 0) {
			String leaveBalanceString = "";
			for (int counter = 0; counter < leaveBalanceArray.length(); counter++) {
				Object leaveBalance = leaveBalanceArray.getJSONObject(counter).opt("leavebalance");
				Object leaveName = leaveBalanceArray.getJSONObject(counter).opt("leavetypeid.name_girnarsoft");
				if (leaveBalanceString.length() > 0) {
					leaveBalanceString += "<BR />";
				}
				leaveBalanceString += (leaveName + ": " + leaveBalance);
			}
			return leaveBalanceString;
		}
		return "";
	}

	private JSONArray getLeaveBalanceArray(Object employeeId, int monthId, long yearId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "leaveregister");
		JSONArray array = new JSONArray();
		array.put("leavebalance");
		array.put("leavetypeid.name_girnarsoft");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND monthid = " + monthId + " AND yearid = " + yearId);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray leaveBalanceArray = object.getJSONArray("leaveregister");
		return leaveBalanceArray;
	}

	private Object getEmployeeEducationArray(Object key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employeeeducations");
		JSONArray array = new JSONArray();
		array.put("passingyear");
		array.put("degreeid.degreename");
		array.put("qualificationtypeid.name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + key);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray educationArray = object.getJSONArray("hris_employeeeducations");
		return educationArray;
	}

	private Object getEmployeeEmployeementArray(Object key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employeeexperiences");
		JSONArray array = new JSONArray();
		array.put("companyname");
		array.put("fromdate");
		array.put("todate");
		array.put("designation");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + key);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeementArray = object.getJSONArray("hris_employeeexperiences");
		return employeementArray;
	}

	private JSONArray getDocumentsArray(Object key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employeedocuments");
		JSONArray array = new JSONArray();
		array.put("documentno");
		array.put("documentid.name");
		array.put("issuedate");
		array.put("expirydate");
		array.put("placeofissue");
		array.put("issueauthority");
		array.put("attachdocument");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + key);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray documentArray = object.getJSONArray("hris_employeedocuments");
		for (int counter = 0; counter < documentArray.length(); counter++) {
			JSONObject documentObject = documentArray.getJSONObject(counter);
			Object attachment = documentObject.opt("attachdocument");
			if (attachment != null) {
				LogUtility.writeError("attachment >> " + counter + " << >> " + attachment);
				String attachedString = attachment.toString();
				int lastIndexOfFileName = attachedString.lastIndexOf("\"filename\":\"");
				String fName = attachedString.substring(lastIndexOfFileName + 12);
				fName = fName.substring(0, fName.indexOf("\""));
				LogUtility.writeError("fName >> " + fName);

				int lastIndexOfFileUrl = attachedString.lastIndexOf("\"url\":\"");
				String url = attachedString.substring(lastIndexOfFileUrl + 7);
				url = url.substring(0, url.indexOf("\""));
				LogUtility.writeError("url >> " + url);

				String att = "<a href='" + url + "' target='_blank'>" + fName + "</a>";
				documentObject.put("url", att);
			}
		}
		return documentArray;
	}
}
