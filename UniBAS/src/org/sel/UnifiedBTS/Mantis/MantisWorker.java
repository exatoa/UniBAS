package org.sel.UnifiedBTS.Mantis;

import java.util.ArrayList;
import java.util.List;

import org.sel.UnifiedBTS.Core.Controller;
import org.sel.UnifiedBTS.Core.Database.SQLConnectionException;
import org.sel.UnifiedBTS.Core.Model.SiteInfo;
import org.sel.UnifiedBTS.Mantis.Model.BugReport;
import org.sel.UnifiedBTS.Mantis.Model.Category;
import org.sel.UnifiedBTS.Mantis.Model.Project;
import org.sel.UnifiedBTS.Mantis.Model.PropertyItem;
import org.sel.UnifiedBTS.Mantis.Model.User;
import org.sel.UnifiedBTS.Util.Config;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.WorkState;
import org.sel.UnifiedBTS.Util.log;

public class MantisWorker extends Controller {

	/////////////////////////////////////////////////////////////////
	//  관리용 관련 멤버변수.
	/////////////////////////////////////////////////////////////////
	private SiteInfo ThisSite = null;
	private MantisDBAdapter Adapter = null;
	private MantisClient Client = null;
	
	
	/////////////////////////////////////////////////////////////////
	//  작업상태 관련변수
	/////////////////////////////////////////////////////////////////
	private List<Integer> BugList = null;
	private WorkState State = null;  
	
	
	///////////////////////////////////////////////////////////////
	// 생성자 
	/////////////////////////////////////////////////////////////
	public MantisWorker(Config _config) {
		super(_config);
	}


	////////////////////////////////////////////////////
	//  작업코드
	////////////////////////////////////////////////////

	

	////////////////////////////////////////////////////
	//  작업코드
	////////////////////////////////////////////////////
	@Override
	public boolean workTarget(SiteInfo _info) {
		
		//1. 필요 도구 및 설정 =======================================================
		Adapter = new MantisDBAdapter(DB);					//Adapter는 예상되는 에러 없음.
		Client = new MantisClient(_info, Adapter);		//Client 생성.
		ThisSite = _info;
		
		//필요한 객체들 생성.
		boolean restartOption = false;
		State = WorkState.getInstance(Config.it().LOG_PATH +"workstate_"+Config.it().NAME+".txt");
		
		//재시작 검토
		if(State.getCurrentBugID()>0) restartOption = true;

		
		//2. 상태 셋팅=======================================================		
		try{
			log.info( "Login and Setting ......");
			if(this.WorkProcessLogin()==false){
				log.info( "Login and Setting                    ... Failed.");
				return false;
			}
			log.info( "Login and Setting                    ... Done.");
			
			if(restartOption==false)
			{
				log.info( "Preprocessing ......");
				if(this.WorkProcessPrepare()==false){
					log.info( "Preprocessing                        ... Failed.");
					return false;
				}
				log.info( "Preprocessing                        ... Done.");
			}
			else
			{
				//이전에 작업 후 에러가 발생했던 버그항목들 재처리.
				log.info( "Reviving ......");
				if(WorkProcessRevive()==false){
					log.info( "Reviving                             ... Failed.");
					return false;
				}
				log.info( "Reviving                             ... Done.");
			}

			log.info( "Crawling ......");
			if(WorkProcessOverall()==false){
				log.info( "Crawling                             ... Failed.");
				return false;
			}
			log.info( "Crawling                             ... Done.");
		}
		catch (Exception e) {
			log.println(e.getMessage());
			log.printStackTrace(e);
			return false;
		}
		
		return true;
	}
	
	


