package org.seal.UniBAS.Mantis;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.seal.UniBAS.Mantis.Model.Attachment;
import org.seal.UniBAS.Mantis.Model.BugChangeset;
import org.seal.UniBAS.Mantis.Model.BugHistory;
import org.seal.UniBAS.Mantis.Model.BugReport;
import org.seal.UniBAS.Mantis.Model.Bugnote;
import org.seal.UniBAS.Mantis.Model.Category;
import org.seal.UniBAS.Mantis.Model.Project;
import org.seal.UniBAS.Mantis.Model.PropertyItem;
import org.seal.UniBAS.Mantis.Model.Relationship;
import org.seal.UniBAS.Mantis.Model.RelationshipType;
import org.seal.UniBAS.Mantis.Model.User;
import org.seal.UniBAS.Util.DateUtil;



public class MantisParser
{
	/**
	 * @uml.property  name="undefinedUserID"
	 */
	//private int UndefinedUserID = 90000000;
	

	/////////////////////////////////////////////////////////////////
	// 로그인 및 환경설정 관련 Parsing
	/////////////////////////////////////////////////////////////////
	
	/**
	 * 환경설정값 변경할수 있게 기존의 환경설정 값을 가져오도록 함.
	 * @param _html
	 * @return
	 */
	public Map<String, String> getPreferences(String _html) {
		
		Map<String,String> map = new HashMap<String,String>();

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		Elements forms = doc.select("form");
		
		Element aForm=null;
		for(Element s : forms)
		{
			if (s.attr("action").compareTo("account_prefs_update.php")==0)
			{
				aForm = s;
				break;
			}
		}
		
		if(aForm==null) return null;
		
		Elements inputs = aForm.select("input");
		String value=null;
		for(Element item : inputs)
		{
			if(item.attr("name").compareTo("account_prefs_update_token")==0){
				value = item.attr("value");
				break;
			}
		}
		
		return map;
	}
	
	
	/**
	 * 페이지에서 환경설정  사용자 아이디 값을 가져옴.
	 * TODO : 
	 * @param _html
	 * @return
	 */
	public String getPrefenceUserID(String _html) {
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		Elements forms = doc.select("form");
		
		Element aForm=null;
		for(Element s : forms)
		{
			if (s.attr("action").compareTo("account_prefs_update.php")==0)
			{
				aForm = s;
				break;
			}
		}
		
		if(aForm==null) return null;
		
		Elements inputs = aForm.select("input");
		String value=null;
		for(Element item : inputs)
		{
			if(item.attr("name").compareTo("user_id")==0){
				value = item.attr("value");
				break;
			}
		}
		
		return value;
		
	}

	
	/**
	 * 환경설정 페이지에서 토큰 값을 가져옴.
	 * @param _html
	 * @return
	 */
	public String getPrefenceToken(String _html) {

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		Elements forms = doc.select("form");
		
		Element aForm=null;
		for(Element s : forms)
		{
			if (s.attr("action").compareTo("account_prefs_update.php")==0)
			{
				aForm = s;
				break;
			}
		}
		
		if(aForm==null) return null;
		
		Elements inputs = aForm.select("input");
		String value=null;
		for(Element item : inputs)
		{
			if(item.attr("name").compareTo("account_prefs_update_token")==0){
				value = item.attr("value");
				break;
			}
		}
		
		return value;
	}
	

	/**
	 * 환경설정 변경결과가 잘 반영되었는지 확인.
	 * TODO : 현재는 검증안함.검증하도록 수정
	 * @param _html
	 * @return
	 */
	public boolean checkPreferenceHtml(String _html) {
		return true;
	}

	


	/**
	 * 로그인이 잘 되었는지 확인을 담당.
	 * @param _html
	 * @return
	 */
	public boolean checkLoginHtml(String _html) {
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		//hide클래스인 table을 검사
		Elements tds = doc.select("table.hide > tbody > tr > td.login-info-left");
		if(tds==null) return false;
		
		for(Element td : tds)
		{
			String str = td.text();
			if(str.toLowerCase().contains("logged in as")==true) return true;
		}
		
		return false;
	}

	
	/////////////////////////////////////////////////////////////////
	//  Preprocessing 관련 Parsing
	/////////////////////////////////////////////////////////////////

