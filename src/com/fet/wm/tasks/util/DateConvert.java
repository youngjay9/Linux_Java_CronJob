package com.fet.wm.tasks.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateConvert {
	
		/**
		 * @param Calendar
		 * @return String 2001/12/13 Format
		 */
		public static final String DateSeparator = "/";
		public static final String LongDatePattern = "yyyy" + DateSeparator + "MM"
				+ DateSeparator + "dd HH:mm:ss";
		public static final String ShortDatePattern = "yyyy" + DateSeparator + "MM"
				+ DateSeparator + "dd";
		public static final String PatternForInfinity = "dd-MMM-yyyy HH:mm";
		public static final String ShortPatternForInfinity = "dd-MMM-yyyy";
	
		public static final int MINUTE = 1;
		public static final int HOUR = 2;
		public static final int DAY = 3;
		public static final int WEEK = 4;
		public static final int MONTH = 5;
		public static final int QUARTER = 6;
		public static final int YEAR = 7;
	
		public static java.text.DateFormatSymbols INT_CN_SYMBOL;// just like 1ды,2ды
	
		private static final long ONE_DAY = 1035352668498L;
	
		static {
			String[] newMonths = new String[12];
			DateFormatSymbols symbol = new DateFormatSymbols(Locale.CHINA);
			String[] months = symbol.getMonths();
			String monthStr = months[0].substring(1, 2);
			for (int i = 1; i <= 12; i++) {
				newMonths[i - 1] = String.valueOf(i).concat(monthStr);
			}
			symbol.setMonths(newMonths);
			INT_CN_SYMBOL = symbol;
		}
	
		public static String DateToStr(Date datetime, String pattern) {
			return DateToStr(datetime, pattern, null);
		}
	
		/**
		 * format the date object to a string
		 * 
		 * @param datetime
		 *            the date object.
		 * @param pattern
		 *            the string pattern
		 * @param locale
		 *            the locale.if the locale is null ,the locale will be set to
		 *            the default.
		 * @return string
		 * @author ford. added in 2002/10/16
		 */
		public static String DateToStr(Date datetime, String pattern, Locale locale) {
			String strdate = "";
			if (datetime == null) {
				return "";
			}
			if (locale == null)
				locale = Locale.getDefault();
			SimpleDateFormat simformat = new SimpleDateFormat(pattern, locale);
			strdate = simformat.format(datetime);
			return strdate;
		}
	
		/**
		 * 
		 * @param original
		 *            the original time.
		 * @param intervalUnit
		 *            the unit,just like day,week,or month You must use the static
		 *            field of this class.
		 * @param intervalCount
		 *            the count,just like 1,2,3,
		 * @param direction
		 *            true-after;false--before;
		 * @return date object
		 */
		public static java.util.Date getRelativeTime(java.util.Date original,
				int intervalUnit, int intervalCount, boolean direction) {
			Calendar calendar = Calendar.getInstance();
			
			// fixed by bobo, 2008/12/02
			if(!direction){
				intervalCount = -intervalCount;
			}
			calendar.setTime(original);
			if (intervalUnit == MINUTE) // minute
				calendar.add(Calendar.MINUTE, intervalCount);
			else if (intervalUnit == HOUR) // hour
				calendar.add(Calendar.HOUR, intervalCount);
			else if (intervalUnit == DAY) // day
				calendar.add(Calendar.DATE, intervalCount);
			else if (intervalUnit == WEEK) // week = 7 days
				calendar.add(Calendar.DATE, intervalCount * 7);
			else if (intervalUnit == MONTH) // month
				calendar.add(Calendar.MONTH, intervalCount);
			else if (intervalUnit == QUARTER) // season = 3 month
				calendar.add(Calendar.MONTH, intervalCount * 3);
			else if (intervalUnit == YEAR) // year
				calendar.add(Calendar.YEAR, intervalCount);
			return calendar.getTime();
		}
	
		public static Date StrToDate(String datetimeStr, String pattern)
				throws ParseException {
			return StrToDate(datetimeStr, pattern, null);
		}
	
		/**
		 * parse the string to a date object.
		 * 
		 * @param datetimeStr
		 *            the string stands date
		 * @param pattern
		 *            the pattern
		 * @param locale
		 *            the locale, if it is null,it will be set to the default
		 *            locale.
		 * @return Date
		 * @throws ParseException
		 * @author ford
		 */
		public static Date StrToDate(String datetimeStr, String pattern,
				Locale locale) throws ParseException {
			if (datetimeStr == null || datetimeStr.equals("")
					|| datetimeStr.equals(" ")) {
				return null;
			}
			java.util.Date date = null;
			if (locale == null)
				locale = Locale.getDefault();
			SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
			try {
				date = format.parse(datetimeStr);
				return date;
			} catch (ParseException e) {
				throw e;
			}
		}
	
		public static int diffDate(Date date1, Date date2) {
	
			// TODO: add detail
			return 0;
		}
	
		public static String dateFormat(long date) {
			DateFormat dateFormat;
			dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN);
			Date temp = new Date(date);
			return dateFormat.format(temp);
	
		}
	
		public static String timeFormat(long date, String formate) {
			DateFormat dateFormat;
			dateFormat = new SimpleDateFormat(formate, Locale.TAIWAN);
			Date temp = new Date(date);
			return dateFormat.format(temp);
	
		}
		//V1.1, Start
		public static String getBeforeDate(Date date,int days,String dataFormat)   
		{   
		    SimpleDateFormat df = new SimpleDateFormat(dataFormat);   
		    Calendar calendar = Calendar.getInstance();      
		    calendar.setTime(date);   
		    calendar.set(Calendar.DAY_OF_YEAR,calendar.get(Calendar.DAY_OF_YEAR) - days);   
		    return df.format(calendar.getTime());   
		}  
		public static String getAfterDate(Date date,int days,String dataFormat)   
		{   
		    SimpleDateFormat df = new SimpleDateFormat(dataFormat);   
		    Calendar calendar = Calendar.getInstance();      
		    calendar.setTime(date);   
		    calendar.set(Calendar.DAY_OF_YEAR,calendar.get(Calendar.DAY_OF_YEAR) + days);   
		    return df.format(calendar.getTime());   
		}  
		//V1.1, End
}
