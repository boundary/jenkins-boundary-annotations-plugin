package hudson.plugins.boundary;

import hudson.model.BuildListener;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;

public class Boundary
{
    private final String BOUNDARY_URI = "https://api.boundary.com/annotations";
    private HttpClient client;
    private String serviceUser;
    private String token;
    private String body;

    public Boundary( String serviceUser, String token )
    {
        this.serviceUser = serviceUser;
        this.token = token;
    }

    public void post( String name, String result, String number, BuildListener listener )
              throws IOException
    {
        createClient(  );

        PostMethod post = new PostMethod( BOUNDARY_URI );
        body = "{\"type\": \"jenkins build\", \"subtype\": \"" + name + "\", \"tags\": [\"" + result + "\",\"" + number  + "\"]}";
        System.out.println(body);
        
        post.addRequestHeader("Content-Type", "application/json");   
        post.setRequestBody( body );

        try
            {
                client.executeMethod( post );
            } 
            
        catch ( Exception e )
            {
                e.printStackTrace( listener.error( "Unable to send message to Boundary API") );
            } 
        
        finally
            {
                post.releaseConnection(  );
            }
    }

    private void createClient(  )
    {
        client = new HttpClient(  );

        Credentials defaultcreds = new UsernamePasswordCredentials( serviceUser, token );
        client.getState(  ).setCredentials( AuthScope.ANY, defaultcreds );
        client.getParams(  ).setAuthenticationPreemptive( true );
    }
}
