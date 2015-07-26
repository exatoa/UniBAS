package org.seal.UniBAS.Bugzilla;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.seal.UniBAS.Core.Database.SQLConnectionException;
import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Util.TextUtil;
import org.seal.UniBAS.Util.log;



public class BugzillaDBAdapter {
	
	/**
	 * @uml.property  name="dB"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private DBManager DB;
	
	Map<String, Object> params;
	
	public BugzillaDBAdapter(DBManager _db){
		DB = _db;
		params = new HashMap<String, Object>();
	}	

	//----------------------------------------------------------
	// 트랜젝션 설정
	//----------------------------------------------------------
	public void setTransaction()
	{
		DB.setTransaction();
	}
	
	public void commit()
	{
		DB.commit();
	}
	
	public void rollback()
	{
		DB.rollback();
	}
	
	
	
	
	//----------------------------------------------------------
	// 단일 데이터 저장.
	//----------------------------------------------------------

	/**
	 * 사용자 아이디를 저장
	 */
	public int saveUser(User _user) throws SQLConnectionException
	{	
		params.clear();
		params.put("id",		_user.ID);
		params.put("name",		TextUtil.addQuotes(_user.LoginName));
		params.put("realname",	TextUtil.addQuotes(_user.RealName));
		params.put("timezone",	_user.Timezone);


		int ret = DB.executeSP("saveUser", params);
		return ret;
	}
	
	/**
	 * 클래스정보를 저장
	 */
	
	public int saveClassification(PropertyItem _class) throws SQLConnectionException
	{	
		
		params.clear();
		params.put("name",	TextUtil.addQuotes(_class.Name));
		params.put("desc",	TextUtil.addQuotes(_class.Desc));


		int ret = DB.executeSP("saveClassification", params);
		return ret;
	}
	
	

	/**
	 * 프로덕트 정보를 저장
	 */
	public int saveProduct(int _classID, Product _product) throws SQLConnectionException
	{	
		
		params.clear();
		params.put("name",		TextUtil.addQuotes(_product.Name));
		params.put("desc",		TextUtil.addQuotes(_product.Desc));
		params.put("class_id",	_classID);//_product.Classification==null?null:_product.Classification.ID);


		int ret = DB.executeSP("saveProduct", params);
		return ret;
	}
		

	/**
	 * 컴포넌트 정보를 저장
	 */
	public int saveComponent(int _productID, PropertyItem _component) throws SQLConnectionException
	{	
		
		params.clear();
		params.put("product_id",	_productID);
		params.put("name",			TextUtil.addQuotes(_component.Name));
		params.put("desc",			TextUtil.addQuotes(_component.Desc));

		int ret = DB.executeSP("saveComponent", params);
		return ret;
	}
	
	
	
	/**
	 * 컴포넌트 정보들을 저장.
	 * @param _productID
	 * @param _components
	 * @return
	 */
	public boolean saveProducts(int _classificationID,  List<Product> _products) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		for(Product p: _products)
		{
			ret = saveProduct(_classificationID, p);
				
			if(ret<=0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveProduct ID : "+ p.ID +", Name :"+p.Name);
			}
			else
				cnt++;
		}

