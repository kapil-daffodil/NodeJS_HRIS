package com.applane.resourceoriented.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class CalculateCtcFromStartToEndForHistory implements ApplaneTask {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	private void sendMailInCaseOfException(Exception e) {
		String traces = ExceptionUtils.getExceptionTraceMessage("EmployeeSalaryGenerationServlet Exception >> ", e);
		LogUtility.writeError(traces);
		ApplaneMail mail = new ApplaneMail();
		StringBuilder builder = new StringBuilder();
		builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
		builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(traces);
		mail.setMessage("Employee Salary Servlet Task Queue Failed", builder.toString(), true);
		try {
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Employee Salary Servlet.");
		}

	}

	public void calculateSalaryInvokeMethod() {
		calculateSalaryInvokeMethod(null, null);
	}

	public void calculateSalaryInvokeMethod(Object[] keys) {
		calculateSalaryInvokeMethod(keys, null);
	}

	public void calculateSalaryInvokeMethod(Object[] keys, Object employeeIdObject) {
		int employeeId = 0;
		try {
			String selectedKeys = "";
			if (keys != null) {
				for (int counter = 0; counter < keys.length; counter++) {
					if (selectedKeys.length() == 0) {
						selectedKeys += keys[counter];
					} else {
						selectedKeys += ("," + keys[counter]);
					}
				}
			}
			if (selectedKeys.length() > 0 || keys == null) {
				JSONArray employeeArray = getEmployees(selectedKeys, employeeIdObject);
				if (employeeArray != null && employeeArray.length() > 0) {
					JSONObject fields = new JSONObject();

					for (int counter = 0; counter < employeeArray.length(); counter++) {
						JSONObject employeeArrayObject = employeeArray.getJSONObject(counter);
						// LogUtility.writeLog("employeeArrayObject >> " + employeeArrayObject);
						if (employeeArrayObject != null) {
							int branchId = Translator.integerValue(employeeArray.getJSONObject(counter).opt("branchid"));
							employeeId = Translator.integerValue(employeeArray.getJSONObject(counter).opt(Updates.KEY));
							JSONArray basicAttay = getEmployeeSalaryComponentsBasic(employeeId);
							Calendar cal = Calendar.getInstance();
							daleteOldCtc(employeeId);
							for (int basicComponentsCounter = 0; basicComponentsCounter < basicAttay.length(); basicComponentsCounter++) {
								JSONObject basicComponentObject = basicAttay.getJSONObject(basicComponentsCounter);
								Object applicableFromDateObject = basicComponentObject.opt("applicablefrom");
								Date applicableFromDate = Translator.dateValue(applicableFromDateObject);
								cal.setTime(applicableFromDate);
								cal.add(Calendar.MONTH, 1);
								Date nextDate = cal.getTime();
								String nextDateInString = EmployeeSalaryGenerationServlet.getDateInString(nextDate);

								fields = getFieldsDetails(nextDateInString);
								fields.put("employeeId", employeeId);
								fields.put("branchId", branchId);
								boolean isIncrement = false;
								populateSalaryComponents(employeeArrayObject, fields, isIncrement);

								isIncrement = true;
								fields = getFieldsDetails("" + applicableFromDateObject);
								fields.put("employeeId", employeeId);
								fields.put("branchId", branchId);

								populateSalaryComponents(employeeArrayObject, fields, isIncrement);
							}
						}
					}
					if (employeeId != 0 && keys == null) {
						initiateTaskQueue(employeeId);
					}
				}
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			try {
				if (employeeId != 0) {
					initiateTaskQueue(employeeId);
				}
			} catch (JSONException e1) {
				String trace1 = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				LogUtility.writeLog("Calculate CTC Exception(1)  >> : [" + trace1 + "]");
				sendMailInCaseOfException(e);
			}
			LogUtility.writeLog("Calculate CTC Exception  >> : [" + trace + "]");
			sendMailInCaseOfException(e);
			throw new RuntimeException(trace);
		}
	}

	private void daleteOldCtc(int employeeId) throws Exception {
		JSONArray ctcArray = getAllCtcArray(employeeId);
		if (ctcArray != null && ctcArray.length() > 0) {
			for (int counter = 0; counter < ctcArray.length(); counter++) {
				ctcArray.getJSONObject(counter).put(Data.Update.Updates.TYPE, Data.Update.Updates.Types.DELETE);
			}
			JSONObject updates = new JSONObject();
			updates.put(Data.Query.RESOURCE, "hris_employees_ctc_history");
			updates.put(Data.Update.UPDATES, ctcArray);
			ResourceEngine.update(updates);
		}
	}

	private JSONArray getAllCtcArray(int employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees_ctc_history");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("hris_employees_ctc_history");
		return rows;
	}

	private JSONArray getEmployeeSalaryComponentsBasic(int employeeId) throws Exception {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeesalarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("amount");
		columnArray.put("applicablefrom");
		columnArray.put("applicableto");
		JSONArray orders = new JSONArray();
		JSONObject orderObject = new JSONObject();
		orderObject.put(Data.Query.Orders.EXPERSSION, "applicablefrom");
		orderObject.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);
		orders.put(orderObject);
		query.put(Data.Query.ORDERS, orders);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND amount > 0.0 AND salarycomponentid.salarycomponenttypeid = " + HRISApplicationConstants.SalaryComponentTypes.BASIC);
		JSONObject object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("employeesalarycomponents");
		return rows;
	}

	private JSONObject getFieldsDetails(String TODAY_DATE) throws Exception {
		JSONObject fields = new JSONObject();
		long yearId = 0;
		int monthId = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTime(Translator.dateValue(TODAY_DATE));

		yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
		monthId = (cal.get(Calendar.MONTH) + 1);
		String monthName = getMonthName(monthId);
		String yearName = getYearName(yearId);
		Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
		String monthFirstDateInString = getDateInString(monthFirstDate);
		String monthLastDateInString = getDateInString(monthLastDate);
		double maxDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
		EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = null;
		int yearIdInt = (int) yearId;
		fields.put("monthId", monthId);
		fields.put("yearId", yearIdInt);
		fields.put("monthName", monthName);
		fields.put("yearName", yearName);
		fields.put("monthFirstDate", monthFirstDate);
		fields.put("monthLastDate", monthLastDate);
		fields.put("monthFirstDateInString", monthFirstDateInString);
		fields.put("monthLastDateInString", monthLastDateInString);
		fields.put("employeeMonthlyAttandance", employeeMonthlyAttandance);
		fields.put("maxDaysInMonth", maxDaysInMonth);
		return fields;
	}

	public static void populateSalaryComponents(JSONObject employeeArrayObject, JSONObject fields, boolean isIncrement) throws JSONException, ParseException {
		String exception = "";
		Object employeeId = null;

		try {
			employeeId = employeeArrayObject.opt("__key__");
			Object joiningDateObject = employeeArrayObject.opt("joiningdate");
			Object relievingDateObject = employeeArrayObject.opt("relievingdate");

			Date joiningDate = null;
			String fromDateStr = fields.optString("monthFirstDateInString");
			String lastDateStr = fields.optString("monthLastDateInString");
			if (joiningDateObject != null) {
				joiningDate = DataTypeUtilities.checkDateFormat(joiningDateObject);
				if (joiningDate != null && EmployeeSalaryGenerationServlet.isNewJoineeOrReleaved(joiningDate, fields)) {
					fromDateStr = getDateInString(joiningDate);
				}
			}
			Date relievingDate = null;
			if (relievingDateObject != null) {
				relievingDate = DataTypeUtilities.checkDateFormat(relievingDateObject);
				if (relievingDate != null && EmployeeSalaryGenerationServlet.isNewJoineeOrReleaved(relievingDate, fields)) {
					lastDateStr = getDateInString(relievingDate);
				}
			}

			long maxDaysInMonth = DataTypeUtilities.differenceBetweenDates(DataTypeUtilities.checkDateFormat(fromDateStr), DataTypeUtilities.checkDateFormat(lastDateStr));
			long maxDaysInMonthTotal = DataTypeUtilities.getNumberOfDaysInMonth(DataTypeUtilities.checkDateFormat(fromDateStr));

			fields.put("maxDaysInMonth", maxDaysInMonth + 1);
			fields.put("maxDaysInMonthTotal", maxDaysInMonthTotal);

			JSONArray employeeSalaryComponentArray = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(fromDateStr, lastDateStr, employeeId); // globle employee id used
			if (employeeSalaryComponentArray.length() > 0) {
				salaryCalculationForRegularEmployee(employeeSalaryComponentArray, fields, employeeId, isIncrement);
			}
		} catch (Exception e) {
			exception += (" << employeeid >> " + employeeId + " << exception >> message [" + e.getMessage() + " << >> " + e.getStackTrace() + "]");
		}
		if (exception.length() > 0) {
			LogUtility.writeLog("exception >> " + exception);
		}
	}

	/**
	 * This method is calculate monthly amount of all components and insert in calculate monthly amount DO.
	 * 
	 * @param employeeSalaryComponentArray
	 * @param fields
	 * @param employeeId
	 * @param isIncrement
	 * @param employeeRecord
	 * @throws JSONException
	 */
	public static void salaryCalculationForRegularEmployee(JSONArray employeeSalaryComponentArray, JSONObject fields, Object employeeId, boolean isIncrement) throws JSONException {

		double ctc = 0.0;
		String monthFirstDateString = fields.optString("monthFirstDateInString");
		String monthLsatDateString = fields.optString("monthLastDateInString");
		Object applicablefrom = null;
		Object applicableto = null;

		for (int counter = 0; counter < employeeSalaryComponentArray.length(); counter++) {

			JSONObject employeeSalaryComponentRecord = employeeSalaryComponentArray.getJSONObject(counter);
			// Object salaryComponentId = employeeSalaryComponentRecord.opt("salarycomponentid");
			// Object salaryComponentName = employeeSalaryComponentRecord.opt("salarycomponentid.name");
			// Object employeeSalaryComponentId = employeeSalaryComponentRecord.opt("__key__");
			Integer paymentCycleId = (Integer) employeeSalaryComponentRecord.opt("salarycomponentid.paymentcycleid");
			double componentAmount = employeeSalaryComponentRecord.opt("amount") == null ? 0.0 : employeeSalaryComponentRecord.optDouble("amount");
			if (componentAmount > 0.0) {
				Integer paymentTypeId = (Integer) employeeSalaryComponentRecord.opt("salarycomponentid.paymenttypeid");

				if (paymentCycleId == HRISApplicationConstants.QUARTERLY) {
					componentAmount = (componentAmount * 4) / 12;
				} else if (paymentCycleId == HRISApplicationConstants.HALF_YEARLY) {
					componentAmount = (componentAmount * 2) / 12;
				} else if (paymentCycleId == HRISApplicationConstants.YEARLY) {
					componentAmount = (componentAmount * 1) / 12;
				} else if (paymentCycleId == HRISApplicationConstants.MONTHLY) {
					componentAmount = (componentAmount * 12) / 12;
				}

				// LogUtility.writeLog("Final componentAmount come for calculation: >> " + componentAmount);
				// double[] calculatedAmount = calculateRegularEmployeeComponentAmount(componentAmount, employeeSalaryComponentRecord);

				if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
					applicablefrom = employeeSalaryComponentRecord.opt("applicablefrom");
					applicableto = employeeSalaryComponentRecord.opt("applicableto");
					Date applicableFromDate = Translator.dateValue(employeeSalaryComponentRecord.opt("applicablefrom"));
					Date applicableToDate = Translator.dateValue(employeeSalaryComponentRecord.opt("applicableto"));

					Date monthFirstDate = (Date) fields.get("monthFirstDate");
					Date monthLastDate = (Date) fields.get("monthLastDate");

					if (applicableFromDate.after(monthFirstDate)) {
						monthFirstDate = applicableFromDate;
					}
					if (applicableToDate.before(monthLastDate)) {
						monthLastDate = applicableToDate;
					}
					Calendar cl1 = Calendar.getInstance();
					Calendar cl2 = Calendar.getInstance();
					cl1.setTime(monthFirstDate);
					cl2.setTime(monthLastDate);
					long milliseconds1 = cl1.getTimeInMillis();
					long milliseconds2 = cl2.getTimeInMillis();
					long diff = milliseconds2 - milliseconds1;
					double incrementDateDifference = 0.0;
					if (cl1.before(cl2)) {
						incrementDateDifference = (diff / (24 * 60 * 60 * 1000) + 1);
					} else if (cl1.equals(cl2)) {
						incrementDateDifference = 1;
					}

					componentAmount = (float) (componentAmount * (incrementDateDifference) / (Long) fields.opt("maxDaysInMonth"));
					// double[] calculatedAmount = calculateRegularEmployeeComponentAmount(employeeId, componentAmount, employeeSalaryComponentRecord, applicableToDate, componentExtraInformation, fields, incrementDateDifference);
					ctc += componentAmount;

				}
			}
		}
		insertCTC(ctc, fields, monthFirstDateString, monthLsatDateString, applicablefrom, applicableto, isIncrement);
	}

	private static void insertCTC(double grossAmount, JSONObject fields, String monthFirstDateString, String monthLsatDateString, Object applicablefrom, Object applicableto, boolean isIncrement) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, "hris_employees_ctc_history");
		JSONObject row = new JSONObject();
		row.put("employeeid", fields.opt("employeeId"));
		row.put("ctc", grossAmount);
		if (isIncrement) {
			row.put("is_increment", 1);
			row.put("month_first_date", monthFirstDateString);
			row.put("applicable_last_date", monthLsatDateString);
		} else {
			row.put("month_first_date", applicablefrom);
			row.put("applicable_last_date", applicableto);
		}
		updates.put(Data.Update.UPDATES, row);
		ResourceEngine.update(updates);
	}

	public static Date getMonthFirstDate(long yearId, long monthId) throws Exception {
		String monthName = getMonthName(monthId);
		String yearName = getYearName(yearId);
		Date date = null;
		date = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		return date;
	}

	/**
	 * This method will return all active and non-active employees(only those who have been relieved in this month)
	 * 
	 * @param employeeIdObject
	 * 
	 * @param yearName
	 * @param monthName
	 * @return
	 * @throws JSONException
	 */

	public static JSONArray getEmployees(String key, Object employeeIdObject) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("officialemailid");
		columnArray.put("incrementduedate");
		columnArray.put("employeescheduleid");
		columnArray.put("holidaycalendarid");
		columnArray.put("leavepolicyid");
		columnArray.put("leavepolicyid.issandwich");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");
		columnArray.put("branchid");
		query.put(Data.Query.COLUMNS, columnArray);
		if (key.length() > 0) {
			query.put(Data.Query.FILTERS, Updates.KEY + " in (" + key + ")");
		}
		if (employeeIdObject != null) {
			query.put(Data.Query.FILTERS, Updates.KEY + " > " + employeeIdObject);
			query.put(Data.Query.MAX_ROWS, 10);
		} else if (key.length() == 0) {
			query.put(Data.Query.MAX_ROWS, 10);
		}
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");
		return employeeArray;
	}

	public static String getDateInString(Date date) {
		if (date != null) {
			return new SimpleDateFormat("yyyy-MM-dd").format(date);
		} else {
			return "";
		}
	}

	public static String getMonthName(long monthId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_months");
			JSONArray array = new JSONArray();
			array.put("name");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + monthId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_months");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				String monthName = rows.getJSONObject(0).optString("name");
				return monthName;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error occured while geting month name from monthid: " + e.getMessage());
		}
	}

	public static String getYearName(long yearId) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_years");
			JSONArray array = new JSONArray();
			array.put("name");
			query.put(Data.Query.COLUMNS, array);
			query.put(Data.Query.FILTERS, "__key__ = " + yearId);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray rows = object.getJSONArray("organization_years");
			int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
			if (rowCount > 0) {
				String yearName = rows.getJSONObject(0).optString("name");
				return yearName;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Error occured while converting yearid into year name.." + e.getMessage());
		}
	}

	public static void initiateTaskQueue(Object employeeId) throws JSONException {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put("employeeId", employeeId);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.CalculateCtcFromStartToEndForHistory", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		JSONObject taskQueueInfo = applaneTaskInfo.getTaskInfo();
		Object employeeid = taskQueueInfo.opt("employeeId");
		calculateSalaryInvokeMethod(null, employeeid);

	}
}
