package com.applane.resourceoriented.hris.reports.navigant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.user.service.UserUtility;

public class LateComingBusinessLogic {

	// Table Names

	private static final String HRIS_BRANCHES = "hris_branches";

	private static final String EMPLOYEE_ATTENDANCE = "employeeattendance";

	private static final String HRIS_EMPLOYEES = "hris_employees";

	private static final String HRIS_SHIFTS = "hris_shifts";

	private static final String HRIS_EMPLOYEES_SHIFT_HISTORY = "hris_employees_shift_history";

	// Column Names

	private static final String HR_EMAIL_IDS = "hr_email_ids";

	private static final String OFFICIAL_EMAIL_ID = "officialemailid";

	private static final String EMPLOYEE_ID = "employeeid";

	private static final String EMPLOYEE_NAME = "name";

	private static final String EMPLOYEE_CODE = "employeecode";

	private static final String EMPLOYEES_SHIFT_ID = "shiftid";

	private static final String BRANCH_ID = "branchid";

	private static final String FIRST_IN_TIME = "firstintime";

	private static final String FROMTIME_TIME = "fromtime_time";

	private static final String FROMTIME_MERIDIEM = "fromtime_meridiem";

	private static final String SHIFT_ID = "shift_id";

	private static final String FROM_DATE = "from_date";

	private static final String TO_DATE = "to_date";

