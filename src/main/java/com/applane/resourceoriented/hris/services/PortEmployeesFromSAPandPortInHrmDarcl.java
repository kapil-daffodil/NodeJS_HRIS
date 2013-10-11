package com.applane.resourceoriented.hris.services;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.scheduler.PortEmployeeFromSapToHrmDarcl;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.google.apphosting.api.DeadlineExceededException;

public class PortEmployeesFromSAPandPortInHrmDarcl implements ApplaneTask {
	public void portEmployeesFromSapToHrm(JSONArray employeeArray, int counter, boolean isInsert) throws Exception {
		JSONObject exceptions = new JSONObject();
		int count = 1;
		for (; counter < employeeArray.length(); counter++) {
			Object employeeCode = employeeArray.getJSONObject(counter).opt("EmpId");
			if (employeeCode != null && ("" + employeeCode).length() > 0) {
				Object employeeCode1 = ("" + employeeCode).substring(2);
				JSONArray alreadyExistArray = getAlreadyExistArray(employeeCode, employeeCode1);
				Object key = null;
				if (!isInsert && alreadyExistArray != null && alreadyExistArray.length() > 0) {
					key = alreadyExistArray.getJSONObject(0).opt("__key__");
				}
				if (alreadyExistArray != null && alreadyExistArray.length() > 0 && isInsert) {
					exceptions.put("" + employeeCode, "already Exist");
				} else {
					try {
						insertOrUpdateEmployee(employeeArray, counter, isInsert, key);
						exceptions.put("" + employeeCode, "Successfully");
					} catch (Exception e) {
						String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
						exceptions.put("" + employeeCode, "Exception >> " + trace);
					}
					if (count == 100) {
						if (isInsert) {
							new MorningSchdularHRIS().sendMailInCaseOfException(new RuntimeException(exceptions.toString()), "Employee Port Status (On Insert)");
						} else {
							new MorningSchdularHRIS().sendMailInCaseOfException(new RuntimeException(exceptions.toString()), "Employee Port Status (On Update)");
						}
						initiateTaskQueueAgain(employeeArray, counter, isInsert);
						break;
					} else {
						count++;
					}
				}
			}
		}
		if (count != 100 && isInsert) {
			new PortEmployeeFromSapToHrmDarcl().initiateTaskQueueAgain();
		}
	}

