package org.seal.UniBAS;

import java.io.IOException;
import java.sql.SQLException;

import org.joda.time.DateTime;
import org.seal.UniBAS.Core.AnalysisWorker;
import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Util.DateUtil;
import org.seal.UniBAS.Util.Settings;
import org.seal.UniBAS.Util.log;


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
			String timeStr = DateUtil.getDateString(DateTime.now(), "yyyyMMdd_hhmmss");			
			log.init(config.LOG_PATH + "analysislog_"+config.AS_NAME+"_"+timeStr+".txt");
		} catch (IOException e){ 
			log.printStackTrace(e);
			return;
		}
		
		
		//3. 선택된 데이터 베이스 생성. (Type 에 따라서 데이터베이스 생성)
		DBManager db = null;
		db = DBManager.getInstance(config.DB_TYPE, config.DB_ADDR, config.DB_PORT, config.DB_ID, config.DB_PW);


		
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
}
	