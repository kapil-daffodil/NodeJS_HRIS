package com.applane.resourceoriented.hris.recruitment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.ApplicationsGenerateCode;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class RecruitmentBusinessLogic implements OperationJob {

	private static final String STATUS_TEMP = "approver_temp";

	// private static final int SENIOR_HR = 1;
	// private static final int DIRECTOR = 2;
	// private static final int BU_HR = 3;
	// private static final int REPORTING_TO = 4;
	// private static final int EMPLOYEE = 5;

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		if (record.has("temp")) {
			return;
		}
		try {
			String operationType = record.getOperationType();
			Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
			JSONArray approverIdArray = getApproverIdArray(currentUserEmail);

			if (operationType.equals(Updates.Types.INSERT)) {
				List<String> buHrEmailList = new ArrayList<String>();
				List<String> buHrNameList = new ArrayList<String>();
				List<Object> buHrIDS = new ArrayList<Object>();

				List<String> directorEmailList = new ArrayList<String>();
				List<String> directorNameList = new ArrayList<String>();
				List<Object> directorIdList = new ArrayList<Object>();

				boolean isDirector = getIsDirectorAndUpdateApprovalSummary(record, buHrEmailList, buHrNameList, buHrIDS, directorEmailList, directorNameList, directorIdList);
				if (!isDirector) {
					RecruitmentMail.sendMailToHOD(record, currentUserEmail);
				} else {
					RecruitmentMail.sendMailToBuHr(record, approverIdArray, buHrEmailList, buHrNameList, directorEmailList, directorNameList, currentUserEmail);
				}
			} else if (operationType.equals(Updates.Types.UPDATE)) {
				if (record.has(STATUS_TEMP) && Translator.integerValue(record.getValue(STATUS_TEMP)) == HRISConstants.LEAVE_APPROVED) {
					validationsOnApprove(record, currentUserEmail, approverIdArray);
				} else if (record.has(STATUS_TEMP) && Translator.integerValue(record.getValue(STATUS_TEMP)) == HRISConstants.LEAVE_REJECTED) {
					validationsOnReject(record, currentUserEmail, approverIdArray);
				} else if (record.has("shortlist_from_year") || record.has("shortlist_from_month") || record.has("shortlist_to_year") || record.has("shortlist_to_month") || record.has("shortlist_from_ctc") || record.has("shortlist_to_ctc") || record.has("shortlist_qualification_id") || record.has("shortlist_resume_title") || record.has("resume_flag") || record.has("resume_status") || record.has("shortlist_candidate_status")) {
					shortlistResumes(record);
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			throw new RuntimeException("RecruitmentBusinessLogic >> On After Exception >> " + trace);
		}
	}

	private void validationsOnReject(Record record, Object currentUserEmail, JSONArray approverIdArray) throws Exception {

		List<String> buHrEmailList = new ArrayList<String>();
		List<String> buHrNameList = new ArrayList<String>();
		List<Object> buHrIDS = new ArrayList<Object>();

		List<String> directorEmailList = new ArrayList<String>();
		List<String> directorNameList = new ArrayList<String>();

		List<String> centralHrEmailList = new ArrayList<String>();
		List<String> centralHrNameList = new ArrayList<String>();
		List<Object> centralHrIDS = new ArrayList<Object>();
		boolean isReportingTo = false;
		boolean isSeniorHr = false;
		boolean isBuHr = false;
		boolean isDirector = false;
		boolean isOther = false;

		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}

		JSONArray hodApprovedSummary = getMrfApprovedSummary(record.getKey(), approverId);
		String statusTemp = "";
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			isReportingTo = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO));
			isSeniorHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR));
			isBuHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR));
			isDirector = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR));
			statusTemp = hodApprovedSummary.getJSONObject(0).optString(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name", null);
		}

		if (statusTemp != null) {
			throw new BusinessLogicException("You Have Already " + statusTemp + ".");
		} else if (isSeniorHr) {
			isOther = true;
		} else if (isReportingTo) {
			isOther = true;
		} else if (isBuHr) {
			isOther = true;
		} else if (isDirector) {
			isOther = true;
		}

		if (isSeniorHr) {
			workOnSeniorHr(record, approverIdArray, record.getValue(STATUS_TEMP), buHrEmailList, buHrNameList, buHrIDS, centralHrEmailList, centralHrNameList, centralHrIDS);
			if (Translator.integerValue(record.getValue(STATUS_TEMP)) == HRISConstants.LEAVE_REJECTED) {
				RecruitmentMail.sendMailToBuHr(record, approverIdArray, buHrEmailList, buHrNameList, directorEmailList, directorNameList, currentUserEmail);
			}
		} else if (isOther) {
			RecruitmentMail.sendMailToEmployeeOnReject(record, approverIdArray, currentUserEmail);
		}

	}

	private void validationsOnApprove(Record record, Object currentUserEmail, JSONArray approverIdArray) throws JSONException, Exception {
		List<String> buHrEmailList = new ArrayList<String>();
		List<String> buHrNameList = new ArrayList<String>();
		List<Object> buHrIDS = new ArrayList<Object>();

		List<String> directorEmailList = new ArrayList<String>();
		List<String> directorNameList = new ArrayList<String>();
		List<Object> directorIdList = new ArrayList<Object>();

		List<String> centralHrEmailList = new ArrayList<String>();
		List<String> centralHrNameList = new ArrayList<String>();
		List<Object> centralHrIDS = new ArrayList<Object>();
		boolean isReportingTo = false;
		boolean isSeniorHr = false;
		boolean isBuHr = false;
		boolean isDirector = false;
		boolean isEmployee = false;

		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}

		JSONArray hodApprovedSummary = getMrfApprovedSummary(record.getKey(), approverId);
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			isReportingTo = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO));
			isSeniorHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR));
			isBuHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR));
			isDirector = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR));
		}

		if (isSeniorHr) {
			isSeniorHr = true;
		} else if (isReportingTo) {
			isReportingTo = true;
		} else if (isBuHr) {
			isBuHr = true;
		} else if (isDirector) {
			isDirector = true;
		}

		if (isReportingTo) {
			boolean isReportingToIsDirector = workOnReportingTo(record, approverIdArray, buHrEmailList, buHrNameList, buHrIDS, directorEmailList, directorNameList, directorIdList);
			if (!isReportingToIsDirector) {
				RecruitmentMail.sendMailToHOD(record, currentUserEmail);
			} else {
				RecruitmentMail.sendMailToBuHr(record, approverIdArray, buHrEmailList, buHrNameList, directorEmailList, directorNameList, currentUserEmail);
			}
		} else if (isSeniorHr) {
			workOnSeniorHr(record, approverIdArray, record.getValue(STATUS_TEMP), buHrEmailList, buHrNameList, buHrIDS, centralHrEmailList, centralHrNameList, centralHrIDS);
			if (Translator.integerValue(record.getValue(STATUS_TEMP)) == HRISConstants.LEAVE_APPROVED) {
				RecruitmentMail.sendMailToSelectedEmails(record, approverIdArray, currentUserEmail);
			}
		} else if (isBuHr) {
			workOnBuHr(record, approverIdArray);
			RecruitmentMail.sendMailToDirector(record, approverIdArray, directorEmailList, directorNameList, currentUserEmail, centralHrEmailList);
		} else if (isDirector) {
			workOnDirector(record, approverIdArray, centralHrEmailList, centralHrNameList, centralHrIDS);
			RecruitmentMail.sendMailToHrManager(record, approverIdArray, centralHrEmailList, centralHrNameList, currentUserEmail);
		} else {
			int employeeId = Translator.integerValue(record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.EMPLOYEE_ID));
			if (approverId == employeeId) {
				if (!record.has("statusid")) {
					return;
				}
			}
		}
	}

	private void workOnSeniorHr(Record record, JSONArray approverIdArray, Object statusId, List<String> buHrEmailList, List<String> buHrNameList, List<Object> buHrIDS, List<String> centralHrEmailList, List<String> centralHrNameList, List<Object> centralHrIDS) throws Exception {
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		JSONArray mrfApprovedSummary = getMrfApprovedSummaryForBuHr(record.getKey(), approverId);
		if (mrfApprovedSummary != null && mrfApprovedSummary.length() > 0) {
			for (int counter = 0; counter < mrfApprovedSummary.length(); counter++) {
				Object key = mrfApprovedSummary.getJSONObject(counter).opt(Updates.KEY);
				updateSummary(key, record);
			}
		}
		List<String> directorEmailList = null;
		List<String> directorNameList = null;
		List<Object> directorIdList = null;

		Object employeeId = record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.EMPLOYEE_ID);
		getEmailAndNameOfDirectorList(employeeId, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);

		if (Translator.integerValue(statusId) == HRISConstants.LEAVE_REJECTED) {
			if (buHrIDS != null && buHrIDS.size() > 0) {
				Object centralHrId = buHrIDS.get(0);
				insertApprovalSummaryCentralHr(record, centralHrId);
			}
		}
	}

	private void workOnDirector(Record record, JSONArray approverIdArray, List<String> centralHrEmailList, List<String> centralHrNameList, List<Object> centralHrIDS) throws Exception {

		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		JSONArray mrfApprovedSummary = getMrfApprovedSummary(record.getKey(), approverId);
		if (mrfApprovedSummary != null && mrfApprovedSummary.length() > 0) {
			Object key = mrfApprovedSummary.getJSONObject(0).opt(Updates.KEY);
			updateSummary(key, record);
		}
		List<String> directorEmailList = null;
		List<String> directorNameList = null;
		List<Object> directorIdList = null;

		List<String> buHrEmailList = null;
		List<String> buHrNameList = null;
		List<Object> buHrIDS = null;
		Object employeeId = record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.EMPLOYEE_ID);
		getEmailAndNameOfDirectorList(employeeId, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);

		if (centralHrEmailList != null && centralHrEmailList.size() > 0) {
			Object centralHrId = centralHrIDS.get(0);
			insertApprovalSummaryCentralHr(record, centralHrId);
		}

	}

	private void insertApprovalSummaryCentralHr(Record record, Object centralHrId) throws Exception {
		JSONObject columns = new JSONObject();
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID, centralHrId);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_CONSIDER, 1);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR, 1);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.MRF_REQUEST_ID, record.getKey());
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE, new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDate()));
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private void workOnBuHr(Record record, JSONArray approverIdArray) throws Exception {
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}
		JSONArray mrfApprovedSummary = getMrfApprovedSummary(record.getKey(), approverId);
		if (mrfApprovedSummary != null && mrfApprovedSummary.length() > 0) {
			Object key = mrfApprovedSummary.getJSONObject(0).opt(Updates.KEY);
			updateSummary(key, record);
		}
		Object directorEmail = record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.DIRECTOR_EMAIL);
		JSONArray directorArray = getApproverIdArray(directorEmail);
		if (directorArray != null && directorArray.length() > 0) {
			Object directorId = directorArray.getJSONObject(0).opt(Updates.KEY);
			insertApprovalSummaryDirector(record, directorId);
		}
	}

	private void insertApprovalSummaryDirector(Record record, Object directorId) throws Exception {
		JSONObject columns = new JSONObject();
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID, directorId);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_CONSIDER, 1);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR, 1);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.MRF_REQUEST_ID, record.getKey());
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE, new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDate()));
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private boolean workOnReportingTo(Record record, JSONArray approverIdArray, List<String> buHrEmailList, List<String> buHrNameList, List<Object> buHrIDS, List<String> directorEmailList, List<String> directorNameList, List<Object> directorIdList) throws Exception {
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}

		JSONArray directorIdArray = new JSONArray();
		directorIdArray = getDirectorArray(approverId);

		int reportingToId = 0;
		if (directorIdArray != null && directorIdArray.length() > 0) {
			reportingToId = Translator.integerValue(directorIdArray.getJSONObject(0).opt("reportingtoid"));
		}
		int directorId = getDirectorId(reportingToId, directorIdArray);

		JSONArray mrfApprovedSummary = getMrfApprovedSummary(record.getKey(), approverId);
		if (mrfApprovedSummary != null && mrfApprovedSummary.length() > 0) {
			Object key = mrfApprovedSummary.getJSONObject(0).opt(Updates.KEY);
			updateSummary(key, record);
		}
		boolean isDirector;
		if (directorId == reportingToId) {
			isDirector = true;
			Object employeeId = record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.EMPLOYEE_ID);

			List<String> centralHrEmailList = null;
			List<String> centralHrNameList = null;
			List<Object> centralHrIDS = null;

			getEmailAndNameOfDirectorList(employeeId, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);
			if (buHrEmailList != null && buHrEmailList.size() > 0) {
				int buhr = 2;
				List<Integer> list = new ArrayList<Integer>();
				for (int counter = 0; counter < buHrIDS.size(); counter++) {
					Object id = buHrIDS.get(counter);
					if (!list.contains(Translator.integerValue(id))) {
						list.add(Translator.integerValue(id));
						insertApprovalSummaryOnInsertOpertion(record, buhr, id);
					}
				}
			}
		} else {
			isDirector = false;
			int isReportingOrBuHr = 1;
			insertApprovalSummaryOnInsertOpertion(record, isReportingOrBuHr, approverId);
		}
		return isDirector;
	}

	private void updateSummary(Object key, Record record) throws Exception {
		JSONObject columns = new JSONObject();
		columns.put(Updates.KEY, key);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_CONSIDER, 2);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.REMARKS, record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.REMARKS_FOR_HISTORY));
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID, record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.STATUS_ID));
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE, new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDate()));
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private boolean getIsDirectorAndUpdateApprovalSummary(Record record, List<String> buHrEmailList, List<String> buHrNameList, List<Object> buHrIDS, List<String> directorEmailList, List<String> directorNameList, List<Object> directorIdList) throws Exception {
		Object employeeId = record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.EMPLOYEE_ID);

		JSONArray directorIdArray = new JSONArray();
		directorIdArray = getDirectorArray(employeeId);

		int reportingToId = 0;
		if (directorIdArray != null && directorIdArray.length() > 0) {
			reportingToId = Translator.integerValue(directorIdArray.getJSONObject(0).opt("reportingtoid"));
		}
		int directorId = getDirectorId(reportingToId, directorIdArray);
		boolean isDirector = false;
		if (directorId == reportingToId) {
			isDirector = true;
		} else {
			isDirector = false;
		}
		if (isDirector) {
			List<String> centralHrEmailList = null;
			List<String> centralHrNameList = null;
			List<Object> centralHrIDS = null;

			getEmailAndNameOfDirectorList(employeeId, directorEmailList, directorNameList, buHrEmailList, buHrNameList, buHrIDS, directorIdList, centralHrEmailList, centralHrNameList, centralHrIDS);
			if (buHrEmailList != null && buHrEmailList.size() > 0) {
				int buhr = 2;
				List<Integer> list = new ArrayList<Integer>();
				for (int counter = 0; counter < buHrIDS.size(); counter++) {
					Object id = buHrIDS.get(counter);
					if (!list.contains(Translator.integerValue(id))) {
						list.add(Translator.integerValue(id));
						insertApprovalSummaryOnInsertOpertion(record, buhr, id);
					}
				}
			}
		} else {
			int reportingTo = 1;
			insertApprovalSummaryOnInsertOpertion(record, reportingTo, reportingToId);
		}
		return isDirector;
	}

	private void getEmailAndNameOfDirectorList(Object employeeId, List<String> emailList, List<String> nameList, List<String> buHrEmailList, List<String> buHrNameList, List<Object> buHrIDS, List<Object> directorIdList, List<String> centralHrEmailList, List<String> centralHrNameList, List<Object> centralHrIDS) throws Exception, JSONException {
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
							emailList.add("" + directorEmailIdObject);
							nameList.add("" + directorNameObject);
							directorIdList.add(directorIdObject);
						}
						if (buHrEmailId != null && buHrEmailList != null && buHrNameList != null) {
							if (buHrEmailList != null) {
								buHrEmailList.add("" + buHrEmailId);
								buHrNameList.add("" + buHrName);
								buHrIDS.add(hrEmailsId);
							}
						}
						if (centralHrEmailList != null && centralHrIDS != null && centralHrNameList != null) {
							if (centralHrEmailList != null) {
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

	private void insertApprovalSummaryOnInsertOpertion(Record record, int isReportingOrBuHr, Object employeeOrApproverId) throws Exception {
		JSONObject columns = new JSONObject();
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_CONSIDER, 1);
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.MRF_REQUEST_ID, record.getKey());
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID, employeeOrApproverId);
		if (isReportingOrBuHr == 1) {
			columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO, true);
		} else if (isReportingOrBuHr == 2) {
			columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR, true);
		}
		columns.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE, new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDate()));
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			if (record.has("temp")) {
				return;
			}
			String operationType = record.getOperationType();
			Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);

			if (operationType.equals(Updates.Types.INSERT)) {
				Object departmentName = record.getValue("employeeid.departmentid.name");
				record.removeUpdate("employeeid.departmentid.name");
				Object resumeCode = ApplicationsGenerateCode.generateCode("" + departmentName, "", 6);
				record.addUpdate("mrf_code", resumeCode);
			} else if (operationType.equals(Updates.Types.UPDATE)) {
				JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
				int approverId = 0;
				if (approverIdArray != null && approverIdArray.length() > 0) {
					approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
				}
				int employeeId = Translator.integerValue(record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.EMPLOYEE_ID));
				if (approverId == employeeId) {
					if (!record.has("statusid")) {
						return;
					}
				}
				if (record.has("statusid")) {

					processOnApprovalReject(record, currentUserEmail);
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AdvanceBusinessLogic >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured.");
		}
	}

	public JSONArray getApproverIdArray(Object currentUserEmail) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + "='" + currentUserEmail + "'");
		JSONArray employeeArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		return employeeArray;
	}

	private void processOnApprovalReject(Record record, Object currentUserEmail) throws Exception {
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
		int approverId = 0;
		if (approverIdArray != null && approverIdArray.length() > 0) {
			approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
		}

		JSONArray hodApprovedSummary = getMrfApprovedSummary(record.getKey(), approverId);
		boolean isReportingTo = false;
		boolean isSeniorHr = false;
		boolean isBuHr = false;
		boolean isDirector = false;

		String statusTemp = "";
		if (hodApprovedSummary != null && hodApprovedSummary.length() > 0) {
			isReportingTo = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO));
			isSeniorHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR));
			isBuHr = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR));
			isDirector = Translator.booleanValue(hodApprovedSummary.getJSONObject(0).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR));
			statusTemp = hodApprovedSummary.getJSONObject(0).optString(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name", null);
		}

		if (statusTemp != null) {
			throw new BusinessLogicException("You Have Already " + statusTemp + ".");
		} else if (isSeniorHr) {
			record.addUpdate(STATUS_TEMP, record.getValue("statusid"));
			if (Translator.integerValue(record.getValue("statusid")) == HRISConstants.LEAVE_REJECTED) {
				record.removeUpdate("statusid");
			}

		} else if (isReportingTo) {
			record.addUpdate(STATUS_TEMP, record.getValue("statusid"));
			record.removeUpdate("statusid");
		} else if (isBuHr) {
			record.addUpdate(STATUS_TEMP, record.getValue("statusid"));
			record.removeUpdate("statusid");
		} else if (isDirector) {
			record.addUpdate(STATUS_TEMP, record.getValue("statusid"));
			record.removeUpdate("statusid");
		} else {
			throw new BusinessLogicException("You are not authorized to Approve / Reject selected request.");
		}
	}

	public JSONArray getMrfApprovedSummaryForBuHr(Object mrfId, Object approverId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);

		columnArray.put(Updates.KEY);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.RecruitmentMrfApprovalProcess.MRF_REQUEST_ID + "=" + mrfId + " AND " + HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR + "=1 AND " + HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_CONSIDER + " = 1");
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);

		return directorIdArray;
	}

	public JSONArray getMrfApprovedSummary(Object mrfId, Object approverId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);

		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name");
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.RecruitmentMrfApprovalProcess.MRF_REQUEST_ID + "=" + mrfId + " AND " + HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + "=" + approverId + " AND " + HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_CONSIDER + " = 1");
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);

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

	private JSONArray getDirectorArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);

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

	public void onDelete(Object[] selectedKeys) throws Exception {
		if (selectedKeys != null && selectedKeys.length > 0) {
			for (int counter = 0; counter < selectedKeys.length; counter++) {
				Object mrfId = selectedKeys[counter];
				JSONArray mrfSummary = getMrfDetails(mrfId);
				JSONArray mrfShortListedResumesDetails = getShortListedResumesDetails(mrfId);
				for (int deleteCounter = 0; deleteCounter < mrfSummary.length(); deleteCounter++) {
					JSONObject object = mrfSummary.getJSONObject(deleteCounter);
					object.put("__type__", "delete");
				}
				for (int shortListedCounter = 0; shortListedCounter < mrfShortListedResumesDetails.length(); shortListedCounter++) {
					JSONObject object = mrfSummary.getJSONObject(shortListedCounter);
					object.put("__type__", "delete");
				}
				deleteShortListedRecords(mrfShortListedResumesDetails);
				deleteSummaryRecords(mrfSummary);
				deleteMrfRequest(mrfId);
			}
		}
	}

	private void deleteShortListedRecords(JSONArray mrfShortListedResumesDetails) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, mrfShortListedResumesDetails);
		updates.put(Data.Update.RESOURCE, HRISApplicationConstants.RecruitmentShortListedResumes.RESOURCE);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private JSONArray getShortListedResumesDetails(Object mrfId) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentShortListedResumes.RESOURCE);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.RecruitmentShortListedResumes.MRF_REQUEST_ID + " = " + mrfId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.RecruitmentShortListedResumes.RESOURCE);
	}

	private void deleteMrfRequest(Object mrfId) throws Exception {
		JSONObject updates = new JSONObject();
		JSONObject update = new JSONObject();
		update.put(Updates.KEY, mrfId);
		update.put("__type__", "delete");
		updates.put(Data.Update.RESOURCE, HRISApplicationConstants.RecruitmentMrfProcess.RESOURCE);
		updates.put(Data.Update.UPDATES, update);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private void deleteSummaryRecords(JSONArray mrfSummary) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.UPDATES, mrfSummary);
		updates.put(Data.Update.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private JSONArray getMrfDetails(Object mrfId) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.RecruitmentMrfApprovalProcess.MRF_REQUEST_ID + " = " + mrfId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
	}

	private void shortlistResumes(Record record) throws Exception {
		Object mrfKey = record.getKey();
		JSONObject parameters = new JSONObject();
		String filter1 = "";
		if (record.getValue("shortlist_resume_title") != null) {
			parameters.put("resumeHeadline", "'%" + record.getValue("shortlist_resume_title") + "%'");
			if (filter1.length() > 0) {
				filter1 += " AND ";
			}
			filter1 += "resume_headline like {resumeHeadline}";
		}
		if (record.getValue("shortlist_qualification_id") != null && record.getValue("shortlist_qualification_id") instanceof JSONArray) {
			String qualificationIds = ((JSONArray) record.getValue("shortlist_qualification_id")).toString();
			if (qualificationIds.length() > 0) {
				qualificationIds = qualificationIds.substring(1, qualificationIds.length() - 1);
			}
			if (qualificationIds.length() > 0) {
				parameters.put("qualificationIds", qualificationIds);

				if (filter1.length() > 0) {
					filter1 += " AND ";
				}
				filter1 += "qualification IN{qualificationIds}";
			}
		}
		if (record.getValue("resume_flag") != null && record.getValue("resume_flag") instanceof JSONArray) {
			String qualificationIds = ((JSONArray) record.getValue("resume_flag")).toString();
			if (qualificationIds.length() > 0) {
				qualificationIds = qualificationIds.substring(1, qualificationIds.length() - 1);
			}
			if (qualificationIds.length() > 0) {
				parameters.put("resumeflag", qualificationIds);

				if (filter1.length() > 0) {
					filter1 += " AND ";
				}
				filter1 += "resume_flag IN{resumeflag}";
			}
		}
		if (record.getValue("shortlist_candidate_status") != null && record.getValue("shortlist_candidate_status") instanceof JSONArray) {
			String qualificationIds = ((JSONArray) record.getValue("shortlist_candidate_status")).toString();
			if (qualificationIds.length() > 0) {
				qualificationIds = qualificationIds.substring(1, qualificationIds.length() - 1);
			}
			if (qualificationIds.length() > 0) {
				parameters.put("shortlist_candidate__status", qualificationIds);

				if (filter1.length() > 0) {
					filter1 += " AND ";
				}
				filter1 += "shortlist_candidate_status IN{shortlist_candidate__status}";
			}
		}
		if (record.getValue("resume_status") != null && Translator.integerValue(record.getValue("resume_status")) != 1) {
			parameters.put("resume__status", record.getValue("resume_status"));
			if (filter1.length() > 0) {
				filter1 += " AND ";
			}
			filter1 += "resume_status={resume__status}";
		}
		String filter2 = "";
		String filter3 = "";
		String filter4 = "";
		String filter5 = "";
		boolean toSalary = true;
		if (record.getValue("shortlist_from_ctc") != null) {
			parameters.put("from_ctc", record.getValue("shortlist_from_ctc"));
			if (filter2.length() > 0) {
				filter2 += " AND ";
			}
			filter2 += "required_salary>={from_ctc}";

			if (record.getValue("shortlist_to_ctc") != null) {
				toSalary = false;
				parameters.put("to_ctc", record.getValue("shortlist_to_ctc"));
				if (filter2.length() > 0) {
					filter2 += " AND ";
				}
				filter2 += "required_salary_to<={to_ctc}";

				if (filter3.length() > 0) {
					filter3 += " AND ";
				}
				filter3 += "required_salary>={from_ctc}";

				if (filter3.length() > 0) {
					filter3 += " AND ";
				}
				filter3 += "required_salary<={to_ctc}";

				if (filter4.length() > 0) {
					filter4 += " AND ";
				}
				filter4 += "required_salary_to>={from_ctc}";

				if (filter4.length() > 0) {
					filter4 += " AND ";
				}
				filter4 += "required_salary_to<={to_ctc}";

				if (filter5.length() > 0) {
					filter5 += " AND ";
				}
				filter5 += "required_salary<={from_ctc}";

				if (filter5.length() > 0) {
					filter5 += " AND ";
				}
				filter5 += "required_salary_to>={to_ctc}";

			}
		}
		if (toSalary && record.getValue("shortlist_to_ctc") != null) {
			parameters.put("to_ctc", record.getValue("shortlist_to_ctc"));
			if (filter2.length() > 0) {
				filter2 += " AND ";
			}
			filter2 += "required_salary_to<={to_ctc}";
		}

		if (record.getValue("shortlist_from_year") != null) {
			parameters.put("exp_from_year", record.getValue("shortlist_from_year"));
			if (filter1.length() > 0) {
				filter1 += " AND ";
			}
			filter1 += "experience_years>={exp_from_year}";

		}
		if (record.getValue("shortlist_to_year") != null) {
			parameters.put("exp_to_year", record.getValue("shortlist_to_year"));
			if (filter1.length() > 0) {
				filter1 += " AND ";
			}
			filter1 += "experience_years<={exp_to_year}";
		}
		String filter = "";
		filter += filter1;
		if (filter2.length() > 0) {
			filter = filter + " AND " + filter2;
		}
		JSONArray optionFilter = new JSONArray();
		if (filter3.length() > 0) {
			optionFilter.put(filter + " AND " + filter3);
		}
		if (filter4.length() > 0) {
			optionFilter.put(filter + " AND " + filter4);
		}
		if (filter5.length() > 0) {
			optionFilter.put(filter + " AND " + filter5);
		}
		JSONArray masterResumesRecords = getMasterResumes(filter, optionFilter, parameters);
		for (int masterResumesCounter = 0; masterResumesCounter < masterResumesRecords.length(); masterResumesCounter++) {
			Object masterResumeKeys = masterResumesRecords.getJSONObject(masterResumesCounter).opt(Updates.KEY);
			insertIntoShortlistedResumes(mrfKey, masterResumeKeys);
		}
	}

	// public void shortListResumes(Object[] mrfSelectedResumes) {
	// try {
	// if (mrfSelectedResumes != null && mrfSelectedResumes.length > 0) {
	// for (int counter = 0; counter < mrfSelectedResumes.length; counter++) {
	// Object mrfKey = mrfSelectedResumes[counter];
	// JSONArray mrfRecords = getMrfRecords(mrfKey);
	// if (mrfRecords != null && mrfRecords.length() > 0) {
	// Object requiredSalary = mrfRecords.getJSONObject(0).opt("ctc_range");
	// Object preferredLocationId = mrfRecords.getJSONObject(0).opt("preference");
	// Object gender = mrfRecords.getJSONObject(0).opt("gender_restriction");
	// Object qualification = mrfRecords.getJSONObject(0).opt("qualification");
	// if (requiredSalary != null || preferredLocationId != null || gender != null || qualification != null) {
	// JSONArray masterResumesRecords = getMasterResumes(requiredSalary, preferredLocationId, gender, qualification);
	// for (int masterResumesCounter = 0; masterResumesCounter < masterResumesRecords.length(); masterResumesCounter++) {
	// Object masterResumeKeys = masterResumesRecords.getJSONObject(masterResumesCounter).opt(Updates.KEY);
	// insertIntoShortlistedResumes(mrfKey, masterResumeKeys);
	// }
	// }
	// }
	// }
	// }
	// } catch (BusinessLogicException e) {
	// throw e;
	// } catch (Exception e) {
	// String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
	// LogUtility.writeError("RecruitmentBusinessLogic >> trace >> " + trace);
	// throw new RuntimeException(e.getMessage());
	// }
	// }

	private void insertIntoShortlistedResumes(Object mrfKey, Object masterResumeKeys) throws Exception {
		boolean isExist = isExist(mrfKey, masterResumeKeys);
		if (!isExist) {
			JSONObject updateColumns = new JSONObject();
			updateColumns.put(HRISApplicationConstants.RecruitmentShortListedResumes.MRF_REQUEST_ID, mrfKey);
			updateColumns.put(HRISApplicationConstants.RecruitmentShortListedResumes.RESUME_HOLDER_ID, masterResumeKeys);
			JSONObject updates = new JSONObject();
			updates.put(Data.Update.UPDATES, updateColumns);
			updates.put(Data.Update.RESOURCE, HRISApplicationConstants.RecruitmentShortListedResumes.RESOURCE);
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
		}
	}

	private boolean isExist(Object mrfKey, Object masterResumeKeys) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentShortListedResumes.RESOURCE);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.RecruitmentShortListedResumes.MRF_REQUEST_ID + " = " + mrfKey + " AND " + HRISApplicationConstants.RecruitmentShortListedResumes.RESUME_HOLDER_ID + " = " + masterResumeKeys);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.RecruitmentShortListedResumes.RESOURCE).length() > 0 ? true : false;
	}

	// private JSONArray getMasterResumes(Object requiredSalary, Object preferredLocationId, Object gender, Object qualification) throws Exception {
	private JSONArray getMasterResumes(String filters, JSONArray optionFilter, JSONObject parameters) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_resume_holder");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.PARAMETERS, parameters);
		query.put(Data.Query.FILTERS, filters);
		query.put(Data.Query.OPTION_FILTERS, optionFilter);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_resume_holder");
	}

	private JSONArray getMrfRecords(Object mrfKey) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("ctc_range");
		columnArray.put("preference");
		columnArray.put("gender_restriction");
		columnArray.put("qualification");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mrf_request_form");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + mrfKey);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_mrf_request_form");
	}

	@SuppressWarnings("unused")
	private Object getDepartmentStrength(Object departmentId) throws Exception {

		LogUtility.writeLog("Department Id : [" + departmentId + "]");

		JSONArray columnArray = new JSONArray();
		columnArray.put(new JSONObject().put("expression", "__key__").put("aggregate", "count").put("alias", "count"));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "departmentid" + " = " + departmentId + " AND employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE);

		int departmentStrength = 0;

		JSONArray jsonArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);

		LogUtility.writeLog("Department Strength Json Array : " + jsonArray);

		if (jsonArray != null && jsonArray.length() > 0) {
			JSONObject jsonObject = jsonArray.optJSONObject(0);
			departmentStrength = jsonObject.optInt("count");
		}

		return departmentStrength;
	}
}