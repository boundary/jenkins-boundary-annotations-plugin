/*
* Author:: Joe Williams (j@boundary.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/

/*
* some of this was based on https://github.com/jenkinsci/hudson-notifo-plugin
*/

package hudson.plugins.boundary;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.browser.GitRepositoryBrowser;
import hudson.plugins.git.browser.GitWeb;
import hudson.plugins.git.browser.GithubWeb;
import hudson.plugins.git.browser.GitoriousWeb;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;

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
        
        BuildData data = build.getAction(BuildData.class);
        if (data != null) {
            
            // Build the commit url and link if we can
            
            Map<String, String> linkThree = new HashMap<String, String>();
            linkThree.put("rel", "version");
            
            // Grab the SHA1
        	String rev = data.getLastBuiltRevision().getSha1String();
            linkThree.put("note", rev);
            
            String commitUrl = buildGitCommitUrl(rev, build);

            if (commitUrl != null) {
                // If the url exists add it to the hash and add
                // the hash to the list of links
            	linkThree.put("href", commitUrl);
            	linkList.add(linkThree);
            }
            
            // Build the repo url and link if we can
            
            Map<String, String> linkFour = new HashMap<String, String>();
            linkFour.put("rel", "repo");
            
            String repoUrl = buildGitRepoUrl(build);
            
            if (repoUrl != null) {
                // If the url exists add it to the hash and add
                // the hash to the list of links
            	linkFour.put("href", repoUrl);
            	linkList.add(linkFour);
            }
        }
        
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
    
    private String buildGitRepoUrl(AbstractBuild<?, ?> build) {
        // Attempt to build a URL for this repo.
        String url = null;
        try {
            SCM scm = build.getProject().getScm();
            if (scm instanceof GitSCM) {
            	GitRepositoryBrowser browser = ((GitSCM) scm).getBrowser();
            	if (browser instanceof GitWeb) {
            		url = ((GitWeb) browser).getUrl().toString();
            	} else if (browser instanceof GithubWeb) {
            		url  = ((GithubWeb) browser).getUrl().toString();
            	}
            }
        }    
        catch (Throwable t) {
            System.out.println("No git repo URL available.");
        }
        
        return url;
    }
    
    private String buildGitCommitUrl(String rev, AbstractBuild<?, ?> build) {
        // Attempt to build a URL for this changeset.
        String url = null;
        try {
            SCM scm = build.getProject().getScm();
            if (scm instanceof GitSCM) {
            	GitRepositoryBrowser browser = ((GitSCM) scm).getBrowser();
            	if (browser instanceof GitWeb) {
            		String baseURL = ((GitWeb) browser).getUrl().toString();
            		url = baseURL + "?a=commit&h=" + rev;
            	} else if (browser instanceof GithubWeb) {
            		String baseURL  = ((GithubWeb) browser).getUrl().toString();
            		url = baseURL + "commit/" + rev;
            	}
            }
        }    
        catch (Throwable t) {
            System.out.println("No git repo URL available.");
        }
        
        return url;
    }

    private void createClient(  )
    {
        client = new HttpClient(  );

        Credentials defaultcreds = new UsernamePasswordCredentials( serviceUser, token );
        client.getState(  ).setCredentials( AuthScope.ANY, defaultcreds );
        client.getParams(  ).setAuthenticationPreemptive( true );
    }
}
