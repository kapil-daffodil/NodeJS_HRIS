package com.applane.procurement;

public interface Procurement {

	String PROCUREMENT_EXPENSES = "procurement_expenses";
	String PROCUREMENT_RECURRING_EXPENSES = "procurement_recurring_expenses";
	String ROUND_OFF = "Round Off";

	public interface ProcurementExpenses {
		String STATUS_ID = "status_id";
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "officeid";
		String BUSINESS_FUNCTION_ID = "business_function_id";
		String VENDOR_ID = "vendor_id";
		String EXPENSE_DATE = "expensedate";
		String TITLE = "title";
		String DESCRIPTION = "description";
		String NONPAYABLE = "nonpayable";
		String PAYMENT_DATE = "paymentdate";
		String INVOICE_ID = "invoiceid";
		String PROVISION_VOUCHER_ID = "provision_voucher_id";
		String EXPENSE_TYPE = "expense_type";
		String EXPENSE_TYPE_ID = "expense_type_id";
		String UPDATE = "update";
		String PARENT_ASSET = "parent_asset";
		String FROM = "from";
		String TO = "to";
		String EXPENSE_MONTH = "expense_month";
		String IS_RECURRING_EXPANSE = "isrecurring_expanse";
		String UPDATE_EXPENSETYPE = "update_expensetype";
		String COSTCENTER_ID = "costcenterid";
		String NEW_BUSINESS_FUNCTION_ID = "newbusiness_function_id";
		String TEMP_COLUMN = "tempcolumn";
		String DELIVERY_ID = "deliveryid";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "convertedamount";
		String ADDON_AMOUNT = "addonamount";
		String ADDON_CONVERTED_AMOUNT = "addonconvertedamount";
		String TAKEOFF_AMOUNT = "takeoffamount";
		String TAKEOFF_CONVERTED_AMOUNT = "takeoffconvertedamount";
		String EXPENSE_AMOUNT = "expenseamount";
		String EXPENSE_CONVERTED_AMOUNT = "expenseconvertedamount";
		String PAYBLE_AMOUNT = "paybleamount";
		String PAYBLE_CONVERTED_AMOUNT = "paybleconvertedamount";
		String PENDING_AMOUNT = "pending_amount";
		String CONVERTED_PENDING_AMOUNT = "converted_pending_amount";
		String VOUCHER_DATE = "voucherdate";

		// String PARENT_EXPANSES = "parentexpanses";
		// extra columns in Recurring table
		public interface Recurring {
			String PROVISION_EXPENSE_ID = "provision_expenseid";
			String PROVISION_VOUCHER_ID = "provision_voucher_id";
			String REVERSE_PROVISION_VOUCHER_ID = "reverse_provision_voucher_id";
			String RECURRING_PERIOD = "recurring_period";

			public interface RecurringPeriodTypes {
				String YEARLY = "Yearly";
				String MONTHLY = "Monthly";
				String QUARTERLY = "Quarterly";
			}
		}
	}

	String PROCUREMENT_RECURRING_EXPENSES_MONTHLY_COST = "procurement_recurring_expenses_monthlycost";

	public interface ProcurementRecurringExpenseMonthlyCost {
		String EXPENSE_ID = "expense_id";
		String MONTH = "month";
		String AMOUNT = "amount";
	}

	String PROCUREMENT_ASSETS = "procurement_assets";

