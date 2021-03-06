package com.applane.resourceoriented.hris.tour;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.1
 * @category HRIS businesslogic
 */

public class TourBeforeApprovalRemarksBusinessLogic implements OperationJob {
	// job define on "hris_tourrequests"
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();
		Object employeeIdObject = record.getValue(HRISApplicationConstants.TourBeforeApprovalComments.EMPLOYEE_ID);
		// Object date = record.getValue(HRISApplicationConstants.TourBeforeApprovalComments.DATE);
		// Object remarks = record.getValue(HRISApplicationConstants.TourBeforeApprovalComments.REMARKS);
		Object tourId = record.getValue(HRISApplicationConstants.TourBeforeApprovalComments.TOUR_ID);

		try {
			if (operation.equalsIgnoreCase("insert")) {
				JSONObject employeeDetails = putEmployeeDetailsIntoObject(record);

				Date tourStartDate = null;
				Date tourEndDate = null;
				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

				if (employeeDetails.opt("startDateObject") != null && employeeDetails.opt("endDateObject") != null) {
					try {
						tourStartDate = updateDateFormat.parse("" + employeeDetails.opt("startDateObject"));
						tourEndDate = updateDateFormat.parse("" + employeeDetails.opt("endDateObject"));
					} catch (ParseException e) {
						throw new RuntimeException("Error Come while parsing " + e.getMessage());
					}
				}
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
				String startDate = dateFormat.format(tourStartDate);
				String endDate = dateFormat.format(tourEndDate);
				List<String> employeeEmailIdList = new ArrayList<String>();
				employeeEmailIdList.add("" + employeeDetails.opt("employeeEmailId"));
				try {
					JSONArray queryArray = getQueryArray(tourId);
					String tableQueryArray = "";
					if (queryArray != null && queryArray.length() > 0) {
						tableQueryArray = getTableFormatOfQueryArray(queryArray);
					}
					employeeEmailIdList.add((String) employeeDetails.optString("approverEmailId"));

					sendTourMailOnInsertUpdate(employeeEmailIdList, startDate, endDate, employeeDetails, employeeIdObject, tableQueryArray);
				} catch (Exception e) {
					throw new RuntimeException("Error Come: " + e.getMessage());

				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("Tour Mail On After mail Exception >> " + trace);
			throw new BusinessLogicException(trace);
		}
	}

	private String getTableFormatOfQueryArray(JSONArray queryArray) throws Exception {
		String valueToMail = "";
		valueToMail += "<table border='1' cellspacing='0'><tr bgcolor='99CCCC'><td>Employee Name</td><td>Date</td><td>Remarks</td></tr>";
		for (int counter = 0; counter < queryArray.length(); counter++) {
			Object employeeName = queryArray.getJSONObject(counter).opt(HRISApplicationConstants.TourBeforeApprovalComments.EMPLOYEE_ID + ".name");
			Object date = queryArray.getJSONObject(counter).opt(HRISApplicationConstants.TourBeforeApprovalComments.DATE);
			Object remarks = queryArray.getJSONObject(counter).opt(HRISApplicationConstants.TourBeforeApprovalComments.REMARKS);
			valueToMail += "<tr ><td>" + employeeName + "</td><td>" + date + "</td><td>" + remarks + "</td></tr>";
		}
		valueToMail += "</table>";
		return valueToMail;
	}

	private JSONArray getQueryArray(Object tourId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.TourBeforeApprovalComments.RESOURCE);
		JSONArray columnArray = new JSONArray();

		columnArray.put(HRISApplicationConstants.TourBeforeApprovalComments.REMARKS);
		columnArray.put(HRISApplicationConstants.TourBeforeApprovalComments.EMPLOYEE_ID + ".name");
		columnArray.put(HRISApplicationConstants.TourBeforeApprovalComments.DATE);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "tour_id in (" + tourId + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		return employeeObject.getJSONArray(HRISApplicationConstants.TourBeforeApprovalComments.RESOURCE);
	}

