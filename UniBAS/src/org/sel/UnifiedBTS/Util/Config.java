package org.sel.UnifiedBTS.Util;

import java.io.File;
import java.io.IOException;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

public class Config {
	
	protected static Config Instance = null;
	
	//긴 메소드명
	public static Config getInstance()
	{
		if (Instance==null)
			Instance =  new Config();
		return Instance;
	}
	
	//단축명 생성자. 
	public static Config it()
	{
		return getInstance();
	}

	//[DB]
	public String DB_TYPE = "MSSQL";
	public String DB_URL = "zeus.sigse.org";//"zeus.sigse.org";
	public String DB_PORT = "1433";
	public String DB_ID = "BugManager";
	public String DB_PW = "smartBug@1";
	public String DB_BASEDB = "UniBAS";
	public String DB_PREFIX = "Bug";

	//[Common]
	public String LOG_PATH = "E:\\_Temp\\BTS\\_temp\\";
	public String NAME = "Mozilla";
	public String TYPE = "Bugzilla";
	public String DESC = "";
	public String START_DATE = "2014-01-01";
	public String END_DATE = "2014-01-31";
	
	//[Crawling]
	public String BASE_URL = "http://bugzilla.mozilla.org/";
	public String USER_ID = "forglee@naver.com";
	public String USER_PW = "Sel535447";
	public int WAIT_MINUTE = 5;
	public int LIMIT_COUNT = 1000;	//연속 다운로드 수 1000
	public int DOWN_SPEED = 40;		//분당 다운로드 속도 40개
	public int CACHE_LEVEL = 2;		//캐쉬 디렉토리 깊이
	public int CHCHE_NAMESIZE = 2;	//캐쉬 디렉토리 이름길이.
	public String CACHE_PATH = "E:\\_Temp\\BTS\\cache2\\";
	
	//[Migration]
	//public String NAME = "Mozilla";		// in Common
	//public String TYPE = "Bugzilla";		// in Common
	
	//[Analysis]
	public int		SITE_ID 	= 6;
	public String 	PROJECT_ID 	= "7,15,16";
	public int		IS_UNIFORMLY= 1;
	public String 	CONDITION 	= "";
	//public String 	NAME 		= "DuplicateTest"; // in Common
	//public String 	DESC 		= "'-----------'"; // in Common
	//public String 	SCHEMA 		= "Analysis_Dupe";	   // in Common
	//public String 	START_DATE 	= "";	// in Common
	//public String 	END_DATE 	= "";	// in Common
	

	
	public String Filename = "config.ini";

	
	
	protected Config()
	{
		//nothing
	}
	
	protected Config(String _path)
	{
		setPath(_path);
	}
	