	public interface ProcurementAssets {
		String STATUS_ID = "status_id";
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "officeid";
		String BUSINESS_FUNCTION_ID = "businessfuctionid";
		String NEW_BUSINESS_FUNCTION_ID = "newbusiness_function_id";
		String VENDOR_ID = "vendor_id";
		String RECEIVE_DATE = "receive_date";
		String ID = "id";
		String DESCRIPTION = "description";
		String CURRENT_VALUE = "currentvalue";
		String PAYMENT_DATE = "paymentdate";
		String INVOICE_ID = "invoiceid";
		String ASSET_TYPE_ID = "assettype_id";
		String SALVAGE_VALUE = "salvage_value";
		String USEFULL_LIFE = "usefull_life";
		String NEXT_DEPRECIATION_DATE = "next_depreciation_date";
		String ASSET_OWNER = "asset_owner";
		String UPDATE = "update";
		String ASSET_CATEGORY_ID = "assetcategory_id";
		String WEAR_TEAR_START_DATE = "depreciation_start_date";
		String OPENING_BALANCE = "opening_balance";
		String VOUCHER_ID = "voucher_id";
		String CONVERSION_RATE = "conversionrate";
		String USED_DAYS = "used_days";
		String USEFULL_DAYS = "usefull_days";
		String DUMPED_DATE = "dumped_date";
		String COMMENT = "comment";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "convertedamount";
		String ADDON_AMOUNT = "addonamount";
		String ADDON_CONVERTED_AMOUNT = "addonconvertedamount";
		String TAKEOFF_AMOUNT = "takeoffamount";
		String TAKEOFF_CONVERTED_AMOUNT = "takeoffconvertedamount";
		String ASSET_AMOUNT = "assetamount";
		String ASSET_CONVERTED_AMOUNT = "assetconvertedamount";
		String PAYBLE_AMOUNT = "paybleamount";
		String PAYBLE_CONVERTED_AMOUNT = "paybleconvertedamount";
		String PENDING_AMOUNT = "pending_amount";
		String CONVERTED_PENDING_AMOUNT = "converted_pending_amount";
	}

	String PROCUREMENT_LOAN = "procurement_loan";

	public interface ProcurementLoan {
		String STATUS_ID = "status_id";
		String PROFITCENTER_ID = "profitcenterid";
		String OFFICE_ID = "officeid";
		String VENDOR_ID = "vendor_id";
		String LOANDATE = "loandate";
		String TITLE = "title";
		String PURPOSE = "purpose";
		String AMOUNT = "amount";
		String CONVERTED_PENDING_AMOUNT = "converted_pending_amount";
		String LOAN_TYPE_ID = "loantypeid";
		String PENDING_AMOUNT = "pending_amount";
		String CONVERSION_RATE = "conversionrate";
		String VOUCHER_ID = "voucher_id";
		String UPDATE = "update";
		String CONVERTED_AMOUNT = "convertedamount";
		String RECEIPT_CENTER = "receipt_center";
		String FINANCE_TYPE = "finance_type";
		String INTEREST_RATE = "interest_rate";
		String RECEIPT_CENTER_ACCOUNT_ID = "receipt_center.accountid";
		String INTEREST_ACCOUNT_ID = "interest_account_id";
		String REIMBURSE = "reimburse";
		String UPDATE_PROFITCENTER_ID = "update_profitcenter_id";
		String UPDATE_OFFICE_ID = "update_officeid";
	}

	String PROCUREMENT_LOAN_TYPES = "procurement_loantypes";

	public interface ProcurementLoantypes {
		String NAME = "name";
		String LOAN_ACCOUNT_ID = "loanaccountid";
	}

	String PROCUREMENT_STATUS = "procurement_status";

	public interface ProcurementStatus {
		String STATUS_TYPE = "status_type";

		public interface Types {
			String PENDING = "Pending";
			String PAID = "Paid";
			String NEW = "New";
			String SUBMITTED = "Submitted";
			String REJECTED = "Rejected";
		}

		public interface TypesKey {
			Integer PENDING = 1;
			Integer PAID = 2;
			Integer NEW = 3;
			Integer SUBMITTED = 4;
			Integer REJECTED = 5;
		}
	}

	String PROCUREMENT_ASSET_STATUS = "procurement_asset_status";

	public interface ProcurementAssetStatus {
		String STATUS_TYPE = "status_type";

		public interface Types {
			String PENDING = "Pending";
			String PAID = "Paid";
			String SOLD = "Sold";
			String DUMPED = "Dumped";
		}

