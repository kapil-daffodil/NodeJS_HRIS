package com.applane.resourceoriented.hris;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.afb.AFBConstants;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class HrisUserDefinedFunctions implements ApplaneTask {
	boolean isPerformancePercentageFirstTime = false;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public static Object getAssetsIds(Object empId) {
		Object branchId = getBranchId(empId);
		try {
			if (empId != null) {

				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "poi_inventory");
				JSONArray array = new JSONArray();
				array.put(Updates.KEY);
				array.put("productid");
				array.put("quantityadded");
				array.put("quantityremained");

				query.put(Data.Query.COLUMNS, array);

				query.put(Data.Query.FILTERS, "quantityadded > 0 AND quantityremained >0 AND locationid = " + branchId);
				query.put(Data.Query.OPTION_FILTERS, "quantityadded > 0 AND quantityremained = null AND locationid = " + branchId);

				JSONObject object = ResourceEngine.query(query);
				JSONArray assetsArray = object.getJSONArray("poi_inventory");

				JSONArray keys = new JSONArray();
				if (assetsArray != null && assetsArray.length() > 0) {
					for (int counter = 0; counter < assetsArray.length(); counter++) {
						keys.put(assetsArray.getJSONObject(counter).opt("productid"));
					}
					return keys;
				}
			} else {
				throw new BusinessLogicException("Employee Can not be null.");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(HrisUserDefinedFunctions.class.getName(), e);
			LogUtility.writeLog("HrisUserDefinedFunctions >> getCurrentEmployeeId >> trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured");
		}
		return null;
	}

	public static Object getBranchId(Object empId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray array = new JSONArray();
			array.put(Updates.KEY);
			array.put("branchid");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + empId);
			JSONObject object = ResourceEngine.query(query);
			JSONArray employeeArray = object.getJSONArray("hris_employees");
			int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
			if (employeeArrayCount > 0) {
				return employeeArray.getJSONObject(0).opt("branchid");
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(HrisUserDefinedFunctions.class.getName(), e);
			LogUtility.writeLog("HrisUserDefinedFunctions >> getCurrentEmployeeId >> trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured");
		}
		return null;
	}

	public static Object getCurrentEmployeeId(Object officialEmailID) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray array = new JSONArray();
			array.put(Updates.KEY);
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + "= '" + officialEmailID + "'");
			JSONObject object = ResourceEngine.query(query);
			JSONArray employeeArray = object.getJSONArray("hris_employees");
			int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
			if (employeeArrayCount > 0) {
				return employeeArray.getJSONObject(0).opt(Updates.KEY);
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(HrisUserDefinedFunctions.class.getName(), e);
			LogUtility.writeLog("HrisUserDefinedFunctions >> getCurrentEmployeeId >> trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured");
		}
		return null;
	}

	public JSONArray allIndirectReportingTo(Object employeeid) {
		try {
			Object employeeID;
			if ((employeeid instanceof JSONObject)) {
				employeeID = ((JSONObject) employeeid).get(Data.Query.KEY);
			} else if ((employeeid instanceof JSONArray)) {
				employeeID = ((JSONArray) employeeid).getJSONObject(0).opt(Data.Query.KEY);
			} else {
				employeeID = employeeid;
			}
			JSONArray employeeArray = getEmployeeIndirectReporting(employeeID);
			int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
			JSONArray inDirectReportingtoArray = new JSONArray();
			if (employeeArrayCount > 0) {
				for (int counter = 0; counter < employeeArrayCount; counter++) {
					Object indirectReportingToId = employeeArray.getJSONObject(counter).opt("indirectreportingto");
					Object indirectReportingToEmailId = employeeArray.getJSONObject(counter).opt("indirectreportingto.officialemailid");
					if (indirectReportingToId != null && indirectReportingToEmailId != null) {
						inDirectReportingtoArray.put(new JSONObject().put("__key__", indirectReportingToId).put("officialemailid", indirectReportingToEmailId));
					}
				}
				return inDirectReportingtoArray;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new BusinessLogicException("You are not Authorized");
		}
	}

	public JSONObject getEmployeeUserId(Object employeeid) throws Exception {
		Object employeeID;
		try {
			if ((employeeid instanceof JSONObject)) {
				employeeID = ((JSONObject) employeeid).get(Data.Query.KEY);
			} else {
				employeeID = employeeid;
			}
			String officialEmailId = null;
			if (employeeID != null) {
				officialEmailId = EmployeeLeaveBusinessLogic.getEmployeeEmailId(employeeID);
			}
			Object employeeuserId = null;
			if (officialEmailId != null && officialEmailId.length() > 0) {
				employeeuserId = EmployeeLeaveBusinessLogic.getReportingToUserId(officialEmailId);
			}
			JSONArray employeeUserArray = new JSONArray();
			JSONObject employeeObject = new JSONObject();
			if (employeeuserId != null && (officialEmailId != null && officialEmailId.length() > 0)) {
				employeeObject.put("__key__", employeeuserId);
				employeeObject.put("emailid", officialEmailId);
				employeeUserArray.put(employeeuserId);
				return employeeObject;
			}
		} catch (Exception e) {
			throw new BusinessLogicException("You are not Authorized");
		}
		return null;
	}

	public JSONArray getLeaveTypeId(Object employeeid) throws JSONException {
		if (employeeid instanceof JSONObject) {
			employeeid = ((JSONObject) employeeid).opt("__key__");
		}
		if (employeeid instanceof JSONArray) {
			employeeid = ((JSONArray) employeeid).getJSONObject(0).opt("__key__");
		}
		Object leavePolicyId = getLeavePolicyId(employeeid);
		if (leavePolicyId != null) {
			JSONArray getAllLeaveTypes = getLeaveRule(leavePolicyId);
			int getAllLeaveTypesCount = (getAllLeaveTypes == null || getAllLeaveTypes.length() == 0) ? 0 : getAllLeaveTypes.length();
			if (getAllLeaveTypesCount > 0) {
				return getAllLeaveTypes;
			}
		}
		return null;
	}

	private Object getLeavePolicyId(Object employeeid) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("leavepolicyid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeid);
		JSONObject object = ResourceEngine.query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees");
		int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
		if (employeeArrayCount > 0) {
			Object leavePolicyId = employeeArray.getJSONObject(0).opt("leavepolicyid");
			return leavePolicyId;
		}
		return null;
	}

	private Object getHolidayCalendarId() throws JSONException {
		Object currentEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("holidaycalendarid");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "officialemailid = '" + currentEmail + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees");
		int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
		if (employeeArrayCount > 0) {
			Object holidayDayCalendarId = employeeArray.getJSONObject(0).opt("holidaycalendarid");
			return holidayDayCalendarId;
		}
		return 0;
	}

	private JSONArray getLeaveRule(Object leavePolicyId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leaverule");
		JSONArray array = new JSONArray();
		array.put("leavetypeid");
		array.put("leavetypeid.name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "leavepolicyid = " + leavePolicyId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray leaveRuleArray = object.getJSONArray("hris_leaverule");
		int leaveRuleArrayCount = (leaveRuleArray == null || leaveRuleArray.length() == 0) ? 0 : leaveRuleArray.length();
		if (leaveRuleArrayCount > 0) {
			JSONArray leaveTypeArray = new JSONArray();
			for (int counter = 0; counter < leaveRuleArrayCount; counter++) {
				Object leaveTypeId = leaveRuleArray.getJSONObject(counter).opt("leavetypeid");
				leaveTypeArray.put(leaveTypeId);
			}
			return leaveTypeArray;
		}
		return null;
	}

	private JSONArray getEmployeeIndirectReporting(Object employeeID) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees__indirectreportingto");
			JSONArray array = new JSONArray();
			array.put("indirectreportingto");
			array.put("indirectreportingto.officialemailid");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "hris_employees = " + employeeID);
			JSONObject object = ResourceEngine.query(query);
			JSONArray employeeArray = object.getJSONArray("hris_employees__indirectreportingto");
			return employeeArray;
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while get employee records.");
		}
	}

	public double calculateYearlyAmount(Object salarycomponentid, Object paymentcycleid, Object amount) {
		if (salarycomponentid != null && paymentcycleid != null && amount != null) {
			double yearlyAmount;
			if ((Integer) paymentcycleid == HRISApplicationConstants.YEARLY) {
				yearlyAmount = ((Number) amount).doubleValue() * HRISApplicationConstants.PAY_YEARLY;
				return yearlyAmount;
			} else if ((Integer) paymentcycleid == HRISApplicationConstants.QUARTERLY) {
				yearlyAmount = ((Number) amount).doubleValue() * HRISApplicationConstants.PAY_QUARTERLY;
				return yearlyAmount;
			} else if ((Integer) paymentcycleid == HRISApplicationConstants.HALF_YEARLY) {
				yearlyAmount = ((Number) amount).doubleValue() * HRISApplicationConstants.PAY_HALFYEARLY;
				return yearlyAmount;
			} else if ((Integer) paymentcycleid == HRISApplicationConstants.MONTHLY) {
				yearlyAmount = ((Number) amount).doubleValue() * HRISApplicationConstants.PAY_MONTHLY;
				return yearlyAmount;
			} else if ((Integer) paymentcycleid == HRISApplicationConstants.DAILY) {
				yearlyAmount = ((Number) amount).doubleValue() * HRISApplicationConstants.PAY_DAILY;
				return yearlyAmount;
			}
		}
		return 0.0;
	}

	public Object calculateVariableAmount(Object rateObject, Object targetObject, Object performanceTypeId, Object amount) {
		try {
			if (performanceTypeId instanceof JSONObject) {
				performanceTypeId = ((JSONObject) performanceTypeId).opt(Updates.KEY);
			} else if (performanceTypeId instanceof JSONArray && ((JSONArray) performanceTypeId).length() > 0) {
				performanceTypeId = ((JSONArray) performanceTypeId).getJSONObject(0).opt(Updates.KEY);
			}
			if (rateObject != null && targetObject != null && performanceTypeId != null && Translator.integerValue(performanceTypeId) == HRISApplicationConstants.TARGET_ACHIEVED) {
				double rate = 0.0;
				double target = 0.0;
				rate = Translator.doubleValue(rateObject);
				target = Translator.doubleValue(targetObject);
				return rate * target;
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(HrisUserDefinedFunctions.class.getName(), e);
			LogUtility.writeLog("HrisUserDefinedFunctions >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknoen Error Ucured, Please Contact to Admin.");
		}
		return amount;
	}

	public double calculatePerformanceAmount(Object employeeid, Object achieved, Object target, Object keyperformanceindicatorid, Object yearId, Object monthId, Object key, Object rate) throws JSONException {
		try {
			if (employeeid != null && achieved != null && keyperformanceindicatorid != null && target != null) {
				monthId = monthId == null ? 0l : Long.parseLong(String.valueOf(monthId));
				yearId = yearId == null ? 0l : Long.parseLong(String.valueOf(yearId));

				String monthName = EmployeeSalaryGenerationServlet.getMonthName(monthId == null ? 0l : Long.parseLong(String.valueOf(monthId)));
				String yearName = EmployeeSalaryGenerationServlet.getYearName(yearId == null ? 0l : Long.parseLong(String.valueOf(yearId)));
				JSONArray employeeRecords = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeid);
				double calculatedAmount = 0d;
				for (int counter = 0; counter < employeeRecords.length(); counter++) { // will be run for one time
					Object branchId = employeeRecords.getJSONObject(counter).opt("branchid");
					if (branchId != null) {
						boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.VARIABLE);
						if (isFreezed) {
							throw new BusinessLogicException("Variable Freezed Please contact Admin Department.");
						}
					}
					Object joiningDateObject = employeeRecords.getJSONObject(counter).opt("joiningdate");

					Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
					Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
					String monthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
					String monthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);

					String fromDateStr = monthFirstDateInString;
					String lastDateStr = monthLastDateInString;

					Date joiningDate = null;
					if (joiningDateObject != null) {
						joiningDate = DataTypeUtilities.checkDateFormat(joiningDateObject);
						if (joiningDate != null && EmployeeMonthlyAttendance.isNewJoineeOrReleaved(joiningDate, monthId, yearId)) {
							fromDateStr = EmployeeSalaryGenerationServlet.getDateInString(joiningDate);
						}
					}
					JSONArray salaryComponentArray = getEmployeeComponentArray(employeeid, keyperformanceindicatorid, fromDateStr, lastDateStr);
					if (salaryComponentArray != null && salaryComponentArray.length() > 0) {
						return (Translator.doubleValue(achieved) * Translator.doubleValue(rate));
					}
				}
				return calculatedAmount;
			}
			return 0.0;
		} catch (BusinessLogicException e) {
			throw new BusinessLogicException(e.getMessage());
		} catch (Exception e) {
			LogUtility.writeLog("HrisUserDefinedFunctions >> calculatePerformanceAmount >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private JSONArray getEmployeeComponentArray(Object employeeid, Object keyperformanceindicatorid, String fromDateStr, String lastDateStr) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("salarycomponentid.performancetypeid");
		array.put("salarycomponentid.performancepercentage");
		array.put("amount");
		array.put("applicablefrom");
		array.put("applicableto");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and salarycomponentid.componentcriteriaid = " + HRISApplicationConstants.PERFORMANCE_BASED + " and salarycomponentid.keyperformanceindicatorid = " + keyperformanceindicatorid + " and applicablefrom <= '" + fromDateStr + "' and applicableto >= '" + fromDateStr + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeeid = " + employeeid + " and salarycomponentid.componentcriteriaid = " + HRISApplicationConstants.PERFORMANCE_BASED + " and salarycomponentid.keyperformanceindicatorid = " + keyperformanceindicatorid + " and applicablefrom <= '" + lastDateStr + "' and applicableto >= '" + lastDateStr + "'");
		JSONObject object = ResourceEngine.query(query);
		JSONArray salaryComponentArray = object.getJSONArray("employeesalarycomponents");
		String keys = "";
		for (int counter = 0; counter < salaryComponentArray.length(); counter++) {
			int key = Translator.integerValue(salaryComponentArray.getJSONObject(counter).opt("__key__"));
			if (keys.length() > 0) {
				keys += ("," + key);
			} else {
				keys += ("" + key);
			}
		}
		String filters = "";
		if (keys.length() > 0) {
			filters += (" and __key__ not in (" + keys + ")");
		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		array = new JSONArray();
		array.put("__key__");
		array.put("salarycomponentid.performancetypeid");
		array.put("salarycomponentid.performancepercentage");
		array.put("amount");
		array.put("applicablefrom");
		array.put("applicableto");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and salarycomponentid.componentcriteriaid = " + HRISApplicationConstants.PERFORMANCE_BASED + " and salarycomponentid.keyperformanceindicatorid = " + keyperformanceindicatorid + " and applicablefrom <= '" + fromDateStr + "' and applicableto >= '" + lastDateStr + "'" + filters);
		object = ResourceEngine.query(query);
		JSONArray salaryComponentArray1 = object.getJSONArray("employeesalarycomponents");
		for (int counter = 0; counter < salaryComponentArray1.length(); counter++) {
			JSONObject object1 = salaryComponentArray1.getJSONObject(counter);
			salaryComponentArray.put(object1);
		}
		return salaryComponentArray;
	}

	// private double getAmount(JSONObject salaryComponentArray, Object achieved, Object target) throws JSONException {
	// int performanceTypeId = salaryComponentArray.optInt("salarycomponentid.performancetypeid");
	// double performancePercentage = salaryComponentArray.opt("salarycomponentid.performancepercentage") == null ? 0.0 : salaryComponentArray.optDouble("salarycomponentid.performancepercentage");
	// double componentAmount = salaryComponentArray.opt("amount") == null ? 0.0 : salaryComponentArray.optDouble("amount");
	// double performanceAmount;
	// if (performanceTypeId == HRISApplicationConstants.TARGET_ACHIEVED && achieved != null && target != null) {
	// double achievedPercentage = (((Number) achieved).doubleValue() / ((Number) target).doubleValue()) * 100;
	// performanceAmount = (achievedPercentage * componentAmount) / 100;
	// return performanceAmount;
	// } else if (performanceTypeId == HRISApplicationConstants.PERFORMANCE_PERCENTAGE && performancePercentage > 0.0 && target == null) {
	// isPerformancePercentageFirstTime = true;
	// performanceAmount = (((Number) achieved).doubleValue() * performancePercentage) / 100;
	// return performanceAmount;
	// } else {
	// return 0.0;
	// }
	// }

	public String getSelectedKeysOfEmployeesForPackage(Object keyObject) {
		try {
			if (keyObject != null) {
				return keyObject.toString();
			} else {
				return null;
			}
		} catch (Exception e) {
			LogUtility.writeLog("HrisUserDefinedFunctions >> getSelectedKeysOfEmployeesForPackage Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException("Some Error Occured while getting Salary Component");
		}
	}

	public Object getSalalryComponent(Object keyObject) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "employeesalarycomponents");
			JSONArray array = new JSONArray();
			array.put("__key__");
			array.put("salarycomponentid");
			array.put("salarycomponentid.name");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + keyObject);
			JSONObject object = ResourceEngine.query(query);
			JSONArray salaryComponentArray = object.getJSONArray("employeesalarycomponents");
			Object name = null;
			if (salaryComponentArray.length() > 0) {
				name = salaryComponentArray.getJSONObject(0).opt("salarycomponentid.name");
			}
			return name;
		} catch (Exception e) {
			LogUtility.writeLog("HrisUserDefinedFunctions >> getSalalryComponent Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException("Some Error Occured while getting Salary Component");
		}
	}

	public Object getSubPackageId(Object parentSalaryPackageId, Object packageCounter) {
		if (parentSalaryPackageId != null) {
			long packageCounterInt = Translator.longValue(packageCounter);
			int packageIdForNested = 0;
			if (parentSalaryPackageId instanceof JSONObject) {
				parentSalaryPackageId = ((JSONObject) parentSalaryPackageId).opt(Updates.KEY);
			}

			try {
				if (packageCounterInt == 0) {
					packageIdForNested = Translator.integerValue(parentSalaryPackageId);
				}
				JSONArray subPackagesArray = getSubPackages(parentSalaryPackageId);
				JSONObject object = null;
				for (int counter = 0; counter < subPackagesArray.length(); counter++) {
					if (packageCounterInt != 0 && packageCounterInt == (counter + 1)) {
						packageIdForNested = Translator.integerValue(subPackagesArray.getJSONObject(counter).opt("subsalarypackagesid"));
						String name = Translator.stringValue(subPackagesArray.getJSONObject(counter).opt("subsalarypackagesid.name"));
						// object.put("packageid_" + packageCounter, new JSONObject().put(Updates.KEY, packageIdForNested).put("name", name));
						object = new JSONObject();
						object.put(Updates.KEY, packageIdForNested).put("name", name);
					}
				}
				return object;
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				LogUtility.writeLog("HrisUserDefinedFunctions >> getSubSalaryPackageId Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
				throw new RuntimeException("Some Error Occured while getting Salary Packages");
			}
		}
		return null;
	}

	public JSONArray getSubSalaryPackageId0(Object parentSalaryPackageId, Object packageCounter) {
		if (parentSalaryPackageId != null) {
			int packageCounterInt = Translator.integerValue(packageCounter);
			int packageIdForNested = 0;
			if (parentSalaryPackageId instanceof JSONObject) {
				parentSalaryPackageId = ((JSONObject) parentSalaryPackageId).opt(Updates.KEY);
			}

			try {
				if (packageCounterInt == 0) {
					packageIdForNested = Translator.integerValue(parentSalaryPackageId);
				}
				JSONArray subPackagesArray = getSubPackages(parentSalaryPackageId);
				JSONArray componentArrayToBeReturn = new JSONArray();
				for (int counter = 0; counter < subPackagesArray.length(); counter++) {
					// keys.put(salaryComponentArray.getJSONObject(counter).opt("subsalarypackagesid"));
					if (packageCounterInt != 0 && packageCounterInt == (counter + 1)) {
						packageIdForNested = Translator.integerValue(subPackagesArray.getJSONObject(counter).opt("subsalarypackagesid"));
					}
				}

				if (packageIdForNested != 0) {
					// List<Integer> list = new ArrayList<Integer>(0);
					JSONArray salaryPackageComponents = getSalaryPackageComponents("" + packageIdForNested);
					for (int counter = 0; counter < salaryPackageComponents.length(); counter++) {
						Object key = salaryPackageComponents.getJSONObject(counter).opt(Updates.KEY);
						// int packageId = Translator.integerValue(salaryPackageComponents.getJSONObject(counter).opt("packageid"));
						// Object fixAmount = null;
						// Object variableAmount = null;
						Object rate = salaryPackageComponents.getJSONObject(counter).opt("rate");
						Object target = salaryPackageComponents.getJSONObject(counter).opt("target");
						Object amount = salaryPackageComponents.getJSONObject(counter).opt("amount");
						Object percentage = salaryPackageComponents.getJSONObject(counter).opt("percentage");
						// if (!list.contains(packageId)) {
						// list.add(packageId);
						// fixAmount = salaryPackageComponents.getJSONObject(counter).opt("packageid.fixamount");
						// variableAmount = salaryPackageComponents.getJSONObject(counter).opt("packageid.variableamount");
						// }
						// updateSalaryPackageComponents(key, rate, target, amount, percentage, fixAmount, variableAmount);
						getComponentArrayToBeReturn(componentArrayToBeReturn, salaryPackageComponents, counter, key, rate, target, amount, percentage);
					}
				}
				return componentArrayToBeReturn;
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				LogUtility.writeLog("HrisUserDefinedFunctions >> getSubSalaryPackageId Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
				throw new RuntimeException("Some Error Occured while getting Salary Packages");
			}
		}
		return null;
	}

	public JSONArray getSubSalaryPackageId1(Object parentSalaryPackageId, Object packageCounter) {
		if (parentSalaryPackageId != null) {
			int packageCounterInt = Translator.integerValue(packageCounter);
			int packageIdForNested = 0;
			if (parentSalaryPackageId instanceof JSONObject) {
				parentSalaryPackageId = ((JSONObject) parentSalaryPackageId).opt(Updates.KEY);
			}

			try {
				if (packageCounterInt == 0) {
					packageIdForNested = Translator.integerValue(parentSalaryPackageId);
				}
				JSONArray subPackagesArray = getSubPackages(parentSalaryPackageId);
				JSONArray componentArrayToBeReturn = new JSONArray();
				for (int counter = 0; counter < subPackagesArray.length(); counter++) {
					// keys.put(salaryComponentArray.getJSONObject(counter).opt("subsalarypackagesid"));
					if (packageCounterInt != 0 && packageCounterInt == (counter + 1)) {
						packageIdForNested = Translator.integerValue(subPackagesArray.getJSONObject(counter).opt("subsalarypackagesid"));
					}
				}

				if (packageIdForNested != 0) {
					// List<Integer> list = new ArrayList<Integer>(0);
					JSONArray salaryPackageComponents = getSalaryPackageComponents("" + packageIdForNested);
					for (int counter = 0; counter < salaryPackageComponents.length(); counter++) {
						Object key = salaryPackageComponents.getJSONObject(counter).opt(Updates.KEY);
						// int packageId = Translator.integerValue(salaryPackageComponents.getJSONObject(counter).opt("packageid"));
						// Object fixAmount = null;
						// Object variableAmount = null;
						Object rate = salaryPackageComponents.getJSONObject(counter).opt("rate");
						Object target = salaryPackageComponents.getJSONObject(counter).opt("target");
						Object amount = salaryPackageComponents.getJSONObject(counter).opt("amount");
						Object percentage = salaryPackageComponents.getJSONObject(counter).opt("percentage");
						// if (!list.contains(packageId)) {
						// list.add(packageId);
						// fixAmount = salaryPackageComponents.getJSONObject(counter).opt("packageid.fixamount");
						// variableAmount = salaryPackageComponents.getJSONObject(counter).opt("packageid.variableamount");
						// }
						// updateSalaryPackageComponents(key, rate, target, amount, percentage, fixAmount, variableAmount);
						getComponentArrayToBeReturn(componentArrayToBeReturn, salaryPackageComponents, counter, key, rate, target, amount, percentage);
					}
				}
				return componentArrayToBeReturn;
			} catch (BusinessLogicException e) {
				throw e;
			} catch (Exception e) {
				LogUtility.writeLog("HrisUserDefinedFunctions >> getSubSalaryPackageId Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
				throw new RuntimeException("Some Error Occured while getting Salary Packages");
			}
		}
		return null;
	}

	private JSONArray getSubPackages(Object parentSalaryPackageId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray array = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_salarypackages__subsalarypackagesid");
		array.put("subsalarypackagesid");
		array.put("subsalarypackagesid.name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "hris_salarypackages = " + parentSalaryPackageId);
		// query.put(Data.Query.ORDERS, orders);
		JSONObject object = ResourceEngine.query(query);
		JSONArray salaryComponentArray = object.getJSONArray("hris_salarypackages__subsalarypackagesid");
		return salaryComponentArray;
	}

	private void getComponentArrayToBeReturn(JSONArray componentArrayToBeReturn, JSONArray salaryPackageComponents, int counter, Object key, Object rate, Object target, Object amount, Object percentage) throws JSONException {
		JSONObject objectForNested = new JSONObject();
		objectForNested.put(Updates.KEY, key);
		JSONObject columnValue = new JSONObject();
		columnValue.put(Updates.KEY, salaryPackageComponents.getJSONObject(counter).opt("packageid"));
		columnValue.put("name", salaryPackageComponents.getJSONObject(counter).opt("packageid.name"));
		objectForNested.put("packageid", columnValue);
		columnValue = new JSONObject();
		columnValue.put(Updates.KEY, salaryPackageComponents.getJSONObject(counter).opt("componentid"));
		columnValue.put("name", salaryPackageComponents.getJSONObject(counter).opt("componentid.name"));
		objectForNested.put("componentid", columnValue);
		objectForNested.put("rate", rate);
		objectForNested.put("target", target);
		objectForNested.put("amount", amount);
		objectForNested.put("percentage", percentage);
		columnValue = new JSONObject();
		columnValue.put(Updates.KEY, salaryPackageComponents.getJSONObject(counter).opt("componentid.performancetypeid"));
		columnValue.put("name", salaryPackageComponents.getJSONObject(counter).opt("componentid.performancetypeid.name"));
		objectForNested.put("componentid.performancetypeid", columnValue);
		columnValue = new JSONObject();
		columnValue.put(Updates.KEY, salaryPackageComponents.getJSONObject(counter).opt("componentid.componentcriteriaid"));
		columnValue.put("name", salaryPackageComponents.getJSONObject(counter).opt("componentid.componentcriteriaid.name"));
		objectForNested.put("componentid.componentcriteriaid", columnValue);

		columnValue = new JSONObject();
		columnValue.put(Updates.KEY, salaryPackageComponents.getJSONObject(counter).opt("componentid.paymenttypeid"));
		columnValue.put("name", salaryPackageComponents.getJSONObject(counter).opt("componentid.paymenttypeid.name"));
		objectForNested.put("componentid.paymenttypeid", columnValue);

		componentArrayToBeReturn.put(objectForNested);
	}

	// private void updateSalaryPackageComponents(Object key, Object rate, Object target, Object amount, Object percentage, Object fixAmount, Object variableAmount) throws JSONException {
	// JSONObject updates = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "hris_salarypackagecomponents");
	// JSONObject row = new JSONObject();
	// row.put(Updates.KEY, key);
	// // row.put("__key__", key);
	// row.put("rate_2", rate);
	// row.put("target_2", target);
	// row.put("amount_2", amount);
	// row.put("percentage_2", percentage);
	// LogUtility.writeLog("fixAmount >> " + fixAmount + " << variableAmount >> " + variableAmount);
	// if (fixAmount != null) {
	// row.put("packageid.fixamount_2", percentage);
	// }
	// if (variableAmount != null) {
	// row.put("packageid.variableamount_2", percentage);
	// }
	// updates.put(Data.Update.UPDATES, row);
	// ResourceEngine.update(updates);
	//
	// }

	private JSONArray getSalaryPackageComponents(String keyInString) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_salarypackagecomponents");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put("packageid");
		array.put("packageid.name");
		array.put("packageid.fixamount");
		array.put("packageid.variableamount");
		array.put("componentid");
		array.put("componentid.performancetypeid");
		array.put("componentid.performancetypeid.name");
		array.put("componentid.name");
		array.put("componentid.componentcriteriaid");
		array.put("componentid.componentcriteriaid.name");
		array.put("componentid.paymenttypeid");
		array.put("componentid.paymenttypeid.name");
		array.put("amount");
		array.put("rate");
		array.put("target");
		array.put("percentage");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "packageid");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "packageid IN(" + keyInString + ")");
		JSONObject object = ResourceEngine.query(query);
		JSONArray salaryComponentArray = object.getJSONArray("hris_salarypackagecomponents");
		return salaryComponentArray;
	}

	private Object getTimeInOffice(Object date, Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("totaltimeinoffice");
		query.put(Data.Query.RESOURCE, "employeeattendance");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid=" + employeeId + " AND attendancedate='" + date + "'");
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeeattendance");
		if (rows != null && rows.length() > 0) {
			return rows.getJSONObject(0).opt("totaltimeinoffice");
		} else {
			return null;
		}
	}

	public void calculatePackageComponents(Object[] selectedKeys, Object fixAmount) {
		try {
			Object packageId = selectedKeys[0];
			JSONArray packageComponents = getPackageComponents(packageId);
			deleteOldCalculatedComponents(packageId);
			double totalAmountFromPackage = 0.0;
			double totalPercentage = 0.0;

			for (int counter = 0; counter < packageComponents.length(); counter++) {
				// Object salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
				// Object componentName = packageComponents.getJSONObject(counter).opt("componentid.name");
				Object percentage = packageComponents.getJSONObject(counter).opt("percentage");
				int paymentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.paymenttypeid"));
				if (packageComponents.getJSONObject(counter).opt("amount") == null) {
					totalPercentage += Translator.doubleValue(percentage);
				}
				if (paymentTypeId != HRISApplicationConstants.DEDUCTABLE) {
					double amount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
					totalAmountFromPackage += amount;
				}
			}
			if (totalPercentage <= 100.0) {
				double balanceAmount = DataTypeUtilities.doubleValue(fixAmount) - totalAmountFromPackage;
				if (balanceAmount < 0.0) {
					balanceAmount = 0.0;
				}
				for (int counter = 0; counter < packageComponents.length(); counter++) {
					Object salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
					Object componentName = packageComponents.getJSONObject(counter).opt("componentid.name");
					Object amount = packageComponents.getJSONObject(counter).opt("amount");
					Object percentage = packageComponents.getJSONObject(counter).opt("percentage");
					double rate = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("rate"));
					double target = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("target"));
					int paymentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.paymenttypeid"));
					int componentCriteria = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.componentcriteriaid"));
					int performanceTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.performancetypeid"));

					if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
						if (amount == null && percentage != null) {
							amount = (float) (balanceAmount * Translator.doubleValue(percentage)) / 100;
						}
						insertCalculatedPackageComponentsDetails(packageId, salaryComponentId, amount);

					} else {
						if (componentCriteria == HRISApplicationConstants.PERFORMANCE_BASED && performanceTypeId == HRISApplicationConstants.TARGET_ACHIEVED) {
							amount = rate * target;
						} else if (amount == null && percentage != null) {
							amount = (float) (balanceAmount * Translator.doubleValue(percentage)) / 100;
						}
						insertCalculatedPackageComponentsDetails(packageId, salaryComponentId, amount);
					}
				}
			}
			if (totalPercentage > 100.0) {
				String title = "";
				String mailValue = "";
				title += "Total Percentage is above 100% (" + totalPercentage + ")";
				mailValue += "Total Percentage Should Less or Equal To 100% but you have assigned (" + totalPercentage + ")";

				PunchingUtility.sendMailsWithoutCC(new String[] { "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL) }, mailValue, title);
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeNameBusinessLogic.class.getName(), e);
			LogUtility.writeLog("HrisUserDefineFunctions calculatePackageComponents Exception Trace >> " + trace);

		}
	}

	private void deleteOldCalculatedComponents(Object packageId) throws Exception {
		JSONArray packageComponents = getCalculatedComponents(packageId);
		for (int counter = 0; counter < packageComponents.length(); counter++) {
			JSONObject row = packageComponents.getJSONObject(counter);
			row.put("__type__", "delete");
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_salarypackage_components_master_calculated");
		updates.put(Data.Update.UPDATES, packageComponents);
		ResourceEngine.update(updates);
	}

	private JSONArray getCalculatedComponents(Object packageid) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_salarypackage_components_master_calculated");
		columns.put("__key__");
		query.put(Data.Query.FILTERS, "package_id = " + packageid);
		query.put(Data.Query.COLUMNS, columns);
		JSONObject data = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = data.getJSONArray("hris_salarypackage_components_master_calculated");
		return rows;
	}

	private JSONArray getPackageComponents(Object packageid) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_salarypackage_components_master");
		columns.put("__key__");
		columns.put("componentid");
		columns.put("componentid.name");
		columns.put("componentid.componentcriteriaid");
		columns.put("componentid.ignoreinexceptionreport");
		columns.put("componentid.paymenttypeid");
		columns.put("componentid.performancetypeid");
		columns.put("amount");
		columns.put("percentage");
		columns.put("rate");
		columns.put("target");
		// columns.put("packageid.fixamount");
		// columns.put("packageid.variableamount");
		query.put(Data.Query.FILTERS, "packageid = " + packageid);
		query.put(Data.Query.COLUMNS, columns);
		JSONObject data = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = data.getJSONArray("hris_salarypackage_components_master");
		return rows;
	}

	public static void insertCalculatedPackageComponentsDetails(Object packageId, Object salaryComponentId, Object amount) throws JSONException {
		JSONObject updates = new JSONObject();
		JSONObject columns = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_salarypackage_components_master_calculated");
		columns.put("package_id", packageId);
		columns.put("componentid", salaryComponentId);
		columns.put("amount", amount);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static void invokeForInsertRights() {
		insertRights(null);
	}

	private static void insertRights(Object employeeId) {
		try {
			JSONArray employeeArray = getEmployees(employeeId);
			JSONArray emailIds = new JSONArray();
			for (int counter = 0; counter < employeeArray.length(); counter++) {
				employeeId = employeeArray.getJSONObject(counter).opt("__key__");
				String offId = employeeArray.getJSONObject(counter).optString("officialemailid", null);
				if (offId != null) {
					// if (emailIds.length() > 0) {
					// emailIds += "','";
					// }
					// emailIds += offId;
					emailIds.put(offId);
				}
			}
			Object ogranizationId = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID);
			if (emailIds.length() > 0) {
				// emailIds = "'" + emailIds + "'";
				JSONArray upUsers = getUserIds(emailIds);
				for (int counter = 0; counter < upUsers.length(); counter++) {
					Object userId = upUsers.getJSONObject(counter).opt("__key__");
					JSONObject object = new JSONObject();
					JSONObject columns = new JSONObject();
					columns.put("userid", userId);
					columns.put("organizationid", ogranizationId);
					columns.put("datascope", "Team");
					columns.put("_creator_", 13652);
					columns.put("appgroupid_key", 1291);
					columns.put("appgroupid_id", "hris_tour_management_a");
					object.put(Data.Update.UPDATES, columns);
					updateRecords(object);

					columns = new JSONObject();
					object = new JSONObject();
					columns.put("userid", userId);
					columns.put("organizationid", ogranizationId);
					columns.put("datascope", "Team");
					columns.put("_creator_", 13652);
					columns.put("appgroupid_key", 1292);
					columns.put("appgroupid_id", "hris_recruitment_management_a");
					object.put(Data.Update.UPDATES, columns);
					updateRecords(object);
				}
			}
			if (employeeArray != null && employeeArray.length() == 200) {
				initiateTaskQueueAgain(employeeId);
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage("HrisUserDefinedFunctions", e);
			LogUtility.writeError("insertRights >> HrisUserDefinedFunctions >> Exception >> " + trace);
		}

	}

	private static void initiateTaskQueueAgain(Object employeeId) throws Exception {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("employeeId", employeeId);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.HrisUserDefinedFunctions", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	public void initiate(ApplaneTaskInfo taskInfoParameters) throws DeadlineExceededException {
		JSONObject taskInfo = taskInfoParameters.getTaskInfo();
		Object employeeId = taskInfo.opt("employeeId");
		LogUtility.writeError("call on initiate with employee >> " + employeeId);
		insertRights(employeeId);
	}

	private static void updateRecords(JSONObject updates) throws Exception {
		updates.put(Data.Query.RESOURCE, "up_userpermissions");
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private static JSONArray getUserIds(JSONArray emailIds) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "up_users");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject parameters = new JSONObject();
		parameters.put("em_ids", emailIds);
		query.put(Data.Query.PARAMETERS, parameters);
		query.put(Data.Query.FILTERS, "emailid IN{em_ids}");
		JSONObject upUserObject = ResourceEngine.query(query);
		JSONArray upUserArray = upUserObject.getJSONArray("up_users");
		return upUserArray;
	}

	private static JSONArray getEmployees(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		String extraFilter = "";
		if (employeeId != null) {
			extraFilter = " and " + Updates.KEY + " > " + employeeId;
		} else {
			// extraFilter = " and " + Updates.KEY + " > 269";
		}

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "officialemailid!=null AND employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE + extraFilter);
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, Updates.KEY);
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.MAX_ROWS, 200);

		JSONObject employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static void createVendor(Object[] key) throws Exception {
		Object category = 2;
		for (int counter = 0; counter < key.length; counter++) {
			Object employeeId = key[counter];
			JSONArray employeeArray = getEmployeeDetails(employeeId);
			JSONArray organizationContactsArray = getOrganizationContactsArray(employeeId);
			JSONObject update = new JSONObject();
			if (organizationContactsArray != null && organizationContactsArray.length() > 0) {
				update.put(Data.Update.Updates.KEY, organizationContactsArray.getJSONObject(0).opt("__key__"));
			} else {
				update.put(AFBConstants.OrganizationAndContacts.EMPLOYEE_ID, employeeId);
			}
			update.put(AFBConstants.OrganizationAndContacts.CONTACT_TYPE_ID, 1);
			update.put(AFBConstants.OrganizationAndContacts.ORGNAME, employeeArray.getJSONObject(0).opt("name"));
			if (employeeArray.getJSONObject(0).opt("name_in_bank") != null) {
				update.put("name_in_bank", employeeArray.getJSONObject(0).opt("name_in_bank"));
			} else {
				update.put("name_in_bank", employeeArray.getJSONObject(0).opt("name"));
			}
			update.put(AFBConstants.OrganizationAndContacts.CONTACT_NAME, employeeArray.getJSONObject(0).opt("employeecode"));
			update.put(AFBConstants.OrganizationAndContacts.ORGANIZATION_CATEGORIES, category);
			JSONObject updateQuery = new JSONObject();
			updateQuery.put(Data.Query.RESOURCE, AFBConstants.ORGANIZATION_ORGANIZATIONANDCONTACTS);
			updateQuery.put(Data.UPDATES, update);
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updateQuery);
		}
	}

	private static JSONArray getOrganizationContactsArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_organizationandcontacts");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("organization_organizationandcontacts");
		return rows;
	}

	private static JSONArray getEmployeeDetails(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("employeecode");
		array.put("name");
		array.put("name_in_bank");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + employeeId);
		JSONObject attendanceObject;
		attendanceObject = ResourceEngine.query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_employees");
		return rows;
	}

	public static String getCabAdminOfficialEmailId() throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_OFFICIAL_EMAIL_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, columns);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.MAIL_CONFIGURATIONS);

		LogUtility.writeLog("getCabAdminOfficialEmailId >> Mail Configuration Json Array : [" + resultJSONArray + "]");

		String cabAdminOfficialEmailId = null;

		if (resultJSONArray != null && resultJSONArray.length() > 0) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(0);
			cabAdminOfficialEmailId = jsonObject.optString(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_OFFICIAL_EMAIL_ID);
		}

		return cabAdminOfficialEmailId;
	}

	public static String[] getCabAdminNameAndOfficialEmailId() throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_OFFICIAL_EMAIL_ID);
		columns.put(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_NAME);
		columns.put(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_ID);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, columns);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.MAIL_CONFIGURATIONS);

		LogUtility.writeLog("getCabAdminOfficialEmailId >> Mail Configuration Json Array : [" + resultJSONArray + "]");

		String cabAdminOfficialEmailId = null;
		String cabAdminName = null;
		String cabAdminId = null;

		if (resultJSONArray != null && resultJSONArray.length() > 0) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(0);

			cabAdminName = jsonObject.optString(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_NAME);
			cabAdminOfficialEmailId = jsonObject.optString(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_OFFICIAL_EMAIL_ID);
			cabAdminId = jsonObject.optString(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_ID);
		}

		return new String[] { cabAdminName, cabAdminOfficialEmailId, cabAdminId };
	}

	public static Object getCabAdminEmployeeId() throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, columns);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.MAIL_CONFIGURATIONS);

		LogUtility.writeLog("getCabAdminOfficialEmailId >> Mail Configuration Json Array : [" + resultJSONArray + "]");

		Object cabAdminEmployeeId = null;

		if (resultJSONArray != null && resultJSONArray.length() > 0) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(0);
			cabAdminEmployeeId = jsonObject.opt(HrisKinds.MailConfigurations.CAB_ADMIN_EMPLOYEE_ID);
		}

		return cabAdminEmployeeId;
	}

	public static Map<Integer, String[]> getOtherBranchHREmailIds(Integer employeeBranchId) throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(Data.Query.KEY);
		columns.put(HrisKinds.HrAssigning.EMPLOYEE_ID);
		columns.put(HrisKinds.HrAssigning.OFFICIAL_EMAIL_ID);
		columns.put(HrisKinds.HrAssigning.BRANCH_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.HR_ASSIGNING);
		query.put(Data.Query.COLUMNS, columns);
		if (employeeBranchId != null) {
			query.put(Data.Query.FILTERS, HrisKinds.HrAssigning.BRANCH_ID + "=" + employeeBranchId);
		}

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.HR_ASSIGNING);

		LogUtility.writeLog("HRIS Branches Json Array : [" + resultJSONArray + "]");

		int noOfBranches = resultJSONArray == null ? 0 : resultJSONArray.length();
		LogUtility.writeLog("No of Branches : [" + noOfBranches + "]");

		Map<Integer, String[]> branchHREmailIdsMapping = new HashMap<Integer, String[]>();

		for (int i = 0; i < noOfBranches; i++) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(i);

			Integer branchId = jsonObject.optInt(HrisKinds.HrAssigning.BRANCH_ID);
			String hrOfficialEMailId = jsonObject.optString(HrisKinds.HrAssigning.OFFICIAL_EMAIL_ID, null);

			List<String> hrEmailIdsList = new ArrayList<String>();

			if (hrOfficialEMailId != null) {
				hrEmailIdsList.add(hrOfficialEMailId);
			}

			LogUtility.writeLog("Branch Id : [" + branchId + "], HR Email Id : [" + hrOfficialEMailId + "]");

			if (hrEmailIdsList.size() > 0) {
				String[] hrEmailIds = new String[hrEmailIdsList.size()];
				hrEmailIdsList.toArray(hrEmailIds);

				branchHREmailIdsMapping.put(branchId, hrEmailIds);
			}
		}

		LogUtility.writeLog("Branch HR Email Ids Mapping : " + branchHREmailIdsMapping);

		return branchHREmailIdsMapping;
	}

	public static Map<Integer, String[]> getNavigantBranchHREmailIds(Integer employeeBranchId) throws Exception {

		JSONArray columns = new JSONArray();
		columns.put(Data.Query.KEY);
		columns.put(new JSONObject().put(HrisKinds.Branches.HR_EMAIL_IDS, new JSONArray().put(HrisKinds.Employess.OFFICIAL_EMAIL_ID)));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.BRANCHES);
		query.put(Data.Query.COLUMNS, columns);
		if (employeeBranchId != null) {
			query.put(Data.Query.FILTERS, HrisKinds.Branches.KEY + "=" + employeeBranchId);
		}

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray hrisBranchesJsonArray = result.optJSONArray(HrisKinds.BRANCHES);

		LogUtility.writeLog("HRIS Branches Json Array : [" + hrisBranchesJsonArray + "]");

		int noOfBranches = hrisBranchesJsonArray == null ? 0 : hrisBranchesJsonArray.length();
		LogUtility.writeLog("No of Branches : [" + noOfBranches + "]");

		Map<Integer, String[]> branchHREmailIdsMapping = new HashMap<Integer, String[]>();

		for (int i = 0; i < noOfBranches; i++) {

			JSONObject jsonObject = hrisBranchesJsonArray.optJSONObject(i);

			Integer branchId = jsonObject.optInt(Data.Query.KEY);
			JSONArray hrEmailIdsJsonArray = jsonObject.optJSONArray(HrisKinds.Branches.HR_EMAIL_IDS);

			LogUtility.writeLog("Branch Id : [" + branchId + "], HR Email Ids Json Array : [" + hrEmailIdsJsonArray + "]");

			int hrEmailIdsLength = hrEmailIdsJsonArray == null ? 0 : hrEmailIdsJsonArray.length();
			LogUtility.writeLog("HR Email Ids Length : [" + hrEmailIdsLength + "]");

			List<String> hrEmailIdsList = new ArrayList<String>();

			for (int j = 0; j < hrEmailIdsLength; j++) {

				JSONObject hrEmailIdJSONObject = hrEmailIdsJsonArray.optJSONObject(i);
				LogUtility.writeLog("HR Email Id Json Object : [" + hrEmailIdJSONObject + "]");

				if (hrEmailIdJSONObject != null) {
					String hrOfficialEMailId = hrEmailIdJSONObject.optString(HrisKinds.Employess.OFFICIAL_EMAIL_ID, null);
					if (hrOfficialEMailId != null) {
						hrEmailIdsList.add(hrOfficialEMailId);
					}
				}
			}

			LogUtility.writeLog("HR Email Ids List : " + hrEmailIdsList);

			if (hrEmailIdsList.size() > 0) {
				String[] hrEmailIds = new String[hrEmailIdsList.size()];
				hrEmailIdsList.toArray(hrEmailIds);

				branchHREmailIdsMapping.put(branchId, hrEmailIds);
			}
		}

		LogUtility.writeLog("Branch HR Email Ids Mapping : " + branchHREmailIdsMapping);

		return branchHREmailIdsMapping;
	}

	public static JSONObject getEmployeeDetail(String officialEmailId) throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(Data.Query.KEY);
		columns.put(HrisKinds.Employess.NAME);
		columns.put(HrisKinds.Employess.EMPLOYEE_CODE);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + "= '" + officialEmailId + "'");

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.getJSONArray(HrisKinds.EMPLOYEES);

		if (resultJSONArray != null && resultJSONArray.length() > 0) {
			return resultJSONArray.optJSONObject(0);
		}

		return null;
	}

	public static JSONObject getMailConfigurationDetail() throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.MailConfigurations.CMD_ID);
		columns.put(HrisKinds.MailConfigurations.CMD_OFFICIAL_EMAIL_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, columns);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.MAIL_CONFIGURATIONS);

		LogUtility.writeLog("getMailConfigurationDetail >> Mail Configuration Json Array : [" + resultJSONArray + "]");

		if (resultJSONArray != null && resultJSONArray.length() > 0) {
			return resultJSONArray.optJSONObject(0);
		}

		return null;
	}

	public static JSONArray getEmployeeAttendance(Object employeeId, Object date) throws Exception {

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEE_ATTENDANCE);

		JSONArray columns = new JSONArray();
		columns.put("__key__");

		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, "employeeid=" + employeeId + " AND attendancedate='" + date + "'");

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.getJSONArray(HrisKinds.EMPLOYEE_ATTENDANCE);

		return resultJSONArray;
	}

	public static String getShiftRequestOffDays(Record record) {

		JSONArray offDaysJsonArray = (JSONArray) record.getValue(HrisKinds.SHIFT_REQUESTS_OFF_DAYS);
		int offDaysLength = (offDaysJsonArray == null) ? 0 : offDaysJsonArray.length();

		List<String> offDaysList = new ArrayList<String>();
		for (int i = 0; i < offDaysLength; i++) {
			JSONObject jsonObject = offDaysJsonArray.optJSONObject(i);
			String offDay = jsonObject.optString(HrisKinds.ShiftRequestsOffDays.OFF_DAY, null);
			if (offDay != null) {
				offDaysList.add(offDay);
			}
		}

		String offDays = offDaysList.toString().replace("[", "").replace("]", "").trim();
		LogUtility.writeLog(HrisUserDefinedFunctions.class.getSimpleName() + " >> getShiftRequestOffDays >> Off Days : " + offDays);

		return offDays;
	}

	public static String getShiftChangeByTLOffDays(Record record) {

		JSONArray offDaysJsonArray = (JSONArray) record.getValue(HrisKinds.SHIFT_CHANGE_BY_TL_OFF_DAYS);
		int offDaysLength = (offDaysJsonArray == null) ? 0 : offDaysJsonArray.length();

		List<String> offDaysList = new ArrayList<String>();
		for (int i = 0; i < offDaysLength; i++) {
			JSONObject jsonObject = offDaysJsonArray.optJSONObject(i);
			String offDay = jsonObject.optString(HrisKinds.ShiftChangeByTLOffDays.OFF_DAY, null);
			if (offDay != null) {
				offDaysList.add(offDay);
			}
		}

		String offDays = offDaysList.toString().replace("[", "").replace("]", "").trim();
		LogUtility.writeLog(HrisUserDefinedFunctions.class.getSimpleName() + " >> getShiftChangeByTLOffDays >> Off Days : " + offDays);

		return offDays;
	}
}