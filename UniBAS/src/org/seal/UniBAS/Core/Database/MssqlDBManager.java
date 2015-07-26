package org.seal.UniBAS.Core.Database;


import java.io.InputStream;
import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.joda.time.DateTime;
import org.seal.UniBAS.Util.log;


public class MssqlDBManager extends DBManager {

	
	public MssqlDBManager(String _servAddr, String _servPort, String _id, String _pw)
	{
		super(_id, _pw);
		
		ClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		ConnStr = "jdbc:sqlserver://{0}:{1};";//DatabaseName={2}
		ConnStr = ConnStr.replaceFirst("\\{0\\}", _servAddr);
		ConnStr = ConnStr.replaceFirst("\\{1\\}", _servPort);
		//ConnStr = ConnStr.replaceFirst("\\{2\\}", _dbName);
		DBName = "";
	}


	/**
	 * 새로운 이름의 데이터베이스를 생성함.
	 * @return 1이면 정상적으로 생성, 2이면 이미 존재함, 0이면 실패.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	@Override
	public synchronized int createDB(String _dbname)
	{
		int ret = 1;
		String sql = "CREATE DATABASE ";
		Statement  stmt = null;
		
		
		try{
			stmt = Conn.createStatement();
			sql = sql + _dbname;			
			stmt.executeUpdate(sql);
		}
		catch (SQLException e){
			if (e.getErrorCode()==1801)  //이미 DB가 있는경우.
			{
				ret = 2;
			}
			else
				ret = 0;

		}finally{
			try {
				stmt.close();
			} catch (SQLException e) {
				ret = 0;
				log.printStackTrace(e);
			}
		}
		
		return ret;
	}
	
	/**
	 * 해당 이름의 데이터 베이스 삭제
	 * @return 1이면 정상적으로 삭제, 0이면 실패.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	@Override
	public synchronized int dropDB(String _dbname)
	{
		int ret = 1;
		String sql = "USE MASTER "
					+"\nIF EXISTS (SELECT * FROM sys.databases WHERE name = '" + _dbname +"')"
				    +"\nBEGIN"
					+"\n   ALTER DATABASE " + _dbname + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE"
					+"\n   DROP DATABASE " +_dbname
					+"\nEND";
							
		Statement  stmt = null;
		try{
			stmt = Conn.createStatement();
			stmt.executeUpdate(sql);
		}
		catch (SQLException e){
			if (e.getErrorCode()==1801)  //이미 DB가 있는경우.
			{
				ret = 2;
			}
			else
				ret = 0;

		}finally{
			try {
				stmt.close();
			} catch (SQLException e) {
				ret = 0;
				//log.printStackTrace(e);
			}
		}
		
		return ret;
	}
	
	/**
	 * 데이터베이스를 변경함
	 * @param _dbname
	 * @return 변경전의 DB명을 반환.
	 * @throws SQLException
	 */
	@Override
	public synchronized String changeDB(String _dbname) throws SQLConnectionException
	{
		String sql = "USE ";
		Statement stmt = null;
		
		int ret=0;
		
		try {
			stmt = Conn.createStatement();
			ret = stmt.executeUpdate(sql+_dbname);		//return이 0이면 실행 에러.
		} catch (SQLException e) {
			//에러메세지 체크 (네트웍 연결문제인 경우 에러 상위로 패스)
			String msg = e.getMessage();
			if( msg.toLowerCase().contains("connection")==true || msg.toLowerCase().contains("shutdown")==true)
				throw new SQLConnectionException();
			
			log.error("Error Query = "+ sql+"; DB="+_dbname);
			log.printStackTrace(e);
			ret =-1;
		}
		finally
		{
			try {
				if (stmt!=null) stmt.close();
			} catch (SQLException e) {
				log.printStackTrace(e);
				ret = 2;
			}
		}
		
		if(ret>=0)
		{
			log.error("Insert Error : "+ ret);
			return null;
	    }

		//기존의 DB명을 반환.
		String old = DBName;
		DBName = _dbname;
		return old;
	}
	
	/**
	 * 스크립트 파일에 있는 SQL문들을 실행
	 * @param _script 스크립트 문장
	 * @return 성공한 스크립트 수를 반환. (치명적인 에러 발생시 -1)
	 */
	@Override
	public synchronized int executeSqlScript(String _script){
	    Statement currentStatement = null;
	    int ret = 1;
        try {
            // Execute statement
            currentStatement = Conn.createStatement();
            currentStatement.execute(_script);
        } catch (SQLException e) {
        	if(e.getErrorCode()==2714)
        		ret=0;
        	else{
        		log.error("Follow Code has error : " + e.getMessage()+ "\n" +_script);
            	//log.printStackTrace(e);
            	ret= -1;
        	}
            
        } finally {
            // Release resources
            if (currentStatement != null) {
                try {
                    currentStatement.close();
                } catch (SQLException e) {
                    log.printStackTrace(e);
                    ret= -1;
                }
            }
            currentStatement = null;
        }
        return ret;
    }