		public interface TypesKey {
			Integer PENDING = 1;
			Integer PAID = 2;
			Integer SOLD = 3;
			Integer DUMPED = 4;
		}
	}

	String PROCUREMENT_VENDORINVOICES = "procurement_vendorinvoices";

	public interface ProcurementVendorinvoices {
		String INVOICE_TYPE = "invoice_type";
		String STATUS = "status";
		String VENDOR_ID = "vendorid";
		String INVOICE_NO = "invoiceno";
		String INVOICE_DATE = "invoicedate";
		String DUE_DATE = "duedate";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "convertedamout";
		String TAX_AMOUNT = "taxamount";
		String CONVERTED_TAX_AMOUNT = "convertedtaxamount";
		String PENDING_AMOUNT = "pendingamount";
		String CONVERSION_RATE = "conversionrate";
		String CONVERTED_PENDING_AMOUNT = "convertedpendingamount";
		String SUPPLEMENTARY_AMOUNT = "supplementaryamount";
		String CONVERTED_SUPPLEMENTARY_AMOUNT = "convertedsupplementaryamount";
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "officeid";
		String VOUCHER_ID = "voucher_id";
		String UPDATE = "update";
		String DEFAULT_CURRENCY = "defaultcurrency";
		String REFERENCE_NUMBER = "reference_number";
		String UPDATE_PROFITCENTER_ID = "update_profitcenter_id";
		String UPDATE_OFFICE_ID = "update_officeid";
		String ARREAR = "arrear";
		String INVOICE_MONTH = "invoicemonth";
		String PO_ID = "poid";
		// transient columns
		String EXPENSE_DATE = "expense_date";
		String EXPENSE_TITLE = "expense_title";
		String EXPENSE_BFID = "expense_bfid";
		String EXPENSE_EXPENSETYPE = "expense_expensetype";
		String EXPENSE_DESCRIPTION = "expense_description";
		String EXPENSE_SUPPTYPE = "expense_supptype";
	}

	String PROCUREMENT_VENDORINVOICELINEITEMS = "procurement_vendorinvoicelineitems";

	public interface ProcurementVendorinvoicelineitems {
		String VENDOR_INVOICE_ID = "vendorinvoiceid";
		String EXPENSE_ID = "exepenseid";
		String ASSET_ID = "assetid";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "convertedamount";
		String PROFITCENTER_ID = "profitcenter_id";
		// String UPDATE = "update";

	}

	String PROCUREMENT_INVOICETYPES = "procurement_invoicetypes";

	public interface ProcurementInvoiceTypes {
		String INVOICE_TYPE = "invoice_type";

		public interface Types {
			String EXPENSE = "Expense";
			String ASSET = "Asset";
			String SALAY = "Salary";
		}

		public interface TypesKey {
			Integer EXPENSE = 1;
			Integer ASSET = 2;
			Integer SALAY = 3;
		}
	}

	String PROCUREMENT_PAYMENTS = "procurement_payments";

	public interface ProcurementPayments {
		String TYPE = "type";
		String VENDOR_ID = "vendor_id";
		String PAYMENT_NO = "payment_no";
		String PAYMENT_DATE = "payment_date";
		String ACCOUNT_ID = "account_id";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "convertedamount";
		String TCS = "tcs";
		String SUPPLEMENTARY_AMOUNT = "supplementaryamount";
		String CONVERTED_SUPPLEMENTARY_AMOUNT = "convertedsupplementaryamount";
		String CONVERTED_TAX_AMOUNT = "convertedtcs";
		String PAYBLE_AMOUNT = "payble_amount";
		String CONVERSION_RATE = "conversionrate";
		String CONVERTED_PAYBLE_AMOUNT = "converted_payble_amount";
		String CURRENCY_FLUCTUATION = "currency_fluctuation";
		String TOTAL_INVOICE_RATE_AMOUNT = "total_invoice_rate_amount";
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "officeid";
		String DETAIL = "detail";
		String VOUCHER_ID = "voucher_id";
		String EXPENSE_VOUCHER_ID = "expense_voucher_id";
		String UPDATE = "update";
		String PAYMENT_MODE = "payment_mode";
		String DEFAULT_CURRENCY = "defaultcurrency";
		String PAYMENT_RECEIPTCENTER_ID = "payment_receiptcenter_id";
		String PAYMENT_OPTION_AMOUNT = "payment_option_amount";
		String UNADJUSTED_AMOUNT = "unadjustedamount";
		String UPDATE_PROFITCENTER_ID = "update_profitcenter_id";
		String UPDATE_OFFICE_ID = "update_officeid";
		String IS_ADVANCE_ADJUSTED = "isadvanceadjusted";
		String CANCEL_VOUCHER_ID1 = "cancelvoucherid1";
		String CANCEL_VOUCHER_ID2 = "cancelvoucherid2";
		String PAYMENT_MONTH = "paymentmonth";

	}

