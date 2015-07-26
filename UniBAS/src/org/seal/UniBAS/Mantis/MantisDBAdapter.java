package org.seal.UniBAS.Mantis;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.seal.UniBAS.Mantis.Model.Attachment;
import org.seal.UniBAS.Mantis.Model.BugHistory;
import org.seal.UniBAS.Mantis.Model.BugReport;
import org.seal.UniBAS.Mantis.Model.Bugnote;
import org.seal.UniBAS.Mantis.Model.Category;
import org.seal.UniBAS.Mantis.Model.Project;
import org.seal.UniBAS.Mantis.Model.PropertyItem;
import org.seal.UniBAS.Mantis.Model.Relationship;
import org.seal.UniBAS.Mantis.Model.User;
import org.seal.UniBAS.Mantis.Model.Vote;
import org.seal.UniBAS.Util.TextUtil;
import org.seal.UniBAS.Util.log;
import org.seal.UniBAS.Core.Database.DBManager;
import org.seal.UniBAS.Core.Database.SQLConnectionException;



public class MantisDBAdapter {
	
	/**
	 * @uml.property  name="dB"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private DBManager DB;
	private Map<String, Object> params = null;
	
	public MantisDBAdapter(DBManager _db){
		DB = _db;
		params = new HashMap<String, Object>();
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
		params.put("role",		TextUtil.addQuotes(_user.Role));
		//params.put("timezone",	_user.Timezone);

		int ret = DB.executeSP("registerUser", params);
		return ret;
	}
	
	/**
	 * 클래스정보를 저장
	 */
	public int saveClassification(PropertyItem _class) throws SQLConnectionException
	{	
		params.clear();
		params.put("id",	_class.ID);
		params.put("name",	TextUtil.addQuotes(_class.Name));
		params.put("desc",	TextUtil.addQuotes(_class.Desc));


		int ret = DB.executeSP("saveClassification", params);
		return ret;
	}
	
	

	/**
	 * 프로덕트 정보를 저장
	 */
	public int saveProject(int _classifficationID, Project _product) throws SQLConnectionException
	{	
		params.clear();
		params.put("id",		_product.ID);
		params.put("name",		TextUtil.addQuotes(_product.Name));
		params.put("desc",		TextUtil.addQuotes(_product.Desc));
		//params.put("class_id",	_classifficationID);//_product.Classification==null?null:_product.Classification.ID);
		params.put("parent_id",	_product.ParentID);


		int ret = DB.executeSP("saveProject", params);
		return ret;
	}
		

	/**
	 * 컴포넌트 정보를 저장
	 */
	public int saveCategory(Category _category) throws SQLConnectionException
	{	
		
		params.clear();
		params.put("id",			_category.ID);
		params.put("project_id",	_category.ParentID);
		params.put("name",			TextUtil.addQuotes(_category.Name));

		int ret = DB.executeSP("saveCategory", params);
		return ret;
	}
	

