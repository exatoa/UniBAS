package org.sel.UnifiedBTS.Mantis;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.sel.UnifiedBTS.Bugzilla.BugzillaParser;
import org.sel.UnifiedBTS.Core.Model.DownloadFile;
import org.sel.UnifiedBTS.Core.Model.SiteInfo;
import org.sel.UnifiedBTS.Core.Network.WebCacheFile;
import org.sel.UnifiedBTS.Mantis.Model.Attachment;
import org.sel.UnifiedBTS.Mantis.Model.BugHistory;
import org.sel.UnifiedBTS.Mantis.Model.BugReport;
import org.sel.UnifiedBTS.Mantis.Model.Category;
import org.sel.UnifiedBTS.Mantis.Model.Project;
import org.sel.UnifiedBTS.Mantis.Model.PropertyItem;
import org.sel.UnifiedBTS.Mantis.Model.User;
import org.sel.UnifiedBTS.Util.Config;
import org.sel.UnifiedBTS.Util.DateUtil;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.log;

public class MantisClient {

	/////////////////////////////////////////////////////////////////
	//  관리용 멤버변수들.
	/////////////////////////////////////////////////////////////////
	private SiteInfo ThisSite;					//수집대상 사이트 정보
	private MantisDBAdapter Adapter = null;		//DB 어댑터
	private MantisParser Parser = null;			//파서
	private WebCacheFile Web = null; 			//웹에 파일 요청 클래스
	
	
	
	private String url = null;
	private String strHttp = null;
	
	
	private int MAX_RETRY = 5;
	
	/////////////////////////////////////////////////////////////////
	//  사이트 관련 URL
	/////////////////////////////////////////////////////////////////
	private String baseURL = "http://www.mantisbt.org/bugs/";	
	//private String listURL = "plugin.php?page=XmlImportExport/export";		지원안하는데가 있음.
	private String listURL = "view_all_bug_page.php";//"print_all_bug_page.php";//
	private String reportURL = "view.php?id={0}";
	private String loginURL = "login.php";
	private String logoutURL = "logout_page.php";
	private String prefURL = "account_prefs_update.php";
	private String prefRequestURL = "account_prefs_page.php";
	private String filedownloadURL = "file_download.php?file_id={0}&type=bug";
	private String searchURL = "view_filters_page.php?for_screen=1&view_type=advanced&target_field=tag_string";
	private String reportIssueURL = "bug_report_page.php";
	private String settingURL = "set_project.php";


	
	///////////////////////////////////////////////////////////////
	// 생성자 및 외부 노출 메서드
	/////////////////////////////////////////////////////////////
	