	/////////////////////////////////////////////////////////////////
	//  전처리 과정 (로그인, 프로덕트, 컴포넌트 키워드 등..)
	/////////////////////////////////////////////////////////////////
	/**
	 * 버그리포트를 수집하기전 처리할 사항들에 대한 처리함수.
	 * @return
	 */
	private boolean WorkProcessLogin()
	{
		//로그인
		boolean ret =  Client.login();
		if(ret==false){
			log.error("Failed to login : \""+ ThisSite.UserID + "\".");
			return false;
		}
		log.info( ThisSite.UserID + " signed in");
		
		//타임존 변경 (UTC)
		ret =  Client.setTimezone(null);
		if(ret==false){
			log.error("Failed to change timezone : \""+ ThisSite.UserID + "\".");
			return false;
		}
		log.info( "Changed timezone to UTC");	
		return true;
	}
	
	/**
	 * 버그리포트를 수집하기전 처리할 사항들에 대한 처리함수.
	 * @return
	 */
	private boolean WorkProcessPrepare() throws SQLConnectionException
	{
		
		//연속적인 페이지 분석을 위한 포석.
		Client.setPreprocessPage();
		
		//Product 처리
		List<Project> products = Client.getProducts();
		if(products == null) return false;
		log.info("Gathered Product information (count="+ products.size()+")");
		
		
		//Resolutions 처리
		List<PropertyItem> resolutions = Client.getResolutions();
		if(resolutions == null) return false;
		log.info("Gathered Resolutions information (count="+ resolutions.size()+")");
		
		
		//Status 처리
		List<PropertyItem> statuses = Client.getStatuses();
		if(statuses == null) return false;
		log.info("Gathered Status information (count="+ statuses.size()+")");
		
		
		//Priority 처리
		List<PropertyItem> priorities = Client.getPriorities();
		if(priorities == null) return false;
		log.info("Gathered Prioritiy information (count="+ priorities.size()+")");
		
		//Severity 처리
		List<PropertyItem> severities = Client.getSeverities();
		if(severities == null) return false;
		log.info("Gathered Severity information (count="+ severities.size()+")");
		
		//Tags 처리
		List<PropertyItem> tags = Client.getTags();
		if(tags == null) return false;
		log.info("Gathered Tags information (count="+ tags.size()+")");
		
		//Reproducibility 처리 (등록 페이지에서 불러와서 랜덤한 productID 가 필요.)
		log.info("project ID : "+products.get(0).ID + "("+products.get(0).Name+")");
		List<PropertyItem> reproducibilities = Client.getReproducibilities(products.get(0).ID);
		if(reproducibilities==null) return false;
		log.info("Gathered Reproducibility information (count="+ reproducibilities.size()+")");
		
		//Category 정보수집 (subProject 제외했었는데 통합...왜 분리했었는지 모르겠음.)
		log.info("Gathering components information......");
		List<Category> categories = new ArrayList<Category>();
		for(Project product : products)
		{
			List<Category> temp = Client.getCategories(product.ID);
			if(temp==null) return false;
			
			//중복없이 추가.
			categories.addAll(temp);
		}
		
		log.info("Gathered category information. (count = "+ categories.size()+")");
		
		
		//DB에 저장.
		Adapter.setTransaction();
		boolean saveState = true;
		saveState = Adapter.saveProjects(1, products);
		saveState = Adapter.saveTags(tags);
		
		saveState = Adapter.savePropertyItems("Resolution", resolutions);
		saveState = Adapter.savePropertyItems("Status", statuses);
		saveState = Adapter.savePropertyItems("Priority", priorities);
		saveState = Adapter.savePropertyItems("Severity", severities);
		saveState = Adapter.savePropertyItems("Reproducibility", reproducibilities);
		saveState = Adapter.saveCategories(categories);

		if(saveState==true){
			Adapter.commit();
			log.info("Saved all informations.");
		}
		else{
			Adapter.rollback();
			log.info("Failed to save informations.");
			
		}
		
		//LastBugID구하기
		int LastBugID = Client.getLastBugID();
		if (LastBugID==-1) return false;
		
		State.setLastBugID(LastBugID);
		log.info("Total BugReport is "+LastBugID);
		
		
		return saveState;
	}
	
	
	/** 
	 * Category값들 사이에 중복이 있는지 검사.
	 *  - WorkProcessPrepare의 부분함수.
	 * @param _items
	 * @param key
	 * @return
	 */
	private boolean containKeys(List<Category> _items, Category key)
	{
		for(Category c : _items)
		{
			if(c.ID==key.ID)
				return true;
		}
		return false;
	}
	
	
	
