package org.seal.UniBAS.Mantis.Model;

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
	
	/**
	 * @uml.property  name="role"
	 */
	public String Role;
	
	public User()
	{
		ID = -1;
		LoginName = null;
		RealName = null;
		Timezone = "UTC";
		Role = null;
	}
	
	public User(int _id, String _loginName)
	{
		ID = _id;
		LoginName = _loginName;
		RealName = null;
		Timezone = "UTC";
		Role = null;
	}
	
	public User(int _id, String _loginName, String _role)
	{
		ID = _id;
		LoginName = _loginName;
		RealName = null;
		Timezone = "UTC";
		Role = _role;
	}
	
	public User(int _id, String _loginName, String _realName, String _timezone)
	{
		ID = _id;
		LoginName = _loginName;
		RealName = _realName;
		Timezone = _timezone;
		Role = null;
	}
	
	public User(User _user)
	{
		ID = _user.ID;
		LoginName = _user.LoginName;
		RealName = _user.RealName;
		Timezone = _user.Timezone;
		Role = _user.Role;
	}
}
