package com.applane.resourceoriented.hris.greeting;

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

public class GreetingMailBusinessLogic {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private static final String NOTIFICATION_TO_HR = "Notification To HR";

	private static final String NOTIFICATION_TO_EMPLOYEE = "Notification To Employee";

	private static final String BIRTHDAY_IMAGE_FILTER = HrisKinds.GreetingConfiguration.GREETING_TYPE + " = '" + HrisKinds.GreetingConfiguration.GreetingTypes.BIRTHDAY + "'";

	private static final String ANNIVERSARY_IMAGE_FILTER = HrisKinds.GreetingConfiguration.GREETING_TYPE + " = '" + HrisKinds.GreetingConfiguration.GreetingTypes.ANNIVERSARY + "'";

	private static final String ACTIVE_EMPLOYEE_FILTER = HrisKinds.Employess.EMPLOYEE_STATUS + "=1";

	private static final String BIRTHDAY_SUBJECT = "Happy Birthday - ";

	private static final String BIRTHDAY_WISH_MESSAGE = "Wish you a very Happy Birthday!!!";

	private static final String ANNIVERSARY_SUBJECT = "Happy Wedding Anniversary - ";

	private static final String ANNIVERSARY_WISH_MESSAGE = "Many many heartiest congratulations on your Wedding Anniversary!!!";

	private static enum GreetingType {
		Birthday, Anniversary
	};

	private static final int NAVIGANT_ORGANIZATION_ID = 7783;

	public void mailGreetings() {
		try {
			String currentDateStr = getCurrentDateStr();

			int year = Calendar.getInstance().get(Calendar.YEAR);
			LogUtility.writeLog("Year : [" + year + "]");

			String filterDate = currentDateStr.replace(year + "", "%");
			LogUtility.writeLog("Filter Date : [" + filterDate + "]");

			String birthdayFilter = HrisKinds.Employess.DATE_OF_BIRTH + " LIKE '" + filterDate + "'";
			List<EmployeeDetail> birthdayEmployeesList = getEmployees(birthdayFilter);

			String anniversaryFilter = HrisKinds.Employess.ANNIVERSARY_DATE + " LIKE '" + filterDate + "'";
			List<EmployeeDetail> anniversaryEmployeesList = getEmployees(anniversaryFilter);

			if (birthdayEmployeesList.size() > 0 || anniversaryEmployeesList.size() > 0) {

				String greetingNotificationSendTo = getGreetingNotificationSendTo();

				if (greetingNotificationSendTo.equals(NOTIFICATION_TO_EMPLOYEE)) {

					String organizationName = Translator.stringValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION));

					LogUtility.writeLog("Organization Name : [" + organizationName + "]");

					sendGreetingMailToEmployee(birthdayEmployeesList, GreetingType.Birthday, organizationName);
					sendGreetingMailToEmployee(anniversaryEmployeesList, GreetingType.Anniversary, organizationName);

				} else if (greetingNotificationSendTo.equals(NOTIFICATION_TO_HR)) {

					int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));

					Map<Integer, String[]> branchHREmailIds = null;

					if (organizationId == NAVIGANT_ORGANIZATION_ID) {
						branchHREmailIds = getNavigantBranchHREmailIds();
					} else {
						branchHREmailIds = getOtherBranchHREmailIds();
					}

					Map<Integer, List<EmployeeDetail>> branchWiseBirthdayEmployee = getBranchWiseEmployee(birthdayEmployeesList);
					Map<Integer, List<EmployeeDetail>> branchWiseAnniversaryEmployee = getBranchWiseEmployee(anniversaryEmployeesList);

