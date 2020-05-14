/********************************************************************************/
/*										*/
/*		NobaseProjectWeb.java						*/
/*										*/
/*	Javascript for Web Pages project (front end)				*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.file.IvyFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;


class NobaseProjectWeb extends NobaseProject
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final Set<String> html_extensions;

static {
   html_extensions = new HashSet<String>();
   html_extensions.add(".html");
   html_extensions.add(".vel");
   html_extensions.add(".handlebars");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseProjectWeb(NobaseMain pm,String name,File base)
{
   super(pm,name,base);
}


/********************************************************************************/
/*										*/
/*	Setup initial paths							*/
/*										*/
/********************************************************************************/

@Override void setupDefaults()
{
}


@Override protected void findFiles(String pfx,File f,boolean reload)
{
   if (!f.isDirectory()) {
      // first update cache for all script files that aren't local
      for (NobasePathSpec ps : project_paths) {
	 if (!ps.isUser() || ps.isExclude()) {
	    if (ps.match(f)) return;
	  }
       }
      String mnm = f.getName();
      int idx = mnm.lastIndexOf(".");
      if (idx > 0 && html_extensions.contains(mnm.substring(idx).toLowerCase())) {
	 findScripts(f);
	 return;
       }
    }

  super.findFiles(pfx,f,reload);
}


private void findScripts(File f)
{
   try (BufferedReader br = new BufferedReader(new FileReader(f))) {
      PrintWriter outf = null;
      int outlno = 1;
      int lno = 1;
      boolean inscript = false;
      for ( ; ; ) {
	  String line = br.readLine();
	  if (line == null) break;
	  String cnts = line.trim().toLowerCase();
	  if (inscript) {
	     if (cnts.startsWith("</script>")) {
		inscript = false;
	      }
	     if (outf != null) outf.println(cnts);
	     ++outlno;
	   }
	  if (cnts.startsWith("<script")) {
	     if (handleScriptLine(line)) {
		// write blank lines to outf to get to right line number
		inscript = true;
		if (outf == null) {
		   File f1 = new File(base_directory,"scripts");
		   File f2 = new File(f1,f.getName());
		   outf = new PrintWriter(new FileWriter(f2));
		   while (outlno < lno) {
		      outf.println();
		      ++outlno;
		    }
		   int idx1 = line.indexOf(">");
		   if (idx1 > 0) outf.println(line.substring(idx1+1));
		   else outf.println();
		   ++outlno;
		 }
	      }
	   }
	  ++lno;
       }

      String cnts = IvyFile.loadFile(f);
      // we should do this without using jsoup so we can track line numbers
      Element doc = Jsoup.parse(cnts,"file://");
      for (Element script : doc.getElementsByTag("script")) {
	 String src = script.attr("src");
	 if (src != null && src.startsWith("http:")) {
	    cacheScript(src);
	  }
	 else if (src == null) {
	    // String scriptcnts = script.text();
	    // create a file containing only the scripts (at the right lines?)
	  }
       }
    }
   catch (IOException e) { }
}



private boolean handleScriptLine(String cnts)
{
   return false;
}



private void cacheScript(String src)
{
   try {
      int idx = src.lastIndexOf("/");
      String base = src.substring(idx+1);
      File f0 = new File(base_directory,"cache");
      File cfile = new File(f0,base);
      URL u = new URL(src);
      URLConnection uconn = u.openConnection();
      if (cfile.exists()) uconn.setIfModifiedSince(cfile.lastModified());
      InputStream ins = uconn.getInputStream();
      String cnts = IvyFile.loadFile(new InputStreamReader(ins,"utf-8"));
      if (cnts != null && cnts.length() > 0) {
	 FileWriter fw = new FileWriter(cfile);
	 fw.write(cnts);
	 fw.close();
       }
      ins.close();
    }
   catch (IOException e) { }
}



}	// end of class NobaseProjectWeb




/* end of NobaseProjectWeb.java */

