package com.applane.resourceoriented.hris.recruitment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.ShortLeaveUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class RecruitmentMail implements OperationJob {

	private static final String EMPLOYEE_STATUS = "employeeStatus";

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
	}

	public static void sendMailToHOD(Record record, Object currentUserEmail) throws Exception {

		JSONObject mrfDetails = putMrfDetails(record);

		boolean reasonIsNewBusiness = Translator.booleanValue(mrfDetails.opt("reasonIsNewBusiness"));
		boolean reasonIsAditionalWork = Translator.booleanValue(mrfDetails.opt("reasonIsAditionalWork"));
		boolean reasonIsReplacement = Translator.booleanValue(mrfDetails.opt("reasonIsReplacement"));
		boolean reasonIsOther = Translator.booleanValue(mrfDetails.opt("reasonIsOther"));

		StringBuilder mailContent = new StringBuilder();
		mailContent.append("Dear Sir / Madam,").append("<BR /><BR />");
		// mailContent.append(mrfDetails.opt("employeeName")).append(" has requested for Manpower.").append("<BR /><BR />");
		mailContent.append("<table border='1' cellpadding='4' cellspacing='0' width='99%'>");
		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='4'>");
		if (mrfDetails.has("organizationname")) {
			mailContent.append("<font size='4'><b>").append(mrfDetails.opt("organizationname")).append("</b></font>").append("<BR />");
		}
		mailContent.append("An ISO 9001: 2000 Certified Company").append("<BR />");
		if (mrfDetails.has("address")) {
			mailContent.append(mrfDetails.opt("address"));
		}
		mailContent.append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='3' width='75%>").append("<font size='4'><b>Manpower Requisition Form</b></font>").append("</td>");
		mailContent.append("<td align='left'>S. No. ").append(mrfDetails.opt("mrfCode")).append("<BR /><BR />Date:").append(mrfDetails.opt("requestDate")).append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Name of Requesting person</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Name").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeName")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Employee Code").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeCode")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDepartment")).append("</td></tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Job Required / Description</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("For (Name the business unit<BR />(Viz. DARCL (Trans-rail / BU-<BR />W / BU-E / BU-S / BU-N / HO-<BR />Hisar / Corporate-GGN / Any<BR />Other)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInBranch")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department / Function<BR />(Mention the sub-function<BR />also)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDepartment")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation/Level").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Job Description").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("jobDescription")).append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("No of Vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("numberOfVacency")).append("</td>").append("<td align='left'>").append("Priority").append("</td>").append("<td>").append(mrfDetails.opt("vacencyPriority")).append("</td>").append("</tr>");
		int vacencyTypeIdInt = Translator.integerValue(mrfDetails.opt("vacencyTypeId"));
		if (vacencyTypeIdInt == 1) {// permanent
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("Months").append("</td>").append("<td>").append(mrfDetails.opt("vancyMonths")).append("</td>").append("</tr>");
		} else {
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("").append("</td>").append("<td>").append("").append("</td>").append("</tr>");
		}

		mailContent.append("<tr><td align='left'>").append("Current Strength (Same<BR />Profile)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("currentStrength")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Desired Qualification").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("desiredQualification")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Required years of Experience").append("</td>").append("<td align='left' colspan='3'> Year: ").append(mrfDetails.opt("experienceYear")).append(", Months: ").append(mrfDetails.opt("experienceMonth")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Place of Initial Posting").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("initialPosting")).append("</td></tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Age Group").append("</td>").append("<td align='left'>").append(mrfDetails.opt("ageFromYear")).append(" - ").append(mrfDetails.opt("ageToYear")).append("</td>").append("<td align='left'>").append("Any Restriction on<BR />Gender?").append("</td>").append("<td>").append(mrfDetails.opt("genderRestriction")).append("</td>").append("</tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>Rs. ").append(mrfDetails.opt("ctcRange")).append("/- to ").append(mrfDetails.opt("ctcRangeTo")).append("/- per month").append("</td>").append("</tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>Mr. / Mrs. ").append(mrfDetails.opt("existingResourceName")).append("(").append(mrfDetails.opt("employeeCode")).append(")</td>").append("<td>Department : ").append(mrfDetails.opt("existingResourceDepartment")).append("(Auto capture from employee profile)").append("</td>").append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("Mr. / Mrs. " + mrfDetails.opt("replacementEmployee") + "(" + mrfDetails.opt("replacementEmployeeCode") + "0") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("CTC (in Rs): " + mrfDetails.opt("replacementEmployeeCtc") == null ? "" : ("CTC (in Rs): " + new DecimalFormat("0.00").format(Translator.doubleValue(mrfDetails.opt("replacementEmployeeCtc")))) + "/- per month") : "")
				.append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='4'>").append(reasonIsOther ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Any Other (Please specify)<BR />").append(reasonIsOther ? mrfDetails.opt("reasonOtherText") : "").append("</td></tr>");

		mailContent.append("</table>");
		LogUtility.writeError("mrfDetails >> " + mrfDetails);
		String title = "Manpower Requisition Form";
		if (mrfDetails.has("employeeReportingToEmail")) {
			String link = getLink(record.getKey(), mrfDetails.opt("employeeReportingToEmail"));
			PunchingUtility.sendMailsWithoutCC(new String[] { "" + mrfDetails.opt("employeeReportingToEmail") }, (mailContent.toString() + link), title);
		}
		String operationType = record.getOperationType();
		if (operationType.equals(Updates.Types.INSERT)) {
			PunchingUtility.sendMailsWithoutCC(new String[] { "" + mrfDetails.opt("employeeEmailId") }, mailContent.toString(), title);
		} else if (operationType.equals(Updates.Types.UPDATE)) {
			PunchingUtility.sendMailsWithoutCC(new String[] { "" + currentUserEmail }, mailContent.toString(), title);
		}

	}

	private static JSONObject putMrfDetails(Record record) throws Exception {

		Object employeeName = record.getValue("employeeid.name");
		Object employeeCode = record.getValue("employeeid.employeecode");
		Object employeeDepartment = record.getValue("employeeid.departmentid.name");
		Object employeeEmailId = record.getValue("employeeid.officialemailid");
		Object employeeDesignation = record.getValue("employeeid.designationid.name");

		Object employeeReportingTo = record.getValue("employeeid.reportingto.name");
		Object employeeReportingToEmail = record.getValue("employeeid.reportingtoid.officialemailid");

		Object requirementInBranch = record.getValue("requirement_for_branch.name");
		Object requirementInDepartment = record.getValue("departmentid.name");
		Object requirementInDesignation = record.getValue("designationid.name");
		Object jobDescription = record.getValue("job_descriprion");

		Object numberOfVacency = record.getValue("number_of_vacancies");
		Object vacencyPriority = record.getValue("priorityid.name");
		Object vacencyTypeId = record.getValue("type_of_vacancies");
		Object vacencyTypeName = record.getValue("type_of_vacancies.name");
		Object vancyMonths = record.getValue("temporary_months");

		Object currentStrength = record.getValue("current_strength");

		Object desiredQualification = record.getValue("qualification.name");
		Object experienceYear = record.getValue("experience_year");
		Object experienceMonth = record.getValue("experience_months");
		Object initialPosting = record.getValue("initial_posting.name");
		Object ageFromYear = record.getValue("required_age_from");
		Object ageToYear = record.getValue("required_age_to");
		Object genderRestriction = record.getValue("gender_restriction.name");
		Object prefference = record.getValue("preference.name");
		Object ctcRange = record.getValue("ctc_range");
		Object ctcRangeTo = record.getValue("ctc_range_to");

		Object isExistingResource = record.getValue("is_existing_resource.name");
		Object existingResourceName = record.getValue("exist_employee_id.name");
		Object existingResourceDepartment = record.getValue("exist_department_id.name");

		Object reasonIsNewBusiness = record.getValue("reason_new_business");
		Object reasonIsAditionalWork = record.getValue("reason_addition_work");
		Object reasonIsReplacement = record.getValue("reason_replacement");
		Object reasonIsOther = record.getValue("reason_other_boolean");

		Object replacementEmployee = record.getValue("replacement_employeeid.name");
		Object replacementEmployeeCtc = record.getValue("replacement_employeeid.ctc");
		Object replacementEmployeeCode = record.getValue("replacement_employeeid.employeecode");
		Object reasonOtherText = record.getValue("reason_other_text");

		Object mrfCode = record.getValue("mrf_code");
		Object requestDate = record.getValue("request_date");

		JSONObject mrfDetails = new JSONObject();
		JSONArray mailConfigurationSetup = ShortLeaveUtility.getMailConfigurationSetup();
		if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
			Object organizationname = mailConfigurationSetup.getJSONObject(0).opt("organizationname");
			Object address = mailConfigurationSetup.getJSONObject(0).opt("address");
			mrfDetails.put("organizationname", organizationname);
			mrfDetails.put("address", address);
		}

		mrfDetails.put("mrfCode", mrfCode == null ? "" : mrfCode);
		mrfDetails.put("requestDate", requestDate == null ? "" : requestDate);
		mrfDetails.put("employeeName", employeeName == null ? "" : employeeName);
		mrfDetails.put("employeeCode", employeeCode == null ? "" : employeeCode);
		mrfDetails.put("employeeDepartment", employeeDepartment == null ? "" : employeeDepartment);
		mrfDetails.put("employeeReportingTo", employeeReportingTo == null ? "" : employeeReportingTo);
		if (employeeReportingToEmail != null) {
			mrfDetails.put("employeeReportingToEmail", employeeReportingToEmail);
		}
		mrfDetails.put("employeeDesignation", employeeDesignation == null ? "" : employeeDesignation);
		mrfDetails.put("requirementInBranch", requirementInBranch == null ? "" : requirementInBranch);
		mrfDetails.put("requirementInDepartment", requirementInDepartment == null ? "" : requirementInDepartment);
		mrfDetails.put("requirementInDesignation", requirementInDesignation == null ? "" : requirementInDesignation);
		mrfDetails.put("jobDescription", jobDescription == null ? "" : jobDescription);
		mrfDetails.put("numberOfVacency", numberOfVacency == null ? "" : numberOfVacency);
		mrfDetails.put("vacencyPriority", vacencyPriority == null ? "" : vacencyPriority);
		if (vacencyTypeId != null) {
			mrfDetails.put("vacencyTypeId", vacencyTypeId);
		}

		mrfDetails.put("employeeEmailId", employeeEmailId == null ? "" : employeeEmailId);
		mrfDetails.put("vacencyTypeName", vacencyTypeName == null ? "" : vacencyTypeName);
		mrfDetails.put("vancyMonths", vancyMonths == null ? "" : vancyMonths);
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
		mrfDetails.put("ctcRangeTo", ctcRangeTo == null ? "" : ctcRangeTo);
		mrfDetails.put("isExistingResource", isExistingResource == null ? "" : isExistingResource);
		mrfDetails.put("existingResourceName", existingResourceName == null ? "" : existingResourceName);
		mrfDetails.put("existingResourceDepartment", existingResourceDepartment == null ? "" : existingResourceDepartment);
		mrfDetails.put("reasonIsNewBusiness", reasonIsNewBusiness == null ? "" : reasonIsNewBusiness);
		mrfDetails.put("reasonIsAditionalWork", reasonIsAditionalWork == null ? "" : reasonIsAditionalWork);
		mrfDetails.put("reasonIsReplacement", reasonIsReplacement == null ? "" : reasonIsReplacement);
		mrfDetails.put("reasonIsOther", reasonIsOther == null ? "" : reasonIsOther);
		mrfDetails.put("replacementEmployee", replacementEmployee == null ? "" : replacementEmployee);
		mrfDetails.put("replacementEmployeeCtc", replacementEmployeeCtc == null ? "" : replacementEmployeeCtc);
		mrfDetails.put("replacementEmployeeCode", replacementEmployeeCode == null ? "" : replacementEmployeeCode);
		mrfDetails.put("reasonOtherText", reasonOtherText == null ? "" : reasonOtherText);

		return mrfDetails;
	}

	public static void sendMailToBuHr(Record record, JSONArray approverIdArray, List<String> buHrEmailList, List<String> buHrNameList, List<String> directorEmailList, List<String> directorNameList, Object currentUserEmail) throws Exception {

		JSONObject mrfDetails = putMrfDetails(record);

		boolean reasonIsNewBusiness = Translator.booleanValue(mrfDetails.opt("reasonIsNewBusiness"));
		boolean reasonIsAditionalWork = Translator.booleanValue(mrfDetails.opt("reasonIsAditionalWork"));
		boolean reasonIsReplacement = Translator.booleanValue(mrfDetails.opt("reasonIsReplacement"));
		boolean reasonIsOther = Translator.booleanValue(mrfDetails.opt("reasonIsOther"));

		StringBuilder mailContent = new StringBuilder();
		mailContent.append("Dear Sir / Madam,").append("<BR /><BR />");
		// mailContent.append(mrfDetails.opt("employeeName")).append(" has requested for Manpower.").append("<BR /><BR />");
		mailContent.append("<table border='1' cellpadding='4' cellspacing='0' width='99%'>");
		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='4'>");
		if (mrfDetails.has("organizationname")) {
			mailContent.append("<font size='4'><b>").append(mrfDetails.opt("organizationname")).append("</b></font>").append("<BR />");
		}
		mailContent.append("An ISO 9001: 2000 Certified Company").append("<BR />");
		if (mrfDetails.has("address")) {
			mailContent.append(mrfDetails.opt("address"));
		}
		mailContent.append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='3' width='75%>").append("<font size='4'><b>Manpower Requisition Form</b></font>").append("</td>");
		mailContent.append("<td align='left'>S. No. ").append(mrfDetails.opt("mrfCode")).append("<BR /><BR />Date:").append(mrfDetails.opt("requestDate")).append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Name of Requesting person</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Name").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeName")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Employee Code").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeCode")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDepartment")).append("</td></tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Job Required / Description</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("For (Name the business unit<BR />(Viz. DARCL (Trans-rail / BU-<BR />W / BU-E / BU-S / BU-N / HO-<BR />Hisar / Corporate-GGN / Any<BR />Other)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInBranch")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department / Function<BR />(Mention the sub-function<BR />also)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDepartment")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation/Level").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Job Description").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("jobDescription")).append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("No of Vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("numberOfVacency")).append("</td>").append("<td align='left'>").append("Priority").append("</td>").append("<td>").append(mrfDetails.opt("vacencyPriority")).append("</td>").append("</tr>");
		int vacencyTypeIdInt = Translator.integerValue(mrfDetails.opt("vacencyTypeId"));
		if (vacencyTypeIdInt == 1) {// permanent
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("Months").append("</td>").append("<td>").append(mrfDetails.opt("vancyMonths")).append("</td>").append("</tr>");
		} else {
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("").append("</td>").append("<td>").append("").append("</td>").append("</tr>");
		}

		mailContent.append("<tr><td align='left'>").append("Current Strength (Same<BR />Profile)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("currentStrength")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Desired Qualification").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("desiredQualification")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Required years of Experience").append("</td>").append("<td align='left' colspan='3'> Year: ").append(mrfDetails.opt("experienceYear")).append(", Months: ").append(mrfDetails.opt("experienceMonth")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Place of Initial Posting").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("initialPosting")).append("</td></tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Age Group").append("</td>").append("<td align='left'>").append(mrfDetails.opt("ageFromYear")).append(" - ").append(mrfDetails.opt("ageToYear")).append("</td>").append("<td align='left'>").append("Any Restriction on<BR />Gender?").append("</td>").append("<td>").append(mrfDetails.opt("genderRestriction")).append("</td>").append("</tr>");

		// mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>").append(mrfDetails.opt("ctcRange")).append("</td>").append("</tr>");
		// mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>").append(mrfDetails.opt("existingResourceName")).append("</td>").append("<td>").append(mrfDetails.opt("existingResourceDepartment")).append("</td>").append("</tr>");
		//
		// mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");
		//
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployee") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployeeCtc") : "").append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>Rs. ").append(mrfDetails.opt("ctcRange")).append("/- to ").append(mrfDetails.opt("ctcRangeTo")).append("/- per month").append("</td>").append("</tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>Mr. / Mrs. ").append(mrfDetails.opt("existingResourceName")).append("(").append(mrfDetails.opt("employeeCode")).append(")</td>").append("<td>Department : ").append(mrfDetails.opt("existingResourceDepartment")).append("(Auto capture from employee profile)").append("</td>").append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("Mr. / Mrs. " + mrfDetails.opt("replacementEmployee") + "(" + mrfDetails.opt("replacementEmployeeCode") + "0") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("CTC (in Rs): " + mrfDetails.opt("replacementEmployeeCtc") == null ? "" : ("CTC (in Rs): " + new DecimalFormat("0.00").format(Translator.doubleValue(mrfDetails.opt("replacementEmployeeCtc")))) + "/- per month") : "")
				.append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='4'>").append(reasonIsOther ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Any Other (Please specify)<BR />").append(reasonIsOther ? mrfDetails.opt("reasonOtherText") : "").append("</td></tr>");

		mailContent.append("</table>");

		JSONArray summaryRecords = getSummaryRecords(record.getKey());
		if (summaryRecords != null && summaryRecords.length() > 0) {
			mailContent.append("<BR />");
			mailContent.append("Approval Summary");
			mailContent.append("<BR />");
			mailContent.append("<table cellpadding='5' cellspacing='0' border='1'>");
			mailContent.append("<tr><td>E. Code</td><td>Name</td><td>Department</td><td>Employee Status</td><td>Approve / Reject<BR /> Status</td><td>Remarks</td><td>Date</td></tr>");
			for (int counter = 0; counter < summaryRecords.length(); counter++) {
				Object employeeCode = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".employeecode");
				Object employeeName = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".name");
				Object employeeDepartment = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".departmentid.name");
				Object date = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE);
				Object remarks = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.REMARKS);
				Object approveOrRejectStatus = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name");
				Object employeeStatus = summaryRecords.getJSONObject(counter).opt(EMPLOYEE_STATUS);
				employeeCode = employeeCode == null ? "" : employeeCode;
				employeeName = employeeName == null ? "" : employeeName;
				employeeDepartment = employeeDepartment == null ? "" : employeeDepartment;
				date = date == null ? "" : date;
				remarks = remarks == null ? "" : remarks;
				approveOrRejectStatus = approveOrRejectStatus == null ? "" : approveOrRejectStatus;

				mailContent.append("<tr><td>").append(employeeCode).append("</td><td>").append(employeeName).append("</td><td>").append(employeeDepartment).append("</td><td>").append(employeeStatus).append("</td><td>").append(approveOrRejectStatus).append("</td><td>").append(remarks).append("</td><td>").append(date).append("</td></tr>");
			}
			mailContent.append("</table>");
		}

		String title = "Manpower Requisition Form";
		List<String> list = new ArrayList<String>();

		for (int icounter = 0; icounter < buHrEmailList.size(); icounter++) {
			Object emailId = buHrEmailList.get(icounter);
			if (!list.contains(emailId)) {
				list.add("" + emailId);
				String link = getLinkForBuHr(record.getKey(), emailId, directorEmailList);
				LogUtility.writeError("emailId >> " + emailId);
				PunchingUtility.sendMailsWithoutCC(new String[] { "" + emailId }, (mailContent.toString() + link), title);
			}

		}
		PunchingUtility.sendMailsWithoutCC(new String[] { "" + currentUserEmail }, mailContent.toString(), title);

	}

	public static Object getCurrentOrganization() throws JSONException {
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

	public static JSONArray getSummaryRecords(Object key) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".name");
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".employeecode");
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".departmentid.name");
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.REMARKS);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name");

		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR);
		columnArray.put(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO);
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.RecruitmentMrfApprovalProcess.MRF_REQUEST_ID + " = " + key);
		JSONArray summaryRecords = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.RecruitmentMrfApprovalProcess.RESOURCE);
		for (int counter = 0; counter < summaryRecords.length(); counter++) {
			JSONObject object = summaryRecords.getJSONObject(counter);
			boolean isBuHr = Translator.booleanValue(object.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_BU_HR));
			boolean isSeniorHr = Translator.booleanValue(object.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_SENIOR_HR));
			boolean isDirector = Translator.booleanValue(object.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_DIRECTOR));
			boolean isReportingTo = Translator.booleanValue(object.opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.IS_REPORTING_TO));
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
			object.put(EMPLOYEE_STATUS, status);
		}

		return summaryRecords;
	}

	public static void sendMailToDirector(Record record, JSONArray approverIdArray, List<String> directorEmailList, List<String> directorNameList, Object currentUserEmail, List<String> centralHrEmailList) throws Exception {

		JSONObject mrfDetails = putMrfDetails(record);

		boolean reasonIsNewBusiness = Translator.booleanValue(mrfDetails.opt("reasonIsNewBusiness"));
		boolean reasonIsAditionalWork = Translator.booleanValue(mrfDetails.opt("reasonIsAditionalWork"));
		boolean reasonIsReplacement = Translator.booleanValue(mrfDetails.opt("reasonIsReplacement"));
		boolean reasonIsOther = Translator.booleanValue(mrfDetails.opt("reasonIsOther"));

		StringBuilder mailContent = new StringBuilder();
		mailContent.append("Dear Sir / Madam,").append("<BR /><BR />");
		// mailContent.append(mrfDetails.opt("employeeName")).append(" has requested for Manpower.").append("<BR /><BR />");
		mailContent.append("<table border='1' cellpadding='4' cellspacing='0' width='99%'>");
		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='4'>");
		if (mrfDetails.has("organizationname")) {
			mailContent.append("<font size='4'><b>").append(mrfDetails.opt("organizationname")).append("</b></font>").append("<BR />");
		}
		mailContent.append("An ISO 9001: 2000 Certified Company").append("<BR />");
		if (mrfDetails.has("address")) {
			mailContent.append(mrfDetails.opt("address"));
		}
		mailContent.append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='3' width='75%>").append("<font size='4'><b>Manpower Requisition Form</b></font>").append("</td>");
		mailContent.append("<td align='left'>S. No. ").append(mrfDetails.opt("mrfCode")).append("<BR /><BR />Date:").append(mrfDetails.opt("requestDate")).append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Name of Requesting person</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Name").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeName")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Employee Code").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeCode")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDepartment")).append("</td></tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Job Required / Description</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("For (Name the business unit<BR />(Viz. DARCL (Trans-rail / BU-<BR />W / BU-E / BU-S / BU-N / HO-<BR />Hisar / Corporate-GGN / Any<BR />Other)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInBranch")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department / Function<BR />(Mention the sub-function<BR />also)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDepartment")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation/Level").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Job Description").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("jobDescription")).append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("No of Vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("numberOfVacency")).append("</td>").append("<td align='left'>").append("Priority").append("</td>").append("<td>").append(mrfDetails.opt("vacencyPriority")).append("</td>").append("</tr>");
		int vacencyTypeIdInt = Translator.integerValue(mrfDetails.opt("vacencyTypeId"));
		if (vacencyTypeIdInt == 1) {// permanent
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("Months").append("</td>").append("<td>").append(mrfDetails.opt("vancyMonths")).append("</td>").append("</tr>");
		} else {
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("").append("</td>").append("<td>").append("").append("</td>").append("</tr>");
		}

		mailContent.append("<tr><td align='left'>").append("Current Strength (Same<BR />Profile)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("currentStrength")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Desired Qualification").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("desiredQualification")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Required years of Experience").append("</td>").append("<td align='left' colspan='3'> Year: ").append(mrfDetails.opt("experienceYear")).append(", Months: ").append(mrfDetails.opt("experienceMonth")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Place of Initial Posting").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("initialPosting")).append("</td></tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Age Group").append("</td>").append("<td align='left'>").append(mrfDetails.opt("ageFromYear")).append(" - ").append(mrfDetails.opt("ageToYear")).append("</td>").append("<td align='left'>").append("Any Restriction on<BR />Gender?").append("</td>").append("<td>").append(mrfDetails.opt("genderRestriction")).append("</td>").append("</tr>");

		// mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>").append(mrfDetails.opt("ctcRange")).append("</td>").append("</tr>");
		// mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>").append(mrfDetails.opt("existingResourceName")).append("</td>").append("<td>").append(mrfDetails.opt("existingResourceDepartment")).append("</td>").append("</tr>");
		//
		// mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");
		//
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployee") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployeeCtc") : "").append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>Rs. ").append(mrfDetails.opt("ctcRange")).append("/- to ").append(mrfDetails.opt("ctcRangeTo")).append("/- per month").append("</td>").append("</tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>Mr. / Mrs. ").append(mrfDetails.opt("existingResourceName")).append("(").append(mrfDetails.opt("employeeCode")).append(")</td>").append("<td>Department : ").append(mrfDetails.opt("existingResourceDepartment")).append("(Auto capture from employee profile)").append("</td>").append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("Mr. / Mrs. " + mrfDetails.opt("replacementEmployee") + "(" + mrfDetails.opt("replacementEmployeeCode") + "0") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("CTC (in Rs): " + mrfDetails.opt("replacementEmployeeCtc") == null ? "" : ("CTC (in Rs): " + new DecimalFormat("0.00").format(Translator.doubleValue(mrfDetails.opt("replacementEmployeeCtc")))) + "/- per month") : "")
				.append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='4'>").append(reasonIsOther ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Any Other (Please specify)<BR />").append(reasonIsOther ? mrfDetails.opt("reasonOtherText") : "").append("</td></tr>");

		mailContent.append("</table>");

		JSONArray summaryRecords = getSummaryRecords(record.getKey());
		if (summaryRecords != null && summaryRecords.length() > 0) {
			mailContent.append("<BR />");
			mailContent.append("Approval Summary");
			mailContent.append("<BR />");
			mailContent.append("<table cellpadding='5' cellspacing='0' border='1'>");
			mailContent.append("<tr><td>E. Code</td><td>Name</td><td>Department</td><td>Employee Status</td><td>Approve / Reject<BR /> Status</td><td>Remarks</td><td>Date</td></tr>");
			for (int counter = 0; counter < summaryRecords.length(); counter++) {
				Object employeeCode = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".employeecode");
				Object employeeName = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".name");
				Object employeeDepartment = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".departmentid.name");
				Object date = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE);
				Object remarks = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.REMARKS);
				Object approveOrRejectStatus = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name");
				Object employeeStatus = summaryRecords.getJSONObject(counter).opt(EMPLOYEE_STATUS);
				employeeCode = employeeCode == null ? "" : employeeCode;
				employeeName = employeeName == null ? "" : employeeName;
				employeeDepartment = employeeDepartment == null ? "" : employeeDepartment;
				date = date == null ? "" : date;
				remarks = remarks == null ? "" : remarks;
				approveOrRejectStatus = approveOrRejectStatus == null ? "" : approveOrRejectStatus;

				mailContent.append("<tr><td>").append(employeeCode).append("</td><td>").append(employeeName).append("</td><td>").append(employeeDepartment).append("</td><td>").append(employeeStatus).append("</td><td>").append(approveOrRejectStatus).append("</td><td>").append(remarks).append("</td><td>").append(date).append("</td></tr>");
			}
			mailContent.append("</table>");
		}
		Object directorEmail = record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.DIRECTOR_EMAIL);
		String title = "Manpower Requisition Form";
		if (directorEmail != null) {
			String link = getLink(record.getKey(), directorEmail);
			PunchingUtility.sendMailsWithoutCC(new String[] { "" + directorEmail }, (mailContent.toString() + link), (title + " << Director emailId >> " + directorEmail));
		}
		PunchingUtility.sendMailsWithoutCC(new String[] { "" + currentUserEmail }, mailContent.toString(), title);

	}

	private static String getLinkForBuHr(Object key, Object emailId, List<String> directorEmailList) throws Exception {
		StringBuffer directorEmails = new StringBuffer();
		if (directorEmailList != null && directorEmailList.size() > 0) {
			directorEmails.append("<b>Select Director:</b> ").append("<select name='directorEmail'>");
			for (int directorCounter = 0; directorCounter < directorEmailList.size(); directorCounter++) {
				directorEmails.append("<option value='").append(directorEmailList.get(directorCounter)).append("'>").append(directorEmailList.get(directorCounter)).append("</option>");
			}
			directorEmails.append("</select>");
		}

		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		// messageContents.append("Approve or Reject leave(s):").append("<BR />");
		messageContents.append("<table border='0' width='50%'> <tr valign='bottom'><td align='left'>");
		// messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvemrf'>");
		messageContents.append("<INPUT TYPE='hidden' name='_mrf_' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='em' VALUE='").append(emailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='_ap_' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");// approve_by_seniour
		messageContents.append(directorEmails.toString()).append("&nbsp;&nbsp;&nbsp;&nbsp;");
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rem' ").append("' /></ br>");// remarks_for_history
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td><td align='left'>");
		// messageContents.append("<form action='http://apps.applane.com/escape/approveadvance' method='GET'>");
		messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvemrf' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='_mrf_' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='em' VALUE='").append(emailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='_ap_' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");// approve_by_seniour
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rem' ").append("' /></ br>");// remarks_for_history
		// messageContents.append("<input type='submit' style=\"font-face: 'Comic Sans MS'; font-size: 14px; color: black; background-color: #AFEEEE; border: 1.3pt ridge #5F9EA0;padding: 3px;\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	private static String getLink(Object key, Object emailId) throws Exception {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		// messageContents.append("Approve or Reject leave(s):").append("<BR />");
		messageContents.append("<table border='0' width='50%'> <tr valign='bottom'><td align='left'>");
		// messageContents.append("<form action='http://apps.applane.com/escape/approveadvance'>");
		messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvemrf'>");
		messageContents.append("<INPUT TYPE='hidden' name='_mrf_' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='em' VALUE='").append(emailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='_ap_' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");// approve_by_seniour
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rem' ").append("' /></ br>");// remarks_for_history
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td><td align='left'>");
		// messageContents.append("<form action='http://apps.applane.com/escape/approveadvance' method='GET'>");
		messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvemrf' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='_mrf_' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='em' VALUE='").append(emailId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='_ap_' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");// approve_by_seniour
		messageContents.append("<b>Remarks:</b>");
		messageContents.append("<INPUT TYPE='text' name='rem' ").append("' /></ br>");// remarks_for_history
		// messageContents.append("<input type='submit' style=\"font-face: 'Comic Sans MS'; font-size: 14px; color: black; background-color: #AFEEEE; border: 1.3pt ridge #5F9EA0;padding: 3px;\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	public static void sendMailToHrManager(Record record, JSONArray approverIdArray, List<String> centralHrEmailList, List<String> centralHrNameList, Object currentUserEmail) throws Exception {

		JSONObject mrfDetails = putMrfDetails(record);

		boolean reasonIsNewBusiness = Translator.booleanValue(mrfDetails.opt("reasonIsNewBusiness"));
		boolean reasonIsAditionalWork = Translator.booleanValue(mrfDetails.opt("reasonIsAditionalWork"));
		boolean reasonIsReplacement = Translator.booleanValue(mrfDetails.opt("reasonIsReplacement"));
		boolean reasonIsOther = Translator.booleanValue(mrfDetails.opt("reasonIsOther"));

		StringBuilder mailContent = new StringBuilder();
		mailContent.append("Dear Sir / Madam,").append("<BR /><BR />");
		// mailContent.append(mrfDetails.opt("employeeName")).append(" has requested for Manpower.").append("<BR /><BR />");
		mailContent.append("<table border='1' cellpadding='4' cellspacing='0' width='99%'>");
		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='4'>");
		if (mrfDetails.has("organizationname")) {
			mailContent.append("<font size='4'><b>").append(mrfDetails.opt("organizationname")).append("</b></font>").append("<BR />");
		}
		mailContent.append("An ISO 9001: 2000 Certified Company").append("<BR />");
		if (mrfDetails.has("address")) {
			mailContent.append(mrfDetails.opt("address"));
		}
		mailContent.append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='3' width='75%>").append("<font size='4'><b>Manpower Requisition Form</b></font>").append("</td>");
		mailContent.append("<td align='left'>S. No. ").append(mrfDetails.opt("mrfCode")).append("<BR /><BR />Date:").append(mrfDetails.opt("requestDate")).append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Name of Requesting person</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Name").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeName")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Employee Code").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeCode")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDepartment")).append("</td></tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Job Required / Description</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("For (Name the business unit<BR />(Viz. DARCL (Trans-rail / BU-<BR />W / BU-E / BU-S / BU-N / HO-<BR />Hisar / Corporate-GGN / Any<BR />Other)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInBranch")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department / Function<BR />(Mention the sub-function<BR />also)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDepartment")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation/Level").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Job Description").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("jobDescription")).append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("No of Vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("numberOfVacency")).append("</td>").append("<td align='left'>").append("Priority").append("</td>").append("<td>").append(mrfDetails.opt("vacencyPriority")).append("</td>").append("</tr>");
		int vacencyTypeIdInt = Translator.integerValue(mrfDetails.opt("vacencyTypeId"));
		if (vacencyTypeIdInt == 1) {// permanent
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("Months").append("</td>").append("<td>").append(mrfDetails.opt("vancyMonths")).append("</td>").append("</tr>");
		} else {
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("").append("</td>").append("<td>").append("").append("</td>").append("</tr>");
		}

		mailContent.append("<tr><td align='left'>").append("Current Strength (Same<BR />Profile)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("currentStrength")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Desired Qualification").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("desiredQualification")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Required years of Experience").append("</td>").append("<td align='left' colspan='3'> Year: ").append(mrfDetails.opt("experienceYear")).append(", Months: ").append(mrfDetails.opt("experienceMonth")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Place of Initial Posting").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("initialPosting")).append("</td></tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Age Group").append("</td>").append("<td align='left'>").append(mrfDetails.opt("ageFromYear")).append(" - ").append(mrfDetails.opt("ageToYear")).append("</td>").append("<td align='left'>").append("Any Restriction on<BR />Gender?").append("</td>").append("<td>").append(mrfDetails.opt("genderRestriction")).append("</td>").append("</tr>");

		// mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>").append(mrfDetails.opt("ctcRange")).append("</td>").append("</tr>");
		// mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>").append(mrfDetails.opt("existingResourceName")).append("</td>").append("<td>").append(mrfDetails.opt("existingResourceDepartment")).append("</td>").append("</tr>");
		//
		// mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");
		//
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployee") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployeeCtc") : "").append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>Rs. ").append(mrfDetails.opt("ctcRange")).append("/- to ").append(mrfDetails.opt("ctcRangeTo")).append("/- per month").append("</td>").append("</tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>Mr. / Mrs. ").append(mrfDetails.opt("existingResourceName")).append("(").append(mrfDetails.opt("employeeCode")).append(")</td>").append("<td>Department : ").append(mrfDetails.opt("existingResourceDepartment")).append("(Auto capture from employee profile)").append("</td>").append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("Mr. / Mrs. " + mrfDetails.opt("replacementEmployee") + "(" + mrfDetails.opt("replacementEmployeeCode") + "0") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("CTC (in Rs): " + mrfDetails.opt("replacementEmployeeCtc") == null ? "" : ("CTC (in Rs): " + new DecimalFormat("0.00").format(Translator.doubleValue(mrfDetails.opt("replacementEmployeeCtc")))) + "/- per month") : "")
				.append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='4'>").append(reasonIsOther ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Any Other (Please specify)<BR />").append(reasonIsOther ? mrfDetails.opt("reasonOtherText") : "").append("</td></tr>");

		mailContent.append("</table>");

		JSONArray summaryRecords = getSummaryRecords(record.getKey());
		if (summaryRecords != null && summaryRecords.length() > 0) {
			mailContent.append("<BR />");
			mailContent.append("Approval Summary");
			mailContent.append("<BR />");
			mailContent.append("<table cellpadding='5' cellspacing='0' border='1'>");
			mailContent.append("<tr><td>E. Code</td><td>Name</td><td>Department</td><td>Employee Status</td><td>Approve / Reject<BR /> Status</td><td>Remarks</td><td>Date</td></tr>");
			for (int counter = 0; counter < summaryRecords.length(); counter++) {
				Object employeeCode = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".employeecode");
				Object employeeName = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".name");
				Object employeeDepartment = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".departmentid.name");
				Object date = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE);
				Object remarks = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.REMARKS);
				Object approveOrRejectStatus = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name");
				Object employeeStatus = summaryRecords.getJSONObject(counter).opt(EMPLOYEE_STATUS);
				employeeCode = employeeCode == null ? "" : employeeCode;
				employeeName = employeeName == null ? "" : employeeName;
				employeeDepartment = employeeDepartment == null ? "" : employeeDepartment;
				date = date == null ? "" : date;
				remarks = remarks == null ? "" : remarks;
				approveOrRejectStatus = approveOrRejectStatus == null ? "" : approveOrRejectStatus;

				mailContent.append("<tr><td>").append(employeeCode).append("</td><td>").append(employeeName).append("</td><td>").append(employeeDepartment).append("</td><td>").append(employeeStatus).append("</td><td>").append(approveOrRejectStatus).append("</td><td>").append(remarks).append("</td><td>").append(date).append("</td></tr>");
			}
			mailContent.append("</table>");
		}

		String title = "Manpower Requisition Form";
		List<String> list = new ArrayList<String>();

		for (int icounter = 0; icounter < centralHrEmailList.size(); icounter++) {
			Object emailId = centralHrEmailList.get(icounter);
			if (!list.contains(emailId)) {
				list.add("" + emailId);
				String link = getLink(record.getKey(), emailId);
				LogUtility.writeError("emailId >> " + emailId);
				if (emailId != null) {
					PunchingUtility.sendMailsWithoutCC(new String[] { "" + emailId }, (mailContent.toString() + link), title);
				}
			}

		}
		PunchingUtility.sendMailsWithoutCC(new String[] { "" + currentUserEmail }, mailContent.toString(), title); // send mail to approver

	}

	public static void sendMailToEmployeeOnReject(Record record, JSONArray approverIdArray, Object currentUserEmail) throws Exception {

		JSONObject mrfDetails = putMrfDetails(record);

		boolean reasonIsNewBusiness = Translator.booleanValue(mrfDetails.opt("reasonIsNewBusiness"));
		boolean reasonIsAditionalWork = Translator.booleanValue(mrfDetails.opt("reasonIsAditionalWork"));
		boolean reasonIsReplacement = Translator.booleanValue(mrfDetails.opt("reasonIsReplacement"));
		boolean reasonIsOther = Translator.booleanValue(mrfDetails.opt("reasonIsOther"));

		StringBuilder mailContent = new StringBuilder();
		mailContent.append("Dear Sir / Madam,").append("<BR /><BR />");
		// mailContent.append(mrfDetails.opt("employeeName")).append(" has requested for Manpower.").append("<BR /><BR />");
		mailContent.append("<table border='1' cellpadding='4' cellspacing='0' width='99%'>");
		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='4'>");
		if (mrfDetails.has("organizationname")) {
			mailContent.append("<font size='4'><b>").append(mrfDetails.opt("organizationname")).append("</b></font>").append("<BR />");
		}
		mailContent.append("An ISO 9001: 2000 Certified Company").append("<BR />");
		if (mrfDetails.has("address")) {
			mailContent.append(mrfDetails.opt("address"));
		}
		mailContent.append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='3' width='75%>").append("<font size='4'><b>Manpower Requisition Form</b></font>").append("</td>");
		mailContent.append("<td align='left'>S. No. ").append(mrfDetails.opt("mrfCode")).append("<BR /><BR />Date:").append(mrfDetails.opt("requestDate")).append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Name of Requesting person</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Name").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeName")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Employee Code").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeCode")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDepartment")).append("</td></tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Job Required / Description</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("For (Name the business unit<BR />(Viz. DARCL (Trans-rail / BU-<BR />W / BU-E / BU-S / BU-N / HO-<BR />Hisar / Corporate-GGN / Any<BR />Other)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInBranch")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department / Function<BR />(Mention the sub-function<BR />also)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDepartment")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation/Level").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Job Description").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("jobDescription")).append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("No of Vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("numberOfVacency")).append("</td>").append("<td align='left'>").append("Priority").append("</td>").append("<td>").append(mrfDetails.opt("vacencyPriority")).append("</td>").append("</tr>");
		int vacencyTypeIdInt = Translator.integerValue(mrfDetails.opt("vacencyTypeId"));
		if (vacencyTypeIdInt == 1) {// permanent
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("Months").append("</td>").append("<td>").append(mrfDetails.opt("vancyMonths")).append("</td>").append("</tr>");
		} else {
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("").append("</td>").append("<td>").append("").append("</td>").append("</tr>");
		}

		mailContent.append("<tr><td align='left'>").append("Current Strength (Same<BR />Profile)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("currentStrength")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Desired Qualification").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("desiredQualification")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Required years of Experience").append("</td>").append("<td align='left' colspan='3'> Year: ").append(mrfDetails.opt("experienceYear")).append(", Months: ").append(mrfDetails.opt("experienceMonth")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Place of Initial Posting").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("initialPosting")).append("</td></tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Age Group").append("</td>").append("<td align='left'>").append(mrfDetails.opt("ageFromYear")).append(" - ").append(mrfDetails.opt("ageToYear")).append("</td>").append("<td align='left'>").append("Any Restriction on<BR />Gender?").append("</td>").append("<td>").append(mrfDetails.opt("genderRestriction")).append("</td>").append("</tr>");

		// mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>").append(mrfDetails.opt("ctcRange")).append("</td>").append("</tr>");
		// mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>").append(mrfDetails.opt("existingResourceName")).append("</td>").append("<td>").append(mrfDetails.opt("existingResourceDepartment")).append("</td>").append("</tr>");
		//
		// mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");
		//
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployee") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployeeCtc") : "").append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>Rs. ").append(mrfDetails.opt("ctcRange")).append("/- to ").append(mrfDetails.opt("ctcRangeTo")).append("/- per month").append("</td>").append("</tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>Mr. / Mrs. ").append(mrfDetails.opt("existingResourceName")).append("(").append(mrfDetails.opt("employeeCode")).append(")</td>").append("<td>Department : ").append(mrfDetails.opt("existingResourceDepartment")).append("(Auto capture from employee profile)").append("</td>").append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("Mr. / Mrs. " + mrfDetails.opt("replacementEmployee") + "(" + mrfDetails.opt("replacementEmployeeCode") + "0") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("CTC (in Rs): " + mrfDetails.opt("replacementEmployeeCtc") == null ? "" : ("CTC (in Rs): " + new DecimalFormat("0.00").format(Translator.doubleValue(mrfDetails.opt("replacementEmployeeCtc")))) + "/- per month") : "")
				.append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='4'>").append(reasonIsOther ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Any Other (Please specify)<BR />").append(reasonIsOther ? mrfDetails.opt("reasonOtherText") : "").append("</td></tr>");

		mailContent.append("</table>");
		LogUtility.writeError("mrfDetails >> " + mrfDetails);
		String title = "Manpower Requisition Form Rejected";
		if (mrfDetails.has("employeeReportingToEmail")) {
			PunchingUtility.sendMailsAccToProfitCenters(new String[] { "" + mrfDetails.opt("employeeReportingToEmail") }, mailContent.toString(), title, new String[] { "" + currentUserEmail });

		}
	}

	public static void sendMailToSelectedEmails(Record record, JSONArray approverIdArray, Object currentUserEmail) throws Exception {

		JSONObject mrfDetails = putMrfDetails(record);

		boolean reasonIsNewBusiness = Translator.booleanValue(mrfDetails.opt("reasonIsNewBusiness"));
		boolean reasonIsAditionalWork = Translator.booleanValue(mrfDetails.opt("reasonIsAditionalWork"));
		boolean reasonIsReplacement = Translator.booleanValue(mrfDetails.opt("reasonIsReplacement"));
		boolean reasonIsOther = Translator.booleanValue(mrfDetails.opt("reasonIsOther"));

		StringBuilder mailContent = new StringBuilder();
		mailContent.append("Dear Sir / Madam,").append("<BR /><BR />");
		// mailContent.append(mrfDetails.opt("employeeName")).append(" has requested for Manpower.").append("<BR /><BR />");
		mailContent.append("<table border='1' cellpadding='4' cellspacing='0' width='99%'>");
		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='4'>");
		if (mrfDetails.has("organizationname")) {
			mailContent.append("<font size='4'><b>").append(mrfDetails.opt("organizationname")).append("</b></font>").append("<BR />");
		}
		mailContent.append("An ISO 9001: 2000 Certified Company").append("<BR />");
		if (mrfDetails.has("address")) {
			mailContent.append(mrfDetails.opt("address"));
		}
		mailContent.append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr>");
		mailContent.append("<td align='center' colspan='3' width='75%>").append("<font size='4'><b>Manpower Requisition Form</b></font>").append("</td>");
		mailContent.append("<td align='left'>S. No. ").append(mrfDetails.opt("mrfCode")).append("<BR /><BR />Date:").append(mrfDetails.opt("requestDate")).append("</td>");
		mailContent.append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Name of Requesting person</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Name").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeName")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Employee Code").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeCode")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("employeeDepartment")).append("</td></tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Job Required / Description</b></font>").append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("For (Name the business unit<BR />(Viz. DARCL (Trans-rail / BU-<BR />W / BU-E / BU-S / BU-N / HO-<BR />Hisar / Corporate-GGN / Any<BR />Other)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInBranch")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Department / Function<BR />(Mention the sub-function<BR />also)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDepartment")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Designation/Level").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("requirementInDesignation")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Job Description").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("jobDescription")).append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("No of Vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("numberOfVacency")).append("</td>").append("<td align='left'>").append("Priority").append("</td>").append("<td>").append(mrfDetails.opt("vacencyPriority")).append("</td>").append("</tr>");
		int vacencyTypeIdInt = Translator.integerValue(mrfDetails.opt("vacencyTypeId"));
		if (vacencyTypeIdInt == 1) {// permanent
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("Months").append("</td>").append("<td>").append(mrfDetails.opt("vancyMonths")).append("</td>").append("</tr>");
		} else {
			mailContent.append("<tr>").append("<td align='left'>").append("Type of vacancies").append("</td>").append("<td align='left'>").append(mrfDetails.opt("vacencyTypeName")).append("</td>").append("<td align='left'>").append("").append("</td>").append("<td>").append("").append("</td>").append("</tr>");
		}

		mailContent.append("<tr><td align='left'>").append("Current Strength (Same<BR />Profile)").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("currentStrength")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Desired Qualification").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("desiredQualification")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Required years of Experience").append("</td>").append("<td align='left' colspan='3'> Year: ").append(mrfDetails.opt("experienceYear")).append(", Months: ").append(mrfDetails.opt("experienceMonth")).append("</td></tr>");
		mailContent.append("<tr><td align='left'>").append("Place of Initial Posting").append("</td>").append("<td align='left' colspan='3'>").append(mrfDetails.opt("initialPosting")).append("</td></tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Age Group").append("</td>").append("<td align='left'>").append(mrfDetails.opt("ageFromYear")).append(" - ").append(mrfDetails.opt("ageToYear")).append("</td>").append("<td align='left'>").append("Any Restriction on<BR />Gender?").append("</td>").append("<td>").append(mrfDetails.opt("genderRestriction")).append("</td>").append("</tr>");

		// mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>").append(mrfDetails.opt("ctcRange")).append("</td>").append("</tr>");
		// mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>").append(mrfDetails.opt("existingResourceName")).append("</td>").append("<td>").append(mrfDetails.opt("existingResourceDepartment")).append("</td>").append("</tr>");
		//
		// mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");
		//
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		// mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployee") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? mrfDetails.opt("replacementEmployeeCtc") : "").append("</td></tr>");

		mailContent.append("<tr>").append("<td align='left'>").append("Preference(from any specific<BR />geographic or domain area)").append("</td>").append("<td align='left'>").append(mrfDetails.opt("prefference")).append("</td>").append("<td align='left'>").append("Compensation Range(in<BR />INR and CTC annually)").append("</td>").append("<td>Rs. ").append(mrfDetails.opt("ctcRange")).append("/- to ").append(mrfDetails.opt("ctcRangeTo")).append("/- per month").append("</td>").append("</tr>");
		mailContent.append("<tr>").append("<td align='left'>").append("Is Required Skill Set Available<BR />in the Company").append("</td>").append("<td align='left'>").append(mrfDetails.opt("isExistingResource")).append("</td>").append("<td align='left'>Mr. / Mrs. ").append(mrfDetails.opt("existingResourceName")).append("(").append(mrfDetails.opt("employeeCode")).append(")</td>").append("<td>Department : ").append(mrfDetails.opt("existingResourceDepartment")).append("(Auto capture from employee profile)").append("</td>").append("</tr>");

		mailContent.append("<tr><td align='center' colspan='4' bgcolor='A9A9A9'>").append("<font size='3'><b>Reasons</b></font>").append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsNewBusiness ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("New business<BR />Business expanded from existing customer").append("</td>").append("<td align='left' colspan='2'>").append(reasonIsAditionalWork ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Expansion in department due to additional<BR />work/assignments.").append("</td></tr>");
		mailContent.append("<tr><td align='left' colspan='2'>").append(reasonIsReplacement ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Replacement of (Mention the name and<BR />last CTC drawn of the individual whose<BR />replacement is sought)").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("Mr. / Mrs. " + mrfDetails.opt("replacementEmployee") + "(" + mrfDetails.opt("replacementEmployeeCode") + "0") : "").append("</td>").append("<td align='left'>").append(reasonIsReplacement ? ("CTC (in Rs): " + mrfDetails.opt("replacementEmployeeCtc") == null ? "" : ("CTC (in Rs): " + new DecimalFormat("0.00").format(Translator.doubleValue(mrfDetails.opt("replacementEmployeeCtc")))) + "/- per month") : "")
				.append("</td></tr>");

		mailContent.append("<tr><td align='left' colspan='4'>").append(reasonIsOther ? "<input type=\"checkbox\" DISABLED CHECKED>" : "<input type=\"checkbox\" DISABLED UNCHECKED>").append("Any Other (Please specify)<BR />").append(reasonIsOther ? mrfDetails.opt("reasonOtherText") : "").append("</td></tr>");

		mailContent.append("</table>");

		JSONArray summaryRecords = getSummaryRecords(record.getKey());
		if (summaryRecords != null && summaryRecords.length() > 0) {
			mailContent.append("<BR />");
			mailContent.append("Approval Summary");
			mailContent.append("<BR />");
			mailContent.append("<table cellpadding='5' cellspacing='0' border='1'>");
			mailContent.append("<tr><td>E. Code</td><td>Name</td><td>Department</td><td>Employee Status</td><td>Approve / Reject<BR /> Status</td><td>Remarks</td><td>Date</td></tr>");
			for (int counter = 0; counter < summaryRecords.length(); counter++) {
				Object employeeCode = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".employeecode");
				Object employeeName = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".name");
				Object employeeDepartment = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.EMPLOYEE_ID + ".departmentid.name");
				Object date = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.DATE);
				Object remarks = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.REMARKS);
				Object approveOrRejectStatus = summaryRecords.getJSONObject(counter).opt(HRISApplicationConstants.RecruitmentMrfApprovalProcess.STATUS_ID + ".name");
				Object employeeStatus = summaryRecords.getJSONObject(counter).opt(EMPLOYEE_STATUS);
				employeeCode = employeeCode == null ? "" : employeeCode;
				employeeName = employeeName == null ? "" : employeeName;
				employeeDepartment = employeeDepartment == null ? "" : employeeDepartment;
				date = date == null ? "" : date;
				remarks = remarks == null ? "" : remarks;
				approveOrRejectStatus = approveOrRejectStatus == null ? "" : approveOrRejectStatus;

				mailContent.append("<tr><td>").append(employeeCode).append("</td><td>").append(employeeName).append("</td><td>").append(employeeDepartment).append("</td><td>").append(employeeStatus).append("</td><td>").append(approveOrRejectStatus).append("</td><td>").append(remarks).append("</td><td>").append(date).append("</td></tr>");
			}
			mailContent.append("</table>");
		}
		Object employeeId = record.getValue(HRISApplicationConstants.RecruitmentMrfProcess.EMPLOYEE_ID);
		JSONArray selectedEmailsArray = getSelectedEmailsArray(employeeId);
		String[] emails = getEmails(selectedEmailsArray);
		LogUtility.writeError("mrfDetails >> " + mrfDetails);
		String title = "Manpower Requisition Form Approved";

		PunchingUtility.sendMailsAccToProfitCenters(new String[] { "" + currentUserEmail, "" + mrfDetails.opt("employeeReportingToEmail") }, mailContent.toString(), title, emails);

	}

	private static String[] getEmails(JSONArray selectedEmailsArray) throws Exception {
		Object profitCentersObject = selectedEmailsArray.getJSONObject(0).opt("profitcenterid");
		JSONArray profitCenterArray = new JSONArray();
		if (profitCentersObject instanceof JSONArray) {
			profitCenterArray = (JSONArray) profitCentersObject;
		} else if (profitCentersObject instanceof JSONObject) {
			profitCenterArray.put((JSONObject) profitCentersObject);
		}
		if (profitCenterArray != null && profitCenterArray.length() > 0) {
			String[] emails = new String[selectedEmailsArray.length()];
			for (int counter = 0; counter < profitCenterArray.length(); counter++) {
				Object emailsEmailIdObject = profitCenterArray.getJSONObject(counter).opt("emails.officialemailid");
				emails[counter] = "" + emailsEmailIdObject;
			}
			return emails;
		}
		return null;
	}

	private static JSONArray getSelectedEmailsArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);

		JSONArray childColumns = new JSONArray();
		childColumns.put("emails.officialemailid");
		childColumns.put("emails.name");
		childColumns.put("emails");

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
}
