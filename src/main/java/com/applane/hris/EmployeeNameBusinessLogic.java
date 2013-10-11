package com.applane.hris;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.afb.AFBConstants;
import com.applane.afb.user.AfbUserService;
import com.applane.afb.user.EmployeeCacheHandler;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.Server;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.utils.CoreServerUtils;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.DatabaseEngineUtility;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeMonthlyAttendance;
import com.applane.resourceoriented.hris.EmployeeNameBusinessLogicUtility;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.cachehandler.UserCacheHandler;
import com.applane.user.exception.UserNotFoundException;
import com.applane.user.service.UserService;
import com.applane.user.utility.PasswordUtility;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

public class EmployeeNameBusinessLogic implements OperationJob, ApplaneTask {
	// job define on "employeeimport" query resource and hris_employees

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		try {
			String operationType = record.getOperationType();
			String serverName = SystemProperty.applicationId.get();

			if (record.getValue("temp") != null) {
				return;
			}
			Object employeeId = record.getKey();

			EmployeeNameBusinessLogicUtility.validationOnOfficialEmailId(record, employeeId, operationType);
			int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			if (serverName.equals(Server.CRM) && currentOrganizationId != 10719) {
				EmployeeNameBusinessLogicUtility.validationOnModeOfSalary(record);
			}
			EmployeeNameBusinessLogicUtility.validationOnSalaryPackageId(record, employeeId);
			EmployeeNameBusinessLogicUtility.validationOnReportingTypeId(record, employeeId);// to assign salary components
			EmployeeNameBusinessLogicUtility.validationOnRelievingDate(record, employeeId);
			if (record.has("firstname") || record.has("lastname")) {
				String fullName = getEmployeeFullName((String) record.getValue("firstname"), (String) record.getValue("lastname"));
				record.addUpdate("name", fullName);
				if (operationType.equals(Updates.Types.INSERT)) {
					record.addUpdate("name_in_bank", fullName);
				}
				if (record.has("firstname")) {
					record.addUpdate("firstname", Translator.stringValue(record.getValue("firstname")).trim());
				}
				if (record.has("lastname")) {
					record.addUpdate("lastname", Translator.stringValue(record.getValue("lastname")).trim());
				}
			}

			if (operationType.equals(Updates.Types.INSERT)) {
				record.addUpdate("employeestatusid", HRISApplicationConstants.EMPLOYEE_ACTIVE);
				if (record.has("relationwithnomineeid.name")) {
					String relationWithNominee = Translator.stringValue(record.getValue("relationwithnomineeid.name"));
					if (relationWithNominee != null) {
						Object nomineeRelationId = PunchingUtility.getId("relationwithnominee", "name", "" + relationWithNominee);
						if (nomineeRelationId == null) {
							throw new BusinessLogicException("(BL) Relation not found in Nominee Relation table with name >> [" + record.getValue("relationwithnomineeid.name") + "]");
						} else {
							record.removeUpdate("relationwithnomineeid.name");
							record.addUpdate("relationwithnomineeid", nomineeRelationId);
						}
					}
				}

				validateLoginConditions(record);
			} else if (operationType.equals(Updates.Types.UPDATE)) {

				validateLoginConditions(record);

				Object joiningDateObject = record.getValue("joiningdate");
				Date joiningDate = null;
				if (joiningDateObject != null) {
					joiningDate = DataTypeUtilities.checkDateFormat(joiningDateObject);
				}

				if (!record.has("reportingtypeid") && record.has("incrementduedate")) {
					Object incrementDateObject = record.getValue("incrementduedate");
					Date incrementDate = null;
					Object monthId = null;
					Object yearId = null;
					if (incrementDateObject != null) {
						incrementDate = DataTypeUtilities.checkDateFormat(incrementDateObject);
						String monthName = DataTypeUtilities.getCurrentMonth(incrementDate);
						String yearName = DataTypeUtilities.getCurrentYear(incrementDate);
						monthId = getMonthId(monthName);
						yearId = getYearId(yearName);
					}
					if (incrementDate != null && joiningDate != null && monthId != null && yearId != null) {
						if (incrementDate.before(joiningDate) || incrementDate.equals(joiningDate)) {
							throw new BusinessLogicException("Increment date must be greater than joining date.");
						} else {
							insertRecordInIncrementHistory(employeeId, incrementDateObject, monthId, yearId);
						}
					}
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			LogUtility.writeError(ExceptionUtils.getExceptionTraceMessage(EmployeeNameBusinessLogic.class.getName(), e));
			throw new BusinessLogicException("Sorry for the inconvinence caused. Unable to add employee.");
		}
	}

	private void updateAdditionalInformation(Object employeeId, Object employeeCode) throws Exception {
		JSONObject updates = new JSONObject();
		JSONObject columns = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		columns.put("employeeid", employeeId);
		columns.put("employeecode", employeeCode);
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public void populateSelectedSalaryComponents(Record record, Object employeeId) throws Exception {
		Object componentId = record.getValue("reportingtypeid");
		Object fromDateObject = record.getValue("preferreddateofbirth"); // using for component from date
		Object toDateObject = record.getValue("incrementduedate"); // using for component to date
		Object dueMonthId = record.getValue("duemonthid");
		Object grossAmount = record.getValue("extraworkingdaybalance");
		record.removeUpdate("reportingtypeid");
		record.removeUpdate("preferreddateofbirth");
		record.removeUpdate("incrementduedate");
		record.removeUpdate("duemonthid");
		record.removeUpdate("extraworkingdaybalance"); // used for amount

		Date fromDate = Translator.dateValue(fromDateObject);
		Date toDate = Translator.dateValue(toDateObject);

		if (fromDateObject != null && toDateObject != null && dueMonthId != null && grossAmount != null) {
			if (fromDate.equals(toDate) || fromDate.after(toDate)) {
				throw new BusinessLogicException("To Date Should Be Greater then From Date");
			} else {
				insertEmployeeSalaryPackageComponentsDetails(null, employeeId, componentId, fromDateObject, toDateObject, grossAmount, dueMonthId, 0.0, 0.0);
			}
		} else {
			throw new BusinessLogicException("From Date, To Date, Amount and Due Month can not be NULL");
		}
	}

	public JSONArray getAssignedAssets(Object employeeId) {
		try {
			JSONObject query = new JSONObject();
			JSONArray columnArray = new JSONArray();
			query.put(Data.Query.RESOURCE, HRISApplicationConstants.EmployeeAssets.RESOURCE);
			columnArray.put(HRISApplicationConstants.EmployeeAssets.RETURN_DATE);
			columnArray.put(Updates.KEY);
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, HRISApplicationConstants.EmployeeAssets.EMPLOYEE_ID + " = " + employeeId + " and " + HRISApplicationConstants.EmployeeAssets.RETURN_DATE + " = null");
			JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.EmployeeAssets.RESOURCE);
			return rows;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("EmployeeNameBusinessLogic Exception [" + trace + "]");
			throw new BusinessLogicException("Some Unknown Error Occured While Getting Assets.");
		}
	}