	String PROCUREMENT_PAYMENT_LINEITEMS = "procurement_payment_lineitems";

	public interface ProcurementPaymentLineItems {
		String PAYMENT_ID = "payment_id";
		String EXPENSE_ID = "expense_id";
		String ASSET_ID = "asset_id";
		String LOAN_ID = "loan_id";
		String INVOICE_ID = "invoice_id";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "converted_amount";
		String TCS = "tcs";
		String INVOICE_RATE_AMOUNT = "invoice_rate_amount";
		String PROFITCENTER_ID = "profitcenter_id";
		String CURRENCY_FLUCTUATION = "currency_fluctuation";
		String PRINCIPAL_AMOUNT = "principal_amount";
		String INTEREST_AMOUNT = "interest_amount";
		// String UPDATE = "update";

	}

	String PROCUREMENT_ADVANCE = "procurement_advance";

	public interface ProcurementAdvance {
		String PAYMENT_ID = "payment_id";
		String ADVANCE_ID = "advance_id";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "converted_amount";
		String ADVANCE_RATE_AMOUNT = "advance_rate_amount";
		String PROFITCENTER_ID = "profitcenter_id";
		String CURRENCY_FLUCTUATION = "currency_fluctuation";
	}

	String PROCUREMENT_PAYMENTTYPES = "procurement_paymenttypes";

	public interface ProcurementPaymenttypes {
		String PAYMENT_TYPE = "payment_type";

		public interface Types {
			String EXPENSE = "Expense";
			String ASSET = "Asset";
			String LOAN = "Loan";
			String INVOICE = "Invoice";
			String ADVANCE = "Advance";
		}

		public interface TypesKey {
			Integer EXPENSE = 1;
			Integer ASSET = 2;
			Integer LOAN = 3;
			Integer INVOICE = 4;
			Integer ADVANCE = 5;
		}
	}

	String PROCUREMENT_FUNDS_TRANSFER = "procurement_funds_transfer";

	public interface ProcurementFundsTransfer {
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "office_id";
		String DESCRIPTION = "description";
		String AMOUNT = "amount";
		String TRANSFER_DATE = "transfer_date";
		String UPDATE = "update";
		String VOUCHER_ID = "voucher_id";
		String CONVERSION_RATE = "conversionrate";
		String FROM_ACCOUNT = "from_account";
		String TO_ACCOUNT = "to_account";
		String FUNDS_TRANSFER_NO = "funds_transfer_no";
		String FROM_PROFITCENTER = "from_profitcenter";
		String FROM_OFFICE = "from_officeid";
		String SECOND_VOUCHERID = "voucherid";
		String SUPP_VOUCHERID = "supp_voucherid";

	}

	String BUSINESS_FUNCTIONS_ACCOUNTS = "businessfunctionsaccounts";

	public interface BusinessFunctionsAccounts {
		String BUSINESS_FUNCTION_ID = "businessfunctionid";
		String EXPANSE_ACCOUNT = "expanseaccount";
		String PROVISION_ACCOUNT = "provisionaccount";
	}

	String PROCUREMENT_ASSETTYPES = "procurement_assettypes";

