package com.applane.resourceoriented.hris.reports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.UnderlineStyle;
import jxl.format.VerticalAlignment;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.applane.databaseengine.ApplaneDatabaseEngineIMPL;
import com.applane.databaseengine.shared.constants.Data;
import com.applane.databaseengine.utils.CurrentState;
import com.applane.resourceoriented.hris.HRISApplicationConstants;
import com.applane.ui.browser.shared.constants.CurrentSession;

public class EmployeeMonthlyAttendanceReport {
	public static void generateReport(ByteArrayOutputStream outputStream, JSONObject parameterObject) throws RowsExceededException, WriteException, JSONException, IOException {
		JSONObject query = new JSONObject();
		query.put(Data.Query.RESOURCE, "hris_employees");
		JSONArray columns = new JSONArray();
		columns.put("employeecode");
		columns.put("name");
		columns.put("departmentid.name");
		columns.put("branchid.name");
		query.put(Data.Query.COLUMNS, columns);
		query.put(Data.Query.ORDERS, new JSONArray().put(new JSONObject().put(Data.Query.Orders.EXPERSSION, "name").put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC)));
		query.put(Data.Query.PARAMETERS, parameterObject);
		StringBuffer filter = new StringBuffer();
		filter.append("employeestatusid = " + HRISApplicationConstants.EMPLOYEE_ACTIVE);
		if (!parameterObject.isNull("departmentid")) {
			filter.append(" AND departmentid={departmentid}");
		}
		if (!parameterObject.isNull("branchid")) {
			filter.append(" AND branchid={branchid}");
		}
		query.put(Data.Query.FILTERS, filter);
		query.put(Data.Query.OPTION_FILTERS, "employeestatusid = " + HRISApplicationConstants.EMPLOYEE_INACTIVE + " AND relievingdate>={fromdate}");
		JSONObject employeeAttendanceChild = new JSONObject();
		employeeAttendanceChild.put(Data.Query.Childs.RELATED_COLUMN, "employeeid");
		employeeAttendanceChild.put(Data.Query.Childs.ALIAS, "employeeattendance");
		JSONArray employeeAttendanceColumns = new JSONArray();
		employeeAttendanceColumns.put("attendancetypeid.name");
		employeeAttendanceColumns.put("attendancedate");
		JSONObject childQuery = new JSONObject();
		JSONArray childOrders = new JSONArray();
		childOrders.put(new JSONObject().put(Data.Query.Orders.EXPERSSION, "attendancedate").put(Data.Query.Orders.TYPE, Data.Query.Orders.OrderType.ASC));
		childQuery.put(Data.Query.ORDERS, childOrders);
		childQuery.put(Data.Query.RESOURCE, "employeeattendance");
		childQuery.put(Data.Query.COLUMNS, employeeAttendanceColumns);
		childQuery.put(Data.Query.FILTERS, "attendancedate>={fromdate} AND attendancedate<={todate}");
		employeeAttendanceChild.put(Data.Query.Childs.QUERY, childQuery);
		query.put(Data.Query.CHILDS, new JSONArray().put(employeeAttendanceChild));