	/**
	 * 앞서 진행한 작업에서 실패한 버그리포트들을 선택해서 다시 작업한다.
	 * @return
	 */
	private boolean WorkProcessRevive() {
		
		List<Integer> ids = getRevivalIDList();
		
		for(Integer id : ids)
		{
			workProcessUnitManager(id);
		}
		
		return true;
	}
	

	/**
	 * DB에서 받아온 문자열 ID 리스트를 List<Integer> 로 변경
	 * @return
	 */
	public List<Integer> getRevivalIDList() {
		
		List<Integer> ids = new ArrayList<Integer>();
		
		while(true)
		{
			try{
				String str = Adapter.getRevivalIDList();
						
				if(str.length()==0) return ids;
				
				String[] strArr = str.split(",");
				
				for(int i=0; i<strArr.length; i++)
				{
					ids.add(Integer.parseInt(strArr[i]));
				}
			}
			catch(SQLConnectionException e)
			{
				DB.reconnection();
				continue;
			}
			break;
		}
		
		return ids;
	}

	
	
	
	/////////////////////////////////////////////////////////////////
	//  버그리포트 처리 로직
	/////////////////////////////////////////////////////////////////
	/**
	 * 전체적인 수집과정을 수행
	 * @return
	 */
	private boolean WorkProcessOverall() {
		
		int id = State.getCurrentBugID();
		if(id==0)id++;
		
		//현재 ID부터 마지막까지 반복해서 작업
		for( ; id <= State.getLastBugID(); State.setCurrentBugID(++id))
		{
			workProcessUnitManager(id);
		}
		return true;
	}

	
	private int workProcessUnitManager(int id) {
		
		int ret = 0;
		while(true)
		{
			try{
				Adapter.AddUnit(id);						//등록
			}
			catch(SQLConnectionException e)
			{
				DB.reconnection();
				continue;
			}
			break;
		}
		
		
		log.info("");
		log.info("Report "+id+" : Trying to analysis....");
		
		ret = this.workProcessUnit(id);
		
		while(true)
		{
			try{
				//작업결과 반영.
				switch(ret)
				{
				case 0:
					log.error("Report "+id+" : Failed to analysis");
					break;
				
				case -1:	//접근권한이 없음
					log.error("Report "+id+" : Access Denied.");
					Adapter.UpdateUnit(id, "PRIVATE",null,null);
					break;
									
				case -2:	//버그리포트가 없음 (삭제됨)
					log.info("Report "+id+" : Not found Issue");
					Adapter.UpdateUnit(id, "NONE",null,null);	
					break;
					
				case -3:
					log.error("Report "+id+" : Project deleted. Not found Issue.");
					Adapter.UpdateUnit(id, "APP_ERROR",null,null);
					break;
				
				case -4:
					log.error("Report "+id+" : Unknown Application error..");
					Adapter.UpdateUnit(id, "APP_ERROR",null,null);
					break;
					
				case -5: //첨부파일 일부가 저장안됨
					log.warn("Report "+id+" : It has some problems with Attached Files");
					Adapter.UpdateUnit(id, "DONE_WITHOUTFILE",null,null);
					break;
					
				case -6:
					log.error("Report "+id+" : Failed to save information");
					Adapter.UpdateUnit(id, "FAIL",null,null);
					break;
					
				default :	//성공
					log.info("Report "+id+" : Done!");
					Adapter.UpdateUnit(id, "DONE",null,null);
					break;
				}
			}
			catch(SQLConnectionException e)
			{
				DB.reconnection();
				continue;
			}
			break;
		}
		
		return ret;
	}

