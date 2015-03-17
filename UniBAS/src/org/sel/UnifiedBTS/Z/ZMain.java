package org.sel.UnifiedBTS.Z;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import org.sel.UnifiedBTS.Core.Network.WebCacheFile;
import org.sel.UnifiedBTS.Util.log;


public class ZMain {

	
	public static void main(String[] args) {
		ZMain main = new ZMain();
		
		
		//Statistics 가져와서 지정된 파일에 저장하는 것.
		main.Main_StatisticsMain();

	}
	

	/**
	 * Statistics 가져와서 지정된 파일에 저장하는 것.(Bugzilla에 대해서만 작동)
	 */
	public void Main_StatisticsMain() {
		String logFileName = "E:\\_Temp\\BTS\\log_statis2.txt";
		String resFileName = "E:\\_temp\\BTS\\statisc2.txt";
		String URL = "https://bugzilla.mozilla.org/page.cgi?id=productdashboard.html&tab=summary&product={0}&bug_status=all";
		
		//2. 로그 파일 설정=======================================================
		try {
			log.init(logFileName);
		} catch (IOException e){ 
			log.printStackTrace(e);
			return;
		}

		
		String[] products = new String[]{"Add-on SDK", "Android Background Services", "Calendar", "Composer", "Firefox", "Firefox for Android", 
				"Firefox for Metro", "Firefox Health Report", "Firefox OS", "Instantbird", "Mozilla Localizations", "Mozilla Services", 
				"Other Applications", "Penelope", "Powertool", "SeaMonkey", "Thunderbird", "Chat Core", "Core", "Directory", "JSS", 
				"MailNews Core", "NSPR", "NSS", "Plugins", "Rhino", "Tamarin", "Testing", "Toolkit", "addons.mozilla.org", "AUS", 
				"Bugzilla", "Input", "Marketplace", "Socorro", "Testopia", "Webtools", "Air Mozilla", "Audio/Visual Infrastructure",
				"bugzilla.mozilla.org", "Community Tools", "Data & BI Services Team", "Data Safety", "Datazilla", "Developer Documentation", 
				"Developer Ecosystem", "Developer Engagement", "Extend Firefox", "Firefox Affiliates", "Infrastructure & Operations", 
				"Instantbird Servers", "Intellego", "Internet Public Policy", "L20n", "Loop", "Marketing", "Mozilla Communities", "Mozilla Developer Network", 
				"Mozilla Foundation", "Mozilla Foundation Communications", "Mozilla Grants", "Mozilla Labs", "Mozilla Messaging", "Mozilla Metrics", "Mozilla QA", 
				"Mozilla Reps", "mozilla.org", "mozillaignite", "Pancake", "Petri", "Privacy", "quality.mozilla.org", "Release Engineering", "Snippets", 
				"support.mozilla.org", "Tech Evangelism", "Tracking", "Untriaged Bugs", "Web Apps", "Webmaker", "Websites", "www.mozilla.org", 
				"Boot2Gecko Graveyard", "Camino Graveyard", "CCK", "Core Graveyard", "Derivatives", "Documentation", "Fennec Graveyard", "Firefox Graveyard", 
				"Grendel", "MailNews Core Graveyard", "Minimo", "Mozilla Labs Graveyard", "Mozilla Localizations Graveyard", "Mozilla QA Graveyard", 
				"MozillaClassic", "Other Applications Graveyard", "Servo", "Skywriter", "support.mozilla.org Graveyard", "support.mozillamessaging.com Graveyard",
				"Toolkit Graveyard", "Websites Graveyard", "Webtools Graveyard"};
		
		WebCacheFile web = WebCacheFile.getInstance();
		web.setLimitation(1200, 10);
		web.setDownloadSpeed(40);
		
		MozillaSummaryParser parser = new MozillaSummaryParser();
		File file = new File(resFileName);
		FileWriter writer = null;

		try {
			
			writer = new FileWriter(file);
			String url = "";
			for(String t : products)
			{
				url = URL.replaceFirst("\\{0\\}", URLEncoder.encode(t, "utf-8"));
				
				String strHttp = web.getBody(url);
				log.println(strHttp);
				String result = parser.analysisStatistics(strHttp);
				if(result==null){
					result = t + "\t";
				}
				writer.write(result+"\n");
				writer.flush();
				log.println("Processed "+url);
				System.out.println("Processed "+url);
			}
			
			writer.close();
			
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
