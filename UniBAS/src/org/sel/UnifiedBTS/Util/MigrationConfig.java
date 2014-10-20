package org.sel.UnifiedBTS.Util;

import java.io.File;
import java.io.IOException;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

public class MigrationConfig {
	
	private static MigrationConfig Instance = null;
	
	//긴 메소드명
	public static MigrationConfig getInstance()
	{
		if (Instance==null)
			Instance =  new MigrationConfig();
		return Instance;
	}
	
	//단축명
	public static MigrationConfig it()
	{
		return getInstance();
	}

	//[SITE]
	public String 	SCHEMA 		= "bugzilla_mozilla";
	public String 	TYPE 		= "Bugzilla";

	//[SETTING]
	public String LOG_PATH = "E:\\_Temp\\BTS\\_temp\\";
	
	//[DB]
	public String DB_TYPE = "MSSQL";
	public String DB_URL = "210.125.146.156";//"zeus.sigse.org";
	//public String DB_URL = "localhost";
	public String DB_PORT = "1433";
	public String DB_ID = "BugManager";
	public String DB_PW = "smartBug@1";
	public String DB_BASEDB = "UniBAS";
	public String DB_PREFIX = "Bug";

	
	public String Filename = "migration_config.ini";

	
	
	private MigrationConfig()
	{
		//nothing
	}
	
	private MigrationConfig(String _path)
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
		
			value = ini.get("SITE", "SCHEMA");			SCHEMA = value;
			value = ini.get("SITE", "TYPE");			TYPE = value;
			
			value = ini.get("SETTING", "LOG_PATH");		LOG_PATH = value;
			
			value = ini.get("DB", "DB_TYPE");			DB_TYPE = value;
			value = ini.get("DB", "DB_URL");			DB_URL = value;
			value = ini.get("DB", "DB_PORT");			DB_PORT = value;
			value = ini.get("DB", "DB_ID");				DB_ID = value;
			value = ini.get("DB", "DB_PW");				DB_PW = value;
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

			ini.add("SITE", "SCHEMA", 		"Analysis_Dupe");
			ini.add("SITE", "TYPE", 		"Bugzilla");
												
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
