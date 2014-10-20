package org.sel.UnifiedBTS.Util;


import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
 
public class XML {

    public static String createElement(List<TermElement> _list){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
         
        try{
            db = dbf.newDocumentBuilder();
        }catch(ParserConfigurationException e){
            e.printStackTrace();
        }
        
        Document d = db.newDocument();
        
        Element root = d.createElement("Es");
        d.appendChild(root);
        
        //자식 엘러먼트
        Element row = null;
        for(TermElement e:_list)
        {
        	row = d.createElement("E");
        	row.setAttribute("src_type",	e.SrcType);
        	row.setAttribute("src_id", 		Integer.toString(e.SrcID));
        	row.setAttribute("seq", 		Integer.toString(e.Seq));
        	row.setAttribute("term", 		e.Term);
        	row.setAttribute("stopwords", 	(e.Stopwords==true)?"1":"0");
       	
        	root.appendChild(row);
        }
        
        //XML 쓰기       
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
    	Transformer transformer = null;;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

    	transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    	transformer.setOutputProperty(OutputKeys.INDENT, "yes");	
    	
    	StringWriter sw = new StringWriter();
    	DOMSource source = new DOMSource(d);
    	//StreamResult result = new StreamResult(System.out);//StreamResult(new FileOutputStream(new File("C:\\file.xml")));
    	StreamResult result = new StreamResult(sw);
    	
    	

    	// 파일로 쓰지 않고 콘솔에 찍어보고 싶을 경우 다음을 사용 (디버깅용)
    	//StreamResult result = new StreamResult(System.out);
    	try {
			transformer.transform(source, result);
		} catch (TransformerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
        
        
        return sw.toString();
    }
}
