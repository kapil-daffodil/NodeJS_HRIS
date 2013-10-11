package com.applane.afb.user;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.afb.AFBConstants;
import com.applane.afb.AFBConstants.BusinessFunctionApplications;
import com.applane.afb.AFBConstants.BusinessFunctions;
import com.applane.afb.AFBConstants.DefaultApplications;
import com.applane.app.cachehandler.AppGroupCacheHandler;
import com.applane.app.exception.AppGroupNotFoundException;
import com.applane.app.exception.ApplicationNotFoundException;
import com.applane.app.shared.constants.AppsStudio;
import com.applane.app.shared.constants.AppsStudio.Kind.AppGroups;
import com.applane.app.shared.constants.AppsStudio.Kind.AppParameters;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.ResourceNotFoundException;
import com.applane.databaseengine.Server;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.DatabaseEngineUtility;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.hris.HrisKinds.Employess;
import com.applane.moduleimpl.shared.constants.ModuleConstants;
import com.applane.service.applaneurlfetch.ApplaneURLFetch;
import com.applane.service.mail.ApplaneMail;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.cachehandler.UserCacheHandler;
import com.applane.user.exception.UserNotFoundException;
import com.applane.user.shared.constants.UserPermission;
import com.applane.user.shared.constants.UserPermission.Kind.UserPermissionParameters;
import com.applane.user.shared.constants.UserPermission.Kind.UserPermissions;
import com.google.appengine.api.utils.SystemProperty;

public class AfbUserService {

	/**
	 * Method to allocate permission to new User added into an organization.
	 * 
	 * @param record
	 */

	public static final String PROFIT_CENTER = AFBConstants.AfbUserService.PROFIT_CENTER;

	public static void addUser(Record record) throws JSONException, ResourceNotFoundException, AppGroupNotFoundException, UserNotFoundException {
		String when = record.when();
		if (record.has(HrisKinds.Employess.CREATE_LOGIN_ACCOUNT) && record.has(HrisKinds.Employess.OFFICIAL_EMAIL_ID)) {
			if (when.equals(Record.BEFORE)) {
				throw new BusinessLogicException("You can not perform this this task in before job.");
			}
			String userName = (String) record.getValue(HrisKinds.Employess.FIRST_NAME);
			String lastName = (String) record.getValue(HrisKinds.Employess.LAST_NAME);
			if (lastName != null && lastName.trim().length() > 0) {
				userName = userName + " " + lastName;
			}
			/* If user is created from HRIS_Services in CRM then profitCentreAlias will be available otherwise profitCentreid */
			Object profitCentersobj = record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID_ALIAS);
			Object reportingToEmpId = (record.getValue(HrisKinds.Employess.REPORTING_TO));
			String whiteListedIpAddress = (String) record.getValue(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS);
			JSONArray profitCenters = (JSONArray) (profitCentersobj == null ? record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID) : profitCentersobj);
			String officialEmail = (String) record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
			Object userId = UserCacheHandler.addUserIfNotExist(officialEmail, userName, null, null, 0, true);
			boolean highPrivilege = DatabaseEngineUtility.getBooleanValue(record.getValue(HrisKinds.Employess.HIGH_PRIVILEGE));
			Object reportingTo = reportingToEmpId == null ? null : getUserId(reportingToEmpId);

