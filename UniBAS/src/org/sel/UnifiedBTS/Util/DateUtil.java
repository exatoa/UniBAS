package org.sel.UnifiedBTS.Util;

import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateUtil {
	
//	public static String getUTC(String _date)
//	{
//		DateFormat dfRead = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
//		DateFormat dfReadDate = new SimpleDateFormat("yyyy-MM-dd");
//		DateFormat dfUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		dfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//		
//		Date date;
//		try {
//			date = dfRead.parse(_date);
//		} catch (ParseException e) {
//			log.printStackTrace(e);
//			return "";
//		}
//		
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(date);
//		
//		return dfUTC.format(cal.getTime());
//	}
	
	

	/**
	 * 문자열을 파싱하여 DateTime를 돌려줌 (문자열 : yyyy-MM-dd)
	 * @param _strDate
	 * @return
	 */
	public static DateTime getDate(String _strDate)
	{
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		DateTime dt = null;
		try{
			
			dt = format.parseDateTime(_strDate);
		}
		catch(IllegalArgumentException e)
		{
			dt = null;
		}		
		return dt;
	}
	
	/**
	 * 문자열을 파싱하여 DateTime를 돌려줌 (문자열 : yyyy-MM-dd)
	 * @param _strDate
	 * @return
	 */
	public static String  getDateString(DateTime _date)
	{
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		return _date.toString(format);
	}
	
	/**
	 * 문자열을 파싱하여 DateTime를 돌려줌 (문자열 : yyyyMMdd_hhmmss)
	 * @param _strDate
	 * @return
	 */
	public static String getDateString(DateTime _date, String _format)
	{
		DateTimeFormatter format = DateTimeFormat.forPattern(_format);
		return _date.toString(format);		
	}
	
	

	/**
	 * 문자열을 파싱하여 DateTime값을 돌려줌 (형식 : 2014-01-01 10:30:00 +0900 or KST)
	 * @param _strTime
	 * @return
	 */
	public static DateTime getDateTime(String _strTime, boolean _isZoneString)
	{
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z");
		DateTimeFormatter sFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		
		if(_isZoneString==true)
			format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z");
		
		//파싱
		DateTime dt = null;
		
		try{
			//IST, KST의 경우 에러 발생.
			DateTimeZone timeZone = null;
			DateTimeZone originZone = null;
			if( _strTime.contains( "IST" )) { 					// Assume IST = India time.
			    timeZone = DateTimeZone.forID("Asia/Kolkata");	//IST TimeZone생성.
			    _strTime = _strTime.replace( "IST", "").trim();	//IST 문자제거
			    dt = sFormat.parseDateTime(_strTime);			//문자열 파싱
			    originZone = dt.getZone();						//기존 타임존 저장
			    dt = dt.withZone(timeZone);						//새 타임존 할당
			    int zoneOffset = originZone.getStandardOffset(0) - timeZone.getStandardOffset(0);		//타임존 오프셋 계산
			    dt = dt.plusMillis(zoneOffset);					//오프셋 만큼 변경.
			}
			else if( _strTime.contains("KST")) { 				// Assume KST = Korea time.
			    timeZone = DateTimeZone.forID("Asia/Seoul");	//IST TimeZone생성.
			    _strTime = _strTime.replace( "KST", "").trim();	//IST 문자제거
			    dt = sFormat.parseDateTime(_strTime);			//문자열 파싱
			    originZone = dt.getZone();						//기존 타임존 저장
			    dt = dt.withZone(timeZone);						//새 타임존 할당
			    int zoneOffset = originZone.getStandardOffset(0) - timeZone.getStandardOffset(0);		//타임존 오프셋 계산
			    dt = dt.plusMillis(zoneOffset);					//오프셋 만큼 변경.
			}
			else
		    	dt = format.parseDateTime(_strTime);
		}
		catch(IllegalArgumentException e)
		{
			log.error("DateTime Conversion Error:" +_strTime);
			log.printStackTrace(e);
			dt = null;
		}
		
		return dt;
	}
	
	/**
	 * 문자열을 파싱하여 Timezone 값을 구함 (ShortName) (문자열 : 2014-01-01 10:30:00 +0900 or KST)
	 * @param _strTime
	 * @return
	 */
	public static String getTimezoneString(String _strTime, boolean _isZoneString)
	{
		DateTime dt = DateUtil.getDateTime(_strTime, _isZoneString);
		DateTimeZone zone = dt.getZone();
		return zone.getShortName(0);
	}
	
	/**
	 * long형의 unix Timestamp를 받아서 String 으로 변환 (형식 : yyyy-MM-dd HH:mm:ss)
	 * TODO 몇초 웹의 GMT랑 변환된 UTC랑 몇초 차이가 나는데 ....원래 그런건가?   
	 * @param _unix_timestamp
	 * @return
	 */
	public static String getStringFromTimestamp(long _timestamp) {

		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		long time = _timestamp*1000L;
		DateTime dt = new DateTime(DateTimeZone.UTC);
		dt = dt.withMillis(time);
		return dt.toString(format);
	}	
	

	
	/**
	 * 문자열의 시간을 UTC 시간으로 변경된 문자열을 반환 (문자열 : 2014-01-01 10:30:00 +0900 or KST)
	 * @param _strTime
	 * @return
	 */
	public static String getUTCString(String _strTime, boolean _isZoneString)
	{
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		
		//파싱시간.
		DateTime dt = DateUtil.getDateTime(_strTime, _isZoneString);
		
		//존 변경
		DateTimeZone zone = DateTimeZone.forID("UTC");
		dt = dt.withZone(zone);

		return dt.toString(format);
	}


	public static String getNowString() {
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		DateTime dt = DateTime.now();
		
		//DateTimeZone zone = DateTimeZone.forID("UTC");
		//dt = dt.withZone(zone);

		return dt.toString(format);
	}

	
	public static void test()
	{
		log.info(DateUtil.getStringFromTimestamp(1262417841L));
		log.info(DateUtil.getStringFromTimestamp(1397021041L));
		
		

	}

	/**
	 * 지정된 날짜가 두 날짜 사이에 있는지 비요.
	 * @param 생성일
	 * @param startDate
	 * @param endDate
	 * @return 포함되어 있으면 true, 아니면 false
	 */
	public static boolean compareOverDate(String _date, String _startDate, String _endDate) {

		//_startDate <= _creationTS <= _endDate   ==>   false;
		DateTime dtStart = getDate(_startDate);
		DateTime dtEnd = getDate(_endDate);
		DateTime dt = getDate(_date);
		if (dt==null) dt = DateTime.now();
		
		if(dtStart.compareTo(dt)<=0 && dt.compareTo(dtEnd)<=0){
			return true;
		}
		
//		if(DateUtil.compareDate(_startDate, _date)>=0)
//			if(DateUtil.compareDate(_date, _endDate)>=0) return false;
		
		return false;
	}
	
	
	public static int compareDate(String _data1, String _data2) {
		DateTime dt1 = getDate(_data1.substring(0,10));
		DateTime dt2 = getDate(_data2.substring(0,10));
		
		return dt1.compareTo(dt2)*-1;
	}

	/**
	 * 날짜값을 입력받아서 표준형식으로 내보냄
	 * 형식 : yyyy-MM-dd hh:mm:ss
	 * @param _dateStr
	 * @return
	 */
	public static String getStandardFormat(String _dateStr, DateTimeZone _zone) {
		
		DateTimeFormatter format = DateTimeFormat.forPattern("dd-MM-yy HH:mm");//
		DateTimeFormatter format2 = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
		DateTimeFormatter format3 = DateTimeFormat.forPattern("yy-MM-dd HH:mm");//14-Jun-30 10:41
		DateTimeFormatter standard = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		
		
		DateTime dt = null;
		try{
			dt = format.parseDateTime(_dateStr);
		}
		catch(IllegalArgumentException e){
			try{
				dt = format2.parseDateTime(_dateStr);
			}
			catch(IllegalArgumentException e2){
				try{
					_dateStr = changeNumberMonth(_dateStr);
					dt = format3.parseDateTime(_dateStr);
				}
				catch(IllegalArgumentException e3){
					e3.printStackTrace();
					return null;
				}
			}
		}
		
		//TODO : time존 만큼 변경해서 반환해야함.
		
		return dt.toString(standard); 
	}

	
	private static String changeNumberMonth(String _cal)
	{
		String str = _cal.replace("Jan", "01");
		str = str.replace("Feb", "02");
		str = str.replace("Mar", "03");
		str = str.replace("Apr", "04");
		str = str.replace("May", "05");
		str = str.replace("Jun", "06");
		str = str.replace("Jul", "07");
		str = str.replace("Aug", "08");
		str = str.replace("Sep", "09");
		str = str.replace("Oct", "10");
		str = str.replace("Nov", "11");
		str = str.replace("Dec", "12");
		
//		str = str.replace("January", "01");
//		str = str.replace("Febuary", "02");
//		str = str.replace("March", "03");
//		str = str.replace("April", "04");
//		str = str.replace("May", "05");
//		str = str.replace("June", "06");
//		str = str.replace("July", "07");
//		str = str.replace("August", "08");
//		str = str.replace("September", "09");
//		str = str.replace("October", "10");
//		str = str.replace("November", "11");
//		str = str.replace("December", "12");
		
		return str;
	}
	
}
