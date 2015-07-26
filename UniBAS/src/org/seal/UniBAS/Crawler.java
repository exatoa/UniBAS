package org.seal.UniBAS;


import java.io.IOException;
import java.sql.SQLException;

import org.seal.UniBAS.Bugzilla.BugzillaPreWorker;
import org.seal.UniBAS.Bugzilla.BugzillaWorker;
import org.seal.UniBAS.Core.Controller;
import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Core.Network.WebCacheFile;
import org.seal.UniBAS.Mantis.MantisWorker;
import org.seal.UniBAS.Util.DateUtil;
import org.seal.UniBAS.Util.Settings;
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
public class Crawler
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
		

	    //1. 설정 파일 로드.
		Settings config = Settings.getInstance();
		if(Settings.Initialized == true) 
		{
			System.out.println("Made default Settings to Settings.json file.");
			System.out.println("Check your folder. And Change your own settings.");
			return;
		}
		else if(config==null)
		{
			System.out.println("Error! Setting is invalid");
			return;
		}
		//설정 출력
		
		config.printSettings();
		
		//2. 로그 파일 설정=======================================================
		try {
			log.init(config.LOG_PATH + "log_"+config.SYS_NAME+".txt");
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
		web.setCacheNamesize(config.CACHE_NAMESIZE);
		
		
		//4. 선택된 데이터 베이스 생성. (Type 에 따라서 데이터베이스 생성)
		DBManager db = null;
		db = DBManager.getInstance(config.DB_TYPE, config.DB_ADDR, config.DB_PORT, config.DB_ID, config.DB_PW);
		
		
		
		//5. 조건에 맞는 컨트롤러 생성.=======================================================
		Controller controller = null;
		if(config.SYS_TYPE.toLowerCase().compareTo("bugzilla")==0){
			//TODO::이건 무슨 코드지? 전처리가 pre이면....뭔가 preworker를 실행한다.?
			if(config.DB_PREFIX.toLowerCase().compareTo("pre")==0)
				controller= new BugzillaPreWorker(config);
			else
				controller= new BugzillaWorker(config);
		}
		else if(config.SYS_TYPE.toLowerCase().compareTo("mantis")==0)
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

	}
}
	