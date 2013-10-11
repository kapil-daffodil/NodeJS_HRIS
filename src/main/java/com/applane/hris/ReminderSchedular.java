package com.applane.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.service.cron.ApplaneCron;
import com.applane.service.mail.ApplaneMail;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class ReminderSchedular implements ApplaneCron {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void cronCall() throws DeadlineExceededException {
		try {
			schedularForAllOrganizations();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void schedularForAllOrganizations() throws JSONException, ParseException {
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		Date currentDate = dateFormat.parse(TODAY_DATE);

		JSONArray organizationArray = getOrganizationList();
		int organizationArrayCount = (organizationArray == null || organizationArray.length() == 0) ? 0 : organizationArray.length();
		if (organizationArrayCount > 0) {
			for (int organizationCounter = 0; organizationCounter < organizationArrayCount; organizationCounter++) {
				Object organizationId = organizationArray.getJSONObject(organizationCounter).opt("organizationid");
				Object organizationName = organizationArray.getJSONObject(organizationCounter).opt("organizationid.organization");

				if (organizationId != null && organizationName != null) {
					setSession(organizationId, organizationName);
					boolean isLeaveReminder = CommunicationUtility.isLeaveReminderRequired();
					if (isLeaveReminder) {
						JSONArray leaveArray = getLeaveRequestRecords();
						int leaveArrayCount = (leaveArray == null || leaveArray.length() == 0) ? 0 : leaveArray.length();
						if (leaveArrayCount > 0) {
							for (int leaveCounter = 0; leaveCounter < leaveArrayCount; leaveCounter++) {
								Object leaveRequestId = leaveArray.getJSONObject(leaveCounter).opt("__key__");
								Object sendReminderObject = leaveArray.getJSONObject(leaveCounter).opt("sendreminder");
								boolean sendReminder = CommunicationUtility.booleanValue(sendReminderObject);
								String leaveCode = leaveArray.getJSONObject(leaveCounter).optString("leavecode");
								String employeeOfficialEmailId = (String) (leaveArray.getJSONObject(leaveCounter).opt("employeeid.officialemailid") == null ? "" : leaveArray.getJSONObject(leaveCounter).opt("employeeid.officialemailid"));
								String employeeName = (String) (leaveArray.getJSONObject(leaveCounter).opt("employeeid.name") == null ? "" : leaveArray.getJSONObject(leaveCounter).opt("employeeid.name"));
								String approverOfficialEmailId = (String) (leaveArray.getJSONObject(leaveCounter).opt("approverid.officialemailid") == null ? "" : leaveArray.getJSONObject(leaveCounter).opt("approverid.officialemailid"));
								String approverName = (String) (leaveArray.getJSONObject(leaveCounter).opt("approverid.name") == null ? "" : leaveArray.getJSONObject(leaveCounter).opt("approverid.name"));
								Object leaveRequestDateObject = leaveArray.getJSONObject(leaveCounter).opt("requestdate");
								Date leaveRequestDate = CommunicationUtility.checkDateFormat(leaveRequestDateObject);
								String leaveRequestDateInString = getDateInString(leaveRequestDate);
								long hoursDifference = hoursDifference(leaveRequestDate, currentDate);
								if (hoursDifference >= 48 && !sendReminder) {
									sendLeaveReminder(leaveCode, employeeName, employeeOfficialEmailId, approverName, approverOfficialEmailId, leaveRequestDateInString);
									updateLeaveRecord(leaveRequestId);
								}
							}
						}
						JSONArray shortLeaveRequestArray = getShortLeaveRequestRecords();
						int shortLeaveRequestArrayCount = (shortLeaveRequestArray == null || shortLeaveRequestArray.length() == 0) ? 0 : shortLeaveRequestArray.length();
						if (shortLeaveRequestArrayCount > 0) {
							for (int shortLeaveCounter = 0; shortLeaveCounter < shortLeaveRequestArrayCount; shortLeaveCounter++) {
								Object shortLeaveRequestId = shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("__key__");
								Object sendReminderObject = shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("sendreminder");
								boolean sendReminder = CommunicationUtility.booleanValue(sendReminderObject);
								String shortLeaveCode = shortLeaveRequestArray.getJSONObject(shortLeaveCounter).optString("shortleavecode");
								String employeeOfficialEmailId = (String) (shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("employeeid.officialemailid") == null ? "" : shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("employeeid.officialemailid"));
								String employeeName = (String) (shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("employeeid.name") == null ? "" : shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("employeeid.name"));
								String approverOfficialEmailId = (String) (shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("approverid.officialemailid") == null ? "" : shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("approverid.officialemailid"));
								String approverName = (String) (shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("approverid.name") == null ? "" : shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("approverid.name"));
								Object shortLeaveRequestDateObject = shortLeaveRequestArray.getJSONObject(shortLeaveCounter).opt("requestdate");
								Date shortLeaveRequestDate = CommunicationUtility.checkDateFormat(shortLeaveRequestDateObject);
								String shortLeaveRequestDateInString = getDateInString(shortLeaveRequestDate);
								long hoursDifference = hoursDifference(shortLeaveRequestDate, currentDate);
								if (hoursDifference >= 48 && !sendReminder) {
									sendShortLeaveReminder(shortLeaveCode, employeeName, employeeOfficialEmailId, approverName, approverOfficialEmailId, shortLeaveRequestDateInString);
									updateShortLeaveRecord(shortLeaveRequestId);
								}
							}
						}
					}

					boolean isTourReminder = CommunicationUtility.isTourReminderRequired();
					if (isTourReminder) {
						JSONArray tourRequestArray = getTourRequestRecords();
						int tourRequestArrayCount = (tourRequestArray == null || tourRequestArray.length() == 0) ? 0 : tourRequestArray.length();
						if (tourRequestArrayCount > 0) {
							for (int tourCounter = 0; tourCounter < tourRequestArrayCount; tourCounter++) {
								Object tourRequestId = tourRequestArray.getJSONObject(tourCounter).opt("__key__");
								Object sendReminderObject = tourRequestArray.getJSONObject(tourCounter).opt("sendreminder");
								boolean sendReminder = CommunicationUtility.booleanValue(sendReminderObject);
								String tourCode = tourRequestArray.getJSONObject(tourCounter).optString("tourcode");
								String employeeOfficialEmailId = (String) (tourRequestArray.getJSONObject(tourCounter).opt("employeeid.officialemailid") == null ? "" : tourRequestArray.getJSONObject(tourCounter).opt("employeeid.officialemailid"));
								String employeeName = (String) (tourRequestArray.getJSONObject(tourCounter).opt("employeeid.name") == null ? "" : tourRequestArray.getJSONObject(tourCounter).opt("employeeid.name"));
								String approverOfficialEmailId = (String) (tourRequestArray.getJSONObject(tourCounter).opt("approverid.officialemailid") == null ? "" : tourRequestArray.getJSONObject(tourCounter).opt("approverid.officialemailid"));
								String approverName = (String) (tourRequestArray.getJSONObject(tourCounter).opt("approverid.name") == null ? "" : tourRequestArray.getJSONObject(tourCounter).opt("approverid.name"));
								Object tourRequestDateObject = tourRequestArray.getJSONObject(tourCounter).opt("creationdate");
								Date tourRequestDate = CommunicationUtility.checkDateFormat(tourRequestDateObject);
								String tourRequestDateInString = getDateInString(tourRequestDate);
								long hoursDifference = hoursDifference(tourRequestDate, currentDate);
								if (hoursDifference >= 48 && !sendReminder) {
									sendTourReminder(tourCode, employeeName, employeeOfficialEmailId, approverName, approverOfficialEmailId, tourRequestDateInString);
									updateTourRecord(tourRequestId);
								}
							}
						}
					}
				}
			}
		}
	}

	private void sendShortLeaveReminder(String shortLeaveCode, String employeeName, String employeeOffcialEmailId, String approverName, String approverOfficialEmailid, String shortLeaveRequestDate) {
		ApplaneMail mailSender = new ApplaneMail();

		boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
		String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
		String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
		String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);
		String title = "Reminder: Approval pending on short leave (" + shortLeaveCode + ") requested by " + employeeOffcialEmailId;
		StringBuffer messageContents = new StringBuffer();
		messageContents.append("<img src=\"" + organizationLogo + "\" alt=\" \" />");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
		messageContents.append("Hello " + approverName + ",<BR><BR>");
		messageContents.append("Approval is pending on following short leave request:<br>");
		messageContents.append("<ul><li><B>leave Code  : </B>" + shortLeaveCode + "</li>");
		messageContents.append("<li><B>Requested By : </B>" + employeeOffcialEmailId + "</li>");
		messageContents.append("<li><B>Requested On : </B>" + shortLeaveRequestDate + "</li></ul>");
		messageContents.append("Regards, <BR>");
		messageContents.append("(" + organizationName + ")");
		try {
			mailSender.setMessage(title, messageContents.toString(), true);
			mailSender.setTo(approverOfficialEmailid);
			mailSender.setReplyTo(employeeOffcialEmailId);
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while send short leave reminder." + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void sendTourReminder(String tourCode, String employeeName, String employeeOffcialEmailId, String approverName, String approverOfficialEmailid, String tourRequestDate) {
		ApplaneMail mailSender = new ApplaneMail();
		boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
		String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
		String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
		String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);
		String title = "Reminder: Approval pending on tour (" + tourCode + ") requested by " + employeeOffcialEmailId;
		StringBuffer messageContents = new StringBuffer();
		messageContents.append("<img src=\"" + organizationLogo + "\" alt=\" \" />");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
		messageContents.append("Hello " + approverName + ",<BR><BR>");
		messageContents.append("Approval is pending on following tour request:<br>");
		messageContents.append("<ul><li><B>Tour Code  : </B>" + tourCode + "</li>");
		messageContents.append("<li><B>Requested By : </B>" + employeeOffcialEmailId + "</li>");
		messageContents.append("<li><B>Requested On : </B>" + tourRequestDate + "</li></ul>");
		messageContents.append("Regards, <BR>");
		messageContents.append("(" + organizationName + ")");
		try {
			mailSender.setMessage(title, messageContents.toString(), true);
			mailSender.setTo(approverOfficialEmailid);
			mailSender.setReplyTo(employeeOffcialEmailId);
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while send tour reminder." + e.getMessage());
		}
	}

	private JSONArray getTourRequestRecords() {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_tourrequests");
			JSONArray array = new JSONArray();
			array.put("__key__");
			array.put("tourcode");
			array.put("sendreminder");
			array.put("employeeid.officialemailid");
			array.put("employeeid.name");
			array.put("approverid.officialemailid");
			array.put("approverid.name");
			array.put("creationdate");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "tourstatusid = " + HRISConstants.TOUR_NEW);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray tourArray = object.getJSONArray("hris_tourrequests");
			return tourArray;
		} catch (JSONException e) {
			throw new BusinessLogicException("Some unknown error occured while get tour request records." + e.getMessage());
		}
	}

	private void sendLeaveReminder(String leaveCode, String employeeName, String employeeOffcialEmailId, String approverName, String approverOfficialEmailid, String leaveRequestDate) {
		ApplaneMail mailSender = new ApplaneMail();
		String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
		String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
		String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);
		String title = "Reminder: Approval pending on leave (" + leaveCode + ") requested by " + employeeOffcialEmailId;
		StringBuffer messageContents = new StringBuffer();
		messageContents.append("<img src=\"" + organizationLogo + "\" alt=\" \" />");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
		messageContents.append("Hello " + approverName + ",<BR><BR>");
		messageContents.append("Approval is pending on following leave request:<br>");
		messageContents.append("<ul><li><B>Leave Code  : </B>" + leaveCode + "</li>");
		messageContents.append("<li><B>Requested By : </B>" + employeeOffcialEmailId + "</li>");
		messageContents.append("<li><B>Requested On : </B>" + leaveRequestDate + "</li></ul>");
		messageContents.append("Regards, <BR>");
		messageContents.append("(" + organizationName + ")");
		try {
			mailSender.setMessage(title, messageContents.toString(), true);
			mailSender.setTo(approverOfficialEmailid);
			mailSender.setReplyTo(employeeOffcialEmailId);
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while send leave reminder." + e.getMessage());
		}
	}

	private JSONArray getOrganizationList() {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "up_organizationapplications");
			JSONArray array = new JSONArray();
			array.put("organizationid");
			array.put("organizationid.organization");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "applicationid = 601");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray organizationArray = object.getJSONArray("up_organizationapplications");
			return organizationArray;
		} catch (JSONException e) {
			throw new BusinessLogicException("Some unknown error occured while get organization list." + e.getMessage());
		}
	}

	private void setSession(Object organizationId, Object organizationName) {
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, organizationId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, organizationName);
	}

	private JSONArray getShortLeaveRequestRecords() {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_shortleaverequests");
			JSONArray array = new JSONArray();
			array.put("__key__");
			array.put("shortleavecode");
			array.put("sendreminder");
			array.put("employeeid.officialemailid");
			array.put("employeeid.name");
			array.put("approverid.officialemailid");
			array.put("approverid.name");
			array.put("requestdate");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "shortleavestatusid = " + HRISConstants.SHORT_LEAVE_NEW);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray shortLeaveArray = object.getJSONArray("hris_shortleaverequests");
			return shortLeaveArray;
		} catch (JSONException e) {
			throw new BusinessLogicException("Some unknown error occured while get short leave records." + e.getMessage());
		}
	}

	private JSONArray getLeaveRequestRecords() {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_leaverequests");
			JSONArray array = new JSONArray();
			array.put("__key__");
			array.put("leavecode");
			array.put("sendreminder");
			array.put("employeeid.officialemailid");
			array.put("employeeid.name");
			array.put("approverid.officialemailid");
			array.put("approverid.name");
			array.put("otherapproverid");
			array.put("requestdate");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "leavestatusid = " + HRISConstants.LEAVE_NEW);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray leaveArray = object.getJSONArray("hris_leaverequests");
			return leaveArray;
		} catch (JSONException e) {
			throw new BusinessLogicException("Some unknown error occured while get leave records." + e.getMessage());
		}
	}

	private long hoursDifference(Date requestDate, Date currentDate) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(requestDate);

		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(currentDate);

		// Get the represented date in milliseconds
		long time1 = cal1.getTimeInMillis();
		long time2 = cal2.getTimeInMillis();
		long diff = time2 - time1;
		long diffSec = diff / 1000;
		long diffMin = diff / (60 * 1000);
		// Difference in hours
		long diffHours = diff / (60 * 60 * 1000);
		return diffHours;
	}

	private String getDateInString(Date date) {
		try {
			if (date != null) {
				return new SimpleDateFormat("dd MMM yyyy").format(date);
			} else {
				return "";
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error come while parsing date in string." + e.getMessage());
		}
	}

	private void updateLeaveRecord(Object leaveRequestId) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "hris_leaverequests");
			JSONObject row = new JSONObject();
			row.put("__key__", leaveRequestId);
			row.put("sendreminder", 1);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while update send reminder in leave.");
		}
	}

	private void updateTourRecord(Object tourRequestId) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "hris_tourrequests");
			JSONObject row = new JSONObject();
			row.put("__key__", tourRequestId);
			row.put("sendreminder", 1);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while update send reminder in tour.");
		}
	}

	private void updateShortLeaveRecord(Object shortLeaveRequestId) {
		try {
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "hris_shortleaverequests");
			JSONObject row = new JSONObject();
			row.put("__key__", shortLeaveRequestId);
			row.put("sendreminder", 1);
			updates.put(Data.Update.UPDATES, row);
			ResourceEngine.update(updates);
		} catch (Exception e) {
			throw new BusinessLogicException("Some unknown error occured while update send reminder in short leave.");
		}
	}

}
