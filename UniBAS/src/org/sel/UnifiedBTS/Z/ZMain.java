package org.sel.UnifiedBTS.Z;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.sel.UnifiedBTS.Core.Network.WebCacheFile;
import org.sel.UnifiedBTS.Util.log;

//import de.lightningbug.api.domain.Product;
//import de.lightningbug.api.service.ProductService;
import rs.baselib.security.AuthorizationCallback;
import rs.baselib.security.AuthorizationCallback;
import rs.baselib.security.SimpleAuthorizationCallback;


//b4j관련 라이브러리.
//import org.apache.commons.configuration.XMLConfiguration;
import java.lang.Iterable;
import b4j.core.session.AbstractPlainHttpSession;
import b4j.core.session.BugzillaHttpSession;
import b4j.core.DefaultIssue;
import b4j.core.DefaultSearchData;
import b4j.core.Issue;


public class ZMain {

	
	public static void main(String[] args) {
		ZMain main = new ZMain();
		
		
		//Statistics 가져와서 지정된 파일에 저장하는 것.
		//main.Main_StatisticsMain();
		
		//main.Main_LightningBugAPI();
		
		main.Main_b4jBugAPI();
	}
	
	/**
	 * LightningBugAPI 테스트.  
	 * HTTPS접근에서 에러발생.
	 */
//	private void Main_LightningBugAPI() {
//		String logFileName = "E:\\_Temp\\BTS\\log_statis2.txt";
//		String resFileName = "E:\\_temp\\BTS\\statisc2.txt";
//		String URL = "https://bugzilla.mozilla.org/page.cgi?id=productdashboard.html&tab=summary&product={0}&bug_status=all";
//		//2. 로그 파일 설정=======================================================
//		try {
//			log.init(logFileName);
//		} catch (IOException e){ 
//			log.printStackTrace(e);
//			return;
//		}
//		
//	
//		
//		
//		try {
//			final BugzillaClient client = new BugzillaClient(new URL("https://bugzilla.mozilla.org"), "forglee@naver.com", "Sel535447");
//			
//			
//			client.login();
//					
//		
//			//create the product service an query all products
//			final List<Product> products = new ProductService(client).getProducts();
//	
//			//display the product configuration
//			for(final Product product : products)
//			{
//	
//			   System.out.print(product.getId() + ": ");
//			   System.out.println(product.getName());
//	
//			   System.out.println("Components:");
//			   final Set<String> components = product.getComponents();
//			   for(String component : components){
//			      System.out.println(component);
//			   }
//	
//			   System.out.println("Severities:");
//			   final Set<String> severities = product.getSeverities();
//			   for(final String severity : severities){
//			      System.out.println(severity);
//			   }
//	
//			   System.out.println("Versions:");
//			   final Set<String> versions = product.getVersions();
//			   for(final String version : versions){
//			      System.out.println(version);
//			   }
//	
//			   System.out.println("Milestones:");
//			   final Set<String> milestones = product.getMilestones();
//			   for(final String milestone : milestones){
//			      System.out.println(milestone);
//			   }
//		   
//			}
//		}
//		catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//	
//	}

	
	/**
	 * LightningBugAPI 테스트.  
	 * HTTPS접근에서 에러발생.
	 */
	private void Main_b4jBugAPI() {
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
		
		
				
		 
		// Configure from file
		XMLConfiguration myConfig;
		BugzillaHttpSession session = null;
		try {
			//URL pathFile = this.getClass().getClassLoader().getResourceAsStream();
			//URL pathFile = ZMain.class.getResource("config/b4j.bugzilla.xml");
			//URL pathFile = this.getClass().getClassLoader().getResource("resource/bj4.bugzilla.xml");
			File configFile= new File("config/b4j.bugzilla.xml");
			if(configFile == null) return;
			myConfig = new XMLConfiguration(configFile);
			
			// Create the session
			session = new BugzillaHttpSession();
			//BugzillaClient client = new BugzillaClient(new URL("https://bugzilla.mozilla.org"), "forglee@naver.com", "Sel535447");
			session.configure(myConfig);
			session.setBaseUrl(new URL("https://bugzilla.mozilla.org"));
		    session.setBugzillaBugClass(DefaultIssue.class);
		    
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		
	    
	    
//	    //코드로 설정하는 방법 (안됨  AuthorizationCallback을 찾을 수 없음)
//	    AuthorizationCallback authCallback = new SimpleAuthorizationCallback("forglee@naver.com", "Sel535447");
//	    session.setAuthorizationCallback(authCallback);
//
//	    //If you sit behind a proxy, then you will need this snippet:
//	    session.getHttpSessionParams().setProxyHost("192.168.0.250");
//	    session.getHttpSessionParams().setProxyPort(8080);
//	    //If your proxy requires authorization:
//	    AuthorizationCallback proxyAuthCallback = new SimpleAuthorizationCallback("username", "password");
//	    session.getHttpSessionParams().setProxyAuthorizationCallback(proxyAuthCallback);
	 
	    
	    
		 
		// Open the session
		if (session.open()) {
		    // Search abug
		    DefaultSearchData searchData = new DefaultSearchData();
		    searchData.add("classification", "Java Projects");
		    searchData.add("product", "Bugzilla for Java");
		 
		    // Perform the search
		    Iterable<Issue> i = session.searchBugs(searchData, null);
		    for (Issue issue : i) {
		       System.out.println("Bug found: "+issue.getId()+" - "+issue.getSummary());
		    }
		    

		    // Close the session
		    session.close();
		}
		
		

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
