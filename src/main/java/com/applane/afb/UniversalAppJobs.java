package com.applane.afb;

import com.applane.afb.AFBConstants.DefaultApplications;
import com.applane.afb.user.AfbUserService;
import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.shared.constants.Data.Update.Updates;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class UniversalAppJobs implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		String operationType = record.getOperationType();
		try {
			if (operationType.equals(Updates.Types.INSERT)) {
				Object appGroupKey = record.getValue(DefaultApplications.APPGROUP_ID);
				AfbUserService.allocateUniversalApp(appGroupKey);
			} else if (operationType.equals(Updates.Types.DELETE)) {
				Object appGroupKey = record.getOldValue(DefaultApplications.APPGROUP_ID);
				AfbUserService.misallocateUniversalApp(appGroupKey);
			}
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}
}
