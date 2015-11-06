package org.seal.UniBAS.Bugzilla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.seal.UniBAS.Bugzilla.Model.BugHistory;
import org.seal.UniBAS.Bugzilla.Model.BugReport;
import org.seal.UniBAS.Bugzilla.Model.Product;
import org.seal.UniBAS.Bugzilla.Model.PropertyItem;
import org.seal.UniBAS.Bugzilla.Model.Vote;
import org.seal.UniBAS.Core.Controller;
import org.seal.UniBAS.Core.Database.SQLConnectionException;
import org.seal.UniBAS.Core.Model.SiteInfo;
import org.seal.UniBAS.Util.SerializedList;
import org.seal.UniBAS.Util.Settings;
import org.seal.UniBAS.Util.TextUtil;
import org.seal.UniBAS.Util.WorkState;
import org.seal.UniBAS.Util.log;




public class BugzillaWorker extends Controller{
	/////////////////////////////////////////////////////////////////
	//  관리용 관련 멤버변수.
	/////////////////////////////////////////////////////////////////
	private SiteInfo ThisSite = null;
	private BugzillaDBAdapter Adapter = null;
	private BugzillaClient Client = null;
	
	

	
	/////////////////////////////////////////////////////////////////
	//  작업상태 관련변수
	/////////////////////////////////////////////////////////////////
	private List<Integer> BugList = null;
	private WorkState State = null;  
	
	
	///////////////////////////////////////////////////////////////
	// 생성자 
	/////////////////////////////////////////////////////////////

	public BugzillaWorker(Settings _setting) {
		super(_setting);
	}


	////////////////////////////////////////////////////
	//  작업코드
	////////////////////////////////////////////////////
	@Override
	public boolean workTarget(SiteInfo _info) {
		
		//1. 필요 도구 및 설정 =======================================================
		Adapter = new BugzillaDBAdapter(DB);			//Adapter는 예상되는 에러 없음.
		Client = new BugzillaClient(_info, Adapter);				//Client 생성.
		ThisSite = _info;
		
		//필요한 객체들 생성.
		boolean restartOption = false;
		BugList = new SerializedList<Integer>(Settings.it().LOG_PATH +"serializedQueue_"+Settings.it().SYS_NAME+".txt");
		State = WorkState.getInstance(Settings.it().LOG_PATH +"workstate_"+Settings.it().SYS_NAME+".txt");
		
		//재시작 검토
		if(State.getCurrentBugID()>0) restartOption = true;
		//if(BugList.size()!=0) restartOption = true;
		//restartOption = this.checkRestart();

		
		//2. 상태 셋팅=======================================================		
		try{
			log.info( "Login and Setting ......");
			//if(this.WorkProcessLogin()==false) return false;
			log.info("Login pass!!!!!!");
			log.info( "Login and Setting                    ... Done.");
			
			if(restartOption==false)
			{
				log.info( "Preprocessing ......");
				if(this.WorkProcessPrepare()==false) return false;
				log.info( "Preprocessing                        ... Done.");
			}
			else
			{
				//log.info("*********** Old Last BugID :" + State.getLastBugID());
				//이전에 작업 후 에러가 발생했던 버그항목들 재처리.
				log.info( "Reviving ......");
				if(WorkProcessRevive()==false){
					log.info( "Reviving                             ... Failed.");
					return false;
				}
				log.info( "Reviving                             ... Done.");
			}
			
//			if(restartOption==true)
//			{
//				log.info( "Crawling LastWork......");
//				if(this.WorkProcessLastWork()==false) return false;
//				log.info( "Crawling LastWork                    ... Done.");
//			}
			//State.setLastBugID(500);
						
			log.info( "Crawling ......");
			if(this.WorkProcessOverall()==false) return false;
			log.info( "Crawling                             ... Done.");			
		}
		catch (Exception e) {
			log.println(e.getMessage());
			log.printStackTrace(e);
			return false;
		}

		
		SerializedList<Integer> serializedList = (SerializedList<Integer>)BugList;
		serializedList.close();
		
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
		//프로덕트에 대한 정보 수집
		List<Product> products = Client.getProducts();
		if(products==null)	return false;
		log.info( "Saved "+ products.size() +" products.");
		
		
		//Component에 대한 정보 수집
		int cnt = 0;
		for(Product p : products)
		{
			p.ID = Adapter.saveProduct(-1, p);
			List<PropertyItem> components = Client.getComponent(p.Name);
			
			if(components!=null)	Adapter.saveComponents(p.ID, components);
			log.info( "Saved "+components.size()+" components of Product "+p.Name+".");
			
			cnt += components.size();
		}
		log.info( "Saved components. (TotalCount = "+ cnt+")");
				
		//Keyword에 대한 정보 수집
		List<PropertyItem> keywords = Client.getKeywords();
		if(keywords==null) return false;	
		Adapter.saveKeywords(keywords);
		log.info( "Saved "+keywords.size()+" keywords");	
		
		
		//LastBugID구하기
		int LastBugID = Client.getLastBugID();
		if (LastBugID==-1) return false;
		
		State.setLastBugID(LastBugID);
		log.info("Total BugReport is "+LastBugID);				
		return true;
	}
	
	
	
