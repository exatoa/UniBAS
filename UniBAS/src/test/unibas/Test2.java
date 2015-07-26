package test.unibas;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.sel.UnifiedBTS.Util.ProcessLauncher;
import org.sel.UnifiedBTS.Util.ProcessLauncher.OutputListener;
import org.sel.UnifiedBTS.Util.TextUtil;
import org.sel.UnifiedBTS.Util.XML;
import org.sel.UnifiedBTS.Util.TermElement;
import org.sel.UnifiedBTS.Util.log;

public class Test2 {

	/**
	 * 테스트 가동을 위한 환경
	 * @param args
	 */
	public static void main(String[] args) {
		new Test2().run();
	}
	
	/**
	 * 테스트 메인
	 */	
	public void run() {
		//ConvertUrls();
		//TimeTest();
		//bucketTest();
		//XMLTest();
		//FileTest();
		cmd_test();
		
	}
	
	private void FileTest() {

		InputStream file = this.getClass().getClassLoader().getResourceAsStream("schema/mssql_unified_TF.sql");
		if(file==null){
			System.out.println("Error file");
			return;
		}

	    // Delimiter
	    String delimiter = ";;";

	    // Create scanner
	    Scanner scanner = null;
	    try {
	        scanner = new Scanner(file,"UTF-8");
	        scanner.useDelimiter(delimiter);
	    }
	    catch(IllegalArgumentException e){
	    	log.printStackTrace(e);
	    	scanner = null;
	    }
	    if(scanner==null)
	    {
	    	System.out.println("Error scanner");
	    	return;
	    }
	    	

	    // Loop through the SQL file statements
	    int i=0;
	    while(scanner.hasNext()) {

	        // Get statement 
	    	i++;
	        String rawStatement = scanner.next()+";";
	        System.out.println(">>["+i+"=============states===================================");
	        System.out.println(rawStatement);
	    }
		
	}

	String[] dic = {"The","$88","billion","JWST","features","18","hexagonal","mirror","segments","that","will","work","together","to","form","one","21-foot-wide","65","meters","mirror","larger","than","any","other","mirror","that","ever","flown","in","space","NASA","officials","said","For","comparison","the","agency","iconic","Hubble","Space","Telescope","sports","an","8-foot","or","24","m","primary","mirror","JWST","is","optimized","to","view","in","infrared","light","The","telescope","should","be","able","to","do","lots","of","different","things","during","its","operational","life","researchers","say","including","scanning","the","atmospheres","of","alien","planets","for","oxygen","and","other","gases","that","could","be","produced","by","living","organisms","Such","delicate","work","is","best","performed","by","space","telescopes","which","don't","have","to","look","through","Earth","atmosphere","JWST","will","work","in","concert","with","another","NASA","space","mission","in","this","regard","performing","follow-up","observations","on","promising","nearby","worlds","found","by","the","agency","Transiting","Exoplanet","Survey","Satellite","TESS","which","is","scheduled","to","blast","off","in","2017","With","the","James","Webb","we","have","our","first","chance","our","first","capability","of","finding","signs","of","life","on","another","planet","MIT","astrophysicist","Sara","Seager","said","during","Monday","NASA","briefing","Now","nature","just","has","to","provide","for","us","5","Bold","Claims","of","Alien","Life","A","numbers","game","But","nature","may","not","be","so","willing","at","least","during","the","JWST","mission","Seager","and","other","experts","stress","And","it","all","comes","down","to","numbers","There","is","no","shortage","of","planets","in","the","Milky","Way","Our","galaxy","teems","with","at","least","100","billion","planets","10","to","20","percent","of","which","Mountain","said","likely","circle","in","their","host","star","habitable","zone","that","just-right","range","of","distances","that","could","allow","liquid","water","to","exist","on","a","world","surface","If","there","nothing","terribly","special","about","Earth","then","life","should","be","common","throughout","the","cosmos","many","scientists","think","But","most","exoplanets","are","very","far","away","and","all","of","them","are","faint","JWST","while","large","by","current","standards","won't","have","enough","light-collecting","area","to","investigate","more","than","a","handful","of","potentially","habitable","planets","researchers","say","A","spacecraft","with","a","33-foot","10","m","mirror","would","give","researchers","a","much","better","chance","of","finding","biosignatures","in","alien","atmospheres","but","Mountain","would","like","something","even","bigger","With","a","20-meter","telescope","we","can","see","hundreds","of","Earth-like","planets","around","other","stars","he","said","That","what","it","takes","to","find","life"};
	int dic_size = 0;
	
