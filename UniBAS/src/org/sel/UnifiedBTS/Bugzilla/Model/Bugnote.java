package org.sel.UnifiedBTS.Bugzilla.Model;

public class Bugnote {
	
	/**
	 * @uml.property  name="iD"
	 */
	public int ID;
	/**
	 * @uml.property  name="type"
	 */
	public int Type;
	/**
	 * @uml.property  name="isPrivate"
	 */
	public int isPrivate;
	/**
	 * @uml.property  name="submitter"
	 * @uml.associationEnd  
	 */
	public User Submitter;
	/**
	 * @uml.property  name="creationTime"
	 */
	public String CreationTime;	//DateTime
	/**
	 * @uml.property  name="theText"
	 */
	public String TheText;
	/**
	 * @uml.property  name="attachID"
	 */
	public int AttachID;
	
//	public int UserID;
//	public String UserName;
	public Bugnote()
	{
		
	}
	
	public Bugnote(int _id, int _userID, String _userName, String _time, String _text, int _isPrivate){
		this(_id, _userID, _userName, _time, _text, _isPrivate, -1);
	}
	
	public Bugnote(int _id, int _userID, String _userName, String _time, String _text, int _isPrivate, int _attachID){
		ID = _id;
		Submitter = new User(_userID, _userName);
		CreationTime = _time;	//DateTime
		TheText = _text;
		AttachID = _attachID;
		isPrivate = _isPrivate;
	}
}
