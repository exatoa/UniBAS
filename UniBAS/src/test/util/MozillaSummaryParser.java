package test.util;

import java.util.HashMap;
import java.util.Map;



import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;


public class MozillaSummaryParser 
{
	
	public String RegexEmail = "^([0-9a-zA-Z_.+\\-]+)@([0-9a-zA-Z_-]+)(\\.[.0-9a-zA-Z_-]+)"; 
	public MozillaSummaryParser()
	{
		
	}
	/////////////////////////////////////////////////////////////////
	//  Parsing
	/////////////////////////////////////////////////////////////////
	/**
	 * 파싱을 통하여 리포트 리스트를 추가.
	 * @param _xml
	 * @return
	 */
	public String analysisStatistics(String _html)
	{
		String result="";
		Document doc = Jsoup.parse(_html, "", Parser.htmlParser());
		if(doc==null) return null;

		Map<String, Integer> keys = new HashMap<String, Integer>();
		String title=null;
		try{
			
			//버그제목
			Elements divs = doc.select("div.product_name");
			Element title_div = divs.get(0);
			title = title_div.text();
				
			
			//버그카운트
			Elements scripts = doc.select("script");
			Element script = scripts.get(scripts.size()-1);
			
			String str = "";
			for (DataNode node : script.dataNodes()) {
	            str += node.getWholeData();
	        }		
			String sub ;
			String sigCounts = "PD.summary.bug_counts = ";
			String sigStatus = "PD.summary.status_counts = ";
			String sigPriority = "PD.summary.priority_counts = ";
			String sigSeverity = "PD.summary.severity_counts = ";
			int lastIdx = 0;
			
			//bug count
			lastIdx =  str.indexOf("];")+2;
			sub = str.substring(str.indexOf(sigCounts)+sigCounts.length(),lastIdx);
			getKeyvalue2(sub, keys);
			
			
			//status
			lastIdx =  str.indexOf("];", lastIdx)+2;
			sub = str.substring(str.indexOf(sigStatus)+sigStatus.length(), lastIdx);
			getKeyvalue2(sub, keys);
			
			//sigPriority
			lastIdx =  str.indexOf("];", lastIdx)+2;
			sub = str.substring(str.indexOf(sigPriority)+sigPriority.length(), lastIdx);
			getKeyvalue2(sub, keys);
			
			//sigSeverity
			lastIdx =  str.indexOf("];", lastIdx)+2;
			sub = str.substring(str.indexOf(sigSeverity)+sigSeverity.length(), lastIdx);
			getKeyvalue2(sub, keys);
			
		
		}
		catch(Exception e)
		{
			return null;
		}
		
		Integer v;
		result += title + "\t";
		
		
		v = keys.get("Closed Bugs");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("Open Bugs");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("Total Bugs");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("ASSIGNED");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("NEW");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("REOPENED");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("RESOLVED");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("UNCONFIRMED");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("VERIFIED");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		result += "\t";
		
		v = keys.get("P1");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("P2");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("P3");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("P4");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("P5");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("--");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		result += "\t";
		
		v = keys.get("blocker");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("critical");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("enhancement");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("major");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("minor");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("normal");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		
		v = keys.get("trivial");
		if(v==null) result += "\t";
		else		result += v.toString() + "\t";
		

				
		return result;
	}
	
	
	private void getKeyvalue2(String sub, Map<String, Integer> keys)
	{
		sub = sub.replaceAll("name:", "\"name\":");
		sub = sub.replaceAll("count:", "\"count\":");
		sub = sub.replaceAll("percentage:", "\"percentage\":");
		sub = sub.replaceAll("link:", "\"link\":");
		sub = sub.replaceAll("'<.*</a>'", "0");
		sub = sub.replaceAll(";$", "");

		JSONParser parser=new JSONParser();
		Object obj;
		try {
			obj = parser.parse(sub);

			JSONArray array=(JSONArray)obj; 
			
			for(Object json : array)
			{
				JSONObject j = (JSONObject)json;
				String name = (String)j.get("name");
				int value = Integer.parseInt(j.get("count").toString());
				keys.put(name, value);
			}
			
		} catch (ParseException pe) {
			 System.out.println("position: " + pe.getPosition());
		    System.out.println(pe);
		}
	}
	
	
}