		if(cnt!= _products.size())
			return false;
		return true;
	}


	
	
	/**
	 * 컴포넌트 정보들을 저장.
	 * @param _productID
	 * @param _components
	 * @return
	 */
	public boolean saveComponents(int _productID,  List<PropertyItem> _components) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		for(PropertyItem c: _components)
		{
			ret = saveComponent(_productID, c);
				
			if(ret<=0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveComponent ID : "+ c.ID +", Name :"+c.Name);
			}
			else
				cnt++;
		}

		if(cnt!= _components.size())
			return false;
		return true;
	}

	

	/**
	 * 태그정보들을 저장.
	 * @param _tags
	 * @return
	 */
	public boolean saveKeywords(List<PropertyItem> _tags) throws SQLConnectionException {

		
		int ret;
		int cnt=0;
		for(PropertyItem t: _tags)
		{
			params.clear();
			params.put("name",			TextUtil.addQuotes(t.Name));
			params.put("desc",			TextUtil.addQuotes(t.Desc));
			
			ret = DB.executeSP("saveKeyword", params);
			
			if(ret==0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveTag ID : "+ t.ID +", Name :"+t.Name);
			}
			else
				cnt++;
		}

		if(cnt!= _tags.size())
			return false;
		return true;
	}
	
	

	/**
	 * 버그리포트를 저장.
	 */
	public int saveBugReport(BugReport _report) throws SQLConnectionException
	{	
		params.clear();
		params.put("bug_id",			_report.BugID);
		params.put("alias",				TextUtil.addQuotes(_report.Alias));
		params.put("status",			TextUtil.addQuotes(_report.Status));
		params.put("resolution",		TextUtil.addQuotes(_report.Resolution));
		params.put("product_id",		_report.Product.ID);
		params.put("component_id",		_report.Component.ID);
		params.put("version",			TextUtil.addQuotes(_report.Version));
		params.put("target_milestone",	TextUtil.addQuotes(_report.Milestone));
		params.put("priority",			TextUtil.addQuotes(_report.Priority));
		params.put("severity",			TextUtil.addQuotes(_report.Severity));
		params.put("platform",			TextUtil.addQuotes(_report.Platform));
		params.put("os",				TextUtil.addQuotes(_report.OS));
		params.put("votes",				_report.Votes);
		
		params.put("reporter_id",		_report.Reporter==null?null:_report.Reporter.ID);
		params.put("assignee_id",		_report.Assignee==null?null:_report.Assignee.ID);
		params.put("qa_contact_id",		_report.QA==null?null:_report.QA.ID);
		
		params.put("short_desc",		TextUtil.addQuotes(_report.Summary));
		params.put("status_whiteboard",	TextUtil.addQuotes(_report.StatusWhiteboard));
		params.put("creation_ts",		_report.CreationTime);
		params.put("delta_ts",			_report.UpdatedTime);

		params.put("keywords",			TextUtil.addQuotes(_report.Tags));
		params.put("bug_file_loc",		TextUtil.addQuotes(_report.BugFileLoc));
		params.put("deadline",			_report.Deadline);
		
		params.put("reporter_accessible",	_report.ReporterAccessible);
		params.put("cclist_accessible",	_report.CClistAccessible);
		params.put("everconfirmed",		_report.Everconfirmed);
		params.put("lastdiffed",		_report.Lastdiffed);
		params.put("remaining_time",	_report.RemainingTime);
		params.put("estimated_time",	_report.EstimatedTime);
		params.put("actual_time",		_report.ActualTime);

		int ret = DB.executeSP("saveBugReport", params);
		return ret;
	}

	
	//----------------------------------------------------------
	// 복합 데이터 저장.
	//----------------------------------------------------------
	/**
	 * 
	 * @param _bugid
	 * @param _cc
	 * @return
	 */
	public boolean saveCC(int _bugid, List<User> _cc) throws SQLConnectionException {

		int ret;
		int cnt =0;
		for(User u: _cc)
		{
			
			u.ID = saveUser(u);
			if(u.ID <=0){
				log.error("Unknown Submitter :: Cannot register user (Name : "+u.LoginName+", BugID : "+_bugid+")");
				continue;				
			}
			
			
			params.clear();
			params.put("bug_id",	_bugid);
			params.put("user_id",	u.ID);
			
			ret = DB.executeSP("saveCC", params);
		
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveCC : "+ u.ID);
			}
			else
				cnt++;
		}

		if(cnt!= _cc.size())
			return false;
		return true;
	}


	/**
	 * SeeAlso정보들 저장.
	 * @param _bugid
	 * @param _seealsos
	 * @return
	 */
	public boolean saveSeeAlsos(int _bugid, List<String> _seealsos) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		for(String s: _seealsos)
		{
			params.clear();
			params.put("bug_id", 	_bugid);
			params.put("value",		TextUtil.addQuotes(s));

			ret = DB.executeSP("saveSeeAlso", params);
		
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveSeeAlso : "+ s);
			}
			else cnt++;
		}

		if(cnt!= _seealsos.size())
			return false;
		return true;
	}

	/**
	 * 버그리포트들 간의 관계를 기록(saveDependency : 2, saveBlocks : 3, saveDuplications : 0)
	 * @param _bugid
	 * @param _type
	 * @param _dependson
	 * @return
	 */
	public boolean saveRelationship(List<Relationship> _relations) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		String spname = "";
		for(Relationship r: _relations)
		{
			params.clear();
			if (r.Type==RelationshipType.ChildOf){
				spname = "saveDependency";
				params.put("bug_id", 		r.SrcID);
				params.put("dependson",		r.DestID);
			}
			else if(r.Type == RelationshipType.ParentOf)
			{
				spname = "saveDependency";
				params.put("bug_id",	r.DestID);
				params.put("dependson",	r.SrcID);
			}
			else if(r.Type == RelationshipType.DuplicateOf)
			{
				spname = "saveDuplication";
				params.put("bug_id",	r.SrcID);
				params.put("dup_id",	r.DestID);
			}
			else if(r.Type == RelationshipType.RelatedTo)
			{
				spname = "saveRelationship";
				params.put("src_id",	r.SrcID);
				params.put("dest_id",	r.DestID);
			}
			
			ret = DB.executeSP(spname, params);
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveRelationship bugID : "+ r.SrcID + ", target : "+r.DestID);
			}
			else
				cnt++;
		}

		if(cnt!= _relations.size())
			return false;
		return true;
	}


	/**
	 * 버그노트들을 저장한다.
	 * @param _bugid
	 * @param _notes
	 * @return
	 */
	public boolean saveComments(int _bugid, List<Bugnote> _notes) throws SQLConnectionException {
		int ret;
		int cnt=0;
		for(Bugnote n: _notes)
		{
			n.Submitter.ID = saveUser(n.Submitter);
			if(n.Submitter.ID <=0){
				log.error("Unknown Submitter :: Cannot register user (Name : "+n.Submitter.LoginName+", noteID : "+n.ID+", BugID : "+_bugid+")");
				continue;				
			}
			params.clear();
			params.put("comment_id", 	n.ID);
			params.put("type", 			n.Type);
			params.put("bug_id", 		_bugid);			
			params.put("who",	 		n.Submitter.ID);
			params.put("bug_when",	 	n.CreationTime);
			params.put("thetext", 		TextUtil.addQuotes(n.TheText));
			params.put("isprivate", 	n.isPrivate);

			ret = DB.executeSP("saveComment", params);
		
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveComment (bugid="+_bugid+", noteID : "+ n.ID+")");
			}
			else
				cnt++;
		}

		if(cnt!= _notes.size())
			return false;
		return true;
	}
	
	/**
	 * 첨부파일에 대한 저장.
	 * @param _bugid
	 * @param _attachments
	 * @return
	 */
	public boolean saveAttachments(int _bugid, List<Attachment> _attachments)  throws SQLConnectionException{
		int cnt=0;
		int ret;
		
		for(Attachment a: _attachments)
		{
			a.Attacher.ID = saveUser(a.Attacher);
			if(a.Attacher.ID <=0){
				log.error("Unknown Attacher :: Cannot register user (Name : "+a.Attacher.LoginName+", AttachID : "+a.ID+", BugID : "+_bugid+")");
				continue;				
			}
			params.clear();
			params.put("attach_id", 	a.ID);
			params.put("bug_id", 		_bugid);
			params.put("creation_ts", 	a.CreationTime);
			params.put("updated_ts", 	a.ModificationTime);
			params.put("description", 	TextUtil.addQuotes(a.Desc));
			params.put("filename", 		TextUtil.addQuotes(a.Filename));
			params.put("filesize", 		a.FileSize);
			params.put("mimetype", 		a.MimeType);
			params.put("isobsolete", 	a.isObsolete);
			params.put("ispatch", 		a.isPatch);
			params.put("isprivate", 	a.isPrivate);
			params.put("isurl", 		a.isUrl);
			params.put("user_id", 		a.Attacher.ID);
			params.put("thedata", 		a.Data);
			
			
			ret = DB.executeSP("saveAttachment", params);
			
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveAttachment AttachID : "+ a.ID);
			}
			else
				cnt++;
		}
		
		if(cnt!= _attachments.size())
			return false;
		return true;
	}
	
	/**
	 * 버그리포트의 변경이력에 대해 저장.
	 * @param _bugid
	 * @param _histories
	 * @return
	 * @throws SQLException
	 */
	public boolean saveHistories(int _bugid, List<BugHistory> _histories) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		for(BugHistory h: _histories)
		{
			h.Who.ID = saveUser(h.Who);
			if(h.Who.ID <0){
				log.error("Unknown Changer :: Cannot register user in History(Name : "+h.Who.LoginName+", BugID : "+_bugid+")");
				continue;
			}
			params.clear();
			params.put("bug_id", 		_bugid);
			params.put("who", 			h.Who.ID);
			params.put("when", 			(h.When.length()>19)?h.When.substring(0,19):h.When);
			params.put("what", 			TextUtil.addQuotes(h.FieldID));
			params.put("removed", 		TextUtil.addQuotes(h.Removed));
			params.put("added", 		TextUtil.addQuotes(h.Added));
			params.put("attach_id", 	h.AttachID);
			
			ret = DB.executeSP("saveActivity", params);
			
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveActivity bugID : "+ _bugid +", Who :"+h.Who.LoginName);
			}
			else
				cnt++;
		}
		
		if(cnt!= _histories.size())
			return false;
		return true;
	}


	
	/**
	 * 버그리포트에 대한 투표현황을 저장.
	 * @param _bugid
	 * @param _votes
	 * @return
	 */
	public boolean saveVotes(int _bugid, List<Vote> _votes) throws SQLConnectionException{
		
		int ret;
		int cnt=0;		
		for(Vote v: _votes)
		{
			v.Voter.ID = saveUser(v.Voter);
			if(v.Voter.ID <=0){
				log.error("Unknown Submitter :: Cannot register user in Vote (Name : "+v.Voter.LoginName+", BugID : "+_bugid+")");
				continue;				
			}
			params.clear();
			params.put("bug_id",		_bugid);
			params.put("user_id", 		v.Voter.ID);
			params.put("count",			v.Count);
			
			ret = DB.executeSP("saveVote", params);
			
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveVote BUGID : "+ _bugid +", Name :"+ v.Voter.LoginName);
			}
			else
				cnt++;
		}
		
		if(cnt!= _votes.size())
			return false;
		return true;
	}

	
	
	/**
	 * 마이그레이션 함수들을 실행.
	 * @param _tableName
	 * @param _siteID
	 * @return
	 */
	public int migration(String _tableName, int _siteID) throws SQLConnectionException{
	
		int ret;
		params.clear();
		params.put("site_id",	_siteID);
		
		ret = DB.executeSP("Migration_"+_tableName, params);
			
		return ret;
	}


	public boolean checkDuplicateReport(int id)  throws SQLConnectionException{
		int ret;
		params.clear();	
		params.put("bug_id",	id);
		
		ret = DB.executeSP("check_bugDuplicate", params);
		
		if(ret<=0) return false;
		else return true;
	}


	public String getDataBugID(int id) throws SQLConnectionException {
		String ret;
		params.clear();
		params.put("bug_id",	id);
		
		ret = DB.executeSPStr("getDateBugID", params);
		
		return ret;		
	}


	public int checkLastBugID(String _str, String _delimiter)  throws SQLConnectionException{
		params.clear();
		params.put("strID",	_str);
		params.put("delimiter",	_delimiter);
		
		return DB.executeSP("checkLastBugID", params);
	}

	
	/**
	 * 버그리포트 리스트를 추가한다. : list_manager - SAVED
	 * @param _date
	 * @param _type
	 * @param _url
	 * @return
	 */
	public int addList(String _date, int _type, String _url, String _path) throws SQLConnectionException {
		
		params.clear();
		params.put("date",	_date);
		params.put("type",	_type);
		params.put("url",	_url);
		params.put("path",	_path);
		
		return DB.executeSP("addListInfo", params);
	}
	
	/**
	 * 버그리포트 리스트의 수집상태를 반영 : SAVED, 버그리포트 추가등록 (PENDING)
	 * @param _date
	 * @param _type
	 * @param _url
	 * @return
	 */
	public int add_BugList(int _listID, String _delimiter, String _strID) throws SQLConnectionException {
		params.clear();
		params.put("id",	_listID);
		params.put("delimiter",	_delimiter);
		params.put("strID",	_strID);
		
		return DB.executeSP("add_BugList", params);
	}

	/**
	 * 버그리포트의 수집상태를 반영 : REQUESTED
	 * @param bug_id
	 * @param url
	 * @return
	 */
	public int update_bugURL(int bug_id, String url) throws SQLConnectionException {
		params.clear();
		params.put("id",	bug_id);
		params.put("url",	url);
		
		return DB.executeSP("updateBug_bugURL", params);			
	}

	/**
	 * 버그리포트의 수집상태를 반영 : DOWNLOADED
	 * @param bug_id
	 * @param path
	 * @return
	 */
	public int update_bugDownload(int bug_id, String path) throws SQLConnectionException {
			
		params.clear();
		params.put("id",	bug_id);
		params.put("path",	path);
				
		return DB.executeSP("updateBug_bugDownload", params);	
	}


	/**
	 * 버그리포트의 수집상태를 반영 : PARSED
	 * @param bug_id
	 * @return
	 */
	public int update_bugParsed(int bug_id)  throws SQLConnectionException{

		params.clear();
		params.put("id",	bug_id);
		
		return DB.executeSP("updateBug_bugParsed", params);
	}
	
	/**
	 * 버그리포트의 수집상태를 반영 : SAVED
	 * @param bug_id
	 * @return
	 */
	public int update_bugSaved(int bug_id) throws SQLConnectionException {
		
		params.clear();
		params.put("id",	bug_id);
				
		return DB.executeSP("updateBug_bugSaved", params);			
	}


	public int update_histURL(int _bugID, String url) throws SQLConnectionException {
		params.clear();
		params.put("id",	_bugID);
		params.put("url",	url);
		
		return DB.executeSP("updateBug_histURL", params);		
	}


	public int update_histDownload(int _bugID, String path) throws SQLConnectionException {
		
		params.clear();
		params.put("id",	_bugID);
		params.put("path",	path);
				
		return DB.executeSP("updateBug_histDownload", params);	
	}


	public int update_histParsed(int _bugID)  throws SQLConnectionException{

		params.clear();
		params.put("id",	_bugID);
		
		return DB.executeSP("updateBug_histParsed", params);		
	}
	
	public int update_histSaved(int _bugID)  throws SQLConnectionException{

		params.clear();
		params.put("id",	_bugID);
		
		return DB.executeSP("updateBug_histSaved", params);
	}


	public int update_voteURL(int _bugID, String url)  throws SQLConnectionException{
		params.clear();
		params.put("id",	_bugID);
		params.put("url",	url);
		
		return DB.executeSP("updateBug_voteURL", params);	
	}


	public int update_voteDownload(int _bugID, String path) throws SQLConnectionException {
		
		params.clear();
		params.put("id",	_bugID);
		params.put("path",	path);
				
		return DB.executeSP("updateBug_voteDownload", params);			
	}
	
	
	
	public int update_voteParsed(int _bugID) throws SQLConnectionException {

		params.clear();
		params.put("id",	_bugID);
		
		return DB.executeSP("updateBug_voteParsed", params);
	}
	
	public int update_voteSaved(int _bugID) throws SQLConnectionException {
		
		params.clear();
		params.put("id",	_bugID);
				
		return DB.executeSP("updateBug_voteSaved", params);	
	}

	
	/**
	 * 버그리포트의 수집상태를 반영 : DONE
	 * @param bug_id
	 * @return
	 */
	public int update_Done(int bug_id) throws SQLConnectionException {
		
		params.clear();
		params.put("id",	bug_id);
				
		return DB.executeSP("updateBug_done", params);			
	}



	public int update_BugInfo(int bug_id, String state, String reportURL, String reportPath, String histURL, String histPath, String voteURL, String votePath) throws SQLConnectionException {
		
		params.clear();
		params.put("bug_id",		bug_id);
		params.put("progress",		state);
		params.put("report_url",	reportURL);
		params.put("report_path",	reportPath);
		params.put("hist_url",		histURL);
		params.put("hist_path",		histPath);
		params.put("vote_url",		voteURL);
		params.put("vote_path",		votePath);
		
		return DB.executeSP("update_bugInfo", params);
	}

	public String getRevivalIDList()  throws SQLConnectionException{
		
		params.clear();

		String ret = DB.executeSPStr("getRevivalIDList", params);
		return ret;
	}








//	
//	/**
//	 * 각종 속성값에 대한 저장. (id, name으로 이루어진 값들)
//	 * @param _type
//	 * @param items
//	 * @return
//	 */
//	public boolean savePropertyItems(String _type, List<PropertyItem> items){
//		Map<String, Object> params = new HashMap<String, Object>();
//		
//		String procedureName = "save"+_type;
//		int ret;
//		int cnt=0;
//		for(PropertyItem i: items)
//		{
//			params.put("id",			i.ID);
//			params.put("value",			TextUtil.addQuotes(i.Name));
//			
//			ret = DB.executeSP(procedureName, params);
//			
//			if(ret==0)
//			{
//				log.error("CODE:"+ ret+ ", Insert Error "+procedureName+" ID : "+ i.ID +", Name :"+i.Name);
//			}
//			else
//				cnt++;
//		}
//
//		if(cnt!= items.size())
//			return false;
//		return true;
//	}
}
