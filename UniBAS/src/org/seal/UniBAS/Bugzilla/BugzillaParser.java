package org.seal.UniBAS.Bugzilla;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.seal.UniBAS.Bugzilla.Model.Attachment;
import org.seal.UniBAS.Bugzilla.Model.BugHistory;
import org.seal.UniBAS.Bugzilla.Model.BugReport;
import org.seal.UniBAS.Bugzilla.Model.Bugnote;
import org.seal.UniBAS.Bugzilla.Model.Product;
import org.seal.UniBAS.Bugzilla.Model.PropertyItem;
import org.seal.UniBAS.Bugzilla.Model.Relationship;
import org.seal.UniBAS.Bugzilla.Model.RelationshipType;
import org.seal.UniBAS.Bugzilla.Model.User;
import org.seal.UniBAS.Bugzilla.Model.Vote;
import org.seal.UniBAS.Util.DateUtil;
import org.seal.UniBAS.Util.log;


public class BugzillaParser 
{
	
	public String RegexEmail = "^.*@([0-9a-zA-Z_-]+)(\\.[0-9a-zA-Z_-]+)+";//"^([0-9a-zA-Z_.+\\-]+)@([0-9a-zA-Z_-]+)(\\.[.0-9a-zA-Z_-]+)"; 
	public BugzillaParser()
	{
		
	}
	/////////////////////////////////////////////////////////////////
	//  Parsing
	/////////////////////////////////////////////////////////////////
	/**
	 * 파싱을 통하여 리포트 리스트를 추가.
	 * @param _xml
	 * @return
	 */
	public List<String> analysisBugList(String _xml)
	{
		// Using Jsup to parsing xml.
		
		List<String> list = new ArrayList<String>();
		
	    Document doc = Jsoup.parse(_xml, "", Parser.xmlParser());
	    for (Element e : doc.select("entry id")) {
	    	list.add(e.text());
	    	//list.add(e.text().trim()+"&ctype=xml");
	    }
	    return list;
	}
	
	
	public String getPreferenceToken(String _html) {

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		if(doc==null) return null;

		//목표 form선택
		Elements forms = doc.select("form");
		Element aForm=null;
		for(Element s : forms)
		{
			if (s.attr("action").compareTo("userprefs.cgi")==0)
			{
				aForm = s;
				break;
			}
		}
		if(aForm==null) return null;

		//token값 획득.
		Elements inputs = aForm.select("input");
		String value=null;
		for(Element item : inputs)
		{
			if(item.attr("name").compareTo("token")==0){
				value = item.attr("value");
				break;
			}
		}
		
		return value;
	}
	
	
	public Map<String,String> getPreferenceParams(String _html) {
		Map<String,String> map = new HashMap<String,String>();

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		if(doc==null) return null;

		//목표 form선택
		Elements forms = doc.select("form");
		Element aForm=null;
		for(Element s : forms)
		{
			if (s.attr("action").compareTo("userprefs.cgi")==0)
			{
				aForm = s;
				break;
			}
		}
		if(aForm==null) return null;

		//token값 획득.
		Elements inputs = aForm.select("input");
		//String value=null;
		for(Element item : inputs)
		{
			String name = item.attr("name");
			String value = item.attr("value");
			
			if(name==null || name=="") continue;
			map.put(name, value);
			
//			if(item.attr("name").compareTo("token")==0){
//				value = item.attr("value");
//				break;
//			}
		}
		
		Elements selects = aForm.select("select");
		
		for(Element select : selects)
		{
			Elements options  = select.select("option");
			String name = select.attr("name");
			
			//timezone에 대한 변경처리.
			if(name.compareTo("timezone")==0){
				map.put(name, "UTC");
				continue;
			}
		
			//기타 옵션에 대한 선택.
			for(Element op : options)
			{
				if (op.attr("selected").compareTo("selected")==0)
					map.put(name, op.attr("value"));
			}
		}
		return map;
	}
	