			Object businessFunctionIds = record.getValue(HrisKinds.Employess.BUSINESS_FUNCTION_ID);
			JSONArray businessFunctions = businessFunctionIds.equals(JSONObject.NULL) ? null : (businessFunctionIds instanceof JSONArray ? (JSONArray) businessFunctionIds : new JSONArray().put(businessFunctionIds));
			LogUtility.writeError("Business Functions found are " + businessFunctions);
			if (businessFunctions != null && businessFunctions.length() > 0) {
				JSONArray appGroups = getBusinessFunctionAppGroups(businessFunctions);
				allocatePermissions(userId, appGroups, profitCenters, reportingTo, whiteListedIpAddress, highPrivilege);
			}
			allocateUnviversalPermissions(userId, profitCenters, reportingTo, whiteListedIpAddress, highPrivilege);
		}
	}

	private static Object getUserId(Object reportingToEmpId) throws JSONException, UserNotFoundException {
		JSONObject parameters = new JSONObject();
		parameters.put("__key____", reportingToEmpId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.COLUMNS, new JSONArray().put(HrisKinds.Employess.OFFICIAL_EMAIL_ID));
		query.put(Data.Query.FILTERS, Data.Query.KEY + "={__key____}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		Object userId = null;
		if (result.length() > 0) {
			String emailid = result.getJSONObject(0).getString(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
			userId = UserCacheHandler.getUser(emailid).get(Data.Query.KEY);
		}
		return userId;
	}

	/**
	 * 
	 * @param record
	 * @throws JSONException
	 * @throws UserNotFoundException
	 * @throws ResourceNotFoundException
	 * @throws ApplicationNotFoundException
	 */
	public static void updateUser(Record record) throws JSONException, UserNotFoundException, ResourceNotFoundException, AppGroupNotFoundException {
		String when = record.when();
		boolean businessFunctionUpdated = false;
		boolean profitCentreUpdated = false;
		if (when.equals(Record.BEFORE)) {
			throw new BusinessLogicException("You can not perform this action in Before job.");
		}
		if (record.has(HrisKinds.Employess.EMPLOYEE_STATUS) || record.has(HrisKinds.Employess.EMPLOYEE_STATUS + ".name")) {
			Object updatedStaus = record.getValue(HrisKinds.Employess.EMPLOYEE_STATUS);
			if (updatedStaus.toString().equals(AFBConstants.AfbUserService.ACTIVATE_ACCOUNT)) {
				allocatePermissionsToExistingEmployee(record);
				return;
			} else if (updatedStaus.toString().equals(AFBConstants.AfbUserService.DEACTIVATE_ACCOUNT)) {
				String emailid = (String) record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
				misallocateUserPermission(UserCacheHandler.getUser(emailid).get(Data.Query.KEY));
				return;
			}
		}
		if (record.has(HrisKinds.Employess.CREATE_LOGIN_ACCOUNT)) {
			Boolean createAccount = DatabaseEngineUtility.getBooleanValue(record.getNewValue(HrisKinds.Employess.CREATE_LOGIN_ACCOUNT));
			if (createAccount) {
				allocatePermissionsToExistingEmployee(record);
			} else {
				String emailid = (String) record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
				misallocateUserPermission(UserCacheHandler.getUser(emailid).get(Data.Query.KEY));
			}
			return;
		}

		if (record.has(HrisKinds.Employess.OFFICIAL_EMAIL_ID)) {
			return;
		}

		Set<Long> updatedBusinessFunctions = convertToSet((JSONArray) record.getValue(HrisKinds.Employess.BUSINESS_FUNCTION_ID));
		Set<Long> bFunctionsAdded = new HashSet<Long>();
		Set<Long> bFunctionsRemoved = new HashSet<Long>();
		if (record.has(HrisKinds.Employess.BUSINESS_FUNCTION_ID)) {
			businessFunctionUpdated = true;
			excerciseBusinessFunctions(record, bFunctionsAdded, bFunctionsRemoved);
		}
		if (record.has(HrisKinds.Employess.PROFIT_CENTER_ID_ALIAS) || record.has(HrisKinds.Employess.PROFIT_CENTER_ID)) {
			profitCentreUpdated = true;
		}
		Boolean scope = record.has(HrisKinds.Employess.HIGH_PRIVILEGE) ? scope = DatabaseEngineUtility.getBooleanValue(record.getNewValue(HrisKinds.Employess.HIGH_PRIVILEGE)) : null;
		String whiteListedIpAddresses = record.has(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS) ? (String) record.getValue(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS) : null;
		Object emailId = record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		if (emailId == null || emailId.equals(JSONObject.NULL)) {
			return;
		}

		/* Update User Name */
		if (record.has(HrisKinds.Employess.FIRST_NAME) || record.has(HrisKinds.Employess.LAST_NAME)) {
			String firstName = (String) record.getValue(HrisKinds.Employess.FIRST_NAME);
			String lastName = (String) (record.has(HrisKinds.Employess.LAST_NAME) ? record.getValue(HrisKinds.Employess.LAST_NAME) : record.getOldValue(HrisKinds.Employess.LAST_NAME));
			String userName = firstName + ((lastName == null || JSONObject.NULL.equals(lastName)) ? "" : (" " + lastName));
			UserCacheHandler.updateUserName((String) emailId, userName);
		}

		Object userId = UserCacheHandler.getUser(emailId).get(Data.Query.KEY);
		LogUtility.writeError("BusinessFunction Added : " + businessFunctionUpdated + " ProfitCenetr Added " + profitCentreUpdated);
		JSONArray remainingBFunctions = null;
		if (businessFunctionUpdated) {
			Set<Long> oldExistingBF = new HashSet<Long>(updatedBusinessFunctions);
			oldExistingBF.removeAll(bFunctionsAdded);
			remainingBFunctions = new JSONArray(oldExistingBF);
		} else {
			remainingBFunctions = new JSONArray(updatedBusinessFunctions);
		}
		/* Business Function update handling */
		if (businessFunctionUpdated) {
			if (bFunctionsRemoved.size() > 0) {
				misallocateUserPermission(userId, bFunctionsRemoved);
			}
			if (bFunctionsAdded.size() > 0) {
				JSONArray applications = getBusinessFunctionAppGroups(new JSONArray(bFunctionsAdded));
				JSONArray profitCentre = null;
				if (!profitCentreUpdated) {
					Object profiteCentersObject = record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID_ALIAS);
					profitCentre = profiteCentersObject == null ? (JSONArray) record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID) : (JSONArray) profiteCentersObject;
				}
				allocatePermissions(userId, applications, profitCentre, null, whiteListedIpAddresses, scope);
			}
		}
		/* Profit Centre update handling */
		if (profitCentreUpdated) {
			JSONArray result = getUserpermissionKeys(userId);
			removeProfitCenterPermissions(result);
			JSONArray profiteCentres = record.has(HrisKinds.Employess.PROFIT_CENTER_ID_ALIAS) ? (JSONArray) record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID_ALIAS) : (JSONArray) record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID);
			if (profiteCentres.length() > 0) {
				LogUtility.writeError("ProfiteCenetr Alias : " + profiteCentres);
				addProfitCenter(result, profiteCentres);
			}
		}
		if (remainingBFunctions == null || remainingBFunctions.length() == 0) {
			/* Nothing left to update */
			return;
		}
		if (scope != null) {
			/* Scope update handling */
			updateScope(userId, remainingBFunctions, whiteListedIpAddresses, scope);
		} else if (record.has(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS)) {
			updateScope(userId, remainingBFunctions, whiteListedIpAddresses, null);
		}
	}

	private static void addProfitCenter(JSONArray userPermission, JSONArray array) throws JSONException, ResourceNotFoundException {
		int length = userPermission.length();
		for (int i = 0; i < length; i++) {
			JSONObject row = userPermission.getJSONObject(i);
			Object appGroupId = row.get(UserPermissions.APPGROUP_ID);
			JSONObject appParameters = getAppParameters(appGroupId);
			if (appParameters != null && appParameters.length() > 0) {
				JSONArray addAppParameters = addAppParameters(appParameters, array, row.get(Data.Query.KEY));
				if (addAppParameters != null && addAppParameters.length() > 0) {
					JSONObject updateProfiteCenter = new JSONObject();
					updateProfiteCenter.put(Data.Update.RESOURCE, UserPermission.Kind.USER_PERMISSION_PARAMETERS);
					updateProfiteCenter.put(Data.Update.UPDATES, addAppParameters);
					ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updateProfiteCenter);
				}
			}
		}
	}

	/**
	 * 
	 * @param record
	 * @throws JSONException
	 * @throws UserNotFoundException
	 */
	public static void deleteUser(Record record) throws JSONException, UserNotFoundException {
		String when = record.when();
		if (when.equals(Record.BEFORE)) {
			throw new BusinessLogicException("You can not perform this action in before job.");
		}
		String officialEmailids = (String) record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		Object userid = UserCacheHandler.getUser(officialEmailids).get(Data.Query.KEY);
		Object organizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);

		JSONObject parameters = new JSONObject();
		parameters.put("userId__", userid);
		parameters.put("orgid__", organizationId);

		StringBuilder filters = new StringBuilder(UserPermissions.USER_ID).append("={userId__}");
		filters.append(" AND ");
		filters.append(UserPermissions.ORGANIZATION_ID).append("={orgid__}");
		filters.append(" AND ");
		filters.append(UserPermissions.DERIVED).append("='").append(UserPermissions.DerivedFrom.BUSINESS_FUNCTION).append("'");

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		query.put(Data.Query.FILTERS, filters.toString());
		query.put(Data.Query.PARAMETERS, parameters);

		JSONObject update = new JSONObject();
		update.put(Updates.QUERY, query);
		update.put(Updates.TYPE, Updates.Types.DELETE);

		JSONObject deleteOperation = new JSONObject();
		deleteOperation.put(Data.Update.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		deleteOperation.put(Data.Update.UPDATES, update);

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(deleteOperation);
	}

	public static void allocateUniversalApp(Object key) throws JSONException, AppGroupNotFoundException, ResourceNotFoundException, UserNotFoundException {
		JSONObject appGroup = new JSONObject();
		JSONObject appParameters = getAppParameters(key);
		appGroup.put(AppsStudio.Kind.APP_PARAMETERS, appParameters);
		appGroup.put(BusinessFunctions.APPGROUP_ID, key);

		/****
		 * Query to fetch employee who has login enable account in current organization
		 ****/
		JSONArray requiredColumns = new JSONArray();
		requiredColumns.put(HrisKinds.Employess.HIGH_PRIVILEGE);
		requiredColumns.put(HrisKinds.Employess.PROFIT_CENTER_ID);
		requiredColumns.put(HrisKinds.Employess.REPORTING_TO);
		requiredColumns.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		requiredColumns.put(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS);

		JSONObject employeeQuery = new JSONObject();
		employeeQuery.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		employeeQuery.put(Data.Query.COLUMNS, requiredColumns);
		employeeQuery.put(Data.Query.FILTERS, HrisKinds.Employess.CREATE_LOGIN_ACCOUNT + "=true AND " + HrisKinds.Employess.OFFICIAL_EMAIL_ID + "!=null");

		JSONArray employeesInfo = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(employeeQuery).getJSONArray(HrisKinds.EMPLOYEES);
		int employeesCount = employeesInfo.length();
		for (int i = 0; i < employeesCount; i++) {
			JSONObject employee = employeesInfo.getJSONObject(i);
			Object userId = UserCacheHandler.addUserIfNotExist(employee.getString(Employess.OFFICIAL_EMAIL_ID), employee.getString(Employess.OFFICIAL_EMAIL_ID));
			Object reportingTo = employee.isNull(Employess.REPORTING_TO) ? null : getUserId(employee.get(Employess.REPORTING_TO));
			JSONArray profitCenters = employee.isNull(Employess.PROFIT_CENTER_ID) ? null : employee.getJSONArray(Employess.PROFIT_CENTER_ID);
			Boolean highPrivilege = employee.isNull(Employess.HIGH_PRIVILEGE) ? false : DatabaseEngineUtility.getBooleanValue(employee.get(Employess.HIGH_PRIVILEGE));
			String whiteListedIpAddresses = employee.optString(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS, null);
			allocatePermissions(userId, new JSONArray().put(appGroup), profitCenters, reportingTo, whiteListedIpAddresses, highPrivilege);
		}
	}

	/************************************ Method when a new employee is added **********************************************/

	/**
	 * 
	 * @param userId
	 * @param appGroups
	 * @param profitCenters
	 * @param reportingTo
	 * @param whiteListedIp
	 * @param highPrivilege
	 * @throws JSONException
	 * @throws ResourceNotFoundException
	 * @throws AppGroupNotFoundException
	 */
	private static void allocatePermissions(Object userId, JSONArray appGroups, JSONArray profitCenters, Object reportingTo, String whiteListedIp, Boolean highPrivilege) throws JSONException, ResourceNotFoundException, AppGroupNotFoundException {
		JSONArray userPermissions = new JSONArray();
		JSONArray appKeys = new JSONArray();
		String accessLevel = highPrivilege == null ? null : (highPrivilege ? UserPermissions.DataScope.ORGANIZATION : UserPermissions.DataScope.TEAM);
		int noOfAppGroups = appGroups != null ? appGroups.length() : 0;
		for (int j = 0; j < noOfAppGroups; j++) {
			JSONObject appGroupInfo = appGroups.getJSONObject(j);
			Object appGroupKey = appGroupInfo.get(BusinessFunctions.APPGROUP_ID);
			String appGroupId = AppGroupCacheHandler.getAppGroupWithKey(appGroupKey).getString(AppGroups.ID);
			JSONObject populatedPermission = populatePermissionRow(userId, appGroupKey, appGroupId, whiteListedIp);
			populatedPermission.put(UserPermissions.DATA_SCOPE, accessLevel);
			JSONObject appParameters = appGroupInfo.optJSONObject(AppsStudio.Kind.APP_PARAMETERS);

			if (profitCenters != null && profitCenters.length() > 0 && appParameters != null) {
				JSONArray addAppParameters = addAppParameters(appParameters, profitCenters, null);
				populatedPermission.put(UserPermission.Kind.USER_PERMISSION_PARAMETERS, addAppParameters);
			}
			userPermissions.put(populatedPermission);
			appKeys.put(appGroupKey);
		}
		LogUtility.writeError("Going to inser ....userpermissions " + userPermissions);
		if (userPermissions.length() > 0) {
			insertUserPermissions(userPermissions);
		}
	}

	private static void allocateUnviversalPermissions(Object userId, JSONArray profitCenters, Object reportingTo, String whiteListedIpAddresses, Boolean highPrivilege) throws JSONException, AppGroupNotFoundException, ResourceNotFoundException {
		Object organizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
		JSONArray appGroups = getDefaultAppGroups(organizationId);
		if (appGroups.length() > 0) {
			allocatePermissions(userId, appGroups, profitCenters, reportingTo, whiteListedIpAddresses, highPrivilege);
		}
	}

	private static JSONArray getDefaultAppGroups(Object organizationId) throws JSONException {
		JSONObject parameters = new JSONObject();
		parameters.put("org__i_d", organizationId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AFBConstants.DEFAULT_APPLICATIONS);
		query.put(Data.Query.COLUMNS, new JSONArray().put(DefaultApplications.APPGROUP_ID));
		query.put(Data.Query.FILTERS, ModuleConstants.UserNamespace.ORGANIZATION_ID + "={org__i_d}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AFBConstants.DEFAULT_APPLICATIONS);
		JSONArray appGroups = new JSONArray();
		int length = result.length();
		for (int i = 0; i < length; i++) {
			appGroups.put(new JSONObject().put(BusinessFunctions.APPGROUP_ID, result.getJSONObject(i).get(DefaultApplications.APPGROUP_ID)));
		}
		return appGroups;
	}

	private static void insertUserPermissions(JSONArray userPermissions) throws JSONException {
		JSONObject child = new JSONObject();
		child.put(Data.Query.Childs.RELATED_COLUMN, UserPermissionParameters.USER_PERMISSION_ID);
		child.put(Data.Query.Childs.ALIAS, UserPermission.Kind.USER_PERMISSION_PARAMETERS);
		child.put(Data.Query.Childs.QUERY, new JSONObject().put(Data.Query.RESOURCE, UserPermission.Kind.USER_PERMISSION_PARAMETERS));

		JSONObject userPermission = new JSONObject();
		userPermission.put(Data.Update.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		userPermission.put(Data.Update.UPDATES, userPermissions);
		userPermission.put(Data.Update.CHILDS, new JSONArray().put(child));

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(userPermission);
	}

	private static JSONArray addAppParameters(JSONObject parameters, JSONArray profitCenters, Object userpermissionid) throws JSONException, ResourceNotFoundException {
		Object referenceQuery = parameters.get(AppParameters.REFERENCE_QUERY);
		String displayName = parameters.getString(AppParameters.DISPLAY_NAME);

		JSONObject appParameter = new JSONObject();
		appParameter.put(Data.Query.KEY, parameters.opt(Data.Query.KEY));
		appParameter.put(AppParameters.PARAMETER_NAME, parameters.opt(AppParameters.PARAMETER_NAME));
		appParameter.put(AppParameters.REFERENCE_QUERY, referenceQuery);
		appParameter.put(AppParameters.DISPLAY_NAME, displayName);

		Set<Object> manipulatedProfiteCenters = new HashSet<Object>();
		Map<Object, String> scopeMapping = new HashMap<Object, String>();
		manipulateProfitCentreAndDataScope(profitCenters, manipulatedProfiteCenters, scopeMapping);
		JSONArray result = getProfitCenters(referenceQuery, new JSONArray(manipulatedProfiteCenters));
		int length = result.length();
		JSONArray appParameters = new JSONArray();
		for (int i = 0; i < length; i++) {
			JSONObject value = result.getJSONObject(i);
			Object profiteCentreId = value.get(Data.Query.KEY);

			JSONObject appParameterValue = new JSONObject();
			appParameterValue.put("name", value.getString(displayName));
			appParameterValue.put("key", profiteCentreId);
			appParameterValue.put("table", referenceQuery);
			appParameterValue.put(Data.Query.KEY, value.get(Data.Query.KEY));

			JSONObject populatedParameter = new JSONObject();
			populatedParameter.put(UserPermissionParameters.APP_PARAMETER_ID, new JSONObject(appParameter.toString()));
			populatedParameter.put(UserPermissionParameters.VALUE, appParameterValue);
			populatedParameter.put(UserPermissionParameters.USER_PERMISSION_ID, userpermissionid);
			populatedParameter.put(UserPermissionParameters.DATA_SCOPE, scopeMapping.get(profiteCentreId));

			appParameters.put(populatedParameter);
		}
		return appParameters.length() > 0 ? appParameters : null;
	}

	/**
	 * This method allocated data access right according to profit centre.
	 * 
	 * @param profitCenters
	 * @param manipulatedProfiteCenters
	 * @param scopeMapping
	 * @throws JSONException
	 */
	private static void manipulateProfitCentreAndDataScope(JSONArray profitCenters, Set<Object> manipulatedProfiteCenters, Map<Object, String> scopeMapping) throws JSONException {
		int length = profitCenters.length();
		if (length > 0) {
			Object object = profitCenters.get(0);
			if (object instanceof JSONObject) {
				for (int i = 0; i < length; i++) {
					JSONObject profitCentreDetails = profitCenters.getJSONObject(i);
					Object profitCenterObj = profitCentreDetails.get(HrisKinds.Employess.PROFIT_CENTER_ID);
					Object key = (profitCenterObj instanceof JSONObject) ? ((JSONObject) profitCenterObj).get(HrisKinds.Employess.PROFIT_CENTER_ID) : profitCenterObj;
					boolean organizationLevelScope = DatabaseEngineUtility.getBooleanValue(profitCentreDetails.opt(HrisKinds.EmployeesProfitCentre.ORGANIZATION_LEVEL_SCOPE));
					String scope = organizationLevelScope ? UserPermissions.DataScope.ORGANIZATION : UserPermissions.DataScope.TEAM;
					scopeMapping.put(key, scope);
					manipulatedProfiteCenters.add(key);
				}
			} else {
				for (int i = 0; i < length; i++) {
					Object key = profitCenters.get(i);
					scopeMapping.put(key, UserPermissions.DataScope.TEAM);
					manipulatedProfiteCenters.add(key);
				}
			}
		}
	}

	private static JSONArray getProfitCenters(Object referenceQuery, JSONArray profitCenters) throws JSONException {

		JSONObject parameters = new JSONObject();
		parameters.put("alotedprofitcenters__", profitCenters);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, referenceQuery);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "IN {alotedprofitcenters__}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray((String) referenceQuery);
		return result;
	}

	private static JSONObject populatePermissionRow(Object userid, Object appGroupKey, String appGroupID, String whiteListedIpAddresses) throws JSONException {
		Object organizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);

		JSONObject newPermission = new JSONObject();
		newPermission.put(UserPermissions.APPGROUP_ID, appGroupKey);
		newPermission.put(UserPermissions.APPGROUP_ID + "." + AppGroups.ID, appGroupID);
		newPermission.put(UserPermissions.ORGANIZATION_ID, organizationId);
		newPermission.put(UserPermissions.USER_ID, userid);
		newPermission.put(UserPermissions.DERIVED, UserPermissions.DerivedFrom.BUSINESS_FUNCTION);
		newPermission.put(UserPermissions.IGNORE_WELCOME_MAIL, true);
		newPermission.put(UserPermissions.PERMISIBLE_IP_ADDRESSES, whiteListedIpAddresses);
		return newPermission;
	}

	/******************************************** Method to call when employee is updated ********************************************/

	private static void removeProfitCenterPermissions(JSONArray result) throws JSONException {
		int length = result.length();
		if (length > 0) {
			JSONArray permissionIds = new JSONArray();
			for (int i = 0; i < length; i++) {
				permissionIds.put(result.getJSONObject(i).get(Data.Query.KEY));
			}

			JSONObject parameters = new JSONObject();
			parameters.put("keys__", permissionIds);

			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, UserPermission.Kind.USER_PERMISSION_PARAMETERS);
			query.put(Data.Query.COLUMNS, new JSONArray().put(Data.Query.KEY));
			query.put(Data.Query.FILTERS, UserPermissionParameters.USER_PERMISSION_ID + " IN{keys__}");
			query.put(Data.Query.PARAMETERS, parameters);

			JSONObject update = new JSONObject();
			update.put(Data.Update.Updates.QUERY, query);
			update.put(Updates.TYPE, Updates.Types.DELETE);

			JSONObject deleteOperation = new JSONObject();
			deleteOperation.put(Data.Update.RESOURCE, UserPermission.Kind.USER_PERMISSION_PARAMETERS);
			deleteOperation.put(Data.Update.UPDATES, update);
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(deleteOperation);
		}

	}

	private static JSONArray getUserpermissionKeys(Object userId) throws JSONException {
		Object currentOrganizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
		JSONObject parameters = new JSONObject();
		parameters.put("orga_n_z_yionid", currentOrganizationId);
		parameters.put("U_s__ree__", userId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		query.put(Data.Query.COLUMNS, new JSONArray().put(UserPermissions.APPGROUP_ID));
		query.put(Data.Query.FILTERS, UserPermissions.USER_ID + " = {U_s__ree__} AND " + UserPermissions.ORGANIZATION_ID + " = {orga_n_z_yionid} AND " + UserPermissions.DERIVED + " != NULL");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(UserPermission.Kind.USERPERMISSIONS);
		return result;
	}

	/**
	 * Method to update scope in default applications and applications related to the remaining business functions left.
	 * 
	 * @param userId
	 * @param remainingBFunctions
	 * @param scope
	 * @throws JSONException
	 */
	private static void updateScope(Object userId, JSONArray remainingBFunctions, String whiteListedIPAddress, Boolean scope) throws JSONException {
		Object organizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
		JSONArray appGroups = new JSONArray();
		JSONArray functionAppGroups = getBusinessFunctionAppGroups(remainingBFunctions);
		int length = functionAppGroups == null ? 0 : functionAppGroups.length();
		for (int i = 0; i < length; i++) {
			appGroups.put(functionAppGroups.getJSONObject(i).get(BusinessFunctions.APPGROUP_ID));
		}
		JSONArray defaultAppGroups = getDefaultAppGroups(organizationId);
		length = defaultAppGroups.length();
		for (int i = 0; i < length; i++) {
			appGroups.put(defaultAppGroups.getJSONObject(i).get(BusinessFunctions.APPGROUP_ID));
		}

		JSONObject parameter = new JSONObject();
		parameter.put("userid__", userId);
		parameter.put("org__id_", organizationId);
		parameter.put("a__p_pKEY", appGroups);

		StringBuilder filterBuilder = new StringBuilder(UserPermissions.USER_ID).append("{userid__}");
		filterBuilder.append(" AND ");
		filterBuilder.append(UserPermissions.ORGANIZATION_ID).append("={org__id_}");
		filterBuilder.append(" AND ");
		filterBuilder.append(UserPermissions.APPGROUP_ID).append("_key IN {a__p_pKEY}");

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		query.put(Data.Query.FILTERS, filterBuilder.toString());
		query.put(Data.Query.PARAMETERS, parameter);

		String scopeLevel = scope ? UserPermissions.DataScope.ORGANIZATION : UserPermissions.DataScope.TEAM;

		JSONObject update = new JSONObject();
		update.put(Updates.TYPE, Updates.Types.UPDATE);
		update.put(UserPermissions.DATA_SCOPE, scopeLevel);
		update.put(UserPermissions.PERMISIBLE_IP_ADDRESSES, whiteListedIPAddress);
		update.put(Updates.QUERY, query);

		JSONObject updateOperation = new JSONObject();
		updateOperation.put(Data.Update.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		updateOperation.put(Data.Update.UPDATES, update);
	}

	public static void misallocateUniversalApp(Object appGroupKey) throws JSONException, UserNotFoundException {
		JSONArray requiredColumns = new JSONArray();
		requiredColumns.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID);

		JSONObject employeeQuery = new JSONObject();
		employeeQuery.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		employeeQuery.put(Data.Query.COLUMNS, requiredColumns);
		employeeQuery.put(Data.Query.FILTERS, HrisKinds.Employess.CREATE_LOGIN_ACCOUNT + "=true AND " + HrisKinds.Employess.OFFICIAL_EMAIL_ID + "!=null");

		JSONArray employeesInfo = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(employeeQuery).getJSONArray(HrisKinds.EMPLOYEES);
		int employeesCount = employeesInfo.length();
		JSONArray appGroups = new JSONArray().put(appGroupKey);
		for (int i = 0; i < employeesCount; i++) {
			JSONObject employee = employeesInfo.getJSONObject(i);
			Object userId = UserCacheHandler.addUserIfNotExist(employee.getString(Employess.OFFICIAL_EMAIL_ID), employee.getString(Employess.OFFICIAL_EMAIL_ID));
			misallocateUserPermission(userId, appGroups);
		}
	}

	private static void misallocateUserPermission(Object userid) throws JSONException {
		Object organizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);

		JSONObject parameters = new JSONObject();
		parameters.put("userr__id_", userid);
		parameters.put("org__id_", organizationId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		query.put(Data.Query.FILTERS, UserPermissions.USER_ID + "={userr__id_} AND " + UserPermissions.ORGANIZATION_ID + "={org__id_} AND " + UserPermissions.DERIVED + "='" + UserPermissions.DerivedFrom.BUSINESS_FUNCTION + "'");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONObject update = new JSONObject();
		update.put(Updates.TYPE, Updates.Types.DELETE);
		update.put(Updates.QUERY, query);

		JSONObject deleteOperation = new JSONObject();
		deleteOperation.put(Data.Update.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		deleteOperation.put(Data.Update.UPDATES, update);

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(deleteOperation);
	}

	private static void misallocateUserPermission(Object userId, Set<Long> bFunctionsRemoved) throws JSONException {
		JSONArray appKeys = new JSONArray();
		JSONArray appGroupInfos = getBusinessFunctionAppGroups(new JSONArray(bFunctionsRemoved));
		int length = appGroupInfos == null ? 0 : appGroupInfos.length();
		for (int i = 0; i < length; i++) {
			JSONObject appGroupInfo = appGroupInfos.getJSONObject(i);
			appKeys.put(appGroupInfo.get(BusinessFunctionApplications.APPGROUP_ID));
		}
		if (appKeys.length() > 0) {
			misallocateUserPermission(userId, appKeys);
		}
	}

	private static void misallocateUserPermission(Object userId, JSONArray appGroupKeys) throws JSONException {
		Object organizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);

		JSONObject parameters = new JSONObject();
		parameters.put("userr__id_", userId);
		parameters.put("org__id_", organizationId);
		parameters.put("app__key__", appGroupKeys);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		query.put(Data.Query.FILTERS, UserPermissions.USER_ID + "={userr__id_} AND " + UserPermissions.ORGANIZATION_ID + "={org__id_} AND " + UserPermissions.APPGROUP_ID + "_key IN{app__key__} AND " + UserPermissions.DERIVED + "='" + UserPermissions.DerivedFrom.BUSINESS_FUNCTION + "'");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONObject update = new JSONObject();
		update.put(Updates.TYPE, Updates.Types.DELETE);
		update.put(Updates.QUERY, query);

		JSONObject deleteOperation = new JSONObject();
		deleteOperation.put(Data.Update.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
		deleteOperation.put(Data.Update.UPDATES, update);

		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(deleteOperation);
	}

	private static void allocatePermissionsToExistingEmployee(Record record) throws JSONException, ResourceNotFoundException, AppGroupNotFoundException, UserNotFoundException {
		String emailid = (String) record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		Object reportingToEmployeeId = record.getValue(HrisKinds.Employess.REPORTING_TO);
		Object reportingToId = getUserId(reportingToEmployeeId);

		Object ProfiteCenterAlias = record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID_ALIAS);
		JSONArray profitCenters = (JSONArray) (ProfiteCenterAlias == null ? record.getValue(HrisKinds.Employess.PROFIT_CENTER_ID) : ProfiteCenterAlias);
		JSONArray businessFunctions = (JSONArray) record.getValue(HrisKinds.Employess.BUSINESS_FUNCTION_ID);
		boolean highPrivilege = DatabaseEngineUtility.getBooleanValue(record.getValue(HrisKinds.Employess.HIGH_PRIVILEGE));
		String whiteListedIpAddresses = (String) record.getValue(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS);
		Object userId = UserCacheHandler.addUserIfNotExist(emailid, null);
		if (businessFunctions != null && businessFunctions.length() > 0) {
			JSONArray businessFunctionAppGroups = getBusinessFunctionAppGroups(businessFunctions);
			LogUtility.writeLog("Detail of App Group " + businessFunctionAppGroups);
			allocatePermissions(userId, businessFunctionAppGroups, profitCenters, reportingToId, whiteListedIpAddresses, highPrivilege);
		}
		allocateUnviversalPermissions(userId, profitCenters, reportingToId, whiteListedIpAddresses, highPrivilege);
	}

	private static Set<Long> excerciseBusinessFunctions(Record record, Set<Long> bFunctionAdded, Set<Long> bFunctionRemoved) throws JSONException {
		Set<Long> previousBF = convertToSet((JSONArray) record.getOldValue(HrisKinds.Employess.BUSINESS_FUNCTION_ID));
		Set<Long> existingBF = convertToSet((JSONArray) record.getNewValue(HrisKinds.Employess.BUSINESS_FUNCTION_ID));

		bFunctionRemoved.addAll(previousBF);
		bFunctionRemoved.removeAll(existingBF);

		bFunctionAdded.addAll(existingBF);
		bFunctionAdded.removeAll(previousBF);

		return existingBF;
	}

	private static Set<Long> convertToSet(JSONArray manyToMany) throws NumberFormatException, JSONException {
		Set<Long> set = new HashSet<Long>();
		int length = manyToMany == null ? 0 : manyToMany.length();
		for (int i = 0; i < length; i++) {
			set.add(Long.valueOf(manyToMany.get(i).toString()));
		}
		return set;
	}

	/*********************************************************** Common Methods ********************************************************/
	private static JSONArray getBusinessFunctionAppGroups(Object bfKey) throws JSONException {
		JSONArray appGroups = null;
		JSONArray result = getBusinesFunctionsDetail(bfKey);
		int length = result.length();
		if (length == 0) {
			throw new BusinessLogicException("No business function found having key in " + bfKey);
		} else {
			appGroups = new JSONArray();
			for (int i = 0; i < length; i++) {
				JSONObject bfDetails = result.getJSONObject(i);
				if (!bfDetails.isNull(BusinessFunctions.APPGROUP_ID)) {
					Object appGroupId = bfDetails.get(BusinessFunctions.APPGROUP_ID);
					JSONObject appParameters = getAppParameters(appGroupId);
					bfDetails.put(AppsStudio.Kind.APP_PARAMETERS, appParameters);
					appGroups.put(bfDetails);
				} else if (!bfDetails.isNull(BusinessFunctions.PARENT_BUSINESS_FUNCTION)) {
					Object parentBusinessFunction = bfDetails.get(BusinessFunctions.PARENT_BUSINESS_FUNCTION);
					JSONArray appGrps = getBusinessFunctionAppGroups(parentBusinessFunction);
					int appsCount = appGrps == null ? 0 : appGrps.length();
					for (int j = 0; j < appsCount; j++) {
						appGroups.put(appGrps.get(j));
					}
				}
			}
		}
		return appGroups;
	}

	private static JSONArray getBusinesFunctionsDetail(Object bfKey) throws JSONException {
		JSONObject query = null;
		try {
			boolean useIn = bfKey instanceof JSONArray ? true : false;
			if (useIn && ((JSONArray) bfKey).length() == 0) {
				((JSONArray) bfKey).put(-1);
			}

			JSONObject parameters = new JSONObject();
			parameters.put("key___", bfKey);

			JSONArray requiredColumns = new JSONArray();
			requiredColumns.put(BusinessFunctions.APPGROUP_ID);
			requiredColumns.put(BusinessFunctions.InterInstanceColumns.LABEL);
			requiredColumns.put(BusinessFunctions.PARENT_BUSINESS_FUNCTION);

			query = new JSONObject();
			query.put(Data.Query.RESOURCE, AFBConstants.BUSINESS_FUNCTIONS);
			query.put(Data.Query.COLUMNS, requiredColumns);
			query.put(Data.Query.FILTERS, Data.Query.KEY + (useIn ? " IN" : " =") + "{key___}");
			query.put(Data.Query.PARAMETERS, parameters);

			JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AFBConstants.BUSINESS_FUNCTIONS);
			return result;
		} catch (Exception e) {
			String exceptionTraceMessage = ExceptionUtils.getExceptionTraceMessage(AfbUserService.class.getName(), e);
			try {
				if (SystemProperty.applicationId.get().equals(Server.SIS)) {
					ApplaneMail applaneMail = new ApplaneMail();
					applaneMail.setMessage("Error on Sis", query.toString(), false);
					applaneMail.setTo("munish.kumar@daffodilsw.com");
					applaneMail.sendAsAdmin();
				}
			} catch (Exception e1) {
			}
			throw new BusinessLogicException(exceptionTraceMessage);
		}
	}

	@SuppressWarnings("unused")
	private static Object getParentBusinessFunction(Object bfKey) throws JSONException {
		JSONObject parameters = new JSONObject();
		parameters.put("bf__", bfKey);

		JSONArray requiredColumns = new JSONArray();
		requiredColumns.put(AFBConstants.BusinessFunctions.PARENT_BUSINESS_FUNCTION);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AFBConstants.BUSINESS_FUNCTIONS);
		query.put(Data.Query.COLUMNS, requiredColumns);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "={bf__}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AFBConstants.BUSINESS_FUNCTIONS);
		return result.length() == 0 ? null : result.getJSONObject(0).get(Data.Query.KEY);
	}

	private static JSONObject getAppParameters(Object appGroupId) throws JSONException {
		JSONObject parameters = new JSONObject();
		parameters.put("app__group__id___", appGroupId);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AppsStudio.Kind.APP_PARAMETERS);
		query.put(Data.Query.FILTERS, AppParameters.APPGROUP_ID + "={app__group__id___} AND " + AppParameters.PARAMETER_NAME + "='" + PROFIT_CENTER + "'");
		query.put(Data.Query.PARAMETERS, parameters);
		query.put(Data.Query.DATABASE_NAME, Server.SANDBOX_APPLANE_DATABASE);
		query.put(Data.Query.INSTANCE_NAME, Server.SANDBOX_INSTANCE_NAME);
		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AppsStudio.Kind.APP_PARAMETERS);
		return result.length() == 0 ? null : result.getJSONObject(0);
	}

	// private static JSONArray getPermissionRows(Object userId, Object appGroupKey, boolean applyBFCheck) throws JSONException {
	// Object organizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
	// boolean applyIn = appGroupKey instanceof JSONArray;
	//
	// JSONObject parameters = new JSONObject();
	// parameters.put("app__group__key", appGroupKey);
	// parameters.put("user__id__", userId);
	// parameters.put("or__ganID", organizationId);
	//
	// JSONArray columns = new JSONArray();
	// columns.put(Data.Query.KEY);
	//
	// StringBuilder filter = new StringBuilder(UserPermissions.USER_ID).append("={user__id__}");
	// filter.append(" AND ");
	// filter.append(UserPermissions.ORGANIZATION_ID).append("={or__ganID}");
	// filter.append(" AND ");
	// filter.append(UserPermissions.APPGROUP_ID).append("_key" + (applyIn ? " IN " : "=") + "{app__group__key}");
	// if (applyBFCheck) {
	// filter.append(" AND ");
	// filter.append(UserPermissions.DERIVED).append("='").append(UserPermissions.DerivedFrom.BUSINESS_FUNCTION).append("'");
	// }
	//
	// JSONObject query = new JSONObject();
	// query.put(Data.Query.RESOURCE, UserPermission.Kind.USERPERMISSIONS);
	// query.put(Data.Query.COLUMNS, columns);
	// query.put(Data.Query.FILTERS, filter.toString());
	// query.put(Data.Query.PARAMETERS, parameters);
	//
	// JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(UserPermission.Kind.USERPERMISSIONS);
	// JSONArray keys = new JSONArray();
	// for (int i = 0; i < result.length(); i++) {
	// keys.put(result.getJSONObject(i).get(Data.Query.KEY));
	// }
	// return keys;
	// }

	public static void main(String[] args) throws IOException {
		String encode = URLEncoder.encode("AIzaSyCwfBe3U-vYWhGfM1CpjEnVOwZh-PpU8Ps", "UTF-8");
		String url = "https://www.googleapis.com/latitude/v1/currentLocation?key=" + encode;
		System.out.println(encode);
		String requestToRemoteService = ApplaneURLFetch.requestToRemoteService(url);
		System.out.println(requestToRemoteService);
	}

}
