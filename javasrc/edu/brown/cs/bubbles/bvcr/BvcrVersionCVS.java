/********************************************************************************/
/*										*/
/*		BvcrVersionCVS.java						*/
/*										*/
/*	Bubble Version Collaboration Repository interface to CVS		*/
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


package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.board.BoardProperties;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;


class BvcrVersionCVS extends BvcrVersionManager implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		cvs_root;
private String		cvs_command;

private static BoardProperties bvcr_properties = BoardProperties.getProperties("Bvcr");

private static SimpleDateFormat CVS_DATE = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss ZZZZZ");





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrVersionCVS(BvcrProject bp)
{
   super(bp);

   cvs_command = bvcr_properties.getProperty("bvcr.cvs.command","cvs");

   findCvsRoot();
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static BvcrVersionManager getRepository(BvcrProject bp,File srcdir)
{
   if (srcdir == null) return null;

   File f1 = new File(srcdir,"CVS");
   if (f1.exists() && f1.isDirectory()) {
      System.err.println("BVCR: HANDLE CVS REPOSITORY " + srcdir);
      return new BvcrVersionCVS(bp);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override File getRootDirectory() 		{ return cvs_root; }

@Override String getRepositoryName()
{
   return super.getRepositoryName();
}


@Override String getRepoType()
{
   return "CVS";
}


@Override void getDifferences(BvcrDifferenceSet ds)
{
   String cmd = cvs_command  + " diff -R -b";

   String v0 = ds.getStartVersion();
   String v1 = ds.getEndVersion();
   if (v0 != null) {
      cmd += " -r " + v0;
      if (v1 != null) cmd += " -r " + v1;
    }

   List<File> diffs = ds.getFilesToCompute();
   if (diffs == null) {
      cmd += " " + cvs_root.getPath();
    }
   else if (diffs.size() == 0) return;
   else {
      for (File f : diffs) {
	 cmd += " " + getRelativePath(f);
       }
    }

   CvsDiffAnalyzer da = new CvsDiffAnalyzer(ds);
   runCommand(cmd,da);
}




/********************************************************************************/
/*										*/
/*	Find the top of the CVS repository					*/
/*										*/
/********************************************************************************/

private void findCvsRoot()
{
   File f = new File(for_project.getSourceDirectory());
   for ( ; ; ) {
      File fp = f.getParentFile();
      File fp1 = new File(fp,"CVS");
      if (!fp1.exists()) break;
      f = fp;
    }
   cvs_root = f;
}



/********************************************************************************/
/*										*/
/*	History gathering for a file						*/
/*										*/
/********************************************************************************/

@Override void findHistory(File f,IvyXmlWriter xw)
{
   StringCommand cmd = new StringCommand(cvs_command + " log " + getRelativePath(f));
   String rslt = cmd.getContent();

   StringTokenizer tok = new StringTokenizer(rslt,"\n");
   String rev = null;
   Date d = null;
   String auth = null;
   String msg = null;
   BvcrFileVersion prior = null;
   Collection<BvcrFileVersion> fvs = new ArrayList<BvcrFileVersion>();
   String headversion = null;

   while (tok.hasMoreTokens()) {
      String ln = tok.nextToken();
      if (rev == null) {
	 if (ln.startsWith("revision ")) rev = ln.substring(9).trim();
	 else if (ln.startsWith("head: ")) {
	    headversion = ln.substring(6);
	  }
       }
      else {
	 if (ln.startsWith("date: ")) {
	    StringTokenizer ltok = new StringTokenizer(ln,";");
	    while (ltok.hasMoreTokens()) {
	       String itm = ltok.nextToken();
	       int idx = itm.indexOf(":");
	       if (idx >= 0) {
		  String what = itm.substring(0,idx).trim();
		  String cnts = itm.substring(idx+1).trim();
		  if (what.equals("date")) {
		     try {
			d = CVS_DATE.parse(cnts);
		      }
		     catch (ParseException e) { }
		   }
		  else if (what.equals("author")) auth = cnts;
		}
	     }
	  }
	 else if (ln.startsWith("----------------------------") ||
		     ln.startsWith("===================================================")) {
	    if (auth != null && d != null) {
	       BvcrFileVersion fv = new BvcrFileVersion(f,rev,d,auth,msg);
	       if (headversion != null && headversion.equals(rev)) {
		  fv.addAlternativeName("HEAD");
		}
	       if (prior != null) prior.addPriorVersion(fv);
	       prior = fv;
	       fvs.add(fv);
	     }
	    rev = null;
	    d = null;
	    msg = null;
	    auth = null;
	  }
	 else if (msg == null) msg = ln;
	 else msg = msg + "\n" + ln;
       }
    }

   xw.begin("HISTORY");
   xw.field("FILE",f.getPath());
   for (BvcrFileVersion fv : fvs) {
      fv.outputXml(xw);
    }
   xw.end("HISTORY");
}




/********************************************************************************/
/*                                                                              */
/*      Handle project history                                                  */
/*                                                                              */
/********************************************************************************/

@Override void findProjectHistory(IvyXmlWriter xw) 
{ }


@Override void showChangedFiles(IvyXmlWriter xw,boolean ign)
{ }


@Override void ignoreFiles(IvyXmlWriter xw,List<String> files) 
{ }

@Override void addFiles(IvyXmlWriter xw,List<String> files) 
{ }

@Override void removeFiles(IvyXmlWriter xw,List<String> files) 
{ }


@Override void doCommit(IvyXmlWriter xw,String msg) 
{ }


@Override void doPush(IvyXmlWriter xw) 
{ }


@Override void doUpdate(IvyXmlWriter xw,boolean keep, boolean remove) 
{ }


@Override void doSetVersion(IvyXmlWriter xw,String ver) 
{ }


@Override void doStash(IvyXmlWriter xw,String name) 
{ }




/********************************************************************************/
/*										*/
/*	CVS diff analyzer							*/
/*										*/
/********************************************************************************/

protected static class CvsDiffAnalyzer implements CommandCallback {

   private BvcrDifferenceSet diff_set;
   private int source_line;
   private int target_line;
   private int del_count;
   private String base_version;
   private String cur_file;

   CvsDiffAnalyzer(BvcrDifferenceSet ds) {
      diff_set = ds;
      source_line = 0;
      target_line = 0;
      del_count = 0;
      base_version = null;
      cur_file = null;
    }

   @Override public void handleLine(String ln) {
      if (ln.length() == 0) return;
      char ch = ln.charAt(0);
      switch (ch) {
         case 'I' :                     // Index: <file>
            cur_file = ln.substring(7);
            source_line = 0;
            del_count = 0;
            break;
         case 'c' :                     // cvs diff: ...
         case '=' :                     // =============
         case 'd' :                     // diff -rxxx <filebase>
         default :
            source_line = 0;
            del_count = 0;
            break;
         case 'r' :                     // retrieving revision
            int idx1 = ln.lastIndexOf(" ");
            base_version = ln.substring(idx1+1);
            diff_set.beginFile(cur_file,base_version);
            break;
         case '0' : case '1' : case '2' : case '3' : case '4' :
         case '5' : case '6' : case '7' : case '8' : case '9' :
            StringTokenizer tok = new StringTokenizer(ln,"acd,",true);
            int ln0 = 0;
            int ln1 = 0;
            String op = null;
            while (tok.hasMoreTokens()) {
               String t = tok.nextToken();
               if (t.equals(",")) {
        	  String n = tok.nextToken();
        	  if (op == null) ln1 = Integer.parseInt(n);
        	}
               else if (Character.isDigit(t.charAt(0))) {
        	  if (op == null) {
                     ln0 = Integer.parseInt(t);
                     ln1 = ln0;
                   }
        	}
               else op = t;
             }
            source_line = ln0;
            target_line = ln1;
            del_count = 0;
            break;
         case '<' :                     // < (source line)
            if (source_line != 0) {
               diff_set.noteDelete(source_line,target_line,ln.substring(1));
               ++source_line;
               ++del_count;
             }
            break;
         case '>' :
            if (source_line != 0) {
               diff_set.noteInsert(source_line - del_count,target_line,ln.substring(1));
               ++target_line;
             }
            break;
       }
    }

   @Override public void handleDone(int sts) {
      diff_set.finish();
    }

}	// end of inner class CvsDiffAnalyzer





}	// end of class BvcrVersionCVS



/* end of BvcrVersionCVS.java */

