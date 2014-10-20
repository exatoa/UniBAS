package org.sel.UnifiedBTS.Core.Model;

/**
* 다운로드 파일 형식
*/
public class DownloadFile {
	
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
	
	
	public DownloadFile(String _mimeType,  String _data, long _size) {
		MimeType = _mimeType;
		Data = _data;
		FileSize = _size;
	}

}