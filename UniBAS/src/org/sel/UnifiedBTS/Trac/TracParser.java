package org.sel.UnifiedBTS.Trac;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.sel.UnifiedBTS.Trac.Model.BugReport;
import org.sel.UnifiedBTS.Trac.Model.TracError;



public class TracParser {
	/**
	 * @uml.property  name="undefinedUserID"
	 */
	//private int UndefinedUserID = 90000000;
	

	public TracError checkNotExists(String _html) {
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		Elements contents = doc.select("#content.error");
		if (contents==null || contents.size()==0) return null;
		Element content = contents.get(0);
		
		TracError error = new TracError();
		
		
		Elements ps = content.select("p.message");
		if(ps==null){
			error.Code = 0;
			return error;
		}
		
		error.Msg = ps.get(0).text();
		if (error.Msg.contains("not exist")==true)
			error.Code = -1;
		else if(error.Msg.contains("없습니다")==true)
			error.Code = -1;
		else
			error.Code = -2;
					
		return error;
	}

	public BugReport getTicketID(String _html) {
		
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		
		Elements headers = doc.select("#ticket h2");
		
		Element header=null;
		for(Element h : headers)
		{
			String s = h.text().trim();
			if (s.startsWith("#")==true)
			{
				header = h;
				break;
			}
		}
		if(header==null) return null;
		
		
		BugReport report = new BugReport();
		
		Elements links = header.select("a");
		
		String strID = links.get(0).text().trim();
		Integer id = Integer.parseInt(strID.substring(1));
		report.ID = id;
		
		//date값
		Elements date = doc.select("#ticket div.date");
		if (date==null)
			report.Date = "";
		else
			report.Date = date.get(0).text();
				
		return report;
	}
	
}