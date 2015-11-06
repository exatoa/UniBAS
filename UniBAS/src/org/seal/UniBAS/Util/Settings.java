package org.seal.UniBAS.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.hjson.*;

/**
 * 프로그램 전체 설정 파일.
 * Singleton 
 * 처음 실행시(설정파일이 없는 경우), Initialized변수를 true로 만들고 Instance는 null을 반환
 * 프로그램 내부에서 기본 설정파일 생성
 * 사용자는 설정파일을 변경하여 재시작 시키면 됨.
 * @author Zeck
 * 2015-07-27 
 */
public class Settings {
	
	protected static Settings Instance = null;
	protected static String FileName = "Settings.hjson";
	public static boolean Initialized = false;
	
	/**
	 * 인스턴스 생성 및 인스턴스 반환
	 * @return null or Instance.
	 */
	public static Settings getInstance()
	{
		//인스턴스에 값을 
		if (Instance==null){
			Instance =  new Settings();
			
			File file = new File(FileName);			
			if(file.exists())
			{
				try {
					Instance.Json = Instance.getJsonObject();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				Instance.mappingVariables();
				
			}
			//json 설정파일이 존재하지 않으면...새 파일 생성.
			else{
				if (Instance.makeDefaultSettings()==false)
					return null;
				Initialized = true;
				return null;
			}
		}
		return Instance;
	}
	
	//단축명 생성자. 
	public static Settings it()
	{
		return getInstance();
	}
	

	
		
	/*************************************************************************/
	/************ non-static Class part **************************************/
	/*************************************************************************/
	
	public JsonObject Json = null;
	
	/**
	 * 생성자
	 */
	private Settings()
	{
		//현재는 내용없음
	}
	
	
	/*************************************************************/
	/** 설정 변수                                                                             **/
	/*************************************************************/
	
	//[Common]
	public String DB_TYPE = "MSSQL";
	public String DB_ADDR = "zeus.sigse.org";
	public String DB_PORT = "1433";
	public String DB_ID = "BugManager";
	public String DB_PW = "smartBug@1";
	public String DB_BASEDB = "UniBAS2";
	public String DB_PREFIX = "Bug";

	public int CACHE_LEVEL = 2;		//캐쉬 디렉토리 깊이
	public int CACHE_NAMESIZE = 2;	//캐쉬 디렉토리 이름길이.
	public String CACHE_PATH = "E:\\_Research\\2014_UniBAS\\_cache\\";
	
	public String LOG_PATH = "E:\\_Research\\2014_UniBAS\\.logs\\";
	
	
	//[Crawling]
	public String SYS_NAME = "Mozilla";
	public String SYS_TYPE = "Bugzilla";
	public String SYS_DESC = "";
	public String SYS_URL = "http://bugzilla.mozilla.org/";
	public String SYS_ID = "forglee@naver.com";
	public String SYS_PW = "Sel535447";
	public int WAIT_MINUTE = 0;
	public int LIMIT_COUNT = 0;
	public int DOWN_SPEED = 0;
	
	
	//[Analysis]
	public String 	AS_NAME 	= "";
	public String 	AS_DESC 	= "";
	public int		SITE_ID 	= 0;
	public String 	PROJECT_ID 	= "";
	public int		IS_UNIFORMLY= 0;
	public String 	START_DATE 	= "";
	public String 	END_DATE 	= "";
	public String 	CONDITION 	= "";

	//[Extract]
	public String 	EX_INPUTS = "";
	public String 	EX_SITE = "";
	public String 	EX_PAGE = "";
	public String 	EX_OUTPUTPATH = "";
	public String 	EX_EXTNAME = "";
	public int 		EX_SPLIT_CNT = 0;
	
	
	public void mappingVariables()
	{
		//Common
		DB_TYPE = getString("COMMON", "DB_TYPE");
		DB_ADDR = getString("COMMON", "DB_ADDR");
		DB_PORT = String.valueOf(getInt("COMMON", "DB_PORT"));
		DB_ID = getString("COMMON", "DB_ID");
		DB_PW = getString("COMMON", "DB_PW");
		DB_BASEDB = getString("COMMON", "DB_BASEDB");
		DB_PREFIX = getString("COMMON", "DB_PREFIX");

		CACHE_PATH = getString("COMMON","CACHE_PATH");
		CACHE_LEVEL = getInt("COMMON","CACHE_LEVEL");
		CACHE_NAMESIZE = getInt("COMMON","CACHE_NAMESIZE");
		
		LOG_PATH = getString("COMMON","LOG_PATH");
		
		//Crawling
		SYS_NAME = getString("CRAWLING", "SYS_NAME");
		SYS_TYPE = getString("CRAWLING", "SYS_TYPE");
		SYS_DESC = getString("CRAWLING", "SYS_DESC");
		SYS_URL = getString("CRAWLING", "SYS_URL");
		SYS_ID = getString("CRAWLING", "SYS_ID");
		SYS_PW = getString("CRAWLING","SYS_PW");
		WAIT_MINUTE = getInt("CRAWLING","WAIT_MINUTE");
		LIMIT_COUNT = getInt("CRAWLING","LIMIT_COUNT");
		DOWN_SPEED = getInt("CRAWLING","DOWN_SPEED");
		
		//Analysis
		AS_NAME = getString("ANALYSIS","AS_NAME");
		AS_DESC = getString("ANALYSIS","AS_DESC");
		SITE_ID = getInt("ANALYSIS","SITE_ID");
		PROJECT_ID = getString("ANALYSIS","PROJECT_ID");
		IS_UNIFORMLY = getInt("ANALYSIS","IS_UNIFORMLY");
		CONDITION = getString("ANALYSIS","CONDITION");
		START_DATE = getString("ANALYSIS", "START_DATE");
		END_DATE = getString("ANALYSIS", "END_DATE");
		
		//Extract
		EX_INPUTS = getString("EXTRACT","EX_INPUTS");
		EX_SITE = getString("EXTRACT","EX_SITE");
		EX_PAGE = getString("EXTRACT","EX_PAGE");
		EX_OUTPUTPATH = getString("EXTRACT","EX_OUTPUTPATH");
		EX_EXTNAME = getString("EXTRACT","EX_EXTNAME");
		EX_SPLIT_CNT = getInt("EXTRACT","EX_SPLIT_CNT");
	}
	
	/**
	 * 로드된 설정을 출력
	 */
	public void printSettings() {
		System.out.println("*********** Settings **********");
		System.out.println("[Common]");
		System.out.println("DB_TYPE : " + DB_TYPE);
		System.out.println("DB_ADDR : " + DB_ADDR);
		System.out.println("DB_PORT : " + DB_PORT);
		System.out.println("DB_ID : " + DB_ID);
		System.out.println("DB_PW : " + DB_PW);
		System.out.println("DB_BASEDB : " + DB_BASEDB);
		System.out.println("DB_PREFIX : " + DB_PREFIX);
		System.out.println("CACHE_PATH : " + CACHE_PATH);
		System.out.println("CACHE_LEVEL : " + CACHE_LEVEL);
		System.out.println("CACHE_NAMESIZE : " + CACHE_NAMESIZE);
		System.out.println("LOG_PATH : " + LOG_PATH);
		
		System.out.println("");
		System.out.println("[Crawling]");
		System.out.println("SYS_NAME : " + SYS_NAME);
		System.out.println("SYS_TYPE : " + SYS_TYPE);
		System.out.println("SYS_DESC : " + SYS_DESC);
		System.out.println("SYS_URL : " + SYS_URL);
		System.out.println("SYS_ID : " + SYS_ID);
		System.out.println("SYS_PW : " + SYS_PW);
		System.out.println("WAIT_MINUTE : " + String.valueOf(WAIT_MINUTE));
		System.out.println("LIMIT_COUNT : " + String.valueOf(LIMIT_COUNT));
		System.out.println("DOWN_SPEED : " + String.valueOf(DOWN_SPEED));
		System.out.println("");

		System.out.println("[Analysis]");
		System.out.println("AS_NAME : " + AS_NAME);
		System.out.println("AS_DESC" + AS_DESC);
		System.out.println("SITE_ID : " + SITE_ID);
		System.out.println("PROJECT_ID : " + PROJECT_ID);
		System.out.println("IS_UNIFORMLY : " + IS_UNIFORMLY);
		System.out.println("CONDITION : " + CONDITION);
		System.out.println("START_DATE : " + START_DATE);
		System.out.println("END_DATE : " + END_DATE);
		
		System.out.println("");
		System.out.println("[Extract]");
		System.out.println("EX_INPUTS : " + EX_INPUTS);
		System.out.println("EX_SITE : " + EX_SITE);
		System.out.println("EX_PAGE : " + EX_PAGE);
		System.out.println("EX_OUTPUTPATH : " + EX_OUTPUTPATH);
		System.out.println("EX_EXTNAME : " + EX_EXTNAME);
		System.out.println("EX_SPLIT_CNT : " + EX_SPLIT_CNT);
		System.out.println("********************************");
	}
	
	
	
	
	/*************************************************************/
	/** data type별 JOSN data return                            **/
	/*************************************************************/
	public String getString(String _group, String _value)
	{
		JsonObject groupObjcet = Json.get(_group).asObject();
		return groupObjcet.get(_value).asString();
	}
	
	public int getInt(String _group, String _value)
	{
		JsonObject groupObjcet = Json.get(_group).asObject();
		return groupObjcet.get(_value).asInt();
	}
	
	public double getDouble(String _group, String _value)
	{
		JsonObject groupObjcet = Json.get(_group).asObject();
		return groupObjcet.get(_value).asDouble();
	}	
	
	public double getFloat(String _group, String _value)
	{
		JsonObject groupObjcet = Json.get(_group).asObject();
		return groupObjcet.get(_value).asFloat();
	}
	
	public boolean getBoolean(String _group, String _value)
	{
		JsonObject groupObjcet = Json.get(_group).asObject();
		return groupObjcet.get(_value).asBoolean();
	}
	
	
	
	/*************************************************************/
	/** 데이터 파일 로드 관련 코드                                                     **/
	/*************************************************************/

	/**
	 * JsonObject Load (외부 설정파일에서 로드)
	 * @param _file
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected JsonObject getJsonObject() throws FileNotFoundException, IOException
	{
		File file = new File(FileName);
		
		//설정파일 읽기
		StringBuilder builder = new StringBuilder();
		
		InputStream input = new FileInputStream(file);
		Scanner scanner = null;
	    try {
	        scanner = new Scanner(input,"UTF-8");
	        scanner.useDelimiter("\n");
	    }
	    catch(IllegalArgumentException e){
	    	log.printStackTrace(e);
	    	return null;
	    }

	    //내부 파일 읽기
	    while(scanner.hasNext()) {
	    	String text = scanner.next();
	    	builder.append(text);
	    	builder.append(scanner.delimiter());	    	
	    }
	    scanner.close();
		
	    //JSON으로 변환
		return JsonValue.readHjson(builder.toString()).asObject();
	}
	
	
	
	/**
	 * 기본 설정파일 생성 (resource 의 설정파일을 외부로 복사)
	 * @param _inputFile
	 * @param _outputFile
	 * @return
	 * @throws IOException 
	 */
	protected boolean makeDefaultSettings()
	{
		//설정파일 읽기
		StringBuilder builder = new StringBuilder();

		//파일을 읽어들일 스캐너 생성.
		//기본 설정 파일 생성
		InputStream input = this.getClass().getClassLoader().getResourceAsStream("settings/" + FileName);
	    Scanner scanner = null;
	    try {
	        scanner = new Scanner(input,"UTF-8");
	        scanner.useDelimiter("\n");
	    }
	    catch(IllegalArgumentException e){
	    	log.printStackTrace(e);
	    	return false;
	    }

	    //내부 파일 읽기
	    while(scanner.hasNext()) {
	    	String text = scanner.next();
	    	builder.append(text);
	    	builder.append(scanner.delimiter());	    	
	    }
	    scanner.close();
	    
	    File file = new File(FileName);
		//외부 폴더에 쓰기
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(builder.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}		
		
		return true;
	}

	

}
