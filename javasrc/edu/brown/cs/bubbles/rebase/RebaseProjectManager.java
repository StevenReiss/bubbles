/********************************************************************************/
/*										*/
/*		RebaseProjectManager.java					*/
/*										*/
/*	Project manager for rebus backend					*/
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


class RebaseProjectManager implements RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseMain	rebase_main;
private Map<String,RebaseProject> project_map;
private List<RebaseProject> all_projects;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseProjectManager(RebaseMain rm)
{
   rebase_main = rm;
   project_map = new HashMap<>();
   all_projects = new ArrayList<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

RebaseProject findProject(String nm)
{
   if (nm == null) return null;

   return project_map.get(nm);
}


RebaseFile findFile(String path)
{
   path = RebaseMain.fixFileName(path);

   StringTokenizer tok = new StringTokenizer(path,"/");
   if (!tok.hasMoreTokens()) return null;
   String hd = tok.nextToken();
   if (!hd.equals("REBUS")) return null;
   String pid = tok.nextToken();
   RebaseProject rp = project_map.get(pid);
   if (rp == null) {
      rp = project_map.get("P_" + pid);
    }
   if (rp == null) return null;

   return rp.findFile(path);
}


RebaseProject findNewProject(String id,String name,RebaseRepo repo)
{
   RebaseProject rp = project_map.get(id);
   if (rp == null) {
      int ct = 0;
      name = name.replace(" ","_");
      if (name.length() == 0 || !Character.isAlphabetic(name.charAt(0))) name = "P_" + name;
      String onm = name;
      while (project_map.get(name) != null) {
	 name = onm + (++ct);
       }
      rp = new RebaseProject(rebase_main,repo,id,name);
      project_map.put(id,rp);
      project_map.put(name,rp);
      all_projects.add(rp);
    }
   return rp;
}



/********************************************************************************/
/*										*/
/*	Output and command methods						*/
/*										*/
/********************************************************************************/

void dumpPackage(String proj,String nm,IvyXmlWriter xw)  throws RebaseException
{
   RebaseProject rp = findProject(proj);
   if (rp == null) throw new RebaseException("Unknown project " + proj);
   rp.outputPackage(nm,xw);
}


void delete(String proj,String what,String path)
{
   if (proj == null && path == null && what.equals("PROJECT")) {
      RebaseWordFactory.getFactory().clear();
      Set<RebaseProject> pjts = new HashSet<RebaseProject>(project_map.values());
      for (RebaseProject rp : pjts) {
	 delete(rp.getId(),what,null);
       }
      return;
    }

   RebaseProject rp = getProject(proj,path);
   if (rp == null) return;

   if (what.equals("PROJECT")) {
      rp.delete(what,null);
      project_map.remove(rp.getName());
      project_map.remove(rp.getId());
      // resource message?
    }
   else {
      rp.delete(what,path);
    }
}



/********************************************************************************/
/*										*/
/*	Command methods 							*/
/*										*/
/********************************************************************************/

void listProjects(IvyXmlWriter xw)
{
   for (RebaseProject rp : all_projects) {
      xw.begin("PROJECT");
      xw.field("NAME",rp.getName());
      xw.field("ISREBUS",true);
      xw.field("BASE","/rebus/" + rp.getId());
      xw.end("PROJECT");
    }
}



void openProject(String proj,boolean files,boolean paths,boolean clss,
      boolean opts,IvyXmlWriter xw) throws RebaseException
{
   RebaseProject rp = findProject(proj);
   if (rp == null) throw new RebaseException("Unknown project " + proj);
   if (xw != null) rp.outputProject(files,paths,clss,opts,xw);
}





void buildProject(String pid,boolean clean,boolean full,boolean refresh,IvyXmlWriter xw)
{
   if (pid == null) {
      for (RebaseProject rp : all_projects) {
	 rp.buildProject(clean,full,refresh,xw);
       }
    }
   else {
      RebaseProject rp = findProject(pid);
      if (rp != null) rp.buildProject(clean,full,refresh,xw);
    }
}



/********************************************************************************/
/*										*/
/*	Name getting methods							*/
/*										*/
/********************************************************************************/

void getAllNames(String proj,String bid,Set<String> files,String bkg,
      IvyXmlWriter xw) throws RebaseException
{
   NameThread nt = null;
   if (bkg != null) nt = new NameThread(bid,bkg,files);

   if (files != null && File.separatorChar != '/') {
      Set<String> nf = new HashSet<String>();
      for (String f : files) {
	 String f1 = RebaseMain.fixFileName(f);
	 nf.add(f1);
       }
      files = nf;
    }

   if (proj != null) {
      RebaseProject rp = findProject(proj);
      if (rp == null) throw new RebaseException("Unknown project " + proj);
      handleAllNames(rp,files,nt,xw);
    }
   else {
      for (RebaseProject rp : new ArrayList<RebaseProject>(all_projects)) {
	 handleAllNames(rp,files,nt,xw);
       }
    }

   if (nt != null) nt.start();
}



private void handleAllNames(RebaseProject rp,Set<String> files,
      NameThread nt,IvyXmlWriter xw)
{
   if (rp == null) return;
   if (nt == null) rp.outputAllNames(files,xw);
   else nt.addProject(rp);
}




private class NameThread extends Thread {

   private String bump_id;
   private String name_id;
   private List<RebaseProject> project_names;
   private Set<String> file_set;

   NameThread(String bid,String nid,Set<String> files) {
      super("Rebase_GetNames");
      bump_id = bid;
      name_id = nid;
      file_set = files;
      project_names = new ArrayList<RebaseProject>();
    }

   void addProject(RebaseProject pp) {
      project_names.add(pp);
    }

   @Override public void run() {
      RebaseMain.logD("START NAMES FOR " + name_id);

      IvyXmlWriter xw = null;
      try {
	 for (RebaseProject rp : project_names) {
	    if (xw == null) {
	       xw = rebase_main.beginMessage("NAMES",bump_id);
	       xw.field("NID",name_id);
	     }
	    try {
	       rp.outputAllNames(file_set,xw);
	       if (xw.getLength() <= 0 || xw.getLength() > 1000000) {
		  rebase_main.finishMessageWait(xw,15000);
		  xw = null;
		}
	     }
	    catch (Throwable t) {
	       RebaseMain.logE("Problem getting names",t);
	       xw = null;
	     }
	  }

	 if (xw != null) {
	    rebase_main.finishMessageWait(xw);
	  }
       }
      finally {
	 RebaseMain.logD("FINISH NAMES FOR " + name_id);
	 xw = rebase_main.beginMessage("ENDNAMES",bump_id);
	 xw.field("NID",name_id);
	 rebase_main.finishMessage(xw);
       }
    }

}	// end of inner class NameThread



/********************************************************************************/
/*										*/
/*	Search commands 							*/
/*										*/
/********************************************************************************/

void patternSearch(String proj,String pat,String typ,boolean defs,boolean refs,
      boolean sys,IvyXmlWriter xw)
	throws RebaseException
{
   if (proj != null) {
      RebaseProject rp = findProject(proj);
      if (rp != null) rp.patternSearch(pat,typ,defs,refs,sys,xw);
    }
   else {
      for (RebaseProject rp : new ArrayList<RebaseProject>(all_projects)) {
	 rp.patternSearch(pat,typ,defs,refs,sys,xw);
       }
    }


}


void findAll(String proj,String file,int soffset,int eoffset,boolean defs,
      boolean refs,boolean imps,
      boolean type,boolean ronly,boolean wonly,IvyXmlWriter xw)
{
   RebaseProject rp = getProject(proj,file);
   if (rp != null) {
      rp.findAll(file,soffset,eoffset,defs,refs,imps,type,ronly,wonly,xw);
    }
   else if (proj == null) {
      for (RebaseProject rp0 : new ArrayList<RebaseProject>(all_projects)) {
	 rp0.findAll(file,soffset,eoffset,defs,refs,imps,type,ronly,wonly,xw);
       }
    }
}

void findPackage(String proj,String pkg,IvyXmlWriter xw)
{
   RebaseProject rp = findProject(proj);
   if (rp == null) return;

   rp.findPackage(pkg,xw);
}

void findByKey(String proj,String bid,String key,String file,IvyXmlWriter xw)
{
   RebaseProject rp = getProject(proj,file);
   if (rp != null) rp.findByKey(file,key,xw);
}


void textSearch(String proj,int fgs,String pat,int max,IvyXmlWriter xw)
{
   if (proj != null) {
      RebaseProject rp = findProject(proj);
      if (rp != null)
	 rp.textSearch(fgs,pat,max,xw);
    }
   else {
      for (RebaseProject rp : new ArrayList<RebaseProject>(all_projects)) {
	 rp.textSearch(fgs,pat,max,xw);
       }
    }
}


void getFullyQualifiedName(String proj,String file,int spos,int epos,IvyXmlWriter xw)
{
   RebaseProject rp = getProject(proj,file);
   if (rp != null) rp.getFullyQualifiedName(file,spos,epos,xw);
}


void getTextRegions(String proj,String bid,String file,String cls,boolean pfx,
      boolean statics,boolean compunit,boolean imports,boolean pkg,
      boolean topdecls,boolean fields,boolean all,IvyXmlWriter xw) throws RebaseException
{
   RebaseProject rp = getProject(proj,file);
   if (rp != null) rp.getTextRegions(bid,file,cls,pfx,statics,compunit,imports,pkg,topdecls,fields,all,xw);
}




/********************************************************************************/
/*										*/
/*	Formatting methods							*/
/*										*/
/********************************************************************************/

void formatCode(String proj,String bid,String file,
      int spos,int epos,IvyXmlWriter xw)
{
   RebaseProject rp = getProject(proj,file);
   if (rp != null) {
      rp.formatCode(file,spos,epos,xw);
    }
   else if (proj == null) {
      for (RebaseProject rp0 : new ArrayList<RebaseProject>(all_projects)) {
	 rp0.formatCode(file,spos,epos,xw);
       }
    }
}





/********************************************************************************/
/*										*/
/*	Handle user selections of good/bad items				*/
/*										*/
/********************************************************************************/

void handleAccept(String proj,String file,boolean fg,int spos,int epos)
{
   RebaseProject rp = getProject(proj,file);
   if (rp != null)  rp.noteAccept(file,fg);
}



/********************************************************************************/
/*										*/
/*	Handle export requests							*/
/*										*/
/********************************************************************************/

void handleExport(String proj,String dir,boolean accepted,String file)
	throws RebaseException
{
   File fdir = new File(dir);
   if (!fdir.exists() && !fdir.mkdirs())
      throw new RebaseException("Can't create export directory " + dir);
   if (!fdir.isDirectory())
      throw new RebaseException("Not a directory: " + dir);

   RebaseProject rp = getProject(proj,file);
   if (rp != null) {
      rp.export(fdir,file,accepted);
    }
   else {
      for (RebaseProject rp0 : new ArrayList<RebaseProject>(all_projects)) {
	 rp0.export(fdir,file,accepted);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private RebaseProject getProject(String proj,String file)
{
   if (proj == null && file != null) {
      StringTokenizer tok = new StringTokenizer(file,"/");
      if (tok.countTokens() > 3) {	// File is /REBUS/<project>/<Engine>/...
	 String rebus = tok.nextToken();
	 if (!rebus.equals("REBUS")) return null;
	 proj = tok.nextToken();
       }
    }

   if (proj == null) return null;

   return findProject(proj);
}


/********************************************************************************/
/*										*/
/*	Search Commands 							*/
/*										*/
/********************************************************************************/

void handleNext(String proj,String file)
{
   RebaseFile rf = findFile(file);
   if (rf == null) return;
   RebaseRequest rq = rf.getRequest();
   if (rq == null) return;
   rq.startNextSearch();
}



void handleExpand(String proj,String file)
{
   RebaseFile rf = findFile(file);
   if (rf == null) return;
   RebaseSource rs = rf.getSource().getBaseSource();
   if (rs == null) return;
   RebaseProject rp = getProject(proj,file);
   if (rp == null) return;
   BackgroundExpander bs = new BackgroundExpander(rp,rf,rs);
   rebase_main.startTask(bs);
}


private class BackgroundExpander implements Runnable {

   private RebaseProject for_project;
   private RebaseFile for_file;
   private RebaseSource for_source;

   BackgroundExpander(RebaseProject rp,RebaseFile rf,RebaseSource rs) {
      for_project = rp;
      for_file = rf;
      for_source = rs;
    }

   @Override public void run() {
      switch (for_source.getSourceType()) {
	 case FILE :
	    for_source.setSourceType(SourceType.PACKAGE);
	    for_project.addPackageFiles(for_file);
	    break;
	 case PACKAGE :
	    for_source.setSourceType(SourceType.SYSTEM);
	    for_project.addSystemFiles(for_file);
	    break;
	 case SYSTEM :
	    break;
       }
    }

}	// end of inner class BackgroundExpander




}	// end of class RebaseProjectManager




/* end of RebaseProjectManager.java */

