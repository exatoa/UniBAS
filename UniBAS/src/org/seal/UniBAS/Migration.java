package org.seal.UniBAS;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.UnknownOptionException;
import jargs.gnu.CmdLineParser.Option.StringOption;

import java.io.IOException;
import java.sql.SQLException;

import org.seal.UniBAS.Core.MigrationAdapter;
import org.seal.UniBAS.Core.MigrationWorker;
import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Util.DateUtil;
import org.seal.UniBAS.Util.Config;
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
public class Migration {

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
			log.init(config.LOG_PATH + "log_mig_"+config.NAME+".txt");
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


		MigrationWorker main = new MigrationWorker(config);
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
		log.info("Schema="+config.NAME);
		log.info("Type="+config.TYPE);

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
		Option optName = new StringOption('n',"name");
		Option optType = new StringOption('t',"type");
		
		
		//System-----------------------------------------------------
		Option optLogPath = new StringOption('l', "logpath");
		Option optBaseDB = new StringOption('f', "basedb");

		
	
		//옵션등록
		optParser.addOption(optName);
		optParser.addOption(optType);
		optParser.addOption(optBaseDB);
		optParser.addOption(optLogPath);

		
		
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
		
		if((option =(String)optParser.getOptionValue(optName))!=null) config.NAME = option;
		if((option =(String)optParser.getOptionValue(optType))!=null) config.TYPE = option;
		
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
	