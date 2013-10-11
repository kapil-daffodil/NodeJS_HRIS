package com.applane.resourceoriented.hris.reports;

import java.text.DecimalFormat;
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
import com.applane.databaseengine.Result;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeMonthlyAttandanceUpdateAllColumns;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class CalculateBasicIfGreaterThan40Percent implements QueryJob {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onQuery(Query query) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		try {
			Query query = result.getQuery();
			String TODAY_DATE = query.getParameter("date") == null ? "" : ("" + query.getParameter("date"));
			JSONArray employeeArray = result.getRecords();
			LogUtility.writeError("employeeArray >> " + employeeArray);
			JSONArray employeeBasicRecords = new JSONArray();
			calculateSalaryInvokeMethod(employeeBasicRecords, employeeArray, TODAY_DATE);
			employeeArray.put(new JSONObject().put("employeeBasicRecords", employeeBasicRecords));
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("CalculateBasicIfGreaterThan40Percent Exception [" + trace + "]");
			throw new BusinessLogicException("Some Unknown Error Occured Please Contact To Admin.");
		}
	}

	public void calculateSalaryInvokeMethod() {
		calculateSalaryInvokeMethod(new JSONArray(), null, "");
	}

	public void calculateSalaryInvokeMethod(JSONArray employeeBasicRecords, JSONArray employeeArray, String TODAY_DATE) {
		try {
			if (TODAY_DATE.length() == 0) {
				TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(Translator.dateValue(TODAY_DATE));

			long yearId = EmployeeReleasingSalaryServlet.getYearId(String.valueOf(cal.get(Calendar.YEAR)));
			int monthId = (cal.get(Calendar.MONTH) + 1);
			if (employeeArray == null) {
				employeeArray = getEmployeeRecordsForSalarySheetReGenerate();
			}
			if (employeeArray != null && employeeArray.length() > 0) {
				String monthName = getMonthName(monthId);
				String yearName = getYearName(yearId);
				Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);
				String monthFirstDateInString = getDateInString(monthFirstDate);
				String monthLastDateInString = getDateInString(monthLastDate);
				double maxDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
				EmployeeMonthlyAttandanceUpdateAllColumns employeeMonthlyAttandance = null;

				JSONObject fields = new JSONObject();

				fields.put("monthId", monthId);
				fields.put("yearId", yearId);
				fields.put("monthName", monthName);
				fields.put("yearName", yearName);
				fields.put("monthFirstDate", monthFirstDate);
				fields.put("monthLastDate", monthLastDate);
				fields.put("monthFirstDateInString", monthFirstDateInString);
				fields.put("monthLastDateInString", monthLastDateInString);
				fields.put("employeeMonthlyAttandance", employeeMonthlyAttandance);
				fields.put("maxDaysInMonth", maxDaysInMonth);
				int employeeId = 0;
				for (int counter = 0; counter < employeeArray.length(); counter++) {
					JSONObject employeeArrayObject = employeeArray.getJSONObject(counter);
					if (employeeArrayObject != null) {
						int branchId = Translator.integerValue(employeeArray.getJSONObject(counter).opt("branchid"));
						employeeId = Translator.integerValue(employeeArray.getJSONObject(counter).opt(Updates.KEY));
						String branchName = Translator.stringValue(employeeArray.getJSONObject(counter).opt("branchid.name"));
						String employeeName = Translator.stringValue(employeeArray.getJSONObject(counter).opt("name"));
						String employeeCode = Translator.stringValue(employeeArray.getJSONObject(counter).opt("employeecode"));
						JSONArray profitcenterArray = employeeArray.getJSONObject(counter).optJSONArray("profitcenterid");
						String profitcenterName = "";
						if (profitcenterArray != null && profitcenterArray.length() > 0) {
							JSONObject object = profitcenterArray.getJSONObject(0);
							if (object != null) {
								profitcenterName = object.optString("name", "");
							}
						}
						fields.put("employeeId", employeeId);
						fields.put("branchId", branchId);
						JSONObject details = populateSalaryComponents(employeeArrayObject, fields);
						if (details != null) {
							details.put("employeeName", employeeName);
							details.put("branchName", branchName);
							details.put("employeeCode", employeeCode);
							details.put("profitcenterName", profitcenterName);
							employeeBasicRecords.put(details);
						}
					}
				}
				// LogUtility.writeLog("employeeBasicRecords >> " + employeeBasicRecords);
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			sendMailInCaseOfException(trace);
			throw new RuntimeException(trace);
		}
	}

	public static JSONObject populateSalaryComponents(JSONObject employeeArrayObject, JSONObject fields) throws JSONException, ParseException {
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
				return salaryCalculationForRegularEmployee(employeeSalaryComponentArray, fields, employeeId);
			}
		} catch (Exception e) {
			exception += (" << employeeid >> " + employeeId + " << exception >> message [" + e.getMessage() + " << >> " + e.getStackTrace() + "]");
		}
		if (exception.length() > 0) {
		}
		return null;
	}

	/**
	 * This method is calculate monthly amount of all components and insert in calculate monthly amount DO.
	 * 
	 * @param employeeSalaryComponentArray
	 * @param fields
	 * @param employeeId
	 * @param employeeBasicRecords
	 * @param employeeRecord
	 * @throws JSONException
	 */
	public static JSONObject salaryCalculationForRegularEmployee(JSONArray employeeSalaryComponentArray, JSONObject fields, Object employeeId) throws JSONException {

		double ctc = 0.0;
		double basicAmount = 0.0;
		double performanceAmount = 0.0;
		double fixAmount = 0.0;

		for (int counter = 0; counter < employeeSalaryComponentArray.length(); counter++) {

			JSONObject employeeSalaryComponentRecord = employeeSalaryComponentArray.getJSONObject(counter);
			int componentTypeId = Translator.integerValue(employeeSalaryComponentRecord.opt("salarycomponentid.salarycomponenttypeid"));
			int componentCriteriaId = Translator.integerValue(employeeSalaryComponentRecord.opt("salarycomponentid.componentcriteriaid"));
			// Object employeeSalaryComponentId = employeeSalaryComponentRecord.opt("__key__");
			Integer paymentCycleId = (Integer) employeeSalaryComponentRecord.opt("salarycomponentid.paymentcycleid");
			double componentAmount = employeeSalaryComponentRecord.opt("amount") == null ? 0.0 : employeeSalaryComponentRecord.optDouble("amount");
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
				if (componentCriteriaId == HRISApplicationConstants.PERFORMANCE_BASED || componentCriteriaId == HRISApplicationConstants.EXPERIENCE_BASED) {
					performanceAmount += componentAmount;
				} else {
					fixAmount += componentAmount;
				}
				if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.BASIC) {
					basicAmount += componentAmount;
				}
				ctc += componentAmount;
			}
		}
		double basicPercentage = (float) ((basicAmount * 100) / fixAmount);
		JSONObject employeeBasicRecords = new JSONObject();
		DecimalFormat df = new DecimalFormat("#.##");
		employeeBasicRecords.put("basicPercentage", df.format(basicPercentage));
		employeeBasicRecords.put("performanceAmount", df.format(performanceAmount));
		employeeBasicRecords.put("fixAmount", df.format(fixAmount));
		employeeBasicRecords.put("basicAmount", df.format(basicAmount));
		employeeBasicRecords.put("ctc", df.format(ctc));
		return employeeBasicRecords;
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

	public static JSONArray getEmployeeRecordsForSalarySheetReGenerate() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("employeecode");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");
		columnArray.put("branchid");
		columnArray.put("branchid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.MAX_ROWS, 20);
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

	private void sendMailInCaseOfException(String traces) {
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
}
