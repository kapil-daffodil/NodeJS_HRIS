package com.applane.resourceoriented.hris.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class CommunicationUtility {

	public static Map<String, Object> getCommunicationInfo() {
		ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
		Map<String, Object> configMap = new HashMap<String, Object>();

		// fire query on communication do and retrieve info
		String leaveReplyToEmailId = null;
		String notificationReplyToEmailId = null;
		String eventReplyToEmailId = null;
		String tourReplyToEmailId = null;
		String resignationReplyToEmailId = null;

		String signature = null;
		String organizationLogo = null;
		String organizationName = null;

		Object disableleaveEmailObject = null;
		boolean disableleaveEmail;
		Object disableleaveSMSObject = null;
		boolean disableleaveSMS;
		String leaveReplyToMobileNo = null;
		String leaveConcernedPerson = null;

		Object disableNotificationEmailObject = null;
		boolean disableNotificationEmail;
		Object disableNotificationSMSObject = null;
		boolean disableNotificationSMS;
		String NotificationReplyToMobileNo = null;

		Object disableEventEmailObject = null;
		boolean disableEventEmail;
		Object disableEventSMSObject = null;
		boolean disableEventSMS;
		String eventReplyToMobileNo = null;

		Object disableTourEmailObject = null;
		boolean disableTourEmail;
		Object disableTourSMSObject = null;
		boolean disableTourSMS;
		String tourReplyToMobileNo = null;
		String tourConcernedPerson = null;

		Object disableResignationEmailObject = null;
		boolean disableResignationEmail;
		Object disableResignationSMSObject = null;
		boolean disableResignationSMS;
		String resignationReplyToMobileNo = null;

		try {
			JSONObject mailconfigQuery = new JSONObject();
			mailconfigQuery.put(Data.Query.RESOURCE, "hris_mailconfigurations");
			JSONArray mailConfigColumnArray = new JSONArray();
			mailConfigColumnArray.put("organizationname");
			mailConfigColumnArray.put("organizationlogo");
			mailConfigColumnArray.put("signature");

			mailConfigColumnArray.put("disableleavemail");
			mailConfigColumnArray.put("disableleavesms");
			mailConfigColumnArray.put("leavereplytoid.officialemailid");
			mailConfigColumnArray.put("leavereplytonumber");
			mailConfigColumnArray.put("leaveconcernedpersons");

			mailConfigColumnArray.put("disablenotificationmail");
			mailConfigColumnArray.put("disablenotificationsms");
			mailConfigColumnArray.put("notificationreplytoid.officialemailid");
			mailConfigColumnArray.put("notificationreplytonumber");

			mailConfigColumnArray.put("disabletourmail");
			mailConfigColumnArray.put("disabletoursms");
			mailConfigColumnArray.put("tourreplytoid.officialemailid");
			mailConfigColumnArray.put("tourreplytonumber");
			mailConfigColumnArray.put("tourconcernedpersons");

			mailConfigColumnArray.put("disableresignationmail");
			mailConfigColumnArray.put("disableresignationsms");
			mailConfigColumnArray.put("resignationreplytoid.officialemailid");
			mailConfigColumnArray.put("resignationreplytonumber");

			mailConfigColumnArray.put("disableeventmail");
			mailConfigColumnArray.put("disableeventsms");
			mailConfigColumnArray.put("eventreplytoid.officialemailid");
			mailConfigColumnArray.put("eventreplytonumber");
			
			mailConfigColumnArray.put("cmd_id");
			mailConfigColumnArray.put("cmd_id.officialemailid");

			mailconfigQuery.put(Data.Query.COLUMNS, mailConfigColumnArray);
			JSONObject mailConfigurationObject;
			mailConfigurationObject = ResourceEngine.query(mailconfigQuery);
			JSONArray mailConfigurationArray = mailConfigurationObject.getJSONArray("hris_mailconfigurations");
			int mailConfigCount = (mailConfigurationArray == null || mailConfigurationArray.length() == 0) ? 0 : mailConfigurationArray.length();

			if (mailConfigCount > 0) {
				String organizationLogoObject = (String) mailConfigurationArray.getJSONObject(0).opt("organizationlogo");
				if (organizationLogoObject != null) {
					JSONArray logoJsonArray = Translator.convertOrgLogo(organizationLogoObject);
					organizationLogo = (String) logoJsonArray.getJSONObject(0).opt("url");
				} else {
					LogUtility.writeLog("organizationlogoObject found null");
				}

				int logoCount = (mailConfigurationArray == null || mailConfigurationArray.length() == 0) ? 0 : mailConfigurationArray.length();
				if (logoCount > 0) {
					organizationName = (String) mailConfigurationArray.getJSONObject(0).opt("organizationname");
					signature = (String) mailConfigurationArray.getJSONObject(0).opt("signature");

					leaveReplyToEmailId = (String) mailConfigurationArray.getJSONObject(0).opt("leavereplytoid.officialemailid");
					disableleaveEmailObject = mailConfigurationArray.getJSONObject(0).opt("disableleavemail");
					disableleaveSMSObject = mailConfigurationArray.getJSONObject(0).opt("disableleavesms");
					leaveReplyToMobileNo = (String) mailConfigurationArray.getJSONObject(0).opt("leavereplytonumber");
					leaveConcernedPerson = (String) (mailConfigurationArray.getJSONObject(0).opt("leaveconcernedpersons") == null ? "" : mailConfigurationArray.getJSONObject(0).opt("leaveconcernedpersons"));

					notificationReplyToEmailId = (String) mailConfigurationArray.getJSONObject(0).opt("notificationreplytoid.officialemailid");
					disableNotificationEmailObject = mailConfigurationArray.getJSONObject(0).opt("disablenotificationmail");
					disableNotificationSMSObject = mailConfigurationArray.getJSONObject(0).opt("disablenotificationsms");
					NotificationReplyToMobileNo = (String) mailConfigurationArray.getJSONObject(0).opt("notificationreplytonumber");

					eventReplyToEmailId = (String) mailConfigurationArray.getJSONObject(0).opt("eventreplytoid.officialemailid");
					disableEventEmailObject = mailConfigurationArray.getJSONObject(0).opt("disableeventmail");
					disableEventSMSObject = mailConfigurationArray.getJSONObject(0).opt("disableeventsms");
					eventReplyToMobileNo = (String) mailConfigurationArray.getJSONObject(0).opt("eventreplytonumber");

					tourReplyToEmailId = (String) mailConfigurationArray.getJSONObject(0).opt("tourreplytoid.officialemailid");
					disableTourEmailObject = mailConfigurationArray.getJSONObject(0).opt("disabletourmail");
					disableTourSMSObject = mailConfigurationArray.getJSONObject(0).opt("disabletoursms");
					tourReplyToMobileNo = (String) mailConfigurationArray.getJSONObject(0).opt("tourreplytonumber");
					tourConcernedPerson = (String) (mailConfigurationArray.getJSONObject(0).opt("tourconcernedpersons") == null ? "" : mailConfigurationArray.getJSONObject(0).opt("tourconcernedpersons"));

					resignationReplyToEmailId = (String) mailConfigurationArray.getJSONObject(0).opt("resignationreplytoid.officialemailid");
					disableResignationEmailObject = mailConfigurationArray.getJSONObject(0).opt("disableresignationmail");
					disableResignationSMSObject = mailConfigurationArray.getJSONObject(0).opt("disableresignationsms");
					resignationReplyToMobileNo = (String) mailConfigurationArray.getJSONObject(0).opt("resignationreplytonumber");
					Object cmdId = mailConfigurationArray.getJSONObject(0).opt("cmd_id");
					Object cmdMailId = mailConfigurationArray.getJSONObject(0).opt("cmd_id.officialemailid");
					configMap.put("cmdId", cmdId);
					configMap.put("cmdMailId", cmdMailId);
				}
			}
			disableleaveEmail = booleanValue(disableleaveEmailObject);
			disableleaveSMS = booleanValue(disableleaveSMSObject);
			disableEventEmail = booleanValue(disableEventEmailObject);
			disableEventSMS = booleanValue(disableEventSMSObject);
			disableNotificationEmail = booleanValue(disableNotificationEmailObject);
			disableNotificationSMS = booleanValue(disableNotificationSMSObject);
			disableResignationEmail = booleanValue(disableResignationEmailObject);
			disableResignationSMS = booleanValue(disableResignationSMSObject);
			disableTourEmail = booleanValue(disableTourEmailObject);
			disableTourSMS = booleanValue(disableTourSMSObject);

		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error come while check disable sms aur disable email  " + e.getMessage());
		}
		// end

		// populate Event configuration
		Map<String, Object> eventConfigMap = new HashMap<String, Object>();
		// @TODO populate eventConfigMap

		eventConfigMap.put(Communication_Constants.Properties.DISABLE_MAIL, disableEventEmail);
		eventConfigMap.put(Communication_Constants.Properties.DISABLE_SMS, disableEventSMS);
		eventConfigMap.put(Communication_Constants.Properties.REPLY_TO_ID, eventReplyToEmailId);
		eventConfigMap.put(Communication_Constants.Properties.REPLY_TO_MOBILE, eventReplyToMobileNo);
		configMap.put(Communication_Constants.Type.EVENT_CONFIG, eventConfigMap);

		// populate Leave configuration
		Map<String, Object> leaveConfigMap = new HashMap<String, Object>();
		// @TODO populate leaveConfigMap

		String[] leaveConcernedPersonArray = leaveConcernedPerson == null ? null : leaveConcernedPerson.trim().split(",");
		if (leaveConcernedPersonArray != null && leaveConcernedPersonArray.length > 0) {
			leaveConfigMap.put(Communication_Constants.Properties.CONCERNED_PERSONS, leaveConcernedPersonArray);
		}
		leaveConfigMap.put(Communication_Constants.Properties.DISABLE_MAIL, disableleaveEmail);
		leaveConfigMap.put(Communication_Constants.Properties.DISABLE_SMS, disableleaveSMS);
		leaveConfigMap.put(Communication_Constants.Properties.REPLY_TO_ID, leaveReplyToEmailId);
		leaveConfigMap.put(Communication_Constants.Properties.REPLY_TO_MOBILE, leaveReplyToMobileNo);
		configMap.put(Communication_Constants.Type.LEAVE_CONFIG, leaveConfigMap);

		// populate Notification configuration
		Map<String, Object> notificationConfigMap = new HashMap<String, Object>();
		// @TODO populate notificationConfigMap
		notificationConfigMap.put(Communication_Constants.Properties.DISABLE_MAIL, disableNotificationEmail);
		notificationConfigMap.put(Communication_Constants.Properties.DISABLE_SMS, disableNotificationSMS);
		notificationConfigMap.put(Communication_Constants.Properties.REPLY_TO_ID, notificationReplyToEmailId);
		notificationConfigMap.put(Communication_Constants.Properties.REPLY_TO_MOBILE, NotificationReplyToMobileNo);
		configMap.put(Communication_Constants.Type.NOTIFICATION_CONFIG, notificationConfigMap);

		// populate Resignation configuration
		Map<String, Object> resignationConfigMap = new HashMap<String, Object>();
		// @TODO populate resignationConfigMap
		resignationConfigMap.put(Communication_Constants.Properties.DISABLE_MAIL, disableResignationEmail);
		resignationConfigMap.put(Communication_Constants.Properties.DISABLE_SMS, disableResignationSMS);
		resignationConfigMap.put(Communication_Constants.Properties.REPLY_TO_ID, resignationReplyToEmailId);
		resignationConfigMap.put(Communication_Constants.Properties.REPLY_TO_MOBILE, resignationReplyToMobileNo);
		configMap.put(Communication_Constants.Type.RESIGNATION_CONFIG, resignationConfigMap);

		// populate Tour configuration
		Map<String, Object> tourConfigMap = new HashMap<String, Object>();
		// @TODO populate tourConfigMap
		String[] tourConcernedPersonArray = tourConcernedPerson == null ? null : tourConcernedPerson.trim().split(",");
		if (tourConcernedPersonArray != null && tourConcernedPersonArray.length > 0) {
			tourConfigMap.put(Communication_Constants.Properties.CONCERNED_PERSONS, tourConcernedPersonArray);
		}
		tourConfigMap.put(Communication_Constants.Properties.DISABLE_MAIL, disableTourEmail);
		tourConfigMap.put(Communication_Constants.Properties.DISABLE_SMS, disableTourSMS);
		tourConfigMap.put(Communication_Constants.Properties.REPLY_TO_ID, tourReplyToEmailId);
		tourConfigMap.put(Communication_Constants.Properties.REPLY_TO_MOBILE, tourReplyToMobileNo);
		configMap.put(Communication_Constants.Type.TOUR_CONFIG, tourConfigMap);

		// Add Logo, Org name and signature
		configMap.put(Communication_Constants.ORGANIZATION_NAME, organizationName);
		configMap.put(Communication_Constants.ORGANIZATION_LOGO, organizationLogo);
		configMap.put(Communication_Constants.ORGANIZATION_SIGNATURE, signature);

		// return info map
		return configMap;
	}

	public static boolean booleanValue(Object value) {
		if (value == null) {
			return false;
		}
		boolean isboolean = false;
		if (value instanceof Integer) {
			int iday = ((Integer) value).intValue();
			if (iday == 1) {
				isboolean = true;
			}
		} else if (value instanceof Short) {
			int iday = ((Short) value).shortValue();
			if (iday == 1) {
				isboolean = true;
			}
		} else if (value instanceof Boolean) {
			isboolean = (Boolean) value;
		}

		return isboolean;

	}

	public static long differenceBetweenDates(Date fromDate, Date toDate) {
		// Creates two calendars instances
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();

		// Set the date for both of the calendar instance
		cal1.setTime(toDate);
		cal2.setTime(fromDate);

		// Get the represented date in milliseconds
		long milis1 = cal1.getTimeInMillis();
		long milis2 = cal2.getTimeInMillis();
		// Calculate difference in milliseconds
		long diff = milis1 - milis2;

		// Calculate difference in days
		long diffDays = diff / (24 * 60 * 60 * 1000);

		return diffDays;
	}

	public static JSONArray getHRManagerRecord(Object branchId) {
		try {
			JSONObject hrQuery = new JSONObject();
			hrQuery.put(Data.Query.RESOURCE, "hris_hrassigning");
			JSONArray hrArray = new JSONArray();
			hrArray.put("employeeid");
			hrArray.put("employeeid.officialemailid");
			hrArray.put("cashier_id");
			hrArray.put("cashier_id.officialemailid");
			hrArray.put("cashier_id.name");
			hrArray.put("mobileno");
			hrQuery.put(Data.Query.COLUMNS, hrArray);
			hrQuery.put(Data.Query.FILTERS, "branchid = " + branchId);
			JSONObject hrObject;
			ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			hrObject = ResourceEngine.query(hrQuery);
			JSONArray hrAssigningArray = hrObject.getJSONArray("hris_hrassigning");
			return hrAssigningArray;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Error come while generate HR Manager Records." + e.getMessage());
		}
	}

	public static Date checkDateFormat(Object dateObject) {
		Date date = null;
		SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			date = queryDateFormat.parse("" + dateObject);
		} catch (ParseException e) {
			e.printStackTrace();
			try {
				date = updateDateFormat.parse("" + dateObject);
			} catch (ParseException e1) {
				e1.printStackTrace();
				throw new BusinessLogicException("date is not parsable" + e.getMessage());
			}
		}
		return date;
	}

	public static boolean isLeaveReminderRequired() {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
			JSONArray array = new JSONArray();
			array.put("leavereminder");
			array.put("disableleavemail");
			query.put(Data.Query.COLUMNS, array);
			JSONObject object;
			ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			object = ResourceEngine.query(query);
			JSONArray communicationArray = object.getJSONArray("hris_mailconfigurations");
			int communicationArrayCount = (communicationArray == null || communicationArray.length() == 0) ? 0 : communicationArray.length();
			if (communicationArrayCount > 0) {
				Object leaveReminderObject = communicationArray.getJSONObject(0).opt("leavereminder");
				Object disableLeaveMailObject = communicationArray.getJSONObject(0).opt("disableleavemail");
				boolean leaveReminder = booleanValue(leaveReminderObject);
				boolean disableLeaveMail = booleanValue(disableLeaveMailObject);
				if (!disableLeaveMail && !leaveReminder) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Exception come while check leave reminder required." + e.getMessage());
		}
	}

	public static boolean isTourReminderRequired() {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
			JSONArray array = new JSONArray();
			array.put("tourreminder");
			array.put("disabletourmail");
			query.put(Data.Query.COLUMNS, array);
			JSONObject object;
			ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			object = ResourceEngine.query(query);
			JSONArray communicationArray = object.getJSONArray("hris_mailconfigurations");
			int communicationArrayCount = (communicationArray == null || communicationArray.length() == 0) ? 0 : communicationArray.length();
			if (communicationArrayCount > 0) {
				Object tourReminderObject = communicationArray.getJSONObject(0).opt("tourreminder");
				Object disableTourMailObject = communicationArray.getJSONObject(0).opt("disabletourmail");
				boolean tourReminder = booleanValue(tourReminderObject);
				boolean disableTourMail = booleanValue(disableTourMailObject);
				if (!disableTourMail && !tourReminder) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("Exception come while check tour reminder required." + e.getMessage());
		}
	}
}
