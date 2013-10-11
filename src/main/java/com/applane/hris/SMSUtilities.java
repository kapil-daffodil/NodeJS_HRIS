package com.applane.hris;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.applane.app.shared.constants.View;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.service.smshelper.SendSMSService;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class SMSUtilities {

	public static void setValueInSession(Object userId, Object employeeEmailId, Object organizationId, Object organizationName) throws JSONException {
		/*
		 * Object userId = 1732; Object userEmailid = "ajaypal.singh@daffodilsw.com";
		 * 
		 * Object organizationId = 2; Object organizationName = "DaffodilCRM";
		 */

		Object applicationId = 601;
		Object applicationName = "hris";

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_ID, userId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_USER_EMAIL, employeeEmailId);

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, organizationId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, organizationName);

		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID, applicationId);
		CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION, applicationName);
	}

	@SuppressWarnings("deprecation")
	public static void sendMessage(String senderMobileNo, String message) {
		try {
			List<Object> smsList = new ArrayList<Object>();
			JSONObject smsTo = new JSONObject();
			smsTo.put(View.Action.Types.SendSMS.TO, senderMobileNo);
			smsTo.put(View.Action.Types.SendSMS.FROM, HRISConstants.SMS_FROM_NAME);
			smsTo.put(View.Action.Types.SendSMS.SMS, message);
			smsList.add(smsTo);
			SendSMSService.sendSMSUtility(smsList.toArray());
		} catch (Exception e) {
			throw new BusinessLogicException("Unexpected error occured while sending error info back to user..." + e.getMessage());
		}
	}
}
