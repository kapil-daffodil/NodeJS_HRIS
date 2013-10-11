package com.applane.resourceoriented.hris.utility;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.applane.resourceoriented.hris.reports.AbsentExcelReportBusinessLogic;
import com.applane.resourceoriented.hris.reports.EmployeeDetailsExcelReport;
import com.applane.resourceoriented.hris.reports.SalaryExpenseOnlyArrearsRunInSelectedMonth;
import com.applane.resourceoriented.hris.reports.SalaryExpenseWithArrearsOfSameMonth;
import com.applane.resourceoriented.hris.reports.SalaryExpenseWithoutArrears;
import com.applane.resourceoriented.hris.reports.SalarySheetOnlyArrearsRunInSelectedMonth;
import com.applane.resourceoriented.hris.reports.SalarySheetOnlySalary;
import com.applane.resourceoriented.hris.reports.TdsDeductionExcelReport;

@ApplicationPath("/resthris")
public class HRISRestGateway extends Application {
	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(EmployeeDetailsExcelReport.class);
		classes.add(TdsDeductionExcelReport.class);
		classes.add(AbsentExcelReportBusinessLogic.class);
		classes.add(SalaryExpenseWithArrearsOfSameMonth.class);
		classes.add(SalaryExpenseWithoutArrears.class);
		classes.add(SalaryExpenseOnlyArrearsRunInSelectedMonth.class);
		classes.add(SalarySheetOnlySalary.class);
		classes.add(SalarySheetOnlyArrearsRunInSelectedMonth.class);
		return classes;
	}
}
