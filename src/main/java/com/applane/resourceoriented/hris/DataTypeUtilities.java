package com.applane.resourceoriented.hris;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import com.applane.databaseengine.LogUtility;
import com.applane.databaseengine.exception.BusinessLogicException;
import com.applane.service.mail.ApplaneMail;
import com.applane.service.mail.ApplaneMailException;
import com.google.appengine.api.utils.SystemProperty;

public class DataTypeUtilities {

	public static long daysBetween(Date max, Date min) {
		return (max.getTime() - min.getTime()) / 86400000;
	}

	public static Date checkDateFormat(Object dateObject) {
		if (dateObject == null) {
			return null;
		}
		Date date = null;
		SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			date = queryDateFormat.parse("" + dateObject);
		} catch (ParseException e) {
			e.printStackTrace();
			try {
				date = updateDateFormat.parse("" + dateObject);
			} catch (ParseException e1) {
				e1.printStackTrace();
				throw new BusinessLogicException("date is not parsable" + e.getMessage());
			}
		}
		return date;
	}

	public static int integerValue(Object value) {
		if (value instanceof String) {
			return Integer.parseInt(value.toString());
		}
		return (value == null ? 0 : ((Number) value).intValue());
	}

	public static long differenceBetweenDates(Date leaveFromDate, Date leaveToDate) {
		// Creates two calendars instances
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();

		// Set the date for both of the calendar instance
		cal1.setTime(leaveToDate);
		cal2.setTime(leaveFromDate);

		// Get the represented date in milliseconds
		long milis1 = cal1.getTimeInMillis();
		long milis2 = cal2.getTimeInMillis();

		// Calculate difference in milliseconds
		long diff = milis1 - milis2;

		// Calculate difference in days
		long diffDays = diff / (24 * 60 * 60 * 1000);

		return diffDays;
	}

	public static long monthsBetweenDates(Date leaveFromDate, Date leaveToDate) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(leaveToDate);
		cal2.setTime(leaveFromDate);
		long milis1 = cal1.getTimeInMillis();
		long milis2 = cal2.getTimeInMillis();
		long diff = milis1 - milis2;
		long diffDays = (int) (diff / ((365.24 * 24 * 60 * 60 * 1000) / 12) + 1.0);
		return diffDays;
	}

	public static boolean booleanValue(Object value) {
		if (value == null) {
			return false;
		}
		boolean isboolean = false;
		if (value instanceof Integer) {
			int iday = ((Integer) value).intValue();
			if (iday == 1) {
				isboolean = true;
			}
		} else if (value instanceof Short) {
			int iday = ((Short) value).shortValue();
			if (iday == 1) {
				isboolean = true;
			}
		} else if (value instanceof Boolean) {
			isboolean = (Boolean) value;
		}

		return isboolean;
	}

	public static String showDifference(Date firstDate, Date secondDate) {
		long firstDateMS = firstDate.getTime();
		long secondDateMS = secondDate.getTime();
		String totalTimeCalculated = dissociateTimeInHrsAndMin(secondDateMS - firstDateMS);
		return totalTimeCalculated;
	}

	public static long showTimeDifferenceInMiliSeconds(Date firstDate, Date secondDate) {
		long firstDateMS = firstDate.getTime();
		long secondDateMS = secondDate.getTime();
		long totalMiliSeconds = secondDateMS - firstDateMS;
		return totalMiliSeconds;
	}

	public static String dissociateTimeInHrsAndMin(long millis) {
		if (millis < 0) {
			throw new IllegalArgumentException("Duration must be greater than zero!");
		}

		long days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days);
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes);
		// long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

		StringBuilder sb = new StringBuilder(64);
		/*
		 * sb.append(days); sb.append(" Days ");
		 */
		sb.append(hours);
		sb.append(" Hr.");
		sb.append(minutes);
		sb.append(" Min.");
		/*
		 * sb.append(seconds); sb.append(" Seconds");
		 */

		return (sb.toString());
	}

	public static long calculateMiliseconds(Date firstDate, Date secondDate) {
		long firstDateMS = firstDate.getTime();
		long secondDateMS = secondDate.getTime();
		long totalMiliSeconds = secondDateMS - firstDateMS;
		return totalMiliSeconds;
	}

	public static String changeDateInString(Object dateObject) {
		Date date = checkDateFormat(dateObject);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dateInString = sdf.format(date);
		return dateInString;
	}

	public static Date getMonthFirstDate(String yearName, String monthName) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMMM-dd");
		String startDate = yearName + "-" + monthName + "-" + "1";
		Date monthStartDate = dateFormat.parse(startDate);
		return monthStartDate;
	}

	public static Date getMonthFirstDate(Date date) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int lastDate = cal.getActualMinimum(Calendar.DATE);
		cal.set(Calendar.DAY_OF_MONTH, lastDate);
		Date lastDayofMonth = cal.getTime();
		return lastDayofMonth;
	}

	public static Date getMonthLastDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int lastDate = cal.getActualMaximum(Calendar.DATE);
		cal.set(Calendar.DAY_OF_MONTH, lastDate);
		Date lastDayofMonth = cal.getTime();
		return lastDayofMonth;
	}

	public static String getCurrentMonth(Date date) {
		SimpleDateFormat sdf;
		sdf = new SimpleDateFormat("MMMM");
		String currentMonth = sdf.format(date);
		return currentMonth;
	}

	public static String getNextDate(String dateInString) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = formatter.parse(dateInString);
		Calendar cal = new GregorianCalendar();
		cal.setTime(currentDate);
		cal.add(Calendar.DATE, 1);
		String nextDate = formatter.format(cal.getTime());
		return nextDate;
	}

	public static String getCurrentYear(Date date) {
		SimpleDateFormat sdf;
		sdf = new SimpleDateFormat("yyyy");
		String currentYear = sdf.format(date);
		return currentYear;
	}

	public static long getNumberOfDaysInMonth(Date date) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		long numOfDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		return numOfDaysInMonth;
	}

	public static String getWeekDay(Object dateObject) throws ParseException {
		Date date = null;
		String weekDay = null;
		// int dayOfWeek = 0;
		date = checkDateFormat(dateObject);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		// dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		String strDateFormat = "EEEE";
		SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
		weekDay = sdf.format(date);
		weekDay = weekDay.toLowerCase();
		return weekDay;
	}

	public static int getWeekNo(Object dateObject) throws ParseException {
		int weekDayNo = 0;
		Date date = null;
		date = checkDateFormat(dateObject);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		weekDayNo = cal.get(Calendar.WEEK_OF_MONTH);
		return weekDayNo;

	}

	public static int getWeekDayNo(Object dateObject) throws ParseException {
		int dayNo = 0;
		Date date = null;
		date = checkDateFormat(dateObject);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		dayNo = cal.get(Calendar.DAY_OF_WEEK);
		return dayNo;
	}

	public static void sendExceptionMail(String traces) {
		ApplaneMail mail = new ApplaneMail();
		StringBuilder builder = new StringBuilder();
		builder.append("<b>Server Name is :</b> ").append(SystemProperty.applicationId.get());
		builder.append("<br><br><b>Exception traces are given below :</b><br><br>").append(traces);
		mail.setMessage("HRIS Exception", builder.toString(), true);
		try {
			mail.setTo("kapil.dalal@daffodilsw.com");
			mail.sendAsAdmin();
		} catch (ApplaneMailException e1) {
			LogUtility.writeLog("Exception Come while send mail in Employee Punchin Cron.");
		}
	}

	public static String stringValue(Object value) {

		return (value == null ? "" : "" + value);
	}

	public static String getBackDate(String dateInString) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = (Date) formatter.parse(dateInString);
		Calendar cal = new GregorianCalendar();
		cal.setTime(currentDate);
		cal.add(Calendar.DATE, -1);
		String backDate = formatter.format(cal.getTime());
		return backDate;
	}

	public static Date getBackDate(Object date) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = null;
		if (date instanceof Date) {
			currentDate = (Date) date;
		} else {
			currentDate = (Date) formatter.parse("" + date);
		}
		Calendar cal = new GregorianCalendar();
		cal.setTime(currentDate);
		cal.add(Calendar.DATE, -1);
		// LogUtility.writeError("backDate >> " + cal.getTime());
		return cal.getTime();
	}

	public static Date subtractMonth(Date monthFirstDate, Integer paidAfterMonth) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(monthFirstDate);
		cal.add(Calendar.MONTH, -paidAfterMonth);
		Date backDate = cal.getTime();
		return backDate;
	}

	public static Date getFirstDayOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}

	// public static Date getLastDayOfMonth(Date date) {
	// Calendar calendar = Calendar.getInstance();
	// calendar.setTime(date);
	// calendar.set(Calendar.DAY_OF_MONTH, 1);
	// calendar.add(Calendar.DAY_OF_MONTH, -1);
	// calendar.add(Calendar.MONTH, 1);
	// return calendar.getTime();
	// }

	public static double doubleValue(Object value) {
		if (value instanceof Double) {
			return ((Double) value).doubleValue();
		} else if (value instanceof Integer) {
			return ((Integer) value).intValue();
		}
		return (value == null ? 0d : (Double) value);
	}

	public Date getFormatedDate(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMMM-dd");
		try {
			return dateFormat.parse(date.toString());
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("Error while parsing date: " + date);
		}
	}
}
