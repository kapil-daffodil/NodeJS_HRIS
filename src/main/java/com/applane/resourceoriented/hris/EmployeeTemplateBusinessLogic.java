package com.applane.resourceoriented.hris;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.ScriptParser;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeTemplateBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			Object templateId = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.TEMPLATE_ID);
			LogUtility.writeError("templateId >> " + Translator.integerValue(templateId));
			if (Translator.integerValue(templateId) == 2) {
				sendIncrementLetter(record);
			} else {
				sendOtherLetters(record);
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeTemplateBusinessLogic After Job Exception >> " + ExceptionUtils.getExceptionTraceMessage("EmployeeTemplateBusinessLogic", e));
			throw new RuntimeException(e);
		}
	}

	private void sendIncrementLetter(Record record) {
		Object templateIdObject = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.TEMPLATE_ID);

		Object applicableFromYear = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.APPLICABLE_YEAR);
		Object effectDate = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.DATE);

		Object fixedSalaryFirst = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.FIXED_SALARY_FIRST);
		Object variableSalaryFirst = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.VARIABLE_PART_FIRST);
		Object totalSalaryFirst = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.TOTAL_SALARY_FIRST);
		Object gratuityFirst = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.GRATUITY_FIRST);
		Object healthInsuranceFirst = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.HEALTH_INSURANCE_FIRST);

		Object fixedSalarySecond = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.FIXED_SALARY_SECOND);
		Object variableSalarySecond = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.VARIABLE_PART_SECOND);
		Object totalSalarySecond = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.TOTAL_SALARY_SECOND);
		Object gratuitySecond = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.GRATUITY_SECOND);
		Object healthInsuranceSecond = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.HEALTH_INSURANCE_SECOND);

		Object selectedKeys = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.EMPLOYEE_IDS);
		String keys = "";
		if (selectedKeys != null && selectedKeys.toString().length() > 2) {
			keys = selectedKeys.toString().substring(1, selectedKeys.toString().length() - 1);
			try {
				Object templateId = null;
				if (templateIdObject != null) {
					if (templateIdObject instanceof JSONObject) {
						templateId = ((JSONObject) templateIdObject).opt(Updates.KEY);
					} else if (templateIdObject instanceof JSONArray && ((JSONArray) templateIdObject).length() > 0) {
						templateId = ((JSONArray) templateIdObject).getJSONObject(0).opt(Updates.KEY);
					} else {
						templateId = templateIdObject;
					}
					if (templateId != null) {
						JSONArray templateArray = getTemplateArray(templateId);
						if (templateArray != null && templateArray.length() > 0) {
							String title = "" + templateArray.getJSONObject(0).opt(HRISApplicationConstants.TemplateMail.TITLE);
							JSONObject query = new JSONObject("" + templateArray.getJSONObject(0).opt(HRISApplicationConstants.TemplateMail.QUERY));
							JSONObject templateXSLT = new JSONObject("" + templateArray.getJSONObject(0).opt(HRISApplicationConstants.TemplateMail.TEMPLATE));
							JSONArray resultArray = getResult(query, keys);
							double totalVariable = 0.0;
							double annualSalary = 0.0;
							boolean hasNext6Month = false;

							for (int resultCounter = 0; resultCounter < resultArray.length(); resultCounter++) {
								JSONObject result = resultArray.getJSONObject(resultCounter);
								if (fixedSalaryFirst != null) {
									annualSalary += Translator.doubleValue(fixedSalaryFirst);
									result.put("fixed_salary_first", fixedSalaryFirst);
								}
								if (fixedSalarySecond != null) {
									hasNext6Month = true;
									annualSalary += Translator.doubleValue(fixedSalarySecond);
									result.put("fixed_salary_second", fixedSalarySecond);
								}
								if (variableSalaryFirst != null) {
									totalVariable += Translator.doubleValue(variableSalaryFirst);
									annualSalary += Translator.doubleValue(variableSalaryFirst);
									result.put("variable_part_first", variableSalaryFirst);
								}
								if (variableSalarySecond != null) {
									totalVariable += Translator.doubleValue(variableSalarySecond);
									annualSalary += Translator.doubleValue(variableSalarySecond);
									result.put("variable_part_second", variableSalarySecond);
								}
								if (totalSalaryFirst != null) {
									annualSalary += Translator.doubleValue(totalSalaryFirst);
									result.put("total_salary_first", totalSalaryFirst);
								}
								if (totalSalarySecond != null) {
									annualSalary += Translator.doubleValue(totalSalarySecond);
									result.put("total_salary_second", totalSalarySecond);
								}
								if (gratuityFirst != null) {
									annualSalary += Translator.doubleValue(gratuityFirst);
									result.put("gratuity_first", gratuityFirst);
								}
								if (gratuitySecond != null) {
									annualSalary += Translator.doubleValue(gratuitySecond);
									result.put("gratuity_second", gratuitySecond);
								}
								if (healthInsuranceFirst != null) {
									annualSalary += Translator.doubleValue(healthInsuranceFirst);
									result.put("health_insurance_first", healthInsuranceFirst);
								}
								if (healthInsuranceSecond != null) {
									annualSalary += Translator.doubleValue(healthInsuranceSecond);
									result.put("health_insurance_second", healthInsuranceSecond);
								}

								if (applicableFromYear != null) {
									result.put("year", applicableFromYear);
								}

								if (effectDate != null) {
									result.put("from_date", effectDate);
								}
								if (hasNext6Month) {
									result.put("annual_salary", annualSalary);
								}
								result.put("total_variable", totalVariable);
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
	}

	private void sendOtherLetters(Record record) {
		Object templateIdObject = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.TEMPLATE_ID);
		Object selectedKeys = record.getValue(HRISApplicationConstants.EmployeeMailTemplates.EMPLOYEE_IDS);
		String keys = "";
		LogUtility.writeError("templateIdObject >> " + templateIdObject);
		LogUtility.writeError("selectedKeys >> " + selectedKeys);
		if (selectedKeys != null && selectedKeys.toString().length() > 2) {
			LogUtility.writeError("selectedKeys.length >> " + selectedKeys.toString().length());
			keys = selectedKeys.toString().substring(1, selectedKeys.toString().length() - 1);
			try {
				Object templateId = null;
				if (templateIdObject != null) {
					if (templateIdObject instanceof JSONObject) {
						templateId = ((JSONObject) templateIdObject).opt(Updates.KEY);
					} else if (templateIdObject instanceof JSONArray && ((JSONArray) templateIdObject).length() > 0) {
						templateId = ((JSONArray) templateIdObject).getJSONObject(0).opt(Updates.KEY);
					} else {
						templateId = templateIdObject;
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

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {

	}

}