	public interface ProcurementAssettype {
		String TYPE = "type";

		public interface Types {
			String CURRENT_ASSET = "Current Asset";
			String FIXED_ASSET = "Fixed Asset";
			String LOW_VALUE_ASSET = "Low Value Asset";
		}

		public interface TypesKey {
			Integer CURRENT_ASSET = 1;
			Integer FIXED_ASSET = 2;
			Integer LOW_VALUE_ASSET = 3;
		}
	}

	String PROCUREMENT_ASSET_CATEGORIES = "procurement_assetcategories";

	public interface ProcurementAssetcategories {
		String ASSET_CATEGORY = "asset_category";
		String ACCOUNT = "account";
		String PARENT_ASSET_CATEGORY = "parent_asset_category";
		String PERCENTAGE = "percentage";
		// String DEPRICATION_RULE_ID = "depreciation_rule_id";
	}

	String PROCUREMENT_PAYMENTMODES = "procurement_paymentmodes";

	public interface ProcurementPaymentmodes {
		String PAYMENT_MODE = "payment_mode";
	}

	String PROCUREMENT_DEPRICATION_RULES = "procurement_depricationrules";

	public interface ProcurementDepricationRules {
		String RULE = "rule";

		public interface Types {
			String STRAIGHT_LINE = "Straight Line";
			String REDUCING_BALANCE = "Reducing Balance";
		}
	}

	String DEPRECIATION = "Depreciation";
	String DEPRECIATION_RESERVE = "Depreciation Reserve";
	String BROUGHT_FORWARD = "Brought Forward";
	String PROCUREMENT_EXPENSETYPE = "procurement_expensetype";

	public interface ProcurementExpensetype {
		String EXPENSE_TYPE = "expense_type";
		String EXPANSE_ACCOUNT = "expanseaccount";
		String PROVISION_ACCOUNT = "provisionaccount";
		String SERVICE_TYPE = "service_type";
		String THRESHOLD_ONE_TIME = "one_time";
		String THRESHOLD_PER_YEAR = "one_year";
	}

	String PROCUREMENT_SALARY_DETAIL = "procurement_salary_detail";

	public interface ProcurementSalaryDetail {
		String INVOICE_ID = "invoice_id";
		String PROFITCENTER_ID = "profitcenter_id";
		String VENDOR_ID = "vendor_id";
		String OFFICE_ID = "office_id";
		String GROSS_SALARY = "gross_salary";
		String SALARY_DEDUCTIONS = "salary_deductions";// JSONArray which contains deductible salary components.
		String TDS = "";
		String PAYBLE_AMOUNT = "payble_amount";
		String SALARY_DATE = "salary_date";
		String SALARY_MONTH = "salary_month";
		String BANK_ACCOUNT = "bank_account";
		String ADVANCE = "advance";
		String TRANSFER_MODE = "transfer_mode";
	}

	String PROCUREMENT_SALARY_ADVISE = "procurement_salary_advise";

	public interface ProcurementSalaryAdvise {
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "office_id";
		String OFFICE_ID_FOR_RECORDS = "office_id_for_records";
		String DATE = "date";
		String RECEIPTCENTER_ID = "receiptcenter_id";
		String SALARY_MONTH = "salary_month";
		String YEAR = "year";
		String CHEQUE_DATE = "chequedate";
		String CHEQUE_NO = "chequeno";
		String VOUCHERID = "voucherid";
		String ISUPDATEINTERNAL = "isupdateinternal";
		String PAYMENT_MODE = "paymentmode";
		String SALARY_TYPE = "salarytype";
		String IS_CREATE_VOUCHER = "is_create_voucher";

		public interface SalaryTypes {
			String BOTH = "Both";
			String SALARY_RELEASED = "Salary Released";
			String ARREARS_RELEASED = "Arrears Released";
		}

		public interface SalaryTransferType {
			int ACCOUNT_TRANSFER = 1;
			int CHEQUE_TRANSFER = 2;
		}
	}

