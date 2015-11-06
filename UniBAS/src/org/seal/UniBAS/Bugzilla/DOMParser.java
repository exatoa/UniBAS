package org.seal.UniBAS.Bugzilla;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class DOMParser {
	////////////////////////////////////////////////////////////////////
	// 싱글톤 적용 함수
	////////////////////////////////////////////////////////////////////	
	private static DOMParser instance = null;
	
	public static DOMParser getInstance(){
		
		//기존에 존재하는 instance 반환
		if (instance != null)	return instance;
		
		//새 인스턴스 생성
		try {			
			instance = new DOMParser();
			instance.createParser();
		} 
		catch (ParserConfigurationException e) {
			//에러 발생서 처리
			e.printStackTrace();
			instance = null;
		}		
		return instance;
	}
	
	////////////////////////////////////////////////////////////////////
	// 클래스 함수
	////////////////////////////////////////////////////////////////////
	private DocumentBuilder xmlParser= null; 
	private XPath finder = null;
	
	/**
	 * 싱글톤을 위한 빈 생성자 생성
	 */
	private DOMParser(){
		
	}
	
	
	/**
	 * 파서 생성 
	 * @throws ParserConfigurationException
	 */
	private void createParser() throws ParserConfigurationException 
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		xmlParser = factory.newDocumentBuilder();
		finder = XPathFactory.newInstance().newXPath();
	}

	
	/**
	 * 파일을 로드하여 DOM 생성
	 * @param _ins
	 * @return
	 * @throws Exception
	 */
	public Document parseXML(File _file)
	{
		Document doc = null;
	    try {
	    	FileInputStream stream;
	    	stream = new FileInputStream(_file);
	    	InputSource is = new InputSource(stream);
			doc = xmlParser.parse(is);
		}
	    catch (SAXException e) {
			e.printStackTrace();
		}
	    catch (IOException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		 
	    return doc;
	}
	
	/**
	 * String 데이터를 파싱하여  DOM 생성
	 * @param _ins
	 * @return
	 * @throws Exception
	 */
	public Document parseXML(String _xml)
	{
		Document doc = null;
	    try {
	    	InputSource is = new InputSource(new StringReader(_xml));
			doc = xmlParser.parse(is);
		}
	    catch (SAXException e) {
			e.printStackTrace();
		}
	    catch (IOException e) {
			e.printStackTrace();
		}
		 
	    return doc;
	}
	
	/**
	 * 웹페이지를 직접 접근하여   DOM 생성
	 * @param _ins
	 * @return
	 * @throws Exception
	 */
	public Document parseXMLFromURL(String _url)
	{
		Document doc = null;
	    try {
			doc = xmlParser.parse(_url);
		}
	    catch (SAXException e) {
			e.printStackTrace();
		}
	    catch (IOException e) {
			e.printStackTrace();
		}
		 
	    return doc;
	}

	
	public static void main(String[] args)
	{
		//테스트용 변수
		String filename = "E:\\_Research\\2014_UniBAS\\_cache\\bugzilla.mozilla.org\\show_bug.cgi\\0c\\0c\\0c0c6212fb8883d0cdca72c52bce08f5.txt";
		
		DOMParser parser = DOMParser.getInstance();
		
		String xml = "<long_desc isprivate=\"0\">\n<commentid>7607158</commentid>\n<who name=\"SC\">shajith.chacko@gmail.com</who>\n<bug_when>2013-07-06 22:06:00 +0000</bug_when>\n<thetext>OK, reading the source in bootstrap.py, it looked like the failure had something to do with the version of mercurial. I force-installed mercurial (brew link --overwrite mercurial), which got rid of this failure. Continuing with build now.\n\nI am not sure if this needs any fixing.</thetext>\n</long_desc>";
			
		//Document doc = parser.parseXML(new File(filename));
		//parser.extractLongDesc(doc);
		Document doc = parser.parseXML(xml);
		//parser.extractText(doc);
		
//		String xml =parser.readTextFile(filename, null);
//		parser.jsoup(xml);
		
		


      
	}
	
	
	
}
