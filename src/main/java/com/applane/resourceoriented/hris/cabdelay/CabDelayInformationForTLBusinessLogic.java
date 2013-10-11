package com.applane.resourceoriented.hris.cabdelay;

import org.json.JSONArray;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.applane.hris.HrisKinds;
import com.google.apphosting.api.DeadlineExceededException;

public class CabDelayInformationForTLBusinessLogic implements OperationJob {

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		try {
			LogUtility.writeLog("On After Update : " + record.getValues());

			JSONArray employeeIds = (JSONArray) record.getValue(HrisKinds.CabDelayInformationTL.EMPLOYEE_ID);
			Object fromTimeMeridiem = record.getValue(HrisKinds.CabDelayInformationTL.FROM_TIME_MERIDIEM);
			Object fromTimeTime = record.getValue(HrisKinds.CabDelayInformationTL.FROM_TIME_TIME);
			Object reportingTo = record.getValue(HrisKinds.CabDelayInformationTL.REPORTINGTO_ID);
			Object toTimeMeridiem = record.getValue(HrisKinds.CabDelayInformationTL.TO_TIME_MERIDIEM);
			Object toTimeTime = record.getValue(HrisKinds.CabDelayInformationTL.TO_TIME_TIME);
			Object reason = record.getValue(HrisKinds.CabDelayInformationTL.REASON);
			Object date = record.getValue(HrisKinds.CabDelayInformationTL.DATE);
			Object statusId = record.getValue(HrisKinds.CabDelayInformationTL.STATUS_ID);
			Object attendanceTypeId = record.getValue(HrisKinds.CabDelayInformationTL.ATTENDANCE_TYPE_ID);
			Object leaveTypeId = record.getValue(HrisKinds.CabDelayInformationTL.LEAVE_TYPE_ID);
			Object paidStatus = record.getValue(HrisKinds.CabDelayInformationTL.PAID_STATUS);
			Object approveByAdminStatusId = record.getValue(HrisKinds.CabDelayInformationTL.APPROVE_BY_ADMIN_STATUS_ID);
			Object cabAdminEmployeeId = record.getValue(HrisKinds.CabDelayInformationTL.CAB_ADMIN_EMPLOYEE_ID);
			Object cabDelayRemarksId = record.getValue(HrisKinds.CabDelayInformationTL.CAB_DELAY_REMARKS_ID);

			int noOfEmployees = (employeeIds == null) ? 0 : employeeIds.length();

			for (int i = 0; i < noOfEmployees; i++) {

				Object employeeId = employeeIds.opt(i);

				JSONObject row = new JSONObject();

				row.put(HrisKinds.CabDelayInformation.EMPLOYEE_ID, employeeId);
				row.put(HrisKinds.CabDelayInformation.FROM_TIME_MERIDIEM, fromTimeMeridiem);
				row.put(HrisKinds.CabDelayInformation.FROM_TIME_TIME, fromTimeTime);
				row.put(HrisKinds.CabDelayInformation.REPORTINGTO_ID, reportingTo);
				row.put(HrisKinds.CabDelayInformation.TO_TIME_MERIDIEM, toTimeMeridiem);
				row.put(HrisKinds.CabDelayInformation.TO_TIME_TIME, toTimeTime);
				row.put(HrisKinds.CabDelayInformation.REASON, reason);
				row.put(HrisKinds.CabDelayInformation.DATE, date);
				row.put(HrisKinds.CabDelayInformation.STATUS_ID, statusId);
				row.put(HrisKinds.CabDelayInformation.ATTENDANCE_TYPE_ID, attendanceTypeId);
				row.put(HrisKinds.CabDelayInformation.LEAVE_TYPE_ID, leaveTypeId);
				row.put(HrisKinds.CabDelayInformation.PAID_STATUS, paidStatus);
				row.put(HrisKinds.CabDelayInformation.APPROVE_BY_ADMIN_STATUS_ID, approveByAdminStatusId);
				row.put(HrisKinds.CabDelayInformation.CAB_ADMIN_EMPLOYEE_ID, cabAdminEmployeeId);
				row.put(HrisKinds.CabDelayInformation.CAB_DELAY_REMARKS_ID, cabDelayRemarksId);

				JSONObject updates = new JSONObject();
				updates.put(Data.Query.RESOURCE, HrisKinds.CAB_DELAY_INFORMATION);
				updates.put(Data.Update.UPDATES, row);

				ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().update(updates);
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(this.getClass().getName(), e));
		}
	}

	@Override
	public void onBeforeUpdate(Record arg0) throws VetoException, DeadlineExceededException {

	}
}
