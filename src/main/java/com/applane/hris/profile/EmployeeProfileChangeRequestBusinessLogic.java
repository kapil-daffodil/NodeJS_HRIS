package com.applane.hris.profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeProfileChangeRequestBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			String operationType = record.getOperationType();
			LogUtility.writeLog("onAfterUpdate >> Record Values : " + record.getValues());

			if (operationType.equalsIgnoreCase(Updates.Types.INSERT)) {

				Object employeeName = record.getValue(HrisKinds.EmployeeProfileChangeRequest.EMPLOYEE_NAME);
				Object employeeCode = record.getValue(HrisKinds.EmployeeProfileChangeRequest.EMPLOYEE_CODE);
				String profileChangeRequestMessage = Translator.stringValue(record.getValue(HrisKinds.EmployeeProfileChangeRequest.PROFILE_CHANGE_REQUEST_MESSAGE));
				String hrOfficialEmailId = Translator.stringValue(record.getValue(HrisKinds.EmployeeProfileChangeRequest.HR_EMPLOYEE_OFFICIAL_EMAIL_ID));

				LogUtility.writeLog("Employee Name : " + employeeName + ", Employee Code : " + employeeCode + ", Message : " + profileChangeRequestMessage + ", HR Email Id : " + hrOfficialEmailId);

				if (hrOfficialEmailId != null && hrOfficialEmailId.toString().length() > 0) {
					sendMail(employeeName, employeeCode, profileChangeRequestMessage, hrOfficialEmailId);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(EmployeeProfileChangeRequestBusinessLogic.class.getName(), e));
		}
	}

	private void sendMail(Object employeeName, Object employeeCode, String profileChangeRequestMessage, String hrOfficialEmailId) {
		try {
			ApplaneMail mailSender = new ApplaneMail();
			mailSender.setMessage("Profile Change Request by " + employeeName + " : " + employeeCode, profileChangeRequestMessage, true);
			mailSender.setTo(hrOfficialEmailId);
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		} catch (ApplaneMailException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}
	}

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {

	}

	public Object getHrEmployeeId(Object branchId) {
		try {
			int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			LogUtility.writeLog("getHrEmployeeId >> Branch Id : " + branchId + ", Organization Id : " + organizationId);

			Object hrEmployeeId = null;

			if (organizationId == HrisKinds.NAVIGANT_ORGANIZATION_ID) {
				hrEmployeeId = getNavigantBranchHREmployeeId(branchId);
			} else {
				hrEmployeeId = getOtherBranchHREmployeeId(branchId);
			}

			LogUtility.writeLog("HR Employee Id : " + hrEmployeeId);

			return hrEmployeeId;

		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(EmployeeProfileChangeRequestBusinessLogic.class.getName(), e));
		}
	}

	private Object getOtherBranchHREmployeeId(Object branchId) throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.HrAssigning.EMPLOYEE_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.HR_ASSIGNING);
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, HrisKinds.HrAssigning.BRANCH_ID + "=" + branchId);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.HR_ASSIGNING);

		LogUtility.writeLog("HRIS Assigning Json Array : [" + resultJSONArray + "]");

		Object hrEmployeeId = null;

		int noOfHREmployees = resultJSONArray == null ? 0 : resultJSONArray.length();

		if (noOfHREmployees > 0) {
			JSONObject jsonObject = resultJSONArray.optJSONObject(0);
			hrEmployeeId = jsonObject.opt(HrisKinds.HrAssigning.EMPLOYEE_ID);
		}

		return hrEmployeeId;
	}

	private Object getNavigantBranchHREmployeeId(Object branchId) throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.Branches.HR_EMAIL_IDS);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.BRANCHES);
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, Data.Query.KEY + "=" + branchId);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.BRANCHES);

		LogUtility.writeLog("HRIS Branches Json Array : [" + resultJSONArray + "]");

		Object hrEmployeeId = null;

		int noOfHrisBranches = resultJSONArray == null ? 0 : resultJSONArray.length();

		if (noOfHrisBranches > 0) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(0);
			JSONArray hrEmployeeIds = jsonObject.optJSONArray(HrisKinds.Branches.HR_EMAIL_IDS);

			int noOfHREmployees = (hrEmployeeIds == null) ? 0 : hrEmployeeIds.length();

			if (noOfHREmployees > 0) {
				hrEmployeeId = hrEmployeeIds.opt(0);
			}
		}

		return hrEmployeeId;
	}
}
