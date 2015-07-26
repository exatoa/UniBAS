package test.unibas;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTimeZone;
import org.seal.UniBAS.Bugzilla.BugzillaParser;
import org.seal.UniBAS.Bugzilla.Model.BugReport;
import org.seal.UniBAS.Core.Network.ContentTypeExtended;
import org.seal.UniBAS.Core.Network.WebCacheFile;
import org.seal.UniBAS.Util.DateUtil;
import org.seal.UniBAS.Util.SerializedList;
import org.seal.UniBAS.Util.TextUtil;
import org.seal.UniBAS.Util.XML;
import org.seal.UniBAS.Util.TermElement;
import org.seal.UniBAS.Util.log;

public class Test {

	/**
	 * 테스트 가동을 위한 환경
	 * @param args
	 */
	public static void main(String[] args) {
		new Test().run();
	}
	
	/**
	 * 테스트 메인
	 */	
	public void run() {
		//ConvertUrls();
		//TimeTest();
		//bucketTest();
		//XMLTest();
		TextTest();
	}
	
	private void TextTest() {
		String sql = "Select * from  <{0}> where id=10";
		//sql = sql.replaceAll("<\\{"+Integer.toString(0)+"\\}>", args[i]);
		
	}

	String[] dic = {"The","$88","billion","JWST","features","18","hexagonal","mirror","segments","that","will","work","together","to","form","one","21-foot-wide","65","meters","mirror","larger","than","any","other","mirror","that","ever","flown","in","space","NASA","officials","said","For","comparison","the","agency","iconic","Hubble","Space","Telescope","sports","an","8-foot","or","24","m","primary","mirror","JWST","is","optimized","to","view","in","infrared","light","The","telescope","should","be","able","to","do","lots","of","different","things","during","its","operational","life","researchers","say","including","scanning","the","atmospheres","of","alien","planets","for","oxygen","and","other","gases","that","could","be","produced","by","living","organisms","Such","delicate","work","is","best","performed","by","space","telescopes","which","don't","have","to","look","through","Earth","atmosphere","JWST","will","work","in","concert","with","another","NASA","space","mission","in","this","regard","performing","follow-up","observations","on","promising","nearby","worlds","found","by","the","agency","Transiting","Exoplanet","Survey","Satellite","TESS","which","is","scheduled","to","blast","off","in","2017","With","the","James","Webb","we","have","our","first","chance","our","first","capability","of","finding","signs","of","life","on","another","planet","MIT","astrophysicist","Sara","Seager","said","during","Monday","NASA","briefing","Now","nature","just","has","to","provide","for","us","5","Bold","Claims","of","Alien","Life","A","numbers","game","But","nature","may","not","be","so","willing","at","least","during","the","JWST","mission","Seager","and","other","experts","stress","And","it","all","comes","down","to","numbers","There","is","no","shortage","of","planets","in","the","Milky","Way","Our","galaxy","teems","with","at","least","100","billion","planets","10","to","20","percent","of","which","Mountain","said","likely","circle","in","their","host","star","habitable","zone","that","just-right","range","of","distances","that","could","allow","liquid","water","to","exist","on","a","world","surface","If","there","nothing","terribly","special","about","Earth","then","life","should","be","common","throughout","the","cosmos","many","scientists","think","But","most","exoplanets","are","very","far","away","and","all","of","them","are","faint","JWST","while","large","by","current","standards","won't","have","enough","light-collecting","area","to","investigate","more","than","a","handful","of","potentially","habitable","planets","researchers","say","A","spacecraft","with","a","33-foot","10","m","mirror","would","give","researchers","a","much","better","chance","of","finding","biosignatures","in","alien","atmospheres","but","Mountain","would","like","something","even","bigger","With","a","20-meter","telescope","we","can","see","hundreds","of","Earth-like","planets","around","other","stars","he","said","That","what","it","takes","to","find","life"};
	int dic_size = 0;
	
	private void XMLTest() {
		List<TermElement> list = new ArrayList<TermElement>();
		
		dic_size = dic.length;
		
		for(int src=0; src<100; src++){			
			for(int i=0; i<5; i++){
				TermElement e = new TermElement();
				e.SrcType = "C";
				e.SrcID = src;
				e.Seq = i;
				e.Term = TextUtil.addQuotes(dic[(int) ((Math.random()*10000)%dic_size)]);
				e.Stopwords = false;
				list.add(e);	
		}}
		
		for(int src=0; src<5; src++){
			for(int i=0; i<5; i++){			
				TermElement e = new TermElement();
				e.SrcType = "D";
				e.SrcID = src;
				e.Seq = i;
				e.Term = TextUtil.addQuotes(dic[(int) ((Math.random()*10000)%dic_size)]);
				e.Stopwords = false;
				list.add(e);	
		}}
		
		for(int src=0; src<5; src++){
			for(int i=0; i<5; i++){
				TermElement e = new TermElement();
				e.SrcType = "S";
				e.SrcID = src;
				e.Seq = i;
				e.Term = TextUtil.addQuotes(dic[(int) ((Math.random()*10000)%dic_size)]);
				e.Stopwords = false;
				list.add(e);	
		}}
		
		String xml = XML.createElement(list);
		
		System.out.println(xml);
		
	}

