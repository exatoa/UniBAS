package org.seal.UniBAS.Mantis.Model;

public class Bugnote {

	public int ID;
	public int Type;
	public int isPrivate;
	public User Submitter;
	public String CreationTime;	//DateTime
	public String UpdateTime;	//DateTime
	public String TheText;
	public int AttachID;

//	public int UserID;
//	public String UserName;
	public Bugnote()
	{
		
	}
	
	public Bugnote(int _id, int _type, User _user, String _creation_ts, String _update_ts, String _text, int _isPrivate){
		this(_id, _type, _user, _creation_ts, _update_ts, _text, _isPrivate, -1);
	}
	
	public Bugnote(int _id, int _type, User _user, String _creation_ts, String _update_ts, String _text, int _isPrivate, int _attachID){
		ID = _id;
		Submitter = _user;
		CreationTime = _creation_ts;	//DateTime
		UpdateTime = _update_ts;	//DateTime
		TheText = _text;
		AttachID = _attachID;
		isPrivate = _isPrivate;
		Type = _type;
	}
}
