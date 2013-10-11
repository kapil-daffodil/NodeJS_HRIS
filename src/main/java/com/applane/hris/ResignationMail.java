package com.applane.hris;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.app.shared.constants.View;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.service.smshelper.SendSMSService;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class ResignationMail implements OperationJob {

	// job define on "hris_employeeresignations"
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();
		if (operation.equalsIgnoreCase("insert")) {

			Object employeeIdObject = record.getValue("employeeid");
			Object approverIdObject = record.getValue("approverid");
			Object relievingDateObject = record.getValue("proposedrelievingdate");
			String resignationReason = (String) record.getValue("reason");
			String resignationCode = (String) record.getValue("resignationcode");

			Date relievingDate = null;
			SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

			if (relievingDateObject != null) {
				try {
					relievingDate = updateDateFormat.parse("" + relievingDateObject);
				} catch (ParseException e) {
					throw new BusinessLogicException("Error Come while parsing date. " + e.getMessage());
				}
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
			String employeeRelievingDate = dateFormat.format(relievingDate);
			List<String> employeeEmailIdList = new ArrayList<String>();

			try {
				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_employees");
				JSONArray columnArray = new JSONArray();
				columnArray.put("officialemailid");
				columnArray.put("name");
				columnArray.put("mobileno");
				query.put(Data.Query.COLUMNS, columnArray);
				query.put(Data.Query.FILTERS, "__key__= " + employeeIdObject);
				JSONObject employeeObject;
				employeeObject = ResourceEngine.query(query);
				JSONArray employeeIdArray = employeeObject.getJSONArray("hris_employees");
				String employeeEmailId = (String) employeeIdArray.getJSONObject(0).opt("officialemailid");
				String employeeName = (String) employeeIdArray.getJSONObject(0).opt("name");
				String employeeMobileNo = (String) employeeIdArray.getJSONObject(0).opt("mobileno");

				if (employeeEmailId != null) {
					employeeEmailIdList.add((String) employeeEmailId);
				}

				JSONObject approverQuery = new JSONObject();
				approverQuery.put(Data.Query.RESOURCE, "hris_employees");
				JSONArray array = new JSONArray();
				array.put("officialemailid");
				array.put("name");
				array.put("mobileno");
				approverQuery.put(Data.Query.COLUMNS, array);
				approverQuery.put(Data.Query.FILTERS, "__key__= " + approverIdObject);
				JSONObject approverObject;
				approverObject = ResourceEngine.query(approverQuery);
				JSONArray approverIdArray = approverObject.getJSONArray("hris_employees");
				String approverEmailId = (String) approverIdArray.getJSONObject(0).opt("officialemailid");
				String approverName = (String) approverIdArray.getJSONObject(0).opt("name");
				String approverMobileNo = (String) approverIdArray.getJSONObject(0).opt("mobileno");
				if (approverEmailId != null) {
					employeeEmailIdList.add((String) approverEmailId);
				}

				sendResignationMail(employeeEmailIdList, employeeName, employeeEmailId, approverName, approverEmailId, employeeRelievingDate, resignationReason, resignationCode);
				sendResignationSMS(employeeMobileNo, approverMobileNo, employeeName, employeeRelievingDate);
			} catch (Exception e) {
				throw new BusinessLogicException("Error come while Send Email or SMS." + e.getMessage());
			}
		} else if (operation.equalsIgnoreCase("update")) {
//			Object employeeResignationId = record.getKey();
			int newresignationstatus = (Integer) record.getValue("resignationstatusid");
			String approverComment = (String) record.getValue("comment");
			try {
				String employeeName = null;
				String employeeEmailid = null;
				int oldresignationStatus = 0;
				Object relievingDateObject = null;
				String approverName = null;
				String approverEmailID = null;
				String resignationReason = null;
				String employeeMobileNo = null;
				String approverMobileNo = null;
				String relievingDate = null;
				String resignationCode = null;

				List<String> employeeEmailIdList = new ArrayList<String>();

				employeeName = (String) record.getOldValue("employeeid.name");
				employeeMobileNo = (String) record.getOldValue("employeeid.mobileno");
				employeeEmailid = (String) record.getOldValue("employeeid.officialemailid");
				approverName = (String) record.getOldValue("approverid.name");
				approverMobileNo = (String) record.getOldValue("approverid.mobileno");
				approverEmailID = (String) record.getOldValue("approverid.officialemailid");

				if (approverEmailID != null) {
					employeeEmailIdList.add((String) approverEmailID);
				}
				oldresignationStatus = (Integer) record.getOldValue("resignationstatusid");
				relievingDateObject = record.getOldValue("proposedrelievingdate");
				resignationReason = (String) record.getOldValue("reason");
				resignationCode = (String) (record.getOldValue("resignationcode") == null ? "" : record.getOldValue("resignationcode"));

				Date employeeRelievingDate = null;

				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

				if (relievingDateObject != null) {
					try {
						employeeRelievingDate = updateDateFormat.parse("" + relievingDateObject);
					} catch (ParseException e) {
						throw new RuntimeException("Error Come while parsing Dates" + e.getMessage());
					}
				}
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
				relievingDate = dateFormat.format(employeeRelievingDate);
				if ((oldresignationStatus == HRISConstants.RESIGNATION_NEW || oldresignationStatus == HRISConstants.RESIGNATION_ONHOLD || oldresignationStatus == HRISConstants.RESIGNATION_REJECTED) && newresignationstatus == HRISConstants.RESIGNATION_ACCEPTED) {
					approveResignationMail(employeeEmailIdList, employeeName, employeeEmailid, approverName, approverEmailID, approverComment, relievingDate, resignationReason, resignationCode);
					approveResignationSMS(employeeName, approverName, employeeMobileNo, approverMobileNo, relievingDate);
				}

			} catch (Exception e) {
				throw new BusinessLogicException("Error come while send Email or SMS. " + e.getMessage());
			}
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void approveResignationSMS(String employeeName, String approverName, String employeeMobileNo, String approverMobileNo, String relievingDate) {
		String message = "Resignation of " + employeeName + " has been accepted by " + approverName + " and to be relieved on " + relievingDate + ".";
		// String otherMessage = "Leave request of " + employeeName + " from " + fromDate + " to " + toDate + " has been approved by " + otherApproverName + ".";
		List<Object> smsList = new ArrayList<Object>();
		List<String> employeeMobileNoList = new ArrayList<String>();
		employeeMobileNoList.add((String) employeeMobileNo);
		try {
//			String SenderName = null;
			boolean disableSMS;
			String replyToMobileNo = null;

			disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);
			replyToMobileNo = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_MOBILE);
//			SenderName = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);

			if (replyToMobileNo != null && !employeeMobileNoList.contains((String) replyToMobileNo)) {
				employeeMobileNoList.add((String) replyToMobileNo);
			}
			if (!disableSMS) {
				int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();
				for (int counter = 0; counter < smsToCount; counter++) {
					JSONObject smsTo = new JSONObject();
					smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
					smsTo.put(View.Action.Types.SendSMS.FROM, approverMobileNo);
					smsTo.put(View.Action.Types.SendSMS.SMS, message);
					smsList.add(smsTo);
				}
				try {
					SendSMSService.sendSMSUtility(smsList.toArray());
				} catch (IOException e) {
					throw new BusinessLogicException("Error while send SMS " + e.getMessage());
				}
			}
		} catch (JSONException e) {
			throw new BusinessLogicException("Error come while send SMS " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void approveResignationMail(List<String> employeeEmailIdList, String employeeName, String employeeEmailid, String approverName, String approverEmailID, String approverComment, String relievingDate, String resignationReason, String resignationcode) {

		ApplaneMail mailSender = new ApplaneMail();
		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String replyToId = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);
			if (!disableEmail) {
				if (replyToId != null && !employeeEmailIdList.contains((String) replyToId)) {
					employeeEmailIdList.add((String) replyToId);
				}
				if (organizationLogo == null || signature == null || replyToId == null) {
					WarningCollector.collect("Kindly configure your Communication configuration from SETUP.");
				} else {
					String title = "Resignation Accept (" + resignationcode + ")";
					StringBuffer messageContents = new StringBuffer();
					messageContents.append("<img src=\"" + organizationLogo + " \"/>");
					messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
					messageContents.append("Hello " + employeeName + ",<BR><BR>");
					messageContents.append("Your Resignation has been accepted. It has the following details: <br>");
					messageContents.append("<ul><li><B>Proposed Relieving Date : </B>" + relievingDate + "</li>");
					messageContents.append("<li><B>Reason : </B>" + resignationReason + "</li>");
					messageContents.append("<li><B>Approved By: </B>" + approverName + "</li>");
					messageContents.append("<li><B>Approver Comments : </B>" + approverComment + "</li></ul>");
					messageContents.append("Regards, <BR>");
					messageContents.append(approverName + "<BR>");
					messageContents.append("(" + organizationName + ")");

					int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
					if (mailToCount > 0) {
						try {
							mailSender.setMessage(title, messageContents.toString(), true);
							mailSender.setTo(employeeEmailid);
							String[] ccEmailIds = (String[]) employeeEmailIdList.toArray(new String[employeeEmailIdList.size()]);
							mailSender.setCc(ccEmailIds);
							mailSender.setReplyTo(approverEmailID);
							mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
						} catch (ApplaneMailException e) {
							throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while send Email. " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void sendResignationMail(List<String> employeeEmailIdList, String employeeName, String employeeEmailID, String approverName, String approverEmailID, String relievingDate, String resignationReason, String resignationcode) {
		ApplaneMail mailSender = new ApplaneMail();

		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
//			String replyToId = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);
			if (!disableEmail) {
				if (organizationLogo == null || signature == null) {
					WarningCollector.collect("Kindly configure your Communication from SETUP.");
				} else {
					String title = "Apply For Resignation (" + resignationcode + ")";
					StringBuffer messageContents = new StringBuffer();
					messageContents.append("<img src=\"" + organizationLogo + " \"/>");
					messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
					messageContents.append("Dear Sir/Mam,<BR><BR>");
					messageContents.append(employeeName + " has apply for resignation : <br>");
					messageContents.append("<ul><li><B>Proposed Relieving Date : </B>" + relievingDate + "</li>");
					messageContents.append("<li><B>Reason : </B>" + resignationReason + "</li></ul>");
					messageContents.append("Regards, <BR>");
					messageContents.append(employeeName + "<BR>");
					messageContents.append("(" + organizationName + ")");

					int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
					for (int counter = 0; counter < mailToCount; counter++) {
						try {
							mailSender.setMessage(title, messageContents.toString(), true);
							mailSender.setTo(employeeEmailIdList.get(counter));
							mailSender.setReplyTo(employeeEmailID);
							mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
						} catch (ApplaneMailException e) {
							throw new BusinessLogicException("Some unknown error occured while send Email. " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e1) {
			throw new BusinessLogicException("Error occured while send mail. " + e1.getMessage());
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void sendResignationSMS(String employeeMobileNo, String approverMobileNo, String employeeName, String relievingDate) {
		String message = employeeName + " has applied for resignation and to be relieved on " + relievingDate;
		List<Object> smsList = new ArrayList<Object>();

		try {
//			String SenderName = null;
			boolean disableSMS;
//			String replyToMobileNo = null;
			disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);
//			replyToMobileNo = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_MOBILE);
//			SenderName = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.RESIGNATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			if (!disableSMS) {
				JSONObject smsTo = new JSONObject();
				smsTo.put(View.Action.Types.SendSMS.TO, approverMobileNo);
				smsTo.put(View.Action.Types.SendSMS.FROM, employeeMobileNo);
				smsTo.put(View.Action.Types.SendSMS.SMS, message);
				smsList.add(smsTo);
				try {
					SendSMSService.sendSMSUtility(smsList.toArray());
				} catch (IOException e) {
					throw new BusinessLogicException("Error while send SMS " + e.getMessage());
				}
			}
		} catch (JSONException e) {
			throw new BusinessLogicException("Error come while send SMS " + e.getMessage());
		}
	}

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

	}
}