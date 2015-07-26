package org.seal.UniBAS.Core;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Core.Database.SQLConnectionException;
import org.seal.UniBAS.Util.log;

public class MigrationAdapter {
	
	private DBManager DB;
	private String DBType;
	
	public MigrationAdapter(DBManager _db, String _type){
		DB = _db;
		DBType = _type;
	}	


	/**
	 * 마이그레이션에 필요한 함수들 생성.
	 * @param dB_TYPE 

	 * @return
	 */
	public int createMigrationFoundation(String _dbname, String _sysType) throws SQLConnectionException {
		int ret = 0;
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RSM_"+_sysType.toLowerCase()+"_migration.sql");
		if(file==null) return 0;
    
					
		//SQL 쿼리들 실행
		ret = this.executeSqlScript(file, new String[] {_dbname});
	    if(ret<=0) 	return 0;
	    
	    return 1;
	}


	
	
	
	
	/**
	 * 스크립트 파일에 있는 SQL문들을 실행
	 * @param _inputFile
	 * @return 성공한 스크립트 수를 반환. (치명적인 에러 발생시 -1)
	 */
	public int executeSqlScript(InputStream _inputFile, String[] args) throws SQLConnectionException {

	    // Delimiter
	    String delimiter = ";;";

	    // Create scanner
	    Scanner scanner = null;
	    try {
	        scanner = new Scanner(_inputFile,"UTF-8");
	        scanner.useDelimiter(delimiter);
	    }
	    catch(IllegalArgumentException e){
	    	log.printStackTrace(e);
	    	scanner = null;
	    }
	    if(scanner==null)
	    	return -1;

	    // Loop through the SQL file statements
	    int cnt = 0;
	    int err = 0;
	    int ret;
	    
	    while(scanner.hasNext()) {
	    	//스크립트의 파라메터 수정.
	    	String sql = scanner.next();
	    	
	    	//인자로 변수들을 치환.
	    	if(args!=null && args.length!=0)
	    	{
	    		for(int i=0; i<args.length; i++)
	    		{
	    			sql = sql.replaceAll("<\\{"+Integer.toString(i)+"\\}>", args[i]);
	    		}
	    	}
	    	ret = DB.executeSqlScript(sql);
	    	if(ret<0)	err+=1;
	    	else		cnt++;
	    }
	    scanner.close();
	    
	    //에러가 있으면 에러수  * -1;
	    if (err>0) 
	    	return -1*err;
	    else
	    	return cnt;
	}


	
	/**
	 * RIM으로 부터 분석 인스턴스로 데이터 이동.
	 * @param sCHEMA
	 * @param sITE_ID
	 * @param pROJECT_ID
	 * @param sTART_DATE
	 * @param eND_DATE
	 * @param cONDITION
	 */
	public int removeSiteData(int _siteID) throws SQLConnectionException
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site_id",		_siteID);
		
		int ret = DB.executeSP("deleteSite_RIM", params);
		return ret;
	}

	
	
	/**
	 * RIM으로 부터 분석 인스턴스로 데이터 이동.
	 * @param sCHEMA
	 * @param sITE_ID
	 * @param pROJECT_ID
	 * @param sTART_DATE
	 * @param eND_DATE
	 * @param cONDITION
	 */
	public int moveAnalysis(String _schema, int _siteID, String _projectID, String _sDate, String _eDate, String _cond) throws SQLConnectionException
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("SCHEMA_NAME", 	_schema);
		params.put("SITE_ID",		_siteID);
		params.put("PROJECT_ID",	_projectID);
		params.put("start_date", 	(_sDate!=null &&_sDate.length()==0)?null:_sDate);
		params.put("end_date",		(_eDate!=null &&_eDate.length()==0)?null:_eDate);
		params.put("condition",		(_cond!=null &&_cond.length()==0)?null:_cond);
		
		int ret = DB.executeSP("moveAnalysis", params);
		return ret;
	}


	
	/**
	 * 분석인스턴스에 대한 관리정보를 저장.
	 * @param _analysisID
	 * @param _schema
	 * @param _siteID
	 * @param _projectID
	 * @return
	 */
	public int saveAnalysisInfo(String _title, String _desc, String _schema, int _isUniformly, String _sDate, String _eDate, String _cond) throws SQLConnectionException 
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("TITLE", 		_title);
		params.put("DESCRIPTION", 	_desc);
		params.put("SCHEMA_NAME", 	_schema);
		params.put("IS_UNIFORMLY", 	_isUniformly);
		params.put("START_DATE", 	(_sDate!=null &&_sDate.length()==0)?null:_sDate);
		params.put("END_DATE",		(_eDate!=null &&_eDate.length()==0)?null:_eDate);
		params.put("CONDITION",		(_cond!=null &&_cond.length()==0)?null:_cond);
		
		int ret = DB.executeSP("saveAnalysisInfo", params);
		return ret;
	}


	/**
	 * 마이그레이션에 필요한 도구 생성
	 * @param _analysisID
	 * @param _schema
	 * @param _siteID
	 * @param _projectID
	 * @return
	 */
	public int execMigrationSP(String _tablename, int _siteID)  throws SQLConnectionException
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site_id", 	_siteID);
		
		int ret = DB.executeSP("Migration_"+_tablename, params);
		return ret;
	}
	

	/**
	 * 마이그레이션에 필요한 도구 삭제
	 * @param commonDBname
	 * @param type
	 * @return
	 */
	public int dropMigrationProcedure() throws SQLConnectionException{
		//모든 Migration으로 시작하는 프로시저를 삭제하는 쿼리.
		String sql = "Declare @resCnt as integer Declare @sql as nvarchar(max) Declare @params as nvarchar(256)= '@resCnt as int OUTPUT' DECLARE @pname AS NVARCHAR(256) DECLARE MIG_PROG CURSOR for select name from sys.objects where type='P' and name like N'Migration%' OPEN MIG_PROG FETCH NEXT FROM MIG_PROG INTO @pname WHILE (@@FETCH_STATUS = 0) BEGIN 	SET @sql = 'drop procedure '+@pname EXEC sp_executesql @sql, @params, @resCnt OUTPUT FETCH NEXT FROM MIG_PROG INTO @pname END CLOSE MIG_PROG DEALLOCATE MIG_PROG";
		
		return DB.executeSqlScript(sql);
	}
	
	


	/**
	 * 이동할 사이트의 아이디 얻기
	 * @param _schema
	 * @return
	 */
	public int getSiteID(String _schema)  throws SQLConnectionException{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("schema", 	_schema);
		
		int ret = DB.executeSP("getSiteID", params);
		return ret;
	}


	/**
	 * 이동한 사이트에 대한 요약정보 생성
	 * @param _siteID
	 * @return
	 */
	public int makeSummary(int _siteID) throws SQLConnectionException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site_id", 	_siteID);
		
		int ret = DB.executeSP("makeSummary", params);
		return ret;
	}


	/**
	 * 이동한 사이트의 정보 업데이트
	 * @param _siteID
	 * @return
	 */
	public int updateSiteInfoMig(int _siteID) throws SQLConnectionException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site_id", 	_siteID);
		
		int ret = DB.executeSP("updateSiteInfoMig", params);
		return ret;
	}


	
}
