package com.applane.resourceoriented.hris;

import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class CreateEmployeeUser {

	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void createLoginDetail(Object[] key) {
		LogUtility.writeLog("CreateEmployeeUser-->createLoginDetail()...");

		try {
			if (key != null) {
				for (int counter = 0; counter < key.length; counter++) {

					String[] employeeDetail = getEmployeeDetail(key[counter]);
					LogUtility.writeLog("employeeDetail is :: >> " + employeeDetail.toString());
					String emailId = employeeDetail[0];
					LogUtility.writeLog("emailId is : >> " + emailId);
					if (emailId != null) {
						String name = employeeDetail[1];
						checkUserPermission("hris_portal", 776, emailId, name, 451, false, true);
					}
				}
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

	}

	private void setIds(HashSet<String> unResetableIDs) {
		unResetableIDs.add("inderjeet.singh@daffodilsw.com");
		unResetableIDs.add("dipendra.singh@daffodilsw.com");
		unResetableIDs.add("kushal.kumar@daffodilsw.com");
		unResetableIDs.add("sachin.mangla@daffodilsw.com");
		unResetableIDs.add("dolly.s@daffodilsw.com");
		unResetableIDs.add("ajaypal.singh@daffodilsw.com");
		unResetableIDs.add("shalini.singh@daffodilsw.com");
		unResetableIDs.add("sachin.garg@daffodilsw.com");
		unResetableIDs.add("yogesh@daffodilsw.com");
		unResetableIDs.add("ashish.mittal@daffodilsw.com");
		unResetableIDs.add("parveen.aggarwal@daffodilsw.com");
		unResetableIDs.add("nitin.goel@daffodildb.com");
		unResetableIDs.add("medha.sharma@daffodilsw.com");
		unResetableIDs.add("gaurav.sharma@daffodilsw.com");
		unResetableIDs.add("meenakshi.kamboj@daffodildb.com");
		unResetableIDs.add("pawan.phutela@daffodilsw.com");
		unResetableIDs.add("subhash.kumar@daffodilsw.com");
		unResetableIDs.add("rohit.bansal@daffodilsw.com");
		unResetableIDs.add("kapil.dalal@daffodilsw.com ");
		unResetableIDs.add("ashok.kumar@daffodilsw.com");
		unResetableIDs.add("munish.kumar@daffodilsw.com");
		unResetableIDs.add("santlal.kurmi@daffodilsw.com");
		unResetableIDs.add("victor@daffodilsw.com");
		unResetableIDs.add("rick@daffodilsw.com");
		unResetableIDs.add("steve@daffodilsw.com");
		unResetableIDs.add("sanjay.kumar@daffodilsw.com");
		unResetableIDs.add("mike@daffodilsw.com");
		unResetableIDs.add("matt@daffodilsw.com");
		unResetableIDs.add("joy@daffodilsw.com");
		unResetableIDs.add("alice@daffodilsw.com");
		unResetableIDs.add("vipin.chander@daffodilsw.com");
		unResetableIDs.add("anurag.rawat@daffodilsw.com");
		unResetableIDs.add("saundaraya.gupta@daffodilsw.com");
		unResetableIDs.add("sam@daffodilsw.com");
		unResetableIDs.add("david@daffodilsw.com");
		unResetableIDs.add("rajat.chugh@daffodilsw.com");
		unResetableIDs.add("chris@daffodilsw.com");
		unResetableIDs.add("rishi.sharma@daffodilsw.com");
		unResetableIDs.add("niti.kashyap@daffodilsw.com");
		unResetableIDs.add("riddhi.uppal@daffodilsw.com");
		unResetableIDs.add("ankur.khandelwal@daffodilsw.com");
		unResetableIDs.add("vipul.sharma@daffodilsw.com");
	}

	public String[] getEmployeeDetail(Object key) throws JSONException {
		LogUtility.writeLog("getEmployeeDetail Method call.....");
		String emailId = "";
		String name = "";
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("officialemailid");
		array.put("name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees");
		LogUtility.writeLog("rows is : > >" + rows);
		if (rows != null) {
			if (rows.length() > 0) {
				emailId = rows.getJSONObject(0).optString("officialemailid");
				LogUtility.writeLog("emailId is : >> " + emailId);
				name = rows.getJSONObject(0).optString("name");
				LogUtility.writeLog("name is : >> " + name);
			}
		}
		if (emailId != null && emailId.length() > 0) {
			return new String[] { emailId, name };
		} else {
			throw new BusinessLogicException("Official Email Id not define for " + name);
		}
	}

	public Object getUserId(String emailId, String userName, boolean update) throws JSONException {
		LogUtility.writeLog("getUserId Method call....");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "up_users");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "emailid = \"" + emailId + "\"");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("up_users");
		LogUtility.writeLog("rows is : >> " + rows);
		Object userId = null;
		if (rows != null) {
			if (rows.length() > 0) {
				userId = rows.getJSONObject(0).opt("__key__");
				LogUtility.writeLog("userId is : >> " + userId);
				HashSet<String> unResetableIDs = new HashSet<String>();
				setIds(unResetableIDs);
				if (update && !unResetableIDs.contains(emailId)) {
					udpateAppalneUser(userId);
				}
			} else {
				LogUtility.writeLog("Come for create applane user....1");
				userId = createAppalneUser(emailId, userName);
			}
		} else {
			LogUtility.writeLog("Come for create applane user.....2");
			userId = createAppalneUser(emailId, userName);
		}
		return userId;
	}

	private void udpateAppalneUser(Object key) throws JSONException {
		LogUtility.writeLog("update applane user method calll....");
		String password = "f771ac9bb3698b0374ec8af2c83562cf";
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "up_users");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("password", password);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private Object createAppalneUser(String emailId, String userName) throws JSONException {
		LogUtility.writeLog("Create applane user methos calll.....");
		Object ORGANIZATION_ID = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
		Object userId;
		String password = "f771ac9bb3698b0374ec8af2c83562cf";
		userName = userName == null ? emailId : userName.length() == 0 ? emailId : userName;
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "up_users");
		JSONObject row = new JSONObject();
		row.put("emailid", emailId);
		row.put("organizations", ORGANIZATION_ID);
		row.put("provider", "applane");
		row.put("status", 1);
		row.put("user", userName);
		row.put("password", password);
		row.put("limit", 50);
		updates.put(Data.Update.UPDATES, row);
		JSONObject studentJSON = ResourceEngine.update(updates);

		JSONArray jsonarray = studentJSON.getJSONArray("up_users");
		studentJSON = jsonarray.getJSONObject(0).getJSONObject("__key__");
		userId = studentJSON.get("actual");
		return userId;
	}

	public void checkUserPermission(String applicationid, Object APPLICATION_ID, String emailId, String userName, Object roleId, boolean isdelete, boolean isPasswordUpdate) throws JSONException {
		com.applane.databaseengine.LogUtility.writeLog("Permission Method Called");

		Object ORGANIZATION_ID = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);

		Object userId = getUserId(emailId, userName, isPasswordUpdate);
		LogUtility.writeLog("userId is : : >> " + userId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "up_userpermissions");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("roles");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "applicationid.id = '" + applicationid + "' and organizationid='" + ORGANIZATION_ID + "' and userid='" + userId + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("up_userpermissions");
		boolean isRights = false;
		HashSet<Object> set = new HashSet<Object>();
		set.add(roleId);
		Object permissionId = null;
		JSONArray arr = new JSONArray();
		if (rows != null) {
			if (rows.length() > 0) {
				permissionId = rows.getJSONObject(0).opt("__key__");
				Object roles = rows.getJSONObject(0).opt("roles");

				if (roles instanceof JSONArray) {
					arr = (JSONArray) roles;
				} else {
					arr = arr.put(roles);
				}
				for (int counter = 0; counter < arr.length(); counter++) {

					Object oldRoleId = arr.opt(counter);
					if (set.contains(oldRoleId)) {
						isRights = true;
					}

				}
				com.applane.databaseengine.LogUtility.writeLog(arr.toString());
			}
		}
		LogUtility.writeLog("isdelete[" + isdelete + "]isRights[" + isRights + "]");
		if (!isRights && !isdelete) {

			if (permissionId == null) {
				JSONObject application = new JSONObject();
				application.put("__key__", APPLICATION_ID);
				application.put("id", applicationid);
				JSONObject updates = new JSONObject();
				updates.put(Data.Query.RESOURCE, "up_userpermissions");
				JSONObject row = new JSONObject();
				row.put("applicationid", application);
				row.put("organizationid", ORGANIZATION_ID);
				row.put("userid", userId);
				row.put("datascope", "Self");
				updates.put(Data.Update.UPDATES, row);
				JSONObject teacherJSON = ResourceEngine.update(updates);

				JSONArray jsonarray = teacherJSON.getJSONArray("up_userpermissions");
				teacherJSON = jsonarray.getJSONObject(0).getJSONObject("__key__");
				permissionId = teacherJSON.get("actual");
			}

			try {
				boolean rolealready = false;
				query = new JSONObject();
				query.put(Data.Query.RESOURCE, "up_userpermissions__roles");
				array = new JSONArray();
				array.put("roles");
				query.put(Data.Query.COLUMNS, array);
				query.put(Data.Query.FILTERS, "up_userpermissions= '" + permissionId + "' and roles='" + roleId + "'");
				JSONObject quoteobject;
				quoteobject = ResourceEngine.query(query);
				rows = quoteobject.getJSONArray("up_userpermissions__roles");

				if (rows != null && rows.length() > 0) {
					rolealready = true;
				}
				if (!rolealready) {
					com.applane.databaseengine.LogUtility.writeLog("Role Given");
					JSONObject updates = new JSONObject();
					updates.put(Data.Query.RESOURCE, "up_userpermissions__roles");
					JSONObject row = new JSONObject();
					row.put("up_userpermissions", permissionId);
					row.put("roles", roleId);
					updates.put(Data.Update.UPDATES, row);
					ResourceEngine.update(updates);
				}
			} catch (Exception e) {
				com.applane.databaseengine.LogUtility.writeLog("Already Role Given");
			}
		}
	}
}
