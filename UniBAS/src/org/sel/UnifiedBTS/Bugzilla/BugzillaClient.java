package org.sel.UnifiedBTS.Bugzilla;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.sel.UnifiedBTS.Bugzilla.Model.BugHistory;
import org.sel.UnifiedBTS.Bugzilla.Model.BugReport;
import org.sel.UnifiedBTS.Bugzilla.Model.Product;
import org.sel.UnifiedBTS.Bugzilla.Model.PropertyItem;
import org.sel.UnifiedBTS.Bugzilla.Model.Relationship;
import org.sel.UnifiedBTS.Bugzilla.Model.RelationshipType;
import org.sel.UnifiedBTS.Bugzilla.Model.Vote;
import org.sel.UnifiedBTS.Core.Database.SQLConnectionException;
import org.sel.UnifiedBTS.Core.Model.SiteInfo;
import org.sel.UnifiedBTS.Core.Network.WebCacheFile;
import org.sel.UnifiedBTS.Util.DateUtil;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.log;

public class BugzillaClient {
	
	/////////////////////////////////////////////////////////////////
	//  관리용 관련 멤버변수.
	/////////////////////////////////////////////////////////////////
	private SiteInfo ThisSite = null;
	private BugzillaDBAdapter Adapter= null;
	public BugzillaParser Parser = null;
	private WebCacheFile Web = null;
	
	private final int MAX_RETRY = 10;
	
	
	/////////////////////////////////////////////////////////////////
	//  사이트 관련 URL
	/////////////////////////////////////////////////////////////////
	public String baseURL = null;
	private String productURL = "describecomponents.cgi?full=1";
	private String componentURL = "describecomponents.cgi?product={0}";
	private String keywordURL = "describekeywords.cgi";
	
	private String listURL = "buglist.cgi?query_format=advanced&columnlist=opendate&f1=bug_id&o1=greaterthan&v1={0}&f2=bug_id&o2=lessthaneq&v2={1}";
	
	private String reportURL = "show_bug.cgi?id={0}&ctype=xml";
	private String activityURL = "show_activity.cgi?id={0}";
	private String voteURL = "page.cgi?id=voting/bug.html&bug_id={0}";

	private String loginURL = "index.cgi";//?GoAheadAndLogIn=1";
	private String logoutURL = "index.cgi?logout=1";
	private String PreferenceURL = "userprefs.cgi";
	
	
	/////////////////////////////////////////////////////////////////
	//  삭제예정
	/////////////////////////////////////////////////////////////////
	//private String userURL = "user_profile.cgi?login={0}";
	//private String listURL = "buglist.cgi?query_format=advanced&f1=creation_ts&o1=greaterthaneq&v1={0}&f2=creation_ts&o2=lessthan&v2={1}&ctype=atom";
	
	//관련된 버그를 찾을 시, 생성일 확인가능한 페이지.
	private String OpenDateSearch = "buglist.cgi?query_format=advanced&columnlist=opendate&f1=bug_id&o1=anyexact&v1={0}";
	
	

	
	/////////////////////////////////////////////////////////////////
	//  작업상태 관련변수
	/////////////////////////////////////////////////////////////////
	private String Url = null;
	private String HttpString = null;
	
	
	///////////////////////////////////////////////////////////////
	// 생성자 및 외부 노출 메서드
	/////////////////////////////////////////////////////////////
	public BugzillaClient(SiteInfo _info, BugzillaDBAdapter _adapter) {
		Parser = new BugzillaParser();					//Parser 예상되는 에러 없음
		Web = WebCacheFile.getInstance();
		Adapter = _adapter;
		ThisSite = _info;
		
		baseURL = _info.BaseUrl;
		if (baseURL.charAt(baseURL.length()-1)!='/')
			baseURL = baseURL + "/";
	}

	
	
	/////////////////////////////////////////////////////////////////
	//  Getter 정의
	/////////////////////////////////////////////////////////////////
	/**
	 * 로그인할 주소를 반환
	 * @return
	 */
	private String getLoginURL() {
		return baseURL+loginURL;
	}
	
	
	private String getLogoutURL() {
		return baseURL+logoutURL;
	}
	
	
	private String getPreferenceURL() {
		return baseURL+PreferenceURL;
	}
	

	private String getListURL(int _sID, int _eID) {
		String ret;
		ret = listURL.replaceFirst("\\{0\\}", Integer.toString(_sID));
		ret = ret.replaceFirst("\\{1\\}", Integer.toString(_eID));
		
		return baseURL + ret;
	}
	