	public BugReport analysisReport(String _xml, String _user)
	{		
		Elements e;
		BugReport report;
		Document doc = Jsoup.parse(_xml, "", Parser.xmlParser());
		
		//버그질라 정보확인
		Elements root = doc.select("bugzilla");
		if(root.size()!=1) return null;
		
		//로그인계정확인.
		String loginname = root.get(0).attr("exporter");
		if (loginname==null || loginname.compareTo(_user)!=0) return null;
		
		//정보추출.
		if (doc.select("bug").size()==0) return null;
		
		report = new BugReport();
		report.BugID = Integer.parseInt(doc.select("bug > bug_id").get(0).text());
		
		report.Status = doc.select("bug > bug_status").get(0).text();
		report.Resolution = doc.select("bug > resolution").get(0).text();
		
		report.Classification = new PropertyItem();
		report.Classification.ID = Integer.parseInt(doc.select("bug > classification_id").get(0).text());
		report.Classification.Name = doc.select("bug > classification").get(0).text();
		
		report.Product = new Product();
		report.Product.Name = doc.select("bug > product").get(0).text();
		
		report.Component = new PropertyItem();
		report.Component.Name = doc.select("bug > component").get(0).text();
		//report.Component.Product = report.Product;
		
		report.Version = doc.select("bug > version").get(0).text();
		report.Priority = doc.select("bug > priority").get(0).text();
		report.Severity = doc.select("bug > bug_severity").get(0).text();
		report.Milestone = doc.select("bug > target_milestone").get(0).text();
		report.Platform = doc.select("bug > rep_platform").get(0).text();
		report.OS = doc.select("bug > op_sys").get(0).text();
		
		report.Summary = doc.select("bug > short_desc").get(0).text();
		report.StatusWhiteboard = doc.select("bug > status_whiteboard").get(0).text();
		report.Tags = doc.select("bug > keywords").get(0).text();		
		report.Votes = Integer.parseInt(doc.select("bug > votes").get(0).text());
		
		report.UpdatedTime = DateUtil.getUTCString(doc.select("bug > delta_ts").get(0).text(), false);
		report.CreationTime = DateUtil.getUTCString(doc.select("bug > creation_ts").get(0).text(), false);
		
		report.BugFileLoc = doc.select("bug > bug_file_loc").get(0).text();
		report.Everconfirmed = Integer.parseInt(doc.select("bug > everconfirmed").get(0).text());
		report.ReporterAccessible = Integer.parseInt(doc.select("bug > reporter_accessible").get(0).text());
		report.CClistAccessible = Integer.parseInt(doc.select("bug > cclist_accessible").get(0).text());
		
		
		//optional값들 처리.		
		e = doc.select("bug > lastdiffed");
		if(e.size()!=0){
			report.Lastdiffed = DateUtil.getUTCString(e.get(0).text(), false);
		}
		
		e = doc.select("bug > deadline");
		if(e.size()!=0){
			report.Deadline = e.get(0).text();
		}

		e = doc.select("bug > remaining_time");
		if(e.size()!=0){
			report.RemainingTime = Double.parseDouble(e.get(0).text());
		}
		
		e = doc.select("bug > estimated_time");
		if(e.size()!=0){
			report.EstimatedTime = Double.parseDouble(e.get(0).text());
		}
		
		e = doc.select("bug > actual_time");
		if(e.size()!=0){
			report.ActualTime = Double.parseDouble(e.get(0).text());
		}
		
		
		
		 
		//속성이 있는 값들 처리 User ID들.
		e = doc.select("bug > reporter");
		if(e.size()!=0){
			report.Reporter = new User();
			report.Reporter.ID = -1;
			report.Reporter.LoginName = e.get(0).text();
			report.Reporter.RealName = e.get(0).attr("name");
			report.Reporter.Timezone = DateUtil.getTimezoneString(doc.select("bug > creation_ts").get(0).text(), false);
		}
		
		e = doc.select("bug > assigned_to");
		if(e.size()!=0){
			report.Assignee = new User();
			report.Assignee.ID = -1;
			report.Assignee.LoginName = e.get(0).text();
			report.Assignee.RealName = e.get(0).attr("name"); 
		}
		
		e = doc.select("bug > qa_contact");
		if(e.size()!=0){
			report.QA = new User();
			report.QA.ID = -1;
			report.QA.LoginName = e.get(0).text();
			report.QA.RealName = e.get(0).attr("name");
		}
		
		
		//CC는 리스트 처리
		for(Element item : doc.select("bug>cc")){
			User user = new User(-1, item.text());
			report.CCList.add(user);
		}
			
		
		//SeeAlso는 리스트 처리
		for(Element item : doc.select("bug>see_also")){
			report.SeeAlsos.add(item.text());
		}
		
	
		//comments
		for(Element item : doc.select("bug>long_desc")){
			Bugnote note = new Bugnote();
			note.ID = Integer.parseInt(item.select("commentid").get(0).text());
			note.Submitter = new User(-1, item.select("who").get(0).text(), item.select("who").get(0).attr("name"));
			note.CreationTime = DateUtil.getUTCString(item.select("bug_when").get(0).text(), false);
			note.TheText = item.select("thetext").get(0).text();
			note.isPrivate = Integer.parseInt(item.attr("isprivate"));

			e =item.select("attachid"); 
			if(e.size()!=0)
				note.AttachID = Integer.parseInt(e.get(0).text());
			else
				note.AttachID = -1;
			report.Bugnotes.add(note);
		} 
	    
		//Duplication
		for(Element item : doc.select("bug>dup_id")){
			report.Relationships.add(new Relationship(report.BugID, Integer.parseInt(item.text()), RelationshipType.DuplicateOf));
			//report.Duplications.add();
		}
		
		//Dependencies
		for(Element item : doc.select("bug>dependson")){
			report.Relationships.add(new Relationship(report.BugID, Integer.parseInt(item.text()), RelationshipType.ChildOf));
			//report.Dependencies.add(Integer.parseInt(item.text()));
		} 
		
		//Blocks
		for(Element item : doc.select("bug>blocked")){
			report.Relationships.add(new Relationship(report.BugID, Integer.parseInt(item.text()), RelationshipType.ParentOf));
			//report.Blocks.add(Integer.parseInt(item.text()));
		} 
		
		//Attachment는 리스트 처리
		for(Element item : doc.select("attachment")){
			Attachment attach = new Attachment();
			attach.ID = Integer.parseInt(item.select("attachid").get(0).text());
			attach.CreationTime = DateUtil.getUTCString(item.select("date").get(0).text(), false);
			attach.Desc = item.select("desc").get(0).text();
			attach.Filename = item.select("filename").get(0).text();
			attach.isObsolete = Integer.parseInt(item.attr("isobsolete"));
			attach.isPatch = Integer.parseInt(item.attr("ispatch"));
			attach.isPrivate = Integer.parseInt(item.attr("isprivate"));
			attach.isUrl = 0;
			attach.MimeType = item.select("type").get(0).text();
			attach.ModificationTime = DateUtil.getUTCString(item.select("delta_ts").get(0).text(), false);
			attach.Attacher.LoginName = item.select("attacher").get(0).text();
			attach.MimeType = item.select("type").get(0).text();
			
			e = item.select("data");
			attach.Data = e.get(0).text();
			attach.FileSize = Integer.parseInt(item.select("size").get(0).text());
					
			report.Attachments.add(attach);
		}
	    return report;
	}


