package com.applane.resourceoriented.hris.utility;

import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.applane.user.service.UserUtility;

public class HrisHelper {
	public static void setHeaderValue(int column, int rowNo, String value, WritableCellFormat cellFormat, WritableSheet sheet) throws RowsExceededException, WriteException {
		Label label = new Label(column, rowNo, value, cellFormat);
		sheet.addCell(label);

	}

	public static void sendMails(String[] toMailIds, String mailValue, String title) {
		try {
			for (int counter = 0; counter < toMailIds.length; counter++) {
				ApplaneMail mailSender = new ApplaneMail();
				mailSender.setMessage(title, mailValue.toString(), true);
				mailSender.setTo(toMailIds[counter]);
				mailSender.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
			}
		} catch (ApplaneMailException e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage("HrisHelper", e));
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage("HrisHelper", e));
		}
	}
}