	private void initiateTaskQueueAgain(JSONArray employeeArray, int counter, boolean isInsert) throws Exception {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("employeeArray", employeeArray);
		taskQueueParameters.put("counter", counter);
		taskQueueParameters.put("isInsert", isInsert);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.services.PortEmployeesFromSAPandPortInHrmDarcl", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	private void insertOrUpdateEmployee(JSONArray employeeArray, int counter, boolean isInsert, Object key) throws Exception {
		Object employeeCode = employeeArray.getJSONObject(counter).opt("EmpId");
		Object fatherName = employeeArray.getJSONObject(counter).opt("FatherName");
		Object employeeName = employeeArray.getJSONObject(counter).opt("EmpName");
		Object address = employeeArray.getJSONObject(counter).opt("EmpAddress");
		Object branch = employeeArray.getJSONObject(counter).opt("PostedAtBranch");
		Object phoneNumber = employeeArray.getJSONObject(counter).opt("EmpTelNo");
		Object pfNumber = employeeArray.getJSONObject(counter).opt("ProvidentFundNo");
		Object dateOfJoining = employeeArray.getJSONObject(counter).opt("D.O.J");
		Object businessFunction = employeeArray.getJSONObject(counter).opt("Business Function");

		Object nominee = employeeArray.getJSONObject(counter).opt("Nominee");
		Object schedule = employeeArray.getJSONObject(counter).opt("Schedule");
		Object gender = employeeArray.getJSONObject(counter).opt("SEX");
		// Object bankCode = employeeArray.getJSONObject(counter).opt("BankCode");
		Object department = employeeArray.getJSONObject(counter).opt("Departments");
		Object ctc = employeeArray.getJSONObject(counter).opt("CTC");
		Object nomineeRelation = employeeArray.getJSONObject(counter).opt("NomineeRelation");
		Object maritalStatus = employeeArray.getJSONObject(counter).opt("MaritalStatus");
		Object dateOfBirth = employeeArray.getJSONObject(counter).opt("D.O.B");
		Object designation = employeeArray.getJSONObject(counter).opt("Designation");
		Object bankAccount = employeeArray.getJSONObject(counter).opt("BankAccountNo");
		Object resignationDate = employeeArray.getJSONObject(counter).opt("ResignationDate");

		Object branchId = getBranchId(branch);
		Object scheduleId = getScheduleId(schedule);
		Object genderId = getGenderId(gender);
		Object departmentId = getDepartmentId(department);
		Object nomineeRelationId = getNomineeRelationId(nomineeRelation);
		Object maritalStatusId = getMaritalStatusId(maritalStatus);
		Object designationId = getDesignationId(designation);
		Object businessFunctionId = getBusinessFunctionId(businessFunction);

		Object leavePolicyId = getLeavePolicyId();
		Object holidayCalendarId = getHolidayCalendarId();

		JSONObject updatesColumns = new JSONObject();
		if (!isInsert && key != null) {
			updatesColumns.put("__key__", key);
		} else {
			updatesColumns.put("employeestatusid", 1);
			updatesColumns.put("employeecode", employeeCode);
		}
		if (fatherName != null && ("" + fatherName).length() > 0) {
			updatesColumns.put("fathername", fatherName);
		}
		if (employeeName != null && ("" + employeeName).length() > 0) {
			updatesColumns.put("firstname", employeeName);
			updatesColumns.put("name", employeeName);
		}
		if (address != null && ("" + address).length() > 0) {
			updatesColumns.put("address", address);
		}
		if (phoneNumber != null && ("" + phoneNumber).length() > 0) {
			updatesColumns.put("mobileno", phoneNumber);
		}
		if (ctc != null && ("" + ctc).length() > 0) {
			updatesColumns.put("ctc", ctc);
		}
		if (pfNumber != null && ("" + pfNumber).length() > 0) {
			updatesColumns.put("providentfundno", pfNumber);
		}
		if (dateOfJoining != null && ("" + dateOfJoining).length() > 0) {
			updatesColumns.put("joiningdate", dateOfJoining);
		}
		if (nominee != null && ("" + nominee).length() > 0) {
			updatesColumns.put("nomineename", nominee);
		}
		if (dateOfBirth != null && ("" + dateOfBirth).length() > 0) {
			updatesColumns.put("dateofbirth", dateOfBirth);
		}
		if (resignationDate != null && ("" + resignationDate).length() > 0) {
			updatesColumns.put("employeestatusid", 2);
			updatesColumns.put("relievingdate", resignationDate);
		}
		if (branchId != null && ("" + branchId).length() > 0) {
			updatesColumns.put("branchid", branchId);
		}
		if (scheduleId != null && ("" + scheduleId).length() > 0) {
			updatesColumns.put("employeescheduleid", scheduleId);
		}
		if (genderId != null && ("" + genderId).length() > 0) {
			updatesColumns.put("genderid", genderId);
		}
		if (departmentId != null && ("" + departmentId).length() > 0) {
			updatesColumns.put("departmentid", departmentId);
		}
		if (nomineeRelationId != null && ("" + nomineeRelationId).length() > 0) {
			updatesColumns.put("relationwithnomineeid", nomineeRelationId);
		}
		if (maritalStatusId != null && ("" + maritalStatusId).length() > 0) {
			updatesColumns.put("maritalstatusid", maritalStatusId);
		}
		if (designationId != null && ("" + designationId).length() > 0) {
			updatesColumns.put("designationid", designationId);
		}
		if (businessFunctionId != null && ("" + businessFunctionId).length() > 0) {
			updatesColumns.put("businessfunctionid", businessFunctionId);
		}
		if (leavePolicyId != null && ("" + leavePolicyId).length() > 0) {
			updatesColumns.put("leavepolicyid", leavePolicyId);
		}
		if (holidayCalendarId != null && ("" + holidayCalendarId).length() > 0) {
			updatesColumns.put("holidaycalendarid", holidayCalendarId);
		}
		if (bankAccount != null && ("" + bankAccount).trim().length() > 0) {
			updatesColumns.put("accountno", bankAccount);
			updatesColumns.put("bankname", 1);
			updatesColumns.put("name_in_bank", employeeName);
			updatesColumns.put("modeofsalaryid", 1); // Account Transfer
		} else {
			updatesColumns.put("modeofsalaryid", 2); // By Cheque
		}

		updatesColumns.put("temp", "temp");

		JSONObject updates = new JSONObject();
		updates.put(Data.Update.RESOURCE, "hris_employees");
		updates.put(Data.Update.UPDATES, updatesColumns);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
	}