	String PROCUREMENT_SALARY_ADVISE_LINEITEMS = "procurement_salary_advise_lineitems";

	public interface ProcurementSalaryAdviseLineItems {
		String SALARY_ADVISE_ID = "salary_advise_id";
		String INVOICE_ID = "invoice_id";
		String PROFITCENTER_ID = "profitcenter_id";
		String VENDOR_ID = "vendor_id";
		String OFFICE_ID = "office_id";
		String GROSS_SALARY = "gross_salary";
		String TDS = "tds";
		String PAYBLE_AMOUNT = "payble_amount";
		String SALARY_DATE = "salary_date";
		String BANK_ACCOUNT = "bank_account";
		String ADVANCE = "advance";
		String CHEQUE_DATE = "chequedate";
		String CHEQUE_NO = "cheque_no";
		String PAYMENT_DATE = "paymentdate";
		String IS_ON_HOLD = "is_on_hold";
	}

	String RE_RELEASE = "re_release";
	String Arrear = "arrear";
	String PROCUREMENT_SALARY_DEDUCTION = "procurement_salary_deduction";
	String PROCUREMENT_SALARY_ADVISE_DEDUCTION_LINEITEMS = "procurement_salary_advise_deduction_lineitems";

	// Both resources share same columns name.
	public interface ProcurementSalaryDeduction {
		String PROFITCENTER_ID = "profitcenter_id";
		String SALARY_ADVISE_ID = "salary_advise_id";
		String CONVERTED_AMOUNT = "converted_amount";
		String VOUCHER_AMOUNT = "voucher_amount";
		String PERCENTAGE = "percentage";
		String SUPPLEMENTARY_VENDOR_INVOICE_ID = "supplementary_vendorinvoiceid";
		String SUPPLEMENTARY_TRANSECTION_TYPE_ID = "supplementarytransectiontypeid";
		String AMOUNT = "amount";
		String ADVANCE_VOUCHERID = "advance_voucherid";

		public interface DeductionColumns {
			String NAME = "name";
			String NAME_ID = "name_id";
			String AMOUNT = "amount";
		}
	}

	String PROCUREMENT_PROVISION_EXPENSES = "procurement_provision_expenses";

	public interface ProcurementProvisionExpenses {
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "office_id";
		String RECEIPTCENTER_ID = "receiptcenter_id";
		String PROVISION_MONTH = "provision_month";
		String PROVISION_YEAR = "provision_year";
	}

	String PROCUREMENT_REPORTCOLUMNS = "procurement_reportcolumns";

	public interface ProcurementReportColumns {
		String NAME = "name";

		public interface Types {
			String VAT = "VAT";
			Integer VAT_ID = 1;
			String SURCHARGE_ON_VAT = "Surcharge On VAT";
			Integer SURCHARGE_ON_VAT_ID = 2;
			String TDS = "TDS";
			Integer TDS_ID = 3;
			String SERVICE_TAX_CENVAT = "Service Tax Cenvat";
			Integer SERVICE_TAX_CENVAT_ID = 4;
		}
	}

	String SERVICE_TAX_CENVAT = "servicetax_cenvat";

	public interface ServiceTaxCenvat {
		String DATE = "date";
		String MONTH = "month";
		String YEAR = "year";
		String SERVICE_TAX_OPENING_BALANCE = "servicetax_openingbalance";
		String CURRENT_SERVICE_TAX = "current_servicetax";
		String TOTAL_SERVICE_TAX = "total_servicetax";
		String UTILIZE_SERVICE_TAX = "utilize_servicetax";
		String BALANCE_SERVICE_TAX = "balance_servicetax";
		String ADDITIONAL_TAXES_OPENING_BALANCE = "additional_taxes_openingbalance";
		String EDUCATION_CESS = "education_cess";
		String HIGHER_EDUCATION_CESS = "higher_education_cess";
		String TOTAL_ADDITIONAL_TAXES = "total_additional_taxes";
		String UTILIZE_ADDITIONAL_TAXES = "utilize_additionaltaxes";
		String BALANCE_ADDITIONAL_TAXES = "balance_additionaltaxes";
	}

