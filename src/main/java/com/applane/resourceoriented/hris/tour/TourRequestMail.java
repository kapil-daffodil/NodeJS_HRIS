package com.applane.resourceoriented.hris.tour;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.app.shared.constants.View;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.WarningCollector;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.leave.LeaveMail;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.service.smshelper.SendSMSService;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.1
 * @category HRIS businesslogic
 */

public class TourRequestMail implements OperationJob {
	// job define on "hris_tourrequests"
	ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		String operation = record.getOperationType();
		Object employeeId = record.getValue("employeeid");
		if (record.has("temp")) {
			return;
		}
		try {
			JSONArray indirectingReportingToIds = new LeaveMail().getIndirectingReportingToIds(employeeId);
			Map<String, Object> indirectReportingToMap = new HashMap<String, Object>();
			if (indirectingReportingToIds != null && indirectingReportingToIds.length() > 0) {
				for (int counter = 0; counter < indirectingReportingToIds.length(); counter++) {
					String offMail = indirectingReportingToIds.getJSONObject(counter).optString("indirectreportingto.officialemailid");
					Object indId = indirectingReportingToIds.getJSONObject(counter).opt("indirectreportingto");
					indirectReportingToMap.put(offMail, indId);
				}
			}
			if (operation.equalsIgnoreCase("insert")) {
				String smsCode = (String) record.getValue("smscode");

				JSONObject employeeDetails = putEmployeeDetailsIntoObject(record);

				Date tourStartDate = null;
				Date tourEndDate = null;
				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

				if (employeeDetails.opt("startDateObject") != null && employeeDetails.opt("endDateObject") != null) {
					try {
						tourStartDate = updateDateFormat.parse("" + employeeDetails.opt("startDateObject"));
						tourEndDate = updateDateFormat.parse("" + employeeDetails.opt("endDateObject"));
					} catch (ParseException e) {
						throw new RuntimeException("Error Come while parsing " + e.getMessage());
					}
				}
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
				String startDate = dateFormat.format(tourStartDate);
				String endDate = dateFormat.format(tourEndDate);
				List<String> employeeEmailIdList = new ArrayList<String>();
				employeeEmailIdList.add("" + employeeDetails.opt("employeeEmailId"));
				try {
					employeeEmailIdList.add((String) employeeDetails.optString("approverEmailId"));

					if (indirectReportingToMap != null && indirectReportingToMap.size() > 0) {
						for (String offMailID : indirectReportingToMap.keySet()) {
							employeeEmailIdList.add(offMailID);
						}
					}
					sendTourMailOnInsertUpdate(employeeEmailIdList, startDate, endDate, employeeDetails, indirectReportingToMap);
					try {
						sendTourSMS(employeeDetails, startDate, endDate, smsCode);
					} catch (Exception e) {
						String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
						LogUtility.writeLog("Erroe While Sending SMS Trace >> " + trace);
					}
				} catch (Exception e) {
					throw new RuntimeException("Error Come: " + e.getMessage());

				}
			} else if (operation.equalsIgnoreCase("update")) {
				// Object tourRequestId = record.getKey();
				JSONObject employeeDetails = putEmployeeDetailsIntoObject(record);
				if (record.getNewValue("expense_status") != null) {
					String expenseStatus = "" + record.getValue("expense_status");
					if (expenseStatus.equals("Expense Submitted")) {
						JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.get("branchId"));
						sendEmailToCashier(hrAssigningArray, employeeDetails, record);

					}
					return;
				}

				if (record.has("freeze_status")) {
					return;
				}
				int newTourStatus = (Integer) record.getNewValue("tourstatusid");
				String approverComment = (String) record.getValue("comment");
				// try {
				List<String> employeeEmailIdList = new ArrayList<String>();

				Date tourStartDate = null;
				Date tourEndDate = null;

				int oldTourStatus = Translator.integerValue(employeeDetails.opt("oldTourStatus"));

				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

				if (employeeDetails.opt("startDateObject") != null && employeeDetails.opt("endDateObject") != null) {
					try {
						tourStartDate = updateDateFormat.parse("" + employeeDetails.opt("startDateObject"));
						tourEndDate = updateDateFormat.parse("" + employeeDetails.opt("endDateObject"));
					} catch (ParseException e) {
						throw new RuntimeException("Error Come while parsing Dates" + e.getMessage());
					}
				}
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
				String startDate = dateFormat.format(tourStartDate);
				String endDate = dateFormat.format(tourEndDate);
				if (indirectReportingToMap != null && indirectReportingToMap.size() > 0) {
					for (String offMailID : indirectReportingToMap.keySet()) {
						if (!employeeEmailIdList.contains(offMailID)) {
							employeeEmailIdList.add(offMailID);
						}
					}
				}
				if (!employeeEmailIdList.contains("" + employeeDetails.opt("employeeEmailId"))) {
					employeeEmailIdList.add("" + employeeDetails.opt("employeeEmailId"));
				}

				JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.get("branchId"));
				int hrAssigningCount = (hrAssigningArray == null || hrAssigningArray.length() == 0) ? 0 : hrAssigningArray.length();
				if (hrAssigningCount > 0) {
					String hrId = "" + hrAssigningArray.getJSONObject(0).opt("employeeid");
					String hrEmailId = (String) hrAssigningArray.getJSONObject(0).opt("employeeid.officialemailid");
					if (hrEmailId != null) {
						employeeEmailIdList.add((String) hrEmailId);
					}
					if (oldTourStatus == HRISConstants.TOUR_NEW && newTourStatus == HRISConstants.TOUR_APPROVED || oldTourStatus == HRISConstants.TOUR_REJECTED && newTourStatus == HRISConstants.TOUR_APPROVED) {
						if (hrId != null) {
							updateCCEmployee(employeeDetails.get("key"), hrId);
						}
					}
				}
				if (employeeDetails.opt("approverEmailId") != null && ("" + employeeDetails.opt("approverEmailId")).length() > 0 && !employeeEmailIdList.contains("" + employeeDetails.opt("approverEmailId"))) {
					employeeEmailIdList.add("" + employeeDetails.opt("approverEmailId"));
				}
				if (oldTourStatus == HRISConstants.TOUR_NEW && newTourStatus == HRISConstants.TOUR_NEW) {
					sendTourMailOnInsertUpdate(employeeEmailIdList, startDate, endDate, employeeDetails, indirectReportingToMap);
				}
				if (oldTourStatus == HRISConstants.TOUR_NEW && newTourStatus == HRISConstants.TOUR_REJECTED || oldTourStatus == HRISConstants.TOUR_APPROVED && newTourStatus == HRISConstants.TOUR_REJECTED) {
					rejectTourMail(employeeDetails, employeeEmailIdList, startDate, endDate, approverComment);
				}
				if (oldTourStatus == HRISConstants.TOUR_NEW && newTourStatus == HRISConstants.TOUR_APPROVED || oldTourStatus == HRISConstants.TOUR_REJECTED && newTourStatus == HRISConstants.TOUR_APPROVED) {
					approveTourMail(employeeDetails, employeeEmailIdList, startDate, endDate, approverComment);
					approveTourSMS(employeeDetails, startDate, endDate);
				}
				// } catch (Exception e) {
				// throw new RuntimeException("Error Come during update operation: " + e.getMessage());
				// }
			}
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("Tour Mail On After mail Exception >> " + trace);
			throw new BusinessLogicException(trace);
		}
	}

	private void sendEmailToCashier(JSONArray hrAssigningArray, JSONObject employeeDetails, Record record) throws Exception {
		JSONArray employeeTourRecords = getEmployeeTourRecords(record.getKey());
		String travellingReport = getTravellingReport(employeeTourRecords);
		Object key = record.getKey();
		Object cashierName = hrAssigningArray.getJSONObject(0).opt("cashier_id.name");
		Object cashierEmailId = hrAssigningArray.getJSONObject(0).opt("cashier_id.officialemailid");
		Object cashierId = hrAssigningArray.getJSONObject(0).opt("cashier_id");
		Object employeeName = employeeTourRecords.getJSONObject(0).opt("employeeid.name");
		Object employeeCode = employeeTourRecords.getJSONObject(0).opt("employeeid.employeecode");
		String linkForCashier = getLinkForCashier(key, cashierId);
		if (cashierEmailId != null) {
			String title = "Tour Expense Submitted";
			String mailValue = "Hello " + cashierName + ",<BR />";
			mailValue += employeeName + "(" + employeeCode + ") has submitted tour expense. Details as given below:<BR /><BR />";

			mailValue = travellingReport + linkForCashier;
			HrisHelper.sendMails(new String[] { "" + cashierEmailId }, mailValue, title);
		}
	}

	public String getTravellingReport(JSONArray employeeTourRecords) throws Exception {
		// Object employeeId = employeeTourRecords.getJSONObject(0).opt("employeeid");
		Object employeeName = employeeTourRecords.getJSONObject(0).opt("employeeid.name");
		Object employeeCode = employeeTourRecords.getJSONObject(0).opt("employeeid.employeecode");
		Object employeeDepartmentName = employeeTourRecords.getJSONObject(0).opt("employeeid.departmentid.name");
		Object employeeDesignationName = employeeTourRecords.getJSONObject(0).opt("employeeid.designationid.name");

		JSONArray tourDetailsArray = employeeTourRecords.getJSONObject(0).getJSONArray("hris_tourdetails");
		JSONArray lodgingDetailsArray = employeeTourRecords.getJSONObject(0).getJSONArray("hris_tour_details_lodging");
		JSONArray localExpenseDetailsArray = employeeTourRecords.getJSONObject(0).getJSONArray("hris_tour_local_expense");
		JSONArray foodingDetailsArray = employeeTourRecords.getJSONObject(0).getJSONArray("hris_tour_details_fooding");
		JSONArray travelDetailsArray = employeeTourRecords.getJSONObject(0).getJSONArray("hris_tour_details_travel");
		JSONArray hotelDetailsArray = employeeTourRecords.getJSONObject(0).getJSONArray("hris_tour_details_hotel");
		JSONArray tourOtherExpensesArray = employeeTourRecords.getJSONObject(0).getJSONArray("hris_tour_other_expenses");
		Object tourCode = employeeTourRecords.getJSONObject(0).opt("tourcode");
		Object tourPurpose = employeeTourRecords.getJSONObject(0).opt("tourpurpose");
		Object approverName = employeeTourRecords.getJSONObject(0).opt("approverid.name");
		Object startDate = employeeTourRecords.getJSONObject(0).opt("startdate");
		Object endDate = employeeTourRecords.getJSONObject(0).opt("enddate");
		Object briefTourDescription = employeeTourRecords.getJSONObject(0).opt("brief_tour_description");
		Object accessClaimAmount = employeeTourRecords.getJSONObject(0).opt("access_amount");
		Object excessClaimReason = employeeTourRecords.getJSONObject(0).opt("excess_claim_reason");
		if (Translator.doubleValue(accessClaimAmount) <= 0.0) {
			excessClaimReason = "";
		}

		double[] totalAmount = { 0.0, 0.0, 0.0 };
		Object tourPlaces = getTourPlaces(tourDetailsArray);
		JSONObject lodgingObject = getLodgingObject(lodgingDetailsArray, totalAmount);
		JSONObject localExpenseDetails = getLocalExpenseObject(localExpenseDetailsArray, totalAmount);
		JSONObject foodingDetails = getFoodingObject(foodingDetailsArray, totalAmount);
		JSONObject travelDetails = getTravelObject(travelDetailsArray, totalAmount);
		JSONObject hotelDetails = getHotelObject(hotelDetailsArray, totalAmount);
		JSONObject tourOtherDetails = getLocalExpenseObject(tourOtherExpensesArray, totalAmount);

		double totalAdvanceTaken = Translator.doubleValue(employeeTourRecords.getJSONObject(0).opt("advanceamount_amount") == null ? 0.0 : employeeTourRecords.getJSONObject(0).opt("advanceamount_amount"));
		totalAdvanceTaken += Translator.doubleValue(employeeTourRecords.getJSONObject(0).opt("advance_against_tickting") == null ? 0.0 : employeeTourRecords.getJSONObject(0).opt("advance_against_tickting"));
		String advance = new DecimalFormat("#.00").format(totalAdvanceTaken);
		Object lodgingApprovedAmount = lodgingObject.opt("approvedAmount");
		Object lodgingExpectedAmount = lodgingObject.opt("expectedAmount");
		Object lodgingActualAmount = lodgingObject.opt("actualAmount");

		Object travelApprovedAmount = travelDetails.opt("approvedAmount");
		Object travelExpectedAmount = travelDetails.opt("expectedAmount");
		Object travelActualAmount = travelDetails.opt("actualAmount");

		Object foodingApprovedAmount = foodingDetails.opt("approvedAmount");
		Object foodingExpectedAmount = foodingDetails.opt("expectedAmount");
		Object foodingActualAmount = foodingDetails.opt("actualAmount");

		Object hotelApprovedAmount = hotelDetails.opt("approvedAmount");
		Object hotelExpectedAmount = hotelDetails.opt("expectedAmount");
		Object hotelActualAmount = hotelDetails.opt("actualAmount");

		Object localExpenseApprovedAmount = localExpenseDetails.opt("approvedAmount");
		Object localExpenseExpectedAmount = localExpenseDetails.opt("expectedAmount");
		Object localExpenseActualAmount = localExpenseDetails.opt("actualAmount");

		Object otherApprovedAmount = tourOtherDetails.opt("approvedAmount");
		Object otherExpectedAmount = tourOtherDetails.opt("expectedAmount");
		Object otherActualAmount = tourOtherDetails.opt("actualAmount");

		double totalApprovedAmount = totalAmount[0];
		double totalExpectedAmount = totalAmount[1];
		double totalActualAmount = totalAmount[2];

		String netPayable = new DecimalFormat("#.00").format((totalAmount[0] - totalAdvanceTaken));

		JSONArray configurationSetup = getConfigurationSetup();
		JSONObject tourDetailsObject = new JSONObject();
		if (configurationSetup != null && configurationSetup.length() > 0) {
			tourDetailsObject = configurationSetup.getJSONObject(0);
		}
		String travelDetailsFormat = getTravelDetailsFormat(travelDetailsArray);
		String localDetailsFormat = getLocalDetailsFormat(localExpenseDetailsArray);
		String otherDetailsFormat = getOtherDetailsFormat(tourOtherExpensesArray);

		String travellingReport = "<body> " + "<div id='table1'><table border='0' width='98%' cellspacing='0'  align='center'> <tr> <td align='center'><font face='sans-serif' size='3'> <b>" + tourDetailsObject.opt("organizationname") + "</b></font><BR /> <table border='1' width='100%' cellspacing='0'  align='center'>" + "<tr height='10'> <td colspan='4' align='left' width='45%' style='border-right:0px;'> <b>Tour Code: </b>" + tourCode + " </td><td colspan='4' align='left' width='55%' style='border-left:0px;'> <b>Travelling Form</b> </td> </tr>" + "<tr>" + "<td width='5%'>  </td>" + "<td width='15%'><font face='sans-serif' size='2'><b>Name</b></font></td>" + "<td width='13%'> <font face='sans-serif' size='2'>" + employeeName + " </font></td>"
				+ "<td width='13%'> <font face='sans-serif' size='2'><b>Emp. Code</b></font> </td>" + "<td width='13%'><font face='sans-serif' size='2'> " + employeeCode + "</font> </td>" + "<td width='13%'> <font face='sans-serif' size='2'><b>Designation</b></font> </td>" + "<td width='13%'><font face='sans-serif' size='2'> " + employeeDesignationName + "</font> </td>" + "<td width='13%'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td width='5%'>  </td>" + "<td width='15%'><font face='sans-serif' size='2'><b>Place of Tour </b></font></td>" + "<td width='39%' colspan='3'> <font face='sans-serif' size='2'>" + tourPlaces + "</font></td>" + "<td width='13%'> <font face='sans-serif' size='2'><b>Department</b></font> </td>" + "<td width='13%'><font face='sans-serif' size='2'> "
				+ employeeDepartmentName + "</font> </td>" + "<td width='13%'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td width='5%'>  </td>" + "<td width='15%'><font face='sans-serif' size='2'><b>Purpose</b></font></td>" + "<td width='13%' colspan='3'> <font face='sans-serif' size='2'>" + tourPurpose + "</font> </td>" + "<td width='13%'> <font face='sans-serif' size='2'><b>Tour Authorized By</b></font> </td>" + "<td width='13%'><font face='sans-serif' size='2'> " + approverName + " JI</font> </td>" + "<td width='13%'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td width='5%'>  </td>" + "<td width='15%'><font face='sans-serif' size='2'><b>Date &amp; Time of Departure</b></font></td>" + "<td width='13%'> <font face='sans-serif' size='2'>" + startDate + "</font> </td>"
				+ "<td width='13%'> <b>&nbsp;</b> </td>" + "<td width='13%'> &nbsp; </td>" + "<td width='13%'> <font face='sans-serif' size='2'><b>Date &amp; Time for Arrival</b></font></td>" + "<td width='13%'><font face='sans-serif' size='2'> " + endDate + "</font> </td>" + "<td width='13%'> &nbsp; </td>" + "</tr>" + "<tr height='10'> <td colspan='8'> &nbsp; </td> </tr>" + "<tr>" + "<td>  </td>" + "<td colspan='2'>&nbsp;</td>" + "<td align='center'> <font face='sans-serif' size='2'><b>Expenses Incurred (In Rs.)</b></font> </td>" + "<td align='center'> <font face='sans-serif' size='2'><b>Amount as per Eligibility (In Rs.)</b></font> </td>" + "<td align='center'> <font face='sans-serif' size='2'><b>Amount Approved (In Rs.)</b></font> </td>"
				+ "<td colspan='2' align='center'> <font face='sans-serif' size='2'><b>Remarks (if any)</b></font> </td>" + "</tr>" + "<tr>" + "<td> 1 </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Fare</b></font></td>" + "<td align='right'><font face='sans-serif' size='2'> " + travelActualAmount + "</font> </td>" + "<td align='right'><font face='sans-serif' size='2'>" + travelExpectedAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + travelApprovedAmount + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> 2 </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Fooding</b></font></td>" + "<td align='right'><font face='sans-serif' size='2'> " + foodingActualAmount + "</font> </td>"
				+ "<td align='right'> <font face='sans-serif' size='2'>" + foodingExpectedAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + foodingApprovedAmount + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> 3 </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Hotel Bills</b></font></td>" + "<td align='right'> <font face='sans-serif' size='2'>" + hotelActualAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + hotelExpectedAmount + " </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + hotelApprovedAmount + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> 4 </td>"
				+ "<td colspan='2'><font face='sans-serif' size='2'><b>Local Conveyance</b></font></td>" + "<td align='right'> <font face='sans-serif' size='2'>" + localExpenseActualAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + localExpenseExpectedAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + localExpenseApprovedAmount + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> 5 </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Lodging Allowances (Own Arrangement Cases)</b></font></td>" + "<td align='right'> <font face='sans-serif' size='2'>" + lodgingActualAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + lodgingExpectedAmount + "</font> </td>"
				+ "<td align='right'> <font face='sans-serif' size='2'>" + lodgingApprovedAmount + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> 6 </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Other Expense</b></font></td>" + "<td align='right'> <font face='sans-serif' size='2'>" + otherActualAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + otherExpectedAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + otherApprovedAmount + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Total Amount  (A)</b></font></td>" + "<td align='right'> <font face='sans-serif' size='2'>"
				+ totalActualAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + totalExpectedAmount + "</font> </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + totalApprovedAmount + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Less : Advance Taken (B)</b></font></td>" + "<td> &nbsp; </td>" + "<td> &nbsp; </td>" + "<td align='right'> <font face='sans-serif' size='2'>" + advance + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Net Payable/ Refundable</b></font></td>" + "<td> &nbsp; </td>" + "<td> &nbsp; </td>"
				+ "<td align='right'> <font face='sans-serif' size='2'>" + netPayable + "</font> </td>" + "<td colspan='2'> &nbsp; </td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>In Case of Excess Claim Specify Reason</b></font></td>" + "<td colspan='5'> <font face='sans-serif' size='2'>" + excessClaimReason + "</font> </td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Amount of Excess Claim</b></font></td>" + "<td colspan='2' align='right'> <font face='sans-serif' size='2'>" + accessClaimAmount + "</font> </td>" + "<td colspan='3'> </td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td align='center' colspan='7'><font face='sans-serif' size='2'><b>Brief Tour Report</b></font></td>"
				+ "</tr>" + "<tr height='80'>" + "<td align='left' colspan='8'>" + briefTourDescription + "</td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td colspan='2' rowspan='2'>&nbsp;</td>" + "<td colspan='2' rowspan='2'>&nbsp;</td>" + "<td> &nbsp; </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Passed of Rs�����������</b></font></td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td> &nbsp; </td>" + "<td colspan='2'>&nbsp;</td>" + "</tr>" + "<tr>" + "<td> &nbsp; </td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Signature of Claimant</b></font></td>" + "<td colspan='2'><font face='sans-serif' size='2'><b>Signature of Approving Authority</b></font></td>" + "<td> &nbsp; </td>"
				+ "<td colspan='2'><font face='sans-serif' size='2'><b>Account Officer/Branch Manager</b></font></td>" + "</tr>" + "</table> </td> </tr> </table>" + "</div>"

				+ "<table border='0' width='98%' cellspacing='0'  align='center'> " + "	<tr> <td align='center'><font face='sans-serif' size='3'> <b><xsl:value-of select='configurationSetup/organizationname'/></b></font><BR />" + "<table border='1' width='100%' cellspacing='0'  align='center'>" + "<tr height='10'> <td colspan='10' align='center'> <font face='sans-serif' size='2'><b>Fare (Ticket/Supporting Needs to be Attached)</b></font> </td> </tr>" + "<tr>" + "<td width='15%' align='center'><font face='sans-serif' size='2'><b>Date</b></font></td>" + "<td width='18%' colspan='2' align='center'><font face='sans-serif' size='2'><b>From</b></font></td>" + "<td width='17%' colspan='2' align='center'><font face='sans-serif' size='2'><b>To</b></font></td>"
				+ "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Mode</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Class</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Amount Claimed (In Rs.)</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Amount Approved (In Rs.)</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Supporting�s</b></font></td>" + "</tr>" + "<tr>" + "<td>&nbsp;</td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Place</b></font></td>" + "<td width='8%' align='center'><font face='sans-serif' size='2'><b>Time</b></font></td>"
				+ "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Place</b></font></td>" + "<td width='7%' align='center'><font face='sans-serif' size='2'><b>Time</b></font></td>" + "<td>&nbsp;</td>" + "<td>&nbsp;</td>" + "<td>&nbsp;</td>" + "<td>&nbsp;</td>" + "<td>&nbsp;</td>" + "</tr>" +

				travelDetailsFormat + "</table>" +

				"<table border='1' width='100%' cellspacing='0'  align='center'>" + "<tr height='10'> <td colspan='8' align='center'> <font face='sans-serif' size='2'><b>Local Conveyance</b></font> </td> </tr>" + "<tr>" + "<td width='15%' align='center'><font face='sans-serif' size='2'><b>Date</b></font></td>" + "<td width='12%' align='center'><font face='sans-serif' size='2'><b>From</b></font></td>" + "<td width='12%' align='center'><font face='sans-serif' size='2'><b>To</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Mode</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Aprox. Distance in  K.M.</b></font></td>"
				+ "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Amount Claimed (In Rs.)</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Amount Approved (In Rs.)</b></font></td>" + "<td width='21%' align='center'><font face='sans-serif' size='2'><b>Purpose</b></font></td>" + "</tr>" +

				localDetailsFormat + "</table>" + "" +

				"<table border='1' width='100%' cellspacing='0'  align='center'>" + "" + "" + "<tr height='10'> <td colspan='5' align='center'> <b>Other Expense</b> </td> </tr>" + "<tr>" + "	" + "<td width='15%' align='center'><font face='sans-serif' size='2'><b>Date</b></font></td>" + "<td width='20%' align='center'><font face='sans-serif' size='2'><b>Detail</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Amount Claimed (In Rs.)</b></font></td>" + "<td width='10%' align='center'><font face='sans-serif' size='2'><b>Amount Approved (In Rs.)</b></font></td>" + "<td width='45%' align='center'><font face='sans-serif' size='2'><b>Purpose</b></font></td>" + "</tr>" + "" + otherDetailsFormat + "</table>" + "</td> </tr>"
				+ "<tr height='80' valign='bottom'><td align='right'><font face='sans-serif' size='2'><b>Signature of Claimant</b></font></td></tr>" + "</table>" + "</body>";

		return travellingReport;
	}

	private String getOtherDetailsFormat(JSONArray tourOtherExpensesArray) throws Exception {
		String format = "<tr>";
		for (int counter = 0; counter < tourOtherExpensesArray.length(); counter++) {
			Object fromLocation = tourOtherExpensesArray.getJSONObject(counter).opt("other_expense_id.name");
			Object fromDate = tourOtherExpensesArray.getJSONObject(counter).opt("from_date");
			Object amount = tourOtherExpensesArray.getJSONObject(counter).opt("amount");
			Object approvedAmount = tourOtherExpensesArray.getJSONObject(counter).opt("approved_amount");
			Object purpose = tourOtherExpensesArray.getJSONObject(counter).opt("purpose");

			format += "<td align='center'><font face='sans-serif' size='2'>" + fromDate == null ? "" : fromDate + "</font></td>";
			format += "<td'><font face='sans-serif' size='2'>" + fromLocation == null ? "" : fromLocation + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + amount == null ? "" : amount + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + approvedAmount == null ? "" : approvedAmount + "</font></td>";
			format += "<td><font face='sans-serif' size='2'>" + purpose == null ? "" : purpose + "</font></td>";
		}
		format += "</tr>";
		return format;
	}

	private String getLocalDetailsFormat(JSONArray localExpenseDetailsArray) throws Exception {
		String format = "<tr>";
		for (int counter = 0; counter < localExpenseDetailsArray.length(); counter++) {
			Object fromLocation = localExpenseDetailsArray.getJSONObject(counter).opt("from_location");
			Object toLocation = localExpenseDetailsArray.getJSONObject(counter).opt("to_location");
			Object travelTypeName = localExpenseDetailsArray.getJSONObject(counter).opt("travel_type_id.name");
			Object fromDate = localExpenseDetailsArray.getJSONObject(counter).opt("from_date");
			Object killoMeters = localExpenseDetailsArray.getJSONObject(counter).opt("killo_meters");
			Object amount = localExpenseDetailsArray.getJSONObject(counter).opt("amount");
			Object approvedAmount = localExpenseDetailsArray.getJSONObject(counter).opt("approved_amount");
			Object purpose = localExpenseDetailsArray.getJSONObject(counter).opt("purpose");

			format += "<td align='center'><font face='sans-serif' size='2'>" + fromDate == null ? "" : fromDate + "</font></td>";
			format += "<td'><font face='sans-serif' size='2'>" + fromLocation == null ? "" : fromLocation + "</font></td>";
			format += "<td><font face='sans-serif' size='2'>" + toLocation == null ? "" : toLocation + "</font></td>";
			format += "<td><font face='sans-serif' size='2'>" + travelTypeName == null ? "" : travelTypeName + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + killoMeters == null ? "" : killoMeters + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + amount == null ? "" : amount + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + approvedAmount == null ? "" : approvedAmount + "</font></td>";
			format += "<td><font face='sans-serif' size='2'>" + purpose == null ? "" : purpose + "</font></td>";
		}
		format += "</tr>";
		return format;
	}

	private String getTravelDetailsFormat(JSONArray travelDetailsArray) throws Exception {
		String format = "<tr>";
		for (int counter = 0; counter < travelDetailsArray.length(); counter++) {
			Object fromCityName = travelDetailsArray.getJSONObject(counter).opt("cityid.name");
			Object toCityName = travelDetailsArray.getJSONObject(counter).opt("to_city_id.name");
			Object fromDate = travelDetailsArray.getJSONObject(counter).opt("from_date");
			Object travelTypeName = travelDetailsArray.getJSONObject(counter).opt("travel_type_id.name");
			Object actualAmount = travelDetailsArray.getJSONObject(counter).opt("actual_amount");
			Object approvedAmount = travelDetailsArray.getJSONObject(counter).opt("approved_amount");
			Object purpose = travelDetailsArray.getJSONObject(counter).opt("purpose");
			format += "<td align='center'><font face='sans-serif' size='2'>" + fromDate == null ? "" : fromDate + "</font></td>";
			format += "<td'><font face='sans-serif' size='2'>" + fromCityName == null ? "" : fromCityName + "</font></td>";
			format += "<td><font face='sans-serif' size='2'>" + toCityName == null ? "" : toCityName + "</font></td>";
			format += "<td><font face='sans-serif' size='2'>" + travelTypeName == null ? "" : travelTypeName + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + actualAmount == null ? "" : actualAmount + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + approvedAmount == null ? "" : approvedAmount + "</font></td>";
			format += "<td align='right'><font face='sans-serif' size='2'>" + purpose == null ? "" : purpose + "</font></td>";
		}
		format += "</tr>";
		return format;
	}

	private Object getTourPlaces(JSONArray tourDetailsArray) throws Exception {
		String placeOfTours = "";
		List<Integer> list = new ArrayList<Integer>();
		for (int counter = 0; counter < tourDetailsArray.length(); counter++) {
			int cityId = Translator.integerValue(tourDetailsArray.getJSONObject(counter).opt("cityid"));
			String place = Translator.stringValue(tourDetailsArray.getJSONObject(counter).opt("cityid.name"));
			if (!list.contains(cityId)) {
				list.add(cityId);
				if (placeOfTours.length() > 0) {
					placeOfTours += ",";
				}
				placeOfTours += place;
			}
		}
		return placeOfTours;
	}

	private JSONObject getHotelObject(JSONArray hotelDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < hotelDetailsArray.length(); counter++) {
			JSONObject object = hotelDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("#.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("#.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("actual_amount", new DecimalFormat("#.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private JSONObject getTravelObject(JSONArray travelDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < travelDetailsArray.length(); counter++) {
			JSONObject object = travelDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("actual_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private JSONObject getFoodingObject(JSONArray foodingDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < foodingDetailsArray.length(); counter++) {
			JSONObject object = foodingDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("actual_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private JSONObject getLocalExpenseObject(JSONArray localExpenseDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < localExpenseDetailsArray.length(); counter++) {
			JSONObject object = localExpenseDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("amount"))));
			object.put("killo_meters", new DecimalFormat("#.##").format(Translator.doubleValue(object.opt("killo_meters"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private JSONObject getLodgingObject(JSONArray lodgingDetailsArray, double[] totalAmount) throws Exception {
		double approvedAmount = 0.0;
		double expectedAmount = 0.0;
		double actualAmount = 0.0;
		for (int counter = 0; counter < lodgingDetailsArray.length(); counter++) {
			JSONObject object = lodgingDetailsArray.getJSONObject(counter);
			object.put("approved_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("approved_amount"))));
			object.put("expected_amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("expected_amount"))));
			object.put("amount", new DecimalFormat("0.00").format(Translator.doubleValue(object.opt("actual_amount"))));

			approvedAmount += Translator.doubleValue(object.opt("approved_amount"));
			expectedAmount += Translator.doubleValue(object.opt("expected_amount"));
			actualAmount += Translator.doubleValue(object.opt("actual_amount"));

		}
		totalAmount[0] += approvedAmount;
		totalAmount[1] += expectedAmount;
		totalAmount[2] += actualAmount;
		return new JSONObject().put("approvedAmount", new DecimalFormat("0.00").format(approvedAmount)).put("expectedAmount", new DecimalFormat("0.00").format(expectedAmount)).put("actualAmount", new DecimalFormat("0.00").format(actualAmount));
	}

	private JSONArray getConfigurationSetup() throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_mailconfigurations");
		columnArray.put("organizationname");
		columnArray.put("organizationlogo");
		columnArray.put("address");
		query.put(Data.Query.COLUMNS, columnArray);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_mailconfigurations");
	}

	public JSONArray getEmployeeTourRecords(Object key) throws JSONException {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_tourrequests");
		columnArray.put(Updates.KEY);
		JSONArray childs = new JSONArray();

		JSONObject innerObject = new JSONObject();
		JSONObject innerQuery = new JSONObject();
		JSONArray innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("cityid");
		innerColumns.put("cityid.name");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
		innerObject.put("relatedcolumn", "tourrequestid");
		innerObject.put("alias", "hris_tourdetails");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		innerObject = new JSONObject();
		innerQuery = new JSONObject();
		innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("expected_amount");
		innerColumns.put("actual_amount");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tour_details_lodging");
		innerObject.put("relatedcolumn", "tourid");
		innerObject.put("alias", "hris_tour_details_lodging");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		innerObject = new JSONObject();
		innerQuery = new JSONObject();
		innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("from_date");
		innerColumns.put("to_date");
		innerColumns.put("from_location");
		innerColumns.put("to_location");
		innerColumns.put("travel_mode_id.name");
		innerColumns.put("killo_meters");
		innerColumns.put("amount");
		innerColumns.put("approved_amount");
		innerColumns.put("purpose");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tour_local_expense");
		innerObject.put("relatedcolumn", "tourid");
		innerObject.put("alias", "hris_tour_local_expense");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		innerObject = new JSONObject();
		innerQuery = new JSONObject();
		innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("from_date");
		innerColumns.put("other_expense_id.name");
		innerColumns.put("amount");
		innerColumns.put("approved_amount");
		innerColumns.put("purpose");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tour_other_expenses");
		innerObject.put("relatedcolumn", "tourid");
		innerObject.put("alias", "hris_tour_other_expenses");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		innerObject = new JSONObject();
		innerQuery = new JSONObject();
		innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("expected_amount");
		innerColumns.put("actual_amount");
		innerColumns.put("approved_amount");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tour_details_fooding");
		innerObject.put("relatedcolumn", "tourid");
		innerObject.put("alias", "hris_tour_details_fooding");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		innerObject = new JSONObject();
		innerQuery = new JSONObject();
		innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("expected_amount");
		innerColumns.put("actual_amount");
		innerColumns.put("approved_amount");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tour_details_hotel");
		innerObject.put("relatedcolumn", "tourid");
		innerObject.put("alias", "hris_tour_details_hotel");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		innerObject = new JSONObject();
		innerQuery = new JSONObject();
		innerColumns = new JSONArray();
		innerColumns.put(Updates.KEY);
		innerColumns.put("cityid.name");
		innerColumns.put("to_city_id.name");
		innerColumns.put("from_date");
		innerColumns.put("travel_type_id.name");
		// innerColumns.put("purpose");
		innerColumns.put("actual_amount");
		innerColumns.put("approved_amount");
		innerColumns.put("expected_amount");
		innerQuery.put(Data.Query.COLUMNS, innerColumns);
		innerQuery.put(Data.Query.RESOURCE, "hris_tour_details_travel");
		innerObject.put("relatedcolumn", "tourid");
		innerObject.put("alias", "hris_tour_details_travel");
		innerObject.put("parentcolumnalias", "__key__");
		innerObject.put("parentcolumn", "__key__");
		innerObject.put(Data.QUERY, innerQuery);
		childs.put(innerObject);

		query.put(Data.Query.CHILDS, childs);

		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.employeecode");
		columnArray.put("employeeid.designationid.name");
		columnArray.put("employeeid.departmentid.name");
		columnArray.put("tourpurpose");
		columnArray.put("approverid.name");
		columnArray.put("startdate");
		columnArray.put("enddate");
		columnArray.put("tourcode");
		columnArray.put("excess_claim_reason");
		columnArray.put("access_amount");
		columnArray.put("brief_tour_description");
		columnArray.put("advanceamount_amount");
		columnArray.put("advance_against_tickting");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);

		JSONArray rows = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_tourrequests");

		return rows;
	}

	public String getLinkForCashier(Object key, Object cashierId) throws Exception {
		StringBuffer emailsTo = new StringBuffer();
		emailsTo.append("<BR /><b>Select Email To:</b> ").append("<select name='emailTo'>");
		emailsTo.append("<option value='").append(HRISApplicationConstants.TourAfterApprovalCommentsEmailTo.HOD).append("'>").append("HOD").append("</option>");
		emailsTo.append("<option value='").append(HRISApplicationConstants.TourAfterApprovalCommentsEmailTo.EMPLOYEE).append("'>").append("Employee").append("</option>");
		emailsTo.append("</select><BR />");

		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();

		messageContents.append("<BR />");
		messageContents.append("<table border='0' width='99%'> <tr><td>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/tourexpensequery' method='GET'>");
		messageContents.append("<form action='http://apps.applane.com/escape/tourexpensequery'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(cashierId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td><td>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/tourexpensequery' method='GET'>");
		messageContents.append("<form action='http://apps.applane.com/escape/tourexpensequery' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(cashierId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr>");

		messageContents.append("<tr><td colspan='2'>");
		messageContents.append("<BR />");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/tourexpensequery' method='GET'>");
		messageContents.append("<form action='http://apps.applane.com/escape/tourexpensequery' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(cashierId).append("' />");
		messageContents.append(emailsTo).append("<BR />");
		messageContents.append("<INPUT TYPE='text' name='query' ").append("/>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Send Query  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		messageContents.append("<BR />");

		return messageContents.toString();
	}

	private void updateCCEmployee(Object tourRequestKey, String hrId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.RESOURCE, "hris_tourrequests__employees_in_cc");
		query.put(Data.Query.FILTERS, "hris_tourrequests = " + tourRequestKey + " AND employees_in_cc = " + hrId);
		JSONArray array = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_tourrequests__employees_in_cc");
		if (hrId != null && (array == null || array.length() == 0)) {
			JSONObject updateColumns = new JSONObject();
			updateColumns.put("hris_tourrequests", tourRequestKey);
			updateColumns.put("employees_in_cc", hrId);
			JSONObject updates = new JSONObject();
			updates.put(Data.Update.UPDATES, updateColumns);
			updates.put(Data.Update.RESOURCE, "hris_tourrequests__employees_in_cc");
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
		}
	}

	@SuppressWarnings("unchecked")
	private void rejectTourMail(JSONObject employeeDetails, List<String> employeeEmailIdList, String startDate, String endDate, String approverComment) {

		ApplaneMail mailSender = new ApplaneMail();
		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String replyToId = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);

			String[] concernedPersons = (String[]) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.CONCERNED_PERSONS);
			for (int counter = 0; concernedPersons != null && (counter < concernedPersons.length); counter++) {
				String concernedPersonEmailId = concernedPersons[counter];

				if (concernedPersonEmailId != null && !employeeEmailIdList.contains((String) concernedPersonEmailId)) {
					employeeEmailIdList.add(concernedPersonEmailId);
				}
			}

			if (!disableEmail) {
				JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.opt("branchId"));
				int hrAssigningCount = (hrAssigningArray == null || hrAssigningArray.length() == 0) ? 0 : hrAssigningArray.length();
				if (hrAssigningCount > 0) {
					String hrEmailId = (String) hrAssigningArray.getJSONObject(0).opt("employeeid.officialemailid");

					if (hrEmailId != null && !employeeEmailIdList.contains((String) hrEmailId)) {
						employeeEmailIdList.add((String) hrEmailId);
					} else {
						if (replyToId != null && !employeeEmailIdList.contains((String) replyToId)) {
							employeeEmailIdList.add((String) replyToId);
						}
					}
				} else {
					if (replyToId != null && !employeeEmailIdList.contains((String) replyToId)) {
						employeeEmailIdList.add((String) replyToId);
					}
				}

				if (organizationLogo == null || signature == null) {// || replyToId == null) {
					WarningCollector.collect("Kindly configure your Communication configuration from SETUP.");
				} else {
					String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);

					if (CURRENT_USER_EMAILID.equalsIgnoreCase("" + employeeDetails.opt("approverEmailId"))) {
						String title = "Approved Tour (" + employeeDetails.opt("tourCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.opt("employeeName") + ",<BR><BR>");
						messageContents.append("Your tour request detail as per below has been approved: <BR />");

						messageContents.append("<table border='0' width='99%'>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Code : </B>").append(employeeDetails.opt("tourCode")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Type : </B>").append(employeeDetails.opt("tourType")).append("</td></tr>");
						messageContents.append("<tr><td width='50%'>").append("<B>Depart On : </B>").append(startDate).append("</td>");
						messageContents.append("<td width='50%'>").append("<B>Arrive On : </B>").append(endDate).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Purpose : </B>").append(employeeDetails.opt("tourPurpose")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Advance Amount Required(If Any): </B>").append(employeeDetails.opt("totalAdvance") == null ? "" : employeeDetails.opt("totalAdvance")).append("</td></tr>");

						messageContents.append("<tr><td colspan='2'>").append("<B>Rejected By: </B>").append(employeeDetails.opt("approverName")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Rejecter Comments : </B>").append(approverComment).append("</td></tr>");

						messageContents.append("</table><BR /><BR />");

						messageContents.append("Regards, <BR>");
						messageContents.append(employeeDetails.opt("approverName") + "<BR>");
						messageContents.append("(" + organizationName + ")");

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.opt("employeeEmailId"));
								String[] ccEmailIds = (String[]) employeeEmailIdList.toArray(new String[employeeEmailIdList.size()]);
								mailSender.setCc(ccEmailIds);
								// mailSender.setReplyTo("" + employeeDetails.opt("approverEmailId"));
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (Exception e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					} else {
						String currentUserName = getCurrentUserName(CURRENT_USER_EMAILID);

						String title = "Approved Tour (" + employeeDetails.opt("tourCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Hello " + employeeDetails.opt("employeeName") + ",<BR><BR>");
						messageContents.append("Your Tour request has been approved. It has the following details: <br>");

						messageContents.append("<table border='0' width='99%'>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Code : </B>").append(employeeDetails.opt("tourCode")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Type : </B>").append(employeeDetails.opt("tourType")).append("</td></tr>");
						messageContents.append("<tr><td width='50%'>").append("<B>Depart On : </B>").append(startDate).append("</td>");
						messageContents.append("<td width='50%'>").append("<B>Arrive On : </B>").append(endDate).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Purpose : </B>").append(employeeDetails.opt("tourPurpose")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Advance Amount Required(If Any): </B>").append(employeeDetails.opt("totalAdvance") == null ? "" : employeeDetails.opt("totalAdvance")).append("</td></tr>");

						messageContents.append("<tr><td colspan='2'>").append("<B>Rejected By: </B>").append(currentUserName).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Rejecter Comments : </B>").append(approverComment).append("</td></tr>");

						messageContents.append("</table><BR /><BR />");

						messageContents.append("Regards, <BR>");
						messageContents.append(currentUserName + "<BR>");
						messageContents.append("(" + organizationName + ")");

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.opt("employeeEmailId"));
								String[] ccEmailIds = (String[]) employeeEmailIdList.toArray(new String[employeeEmailIdList.size()]);
								mailSender.setCc(ccEmailIds);
								// mailSender.setReplyTo(CURRENT_USER_EMAILID);
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (Exception e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error while sending mail : " + e.getMessage());
		}

	}

	private JSONObject putEmployeeDetailsIntoObject(Record record) throws Exception {
		Object key = record.getValue(Updates.KEY);
		Object employeeId = record.getValue("employeeid");
		Object branchId = record.getValue("employeeid.branchid");
		Object employeeCode = record.getValue("employeeid.employeecode");
		Object branchName = record.getValue("employeeid.branchid.name");
		Object approverIdObject = record.getValue("approverid");
		Object tourTypeIdObject = record.getValue("tourtypeid");
		Object startDateObject = record.getValue("startdate");
		Object endDateObject = record.getValue("enddate");
		Object advanceAmount = record.getValue("advanceamount_amount");
		Object advanceAgainstTickting = record.getValue("advance_against_tickting");
		String tourPurpose = Translator.stringValue(record.getValue("tourpurpose"));
		String tourCode = Translator.stringValue(record.getValue("tourcode"));
		String smsCode = Translator.stringValue(record.getValue("smscode"));

		String employeeEmailId = Translator.stringValue(record.getValue("employeeid.officialemailid"));
		String employeeName = Translator.stringValue(record.getValue("employeeid.name"));
		String employeeMobileNo = Translator.stringValue(record.getValue("employeeid.mobileno"));

		String approverEmailId = Translator.stringValue(record.getValue("approverid.officialemailid"));
		String approverName = Translator.stringValue(record.getValue("approverid.name"));
		String approverMobileNo = Translator.stringValue(record.getValue("approverid.mobileno"));

		String tourType = Translator.stringValue(record.getValue("tourtypeid.name"));
		Object genderId = record.getValue("employeeid.genderid");

		Object substituteName = record.getValue("substituteid.name");
		Object substituteGenderId = record.getValue("substituteid.genderid");

		int oldTourStatus = Translator.integerValue(record.getOldValue("tourstatusid"));

		String gender = "";
		String gender1 = "";
		if (Translator.integerValue(genderId) == 1) {
			gender = "Mr. ";
			gender1 = "His ";
		} else if (Translator.integerValue(genderId) == 2) {
			gender = "Ms. ";
			gender1 = "Her ";
		} else {
			gender = "Mr./Ms. ";
			gender1 = "His/Her ";
		}
		String substituteGender = "";
		String substituteGender1 = "";
		if (Translator.integerValue(substituteGenderId) == 1) {
			substituteGender = "Mr. ";
			substituteGender1 = "His ";
		} else if (Translator.integerValue(substituteGenderId) == 2) {
			substituteGender = "Ms. ";
			substituteGender1 = "Her ";
		} else {
			substituteGender = "Mr./Ms. ";
			substituteGender1 = "His/Her ";
		}

		JSONObject employeeDetails = new JSONObject();
		employeeDetails.put("key", key);
		employeeDetails.put("employeeId", employeeId);
		employeeDetails.put("approverIdObject", approverIdObject);
		employeeDetails.put("tourTypeIdObject", tourTypeIdObject);
		employeeDetails.put("startDateObject", startDateObject);
		employeeDetails.put("endDateObject", endDateObject);
		employeeDetails.put("tourPurpose", tourPurpose);
		employeeDetails.put("tourCode", tourCode);
		employeeDetails.put("smsCode", smsCode);
		employeeDetails.put("employeeEmailId", employeeEmailId);
		employeeDetails.put("employeeName", employeeName);
		employeeDetails.put("employeeMobileNo", employeeMobileNo);
		employeeDetails.put("approverEmailId", approverEmailId);
		employeeDetails.put("approverName", approverName);
		employeeDetails.put("approverMobileNo", approverMobileNo);
		employeeDetails.put("tourType", tourType);
		employeeDetails.put("oldTourStatus", oldTourStatus);
		employeeDetails.put("branchId", branchId);
		employeeDetails.put("employeeCode", employeeCode);
		employeeDetails.put("branchName", branchName);
		if ((Translator.doubleValue(advanceAmount) + Translator.doubleValue(advanceAgainstTickting)) > 0.0) {
			employeeDetails.put("totalAdvance", (Translator.doubleValue(advanceAmount) + Translator.doubleValue(advanceAgainstTickting)));
		}
		employeeDetails.put("gender", gender);
		employeeDetails.put("gender1", gender1);
		if (substituteName != null) {
			employeeDetails.put("substituteName", substituteName);
			employeeDetails.put("substituteGender", substituteGender);
			employeeDetails.put("substituteGender1", substituteGender1);
		}

		return employeeDetails;
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void approveTourSMS(JSONObject employeeDetails, String startDate, String endDate) {
		String message = "Tour request of " + employeeDetails.opt("employeeName") + " from " + startDate + " to " + endDate + " has been approved by " + employeeDetails.opt("approverName") + ".";
		// String otherMessage = "Leave request of " + employeeName + " from " + fromDate + " to " + toDate + " has been approved by " + otherApproverName + ".";

		List<Object> smsList = new ArrayList<Object>();
		List<String> employeeMobileNoList = new ArrayList<String>();
		employeeMobileNoList.add((String) employeeDetails.opt("employeeMobileNo"));

		String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);

		try {
			// String replyToID = null;
			boolean disableSMS;
			String replytoMobileNo = null;

			disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);
			replytoMobileNo = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_MOBILE);
			// replyToID = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);

			if (!disableSMS) {
				JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.opt("branchId"));
				int hrAssigningCount = (hrAssigningArray == null || hrAssigningArray.length() == 0) ? 0 : hrAssigningArray.length();
				if (hrAssigningCount > 0) {
					String hrMobileNo = (String) hrAssigningArray.getJSONObject(0).opt("mobileno");

					if (hrMobileNo != null && !employeeMobileNoList.contains((String) hrMobileNo)) {
						employeeMobileNoList.add((String) hrMobileNo);
					} else {
						if (replytoMobileNo != null && !employeeMobileNoList.contains((String) replytoMobileNo)) {
							employeeMobileNoList.add((String) replytoMobileNo);
						}
					}
				} else {
					if (replytoMobileNo != null && !employeeMobileNoList.contains((String) replytoMobileNo)) {
						employeeMobileNoList.add((String) replytoMobileNo);
					}
				}

				JSONObject query = new JSONObject();
				query.put(Data.Query.RESOURCE, "hris_employees");
				JSONArray columnArray = new JSONArray();
				columnArray.put("mobileno");
				columnArray.put("name");
				query.put(Data.Query.COLUMNS, columnArray);
				query.put(Data.Query.FILTERS, "officialemailid = '" + CURRENT_USER_EMAILID + "'");
				JSONObject employeeObject;
				employeeObject = ResourceEngine.query(query);
				JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");

				String currentUserMobileNo = employeeArray.getJSONObject(0).optString("mobileno");
				String currentUserName = employeeArray.getJSONObject(0).optString("name");

				if (employeeDetails.opt("approverMobileNo") != null && currentUserMobileNo.equalsIgnoreCase("" + employeeDetails.opt("approverMobileNo"))) {
					int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();

					for (int counter = 0; counter < smsToCount; counter++) {
						JSONObject smsTo = new JSONObject();
						smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
						smsTo.put(View.Action.Types.SendSMS.FROM, employeeDetails.opt("approverMobileNo"));
						smsTo.put(View.Action.Types.SendSMS.SMS, message);
						smsList.add(smsTo);
					}
					try {
						SendSMSService.sendSMSUtility(smsList.toArray());
					} catch (IOException e) {
						throw new BusinessLogicException("Error while send SMS by approver." + e.getMessage());
					}
				} else {
					String anotherMessage = "Tour request of " + employeeDetails.opt("employeeName") + " from " + startDate + " to " + endDate + " has been approved by " + currentUserName + ".";

					int smsToCount = employeeMobileNoList == null ? 0 : employeeMobileNoList.size();
					for (int counter = 0; counter < smsToCount; counter++) {
						JSONObject smsTo = new JSONObject();
						smsTo.put(View.Action.Types.SendSMS.TO, employeeMobileNoList.get(counter));
						smsTo.put(View.Action.Types.SendSMS.FROM, currentUserMobileNo);
						smsTo.put(View.Action.Types.SendSMS.SMS, anotherMessage);
						smsList.add(smsTo);
					}
					try {
						SendSMSService.sendSMSUtility(smsList.toArray());
					} catch (IOException e) {
						throw new BusinessLogicException("Error while send SMS by other user." + e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error come while send SMS " + e.getMessage());
		}
	}

	private String getLinkToApproveByMail(Object key, Object approverId) throws JSONException {
		StringBuffer messageContents = new StringBuffer();
		Object currentOrganization = getCurrentOrganization();
		messageContents.append("<BR />");
		messageContents.append("<table border='0' width='99%'> <tr><td>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvetour'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_APPROVED).append("' />");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Approve  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");

		messageContents.append("</td><td>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvetour' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='approvereject' VALUE='").append(HRISConstants.LEAVE_REJECTED).append("' />");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Reject  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr>");

		messageContents.append("<tr><td colspan='2'>");
		messageContents.append("<form action='http://apps.applane.com/escape/approvetour' method='GET'>");
		// messageContents.append("<form action='http://labs.applanecrmhrd.appspot.com/escape/approvetour' method='GET'>");
		messageContents.append("<INPUT TYPE='hidden' name='lr' VALUE='").append(key).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='orn' VALUE='").append(currentOrganization).append("' />");
		messageContents.append("<INPUT TYPE='hidden' name='apid' VALUE='").append(approverId).append("' />");
		messageContents.append("<INPUT TYPE='text' name='query' ").append("/>");
		messageContents.append("<input type='submit' style=\"background-color:#deeef5;border:1px solid #a3cfe4;text-align:center;border-radius:4px;padding:3px;font-weight:normal;color:#069\" value='  Send Query  '").append("' />&nbsp;&nbsp;&nbsp;");
		messageContents.append("</form>");
		messageContents.append("</td></tr></table>");

		messageContents.append("<BR />");
		return messageContents.toString();
	}

	public Object getCurrentOrganization() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "up_organizations");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("organization");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ in (" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID) + ")");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray organizationArray = employeeObject.getJSONArray("up_organizations");
		if (organizationArray != null && organizationArray.length() > 0) {
			return organizationArray.getJSONObject(0).opt("organization");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void approveTourMail(JSONObject employeeDetails, List<String> employeeEmailIdList, String startDate, String endDate, String approverComment) {

		ApplaneMail mailSender = new ApplaneMail();
		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String replyToId = (String) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.REPLY_TO_ID);
			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);

			String[] concernedPersons = (String[]) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.CONCERNED_PERSONS);
			for (int counter = 0; concernedPersons != null && (counter < concernedPersons.length); counter++) {
				String concernedPersonEmailId = concernedPersons[counter];

				if (concernedPersonEmailId != null && !employeeEmailIdList.contains((String) concernedPersonEmailId)) {
					employeeEmailIdList.add(concernedPersonEmailId);
				}
			}

			if (!disableEmail) {
				JSONArray hrAssigningArray = CommunicationUtility.getHRManagerRecord(employeeDetails.opt("branchId"));
				int hrAssigningCount = (hrAssigningArray == null || hrAssigningArray.length() == 0) ? 0 : hrAssigningArray.length();
				if (hrAssigningCount > 0) {
					String hrEmailId = (String) hrAssigningArray.getJSONObject(0).opt("employeeid.officialemailid");

					if (hrEmailId != null && !employeeEmailIdList.contains((String) hrEmailId)) {
						employeeEmailIdList.add((String) hrEmailId);
					} else {
						if (replyToId != null && !employeeEmailIdList.contains((String) replyToId)) {
							employeeEmailIdList.add((String) replyToId);
						}
					}
				} else {
					if (replyToId != null && !employeeEmailIdList.contains((String) replyToId)) {
						employeeEmailIdList.add((String) replyToId);
					}
				}

				if (organizationLogo == null || signature == null) {// || replyToId == null) {
					WarningCollector.collect("Kindly configure your Communication configuration from SETUP.");
				} else {
					String CURRENT_USER_EMAILID = (String) CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);

					if (CURRENT_USER_EMAILID.equalsIgnoreCase("" + employeeDetails.opt("approverEmailId"))) {
						String title = "Approved Tour (" + employeeDetails.opt("tourCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Dear " + employeeDetails.opt("employeeName") + ",<BR><BR>");
						messageContents.append("Your tour request detail as per below has been approved: <BR />");

						messageContents.append("<table border='0' width='99%'>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Code : </B>").append(employeeDetails.opt("tourCode")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Type : </B>").append(employeeDetails.opt("tourType")).append("</td></tr>");
						messageContents.append("<tr><td width='50%'>").append("<B>Depart On : </B>").append(startDate).append("</td>");
						messageContents.append("<td width='50%'>").append("<B>Arrive On : </B>").append(endDate).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Purpose : </B>").append(employeeDetails.opt("tourPurpose")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Advance Amount Required(If Any): </B>").append(employeeDetails.opt("totalAdvance") == null ? "" : employeeDetails.opt("totalAdvance")).append("</td></tr>");

						messageContents.append("<tr><td colspan='2'>").append("<B>Approved By: </B>").append(employeeDetails.opt("approverName")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Approver Comments : </B>").append(approverComment).append("</td></tr>");

						messageContents.append("</table><BR /><BR />");

						messageContents.append("Regards, <BR>");
						messageContents.append(employeeDetails.opt("approverName") + "<BR>");
						messageContents.append("(" + organizationName + ")");

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.opt("employeeEmailId"));
								String[] ccEmailIds = (String[]) employeeEmailIdList.toArray(new String[employeeEmailIdList.size()]);
								mailSender.setCc(ccEmailIds);
								// mailSender.setReplyTo("" + employeeDetails.opt("approverEmailId"));
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (Exception e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					} else {
						String currentUserName = getCurrentUserName(CURRENT_USER_EMAILID);

						String title = "Approved Tour (" + employeeDetails.opt("tourCode") + ")";
						StringBuffer messageContents = new StringBuffer();
						messageContents.append("<img src=\"" + organizationLogo + " \"/>");
						messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
						messageContents.append("Hello " + employeeDetails.opt("employeeName") + ",<BR><BR>");
						messageContents.append("Your Tour request has been approved. It has the following details: <br>");

						messageContents.append("<table border='0' width='99%'>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Code : </B>").append(employeeDetails.opt("tourCode")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Type : </B>").append(employeeDetails.opt("tourType")).append("</td></tr>");
						messageContents.append("<tr><td width='50%'>").append("<B>Depart On : </B>").append(startDate).append("</td>");
						messageContents.append("<td width='50%'>").append("<B>Arrive On : </B>").append(endDate).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Tour Purpose : </B>").append(employeeDetails.opt("tourPurpose")).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Advance Amount Required(If Any): </B>").append(employeeDetails.opt("totalAdvance") == null ? "" : employeeDetails.opt("totalAdvance")).append("</td></tr>");

						messageContents.append("<tr><td colspan='2'>").append("<B>Approved By: </B>").append(currentUserName).append("</td></tr>");
						messageContents.append("<tr><td colspan='2'>").append("<B>Approver Comments : </B>").append(approverComment).append("</td></tr>");

						messageContents.append("</table><BR /><BR />");

						messageContents.append("Regards, <BR>");
						messageContents.append(currentUserName + "<BR>");
						messageContents.append("(" + organizationName + ")");

						int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
						if (mailToCount > 0) {
							try {
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo("" + employeeDetails.opt("employeeEmailId"));
								String[] ccEmailIds = (String[]) employeeEmailIdList.toArray(new String[employeeEmailIdList.size()]);
								mailSender.setCc(ccEmailIds);
								// mailSender.setReplyTo(CURRENT_USER_EMAILID);
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							} catch (Exception e) {
								throw new BusinessLogicException("Some unknown error occured " + e.getMessage());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error while sending mail : " + e.getMessage());
		}
	}

	private String getCurrentUserName(String CURRENT_USER_EMAILID) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columnArray = new JSONArray();
		columnArray.put("name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "officialemailid = '" + CURRENT_USER_EMAILID + "'");
		JSONObject employeeObject;
		employeeObject = ResourceEngine.query(query);
		JSONArray employeeArray = employeeObject.getJSONArray("hris_employees");

		String currentUserName = employeeArray.getJSONObject(0).optString("name");
		return currentUserName;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void sendTourSMS(JSONObject employeeDetails, String startDate, String endDate, String smsCode) {

		String message = employeeDetails.optString("employeeName") + " has requested for tour from " + startDate + " to " + endDate + " and SMS code is " + smsCode + ".";
		List<Object> smsList = new ArrayList<Object>();
		try {
			boolean disableSMS;
			disableSMS = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.DISABLE_SMS);

			if (!disableSMS) {
				JSONObject smsTo = new JSONObject();
				smsTo.put(View.Action.Types.SendSMS.TO, employeeDetails.optString("approverMobileNo"));
				smsTo.put(View.Action.Types.SendSMS.FROM, employeeDetails.optString("employeeMobileNo"));
				smsTo.put(View.Action.Types.SendSMS.SMS, message);
				smsList.add(smsTo);
				try {
					SendSMSService.sendSMSUtility(smsList.toArray());
				} catch (Exception e) {
					throw new BusinessLogicException("Error while send SMS.. " + e.getMessage());
				}
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Error come while send SMS... " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void sendTourMailOnInsertUpdate(List<String> employeeEmailIdList, String startDate, String endDate, JSONObject employeeDetails, Map<String, Object> indirectReportingToMap) {
		ApplaneMail mailSender = new ApplaneMail();

		try {
			boolean disableEmail = (Boolean) ((Map<String, Object>) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.Type.TOUR_CONFIG)).get(Communication_Constants.Properties.DISABLE_MAIL);
			String organizationLogo = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_LOGO);
			String signature = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_SIGNATURE);
			String organizationName = (String) CommunicationUtility.getCommunicationInfo().get(Communication_Constants.ORGANIZATION_NAME);

			if (!disableEmail) {
				if (organizationLogo == null || signature == null) {
					WarningCollector.collect("Kindly configure your Communication from SETUP.");
				} else {
					String title = "Request For Tour (" + employeeDetails.opt("tourCode") + ")";

					int mailToCount = employeeEmailIdList == null ? 0 : employeeEmailIdList.size();
					List<String> list = new ArrayList<String>();
					for (int counter = 0; counter < mailToCount; counter++) {
						try {
							String mailTo = employeeEmailIdList.get(counter);
							if (!list.contains(mailTo)) {
								list.add(mailTo);

								StringBuffer messageContents = new StringBuffer();
								messageContents.append("<img src=\"" + organizationLogo + " \"/>");
								messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
								messageContents.append("Dear Sir/Madam,<BR><BR>");
								messageContents.append(employeeDetails.opt("gender")).append(" ").append(employeeDetails.opt("employeeName")).append(" (Employee Code ").append(employeeDetails.opt("employeeCode")).append(") working in ").append(employeeDetails.opt("branchName")).append(" has entered ").append(employeeDetails.opt("gender1")).append(" tour request. Detail as given below: <BR /><BR />");

								messageContents.append("<table border='0' width='99%'>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Tour Code : </B>").append(employeeDetails.opt("tourCode")).append("</td></tr>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Tour Type : </B>").append(employeeDetails.opt("tourType")).append("</td></tr>");
								messageContents.append("<tr><td width='50%'>").append("<B>Depart On : </B>").append(startDate).append("</td>");
								messageContents.append("<td width='50%'>").append("<B>Arrive On : </B>").append(endDate).append("</td></tr>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Tour Purpose : </B>").append(employeeDetails.opt("tourPurpose")).append("</td></tr>");
								messageContents.append("<tr><td colspan='2'>").append("<B>Advance Amount Required (If Any): </B>").append(employeeDetails.opt("totalAdvance") == null ? "" : employeeDetails.opt("totalAdvance")).append("</td></tr>");
								messageContents.append("</table><BR />");
								if (employeeDetails.has("substituteName")) {
									messageContents.append(employeeDetails.opt("substituteGender")).append(employeeDetails.opt("substituteName")).append(" would be  ").append(employeeDetails.opt("gender1")).append(" substitute during tour period.").append("<BR />");
								}

								if (!mailTo.equals(employeeDetails.opt("employeeEmailId"))) {
									String link = getLinkToApproveByMail(employeeDetails.opt("key"), employeeDetails.opt("approverIdObject"));
									messageContents.append(link);
								} else if (indirectReportingToMap.containsKey(employeeEmailIdList.get(counter))) {
									messageContents.append(getLinkToApproveByMail(employeeDetails.opt("key"), indirectReportingToMap.get(employeeEmailIdList.get(counter))));
								}
								messageContents.append("<BR />Regards, <BR />");
								messageContents.append(employeeDetails.opt("employeeName") + "<BR />");
								messageContents.append("(" + organizationName + ")");
								mailSender.setMessage(title, messageContents.toString(), true);
								mailSender.setTo(mailTo);
								// mailSender.setReplyTo("" + employeeDetails.opt("employeeEmailId"));
								mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
							}
						} catch (ApplaneMailException e) {
							throw new RuntimeException("Some unknown error occured " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e1) {
			throw new RuntimeException("Some unknown error occured while send mail " + e1.getMessage());
		}
	}

	public void onBeforeUpdate(Record record) throws DeadlineExceededException {

	}

}
