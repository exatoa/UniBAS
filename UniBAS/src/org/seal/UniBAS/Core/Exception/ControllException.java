package org.seal.UniBAS.Core.Exception;

public class ControllException extends Exception {
  
	private static final long serialVersionUID = -2426024183036721767L;
	//static final long serialVersionUID = -3387516993124229948L;
	
	
	public int ErrorCode = -1;
			
    public ControllException()
    {
        super();
        ErrorCode = -1;
    }

    public ControllException(int errCode, String arg0)
    {
        super(arg0);
        ErrorCode = errCode;
    }

    public ControllException(int errCode, java.lang.String arg0, java.lang.Throwable arg1)
    {
        super(arg0, arg1);
        ErrorCode = errCode;
    }

    public ControllException(int errCode, Throwable arg0)
    {
        super(arg0);
        ErrorCode = errCode;
    }

    protected ControllException(int errCode, java.lang.String arg0, java.lang.Throwable arg1, boolean arg2, boolean arg3)
    {
        super(arg0, arg1, arg2, arg3);
        ErrorCode = errCode;
    }
}
