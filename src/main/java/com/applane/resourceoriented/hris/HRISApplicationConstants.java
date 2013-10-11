package com.applane.resourceoriented.hris;

public interface HRISApplicationConstants {

	/**
	 * Duration Type
	 * */

	public static int DURATION_FULLDAY = 1;

	public static int DURATION_FIRSTHALF = 2;

	public static int DURATION_SECONDHALF = 3;

	/**
	 * Attendance Types
	 */

	public static int ATTENDANCE_PRESENT = 1;

	public static int ATTENDANCE_ABSENT = 2;

	public static int ATTENDANCE_LEAVE = 3;

	public static int ATTENDANCE_TOUR = 4;

	public static int ATTENDANCE_HOLIDAY = 5;

	public static int ATTENDANCE_OFF = 6;

	public static int ATTENDANCE_EXTRAWORKINGDAY = 7;

	public static int ATTENDANCE_EXTRAWORKINGDAY_HALF = 8;

	public static int ATTENDANCE_FIRST_HALFDAY_LEAVE = 9;

	public static int ATTENDANCE_UNKNOWN = 10;

	public static int ATTENDANCE_WORK_FROM_HOME = 11;

	public static int ATTENDANCE_SECOND_HALFDAY_LEAVE = 12;

	public static int ATTENDANCE_WORK_FROM_HOME_HALF_DAY = 13;

	public static int ATTENDANCE_HALF_DAY_OFF = 14;

	public static int ATTENDANCE_HALF_DAY_ABSENT = 15;

	/**
	 * Employee Status
	 */

	public static int EMPLOYEE_ACTIVE = 1;

	public static int EMPLOYEE_INACTIVE = 2;

	/*
	 * Punching In/Out Status
	 */
	public static int PUNCH_ENTRY = 1;

	public static int PUNCH_EXIT = 2;

	/*
	 * Punching types
	 */

	public static int PUNCHING_CONSISTENT = 1;

	public static int PUNCHING_INCONSISTENT = 2;

	public static long FIVE_HOURS = 18000000;

	/*
	 * Salary Component Criteria
	 */

	public static int FIXED_BASED = 1;

	public static int ATTENDANCE_BASED = 2;

	public static int PERFORMANCE_BASED = 3;

	public static int EXPERIENCE_BASED = 4;

	public static int NET_PAYABLE_BASED = 5;

	/*
	 * Payment Types
	 */
	public static int PAYABLE = 1;

	public static int DEDUCTABLE = 2;

	/*
	 * Performance Types
	 */
	public static int TARGET_ACHIEVED = 1;

	public static int PERFORMANCE_PERCENTAGE = 2;

	/*
	 * Salary Payment Cycles
	 */

	public static Integer YEARLY = 1;

	public static Integer HALF_YEARLY = 2;

	public static Integer QUARTERLY = 3;

	public static Integer MONTHLY = 4;

	public static Integer DAILY = 5;

	public static Integer ONE_TIME = 6;

	/*
	 * Payment period
	 */

	public static int PAY_YEARLY = 1;

	public static int PAY_HALFYEARLY = 2;

	public static int PAY_QUARTERLY = 4;

	public static int PAY_MONTHLY = 12;

	public static int PAY_DAILY = 365;

	/*
	 * Transient Column value
	 */

	public static String TEMP_VALUE = "temp";

	public static String TEMP_VALUE_RUN_JOBS = "sendReverseProcess";

	public static String TEMP_VALUE_RUN_JOBS_ON_LEAVE_APPROVAL = "sendReverseProcessOnLeaveApproval";

	public static String TEMP_VALUE_TO_MARK_ABSENT = "markAbsent";

	public static String TEMP_VALUE_FROM_POPULATE_MONTHLY_ATTENDANCE = "fromPopulateMonthlyAttendance";

	public static String TEMP_VALUE_FROM_NIGHT_SCHEDULER_POPULATE_ATTENDANCE = "fromNightSchedulerPopulateAttendance";

	public static String TEMP_VALUE_STOP_POPULATEMONTHLY_ATTENDANCE = "stopPopulateMonthlyAttendance";

	/**
	 * Monthly attandance aggregate
	 */

	public static String TOTAL_OFF_IN_MONTH = "totalmonthlyoff";

	public static String TOTAL_HOLIDAYS_IN_MONTH = "totalmonthlyholiday";

	public static String TOTAL_NON_WORKING_DAY_IN_MONTH = "nonworkingdays";

	/**
	 * Branches
	 */

