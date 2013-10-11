package com.applane.vender_portal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.moduleimpl.SystemParameters;
import com.applane.service.blob.BlobService;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.ui.browser.shared.constants.ColumnView;
import com.google.apphosting.api.DeadlineExceededException;

public class VendorStatementsJob implements OperationJob {
	// ["","","","","","","requestdate","",""]
	public static void acceptStatementRequest(Object[] selectedKeys) throws JSONException {
		updateStatus(selectedKeys, "Accepted");
	}

	public static void rejectStatementRequest(Object[] selectedKeys) throws JSONException {
		updateStatus(selectedKeys, "Rejected");
	}

	private static void updateStatus(Object[] selectedKeys, String status) throws JSONException {
		JSONArray updates = new JSONArray();
		int length = selectedKeys.length;
		for (int i = 0; i < length; i++) {
			JSONObject update = new JSONObject();
			Object key = selectedKeys[i];
			update.put(Data.Query.KEY, key);
			update.put("status", status);
			updates.put(update);
		}
		if (updates.length() > 0) {
			JSONObject updateQuery = new JSONObject();
			updateQuery.put(Data.Query.RESOURCE, "vendorportal_statements");
			updateQuery.put(Data.Update.UPDATES, updates);
			ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updateQuery);
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			String operationType = record.getOperationType();
			Object vendorId = record.getValue("vendorid");
			Object vendorEmailId = record.getValue("vendorid.orgcontactemailid");
			Object vendorName = record.getValue("vendorid.orgname");
			Object vendorAddress = record.getValue("vendorid.business_address");
			Object supportPersonName = record.getValue("vendorid.supportpersonid.name");
			Object supportPersonEmailId = record.getValue("vendorid.supportpersonid.officialemailid");
			Object status = record.getValue("status");
			Object from = record.getValue("from");
			Object to = record.getValue("to");
			if (operationType.equals(Data.Update.Updates.Types.INSERT)) {
				if (supportPersonEmailId != null) {
					sendMail((String) vendorName, (String) supportPersonEmailId, (String) supportPersonName, from, to, operationType, (String) status, null);
				}
			} else if (operationType.equals(Data.Update.Updates.Types.UPDATE)) {
				if (vendorEmailId != null) {
					String fileKey = null;
					JSONArray documentData = getDocumentData(vendorId, from, to);
					LogUtility.writeLog("documentdata>>" + documentData);
					if (documentData.length() > 0) {
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						createExcel(outputStream, documentData, vendorName, from, to, vendorAddress);
						JSONObject uploadImage = BlobService.uploadImage(outputStream.toByteArray(), "StatementReport.xls");
						fileKey = uploadImage.getString(ColumnView.Column.Types.FileUpload.File.KEY);

						JSONObject updateQuery = new JSONObject();
						updateQuery.put(Data.Query.RESOURCE, "vendorportal_statements");
						updateQuery.put(Data.Update.UPDATES, new JSONObject().put(Data.Query.KEY, record.getKey()).put("statement", new JSONArray().put(uploadImage)));
						updateQuery.put(Data.Update.AVOID_TASKS, true);
						ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updateQuery);

					}
					sendMail((String) supportPersonName, (String) vendorEmailId, (String) vendorName, from, to, operationType, (String) status, fileKey);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	private JSONArray getDocumentData(Object vendorId, Object fromDate, Object toDate) throws JSONException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "vendorportal_documents");
		query.put(Data.Query.FILTERS, "vendorid={vendorId__} AND documentdate>={fromDate__} AND  documentdate<={toDate__}");
		JSONArray columns = new JSONArray();
		columns.put("status.status_type");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.MERGE_COLUMNS, true);
		query.put(Data.Query.PARAMETERS, new JSONObject().put("vendorId__", vendorId).put("fromDate__", fromDate).put("toDate__", toDate));
		return ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("vendorportal_documents");
	}

	private void sendMail(String senderName, String toEmail, String toName, Object from, Object to, String operationType, String status, String fileKey) throws Exception {
		String fromDate = new SimpleDateFormat("dd MMM, yyyy").format(from);
		String toDate = new SimpleDateFormat("dd MMM, yyyy").format(to);
		String subject = "Statement Detail (" + fromDate + "-" + toDate + ")";
		ApplaneMail mail = new ApplaneMail();
		StringBuilder message = new StringBuilder();
		message.append("<div>Hello  " + toName + "</div><br/><div>").append("<b>").append(senderName);
		if (operationType.equals(Data.Update.Updates.Types.INSERT)) {
			message.append("</b> Requested for Statement of Period ").append(fromDate).append(" - ").append(toDate);
			message.append(". Kindly look into this.</div>");
		} else {
			message.append("</b> ").append(status).append(" your Statement Request of Period ").append(fromDate).append(" - ").append(toDate);
			message.append(fileKey != null && fileKey.trim().length() > 0 ? ". Please Find Attached Statement.</div>" : ". No Transaction Found for selected period.</div>");
			if (fileKey != null && fileKey.trim().length() > 0) {
				mail.setAttachments(BlobService.blobToAttachment(fileKey));
			}
			subject = subject + " " + status;
		}
		message.append("<br/><br/>Thanks &amp; Regards <BR /> Applane Team <br/><br/>** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
		mail.setMessage(subject, message.toString(), true);
		mail.setTo(toEmail);
		mail.sendAsAdmin();
	}

	public static void createExcel(ByteArrayOutputStream outputStream, JSONArray documentData, Object vendorName, Object from, Object to, Object vendorAddress) throws JSONException, RowsExceededException, WriteException, IOException, ApplaneMailException {
		WritableWorkbook workbook = Workbook.createWorkbook(outputStream);
		WritableSheet sheet = workbook.createSheet("Statement Report", 0);
		int row = 0;
		int column = 0;
		WritableFont headerfont = new WritableFont(WritableFont.ARIAL);
		headerfont.setBoldStyle(WritableFont.BOLD);
		WritableCellFormat headerFormat = new WritableCellFormat(headerfont);
		headerFormat.setAlignment(Alignment.CENTRE);
		Label label = new Label(column, row, "Vendor Name : ", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "" + vendorName);
		sheet.addCell(label);
		column = 0;
		label = new Label(column, ++row, "Address : ", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "" + ((JSONObject) vendorAddress).getJSONObject("address").getString("address"));
		sheet.addCell(label);
		column = 0;
		label = new Label(column, ++row, "Date : ", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "" + new SimpleDateFormat("dd MMMM, yyyy").format(SystemParameters.getCurrentDate()));
		sheet.addCell(label);
		column = 0;
		label = new Label(column, ++row, "Start Date  : ", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "" + new SimpleDateFormat("dd MMMM, yyyy").format(from));
		sheet.addCell(label);
		column = 0;
		label = new Label(column, ++row, "End Date : ", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "" + new SimpleDateFormat("dd MMMM, yyyy").format(to));
		sheet.addCell(label);
		column = 0;
		label = new Label(++column, ++row, "Serial No.", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "Status", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "Type", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "Document No.", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "Document Date", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "Amount", headerFormat);
		sheet.addCell(label);
		label = new Label(++column, row, "DR/CR", headerFormat);
		sheet.addCell(label);
		row++;
		for (int i = column; i > 1; i--) {
			sheet.setColumnView(i, 25);
		}
		column = 0;
		int dataLength = documentData.length();
		for (int i = 0; i < dataLength; i++) {
			JSONObject info = documentData.getJSONObject(i);
			String status = info.optString("status.status_type", "-");
			String type = info.optString("type", "-");
			String documentNo = info.optString("documentno", "-");
			String documentDate = info.optString("documentdate", "-");
			String amount = info.optJSONObject("amount") == null ? "0" : String.valueOf(info.getJSONObject("amount").opt("amount"));
			String amountType = info.optString("amounttype", "-");
			sheet.setColumnView(column, 25);
			label = new Label(++column, row, i + 1 + "");
			sheet.addCell(label);
			label = new Label(++column, row, status);
			sheet.addCell(label);
			label = new Label(++column, row, type);
			sheet.addCell(label);
			label = new Label(++column, row, documentNo);
			sheet.addCell(label);
			label = new Label(++column, row, documentDate);
			sheet.addCell(label);
			label = new Label(++column, row, amount);
			sheet.addCell(label);
			label = new Label(++column, row, amountType);
			sheet.addCell(label);
			row++;
			column = 0;
		}
		workbook.write();
		workbook.close();
	}

	@Override
	public void onBeforeUpdate(Record arg0) throws VetoException, DeadlineExceededException {
		// TODO Auto-generated method stub

	}

}
