package com.applane.resourceoriented.hris.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;

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
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.greeting.GreetingMailBusinessLogic;
import com.applane.resourceoriented.hris.greeting.PeriodCompletionMailBusinessLogic;
import com.applane.resourceoriented.hris.reports.AbsentReportOnEmailBusinessLogic;
import com.applane.resourceoriented.hris.reports.DepartmentStrengthNotificationMail;
import com.applane.resourceoriented.hris.reports.EmployeeDailyShortAttendanceReportBusinessLogic;
import com.applane.resourceoriented.hris.reports.EmployeeMonthlyShortAttendanceReportBusinessLogic;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetExceptionReportOnMail;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.reports.MonthlyAttendanceOnMail;
import com.applane.resourceoriented.hris.reports.ProbationEndingNotification;
import com.applane.resourceoriented.hris.reports.navigant.LateComingBusinessLogic;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.cron.ApplaneCron;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class MorningSchdularHRIS implements ApplaneCron, ApplaneTask {

	public static final String ORGANIZATION_COUNTER = "organizationCounter";
	public static final String PURPOSE = "purose";
	public static final String SEND_DAILY_ATTENDANCE_MAIL = "sendDailyAttendanceMail";
	public static final String SEND_DAILY_SHORT_ATTENDANCE_MAIL = "sendDailyShortAttendanceMail";
	public static final String SEND_MONTHLY_SHORT_ATTENDANCE_MAIL = "sendMonthlyShortAttendanceMail";
	public static final String SEND_MONTHLY_ATTENDANCE_MAIL = "sendMonthlytAttendanceMail";
	public static final String SEND_EXCEPTION_REPORT_MAIL = "sendExceptionReport";
	public static final String SEND_ATTENDANCE_SUMMARY = "sendAttendanceSummary";
	public static final String SEND_DEPARTMENT_STRENGTH_NOTIFICATION = "sendDepartmentStrengthNotification";
	public static final String SEND_LATE_COMING_NOTIFICATION = "sendLateComingNotification";
	public static final String SEND_PROBATION_PERIOD_END_REPORT = "sendProbationPeriodEndReport";
	public static final String SEND_GREETING_MAIL = "sendGreetingMail";
	public static final String SEND_PERIOD_COMPLETION_MAIL = "sendPeriodCompletionMail";
	public static final String SEND_ABSENT_REPORT = "disable_absent_report";
	public static final String NEXT_ORGANIZATION = "nextOrganization";

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	public void cronCall() throws DeadlineExceededException {
		try {
			String serverName = SystemProperty.applicationId.get();
			if (serverName.equalsIgnoreCase("applanecrmhrd")) {
				JSONObject taskQueueInfo = new JSONObject();
				taskQueueInfo.put(ORGANIZATION_COUNTER, 0);
				taskQueueInfo.put(PURPOSE, SEND_DAILY_ATTENDANCE_MAIL);
				schedularForAllOrganizations(taskQueueInfo);
			}
		} catch (Exception e) {
			sendMailInCaseOfException(e, "Morning Scheduler HRIS Failed");
			throw new RuntimeException();
		}
	}

	public void schedularForAllOrganizations(JSONObject taskQueueInfo) throws JSONException, ParseException {

		String[] organizations = HRISApplicationConstants.ORGANIZATIONS;
		int length = organizations.length;
		String purpose = taskQueueInfo.optString(PURPOSE);
		Object userId = 13652;
		int counter = taskQueueInfo.opt(ORGANIZATION_COUNTER) == null ? -1 : Translator.integerValue(taskQueueInfo.opt(ORGANIZATION_COUNTER));
		if (counter != -1) {

			String organization = organizations[counter];
			PunchingUtility.setValueInSession(organization, userId, ApplicationConstants.OWNER);
			EmployeeSalarySheetReport essr = new EmployeeSalarySheetReport();
			EmployeeMonthlyShortAttendanceReportBusinessLogic emsar = new EmployeeMonthlyShortAttendanceReportBusinessLogic();
			EmployeeDailyShortAttendanceReportBusinessLogic edsar = new EmployeeDailyShortAttendanceReportBusinessLogic();
			JSONArray mailConfigurationSetup = getMailConfigurationSetup();
			boolean sendDailyAttendanceMailDisabled = false;
			boolean shortAttendanceMailDisabled = false;
			boolean sendMonthlyAttendanceMailDisabled = false;
			boolean sendExceptionReportMailDisabled = false;
			boolean disableGreetingNotification = false;
			boolean disablePeriodCompletionNotification = false;
			boolean disableAbsentReport = false;

			if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
				sendExceptionReportMailDisabled = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_exception_report_mail"));
				sendDailyAttendanceMailDisabled = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_attendance_report_mail"));
				shortAttendanceMailDisabled = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_daily_short_attendance_report_mail"));
				sendMonthlyAttendanceMailDisabled = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_monthly_short_attendance_report_mail"));
				disableGreetingNotification = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt(HrisKinds.MailConfigurations.DISABLE_GREETING_NOTIFICATION_MAIL));
				disablePeriodCompletionNotification = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt(HrisKinds.MailConfigurations.DISABLE_PERIOD_COMPLETION_NOTIFICATION_MAIL));
				disableAbsentReport = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_absent_report"));
			}
			if (!sendDailyAttendanceMailDisabled && purpose.equals(SEND_DAILY_ATTENDANCE_MAIL)) {
				try {
					essr.sendYesterdayAttendanceMailToHR();
					initiateTaskQueue(counter, SEND_DAILY_SHORT_ATTENDANCE_MAIL);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_DAILY_SHORT_ATTENDANCE_MAIL);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_DAILY_ATTENDANCE_MAIL (exception)");
				}
				return;
			}
			if (sendDailyAttendanceMailDisabled && purpose.equals(SEND_DAILY_ATTENDANCE_MAIL)) {
				purpose = SEND_DAILY_SHORT_ATTENDANCE_MAIL;
			}
			if (!shortAttendanceMailDisabled && purpose.equals(SEND_DAILY_SHORT_ATTENDANCE_MAIL)) {
				try {
					edsar.sendDailyShortAttendanceReport();
					initiateTaskQueue(counter, SEND_MONTHLY_SHORT_ATTENDANCE_MAIL);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_MONTHLY_SHORT_ATTENDANCE_MAIL);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_DAILY_SHORT_ATTENDANCE_MAIL (exception)");
				}
				return;
			}
			if (shortAttendanceMailDisabled && purpose.equals(SEND_DAILY_SHORT_ATTENDANCE_MAIL)) {
				purpose = SEND_MONTHLY_SHORT_ATTENDANCE_MAIL;
			}
			if (!sendMonthlyAttendanceMailDisabled && purpose.equals(SEND_MONTHLY_SHORT_ATTENDANCE_MAIL)) {
				try {
					emsar.sendMonthlyShortAttendanceReportFromTaskQueue(false, null);
					initiateTaskQueue(counter, SEND_EXCEPTION_REPORT_MAIL);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_EXCEPTION_REPORT_MAIL);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_EXCEPTION_REPORT_MAIL (exception)");
				}
				return;
			}
			if (sendMonthlyAttendanceMailDisabled && purpose.equals(SEND_MONTHLY_SHORT_ATTENDANCE_MAIL)) {
				purpose = SEND_EXCEPTION_REPORT_MAIL;
			}
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			boolean isTodayFriday = Translator.getTodayIs(Translator.dateValue(TODAY_DATE), "friday");
			if (!sendExceptionReportMailDisabled && isTodayFriday && purpose.equals(SEND_EXCEPTION_REPORT_MAIL)) {
				try {
					EmployeeSalarySheetExceptionReportOnMail.sendExceptionReportMailByScheduler(null, counter);
					// NEXT_ORGANIZATION purpose task queue initiated in EmployeeSalarySheetExceptionReportOnMail
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_ATTENDANCE_SUMMARY);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_EXCEPTION_REPORT_MAIL (exception)");
				}
				return;
			}
			if ((sendExceptionReportMailDisabled && purpose.equals(SEND_EXCEPTION_REPORT_MAIL)) || (purpose.equals(SEND_EXCEPTION_REPORT_MAIL) && !sendExceptionReportMailDisabled && !isTodayFriday)) {
				if (counter == 0) {
					purpose = SEND_ATTENDANCE_SUMMARY;
				} else {
					purpose = SEND_MONTHLY_ATTENDANCE_MAIL;
				}
			}
			LogUtility.writeError("1 >> length >> " + length + " << counter >> " + counter + " << purpose >> " + purpose);
			if (counter == 0 && purpose.equals(SEND_ATTENDANCE_SUMMARY)) {
				try {
					// AttendanceSummaryReport.sendAttendanceSummaryReport();
					initiateTaskQueue(counter, SEND_MONTHLY_ATTENDANCE_MAIL);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_MONTHLY_ATTENDANCE_MAIL);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_ATTENDANCE_SUMMARY (exception)");
				}
				return;
			}
			// else if (counter != 0 && purpose.equals(SEND_ATTENDANCE_SUMMARY)) {
			// initiateTaskQueue(counter, SEND_MONTHLY_ATTENDANCE_MAIL);
			// return;
			// }
			LogUtility.writeError("2 >> length >> " + length + " << counter >> " + counter + " << purpose >> " + purpose);
			if (purpose.equals(SEND_MONTHLY_ATTENDANCE_MAIL)) {
				try {
					new MonthlyAttendanceOnMail().sendMonthlyAttendanceReportFromTaskQueue(false, null);
					initiateTaskQueue(counter, SEND_DEPARTMENT_STRENGTH_NOTIFICATION);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_DEPARTMENT_STRENGTH_NOTIFICATION);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_MONTHLY_ATTENDANCE_MAIL (exception)");
				}
				return;
			}

			LogUtility.writeError("3 >> length >> " + length + " << counter >> " + counter + " << purpose >> " + purpose);
			if (purpose.equals(SEND_DEPARTMENT_STRENGTH_NOTIFICATION)) {
				try {
					DepartmentStrengthNotificationMail.mailDepartmentStrengthNotification();
					initiateTaskQueue(counter, SEND_LATE_COMING_NOTIFICATION);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_LATE_COMING_NOTIFICATION);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_DEPARTMENT_NOTIFICATION (exception)");
				}
				return;
			}

			LogUtility.writeError("4 >> length >> " + length + " << counter >> " + counter + " << purpose >> " + purpose);
			if (purpose.equals(SEND_LATE_COMING_NOTIFICATION)) {
				try {
					new LateComingBusinessLogic().mailLateComingReport();
					initiateTaskQueue(counter, SEND_GREETING_MAIL);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_GREETING_MAIL);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_LATE_COMING_NOTIFICATION (exception)");
				}
				return;
			}

			if (disableGreetingNotification && purpose.equals(SEND_GREETING_MAIL)) {
				purpose = SEND_PERIOD_COMPLETION_MAIL;
			}

			LogUtility.writeError("5 >> length >> " + length + " << counter >> " + counter + " << purpose >> " + purpose);
			if (purpose.equals(SEND_GREETING_MAIL)) {
				try {
					new GreetingMailBusinessLogic().mailGreetings();
					initiateTaskQueue(counter, SEND_PERIOD_COMPLETION_MAIL);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_PERIOD_COMPLETION_MAIL);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_GREETING_NOTIFICATION (exception)");
				}
				return;
			}

			if (disablePeriodCompletionNotification && purpose.equals(SEND_PERIOD_COMPLETION_MAIL)) {
				purpose = SEND_ABSENT_REPORT;
			}

			LogUtility.writeError("6 >> length >> " + length + " << counter >> " + counter + " << purpose >> " + purpose);
			if (purpose.equals(SEND_PERIOD_COMPLETION_MAIL)) {
				try {
					new PeriodCompletionMailBusinessLogic().mailPeriodCompletion();
					initiateTaskQueue(counter, SEND_ABSENT_REPORT);
				} catch (Exception e) {
					initiateTaskQueue(counter, SEND_ABSENT_REPORT);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_PERIOD_COMPLETION_NOTIFICATION (exception)");
				}
				return;
			}
			if ((disableAbsentReport || !isTodayFriday) && purpose.equals(SEND_ABSENT_REPORT)) {
				purpose = SEND_PROBATION_PERIOD_END_REPORT;
			}
			if (!disableAbsentReport && isTodayFriday && purpose.equals(SEND_ABSENT_REPORT)) {
				try {
					new AbsentReportOnEmailBusinessLogic().sendAbsentReportOnEmail(null);
					initiateTaskQueue(counter, NEXT_ORGANIZATION);
				} catch (Exception e) {
					initiateTaskQueue(counter, NEXT_ORGANIZATION);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_ABSENT_REPORT (exception)");
				}
				return;
			}
			if (purpose.equals(SEND_PROBATION_PERIOD_END_REPORT)) {
				try {
					new AbsentReportOnEmailBusinessLogic().sendAbsentReportOnEmail(null);
					new ProbationEndingNotification().sendNotificationToProfitCenterWise();
					initiateTaskQueue(counter, NEXT_ORGANIZATION);
				} catch (Exception e) {
					initiateTaskQueue(counter, NEXT_ORGANIZATION);
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "SEND_PROBATION_PERIOD_END_REPORT (exception)");
				}
				return;
			}

			LogUtility.writeError("7 >> length >> " + length + " << counter >> " + counter + " << purpose >> " + purpose);
			if (length > (counter + 1) && purpose.equals(NEXT_ORGANIZATION)) {
				counter++;
				initiateTaskQueue(counter, SEND_DAILY_ATTENDANCE_MAIL);
			}
		}
	}

	public JSONArray getMailConfigurationSetup() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put("organizationlogo");
		columnArray.put("disable_exception_report_mail");
		columnArray.put("disable_attendance_report_mail");
		columnArray.put("disable_punch_client_report_mail");
		columnArray.put("disable_daily_short_attendance_report_mail");
		columnArray.put("disable_monthly_short_attendance_report_mail");
		columnArray.put("disable_calculate_ctc");
		columnArray.put("disable_mark_short_leave");
		columnArray.put("disable_mark_half_day_leave");
		columnArray.put("disable_mark_absent");
		columnArray.put("disable_populate_attendance");
		columnArray.put("disable_carry_forward_leaves");
		columnArray.put("disable_absent_report");
		columnArray.put(HrisKinds.MailConfigurations.DISABLE_GREETING_NOTIFICATION_MAIL);
		columnArray.put(HrisKinds.MailConfigurations.DISABLE_PERIOD_COMPLETION_NOTIFICATION_MAIL);
		JSONArray keys = new JSONArray();

		keys.put(Updates.KEY);
		JSONObject branches = new JSONObject();
		branches.put("disable_d_att_m_b_w", keys);
		columnArray.put(branches);

		branches = new JSONObject();
		branches.put("disable_d_m_s_att_m_b_w", keys);
		columnArray.put(branches);

		branches = new JSONObject();
		branches.put("disable_d_s_att_m_b_w", keys);
		columnArray.put(branches);

		branches = new JSONObject();
		branches.put("disable_p_c_m_b_w", keys);
		columnArray.put(branches);

		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_mailconfigurations");
		return employeeArray;
	}

	public void initiateTaskQueue(Object counter, String purpose) throws JSONException {
		JSONObject taskQueueInfo = new JSONObject();
		taskQueueInfo.put(ORGANIZATION_COUNTER, counter);
		taskQueueInfo.put(PURPOSE, purpose);
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueInfo);
	}

	public void sendMailInCaseOfException(Exception e, String title) {
		try {
			String traces = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			ApplaneMail mail = new ApplaneMail();
			StringBuilder builder = new StringBuilder();
			builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
			builder.append("<br><br><b>Exception traces are given below :</b><br></br><hr></hr><br></br>").append(traces);
			mail.setMessage(title, builder.toString(), true);
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Employee Salary Servlet.");
		}
	}

	@Override
	public void initiate(ApplaneTaskInfo taskQueueInfoParameter) throws DeadlineExceededException {
		JSONObject taskQueueInfo = taskQueueInfoParameter.getTaskInfo();
		try {
			schedularForAllOrganizations(taskQueueInfo);
		} catch (Exception e) {
			sendMailInCaseOfException(e, "Morning Scheduler HRIS Failed");
			throw new RuntimeException();
		}
	}
}
