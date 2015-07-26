package test.ref;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

public class GetThread extends Thread {

    /**
	 * @uml.property  name="httpClient"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private final CloseableHttpClient httpClient;
    /**
	 * @uml.property  name="context"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private final HttpContext context;
    /**
	 * @uml.property  name="httpget"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private final HttpGet httpget;

    public GetThread(CloseableHttpClient httpClient, HttpGet httpget) {
        this.httpClient = httpClient;
        this.context = HttpClientContext.create();
        this.httpget = httpget;
    }

    @Override
    public void run() {
        try {
            CloseableHttpResponse response = httpClient.execute(httpget, context);
            try {
                HttpEntity entity = response.getEntity();
                System.out.println("response:"+response.getStatusLine());
            } finally {
                response.close();
            }
        } catch (ClientProtocolException ex) {
            // Handle protocol errors
        } catch (IOException ex) {
            // Handle I/O errors
        }
    }

}
