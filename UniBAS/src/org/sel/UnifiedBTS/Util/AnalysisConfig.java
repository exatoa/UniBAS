package org.sel.UnifiedBTS.Util;

import java.io.File;
import java.io.IOException;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

public class AnalysisConfig extends Config{
	
	private static AnalysisConfig Instance = null;
	
	//긴 메소드명
	public static AnalysisConfig getInstance()
	{
		if (Instance==null)
			Instance =  new AnalysisConfig();
		return Instance;
	}
	
	//단축명
	public static AnalysisConfig it()
	{
		return getInstance();
	}

	//[SITE]
	public String 	TITLE 		= "DuplicateTest";
	public String 	DESC 		= "'-----------'";
	public String 	SCHEMA 		= "Analysis_Dupe";
	public int		SITE_ID 	= 6;
	public String 	PROJECT_ID 	= "7,15,16";
	public int		IS_UNIFORMLY= 1;
	public String 	START_DATE 	= "";
	public String 	END_DATE 	= "";
	public String 	CONDITION 	= "";
	
	
	public String Filename = "analysis_config.ini";

	
	
	protected AnalysisConfig()
	{
		//nothing
	}
	
	protected AnalysisConfig(String _path)
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
			ini = new Ini(new File("config.ini"));
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
		
			value = ini.get("SITE", "TITLE");			TITLE = value;
			value = ini.get("SITE", "DESC");			DESC = value;
			value = ini.get("SITE", "SCHEMA");			SCHEMA = value;
			value = ini.get("SITE", "SITE_ID");			SITE_ID = Integer.parseInt(value);
			value = ini.get("SITE", "PROJECT_ID");		PROJECT_ID = value;
			value = ini.get("SITE", "IS_UNIFORMLY");	IS_UNIFORMLY = Integer.parseInt(value);
			value = ini.get("SITE", "START_DATE");		START_DATE = value;
			value = ini.get("SITE", "END_DATE");		END_DATE = value;
			value = ini.get("SITE", "CONDITION");		CONDITION = value;
														
			value = ini.get("SETTING", "LOG_PATH");		LOG_PATH = value;
			
			value = ini.get("DB", "DB_TYPE");			DB_TYPE = value;
			value = ini.get("DB", "DB_URL");			DB_URL = value;
			value = ini.get("DB", "DB_PORT");			DB_PORT = value;
			value = ini.get("DB", "DB_ID");				DB_ID = value;
			value = ini.get("DB", "DB_PW");				DB_PW = value;
			value = ini.get("DB", "DESC");				DESC = value;
			value = ini.get("DB", "DB_BASEDB");			DB_BASEDB = value;
			value = ini.get("DB", "DB_PREFIX");			DB_PREFIX = value;
			
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
			Ini ini = new Ini(new File(Filename));

			ini.add("SITE", "TITLE", 		"DuplicateTest");	
			ini.add("SITE", "DESC", 		"-----------");			
			ini.add("SITE", "SCHEMA", 		"Analysis_Dupe");			
			ini.add("SITE", "SITE_ID", 		6);			
			ini.add("SITE", "PROJECT_ID", 	"7,15,16");			
			ini.add("SITE", "IS_UNIFORMLY",	1);			
			ini.add("SITE", "START_DATE", 	"");		
			ini.add("SITE", "END_DATE", 	"");
			ini.add("SITE", "CONDITION",	"");
												
			ini.add("SETTING", "LOG_PATH", "E:\\_Temp\\BTS\\_temp\\");		
			
			
			ini.add("DB", "DB_TYPE", "MSSQL");			
			ini.add("DB", "DB_URL", "210.125.146.156");// "zeus.sigse.org");			
			ini.add("DB", "DB_PORT", "1433");			
			ini.add("DB", "DB_ID", "BugManager");				
			ini.add("DB", "DB_PW", "smartBug@1");				
			ini.add("DB", "DB_BASEDB", "UniBAS");			
			ini.add("DB", "DB_PREFIX", "Bug");	

		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
