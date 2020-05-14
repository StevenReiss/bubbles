/********************************************************************************/
/*										*/
/*		RebaseProject.java						*/
/*										*/
/*	Project information for REBUS back end					*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class RebaseProject implements RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private String		project_id;
private Map<String,RebaseFile> project_files;
private int		pending_files;
private List<RebaseFile> new_files;
private RebaseMain	rebase_main;
private RebaseProjectSemantics project_root;
private Set<RebaseFile> accepted_files;


private static String [] prefix_set = new String[] {
   "java.lang.", "java.util.", "java.io.", "java.awt.",
   "java.net.", "java.text.", "javax.swing.", "javax.xml.",
   "org.w3c."
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseProject(RebaseMain rm,RebaseRepo repo,String id,String name)
{
   rebase_main = rm;
   project_name = name;
   project_id = id;
   project_files = new HashMap<String,RebaseFile>();
   pending_files = 0;
   new_files = new ArrayList<RebaseFile>();
   project_root = null;
   accepted_files = new HashSet<RebaseFile>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

RebaseFile findFile(String path)
{
   if (path == null) return null;

   path = RebaseMain.fixFileName(path);

   return project_files.get(path);
}


String getName()
{
   return project_name;
}


String getId()
{
   return project_id;
}


/********************************************************************************/
/*										*/
/*	File add methods							*/
/*										*/
/********************************************************************************/

synchronized void notePending(int ct)
{
   pending_files += ct;
}

synchronized void notePending()
{
   pending_files += 1;
}


synchronized void donePending()
{
   if (pending_files == 0) return;
   if (--pending_files == 0) {
      buildProject(false,true,false,null);
    }
}



boolean addFile(RebaseFile rf)
{
   String fnm = rf.getFileName();
   if (project_files.containsKey(fnm)) return false;

   project_files.put(fnm,rf);

   new_files.add(rf);

   project_root = null;

   return true;
}





/********************************************************************************/
/*										*/
/*	Expansion methods							*/
/*										*/
/********************************************************************************/

void addPackageFiles(RebaseFile rf)
{
   notePending();

   String pkgname = rf.getPackageName();
   addFilesForPackage(pkgname,rf,null);

   donePending();
}



private void addFilesForPackage(String pkgname,RebaseFile rf,Set<String> ignore)
{
   String tok = pkgname;
   if (pkgname == null || pkgname.length() == 0) tok = "class";
   String pid = rf.getProjectId();
   try {
      List<RebaseSource> srcs = rf.getRepository().getSources(tok,rf.getRequest(),rf.getSource().getBaseSource(),this);
      for (RebaseSource rs : srcs) {
	 if (!rs.getProjectId().equals(pid)) continue;
	 if (project_files.get(rs.getPath()) != null) continue;
	 if (ignore != null) {
	    if (ignore.contains(rs.getPath())) continue;
	    ignore.add(rs.getPath());
	  }
	 RebaseFile nrf = new RebaseFile(rs);
	 String pkg = nrf.getPackageName();
	 if (pkg == null || !pkg.equals(pkgname)) continue;
	 addFile(nrf);
       }
    }
   catch (RebaseException e) {
      RebaseMain.logE("Problem getting package data",e);
    }
}


void addSystemFiles(RebaseFile rf0)
{
   if (rf0 == null) return;

   String pkg0 = rf0.getPackageName();

   Set<RebaseFile> filesdone = new HashSet<RebaseFile>();
   Set<String> pkgsdone = new HashSet<String>();
   Set<String> pkgstodo = new HashSet<String>();
   Set<String> ignore = new HashSet<String>();

   for ( ; ; ) {
      List<RebaseFile> filestodo = new ArrayList<RebaseFile>(project_files.values());
      for (RebaseFile rf : filestodo) {
	 if (filesdone.contains(rf)) continue;
	 RebaseSemanticData rsd = rebase_main.getSemanticData(rf);
	 if (rsd == null) continue;
	 Set<String> pkgs = rsd.getRelatedPackages();
	 for (String pkg : pkgs) {
	    if (pkgsdone.contains(pkg) || pkgstodo.contains(pkg)) continue;
	    if (isPackageRelevant(pkg0,pkg)) pkgstodo.add(pkg);
	    else pkgsdone.add(pkg);
	  }
       }
      if (pkgstodo.isEmpty()) break;
      for (String pkg : pkgstodo) {
	 addFilesForPackage(pkg,rf0,ignore);
	 pkgsdone.add(pkg);
       }
    }
}



