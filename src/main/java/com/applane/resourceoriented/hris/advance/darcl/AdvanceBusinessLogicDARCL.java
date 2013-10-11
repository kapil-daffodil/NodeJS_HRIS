package com.applane.resourceoriented.hris.advance.darcl;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.InterAppCommunication;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.procurement.Procurement;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class AdvanceBusinessLogicDARCL implements OperationJob {

	private static final int BU_HR = 1;
	private static final int DIRECTOR = 2;
	private static final int SENIOR_HR = 3;
	private static final int CMD = 4;

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			boolean isDARCL = false;
			int darcl = 606;
			// int darcl = 5; // testing in daffodil
			if (currentOrganizationId == darcl) {
				isDARCL = true;
			}
			if (record.has("temp") || !isDARCL) {
				return;
			}
			String operationType = record.getOperationType();
			Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);

			LogUtility.writeError("before) insert values >> " + record.getValues());

			if (operationType.equals(Updates.Types.INSERT)) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_PROCEEDING, 1);
			} else if (operationType.equals(Updates.Types.UPDATE)) {
				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR)) {
					int status = Translator.integerValue(record.getNewValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR));
					if (status == HRISConstants.LEAVE_APPROVED && record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR) == null || record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR) == JSONObject.NULL) {
						throw new BusinessLogicException("Amount is Can Not Be Null.");
					}
					processOnSeniorApproval(record, currentUserEmail);
				} else if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR)) {
					processOnDirectorAndBuHrApproval(record, currentUserEmail);
				}
			}
			LogUtility.writeError("aftet) insert values >> " + record.getValues());
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AdvanceBusinessLogic >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured.");
		}
	}

	private void processOnDirectorAndBuHrApproval(Record record, Object currentUserEmail) throws Exception {
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);

		int status = Translator.integerValue(record.getNewValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR));
		if (status == HRISConstants.LEAVE_APPROVED) {
			workOnApproval(record, approverIdArray, status);
		} else if (status == HRISConstants.LEAVE_REJECTED) {
			workOnReject(record, approverIdArray, status);
		}
	}

	private void workOnReject(Record record, JSONArray approverIdArray, int status) throws JSONException, Exception {
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		// JSONArray directorIdArray = getDirectorArray(approverId);
		// int directorId = getDirectorId(approverId, directorIdArray);
		JSONArray hodApprovedSummary = getHodApprovedSummaryForBuHrAndDirector(record.getKey(), approverId);
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			if (hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID) != null) {
				String statusTemp = hodApprovedSummary.getJSONObject(0).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "");
				throw new BusinessLogicException("You Have Already " + statusTemp + ".");
			}
			boolean isBuHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
			boolean isDirector = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
			boolean isSeniorHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
			boolean isCmd = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD));

			record.addUpdate(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS, status);

			if (isBuHr) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_PROCEEDING, 2);
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, BU_HR);
				if (status == HRISConstants.LEAVE_APPROVED && record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == null || record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == JSONObject.NULL) {
					throw new BusinessLogicException("Amount is Can Not Be Null.");
				}
			} else if (isDirector) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_PROCEEDING, 2);
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, DIRECTOR);
				if (status == HRISConstants.LEAVE_APPROVED && record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == null || record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == JSONObject.NULL) {
					throw new BusinessLogicException("Amount is Can Not Be Null.");
				}
			} else if (isSeniorHr) {
				record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR);
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, SENIOR_HR);
				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)) {
					record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
				}
			} else if (isCmd) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, CMD);
				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)) {
					record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
				}
			} else {
				throw new BusinessLogicException("You Are Not Authorized.");
			}
		} else {
			record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR);
			throw new BusinessLogicException("You Have Already Rejected.");
		}
	}

	private void workOnApproval(Record record, JSONArray approverIdArray, int status) throws JSONException, Exception {
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		// JSONArray directorIdArray = getDirectorArray(approverId);
		// int directorId = getDirectorId(approverId, directorIdArray);
		JSONArray hodApprovedSummary = getHodApprovedSummaryForBuHrAndDirector(record.getKey(), approverId);
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			if (hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID) != null) {
				String statusTemp = hodApprovedSummary.getJSONObject(0).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "");
				throw new BusinessLogicException("You Have Already " + statusTemp + ".");
			}
			boolean isBuHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
			boolean isDirector = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
			boolean isSeniorHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
			boolean isCmd = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD));

			record.addUpdate(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS, status);
			if (isBuHr) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, BU_HR);
				record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR);
				if (status == HRISConstants.LEAVE_APPROVED && record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == null || record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == JSONObject.NULL) {
					throw new BusinessLogicException("Amount is Can Not Be Null.");
				}
			} else if (isDirector) {

				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, DIRECTOR);
				if (status == HRISConstants.LEAVE_APPROVED && record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == null || record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == JSONObject.NULL) {
					throw new BusinessLogicException("Amount is Can Not Be Null.");
				} else if (status == HRISConstants.LEAVE_APPROVED) {
					double amount = Translator.doubleValue(record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT));
					if (amount >= 75000) {
						record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR);
					}
				}
			} else if (isSeniorHr) {
				record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR);
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, SENIOR_HR);
				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)) {
					record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
				}
			} else if (isCmd) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_BU_HR, CMD);
				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)) {
					record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
				}
			} else {
				throw new BusinessLogicException("You Are Not Authorized.");
			}
		} else {
			record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR);
			throw new BusinessLogicException("You Have Already Approved.");
		}
	}

	private JSONArray getHodApprovedSummaryForBuHrAndDirector(Object advanceId, int approverId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + "=" + advanceId + " AND " + HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + "=" + approverId + " AND " + HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER + "=1");
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		return directorIdArray;
	}

	private JSONArray getHodApprovedSummaryForBuHrAndDirectorForUpdate(Object advanceId, Object object) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + "=" + advanceId + " AND " + HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + "=" + object + " AND " + HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER + "=1");
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		return directorIdArray;
	}

	private void processOnSeniorApproval(Record record, Object currentUserEmail) throws Exception, JSONException {
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);

		int status = Translator.integerValue(record.getNewValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR));
		if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR) || record.has(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR)) {

			if (status == HRISConstants.LEAVE_REJECTED) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_PROCEEDING, 2);
			}

			int approverId = 0;
			if (approverIdArray != null && approverIdArray.length() > 0) {
				approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
			}

			JSONArray hodApprovedSummary = getHodApprovedSummary(record.getKey(), approverId);
			boolean isReportingTo = false;
			boolean isGurrenter = false;
			// Object statusUpdated = null;
			String statusTemp = "";
			if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
				isReportingTo = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
				isGurrenter = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));
				// statusUpdated = hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID);
				statusTemp = hodApprovedSummary.getJSONObject(0).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", null);
			}
			JSONArray directorIdArray = new JSONArray();

			if (statusTemp != null) {
				throw new BusinessLogicException("You Have Already " + statusTemp + ".");
			} else if (isGurrenter) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.IS_GURRENTER, 1);
				directorIdArray = getDirectorArray(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID));
			} else if (isReportingTo) {
				directorIdArray = getDirectorArray(approverId);
			} else {
				throw new BusinessLogicException("You are not authorized.");
			}
			int reportingToId = 0;
			if (directorIdArray != null && directorIdArray.length() > 0) {
				reportingToId = Translator.integerValue(directorIdArray.getJSONObject(0).opt("reportingtoid"));
			}
			int directorId = getDirectorId(reportingToId, directorIdArray);

			record.addUpdate(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS, status);
			if (directorId == reportingToId) {
				record.addUpdate(HRISApplicationConstants.AdvanceAmount.TEMP_IS_DIRECTOR, 1);
			} else {
				record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR);
			}

		}
	}

	private JSONArray getHodApprovedSummaryForReportingTo(Object advanceId, int approverId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + "=" + advanceId + " AND " + HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + "=" + approverId);// + " AND " + HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO + "=1");
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		return directorIdArray;
	}

	private JSONArray getHodApprovedSummary(Object advanceId, int approverId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name");
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + "=" + advanceId + " AND " + HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + "=" + approverId);
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		return directorIdArray;
	}

	private int getDirectorId(int approverId, JSONArray directorIdArray) throws JSONException {
		int directorId = 0;
		if (directorIdArray != null && directorIdArray.length() > 0) {
			Object profitCentersObject = directorIdArray.getJSONObject(0).opt("profitcenterid");
			JSONArray profitCenterArray = new JSONArray();
			if (profitCentersObject instanceof JSONArray) {
				profitCenterArray = (JSONArray) profitCentersObject;
			} else if (profitCentersObject instanceof JSONObject) {
				profitCenterArray.put((JSONObject) profitCentersObject);
			}
			if (profitCenterArray != null && profitCenterArray.length() > 0) {
				for (int counter = 0; counter < profitCenterArray.length(); counter++) {
					Object directorIdObject = profitCenterArray.getJSONObject(counter).opt("director_id");
					if (directorIdObject != null) {
						int directorIdTemp = Translator.integerValue(directorIdObject);
						if (directorIdTemp != 0) {
							if (directorIdTemp == approverId) {
								return directorIdTemp;
							} else {
								directorId = directorIdTemp;
							}
						}
					}
				}
			}
		}
		return directorId;
	}

	private JSONArray getReportingToArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		columnArray.put("name");
		columnArray.put("employeecode");
		columnArray.put("officialemailid");
		columnArray.put("reportingtoid");
		columnArray.put("reportingtoid.officialemailid");
		columnArray.put("reportingtoid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + "=" + employeeId);
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);

		return directorIdArray;
	}

	private JSONArray getDirectorArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		// columnArray.put("profitcenterid.director_id");
		// columnArray.put("profitcenterid.director_id.officialemailid");

		JSONArray childColumns = new JSONArray();
		childColumns.put("director_id");
		childColumns.put("director_id.officialemailid");
		childColumns.put("director_id.name");

		childColumns.put("hr_emails_id.officialemailid");
		childColumns.put("hr_emails_id.name");
		childColumns.put("hr_emails_id");

		childColumns.put("central_hr.officialemailid");
		childColumns.put("central_hr.name");
		childColumns.put("central_hr");

		childColumns.put("cmd_id.officialemailid");
		childColumns.put("cmd_id.name");
		childColumns.put("cmd_id");

		JSONObject directorIdColumnArray = new JSONObject();
		directorIdColumnArray.put("profitcenterid", childColumns);

		columnArray.put(directorIdColumnArray);
		columnArray.put("reportingtoid");
		columnArray.put("reportingtoid.officialemailid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + "=" + employeeId);
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);

		return directorIdArray;
	}

	private JSONArray getApproverIdArray(Object currentUserEmail) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		columnArray.put(Updates.KEY);
		columnArray.put(HrisKinds.Employess.NAME);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + "='" + currentUserEmail + "'");
		JSONArray employeeArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		return employeeArray;
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		boolean isDARCL = false;
		int darcl = 606;
		// int darcl = 5; // testing in daffodil
		if (currentOrganizationId == darcl) {
			isDARCL = true;
		}
		if (record.has("temp") || !isDARCL) {
			return;
		}
		Object key = record.getKey();
		try {
			String operationType = record.getOperationType();
			if (operationType.equals(Updates.Types.INSERT)) {
				sendMailOnInsert(record);
			} else if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
				int status = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS)); // temp Approver Id Used As Status
				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR)) {
					if (status == HRISConstants.LEAVE_APPROVED) {
						workOnUpdates(record);
					} else if (status == HRISConstants.LEAVE_REJECTED) {
						workOnUpdatesOnSeniorReject(record);
					}
				} else if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)) {
					if (status == HRISConstants.LEAVE_APPROVED) {
						workOnUpdatesOnDirecterOnApproval(record);
					} else if (status == HRISConstants.LEAVE_REJECTED) {
						workOnUpdatesOnDirecterOnReject(record);
					}

				} else if (record.has(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY)) {
					workOnUpdatesOnSeniorReject(record);
				} else if (record.has(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR)) {
					if (status == HRISConstants.LEAVE_APPROVED) {
						workOnUpdatesOnDirecterOnApproval(record);
					} else if (status == HRISConstants.LEAVE_REJECTED) {
						workOnUpdatesOnDirecterOnReject(record);
					}
				}
				if (record.has(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS) && record.getNewValue(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS) != null && record.getNewValue(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS) != JSONObject.NULL) {
					workOnEmiMonths(record);
				}
			}
		} catch (BusinessLogicException e) {
			try {
				insertEmiMonths(key);
			} catch (JSONException e1) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				LogUtility.writeLog("AdvanceBusinessLogic insertEmiMonths >> Exception Trace >> " + trace);
				throw new BusinessLogicException("Some Unknown Error Occured.");
			}
			throw e;
		} catch (Exception e) {
			try {
				insertEmiMonths(key);
			} catch (JSONException e1) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				LogUtility.writeLog("AdvanceBusinessLogic insertEmiMonths >> Exception Trace >> " + trace);
				throw new BusinessLogicException("Some Unknown Error Occured.");
			}
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AdvanceBusinessLogic After >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured.");
		}
	}

	private void workOnUpdatesOnSeniorReject(Record record) throws Exception {
		Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}

		JSONArray hodApprovedSummary = getHodApprovedSummaryForReportingTo(record.getKey(), approverId);
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			boolean isGurrenter = hodApprovedSummary.getJSONObject(0).optBoolean(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER);
			boolean isreportingTo = hodApprovedSummary.getJSONObject(0).optBoolean(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO);
			if (isGurrenter || isreportingTo) {
				int reportingToId = 0;
				String reportingToEmailId = "";
				String reportingToname = "";
				String approverName = "";
				String approverEmail = "";
				Object approverCode = "";
				JSONArray reportingToArray = new JSONArray();
				if (isGurrenter) {
					reportingToArray = getReportingToArray(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID));
				} else {
					reportingToArray = getReportingToArray(approverId);
				}
				if (reportingToArray != null && reportingToArray.length() > 0) {
					reportingToname = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
					reportingToEmailId = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.officialemailid"));
					approverName = Translator.stringValue(reportingToArray.getJSONObject(0).opt("name"));
					approverEmail = Translator.stringValue(reportingToArray.getJSONObject(0).opt("officialemailid"));
					reportingToId = Translator.integerValue(reportingToArray.getJSONObject(0).opt("reportingtoid"));
					approverCode = reportingToArray.getJSONObject(0).opt("employeecode");
				}

				Object summaryKey = hodApprovedSummary.getJSONObject(0).opt(Data.Query.KEY);
				int isApprover = 0;
				updateSummaryOfAdvance(summaryKey, record, isApprover, reportingToId);
				sendMailToEmployeeAndReportingTo(record, reportingToEmailId, reportingToname, approverName, approverEmail, approverId, approverCode);
			}
		}
	}

	private void sendMailToEmployeeAndReportingTo(Record record, String reportingToEmailId, String reportingToname, String approverName, String approverEmail, int approverId, Object approverCode) throws Exception {

		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		// Object requesterEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".officialemailid");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		// Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY);
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);

		String title = "FWD: Request For Advance";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		String reportingTonameEmploye = "";
		String employeeEmailId = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
			employeeEmailId = Translator.stringValue(reportingToArray.getJSONObject(0).opt("officialemailid"));
		}

		StringBuffer messageContents = new StringBuffer();

		messageContents.append("Dear ").append(reportingToname).append("<BR />");

		messageContents.append(approverName).append(" (").append(approverCode).append(") Has Rejected Your Advance Request").append("<BR /><BR />");
		messageContents.append("<b>Remarks:</b> ").append(remarksorHistory);
		if (summaryDetails != null && summaryDetails.length() > 0) {
			messageContents.append("<table border='1' width='99%'>");
			messageContents.append("<tr>");
			messageContents.append("<th width='20%'>").append("Emp. Code").append("</th>");
			messageContents.append("<th width='20%'>").append("Employee").append("</th>");
			messageContents.append("<th width='20%'>").append("Position").append("</th>");
			messageContents.append("<th width='20%'>").append("Emp. Status").append("</th>");
			messageContents.append("<th width='35%'>").append("Remarks").append("</th>");
			messageContents.append("<th width='15%'>").append("Approved Amount").append("</th>");
			messageContents.append("<th width='15%'>").append("Status").append("</th>");
			messageContents.append("<th width='15%'>").append("Date").append("</th>");
			messageContents.append("</tr>");
			for (int i = 0; i < summaryDetails.length(); i++) {
				String employeeStatus = "";

				boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
				boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
				boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
				boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
				boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
				boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

				if (isEmployee) {
					employeeStatus = "Employee";
				} else if (isReportingTo) {
					employeeStatus = "H.O.D.";
				} else if (isBuHr) {
					employeeStatus = "BU. HR";
				} else if (isSeniorHr) {
					employeeStatus = "HR. Head Office";
				} else if (isDirector) {
					employeeStatus = "Approving Authority";
				} else if (isGurrenter) {
					employeeStatus = "Guarantor";
				}

				messageContents.append("<tr>");
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
				messageContents.append("<td>").append(employeeStatus).append("</td>");

				Object lastAdvance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
				Object balance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
				Object dateOfCleared = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

				int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
				if (empId == approverId) {
					String status = "";
					int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
					if (statusTemp == HRISConstants.LEAVE_APPROVED) {
						status = "Approved";
					} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
						status = "Rejected";
					} else {
						status = "Not Know";
					}
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
					messageContents.append("<td>").append("").append("</td>");
					messageContents.append("<td>").append(status).append("</td>");

				} else {
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
				}
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
				messageContents.append("</tr>");
			}
			messageContents.append("</table>");
		}
		messageContents.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContents.append("<BR />Employee Details").append("<BR /><BR />");
		messageContents.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContents.append("Dear ").append(reportingTonameEmploye);
		messageContents.append("<BR /><BR />");
		messageContents.append(requesterName).append(" Has Requested For Advance.<br>");

		messageContents.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
		messageContents.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
		messageContents.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
		messageContents.append("<li><B>Reason : </B>").append(remarks).append("</li>");
		messageContents.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
		messageContents.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
		messageContents.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
		messageContents.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
		messageContents.append("<BR />");
		messageContents.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
		messageContents.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

		PunchingUtility.sendMailsAccToProfitCenters(new String[] { employeeEmailId }, messageContents.toString(), title, new String[] { "" + approverEmail });

	}

	private void sendMailToEmployeeAndDirectorBuHr(Record record, String reportingToEmailId, String reportingToname, String approverName, String approverEmail, int approverId, String approverCode) throws Exception {

		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		// Object requesterEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".officialemailid");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		// Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY);
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);

		String title = "FWD: Request For Advance Rejected";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		String reportingTonameEmploye = "";
		String employeeEmailId = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
			employeeEmailId = Translator.stringValue(reportingToArray.getJSONObject(0).opt("officialemailid"));
		}

		StringBuffer messageContents = new StringBuffer();

		messageContents.append("Dear ").append(requesterName).append("<BR /><BR />");

		messageContents.append(approverName).append(" (").append(approverCode).append(") Has Rejected Your Advance Request").append("<BR /><BR />");
		messageContents.append("<b>Remarks:</b> ").append(remarksorHistory);
		if (summaryDetails != null && summaryDetails.length() > 0) {
			messageContents.append("<table border='1' width='99%'>");
			messageContents.append("<tr>");
			messageContents.append("<th width='20%'>").append("Emp. Code").append("</th>");
			messageContents.append("<th width='20%'>").append("Employee").append("</th>");
			messageContents.append("<th width='20%'>").append("Position").append("</th>");
			messageContents.append("<th width='20%'>").append("Emp. Status").append("</th>");
			messageContents.append("<th width='35%'>").append("Remarks").append("</th>");
			messageContents.append("<th width='15%'>").append("Approved Amount").append("</th>");
			messageContents.append("<th width='15%'>").append("Status").append("</th>");
			messageContents.append("<th width='15%'>").append("Date").append("</th>");
			messageContents.append("</tr>");
			for (int i = 0; i < summaryDetails.length(); i++) {
				String employeeStatus = "";

				boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
				boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
				boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
				boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
				boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
				boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

				if (isEmployee) {
					employeeStatus = "Employee";
				} else if (isReportingTo) {
					employeeStatus = "H.O.D.";
				} else if (isBuHr) {
					employeeStatus = "BU. HR";
				} else if (isSeniorHr) {
					employeeStatus = "HR. Head Office";
				} else if (isDirector) {
					employeeStatus = "Approving Authority";
				} else if (isGurrenter) {
					employeeStatus = "Guarantor";
				}

				messageContents.append("<tr>");
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
				messageContents.append("<td>").append(employeeStatus).append("</td>");

				Object lastAdvance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
				Object balance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
				Object dateOfCleared = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

				int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
				if (empId == approverId) {
					String status = "";
					int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
					if (statusTemp == HRISConstants.LEAVE_APPROVED) {
						status = "Approved";
					} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
						status = "Rejected";
					} else {
						status = "Not Know";
					}
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
					messageContents.append("<td>").append("").append("</td>");
					messageContents.append("<td>").append(status).append("</td>");

				} else {
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
					messageContents.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
				}
				messageContents.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
				messageContents.append("</tr>");
			}
			messageContents.append("</table>");
		}
		messageContents.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContents.append("<BR />Employee Details").append("<BR /><BR />");
		messageContents.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContents.append("Dear ").append(reportingTonameEmploye);
		messageContents.append("<BR /><BR />");
		messageContents.append(requesterName).append(" Has Requested For Advance.<br>");

		messageContents.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
		messageContents.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
		messageContents.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
		messageContents.append("<li><B>Reason : </B>").append(remarks).append("</li>");
		messageContents.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
		messageContents.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
		messageContents.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
		messageContents.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
		messageContents.append("<BR />");
		messageContents.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
		messageContents.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

		PunchingUtility.sendMailsAccToProfitCenters(new String[] { employeeEmailId }, messageContents.toString(), title, new String[] { "" + approverEmail });

	}

	private void workOnUpdatesOnDirecterOnReject(Record record) throws Exception {
		Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		String approverName = "";
		String reportingToName = "";
		String reportingToEmailId = "";
		String approverEmail = "";
		String approverCode = "";
		JSONArray reportingToArray = getReportingToArray(approverId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			approverName = Translator.stringValue(reportingToArray.getJSONObject(0).opt("name"));
			approverEmail = Translator.stringValue(reportingToArray.getJSONObject(0).opt("officialemailid"));
			reportingToEmailId = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.officialemailid"));
			reportingToName = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
			approverCode = Translator.stringValue(reportingToArray.getJSONObject(0).opt("employeecode"));
		}
		JSONArray hodApprovedSummary = getHodApprovedSummaryForBuHrAndDirectorForUpdate(record.getKey(), approverId);
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			Object summaryKey = hodApprovedSummary.getJSONObject(0).opt(Data.Query.KEY);
			int isBuHr = 4;
			updateSummaryOfAdvance(summaryKey, record, isBuHr, 0);
			String approverStatus = "" + record.getValue(HRISApplicationConstants.AdvanceAmount.IS_BU_HR);
			if (approverStatus.equals("" + BU_HR)) {
				sendMailToEmployeeAndDirectorBuHr(record, reportingToEmailId, reportingToName, approverName, approverEmail, approverId, approverCode);
			} else if (approverStatus.equals("" + DIRECTOR)) {
				sendMailToEmployeeAndDirectorBuHr(record, reportingToEmailId, reportingToName, approverName, approverEmail, approverId, approverCode);
			} else if (approverStatus.equals("" + SENIOR_HR)) {
				sendMailToBuHrOnReject(record, reportingToName, reportingToEmailId, approverName, approverEmail, approverId, approverCode);
			} else if (approverStatus.equals("" + CMD)) {
				sendMailToEmployeeAndDirectorBuHr(record, reportingToEmailId, reportingToName, approverName, approverEmail, approverId, approverCode);
			}
		}
	}

	private void sendMailToBuHrOnReject(Record record, String reportingToname, String reportingToEmailId, String approverName, String approverEmail, int approverId, String approverCode) throws Exception {
		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		// Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY);
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);

		String title = "FWD: Request For Advance Rejected";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		String reportingTonameEmploye = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
		}
		// insertIntoSummaryReportingToOnInsert(record, isEmployee, reportingToId);
		int[] size = { 0 };
		List<String> directorEmailList = new ArrayList<String>();
		List<String> directorNameList = new ArrayList<String>();
		List<Object> directorIdList = new ArrayList<Object>();

		List<String> buHrEmailList = new ArrayList<String>();
		List<String> buHrNameList = new ArrayList<String>();
		List<Object> buHrIDS = new ArrayList<Object>();

		List<String> centralHrEmailList = new ArrayList<String>();
		List<String> centralHrNameList = new ArrayList<String>();
		List<Object> centralHrIDS = new ArrayList<Object>();

		getEmailAndNameOfDirectorList(employeeId, size, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);
		int buHr = 3;
		StringBuffer messageContentsHeader = new StringBuffer();
		StringBuffer messageContentsLink = new StringBuffer();
		StringBuffer messageContentsFooter = new StringBuffer();

		// messageContentsHeader.append("Dear Sir / Mam");
		List<Integer> list = new ArrayList<Integer>();
		for (int counter = 0; counter < buHrIDS.size(); counter++) {
			if (!list.contains(Translator.integerValue(buHrIDS.get(counter)))) {
				list.add(Translator.integerValue(buHrIDS.get(counter)));
				updateSummaryOfAdvance(null, record, buHr, Translator.integerValue(buHrIDS.get(counter)));
			}
			// if (counter > 0) {
			// messageContents.append(" / ");
			// }
			// messageContents.append(buHrNameList.get(counter));
		}
		for (int counter = 0; counter < buHrEmailList.size(); counter++) {

			messageContentsHeader.append("Dear Sir / Mam");
			messageContentsHeader.append("<BR />");
			messageContentsHeader.append(approverName).append(" (").append(approverCode).append(") Has Rejected Amount For ").append(requesterName).append("<BR /><BR />");
			messageContentsHeader.append("<b>Remarks:</b> ").append(remarksorHistory);
			messageContentsLink.append(getLinkForBuHr(record.getKey(), buHrEmailList.get(counter), directorEmailList));

			if (summaryDetails != null && summaryDetails.length() > 0) {
				messageContentsHeader.append("<table border='1' width='99%'>");
				messageContentsHeader.append("<tr>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Code").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Employee").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Position").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Status").append("</th>");
				messageContentsHeader.append("<th width='35%'>").append("Remarks").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Approved Amount").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Status").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Date").append("</th>");
				messageContentsHeader.append("</tr>");
				for (int i = 0; i < summaryDetails.length(); i++) {
					String employeeStatus = "";

					boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
					boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
					boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
					boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
					boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
					boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

					if (isEmployee) {
						employeeStatus = "Employee";
					} else if (isReportingTo) {
						employeeStatus = "H.O.D.";
					} else if (isBuHr) {
						employeeStatus = "BU. HR";
					} else if (isSeniorHr) {
						employeeStatus = "HR. Head Office";
					} else if (isDirector) {
						employeeStatus = "Approving Authority";
					} else if (isGurrenter) {
						employeeStatus = "Guarantor";
					}

					messageContentsHeader.append("<tr>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
					messageContentsHeader.append("<td>").append(employeeStatus).append("</td>");

					Object lastAdvance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
					Object balance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
					Object dateOfCleared = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

					int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
					if (empId == approverId) {
						String status = "";
						int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
						if (statusTemp == HRISConstants.LEAVE_APPROVED) {
							status = "Approved";
						} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
							status = "Rejected";
						} else {
							status = "Not Know";
						}
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
						messageContentsHeader.append("<td>").append("").append("</td>");
						messageContentsHeader.append("<td>").append(status).append("</td>");

					} else {
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
					}
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
					messageContentsHeader.append("</tr>");
				}
				messageContentsHeader.append("</table>");
			}
			messageContentsFooter.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			messageContentsFooter.append("<BR />Employee Details").append("<BR /><BR />");
			messageContentsFooter.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
			messageContentsFooter.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			messageContentsFooter.append("Dear ").append(reportingTonameEmploye);
			messageContentsFooter.append("<BR /><BR />");
			messageContentsFooter.append(requesterName).append(" Has Requested For Advance.<br>");

			messageContentsFooter.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
			messageContentsFooter.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
			messageContentsFooter.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
			messageContentsFooter.append("<li><B>Reason : </B>").append(remarks).append("</li>");
			messageContentsFooter.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
			messageContentsFooter.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
			messageContentsFooter.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
			messageContentsFooter.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
			messageContentsFooter.append("<BR />");
			messageContentsFooter.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
			messageContentsFooter.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

			String contentsForBuHr = messageContentsFooter.toString() + messageContentsLink.toString() + messageContentsFooter.toString();
			PunchingUtility.sendMailsWithoutCC(new String[] { buHrEmailList.get(counter) }, contentsForBuHr, title);
		}
		String contentsForApprover = messageContentsFooter.toString() + messageContentsLink.toString() + messageContentsFooter.toString();
		PunchingUtility.sendMailsWithoutCC(new String[] { approverEmail }, contentsForApprover, title);
		// PunchingUtility.sendMailsAccToProfitCenters(emails, messageContentsHeader.toString(), title, new String[] { "" + approverEmail });
	}

	private void workOnEmiMonths(Record record) throws Exception {
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");
		Object branchid = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".branchid");
		int emiForMonths = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS));
		Object fromDate = record.getValue(HRISApplicationConstants.AdvanceAmount.FROM_DATE);
		Object dueMonthId = record.getValue(HRISApplicationConstants.AdvanceAmount.DUE_MONTH_ID);
		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		double approveByDirectorAmount, advanceAmount = approveByDirectorAmount = Translator.doubleValue(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR));
		approveByDirectorAmount = (double) approveByDirectorAmount / emiForMonths;
		Calendar cal = Calendar.getInstance();
		cal.setTime(Translator.dateValue(fromDate));
		cal.add(Calendar.MONTH, emiForMonths);
		cal.add(Calendar.DAY_OF_MONTH, -1);
		Date toDate = cal.getTime();
		String toDateStr = EmployeeSalaryGenerationServlet.getDateInString(toDate);
		String filter = SalaryComponentKinds.COMPONENT_TYPE_ID + " = " + HRISApplicationConstants.SalaryComponentTypes.ADVANCE;
		Object componentId = getComponentId(filter);
		if (componentId != null) {
			String invoiceDate = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date currentDate = Translator.dateValue(invoiceDate);
			cal.setTime(currentDate);

			String monthName = EmployeeSalaryGenerationServlet.getMonthName((cal.get(Calendar.MONTH) + 1));
			String yearName = "" + cal.get(Calendar.YEAR);
			String invoiceNumber = requesterEmployeeCode + "/" + monthName + "/" + yearName + "/Advance";
			Object profitcenterid = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".profitcenterid");
			if (profitcenterid != null && profitcenterid instanceof JSONArray && ((JSONArray) profitcenterid).length() > 0) {
				profitcenterid = ((JSONArray) profitcenterid).get(0);
			}
			insertAdvanceInvoice("" + requesterName, branchid, profitcenterid, currentDate, invoiceNumber, advanceAmount);
			EmployeeNameBusinessLogic.insertEmployeeSalaryPackageComponentsDetails(null, employeeId, componentId, fromDate, toDateStr, approveByDirectorAmount, dueMonthId, 0.0, 0.0);
		} else {
			sendEmailIfComponentNull(fromDate, toDateStr, approveByDirectorAmount, requesterName, requesterEmployeeCode);
			throw new BusinessLogicException("Advance Type Component Not Fount In Salary Components.(Error Emailed To User.)");
		}
	}

	private void workOnUpdatesOnDirecterOnApproval(Record record) throws Exception {
		Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		String approverName = "";
		String approverCode = "";
		JSONArray reportingToArray = getReportingToArray(approverId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			approverName = Translator.stringValue(reportingToArray.getJSONObject(0).opt("name"));
			approverCode = Translator.stringValue(reportingToArray.getJSONObject(0).opt("employeecode"));
		}
		JSONArray hodApprovedSummary = getHodApprovedSummaryForBuHrAndDirectorForUpdate(record.getKey(), approverId);
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			Object summaryKey = hodApprovedSummary.getJSONObject(0).opt(Data.Query.KEY);
			int isBuHr = 4;
			updateSummaryOfAdvance(summaryKey, record, isBuHr, 0);
			String approverStatus = "" + record.getValue(HRISApplicationConstants.AdvanceAmount.IS_BU_HR);
			if (approverStatus.equals("" + BU_HR)) {
				sendMailToHrManager(record, approverName, approverId, approverCode);
			} else if (approverStatus.equals("" + SENIOR_HR)) {
				sendMailToDirector(record, approverName, approverId, approverCode);
			} else if (approverStatus.equals("" + DIRECTOR)) {
				double amount = Translator.doubleValue(record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT));
				if (amount >= 75000) {
					sendEmailToCMD(record, approverName, approverId, approverCode);
				}
			} else if (approverStatus.equals("" + CMD)) {
				// TODO send email to employee after approve
			}
		}
	}

	private void sendEmailToCMD(Record record, String approverName, int approverId, String approverCode) throws Exception {

		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterBranchName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".branchid.name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);

		Object loneRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS);
		Object intrestPercentage = record.getValue(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE);

		// Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		// Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR);
		Object employeeCtc = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".ctc") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".ctc");
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);
		Object directorEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.DIRECTOR_EMAIL) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.DIRECTOR_EMAIL);
		String gurrenterName = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".name"));
		String gurrenterBranchName = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".branchid.name"));
		String gurrenterCode = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".employeecode"));
		int genderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".genderid"));
		int employeeGenderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".genderid"));
		String gender = "";
		String employeeGender = "";
		String employeeGender1 = "";
		if (genderId == 1) {
			gender = "Mr. ";
		} else if (genderId == 2) {
			gender = "Ms. ";
		} else {
			gender = "Mr./Ms. ";
		}
		if (employeeGenderId == 1) {
			employeeGender = "Mr. ";
			employeeGender1 = "His ";
		} else if (employeeGenderId == 2) {
			employeeGender = "Ms. ";
			employeeGender1 = "Her ";
		} else {
			employeeGender = "Mr./Ms. ";
			employeeGender1 = "His/Her ";
		}
		String title = "FWD: Request For Advance";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		String reportingTonameEmploye = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
		}

		// object.put("cmdEmailIdObject", cmdEmailIdObject);
		// object.put("cmdNameObject", cmdNameObject);
		// object.put("cmdIdObject", cmdIdObject);

		JSONArray cmdArray = getCmdEmailArray(employeeId);
		if (cmdArray != null && cmdArray.length() > 0) {

			int cmd = 7;
			StringBuffer messageContents = new StringBuffer();
			StringBuffer messageContentsHeader = new StringBuffer();
			StringBuffer messageContentsLink = new StringBuffer();
			StringBuffer messageContentsFooter = new StringBuffer();
			StringBuffer messageContentsRegards = new StringBuffer();

			messageContentsHeader.append("Dear Sir/Madam").append("<BR />");

			messageContentsHeader.append(employeeGender).append(" ").append(requesterName).append(" (").append(requesterEmployeeCode).append(") working in").append(requesterBranchName).append(" has entered an advance request of Rs. ").append(requestAmount).append("/- only. <b><i><u>EMI</u></i></b ").append(emiRemarks).append(", <b><i><u>Due to</u></i></b> ").append(remarks).append(".").append("<BR /><BR />");
			messageContentsHeader.append(gender).append(" ").append(gurrenterName).append(" (").append(gurrenterCode).append(") working in ").append(gurrenterBranchName).append(" has been seleced as guarantor in this request.").append("<BR /><BR />");
			messageContentsHeader.append(employeeGender1).append(" previous loan details is as below:<BR /><BR />");

			messageContentsHeader.append("CTC as on date(In Rs.): ").append(employeeCtc).append("/- only <BR />");

			messageContentsHeader.append("Last Advance Amount Taken: ").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("<BR />");
			messageContentsHeader.append("Balance Amount: ").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("<BR />");
			messageContentsHeader.append("Date of Last Advance Cleared: ").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("<BR />");

			updateSummaryOfAdvance(null, record, cmd, Translator.integerValue(cmdArray.getJSONObject(0).opt("cmdIdObject")));

			messageContentsHeader.append("<b>Lone Remarks (From: SBI or Other Bank):</b> ").append(loneRemarks).append("<BR />");
			messageContentsHeader.append("<b>Intrest:</b> ").append(intrestPercentage).append("%").append("<BR />");
			messageContentsLink.append(getLinkForHrManager(record.getKey(), cmdArray.getJSONObject(0).opt("cmdEmailIdObject"))).append("<BR />");
			if (summaryDetails != null && summaryDetails.length() > 0) {
				messageContentsHeader.append("<table border='1' width='99%'>");
				messageContentsHeader.append("<tr>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Code").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Employee").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Position").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Status").append("</th>");
				messageContentsHeader.append("<th width='35%'>").append("Remarks").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Approved Amount").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Status").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Date").append("</th>");
				messageContentsHeader.append("</tr>");
				for (int i = 0; i < summaryDetails.length(); i++) {
					String employeeStatus = "";

					boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
					boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
					boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
					boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
					boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
					boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

					if (isEmployee) {
						employeeStatus = "Employee";
					} else if (isReportingTo) {
						employeeStatus = "H.O.D.";
					} else if (isBuHr) {
						employeeStatus = "BU. HR";
					} else if (isSeniorHr) {
						employeeStatus = "HR. Head Office";
					} else if (isDirector) {
						employeeStatus = "Approving Authority";
					} else if (isGurrenter) {
						employeeStatus = "Guarantor";
					}

					messageContentsHeader.append("<tr>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
					messageContentsHeader.append("<td>").append(employeeStatus).append("</td>");

					Object lastAdvanceSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
					Object balanceSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
					Object dateOfClearedSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

					int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
					boolean isConsider = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER));
					if (empId == approverId && isConsider) {
						String status = "";
						int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
						if (statusTemp == HRISConstants.LEAVE_APPROVED) {
							status = "Approved";
						} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
							status = "Rejected";
						} else {
							status = "Not Know";
						}
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvanceSummary == null ? "" : ("<BR /> LAT.: " + lastAdvanceSummary)).append(balanceSummary == null ? "" : ("<BR /> Bal.:" + balanceSummary)).append(dateOfClearedSummary == null ? "" : ("<BR /> DOC.:" + dateOfClearedSummary)).append("</td>");
						messageContentsHeader.append("<td>").append(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)).append("</td>");
						messageContentsHeader.append("<td>").append(status).append("</td>");

					} else {
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvanceSummary == null ? "" : ("<BR /> LAT.: " + lastAdvanceSummary)).append(balanceSummary == null ? "" : ("<BR /> Bal.:" + balanceSummary)).append(dateOfClearedSummary == null ? "" : ("<BR /> DOC.:" + dateOfClearedSummary)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
					}
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
					messageContentsHeader.append("</tr>");
				}
				messageContentsHeader.append("</table><BR />");
			}

			JSONArray approverIdArray = getApproverIdArray(directorEmail);
			String directorName = "";
			if (approverIdArray != null && approverIdArray.length() > 0) {
				directorName = Translator.stringValue(approverIdArray.getJSONObject(0).opt(HrisKinds.Employess.NAME));
			}

			messageContentsHeader.append("Mr./Ms. ").append(directorName).append(" has also given their approval on this request. You are requested to please give your approval on the same.");

			messageContentsFooter.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			messageContentsFooter.append("<BR />Employee Details").append("<BR /><BR />");
			messageContentsFooter.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
			messageContentsFooter.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			messageContentsFooter.append("Dear ").append(reportingTonameEmploye);
			messageContentsFooter.append("<BR /><BR />");
			messageContentsFooter.append(requesterName).append(" Has Requested For Advance.<br>");

			messageContentsFooter.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
			messageContentsFooter.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
			messageContentsFooter.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
			messageContentsFooter.append("<li><B>Reason : </B>").append(remarks).append("</li>");
			messageContentsFooter.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
			messageContentsFooter.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
			messageContentsFooter.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
			messageContentsFooter.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
			messageContentsFooter.append("<BR />");
			messageContentsRegards.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
			messageContentsRegards.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

			String contertForHrManager = messageContents.toString() + messageContentsHeader.toString() + messageContentsLink.toString() + messageContentsFooter.toString() + messageContentsRegards.toString();

			PunchingUtility.sendMailsWithoutCC(new String[] { "" + cmdArray.getJSONObject(0).opt("cmdEmailIdObject") }, contertForHrManager, title);
			try {
				messageContents = new StringBuffer();
				messageContents.append("Respected Sir / Madam").append(" <BR /><BR />");
				messageContents.append("You have approved advance request of ").append(employeeGender).append(" ").append(requesterName).append(" (Emp. Code: ").append(requesterEmployeeCode).append(") details as below:");

				messageContentsRegards = new StringBuffer();
				messageContentsRegards.append("Regards <BR /> ").append("Applane Team").append("<BR />");
				messageContentsRegards.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

				String contentForDirector = messageContents.toString() + messageContentsHeader.toString() + messageContentsFooter.toString() + messageContentsRegards.toString();
				PunchingUtility.sendMailsWithoutCC(new String[] { "" + directorEmail }, contentForDirector, title);
			} catch (Exception e) {
				LogUtility.writeLog("AdvanceBusinessLogic Exception >> mail Sending To CMD: Exception >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			}
		}
	}

	private JSONArray getCmdEmailArray(Object employeeId) throws Exception {

		JSONArray directorIdArray = getDirectorArray(employeeId);
		if (directorIdArray != null && directorIdArray.length() > 0) {

			if (directorIdArray != null && directorIdArray.length() > 0) {
				Object profitCentersObject = directorIdArray.getJSONObject(0).opt("profitcenterid");
				JSONArray profitCenterArray = new JSONArray();
				if (profitCentersObject instanceof JSONArray) {
					profitCenterArray = (JSONArray) profitCentersObject;
				} else if (profitCentersObject instanceof JSONObject) {
					profitCenterArray.put((JSONObject) profitCentersObject);
				}
				if (profitCenterArray != null && profitCenterArray.length() > 0) {

					Object cmdEmailIdObject = profitCenterArray.getJSONObject(0).opt("cmd_id.officialemailid");
					Object cmdNameObject = profitCenterArray.getJSONObject(0).opt("cmd_id.name");
					Object cmdIdObject = profitCenterArray.getJSONObject(0).opt("cmd_id");
					JSONObject object = new JSONObject();
					object.put("cmdEmailIdObject", cmdEmailIdObject);
					object.put("cmdNameObject", cmdNameObject);
					object.put("cmdIdObject", cmdIdObject);
					return new JSONArray().put(object);
				}
			} else {
				return null;
			}
		}
		return null;
	}

	private void sendMailToHrManager(Record record, String approverName, int approverId, String approverCode) throws Exception {

		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);

		Object loneRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS);
		Object intrestPercentage = record.getValue(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE);

		// Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		// Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR);
		// Object branchId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".branchid") == null ? 0 : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".branchid");
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);
		// Object directorEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.DIRECTOR_EMAIL) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.DIRECTOR_EMAIL);
		// String gurrenterName = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".name"));
		int genderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".genderid"));
		int employeeGenderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".genderid"));
		// String gender = "";
		String employeeGender = "";
		// String employeeGender1 = "";
		if (genderId == 1) {
			// gender = "Mr. ";
		} else if (genderId == 2) {
			// gender = "Ms. ";
		} else {
			// gender = "Mr./Ms. ";
		}
		if (employeeGenderId == 1) {
			employeeGender = "Mr. ";
			// employeeGender1 = "His ";
		} else if (employeeGenderId == 2) {
			employeeGender = "Ms. ";
			// employeeGender1 = "Her ";
		} else {
			employeeGender = "Mr./Ms. ";
			// employeeGender1 = "His/Her ";
		}
		String title = "FWD: Request For Advance";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		String reportingTonameEmploye = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
		}
		int[] size = { 0 };
		List<String> directorEmailList = new ArrayList<String>();
		List<String> directorNameList = new ArrayList<String>();
		List<Object> directorIdList = new ArrayList<Object>();

		List<String> buHrEmailList = new ArrayList<String>();
		List<String> buHrNameList = new ArrayList<String>();
		List<Object> buHrIDS = new ArrayList<Object>();

		List<String> centralHrEmailList = new ArrayList<String>();
		List<String> centralHrNameList = new ArrayList<String>();
		List<Object> centralHrIDS = new ArrayList<Object>();

		getEmailAndNameOfDirectorList(employeeId, size, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);

		if (centralHrEmailList.size() > 0) {

			int seniorHrManager = 5;
			StringBuffer messageContents = new StringBuffer();
			StringBuffer messageContentsHeader = new StringBuffer();
			StringBuffer messageContentsLink = new StringBuffer();
			StringBuffer messageContentsFooter = new StringBuffer();
			StringBuffer messageContentsRegards = new StringBuffer();
			// messageContentsHeader.append("Dear Sir/Madam").append(centralHrNameList.get(0));
			messageContents.append("Dear Sir/Madam").append("<BR /><BR />");
			messageContents.append("Please find enclosed herewith advance application of ").append(employeeGender).append(" ").append(requesterName).append(" (").append(requesterEmployeeCode).append(")<BR /><BR />");
			messageContents.append("Detail as below:").append("<BR /><BR />");
			// messageContentsHeader.append("<b>Guarantor : </b>").append(gurrenterName);

			updateSummaryOfAdvance(null, record, seniorHrManager, Translator.integerValue(centralHrIDS.get(0)));

			String[] directorAndBuHrEmails = new String[(directorEmailList.size() + buHrEmailList.size())];
			int counter = 0;
			for (counter = 0; counter < directorEmailList.size(); counter++) {
				directorAndBuHrEmails[counter] = directorEmailList.get(counter);
			}
			for (int buHrCounter = 0; buHrCounter < buHrEmailList.size(); buHrCounter++) {
				directorAndBuHrEmails[counter] = buHrEmailList.get(buHrCounter);
				counter++;
			}

			// messageContentsHeader.append("<BR />");
			// messageContentsHeader.append(approverName).append(" (").append(approverCode).append(") Has Approved Amount For ").append(requesterName).append(" (").append(requesterEmployeeCode).append(")<BR /><BR />");
			messageContentsHeader.append("<b>Lone Remarks (From: SBI or Other Bank):</b> ").append(loneRemarks).append("<BR />");
			messageContentsHeader.append("<b>Intrest:</b> ").append(intrestPercentage).append("%");
			messageContentsLink.append(getLinkForHrManager(record.getKey(), centralHrEmailList.get(0))).append("<BR />");
			if (summaryDetails != null && summaryDetails.length() > 0) {
				messageContentsHeader.append("<table border='1' width='99%'>");
				messageContentsHeader.append("<tr>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Code").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Employee").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Position").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Status").append("</th>");
				messageContentsHeader.append("<th width='35%'>").append("Remarks").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Approved Amount").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Status").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Date").append("</th>");
				messageContentsHeader.append("</tr>");
				for (int i = 0; i < summaryDetails.length(); i++) {
					String employeeStatus = "";

					boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
					boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
					boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
					boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
					boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
					boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

					if (isEmployee) {
						employeeStatus = "Employee";
					} else if (isReportingTo) {
						employeeStatus = "H.O.D.";
					} else if (isBuHr) {
						employeeStatus = "BU. HR";
					} else if (isSeniorHr) {
						employeeStatus = "HR. Head Office";
					} else if (isDirector) {
						employeeStatus = "Approving Authority";
					} else if (isGurrenter) {
						employeeStatus = "Guarantor";
					}

					messageContentsHeader.append("<tr>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
					messageContentsHeader.append("<td>").append(employeeStatus).append("</td>");

					Object lastAdvanceSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
					Object balanceSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
					Object dateOfClearedSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

					int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
					boolean isConsider = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER));
					if (empId == approverId && isConsider) {
						String status = "";
						int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
						if (statusTemp == HRISConstants.LEAVE_APPROVED) {
							status = "Approved";
						} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
							status = "Rejected";
						} else {
							status = "Not Know";
						}
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvanceSummary == null ? "" : ("<BR /> LAT.: " + lastAdvanceSummary)).append(balanceSummary == null ? "" : ("<BR /> Bal.:" + balanceSummary)).append(dateOfClearedSummary == null ? "" : ("<BR /> DOC.:" + dateOfClearedSummary)).append("</td>");
						messageContentsHeader.append("<td>").append(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)).append("</td>");
						messageContentsHeader.append("<td>").append(status).append("</td>");

					} else {
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvanceSummary == null ? "" : ("<BR /> LAT.: " + lastAdvanceSummary)).append(balanceSummary == null ? "" : ("<BR /> Bal.:" + balanceSummary)).append(dateOfClearedSummary == null ? "" : ("<BR /> DOC.:" + dateOfClearedSummary)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
					}
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
					messageContentsHeader.append("</tr>");
				}
				messageContentsHeader.append("</table>");
			}
			messageContentsFooter.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			messageContentsFooter.append("<BR />Employee Details").append("<BR /><BR />");
			messageContentsFooter.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
			messageContentsFooter.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			messageContentsFooter.append("Dear ").append(reportingTonameEmploye);
			messageContentsFooter.append("<BR /><BR />");
			messageContentsFooter.append(requesterName).append(" Has Requested For Advance.<br>");

			messageContentsFooter.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
			messageContentsFooter.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
			messageContentsFooter.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
			messageContentsFooter.append("<li><B>Reason : </B>").append(remarks).append("</li>");
			messageContentsFooter.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
			messageContentsFooter.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
			messageContentsFooter.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
			messageContentsFooter.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
			messageContentsFooter.append("<BR />");
			messageContentsRegards.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
			messageContentsRegards.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

			String contertForHrManager = messageContents.toString() + messageContentsHeader.toString() + messageContentsLink.toString() + messageContentsFooter.toString() + messageContentsRegards.toString();

			PunchingUtility.sendMailsWithoutCC(new String[] { centralHrEmailList.get(0) }, contertForHrManager, title);
			try {
				messageContents = new StringBuffer();
				messageContents.append("Respected Sir / Madam").append(" <BR /><BR />");
				messageContents.append("You have approved advance request of ").append(employeeGender).append(" ").append(requesterName).append(" (Emp. Code: ").append(requesterEmployeeCode).append(") details as below:");

				messageContentsRegards = new StringBuffer();
				messageContentsRegards.append("Regards <BR /> ").append("Applane Team").append("<BR />");
				messageContentsRegards.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

				String contentForDirector = messageContents.toString() + messageContentsHeader.toString() + messageContentsFooter.toString() + messageContentsRegards.toString();
				PunchingUtility.sendMailsWithoutCC(directorAndBuHrEmails, contentForDirector, title);
			} catch (Exception e) {
				LogUtility.writeLog("AdvanceBusinessLogic Exception >> mail Sending To BU and Director: Exception >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			}
		}
	}

	private void sendMailToDirector(Record record, String approverName, int approverId, String approverCode) throws Exception {
		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		// Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		// Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR);
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);

		Object lastAdvance = record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP);
		Object balance = record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP);
		Object dateOfCleared = record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP);

		Object loneRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS);
		Object intrestPercentage = record.getValue(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE);

		Object ctc = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".ctc");
		Object directorEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.DIRECTOR_EMAIL);
		String gurrenterName = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".name"));
		String gurrenterCode = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".employeecode"));
		int genderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".genderid"));
		int employeeGenderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".genderid"));

		String gender = "";
		String employeeGender = "";
		String employeeGender1 = "";
		if (genderId == 1) {
			gender = "Mr. ";
		} else if (genderId == 2) {
			gender = "Ms. ";
		} else {
			gender = "Mr./Ms. ";
		}
		if (employeeGenderId == 1) {
			employeeGender = "Mr. ";
			employeeGender1 = "His ";
		} else if (employeeGenderId == 2) {
			employeeGender = "Ms. ";
			employeeGender1 = "Her ";
		} else {
			employeeGender = "Mr./Ms. ";
			employeeGender1 = "His/Her ";
		}
		JSONArray approverIdArray = getApproverIdArray(directorEmail);
		int directorId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			directorId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		String title = "FWD: Request For Advance";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		String reportingTonameEmploye = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
		}
		// insertIntoSummaryReportingToOnInsert(record, isEmployee, reportingToId);
		int[] size = { 0 };
		List<String> directorEmailList = new ArrayList<String>();
		List<String> directorNameList = new ArrayList<String>();
		List<Object> directorIdList = new ArrayList<Object>();

		List<String> buHrEmailList = new ArrayList<String>();
		List<String> buHrNameList = new ArrayList<String>();
		List<Object> buHrIDS = new ArrayList<Object>();

		List<String> centralHrEmailList = new ArrayList<String>();
		List<String> centralHrNameList = new ArrayList<String>();
		List<Object> centralHrIDS = new ArrayList<Object>();

		getEmailAndNameOfDirectorList(employeeId, size, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);
		int director = 6;
		StringBuffer messageContentsHeader = new StringBuffer();
		StringBuffer messageContentsLink = new StringBuffer();
		StringBuffer messageContentsFooter = new StringBuffer();
		updateSummaryOfAdvance(null, record, director, directorId);
		// }
		String[] buHrEmails = new String[buHrEmailList.size()];
		for (int counter = 0; counter < buHrEmailList.size(); counter++) {
			buHrEmails[counter] = buHrEmailList.get(counter);
		}

		messageContentsHeader = new StringBuffer();
		messageContentsLink = new StringBuffer();
		messageContentsFooter = new StringBuffer();

		messageContentsHeader.append("Respected Sir / Madam");
		messageContentsHeader.append("<BR /><BR />");
		messageContentsHeader.append(employeeGender).append(" ").append(requesterName).append("(").append(requesterEmployeeCode).append(") Has entered an advance request of Rs.  ").append(requestAmount).append(", <b><i><u>EMI</u></i></b> ").append(emiRemarks).append(", <b><i><u>Due to:</u></i></b> ").append(remarks).append(". ").append(gender).append(" ").append(gurrenterName).append("(").append(gurrenterCode).append(") has been selected as guarantor in this request.").append("<BR /><BR />");

		messageContentsHeader.append(employeeGender1).append(" previous Advance detail is as below:").append(requesterName).append("<BR />");

		messageContentsHeader.append("<b>CTC as on date(In Rs.):</b> ").append(ctc == null ? "" : ctc).append("<BR />");
		messageContentsHeader.append("<b>Last Advance Amount Taken:</b> ").append(lastAdvance == null ? "" : lastAdvance).append("<BR />");
		messageContentsHeader.append("<b>Balance Amount(If Any):</b> ").append(balance == null ? "" : balance).append("<BR />");
		messageContentsHeader.append("<b>Date of Last Advance Cleared:</b> ").append(dateOfCleared == null ? "" : dateOfCleared).append("<BR /><BR />");
		messageContentsHeader.append("<b>Lone Remarks(From: SBI ot Other Bank):</b> ").append(loneRemarks == null ? "" : loneRemarks).append("<BR />");
		messageContentsHeader.append("<b>Interest:</b> ").append(intrestPercentage == null ? "" : intrestPercentage).append("%<BR /><BR />");

		// messageContentsHeader.append("<b>").append(approverName.trim()).append("'s</b> Remarks: ").append("<BR />");
		// messageContentsHeader.append("<b>Amount:</b> ").append(approvedAmount).append("<BR />");
		// messageContentsHeader.append("<b>Remarks:</b> ").append(remarksorHistory).append("<BR />");
		// messageContentsLink.append(getLinkForDirector(record.getKey(), directorEmailList.get(counter)));
		messageContentsLink.append("You are requested to please give your approval on the same.").append("<BR />");
		messageContentsLink.append(getLinkForDirector(record.getKey(), directorEmail));

		if (summaryDetails != null && summaryDetails.length() > 0) {
			messageContentsHeader.append("<table border='1' width='99%'>");
			messageContentsHeader.append("<tr>");
			messageContentsHeader.append("<th width='20%'>").append("Emp. Code").append("</th>");
			messageContentsHeader.append("<th width='20%'>").append("Employee").append("</th>");
			messageContentsHeader.append("<th width='20%'>").append("Position").append("</th>");
			messageContentsHeader.append("<th width='20%'>").append("Emp. Status").append("</th>");
			messageContentsHeader.append("<th width='35%'>").append("Remarks").append("</th>");
			messageContentsHeader.append("<th width='15%'>").append("Approved Amount").append("</th>");
			messageContentsHeader.append("<th width='15%'>").append("Status").append("</th>");
			messageContentsHeader.append("<th width='15%'>").append("Date").append("</th>");
			messageContentsHeader.append("</tr>");
			for (int i = 0; i < summaryDetails.length(); i++) {
				String employeeStatus = "";

				boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
				boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
				boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
				boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
				boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
				boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

				if (isEmployee) {
					employeeStatus = "Employee";
				} else if (isReportingTo) {
					employeeStatus = "H.O.D.";
				} else if (isBuHr) {
					employeeStatus = "BU. HR";
				} else if (isSeniorHr) {
					employeeStatus = "HR. Head Office";
				} else if (isDirector) {
					employeeStatus = "Approving Authority";
				} else if (isGurrenter) {
					employeeStatus = "Guarantor";
				}

				messageContentsHeader.append("<tr>");
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
				messageContentsHeader.append("<td>").append(employeeStatus).append("</td>");

				Object lastAdvanceSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
				Object balanceSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
				Object dateOfClearedSummary = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

				int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
				boolean isConsider = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER));
				if (empId == approverId && isConsider) {
					String status = "";
					int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
					if (statusTemp == HRISConstants.LEAVE_APPROVED) {
						status = "Approved";
					} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
						status = "Rejected";
					} else {
						status = "Not Know";
					}
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvanceSummary == null ? "" : ("<BR /> LAT.: " + lastAdvanceSummary)).append(balanceSummary == null ? "" : ("<BR /> Bal.:" + balanceSummary)).append(dateOfClearedSummary == null ? "" : ("<BR /> DOC.:" + dateOfClearedSummary)).append("</td>");
					messageContentsHeader.append("<td>").append(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)).append("</td>");
					messageContentsHeader.append("<td>").append(status).append("</td>");

				} else {
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvanceSummary == null ? "" : ("<BR /> LAT.: " + lastAdvanceSummary)).append(balanceSummary == null ? "" : ("<BR /> Bal.:" + balanceSummary)).append(dateOfClearedSummary == null ? "" : ("<BR /> DOC.:" + dateOfClearedSummary)).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
				}
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
				messageContentsHeader.append("</tr>");
			}
			messageContentsHeader.append("</table>");
		}

		messageContentsFooter.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContentsFooter.append("<BR />Employee Details").append("<BR /><BR />");
		messageContentsFooter.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContentsFooter.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContentsFooter.append("Dear ").append(reportingTonameEmploye);
		messageContentsFooter.append("<BR /><BR />");
		messageContentsFooter.append(requesterName).append(" Has Requested For Advance.<br>");

		messageContentsFooter.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
		messageContentsFooter.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
		messageContentsFooter.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
		messageContentsFooter.append("<li><B>Reason : </B>").append(remarks).append("</li>");
		messageContentsFooter.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
		messageContentsFooter.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
		messageContentsFooter.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
		messageContentsFooter.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
		messageContentsFooter.append("<BR />");
		messageContentsFooter.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
		messageContentsFooter.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

		String contentForDirector = messageContentsHeader.toString() + messageContentsLink.toString() + messageContentsFooter.toString();
		PunchingUtility.sendMailsWithoutCC(new String[] { "" + directorEmail }, contentForDirector, title);
		String contentForBuHr = messageContentsHeader.toString() + messageContentsFooter.toString();
		PunchingUtility.sendMailsWithoutCC(buHrEmails, contentForBuHr, title);
		// PunchingUtility.sendMailsAccToProfitCenters(directorEmails, messageContentsHeader.toString(), title, buHrEmails);
	}

	private void workOnUpdates(Record record) throws Exception, JSONException {
		Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}

		JSONArray hodApprovedSummary = getHodApprovedSummaryForReportingTo(record.getKey(), approverId);
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			boolean isGurrenter = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));
			boolean isreportingTo = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
			if (isGurrenter || isreportingTo) {
				int reportingToId = 0;
				String reportingToEmailId = "";
				String reportingToname = "";
				String approverName = "";
				String approverEmail = "";
				String approverCode = "";
				JSONArray reportingToArray = new JSONArray();
				if (record.has(HRISApplicationConstants.AdvanceAmount.IS_GURRENTER)) {
					reportingToArray = getReportingToArray(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID));
				} else {
					reportingToArray = getReportingToArray(approverId);
				}
				if (reportingToArray != null && reportingToArray.length() > 0) {
					reportingToname = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
					reportingToEmailId = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.officialemailid"));
					approverName = Translator.stringValue(reportingToArray.getJSONObject(0).opt("name"));
					approverEmail = Translator.stringValue(reportingToArray.getJSONObject(0).opt("officialemailid"));
					reportingToId = Translator.integerValue(reportingToArray.getJSONObject(0).opt("reportingtoid"));
					approverCode = Translator.stringValue(reportingToArray.getJSONObject(0).opt("employeecode"));
				}

				Object summaryKey = hodApprovedSummary.getJSONObject(0).opt(Data.Query.KEY);
				int isApprover = 0;
				int isReportingTo = 1;
				// int isDirector = 2;
				updateSummaryOfAdvance(summaryKey, record, isApprover, reportingToId);
				if (!record.has(HRISApplicationConstants.AdvanceAmount.TEMP_IS_DIRECTOR)) {
					sendMailToReportingTo(record, reportingToEmailId, reportingToId, reportingToname, approverName, approverEmail, approverId, approverCode);
					updateSummaryOfAdvance(summaryKey, record, isReportingTo, reportingToId);
				} else {
					// updateSummaryOfAdvance(summaryKey, record, isDirector, reportingToId);
					updateAdvanceDetailsEmailedToBuHR(record.getKey());
					sendMailToBuHr(record, reportingToname, reportingToEmailId, approverName, approverEmail, approverId, approverCode);
				}
			}
		}
	}

	private void updateAdvanceDetailsEmailedToBuHR(Object key) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceAmount.RESOURCE);
		JSONObject row = new JSONObject();

		row.put(Updates.KEY, key);
		row.put(HRISApplicationConstants.AdvanceAmount.EMAILED_TO_BU_HR, 1);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private void sendMailToReportingTo(Record record, String reportingToEmailId, int reportingToId, String reportingToname, String approverName, String approverEmail, int approverId, String approverCode) throws Exception {
		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		// Object requesterEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".officialemailid");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY);
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);
		String gurrenterName = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".name"));
		String gurrenterCode = Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".employeecode"));

		int genderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".genderid"));
		int employeeGenderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".genderid"));
		String gender = "";
		String gender1 = "";
		String employeeGender = "";
		String employeeGender1 = "";
		if (genderId == 1) {
			gender = "Mr. ";
			gender1 = "His ";
		} else if (genderId == 2) {
			gender = "Ms. ";
			gender1 = "Her ";
		} else {
			gender = "Mr./Ms. ";
			gender1 = "His/Her ";
		}
		if (employeeGenderId == 1) {
			employeeGender = "Mr. ";
			employeeGender1 = "His ";
		} else if (employeeGenderId == 2) {
			employeeGender = "Ms. ";
			employeeGender1 = "Her ";
		} else {
			employeeGender = "Mr./Ms. ";
			employeeGender1 = "His/Her ";
		}

		String title = "FWD: Request For Advance";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		String reportingTonameEmploye = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
		}

		StringBuffer messageContentsHeader = new StringBuffer();
		StringBuffer messageContentsLink = new StringBuffer();
		StringBuffer messageContentsFooter = new StringBuffer();

		messageContentsHeader.append("Dear Sir/Madam").append("<BR />");

		messageContentsHeader.append(employeeGender).append(" ").append(requesterName).append(" (").append(requesterEmployeeCode).append(") has entered an advance request of Rs. ").append(requestAmount).append("/- only. <b><i><u>Due to</u></i></b> ").append(remarks).append(", <b><i><u>EMI</u></i></b ").append(emiRemarks).append(".").append("<BR /><BR />");
		messageContentsHeader.append(gender).append(" ").append(gurrenterName).append(" (").append(gurrenterCode).append(") has been seleced as guarantor in this request and ").append(gender1).append(" consent has also been received.<BR /><BR />");
		messageContentsHeader.append(employeeGender1).append(" previous loan details is as below:<BR /><BR />");

		messageContentsHeader.append("Last Advance Amount Taken: ").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("<BR />");
		messageContentsHeader.append("Balance Amount: ").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("<BR />");
		messageContentsHeader.append("Date of Last Advance Cleared: ").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("<BR />");

		messageContentsHeader.append("<BR />").append(approverName.trim()).append("'s Remarks:").append("<BR />");
		messageContentsHeader.append("<b>Amount:</b> ").append(approvedAmount).append("<BR />");
		messageContentsHeader.append("<b>Remarks:</b> ").append(remarksorHistory).append("<BR />");

		messageContentsLink.append("You are requested to please give your remarks on the same.").append("<BR />");
		messageContentsLink.append(getLinkForReportingTo(record.getKey(), reportingToEmailId));

		if (summaryDetails != null && summaryDetails.length() > 0) {
			messageContentsHeader.append("<table border='1' width='99%'>");
			messageContentsHeader.append("<tr>");
			messageContentsHeader.append("<th width='20%'>").append("Emp. Code").append("</th>");
			messageContentsHeader.append("<th width='20%'>").append("Employee").append("</th>");
			messageContentsHeader.append("<th width='20%'>").append("Position").append("</th>");
			messageContentsHeader.append("<th width='20%'>").append("Emp. Status").append("</th>");
			messageContentsHeader.append("<th width='35%'>").append("Remarks").append("</th>");
			messageContentsHeader.append("<th width='15%'>").append("Approved Amount").append("</th>");
			messageContentsHeader.append("<th width='15%'>").append("Status").append("</th>");
			messageContentsHeader.append("<th width='15%'>").append("Date").append("</th>");
			messageContentsHeader.append("</tr>");
			for (int i = 0; i < summaryDetails.length(); i++) {
				String employeeStatus = "";

				boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
				boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
				boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
				boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
				boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
				boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

				if (isEmployee) {
					employeeStatus = "Employee";
				} else if (isReportingTo) {
					employeeStatus = "H.O.D.";
				} else if (isBuHr) {
					employeeStatus = "BU. HR";
				} else if (isSeniorHr) {
					employeeStatus = "HR. Head Office";
				} else if (isDirector) {
					employeeStatus = "Approving Authority";
				} else if (isGurrenter) {
					employeeStatus = "Guarantor";
				}

				messageContentsHeader.append("<tr>");
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
				messageContentsHeader.append("<td>").append(employeeStatus).append("</td>");

				Object lastAdvance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
				Object balance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
				Object dateOfCleared = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

				int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
				if (empId == approverId) {
					String status = "";
					int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
					if (statusTemp == HRISConstants.LEAVE_APPROVED) {
						status = "Approved";
					} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
						status = "Rejected";
					} else {
						status = "Not Know";
					}
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
					messageContentsHeader.append("<td>").append(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR)).append("</td>");
					messageContentsHeader.append("<td>").append(status).append("</td>");

				} else {
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
				}
				messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
				messageContentsHeader.append("</tr>");
			}
			messageContentsHeader.append("</table>");
		}

		messageContentsFooter.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContentsFooter.append("<BR />Employee Details").append("<BR /><BR />");
		messageContentsFooter.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContentsFooter.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContentsFooter.append("Dear ").append(reportingTonameEmploye);
		messageContentsFooter.append("<BR /><BR />");
		messageContentsFooter.append(requesterName).append(" Has Requested For Advance.<br>");

		messageContentsFooter.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
		messageContentsFooter.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
		messageContentsFooter.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
		messageContentsFooter.append("<li><B>Reason : </B>").append(remarks).append("</li>");
		messageContentsFooter.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
		messageContentsFooter.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
		messageContentsFooter.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
		messageContentsFooter.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
		messageContentsFooter.append("<BR />");
		messageContentsFooter.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
		messageContentsFooter.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

		String contectForReportingTo = messageContentsHeader.toString() + messageContentsLink.toString() + messageContentsFooter.toString();
		String contectForApprover = messageContentsHeader.toString() + messageContentsFooter.toString();

		PunchingUtility.sendMailsWithoutCC(new String[] { reportingToEmailId }, contectForReportingTo, title);
		PunchingUtility.sendMailsWithoutCC(new String[] { "" + approverEmail }, contectForApprover, title);

		// PunchingUtility.sendMailsAccToProfitCenters(new String[] { reportingToEmailId }, messageContentsHeader.toString(), title, new String[] { "" + approverEmail });
	}

	private JSONArray getSummaryDetails(Object key) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode");
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name");
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name");
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name");
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE);

		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD);

		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
		columnArray.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + "=" + key);
		JSONArray summeryArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);

		return summeryArray;
	}

	private void sendMailToBuHr(Record record, String reportingToname, String reportingToEmailId, String approverName, String approverEmail, int approverId, String approverCode) throws Exception {
		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		// Object approvedAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		// Object remarksorHistory = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY);
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);

		String title = "FWD: Request For Advance";

		Object key = record.getKey();
		JSONArray summaryDetails = getSummaryDetails(key);

		// String reportingTonameEmploye = "";
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			// reportingTonameEmploye = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
		}
		// insertIntoSummaryReportingToOnInsert(record, isEmployee, reportingToId);
		int[] size = { 0 };
		List<String> directorEmailList = new ArrayList<String>();
		List<String> directorNameList = new ArrayList<String>();
		List<Object> directorIdList = new ArrayList<Object>();

		List<String> buHrEmailList = new ArrayList<String>();
		List<String> buHrNameList = new ArrayList<String>();
		List<Object> buHrIDS = new ArrayList<Object>();

		List<String> centralHrEmailList = new ArrayList<String>();
		List<String> centralHrNameList = new ArrayList<String>();
		List<Object> centralHrIDS = new ArrayList<Object>();

		getEmailAndNameOfDirectorList(employeeId, size, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);
		int buHr = 3;
		StringBuffer messageContentsHeader = new StringBuffer();
		StringBuffer messageContentsLink = new StringBuffer();
		StringBuffer messageContentsFooter = new StringBuffer();

		List<Integer> list = new ArrayList<Integer>();
		for (int counter = 0; counter < buHrIDS.size(); counter++) {
			if (!list.contains(Translator.integerValue(buHrIDS.get(counter)))) {
				list.add(Translator.integerValue(buHrIDS.get(counter)));
				updateSummaryOfAdvance(null, record, buHr, Translator.integerValue(buHrIDS.get(counter)));
			}
			// if (counter > 0) {
			// messageContents.append(" / ");
			// }
			// messageContents.append(buHrNameList.get(counter));
		}
		String[] emails = new String[buHrEmailList.size()];
		for (int counter = 0; counter < buHrEmailList.size(); counter++) {
			emails[counter] = buHrEmailList.get(counter);

			messageContentsHeader = new StringBuffer();
			messageContentsLink = new StringBuffer();
			messageContentsFooter = new StringBuffer();

			messageContentsHeader.append("Dear Sir / Mam");
			messageContentsHeader.append("<BR />");
			// messageContentsHeader.append(approverName).append(" Has Approved Amount For <b>").append(requesterName).append("</b><BR /><BR />");
			// messageContentsHeader.append("<b>Amount:</b> ").append(approvedAmount).append("<BR />");
			// messageContentsHeader.append("<b>Remarks:</b> ").append(remarksorHistory);

			messageContentsLink.append(getLinkForBuHr(record.getKey(), buHrEmailList.get(counter), directorEmailList));

			if (summaryDetails != null && summaryDetails.length() > 0) {
				messageContentsHeader.append("<table border='1' width='99%'>");
				messageContentsHeader.append("<tr>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Code").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Employee").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Position").append("</th>");
				messageContentsHeader.append("<th width='20%'>").append("Emp. Status").append("</th>");
				messageContentsHeader.append("<th width='35%'>").append("Remarks").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Approved Amount").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Status").append("</th>");
				messageContentsHeader.append("<th width='15%'>").append("Date").append("</th>");
				messageContentsHeader.append("</tr>");
				for (int i = 0; i < summaryDetails.length(); i++) {
					String employeeStatus = "";

					boolean isEmployee = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE));
					boolean isReportingTo = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO));
					boolean isBuHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR));
					boolean isSeniorHr = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR));
					boolean isDirector = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR));
					boolean isGurrenter = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER));

					if (isEmployee) {
						employeeStatus = "Employee";
					} else if (isReportingTo) {
						employeeStatus = "H.O.D.";
					} else if (isBuHr) {
						employeeStatus = "BU. HR";
					} else if (isSeniorHr) {
						employeeStatus = "HR. Head Office";
					} else if (isDirector) {
						employeeStatus = "Approving Authority";
					} else if (isGurrenter) {
						employeeStatus = "Guarantor";
					}

					messageContentsHeader.append("<tr>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".employeecode")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".name")).append("</td>");
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID + ".designationid.name")).append("</td>");
					messageContentsHeader.append("<td>").append(employeeStatus).append("</td>");

					Object lastAdvance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN);
					Object balance = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE);
					Object dateOfCleared = summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED);

					int empId = Translator.integerValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID));
					boolean isConsider = Translator.booleanValue(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER));
					if (empId == approverId && isConsider) {
						String status = "";
						int statusTemp = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
						if (statusTemp == HRISConstants.LEAVE_APPROVED) {
							status = "Approved";
						} else if (statusTemp == HRISConstants.LEAVE_REJECTED) {
							status = "Rejected";
						} else {
							status = "Not Know";
						}
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
						messageContentsHeader.append("<td>").append(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR)).append("</td>");
						messageContentsHeader.append("<td>").append(status).append("</td>");

					} else {
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS)).append(lastAdvance == null ? "" : ("<BR /> LAT.: " + lastAdvance)).append(balance == null ? "" : ("<BR /> Bal.:" + balance)).append(dateOfCleared == null ? "" : ("<BR /> DOC.:" + dateOfCleared)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT)).append("</td>");
						messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).optString(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID + ".name", "")).append("</td>");
					}
					messageContentsHeader.append("<td>").append(summaryDetails.getJSONObject(i).opt(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE)).append("</td>");
					messageContentsHeader.append("</tr>");
				}
				messageContentsHeader.append("</table>");
			}
			messageContentsFooter.append("<BR /><hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			messageContentsFooter.append("<BR />Employee Details").append("<BR /><BR />");
			messageContentsFooter.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
			messageContentsFooter.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

			// messageContentsFooter.append("Dear ").append(reportingTonameEmploye);
			messageContentsFooter.append("<BR /><BR />");
			messageContentsFooter.append(requesterName).append(" Has Requested For Advance.<br>");

			messageContentsFooter.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
			messageContentsFooter.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
			messageContentsFooter.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
			messageContentsFooter.append("<li><B>Reason : </B>").append(remarks).append("</li>");
			messageContentsFooter.append("<li><B>EMI Remarks : </B>").append(emiRemarks).append("</li>");
			messageContentsFooter.append("<li><B>Last Advance Taken Amount : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");
			messageContentsFooter.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>");
			messageContentsFooter.append("<li><B>Date of Last Advance cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li></ul>");
			messageContentsFooter.append("<BR />");
			messageContentsFooter.append("Regards <BR /> ").append(approverName).append("<BR />Employee Code:").append(approverCode).append("<BR />");
			messageContentsFooter.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

			String contentForBuHr = messageContentsHeader.toString() + messageContentsLink.toString() + messageContentsFooter.toString();
			PunchingUtility.sendMailsWithoutCC(new String[] { buHrEmailList.get(counter) }, contentForBuHr, title);
		}
		String contentForApprover = messageContentsHeader.toString() + messageContentsFooter.toString();
		PunchingUtility.sendMailsWithoutCC(new String[] { approverEmail }, contentForApprover, title);
		// PunchingUtility.sendMailsAccToProfitCenters(emails, messageContentsHeader.toString(), title, new String[] { "" + approverEmail });
	}

	private void updateSummaryOfAdvance(Object summaryKey, Record record, int isEmployee, int reportingToId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);
		JSONObject row = new JSONObject();
		if (isEmployee == 0) {

			row.put(Updates.KEY, summaryKey);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT, record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR));
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID, record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 2);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS, record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY));

			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}

		} else if (isEmployee == 1) {

			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, reportingToId);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_REPORTING_TO, 1);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID, record.getKey());

			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}

		} else if (isEmployee == 2) {

			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, reportingToId);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR, 1);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID, record.getKey());
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 1);

			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}

		} else if (isEmployee == 3) { // bu hr insert

			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, reportingToId);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_BU_HR, 1);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID, record.getKey());
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 1);

			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}

		} else if (isEmployee == 4) { // bu hr or Director or sr. hr manager update

			row.put(Updates.KEY, summaryKey);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.STATUS_ID, record.getValue(HRISApplicationConstants.AdvanceAmount.TEMP_APPROVED_STATUS));
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT, record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR));
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 2);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS, record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS_FOR_HISTORY_DIRECTOR));

			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LONE_REMARKS, record.getValue(HRISApplicationConstants.AdvanceAmount.LONE_REMARKS));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.INTREST_PERCENTAGE, record.getValue(HRISApplicationConstants.AdvanceAmount.INTREST_PERCENTAGE));
			}

		} else if (isEmployee == 5) { // sr. hr manager

			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, reportingToId);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_SENIOR_HR, 1);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID, record.getKey());
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 1);

			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}

		} else if (isEmployee == 6) { // director insert

			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, reportingToId);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_DIRECTOR, 1);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID, record.getKey());
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 1);
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}

		} else if (isEmployee == 7) { // director insert

			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, reportingToId);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CMD, 1);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID, record.getKey());
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 1);
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.LAST_ADVANCE_TAKEN, record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.BALANCE, record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE_TEMP));
			}
			if (record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP) != null) {
				row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE_OF_LAST_ADVANCE_CLEARED, record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED_TEMP));
			}

		}
		row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime()));
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private void insertEmiMonths(Object key) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceAmount.RESOURCE);
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS, JSONObject.NULL);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private void insertAdvanceInvoice(String employeeName, Object branchid, Object profitcenterid, Date invoiceDate, String invoiceNumber, double salaryDifference) throws JSONException {
		JSONObject advanceObject = new JSONObject();
		advanceObject.put(Procurement.ProcurementPayments.PROFITCENTER_ID, profitcenterid);
		advanceObject.put(Procurement.ProcurementPayments.OFFICE_ID, branchid);
		advanceObject.put(Procurement.ProcurementPayments.PAYMENT_NO, invoiceNumber + "/Advance");
		advanceObject.put(Procurement.ProcurementPayments.PAYMENT_DATE, invoiceDate);
		advanceObject.put(Procurement.ProcurementPayments.VENDOR_ID, employeeName);
		advanceObject.put(Procurement.ProcurementPayments.AMOUNT, salaryDifference);

		InterAppCommunication.invokeMethod("com.applane.procurement.SalaryDisbursement", "payAdvanceSalaryOfEmployee", new Object[] { advanceObject });
	}

	private void sendEmailIfComponentNull(Object fromDate, String toDateStr, double approveByDirectorAmount, Object requesterName, Object requesterEmployeeCode) {
		StringBuffer messageContents = new StringBuffer();
		if (requesterEmployeeCode == null) {
			requesterEmployeeCode = "";
		}
		if (requesterName == null) {
			requesterName = "";
		}
		if (fromDate == null) {
			fromDate = "";
		}
		if (toDateStr == null) {
			toDateStr = "";
		}
		messageContents.append("<b>Advance Type Component Not Fount In Salary Components.</b> <BR />");
		messageContents.append("<ul><li><B>Employee Code : </B>" + requesterEmployeeCode + "</li>");
		messageContents.append("<li><B>Employee Name : </B>" + requesterName + "</li>");
		messageContents.append("<li><B>From Date : </B>" + fromDate + "</li>");
		messageContents.append("<li><B>To Date : </B>" + toDateStr + "</li>");
		messageContents.append("<li><B>Amount : </B>" + approveByDirectorAmount + "</li></ul>");
		messageContents.append("<BR />");
		messageContents.append("Regards <BR /> Applane Team<BR />");
		messageContents.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
		String title = "Error While Assigning Advance Component to " + requesterName;
		String currentUserEmail = "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		HrisHelper.sendMails(new String[] { currentUserEmail }, messageContents.toString(), title);
	}

	private Object getComponentId(String filter) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, SalaryComponentKinds.RESOURCE);
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, filter);
		JSONArray salaryComponentTypeArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(SalaryComponentKinds.RESOURCE);
		if (salaryComponentTypeArray != null && salaryComponentTypeArray.length() > 0) {
			return salaryComponentTypeArray.getJSONObject(0).opt(Updates.KEY);
		}
		return null;
	}

	private void sendMailOnInsert(Record record) throws Exception {
		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO) == null ? "" : communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME) == null ? "Organization Logo" : communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);

		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode") == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");

		Object requesterEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".officialemailid");

		Object gurrenterId = record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID);
		Object gurrenterName = record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".name");
		Object gurrenterCode = record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".employeecode");
		Object gurrenterEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".officialemailid");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);
		Object emiRemarks = record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS) == null ? "" : record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_REMARKS);

		String title = "Request For Advance";

		int reportingToId = 0;
		// String reportingToEmailId = "";
		// String reportingToname = "";
		// boolean isDirector = false;
		JSONArray reportingToArray = getReportingToArray(employeeId);
		if (reportingToArray != null && reportingToArray.length() > 0) {
			// reportingToname = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.name"));
			// reportingToEmailId = Translator.stringValue(reportingToArray.getJSONObject(0).opt("reportingtoid.officialemailid"));
			reportingToId = Translator.integerValue(reportingToArray.getJSONObject(0).opt("reportingtoid"));
		}
		int isEmployee = 0, isGurrenter = 1;
		insertIntoSummaryReportingToOnInsert(record, isEmployee, reportingToId);
		insertIntoSummaryReportingToOnInsert(record, isGurrenter, Translator.integerValue(gurrenterId));
		// insertIntoSummaryReportingToOnInsert(record, isReportingTo, reportingToId);

		// content for employee
		StringBuffer messageContentsHeader = new StringBuffer();
		StringBuffer messageContentsFooter = new StringBuffer();
		StringBuffer messageContentsLink = new StringBuffer();

		getContentForEmployee(record, organizationLogo, organizationName, requesterName, requesterEmployeeCode, gurrenterName, gurrenterEmail, requestDate, requestAmount, remarks, emiRemarks, messageContentsHeader, messageContentsFooter, messageContentsLink, gurrenterCode);

		String comtentsForEmployee = messageContentsHeader.toString() + messageContentsFooter;
		PunchingUtility.sendMailsWithoutCC(new String[] { "" + requesterEmail }, comtentsForEmployee, title);

		// content for gurrenter
		messageContentsHeader = new StringBuffer();
		messageContentsFooter = new StringBuffer();
		messageContentsLink = new StringBuffer();

		getContentForGurrenter(record, organizationLogo, organizationName, requesterName, requesterEmployeeCode, gurrenterName, gurrenterEmail, requestDate, requestAmount, remarks, emiRemarks, messageContentsHeader, messageContentsFooter, messageContentsLink);

		String comtentsForGurrenter = messageContentsHeader.toString() + messageContentsLink.toString() + messageContentsFooter;
		PunchingUtility.sendMailsWithoutCC(new String[] { "" + gurrenterEmail }, comtentsForGurrenter, title);

	}

	private void getContentForGurrenter(Record record, Object organizationLogo, Object organizationName, Object requesterName, Object requesterEmployeeCode, Object gurrenterName, Object gurrenterEmail, Object requestDate, Object requestAmount, Object remarks, Object emiRemarks, StringBuffer messageContentsHeader, StringBuffer messageContentsFooter, StringBuffer messageContentsLink) throws Exception {
		messageContentsHeader.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContentsHeader.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContentsHeader.append("Dear ").append(gurrenterName);
		messageContentsHeader.append("<BR /><BR />");
		messageContentsHeader.append("I have entered an advance request of Rs. ").append(new DecimalFormat("#.##").format(requestAmount)).append("/- only due to ").append(remarks).append(" reason. EMI ").append(emiRemarks).append(".<BR /><BR />");
		messageContentsHeader.append("My previous advance detail is as below:");

		messageContentsHeader.append("<ul><li><B>Last Advance Taken: </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.LAST_ADVANCE_TAKEN))).append("</li>");// last advance
		messageContentsHeader.append("<li><B>Balance(In Rs.) : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.BALANCE))).append("</li>"); // balance
		messageContentsHeader.append("<li><B>Date of Last Advance Cleared : </B>").append(Translator.stringValue(record.getValue(HRISApplicationConstants.AdvanceAmount.DATE_OF_LAST_ADVANCE_CLEARED))).append("</li><</ul>");// date of last advance cleared
		messageContentsHeader.append("<BR /><BR />");
		messageContentsHeader.append("Now I am selecting you as a guarantor in my advance request. Please give your remarks on this request.<BR /><BR />");

		messageContentsLink.append(getLinkForReportingTo(record.getKey(), gurrenterEmail)).append("<BR />");
		messageContentsFooter.append("Regards <BR /> ").append(record.getValue("employeeid.name")).append("<BR />Employee Code: ").append(record.getValue("employeeid.employeecode")).append("<BR />").append(record.getValue("employeeid.designationid.name")).append("<BR />");
		messageContentsFooter.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
	}

	private void getContentForEmployee(Record record, Object organizationLogo, Object organizationName, Object requesterName, Object requesterEmployeeCode, Object gurrenterName, Object gurrenterEmail, Object requestDate, Object requestAmount, Object remarks, Object emiRemarks, StringBuffer messageContentsHeader, StringBuffer messageContentsFooter, StringBuffer messageContentsLink, Object gurrenterCode) throws Exception {

		int genderId = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.GURRENTER_ID + ".genderid"));
		String gender = "";
		if (genderId == 1) {
			gender = "Mr. ";
		} else if (genderId == 2) {
			gender = "Ms. ";
		} else {
			gender = "Mr./Ms. ";
		}

		messageContentsHeader.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContentsHeader.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");

		messageContentsHeader.append("Dear ").append(requesterName);
		messageContentsHeader.append("<BR /><BR />");
		messageContentsHeader.append("You have entered an advance request of Rs. ").append(requestAmount).append(", EMI ").append(emiRemarks).append(" due to ").append(remarks).append(".<BR /><BR />");
		messageContentsHeader.append(gender).append(" ").append(gurrenterName).append(" (").append(gurrenterCode).append(") has been selected as guarantor in this request.").append("<BR /><BR />");
		messageContentsHeader.append("Your request has been forwarded to ").append(record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".reportingtoid.name"));// reporting to name
		messageContentsHeader.append("<BR />");
		messageContentsHeader.append("This is for your information.");
		messageContentsHeader.append("<BR />");
		messageContentsFooter.append("Regards <BR /> ").append(requesterName).append("<BR />Employee Code:").append(requesterEmployeeCode).append("<BR />");
		messageContentsFooter.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
	}

	private String getLinkForReportingTo(Object key, Object toEmailId) throws Exception {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		// messageContents.append("Approve or Reject leave(s):").append("<BR />");
		messageContents.append("<table border='0' width='50%'> <tr valign='bottom'><td align='left'>");
		messageContents.append("<form action/='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");
		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abs' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");// approve_by_seniour
		messageContents.append("<b>Amount:</b>");
		messageContents.append("<INPUT TYPE='text' name='ambs' ").append("' /></ br>");// approved_amount_by_seniour
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfh' ").append("' /></ br>");// remarks_for_history
		// messageContents.append("<input type='submit' style=\"font-face: 'Comic Sans MS'; font-size: 14px; color: black; background-color: #AFEEEE; border: 1.3pt ridge #5F9EA0;padding: 3px;\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td><td align='left'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");
		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abs' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");// approve_by_seniour
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfh' ").append("' /></ br>");// remarks_for_history
		// messageContents.append("<input type='submit' style=\"font-face: 'Comic Sans MS'; font-size: 14px; color: black; background-color: #AFEEEE; border: 1.3pt ridge #5F9EA0;padding: 3px;\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	private String getLinkForBuHr(Object key, Object toEmailId, List<String> directorEmailList) throws Exception {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		StringBuffer directorEmails = new StringBuffer();
		if (directorEmailList != null && directorEmailList.size() > 0) {
			directorEmails.append("<b>Select Approving Authority:</b> ").append("<select name='directorEmail'>");
			for (int directorCounter = 0; directorCounter < directorEmailList.size(); directorCounter++) {
				directorEmails.append("<option value='").append(directorEmailList.get(directorCounter)).append("'>").append(directorEmailList.get(directorCounter)).append("</option>");
			}
			directorEmails.append("</select>");
		}

		messageContents.append("<BR />");
		messageContents.append("<table border='0' width='99%'> <tr valign='bottom'><td align='left' width='100%'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");
		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abd' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");// approve_by_director

		messageContents.append("<b>Last Advance Taken Amount:</b>");
		messageContents.append("<INPUT TYPE='text' name='lat' ").append("' /><BR /><BR />");// last_advance_taken
		messageContents.append("<b>Balance Amount(If Any):</b>");
		messageContents.append("<INPUT TYPE='text' name='bal' ").append("' /><BR /><BR />");// balance
		messageContents.append("<b>Date Of Last Advance Cleared:</b>");
		messageContents.append("<INPUT TYPE='text' name='dolac' ").append("' />&nbsp;&nbsp;&nbsp;&nbsp;");// date_of_last_advance_cleared
		messageContents.append("<b>Date Format Should Be: 2013-01-01</b><BR /><BR />");
		messageContents.append("<b>Remarks of Lone (From: SBI or any Other Bank):</b>");
		messageContents.append("<INPUT TYPE='text' name='l_remarks' ").append("' /><BR /><BR />");// lone remarks
		messageContents.append("<b>Interest Percentage:</b>");
		messageContents.append("<INPUT TYPE='text' name='int_per' ").append("' /><BR /><BR />");// intrest percentage
		messageContents.append("<b>Recommendation From BU HR:</b>");
		messageContents.append("<INPUT TYPE='text' name='ambd' ").append("' />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");// approved_amount_by_director
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfhd' ").append("' /><BR />");// remarks_for_history_director
		messageContents.append(directorEmails.toString()).append("<BR />");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td></tr><tr><td align='left'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");
		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abd' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");// approve_by_director
		messageContents.append("<BR /><b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfhd' ").append("' /></ br>");// remarks_for_history_director
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	private String getLinkForHrManager(Object key, Object toEmailId) throws Exception {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		messageContents.append("<table border='0' width='50%'> <tr valign='bottom'><td align='left'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");
		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abd' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");// approve_by_director

		messageContents.append("<b>Remarks of Lone (From: SBI or any Other Bank):</b>");
		messageContents.append("<INPUT TYPE='text' name='l_remarks' ").append("' /><BR /><BR />");// lone remarks
		messageContents.append("<b>Interest Percentage:</b>");
		messageContents.append("<INPUT TYPE='text' name='int_per' ").append("' /><BR /><BR />");// interest percentage

		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfhd' ").append("' /></ br>");// remarks_for_history_director
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td><td align='left'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");

		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abd' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");// approve_by_director
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfhd' ").append("' /></ br>");// remarks_for_history_director
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	private String getLinkForDirector(Object key, Object toEmailId) throws Exception {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		messageContents.append("<table border='0' width='50%'> <tr valign='bottom'><td align='left'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");
		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abd' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");// approve_by_director

		messageContents.append("<b>Amount:</b>");
		messageContents.append("<INPUT TYPE='text' name='ambd' ").append("' /></ br>");// approved_amount_by_director
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfhd' ").append("' /></ br>");// remarks_for_history_director
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td><td align='left'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveadvance'>");
		messageContents.append("<INPUT TYPE='hidden' name='advk' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apm' VALUE='").append(toEmailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='abd' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");// approve_by_director
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rfhd' ").append("' /></ br>");// remarks_for_history_director
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	public Object getCurrentOrganization() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "up_organizations");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("organization");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + ")");
		JSONObject employeeObject;
		employeeObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray organizationArray = employeeObject.getJSONArray("up_organizations");
		if (organizationArray != null && organizationArray.length() > 0) {
			return organizationArray.getJSONObject(0).opt("organization");
		}
		return null;
	}

	private void insertIntoSummaryReportingToOnInsert(Record record, int isEmployee, int reportingToId) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);
		JSONObject row = new JSONObject();
		if (isEmployee == 0) {
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID));
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_EMPLOYEE, 1);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.REMARKS, record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS));
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.APPROVED_AMOUNT, record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT));
		} else if (isEmployee == 1) {
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.EMPLOYEE_ID, reportingToId);
			row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_GURRENTER, 1);
		}
		row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.IS_CONSIDER, 1);

		row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID, record.getKey());
		row.put(HRISApplicationConstants.AdvanceApprovedHODSummary.DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime()));
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private void getEmailAndNameOfDirectorList(Object employeeId, int[] size, List<String> emailList, List<String> nameList, List<String> buHrEmailList, List<String> buHrNameList, List<Object> buHrIDS, List<Object> directorIdList, List<String> centralHrEmailList, List<String> centralHrNameList, List<Object> centralHrIDS) throws Exception, JSONException {
		JSONArray directorIdArray = getDirectorArray(employeeId);
		if (directorIdArray != null && directorIdArray.length() > 0) {

			if (directorIdArray != null && directorIdArray.length() > 0) {
				Object profitCentersObject = directorIdArray.getJSONObject(0).opt("profitcenterid");
				JSONArray profitCenterArray = new JSONArray();
				if (profitCentersObject instanceof JSONArray) {
					profitCenterArray = (JSONArray) profitCentersObject;
				} else if (profitCentersObject instanceof JSONObject) {
					profitCenterArray.put((JSONObject) profitCentersObject);
				}
				if (profitCenterArray != null && profitCenterArray.length() > 0) {
					for (int counter = 0; counter < profitCenterArray.length(); counter++) {
						Object directorEmailIdObject = profitCenterArray.getJSONObject(counter).opt("director_id.officialemailid");
						Object directorNameObject = profitCenterArray.getJSONObject(counter).opt("director_id.name");
						Object directorIdObject = profitCenterArray.getJSONObject(counter).opt("director_id");
						Object buHrEmailId = profitCenterArray.getJSONObject(counter).opt("hr_emails_id.officialemailid");
						Object buHrName = profitCenterArray.getJSONObject(counter).opt("hr_emails_id.name");
						Object hrEmailsId = profitCenterArray.getJSONObject(counter).opt("hr_emails_id");

						Object centralHrEmailId = profitCenterArray.getJSONObject(counter).opt("central_hr.officialemailid");
						Object centralHrName = profitCenterArray.getJSONObject(counter).opt("central_hr.name");
						Object centralHrId = profitCenterArray.getJSONObject(counter).opt("central_hr");

						if (directorEmailIdObject != null && emailList != null && nameList != null) {
							size[0]++;
							emailList.add("" + directorEmailIdObject);
							nameList.add("" + directorNameObject);
							directorIdList.add(directorIdObject);
						}
						if (buHrEmailId != null && buHrEmailList != null && buHrNameList != null) {
							if (buHrEmailList != null) {
								size[0]++;
								buHrEmailList.add("" + buHrEmailId);
								buHrNameList.add("" + buHrName);
								buHrIDS.add(hrEmailsId);
							}
						}
						if (centralHrEmailList != null && centralHrIDS != null && centralHrNameList != null) {
							if (centralHrEmailList != null) {
								size[0]++;
								centralHrEmailList.add("" + centralHrEmailId);
								centralHrNameList.add("" + centralHrName);
								centralHrIDS.add(centralHrId);
							}
						}
					}
				}
			}
		}
	}
}
