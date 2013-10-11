package com.applane.afb.user;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneCache;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.ScriptParser;
import com.applane.moduleimpl.SystemParameters;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.cachehandler.UserCacheHandler;
import com.applane.user.exception.UserNotFoundException;

public class EmployeeCacheHandler {

	private static final String EMPLOYEE_DATA = "employeedata";
	public static final String IMMEDIATE_TEAM = "ImmediateTeam";
	public static final String EMPLOYEE_TREE = "employeetree";
	public static final String BUSINESS_FUNCTIONS_HIERARCHY = "businessFunctionsHierarchy";

	public static void getOrganizationEmployeesTree(Object employeeId) throws JSONException, ApplaneMailException {
		ApplaneMail mail = new ApplaneMail();
		mail.setMessage(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION) + " Employee Tree", getEmployeeTreeHtml(null, getEmployeeTree(employeeId, null, true), false, null), true);
		mail.setTo((String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL));
		mail.sendAsAdmin();
	}

	public static JSONObject getEmployeeTree(Object employeeId, Map<Object, Object> dataToPopulate, boolean retreiveFullTree) throws JSONException {
		JSONObject employee = getEmployeeWithImmediateTeam(employeeId);
		if (dataToPopulate != null && dataToPopulate.containsKey(employeeId)) {
			employee.put(EMPLOYEE_DATA, dataToPopulate.get(employeeId));
		}
		Object immediateTeam = employee.remove(IMMEDIATE_TEAM);
		if (immediateTeam != null && immediateTeam instanceof JSONArray) {
			employee.put(EMPLOYEE_TREE, getEmployeeTeamTree((JSONArray) immediateTeam, dataToPopulate, retreiveFullTree));
		}
		return employee;
	}

	public static JSONObject getReverseEmployeeTree(Object employeeId, Map<Object, Object> dataToPopulate) throws JSONException {
		JSONObject employeeReportingTo = getEmployeeRreportingTo(employeeId, new JSONArray().put(HrisKinds.Employess.REPORTING_TO).put(HrisKinds.Employess.NAME));
		if (dataToPopulate != null && dataToPopulate.containsKey(employeeId)) {
			employeeReportingTo.put(EMPLOYEE_DATA, dataToPopulate.get(employeeId));
		}
		Object reportingTo = employeeReportingTo.opt(HrisKinds.Employess.REPORTING_TO);
		if (reportingTo != null) {
			employeeReportingTo.put(HrisKinds.Employess.REPORTING_TO, getReverseEmployeeTree(reportingTo, dataToPopulate));
		}
		return employeeReportingTo;
	}

	public static String getEmployeeTreeHtml(String summaryHeader, JSONObject row, boolean detailedView, JSONObject xslt) throws JSONException {
		String label = detailedView ? "Details:" : "Summary:";
		StringBuilder html = new StringBuilder()
				.append("<html><head><script language=\"javascript\" type=\"text/javascript\"> function addEvents() {activateTree(document.getElementById(\"LinkedList1\"));activateTree(document.getElementById(\"LinkedList2\"));}function activateTree(oList) {for (var i=0; i <oList.getElementsByTagName(\"ul\").length; i++) {oList.getElementsByTagName(\"ul\")[i].style.display=\"none\";}if (oList.addEventListener) {oList.addEventListener(\"click\", toggleBranch,false);} else if (oList.attachEvent) { oList.attachEvent(\"onclick\", toggleBranch); }       addLinksToBranches(oList);}function toggleBranch(event) {var oBranch,cSubBranches;if (event.target) {oBranch = event.target;} else if (event.srcElement) {  oBranch = event.srcElement;}cSubBranches = oBranch.getElementsByTagName(\"ul\"); if (cSubBranches.length > 0) { if (cSubBranches[0].style.display == \"block\") { cSubBranches[0].style.display = \"none\";} else {cSubBranches[0].style.display = \"block\";}}}function addLinksToBranches(oList) {var cBranches =oList.getElementsByTagName(\"li\"); var i, n, cSubBranches;if (cBranches.length > 0) {for (i=0, n =cBranches.length; i < n; i++) {cSubBranches = cBranches[i].getElementsByTagName(\"ul\");if (cSubBranches.length > 0) {addLinksToBranches(cSubBranches[0]);cBranches[i].className =\"HandCursorStyle\";cBranches[i].style.color = \"blue\";cSubBranches[0].style.color = \"black\";           cSubBranches[0].style.cursor = \"auto\";}}}}</script> <style onload=\"addEvents()\">@page{size: 12.0in 14.0in;} ul.LinkedList { display: block; } ul{ display: block; }.HandCursorStyle { cursor: pointer; cursor: hand; } </style></head>");
		html.append("<table width=\"100%\" style=\"height:100%;border-collapse:collapse;\"><tr  style=\"height:3%;border-collapse:collapse;\"><td width=\"1%\">&#160;</td><td style=\"color:white;font-size:20px;\" bgcolor=\"#6699EE\"><b>" + label + "</b></td><td width=\"1%\">&#160;</td><tr  style=\"height:89%;border-collapse:collapse;\"><td width=\"1%\">&#160;</td><td width=\"100%\" bgcolor=\"#B4DAFF\" style=\"height:100%;border-top:1px solid black;border-bottom:1px solid black;border-collapse:collapse;\">");
		if (summaryHeader != null && summaryHeader.trim().length() > 0) {
			html.append("<div width=\"100%\"><b>").append(summaryHeader.replaceAll("&", "&amp;")).append("</b></div>");
		}
		html.append(detailedView ? "<div style=\"height: 100%;\"><ul id=\"LinkedList2\" \" class=\"LinkedList\">&#160;" : "<div style=\"height: 100%;\"><ul id=\"LinkedList1\" \" class=\"LinkedList\">&#160;");
		String name = row.optString("name", "").replaceAll("&", "&amp;");
		JSONObject employeeData = row.optJSONObject(EMPLOYEE_DATA);
		if (employeeData != null) {
			String summary = employeeData.optString("summary", "").replaceAll("&", "&amp;");
			JSONObject detailObject = employeeData.optJSONObject("detail");
			html.append("<li><div width=\"100%\"><b>").append(name).append("</b> - ").append(summary).append("</div>");
			if (detailObject != null && detailedView) {
				String resolveScript = "Problem In Rendering of Data";
				if (xslt != null && xslt instanceof JSONObject) {
					resolveScript = ScriptParser.resolveScript(xslt, detailObject);
				}
				html.append("<ul><li>").append(resolveScript).append("</li></ul><br/>");
			}
		} else {
			html.append("<li><div width=\"100%\"><b>").append(name).append("</b>").append("</div>");
		}
		getDivHTML(row, html, true, detailedView, xslt);
		html.append("</li></ul></div></td></tr></table></html>");
		return html.toString();
	}

	public static Object getReportingToOfEmployee(Object employeeId) throws JSONException {
		JSONObject result = getEmployeeRreportingTo(employeeId, new JSONArray().put(HrisKinds.Employess.REPORTING_TO));
		if (result == null) {
			throw new BusinessLogicException("No employee found corresponding to employeeId [" + employeeId + "]");
		}
		return result.opt(HrisKinds.Employess.REPORTING_TO);
	}

	private static JSONObject getEmployeeRreportingTo(Object employeeId, JSONArray columns) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.FILTERS, Data.Query.KEY + " ={employee__id_}");
		query.put(Data.Query.PARAMETERS, new JSONObject().put("employee__id_", employeeId));
		if (employeeId instanceof String) {
			query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + " ={employee__id_}");
		} else if (employeeId instanceof JSONObject) {
			query.put(Data.Query.FILTERS, Data.Query.KEY + " ={employee__id_}");
			query.put(Data.Query.PARAMETERS, new JSONObject().put("employee__id_", ((JSONObject) employeeId).get(Data.Query.KEY)));
		}
		query.put(Data.Query.COLUMNS, columns);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES).optJSONObject(0);
	}

	public static JSONObject getEmployee(String emailId) throws JSONException {
		String key = getCacheKey(emailId);
		JSONObject employee = ApplaneCache.getJSONObject(key);
		if (employee == null) {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
			query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + " = '" + emailId + "'");
			query.put(Data.Query.COLUMNS, getRequiredColumns());

			JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
			if (result.length() == 0) {
				throw new BusinessLogicException("No employee found corresponding to userid [" + emailId + "]");
			}
			employee = result.getJSONObject(0);
			Set<Object> teamMembers = getTeamMembers(employee, null);
			JSONArray businessFunctions = employee.optJSONArray(HrisKinds.Employess.BUSINESS_FUNCTION_ID);
			if (businessFunctions != null) {
				JSONArray allBusinessFunctions = populateAllBusinessFunctions(businessFunctions);
				employee.put(BUSINESS_FUNCTIONS_HIERARCHY, allBusinessFunctions);
			}
			employee.put(SystemParameters.CURRENT_ACCESS_USERS, new JSONArray(teamMembers));
			ApplaneCache.putJSONObject(getCacheKey(employee.get(Data.Query.KEY)), employee);
			ApplaneCache.putJSONObject(key, employee);
		}
		return employee;
	}

	public static JSONObject getEmployee(Object employeeId) throws JSONException {
		if (employeeId instanceof String) {
			return getEmployee((String) employeeId);
		}
		String key = getCacheKey(employeeId);
		JSONObject employee = ApplaneCache.getJSONObject(key);
		if (employee == null) {
			JSONObject parameter = new JSONObject();
			parameter.put("employee__id_", employeeId);

			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
			query.put(Data.Query.FILTERS, Data.Query.KEY + " ={employee__id_}");
			query.put(Data.Query.COLUMNS, getRequiredColumns());
			query.put(Data.Query.PARAMETERS, parameter);

			JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
			if (result.length() == 0) {
				throw new BusinessLogicException("No employee found corresponding to employeeId [" + employeeId + "]");
			}
			employee = result.getJSONObject(0);
			Set<Object> teamMembers = getTeamMembers(employee, null);
			JSONArray businessFunctions = employee.optJSONArray(HrisKinds.Employess.BUSINESS_FUNCTION_ID);
			if (businessFunctions != null) {
				JSONArray allBusinessFunctions = populateAllBusinessFunctions(businessFunctions);
				employee.put(BUSINESS_FUNCTIONS_HIERARCHY, allBusinessFunctions);
			}
			employee.put(SystemParameters.CURRENT_ACCESS_USERS, new JSONArray(teamMembers));
			ApplaneCache.putJSONObject(getCacheKey(employee.get(HrisKinds.Employess.OFFICIAL_EMAIL_ID)), employee);
			ApplaneCache.putJSONObject(key, employee);
		}
		return employee;
	}

	public static JSONArray getUserTeamMember(Long userid) throws UserNotFoundException, JSONException {
		JSONObject employeeFromUserId = getEmployeeFromUserId(userid);
		return employeeFromUserId.getJSONArray(SystemParameters.CURRENT_ACCESS_USERS);
	}

	public static JSONArray getEmployeesTeamMember(String emailid) throws JSONException {
		return getEmployee(emailid).getJSONArray(SystemParameters.CURRENT_ACCESS_USERS);
	}

	public static JSONObject getEmployeeFromUserId(Long userId) throws UserNotFoundException, JSONException {
		String emailId = UserCacheHandler.getEmailId(userId);
		return getEmployee(emailId);
	}

	public static void clearCache(String emailid) {
		String cacheKey = getCacheKey(emailid);
		ApplaneCache.remove(cacheKey);
	}

	/*
	 * ----------------------------------------Private Methods---------------------------------------
	 */

	/**
	 * 
	 * @param emailId
	 * @return
	 */
	private static String getCacheKey(Object key) {
		Object organizationid = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
		return "employeee__hris_of__" + organizationid + "__" + key + "__";
	}

	private static JSONArray getEmployeeTeamTree(JSONArray immediateTeam, Map<Object, Object> dataToPopulate, boolean retreiveFullTree) throws JSONException {
		JSONArray childEmployees = new JSONArray();
		int length = immediateTeam.length();
		for (int i = 0; i < length; i++) {
			Object employeeId = immediateTeam.get(i);
			JSONObject employeeTree = getEmployeeTree(employeeId, dataToPopulate, retreiveFullTree);
			if ((dataToPopulate != null && dataToPopulate.containsKey(employeeId)) || retreiveFullTree || (employeeTree.optJSONArray(EMPLOYEE_TREE) != null && employeeTree.getJSONArray(EMPLOYEE_TREE).length() > 0)) {
				childEmployees.put(employeeTree);
			}
		}
		return childEmployees;
	}

	private static JSONObject getEmployeeWithImmediateTeam(Object employeeId) throws JSONException {
		// String cacheKey = "employeeeteam__hris_of__" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "__" + employeeId;
		// JSONObject employee = ApplaneCache.getJSONObject(cacheKey);
		// if (employee == null) {
		JSONObject parameter = new JSONObject();
		parameter.put("employee__id_", employeeId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.FILTERS, Data.Query.KEY + " ={employee__id_}");
		query.put(Data.Query.COLUMNS, new JSONArray().put(HrisKinds.Employess.NAME));
		query.put(Data.Query.PARAMETERS, parameter);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		if (result.length() == 0) {
			throw new BusinessLogicException("No employee found corresponding to employeeId [" + employeeId + "]");
		}
		JSONObject employee = result.getJSONObject(0);
		Set<Object> teamMembers = getImmediateTeamMembers(employeeId, true);
		if (teamMembers.size() > 0) {
			employee.put(IMMEDIATE_TEAM, new JSONArray(teamMembers));
		}
		// ApplaneCache.putJSONObject(cacheKey, employee);
		// }
		return employee;
	}

	private static Set<Object> getTeamMembers(Object employeeInfo, Set<Object> avoid) throws JSONException {
		Object employeeId = employeeInfo;
		if (employeeInfo instanceof JSONObject) {
			employeeId = ((JSONObject) employeeInfo).get(Data.Query.KEY);
		}
		Set<Object> teamMembers = getImmediateTeamMembers(employeeId, false);
		if (employeeInfo instanceof JSONObject) {
			((JSONObject) employeeInfo).put(IMMEDIATE_TEAM, new JSONArray(teamMembers));
		}

		Set<Object> employeeToAvoid = new HashSet<Object>(teamMembers);
		employeeToAvoid.add(employeeId);
		if (avoid != null) {
			employeeToAvoid.addAll(avoid);
			teamMembers.removeAll(avoid);
		}

		Set<Object> team = new HashSet<Object>();
		for (Object member : teamMembers) {
			Set<Object> childTeam = getTeamMembers(member, employeeToAvoid);
			team.addAll(childTeam);
		}
		team.add(employeeId);
		return team;
	}

	/**
	 * To Fetch all child Business Functions
	 * 
	 * @param businessFunction
	 * @return
	 * @throws JSONException
	 */
	private static JSONArray populateAllBusinessFunctions(JSONArray businessFunction) throws JSONException {
		if (businessFunction.length() == 0) {
			businessFunction.put(-1);
		}
		Set<Object> children = new HashSet<Object>();
		Set<Object> childBusinessFunctions = getChildBusinessFunction(businessFunction);
		while (childBusinessFunctions.size() > 0) {
			children.addAll(childBusinessFunctions);
			childBusinessFunctions = getChildBusinessFunction(new JSONArray(childBusinessFunctions));
		}
		JSONArray fullHirarchy = new JSONArray();
		for (int i = 0; i < businessFunction.length(); i++) {
			fullHirarchy.put(businessFunction.get(i));
		}
		if (children.size() > 0) {
			for (Object object : children) {
				fullHirarchy.put(object);
			}
		}
		return fullHirarchy;
	}

	private static Set<Object> getChildBusinessFunction(JSONArray businessFunction) throws JSONException {
		JSONObject parameters = new JSONObject();
		parameters.put("parent__bsn_fcn", businessFunction);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.BUSINESS_FUNCTIONS);
		query.put(Data.Query.COLUMNS, new JSONArray().put(Data.Query.KEY));
		query.put(Data.Query.FILTERS, HrisKinds.BusinessFunction.PARENT_BUSINESSFUNCTION + " IN{parent__bsn_fcn}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.BUSINESS_FUNCTIONS);
		int length = result.length();
		Set<Object> child = new HashSet<Object>();
		for (int i = 0; i < length; i++) {
			child.add(result.getJSONObject(i).get(Data.Query.KEY));
		}
		return child;

	}

	private static Set<Object> getImmediateTeamMembers(Object employeeId, boolean onlyActive) throws JSONException {
		JSONObject parameters = new JSONObject();
		parameters.put("emopl__id", employeeId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.COLUMNS, new JSONArray().put(Data.Query.KEY));
		String filter = HrisKinds.Employess.REPORTING_TO + "={emopl__id}" + (onlyActive ? " AND employeestatusid=1" : "");
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		Set<Object> teamSet = new HashSet<Object>();
		int length = result.length();
		for (int i = 0; i < length; i++) {
			JSONObject row = result.getJSONObject(i);
			teamSet.add(row.get(Data.Query.KEY));
		}
		return teamSet;
	}

	private static JSONArray getRequiredColumns() {
		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		columns.put(HrisKinds.Employess.BUSINESS_FUNCTION_ID);
		return columns;
	}

	private static void getDivHTML(JSONObject row, StringBuilder html, boolean isRoot, boolean detailedView, JSONObject xslt) throws JSONException {
		String name = row.optString("name", "").replaceAll("&", "&amp;");
		JSONObject employeeData = row.optJSONObject(EMPLOYEE_DATA);
		if (!isRoot && employeeData != null) {
			String summary = employeeData.optString("summary", "").replaceAll("&", "&amp;");
			JSONObject detailObject = employeeData.optJSONObject("detail");
			html.append("<li><div width=\"100%\"><b>").append(name).append("</b> - ").append(summary).append("</div>");
			if (detailObject != null && detailedView) {
				String resolveScript = "Problem In Rendering of Data";
				if (xslt != null && xslt instanceof JSONObject) {
					resolveScript = ScriptParser.resolveScript(xslt, detailObject);
				}
				html.append("<ul><li>").append(resolveScript).append("</li></ul><br/>");
			}
		} else if (!isRoot) {
			html.append("<li><div width=\"100%\"><b>").append(name).append("</b>").append("</div>");
		}
		JSONArray employeeTree = row.optJSONArray(EMPLOYEE_TREE);
		if (employeeTree != null && employeeTree.length() > 0) {
			int length = employeeTree.length();
			for (int j = 0; j < length; j++) {
				html.append("<ul>");
				getDivHTML(employeeTree.getJSONObject(j), html, false, detailedView, xslt);
				html.append("</ul>");
			}
		}
		if (!isRoot) {
			html.append("</li>");
		}
	}

	public static JSONArray getEmployeeDirectandIndirectTeamMembers(String emailid) {
		try {
			JSONObject employee = getEmployee(emailid);
			if (employee != null) {
				Object employeeid = employee.get(Data.Query.KEY);
				Set<Object> immediateIndirectTeamMembers = getImmediateIndirectTeamMembers(employeeid, true);
				JSONArray directTeamMembers = employee.getJSONArray(IMMEDIATE_TEAM);
				int length = directTeamMembers == null ? 0 : directTeamMembers.length();
				for (int i = 0; i < length; i++) {
					immediateIndirectTeamMembers.add(directTeamMembers.get(i));
				}
				immediateIndirectTeamMembers.add(employeeid);
				return new JSONArray(immediateIndirectTeamMembers);
			}
		} catch (JSONException e) {
			throw new RuntimeException();
		}

		return new JSONArray();
	}

	private static Set<Object> getImmediateIndirectTeamMembers(Object employeeId, boolean onlyActive) throws JSONException {
		JSONObject parameters = new JSONObject();
		parameters.put("emopl__id", employeeId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.COLUMNS, new JSONArray().put(Data.Query.KEY));
		String filter = "indirectreportingto" + "={emopl__id}" + (onlyActive ? " AND employeestatusid=1" : "");
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		Set<Object> teamSet = new HashSet<Object>();
		int length = result.length();
		for (int i = 0; i < length; i++) {
			JSONObject row = result.getJSONObject(i);
			teamSet.add(row.get(Data.Query.KEY));
		}
		return teamSet;
	}

}
