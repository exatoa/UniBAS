package org.sel.UnifiedBTS.Core.Network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.sel.UnifiedBTS.Core.Model.DownloadFile;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.log;

public class WebFile{
	/////////////////////////////////////////////////////////////
	//		싱글톤 코드.
	/////////////////////////////////////////////////////////////
	protected static WebFile Instance = null;
	
	public static WebFile getInstance()
	{
		if (Instance==null)
			Instance = new WebFile();
		
		return Instance;
	}
	
	/////////////////////////////////////////////////////////////
	//		객체 : 멤버변수
	/////////////////////////////////////////////////////////////
	//Connection 관련 변수
    protected CloseableHttpClient httpClient;
    protected HttpClientContext context;
    protected CookieStore cookieStore;

    
    //seamless (stableness) 지원을 위한 변수
    private int  MaxRetry = 30;						//재연결 시도회수 (0은 무한대)
	private long  ReconnectionSleepTime = 5000L;	//재연결 시 대기시간.
    

	//웹 요청속도 제어를 위한 변수.
    private long DownloadSpeed = 40;		//최대 1분당 40개 다운받을 수 있는 속도
    private long DownloadInteval= 1500;		//각 다운로드간 시간차 (1000 * 60 / DownloadSpeed)
    
    private long CurrentCount = 0;			//현재 다운받은 개수
    private int LimitCount = 1200;			//연속으로 받을 수 있는 최대 개수
    private long WaitMinute = 10;			//Limit 초과시 휴식시간
    private long LastTime = DateTime.now().getMillis();

	
    
	/////////////////////////////////////////////////////////////
	//		객체 : 생성/삭제 관련 메소드
	/////////////////////////////////////////////////////////////
    /**
     * 기본 생성자
     */
    protected WebFile() 
	{
    	initialize();
	}
    
    /**
     * 객체 초기화 작업 (내부에서 재초기화가 필요하여 생성자에서 분리)
     */
    protected void initialize()
    {
    	//프록시 설정.(HTTP는 가능하지만, HTTPS는 4.x버전부터는 터널링만 지원하고 있어서 확인 안됨).
    	//HttpHost proxy = new HttpHost("127.0.0.1", 8888);
    	//DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

    	PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		httpClient = HttpClients.custom().setConnectionManager(cm).build();//.setRoutePlanner(routePlanner)
		cookieStore = new BasicCookieStore();					// Create Cookie Store
		context = HttpClientContext.create();					// Create local HTTP context		
		context.setCookieStore(cookieStore);					// Bind custom cookie store to the local context
		context.setAttribute("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36");
		
		this.setDownloadSpeed(DownloadSpeed);
    }
    
    /**
     * 연속적인 다운로드 개수 제어 
     * (기본값 : 1000개 다운로드 시 5분 휴식) 
     * @param _maxCount
     * @param _wait
     */
    public void setLimitation(int _maxCount, int _wait)
    {
    	LimitCount = _maxCount;
    	WaitMinute = _wait;
    }
    
    /**
     * 다운로드 속도를 설정함
     * @param _downSpeed 분당 다운로드 요청가능한 개수
     */
    public void setDownloadSpeed(long _downSpeed)
    {
    	DownloadSpeed = _downSpeed;
    	DownloadInteval = (long)(1000*60/DownloadSpeed);
    }
    
    /**
     * 재요청 횟수 설정
     * @param _count
     */
    public void setRetryCount(int _count)
    {
    	this.MaxRetry = _count;
    }
    

    /**
     * 재시도시 몇초간 쉬었다가 작업할지를 결정
     * @param _time 쉬는 시간 (초 단위)
     */
    public void setRetrySleepTime(long _time)
    {
    	this.ReconnectionSleepTime = _time;
    }


