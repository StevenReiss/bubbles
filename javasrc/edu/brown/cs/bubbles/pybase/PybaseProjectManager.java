/********************************************************************************/
/*										*/
/*		PybaseProjectManager.java					*/
/*										*/
/*	Python Bubbles Base file and project manager				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractInterpreterManager;
import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.Found;
import edu.brown.cs.bubbles.pybase.symbols.SourceToken;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.stmtType;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class PybaseProjectManager implements PybaseConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybaseMain pybase_main;
private File	work_space;
private Map<String,PybaseProject> all_projects;
private List<InterpreterSpec> all_interpreters;

private static final Pattern EXEC_PAT = Pattern.compile("^EXECUTABLE\\:(.*)\\|$");

private static final String PROJECTS_FILE = ".projects";






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseProjectManager(PybaseMain pm,File ws) throws PybaseException
{
   pybase_main = pm;

   if (!ws.exists()) ws.mkdirs();
   if (!ws.exists() || !ws.isDirectory())
      throw new PybaseException("Illegal work space specified: " + ws);

   work_space = ws;
   all_projects = new TreeMap<String,PybaseProject>();
   all_interpreters = new ArrayList<InterpreterSpec>();

   loadPythonData();
   setupPydev();
}



/********************************************************************************/
/*										*/
/*	Setup methods							       */
/*										*/
/********************************************************************************/

IPathSpec createPathSpec(Element xml)		{ return new PathSpec(xml); }

IPathSpec createPathSpec(File src,boolean user,boolean output,boolean rel) {
   return new PathSpec(src,user,output,rel);
}

IFileSpec createFileSpec(Element xml)		{ return new FileSpec(xml); }

