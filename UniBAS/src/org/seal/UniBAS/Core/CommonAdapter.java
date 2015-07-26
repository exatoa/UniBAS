package org.seal.UniBAS.Core;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Core.Database.SQLConnectionException;
import org.seal.UniBAS.Core.Model.SiteInfo;
import org.seal.UniBAS.Util.log;

public class CommonAdapter {
	
	private DBManager DB;
	private String DBType;
	
	public CommonAdapter(DBManager _db, String _type){
		DB = _db;
		DBType = _type;
	}	

	
	/**
	 * 인자로 받은 DB이름으로 공통데이터베이스를 생성하고 기초 테이블 및 SP들을 생성
	 * @param _dbname
	 * @return 생성된DB이름
	 * @throws SQLException
	 */
	public String createCommonDB(String _dbname) throws SQLConnectionException
	{
		int ret = DB.createDB(_dbname);
		if (ret==0) return null;
		if (ret==2)
		{
			if(DB.changeDB(_dbname)==null)
		    	return null; 
			return _dbname;		//기존에 데이터베이스가 있는경우, 성공
		}
		
		//결과가 1인경우에는 테이블 생성 실행 
		if(DB.changeDB(_dbname)==null) return null;

		
		//관리자 모델 생성
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RIM_info.sql");
		if(file==null) return null;
		
	    //SQL 쿼리들 실행
	    try {
	    	ret = this.executeSqlScript(file, null);//ret = DB.executeSqlScript(file);
		    if(ret<=0) 	return null;
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	    
	    
	    //저장소 독립모델 생성
		file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RIM_main.sql");
		if(file==null) return null;
		
	    //SQL 쿼리들 실행
	    try {
	    	ret = this.executeSqlScript(file, new String[] {"identity(1,1)"});//ret = DB.executeSqlScript(file);
		    if(ret<=0) 	return null;
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	    
	    
	    //원래 DB로 설정.
	    if(DB.changeDB(_dbname)==null)
	    	return null; 
	    
	    return _dbname;
	}
	
	/**
	 * 타겟에 대한 DB들을 생성. 적용된 데이터베이스 이름을 반환.
	 * @return
	 */
	public String createTargetDB(String _dbname, String _systemType) throws SQLConnectionException
	{
		//DB생성코드
		int ret = DB.createDB(_dbname);
		if (ret==0) return null;
		if (ret==2)	return _dbname;		//기존에 데이터베이스가 있는경우, 성공

		
		//DB 변경.
		String old = DB.changeDB(_dbname);
		if(old==null) 	return null;
		
		
		ret = 0;
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RSM_"+_systemType.toLowerCase()+".sql");
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
//
//	/**
//	 * 타겟에 대한 DB들을 생성. 적용된 데이터베이스 이름을 반환.
//	 * @return
//	 */
//	public String createUnifiedDB(String _dbname)
//	{
//		//DB생성코드
//		int ret = DB.createDB(_dbname);
//		if (ret==0) return null;
//		if (ret==2)	return _dbname;		//기존에 데이터베이스가 있는경우, 성공
//
//		
//		//DB 변경.
//		String old = DB.changeDB(_dbname);
//		if(old==null) 	return null;
//		
//		
//		ret = 0;
//		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RIM_main.sql");
//		if(file==null) return null;
//    
//		//SQL 쿼리들 실행
//		ret = DB.executeSqlScript(file);
//	    if(ret<=0) 	return null;
//	    
//	    return DB.changeDB(old); //최근에 작업한 DB이름 (지금 생성된 DB명이 반환됨) 
//	}
//	
	/**
	 * 마이그레이션을 위한 코드 생성.
	 * @return
	 */
	public int createMigrationProcedure(String _dbname, String _systemType)
	{	
		//데이터 베이스 변경없이  현재 코드에서 수행.

		int ret = 0;
		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/"+DBType.toLowerCase()+"/RSM_"+_systemType.toLowerCase()+"_migration.sql");
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
	public int executeSqlScript(InputStream _inputFile, String[] args){

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
	 * 사이트에 대한 정보를 저장.
	 */
	public int saveSiteInfo(SiteInfo _info)  throws SQLConnectionException
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", 			_info.Name);
		params.put("description",	_info.Desc);
		params.put("site_type",		_info.Type);
		params.put("sname", 		_info.SchemaName);
		params.put("base_url",		_info.BaseUrl);
		params.put("log_path",		_info.LogPath);
		params.put("cache_path",	_info.CachePath);
		
		int ret = DB.executeSP("saveSiteInfo", params);
		return ret;
	}
	
	public boolean updateSiteInfo(String _name, String _result)  throws SQLConnectionException
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", 			_name);
		params.put("status",		_result);
		
		int ret = DB.executeSP("updateSiteInfo", params);
		
		//에러 메세지 표시
		switch(ret)
		{
			case 1 : return true;
			case -1 : log.error("Invalid name " + _name); break;
		}
		
		return false;
	}


	/**
	 * 모든 Migration 프로시저를 삭제하는 함수.
	 * @param commonDBname
	 * @param type
	 * @return
	 */
	public int dropMigrationProcedure() {
		//모든 Migration으로 시작하는 프로시저를 삭제하는 쿼리.
		String sql = "Declare @resCnt as integer Declare @sql as nvarchar(max) Declare @params as nvarchar(256)= '@resCnt as int OUTPUT' DECLARE @pname AS NVARCHAR(256) DECLARE MIG_PROG CURSOR for select name from sys.objects where type='P' and name like N'Migration%' OPEN MIG_PROG FETCH NEXT FROM MIG_PROG INTO @pname WHILE (@@FETCH_STATUS = 0) BEGIN 	SET @sql = 'drop procedure '+@pname EXEC sp_executesql @sql, @params, @resCnt OUTPUT FETCH NEXT FROM MIG_PROG INTO @pname END CLOSE MIG_PROG DEALLOCATE MIG_PROG";
		
		return DB.executeSqlScript(sql);
	}
}
