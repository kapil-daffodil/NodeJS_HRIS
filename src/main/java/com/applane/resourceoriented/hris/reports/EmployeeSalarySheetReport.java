package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.NumberFormats;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.scheduler.MorningSchdularHRIS;
import com.applane.resourceoriented.hris.utility.ApplicationConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class EmployeeSalarySheetReport extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// private static final Object JSONArray = null;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		salarySheetExcelReport(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}

	static ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();

	private void salarySheetExcelReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Salary_Sheet.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");

		try {
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet sheet = workbook.createSheet("First Sheet", 0);

			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat hdFormat = new WritableCellFormat(font);
			hdFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
			hdFormat.setAlignment(Alignment.CENTRE);

			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);
			String monthName = null;

			String yearName = null;
			String key = req.getParameter("keys");
			if (key.length() > 2) {
				key = key.substring(1, key.length() - 1);
			}
			String[] keys = key.split(",");
			key = keys[0];

			JSONArray salaryComponentArray = getSalaryComponents();

			int salaryComponentArrayCount = (salaryComponentArray == null || salaryComponentArray.length() == 0) ? 0 : salaryComponentArray.length();
			LinkedHashMap<Integer, String> componentsMap = new LinkedHashMap<Integer, String>();
			LinkedHashMap<Integer, Object[]> employRecordsForFirstComponents = new LinkedHashMap<Integer, Object[]>();
			LinkedHashMap<Integer, Object[]> employRecordsForSecondComponents = new LinkedHashMap<Integer, Object[]>();
			if (salaryComponentArrayCount > 0) {

				for (int counter = 0; counter < salaryComponentArrayCount; counter++) {
					// 16 salary components like HRA, basic
					String salaryComponent = salaryComponentArray.getJSONObject(counter).optString("name");
					int salaryComponentId = salaryComponentArray.getJSONObject(counter).optInt("__key__");
					componentsMap.put(salaryComponentId, salaryComponent);
				}
			}

			JSONArray takeSalarySheetArray = getTakeSalarySheetRecord(key);

			int takeSalarySheetArrayCount = (takeSalarySheetArray == null || takeSalarySheetArray.length() == 0) ? 0 : takeSalarySheetArray.length();
			String organizationName = "";
			String organizationAddress = "";
			// String organizationLogo = "";
			Object branchName = "";
			int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			if (takeSalarySheetArrayCount > 0) {

				Object branchId = takeSalarySheetArray.getJSONObject(0).opt("branchid");
				Object monthId = takeSalarySheetArray.getJSONObject(0).opt("monthid");
				branchName = takeSalarySheetArray.getJSONObject(0).opt("branchid.name");
				if (monthId != null) {
					monthName = getMonthName(monthId);
				}
				Object yearId = takeSalarySheetArray.getJSONObject(0).opt("yearid");
				if (yearId != null) {
					yearName = getYearName(yearId);
				}

				Date monthFirstDate = null;
				if (monthName != null && monthName.length() > 0 && yearName != null && yearName.length() > 0) {
					monthFirstDate = DataTypeUtilities.getMonthFirstDate(yearName, monthName);
				}
				Date monthLastDate = DataTypeUtilities.getMonthLastDate(monthFirstDate);

				long maxDayInMonth = 0;
				if (monthFirstDate != null) {
					maxDayInMonth = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDate);
				}
				JSONArray salarySheetArray = getSalarySheetRecords(monthId, yearId, branchId);
				JSONArray configurationSetup = getConfigurationSetup();
				if (configurationSetup != null && configurationSetup.length() > 0) {
					organizationName = configurationSetup.getJSONObject(0).optString("organizationname");
					organizationAddress = configurationSetup.getJSONObject(0).optString("address");
					// organizationLogo = getLogo(configurationSetup.getJSONObject(0).optString("organizationlogo"));
				}
				int salarySheetArrayCount = (salarySheetArray == null || salarySheetArray.length() == 0) ? 0 : salarySheetArray.length();
				if (salarySheetArrayCount > 0) {
					double salaryHoldDays = 0;
					for (int counter = 0; counter < salarySheetArrayCount; counter++) {
						HashMap<Integer, Object[]> employComponentFirstDetail = new HashMap<Integer, Object[]>();
						HashMap<Integer, Object[]> employComponentSecondDetail = new HashMap<Integer, Object[]>();
						Object[] employeeDetailFirst = new Object[18];
						Object[] employeeDetailSecond = new Object[18];

						int employeeId = salarySheetArray.getJSONObject(counter).optInt("employeeid");
						String employeeName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.name"));
						String employeeCode = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.employeecode"));
						String accountNo = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("employeeid.accountno"));
						String currentBranchName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("sub_branchid.name"));
						String departmentName = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("sub_branchid.name"));

						String invoiceNumber = DataTypeUtilities.stringValue(salarySheetArray.getJSONObject(counter).opt("invoicenumber"));

						double presentDays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("present"));
						double leaves = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("leaves"));
						// double totalOff = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("totaloff"));
						double nonWorkingDays = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("nonworkingdays"));
						double ewds = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("extraworkingday"));
						double nonPayableLeaves = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("nonpayableleaves"));
						if (organizationId == 10719) {
							salaryHoldDays = getSalaryHoldDays(employeeId);
						}
						double presentDaysFirst = 0.0;
						double leavesFirst = 0.0;
						double nonWorkingDaysFirst = 0.0;
						double nonPayableLeavesFirst = 0.0;
						double ewdsFirst = 0.0;
						double presentDaysSecond = 0.0;
						double leavesSecond = 0.0;
						double nonWorkingDaysSecond = 0.0;
						double nonPayableLeavesSecond = 0.0;
						double ewdsSecond = 0.0;
						// double nonWorkingDays = 0.0;
						// double attendanceBasedAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("attendancebasedamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).optDouble("attendancebasedamount"))).doubleValue();
						// double performanceBasedAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("performancebasedamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).optDouble("performancebasedamount"))).doubleValue();
						// double fixedBasedAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("fixedamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).opt("fixedamount"))).doubleValue();
						// double deductionAmount = ((Number) (salarySheetArray.getJSONObject(counter).opt("deductionamount") == null ? 0.0 : salarySheetArray.getJSONObject(counter).opt("deductionamount"))).doubleValue();
						// double nonPayableLeaves = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("nonpayableleaves"));
						// double payableAmount = DataTypeUtilities.doubleValue(salarySheetArray.getJSONObject(counter).opt("payableamount"));
						JSONArray monthlySalaryArray = getEmployeeMonthlySalaryRecords(employeeId, monthId, yearId, invoiceNumber);
						double payableAmountFirst = 0.0;
						double payableAmountSecond = 0.0;
						double totalActualAmountFirst = 0d;
						double totalGrossAmountFirst = 0d;
						double totalActualAmountSecond = 0d;
						double totalGrossAmountSecond = 0d;
						JSONArray monthlyAttendance = getMonthlyAttendance(employeeId, yearId, monthId);
						if (monthlySalaryArray != null && monthlySalaryArray.length() > 0 && monthlyAttendance != null && monthlyAttendance.length() > 0) {
							double actualNonPayAbleLeaves = Translator.doubleValue(monthlyAttendance.getJSONObject(0).opt("nonpayableleaves"));
							for (int counter1 = 0; counter1 < monthlySalaryArray.length(); counter1++) {
								Object[] employComponentArray = new Object[3];

								int salaryComponentId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid");
								int paymentTypeId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid.paymenttypeid");
								int isInFirstComponents = Translator.integerValue(monthlySalaryArray.getJSONObject(counter1).opt("isinfirstcomponents"));

								// int escId = monthlySalaryArray.getJSONObject(counter1).optInt("employeesalarycomponentid");
								int componentTypeId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID);
								// int monthIdFromComponents = monthlySalaryArray.getJSONObject(counter1).optInt("salarymonthid");
								// int yearIdFromComponents = monthlySalaryArray.getJSONObject(counter1).optInt("salaryyearid");
								int payToId = monthlySalaryArray.getJSONObject(counter1).optInt("salarycomponentid." + SalaryComponentKinds.AMOUNT_PAY_TO_ID);
								boolean isFirstTemp = false;
								boolean isSecondTemp = false;
								if (componentTypeId == HRISApplicationConstants.SalaryComponentTypes.BASIC) {
									Object applicableFrom = monthlySalaryArray.getJSONObject(counter1).opt("employeesalarycomponentid.applicablefrom");
									Object applicableTo = monthlySalaryArray.getJSONObject(counter1).opt("employeesalarycomponentid.applicableto");

									Date applicableFromDate = Translator.dateValue(applicableFrom);
									Date applicableToDate = Translator.dateValue(applicableTo);
									boolean isFirst = false;
									if (monthFirstDate.after(applicableFromDate) || monthFirstDate.equals(applicableFromDate)) {
										isFirst = true;
										isFirstTemp = true;
										applicableFrom = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
									}

									if (monthLastDate.before(applicableToDate) || monthLastDate.equals(applicableToDate)) {
										isSecondTemp = true;
										applicableTo = EmployeeSalaryGenerationServlet.getDateInString(monthLastDate);
									}

									JSONArray attendance = getDailyAttendance(employeeId, applicableFrom, applicableTo);
									if (attendance != null && attendance.length() > 0) {
										double presentDaysTemp = 0.0;
										double leaveDaysTemp = 0.0;
										double ewdDaysTemp = 0.0;
										double nonWorkingDaysTemp = 0.0;

										for (int innerCounter = 0; innerCounter < attendance.length(); innerCounter++) {
											int attendanceTypeId = Translator.integerValue(attendance.getJSONObject(innerCounter).opt("attendancetypeid"));
											int paidStatusId = Translator.integerValue(attendance.getJSONObject(innerCounter).opt("paidstatus"));
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
												presentDaysTemp += 1.0;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
												leaveDaysTemp += 1.0;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
												leaveDaysTemp += 0.50;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HOLIDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
												nonWorkingDaysTemp++;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && paidStatusId == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_PAYABLE) {
												ewdDaysTemp += 1.0;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF && paidStatusId == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_PAYABLE) {
												ewdDaysTemp += 0.50;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
												presentDaysTemp += 0.50;
											}
										}
										if (isFirst) {
											presentDaysFirst = presentDaysTemp;
											nonWorkingDaysFirst = nonWorkingDaysTemp;
											ewdsFirst = ewdDaysTemp;
											leavesFirst = leaveDaysTemp;
											if (leaveDaysTemp > actualNonPayAbleLeaves || leaveDaysTemp == actualNonPayAbleLeaves) {
												nonPayableLeavesFirst = actualNonPayAbleLeaves;
												actualNonPayAbleLeaves = 0.0;
											} else {
												nonPayableLeavesFirst = leaveDaysTemp;
												actualNonPayAbleLeaves = actualNonPayAbleLeaves - leaveDaysTemp;
											}
										} else {
											presentDaysSecond = presentDaysTemp;
											nonWorkingDaysSecond = nonWorkingDaysTemp;
											ewdsSecond = ewdDaysTemp;
											leavesSecond = leaveDaysTemp;
											if (leaveDaysTemp > actualNonPayAbleLeaves || leaveDaysTemp == actualNonPayAbleLeaves) {
												nonPayableLeavesSecond = actualNonPayAbleLeaves;
												actualNonPayAbleLeaves = 0.0;
											} else {
												nonPayableLeavesSecond = leaveDaysTemp;
												actualNonPayAbleLeaves = actualNonPayAbleLeaves - leaveDaysTemp;
											}
										}
									} else {
										presentDaysFirst = presentDays;
										nonWorkingDaysFirst = nonWorkingDays;
										ewdsFirst = ewds;
										leavesFirst = leaves;
										nonPayableLeavesFirst = nonPayableLeaves;
									}
								}
								if ((!isFirstTemp && isSecondTemp) || (isFirstTemp && !isSecondTemp)) {
									JSONArray attendance = getDailyAttendance(employeeId, EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate), EmployeeSalaryGenerationServlet.getDateInString(monthLastDate));
									if (attendance != null && attendance.length() > 0) {
										double presentDaysTemp = 0.0;
										double leaveDaysTemp = 0.0;
										double ewdDaysTemp = 0.0;
										double nonWorkingDaysTemp = 0.0;

										for (int innerCounter = 0; innerCounter < attendance.length(); innerCounter++) {
											int attendanceTypeId = Translator.integerValue(attendance.getJSONObject(innerCounter).opt("attendancetypeid"));
											int paidStatusId = Translator.integerValue(attendance.getJSONObject(innerCounter).opt("paidstatus"));
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
												presentDaysTemp += 1.0;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
												leaveDaysTemp += 1.0;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
												leaveDaysTemp += 0.50;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_OFF || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_HOLIDAY || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
												nonWorkingDaysTemp++;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY && paidStatusId == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_PAYABLE) {
												ewdDaysTemp += 1.0;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF && paidStatusId == HRISApplicationConstants.LeavePaidStatus.LEAVE_STATUS_PAYABLE) {
												ewdDaysTemp += 0.50;
											}
											if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME_HALF_DAY) {
												presentDaysTemp += 0.50;
											}
										}
										presentDaysFirst = presentDaysTemp;
										nonWorkingDaysFirst = nonWorkingDaysTemp;
										ewdsFirst = ewdDaysTemp;
										leavesFirst = leaveDaysTemp;
										if (leaveDaysTemp > actualNonPayAbleLeaves || leaveDaysTemp == actualNonPayAbleLeaves) {
											nonPayableLeavesFirst = actualNonPayAbleLeaves;
											actualNonPayAbleLeaves = 0.0;
										} else {
											nonPayableLeavesFirst = leaveDaysTemp;
											actualNonPayAbleLeaves = actualNonPayAbleLeaves - leaveDaysTemp;
										}
									}
								}
								String monthlySalaryComponent = monthlySalaryArray.getJSONObject(counter1).optString("salarycomponentid.name");
								double receivedAmount = DataTypeUtilities.doubleValue(monthlySalaryArray.getJSONObject(counter1).opt("amount"));
								double actualAmount = DataTypeUtilities.doubleValue(monthlySalaryArray.getJSONObject(counter1).opt("actualamount"));

								// if (employComponentDetail.containsKey(salaryComponentId)) {
								// employComponentArray = employComponentDetail.get(salaryComponentId); // if component already contain
								// double receivedAmountInner = (Double) employComponentArray[1];
								// double actualAmountInner = (Double) employComponentArray[2];
								// employComponentArray[1] = receivedAmountInner + receivedAmount;
								// employComponentArray[2] = actualAmountInner + actualAmount;
								// } else
								employComponentArray[0] = monthlySalaryComponent;
								employComponentArray[1] = receivedAmount;
								employComponentArray[2] = actualAmount;
								if (isInFirstComponents == 0 || isInFirstComponents == HRISApplicationConstants.EmployeeDecision.YES) {
									if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
										totalActualAmountFirst += actualAmount;
										totalGrossAmountFirst += receivedAmount;
										if (payToId != HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
											payableAmountFirst += receivedAmount;
										}
									}
									if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
										payableAmountFirst -= receivedAmount;
									}
									employComponentFirstDetail.put(salaryComponentId, employComponentArray);
								} else {
									if (paymentTypeId == HRISApplicationConstants.PAYABLE) {
										totalActualAmountSecond += actualAmount;
										totalGrossAmountSecond += receivedAmount;
										if (payToId != HRISApplicationConstants.AMOUNT_TO_GOVERNMENT) {
											payableAmountSecond += receivedAmount;
										}
									}
									if (paymentTypeId == HRISApplicationConstants.DEDUCTABLE) {
										payableAmountSecond -= receivedAmount;

									}
									employComponentSecondDetail.put(salaryComponentId, employComponentArray);
								}

							}
						}
						employeeDetailFirst[0] = employeeName;
						employeeDetailFirst[1] = counter + 1;
						employeeDetailFirst[2] = employeeCode;
						employeeDetailFirst[3] = employComponentFirstDetail;
						employeeDetailFirst[4] = accountNo;
						employeeDetailFirst[5] = (int) (payableAmountFirst + 0.5);
						employeeDetailFirst[6] = (int) (totalActualAmountFirst + 0.5);

						employeeDetailFirst[7] = maxDayInMonth; // no of days in month
						employeeDetailFirst[8] = presentDaysFirst;
						employeeDetailFirst[9] = leavesFirst;
						employeeDetailFirst[10] = nonWorkingDaysFirst;
						employeeDetailFirst[11] = ewdsFirst;
						employeeDetailFirst[12] = (presentDaysFirst + (leavesFirst - nonPayableLeavesFirst) + nonWorkingDaysFirst + ewdsFirst);
						employeeDetailFirst[13] = nonPayableLeavesFirst;
						employeeDetailFirst[14] = (int) (totalGrossAmountFirst + 0.5);
						employeeDetailFirst[15] = currentBranchName;
						employeeDetailFirst[16] = departmentName;
						employeeDetailFirst[17] = salaryHoldDays;

						employeeDetailSecond[0] = employeeName;
						employeeDetailSecond[1] = counter + 1;
						employeeDetailSecond[2] = employeeCode;
						employeeDetailSecond[3] = employComponentSecondDetail;
						employeeDetailSecond[4] = accountNo;
						employeeDetailSecond[5] = (int) (payableAmountSecond + 0.5);
						employeeDetailSecond[6] = (int) (totalActualAmountSecond + 0.5);

						employeeDetailSecond[7] = maxDayInMonth; // no of days in month
						employeeDetailSecond[8] = presentDaysSecond;
						employeeDetailSecond[9] = leavesSecond;
						employeeDetailSecond[10] = nonWorkingDaysSecond;
						employeeDetailSecond[11] = ewdsSecond;
						employeeDetailSecond[12] = (presentDaysSecond + (leavesSecond - nonPayableLeavesSecond) + nonWorkingDaysSecond + ewdsSecond);
						employeeDetailSecond[13] = nonPayableLeavesSecond;
						employeeDetailSecond[14] = (int) (totalGrossAmountSecond + 0.5);
						employeeDetailSecond[15] = currentBranchName;
						employeeDetailSecond[16] = departmentName;
						employeeDetailSecond[17] = salaryHoldDays;

						employRecordsForFirstComponents.put(employeeId, employeeDetailFirst);
						if (employComponentSecondDetail.size() > 0) {
							employRecordsForSecondComponents.put(employeeId, employeeDetailSecond);
						}
					}
				}
			}
			// LogUtility.writeLog("employRecordsForFirstComponents >> " + employRecordsForFirstComponents + " << employRecordsForSecondComponents >> " + employRecordsForSecondComponents);
			int[] row = { 9 };
			int column = 0;
			WritableFont font1 = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
			WritableCellFormat hdFormat1 = new WritableCellFormat(font1);

			hdFormat1.setBorder(Border.ALL, BorderLineStyle.THIN);
			hdFormat1.setAlignment(Alignment.CENTRE);

			sheet.mergeCells(column + 4, 4, column + 10, 6);
			setValueInSheet(sheet, font1, hdFormat1, 4, column + 4, organizationName);

			sheet.mergeCells(column + 4, 7, column + 10, 7);
			setValueInSheet(sheet, font, hdFormat, 7, column + 4, organizationAddress);

			sheet.mergeCells(column + 4, row[0] - 1, column + 10, row[0] - 1);
			setValueInSheet(sheet, font1, hdFormat1, row[0] - 1, column + 4, "Employee Salary Sheet for " + monthName + "-" + yearName);
			sheet.setColumnView(column, 8);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Sr. No.");
			sheet.setColumnView(column, 25);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Employee Name");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Employee Code");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Branch Name");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Current Location");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Account No.");
			sheet.setColumnView(column, 20);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Department");
			if (organizationId == 10719) {
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row[0], column++, "Salary Hold Days");
			}
			for (Integer componentId : componentsMap.keySet()) {
				String componentName = componentsMap.get(componentId);
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row[0], column++, componentName);
			}
			sheet.setColumnView(column, 11);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Total");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "No. of Days in Month");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Presents Days");
			sheet.setColumnView(column, 11);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Leaves");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Non Working Days");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Non Payable Leaves");
			sheet.setColumnView(column, 15);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "EWD(s)");
			sheet.setColumnView(column, 12);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Salary Days");
			for (Integer componentId : componentsMap.keySet()) {
				String componentName = componentsMap.get(componentId);
				// received package component name write into sheet
				sheet.setColumnView(column, 20);
				setValueInSheet(sheet, font, hdFormat, row[0], column++, componentName);
			}
			sheet.setColumnView(column, 25);
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Gross Amount");
			setValueInSheet(sheet, font, hdFormat, row[0], column++, "Net Payable Amount");
			for (Integer employeeId : employRecordsForFirstComponents.keySet()) {
				putAllValuesIntoSheet(sheet, numbercellformat, componentsMap, employRecordsForFirstComponents, row, employeeId, branchName);
				if (employRecordsForSecondComponents.containsKey(employeeId)) {
					putAllValuesIntoSheet(sheet, numbercellformat, componentsMap, employRecordsForSecondComponents, row, employeeId, branchName);
				}
			}
			workbook.write();

			workbook.close();
			// } catch (DeadlineExceededException e) {
			// LogUtility.writeLog("First exception block...");
			// throw e;
			// } catch (BusinessLogicException e) {
			// LogUtility.writeLog("Second exception block");
			// throw e;
		} catch (Exception e) {
			LogUtility.writeLog("Salary sheet Excel Report Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new BusinessLogicException(e.getMessage());
		}
	}

	private double getSalaryHoldDays(int employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "hris_employees_additional_information");
		columnArray.put("solary_on_hold_days");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId);
		JSONArray array = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees_additional_information");
		if (array != null && array.length() > 0) {
			return Translator.integerValue(array.getJSONObject(0).opt("solary_on_hold_days"));
		}
		return 0;
	}

	private JSONArray getMonthlyAttendance(int employeeId, Object yearId, Object monthId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeemonthlyattendance");
		columnArray.put("nonpayableleaves");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND monthid = " + monthId + " AND yearid = " + yearId);
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeemonthlyattendance");
	}

	private JSONArray getDailyAttendance(int employeeId, Object applicableFrom, Object applicableTo) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, "employeeattendance");
		columnArray.put("attendancetypeid");
		columnArray.put("paidstatus");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " AND attendancedate >= '" + applicableFrom + "' AND attendancedate <= '" + applicableTo + "'");
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("employeeattendance");
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

	private void putAllValuesIntoSheet(WritableSheet sheet, WritableCellFormat numbercellformat, LinkedHashMap<Integer, String> componentsMap, LinkedHashMap<Integer, Object[]> employRecordsForFirstComponents, int[] row, Integer employeeId, Object branchName) throws RowsExceededException, WriteException, NumberFormatException {
		int column;
		int organizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		row[0]++;
		column = 0;
		Object[] employeeDetail = employRecordsForFirstComponents.get(employeeId);
		putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[1], row[0], column++); // serial no.
		putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[0], row[0], column++); // name of employee
		putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[2], row[0], column++); // e.code
		putHeaderDefault(numbercellformat, sheet, "" + branchName, row[0], column++); // branch name
		putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[15], row[0], column++); // current branch name
		putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[4], row[0], column++); // a/c no.
		putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[16], row[0], column++); // department name

		if (organizationId == 10719) {
			putHeaderDefault(numbercellformat, sheet, "" + employeeDetail[17], row[0], column++); // department name
		}

		@SuppressWarnings("unchecked")
		HashMap<Integer, Object[]> employComponentDetail = (HashMap<Integer, Object[]>) employeeDetail[3];
		// LogUtility.writeLog("employComponentDetail << " + employComponentDetail);
		for (Integer componentId : componentsMap.keySet()) {
			Object[] componentArray = employComponentDetail.get(componentId);
			if (componentArray != null) {
				putHeader(numbercellformat, sheet, DataTypeUtilities.doubleValue(componentArray[2]), row[0], column++); // write actual amount
			} else {
				putHeaderDefault(numbercellformat, sheet, "", row[0], column++); // write blank if component not assigned to employee
			}
		}
		putHeader(numbercellformat, sheet, (int) (Double.parseDouble("" + employeeDetail[6]) + 0.50), row[0], column++);
		putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[7]), row[0], column++);
		putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[8]), row[0], column++);
		putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[9]), row[0], column++);
		putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[10]), row[0], column++);
		putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[13]), row[0], column++);
		putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[11]), row[0], column++);
		putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[12]), row[0], column++);
		// putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[15]), row[0], column++);
		// putHeader(numbercellformat, sheet, Double.parseDouble("" + employeeDetail[14]), row[0], column++);
		for (Integer componentId : componentsMap.keySet()) {
			Object[] componentArray = employComponentDetail.get(componentId);
			if (componentArray != null && ("" + componentArray[1]).length() > 0) {
				// LogUtility.writeLog("componentArray[1] > [" + componentArray[1] + "]");
				putHeader(numbercellformat, sheet, (int) (Double.parseDouble("" + componentArray[1]) + 0.50), row[0], column++); // write received amount
			} else {
				putHeaderDefault(numbercellformat, sheet, "", row[0], column++); // write blank if component not assigned to employee
			}
		}
		putHeader(numbercellformat, sheet, (int) Double.parseDouble("" + employeeDetail[14]), row[0], column++);
		putHeader(numbercellformat, sheet, (int) Double.parseDouble("" + employeeDetail[5]), row[0], column++);
	}

	// private JSONArray getAttendanceFromComponentMonthlyAmount(int employeeId, int escId, int monthIdFromComponents, int yearIdFromComponents, int isInFirstComponents) throws JSONException {
	// JSONObject query = new JSONObject();
	// JSONArray columnArray = new JSONArray();
	// query.put(Data.Query.RESOURCE, "componentmonthlyamount");
	// columnArray.put("present");
	// columnArray.put("absent");
	// columnArray.put("actualnonworkingdays");
	// columnArray.put("ewd");
	// columnArray.put("leaves");
	// columnArray.put("nonpayableleaves");
	// columnArray.put("isinfirstcomponents");
	// query.put(Data.Query.COLUMNS, columnArray);
	// query.put(Data.Query.FILTERS, "employeeid=" + employeeId + " and employeesalarycomponentid=" + escId + " and monthid = " + monthIdFromComponents + " and yearid=" + yearIdFromComponents);
	// return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("componentmonthlyamount");
	// }

	public void setValueInSheet(WritableSheet sheet, WritableFont font, WritableCellFormat hdFormat, int rowNo, int cellNo, String value) throws WriteException, RowsExceededException {
		WritableCellFormat newFormat = new WritableCellFormat(hdFormat);
		Label label;
		newFormat.setFont(font);
		newFormat.setWrap(true);
		label = new Label(cellNo, rowNo, value, newFormat);
		sheet.addCell(label);
	}

	public void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		// Label label = new Label(column, row, value, cellFormat);
		// sheet.addCell(label);

		Number numobj = new Number(column, row, value, cellFormat);
		sheet.addCell(numobj);
	}

	public void putHeaderDefault(WritableCellFormat numbercellformat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value, numbercellformat);
		sheet.addCell(label);
	}

	public JSONArray getSalaryComponents() throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarycomponents");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("name");
		columnArray.put("ignoreinexceptionreport");
		columnArray.put("compulsoryifdefined");
		query.put(Data.Query.COLUMNS, columnArray);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarycomponents");
		return array;
	}

	public JSONArray getEmployeeMonthlySalaryRecords(Object employeeId, Object monthId, Object yearId, String invoiceNumber) throws JSONException {
		String filters = "";
		if (invoiceNumber.length() > 0) {
			filters += " and invoicenumber = '" + invoiceNumber + "'";
		}
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "employeemonthlysalarycomponents ");
		JSONArray columnArray = new JSONArray();
		columnArray.put("salarycomponentid");
		columnArray.put("salarycomponentid.name");
		columnArray.put("salarycomponentid.paymenttypeid");
		columnArray.put("salarycomponentid." + SalaryComponentKinds.COMPONENT_TYPE_ID);
		columnArray.put("salarycomponentid." + SalaryComponentKinds.AMOUNT_PAY_TO_ID);
		columnArray.put("amount");
		columnArray.put("actualamount");
		columnArray.put("actualamount");
		columnArray.put("salarymonthid");
		columnArray.put("salaryyearid");
		columnArray.put("employeesalarycomponentid");
		columnArray.put("employeesalarycomponentid.applicablefrom");
		columnArray.put("employeesalarycomponentid.applicableto");
		columnArray.put("isinfirstcomponents");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "employeeid = " + employeeId + " and salarymonthid = " + monthId + " and salaryyearid = " + yearId + filters);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("employeemonthlysalarycomponents ");
		return array;
	}

	JSONArray getSalarySheetRecords(Object monthId, Object yearId, Object branchId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "salarysheet");
		JSONArray columnArray = new JSONArray();
		columnArray.put("__key__");
		columnArray.put("employeeid");
		columnArray.put("employeeid.name");
		columnArray.put("employeeid.panno");
		columnArray.put("employeeid.employeecode");
		columnArray.put("employeeid.accountno");
		columnArray.put("employeeid.bankname");
		columnArray.put("employeeid.departmentid.name");
		columnArray.put("present");
		columnArray.put("leaves");
		columnArray.put("monthid");
		columnArray.put("yearid");
		columnArray.put("totalworkingdays");
		columnArray.put("totaloff");
		columnArray.put("extraworkingday");
		columnArray.put("holidays");
		columnArray.put("nonworkingdays");
		columnArray.put("attendancebasedamount");
		columnArray.put("nonpayableleaves");
		columnArray.put("performancebasedamount");
		columnArray.put("fixedamount");
		columnArray.put("deductionamount");
		columnArray.put("payableamount");
		columnArray.put("invoicenumber");
		columnArray.put("sub_branchid.name");
		JSONObject order = new JSONObject();
		order.put(Data.Query.Orders.EXPERSSION, "employeeid.name");
		order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
		JSONArray orderByExpression = new JSONArray();
		orderByExpression.put(order);
		query.put(Data.Query.ORDERS, orderByExpression);

		query.put(Data.Query.COLUMNS, columnArray);
		// query.put(Data.Query.FILTERS, "branchid= " + branchId + " and monthid = " + monthId + " and yearid = " + yearId + " and employeeid=124");
		query.put(Data.Query.FILTERS, "branchid= " + branchId + " and monthid = " + monthId + " and yearid = " + yearId + " and isarrear = NULL");
		query.put(Data.Query.OPTION_FILTERS, "branchid= " + branchId + " and monthid = " + monthId + " and yearid = " + yearId + " and isarrear = 0");
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("salarysheet");
		return array;
	}

	public JSONArray getTakeSalarySheetRecord(String key) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "takesalarysheets");
		JSONArray columnArray = new JSONArray();
		columnArray.put("branchid");
		columnArray.put("monthid");
		columnArray.put("yearid");
		columnArray.put("monthid.name");
		columnArray.put("yearid.name");
		columnArray.put("branchid.name");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, "__key__ = " + key);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray array = object.getJSONArray("takesalarysheets");
		return array;
	}

	public static String getMonthName(Object monthId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_months");
		JSONArray array = new JSONArray();
		array.put("name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + monthId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_months");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			String monthName = rows.getJSONObject(0).optString("name");
			return monthName;
		}
		return null;
	}

	public static String getYearName(Object yearId) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "organization_years");
		JSONArray array = new JSONArray();
		array.put("name");
		query.put(Data.Query.COLUMNS, array);
		query.put(Data.Query.FILTERS, "__key__ = " + yearId);
		JSONObject object;
		object = ResourceEngine.query(query);
		JSONArray rows = object.getJSONArray("organization_years");
		int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
		if (rowCount > 0) {
			String yearName = rows.getJSONObject(0).optString("name");
			return yearName;
		}
		return null;
	}

	/**
	 * Method to Send Mail to HR Yesterday Attendance of Employee
	 * 
	 * @throws ParseException
	 * */
	public void sendYesterdayAttendanceMailToHR() throws ParseException {
		String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
		String backDate = DataTypeUtilities.getBackDate(TODAY_DATE);

		String strDateFormat = "EEEE";
		SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
		String weekDay = sdf.format(Translator.dateValue(TODAY_DATE));
		boolean isTodayFriday = false;
		// if (weekDay.toLowerCase().equalsIgnoreCase("monday")) {
		// isTodayMonday = true;
		// }
		if (weekDay.toLowerCase().equalsIgnoreCase("friday")) {
			isTodayFriday = true;
		}
		// if (weekDay.toLowerCase().indexOf("wednes") != -1) {
		// isTodayMonday = true;
		// }

		try {
			JSONArray mailIdDetailProfitCenterWise = getMailIdProfitCenterWise("");
			JSONArray mailConfigurationSetup = new MorningSchdularHRIS().getMailConfigurationSetup();
			// LogUtility.writeLog("mailConfigurationSetup >> " + mailConfigurationSetup);
			List<Integer> disableDailyAttendanceMailBranchWise = new ArrayList<Integer>();
			JSONObject disableRecords = new JSONObject();
			String logo = "";
			if (mailConfigurationSetup != null && mailConfigurationSetup.length() > 0) {
				Object organizationlogo = mailConfigurationSetup.getJSONObject(0).opt("organizationlogo");
				if (organizationlogo != null) {
					logo = getLogo(organizationlogo);
				}
				Object mailIdArray = mailConfigurationSetup.getJSONObject(0).opt("disable_d_att_m_b_w");
				if (mailIdArray != null && mailIdArray instanceof JSONArray) {
					for (int counterEmail = 0; counterEmail < ((JSONArray) mailIdArray).length(); counterEmail++) {
						disableDailyAttendanceMailBranchWise.add(Translator.integerValue(((JSONArray) mailIdArray).getJSONObject(counterEmail).opt(Updates.KEY)));
					}
				}
			}
			// LogUtility.writeLog("disableDailyAttendanceMailBranchWise >> " + disableDailyAttendanceMailBranchWise);
			disableRecords.put("1", disableDailyAttendanceMailBranchWise);
			HashMap<String, String> reportingHeadMailMap = new HashMap<String, String>();
			HashMap<String, String> indirectReportingHeadMailMap = new HashMap<String, String>();
			HashMap<String, String> employeeMailMap = new HashMap<String, String>();
			HashMap<String, String> employeeWeeklyAttendanceMailToReportingHeadMap = new HashMap<String, String>();
			String[] recordNotfound = { "" };
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
					}
					String filter = "profitcenterid = " + profitCenterId + " and employeestatusid=1";
					JSONArray employeesArray = getEmployeeArray(filter);
					String valueToMail = getValueToMail(employeesArray, backDate, logo, reportingHeadMailMap, employeeMailMap, employeeWeeklyAttendanceMailToReportingHeadMap, isTodayFriday, indirectReportingHeadMailMap, disableRecords, recordNotfound);

					if (emailIds != null && emailIds.length > 0 && valueToMail.length() > 0) {
						valueToMail += "Regards <BR /> Applane Team<br><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
						String[] toMail = new String[1];
						toMail[0] = ApplicationConstants.USER;
						try {
							String title = "Daily Attendance Report For [" + backDate + "]";
							PunchingUtility.sendMailsAccToProfitCenters(toMail, valueToMail, title, emailIds);
						} catch (Exception e) {
							new MorningSchdularHRIS().sendMailInCaseOfException(e, "Mail failed for mailId to hr");
						}
					}
					// else {
					// throw new BusinessLogicException("HR e-mail id's not found");
					// }
				}
				if (employeeMailMap.size() > 0) {
					String header = employeeMailMap.get("header");
					String footer = employeeMailMap.get("footer");
					if (recordNotfound[0].length() > 0) {
						HrisHelper.sendMails(new String[] { "kapil.dalal@daffodilsw.com" }, header + recordNotfound[0] + footer, "Daily Attendance Report For " + backDate + " (Record Not Found)");
					}
					employeeMailMap.remove("header");
					employeeMailMap.remove("footer");
					for (String epmloyeeMailId : employeeMailMap.keySet()) {
						String attendanceValueToMailToEmployee = employeeMailMap.get(epmloyeeMailId);
						attendanceValueToMailToEmployee = header + attendanceValueToMailToEmployee + footer;
						String attendanceValueToMailToEmployeeReportingHead = "";
						if (reportingHeadMailMap.containsKey(epmloyeeMailId)) {
							attendanceValueToMailToEmployeeReportingHead = reportingHeadMailMap.get(epmloyeeMailId);
							attendanceValueToMailToEmployeeReportingHead = "<table border='1' cellspacing='0'><tr bgcolor='99CCCC'><td>Name</td><td>Attendance Type</td><td>In Time</td><td>Out Time</td><td>Total Break</td><td>Total Time In Office</td><td>Status</td></tr>" + attendanceValueToMailToEmployeeReportingHead + footer;
							reportingHeadMailMap.remove(epmloyeeMailId);
						}

						if (attendanceValueToMailToEmployeeReportingHead.length() > 0) {
							attendanceValueToMailToEmployee += "<br /><br /><b>Attendance Of Your Team (Direct)</b><br /><br />";
							attendanceValueToMailToEmployee += attendanceValueToMailToEmployeeReportingHead;
						}

						attendanceValueToMailToEmployeeReportingHead = "";
						if (indirectReportingHeadMailMap.containsKey(epmloyeeMailId)) {
							attendanceValueToMailToEmployeeReportingHead = indirectReportingHeadMailMap.get(epmloyeeMailId);
							attendanceValueToMailToEmployeeReportingHead = "<table border='1' cellspacing='0'><tr bgcolor='99CCCC'><td>Name</td><td>Attendance Type</td><td>In Time</td><td>Out Time</td><td>Total Break</td><td>Total Time In Office</td><td>Status</td></tr>" + attendanceValueToMailToEmployeeReportingHead + footer;
							indirectReportingHeadMailMap.remove(epmloyeeMailId);
						}

						if (attendanceValueToMailToEmployeeReportingHead.length() > 0) {
							attendanceValueToMailToEmployee += "<br /><br /><b>Attendance Of Your Team (In Directly)</b><br /><br />";
							attendanceValueToMailToEmployee += attendanceValueToMailToEmployeeReportingHead;
						}

						String[] mailIds = { epmloyeeMailId };
						try {
							attendanceValueToMailToEmployee += "Regards <BR /> Applane Team<BR /><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
							HrisHelper.sendMails(mailIds, attendanceValueToMailToEmployee, "Daily Attendance Report For " + backDate);
						} catch (Exception e) {
							new MorningSchdularHRIS().sendMailInCaseOfException(e, "Mail failed for employee >> " + epmloyeeMailId);
						}
					}
				}

				if (reportingHeadMailMap.size() > 0) {
					String header = reportingHeadMailMap.get("header");
					String footer = reportingHeadMailMap.get("footer");
					reportingHeadMailMap.remove("header");
					reportingHeadMailMap.remove("footer");
					for (String reportingHeadEpmloyeeMailId : reportingHeadMailMap.keySet()) {
						String attendanceValueToMailToReportingHead = reportingHeadMailMap.get(reportingHeadEpmloyeeMailId);
						attendanceValueToMailToReportingHead = header + attendanceValueToMailToReportingHead + footer;
						String[] mailIds = { reportingHeadEpmloyeeMailId };
						try {
							attendanceValueToMailToReportingHead += "Regards <BR /> Applane Team<BR /><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
							HrisHelper.sendMails(mailIds, attendanceValueToMailToReportingHead, "Daily Attendance Report of Your Team For " + backDate);
						} catch (Exception e) {
							new MorningSchdularHRIS().sendMailInCaseOfException(e, "Mail failed for reportingHeadEpmloyeeMailId >> " + reportingHeadEpmloyeeMailId);
						}
					}
				}
				if (isTodayFriday && employeeWeeklyAttendanceMailToReportingHeadMap.size() > 0) {
					String header = employeeWeeklyAttendanceMailToReportingHeadMap.get("header");
					String footer = employeeWeeklyAttendanceMailToReportingHeadMap.get("footer");
					String title = employeeWeeklyAttendanceMailToReportingHeadMap.get("title");
					employeeWeeklyAttendanceMailToReportingHeadMap.remove("header");
					employeeWeeklyAttendanceMailToReportingHeadMap.remove("footer");
					employeeWeeklyAttendanceMailToReportingHeadMap.remove("title");
					for (String reportingHeadEpmloyeeMailId : employeeWeeklyAttendanceMailToReportingHeadMap.keySet()) {
						String attendanceValueToMailToReportingHead = employeeWeeklyAttendanceMailToReportingHeadMap.get(reportingHeadEpmloyeeMailId);
						attendanceValueToMailToReportingHead = header + attendanceValueToMailToReportingHead + footer;
						String[] mailIds = { reportingHeadEpmloyeeMailId };
						try {
							attendanceValueToMailToReportingHead += "Regards <BR /> Applane Team<BR /><br>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.";
							HrisHelper.sendMails(mailIds, attendanceValueToMailToReportingHead, title);
						} catch (Exception e) {
							new MorningSchdularHRIS().sendMailInCaseOfException(e, "Mail failed for reportingHeadEpmloyeeMailId >> " + reportingHeadEpmloyeeMailId);
						}
					}
				}
			}
		} catch (Exception e) {
			LogUtility.writeLog("NotebookSubmitIsComplete >> getValueToMail >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

	}

	public static JSONArray getMailIdProfitCenterWise(String filter) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "organization_profitcenter");
			JSONArray columnArray = new JSONArray();
			columnArray.put(Updates.KEY);
			JSONArray off = new JSONArray();
			JSONObject offOb = new JSONObject();
			off.put("officialemailid");
			offOb.put("emails", off);
			columnArray.put(offOb);
			columnArray.put("name");
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, filter);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("organization_profitcenter");
			return array;
		} catch (Exception e) {
			throw new BusinessLogicException("Some Error While Getting Hr Records >> " + e);
		}
	}

	public String getLogo(Object emblem) throws JSONException {
		String pathstr = "";
		if (emblem instanceof JSONArray) {
			pathstr = ((JSONArray) emblem).getJSONObject(0).getString("url");
		} else if (emblem instanceof JSONObject) {
			pathstr = ((JSONObject) emblem).getString("url");
		} else {
			String str = "" + emblem;
			String substr = str.substring(str.indexOf("url\":"), str.length());
			substr = substr.substring(6, substr.length());
			pathstr = substr.substring(0, substr.indexOf("\""));
		}
		return pathstr;
	}

	public static JSONArray getHrMailIdDetail(String filter) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_hrassigning");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("employeeid");
			columnArray.put("employeeid.officialemailid");
			columnArray.put("employeeid.name");
			columnArray.put("branchid");
			columnArray.put("branchid.name");
			columnArray.put("branchid.disable_punch_client_email");
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, filter);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_hrassigning");
			return array;
		} catch (Exception e) {
			throw new BusinessLogicException("Some Error While Getting Hr Records >> " + e);
		}
	}

	public static JSONArray getHrMailIdDetailFromBranch(String filter) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_branches");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put(new JSONObject().put("hr_email_ids", new JSONArray().put("officialemailid")));
			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, filter);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_branches");
			return array;
		} catch (Exception e) {
			throw new BusinessLogicException("Some Error While Getting Hr Records >> " + e);
		}
	}

	@SuppressWarnings("unchecked")
	private String getValueToMail(JSONArray employeesArray, String backDate, String logo, HashMap<String, String> reportingHeadMailMap, HashMap<String, String> employeeMailMap, HashMap<String, String> employeeWeeklyAttendanceMailToReportingHeadMap, boolean isTodayMonday, HashMap<String, String> indirectReportingHeadMailMap, JSONObject disableRecords, String[] recordNotfound) {
		try {

			List<Integer> disableDailyAttendanceMailBranchWise = (List<Integer>) disableRecords.opt("1");
			// LogUtility.writeLog("1 >> disableDailyAttendanceMailBranchWise >> " + disableDailyAttendanceMailBranchWise);

			Calendar cal = Calendar.getInstance();
			cal.setTime(Translator.dateValue(backDate));
			cal.add(Calendar.DAY_OF_MONTH, -6);
			String fromDate = EmployeeSalaryGenerationServlet.getDateInString(cal.getTime());
			String valueToMail = "";
			valueToMail += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
			valueToMail += "<hr></hr><br></br>";
			valueToMail += "<table border='1' cellspacing='0'><tr bgcolor='99CCCC'><td>Name</td><td>Attendance Type</td><td>In Time</td><td>Out Time</td><td>Total Break</td><td>Total Time In Office</td><td>Status</td></tr>";

			String headerToMap = "";
			headerToMap += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
			headerToMap += "<hr></hr><br></br>";
			headerToMap += "<table border='1' cellspacing='0'><tr bgcolor='99CCCC'><td>Name</td><td>Attendance Type</td><td>In Time</td><td>Out Time</td><td>Total Break</td><td>Total Time In Office</td><td>Status</td></tr>";

			String headerToWeeklyMap = "";
			String title = "Weekly Report from " + fromDate + " to " + backDate;
			headerToWeeklyMap += "<table><tr><td><img src=\"" + logo + "\" alt=\"Organization Logo\" height='50' width='200'/></td></tr></table>";
			headerToWeeklyMap += "<hr></hr><br></br>";
			headerToWeeklyMap += "<table border='1' cellspacing='0'><tr bgcolor='99CCCC'><td>Name</td><td>Present Days</td><td>Absent Days</td><td>Leaves</td><td>Tours</td><td>EWDs</td><td>Work From Home</td><td>Unknown</td></tr>";

			employeeMailMap.put("header", headerToMap);
			reportingHeadMailMap.put("header", headerToMap);
			employeeWeeklyAttendanceMailToReportingHeadMap.put("header", headerToWeeklyMap);
			employeeWeeklyAttendanceMailToReportingHeadMap.put("title", title);
			boolean recordIsBlank = true;
			// LogUtility.writeLog("employeesArray >> " + employeesArray);
			for (int counter = 0; counter < employeesArray.length(); counter++) {
				int branchId = Translator.integerValue(employeesArray.getJSONObject(counter).opt("branchid"));
				String name = Translator.stringValue(employeesArray.getJSONObject(counter).opt("name"));
				String officialMail = Translator.stringValue(employeesArray.getJSONObject(counter).opt("officialemailid"));
				String reportingHeadOfficialMail = Translator.stringValue(employeesArray.getJSONObject(counter).opt("reportingtoid.officialemailid"));
				String indirectReportingHeadOfficialMail = Translator.stringValue(employeesArray.getJSONObject(counter).opt("indirectreportingto.officialemailid"));
				Object emloyeeId = employeesArray.getJSONObject(counter).opt("__key__");
				String filter = "employeeid = " + emloyeeId + " and attendancedate = '" + backDate + "'";
				JSONArray employeeAttendanceArray = getEmployeeAttendanceArray(filter);
				String valueToMap = "";
				String valueToWeeklyMap = "";
				boolean isOff = true;
				if (employeeAttendanceArray != null && employeeAttendanceArray.length() > 0) {
					String attnDanceType = Translator.stringValue(employeeAttendanceArray.getJSONObject(0).opt("attendancetypeid.name"));
					String inTime = Translator.stringValue(employeeAttendanceArray.getJSONObject(0).opt("firstintime"));
					String outTime = Translator.stringValue(employeeAttendanceArray.getJSONObject(0).opt("lastouttime"));
					String breakTime = Translator.stringValue(employeeAttendanceArray.getJSONObject(0).opt("totalbreaktime"));
					String timeInsideOffice = Translator.stringValue(employeeAttendanceArray.getJSONObject(0).opt("totaltimeinoffice"));
					String status = Translator.stringValue(employeeAttendanceArray.getJSONObject(0).opt("punchingtypeid.name"));
					int attendanceTypeId = Translator.integerValue(employeeAttendanceArray.getJSONObject(0).opt("attendancetypeid"));
					if (attendanceTypeId != HRISApplicationConstants.ATTENDANCE_OFF && attendanceTypeId != HRISApplicationConstants.ATTENDANCE_HOLIDAY) {
						if (recordIsBlank) {
							recordIsBlank = false;
						}
						if (!disableDailyAttendanceMailBranchWise.contains(branchId)) {
							valueToMail += "<tr><td>" + name + "</td><td>&nbsp;" + attnDanceType + "</td><td>&nbsp;" + inTime + "</td><td>&nbsp;" + outTime + "</td><td>&nbsp;" + breakTime + "</td><td>&nbsp;" + timeInsideOffice + "</td><td>&nbsp;" + status + "</td></tr>";
							valueToMap += "<tr><td>" + name + "</td><td>&nbsp;" + attnDanceType + "</td><td>&nbsp;" + inTime + "</td><td>&nbsp;" + outTime + "</td><td>&nbsp;" + breakTime + "</td><td>&nbsp;" + timeInsideOffice + "</td><td>&nbsp;" + status + "</td></tr>";
						}

						isOff = false;
					}
				} else {
					if (recordIsBlank) {
						recordIsBlank = false;
					}
					if (!disableDailyAttendanceMailBranchWise.contains(branchId)) {
						recordNotfound[0] += "<tr><td>&nbsp;" + name + "</td><td colspan='6'>&nbsp;Record not Found</td></tr>";
					}
				}
				if (reportingHeadOfficialMail.length() > 0 && !isOff) {
					String valueToMapForReportingHead = "";
					if (reportingHeadMailMap.containsKey(reportingHeadOfficialMail)) {
						valueToMapForReportingHead += reportingHeadMailMap.get(reportingHeadOfficialMail);
					}
					if (!employeeMailMap.containsKey(officialMail) && !disableDailyAttendanceMailBranchWise.contains(branchId)) {
						reportingHeadMailMap.put(reportingHeadOfficialMail, (valueToMapForReportingHead + valueToMap));
					}
				}

				if (indirectReportingHeadOfficialMail.length() > 0 && !isOff) {
					String valueToMapForReportingHead = "";
					if (indirectReportingHeadMailMap.containsKey(indirectReportingHeadOfficialMail)) {
						valueToMapForReportingHead += indirectReportingHeadMailMap.get(indirectReportingHeadOfficialMail);
					}
					if (!employeeMailMap.containsKey(officialMail) && !disableDailyAttendanceMailBranchWise.contains(branchId)) {
						indirectReportingHeadMailMap.put(indirectReportingHeadOfficialMail, (valueToMapForReportingHead + valueToMap));
					}
				}

				if (isTodayMonday) {
					double presendDays = 0.0;
					double absentDays = 0.0;
					double leaveDays = 0.0;
					double ewdDays = 0.0;
					double tourDays = 0.0;
					double unKnown = 0.0;
					double workFromHome = 0.0;

					filter = "employeeid = " + emloyeeId + " and attendancedate >= '" + fromDate + "' and attendancedate <= '" + backDate + "'";
					employeeAttendanceArray = getEmployeeAttendanceArray(filter);
					for (int innerCounter = 0; innerCounter < employeeAttendanceArray.length(); innerCounter++) {
						int attendanceTypeId = Translator.integerValue(employeeAttendanceArray.getJSONObject(innerCounter).opt("attendancetypeid"));
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_PRESENT) {
							presendDays += 1.0;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_ABSENT) {
							absentDays += 1.0;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_LEAVE) {
							leaveDays += 1.0;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_FIRST_HALFDAY_LEAVE || attendanceTypeId == HRISApplicationConstants.ATTENDANCE_SECOND_HALFDAY_LEAVE) {
							leaveDays += 0.50;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY) {
							ewdDays += 1.0;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_EXTRAWORKINGDAY_HALF) {
							ewdDays += 0.50;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_TOUR) {
							tourDays += 1.0;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_UNKNOWN) {
							unKnown += 1.0;
						}
						if (attendanceTypeId == HRISApplicationConstants.ATTENDANCE_WORK_FROM_HOME) {
							workFromHome += 1.0;
						}
					}
					if (employeeWeeklyAttendanceMailToReportingHeadMap.containsKey(reportingHeadOfficialMail)) {
						valueToWeeklyMap = employeeWeeklyAttendanceMailToReportingHeadMap.get(reportingHeadOfficialMail);
					}

					valueToWeeklyMap += "<tr><td>" + name + "</td><td>&nbsp;" + presendDays + "</td><td>&nbsp;" + absentDays + "</td><td>&nbsp;" + leaveDays + "</td><td>&nbsp;" + tourDays + "</td><td>&nbsp;" + ewdDays + "</td><td>&nbsp;" + workFromHome + "</td><td>&nbsp;" + unKnown + "</td></tr>";
					if (!employeeMailMap.containsKey(officialMail) && !disableDailyAttendanceMailBranchWise.contains(branchId)) {
						employeeWeeklyAttendanceMailToReportingHeadMap.put(reportingHeadOfficialMail, valueToWeeklyMap);
					}

				}
				if (officialMail.length() > 0 && !isOff && !employeeMailMap.containsKey(officialMail) && !disableDailyAttendanceMailBranchWise.contains(branchId)) {
					employeeMailMap.put(officialMail, valueToMap);
				}
			}
			valueToMail += "</table>";
			employeeMailMap.put("footer", "</table>");
			reportingHeadMailMap.put("footer", "</table>");
			employeeWeeklyAttendanceMailToReportingHeadMap.put("footer", "</table>");

			// LogUtility.writeLog("employeeMailMap >> " + employeeMailMap + " << reportingHeadMailMap >> " + reportingHeadMailMap);
			if (recordIsBlank) {
				valueToMail = "";
			}
			return valueToMail;
		} catch (Exception e) {
			LogUtility.writeLog("NotebookSubmitIsComplete >> getValueToMail >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}

	}

	private JSONArray getEmployeeArray(String filter) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "hris_employees");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("name");
			columnArray.put("branchid");
			columnArray.put("officialemailid");
			columnArray.put("reportingtoid.officialemailid");
			columnArray.put("indirectreportingto.officialemailid");

			JSONObject order = new JSONObject();
			order.put(Data.Query.Orders.EXPERSSION, "name");
			order.put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC);
			JSONArray orderByExpression = new JSONArray();
			orderByExpression.put(order);
			query.put(Data.Query.ORDERS, orderByExpression);

			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, filter);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("hris_employees");
			return array;
		} catch (Exception e) {
			LogUtility.writeLog("NotebookSubmitIsComplete >> getValueToMail >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	public JSONArray getEmployeeAttendanceArray(String filter) {
		try {
			JSONObject query = new JSONObject();
			query.put(Data.Query.RESOURCE, "employeeattendance");
			JSONArray columnArray = new JSONArray();
			columnArray.put("__key__");
			columnArray.put("attendancetypeid.name");
			columnArray.put("attendancetypeid");
			columnArray.put("attendancedate");
			columnArray.put("firstintime");
			columnArray.put("lastouttime");
			columnArray.put("totalbreaktime");
			columnArray.put("totaltimeinoffice");
			columnArray.put("paidstatus");
			columnArray.put("shortleaves");
			columnArray.put("punchingtypeid.name");
			columnArray.put("attendance_status");
			columnArray.put("attendance_type_short_leave_id");
			JSONObject employeeObject = new JSONObject();
			JSONArray innerArray = new JSONArray();
			innerArray.put(Updates.KEY);
			innerArray.put("name");
			innerArray.put("employeecode");
			innerArray.put("departmentid.name");
			innerArray.put("reportingtoid.name");
			employeeObject.put("employeeid", innerArray);
			columnArray.put(employeeObject);

			query.put(Data.Query.COLUMNS, columnArray);
			query.put(Data.Query.FILTERS, filter);
			JSONObject object;
			object = ResourceEngine.query(query);
			JSONArray array = object.getJSONArray("employeeattendance");
			return array;
		} catch (Exception e) {
			LogUtility.writeLog("NotebookSubmitIsComplete >> getValueToMail >> Exception  >> : [" + ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e) + "]");
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}
}
