package com.applane.resourceoriented.hris.advance;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.service.applaneurlfetch.ApplaneURLFetch;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class AdvanceUtility {

	@SuppressWarnings("unused")
	private Object getEmployeeIDSToShowAdvanceRecords(Object officialEmailID) {
		try {
			int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			boolean isDARCL = false;
			if (currentOrganizationId == 606) {
				isDARCL = true;
			}
			String filter = HrisKinds.Employess.OFFICIAL_EMAIL_ID + "= '" + officialEmailID + "'";
			JSONArray employeeArray = getEmployeeRecords(filter);
			int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
			if (employeeArrayCount > 0) {
				String ids = "";
				Object employeeId = employeeArray.getJSONObject(0).opt(Updates.KEY);

				if (isDARCL) {
					JSONArray advanceIds = gatAdvanceIds(employeeId);
					for (int counter = 0; counter < advanceIds.length(); counter++) {
						if (ids.length() > 0) {
							ids += ",";
						}
						ids += advanceIds.getJSONObject(counter).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + "." + HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
					}
					if (ids.length() == 0) {
						return 0;
					}
					return ids;
				}

				ids += employeeId;
				filter = "reportingtoid = " + employeeId;
				employeeArray = getEmployeeRecords(filter);
				if (employeeArray != null && employeeArray.length() > 0) {
					for (int counter = 0; counter < employeeArray.length(); counter++) {
						Object employeeIdTemp = employeeArray.getJSONObject(counter).opt(Updates.KEY);
						if (ids.length() > 0) {
							ids += ",";
						}
						ids += employeeIdTemp;
					}
				}
				JSONArray businessUnitArray = getBusinessUnitArray(employeeId);
				if (businessUnitArray != null && businessUnitArray.length() > 0) {
					String businessUnits = "";
					for (int counter = 0; counter < businessUnitArray.length(); counter++) {
						if (businessUnits.length() > 0) {
							businessUnits += ",";
						}
						businessUnits += businessUnitArray.getJSONObject(counter).optString(Updates.KEY);
					}
					if (businessUnits.length() > 0) {
						filter = "profitcenterid IN(" + businessUnits + ")";
						employeeArray = getEmployeeRecords(filter);
						if (employeeArray != null && employeeArray.length() > 0) {
							for (int counter = 0; counter < employeeArray.length(); counter++) {
								Object employeeIdTemp = employeeArray.getJSONObject(counter).opt(Updates.KEY);
								if (ids.length() > 0) {
									ids += ",";
								}
								ids += employeeIdTemp;
							}
						}
					}
				}
				return ids;
			} else {
				throw new BusinessLogicException("You are not an employee of current organization.");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("HrisUserDefinedFunctions >> getCurrentEmployeeId >> trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured");
		}
	}

	public static Object isBuHr() throws Exception {
		Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		String filter = HrisKinds.Employess.OFFICIAL_EMAIL_ID + "= '" + currentUserEmail + "'";
		JSONArray employeeArray = new AdvanceUtility().getEmployeeRecords(filter);
		int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
		if (employeeArrayCount > 0) {
			Object employeeId = employeeArray.getJSONObject(0).opt(Updates.KEY);
			JSONArray buHrs = new AdvanceUtility().getBuHrs(employeeId);
			if (buHrs != null && buHrs.length() > 0) {
				return true;
			}
		}
		return false;
	}

	private JSONArray getBuHrs(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_profitcenter__hr_emails_id");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "hr_emails_id	 = " + employeeId);
		query.put(Data.Query.MAX_ROWS, 1);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("organization_profitcenter__hr_emails_id");
		return employeeArray;
	}

	private JSONArray gatAdvanceIds(Object ids) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + "." + HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + "=" + ids);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray(HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);
		return employeeArray;
	}

	private JSONArray getBusinessUnitArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_profitcenter");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "director_id	 = " + employeeId);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("organization_profitcenter");
		return employeeArray;
	}

	private JSONArray getEmployeeRecords(String filter) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, filter);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees");
		return employeeArray;
	}

	public JSONArray getAlreadyTakenAdvanceDetails(Object employeeId) {
		try {
			if (employeeId != null) {
				JSONArray advanceDetails = getAdvanceDetails(employeeId);
				JSONArray details = new JSONArray();
				for (int counter = 0; counter < advanceDetails.length(); counter++) {
					JSONObject object = advanceDetails.getJSONObject(counter);
					JSONObject objectDetails = new JSONObject();
					objectDetails.put("advance_amount_id." + HRISApplicationConstants.AdvanceAmount.REQUEST_DATE, object.opt(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE));
					objectDetails.put("advance_amount_id." + HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR, Translator.doubleValue(object.opt(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)));
					details.put(objectDetails);
				}
				return details;
			} else {
				throw new BusinessLogicException("Employee can not be NULL employee > " + employeeId);
			}
		} catch (Exception e) {
			LogUtility.writeLog("ShortLeaveUtility getShortLeavesInAMonth Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			// throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			throw new BusinessLogicException("Some error occured while fetching your leaves in current month, Please try after some time");
		}
	}

	@SuppressWarnings("unused")
	// used in advance panel view to get advance amount return to company
	private Object getBalanceAmountDetails(Object employeeId, Object employeeCode) {
		try {
			if (employeeId != null && employeeCode != null) {
				if (("" + employeeCode).length() < 8) {
					employeeCode = "00" + employeeCode;
				}
				String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
				Calendar cal = Calendar.getInstance();
				String inOutDate = cal.get(Calendar.DAY_OF_MONTH) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR);

				// String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/getEmpVendorAccBal?EmpID=" + employeeCode + "&DateFrom=" + inOutDate + "&DateTo=" + inOutDate;
				String urlString = "http://apps.darcl.com:9080/EmployeeDetailWS/employeedata/getEmpVendorAccBal?EmpID=" + employeeCode + "&AsOnDate=" + TODAY_DATE;
				String employeesJsonObjectInString = ApplaneURLFetch.requestToRemoteService(urlString);
				JSONObject employeeJSONObject = null;
				JSONArray employeeBalanceAmountArray = new JSONArray();
				boolean isInsert = true;
				try {
					employeeJSONObject = new JSONObject(employeesJsonObjectInString);
					employeeBalanceAmountArray = employeeJSONObject.getJSONArray("EmployeeDetailList");
					if (employeeBalanceAmountArray != null && employeeBalanceAmountArray.length() > 0) {
						return employeeBalanceAmountArray.getJSONObject(0).opt("Amount");
					}
				} catch (Exception e) {
					LogUtility.writeLog("exception 1 >>" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
					throw new RuntimeException(e);
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("AdvanceUtility getBalanceAmountDetails Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException("Some error occured, Please try after some time");
		}
		return 0;
	}

	private Object getAmountReturnDetails(Object employeeId) throws JSONException {
		JSONArray advanceDetails = getReturnAmountDetails(employeeId);
		double amount = 0.0;
		for (int counter = 0; counter < advanceDetails.length(); counter++) {
			JSONObject object = advanceDetails.getJSONObject(counter);
			amount += Translator.doubleValue(object.opt("amount"));
		}
		return amount;
	}

	private JSONArray getReturnAmountDetails(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		JSONArray array = new JSONArray();
		array.put("amount");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "salaryyearid>3 and salarycomponentid.salarycomponenttypeid=" + HRISApplicationConstants.SalaryComponentTypes.ADVANCE + " AND employeeid=" + employeeId);
		query.put(Data.Query.OPTION_FILTERS, "salaryyearid = 3 and salarymonthid = 12 and salarycomponentid.salarycomponenttypeid=" + HRISApplicationConstants.SalaryComponentTypes.ADVANCE + " AND employeeid=" + employeeId);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("employeemonthlysalarycomponents");
		return employeeArray;
	}

	private JSONArray getAdvanceDetails(Object employeeId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceAmount.RESOURCE);
		JSONArray array = new JSONArray();
		array.put(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		array.put(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + "=" + employeeId + " and " + HRISApplicationConstants.AdvanceAmount.REQUEST_DATE + ">='2012-12-01' and " + HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR + "=" + HRISConstants.LEAVE_APPROVED);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray(HRISApplicationConstants.AdvanceAmount.RESOURCE);
		return employeeArray;
	}
}
