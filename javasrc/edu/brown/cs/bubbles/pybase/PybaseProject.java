/********************************************************************************/
/*										*/
/*		PybaseProject.java						*/
/*										*/
/*	Python Bubbles Base holder of project information			*/
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


package edu.brown.cs.bubbles.pybase;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public class PybaseProject implements PybaseConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybaseMain	pybase_main;
private PybaseProjectManager project_manager;
private String project_name;
private File base_directory;
private List<IPathSpec> project_paths;
private List<IFileSpec> project_files;
private PybaseNature project_nature;
private boolean is_open;
private IInterpreterSpec python_interpreter;
private PybasePreferences pybase_prefs;

private Map<IFileData,ISemanticData> parse_data;
private Set<IFileData> all_files;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseProject(PybaseMain pm,String name,File base)
{
   pybase_main = pm;
   project_manager = pm.getProjectManager();
   base_directory = base.getAbsoluteFile();
   try {
      base_directory = base_directory.getCanonicalFile();
   }
   catch (IOException e) { }
   if (name == null) name = base.getName();
   project_name = name;
   project_paths = new ArrayList<IPathSpec>();
   project_files = new ArrayList<IFileSpec>();
   pybase_prefs = new PybasePreferences(pm.getSystemPreferences());
   all_files = new HashSet<IFileData>();
   parse_data = new HashMap<IFileData,ISemanticData>();

   File f = new File(base_directory,".pybase");
   if (!f.exists()) f.mkdir();

   File f1 = new File(base_directory,".pyproject");
   Element xml = IvyXml.loadXmlFromFile(f1);
   if (xml == null) {
      setupDefaults();
    }
   else {
      String bfile = IvyXml.getTextElement(xml,"EXE");
      File bf = (bfile == null ? null : new File(bfile));
      python_interpreter = project_manager.findInterpreter(bf);
      for (Element pe : IvyXml.children(xml,"PATH")) {
	 IPathSpec ps = project_manager.createPathSpec(pe);
	 project_paths.add(ps);
       }
      for (Element fe : IvyXml.children(xml,"FILE")) {
	 IFileSpec fs = project_manager.createFileSpec(fe);
	 project_files.add(fs);
       }
      pybase_prefs.loadXml(xml);
    }
   project_nature = new PybaseNature(pm,this);
   is_open = false;
   saveProject();
}




