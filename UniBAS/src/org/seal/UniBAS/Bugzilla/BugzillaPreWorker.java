package org.seal.UniBAS.Bugzilla;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.seal.UniBAS.Bugzilla.Model.BugHistory;
import org.seal.UniBAS.Bugzilla.Model.BugReport;
import org.seal.UniBAS.Bugzilla.Model.Vote;
import org.seal.UniBAS.Core.Controller;
import org.seal.UniBAS.Core.Database.SQLConnectionException;
import org.seal.UniBAS.Core.Model.SiteInfo;
import org.seal.UniBAS.Util.SerializedList;
import org.seal.UniBAS.Util.Settings;
import org.seal.UniBAS.Util.WorkState;
import org.seal.UniBAS.Util.log;

public class BugzillaPreWorker extends Controller {
	// ///////////////////////////////////////////////////////////////
	// 관리용 관련 멤버변수.
	// ///////////////////////////////////////////////////////////////
	private SiteInfo ThisSite = null;
	private BugzillaDBAdapter Adapter = null;
	private BugzillaPreClient Client = null;

	// ///////////////////////////////////////////////////////////////
	// 작업상태 관련변수
	// ///////////////////////////////////////////////////////////////
	private List<Integer> BugList = null;
	private WorkState State = null;

	// /////////////////////////////////////////////////////////////
	// 생성자
	// ///////////////////////////////////////////////////////////

	public BugzillaPreWorker(Settings _setting) {
		super(_setting);
	}

	// //////////////////////////////////////////////////
	// 작업코드
	// //////////////////////////////////////////////////
	@Override
	public boolean workTarget(SiteInfo _info) {

		// 1. 필요 도구 및 설정 =======================================================
		Adapter = new BugzillaDBAdapter(DB); // Adapter는 예상되는 에러 없음.
		Client = new BugzillaPreClient(_info, Adapter); // Client 생성.
		ThisSite = _info;

		// 필요한 객체들 생성.
		boolean restartOption = false;
		BugList = new SerializedList<Integer>(Settings.it().LOG_PATH
				+ "serializedQueue_" + Settings.it().SYS_NAME + ".txt");
		State = WorkState.getInstance(Settings.it().LOG_PATH + "workstate_"
				+ Settings.it().SYS_NAME + ".txt");

		// 재시작 검토
		// State.setCurrentBugID(680678);
		if (State.getCurrentBugID() > 0)
			restartOption = true;

		// 2. 상태 셋팅=======================================================
		try {
			log.info("Login and Setting ......");
			if (this.WorkProcessLogin() == false)
				return false;
			log.info("Login and Setting                    ... Done.");

			if (restartOption == false) {
				log.info("Preprocessing ......");
				if (this.WorkProcessPrepare() == false)
					return false;
				log.info("Preprocessing                        ... Done.");
			}

			log.info("Crawling ......");
			if (this.WorkProcessOverall() == false)
				return false;
			log.info("Crawling                             ... Done.");
		} catch (Exception e) {
			log.println(e.getMessage());
			log.printStackTrace(e);
			return false;
		}

		SerializedList<Integer> serializedList = (SerializedList<Integer>) BugList;
		serializedList.close();

		return true;
	}

