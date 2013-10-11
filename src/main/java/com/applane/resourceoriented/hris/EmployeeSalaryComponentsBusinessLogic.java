package com.applane.resourceoriented.hris;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.hris.HrisKinds;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeSalaryComponentsBusinessLogic implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		String operationType = record.getOperationType();
		try {
			if (record.has("duemonthid") && !record.has("applicablefrom") && !record.has("applicableto")) {
				return;
			}
			Object temp = record.getValue("temp");
			Object employeeId = record.getValue("employeeid");
			if (employeeId == null) {
				employeeId = record.getValue("employeeid.__key__");
			}
			Object salaryComponentId = record.getValue("salarycomponentid");
			if (record.has("amount")) {

				int paymentTypeId = Translator.integerValue(record.getValue("salarycomponentid.paymenttypeid"));
				if (!record.has("salarycomponentid.paymenttypeid")) {
					JSONArray salaryComponentsArray = getSalaryComponentsArray(salaryComponentId);
					if (salaryComponentsArray != null && salaryComponentsArray.length() > 0) {
						paymentTypeId = Translator.integerValue(salaryComponentsArray.getJSONObject(0).opt("paymenttypeid"));
					}
				}
				if (paymentTypeId != 0 && paymentTypeId == HRISApplicationConstants.PAYABLE) {
					record.addUpdate("payable_amount", record.getValue("amount"));
				} else if (paymentTypeId != 0 && paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
					record.addUpdate("deductable_amount", record.getValue("amount"));
				}
			}
			int salaryComponentCriteriaId = Translator.integerValue(record.getValue("salarycomponentid.componentcriteriaid"));
			Object applicableFromObject = record.getValue("applicablefrom");
			Object applicableToObject = record.getValue("applicableto");
			Date applicableToDate = DataTypeUtilities.checkDateFormat(applicableToObject);
			Date applicableFromDate = DataTypeUtilities.checkDateFormat(applicableFromObject);
			Date joiningDate = null;
			if (temp == null && salaryComponentCriteriaId != HRISApplicationConstants.FIXED_BASED) {
				JSONArray rows = getJoiningDateOFEmployee(employeeId);
				if (rows != null && rows.length() > 0 && rows.getJSONObject(0).opt("joiningdate") != null) {
					joiningDate = Translator.dateValue(rows.getJSONObject(0).opt("joiningdate"));
				}
				if (joiningDate != null && joiningDate.after(applicableFromDate)) {
					throw new BusinessLogicException("Applicable From date should be greater or equal to joining date for not fixed type components, joining date: (" + rows.getJSONObject(0).opt("joiningdate") + ")");
				}
			}

			if (temp != null) {
				record.removeUpdate("temp");
			}
			if (temp == null && applicableFromDate == null || applicableToDate == null) {
				throw new BusinessLogicException("Applicable From date and Applicable To date is compulsory.");
			}
			if (temp == null) {
				if (operationType.equals(Updates.Types.INSERT)) {
					validationForDueMonth(record);// pending
				}
			}
			// Applicable Date Restriction
			// if (operationType.equals(Updates.Types.UPDATE) && record.has("applicablefrom")) {
			// Object applicableFromDatObjectNew = record.getNewValue("applicablefrom");
			// Object applicableFromDatObjectOld = record.getOldValue("applicablefrom");
			// Date applicableFromDatNew = Translator.dateValue(applicableFromDatObjectNew);
			// Date applicableFromDatOld = Translator.dateValue(applicableFromDatObjectOld);
			// applicableFromDatOld = DataTypeUtilities.getMonthLastDate(applicableFromDatOld);
			//
			// if (applicableFromDatNew.after(applicableFromDatOld)) {
			// int counter = 0;
			// Calendar cal = Calendar.getInstance();
			// for (;; counter++) {
			// cal.setTime(applicableFromDatOld);
			// cal.add(Calendar.MONTH, counter);
			// Date date = cal.getTime();
			// if (date.before(applicableFromDatNew)) {
			// Object key = record.getKey();
			// int monthId = cal.get(Calendar.MONTH) + 1;
			// long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
			// boolean isComponentExistingInSalary = isComponentExistingInSalary(employeeId, key, monthId, yearId);
			// if (isComponentExistingInSalary) {
			// throw new BusinessLogicException("You can not change applicable date because this changing period used in salary.");
			// }
			// }
			// }
			// }
			// }
			// if (operationType.equals(Updates.Types.UPDATE) && record.has("applicableto")) {
			// Object applicableToDateObjectNew = record.getNewValue("applicableto");
			// Object applicableToDateObjectOld = record.getOldValue("applicableto");
			// Date applicableToDateNew = Translator.dateValue(applicableToDateObjectNew);
			// Date applicableToDateOld = Translator.dateValue(applicableToDateObjectOld);
			// applicableToDateOld = DataTypeUtilities.getMonthLastDate(applicableToDateOld);
			//
			// if (applicableToDateNew.before(applicableToDateOld)) {
			// int counter = 0;
			// Calendar cal = Calendar.getInstance();
			// for (;; counter++) {
			// cal.setTime(applicableToDateNew);
			// cal.add(Calendar.MONTH, counter);
			// Date date = cal.getTime();
			// if (date.before(applicableToDateOld)) {
			// Object key = record.getKey();
			// int monthId = cal.get(Calendar.MONTH) + 1;
			// long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
			// boolean isComponentExistingInSalary = isComponentExistingInSalary(employeeId, key, monthId, yearId);
			// if (isComponentExistingInSalary) {
			// throw new BusinessLogicException("You can not change applicable date because this changing period used in salary.");
			// }
			// }
			// }
			// }
			// }

			// months = DataTypeUtilities.monthsBetweenDates(applicableFromDate, applicableToDate);
			// cal = Calendar.getInstance();
			// for (int counter = 0; counter < months; counter++) {
			// cal.setTime(applicableFromDate);
			// cal.add(Calendar.MONTH, counter);
			// int monthId = cal.get(Calendar.MONTH) + 1;
			// long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
			// if (branchId != null) {
			// boolean isFreezed = PopulateEmployeeSalaryBusinessLogic.getIsFreezedSalary(branchId, monthId, yearId, HRISApplicationConstants.Freez.SALARY_COMPONENTS);
			// if (isFreezed) {
			// throw new BusinessLogicException("Salary Components Freezed for Month \"" + EmployeeReleasingSalaryServlet.getMonthName(monthId) + "\", Please contact Admin Department");
			// }
			// }
			// }

			if (applicableToDate.before(applicableFromDate) || applicableToDate.equals(applicableFromDate)) {
				throw new BusinessLogicException("Applicable To date must be greater than Applicable From date.");
			}
			if (temp == null) {
				Object key = null;
				if (operationType.equals(Updates.Types.UPDATE)) {
					key = record.getKey();
				}
				JSONArray componentAlreadyExistArray = EmployeeNameBusinessLogic.getSalaryComponentsToSkipInsert(key, employeeId, salaryComponentId, "" + applicableFromObject, "" + applicableToObject);
				if (componentAlreadyExistArray != null && componentAlreadyExistArray.length() > 0) {
					String fromDate = DataTypeUtilities.stringValue(componentAlreadyExistArray.getJSONObject(0).opt("applicablefrom"));
					String toDate = DataTypeUtilities.stringValue(componentAlreadyExistArray.getJSONObject(0).opt("applicableto"));
					String componentName = DataTypeUtilities.stringValue(componentAlreadyExistArray.getJSONObject(0).opt("salarycomponentid.name"));
					throw new BusinessLogicException(componentName + " Component Already Exist in Date range with From Date: " + fromDate + " and To Date: " + toDate);
				}
			}
			if (temp == null && isComponentBasicType(salaryComponentId)) {
				Date incrementDate = applicableFromDate;
				Object monthId = null;
				Object yearId = null;
				String monthName = DataTypeUtilities.getCurrentMonth(incrementDate);
				String yearName = DataTypeUtilities.getCurrentYear(incrementDate);
				monthId = EmployeeNameBusinessLogic.getMonthId(monthName);
				yearId = EmployeeNameBusinessLogic.getYearId(yearName);

				new EmployeeNameBusinessLogic().insertRecordInIncrementHistory(employeeId, applicableFromDate, monthId, yearId);
				if (operationType.equals(Updates.Types.UPDATE) && (record.has("applicablefrom") || record.has("applicableto"))) {
					sendMailToDirector(record);
				}
			}
		} catch (BusinessLogicException e) {
			String exceptionTraceMessage = ExceptionUtils.getExceptionTraceMessage(EmployeeSalaryComponentsBusinessLogic.class.getName(), e);
			LogUtility.writeLog("EmployeeSalaryComponentsBusinessLogic >> On Before Update / Insert >> " + exceptionTraceMessage);
			throw new BusinessLogicException(e.getMessage());
		} catch (Exception e) {
			String exceptionTraceMessage = ExceptionUtils.getExceptionTraceMessage(EmployeeSalaryComponentsBusinessLogic.class.getName(), e);
			LogUtility.writeLog("EmployeeSalaryComponentsBusinessLogic >> On Before Update / Insert >> " + exceptionTraceMessage);
			throw new BusinessLogicException("Some unknown error occured while applying date validations.");
		}
	}

	private boolean isComponentExistingInSalary(Object employeeId, Object key, int monthId, long yearId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		columns.put("__key__");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND employeesalarycomponentid=" + key + " AND salarymonthid = " + monthId + " AND salaryyearid = " + yearId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlysalarycomponents");
		if (rows != null && rows.length() > 0) {
			return true;
		}
		return false;
	}

	private JSONArray getSalaryComponentsArray(Object salaryComponentId) throws Exception {

		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		columns.put("paymenttypeid");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + salaryComponentId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("salarycomponents");
		return rows;
	}

	private void validationForDueMonth(Record record) {
		Object applicableFromObject = record.getValue("applicablefrom");
		Object applicableToObject = record.getValue("applicableto");
		Object dueMonthId = record.getValue("duemonthid");
		if (dueMonthId == null) {
			throw new BusinessLogicException("Due Month Can Not Be Null.");
		}
		Calendar cal = Calendar.getInstance();
		List<Integer> dueMonthList = new ArrayList<Integer>();
		int counter = 0;
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(Translator.dateValue(applicableToObject));

		for (;;) {

			cal.setTime(Translator.dateValue(applicableFromObject));
			cal.add(Calendar.MONTH, counter);
			Date date = cal.getTime();
			if (date.before(monthLastDate)) {
				int monthId = cal.get(Calendar.MONTH) + 1;
				if (!dueMonthList.contains(monthId)) {
					dueMonthList.add(monthId);
				}
			} else {
				break;
			}
			counter++;
		}
		if (!dueMonthList.contains(Translator.integerValue(dueMonthId))) {
			throw new BusinessLogicException("Please Add a Valid Due Month Between From Date And To Date.");
		}
	}

	private void sendMailToDirector(Record record) throws Exception {
		Object employeeId = record.getValue("employeeid");
		Object employeeName = record.getValue("employeeid.name");
		StringBuffer messageContents = new StringBuffer();

		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();
		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);
		messageContents.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
		JSONArray directorArray = getDirectorArray(employeeId);
		String[] directorEmails = getDirectorEmails(directorArray);
		Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
		if (approverIdArray != null && approverIdArray.length() > 0) {
			messageContents.append("Dear Sir / Mam").append("<BR /><BR />");
			messageContents.append("Increment Due For <b>").append(employeeName).append("</b> but ");
			messageContents.append("<b>").append(approverIdArray.getJSONObject(0).opt("name")).append("</b>").append(" has Changed due dates details for <b>").append(record.getValue("salarycomponentid.name")).append("</b> given below:").append("<BR />");
			if (record.has("applicablefrom")) {
				messageContents.append("<b>Applicable From Date: </b>").append(record.getOldValue("applicablefrom")).append("  to  ").append(record.getNewValue("applicablefrom")).append("<BR />");
			}
			if (record.has("applicableto")) {
				messageContents.append("<b>Applicable Upto Date: </b>").append(record.getOldValue("applicableto")).append("  to  ").append(record.getNewValue("applicableto")).append(" <BR />");
			}
			messageContents.append("<BR />");
			messageContents.append("Regards <BR /> Applane Team<BR />");
			messageContents.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
			String title = "Increment Due But Component's Date Changed";
			HrisHelper.sendMails(directorEmails, messageContents.toString(), title);
		}
	}

	private JSONArray getApproverIdArray(Object currentUserEmail) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		columnArray.put(Updates.KEY);
		columnArray.put("name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + "='" + currentUserEmail + "'");
		JSONArray employeeArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		return employeeArray;
	}

	private String[] getDirectorEmails(JSONArray directorArray) throws JSONException {
		List<String> list = new ArrayList<String>();
		if (directorArray != null && directorArray.length() > 0) {
			Object profitCentersObject = directorArray.getJSONObject(0).opt("profitcenterid");
			JSONArray profitCenterArray = new JSONArray();
			if (profitCentersObject instanceof JSONArray) {
				profitCenterArray = (JSONArray) profitCentersObject;
			} else if (profitCentersObject instanceof JSONObject) {
				profitCenterArray.put((JSONObject) profitCentersObject);
			}
			if (profitCenterArray != null && profitCenterArray.length() > 0) {
				for (int counter = 0; counter < profitCenterArray.length(); counter++) {
					Object directorEmailObject = profitCenterArray.getJSONObject(counter).opt("director_id.officialemailid");
					if (directorEmailObject != null) {
						list.add("" + directorEmailObject);
					}
				}
			}
		}
		String[] emails = null;
		if (list.size() > 0) {
			emails = new String[list.size()];
			for (int counter = 0; counter < list.size(); counter++) {
				emails[counter] = list.get(counter);
			}
		}
		return emails == null ? new String[] { ApplicationConstants.OWNER } : emails;
	}

	private JSONArray getDirectorArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		// columnArray.put("profitcenterid.director_id");
		// columnArray.put("profitcenterid.director_id.officialemailid");

		JSONArray childColumns = new JSONArray();
		childColumns.put("director_id");
		childColumns.put("director_id.officialemailid");
		childColumns.put("director_id.name");

		JSONObject directorIdColumnArray = new JSONObject();
		directorIdColumnArray.put("profitcenterid", childColumns);

		columnArray.put(directorIdColumnArray);

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + "=" + employeeId);
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);

		return directorIdArray;
	}

	public static JSONArray getJoiningDateOFEmployee(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_employees");
		columns.put("joiningdate");
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + employeeId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees");
		return rows;
	}

	private boolean isComponentBasicType(Object salaryComponentId) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		columns.put(SalaryComponentKinds.COMPONENT_TYPE_ID);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + salaryComponentId);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("salarycomponents");
		if (rows != null && rows.length() > 0) {
			int ignoreAble = Translator.integerValue(rows.getJSONObject(0).opt(SalaryComponentKinds.COMPONENT_TYPE_ID));
			if (ignoreAble == HRISApplicationConstants.SalaryComponentTypes.BASIC) {
				return true;
			}
		}
		return false;
	}

	public void onAfterUpdate(Record record) {

	}

	public void deleteSelecetdComponents(Object[] selectedComponentsKeys) throws Exception {
		LogUtility.writeLog("selectedComponentsKeys >> " + selectedComponentsKeys);
		if (selectedComponentsKeys != null && selectedComponentsKeys.length > 0) {
			for (int counter = 0; counter < selectedComponentsKeys.length; counter++) {
				Object key = selectedComponentsKeys[counter];
				if (!componentExistInReleasedSalary(key)) {
					deleteComponent(key);
				} else {
					WarningCollector.collect("Component Used In Salary.");
				}
			}
		}
	}

	private void deleteComponent(Object key) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONObject row = new JSONObject();
		row.put("__key__", key);
		row.put("__type__", "delete");
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private boolean componentExistInReleasedSalary(Object key) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents");
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeesalarycomponentid = " + key);
		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlysalarycomponents");
		if (rows != null && rows.length() > 0) {
			return true;
		}
		return false;
	}
}
