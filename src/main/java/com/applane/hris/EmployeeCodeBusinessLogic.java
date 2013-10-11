package com.applane.hris;

import java.io.ByteArrayOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.app.service.ViewService;
import com.applane.databaseengine.ApplaneDatabaseEngine;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.service.exportview.ExcelGenerator;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.Attachment;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.applane.user.service.UserUtility;
import com.applane.user.shared.constants.UserState;
import com.google.apphosting.api.DeadlineExceededException;

public class EmployeeCodeBusinessLogic implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		if (record.getValue("temp") != null) {
			return;
		}
		if (record.getOperationType().equalsIgnoreCase(Updates.Types.INSERT) || (record.getOperationType().equalsIgnoreCase(Updates.Types.UPDATE) && record.getValue("employeecode") == null)) {
			// Object employeeId = record.getValue("__key__");
			Object employeeCode = record.getValue("employeecode");
			Object departmentId = record.getValue("departmentid");
			boolean isAutoEnquiryNo = checkEnableDisable();
			if (!isAutoEnquiryNo && (employeeCode == null || ((String) employeeCode).length() == 0)) {
				JSONArray ApplicationSequencesList = null;
				Integer isDepartmentName = null;
				try {
					ApplicationSequencesList = ApplicationsGenerateCode.getNumberGenerationSchemeDetail(5);
					isDepartmentName = ApplicationSequencesList.getJSONObject(0).optInt("isdepartmentname");
				} catch (JSONException e1) {
					throw new BusinessLogicException("Error come while get depratment is true or false");
				}
				Boolean isDepartment = Boolean.TRUE;
				if (isDepartmentName == null || isDepartmentName.equals(new Integer(0))) {
					isDepartment = Boolean.FALSE;
				}
				if (isDepartment.equals(Boolean.TRUE) && departmentId == null) {
					throw new BusinessLogicException("Please select employee department.");
				} else {
					try {
						JSONObject query = new JSONObject();
						query.put(Data.Query.RESOURCE, "hris_departments");
						JSONArray array = new JSONArray();
						array.put("name");
						array.put("abbreviations");
						query.put(Data.Query.COLUMNS, array);
						query.put(Data.Query.FILTERS, "__key__ = " + departmentId);
						ApplaneDatabaseEngine ResourceEngine = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine();
						JSONObject object = ResourceEngine.query(query);
						JSONArray rows = object.getJSONArray("hris_departments");
						int rowCount = (rows == null || rows.length() == 0) ? 0 : rows.length();
						String departmentName = null;
						String departmentAbbreviation = null;
						if (rowCount > 0) {
							departmentName = rows.getJSONObject(0).optString("name") == null ? "" : rows.getJSONObject(0).optString("name");
							departmentAbbreviation = rows.getJSONObject(0).optString("abbreviations") == null ? "" : rows.getJSONObject(0).optString("abbreviations");
						}
						employeeCode = ApplicationsGenerateCode.generateCode(departmentName, departmentAbbreviation, 5);
						record.addUpdate("employeecode", employeeCode);
					} catch (Exception e) {
						throw new BusinessLogicException("Error come while generate Employee Code.");
					}

				}
			} else if (employeeCode == null) {
				throw new BusinessLogicException("Employee Code can't be blank.");
			}
		}
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {

	}

	public boolean checkEnableDisable() {
		try {
			JSONArray array = ApplicationsGenerateCode.getNumberGenerationSchemeDetail(5);
			if (array == null || array.length() == 0) {
				return true;
			}
			return false;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public void downloadEmployeeReport(Object[] selectedKeys) {
		try {
			JSONObject viewInfo = new JSONObject();
			viewInfo.put("id", "employee_details");
			viewInfo.put("source", "employee_details");
			viewInfo.put("viewid", "employeetableview");
			viewInfo.put(Data.Query.MAX_ROWS, -1);
			JSONObject view = ViewService.get(viewInfo, null, false);
			JSONObject metaData = view.getJSONObject(UserState.View.METADATA);
			JSONObject data = view.getJSONObject(UserState.View.DATA);
			ExcelGenerator generator = new ExcelGenerator(metaData, data);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			generator.generate(outputStream);
			if (outputStream.size() > 0) {
				Attachment relationshipFile = new Attachment("EmployeeDetails.xls", null, outputStream.toByteArray());
				ApplaneMail mail = new ApplaneMail();
				StringBuffer mailBody = new StringBuffer();
				mailBody.append("<div>Dear Sir/Mam").append("</div><br><div>Please Find Attached File. </br></div>");
				mail.setMessage("Employee Records", mailBody.toString(), true);
				mail.setAttachments(relationshipFile);
				mail.setTo(CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL).toString());
				mail.sendAsAdmin(UserUtility.getApplaneUserAdvocateEmailId());
			}
			// }
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

}
