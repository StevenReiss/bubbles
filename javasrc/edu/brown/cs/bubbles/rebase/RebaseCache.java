/********************************************************************************/
/*										*/
/*		RebaseCache.java						*/
/*										*/
/*	Handle caching of URL requests						*/
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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;



class RebaseCache implements RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		base_directory;
private boolean 	use_cache;
private Set<URL>	added_urls;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseCache()
{
   base_directory = null;
   use_cache = false;
   added_urls = new HashSet<URL>();
}



/********************************************************************************/
/*										*/
/*	Setup caching							       */
/*										*/
/********************************************************************************/

void setupCache(String dir,boolean use)
{
   File fdir = new File(dir);
   base_directory = fdir;
   use_cache = use;
   if (!base_directory.exists()) base_directory.mkdirs();
   if (!base_directory.exists() || !base_directory.isDirectory()) use_cache = false;
   if (!base_directory.canWrite()) use_cache = false;
}



/********************************************************************************/
/*										*/
/*	Main entry points							*/
/*										*/
/********************************************************************************/

BufferedReader getReader(URL url,boolean cache) throws IOException
{
   if (!use_cache || !cache) {
      return getURLConnection(url);
    }

   File dir = getDirectory(url);
   if (dir != null) {
      File df = new File(dir,CACHE_DATA_FILE);
      return new BufferedReader(new FileReader(df));
    }

   return getURLConnection(url);
}



boolean wasAddedToCache(URL url)
{
   if (!use_cache) return true;

   return added_urls.contains(url);
}



/********************************************************************************/
/*										*/
/*	HTTP methods								*/
/*										*/
/********************************************************************************/

private BufferedReader getURLConnection(URL url) throws IOException
{
   URLConnection uc = url.openConnection();
   uc.setReadTimeout(60000);
   uc.setAllowUserInteraction(false);
   uc.setDoOutput(false);
   uc.addRequestProperty("Connection","close");

   // if returned a 420 status, then delay for 10 seconds and try again

   return new BufferedReader(new InputStreamReader(uc.getInputStream(),"UTF-8"));
}



/********************************************************************************/
/*										*/
/*	Directory methods							*/
/*										*/
/********************************************************************************/

private File getDirectory(URL u) throws IOException
{
   if (u == null) return null;

   String un = u.toExternalForm().toLowerCase();
   if (un.length() == 0) return null;
   int hvl = 0;
   try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte [] dvl = md.digest(un.getBytes());
      for (int i = 0; i < dvl.length; ++i) {
	 int j = i % 4;
	 int x = (dvl[i] & 0xff);
	 hvl ^= (x << (j*8));
       }
    }
   catch (NoSuchAlgorithmException e) {
      hvl = un.hashCode();
    }
   hvl &= 0x7fffffff;
   int h1 = hvl % 512;
   int h2 = (hvl/512) % 512;
   int h3 = (hvl/512/512) % 4096;

   File dtop = new File(base_directory,"S6$" + h1);
   dtop = new File(dtop,"S6$" + h2);
   if (!dtop.exists() && !dtop.mkdirs()) return null;

   String dir0 = "S6$" + h3;
   for (int i = 0; i < 26*27; ++i) {
      StringBuilder sb = new StringBuilder(dir0);
      int j0 = i % 26;
      int j1 = i / 26;
      if (j1 > 0) sb.append((char)('a' + j1 - 1));
      sb.append((char)('a' + j0));
      File dir = new File(dtop,sb.toString());
      File urlf = new File(dir,CACHE_URL_FILE);
      boolean fg = dir.mkdirs();
      if (!fg && !dir.exists()) return null;

      if (fg) { 			// we own the directory
	 BufferedReader br = getURLConnection(u);	// throw exception on bad url
         File df = new File(dir,CACHE_DATA_FILE);
	 try (FileWriter dw = new FileWriter(df)) {
	    char [] buf = new char[8192];
	    for ( ; ; ) {
	       int ln = br.read(buf);
	       if (ln < 0) break;
	       dw.write(buf,0,ln);
	     }
	    br.close();
	    try (FileWriter fw = new FileWriter(urlf)) {
               fw.write(u.toExternalForm() + "\n");
             }
	    added_urls.add(u);
	    return dir;
	  }
	 catch (IOException e) {
	    System.err.println("REBASE: Failed to create URL cache file: " + e);
	    return null;
	  }
       }
      else {				// directory already exists
	 for (int k = 0; k < 20 && !urlf.exists(); ++k) {
	    try {
	       Thread.sleep(1);
	     }
	    catch (InterruptedException e) { }
	  }
	 if (!urlf.exists()) return null;
         System.err.println("S6: Use CACHE: " + dir);
         try (BufferedReader br = new BufferedReader(new FileReader(urlf))) {
	    String ln = br.readLine();
	    if (ln == null) continue;
	    ln = ln.trim();
	    if (ln.equalsIgnoreCase(u.toExternalForm())) {
	       added_urls.remove(u);
	       return dir;
	     }
	  }
	 catch (IOException e) {
	    System.err.println("REBASE: Problem reading URL cache file: " + e);
	  }
       }
    }

   return null;
}





}	// end of class RebaseCache




/* end of RebaseCache.java */

