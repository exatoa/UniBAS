package org.seal.UniBAS.Core.Database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.seal.UniBAS.Util.log;

public abstract class DBManager {
	
	/**
	 * @uml.property  name="connStr"
	 */
	protected String ConnStr = "";
	/**
	 * @uml.property  name="className"
	 */
	protected String ClassName = "";
	/**
	 * @uml.property  name="conn"
	 */
	protected Connection Conn = null;
	/**
	 * @uml.property  name="userID"
	 */
	protected String UserID = null;
	/**
	 * @uml.property  name="userPW"
	 */
	protected String UserPW = null;
	/**
	 * @uml.property  name="dBName"
	 */
	protected String DBName = null;
	//protected String TargetName = null;
	
	protected boolean Transaction = false;
	
	protected static DBManager DB=null;
	
	private int  MAX_RETRY = 100;
	private long WAIT_TIME = 5000;		//5초
	
	protected DBManager(String _id, String _pw)
	{
		ConnStr = "";
		UserID = _id;
		UserPW = _pw;
	}
	
	
	/**
	 * 실제 Database 객체를 반환.
	 * @param _Type : 데이터베이스 종류
	 * @param _servAddr
	 * @param _port
	 * @param _id
	 * @param _pw
	 * @return
	 */
	public static DBManager getInstance(String _Type, String _servAddr, String _port, String _id, String _pw)
	{	
		if(DB==null){
			DB = new MssqlDBManager(_servAddr, _port, _id, _pw);
			DB.connection();	//초기화를 위한 기본연결
		}		
		return DB;
	}
	
	/**
	 * 편의상 사용 함수. 실제 Database 객체를 반환.
	 * @param _Type : 데이터베이스 종류
	 * @param _servAddr
	 * @param _port
	 * @param _id
	 * @param _pw
	 * @return
	 */
	public static DBManager getInstance()
	{
		return DB;
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
			Conn = DriverManager.getConnection(ConnStr, UserID,UserPW);//"BugManager", "smartBug@1");
			return true;
			
		} catch (ClassNotFoundException e) {
			log.error("Couldn't find a JDBC class. Check the JDBC." );
			log.printStackTrace(e);			
		} catch (SQLException e) {
			log.error("Failed to connection DBMS. ErrorCode:"+e.getErrorCode()+"; "+e.getMessage() );
			if(e.getErrorCode()!=0)
				log.printStackTrace(e);			
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
		boolean ret = true;
		long retry = 0;
		
		while(true)
		{
			//Conn유효성 체크.
			try {
				if(Conn!=null && Conn.isValid(1)==true)
					Conn.close();
				Conn = null;
			
				//연결시도		
				ret =connection();
			
				//기존에 연결되었던 DB에 연결
				if (ret==true){
					if(changeDB(DBName)==null) ret = false;
				}
				
				//실패시 일정시간 대기 후 재시도.
				if (ret==false){
					log.error("Retry Reconnection Database after "+(WAIT_TIME/1000)+" sec ("+(++retry)+")");
					Thread.sleep(WAIT_TIME);					
					continue;
				}
			
			} catch (SQLException e) {
				continue;
			}
			catch (InterruptedException e) {
				log.printStackTrace(e);
				//방법이없네;;;
			}
			break;
		}

		return ret;
	}
	
	
	
	/**
	 * 데이터베이스에 연결 해제
	 * @return
	 * @throws SQLException
	 */
	public boolean disconnection() throws SQLException
	{
		Conn.close();
		return true;
	}
	

	/**
	 * 새로운 이름의 데이터베이스를 생성함.
	 * @return 1이면 정상적으로 생성, 2이면 이미 존재함, 0이면 실패.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public abstract int createDB(String _dbname);
	
	
	/**
	 * 해당 이름의 데이터 베이스 삭제
	 * @return 1이면 정상적으로 삭제, 0이면 실패.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public abstract int dropDB(String _dbname);
	
	/**
	 * 데이터베이스를 변경함
	 * @param _dbname
	 * @return
	 * @throws SQLException
	 */
	public abstract String changeDB(String _dbname) throws SQLConnectionException;	
	
	/**
	 * 입력받은 sp를 param을 넘겨주어 실행.
	 * @param _spname
	 * @param _params
	 * @return
	 */
	public abstract int executeSP(String _spname, Map<String, Object> _params) throws SQLConnectionException;
	
	/**
	 * 입력받은 sp를 param을 넘겨주어 실행.
	 * @param _spname
	 * @param _params
	 * @return
	 */
	public abstract String executeSPStr(String _spname, Map<String, Object> _params) throws SQLConnectionException;
	
	/**
	 * 스크립트 파일에 있는 SQL문들을 실행
	 * @param _inputFile
	 * @return 성공한 스크립트 수를 반환. (치명적인 에러 발생시 -1)
	 */
	public abstract int executeSqlScript(InputStream _inputFile);

	/**
	 * 트랜젝션 관련 메소드.
	 */
	public abstract void setTransaction();
	public abstract void commit();
	public abstract void rollback();

	/**
	 * 스크립트 파일에 있는 SQL문들을 실행
	 * @param _inputFile
	 * @return 성공한 스크립트 수를 반환. (치명적인 에러 발생시 -1)
	 */
	public abstract int executeSqlScript(String _script);
}