	private JSONObject putEmployeeDetailsIntoObject(Record record) throws Exception {
		Object tourId = record.getValue(HRISApplicationConstants.TourBeforeApprovalComments.TOUR_ID);
		JSONArray tourDetails = getTourDetails(tourId);
		if (tourDetails != null && tourDetails.length() > 0) {

			Object key = tourDetails.getJSONObject(0).opt(Updates.KEY);
			Object employeeId = tourDetails.getJSONObject(0).opt("employeeid");
			Object branchId = tourDetails.getJSONObject(0).opt("employeeid.branchid");
			Object employeeCode = tourDetails.getJSONObject(0).opt("employeeid.employeecode");
			Object branchName = tourDetails.getJSONObject(0).opt("employeeid.branchid.name");
			Object approverIdObject = tourDetails.getJSONObject(0).opt("approverid");
			Object tourTypeIdObject = tourDetails.getJSONObject(0).opt("tourtypeid");
			Object startDateObject = tourDetails.getJSONObject(0).opt("startdate");
			Object endDateObject = tourDetails.getJSONObject(0).opt("enddate");
			Object advanceAmount = tourDetails.getJSONObject(0).opt("advanceamount_amount");
			Object advanceAgainstTickting = tourDetails.getJSONObject(0).opt("advance_against_tickting");
			String tourPurpose = Translator.stringValue(tourDetails.getJSONObject(0).opt("tourpurpose"));
			String tourCode = Translator.stringValue(tourDetails.getJSONObject(0).opt("tourcode"));

			String employeeEmailId = Translator.stringValue(tourDetails.getJSONObject(0).opt("employeeid.officialemailid"));
			String employeeName = Translator.stringValue(tourDetails.getJSONObject(0).opt("employeeid.name"));

			String approverEmailId = Translator.stringValue(tourDetails.getJSONObject(0).opt("approverid.officialemailid"));
			String approverName = Translator.stringValue(tourDetails.getJSONObject(0).opt("approverid.name"));
			String approverId = Translator.stringValue(tourDetails.getJSONObject(0).opt("approverid"));

			String tourType = Translator.stringValue(tourDetails.getJSONObject(0).opt("tourtypeid.name"));
			Object genderId = tourDetails.getJSONObject(0).opt("employeeid.genderid");

			Object substituteName = tourDetails.getJSONObject(0).opt("substituteid.name");
			Object substituteGenderId = tourDetails.getJSONObject(0).opt("substituteid.genderid");

			String gender = "";
			String gender1 = "";
			if (Translator.integerValue(genderId) == 1) {
				gender = "Mr. ";
				gender1 = "His ";
			} else if (Translator.integerValue(genderId) == 2) {
				gender = "Ms. ";
				gender1 = "Her ";
			} else {
				gender = "Mr./Ms. ";
				gender1 = "His/Her ";
			}
			String substituteGender = "";
			String substituteGender1 = "";
			if (Translator.integerValue(substituteGenderId) == 1) {
				substituteGender = "Mr. ";
				substituteGender1 = "His ";
			} else if (Translator.integerValue(substituteGenderId) == 2) {
				substituteGender = "Ms. ";
				substituteGender1 = "Her ";
			} else {
				substituteGender = "Mr./Ms. ";
				substituteGender1 = "His/Her ";
			}

			JSONObject employeeDetails = new JSONObject();
			employeeDetails.put("key", key);
			employeeDetails.put("employeeId", employeeId);
			employeeDetails.put("approverIdObject", approverIdObject);
			employeeDetails.put("tourTypeIdObject", tourTypeIdObject);
			employeeDetails.put("startDateObject", startDateObject);
			employeeDetails.put("endDateObject", endDateObject);
			employeeDetails.put("tourPurpose", tourPurpose);
			employeeDetails.put("tourCode", tourCode);
			employeeDetails.put("employeeEmailId", employeeEmailId);
			employeeDetails.put("employeeName", employeeName);
			employeeDetails.put("approverEmailId", approverEmailId);
			employeeDetails.put("approverName", approverName);
			employeeDetails.put("approverId", approverId);
			employeeDetails.put("tourType", tourType);
			employeeDetails.put("branchId", branchId);
			employeeDetails.put("employeeCode", employeeCode);
			employeeDetails.put("branchName", branchName);
			if ((Translator.doubleValue(advanceAmount) + Translator.doubleValue(advanceAgainstTickting)) > 0.0) {
				employeeDetails.put("totalAdvance", (Translator.doubleValue(advanceAmount) + Translator.doubleValue(advanceAgainstTickting)));
			}
			employeeDetails.put("gender", gender);
			employeeDetails.put("gender1", gender1);
			if (substituteName != null) {
				employeeDetails.put("substituteName", substituteName);
				employeeDetails.put("substituteGender", substituteGender);
				employeeDetails.put("substituteGender1", substituteGender1);
			}
			return employeeDetails;
		}
		return null;
	}