	/**
	 * Product정보들을 반환. (preWork에 해당하는 작업)
	 * @param _html
	 * @return
	 */
	public List<Project> analysisProduct(String _html) {
		
		List<Project> list = new ArrayList<Project>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		Elements selects = doc.select("form select");
		
		Element aSelect=null;
		for(Element s : selects)
		{
			if (s.attr("name").compareTo("project_id")==0)
			{
				aSelect = s;
				break;
			}
		}
		
		if(aSelect==null) return null;
		
		Elements options = aSelect.select("option");
		for(Element op : options)
		{
			Project item = new Project();
			String strID = op.attr("value");
			int idx =strID.indexOf(';');
			if (idx>0){
				item.ID = Integer.parseInt(strID.substring(idx+1,strID.length()));
				item.ParentID = Integer.parseInt(strID.substring(0,idx));
			}
			else
				item.ID = Integer.parseInt(strID);
			
			item.Name = op.text().trim();
			
			if(item.ID != 0) list.add(item);	//[Any], General All 등의 항목제외하기 위해.
		}
		
		return list;
	}
	
	
	/**
	 * Select태그에 대한 옵션들을 반환하는 함수 (preWork에 해당하는 작업)
	 * @param _html
	 * @return
	 */
	public List<PropertyItem> analysisSelectOptions(String _html, String _selectName) {
		
		List<PropertyItem> list = new ArrayList<PropertyItem>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		Elements selects = doc.select("form select");
		
		Element aSelect=null;
		for(Element s : selects)
		{
			if (s.attr("name").compareTo(_selectName)==0)
			{
				aSelect = s;
				break;
			}
		}
		
		if(aSelect==null) return null;
		
		Elements options = aSelect.select("option");
		for(Element op : options)
		{
			PropertyItem item = new PropertyItem();
			item.ID = Integer.parseInt(op.attr("value"));
			item.Name = op.text().trim();
			
			if(item.ID != 0) list.add(item);
		}
		
		return list;
	}
	
	



	/////////////////////////////////////////////////////////////////
	// 버그리포트 관련 Parsing
	/////////////////////////////////////////////////////////////////


	/**
	 * 입력받은 xml로부터 버그리포트 정보를 추출한다. 3
	 * (XML용, 일부 사이트만 지원해서 사용안함)
	 * @param _xml
	 * @return 버그리포트 리스트
	 */
	public List<BugReport> analysisReports(String _xml) {
		
		//ListReports.add(url);
		
		List<BugReport> list = new ArrayList<BugReport>();
		
		Elements es;
		Elements issues;
		BugReport report;
		Document doc = Jsoup.parse(_xml, "", Parser.xmlParser());
		
		
		issues = doc.select("mantis>issue");
		for(Element e : issues)
		{
			report = new BugReport();
			report.ID = Integer.parseInt(e.select("id").get(0).text());
			
			report.Project = new Project();
			report.Project.Name = e.select("project").get(0).text();
			report.Project.ID = Integer.parseInt(e.select("project").get(0).attr("id"));
			
			report.Category = new Category();
			report.Category.Name = e.select("category").get(0).text();
			report.Category.ID = Integer.parseInt(e.select("category").get(0).attr("id"));
			
						
			report.ViewState = Integer.parseInt(e.select("view_state").get(0).attr("id"));		//No match
			if(report.ViewState==10)  report.ViewState= 0;
			else 					report.ViewState= 1;
			
			report.DateSubmitted = DateUtil.getStringFromTimestamp(Long.parseLong(e.select("date_submitted").get(0).text()));
			report.LastUpdated = DateUtil.getStringFromTimestamp(Long.parseLong(e.select("last_updated").get(0).text()));
			
			es = e.select("reporter");
			if(es.size()>0){
				report.Reporter = new User();
				String strID = es.get(0).attr("id");
				if(strID!=""){
					report.Reporter.ID =  Integer.parseInt(strID);
				}
				else{
					report.Reporter.ID  = -1;//UndefinedUserID++;
				}
					
				report.Reporter.LoginName = es.get(0).text();
			}
			
			es = e.select("handeler");
			if(es.size()>0){
				report.Handler = new User();
				report.Handler.ID =  Integer.parseInt(es.get(0).attr("id"));
				report.Handler.LoginName = es.get(0).text();
			}
			
			es = e.select("priority");			if(es.size()>0){ report.Priority = es.get(0).text(); }
			es = e.select("severity");			if(es.size()>0){ report.Severity = es.get(0).text(); }
			es = e.select("reproducibility");	if(es.size()>0){ report.Reproducibility = es.get(0).text(); }
			
			es = e.select("status");			if(es.size()>0){ report.Status = es.get(0).text(); }
			es = e.select("resolution");		if(es.size()>0){ report.Resolution = es.get(0).text(); }
			es = e.select("platform");			if(es.size()>0){ report.Platform = es.get(0).text(); }
			es = e.select("os");				if(es.size()>0){ report.OS = es.get(0).text(); }
			es = e.select("os_build");			if(es.size()>0){ report.OS_Build = es.get(0).text(); }
			es = e.select("version");			if(es.size()>0){ report.Version = es.get(0).text(); }
			es = e.select("fixed_in_version");	if(es.size()>0){ report.FixedInVersion = es.get(0).text(); }
			es = e.select("target_version");	if(es.size()>0){ report.TargetVersion = es.get(0).text(); }
			//es = e.select("reproducibility");	if(es.size()>0){ report.Reproducibility = es.get(0).text(); }
			//es = e.select("reproducibility");	if(es.size()>0){ report.Reproducibility = es.get(0).text(); }
			
			
			//호환되지 않는 필드들.
			es = e.select("projection");		if(es.size()>0){ report.Projection = Integer.parseInt(es.get(0).attr("id")); }
			//es = e.select("eta");				if(es.size()>0){ report.ETA = es.get(0).text(); }
			es = e.select("due_date");			if(es.size()>0){ report.DueDate = es.get(0).text(); }
			es = e.select("profile_id");		if(es.size()>0){ report.ProfileID = Integer.parseInt(es.get(0).text()); }
			es = e.select("summary");			if(es.size()>0){ report.Summary = es.get(0).text(); }
			
			
			//Comment들 추가
			es = e.select("description");
			if(es.size()>0){
				report.Description = es.get(0).text();
				//report.Bugnotes.add(new Bugnote(1, 1, report.Reporter.ID, report.Reporter.LoginName, report.DateSubmitted,report.DateSubmitted, , 0));
			}
			es = e.select("steps_to_reproduce");
			if(es.size()>0){
				report.Steps = es.get(0).text();
				//report.Bugnotes.add(new Bugnote(2, 2, report.Reporter.ID, report.Reporter.LoginName, report.DateSubmitted,report.DateSubmitted, es.get(0).text(), 0));
			}
			es = e.select("summary");
			if(es.size()>0){
				report.Summary = es.get(0).text();
				//report.Bugnotes.add(new Bugnote(3, 3, report.Reporter.ID, report.Reporter.LoginName, report.DateSubmitted,report.DateSubmitted, es.get(0).text(), 0));
			} 
			es = e.select("additional_infomation");
			if(es.size()>0){
				report.Summary = es.get(0).text();
				//report.Bugnotes.add(new Bugnote(3, 3, report.Reporter.ID, report.Reporter.LoginName, report.DateSubmitted,report.DateSubmitted, es.get(0).text(), 0));
			} 
			
			//버그리포트 리스트에 추가.
			list.add(report);
		}

		return list;
	}