    /**
     * WebFile에 대한 모든 리소스를 해제하고 닫음.
     */
	public void close()
	{
		try {
			httpClient.close();
		} catch (IOException e) {
			log.printStackTrace(e);
		}
		httpClient = null;
		cookieStore = null;
		context = null;		
	}
	
	/**
	 * 객체 삭제 후 재연결. (기본값만큼 휴식)
	 * @return
	 */
	private boolean reconnection() {
		return reconnection(ReconnectionSleepTime);
	}
	
	/**
	 *  객체 삭제 후 재연결.
	 * @param _sleepTime
	 * @return
	 */
	private boolean reconnection(long _sleepTime) {
		//기존연결 종료
		this.close();
		
		//휴식
		try {
			
			Thread.sleep(_sleepTime);
		} catch (InterruptedException e) {
			log.printStackTrace(e);
		}
		
		//초기화
		initialize();
		
		return true;
	}
	
  
	
	/////////////////////////////////////////////////////////////
	//		객체 : 메인 서비스 메소드
	/////////////////////////////////////////////////////////////
	 /**
     * URL페이지에 로그인 시도. POST 방식
     * @param _url
     * @param _param
     * @return 요청 결과 페이지의 HTML(XML) string
     */
	public String getBody(String _url, Map<String,String> _param) {
		
		//파라메터 인코딩
		List<NameValuePair> params = convertParam(_param);
		UrlEncodedFormEntity encodedParams = new UrlEncodedFormEntity(params, Charset.forName("UTF-8"));
		
		//POST 객체 선언
		HttpPost httpost = new HttpPost(_url);
		httpost.setEntity(encodedParams);	//POST method 파라메터 설정.

		
		//서버에 접속요청
		controlSpeed();
		String retBody=null;
		int responseCode = 0;
        CloseableHttpResponse response = null;
		try {
			//접속시도시 거부되면 다시 전송
			HttpHostConnectException exception = null;
			int retry = 0;
			do{
				try{
					response = httpClient.execute(httpost, context);
				}
				catch(HttpHostConnectException e)	//Connection refused://서버측에서 연결 차단.
				{
					exception = e;
					log.error("Retry Connection ("+retry+") : "+e.getMessage());
					reconnection();
					continue;
				}
				
				responseCode = response.getStatusLine().getStatusCode();
				if(responseCode==0)
				{
					//무슨 경우지....?
					log.error("Responsed 0,   Wait and retry ("+retry+") : It will restart 1 minutes");
					reconnection(60000L);	//1분.
					continue;
				}
				
		        if (!(responseCode==200||(responseCode >=301 && responseCode <=303))) throw new Exception();
					
	            //응답이 redirection인 경우
				if((responseCode >=301 && responseCode <=303)){

					String newURL = getCompatibilityURI(httpost.getURI(),response.getFirstHeader("Location").getValue());
	            	response.close();						//기존응답 닫기
	            	httpost.setURI(URI.create(newURL));		//새 URL로 변경
	            	log.warn("redirection : "+ newURL);
	            	continue;
	            }	
				break; 
				
			}while(++retry<MaxRetry);
			if(retry>=MaxRetry) throw exception;
			
						
            HttpEntity entity = response.getEntity();												//http정보획득
            ContentTypeExtended type = ContentTypeExtended.getContentType(entity.getContentType().toString());	//인코딩정보
            
            retBody = EntityUtils.toString(entity, type.CharSet);
		}
		catch(Exception e){
			log.error("ResponseCode = "+responseCode+", URL="+ _url);
			log.printStackTrace(e);
			
			retBody = null;
		}
		finally {
			try {
				response.close();
			} catch (Exception e) {
				log.printStackTrace(e);
				retBody = null;
			}
		}
        return retBody;        
	}
	
