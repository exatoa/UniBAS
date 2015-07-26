package org.seal.UniBAS.Util;


import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.StringCharacterIterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.entity.UrlEncodedFormEntity;


public class TextUtil {

	public static String addQuotes( String _text){
		if(_text==null) return null;
		if(_text.length()==0) return _text;
		
        final StringBuffer sb                   = new StringBuffer( _text.length() * 2 );
        final StringCharacterIterator iterator  = new StringCharacterIterator( _text );
        
  	  	char character = iterator.current();
        
        while( character != StringCharacterIterator.DONE ){
            if( character == '\'' ) sb.append( "''" );
            else sb.append( character );
            
            character = iterator.next();
        }
        
        return sb.toString();
    }
	
	public static String addSlashes( String text ){
        final StringBuffer sb                   = new StringBuffer( text.length() * 2 );
        final StringCharacterIterator iterator  = new StringCharacterIterator( text );
        
  	  	char character = iterator.current();
        
        while( character != StringCharacterIterator.DONE ){
            if( character == '"' ) sb.append( "\\\"" );
            else if( character == '\'' ) sb.append( "\\\'" );
            else if( character == '\\' ) sb.append( "\\\\" );
            else if( character == '\n' ) sb.append( "\\n" );
            else if( character == '{'  ) sb.append( "\\{" );
            else if( character == '}'  ) sb.append( "\\}" );
            else sb.append( character );
            
            character = iterator.next();
        }
        
        return sb.toString();
    }
	
	public static String delSlashes( String text ){    	
        final StringBuffer sb                   = new StringBuffer( text.length() * 2 );
        final StringCharacterIterator iterator  = new StringCharacterIterator( text );
        
  	  	char character = iterator.current();
        
        while( character != StringCharacterIterator.DONE ){
            if( character == '\\' ){
            	character = iterator.next();
            	if(character != StringCharacterIterator.DONE) break;
            	sb.append( character );  
            }
            else sb.append( character );
            
            character = iterator.next();
        }
        
        return sb.toString();
    }
	
	public static int getSizeBinary(String _base64){
		if (_base64==null) return 0;
		if (_base64.length()==0) return 0;
		int padding = 0;
		if (_base64.charAt(_base64.length()-2)=='=')	padding = 2;
		else if (_base64.charAt(_base64.length()-1)=='=') padding = 1;
		
		int size = _base64.length();
		
		size = (((size/4)*3) - padding);
			
        return size;
    }
	
	/**
	 * Base64로 인코딩된 데이터를 파일로 출력.
	 * @param _filename
	 * @param _data
	 * @return 실행된 결과.
	 * @throws Exception
	 */
	public static boolean writeBase64File(String _filename, String _base64data) {
				
		File file = new File(_filename);
		File parent = file.getParentFile();
    	boolean ret = true;
    	BufferedOutputStream bos = null;
    	
    	
		try {
			//상위폴더가 없는경우 상위폴더 생성.
			if(parent !=null)
				Files.createDirectories(parent.toPath());
			
			//file.createNewFile();
			bos = new BufferedOutputStream(new FileOutputStream(file));
	    	
			bos.write(Base64.decodeBase64(_base64data));
			bos.flush();
		} catch (Exception e) {
			log.printStackTrace(e);
			ret = false;
		}
		finally
		{
			try {
				bos.close();
			} catch (IOException e) {
				log.printStackTrace(e);
			}
		}
		return ret;	
	}
	
	/**
	 * Base64로 인코딩된 데이터를 파일로 출력.
	 * @param _filename
	 * @param _data
	 * @return 실행된 결과.
	 * @throws Exception
	 */
	public static String readBase64File(String _filename)
	{
		
		//파일 존재 확인.
		File file = new File(_filename);
		if(file.exists()==false)
			return null;
		
		
		byte[] binaryData = new byte[(int) file.length()];
		DataInputStream dis = null;
		
		try {
			dis = new DataInputStream(new FileInputStream(file));
			dis.readFully(binaryData);
		}
		catch (FileNotFoundException e) {
				log.printStackTrace(e);
				binaryData = null;
		}
		catch (IOException e) {
			log.printStackTrace(e);
			binaryData = null;
		}
		finally{
			try {
				dis.close();
			} catch (IOException e) {
				log.printStackTrace(e);
			}
		}
		
		
		if (binaryData!=null){
			byte[] base64 = Base64.encodeBase64(binaryData);
			return new String(base64);
		}		
		return null;	
	}
	
	
	
	/**
	 * 일반 텍스트 파일을 기록.
	 * @param _filename
	 * @param _data
	 * @return 실행된 결과.
	 * @throws Exception
	 */
	public static boolean writeTextFile(String _filename, String _data, String _encoding) 
	{
		boolean ret = true;

		//기존의 폴더 생성.
		File file = new File(_filename);
		File parent = file.getParentFile();
		String encoding = _encoding;

		if(encoding==null)	encoding = "UTF-8";
		
		
	
		OutputStreamWriter sw = null;
		
		try{
			if(parent !=null)
				Files.createDirectories(parent.toPath());
			
			sw = new OutputStreamWriter(new FileOutputStream(file,false), encoding);
			sw.write(_data);
			
		}
		catch(IOException e){
			log.printStackTrace(e);
			ret = false;
		}
		finally{
			try {
				sw.close();
			} catch (IOException e) {
				log.printStackTrace(e);
			}
		}
		
		return ret;	
	}

	