	private void bucketTest() {
		
		int i;
		String str = "E:\\_temp\\BTS\\cache2\\www.mantisbt.org\\bugs\\view.php\\0b30ef0e4bad967092d5c6fac5a82d3a.txt";
		String[] list = {"http://www.mantisbt.org/bugs/view.php?id=1"
						,"http://bugs.scribus.net/view_all_bug_page.php?dir_0=ASC&dir_1=&handler_id[]=0&start_month=7&start_year=2014&page_number=1&show_category[]=0&type=1&reporter_id[]=0&os[]=0&show_profile[]=0&show_priority[]=0&view_state=0&sticky_issues=on&end_year=2014&show_resolution[]=0&per_page=0&sort_0=id&match_type=0&user_monitor[]=0&do_filter_by_date=on&platform[]=0&os_build[]=0&start_day=6&end_month=7&show_status[]=0&view_type=advanced&project_id=0&highlight_changed=0&sort_1=&end_day=6&note_user_id[]=0&show_severity[]=0&relationship_type=-1"
						,"http://bugs.scribus.net/bug_report_page.php?project_id=3"
						,
				
		};
		String ret = "";
		for(i=0; i<list.length; i++)
		{
			ret = TextUtil.makeBucket(TextUtil.convertURLtoPath(list[i]), 2,1);
			System.out.println(Integer.toString(i) + " : " + ret);
			
		}
		
	}

	private static void TimeTest()
	{
		String[] time = new String[] {"2014-04-24 11:08","2014-4-24 11:08","14-Jun-24 11:08", "22-04-11 14:43", "10-04-2011 14:43"};
		
		for(int i=0; i<time.length; i++)
		{
			System.out.print(time[i] + ":");
			String ret = DateUtil.getStandardFormat(time[i], DateTimeZone.UTC);
			System.out.println(ret);
		}
		
	}	
	
	
	
	private void ConvertUrls() {
		
		String[] list = {"http://bugs.scribus.net/view.php?id=5397"};
		
		for(int i=0; i<list.length; i++)
		{
			System.out.println(TextUtil.convertURLtoPath(list[i]));
		}
	}


	

	private void TestSerial() {
		List<String> list = new SerializedList<String>("E:\\_temp\\BTS\\testList.txt");
		
		System.out.println("==============PREstate=============");
		int size = list.size();
		for(int i=0; i<size; i++)
		{	
			System.out.println(list.get(i));			
		}
		
		list.add("https://landfill.bugzilla.org/bugzilla-tip/show_bug.cgi?id=1234");
		list.add("https://landfill.bugzilla.org/bugzilla-tip/show_bug.cgi?id=1235");
		list.add("https://landfill.bugzilla.org/bugzilla-tip/show_bug.cgi?id=1236");
		list.add("https://landfill.bugzilla.org/bugzilla-tip/show_bug.cgi?id=1237");
		list.add("https://landfill.bugzilla.org/bugzilla-tip/show_bug.cgi?id=1238");
		list.add("https://landfill.bugzilla.org/bugzilla-tip/show_bug.cgi?id=1239");
		
		System.out.println("==============POST state=============");
		size = list.size();
		for(int i=0; i<size; i++)
		{	
			System.out.println(list.get(i));			
		}
		
	}