	private JSONArray getTourDetails(Object tourId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_tourrequests");
		JSONArray columnArray = new JSONArray();

		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.branchid");
		columnArray.put("employeeid.genderid");
		columnArray.put("employeeid.employeecode");
		columnArray.put("employeeid.branchid.name");
		columnArray.put("employeeid.officialemailid");
		columnArray.put("approverid");
		columnArray.put("approverid.officialemailid");
		columnArray.put("approverid.name");
		columnArray.put("tourtypeid");
		columnArray.put("startdate");
		columnArray.put("enddate");
		columnArray.put("advanceamount_amount");
		columnArray.put("advance_against_tickting");
		columnArray.put("tourpurpose");
		columnArray.put("tourcode");
		columnArray.put("tourtypeid.name");
		columnArray.put("substituteid.name");
		columnArray.put("substituteid.genderid");
		columnArray.put("tourstatusid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + tourId + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		return employeeObject.getJSONArray("hris_tourrequests");
	}

	private String getLinkToApproveByMail(Object key, Object approverId) throws JSONException {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		messageContents.append("<table border='0' width='99%'> <tr><td>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvetour' method='GET'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvetour' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='text' name='query' ").append("/>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Send Query  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

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
		employeeObject = ResourceEngine.query(query);
		JSONArray organizationArray = employeeObject.getJSONArray("up_organizations");
		if (organizationArray != null && organizationArray.length() > 0) {
			return organizationArray.getJSONObject(0).opt("organization");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void sendTourMailOnInsertUpdate(List<String> employeeEmailIdList, String startDate, String endDate, JSONObject employeeDetails, Object employeeIdObject, String tableQueryArray) {
		ApplaneMail mailSender = new ApplaneMail();

		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);

			int currentEmployeeId = Translator.integerValue(employeeIdObject);
			int applyByEmployeeId = Translator.integerValue(employeeDetails.get("employeeId"));
			// int approverId = Translator.integerValue(employeeDetails.get("approverId"));

			if (!disableEmail) {
				if (organizationLogo == null || signature == null) {
					WarningCollector.collect("Kindly configure your Communication from SETUP.");
				} else {
					String title = "Request For Tour (" + employeeDetails.opt("tourCode") + ")";

					int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
					List<String> list = new ArrayList<String>();
					for (int counter = 0; counter < mailToCount; counter++) {
						try {
							String mailTo = employeeEmailIdList.get(counter);
							if (!list.contains(mailTo)) {
								list.add(mailTo);

								StringBuffer messageContents = new StringBuffer();
								messageContents.append("<img src=\"" + organizationLogo + " \"/>");
								messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
								if (tableQueryArray.length() > 0) {
									messageContents.append(tableQueryArray).append("<BR /><BR />");
									messageContents.append("<b>Tour Details:</b>").append("<BR />");
								}
								messageContents.append("Dear Sir/Madam,<BR><BR>");
								messageContents.append(employeeDetails.opt("gender")).append(" ").append(employeeDetails.opt("employeeName")).append(" (Employee Code ").append(employeeDetails.opt("employeeCode")).append(") working in ").append(employeeDetails.opt("branchName")).append(" has entered ").append(employeeDetails.opt("gender1")).append(" tour request. Detail as given below: <BR /><BR />");

								messageContents.append("<table border='0' width='99%'>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Tour Code : </B>").append(employeeDetails.opt("tourCode")).append("</td></tr>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Tour Type : </B>").append(employeeDetails.opt("tourType")).append("</td></tr>");
								messageContents.append("<tr><td width='50%'>").append("<B>Depart On : </B>").append(startDate).append("</td>");
								messageContents.append("<td width='50%'>").append("<B>Arrive On : </B>").append(endDate).append("</td></tr>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Tour Purpose : </B>").append(employeeDetails.opt("tourPurpose")).append("</td></tr>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Advance Amount Required (If Any): </B>").append(employeeDetails.opt("totalAdvance") == null ? "" : employeeDetails.opt("totalAdvance")).append("</td></tr>");
								messageContents.append("</table><BR />");
								if (employeeDetails.has("substituteName")) {
									messageContents.append(employeeDetails.opt("substituteGender")).append(employeeDetails.opt("substituteName")).append(" would be  ").append(employeeDetails.opt("gender1")).append(" substitute during tour period.").append("<BR />");
								}

								if (mailTo.equals(("" + employeeDetails.opt("employeeEmailId")))) {
									String link = getLinkToApproveByMail(employeeDetails.opt("key"), applyByEmployeeId);
									messageContents.append(link);
								} else {
									String link = getLinkToApproveByMail(employeeDetails.opt("key"), employeeDetails.opt("approverIdObject"));
									messageContents.append(link);
								}

								if (currentEmployeeId == applyByEmployeeId) {
									messageContents.append("<BR />Regards, <BR />");
									messageContents.append(employeeDetails.opt("employeeName") + "<BR />");
								} else {
									messageContents.append("<BR />Regards, <BR />");
									messageContents.append(employeeDetails.opt("approverName") + "<BR />");
								}
								messageContents.append("(" + organizationName + ")");
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo(mailTo);
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							}
						} catch (ApplaneMailException e) {
							throw new RuntimeException("Some unknown error occured " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e1) {
			throw new RuntimeException("Some unknown error occured while send mail " + e1.getMessage());
		}
	}

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

	}

}