	public static void validateLoginConditions(Record record) {
		boolean createLoginAccount = DatabaseEngineUtility.getBooleanValue(record.getValue((HrisKinds.Employess.CREATE_LOGIN_ACCOUNT)));
		String officialEmailid = (String) record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		if (createLoginAccount) {
			if (officialEmailid == null) {
				throw new BusinessLogicException("An valid EmailId is must for a login enable account.");
			} else if (officialEmailid != null && !CoreServerUtils.isEmailValid(officialEmailid)) {
				throw new BusinessLogicException("Please specify a valid official EmailId.");
			}
		}
	}

	public JSONArray getSalaryComponentsForPackage() {
		try {
			JSONObject query = new JSONObject();
			JSONArray columns = new JSONArray();
			query.put(Data.Query.RESOURCE, "salarycomponents");
			columns.put(Updates.KEY);
			columns.put("name");
			columns.put("rate");
			columns.put("target");
			columns.put("componentcriteriaid");
			columns.put("componentcriteriaid.name");
			columns.put("performancetypeid");
			query.put(Data.Query.COLUMNS, columns);
			JSONObject data = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
			JSONArray rows = data.getJSONArray("salarycomponents");
			JSONArray componentsArray = new JSONArray();
			for (int counter = 0; counter < rows.length(); counter++) {
				JSONObject columnValue = new JSONObject();
				JSONObject object = new JSONObject();
				columnValue.put(Updates.KEY, rows.getJSONObject(counter).opt(Updates.KEY));
				columnValue.put("name", rows.getJSONObject(counter).opt("name"));
				object.put("componentid", columnValue);
				if (Translator.integerValue(rows.getJSONObject(counter).opt("componentcriteriaid")) == HRISApplicationConstants.PERFORMANCE_BASED && Translator.integerValue(rows.getJSONObject(counter).opt("performancetypeid")) == HRISApplicationConstants.TARGET_ACHIEVED) {
					object.put("rate", rows.getJSONObject(counter).opt("rate"));
					object.put("target", rows.getJSONObject(counter).opt("target"));
					object.put("amount", (Translator.doubleValue(rows.getJSONObject(counter).opt("rate")) * Translator.doubleValue(rows.getJSONObject(counter).opt("target"))));

				}
				columnValue = new JSONObject();
				columnValue.put(Updates.KEY, rows.getJSONObject(counter).opt("componentcriteriaid"));
				columnValue.put("name", rows.getJSONObject(counter).opt("componentcriteriaid.name"));

				object.put("componentid.componentcriteriaid", columnValue);
				componentsArray.put(object);
			}
			return componentsArray;

		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public JSONArray getSalaryComponentsForPackageFromMaster() {
		try {
			JSONObject query = new JSONObject();
			JSONArray columns = new JSONArray();
			query.put(Data.Query.RESOURCE, "hris_salarypackage_components_master");
			columns.put(Updates.KEY);
			columns.put("amount");
			columns.put("percentage");
			columns.put("rate");
			columns.put("target");
			columns.put("componentid");
			columns.put("componentid.name");
			columns.put("componentid.rate");
			columns.put("componentid.target");
			columns.put("componentid.componentcriteriaid");
			columns.put("componentid.componentcriteriaid.name");
			columns.put("componentid.performancetypeid");
			query.put(Data.Query.COLUMNS, columns);
			JSONObject data = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
			JSONArray rows = data.getJSONArray("hris_salarypackage_components_master");
			if (rows == null || rows.length() == 0) {
				return getSalaryComponentsForPackage();
			}
			JSONArray componentsArray = new JSONArray();
			for (int counter = 0; counter < rows.length(); counter++) {
				JSONObject columnValue = new JSONObject();
				JSONObject object = new JSONObject();
				columnValue.put(Updates.KEY, rows.getJSONObject(counter).opt("componentid"));
				columnValue.put("name", rows.getJSONObject(counter).opt("componentid.name"));
				object.put("componentid", columnValue);
				object.put("amount", rows.getJSONObject(counter).opt("amount"));
				object.put("percentage", rows.getJSONObject(counter).opt("percentage"));
				if (Translator.integerValue(rows.getJSONObject(counter).opt("componentid.componentcriteriaid")) == HRISApplicationConstants.PERFORMANCE_BASED && Translator.integerValue(rows.getJSONObject(counter).opt("componentid.performancetypeid")) == HRISApplicationConstants.TARGET_ACHIEVED) {
					object.put("rate", rows.getJSONObject(counter).opt("rate"));
					object.put("target", rows.getJSONObject(counter).opt("target"));
					object.put("amount", (Translator.doubleValue(rows.getJSONObject(counter).opt("rate")) * Translator.doubleValue(rows.getJSONObject(counter).opt("target"))));

				}
				columnValue = new JSONObject();
				columnValue.put(Updates.KEY, rows.getJSONObject(counter).opt("componentid.componentcriteriaid"));
				columnValue.put("name", rows.getJSONObject(counter).opt("componentid.componentcriteriaid.name"));

				object.put("componentid.componentcriteriaid", columnValue);
				componentsArray.put(object);
			}
			return componentsArray;

		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public static void insertEmployeeSalaryPackageComponentsDetails(Object key, Object employeeId, Object componentId, Object fromDate, Object toDate, Object amount, Object dueMonthId, double rate, double target) throws JSONException {
		JSONObject updates = new JSONObject();
		JSONObject columns = new JSONObject();
		updates.put(Data.Query.RESOURCE, "employeesalarycomponents");
		if (key != null) {
			columns.put(Updates.KEY, key);
		}
		columns.put("temp", HRISApplicationConstants.TEMP_VALUE);
		columns.put("employeeid", employeeId);
		columns.put("salarycomponentid", componentId);
		columns.put("applicablefrom", fromDate);
		columns.put("applicableto", toDate);
		columns.put("amount", amount);
		columns.put("duemonthid", dueMonthId);
		if (rate > 0.0) {
			columns.put("rate", rate);
		}
		if (target > 0.0) {
			columns.put("target", target);
		}
		updates.put(Data.Update.UPDATES, columns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private JSONArray getPackageComponents(Object packageid) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columns = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_salarypackagecomponents");
		columns.put("__key__");
		columns.put("componentid");
		columns.put("componentid.name");
		columns.put("componentid.componentcriteriaid");
		columns.put("componentid.ignoreinexceptionreport");
		columns.put("componentid.paymenttypeid");
		columns.put("componentid.performancetypeid");
		columns.put("componentid.salarycomponenttypeid");
		columns.put("amount");
		columns.put("percentage");
		columns.put("rate");
		columns.put("target");
		columns.put("packageid.fixamount");
		columns.put("packageid.variableamount");
		query.put(Data.Query.FILTERS, "packageid = " + packageid);
		query.put(Data.Query.COLUMNS, columns);
		JSONObject data = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = data.getJSONArray("hris_salarypackagecomponents");
		return rows;
	}

	public static JSONArray getSalaryComponentsToSkipInsert(Object updateKey, Object employeeId, Object salaryComponentId, String fromDate, String toDate) throws Exception {
		String extreFilter = "";
		if (updateKey != null) {
			extreFilter = " and " + Updates.KEY + " != " + updateKey;
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("salarycomponentid.name");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponentid = " + salaryComponentId + " and employeeid = " + employeeId + " and applicablefrom <= '" + fromDate + "' and applicableto >= '" + fromDate + "'" + extreFilter);
		query.put(Data.Query.OPTION_FILTERS, "salarycomponentid = " + salaryComponentId + " and employeeid = " + employeeId + " and applicablefrom <= '" + toDate + "' and applicableto >= '" + toDate + "'" + extreFilter);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");

		String keys = "";
		for (int counter = 0; counter < rows.length(); counter++) {
			int key = Translator.integerValue(rows.getJSONObject(counter).opt("__key__"));
			if (keys.length() > 0) {
				keys += ("," + key);
			} else {
				keys += ("" + key);
			}
		}
		String filters = "";
		if (keys.length() > 0) {
			if (updateKey != null) {
				keys += ("," + updateKey);
			}
			filters += (" and " + Updates.KEY + " not in (" + keys + ")");
		} else if (updateKey != null) {
			keys += ("" + updateKey);
			filters += (" and " + Updates.KEY + " not in (" + keys + ")");

		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "salarycomponentid = " + salaryComponentId + " and employeeid = " + employeeId + " and applicablefrom >= '" + fromDate + "' and applicableto <= '" + toDate + "'" + filters);
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows1 = object.getJSONArray("employeesalarycomponents");

		for (int counter = 0; counter < rows1.length(); counter++) {
			JSONObject object1 = rows1.getJSONObject(counter);
			rows.put(object1);
		}
		// LogUtility.writeLog("rows >> " + rows);
		return rows;
	}

	public static Object getYearId(String yearName) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_years");
			JSONArray array = new JSONArray();
			array.put("__key__");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "name = '" + yearName + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_years");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				Object yearId = rows.getJSONObject(0).opt("__key__");
				return yearId;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error while retrieving Year id from year name: " + yearName);
		}
	}

	public static Object getMonthId(String monthhName) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_months");
			JSONArray array = new JSONArray();
			array.put("__key__");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "name = '" + monthhName + "'");
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_months");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				Object monthId = rows.getJSONObject(0).opt("__key__");
				return monthId;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error while retrieving month id from month name: " + monthhName + " : " + e.getMessage());
		}
	}

	private Object incrementHistoryRecord(Object employeeId, Object incrementDate, Object monthId, Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "incrementhistory ");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and monthid = " + monthId + " and yearid = " + yearId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("incrementhistory ");
		int arrayCount = (array == null || array.length() == 0) ? 0 : array.length();
		if (arrayCount > 0) {
			Object incrementHistoryId = array.getJSONObject(0).opt("__key__");
			return incrementHistoryId;
		}
		return null;
	}

