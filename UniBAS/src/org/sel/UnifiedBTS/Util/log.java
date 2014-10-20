package org.sel.UnifiedBTS.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class log {
	//private String Filename = "";
	private static File file = null;
	private static FileWriter writer = null;
	private static BufferedWriter out=null;
	
	public static void init(String _path) throws IOException
	{
		file = new File(_path);
		File parent = file.getParentFile();

		if(!parent.exists() && !parent.mkdirs()){
		    throw new IllegalStateException("Couldn't create dir: " + parent);
		}
		
		writer = new FileWriter(file,true);
		out = new BufferedWriter(writer);
				
	}
	
		
	public static void close() throws IOException
	{
		out.close();
		writer.close();					
	}
	
	public static boolean print(String _msg)
	{
		return println(LogType.Normal, _msg);		
	}
	
	public static boolean info(String _msg)
	{
		return println(LogType.Message, _msg);		
	}
	
	public static boolean warn(String _msg)
	{
		return println(LogType.Warning, _msg);		
	}
	
	public static boolean error(String _msg)
	{
		return println(LogType.Error, _msg);		
	}

	public static boolean notice(String _msg)
	{
		return println(LogType.Notice, _msg);		
	}
	
	public static boolean logging(String _msg)
	{
		return println(LogType.Logging, _msg);		
	}
	
	public static boolean println(LogType _type, String _msg)
	{
		boolean ret = true;
		
		String header = "";
		switch(_type)
		{
		case Normal :	header = "";	break;
		case Message : 	header = "[Msg]# ";	break;
		case Warning :	header = "[WAR]# ";	break;
		case Error :	header = "[ERR]# ";	break;			
		case Notice :	header = "[Notice]# ";	break;
		case Logging :	header = "[Log]# ";	break;
		}
		_msg = "["+DateUtil.getNowString()+"]"+ header +_msg;
		
		
		//타입이 Logging인 경우는 화면 출력만 시행
		if(_type!=LogType.Logging)
			System.out.println(_msg);
		
		//if(_type!=LogType.Message)	//TODO : 원래대로 돌려야함.
			ret = println(_msg);
	
		return ret;
	}

	public static boolean println(String _msg)
	{
		boolean ret = true;
		
		try {
			writer.write(" "+_msg);
			writer.write("\r\n");
			writer.flush();
		  
		} catch (Exception e) {
			try {
				close();
			} catch (IOException e2) {
				e.printStackTrace();
				ret = false;
				throw new RuntimeException(e2);
			}
			
		}
		return ret;
	}

	public static boolean printStackTrace(Throwable exception) {
		
		
		PrintWriter printWriter = new PrintWriter(writer, true);
		exception.printStackTrace(printWriter);
		exception.printStackTrace();
		
		return true;
	}
	
	public static boolean printStackTrace(StackTraceElement[] elements) {
		String PADDING = "       ";
		boolean ret = true;
		try{
			//add each element of the stack trace
			for (StackTraceElement e : elements){
				writer.write(PADDING + e);
				writer.write("\r\n");
				writer.flush();
			}
		
		} 
		catch (Exception e) {
			try {
				close();
			} catch (IOException e2) {
				e.printStackTrace();
				ret = false;
			}
		}

		return ret;		
	}
	
}