	/**
	 * 페이지에서 태그 문자열을 추출 (dailyWork에 해당하는 작업)
	 * @param _html
	 * @return
	 */
	public String analysisBugtagString(String _html) {
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements tables;
		tables = doc.select("body > table");
		Element Tags = null;
		
		//TODO :  > td.category  로 변경하면 빠름.
		for(Element table : tables)
		{
			Elements trs = table.select("tbody > tr");
			for(Element tr : trs)
			{
				Elements tds = tr.select("> td");
				if (tds.size()<=0) continue;
				if (tds.get(0).attr("class").compareTo("category")==0 &&
				    tds.get(0).text().toLowerCase().compareTo("tags")==0)
				{
					Tags = tds.get(1);
					break;
				}
			}
			if(Tags!=null) break;
		}
		
		if (Tags==null) return null;
		
		return Tags.text();
	}

	/**
	 * 페이지에서 태그정보를 추출 (dailyWork에 해당하는 작업)
	 * @param _html
	 * @return
	 *//*
	private List<PropertyItem> analysisBugtags(String _html) {
		
		List<PropertyItem> list = new ArrayList<PropertyItem>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements tables;
		tables = doc.select("body > table");
		Element Tags = null;
		
		for(Element table : tables)
		{
			Elements trs = table.select("> tr");
			for(Element tr : trs)
			{
				Elements tds = tr.select("> td");
				if (tds.size()<=0) continue;
				if (tds.get(0).attr("class").compareTo("category")==0 &&
				    tds.get(0).text().toLowerCase().compareTo("tags")==0)
				{
					Tags = tds.get(1);
					break;
				}
			}
			if(Tags!=null) break;
		}
		
		if (Tags==null) return list;
		
		
		//찾아진 Tags 카테고리 내에서 데이터 검출.
		Elements as = Tags.select("a");
		
		for(Element a: as)
		{
			String strID = a.attr("href");
			String name = a.text();
			strID = strID.substring(strID.indexOf("tag_id=")+7);
			if(strID.indexOf('&')>=0)
				strID = strID.substring(0, strID.indexOf('&'));
			int id = Integer.parseInt(strID);
			
			list.add(new PropertyItem(id, name));
		}
		
		return list;
	}

	*/
	/**
	 * 페이지에서 관계정보를 추출 (dailyWork에 해당하는 작업)
	 * @param _html
	 * @return
	 */
	public List<Relationship> analysisRelationships(int _bugID, String _html) {
		
		List<Relationship> list = new ArrayList<Relationship>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements trs;
		trs = doc.select("#relationships_open > table table tr");
		
		
		for(Element tr : trs)
		{
			Elements tds = tr.select("td");
			if(tds.size()!=5) continue;
			
			Relationship r = new Relationship();
			String strType = tds.get(0).text().trim();
			
			if(strType.contains("duplicate of")==true)
				r.Type = RelationshipType.DuplicateOf;
			
			else if(strType.contains("has duplicate")==true)
				r.Type = RelationshipType.HasDuplicate;
			
			else if(strType.contains("child of")==true)
				r.Type = RelationshipType.ChildOf;
			
			else if(strType.contains("parent of")==true)
				r.Type = RelationshipType.ParentOf;
			
			else if(strType.contains("related to")==true)
				r.Type = RelationshipType.RelatedTo;
			
			else
				r.Type = RelationshipType.None;
			
			r.SrcID = _bugID;
			r.DestID = Integer.parseInt(tds.get(1).text().trim());
				
			list.add(r);
		}
		
		return list;
	}