	/**
	 * 기존에 실패한 버그리포트들에 대해 재작업.
	 * @return
	 */
	private boolean WorkProcessRevive() {
		
		//DB에 실패한 버그리포트들을 가져온다.
		List<Integer> ids = getRevivalIDList();
		
		int curID = State.getCurrentBugID();
		for(int i=0; i<ids.size(); )
		{
			if(ids.get(i)>=curID) ids.remove(i);
			else i++;
		}
		
		//재수집을 수행		
		for(Integer id : ids)
		{
			//해당 버그리포트에 대하여 처리.
			boolean ret = false;
			try{
				ret = this.workProcessUnit(id);	
			}
			catch(Exception e)
			{
				log.printStackTrace(e);
				log.error(e.getMessage());
			}
			
			//리포트에 대한 처리결과 보고.
			if(ret==true)
			{
				log.info( "Revival : BugReport "+id+" analysis ... Done.");
			}
			else
			{
				log.info( "Revival : BugReport "+id+" analysis ... Failed.");
			}
		}
		
		return true;
	}

	
	/**
	 * DB에서 받아온 문자열 ID 리스트를 List<Integer> 로 변경
	 * @return
	 */
	public List<Integer> getRevivalIDList(){
		
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
	private int GroupSize = 100;

	
	/**
	 * 버그리포트를 수집하는 전체 과정 감독
	 * @return
	 */	
	private boolean WorkProcessOverall()
	{			
		int startID = 0;
		int curID = State.getCurrentBugID();
		boolean restart = false;
		if(curID != 0) restart = true;
		
		startID = (curID/GroupSize) * GroupSize;
		
		boolean flag = true;
		
		while(startID < State.getLastBugID())
		{
			//그룹의 작업이 끝나면  수집대상 리스트를 재설정
			if(restart==false)
			{				
				if(this.setBugList(startID)==false)
				{
					flag = false;
					break;
				}
			}
			restart = false;	//재시작을 첫번째만 적용하기위한 코드.
			

			//수집대상 리스트에 대한 작업.
			int startIdx = BugList.indexOf(curID); 	//시작아이디의 위치얻기.
			if (startIdx <0) startIdx = 0;			//없는 경우 처음부터시작.
			
			for(int i = startIdx; i < BugList.size(); i++)//Integer id : BugList)
			{
				int bug_id = BugList.get(i);
				State.setCurrentBugID(bug_id);		//작업중인 ID설정
			
				//해당 버그리포트에 대하여 처리.
				boolean ret = false;
				try{
					ret = this.workProcessUnit(bug_id);	
				}
				catch(Exception e)
				{
					log.printStackTrace(e);
					log.error(e.getMessage());
				}
				
				//리포트에 대한 처리결과 보고.
				if(ret==true){
					log.info( "BugReport "+bug_id+" analysis ... Done.");
				}
				else{
					log.info( "BugReport "+bug_id+" analysis ... Failed.");
				}
			}
			
			//다음 작업을 위한 인덱스 증가.
			startID += GroupSize;
		}
		
		return flag;
	}
	
	

	/**성능테스트**/
	private DateTime prev = null;
	private long getDiff()
	{
		if(prev==null) prev = DateTime.now();
		DateTime now = DateTime.now();
		long diffInMillis =  now.getMillis() - prev.getMillis();
		prev = now;
		return diffInMillis;		
	}

	/**
	 * 한 버그리포트에 대한 처리
	 * @param bug_id
	 * @return
	 */
	private boolean workProcessUnit(int bug_id) throws SQLConnectionException
	{
		String reportURL = "";
		String reportPath = "";
		String histURL = "";
		String histPath = "";
		String voteURL = "";
		String votePath = "";
		String state = "PENDING";
		boolean ret = true;
		
		while(true)
		{
			try{
				Adapter.setTransaction();
				BugReport report = null;
				
				//getDiff();				
				//버그리포트 페이지 분석		
				if((report = makeBugreport(bug_id))==null) ret= false;
				else{
					state = "BUG_SAVED";
					reportURL = Client.getLastUrl();
					reportPath = TextUtil.convertURLtoPath(reportURL);
				}
				//log.info("WorkingTime - BugReport : "+getDiff());	
				
				//버그 히스토리 페이지 분석
				if(ret==true){
					if(makeHistory(bug_id)==false) ret= false;
					else{
						state = "HIST_SAVED";
						histURL = Client.getLastUrl();
						histPath = TextUtil.convertURLtoPath(histURL);
					}
				}
				//log.info("WorkingTime - History : "+getDiff());
				
				//투표 페이지 분석 및 저장.
				if(ret==true){
					if(makeVote(bug_id, report.Votes)==false) ret= false;
					else{
						state = "VOTE_SAVED";
						voteURL = Client.getLastUrl();
						votePath = TextUtil.convertURLtoPath(voteURL);
					}
				}
				//log.info("WorkingTime - Vote : "+getDiff());
				
				if(ret==true){
					state = "DONE";
					Adapter.commit();
				}
				else
				{
					Adapter.rollback();
				}
		
				Adapter.update_BugInfo(bug_id, state, reportURL, reportPath, histURL, histPath, voteURL, votePath);
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
	 * 버그리포트 수집 대상목록을 갱신함.
	 * @param _startIdx
	 * @return
	 */
	private boolean setBugList(int _startIdx)
	{
		BugList.clear();
		 
		//수집할 버그리스트의 아이디들을 가져옴
		List<Integer> list = Client.getList(_startIdx, _startIdx+GroupSize);
		if(list==null) return false;
		Collections.sort(list);
		BugList.addAll(list);
		 
		while(true)
		{
			try{
				//DB에 수집상태 반영
				String path = TextUtil.convertURLtoPath(Client.getLastUrl());
				int listID = Adapter.addList("", 0, Client.getLastUrl(), path);
				if (listID<=0) return false;
								
				// 수집대상 버그리포트 ID들 DB에 추가
				String delimiter = ",";
				String strID = "";
				for(Integer item : BugList)	strID += item + delimiter;
				Adapter.add_BugList(listID, delimiter, strID);
			}
			catch(SQLConnectionException e)
			{
				DB.reconnection();
				continue;
			}
			catch(Exception e)	//UnknownHostException 이 던져지는데...이놈을 잡을 수가 없네;;
			{
				if(e instanceof java.net.UnknownHostException){
					log.error("java.net.UnknownHostException Occured!");
					DB.reconnection();
					continue;
				}else{
					log.error("Unknown Error occured!");
					log.printStackTrace(e);
					return false;
				}		
			}
			break;
		}

		return true;
	}

	
	/**
	 * 버그리포트를 분석하여 저장한다.
	 * @param bug_id
	 * @param _report 
	 * @return
	 */
	private BugReport makeBugreport(int bug_id) throws SQLConnectionException {
		
		BugReport report = null;
		
		//리포트 페이지 가져오기.
		report = Client.getBugreport(bug_id);
		if(report==null) return null;
		
		//버그리포트 페이지 저장
		if(report!=null && saveBugReport(report)==false) report= null;
		
		return report;
	}


	/**
	 * 버그리포트의 모든 정보를 데이터베이스에 저장.
	 * @param report
	 * @return
	 */
	private boolean saveBugReport(BugReport report) throws SQLConnectionException
	{
		boolean saveState = true; 
		report.Reporter.ID =Adapter.saveUser(report.Reporter);
		if(report.Assignee!=null) 	report.Assignee.ID = Adapter.saveUser(report.Assignee);
		if(report.QA!=null) 		report.QA.ID = Adapter.saveUser(report.QA);
		
		if(report.Classification !=null)	report.Classification.ID = Adapter.saveClassification(report.Classification);
			
		if(report.Product !=null) 			report.Product.ID   = Adapter.saveProduct(report.Classification.ID, report.Product);
		if(report.Component !=null)			report.Component.ID = Adapter.saveComponent(report.Product.ID, report.Component);
		
		
		int ret = Adapter.saveBugReport(report);
		if (ret==-1){
			log.error("Cannot save BugReport ..... ID : "+report.BugID);
			saveState = false;
		}
					
		if (report.Bugnotes.size()!=0)   	
			if(Adapter.saveComments(report.BugID, report.Bugnotes)==false)	saveState = false;
		
		if (report.Attachments.size()!=0)	
			if(Adapter.saveAttachments(report.BugID, report.Attachments)==false)	saveState = false;
		
		if (report.CCList.size()!=0){
        	if(Adapter.saveCC(report.BugID, report.CCList)==false)	saveState = false;	        	
        }
        	
        if (report.SeeAlsos.size()!=0)
        	if(Adapter.saveSeeAlsos(report.BugID, report.SeeAlsos)==false)	saveState = false;
        
        if (report.Relationships.size()!=0)	
        	if(Adapter.saveRelationship(report.Relationships)==false)	saveState = false;
		
        
        return saveState;
	}


	
	/**
	 * 버그리포트 히스토리 추출을 하는 컨트롤 코드
	 * @param _url
	 * @return
	 */
	private boolean makeHistory(int _bugID) throws SQLConnectionException
	{
		//getDiff();
		
		//버그리포트 History 페이지 분석.
		List<BugHistory> histories =null;
		histories = Client.getHistories(_bugID);
		//log.info("WorkingTime - History analysis : "+getDiff());	
		
		//히스토리 정보 DB에 저장.
		if(histories!=null && Adapter.saveHistories(_bugID, histories)==false)	
			return false;
		//log.info("WorkingTime - History save : "+getDiff());	
		
		//상태 업데이트  : 다운로드 완료.
		if(Adapter.update_histSaved(_bugID)<0) return false;
		
		
		return true;
	}
	
	

	/**
	 * 투표 정보 추출을 하는 컨트롤 코드
	 * @param _bugID
	 * @param  
	 * @return
	 */
	private boolean makeVote(int _bugID, int _count) throws SQLConnectionException {
		
		if(_count==0)
		{
			Adapter.update_voteSaved(_bugID);
			return true;
		}			
		
		//버그리포트 History 페이지 분석.
		List<Vote> votes = null;
		votes = Client.getVotes(_bugID);
		//log.info("WorkingTime - Vote analysis : "+getDiff());	
		
		//히스토리 정보 DB에 저장.
		if(votes!=null && Adapter.saveVotes(_bugID, votes)==false)	
			return false;
		//log.info("WorkingTime - Vote save : "+getDiff());	
	
		//상태 업데이트  : 다운로드 완료.
		if(Adapter.update_voteSaved(_bugID)<0) return false;
		
		return true;
	}
	
	

	
	/////////////////////////////////////////////////////////////////
	//  유틸 메소드
	/////////////////////////////////////////////////////////////////
	private String getStringFromList(List<Integer> _list, int _start, int _size)
	{
		String strList = "";
		
		int max = _list.size();
		for(int i=_start; i<_start+_size; i++)
		{
			if(i>=max) break;
			int targetID = _list.get(i);
			if(targetID==-1) continue;
			strList += ","+Integer.toString(targetID);
		}
		
		if(strList.length()>0)	return strList.substring(1);
		
		return null;
	}
	
}
