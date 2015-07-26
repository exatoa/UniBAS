package org.seal.UniBAS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.seal.UniBAS.Core.Model.SiteInfo;
import org.seal.UniBAS.Core.Network.WebCacheFile;
import org.seal.UniBAS.Trac.TracClient;
import org.seal.UniBAS.Trac.Model.BugReport;
import org.seal.UniBAS.Util.Settings;
import org.seal.UniBAS.Util.log;

public class CheckTrac {

	public static FileWriter writer = null;
	public static void main(String[] args) {
		

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
			log.init(config.LOG_PATH + "log_Trac.txt");
		} catch (IOException e){ 
			log.printStackTrace(e);
			return;
		}
		
		
		File file = new File(config.LOG_PATH+"result_Trac_twisted.txt");
		try {
			writer = new FileWriter(file);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		//1. 로그 파일 설정=======================================================
		try {
			log.init(config.LOG_PATH + "log_Trac_twisted.txt");
		} catch (IOException e){ 
			log.printStackTrace(e);
			return;
		}

		
		//2. WebFile클래스를 위한 환경설정.
		WebCacheFile web = WebCacheFile.getInstance();
		web.setLocalProxy(config.CACHE_PATH);
		web.setLimitation(config.LIMIT_COUNT, config.WAIT_MINUTE);
		web.setDownloadSpeed(config.DOWN_SPEED);
		web.setCacheLevel(config.CACHE_LEVEL);
		web.setCacheNamesize(config.CACHE_NAMESIZE);
		
		
		
		try{
			SiteInfo info = new SiteInfo("Twisted", "trac_twisted", "", "Trac", "https://twistedmatrix.com/trac/", "", "", "", "");
			checkList(info, 7100);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	public static void checkList(SiteInfo _info, int _max) throws IOException
	{
		int count = 0;
		TracClient client = new TracClient(_info);
		BugReport report = null;
		
		log.info("------------------"+_info.Name+" List-----------------------------");
		writer.write("------------------"+_info.Name+" List-----------------------------\n");
		for(int i=1; i<_max; i++)
		{
			report = client.getBugreport(i);
			
			if(report==null){
				log.error(_info.Name +"| ID=" + i+", None");
				writer.write(_info.Name +"| ID=" + i+", None"+"\n");
			}
			else
			{
				if(report.ID<0)
				{
					log.error(_info.Name +"| ErrorCode=" + report.ID +", Msg="+report.Date);
					writer.write(_info.Name +"| ErrorCode=" + report.ID +", Msg="+report.Date+"\n");
				}
				else
				{
					log.info(_info.Name +"| ID=" + report.ID +", Date="+report.Date);
					writer.write(_info.Name +"| ID=" + report.ID +", Date="+report.Date+"\n");
					count++;
				}
			}
			writer.flush();
		}
		log.info(_info.Name +"| Total Count = "+count);
		writer.write(_info.Name +"| Total Count = "+count+"\n");
		writer.write("\n\n");
		
	}

}