	/**
	 * 한개의 버그리포트에 대해서 작업한다.
	 * @param id
	 * @return Success - 작업이 완료된 버그의 ID
	 *         0 - 버그리포트 분석중 에러 발생
	 *         -1 - 버그리포트를 읽을 권한이 없음 (private)
	 *         -2 -  버그리포트가 존재하지않음 (삭제됨)
	 *         -3 - 버그리포트가 속한 프로젝트가 삭제됨
	 *         -4 - 다른 응용프로그램 에러
	 *         -5 - 저장은 되었지만 첨부파일 일부가 저장되지 않음
	 *         -6 - 데이터 저장중 실패
	 */
	private int workProcessUnit(int id) {
		
		String BugUrl = "";
		String BugPath = "";
		
		while(true)
		{
			try{
				//해당 버그리포트 가져오기.
				BugReport report = Client.getBugreport(id);
				if(report==null)	return 0;			//버그리포트 분석중 에러 발생
				if(report.ID==0)	return -1;			//버그리포트를 읽을 권한이 없음 (private)
				if(report.ID==-1)	return -2;			//버그리포트가 존재하지않음 (삭제됨)
				if(report.ID==-2)	return -3;			//버그리포트가 속한 프로젝트가 삭제됨
				if(report.ID==-3)	return -4;			//다른 응용프로그램 에러
				
				BugUrl = Client.getLastUrl();
				BugPath = TextUtil.convertURLtoPath(BugUrl);
				
				
				//버그 첨부파일 바이너리 다운로드.
				boolean downloadState = true;
				if(report.Attachments!=null){
					if(Client.getBinaryData(report.Attachments)==false){
						Adapter.UpdateUnit(id, "DOWNLOAD_ERROR", BugUrl, BugPath);
						downloadState = false;
					}
				}	
					
				//데이터 저장 시작.
				Adapter.setTransaction();
								
				if(saveBugreport(report)==true)
				{
					Adapter.commit();
					Adapter.UpdateUnit(id, "SAVED", BugUrl, BugPath);
					if(downloadState==true)	return report.ID;
					else					return -5;					//저장은 되었지만 첨부파일 일부가 저장되지 않음
				}
				else
				{
					Adapter.rollback();
					Adapter.UpdateUnit(id, "FAIL", BugUrl, BugPath);
					return -6;											// 에러 발생
				}
			}			
			catch(SQLConnectionException e)
			{
				DB.reconnection();
				continue;
			}
		}
	}

	

	/////////////////////////////////////////////////////////////////
	//  저장 관련로직
	/////////////////////////////////////////////////////////////////
	/**
	 * 버그리포트의 정보를 데이터베이스에 저장하는 것을 담당
	 * @return
	 */
	public boolean saveBugreport(BugReport _report) throws SQLConnectionException {
		
		//DB에 데이터 저장.
		boolean saveState = true;
		
		_report.Reporter.ID = Adapter.saveUser(_report.Reporter);
		if(_report.Reporter.ID <=0) return false;
		
		if(_report.Handler!=null){
			_report.Handler.ID = Adapter.saveUser(_report.Handler);
			if(_report.Handler.ID <=0) return false;
		}
		else
			_report.Handler = new User();
		
		int ret = Adapter.saveBugReport(_report);
		if (ret==-1){
			log.error("Cannot save BugReport ..... ID : "+_report.ID);
			saveState = false;
		}
		
		//버그 노트에 대한 정보 저장.
		if (_report.Bugnotes!=null && _report.Bugnotes.size()!=0)   	
			if(Adapter.saveBugnotes(_report.ID, _report.Bugnotes)==false)	saveState = false;
				
		//Attachment 정보저장
		if (_report.Attachments!=null && _report.Attachments.size()!=0)	
			if(Adapter.saveAttachments(_report.ID, _report.Attachments)==false)	saveState = false;
		
		//History 정보 저장.
		if (_report.Histories!=null && _report.Histories.size()!=0)
			if(Adapter.saveHistories(_report.ID, _report.Histories)==false)	saveState = false;

		//관계 정보 저장.
		if (_report.Relationships!=null && _report.Relationships.size()!=0)	
        	if(Adapter.saveRelationships(_report.Relationships)==false)	saveState = false;
		
        
		return saveState;
	}

}
