package com.applane.hris;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.google.apphosting.api.ApiProxy;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class BalanceLeaves {

	// job define on "hris_leaverequest" view new leave request column
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public JSONArray employeeBalanceLeave(Object employeeid, Object requestdate) {

		String applicationVersionId = ApiProxy.getCurrentEnvironment().getVersionId().substring(0, ApiProxy.getCurrentEnvironment().getVersionId().indexOf("."));
		if (applicationVersionId.equalsIgnoreCase("testviews")) {
			JSONArray balanceLeaveArray = com.applane.resourceoriented.hris.BalanceLeaves.employeeBalanceLeave(employeeid, requestdate);
			return balanceLeaveArray;
		} else {
			try {
				// GET EMPLOYEEID FROM KEY
				Object employeeId;
				if ((employeeid instanceof JSONObject)) {
					employeeId = ((JSONObject) employeeid).get(Data.Query.KEY);
				} else {
					employeeId = employeeid;
				}

				// get designationid from hris_employees where employeeId is above

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

						// GET leavepolicyid from hris_leavepolicy__designationid where designationid is above
						JSONObject leavePolicyQuery = new JSONObject();
						leavePolicyQuery.put(Data.Query.RESOURCE, "hris_designations__leavepolicyid");
						JSONArray policyArray = new JSONArray();
						policyArray.put("leavepolicyid");
						policyArray.put("leavepolicyid.fromdate");
						policyArray.put("leavepolicyid.todate");
						leavePolicyQuery.put(Data.Query.COLUMNS, policyArray);
						leavePolicyQuery.put(Data.Query.FILTERS, "hris_designations= " + designationId);
						JSONObject leavePolicyObject;
						leavePolicyObject = ResourceEngine.query(leavePolicyQuery);
						JSONArray leavePolicyArray = leavePolicyObject.getJSONArray("hris_designations__leavepolicyid");

						int leavePolicyArrayCount = leavePolicyArray == null ? 0 : leavePolicyArray.length();
						if (leavePolicyArrayCount > 0) {
							Object leavePolicyId = leavePolicyArray.getJSONObject(0).opt("leavepolicyid");
							Object leavePolicyFromDateObject = leavePolicyArray.getJSONObject(0).opt("leavepolicyid.fromdate");
							Object leavePolicyToDateObject = leavePolicyArray.getJSONObject(0).opt("leavepolicyid.todate");

							SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

							Date leavePolicyFromDate = null;
							Date leavePolicyToDate = null;
							Date requestDate = null;

							if (leavePolicyFromDateObject != null && leavePolicyToDateObject != null && requestdate != null) {
								// parse leave policy from date
								try {
									leavePolicyFromDate = queryDateFormat.parse("" + leavePolicyFromDateObject);
								} catch (ParseException e) {
									e.printStackTrace();
									try {
										leavePolicyFromDate = updateDateFormat.parse("" + leavePolicyFromDateObject);
									} catch (ParseException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
										throw new RuntimeException("LEAVE POLICY from date is not parsable" + e.getMessage());
									}
								}

								// parse leave policy to date
								try {
									leavePolicyToDate = queryDateFormat.parse("" + leavePolicyToDateObject);
								} catch (ParseException e) {
									e.printStackTrace();
									try {
										leavePolicyToDate = updateDateFormat.parse("" + leavePolicyToDateObject);
									} catch (ParseException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
										throw new RuntimeException("LEAVE POLICY to date is not parsable" + e.getMessage());
									}
								}

								// parse request date
								try {
									requestDate = queryDateFormat.parse("" + requestdate);
								} catch (ParseException e) {
									e.printStackTrace();
									try {
										requestDate = updateDateFormat.parse("" + requestdate);
									} catch (ParseException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
										throw new RuntimeException("REQUEST date is not parsable" + e.getMessage());
									}
								}
							} else {
								LogUtility.writeLog("one or more  than one dates are null");
							}

							if (leavePolicyFromDate.before(requestDate) && requestDate.before(leavePolicyToDate)) {
								// GET leaveruleid, leavetypeid, and noofleaves from hris_leaverules where leavepolicyid is above
								JSONObject leaveRuleQuery = new JSONObject();
								leaveRuleQuery.put(Data.Query.RESOURCE, "hris_leaverule");
								JSONArray ruleArray = new JSONArray();
								ruleArray.put("__key__");
								ruleArray.put("leavetypeid");
								ruleArray.put("leavetypeid.name");
								ruleArray.put("noofleaves");
								ruleArray.put("maxallowed");
								leaveRuleQuery.put(Data.Query.COLUMNS, ruleArray);
								leaveRuleQuery.put(Data.Query.FILTERS, "leavepolicyid= " + leavePolicyId);
								JSONObject leaveRuleObject;
								leaveRuleObject = ResourceEngine.query(leaveRuleQuery);
								JSONArray leaveRuleArray = leaveRuleObject.getJSONArray("hris_leaverule");

								int leaveCount = (leaveRuleArray == null || leaveRuleArray.length() == 0) ? 0 : leaveRuleArray.length();
								JSONArray categoryTypeArray = new JSONArray();
								if (leaveCount > 0) {
									for (int leaveCounter = 0; leaveCounter < leaveCount; leaveCounter++) {
										Object key = leaveRuleArray.getJSONObject(leaveCounter).opt("__key__");

										Object leaveTypeId = leaveRuleArray.getJSONObject(leaveCounter).opt("leavetypeid");
										Object leaveType = leaveRuleArray.getJSONObject(leaveCounter).opt("leavetypeid.name");
										Double noofLeaves = leaveRuleArray.getJSONObject(leaveCounter).getDouble("noofleaves");
										Object maxAllowedLeavesInMonth = leaveRuleArray.getJSONObject(leaveCounter).opt("maxallowed");

										JSONObject typeobject = new JSONObject();

										JSONObject leaveTypeObject = new JSONObject();
										leaveTypeObject.put("__key__", leaveTypeId);
										leaveTypeObject.put("name", leaveType);
										typeobject.put("leavetypeid", leaveTypeObject);
										typeobject.put("totalleaves", noofLeaves);
										Double takenLeaves = calculateLeaveTaken(employeeId, leaveTypeId, leavePolicyFromDateObject, leavePolicyToDateObject);
										typeobject.put("takenleaves", takenLeaves);
										Double balanceLeave = noofLeaves - takenLeaves;
										String balanceMessage = "";
										if (balanceLeave >= 0) {
											balanceMessage = balanceLeave + " leaves pending";
										} else {
											balanceMessage = (balanceLeave * (-1.0)) + " leaves overdue";
										}
										typeobject.put("balanceleaves", balanceMessage);
										categoryTypeArray.put(leaveCounter, typeobject);
									}
								}
								return categoryTypeArray;
							} else {
								WarningCollector.collect("Your leave policy is outdated.");
							}
						}
					}
				}
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Error while generating leave balance report" + e.getMessage());
			}

		}

	}

	public JSONArray populateEmployeeBalanceLeavesDARCL(Object employeeid, Object requestdate) {

		String applicationVersionId = ApiProxy.getCurrentEnvironment().getVersionId().substring(0, ApiProxy.getCurrentEnvironment().getVersionId().indexOf("."));
		if (applicationVersionId.equalsIgnoreCase("testviews")) {
			JSONArray balanceLeaveArray = com.applane.resourceoriented.hris.BalanceLeaves.employeeBalanceLeave(employeeid, requestdate);
			return balanceLeaveArray;
		} else {
			try {
				// GET EMPLOYEEID FROM KEY
				if ((employeeid instanceof JSONArray)) {
					employeeid = ((JSONArray) employeeid).getJSONObject(0).get(Data.Query.KEY);
				}

				if ((employeeid instanceof JSONObject)) {
					employeeid = ((JSONObject) employeeid).get(Data.Query.KEY);
				}

				JSONArray employeeLeavePolicyArray = getEmployeeLeavePolicyId(employeeid);
				if (employeeLeavePolicyArray != null && employeeLeavePolicyArray.length() > 0) {
					Object leavePolicyId = employeeLeavePolicyArray.getJSONObject(0).opt("leavepolicyid");
					Object leavePolicyFromDate = employeeLeavePolicyArray.getJSONObject(0).opt("leavepolicyid.fromdate");
					Object leavePolicyToDate = employeeLeavePolicyArray.getJSONObject(0).opt("leavepolicyid.todate");
					JSONArray employeeLeaveRuleArray = getEmployeeLeaveRule(employeeid, leavePolicyId);
					if (employeeLeaveRuleArray != null && employeeLeaveRuleArray.length() > 0) {
						JSONArray categoryTypeArray = new JSONArray();
						for (int counter = 0; counter < employeeLeaveRuleArray.length(); counter++) {
							Object leaveTypeId = employeeLeaveRuleArray.getJSONObject(counter).opt("leavetypeid");
							Object leaveTypeName = employeeLeaveRuleArray.getJSONObject(counter).opt("leavetypeid.name");
							Object openingBalance = employeeLeaveRuleArray.getJSONObject(counter).opt("openingbalance");
							Object assignedLeave = employeeLeaveRuleArray.getJSONObject(counter).opt("assignedleaves");
							double totalMonthBetweenFromDateAneToDate = getTotalMonthBetweenFromDateAneToDate(Translator.dateValue(leavePolicyFromDate), Translator.dateValue(leavePolicyToDate));
							double takenLeaves = calculateLeaveTaken(employeeid, leaveTypeId, leavePolicyFromDate, leavePolicyToDate);
							double totalAssignedLeaves = Translator.doubleValue(openingBalance) + (Translator.doubleValue(assignedLeave) / totalMonthBetweenFromDateAneToDate);
							double balance = totalAssignedLeaves - takenLeaves;

							JSONObject leaveTypeObject = new JSONObject();
							JSONObject typeobject = new JSONObject();

							leaveTypeObject.put("__key__", leaveTypeId);
							leaveTypeObject.put("name", leaveTypeName);

							typeobject.put("leavetypeid", leaveTypeObject);
							typeobject.put("totalleaves", totalAssignedLeaves);
							typeobject.put("takenleaves", takenLeaves);
							String balanceMessage = "";
							if (balance >= 0) {
								balanceMessage = new DecimalFormat("0.#").format(balance) + " leaves pending";
							} else {
								balanceMessage = new DecimalFormat("0.#").format((balance * (-1.0))) + " leaves overdue";
							}
							typeobject.put("balanceleaves", balanceMessage);
							categoryTypeArray.put(counter, typeobject);
						}
						return categoryTypeArray;
					}
				}

			} catch (Exception e) {
				LogUtility.writeLog("BalanceLeaves >> populateEmployeeBalanceLeavesDARCL Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
				throw new RuntimeException("Some Error Occured while Generating Leave Balance");
			}
		}
		return null;

	}

	private double getTotalMonthBetweenFromDateAneToDate(Date fromDate, Date toDate) {
		final double AVERAGE_MILLIS_PER_MONTH = 365.24 * 24 * 60 * 60 * 1000 / 12;
		return (toDate.getTime() - fromDate.getTime()) / AVERAGE_MILLIS_PER_MONTH;
	}

	private JSONArray getEmployeeLeaveRule(Object employeeid, Object leavePolicyId) throws JSONException {
		JSONObject leavePolicyQuery = new JSONObject();
		leavePolicyQuery.put(Data.Query.RESOURCE, "employeebalanceleavehistory");
		JSONArray policyArray = new JSONArray();
		policyArray.put("leavepolicyid");
		policyArray.put("leavetypeid");
		policyArray.put("leavetypeid.name");
		policyArray.put("openingbalance");
		policyArray.put("assignedleaves");
		leavePolicyQuery.put(Data.Query.COLUMNS, policyArray);
		leavePolicyQuery.put(Data.Query.FILTERS, "employeeid = " + employeeid + " and leavepolicyid = " + leavePolicyId);
		JSONObject leavePolicyObject;
		leavePolicyObject = ResourceEngine.query(leavePolicyQuery);
		JSONArray leavePolicyArray = leavePolicyObject.getJSONArray("employeebalanceleavehistory");
		return leavePolicyArray;
	}

	private JSONArray getEmployeeLeavePolicyId(Object employeeid) throws JSONException {
		JSONObject leavePolicyQuery = new JSONObject();
		leavePolicyQuery.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray policyArray = new JSONArray();
		policyArray.put("leavepolicyid");
		policyArray.put("leavepolicyid.fromdate");
		policyArray.put("leavepolicyid.todate");
		leavePolicyQuery.put(Data.Query.COLUMNS, policyArray);
		leavePolicyQuery.put(Data.Query.FILTERS, "__key__ = " + employeeid);
		JSONObject leavePolicyObject;
		leavePolicyObject = ResourceEngine.query(leavePolicyQuery);
		JSONArray leavePolicyArray = leavePolicyObject.getJSONArray("hris_employees");
		return leavePolicyArray;
	}

	private Double calculateLeaveTaken(Object employeeId, Object leaveTypeId, Object leavePolicyFromDate, Object leavePolicyToDate) throws JSONException {
		// select * from hris_leaverequest
		// where fromDateHL>leavePolicyFromDate and toDateHL < leavePolicyToDate and employeeid = employeeId
		JSONObject leaveCountAggregate = new JSONObject();
		leaveCountAggregate.put(Data.Query.Columns.EXPRESSION, "leavecount");
		leaveCountAggregate.put(Data.Query.Columns.AGGREGATE, "sum");
		JSONObject compensateAggregate = new JSONObject();
		compensateAggregate.put(Data.Query.Columns.EXPRESSION, "compensate");
		compensateAggregate.put(Data.Query.Columns.AGGREGATE, "sum");

		JSONObject leaveQuery = new JSONObject();
		leaveQuery.put(Data.Query.RESOURCE, "hris_leaverequests");
		JSONArray leaveArray = new JSONArray();
		leaveArray.put(leaveCountAggregate);
		leaveArray.put(compensateAggregate);
		leaveQuery.put(Data.Query.COLUMNS, leaveArray);
		leaveQuery.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and leavetypeid = " + leaveTypeId + " and fromdate > '" + leavePolicyFromDate + "' and todate < '" + leavePolicyToDate + "'" + " and leavestatusid = " + HRISConstants.LEAVE_APPROVED);

		JSONArray groupby = new JSONArray();
		groupby.put("leavetypeid");
		leaveQuery.put(Data.Query.GROUPS, groupby);

		JSONObject leaveRequestObject;
		leaveRequestObject = ResourceEngine.query(leaveQuery);
		JSONArray leaveRequestArray = leaveRequestObject.getJSONArray("hris_leaverequests");
		LogUtility.writeLog("leaveRequestArray IS : >> " + leaveRequestArray);

		double approvedLeaves = 0d;
		double compensateLeaves = 0d;
		double takenLeaves = 0d;
		if (leaveRequestArray.length() > 0) {
			LogUtility.writeLog("leaveRequestArray is greater than zero :");

			approvedLeaves = leaveRequestArray.getJSONObject(0).opt("leavecount") == null ? 0.0 : leaveRequestArray.getJSONObject(0).optDouble("leavecount");
			LogUtility.writeLog("approved Leaves are: >>" + approvedLeaves);

			compensateLeaves = leaveRequestArray.getJSONObject(0).opt("compensate") == null ? 0.0 : leaveRequestArray.getJSONObject(0).optDouble("compensate");
			takenLeaves = approvedLeaves - compensateLeaves;
		}
		return takenLeaves;
	}
}
