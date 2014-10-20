package org.sel.UnifiedBTS.Core;

import org.sel.UnifiedBTS.Core.Database.DBManager;
import org.sel.UnifiedBTS.Core.Database.SQLConnectionException;
import org.sel.UnifiedBTS.Exception.ControllException;
import org.sel.UnifiedBTS.Util.Config;
import org.sel.UnifiedBTS.Util.log;

public class AnalysisWorker {

	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	//   non- static 영역
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	protected Config Setting = null;	//설정값
	protected DBManager DB = null;		//사용할 데이터베이스
	protected AnalysisAdapter Adapter = null;
	
	
	protected int SiteID = 0;			//작업중인 Site ID;
	protected int AnalysisID = 0;		//작업중인 Site ID;

	public AnalysisWorker(Config _config)
	{	
		Setting = _config;
		DB = DBManager.getInstance();
	}

		
	public boolean run()
	{
		boolean retFlag = true;
		try
		{
			
			//1.준비=======================================================
			Adapter = new AnalysisAdapter(DB, Setting.DB_TYPE);
			
			//기본 데이터베이스에서 시작.
			DB.changeDB(Setting.DB_BASEDB);
			
			
			//mssql_analysis.sql 외부 파일을 실행.  (분석모델 생성에 필요한 도구)
			if(Adapter.createAnalysisFoundation(Setting.DB_TYPE)==false){
				throw new ControllException("mssql_analysis_create.sql 생성 실패");
			}
			log.info("분석모델 생성에 필요한 도구 생성 성공");
	
			
			//2.분석 DB 생성=======================================================
			//mssql_unified.sql 외부 파일을 실행.  (분석모델 담겨있음.)
			//mssql_unified_TF.sql 외부 파일을 실행.  (TF모델 담겨있음.)
			if(Adapter.createAnalysisDB(Setting.NAME, Setting.DB_TYPE)==null){
				throw new ControllException("analysis DB 생성 실패 : "+Setting.NAME);
			}
			log.info("analysis DB 생성 성공 : "+Setting.NAME);
			
			
			//3.분석정보 생성=======================================================
			AnalysisID = Adapter.saveAnalysisInfo(Setting.NAME, Setting.DESC, Setting.NAME, Setting.IS_UNIFORMLY, Setting.START_DATE, Setting.END_DATE, Setting.CONDITION);
			if (AnalysisID <=0){
				throw new ControllException("analysis 정보등록 실패 : "+Setting.NAME);
			}
			log.info("AnalysisID : "+AnalysisID);
					
			
			//이미 이동된 데이터인지 검토
			//TODO
			
			//4.데이터 이동 시작 (오래걸림)
			log.info("데이터 이동 시작 : "+AnalysisID);
			int ret = Adapter.moveAnalysis(Setting.NAME, Setting.SITE_ID, Setting.PROJECT_ID, Setting.START_DATE, Setting.END_DATE, Setting.CONDITION);
			if (ret <=0){
				throw new ControllException("데이터 이동 실패 : "+Setting.NAME);
			}
			log.info("데이터 이동 완료 : "+AnalysisID);
		
			
			//5. 요약정보 생성
			log.info("요약 정보 생성 시작 : "+AnalysisID);
			ret = Adapter.makeAnalysisSummary(AnalysisID, Setting.NAME, Setting.SITE_ID, Setting.PROJECT_ID);
			if (ret <=0){
				throw new ControllException("요약 정보 생성 실패 : "+AnalysisID);
			}
			log.info("요약 정보 생성 완료 : "+AnalysisID);
	
		
			//6. 필드 정보 매핑
			//초기 매핑정보 생성.
			String xml_str = "";
			xml_str += "<Map>";
			xml_str += "<E type=\"severity\"	old=\"trivial\"		new=\"minor\"		/>";
			xml_str += "<E type=\"severity\"	old=\"major\"		new=\"normal\"	/>";
			xml_str += "<E type=\"priority\"	old=\"P2\"			new=\"P1\"			/>";
			xml_str += "<E type=\"priority\"	old=\"P4\"			new=\"P5\"			/>";
			xml_str += "<E type=\"resolution\"	old=\"EXPIRED\"		new=\"FIXED\"		/>";
			xml_str += "<E type=\"resolution\"	old=\"MOVED\"		new=\"FIXED\"	/>";
			xml_str += "<E type=\"resolution\"	old=\"INVALID\"		new=\"INCOMPLETE\"	/>";
			xml_str += "<E type=\"resolution\"	old=\"WONTFIX\"		new=\"FIXED\"		/>";
			xml_str += "<E type=\"resolution\"	old=\"WORKSFORME\"	new=\"INCOMPLETE\"	/>";
			xml_str += "<E type=\"status\"		old=\"ASSIGNED\"	new=\"OPEN\"	/>";
			xml_str += "<E type=\"status\"		old=\"CLOSED\"		new=\"CLOSED\"		/>";
			xml_str += "<E type=\"status\"		old=\"NEW\"			new=\"OPEN\"		/>";
			xml_str += "<E type=\"status\"		old=\"REOPENED\"	new=\"OPEN\"	/>";
			xml_str += "<E type=\"status\"		old=\"RESOLVED\"	new=\"CLOSED\"	/>";
			xml_str += "<E type=\"status\"		old=\"UNCONFIRMED\"	new=\"OPEN\"		/>";
			xml_str += "<E type=\"status\"		old=\"VERIFIED\"	new=\"CLOSED\"	/>";
			xml_str += "</Map>";
			
			
			//데이터 베이스 변경
			DB.changeDB(Setting.NAME);
			log.info("Change DB : "+ Setting.NAME);
			
			//매핑정보 설정
			Adapter.createAnalysisQueries(Setting.DB_TYPE);
			log.info("created quries in "+ Setting.NAME);
				
			//매핑정보 설정
			Adapter.initializeFieldMap(Setting.SITE_ID, xml_str);
			Adapter.initializeFieldType(Setting.SITE_ID);
			log.info("updated Field Mapping: "+ Setting.NAME);
			
			//데이터 베이스 다시 복귀
			DB.changeDB(Setting.DB_BASEDB);
			log.info("Change DB : "+ Setting.DB_BASEDB);
			
			//7. NLP 처리 프로세스 가동.
			//TODO : 파이썬 프로그램 실행.
			log.info("NLP : Python 프로그램을 실행 시켜주세요.");
	
			log.info("Analysis Create Done : "+ Setting.NAME);
		}
		catch(SQLConnectionException e)
		{
			log.error("SQL connection Problem Occured");
			log.printStackTrace(e);
			retFlag = false;
		}
		catch (ControllException e) {
			log.error("Working Error : "+ e.getMessage());
			log.printStackTrace(e);
			retFlag = false;
		}
		
		return retFlag;
	}
	
}