void setupDefaults()
{
   python_interpreter = project_manager.findInterpreter(null);
   if (python_interpreter == null)
      PybaseMain.logE("No interpreter found");
   else
      PybaseMain.logD("Found interpreter " + python_interpreter.getName());

   File src = new File(base_directory,"src");
   if (src.isDirectory() || src.mkdir()) {
      File rsrc = new File("/src");
      IPathSpec ps = project_manager.createPathSpec(rsrc,true,true,true);
      project_paths.add(ps);
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return project_name; }
File getBasePath()				{ return base_directory; }
public File getBinary() 			{ return python_interpreter.getExecutable(); }
PybaseVersion getVersion()			{ return python_interpreter.getPythonVersion(); }
public PybaseNature getNature() 		{ return project_nature; }
PybasePreferences getPreferences()		{ return pybase_prefs; }
public boolean isOpen() 			{ return is_open; }
public boolean exists() 			{ return base_directory.exists(); }
public PybaseProject [] getReferencedProjects() { return new PybaseProject[0]; }
public PybaseProject [] getReferencingProjects() { return new PybaseProject[0]; }

Collection<IFileData> getAllFiles()
{
   return new ArrayList<IFileData>(all_files);
}


ISemanticData getSemanticData(String file)
{
   for (IFileData ifd : all_files) {
      if (ifd.getFile().getPath().equals(file)) return getParseData(ifd);
    }

   return null;
}


String getVersionName()
{
   String nm = python_interpreter.getPythonVersion().toString();
   if (nm.startsWith("VERSION_")) nm = nm.substring(8);
   nm = nm.replace("_",".");
   return "python " + nm;
}

String getProjectSourcePath()
{
   String rslt = "";
   for (IPathSpec ps : project_paths) {
      if (rslt.length() > 0) rslt += "|";
      rslt += ps.getOSFile(base_directory);
    }
   return rslt;
}


ISemanticData reparseFile(IFileData fd)
{
   if (fd == null) return null;
   parseFile(fd);
   return parse_data.get(fd);
}



ISemanticData getParseData(IFileData fd)
{
   if (fd == null) return null;
   if (parse_data.get(fd) == null) {
      parseFile(fd);
    }

   return parse_data.get(fd);
}



/********************************************************************************/
/*										*/
/*	Editing methods 							 */
/*										*/
/********************************************************************************/

void editProject(Element pxml)
{
   for (Element oelt : IvyXml.children(pxml,"OPTION")) {
      String k = IvyXml.getAttrString(oelt,"KEY");
      String v = IvyXml.getAttrString(oelt,"VALUE");
      pybase_prefs.setProperty(k,v);
   }

   Set<IPathSpec> done = new HashSet<IPathSpec>();
   boolean havepath = false;
   for (Element pelt : IvyXml.children(pxml,"PATH")) {
      havepath = true;
      File f1 = new File(IvyXml.getAttrString(pelt,"DIRECTORY"));
      try {
	 f1 = f1.getCanonicalFile();
       }
      catch (IOException e) {
	 f1 = f1.getAbsoluteFile();
       }
      boolean fnd = false;
      for (IPathSpec ps : project_paths) {
	 if (done.contains(ps)) continue;
	 File p1 = ps.getOSFile(base_directory);
	 if (f1.equals(p1)) {
	    done.add(ps);
	    fnd = true;
	    // handle changes to ps at this point
	    break;
	  }
       }
      if (!fnd) {
	 String fp1 = f1.getPath();
	 String fp2 = base_directory.getPath();
	 boolean rel = false;
	 if (fp1.startsWith(fp2)) {
	    String fp3 = fp1.substring(fp2.length());
	    StringTokenizer tok = new StringTokenizer(fp3,File.separator);
	    File fd = null;
	    while (tok.hasMoreTokens()) {
	       String nm0 = tok.nextToken();
	       if (fd == null) fd = new File(nm0);
	       else fd = new File(fd,nm0);
	     }
	    f1 = fd;
	    rel = true;
	  }
	 boolean usr = IvyXml.getAttrBool(pelt,"USER");
	 IPathSpec ps = project_manager.createPathSpec(f1,usr,true,rel);
	 done.add(ps);
	 project_paths.add(ps);
       }
    }

   if (havepath) {
      for (Iterator<IPathSpec> it = project_paths.iterator(); it.hasNext(); ) {
	 IPathSpec ps = it.next();
	 if (!done.contains(ps)) it.remove();
       }
    }

   project_nature.rebuildPath();

   saveProject();
}




/********************************************************************************/
/*										*/
/*	Create new package							*/
/*										*/
/********************************************************************************/

void createPackage(String name,boolean force,IvyXmlWriter xw)
{
   File dir = null;
   String [] comps = name.split("\\.");
   for (IPathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getOSFile(base_directory);
      if (!f.exists()) continue;
      for (int i = 0; i < comps.length-1; ++i) {
	 f = new File(f,comps[i]);
       }
      if (f.exists()) {
	 dir = new File(f,comps[comps.length-1]);
	 break;
       }
    }

   if (dir != null && xw != null) {
      xw.begin("PACKAGE");
      xw.field("NAME",name);
      xw.field("PATH",dir.getAbsolutePath());
      xw.end("PACKAGE");
    }
}


/********************************************************************************/
/*										*/
/*	Create new package							*/
/*										*/
/********************************************************************************/

void findPackage(String name,IvyXmlWriter xw)
{
   File dir = null;
   String [] comps = name.split("\\.");
   for (IPathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getOSFile(base_directory);
      if (!f.exists()) continue;
      for (int i = 0; i < comps.length; ++i) {
	 f = new File(f,comps[i]);
       }
      if (f.exists()) {
	 dir = new File(f,comps[comps.length-1]);
	 break;
       }
    }

   if (dir != null && xw != null) {
      xw.begin("PACKAGE");
      xw.field("NAME",name);
      xw.field("PATH",dir.getAbsolutePath());
      xw.end("PACKAGE");
    }
}



/********************************************************************************/
/*										*/
/*	Create new module							*/
/*										*/
/********************************************************************************/

void createModule(String name,String cnts,IvyXmlWriter xw) throws PybaseException
{
   File fil = findModuleFile(name);

   try {
      FileWriter fw = new FileWriter(fil);
      fw.write(cnts);
      fw.close();
    }
   catch (IOException e) {
      throw new PybaseException("Problem writing new module code",e);
    }

   if (xw != null) {
      xw.begin("FILE");
      xw.field("PATH",fil.getAbsolutePath());
      xw.end();
    }

   build(true,false);
}




public File findModuleFile(String name) throws PybaseException
{
   if (name == null) throw new PybaseException("No file name given for package");

   File dir = null;
   String [] comps = name.split("\\.");
   for (IPathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getOSFile(base_directory);
      if (!f.exists()) continue;
      for (int i = 0; i < comps.length-1; ++i) {
	 f = new File(f,comps[i]);
       }
      if (f.exists()) {
	 dir = f;
	 break;
       }
    }

   if (dir == null)
      throw new PybaseException("Can't find package for new module " + name);

   File fil = new File(dir,comps[comps.length-1] + ".py");

   return fil;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void saveProject()
{
   try {
      File f1 = new File(base_directory,".pyproject");
      IvyXmlWriter xw = new IvyXmlWriter(f1);
      outputXml(xw);
      xw.close();
    }
   catch (IOException e) {
      PybaseMain.logE("Problem writing project file",e);
    }
}



void outputXml(IvyXmlWriter xw)
{
   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("BASE",base_directory.getPath());
   if (python_interpreter != null) {
      xw.textElement("EXE",python_interpreter.getExecutable().getPath());
    }
   for (IPathSpec ps : project_paths) {
      ps.outputXml(xw);
    }
   for (IFileSpec fs : project_files) {
      fs.outputXml(xw);
    }
   pybase_prefs.outputXml(xw,false);
   xw.end("PROJECT");
}




void outputProject(boolean files,boolean paths,boolean clss,boolean opts,IvyXmlWriter xw)
{
   if (xw == null) return;

   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("PATH",base_directory.getPath());
   xw.field("WORKSPACE",project_manager.getWorkSpaceDirectory().getPath());
   if (python_interpreter != null) {
      xw.textElement("EXE",python_interpreter.getExecutable().getPath());
    }

   if (paths) {
      for (IPathSpec ps : project_paths) {
	 ps.outputXml(xw);
       }
    }
   if (files) {
      for (IFileSpec fs : project_files) {
	 fs.outputXml(xw);
       }
    }

   if (opts) pybase_prefs.outputXml(xw,true);

   xw.end("PROJECT");
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void open()
{
   if (is_open) return;
   for (IPathSpec ps : project_paths) {
      File dir = ps.getOSFile(base_directory);
      loadFiles(null,dir,false);
    }
   is_open = true;
   saveProject();
}



void build(boolean refresh,boolean reload)
{
   if (!is_open) {
      open();
      return;
    }

   Set<IFileData> oldfiles = null;

   if (refresh) {
      oldfiles = new HashSet<IFileData>(all_files);
      if (reload) {
	 all_files.clear();
	 parse_data.clear();
       }
    }

   for (IPathSpec ps : project_paths) {
      File dir = ps.getOSFile(base_directory);
      loadFiles(null,dir,reload);
    }

   if (oldfiles != null) {
      handleRefresh(oldfiles);
    }
}



private void handleRefresh(Set<IFileData> oldfiles)
{
   IvyXmlWriter xw = pybase_main.beginMessage("RESOURCE");
   int ctr = 0;
   for (IFileData fd : all_files) {
      IFileData old = null;
      for (IFileData ofd : oldfiles) {
	 if (ofd.getFile().equals(fd.getFile())) {
	    old = ofd;
	    break;
	  }
       }
      if (old == null) {
	 outputDelta(xw,"ADDED",fd);
	 ++ctr;
       }
      else if (old.getLastDateLastModified() != fd.getLastDateLastModified()) {
	 oldfiles.remove(old);
	 outputDelta(xw,"CHANGED",fd);
	 ++ctr;
       }
      else {
	 oldfiles.remove(old);
       }
    }
   for (IFileData fd : oldfiles) {
      outputDelta(xw,"REMOVED",fd);
      ++ctr;
    }
   if (ctr > 0) {
      pybase_main.finishMessage(xw);
    }
}



private void outputDelta(IvyXmlWriter xw,String act,IFileData ifd)
{
   xw.begin("DELTA");
   xw.field("KIND",act);
   xw.begin("RESOURCE");
   xw.field("TYPE","FILE");
   xw.field("PROJECT",project_name);
   xw.field("LOCATION",ifd.getFile().getAbsolutePath());
   xw.end("RESOURCE");
   xw.end("DELTA");
}




/********************************************************************************/
/*										*/
/*	Methods to load files							*/
/*										*/
/********************************************************************************/

private void loadFiles(String pfx,File dir,boolean reload)
{
   File [] fls = dir.listFiles(new SourceFilter());

   if (fls != null) {
      for (File f : fls) {
	 if (f.isDirectory()) {
	    String nm = f.getName();
	    String opfx = pfx;
	    if (pfx == null) pfx = nm;
	    else pfx += "." + nm;
	    loadFiles(pfx,f,reload);
	    pfx = opfx;
	  }
	 else {
	    String mnm = f.getName();
	    int idx = mnm.lastIndexOf(".");
	    if (idx >= 0) mnm = mnm.substring(0,idx);
	    if (pfx != null) mnm = pfx + "." + mnm;
	    IFileData fd = PybaseFileManager.getFileManager().getNewFileData(f,mnm,this);
	    ISemanticData isd = parse_data.get(fd);
	    if (reload) {
	       // fd.reload();
	       isd = null;
	     }
	    all_files.add(fd);
	    if (isd == null) {
	       ISemanticData sd = parseFile(fd);
	       if (sd != null) {
		  System.err.println("PYBASE: PARSE YIELDS: " + sd.getRootNode());
		  IvyXmlWriter xw = PybaseMain.getPybaseMain().beginMessage("FILEERROR");
		  xw.field("PROJECT",sd.getProject().getName());
		  xw.field("FILE",fd.getFile().getPath());
		  xw.begin("MESSAGES");
		  for (PybaseMessage m : sd.getMessages()) {
		     try {
			System.err.println("PYBASE: PARSE ERROR: " + m);
			PybaseUtil.outputProblem(m,sd,xw);
		      }
		     catch (Throwable t) {
			PybaseMain.logE("Pybase error message: ",t);
		      }
		   }
		  xw.end("MESSAGES");
		  PybaseMain.getPybaseMain().finishMessage(xw);
		}
	     }
	  }
       }
    }
}



private ISemanticData parseFile(IFileData fd)
{
   PybaseParser pp = new PybaseParser(this,fd);
   ISemanticData sd = pp.parseDocument();
   if (sd != null) parse_data.put(fd,sd);
   else parse_data.remove(fd);

   System.err.println("PYBASE: PARSE " + fd.getFile());
   return sd;
}




private static class SourceFilter implements FileFilter {

   @Override public boolean accept(File path) {
      if (path.isDirectory()) return true;
      String name = path.getName();
      if (name.endsWith(".py") || name.endsWith(".PY")) return true;
      return false;
    }

}	// end of inner class SourceFilter



}	// end of class PybaseProject




/* end of PybaseProject.java */

