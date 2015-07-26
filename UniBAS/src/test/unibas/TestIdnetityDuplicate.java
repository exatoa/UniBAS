package test.unibas;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

import org.seal.UniBAS.Util.TextUtil;
import org.seal.UniBAS.Util.log;



public class TestIdnetityDuplicate {

	public static void main(String[] args) {
	
		try {
			log.init("E:\\_temp\\BTS\\duplicate\\log.txt");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		try
		{
			TestIdnetityDuplicate test = new TestIdnetityDuplicate();
			test.CachePath = "E:\\_temp\\BTS\\duplicate\\cache\\";
			test.setBaseURL("https://bugzilla.mozilla.org/");
			test.run(0, 1060000);
			
			test.delete();
			
		} catch (IOException e) {
			log.error("예기치 않은 파일 에러 발생.");
			log.printStackTrace(e);
		}
		
		
		log.info("Done!");
	}

	
	
	


	/////////////////////////////////////////////////////////////////
	//  사이트 관련 URL
	/////////////////////////////////////////////////////////////////
	public String baseURL = null;
	private String listURL = "buglist.cgi?query_format=advanced&columnlist=opendate&f1=bug_id&o1=greaterthan&v1={0}&f2=bug_id&o2=lessthaneq&v2={1}";
	
	private String reportURL = "show_bug.cgi?id={0}&ctype=xml";
	private String activityURL = "show_activity.cgi?id={0}";
	private String voteURL = "page.cgi?id=voting/bug.html&bug_id={0}";

	
	////////////////////////////////////////////
	//  Getter Setter 정의0
	////////////////////////////////////////////
	public boolean setBaseURL(String _url)
	{
		baseURL = _url;
		if (baseURL.charAt(baseURL.length()-1)!='/')
			baseURL = baseURL + "/";
		
		return true;
	}
	

	public String getListURL(int _sID, int _eID) {
		String ret;
		ret = listURL.replaceFirst("\\{0\\}", Integer.toString(_sID));
		ret = ret.replaceFirst("\\{1\\}", Integer.toString(_eID));
		
		return baseURL + ret;
	}
	
	public String getReportURL(int _bugID)
	{
		return baseURL + reportURL.replaceFirst("\\{0\\}", String.valueOf(_bugID));
	}
	
	
	public String getVotesURL(int _bugID)
	{
		return baseURL + voteURL.replaceFirst("\\{0\\}", String.valueOf(_bugID));
	}
	
	public String getActivityURL(int _bugID)
	{
		return baseURL + activityURL.replaceFirst("\\{0\\}", String.valueOf(_bugID));
	}
	
	
	
	public String CachePath = null;
	public int CacheNamesize = 2;
	public int CacheLevel = 2;
	private int del_count = 0;
	private int point = 1000;
	
	////////////////////////////////////////////
	//  Test Work
	////////////////////////////////////////////
	public boolean run(int _start, int _end) throws IOException
	{
		if (CachePath == null) return false;
		String url;
		int count = 0;
		
		//리스트에 대한 테스트.[완료]
		log.info("[list] Start Test");
		count = 0;
		for(int i=_start; i<=_end; i+=100)
		{
			url = getListURL(i, i+100);
			if(DuplicateCheck(url)==false)
				log.error("[list] Catch Duplicate start with "+ i);
			else
				if((++count)%point==0) log.info("[list] No Duplicate" + i);
		}
		
		//BugreportTest[완료]
		log.info("[Report] Start Test");
		count = 0;
		for(int i=_start; i<=_end; i++)
		{
			url = getReportURL(i);
			if(DuplicateCheck(url)==false)
				log.error("[Report] Catch Duplicate start with "+ i);
			else
				if((++count)%point==0) log.info("[Report] No Duplicate" + i);
		}

		//VotesTest
		log.info("[Votes] Start Test");
		count = 0;
		for(int i=_start; i<=_end; i++)
		{
			url = getVotesURL(i);
			if(DuplicateCheck(url)==false)
				log.error("[Votes] Catch Duplicate start with "+ i);
			else
				if((++count)%point==0) log.info("[Votes] No Duplicate" + i);
		}
		
		//ActivityTest
		log.info("[Activity] Start Test");
		count = 0;
		for(int i=_start; i<=_end; i++)
		{
			url = getActivityURL(i);
			if(DuplicateCheck(url)==false)
				log.error("[Activity] Catch Duplicate start with "+ i);
			else
				if((++count)%point==0) log.info("[Activity] No Duplicate" + i);
		}
	
		return true;
	}
	
	public boolean DuplicateCheck(String url) throws IOException
	{
		String hash;
		String path;
		File file;
		boolean ret = true; 

		hash = TextUtil.convertURLtoPath(url);		//상대경로 구함.
		path = TextUtil.makeBucket(CachePath + hash,CacheNamesize, CacheLevel); //로컬에 있으면 로컬 데이터 반환.
		
		file = new File(path);
		if(file.exists()==true){
			ret = false;
		}
		else{
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		return ret;
	}
	

	public boolean delete() {
		boolean ret = true;
		log.info("[Delete] deleting files....");
		
		ret = delete(new File(CachePath));
				
		if(ret==true){
			log.info("[Delete] deleting files....Done.");
		}
		else
			log.info("[Delete] deleting files....Failed.");
		return ret;
	}


	
	private boolean delete(File _file)  
	{
		boolean ret = true;
		try{
			File[] list = _file.listFiles();
		
			for(File f : list)
			{
				//디렉토리는 재귀.
				if(f.isDirectory()==true)
				{
					if(delete(f)==true){
						Files.delete(f.toPath());	//부모도 삭제.
						del_count++;
					}
					else
						ret = false;
				}
				else{
					Files.delete(f.toPath());
					del_count++;
					
				}
				if((del_count%point)==0) log.info("[delete] deleting files "+ del_count);
			}
		}
		catch(IOException e)
		{
			ret =  false;
		}
		return ret;
	}
}
