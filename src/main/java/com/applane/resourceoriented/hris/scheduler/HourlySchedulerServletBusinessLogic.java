package com.applane.resourceoriented.hris.scheduler;

import org.json.JSONArray;

import com.applane.assistance.Translator;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.PunchingDataServlet;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.cron.ApplaneCron;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class HourlySchedulerServletBusinessLogic implements ApplaneCron {

	@Override
	public void cronCall() throws DeadlineExceededException {
		try {
			String serverName = SystemProperty.applicationId.get();
			if (serverName.equalsIgnoreCase("applanecrmhrd")) {
				scheduler();
			}
		} catch (Exception e) {
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "HourlySchedulerServletBusinessLogic Failed");
			throw new RuntimeException();
		}
	}

	private void scheduler() throws Exception {
		String[] organizations = HRISApplicationConstants.ORGANIZATIONS;
		// Object userId = PunchingUtility.getId("up_users", "emailid", ApplicationConstants.OWNER);
		Object userId = 13652;
		for (int counter = 0; counter < organizations.length; counter++) {
			String organization = organizations[counter];
			PunchingUtility.setValueInSession(organization, userId, ApplicationConstants.OWNER);
			int hours = 3;
			if (counter == 1) {
				hours = 5;
			}
			JSONArray mailConfigurationSetup = new MorningSchdularHRIS().getMailConfigurationSetup();
			boolean notSendPunchClientMail = false;
			if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
				notSendPunchClientMail = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_punch_client_report_mail"));
			}
			if (!notSendPunchClientMail) {
				PunchingDataServlet.checkLastUpdateDurationAndSendNotification(hours);
			}
		}

	}

}
