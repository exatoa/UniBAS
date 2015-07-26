package org.sel.UnifiedBTS.Z;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.log;

class Extract {
	
	//Setting 변수들
	private static String OUTPUT_PATH = "E:\\_Research\\2015_IEICE\\Extract\\bug_july\\";
	private static String LogPath = "E:\\_Research\\2015_IEICE\\Extract\\log.txt";
	private static String LocalProxyPath = "E:\\_Research\\2014_UniBAS\\_cache\\";		//실제 파일이 있는 경로
	
	
	//입력파일 설정
	private static String INPUT_LIST = "E:\\_Research\\2015_IEICE\\Extract\\list_july.txt"; //출력할 파일 목록(버그 아이디리스트)
	private static int HASH_DEPTH = 2; 	//입력 파일의 hash path Level이 몇 단계로 되어있는지 결정
	private static int HASH_SIZE = 2; 		//입력 파일의 hash path Level의 길이
	private String URL_base = "https://bugzilla.mozilla.org/";	//사이트 베이스 URL
	//private String URL_page = "show_activity.cgi?id={0}";		//원본 사이트 URL
	private String URL_page = "show_bug.cgi?id={0}&ctype=xml";
	
	//출력파일 설정
	private static int SPLIT_CNT = 1000; //출력 파일을 몇개 단위로 폴더링 할지 결정
	private static String OUTPUT_EXT = "xml"; //출력파일 확장자
	
	/**
	 * 메인 프로그램
	 * @param args
	 */
	public static void main(String[] args) {
		//1. 로그 파일 설정=======================================================
		try {
			log.init(LogPath);
		} catch (IOException e){ 
			log.printStackTrace(e);
			return;
		}

		
		//2. 원하는 작업 시행
		Extract extract  = new Extract();
		boolean ret= extract.makeXMLwithCount(INPUT_LIST, SPLIT_CNT, HASH_DEPTH,HASH_SIZE, OUTPUT_PATH, OUTPUT_EXT);
		
	}
		
	
	


	/////////////////////////////////////////////////////////////////////////////
	// 메인 로직 함수들.
	/////////////////////////////////////////////////////////////////////////////
	/**
	 * 파일에 있는 리스트에 해당하는 버그아이디들을 출력 패스로 출력.
	 * @param _listfile
	 * @param _depth
	 * @param _size
	 * @param _outputPath
	 * @return
	 */
	public boolean makeXMLwithCount(String _listfile, int _splitCnt, int _depth, int _size, String _outputPath, String _ext)//Map<Integer,Integer> _map)
	{
		//작업대상 파일 로드
		List<Integer> list = loadList(_listfile);
		if(list==null) return false;
		
		
		//기타 변수 기본값 설정.
		String src = null;
		File input = null;					//입력 파일
		long k=0;							//처리된 파일 수 
		long splitID = 0;					//폴더 분리시, 몇개의 파일 단위로 분리할 것인지 결정.
		long Max_Size = list.size();
		
	
		//타겟 디렉토리 생성
		if (createPath(_outputPath + splitID +"\\")==false){
			System.out.println("Output Path Create Error");
			return false;
		}
		
		//리스트 순회(map list) : Map = ID	count    ===>   count별로 폴더 구분.
		Iterator<Integer> it = list.iterator();
        while(it.hasNext()){

        	//출력결과 저장용 폴더 생성.
			if (k%_splitCnt==0){
				if (k==0) 	splitID = 0;
				else 		splitID += _splitCnt;
				
				//대상 폴더 생성.
				if (createPath(_outputPath +splitID+"\\")==false){
					System.out.println("Output Path Create Error");
					return false;
				}
				log.info("Create new #"+(k/_splitCnt)+" subfolder : "+ splitID);	
			}
        
        	//이동 대상 ID 추출
            Integer bug_id = it.next();
            
            
            //소스파일의 실제 경로 추출.
            src = TextUtil.convertURLtoPath(URL_base + URL_page.replaceFirst("\\{0\\}", Integer.toString(bug_id)));
            src = TextUtil.makeBucket(src, _size, _depth);
            src = LocalProxyPath+ src;
            

			//파일 존재 확인
			input = new File(src);
			if(input.exists()==false)
			{
				log.error("["+k+"/"+Max_Size+"] No file.("+bug_id+")");
				continue;
			}
			
			//파일 복사.(카운트별 폴더로 넣음)
			String out = _outputPath + splitID+"\\"+ bug_id +"."+_ext;	//복사대상
			fileCopy(src, out);											//파일복사
			
			
			//결과출력
			if(k%100==0) log.info("["+k+"/"+Max_Size+"] Success.");	//결과 출력.(100개에 한번..)
			
			//처리 카운트 증가.
			k++;
        }        
        
        //최종결과 출력
        if(k==Max_Size){
        	log.info("["+k+"/"+Max_Size+"] Success all works.");
        	return true;
        }
        else{
        	log.error("["+k+"/"+Max_Size+"] Done with some Errors.");
        	return false;
        }
	}
	
	


