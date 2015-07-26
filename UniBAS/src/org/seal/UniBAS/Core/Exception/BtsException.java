package org.seal.UniBAS.Core.Exception;

public class BtsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8863146296733994163L;

	protected String ClassName;
	protected String MethodName;
	protected Object[] Datas;
	

	private BtsException(String _classname, String _methodname, String _msg, Object[] _datas)
	{
		super(_msg);
		ClassName = _classname;
		MethodName = _methodname;
		Datas = _datas;
	}
	
	
	private BtsException(String _msg, Object[] _datas)
	{
		super(_msg);
		ClassName = null;
		MethodName = null;
		Datas = _datas;
	}
		
	public String getDetailMessage()
	{
		return "";
	}

}
