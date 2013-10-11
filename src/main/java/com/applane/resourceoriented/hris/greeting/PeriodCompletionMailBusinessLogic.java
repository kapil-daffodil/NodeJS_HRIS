package com.applane.resourceoriented.hris.greeting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;

public class PeriodCompletionMailBusinessLogic {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private static final String NOTIFICATION_TO_HR = "Notification To HR";

	private static final String NOTIFICATION_TO_EMPLOYEE = "Notification To Employee";

	private static final String PERIOD_COMPLETION_IMAGE_FILTER = HrisKinds.GreetingConfiguration.GREETING_TYPE + " = '" + HrisKinds.GreetingConfiguration.GreetingTypes.PERIOD_COMPLETION + "'";

	private static final String ORGANIZATION_NAME_TO_REPLACE = "{OrganizationName}";

	private static final String YEARS_COMPLETED_IN_STRING = "{YearsCompletedInString}";

	private static final String ACTIVE_EMPLOYEE_FILTER = HrisKinds.Employess.EMPLOYEE_STATUS + "=1";

	private static final String SUBJECT = "Congratulations On Completing " + YEARS_COMPLETED_IN_STRING + " Successfully With " + ORGANIZATION_NAME_TO_REPLACE + " Family!!!";

	private static final String MESSAGE = "Congratulations on completing " + YEARS_COMPLETED_IN_STRING + " successfully with us.<br /><br />" + ORGANIZATION_NAME_TO_REPLACE + " management wants to take this opportunity to appreciate your valuable contribution in the growth of the organization. With your loyalty, commitment and application; you have helped us reach new heights and also have set an example for other employees as well. Thank you so much for your efforts.<br /><br />You are an asset to our organization. Keep it up with your outstanding performance.<br /><br />We appreciate your hard work and contribution and expect that you will continue to excel and contribute to the growth of " + ORGANIZATION_NAME_TO_REPLACE
			+ ".<br /><br />We wish you all the best for your future years in this organization.";

	private static final int NAVIGANT_ORGANIZATION_ID = 7783;

	public void mailPeriodCompletion() {
		try {
			Date minJoiningDate = getMinJoiningDateActiveEmp();

			if (minJoiningDate != null) {

				String filterQueryString = getFilterQueryString(minJoiningDate);

				List<EmployeeDetail> periodCompletedEmployeesList = getPeriodCompletedEmployees(filterQueryString);
				LogUtility.writeLog("Period Completed Employees List : " + periodCompletedEmployeesList);

				String periodCompletionNotificationSendTo = getPeriodCompletionNotificationSendTo();

				if (periodCompletionNotificationSendTo.equals(NOTIFICATION_TO_EMPLOYEE)) {

					String organizationName = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION));
					LogUtility.writeLog("Organization Name : [" + organizationName + "]");

					sendPeriodCompletionMailToEmployee(periodCompletedEmployeesList, organizationName);

				} else if (periodCompletionNotificationSendTo.equals(NOTIFICATION_TO_HR)) {

					int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));

					Map<Integer, String[]> branchHREmailIds = null;

					if (organizationId == NAVIGANT_ORGANIZATION_ID) {
						branchHREmailIds = getNavigantBranchHREmailIds();
					} else {
						branchHREmailIds = getOtherBranchHREmailIds();
					}

					Map<Integer, List<EmployeeDetail>> branchWiseEmployees = getBranchWiseEmployee(periodCompletedEmployeesList);