	public int BRANCH_CLINT_SIDE = 5;

	/**
	 * Leave Paid Status
	 */

	public int LEAVE_EWD_PAID = 1;

	public int LEAVE_EWD_UN_PAID = 2;

	public int LEAVE_EWD_COMBO_OFF = 3;

	/**
	 * Leave Accruel
	 */

	public int LEAVE_DAY_WISE_ACCRUE = 1;

	public int LEAVE_MONTHLY_ACCRUE = 2;

	/**
	 * Amount Pay To
	 */

	public int AMOUNT_TO_EMPLOYEE = 1;

	public int AMOUNT_TO_GOVERNMENT = 2;

	/**
	 * Morning Scheduler for HRIS
	 */

	public String[] ORGANIZATIONS = { "Daffodil", "Darcl Logistics Limited", "girnarsoft.com", "navigant", "Navigant_Technologies__P__Limited" };

	public String[] ORGANIZATIONS_FOR_TESTING = { "DaffodilCRM" };

	/**
	 * Weekdays
	 */

	interface Weekdays {

		public static String MONDAY = "monday";

		public static String TUESDAY = "tuesday";

		public static String WEDNESDAY = "wednesday";

		public static String THURSDAY = "thursday";

		public static String FRIDAY = "friday";

		public static String SATURDAY = "saturday";

		public static String SUNDAY = "sunday";
	}

	interface SalaryComponents {

		public static String EMPLOYEE_SALARY_COMPONENT_ID = "employeesalarycomponentid";

		public static String SALARY_COMPONENT_ID = "salarycomponentid";

		public static String PAYMENT_CYCLE_ID = "paymentcycleid";

		public static String PAYMENT_TYPE_ID = "paymenttypeid";

		public static String COMPONENT_CRITERIA_ID = "componentcriteriaid";

		public static String COMPONENT_AMOUNT = "componentamount";

		public static String KPI_ID = "kpiid";

		public static String PERFORMANCE_TYPE_ID = "performancetypeid";

		public static String PAID_AFTER_MONTH = "paidaftermonth";

		public static String SALARY_ADVANCE_DUE_TO_ARREAR = "Salary Advance Due To Arrear";
	}

	interface ServerName {

		public static String APPLANE_CRM_HRD = "applanecrmhrd";

	}

	interface LeavePaymentType {
		public static int PAID = 1;

		public static int UNPAID = 2;
	}

	interface kpiTypes {
		public static int PERFORMANCE = 1;

		public static int NORMAL = 2;

		public static int EXCEPTION = 3;
	}

	interface Freez {
		public static int ATTENDANCE = 1;

		public static int SALARY = 2;

		public static int VARIABLE = 3;

		public static int SALARY_COMPONENTS = 4;
	}

	interface EmployeeDecision {

		public static int YES = 1;

		public static int NO = 2;
	}

	interface LeaveRequestColumns {

		public static String FROM_DATE = "fromdate";

		public static String TO_DATE = "todate";

		public static String FROM_DURATION_TYPE = "fromdurationtype";

		public static String TO_DURATION_TYPE = "todurationtype";
	}

	interface PerformanceColumns {

		public static String ACTUAL_AMOUNT = "actualamount";

		public static String PERFORMANCE_AMOUNT = "performanceamount";
	}

	interface LeaveTypes {

		public static Integer EARNED_LEAVE = 1;

		public static Integer CASUAL_LEAVE = 2;

		public static Integer MEDICAL_LEAVE = 3;

		public static Integer SPECIAL_LEAVE = 4;
	}

	interface AttendanceParameters {

		public static String TOTAL_PRESENTS = "totalpresents";

		public static String TOTAL_ABSENTS = "totalabsents";

		public static String TOTAL_LEAVES = "totalleaves";

		public static String TOTAL_EXTRAWORKINGDAYS = "totalextraworkingdays";

		public static String TOTAL_EARNED_LEAVES = "totalearnedleaves";

		public static String TOTAL_CASUAL_LEAVES = "totalcasualleaves";

		public static String TOTAL_MEDICAL_LEAVES = "totalmedicalleaves";

		public static String TOTAL_SPECIAL_LEAVES = "totalspecialleaves";

		public static String TOTAL_NON_WORKING_DAYS = "totalnonworkingdays";

	}

	interface LeavePaidStatus {

		public static int LEAVE_STATUS_PAYABLE = 1;

		public static int LEAVE_STATUS_NONPAYABLE = 2;

	}

	interface LeaveCredit {