	/**
	 * 웹페이지의 내용을 반환. (GET 방식)
	 * @param _url
	 * @return
	 * @throws Exception
	 */
	public String getBody(String _url) {
		
		controlSpeed();
		String retBody="";;
		int responseCode = 0;
		CloseableHttpResponse response = null;
        try {
        	
        	HttpGet request = new HttpGet(_url);
        	//request.setHeader("Content-Type","text/html; charset=UTF-8");
        	//URLEncodedUtils.parse(_url,"ISO-8859-1");
        	
        	//접속시도시 거부되면 다시 전송
			HttpHostConnectException exception = null;
			int retry = 0;
			do{
				try{
		            response = httpClient.execute(request, context);
				}
				catch(HttpHostConnectException e)	//Connection refused://서버측에서 연결 차단.
				{
					exception = e;
					log.error("Retry Connection ("+retry+") : "+e.getMessage());
					reconnection();
					continue;
				}
				
				responseCode = response.getStatusLine().getStatusCode();
				
				if(responseCode==0)
				{
					//무슨 경우지....?
					log.error("Responsed 0,   Wait and retry ("+retry+") : It will restart 1 minutes");
					reconnection(60000L);	//1분.
					continue;
				}
                
		        if (!(responseCode==200||(responseCode >=301 && responseCode <=303))) throw new Exception();
					
	            //응답이 redirection인 경우
				if((responseCode >=301 && responseCode <=303)){
					
					//redirection용 새주소 생성.
					String newURL = getCompatibilityURI(request.getURI(),response.getFirstHeader("Location").getValue());
	            	response.close();						//기존응답 닫기
	            	request.setURI(URI.create(newURL));		//새 URL로 변경
	            	log.warn("redirection : "+ newURL);
	            	continue;
	            }	
				
				
				break; 
			}while(++retry<MaxRetry);
			if(retry>=MaxRetry) throw exception;

			
            HttpEntity entity = response.getEntity();												//http정보획득
            ContentTypeExtended type = ContentTypeExtended.getContentType(entity.getContentType().toString());	//인코딩정보
            
            retBody = EntityUtils.toString(entity, type.CharSet);//50MB짜리 페이지 변환하는데 에러 발생. (heap size overflow....)

        }
        catch(SocketException se)
        {
        	log.error("Socket Error trying to get "+_url);
        	log.printStackTrace(se);
        	retBody = null;
        }
        catch(Exception e){
			log.error("ResponseCode = "+responseCode+", URL="+ _url);
			log.printStackTrace(e);
			retBody = null;
		}
        finally {
        	try {
				response.close();
			} catch (Exception e) {
				log.printStackTrace(e);
				retBody = null;
			}
        }
        
        return retBody;
        
        //Future<HttpResponse> future = httpClient.excute(request,null);   이것은 무엇에 쓰는 물건인고?
	}
	
	/**
	 * 입력받은 주소에 도메인이 없으면 도메인을 채움.
	 * @param _base
	 * @param _str
	 * @return
	 */
	private String getCompatibilityURI(URI _base, String _str)
	{
		//redirection용 새주소 생성.
		String ret = null;
		if(_str.startsWith("http://")==false && _str.startsWith("https://")==false)
		{
        	String host = _base.getHost().trim();
        	if(host.endsWith("/")==false) host = host + "/";
        	ret = _base.getScheme()+"://"+host + _str;
		}
		else
			ret = _str;
		
		return ret;
	}
	

