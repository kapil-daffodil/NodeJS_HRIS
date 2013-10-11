package com.applane.afb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneCache;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class LocationCache {

	public static JSONObject getLocation(Object locationId) throws JSONException {
		String cacheKey = "LocationCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + locationId;
		if (ApplaneCache.containsKey(cacheKey)) {
			return ApplaneCache.getJSONObject(cacheKey);
		}
		JSONObject parameters = new JSONObject();
		parameters.put("cache_locationId__", locationId);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AFBConstants.ORGANIZATION_OFFICES);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "={cache_locationId__}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AFBConstants.ORGANIZATION_OFFICES);
		if (result.length() > 0) {
			JSONObject row = result.getJSONObject(0);
			ApplaneCache.putJSONObject(cacheKey, row);
			return row;
		}
		return null;
	}

	public static Object getLocationKey(String location) throws JSONException {
		String cacheKey = "LocationNameCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + location;
		if (ApplaneCache.containsKey(cacheKey)) {
			return ApplaneCache.get(cacheKey);
		}
		JSONObject parameters = new JSONObject();
		parameters.put("cache_locationId__", location);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, AFBConstants.ORGANIZATION_OFFICES);
		query.put(Data.Query.FILTERS, AFBConstants.OrganizationOffices.NAME + "={cache_locationId__}");
		query.put(Data.Query.PARAMETERS, parameters);

		JSONArray result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(AFBConstants.ORGANIZATION_OFFICES);
		if (result.length() > 0) {
			Object locationKey = result.getJSONObject(0).get(Data.Query.KEY);
			ApplaneCache.put(cacheKey, locationKey);
			return locationKey;
		}
		return null;
	}

	public static void clearCache(Object locationId, String locationName) {
		String cacheKey = "LocationCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + locationId;
		if (ApplaneCache.containsKey(cacheKey)) {
			ApplaneCache.remove(cacheKey);
		}
		cacheKey = "LocationNameCache_" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + "_" + locationName;
		if (ApplaneCache.containsKey(cacheKey)) {
			ApplaneCache.remove(cacheKey);
		}
	}

}
