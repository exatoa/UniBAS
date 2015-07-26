package test.unibas;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.sel.UnifiedBTS.Bugzilla.BugzillaWorker;
import org.sel.UnifiedBTS.Core.Controller;
import org.sel.UnifiedBTS.Core.Database.DBManager;
import org.sel.UnifiedBTS.Core.Network.WebCacheFile;
import org.sel.UnifiedBTS.Mantis.MantisWorker;
import org.sel.UnifiedBTS.Util.Config;
import org.sel.UnifiedBTS.Util.DateUtil;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.log;

public class TestMSSQL {
	
	
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
		
		TestMSSQL test = new TestMSSQL("zeus.sigse.org", "1433", "zeck", "zj92ym3n");
		if(test.connection()==false)
		{
			System.out.println("--Error to login");
			return;
		}
		System.out.println("Success to login");
		
		if(test.changeDB("Test")==null)
		{
			System.out.println("Error to change DB");
			return ;
		}
		System.out.println("Change DB");
		
		
		
		
		Map<String, Object> params = new HashMap<String, Object>();
		test.setTransaction();
		for(int i = 0; i<10; i++)
		{
			
			params.clear();
			params.put("a",		i);
			params.put("b",	(int)(Math.random()*100)/1);

			if(test.executeSP("testSP", params)<0)
			{
				System.out.println("Error to execute sp");
				return ;
			}
			System.out.println("inserted "+ i);
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		test.commit();
		System.out.println("Done");
		
	}//Main		
	
	
	//==============================================================================
	protected String ConnStr = "";
	protected String ClassName = "";
	protected Connection Conn = null;
	protected String UserID = null;
	protected String UserPW = null;
	protected String DBName = null;
	protected boolean Transaction = false;
	protected static DBManager DB=null;
	
	
	public TestMSSQL(String _servAddr, String _servPort, String _id, String _pw)
	{
		UserID = _id;
		UserPW = _pw;
		
		ClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		ConnStr = "jdbc:sqlserver://{0}:{1};";//DatabaseName={2}
		ConnStr = ConnStr.replaceFirst("\\{0\\}", _servAddr);
		ConnStr = ConnStr.replaceFirst("\\{1\\}", _servPort);
		DBName = "";
	}
	

	
	/**
	 * 데이터베이스에 로그인
	 * @param _id
	 * @param _pw
	 * @return
	 * @throws SQLException
	 */
	public boolean connection()
	{
		try{
			
			Class.forName(ClassName);
			Conn = DriverManager.getConnection(ConnStr, UserID, UserPW);//"BugManager", "smartBug@1");
			return true;
			
		} catch (ClassNotFoundException e) {
			System.out.println("Couldn't find a JDBC class. Check the JDBC." );
			e.printStackTrace();			
		} catch (SQLException e) {
			System.out.println("Failed to connection DBMS.with "+e.getErrorCode() );
			if(e.getErrorCode()!=0)
				e.printStackTrace();
		}
		return false;
	}
	
	
	/**
	 * 데이터베이스에 재로그인
	 * @param _id
	 * @param _pw
	 * @return
	 * @throws SQLException
	 */
	public boolean reconnection()
	{
		//Conn유효성 체크.
		try {
			if(Conn!=null && Conn.isValid(1)==true)
				Conn.close();
		} catch (SQLException e) {
			
		}
		Conn = null;
		
		//연결시도		
		boolean ret = connection();
		//기존에 연결되었던 DB에 연결
		if (ret==true)
		{
			if(changeDB(DBName)==null) ret = false;
		}
		return ret;
	}
	
	/**
	 * 데이터베이스를 변경함
	 * @param _dbname
	 * @return 변경전의 DB명을 반환.
	 * @throws SQLException
	 */
	public synchronized String changeDB(String _dbname)
	{
		String sql = "USE ";
		Statement stmt = null;
		
		int ret=0;
		
		try {
			stmt = Conn.createStatement();
			ret = stmt.executeUpdate(sql+_dbname);		//return이 0이면 실행 에러.
		} catch (SQLException e) {
			log.error("Error Query: " + sql+_dbname);
			log.printStackTrace(e);
			ret = 1;
		}
		finally
		{
			try {
				stmt.close();
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

	private int  MAX_RETRY = 100;
	private long WAIT_TIME = 5000;		//5초
	
	/**
	 * 사이트에 대한 정보를 저장.
	 */
	public synchronized int executeSP(String _spname, Map<String, Object> _params)
	{
		String sql = "{? = call ";
		sql += _spname + " (";
		sql += convertParameter(_params) + ")}";
	    
	    //Query 문 실행.
	    CallableStatement stmt = null;
		int ret=0;
		try
		{
			//DB접속시 네트웍 상의 문제가 발생시 재시도.
			int retry = 0;
			SQLException exception = null;
			do{
				
				try{
					if(Conn==null) throw new SQLException("User Defined : failed connection");
					stmt = Conn.prepareCall(sql);
					stmt.registerOutParameter(1, java.sql.Types.INTEGER);
					stmt.execute();
					ret = stmt.getInt(1);
				}
				catch(SQLException e)
				{
					exception = e;
					if(stmt!=null) stmt.close();	//기존 연결이 있으면 닫아줌.
					
					String msg = e.getMessage();
					if( msg.toLowerCase().contains("connection")==false && msg.toLowerCase().contains("shutdown")==false)
						throw exception;
					
					
					//일정시간 대기 후 재연결 시도
					System.out.println("Retry Reconnection Database after 5 sec ("+retry+"/"+MAX_RETRY+") : ErrorCode="+e.getErrorCode()+"; "+e.getMessage());
					Thread.sleep(WAIT_TIME);
					reconnection();
					continue;
				}
				break;
				
			}while(++retry<MAX_RETRY);
			if(retry>=MAX_RETRY) throw exception;
		} 
		catch (InterruptedException e) {
			System.out.println("Failed to thread sleep. query="+sql );
			e.printStackTrace();
		}		
		catch(SQLException e){
			System.out.println("Error Query = "+ sql);
			e.printStackTrace();
			ret =-1;
		}
		finally
		{
			try {
				stmt.close();
			} catch (Exception e) {
				System.out.println("Error : stmt.close(); in querying " + sql);
				e.printStackTrace();
				ret =-1;
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
		Transaction = false;
	}
	

	
}