	/**
	 * 스크립트 파일에 있는 SQL문들을 실행
	 * @param _inputFile 스크립트를 포함한 파일.
	 * @return 성공한 스크립트 수를 반환. (치명적인 에러 발생시 -1)
	 */
	@Override
	public synchronized int executeSqlScript(InputStream _inputFile){

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
	    
	    Statement currentStatement = null;
	    while(scanner.hasNext()) {

	        // Get statement 
	        String rawStatement = scanner.next() + ";";
	        try {
	            // Execute statement
	            currentStatement = Conn.createStatement();
	            currentStatement.execute(rawStatement);
	            cnt++;
	        } catch (SQLException e) {
	        	log.print(rawStatement);
	            log.printStackTrace(e);
	            err++;
	        } finally {
	            // Release resources
	            if (currentStatement != null) {
	                try {
	                    currentStatement.close();
	                } catch (SQLException e) {
	                    log.printStackTrace(e);
	                }
	            }
	            currentStatement = null;
	        }
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
	@Override
	public synchronized int executeSP(String _spname, Map<String, Object> _params) throws SQLConnectionException
	{
		String sql = "{? = call ";
		sql += _spname + " (";
		sql += convertParameter(_params) + ")}";
	    
	    //Query 문 실행.
	    CallableStatement stmt = null;
		int ret=0;

		try{
			//setDiff();
			stmt = Conn.prepareCall(sql);
			stmt.registerOutParameter(1, java.sql.Types.INTEGER);
			stmt.execute();
			ret = stmt.getInt(1);
			//log.warn("[diff] "+ _spname + " : "+getDiff()+"ms");
		}
		catch(SQLException e)
		{
			//에러메세지 체크 (네트웍 연결문제인 경우 에러 상위로 패스)
			String msg = e.getMessage();
			if( msg.toLowerCase().contains("connection")==true || msg.toLowerCase().contains("shutdown")==true)
				throw new SQLConnectionException();
			
			log.error("Error Query = "+ sql);
			log.printStackTrace(e);
			ret =-1;
		}
		finally
		{
			try {
				if(stmt!=null) stmt.close();
			} catch (Exception e) {
				log.error("Error : stmt.close(); in querying " + sql);
				log.printStackTrace(e);
				ret =-1;
			}
		}
	    
		return ret;
	}


	
	/**
	 * Stored Procedure를 실행하는 프로시저 (결과로 문자열을 반환)
	 */
	@Override
	public synchronized String executeSPStr(String _spname, Map<String, Object> _params) throws SQLConnectionException {
		//Call Script 생성.
		String sql = "{ call ";
		sql += _spname + " ( ?";
		if(_params.size()>0)  sql += ", ";
		sql += convertParameter(_params) + ")}";
	    

	    //Query 문 실행.
	    CallableStatement stmt = null;
		String ret="";
			
		try{
			//setDiff();
			stmt = Conn.prepareCall(sql);
			stmt.registerOutParameter(1, java.sql.Types.NVARCHAR);
			stmt.execute();
			ret = stmt.getString(1);
			//log.warn("[diff] "+ _spname + " : "+getDiff()+"ms");
		}
		catch(SQLException e)
		{
			//에러메세지 체크 (네트웍 연결문제인 경우 에러 상위로 패스)
			String msg = e.getMessage();
			if( msg.toLowerCase().contains("connection")==true || msg.toLowerCase().contains("shutdown")==true)
				throw new SQLConnectionException();
			
			log.error("Error Query = "+ sql);
			log.printStackTrace(e);
			ret = null;
		}
		finally
		{
			try {
				if(stmt!=null) stmt.close();
			} catch (Exception e) {
				log.error("Error : stmt.close(); in querying " + sql);
				log.printStackTrace(e);
				ret = null;
			}
		}
	    
		return ret;
	}
	
	/**
	 * 파라메터에서 값을 가져와서 Procedure의 파라메터로 변경.
	 * @param _params
	 * @return 변경된 파라메터 문자열.
	 */
	public String convertParameter(Map<String, Object> _params) {
		String param="";
		
	    Iterator<String> keys = _params.keySet().iterator();
	    while(keys.hasNext()){
	    	String key = keys.next();
	    	Object value = _params.get(key);
	    	
	    	if(value==null) continue;
	    	
	    	String type = value.getClass().getSimpleName();	    	
	    	
	    	if (type.compareTo("String")==0){
	    		if (key==null)	continue;
	    		param += "@" + key + "= N'" +  value.toString()+"', ";
	    		continue;
	    	}
	    	
	    	if (type.compareTo("Integer")==0){
	    		if ((Integer)value == -1)	continue;
	    	}	
	    	param += "@" + key + "=" +  value.toString() + ", ";
	    }
	    if(param.length()!=0)	//마지막 파라메터의 ", "를 삭제
	    	param = param.substring(0, param.length()-2);
	    
	    return param;
	}
	
	

	
	/**
	 * 수동 트랜젝션 설정
	 */
	public synchronized void setTransaction() {
		
		try {
			Conn.setAutoCommit(false);
		} catch (SQLException e) {
			log.printStackTrace(e);
		}
		Transaction = true;
	}
	
	/**
	 * 수동 트랜젝션 커밋
	 */
	public synchronized void commit() {
		//This commits the transaction and starts a new one.
		try {
			Conn.commit();
			Conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.printStackTrace(e);
		} 
		Transaction = false;
	}
	
	/**
	 * 수동 트랜젝션 롤백
	 */
	public synchronized void rollback() {
		try {
			Conn.rollback();
			Conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.printStackTrace(e);
		}
		Transaction = false;
	}


	/**성능테스트**/
	private DateTime prev = null;
	private void setDiff()
	{
		prev = DateTime.now();		
	}
	private long getDiff()
	{
		DateTime now = DateTime.now();
		long diffInMillis =  now.getMillis() - prev.getMillis();
		return diffInMillis;		
	}

	
	
}
