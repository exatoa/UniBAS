package org.seal.UniBAS.Core.Database;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;


public class MysqlDBManager extends DBManager {
		
	public MysqlDBManager(String _servAddr, String _servPort, String _id, String _pw) throws ClassNotFoundException, SQLException
	{
		super(_id, _pw);
		Class.forName("com.mysql.jdbc.Driver");
		ConnStr = "jdbc:mysql://{0}:{1}";// /{2}";	//0-URL, 1-PORT, 2-DB_NAME
		ConnStr = ConnStr.replaceFirst("\\{0\\}", _servAddr);
		ConnStr = ConnStr.replaceFirst("\\{1\\}", _servPort);
		//ConnStr = ConnStr.replaceFirst("\\{2\\}", _dbName);
	}
	

	@Override
	public int executeSP(String _spname, Map<String, Object> params) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int createDB(String _dbname) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int dropDB(String _dbname) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public String changeDB(String _dbname) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String executeSPStr(String _spname, Map<String, Object> _params) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setTransaction() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void commit() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void rollback() {
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	public int executeSqlScript(InputStream _inputFile) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int executeSqlScript(String _script) {
		// TODO Auto-generated method stub
		return 0;
	}


}
