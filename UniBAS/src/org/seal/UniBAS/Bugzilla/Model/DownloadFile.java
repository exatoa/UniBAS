package org.seal.UniBAS.Bugzilla.Model;

public class DownloadFile {
	
	/**
	 * @uml.property  name="mimeType"
	 */
	public String MimeType;
	/**
	 * @uml.property  name="fileSize"
	 */
	public int FileSize;
	/**
	 * @uml.property  name="data"
	 */
	public String Data;
	
	
	public DownloadFile(String _mimeType,  String _data, int _size) {
		MimeType = _mimeType;
		Data = _data;
		FileSize = _size;
	}

}