	String PROCUREMENT_FD = "procurement_fd";

	public interface ProcurementFd {
		String STATUS_ID = "status_id";
		String PROFITCENTER_ID = "profitcenterid";
		String OFFICE_ID = "officeid";
		String CUSTOMER_ID = "customer_id";
		String DATE = "date";
		String FDNO = "fdno";
		String LOAN_NO = "loan_no";
		String PURPOSE = "purpose";
		String AMOUNT = "amount";
		String FINANCE_TYPE = "finance_type";
		String FD_TYPE = "fd_type";
		String DURATION = "duration";
		String INTEREST_RATE = "interest_rate";
		String RECEIPT_CENTER = "receipt_center";
		String MATURITY_AMOUNT = "maturity_amount";
		String PENDING_AMOUNT = "pending_amount";
		String CONVERSION_RATE = "conversionrate";
		String VOUCHER_ID = "voucher_id";
		String UPDATE = "update";
		String INTEREST_ACCOUNT_ID = "interest_account_id";

		public interface StatusType {
			String MATURED = "Matured";
			String NEW = "New";
		}
	}

	String PROCUREMENT_FD_RECEIPT = "procurement_fd_receipt";

	public interface ProcurementFdReceipt {
		String RECEIPT_MONTH = "receiptmonth";
		String CONVERTED_AMOUNT = "converted_amount";
		String CONVERTED_RECEIVED_AMOUNT = "converted_received_amount";
		String CONVERSION_RATE = "conversion_rate";
		String BUSINESS_PARTNER_ID = "business_partner_id";
		String RECEIPT_NO = "receipt_no";
		String RECEIPT_DATE = "receipt_date";
		String PROFITCENTER_ID = "profitcenter_id";
		String AMOUNT = "amount";
		String VOUCHER_ID = "voucher_id";
		String OFFICE_ID = "officeid";
		String RECEIVED = "received";
		String OFFICEID = "officeid";
		String DEFAULT_CURRENCY = "defaultcurrency";
		String RECEIVED_AMOUNT = "received_amount";
		String UPDATE = "update";
		String SUPPLEMENTARYAMOUNT = "supplementaryamount";
		String CONVERTEDSUPPLEMENTARYAMOUNT = "convertedsupplementaryamount";
		String PENDINGAMOUNT = "pendingamount";
		String CONVERTEDPENDINGAMOUNT = "convertedpendingamount";
	}

	String PROCUREMENT_FD_RECEIPTLINEITEMS = "procurement_fd_receiptlineitems";

	public interface ProcurementFdReceiptLineItems {
		String AMOUNT = "amount";
		String INTEREST_AMOUNT = "interest_amount";
		String FD_ID = "fd_id";
		String FD_RECEIPT_ID = "fdreceiptid";
	}

	String PROCUREMENT_BULKREIMBURSEMENT_PAYMENTLINEITEMS = "procurement_bulkreimbursement_paymentlineitems";

	public interface Procurement_BulkReimbursement_PaymentLineItems {
		String EXPENSEID = "expenseid";
		String REIMBURSEMENTID = "bulkreimbursementid";
		String AMOUNT = "amount";
		String ADVANCE_AMOUNT = "advance_amount";
		String CONVERTED_AMOUNT = "converted_amount";
	}

	String BULKREIMBURSEMENT_PAYMENTLINEITEMS_INVOICES = "bulkreimbursement_paymentlineitems_invoices";

	public interface BulkReimbursementPaymentLineItemsInvoices {
		String REIMBURSEMENTID = "bulkreimbursementid";
		String VENDOR_INVOICE_ID = "vendorinvoiceid";
		String VENDOR_ID = "vendorid";
		String AMOUNT = "amount";
	}

	String PROCUREMENT_BULKREIMBURSEMENT_PAYMENT = "procurement_bulkreimbursentpayment";