	/**
	 * 버그리포트를 저장.
	 */
	public int saveBugReport(BugReport _report) throws SQLConnectionException
	{	
		params.clear();
		params.put("id", 				_report.ID);
		params.put("project",			TextUtil.addQuotes(_report.Project.Name));
		params.put("category", 			TextUtil.addQuotes(_report.Category.Name));
		params.put("reporter_id", 		_report.Reporter.ID);
		params.put("handler_id", 		_report.Handler.ID);
		params.put("duplicate_id", 		_report.DuplicateID);
		params.put("priority", 			TextUtil.addQuotes(_report.Priority));
		params.put("severity", 			TextUtil.addQuotes(_report.Severity));
		params.put("status", 			TextUtil.addQuotes(_report.Status));
		params.put("resolution", 		TextUtil.addQuotes(_report.Resolution));
		params.put("reproducibility", 	TextUtil.addQuotes(_report.Reproducibility));
		params.put("os", 				TextUtil.addQuotes(_report.OS));
		params.put("os_build", 			TextUtil.addQuotes(_report.OS_Build));
		params.put("platform", 			TextUtil.addQuotes(_report.Platform));
		params.put("build", 			TextUtil.addQuotes(_report.Build));
		params.put("version", 			TextUtil.addQuotes(_report.Version));
		params.put("fixed_in_version", 	TextUtil.addQuotes(_report.FixedInVersion));
		params.put("target_version", 	TextUtil.addQuotes(_report.TargetVersion));
		params.put("summary", 			TextUtil.addQuotes(_report.Summary));
		params.put("date_submitted", 	_report.DateSubmitted);
		params.put("last_updated", 		_report.LastUpdated);
		params.put("due_date",		 	_report.DueDate);
		params.put("projection", 		_report.Projection);
		params.put("profile_id",		_report.ProfileID);
		params.put("view_state",		_report.ViewState);
		params.put("eta", 				_report.ETA);
		params.put("sponsorship_total", _report.SponsorshipTotal);
		params.put("sticky", 			_report.Sticky);
		params.put("tags", 				TextUtil.addQuotes(_report.Tags));
		params.put("description", 		TextUtil.addQuotes(_report.Description));
		params.put("step_procedure", 	TextUtil.addQuotes(_report.Steps));
		params.put("additional_info", 	TextUtil.addQuotes(_report.AdditionalInfo));

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
	public boolean saveCC(int _bugid, List<String> _cc) throws SQLConnectionException {
		
		int ret;
		int cnt =0;
		for(String s: _cc)
		{
			params.clear();
			params.put("bug_id",	_bugid);
			params.put("username",	TextUtil.addQuotes(s));
			
			ret = DB.executeSP("saveCC", params);
		
			if(ret<0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveCC : "+ s);
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
	public boolean saveSeeAlsos(int _bugid, List<String> _seealsos) throws SQLConnectionException
	{
		int ret;
		int cnt=0;
		for(String s: _seealsos)
		{
			params.clear();
			params.put("bug_id", _bugid);
			params.put("value",	TextUtil.addQuotes(s));

			ret = DB.executeSP("saveSeeAlso", params);
		
			if(ret<=0)
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
	public boolean saveRelationships(List<Relationship> _relations) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		for(Relationship r: _relations)
		{
			params.clear();
			params.put("src_bug_id", 		r.SrcID);
			params.put("dest_bug_id",		r.DestID);
			params.put("type",	r.Type.getValue());

			ret = DB.executeSP("saveRelationship", params);
		
			if(ret<=0)
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
	public boolean saveBugnotes(int _bugid, List<Bugnote> _notes) throws SQLConnectionException {
		
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
			//params.put("id", 			n.ID);
			params.put("bug_id", 		_bugid);
			params.put("who", 			n.Submitter.ID);
			params.put("view_state", 	n.isPrivate);
			params.put("creation_ts",	n.CreationTime);
			params.put("update_ts", 	n.UpdateTime);
			params.put("type", 			n.Type);	
			params.put("thetext", 		TextUtil.addQuotes(n.TheText));

			ret = DB.executeSP("saveBugnote", params);
		
			if(ret<=0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveBugnote (bugid="+_bugid+", noteID : "+ n.ID+")");
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
	public boolean saveAttachments(int _bugid, List<Attachment> _attachments) throws SQLConnectionException {
		
		int cnt=0;
		int ret;
		
		for(Attachment a: _attachments)
		{
			a.Attacher.ID = saveUser(a.Attacher);
			if(a.Attacher.ID <=0){
				log.error("Unknown Attacher :: Cannot register user (Name : "+a.Attacher.LoginName+", AttachID : "+a.Attacher.ID+", BugID : "+_bugid+")");
				continue;				
			}
			
			params.clear();
			params.put("file_id", 		a.ID);
			params.put("bug_id", 		_bugid);
			params.put("user_id", 		a.Attacher.ID);
			
			params.put("description", 	TextUtil.addQuotes(a.Desc));
			
			params.put("filename", 		TextUtil.addQuotes(a.Filename));
			params.put("filesize", 		a.FileSize);
			params.put("file_type", 	a.MimeType);
			params.put("creation_ts", 	a.CreationTime);
			params.put("thedata", 		a.Data);
			
			ret = DB.executeSP("saveAttachment", params);
			
			if(ret<=0)
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
			//Mantis에서 WhoID가 없는 사람이 있음.....탈퇴사용자 등...)
			h.Who.ID = saveUser(h.Who);
			if(h.Who.ID <=0){
				log.error("Unknown Changer :: Cannot register user (Name : "+h.Who.LoginName+", BugID : "+_bugid+")");
				continue;				
			}
			
			params.clear();
			params.put("bug_id", 		_bugid);
			params.put("user_id", 		h.Who.ID);
			params.put("when", 			(h.When.length()>19)?h.When.substring(0,19):h.When);
			params.put("field_name", 	TextUtil.addQuotes(h.FieldID));
			params.put("old_value", 	TextUtil.addQuotes(h.Removed));
			params.put("new_value", 	TextUtil.addQuotes(h.Added));
			
			ret = DB.executeSP("saveHistory", params);
			
			if(ret==0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveHistory bugID : "+ _bugid +", Who :"+h.Who.LoginName);
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
	public boolean saveVotes(int _bugid, List<Vote> _votes) throws SQLConnectionException {
		
		int ret;
		int cnt=0;		
		for(Vote v: _votes)
		{
			params.clear();
			params.put("bug_id",		_bugid);
			params.put("user_id", 		v.Voter.ID);
			params.put("username",		TextUtil.addQuotes(v.Voter.LoginName));
			params.put("count",			v.Count);
			
			ret = DB.executeSP("saveVote", params);
			
			if(ret==0)
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
	 * 컴포넌트 정보들을 저장.
	 * @param _productID
	 * @param _components
	 * @return
	 */
	public boolean saveProjects(int _classificationID,  List<Project> _products) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		for(Project p: _products)
		{
			ret = saveProject(_classificationID, p);
				
			if(ret<=0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveProject ID : "+ p.ID +", Name :"+p.Name);
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
	public boolean saveCategories(List<Category> _categories) throws SQLConnectionException {
		
		int ret;
		int cnt=0;
		for(Category c: _categories)
		{
			ret = saveCategory(c);
				
			if(ret<=0)
			{
				log.error("CODE:"+ ret+ ", Insert Error saveComponent ID : "+ c.ID +", Name :"+c.Name);
			}
			else
				cnt++;
		}

		if(cnt!= _categories.size())
			return false;
		return true;
	}


	/**
	 * 태그정보들을 저장.
	 * @param _tags
	 * @return
	 */
	public boolean saveTags(List<PropertyItem> _tags) throws SQLConnectionException {

		int ret;
		int cnt=0;
		for(PropertyItem t: _tags)
		{
			params.clear();
			params.put("id",			t.ID);
			params.put("name",			TextUtil.addQuotes(t.Name));
			
			ret = DB.executeSP("saveTag", params);
			
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
	 * 각종 속성값에 대한 저장. (id, name으로 이루어진 값들)
	 * @param _type
	 * @param items
	 * @return
	 */
	public boolean savePropertyItems(String _type, List<PropertyItem> items) throws SQLConnectionException {
				
		String procedureName = "save"+_type;
		int ret;
		int cnt=0;
		for(PropertyItem i: items)
		{
			params.clear();
			params.put("id",			i.ID);
			params.put("value",			TextUtil.addQuotes(i.Name));
			
			ret = DB.executeSP(procedureName, params);
			
			if(ret==0)
			{
				log.error("CODE:"+ ret+ ", Insert Error "+procedureName+" ID : "+ i.ID +", Name :"+i.Name);
			}
			else
				cnt++;
		}

		if(cnt!= items.size())
			return false;
		return true;
	}
	
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

	/**
	 * 작업할 새 버그 아이디를 등록한다.
	 * @param id
	 */
	public int AddUnit(int id) throws SQLConnectionException {
		
		params.clear();
		params.put("bug_id", 				id);

		int ret = DB.executeSP("addUnit", params);
		return ret;
	}
	
	/**
	 * 현재 작업상태를 반영한다.
	 * @param _id
	 * @param _progress
	 * @param _url
	 * @param _path
	 * @return
	 */
	public int UpdateUnit(int _id, String _progress, String _url, String _path) throws SQLConnectionException {

		params.clear();
		params.put("bug_id", 	_id);
		params.put("progress", 	_progress);
		params.put("url", 		_url);
		params.put("path", 		_path);

		int ret = DB.executeSP("updateUnit", params);
		return ret;
		
	}
	
	/**
	 * 재시작할 아이디들의 목록을 받아옴.
	 * @param _id
	 * @param _progress
	 * @param _url
	 * @param _path
	 * @return
	 */
	public String getRevivalIDList() throws SQLConnectionException {

		params.clear();

		String ret = DB.executeSPStr("getRevivalIDList", params);
		return ret;
		
	}
}
