package org.seal.UniBAS.Bugzilla.Model;

public class User {
	/**
	 * @uml.property  name="iD"
	 */
	public int ID;
	/**
	 * @uml.property  name="loginName"
	 */
	public String LoginName;
	/**
	 * @uml.property  name="realName"
	 */
	public String RealName;
	/**
	 * @uml.property  name="timezone"
	 */
	public String Timezone;
	
	public User()
	{
		ID = -1;
		LoginName = null;
		RealName = null;
		Timezone = "UTC";
	}
	
	public User(int _id, String _loginName)
	{
		ID = _id;
		LoginName = _loginName;
		RealName = null;
		Timezone = "UTC";
	}
	
	public User(int _id, String _loginName, String _realName)
	{
		ID = _id;
		LoginName = _loginName;
		RealName = _realName;
		Timezone = "UTC";
	}
	
	public User(int _id, String _loginName, String _realName, String _timezone)
	{
		ID = _id;
		LoginName = _loginName;
		RealName = _realName;
		Timezone = _timezone;
	}
}
