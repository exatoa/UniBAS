package org.sel.UnifiedBTS.Core;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.sel.UnifiedBTS.Core.Database.DBManager;
import org.sel.UnifiedBTS.Core.Database.SQLConnectionException;
import org.sel.UnifiedBTS.Core.Model.SiteInfo;
import org.sel.UnifiedBTS.Util.log;

public class AnalysisAdapter {
	
	private DBManager DB;
	private String DBType;
	
	public AnalysisAdapter(DBManager _db, String _type){
		DB = _db;
		DBType = _type;
	}	


	/**
	 * 분석 데이터 베이스 생성에 필요한 도구들을 추가
	 * @param dB_TYPE
	 * @return
	 */
	public boolean createAnalysisFoundation(String dB_TYPE)  throws SQLConnectionException{
		
		int ret = 0;
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RIM_analysis_instance.sql");
		if(file==null) return false;
    
		//SQL 쿼리들 실행
	    try {
	    	ret = this.executeSqlScript(file, null);
		    if(ret<=0) 	throw new IOException();
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	    
	    return true;
	}
	
	/**
	 * 분석 데이터 베이스 생성에 필요한 도구들을 추가
	 * @param dB_TYPE
	 * @return
	 */
	public boolean createAnalysisQueries(String dB_TYPE)  throws SQLConnectionException{
		
		int ret = 0;
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RIM_analysis_query.sql");
		if(file==null) return false;
    
		//SQL 쿼리들 실행
	    try {
	    	ret = this.executeSqlScript(file, null);
		    if(ret<=0) 	throw new IOException();
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	    
	    return true;
	}

	/**
	 * 타겟에 대한 DB들을 생성. 적용된 데이터베이스 이름을 반환.
	 * @return
	 */
	public String createAnalysisDB(String _dbname, String _systemType) throws SQLConnectionException
	{
		//DB생성코드
		int ret = DB.createDB(_dbname);
		if (ret==0) return null;
		if (ret==2)	return _dbname;		//기존에 데이터베이스가 있는경우, 성공

		
		//DB 변경.
		String old = DB.changeDB(_dbname);
		if(old==null) 	return null;
		
		
		ret = 0;
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RIM_main.sql");
		if(file==null) return null;
    
		//SQL 쿼리들 실행
	    try {
	    	ret = this.executeSqlScript(file, new String[] {""});	//없앰.
		    if(ret<=0) 	throw new IOException();
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	    
	    ret = 0;
		file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RIM_TF.sql");
		if(file==null) return null;
    
		//SQL 쿼리들 실행
	    try {
	    	ret = this.executeSqlScript(file, null);
		    if(ret<=0) 	throw new IOException();
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	    
	    return DB.changeDB(old); //최근에 작업한 DB이름 (지금 생성된 DB명이 반환됨) 
	}

	
	/**
	 * 스크립트 파일에 있는 SQL문들을 실행
	 * @param _inputFile
	 * @return 성공한 스크립트 수를 반환. (치명적인 에러 발생시 -1)
	 */
	public int executeSqlScript(InputStream _inputFile, String[] args) throws SQLConnectionException{

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
	 * 분석인스턴스에 대한 요약 정보를 생성
	 * @param _analysisID
	 * @param _schema
	 * @param _siteID
	 * @param _projectID
	 * @return
	 */
	public int makeAnalysisSummary(int _analysisID, String _schema, int _siteID,	String _projectID)  throws SQLConnectionException
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("analysis_id", 	_analysisID);
		params.put("Schema_Name", 	_schema);
		params.put("site_id", 		_siteID);
		params.put("project_id", 	_projectID);
		
		int ret = DB.executeSP("makeAnalysisSummary", params);
		return ret;
	}


	/**
	 * 분석 인스턴스의 필드 매핑을 위하여 매핑정보 업데이트
	 * @param _siteID
	 * @param xml_str
	 * @return
	 */
	public int initializeFieldMap(int _siteID, String xml_str) throws SQLConnectionException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site_id", 	_siteID);
		params.put("xml", 		xml_str);
			
		int ret = DB.executeSP("initializeFieldMap", params);
		return ret;
	}


	/**
	 * 분석 인스턴스의 버그리포트에 대해 필드 매핑을 시행.
	 * @param _siteID
	 * @return
	 */
	public int initializeFieldType(int _siteID) throws SQLConnectionException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site_id", 	_siteID);
			
		int ret = DB.executeSP("initializeFieldType", params);
		return ret;		
	}


	/**
	 * 분석 인스턴스의 버그리포트에 대해 필드 매핑을 시행 초기가 아닌 두번째부터.  (이때는 RIM의 정보를 이용함)
	 * @param _siteID
	 * @return
	 */
	public int updateFieldType(int _siteID) throws SQLConnectionException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("site_id", 	_siteID);
			
		int ret = DB.executeSP("updateFieldType", params);
		return ret;		
	}




	
}
