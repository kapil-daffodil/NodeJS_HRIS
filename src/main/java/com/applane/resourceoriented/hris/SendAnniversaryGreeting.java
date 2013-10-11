package com.applane.resourceoriented.hris;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class SendAnniversaryGreeting implements OperationJob {
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		if (record.getOperationType().equalsIgnoreCase("Insert")) {

			Object employeeId = record.getValue("employeeid");
			String greeting = (String) record.getValue("greeting");
			try {
				JSONArray employeeArray = getOfficialEmailId(employeeId);
				String employeeEmailId = employeeArray.getJSONObject(0).optString("officialemailid");
				// String employeeMobileNo = employeeArray.getJSONObject(0).optString("mobileno");
				String employeeName = employeeArray.getJSONObject(0).optString("name");
				if (record.has("greeting_template_id")) {
					JSONArray greetingArray = getGreetingArray(record.getValue("greeting_template_id"));
					if (greetingArray != null && greetingArray.length() > 0) {
						greeting = getGreetingValue(greetingArray);
					}
				}
				if (employeeEmailId != null && employeeName != null && greeting != null) {
					sendMail(employeeEmailId, employeeName, greeting);
				} else {
					throw new BusinessLogicException("Some Value missing from Employee Name, Employee Official Email Id or Greeting can not be null");
				}
			} catch (Exception e) {
				throw new BusinessLogicException("" + e.getMessage());
			}
		}
	}

	private JSONArray getOfficialEmailId(Object employeeId) throws JSONException {
		JSONObject employeeQuery = new JSONObject();
		employeeQuery.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray employeeColumnArray = new JSONArray();
		employeeColumnArray.put("name");
		employeeColumnArray.put("officialemailid");
		employeeColumnArray.put("mobileno");
		employeeQuery.put(Data.Query.COLUMNS, employeeColumnArray);
		employeeQuery.put(Data.Query.FILTERS, "__key__= " + employeeId);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(employeeQuery);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	private String getGreetingValue(JSONArray greetingArray) throws Exception {
		Object header = greetingArray.getJSONObject(0).opt("header");
		Object footer = greetingArray.getJSONObject(0).opt("footer");
		Object image = getLogo(greetingArray.getJSONObject(0).opt("image"));
		String greeting = "";
		greeting += header;
		greeting += "<BR /><BR />";
		// height='50' width='200'
		greeting += "<img src=\"" + image + "\" alt=\"Greeting Image\"/>";
		greeting += "<BR /><BR />";
		greeting += footer;
		return greeting;
	}

	public String getLogo(Object emblem) throws JSONException {
		String pathstr = "";
		if (emblem instanceof JSONArray) {
			pathstr = ((JSONArray) emblem).getJSONObject(0).getString("url");
		} else if (emblem instanceof JSONObject) {
			pathstr = ((JSONObject) emblem).getString("url");
		} else {
			String str = "" + emblem;
			String substr = str.substring(str.indexOf("url\":"), str.length());
			substr = substr.substring(6, substr.length());
			pathstr = substr.substring(0, substr.indexOf("\""));
		}
		return pathstr;
	}

	private JSONArray getGreetingArray(Object greetingTemplateId) throws Exception {
		JSONObject employeeQuery = new JSONObject();
		employeeQuery.put(Data.Query.RESOURCE, "hris_greeting_templates");
		JSONArray employeeColumnArray = new JSONArray();
		employeeColumnArray.put("header");
		employeeColumnArray.put("footer");
		employeeColumnArray.put("image");
		employeeQuery.put(Data.Query.COLUMNS, employeeColumnArray);
		employeeQuery.put(Data.Query.FILTERS, "__key__= " + greetingTemplateId);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(employeeQuery);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_greeting_templates");
		return employeeArray;
	}

	private void sendMail(String employeeEmailId, String employeeName, String greeting) {
		String CURRENT_USER = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		ApplaneMail mailSender = new ApplaneMail();
		String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
		String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
		String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);
		if (organizationLogo == null || signature == null || organizationName == null) {
			WarningCollector.collect("Kindly configure your Communications from SETUP.");
		} else {
			String subject = "Anniversary Wishes To " + employeeName;
			StringBuffer messageContents = new StringBuffer();
			messageContents.append("<img src=\"" + organizationLogo + " \"/>");
			messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
			messageContents.append("<BR />");
			messageContents.append(greeting);
			messageContents.append("<BR /><BR />");
			messageContents.append("Regards,<BR />");
			messageContents.append(signature + "<BR />");
			messageContents.append("(" + organizationName + ")");
			try {
				mailSender.setMessage(subject, messageContents.toString(), true);
				mailSender.setTo(employeeEmailId);
				mailSender.setReplyTo(CURRENT_USER);
				mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
			} catch (ApplaneMailException e) {
				throw new BusinessLogicException("Some unknown error occured while send greeting." + e.getMessage());
			}
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
	}

}
