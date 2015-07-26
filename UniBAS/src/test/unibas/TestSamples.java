package test.unibas;

public class TestSamples {
/////////////////////////////////////////////////////////////////
//  테스트 필드
/////////////////////////////////////////////////////////////////

/*	
/**
* 입력스트림을 인코딩하여 String으로 반환
* @param _stream
* @param _encoding
* @return
* @throws Exception
*
private String ConvertHttpBody(InputStream _stream, String _encoding) throws Exception
{
String str = "";
byte [] tmp = new byte[2048];

while(_stream.read(tmp)!=-1){
str += new String(tmp, _encoding);	//기본은 UTF-8로 동작
}

if(_encoding.toUpperCase().compareTo("UTF-8")!=0);
//TODO :: UTF-8로 인코딩

return str;

//String str = IOUtils.toString(input, "UTF-8");  이런것도 있다는데 IOUtils는 없음.
//URLEncodedUtils.parse(HttpEntity);		//자동 인코딩 결정...해서 파싱하는듯한데? 리턴이 List네...?.
}
*/

/*테스트 코드들.

//스레드 실행예제-----------------
public void Start() throws InterruptedException, ClientProtocolException 
{

PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();

// URIs to perform GETs on
String[] urisToGet = {
"http://www.naver.com/",
"http://www.google.com/",
"http://www.daum.net/",
"http://www.cbnu.ac.kr/"
};

// create a thread for each URI
GetThread[] threads = new GetThread[urisToGet.length];
for (int i = 0; i < threads.length; i++) {
HttpGet httpget = new HttpGet(urisToGet[i]);
threads[i] = new GetThread(httpClient, httpget);
}

// start the threads
for (int j = 0; j < threads.length; j++) {
threads[j].start();
}

// join the threads
for (int j = 0; j < threads.length; j++) {
threads[j].join();
}
}



public void consumeContent(HttpEntity _entity) throws IOException
{
System.out.println(EntityUtils.toString(_entity, "UTF-8"));
}

public boolean saveToFile(String _filename, HttpEntity _entity)
{
return true;
//아래 코드를 활용.
//File file = new File("somefile.txt");
//FileEntity entity = new FileEntity(file, 
//ContentType.create("text/plain", "UTF-8"));        
//
//HttpPost httppost = new HttpPost("http://localhost/action.do");
//httppost.setEntity(entity);
}

/**
* This example demonstrates the use of a local HTTP context populated with
* custom attributes.
*
public void AsyncClientCustomContext() throws Exception {

CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();

try {
// Create a local instance of cookie store
CookieStore cookieStore = new BasicCookieStore();

// Create local HTTP context
HttpClientContext localContext = HttpClientContext.create();
// Bind custom cookie store to the local context
localContext.setCookieStore(cookieStore);

HttpGet httpget = new HttpGet("http://localhost/");
System.out.println("Executing request " + httpget.getRequestLine());

httpclient.start();

// Pass local context as a parameter
Future<HttpResponse> future = httpclient.execute(httpget, localContext, null);

// Please note that it may be unsafe to access HttpContext instance
// while the request is still being executed

HttpResponse response = future.get();
System.out.println("Response: " + response.getStatusLine());
List<Cookie> cookies = cookieStore.getCookies();
for (int i = 0; i < cookies.size(); i++) {
System.out.println("Local cookie: " + cookies.get(i));
}
System.out.println("Shutting down");
} finally {
httpclient.close();
}
}

*/
}