	/**
	 * 웹문서에서 버그리포트 히스토리 정보를 추출
	 * @param _html
	 * @return
	 */
	public List<BugHistory> analysisHistory(String _html)
	{
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		if (doc.select("#bugzilla-body").size()==0) return null;
		
		
		//History가 있는지 없는지 검사.
		Elements ps = doc.select("#bugzilla-body > p");
		if(ps.size()<2) return null;
		if(ps.get(1).text().trim().startsWith("No changes")==true)
			return new ArrayList<BugHistory>();
		
		
		//테이블 선택
		Elements tables = doc.select("#bugzilla-body > table");
		if(tables.size()==0) return null;
		
		int idx=0;
		boolean flag = false;
		for(idx =0; idx<tables.size(); idx++)
		{
			Elements ths = tables.get(idx).select("tbody>tr>th");
			if(ths.size()!=5) continue;
			
			if(ths.get(0).text().toLowerCase().compareTo("who")==0
			   && ths.get(1).text().toLowerCase().compareTo("when")==0 
			   && ths.get(2).text().toLowerCase().compareTo("what")==0 
			   && ths.get(3).text().toLowerCase().compareTo("removed")==0 
			   &&ths.get(4).text().toLowerCase().compareTo("added")==0)
				
			{
				flag = true;
				break;
			}
			
		}
		
		if (flag==false) return null;
		
		//데이터 추출.
		List<BugHistory> activityList = new ArrayList<BugHistory>();
		
		
		String who = "";
		String when = "";
		String what = "";
		String removed = "";
		String added = "";
		int attahcID = -1;
		Element table = tables.get(idx);
		for(Element e : table.select("tr")){
			Elements tds = e.select("td");
			Element eWhat = null;
			
			if(tds.size()!=3 && tds.size()!=5) continue;
			if(tds.size()==3){
				eWhat = tds.get(0);
				what = eWhat.text();
				removed = tds.get(1).text();
				added = tds.get(2).text();
			}
			
			if(tds.size()==5){
				who = tds.get(0).text();
				when = tds.get(1).text();
				eWhat = tds.get(2);
				what = eWhat.text();
				removed = tds.get(3).text();
				added = tds.get(4).text();
			}
			//첨부파일에 대한 처리  (Attachment에 링크를 걸지 않을 수도 있음. 이에 대한 처리 수정 [2014-03-21])
			if(what.startsWith("Attachment")==true)
			{
				try{
					Elements es = eWhat.select("a");
					if (es.size() <=0) throw new Exception();
					
					String tmp = es.get(0).text();
					int sharp = tmp.indexOf('#');
					if( sharp <0) throw new Exception();
					
					attahcID = Integer.parseInt(tmp.substring(sharp+1).trim());
				}
				catch(Exception exception)
				{
					attahcID = -1;
				}
			}
			else 
				attahcID = -1;
			
			if(who.matches(RegexEmail)==false)	//이메일 검사
			{
				return null;
			}
			
			activityList.add(new BugHistory(who,DateUtil.getUTCString(when, true),what,removed,added, attahcID));
		}

		
		return activityList;
	}


	
	/**
	 * 파싱을 통하여 html문서에서 키워드를 분석.
	 * @param _html : html태그를 가진 문서 스트링.
	 * @return 키워드 리스트
	 */
	public List<PropertyItem> analysisTags(String _html) {

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		if (doc.select("#bugzilla-body").size()==0) return null;
		
		
		List<PropertyItem> tagList = new ArrayList<PropertyItem>();
		
		//해당 테이블에 대해서 키워드 등록. (because HTML)
		Elements tables = doc.select("#bugzilla-body table");

		for(Element table : tables)//idx =0; idx<tables.size(); idx++)
		{
			//행확인
			Elements trs = table.select("tbody>tr");
			if(trs.size()<=1) continue;
			
			//열확인
			Elements ths = trs.get(0).select("th");
			if(ths.size()!=4) continue;
			if(!(ths.get(0).text().toLowerCase().compareTo("name")==0
			   && ths.get(1).text().toLowerCase().compareTo("description")==0 
			   && ths.get(2).text().toLowerCase().compareTo("open bugs")==0 
			   && ths.get(3).text().toLowerCase().compareTo("total bugs")==0))
				continue;

			//데이터 추출.
			String name = "";
			String desc = "";

			for(int i=1; i<trs.size(); i++)
			{
				Elements list = trs.get(i).select("th,td");
				
				if(list==null || list.size()!=4) continue;
				name = list.get(0).text();
				desc = list.get(1).text();
				
				if (name=="") continue;
				
				tagList.add(new PropertyItem(-1, name, desc));
			}
		}//for
		
		
		return tagList;
	}


