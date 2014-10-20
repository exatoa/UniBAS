package org.sel.UnifiedBTS.Trac;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.sel.UnifiedBTS.Core.Model.SiteInfo;
import org.sel.UnifiedBTS.Core.Network.WebCacheFile;
import org.sel.UnifiedBTS.Trac.Model.BugReport;
import org.sel.UnifiedBTS.Trac.Model.TracError;
import org.sel.UnifiedBTS.Util.Config;
import org.sel.UnifiedBTS.Util.DateUtil;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.log;

public class TracClient {

	/////////////////////////////////////////////////////////////////
	//  관리용 멤버변수들.
	/////////////////////////////////////////////////////////////////
	//private SiteInfo ThisSite;					//수집대상 사이트 정보
	//private MantisDBAdapter Adapter = null;		//DB 어댑터
	private TracParser Parser = null;			//파서
	private WebCacheFile Web = null; 			//웹에 파일 요청 클래스
	
	
	private String url = null;
	private String strHttp = null;
	
	
	private int MAX_RETRY = 5;
	
	/////////////////////////////////////////////////////////////////
	//  사이트 관련 URL
	/////////////////////////////////////////////////////////////////
	private String baseURL = "http://trac.edgewall.org/";	
	private String reportURL = "ticket/{0}";
	
	///////////////////////////////////////////////////////////////
	// 생성자 및 외부 노출 메서드
	/////////////////////////////////////////////////////////////
	
	public TracClient(SiteInfo _info) {
		Parser = new TracParser();					//Parser 예상되는 에러 없음
		Web = WebCacheFile.getInstance();
		
		baseURL = _info.BaseUrl;
		if (baseURL.charAt(baseURL.length()-1)!='/')
			baseURL = baseURL + "/";
	}
	
	
	
	
	
	/////////////////////////////////////////////////////////////////
	//  Getter 정의
	/////////////////////////////////////////////////////////////////

	/*--------URL 반환함수 ---------------*/

	private String getReportURL(int _bugID)
	{
		return baseURL + reportURL.replaceFirst("\\{0\\}", Integer.toString(_bugID));
	}
		
	/////////////////////////////////////////////////////////////////
	//  전처리 관련 코드
	/////////////////////////////////////////////////////////////////

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
			//에러 체크.
			TracError error = Parser.checkNotExists(strHttp);
			if(error==null) break;
			else if(error.Code==-1) continue;
			else
			{
				report = new BugReport();
				report.ID = error.Code;
				report.Date = error.Msg;
				return report;
			}
		
		}while(++retry<MAX_RETRY);
		if(retry>=MAX_RETRY){
			log.error("BugReport page cannot download" + url);
			return null;
		}

		//버그 아이디 분석.
		report = Parser.getTicketID(strHttp);
		if (report==null) return null;

		return report;
	}

}