	public interface Procurement_BulkReimbursement_Payment {
		String PAYMENT_DATE = "date";
		String TOTAL_AMOUNT = "total_amount";
		String TOTAL_CONVERTED_AMOUNT = "total_converted_amount";
		String DEFAULT_CURRENCY = "defaultcurrency";
		String CONVERSION_RATE = "conversionrate";
		String PROFIT_CENTER = "profitcenterid";
		String OFFICEID = "officeid";
		String RECEIPT_CENTER = "recieptcenter";
		String PAYMENTNO = "paymentno";
		String VOUCHERID = "voucherid";
		String SECOND_VOUCHERID = "second_voucherid";
		String ISUPDATE_INTERNAL = "isupdateinternal";
		String UPDATE_PROFITCENTER_ID = "update_profitcenter_id";
		String UPDATE_OFFICE_ID = "update_officeid";
	}

	String PROCUREMENT_ADVANCE_RECEIPT = "procurement_advancereceipt";

	public interface Procurement_AdvanceReceipt {
		String PROFITCENTER = "profitcenterid";
		String LOCATION = "officeid";
		String BUSINESS_PARTNER = "business_partnerid";
		String RECEIPT_NO = "receiptid";
		String RECEIPT_DATE = "receiptdate";
		String DEFAULT_CURRENCY = "defaultcurrency";
		String CONVERSION_RATE = "conversion_rate";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "converted_amount";
		String VOUCHERID = "voucherid";
		String PAID_AMOUNT = "paidamount";

	}

	String PROCUREMENT_ADVANCE_RECEIPT_LINEITEMS = "procurement_advancereceipt_lineitems";

	public interface Procurement_Advance_Receipt_Lineitems {
		String ADVANCE_RECEIPT_ID = "advancereceiptid";
		String PAYMENT_ID = "paymentid";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "converted_amount";

	}

	String PROCUREMENT_VENDOR_EXPENSE_TYPES = "procurement_vendor_expense_types";

	public interface Procurement_Vendor_Expense_Types {
		String VENDOR_ID = "vendor_id";
		String EXPENSE_TYPE = "expense_type";
	}

	String PROCUREMENT_VENDOR_ADVISE = "procurement_vendor_advise";

	public interface ProcurementVendorAdvise {
		String DATE = "date";
		String RECEIPTCENTER_ID = "receiptcenter_id";
		String RECEIPTCENTER_ACCOUNT_ID = "receiptcenteraccountid";
		String PAY_FOR_ONE_VENDOR = "payforonevendor";
		String PAYMENT_MODE = "paymentmode";
		String CHEQUE_NO = "cheque_no";
		String CHEQUE_DATE = "chequedate";
		String VALIDATE_CHEQUE = "validatecheque";
		String VOUCHER_ID = "voucherid";
	}

	String PROCUREMENT_VENDOR_ADVISE_LINEITEMS = "procurement_vendor_advise_lineitems";

	public interface ProcurementVendorAdviseLineItems {
		String VENDOR_ADVISE_ID = "vendor_advise_id";
		String VENDOR_ID = "vendor_id";
		String INVOICE_ID = "invoice_id";
		String PAYBLE_AMOUNT = "payble_amount";
		String PAYMENT_MODE = "paymentmode";
		String CHEQUE_NO = "cheque_no";
		String CHEQUE_DATE = "chequedate";
		String PROFITCENTER_ID = "profitcenter_id";
		String OFFICE_ID = "office_id";
		String VALIDATE_CHEQUE = "validatecheque";
		String VOUCHER_ID = "voucherid";
	}

	String PROCUREMENT_ASSET_DEPRECIATION_DETAILS = "procurement_assetdepreciationdetails";

	public interface ProcurementAssetDepreciationDetails {
		String ASSET_ID = "assetid";
		String AMOUNT = "amount";
		String CONVERTED_AMOUNT = "convertedamount";
		String DATE = "date";
		String VOUCHER_ID = "voucherid";
	}
}
