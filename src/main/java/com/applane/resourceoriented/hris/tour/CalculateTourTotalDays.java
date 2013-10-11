package com.applane.resourceoriented.hris.tour;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class CalculateTourTotalDays {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public long calculateTotaldays(Object startdate, Object enddate) {
		Date departDate = null;
		Date arriveDate = null;
		if (startdate != null && enddate != null) {
			try {
				departDate = Translator.dateValue(startdate);
				arriveDate = Translator.dateValue(enddate);
				Long dateDiff = DataTypeUtilities.differenceBetweenDates(departDate, arriveDate) + 1;
				return dateDiff;
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Error while generating total days." + e.getMessage());
			}
		}

		return 0;
	}

	public long calculateDays(Object fromdate, Object todate) {
		Date tourFromDate = null;
		Date tourToDate = null;

		if (fromdate != null && todate != null) {
			try {
				tourFromDate = Translator.dateValue(fromdate);
				tourToDate = Translator.dateValue(todate);
				long dateDiff = DataTypeUtilities.differenceBetweenDates(tourFromDate, tourToDate) + 1;
				// Long dateDiff = CommunicationUtility.differenceBetweenDates(tourFromDate, tourToDate) + 1;
				return dateDiff;
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Some unexpected error occured while calculating total no of days." + e.getMessage());
			}
		}
		return 0;
	}

	public Object getTourExpectedAmountLodging(Object tourId, Object cityCountryId, Object employeeIdObject, Object numberofdays, Object tourType) {
		LogUtility.writeLog("Lodging << tourId >> " + tourId + " << cityid >> " + cityCountryId + " << employeeId >> " + employeeIdObject + " << numberofdays >> " + numberofdays);
		if (cityCountryId != null && employeeIdObject != null && numberofdays != null && Translator.doubleValue(numberofdays) > 0.0) {
			double amount = 0.0;
			try {
				int expenseType = HRISApplicationConstants.LodgingComponents.LODGING;
				Object employeeId = null;
				if (employeeIdObject instanceof JSONArray) {
					employeeId = ((JSONArray) employeeIdObject).getJSONObject(0).opt(Updates.KEY);
				} else if (employeeIdObject instanceof JSONObject) {
					employeeId = ((JSONObject) employeeIdObject).opt(Updates.KEY);
				} else {
					employeeId = employeeIdObject;
				}
				String filter = "employeeid = " + employeeId;
				JSONArray employeeArray = getEmployeeRecord(ResourceEngine, filter);
				if (employeeArray != null && employeeArray.length() > 0) {
					Object employeeGradeId = employeeArray.getJSONObject(0).opt("employee_grade_id");
					Object cityCountryGradeId = null;
					if (("" + tourType).equals("Domestic")) {
						cityCountryGradeId = getCityGradeID(cityCountryId);
					} else if (("" + tourType).equals("Foreign")) {
						cityCountryGradeId = getCountryGradeID(cityCountryId);
					}
					JSONArray lodgingArray = getLodgingArray(ResourceEngine, cityCountryGradeId, employeeGradeId, expenseType, tourType);

					if (lodgingArray != null && lodgingArray.length() > 0) {
						Double lodgingExpectedAmount = 0.0;
						JSONObject lodgingAmountObject;
						lodgingAmountObject = (JSONObject) lodgingArray.getJSONObject(0).opt("amount");
						Double lodgingAmount = (Double) lodgingAmountObject.opt("amount");

						lodgingExpectedAmount = lodgingAmount * Translator.doubleValue(numberofdays);

						amount = amount + lodgingExpectedAmount;
					}
				}
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				throw new RuntimeException("Some Error Occured. Trace :- [" + trace + "]");
			}
			return amount;
		}
		return null;
	}

	public Object getTourExpectedAmountFooding(Object tourId, Object cityCountryId, Object employeeIdObject, Object numberofdays, Object tourType) {
		LogUtility.writeLog("Fooding << tourId >> " + tourId + " << cityid >> " + cityCountryId + " << employeeId >> " + employeeIdObject + " << numberofdays >> " + numberofdays);
		if (cityCountryId != null && employeeIdObject != null && numberofdays != null && Translator.doubleValue(numberofdays) > 0.0) {
			double amount = 0.0;
			try {
				int expenseType = HRISApplicationConstants.LodgingComponents.FOODING;
				Object employeeId = null;
				if (employeeIdObject instanceof JSONArray) {
					employeeId = ((JSONArray) employeeIdObject).getJSONObject(0).opt(Updates.KEY);
				} else if (employeeIdObject instanceof JSONObject) {
					employeeId = ((JSONObject) employeeIdObject).opt(Updates.KEY);
				} else {
					employeeId = employeeIdObject;
				}
				String filter = "employeeid = " + employeeId;
				JSONArray employeeArray = getEmployeeRecord(ResourceEngine, filter);
				if (employeeArray != null && employeeArray.length() > 0) {
					Object employeeGradeId = employeeArray.getJSONObject(0).opt("employee_grade_id");
					Object cityCountryGradeId = null;
					if (("" + tourType).equals("Domestic")) {
						cityCountryGradeId = getCityGradeID(cityCountryId);
					} else if (("" + tourType).equals("Foreign")) {
						cityCountryGradeId = getCountryGradeID(cityCountryId);
					}
					JSONArray lodgingArray = getLodgingArray(ResourceEngine, cityCountryGradeId, employeeGradeId, expenseType, tourType);

					if (lodgingArray != null && lodgingArray.length() > 0) {
						Double lodgingExpectedAmount = 0.0;
						JSONObject lodgingAmountObject;
						lodgingAmountObject = (JSONObject) lodgingArray.getJSONObject(0).opt("amount");
						Double lodgingAmount = (Double) lodgingAmountObject.opt("amount");

						lodgingExpectedAmount = lodgingAmount * Translator.doubleValue(numberofdays);

						amount = amount + lodgingExpectedAmount;
					}
				}
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				throw new RuntimeException("Some Error Occured. Trace :- [" + trace + "]");
			}
			return amount;
		}
		return null;
	}

	public Object getTourExpectedAmountHotel(Object tourId, Object cityCountryId, Object employeeIdObject, Object numberofdays, Object tourType) {
		LogUtility.writeLog("Hotel << tourId >> " + tourId + " << cityid >> " + cityCountryId + " << employeeId >> " + employeeIdObject + " << numberofdays >> " + numberofdays);
		if (cityCountryId != null && employeeIdObject != null && numberofdays != null && Translator.doubleValue(numberofdays) > 0.0) {
			double amount = 0.0;
			try {
				int expenseType = HRISApplicationConstants.LodgingComponents.HOTEL;
				Object employeeId = null;
				if (employeeIdObject instanceof JSONArray) {
					employeeId = ((JSONArray) employeeIdObject).getJSONObject(0).opt(Updates.KEY);
				} else if (employeeIdObject instanceof JSONObject) {
					employeeId = ((JSONObject) employeeIdObject).opt(Updates.KEY);
				} else {
					employeeId = employeeIdObject;
				}
				String filter = "employeeid = " + employeeId;
				JSONArray employeeArray = getEmployeeRecord(ResourceEngine, filter);
				if (employeeArray != null && employeeArray.length() > 0) {
					Object employeeGradeId = employeeArray.getJSONObject(0).opt("employee_grade_id");
					Object cityCountryGradeId = null;
					if (("" + tourType).equals("Domestic")) {
						cityCountryGradeId = getCityGradeID(cityCountryId);
					} else if (("" + tourType).equals("Foreign")) {
						cityCountryGradeId = getCountryGradeID(cityCountryId);
					}
					JSONArray lodgingArray = getLodgingArray(ResourceEngine, cityCountryGradeId, employeeGradeId, expenseType, tourType);

					if (lodgingArray != null && lodgingArray.length() > 0) {
						Double lodgingExpectedAmount = 0.0;
						JSONObject lodgingAmountObject;
						lodgingAmountObject = (JSONObject) lodgingArray.getJSONObject(0).opt("amount");
						Double lodgingAmount = (Double) lodgingAmountObject.opt("amount");

						lodgingExpectedAmount = lodgingAmount * Translator.doubleValue(numberofdays);

						amount = amount + lodgingExpectedAmount;
					}
				}
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				throw new RuntimeException("Some Error Occured. Trace :- [" + trace + "]");
			}
			return amount;
		}
		return null;
	}

	public Object getTourExpectedAmountTravel(Object cityid, Object travelTypeId, Object employeeIdObject, Object tourType) {
		LogUtility.writeLog("Travel << cityid >> " + cityid + " << employeeId >> " + employeeIdObject + " << travelTypeId >> " + travelTypeId);
		if (cityid != null && employeeIdObject != null && travelTypeId != null) {

			double amount = 0.0;
			try {
				Object employeeId = null;
				if (employeeIdObject instanceof JSONArray) {
					employeeId = ((JSONArray) employeeIdObject).getJSONObject(0).opt(Updates.KEY);
				} else if (employeeIdObject instanceof JSONObject) {
					employeeId = ((JSONObject) employeeIdObject).opt(Updates.KEY);
				} else {
					employeeId = employeeIdObject;
				}
				String filter = "employeeid = " + employeeId;
				JSONArray employeeArray = getEmployeeRecord(ResourceEngine, filter);
				if (employeeArray != null && employeeArray.length() > 0) {
					Object employeeGradeId = employeeArray.getJSONObject(0).opt("employee_grade_id");
					Object cityGradeId = getCityGradeID(cityid);
					JSONArray limitGradeArray = getExpenseLimit(ResourceEngine, cityGradeId, employeeGradeId, travelTypeId, tourType);

					if (limitGradeArray != null && limitGradeArray.length() > 0) {
						JSONObject lodgingAmountObject;
						lodgingAmountObject = (JSONObject) limitGradeArray.getJSONObject(0).opt("amount");
						Double lodgingAmount = (Double) lodgingAmountObject.opt("amount");
						amount = amount + lodgingAmount;
					}
				}
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				throw new RuntimeException("Some Error Occured. Trace :- [" + trace + "]");
			}
			return amount;
		}
		return null;
	}

	public JSONArray getTravelModes(Object employeeIdObject, Object travelTypeId, Object tourType) {
		try {
			LogUtility.writeLog("Travel Modes employeeId >> " + employeeIdObject + " << travelTypeId >> " + travelTypeId + " << tourType >> " + tourType);
			Object employeeId = null;
			if (employeeIdObject instanceof JSONArray) {
				employeeId = ((JSONArray) employeeIdObject).getJSONObject(0).opt(Updates.KEY);
			} else if (employeeIdObject instanceof JSONObject) {
				employeeId = ((JSONObject) employeeIdObject).opt(Updates.KEY);
			} else {
				employeeId = employeeIdObject;
			}
			String filter = "employeeid = " + employeeId;
			JSONArray employeeArray = getEmployeeRecord(ResourceEngine, filter);
			if (employeeArray != null && employeeArray.length() > 0) {
				Object employeeGradeId = employeeArray.getJSONObject(0).opt("employee_grade_id");
				JSONArray travelModes = getTravelModesArray(travelTypeId, employeeGradeId, tourType);
				if (travelModes == null || travelModes.length() == 0) {
					travelModes.put(-1);
				}
				return travelModes;
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			throw new RuntimeException("Some Error Occured. Trace :- [" + trace + "]");
		}
		return null;
	}

	private JSONArray getTravelModesArray(Object travelTypeId, Object employeeGradeId, Object tourType) throws JSONException {
		JSONObject limitQuery = new JSONObject();
		limitQuery.put(Data.Query.RESOURCE, "hris_expenselimits");
		JSONArray limitArray = new JSONArray();
		limitArray.put("__key__");
		limitArray.put("travel_mode_id");
		limitArray.put("travel_mode_id.name");

		limitQuery.put(Data.Query.COLUMNS, limitArray);
		limitQuery.put(Data.Query.FILTERS, "employeegradeid = " + employeeGradeId + " and tour_type = '" + tourType + "' AND tourparticularid = " + travelTypeId);
		JSONObject object = ResourceEngine.query(limitQuery);
		JSONArray limitGradeArray = object.getJSONArray("hris_expenselimits");
		JSONArray travelModes = new JSONArray();
		for (int counter = 0; counter < limitGradeArray.length(); counter++) {
			Object travelModeId = limitGradeArray.getJSONObject(counter).opt("travel_mode_id");
			if (travelModeId != null) {
				travelModes.put(travelModeId);
			}
		}
		LogUtility.writeLog("travelModes >> " + travelModes);
		return travelModes;
	}

	public JSONObject getTourExpectedAmount(Object cityCountryId, Object numberofdays, Object parent_includelodging, Object checkin, Object checkout, Object tourType) {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		String CURRENT_USER = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		boolean includeLodging = Translator.booleanValue(parent_includelodging);
		try {
			Object cityCountryGradeId = null;
			if (("" + tourType).equals("Domestic")) {
				cityCountryGradeId = getCityGradeID(cityCountryId);
			} else if (("" + tourType).equals("Foreign")) {
				cityCountryGradeId = getCountryGradeID(cityCountryId);
			}

			String filter = "employeeid.officialemailid = '" + CURRENT_USER + "'";
			JSONArray employeeArray = getEmployeeRecord(ResourceEngine, filter);
			if (employeeArray != null && employeeArray.length() > 0) {
				Object employeeGradeId = employeeArray.getJSONObject(0).opt("employee_grade_id");
				// Object employeeGradeId = getEmployeeGradeId(designationId);
				JSONArray limitGradeArray = getExpenseLimit(ResourceEngine, cityCountryGradeId, employeeGradeId, null, tourType);
				int limitGradeArrayCount = limitGradeArray == null ? 0 : limitGradeArray.length();
				if (limitGradeArrayCount > 0) {
					Double particularsExpectedAmount = 0.0;
					Double totalExpectedAmount = 0.0;
					int amountTypeID = 0;
					String amountType = null;
					JSONObject amountObject;
					for (int counter = 0; counter < limitGradeArrayCount; counter++) {
						// String particularName = limitGradeArray.getJSONObject(counter).optString("tourparticularid.name");
						amountObject = (JSONObject) limitGradeArray.getJSONObject(counter).opt("amount");
						Double amount = (Double) amountObject.opt("amount");
						JSONObject amountTypeObject = (JSONObject) amountObject.opt("type");
						amountType = (String) amountTypeObject.opt("currency");
						amountTypeID = (Integer) amountTypeObject.opt("__key__");
						particularsExpectedAmount = particularsExpectedAmount + amount;
						totalExpectedAmount = particularsExpectedAmount * (Integer) (numberofdays == null ? 0 : numberofdays);
					}
					if (includeLodging) {

						int expenseType = HRISApplicationConstants.LodgingComponents.LODGING;
						JSONArray lodgingArray = getLodgingArray(ResourceEngine, cityCountryGradeId, employeeGradeId, expenseType, tourType);
						int lodgingArrayCount = lodgingArray == null ? 0 : lodgingArray.length();
						if (lodgingArrayCount > 0) {
							Double lodgingExpectedAmount = 0.0;
							// int lodgingAmountTypeID = 0;
							// String lodgingAmountType = null;
							JSONObject lodgingAmountObject;
							// String lodgingExpenseName = lodgingArray.getJSONObject(0).optString("lodgingexpenseid.name");
							lodgingAmountObject = (JSONObject) lodgingArray.getJSONObject(0).opt("amount");
							Double lodgingAmount = (Double) lodgingAmountObject.opt("amount");
							// JSONObject lodgingAmountTypeObject = (JSONObject) lodgingAmountObject.opt("type");
							// lodgingAmountType = (String) lodgingAmountTypeObject.opt("currency");
							// lodgingAmountTypeID = (Integer) lodgingAmountTypeObject.opt("__key__");
							long dateDiffernce = getDateDifference(checkin, checkout);
							lodgingExpectedAmount = lodgingAmount * dateDiffernce;
							totalExpectedAmount = totalExpectedAmount + lodgingExpectedAmount;
						}
					}
					JSONObject totalExpectedExpenseObject = new JSONObject();
					totalExpectedExpenseObject.put("amount", totalExpectedAmount);
					JSONObject typeObject = new JSONObject();
					typeObject.put("__key__", amountTypeID);
					typeObject.put("currency", amountType);
					totalExpectedExpenseObject.put("type", typeObject);
					return totalExpectedExpenseObject;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error while generating Total Expected Amount" + e.getMessage());
		}
		return null;
	}

	private JSONArray getLodgingArray(ApplaneDatabaseEngine ResourceEngine, Object cityCountryGradeId, Object employeeGradeId, int expenseType, Object tourType) throws JSONException {
		JSONObject lodgingQuery = new JSONObject();
		lodgingQuery.put(Data.Query.RESOURCE, "hris_lodgingexpenselimits");
		JSONArray columnArray = new JSONArray();
		columnArray.put("lodgingexpenseid.name");
		JSONObject lodgingExpenseAmount = new JSONObject();
		lodgingExpenseAmount.put("expression", "amount");
		lodgingExpenseAmount.put("type", "currency");
		columnArray.put(lodgingExpenseAmount);
		lodgingQuery.put(Data.Query.COLUMNS, columnArray);
		lodgingQuery.put(Data.Query.FILTERS, "employeegradeid = " + employeeGradeId + " and citygradeid = " + cityCountryGradeId + " and lodgingexpenseid=" + expenseType + " and tour_type='" + tourType + "'");
		JSONObject lodgingObject;
		lodgingObject = ResourceEngine.query(lodgingQuery);
		JSONArray lodgingArray = lodgingObject.getJSONArray("hris_lodgingexpenselimits");
		return lodgingArray;
	}

	private JSONArray getEmployeeRecord(ApplaneDatabaseEngine ResourceEngine, String filter) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		JSONArray array = new JSONArray();
		array.put("employee_grade_id");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, filter);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees_additional_information");
		return employeeArray;
	}

	private JSONArray getExpenseLimit(ApplaneDatabaseEngine ResourceEngine, Object cityGradeId, Object employeeGradeId, Object travelTypeId, Object tourType) throws JSONException {
		String filter = "";
		if (travelTypeId != null) {
			filter = " and tourparticularid = " + travelTypeId;
		}
		JSONObject limitQuery = new JSONObject();
		limitQuery.put(Data.Query.RESOURCE, "hris_expenselimits");
		JSONArray limitArray = new JSONArray();
		limitArray.put("tourparticularid.name");
		JSONObject tourParticularAmount = new JSONObject();
		tourParticularAmount.put("expression", "amount");
		tourParticularAmount.put("type", "currency");
		limitArray.put(tourParticularAmount);

		limitQuery.put(Data.Query.COLUMNS, limitArray);
		limitQuery.put(Data.Query.FILTERS, "employeegradeid = " + employeeGradeId + " and citygradeid = " + cityGradeId + " and tour_type = '" + tourType + "'" + filter);
		JSONObject object;
		object = ResourceEngine.query(limitQuery);
		JSONArray limitGradeArray = object.getJSONArray("hris_expenselimits");
		return limitGradeArray;
	}

	public Long getDateDifference(Object checkin, Object checkout) {
		Date checkInDate = null;
		Date checkOutDate = null;
		Long datesDiff = null;
		if (checkin != null && checkout != null) {
			try {
				checkInDate = Translator.dateValue(checkin);
				checkOutDate = Translator.dateValue(checkout);
				datesDiff = DataTypeUtilities.differenceBetweenDates(checkInDate, checkOutDate) + 1;
				return datesDiff;
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Some unexpected error occured while calculating no. of loadging days." + e.getMessage());
			}
		} else {
			return 0l;
		}
	}

	public JSONObject calculateLodgingAllowedAmount(Object employeeid, Object cityCountryId, Object checkin, Object checkout, Object tourType) {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		try {
			if (tourType == null) {
				return null;
			}
			Object cityCountryGradeId = null;
			if (("" + tourType).equals("Domestic")) {
				cityCountryGradeId = getCityGradeID(cityCountryId);
			} else if (("" + tourType).equals("Foreign")) {
				cityCountryGradeId = getCountryGradeID(cityCountryId);
			}
			Object employeeGradeId = getEmployeeGradeId(employeeid);

			int expenseType = HRISApplicationConstants.LodgingComponents.LODGING;
			JSONArray lodgingArray = getLodgingArray(ResourceEngine, cityCountryGradeId, employeeGradeId, expenseType, tourType);
			int lodgingArrayCount = lodgingArray == null ? 0 : lodgingArray.length();
			if (lodgingArrayCount > 0) {
				Double allowedLodgingExpectedAmount = 0.0;
				int lodgingAmountTypeID = 0;
				String lodgingAmountType = null;
				JSONObject lodgingAmountObject;
				// String lodgingExpenseName = lodgingArray.getJSONObject(0).optString("lodgingexpenseid.name");
				lodgingAmountObject = (JSONObject) lodgingArray.getJSONObject(0).opt("amount");
				Double lodgingAmount = (Double) lodgingAmountObject.opt("amount");
				JSONObject lodgingAmountTypeObject = (JSONObject) lodgingAmountObject.opt("type");
				lodgingAmountType = (String) lodgingAmountTypeObject.opt("currency");
				lodgingAmountTypeID = (Integer) lodgingAmountTypeObject.opt("__key__");
				long dateDiffernce = getDateDifference(checkin, checkout);
				allowedLodgingExpectedAmount = lodgingAmount * dateDiffernce;
				JSONObject totalAllowedExpectedObject = new JSONObject();
				totalAllowedExpectedObject.put("amount", allowedLodgingExpectedAmount);
				JSONObject typeObject = new JSONObject();
				typeObject.put("__key__", lodgingAmountTypeID);
				typeObject.put("currency", lodgingAmountType);
				totalAllowedExpectedObject.put("type", typeObject);
				return totalAllowedExpectedObject;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unexpected error occured while calculating allowed expense." + e.getMessage());
		}
		return null;
	}

	public JSONObject calculateTourExpenseAllowedAmount(Object cityid, long numberofdays, Object particularid, Object employeeid) {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		try {
			Object cityGradeId = getCityGradeID(cityid);
			Object employeeGradeId = getEmployeeGradeId(employeeid);
			JSONObject limitQuery = new JSONObject();
			limitQuery.put(Data.Query.RESOURCE, "hris_expenselimits");
			JSONArray limitArray = new JSONArray();
			limitArray.put("tourparticularid.name");
			JSONObject tourParticularAmount = new JSONObject();
			tourParticularAmount.put("expression", "amount");
			tourParticularAmount.put("type", "currency");
			limitArray.put(tourParticularAmount);

			limitQuery.put(Data.Query.COLUMNS, limitArray);
			limitQuery.put(Data.Query.FILTERS, "employeegradeid = " + employeeGradeId + " and citygradeid = " + cityGradeId + " and tourparticularid = " + particularid);
			JSONObject object;
			object = ResourceEngine.query(limitQuery);
			JSONArray limitGradeArray = object.getJSONArray("hris_expenselimits");
			int limitGradeArrayCount = limitGradeArray == null ? 0 : limitGradeArray.length();
			if (limitGradeArrayCount > 0) {
				Double totalAllowedAmount = 0.0;
				int amountTypeID = 0;
				String amountType = null;
				JSONObject amountObject;
				// String particularName = limitGradeArray.getJSONObject(0).optString("tourparticularid.name");
				amountObject = (JSONObject) limitGradeArray.getJSONObject(0).opt("amount");
				Double amount = (Double) amountObject.opt("amount");
				JSONObject amountTypeObject = (JSONObject) amountObject.opt("type");
				amountType = (String) amountTypeObject.opt("currency");
				amountTypeID = (Integer) amountTypeObject.opt("__key__");
				totalAllowedAmount = amount * numberofdays;
				JSONObject totalAllowedExpectedObject = new JSONObject();
				totalAllowedExpectedObject.put("amount", totalAllowedAmount);
				JSONObject typeObject = new JSONObject();
				typeObject.put("__key__", amountTypeID);
				typeObject.put("currency", amountType);
				totalAllowedExpectedObject.put("type", typeObject);
				return totalAllowedExpectedObject;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unexpected error occured while calculating particular allowed expense." + e.getMessage());
		}
		return null;
	}

	private Object getCityGradeID(Object cityid) {
		try {
			JSONObject cityQuery = new JSONObject();
			cityQuery.put(Data.Query.RESOURCE, "hris_city__citygradeid");
			JSONArray cityArray = new JSONArray();
			cityArray.put("citygradeid");
			cityQuery.put(Data.Query.COLUMNS, cityArray);
			cityQuery.put(Data.Query.FILTERS, "hris_city = " + cityid);
			JSONObject cityGradeObject;
			cityGradeObject = ResourceEngine.query(cityQuery);
			JSONArray cityGradeArray = cityGradeObject.getJSONArray("hris_city__citygradeid");
			int cityGradeArrayCount = cityGradeArray == null ? 0 : cityGradeArray.length();

			if (cityGradeArrayCount > 0) {
				Object cityGradeId = cityGradeArray.getJSONObject(0).opt("citygradeid");
				return cityGradeId;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unexpected error occured while get city gradeId." + e.getMessage());
		}
		return null;
	}

	private Object getCountryGradeID(Object countryId) {
		try {
			JSONObject cityQuery = new JSONObject();
			cityQuery.put(Data.Query.RESOURCE, "hris_countries__grade_id");
			JSONArray cityArray = new JSONArray();
			cityArray.put("grade_id");
			cityQuery.put(Data.Query.COLUMNS, cityArray);
			cityQuery.put(Data.Query.FILTERS, "hris_countries = " + countryId);
			JSONObject cityGradeObject;
			cityGradeObject = ResourceEngine.query(cityQuery);
			JSONArray cityGradeArray = cityGradeObject.getJSONArray("hris_countries__grade_id");
			int cityGradeArrayCount = cityGradeArray == null ? 0 : cityGradeArray.length();

			if (cityGradeArrayCount > 0) {
				Object cityGradeId = cityGradeArray.getJSONObject(0).opt("grade_id");
				return cityGradeId;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unexpected error occured while get country gradeId." + e.getMessage());
		}
		return null;
	}

	private Object getEmployeeGradeId(Object employeeid) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
			JSONArray array = new JSONArray();
			array.put("employee_grade_id");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "employeeid = " + employeeid);
			JSONObject employeeObject;
			employeeObject = ResourceEngine.query(query);
			JSONArray employeeArray = employeeObject.getJSONArray("hris_employees_additional_information");
			int employeeArrayCount = employeeArray == null ? 0 : employeeArray.length();
			if (employeeArrayCount > 0) {
				Object designationId = employeeArray.getJSONObject(0).opt("employee_grade_id");
				return designationId;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unexpected error occured while get DesignationID." + e.getMessage());
		}
		return null;
	}
}
