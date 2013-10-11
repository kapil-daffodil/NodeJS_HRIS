package com.applane.resourceoriented.hris;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class PunchingDataServlet extends HttpServlet implements ApplaneTask {

	private static final long serialVersionUID = 1L;
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void initiate(ApplaneTaskInfo taskInfo) throws DeadlineExceededException {

	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// JSONObject upload = UploadService.upload(req);
		readAttendanceParameters(req, resp);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void readAttendanceParameters(HttpServletRequest req, HttpServletResponse resp) {
		Map<String, Object> paramMap = req.getParameterMap();
		String params = "";
		try {
			Iterator it = paramMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				params = params + "Parameter1 is [" + pairs.getKey() + "] and value is [" + pairs.getValue() + "]\n";
			}

			if (paramMap == null || paramMap.size() == 0) {
				resp.getWriter().write("No parameter found in Attendance servlet..");
			} else {
				/*
				 * Get parameters from client program cardno, IOdate, IOTime,Gate No.,IO Status
				 */
				String cardNumber = ((String[]) paramMap.get("cardno"))[0];
				String inOutDate = ((String[]) paramMap.get("inoutdate"))[0];
				String inOutTime = ((String[]) paramMap.get("inouttime"))[0];
				String inOutGateName = ((String[]) paramMap.get("inoutgatename"))[0];
				String inOutStatus = ((String[]) paramMap.get("inoutstatus"))[0];
				String branchName = ((String[]) paramMap.get("branch"))[0];
				String organizationName = ((String[]) paramMap.get("organizationname"))[0];
				// String holderName = ((String[]) paramMap.get("holdername"))[0];
				String organizationId = ((String[]) paramMap.get("organizationid"))[0];
				// String holderNumber = ((String[]) paramMap.get("holderno"))[0];
				// String inOutGateNo = ((String[]) paramMap.get("inoutgateno"))[0];
				// String departmentNumber = ((String[]) paramMap.get("departmentnumber"))[0];
				// LogUtility.writeLog("organizationId is : >>> " + organizationId);
				// Object applicationId = 777;
				// Object applicationName = "hris_services";
				// setSession(organizationId, organizationName, applicationId, applicationName);

				/*
				 * Select __key__,name from hris_employees where deviceid='cardno',employeestatus='Active';
				 */
				// Object userId = PunchingUtility.getId("up_users", "emailid", ApplicationConstants.OWNER);
				if (Translator.integerValue(organizationId) == 7783) {
					Object userId = 25229;
					PunchingUtility.setValueInSession(organizationName, userId, ApplicationConstants.ADMIN_NAVIGANT);
				} else if (Translator.integerValue(organizationId) == 10719) {
					Object userId = 25230;
					PunchingUtility.setValueInSession(organizationName, userId, ApplicationConstants.ADMIN_GIRNAR_SOFT);
				} else {
					Object userId = 13652;
					PunchingUtility.setValueInSession(organizationName, userId, ApplicationConstants.OWNER);
				}
				JSONArray employeeArray = PunchingUtility.getEmployeeRecords(cardNumber, branchName, organizationName);
				int employeeArrayCount = (employeeArray == null || employeeArray.length() == 0) ? 0 : employeeArray.length();
				if (paramMap.get("processstatus") != null) {
					String processStatus = ((String[]) paramMap.get("processstatus"))[0];
					if (processStatus.equalsIgnoreCase("1")) {
						if (inOutDate != null && !inOutDate.equalsIgnoreCase("null")) {
							Object branchId = PunchingUtility.getId("hris_branches", "name", branchName);
							PunchingUtility.updateLastPunchDateStatus(inOutDate, branchId, organizationName);
						} else {
							sendMailInCaseOfInOutDateNull(branchName);
						}
					}
				}

				if (employeeArrayCount > 0 && inOutDate != null && !inOutDate.equalsIgnoreCase("null") && inOutTime != null && !inOutTime.equalsIgnoreCase("null") && cardNumber != null && !cardNumber.equalsIgnoreCase("null") && inOutStatus != null && !inOutStatus.equalsIgnoreCase("null")) {
					Object employeeId = employeeArray.getJSONObject(0).opt("__key__");
					String employeeOfficialEmailId = (String) employeeArray.getJSONObject(0).opt("officialemailid");
					Object employeeUserId = null;
					try {
						employeeUserId = PunchingUtility.getId("up_users", "emailid", employeeOfficialEmailId);
					} catch (Exception e) {
						sendStatus(employeeOfficialEmailId, branchName, cardNumber);
					}

					Object inOutStatusId = PunchingUtility.getInOutStatusId(inOutStatus);
					PunchingUtility.insertPunchingRecord(inOutDate, inOutTime, inOutStatusId, employeeId, employeeUserId, inOutGateName);
				} else {
					// sendStatus(null, cardNumber, branch);
				}
			}
		} catch (Exception e) {
			String traces = ExceptionUtils.getExceptionTraceMessage("PunchingDataServlet", e);
			DataTypeUtilities.sendExceptionMail(traces);
			throw new BusinessLogicException("Some error ocuured while update employee punching data");
		}
	}

	private static void sendMailInCaseOfInOutDateNull(String branch) {
		ApplaneMail mail = new ApplaneMail();
		StringBuilder builder = new StringBuilder();
		builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
		builder.append("<br><br><b>In Out Date: Null</b><br></br><b>branchId is: " + branch + "</b><hr></hr><br></br>").append("");
		mail.setMessage("Last Date of Punch update history found \"Null\" for branchId is: " + branch, builder.toString(), true);
		try {
			mail.setTo(ApplicationConstants.OWNER);
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Punch Process status's date found null.");
		}

	}

	private static void sendStatus(String officialEmailId, String branch, String cardNo) {
		if (officialEmailId != null && officialEmailId.length() > 0 && cardNo != null && cardNo.length() > 0) {
			StringBuilder message = new StringBuilder("User Account not created for this employee [" + officialEmailId + "] whose branch is [" + branch + "] and card Number is [" + cardNo + "]");
			new ApplaneMail().sendExceptionMail(new String[] { ApplicationConstants.OWNER }, null, null, "Daily Punching Data Missing Status for branch " + branch, message.toString(), true);
		} else if (branch != null && branch.length() > 0 && cardNo != null && cardNo.length() > 0) {
			StringBuilder message = new StringBuilder("No employee found for this card number [" + cardNo + "] and branch [" + branch + "]");
			new ApplaneMail().sendExceptionMail(new String[] { ApplicationConstants.OWNER }, null, null, "Daily Punching Data Missing Status for branch " + branch, message.toString(), true);
		}
	}

	public static void checkLastUpdateDurationAndSendNotification(int hours) throws Exception {
		// try {
		JSONArray branchAndHRDetails = EmployeeSalarySheetReport.getHrMailIdDetail("");
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
		Date todayDate = DataTypeUtilities.checkDateFormat(TODAY_DATE);

		Calendar cal = Calendar.getInstance();
		cal.setTime(todayDate);
		// int currentHours = cal.get(Calendar.HOUR_OF_DAY);
		for (int counter = 0; counter < branchAndHRDetails.length(); counter++) {
			Object branchId = branchAndHRDetails.getJSONObject(counter).opt("branchid");
			boolean isDisableEmail = Translator.booleanValue(branchAndHRDetails.getJSONObject(counter).opt("branchid.disable_punch_client_email"));
			JSONArray lastUpdateDaterecord = PunchingUtility.getLastDateUpdateForBranch(branchId);
			if (lastUpdateDaterecord != null && lastUpdateDaterecord.length() > 0 && !isDisableEmail) {
				Object lastUpdatedDate = lastUpdateDaterecord.getJSONObject(0).opt("datetime");
				if (lastUpdatedDate != null) {
					Object branchName = branchAndHRDetails.getJSONObject(counter).opt("branchid.name");
					String mailId = DataTypeUtilities.stringValue(branchAndHRDetails.getJSONObject(counter).opt("employeeid.officialemailid"));
					String hrName = DataTypeUtilities.stringValue(branchAndHRDetails.getJSONObject(counter).opt("employeeid.name"));
					String logo = "";// new EmployeeSalarySheetReport().getLogo();
					Date lastUpdatedDateTime = DataTypeUtilities.checkDateFormat(lastUpdatedDate);
					if (lastUpdatedDateTime != null) {
						long timeDifference = Translator.getDifferenceInMS(lastUpdatedDateTime, todayDate);
						String totalTimeInOfice = Translator.getTimeFromMillisecond(timeDifference);
						int hourOfLastUpdate = Integer.parseInt(totalTimeInOfice.split(":")[0]);
						Date todayDateTemp = Translator.dateValue(TODAY_DATE);
						Date lastUpdatedDateTimeTemp = Translator.dateValue(lastUpdatedDate);
						// LogUtility.writeLog("hourOfLastUpdate >> " + hourOfLastUpdate + " << totalTimeInOfice >> " + totalTimeInOfice + " << lastUpdatedDateTime >> " + lastUpdatedDateTime + " << todayDate >> " + todayDate + " << todayDateTemp >> " + todayDateTemp + " << lastUpdatedDateTimeTemp >> " + lastUpdatedDateTimeTemp);

						if (mailId.length() > 0 && ((todayDateTemp.after(lastUpdatedDateTimeTemp)) || (hourOfLastUpdate) >= hours)) {
							String title = "Punch Client Not Running for " + branchName;
							String[] mailIds = new String[1];
							mailIds[0] = mailId;
							String adminMail = "";
							if (branchName.equals("Suncity")) {
								adminMail = ApplicationConstants.ADMIN_SUNCITY;
							} else if (branchName.equals("Sector-33")) {
								adminMail = ApplicationConstants.ADMIN_SEC_33;
							} else if (hours == 5) {
								adminMail = ApplicationConstants.ADMIN_DARCL_HO_HISAR;
							}
							PunchingUtility.sendMailToHr(title, hrName, mailIds, "" + branchName, "" + lastUpdatedDate, logo, adminMail);
						}

					}
				}
			}
		}
		// } catch (Exception e) {
		// LogUtility.writeLog("Punching Data Servelet >> checkLastUpdateDurationAndSendNotification >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
		// throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		// }
	}

	// public static void setSession(Object organizationId, Object organizationName, Object applicationId, Object applicationName) {
	// CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID, organizationId);
	// CurrentState.setCurrentVariable(CurrentSession.CURRENT_ORGANIZATION, organizationName);
	// CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION_ID, applicationId);
	// CurrentState.setCurrentVariable(CurrentSession.CURRENT_APPLICATION, applicationName);
	// }

}
