package test.unibas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.seal.UniBAS.Mantis.Model.BugReport;
import org.seal.UniBAS.Mantis.MantisException;
import org.seal.UniBAS.Mantis.MantisParser;
import org.seal.UniBAS.Util.Settings;
import org.seal.UniBAS.Util.log;
import org.seal.UniBAS.Core.Network.WebCacheFile;


public class TestMantis {
	/**
	 * 테스트 가동을 위한 환경
	 * @param args
	 */
	public static void main(String[] args) {
		new TestMantis().run();
	}
	
	/**
	 * 테스트 실행
	 */
	public void run()
	{
		SetConfig();
		
		
		AnalysisTest();
	}
	

	
	/**
	 * 기본적인 베이스 변수들 선언.
	 */
	private Settings config = null;
	private MantisParser Parser = null;
	private WebCacheFile Web = null;


	
	/**
	 * 테스트를 위한 환경설정
	 * @return
	 */
	private void SetConfig()
	{
		//기본 config생성.
		config = Settings.getInstance();
		config.SYS_NAME = "TestMantis_Site";
		config.SYS_URL = "http://bugs.scribus.net/";
		//config.BASE_URL = "https://mantis.phplist.com/";
		//config.BASE_URL = "http://www.mantisbt.org/bugs/";
		//config.BASE_URL = "http://mantis.doering-thomas.de/";
		//config.BASE_URL = "http://mantis.pikatech.com/";
		config.SYS_ID = "forglee";
		config.SYS_PW = "Sel535447";
	}
	
	/**
	 * 테스트 대상 리포트.
	 */

	private static int[] ids ={5397, 11703};//8268, 8699, 9071, 9420, 9646, 9689, 9695, 9711, 9764, 9793, 10004, 10129, 10333, 10339, 10342, 10343, 10344, 10551, 10552, 10577, 10589, 10601, 10602, 10603, 10604, 10605, 10606, 10737, 10822, 10893, 10894, 10896, 10897, 10902, 10904, 10911, 10912, 10913, 10914, 10915, 10921, 10938, 10939, 10940, 10941, 10942, 10949, 10950, 10951, 10956, 10957, 10958, 10959, 10960, 10961, 10967, 10980, 10981, 10982, 10983, 10988, 10989, 11000, 11003, 11004, 11005, 11006, 11007, 11012, 11021, 11022, 11024, 11030, 11031, 11068, 11079, 11085, 11086, 11088, 11089, 11090, 11093, 11098, 11103, 11147, 11191, 11192, 11193, 11194, 11195, 11196, 11197, 11198, 11199, 11200, 11205, 11222, 11229, 11241, 11263, 11264, 11265, 11266, 11267, 11268, 11269, 11270, 11315, 11403, 11422, 11425, 11441, 11492, 11514, 11583, 11637, 11700, 11756, 11822, 11827, 11836, 11837, 11838, 11841, 11842, 11846, 11906, 12046, 12350};
	//private static int[] ids ={13187, 13876, 13877, 14029, 14115, 14762, 15125, 15151, 15162, 15186, 15355, 15383, 15414, 15423, 15479, 15484, 15490, 15491, 15493, 15495, 15496, 15497, 15498, 15531, 15535, 15565, 15606, 16627, 16708, 16783, 16788, 16861, 17237, 17243, 17244, 17246};
	//private static int[] ids ={681, 709, 2668, 2928, 3424, 3929, 3948, 4044, 4061, 4062, 4220, 4269, 4428, 4465, 4528, 4626, 4661, 4662, 4736, 5887, 7042, 7055, 7073, 7076, 7240, 7606, 7729, 8199, 8381, 8596, 9266, 9683, 9991, 10153, 10505, 10628, 10900, 10914, 11031, 11269, 11272, 11415, 11808, 11826, 11912, 12281, 12533, 12538, 12545, 12607, 12645, 12723, 12750, 12754, 12789, 12869, 12890, 13033, 13060, 13065, 13077, 13107, 13122, 13163, 13304, 13309, 13450, 13477, 13488, 13615, 13622, 13630, 13701, 13705, 13706, 13751, 14040, 14102, 14153, 14219, 14234, 14258, 14276, 14305, 14350, 14364, 14382, 14416, 14426, 14430, 14439, 14481, 14572, 14588, 14713, 14716, 14799, 14813, 14814, 14815, 14825, 14866, 14889, 15207, 15412, 15413, 15438, 15499, 15526, 15542, 15549, 15577, 15578, 15579, 15580, 15673, 15684, 15892, 15919, 15941, 16039, 16062, 16091, 16147, 16243, 16327, 16385, 16395, 16427, 16464, 16471, 16497, 16510, 16558, 16559, 16642, 16920, 16988, 16996, 17089, 17125, 17126, 17425, 17426, 17427, 17428, 17429, 17430, 17431, 17432, 17433, 17434, 17435, 17436, 17439, 17452, 17453};
	//private static int[] ids ={1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171};
	//private static int[] ids ={155, 236, 475, 476, 854, 862, 901, 1054, 1237, 9160, 9579, 13876, 13877, 15179, 15357, 15516, 16627, 16639, 16811, 16827};
	
	
	/**
	 * 버그리포트를 가져와서 파싱해보는 테스트.
	 */
	private void AnalysisTest()
	{
		
		if (PrepareTest()==false){
			log.info("Fail to prepare");
			return;
		}
		
		if (LoginTest()==false){
			log.info("Fail to login");
			return;
		}
		
		int i=0;
		for(i=0; i<ids.length; i++)
		{
			BugReport report = bugUnitTest(ids[i]);
			if(report==null){
				log.error("Report "+ids[i]+" : analysis Error;");
				break;				//버그리포트 분석중 에러 발생
			}
		
			//작업결과 반영.
			switch(report.ID)
			{
			
			case 0:	//접근권한이 없음
				log.error("Report "+ids[i]+" : Access Denied.");
				break;
								
			case -1:	//버그리포트가 없음 (삭제됨)
				log.info("Report "+ids[i]+" : Not found Issue");
				break;
				
			case -2:
				log.error("Report "+ids[i]+" : Project deleted. Not found Issue.");
				break;
			
			case -3:
				log.error("Report "+ids[i]+" : Unknown Application error..");
				break;

			default :	//성공
				log.info("Report "+ids[i]+" : Done!");
				break;
			}
		}
		
		if(i==ids.length)
			log.info("Done!");
		else
			log.info("Fail!");

	}
	

		
		
