package org.sel.UnifiedBTS.Mantis.Model;

public class Attachment {
	
	/**
	 * @uml.property  name="iD"
	 */
	public int ID;
	/**
	 * @uml.property  name="creationTime"
	 */
	public String CreationTime;	//DateTime
	/**
	 * @uml.property  name="modificationTime"
	 */
	public String ModificationTime;	//DateTime
	/**
	 * @uml.property  name="desc"
	 */
	public String Desc;
	/**
	 * @uml.property  name="filename"
	 */
	public String Filename;
	/**
	 * @uml.property  name="attacher"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	public User Attacher;
	/**
	 * @uml.property  name="isObsolete"
	 */
	public int isObsolete;
	/**
	 * @uml.property  name="isPatch"
	 */
	public int isPatch;
	/**
	 * @uml.property  name="isPrivate"
	 */
	public int isPrivate;
	/**
	 * @uml.property  name="isUrl"
	 */
	public int isUrl;
	/**
	 * @uml.property  name="mimeType"
	 */
	public String MimeType;

	/**
	 * @uml.property  name="fileSize"
	 */
	public long FileSize;
	/**
	 * @uml.property  name="data"
	 */
	public String Data;
	

	
	public Attachment(int _attachID, String _desc,	String _filename, int _isObsolete, int _isPatch, int _isPrivate, int _isUrl,  String _creation_ts,  String _delta_ts, int _attacherID, String _attacher, String _mimeType, String _data, long _size) {
		ID = _attachID;
		CreationTime = _creation_ts;
		ModificationTime = _delta_ts;
		
		Desc = _desc;
		Filename = _filename;
		isObsolete = _isObsolete;
		isPatch = _isPatch;
		isPrivate = _isPrivate;
		isUrl = _isUrl;
		Attacher = new User(_attacherID, _attacher);
		
		FileSize = _size;
		MimeType = _mimeType;
		Data = _data;
	}
	
	public Attachment() {
		ID = -1;
		CreationTime = null;
		ModificationTime =null;
		
		Desc = null;
		Filename = null;
		isObsolete = 0;
		isPatch = 0;
		isPrivate = 0;
		isUrl = 0;
		
		Attacher = new User();
		FileSize = 0;
		MimeType = null;
		Data = null;
	}

	public Attachment(int _id) {
		this();
		ID = _id;
	}

}
