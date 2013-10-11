package com.applane.resourceoriented.hris;

import com.applane.assistance.Translator;
import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.utils.ExceptionUtils;

public class LeaveRuleBusinessLogic implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) {
		String operationType = record.getOperationType();
		try {
			if (operationType.equalsIgnoreCase("insert")) {
				applyValidation(record);
			} else if (operationType.equalsIgnoreCase("update")) {
				applyValidation(record);
			}
		} catch (BusinessLogicException e) {
			throw e;
		} catch (Exception e) {
			LogUtility.writeError(ExceptionUtils.getExceptionTraceMessage(LeaveRuleBusinessLogic.class.getName(), e));
			throw new BusinessLogicException("Sorry for the inconvinence caused. Unable to update leave rule record.");
		}
	}

	private void applyValidation(Record record) {

		boolean ispayable = DataTypeUtilities.booleanValue(record.getValue("payable"));
		boolean isEwdAdjustAgainstLeaves = DataTypeUtilities.booleanValue(record.getValue("isextraworkingdaysadjustagainstleave"));
		if (ispayable == false && isEwdAdjustAgainstLeaves == true) {
			throw new BusinessLogicException("For non payable leave type extra working day not adjust against leave.");
		}
		if (Translator.integerValue(record.getValue("leavecreditid")) == HRISApplicationConstants.LeaveCredit.YEARLY_CREDIT) {
			if (record.getValue("proportionate_base") == null || record.getValue("accrual_base") == null) {
				throw new BusinessLogicException("For Yearly Credit Proportionate Base and Accrual Base is Mandatory.");
			}
		}
	}

	@Override
	public void onAfterUpdate(Record record) {
		// TODO Auto-generated method stub
	}
}