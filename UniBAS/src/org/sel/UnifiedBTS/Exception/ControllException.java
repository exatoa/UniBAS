package org.sel.UnifiedBTS.Exception;

public class ControllException extends Exception {
  
	private static final long serialVersionUID = -2426024183036721767L;
	//static final long serialVersionUID = -3387516993124229948L;
	
    public ControllException()
    {
        super();
    }

    public ControllException(String arg0)
    {
        super(arg0);
    }

    public ControllException(java.lang.String arg0, java.lang.Throwable arg1)
    {
        super(arg0, arg1);
    }

    public ControllException(Throwable arg0)
    {
        super(arg0);
    }

    protected ControllException(java.lang.String arg0, java.lang.Throwable arg1, boolean arg2, boolean arg3)
    {
        super(arg0, arg1, arg2, arg3);
    }
}
