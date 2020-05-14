/********************************************************************************/
/*										*/
/*		RebaseWordCacheSetup.java					*/
/*										*/
/*	Class to setup base word bag from cache information			*/
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



package edu.brown.cs.bubbles.rebase.word;

import edu.brown.cs.ivy.file.IvyFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RebaseWordCacheSetup implements RebaseWordConstants
{


/********************************************************************************/
/*										*/
/*	Main Program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   RebaseWordCacheSetup wcs = new RebaseWordCacheSetup(args);
   wcs.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		cache_directory;
private File		word_file;
private RebaseWordBag	word_bag;
private long		after_date;
private File		tar_file;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private RebaseWordCacheSetup(String [] args)
{
   cache_directory = new File("/ws/volfred/s6/cache");
   word_file = new File(System.getProperty("user.home") + "/.rebus/allwords.bag.zip");
   word_bag = new RebaseWordBag();
   after_date = 0;
   tar_file = null;

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (cache_directory != null) {
      if (!cache_directory.isDirectory() || !cache_directory.exists() ||
	     !cache_directory.canRead()) {
	 System.err.println("CacheSetup: Can't use cache directory " + cache_directory);
	 return;
       }
      processFiles(cache_directory);
    }
   else if (tar_file != null) {
      processTarFile();
    }

   try {
      word_bag.outputBag(word_file);
    }
   catch (IOException e) {
      System.err.println("CacheSetup: Problem saving word bag: " + e);
    }
}



private void processFiles(File dir)
{
   File f1 = new File(dir,"URL");
   if (f1.exists()) {
      processUrl(dir,f1);
    }
   else {
      for (File f : dir.listFiles()) {
	 if (f.isDirectory()) processFiles(f);
       }
    }
}



private void processUrl(File dir,File urlf)
{
   if (urlf.lastModified() < after_date) return;

   try {
      BufferedReader br = new BufferedReader(new FileReader(urlf));
      String url = br.readLine();
      br.close();
      if (url == null) return;
      if (!url.endsWith(".java")) return;
      File f1 = new File(dir,"DATA");
      String txt = IvyFile.loadFile(f1);
      word_bag.addWords(txt);
    }
   catch (IOException e) {
      System.err.println("CacheSetup: I/O problem on cache file: " + e);
    }
}



private void processTarFile()
{
   try {
      FileInputStream fis = new FileInputStream(tar_file);
      TarArchiveInputStream tis = new TarArchiveInputStream(new BufferedInputStream(fis));

      TarArchiveEntry ent = null;
      while ((ent = tis.getNextTarEntry()) != null) {
	 if (ent.getName().endsWith(".java")) {
	    InputStreamReader isr = new InputStreamReader(tis);
	    String txt = IvyFile.loadFile(isr);
	    word_bag.addWords(txt);
	  }
       }

      fis.close();
    }
   catch (IOException e) {
      System.err.println("CacheSetup: I/O problem on tar file: " + e);
    }
}




/********************************************************************************/
/*										*/
/*	Argument processing methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   boolean havecache = false;
   boolean update = false;
   boolean havedate = false;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-c") && i+1 < args.length) {           // -cache <cache>
	    if (havecache) badArgs();
	    havecache = true;
	    cache_directory = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-w") && i+1 < args.length) {      // -words <word file>
	    word_file = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-u")) {                           // -updated
	    update = true;
	  }
	 else if (args[i].startsWith("-d")) {                           // -date
	    after_date = 0;
	    havedate = true;
	  }
	 else if (args[i].startsWith("-t") && i+1 < args.length) {      // -tar <tar file>
	    tar_file = new File(args[++i]);
	    cache_directory = null;
	    update = false;
	  }
	 else badArgs();
       }
      else if (!havecache) {
	 havecache = true;
	 cache_directory = new File(args[++i]);
       }
      else badArgs();
    }

   if (update) {
      try {
	 word_bag.inputBag(word_file);
	 if (!havedate) after_date = word_file.lastModified();
       }
      catch (IOException e) {
	 System.err.println("CacheSetup: Problem reading old word bag: " + e);
	 System.exit(1);
       }
    }
}




private void badArgs()
{
   System.err.println("CacheSetup: cachesetup [-cache <cache>] [-words <wordfile>] [-update]");
}




}	// end of class RebaseWordCacheSetup




/* end of RebaseWordCacheSetup.java */