					for (Iterator<Integer> iterator = branchHREmailIds.keySet().iterator(); iterator.hasNext();) {

						Integer branchId = iterator.next();

						List<EmployeeDetail> branchWiseBirthdayEmployeeList = branchWiseBirthdayEmployee.get(branchId);
						List<EmployeeDetail> branchWiseAnniversaryEmployeeList = branchWiseAnniversaryEmployee.get(branchId);

						String hrMailGreetingMessage = getHrMailGreetingMessage(branchWiseBirthdayEmployeeList, branchWiseAnniversaryEmployeeList);

						if (hrMailGreetingMessage != null) {

							String[] hrEmailIds = branchHREmailIds.get(branchId);
							sendMail("Greeting Detail - " + currentDateStr, hrMailGreetingMessage, hrEmailIds);
						}
					}
				}
			}
		} catch (Exception e) {
			LogUtility.writeError("Greeting Report Mail : " + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
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

		return branchWiseEmployeesMap;
	}

	private void sendGreetingMailToEmployee(List<EmployeeDetail> employeeList, GreetingType greetingType, String organizationName) throws JSONException {

		if (employeeList.size() > 0) {

			String imageFilter = BIRTHDAY_IMAGE_FILTER;

			if (greetingType == GreetingType.Anniversary) {
				imageFilter = ANNIVERSARY_IMAGE_FILTER;
			}

			List<String> imageUrlList = getImageUrl(imageFilter);
			int noOfImages = (imageUrlList == null) ? 0 : imageUrlList.size();

			for (Iterator<EmployeeDetail> iterator = employeeList.iterator(); iterator.hasNext();) {

				EmployeeDetail employeeDetail = iterator.next();

				String employeeName = employeeDetail.getName();
				String officialEmailId = employeeDetail.getOfficialEmailId();
				String imageUrl = null;

				if (noOfImages > 0) {
					int randomIndex = getRandomIndex(noOfImages);
					imageUrl = imageUrlList.get(randomIndex);
				}

				String greetingMessage = "";
				String subject = "";

				if (greetingType == GreetingType.Birthday) {
					greetingMessage = getEmployeeMailGreetingMessage(employeeName, imageUrl, BIRTHDAY_WISH_MESSAGE, organizationName);
					subject = BIRTHDAY_SUBJECT + employeeName;
				} else {
					greetingMessage = getEmployeeMailGreetingMessage(employeeName, imageUrl, ANNIVERSARY_WISH_MESSAGE, organizationName);
					subject = ANNIVERSARY_SUBJECT + employeeName;
				}

				sendMail(subject, greetingMessage, officialEmailId);
			}
		}
	}

	private List<String> getImageUrl(String filter) throws JSONException {

		JSONArray columns = new JSONArray();
		columns.put(HrisKinds.GreetingConfiguration.IMAGE_URL);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.GREETING_CONFIGURATION);
		query.put(Data.Query.FILTERS, filter);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);

		LogUtility.writeLog("Greeting Configuration Result : " + result);

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

	private String getHrMailGreetingMessage(List<EmployeeDetail> branchWiseBirthdayEmployeeList, List<EmployeeDetail> branchWiseAnniversaryEmployeeList) {

		int noOfBirthdayEmployees = (branchWiseBirthdayEmployeeList == null) ? 0 : branchWiseBirthdayEmployeeList.size();
		int noOfAnniversaryEmployees = (branchWiseAnniversaryEmployeeList == null) ? 0 : branchWiseAnniversaryEmployeeList.size();

		if (noOfBirthdayEmployees <= 0 && noOfAnniversaryEmployees <= 0) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Dear Sir/Mam");
		sb.append("<br /><br />");
		sb.append("Greeting details are as follows");
		sb.append("<br /><br />");

		if (noOfBirthdayEmployees > 0) {

			sb.append("<h3>Birthday Details</h3>");
			sb.append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"border: 1px solid black;\">");
			sb.append("<tr>");
			sb.append("<th width=\"120px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Employee Code");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Employee Name");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black;\">");
			sb.append("Email Id");
			sb.append("</th>");
			sb.append("</tr>");

			for (Iterator<EmployeeDetail> iterator = branchWiseBirthdayEmployeeList.iterator(); iterator.hasNext();) {

				EmployeeDetail employeeDetail = iterator.next();

				String employeeCode = employeeDetail.getEmployeeCode();
				String name = employeeDetail.getName();
				String officialEmailId = employeeDetail.getOfficialEmailId();

				sb.append("<tr>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(employeeCode);
				sb.append("</td>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(name);
				sb.append("</td>");
				sb.append("<td align=\"center\">");
				sb.append(officialEmailId);
				sb.append("</td>");
				sb.append("</tr>");
			}

			sb.append("</table>");
			sb.append("<br /><br />");
		}

		if (noOfAnniversaryEmployees > 0) {

			sb.append("<h3>Anniversary Details</h3>");
			sb.append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"border: 1px solid black;\">");
			sb.append("<tr>");
			sb.append("<th width=\"120px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Employee Code");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black; border-right: 1px solid black;\">");
			sb.append("Employee Name");
			sb.append("</th>");
			sb.append("<th width=\"200px\" style=\"border-bottom: 1px solid black;\">");
			sb.append("Email Id");
			sb.append("</th>");
			sb.append("</tr>");

			for (Iterator<EmployeeDetail> iterator = branchWiseAnniversaryEmployeeList.iterator(); iterator.hasNext();) {

				EmployeeDetail employeeDetail = iterator.next();

				String employeeCode = employeeDetail.getEmployeeCode();
				String name = employeeDetail.getName();
				String officialEmailId = employeeDetail.getOfficialEmailId();

				sb.append("<tr>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(employeeCode);
				sb.append("</td>");
				sb.append("<td align=\"center\" style=\"border-right: 1px solid black;\">");
				sb.append(name);
				sb.append("</td>");
				sb.append("<td align=\"center\">");
				sb.append(officialEmailId);
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

	private String getCurrentDateStr() {

		Calendar calendar = Calendar.getInstance();

		Date date = calendar.getTime();
		String dateStr = DATE_FORMAT.format(date);

		LogUtility.writeLog("Current Formatted Date : [" + dateStr + "]");

		return dateStr;
	}

	private List<EmployeeDetail> getEmployees(String greetingFilter) throws JSONException {

		JSONArray columnArray = new JSONArray();
		columnArray.put(HrisKinds.Employess.NAME);
		columnArray.put(HrisKinds.Employess.BRANCH_ID);
		columnArray.put(HrisKinds.Employess.OFFICIAL_EMAIL_ID);
		columnArray.put(HrisKinds.Employess.EMPLOYEE_CODE);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		query.put(Data.Query.FILTERS, ACTIVE_EMPLOYEE_FILTER + " AND " + greetingFilter);

		JSONObject result = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);

		LogUtility.writeLog("Greeting Filter : " + greetingFilter);
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

			EmployeeDetail employeeDetail = new EmployeeDetail();
			employeeDetail.setName(name);
			employeeDetail.setBranchId(branchId);
			employeeDetail.setOfficialEmailId(officialEmailId);
			employeeDetail.setEmployeeCode(employeeCode);

			employeeDetailList.add(employeeDetail);
		}

		return employeeDetailList;
	}

	private String getGreetingNotificationSendTo() throws Exception {

		JSONArray columnArray = new JSONArray();
		columnArray.put(HrisKinds.MailConfigurations.EMPLOYEE_GREETING_NOTIFICATION_SEND_TO);

		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.MAIL_CONFIGURATIONS);
		query.put(Data.Query.COLUMNS, columnArray);

		JSONObject object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray mailConfigurationJsonArray = object.optJSONArray(HrisKinds.MAIL_CONFIGURATIONS);

		LogUtility.writeLog("Mail Configuration Json Array : [" + mailConfigurationJsonArray + "]");

		String greetingNotificationSendTo = null;

		if (mailConfigurationJsonArray != null && mailConfigurationJsonArray.length() > 0) {

			JSONObject jsonObject = mailConfigurationJsonArray.getJSONObject(0);
			greetingNotificationSendTo = jsonObject.optString(HrisKinds.MailConfigurations.EMPLOYEE_GREETING_NOTIFICATION_SEND_TO, null);
		}

		LogUtility.writeLog("Greeting Notification Send To : [" + greetingNotificationSendTo + "]");

		if (greetingNotificationSendTo == null || greetingNotificationSendTo.length() <= 0) {
			greetingNotificationSendTo = NOTIFICATION_TO_HR;
		}

		return greetingNotificationSendTo;
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
	}

	public static void main(String[] args) {

	}
}
