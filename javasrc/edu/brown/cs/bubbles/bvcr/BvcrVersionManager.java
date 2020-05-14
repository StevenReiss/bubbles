/********************************************************************************/
/*										*/
/*		BvcrVersionManager.java 					*/
/*										*/
/*	Bubble Version Collaboration Repository generic version manager iface	*/
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


import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




abstract class BvcrVersionManager implements BvcrConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected BvcrProject	for_project;
private BvcrMonitor for_monitor;
protected String	repository_id;



/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static BvcrVersionManager createVersionManager(BvcrProject bp,BvcrMonitor mon)
{
   File f = new File(bp.getSourceDirectory());
   if (f == null || !f.exists() || !f.isDirectory()) return null;

   BvcrVersionManager bvm = null;

   System.err.println("BVCR: check repository for " + f);

   if (bvm == null) bvm = BvcrVersionGIT.getRepository(bp,f);
   if (bvm == null) bvm = BvcrVersionSVN.getRepository(bp,f);
   if (bvm == null) bvm = BvcrVersionCVS.getRepository(bp,f);
   // handle HG

   if (bvm != null) bvm.for_monitor = mon;

   return bvm;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BvcrVersionManager(BvcrProject bp)
{
   for_project = bp;
   repository_id = null;
}



/********************************************************************************/
/*										*/
/*	Generic operations on the version management system			*/
/*										*/
/********************************************************************************/

String getRepositoryId()
{
   if (repository_id == null) {
      File f = getRootDirectory();
      File f0 = new File(f,".uid");
      if (f0.exists()) {
	 try (BufferedReader br = new BufferedReader(new FileReader(f0))) {
	    for ( ; ; ) {
	       String ln = br.readLine();
	       if (ln == null) break;
	       ln = ln.trim();
	       if (ln.length() > 0) {
		  repository_id = ln;
		  break;
		}
	     }
	  }
	 catch (IOException e) { }
       }
      else {
	 generateRepositoryId(f0);
       }
    }

   return repository_id;
}



private void generateRepositoryId(File f)
{
   UUID u = UUID.randomUUID();

   try {
      FileWriter fw = new FileWriter(f);
      fw.write(u.toString() + "\n");
      fw.close();
    }
   catch (IOException e) {
      System.err.println("BVCR: Can't create .uid file " + f.getAbsolutePath());
      e.printStackTrace();
      return;
    }

   List<String> files = new ArrayList<String>();
   files.add(f.getPath());
   IvyXmlWriter xw = new IvyXmlWriter();
   addFiles(xw,files);
   xw.close();
}


String getRepositoryName()
{
   return getRepositoryId();
}


abstract String getRepoType();



abstract File getRootDirectory();
abstract void getDifferences(BvcrDifferenceSet ds);
abstract void findHistory(File f,IvyXmlWriter xw);

abstract void findProjectHistory(IvyXmlWriter xw);
abstract void showChangedFiles(IvyXmlWriter xw,boolean ign);
abstract void ignoreFiles(IvyXmlWriter xw,List<String> files);
abstract void addFiles(IvyXmlWriter xw,List<String> files);
abstract void removeFiles(IvyXmlWriter xw,List<String> files);

abstract void doCommit(IvyXmlWriter xw,String msg);
abstract void doPush(IvyXmlWriter xw);
abstract void doUpdate(IvyXmlWriter xw,boolean keep,boolean remove);
abstract void doSetVersion(IvyXmlWriter xw,String ver);
abstract void doStash(IvyXmlWriter xw,String name);




/********************************************************************************/
/*										*/
/*	File differencing commands						*/
/*										*/
/********************************************************************************/

void findFileDiffs(BvcrMain bm,File f,String vfr,String vto,IvyXmlWriter xw)
{
   BvcrDifferenceSet ds = new BvcrDifferenceSet(bm,for_project);
   ds.setForFileDifference(f,vfr,vto);

   getDifferences(ds);

   ds.outputXml(xw);
}




/********************************************************************************/
/*										*/
/*	Methods to run commands and provide the output				*/
/*										*/
/********************************************************************************/

protected interface CommandCallback {

   void handleLine(String line);
   void handleDone(int sts);

}



protected String runCommand(String cmd,CommandCallback cb)
{
   if (for_monitor != null) {
      if (!for_monitor.startDelay()) return null;
    }

   try {
      IvyExec ex = new IvyExec(cmd,getRootDirectory(),IvyExec.READ_OUTPUT);
      System.err.println("BVCR: Run " + ex.getCommand());
      InputStream ins = ex.getInputStream();
      try (BufferedReader br = new BufferedReader(new InputStreamReader(ins))) {
         for ( ; ; ) {
            String ln = br.readLine();
            if (ln == null) break;
            cb.handleLine(ln);
          }
       }
      cb.handleDone(ex.waitFor());
    }
   catch (IOException e) {
      System.err.println("BVCR: Problem running command: " + cmd + ": " + e);
    }
   finally {
      if (for_monitor != null) for_monitor.endDelay();
    }

   return null;
}


protected class StringCommand implements CommandCallback {

   private StringBuffer msg_content;
   private int command_status;

   StringCommand(String cmd) {
      msg_content = new StringBuffer();
      command_status = 0;
      runCommand(cmd,this);
    }

   @Override public void handleLine(String ln) {
      msg_content.append(ln);
      msg_content.append("\n");
    }

   @Override public void handleDone(int sts)	{ command_status = sts; }

   String getContent()				{ return msg_content.toString(); }
   int getStatus()				{ return command_status; }

}	// end of inner class XmlMessage



protected class XmlCommand extends StringCommand {

   XmlCommand(String cmd) {
      super(cmd);
    }

   Element getXml() {
      return IvyXml.convertStringToXml(getContent());
    }

}	// end of inner class XmlMessage




String getRelativePath(File f)
{
   File f1 = getRootDirectory();
   // System.err.println("GET REL PATH " + f + " " + f1);

   String s = f.getPath();
   if (!f.isAbsolute() || f1 == null) return s;

   String s1 = f1.getPath();
   String s2 = s;
   try {
      s1 = f1.getCanonicalPath();
      s2 = f.getCanonicalPath();
    }
   catch (IOException e) { }

   // System.err.println("CHECK PATH " + s2 + " " + s1);
   if (s2.startsWith(s1)) {
      int ln = s1.length() + 1;
      s = s2.substring(ln);
    }
   return s;
}




/********************************************************************************/
/*										*/
/*	Difference analyzer for generic diff command				*/
/*										*/
/********************************************************************************/

private static final Pattern LINE_PAT = Pattern.compile("@@ \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) @@.*");
private static final Pattern LINE_PAT1 = Pattern.compile("@@ \\-(\\d+) \\+(\\d+) @@.*");
private static final Pattern LINE_PAT2 = Pattern.compile("@@@ \\-(\\d+),(\\d+) \\-(\\d+),(\\d+) \\+(\\d+),(\\d+) @@@.*");
private static final Pattern SOURCE_PAT = Pattern.compile("\\-\\-\\- (\\S+)\\s+\\(revision (\\w+)\\)");

private static final Pattern GIT_INDEX = Pattern.compile("index ([0-9a-f.]+)(\\s|,).*");
private static final Pattern GIT_SOURCE = Pattern.compile("\\-\\-\\- a[/\\\\](\\S+)");



protected class DiffAnalyzer implements CommandCallback {

   BvcrDifferenceSet diff_set;
   private int source_line;
   private int target_line;
   private int del_count;
   private String base_version;

   DiffAnalyzer(BvcrDifferenceSet ds) {
      diff_set = ds;
      source_line = 0;
      target_line = 0;
      del_count = 0;
      base_version = null;
    }

   @Override public void handleLine(String ln) {
      if (ln.length() == 0) return;
      char ch = ln.charAt(0);
      switch (ch) {
	 case 'I' :                     // skip Index: lines and ========
	 case '=' :
	    source_line = 0;
	    break;
	 case 'i' :                     // git index line
	    source_line = 0;
	    Matcher m3 = GIT_INDEX.matcher(ln);
	    if (m3.matches()) {
	       base_version = m3.group(1);
	     }
	    break;
	 case '\\' :
	    break;
	 case '@' :
	    Matcher m4 = LINE_PAT.matcher(ln);
	    Matcher m5 = LINE_PAT1.matcher(ln);
	    Matcher m6 = LINE_PAT2.matcher(ln);
	    if (m4.matches()) {
	       source_line = Integer.parseInt(m4.group(1));
	       target_line = Integer.parseInt(m4.group(3));
	       del_count = 0;
	     }
	    else if (m5.matches()) {
	       source_line = Integer.parseInt(m5.group(1));
	       target_line = Integer.parseInt(m5.group(2));
	       del_count = 0;
	     }
	    else if (m6.matches()) {
	       source_line = Integer.parseInt(m6.group(1));
	       target_line = Integer.parseInt(m6.group(5));
	       del_count = 0;
	     }
	    else source_line = 0;
	    break;
	 case '-' :
	    if (source_line == 0) {
	       Matcher m1 = SOURCE_PAT.matcher(ln);
	       Matcher m2 = GIT_SOURCE.matcher(ln);
	       if (m1.matches()) {
		  String fil = m1.group(1);
		  String ver = m1.group(2);
		  // System.err.println("BVCR: start file " + fil + " " + ver);
		  diff_set.beginFile(fil,ver);
		}
	       else if (m2.matches()) {
		  String fil = m2.group(1);
		  File f = new File(getRootDirectory(),fil);
		  // System.err.println("BVCR: start git file " + fil + " " + getRootDirectory() + " " + f);
		  diff_set.beginFile(f.getPath(),base_version);
		}
	     }
	    else {
	       diff_set.noteDelete(source_line,target_line,ln.substring(1));
	       ++source_line;
	       ++del_count;
	     }
	    break;
	 case ' ' :
	    if (source_line != 0) {
	       ++source_line;
	       ++target_line;
	     }
	    break;
	 case '+' :
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

}	// end of inner class DiffAnalyzer





}	// end of abstract class BvcrVersionManager




/* end of BvcrVersionManager.java */