	/**
	 * 페이지에서 메모정보를 추출
	 * @param _html
	 * @return
	 */
	public List<Bugnote> analysisBugnotes(String _html) {

		List<Bugnote> list = new ArrayList<Bugnote>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements trs;
		trs = doc.select("#bugnotes_open > table tr.bugnote");
		
		
		//버그노트가 있는 행만 선택
		for(Element tr : trs)
		{
			//노트 ID얻기.
			int id = Integer.parseInt(tr.attr("id").substring(1));	//처음 "c"를 제외
			
			User user = new User();
			
			//등록자 정보 얻기.
			Element td1 = tr.select("td.bugnote-public").get(0);
			Elements as = td1.select("a");

			String strID="";
			for(Element a : as)
			{
				strID = a.attr("href");
				if(strID.matches(".+view_user_page\\.php\\?.*id=[0-9]+.*")==false) continue;
				
				//id 얻기.
				strID = strID.substring(strID.indexOf("id=")+3);
				if(strID.indexOf('&')>=0)
					strID = strID.substring(0, strID.indexOf('&'));
				
				user.ID = Integer.parseInt(strID);
				user.LoginName = a.text();
			}
			
			//ID에 대한 링크를 찾을 수 없는 경우.
			if (user.LoginName==null){
				Elements fonts = td1.select("font");
				if(fonts.size()!=0){
					user.ID = -1; //UndefinedUserID++;  DB에서 자동할당.
					user.LoginName = fonts.get(0).text();
				}
			}
			
			//등록일 추출.
			Elements spans = td1.select("span");
			String creationTime=null;
			String updateTime=null;
			String temp;
			for(Element span : spans)
			{
				temp = span.text();
				if(temp.matches(".*[0-9]+-[0-9a-zA-Z]+-[0-9]+ [0-9]+:[0-9]+")==true)
				{
					if(temp.startsWith("edited on")==true){
						updateTime = temp.substring(temp.indexOf(":")+1).trim();
						updateTime = DateUtil.getStandardFormat(updateTime, null);
					}
					else
						creationTime = DateUtil.getStandardFormat(temp, null);
				}
				
				if(temp.matches("\\(.+\\)")==true)
				{
					if(temp.matches("\\([0-9]+\\)")==true)	continue;		//ID항목임.
					else user.Role = temp.substring(1,temp.length()-1);
				}
			}
			if(updateTime==null) updateTime = creationTime;
			
			//노트 내용 추출.
			String comment = tr.select("td.bugnote-note-public").get(0).html();
			
			list.add(new Bugnote(id, 0, user, creationTime, updateTime, comment, 0));
		}
		return list;
		
	}



