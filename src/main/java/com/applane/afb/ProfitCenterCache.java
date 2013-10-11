package com.applane.afb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneCache;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.DatabaseEngineUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class ProfitCenterCache {

	public static boolean isOrganizationLevel(long profitCenterId) throws JSONException {
		JSONObject profitCenter = getProfitCenter(profitCenterId);
		if (profitCenter != null) {
			return profitCenter.isNull(AFBConstants.OrganizationProfitCenter.ORGANIZATION_LEVEL) ? false : DatabaseEngineUtility.getBooleanValue(profitCenter.get(AFBConstants.OrganizationProfitCenter.ORGANIZATION_LEVEL));
		}
		return false;
	}

	public boolean isOrganizationLevel(String name, JSONArray value) throws JSONException {
		if (name.equals(AFBConstants.AfbUserService.PROFIT_CENTER)) {
			int length = value.length();
			for (int i = 0; i < length; i++) {
				long profitCenterId = value.getLong(i);
				if (isOrganizationLevel(profitCenterId)) {
					return true;
				}
			}
		}
		return false;
	}

	public static JSONObject getProfitCenter(Object profitCenterId) throws JSONException {
		String cacheKey = "ProfitCenterCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + profitCenterId;
		if (ApplaneCache.containsKey(cacheKey)) {
			JSONObject profitCenter = ApplaneCache.getJSONObject(cacheKey);
			return profitCenter;
		}
		JSONObject parameters = new JSONObject();
		parameters.put("org__n_ame_", profitCenterId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AFBConstants.ORG_PROFIT_CENTER);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "={org__n_ame_}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AFBConstants.ORG_PROFIT_CENTER);
		if (result.length() > 0) {
			JSONObject row = result.getJSONObject(0);
			ApplaneCache.putJSONObject(cacheKey, row);
			return row;
		}
		return null;
	}

	public static Object getProfitCenterKey(Object profitCenterName) throws JSONException {
		String cacheKey = "ProfitCenterNameCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + profitCenterName;
		if (ApplaneCache.containsKey(cacheKey)) {
			return ApplaneCache.get(cacheKey);
		}
		JSONObject parameters = new JSONObject();
		parameters.put("org__n_ame_", profitCenterName);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AFBConstants.ORG_PROFIT_CENTER);
		query.put(Data.Query.FILTERS, AFBConstants.OrganizationProfitCenter.NAME + "={org__n_ame_}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AFBConstants.ORG_PROFIT_CENTER);
		if (result.length() > 0) {
			Object profitCenterKey = result.getJSONObject(0).get(Data.Query.KEY);
			ApplaneCache.put(cacheKey, profitCenterKey);
			return profitCenterKey;
		}
		return null;
	}

	public static void clearCache(Object profitCenterId, String profitCenterName) {
		String cacheKey = "ProfitCenterCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + profitCenterId;
		if (ApplaneCache.containsKey(cacheKey)) {
			ApplaneCache.remove(cacheKey);
		}
		cacheKey = "OrganizationProfitCenterCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
		if (ApplaneCache.containsKey(cacheKey)) {
			ApplaneCache.remove(cacheKey);
		}
		cacheKey = "ProfitCenterNameCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + profitCenterName;
		if (ApplaneCache.containsKey(cacheKey)) {
			ApplaneCache.remove(cacheKey);
		}
	}

	public boolean avoidProfitCenterParameter(String name) throws JSONException {
		Object currentVariable = CurrentState.getCurrentVariable(name);
		long id = Long.valueOf(currentVariable.toString());
		return (name.equals(AFBConstants.AfbUserService.PROFIT_CENTER) && ProfitCenterCache.isOrganizationLevel(id));
	}
}