	private void TestProxy()
	{
		
		String httpsURL = "https://landfill.bugzilla.org/bugzilla-tip/";
		String query=null;
		try{
			query = "Bugzilla_login="+URLEncoder.encode("forglee@naver.com","UTF-8");
			query += "&";
			query += "Bugzilla_password="+URLEncoder.encode("Sel535447","UTF-8") ;
			query += "&";
			query += "Bugzilla_remember="+URLEncoder.encode("on","UTF-8") ;
			query += "&";
			query += "GoAheadAndLogIn="+URLEncoder.encode("Log in","UTF-8") ;
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		Map<String, String> param = new HashMap<String, String>();
		
		param.put("Bugzilla_loagin", "forglee@naver.com");
		param.put("Bugzilla_password", "Sel535447");
		param.put("Bugzilla_remember", "on");
		param.put("GoAheadAndLogIn", "Log in");
		
		
		
		SampleHTTPS https = new SampleHTTPS();
		String msg="";
		try {
			msg = https.httpsPOST(httpsURL, param);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(msg);
	}
	
	public String httpsPost(String url, String param)
	{
		
		String retString=null;
		try {
			
			URL myurl = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
			con.setRequestMethod("POST");
	
			con.setRequestProperty("Content-length", String.valueOf(param.length())); 
			con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)"); 
			con.setDoOutput(true); 
			con.setDoInput(true); 
	
			DataOutputStream output = new DataOutputStream(con.getOutputStream());  
			output.writeBytes(param);
			output.close();
	
			DataInputStream input = new DataInputStream( con.getInputStream() ); 
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			retString = response.toString();
		
		}
		catch( MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return retString;
	}
	
	 /**
     * URL페이지에 로그인 시도. POST 방식
     * @param _url
     * @param _param
     * @return 요청 결과 페이지의 HTML(XML) string
     */
	
    protected CloseableHttpClient httpClient = null;
    protected HttpClientContext context = null;
    protected CookieStore cookieStore = null;
    
    
	public String getBody(String _url) {
				
		//POST 객체 선언
		HttpGet request = new HttpGet(_url);
		
		//서버에 접속요청
		String retBody=null;
		int responseCode = 0;
        CloseableHttpResponse response = null;
		try {
			//접속시도시 거부되면 다시 전송
			HttpHostConnectException exception = null;
			response = httpClient.execute(request, context);
			
            responseCode = response.getStatusLine().getStatusCode();
            
            System.out.println("responseCode="+ responseCode);
            //if (!(responseCode==200||responseCode==302)) throw new Exception();
			
						
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
	
	private final String USER_AGENT = "Mozilla/5.0";
	
	private String httpGet(String url) throws Exception {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		// optional default is GET
		con.setRequestMethod("GET");
 
		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		return response.toString();
 
	}
	
	

	
	private String baseURL = "https://bugzilla.mozilla.org/";
	private String reportURL = "show_bug.cgi?id={0}&ctype=xml";
	private String StartDate = "2014-05-04";
	private String EndDate = "2014-05-08";

	
	private void testSite()
	{
		String url = "https://bugzilla.mozilla.org/show_bug.cgi?id=732731&ctype=xml";
		
		BugzillaParser parser = new BugzillaParser();
		
		WebCacheFile web = WebCacheFile.getInstance();
		
		System.out.println("URL :"+url);
		String strHttp = web.getBody(url);
		
		System.out.println("Parsing : "+url);
		BugReport report = parser.analysisReport(strHttp, "");
		System.out.println("bugID = "+report.BugID);
		
	}
	
	private void testCompare()

	{
		for(int i=1; i<15; i++)
		{
			String date = "2014-05-";
			String day = Integer.toString(i);
			if (day.length()==1) day = "0"+day;
			date = date+day;
			
			if(DateUtil.compareOverDate("fri 09:11", StartDate, EndDate)==true){
				System.out.println(date + " : 추가할 필요없어, 기간안에 있어");
				continue;
			}
			System.out.println(date + " : 추가해야 돼");
		}
		
	}
//	{
//		
//		String Url = "https://bugzilla.mozilla.org/show_bug.cgi?id=537647";
//
//		if(checkAddReport(Url)==null){
//			System.out.println("null이네");
//			return;
//		}
//		System.out.println("null아니네");
//		
//	}
//	
//	
//	private static String checkAddReportID(String _url)
//	{
//		//베이스 도메인과 일치하는지 확인.
//		String prefix = baseURL + reportURL.substring(0,reportURL.lastIndexOf('&')-3);
//		if(_url.startsWith(prefix)==false) return null;
//		
//		String newUrl =_url.substring(prefix.length());
//		int idx = newUrl.indexOf('&');
//		if(idx>0)
//			newUrl = newUrl.substring(0,idx);
//		newUrl = baseURL + reportURL.replaceFirst("\\{0\\}", newUrl);		//링크주소 생성.
//		
//		 
//		WebCacheFile Web = WebCacheFile.getInstance();
//		String strHttp = Web.getBody(newUrl);
//		BugzillaParser Parser = new BugzillaParser();
//		
//		if(DateUtil.analysisOverDate(strHttp, StartDate, EndDate)==true)
//			return newUrl;
//		
//		return null;
//	}
//	

	
	private Map<String, String> getLoginParam(String _id, String _pw) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("Bugzilla_login", _id);
		params.put("Bugzilla_password", _pw);
		params.put("Bugzilla_password_dummy_top", "password");
		
		return params;
	}
	
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
	 
	 
	 

	
}
