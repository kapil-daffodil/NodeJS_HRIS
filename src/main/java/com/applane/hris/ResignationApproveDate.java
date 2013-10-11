package com.applane.hris;

import java.text.SimpleDateFormat;

import com.applane.databaseengine.Record;
import com.applane.databaseengine.job.OperationJob;
import com.applane.moduleimpl.SystemParameters;
import com.applane.resourceoriented.hris.utility.HRISConstants;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * @author Ajay Pal Singh
 * @version 1.0
 * @category HRIS businesslogic
 */
public class ResignationApproveDate implements OperationJob{

	//job define on "hris_employeeresignation"
	@Override
	public void onBeforeUpdate(Record record) throws DeadlineExceededException {
		// TODO Auto-generated method stub
		int newStatus = (Integer) record.getValue("resignationstatusid");
		int oldStatus = (Integer) record.getOldValue("resignationstatusid");
		if((oldStatus == HRISConstants.RESIGNATION_NEW || oldStatus == HRISConstants.RESIGNATION_ONHOLD || oldStatus == HRISConstants.RESIGNATION_REJECTED)&& newStatus == HRISConstants.RESIGNATION_ACCEPTED){
			
			String TODAY_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(SystemParameters.getCurrentDateTime());
			record.addUpdate("approveddate",TODAY_DATE);
		}
	}

	@Override
	public void onAfterUpdate(Record record) throws DeadlineExceededException {
		
	}

}
