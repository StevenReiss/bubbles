/********************************************************************************/
/*										*/
/*		NobaseProject.java						*/
/*										*/
/*	description of class							*/
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

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.json.JSONObject;
import org.w3c.dom.Element;

import org.eclipse.wst.jsdt.core.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

class NobaseProject implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected NobaseMain	nobase_main;
protected NobaseProjectManager project_manager;
private String project_name;
protected File base_directory;
protected List<NobasePathSpec> project_paths;
private List<File> project_files;
private boolean is_open;
private NobasePreferences nobase_prefs;
private File		js_runner;
protected NobaseScope	global_scope;
private NobaseResolver	project_resolver;
private File		base_path;
private NobaseModule	module_manager;

private Map<NobaseFile,ISemanticData> parse_data;
private Set<NobaseFile> all_files;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseProject(NobaseMain pm,String name,File base)
{
   nobase_main = pm;
   project_manager = pm.getProjectManager();
   base_directory = base.getAbsoluteFile();
   try {
      base_directory = base_directory.getCanonicalFile();
    }
   catch (IOException e) { }
   if (name == null) name = base.getName();
   project_name = name;
   project_paths = new ArrayList<NobasePathSpec>();
   project_files = new ArrayList<File>();
   nobase_prefs = new NobasePreferences(pm.getSystemPreferences());
   all_files = new HashSet<NobaseFile>();
   parse_data = new HashMap<NobaseFile,ISemanticData>();
   global_scope = new NobaseScope(ScopeType.GLOBAL,null);
   NobaseSymbol undef = new NobaseSymbol(this,null,null,"undefined",true);
   global_scope.define(undef);
   NobaseSymbol ths = new NobaseSymbol(this,null,null,"this",true);
   NobaseValue thisval = NobaseValue.createObject();
   ths.setValue(thisval);
   global_scope.define(ths);

   base_path = null;

   File f = new File(base_directory,".nobase");
   if (!f.exists()) f.mkdir();

   File f1 = new File(base_directory,".jsproject");
   Element xml = IvyXml.loadXmlFromFile(f1);
   if (xml == null) {
      setupDefaults();
    }
   else {
      String bfile = IvyXml.getTextElement(xml,"EXE");
      js_runner = findInterpreter(bfile);
      if (js_runner == null) js_runner = findInterpreter(null);
      for (Element pe : IvyXml.children(xml,"PATH")) {
	 NobasePathSpec ps = project_manager.createPathSpec(pe);
	 project_paths.add(ps);
       }
      for (Element fe : IvyXml.children(xml,"FILE")) {
	 String nm = IvyXml.getTextElement(fe,"NAME");
	 File fs = new File(nm);
	 project_files.add(fs);
       }
      nobase_prefs.loadXml(xml);
    }
   is_open = false;
   saveProject();

   project_resolver = new NobaseResolver(this,global_scope);
   module_manager = new NobaseModule(this);
}




void setupDefaults()
{
   js_runner = findInterpreter(null);
}



