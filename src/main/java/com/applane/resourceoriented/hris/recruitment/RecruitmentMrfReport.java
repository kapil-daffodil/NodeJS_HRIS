package com.applane.resourceoriented.hris.recruitment;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.ShortLeaveUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class RecruitmentMrfReport implements QueryJob {

	@Override
	public void onQuery(Query arg0) throws DeadlineExceededException {

	}

	@Override
	public void onResult(Result result) throws DeadlineExceededException {
		try {
			if (result != null) {
				JSONArray records = result.getRecords();
				if (records != null && records.length() > 0) {
					JSONObject formResult = records.getJSONObject(0);
					JSONObject mrfDetails = putMrfDetails(formResult);
					JSONArray mailConfigurationSetup = ShortLeaveUtility.getMailConfigurationSetup();
					if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
						Object organizationName = mailConfigurationSetup.getJSONObject(0).opt("organizationname");
						Object organizationAddress = mailConfigurationSetup.getJSONObject(0).opt("address");
						mrfDetails.put("organizationName", organizationName);
						mrfDetails.put("organizationAddress", organizationAddress);
					}
					Object mrfId = formResult.opt(Updates.KEY);
					JSONArray summaryRecords = RecruitmentMail.getSummaryRecords(mrfId);
					if (summaryRecords != null && summaryRecords.length() > 0) {
						for (int counter = 0; counter < summaryRecords.length(); counter++) {
							JSONObject summaryObject = summaryRecords.getJSONObject(counter);
							boolean isBuHr = Translator.booleanValue(summaryObject.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR));
							boolean isSeniorHr = Translator.booleanValue(summaryObject.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR));
							boolean isDirector = Translator.booleanValue(summaryObject.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR));
							boolean isReportingTo = Translator.booleanValue(summaryObject.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO));
							String status = "";

							if (isReportingTo) {
								status = "H.O.D.";
							} else if (isBuHr) {
								status = "BU. HR";
							} else if (isSeniorHr) {
								status = "HR. Head Office";
							} else if (isDirector) {
								status = "Approving Authority";
							}
							summaryObject.put("employeeStatus", status);
						}
						mrfDetails.put("approvalProcess", summaryRecords);

					}
					JSONObject object = new JSONObject();
					object.put("mrfDetails", mrfDetails);
					records.put(object);
				}

			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("RecruitmentMrfReport >> Exception Trace >> " + trace);
			throw new RuntimeException(e.getMessage());
		}
	}

	private static JSONObject putMrfDetails(JSONObject record) throws Exception {

		Object employeeName = record.opt("employeeid.name");
		Object employeeCode = record.opt("employeeid.employeecode");
		Object employeeDepartment = record.opt("employeeid.departmentid.name");
		Object employeeEmailId = record.opt("employeeid.officialemailid");
		Object employeeDesignation = record.opt("employeeid.designationid.name");

		Object employeeReportingTo = record.opt("employeeid.reportingto.name");
		Object employeeReportingToEmail = record.opt("employeeid.reportingtoid.officialemailid");

		Object requirementInBranch = record.opt("requirement_for_branch.name");
		Object requirementInDepartment = record.opt("departmentid.name");
		Object requirementInDesignation = record.opt("designationid.name");
		Object jobDescription = record.opt("job_descriprion");

		Object numberOfVacency = record.opt("number_of_vacancies");
		Object vacencyPriority = record.opt("priorityid.name");
		Object vacencyTypeId = record.opt("type_of_vacancies");
		Object vacencyTypeName = record.opt("type_of_vacancies.name");
		Object vancyMonths = record.opt("temporary_months");

		Object currentStrength = record.opt("current_strength");

		Object desiredQualification = record.opt("qualification.name");
		Object experienceYear = record.opt("experience_year");
		Object experienceMonth = record.opt("experience_months");
		Object initialPosting = record.opt("initial_posting.name");
		Object ageFromYear = record.opt("required_age_from");
		Object ageToYear = record.opt("required_age_to");
		Object genderRestriction = record.opt("gender_restriction.name");
		Object prefference = record.opt("preference.name");
		Object ctcRange = record.opt("ctc_range");

		Object isExistingResource = record.opt("is_existing_resource.name");
		Object existingResourceName = record.opt("exist_employee_id.name");
		Object existingResourceDepartment = record.opt("exist_department_id.name");

		Object reasonIsNewBusiness = record.opt("reason_new_business");
		Object reasonIsAditionalWork = record.opt("reason_addition_work");
		Object reasonIsReplacement = record.opt("reason_replacement");
		Object reasonIsOther = record.opt("reason_other_boolean");

		Object replacementEmployee = record.opt("replacement_employeeid.name");
		Object replacementEmployeeCtc = record.opt("replacement_employeeid.ctc");
		Object reasonOtherText = record.opt("reason_other_text");

		JSONObject mrfDetails = new JSONObject();
		JSONArray mailConfigurationSetup = ShortLeaveUtility.getMailConfigurationSetup();
		if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
			Object organizationname = mailConfigurationSetup.getJSONObject(0).opt("organizationname");
			Object address = mailConfigurationSetup.getJSONObject(0).opt("address");
			mrfDetails.put("organizationname", organizationname);
			mrfDetails.put("address", address);
		}

		mrfDetails.put("employeeName", employeeName == null ? "" : employeeName);
		mrfDetails.put("employeeCode", employeeCode == null ? "" : employeeCode);
		mrfDetails.put("employeeDepartment", employeeDepartment == null ? "" : employeeDepartment);
		mrfDetails.put("employeeReportingTo", employeeReportingTo == null ? "" : employeeReportingTo);
		if (employeeReportingToEmail != null) {
			mrfDetails.put("employeeReportingToEmail", employeeReportingToEmail);
		}
		mrfDetails.put("employeeDesignation", employeeDesignation == null ? "" : employeeDesignation);
		mrfDetails.put("requirementInBU", requirementInBranch == null ? "" : requirementInBranch);
		mrfDetails.put("requirementInDepartment", requirementInDepartment == null ? "" : requirementInDepartment);
		mrfDetails.put("requirementInDesignation", requirementInDesignation == null ? "" : requirementInDesignation);
		mrfDetails.put("jobDescription", jobDescription == null ? "" : jobDescription);
		mrfDetails.put("numberOfVacency", numberOfVacency == null ? "" : numberOfVacency);
		mrfDetails.put("vacencyPriority", vacencyPriority == null ? "" : vacencyPriority);
		if (vacencyTypeId != null) {
			mrfDetails.put("vacencyTypeId", vacencyTypeId);
		}

		mrfDetails.put("employeeEmailId", employeeEmailId == null ? "" : employeeEmailId);
		mrfDetails.put("vacancyTypeName", vacencyTypeName == null ? "" : vacencyTypeName);
		mrfDetails.put("vacancyMonths", vancyMonths == null ? "" : vancyMonths);
		mrfDetails.put("currentStrength", currentStrength == null ? "" : currentStrength);
		mrfDetails.put("desiredQualification", desiredQualification == null ? "" : desiredQualification);
		mrfDetails.put("experienceYear", experienceYear == null ? "" : experienceYear);
		mrfDetails.put("experienceMonth", experienceMonth == null ? "" : experienceMonth);
		mrfDetails.put("initialPosting", initialPosting == null ? "" : initialPosting);
		mrfDetails.put("ageFromYear", ageFromYear == null ? "" : ageFromYear);
		mrfDetails.put("ageToYear", ageToYear == null ? "" : ageToYear);
		mrfDetails.put("genderRestriction", genderRestriction == null ? "" : genderRestriction);
		mrfDetails.put("prefference", prefference == null ? "" : prefference);
		mrfDetails.put("ctcRange", ctcRange == null ? "" : ctcRange);
		mrfDetails.put("isExistingResource", isExistingResource == null ? "" : isExistingResource);
		mrfDetails.put("existingResourceName", existingResourceName == null ? "" : existingResourceName);
		mrfDetails.put("existingResourceDepartment", existingResourceDepartment == null ? "" : existingResourceDepartment);
		mrfDetails.put("reasonIsNewBusiness", reasonIsNewBusiness == null ? "" : reasonIsNewBusiness);
		mrfDetails.put("reasonIsAditionalWork", reasonIsAditionalWork == null ? "" : reasonIsAditionalWork);
		mrfDetails.put("reasonIsReplacement", reasonIsReplacement == null ? "" : reasonIsReplacement);
		mrfDetails.put("reasonIsOther", reasonIsOther == null ? "" : reasonIsOther);
		mrfDetails.put("replacementEmployee", replacementEmployee == null ? "" : replacementEmployee);
		mrfDetails.put("replacementEmployeeCtc", replacementEmployeeCtc == null ? "" : replacementEmployeeCtc);
		mrfDetails.put("reasonOtherText", reasonOtherText == null ? "" : reasonOtherText);

		return mrfDetails;
	}
}
