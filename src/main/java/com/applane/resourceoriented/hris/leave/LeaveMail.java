package com.applane.resourceoriented.hris.leave;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.app.shared.constants.View;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.service.smshelper.SendSMSService;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

// job define on "hris_leaverequest"

public class LeaveMail implements OperationJob {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			if (record.has("temp")) {
				return;
			}
			String operation = record.getOperationType();
			Object employeeId = record.getValue("employeeid");
			JSONArray indirectingReportingToIds = getIndirectingReportingToIds(employeeId);
			Map<String, Object> indirectReportingToMap = new HashMap<String, Object>();
			if (indirectingReportingToIds != null && indirectingReportingToIds.length() > 0) {
				for (int counter = 0; counter < indirectingReportingToIds.length(); counter++) {
					String offMail = indirectingReportingToIds.getJSONObject(counter).optString("indirectreportingto.officialemailid");
					Object indId = indirectingReportingToIds.getJSONObject(counter).opt("indirectreportingto");
					indirectReportingToMap.put(offMail, indId);
				}
			}

			LogUtility.writeError("operation >> " + operation + "<< record >> " + record.getValues());

			if (operation.equalsIgnoreCase("insert")) {
				String employeeName = DataTypeUtilities.stringValue(record.getValue("employeeid.name"));
				String employeeMobileNo = DataTypeUtilities.stringValue(record.getValue("employeeid.mobileno"));
				Object branchId = record.getValue("employeeid.branchid");

				String approverMobileNo = DataTypeUtilities.stringValue(record.getValue("approverid.mobileno"));
				Object fromDateObject = record.getValue("fromdate");

				Object toDateObject = record.getValue("todate");
				String smsCode = (String) record.getValue("smscode");

				JSONObject employeeDetails = putEmployeeDetailsIntoJSONObject(record);

				Date fromLeaveDate = null;
				Date toLeaveDate = null;
				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

				if (fromDateObject != null && toDateObject != null) {
					try {
						fromLeaveDate = updateDateFormat.parse("" + fromDateObject);
						toLeaveDate = updateDateFormat.parse("" + toDateObject);
					} catch (ParseException e) {
						throw new RuntimeException("Error Come while parsing " + e.getMessage());
					}
				}

				SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
				String fromDate = fromLeaveDate == null ? "" : dateFormat.format(fromLeaveDate);
				String toDate = toLeaveDate == null ? "" : dateFormat.format(toLeaveDate);
				List<String> employeeEmailIdList = new ArrayList<String>();
				try {

					if (indirectReportingToMap != null && indirectReportingToMap.size() > 0) {
						for (String offMailID : indirectReportingToMap.keySet()) {
							if (offMailID != null && offMailID.length() > 0 && !employeeEmailIdList.contains(offMailID)) {
								employeeEmailIdList.add(offMailID);
							}
						}
					}
					sendLeaveMail(employeeDetails, employeeEmailIdList, fromDate, toDate);
					try {
						sendLeaveSMS(employeeMobileNo, approverMobileNo, employeeName, fromDate, toDate, branchId, smsCode);
					} catch (BusinessLogicException e) {
						LogUtility.writeLog("Leave Mail BL >> " + e.getMessage());
					}
				} catch (Exception e) {
					throw new RuntimeException("Error Come: " + e.getMessage());

				}
			} else if (operation.equalsIgnoreCase("update")) {
				int newLeaveStatus = (Integer) record.getNewValue("leavestatusid");
				String approverComment = Translator.stringValue(record.getValue("approvercomment"));
				int leaveStatus = Translator.integerValue(record.getValue("leavestatusid"));
				int oldLeaveStatus = (Integer) record.getOldValue("leavestatusid");
				try {

					Set<String> employeeEmailIdList = new HashSet<String>();

					Date fromLeaveDate = null;
					Date toLeaveDate = null;

					JSONObject employeeDetails = putEmployeeDetailsIntoJSONObject(record);

					SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

					if (employeeDetails.get("fromDateObject") != null && employeeDetails.get("toDateObject") != null) {
						try {
							fromLeaveDate = updateDateFormat.parse("" + employeeDetails.get("fromDateObject"));
							toLeaveDate = updateDateFormat.parse("" + employeeDetails.get("toDateObject"));
						} catch (ParseException e) {
							throw new RuntimeException("Error Come while parsing Dates" + e.getMessage());
						}
					}

					SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
					String fromDate = fromLeaveDate == null ? "" : dateFormat.format(fromLeaveDate);
					String toDate = toLeaveDate == null ? "" : dateFormat.format(toLeaveDate);
					if (indirectReportingToMap != null && indirectReportingToMap.size() > 0) {
						for (String offMailID : indirectReportingToMap.keySet()) {
							if (offMailID != null && offMailID.length() > 0 && !employeeEmailIdList.contains(offMailID)) {
								employeeEmailIdList.add(offMailID);
							}
						}
					}
					if (leaveStatus == HRISConstants.LEAVE_NEW) {
						try {
							List<String> list = new ArrayList<String>(employeeEmailIdList);
							sendLeaveMail(employeeDetails, list, fromDate, toDate);
						} catch (Exception e) {
							LogUtility.writeLog("Leave Mail Exception Sending Updates with status new: Trace >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
							throw new RuntimeException("Error Come during Sending Mail On Update Operation: " + e.getMessage());
						}
					}
					if ((oldLeaveStatus == HRISConstants.LEAVE_NEW && newLeaveStatus == HRISConstants.LEAVE_APPROVED) || (oldLeaveStatus == HRISConstants.LEAVE_REJECTED && newLeaveStatus == HRISConstants.LEAVE_APPROVED)) {
						try {
							String[] leaveNotificationEmployeeOfficialMailIds = getLeaveNotificationOfficialMailIds(record);
							if (leaveNotificationEmployeeOfficialMailIds != null && leaveNotificationEmployeeOfficialMailIds.length > 0) {
								for (String leaveNotificationEmployeeOfficialMailId : leaveNotificationEmployeeOfficialMailIds) {
									if (leaveNotificationEmployeeOfficialMailId != null && leaveNotificationEmployeeOfficialMailId.length() > 0) {
										employeeEmailIdList.add(leaveNotificationEmployeeOfficialMailId);
									}
								}
							}

							LogUtility.writeLog("employeeEmailIdList >> " + employeeEmailIdList);

							approveLeaveMail(employeeDetails, employeeEmailIdList, fromDate, toDate, approverComment);
							approveLeaveSMS(employeeDetails, fromDate, toDate);
						} catch (BusinessLogicException e) {
							LogUtility.writeLog("1 > Leave Mail BL >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
						}
					}
					if ((oldLeaveStatus == HRISConstants.LEAVE_NEW && newLeaveStatus == HRISConstants.LEAVE_REJECTED) || (oldLeaveStatus == HRISConstants.LEAVE_APPROVED && newLeaveStatus == HRISConstants.LEAVE_REJECTED)) {
						try {
							rejectLeaveMail(employeeDetails, employeeEmailIdList, fromDate, toDate, approverComment);
						} catch (BusinessLogicException e) {
							LogUtility.writeLog("2 > Leave Mail BL >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Error Come during update operation: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("Leave Mail On After mail Exception >> " + trace);
			throw new BusinessLogicException(trace);
		}

	}

	private String[] getLeaveNotificationOfficialMailIds(Record record) throws JSONException {

		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put(new JSONObject().put("emp_leave_notification_id", new JSONArray().put("officialemailid")));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		query.put(Data.Query.COLUMNS, array);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray mailConfigurationLeaveNotificationJsonArray = object.optJSONArray("hris_mailconfigurations");

		LogUtility.writeLog("Mail Configuration Leave Notification Employees : " + mailConfigurationLeaveNotificationJsonArray);

		StringBuilder sb = new StringBuilder();

		if (mailConfigurationLeaveNotificationJsonArray != null && mailConfigurationLeaveNotificationJsonArray.length() > 0) {

			JSONObject jsonObject = mailConfigurationLeaveNotificationJsonArray.getJSONObject(0);

			JSONArray leaveNotificationEmployeeJsonArray = jsonObject.optJSONArray("emp_leave_notification_id");
			LogUtility.writeLog("Leave Notification Employee Json Array : [" + leaveNotificationEmployeeJsonArray + "]");

			int ccEmployeesLength = (leaveNotificationEmployeeJsonArray == null) ? 0 : leaveNotificationEmployeeJsonArray.length();
			LogUtility.writeLog("ccEmployees : [" + leaveNotificationEmployeeJsonArray + "], ccEmployeesLength : " + ccEmployeesLength);

			for (int i = 0; i < ccEmployeesLength; i++) {
				String ccEmployeeOfficialMailId = leaveNotificationEmployeeJsonArray.getJSONObject(i).optString("officialemailid", null);
				if (ccEmployeeOfficialMailId != null) {
					sb.append(ccEmployeeOfficialMailId).append(",");
				}
			}
		}

		LogUtility.writeLog("Leave Notification Employees String Builder : " + sb);

		String[] leaveNotificationEmployeeOfficialMailIds = null;
		if (sb.length() > 0) {
			leaveNotificationEmployeeOfficialMailIds = sb.toString().split(",");
		}

		return leaveNotificationEmployeeOfficialMailIds;
	}

	private JSONObject putEmployeeDetailsIntoJSONObject(Record record) throws JSONException {
		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		Object key = record.getKey();
		String employeeEmailId = DataTypeUtilities.stringValue(record.getValue("employeeid.officialemailid"));
		String employeeCode = DataTypeUtilities.stringValue(record.getValue("employeeid.employeecode"));
		String employeeDesignation = DataTypeUtilities.stringValue(record.getValue("employeeid.designationid.name"));
		String employeeName = DataTypeUtilities.stringValue(record.getValue("employeeid.name"));
		String employeeMobileNo = DataTypeUtilities.stringValue(record.getValue("employeeid.mobileno"));
		Object branchId = record.getValue("employeeid.branchid");
		Object approverEmailId = record.getValue("approverid.officialemailid");
		Object approverId = record.getValue("approverid");
		String approverName = DataTypeUtilities.stringValue(record.getValue("approverid.name"));
		String approverMobileNo = DataTypeUtilities.stringValue(record.getValue("approverid.mobileno"));
		String leaveType = DataTypeUtilities.stringValue(record.getValue("leavetypeid.name"));
		String leaveTypeGirnarSoft = DataTypeUtilities.stringValue(record.getValue("leavetypeid.name_girnarsoft"));
		Object fromDateObject = record.getValue("fromdate");
		String fromDurationType = DataTypeUtilities.stringValue(record.getValue("fromdurationtypeid.name"));
		String toDurationType = DataTypeUtilities.stringValue(record.getValue("todurationtypeid.name"));
		Object toDateObject = record.getValue("todate");
		String leaveReason = Translator.stringValue(record.getValue("reason"));
		String leaveCode = Translator.stringValue(record.getValue("leavecode"));
		String smsCode = Translator.stringValue(record.getValue("smscode"));
		Object substituteName = record.getValue("substitute_id.name");
		Object requestDate = record.getValue("requestdate");
		Object employeeId = record.getValue("employeeid");
		Object genderId = record.getValue("employeeid.genderid");
		Object substituteGenderId = record.getValue("substitute_id.genderid");
		Object branchName = record.getValue("employeeid.branchid.name");
		Object leaveApproveByCmdMoreThen = record.getValue("employeeid.leavepolicyid.leave_approve_by_cmd_more_than");

		String otherApproverName = Translator.stringValue(record.getValue("otherapproverid.name"));
		String otherApproverEmailID = Translator.stringValue(record.getValue("otherapproverid.officialemailid"));
		String otherApproverMobileNo = Translator.stringValue(record.getValue("otherapproverid.mobileno"));

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
		employeeDetails.put("employeeEmailId", employeeEmailId);
		employeeDetails.put("employeeCode", employeeCode);
		employeeDetails.put("employeeDesignation", employeeDesignation);
		employeeDetails.put("employeeName", employeeName);
		employeeDetails.put("employeeMobileNo", employeeMobileNo);
		employeeDetails.put("branchId", branchId);
		employeeDetails.put("approverEmailId", approverEmailId);
		employeeDetails.put("approverId", approverId);
		employeeDetails.put("approverName", approverName);
		employeeDetails.put("approverMobileNo", approverMobileNo);
		if (organizationId == 10719) {
			employeeDetails.put("leaveType", leaveTypeGirnarSoft);
		} else {
			employeeDetails.put("leaveType", leaveType);
		}
		employeeDetails.put("fromDurationType", fromDurationType);
		employeeDetails.put("toDurationType", toDurationType);
		employeeDetails.put("fromDateObject", fromDateObject);
		employeeDetails.put("toDateObject", toDateObject);
		employeeDetails.put("leaveReason", leaveReason);
		employeeDetails.put("leaveCode", leaveCode);
		employeeDetails.put("branchName", branchName);
		employeeDetails.put("smsCode", smsCode);
		employeeDetails.put("gender", gender);
		employeeDetails.put("gender1", gender1);
		if (leaveApproveByCmdMoreThen != null) {
			employeeDetails.put("leaveApproveByCmdMoreThen", leaveApproveByCmdMoreThen);
		}
		if (substituteName != null) {
			employeeDetails.put("substituteGender", substituteGender);
			employeeDetails.put("substituteGender1", substituteGender1);
			employeeDetails.put("substituteName", substituteName);
		}
		employeeDetails.put("requestDate", requestDate);
		employeeDetails.put("otherApproverName", otherApproverName);
		employeeDetails.put("otherApproverEmailID", otherApproverEmailID);
		employeeDetails.put("otherApproverMobileNo", otherApproverMobileNo);
		return employeeDetails;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void rejectLeaveMail(JSONObject employeeDetails, Set<String> employeeEmailIdList, String fromDate, String toDate, String approverComment) {

		ApplaneMail mailSender = new ApplaneMail();
		try {

			Map<String, Object> communicationDetails = CommunicationUtility.getCommunicationInfo();
			boolean disableEmail = (Boolean) ((Map<String, Object>) communicationDetails.get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String replyToId = (String) ((Map<String, Object>) communicationDetails.get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			String organizationLogo = (String) communicationDetails.get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) communicationDetails.get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) communicationDetails.get(Communication_Constants.ORGANIZATION_NAME);
			String[] concernedPersons = (String[]) ((Map<String, Object>) communicationDetails.get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.CONCERNED_PERSONS);
			if (organizationLogo == null) {
				organizationLogo = "";
			}
			if (signature == null) {
				signature = "";
			}
			for (int counter = 0; concernedPersons != null && (counter < concernedPersons.length); counter++) {
				String concernedPersonEmailId = concernedPersons[counter];

				if (concernedPersonEmailId != null && !employeeEmailIdList.contains((String) concernedPersonEmailId)) {
					employeeEmailIdList.add(concernedPersonEmailId);
				}
			}
			if (!disableEmail) {
				JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.get("branchId"));
				int hrAssigningCount = (hrAssigningArray == null || hrAssigningArray.length() == 0) ? 0 : hrAssigningArray.length();
				if (hrAssigningCount > 0) {
					String hrEmailId = (String) hrAssigningArray.getJSONObject(0).opt("employeeid.officialemailid");
					if (hrEmailId != null) {
						employeeEmailIdList.add((String) hrEmailId);
					} else {
						if (replyToId != null) {
							employeeEmailIdList.add((String) replyToId);
						}
					}
				} else {
					employeeEmailIdList.add((String) replyToId);
				}

				try {
					String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
					JSONObject query = new JSONObject();
					query.put(Data.Query.RESOURCE, "hris_employees");
					JSONArray columnArray = new JSONArray();
					columnArray.put("name");
					query.put(Data.Query.COLUMNS, columnArray);
					query.put(Data.Query.FILTERS, "officialemailid = '" + CURRENT_USER_EMAILID + "'");
					JSONObject employeeObject;
					employeeObject = ResourceEngine.query(query);
					JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
					String currentUserName = employeeArray.getJSONObject(0).optString("name");
					JSONArray branches = EmployeeSalarySheetReport.getHrMailIdDetail("branchid = " + employeeDetails.get("branchId"));

					if (branches != null && branches.length() > 0) {
						String hrEmailId = branches.getJSONObject(0).optString("employeeid.officialemailid");
						if (hrEmailId.length() > 0) {
							employeeEmailIdList.add(hrEmailId);
						}
					}
					if (CURRENT_USER_EMAILID.equalsIgnoreCase("" + employeeDetails.get("approverEmailId"))) {
						if (employeeDetails.get("otherApproverEmailID") != null) {
							employeeEmailIdList.add((String) employeeDetails.get("otherApproverEmailID"));
						}

						String title = "Rejected Leave (" + employeeDetails.get("leaveCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.get("employeeName") + ",<BR><BR>");
						messageContents.append("Your leave request detail as per below has been rejected: <br>");
						messageContents.append("<ul><li><B>Leave Type : </B>" + employeeDetails.get("leaveType") + "</li>");
						messageContents.append("<li><B>From Date : </B>" + fromDate + "<b>&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("fromDurationType") + "</li>");
						messageContents.append("<li><B>To Date : </B>" + toDate + "<b>&nbsp;&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("toDurationType") + "</li>");
						messageContents.append("<li><B>Reason : </B>" + employeeDetails.get("leaveReason") + "</li>");
						messageContents.append("<li><B>Rejected By: </B>" + employeeDetails.get("approverName") + "</li>");
						messageContents.append("<li><B>Rejecter Comments : </B>" + approverComment + "</li></ul>");
						messageContents.append(getFooterMessageForMail("" + employeeDetails.get("approverName"), organizationName));

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								messageContents.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.get("employeeEmailId"));

								Iterator itr = employeeEmailIdList.iterator();
								int i = 0;
								int size = 0;
								if (employeeDetails.has("approverEmailId")) {
									size = 1;
								}
								String[] ccEmailIds = new String[employeeEmailIdList.size() + size];
								while (itr.hasNext()) {
									ccEmailIds[i] = (String) itr.next();
									i++;
								}
								if (employeeDetails.has("approverEmailId")) {
									ccEmailIds[i] = "" + employeeDetails.get("approverEmailId");
								}
								if (ccEmailIds.length > 0) {
									mailSender.setCc(ccEmailIds);
								}
								mailSender.setReplyTo("" + employeeDetails.get("approverEmailId"));
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (ApplaneMailException e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					} else if (CURRENT_USER_EMAILID.equalsIgnoreCase("" + employeeDetails.get("otherApproverEmailID"))) {
						employeeEmailIdList.add((String) employeeDetails.get("otherApproverEmailID"));

						String title = "Rejected Leave (" + employeeDetails.get("leaveCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.get("employeeName") + ",<BR><BR>");
						messageContents.append("Your leave request detail as per below has been rejected: <br>");
						messageContents.append("<ul><li><B>Leave Type : </B>" + employeeDetails.get("leaveType") + "</li>");
						messageContents.append("<li><B>From Date : </B>" + fromDate + "<b>&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("fromDurationType") + "</li>");
						messageContents.append("<li><B>To Date : </B>" + toDate + "<b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("toDurationType") + "</li>");
						messageContents.append("<li><B>Reason of Leave: </B>" + employeeDetails.get("leaveReason") + "</li>");
						messageContents.append("<li><B>Rejected By: </B>" + employeeDetails.get("otherApproverName") + "</li>");
						messageContents.append("<li><B>Rejecter Comments : </B>" + approverComment + "</li></ul>");
						messageContents.append(getFooterMessageForMail("" + employeeDetails.get("otherApproverName"), organizationName));
						messageContents.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
						mailSender.setMessage(title, messageContents.toString(), true);
						mailSender.setTo("" + employeeDetails.get("employeeEmailId"));

						Iterator itr = employeeEmailIdList.iterator();
						int i = 0;
						int size = 0;
						if (employeeDetails.has("approverEmailId")) {
							size = 1;
						}
						String[] ccEmailIds = new String[employeeEmailIdList.size() + size];
						while (itr.hasNext()) {
							ccEmailIds[i] = (String) itr.next();
							i++;
						}
						if (employeeDetails.has("approverEmailId")) {
							ccEmailIds[i] = "" + employeeDetails.get("approverEmailId");
						}
						if (ccEmailIds.length > 0) {
							mailSender.setCc(ccEmailIds);
						}
						mailSender.setReplyTo("" + employeeDetails.get("otherApproverEmailID"));
						mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
					} else {
						if (employeeDetails.get("otherApproverEmailID") != null) {
							employeeEmailIdList.add((String) employeeDetails.get("otherApproverEmailID"));
						}

						String title = "Rejected Leave (" + employeeDetails.get("leaveCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.get("employeeName") + ",<BR><BR>");
						messageContents.append("Your leave request detail as per below has been rejected: <br>");
						messageContents.append("<ul><li><B>Leave Type : </B>" + employeeDetails.get("leaveType") + "</li>");
						messageContents.append("<li><B>From Date : </B>" + fromDate + "<b>&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("fromDurationType") + "</li>");
						messageContents.append("<li><B>To Date : </B>" + toDate + "<b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("toDurationType") + "</li>");
						messageContents.append("<li><B>Reason of Leave: </B>" + employeeDetails.get("leaveReason") + "</li>");
						messageContents.append("<li><B>Rejected By: </B>" + currentUserName + "</li>");
						messageContents.append("<li><B>Rejecter Comments : </B>" + approverComment + "</li></ul>");
						messageContents.append(getFooterMessageForMail(currentUserName, organizationName));

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								messageContents.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.get("employeeEmailId"));
								Iterator itr = employeeEmailIdList.iterator();
								int i = 0;
								int size = 0;
								if (employeeDetails.has("approverEmailId")) {
									size = 1;
								}
								String[] ccEmailIds = new String[employeeEmailIdList.size() + size];
								while (itr.hasNext()) {
									ccEmailIds[i] = (String) itr.next();
									i++;
								}
								if (employeeDetails.has("approverEmailId")) {
									ccEmailIds[i] = "" + employeeDetails.get("approverEmailId");
								}
								if (ccEmailIds.length > 0) {
									mailSender.setCc(ccEmailIds);
								}
								mailSender.setReplyTo(CURRENT_USER_EMAILID);
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (Exception e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					}
				} catch (ApplaneMailException e) {
					throw new BusinessLogicException("Some unknown error occured other aprrove case" + e.getMessage());
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error while sending mail : " + e.getMessage());
		}

	}

	public JSONArray getIndirectingReportingToIds(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees__indirectreportingto");
		JSONArray columnArray = new JSONArray();
		columnArray.put("indirectreportingto");
		columnArray.put("indirectreportingto.officialemailid");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "hris_employees in (" + employeeId + ")");
		JSONObject employeeObject = ResourceEngine.query(query);
		JSONArray organizationArray = employeeObject.getJSONArray("hris_employees__indirectreportingto");
		return organizationArray;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void approveLeaveSMS(JSONObject employeeDetails, String fromDate, String toDate) throws Exception {

		String message = "Leave request of " + employeeDetails.get("employeeName") + " from " + fromDate + " to " + toDate + " has been approved by " + employeeDetails.get("approverName") + ".";
		String otherMessage = "Leave request of " + employeeDetails.get("employeeName") + " from " + fromDate + " to " + toDate + " has been approved by " + employeeDetails.get("otherApproverName") + ".";
		List<Object> smsList = new ArrayList<Object>();
		List<String> employeeMobileNoList = new ArrayList<String>();
		employeeMobileNoList.add((String) employeeDetails.get("employeeMobileNo"));

		try {
			String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
			// String replyToId = null;
			boolean disableSMS;
			String replytoMobileNo = null;
			disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);
			replytoMobileNo = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_MOBILE);
			if (!disableSMS) {
				JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.get("branchId"));
				int hrAssigningCount = (hrAssigningArray == null || hrAssigningArray.length() == 0) ? 0 : hrAssigningArray.length();
				if (hrAssigningCount > 0) {
					String hrMobileNo = (String) hrAssigningArray.getJSONObject(0).opt("mobileno");
					if (hrMobileNo != null && !employeeMobileNoList.contains((String) hrMobileNo)) {
						employeeMobileNoList.add((String) hrMobileNo);
					} else {
						if (replytoMobileNo != null && !employeeMobileNoList.contains((String) replytoMobileNo)) {
							employeeMobileNoList.add((String) replytoMobileNo);
						}
					}
				} else {
					if (replytoMobileNo != null && !employeeMobileNoList.contains((String) replytoMobileNo)) {
						employeeMobileNoList.add((String) replytoMobileNo);
					}
				}

				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_employees");
				JSONArray columnArray = new JSONArray();
				columnArray.put("mobileno");
				columnArray.put("name");
				query.put(Data.Query.COLUMNS, columnArray);
				query.put(Data.Query.FILTERS, "officialemailid = '" + CURRENT_USER_EMAILID + "'");
				JSONObject employeeObject;
				employeeObject = ResourceEngine.query(query);
				JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
				String currentUserMobileNo = employeeArray.getJSONObject(0).optString("mobileno");
				String currentUserName = employeeArray.getJSONObject(0).optString("name");
				if (employeeDetails.get("otherApproverMobileNo") != null && currentUserMobileNo.equalsIgnoreCase("" + employeeDetails.get("otherApproverMobileNo"))) {
					if (!employeeMobileNoList.contains((String) employeeDetails.get("approverMobileNo"))) {
						employeeMobileNoList.add((String) employeeDetails.get("approverMobileNo"));
					}

					int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();
					for (int counter = 0; counter < smsToCount; counter++) {
						JSONObject smsTo = new JSONObject();
						if (employeeMobileNoList.get(counter) != null && employeeMobileNoList.get(counter).length() > 0) {
							smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
							smsTo.put(View.Action.Types.SendSMS.FROM, employeeDetails.get("otherApproverMobileNo"));
							smsTo.put(View.Action.Types.SendSMS.SMS, otherMessage);
							smsList.add(smsTo);
						}
					}
					try {
						SendSMSService.sendSMSUtility(smsList.toArray());
					} catch (IOException e) {
						throw new BusinessLogicException("Error while send SMS by forward to user." + e.getMessage());
					}
				} else if (employeeDetails.get("approverMobileNo") != null && currentUserMobileNo.equalsIgnoreCase("" + employeeDetails.get("approverMobileNo"))) {
					if (employeeDetails.get("otherApproverMobileNo") != null && !employeeMobileNoList.contains((String) employeeDetails.get("approverMobileNo"))) {
						employeeMobileNoList.add((String) employeeDetails.get("otherApproverMobileNo"));
					}
					int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();
					for (int counter = 0; counter < smsToCount; counter++) {
						if (employeeMobileNoList.get(counter) != null && employeeMobileNoList.get(counter).length() > 0) {
							JSONObject smsTo = new JSONObject();
							smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
							smsTo.put(View.Action.Types.SendSMS.FROM, employeeDetails.get("approverMobileNo"));
							smsTo.put(View.Action.Types.SendSMS.SMS, message);
							smsList.add(smsTo);
						}
					}
					try {
						SendSMSService.sendSMSUtility(smsList.toArray());
					} catch (Exception e) {
						throw new BusinessLogicException("Error while send SMS by approver." + e.getMessage());
					}
				} else {
					String anotherMessage = "Leave request of " + employeeDetails.get("employeeName") + " from " + fromDate + " to " + toDate + " has been approved by " + currentUserName + ".";
					if (employeeDetails.get("approverMobileNo") != null && !employeeMobileNoList.contains((String) employeeDetails.get("approverMobileNo"))) {
						employeeMobileNoList.add((String) employeeDetails.get("approverMobileNo"));
					}

					if (employeeDetails.get("otherApproverMobileNo") != null && !employeeMobileNoList.contains((String) employeeDetails.get("approverMobileNo"))) {
						employeeMobileNoList.add((String) employeeDetails.get("otherApproverMobileNo"));
					}
					int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();

					for (int counter = 0; counter < smsToCount; counter++) {
						JSONObject smsTo = new JSONObject();
						if (employeeMobileNoList.get(counter) != null && employeeMobileNoList.get(counter).length() > 0) {
							smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
							smsTo.put(View.Action.Types.SendSMS.FROM, currentUserMobileNo);
							smsTo.put(View.Action.Types.SendSMS.SMS, anotherMessage);
							smsList.add(smsTo);
						}
					}
					try {
						SendSMSService.sendSMSUtility(smsList.toArray());
					} catch (IOException e) {
						throw new BusinessLogicException("Error while send SMS by other user." + e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error come while send SMS " + e.getMessage());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void approveLeaveMail(JSONObject employeeDetails, Set<String> employeeEmailIdList, String fromDate, String toDate, String approverComment) {
		ApplaneMail mailSender = new ApplaneMail();
		try {

			Map<String, Object> communicationDetails = CommunicationUtility.getCommunicationInfo();
			boolean disableEmail = (Boolean) ((Map<String, Object>) communicationDetails.get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String replyToId = (String) ((Map<String, Object>) communicationDetails.get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			String organizationLogo = (String) communicationDetails.get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) communicationDetails.get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) communicationDetails.get(Communication_Constants.ORGANIZATION_NAME);
			String[] concernedPersons = (String[]) ((Map<String, Object>) communicationDetails.get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.CONCERNED_PERSONS);
			if (organizationLogo == null) {
				organizationLogo = "";
			}
			if (signature == null) {
				signature = "";
			}
			for (int counter = 0; concernedPersons != null && (counter < concernedPersons.length); counter++) {
				String concernedPersonEmailId = concernedPersons[counter];

				if (concernedPersonEmailId != null && !employeeEmailIdList.contains((String) concernedPersonEmailId)) {
					employeeEmailIdList.add(concernedPersonEmailId);
				}
			}
			if (!disableEmail) {
				JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.get("branchId"));
				int hrAssigningCount = (hrAssigningArray == null || hrAssigningArray.length() == 0) ? 0 : hrAssigningArray.length();
				if (hrAssigningCount > 0) {
					Object hrId = hrAssigningArray.getJSONObject(0).opt("employeeid");
					updateCCEmployee(employeeDetails.get("key"), hrId);
					String hrEmailId = (String) hrAssigningArray.getJSONObject(0).opt("employeeid.officialemailid");
					if (hrEmailId != null) {
						employeeEmailIdList.add((String) hrEmailId);
					} else {
						if (replyToId != null) {
							employeeEmailIdList.add((String) replyToId);
						}
					}
				} else {
					employeeEmailIdList.add((String) replyToId);
				}
				JSONArray leaveCommentsArray = getLeaveComments(employeeDetails.opt("key"));
				try {
					String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
					JSONObject query = new JSONObject();
					query.put(Data.Query.RESOURCE, "hris_employees");
					JSONArray columnArray = new JSONArray();
					columnArray.put("name");
					query.put(Data.Query.COLUMNS, columnArray);
					query.put(Data.Query.FILTERS, "officialemailid = '" + CURRENT_USER_EMAILID + "'");
					JSONObject employeeObject;
					employeeObject = ResourceEngine.query(query);
					JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
					String currentUserName = employeeArray.getJSONObject(0).optString("name");

					JSONArray branches = EmployeeSalarySheetReport.getHrMailIdDetail("branchid = " + employeeDetails.get("branchId"));
					if (branches != null && branches.length() > 0) {
						String hrEmailId = branches.getJSONObject(0).optString("employeeid.officialemailid");
						if (hrEmailId.length() > 0) {
							employeeEmailIdList.add(hrEmailId);
						}
					}
					if (CURRENT_USER_EMAILID.equalsIgnoreCase("" + employeeDetails.get("approverEmailId"))) {
						if (employeeDetails.get("otherApproverEmailID") != null) {
							employeeEmailIdList.add((String) employeeDetails.get("otherApproverEmailID"));
						}

						String title = "Approved Leave (" + employeeDetails.get("leaveCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.get("employeeName") + ",<BR><BR>");
						messageContents.append("Your leave request detail as per below has been approved: <br>");
						messageContents.append("<ul><li><B>Leave Type : </B>" + employeeDetails.get("leaveType") + "</li>");
						messageContents.append("<li><B>From Date : </B>" + fromDate + "<b>&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("fromDurationType") + "</li>");
						messageContents.append("<li><B>To Date : </B>" + toDate + "<b>&nbsp;&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("toDurationType") + "</li>");
						messageContents.append("<li><B>Reason : </B>" + employeeDetails.get("leaveReason") + "</li>");
						messageContents.append("<li><B>Approved By: </B>" + employeeDetails.get("approverName") + "</li>");
						messageContents.append("<li><B>Approver Comments : </B>" + approverComment + "</li></ul>");
						messageContents.append(getFooterMessageForMail("" + employeeDetails.get("approverName"), organizationName));

						if (leaveCommentsArray != null & leaveCommentsArray.length() > 0) {
							messageContents.append("<BR /> Leave Comments");
							messageContents.append("<table border='1' width='50%'>");
							messageContents.append("<tr><td width='25%'>Employee Name</td><td width='75%'>Comments</td></tr>");
							for (int counter = 0; counter < leaveCommentsArray.length(); counter++) {
								messageContents.append("<tr><td>").append(leaveCommentsArray.getJSONObject(counter).opt("employeeid.name")).append("</td>");
								messageContents.append("<td>").append(leaveCommentsArray.getJSONObject(counter).opt("comment")).append("</td></tr>");
							}
							messageContents.append("</table>");
						}

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								messageContents.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.get("employeeEmailId"));

								Iterator itr = employeeEmailIdList.iterator();
								int i = 0;
								int size = 0;
								if (employeeDetails.has("approverEmailId")) {
									size = 1;
								}
								String[] ccEmailIds = new String[employeeEmailIdList.size() + size];
								while (itr.hasNext()) {
									ccEmailIds[i] = (String) itr.next();
									i++;
								}
								if (employeeDetails.has("approverEmailId")) {
									ccEmailIds[i] = "" + employeeDetails.get("approverEmailId");
								}
								if (ccEmailIds.length > 0) {
									mailSender.setCc(ccEmailIds);
								}
								mailSender.setReplyTo("" + employeeDetails.get("approverEmailId"));
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (ApplaneMailException e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					} else if (CURRENT_USER_EMAILID.equalsIgnoreCase("" + employeeDetails.get("otherApproverEmailID"))) {
						employeeEmailIdList.add((String) employeeDetails.get("otherApproverEmailID"));

						String title = "Approved Leave (" + employeeDetails.get("leaveCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.get("employeeName") + ",<BR><BR>");
						messageContents.append("Your leave request detail as per below has been approved: <br>");
						messageContents.append("<ul><li><B>Leave Type : </B>" + employeeDetails.get("leaveType") + "</li>");
						messageContents.append("<li><B>From Date : </B>" + fromDate + "<b>&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("fromDurationType") + "</li>");
						messageContents.append("<li><B>To Date : </B>" + toDate + "<b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("toDurationType") + "</li>");
						messageContents.append("<li><B>Reason of Leave: </B>" + employeeDetails.get("leaveReason") + "</li>");
						messageContents.append("<li><B>Approved By: </B>" + employeeDetails.get("otherApproverName") + "</li>");
						messageContents.append("<li><B>Approver Comments : </B>" + approverComment + "</li></ul>");
						if (leaveCommentsArray != null & leaveCommentsArray.length() > 0) {
							messageContents.append("<BR /> Leave Comments");
							messageContents.append("<table border='1' width='50%'>");
							messageContents.append("<tr><td width='25%'>Employee Name</td><td width='75%'>Comments</td></tr>");
							for (int counter = 0; counter < leaveCommentsArray.length(); counter++) {
								messageContents.append("<tr><td>").append(leaveCommentsArray.getJSONObject(counter).opt("employeeid.name")).append("</td>");
								messageContents.append("<td>").append(leaveCommentsArray.getJSONObject(counter).opt("comment")).append("</td></tr>");
							}
							messageContents.append("</table><BR /><BR />");
						}
						messageContents.append(getFooterMessageForMail("" + employeeDetails.get("otherApproverName"), organizationName));
						messageContents.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
						mailSender.setMessage(title, messageContents.toString(), true);
						mailSender.setTo("" + employeeDetails.get("employeeEmailId"));

						Iterator itr = employeeEmailIdList.iterator();
						int i = 0;
						int size = 0;
						if (employeeDetails.has("approverEmailId")) {
							size = 1;
						}
						String[] ccEmailIds = new String[employeeEmailIdList.size() + size];
						while (itr.hasNext()) {
							ccEmailIds[i] = (String) itr.next();
							i++;
						}
						if (employeeDetails.has("approverEmailId")) {
							ccEmailIds[i] = "" + employeeDetails.get("approverEmailId");
						}
						if (ccEmailIds.length > 0) {
							mailSender.setCc(ccEmailIds);
						}
						mailSender.setReplyTo("" + employeeDetails.get("otherApproverEmailID"));
						mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
					} else {
						if (employeeDetails.get("otherApproverEmailID") != null) {
							employeeEmailIdList.add((String) employeeDetails.get("otherApproverEmailID"));
						}

						String title = "Approved Leave (" + employeeDetails.get("leaveCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.get("employeeName") + ",<BR><BR>");
						messageContents.append("Your leave request detail as per below has been approved: <br>");
						messageContents.append("<ul><li><B>Leave Type : </B>" + employeeDetails.get("leaveType") + "</li>");
						messageContents.append("<li><B>From Date : </B>" + fromDate + "<b>&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("fromDurationType") + "</li>");
						messageContents.append("<li><B>To Date : </B>" + toDate + "<b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("toDurationType") + "</li>");
						messageContents.append("<li><B>Reason of Leave: </B>" + employeeDetails.get("leaveReason") + "</li>");
						messageContents.append("<li><B>Approved By: </B>" + currentUserName + "</li>");
						messageContents.append("<li><B>Approver Comments : </B>" + approverComment + "</li></ul>");
						messageContents.append(getFooterMessageForMail(currentUserName, organizationName));

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								messageContents.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.get("employeeEmailId"));
								Iterator itr = employeeEmailIdList.iterator();
								int i = 0;
								int size = 0;
								if (employeeDetails.has("approverEmailId")) {
									size = 1;
								}
								String[] ccEmailIds = new String[employeeEmailIdList.size() + size];
								while (itr.hasNext()) {
									ccEmailIds[i] = (String) itr.next();
									i++;
								}
								if (employeeDetails.has("approverEmailId")) {
									ccEmailIds[i] = "" + employeeDetails.get("approverEmailId");
								}
								if (ccEmailIds.length > 0) {
									mailSender.setCc(ccEmailIds);
								}
								mailSender.setReplyTo(CURRENT_USER_EMAILID);
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (Exception e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					}
				} catch (ApplaneMailException e) {
					LogUtility.writeLog("1 >> Approve Leave Mail >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
					throw new BusinessLogicException("Some unknown error occured other aprrove case" + e.getMessage());
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("2 >> Approve Leave Mail >> " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
			throw new BusinessLogicException("Error while sending mail : " + e.getMessage());
		}
	}

	private void updateCCEmployee(Object leaveRequestKey, Object hrId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.RESOURCE, "hris_leaverequests__employee_cc_id");
		query.put(Data.Query.FILTERS, "hris_leaverequests = " + leaveRequestKey + " AND employee_cc_id = " + hrId);
		JSONArray array = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_leaverequests__employee_cc_id");
		if (array == null || array.length() == 0) {
			JSONObject updateColumns = new JSONObject();
			updateColumns.put("hris_leaverequests", leaveRequestKey);
			updateColumns.put("employee_cc_id", hrId);
			JSONObject updates = new JSONObject();
			updates.put(Data.Update.UPDATES, updateColumns);
			updates.put(Data.Update.RESOURCE, "hris_leaverequests__employee_cc_id");
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
		}
	}

	private JSONArray getLeaveComments(Object key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "leavecomments");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("comment");
		columnArray.put("employeeid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "leaverequestid in (" + key + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		return employeeObject.getJSONArray("leavecomments");
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void sendLeaveSMS(String employeeMobileNo, String approverMobileNo, String employeeName, String fromDate, String toDate, Object branchId, String smsCode) {
		String message = employeeName + " has requested for leave from " + fromDate + " to " + toDate + " and SMS code is " + smsCode + ".";

		if (employeeMobileNo == null) {
			employeeMobileNo = "HRMS";
		}

		List<Object> smsList = new ArrayList<Object>();
		boolean disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);
		try {
			if (!disableSMS && approverMobileNo != null && approverMobileNo.length() > 0) {
				JSONObject smsTo = new JSONObject();
				smsTo.put(View.Action.Types.SendSMS.TO, approverMobileNo);
				smsTo.put(View.Action.Types.SendSMS.FROM, employeeMobileNo);
				smsTo.put(View.Action.Types.SendSMS.SMS, message);
				smsList.add(smsTo);
				try {
					SendSMSService.sendSMSUtility(smsList.toArray());
				} catch (IOException e) {
					throw new BusinessLogicException("Error while send SMS.. " + e.getMessage());
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error come while send SMS... " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void sendLeaveMail(JSONObject employeeDetails, List<String> employeeEmailIdList, String fromDate, String toDate) {
		try {
			Map<String, Object> commInfo = CommunicationUtility.getCommunicationInfo();
			boolean disableEmail = (Boolean) ((Map<String, Object>) commInfo.get(Communication_Constants.Type.LEAVE_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);

			String organizationLogo = (String) commInfo.get(Communication_Constants.ORGANIZATION_LOGO);
			String organizationName = (String) commInfo.get(Communication_Constants.ORGANIZATION_NAME);
			Object cmdId = commInfo.get("cmdId");
			Object cmdMailId = commInfo.get("cmdMailId");
			if (!disableEmail) {
				String title = "Request For Leave (" + employeeDetails.get("leaveCode") + ")";

				if (organizationLogo == null) {
					organizationLogo = "";
				}
				try {
					// leaveApproveByCmdMoreThen
					boolean sendMailToCmd = false;
					if (cmdId != null && employeeDetails.has("leaveApproveByCmdMoreThen") && employeeDetails.opt("leaveApproveByCmdMoreThen") != null) {
						// employeeDetails.put("fromDateObject", fromDateObject);
						// employeeDetails.put("toDateObject", toDateObject);
						long differenceBetweenDates = DataTypeUtilities.differenceBetweenDates(Translator.dateValue(employeeDetails.opt("fromDateObject")), Translator.dateValue(employeeDetails.opt("toDateObject")));
						int maxLeaves = Translator.integerValue(employeeDetails.opt("leaveApproveByCmdMoreThen"));
						if (maxLeaves < differenceBetweenDates) {
							sendMailToCmd = true;
						}
					}
					StringBuffer messageContentsHeader = new StringBuffer();
					StringBuffer messageContentsLink = new StringBuffer();
					StringBuffer messageContentsFooter = new StringBuffer();

					boolean isUser = true;
					String[] approverEmails = new String[employeeEmailIdList.size()];
					for (int counter = 0; counter < approverEmails.length; counter++) {
						approverEmails[counter] = employeeEmailIdList.get(counter);
					}

					getMessageForMailOnInsert(employeeDetails, fromDate, toDate, organizationLogo, organizationName, messageContentsHeader);

					String userContents = getLinkToCommentsByMail(employeeDetails.get("key"), employeeDetails.get("leaveCode"), isUser, employeeDetails.get("employeeId"));
					String approverContents = "";
					if (!sendMailToCmd) {
						approverContents = getLinkToCommentsByMail(employeeDetails.get("key"), employeeDetails.get("leaveCode"), false, employeeDetails.get("employeeId"));
					} else {
						approverContents = getLinkToCommentsByMail(employeeDetails.get("key"), employeeDetails.get("leaveCode"), false, cmdId);
					}
					if (!sendMailToCmd) {
						messageContentsLink.append(getLinkToApproveByMail(employeeDetails.get("key"), employeeDetails.opt("approverId")));
					} else {
						messageContentsLink.append(getLinkToApproveByMail(employeeDetails.get("key"), cmdId));
					}

					messageContentsFooter.append(getFooterMessageForMail(employeeDetails.getString("employeeName"), organizationName));
					messageContentsFooter.append("<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

					String contentsForEmployee = messageContentsHeader.toString() + userContents + messageContentsFooter.toString();

					String contentsForApprover = "";
					contentsForApprover = messageContentsHeader.toString() + approverContents;
					if (!sendMailToCmd) {
						contentsForApprover += messageContentsLink.toString();
					}
					contentsForApprover += messageContentsFooter.toString();

					String contentsForCmd = messageContentsHeader.toString() + approverContents + messageContentsLink.toString() + messageContentsFooter.toString();

					contentsForCmd = messageContentsHeader.toString() + approverContents;
					if (sendMailToCmd) {
						contentsForCmd += messageContentsLink.toString();
					}
					contentsForCmd += messageContentsFooter.toString();

					HrisHelper.sendMails(new String[] { employeeDetails.getString("employeeEmailId") }, contentsForEmployee, title);
					PunchingUtility.sendMailsAccToProfitCenters(new String[] { employeeDetails.getString("approverEmailId") }, contentsForApprover, title, approverEmails);
					if (sendMailToCmd) {
						HrisHelper.sendMails(new String[] { "" + cmdMailId }, contentsForCmd, title);
					}

				} catch (Exception e) {
					throw e;
				}
			}
		} catch (Exception e1) {
			throw new RuntimeException("Some unknown error occured while send mail " + e1.getMessage());
		}
	}

	private String getLinkToCommentsByMail(Object key, Object approverId, boolean isUser, Object employeeId) throws JSONException {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		// messageContents.append("Approve or Reject leave(s):").append("<BR />");
		// messageContents.append("<form action='http://apps.applane.com/escape/approveleave'>");
		messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveleave'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		if (isUser) {
			messageContents.append("<INPUT TYPE='hidden' name='empid' VALUE='").append(employeeId).append("' />");
		}
		messageContents.append("<table border='0' width='98%'> <tr><td  align='left' style=\"vertical-align: top;\" width='20%'>");
		messageContents.append("<b>Comments:</b>");
		messageContents.append("</td><td align='left' style=\"vertical-align: top;\" width='60%'>");
		messageContents.append("<INPUT TYPE='text' name='comments' style=\"width:100%;\"").append("' /></td>");
		messageContents.append("<td  align='left' style=\"vertical-align: top;\" width='20%'><input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Save Comment(s)  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("");
		messageContents.append("</td></tr></table></form></ BR>");

		// messageContents.append("<a href='http://apps.applane.com/escape/approveleave?lr=").append(key).append("&orn=").append(currentOrganization).append("&apid=").append(approverId).append("'>").append("http://apps.applane.com/Approve_Leaves").append("</a>");
		messageContents.append("<BR />");
		return messageContents.toString();
	}

	private String getLinkToApproveByMail(Object key, Object approverId) throws JSONException {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		// messageContents.append("Approve or Reject leave(s):").append("<BR />");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approveleave'>");
		messageContents.append("<table border='0' width='98%' align='center'> <tr valign='bottom'><td align='left' width='100%'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approveleave'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");
		messageContents.append("<b>Comments:</b>");
		messageContents.append("<INPUT TYPE='text' name='comments' style=\"width:80%;\"").append("' /></ br>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td></tr>");
		messageContents.append("<tr><td align='left'>");

		messageContents.append("<form action='http://apps.applane.com/escape/approveleave'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");
		messageContents.append("<b>Comments:</b>");
		messageContents.append("<INPUT TYPE='text' name='comments' style=\"width:80%;\"").append("' /></ br>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr>");
		messageContents.append("</table>");

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

	private String getFooterMessageForMail(String employeeName, String organizationName) {
		StringBuffer messageContents = new StringBuffer();
		messageContents.append("Regards, <BR>");
		messageContents.append(employeeName + "<BR>");
		messageContents.append("(" + organizationName + ")");
		return messageContents.toString();
	}

	private void getMessageForMailOnInsert(JSONObject employeeDetails, String fromDate, String toDate, String organizationLogo, String organizationName, StringBuffer messageContents) throws Exception {

		messageContents.append("<img src=\"" + organizationLogo + "\" alt=\"Organization Logo\" />");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
		messageContents.append("Dear Sir / Madam,<BR><BR>");
		messageContents.append(employeeDetails.get("gender")).append(employeeDetails.get("employeeName")).append("(Employee Code: ").append(employeeDetails.get("employeeCode")).append(") working in ").append(employeeDetails.get("branchName")).append(" has requested for leave(s). Details as given below: <BR />");
		messageContents.append("<ul><li><B>Leave Type : </B>" + employeeDetails.get("leaveType") + "</li>");
		messageContents.append("<li><B>From Date : </B>" + fromDate + "<b>&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("fromDurationType") + "</li>");
		messageContents.append("<li><B>To Date : </B>" + toDate + "<b>&nbsp;&nbsp;&nbsp;&nbsp;Duration Type : </b>" + employeeDetails.get("toDurationType") + "</li>");
		messageContents.append("<li><B>Reason : </B>" + employeeDetails.get("leaveReason") + "</li></ul>");
		if (employeeDetails.has("substituteName")) {
			messageContents.append(employeeDetails.get("substituteGender")).append(employeeDetails.get("substituteName")).append("would be ").append(employeeDetails.get("gender1")).append(" substitute during leave period.");
		}
		JSONArray leaveCommentsArray = getLeaveComments(employeeDetails.opt("key"));
		if (leaveCommentsArray != null & leaveCommentsArray.length() > 0) {
			messageContents.append("<BR /> Leave Comments");
			messageContents.append("<table border='1' width='50%'>");
			messageContents.append("<tr><td width='25%'>Employee Name</td><td width='75%'>Comments</td></tr>");
			for (int counter = 0; counter < leaveCommentsArray.length(); counter++) {
				messageContents.append("<tr><td>").append(leaveCommentsArray.getJSONObject(counter).opt("employeeid.name")).append("</td>");
				messageContents.append("<td>").append(leaveCommentsArray.getJSONObject(counter).opt("comment")).append("</td></tr>");
			}
			messageContents.append("</table><BR /><BR />");
		}

		messageContents.append("<BR />").append(employeeDetails.get("gender1")).append(" Previous Leave details are shown below: <BR /><BR />");
		JSONArray lastThreeDatesList = new JSONArray();
		JSONArray leaveTakenInLestThreeMonths = new LeaveRequestUtility().getLeaveTakenInLestThreMonths(employeeDetails.get("employeeId"), employeeDetails.get("requestDate"), lastThreeDatesList);

		if (lastThreeDatesList != null && lastThreeDatesList.length() > 0) {
			messageContents.append("Last Three Leaves Taken Dates").append("Leave Taken<BR />");
			for (int counter = 0; counter < lastThreeDatesList.length(); counter++) {
				messageContents.append(lastThreeDatesList.opt(counter)).append("<BR />");
			}
			messageContents.append("<BR /><BR />");
		}
		messageContents.append("Month &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append("Leave Taken<BR />");
		if (leaveTakenInLestThreeMonths == null || leaveTakenInLestThreeMonths.length() == 0) {
			messageContents.append("No Leaves");
		}

		for (int counter = 0; counter < leaveTakenInLestThreeMonths.length(); counter++) {
			Object monthId = leaveTakenInLestThreeMonths.getJSONObject(counter).opt("monthid");
			Object monthName = "";
			if (monthId != null && monthId instanceof JSONObject) {
				monthName = ((JSONObject) monthId).opt("name");
			}
			Object taken_leaves = leaveTakenInLestThreeMonths.getJSONObject(counter).opt("taken_leaves");
			if (Translator.doubleValue(taken_leaves) > 0.0) {
				messageContents.append("<b>").append(monthName).append(" </b>").append("&nbsp;&nbsp;&nbsp;").append(taken_leaves).append("<BR />");
			}
		}
	}

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

	}
}
