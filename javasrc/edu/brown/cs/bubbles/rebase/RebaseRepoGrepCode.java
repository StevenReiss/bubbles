/********************************************************************************/
/*										*/
/*		RebaseRepoGrepCode.java 					*/
/*										*/
/*	description of class							*/
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class RebaseRepoGrepCode extends RebaseRepo
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Map<String,String> project_id_map = new HashMap<String,String>();

private final static String	GREPCODE_SCHEME = "http";
private final static String	GREPCODE_AUTHORITY = "grepcode.com";
private final static String	GREPCODE_PATH = "/search";
private final static String	GREPCODE_QUERY = "query=";
private final static String	GREPCODE_QUERY_TAIL = "&entity=type";
private final static String	GREPCODE_PROJECT_QUERY = "/snapshot/";




/********************************************************************************/
/*										*/
/*	Repository methods							*/
/*										*/
/********************************************************************************/

@Override List<URI> generateSearchURIs(List<String> toks,RebaseProject proj)
{
   List<URI> rslt = new ArrayList<URI>();

   for (int i = 0; i < 10; ++i) {
      String q = GREPCODE_QUERY;
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
	 String pid = project_id_map.get(proj.getName());
	 if (pid == null) pid = project_id_map.get(proj.getId());
	 if (pid == null) pid = findPidForProject(proj);
	 if (pid != null) q += " md5:" + pid;
       }
      q += GREPCODE_QUERY_TAIL;
      if (i > 0) q += "&start=" + i*10;
      try {
	 URI uri = new URI(GREPCODE_SCHEME,GREPCODE_AUTHORITY,GREPCODE_PATH,q,null);
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
   Elements keys = doc.select("div.search-result-item div.container-groups div.container-group:eq(0) div.result-list:eq(0) a.container-name");
   for (Element felt : keys) {
      GrepCodeFileSource fs = new GrepCodeFileSource(base,felt,rqst,orig);
      if (fs.isValid()) rslts.add(fs);
    }

   return true;
}




private static String findPidForProject(RebaseProject rp)
{
   String q = GREPCODE_PROJECT_QUERY + rp.getId().replace("@","/");
   try {
      URI uri = new URI(GREPCODE_SCHEME,GREPCODE_AUTHORITY,q,null,null);
      URL url = uri.toURL();
      String pstr = loadURL(url,true);
      Element doc = Jsoup.parse(pstr,url.toString());
      Elements md5 = doc.getElementsContainingOwnText("MD5 Signatures:");
      for (Element e1 : md5) {
	 Element e2 = e1.parent();
	 if (e2 == null) continue;
	 for (Element e3 : e2.getElementsByTag("span")) {
	    if (!e3.className().equals("")) continue;
	    String pid = e3.text();
	    if (pid != null && pid.length() > 0) {
	       project_id_map.put(rp.getId(),pid);
	       return pid;
	     }	
	  }
       }
    }
   catch (Exception e) {
      RebaseMain.logE("Problem with grepcode url: " + e,e);
    }

   return null;
}






/********************************************************************************/
/*										*/
/*	Ohloh Code Source							*/
/*										*/
/********************************************************************************/

private class GrepCodeFileSource extends BaseFileSource implements RebaseSource {

   private String project_id;
   private String project_name;
   private URL file_href;
   private String file_name;
   private String s6_source;

   GrepCodeFileSource(URL base,Element tag,RebaseRequest rqst,RebaseSource orig) {
      super(rqst,orig);
   
      file_href = null;
      project_id = null;
      project_name = null;
      file_name = null;
      s6_source = null;
   
      String href = tag.attr("href");
      String pnam = tag.attr("title");
      String vers = tag.text();
   
      int idx0 = href.lastIndexOf("#");
      if (idx0 > 0) href = href.substring(0,idx0);
   
      if (!href.startsWith("file/")) return;
      String h1 = href.substring(5);
      int idx1 = h1.indexOf(vers);
      if (idx1 < 0) return;
      idx1 += vers.length() + 1;
      String proj = h1.substring(0,idx1-1);
      String h3 = h1.substring(idx1);
      
      s6_source = "GREPCODE:" + href;
   
      try {
         String fref = GREPCODE_SCHEME + "://" + GREPCODE_AUTHORITY +
         "/file_/" + h1 + "/?v=source";
         file_href = new URI(fref).toURL();
        
         project_name = pnam.replace("/","@").replace("&","_");
         project_id = proj.replace("/","@");
        
         file_name = "/REBUS/" + project_id + "/GREPCODE/" + h3;
       }
      catch (URISyntaxException | MalformedURLException e) {
         file_href = null;
       }
    }


   boolean isValid() {
      return file_href != null && project_id != null;
    }


   @Override public RebaseRepo getRepository()		{ return RebaseRepoGrepCode.this; }
   @Override public String getProjectId()		{ return project_id; }
   @Override public String getProjectName()		{ return project_name; }
   @Override public String getPath()				{ return file_name; }
   @Override public String getS6Source()                { return s6_source; }

    @Override public String getText() {
      try {
         return loadSourceURL(file_href,true);
       }
      catch (RebaseException e) {
         RebaseMain.logE("REBASE: Problem loading file " + file_href + ": " + e,e);
       }
      return null;
    }

}	// end of inner class GrepCodeFileSource




}	// end of class RebaseRepoGrepCode




/* end of RebaseRepoGrepCode.java */

