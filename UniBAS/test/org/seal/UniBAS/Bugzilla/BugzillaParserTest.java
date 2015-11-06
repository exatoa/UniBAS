package org.seal.UniBAS.Bugzilla;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.seal.UniBAS.Bugzilla.Model.BugReport;
import org.seal.UniBAS.Util.TextUtil;

public class BugzillaParserTest {

	List<String> files = null;
	List<Integer> answers = null;
	
	
	public void answerSet()
	{
		String base = "E:\\_Research\\2014_UniBAS\\_cache\\bugzilla.mozilla.org\\show_bug.cgi\\";
		
		files = new ArrayList<String>();
		files.add(base+"0c\\0c\\0c0c6212fb8883d0cdca72c52bce08f5.txt");
		files.add(base+"0e\\0e\\0e0e8d760499a4c0f072d1cddb7294c9.txt");
		files.add(base+"0e\\0e\\0e0e9f43875ef702f5a4d3e8879a95f3.txt");
		files.add(base+"0e\\0e\\0e0ea81696acd5b5848fa33c77c845d2.txt");
		files.add(base+"0e\\0e\\0e0eaf80287ac091b929dc91ef459b92.txt");
		files.add(base+"0e\\0e\\0e0eb102a46464a55cc6bbe6a207f016.txt");
		files.add(base+"0e\\0e\\0e0ed9d1438fa60d3374e8cc726f1085.txt");
		
		answers = new ArrayList<Integer>();
		answers.add(890662);
		answers.add(298260);
		answers.add(388069);
		answers.add(926388);
		answers.add(158509);
		answers.add(799198);
		answers.add(669071);
	}
//	@Test
//	public void testBugzillaParser() {
//		fail("Not yet implemented");
//	}

	@Test
	public void testAnalysisReportWithDOM() {
		//fail("Not yet implemented");
		//테스트용 변수
		answerSet();

		List<Integer> ids = new ArrayList<Integer>();
		for(String path:files)
		{
			String xml = TextUtil.readTextFile(path, null);
			BugzillaParser parser = new BugzillaParser();
			BugReport report = parser.analysisReportWithDOM(xml, "forglee@naver.com");
			ids.add(report.BugID);
			//System.out.println(">>"+ report.BugID+ "==================\n");//+report.Bugnotes.get(0).TheText+"\n\n");
		}
		
		assertArrayEquals(answers.toArray(), ids.toArray());		
	}

	@Test
	public void testAnalysisReportWithJSoup() {
		//테스트용 변수
		answerSet();

		List<Integer> ids = new ArrayList<Integer>();
		for(String path:files)
		{
			String xml = TextUtil.readTextFile(path, null);
			BugzillaParser parser = new BugzillaParser();
			BugReport report = parser.analysisReport(xml, "forglee@naver.com");
			ids.add(report.BugID);
			//System.out.println(">>"+ report.BugID+ "==================\n"+report.Bugnotes.get(0).TheText+"\n\n");
		}
		
		assertArrayEquals(answers.toArray(), ids.toArray());	
	}

//	@Test
//	public void testAnalysisHistory() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testAnalysisTags() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testAnalysisComponent() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testAnalysisProduct() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testAnalysisVotes() {
//		fail("Not yet implemented");
//	}

}
