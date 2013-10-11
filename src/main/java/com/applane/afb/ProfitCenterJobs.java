package com.applane.afb;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.exception.VetoException;
import com.applane.databaseengine.job.OperationJob;
import com.applane.databaseengine.utils.ExceptionUtils;
import com.google.apphosting.api.DeadlineExceededException;

public class ProfitCenterJobs implements OperationJob {

	@Override
	public void onBeforeUpdate(Record record) throws VetoException, DeadlineExceededException {
		try {
			ProfitCenterCache.clearCache(record.getKey(), (String) record.getOldValue(AFBConstants.OrganizationProfitCenter.NAME));
		} catch (Exception e) {
			throw new RuntimeException(ExceptionUtils.getExceptionTraceMessage(getClass().getName(), e));
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		// TODO Auto-generated method stub

	}

}
