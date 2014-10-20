package org.sel.UnifiedBTS.Start;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.*;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

public class SampleHTTPS {
	
	class MyTrustManager implements X509TrustManager {
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
			
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
	
	protected List<NameValuePair> convertParam(Map<String, String> params){
        List<NameValuePair> paramList = new ArrayList<NameValuePair>();
        Iterator<String> keys = params.keySet().iterator();
        while(keys.hasNext()){
            String key = keys.next();
            paramList.add(new BasicNameValuePair(key, params.get(key).toString()));
        }
         
        return paramList;
    }

	
	//"https://www.xxxx.dk/htbin/tell2"	
	public String httpsPOST(String _url, Map<String,String> _param) throws IOException {
		//파라메터 인코딩
		List<NameValuePair> params = convertParam(_param);
		UrlEncodedFormEntity encodedParams = new UrlEncodedFormEntity(params, Charset.forName("UTF-8"));
		
		String param = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(encodedParams.getContent()));
		String line;
		while((line = br.readLine()) != null) {
			param += line;
		}
		br.close();
		
		String result = "";
		try{
			SSLContext sslctx = SSLContext.getInstance("SSL");
			sslctx.init(null, new X509TrustManager[]{ new MyTrustManager() }, null);
	
			URL url = new URL(_url);
			
			HttpsURLConnection.setDefaultSSLSocketFactory(sslctx.getSocketFactory());
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			
			PrintStream ps = new PrintStream(con.getOutputStream());
			ps.println(param);
			ps.close();
			
			
			con.connect();
			
			
			if (con.getResponseCode() == HttpsURLConnection.HTTP_OK)
			{
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				while((line = br.readLine()) != null) {
					result += line;
				}
				br.close();
			}
			con.disconnect();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
			
		return result;
	}	
}
