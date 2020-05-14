/********************************************************************************/
/*										*/
/*		RebaseRepo.java 						*/
/*										*/
/*	Generic code for accessing a search engine/repository			*/
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


import edu.brown.cs.bubbles.rebase.word.RebaseWordFactory;

import edu.brown.cs.ivy.exec.IvyExec;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


abstract class RebaseRepo implements RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected RebaseRepo()
{ }



/********************************************************************************/
/*										*/
/*	Repository methods							*/
/*										*/
/********************************************************************************/

List<RebaseSource> getSources(String keys,RebaseRequest rqst,RebaseSource orig,RebaseProject proj)
	throws RebaseException
{
   List<String> toks = IvyExec.tokenize(keys);
   List<URI> uris = generateSearchURIs(toks,proj);
   if (uris == null) throw new RebaseException("No search query generated");

   List<RebaseSource> rslt = new ArrayList<RebaseSource>();

   for (URI uri : uris) {
      try {
	 URL url = uri.toURL();
	 String text = loadURL(url,true);
	 if (text == null) break;
	 Element doc = Jsoup.parse(text,url.toString());
	 if (!addSources(url,doc,rqst,orig,proj,rslt)) break;
       }
      catch (IOException e) {
	 RebaseMain.logE("Problem converting url " + uri + ": " + e,e);
       }
      catch (RebaseException e) {
	 RebaseMain.logI("Problem loading url " + uri + ": " + e);
       }
    }

   return rslt;
}




abstract List<URI> generateSearchURIs(List<String> toks,RebaseProject proj);

abstract protected boolean addSources(URL base,Element doc,RebaseRequest rqst,RebaseSource orig,RebaseProject proj,List<RebaseSource> rslts);




/********************************************************************************/
/*										*/
/*	Common routines for loading URLs					*/
/*										*/
/********************************************************************************/

protected static synchronized String loadURL(URL url,boolean cache) throws RebaseException
{
   RebaseMain.logD("LOAD URI " + url + " " + cache);

   RebaseCache urlcache = RebaseMain.getRebase().getUrlCache();

   StringBuilder buf = new StringBuilder();
   try {
      BufferedReader br = urlcache.getReader(url,cache);
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 buf.append(ln);
	 buf.append("\n");
       }
      br.close();
    }
   catch (IOException e) {
      RebaseMain.logD("Problem accessing url " + url + ": " + e);
      return null;
      // throw new RebaseException("Probelm accessing url " + url + ": " + e,e);
    }

   return buf.toString();
}


protected static String loadSourceURL(URL url,boolean cache) throws RebaseException
{
   String text = loadURL(url,cache);

   if (cache) {
      RebaseCache urlcache = RebaseMain.getRebase().getUrlCache();
      boolean fg = urlcache.wasAddedToCache(url);
      RebaseWordFactory.getFactory().loadSource(text,!fg);
    }

   return text;
}




/********************************************************************************/
/*										*/
/*	Common methods for getting source information				*/
/*										*/
/********************************************************************************/

static String findFileName(String text,String path)
{
   if (text == null || path == null) return null;
   String pats = "^\\s*package\\s+([A-Za-z0-9]+(\\w*\\.\\w*[A-Za-z0-9]+)*)\\w*\\;";
   Pattern pat = Pattern.compile(pats);
   Matcher mat = pat.matcher(text);
   int idx = path.lastIndexOf("/");
   String file = (idx < 0 ? path : path.substring(idx+1));
   if (mat.find()) {
      String pkg = mat.group(1);
      StringTokenizer tok = new StringTokenizer(pkg,". \t\n\f");
      StringBuffer buf = new StringBuffer();
      while (tok.hasMoreTokens()) {
	 String elt = tok.nextToken();
	 buf.append(elt);
	 buf.append("/");
       }
      buf.append(file);
      file = buf.toString();
    }
   return file;
}


static String findPackageName(String text)
{
   if (text == null) return null;
   String pats = "^\\s*package\\s+([A-Za-z0-9]+(\\w*\\.\\w*[A-Za-z0-9]+)*)\\w*\\;";
   Pattern pat = Pattern.compile(pats);
   Matcher mat = pat.matcher(text);
   if (!mat.find()) return "";

   String pkg = mat.group(1);
   StringTokenizer tok = new StringTokenizer(pkg,". \t\n\f");
   StringBuffer buf = new StringBuffer();
   int ctr = 0;
   while (tok.hasMoreTokens()) {
      String elt = tok.nextToken();
      if (ctr++ > 0) buf.append(".");
      buf.append(elt);
    }
   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Basic Source implementations						*/
/*										*/
/********************************************************************************/

protected abstract class BaseFileSource implements RebaseSource {

   private RebaseRequest	source_request;
   private RebaseSource 	base_source;
   private SourceType		source_type;

   protected BaseFileSource(RebaseRequest rr,RebaseSource orig) {
      source_request = rr;
      base_source = orig;
      if (rr == null) source_type = SourceType.FILE;
      else source_type = rr.getSearchType();
    }

   @Override public SourceType getSourceType()		{ return source_type; }

   @Override public RebaseRequest getRequest()		{ return source_request; }
   @Override public SourceLanguage getLanguage() {
      String nm = getPath();
      if (nm.endsWith(".java")) return SourceLanguage.JAVA;
      return null;
    }

   @Override public RebaseSource getBaseSource() {
      if (base_source == null) return this;
      return base_source.getBaseSource();
    }

   @Override public void setSourceType(SourceType st)	{ source_type = st; }

}	// end of inner class BaseFileSource














}	// end of class RebaseRepo




/* end of RebaseRepo.java */

