package com.applane.hris;

import java.io.IOException;
import java.util.ArrayList;
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

public class SendNotificationMail implements OperationJob {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	// job define on hris_sendnotifications
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		if (record.getOperationType().equalsIgnoreCase("Insert")) {
			// Object sendNotificationId = record.getValue("__key__");
			String subjectObject = (String) record.getValue("subject");
			String messageObject = (String) record.getValue("message");
			Object employeeIdObject = record.getValue("employeeid");
			Object groupObject = record.getValue("employeegroupid");

			Object sendSMSObject = record.getValue("sendsms");
			Object sendEmailObject = record.getValue("sendemail");

			boolean sendSMS;
			boolean sendEmail;

			sendSMS = CommunicationUtility.booleanValue(sendSMSObject);
			sendEmail = CommunicationUtility.booleanValue(sendEmailObject);
			try {
				JSONArray employeeArray = new JSONArray();
				if (employeeIdObject != null && employeeIdObject instanceof JSONArray) {
					employeeArray = (JSONArray) employeeIdObject;
				} else if (employeeIdObject != null) {
					employeeArray.put(employeeIdObject);
				}
				int employeeCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
				List<String> employeeEmailIdList = new ArrayList<String>();
				List<String> employeeMobileNoList = new ArrayList<String>();

				for (int employeeCounter = 0; employeeCounter < employeeCount; employeeCounter++) {
					// get officialemailid from JSON
					Object employeeId = ((JSONObject) employeeArray.get(employeeCounter)).opt("__key__");

					JSONObject employeeQuery = new JSONObject();
					employeeQuery.put(Data.Query.RESOURCE, "hris_employees");
					JSONArray employeeColumnArray = new JSONArray();
					employeeColumnArray.put("officialemailid");
					employeeColumnArray.put("mobileno");
					employeeQuery.put(Data.Query.COLUMNS, employeeColumnArray);
					employeeQuery.put(Data.Query.FILTERS, "__key__= " + employeeId);
					JSONObject employeeObject;
					employeeObject = ResourceEngine.query(employeeQuery);
					JSONArray employeeIdArray = employeeObject.getJSONArray("hris_employees");

					String employeeEmailId = employeeIdArray.getJSONObject(0).optString("officialemailid");
					String employeeMobileNo = employeeIdArray.getJSONObject(0).optString("mobileno");
					if (employeeEmailId != null && !employeeEmailIdList.contains((String) employeeEmailId)) {
						employeeEmailIdList.add((String) employeeEmailId);
					}
					if (employeeMobileNo != null && !employeeMobileNoList.contains((String) employeeMobileNo)) {
						employeeMobileNoList.add((String) employeeMobileNo);
					}
				}
				JSONArray groupArray = new JSONArray();
				if (groupObject != null && groupObject instanceof JSONArray) {
					groupArray = (JSONArray) groupObject;
				} else if (groupObject != null) {
					groupArray.put(groupObject);
				}
				int groupCount = (groupArray == null || groupArray.length() == 0) ? 0 : groupArray.length();
				for (int groupCounter = 0; groupCounter < groupCount; groupCounter++) {

					Object employeeGroupId = null;
					if (groupArray.get(groupCounter) instanceof JSONObject) {
						employeeGroupId = ((JSONObject) groupArray.get(groupCounter)).get("__key__");
					} else {
						employeeGroupId = groupArray.get(groupCounter);
					}
					JSONObject groupQuery = new JSONObject();
					groupQuery.put(Data.Query.RESOURCE, "hris_employees__employeegroupid");
					JSONArray groupColumnArray = new JSONArray();
					groupColumnArray.put("hris_employees.officialemailid");
					groupColumnArray.put("hris_employees.mobileno");
					groupQuery.put(Data.Query.COLUMNS, groupColumnArray);
					groupQuery.put(Data.Query.FILTERS, "employeegroupid= " + employeeGroupId);
					JSONObject employeeGroupObject;
					employeeGroupObject = ResourceEngine.query(groupQuery);
					JSONArray employeeEmailArray = employeeGroupObject.getJSONArray("hris_employees__employeegroupid");

					int emailCount = (employeeEmailArray == null || employeeEmailArray.length() == 0) ? 0 : employeeEmailArray.length();
					for (int emailCounter = 0; emailCounter < emailCount; emailCounter++) {
						String groupEmployeeEmailId = employeeEmailArray.getJSONObject(emailCounter).optString("hris_employees.officialemailid");
						String groupEmployeeMobileNo = employeeEmailArray.getJSONObject(emailCounter).optString("hris_employees.mobileno");

						if (groupEmployeeEmailId != null && !employeeEmailIdList.contains((String) groupEmployeeEmailId)) {
							employeeEmailIdList.add((String) groupEmployeeEmailId);
						}

						if (groupEmployeeMobileNo != null && !employeeMobileNoList.contains((String) groupEmployeeMobileNo)) {
							employeeMobileNoList.add((String) groupEmployeeMobileNo);
						}
					}
				}
				// Send Mail
				if (sendEmail) {
					sendMail(employeeEmailIdList, subjectObject, messageObject);
				}
				// Send SMS
				if (sendSMS) {
					sendSMS(employeeMobileNoList, messageObject);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				throw new BusinessLogicException("Error Come while Send SMS Or Email...1" + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Error Come while Send SMS Or Email...2" + e.getMessage());
			}
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void sendSMS(List<String> employeeMobileNoList, String message) {
		// String CURRENT_USER = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		// String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getSystemParameters().getCurrentDateTime());

		try {
			String SenderName = null;
			boolean disableSMS;

			String replyToMobileNo = null;
			List<Object> smsList = new ArrayList<Object>();

			disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.NOTIFICATION_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);
			replyToMobileNo = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.NOTIFICATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_MOBILE);
			SenderName = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.NOTIFICATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			if (!disableSMS) {
				int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();
				int messageSize = (message == null ? 0 : (message.trim().length() <= 125 ? message.trim().length() : 125));
				message = message.substring(0, messageSize);
				for (int counter = 0; counter < smsToCount; counter++) {
					JSONObject smsTo = new JSONObject();
					smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
					smsTo.put(View.Action.Types.SendSMS.FROM, replyToMobileNo);
					smsTo.put(View.Action.Types.SendSMS.SMS, message + ".. - From: " + SenderName);
					smsList.add(smsTo);
				}
				try {
					SendSMSService.sendSMSUtility(smsList.toArray());
				} catch (IOException e) {
					e.printStackTrace();
					throw new BusinessLogicException("Error while send SMS " + e.getMessage());
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unknown error occured while send mail " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void sendMail(List<String> employeeEmailIdList, String subject, String message) {
		// String CURRENT_USER = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		// String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getSystemParameters().getCurrentDateTime());
		ApplaneMail mailSender = new ApplaneMail();
		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.NOTIFICATION_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String replyToId = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.NOTIFICATION_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);

			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);

			if (!disableEmail) {
				if (organizationLogo == null || signature == null || organizationName == null) {
					WarningCollector.collect("Kindly configure your Communications from SETUP.");
				} else {
					StringBuffer messageContents = new StringBuffer();
					messageContents.append("<img src=\"" + organizationLogo + " \"/>");
					messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
					messageContents.append("Hello,<BR><BR>");
					messageContents.append("Please find a notification below : <br>");
					messageContents.append("<B>Title : </B>" + message + "<br><br>");
					messageContents.append("Regards,<BR>");
					messageContents.append(signature + "<BR>");
					messageContents.append("(" + organizationName + ")");

					int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
					for (int counter = 0; counter < mailToCount; counter++) {
						try {
							mailSender.setMessage(subject, messageContents.toString(), true);
							mailSender.setTo(employeeEmailIdList.get(counter));
							mailSender.setReplyTo(replyToId);
							mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
						} catch (ApplaneMailException e) {
							throw new RuntimeException("Some unknown error occured " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Some unknown error occured while send mail " + e.getMessage());
		}
	}

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

	}

}