protected File findInterpreter(String name)
{
   if (name == null) {
      File f = findInterpreter("js");
      if (f != null) return f;
      f = findInterpreter("rhino");
      if (f != null) return f;
      return null;
    }

   File f1 = nobase_main.getRootDirectory();
   File f1a = new File(f1,"lib");
   File f2 = new File(f1a,"JSFiles");
   base_path = f2;
   File f3 = new File(f2,"checkinterpreter.js");

   List<String> args = new ArrayList<String>();
   args.add(name);
   // args.add(f3.getPath());
   try {
      String checkfile = IvyFile.loadFile(f3);
      IvyExec ex = new IvyExec(args,null,null,IvyExec.READ_OUTPUT|IvyExec.PROVIDE_INPUT);
      OutputStream ost = ex.getOutputStream();
      ost.write(checkfile.getBytes());
      ost.close();
      InputStream ins = ex.getInputStream();
      InputStreamReader isr = new InputStreamReader(ins);
      BufferedReader br = new BufferedReader(isr);
      List<String> lines = new ArrayList<String>();
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 lines.add(ln);
       }
      for (String ln : lines) {
	 StringTokenizer tok = new StringTokenizer(ln);
	 if (tok.countTokens() < 2) {
	    NobaseMain.logI("BAD checkinterpreter output LINE: " + ln);
	    continue;
	  }
	 String nam = tok.nextToken();
	 String typ = tok.nextToken();
	 NobaseValue nval = NobaseValue.createAnyValue();
	 if (typ.equalsIgnoreCase("function")) {
	    nval = NobaseValue.createFunction();
	  }
	 else if (typ.equalsIgnoreCase("object")) {
	    nval = NobaseValue.createObject();
	  }
	 else if (typ.equalsIgnoreCase("string")) {
	    if (tok.hasMoreTokens()) {
	       String cnts = tok.nextToken("\n");
	       if (cnts.length() > 1 && cnts.charAt(0) == ' ') cnts = cnts.substring(1);
	       cnts = URLDecoder.decode(cnts,"UTF-8");
	       nval = NobaseValue.createString(cnts);
	     }
	    else nval = NobaseValue.createString();
	  }
	 else if (typ.equalsIgnoreCase("Array")) {
	    nval = NobaseValue.createArrayValue();
	  }
	 defineName(global_scope,null,nam,nval);

       }
      // check versions, etc. here
      ex.waitFor();
    }
   catch (IOException e) {
      return null;
    }

   // might want to get full executable name here
   return new File(name);
}