	private void XMLTest() {
		List<TermElement> list = new ArrayList<TermElement>();
		
		dic_size = dic.length;
		
		for(int src=200; src<300; src++){			
			for(int i=0; i<5; i++){
				TermElement e = new TermElement();
				e.SrcType = "C";
				e.SrcID = src;
				e.Seq = i;
				e.Term = TextUtil.addQuotes(dic[(int) ((Math.random()*10000)%dic_size)]);
				e.Stopwords = false;
				list.add(e);	
		}}
		
		for(int src=15; src<20; src++){
			for(int i=0; i<5; i++){			
				TermElement e = new TermElement();
				e.SrcType = "D";
				e.SrcID = src;
				e.Seq = i;
				e.Term = TextUtil.addQuotes(dic[(int) ((Math.random()*10000)%dic_size)]);
				e.Stopwords = false;
				list.add(e);	
		}}
		
		for(int src=15; src<20; src++){
			for(int i=0; i<5; i++){
				TermElement e = new TermElement();
				e.SrcType = "S";
				e.SrcID = src;
				e.Seq = i;
				e.Term = TextUtil.addQuotes(dic[(int) ((Math.random()*10000)%dic_size)]);
				e.Stopwords = false;
				list.add(e);	
		}}
		
		String xml = XML.createElement(list);
		
		System.out.println(xml);
		
	}

	private void bucketTest() {
		
		int i;
		String str = "E:\\_temp\\BTS\\cache2\\www.mantisbt.org\\bugs\\view.php\\0b30ef0e4bad967092d5c6fac5a82d3a.txt";
		String[] list = {"http://www.mantisbt.org/bugs/view.php?id=1"
						,"http://bugs.scribus.net/view_all_bug_page.php?dir_0=ASC&dir_1=&handler_id[]=0&start_month=7&start_year=2014&page_number=1&show_category[]=0&type=1&reporter_id[]=0&os[]=0&show_profile[]=0&show_priority[]=0&view_state=0&sticky_issues=on&end_year=2014&show_resolution[]=0&per_page=0&sort_0=id&match_type=0&user_monitor[]=0&do_filter_by_date=on&platform[]=0&os_build[]=0&start_day=6&end_month=7&show_status[]=0&view_type=advanced&project_id=0&highlight_changed=0&sort_1=&end_day=6&note_user_id[]=0&show_severity[]=0&relationship_type=-1"
						,"http://bugs.scribus.net/bug_report_page.php?project_id=3"
						,"https://bugzilla.mozilla.org/buglist.cgi?query_format=advanced&columnlist=opendate&f1=bug_id&o1=greaterthan&v1=571900&f2=bug_id&o2=lessthaneq&v2=572000"
						,"https://bugzilla.mozilla.org/show_bug.cgi?id=950646&ctype=xml"
		};
		String ret = "";
		String path = "";
		for(i=0; i<list.length; i++)
		{
			path = TextUtil.convertURLtoPath(list[i]);		//패스 결정
			ret = TextUtil.makeBucket(path, 2,2);			//버킷 결정
			System.out.println(Integer.toString(i) + " : " + ret);
			
		}
		
	}

	
	private void cmd_test()
	{
        String cmd[] = { "cmd",
        				 "/c",
        				 "C:\\Python27\\python.exe", 
						 "D:\\_Zeck\\_Projects\\PyCharm\\UniBAS\\Main.py",
						 "-d",
						 "Analysis_firefox1",
						 "-s",
						 "7",
						 "-m",
						 "1",
						 "-w",
						 "\"b s\""
						 };
//		String cmd[] = {"cmd.exe","/c",
//						 "dir", 
//						 "/w"
//						 };
        
        ProcessLauncher launcher = new ProcessLauncher(cmd);
        OutputListener  listener = new OutputListener()
        {

			@Override
			public void standardOutput(char[] output) {
				System.out.print(output);
				System.out.flush();
			}

			@Override
			public void errorOutput(char[] output) {
				System.out.print(output);
				System.out.flush();
			}
        };
        launcher.addOutputListener(listener);
        
        launcher.launch();
        
	}
	

	
}
