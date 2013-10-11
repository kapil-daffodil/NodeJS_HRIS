package com.applane.resourceoriented.hris.tour;

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

public class TourLimits {

	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public JSONArray employeeTourLimits(Object employeeid, Object cityid) {
		try {
			Object employeeId;
			if ((employeeid instanceof JSONObject)) {
				employeeId = ((JSONObject) employeeid).get(Data.Query.KEY);
			} else {
				employeeId = employeeid;
			}
			Object cityId;
			if ((cityid instanceof JSONObject)) {
				cityId = ((JSONObject) cityid).get(Data.Query.KEY);
			} else {
				cityId = cityid;
			}
			JSONObject cityGradeQuery = new JSONObject();
			cityGradeQuery.put(Data.Query.RESOURCE, "hris_city__citygradeid");
			JSONArray cityArray = new JSONArray();
			cityArray.put("citygradeid");
			cityGradeQuery.put(Data.Query.COLUMNS, cityArray);
			cityGradeQuery.put(Data.Query.FILTERS, "hris_city = " + cityId);
			JSONObject cityGradeObject;
			cityGradeObject = ResourceEngine.query(cityGradeQuery);
			JSONArray cityGradeArray = cityGradeObject.getJSONArray("hris_city__citygradeid");

			int cityGradeArrayCount = cityGradeArray == null ? 0 : cityGradeArray.length();
			if (cityGradeArrayCount > 0) {
				Object cityGradeId = cityGradeArray.getJSONObject(0).opt("citygradeid") == null ? "" : cityGradeArray.getJSONObject(0).opt("citygradeid");

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
								// get expenselimitid from hris_expenselimits__employeegradeid where employeegradeid is above

								JSONObject expenseLimitQuery = new JSONObject();
								expenseLimitQuery.put(Data.Query.RESOURCE, "hris_expenselimits");
								JSONArray expenseLimitColumnArray = new JSONArray();
								expenseLimitColumnArray.put("__key__");
								expenseLimitColumnArray.put("tourparticularid");
								expenseLimitColumnArray.put("tourparticularid.name");
								JSONObject amountColumnJson = new JSONObject();
								amountColumnJson.put("expression", "amount");
								amountColumnJson.put("type", "currency");
								expenseLimitColumnArray.put(amountColumnJson);
								expenseLimitQuery.put(Data.Query.COLUMNS, expenseLimitColumnArray);
								expenseLimitQuery.put(Data.Query.FILTERS, "employeegradeid = " + employeeGradeId + " and citygradeid = " + cityGradeId);
								JSONObject expenseLimitObject;
								expenseLimitObject = ResourceEngine.query(expenseLimitQuery);
								JSONArray expenseLimitArray = expenseLimitObject.getJSONArray("hris_expenselimits");

								int expenseLimitArrayCount = expenseLimitArray == null ? 0 : expenseLimitArray.length();
								JSONArray categoryTypeArray = new JSONArray();

								if (expenseLimitArrayCount > 0) {
									for (int counter = 0; counter < expenseLimitArrayCount; counter++) {
										// Object expenseLimitId = expenseLimitArray.getJSONObject(counter).opt("__key__");
										JSONObject amountObject = (JSONObject) expenseLimitArray.getJSONObject(counter).opt("amount");
										// Double amount = (Double) amountObject.opt("amount");
										// JSONObject amountTypeObject = (JSONObject) amountObject.opt("type");
										// String amountType = (String) amountTypeObject.opt("currency");
										Object tourParticularId = expenseLimitArray.getJSONObject(counter).opt("tourparticularid");
										Object tourParticular = expenseLimitArray.getJSONObject(counter).opt("tourparticularid.name");

										JSONObject typeobject = new JSONObject();
										JSONObject tourParticularObject = new JSONObject();
										tourParticularObject.put("__key__", tourParticularId);
										tourParticularObject.put("name", tourParticular);
										typeobject.put("tourparticularid", tourParticularObject);
										typeobject.put("maxamount", amountObject);

										categoryTypeArray.put(counter, typeobject);
									}
								}
								return categoryTypeArray;
							}
						}
					}
				}
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error while generating tour limits.." + e.getMessage());
		}
	}

	public JSONArray lodgingExpenseLimit(Object employeeid, Object cityid) {
		try {
			// GET EMPLOYEEID FROM KEY
			Object employeeId;
			if ((employeeid instanceof JSONObject)) {
				employeeId = ((JSONObject) employeeid).get(Data.Query.KEY);
			} else {
				employeeId = employeeid;
			}

			Object cityId;
			if ((cityid instanceof JSONObject)) {
				cityId = ((JSONObject) cityid).get(Data.Query.KEY);
			} else {
				cityId = cityid;
			}

			// get Citygradeid from hris_city__citygradeid
			JSONObject cityGradeQuery = new JSONObject();
			cityGradeQuery.put(Data.Query.RESOURCE, "hris_city__citygradeid");
			JSONArray cityArray = new JSONArray();
			cityArray.put("citygradeid");
			cityGradeQuery.put(Data.Query.COLUMNS, cityArray);
			cityGradeQuery.put(Data.Query.FILTERS, "hris_city = " + cityId);
			JSONObject cityGradeObject;
			cityGradeObject = ResourceEngine.query(cityGradeQuery);
			JSONArray cityGradeArray = cityGradeObject.getJSONArray("hris_city__citygradeid");
			int cityGradeArrayCount = cityGradeArray == null ? 0 : cityGradeArray.length();
			if (cityGradeArrayCount > 0) {
				Object cityGradeId = cityGradeArray.getJSONObject(0).opt("citygradeid") == null ? "" : cityGradeArray.getJSONObject(0).opt("citygradeid");
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
								// get expenselimitid from hris_expenselimits__employeegradeid where employeegradeid is above

								JSONObject expenseLimitQuery = new JSONObject();
								expenseLimitQuery.put(Data.Query.RESOURCE, "hris_lodgingexpenselimits");
								JSONArray expenseLimitColumnArray = new JSONArray();
								expenseLimitColumnArray.put("__key__");
								expenseLimitColumnArray.put("lodgingexpenseid");
								expenseLimitColumnArray.put("lodgingexpenseid.name");
								JSONObject amountColumnJson = new JSONObject();
								amountColumnJson.put("expression", "amount");
								amountColumnJson.put("type", "currency");
								expenseLimitColumnArray.put(amountColumnJson);
								expenseLimitQuery.put(Data.Query.COLUMNS, expenseLimitColumnArray);
								expenseLimitQuery.put(Data.Query.FILTERS, "employeegradeid = " + employeeGradeId + " and citygradeid = " + cityGradeId);
								JSONObject expenseLimitObject;
								expenseLimitObject = ResourceEngine.query(expenseLimitQuery);
								JSONArray expenseLimitArray = expenseLimitObject.getJSONArray("hris_lodgingexpenselimits");

								int expenseLimitArrayCount = expenseLimitArray == null ? 0 : expenseLimitArray.length();
								JSONArray categoryTypeArray = new JSONArray();

								if (expenseLimitArrayCount > 0) {
									for (int counter = 0; counter < expenseLimitArrayCount; counter++) {
										// Object lodgingExpenseLimitId = expenseLimitArray.getJSONObject(counter).opt("__key__");
										JSONObject amountObject = (JSONObject) expenseLimitArray.getJSONObject(counter).opt("amount");
										// Double amount = (Double) amountObject.opt("amount");
										// JSONObject amountTypeObject = (JSONObject) amountObject.opt("type");
										// String amountType = (String) amountTypeObject.opt("currency");
										Object lodgingExpenseId = expenseLimitArray.getJSONObject(counter).opt("lodgingexpenseid");
										Object lodgingExpenseName = expenseLimitArray.getJSONObject(counter).opt("lodgingexpenseid.name");

										JSONObject typeobject = new JSONObject();
										JSONObject lodgingExpenseObject = new JSONObject();
										lodgingExpenseObject.put("__key__", lodgingExpenseId);
										lodgingExpenseObject.put("name", lodgingExpenseName);
										typeobject.put("lodgingexpenseid", lodgingExpenseObject);
										typeobject.put("maxamount", amountObject);

										categoryTypeArray.put(counter, typeobject);
									}
								}
								return categoryTypeArray;
							}
						}
					}
				}
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error while generating lodging expense limits.." + e.getMessage());
		}
	}

}