	public MantisClient(SiteInfo _info, MantisDBAdapter _adapter) {
		Parser = new MantisParser();					//Parser 예상되는 에러 없음
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

	/*--------URL 반환함수 ---------------*/
	private String getLoginURL()		{ return baseURL + loginURL; }
	private String getLogoutURL()	{ return baseURL + logoutURL; }
	private String getListURL() 		{ return baseURL + listURL;}
	private String getSearchPageURL(){ return baseURL + searchURL; }
	private String getPreferenceURL(){ return baseURL + prefURL; }
	private String getPrefRequestURL(){ return baseURL + prefRequestURL; }
	

	/**
	 * 버그리포트 등록 페이지 오픈
	 * 실패시 _fail을 true로 주면 프로젝트 설정페이지를 반환.
	 * @param _fail
	 * @return
	 */
	private String getReportIssueURL(boolean _fail){
		if(_fail==true)
			return baseURL + settingURL;
		return baseURL + reportIssueURL; 
	}
	
	
	private String getReportURL(int _bugID)
	{
		return baseURL + reportURL.replaceFirst("\\{0\\}", Integer.toString(_bugID));
	}
	
	private String getDownloadURL(int _id) {
		return baseURL + filedownloadURL.replaceFirst("\\{0\\}", Integer.toString(_id));
	}

	
	
	/*--------파라메터 반환함수 ---------------*/
	private Map<String, String> getLoginParam(String _id, String _pw) {
		Map<String, String> params = new HashMap<String, String>();
		
		params.put("username", _id);
		params.put("password", _pw);
		params.put("return", "index.php");
		
		return params;
	}
	
	private Map<String, String> getPreferenceParam(String _token, String _id) {
		Map<String, String> params = new HashMap<String, String>();
		
		params.put("redirect_url", "account_prefs_page.php");
		params.put("account_prefs_update_token", _token);
		params.put("user_id", _id);
		
		params.put("default_project","0");
		params.put("refresh_delay","30");
		params.put("redirect_delay","2");
		params.put("bugnote_order","ASC");
		params.put("email_on_new","on");
		params.put("email_on_assigned","on");
		params.put("email_on_feedback","on");
		params.put("email_on_resolved","on");
		params.put("email_on_closed","on");
		params.put("email_on_reopened","on");
		params.put("email_on_bugnote","on");
		params.put("email_on_new_min_severity","0");
		params.put("email_on_assigned_min_severity","0");
		params.put("email_on_feedback_min_severity","0");
		params.put("email_on_resolved_min_severity","0");
		params.put("email_on_closed_min_severity","0");
		params.put("email_on_reopened_min_severity","0");
		params.put("email_on_bugnote_min_severity","0");

		params.put("email_on_status","");
		params.put("email_on_status_min_severity","0");
		params.put("email_on_priority","");
		params.put("email_on_priority_min_severity","0");

		params.put("email_bugnote_limit","0");
		
		params.put("timezone", "Africa/Abidjan");//GMT지역
		params.put("language", "english");
		
		return params;
	}
	
	
	private Map<String, String> getListParam(int _year, int _month, int _day) {
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("start_year", Integer.toString(_year));
		params.put("start_month", Integer.toString(_month));
		params.put("start_day", Integer.toString(_day));
		
		params.put("end_year", Integer.toString(_year));
		params.put("end_month", Integer.toString(_month));
		params.put("end_day", Integer.toString(_day));
		
		
		params.put("per_page", "0");			//제한없음
		params.put("do_filter_by_date", "on");	//checked
		params.put("type", "1");					
		params.put("page_number", "1");
		params.put("view_type", "advanced");
		
		params.put("project_id","0");
		//params.put("project_id[]","0");
		params.put("show_category[]","0");
		
		params.put("reporter_id[]","0");
		params.put("user_monitor[]","0");
		params.put("handler_id[]","0");
		params.put("show_severity[]","0");
		params.put("show_resolution[]","0");
		params.put("show_profile[]","0");
		params.put("show_status[]","0");
		params.put("show_priority[]","0");
		params.put("view_state","0");
		params.put("sticky_issues","on");
		params.put("highlight_changed","0");
		params.put("relationship_type","-1");
		params.put("platform[]","0");
		params.put("os[]","0");
		params.put("os_build[]","0");
		params.put("note_user_id[]","0");
		params.put("sort_0","id");
		params.put("dir_0","ASC");
		params.put("sort_1","");
		params.put("dir_1","");
		params.put("match_type","0");
		return params;
	}
	
	
	private Map<String, String> getReportIssueParam(int project_id)
	{
		Map<String, String> params = new HashMap<String, String>();
		params.put("project_id", Integer.toString(project_id));
		
		return params;
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
		return url;
	}
	
	/////////////////////////////////////////////////////////////////
	//  로그인 및 셋팅 관련 코드
	/////////////////////////////////////////////////////////////////
	public boolean login()
	{
		String url = this.getLoginURL();
		Map<String, String> params = this.getLoginParam(ThisSite.UserID, ThisSite.UserPW);
		
		//로그인 결과 페이지 다운로드.
		String strHttp = Web.getBody(url, params);
		if(strHttp==null){
			return false;
		}
		
		//로그인 결과 검증
		if(Parser.checkLoginHtml(strHttp)==false) return false;

		return true;
	}
	
	
	/**
	 * 접속하는 사이트의 timezone을 변경한다.
	 * TODO : 변경해야함.
	 * @return
	 */
	public boolean setTimezone(String _zoneName)
	{
		//환경설정을 읽어오기 위해 웹페이지를 가져옴.
		strHttp = Web.getBody(this.getPrefRequestURL());	//새로 다운로드 필수.
		if(strHttp==null){
			return false;
		}
		
		//변경할 환경변수 지정.
		//Map<String, String> nowPref = Parser.getPreferences(strHttp);
		String prefToken = Parser.getPrefenceToken(strHttp);
		String userID = Parser.getPrefenceUserID(strHttp);
		
		if(prefToken==null || userID==null) return false;
		
		//설정한 결과 반영.
		strHttp = Web.getBody(this.getPreferenceURL(),this.getPreferenceParam(prefToken, userID));
		if(strHttp==null){
			return false;
		}
		
		//설정 검증.
		return Parser.checkPreferenceHtml(strHttp);
	}
	

	/////////////////////////////////////////////////////////////////
	//  전처리 관련 코드
	/////////////////////////////////////////////////////////////////

	private String propertyPage = null;

	/**
	 * 전처리를 위해서 검색페이지를 가져옴. (관련 몇개의 함수는 이 함수가 실행된 후에 실행해야함)
	 */
	public boolean setPreprocessPage()
	{
		propertyPage = Web.getBody(this.getSearchPageURL(), false);
		if(propertyPage==null){
			log.error("Failed to download page for preprocessPage.");
			return false;
		}
		return true;
	}
	
	/**
	 * 프로덕트에 리스트를 반환
	 * setPreprocessPage()가 true를 반환한 후에 작동가능.
	 * @return
	 */
	public List<Project> getProducts() {
		if(propertyPage!=null)
			return Parser.analysisProduct(propertyPage);
		
		return null;
	}
	
	
	/**
	 * 프로덕트에 대한 컴포넌트 리스트를 생성.
	 * @param _productID
	 * @return
	 */
	public List<Category> getCategories(int _productID) {		
				
		strHttp = Web.getBody(getReportIssueURL(false),this.getReportIssueParam(_productID), false);
		if(strHttp==null){
			log.error("Filed to download page for category");
			return null;
		}
		
		List<PropertyItem> list = Parser.analysisSelectOptions(strHttp, "category_id");
		if(list==null){
			log.error("Filed to analysis for category");
			return null;
		}
		
		List<Category> categories = new ArrayList<Category>();
		for(PropertyItem item : list)
		{
			if(item.ID==1) continue;
			categories.add(new Category(item.ID, item.Name, _productID));
		}
		return categories;
	}
	
	/**
	 * Resolution 정보 얻기.
	 * setPreprocessPage()가 true를 반환한 후에 작동가능.
	 * @return
	 */
	public List<PropertyItem> getResolutions() {	
		if(propertyPage==null) return null;
		
		
		List<PropertyItem> resolutions = Parser.analysisSelectOptions(propertyPage, "show_resolution[]");
		if(resolutions==null){
			resolutions = Parser.analysisSelectOptions(propertyPage, "resolution[]");
			if(resolutions==null){
				log.error("Filed to analyze resolutions");
				return null;
			}
		}
		return resolutions;
	}
	
	
	/**
	 * Status 정보들을 가져옴.
	 * setPreprocessPage()가 true를 반환한 후에 작동가능.
	 * @return
	 */
	public List<PropertyItem> getStatuses() {
		if(propertyPage==null) return null;
		
		List<PropertyItem> status = Parser.analysisSelectOptions(propertyPage, "show_status[]");
		if(status==null){
			status = Parser.analysisSelectOptions(propertyPage, "status[]");
			if(status==null){
				log.error("Filed to analyze status");
				return null;
			}
		}
		return status;
	}
	
	
	/**
	 * Priority정보들을 가져옴.
	 *   setPreprocessPage()가 true를 반환한 후에 작동가능.
	 * @return
	 */
	public List<PropertyItem> getPriorities() {
		if(propertyPage==null) return null;
		
		List<PropertyItem> priorities = Parser.analysisSelectOptions(propertyPage, "show_priority[]");
		if(priorities==null){
			priorities = Parser.analysisSelectOptions(propertyPage, "priority[]");
			if(priorities==null){
				log.error("Filed to analyze Prioritiy");
				return null;
			}
		}
		return priorities;
	}
	
	/**
	 * Severity 정보들을 반환하는 함수
	 *   setPreprocessPage()가 true를 반환한 후에 작동가능.
	 * getPreprocess
	 * @return
	 */
	public List<PropertyItem> getSeverities() {	
		if(propertyPage==null) return null;
		
		//Severity
		List<PropertyItem> severities = Parser.analysisSelectOptions(propertyPage, "show_severity[]");
		if(severities==null){
			severities = Parser.analysisSelectOptions(propertyPage, "severity[]");
			if(severities==null){
				log.error("Filed to analyze Severity");
				return null;
			}
		}
		return severities;
	}
	
	/**
	 * Tag정보들을 반환하는 함수
	 *   setPreprocessPage()가 true를 반환한 후에 작동가능.
	 * @return
	 */
	public List<PropertyItem> getTags() {	
		if(propertyPage==null) return null;
		
		//Tags
		List<PropertyItem> tags = Parser.analysisSelectOptions(propertyPage, "tag_select");
		if(tags==null){
				log.error("Filed to analyze Tags");
				return null;
		}
		return tags;

	}
	
	
	/**
	 * Reproducibility값을 가져와서 설정.
	 * @param _productID
	 * @return
	 */
	public List<PropertyItem> getReproducibilities(int _productID) {
		
		List<PropertyItem> reproducibilities = null;
		
		boolean fail = false;
		int retry=0;
		do{	
			url = getReportIssueURL(fail);
			strHttp = Web.getBody(url, this.getReportIssueParam(_productID), true);	//아무 프로덕트 아이디나 필요함.
			if(strHttp==null){
				log.error("Filed to download page for reproducibility");
				return null;
			}
			reproducibilities = Parser.analysisSelectOptions(strHttp, "reproducibility");
			if(reproducibilities==null){
				log.error("Filed to analyze Reproducibility");
				fail = !fail;
				continue;
			}
			break;
		
		}while(++retry<MAX_RETRY);
		if(retry>=MAX_RETRY){
			log.error("BugReport page cannot analysis" + url);
			return null;
		}
		
		
		
		return reproducibilities;
	}
	


	
	/////////////////////////////////////////////////////////////////
	//  버그리포트에 관련된 처리
	/////////////////////////////////////////////////////////////////
	/**
	 * 웹에서 버그리포트를 가져와서 분석한다.
	 * @return
	 */
	public BugReport getBugreport(int _bugID) {

		boolean analysisState = true;		
		BugReport report = null;

		//상세 정보를 위한 페이지 요청.
		url = this.getReportURL(_bugID);
		
		boolean redownload = false;
		int retry=0;
		do{	
		
			log.logging("BugReport analysis : " + url);		//로깅
			
			//웹에 버그리포트 요청.
			strHttp = Web.getBody(url, redownload);			//웹페이지 요청
			if(strHttp==null){
				log.error("BugReport download Error : "+ url);
				retry = retry +1;
				continue;
			}
			
			log.logging("privilige check.");
			if(Parser.checkPrivateReport(strHttp)==true) return new BugReport(0);	//비공개 버그리포트.
			
			log.logging("application error check.");
			MantisException e = Parser.checkReportError(strHttp);
			if (e!=null){//에러가 있음.
				log.warn(e.getMessage());
				if(e.ErrorCode==1100) return new BugReport(-1);			//삭제된 버그리포트
				else if(e.ErrorCode==700) return new BugReport(-2);  	//프로젝트가 삭제됨
				else return new BugReport(-3);							//그외의 에러
			}
			
			log.logging("Login check.");
			if(Parser.checkLoginHtml(strHttp)==false)
			{
				log.error("Login error. retry login. : " + url);			//오류 표시하고 다음으로 넘어감
				login();
				redownload = true;
				continue;
			}
			log.logging("login check_ complete");
			
			break;
		
		}while(++retry<MAX_RETRY);
		if(retry>=MAX_RETRY){
			log.error("BugReport page cannot analysis" + url);
			return null;
		}

		log.logging("analysis start");
		report = Parser.analysisBugreport(strHttp);
		if(report==null){
			log.error("Failed to analysis bugreport on BugID="+ _bugID + "");
			return null;		//그외의 에러.
		}
		
			
		//버그 관계정보 추출
		log.logging("analysis Relationships start");	
		report.Relationships = Parser.analysisRelationships(report.ID, strHttp);
		if(report.Relationships == null){
			log.error("Failed to analysis relationships on BugID="+ report.ID + "");
			analysisState=false;
		}
		
		//버그노트
		log.logging("analysis bugnote start");
		report.Bugnotes = Parser.analysisBugnotes(strHttp);
		if(report.Bugnotes ==null){
			log.error("Failed to analysis bugnotes on BugID="+ report.ID + "");
			analysisState=false;
		}
		
		//버그 히스토리정보추출
		log.logging("analysis history start");
		report.Histories = Parser.analysisHistories(strHttp);
		if(report.Histories==null){
			log.error("Failed to analysis histories on BugID="+ report.ID + "");
			analysisState=false;
		}
		
		//버그 히스토리정보추출
		log.logging("analysis related changeset");
		report.Changesets = Parser.analysisChangesets(strHttp);
		//TODO : Changeset에 대한 처리
//		if(report.Changesets==null){
//			log.error("Failed to analysis changesets on BugID="+ report.ID + "");
//			analysisState=false;
//		}
				
				
		
		//첨부파일과 버그 히스토리 정보를 매칭..
		log.logging("analysis attachment start");
		if(this.MatchAttachHistory(report.Attachments, report.Histories, report.Reporter)==false)
		{
			log.error("Failed to match attachment and history on"+ report.ID + "");
			analysisState=false;
		}
		
		//히스토리에 없는 파일의 경우 소유자가 등록한 것으로 설정
		for(Attachment a: report.Attachments)
		{
			if(a.Attacher!=null && a.Attacher.ID != -1) continue;
			if(a.Attacher!=null && a.Attacher.LoginName!=null) continue;
			
			a.Attacher = new User(report.Reporter);
		}
		
		
		
		//TODO : 삭제대상,   monitor 기능을 포함한 곳이 있는지 검사.
		if(Parser.findMonitor(strHttp)==true)
		{
			log.warn("Found \"monitor\"!!!!!");
		}
		
		if (analysisState==false) return null;
				
		return report;
	}

	/**
	 * 첨부파일 리스트를 웹에서 다운받아서 리스트에 저장.
	 * @param _attaches
	 * @return
	 */
	public boolean getBinaryData(List<Attachment> _attaches)
	{
		boolean ret = true;
		for(Attachment file : _attaches){
			url = this.getDownloadURL(file.ID);
			DownloadFile download = Web.getFileString(url, false);
			if(download==null) {
				//에러 처리.
				log.error("File : "+ file.Filename +". Couldn't download file '"+url+"'");
				ret=false;
				file.MimeType = "Download Error";
				file.FileSize = 0;
				file.Data = null;
				continue;
			}
			file.MimeType = download.MimeType;
			file.FileSize = download.FileSize;
			file.Data = download.Data;
		}
		
		return ret;
	}

	/**
	 * 히스토리 정보와 첨부파일 정보를 매칭하여 첨부파일을 등록한 사용자를 찾음. (attachments변경됨)
	 * @param attachments
	 * @param histories
	 * @return
	 */
	private boolean MatchAttachHistory(List<Attachment> attachments, List<BugHistory> histories, User reporter) {
		boolean ret = true;
		
		if(attachments==null) 		return ret;
		if(attachments.size()==0)	return ret;
		if(histories==null) 		return ret;
		if(histories.size()==0) 	return ret;
		
		
		//파일 리스팅
		for(BugHistory h: histories)
		{
			if(h.FieldID.startsWith("File Added")==false) continue;
			
			String file = h.FieldID.substring(12, h.FieldID.length());
			
			if(file.matches("[0-9]+-")==true)
				file = file.substring(8, file.length());
			
			//파일에서 찾음
			for(Attachment a: attachments)
			{
				if(a.Filename.compareTo(file)!=0) continue;
				
				a.Attacher = new User();
				a.Attacher.ID = h.Who.ID;
				a.Attacher.LoginName = h.Who.LoginName;				
			}
		}
		return ret;
	}
	

	public String getListOverDateURL()
	{
		return baseURL + listURL;
	}

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
			//log.info("test : "+ now.getYear()+", "+now.getMonthOfYear()+", "+now.getDayOfMonth());
			
			//사이트 주소및 파라메터 생성.
			url = getListOverDateURL();
			Map<String, String> param = this.getListParam(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth());
			
			//잘못된 다운로드 처리.
			int retry = 0;
			do{
				//웹에서 다운로드.
				strHttp = Web.getBody(url, param, true);
				if(strHttp==null) return -1;
				
				//리스트 확인
				list = Parser.getBuglist(strHttp);
				if(list==null) {
					continue;
				}
				break;
			}while(++retry <MAX_RETRY);
			if(retry>=MAX_RETRY){
				log.error("buglist page cannot analysis" + url);
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




}
