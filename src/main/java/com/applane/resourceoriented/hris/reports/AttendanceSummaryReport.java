package com.applane.resourceoriented.hris.reports;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.PunchingUtility;

public class AttendanceSummaryReport {
	public static void sendAttendanceSummaryReport() {
		try {
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			String PREVIOUS_DATE = DataTypeUtilities.getBackDate(TODAY_DATE);

			Date currentDate = DataTypeUtilities.checkDateFormat(TODAY_DATE);
			Calendar cal = Calendar.getInstance();
			cal.setTime(currentDate);
			long currentMonthId = cal.get(Calendar.MONTH) + 1;
			String currentMonthName = EmployeeSalaryGenerationServlet.getMonthName(currentMonthId);
			String currentYearName = String.valueOf(cal.get(Calendar.YEAR));
			Date currentMonthFirstDate = DataTypeUtilities.getMonthFirstDate(currentYearName, currentMonthName);
			Date currentMonthLastDate = DataTypeUtilities.getMonthLastDate(currentMonthFirstDate);
			String currentMonthFirstDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthFirstDate);
			String currentMonthLastDateInString = EmployeeSalaryGenerationServlet.getDateInString(currentMonthLastDate);
			// JSONArray hrMailIdDetail = EmployeeSalarySheetReport.getHrMailIdDetail("");
			JSONArray mailIdDetailProfitCenterWise = EmployeeSalarySheetReport.getMailIdProfitCenterWise("");

			if (mailIdDetailProfitCenterWise != null && mailIdDetailProfitCenterWise.length() > 0) {
				for (int counter = 0; counter < mailIdDetailProfitCenterWise.length(); counter++) {
					Object profitCenterId = mailIdDetailProfitCenterWise.getJSONObject(counter).opt(Updates.KEY);

					Object mailIdArray = mailIdDetailProfitCenterWise.getJSONObject(counter).opt("emails");
					String[] emailIds = null;
					if (mailIdArray != null && mailIdArray instanceof JSONArray) {
						emailIds = new String[((JSONArray) mailIdArray).length()];
						for (int counterEmail = 0; counterEmail < ((JSONArray) mailIdArray).length(); counterEmail++) {
							emailIds[counterEmail] = Translator.stringValue(((JSONArray) mailIdArray).getJSONObject(counterEmail).opt("officialemailid"));
						}
						// Object branchId = hrMailIdDetail.getJSONObject(counter).opt("branchid");
						JSONArray attendanceArray = getAttendanceArray(currentMonthFirstDateInString, currentMonthLastDateInString, profitCenterId);
						if (attendanceArray != null && attendanceArray.length() > 0) {
							String title = "Attendance Report and Summary on " + TODAY_DATE;
							String mailContent = populateAttendanceForReport(attendanceArray, TODAY_DATE, PREVIOUS_DATE);
							PunchingUtility.sendMailsAccToProfitCenters(new String[] { ApplicationConstants.USER }, mailContent, title, emailIds);
							// HrisHelper.sendMails(new String[] { "" + ApplicationConstants.OWNER }, mailContent, title);
						}
					}
				}
			}

			// if (hrMailIdDetail != null && hrMailIdDetail.length() > 0) {
			// for (int counter = 0; counter < hrMailIdDetail.length(); counter++) {
			// String hrEmailId = Translator.stringValue(hrMailIdDetail.getJSONObject(counter).opt("employeeid.officialemailid"));
			// LogUtility.writeError("hrEmailId >> " + hrEmailId);
			// if (hrEmailId != null && hrEmailId.length() > 0) {
			// }
			// }
			// }
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage("AttendanceSummaryReport", e);
			throw new RuntimeException("AttendanceSummaryReport >> Trace >> " + trace);
		}
	}

	private static String populateAttendanceForReport(JSONArray attendanceArray, String todayDate, String previousDate) throws Exception {
		double currentDatePresent = 0.0;
		double currentDateAbsent = 0.0;
		double currentDateUnknown = 0.0;
		double currentDateShortLeave = 0.0;
		double currentDateHalfDay = 0.0;
		double currentDateFullDay = 0.0;
		double currentDateEwdHalfDay = 0.0;
		double currentDateEwdFullDay = 0.0;
		double currentDateTour = 0.0;
		double currentDateWfhHalfDay = 0.0;
		double currentDateWfhFullDay = 0.0;

		double yesterdayDatePresent = 0.0;
		double yesterdayDateAbsent = 0.0;
		double yesterdayDateUnknown = 0.0;
		double yesterdayDateShortLeave = 0.0;
		double yesterdayDateHalfDay = 0.0;
		double yesterdayDateFullDay = 0.0;
		double yesterdayDateEwdHalfDay = 0.0;
		double yesterdayDateEwdFullDay = 0.0;
		double yesterdayDateTour = 0.0;
		double yesterdayDateWfhHalfDay = 0.0;
		double yesterdayDateWfhFullDay = 0.0;

		double monthlyDatePresent = 0.0;
		double monthlyDateAbsent = 0.0;
		double monthlyDateUnknown = 0.0;
		double monthlyDateShortLeave = 0.0;

		double monthlyDateHalfDayUnpaid = 0.0;
		double monthlyDateFullDayUnpaid = 0.0;
		double monthlyDateHalfDayBlank = 0.0;
		double monthlyDateFullDayBlank = 0.0;

		double monthlyDateEwdHalfDayBlank = 0.0;
		double monthlyDateEwdFullDayBlank = 0.0;
		double monthlyDateEwdHalfDayUnpaid = 0.0;
		double monthlyDateEwdFullDayUnpaid = 0.0;
		double monthlyDateTour = 0.0;
		double monthlyDateWfhHalfDay = 0.0;
		double monthlyDateWfhFullDay = 0.0;

		Calendar cal = Calendar.getInstance();
		cal.setTime(Translator.dateValue(todayDate));
		int year = cal.get(Calendar.YEAR);
		String monthName = new SimpleDateFormat("MMMM").format(Translator.dateValue(todayDate));

		for (int counter = 0; counter < attendanceArray.length(); counter++) {
			String attendanceDate = Translator.stringValue(attendanceArray.getJSONObject(counter).opt("attendancedate"));
			int attendanceTypeId = Translator.integerValue(attendanceArray.getJSONObject(counter).opt("attendancetypeid"));
			int attendanceTypeShortLeaveId = Translator.integerValue(attendanceArray.getJSONObject(counter).opt("attendance_type_short_leave_id"));
			int paidStatus = Translator.integerValue(attendanceArray.getJSONObject(counter).opt("paidstatus"));
			if (attendanceDate.equals(todayDate)) {
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT) {
					currentDatePresent++;
					monthlyDatePresent++;
					if (attendanceTypeShortLeaveId == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
						currentDateShortLeave++;
						monthlyDateShortLeave++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
					currentDateAbsent++;
					monthlyDateAbsent++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
					currentDateFullDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateFullDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateFullDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) {
					currentDateHalfDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateHalfDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateHalfDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
					currentDateEwdFullDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateEwdFullDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateEwdFullDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					currentDateEwdHalfDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateEwdHalfDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateEwdHalfDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR) {
					currentDateTour++;
					monthlyDateTour++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
					currentDateUnknown++;
					monthlyDateUnknown++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
					currentDateWfhFullDay++;
					monthlyDateWfhFullDay++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
					currentDateWfhHalfDay++;
					monthlyDateWfhHalfDay++;
				}
			} else if (attendanceDate.equals(previousDate)) {
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT) {
					yesterdayDatePresent++;
					monthlyDatePresent++;
					if (attendanceTypeShortLeaveId == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
						yesterdayDateShortLeave++;
						monthlyDateShortLeave++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
					yesterdayDateAbsent++;
					monthlyDateAbsent++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
					yesterdayDateFullDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateFullDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateFullDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) {
					yesterdayDateHalfDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateHalfDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateHalfDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
					yesterdayDateEwdFullDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateEwdFullDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateEwdFullDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					yesterdayDateEwdHalfDay++;
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateEwdHalfDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateEwdHalfDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR) {
					yesterdayDateTour++;
					monthlyDateTour++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
					yesterdayDateUnknown++;
					monthlyDateUnknown++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
					yesterdayDateWfhFullDay++;
					monthlyDateWfhFullDay++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
					yesterdayDateWfhHalfDay++;
					monthlyDateWfhHalfDay++;
				}
			} else {
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT) {
					monthlyDatePresent++;
					if (attendanceTypeShortLeaveId == HRISApplicationConstants.AttendanceTypeShortLeave.SHORT_LEAVE) {
						monthlyDateShortLeave++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
					monthlyDateAbsent++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateFullDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateFullDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) {
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateHalfDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateHalfDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateEwdFullDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateEwdFullDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
					if (paidStatus == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_NONPAYABLE) {
						monthlyDateEwdHalfDayUnpaid++;
					}
					if (paidStatus == 0) {
						monthlyDateEwdHalfDayBlank++;
					}
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR) {
					monthlyDateTour++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
					monthlyDateUnknown++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
					monthlyDateWfhFullDay++;
				}
				if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
					monthlyDateWfhHalfDay++;
				}
			}
		}

		StringBuilder mailContent = new StringBuilder();

		mailContent.append("Dear Sir / Mam,").append("<BR /><BR />");
		mailContent.append("Attendance Report and Summary on ").append(todayDate).append("<BR /><BR />");
		mailContent.append("<table border='0' width='99%'><tr>");
		mailContent.append("<td width='45%'>");

		mailContent.append("<table cellspacing='0' border='1' width='100%'>");
		mailContent.append("<tr bgcolor='99CCCC'><td colspan='2' align='center'>For ").append(todayDate).append("<BR />").append("(Morning Summary)").append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Present </td><td align='center' width='20%'>").append(currentDatePresent).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Unknown </td><td align='center' width='20%'>").append(currentDateUnknown).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Absent </td><td align='center' width='20%'>").append(currentDateAbsent).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Short Leave </td><td align='center' width='20%'>").append(currentDateShortLeave).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Half Day Leave </td><td align='center' width='20%'>").append(currentDateHalfDay).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Full Day Leave </td><td align='center' width='20%'>").append(currentDateFullDay).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Tour </td><td align='center' width='20%'>").append(currentDateTour).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Full </td><td align='center' width='20%'>").append(currentDateWfhFullDay).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Half </td><td align='center' width='20%'>").append(currentDateWfhHalfDay).append("</td></tr>");
		mailContent.append("</table>");
		mailContent.append("</td><td width='10%'></td>");
		mailContent.append("<td width='45%'>");
		mailContent.append("<table cellspacing='0' border='1'>");
		mailContent.append("<tr bgcolor='99CCCC'><td colspan='2' align='center'>For ").append(previousDate).append("<BR />").append("(Evening Summary)").append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Present </td><td align='center' width='20%'>").append(yesterdayDatePresent).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Unknown </td><td align='center' width='20%'>").append(yesterdayDateUnknown).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Absent </td><td align='center' width='20%'>").append(yesterdayDateAbsent).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Short Leave </td><td align='center' width='20%'>").append(yesterdayDateShortLeave).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Half Day Leave </td><td align='center' width='20%'>").append(yesterdayDateHalfDay).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Full Day Leave </td><td align='center' width='20%'>").append(yesterdayDateFullDay).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Tour </td><td align='center' width='20%'>").append(yesterdayDateTour).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Full </td><td align='center' width='20%'>").append(yesterdayDateWfhFullDay).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Half </td><td align='center' width='20%'>").append(yesterdayDateWfhHalfDay).append("</td></tr>");
		mailContent.append("</table>");
		mailContent.append("</td>");
		mailContent.append("</tr><tr>");

		mailContent.append("<td width='45%'>");
		mailContent.append("<table cellspacing='0' border='1' width='100%'>");
		mailContent.append("<tr bgcolor='99CCCC'><td colspan='2' align='center'>For ").append(monthName).append(",").append(year).append("<BR />").append("(Morning Summary)").append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Unknown </td><td align='center' width='20%'>").append(monthlyDateUnknown).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Absent </td><td align='center' width='20%'>").append(monthlyDateAbsent).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Short Leave </td><td align='center' width='20%'>").append(monthlyDateShortLeave).append("</td></tr>");

		mailContent.append("<tr><td align='left' width='80%'>Full Day Leave(Unpaid) </td><td align='center' width='20%'>").append(monthlyDateFullDayUnpaid).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Full Day Leave(Blank) </td><td align='center' width='20%'>").append(monthlyDateFullDayBlank).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Half Day Leave(Unpaid) </td><td align='center' width='20%'>").append(monthlyDateHalfDayUnpaid).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Half Day Leave(Blank) </td><td align='center' width='20%'>").append(monthlyDateHalfDayBlank).append("</td></tr>");

		mailContent.append("<tr><td align='left' width='80%'>Tour </td><td align='center' width='20%'>").append(monthlyDateTour).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Full(Unpaid) </td><td align='center' width='20%'>").append(monthlyDateEwdFullDayUnpaid).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Full(Blank) </td><td align='center' width='20%'>").append(monthlyDateEwdFullDayBlank).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Half(Unpaid) </td><td align='center' width='20%'>").append(monthlyDateEwdHalfDayUnpaid).append("</td></tr>");
		mailContent.append("<tr><td align='left' width='80%'>Extra Working Day Half(Blank) </td><td align='center' width='20%'>").append(monthlyDateEwdHalfDayBlank).append("</td></tr>");
		mailContent.append("</table>");
		mailContent.append("</td><td width='10%'></td>");
		mailContent.append("<td width='45%'>");
		mailContent.append("</td>");
		mailContent.append("</tr></table>");

		return mailContent.toString();
	}

	private static JSONArray getAttendanceArray(String currentMonthFirstDateInString, String currentMonthLastDateInString, Object profitCenterId) throws Exception {
		JSONArray columnArray = new JSONArray();
		columnArray.put("attendancedate");
		columnArray.put("attendancetypeid");
		columnArray.put("paidstatus");
		columnArray.put("attendance_type_short_leave_id");
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "attendancedate >= '" + currentMonthFirstDateInString + "' AND attendancedate<='" + currentMonthLastDateInString + "' AND employeeid.profitcenterid=" + profitCenterId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeeattendance");
	}
}
