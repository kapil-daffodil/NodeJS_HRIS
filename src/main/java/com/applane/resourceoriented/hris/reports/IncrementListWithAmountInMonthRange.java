package com.applane.resourceoriented.hris.reports;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class IncrementListWithAmountInMonthRange implements QueryJob {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onQuery(Query arg0) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONArray array = result.getRecords();
			if (array != null && array.length() > 0) {
				Object branchId;
				branchId = array.getJSONObject(0).opt("branchid");
				Object fromDate = array.getJSONObject(0).opt("fromdate");
				Object toDate = array.getJSONObject(0).opt("todate");
				if (fromDate != null && toDate != null) {

					JSONArray finalResult = calculateSalaryInvokeMethod(branchId, fromDate, toDate);
					JSONObject object = new JSONObject();
					object.put("finalResult", finalResult);
					array.put(object);

				}
			} else {
				throw new BusinessLogicException("From Date and To Date Is Compulsory");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("IncrementListWithAmountInMonthRange Exception  >> : [" + trace + "]");
			sendMailInCaseOfException(e);
			throw new RuntimeException(trace);
		}
	}

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

	public JSONArray calculateSalaryInvokeMethod(Object branchId, Object fromDateObject, Object toDateObject) throws Exception {
		String kapil = "";
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		JSONArray employeeArray = getEmployeeRecordsForSalarySheetReGenerate(branchId, fromDateObject, toDateObject);
		JSONArray finalDetailsArray = new JSONArray();
		if (employeeArray != null && employeeArray.length() > 0) {
			Calendar cal = Calendar.getInstance();
			Date fromDate = Translator.dateValue(fromDateObject);
			Date toDate = Translator.dateValue(toDateObject);
			HashMap<Integer, JSONArray> employeeCtcDetail = new HashMap<Integer, JSONArray>();
			HashMap<Integer, String[]> employeeDetailMap = new HashMap<Integer, String[]>();
			JSONArray salaryComponents = new EmployeeSalarySheetReport().getSalaryComponents();
			ArrayList<Integer> ignoreableComponentList = getIgnoreAbleComponentList(salaryComponents);
			for (int counter = 0; counter < employeeArray.length(); counter++) {
				JSONObject employeeArrayObject = employeeArray.getJSONObject(counter);
				if (employeeArrayObject != null) {
					if (branchId == null) {
						branchId = Translator.integerValue(employeeArrayObject.opt("branchid"));
					}
					int employeeId = Translator.integerValue(employeeArrayObject.opt(Updates.KEY));
					String employeeCode = "" + employeeArrayObject.opt("employeecode");
					String name = "" + employeeArrayObject.opt("name");
					employeeDetailMap.put(employeeId, new String[] { name, employeeCode });
					long diff = DataTypeUtilities.monthsBetweenDates(fromDate, toDate);
					double ctc = 0.0;
					JSONArray details = new JSONArray();
					for (int monthCounter = 0; monthCounter < diff; monthCounter++) {
						cal.setTime(fromDate);
						cal.add(Calendar.MONTH, monthCounter);
						TODAY_DATE = EmployeeSalaryGenerationServlet.getDateInString(cal.getTime());
						JSONObject fields = getFieldsDetails(TODAY_DATE);
						fields.put("employeeId", employeeId);
						fields.put("branchId", branchId);
						ctc = 0.0;
						ctc = populateSalaryComponents(employeeArrayObject, fields, ignoreableComponentList);
						if (employeeId == 130) {
							kapil += ctc + " ctc";
						}
						if (ctc >= 0) {
							JSONObject detailsObject = new JSONObject();
							detailsObject.put("ctc", (int) (ctc + 0.50));
							detailsObject.put("monthName", getMonthName((cal.get(Calendar.MONTH) + 1)));
							details.put(detailsObject);
							break;
						}
					}
					if (details != null && details.length() > 0) {
						employeeCtcDetail.put(employeeId, details);
					}
					if (employeeId == 130) {
						kapil += details + " found";
					}
				}
			}

			for (Integer employeeId : employeeCtcDetail.keySet()) {
				JSONArray ctcDetails = employeeCtcDetail.get(employeeId);
				String[] employeeDetails = employeeDetailMap.get(employeeId);
				String employeeName = employeeDetails[0];
				String employeeCode = employeeDetails[1];
				finalDetailsArray.put(new JSONObject().put("employeeName", employeeName).put("employeeCode", employeeCode).put("employeeCtcDetails", ctcDetails));

			}
		}
		return finalDetailsArray;
	}

	private ArrayList<Integer> getIgnoreAbleComponentList(JSONArray salaryComponents) throws JSONException {
		ArrayList<Integer> ignoreableComponentList = new ArrayList<Integer>();
		for (int counter = 0; counter < salaryComponents.length(); counter++) {
			Integer salaryComponentId = DataTypeUtilities.integerValue(salaryComponents.getJSONObject(counter).opt(Updates.KEY));
			Object ignoreInExceptionReport = DataTypeUtilities.integerValue(salaryComponents.getJSONObject(counter).opt("ignoreinexceptionreport"));
			if (salaryComponentId != null && ignoreInExceptionReport != null && Translator.integerValue(ignoreInExceptionReport) == HRISApplicationConstants.EmployeeDecision.YES) {
				ignoreableComponentList.add(salaryComponentId);
			}
		}
		return ignoreableComponentList;
	}

	private static JSONObject getFieldsDetails(String TODAY_DATE) throws Exception {
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
		long maxDaysInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
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

	public static double populateSalaryComponents(JSONObject employeeArrayObject, JSONObject fields, ArrayList<Integer> ignoreableComponentList) throws Exception {
		Object employeeId = null;
		double ctc = -1;

		Date monthFirstDate = (Date) fields.opt("monthFirstDate");
		Date monthLastDate = (Date) fields.opt("monthLastDate");

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

		JSONArray employeeSalaryComponentArray = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(fromDateStr, lastDateStr, employeeId); // globle employee id used
		if (employeeSalaryComponentArray.length() > 0) {
			boolean isIncrementCurrentMonth = false;
			for (int salaryComponentArrayCounter = 0; salaryComponentArrayCounter < employeeSalaryComponentArray.length(); salaryComponentArrayCounter++) {
				Date applicableToDate = Translator.dateValue(employeeSalaryComponentArray.getJSONObject(salaryComponentArrayCounter).opt("applicableto"));
				Date applicableFromDate = Translator.dateValue(employeeSalaryComponentArray.getJSONObject(salaryComponentArrayCounter).opt("applicablefrom"));
				int salaryComponentId = Translator.integerValue(employeeSalaryComponentArray.getJSONObject(salaryComponentArrayCounter).opt("salarycomponentid"));
				int salaryComponentTypeId = Translator.integerValue(employeeSalaryComponentArray.getJSONObject(salaryComponentArrayCounter).opt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID));
				if (!ignoreableComponentList.contains(salaryComponentId) && salaryComponentTypeId == HRISApplicationConstants.SalaryComponentTypes.BASIC) {
					if ((applicableToDate != null && monthFirstDate.before(applicableToDate) && monthLastDate.after(applicableToDate)) || (monthFirstDate.equals(applicableToDate))) {
						isIncrementCurrentMonth = true;
						break;
					} else if (applicableFromDate != null && applicableFromDate.equals(monthFirstDate)) {
						isIncrementCurrentMonth = true;
						break;
					} else if (applicableToDate != null && applicableToDate.equals(monthLastDate)) {
						isIncrementCurrentMonth = true;
						break;
					}
				}
			}
			if (isIncrementCurrentMonth) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(monthFirstDate);
				cal.add(Calendar.MONTH, -1);
				String TODAY_DATE = EmployeeSalaryGenerationServlet.getDateInString(cal.getTime());
				fields = getFieldsDetails(TODAY_DATE);
				fromDateStr = "" + fields.opt("monthFirstDateInString");
				lastDateStr = "" + fields.opt("monthLastDateInString");
				employeeSalaryComponentArray = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(fromDateStr, lastDateStr, employeeId);
				double previousCTC = salaryCalculationForRegularEmployee(employeeSalaryComponentArray, fields, employeeId);

				cal.setTime(monthFirstDate);
				cal.add(Calendar.MONTH, 1);
				TODAY_DATE = EmployeeSalaryGenerationServlet.getDateInString(cal.getTime());
				fields = getFieldsDetails(TODAY_DATE);
				fromDateStr = "" + fields.opt("monthFirstDateInString");
				lastDateStr = "" + fields.opt("monthLastDateInString");
				employeeSalaryComponentArray = EmployeeSalaryGenerationServlet.getEmployeeSalaryComponents(fromDateStr, lastDateStr, employeeId);
				double nextCTC = salaryCalculationForRegularEmployee(employeeSalaryComponentArray, fields, employeeId);
				ctc = nextCTC - previousCTC;

			}
		}
		return ctc;
	}

	public static double salaryCalculationForRegularEmployee(JSONArray employeeSalaryComponentArray, JSONObject fields, Object employeeId) throws JSONException {

		double ctc = 0.0;

		for (int counter = 0; counter < employeeSalaryComponentArray.length(); counter++) {

			JSONObject employeeSalaryComponentRecord = employeeSalaryComponentArray.getJSONObject(counter);
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
					ctc += componentAmount;

				}
			}
		}
		return ctc;
	}

	public static Date getMonthFirstDate(long yearId, long monthId) throws Exception {
		String monthName = getMonthName(monthId);
		String yearName = getYearName(yearId);
		Date date = null;
		date = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
		return date;
	}

	public static JSONArray getEmployeeRecordsForSalarySheetReGenerate(Object branchId, Object fromDate, Object toDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("name");
		columnArray.put("employeecode");
		columnArray.put("joiningdate");
		columnArray.put("relievingdate");

		String branchFilter = "";
		if (branchId != null) {
			branchFilter = " and branchid = " + branchId;
		}
		query.put(Data.Query.COLUMNS, columnArray);
		// query.put(Data.Query.FILTERS, "__key__ = 87");
		query.put(Data.Query.FILTERS, "joiningdate <='" + toDate + "'" + branchFilter);
		query.put(Data.Query.OPTION_FILTERS, "relievingdate >='" + fromDate + "'" + branchFilter);
		// query.put(Data.Query.MAX_ROWS, 10);

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
			query.put(Data.Query.FILTERS, Updates.KEY + " = " + monthId);
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
			query.put(Data.Query.FILTERS, Updates.KEY + " = " + yearId);
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
}