private boolean isPackageRelevant(String orig,String pkg)
{
   for (String pfx : prefix_set) {
      if (pkg.startsWith(pfx)) return false;
    }

   if (orig != null) {
      int idx = orig.indexOf(".");
      if (idx >= 0) orig = orig.substring(0,idx+1);
      if (!pkg.startsWith(orig)) return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Compilation methods							*/
/*										*/
/********************************************************************************/

synchronized void buildProject(boolean clean,boolean full,boolean refresh,IvyXmlWriter xw)
{
   RebaseProjectSemantics rs = getResolvedSemantics();
   if (rs == null) return;

   if (new_files != null && new_files.size() > 0) {
      IvyXmlWriter nxw = rebase_main.beginMessage("RESOURCE");
      synchronized (this) {
	 for (RebaseFile rf : new_files) {
	    RebaseUtil.outputResourceDelta("ADDED",rf,nxw);
	  }
	 new_files.clear();
       }
      rebase_main.finishMessage(nxw);
    }

   if (xw != null) {
      List<RebaseMessage> msgs = rs.getMessages();
      for (RebaseMessage pm : msgs) {
	 pm.outputProblem(xw);
       }
    }
}



private synchronized RebaseProjectSemantics getResolvedSemantics()
{
   if (project_root == null) {
      Set<RebaseFile> files = new HashSet<RebaseFile>(project_files.values());
      project_root = rebase_main.getSemanticData(files);
    }

   if (project_root != null) project_root.resolve();

   return project_root;
}



void openProject()
{
   getResolvedSemantics();
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputProject(boolean files,boolean paths,boolean clss,boolean opts,
      IvyXmlWriter xw)
{
}


void outputPackage(String nm,IvyXmlWriter xw)
{
}





/********************************************************************************/
/*										*/
/*	Handle Name output							*/
/*										*/
/********************************************************************************/

synchronized void outputAllNames(Set<String> files,IvyXmlWriter xw)
{
   RebaseProjectSemantics rs = getResolvedSemantics();

   if (rs == null) return;

   rs.outputAllNames(files,xw);
}




/********************************************************************************/
/*										*/
/*	Search for a name of a particular type					*/
/*										*/
/********************************************************************************/

synchronized void patternSearch(String pat,String typ,boolean defs,boolean refs,boolean sys,IvyXmlWriter xw)
{
   RebaseProjectSemantics rs = getResolvedSemantics();
   if (rs == null) return;

   // TODO: handle Sys flag

   RebaseSearcher search = rs.findSymbols(pat,typ);
   rs.outputLocations(search,defs,refs,false,false,false,xw);
}


/********************************************************************************/
/*										*/
/*	Search for all uses of name at given source point			*/
/*										*/
/********************************************************************************/

synchronized void findAll(String file,int soff,int eoff,boolean defs,boolean refs,
      boolean imps,boolean typ,
      boolean ronly,boolean wonly,IvyXmlWriter xw)
{
   RebaseProjectSemantics rs = getResolvedSemantics();

   RebaseSearcher search = rs.findSymbolAt(file,soff,eoff);
   search.outputSearchFor(xw);

   rs.outputLocations(search,defs,refs,imps,ronly,wonly,xw);
}



/********************************************************************************/
/*										*/
/*	Search for name by key							*/
/*										*/
/********************************************************************************/

synchronized void findByKey(String file,String key,IvyXmlWriter xw)
{
   RebaseProjectSemantics rs = getResolvedSemantics();

   RebaseSearcher search = rs.findSymbolByKey(project_name,file,key);
   rs.outputLocations(search,true,false,false,false,false,xw);
}




/********************************************************************************/
/*										*/
/*	Text search for a string/pattern					*/
/*										*/
/********************************************************************************/

synchronized void textSearch(int fgs,String pat,int max,IvyXmlWriter xw)
{
   Pattern pp = null;
   try {
      pp = Pattern.compile(pat,fgs);
    }
   catch (PatternSyntaxException e) {
      pp = Pattern.compile(pat,fgs|Pattern.LITERAL);
    }

   int rct = 0;
   Set<RebaseFile> files = new HashSet<RebaseFile>(project_files.values());
   RebaseProjectSemantics rs = getResolvedSemantics();

   for (RebaseFile rf : files) {
      String filetext = RebaseMain.getFileContents(rf);
      Matcher m = pp.matcher(filetext);
      while (m.find()) {
	 if (++rct > max) break;
	 xw.begin("MATCH");
	 xw.field("STARTOFFSET",m.start());
	 xw.field("LENGTH",m.end() - m.start());
	 xw.textElement("FILE",rf.getFileName());
	 rs.outputContainer(rf,m.start(),m.end(),xw);
	 // TODO: find corresponding symbol here and output it
	 xw.end("MATCH");
       }

    }
}



/********************************************************************************/
/*										*/
/*	Get details of the name at a given source point 			*/
/*										*/
/********************************************************************************/

synchronized void getFullyQualifiedName(String file,int spos,int epos,IvyXmlWriter xw)
{
   RebaseProjectSemantics rs = getResolvedSemantics();

   RebaseSearcher search = rs.findSymbolAt(file,spos,epos);
   rs.outputFullName(search,xw);
}




/********************************************************************************/
/*										*/
/*	Handle Region extractions						*/
/*										*/
/********************************************************************************/

synchronized void getTextRegions(String bid,String file,String cls,boolean pfx,boolean statics,
      boolean compunit,boolean imports,boolean pkg,
      boolean topdecls,boolean fields,boolean all,IvyXmlWriter xw)
	throws RebaseException
{
   RebaseFile rf = findFile(file);

   if (rf == null && cls != null) {
      rf = getFileFromClass(cls);
    }
   if (rf == null) return;

   String cnts = RebaseMain.getFileContents(rf);
   RebaseSemanticData rsd = rebase_main.getSemanticData(rf);

   rsd.getTextRegions(cnts,cls,pfx,statics,compunit,imports,pkg,topdecls,fields,all,xw);
}


private RebaseFile getFileFromClass(String cls)
{
   for (RebaseFile rf : project_files.values()) {
      RebaseSemanticData rsd = rebase_main.getSemanticData(rf);
      if (rsd.definesClass(cls)) return rf;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Handle formatting requests						*/
/*										*/
/********************************************************************************/

synchronized void formatCode(String file,int spos,int epos,IvyXmlWriter xw)
{
   RebaseFile rf = findFile(file);
   if (rf == null) return;
   String cnts = RebaseMain.getFileContents(rf);
   RebaseSemanticData rs = rebase_main.getSemanticData(rf);
   rs.formatCode(cnts,spos,epos,xw);
}



/********************************************************************************/
/*										*/
/*	Check for package							*/
/*										*/
/********************************************************************************/

synchronized void findPackage(String pkg,IvyXmlWriter xw)
{
   String fn = "/" + pkg.replace(".","/") + "/";
   String path = null;
   for (String nm : project_files.keySet()) {
      int idx = nm.indexOf(fn);
      if (idx > 0) {
	 int ln = fn.length();
	 path = nm.substring(0,idx+ln);
	 break;
       }
    }

   if (path != null) {
      xw.begin("PACKAGE");
      xw.field("NAME",pkg);
      xw.field("PATH",path);
      xw.begin("ITEM");
      xw.field("TYPE","Package");
      xw.field("NAME",path);
      xw.field("HANDLE",path);
      xw.field("PROJECT",getName());
      xw.end("ITEM");
      xw.end("PACKAGE");
    }
}





/********************************************************************************/
/*										*/
/*	Handle delete requests							*/
/*										*/
/********************************************************************************/

synchronized void delete(String what,String path)
{
   List<RebaseFile> dels = new ArrayList<RebaseFile>();

   switch (what) {
      case "PROJECT" :
      case "FILE" :
	 notePending();
	 for (Iterator<RebaseFile> it = project_files.values().iterator(); it.hasNext(); ) {
	    RebaseFile rf = it.next();
	    if (path == null || rf.getFileName().equals(path)) {
	       it.remove();
	       dels.add(rf);
	     }
	  }
	 donePending();
	 break;
      case "CLASS" :
	 if (path == null) return;
	 RebaseFile rf1 = getFileFromClass(path);
	 if (rf1 != null) {
	    notePending();
	    project_files.remove(rf1.getFileName());
	    dels.add(rf1);
	    donePending();
	  }
	 break;
      case "PACKAGE" :
	 notePending();
	 for (Iterator<RebaseFile> it = project_files.values().iterator(); it.hasNext(); ) {
	    RebaseFile rf = it.next();
	    if (path == null || rf.getPackageName().startsWith(path)) {
	       it.remove();
	       dels.add(rf);
	     }
	  }
	 donePending();
	 break;
    }

   reportDeletes(dels);
}



private void reportDeletes(List<RebaseFile> dels)
{
   if (dels == null || dels.size() == 0) return;
   RebaseEditManager em = rebase_main.getEditorManager();
   RebaseWordFactory wf = RebaseWordFactory.getFactory();

   IvyXmlWriter xw = rebase_main.beginMessage("RESOURCE");
   for (RebaseFile rf : dels) {
      wf.addReject(rf.getText());
      em.removeFile(rf);
      xw.begin("DELTA");
      xw.field("KIND","REMOVED");
      xw.field("PATH",rf.getFileName());
      xw.begin("RESOURCE");
      xw.field("LOCATION",rf.getFileName());
      xw.field("TYPE","FILE");
      xw.field("PROJECT",rf.getProjectName());
      xw.end("RESOURCE");
      xw.end("DELTA");
    }
   rebase_main.finishMessage(xw);
}




/********************************************************************************/
/*										*/
/*	Handle acceptances and export requests					*/
/*										*/
/********************************************************************************/

synchronized void noteAccept(String file,boolean fg)
{
   RebaseWordFactory wf = RebaseWordFactory.getFactory();

   RebaseFile rf = findFile(file);
   if (rf != null) {
      if (fg) {
	 accepted_files.add(rf);
	 wf.addAccept(rf.getText());
       }
      else {
	 accepted_files.remove(rf);
	 wf.removeAccept(rf.getText());
       }
    }
}



synchronized void export(File dir,String fnm,boolean accepted)
{
   if (fnm != null) {
      RebaseFile rf = findFile(fnm);
      doExport(dir,rf,accepted);
    }
   else {
      for (RebaseFile rf : project_files.values()) {
	 doExport(dir,rf,accepted);
       }
    }
}



private void doExport(File dir,RebaseFile rf,boolean accepted)
{
   if (accepted && !accepted_files.contains(rf)) return;

   String pnm = rf.getPackageName();
   String fnm = rf.getFileName();
   int idx = fnm.lastIndexOf("/");
   if (idx >= 0) fnm = fnm.substring(idx+1);
   String name = pnm + "." + fnm;

   File f1 = new File(dir,name);
   // might want to check if f1 exists here

   String cnts = RebaseMain.getFileContents(rf);
   if (cnts == null) return;

   try {
      FileWriter fw = new FileWriter(f1);
      fw.write(cnts);
      fw.close();
    }
   catch (IOException e) {
      RebaseMain.logE("Problem exporting file " + f1,e);
    }
}



}	// end of class RebaseProject




/* end of RebaseProject.java */