	/**
	 * 버그리포트를 수집하기전 처리할 사항들에 대한 처리함수.
	 * 
	 * @return
	 */
	private boolean WorkProcessPrepare() throws SQLConnectionException {
		// LastBugID구하기
		int LastBugID = Client.getLastBugID();
		if (LastBugID == -1)
			return false;

		State.setLastBugID(LastBugID);
		log.info("Total BugReport is " + LastBugID);
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// 전처리 과정 (로그인, 프로덕트, 컴포넌트 키워드 등..)
	// ///////////////////////////////////////////////////////////////
	/**
	 * 버그리포트를 수집하기전 처리할 사항들에 대한 처리함수.
	 * 
	 * @return
	 */
	private boolean WorkProcessLogin() {
		// 로그인
		boolean ret = Client.login();
		if (ret == false) {
			log.error("Failed to login : \"" + ThisSite.UserID + "\".");
			return false;
		}
		log.info(ThisSite.UserID + " signed in");

		// 타임존 변경 (UTC)
		ret = Client.setTimezone(null);
		if (ret == false) {
			log.error("Failed to change timezone : \"" + ThisSite.UserID
					+ "\".");
			return false;
		}
		log.info("Changed timezone to UTC");
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// 버그리포트 처리 로직
	// ///////////////////////////////////////////////////////////////
	private int GroupSize = 100;

	/**
	 * 버그리포트를 수집하는 전체 과정 감독
	 * 
	 * @return
	 */
	private boolean WorkProcessOverall() {
		int startID = 0;
		int curID = State.getCurrentBugID();
		boolean restart = false;
		if (curID != 0)
			restart = true;

		startID = (curID / GroupSize) * GroupSize;

		boolean flag = true;

		while (startID < State.getLastBugID()) {
			// 그룹의 작업이 끝나면 수집대상 리스트를 재설정
			if (restart == false) {
				if (this.setBugList(startID) == false) {
					flag = false;
					break;
				}
			}
			restart = false; // 재시작을 첫번째만 적용하기위한 코드.

			// 수집대상 리스트에 대한 작업.
			int startIdx = BugList.indexOf(curID); // 시작아이디의 위치얻기.
			if (startIdx < 0)
				startIdx = 0; // 없는 경우 처음부터시작.

			for (int i = startIdx; i < BugList.size(); i++)// Integer id :
															// BugList)
			{
				int bug_id = BugList.get(i);
				State.setCurrentBugID(bug_id); // 작업중인 ID설정

				// 해당 버그리포트에 대하여 처리.
				boolean ret = false;
				try {
					ret = this.workProcessUnit(bug_id);
				} catch (Exception e) {
					log.printStackTrace(e);
					log.error(e.getMessage());
				}

				// 리포트에 대한 처리결과 보고.
				if (ret == true) {
					log.info("BugReport " + bug_id + " analysis ... Done.");
				} else {
					log.info("BugReport " + bug_id + " analysis ... Failed.");
				}
			}

			// 다음 작업을 위한 인덱스 증가.
			startID += GroupSize;
		}

		return flag;
	}

	/** 성능테스트 **/
	private DateTime prev = null;

	private long getDiff() {
		if (prev == null)
			prev = DateTime.now();
		DateTime now = DateTime.now();
		long diffInMillis = now.getMillis() - prev.getMillis();
		prev = now;
		return diffInMillis;
	}

	/**
	 * 한 버그리포트에 대한 처리
	 * 
	 * @param bug_id
	 * @return
	 */
	private boolean workProcessUnit(int bug_id) throws SQLConnectionException {
		String state = "PENDING";
		boolean ret = true;

		BugReport report = null;

		// getDiff();
		// 버그리포트 페이지 분석
		if ((report = makeBugreport(bug_id)) == null)
			ret = false;
		else {
			state = "BUG_SAVED";
		}

		// 버그 히스토리 페이지 분석
		if (ret == true) {
			if (makeHistory(bug_id) == false)
				ret = false;
			else {
			}
		}

		// 투표 페이지 분석 및 저장.
		if (ret == true) {
			if (makeVote(bug_id, report.Votes) == false)
				ret = false;
			else {
				state = "VOTE_SAVED";
			}
		}

		if (ret == true) {
			state = "DONE";
		}

		return ret;
	}

	/**
	 * 버그리포트 수집 대상목록을 갱신함.
	 * 
	 * @param _startIdx
	 * @return
	 */
	private boolean setBugList(int _startIdx) {
		BugList.clear();

		// 수집할 버그리스트의 아이디들을 가져옴
		List<Integer> list = Client.getList(_startIdx, _startIdx + GroupSize);
		if (list == null)
			return false;
		Collections.sort(list);
		BugList.addAll(list);

		return true;
	}

	/**
	 * 버그리포트를 분석하여 저장한다.
	 * 
	 * @param bug_id
	 * @param _report
	 * @return
	 */
	private BugReport makeBugreport(int bug_id) throws SQLConnectionException {

		BugReport report = null;

		// 리포트 페이지 가져오기.
		report = Client.getBugreport(bug_id);
		if (report == null)
			return null;

		return report;
	}

	/**
	 * 버그리포트 히스토리 추출을 하는 컨트롤 코드
	 * 
	 * @param _url
	 * @return
	 */
	private boolean makeHistory(int _bugID) throws SQLConnectionException {
		// 버그리포트 History 페이지 분석.
		List<BugHistory> histories = null;
		histories = Client.getHistories(_bugID);
		if (histories == null)
			return false;
		return true;
	}

	/**
	 * 투표 정보 추출을 하는 컨트롤 코드
	 * 
	 * @param _bugID
	 * @param
	 * @return
	 */
	private boolean makeVote(int _bugID, int _count)
			throws SQLConnectionException {

		if (_count == 0) {
			Adapter.update_voteSaved(_bugID);
			return true;
		}

		// 버그리포트 History 페이지 분석.
		List<Vote> votes = null;
		votes = Client.getVotes(_bugID);

		// 히스토리 정보 DB에 저장.
		if (votes == null)
			return false;
		return true;
	}

}
