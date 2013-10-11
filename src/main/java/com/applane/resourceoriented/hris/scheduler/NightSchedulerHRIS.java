package com.applane.resourceoriented.hris.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.taskqueue.ApplaneTask;
import com.applane.databaseengine.taskqueue.ApplaneTaskInfo;
import com.applane.databaseengine.taskqueue.ApplaneTaskService;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.HRISApplicationConstants.QueueConstantsHRIS;
import com.applane.resourceoriented.hris.MarkAttendanceServlet;
import com.applane.resourceoriented.hris.TakeAndMarkAttendanceServlet;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.service.cron.ApplaneCron;
import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.DeadlineExceededException;

public class NightSchedulerHRIS implements ApplaneTask, ApplaneCron {

	private static final String NEXT_ORGANIZATION = "next_organization";
	private static final String LEAVEPOLICYID = "leavepolicyid";
	private static final String LEAVE_TYPE_ID = "leavetypeid";
	public static final String CARRY_FORWARD_LEAVES_ON_LAPS_DATE = "carry_forward_leaves_on_laps_date";
	public static final String BRANCH_KEY = "branch_key";
	public static final String POPULATE_ATTENDANCE = "populate_attendance";
	public static final String MARK_ABSENT = "mark_absent";
	public static final String ORGANIZATION_COUNTER = "__hrisorganizationcounter__";
	public static final String PURPOSE = "__hrispurpose__";
	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void cronCall() throws DeadlineExceededException {
		try {
			String serverName = SystemProperty.applicationId.get();
			if (serverName.equalsIgnoreCase("applanecrmhrd")) {
				initiateTaskQueue("0", MARK_ABSENT, null, false, null, null);
			}
		} catch (Exception e) {
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "NightSchedulerHRIS Failed");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public void initiate(ApplaneTaskInfo applaneTaskInfo) throws DeadlineExceededException {
		try {
			JSONObject taskInfo = applaneTaskInfo.getTaskInfo();
			scheduler(taskInfo);
		} catch (Exception e) {
			new MorningSchdularHRIS().sendMailInCaseOfException(e, "NightSchedulerHRIS Failed");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	// private void scheduler(HttpServletRequest request) throws JSONException {
	private void scheduler(JSONObject taskInfo) throws JSONException {
		Object organizationCounter = taskInfo.opt(ORGANIZATION_COUNTER);
		/* first we will mark absent for previous date then we take attendance for the current date */
		if (organizationCounter == null) {
			/* iniiteTaskQueue from Here */
			/* pickup first organization and complete all its tasks */
			initiateTaskQueue("0", MARK_ABSENT, null, false, null, null);
		} else {
			/* check purpose */
			String[] organizations = HRISApplicationConstants.ORGANIZATIONS;
			int totalOrganizations = organizations.length;
			int counter = Integer.valueOf("" + organizationCounter);
			String orgName = organizations[counter];
			String purpose = taskInfo.getString(PURPOSE);

			String owner = "kapil.dalal@daffodilsw.com";
			Object userId = PunchingUtility.getId("up_users", "emailid", owner);
			PunchingUtility.setValueInSession(orgName, userId, owner);
			JSONArray mailConfigurationSetup = new MorningSchdularHRIS().getMailConfigurationSetup();
			boolean disableMarkAbsent = false;
			boolean disablePopulateAttendance = false;
			boolean disableCarryForwardLeaves = false;
			if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
				disableMarkAbsent = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_mark_absent"));
				disablePopulateAttendance = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_populate_attendance"));
				disableCarryForwardLeaves = Translator.booleanValue(mailConfigurationSetup.getJSONObject(0).opt("disable_carry_forward_leaves"));
			}
			if (disableMarkAbsent && purpose.equals(MARK_ABSENT)) {
				purpose = POPULATE_ATTENDANCE;
			}
			if (disablePopulateAttendance && purpose.equals(POPULATE_ATTENDANCE)) {
				purpose = CARRY_FORWARD_LEAVES_ON_LAPS_DATE;
			}
			if (disableCarryForwardLeaves && purpose.equals(CARRY_FORWARD_LEAVES_ON_LAPS_DATE)) {
				purpose = NEXT_ORGANIZATION;
			}
			if (purpose.equals(MARK_ABSENT)) {
				try {
					new TakeAndMarkAttendanceServlet().markAbsentForYesterDayAttendanceIfItIsUnknown(getPrevoiusDate(), "", taskInfo);
				} catch (Exception e) {
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "NightSchedulerHRIS Mark Absent Failed");
					// send Mail that problem has occurred while marking absent for the given organization
				}
			} else if (purpose.equals(POPULATE_ATTENDANCE)) {
				Object branchKey = taskInfo.opt(BRANCH_KEY);
				try {
					Object branchId = TakeAndMarkAttendanceServlet.getBranche(branchKey == null ? null : Long.valueOf("" + branchKey));
					if (branchId != null) {
						Date currentDate = SystemParameters.getCurrentDateTime();
						String currentDateStr = new SimpleDateFormat("yyyy-MM-dd").format(currentDate);
						if (!TakeAndMarkAttendanceServlet.isExists(branchId, currentDate)) {
							TakeAndMarkAttendanceServlet.insertTakeAttendnace(branchId, currentDateStr);
							MarkAttendanceServlet.readAttendanceParameters(currentDateStr, HRISApplicationConstants.ATTENDANCE_UNKNOWN + "", branchId.toString(), HRISApplicationConstants.TEMP_VALUE_FROM_NIGHT_SCHEDULER_POPULATE_ATTENDANCE, "", "", "", taskInfo);
						} else {
							initiateTaskQueue("" + organizationCounter, POPULATE_ATTENDANCE, branchId.toString(), true, null, null);
						}
					} else {
						if (counter == 0) {
							initiateTaskQueue((counter + ""), CARRY_FORWARD_LEAVES_ON_LAPS_DATE, null, true, null, null);
						}
					}

				} catch (Exception e) {
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "NightSchedulerHRIS POPULATE_ATTENDANCE Failed");
					// send Mail inform that problem has occurred while taking attendance for given branch in given organization
					if (branchKey != null) {
						initiateTaskQueue("" + organizationCounter, POPULATE_ATTENDANCE, "" + branchKey, true, null, null);
					} else {
						if (counter == 0) {
							initiateTaskQueue((counter + ""), CARRY_FORWARD_LEAVES_ON_LAPS_DATE, null, true, null, null);
						}
					}
				}
			} else if (purpose.equals(CARRY_FORWARD_LEAVES_ON_LAPS_DATE)) {
				Object leavePolicyIdParameter = taskInfo.opt(LEAVEPOLICYID);
				Object leaveTypeIdParameter = taskInfo.opt(LEAVE_TYPE_ID);
				try {
					if (leaveTypeIdParameter == null) {
						leavePolicyIdParameter = TakeAndMarkAttendanceServlet.getLeavePolicyId(leavePolicyIdParameter == null ? null : ("" + leavePolicyIdParameter));
					}
					if (leavePolicyIdParameter != null) {
						Object leaveTypeId = TakeAndMarkAttendanceServlet.getLeaveRule(leaveTypeIdParameter, "" + leavePolicyIdParameter);
						if (leaveTypeId != null) {
							if (leaveTypeIdParameter != null && (leaveTypeIdParameter + "").length() > 0) {
								leaveTypeIdParameter = leaveTypeIdParameter + "," + leaveTypeId.toString();
							} else {
								leaveTypeIdParameter = leaveTypeId.toString();
							}
						} else {
							leaveTypeIdParameter = null;
						}
						TakeAndMarkAttendanceServlet.carryForwardLeaves("" + leavePolicyIdParameter, leaveTypeId);

						initiateTaskQueue((counter + ""), CARRY_FORWARD_LEAVES_ON_LAPS_DATE, null, true, "" + leavePolicyIdParameter, leaveTypeIdParameter == null ? null : ("" + leaveTypeIdParameter));
					} else if (totalOrganizations > (counter + 1)) {
						initiateTaskQueue((counter + 1 + ""), MARK_ABSENT, null, true, null, null);
					}
				} catch (Exception e) {
					new MorningSchdularHRIS().sendMailInCaseOfException(e, "NightSchedulerHRIS CARRY_FORWARD_LEAVES_ON_LAPS_DATE Failed");
					if (leavePolicyIdParameter != null) {
						initiateTaskQueue((counter + ""), CARRY_FORWARD_LEAVES_ON_LAPS_DATE, null, true, "" + leavePolicyIdParameter, leaveTypeIdParameter == null ? null : ("" + leaveTypeIdParameter));
					} else if (totalOrganizations > (counter + 1)) {
						initiateTaskQueue((counter + 1 + ""), MARK_ABSENT, null, true, null, null);
					}
				}
			} else if (totalOrganizations > (counter + 1)) {
				initiateTaskQueue((counter + 1 + ""), MARK_ABSENT, null, true, null, null);
			}
		}
	}

	public static void initiateTaskQueue(String organizationCounter, String purpose, String branchKey, boolean addParallel, String leavePolicyId, String leaveRuleId) throws JSONException {
		JSONObject taskQueueParameters = new JSONObject();
		taskQueueParameters.put(ORGANIZATION_COUNTER, organizationCounter);
		taskQueueParameters.put(PURPOSE, purpose);
		taskQueueParameters.put(BRANCH_KEY, branchKey);
		if (leavePolicyId != null) {
			taskQueueParameters.put(LEAVEPOLICYID, leavePolicyId);
			if (leaveRuleId != null) {
				taskQueueParameters.put(LEAVE_TYPE_ID, leaveRuleId);
			}
		}
		ApplaneTaskService.enQueueTask("com.applane.resourceoriented.hris.scheduler.NightSchedulerHRIS", QueueConstantsHRIS.POPULATE_ATTENDANCE, taskQueueParameters);
	}

	private String getPrevoiusDate() throws ParseException {
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		String PREVIOUS_DATE = DataTypeUtilities.getBackDate(TODAY_DATE);
		return PREVIOUS_DATE;
	}

}