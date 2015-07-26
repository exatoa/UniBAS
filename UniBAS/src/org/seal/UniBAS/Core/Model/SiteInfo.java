package org.seal.UniBAS.Core.Model;



/**
* 사이트 정보에 대한 형식
*/
public class SiteInfo {
	
	public String Name;			//  사이트 이름
	public String Desc;			//  사이트 설명
	public String Type;			//  사이트 종류
	public String SchemaName;	//  사이트 정보가 저장될 스키마명
	public String BaseUrl;
	public String UserID;
	public String UserPW;
	public String LogPath;
	public String CachePath;
	
	
	
	public SiteInfo(String _name, String _dbName, String _desc, String _type, String _baseUrl, String _userID, String _userPW, String _logPath, String _cachePath)
	{
		Name = _name;
		SchemaName	= _dbName;
		Desc	= _desc;
		Type	= _type;
		BaseUrl	= _baseUrl;
		UserID	= _userID;
		UserPW	= _userPW;
		LogPath	= _logPath;
		CachePath	= _cachePath;
	}
}
