package com.applane.resourceoriented.hris.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
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
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.DataTypeUtilities;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;

public class GrossSalaryLessAndEqual15000 extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		graduativityReport(req, resp);
	}

	private void graduativityReport(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/vnd.ms-excel");
		String decompositionParameter = "attachment;filename=Gross_Salary_Less_And_Equal_15000_Report.xls";
		resp.setHeader("Content-disposition", decompositionParameter);
		resp.setHeader("Content-Type", "application/octet-stream");

		try {
			WritableWorkbook workbook = Workbook.createWorkbook(resp.getOutputStream());
			WritableSheet activeEmployeeSheet = workbook.createSheet("Active Employee", 0);
			// WritableSheet inActiveEmployeeSheet = workbook.createSheet("In-Active Employee", 1);

			WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
			WritableCellFormat hdFormat = new WritableCellFormat(font);
			hdFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
			hdFormat.setAlignment(Alignment.CENTRE);

			WritableCellFormat numbercellformat = new WritableCellFormat(NumberFormats.FLOAT);
			numbercellformat.setFont(font);
			numbercellformat.setBorder(Border.ALL, BorderLineStyle.THIN);
			// String monthName = null;
			//
			// String yearName = null;

			// if (monthId != 0) {
			// monthName = EmployeeReleasingSalaryServlet.getMonthName(monthId);
			// }
			// if (yearId != 0) {
			// yearName = EmployeeReleasingSalaryServlet.getYearName(yearId);
			// }

			String todayDateString = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
			Date monthFirstDate = DataTypeUtilities.getMonthFirstDate(Translator.dateValue(todayDateString));
			String monthFirstDateString = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDate);
			String filter = "ctc <= 15000 AND relievingdate >='" + monthFirstDateString + "'";
			String optionFilter = "ctc <= 15000 AND employeestatusid= " + HRISApplicationConstants.EMPLOYEE_ACTIVE;
			JSONArray employeeArray = gatEmployees(filter, optionFilter);

			// HashMap<Integer, Double> basicAmount = new HashMap<Integer, Double>();
			int row = 5;
			// int inactiveRow = 5;
			int column = 0;
			setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Sr. No.");

			setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Employee Code");
			setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Employee Name");
			setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Joining Date");
			setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "Relieving Date");
			setValueInSheet(activeEmployeeSheet, font, hdFormat, row, column++, "CTC");
			// column = 0;
			// setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Sr. No.");
			// setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Employee Code");
			// setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Employee Name");
			// setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Joining Date");
			// setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Relieving Date");
			// setValueInSheet(inActiveEmployeeSheet, font, hdFormat, inactiveRow, column++, "Basic");
			int srNo = 1;
			// int inActiveSrNo = 1;

			for (int employeeCounter = 0; employeeCounter < employeeArray.length(); employeeCounter++) {
				Object employeeCode = employeeArray.getJSONObject(employeeCounter).opt(HrisKinds.Employess.EMPLOYEE_CODE);
				Object employeeName = employeeArray.getJSONObject(employeeCounter).opt(HrisKinds.Employess.NAME);
				Object joiningDateObject = employeeArray.getJSONObject(employeeCounter).opt("joiningdate");
				Object relievingDateObject = employeeArray.getJSONObject(employeeCounter).opt("relievingdate");
				double ctc = Translator.doubleValue(employeeArray.getJSONObject(employeeCounter).opt("ctc"));
				// Object employeeId = employeeArray.getJSONObject(employeeCounter).opt(Updates.KEY);
				// int employeeStatusId = Translator.integerValue(employeeArray.getJSONObject(employeeCounter).opt("employeestatusid"));
				// Date monthFirstDateTemp = monthFirstDate;
				// Date monthLastDateTemp = monthLastDate;
				// String monthFirstDateStringTemp = monthFirstDateString;
				// String monthLastDateStringTemp = monthLastDateString;
				// long maxDayInMonthTemp = maxDayInMonth;
				// if (relievingDateObject != null) {
				// Date relievingDate = Translator.dateValue(employeeArray.getJSONObject(employeeCounter).opt("relievingdate"));
				// if (relievingDate.before(monthFirstDate)) {
				// Calendar cal = Calendar.getInstance();
				// cal.setTime(relievingDate);
				// int monthIdTemp = cal.get(Calendar.MONTH) + 1;
				// String monthNameTemp = EmployeeReleasingSalaryServlet.getMonthName(monthIdTemp);
				// String yearNameTemp = "" + cal.get(Calendar.YEAR);
				// monthFirstDateTemp = DataTypeUtilities.getMonthFirstDate(yearNameTemp, monthNameTemp);
				// monthLastDateTemp = DataTypeUtilities.getMonthLastDate(monthFirstDateTemp);
				// monthFirstDateStringTemp = EmployeeSalaryGenerationServlet.getDateInString(monthFirstDateTemp);
				// monthLastDateStringTemp = EmployeeSalaryGenerationServlet.getDateInString(monthLastDateTemp);
				// maxDayInMonthTemp = DataTypeUtilities.getNumberOfDaysInMonth(monthFirstDateTemp);
				// }
				// }

				// Calendar cl1 = Calendar.getInstance();
				// Calendar cl2 = Calendar.getInstance();
				column = 0;
				// if (employeeStatusId == HRISApplicationConstants.EMPLOYEE_ACTIVE) {
				row++;
				putHeaderDefault(hdFormat, activeEmployeeSheet, srNo, row, column++);
				putHeaderDefault(hdFormat, activeEmployeeSheet, employeeCode, row, column++);
				putHeaderDefault(hdFormat, activeEmployeeSheet, employeeName, row, column++);
				putHeaderDefault(hdFormat, activeEmployeeSheet, joiningDateObject == null ? "" : joiningDateObject, row, column++);
				putHeaderDefault(hdFormat, activeEmployeeSheet, relievingDateObject == null ? "" : relievingDateObject, row, column++);
				putHeader(numbercellformat, activeEmployeeSheet, ctc, row, column++);
				srNo++;
				// } else {
				// inactiveRow++;
				// putHeaderDefault(hdFormat, inActiveEmployeeSheet, inActiveSrNo, inactiveRow, column++);
				// putHeaderDefault(hdFormat, inActiveEmployeeSheet, employeeCode, inactiveRow, column++);
				// putHeaderDefault(hdFormat, inActiveEmployeeSheet, employeeName, inactiveRow, column++);
				// putHeaderDefault(hdFormat, inActiveEmployeeSheet, joiningDateObject == null ? "" : joiningDateObject, inactiveRow, column++);
				// putHeaderDefault(hdFormat, inActiveEmployeeSheet, relievingDateObject == null ? "" : relievingDateObject, inactiveRow, column++);
				// putHeader(numbercellformat, inActiveEmployeeSheet, calculatedAmount, inactiveRow, column++);
				// inActiveSrNo++;
				// }

			}
			workbook.write();
			workbook.close();
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("GraduativityExcelReportServlet >> exception Report >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured Please Contact To Admin.");
		}
	}

	public void putHeaderDefault(WritableCellFormat cellFormat, WritableSheet sheet, Object value, int row, int column) throws RowsExceededException, WriteException {
		Label label = new Label(column, row, "" + value);
		sheet.addCell(label);
	}

	public void putHeader(WritableCellFormat cellFormat, WritableSheet sheet, double value, int row, int column) throws RowsExceededException, WriteException {
		Number numobj = new Number(column, row, value, cellFormat);
		sheet.addCell(numobj);
	}

	public void setValueInSheet(WritableSheet sheet, WritableFont font, WritableCellFormat hdFormat, int rowNo, int cellNo, String value) throws WriteException, RowsExceededException {
		WritableCellFormat newFormat = new WritableCellFormat(hdFormat);
		Label label;
		newFormat.setFont(font);
		newFormat.setWrap(true);
		label = new Label(cellNo, rowNo, value, newFormat);
		sheet.addCell(label);
	}

	private JSONArray gatEmployees(String filter, String optionFilter) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		JSONArray columnArray = new JSONArray();
		columnArray.put(Updates.KEY);
		columnArray.put(HrisKinds.Employess.EMPLOYEE_CODE);
		columnArray.put(HrisKinds.Employess.NAME);
		columnArray.put("relievingdate");
		columnArray.put("joiningdate");
		columnArray.put("employeestatusid.name");
		columnArray.put("employeestatusid");
		columnArray.put("ctc");
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.OPTION_FILTERS, optionFilter);
		JSONObject object;
		object = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query);
		JSONArray array = object.getJSONArray(HrisKinds.EMPLOYEES);
		return array;
	}
}