	/**
	 * 파일에 있는 리스트에 해당하는 버그아이디들을 출력 패스로 출력.
	 * 
	 */
	public void makeXML(String[] idList, String _outputPath)
	{
		String path=null;
		File file = null;
		
		//타겟 디렉토리 생성
		file = new File(_outputPath);
		if(file.exists()==false)
		{
			if(file.mkdirs()==false)
			{
				System.out.println("Create Error");
				return;
			}
		}
		
		
		//리스트 순회
		for(int i=0; i<idList.length; i++)
		{
			path = TextUtil.convertURLtoPath(URL_page.replaceFirst("\\{0\\}", idList[i]));
			path = LocalProxyPath+ path;

			//파일 존재 확인 및 복사
			file = new File(path);
			if(file.exists()==true)
			{
				fileCopy(path, _outputPath + i +".xml");
				log.info("["+(i+1)+"/"+idList.length+"] Success.("+idList[i]+"): "+ path);
			}
			else{
				log.error("["+(i+1)+"/"+idList.length+"] No file.("+idList[i]+"): "+ path);
			}

		}

	}


	
	/////////////////////////////////////////////////////////////////////////////
	// 유틸 함수들.
	/////////////////////////////////////////////////////////////////////////////
	/**
	 * NIO 활용 코드 (Channel활용 복사)
	 * from : http://www.yunsobi.com/blog/406
	 * @param source
	 * @param target
	 */
	public boolean fileCopy(String source, String target) {
		boolean ret = true;
		// 복사 대상이 되는 파일 생성
		File sourceFile = new File(source);

		// 스트림, 채널 선언
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		FileChannel fcin = null;
		FileChannel fcout = null;
	

		try {
			// 스트림 생성
			inputStream = new FileInputStream(sourceFile);
			outputStream = new FileOutputStream(target);
			
			// 채널 생성
			fcin = inputStream.getChannel();
			fcout = outputStream.getChannel();

			// 채널을 통한 스트림 전송
			long size = fcin.size();
			fcin.transferTo(0, size, fcout);
			
		} catch (Exception e) {
			e.printStackTrace();
			ret = false;
			
		} finally {
			// 자원 해제
			try {
				fcout.close();
			} catch (IOException ioe) {
				ret = false;
			}
			try {
				fcin.close();
			} catch (IOException ioe) {
				ret = false;
			}
			try {
				outputStream.close();
			} catch (IOException ioe) {
				ret = false;
			}
			try {
				inputStream.close();
			} catch (IOException ioe) {
				ret = false;
			}
		}
		
		return ret;
	}
	
	


	
	/**
	 * 부모 디렉토리 생성.
	 * @param _file 최종 파일
	 */
	private boolean createPath(String _file) {
		
		File fout = new File(_file);
		File parent = fout.getParentFile();
		if(!fout.exists() && !fout.mkdirs()){
		    throw new IllegalStateException("Couldn't create dir: " + parent);
		}		
		return true;
	}
	
	

	
	/**
	 * 버킷별 인덱스 생성.
	 */
	private int[] getIndexWithOne(int _size) {
		int[] index = new int[_size];
		for(int i=0; i<1000; i++) index[i] =1;
		return  index;
	}


	
	/**
	 * 부모 디렉토리 생성.
	 * @param _file 최종 파일
	 */
	private boolean createParentPath(String _file) {
		
		File fout = new File(_file);
		File parent = fout.getParentFile();
		if(!parent.exists() && !parent.mkdirs()){
		    throw new IllegalStateException("Couldn't create dir: " + parent);
		}
		return true;
	}

	
	/**
	 * 맵에 들어갈 데이터.
	 * count	ID	 의 리스트가 있는 파일. 
	 */
	private Map<Integer, Integer> setHash(String _filename) {
		//기본 설정
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		String delimeter = "\t";
		
		// 키값과 카운터가 있는 값의 파일. 로드.
		File src = null;
		FileReader fr = null;
		BufferedReader br=null;
		try {
			src = new File(_filename);
			fr = new FileReader(src);
			br = new BufferedReader(fr);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}
		
		
		//라인단위로 파일 읽어서 맵에 저장.
		try {
			String str;
			while((str = br.readLine()) != null)							//라인 획득
			{
				String[] a = str.split(delimeter);							//라인 분리
				map.put(Integer.parseInt(a[1]),Integer.parseInt(a[0]));		//라인 입력
			
			}
		} catch (IOException e) {
			e.printStackTrace();
			map = null;
		}
		finally
		{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
				
		return map;
	}
	
	/**
	 * 맵에 들어갈 데이터.
	 * count	ID	 의 리스트가 있는 파일. 
	 */
	private List<Integer> loadList(String _filename) {
		//기본 설정
		List<Integer> list = new ArrayList<Integer>();
		
		// 파일 아이디 목록이 있는 파일 열기
		File src = null;
		FileReader fr = null;
		BufferedReader br=null;
		try {
			src = new File(_filename);
			fr = new FileReader(src);
			br = new BufferedReader(fr);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}		
		
		//라인단위로 파일 읽어서 맵에 저장.
		try {
			String str;
			while((str = br.readLine()) != null)							//라인 획득
			{
				list.add(Integer.parseInt(str));
			}
		} catch (IOException e) {
			e.printStackTrace();
			list = null;
		}
		finally
		{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
				
		return list;
	}

}
