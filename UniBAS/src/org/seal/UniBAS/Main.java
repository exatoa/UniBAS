package org.seal.UniBAS;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.UnknownOptionException;
import jargs.gnu.CmdLineParser.Option.StringOption;




import java.io.IOException;
import java.sql.SQLException;

import org.joda.time.DateTime;
import org.seal.UniBAS.Bugzilla.BugzillaPreWorker;
import org.seal.UniBAS.Bugzilla.BugzillaWorker;
import org.seal.UniBAS.Core.Controller;
import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Core.Network.WebCacheFile;
import org.seal.UniBAS.Mantis.MantisWorker;
import org.seal.UniBAS.Util.Config;
import org.seal.UniBAS.Util.DateUtil;
import org.seal.UniBAS.Util.log;


/////////////////////////////////////////////////////////////////
//  정적 필드의 정의
/////////////////////////////////////////////////////////////////
/**
 * 지정된 옵션을 분석하고 작업을 지시.
 * 
 * 이 프로그램은 DB생성권한을 가진 계정을 필요로 합니다.
 * 생성될 기본 DB는 InfoDB가 될 것이며
 * 이미 존재하는 경우 시스템이 종료될 것이며 접두사를 이용해주시기 바랍니다.
 * 
 * @author Zeck
 */
public class Main
{	
	/**
	 * 프로그램 인트로 (initialize)
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.setProperty("java.net.useSystemProxies", "true");
	    System.setProperty("http.proxyHost", "127.0.0.1");
	    System.setProperty("http.proxyPort", "8888");
		System.setProperty("proxySet", "true");
		System.setProperty("https.proxyHost", "127.0.0.1");
	    System.setProperty("https.proxyPort", "8888");
		
		
	    
		//1. 옵션 분석=======================================================
		Config config = createOption(args);
		if(config==null) 
		{
			System.out.println("ERROR: invalid parameter.");
			return;
		}
		//config.DB_PREFIX = "Pre";
		
		
		//2. 로그 파일 설정=======================================================
		try {
			log.init(config.LOG_PATH + "log_"+config.NAME+".txt");
		} catch (IOException e){ 
			log.printStackTrace(e);
			return;
		}

		
		//3. WebFile클래스를 위한 환경설정.
		WebCacheFile web = WebCacheFile.getInstance();
		web.setLocalProxy(config.CACHE_PATH);
		web.setLimitation(config.LIMIT_COUNT, config.WAIT_MINUTE);
		web.setDownloadSpeed(config.DOWN_SPEED);
		web.setCacheLevel(config.CACHE_LEVEL);
		web.setCacheNamesize(config.CHCHE_NAMESIZE);
		
				
		//테스트 코드
		//DateUtil.test();
		printConfig(config);

		
		//4. 선택된 데이터 베이스 생성. (Type 에 따라서 데이터베이스 생성)
		DBManager db = null;
		db = DBManager.getInstance(config.DB_TYPE, config.DB_URL, config.DB_PORT, config.DB_ID, config.DB_PW);
		
		
		
		//5. 조건에 맞는 컨트롤러 생성.=======================================================
		Controller controller = null;
		if(config.TYPE.toLowerCase().compareTo("bugzilla")==0)
			if(config.DB_PREFIX.toLowerCase().compareTo("pre")==0)
				controller= new BugzillaPreWorker(config);
			else
				controller= new BugzillaWorker(config);
		
		else if(config.TYPE.toLowerCase().compareTo("mantis")==0)
			controller= new MantisWorker(config);
		
		else{
			log.error("Not support system.");
			return ;
		}
		

		
		//6.프로그램 실행=======================================================
		log.info("------------------------------------------");
		log.info("Start Program (" +""+ DateUtil.getNowString() + ")" );
		
		if(controller.run())
			log.print("Done");
		else
			log.print("Error Occured!");
		
		
		//마무리 정리.
		try {
			web.close();
			db.disconnection();
			log.close();
		}
		catch (IOException e) {
			log.printStackTrace(e);
		}
		catch(SQLException e){
			log.printStackTrace(e);
		}
		
	}//Main		
	

	/**
	 * 환경설정 정보를 출력
	 * @param config
	 */
	public static void printConfig(Config config) {
		log.info("-------------------<Config>-----------------------");
		log.info("[Site]");
		log.info("Type="+config.TYPE);
		log.info("Name="+config.NAME);
		log.info("Desc="+config.DESC);
		log.info("URL="+config.BASE_URL);
		log.info("ID="+config.USER_ID);
		log.info("PW="+config.USER_PW);
		log.info("StartDate="+config.START_DATE);
		log.info("EndDate="+config.END_DATE);
		log.info("");
		log.info("[System]");
		log.info("LogPath="+config.LOG_PATH);
		log.info("CachePath="+config.CACHE_PATH);
		log.info("WaitMimute="+config.WAIT_MINUTE);
		log.info("LimitCount="+config.LIMIT_COUNT);
		log.info("DownSpeed="+config.DOWN_SPEED);
		log.info("CACHE_LEVEL="+config.CACHE_LEVEL);
		log.info("CHCHE_NAMESIZE="+config.CHCHE_NAMESIZE);
		log.info("");
		log.info("[Database]");
		log.info("URL="+config.DB_URL);
		log.info("Port="+config.DB_PORT);
		log.info("ID="+config.DB_ID);
		log.info("PW="+config.DB_PW);
		log.info("Type="+config.DB_TYPE);
		log.info("Prefix="+config.DB_PREFIX);
		log.info("BaseDB="+config.DB_BASEDB);
		
	}
	

	
	
