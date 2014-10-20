package org.sel.UnifiedBTS.Start;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


import java.nio.channels.FileChannel;

import static java.nio.file.StandardCopyOption.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sel.UnifiedBTS.Test.Test;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.log;

public class ReplaceCaches {

	public static void main(String[] args) {
		new ReplaceCaches().run();

	}
	
	private String LOG_PATH = "E:\\_Temp\\BTS\\ReplaceCaches.txt";
	private String PATH = "E:\\_Temp\\BTS\\cache\\";
	private String TARGET = "E:\\_Temp\\BTS\\cache2\\";
	private String TEMP = "E:\\_Temp\\BTS\\cache_back\\";
	
	private int OLD_SIZE = 2;
	private int OLD_LEVEL = 2;
	private int NEW_SIZE = 2;
	private int NEW_LEVEL = 2;
	
	private List<String> workingList = null;
	
	/**
	 * 작업 메인함수.
	 */	
	public void run() {
		initialize();
		work();
	}

	
	
	/**
	 * 전체 작업을 관리함.
	 */
	private void work() {
		//작업대상 리스트들 추가
		addWorkingList(PATH);
		
		//
		for(String path : workingList)
		{

			log.info("Start : "+ path);
			
			if(workUnit(path)==false)
			{
				log.error("Faild : " + path);
			}
			else
				log.info("Successed : "+ path);
		}
		
	}

	/**
	 * 지정된 한 폴더를 작업한다.
	 * @param _path
	 * @return
	 */
	private boolean workUnit(String _path) {
		
		File original = new File(_path);	//최초 원본 소스폴더
		File src = null;
		File dest = null;
		
		// 메인루트가 같으면 다른폴더에 임시로 복사 후 작업.
		if(PATH.compareTo(TARGET)==0)		
		{
			src = new File ( TEMP + _path.substring(PATH.length()-1, _path.length()) ); //임시폴더 생성.
			if(moveFile(original, src)==false)
			{
				log.error("Failed to move temp directory");
				return false;
			}
		}
		else
			src = original; //srcPath = TARGET + _path
		
		//이동 작업 시작
		dest = new File(TARGET + _path.substring(PATH.length()-1, _path.length()));
		
		if(ReplaceStructure(src, dest)==false)
		{
			log.error("Failed to replace directory");
			return false;
		}
		
		
		//복사를 하였다면...원본 삭제.
//		if(PATH.compareTo(TARGET)==0)
//		{
//			if(moveTempFile(src,original)==false)
//			{
//				log.error("Failed to move original directory");
//				return false;
//			}
//		}
		
		return true;
	}
	
	
	/**
	 * 실제 파일 이동작업을 실시.
	 * @param src
	 * @param dest
	 * @return
	 */
	private boolean ReplaceStructure(File src, File dest) {

		File[] files = src.listFiles();		
		
		for(File f : files)
		{
			//디렉토리는 재귀.
			if(f.isDirectory()==true)
			{
				if(ReplaceStructure(f, dest)==false) return false;
				else continue;
			}
			
			String path = dest.getPath()+ "\\"+f.getName();
			
			String newPath = TextUtil.makeBucket(path, NEW_SIZE, NEW_LEVEL);
			
			if(moveFile(f, new File(newPath))==false)
				return false;
			
			//log.info(f.getName()+" -->  "+newPath);
		}
		
//		files = src.listFiles();
//		if (files != null && files.length==0)
//			src.delete();
		
		return true;
	}



	/**
	 * 기존의 파일을 임시 패스로 이동.
	 * @param path2
	 * @param srcPath
	 */
	private boolean moveFile(File _src, File _dest) {

		boolean IS_PASS = false;
		
		//부모 폴더가 없는 경우 생성.
		if(_dest.getParentFile().exists()==false)
		{
			_dest.getParentFile().mkdirs();
		}
		else
		{
			//기존에 파일이 있는경우 최신파일을 남김.
			if(_dest.exists()==true)
			{
				if(_src.lastModified() <= _dest.lastModified()) IS_PASS = true;
			}
		}
		
		//nio 파일이동 (복사는 하나만 복사됨..)
		try
		{
			if (IS_PASS==false)
				Files.move(_src.toPath(), _dest.toPath(), REPLACE_EXISTING);
		}
		catch(IOException e)
		{
			log.printStackTrace(e);
			return false;
		}
		return true;		
	}

	
	/**
	 * 작업대상 디렉토리들을 추출한다. (재귀적으로 수행)
	 * @param _path
	 * @return
	 */
	private boolean addWorkingList(String _path)
	{
		if (_path==null) return false;

		File dir = new File(_path);	
		File[] files = dir.listFiles();		

		for(File f : files)
		{
			if(f.isDirectory()==false) continue;
			
			if(checkStructured(f, OLD_SIZE)==true)
				workingList.add(f.getPath());
			else 
				addWorkingList(f.getPath());
		}
		return true;
	}
	
	/**
	 * 구조화된 디렉토리인지 아닌지 확인한다.
	 * @param _f
	 * @param _size
	 * @return
	 */
	private boolean checkStructured(File _f, int _size)
	{
		File[] files = _f.listFiles();
		
		for(File f : files)
		{
			if(f.isDirectory()==false) return false;
			if(f.getName().length()!=_size) return false;
		}
		return true;
	}
	
	
	/**
	 * NIO 활용 코드 (Channel활용 복사)
	 * from : http://www.yunsobi.com/blog/406
	 * @param source
	 * @param target
	 */
	private boolean fileCopy(String source, String target) {
		
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
	 * 작업에 필요한 사항들을 초기화한다.
	 * @return
	 */
	private boolean initialize() {
		
		//1. 설정값들 확인
		workingList = new ArrayList<String>();
		
		File dir = new File(PATH);
		if(dir.exists()==false) return false;
		

		
		//2. 로그 파일 설정=======================================================
		try {
			log.init(LOG_PATH);
		} catch (IOException e){ 
			log.printStackTrace(e);
			return false;
		}
		
		return true;
	}
	
}