	private Object getHolidayCalendarId() throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_holidaycalendars");
		query.put(Data.Query.MAX_ROWS, 1);
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_holidaycalendars");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getLeavePolicyId() throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_leavepolicy");
		query.put(Data.Query.MAX_ROWS, 1);
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_leavepolicy");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getBusinessFunctionId(Object businessFunction) throws Exception {
		if (businessFunction == null || ("" + businessFunction).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "businessfunctions");
		query.put(Data.Query.FILTERS, "name = '" + businessFunction + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("businessfunctions");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getDesignationId(Object designation) throws Exception {
		if (designation == null || ("" + designation).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_designations");
		query.put(Data.Query.FILTERS, "name = '" + designation + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_designations");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getMaritalStatusId(Object maritalStatus) throws Exception {
		if (maritalStatus == null || ("" + maritalStatus).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_maritialstatus");
		query.put(Data.Query.FILTERS, "name = '" + maritalStatus + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("organization_maritialstatus");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getNomineeRelationId(Object nomineeRelation) throws Exception {
		if (nomineeRelation == null || ("" + nomineeRelation).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "relationwithnominee");
		query.put(Data.Query.FILTERS, "name = '" + nomineeRelation + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("relationwithnominee");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getDepartmentId(Object department) throws Exception {
		if (department == null || ("" + department).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_departments");
		query.put(Data.Query.FILTERS, "name = '" + department + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_departments");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getGenderId(Object gender) throws Exception {
		if (gender == null || ("" + gender).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_genders");
		query.put(Data.Query.FILTERS, "name = '" + gender + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("organization_genders");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getScheduleId(Object schedule) throws Exception {
		if (schedule == null || ("" + schedule).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_schedules");
		query.put(Data.Query.FILTERS, "name = '" + schedule + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_schedules");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private Object getBranchId(Object branch) throws Exception {
		if (branch == null || ("" + branch).trim().length() == 0) {
			return null;
		}
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_branches");
		query.put(Data.Query.FILTERS, "name = '" + branch + "'");
		JSONArray branchArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_branches");
		if (branchArray != null && branchArray.length() > 0) {
			return branchArray.getJSONObject(0).opt("__key__");
		} else {
			return null;
		}
	}

	private JSONArray getAlreadyExistArray(Object employeeCode, Object employeeCode1) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		query.put(Data.Query.FILTERS, "employeecode='" + employeeCode + "'");
		query.put(Data.Query.OPTION_FILTERS, "employeecode='" + employeeCode1 + "'");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees");
	}

	@Override
	public void initiate(ApplaneTaskInfo taskInfoObject) throws DeadlineExceededException {
		try {
			Object userId = 13652;
			String organization = "Darcl Logistics Limited";
			PunchingUtility.setValueInSession(organization, userId, ApplicationConstants.OWNER);

			JSONObject taskQueueParameters = taskInfoObject.getTaskInfo();
			JSONArray employeeArray = taskQueueParameters.getJSONArray("employeeArray");
			int counter = Translator.integerValue(taskQueueParameters.opt("counter"));
			boolean isInsert = Translator.booleanValue(taskQueueParameters.opt("isInsert"));

			portEmployeesFromSapToHrm(employeeArray, counter, isInsert);
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("Employee Porting Exception (Sap to Hrm) >> " + trace);
			throw new RuntimeException("Employee Porting Exception (Sap to Hrm) >> " + trace);
		}
	}
}
