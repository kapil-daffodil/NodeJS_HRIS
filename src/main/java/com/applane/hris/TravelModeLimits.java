package com.applane.hris;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;

/**
 * @author Ajay Pal Singh
 * @version 1.1
 * @category HRIS businesslogic
 */

public class TravelModeLimits {

	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public JSONArray employeeTravelModeLimits(Object employeeid) {
		try {
			// GET EMPLOYEEID FROM KEY
			Object employeeId;
			if ((employeeid instanceof JSONObject)) {
				employeeId = ((JSONObject) employeeid).get(Data.Query.KEY);
			} else {
				employeeId = employeeid;
			}
			// get the designation of employee
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray array = new JSONArray();
			array.put("designationid");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__= " + employeeId);
			JSONObject designationObject;
			designationObject = ResourceEngine.query(query);
			JSONArray designationArray = designationObject.getJSONArray("hris_employees");
			int designationArrayCount = designationArray == null ? 0 : designationArray.length();
			if (designationArrayCount > 0) {
				Object designationId = designationArray.getJSONObject(0).opt("designationid");
				if (designationId != null) {
					// get employeegradeid from hris_designations__employeegradeid where designation is above
					JSONObject gradeQuery = new JSONObject();
					gradeQuery.put(Data.Query.RESOURCE, "hris_designations__employeegradeid");
					JSONArray gradeColumnArray = new JSONArray();
					gradeColumnArray.put("employeegradeid");
					gradeQuery.put(Data.Query.COLUMNS, gradeColumnArray);
					gradeQuery.put(Data.Query.FILTERS, "hris_designations= " + designationId);
					JSONObject gradeObject;
					gradeObject = ResourceEngine.query(gradeQuery);
					JSONArray gradeArray = gradeObject.getJSONArray("hris_designations__employeegradeid");
					int gradeArrayCount = gradeArray == null ? 0 : gradeArray.length();
					if (gradeArrayCount > 0) {
						Object employeeGradeId = gradeArray.getJSONObject(0).opt("employeegradeid");
						if (employeeGradeId != null) {
							// get TRAVELexpenselimitid from hris_travelexpenselimits__employeegradeid where employeegradeid is above
							JSONObject expenseLimitQuery = new JSONObject();
							expenseLimitQuery.put(Data.Query.RESOURCE, "hris_travelexpenselimits");
							JSONArray expenseLimitColumnArray = new JSONArray();
							expenseLimitColumnArray.put("__key__");
							expenseLimitColumnArray.put("travelmodeid");
							expenseLimitColumnArray.put("travelmodeid.name");
							expenseLimitQuery.put(Data.Query.COLUMNS, expenseLimitColumnArray);
							expenseLimitQuery.put(Data.Query.FILTERS, "employeegradeid = " + employeeGradeId);
							JSONObject expenseLimitObject;
							expenseLimitObject = ResourceEngine.query(expenseLimitQuery);
							JSONArray expenseLimitArray = expenseLimitObject.getJSONArray("hris_travelexpenselimits");
							int expenseLimitArrayCount = expenseLimitArray == null ? 0 : expenseLimitArray.length();
							JSONArray categoryTypeArray = new JSONArray();
							if (expenseLimitArrayCount > 0) {
								for (int counter = 0; counter < expenseLimitArrayCount; counter++) {
									// Object travelExpenseLimitId = expenseLimitArray.getJSONObject(counter).opt("__key__");
									Object travelmodeId = expenseLimitArray.getJSONObject(counter).opt("travelmodeid");
									Object travelMode = expenseLimitArray.getJSONObject(counter).opt("travelmodeid.name");

									JSONObject typeobject = new JSONObject();
									JSONObject travelModeIdObject = new JSONObject();
									travelModeIdObject.put("__key__", travelmodeId);
									travelModeIdObject.put("name", travelMode);
									typeobject.put("travelmodeid", travelModeIdObject);
									categoryTypeArray.put(counter, typeobject);
								}
							}
							return categoryTypeArray;
						}
					}
				}
			}
			return null;
		} catch (Exception e) {
			throw new BusinessLogicException("Error while generating leave balance report" + e.getMessage());
		}
	}
}