					for (Iterator<Integer> iterator = branchHREmailIds.keySet().iterator(); iterator.hasNext();) {

						Integer branchId = iterator.next();

						List<EmployeeDetail> branchWiseEmployeeList = branchWiseEmployees.get(branchId);

						String hrMailGreetingMessage = getHrMailGreetingMessage(branchWiseEmployeeList);

						if (hrMailGreetingMessage != null) {

							String[] hrEmailIds = branchHREmailIds.get(branchId);
							sendMail("Period Completion Detail : " + getCurrentDateStr(), hrMailGreetingMessage, hrEmailIds);
						}
					}
				}
			}
		} catch (Exception e) {
			LogUtility.writeError("Period Completion Report Mail : " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private String getCurrentDateStr() {

		Calendar calendar = Calendar.getInstance();

		Date date = calendar.getTime();
		String dateStr = DATE_FORMAT.format(date);

		LogUtility.writeLog("Current Formatted Date : [" + dateStr + "]");

		return dateStr;
	}

	private String getNoOfYearsCompleted(Date joiningDate) {

		Calendar joiningDateCalendar = Calendar.getInstance();
		joiningDateCalendar.setTime(joiningDate);

		Calendar calendar = Calendar.getInstance();

		double diffInYears = calendar.get(Calendar.YEAR) - joiningDateCalendar.get(Calendar.YEAR);
		double diffInMonths = calendar.get(Calendar.MONTH) - joiningDateCalendar.get(Calendar.MONTH);

		String noOfYears = "";
		if (diffInYears > 0) {
			noOfYears = (int) diffInYears + ((diffInYears > 1) ? " years " : " year ");
		}

		String noOfMonths = "";
		if (diffInMonths > 0) {
			noOfMonths = (int) diffInMonths + " months";
		}

		String totalTimeCompleted = noOfYears + noOfMonths;
		LogUtility.writeLog("Time Completed : " + totalTimeCompleted);

		return totalTimeCompleted;
	}

	private Map<Integer, List<EmployeeDetail>> getBranchWiseEmployee(List<EmployeeDetail> employeeDetails) {

		Map<Integer, List<EmployeeDetail>> branchWiseEmployeesMap = new HashMap<Integer, List<EmployeeDetail>>();

		for (Iterator<EmployeeDetail> iterator = employeeDetails.iterator(); iterator.hasNext();) {

			EmployeeDetail employeeDetail = iterator.next();

			Integer branchId = employeeDetail.getBranchId();

			List<EmployeeDetail> list = branchWiseEmployeesMap.get(branchId);

			if (list == null) {
				list = new ArrayList<EmployeeDetail>();
				branchWiseEmployeesMap.put(branchId, list);
			}

			list.add(employeeDetail);
		}

		LogUtility.writeLog("Branch Wise Employee Map : " + branchWiseEmployeesMap);

		return branchWiseEmployeesMap;
	}

	private void sendPeriodCompletionMailToEmployee(List<EmployeeDetail> employeeList, String organizationName) throws JSONException {

		if (employeeList.size() > 0) {

			List<String> imageUrlList = getImageUrl();
			int noOfImages = (imageUrlList == null) ? 0 : imageUrlList.size();

			for (Iterator<EmployeeDetail> iterator = employeeList.iterator(); iterator.hasNext();) {

				EmployeeDetail employeeDetail = iterator.next();

				String employeeName = employeeDetail.getName();
				String officialEmailId = employeeDetail.getOfficialEmailId();
				Date joiningDate = employeeDetail.getJoiningDate();

				String imageUrl = null;
				if (noOfImages > 0) {
					int randomIndex = getRandomIndex(noOfImages);
					imageUrl = imageUrlList.get(randomIndex);
				}

				String yearsCompletedInString = getNoOfYearsCompleted(joiningDate);

				String message = MESSAGE.replace(ORGANIZATION_NAME_TO_REPLACE, organizationName);
				message = message.replace(YEARS_COMPLETED_IN_STRING, yearsCompletedInString);
				message = getEmployeeMailGreetingMessage(employeeName, imageUrl, message, organizationName);

				String subject = SUBJECT.replace(ORGANIZATION_NAME_TO_REPLACE, organizationName);
				subject = subject.replace(YEARS_COMPLETED_IN_STRING, yearsCompletedInString);

				sendMail(subject, message, officialEmailId);
			}
		}
	}

	private List<String> getImageUrl() throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.GreetingConfiguration.IMAGE_URL);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.GREETING_CONFIGURATION);
		query.put(Data.Query.FILTERS, PERIOD_COMPLETION_IMAGE_FILTER);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);

		LogUtility.writeLog("Period Completion Configuration Result : " + result);

		JSONArray resultJsonArray = result.getJSONArray(HrisKinds.GREETING_CONFIGURATION);

		int noOfImages = (resultJsonArray == null) ? 0 : resultJsonArray.length();
		List<String> imageUrlList = new ArrayList<String>();

		for (int i = 0; i < noOfImages; i++) {
			JSONObject jsonObject = resultJsonArray.optJSONObject(i);
			String imageUrl = jsonObject.optString(HrisKinds.GreetingConfiguration.IMAGE_URL);
			if (imageUrl != null && imageUrl.length() > 0) {
				imageUrlList.add(imageUrl);
			}
		}

		return imageUrlList;
	}

	private int getRandomIndex(int maxSize) {

		double random = Math.random();

		LogUtility.writeLog("Random No. : [" + random + "]");

		int randomIndex = (int) (random * maxSize);

		return randomIndex;
	}

	private String getEmployeeMailGreetingMessage(String name, String imageUrl, String message, String organizationName) {

		StringBuilder sb = new StringBuilder();

		sb.append("Dear ").append(name).append("<br /><br />");
		sb.append(message);
		sb.append("<br /><br />");
		if (imageUrl != null && imageUrl.length() > 0) {
			sb.append("<p align='center' style='text-align:center'>");
			sb.append("<img src='" + imageUrl + "' />");
			sb.append("</p>");
		}
		sb.append("<br /><br />");
		sb.append("Best Wishes");
		sb.append("<br /><br />");
		sb.append("<b>" + organizationName + " Family</b>");

		return sb.toString();
	}

	private String getHrMailGreetingMessage(List<EmployeeDetail> branchWiseEmployeeList) {

		int noOfEmployees = (branchWiseEmployeeList == null) ? 0 : branchWiseEmployeeList.size();

		if (noOfEmployees <= 0) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Dear Sir/Mam");
		sb.append("<br /><br />");
		sb.append("Period completion details are as follows");
		sb.append("<br /><br />");

		if (noOfEmployees > 0) {

			sb.append("<h3>Period Completion Details</h3>");
			sb.append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"border: 1px solid black;\">");
			sb.append("<tr>");
			sb.append("<th width=\"120px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Employee Code");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Employee Name");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Email Id");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Joining Date");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black;\">");
			sb.append("Period Covered");
			sb.append("</th>");
			sb.append("</tr>");

			for (Iterator<EmployeeDetail> iterator = branchWiseEmployeeList.iterator(); iterator.hasNext();) {

				EmployeeDetail employeeDetail = iterator.next();

				String employeeCode = employeeDetail.getEmployeeCode();
				String name = employeeDetail.getName();
				String officialEmailId = employeeDetail.getOfficialEmailId();
				Date joiningDate = employeeDetail.getJoiningDate();
				String joiningDateStr = DATE_FORMAT.format(joiningDate);

				String noOfYearsCompleted = getNoOfYearsCompleted(joiningDate);

				sb.append("<tr>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(employeeCode);
				sb.append("</td>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(name);
				sb.append("</td>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(officialEmailId);
				sb.append("</td>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(joiningDateStr);
				sb.append("</td>");
				sb.append("<td align=\"center\">");
				sb.append(noOfYearsCompleted);
				sb.append("</td>");
				sb.append("</tr>");
			}

			sb.append("</table>");
		}

		sb.append("<br /><br />");
		sb.append("Regards");
		sb.append("<br /><br />");
		sb.append("<b>Applane Team</b>");

		return sb.toString();
	}

	private void sendMail(String subject, String message, String... toEmailIds) {
		try {
			LogUtility.writeLog("Message : " + message);
			LogUtility.writeLog("To Email Ids : " + Arrays.asList(toEmailIds));

			ApplaneMail mailSender = new ApplaneMail();
			mailSender.setMessage(subject, message, true);
			mailSender.setTo(toEmailIds);

			mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());

		} catch (ApplaneMailException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private String getFilterQueryString(Date minJoiningDate) {

		Calendar currentDateCalendar = Calendar.getInstance();

		Calendar minJoiningDateCalendar = Calendar.getInstance();
		minJoiningDateCalendar.setTime(minJoiningDate);

		List<String> timePeriodDates = new ArrayList<String>();

		while (currentDateCalendar.after(minJoiningDateCalendar)) {

			currentDateCalendar.add(Calendar.MONTH, -6);

			Date date = currentDateCalendar.getTime();
			String dateStr = DATE_FORMAT.format(date);
			dateStr = "'" + dateStr + "'";

			timePeriodDates.add(dateStr);
		}

		String filterQueryString = timePeriodDates.toString().replace("[", "").replace("]", "");
		LogUtility.writeLog("Filter Query String : " + filterQueryString);

		return filterQueryString;
	}

	private Date getMinJoiningDateActiveEmp() throws JSONException {

		JSONArray columnArray = new JSONArray();
		columnArray.put(HrisKinds.Employess.JOINING_DATE);

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, HrisKinds.Employess.JOINING_DATE);
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.FILTERS, ACTIVE_EMPLOYEE_FILTER);
		query.put(Data.Query.ORDERS, new JSONArray().put(order));
		query.put(Data.Query.MAX_ROWS, 1);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJsonArray = result.getJSONArray(HrisKinds.EMPLOYEES);

		int noOfEmployees = (resultJsonArray == null) ? 0 : resultJsonArray.length();

		Date minJoiningDate = null;

		if (noOfEmployees > 0) {

			JSONObject jsonObject = resultJsonArray.optJSONObject(0);
			Object joiningDate = jsonObject.opt(HrisKinds.Employess.JOINING_DATE);

			if (joiningDate != null) {
				try {
					minJoiningDate = DATE_FORMAT.parse("" + joiningDate);
				} catch (ParseException e) {
					throw new RuntimeException("Error on Parsing Joining Date : " + e.getMessage());
				}
			}
		}

		LogUtility.writeLog("Min Joining Date : [" + minJoiningDate + "]");

		return minJoiningDate;
	}

	private List<EmployeeDetail> getPeriodCompletedEmployees(String filterQueryString) throws JSONException {

		JSONArray columnArray = new JSONArray();
		columnArray.put(HrisKinds.Employess.NAME);
		columnArray.put(HrisKinds.Employess.BRANCH_ID);
		columnArray.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		columnArray.put(HrisKinds.Employess.EMPLOYEE_CODE);
		columnArray.put(HrisKinds.Employess.JOINING_DATE);

		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, HrisKinds.Employess.JOINING_DATE);
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.ORDERS, new JSONArray().put(order));
		query.put(Data.Query.FILTERS, ACTIVE_EMPLOYEE_FILTER + " AND " + HrisKinds.Employess.JOINING_DATE + " IN (" + filterQueryString + ")");

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);

		LogUtility.writeLog("Employees Result : " + result);

		JSONArray resultJsonArray = result.getJSONArray(HrisKinds.EMPLOYEES);

		int noOfEmployees = (resultJsonArray == null) ? 0 : resultJsonArray.length();
		List<EmployeeDetail> employeeDetailList = new ArrayList<EmployeeDetail>();

		for (int i = 0; i < noOfEmployees; i++) {

			JSONObject jsonObject = resultJsonArray.optJSONObject(i);

			String name = jsonObject.optString(HrisKinds.Employess.NAME);
			Integer branchId = jsonObject.optInt(HrisKinds.Employess.BRANCH_ID);
			String officialEmailId = jsonObject.optString(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
			String employeeCode = jsonObject.optString(HrisKinds.Employess.EMPLOYEE_CODE);
			Object joiningDate = jsonObject.opt(HrisKinds.Employess.JOINING_DATE);

			Date empJoiningDate = null;
			if (joiningDate != null) {
				try {
					empJoiningDate = DATE_FORMAT.parse("" + joiningDate);
				} catch (ParseException e) {
					throw new RuntimeException("Error on Parsing Joining Date : " + e.getMessage());
				}
			}

			EmployeeDetail employeeDetail = new EmployeeDetail();
			employeeDetail.setName(name);
			employeeDetail.setBranchId(branchId);
			employeeDetail.setOfficialEmailId(officialEmailId);
			employeeDetail.setEmployeeCode(employeeCode);
			employeeDetail.setJoiningDate(empJoiningDate);

			employeeDetailList.add(employeeDetail);
		}

		return employeeDetailList;
	}

	private String getPeriodCompletionNotificationSendTo() throws Exception {

		JSONArray columnArray = new JSONArray();
		columnArray.put(HrisKinds.MailConfigurations.EMPLOYEE_PERIOD_COMPLETION_NOTIFICATION_SEND_TO);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, columnArray);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray mailConfigurationJsonArray = object.optJSONArray(HrisKinds.MAIL_CONFIGURATIONS);

		LogUtility.writeLog("Mail Configuration Json Array : [" + mailConfigurationJsonArray + "]");

		String periodCompletionNotificationSendTo = null;

		if (mailConfigurationJsonArray != null && mailConfigurationJsonArray.length() > 0) {

			JSONObject jsonObject = mailConfigurationJsonArray.getJSONObject(0);
			periodCompletionNotificationSendTo = jsonObject.optString(HrisKinds.MailConfigurations.EMPLOYEE_PERIOD_COMPLETION_NOTIFICATION_SEND_TO, null);
		}

		LogUtility.writeLog("Period Completion Notification Send To : [" + periodCompletionNotificationSendTo + "]");

		if (periodCompletionNotificationSendTo == null || periodCompletionNotificationSendTo.length() <= 0) {
			periodCompletionNotificationSendTo = NOTIFICATION_TO_HR;
		}

		return periodCompletionNotificationSendTo;
	}

	public Map<Integer, String[]> getOtherBranchHREmailIds() throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(Data.Query.KEY);
		columns.put(HrisKinds.HrAssigning.EMPLOYEE_ID);
		columns.put(HrisKinds.HrAssigning.OFFICIAL_EMAIL_ID);
		columns.put(HrisKinds.HrAssigning.BRANCH_ID);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.HR_ASSIGNING);
		query.put(Data.Query.COLUMNS, columns);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray resultJSONArray = result.optJSONArray(HrisKinds.HR_ASSIGNING);

		LogUtility.writeLog("HRIS Branches Json Array : [" + resultJSONArray + "]");

		int noOfBranches = resultJSONArray == null ? 0 : resultJSONArray.length();

		LogUtility.writeLog("No of Branches : [" + noOfBranches + "]");

		Map<Integer, String[]> branchHREmailIdsMapping = new HashMap<Integer, String[]>();

		for (int i = 0; i < noOfBranches; i++) {

			JSONObject jsonObject = resultJSONArray.optJSONObject(i);

			Integer branchId = jsonObject.optInt(HrisKinds.HrAssigning.BRANCH_ID);
			String hrOfficialEMailId = jsonObject.optString(HrisKinds.HrAssigning.OFFICIAL_EMAIL_ID, null);

			List<String> hrEmailIdsList = new ArrayList<String>();

			if (hrOfficialEMailId != null) {
				hrEmailIdsList.add(hrOfficialEMailId);
			}

			LogUtility.writeLog("Branch Id : [" + branchId + "], HR Email Id : [" + hrOfficialEMailId + "]");

			if (hrEmailIdsList.size() > 0) {
				String[] hrEmailIds = new String[hrEmailIdsList.size()];
				hrEmailIdsList.toArray(hrEmailIds);

				branchHREmailIdsMapping.put(branchId, hrEmailIds);
			}
		}

		LogUtility.writeLog("Branch HR Email Ids Mapping : " + branchHREmailIdsMapping);

		return branchHREmailIdsMapping;
	}

	private Map<Integer, String[]> getNavigantBranchHREmailIds() throws Exception {

		JSONArray array = new JSONArray();
		array.put(Data.Query.KEY);
		array.put(new JSONObject().put(HrisKinds.Branches.HR_EMAIL_IDS, new JSONArray().put(HrisKinds.Employess.OFFICIAL_EMAIL_ID)));

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.BRANCHES);
		query.put(Data.Query.COLUMNS, array);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray hrisBranchesJsonArray = result.optJSONArray(HrisKinds.BRANCHES);

		LogUtility.writeLog("HRIS Branches Json Array : [" + hrisBranchesJsonArray + "]");

		int noOfBranches = hrisBranchesJsonArray == null ? 0 : hrisBranchesJsonArray.length();

		LogUtility.writeLog("No of Branches : [" + noOfBranches + "]");

		Map<Integer, String[]> branchHREmailIdsMapping = new HashMap<Integer, String[]>();

		for (int i = 0; i < noOfBranches; i++) {

			JSONObject jsonObject = hrisBranchesJsonArray.optJSONObject(i);

			Integer branchId = jsonObject.optInt(Data.Query.KEY);
			JSONArray hrEmailIdsJsonArray = jsonObject.optJSONArray(HrisKinds.Branches.HR_EMAIL_IDS);

			LogUtility.writeLog("Branch Id : [" + branchId + "], HR Email Ids Json Array : [" + hrEmailIdsJsonArray + "]");

			int hrEmailIdsLength = hrEmailIdsJsonArray == null ? 0 : hrEmailIdsJsonArray.length();

			LogUtility.writeLog("HR Email Ids Length : [" + hrEmailIdsLength + "]");

			List<String> hrEmailIdsList = new ArrayList<String>();

			for (int j = 0; j < hrEmailIdsLength; j++) {

				JSONObject hrEmailIdJSONObject = hrEmailIdsJsonArray.optJSONObject(i);

				LogUtility.writeLog("HR Email Id Json Object : [" + hrEmailIdJSONObject + "]");

				if (hrEmailIdJSONObject != null) {
					String hrOfficialEMailId = hrEmailIdJSONObject.optString(HrisKinds.Employess.OFFICIAL_EMAIL_ID, null);
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

	private class EmployeeDetail {

		private String name;

		private Integer branchId;

		private String officialEmailId;

		private String employeeCode;

		private Date joiningDate;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getBranchId() {
			return branchId;
		}

		public void setBranchId(Integer branchId) {
			this.branchId = branchId;
		}

		public String getOfficialEmailId() {
			return officialEmailId;
		}

		public void setOfficialEmailId(String officialEmailId) {
			this.officialEmailId = officialEmailId;
		}

		public String getEmployeeCode() {
			return employeeCode;
		};

		public void setEmployeeCode(String employeeCode) {
			this.employeeCode = employeeCode;
		}

		public Date getJoiningDate() {
			return joiningDate;
		}

		public void setJoiningDate(Date joiningDate) {
			this.joiningDate = joiningDate;
		}

		@Override
		public String toString() {
			return "Name : " + name + ", Branch Id : " + branchId + ", Joining Date : " + joiningDate;
		}
	}

	public static void main(String[] args) {

	}
}
