package org.seal.UniBAS;

import java.io.IOException;
import java.sql.SQLException;

import org.seal.UniBAS.Core.MigrationWorker;
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
			log.init(config.LOG_PATH + "log_mig_"+config.SYS_NAME+".txt");
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
}
	