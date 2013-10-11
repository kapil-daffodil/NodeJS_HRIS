package com.applane.resourceoriented.hris.tour.reports;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.shared.constants.Data;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */

// Servlet defined for generating detailed tour report
public class TourReportServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/pdf");
		// keys of selected rows recieved as req paramerer in [1,2,3]
		String tourRequestKey = req.getParameter("keys");
		if (tourRequestKey != null && tourRequestKey.length() > 2) {
			tourRequestKey = tourRequestKey.substring(1, tourRequestKey.length() - 1);

			try {
				ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
				SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");

				String tourDepartDate;
				String tourArriveDate;

				Document document = new Document(PageSize.A4, 10, 10, 2, 5);
				PdfWriter.getInstance(document, resp.getOutputStream());
				document.open();
				Paragraph paragraph = new Paragraph("Travelling Bill", FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, Font.UNDERLINE));
				paragraph.setAlignment(Element.ALIGN_CENTER);
				document.add(paragraph);

				JSONObject mailconfigQuery = new JSONObject();
				mailconfigQuery.put(Data.Query.RESOURCE, "hris_mailconfigurations");
				JSONArray mailConfigColumnArray = new JSONArray();
				mailConfigColumnArray.put("organizationlogo");
				mailConfigColumnArray.put("organizationname");
				mailconfigQuery.put(Data.Query.COLUMNS, mailConfigColumnArray);
				JSONObject logoObject;
				logoObject = ResourceEngine.query(mailconfigQuery);
				JSONArray mailConfigArray = logoObject.getJSONArray("hris_mailconfigurations");
				int mailConfigCount = (mailConfigArray == null || mailConfigArray.length() == 0) ? 0 : mailConfigArray.length();
				// String organizationLogo = null;
				String organizationName = null;

				if (mailConfigCount > 0) {
					// String unknownObject = (String) mailConfigArray.getJSONObject(0).opt("organizationlogo");
					// JSONArray jsonArray = new JSONArray(unknownObject);
					// organizationLogo = (String) jsonArray.getJSONObject(0).opt("url");
					int logoCount = (mailConfigArray == null || mailConfigArray.length() == 0) ? 0 : mailConfigArray.length();
					if (logoCount > 0) {
						organizationName = (String) mailConfigArray.getJSONObject(0).opt("organizationname");
					}
				}

				Paragraph paragraph1 = new Paragraph(organizationName, FontFactory.getFont(FontFactory.TIMES_ROMAN, 20, Font.BOLD));
				paragraph1.setAlignment(Element.ALIGN_CENTER);
				document.add(paragraph1);

				JSONObject tourRequestQuery = new JSONObject();
				tourRequestQuery.put(Data.Query.RESOURCE, "hris_tourrequests");
				JSONArray tourRequestColumnArray = new JSONArray();
				tourRequestColumnArray.put("employeeid.name");
				tourRequestColumnArray.put("employeeid.designationid");
				tourRequestColumnArray.put("employeeid.designationid.name");
				tourRequestColumnArray.put("employeeid.departmentid.name");
				tourRequestColumnArray.put("employeeid.employeecode");
				tourRequestColumnArray.put("approverid.officialemailid");

				JSONObject advancePaidAmountColumnJson = new JSONObject();
				advancePaidAmountColumnJson.put("expression", "advancepaidamount");
				advancePaidAmountColumnJson.put("type", "currency");
				tourRequestColumnArray.put(advancePaidAmountColumnJson);

				JSONObject totalPaidAmountColumnJson = new JSONObject();
				totalPaidAmountColumnJson.put("expression", "totalpaidamount");
				totalPaidAmountColumnJson.put("type", "currency");
				tourRequestColumnArray.put(totalPaidAmountColumnJson);

				tourRequestColumnArray.put("startdate");
				tourRequestColumnArray.put("enddate");
				tourRequestColumnArray.put("tourpurpose");
				tourRequestQuery.put(Data.Query.COLUMNS, tourRequestColumnArray);
				tourRequestQuery.put(Data.Query.FILTERS, "__key__ = " + tourRequestKey);
				JSONObject tourRequestObject;
				JSONArray tourRequestArray = null;
				try {
					tourRequestObject = ResourceEngine.query(tourRequestQuery);
					tourRequestArray = tourRequestObject.getJSONArray("hris_tourrequests");
				} catch (Exception e) {
					LogUtility.writeLog("Error while firing query..." + e);
				}
				int tourRequestArrayCount = tourRequestArray == null ? 0 : tourRequestArray.length();

				if (tourRequestArrayCount > 0) {
					String employeeName = (String) tourRequestArray.getJSONObject(0).opt("employeeid.name") == null ? "" : tourRequestArray.getJSONObject(0).optString("employeeid.name");
					Object endDateObject = tourRequestArray.getJSONObject(0).opt("enddate");
					Object employeeDesignationId = tourRequestArray.getJSONObject(0).opt("employeeid.designationid");
					String employeeDesignationName = (String) tourRequestArray.getJSONObject(0).opt("employeeid.designationid.name") == null ? "" : tourRequestArray.getJSONObject(0).optString("employeeid.designationid.name");
					String employeeDepartmentName = tourRequestArray.getJSONObject(0).opt("employeeid.departmentid.name") == null ? "" : tourRequestArray.getJSONObject(0).optString("employeeid.departmentid.name");
					String employeeCode = (String) tourRequestArray.getJSONObject(0).opt("employeeid.employeecode") == null ? "" : tourRequestArray.getJSONObject(0).optString("employeeid.employeecode");

					Object startDateObject = tourRequestArray.getJSONObject(0).opt("startdate");
					String tourPurpose = (String) tourRequestArray.getJSONObject(0).opt("tourpurpose") == null ? "" : tourRequestArray.getJSONObject(0).optString("tourpurpose");
					String tourAuthorizedBy = (String) tourRequestArray.getJSONObject(0).opt("approverid.officialemailid") == null ? "" : tourRequestArray.getJSONObject(0).optString("approverid.officialemailid");

					Double advancePaidAmount = 0.0;
					String advancePaidAmountType = null;
					JSONObject advancePaidAmountObject = (JSONObject) tourRequestArray.getJSONObject(0).opt("advancepaidamount");
					if (advancePaidAmountObject != null) {
						advancePaidAmount = (Double) advancePaidAmountObject.get("amount");
						JSONObject advancePaidAmountTypeObject = (JSONObject) advancePaidAmountObject.get("type");
						advancePaidAmountType = (String) advancePaidAmountTypeObject.get("currency");
					} else {
						advancePaidAmount = 0.0;
						advancePaidAmountType = "";
					}

					Double totalPaidAmount;
					String totalPaidAmountType = null;
					JSONObject totalPaidAmountObject = (JSONObject) tourRequestArray.getJSONObject(0).opt("totalpaidamount");
					if (totalPaidAmountObject != null) {
						totalPaidAmount = (Double) totalPaidAmountObject.get("amount");
						JSONObject advancePaidAmountTypeObject = (JSONObject) totalPaidAmountObject.get("type");
						totalPaidAmountType = (String) advancePaidAmountTypeObject.get("currency");
					} else {
						totalPaidAmount = 0.0;
						totalPaidAmountType = "";
					}

					Date tourStartDate = null;
					Date tourEndDate = null;

					if (startDateObject != null && endDateObject != null) {
						// parse start date
						try {
							tourStartDate = queryDateFormat.parse("" + startDateObject);
						} catch (ParseException e) {
							try {
								tourStartDate = updateDateFormat.parse("" + startDateObject);
							} catch (ParseException e1) {
								throw new BusinessLogicException("Start date is not parsable" + e.getMessage());
							}
						}

						// parse end date
						try {
							tourEndDate = queryDateFormat.parse("" + endDateObject);
						} catch (ParseException e) {
							try {
								tourEndDate = updateDateFormat.parse("" + endDateObject);
							} catch (ParseException e1) {
								throw new BusinessLogicException("End date is not parsable" + e.getMessage());
							}
						}
					}

					SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
					tourDepartDate = dateFormat.format(tourStartDate);
					tourArriveDate = dateFormat.format(tourEndDate);

					PdfPTable tourTable = new PdfPTable(6);
					PdfPCell tourCell = new PdfPCell();

					Font font = new Font(Font.TIMES_ROMAN, 11, Font.BOLD);
					Font valueFont = new Font(Font.TIMES_ROMAN, 11);

					paragraph = new Paragraph("\n", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(6);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk Employeechunk = new Chunk("Employee Name:");
					Employeechunk.setFont(font);

					Chunk EmployeeNameValueChunk = new Chunk(employeeName);
					EmployeeNameValueChunk.setFont(valueFont);

					paragraph = new Paragraph(Employeechunk + " " + EmployeeNameValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk EmployeeCodechunk = new Chunk("Employee Code:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk EmployeeCodeValueChunk = new Chunk(employeeCode, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));

					paragraph = new Paragraph(EmployeeCodechunk + " " + EmployeeCodeValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk departmentChunk = new Chunk("Department:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk departmentValueChunk = new Chunk(employeeDepartmentName, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					paragraph = new Paragraph(departmentChunk + " " + departmentValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk designationChunk = new Chunk("Designation:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk designationValueChunk = new Chunk(employeeDesignationName, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					paragraph = new Paragraph(designationChunk + " " + designationValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk advanceTakenChunk = new Chunk("Total Approved Amount:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk advanceTakenValueChunk = new Chunk(advancePaidAmount + " " + advancePaidAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					paragraph = new Paragraph(advanceTakenChunk + " " + advanceTakenValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk totalPaidChunk = new Chunk("Total Paid Amount:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk totalPaidValueChunk = new Chunk(totalPaidAmount + " " + totalPaidAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					paragraph = new Paragraph(totalPaidChunk + " " + totalPaidValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk departurechunk = new Chunk("Departure Date:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk departureValueChunk = new Chunk(tourDepartDate, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					paragraph = new Paragraph(departurechunk + " " + departureValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk arrivalChunk = new Chunk("Arrival Date:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk arrivalValueChunk = new Chunk(tourArriveDate, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));

					paragraph = new Paragraph(arrivalChunk + " " + arrivalValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(3);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk approvedBychunk = new Chunk("Approved By:", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD));
					Chunk approvedByvalueChunk = new Chunk(tourAuthorizedBy, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					paragraph = new Paragraph(approvedBychunk + " " + approvedByvalueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(6);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					Chunk purposeChunk = new Chunk("Purpose:", font);
					Chunk purposeValueChunk = new Chunk(tourPurpose, valueFont);
					paragraph = new Paragraph(purposeChunk + " " + purposeValueChunk);
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(6);
					tourCell.setBorder(0);
					tourTable.addCell(tourCell);

					paragraph = new Paragraph("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
					tourCell = new PdfPCell(paragraph);
					tourCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					tourCell.setColspan(6);
					tourCell.setBorder(0);
					tourCell.setFixedHeight(10);
					tourTable.addCell(tourCell);

					tourTable.setWidthPercentage(95);
					document.add(tourTable);

					PdfPTable expenseTable = new PdfPTable(20);
					PdfPCell expenseCell = new PdfPCell();

					paragraph = new Paragraph("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					expenseCell = new PdfPCell(paragraph);
					expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					expenseCell.setColspan(20);
					expenseCell.setBorder(0);
					expenseCell.setFixedHeight(10);
					expenseTable.addCell(expenseCell);

					paragraph = new Paragraph("DETAILS OF EXPENSES INCURRED", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					expenseCell = new PdfPCell(paragraph);
					expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					expenseCell.setColspan(20);
					expenseCell.setBorder(0);
					expenseTable.addCell(expenseCell);

					paragraph = new Paragraph("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					expenseCell = new PdfPCell(paragraph);
					expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					expenseCell.setColspan(20);
					expenseCell.setBorder(0);
					expenseTable.addCell(expenseCell);

					// Retrieve employee graded to find max expense limits
					JSONObject gradeQuery = new JSONObject();
					gradeQuery.put(Data.Query.RESOURCE, "hris_designations__employeegradeid");
					JSONArray gradeColumnArray = new JSONArray();
					gradeColumnArray.put("employeegradeid");
					gradeQuery.put(Data.Query.COLUMNS, gradeColumnArray);
					gradeQuery.put(Data.Query.FILTERS, "hris_designations = " + employeeDesignationId);
					JSONObject gradeObject;
					gradeObject = ResourceEngine.query(gradeQuery);
					JSONArray gradeArray = gradeObject.getJSONArray("hris_designations__employeegradeid");
					int gradeArrayCount = gradeArray == null ? 0 : gradeArray.length();
					if (gradeArrayCount > 0) {
						Object employeeGradeId = gradeArray.getJSONObject(0).opt("employeegradeid");
						JSONObject tourDetailQuery = new JSONObject();
						tourDetailQuery.put(Data.Query.RESOURCE, "hris_tourdetails");
						JSONArray tourDetailColumnArray = new JSONArray();
						tourDetailColumnArray.put("__key__");
						tourDetailColumnArray.put("cityid.name");
						tourDetailColumnArray.put("fromdate");
						tourDetailColumnArray.put("todate");
						tourDetailQuery.put(Data.Query.COLUMNS, tourDetailColumnArray);
						tourDetailQuery.put(Data.Query.FILTERS, "tourrequestid = " + tourRequestKey);
						JSONObject tourDetailObject;
						tourDetailObject = ResourceEngine.query(tourDetailQuery);
						JSONArray tourDetailArray = tourDetailObject.getJSONArray("hris_tourdetails");
						int tourDetailArrayCount = tourDetailArray == null ? 0 : tourDetailArray.length();

						for (int tourDetailCounter = 0; tourDetailCounter < tourDetailArrayCount; tourDetailCounter++) {

							Object tourDetailId = tourDetailArray.getJSONObject(tourDetailCounter).opt("__key__");
							Object cityName = tourDetailArray.getJSONObject(tourDetailCounter).opt("cityid.name");
							Object fromDateObject = tourDetailArray.getJSONObject(tourDetailCounter).opt("fromdate");
							Object toDateObject = tourDetailArray.getJSONObject(tourDetailCounter).opt("todate");

							Date tourDetailFromDate = null;
							Date tourDetailToDate = null;

							if (fromDateObject != null && toDateObject != null) {
								// parse start date
								try {
									tourDetailFromDate = queryDateFormat.parse("" + fromDateObject);
								} catch (ParseException e) {
									try {
										tourDetailFromDate = updateDateFormat.parse("" + fromDateObject);
									} catch (ParseException e1) {
										throw new BusinessLogicException("From date is not parsable" + e.getMessage());
									}
								}

								// parse end date
								try {
									tourDetailToDate = queryDateFormat.parse("" + toDateObject);
								} catch (ParseException e) {
									try {
										tourDetailToDate = updateDateFormat.parse("" + toDateObject);
									} catch (ParseException e1) {
										throw new BusinessLogicException("To date is not parsable" + e.getMessage());
									}
								}
							}

							String fromDate = dateFormat.format(tourDetailFromDate);
							String toDate = dateFormat.format(tourDetailToDate);
							paragraph = new Paragraph("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
							expenseCell = new PdfPCell(paragraph);
							expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
							expenseCell.setColspan(20);
							expenseCell.setBorder(0);
							expenseCell.setFixedHeight(15);
							expenseTable.addCell(expenseCell);

							paragraph = new Paragraph("City- " + cityName + " (" + fromDate + " To " + toDate + ")", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
							expenseCell = new PdfPCell(paragraph);
							expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
							expenseCell.setColspan(20);
							expenseCell.setBorder(0);
							expenseTable.addCell(expenseCell);

							paragraph = new Paragraph("Particulars", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
							expenseCell = new PdfPCell(paragraph);
							expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
							expenseCell.setColspan(4);
							expenseTable.addCell(expenseCell);

							paragraph = new Paragraph("Max. Allowed", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
							expenseCell = new PdfPCell(paragraph);
							expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
							expenseCell.setColspan(4);
							expenseTable.addCell(expenseCell);

							paragraph = new Paragraph("Expense Amount", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
							expenseCell = new PdfPCell(paragraph);
							expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
							expenseCell.setColspan(4);
							expenseTable.addCell(expenseCell);

							paragraph = new Paragraph("Approved Amount", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
							expenseCell = new PdfPCell(paragraph);
							expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
							expenseCell.setColspan(4);
							expenseTable.addCell(expenseCell);

							paragraph = new Paragraph("Paid Amount", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
							expenseCell = new PdfPCell(paragraph);
							expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
							expenseCell.setColspan(4);
							expenseTable.addCell(expenseCell);

							// 1. fire query on tourexpenses where tourdetail id is above
							JSONObject tourExpenseQuery = new JSONObject();
							tourExpenseQuery.put(Data.Query.RESOURCE, "hris_tourexpenses");
							JSONArray tourExpensesColumnArray = new JSONArray();
							tourExpensesColumnArray.put("__key__");
							tourExpensesColumnArray.put("tourparticularid");
							tourExpensesColumnArray.put("tourparticularid.name");
							tourExpensesColumnArray.put("employeeid");
							tourExpensesColumnArray.put("employeeid.designationid");
							JSONObject expenseAmountColumnJson = new JSONObject();
							expenseAmountColumnJson.put("expression", "expenseamount");
							expenseAmountColumnJson.put("type", "currency");
							tourExpensesColumnArray.put(expenseAmountColumnJson);
							JSONObject approvedAmountColumnJson = new JSONObject();
							approvedAmountColumnJson.put("expression", "approveamount");
							approvedAmountColumnJson.put("type", "currency");
							tourExpensesColumnArray.put(approvedAmountColumnJson);
							JSONObject paidAmountColumnJson = new JSONObject();
							paidAmountColumnJson.put("expression", "paidamount");
							paidAmountColumnJson.put("type", "currency");
							tourExpensesColumnArray.put(paidAmountColumnJson);
							tourExpensesColumnArray.put("expensedate");
							tourExpenseQuery.put(Data.Query.COLUMNS, tourExpensesColumnArray);
							tourExpenseQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
							JSONObject tourExpenseObject;
							tourExpenseObject = ResourceEngine.query(tourExpenseQuery);
							JSONArray tourExpenseArray = tourExpenseObject.getJSONArray("hris_tourexpenses");
							int tourExpenseArrayCount = tourExpenseArray == null ? 0 : tourExpenseArray.length();

							for (int tourExpenseCounter = 0; tourExpenseCounter < tourExpenseArrayCount; tourExpenseCounter++) {

								// Object tourExpenseId = tourExpenseArray.getJSONObject(tourExpenseCounter).opt("__key__");
								Object tourParticularId = tourExpenseArray.getJSONObject(tourExpenseCounter).opt("tourparticularid");
								String tourParticularName = (String) tourExpenseArray.getJSONObject(tourExpenseCounter).opt("tourparticularid.name");

								// Object tourExpenseDateObject = tourExpenseArray.getJSONObject(tourExpenseCounter).opt("expensedate");

								Double tourExpenseAmount = 0.0;
								String tourExpenseAmountType = "";
								JSONObject expenseAmountObject = (JSONObject) tourExpenseArray.getJSONObject(tourExpenseCounter).opt("expenseamount");
								if (expenseAmountObject != null) {
									tourExpenseAmount = (Double) expenseAmountObject.get("amount");
									JSONObject expenseAmountTypeObject = (JSONObject) expenseAmountObject.get("type");
									tourExpenseAmountType = (String) expenseAmountTypeObject.get("currency");
								} else {
									tourExpenseAmount = 0.0;
									tourExpenseAmountType = "";
								}

								Double tourExpenseApproveAmount = 0.0;
								String tourExpenseApproveAmountType = "";
								JSONObject approveAmountObject = (JSONObject) tourExpenseArray.getJSONObject(tourExpenseCounter).opt("approveamount");
								if (approveAmountObject != null) {
									tourExpenseApproveAmount = (Double) approveAmountObject.get("amount");
									JSONObject approveAmountTypeObject = (JSONObject) approveAmountObject.get("type");
									tourExpenseApproveAmountType = (String) approveAmountTypeObject.get("currency");
								} else {
									tourExpenseApproveAmount = 0.0;
									tourExpenseApproveAmountType = "";
								}

								Double tourExpensePaidAmount = 0.0;
								String tourExpensePaidAmountType = "";
								JSONObject paidAmountObject = (JSONObject) tourExpenseArray.getJSONObject(tourExpenseCounter).opt("paidamount");
								if (paidAmountObject != null) {
									tourExpensePaidAmount = (Double) ((Double) paidAmountObject.get("amount") == null ? " " : paidAmountObject.get("amount"));
									JSONObject paidAmountTypeObject = (JSONObject) paidAmountObject.get("type");
									tourExpensePaidAmountType = (String) ((String) paidAmountTypeObject.get("currency") == null ? "" : paidAmountTypeObject.get("currency"));
								} else {
									tourExpensePaidAmount = 0.0;
									tourExpensePaidAmountType = "";
								}
								int font1 = 11;
								paragraph = new Paragraph(tourParticularName, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1, Font.BOLD));
								expenseCell = new PdfPCell(paragraph);
								expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
								expenseCell.setColspan(4);
								expenseCell.setFixedHeight(20);
								expenseTable.addCell(expenseCell);

								// LogUtility.writeLog("particularMaxAmount " + particularMaxAmount);
								paragraph = new Paragraph("" + " " + "", FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
								expenseCell = new PdfPCell(paragraph);
								expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
								expenseCell.setColspan(4);
								expenseTable.addCell(expenseCell);

								paragraph = new Paragraph(tourExpenseAmount + " " + tourExpenseAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
								expenseCell = new PdfPCell(paragraph);
								expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
								expenseCell.setColspan(4);
								expenseTable.addCell(expenseCell);

								paragraph = new Paragraph(tourExpenseApproveAmount + " " + tourExpenseApproveAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
								expenseCell = new PdfPCell(paragraph);
								expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
								expenseCell.setColspan(4);
								expenseTable.addCell(expenseCell);

								paragraph = new Paragraph(tourExpensePaidAmount + " " + tourExpensePaidAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
								expenseCell = new PdfPCell(paragraph);
								expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
								expenseCell.setColspan(4);
								expenseTable.addCell(expenseCell);

								if (employeeGradeId != null) {
									// get expenselimitid from hris_expenselimits__employeegradeid where employeegradeid is above
									JSONObject expenseLimitQuery = new JSONObject();
									expenseLimitQuery.put(Data.Query.RESOURCE, "hris_expenselimits__employeegradeid");
									JSONArray expenseLimitColumnArray = new JSONArray();
									expenseLimitColumnArray.put("hris_expenselimits");
									expenseLimitQuery.put(Data.Query.COLUMNS, expenseLimitColumnArray);
									expenseLimitQuery.put(Data.Query.FILTERS, "employeegradeid= " + employeeGradeId);
									JSONObject expenseLimitObject;
									expenseLimitObject = ResourceEngine.query(expenseLimitQuery);
									JSONArray expenseLimitArray = expenseLimitObject.getJSONArray("hris_expenselimits__employeegradeid");

									int expenseLimitArrayCount = expenseLimitArray == null ? 0 : expenseLimitArray.length();
									if (expenseLimitArrayCount > 0) {
										for (int counter = 0; counter < expenseLimitArrayCount; counter++) {
											Object expenseLimitId = expenseLimitArray.getJSONObject(counter).opt("hris_expenselimits");

											JSONObject expenseQuery = new JSONObject();
											expenseQuery.put(Data.Query.RESOURCE, "hris_expenselimits");
											JSONArray expenseColumnArray = new JSONArray();
											// expenseColumnArray.put("tourparticularid.name");
											JSONObject amountColumnJson = new JSONObject();
											amountColumnJson.put("expression", "amount");
											amountColumnJson.put("type", "currency");
											expenseColumnArray.put(amountColumnJson);
											expenseQuery.put(Data.Query.COLUMNS, expenseColumnArray);
											expenseQuery.put(Data.Query.FILTERS, "__key__ = " + expenseLimitId + " and tourparticularid = " + tourParticularId);
											JSONObject particularObject;
											particularObject = ResourceEngine.query(expenseQuery);
											JSONArray particularArray = particularObject.getJSONArray("hris_expenselimits");
											int particularRowCount = particularArray == null ? 0 : particularArray.length();
											if (particularRowCount > 0) {
												// Double particularMaxAmount = 0.0;
												// String particularMaxAmountType = "";
												JSONObject particularMaxAmountObject = (JSONObject) particularArray.getJSONObject(0).opt("amount");
												if (particularMaxAmountObject != null) {
													// particularMaxAmount = (Double) particularMaxAmountObject.get("amount");
													// JSONObject particularMaxAmountTypeObject = (JSONObject) particularMaxAmountObject.get("type");
													// particularMaxAmountType = (String) particularMaxAmountTypeObject.get("currency");
												} else {
													// particularMaxAmount = 0.0;
													// particularMaxAmountType = "";
												}
												/*
												 * String tourParticularName = (String) particularArray.getJSONObject(0).opt("tourparticularid.name");
												 */
											}
										}
									}
								}
							}

							// 2. fire query on hris_travelexpenses where tourdetail id is above
							JSONObject travelExpenseQuery = new JSONObject();
							travelExpenseQuery.put(Data.Query.RESOURCE, "hris_travelexpenses");
							JSONArray travelExpensesColumnArray = new JSONArray();
							travelExpensesColumnArray.put("__key__");
							travelExpensesColumnArray.put("travelmodeid");
							JSONObject travelAmountColumnJson = new JSONObject();
							travelAmountColumnJson.put("expression", "travelamount");
							travelAmountColumnJson.put("type", "currency");
							travelExpensesColumnArray.put(travelAmountColumnJson);
							JSONObject travelExpenseApprovedAmountColumn = new JSONObject();
							travelExpenseApprovedAmountColumn.put("expression", "approveamount");
							travelExpenseApprovedAmountColumn.put("type", "currency");
							travelExpensesColumnArray.put(travelExpenseApprovedAmountColumn);
							JSONObject travelPaidAmountColumnJson = new JSONObject();
							travelPaidAmountColumnJson.put("expression", "paidamount");
							travelPaidAmountColumnJson.put("type", "currency");
							travelExpensesColumnArray.put(travelPaidAmountColumnJson);
							travelExpensesColumnArray.put("journydate");
							travelExpenseQuery.put(Data.Query.COLUMNS, travelExpensesColumnArray);
							travelExpenseQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
							JSONObject travelExpenseObject;
							travelExpenseObject = ResourceEngine.query(travelExpenseQuery);
							JSONArray travelExpenseArray = travelExpenseObject.getJSONArray("hris_travelexpenses");

							int travelExpenseArrayCount = travelExpenseArray == null ? 0 : travelExpenseArray.length();
							for (int travelExpenseCounter = 0; travelExpenseCounter < travelExpenseArrayCount; travelExpenseCounter++) {

								// Object travelExpenseId = travelExpenseArray.getJSONObject(travelExpenseCounter).opt("__key__");
								Object travelModeId = travelExpenseArray.getJSONObject(travelExpenseCounter).opt("travelmodeid");
								// Object journyDateObject = travelExpenseArray.getJSONObject(travelExpenseCounter).opt("journydate");
								Double travelAmount = 0.0;
								String travelAmountType = "";
								JSONObject travelAmountObject = (JSONObject) travelExpenseArray.getJSONObject(travelExpenseCounter).opt("travelamount");
								if (travelAmountObject != null) {
									travelAmount = (Double) travelAmountObject.get("amount");
									JSONObject travelAmountTypeObject = (JSONObject) travelAmountObject.get("type");
									travelAmountType = (String) travelAmountTypeObject.get("currency");
								} else {
									travelAmount = 0.0;
									travelAmountType = "";
								}

								Double approveTravelAmount = 0.0;
								String approveTravelAmountType = "";
								JSONObject approveTravelAmountObject = (JSONObject) travelExpenseArray.getJSONObject(travelExpenseCounter).opt("approveamount");
								if (approveTravelAmountObject != null) {
									approveTravelAmount = (Double) approveTravelAmountObject.get("amount");
									JSONObject approveTravelAmountTypeObject = (JSONObject) approveTravelAmountObject.get("type");
									approveTravelAmountType = (String) approveTravelAmountTypeObject.get("currency");
								} else {
									approveTravelAmount = 0.0;
									approveTravelAmountType = "";
								}

								Double paidTravelAmount = 0.0;
								String paidTravelAmountType = "";
								JSONObject travelPaidAmountObject = (JSONObject) travelExpenseArray.getJSONObject(travelExpenseCounter).opt("paidamount");
								if (travelPaidAmountObject != null) {
									paidTravelAmount = (Double) travelPaidAmountObject.get("amount");
									JSONObject paidTravelAmountTypeObject = (JSONObject) travelPaidAmountObject.get("type");
									paidTravelAmountType = (String) paidTravelAmountTypeObject.get("currency");
								} else {
									paidTravelAmount = 0.0;
									paidTravelAmountType = "";
								}

								if (employeeGradeId != null) {
									// get TRAVELexpenselimitid from hris_travelexpenselimits__employeegradeid where employeegradeid is above
									JSONObject travelExpenseLimitQuery = new JSONObject();
									travelExpenseLimitQuery.put(Data.Query.RESOURCE, "hris_travelexpenselimits__employeegradeid");
									JSONArray expenseLimitColumnArray = new JSONArray();
									expenseLimitColumnArray.put("hris_travelexpenselimits");
									travelExpenseLimitQuery.put(Data.Query.COLUMNS, expenseLimitColumnArray);
									travelExpenseLimitQuery.put(Data.Query.FILTERS, "employeegradeid= " + employeeGradeId);
									JSONObject expenseLimitObject;
									expenseLimitObject = ResourceEngine.query(travelExpenseLimitQuery);
									JSONArray travelExpenseLimitArray = expenseLimitObject.getJSONArray("hris_travelexpenselimits__employeegradeid");
									int travelExpenseLimitArrayCount = travelExpenseLimitArray == null ? 0 : travelExpenseLimitArray.length();
									if (travelExpenseLimitArrayCount > 0) {
										for (int counter = 0; counter < travelExpenseLimitArrayCount; counter++) {
											Object travelExpenseLimitId = travelExpenseLimitArray.getJSONObject(counter).opt("hris_travelexpenselimits");
											JSONObject expenseQuery = new JSONObject();
											expenseQuery.put(Data.Query.RESOURCE, "hris_travelexpenselimits");
											JSONArray expenseColumnArray = new JSONArray();
											expenseColumnArray.put("__key__");
											expenseColumnArray.put("travelmodeid.name");
											JSONObject amountColumnJson = new JSONObject();
											amountColumnJson.put("expression", "amount");
											amountColumnJson.put("type", "currency");
											expenseColumnArray.put(amountColumnJson);
											expenseQuery.put(Data.Query.COLUMNS, expenseColumnArray);
											expenseQuery.put(Data.Query.FILTERS, "__key__= " + travelExpenseLimitId + " and travelmodeid = " + travelModeId);
											JSONObject travelModeObject;
											travelModeObject = ResourceEngine.query(expenseQuery);
											JSONArray travelModeArray = travelModeObject.getJSONArray("hris_travelexpenselimits");

											int travelmodeCount = travelModeArray == null ? 0 : travelModeArray.length();
											if (travelmodeCount > 0) {
												JSONObject travelMaxAmountObject = (JSONObject) travelModeArray.getJSONObject(0).opt("amount");
												Double travelMaxAmount = (Double) travelMaxAmountObject.get("amount");
												JSONObject travelMaxAmountTypeObject = (JSONObject) travelMaxAmountObject.get("type");
												String travelMaxAmountType = (String) travelMaxAmountTypeObject.get("currency");
												String travelModeName = (String) travelModeArray.getJSONObject(0).opt("travelmodeid.name");

												int font1 = 11;
												paragraph = new Paragraph(travelModeName, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1, Font.BOLD));
												expenseCell = new PdfPCell(paragraph);
												expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
												expenseCell.setColspan(4);
												expenseCell.setFixedHeight(20);
												expenseTable.addCell(expenseCell);

												paragraph = new Paragraph(travelMaxAmount + " " + travelMaxAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
												expenseCell = new PdfPCell(paragraph);
												expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
												expenseCell.setColspan(4);
												expenseTable.addCell(expenseCell);

												paragraph = new Paragraph(travelAmount + " " + travelAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
												expenseCell = new PdfPCell(paragraph);
												expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
												expenseCell.setColspan(4);
												expenseTable.addCell(expenseCell);

												paragraph = new Paragraph(approveTravelAmount + " " + approveTravelAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
												expenseCell = new PdfPCell(paragraph);
												expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
												expenseCell.setColspan(4);
												expenseTable.addCell(expenseCell);

												paragraph = new Paragraph(paidTravelAmount + " " + paidTravelAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
												expenseCell = new PdfPCell(paragraph);
												expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
												expenseCell.setColspan(4);
												expenseTable.addCell(expenseCell);
											}
										}
									}
								}
							}
							// 3. fire query on hris_localconveyance where tourdetailid is above

							JSONObject localConveyanceExpenseQuery = new JSONObject();
							localConveyanceExpenseQuery.put(Data.Query.RESOURCE, "hris_localconveyance");
							JSONArray localExpensesColumnArray = new JSONArray();
							localExpensesColumnArray.put("__key__");
							localExpensesColumnArray.put("name");

							JSONObject localExpenseAmountColumnJson = new JSONObject();
							localExpenseAmountColumnJson.put("expression", "expenseamount");
							localExpenseAmountColumnJson.put("type", "currency");
							localExpensesColumnArray.put(localExpenseAmountColumnJson);

							JSONObject localExpenseApprovedAmountColumn = new JSONObject();
							localExpenseApprovedAmountColumn.put("expression", "approveamount");
							localExpenseApprovedAmountColumn.put("type", "currency");
							localExpensesColumnArray.put(localExpenseApprovedAmountColumn);

							JSONObject localPaidAmountColumnJson = new JSONObject();
							localPaidAmountColumnJson.put("expression", "paidamount");
							localPaidAmountColumnJson.put("type", "currency");
							localExpensesColumnArray.put(localPaidAmountColumnJson);

							localExpensesColumnArray.put("expensedate");
							localConveyanceExpenseQuery.put(Data.Query.COLUMNS, localExpensesColumnArray);
							localConveyanceExpenseQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
							JSONObject localExpenseObject;
							localExpenseObject = ResourceEngine.query(localConveyanceExpenseQuery);
							JSONArray localExpenseArray = localExpenseObject.getJSONArray("hris_localconveyance");

							int localExpenseArrayCount = localExpenseArray == null ? 0 : localExpenseArray.length();
							if (localExpenseArrayCount > 0) {
								for (int localExpenseCounter = 0; localExpenseCounter < localExpenseArrayCount; localExpenseCounter++) {

									// Object localExpenseId = localExpenseArray.getJSONObject(localExpenseCounter).opt("__key__");
									String localExpenseName = (String) localExpenseArray.getJSONObject(localExpenseCounter).opt("name");
									// Object localExpenseDate = localExpenseArray.getJSONObject(localExpenseCounter).opt("expensedate");
									Double localExpenseAmount = 0.0;
									String localExpenseAmountType = "";
									JSONObject localExpenseAmountObject = (JSONObject) localExpenseArray.getJSONObject(localExpenseCounter).opt("expenseamount");
									if (localExpenseAmountObject != null) {
										localExpenseAmount = (Double) localExpenseAmountObject.get("amount");
										JSONObject localAmountTypeObject = (JSONObject) localExpenseAmountObject.get("type");
										localExpenseAmountType = (String) localAmountTypeObject.get("currency");
									} else {
										localExpenseAmount = 0.0;
										localExpenseAmountType = "";
									}

									Double approveLocalAmount = 0.0;
									String approveLocalAmountType = "";
									JSONObject approveLocalAmountObject = (JSONObject) localExpenseArray.getJSONObject(localExpenseCounter).opt("approveamount");
									if (approveLocalAmountObject != null) {
										approveLocalAmount = (Double) approveLocalAmountObject.get("amount");
										JSONObject approveLocalAmountTypeObject = (JSONObject) approveLocalAmountObject.get("type");
										approveLocalAmountType = (String) approveLocalAmountTypeObject.get("currency");
									} else {
										approveLocalAmount = 0.0;
										approveLocalAmountType = "";
									}

									Double paidLocalAmount = 0.0;
									String paidLocalAmountType = "";
									JSONObject paidLocalAmountObject = (JSONObject) localExpenseArray.getJSONObject(localExpenseCounter).opt("paidamount");
									if (paidLocalAmountObject != null) {
										paidLocalAmount = (Double) paidLocalAmountObject.get("amount");
										JSONObject paidLocalAmountTypeObject = (JSONObject) paidLocalAmountObject.get("type");
										paidLocalAmountType = (String) paidLocalAmountTypeObject.get("currency");
									} else {
										paidLocalAmount = 0.0;
										paidLocalAmountType = "";
									}

									int font1 = 11;
									paragraph = new Paragraph(localExpenseName, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1, Font.BOLD));
									expenseCell = new PdfPCell(paragraph);
									expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
									expenseCell.setColspan(4);
									expenseCell.setFixedHeight(20);
									expenseTable.addCell(expenseCell);

									paragraph = new Paragraph("N/A", FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
									expenseCell = new PdfPCell(paragraph);
									expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
									expenseCell.setColspan(4);
									expenseTable.addCell(expenseCell);

									paragraph = new Paragraph(localExpenseAmount + " " + localExpenseAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
									expenseCell = new PdfPCell(paragraph);
									expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
									expenseCell.setColspan(4);
									expenseTable.addCell(expenseCell);

									paragraph = new Paragraph(approveLocalAmount + " " + approveLocalAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
									expenseCell = new PdfPCell(paragraph);
									expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
									expenseCell.setColspan(4);
									expenseTable.addCell(expenseCell);

									paragraph = new Paragraph(paidLocalAmount + " " + paidLocalAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
									expenseCell = new PdfPCell(paragraph);
									expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
									expenseCell.setColspan(4);
									expenseTable.addCell(expenseCell);

								}

								// 4. FIRE QUERY ON hris_otherexpenses where tour detail id is above
								JSONObject otherExpenseQuery = new JSONObject();
								otherExpenseQuery.put(Data.Query.RESOURCE, "hris_otherexpenses");
								JSONArray otherExpensesColumnArray = new JSONArray();
								otherExpensesColumnArray.put("__key__");
								otherExpensesColumnArray.put("name");

								JSONObject otherExpenseAmountColumnJson = new JSONObject();
								otherExpenseAmountColumnJson.put("expression", "expenseamount");
								otherExpenseAmountColumnJson.put("type", "currency");
								otherExpensesColumnArray.put(otherExpenseAmountColumnJson);

								JSONObject otherExpenseApprovedAmountColumn = new JSONObject();
								otherExpenseApprovedAmountColumn.put("expression", "approveamount");
								otherExpenseApprovedAmountColumn.put("type", "currency");
								otherExpensesColumnArray.put(otherExpenseApprovedAmountColumn);

								JSONObject otherPaidAmountColumnJson = new JSONObject();
								otherPaidAmountColumnJson.put("expression", "paidamount");
								otherPaidAmountColumnJson.put("type", "currency");
								otherExpensesColumnArray.put(otherPaidAmountColumnJson);

								otherExpensesColumnArray.put("expensedate");
								otherExpenseQuery.put(Data.Query.COLUMNS, otherExpensesColumnArray);
								otherExpenseQuery.put(Data.Query.FILTERS, "tourdetailid = " + tourDetailId + " and approved = 1");
								JSONObject otherExpenseObject;
								otherExpenseObject = ResourceEngine.query(otherExpenseQuery);
								JSONArray otherExpenseArray = otherExpenseObject.getJSONArray("hris_otherexpenses");

								int otherExpenseArrayCount = otherExpenseArray == null ? 0 : otherExpenseArray.length();

								if (otherExpenseArrayCount > 0) {
									for (int otherExpenseCounter = 0; otherExpenseCounter < otherExpenseArrayCount; otherExpenseCounter++) {

										// Object otherExpenseId = otherExpenseArray.getJSONObject(otherExpenseCounter).opt("__key__");
										String otherExpenseName = (String) otherExpenseArray.getJSONObject(otherExpenseCounter).opt("name");
										// Object otherExpenseDate = otherExpenseArray.getJSONObject(otherExpenseCounter).opt("expensedate");
										JSONObject otherExpenseAmountObject = (JSONObject) otherExpenseArray.getJSONObject(otherExpenseCounter).opt("expenseamount");
										Double otherExpenseAmount = (Double) otherExpenseAmountObject.get("amount");
										JSONObject otherAmountTypeObject = (JSONObject) otherExpenseAmountObject.get("type");
										String otherExpenseAmountType = (String) otherAmountTypeObject.get("currency");

										JSONObject approveOtherAmountObject = (JSONObject) otherExpenseArray.getJSONObject(otherExpenseCounter).opt("approveamount");
										Double approveOtherAmount = (Double) approveOtherAmountObject.get("amount");
										JSONObject approveOtherAmountTypeObject = (JSONObject) approveOtherAmountObject.get("type");
										String approveOtherAmountType = (String) approveOtherAmountTypeObject.get("currency");

										JSONObject paidOtherAmountObject = (JSONObject) otherExpenseArray.getJSONObject(otherExpenseCounter).opt("paidamount");
										Double paidOtherAmount = (Double) paidOtherAmountObject.get("amount");
										JSONObject paidOtherAmountTypeObject = (JSONObject) paidOtherAmountObject.get("type");
										String paidOtherAmountType = (String) paidOtherAmountTypeObject.get("currency");

										int font1 = 11;
										paragraph = new Paragraph(otherExpenseName, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1, Font.BOLD));
										expenseCell = new PdfPCell(paragraph);
										expenseCell.setHorizontalAlignment(Element.ALIGN_LEFT);
										expenseCell.setColspan(4);
										expenseCell.setFixedHeight(20);
										expenseTable.addCell(expenseCell);

										paragraph = new Paragraph("N/A", FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
										expenseCell = new PdfPCell(paragraph);
										expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
										expenseCell.setColspan(4);
										expenseTable.addCell(expenseCell);

										paragraph = new Paragraph(otherExpenseAmount + " " + otherExpenseAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
										expenseCell = new PdfPCell(paragraph);
										expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
										expenseCell.setColspan(4);
										expenseTable.addCell(expenseCell);

										paragraph = new Paragraph(approveOtherAmount + " " + approveOtherAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
										expenseCell = new PdfPCell(paragraph);
										expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
										expenseCell.setColspan(4);
										expenseTable.addCell(expenseCell);

										paragraph = new Paragraph(paidOtherAmount + " " + paidOtherAmountType, FontFactory.getFont(FontFactory.TIMES_ROMAN, font1));
										expenseCell = new PdfPCell(paragraph);
										expenseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
										expenseCell.setColspan(4);
										expenseTable.addCell(expenseCell);

									}
								}
							}
						}
					}
					expenseTable.setWidthPercentage(95);
					document.add(expenseTable);

					PdfPTable briefTable = new PdfPTable(6);
					PdfPCell briefCell = new PdfPCell();

					paragraph = new Paragraph("\n" + "BRIEF TOUR REPORT", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.UNDERLINE));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_CENTER);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("___________________________________________________________________________________________", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("___________________________________________________________________________________________", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("___________________________________________________________________________________________", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("___________________________________________________________________________________________", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("___________________________________________________________________________________________", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("Signature of Approving", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					paragraph = new Paragraph("Authority", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12));
					briefCell = new PdfPCell(paragraph);
					briefCell.setHorizontalAlignment(Element.ALIGN_LEFT);
					briefCell.setColspan(6);
					briefCell.setBorder(0);
					briefCell.setFixedHeight(20);
					briefTable.addCell(briefCell);

					briefTable.setWidthPercentage(95);
					document.add(briefTable);

				}
				document.close();
			} catch (Exception e) {
				throw new RuntimeException("Error Come while Populate Tour Expense Report." + e.getMessage());
			}
		}
	}
}