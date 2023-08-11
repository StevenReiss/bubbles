/********************************************************************************/
/*										*/
/*		RebaseRepoGithub.java						*/
/*										*/
/*	Github search interface for REBASE					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.rebase;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


class RebaseRepoGithub extends RebaseRepo
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final static String	GITHUB_SCHEME = "https";
private final static String	GITHUB_AUTHORITY = "github.com";
private final static String	GITHUB_PATH = "/search";
private final static String	GITHUB_QUERY = "l=Java&q=";
private final static String	GITHUB_QUERY_TAIL = "&ref=advsearch&type=Code";
private final static String	GITHUB_FILE_AUTHORITY = "raw.github.com";



/********************************************************************************/
/*										*/
/*	Repository methods							*/
/*										*/
/********************************************************************************/

@Override List<URI> generateSearchURIs(List<String> toks,RebaseProject proj)
{
   List<URI> rslt = new ArrayList<URI>();

   for (int i = 0; i < 10; ++i) {
      String q = GITHUB_QUERY;
      int wct = 0;
      for (String s : toks) {
	 if (s.startsWith("mdef:")) s = s.substring(5);
	 else if (s.startsWith("cdef:")) s = "class " + s.substring(5);
	 else if (s.startsWith("idef:")) s = "interface " + s.substring(5);
	 if (wct++ > 0) q += " ";
	 if (s.contains(" ")) {
	    q += "\"" + s + "\"";
	  }
	 else q += s;
       }
      if (proj != null) {
	 q += " repo:" + proj.getId();
       }
      q += GITHUB_QUERY_TAIL;
      if (i > 0) q += "&p=" + (i+1);
      try {
	 URI uri = new URI(GITHUB_SCHEME,GITHUB_AUTHORITY,GITHUB_PATH,q,null);
	 rslt.add(uri);
       }
      catch (URISyntaxException e) {
	 RebaseMain.logE("Problem with ohloh url: " + e,e);
       }
    }

   return rslt;
}


@Override protected boolean addSources(URL base,Element doc,RebaseRequest rqst,RebaseSource orig,
      RebaseProject proj,List<RebaseSource> rslts)
{
   Elements keys = doc.select("div.code-list-item p.title a:eq(1)");
   if (keys.size() == 0) return false;
   for (Element elt : keys) {
      GithubFileSource fs = new GithubFileSource(base,elt,rqst,orig);
      if (fs.isValid()) rslts.add(fs);
    }

   return true;
}





/********************************************************************************/
/*										*/
/*	Ohloh Code Source							*/
/*										*/
/********************************************************************************/

private class GithubFileSource extends BaseFileSource implements RebaseSource {

   private String project_id;
   private String project_name;
   private URL file_href;
   private String file_name;
   private String s6_source;

   GithubFileSource(URL base,Element tag,RebaseRequest rqst,RebaseSource orig) {
      super(rqst,orig);
   
      file_href = null;
      project_id = null;
      project_name = null;
      file_name = null;
      s6_source = null;
   
      String href = tag.attr("href");
   
      int idx = href.indexOf("/blob/");
      if (idx < 0) return;
   
      String proj = href.substring(1,idx);
      String rem = href.substring(idx+6);
   
      try {
         String fref = GITHUB_SCHEME + "://" + GITHUB_FILE_AUTHORITY +
            "/" + proj + "/" + rem;
         file_href = new URI(fref).toURL();
        
         project_name = proj.replace("/","@").replace("&","_");
         project_id = proj;
        
         file_name = "/REBUS/" + project_name + "/GITHUB/" + rem;
        
         s6_source = "GITHUB:" + href;
       }
      catch (URISyntaxException | MalformedURLException e) {
         file_href = null;
       }
    }


   boolean isValid() {
      return file_href != null && project_id != null;
    }


   @Override public RebaseRepo getRepository()		{ return RebaseRepoGithub.this; }
   @Override public String getProjectId()		{ return project_id; }
   @Override public String getProjectName()		{ return project_name; }
   @Override public String getPath()				{ return file_name; }
   @Override public String getS6Source()		{ return s6_source; }

   @Override public String getText() {
      try {
         return loadSourceURL(file_href,true);
       }
      catch (RebaseException e) {
         RebaseMain.logE("REBASE: Problem loading file " + file_href + ": " + e,e);
       }
      return null;
    }

}	// end of inner class GithubFileSource



}	// end of class RebaseRepoGithub							  >G




/* end of RebaseRepoGithub.java */

