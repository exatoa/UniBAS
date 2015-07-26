package org.seal.UniBAS.Core;

import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Core.Database.SQLConnectionException;
import org.seal.UniBAS.Util.Config;
import org.seal.UniBAS.Util.log;

public class MigrationWorker {


	protected Config Setting = null;	//설정값
	protected DBManager DB = null;		//사용할 데이터베이스
	protected MigrationAdapter Adapter = null;
	
	
	protected int SiteID = 0;			//작업중인 Site ID;
	protected int AnalysisID = 0;		//작업중인 Site ID;

	public MigrationWorker(Config _config)
	{	
		Setting = _config;
		DB = DBManager.getInstance();
	}

		
	public boolean run()
	{
		try{
			//1.준비=======================================================
			Adapter = new MigrationAdapter(DB, Setting.DB_TYPE);
			
			//기본 DB에서 시작
			DB.changeDB(Setting.DB_BASEDB);
	
			
			//사이트 아이디 구하기
			SiteID = Adapter.getSiteID(Setting.NAME);
			if(SiteID<=0){
				log.error("잘못된 스키마명 : " + Setting.NAME);
				return false;
			}
			log.info("SiteID = " + SiteID);
			
			
			//이미 존재하는 사이트의 경우 모두 삭제하고 다시시작.
			log.info("removing old data in site_id=" + SiteID+"...");
			int ret = Adapter.removeSiteData(SiteID);
			if(ret<=0){
				log.error("기존 스키마 데이터 삭제중 에러 : " + Setting.NAME);
				return false;
			}
			log.info("Done");
			
			
			//RSM 스키마에서 작업
			DB.changeDB(Setting.NAME);
			

			//마이그레이션 도구 삭제
			if(Adapter.dropMigrationProcedure()==0){
				log.error("migration_프로시저 삭제 실패");
				return false;
			}
			log.info("기존의 migration_프로시저 삭제 성공");
						
			//mssql_bugzilla_migration.sql 외부 파일을 실행.  (마이그레이션에 필요한 도구)
			if(Adapter.createMigrationFoundation(Setting.DB_BASEDB, Setting.TYPE)==0){
				log.error("mssql_bugzilla_migration.sql 생성 실패");
				return false;
			}
			log.info("마이그레이션에 필요한 도구 생성 성공");
		
			
			
			//테이블 이동
			String[] tables = {"bug","comment","attachment","attach_data","history","relationship","reference","monitor","user","project","component","keyword","bug_keyword","additional_info","status","resolution","severity","priority","platform","os","version","milestone"};
			for(String table :tables)
			{
				log.info("migration_"+table+" 이동 시작");
				if(Adapter.execMigrationSP(table, SiteID)<=0){
					log.error("migration_"+table+" 이동 실패");
					return false;
				}
				log.info("migration_"+table+" 이동 성공");
			}
	
			//마이그레이션 도구 삭제
			if(Adapter.dropMigrationProcedure()==0){
				log.error("migration_프로시저 삭제 실패");
				return false;
			}
			log.info("migration_프로시저 삭제 성공");
			
			
			//기본 DB에서 마무리
			DB.changeDB(Setting.DB_BASEDB);
	
			
			//요약정보 생성
			if(Adapter.makeSummary(SiteID)==0){
				log.error("요약정보 생성 실패");
				return false;
			}
			log.info("요약정보 생성 성공");
			
			
			
			//updateSiteInfoMig
			if(Adapter.updateSiteInfoMig(SiteID)==0){
				log.error("상태정보 업데이트 실패");
				return false;
			}
			log.info("상태정보 업데이트 성공");
		}
		catch(SQLConnectionException e)
		{
			log.error("SQLConnectionExceptionOccured ErrCode="+e.getErrorCode()+"; ErrMsg="+e.getMessage());
			return false;
		}
		
		return true;
	}
	
}