	private BugReport bugUnitTest(int _id)
	{
		String url = getReportURL(_id);
		String strHttp = Web.getBody(url);
		if (strHttp==null) return null;
		
		log.info("Bug : "+ url);
		BugReport report = Parser.analysisBugreport(strHttp);
		if(report==null){
			
			MantisException e = Parser.checkReportError(strHttp);
			if (e!=null){
				log.warn(e.getMessage());
				if(e.ErrorCode==1100) return new BugReport(-1);			//삭제된 버그리포트
				else if(e.ErrorCode==700) return new BugReport(-2);  	//프로젝트가 삭제됨
				else return new BugReport(-3);							//그외의 에러
			}
			else if(Parser.checkPrivateReport(strHttp)==true) return new BugReport(0);	//비공개 버그리포트.
			else return null;		//그외의 에러.
		}

		return report;
	}
	
	
	
	/////////////////////////////////////////////////////////
	// 테스트에 필요한 전처리 함수들
	///////////////////////////////////////////////////////


	/**
	 * 테스트에 필요한 베이스 변수들에 값을 할당.
	 * @return
	 */
	private boolean PrepareTest()
	{

		
		try {
			log.init(config.LOG_PATH + "log_"+config.SYS_NAME+".txt");
		} catch (IOException e){ 
			log.printStackTrace(e);
			return false;
		}
		
		//웹설정
		Web = WebCacheFile.getInstance();
		Web.setLocalProxy(config.CACHE_PATH);
		Web.setLimitation(config.LIMIT_COUNT, config.WAIT_MINUTE);
		Web.setDownloadSpeed(config.DOWN_SPEED);
		
		//파싱작업
		Parser = new MantisParser();

		return true;
	}

	
		
	/**
	 * 사이트에 로그인이 되는지 테스트.
	 * @return
	 */
	private boolean LoginTest()
	{
		//로그인
		boolean ret =  login();
		if(ret==false){
			log.error("Failed to login : \""+ config.SYS_ID + "\".");
			return false;
		}
		log.info( config.SYS_ID + " signed in");
		
		
		//타임존 변경 (UTC)
		ret =  setTimezone(null);
		if(ret==false){
			log.error("Failed to change timezone : \""+ config.SYS_ID + "\".");
			return false;
		}
		log.info( "Changed timezone to UTC");	
		return true;
	}
	
	/**
	 * 타임존의 설정을 셋팅함.
	 * @param object
	 * @return
	 */
	private boolean setTimezone(Object object) {
		
		String strHttp;
		String url = config.SYS_URL + "account_prefs_page.php";
		//환경설정을 읽어오기 위해 웹페이지를 가져옴.
		strHttp = Web.getBody(url);	//새로 다운로드 필수.
		if(strHttp==null){
			return false;
		}
		
		//변경할 환경변수 지정.
		log.info(strHttp);
		String prefToken = Parser.getPrefenceToken(strHttp);
		String userID = Parser.getPrefenceUserID(strHttp);
		
		if(prefToken==null || userID==null) return false;
		
		//설정한 결과 반영.
		strHttp = Web.getBody(url,this.getPreferenceParam(prefToken, userID));
		if(strHttp==null){
			return false;
		}
		
		//설정 검증.
		return Parser.checkPreferenceHtml(strHttp);
	}

	
	/////////////////////////////////////////////////////////
	// 테스트에 필요한 보조 함수들 (다른 클래스의 복사본)
	///////////////////////////////////////////////////////

	private String getReportURL(int _id)
	{
		String reportURL = "view.php?id={0}";
		return config.SYS_URL + reportURL.replaceFirst("\\{0\\}", Integer.toString(_id));
	}

	/**
	 * 로그인에 필요한 파라메터를 생성하여 반환
	 * @param _id
	 * @param _pw
	 * @return
	 */
	private Map<String, String> getLoginParam(String _id, String _pw) {
		Map<String, String> params = new HashMap<String, String>();
		
		params.put("username", _id);
		params.put("password", _pw);
		params.put("return", "index.php");
		
		return params;
	}
	
	/**
	 * 환경설정 변경을 위한 파라메터 생성
	 * @param _token
	 * @param _id
	 * @return
	 */
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
	/**
	 * 웹에 로그인을 실제로 진행.
	 * @return
	 */
	public boolean login()
	{
		String url = config.SYS_URL + "login.php";
		Map<String, String> params = this.getLoginParam(config.SYS_ID, config.SYS_PW);
		
		//로그인 결과 페이지 다운로드.
		String strHttp = Web.getBody(url, params);
		if(strHttp==null){
			return false;
		}
		
		//로그인 결과 검증
		if(Parser.checkLoginHtml(strHttp)==false) return false;

		return true;
	}
	
}