	public List<PropertyItem> analysisComponent(String _html) {

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		if (doc.select("#bugzilla-body").size()==0) return null;
		
		
		//컴포넌트 이름 찾기 (because HTML)
		List<PropertyItem> componentList = new ArrayList<PropertyItem>();
		Elements as = doc.select("#bugzilla-body a");
		
		for(Element a : as)
		{
			String url = a.attr("href");
			if (url.indexOf("product=")==-1 || url.indexOf("component=")==-1) continue;
			
			int idx = url.indexOf("component=") + 10;
			String name = url.substring(idx);
			name = name.substring(0,name.indexOf('&'));
			String componentName;
			//인코딩 문제
			try {
				componentName = URLDecoder.decode(name,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				componentName = name;
				log.printStackTrace(e);
				
			}
			
			
			//중복검사
			boolean flag=true;
			for(PropertyItem c: componentList)
			{
				if(c.Name.compareTo(componentName)==0) flag = false;
			}
			
			//등록
			if(flag==true) componentList.add(new PropertyItem(-1, componentName, null));
			
		}
		
		return componentList;
	}


	public List<Product> analysisProduct(String _html) {
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		if (doc.select("#bugzilla-body").size()==0) return null;
		
		
		//컴포넌트 이름 찾기 (because HTML)
		List<Product> productList = new ArrayList<Product>();
		Elements as = doc.select("#bugzilla-body a");

		
		for(Element a : as)
		{
			String url = a.attr("href");
			if (url.indexOf("product=")==-1) continue;
			
			int idx = url.indexOf("product=") + 8;
			String name = url.substring(idx);
			idx = name.indexOf('&');
			if(idx > 0)
				name = name.substring(0, idx);
			
			Product item = new Product();
			//인코딩 문제
			try {
				item.Name = URLDecoder.decode(name,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				item.Name = name;
				log.printStackTrace(e);
			}
			
			
			//중복검사
			boolean flag=true;
			for(Product p: productList)
			{
				if(p.Name.compareTo(item.Name)==0) flag = false;
			}
			
			//등록
			if(flag==true)	productList.add(item);
			
		}
		
		return productList;
	}


	public List<Vote> analysisVotes(String _html) {

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		if (doc.select("#bugzilla-body").size()==0) return null;
		
		
		//테이블이 맞는지 검증. (because HTML)
		Elements tables = doc.select("#bugzilla-body table");
		int idx=0;
		boolean flag = false;
		for(idx =0; idx<tables.size(); idx++)
		{
			Elements ths = tables.get(idx).select("tbody>tr>th");
			if(ths.size()!=2) continue;
			
			if(ths.get(0).text().toLowerCase().compareTo("who")==0
			   && ths.get(1).text().toLowerCase().compareTo("number of votes")==0)
			{
				flag = true;
				break;
			}
		
		}
		if (flag==false) return null;
		
		
		
		//데이터 추출.
		List<Vote> voteList = new ArrayList<Vote>();
		
		String username = "";
		int count = 0;
		
		Element table = tables.get(idx);
		for(Element e : table.select("tr")){
			Elements tds = e.select("td");
			
			if(tds.size()!=2) continue;
			
			username = tds.get(0).text();
			count = Integer.parseInt(tds.get(1).text());
			
			//로그인 된 상태인지 검증
			if(username.matches(RegexEmail)==false)	//이메일 검사
			{
				return null;
			}
						
			voteList.add(new Vote(username, count));
		}
		
		return voteList;
	}
	
	/**
	 * 버그리포트 문서로부터 날짜를 구함.
	 * @param _xml	분석할 문서 스트링
	 * @return 없으면 true, 있으면 false
	 */
	public String getCreationTimefromReport(String _xml)
	{
		String CreationTime;
		try{
			Document doc = Jsoup.parse(_xml, "", Parser.xmlParser());
			if (doc==null) return null;

			CreationTime = DateUtil.getUTCString(doc.select("bug > creation_ts").get(0).text(), false);
		}
		catch(Exception e)
		{
			return null;
		}
		return CreationTime;
	}
	
	
	public List<Integer> analysisDateList(String _html, String _startDate, String _endDate) {
		
		List<Integer> list = new ArrayList<Integer>();
		
		try{
			Document doc = Jsoup.parse(_html, "", Parser.xmlParser());
			if (doc==null) return null;
			
			Elements tables = doc.select("#bugzilla-body table");
			if(tables.size()==0) return null;
			
			
			//테이블 선택
			Elements trs = null;
			for(int i=0; i<tables.size(); i++)
			{
				trs = tables.get(i).select("tr");
				if(trs.size()<=1) continue;
				
				if( trs.get(0).text().toLowerCase().compareTo("id opened")==0 )
					break;
				else trs = null;
			}
			if (trs==null) return null;
			
			
			//실제 데이터 가져오기
			for(int i=1; i<trs.size(); i++)
			{
				Elements tds = trs.get(i).select("td");
				String value = tds.get(1).text();
				if(DateUtil.compareOverDate(value,_startDate, _endDate)==true) continue;
				
				list.add(Integer.parseInt(tds.get(0).text()));					
			}
		}
		catch(Exception e)
		{
			return null;
		}
		
		return list;
	}
	
	
	public boolean verifyResult(String _html) {
		boolean ret = false;
		try{
			if(_html==null) return false;
			
			//TextUtil.writeTextFile("C:\\_temp\\BTS\\preference_result.html", _html,null);
			
			Document doc = Jsoup.parse(_html, "", Parser.xmlParser());
			if (doc==null) return false;
			
			Elements divs = doc.select("#bugzilla-body #message");
			if(divs.size()==0) return false;
			
			
			//테이블 선택
			String message = divs.get(0).text();

			
			if(message.contains("saved")==true)
				return ret = true;
		}
		catch(Exception e)
		{
			ret = false;
		}
		
		return ret;
	}
	
	public Map<String, String> getLoginParams(String _html, String _userID, String _userPW) 
	{
		Map<String,String> map = new HashMap<String,String>();

		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		if(doc==null) return null;

		//목표 form선택
		Elements forms = doc.select("form");
		Element loginForm=null;
		Element tokenForm=null;
		for(Element s : forms)
		{
			if (s.attr("action").contains("index.cgi")==true)
			{
				loginForm = s;
			}
			if (s.attr("action").contains("token.cgi")==true)
			{
				tokenForm = s;
			}
		}
		if(loginForm==null) return null;

		//token값 획득.
		Elements inputs = loginForm.select("input");
		String name = null;
		String value=null;
		for(Element item : inputs)
		{
			name = item.attr("name");
			value = item.attr("value");
			
			if(name==null || name=="") continue;
			if(name.compareTo("Bugzilla_restrictlogin")==0) continue;
			map.put(name, value);
		}
		
		//token값 획득.
		name = "Bugzilla_login_token";
		
		if(map.get(name)!=null && map.get(name).length()==0 && tokenForm!=null)
		{
			inputs = tokenForm.select("input");
			for(Element item : inputs)
			{
				if(item.attr("name").compareTo("token")==0){
					value = item.attr("value");
					map.put(name, value);
					break;
				}
			}
			
		}
		
		printMap(map);
		map.put("Bugzilla_login",_userID);
		map.put("Bugzilla_password",_userPW);
		//map.put("Bugzilla_restrictlogin",_userID);
		//map.put("GoAheadAndLogIn","1");
		//map.put("Bugzilla_remember","checked");
		printMap(map);
		
		
		return map;
	}
	
	public void printMap(Map<String, String> _map)
	{
		System.out.println("-------TEST------------------------");
		Iterator<String> keys = _map.keySet().iterator();
        while(keys.hasNext()){
            String key = keys.next();
            System.out.println(key + " = " + _map.get(key));
        }
	}
	

	
	public boolean verifyLogin(String _html) {
		boolean ret = false;
		try{
			if(_html==null) return false;
			
			Document doc = Jsoup.parse(_html, "", Parser.xmlParser());
			if (doc==null) return false;
			
			Elements as = doc.select("body a");
			if(as.size()==0) return false;
			
			for(Element a :as)
			{
				if(a.text().contains("out")==true && a.attr("href").contains("?logout=1")==true)
				{
					ret = true;
					break;
				}
			}
		}
		catch(Exception e)
		{
			ret = false;
		}
		
		return ret;
	}
	
	
	/**
	 * 다운받은 문서가 login되었는지 검증
	 * @param _html
	 * @return
	 */
	public boolean checkLoginHtml(String _html) {

		boolean flag = false;
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		if (doc==null) return false;
		
		Element header = doc.getElementById("header");
		if (header==null) return false;
		
		
		//링크 탐색
		Elements as = header.select("a");
		for(Element a:as)
		{
			String href = a.attr("href"); 
			if(href.contains("index.cgi?logout=1")==true)
			{
				flag = true;
				break;
			}
		}

		return flag;
	}
	
	/**
	 * 다운받은 문서가 login되었는지 검증
	 * @param _xml
	 * @return
	 */
	public boolean checkLoginXml(String _xml) {
		Document doc = Jsoup.parse(_xml, "", Parser.xmlParser());
		if (doc==null) return false;

		//bugzilla태그 확인
		Elements items = doc.select("bugzilla");
		if(items.size()<=0) return false;
		
		//exporter정보확인
		Element bugzilla = items.get(0);
		String exporter = bugzilla.attr("exporter");
		
		//결과반환.
		if(exporter==null || exporter.length()==0) return false;
		else return true;
	}
	
	
	/**
	 * 버그리포트의 ID리스트를 생성하여 반환.
	 * @param httpString
	 * @return
	 */
	public List<Integer> getBuglist(String _html) {

		List<Integer> list = new ArrayList<Integer>();
		
		try{
			Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
			if (doc==null) return null;
			
			Elements tables = doc.select("#bugzilla-body table");
			if(tables.size()==0) return null;
			
			//테이블 선택
			Elements trs = null;
			for(int i=0; i<tables.size(); i++)
			{
				trs = tables.get(i).select("tr");
				if(trs.size()<=1) continue;
				
				if( trs.get(0).text().toLowerCase().compareTo("id opened")==0 )
					break;
				else trs = null;
			}
			if (trs==null) return null;
			
			
			//실제 데이터 가져오기
			for(int i=1; i<trs.size(); i++)
			{
				Elements tds = trs.get(i).select("td");
				list.add(Integer.parseInt(tds.get(0).text()));
			}
		}
		catch(Exception e)
		{
			log.printStackTrace(e);
			return null;
		}
		
		return list;
	}
	
	
}
