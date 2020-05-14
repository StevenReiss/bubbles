/********************************************************************************/
/*										*/
/*		RebaseRepoOhloh.java						*/
/*										*/
/*	Code to access Ohloh Search engine					*/
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


class RebaseRepoOhloh extends RebaseRepo
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Map<String,String> project_id_map = new HashMap<>();


private final static String	OHLOH_SCHEME = "http";
private final static String	OHLOH_AUTHORITY = "code.openhub.net";
private final static String	OHLOH_PATH = "/search";
private final static String	OHLOH_QUERY = "s=";
private final static String	OHLOH_QUERY_TAIL = "&fl=Java";
private final static String	OHLOH_PROJECT_PATH = "/project";
private final static String 	OHLOH_PROJECT_QUERY = "pid=";




/********************************************************************************/
/*										*/
/*	Repository methods							*/
/*										*/
/********************************************************************************/

@Override List<URI> generateSearchURIs(List<String> toks,RebaseProject proj)
{
   List<URI> rslt = new ArrayList<URI>();

   for (int i = 0; i < 10; ++i) {
      String q = OHLOH_QUERY;
      int wct = 0;
      for (String s : toks) {
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
         if (pid != null) q += "&fp=" + pid;
       }
      q += OHLOH_QUERY_TAIL;
      if (i > 0) q += "&p=" + i;
      try {
	 URI uri = new URI(OHLOH_SCHEME,OHLOH_AUTHORITY,OHLOH_PATH,q,null);
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
   Elements pmap = doc.select("#fp div.facetList");
   for (Element melt : pmap) {
      String val = null;
      Elements e1 = melt.getElementsByTag("input");
      for (Element ielt : e1) {
         val = ielt.val();
         break;
       }
      Elements e2 = melt.getElementsByClass("tileText");
      for (Element nelt : e2) {
         String text = nelt.text();
         project_id_map.put(text,val);
       }
    }
   
   Elements keys = doc.select("div.snippet_header div.projectNameLabel a");
   Elements pths = doc.select("div.snippet_header div.fileNameLabel a");
   int ct = Math.min(keys.size(),pths.size());
   if (ct == 0) return false;
   for (int i = 0; i < ct; ++i) {
      Element pelt = keys.get(i);
      Element felt = pths.get(i);
      OhlohFileSource fs = new OhlohFileSource(base,pelt,felt,rqst,orig);
      if (fs.isValid()) rslts.add(fs);
    }

   return true;
}


private static String findPidForProject(RebaseProject rp)
{
   String q = OHLOH_PROJECT_QUERY + rp.getId();
   try {
      URI uri = new URI(OHLOH_SCHEME,OHLOH_AUTHORITY,OHLOH_PROJECT_PATH,q,null);
      URL url = uri.toURL();
      String pstr = loadURL(url,true);
      Element doc = Jsoup.parse(pstr,url.toString());
      Element inp = doc.getElementById("selectedProject");
      if (inp != null) {
	 String val = inp.val();
	 if (val != null) project_id_map.put(rp.getId(),val);
	 return val;
       }
   }
   catch (Exception e) {
      RebaseMain.logE("Problem with ohloh url: " + e,e);
   }
   
   return null;
}




/********************************************************************************/
/*										*/
/*	Ohloh Code Source							*/
/*										*/
/********************************************************************************/

private class OhlohFileSource extends BaseFileSource implements RebaseSource {

   private String project_id;
   private String project_name;
   private URL file_href;
   private String file_name;
   private String s6_source;

   OhlohFileSource(URL base,Element ptag,Element ftag,RebaseRequest rqst,RebaseSource orig) {
      super(rqst,orig);
      
      s6_source = null;
      project_name = null;
      file_href = null;
      file_name = null;
      project_id = null;
      
      String url = ptag.attr("href");
   
      project_name = ptag.attr("title").replace("/","@").replace("&","_");
      int idx = url.indexOf("pid=");
      int idx1 = url.indexOf("&",idx);
      if (idx > 0 && idx1 > 0) project_id = url.substring(idx+4,idx1);
      else if (idx > 0) project_id = url.substring(idx+4);
      else project_id = null;
   
      url = ftag.attr("href");
      String key = ftag.attr("title");
      try {
         String fid = getParam(url,"fid");
         String cid = getParam(url,"cid");
         idx1 = key.lastIndexOf("/");
         if (fid != null && cid != null) {
            s6_source = "OHLOH:/file?fid=" + fid + "&cid=" + cid;
            file_name = "/REBUS/" + project_id + "/OHLOH/" + cid + "/" + fid + "/" + key;
            int idx2 = url.indexOf("?");
            String nurl = url.substring(0,idx2+1);
            nurl += "fid=" + fid + "&cid=" + cid + "&dl=undefined";
            file_href = new URL(base,nurl);
          }
         else file_href = null;
       }
      catch (MalformedURLException e) {
         file_href = null;
       }
    }


   boolean isValid() {
      return file_href != null && project_id != null;
    }

   
   @Override public RebaseRepo getRepository()		{ return RebaseRepoOhloh.this; }
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

   private String getParam(String url,String id) {
      int idx = url.indexOf(id + "=");
      if (idx < 0) return null;
      idx += id.length() + 1;
      int idx1 = url.indexOf("&",idx);
      if (idx1 < 0) return url.substring(idx);
      return url.substring(idx,idx1);
    }

}	// end of inner class OhlohFileSource


}	// end of class RebaseRepoOhloh




/* end of RebaseRepoOhloh.java */