protected void defineName(NobaseScope scp,NobaseValue scpval,String nam,NobaseValue val)
{
   int idx = nam.indexOf(".");
   if (idx < 0) {
      String bubblesnm = nam;
      NobaseSymbol sym = new NobaseSymbol(this,null,null,nam,true);
      sym.setValue(val);
      if (val.isFunction()) bubblesnm += "()";
      sym.setBubblesName(bubblesnm);
      if (scp != null) scp.define(sym);
      if (scpval != null) {
	 scpval.addProperty(nam,val);
       }
    }
   else {
      String pfx = nam.substring(0,idx);
      String sfx = nam.substring(idx+1);
      NobaseValue nval = null;
      if (scp != null) {
	 nval = scp.lookupValue(pfx,true);
       }
      if (nval == null && scpval != null) {
	 nval = scpval.getProperty(pfx,true);
       }
      if (nval != null) {
	 defineName(null,nval,sfx,val);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return project_name; }
File getBasePath()				{ return base_directory; }
NobasePreferences getPreferences()		{ return nobase_prefs; }
public boolean isOpen() 			{ return is_open; }
public boolean exists() 			{ return base_directory.exists(); }
public NobaseProject [] getReferencedProjects() { return new NobaseProject[0]; }
public NobaseProject [] getReferencingProjects() { return new NobaseProject[0]; }
NobaseScope getGlobalScope()			{ return global_scope; }

Collection<NobaseFile> getAllFiles()
{
   return new ArrayList<NobaseFile>(all_files);
}

NobaseFile findFile(String path)
{
   if (path == null) return null;
   if (File.separatorChar != '/') path = path.replace(File.separatorChar,'/');

   for (NobaseFile ifd : all_files) {
      if (ifd.getFile().getPath().equals(path)) return ifd;
    }

   return null;
}



ISemanticData getSemanticData(String file)
{
   for (NobaseFile ifd : all_files) {
      if (ifd.getFile().getPath().equals(file)) return getParseData(ifd);
    }

   return null;
}


String getProjectSourcePath()
{
   String rslt = "";
   for (NobasePathSpec ps : project_paths) {
      if (rslt.length() > 0) rslt += "|";
      rslt += ps.getFile().getPath();
    }
   return rslt;
}


ISemanticData reparseFile(NobaseFile fd)
{
   if (fd == null) return null;
   parseFile(fd);
   return parse_data.get(fd);
}



ISemanticData getParseData(NobaseFile fd)
{
   if (fd == null) return null;
   if (parse_data.get(fd) == null) {
      parseFile(fd);
    }

   return parse_data.get(fd);
}



/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

void editProject(Element pxml)
{
   for (Element oelt : IvyXml.children(pxml,"OPTION")) {
      String k = IvyXml.getAttrString(oelt,"KEY");
      String v = IvyXml.getAttrString(oelt,"VALUE");
      nobase_prefs.setProperty(k,v);
    }

   Set<NobasePathSpec> done = new HashSet<NobasePathSpec>();
   Set<NobasePathSpec> dels = new HashSet<NobasePathSpec>();
   for (Element pelt : IvyXml.children(pxml,"PATH")) {
      String dir = IvyXml.getAttrString(pelt,"DIRECTORY");
      if (dir == null) continue;
      File f1 = new File(dir);
      try {
	 f1 = f1.getCanonicalFile();
       }
      catch (IOException e) {
	 f1 = f1.getAbsoluteFile();
       }
      NobasePathSpec oldspec = null;
      for (NobasePathSpec ps : project_paths) {
	 if (done.contains(ps)) continue;
	 File p1 = ps.getFile();
	 if (f1.equals(p1)) {
	    done.add(ps);
	    oldspec = ps;
	    break;
	  }
       }
      if (IvyXml.getAttrBool(pelt,"DELETE")) {
	 if (oldspec != null) dels.add(oldspec);
       }
      else {
	 boolean usr = IvyXml.getAttrBool(pelt,"USER");
	 boolean exc = IvyXml.getAttrBool(pelt,"EXCLUDE");
	 boolean nest = IvyXml.getAttrBool(pelt,"NEST");
	 if (oldspec != null) {
	    oldspec.setProperties(usr,exc,nest);
	  }
	 else {
	    NobasePathSpec ps = project_manager.createPathSpec(f1,usr,exc,nest);
	    done.add(ps);
	    project_paths.add(ps);
	  }
       }
    }

   if (!dels.isEmpty()) {
      project_paths.removeAll(dels);
    }
   // project_nature.rebuildPath();

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
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getFile();
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
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getFile();
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
/*	Module management methods						*/
/*										*/
/********************************************************************************/

void setupModule(NobaseFile file,NobaseScope scp)
{
   NobaseValue modval = NobaseValue.createObject();
   scp.setValue(modval);
   defineModuleSymbol(scp,"module",modval);
   defineModuleSymbol(scp,"id",file.getModuleName());
   defineModuleSymbol(scp,"filename",file.getFile().getPath());
   defineModuleSymbol(scp,"loaded",NobaseValue.createBoolean(true));
   NobaseValue reqval = NobaseValue.createNewFunction();
   reqval.setEvaluator(module_manager.getRequiresEvaluator());
   defineModuleSymbol(scp,"require",reqval);
   NobaseValue expval = module_manager.findExportValue(file);
   defineModuleSymbol(scp,"exports",expval);
   defineModuleSymbol(scp,"__dirname",file.getFile().getParent());
   defineModuleSymbol(scp,"__filename",file.getFile().getPath());
}



private void defineModuleSymbol(NobaseScope scp,String name,String val)
{
   NobaseValue nval = NobaseValue.createString(val);
   defineModuleSymbol(scp,name,nval);
}


private void defineModuleSymbol(NobaseScope scp,String name,NobaseValue nval)
{
   NobaseSymbol nsym = new NobaseSymbol(this,null,null,name,true);
   nsym.setValue(nval);
   nsym.setBubblesName("*." + name);
   scp.define(nsym);
   scp.setProperty(name,nval);
}



void finishModule(NobaseFile file)
{
   module_manager.finishExportValue(file);
}


void createModule(String name,String cnts,IvyXmlWriter xw) throws NobaseException
{
   File fil = findModuleFile(name);

   try {
      FileWriter fw = new FileWriter(fil);
      fw.write(cnts);
      fw.close();
    }
   catch (IOException e) {
      throw new NobaseException("Problem writing new module code",e);
    }

   if (xw != null) {
      xw.begin("FILE");
      xw.field("PATH",fil.getAbsolutePath());
      xw.end();
    }

   build(true,false);
}




public File findModuleFile(String name) throws NobaseException
{
   File dir = null;
   String [] comps = name.split("\\.");
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getFile();
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
      throw new NobaseException("Can't find package for new module " + name);

   File fil = new File(dir,comps[comps.length-1] + ".js");

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
      File f1 = new File(base_directory,".jsproject");
      IvyXmlWriter xw = new IvyXmlWriter(f1);
      outputXml(xw);
      xw.close();
    }
   catch (IOException e) {
      NobaseMain.logE("Problem writing project file",e);
    }
}



void outputXml(IvyXmlWriter xw)
{
   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("BASE",base_directory.getPath());
   for (NobasePathSpec ps : project_paths) {
      ps.outputXml(xw);
    }
   for (File fs : project_files) {
      outputFile(fs,xw);
    }
   nobase_prefs.outputXml(xw);
   xw.end("PROJECT");
}




void outputProject(boolean files,boolean paths,boolean clss,boolean opts,IvyXmlWriter xw)
{
   if (xw == null) return;

   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("PATH",base_directory.getPath());
   xw.field("WORKSPACE",project_manager.getWorkSpaceDirectory().getPath());

   if (paths) {
      for (NobasePathSpec ps : project_paths) {
	 ps.outputXml(xw);
       }
    }
   if (files) {
      for (File fs : project_files) {
	 outputFile(fs,xw);
       }
    }

   if (opts) nobase_prefs.outputXml(xw);

   xw.end("PROJECT");
}



private void outputFile(File fs,IvyXmlWriter xw)
{
   xw.begin("FILE");
   xw.field("NAME",fs.getPath());
   xw.end("FILE");
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void open()
{
   if (is_open) return;
   for (NobasePathSpec ps : project_paths) {
      File dir = ps.getFile();
      findFiles(null,dir,false);
    }
   is_open = true;
   saveProject();
}



protected void findFiles(String pfx,File f,boolean reload)
{
   boolean nest = true;
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser() || ps.isExclude()) {
	 if (ps.match(f)) return;
       }
      if (!ps.isNested()) {
	 if (ps.match(f)) nest = false;
       }
    }

   if (f.isDirectory()) {
      File [] fls = f.listFiles(new SourceFilter());
      String npfx = null;
      if (pfx != null) npfx = pfx + "." + f.getName();
      for (File f1 : fls) {
	 if (!nest && f1.isDirectory()) continue;
	 findFiles(npfx,f1,reload);
       }
      return;
    }

   String mnm = f.getName();
   if (!mnm.endsWith(".js") && !mnm.endsWith(".JS")) return;

   int idx = mnm.lastIndexOf(".");
   if (idx >= 0) mnm = mnm.substring(0,idx);
   if (pfx != null) mnm = pfx + "." + mnm;
   NobaseFile fd = findFile(f,mnm);
   ISemanticData isd = parse_data.get(fd);
   if (reload) {
      if (fd.reload()) {
	 module_manager.clearExportValue(fd);
	 isd = null;
       }
    }
   all_files.add(fd);
   if (isd == null) {
      ISemanticData sd = parseFile(fd);
      if (sd != null) {
	 IvyXmlWriter xw = NobaseMain.getNobaseMain().beginMessage("FILEERROR");
	 xw.field("PROJECT",sd.getProject().getName());
	 xw.field("FILE",fd.getFile().getPath());
	 xw.begin("MESSAGES");
	 for (NobaseMessage m : sd.getMessages()) {
	    try {
	       NobaseMain.logI("PARSE ERROR: " + m);
	       NobaseUtil.outputProblem(m,sd,xw);
	     }
	    catch (Throwable t) {
	       NobaseMain.logE("Nobase error message: ",t);
	     }
	  }
	 xw.end("MESSAGES");
	 NobaseMain.getNobaseMain().finishMessage(xw);
       }
    }
}


NobaseFile findFile(File fnm,String mod)
{
   return nobase_main.getFileManager().getNewFileData(fnm,mod,this);
}



void build(boolean refresh,boolean reload)
{
   if (!is_open) {
      open();
      return;
    }

   Set<NobaseFile> oldfiles = null;

   if (refresh) {
      oldfiles = new HashSet<NobaseFile>(all_files);
      if (reload) {
	 all_files.clear();
	 parse_data.clear();
       }
    }

   for (NobasePathSpec ps : project_paths) {
      File dir = ps.getFile();
      findFiles(null,dir,reload);
    }

   if (oldfiles != null) {
      handleRefresh(oldfiles);
    }
}


void rebuild(NobaseFile nf,boolean lib)
{
   parseFile(nf);
}


void buildIfNeeded(NobaseFile nf)
{
   if (parse_data.get(nf) == null) {
      parseFile(nf);
    }
}





private void handleRefresh(Set<NobaseFile> oldfiles)
{
   IvyXmlWriter xw = nobase_main.beginMessage("RESOURCE");
   int ctr = 0;
   for (NobaseFile fd : all_files) {
      NobaseFile old = null;
      for (NobaseFile ofd : oldfiles) {
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
   for (NobaseFile fd : oldfiles) {
      outputDelta(xw,"REMOVED",fd);
      ++ctr;
    }
   if (ctr > 0) {
      nobase_main.finishMessage(xw);
    }
}



private void outputDelta(IvyXmlWriter xw,String act,NobaseFile ifd)
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
/*	Pattern search								*/
/*										*/
/********************************************************************************/

synchronized void patternSearch(String pat,String typ,boolean defs,boolean refs,boolean sys,IvyXmlWriter xw)
{
   if (typ.equals("METHOD") && !pat.contains("()")) pat = pat + "()";
   
   NobaseSearchInstance search = new NobaseSearchInstance(this);
   ASTVisitor nv = search.getFindSymbolsVisitor(pat,typ);
   for (NobaseFile ifd : all_files) {
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      if (isd != null && isd.getRootNode() != null) isd.getRootNode().accept(nv);
    }

   ASTVisitor av = search.getLocationsVisitor(defs,refs,false,false,false);
   for (NobaseFile ifd : all_files) {
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      if (isd != null && isd.getRootNode() != null) isd.getRootNode().accept(av);
    }

   List<SearchResult> rslt = search.getMatches();
   if (rslt == null) return;
   for (SearchResult mtch : rslt) {
      xw.begin("MATCH");
      xw.field("OFFSET",mtch.getOffset());
      xw.field("LENGTH",mtch.getLength());
      xw.field("STARTOFFSET",mtch.getOffset());
      xw.field("ENDOFFSET",mtch.getOffset() + mtch.getLength());
      xw.field("FILE",mtch.getFile().getFile().getPath());
      NobaseSymbol sym = mtch.getContainer();
      if (sym != null) {
	 sym.outputNameData(mtch.getFile(),xw);
       }
      xw.end("MATCH");
    }
}



/********************************************************************************/
/*										*/
/*	Find all command							*/
/*										*/
/********************************************************************************/

void findAll(String file,int soff,int eoff,boolean defs,boolean refs,
      boolean imps,boolean typ,boolean ronly,boolean wonly,IvyXmlWriter xw)
{
   NobaseSearchInstance search = new NobaseSearchInstance(this);
   ASTVisitor av = search.getFindLocationVisitor(soff,eoff);
   for (NobaseFile ifd : all_files) {
      if (!ifd.getFile().getPath().equals(file)) continue;
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      if (isd != null && isd.getRootNode() != null) isd.getRootNode().accept(av);
    }

   search.outputSearchFor(xw);

   ASTVisitor av1 = search.getLocationsVisitor(defs,refs,imps,ronly,wonly);
   for (NobaseFile ifd : all_files) {
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      if (isd != null && isd.getRootNode() != null) isd.getRootNode().accept(av1);
    }

   List<SearchResult> rslt = search.getMatches();
   if (rslt == null) return;
   for (SearchResult mtch : rslt) {
      xw.begin("MATCH");
      xw.field("OFFSET",mtch.getOffset());
      xw.field("LENGTH",mtch.getLength());
      xw.field("STARTOFFSET",mtch.getOffset());
      xw.field("ENDOFFSET",mtch.getOffset() + mtch.getLength());
      xw.field("FILE",mtch.getFile().getFile().getPath());
      NobaseSymbol sym = mtch.getContainer();
      if (sym == null) sym = mtch.getSymbol();
      if (sym != null) {
	 sym.outputNameData(mtch.getFile(),xw);
       }
      xw.end("MATCH");
    }
}



/********************************************************************************/
/*										*/
/*	Get fully qualified name command					*/
/*										*/
/********************************************************************************/

void getFullyQualifiedName(String file,int spos,int epos,IvyXmlWriter xw)
{
   NobaseSearchInstance search = new NobaseSearchInstance(this);
   ASTVisitor av = search.getFindLocationVisitor(spos,epos);
   for (NobaseFile ifd : all_files) {
      if (!ifd.getFile().getPath().equals(file)) continue;
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      if (isd == null || isd.getRootNode() == null) continue;
      isd.getRootNode().accept(av);
    }

   Set<NobaseSymbol> syms = search.getSymbols();
   if (syms == null) return;
   for (NobaseSymbol sym : syms) {
      sym.outputFullName(xw);
    }
}



/********************************************************************************/
/*										*/
/*	Find text regions command						*/
/*										*/
/********************************************************************************/

synchronized void getTextRegions(String bid,String file,String cls,boolean pfx,boolean statics,
      boolean compunit,boolean imports,boolean pkg,
      boolean topdecls,boolean fields,boolean all,IvyXmlWriter xw)
	throws NobaseException
{
   if (file == null) {
      String mnm = cls;
      while (mnm != null) {
	 try {
	    File f1 = findModuleFile(mnm);
	    if (f1 != null) {
	       file = f1.getAbsolutePath();
	       break;
	     }
	  }
	 catch (Throwable t) { }
	 int idx = mnm.lastIndexOf(".");
	 if (idx < 0) break;
	 mnm = mnm.substring(0,idx);
       }
      if (file == null) throw new NobaseException("File must be given");
    }
   try {
      File f1 = new File(file);
      file = f1.getCanonicalPath();
    }
   catch (IOException e) { }

   NobaseFile rf = findFile(file);

   ISemanticData isd = getSemanticData(file);
   if (isd == null) throw new NobaseException("Can't find file data for " + file);

   NobaseSearchInstance search = new NobaseSearchInstance(this);
   search.setFile(rf);
   search.findTextRegions(isd,pfx,statics,compunit,imports,pkg,topdecls,fields,all,xw);
}



/********************************************************************************/
/*										*/
/*	Get Completions command 						*/
/*										*/
/********************************************************************************/

void getCompletions(String file,int offset,IvyXmlWriter xw)
{
   for (NobaseFile ifd : all_files) {
      if (!ifd.getFile().getPath().equals(file)) continue;
      ISemanticData isd = getParseData(ifd);
      NobaseCompletions nc = new NobaseCompletions(isd);
      nc.findCompletions(offset,xw);
      break;
    }
}



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private ISemanticData parseFile(NobaseFile fd)
{
   NobaseMain.logD("PARSE BEGIN " + fd.getFile());

   IParser pp = nobase_main.getParser();
   ISemanticData sd = pp.parse(this,fd,false);
   if (sd != null) {
      parse_data.put(fd,sd);
      project_resolver.resolveSymbols(sd);
    }
   else parse_data.remove(fd);

   NobaseMain.logD("PARSE END " + fd.getFile());

   return sd;
}




private static class SourceFilter implements FileFilter {

   @Override public boolean accept(File path) {
      if (path.isDirectory()) return true;
      String name = path.getName();
      if (name.endsWith(".js") || name.endsWith(".JS")) return true;
      return false;
    }

}	// end of inner class SourceFilter



/********************************************************************************/
/*										*/
/*	Find library files by name						*/
/*										*/
/********************************************************************************/

NobaseFile findRequiresFile(NobaseFile orig,String fnm)
{
   File tgt = null;
   if (fnm.startsWith("./") || fnm.startsWith("../")) {
      // handle relative path user files
      tgt = new File(orig.getFile().getParent(),fnm.substring(2));
      return getNobaseFile(tgt);
    }
   else if (fnm.startsWith("/")) {
      // handle absolute path user files
      tgt = new File(fnm.replace("/",File.separator));
      return getNobaseFile(tgt);
    }

   // handle core modules
   tgt = new File(base_path,fnm);
   NobaseFile nf = getNobaseFile(tgt);
   if (nf != null) return nf;

   for (File par = orig.getFile().getParentFile(); par != null; par = par.getParentFile()) {
      File nmod = new File(par,"node_modules");
      if (nmod.exists()) {
	 nf = getNodeModuleFile(nmod,fnm);
	 if (nf != null) return nf;
       }
    }

   return null;
}



private NobaseFile getNobaseFile(File tgt)
{
   if (!tgt.exists()) {
      String pth = tgt.getPath();
      for (String s : new String [] { ".js", ".json", "node" } ) {
	 File ntgt = new File(pth + s);
	 if (ntgt.exists()) {
            if (s.equals(".json")) {
               NobaseMain.logI("Reading json file " + ntgt);
             }
	    tgt = ntgt;
	    break;
	  }
       }
      if (!tgt.exists()) return null;
    }

   if (tgt.isDirectory()) {
      // might need to look for package.json, index.js, ...
      return null;
    }
   String mnm = getModuleFromPath(tgt);

   return findFile(tgt,mnm);
}


private NobaseFile getNodeModuleFile(File nmod,String fnm)
{
   File tgt = new File(nmod,fnm);
   if (tgt.exists() && tgt.isDirectory()) {
      String mod = getModuleFromPath(tgt);
      File json = new File(tgt,"package.json");
      if (json.exists()) {
	 try {
	    String pkgtxt = IvyFile.loadFile(json);
	    JSONObject jo = new JSONObject(pkgtxt);
	    String mnm = jo.getString("name");
	    if (jo.has("main")) {
	       String main = jo.getString("main");
	       if (mnm == null) mnm = mod;
	       File src = new File(tgt,main);
	       if (src.exists()) {
		  try {
		     src = src.getCanonicalFile();
		   }
		  catch (IOException e) { }
		  if (src.isDirectory()) {
		     src = new File(src,"index.js");
		     if (!src.exists()) src = new File(src,"index.node");
		     if (!src.exists()) return null;
		   }
		  return findFile(src,mnm);
		}
	     }
	  }
	 catch (IOException e) {
	  }
       }
      // try index.js if json fails
      File f1 = new File(tgt,"index.js");
      if (!f1.exists()) f1 = new File(tgt,"index.node");
      if (f1.exists()) {
	 return findFile(f1,mod);
       }
    }

   return getNobaseFile(tgt);
}


private String getModuleFromPath(File f)
{
   String mnm = f.getName();
   int idx = mnm.lastIndexOf(".");
   if (idx > 0) mnm = mnm.substring(0,idx);
   return mnm;
}


/********************************************************************************/
/*										*/
/*	Handle all names							*/
/*										*/
/********************************************************************************/




}	// end of class NobaseProject




/* end of NobaseProject.java */