	public void setPath(String _path)
	{
		if(_path!=null)
			Filename = _path;
	}
	
	
	public boolean readBasicConfig()
	{
		Ini ini=null;
		boolean flag = true;
		try {
			ini = new Ini(new File(Filename));
		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
			flag = false;
		} catch (IOException e) {
			e.printStackTrace();
			flag = false;
		}
		
		if(flag==false)
		{
			writeBasicConfig();
		}
		
		
		
		try{
			String value=null;
			value = ini.get("SETTING", "LOG_PATH");		LOG_PATH = value;
			
			//[DB]
			value = ini.get("DB", "DB_TYPE");			DB_TYPE = value;
			value = ini.get("DB", "DB_URL");			DB_URL = value;
			value = ini.get("DB", "DB_PORT");			DB_PORT = value;
			value = ini.get("DB", "DB_ID");				DB_ID = value;
			value = ini.get("DB", "DB_PW");				DB_PW = value;
			value = ini.get("DB", "DESC");				DESC = value;
			value = ini.get("DB", "DB_BASEDB");			DB_BASEDB = value;
			value = ini.get("DB", "DB_PREFIX");			DB_PREFIX = value;
						
			//[Common]
			value = ini.get("SITE", "NAME");			NAME = value;
			value = ini.get("SITE", "TYPE");			TYPE = value;
			value = ini.get("SITE", "DESC");			DESC = value;
			value = ini.get("SITE", "START_DATE");		START_DATE = value;
			value = ini.get("SITE", "END_DATE");		END_DATE = value;
			
			//[Crawling]
			value = ini.get("SITE", "BASE_URL");		BASE_URL = value;
			value = ini.get("SITE", "USER_ID");			USER_ID = value;
			value = ini.get("SITE", "USER_PW");			USER_PW = value;
			value = ini.get("SETTING", "CACHE_PATH");	CACHE_PATH = value;
			value = ini.get("SETTING", "WAIT_MINUTE");	WAIT_MINUTE = Integer.parseInt(value);
			value = ini.get("SETTING", "LIMIT_COUNT");	LIMIT_COUNT = Integer.parseInt(value);
			value = ini.get("SETTING", "DOWN_SPEED");	DOWN_SPEED = Integer.parseInt(value);
			value = ini.get("SETTING", "CACHE_LEVEL");	CACHE_LEVEL = Integer.parseInt(value);
			value = ini.get("SETTING", "CHCHE_NAMESIZE");	CHCHE_NAMESIZE = Integer.parseInt(value);
			
			//[Analysis]
			value = ini.get("SITE", "SITE_ID");			SITE_ID = Integer.parseInt(value);
			value = ini.get("SITE", "PROJECT_ID");		PROJECT_ID = value;
			value = ini.get("SITE", "IS_UNIFORMLY");	IS_UNIFORMLY = Integer.parseInt(value);
			value = ini.get("SITE", "CONDITION");		CONDITION = value;			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 초기 ini파일을 설정해준다.
	 */
	public void writeBasicConfig()
	{
		try {
			Ini ini = new Ini(new File("config.ini"));

			//[DB]
			ini.add("DB", "DB_TYPE", "MSSQL");			
			ini.add("DB", "DB_URL", "210.125.146.156");// "zeus.sigse.org");			
			ini.add("DB", "DB_PORT", "1433");			
			ini.add("DB", "DB_ID", "BugManager");				
			ini.add("DB", "DB_PW", "smartBug@1");				
			ini.add("DB", "DB_BASEDB", "UniBAS");			
			ini.add("DB", "DB_PREFIX", "Bug");	
						
			//[Common]
			ini.add("SETTING", "LOG_PATH", "E:\\_Temp\\BTS\\_temp\\");
			ini.add("SITE", "NAME", "Mozilla");			
			ini.add("SITE", "TYPE", "Bugzilla");			
			ini.add("SITE", "DESC", "");			
			ini.add("SITE", "START_DATE", "2014-01-01");		
			ini.add("SITE", "END_DATE", "2014-01-31");		
						
			//[Crawling]
			ini.add("SITE", "BASE_URL", "http://bugzilla.mozilla.org/");	
			ini.add("SITE", "USER_ID", "forglee@naver.com");			
			ini.add("SITE", "USER_PW", "Sel535447");					
			ini.add("SETTING", "CACHE_PATH", "E:\\_Temp\\BTS\\cache2\\");	
			ini.add("SETTING", "WAIT_MINUTE", 5);	
			ini.add("SETTING", "LIMIT_COUNT", 1000);	
			ini.add("SETTING", "DOWN_SPEED", 40);	
			ini.add("SETTING", "CACHE_LEVEL", 2);
			ini.add("SETTING", "CHCHE_NAMESIZE", 2);
			
			//[Analysis]
			ini.add("SITE", "SITE_ID", 		6);			
			ini.add("SITE", "PROJECT_ID", 	"7,15,16");			
			ini.add("SITE", "IS_UNIFORMLY",	1);			
			ini.add("SITE", "START_DATE", 	"");		

			
		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
