package com.applane.resourceoriented.hris.tour;

import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.hris.SMSUtilities;
import com.applane.moduleimpl.SystemParameters;
import com.applane.moduleimpl.shared.constants.ModuleConstants;
import com.applane.resourceoriented.hris.utility.HRISConstants;

public class TourRequestApprovalBySMS {
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
			columns.put("tourstatusid");
			columns.put("tourstatusid.name");

			// retrieve value for setting session for excuting further queries
			columns.put("employeeid");
			columns.put("employeeid.officialemailid");
			columns.put("_organizationid_");
			columns.put("_organizationid_.organization");

			leaveQuery.put(Data.Query.COLUMNS, columns);
			leaveQuery.put(Data.Query.FILTERS, "smscode = '" + smsCode + "'");

			JSONArray ignoreModules = new JSONArray();
			ignoreModules.put(ModuleConstants.USER_PERMISSION);
			leaveQuery.put(Data.Query.IGNORE_MODULES, ignoreModules);

			JSONObject tourObject;
			tourObject = ResourceEngine.query(leaveQuery);
			JSONArray tourRequestArray = tourObject.getJSONArray(resourceName);
			int tourRequestArrayCount = (tourRequestArray == null || tourRequestArray.length() == 0) ? 0 : tourRequestArray.length();
			if (tourRequestArrayCount > 0) {
				// Object employeeId = tourRequestArray.getJSONObject(0).opt("employeeid");
				// Object employeeEmailId = tourRequestArray.getJSONObject(0).opt("employeeid.officialemailid");
				Object approverId = tourRequestArray.getJSONObject(0).opt("approverid");
				Object approverEmailId = tourRequestArray.getJSONObject(0).opt("approverid.officialemailid");
				Object orgId = tourRequestArray.getJSONObject(0).opt("_organizationid_");
				Object orgName = tourRequestArray.getJSONObject(0).opt("_organizationid_.organization");
				Object tourRequestId = tourRequestArray.getJSONObject(0).opt("__key__");
				int tourStatusId = tourRequestArray.getJSONObject(0).optInt("tourstatusid");
				// String employeeMobileNo = tourRequestArray.getJSONObject(0).optString("employeeid.mobileno");
				String approverMobileNo = tourRequestArray.getJSONObject(0).optString("approverid.mobileno");
				String tourStatusName = tourRequestArray.getJSONObject(0).optString("tourstatusid.name");
				String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
				// If other approver want to approve the leave request
				if (approverMobileNo.equalsIgnoreCase(senderMobileNo)) {
					// set value in session
					SMSUtilities.setValueInSession(approverId, approverEmailId, orgId, orgName);
					if (action != null && action.equalsIgnoreCase("APR")) {
						if (tourStatusId == HRISConstants.TOUR_APPROVED) {
							SMSUtilities.sendMessage(senderMobileNo, "Tour request already approved.");
						} else {
							if (tourStatusId == HRISConstants.TOUR_NEW || tourStatusId == HRISConstants.TOUR_REJECTED) {
								JSONObject updates = new JSONObject();
								updates.put(Data.Query.RESOURCE, "hris_tourrequests");
								JSONObject row = new JSONObject();
								row.put("__key__", tourRequestId);
								row.put("tourstatusid", 2);
								row.put("comment", "Approved");
								row.put("approveddate", TODAY_DATE);
								row.put("approvedbyid", approverId);
								updates.put(Data.Update.UPDATES, row);
								ResourceEngine.update(updates);
							} else {
								SMSUtilities.sendMessage(senderMobileNo, tourStatusName + " tour request can not be approved.");
							}
						}
					} else if (action != null && action.equalsIgnoreCase("REJ")) {
						if (tourStatusId == HRISConstants.LEAVE_REJECTED) {
							SMSUtilities.sendMessage(senderMobileNo, "Tour request already rejected.");
						} else {
							if (tourStatusId == HRISConstants.TOUR_NEW) {
								JSONObject updates = new JSONObject();
								updates.put(Data.Query.RESOURCE, "hris_tourrequests");
								JSONObject row = new JSONObject();
								row.put("__key__", tourRequestId);
								row.put("tourstatusid", 3);
								row.put("comment", "Rejected");
								row.put("approveddate", TODAY_DATE);
								row.put("approvedbyid", approverId);
								updates.put(Data.Update.UPDATES, row);
								ResourceEngine.update(updates);
							} else {
								// send error mesg status can not be rejected
								SMSUtilities.sendMessage(senderMobileNo, tourStatusName + " tour request can not be rejected.");
							}
						}
					} else {
						// send error mesg invalid action performed
						SMSUtilities.sendMessage(senderMobileNo, "Invalid action performed.");
					}
				} else {
					// send error mesg you are not authorized to act on this request
					SMSUtilities.sendMessage(senderMobileNo, "You are not authorized to update on this request.");
				}
			} else {
				SMSUtilities.sendMessage(senderMobileNo, "Invalid SMS Code." + smsCode);
			}
		} catch (Exception e) {
			// send error mesg Unable to update request at this moment plz try after sometime
			SMSUtilities.sendMessage(senderMobileNo, "Unable to update request at this moment please try after sometime.");
		}
	}
}
