package org.seal.UniBAS.Core;

import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Core.Database.SQLConnectionException;
import org.seal.UniBAS.Core.Model.SiteInfo;
import org.seal.UniBAS.Core.Network.WebCacheFile;
import org.seal.UniBAS.Core.Exception.ControllException;
import org.seal.UniBAS.Util.Settings;
import org.seal.UniBAS.Util.log;


/**
 * 공통적인 네트워크, 데이터베이스에 대한 생성 및 관리를 담당.
 * 추상클래스로서 실제 작업하는 일은 하위 클래스에서 작성.
 * 구체 클래스는 run메소드를 구현하여 실제 크롤링 작업을 작성한다.
 * 
 * @author Zeck
 *
 */
public abstract class Controller {

	protected Settings Setting = null;	//설정값
	protected DBManager DB = null;		//사용할 데이터베이스
	protected WebCacheFile Web = null;	//네트워크 연결 객체
	protected int SiteID = 0;			//작업중인 Site ID;
	
	public Controller(Settings _config)
	{
		Setting = _config;
		
		Web = WebCacheFile.getInstance();
		
		DB = DBManager.getInstance();
	}

	
	public boolean run()
	{
		boolean retFlag = true;
		String resMsg = "";
		try{
			//1.관리 DB 생성=======================================================
			String commonDBname = Setting.DB_BASEDB;
			CommonAdapter common = new CommonAdapter(DB, Setting.DB_TYPE);
			
				
			if(common.createCommonDB(commonDBname)==null){		//시스템 초기화 (Common DB생성)
				throw new ControllException(1, "Common DB initial error!!!");
			}
			log.info( "Common DB initialized!!");
			
			
	
			//2. 대상 정보 저장======================================================
			String targetDBname = Setting.SYS_TYPE +"_" +Setting.SYS_NAME;
			//String unifiedDBname = Setting.DB_PREFIX +"_" +Setting.NAME;
	
			SiteInfo info = new SiteInfo(Setting.SYS_NAME, targetDBname,Setting.SYS_DESC, Setting.SYS_TYPE, Setting.SYS_URL, Setting.SYS_ID, Setting.SYS_PW,  Setting.LOG_PATH,  Setting.CACHE_PATH);
			SiteID =common.saveSiteInfo(info);
			if(SiteID<0)
			{
				throw new ControllException(1, "Invalidate system type!!");
			}
			else
				log.info("Saved the target information \""+Setting.SYS_NAME+"\"");		
	
			
			
			//3. 대상 DB생성.======================================================
			targetDBname = common.createTargetDB(targetDBname, Setting.SYS_TYPE);	//시스템 초기화.
			if(targetDBname==null){
				throw new ControllException(1, "target DB create error!!");
			}
			log.info("Target DB \""+targetDBname+"\" initialized!");
			
				
		
			//4. 대상 DB로 작업상태 전환.
			if(DB.changeDB(targetDBname)==null){
				throw new ControllException(1, "Change targetDB Error \""+targetDBname+"\"");
			}
			

			//5. 시스템에서의 작업
			retFlag = workTarget(info);
			if(retFlag==true)	resMsg = "PSM Completed";
			else				resMsg = "PSM Failed";
			
			
			//6. DB변경
			if(DB.changeDB(commonDBname)==null){
				throw new ControllException(1, "Change commonDB Error \""+commonDBname+"\"");
			}
			
			//7. 정보저장
			if(common.updateSiteInfo(info.Name, resMsg)== false) {
				throw new ControllException(1, "SiteInfo update error!");
			}
						
			//8. 실패시 처리.
			if (retFlag==false)
				throw new ControllException(1, resMsg);
		}
		catch(SQLConnectionException e)
		{
			log.error("SQL connection Problem Occured");
			log.printStackTrace(e);
			retFlag = false;
		}
		catch(ControllException e)
		{
			log.error("Working Error : "+ e.getMessage());
			log.printStackTrace(e);
			retFlag = false;
		}
		
		return retFlag;
		
	}
	


	/**
	 * 실제 컨트롤러 작동 로직. (추상메소드)
	 * @return
	 */
	public abstract boolean workTarget(SiteInfo _info);
}
