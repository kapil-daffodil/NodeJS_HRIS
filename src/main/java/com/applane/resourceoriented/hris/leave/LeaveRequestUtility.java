package com.applane.resourceoriented.hris.leave;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeReleasingSalaryServlet;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;

public class LeaveRequestUtility {
	public JSONArray getLeaveTakenInLestThreMonths(Object employeeId, Object requestDateObject) {
		return getLeaveTakenInLestThreMonths(employeeId, requestDateObject, null);
	}

	public JSONArray getLeaveTakenInLestThreMonths(Object employeeId, Object requestDateObject, JSONArray lastThreeDatesList) {
		JSONArray valueToReturn = new JSONArray();
		try {
			if (employeeId != null && requestDateObject != null) {
				Calendar cal = Calendar.getInstance();
				Date requestDate = Translator.dateValue(requestDateObject);
				getValueToReturn(employeeId, requestDateObject, valueToReturn, cal, requestDate, lastThreeDatesList);
			} else {
				throw new BusinessLogicException("Employee and Request Date Can Not be Null");
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessLogicException("Some Unown Error Occured. Please Contact To Admin.");
		}
		LogUtility.writeLog("valueToReturn >> " + valueToReturn);
		return valueToReturn;
	}

	private void getValueToReturn(Object employeeId, Object requestDateObject, JSONArray valueToReturn, Calendar cal, Date requestDate, JSONArray lastThreeDatesList) throws ParseException, JSONException {
		LogUtility.writeLog("requestDateObject >> " + requestDateObject);
		for (int counter = 0; counter < 3; counter++) {
			cal.setTime(requestDate);
			cal.add(Calendar.MONTH, -counter);
			int monthId = cal.get(Calendar.MONTH) + 1;
			String monthName = EmployeeReleasingSalaryServlet.getMonthName(monthId);
			Date monthFirstDateDate = DataTypeUtilities.getMonthFirstDate("" + cal.get(Calendar.YEAR), monthName);
			Date toDate = DataTypeUtilities.getMonthLastDate(monthFirstDateDate);
			String fromDateString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDateDate);
			String todateString = EmployeeSalaryGenerationServlet.getDateInString(toDate);
			if (toDate.after(requestDate)) {
				todateString = "" + requestDateObject;
			} else {
				todateString = EmployeeSalaryGenerationServlet.getDateInString(toDate);
			}

			String filter = "employeeid=" + employeeId + " AND attendancedate>='" + fromDateString + "' AND attendancedate<='" + todateString + "' AND attendancetypeid IN(" + HRISApplicationConstants.ATTENDANCE_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE + "," + HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY + ")";
			JSONArray takenLeaveDetailsFromDailyAttendance = new EmployeeSalarySheetReport().getEmployeeAttendanceArray(filter);
			double takenLeaves = 0.0;
			LogUtility.writeLog("takenLeaveDetailsFromDailyAttendancec >> " + takenLeaveDetailsFromDailyAttendance);
			putValueIntoArray(valueToReturn, monthId, monthName, takenLeaveDetailsFromDailyAttendance, takenLeaves, lastThreeDatesList);
		}
	}

	private void putValueIntoArray(JSONArray valueToReturn, int monthId, String monthName, JSONArray takenLeaveDetailsFromDailyAttendance, double takenLeaves, JSONArray lastThreeDatesList) throws JSONException {
		for (int attendanceCounter = 0; attendanceCounter < takenLeaveDetailsFromDailyAttendance.length(); attendanceCounter++) {
			int attendanceTypeId = Translator.integerValue(takenLeaveDetailsFromDailyAttendance.getJSONObject(attendanceCounter).opt("attendancetypeid"));
			Object attendanceDate = takenLeaveDetailsFromDailyAttendance.getJSONObject(attendanceCounter).opt("attendancedate");
			if (lastThreeDatesList != null && lastThreeDatesList.length() < 3) {
				lastThreeDatesList.put(attendanceDate);
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
				takenLeaves++;
			}
			if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
				takenLeaves += 0.50;
			}
		}
		JSONObject object = new JSONObject();
		object.put("monthid", new JSONObject().put(Updates.KEY, monthId).put("name", monthName));
		object.put("taken_leaves", takenLeaves);
		valueToReturn.put(object);
	}
}