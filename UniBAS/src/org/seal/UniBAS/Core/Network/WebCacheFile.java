package org.seal.UniBAS.Core.Network;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.seal.UniBAS.Core.Model.DownloadFile;
import org.seal.UniBAS.Util.TextUtil;
import org.seal.UniBAS.Util.log;


public class WebCacheFile extends WebFile {

	protected static WebCacheFile Instance = null;
	
	public static WebCacheFile getInstance()
	{
		if (Instance==null)
			Instance = new WebCacheFile();
		
		return Instance;
	}
	
	
	/////////////////////////////////////////////////////////////
	//		실제 메소드 코드.
	/////////////////////////////////////////////////////////////

	protected String LocalProxyPath = null;		//캐쉬패스
	protected String LastUrl = null;			//마지막 접근 URL
	protected String LastHash = null;			//마지막 접근 URL
	protected int CacheNamesize = 2;
	protected int CacheLevel = 1;
	
	
	protected WebCacheFile()
	{
		super();
		LastUrl = "";
	}
	
	public void setLocalProxy(String _localPath)
	{
		LocalProxyPath = _localPath;
	}
	
	
	public void setCacheLevel(int _level)
	{
		CacheLevel = _level;
	}
	
	public void setCacheNamesize(int _size)
	{
		CacheNamesize = _size;
	}

	
	
	public String getLastHash()
	{
		return LastHash;
	}


	
	/**
     * URL페이지에 로그인 시도. POST 방식
     * @param _url
     * @param _param
     * @return 요청 결과 페이지의 HTML(XML) string
     */
	public String getBody(String _url, Map<String,String> _param, boolean _force) {
		
		String param = convertQuery(_param);
		String urlWithParam = _url+"?"+param;
		String relPath = TextUtil.convertURLtoPath(urlWithParam);
		String data=null;
		
		if(relPath==null) return null;
		
		//최근 방문한 페이지와 같지 않은경우 (로컬부터 검색시작)  //같은경우 바로 웹다운
		if (LastUrl.compareTo(urlWithParam)!=0 && _force==false)
			data = TextUtil.readTextFile(TextUtil.makeBucket(LocalProxyPath + relPath,CacheNamesize, CacheLevel), null); //로컬에 있으면 로컬 데이터 반환.

		//마지막 방문정보 기록 (다음 비교를 위해서)
		LastUrl = urlWithParam;
		LastHash = relPath;

		//로컬에 파일이 존재하면 로컬파일을 리턴.
		if(data!=null) 
			return data;
			
		
		//로컬에 없는 경우 웹에서 가져옴(POST)		
		log.logging("Site(POST) : "+ urlWithParam);
		String http = super.getBody(_url, _param);
		if(http==null) return null;
			
		//로컬에 데이터 저장.
		log.logging("path : " +LocalProxyPath + relPath);
		if(TextUtil.writeTextFile(TextUtil.makeBucket(LocalProxyPath + relPath, CacheNamesize, CacheLevel), http, null)==false)
			return null;
		
		return http;	
        
	}
	
	/**
	 * 웹페이지의 내용을 반환.
	 * @param _url
	 * @param _force 강제로 웹에서 가져오도록 설정
	 * @return
	 * @throws Exception
	 */
	public String getBody(String _url, boolean _force) {
		
		String relPath = TextUtil.convertURLtoPath(_url);
		String data=null;
		
		if(relPath==null) return null;
		
		//최근 방문한 페이지와 같지 않은경우 (로컬부터 검색시작)  //같은경우 바로 웹다운
		if (LastUrl.compareTo(_url)!=0 && _force==false)
			data = TextUtil.readTextFile(TextUtil.makeBucket(LocalProxyPath + relPath,CacheNamesize, CacheLevel), null);	//로컬에 있으면 로컬 데이터 반환.

		//마지막 방문정보 기록 (다음 비교를 위해서)
		LastUrl = _url;
		LastHash = relPath;

		//로컬에 파일이 존재하면 로컬파일을 리턴.
		if(data!=null) 
			return data;
			
		
		//로컬에 없는 경우 웹에서 가져옴(POST)		
		log.logging("Site(GET) : "+ _url);
		String http = super.getBody(_url);
		if(http==null) return null;
			
		//로컬에 데이터 저장.
		if(TextUtil.writeTextFile(TextUtil.makeBucket(LocalProxyPath + relPath, CacheNamesize, CacheLevel), http, null)==false)
			return null;
		
		return http;
	}
	
	/**
	 * url을 통하여 웹의 파일을 다운받는다.
	 * @param _url
	 * @param _force
	 * @return
	 */
	public DownloadFile getFileString(String _url, boolean _force) {
		
		String relPath = TextUtil.convertURLtoPath(_url);
		DownloadFile file = null;
		
		if(relPath==null) return null;
		
		
		//최근 방문한 페이지와 같지 않은경우 (로컬부터 검색시작)  //같은경우 바로 웹다운
		if (LastUrl.compareTo(_url)!=0 && _force==false)
			file = getLocalBinaryFile(LocalProxyPath + relPath);	//로컬에 있으면 로컬 데이터 반환.

		//마지막 방문정보 기록 (다음 비교를 위해서)
		LastUrl = _url;
		LastHash = relPath;

		//로컬에 파일이 존재하면 로컬파일을 리턴.
		if(file!=null) 
			return file;
			
		
		//로컬에 없는 경우 웹에서 가져옴(POST)		
		log.logging("WebFile(GET) : "+ _url);
		file = super.getFileString(_url);
		if(file==null) return null;
			
		//로컬에 데이터 저장.
		if(TextUtil.writeBase64File(TextUtil.makeBucket(LocalProxyPath + relPath, CacheNamesize, CacheLevel), file.Data)==false)
			return null;
		
		return file;
	}

	/////////////////////////////////////////////////////////////////
	//  유틸 메소드
	/////////////////////////////////////////////////////////////////

    
    private String convertQuery(Map<String, String> params){
        StringBuilder sb = new StringBuilder();
         
        Iterator<String> keys = params.keySet().iterator();
        while(keys.hasNext()){
            String key = keys.next();
            sb.append(key);
            sb.append("=");
            sb.append(params.get(key).toString());
            if(keys.hasNext()==true) sb.append("&");
        }
        return sb.toString();
    }
    
    
    public DownloadFile getLocalBinaryFile(String _path)
    {
    	//파일 내용얻기
    	String data = TextUtil.readBase64File(TextUtil.makeBucket(_path,CacheNamesize, CacheLevel));
    	if(data==null) return null;
    	
    	//파일 크기 얻기
    	File f = new File(_path);
		long size = f.length();
		
		return new DownloadFile("binary", data, size);
    }

	
}
