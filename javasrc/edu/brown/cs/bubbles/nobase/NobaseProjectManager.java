/********************************************************************************/
/*										*/
/*		NobaseProjectManager.java					*/
/*										*/
/*	Project manager for nodebubbles 					*/
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
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import org.eclipse.wst.jsdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;



class NobaseProjectManager implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseMain	nobase_main;
private File		work_space;
private Map<String,NobaseProject> all_projects;

private static final String PROJECTS_FILE = ".projects";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseProjectManager(NobaseMain nm,File ws) throws NobaseException
{
   nobase_main = nm;

   if (!ws.exists()) ws.mkdirs();
   if (!ws.exists() || !ws.isDirectory())
      throw new NobaseException("Illegal work space specified: " + ws);

   work_space = ws;
   all_projects = new TreeMap<String,NobaseProject>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

NobasePathSpec createPathSpec(Element xml)		{ return new NobasePathSpec(xml); }

NobasePathSpec createPathSpec(File src,boolean user,boolean exclude,boolean nest) {
   return new NobasePathSpec(src,user,exclude,nest);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

NobaseProject findProject(String proj) throws NobaseException
{
   if (proj == null) return null;

   NobaseProject pp = all_projects.get(proj);
   if (pp == null) throw new NobaseException("Unknown project " + proj);

   return pp;
}



File getWorkSpaceDirectory()
{
   return nobase_main.getWorkSpaceDirectory();
}


List<ISemanticData> getAllSemanticData(String proj) throws NobaseException
{
   List<ISemanticData> rslt = new ArrayList<ISemanticData>();

   if (proj != null) {
      NobaseProject pp = findProject(proj);
      addSemanticData(pp,rslt);
    }
   else {
      for (NobaseProject pp : all_projects.values()) {
	 addSemanticData(pp,rslt);
       }
    }

   return rslt;
}


Collection<NobaseProject> getAllProjects()
{
   return all_projects.values();
}


/********************************************************************************/
/*										*/
/*	LIST PROJECTS command							*/
/*										*/
/********************************************************************************/

void handleListProjects(IvyXmlWriter xw)
{
   for (NobaseProject p : all_projects.values()) {
      xw.begin("PROJECT");
      xw.field("NAME",p.getName());
//    xw.field("ISJS",true);
      xw.field("LANGUAGE","JAVASCRIPT");
      xw.field("BASE",p.getBasePath().getPath());
      xw.end("PROJECT");
    }
}



/********************************************************************************/
/*										*/
/*	CREATE PROJECT command							*/
/*										*/
/********************************************************************************/

void handleCreateProject(String name,String dir,IvyXmlWriter xw) throws NobaseException
{
   File pdir = new File(dir);
   if (!pdir.exists() && !pdir.mkdirs()) {
      throw new NobaseException("Can't create project directory " + pdir);
    }
   if (!pdir.isDirectory()) {
      throw new NobaseException("Project path must be a directory " + pdir);
    }
   pdir = IvyFile.getCanonical(pdir);

   loadProject(name,pdir);

   saveProjects();
   
   xw.begin("PROJECT");
   xw.field("NAME",name);
   xw.end("PROJECT");
}



/********************************************************************************/
/*										*/
/*	EDITPROJECT command							*/
/*										*/
/********************************************************************************/

void handleEditProject(String name,Element pxml,IvyXmlWriter xw) throws NobaseException
{
   NobaseProject pp = findProject(name);
   if (pp == null) throw new NobaseException("Can't find project " + name);
   pp.editProject(pxml);
}




/********************************************************************************/
/*										*/
/*	Handle CREATEPACKAGE command						*/
/*										*/
/********************************************************************************/

void handleCreatePackage(String proj,String name,boolean force,IvyXmlWriter xw)
throws NobaseException
{
   NobaseProject pp = findProject(proj);
   if (pp == null) throw new NobaseException("Can't find project " + name);
   pp.createPackage(name,force,xw);
}




/********************************************************************************/
/*										*/
/*	Handle FINDPACKAGE							*/
/*										*/
/********************************************************************************/

void handleFindPackage(String proj,String name,IvyXmlWriter xw)
throws NobaseException
{
   NobaseProject pp = findProject(proj);
   if (pp == null) throw new NobaseException("Can't find project " + proj);
   pp.findPackage(name,xw);
}




/********************************************************************************/
/*										*/
/*	Handle CREATECLASS (new module) 					*/
/*										*/
/********************************************************************************/

void handleNewModule(String proj,String name,boolean force,String cnts,IvyXmlWriter xw)
throws NobaseException
{
   NobaseProject pp = findProject(proj);
   if (pp == null) throw new NobaseException("Can't find project " + name);
   pp.createModule(name,cnts,xw);
}





/********************************************************************************/
/*										*/
/*	OPEN PROJECT command							*/
/*										*/
/********************************************************************************/

void handleOpenProject(String proj,boolean files,boolean paths,boolean classes,boolean opts,
      IvyXmlWriter xw) throws NobaseException
{
   NobaseProject p = all_projects.get(proj);
   if (p == null) throw new NobaseException("Unknown project " + proj);

   p.open();

   if (xw != null) p.outputProject(files,paths,classes,opts,xw);
}



/********************************************************************************/
/*										*/
/*	BUILD PROJECT command							*/
/*										*/
/********************************************************************************/

void handleBuildProject(String proj,boolean clean,boolean full,boolean refresh,IvyXmlWriter xw)
throws NobaseException
{
   NobaseProject p = all_projects.get(proj);
   if (p == null) throw new NobaseException("Unknown project " + proj);

   p.build(refresh,true);
}



/********************************************************************************/
/*										*/
/*	GET ALL NAMES command							*/
/*										*/
/********************************************************************************/

void handleGetAllNames(String proj,String bid,Set<String> files,String bkg,IvyXmlWriter xw)
throws NobaseException
{
   NameThread nt = null;
   if (bkg != null) nt = new NameThread(bid,bkg);

   if (proj != null) {
      NobaseProject pp = all_projects.get(proj);
      handleAllNames(pp,files,nt,xw);
    }
   else {
      for (NobaseProject pp : new ArrayList<NobaseProject>(all_projects.values())) {
	 handleAllNames(pp,files,nt,xw);
       }
    }

   if (nt != null) nt.start();
}


private void handleAllNames(NobaseProject pp,Set<String> files,NameThread nt,IvyXmlWriter xw)
{
   if (pp == null) return;

   int ctr = 0;
   for (NobaseFile ifd : pp.getAllFiles()) {
      if (files != null && !files.contains(ifd.getFile().getPath())) continue;
      ISemanticData sd = pp.getParseData(ifd);
      if (sd != null) {
	 if (nt != null) nt.addRoot(ifd,pp,sd);
	 else outputTreeNames(pp,ifd,sd,xw);
       }
      ++ctr;
    }

   if (ctr == 0) {
      if (nt == null) NobaseUtil.outputProjectSymbol(pp,xw);
      else nt.addProject(pp);
    }
}


private void outputTreeNames(NobaseProject pp,NobaseFile file,ISemanticData sd,IvyXmlWriter xw)
{
   if (sd == null) return;
   xw.begin("FILE");
   xw.textElement("PATH",file.getFile().getAbsolutePath());
   ASTNode root = sd.getRootNode();

   if (root != null) {
      NameWalker nw = new NameWalker(xw);
      root.accept(nw);
    }

   xw.end("FILE");
}



private static class NameWalker extends DefaultASTVisitor {

   private IvyXmlWriter xml_writer;

   NameWalker(IvyXmlWriter xw) {
      xml_writer = xw;
    }

   @Override public void postVisit(ASTNode n) {
      // NobaseName nnm = n.getNobaseName();
      // if (nnm != null) NobaseUtil.outputName(nnm,xml_writer);
      NobaseSymbol nsym = NobaseAst.getDefinition(n);
      if (nsym != null && nsym.getDefNode() == n) {
	 NobaseUtil.outputName(nsym,xml_writer);
       }
    }

   @Override public boolean visit(Block b) {
      return true;
    }

   @Override public boolean visit(FunctionDeclaration fd) {
      return true;
    }

   @Override public boolean visit(VariableDeclarationFragment d) {
      if (d.getInitializer() != null &&
            d.getInitializer() instanceof FunctionExpression) {
         return true;
       }
      return false;
    }

   @Override public boolean visit(FunctionExpression fc) {
      return true;
    }

   @Override public boolean visitNode(ASTNode n) {
      if (n instanceof VariableDeclarationStatement &&
            n.getParent() instanceof JavaScriptUnit)
         return true;
      if (n instanceof TypeDeclarationStatement) return true;
      if (n instanceof Statement) return false;
      return true;
    }

}	// end of inner class NameWalker



private class NameThread extends Thread {

   private String bump_id;
   private String name_id;
   private Map<NobaseFile,NobaseProject> ifile_project;
   private Map<NobaseFile,ISemanticData> ifile_data;
   private List<NobaseProject> project_names;

   NameThread(String bid,String nid) {
      super("Nobase_GetNames");
      bump_id = bid;
      name_id = nid;
      ifile_project = new HashMap<NobaseFile,NobaseProject>();
      ifile_data = new HashMap<NobaseFile,ISemanticData>();
      project_names = new ArrayList<NobaseProject>();
    }

   void addProject(NobaseProject pp) {
      project_names.add(pp);
    }

   void addRoot(NobaseFile ifd,NobaseProject pp,ISemanticData sd) {
      ifile_project.put(ifd,pp);
      ifile_data.put(ifd,sd);
    }

   @Override public void run() {
      NobaseMain.logD("START NAMES FOR " + name_id);

      IvyXmlWriter xw = null;

      for (Map.Entry<NobaseFile,NobaseProject> ent : ifile_project.entrySet()) {
	 if (xw == null) {
	    xw = nobase_main.beginMessage("NAMES",bump_id);
	    xw.field("NID",name_id);
	  }
	 NobaseFile ifd = ent.getKey();
	 NobaseProject pp = ent.getValue();
	 ISemanticData sd = ifile_data.get(ifd);
	 outputTreeNames(pp,ifd,sd,xw);
	 if (xw.getLength() <= 0 || xw.getLength() > 1000000) {
	    nobase_main.finishMessageWait(xw,15000);
	    NobaseMain.logD("OUTPUT NAMES: " + xw.toString());
	    xw = null;
	  }
       }

      for (NobaseProject pp : project_names) {
	 if (xw == null) {
	    xw = nobase_main.beginMessage("NAMES",bump_id);
	    xw.field("NID",name_id);
	  }
	 NobaseUtil.outputProjectSymbol(pp,xw);
       }

      if (xw != null) {
	 nobase_main.finishMessageWait(xw);
       }

      NobaseMain.logD("FINISH NAMES FOR " + name_id);
      xw = nobase_main.beginMessage("ENDNAMES",bump_id);
      xw.field("NID",name_id);
      nobase_main.finishMessage(xw);
    }

}	// end of inner class NameThread





/********************************************************************************/
/*										*/
/*	Handle search commadns							*/
/*										*/
/********************************************************************************/

void handlePatternSearch(String proj,String pat,String sf,
      boolean defs,boolean refs,boolean sys,IvyXmlWriter xw)
	throws NobaseException
{
   if (proj != null) {
      NobaseProject np = findProject(proj);
      if (np != null) np.patternSearch(pat,sf,defs,refs,sys,xw);
    }
   else {
      for (NobaseProject np : new ArrayList<NobaseProject>(all_projects.values())) {
	 np.patternSearch(pat,sf,defs,refs,sys,xw);
       }
    }
}


void handleKeySearch(String proj,String file,String key,IvyXmlWriter xw)
	throws NobaseException
{
   String what = "Field";
   if (key.endsWith("()")) what = "Method";
   handlePatternSearch(proj,key,what,true,false,false,xw);
}



void handleFindAll(String proj,String file,int soffset,int eoffset,boolean defs,
      boolean refs,boolean imps,
      boolean type,boolean ronly,boolean wonly,IvyXmlWriter xw)
{
   NobaseProject rp = getProject(proj,file);
   if (rp != null) {
      rp.findAll(file,soffset,eoffset,defs,refs,imps,type,ronly,wonly,xw);
    }
   else if (proj == null) {
      for (NobaseProject rp0 : new ArrayList<NobaseProject>(all_projects.values())) {
	 rp0.findAll(file,soffset,eoffset,defs,refs,imps,type,ronly,wonly,xw);
       }
    }
}



void getFullyQualifiedName(String proj,String file,int spos,int epos,IvyXmlWriter xw)
{
   NobaseProject np = getProject(proj,file);
   if (np != null) np.getFullyQualifiedName(file,spos,epos,xw);
}



void getTextRegions(String proj,String bid,String file,String cls,boolean pfx,
      boolean statics,boolean compunit,boolean imports,boolean pkg,
      boolean topdecls,boolean fields,boolean all,IvyXmlWriter xw) throws NobaseException
{
   NobaseProject rp = getProject(proj,file);
   if (rp != null) rp.getTextRegions(bid,file,cls,pfx,statics,compunit,imports,pkg,topdecls,fields,all,xw);
}



/********************************************************************************/
/*										*/
/*	Handle get completions command						*/
/*										*/
/********************************************************************************/

void getCompletions(String proj,String bid,String file,int offset,IvyXmlWriter xw)
	throws NobaseException
{
   NobaseProject rp = getProject(proj,file);
   if (rp != null) {
      rp.getCompletions(file,offset,xw);
    }
}



/********************************************************************************/
/*										*/
/*	Preferences commands							*/
/*										*/
/********************************************************************************/

void handleGetPreferences(String proj,IvyXmlWriter xw)
{
   NobaseProject np = getProject(proj,null);
   if (np == null) {
      nobase_main.getSystemPreferences().dumpPreferences(xw);
    }
   else {
      np.getPreferences().dumpPreferences(xw);
    }
}



void handleSetPreferences(String proj,Element xml)
{
   if (proj == null) {
      nobase_main.getSystemPreferences().setPreferences(xml);
      for (NobaseProject np0 : new ArrayList<NobaseProject>(all_projects.values())) {
	 np0.getPreferences().setPreferences(xml);
       }
    }
   else {
      NobaseProject np = getProject(proj,null);
      if (np != null) np.getPreferences().setPreferences(xml);
    }
}




/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private NobaseProject getProject(String proj,String file)
{
   if (proj == null && file != null) {
      StringTokenizer tok = new StringTokenizer(file,"/");
      if (tok.countTokens() > 3) {	// File is /REBUS/<project>/<Engine>/...
	 String rebus = tok.nextToken();
	 if (rebus.equals("REBUS")) {
            proj = tok.nextToken();
          }
       }
    }
   if (proj == null && file != null) {
      File f = new File(file);
      for (NobaseProject p : all_projects.values()) {
         if (p.containsFile(f)) return p;
       }
    }

   if (proj == null) return null;

   try {
      return findProject(proj);
    }
   catch (NobaseException e) { }

   return null;
}


/********************************************************************************/
/*										*/
/*	Handle loading projects 						*/
/*										*/
/********************************************************************************/

void loadProjects()
{
   File f = new File(work_space,PROJECTS_FILE);
   if (f.exists()) {
      Element xml = IvyXml.loadXmlFromFile(f);
      if (xml != null) {
	 for (Element pelt : IvyXml.children(xml,"PROJECT")) {
	    String nm = IvyXml.getAttrString(pelt,"NAME");
	    String pnm = IvyXml.getAttrString(pelt,"PATH");
	    File pf = new File(pnm);
	    if (pf.exists()) loadProject(nm,pf);
	  }
	 return;
       }
    }

   for (File df : work_space.listFiles()) {
      if (!df.getName().startsWith(".") && df.isDirectory()) {
	 File pf = new File(df,".jsproject");
	 if (pf.exists()) {
	    loadProject(df.getName(),df);
	  }
       }
    }

   saveProjects();
}


void saveProjects()
{
   File f = new File(work_space,PROJECTS_FILE);
   try {
      IvyXmlWriter xw = new IvyXmlWriter(f);
      xw.begin("PROJECTS");
      for (NobaseProject pp : all_projects.values()) {
	 xw.begin("PROJECT");
	 xw.field("NAME",pp.getName());
	 xw.field("PATH",pp.getBasePath().getPath());
	 xw.end("PROJECT");
       }
      xw.end("PROJECTS");
      xw.close();
    }
   catch (IOException e) {
      NobaseMain.logE("Problem writing project file",e);
    }
}


private void loadProject(String nm,File pf)
{
   NobaseProject p = null;

   File f = new File(pf,".html");
   if (f.exists()) p = new NobaseProjectWeb(nobase_main,nm,pf);
   else p = new NobaseProjectNode(nobase_main,nm,pf);

   all_projects.put(p.getName(),p);
}



/********************************************************************************/
/*										*/
/*	Helper routines 							*/
/*										*/
/********************************************************************************/

private void addSemanticData(NobaseProject pp,List<ISemanticData> rslt)
{
   for (NobaseFile ifd : pp.getAllFiles()) {
      ISemanticData isd = pp.getParseData(ifd);
      rslt.add(isd);
    }
}




}	// end of class NobaseProjectManager




/* end of NobaseProjectManager.java */