	private static final String EMPLOYEE_DEPARTMENT = "departmentid.name";

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public void mailLateComingReport() {
		try {
			Map<Integer, String[]> branchHREmailIds = getBranchHREmailIds();

			if (branchHREmailIds.size() > 0) {

				String previousDate = getPreviousDate();
				Map<Object, ShiftDetail> shiftDetailsMap = getShiftDetails();

				for (Iterator<Integer> iterator = branchHREmailIds.keySet().iterator(); iterator.hasNext();) {

					Integer branchId = iterator.next();
					String[] hrEmailIds = branchHREmailIds.get(branchId);

					StringBuilder sb = new StringBuilder();
					boolean isSendMail = false;

					Map<Object, EmployeeAttendanceDetail> employeeDetailMap = getBranchEmployeeDetail(previousDate, branchId);

					if (employeeDetailMap.size() > 0) {

						String branchEmployees = employeeDetailMap.keySet().toString().replace("[", "").replace("]", "");

						LogUtility.writeLog("Branch Employees : [" + branchEmployees + "]");

						setEmployeeAttendanceDetails(previousDate, branchEmployees, employeeDetailMap);
						Map<Object, Object> employeesShift = getEmployeesShift(previousDate, branchEmployees);

						for (Iterator<Object> employeeDetailMapIterator = employeeDetailMap.keySet().iterator(); employeeDetailMapIterator.hasNext();) {

							Object employeeId = employeeDetailMapIterator.next();

							EmployeeAttendanceDetail employeeAttendanceDetail = employeeDetailMap.get(employeeId);
							Object employeeCode = employeeAttendanceDetail.getEmployeeCode();
							String employeeName = employeeAttendanceDetail.getEmployeeName();
							String firstInTimeAndMeridiem = employeeAttendanceDetail.getFirstInTime();
							String departmentName = employeeAttendanceDetail.getDepartmentName();

							LogUtility.writeLog("First In Time and Meridiem : " + firstInTimeAndMeridiem);

							if (firstInTimeAndMeridiem != null) {

								String[] firstInTimeAndMeridiemArr = firstInTimeAndMeridiem.trim().split(" ");

								if (firstInTimeAndMeridiemArr.length == 2) {

									String firstInTime = firstInTimeAndMeridiemArr[0];
									String firstInTimeMeridiem = firstInTimeAndMeridiemArr[1];

									LogUtility.writeLog("First In Time : [" + firstInTime + "], Meridiem : [" + firstInTimeMeridiem + "]");

									Object shiftId = employeesShift.get(employeeId);
									if (shiftId == null) {
										shiftId = employeeAttendanceDetail.getShiftId();
									}

									LogUtility.writeLog("Shift Id : " + shiftId);

									ShiftDetail shiftDetail = shiftDetailsMap.get(shiftId);

									String shiftFromTime = shiftDetail.getFromTime();
									String shiftFromTimeMeridiem = shiftDetail.getFromTimeMeridiem();

									LogUtility.writeLog("Shift From Time : [" + shiftFromTime + "], Meridiem : [" + shiftFromTimeMeridiem + "]");

									Calendar attendanceCalendar = getCalendar(firstInTime, firstInTimeMeridiem);
									Calendar shiftCalendar = getCalendar(shiftFromTime, shiftFromTimeMeridiem);

									if (attendanceCalendar.after(shiftCalendar)) {
										addLateComingDetail(sb, employeeCode, employeeName, shiftFromTime + " " + shiftFromTimeMeridiem, firstInTimeAndMeridiem, departmentName);
										isSendMail = true;
									}
								}
							}
						}

						if (isSendMail) {
							sb.append("</table><br /><br />Regards,<br /><br /><b>Applane Team</b>");
							sendLateComingNotificationMail(sb, hrEmailIds, previousDate);
						}
					}
				}
			}
		} catch (Exception e) {
			LogUtility.writeError("Late Coming Report Mail : " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private void addLateComingDetail(StringBuilder sb, Object employeeCode, String employeeName, String shiftInTime, String actualInTime, String departmentName) {

		if (sb.toString().length() <= 0) {
			sb.append("Hello Sir").append("<br /><br />");
			sb.append("Late coming information is as follows").append("<br /><br />");
			sb.append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"border: 1px solid black;\">");
			sb.append("<tr>");
			sb.append("<th width=\"160px\" bgcolor=\"99CCCC\" style=\"border-bottom: 1px solid black; border-right: 1px solid black; text-align: left; padding-left: 5px;\">");
			sb.append("Employee Code");
			sb.append("</th>");
			sb.append("<th width=\"160px\" bgcolor=\"99CCCC\" style=\"border-bottom: 1px solid black; border-right: 1px solid black; text-align: left; padding-left: 5px;\">");
			sb.append("Employee Name");
			sb.append("</th>");
			sb.append("<th width=\"160px\" bgcolor=\"99CCCC\" style=\"border-bottom: 1px solid black; border-right: 1px solid black; \">");
			sb.append("Department");
			sb.append("</th>");
			sb.append("<th width=\"140px\" bgcolor=\"99CCCC\" style=\"border-bottom: 1px solid black; border-right: 1px solid black; \">");
			sb.append("Shift In Time");
			sb.append("</th>");
			sb.append("<th width=\"140px\" bgcolor=\"99CCCC\" style=\"border-bottom: 1px solid black; \">");
			sb.append("Actual In Time");
			sb.append("</th>");
			sb.append("</tr>");
		}

		sb.append("<tr>");
		sb.append("<td align=\"left\" style=\"border-right: 1px solid black;border-bottom: 1px solid black;padding-left: 10px;\">");
		sb.append(employeeCode);
		sb.append("</td>");
		sb.append("<td align=\"left\" style=\"border-right: 1px solid black;border-bottom: 1px solid black;padding-left: 10px;\">");
		sb.append(employeeName);
		sb.append("</td>");
		sb.append("<td align=\"center\" style=\"border-right: 1px solid black;border-bottom: 1px solid black;\">");
		sb.append(departmentName);
		sb.append("</td>");
		sb.append("<td align=\"center\" style=\"border-right: 1px solid black;border-bottom: 1px solid black;\">");
		sb.append(shiftInTime);
		sb.append("</td>");
		sb.append("<td align=\"center\" style=\"border-bottom: 1px solid black;\">");
		sb.append(actualInTime);
		sb.append("</td>");
		sb.append("</tr>");
	}

	private void sendLateComingNotificationMail(StringBuilder sb, String[] branchHREmailIds, String date) {
		try {
			ApplaneMail mailSender = new ApplaneMail();
			mailSender.setMessage("Late Coming Information - " + date, sb.toString(), true);
			mailSender.setTo(ApplicationConstants.OWNER);
			mailSender.setCc(branchHREmailIds);
			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
		} catch (ApplaneMailException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(LateComingBusinessLogic.class.getName(), e));
		}
	}

	private Calendar getCalendar(String time, String meridiem) {

		Calendar calendar = Calendar.getInstance();

		setHourAndMinute(time, calendar);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		if (meridiem.equalsIgnoreCase("AM")) {
			calendar.set(Calendar.AM_PM, Calendar.AM);
		} else {
			calendar.set(Calendar.AM_PM, Calendar.PM);
		}

		return calendar;
	}

	private void setHourAndMinute(String time, Calendar calendar) {

		String[] hourAndMinutes = time.split(":");

		if (hourAndMinutes.length == 2) {

			int hour = Integer.parseInt(hourAndMinutes[0]);
			int minute = Integer.parseInt(hourAndMinutes[1]);

			calendar.set(Calendar.HOUR, hour);
			calendar.set(Calendar.MINUTE, minute);
		}
	}

	private Map<Object, Object> getEmployeesShift(String date, String branchEmployees) throws JSONException {

		JSONArray array = new JSONArray();
		array.put(EMPLOYEE_ID);
		array.put(SHIFT_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRIS_EMPLOYEES_SHIFT_HISTORY);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, FROM_DATE + " < '" + date + "' and " + TO_DATE + " >= '" + date + "' and " + EMPLOYEE_ID + " in (" + branchEmployees + ")");

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "__key__");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.DESC);

		JSONArray orderByExpression = new JSONArray();
		orderByExpression.put(order);

		query.put(Data.Query.ORDERS, orderByExpression);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeesShiftJsonArray = object.optJSONArray(HRIS_EMPLOYEES_SHIFT_HISTORY);

		int employeesShiftLength = (employeesShiftJsonArray == null) ? 0 : employeesShiftJsonArray.length();

		Map<Object, Object> employeesShiftMap = new HashMap<Object, Object>();

		for (int i = 0; i < employeesShiftLength; i++) {

			JSONObject employeeShiftJSONObject = employeesShiftJsonArray.optJSONObject(i);

			if (employeeShiftJSONObject != null) {

				Object employeeId = employeeShiftJSONObject.opt(EMPLOYEE_ID);
				Object shiftId = employeeShiftJSONObject.opt(SHIFT_ID);

				if (!employeesShiftMap.containsKey(employeeId)) {
					employeesShiftMap.put(employeeId, shiftId);
				}
			}
		}

		return employeesShiftMap;
	}

	private void setEmployeeAttendanceDetails(String date, String branchEmployees, Map<Object, EmployeeAttendanceDetail> employeeDetailMap) throws JSONException {

		JSONArray array = new JSONArray();
		array.put(EMPLOYEE_ID);
		array.put(FIRST_IN_TIME);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, EMPLOYEE_ATTENDANCE);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "attendancedate <= '" + date + "' and attendancedate >= '" + date + "' and " + EMPLOYEE_ID + " in (" + branchEmployees + ")");

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray employeeAttendanceJsonArray = object.optJSONArray(EMPLOYEE_ATTENDANCE);

		int employeeAttendanceLength = (employeeAttendanceJsonArray == null) ? 0 : employeeAttendanceJsonArray.length();

		for (int i = 0; i < employeeAttendanceLength; i++) {

			JSONObject employeeAttendanceJSONObject = employeeAttendanceJsonArray.optJSONObject(i);

			if (employeeAttendanceJSONObject != null) {

				Object employeeId = employeeAttendanceJSONObject.opt(EMPLOYEE_ID);
				String firstInTime = employeeAttendanceJSONObject.optString(FIRST_IN_TIME);

				EmployeeAttendanceDetail employeeAttendanceDetail = employeeDetailMap.get(employeeId);
				employeeAttendanceDetail.setFirstInTime(firstInTime);
			}
		}
	}

	private Map<Object, EmployeeAttendanceDetail> getBranchEmployeeDetail(String date, Integer branchId) throws JSONException {

		JSONArray array = new JSONArray();
		array.put(Data.Query.KEY);
		array.put(EMPLOYEE_NAME);
		array.put(EMPLOYEE_CODE);
		array.put(EMPLOYEE_DEPARTMENT);
		array.put(EMPLOYEES_SHIFT_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRIS_EMPLOYEES);
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, BRANCH_ID + " = " + branchId);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray branchEmployeesJSONArray = object.optJSONArray(HRIS_EMPLOYEES);

		int noOfEmployees = (branchEmployeesJSONArray == null) ? 0 : branchEmployeesJSONArray.length();

		Map<Object, EmployeeAttendanceDetail> employeeDetailMap = new HashMap<Object, EmployeeAttendanceDetail>();

		for (int i = 0; i < noOfEmployees; i++) {

			JSONObject branchEmployeeJsonObject = branchEmployeesJSONArray.optJSONObject(i);

			if (branchEmployeeJsonObject != null) {

				Object employeeId = branchEmployeeJsonObject.opt(Data.Query.KEY);
				String employeeName = branchEmployeeJsonObject.optString(EMPLOYEE_NAME);
				Object employeeCode = branchEmployeeJsonObject.opt(EMPLOYEE_CODE);
				String departmentName = branchEmployeeJsonObject.optString(EMPLOYEE_DEPARTMENT);
				Object shiftId = branchEmployeeJsonObject.opt(SHIFT_ID);

				EmployeeAttendanceDetail employeeAttendanceDetail = new EmployeeAttendanceDetail();
				employeeAttendanceDetail.setEmployeeId(employeeId);
				employeeAttendanceDetail.setEmployeeName(employeeName);
				employeeAttendanceDetail.setEmployeeCode(employeeCode);
				employeeAttendanceDetail.setShiftId(shiftId);
				employeeAttendanceDetail.setDepartmentName(departmentName);

				employeeDetailMap.put(employeeId, employeeAttendanceDetail);
			}
		}

		return employeeDetailMap;
	}

	private String getPreviousDate() {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);

		Date date = calendar.getTime();
		String dateStr = dateFormat.format(date);

		LogUtility.writeLog("Previous Date : [" + dateStr + "]");

		return dateStr;
	}

	private Map<Integer, String[]> getBranchHREmailIds() throws Exception {

		JSONArray array = new JSONArray();
		array.put(Data.Query.KEY);
		array.put(new JSONObject().put(HR_EMAIL_IDS, new JSONArray().put(OFFICIAL_EMAIL_ID)));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRIS_BRANCHES);
		query.put(Data.Query.COLUMNS, array);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray hrisBranchesJsonArray = object.optJSONArray(HRIS_BRANCHES);

		LogUtility.writeLog("HRIS Branches Json Array : [" + hrisBranchesJsonArray + "]");

		int noOfBranches = hrisBranchesJsonArray == null ? 0 : hrisBranchesJsonArray.length();

		LogUtility.writeLog("No of Branches : [" + noOfBranches + "]");

		Map<Integer, String[]> branchHREmailIdsMapping = new HashMap<Integer, String[]>();

		for (int i = 0; i < noOfBranches; i++) {

			JSONObject jsonObject = hrisBranchesJsonArray.optJSONObject(i);

			Integer branchId = jsonObject.optInt(Data.Query.KEY);
			JSONArray hrEmailIdsJsonArray = jsonObject.optJSONArray(HR_EMAIL_IDS);

			LogUtility.writeLog("Branch Id : [" + branchId + "], HR Email Ids Json Array : [" + hrEmailIdsJsonArray + "]");

			int hrEmailIdsLength = hrEmailIdsJsonArray == null ? 0 : hrEmailIdsJsonArray.length();

			LogUtility.writeLog("HR Email Ids Length : [" + hrEmailIdsLength + "]");

			List<String> hrEmailIdsList = new ArrayList<String>();

			for (int j = 0; j < hrEmailIdsLength; j++) {

				JSONObject hrEmailIdJSONObject = hrEmailIdsJsonArray.optJSONObject(i);

				LogUtility.writeLog("HR Email Id Json Object : [" + hrEmailIdJSONObject + "]");

				if (hrEmailIdJSONObject != null) {
					String hrOfficialEMailId = hrEmailIdJSONObject.optString(OFFICIAL_EMAIL_ID, null);
					if (hrOfficialEMailId != null) {
						hrEmailIdsList.add(hrOfficialEMailId);
					}
				}
			}

			LogUtility.writeLog("HR Email Ids List : " + hrEmailIdsList);

			if (hrEmailIdsList.size() > 0) {
				String[] hrEmailIds = new String[hrEmailIdsList.size()];
				hrEmailIdsList.toArray(hrEmailIds);

				branchHREmailIdsMapping.put(branchId, hrEmailIds);
			}
		}

		LogUtility.writeLog("Branch HR Email Ids Mapping : " + branchHREmailIdsMapping);

		return branchHREmailIdsMapping;
	}

	private Map<Object, ShiftDetail> getShiftDetails() throws JSONException {

		JSONArray array = new JSONArray();
		array.put(Data.Query.KEY);
		array.put(FROMTIME_TIME);
		array.put(FROMTIME_MERIDIEM);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRIS_SHIFTS);
		query.put(Data.Query.COLUMNS, array);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray hrisShiftsJSONArray = object.optJSONArray(HRIS_SHIFTS);

		int noOfShifts = (hrisShiftsJSONArray == null) ? 0 : hrisShiftsJSONArray.length();

		Map<Object, ShiftDetail> shiftDetailMap = new HashMap<Object, ShiftDetail>();

		for (int i = 0; i < noOfShifts; i++) {

			JSONObject shiftJsonObject = hrisShiftsJSONArray.optJSONObject(i);

			if (shiftJsonObject != null) {

				Object shiftId = shiftJsonObject.opt(Data.Query.KEY);

				String fromTime = shiftJsonObject.optString(FROMTIME_TIME);
				String fromTimeMeridiem = shiftJsonObject.optString(FROMTIME_MERIDIEM);

				ShiftDetail shiftDetail = new ShiftDetail();
				shiftDetail.setId(shiftId);
				shiftDetail.setFromTime(fromTime);
				shiftDetail.setFromTimeMeridiem(fromTimeMeridiem);

				shiftDetailMap.put(shiftId, shiftDetail);
			}
		}

		return shiftDetailMap;
	}

	class ShiftDetail {

		Object id;

		String fromTime;

		String fromTimeMeridiem;

		public Object getId() {
			return id;
		}

		public void setId(Object id) {
			this.id = id;
		}

		public String getFromTime() {
			return fromTime;
		}

		public void setFromTime(String fromTime) {
			this.fromTime = fromTime;
		}

		public String getFromTimeMeridiem() {
			return fromTimeMeridiem;
		}

		public void setFromTimeMeridiem(String fromTimeMeridiem) {
			this.fromTimeMeridiem = fromTimeMeridiem;
		}

		@Override
		public String toString() {
			return "Id : " + id + ", From Time : " + fromTime + ", From Time Meridiem : " + fromTimeMeridiem;
		}
	}

	class EmployeeAttendanceDetail {

		private Object employeeId;

		private Object employeeCode;

		private String employeeName;

		private String firstInTime;

		private Object shiftId;

		private String departmentName;

		public Object getEmployeeId() {
			return employeeId;
		}

		public void setEmployeeId(Object employeeId) {
			this.employeeId = employeeId;
		}

		public Object getEmployeeCode() {
			return employeeCode;
		}

		public void setEmployeeCode(Object employeeCode) {
			this.employeeCode = employeeCode;
		}

		public String getEmployeeName() {
			return employeeName;
		}

		public void setEmployeeName(String employeeName) {
			this.employeeName = employeeName;
		}

		public String getFirstInTime() {
			return firstInTime;
		}

		public void setFirstInTime(String firstInTime) {
			this.firstInTime = firstInTime;
		}

		public Object getShiftId() {
			return shiftId;
		}

		public void setShiftId(Object shiftId) {
			this.shiftId = shiftId;
		}

		public String getDepartmentName() {
			return departmentName;
		}

		public void setDepartmentName(String departmentName) {
			this.departmentName = departmentName;
		}

		@Override
		public String toString() {
			return "Employee Id : " + employeeId + ", First In Time : " + firstInTime;
		}
	}

	public static void main(String[] args) {

		boolean isSendMail = false;
		StringBuilder sb = new StringBuilder();

		String[] employeeCodes = { "Code1", "Code2", "Code3" };
		String[] employeeNames = { "Name1", "Name2", "Name3" };
		String[] shiftInTimes = { "9:00", "10:00", "11:00" };
		String[] shiftInTimesMeridiam = { "AM", "AM", "AM" };
		String[] firstInTimes = { "9:10 AM", "10:10 AM", "11:10 AM" };

		for (int i = 0; i < firstInTimes.length; i++) {

			String employeeCode = employeeCodes[i];

			String[] firstInTimeAndMeridiem = firstInTimes[i].trim().split(" ");

			if (firstInTimeAndMeridiem.length == 2) {

				String firstInTime = firstInTimeAndMeridiem[0];
				String firstInTimeMeridiem = firstInTimeAndMeridiem[1];

				String shiftFromTime = shiftInTimes[i];
				String shiftFromTimeMeridiem = shiftInTimesMeridiam[i];

				LateComingBusinessLogic lateComingBusinessLogic = new LateComingBusinessLogic();

				Calendar attendanceCalendar = lateComingBusinessLogic.getCalendar(firstInTime, firstInTimeMeridiem);
				Calendar shiftCalendar = lateComingBusinessLogic.getCalendar(shiftFromTime, shiftFromTimeMeridiem);

				if (attendanceCalendar.after(shiftCalendar)) {
					lateComingBusinessLogic.addLateComingDetail(sb, employeeCode, employeeNames[i], shiftFromTime, firstInTime, "Marketing");
					isSendMail = true;
				}
			}
		}

		if (isSendMail) {
			sb.append("</table><br /><br />Regards,<br /><br /><b>Applane Team</b>");
		}

		System.out.println(sb);
	}
}