	public void insertRecordInIncrementHistory(Object employeeId, Object incrementDate, Object monthId, Object yearId) throws JSONException {
		Object incrementHistoryId = incrementHistoryRecord(employeeId, incrementDate, monthId, yearId);
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "incrementhistory");
		JSONObject row = new JSONObject();
		if (incrementHistoryId != null) {
			row.put("__key__", incrementHistoryId);
		}
		row.put("employeeid", employeeId);
		row.put("previousincrementdate", incrementDate);
		row.put("monthid", monthId);
		row.put("yearid", yearId);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private String getEmployeeFullName(String firstName, String lastName) {
		firstName = (firstName == null || firstName.trim().length() == 0) ? "" : firstName.trim();
		lastName = (lastName == null || lastName.trim().length() == 0) ? "" : lastName.trim();
		String fullName = firstName.trim() + " " + lastName.trim();
		return fullName;
	}

	// private void insertEmployeeSalaryPackageComponents(Object key, Object
	// employeeId, JSONArray packageComponents, Object packageid) throws
	// JSONException {
	// for (int counter = 0; counter < packageComponents.length(); counter++) {
	// Object componentId =
	// packageComponents.getJSONObject(counter).opt("componentid");
	// Object amount = packageComponents.getJSONObject(counter).opt("amount");
	// insertEmployeeSalaryPackageComponentsDetails(componentId, amount, key,
	// employeeId, packageid);
	// }
	// }