IPathSpec createSourcePath(File dir,boolean rel)
{
   return new PathSpec(dir,true,true,rel);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

PybaseProject findProject(String proj) throws PybaseException
{
   if (proj == null) return null;

   PybaseProject pp = all_projects.get(proj);
   if (pp == null) throw new PybaseException("Unknown project " + proj);

   return pp;
}


List<ISemanticData> getAllSemanticData(String proj) throws PybaseException
{
   List<ISemanticData> rslt = new ArrayList<ISemanticData>();

   if (proj != null) {
      PybaseProject pp = findProject(proj);
      addSemanticData(pp,rslt);
    }
   else {
      for (PybaseProject pp : all_projects.values()) {
	 addSemanticData(pp,rslt);
       }
    }

   return rslt;
}



File getWorkSpaceDirectory()
{
   return pybase_main.getWorkSpaceDirectory();
}



/********************************************************************************/
/*										*/
/*	LIST PROJECTS command							*/
/*										*/
/********************************************************************************/

void handleListProjects(IvyXmlWriter xw)
{
   for (PybaseProject p : all_projects.values()) {
      xw.begin("PROJECT");
      xw.field("NAME",p.getName());
      xw.field("ISPYTHON",true);
      xw.field("BASE",p.getBasePath().getPath());
      xw.end("PROJECT");
    }
}



/********************************************************************************/
/*										*/
/*	CREATE PROJECT command							*/
/*										*/
/********************************************************************************/

void handleCreateProject(String name,String dir,IvyXmlWriter xw) throws PybaseException
{
   File pdir = new File(dir);
   if (!pdir.exists() && !pdir.mkdirs()) {
      throw new PybaseException("Can't create project directory " + pdir);
    }
   if (!pdir.isDirectory()) {
      throw new PybaseException("Project path must be a directory " + pdir);
    }
   pdir = IvyFile.getCanonical(pdir);

   loadProject(name,pdir);

   saveProjects();
}



/********************************************************************************/
/*										*/
/*	EDITPROJECT command							*/
/*										*/
/********************************************************************************/

void handleEditProject(String name,Element pxml,IvyXmlWriter xw) throws PybaseException
{
    PybaseProject pp = findProject(name);
    if (pp == null) throw new PybaseException("Can't find project " + name);
    pp.editProject(pxml);
}




/********************************************************************************/
/*										*/
/*	Handle CREATEPACKAGE command						*/
/*										*/
/********************************************************************************/

void handleCreatePackage(String proj,String name,boolean force,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseProject pp = findProject(proj);
   if (pp == null) throw new PybaseException("Can't find project " + name);
   pp.createPackage(name,force,xw);
}




/********************************************************************************/
/*										*/
/*	Handle FINDPACKAGE							*/
/*										*/
/********************************************************************************/

void handleFindPackage(String proj,String name,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseProject pp = findProject(proj);
   if (pp == null) throw new PybaseException("Can't find project " + proj);
   pp.findPackage(name,xw);
}




/********************************************************************************/
/*										*/
/*	Handle CREATECLASS (new module) 					*/
/*										*/
/********************************************************************************/

void handleNewModule(String proj,String name,boolean force,String cnts,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseProject pp = findProject(proj);
   if (pp == null) throw new PybaseException("Can't find project " + name);
   pp.createModule(name,cnts,xw);
}





/********************************************************************************/
/*										*/
/*	OPEN PROJECT command							*/
/*										*/
/********************************************************************************/

void handleOpenProject(String proj,boolean files,boolean paths,boolean classes,boolean opts,
			  IvyXmlWriter xw) throws PybaseException
{
   PybaseProject p = all_projects.get(proj);
   if (p == null) throw new PybaseException("Unknown project " + proj);

   p.open();

   if (xw != null) p.outputProject(files,paths,classes,opts,xw);
}



/********************************************************************************/
/*										*/
/*	BUILD PROJECT command							*/
/*										*/
/********************************************************************************/

void handleBuildProject(String proj,boolean clean,boolean full,boolean refresh,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseProject p = all_projects.get(proj);
   if (p == null) throw new PybaseException("Unknown project " + proj);

   p.build(refresh,true);
}



/********************************************************************************/
/*										*/
/*	GET ALL NAMES command							*/
/*										*/
/********************************************************************************/

void handleGetAllNames(String proj,String bid,Set<String> files,String bkg,IvyXmlWriter xw)
	throws PybaseException
{
   NameThread nt = null;
   if (bkg != null) nt = new NameThread(bid,bkg);

   if (proj != null) {
      PybaseProject pp = all_projects.get(proj);
      handleAllNames(pp,files,nt,xw);
    }
   else {
      for (PybaseProject pp : new ArrayList<PybaseProject>(all_projects.values())) {
	 handleAllNames(pp,files,nt,xw);
       }
    }

   if (nt != null) nt.start();
}


private void handleAllNames(PybaseProject pp,Set<String> files,NameThread nt,IvyXmlWriter xw)
{
   if (pp == null) return;

   int ctr = 0;
   for (IFileData ifd : pp.getAllFiles()) {
      if (files != null && !files.contains(ifd.getFile().getPath())) continue;
      ISemanticData sd = pp.getParseData(ifd);
      if (sd != null) {
	 if (nt != null) nt.addRoot(ifd,pp,sd);
	 else outputTreeNames(pp,ifd,sd,xw);
       }
      ++ctr;
    }

   if (ctr == 0) {
      if (nt == null) PybaseUtil.outputProjectSymbol(pp,xw);
      else nt.addProject(pp);
    }
}


private void outputTreeNames(PybaseProject pp,IFileData file,ISemanticData sd,IvyXmlWriter xw)
{
   if (sd == null) return;
   xw.begin("FILE");
   xw.textElement("PATH",file.getFile().getAbsolutePath());
   outputTreeNames(pp,file,sd.getScope(),xw);
   outputTopDefs(pp,file,(Module) sd.getRootNode(),xw);
   xw.end("FILE");
   System.err.println("PYBASE: Output names from: " + sd.getScope());
}


private void outputTreeNames(PybaseProject pp,IFileData file,PybaseScopeItems scp,IvyXmlWriter xw)
{
   if (scp == null) return;

   for (Found fnd : scp.getAllSymbols()) {
     PybaseUtil.outputSymbol(pp,file,fnd,xw);
     AbstractToken tok = fnd.getSingle().getGenerator();
     if (tok.getType() == TokenType.CLASS) {
	if (scp.getChildren() != null) {
	   for (PybaseScopeItems cscp : scp.getChildren()) {
	      switch (cscp.getScopeType()) {
		 case CLASS :
		    outputTreeNames(pp,file,cscp,xw);
		    break;
		 default :
		    break;
	      }
	   }
	}
      }
    }
}


private void outputTopDefs(PybaseProject pp,IFileData file,Module root,IvyXmlWriter xw)
{
   String mnm = file.getModuleName();
   String pkg = null;
   int idx = mnm.lastIndexOf(".");
   if (idx > 0) {
      pkg = mnm.substring(0,idx);
      mnm = mnm.substring(idx+1);
   }
   SourceToken modtok = new SourceToken(root,mnm,null,null,pkg);
   PybaseUtil.outputSymbol(pp,file,modtok,root,xw);

   if (root != null) {
      for (stmtType st : root.body) {
	 if (st instanceof Assign) continue;
	 if (st instanceof FunctionDef) continue;
	 if (st instanceof ClassDef) continue;
	 if (st instanceof Import) continue;
	 PybaseUtil.outputSymbol(pp,file,st,xw);
       }
    }
}




private class NameThread extends Thread {

   private String bump_id;
   private String name_id;
   private Map<IFileData,PybaseProject> ifile_project;
   private Map<IFileData,ISemanticData> ifile_data;
   private List<PybaseProject> project_names;

   NameThread(String bid,String nid) {
      super("Pybase_GetNames");
      bump_id = bid;
      name_id = nid;
      ifile_project = new HashMap<IFileData,PybaseProject>();
      ifile_data = new HashMap<IFileData,ISemanticData>();
      project_names = new ArrayList<PybaseProject>();
    }

   void addProject(PybaseProject pp) {
      project_names.add(pp);
    }

   void addRoot(IFileData ifd,PybaseProject pp,ISemanticData sd) {
      ifile_project.put(ifd,pp);
      ifile_data.put(ifd,sd);
    }

   @Override public void run() {
      PybaseMain.logD("START NAMES FOR " + name_id);

      IvyXmlWriter xw = null;

      for (Map.Entry<IFileData,PybaseProject> ent : ifile_project.entrySet()) {
	 if (xw == null) {
	    xw = pybase_main.beginMessage("NAMES",bump_id);
	    xw.field("NID",name_id);
	  }
	 IFileData ifd = ent.getKey();
	 PybaseProject pp = ent.getValue();
	 ISemanticData sd = ifile_data.get(ifd);
	 outputTreeNames(pp,ifd,sd,xw);
	 if (xw.getLength() <= 0 || xw.getLength() > 1000000) {
	    pybase_main.finishMessageWait(xw,15000);
	    PybaseMain.logD("OUTPUT NAMES: " + xw.toString());
	    xw = null;
	  }
       }

      for (PybaseProject pp : project_names) {
	 if (xw == null) {
	    xw = pybase_main.beginMessage("NAMES",bump_id);
	    xw.field("NID",name_id);
	  }
	 PybaseUtil.outputProjectSymbol(pp,xw);
       }

      if (xw != null) {
	 pybase_main.finishMessageWait(xw);
       }

      PybaseMain.logD("FINISH NAMES FOR " + name_id);
      xw = pybase_main.beginMessage("ENDNAMES",bump_id);
      xw.field("NID",name_id);
      pybase_main.finishMessage(xw);
    }

}	// end of inner class NameThread






/********************************************************************************/
/*										*/
/*	Handle interpreter management						*/
/*										*/
/********************************************************************************/

private void loadPythonData()
{
   File mf = new File(work_space,".metadata");
   File pf = new File(mf,".pybase");
   pf = new File(pf,".pythondata");
   if (pf.exists()) {
      Element xml = IvyXml.loadXmlFromFile(pf);
      for (Element pe : IvyXml.children(xml,"PYTHON")) {
	 InterpreterSpec is = new InterpreterSpec(pe);
	 all_interpreters.add(is);
       }
    }
}


private void storePythonData()
{
   File mf = new File(work_space,".metadata");
   File pf = new File(mf,".pybase");
   pf = new File(pf,".pythondata");
   try {
      IvyXmlWriter xw = new IvyXmlWriter(pf);
      xw.begin("PYBASE");
      for (InterpreterSpec is : all_interpreters) {
	 is.outputXml(xw);
       }
      xw.end("PYBASE");
      xw.close();
    }
   catch (IOException e) { }
}




IInterpreterSpec findInterpreter(File exe)
{
   if (exe == null) {
      IInterpreterSpec s1 = findInterpreter(new File("python"));
      if (s1 != null) return s1;
      s1 = findInterpreter(new File("jython"));
      if (s1 != null) return s1;
      s1 = findInterpreter(new File("ironpython"));
      if (s1 != null) return s1;
      return null;
    }

   for (InterpreterSpec is : all_interpreters) {
      if (exe.equals(is.getExecutable())) return is;
      if (exe.getPath().equals(is.getExecutable().getName())) return is;
    }

   InterpreterSpec nis = new InterpreterSpec(exe);
   if (nis.getPythonVersion() == PybaseVersion.DEFAULT) {
      nis = new InterpreterSpec(exe.getName());
    }

   PybaseMain.logD("Try interpreter " + nis.getPythonVersion());

   if (nis.getPythonVersion() == PybaseVersion.DEFAULT) return null;

   for (InterpreterSpec is : all_interpreters) {
      if (is.getExecutable().equals(nis.getExecutable())) return is;
    }

   AbstractInterpreterManager mgr = PybaseNature.getInterpreterManager(nis.getPythonType());

   if (mgr != null) {
      mgr.createInterpreterInfo(nis.getExecutable().getPath(),false);
    }

   all_interpreters.add(nis);
   storePythonData();
   setupPydev();

   return nis;
}




/********************************************************************************/
/*										*/
/*	Pydev manager interface 						*/
/*										*/
/********************************************************************************/

private void setupPydev()
{
   AbstractInterpreterManager mgr = PybaseNature.getInterpreterManager(PybaseInterpreterType.PYTHON);
   setupPydev(PybaseInterpreterType.PYTHON,mgr);
   mgr = PybaseNature.getInterpreterManager(PybaseInterpreterType.JYTHON);
   setupPydev(PybaseInterpreterType.JYTHON,mgr);
   mgr = PybaseNature.getInterpreterManager(PybaseInterpreterType.IRONPYTHON);
   setupPydev(PybaseInterpreterType.IRONPYTHON,mgr);
}



private void setupPydev(PybaseInterpreterType pt,AbstractInterpreterManager mgr)
{
   if (mgr == null) return;

   List<PybaseInterpreter> infos = new ArrayList<PybaseInterpreter>();
   for (InterpreterSpec is : all_interpreters) {
      if (is.getPythonType() == pt) {
	 PybaseInterpreter ii = is.getPydevInfo();
	 if (ii != null) infos.add(ii);
       }
    }

   if (infos.size() == 0) return;

   PybaseInterpreter [] infoa = new PybaseInterpreter[infos.size()];
   infoa = infos.toArray(infoa);
   mgr.setInfos(infoa,null);
}




/********************************************************************************/
/*										*/
/*	Interpreter data							*/
/*										*/
/********************************************************************************/

private class InterpreterSpec implements IInterpreterSpec {

   private PybaseVersion python_version;
   private PybaseInterpreterType python_type;
   private File python_binary;
   private List<PathSpec> path_specs;
   private List<ModuleSpec> module_specs;
   private PybaseInterpreter pydev_info;

   InterpreterSpec(Element xml) {
      this();
      python_version = IvyXml.getAttrEnum(xml,"VERSION",PybaseVersion.DEFAULT);
      python_type = IvyXml.getAttrEnum(xml,"TYPE",PybaseInterpreterType.PYTHON);
      python_binary = new File(IvyXml.getTextElement(xml,"EXE"));
      for (Element pe : IvyXml.children(xml,"PATH")) {
	 PathSpec ps = new PathSpec(pe);
	 path_specs.add(ps);
       }
      for (Element me : IvyXml.children(xml,"MODULE")) {
	 ModuleSpec ms = new ModuleSpec(me);
	 module_specs.add(ms);
       }
    }

   InterpreterSpec(String nm) {
      this(new File(nm));
    }

   InterpreterSpec(File exe) {
      this();
      File f1 = pybase_main.getRootDirectory();
      File f2 = new File(f1,"PySrc");
      File f3 = new File(f2,"interpreterInfo.py");
   
      List<String> args = new ArrayList<String>();
      args.add(exe.getPath());
      args.add(f3.getPath());
      PybaseMain.logD("Run: " + exe.getPath() + " " + f3.getPath());
   
      try {
         IvyExec ex = new IvyExec(args,null,null,IvyExec.READ_OUTPUT);
         InputStream ins = ex.getInputStream();
         InputStreamReader isr = new InputStreamReader(ins);
         BufferedReader br = new BufferedReader(isr);
         List<String> lines = new ArrayList<String>();
         for ( ; ; ) {
            String ln = br.readLine();
            if (ln == null) break;
            PybaseMain.logD("OUTPUT: " + ln);
            lines.add(ln);
          }
         loadInterpreterData(lines);
         ex.waitFor();
       }
      catch (IOException e) {
         PybaseMain.logD("Problem finding interpreter " + exe);
       }
    }

   private InterpreterSpec() {
      python_version = PybaseVersion.DEFAULT;
      python_type = PybaseInterpreterType.PYTHON;
      python_binary = null;
      path_specs = new ArrayList<PathSpec>();
      module_specs = new ArrayList<ModuleSpec>();
      pydev_info = null;
    }

   @Override public String getName() {
      String type = python_type.toString().toLowerCase();
      String v = python_version.toString();
      if (v.startsWith("VERSION_")) v = v.substring(8);
      v = v.replace("_",".");
      return type + " " + v;
    }

   @Override public PybaseVersion getPythonVersion()		{ return python_version; }
   @Override public PybaseInterpreterType getPythonType()	{ return python_type; }
   @Override public File getExecutable()			{ return python_binary; }

   @Override public List<File> getPythonPath() {
      List<File> rslt = new ArrayList<File>();
      for (PathSpec ps : path_specs) {
	 rslt.add(ps.getFile());
       }
      return rslt;
    }

   @Override public List<String> getForcedLibraries() {
      List<String> rslt = new ArrayList<String>();
      for (ModuleSpec ms : module_specs) {
	 rslt.add(ms.getName());
       }
      return rslt;
    }

   PybaseInterpreter getPydevInfo() {
      if (pydev_info == null) {
	 AbstractInterpreterManager mgr = PybaseNature.getInterpreterManager(python_type);
	 if (mgr != null) {
	    pydev_info = mgr.createInterpreterInfo(getExecutable().getPath(),false);
	  }
       }
      return pydev_info;
    }

   void outputXml(IvyXmlWriter xw) {
     xw.begin("PYTHON");
     xw.field("VERSION",python_version);
     xw.field("TYPE",python_type);
     xw.textElement("EXE",python_binary);
     for (PathSpec ps : path_specs) {
	ps.outputXml(xw);
      }
     for (ModuleSpec ms : module_specs) {
	ms.outputXml(xw);
      }
     xw.end("PYTHON");
   }

   private void loadInterpreterData(List<String> lines) {
      if (lines.size() < 2) return;

      String vname = lines.get(0);
      if (vname.contains("2.4")) python_version = PybaseVersion.VERSION_2_4;
      else if (vname.contains("2.5")) python_version = PybaseVersion.VERSION_2_5;
      else if (vname.contains("2.6")) python_version = PybaseVersion.VERSION_2_6;
      else if (vname.contains("2.7")) python_version = PybaseVersion.VERSION_2_7;
      else if (vname.contains("3.0")) python_version = PybaseVersion.VERSION_3_0;
      else python_version = PybaseVersion.DEFAULT;

      Matcher m1 = EXEC_PAT.matcher(lines.get(1));
      if (m1.matches()) {
	 python_binary = new File(m1.group(1));
	 String nm = python_binary.getName();
	 if (nm.equals("jython")) python_type = PybaseInterpreterType.JYTHON;
	 else if (nm.equals("ironpython")) python_type = PybaseInterpreterType.IRONPYTHON;
	 else python_type = PybaseInterpreterType.PYTHON;
       }

      int ln = 2;
      while (ln < lines.size() && lines.get(ln).startsWith("|")) {
	 String lib = lines.get(ln);
	 lib = lib.substring(1);
	 if (lib.endsWith("OUT_PATH")) {
	    lib = lib.substring(0,lib.length() - 8);
	  }
	 else if (lib.endsWith("INS_PATH")) {
	    lib = lib.substring(0,lib.length() - 8);
	  }
	 PathSpec ps = new PathSpec(new File(lib),false,false,false);
	 path_specs.add(ps);
	 ++ln;
       }

      if (lines.get(ln).startsWith("@")) {
	 ++ln;
	 while (ln < lines.size() && lines.get(ln).startsWith("|")) {
	    String dll = lines.get(ln);
	    dll = dll.substring(1);
	    ++ln;
	  }
       }

      if (lines.get(ln).startsWith("$")) {
	 ++ln;
	 while (ln < lines.size() && lines.get(ln).startsWith("|")) {
	    String mod = lines.get(ln);
	    mod = mod.substring(1);
	    ModuleSpec ms = new ModuleSpec(mod,true);
	    module_specs.add(ms);
	    ++ln;
	  }
       }
    }
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
	 File pf = new File(df,".pyproject");
	 if (pf.exists()) {
	    loadProject(df.getName(),pf);
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
      for (PybaseProject pp : all_projects.values()) {
	 xw.begin("PROJECT");
	 xw.field("NAME",pp.getName());
	 xw.field("PATH",pp.getBasePath().getPath());
	 xw.end("PROJECT");
       }
      xw.end("PROJECTS");
      xw.close();
    }
   catch (IOException e) {
      PybaseMain.logE("Problem writing project file",e);
    }
}


private void loadProject(String nm,File pf)
{
   PybaseProject p = new PybaseProject(pybase_main,nm,pf);
   all_projects.put(p.getName(),p);
}



/********************************************************************************/
/*										*/
/*	Helper routines 							*/
/*										*/
/********************************************************************************/

private static File getAbsolutePath(File par,File chld)
{
   File p = chld.getParentFile();
   if (p == null) return new File(par,chld.getName());
   else {
      File f1 = getAbsolutePath(par,p);
      return new File(f1,chld.getName());
    }
}



private void addSemanticData(PybaseProject pp,List<ISemanticData> rslt)
{
   for (IFileData ifd : pp.getAllFiles()) {
      ISemanticData isd = pp.getParseData(ifd);
      rslt.add(isd);
    }
}




/********************************************************************************/
/*										*/
/*	Path specification							*/
/*										*/
/********************************************************************************/

private static class PathSpec implements IPathSpec {

   private File directory_file;
   private boolean is_user;
   private boolean is_output;
   private boolean is_relative;

   PathSpec(Element xml) {
      directory_file = new File(IvyXml.getTextElement(xml,"DIR"));
      is_user = IvyXml.getAttrBool(xml,"USER");
      is_output = IvyXml.getAttrBool(xml,"OUTPUT");
      is_relative = IvyXml.getAttrBool(xml,"RELATIVE");
    }

   PathSpec(File f,boolean u,boolean o,boolean r) {
      directory_file = f;
      is_user = u;
      is_output = o;
      is_relative = r;
    }

   @Override public File getFile()		{ return directory_file; }
   @Override public boolean isUser()		{ return is_user; }
   @Override public boolean isRelative()	{ return is_relative; }
   @Override public File getOSFile(File base) {
      if (!is_relative) return directory_file;
      return getAbsolutePath(base,directory_file);
    }

   @Override public void outputXml(IvyXmlWriter xw) {
      xw.begin("PATH");
      xw.field("DIR",directory_file.getPath());
      xw.field("USER",is_user);
      xw.field("OUTPUT",is_output);
      xw.field("RELATIVE",is_relative);
      xw.end("PATH");
    }

}	// end of inner class PathSpec




/********************************************************************************/
/*										*/
/*	Module information							*/
/*										*/
/********************************************************************************/

private static class ModuleSpec implements IModuleSpec {

   private String module_name;
   boolean is_required;

   ModuleSpec(Element xml) {
      module_name = IvyXml.getAttrString(xml,"NAME");
      is_required = IvyXml.getAttrBool(xml,"REQUIRED");
    }

   ModuleSpec(String nm,boolean req) {
      module_name = nm;
      is_required = req;
    }

   @Override public String getName()			{ return module_name; }
   @Override public boolean isRequired()		{ return is_required; }

   @Override public void outputXml(IvyXmlWriter xw) {
      xw.begin("MODULE");
      xw.field("NAME",module_name);
      xw.field("REQUIRED",is_required);
      xw.end("MODULE");
    }

}	// end of inner class ModuleSpec



/********************************************************************************/
/*										*/
/*	File information							*/
/*										*/
/********************************************************************************/

private static class FileSpec implements IFileSpec{

   private File source_file;

   FileSpec(Element xml) {
      source_file = new File(IvyXml.getTextElement(xml,"NAME"));
    }

   @Override public File getFile()			{ return source_file; }
   @Override public FileType getFileType()		{ return FileType.UNKNOWN; }

   @Override public Collection<IFileSpec> getRelatedFiles() {
      return null;
    }

   @Override public void outputXml(IvyXmlWriter xw) {
      xw.begin("FILE");
      xw.field("NAME",source_file.getPath());
      xw.end("FILE");
    }

}	// end of inner class FileSpec





}	// end of class PybaseProjectManager




/* end of PybaseProjectManager.java */


