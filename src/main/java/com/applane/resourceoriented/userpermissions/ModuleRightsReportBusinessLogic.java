package com.applane.resourceoriented.userpermissions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.app.cachehandler.AppGroupCacheHandler;
import com.applane.assistance.Translator;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class ModuleRightsReportBusinessLogic implements QueryJob {

	@Override
	public void onQuery(Query query) throws DeadlineExceededException {

		int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		LogUtility.writeLog("Current Organization Id : " + currentOrganizationId);

		query.addFilter(UserPermissionKinds.UserPermission.ORGANIZATION_ID + " = " + currentOrganizationId);
	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {

		LogUtility.writeLog(getClass().getName() + " >> onResult Begin");

		try {
			JSONArray records = result.getRecords();
			int noOfRecords = (records == null) ? 0 : records.length();

			Map<Object, UserPermissionDetails> userPermissionDetailMap = new LinkedHashMap<Object, UserPermissionDetails>();

			for (int i = 0; i < noOfRecords; i++) {

				JSONObject jsonObject = records.optJSONObject(i);

				Object userId = jsonObject.opt(UserPermissionKinds.UserPermission.USER_ID);
				String user = jsonObject.optString(UserPermissionKinds.UserPermission.USER);
				String userEmailId = jsonObject.optString(UserPermissionKinds.UserPermission.USER_EMAIL_ID);
				Object appGroupIdKey = jsonObject.opt(UserPermissionKinds.UserPermission.APPGROUPID_KEY);

				JSONObject appGroupJsonObject = AppGroupCacheHandler.getAppGroupWithKey(appGroupIdKey);
				String appGroupLabel = appGroupJsonObject.optString(UserPermissionKinds.AppGroups.LABEL);
				String appGroupType = appGroupJsonObject.optString(UserPermissionKinds.AppGroups.APP_GROUP_TYPE);

				UserPermissionDetails userPermissionDetails = userPermissionDetailMap.get(userId);

				if (userPermissionDetails == null) {

					userPermissionDetails = new UserPermissionDetails();
					userPermissionDetails.setUserId(userId);
					userPermissionDetails.setUser(user);
					userPermissionDetails.setUserEmailId(userEmailId);

					List<String> appGroupLabelList = new ArrayList<String>();
					userPermissionDetails.setAppGroupLabelList(appGroupLabelList);

					userPermissionDetailMap.put(userId, userPermissionDetails);
				}

				List<String> appGroupLabelList = userPermissionDetails.getAppGroupLabelList();
				if (!appGroupLabelList.contains(appGroupLabel) && appGroupType.equals(UserPermissionKinds.AppGroups.AppGroupType.LEAF)) {
					appGroupLabelList.add(appGroupLabel);
				}
			}

			JSONArray modulesRightReport = new JSONArray();

			for (Iterator<Object> iterator = userPermissionDetailMap.keySet().iterator(); iterator.hasNext();) {

				Object userId = iterator.next();
				UserPermissionDetails userPermissionDetails = userPermissionDetailMap.get(userId);
				List<String> appGroupLabelList = userPermissionDetails.getAppGroupLabelList();

				String appGroupLabels = "";
				if (appGroupLabelList.size() > 0) {
					appGroupLabels = appGroupLabelList.toString().replace("[", "").replace("]", "");
				}

				JSONObject reportJsonObject = new JSONObject();
				reportJsonObject.put("User", userPermissionDetails.getUser());
				reportJsonObject.put("Email", userPermissionDetails.getUserEmailId());
				reportJsonObject.put("AppGroups", appGroupLabels);

				modulesRightReport.put(reportJsonObject);
			}

			JSONObject reportsJsonObject = new JSONObject();
			reportsJsonObject.put("modulesRightReport", modulesRightReport);

			JSONArray array = new JSONArray();
			array.put(reportsJsonObject);

			records.put(new JSONObject().put("records", array));

			LogUtility.writeLog("Reports Array >> " + array);

		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeError("ModuleRightsReportBusinessLogic >> Exception Trace >> " + trace);
		}

		LogUtility.writeLog(getClass().getName() + " >> onResult End");
	}

	private class UserPermissionDetails {

		private Object userId;

		private String user;

		private String userEmailId;

		private List<String> appGroupLabelList;

		public Object getUserId() {
			return userId;
		}

		public void setUserId(Object userId) {
			this.userId = userId;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getUserEmailId() {
			return userEmailId;
		}

		public void setUserEmailId(String userEmailId) {
			this.userEmailId = userEmailId;
		}

		public List<String> getAppGroupLabelList() {
			return appGroupLabelList;
		}

		public void setAppGroupLabelList(List<String> appGroupLabelList) {
			this.appGroupLabelList = appGroupLabelList;
		}
	}

	interface UserPermissionKinds {

		String USER_PERMISSION = "up_userpermissions";

		String APPGROUPS = "appgroups";

		interface UserPermission {

			String USER_ID = "userid";

			String USER = "userid.user";

			String USER_EMAIL_ID = "userid.emailid";

			String APPGROUPID_KEY = "appgroupid_key";

			String APPGROUPID_ID = "appgroupid_id";

			String ORGANIZATION_ID = "organizationid";
		}

		interface AppGroups {

			String LABEL = "label";

			String APP_GROUP_TYPE = "appgrouptype";

			interface AppGroupType {

				String LEAF = "Leaf";
			}
		}
	}
}