	// private Object insertNewPackageHistory(Object employeeId, Object
	// packageid, String TODAY_DATE) throws JSONException {
	// JSONObject updates = new JSONObject();
	// JSONObject columns = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "hris_employeepackagehistory");
	// columns.put("employeeid", employeeId);
	// columns.put("packageid", packageid);
	// columns.put("status", true);
	// columns.put("fromdate", TODAY_DATE);
	// updates.put(Data.Update.UPDATES, columns);
	// JSONObject packageHistoryJSON = ResourceEngine.update(updates);
	//
	// JSONArray jsonarray =
	// packageHistoryJSON.getJSONArray("hris_employeepackagehistory");
	// packageHistoryJSON = jsonarray.getJSONObject(0).getJSONObject("__key__");
	// Object newKey = packageHistoryJSON.get("actual");
	// return newKey;
	// }
	//
	// private void updatePackageHistory(Object key, String TODAY_DATE) throws
	// JSONException, ParseException {
	// String backDate = DataTypeUtilities.getBackDate(TODAY_DATE);
	// JSONObject updates = new JSONObject();
	// JSONObject columns = new JSONObject();
	// updates.put(Data.Query.RESOURCE, "hris_employeepackagehistory");
	// columns.put("__key__", key);
	// columns.put("status", false);
	// columns.put("todate", backDate);
	// updates.put(Data.Update.UPDATES, columns);
	// ResourceEngine.update(updates);
	// }
	//
	// private JSONArray getEmployeActivePackage(Object employeeId) throws
	// JSONException {
	// JSONObject query = new JSONObject();
	// JSONArray columns = new JSONArray();
	// query.put(Data.Query.RESOURCE, "hris_employeepackagehistory");
	// columns.put("__key__");
	// query.put(Data.Query.FILTERS, "status = 1 and employeeid = " +
	// employeeId);
	// query.put(Data.Query.COLUMNS, columns);
	// JSONObject data = ResourceEngine.query(query);
	// JSONArray rows = data.getJSONArray("hris_employeepackagehistory");
	// return rows;
	// }

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		String operationType = record.getOperationType();
		// String serverName = SystemProperty.applicationId.get();
		// if (!serverName.equals(Server.CRM)) {
		// return;
		// }
		try {
			String serverName = SystemProperty.applicationId.get();
			if (operationType.equals(Updates.Types.INSERT) && serverName.equals(Server.CRM)) {
				Object employeeCode = record.getValue("employeecode");
				Object nameInBank = record.getValue("name_in_bank");
				Object fullName = record.getValue("name");
				Object key = record.getKey();
				Object category = 2;
				createVendor(fullName, employeeCode, category, key, nameInBank);
				EmployeeNameBusinessLogicUtility.insertBirthdayAnniversary(key, record);
			}
			if (serverName.equals(Server.CRM)) {
				EmployeeNameBusinessLogicUtility.validationOnReportingToId(record);
				EmployeeNameBusinessLogicUtility.validationOnBranchId(record);
				EmployeeNameBusinessLogicUtility.validationOnSubBranchId(record);
				EmployeeNameBusinessLogicUtility.sendMailToAdminOnShiftChange(record);
			}
			if (record.has("noticeperiodenddate")) {
				EmployeeNameBusinessLogicUtility.updateOrInsertInFnF(record);
			}
			// boolean sendMail
			Object employeeId = record.getKey();
			int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			if (record.has("joiningdate") && organizationId == 5) {
				EmployeeMonthlyAttendance.insertAttendanceFromJoiningToToday(employeeId, Translator.dateValue(record.getValue("joiningdate")));
			}
			if (!operationType.equalsIgnoreCase(Updates.Types.DELETE)) {
				sendMailToNewEmployee(record);
			}
			if (operationType.equalsIgnoreCase(Updates.Types.INSERT)) {
				if (record.has("employeecode")) {
					updateAdditionalInformation(employeeId, record.getValue("employeecode"));
				}
				AfbUserService.addUser(record);
			}
			if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
				if (record.has(HrisKinds.Employess.CREATE_LOGIN_ACCOUNT) || record.has(HrisKinds.Employess.BUSINESS_FUNCTION_ID) || record.has(HrisKinds.Employess.PROFIT_CENTER_ID) || record.has(HrisKinds.Employess.REPORTING_TO) || record.has(HrisKinds.Employess.EMPLOYEE_STATUS) || record.has(HrisKinds.Employess.EMPLOYEE_STATUS + ".name") || record.has(HrisKinds.Employess.WHITELISTIED_IP_ADDRESS) || record.has(HrisKinds.Employess.PROFIT_CENTER_ID_ALIAS)) {
					EmployeeCacheHandler.clearCache((String) record.getValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID));
					AfbUserService.updateUser(record);
				}
			}
			if (operationType.equalsIgnoreCase(Updates.Types.DELETE)) {
				AfbUserService.deleteUser(record);
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private static String getOrganizationLabel(Object organizationId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "up_organizations");
			JSONArray array = new JSONArray();
			array.put("label");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + organizationId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("up_organizations");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				return DataTypeUtilities.stringValue(rows.getJSONObject(0).opt("label"));
			}
			return "";
		} catch (Exception e) {
			throw new RuntimeException("Error while retrieving Organization Label: " + e.getMessage());
		}
	}

	private void sendMailToNewEmployee(Record record) {
		try {
			String officialMailId = Translator.stringValue(record.getValue("officialemailid"));
			String userName = Translator.stringValue(record.getValue("name"));
			try {
				UserCacheHandler.getUser(officialMailId);
			} catch (UserNotFoundException e) {
				JSONObject employeeDetails = UserCacheHandler.register(officialMailId, userName, null, "applane", 0, true);
				String url = UserService.getPasswordResetURL(employeeDetails);

				String[] toMailIds = new String[1];
				toMailIds[0] = officialMailId;

				StringBuilder content = new StringBuilder();

				String serverName = SystemProperty.applicationId.get();
				String title = "";
				if (serverName.equalsIgnoreCase(Server.SIS)) {
					title = "Welcome to Applane for Education";
					content.append("Dear Sir/Ma'am,");
					content.append("<br>");
					content.append("You have been given rights on Applane for Education for ").append(getOrganizationLabel(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID))).append(".");
					content.append("Please login ").append("<a href='http://afe.applane.com'>").append("here").append("</a> to use Applane for Education. In case of any query/issue please contact your IT in-charge.");
					content.append("<br>");
					content.append("<br>");
					content.append("Regards,");
					content.append("<br>");
					content.append("Team Applane!");
				} else {
					title = "User Aceess Mail";
					content.append("Dear ").append(userName).append("<br><br>");
					content.append("You have been given access in Applane for Business by ").append(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL)).append(".<br><br>");
					content.append("Please reset your password by accessing the link given below :").append("<br>");
					content.append("<a href='").append(url).append("'>").append(url).append("</a>").append("<br><br>");
					content.append("You can access the solution by this url").append("<a href='http://afb.applane.com'>").append("http://afb.applane.com").append("</a><br><br>");
					content.append("Note : If click upon the above links does not work then copy and paste this url in browser.").append("<br><br>");
					content.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
				}
				// String mailValue =
				// ".<br />You can access the solution by this url http://afb.applane.com";
				// mailValue = "<BR />  " + url;
				HrisHelper.sendMails(toMailIds, content.toString(), title);
			}
		} catch (Exception e) {
			LogUtility.writeLog("EmployeeNameBusinessLogic >> getValueToMail >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
		}
	}

	public static Object createVendor(Object vendorName, Object employeeCode, Object category, Object key, Object nameInBank) throws JSONException {
		JSONObject update = new JSONObject();
		update.put(AFBConstants.OrganizationAndContacts.ORGNAME, vendorName);
		update.put(AFBConstants.OrganizationAndContacts.EMPLOYEE_ID, key);
		update.put(AFBConstants.OrganizationAndContacts.CONTACT_NAME, employeeCode);
		if (nameInBank != null) {
			update.put("name_in_bank", nameInBank);
		} else {
			update.put("name_in_bank", vendorName);
		}
		update.put(AFBConstants.OrganizationAndContacts.CONTACT_TYPE_ID, 1);
		update.put(AFBConstants.OrganizationAndContacts.ORGANIZATION_CATEGORIES, category);
		JSONObject updateQuery = new JSONObject();
		updateQuery.put(Data.Query.RESOURCE, AFBConstants.ORGANIZATION_ORGANIZATIONANDCONTACTS);
		updateQuery.put(Data.UPDATES, update);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updateQuery).getJSONArray(AFBConstants.ORGANIZATION_ORGANIZATIONANDCONTACTS).getJSONObject(0).getJSONObject(Data.Query.KEY).get(Data.Update.Updates.Key.ACTUAL);
	}

	public static void createEmployee(JSONObject employeeDetails) throws Exception {
		Object firstName = employeeDetails.opt(HrisKinds.Employess.FIRST_NAME);
		Object officialEmailId = employeeDetails.opt(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		Object employeeCode = employeeDetails.opt(HrisKinds.Employess.EMPLOYEE_CODE);
		JSONObject updates = new JSONObject();

		updates.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		JSONObject row = new JSONObject();
		row.put(HrisKinds.Employess.FIRST_NAME, firstName);
		row.put(HrisKinds.Employess.NAME, firstName);
		row.put(HrisKinds.Employess.EMPLOYEE_STATUS, HRISApplicationConstants.EMPLOYEE_ACTIVE);
		row.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID, officialEmailId);
		row.put(HrisKinds.Employess.EMPLOYEE_CODE, employeeCode);
		row.put("temp", "temp");
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	@Override
	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		if (organizationId == 10719) {
			asssigneSalaryPackageGirnarSoft(applaneTaskInfo);
		} else {
			asssigneSalaryPackage(applaneTaskInfo);
		}
	}

	private void asssigneSalaryPackageGirnarSoft(ApplaneTaskInfo applaneTaskInfo) {

		try {
			JSONObject fields = applaneTaskInfo.getTaskInfo();
			if (fields != null) {
				Object packageId = fields.opt("packageid");
				Object dueMonthId = fields.opt("dueMonthId");
				double enteredAmount = Translator.doubleValue(fields.opt("grossAmount"));
				// Date fromDate = (Date) fields.opt("fromDate");
				// Date toDate = (Date) fields.opt("toDate");
				Object employeeId = fields.opt("employeeId");
				Object fromDateObject = fields.opt("fromDateObject");
				Object toDateObject = fields.opt("toDateObject");
				Object joiningDate = fields.opt("joiningDate");
				JSONArray packageComponents = getPackageComponents(packageId);
				double totalAmountFromPackage = 0.0;
				JSONArray overlapingList = new JSONArray();
				double totalPercentage = 0.0;
				double totalAmount = 0.0;

				double grossAmount = enteredAmount;

				double basicAmount = 0.0;
				double hraAmount = 0.0;

				double gratuityAmount = 0.0;
				double gratuityPercentage = 0.0;
				double pfEmployeeAmount = 0.0;
				double pfEmployeePercentage = 0.0;
				double pfEmployerAmount = 0.0;
				double pfEmployerPercentage = 0.0;

				double esiEmployeeAmount = 0.0;
				double esiEmployeePercentage = 0.0;
				double esiEmployerAmount = 0.0;
				double esiEmployerPercentage = 0.0;

				for (int counter = 0; counter < packageComponents.length(); counter++) {
					Object percentage = packageComponents.getJSONObject(counter).opt("percentage");
					int componentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.salarycomponenttypeid"));
					if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.GRATUITY) {
						if (packageComponents.getJSONObject(counter).opt("amount") == null) {
							gratuityPercentage += Translator.doubleValue(percentage);
						} else {
							gratuityAmount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						}

					}
					if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE) {
						if (packageComponents.getJSONObject(counter).opt("amount") == null) {
							pfEmployeePercentage = Translator.doubleValue(percentage);
						} else {
							pfEmployeeAmount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						}

					}
					if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYER) {
						if (packageComponents.getJSONObject(counter).opt("amount") == null) {
							pfEmployerPercentage = Translator.doubleValue(percentage);
						} else {
							pfEmployerAmount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						}

					}
					if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE) {
						if (packageComponents.getJSONObject(counter).opt("amount") == null) {
							esiEmployeePercentage = Translator.doubleValue(percentage);
						} else {
							esiEmployeeAmount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						}

					}
					if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYER) {
						if (packageComponents.getJSONObject(counter).opt("amount") == null) {
							esiEmployerPercentage = Translator.doubleValue(percentage);
						} else {
							esiEmployerAmount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						}

					}
				}
				if (gratuityAmount > 0) {
					grossAmount = grossAmount - gratuityAmount;
				} else if (gratuityPercentage > 0) {
					gratuityAmount = ((double) grossAmount * gratuityPercentage) / 100;
					grossAmount = grossAmount - gratuityAmount;
				}
				if (pfEmployeeAmount > 0) {
					grossAmount = grossAmount - pfEmployeeAmount;
				} else if (pfEmployeePercentage > 0) {
					pfEmployeeAmount = (((double) grossAmount * pfEmployeePercentage) / 100) / 2;
					grossAmount = grossAmount - pfEmployeeAmount;
				}
				if (pfEmployerAmount > 0) {
				} else if (pfEmployerPercentage > 0) {
					pfEmployerAmount = (((double) grossAmount * pfEmployerPercentage) / 100) / 2;
				}
				if (esiEmployeeAmount > 0) {
					grossAmount = grossAmount - esiEmployeeAmount;
				} else if (esiEmployeePercentage > 0) {
					esiEmployeeAmount = ((double) enteredAmount * esiEmployeePercentage) / 100;
					grossAmount = grossAmount - esiEmployeeAmount;
				}
				if (esiEmployerAmount > 0) {
					grossAmount = grossAmount - esiEmployerAmount;
				} else if (esiEmployerPercentage > 0) {
					esiEmployerAmount = ((double) enteredAmount * esiEmployerPercentage) / 100;
					grossAmount = grossAmount - esiEmployerAmount;
				}
				basicAmount = grossAmount / 2;
				totalAmount = gratuityAmount + pfEmployeeAmount + basicAmount + esiEmployeeAmount + esiEmployerAmount + pfEmployerAmount;

				for (int counter = 0; counter < packageComponents.length(); counter++) {
					Object salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
					Object componentName = packageComponents.getJSONObject(counter).opt("componentid.name");
					double percentage = Translator.doubleValue(packageComponents.getJSONObject(counter).opt("percentage"));
					int paymentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.paymenttypeid"));

					int componentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.salarycomponenttypeid"));
					if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.HRA) {
						if (packageComponents.getJSONObject(counter).opt("amount") == null) {
							hraAmount = ((double) basicAmount * percentage) / 100;
						} else {
							hraAmount = Translator.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						}
						totalAmount += hraAmount;
					}
					if (componentTypeId != HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYER && componentTypeId != HRISApplicationConstants.SalaryComponentTypes.BASIC && componentTypeId != HRISApplicationConstants.SalaryComponentTypes.HRA && componentTypeId != HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE && componentTypeId != HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE && componentTypeId != HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYER && packageComponents.getJSONObject(counter).opt("amount") != null) {
						totalAmount += Translator.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
					}
					if (paymentTypeId != HRISApplicationConstants.DEDUCTABLE) {
						double amount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						totalAmountFromPackage += amount;
						JSONArray salaryComponentsForSkip = getSalaryComponentsToSkipInsert(null, employeeId, salaryComponentId, "" + fromDateObject, "" + toDateObject);
						if (salaryComponentsForSkip != null && salaryComponentsForSkip.length() > 0) {
							overlapingList.put(componentName);
						}
					}
				}
				double balanceAmountForFlaxi = enteredAmount - totalAmount;
				boolean onlyForDeductable = false;
				if (overlapingList.length() == 0 && totalPercentage <= 100.0) {
					double balanceAmount = DataTypeUtilities.doubleValue(grossAmount) - totalAmountFromPackage;
					if (balanceAmount < 0.0) {
						balanceAmount = 0.0;
					}
					for (int counter = 0; counter < packageComponents.length(); counter++) {
						Object salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
						Object amount = packageComponents.getJSONObject(counter).opt("amount");
						double rate = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("rate"));
						double target = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("target"));
						int componentCriteria = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.componentcriteriaid"));
						int performanceTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.performancetypeid"));
						int componentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.salarycomponenttypeid"));

						if (componentCriteria == HRISApplicationConstants.FIXED_BASED) {
							if (joiningDate != null) {
								Date joiningDate1 = Translator.dateValue(joiningDate);
								if (joiningDate1.equals(Translator.dateValue(fromDateObject))) {
									String firstDate = EmployeeSalaryGenerationServlet.getDateInString(DataTypeUtilities.getFirstDayOfMonth(joiningDate1));
									fromDateObject = firstDate;
								}
							}
						}

						// if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
						// JSONArray salaryComponentsForSkip = getSalaryComponentsToSkipInsert(null, employeeId, salaryComponentId, "" + fromDateObject, "" + toDateObject);
						// if (salaryComponentsForSkip == null || salaryComponentsForSkip.length() == 0) {
						// if (amount == null && percentage != null) {
						// amount = (float) (balanceAmount * Translator.doubleValue(percentage)) / 100;
						// }
						// insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, amount, dueMonthId, rate, target);
						// } else {
						// overlapingList.put(componentName);
						// onlyForDeductable = true;
						// }
						// } else
						{
							if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.BASIC) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, basicAmount, dueMonthId, rate, target);
							} else if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.HRA) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, hraAmount, dueMonthId, rate, target);
							} else if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYEE) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, pfEmployeeAmount, dueMonthId, rate, target);
							} else if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.PF_EMPLOYER) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, pfEmployerAmount, dueMonthId, rate, target);
							} else if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.FLAXI) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, balanceAmountForFlaxi, dueMonthId, rate, target);
							} else if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYEE) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, esiEmployeeAmount, dueMonthId, rate, target);
							} else if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.ESI_EMPLOYER) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, esiEmployerAmount, dueMonthId, rate, target);
							} else if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.GRATUITY) {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, gratuityAmount, dueMonthId, rate, target);
							} else if (componentCriteria == HRISApplicationConstants.PERFORMANCE_BASED && performanceTypeId == HRISApplicationConstants.TARGET_ACHIEVED) {
								amount = rate * target;
							} else {
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, amount, dueMonthId, rate, target);
							}
						}
					}
				}

				if (overlapingList.length() > 0) {
					JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeId);

					String title = "";
					String mailValue = "";
					if (employeeArray != null && employeeArray.length() > 0) {
						mailValue += "Employee Name:- " + employeeArray.getJSONObject(0).opt("name");
						mailValue += "<BR />Employee Code:- " + employeeArray.getJSONObject(0).opt("employeecode");
					}
					if (onlyForDeductable) {
						mailValue += "<BR /><BR />List only for Deductable Components <BR /><BR /> ";
					}
					if (overlapingList.length() > 0) {
						title += "Component Over Laping";
						mailValue += "<BR /><BR />Component Lists: <BR /><BR /> " + overlapingList.toString();
					}
					// if (totalPercentage > 100.0) {
					// if (title.length() > 0) {
					// title += " and Total Percentage is above 100% (" + totalPercentage + ")";
					// mailValue += "<BR /><BR /> Total Percentage Should Less or Equal To 100% but you have assigned (" + totalPercentage + ")";
					// } else {
					// title += "Total Percentage is above 100% (" + totalPercentage + ")";
					// mailValue += "Total Percentage Should Less or Equal To 100% but you have assigned (" + totalPercentage + ")";
					// }
					// }

					PunchingUtility.sendMailsWithoutCC(new String[] { "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL) }, mailValue, title);
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeNameBusinessLogic.class.getName(), e);
			LogUtility.writeLog("EmployeeNameBusinessLogic Exception Trace >> " + trace);

		}

	}

	private void asssigneSalaryPackage(ApplaneTaskInfo applaneTaskInfo) {
		try {
			JSONObject fields = applaneTaskInfo.getTaskInfo();
			if (fields != null) {
				Object packageId = fields.opt("packageid");
				Object dueMonthId = fields.opt("dueMonthId");
				Object grossAmount = fields.opt("grossAmount");
				// Date fromDate = (Date) fields.opt("fromDate");
				// Date toDate = (Date) fields.opt("toDate");
				Object employeeId = fields.opt("employeeId");
				Object fromDateObject = fields.opt("fromDateObject");
				Object toDateObject = fields.opt("toDateObject");
				Object joiningDate = fields.opt("joiningDate");
				JSONArray packageComponents = getPackageComponents(packageId);
				double totalAmountFromPackage = 0.0;
				JSONArray overlapingList = new JSONArray();
				double totalPercentage = 0.0;

				for (int counter = 0; counter < packageComponents.length(); counter++) {
					Object salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
					Object componentName = packageComponents.getJSONObject(counter).opt("componentid.name");
					Object percentage = packageComponents.getJSONObject(counter).opt("percentage");
					int paymentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.paymenttypeid"));
					if (packageComponents.getJSONObject(counter).opt("amount") == null) {
						totalPercentage += Translator.doubleValue(percentage);
					}
					if (paymentTypeId != HRISApplicationConstants.DEDUCTABLE) {
						double amount = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("amount"));
						totalAmountFromPackage += amount;
						JSONArray salaryComponentsForSkip = getSalaryComponentsToSkipInsert(null, employeeId, salaryComponentId, "" + fromDateObject, "" + toDateObject);
						if (salaryComponentsForSkip != null && salaryComponentsForSkip.length() > 0) {
							overlapingList.put(componentName);
						}
					}
				}
				boolean onlyForDeductable = false;
				if (overlapingList.length() == 0 && totalPercentage <= 100.0) {
					double balanceAmount = DataTypeUtilities.doubleValue(grossAmount) - totalAmountFromPackage;
					if (balanceAmount < 0.0) {
						balanceAmount = 0.0;
					}
					for (int counter = 0; counter < packageComponents.length(); counter++) {
						Object salaryComponentId = packageComponents.getJSONObject(counter).opt("componentid");
						Object componentName = packageComponents.getJSONObject(counter).opt("componentid.name");
						Object amount = packageComponents.getJSONObject(counter).opt("amount");
						Object percentage = packageComponents.getJSONObject(counter).opt("percentage");
						double rate = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("rate"));
						double target = DataTypeUtilities.doubleValue(packageComponents.getJSONObject(counter).opt("target"));
						int paymentTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.paymenttypeid"));
						int componentCriteria = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.componentcriteriaid"));
						int performanceTypeId = DataTypeUtilities.integerValue(packageComponents.getJSONObject(counter).opt("componentid.performancetypeid"));

						if (componentCriteria == HRISApplicationConstants.FIXED_BASED) {
							if (joiningDate != null) {
								Date joiningDate1 = Translator.dateValue(joiningDate);
								if (joiningDate1.equals(Translator.dateValue(fromDateObject))) {
									String firstDate = EmployeeSalaryGenerationServlet.getDateInString(DataTypeUtilities.getFirstDayOfMonth(joiningDate1));
									fromDateObject = firstDate;
								}
							}
						}

						if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
							JSONArray salaryComponentsForSkip = getSalaryComponentsToSkipInsert(null, employeeId, salaryComponentId, "" + fromDateObject, "" + toDateObject);
							if (salaryComponentsForSkip == null || salaryComponentsForSkip.length() == 0) {
								if (amount == null && percentage != null) {
									amount = (float) (balanceAmount * Translator.doubleValue(percentage)) / 100;
								}
								insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, amount, dueMonthId, rate, target);
							} else {
								overlapingList.put(componentName);
								onlyForDeductable = true;
							}
						} else {
							if (componentCriteria == HRISApplicationConstants.PERFORMANCE_BASED && performanceTypeId == HRISApplicationConstants.TARGET_ACHIEVED) {
								amount = rate * target;
							} else if (amount == null && percentage != null) {
								amount = (float) (balanceAmount * Translator.doubleValue(percentage)) / 100;
							}
							insertEmployeeSalaryPackageComponentsDetails(null, employeeId, salaryComponentId, fromDateObject, toDateObject, amount, dueMonthId, rate, target);
						}
					}
				}

				if (overlapingList.length() > 0 || totalPercentage > 100.0) {
					JSONArray employeeArray = EmployeeSalaryGenerationServlet.getEmployeeRecordsForSalarySheetReGenerate(employeeId);

					String title = "";
					String mailValue = "";
					if (employeeArray != null && employeeArray.length() > 0) {
						mailValue += "Employee Name:- " + employeeArray.getJSONObject(0).opt("name");
						mailValue += "<BR />Employee Code:- " + employeeArray.getJSONObject(0).opt("employeecode");
					}
					if (onlyForDeductable) {
						mailValue += "<BR /><BR />List only for Deductable Components <BR /><BR /> ";
					}
					if (overlapingList.length() > 0) {
						title += "Component Over Laping";
						mailValue += "<BR /><BR />Component Lists: <BR /><BR /> " + overlapingList.toString();
					}
					if (totalPercentage > 100.0) {
						if (title.length() > 0) {
							title += " and Total Percentage is above 100% (" + totalPercentage + ")";
							mailValue += "<BR /><BR /> Total Percentage Should Less or Equal To 100% but you have assigned (" + totalPercentage + ")";
						} else {
							title += "Total Percentage is above 100% (" + totalPercentage + ")";
							mailValue += "Total Percentage Should Less or Equal To 100% but you have assigned (" + totalPercentage + ")";
						}
					}

					PunchingUtility.sendMailsWithoutCC(new String[] { "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL) }, mailValue, title);
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(EmployeeNameBusinessLogic.class.getName(), e);
			LogUtility.writeLog("EmployeeNameBusinessLogic Exception Trace >> " + trace);

		}
	}

	public static void resetPassword(Object[] selectedKeys, Object password) {
		try {
			for (int counter = 0; counter < selectedKeys.length; counter++) {
				JSONArray employeeArray = getEmployeeArray(selectedKeys[counter]);
				Object employeeOffecialEmailId = null;
				Object employeeName = null;
				if (employeeArray != null && employeeArray.length() > 0) {
					employeeOffecialEmailId = employeeArray.getJSONObject(0).opt(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
					employeeName = employeeArray.getJSONObject(0).opt(HrisKinds.Employess.NAME);
					if (employeeOffecialEmailId != null && CoreServerUtils.isEmailValid("" + employeeOffecialEmailId)) {
						Object userId = PunchingUtility.getId("up_users", "emailid", "" + employeeOffecialEmailId);
						String encryptedPassword = PasswordUtility.getEncryptedPassword("" + password);
						updatePassword(userId, encryptedPassword);
					} else {
						WarningCollector.collect("[" + employeeName + "] Please check official EmailId may be not found or not valid.");
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage("com.applane.hris.EmployeeNameBusinessLogic", e);
			throw new RuntimeException("com.applane.hris.EmployeeNameBusinessLogic (resetPassword) >> " + trace);
		}
	}

	private static void updatePassword(Object userId, String encryptedPassword) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "up_users");
		JSONObject row = new JSONObject();
		row.put("__key__", userId);
		row.put("password", encryptedPassword);
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	private static JSONArray getEmployeeArray(Object key) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
			JSONArray array = new JSONArray();
			array.put(HrisKinds.Employess.NAME);
			array.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + key);
			JSONObject object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray(HrisKinds.EMPLOYEES);
			return rows;
		} catch (Exception e) {
			throw new RuntimeException("Error while getting email id for reset password.");
		}
	}
}