	/**
	 * 웹페이지에 파일 다운로드를 요청
	 * @param _url
	 * @return
	 */
	public DownloadFile getFileString(String _url)
	{
		controlSpeed();
		
		DownloadFile file=null;
		
		int responseCode = 0;
		CloseableHttpResponse response = null;
        try {
            HttpGet request = new HttpGet(_url);
            request.setHeader("Content-Type","text/html; charset=UTF-8");
 
            response = httpClient.execute(request, context);
            responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) throw new SocketException();
            
            
            HttpEntity entity = response.getEntity();						//http정보획득
            
            String data="";
    		ContentTypeExtended etype = ContentTypeExtended.getContentType(entity.getContentType().toString());	//인코딩정보
    		
    		//Data 생성.
            if (entity != null) {
                InputStream instream = entity.getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[256];
                int cnt;
                
                while((cnt = instream.read(buffer))!=-1)
                {
                	bos.write(buffer,0, cnt);
                }
                
                data = Base64.encodeBase64String(bos.toByteArray());
            }

            file = new DownloadFile(etype.Type, data, TextUtil.getSizeBinary(data));
            
        }
        catch(SocketException e)
        {
        	log.error("ResponseCode = "+responseCode+", URL="+ _url);
        	log.printStackTrace(e);
        	file = null;

        }
        catch(Exception e) {
        	log.error("Error processing downloadFile, URL="+ _url);
        	log.printStackTrace(e);
        	file = null;

        }
        finally {
        	try {
				response.close();
			} catch (Exception e) {
				log.printStackTrace(e);
				file = null;
			}
        }
        
        return file;
	}

	


	/////////////////////////////////////////////////////////////////
	//  유틸 메소드
	/////////////////////////////////////////////////////////////////


    
  	/////////////////////////////////////////////////////////////
  	//		객체 : 유틸 메소드
  	/////////////////////////////////////////////////////////////
	/**
	 * 웹서버에서 데이터를 가져오는 시간을 제어한다. 너무 빠른 속도로 접근하면 중간에 휴식
	 */
	private void controlSpeed() {
		
		//연속다운로드가 0인경우 바로 반환.
		if (CurrentCount == 0) {
			LastTime = DateTime.now().getMillis();
			CurrentCount++;
			return;
		}

		//연속다운로드 개수 초과시 일정시간 대기
		if (CurrentCount >= LimitCount) {
			log.info("Limit " + LimitCount + " exceeded. Rest of " + WaitMinute	+ " minutes.");
			
			// 지정된 시간 휴식.
			try {
				Thread.sleep(WaitMinute * 60 * 1000);
			} catch (InterruptedException e) {
				log.error("Thread Sleep Error. just Go!");
				log.printStackTrace(e);
			}
			CurrentCount = 0;
		}

		// 연속 다운로드 중 : 각 다운로드 간격 조절
		long now = DateTime.now().getMillis(); // 현재시간 가져오기;
		long dt = now - LastTime;
		long diff = DownloadInteval - dt;

		// dt가 제한시간보다 짧은 경우 제한시간 만큼 휴식
		if (diff > 0) {
			try {
				Thread.sleep(diff);
			} catch (InterruptedException e) {
				log.error("Thread Sleep Error. just Go!");
				log.printStackTrace(e);
			}
		}

		// 다음 처리를 위한 계산.
		LastTime = DateTime.now().getMillis();
		CurrentCount++;
		if(diff>0)
			log.info("[" + CurrentCount + "/" + LimitCount + "] Rested " + diff	+ " milliseconds for adjust time-rate.");
		else 
			log.info("[" + CurrentCount + "/" + LimitCount + "] Rested 0 milliseconds");

		return;
	}
      
  	
  	/**
  	 * 맵 파라메터를 ListValuePair로 변환.
  	 * @param params
  	 * @return
  	 */
      protected List<NameValuePair> convertParam(Map<String, String> params){
          List<NameValuePair> paramList = new ArrayList<NameValuePair>();
          Iterator<String> keys = params.keySet().iterator();
          while(keys.hasNext()){
              String key = keys.next();
              paramList.add(new BasicNameValuePair(key, params.get(key).toString()));
          }
           
          return paramList;
      }


  	/**
  	 * 현재 쿠키의 정보를 출력 : 테스트 함수
  	 */
  	public void printCookie()
  	{
  		//시스템 쿠키정보 출력
  		for(Cookie c :cookieStore.getCookies())
  		{
  			log.info("[network] Login cookie : " + c);
  		}
  	}
  	
	
	
}