		public static Integer MONTHLY_CREDIT = 1;

		public static Integer YEARLY_CREDIT = 2;

		public static Integer HALF_YEARLY_CREDIT = 3;

		public static Integer QUATERLY_YEARLY_CREDIT = 4;

	}

	interface AttendanceTypeShortLeave {

		public static Integer SHORT_LEAVE = 1;

		public static Integer NOT_CONSIDER_AS_SHORT_LEAVE = 2;

	}

	interface QueueConstantsHRIS {

		public static String POPULATE_ATTENDANCE = "PopulateAttendance";

		public static String SALARY_PROCESS = "SalaryProcess";

	}

	interface SalaryComponentTypes {

		public static int PF_EMPLOYEE = 1;

		public static int PF_EMPLOYER = 2;

		public static int ESI_EMPLOYEE = 3;

		public static int ESI_EMPLOYER = 4;

		public static int LWF_EMPLOYEE = 5;

		public static int LWF_EMPLOYER = 6;

		public static int TDS = 7;

		public static int BASIC = 8;

		public static int TA = 9;

		public static int HRA = 10;

		public static int ADVANCE = 11;

		public static int USL = 12;

		public static int GRATUITY = 13;

		public static int FLAXI = 14;
	}

	interface LodgingComponents {

		public static Integer LODGING = 1;

		public static Integer FOODING = 2;

		public static Integer HOTEL = 3;

	}

	interface TakeSalarySheet {

		public static String BRANCH_ID = "branchid";

		public static String MONTH_ID = "monthid";

		public static String YEAR_ID = "yearid";

	}

	interface FinancialYear {

		public static String RESOURCE = "finencialyear";

		public static String FROM_DATE = "fromdate";

		public static String TO_DATE = "todate";

		public static String NAME = "name";
	}

	interface TDSRules {

		public static String RESOURCE = "tdsrules";

		public static String FINANCIAL_YEAR_ID = "financialyearid";

		public static String HRA_ON_BASIC = "hra_on_basic";

		public static String SQD = "saving_qualifying_deduction";

		public static String SQR = "saving_qualifying_rebate";

		public static String CHILDREN_EDUCATION_ALLOWANCE = "childran_education_allowance";

		public static String REBATE_BY_GOVERNMENT = "rebate_by_government";

		public static String MAX_CHILDREN_ALLOWED = "maxchildrenallowed";
	}

	interface TDSDeclarationSheet {

		public static String RESOURCE = "employee_incometax_declaration";

		public static String EMPLOYEE_ID = "employee_id";

		public static String HRA_APRIL_RENT = "april_rent_amount";

		public static String HRA_MAY_RENT = "may_rent_amount";

		public static String HRA_JUNE_RENT = "june_rent_amount";

		public static String HRA_JULY_RENT = "july_rent_amount";

		public static String HRA_AUGUST_RENT = "august_rent_amount";

		public static String HRA_SEPTEMBER_RENT = "september_rent_amount";

		public static String HRA_OCTOBER_RENT = "october_rent_amount";

		public static String HRA_NOVEMBER_RENT = "november_rent_amount";

		public static String HRA_DECEMBER_RENT = "december_rent_amount";

		public static String HRA_JANUARY_RENT = "january_rent_amount";

		public static String HRA_FEBRUARY_RENT = "february_rent_amount";

		public static String HRA_MARCH_RENT = "march_rent_amount";

		public static String HRA_TOTAL = "total_rent_amount";

		public static String A_80_D = "amount_80d_amount";

		public static String A_80_DD = "amount_80dd_amount";

		public static String A_80_DDB = "amount_80ddb_amount";

		public static String A_80_E = "amount_80e_amount";

		public static String A_80_U = "amount_80u_amount";

		public static String B_80_C_LIC = "rebate_amount_1_amount";

		public static String B_80_C_PPF = "rebate_amount_2_amount";

		public static String B_80_C_PF = "rebate_amount_3_amount";

		public static String B_80_C_NATIONAL_SAVING = "rebate_amount_4_amount";

		public static String B_80_C_PENSION_SCHEME = "rebate_amount_5_amount";

		public static String B_80_C_DEPOSIT_IN_5_YEAR_TERM = "rebate_amount_6_amount";

		public static String B_80_C_HOUSE_LONE = "rebate_amount_7_amount";

		public static String B_80_C_TUTION_FEES = "rebate_amount_8_amount";

		public static String B_80_C_FIXED_DEPOSIT = "rebate_amount_10_amount";

