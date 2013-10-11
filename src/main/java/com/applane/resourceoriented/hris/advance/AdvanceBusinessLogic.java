package com.applane.resourceoriented.hris.advance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.assistance.InterAppCommunication;
import com.applane.assistance.Translator;
import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.EmployeeNameBusinessLogic;
import com.applane.hris.HrisKinds;
import com.applane.moduleimpl.SystemParameters;
import com.applane.procurement.Procurement;
import com.applane.resourceoriented.hris.EmployeeSalaryGenerationServlet;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.resourceoriented.hris.reports.EmployeeSalarySheetReport;
import com.applane.resourceoriented.hris.utility.CommunicationUtility;
import com.applane.resourceoriented.hris.utility.Communication_Constants;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.applane.resourceoriented.hris.utility.HrisHelper;
import com.applane.resourceoriented.hris.utility.PunchingUtility;
import com.applane.resourceoriented.hris.utility.constants.SalaryComponentKinds;
import com.applane.ui.browser.shared.constants.CurrentSession;
import com.google.apphosting.api.DeadlineExceededException;

public class AdvanceBusinessLogic implements OperationJob {

	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
			boolean isDARCL = false;
			if (currentOrganizationId == 606) {
				isDARCL = true;
			}
			if (record.has("temp") || isDARCL) {
				return;
			}
			Object currentUserEmail = CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
			if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR)) {
				int status = Translator.integerValue(record.getNewValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR));
				int oldSeniourStatus = Translator.integerValue(record.getOldValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR));
				int oldDirectorStatus = Translator.integerValue(record.getOldValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR));
				JSONArray approverIdArray = getApproverIdArray(currentUserEmail);
				Object employeeId = record.getValue("employeeid");
				if (employeeId != null) {
					int approverId = 0;
					if (approverIdArray != null && approverIdArray.length() > 0) {
						approverId = Translator.integerValue(approverIdArray.getJSONObject(0).opt(Updates.KEY));
					}

					JSONArray directorIdArray = getDirectorArray(employeeId);
					int directorId = getDirectorId(approverId, directorIdArray);

					if (status == HRISConstants.LEAVE_APPROVED) {
						if (directorId == approverId) {
							int reportingToId = 0;
							if (directorIdArray != null && directorIdArray.length() > 0) {
								reportingToId = Translator.integerValue(directorIdArray.getJSONObject(0).opt("reportingtoid"));
							}
							if (oldSeniourStatus != HRISConstants.LEAVE_APPROVED && reportingToId != approverId) {
								throw new BusinessLogicException("You Can Not Approve Without HOD Approval Of Requester.");
							}
							if (oldDirectorStatus == HRISConstants.LEAVE_APPROVED) {
								throw new BusinessLogicException("You Have Already Approved.");
							}
							record.addUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR, record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR));
							record.addUpdate(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR, record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR));
							record.addUpdate(HRISApplicationConstants.AdvanceAmount.DIRECTOR_APPROVER_ID, approverId);
							if (reportingToId != approverId) {
								record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR);
								record.removeUpdate(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR);
								record.removeUpdate(HRISApplicationConstants.AdvanceAmount.SENIOR_APPROVER_ID);
							}
						} else if (oldSeniourStatus == HRISConstants.LEAVE_APPROVED) {
							throw new BusinessLogicException("You Have Already Approved.");
						} else {
							record.addUpdate(HRISApplicationConstants.AdvanceAmount.SENIOR_APPROVER_ID, approverId);
						}
					} else if (status == HRISConstants.LEAVE_REJECTED) {
						if (directorId == approverId) {
							if (oldDirectorStatus == HRISConstants.LEAVE_APPROVED) {
								throw new BusinessLogicException("You Have Already Approved.");
							}
							if (oldDirectorStatus == HRISConstants.LEAVE_REJECTED) {
								throw new BusinessLogicException("You Have Already Rejected.");
							}
						} else if (oldSeniourStatus == HRISConstants.LEAVE_REJECTED) {
							throw new BusinessLogicException("You Have Already Rejected.");
						} else if (oldSeniourStatus == HRISConstants.LEAVE_APPROVED) {
							throw new BusinessLogicException("You Have Already Approved.");
						}
					}
				} else {
					throw new BusinessLogicException("Employee is Mandatory Field.");
				}
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AdvanceBusinessLogic >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured.");
		}
	}

	private int getDirectorId(int approverId, JSONArray directorIdArray) throws JSONException {
		int directorId = 0;
		if (directorIdArray != null && directorIdArray.length() > 0) {
			Object profitCentersObject = directorIdArray.getJSONObject(0).opt("profitcenterid");
			JSONArray profitCenterArray = new JSONArray();
			if (profitCentersObject instanceof JSONArray) {
				profitCenterArray = (JSONArray) profitCentersObject;
			} else if (profitCentersObject instanceof JSONObject) {
				profitCenterArray.put((JSONObject) profitCentersObject);
			}
			if (profitCenterArray != null && profitCenterArray.length() > 0) {
				for (int counter = 0; counter < profitCenterArray.length(); counter++) {
					Object directorIdObject = profitCenterArray.getJSONObject(counter).opt("director_id");
					if (directorIdObject != null) {
						int directorIdTemp = Translator.integerValue(directorIdObject);
						if (directorIdTemp != 0) {
							if (directorIdTemp == approverId) {
								return directorIdTemp;
							} else {
								directorId = directorIdTemp;
							}
						}
					}
				}
			}
		}
		return directorId;
	}

	private JSONArray getDirectorArray(Object employeeId) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		// columnArray.put("profitcenterid.director_id");
		// columnArray.put("profitcenterid.director_id.officialemailid");

		JSONArray childColumns = new JSONArray();
		childColumns.put("director_id");
		childColumns.put("director_id.officialemailid");
		childColumns.put("director_id.name");
		JSONObject directorIdColumnArray = new JSONObject();
		directorIdColumnArray.put("profitcenterid", childColumns);

		columnArray.put(directorIdColumnArray);
		columnArray.put("reportingtoid");

		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, Updates.KEY + "=" + employeeId);
		JSONArray directorIdArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);

		return directorIdArray;
	}

	private JSONArray getApproverIdArray(Object currentUserEmail) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HrisKinds.EMPLOYEES);
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HrisKinds.Employess.OFFICIAL_EMAIL_ID + "='" + currentUserEmail + "'");
		JSONArray employeeArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HrisKinds.EMPLOYEES);
		return employeeArray;
	}

	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		int currentOrganizationId = Translator.integerValue(CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION_ID));
		boolean isDARCL = false;
		if (currentOrganizationId == 606) {
			isDARCL = true;
		}
		if (record.has("temp") || isDARCL) {
			return;
		}
		Object key = record.getKey();
		try {
			String operationType = record.getOperationType();
			if (operationType.equalsIgnoreCase(Updates.Types.INSERT)) {
				sendMail(record, 0);
			} else if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {

				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR)) {
					int seniourStatus = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_SENIOUR));
					if (seniourStatus == HRISConstants.LEAVE_APPROVED) {
						sendMail(record, 1);
					} else if (seniourStatus == HRISConstants.LEAVE_REJECTED) {
						sendMail(record, 2);
					}
				}
				if (record.has(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR)) {
					int directorStatus = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVE_BY_DIRECTOR));
					if (directorStatus == HRISConstants.LEAVE_APPROVED) {
						sendMail(record, 1);
					} else if (directorStatus == HRISConstants.LEAVE_REJECTED) {
						sendMail(record, 2);
					}
				}
				if (record.has(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS) && record.getNewValue(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS) != null && record.getNewValue(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS) != JSONObject.NULL) {
					Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
					Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");
					Object branchid = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".branchid");
					int emiForMonths = Translator.integerValue(record.getValue(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS));
					Object fromDate = record.getValue(HRISApplicationConstants.AdvanceAmount.FROM_DATE);
					Object dueMonthId = record.getValue(HRISApplicationConstants.AdvanceAmount.DUE_MONTH_ID);
					Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
					double approveByDirectorAmount, advanceAmount = approveByDirectorAmount = Translator.doubleValue(record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR));
					approveByDirectorAmount = (double) approveByDirectorAmount / emiForMonths;
					Calendar cal = Calendar.getInstance();
					cal.setTime(Translator.dateValue(fromDate));
					cal.add(Calendar.MONTH, emiForMonths);
					cal.add(Calendar.DAY_OF_MONTH, -1);
					Date toDate = cal.getTime();
					String toDateStr = EmployeeSalaryGenerationServlet.getDateInString(toDate);
					String filter = SalaryComponentKinds.COMPONENT_TYPE_ID + " = " + HRISApplicationConstants.SalaryComponentTypes.ADVANCE;
					Object componentId = getComponentId(filter);
					if (componentId != null) {
						String invoiceDate = new SimpleDateFormat("yyyy-MM-dd").format(SystemParameters.getCurrentDateTime());
						Date currentDate = Translator.dateValue(invoiceDate);
						cal.setTime(currentDate);

						String monthName = EmployeeSalaryGenerationServlet.getMonthName((cal.get(Calendar.MONTH) + 1));
						String yearName = "" + cal.get(Calendar.YEAR);
						String invoiceNumber = requesterEmployeeCode + "/" + monthName + "/" + yearName + "/Advance";
						Object profitcenterid = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".profitcenterid");
						if (profitcenterid != null && profitcenterid instanceof JSONArray && ((JSONArray) profitcenterid).length() > 0) {
							profitcenterid = ((JSONArray) profitcenterid).get(0);
						}
						insertAdvanceInvoice("" + requesterName, branchid, profitcenterid, currentDate, invoiceNumber, advanceAmount);
						EmployeeNameBusinessLogic.insertEmployeeSalaryPackageComponentsDetails(null, employeeId, componentId, fromDate, toDateStr, approveByDirectorAmount, dueMonthId, 0.0, 0.0);
					} else {
						sendEmailIfComponentNull(fromDate, toDateStr, approveByDirectorAmount, requesterName, requesterEmployeeCode);
						throw new BusinessLogicException("Advance Type Component Not Fount In Salary Components.(Error Emailed To User.)");
					}
				}
			}
		} catch (BusinessLogicException e) {
			try {
				insertEmiMonths(key);
			} catch (JSONException e1) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				LogUtility.writeLog("AdvanceBusinessLogic insertEmiMonths >> Exception Trace >> " + trace);
				throw new BusinessLogicException("Some Unknown Error Occured.");
			}
			throw e;
		} catch (Exception e) {
			try {
				insertEmiMonths(key);
			} catch (JSONException e1) {
				String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
				LogUtility.writeLog("AdvanceBusinessLogic insertEmiMonths >> Exception Trace >> " + trace);
				throw new BusinessLogicException("Some Unknown Error Occured.");
			}
			String trace = ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e);
			LogUtility.writeLog("AdvanceBusinessLogic After >> Exception Trace >> " + trace);
			throw new BusinessLogicException("Some Unknown Error Occured.");
		}
	}

	private void insertEmiMonths(Object key) throws JSONException {
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceAmount.RESOURCE);
		JSONObject row = new JSONObject();
		row.put(Updates.KEY, key);
		row.put(HRISApplicationConstants.AdvanceAmount.EMI_MONTHS, JSONObject.NULL);
		row.put("temp", HRISApplicationConstants.TEMP_VALUE);
		updates.put(Data.Update.UPDATES, row);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

	}

	private void insertAdvanceInvoice(String employeeName, Object branchid, Object profitcenterid, Date invoiceDate, String invoiceNumber, double salaryDifference) throws JSONException {
		JSONObject advanceObject = new JSONObject();
		advanceObject.put(Procurement.ProcurementPayments.PROFITCENTER_ID, profitcenterid);
		advanceObject.put(Procurement.ProcurementPayments.OFFICE_ID, branchid);
		advanceObject.put(Procurement.ProcurementPayments.PAYMENT_NO, invoiceNumber + "/Advance");
		advanceObject.put(Procurement.ProcurementPayments.PAYMENT_DATE, invoiceDate);
		advanceObject.put(Procurement.ProcurementPayments.VENDOR_ID, employeeName);
		advanceObject.put(Procurement.ProcurementPayments.AMOUNT, salaryDifference);

		InterAppCommunication.invokeMethod("com.applane.procurement.SalaryDisbursement", "payAdvanceSalaryOfEmployee", new Object[] { advanceObject });
	}

	private void sendEmailIfComponentNull(Object fromDate, String toDateStr, double approveByDirectorAmount, Object requesterName, Object requesterEmployeeCode) {
		StringBuffer messageContents = new StringBuffer();
		if (requesterEmployeeCode == null) {
			requesterEmployeeCode = "";
		}
		if (requesterName == null) {
			requesterName = "";
		}
		if (fromDate == null) {
			fromDate = "";
		}
		if (toDateStr == null) {
			toDateStr = "";
		}
		messageContents.append("<b>Advance Type Component Not Fount In Salary Components.</b> <BR />");
		messageContents.append("<ul><li><B>Employee Code : </B>" + requesterEmployeeCode + "</li>");
		messageContents.append("<li><B>Employee Name : </B>" + requesterName + "</li>");
		messageContents.append("<li><B>From Date : </B>" + fromDate + "</li>");
		messageContents.append("<li><B>To Date : </B>" + toDateStr + "</li>");
		messageContents.append("<li><B>Amount : </B>" + approveByDirectorAmount + "</li></ul>");
		messageContents.append("<BR />");
		messageContents.append("Regards <BR /> Applane Team<BR />");
		messageContents.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");
		String title = "Error While Assigning Advance Component to " + requesterName;
		String currentUserEmail = "" + CurrentState.getCurrentVariable(CurrentSession.CURRENT_USER_EMAIL);
		HrisHelper.sendMails(new String[] { currentUserEmail }, messageContents.toString(), title);
	}

	private Object getComponentId(String filter) throws Exception {
		JSONObject query = new JSONObject();
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, SalaryComponentKinds.RESOURCE);
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, filter);
		JSONArray salaryComponentTypeArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(SalaryComponentKinds.RESOURCE);
		if (salaryComponentTypeArray != null && salaryComponentTypeArray.length() > 0) {
			return salaryComponentTypeArray.getJSONObject(0).opt(Updates.KEY);
		}
		return null;
	}

	private void sendMail(Record record, int statusApprovedOrReject) throws Exception {
		String operationType = record.getOperationType();
		Map<String, Object> communicationInfo = CommunicationUtility.getCommunicationInfo();

		Object organizationLogo = communicationInfo.get(Communication_Constants.ORGANIZATION_LOGO);
		Object organizationName = communicationInfo.get(Communication_Constants.ORGANIZATION_NAME);
		Object employeeId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID);
		Object requesterName = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".name");
		Object branchId = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".branchid");
		Object requesterEmployeeCode = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".employeecode");
		Object directorName = record.getValue(HRISApplicationConstants.AdvanceAmount.DIRECTOR_APPROVER_ID + ".name");
		Object seniorName = record.getValue(HRISApplicationConstants.AdvanceAmount.SENIOR_APPROVER_ID + ".name");

		Object requesterEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.EMPLOYEE_ID + ".officialemailid");
		Object directorEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.DIRECTOR_APPROVER_ID + ".officialemailid");
		Object seniorEmail = record.getValue(HRISApplicationConstants.AdvanceAmount.SENIOR_APPROVER_ID + ".officialemailid");

		Object requestDate = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_DATE);
		Object requestAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.REQUEST_AMOUNT);
		Object remarks = record.getValue(HRISApplicationConstants.AdvanceAmount.REMARKS);

		String title = "Request For Advance";
		boolean mailToHr = false;

		if (organizationLogo == null) {
			organizationLogo = "";
		}
		if (organizationName == null) {
			organizationName = "Organization Logo";
		}

		if (seniorName == null) {
			seniorName = record.getValue("employeeid.reportingtoid.name");
		}
		if (requesterEmployeeCode == null) {
			requesterEmployeeCode = "";
		}
		if (seniorEmail == null) {
			seniorEmail = record.getValue("employeeid.reportingtoid.officialemailid");
		}
		if (requestDate == null) {
			requestDate = "";
		}
		if (requestAmount == null) {
			requestAmount = "";
		}
		if (remarks == null) {
			remarks = "";
		}

		StringBuffer messageContents = new StringBuffer();
		Object approveBySeniorAmount = null;
		Object approveByDirectorAmount = null;
		messageContents.append("<img src=\"").append(organizationLogo).append("\" alt=\"").append(organizationName).append("\"/>");
		messageContents.append("<hr style='border:none;height:5px;background:rgb(90,154,225);'/>");
		int[] size = { 0 };
		List<String> emailList = new ArrayList<String>();
		List<String> nameList = new ArrayList<String>();

		getEmailAndNameOfDirectorList(employeeId, size, emailList, nameList);

		if (operationType.equalsIgnoreCase(Updates.Types.INSERT)) {
			messageContents.append("Dear ").append(seniorName);
			{
				for (int counter = 0; counter < nameList.size(); counter++) {
					messageContents.append(" / ").append(nameList.get(counter));
				}
			}
			messageContents.append("<BR /><BR />");
			messageContents.append(requesterName).append(" Has Requested For Advance<br>");

		} else if (operationType.equalsIgnoreCase(Updates.Types.UPDATE)) {
			messageContents.append("Dear ").append(requesterName).append("<BR /><BR />");
			if (record.has(HRISApplicationConstants.AdvanceAmount.SENIOR_APPROVER_ID)) {
				if (statusApprovedOrReject == 1) {
					title += " Approved";
					messageContents.append(seniorName).append(" Has Approved Your Request For Advance Amount.<br>");
				} else if (statusApprovedOrReject == 2) {
					title += " Rejected";
					messageContents.append(seniorName).append(" Has Rejected Your Advance Amount Request.<br>");
				}

			} else if (record.has(HRISApplicationConstants.AdvanceAmount.DIRECTOR_APPROVER_ID)) {
				if (statusApprovedOrReject == 1) {
					title += " Approved";
					mailToHr = true;
					messageContents.append(directorName).append(" Has Approved Amount.<br>");
				} else if (statusApprovedOrReject == 2) {
					title += " Rejected";
					messageContents.append(directorName).append(" Has Rejected Your Advance Amount Request.<br>");
				}
			}
		}
		approveBySeniorAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_SENIOUR);
		approveByDirectorAmount = record.getValue(HRISApplicationConstants.AdvanceAmount.APPROVED_AMOUNT_BY_DIRECTOR);

		messageContents.append("<ul><li><B>Employee Code : </B>").append(requesterEmployeeCode).append("</li>");
		messageContents.append("<li><B>Request Date : </B>").append(requestDate).append("</li>");
		messageContents.append("<li><B>Request Amount : </B>").append(requestAmount).append("</li>");
		if (approveBySeniorAmount != null) {
			if (statusApprovedOrReject == 1) {
				messageContents.append("<li><B>Approved Amount By Senior : </B>").append(approveBySeniorAmount).append("</li>");
			} else if (statusApprovedOrReject == 2) {
				messageContents.append("<li><B>Approved Amount By Senior : </B>").append("Rejected").append("</li>");
			}
		}
		if (approveByDirectorAmount != null) {
			if (statusApprovedOrReject == 1) {
				messageContents.append("<li><B>Request Amount By Director : </B>").append(approveByDirectorAmount).append("</li>");
			} else if (statusApprovedOrReject == 2) {
				messageContents.append("<li><B>Request Amount By Dorector : </B>").append("Rejected").append("</li>");
			}

		}
		if (statusApprovedOrReject == 0) {
			messageContents.append("<li><B>Reason : </B>").append(remarks).append("</li></ul>");
		}
		messageContents.append("<BR />");
		messageContents.append("Regards <BR /> Applane Team<BR />");
		messageContents.append("** Please do not reply to this email.  Your response will not be received.  For more details please log in to your Applane account.");

		if (seniorEmail != null && ("" + seniorEmail).length() > 0) {
			size[0]++;
		}
		if (directorEmail != null && ("" + directorEmail).length() > 0) {
			size[0]++;
		}
		Object emailId = null;
		if (mailToHr) {
			String filter = "branchid = " + branchId;
			JSONArray hrMailIdDetail = EmployeeSalarySheetReport.getHrMailIdDetail(filter);
			if (hrMailIdDetail != null && hrMailIdDetail.length() > 0) {
				emailId = hrMailIdDetail.getJSONObject(0).opt("employeeid.officialemailid");
				if (emailId != null) {
					size[0]++;
				} else {
					mailToHr = false;
				}
			}
		}
		String[] emails = new String[size[0]];
		int i = 0;
		if (size[0] > 0 && seniorEmail != null) {
			emails[0] = "" + seniorEmail;
			i = 1;
		}
		if (size[0] > 1 && directorEmail != null) {
			emails[1] = "" + directorEmail;
			i = 2;
		}
		for (int counter = 0; counter < emailList.size(); counter++) {
			emails[i] = emailList.get(counter);
			i++;
		}
		if (mailToHr) {
			emails[i] = "" + emailId;
		}
		PunchingUtility.sendMailsAccToProfitCenters(new String[] { "" + requesterEmail }, messageContents.toString(), title, emails);
	}

	private void getEmailAndNameOfDirectorList(Object employeeId, int[] size, List<String> emailList, List<String> nameList) throws Exception, JSONException {
		JSONArray directorIdArray = getDirectorArray(employeeId);
		if (directorIdArray != null && directorIdArray.length() > 0) {

			if (directorIdArray != null && directorIdArray.length() > 0) {
				Object profitCentersObject = directorIdArray.getJSONObject(0).opt("profitcenterid");
				JSONArray profitCenterArray = new JSONArray();
				if (profitCentersObject instanceof JSONArray) {
					profitCenterArray = (JSONArray) profitCentersObject;
				} else if (profitCentersObject instanceof JSONObject) {
					profitCenterArray.put((JSONObject) profitCentersObject);
				}
				if (profitCenterArray != null && profitCenterArray.length() > 0) {
					for (int counter = 0; counter < profitCenterArray.length(); counter++) {
						Object directorEmailIdObject = profitCenterArray.getJSONObject(counter).opt("director_id.officialemailid");
						Object directorNameObject = profitCenterArray.getJSONObject(counter).opt("director_id.name");
						if (directorEmailIdObject != null) {
							emailList.add("" + directorEmailIdObject);
							nameList.add("" + directorNameObject);
							size[0]++;
						}
					}
				}
			}
		}
	}

	public void deleteSelectedRecords(Object[] keys) throws JSONException {
		if (keys == null || keys.length == 0) {
			return;
		}
		String selectedKeys = "";
		JSONObject query = new JSONObject();
		JSONArray advanceArray = new JSONArray();
		for (int counter = 0; counter < keys.length; counter++) {
			if (selectedKeys.length() > 0) {
				selectedKeys += ",";
			}
			selectedKeys += "" + keys[counter];

			JSONObject row = new JSONObject();
			row.put("__key__", keys[counter]);
			row.put("__type__", "delete");
			advanceArray.put(row);
		}
		JSONObject updates = new JSONObject();
		updates.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceAmount.RESOURCE);

		updates.put(Data.Update.UPDATES, advanceArray);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);

		JSONArray advanceSummaryArray = getAdvanceSummaryArray(selectedKeys, query);

		for (int counter = 0; counter < advanceSummaryArray.length(); counter++) {
			JSONObject object = advanceSummaryArray.getJSONObject(counter);
			object.put("__type__", "delete");
		}
		query = new JSONObject();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);
		query.put(Data.UPDATES, advanceSummaryArray);
		ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(query);

	}

	private JSONArray getAdvanceSummaryArray(String selectedKeys, JSONObject query) throws JSONException {
		JSONArray columnArray = new JSONArray();
		query.put(Data.Query.RESOURCE, HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);
		columnArray.put(Updates.KEY);
		query.put(Data.Query.COLUMNS, columnArray);
		query.put(Data.Query.FILTERS, HRISApplicationConstants.AdvanceApprovedHODSummary.ADVANCE_ID + " IN(" + selectedKeys + ")");
		JSONArray advanceSummaryArray = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray(HRISApplicationConstants.AdvanceApprovedHODSummary.RESOURCE);
		return advanceSummaryArray;
	}
}
