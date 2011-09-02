package hudson.plugins.boundary;

import hudson.model.BuildListener;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import org.codehaus.jackson.map.ObjectMapper;

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
    
    public String annotation()
    {
        
        HashMap<String, Object> annot = new HashMap<String, Object>();

        annot.put("type", "jenkins build");
        annot.put("subtype", "test");
        annot.put("creation_time", 1308336162);
        annot.put("start_time", 1308336162);
        annot.put("end_time", 1308336162);

        ArrayList<Map<String, String>> linkList = new ArrayList<Map<String, String>>();

        Map<String, String> linkOne = new HashMap<String, String>();
        linkOne.put("rel", "self");
        linkOne.put("href", "https://api.boundary.com/annotations/9af9c92d72c9974d");

        Map<String, String> linkTwo = new HashMap<String, String>();
        linkTwo.put("rel", "origin");
        linkTwo.put("href", "http://hudson.boundary.com:8080/");
        linkTwo.put("note", "hudson");

        Map<String, String> linkThree = new HashMap<String, String>();
        linkThree.put("rel", "origin");
        linkThree.put("href", "http://hudson.boundary.com:8080/");
        linkThree.put("note", "hudson");

        Map<String, String> linkFour = new HashMap<String, String>();
        linkFour.put("rel", "version");
        linkFour.put("href", "https://git.cid1.boundary.com/metermgr/tree/350bdc15236d3829ef3985535c650a6c64ddce80");
        linkFour.put("note", "350bdc15236d3829ef3985535c650a6c64ddce80");

        Map<String, String> linkFive = new HashMap<String, String>();
        linkFive.put("rel", "build");
        linkFive.put("href", "http://hudson.boundary.com:8080/job/metermgr/210/");
        linkFive.put("note", "210");


        linkList.add(linkOne);
        linkList.add(linkTwo);
        linkList.add(linkThree);
        linkList.add(linkFour);
        linkList.add(linkFive);

        annot.put("links", linkList);
        annot.put("tags", Arrays.asList(new String[]{"metermgr", "deploy", "erlang", "cid1"}));

        ObjectMapper mapper = new ObjectMapper();
        String jsonOutput = new String();
        
        try {
            jsonOutput = mapper.writeValueAsString(annot);
            System.out.println(jsonOutput);
        }
        catch(IOException ioe) {
            System.out.println("json error: " + ioe);
        }
        
        return jsonOutput;
    }

    public void post( String json, BuildListener listener )
              throws IOException
    {
        createClient(  );

        PostMethod post = new PostMethod( BOUNDARY_URI );
        
        post.addRequestHeader("Content-Type", "application/json");   
        post.setRequestBody( json );

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