		public static String B_80_C_EQUITY_LINKED_SAVING = "rebate_amount_11_amount";

		public static String CHILDRENS = "chilldren_cea";

	}

	interface TDSRulesPercentages {
		public static String RESOURCE = "tdsrules_amount_range_percentage";

		public static String TDS_RULES_ID = "tdsrulesid";

		public static String FROM_AMOUNT = "fromamount";

		public static String TO_AMOUNT = "toamount";

		public static String PERCENTAGE = "percentage";
	}

	interface EmployeeAssets {
		public static String RESOURCE = "hris_employeeassets";

		public static String EMPLOYEE_ID = "employeeid";

		public static String ASSIGNING_DATE = "assigningdate";

		public static String RETURN_DATE = "returndate";

		public static String IDENTITY_NUMBER = "identitynumber";
	}

	interface TemplateMail {
		public static String RESOURCE = "hris_template_mail";

		public static String NAME = "name";

		public static String QUERY = "query";

		public static String TEMPLATE = "template";

		public static String MAIL_ID_COLUMN = "mail_id_column";

		public static String TITLE = "title";
	}

	interface EncashBalanceLeaves {
		public static String RESOURCE = "hris_encash_leaves";

		public static String STATUS_ID = "statusid";

		public static String REQUEST_DATE = "request_date";

		public static String ENCASH_LEAVES = "encash_leaves";

		public static String EMPLOYEE_ID = "employeeid";

		public static String BALANCE_LEAVES = "balance_leaves";

		public static String LEAVE_TYPE_ID = "leave_type_id";

		public static String AMOUNT = "amount";

		public static String LEAVES = "leaves";

		public static String YEAR_ID = "yearid";

		public static String MONTH_ID = "monthid";
	}

	interface AdvanceAmount {
		public static String RESOURCE = "hris_advance_amount";

		public static String EMPLOYEE_ID = "employeeid";

		public static String REQUEST_DATE = "request_date";

		public static String REQUEST_AMOUNT = "request_amount";

		public static String APPROVE_BY_SENIOUR = "approve_by_seniour";

		public static String APPROVE_BY_DIRECTOR = "approve_by_director";

		public static String APPROVED_AMOUNT_BY_SENIOUR = "approved_amount_by_seniour";

		public static String APPROVED_AMOUNT_BY_DIRECTOR = "approved_amount_by_director";

		public static String FROM_DATE = "from_date";

		public static String DUE_MONTH_ID = "duemonthid";

		public static String EMI_MONTHS = "emi_per_month";

		public static String REMARKS = "remarks";

		public static String EMI_REMARKS = "emi_required";

		public static String SENIOR_APPROVER_ID = "senior_approver_id";

		public static String DIRECTOR_APPROVER_ID = "director_approver_id";

		public static String TEMP_APPROVED_STATUS = "approver_id";

		public static String TEMP_IS_DIRECTOR = "is_director";

		public static String EMAILED_TO_BU_HR = "emailed_to_bu_hr";

		public static String IS_PROCEEDING = "is_proceeding";

		public static String REMARKS_FOR_HISTORY = "remarks_for_history";

		public static String REMARKS_FOR_HISTORY_DIRECTOR = "remarks_for_history_director";

		public static String IS_BU_HR = "is_bu_hr";

		public static String LAST_ADVANCE_TAKEN_TEMP = "last_advance_taken_temp";

		public static String BALANCE_TEMP = "balance_temp";

		public static String DATE_OF_LAST_ADVANCE_CLEARED_TEMP = "date_of_last_advance_cleared_temp";

		public static String LAST_ADVANCE_TAKEN = "last_advance_taken";

		public static String BALANCE = "balance";

		public static String DATE_OF_LAST_ADVANCE_CLEARED = "date_of_last_advance_cleared";

		public static String GURRENTER_ID = "gurrenter_id";

		public static String IS_GURRENTER = "is_gurrenter";

		public static String DIRECTOR_EMAIL = "director_email";

		public static String LONE_REMARKS = "lone_remarks";

		public static String INTREST_PERCENTAGE = "intrest_percentage";
	}

	interface AdvanceApprovedHODSummary {
		public static String RESOURCE = "hris_advance_approved_hod_summary";

		public static String ADVANCE_ID = "advance_id";

		public static String EMPLOYEE_ID = "employeeid";

		public static String DATE = "date";

		public static String APPROVED_AMOUNT = "amount";

		public static String STATUS_ID = "approve_reject_statusid";

		public static String REMARKS = "remarks";

		public static String IS_REPORTING_TO = "isreportingto";