	/**
	 * 일반 텍스트 파일을 기록.
	 * @param _filename
	 * @param _data
	 * @return 실행된 결과.
	 * @throws Exception
	 */
	public static String readTextFile(String _filename, String _encoding)
	{
		
		//파일 존재 확인.
		File file = new File(_filename);
		if(file.exists()==false)	return null;
		
		//파일 인코딩 확인
		String encoding = _encoding;
		if (encoding==null) encoding = "UTF-8";

		String result = null;
		StringWriter sw = new StringWriter();			//결과버퍼
		InputStreamReader reader = null;
	    char[] buffer = new char[1024 * 4];
	    int n = 0;
	    
	    
	    try{
	    	reader = new InputStreamReader(new FileInputStream(file), encoding);
	    
	    	while (-1 != (n = reader.read(buffer))) {
	    		sw.write(buffer, 0, n);
	    	}
	    	result = sw.toString();
	    }
		catch(Exception e)
		{
			log.printStackTrace(e);
			result = null;
		}
	    finally{
	    	try {
				reader.close();
				sw.close();
			} catch (IOException e) {
				log.printStackTrace(e);
			}
	    }
	    
	    
		return result;
	}
	
	
	/**
	 * 파일 패스 구함. (신버전 : MD5이용해서 파일명 선정)
	 * @param _url
	 * @return
	 */
    public static String convertURLtoPath(String _url)
    {
    	if (_url==null) return null;
    	
    	String fragment = null;
    	String query=null ;
    	int idx = -1;
    	idx = _url.indexOf("#");
    	if(idx>0){
    		fragment = _url.substring(idx+1, _url.length());
    		_url = _url.substring(0, idx);
    	}
    	idx = _url.indexOf("?");
    	if(idx>0){
    		query = _url.substring(idx+1, _url.length());
    		_url = _url.substring(0, idx);
    	}
    	
    	
    	URI uri = null;
    	try{    		
    		uri = URI.create(_url);
    	}    
    	catch(IllegalArgumentException e)
    	{
    		log.printStackTrace(e);
        	return null;
    	}
    	
    	String host = uri.getHost();
    	int port = uri.getPort();
    	String portStr = "";
    	String path = uri.getPath();
    
    	
    	if (port !=-1)
    		portStr = "#"+ String.valueOf(port);
    	
    	
    	//경로에 있는 구분자를, 파일경로 구분자로 변경.
    	if (path==null) path="\\";
    	else{
    		path = path.replace('/', '\\');
    		if (path.endsWith("\\")==false) path += "\\";
    	}
    	
    	if (path.endsWith("\\")==false) path += "\\";
    	
    	if(query==null) query = "";
    	if(fragment==null) fragment = "";
    	
    	String name = query + fragment;

    	String filename = getHash(name);
    	
    	//String bucket= filename.substring(0,2) + "\\";
    
    	String filepath = host + portStr + path + filename+".txt";//bucket + 

    	return filepath;
    }
    
    /**
     * 주어진 패스로부터 버킷을 생성.
     * 맨마지막에 주어진 파일명을 이용하여 생성.
     * 최종변경된 path를 반환
     * @param _path
     * @return
     */
    public static String makeBucket(String _path, int _size, int _level)
    {
    	if(_size<=0) return null;
    	int idx = _path.lastIndexOf("\\");
    	if (idx<=0) return null;
    	
    	String parent = _path.substring(0,idx+1);
    	String filename = _path.substring(idx+1, _path.length());
    	
    	if(filename.length()<_size*_level) return null;

    	String bucket = "";
    	for(int i=0; i<_level; i++)
    	{
    		bucket += filename.substring(i*_size,(i+1)*_size)+"\\";
    	}    	
    	return parent + bucket + filename;
    }
    
    /**
     * 파일 패스 구함. (구버전)
     * @param _url
     * @return
     */
    public static String convertURLtoFilePath(String _url)
    {
    	URI uri = null;
    	try{
    		uri = URI.create(_url);
    	}
    	catch(IllegalArgumentException e)
    	{
    		log.printStackTrace(e);
    		return null;
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append("hi");
    	
    	String host = uri.getHost();
    	int port = uri.getPort();
    	String portStr = "";
    	String path = uri.getPath();
    	String query = uri.getQuery();
    	String fragment = uri.getFragment();
    
    	
    	if (port !=-1)
    		portStr = "#"+ String.valueOf(port);
    	    	
    	//경로에 있는 값들을 파일 경로 구분자로 대체
    	if (path==null) path="\\";
    	else{
    		path = path.replace('/', '\\');
    		if (path.endsWith("\\")==false) path += "\\";
    	}
    	
    	//쿼리의 특수기호들을 _로 변경.
    	if (query==null) query = "";
    	else{
    		query = query.replaceAll("(/|\\\\|\\?|:|\\*|\"|<|>|\\|)", "_");
    	}

    	if(fragment ==null) fragment="";
    	else fragment = "#" + fragment;
    	
    
    	String filepath = host + portStr + path + query + fragment;
    	
    	if (filepath.endsWith("\\")) filepath += "#index";
    	
    	return filepath;
    }
    
    /**
     * MD5 해쉬값을 돌려준다.
     * @param str
     * @return
     */
    public static String getHash(String str){
    	String MD5 = ""; 
    	try{
    		MessageDigest md = MessageDigest.getInstance("MD5"); 
    		md.update(str.getBytes()); 
    		byte byteData[] = md.digest();
    		StringBuffer sb = new StringBuffer(); 
    		for(int i = 0 ; i < byteData.length ; i++){
    			sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
    		}
    		MD5 = sb.toString();
    		
    	}catch(NoSuchAlgorithmException e){
    		e.printStackTrace(); 
    		MD5 = null; 
    	}
    	return MD5;
    }
    

}
