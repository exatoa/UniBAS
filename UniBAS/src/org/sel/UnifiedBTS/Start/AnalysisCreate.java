package org.sel.UnifiedBTS.Start;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.UnknownOptionException;
import jargs.gnu.CmdLineParser.Option.StringOption;





import java.io.IOException;
import java.sql.SQLException;

import org.joda.time.DateTime;
import org.sel.UnifiedBTS.Core.AnalysisWorker;
import org.sel.UnifiedBTS.Core.Database.DBManager;
import org.sel.UnifiedBTS.Util.Config;
import org.sel.UnifiedBTS.Util.DateUtil;
import org.sel.UnifiedBTS.Util.log;


/////////////////////////////////////////////////////////////////
//  정적 필드의 정의
/////////////////////////////////////////////////////////////////
/**
 * 지정된 옵션을 분석하고 작업을 지시.
 * 
 * 
 * @author Zeck
 */
public class AnalysisCreate
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
		
		//2. 로그 파일 설정=======================================================
		try {
			String timeStr = DateUtil.getDateString(DateTime.now(), "yyyyMMdd_hhmmss");			
			log.init(config.LOG_PATH + "analysislog_"+config.NAME+"_"+timeStr+".txt");
		} catch (IOException e){ 
			log.printStackTrace(e);
			return;
		}
		
		printConfig(config);
		
		//3. 선택된 데이터 베이스 생성. (Type 에 따라서 데이터베이스 생성)
		DBManager db = null;
		db = DBManager.getInstance(config.DB_TYPE, config.DB_URL, config.DB_PORT, config.DB_ID, config.DB_PW);


		
		//4.프로그램 실행=======================================================
		log.info("------------------------------------------");
		log.info("Start Program (" +""+ DateUtil.getNowString() + ")" );


		AnalysisWorker main = new AnalysisWorker(config);
		main.run();
		
		
		
		//마무리 정리.
		try {
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
		log.info("Title="+config.NAME);
		log.info("Desc="+config.DESC);
		//log.info("Schema="+config.SCHEMA);
		log.info("SiteID="+config.SITE_ID);
		log.info("PorjectID="+config.PROJECT_ID);
		log.info("Uniformly="+config.IS_UNIFORMLY);
		log.info("StartDate="+config.START_DATE);
		log.info("EndDate="+config.END_DATE);
		log.info("Condition="+config.CONDITION);
		log.info("");
		log.info("[System]");
		log.info("LogPath="+config.LOG_PATH);
		
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
		Option optTitle = new StringOption('n',"name");		//schema 명칭도됨.
		Option optDesc = new StringOption('d',"desc");
		//Option optSchema = new StringOption('a',"schema");
		Option optSiteID = new StringOption('s',"siteid");
		Option optProjects = new StringOption('p', "projectid");
		Option optUniformly = new StringOption('u',"isuniformly");
		Option optStartDate = new StringOption('b', "begindate");
		Option optEndDate = new StringOption('e', "enddate");
		Option optCondition = new StringOption('c', "enddate");
		
		
		
		//System-----------------------------------------------------
		Option optLogPath = new StringOption('l', "logpath");
		Option optBaseDB = new StringOption('f', "basedb");

		
		
		//TODO:데이터베이스 관련 옵션
		//Option optDBType = new StringOptin("x","dbType");
		
		
		
		//옵션등록
		optParser.addOption(optTitle);
		optParser.addOption(optDesc);
		//optParser.addOption(optSchema);
		optParser.addOption(optSiteID);
		optParser.addOption(optProjects);
		optParser.addOption(optUniformly);
		optParser.addOption(optStartDate);
		optParser.addOption(optEndDate);
		optParser.addOption(optCondition);
		
		optParser.addOption(optLogPath);
		optParser.addOption(optBaseDB);

		
		
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
		
		if((option =(String)optParser.getOptionValue(optTitle))!=null) config.NAME = option;
		if((option =(String)optParser.getOptionValue(optDesc))!=null) config.DESC = option;
		//if((option =(String)optParser.getOptionValue(optSchema))!=null) config.SCHEMA = option;
		if((option =(String)optParser.getOptionValue(optSiteID))!=null) config.SITE_ID = Integer.parseInt(option);
		if((option =(String)optParser.getOptionValue(optProjects))!=null) config.PROJECT_ID = option;
		if((option =(String)optParser.getOptionValue(optUniformly))!=null) config.IS_UNIFORMLY = Integer.parseInt(option);
		if((option =(String)optParser.getOptionValue(optStartDate))!=null) config.START_DATE = option;
		if((option =(String)optParser.getOptionValue(optEndDate))!=null) config.END_DATE = option;
		if((option =(String)optParser.getOptionValue(optCondition))!=null) config.CONDITION = option;
		
		if((option =(String)optParser.getOptionValue(optBaseDB))!=null) config.DB_BASEDB = option;
		if((option =(String)optParser.getOptionValue(optLogPath))!=null) config.LOG_PATH = option;
		
		
		//옵션 검토
//		DateTime start =DateUtil.getDate(config.START_DATE);
//		DateTime end = DateUtil.getDate(config.END_DATE);
//		DateTime now = DateTime.now();
//		
//		if (start.isAfterNow()==true) config = null;
//		if (end.isAfterNow()==true) config.END_DATE = DateUtil.getDateString(now);
		
		
		config.LOG_PATH = config.LOG_PATH.replace("\\\\", "\\");
		if(config.LOG_PATH.endsWith("\\")==false) config.LOG_PATH = config.LOG_PATH+"\\";
				
		return config;
	}
}
	