	/**
	 * 인자들로부터 옵션들을 추출하여 옵션을 생성.
	 * @param args
	 * @return
	 */
	public static Config createOption(String[] args)
	{
		CmdLineParser optParser = new CmdLineParser();
		Config config = Config.getInstance();///new Config();
		
		//옵션정의
		//Info-----------------------------------------------------
		Option optURL = new StringOption('i',"siteurl");
		Option optID = new StringOption('u',"id");
		Option optPW = new StringOption('p',"pw");
		Option optName = new StringOption('n',"name");
		Option optType = new StringOption('t', "type");
		Option optDesc = new StringOption('d',"desc");
//		Option optStartDate = new StringOption('b', "begindate");
//		Option optEndDate = new StringOption('e', "enddate");
		//System-----------------------------------------------------
		Option optBaseDB = new StringOption('b', "base_db");
		Option optLogPath = new StringOption('l', "logpath");
		Option optCachePath = new StringOption('c', "cachepath");
		Option optWaitMinute = new StringOption('w', "waitminute");
		Option optLimitCount = new StringOption('m', "limitcount");
		Option optDownSpeed = new StringOption('s', "downspeed");
		
		
		//TODO:데이터베이스 관련 옵션
		//Option optDBType = new StringOptin("x","dbType");
		
		
		
		//옵션등록
		optParser.addOption(optURL);
		optParser.addOption(optID);
		optParser.addOption(optPW);
		optParser.addOption(optName);
		optParser.addOption(optType);
		optParser.addOption(optDesc);
		//optParser.addOption(optStartDate);
		//optParser.addOption(optEndDate);
		optParser.addOption(optLogPath);
		optParser.addOption(optCachePath);
		optParser.addOption(optLimitCount);
		optParser.addOption(optWaitMinute);
		optParser.addOption(optDownSpeed);
		
		
		
		
		//옵션 파싱
		String option = null;
		try {
			optParser.parse(args);
			
		} catch (IllegalOptionValueException e1) {		
			log.printStackTrace(e1);
			return null;
		} catch (UnknownOptionException e1) {
			log.printStackTrace(e1);
			return null;
		}
		
		if((option =(String)optParser.getOptionValue(optURL))!=null) config.BASE_URL = option;
		if((option =(String)optParser.getOptionValue(optID))!=null) config.USER_ID = option;
		if((option =(String)optParser.getOptionValue(optPW))!=null) config.USER_PW = option;
		if((option =(String)optParser.getOptionValue(optName))!=null) config.NAME = option;
		if((option =(String)optParser.getOptionValue(optType))!=null) config.TYPE = option;
		if((option =(String)optParser.getOptionValue(optDesc))!=null) config.DESC = option;
		
		//if((option =(String)optParser.getOptionValue(optStartDate))!=null) config.START_DATE = option;
		//if((option =(String)optParser.getOptionValue(optEndDate))!=null) config.END_DATE = option;
		if((option =(String)optParser.getOptionValue(optBaseDB))!=null) config.DB_BASEDB = option;
		if((option =(String)optParser.getOptionValue(optLogPath))!=null) config.LOG_PATH = option;
		if((option =(String)optParser.getOptionValue(optCachePath))!=null) config.CACHE_PATH = option;
		if((option =(String)optParser.getOptionValue(optLimitCount))!=null) config.LIMIT_COUNT = Integer.parseInt(option);
		if((option =(String)optParser.getOptionValue(optWaitMinute))!=null) config.WAIT_MINUTE = Integer.parseInt(option);
		if((option =(String)optParser.getOptionValue(optDownSpeed))!=null) config.DOWN_SPEED = Integer.parseInt(option);
		
		
		//옵션 검토
//		DateTime start =DateUtil.getDate(config.START_DATE);
//		DateTime end = DateUtil.getDate(config.END_DATE);
//		DateTime now = DateTime.now();
//		
//		if (start.isAfterNow()==true) config = null;
//		if (end.isAfterNow()==true) config.END_DATE = DateUtil.getDateString(now);
		
		
		config.LOG_PATH = config.LOG_PATH.replace("\\\\", "\\");
		config.CACHE_PATH = config.CACHE_PATH.replace("\\\\", "\\");
		if(config.LOG_PATH.endsWith("\\")==false) config.LOG_PATH = config.LOG_PATH+"\\";
		if(config.CACHE_PATH.endsWith("\\")==false) config.CACHE_PATH = config.CACHE_PATH+"\\";
				
		return config;
	}
}
	