	private String getReportURL(int _bugID)
	{
		return baseURL + reportURL.replaceFirst("\\{0\\}", String.valueOf(_bugID));
	}
	
	
	private String getVotesURL(int _bugID)
	{
		return baseURL + voteURL.replaceFirst("\\{0\\}", String.valueOf(_bugID));
	}
	
	private String getActivityURL(int _bugID)
	{
		return baseURL + activityURL.replaceFirst("\\{0\\}", String.valueOf(_bugID));
	}
	
	private String getProductURL()
	{
		return baseURL + productURL;
	}
	
	private String getComponentURL(String _product)
	{
		String product;
		try {
			product = URLEncoder.encode(_product,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.printStackTrace(e);
			product = _product.replaceAll(" ", "%20");
		}
		return baseURL + componentURL.replaceFirst("\\{0\\}", product);
	}
	
	
	private String getTagURL()
	{
		return baseURL + keywordURL;
	}
	
	

	/**
	 * 지정일 이상에 발생된 버그리포트를 가져올 수 있는 URL을 생성
	 * @param _date
	 * @return
	 */
	public String getListOverDateURL(String _date)
	{
		String listURL_Overdeate = "buglist.cgi?query_format=advanced&columnlist=opendate&order=bug_id%20ASC&f1=creation_ts&o1=greaterthaneq&v1={0}";
		String url = baseURL + listURL_Overdeate.replaceFirst("\\{0\\}", _date);  
		return url;
	}
	
	
	/////////////////////////////////////////////////////////////////
	//  보조 서비스 메소드
	/////////////////////////////////////////////////////////////////
	/**
	 * 작업에 사용된 마지막 접근 Url을 반환한다.
	 * @return
	 */
	public String getLastUrl()
	{
		return Url;
	}
	
	/////////////////////////////////////////////////////////////////
	//  전처리 작업에 관한 메소드
	/////////////////////////////////////////////////////////////////	
	/**
	 * 지정된 사이트에 로그인을 수행
	 * TODO : 로그인이 안되는 사이트에 대한 해결
	 * @return 로그인 성공여부
	 */
	public boolean login()
	{
		//1차 페이지 요청
		Web.printCookie();
		HttpString = Web.getBody(this.getLoginURL(), true);
		if(HttpString==null){
			log.error("Failed to get login page.");
			return false;
		}
		Web.printCookie();				//테스트코드
		//TextUtil.writeTextFile("C:\\_temp\\login_ready.html", HttpString, null);//테스트코드
		
		//파라메터 생성
		Map<String,String> loginParams = Parser.getLoginParams(HttpString, ThisSite.UserID, ThisSite.UserPW);
		Parser.printMap(loginParams);	//테스트코드
		Web.printCookie();				//테스트코드
		
		//2차 로그인
		HttpString = Web.getBody(this.getLoginURL(), loginParams, true);
		//if(HttpString!=null)
			//TextUtil.writeTextFile("C:\\_temp\\login_result.html", HttpString, null);
		
		//로그인 결과 반환
		return Parser.verifyLogin(HttpString);
	}
	
	/**
	 * 연결된 클라이언트의 TimeZone을 설정
	 * 입력값이 null이면 UTC로 설정
	 * @param _zonename
	 * @return
	 */
	public boolean setTimezone(String _zonename)
	{
		if (_zonename == null)
			_zonename = "UTC";
		
		
		//환경설정 페이지 얻기
		boolean ret = false;
		HttpString = Web.getBody(this.getPreferenceURL(), true);
		if(HttpString==null){
			return false;
		}
		
		//환경설정 변경
		Map<String,String> params = Parser.getPreferenceParams(HttpString);
		
		//환경설정 셋팅
		HttpString = Web.getBody(this.getPreferenceURL(), params, true);
		ret = Parser.verifyResult(HttpString);
		
		return ret;
	}
	
	
	/**
	 * 연결된 사이트의 제품정보를 가져온다.
	 * @return
	 */
	public List<Product> getProducts()
	{
		List<Product> products = null;
		
		//프로덕트에 대한 정보 수집
		Url = this.getProductURL();
		HttpString = Web.getBody(Url, false);
		products = Parser.analysisProduct(HttpString);
		
		return products;		
	}
	
	
	/**
	 * 연결된 사이트의 컴포넌트정보를 가져온다.
	 * @param _productName 제품명
	 * @return
	 */
	public List<PropertyItem> getComponent(String _productName)
	{
		List<PropertyItem> components = null;
		
		Url = this.getComponentURL(_productName);
		HttpString = Web.getBody(Url, false);
		components = Parser.analysisComponent(HttpString);
		
		return components;		
	}
	
	/**
	 * 연결된 사이트의 컴포넌트정보를 가져온다.
	 * @param _productName 제품명
	 * @return
	 */
	public List<PropertyItem> getKeywords()
	{
		List<PropertyItem> keywords =null;

		Url = this.getTagURL();
		HttpString = Web.getBody(Url, false);
		keywords = Parser.analysisTags(HttpString);
		
		return keywords;		
	}
	
	
	
	/////////////////////////////////////////////////////////////////
	//  메인 작업에 관한 메소드
	/////////////////////////////////////////////////////////////////	
	/**
	 * 일정범위 내의 수집가능한 버그리포트 아이디를 가져옴.
	 * @return 버그리포트 아이디 리스트
	 */
	public List<Integer> getList(int _startID, int _endID)
	{
		List<Integer> list = new ArrayList<Integer>();

		//Url가져오기
		Url = this.getListURL(_startID, _endID);
		
		
		//잘못된 다운로드 처리.
		int retry = 0;
		boolean reDownload = false;
		do{
			//웹에서 다운로드.
			HttpString = Web.getBody(Url,reDownload);
			if(HttpString==null) return null;
			
			//리스트 확인
			list = Parser.getBuglist(HttpString);
			if(list==null) {
				reDownload = true;
				continue;
			}
			break;
		}while(++retry <MAX_RETRY);
		if(retry>=MAX_RETRY){
			log.error("buglist page cannot analysis" + Url);
			return null;
		}
		
		return list;		
	}
	

	
	/**
	 * 버그리포트 페이지를 가져와서 분석하여 저장하는 전체 과정을 다룸
	 * @param _url
	 * @return
	 */
	public BugReport getBugreport(int bug_id)
	{
		//버그리포트 페이지 분석.
		BugReport report = null;
		Url = getReportURL(bug_id);
		

		boolean redownload = false;
		int retry=0;
		do{	
		
			log.info("BugReport analysis : " + Url);		//로깅
			
			//웹에 버그리포트 요청.
			HttpString = Web.getBody(Url, redownload);					//웹에 페이지 요청
			if(HttpString==null){
				log.error("BugReport download Error : " + Url);		//오류 표시하고 다음으로 넘어감
				redownload = true;
				continue;
			}
			
			if(Parser.checkLoginXml(HttpString)==false)
			{
				log.error("Login error. retry login. : " + Url);			//오류 표시하고 다음으로 넘어감
				login();
				redownload = true;
				continue;
			}
			
			//버그리포트 페이지 분석.
			report = Parser.analysisReport(HttpString, ThisSite.UserID);	//페이지 결과분석
			if(report==null){
				log.error("BugReport analysis Error : " + Url);		//오류 표시하고 다음으로 넘어감.
				redownload = true;
				continue;
			}
			break;
		
		}while(++retry<MAX_RETRY);
		if(retry>=MAX_RETRY){
			log.error("BugReport page cannot analysis" + Url);
			return null;
		}
		
        return report;
	}

	
	
	
	/**
	 * 웹에서 히스토리를 가져와서 분석하여 히스토리 리스트를 반환한다.
	 * @param _bugID
	 * @return
	 */
	public List<BugHistory> getHistories(int _bugID) {

		//버그리포트 History 페이지 분석.
		List<BugHistory> histories =null;
		HttpString = null;
		int retry = 0;
		boolean reDownload = false;
		
		do{
			
			Url = this.getActivityURL(_bugID);
			
			HttpString = Web.getBody(Url, reDownload);
			if(HttpString==null){
				log.error("BugHistory download Error : " + Url);			//오류 표시하고 다음으로 넘어감
				continue;
			}
			
			if(Parser.checkLoginHtml(HttpString)==false)
			{
				log.error("Login error. retry login. : " + Url);			//오류 표시하고 다음으로 넘어감
				login();
				reDownload = true;
				continue;
			}
			
			//버그 히스토리 분석
			histories = Parser.analysisHistory(HttpString);
			if(histories==null)
			{
				log.error("BugHistory analysis Error : " + Url);			//오류 표시하고 다음으로 넘어감.
				reDownload = true;
				continue;
			}
			
			break;
			
		}while(++retry <MAX_RETRY);
		if(retry>=MAX_RETRY){
			log.error("BugHistory page cannot analysis" + Url);
			return null;
		}
		
		return histories;
	}



	/**
	 * 웹에서 투표정보를 가져와서 투표리스트를 생성한다.
	 * @param _bugID
	 * @return
	 */
	public List<Vote> getVotes(int _bugID) throws SQLConnectionException {
		List<Vote> votes = null;
		int retry = 0;
		boolean reDownload = false;
		
		do{
			Url = this.getVotesURL(_bugID);
			
			//상태 업데이트 : 웹 요청
			Adapter.update_voteURL(_bugID, Url);
			
			HttpString = Web.getBody(Url, reDownload);
			if(HttpString==null){
				log.error("Vote download Error : " + Url);			//오류 표시하고 다음으로 넘어감
				continue;
			}
			
			if(Parser.checkLoginHtml(HttpString)==false)
			{
				log.error("Login error. retry login. : " + Url);			//오류 표시하고 다음으로 넘어감
				login();
				reDownload = true;
				continue;				
			}
			
			
			votes = Parser.analysisVotes(HttpString);
			if(votes==null){
				log.error("Vote Anslysis Error : " + Url);			//오류 표시하고 다음으로 넘어감
				reDownload = true;
				continue;
			}
						
			//상태 업데이트  : 파싱 완료.
			Adapter.update_voteParsed(_bugID);
			break;

		}while(++retry <MAX_RETRY);
		if(retry>=MAX_RETRY){
			log.error("Vote page cannot analysis" + Url);
			return null;
		}
		return null;
	}

	


	/**
	 * 현재 수집시작시간의 마지막 버그리포트 아이디를 가져옴.
	 * @return
	 */
	public int getLastBugID() {
		
		//리스트를 수집하기위한 날짜 계산
		DateTime limit =  DateUtil.getDate("1998-01-01");
		DateTime now = DateTime.now();
		
		int id=-1;
		List<Integer> list = null;
		
		//날짜를 감소하면서 작업.
		while(now.compareTo(limit)>=0)
		{
			log.info("Searching for Last bug report...."+DateUtil.getDateString(now));
			
			Url = getListOverDateURL(DateUtil.getDateString(now));
			
			//잘못된 다운로드 처리.
			int retry = 0;
			boolean reDownload = false;
			do{
				//웹에서 다운로드.
				HttpString = Web.getBody(Url,reDownload);
				if(HttpString==null) return -1;
				
				//리스트 확인
				list = Parser.getBuglist(HttpString);
				if(list==null) {
					reDownload = true;
					continue;
				}
				break;
			}while(++retry <MAX_RETRY);
			if(retry>=MAX_RETRY){
				log.error("buglist page cannot analysis" + Url);
				return -1;
			}
			
			//마지막 항목 반환
			if(list.size()!=0)
			{
				id = list.get(list.size()-1);
				break;
			}
			
			//이전날로 넘어감.
			now = now.minusDays(1); //.add(Calendar.DATE, 1);
		}
			
		return id;
	}
	

	//////////////////////////////////////////////////////
	//   유틸 메소드
	//////////////////////////////////////////////////////
	/**
	 * URL로 부터 버그ID를 추출함
	 * @param _url
	 * @return 있으면 해당 ID, 없으면 -1반환.
	 */
	private int getBugID(String _url) {
		//베이스 도메인과 일치하는지 확인.
		String prefix = baseURL + reportURL.substring(0,reportURL.lastIndexOf('&')-3);
		if(_url.startsWith(prefix)==false) return -1;

		String strID =_url.substring(prefix.length());
		int idx = strID.indexOf('&');
		if(idx>0)
			strID = strID.substring(0,idx);
		
		int ret =-1;
		try{
			ret = Integer.parseInt(strID);		
		}
		catch(NumberFormatException e)
		{
			log.printStackTrace(e);
		}
		return ret;
	}
	
	

	//////////////////////////////////////////////////////
	//   보존 코드
	//////////////////////////////////////////////////////
		

	/**
	 * 로그인에 필요한 파라메터들을 반환.
	 * @param _id
	 * @param _pw
	 * @return
	 */
	private Map<String, String> getLoginParam(String _id, String _pw) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("Bugzilla_login", _id);
		params.put("Bugzilla_password", _pw);
		params.put("Bugzilla_password_dummy_top", "password");
		
		return params;
	}
	
	
}
