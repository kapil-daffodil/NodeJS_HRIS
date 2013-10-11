package com.applane.resourceoriented.hris.reports;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.ScriptParser;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;

public class TemplateMailToSelectedEmployees {

	public void sendMail(Object[] selectedKeys, Object templateIdObject) {
		String keys = "";
		if (selectedKeys != null) {
			for (int counter = 0; counter < selectedKeys.length; counter++) {
				if (keys.length() > 0) {
					keys += ",";

				}
				keys += selectedKeys[counter];
			}
		}
		try {
			Object templateId = null;
			if (templateIdObject != null) {
				if (templateIdObject instanceof JSONObject) {
					templateId = ((JSONObject) templateIdObject).opt(Updates.KEY);
				} else if (templateIdObject instanceof JSONArray && ((JSONArray) templateIdObject).length() > 0) {
					templateId = ((JSONArray) templateIdObject).getJSONObject(0).opt(Updates.KEY);
				}
				if (templateId != null) {
					JSONArray templateArray = getTemplateArray(templateId);
					if (templateArray != null && templateArray.length() > 0) {
						String title = "" + templateArray.getJSONObject(0).opt(HRISApplicationConstants.TemplateMail.TITLE);
						JSONObject query = new JSONObject("" + templateArray.getJSONObject(0).opt(HRISApplicationConstants.TemplateMail.QUERY));
						JSONObject templateXSLT = new JSONObject("" + templateArray.getJSONObject(0).opt(HRISApplicationConstants.TemplateMail.TEMPLATE));
						JSONArray resultArray = getResult(query, keys);
						for (int resultCounter = 0; resultCounter < resultArray.length(); resultCounter++) {
							JSONObject result = resultArray.getJSONObject(resultCounter);
							String mailId = result.optString("" + templateArray.getJSONObject(0).opt(HRISApplicationConstants.TemplateMail.MAIL_ID_COLUMN));
							JSONObject resultToResolveHTML = new JSONObject().put(query.optString(Data.Query.RESOURCE), new JSONArray().put(result));

							try {
								String resolveHtml = "<html>";
								resolveHtml = resolveHtml.concat(ScriptParser.resolveScript(templateXSLT, resultToResolveHTML)).concat("</html>");
								HrisHelper.sendMails(new String[] { mailId }, resolveHtml, title);
							} catch (Exception e) {
								String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
								LogUtility.writeLog(mailId + " << TemplateMailToSelectedEmployees >> exception Trace >> " + trace);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("TemplateMailToSelectedEmployees >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured, Please Contact To Admin.");
		}
	}

	private JSONArray getResult(JSONObject query, String keys) throws JSONException {
		query.put(Data.Query.FILTERS, Updates.KEY + " IN(" + keys + ")");
		JSONObject resultObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		String resouce = query.optString(Data.Query.RESOURCE);
		JSONArray rows = resultObject.getJSONArray(resouce);
		return rows;
	}

	private JSONArray getTemplateArray(Object templateId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.TemplateMail.RESOURCE);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(HRISApplicationConstants.TemplateMail.NAME);
		columnArray.put(HRISApplicationConstants.TemplateMail.QUERY);
		columnArray.put(HRISApplicationConstants.TemplateMail.TEMPLATE);
		columnArray.put(HRISApplicationConstants.TemplateMail.TITLE);
		columnArray.put(HRISApplicationConstants.TemplateMail.MAIL_ID_COLUMN);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + templateId);
		JSONObject attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray templateArray = attendanceObject.getJSONArray(HRISApplicationConstants.TemplateMail.RESOURCE);
		return templateArray;
	}

}