		public static String IS_DIRECTOR = "isdirector";

		public static String IS_EMPLOYEE = "isemployee";

		public static String IS_CONSIDER = "is_consider";

		public static String IS_BU_HR = "isbuhr";

		public static String IS_SENIOR_HR = "is_senior_hr";

		public static String LAST_ADVANCE_TAKEN = "last_advance_taken";

		public static String BALANCE = "balance";

		public static String DATE_OF_LAST_ADVANCE_CLEARED = "date_of_last_advance_cleared";

		public static String IS_GURRENTER = "is_gurrenter";

		public static String IS_CMD = "is_cmd";

		public static String LONE_REMARKS = "lone_remarks";

		public static String INTREST_PERCENTAGE = "intrest_percentage";
	}

	interface EmployeeMailTemplates {
		public static String RESOURCE = "hris_employees_templates";

		public static String TEMPLATE_ID = "templeteid";

		public static String FIXED_SALARY_FIRST = "fixed_salary_first";

		public static String VARIABLE_PART_FIRST = "variable_part_first";

		public static String TOTAL_SALARY_FIRST = "total_salary_first";

		public static String GRATUITY_FIRST = "gratuity_first";

		public static String HEALTH_INSURANCE_FIRST = "health_insurance_first";

		public static String FIXED_SALARY_SECOND = "fixed_salary_second";

		public static String VARIABLE_PART_SECOND = "variable_part_second";

		public static String TOTAL_SALARY_SECOND = "total_salary_second";

		public static String GRATUITY_SECOND = "gratuity_second";

		public static String HEALTH_INSURANCE_SECOND = "health_insurance_second";

		public static String EMPLOYEE_IDS = "employee_ids";

		public static String APPLICABLE_YEAR = "applicable_year";

		public static String DATE = "date";

	}

	interface RecruitmentMrfApprovalProcess {
		public static String RESOURCE = "hris_mrf_approveal_process";

		public static String EMPLOYEE_ID = "employeeid";

		public static String DATE = "date";

		public static String STATUS_ID = "approve_reject_statusid";

		public static String REMARKS = "remarks";

		public static String IS_REPORTING_TO = "isreportingto";

		public static String IS_DIRECTOR = "isdirector";

		public static String IS_EMPLOYEE = "isemployee";

		public static String MRF_REQUEST_ID = "mrf_request_id";

		public static String IS_CONSIDER = "is_consider";

		public static String IS_BU_HR = "isbuhr";

		public static String IS_SENIOR_HR = "is_senior_hr";
	}

	interface RecruitmentMrfProcess {
		public static String RESOURCE = "hris_mrf_request_form";

		public static String STATUS_ID = "statusid";

		public static String EMPLOYEE_ID = "employeeid";

		public static String APPROVER_TEMP = "approver_temp";

		public static String REMARKS_FOR_HISTORY = "remarks_for_history";

		public static String DIRECTOR_EMAIL = "director_email";

		public static String STATUS_TEMP = "status_temp";
	}

	interface RecruitmentShortListedResumes {
		public static String RESOURCE = "hris_mrf_short_listed_resumes";

		public static String RESUME_HOLDER_ID = "resume_holder_id";

		public static String MRF_REQUEST_ID = "mrf_request_id";

		public static String INTERVIEW_ASSIGNED_TO_ID = "interview_assigned_to_id";

		public static String INTERVIEW_DATE = "interview_date";

		public static String REMARKS = "remarks";
	}

	interface LeaveAccrualYearly {
		public static String RESOURCE = "hris_leave_accrual_yearly";

		public static String EMPLOYEE_ID = "employeeid";

		public static String DATE = "date";

	}

	interface TourBeforeApprovalComments {
		public static String RESOURCE = "hris_tourrequests_before_approval_comments";

		public static String TOUR_ID = "tour_id";

		public static String REMARKS = "remarks";

		public static String DATE = "date";

		public static String EMPLOYEE_ID = "employee_id";
	}

	interface TourAfterApprovalComments {
		public static String RESOURCE = "hris_tourrequests_after_approval_comments";

		public static String TOUR_ID = "tour_id";

		public static String REMARKS = "remarks";

		public static String DATE = "date";

		public static String EMPLOYEE_ID = "employeeid";

		public static String EMAIL_TO = "email_to";
	}

	interface TourAfterApprovalCommentsEmailTo {

		public static int HOD = 1;

		public static int EMPLOYEE = 2;

		public static int CASHIER = 3;
	}

}