	/**
	 * 페이지에서 히스토리정보를 추출
	 * @param _html
	 * @return
	 */
	public List<BugHistory> analysisHistories(String _html) {

		List<BugHistory> list = new ArrayList<BugHistory>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements trs;
		trs = doc.select("#history_open > table tr.row-1, #history_open > table tr.row-2");
		
		for(Element tr : trs)
		{
			BugHistory history= new BugHistory();
			Elements tds = tr.select("td");
			if(tds.size()!=4) continue;
			
			
			history.When = DateUtil.getStandardFormat(tds.get(0).text(), null);				//when
			history.FieldID = tds.get(2).text();			//field
			
			//변경된 값 얻기.
			history.Added =  tds.get(3).text();
			history.Removed ="";
			
			if (history.Added.indexOf("=>")>=0)
			{
				history.Removed = history.Added.substring(0, history.Added.indexOf("=>")).trim();
				history.Added = history.Added.substring(history.Added.indexOf("=>")+2).trim();
			}

			//등록자 ID, Name
			Elements es = tds.get(1).select("a");
			String text = tds.get(1).text();
			if(es.size()>0)
			{
				Element a= es.get(0);
				String strID = a.attr("href");
				strID = strID.substring(strID.indexOf("id=")+3);
				if(strID.indexOf('&')>=0)
					strID = strID.substring(0, strID.indexOf('&'));
				history.Who.ID = Integer.parseInt(strID);
				history.Who.LoginName = a.text();
			}
			else if(text!=""){
				//중복된 ID 없이 어떻게 등록시키지??? ==> 90,000,000번대 사용자번호는 아이디가 없는 사용자.
				history.Who.ID = -1;//UndefinedUserID++;
				history.Who.LoginName = text;
			}
			else{
				history.Who.ID = 1;
				history.Who.LoginName = "Administrator";
			}
				
			
			
			list.add(history);
		}
		return list;
		
	}



	
	/**
	 * bug report페이지에서 속성들이 있는 테이블의 html 을 반환.
	 * @param _html
	 * @return
	 */
	public String getTableCode(String _html) {
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements tables = doc.select("body > table");
				
		Element selectedTable = null;
		boolean row1 = false;
		boolean row2 = false;
		
		for(Element table : tables)
		{
			Elements trs = table.select("tbody > tr");
			for(Element tr : trs)
			{
				String clsName = tr.attr("class");
				if (clsName==null) continue;
				
				if(clsName.toLowerCase().contains("row-1")==true)
					row1 = true;				
				
				if(clsName.toLowerCase().contains("row-2")==true)
					row2 = true;
				
				if (row1==true && row2==true){
					selectedTable = table;
					break;
				}
			}
			if(selectedTable !=null) break;
		}
		
		//선택된 테이블을 반환.
		if(selectedTable !=null) return selectedTable.outerHtml();
		return null;
	}
	
	
	
