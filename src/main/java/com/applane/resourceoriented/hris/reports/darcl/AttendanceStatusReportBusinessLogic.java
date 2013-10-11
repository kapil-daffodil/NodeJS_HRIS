package com.applane.resourceoriented.hris.reports.darcl;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Result;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.QueryJob;
import com.applane.databaseengine.resource.Query;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class AttendanceStatusReportBusinessLogic implements QueryJob {

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void onQuery(Query arg0) throws DeadlineExceededException {

	}

	public void onResult(Result result) throws DeadlineExceededException {
		try {
			JSONArray array = result.getRecords();
			if (array != null && array.length() > 0) {
				Object branchId = array.getJSONObject(0).opt("branchid");
				Object departmentId = array.getJSONObject(0).opt("departmentid");
				Object fromDate = array.getJSONObject(0).opt("fromdate");
				Object toDate = array.getJSONObject(0).opt("todate");
				if (fromDate != null && toDate != null) {

					JSONArray finalResult = attendanceStatus(branchId, fromDate, toDate, departmentId);
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

	public JSONArray attendanceStatus(Object branchId, Object fromDateObject, Object toDateObject, Object departmentId) throws Exception {

		String extraFilters = "";
		if (branchId != null) {
			extraFilters += " and employeeid.branchid = " + branchId;
		}
		if (departmentId != null) {
			extraFilters += " and employeeid.departmentid = " + departmentId;
		}
		String filter = "attendancedate >= '" + fromDateObject + "' and attendancedate <= '" + toDateObject + "'" + extraFilters;// + "' and attendancetypeid in (" + HRISApplicationConstants.ATTENDANCE_ABSENT + ", " + HRISApplicationConstants.ATTENDANCE_UNKNOWN + ", " + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY + ", " + HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF + ")";
		JSONArray employeeAttendanceArray = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);

		JSONArray finalDetailsArray = new JSONArray();

		if (employeeAttendanceArray != null && employeeAttendanceArray.length() > 0) {
			HashMap<Integer, Object[]> employeeDetailMap = new HashMap<Integer, Object[]>();
			for (int counter = 0; counter < employeeAttendanceArray.length(); counter++) {
				JSONObject attendanceObject = employeeAttendanceArray.getJSONObject(counter);
				JSONArray employeeArray = new JSONArray();
				Object employeeObject = attendanceObject.opt("employeeid");
				if (employeeObject instanceof JSONObject) {
					employeeArray.put(employeeObject);
				} else if (employeeObject instanceof JSONArray) {
					employeeArray = (JSONArray) employeeObject;
				}
				int attendanceTypeId = Translator.integerValue(attendanceObject.opt("attendancetypeid"));
				int employeeId = 0;
				String employeeName = "";
				String employeeCode = "";
				String department = "";
				String reportingTo = "";
				if (employeeArray != null && employeeArray.length() > 0) {
					employeeId = Translator.integerValue(employeeArray.getJSONObject(0).opt(Updates.KEY));
					employeeCode = "" + employeeArray.getJSONObject(0).opt("employeecode");
					employeeName = "" + employeeArray.getJSONObject(0).opt("name");
					department = "" + employeeArray.getJSONObject(0).opt("departmentid.name");
					reportingTo = "" + employeeArray.getJSONObject(0).opt("reportingtoid.name");
				}
				Object[] details = new Object[6];
				if (employeeDetailMap.containsKey(employeeId)) {
					details = employeeDetailMap.get(employeeId);
				} else {
					details[0] = employeeName;
					details[1] = employeeCode;
					details[2] = department;
					details[3] = reportingTo;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HOLIDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					details[4] = Translator.doubleValue(details[4]) + 1.0;
				}

				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
					details[5] = Translator.doubleValue(details[5]) + 1.0;
				}

				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
					details[4] = Translator.doubleValue(details[4]) + 0.50;
					details[5] = Translator.doubleValue(details[5]) + 0.50;
				}
				employeeDetailMap.put(employeeId, details);
			}

			long daysBetweenDates = DataTypeUtilities.differenceBetweenDates(Translator.dateValue(fromDateObject), Translator.dateValue(toDateObject)) + 1l;

			for (Integer employeeId : employeeDetailMap.keySet()) {
				Object[] details = employeeDetailMap.get(employeeId);
				Object employeeName = details[0];
				Object employeeCode = details[1];
				Object departmentName = details[2];
				Object reportingToName = details[3];
				Object employeePresent = details[4];
				Object employeeLeave = details[5];
				double presentPercentage = daysBetweenDates == 0 ? 0.0 : ((Translator.doubleValue(details[4]) * 100) / daysBetweenDates);
				finalDetailsArray.put(new JSONObject().put("employeeName", employeeName).put("employeeCode", employeeCode).put("reportingToName", reportingToName).put("departmentName", departmentName).put("employeePresent", employeePresent).put("employeeLeave", employeeLeave).put("presentPercentage", new DecimalFormat("#.##").format(presentPercentage)).put("daysBetweenDates", daysBetweenDates));
			}
		}
		return finalDetailsArray;
	}
}
