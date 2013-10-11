package com.applane.hris;

import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.moduleimpl.SystemParameters;
import com.applane.moduleimpl.shared.constants.ModuleConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;

public class LeaveRequestApprovalBySMS {

	public static void approveLeave(String[] messageComponents, String senderMobileNo, String resourceName) {

		try {
			// String applicationName = messageComponents[0];
			String smsCode = messageComponents[1];
			String action = messageComponents[2];

			ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
			// fire query on hris_leaverequest for get employeeid.mobileno, approverid.mobileno and statusid
			JSONObject leaveQuery = new JSONObject();
			leaveQuery.put(Data.Query.RESOURCE, resourceName);
			JSONArray columns = new JSONArray();
			columns.put("__key__");
			columns.put("employeeid.mobileno");
			columns.put("approverid");
			columns.put("approverid.officialemailid");
			columns.put("approverid.mobileno");
			columns.put("leavestatusid");
			columns.put("leavestatusid.name");
			columns.put("otherapproverid");
			columns.put("otherapproverid.mobileno");
			columns.put("otherapproverid.officialemailid");

			// retrieve value for setting session for excuting further queries
			columns.put("employeeid");
			columns.put("employeeid.officialemailid");
			columns.put("_organizationid_");
			columns.put("_organizationid_.organization");

			leaveQuery.put(Data.Query.COLUMNS, columns);
			leaveQuery.put(Data.Query.FILTERS, "smscode = '" + smsCode + "'");

			// skip
			JSONArray ignoreModules = new JSONArray();
			ignoreModules.put(ModuleConstants.USER_PERMISSION);
			leaveQuery.put(Data.Query.IGNORE_MODULES, ignoreModules);

			JSONObject leaveObject;
			leaveObject = ResourceEngine.query(leaveQuery);
			JSONArray leaveRequestArray = leaveObject.getJSONArray(resourceName);
			int leaveRequestArrayCount = (leaveRequestArray == null || leaveRequestArray.length() == 0) ? 0 : leaveRequestArray.length();
			if (leaveRequestArrayCount > 0) {
				// Object employeeId = leaveRequestArray.getJSONObject(0).opt("employeeid");
				// Object employeeEmailId = leaveRequestArray.getJSONObject(0).opt("employeeid.officialemailid");
				Object approverId = leaveRequestArray.getJSONObject(0).opt("approverid");
				Object approverEmailId = leaveRequestArray.getJSONObject(0).opt("approverid.officialemailid");
				Object orgId = leaveRequestArray.getJSONObject(0).opt("_organizationid_");
				Object orgName = leaveRequestArray.getJSONObject(0).opt("_organizationid_.organization");
				Object leaveRequestId = leaveRequestArray.getJSONObject(0).opt("__key__");
				int leaveStatusId = leaveRequestArray.getJSONObject(0).optInt("leavestatusid");
				// String employeeMobileNo = leaveRequestArray.getJSONObject(0).optString("employeeid.mobileno");
				String approverMobileNo = leaveRequestArray.getJSONObject(0).optString("approverid.mobileno");
				String leaveStatusName = leaveRequestArray.getJSONObject(0).optString("leavestatusid.name");
				Object otherApproverId = leaveRequestArray.getJSONObject(0).opt("otherapproverid");
				Object otherApproverEmailId = leaveRequestArray.getJSONObject(0).opt("otherapproverid.officialemailid");
				String otherApproverMobileNo = leaveRequestArray.getJSONObject(0).optString("otherapproverid.mobileno");
				String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
				// If other approver want to approve the leave request

				if (otherApproverMobileNo.equalsIgnoreCase(senderMobileNo)) {
					// set value in session
					SMSUtilities.setValueInSession(otherApproverId, otherApproverEmailId, orgId, orgName);
					if (action != null && action.equalsIgnoreCase("APR")) {
						if (leaveStatusId == HRISConstants.LEAVE_APPROVED) {
							SMSUtilities.sendMessage(senderMobileNo, "Leave request already approved.");
						} else {
							if (leaveStatusId == HRISConstants.LEAVE_NEW || leaveStatusId == HRISConstants.LEAVE_REJECTED) {
								JSONObject updates = new JSONObject();
								updates.put(Data.Query.RESOURCE, "hris_leaverequests");
								JSONObject row = new JSONObject();
								row.put("__key__", leaveRequestId);
								row.put("leavestatusid", 2);
								row.put("approvercomment", "Approved");
								row.put("approveddate", TODAY_DATE);
								row.put("approvedbyid", otherApproverId);
								updates.put(Data.Update.UPDATES, row);
								ResourceEngine.update(updates);
							} else {
								// send error mesg status can not be approved
								SMSUtilities.sendMessage(senderMobileNo, leaveStatusName + " leave can not be approved.");
							}
						}
					} else if (action != null && action.equalsIgnoreCase("REJ")) {

						if (leaveStatusId == HRISConstants.LEAVE_REJECTED) {
							SMSUtilities.sendMessage(senderMobileNo, "Leave request already rejected.");
						} else {
							if (leaveStatusId == HRISConstants.LEAVE_NEW) {
								JSONObject updates = new JSONObject();
								updates.put(Data.Query.RESOURCE, "hris_leaverequests");
								JSONObject row = new JSONObject();
								row.put("__key__", leaveRequestId);
								row.put("leavestatusid", 3);
								row.put("approvercomment", "Rejected");
								row.put("approveddate", TODAY_DATE);
								row.put("approvedbyid", otherApproverId);
								updates.put(Data.Update.UPDATES, row);
								ResourceEngine.update(updates);
							} else {
								// send error mesg status can not be rejected
								SMSUtilities.sendMessage(senderMobileNo, leaveStatusName + " leave can not be rejected.");
							}
						}
					} else {
						// send error mesg invalid action performed
						SMSUtilities.sendMessage(senderMobileNo, "Invalid action performed.");
					}
				} else if (approverMobileNo.equalsIgnoreCase(senderMobileNo)) {

					// set value in session
					SMSUtilities.setValueInSession(approverId, approverEmailId, orgId, orgName);

					if (action != null && action.equalsIgnoreCase("APR")) {
						if (leaveStatusId == HRISConstants.LEAVE_APPROVED) {
							SMSUtilities.sendMessage(senderMobileNo, "Leave request already approved.");
						} else {
							if (leaveStatusId == HRISConstants.LEAVE_NEW || leaveStatusId == HRISConstants.LEAVE_REJECTED) {
								JSONObject updates = new JSONObject();
								updates.put(Data.Query.RESOURCE, "hris_leaverequests");
								JSONObject row = new JSONObject();
								row.put("__key__", leaveRequestId);
								row.put("leavestatusid", 2);
								row.put("approvercomment", "Approved");
								row.put("approveddate", TODAY_DATE);
								row.put("approvedbyid", approverId);
								updates.put(Data.Update.UPDATES, row);
								ResourceEngine.update(updates);
							} else {
								// send error mesg status can not be approved
								SMSUtilities.sendMessage(senderMobileNo, leaveStatusName + " leave can not be approved.");
							}
						}
					} else if (action != null && action.equalsIgnoreCase("REJ")) {
						if (leaveStatusId == HRISConstants.LEAVE_REJECTED) {
							SMSUtilities.sendMessage(senderMobileNo, "Leave request already rejected.");
						} else {
							if (leaveStatusId == HRISConstants.LEAVE_NEW) {
								JSONObject updates = new JSONObject();
								updates.put(Data.Query.RESOURCE, "hris_leaverequests");
								JSONObject row = new JSONObject();
								row.put("__key__", leaveRequestId);
								row.put("leavestatusid", 3);
								row.put("approvercomment", "Rejected");
								row.put("approveddate", TODAY_DATE);
								row.put("approvedbyid", approverId);
								updates.put(Data.Update.UPDATES, row);
								ResourceEngine.update(updates);
							} else {
								// send error mesg status can not be rejected
								SMSUtilities.sendMessage(senderMobileNo, leaveStatusName + " leave can not be rejected.");
							}
						}
					} else {
						// send error mesg invalid action performed
						SMSUtilities.sendMessage(senderMobileNo, "Invalid action performed.");
					}
				} else {
					// send error mesg you are not authorized to act on this request
					SMSUtilities.sendMessage(senderMobileNo, "You are not authorized to act on this request.");
				}
			} else {
				SMSUtilities.sendMessage(senderMobileNo, "Invalid SMS Code " + smsCode);
			}
		} catch (Exception e) {
			// send error mesg Unable to update request at this moment plz try after sometime
			LogUtility.writeLog("Some unknown error while update status" + e.getMessage());
			SMSUtilities.sendMessage(senderMobileNo, "Unable to update request at this moment please try after sometime.");
		}
	}
}
