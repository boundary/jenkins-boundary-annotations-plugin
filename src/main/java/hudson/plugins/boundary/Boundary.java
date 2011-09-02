package hudson.plugins.boundary;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

import hudson.plugins.git.util.BuildData;

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
    
    public void annotate(AbstractBuild<?, ?> build)
    {
        
        HashMap<String, Object> annot = new HashMap<String, Object>();

        annot.put("type", "build");
        annot.put("subtype", build.getProject().getName());

        ArrayList<Map<String, String>> linkList = new ArrayList<Map<String, String>>();

        Map<String, String> linkOne = new HashMap<String, String>();
        linkOne.put("rel", "origin");
        linkOne.put("href", Hudson.getInstance().getRootUrl());
        linkOne.put("note", "jenkins");
        linkList.add(linkOne);
        
        Map<String, String> linkTwo = new HashMap<String, String>();
        linkTwo.put("rel", "build");
        linkTwo.put("href", Hudson.getInstance().getRootUrl() + build.getUrl());
        linkTwo.put("note", build.getDisplayName());
        linkList.add(linkTwo);
        
        Map<String, String> linkThree = new HashMap<String, String>();
        linkThree.put("rel", "version");
        
        BuildData data = build.getAction(BuildData.class);
        if (data != null) {
            String rev = data.getLastBuiltRevision().getSha1String();            
            linkThree.put("note", rev);
        }
        
        linkList.add(linkThree);
        
        annot.put("links", linkList);
        annot.put("tags", Arrays.asList(new String[]{build.getProject().getName(), "build", build.getResult().toString()}));

        ObjectMapper mapper = new ObjectMapper();
        String jsonOutput = new String();
        
        try {
            jsonOutput = mapper.writeValueAsString(annot);
            System.out.println(jsonOutput);
        }
        catch(IOException ioe) {
            System.out.println("json error: " + ioe);
        }
        
        createClient(  );

        PostMethod post = new PostMethod( BOUNDARY_URI );
        
        post.addRequestHeader("Content-Type", "application/json");   
        post.setRequestBody( jsonOutput );

        try
            {
                client.executeMethod( post );
            } 
            
        catch ( Exception e )
            {
                System.out.println( "Unable to send message to Boundary API: \n" + e);
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
