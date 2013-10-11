package com.applane.resourceoriented.hris;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.shifts.ShiftsBusinessLogic;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeNameBusinessLogicUtility implements ApplaneTask {

	public static void validationOnSubBranchId(Record record) throws Exception {
		if (record.has("subbranchid")) {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Object employeeId = record.getKey();
			Object branchId = record.getNewValue("subbranchid");
			Object oldBranchId = record.getOldValue("subbranchid");
			JSONArray subBranchHistoryArray = getSubBranchIdHistoryArray(employeeId);
			String backDate = DataTypeUtilities.getBackDate(TODAY_DATE);
			String operationType = record.getOperationType();
			if (operationType.equals(Updates.Types.INSERT)) {
				insertSubBranchIdDated(employeeId, TODAY_DATE, record.getValue("subbranchid"));
			} else {
				if (subBranchHistoryArray != null && subBranchHistoryArray.length() > 0) {
					Object key = subBranchHistoryArray.getJSONObject(0).opt(Updates.KEY);
					updateSubBranchIdDated(key, backDate);
				} else if (oldBranchId != null) {
					insertSubBranchIdDatedOldRecord(employeeId, backDate, oldBranchId);
				}
				insertSubBranchIdDated(employeeId, TODAY_DATE, branchId);
			}
		}
	}

	private static void insertSubBranchIdDatedOldRecord(Object employeeId, String backDate, Object branchId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_sub_branch_id_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("to_date", backDate);
		row.put("sub_branchid", branchId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	public static void insertSubBranchIdDated(Object employeeId, String todayDate, Object branchId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_sub_branch_id_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("from_date", todayDate);
		row.put("sub_branchid", branchId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static void updateSubBranchIdDated(Object key, String backDate) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_sub_branch_id_history");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put("to_date", backDate);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static JSONArray getSubBranchIdHistoryArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_sub_branch_id_history");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND to_date = null");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees_sub_branch_id_history");
		return employeeArray;
	}

	public static void validationOnBranchId(Record record) throws Exception {
		if (record.has("branchid")) {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Object employeeId = record.getKey();
			Object branchId = record.getNewValue("branchid");
			Object oldBranchId = record.getOldValue("branchid");
			JSONArray branchHistoryArray = getBranchIdHistoryArray(employeeId);
			String backDate = DataTypeUtilities.getBackDate(TODAY_DATE);
			String operationType = record.getOperationType();
			if (operationType.equals(Updates.Types.INSERT)) {
				insertBranchIdDated(employeeId, TODAY_DATE, record.getValue("branchid"));
			} else {
				if (branchHistoryArray != null && branchHistoryArray.length() > 0) {
					Object key = branchHistoryArray.getJSONObject(0).opt(Updates.KEY);
					updateBranchIdDated(key, backDate);
				} else if (oldBranchId != null) {
					insertBranchIdDatedOldRecord(employeeId, backDate, oldBranchId);
				}
				insertBranchIdDated(employeeId, TODAY_DATE, branchId);
			}
		}
	}

	private static void insertBranchIdDatedOldRecord(Object employeeId, String backDate, Object branchId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_branch_id_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("to_date", backDate);
		row.put("branchid", branchId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static void insertBranchIdDated(Object employeeId, String todayDate, Object branchId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_branch_id_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("from_date", todayDate);
		row.put("branchid", branchId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static void updateBranchIdDated(Object key, String backDate) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_branch_id_history");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put("to_date", backDate);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static JSONArray getBranchIdHistoryArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_branch_id_history");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND to_date = null");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees_branch_id_history");
		return employeeArray;
	}

	public static void validationOnReportingToId(Record record) throws Exception {
		if (record.has("reportingtoid")) {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Object employeeId = record.getKey();
			Object reportingToId = record.getNewValue("reportingtoid");
			Object oldReportingToId = record.getOldValue("reportingtoid");
			JSONArray reportingToHistoryArray = getReportingToHistoryArray(employeeId);
			String backDate = DataTypeUtilities.getBackDate(TODAY_DATE);
			String operationType = record.getOperationType();
			if (operationType.equals(Updates.Types.INSERT)) {
				insertReportingToDated(employeeId, TODAY_DATE, record.getValue("reportingtoid"));
			} else {
				if (reportingToHistoryArray != null && reportingToHistoryArray.length() > 0) {
					Object key = reportingToHistoryArray.getJSONObject(0).opt(Updates.KEY);
					updateReportingToDated(key, backDate);
				} else if (oldReportingToId != null) {
					insertReportingToIdDatedOldRecord(employeeId, backDate, oldReportingToId);
				}
				insertReportingToDated(employeeId, TODAY_DATE, reportingToId);
			}
		}
	}

	private static void insertReportingToIdDatedOldRecord(Object employeeId, String backDate, Object oldReportingToId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_reporting_to_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("to_date", backDate);
		row.put("reporting_to_id", oldReportingToId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static void insertReportingToDated(Object employeeId, String todayDate, Object reportingToId) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_reporting_to_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", employeeId);
		row.put("from_date", todayDate);
		row.put("reporting_to_id", reportingToId);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static void updateReportingToDated(Object key, String backDate) throws Exception {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_reporting_to_history");
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put("to_date", backDate);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static JSONArray getReportingToHistoryArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_reporting_to_history");
		JSONArray array = new JSONArray();
		array.put("__key__");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND to_date = null");
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeArray = object.getJSONArray("hris_employees_reporting_to_history");
		return employeeArray;
	}

	public static void validationOnOfficialEmailId(Record record, Object employeeId, String operationType) throws Exception {
		if (record.has(HrisKinds.Employess.OFFICIAL_EMAIL_ID)) {
			boolean officialEmailIdKey = false;
			if (operationType.equals(Updates.Types.INSERT)) {
				officialEmailIdKey = PunchingUtility.checkDuplicate("hris_employees", HrisKinds.Employess.OFFICIAL_EMAIL_ID, "" + record.getNewValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID), null);
			} else if (operationType.equals(Updates.Types.UPDATE)) {
				officialEmailIdKey = PunchingUtility.checkDuplicate("hris_employees", HrisKinds.Employess.OFFICIAL_EMAIL_ID, "" + record.getNewValue(HrisKinds.Employess.OFFICIAL_EMAIL_ID), employeeId);
			}
			if (officialEmailIdKey) {
				throw new BusinessLogicException("This Official E-Maild Id has been already assign to another employee.");
			}
		}
	}

	public static void validationOnRelievingDate(Record record, Object employeeId) {
		if (record.has("relievingdate") && record.getNewValue("relievingdate") != null) {
			JSONArray assignedAssets = null;
			try {
				assignedAssets = new EmployeeNameBusinessLogic().getAssignedAssets(employeeId);
			} catch (BusinessLogicException e) {
				throw e;
			}
			if (assignedAssets != null && assignedAssets.length() > 0) {
				throw new BusinessLogicException("You Have Assigned Some Assets To This Employee Please First Get All Assets Than Try to Releave.");
			}
			record.addUpdate("employeestatusid", HRISApplicationConstants.EMPLOYEE_INACTIVE);
		} else if (record.has("relievingdate") && record.getNewValue("relievingdate") == null) {
			record.addUpdate("employeestatusid", HRISApplicationConstants.EMPLOYEE_ACTIVE);
		}
	}

	public static void validationOnReportingTypeId(Record record, Object employeeId) throws Exception {
		if (record.has("reportingtypeid")) {
			new EmployeeNameBusinessLogic().populateSelectedSalaryComponents(record, employeeId);
		}
	}

	public static void validationOnSalaryPackageId(Record record, Object employeeId) throws Exception {
		if (record.has("packageid")) {
			populateEmployeeSalaryComponents(record, employeeId);
		}
	}

	public static void validationOnModeOfSalary(Record record) throws Exception {
		Object modeOfSalaryTransferId = record.getValue("modeofsalaryid");
		if (modeOfSalaryTransferId == null) {
			throw new BusinessLogicException("Mode Of Salary Is Mandatory Field.");
		} else if (Translator.integerValue(modeOfSalaryTransferId) == 1) {
			Object accNo = record.getValue("accountno");
			Object bankNameId = record.getValue("bankname");
			Object nameInBank = record.getValue("name_in_bank");
			if (accNo == null || bankNameId == null || nameInBank == null) {
				throw new BusinessLogicException("For \"Account Transfer\" Salary Mode \"Account Number\", \"Bank Name\", \"Name In Bank\" is Mandatory Fields.");
			}
			if (record.has("bankname") || (bankNameId != null && record.has("accountno"))) {
				JSONArray banksArray = getBanksArray(bankNameId);
				if (banksArray != null && banksArray.length() > 0) {
					Object accNumbertLength = banksArray.getJSONObject(0).opt("account_number_length");
					if (accNumbertLength != null) {
						int accLength = Translator.integerValue(accNumbertLength);
						int accNoLength = Translator.stringValue(accNo).length();
						if (accLength != accNoLength) {
							throw new BusinessLogicException("Account Number length Should Be Equal to " + accLength);
						}
					}
				}
			}
		}
	}

	private static JSONArray getBanksArray(Object bankName) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "fee_banks");
		JSONArray array = new JSONArray();
		array.put("__key__");
		array.put("account_number_length");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + bankName);
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray banksArray = object.getJSONArray("fee_banks");
		return banksArray;
	}

	public static void populateEmployeeSalaryComponents(Record record, Object employeeId) throws JSONException, Exception {
		if (record.has("packageid")) {
			Object packageid = record.getValue("packageid");
			Object fromDateObject = record.getValue("noticeperiodstartdate"); // using for component from date
			Object toDateObject = record.getValue("noticeperiodenddate"); // using for component to date
			Object dueMonthId = record.getValue("duemonthid");
			Object grossAmount = record.getValue("specialleavebalance");
			Object joiningDate = record.getValue("joiningdate");
			record.removeUpdate("packageid");
			record.removeUpdate("noticeperiodstartdate");
			record.removeUpdate("noticeperiodenddate");
			record.removeUpdate("duemonthid");
			record.removeUpdate("specialleavebalance"); // used for amount

			Date fromDate = Translator.dateValue(fromDateObject);
			Date toDate = Translator.dateValue(toDateObject);

			if (fromDateObject != null && toDateObject != null && dueMonthId != null && grossAmount != null) {

				JSONObject fields = new JSONObject();
				fields.put("packageid", packageid);
				fields.put("dueMonthId", dueMonthId);
				fields.put("grossAmount", grossAmount);
				fields.put("fromDate", fromDate);
				fields.put("toDate", toDate);
				fields.put("employeeId", employeeId);
				fields.put("fromDateObject", fromDateObject);
				fields.put("toDateObject", toDateObject);
				fields.put("joiningDate", joiningDate);
				if (fromDate.equals(toDate) || fromDate.after(toDate)) {
					throw new BusinessLogicException("To Date Should Be Greater then From Date");
				}

				ApplaneTaskService.enQueueTask("com.applane.hris.EmployeeNameBusinessLogic", QueueConstantsHRIS.POPULATE_ATTENDANCE, fields);

			} else {
				throw new BusinessLogicException("From Date, To Date, Amount and Due Month can not be NULL");
			}
		}
	}

	public static void updateOrInsertInFnF(Record record) throws Exception {
		Object employeeId = record.getKey();
		Object noticePeriodEndDate = record.getValue("noticeperiodenddate");
		if (noticePeriodEndDate != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(Translator.dateValue(noticePeriodEndDate));
			int monthId = cal.get(Calendar.MONTH);
			long yearId = EmployeeReleasingSalaryServlet.getYearId("" + cal.get(Calendar.YEAR));
			JSONArray fnf = getFnfIfAlreadyExist(employeeId, monthId, yearId);
			if (fnf == null || fnf.length() == 0) {
				insertIntoFnf(employeeId, monthId, yearId);
			}
		}
	}

	private static void insertIntoFnf(Object employeeId, int monthId, long yearId) throws Exception {
		JSONObject updateColumn = new JSONObject();
		updateColumn.put("employeeid", employeeId);
		updateColumn.put("monthid", monthId);
		updateColumn.put("yearid", yearId);
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.RESOURCE, "hris_salary_hold");
		updates.put(Data.Update.UPDATES, updateColumn);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private static JSONArray getFnfIfAlreadyExist(Object employeeId, int monthId, long yearId) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_salary_hold");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid=" + employeeId + " AND monthid=" + monthId + " AND yearid=" + yearId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_salary_hold");
	}

	public static void insertBirthdayAnniversary(Object key, Record record) throws Exception {
		JSONArray birthdayRecord = getBirthdayRecord(key);
		Object birthdayKey = null;
		Object birthdayDate = null;
		if (birthdayRecord != null && birthdayRecord.length() > 0) {
			birthdayKey = birthdayRecord.getJSONObject(0).opt(Updates.KEY);
			birthdayDate = birthdayRecord.getJSONObject(0).opt("date");
		}
		Object employeeBirthdayDate = record.getValue("dateofbirth");
		if (!("" + employeeBirthdayDate).equals("" + birthdayDate)) {
			insertOrUpdateBirthdayDate(key, birthdayKey, employeeBirthdayDate);
		}

		JSONArray anniversaryRecord = getAnniversaryRecord(key);
		Object anniversaryKey = null;
		Object anniversaryDate = null;
		if (anniversaryRecord != null && anniversaryRecord.length() > 0) {
			anniversaryKey = anniversaryRecord.getJSONObject(0).opt(Updates.KEY);
			anniversaryDate = anniversaryRecord.getJSONObject(0).opt("date");
		}
		Object employeeAnniversaryDate = record.getValue("anniversarydate");
		if (!("" + employeeAnniversaryDate).equals("" + anniversaryDate)) {
			insertOrUpdateAnniversaryDate(key, anniversaryKey, employeeAnniversaryDate);
		}
	}

	private static void insertOrUpdateAnniversaryDate(Object employeeId, Object anniversaryKey, Object employeeAnniversaryDate) throws Exception {
		JSONObject updateColumn = new JSONObject();
		if (anniversaryKey != null) {
			updateColumn.put(Updates.KEY, anniversaryKey);
		}
		updateColumn.put("employeeid", employeeId);
		updateColumn.put("date", employeeAnniversaryDate);
		updateColumn.put("type", "Anniversary");
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.RESOURCE, "hris_employees_birthday_anniversary");
		updates.put(Data.Update.UPDATES, updateColumn);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static JSONArray getAnniversaryRecord(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_birthday_anniversary");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put("date");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + employeeId + " AND type='Anniversary'");
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray birthdayArray = object.getJSONArray("hris_employees_birthday_anniversary");
		return birthdayArray;
	}

	private static void insertOrUpdateBirthdayDate(Object employeeId, Object birthdayKey, Object employeeBirthdayDate) throws Exception {

		JSONObject updateColumn = new JSONObject();
		if (birthdayKey != null) {
			updateColumn.put(Updates.KEY, birthdayKey);
		}
		updateColumn.put("employeeid", employeeId);
		updateColumn.put("date", employeeBirthdayDate);
		updateColumn.put("type", "Birthday");
		JSONObject updates = new JSONObject();
		updates.put(Data.Update.RESOURCE, "hris_employees_birthday_anniversary");
		updates.put(Data.Update.UPDATES, updateColumn);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	public static JSONArray getBirthdayRecord(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_birthday_anniversary");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put("date");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, Updates.KEY + " = " + employeeId + " AND type='Birthday'");
		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray birthdayArray = object.getJSONArray("hris_employees_birthday_anniversary");
		return birthdayArray;
	}

	public static void invokeMethodUpdateBirthdayAnniversary() throws Exception {
		updateBirthdayAnniversary(null);
	}

	public static void updateBirthdayAnniversary(Object keys) throws Exception {

		try {
			Object employeeId = keys;
			JSONArray employeeArray = getEmployeeArray(employeeId);
			if (employeeArray != null && employeeArray.length() > 0) {
				for (int counter = 0; counter < employeeArray.length(); counter++) {

					employeeId = employeeArray.getJSONObject(counter).opt(Updates.KEY);

					JSONArray birthdayRecord = getBirthdayRecord(employeeId);
					Object birthdayKey = null;
					Object birthdayDate = null;
					if (birthdayRecord != null && birthdayRecord.length() > 0) {
						birthdayKey = birthdayRecord.getJSONObject(0).opt(Updates.KEY);
						birthdayDate = birthdayRecord.getJSONObject(0).opt("date");
					}
					Object employeeBirthdayDate = employeeArray.getJSONObject(counter).opt("dateofbirth");

					if (employeeBirthdayDate != null && !("" + employeeBirthdayDate).equals("" + birthdayDate)) {
						insertOrUpdateBirthdayDate(employeeId, birthdayKey, employeeBirthdayDate);
					}

					JSONArray anniversaryRecord = getAnniversaryRecord(employeeId);
					Object anniversaryKey = null;
					Object anniversaryDate = null;
					if (anniversaryRecord != null && anniversaryRecord.length() > 0) {
						anniversaryKey = anniversaryRecord.getJSONObject(0).opt(Updates.KEY);
						anniversaryDate = anniversaryRecord.getJSONObject(0).opt("date");
					}
					Object employeeAnniversaryDate = employeeArray.getJSONObject(counter).opt("anniversarydate");
					if (employeeAnniversaryDate != null && !(("" + employeeAnniversaryDate).equals("" + anniversaryDate))) {
						insertOrUpdateAnniversaryDate(employeeId, anniversaryKey, employeeAnniversaryDate);
					}
				}
				if (employeeArray != null && employeeArray.length() == 50) {
					initiateTaskQueueAgain(employeeId);
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage("EmployeeNameBusinessLogicUtility", e);
			LogUtility.writeError("EmployeeNameBusinessLogicUtility >> invokeMethodUpdateBirthdayAnniversary >> exception trace >> " + trace);
		}
	}

	private static void initiateTaskQueueAgain(Object employeeId) throws Exception {
		JSONObject taskQueueInfo = new JSONObject();
		taskQueueInfo.put("employeeId", employeeId);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.EmployeeNameBusinessLogicUtility", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueInfo);
	}

	private static JSONArray getEmployeeArray(Object key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray array = new JSONArray();
		array.put(Updates.KEY);
		array.put("dateofbirth");
		array.put("anniversarydate");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.MAX_ROWS, 50);
		if (key != null) {
			query.put(Data.Query.FILTERS, Updates.KEY + " > " + key);
		}

		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "__key__");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		query.put(Data.Query.ORDERS, orders);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray birthdayArray = object.getJSONArray("hris_employees");
		return birthdayArray;
	}

	public void initiate(ApplaneTaskInfo taskInfo) throws DeadlineExceededException {
		try {
			JSONObject taskParameter = taskInfo.getTaskInfo();
			Object employeeId = taskParameter.opt("employeeId");
			updateBirthdayAnniversary(employeeId);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage("EmployeeNameBusinessLogicUtility", e);
			LogUtility.writeError("EmployeeNameBusinessLogicUtility >> initiate >> exception trace >> " + trace);
		}
	}

	public static void sendMailToAdminOnShiftChange(Record record) throws Exception {
		if (record.has("shiftid") && record.getValue("shiftid") != null) {
			Object key = record.getKey();
			boolean hasCab = getCabDetail(key);

			String adminEmail = !hasCab ? null : ShiftsBusinessLogic.getCabNotificationMail();
			if (adminEmail != null) {
				String employeeName = "" + record.getValue("name");
				String shiftName = "" + record.getValue("shiftid.name");
				StringBuffer sb = new StringBuffer();
				sb.append("Dear Admin,").append("<BR /><BR />");
				sb.append(shiftName).append(" shift has been assigned to ").append(employeeName).append(", please arrange the cab.").append("<BR /><BR />");
				sb.append("Regards").append("<BR />");
				sb.append("Applane Team");
			}
		}
	}

	private static boolean getCabDetail(Object key) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		JSONArray array = new JSONArray();
		array.put("cab_status");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "employeeid = " + key);
		JSONObject attendanceObject = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray rows = attendanceObject.getJSONArray("hris_employees_additional_information");
		if (rows != null && rows.length() > 0) {
			String cabStatus = rows.getJSONObject(0).optString("cab_status", null);
			if (cabStatus.equalsIgnoreCase("CAB")) {
				return true;
			}
		}
		return false;
	}
}