//********************************************************************************/
/*										*/
/*		BvcrVersionGIT.java						*/
/*										*/
/*	Bubble Version Collaboration Repository interface to GIT		*/
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
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;



class BvcrVersionGIT extends BvcrVersionManager implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		git_root;
private String		git_command;
private String		current_version;
private String          long_version;

private static BoardProperties bvcr_properties = BoardProperties.getProperties("Bvcr");

private static SimpleDateFormat GIT_DATE = new SimpleDateFormat("EEE MMM dd kk:mm:ss yyyy ZZZZZ");

private static String GIT_LOG_FORMAT = "%H%x09%h%x09%an%x09%ae%x09%ad%x09%P%x09%d%x09%s%n%b%n***EOF";
private static String GIT_PRIOR_FORMAT = "%H%x09%P%x09%d%n";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrVersionGIT(BvcrProject bp)
{
   super(bp);

   git_command = bvcr_properties.getProperty("bvcr.git.command","git");
   current_version = bvcr_properties.getProperty("bvcr.git." + bp.getName() + ".origin");

   findGitRoot();

   if (current_version == null) {
      current_version = "HEAD";
      // String cmd = git_command + " branch --all";
      String cmd = git_command + " branch -a";
      StringCommand sc = new StringCommand(cmd);
      String vers = sc.getContent();
      StringTokenizer tok = new StringTokenizer(vers," \r\n\t");
      boolean star = false;
      while (tok.hasMoreTokens()) {
	 String v = tok.nextToken();
	 if (v.equals("*")) star = true;
	 else {
	    if (star) {
	       IvyLog.logD("BVCR","FOUND BRANCH " + v);
	       current_version = v;
	     }
	    star = false;
	  }
       }
    }
   findCurrentVersion();
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static BvcrVersionManager getRepository(BvcrProject bp,File srcdir)
{
   if (srcdir == null) return null;

   File fp = srcdir;
   while (fp != null && fp.exists() && fp.isDirectory()) {
      File f2 = new File(fp,".git");
      if (f2.exists() && f2.isDirectory()) {
	 IvyLog.logD("BVCR","HANDLE GIT REPOSITORY " + srcdir);
	 return new BvcrVersionGIT(bp);
       }
      fp = fp.getParentFile();
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override File getRootDirectory()		{ return git_root; }

@Override String getRepositoryName()
{
   return super.getRepositoryName();
}


@Override String getRepoType()
{
   return "GIT";
}


@Override void getDifferences(BvcrDifferenceSet ds)
{
   String cmd = git_command  + " diff -w";

   String v0 = ds.getStartVersion();
   String v1 = ds.getEndVersion();
   if (v0 != null) {
      cmd += " " + v0;
      if (v1 != null) cmd += " " + v1;
    }
   else {
      String v2 = getCurrentVersion();
      if (v2 != null) cmd += " " + v2;
    }

   List<File> diffs = ds.getFilesToCompute();
   if (diffs == null) {
      // cmd += " " + git_root.getPath();
    }
   else if (diffs.size() == 0) return;
   else {
      cmd += " --";
      for (File f : diffs) {
         f = IvyFile.getCanonical(f);
	 cmd += " " + f.getAbsolutePath();
       }
    }

   DiffAnalyzer da = new DiffAnalyzer(ds);
   runCommand(cmd,da);
}




/********************************************************************************/
/*										*/
/*	Find the top of the GIT repository					*/
/*										*/
/********************************************************************************/

private void findGitRoot()
{
   File f = new File(for_project.getSourceDirectory());
   for ( ; ; ) {
      File fp2 = new File(f,".git");
      if (fp2.exists()) break;
      f = f.getParentFile();
    }
   for ( ; ; ) {
      File fp = f.getParentFile();
      File fp1 = new File(fp,".git");
      if (!fp1.exists()) break;
      f = fp;
    }

   IvyLog.logD("BVCR","GIT root = " + f);

   git_root = f;
}



/********************************************************************************/
/*										*/
/*	History gathering for a file						*/
/*										*/
/********************************************************************************/

@Override void findHistory(File f,IvyXmlWriter xw)
{
   String head = null;
   Map<String,List<String>> priors = new HashMap<String,List<String>>();
   String cmd1 = git_command + " log --reverse '--pretty=format:" + GIT_PRIOR_FORMAT + "'";
   StringCommand cmd = new StringCommand(cmd1);
   StringTokenizer tok = new StringTokenizer(cmd.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String ent = tok.nextToken();
      try {
	 String [] ldata = ent.split("\t",3);
	 String rev = ldata[0];
	 String prev = ldata[1];
	 String alts = ldata[2];
	 List<String> prevs = priors.get(rev);
	 if (prevs == null) {
	    prevs = new ArrayList<String>();
	    priors.put(rev,prevs);
	  }
	 for (StringTokenizer ltok = new StringTokenizer(prev,"(, )"); ltok.hasMoreTokens(); ) {
	    String pid = ltok.nextToken();
	    prevs.add(pid);
	  }
	 if (head == null && alts != null && alts.length() > 0) {
	    for (StringTokenizer ltok = new StringTokenizer(alts,"(, )"); ltok.hasMoreTokens(); ) {
	       String nm = ltok.nextToken();
	       if (nm.equals("HEAD")) head = rev;
	     }
	  }
       }
      catch (Throwable t) {
	 IvyLog.logE("BVCR","Problem parsing priors log entry",t);
       }
    }

   String cmds = git_command + " log --reverse '--pretty=format:" + GIT_LOG_FORMAT + "'";
   f = IvyFile.getCanonical(f);
   cmds += " -- " + f.getAbsolutePath();
   // parent version information is inaccurate in this case

   cmd = new StringCommand(cmds);

   Map<String,BvcrFileVersion> fvs = new LinkedHashMap<String,BvcrFileVersion>();

   tok = new StringTokenizer(cmd.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String ent = tok.nextToken();
      try {
	 String [] ldata = ent.split("\t",8);
	 String rev = ldata[0];
	 String srev = ldata[1];
	 String auth = ldata[2];
	 String email = ldata[3];
	 Date d = GIT_DATE.parse(ldata[4]);
	 String prev = ldata[5];
	 String alts = ldata[6];
	 String msg = ldata[7];
	 String bdy = "";

	 if (email != null) {
	    if (auth != null) auth += " (" + email + ")";
	    else auth = email;
	  }

	 BvcrFileVersion fv = new BvcrFileVersion(f,rev,d,auth,msg);
	 Set<String> done = new HashSet<String>();
	 for (StringTokenizer ltok = new StringTokenizer(prev,"(, )"); ltok.hasMoreTokens(); ) {
	    String pid = ltok.nextToken();
	    addPriors(fv,pid,fvs,priors,done);
	  }
	 if (srev != null && srev.length() > 0) fv.addAlternativeId(srev,null);
	 if (alts != null && alts.length() > 0) {
	    for (StringTokenizer ltok = new StringTokenizer(alts,"(, )"); ltok.hasMoreTokens(); ) {
	       String nm = ltok.nextToken();
	       fv.addAlternativeName(nm);
	     }
	  }
	 fvs.put(rev,fv);
	 while (tok.hasMoreTokens()) {
	    String bdl = tok.nextToken();
	    if (bdl.equals("***EOF")) break;
	    bdy += bdl + "\n";
	   }
	 bdy = bdy.trim();
	 if (bdy.length() > 2) fv.addVersionBody(bdy);
       }
      catch (Throwable e) {
	 IvyLog.logE("BVCR","Problem parsing log entry",e);
       }
    }

   if (head != null && !fvs.containsKey(head)) {
      BvcrFileVersion fv = new BvcrFileVersion(f,head,null,null,null);
      Set<String> done = new HashSet<String>();
      addPriors(fv,head,fvs,priors,done);
      for (BvcrFileVersion pv : fv.getPriorVersions(null)) {
	 pv.addAlternativeId("HEAD",null);
	 break;
       }
    }

   xw.begin("HISTORY");
   xw.field("FILE",f.getPath());
   for (BvcrFileVersion fv : fvs.values()) {
      fv.outputXml(xw);
    }
   xw.end("HISTORY");
}




/********************************************************************************/
/*										*/
/*	Get project version history						*/
/*										*/
/********************************************************************************/

@Override void findProjectHistory(IvyXmlWriter xw)
{
   String cmd1 = git_command + " log --reverse '--pretty=format:" + GIT_LOG_FORMAT + "'";
   StringCommand cmd = new StringCommand(cmd1);
   StringTokenizer tok = new StringTokenizer(cmd.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String ent = tok.nextToken();
      try {
	 String [] ldata = ent.split("\t",8);
	 String rev = ldata[0];
	 String srev = ldata[1];
	 String auth = ldata[2];
	 String email = ldata[3];
	 Date d = GIT_DATE.parse(ldata[4]);
	 String prev = ldata[5];
	 String alts = ldata[6];
	 String msg = ldata[7];
	 String bdy = "";

	 if (email != null) {
	    if (auth != null) auth += " (" + email + ")";
	    else auth = email;
	  }

	 xw.begin("VERSION");
	 xw.field("NAME",rev);
	 xw.field("DATE",d);
	 xw.field("AUTHOR",auth);

	 xw.textElement("MSG",msg);

	 for (StringTokenizer ltok = new StringTokenizer(prev,"(, )"); ltok.hasMoreTokens(); ) {
	    String pid = ltok.nextToken();
	    if (pid == null) continue;
	    xw.begin("PRIOR");
	    xw.field("ID",pid);
	    xw.end("PRIOR");
	  }
	 if (srev != null && srev.length() > 0) {
	    xw.begin("ALTERNATIVE");
	    xw.field("ID",srev);
	    xw.end("ALTERNATIVE");
	  }
	 if (alts != null && alts.length() > 0) {
	    for (StringTokenizer ltok = new StringTokenizer(alts,"(,)"); ltok.hasMoreTokens(); ) {
	       String nm = ltok.nextToken().trim();
	       xw.begin("ALTERNATIVE");
	       xw.field("NAME",nm);
	       xw.end("ALTERNATIVE");
	     }
	  }
	 while (tok.hasMoreTokens()) {
	    String bdl = tok.nextToken();
	    if (bdl.equals("***EOF")) break;
	    bdy += bdl + "\n";
	  }
	 bdy = bdy.trim();
	 if (bdy.length() > 2) xw.textElement("BODY",bdy);

	 xw.end("VERSION");
       }
      catch (Throwable e) {
	 IvyLog.logE("BVCR","Problem parsing log entry",e);
       }
    }
}


@Override String getCurrentVersion() 
{
   if (long_version == null) {
      findCurrentVersion();
    }
   return long_version;
}


private void findCurrentVersion()
{
   String cmd1 = git_command + " rev-parse origin";
   StringCommand cmd = new StringCommand(cmd1);
   String rslt = cmd.getContent();
   IvyLog.logD("BVCR","Current version found as " + rslt);
   
   StringTokenizer tok = new StringTokenizer(rslt,"\n\r");
   long_version = null;
   if (tok.hasMoreTokens()) long_version = tok.nextToken().trim();
}
  
   
   


/********************************************************************************/
/*										*/
/*	Get Status of all files in the project					*/
/*										*/
/********************************************************************************/

@Override void showChangedFiles(IvyXmlWriter xw,boolean ign)
{
   String cmd = git_command + " status --porcelain";
   if (ign) cmd += " --ignored";
   StringCommand cm = new StringCommand(cmd);

   Map<File,FileData> filemap = new HashMap<File,FileData>();

   StringTokenizer tok = new StringTokenizer(cm.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String ent = tok.nextToken();
      String what0 = ent.substring(1,2);
      String what1 = ent.substring(0,1);
      if (what0.equals(" ")) what0 = what1;

      String file = ent.substring(3);
      String tofile = null;
      file = file.replace("\\\"","\"");
      int idx1 = file.indexOf(" -> ");
      if (idx1 > 0) {
	 tofile = file.substring(idx1+4);
	 file = file.substring(0,idx1);
       }
      if (file.startsWith("\"")) {
	 file = file.substring(1,file.length()-1);
       }
      if (tofile != null && tofile.startsWith("\"")) {
	 tofile = tofile.substring(1,tofile.length()-1);
       }
      File nf = new File(git_root,file);

      String sts = "UNMODIFIED";
      boolean push = false;
      switch (what0) {
	 case "M" :
	    sts = "MODIFIED";
	    break;
	 case "A" :
	    sts = "ADDED";
	    push = true;
	    break;
	 case "D" :
	    sts = "DELETED";
	    break;
	 case "R" :
	    sts = "RENAMED";
	    break;
	 case "C" :
	    sts = "COPIED";
	    break;
	 case "U" :
	    sts = "UNMERGED";
	    break;
	 case "?" :
	    sts = "UNTRACKED";
	    break;
	 case "!" :
	    sts = "IGNORED";
	    break;
       }
      FileData fd = new FileData(sts,tofile);
      filemap.put(nf,fd);
      if (push) fd.pushNeeded();
    }

   cm = new StringCommand(git_command + " diff --name-only --cached origin/master");
   tok = new StringTokenizer(cm.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String fnm = tok.nextToken();
      if (fnm.startsWith("commit ") || fnm.contains(": ")) continue;
      if (Character.isWhitespace(fnm.charAt(0))) continue;
      fnm = fnm.trim();
      File nf = new File(git_root,fnm);
      FileData fd = filemap.get(nf);
      if (fd == null) {
	 fd = new FileData(null,"UNMODIFIED");
	 filemap.put(nf,fd);
       }
      fd.pushNeeded();
    }

   for (Map.Entry<File,FileData> ent : filemap.entrySet()) {
      xw.begin("FILE");
      xw.field("NAME",ent.getKey().getPath());
      ent.getValue().outputXml(xw);
      xw.end("FILE");
    }
}


private class FileData {

   private String new_name;
   private String file_state;
   private boolean needs_push;

   FileData(String state,String tofile) {
      new_name = tofile;
      file_state = state;
    }

   void pushNeeded()				{ needs_push = true; }

   void outputXml(IvyXmlWriter xw) {
      if (new_name != null) {
	 File nf1 = new File(git_root,new_name);
	 xw.field("NEWNAME",nf1.getPath());
       }
      xw.field("STATE",file_state);
      if (needs_push) xw.field("UNPUSHED",true);
    }

}	// end of inner class FileData



/********************************************************************************/
/*										*/
/*	Handle request to update files						*/
/*										*/
/********************************************************************************/

@Override void ignoreFiles(IvyXmlWriter xw,List<String> files)
{
   if (files.isEmpty()) return;
   File ign = new File(git_root,".gitignore");
   try {
      FileWriter fw = new FileWriter(ign,true);
      PrintWriter pw = new PrintWriter(fw);
      for (String fnm : files) {
	 ignoreFile(xw,pw,fnm);
       }
      pw.close();
    }
   catch (IOException e) {
      xw.textElement("ERROR",e.getMessage());
    }
}



private void ignoreFile(IvyXmlWriter xw,PrintWriter pw,String fnm)
{
   String rnm = git_root.getPath();
   File ignf = new File(fnm);
   if (fnm.startsWith(rnm)) {
      fnm = fnm.substring(rnm.length());
      if (fnm.startsWith("/")) fnm = fnm.substring(1);
    }
   if (ignf.isDirectory()) fnm += "/";
   pw.println(fnm);
   xw.textElement("IGNORE",fnm);
}




@Override void addFiles(IvyXmlWriter xw,List<String> files)
{
   if (files.isEmpty()) return;
   String cmd = git_command + " add";
   for (String fnm : files) {
      cmd += " " + fnm;
    }
   StringCommand cmd1 = new StringCommand(cmd);
   if (cmd1 != null) {
      // handle error here
    }
}



@Override void removeFiles(IvyXmlWriter xw,List<String> files)
{
   if (files.isEmpty()) return;
   String cmd = git_command + " rm --cached";
   for (String fnm : files) {
      cmd += " " + fnm;
    }
   StringCommand cmd1 = new StringCommand(cmd);
   if (cmd1 != null) {
      // handle error here
    }
}




//********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override void doCommit(IvyXmlWriter xw,String msg)
{
   File f = null;
   try {
      f = File.createTempFile("BVCRcommitmessage","txt");
      FileWriter fw = new FileWriter(f);
      fw.write(msg);
      if (!msg.endsWith("\n")) fw.write("\n");
      fw.close();
      msg = null;
    }
   catch (IOException e) {
      msg = msg.replace("\\","\\\\");
      msg = msg.replace("'","\\'");
      msg = msg.replace("\t","\\t");
      msg = msg.replace("\n","\\n");
    }

   String cmd = git_command + " commit -a";
   if (msg == null && f != null) {
      cmd += " -F " + f.getPath();
    }
   else {
      cmd += " -m '" + msg + "'";
    }
   StringCommand cmd1 = new StringCommand(cmd);
   IvyLog.logD("BVCR","RESULT OF COMMIT: " + cmd1.getContent()+ " " + cmd1.getStatus());
   // comvert output to xml using xw

   if (f != null) f.delete();

   if (cmd1.getStatus() == 0) xw.textElement("OK",cmd1.getContent());
   else xw.textElement("ERROR",cmd1.getContent());
}


@Override void doPush(IvyXmlWriter xw)
{
   String cmd = git_command + " push origin master";
   StringCommand rslt = new StringCommand(cmd);
   IvyLog.logD("BVCR","RESULT OF PUSH: " + rslt.getContent() + " " + rslt.getStatus());

   if (rslt.getStatus() == 0) xw.textElement("OK",rslt.getContent());
   else xw.textElement("ERROR",rslt.getContent());
   
   long_version = null;
}


@Override void doUpdate(IvyXmlWriter xw,boolean keep,boolean remove)
{
   String args = " -s recursive -Xpatience -Xignore-space-change";
   if (keep) args += " -Xours";
   else if (remove) args += " -Xtheirs";

   String cmd = git_command + " pull " + args;
   StringCommand rslt = new StringCommand(cmd);
   IvyLog.logD("BVCR","RESULT OF PULL: " + rslt.getContent() + " " + rslt.getStatus());
   if (rslt.getStatus() != 0) {
      cmd = git_command + " pull " + args + " origin master";
      rslt = new StringCommand(cmd);
      IvyLog.logD("BVCR","RESULT OF ORIGIN PULL: " + rslt.getContent() + " " + rslt.getStatus());
    }

   if (rslt.getStatus() == 0) xw.textElement("OK",rslt.getContent());
   else xw.textElement("ERROR",rslt.getContent());
}


@Override void doSetVersion(IvyXmlWriter xw,String ver)
{
   String cmd = git_command + " checkout " + ver;
   StringCommand rslt = new StringCommand(cmd);
   IvyLog.logD("BVCR","RESULT OF CHECKOUT: " + rslt.getContent() + " " + rslt.getStatus());

   if (rslt.getStatus() == 0) xw.textElement("OK",rslt.getContent());
   else xw.textElement("ERROR",rslt.getContent());
}


@Override void doStash(IvyXmlWriter xw,String msg)
{
   msg = msg.replace("\\","\\\\");
   msg = msg.replace("'","\\'");
   msg = msg.replace("\t","\\t");
   msg = msg.replace("\n","\\n");

   String cmd = git_command + " stash " + msg;
   StringCommand rslt = new StringCommand(cmd);
   IvyLog.logD("BVCR","RESULT OF STASH: " + rslt.getContent() + " " + rslt.getStatus());

   if (rslt.getStatus() == 0) xw.textElement("OK",rslt.getContent());
   else xw.textElement("ERROR",rslt.getContent());
}



@Override void doFetch()
{
   String cmd = git_command + " fetch";
   StringCommand rslt = new StringCommand(cmd);
   IvyLog.logD("BVCR","RESULT OF FETCH: " + rslt.getContent() + " " + rslt.getStatus());
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private void addPriors(BvcrFileVersion fv,String id,Map<String,BvcrFileVersion> known,
			  Map<String,List<String>> priors,
			  Set<String> done)
{
   if (id == null) return;
   if (done.contains(id)) return;
   done.add(id);

   BvcrFileVersion pv = known.get(id);
   if (pv != null) {
      fv.addPriorVersion(pv);
      return;
    }

   List<String> prs = priors.get(id);
   if (prs == null) {
      IvyLog.logE("BVCR","Can't find prior version " + id);
      return;
    }

   for (String s : prs) {
      if (s == null || s.equals(id)) continue;
      addPriors(fv,s,known,priors,done);
    }
}




}	// end of class BvcrVersionGIT



/* end of BvcrVersionGIT.java */