	/**
	 * 버그리포트 페이지를 분석해서 버그리포트 클래스를 생성하여 반환.
	 * @param _html
	 * @return
	 */
	public BugReport analysisBugreport(String _html)
	{
		String tableStr = getTableCode(_html);
		if(tableStr==null) return null;
		
		BugReport report = new BugReport();
		
		//단순속성 추출===========================================
		//기본 속성
		String str = getStringFromTag(getReportAttr(tableStr, "ID"));
		if(str!=null) report.ID = Integer.parseInt(str);
		
		str = getStringFromTag(getReportAttr(tableStr, "View Status"));
		if(str!=null && str.compareTo("public")==0) report.ViewState = 10;	//public 기본값.
		else										report.ViewState = 50;	//private 기본값.
		
		str = getStringFromTag(getReportAttr(tableStr,"Date Submitted"));
		if(str!=null) report.DateSubmitted = DateUtil.getStandardFormat(str,null);
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "Last Update"));
		if(str!=null) report.LastUpdated = DateUtil.getStandardFormat(str,null);
		else return null;
		

		str = getStringFromTag(getReportAttr(tableStr, "Priority"));
		if(str!=null) report.Priority = str;
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "Severity"));
		if(str!=null) report.Severity = str;
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "Status"));
		if(str!=null) report.Status = str;
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "Resolution"));
		if(str!=null) report.Resolution = str;
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "Reproducibility"));	//없는 사이트들도 있음
		if(str!=null) report.Reproducibility = str;
		
		
		//버전관련 정보 
		str = getStringFromTag(getReportAttr(tableStr, "Platform"));
		if(str!=null) report.Platform = str;
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "OS"));
		if(str!=null) report.OS = str;
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "OS Version"));
		if(str!=null) report.OS_Build = str;
		else return null;
		
		str = getStringFromTag(getReportAttr(tableStr, "Product Version"));
		if(str!=null) report.Version = str;	//없는 사이트들도 있음.
				
		str = getStringFromTag(getReportAttr(tableStr, "Product Build"));
		if(str!=null) report.Build = str;	//없는 사이트들도 있음.
		
		str = getStringFromTag(getReportAttr(tableStr, "Target Version"));
		if(str!=null) report.TargetVersion = str;//없는 사이트들도 있음.
		
		str = getStringFromTag(getReportAttr(tableStr, "Fixed in Version"));
		if(str!=null) report.FixedInVersion = str;//없는 사이트들도 있음.
		
		
		str = getStringFromTag(getReportAttr(tableStr, "Summary"));
		if(str!=null) report.Summary = str;
		else return null;

		str = getStringFromTag(getReportAttr(tableStr, "Tags"));
		if(str!=null) {
			if(str.contains("No tags attached")!=true)
				report.Tags = str;	
			else 
				report.Tags = "";
		}
		else return null;
		
		//상세 텍스트		
		str = getReportAttr(tableStr, "Description");
		if(str!=null) report.Description = str;
		else return null;
		
		str = getReportAttr(tableStr, "Steps To Reproduce");
		if(str!=null) report.Steps = str;
		
		str = getReportAttr(tableStr, "Additional Information");
		if(str!=null) report.AdditionalInfo = str;


		
		//복합속성 추출===========================================
		//사용자정보
		str = getStringFromTag(getReportAttr(tableStr, "Reporter"));
		if(str!=null) report.Reporter = new User(-1, str);
		
		str = getStringFromTag(getReportAttr(tableStr, "Assigned To"));
		if(str!=null && str.length()!=0) report.Handler =new User(-1, str);
		
			
		//프로젝트 정보
		str = getStringFromTag(getReportAttr(tableStr, "Project")); 	//없으면 category에 []사이에 있을 수도 잇음.
		if(str!=null) report.Project = new Project(str, -1);
		
		str = getStringFromTag(getReportAttr(tableStr, "Category"));
		if(str!=null) report.Category = new Category(str, -1);

		
		//첨부파일 리스트 분석.
		str = getReportAttr(tableStr, "Attached Files");
		report.Attachments = analysisAttachements(str);
		
		
		return report;
	}
	
	/**
	 * bugReport 페이지에서 첨부파일 정보를 추출하여 리스트를 반환
	 * @param _tableStr
	 * @return
	 */
	private List<Attachment> analysisAttachements(String _tableStr)
	{
		
		List<Attachment> attaches = new ArrayList<Attachment>();
		
		Document doc = Jsoup.parse(_tableStr, "", Parser.htmlParser());
				
		//찾아진 Attachement 태그 내에서 정보추출.
		Elements as = doc.select("a");
		
		for(Element a : as)
		{
			Attachment attach = new Attachment();
			
			attach.Filename = a.text();
			if(attach.Filename==null) continue;
			if(a.attributes().size()!=1) continue;
			if(attach.Filename.compareTo("")==0 || attach.Filename.compareTo("^")==0) continue;
			
			
			//ID추출
			String strID = a.attr("href");
			if(strID.matches("file_download\\.php\\?.*file_id.*")==false) continue;
			strID = strID.substring(strID.indexOf("file_id=")+8);
			if(strID.indexOf("&")>=0)
				strID = strID.substring(0, strID.indexOf('&'));
			attach.ID = Integer.parseInt(strID);
			
			attaches.add(attach);
		}
		
		//생성일 정보추출.
		Elements spans = doc.select("span");
		int idx=0;
		for(Element span: spans)
		{
			String str = span.text();
			
			if(str.matches("[0-9]+-[0-9a-zA-Z]+-[0-9]+ [0-9]+:[0-9]+")==true)
			{
				attaches.get(idx).CreationTime = DateUtil.getStandardFormat(str, null);
				attaches.get(idx).ModificationTime = attaches.get(idx).CreationTime;
				idx++;				
			}
		}
		
		return attaches;
	}
	
	
	/**
	 * 태그로 쌓여있는 데이터에서 모든 텍스트 만을 추출.
	 * @param _tags
	 * @return
	 */
	private String getStringFromTag(String _tags)
	{
		if(_tags==null) return null;
		Document doc = Jsoup.parse(_tags, "", Parser.htmlParser());
		String text = doc.text();
		
		if(text!=null)
			text = text.trim();		
		return text;
	}
	
	/**
	 * _html문서로부터 특정 속성의 값을 찾음.
	 * @param _table (table태그가 최상위 이어야함)
	 * @return
	 */
	private String getReportAttr(String _table, String _findAttr) {

		Document doc = Jsoup.parse(_table, "", Parser.htmlParser());
		Elements trs = doc.select("tbody > tr");
		_findAttr = _findAttr.toLowerCase();

		boolean rowCategory = false;
		boolean found = false;
		int row = 0;
		int col = 0;
		
		//속성값을 찾기위한 테이블 탐색
		for(row = 0; row <trs.size(); row++)
		{
			Element tr = trs.get(row);
			
			//
			String clsName = tr.attr("class");
			if(clsName==null) continue;
			if(clsName.toLowerCase().contains("spacer")==true) continue;
			
			//한 행이 모두 카테고리인지 구분.
			rowCategory = checkRowCategory(tr);
			

			//찾고자하는 속성값을 선택.
			Elements tds = tr.select("td");
			for(col=0; col<tds.size(); col++)
			{
				Element td = tds.get(col);
				
				//카테고리 확인
				String type = td.attr("class");
				if(type==null) continue;	//속성명이 없으면 통과
				if(type.toLowerCase().contains("category")==false) continue;
				String attr = td.text();			//버그리포트 속성 값 얻기.
				if(attr==null) continue;
				
				//속성을 찾은 경우.
				if(attr.toLowerCase().compareTo(_findAttr)==0)
				{
					if(rowCategory==true) 	row++;
					else 					col++;
					found = true;
				}
				if (found==true) break;	//찾은경우 루프탈출
			}
			
			if (found==true) break;	//찾은경우 루프탈출
		}
		
		
		if (found==false) return null;
		
		//최종적으로 찾아진 값을 반환.
		Element tr = trs.get(row);
		Elements tds = tr.select("td");
		Element td= tds.get(col);
		
		return td.html();
	}
		
	/**
	 * 테이블의 한 행이 모두 카테고리인지 확인.
	 * getReportAttr 의 하위함수
	 * @param _tr
	 * @return
	 */
	private boolean checkRowCategory(Element _tr)
	{
		boolean ret=false;
		int count=0;
		Elements tds = _tr.select("td");
		for(Element td : tds)
		{
			String type = td.attr("class");
			if (type==null) continue;
			if(type.toLowerCase().contains("category")==true) count++;
		}
		if(count==tds.size())  ret=true;
		
		return ret;
	}



	/**
	 * 임시 테스트 함수.  버그 모니터값이 있는지 확인해보는 함수.
	 * @param _html
	 * @return
	 */
	public boolean findMonitor(String _html) {
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		String str = doc.text();
		
		if(str.toLowerCase().indexOf("monitor")>0) return true;
		return false;
	}



	/**
	 * 버그리스트 페이지에서 버그아이디 목록을 추출.
	 * @param _html
	 * @return
	 */
	public List<Integer> getBuglist(String _html) {

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements tables = doc.select("body >form > table");
				
		//대상 테이블 선택
		Element selectedTable = null;
		for(Element table : tables)
		{
			Elements trs = table.select("tbody > tr");
			if(trs.size()<2) continue;
			
			if(trs.get(0).text().contains("Viewing Issues")==true && trs.get(1).attr("class").contains("row-category")==true)
			{
				selectedTable = table;
				break;
			}
		}
		if(selectedTable==null) return null;
		
		
		//테이블에서 ID위치 확인.
		Elements trs = selectedTable.select("tbody > tr");
		Elements titles = trs.get(1).select("td");
		int col = -1;
		for(col = 0; col < titles.size(); col++)
		{
			String text = titles.get(col).text();
			if (text==null) continue;
			if (text.trim().compareTo("ID")==0)
				break;
		}
		if(col>=titles.size()) return null;
		
		
		//테이블에서 ID추출.
		List<Integer> list = new ArrayList<Integer>();
		for(int row=2; row < trs.size(); row++)
		{
			Element tr = trs.get(row);
			Elements tds = tr.select("td");
			if(tds.size()<col) continue;
			
			String strID = tds.get(col).text();
			if(strID==null) continue;
			list.add(Integer.parseInt(strID));		//리스트에 추가.
		}
		
		Collections.sort(list);
		
		return list;
	}

	


	/**
	 * 버그리포트의 첨부파일 정보 추출 (dailyWork에 해당하는 내용)
	 * @param _html
	 * @return
	 */
	public List<Attachment> analysisAttachements_old(String _html) {

		List<Attachment> list = new ArrayList<Attachment>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements tables;
		tables = doc.select("body > table");
		Element AttachTag = null;
		
		for(Element table : tables)
		{
			Elements trs = table.select("tbody > tr");
			for(Element tr : trs)
			{
				Elements tds = tr.select("> td");
				if (tds.size()<=0) continue;
				if (tds.get(0).attr("class").compareTo("category")==0 &&
					    tds.get(0).text().toLowerCase().compareTo("attached files")==0)
				{
					AttachTag = tds.get(1);
					break;
				}
			}
			if(AttachTag!=null) break;
		}
		if (AttachTag==null) return null;
		
		
		//찾아진 Attachement 태그 내에서 정보추출.
		Elements as = AttachTag.select("a");
		
		for(Element a : as)
		{
			Attachment attach = new Attachment();
			
			attach.Filename = a.text();
			if(attach.Filename==null) continue;
			if(a.attributes().size()!=1) continue;
			if(attach.Filename.compareTo("")==0 || attach.Filename.compareTo("^")==0) continue;
			
			
			//ID추출
			String strID = a.attr("href");
			if(strID.matches("file_download\\.php\\?.*file_id.*")==false) continue;
			strID = strID.substring(strID.indexOf("file_id=")+8);
			if(strID.indexOf("&")>=0)
				strID = strID.substring(0, strID.indexOf('&'));
			attach.ID = Integer.parseInt(strID);
			
			list.add(attach);
		}
		
		//생성일 정보추출.
		Elements spans = AttachTag.select("span");
		int idx=0;
		for(Element span: spans)
		{
			String str = span.text();
			
			if(str.matches("[0-9]+-[0-9a-zA-Z]+-[0-9]+ [0-9]+:[0-9]+")==true)
			{
				list.get(idx).CreationTime = DateUtil.getStandardFormat("str", null);
				list.get(idx).ModificationTime = list.get(idx).CreationTime;
				idx++;				
			}
		}
		
		return list;
		
//		//찾아진 Attachement 태그 내에서 정보추출.
//		Elements as = AttachTag.select("a");
//		
//		int loc=0;
//		for(Element a: as)
//		{
//			String strID = a.attr("href");
//			strID = strID.substring(strID.indexOf("file_id=")+8);
//			if(strID.indexOf("&")>=0)
//				strID = strID.substring(0, strID.indexOf('&'));
//			int id = Integer.parseInt(strID);
//			int idx;
//			for(idx=0; idx<list.size(); idx++)
//			{
//				if (list.get(idx).ID==id) break;
//			}
//			
//			//기존에 등록된 ID가 없으면 loc를 초기화. 아니면 ++
//			if(idx!=list.size()) loc++;
//			else loc = 1;
//			
//			//loc가 2인경우!  filename;
//			if(loc==2) list.get(idx).Filename = a.text();
//			
//			list.add(new Attachment(id));
//		}
		
	}


	/**
	 * 입력받은 데이터가 버그리포트가 없다는 에러메세지 인지 확인.
	 * @param _html
	 * @return  null이면 에러가 아님.
	 */
	public MantisException checkReportError(String _html) {
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements tables = doc.select("table");
					
		if(tables==null) return null;
		

		//에러 테이블 찾기.
		Element theTable = null;
		for(Element table : tables)
		{
			String str = table.text().trim();
			if(str.startsWith("APPLICATION ERROR #")==true){
				theTable = table;
				break;
			}
		}
		if(theTable==null) return null;
		
		
		Elements trs = theTable.select("tr");
		
		if (trs==null) return null;				//알수없는 에러
		if (trs.size()!=3) return null;			//알수없는 에러
		
		
		
		//테이블의 정보
		String ErrorCode = trs.get(0).text();		//첫번째 행
		String ErrorMsg = trs.get(1).text();		//두번째 행
		int Code = Integer.parseInt(ErrorCode.substring(ErrorCode.indexOf("#")+1));	//코드값 추출
		
		MantisException exception = new MantisException(Code,ErrorMsg);
		return exception;			
	}
	
	/**
	 * 입력받은 데이터가 접근허용되지 않은 리포트인지 확인.
	 * @param _html
	 * @return
	 */
	public boolean checkPrivateReport(String _html) {
		if(_html==null) return false;
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements ps = doc.select("body > center > p");
		
		if (ps==null) return false;
		if (ps.size()<=0) return false;
		
		String Msg = ps.get(0).text();
		
		return Msg.contains("Access Denied");
	}


	public List<BugChangeset> analysisChangesets(String _html) {

		List<BugChangeset> list = new ArrayList<BugChangeset>();
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		Elements trs;
		trs = doc.select("#Source_open > table tr.row-1, #Source_open > table tr.row-2");
		if (trs==null) return null;
		
		for(Element tr : trs)
		{
			BugChangeset changeset= new BugChangeset();
			Elements tds = tr.select("td");
			if(tds.size()!=4) continue;
						
			
			list.add(changeset);
		}
		return list;
	}
	
}