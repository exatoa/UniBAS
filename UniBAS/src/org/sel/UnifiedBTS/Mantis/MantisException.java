package org.sel.UnifiedBTS.Mantis;

import org.sel.UnifiedBTS.Util.log;

public class MantisException extends Exception{

	public int ErrorCode = 0;
	public String ErrorMsg = "";
	/**
	 * 
	 */
	private static final long serialVersionUID = 4109234694393317415L;
	
	public MantisException()
	{
		super();
		ErrorCode = 0;
		ErrorMsg = "";
	}
	
	public MantisException(String _msg)
	{
		super(_msg);
		ErrorCode = 0;
		ErrorMsg = "";
	}
	
	public MantisException(int _code, String _msg)
	{
		super(_msg);
		ErrorCode = _code;
		ErrorMsg = _msg;
	}
	
	public String getMessage()
	{
		return "Application Error " + ErrorCode + " : " + ErrorMsg;
	}
}