		JSONArray resultData = ApplaneDatabaseEngineIMPL.getApplaneDatabaseEngine().query(query).getJSONArray("hris_employees");
		// JSONArray resultData = new JSONArray(
		// "[{'__key__':160,'branchid.name':'Suncity','employeeattendance':[{'__key__':130377,'employeeid':160,'attendancedate':'2013-06-01','attendancetypeid.name':'Off'},{'__key__':130603,'employeeid':160,'attendancedate':'2013-06-02','attendancetypeid.name':'Off'},{'__key__':130934,'employeeid':160,'attendancedate':'2013-06-03','attendancetypeid.name':'Tour'},{'__key__':131186,'employeeid':160,'attendancedate':'2013-06-04','attendancetypeid.name':'Tour'},{'__key__':131436,'employeeid':160,'attendancedate':'2013-06-05','attendancetypeid.name':'Tour'},{'__key__':131675,'employeeid':160,'attendancedate':'2013-06-06','attendancetypeid.name':'Tour'},{'__key__':131931,'employeeid':160,'attendancedate':'2013-06-07','attendancetypeid.name':'Tour'},{'__key__':132091,'employeeid':160,'attendancedate':'2013-06-08','attendancetypeid.name':'Off'},{'__key__':132331,'employeeid':160,'attendancedate':'2013-06-09','attendancetypeid.name':'Off'},{'__key__':132660,'employeeid':160,'attendancedate':'2013-06-10','attendancetypeid.name':'Tour'},{'__key__':132913,'employeeid':160,'attendancedate':'2013-06-11','attendancetypeid.name':'Tour'},{'__key__':133171,'employeeid':160,'attendancedate':'2013-06-12','attendancetypeid.name':'Tour'},{'__key__':133417,'employeeid':160,'attendancedate':'2013-06-13','attendancetypeid.name':'Tour'},{'__key__':133663,'employeeid':160,'attendancedate':'2013-06-14','attendancetypeid.name':'Tour'},{'__key__':133845,'employeeid':160,'attendancedate':'2013-06-15','attendancetypeid.name':'Off'},{'__key__':134073,'employeeid':160,'attendancedate':'2013-06-16','attendancetypeid.name':'Off'},{'__key__':137945,'employeeid':160,'attendancedate':'2013-06-17','attendancetypeid.name':'Tour'},{'__key__':134620,'employeeid':160,'attendancedate':'2013-06-18','attendancetypeid.name':'Tour'},{'__key__':134866,'employeeid':160,'attendancedate':'2013-06-19','attendancetypeid.name':'Tour'},{'__key__':135040,'employeeid':160,'attendancedate':'2013-06-20','attendancetypeid.name':'Tour'},{'__key__':135366,'employeeid':160,'attendancedate':'2013-06-21','attendancetypeid.name':'Tour'},{'__key__':135533,'employeeid':160,'attendancedate':'2013-06-22','attendancetypeid.name':'Off'},{'__key__':135781,'employeeid':160,'attendancedate':'2013-06-23','attendancetypeid.name':'Off'},{'__key__':136120,'employeeid':160,'attendancedate':'2013-06-24','attendancetypeid.name':'Tour'},{'__key__':136383,'employeeid':160,'attendancedate':'2013-06-25','attendancetypeid.name':'Tour'},{'__key__':136686,'employeeid':160,'attendancedate':'2013-06-26','attendancetypeid.name':'Tour'},{'__key__':137010,'employeeid':160,'attendancedate':'2013-06-27','attendancetypeid.name':'Tour'},{'__key__':137283,'employeeid':160,'attendancedate':'2013-06-28','attendancetypeid.name':'Tour'},{'__key__':137468,'employeeid':160,'attendancedate':'2013-06-29','attendancetypeid.name':'Off'},{'__key__':137709,'employeeid':160,'attendancedate':'2013-06-30','attendancetypeid.name':'Off'}],'name':'A. Venkat  Raman','departmentid.name':'Marketing','employeecode':'DFG-1280'},{'__key__':174,'branchid.name':'Silokhera SEZ','employeeattendance':[{'__key__':130382,'employeeid':174,'attendancedate':'2013-06-01','attendancetypeid.name':'Off'},{'__key__':130610,'employeeid':174,'attendancedate':'2013-06-02','attendancetypeid.name':'Off'},{'__key__':130937,'employeeid':174,'attendancedate':'2013-06-03','attendancetypeid.name':'Present'},{'__key__':131189,'employeeid':174,'attendancedate':'2013-06-04','attendancetypeid.name':'Present'},{'__key__':131438,'employeeid':174,'attendancedate':'2013-06-05','attendancetypeid.name':'Present'},{'__key__':131678,'employeeid':174,'attendancedate':'2013-06-06','attendancetypeid.name':'Present'},{'__key__':131933,'employeeid':174,'attendancedate':'2013-06-07','attendancetypeid.name':'Present'},{'__key__':132098,'employeeid':174,'attendancedate':'2013-06-08','attendancetypeid.name':'Off'},{'__key__':132338,'employeeid':174,'attendancedate':'2013-06-09','attendancetypeid.name':'Off'},{'__key__':132662,'employeeid':174,'attendancedate':'2013-06-10','attendancetypeid.name':'Present'},{'__key__':132915,'employeeid':174,'attendancedate':'2013-06-11','attendancetypeid.name':'Present'},{'__key__':133173,'employeeid':174,'attendancedate':'2013-06-12','attendancetypeid.name':'Present'},{'__key__':133419,'employeeid':174,'attendancedate':'2013-06-13','attendancetypeid.name':'Present'},{'__key__':133666,'employeeid':174,'attendancedate':'2013-06-14','attendancetypeid.name':'Present'},{'__key__':133850,'employeeid':174,'attendancedate':'2013-06-15','attendancetypeid.name':'Off'},{'__key__':134080,'employeeid':174,'attendancedate':'2013-06-16','attendancetypeid.name':'Off'},{'__key__':139227,'employeeid':174,'attendancedate':'2013-06-17','attendancetypeid.name':'Present'},{'__key__':134622,'employeeid':174,'attendancedate':'2013-06-18','attendancetypeid.name':'Present'},{'__key__':134869,'employeeid':174,'attendancedate':'2013-06-19','attendancetypeid.name':'Present'},{'__key__':135045,'employeeid':174,'attendancedate':'2013-06-20','attendancetypeid.name':'Present'},{'__key__':135368,'employeeid':174,'attendancedate':'2013-06-21','attendancetypeid.name':'Present'},{'__key__':135539,'employeeid':174,'attendancedate':'2013-06-22','attendancetypeid.name':'Off'},{'__key__':135788,'employeeid':174,'attendancedate':'2013-06-23','attendancetypeid.name':'Off'},{'__key__':136123,'employeeid':174,'attendancedate':'2013-06-24','attendancetypeid.name':'Present'},{'__key__':136385,'employeeid':174,'attendancedate':'2013-06-25','attendancetypeid.name':'Present'},{'__key__':136688,'employeeid':174,'attendancedate':'2013-06-26','attendancetypeid.name':'Present'},{'__key__':137013,'employeeid':174,'attendancedate':'2013-06-27','attendancetypeid.name':'Present'},{'__key__':137285,'employeeid':174,'attendancedate':'2013-06-28','attendancetypeid.name':'Present'},{'__key__':137473,'employeeid':174,'attendancedate':'2013-06-29','attendancetypeid.name':'Off'},{'__key__':137716,'employeeid':174,'attendancedate':'2013-06-30','attendancetypeid.name':'Off'}],'name':'Abhijeet  Sharma','departmentid.name':'Technical','employeecode':'DFG-1285'},{'__key__':307,'branchid.name':'Sector-33','employeeattendance':[{'__key__':130458,'employeeid':307,'attendancedate':'2013-06-01','attendancetypeid.name':'Off'},{'__key__':130699,'employeeid':307,'attendancedate':'2013-06-02','attendancetypeid.name':'Off'},{'__key__':130799,'employeeid':307,'attendancedate':'2013-06-03','attendancetypeid.name':'Present'},{'__key__':131120,'employeeid':307,'attendancedate':'2013-06-04','attendancetypeid.name':'Present'},{'__key__':131345,'employeeid':307,'attendancedate':'2013-06-05','attendancetypeid.name':'Present'},{'__key__':131593,'employeeid':307,'attendancedate':'2013-06-06','attendancetypeid.name':'Present'},{'__key__':131850,'employeeid':307,'attendancedate':'2013-06-07','attendancetypeid.name':'Present'},{'__key__':132182,'employeeid':307,'attendancedate':'2013-06-08','attendancetypeid.name':'Off'},{'__key__':132426,'employeeid':307,'attendancedate':'2013-06-09','attendancetypeid.name':'Off'},{'__key__':132573,'employeeid':307,'attendancedate':'2013-06-10','attendancetypeid.name':'Present'},{'__key__':132773,'employeeid':307,'attendancedate':'2013-06-11','attendancetypeid.name':'Second Half Day Leave'},{'__key__':133051,'employeeid':307,'attendancedate':'2013-06-12','attendancetypeid.name':'Present'},{'__key__':133283,'employeeid':307,'attendancedate':'2013-06-13','attendancetypeid.name':'Present'},{'__key__':133606,'employeeid':307,'attendancedate':'2013-06-14','attendancetypeid.name':'Present'},{'__key__':133926,'employeeid':307,'attendancedate':'2013-06-15','attendancetypeid.name':'Off'},{'__key__':134168,'employeeid':307,'attendancedate':'2013-06-16','attendancetypeid.name':'Off'},{'__key__':138822,'employeeid':307,'attendancedate':'2013-06-17','attendancetypeid.name':'Full Day Leave'},{'__key__':134543,'employeeid':307,'attendancedate':'2013-06-18','attendancetypeid.name':'Present'},{'__key__':134762,'employeeid':307,'attendancedate':'2013-06-19','attendancetypeid.name':'Present'},{'__key__':135122,'employeeid':307,'attendancedate':'2013-06-20','attendancetypeid.name':'Present'},{'__key__':135293,'employeeid':307,'attendancedate':'2013-06-21','attendancetypeid.name':'Present'},{'__key__':135620,'employeeid':307,'attendancedate':'2013-06-22','attendancetypeid.name':'EWD-Full'},{'__key__':135872,'employeeid':307,'attendancedate':'2013-06-23','attendancetypeid.name':'Off'},{'__key__':136036,'employeeid':307,'attendancedate':'2013-06-24','attendancetypeid.name':'Present'},{'__key__':136325,'employeeid':307,'attendancedate':'2013-06-25','attendancetypeid.name':'Present'},{'__key__':136590,'employeeid':307,'attendancedate':'2013-06-26','attendancetypeid.name':'Present'},{'__key__':136933,'employeeid':307,'attendancedate':'2013-06-27','attendancetypeid.name':'Present'},{'__key__':137223,'employeeid':307,'attendancedate':'2013-06-28','attendancetypeid.name':'Present'},{'__key__':137546,'employeeid':307,'attendancedate':'2013-06-29','attendancetypeid.name':'EWD-Full'},{'__key__':137800,'employeeid':307,'attendancedate':'2013-06-30','attendancetypeid.name':'Off'}],'name':'Abhinav Maheshwari','departmentid.name':'Technical','employeecode':'DFG- 1444'},{'__key__':234,'branchid.name':'Sector-33','employeeattendance':[{'__key__':130416,'employeeid':234,'attendancedate':'2013-06-01','attendancetypeid.name':'Off'},{'__key__':130649,'employeeid':234,'attendancedate':'2013-06-02','attendancetypeid.name':'Off'},{'__key__':130877,'employeeid':234,'attendancedate':'2013-06-03','attendancetypeid.name':'Present'},{'__key__':131138,'employeeid':234,'attendancedate':'2013-06-04','attendancetypeid.name':'Present'},{'__key__':131360,'employeeid':234,'attendancedate':'2013-06-05','attendancetypeid.name':'Present'},{'__key__':131605,'employeeid':234,'attendancedate':'2013-06-06','attendancetypeid.name':'Present'},{'__key__':131863,'employeeid':234,'attendancedate':'2013-06-07','attendancetypeid.name':'Present'},{'__key__':132136,'employeeid':234,'attendancedate':'2013-06-08','attendancetypeid.name':'Off'},{'__key__':132376,'employeeid':234,'attendancedate':'2013-06-09','attendancetypeid.name':'Off'},{'__key__':132582,'employeeid':234,'attendancedate':'2013-06-10','attendancetypeid.name':'Present'},{'__key__':132778,'employeeid':234,'attendancedate':'2013-06-11','attendancetypeid.name':'Present'},{'__key__':133057,'employeeid':234,'attendancedate':'2013-06-12','attendancetypeid.name':'Present'},{'__key__':133289,'employeeid':234,'attendancedate':'2013-06-13','attendancetypeid.name':'Present'},{'__key__':133626,'employeeid':234,'attendancedate':'2013-06-14','attendancetypeid.name':'Present'},{'__key__':133884,'employeeid':234,'attendancedate':'2013-06-15','attendancetypeid.name':'EWD-Full'},{'__key__':134118,'employeeid':234,'attendancedate':'2013-06-16','attendancetypeid.name':'Off'},{'__key__':134356,'employeeid':234,'attendancedate':'2013-06-17','attendancetypeid.name':'Present'},{'__key__':134557,'employeeid':234,'attendancedate':'2013-06-18','attendancetypeid.name':'Present'},{'__key__':134771,'employeeid':234,'attendancedate':'2013-06-19','attendancetypeid.name':'Present'},{'__key__':135075,'employeeid':234,'attendancedate':'2013-06-20','attendancetypeid.name':'Present'},{'__key__':135308,'employeeid':234,'attendancedate':'2013-06-21','attendancetypeid.name':'Present'},{'__key__':135572,'employeeid':234,'attendancedate':'2013-06-22','attendancetypeid.name':'Off'},{'__key__':135822,'employeeid':234,'attendancedate':'2013-06-23','attendancetypeid.name':'Off'},{'__key__':136052,'employeeid':234,'attendancedate':'2013-06-24','attendancetypeid.name':'Present'},{'__key__':136264,'employeeid':234,'attendancedate':'2013-06-25','attendancetypeid.name':'Present'},{'__key__':136519,'employeeid':234,'attendancedate':'2013-06-26','attendancetypeid.name':'Present'},{'__key__':136960,'employeeid':234,'attendancedate':'2013-06-27','attendancetypeid.name':'Present'},{'__key__':137247,'employeeid':234,'attendancedate':'2013-06-28','attendancetypeid.name':'Present'},{'__key__':137502,'employeeid':234,'attendancedate':'2013-06-29','attendancetypeid.name':'Off'},{'__key__':137750,'employeeid':234,'attendancedate':'2013-06-30','attendancetypeid.name':'Off'}],'name':'Abhishek   Gupta','departmentid.name':'Technical','employeecode':'DFG-1391'},{'__key__':237,'branchid.name':'Sector-33','employeeattendance':[{'__key__':130417,'employeeid':237,'attendancedate':'2013-06-01','attendancetypeid.name':'Absent'},{'__key__':130651,'employeeid':237,'attendancedate':'2013-06-02','attendancetypeid.name':'Absent'},{'__key__':130948,'employeeid':237,'attendancedate':'2013-06-03','attendancetypeid.name':'Absent'},{'__key__':131200,'employeeid':237,'attendancedate':'2013-06-04','attendancetypeid.name':'Absent'},{'__key__':131449,'employeeid':237,'attendancedate':'2013-06-05','attendancetypeid.name':'Absent'},{'__key__':131693,'employeeid':237,'attendancedate':'2013-06-06','attendancetypeid.name':'Absent'},{'__key__':131946,'employeeid':237,'attendancedate':'2013-06-07','attendancetypeid.name':'Absent'},{'__key__':132137,'employeeid':237,'attendancedate':'2013-06-08','attendancetypeid.name':'Absent'},{'__key__':132378,'employeeid':237,'attendancedate':'2013-06-09','attendancetypeid.name':'Absent'},{'__key__':132677,'employeeid':237,'attendancedate':'2013-06-10','attendancetypeid.name':'Absent'},{'__key__':132927,'employeeid':237,'attendancedate':'2013-06-11','attendancetypeid.name':'Absent'},{'__key__':133183,'employeeid':237,'attendancedate':'2013-06-12','attendancetypeid.name':'Absent'},{'__key__':133429,'employeeid':237,'attendancedate':'2013-06-13','attendancetypeid.name':'Absent'},{'__key__':133678,'employeeid':237,'attendancedate':'2013-06-14','attendancetypeid.name':'Absent'},{'__key__':133885,'employeeid':237,'attendancedate':'2013-06-15','attendancetypeid.name':'Off'},{'__key__':134120,'employeeid':237,'attendancedate':'2013-06-16','attendancetypeid.name':'Off'},{'__key__':139234,'employeeid':237,'attendancedate':'2013-06-17','attendancetypeid.name':'Unknown'},{'__key__':134632,'employeeid':237,'attendancedate':'2013-06-18','attendancetypeid.name':'Absent'},{'__key__':134875,'employeeid':237,'attendancedate':'2013-06-19','attendancetypeid.name':'Absent'},{'__key__':135077,'employeeid':237,'attendancedate':'2013-06-20','attendancetypeid.name':'Absent'},{'__key__':135375,'employeeid':237,'attendancedate':'2013-06-21','attendancetypeid.name':'Absent'},{'__key__':135573,'employeeid':237,'attendancedate':'2013-06-22','attendancetypeid.name':'Absent'},{'__key__':135824,'employeeid':237,'attendancedate':'2013-06-23','attendancetypeid.name':'Absent'},{'__key__':136136,'employeeid':237,'attendancedate':'2013-06-24','attendancetypeid.name':'Absent'},{'__key__':136393,'employeeid':237,'attendancedate':'2013-06-25','attendancetypeid.name':'Absent'},{'__key__':136694,'employeeid':237,'attendancedate':'2013-06-26','attendancetypeid.name':'Absent'},{'__key__':137020,'employeeid':237,'attendancedate':'2013-06-27','attendancetypeid.name':'Absent'},{'__key__':137291,'employeeid':237,'attendancedate':'2013-06-28','attendancetypeid.name':'Absent'},{'__key__':137503,'employeeid':237,'attendancedate':'2013-06-29','attendancetypeid.name':'Absent'},{'__key__':137752,'employeeid':237,'attendancedate':'2013-06-30','attendancetypeid.name':'Absent'}],'name':'Abhishek  Saini','departmentid.name':'Technical','employeecode':'DFG-1394'},{'__key__':372,'branchid.name':'Silokhera SEZ','employeeattendance':[{'__key__':130501,'employeeid':372,'attendancedate':'2013-06-01','attendancetypeid.name':'Off'},{'__key__':130746,'employeeid':372,'attendancedate':'2013-06-02','attendancetypeid.name':'Off'},{'__key__':130797,'employeeid':372,'attendancedate':'2013-06-03','attendancetypeid.name':'Present'},{'__key__':131111,'employeeid':372,'attendancedate':'2013-06-04','attendancetypeid.name':'Present'},{'__key__':131337,'employeeid':372,'attendancedate':'2013-06-05','attendancetypeid.name':'Present'},{'__key__':131586,'employeeid':372,'attendancedate':'2013-06-06','attendancetypeid.name':'Present'},{'__key__':131843,'employeeid':372,'attendancedate':'2013-06-07','attendancetypeid.name':'Present'},{'__key__':132227,'employeeid':372,'attendancedate':'2013-06-08','attendancetypeid.name':'Off'},{'__key__':132472,'employeeid':372,'attendancedate':'2013-06-09','attendancetypeid.name':'Off'},{'__key__':132566,'employeeid':372,'attendancedate':'2013-06-10','attendancetypeid.name':'Present'},{'__key__':132769,'employeeid':372,'attendancedate':'2013-06-11','attendancetypeid.name':'Present'},{'__key__':133047,'employeeid':372,'attendancedate':'2013-06-12','attendancetypeid.name':'Present'},{'__key__':133280,'employeeid':372,'attendancedate':'2013-06-13','attendancetypeid.name':'Present'},{'__key__':133597,'employeeid':372,'attendancedate':'2013-06-14','attendancetypeid.name':'Present'},{'__key__':133967,'employeeid':372,'attendancedate':'2013-06-15','attendancetypeid.name':'Off'},{'__key__':134214,'employeeid':372,'attendancedate':'2013-06-16','attendancetypeid.name':'Off'},{'__key__':134331,'employeeid':372,'attendancedate':'2013-06-17','attendancetypeid.name':'Present'},{'__key__':134536,'employeeid':372,'attendancedate':'2013-06-18','attendancetypeid.name':'Present'},{'__key__':134754,'employeeid':372,'attendancedate':'2013-06-19','attendancetypeid.name':'Present'},{'__key__':135164,'employeeid':372,'attendancedate':'2013-06-20','attendancetypeid.name':'Full Day Leave'},{'__key__':135285,'employeeid':372,'attendancedate':'2013-06-21','attendancetypeid.name':'Present'},{'__key__':135666,'employeeid':372,'attendancedate':'2013-06-22','attendancetypeid.name':'Off'},{'__key__':135918,'employeeid':372,'attendancedate':'2013-06-23','attendancetypeid.name':'Off'},{'__key__':136027,'employeeid':372,'attendancedate':'2013-06-24','attendancetypeid.name':'Present'},{'__key__':136255,'employeeid':372,'attendancedate':'2013-06-25','attendancetypeid.name':'Present'},{'__key__':136506,'employeeid':372,'attendancedate':'2013-06-26','attendancetypeid.name':'Present'},{'__key__':136919,'employeeid':372,'attendancedate':'2013-06-27','attendancetypeid.name':'Present'},{'__key__':137216,'employeeid':372,'attendancedate':'2013-06-28','attendancetypeid.name':'Second Half Day Leave'},{'__key__':137587,'employeeid':372,'attendancedate':'2013-06-29','attendancetypeid.name':'Off'},{'__key__':137846,'employeeid':372,'attendancedate':'2013-06-30','attendancetypeid.name':'Off'}],'name':'Abhishek Mishra','departmentid.name':'Marketing','employeecode':'DFG-1496'},{'__key__':414,'branchid.name':'Silokhera SEZ','employeeattendance':[],'name':'Adil Ahmad','departmentid.name':'Marketing','employeecode':'DFG-1538'}]");
		if (resultData != null) {
			Object organizationName = CurrentState.getCurrentVariable(CurrentSession.CURRENT_ORGANIZATION);
			// Object organizationName = "Daffodil S/W";
			// WritableWorkbook workbook = Workbook.createWorkbook(new File("/home/shekhar/att.xls"));
			WritableWorkbook workbook = Workbook.createWorkbook(outputStream);
			WritableSheet sheet = workbook.createSheet("Attendance Report", 0);
			// create Formater
			WritableCellFormat formatGrayCenterCenterBold = getFormater(Colour.GRAY_25, VerticalAlignment.CENTRE, Alignment.CENTRE);
			WritableFont boldFont = new WritableFont(WritableFont.createFont("MS Sans Serif"), 14, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE);
			WritableCellFormat formaterBlackLeft = getFormater(Colour.BLACK, VerticalAlignment.CENTRE, Alignment.LEFT);
			WritableCellFormat formaterBlackCenterSRNO = getFormater(Colour.BLACK, VerticalAlignment.CENTRE, Alignment.CENTRE);
			WritableCellFormat formaterBlackCenterCenterBold = getFormater(Colour.BLACK, VerticalAlignment.CENTRE, Alignment.CENTRE);
			// Add Font
			formaterBlackCenterCenterBold.setFont(boldFont);
			int row = 0;
			int column = 0;
			addLabel(sheet, column, row++, organizationName + "", formaterBlackCenterCenterBold);
			String fromDate = parameterObject.getString("fromdate");
			addLabel(sheet, column, row, "Attendance Report-" + HRISExcelReportsHandler.getMonthName(fromDate), formaterBlackCenterCenterBold);
			addLabel(sheet, column++, row = row + 2, "S.No.", formatGrayCenterCenterBold);
			addLabel(sheet, column++, row, "Emp. Code", formatGrayCenterCenterBold);
			addLabel(sheet, column++, row, "Name", formatGrayCenterCenterBold);
			addLabel(sheet, column++, row, "Department", formatGrayCenterCenterBold);
			addLabel(sheet, column++, row, "Branch", formatGrayCenterCenterBold);
			Calendar calendarFromDate = HRISExcelReportsHandler.getCalendarDate(fromDate);
			String shortMonthName = new SimpleDateFormat("MMM").format(calendarFromDate.getTime());
			int noOfDays = HRISExcelReportsHandler.getNoOfDays(fromDate);
			Set<String> daysSet = new LinkedHashSet<String>();
			for (int i = 1; i <= noOfDays; i++) {
				addLabel(sheet, column++, row, i + "-" + shortMonthName, formatGrayCenterCenterBold);
				fromDate = fromDate.substring(0, fromDate.lastIndexOf("-")).concat(i > 9 ? "-" + i : "-0" + i);
				daysSet.add(fromDate);
				sheet.setColumnView(i + 4, 15);
			}
			sheet.setColumnView(0, 8);
			sheet.setColumnView(1, 20);
			sheet.setColumnView(2, 20);
			sheet.setColumnView(3, 20);
			sheet.setColumnView(4, 20);
			sheet.setRowView(0, 500);
			sheet.setRowView(1, 500);
			sheet.mergeCells(0, 0, noOfDays + 4, 0);
			sheet.mergeCells(0, 1, noOfDays + 4, 1);
			sheet.setRowView(row, 400);
			row++;
			column = 0;
			int resultLength = resultData.length();
			for (int i = 0; i < resultLength; i++) {
				JSONObject employeeObject = resultData.getJSONObject(i);
				String empCode = employeeObject.optString("employeecode", "No Data");
				String empName = employeeObject.optString("name", "No Data");
				String departmentName = employeeObject.optString("departmentid.name", "No Data");
				String branchName = employeeObject.optString("branchid.name", "No Data");
				addLabel(sheet, column++, row, i + 1 + "", formaterBlackCenterSRNO);
				addLabel(sheet, column++, row, empCode, formaterBlackLeft);
				addLabel(sheet, column++, row, empName, formaterBlackLeft);
				addLabel(sheet, column++, row, departmentName, formaterBlackLeft);
				addLabel(sheet, column++, row, branchName, formaterBlackLeft);
				JSONArray empAttendance = employeeObject.getJSONArray("employeeattendance");
				Map<String, String> empAttendanceMap = getEmpAttendanceSet(empAttendance);
				if (empAttendanceMap != null) {
					for (String day : daysSet) {
						String attStatus = empAttendanceMap.get(day);
						addLabel(sheet, column++, row, attStatus, formaterBlackLeft);
					}
				}
				row++;
				column = 0;
			}
			workbook.write();
			workbook.close();
		}
	}

	private static Map<String, String> getEmpAttendanceSet(JSONArray empAttendance) throws JSONException {
		Map<String, String> empAttendanceMap = null;
		int attLength = empAttendance.length();
		if (attLength > 0) {
			empAttendanceMap = new HashMap<String, String>();
			for (int i = 0; i < attLength; i++) {
				JSONObject attendaceObject = empAttendance.getJSONObject(i);
				String attendanceDate = attendaceObject.getString("attendancedate");
				String attendanceStatus = attendaceObject.getString("attendancetypeid.name");
				empAttendanceMap.put(attendanceDate, attendanceStatus);
			}
		}
		return empAttendanceMap;
	}

	private static void addLabel(WritableSheet sheet, int column, int row, String s, WritableCellFormat times) throws WriteException, RowsExceededException {
		Label label;
		label = new Label(column, row, s, times);
		sheet.addCell(label);
	}

	private static WritableCellFormat getFormater(Colour color, VerticalAlignment verticalAlignment, Alignment horizontalAlignment) throws WriteException {
		WritableCellFormat cellFormat = getFont();
		cellFormat.setBackground(color);
		cellFormat.setAlignment(horizontalAlignment);
		cellFormat.setVerticalAlignment(verticalAlignment);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN, Colour.BLACK);
		cellFormat.setWrap(true);
		return cellFormat;
	}

	private static WritableCellFormat getFont() throws WriteException {
		WritableFont times10pt = new WritableFont(WritableFont.TIMES, 10);
		WritableCellFormat times = new WritableCellFormat(times10pt);
		times.setWrap(true);
		WritableCellFormat cellFormat = new WritableCellFormat(times10pt);
		return cellFormat;
	}
}