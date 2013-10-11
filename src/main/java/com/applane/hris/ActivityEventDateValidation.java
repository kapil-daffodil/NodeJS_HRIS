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

public class ActivityEventDateValidation implements OperationJob {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		activityEventDateValidation(record);
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		sendActivityEventMail(record);

	}

	private void activityEventDateValidation(Record record) {
		Object startDateObject = record.getValue("startdate");
		Object endDateObject = record.getValue("enddate");
		String operation = record.getOperationType();
		java.util.Date startDate = null;
		java.util.Date endDate = null;

		if (operation.equalsIgnoreCase("insert")) {
			if (startDateObject != null && endDateObject != null) {

				try {
					startDate = getDate(startDateObject.toString());
					endDate = getDate(endDateObject.toString());
				} catch (Exception e) {
					e.printStackTrace();
					throw new BusinessLogicException(e.getMessage());
				}
				if (endDate.before(startDate)) {
					throw new BusinessLogicException("End Date must be greater than or equal to Start Date.");
				}
			}
		} else if (operation.equalsIgnoreCase("update")) {
			if (startDateObject != null && endDateObject != null) {
				try {
					startDate = getDate(startDateObject.toString());
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException("Start date is not parsable" + e.getMessage());
				}
				try {
					endDate = getDate(endDateObject.toString());
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException("End date is not parsable" + e.getMessage());
				}
				if (endDate.before(startDate)) {
					throw new BusinessLogicException("End Date must be greater than or equal to Start Date.");
				}
			}
		}
	}

	private void sendActivityEventMail(Record record) {

		if (record.getOperationType().equalsIgnoreCase("Insert")) {

			// Object eventId = record.getValue("__key__");
			String titleObject = (String) record.getValue("title");
			String eventPlaceObject = (String) record.getValue("eventplace");
			String eventDescriptionObject = (String) record.getValue("description");
			Object employeeIdObject = record.getValue("employeeid");
			Object groupObject = record.getValue("employeegroupid");
			Object fromDateObject = record.getValue("startdate");
			Object toDateObject = record.getValue("enddate");
			Object sendSMSObject = record.getValue("sendsms");
			Object sendEmailObject = record.getValue("sendemail");
			Date eventStartDate = null;
			Date eventEndDate = null;
			SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

			if (fromDateObject != null && toDateObject != null) {
				try {
					eventStartDate = updateDateFormat.parse("" + fromDateObject);
					eventEndDate = updateDateFormat.parse("" + toDateObject);
				} catch (ParseException e) {
					e.printStackTrace();
					throw new RuntimeException("Error Come while parsing " + e.getMessage());
				}
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
			String startDate = dateFormat.format(eventStartDate);
			String endDate = dateFormat.format(eventEndDate);

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
					Object employeeId = ((JSONObject) employeeArray.get(employeeCounter)).get("__key__");
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
					// LogUtility.writeLog("SEND MAIL NOT EQUAL TO NULL");
					sendMail(employeeEmailIdList, titleObject, eventPlaceObject, eventDescriptionObject, startDate, endDate);
				}
				// Send SMS
				if (sendSMS) {
					sendSMS(employeeMobileNoList, titleObject, eventPlaceObject, eventDescriptionObject, startDate, endDate);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				throw new BusinessLogicException("Error come while send SMS or Email. " + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				throw new BusinessLogicException("Error come while send SMS or Email. " + e.getMessage());

			}
		}
	}

	@SuppressWarnings("unchecked")
	private void sendMail(List<String> employeeEmailIdList, String title, String eventplace, String description, String startDate, String endDate) {

		ApplaneMail mailSender = new ApplaneMail();
		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.EVENT_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String replyToId = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.EVENT_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);

			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);
			if (!disableEmail) {
				if (organizationLogo == null || signature == null || replyToId == null) {
					WarningCollector.collect("Kindly configure your mail configuration from SETUP.");
				} else {
					StringBuffer messageContents = new StringBuffer();
					messageContents.append("<img src=\"" + organizationLogo + " \"/>");
					messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
					messageContents.append("Hello,<BR><BR>");
					messageContents.append("Please find a circular below : <br><br>");
					messageContents.append("<B>Description: </B>" + description + "<br><br>");
					messageContents.append("<B>Start Date: </B>" + startDate + "<b>&nbsp;&nbsp;&nbsp;End Date: </b>" + endDate + "<br><br>");
					messageContents.append("<B>Place: </B>" + eventplace + "<br><br>");
					messageContents.append("Regards, <BR>");
					messageContents.append(signature + "<BR>");
					messageContents.append("(" + organizationName + ")");

					int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
					for (int counter = 0; counter < mailToCount; counter++) {
						try {
							mailSender.setMessage(title, messageContents.toString(), true);
							// LogUtility.writeLog("EMAIL ID IS : >>" + employeeEmailIdList.get(counter));
							mailSender.setTo(employeeEmailIdList.get(counter));
							mailSender.setReplyTo(replyToId);
							mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
						} catch (ApplaneMailException e) {
							throw new BusinessLogicException("Error come while send Email." + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unknown error occured while send Email " + e.getMessage());
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void sendSMS(List<String> employeeMobileNoList, String title, String eventPlace, String eventDescription, String startDate, String endDate) {
		try {
			String SenderName = null;
			boolean disableSMS;
			String replyToMobileNo = null;
			List<Object> smsList = new ArrayList<Object>();

			disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.EVENT_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);
			replyToMobileNo = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.EVENT_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_MOBILE);
			SenderName = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.EVENT_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			if (!disableSMS) {
				String message = "Activity: " + title;
				int messageSize = (message == null ? 0 : (message.trim().length() <= 65 ? message.trim().length() : 65));
				message = message.substring(0, messageSize);
				message = message + ", Place: " + eventPlace + ", Start: " + startDate + ", End: " + endDate;
				int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();
				for (int counter = 0; counter < smsToCount; counter++) {
					JSONObject smsTo = new JSONObject();
					smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
					smsTo.put(View.Action.Types.SendSMS.FROM, replyToMobileNo);
					smsTo.put(View.Action.Types.SendSMS.SMS, message + " - From: " + SenderName);
					smsList.add(smsTo);
				}
				try {
					SendSMSService.sendSMSUtility(smsList.toArray());
				} catch (IOException e) {
					e.printStackTrace();
					throw new BusinessLogicException("Error while send SMS." + e.getMessage());
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			throw new BusinessLogicException("Some unknown error occured while send SMS." + e.getMessage());
		}
	}

	public Date getDate(String date) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
		} catch (ParseException e) {
			try {
				return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(date);
			} catch (ParseException e1) {
				try {
					return new SimpleDateFormat("dd/MM/yyyy").parse(date);
				} catch (ParseException e2) {
					try {
						return new SimpleDateFormat("yyyy-MM-dd").parse(date);
					} catch (ParseException e3) {
						e3.printStackTrace();
						throw new BusinessLogicException("" + e3.getMessage());
					}
				}
			}
		}
	}

}
