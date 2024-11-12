/********************************************************************************/
/*										*/
/*		BedrockProject.java						*/
/*										*/
/*	Project manager for Bubbles - Eclipse interface 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */


/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyPathPattern;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.service.prefs.Preferences;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.filechooser.FileSystemView;



class BedrockProject implements BedrockConstants, IResourceChangeListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;
private boolean projects_inited;
private boolean projects_registered;
private Set<IProject> open_projects;
private boolean projects_setup;
private boolean use_android;

private static boolean		initial_build = false;
private static boolean		initial_refresh = false;

private static Set<String> ignore_projects;

private static boolean		show_events = false;
private static boolean		show_preferences = false;

static {
   ignore_projects = new HashSet<String>();
   ignore_projects.add("RemoteSystemsTempFiles");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockProject(BedrockPlugin bp)
{
   our_plugin = bp;
   projects_inited = false;
   projects_registered = false;
   projects_setup = false;
   open_projects = new HashSet<>();
}



/********************************************************************************/
/*										*/
/*	Starutp methods 							*/
/*										*/
/********************************************************************************/

void initialize()
{
   if (projects_inited) return;

   IvyXmlWriter xw = new IvyXmlWriter();		// force loading
   xw.close();

   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();

   BedrockPlugin.logI("WORKSPACE = " + wr.getName() + " " + wr.getFullPath());

   IProject[] projs = wr.getProjects();
   for (int i = 0; i < projs.length; ++i) {
      if (ignore_projects.contains(projs[i].getName())) continue;
      BedrockPlugin.logI("    PROJECT = " + projs[i].getName());
      String desc = "Project Setup for " + projs[i].getName();
      if (projs[i].isOpen()) {
	 BedrockPlugin.logI("PROJECT OPEN");
	 if (initial_build) {
	    try {
	       BedrockPlugin.logD("BUILD");
	       BedrockProgressMonitor pm = new BedrockProgressMonitor(our_plugin,desc);
	       projs[i].build(IncrementalProjectBuilder.INCREMENTAL_BUILD,pm);
	       pm.finish();
	     }
	    catch (CoreException e) {
	       BedrockPlugin.logE("Problem doing initial build: " + e);
	     }
	  }
	 attachProject(projs[i],false);
       }
      else if (!PlatformUI.isWorkbenchRunning()) {
	 try {
	    if (initial_refresh) {
	       BedrockPlugin.logI("REFRESH");
	       projs[i].refreshLocal(IResource.DEPTH_INFINITE,null);
	     }
	    if (initial_build) {
	       BedrockPlugin.logI("BUILD");
	       BedrockProgressMonitor pm = new BedrockProgressMonitor(our_plugin,desc);
	       projs[i].build(IncrementalProjectBuilder.INCREMENTAL_BUILD,pm);
	       pm.finish();
	     }
	  }
	 catch (CoreException e) {
	    BedrockPlugin.logE("Problem doing initial build: " + e);
	  }
       }
    }

   projects_inited = true;
}



void register()
{
   if (projects_registered) return;
   projects_registered = true;

   JavaCore.addPreProcessingResourceChangedListener(this,
				     IResourceChangeEvent.POST_CHANGE|IResourceChangeEvent.POST_BUILD);
}


void terminate()
{
   for (IProject p : new ArrayList<IProject>(open_projects)) {
      detachProject(p);
    }
}



/********************************************************************************/
/*										*/
/*	Command processing							*/
/*										*/
/********************************************************************************/

void listProjects(IvyXmlWriter xw)
{
   setupProjects();

   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();

   for (IProject ip : open_projects) {
      if (ignore_projects.contains(ip.getName())) continue;
      if (ip.getLocation() == null) continue;
      xw.begin("PROJECT");
      xw.field("NAME",ip.getName());
      xw.field("OPEN",ip.isOpen());
      xw.field("WORKSPACE",wr.getLocation().toOSString());
      boolean isjava = false;
      boolean isandroid = false;
      try {
	 isjava = ip.hasNature(JavaCore.NATURE_ID);
	 isandroid = ip.hasNature("com.android.ide.eclipse.adt.AndroidNature");
       }
      catch (CoreException e) { }
      xw.field("ISJAVA",isjava);
      xw.field("ISANDROID",isandroid);
      try {
	 xw.cdataElement("DESCRIPTION",ip.getDescription().getComment());
       }
      catch (CoreException e) { }
      xw.textElement("BASE",ip.getFullPath().toOSString());
      try {
	 IProject[] rp = ip.getReferencedProjects();
	 for (int j = 0; j < rp.length; ++j) {
	    xw.textElement("REFERENCES",rp[j].getName());
	  }
       }
      catch (Exception e) { }
      IProject[] up = ip.getReferencingProjects();
      for (int j = 0; j < up.length; ++j) {
	 xw.textElement("USEDBY",up[j].getName());
       }
      xw.end("PROJECT");
    }

   xw.begin("VMS");
   IVMInstallType [] vmtypes = JavaRuntime.getVMInstallTypes();
   IVMInstall dflt = JavaRuntime.getDefaultVMInstall();
   for (IVMInstallType vtyp : vmtypes) {
      IVMInstall [] inst = vtyp.getVMInstalls();
      for (IVMInstall vm : inst) {
	 xw.begin("VM");
	 xw.field("NAME",vm.getName());
	 xw.field("TYPE",vtyp.getName());
	 xw.field("LOCATION",vm.getInstallLocation());
	 xw.field("JAVADOC",vm.getJavadocLocation());
	 boolean valid = vtyp.validateInstallLocation(vm.getInstallLocation()).isOK();
	 xw.field("VALID",valid);
	 if (vm == dflt) xw.field("DEFAULT",true);
	 if (vm.getVMArguments() != null) {
	    for (String s : vm.getVMArguments()) {
	       xw.textElement("ARGUMENT",s);
	     }
	  }
	 xw.end("VM");
       }
    }
   xw.end("VMS");
}



void openProject(String name,boolean fil,boolean pat,boolean cls,boolean opt,boolean imps,
		    String bkg,IvyXmlWriter xw)
	throws BedrockException
{
   setupProjects();

   IProject p = findProject(name);
   if (p == null) return;

   boolean fg = attachProject(p,false);

   if (!fg) {
      xw.emptyElement("FAIL");
    }
   else if (bkg != null) {
      ProjectThread pt = new ProjectThread(bkg,p,fil,pat,cls,opt);
      pt.start();
      outputProject(p,false,false,false,false,false,xw);
    }
   else if (xw != null) {
      outputProject(p,fil,pat,cls,opt,imps,xw);
    }
}



void closeProject(String name,IvyXmlWriter _xw) throws BedrockException
{
   detachProject(findProject(name));
}



void listSourceFiles(String name,IvyXmlWriter xw) throws BedrockException
{
   IProject ip = findProject(name);
   if (ip == null) return;

   IJavaProject jp = JavaCore.create(ip);

   addSourceFiles(jp,xw,new JavaSourceFilter());
}



void buildProject(String proj,boolean clean,boolean full,boolean refresh,IvyXmlWriter xw)
	throws BedrockException
{
   IProject ip = findProject(proj);

   handleBuild(ip,clean,full,refresh);

   IMarker [] mrks;
   BuildDoneThread bdt = null;
   try {
      mrks = ip.findMarkers(null,true,IResource.DEPTH_INFINITE);
      Map<Long,IMarker> marset = new HashMap<>();
      for (IMarker m : mrks) {
	 marset.put(m.getId(),m);
       }
      BedrockUtil.outputMarkers(ip,marset.values(),xw);
      bdt = new BuildDoneThread(ip,marset.values());
    }
   catch (CoreException e) {
       throw new BedrockException("Problem finding errors",e);
    }

   BedrockPlugin.logD("Finished build with " + bdt);

   if (bdt != null) {
      bdt.start();
    }
}



private class BuildDoneThread extends Thread {

   private IProject for_project;
   private Collection<IMarker> marker_set;

   BuildDoneThread(IProject p,Collection<IMarker> mrks) {
      for_project = p;
      marker_set = mrks;
    }

   @Override public void run() {
      IvyXmlWriter xw = our_plugin.beginMessage("BUILDDONE");
      xw.field("PROJECT",for_project.getName());
      // need to include all files here, not just those with problems?
      xw.begin("PROBLEMS");
      BedrockUtil.outputMarkers(for_project,marker_set,xw);
      xw.end("PROBLEMS");
      our_plugin.finishMessage(xw);
    }

}	// end of inner class BuildDoneThread



/********************************************************************************/
/*										*/
/*	Thread to compute and output project definitions			*/
/*										*/
/********************************************************************************/

private class ProjectThread extends Thread {

   private IProject for_project;
   private boolean do_files;
   private boolean do_patterns;
   private boolean do_classes;
   private boolean do_options;
   private String return_id;

   ProjectThread(String bkg,IProject p,boolean fil,boolean pat,boolean cls,boolean opt) {
      super("Bedrock_GetProjectInfo");
      return_id = bkg;
      for_project = p;
      do_files = fil;
      do_patterns = pat;
      do_classes = cls;
      do_options = opt;
    }

   @Override public void run() {
      IvyXmlWriter xw = our_plugin.beginMessage("PROJECTDATA");
      xw.field("BACKGROUND",return_id);
      outputProject(for_project,do_files,do_patterns,do_classes,do_options,false,xw);
      our_plugin.finishMessage(xw);
    }

}	// end of inner class ProjectThread




/********************************************************************************/
/*										*/
/*	Project property editing						*/
/*										*/
/********************************************************************************/

void editProject(String proj,boolean lcl,Element xml,IvyXmlWriter xw) throws BedrockException
{
   if (lcl) {
      localEditProject(xml,xw);
      return;
    }

   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();
   IProject ip = wr.getProject(proj);
   IWorkbenchWindow ww = null;

   try {
      IWorkbench wb = PlatformUI.getWorkbench();
      ww = wb.getActiveWorkbenchWindow();
      if (ww == null) {
	 IWorkbenchWindow [] wins = wb.getWorkbenchWindows();
	 if (wins != null && wins.length > 0) ww = wins[0];
       }
      if (ww == null) ww = wb.openWorkbenchWindow(ip);
    }
   catch (Throwable t) {
      BedrockPlugin.logE("BEDROCK: problem finding window: " + t,t);
      return;
    }

   SelProvider sp = new SelProvider(ip,ww.getShell());

   Display.getDefault().asyncExec(new RunPropDialog(sp));
}


void localEditProject(Element pxml,IvyXmlWriter xw) throws BedrockException
{
   String pnm = IvyXml.getAttrString(pxml,"NAME");
   IProject ip = findProject(pnm);
   IJavaProject ijp = JavaCore.create(ip);
   List<IClasspathEntry> ents = new ArrayList<>();
   try {
      for (Element oe : IvyXml.children(pxml,"OPTION")) {
	 String k = IvyXml.getAttrString(oe,"NAME");
	 String v = IvyXml.getAttrString(oe,"VALUE");
	 if (k.startsWith("edu.brown.cs.bubbles.bedrock.")) {
	    String sfx = k.substring(29);
	    QualifiedName qn = new QualifiedName("edu.brown.cs.bubbles.bedrock",sfx);
	    try {
	       ip.setPersistentProperty(qn,v);
	     }
	    catch (CoreException e) {
	       BedrockPlugin.logD("Problem setting property " + qn + ": " + e);
	     }
	  }
	 else ijp.setOption(k,v);
       }

      for (Element xe : IvyXml.children(pxml,"XPREF")) {
	  String q = IvyXml.getAttrString(xe,"NODE");
	  String k = IvyXml.getAttrString(xe,"KEY");
	  String v = IvyXml.getAttrString(xe,"VALUE");
	  IPreferencesService ps = Platform.getPreferencesService();
	  Preferences rn = ps.getRootNode();
	  Preferences qn = rn.node(q);
	  qn.put(k,v);
       }

      for (IClasspathEntry cpe : ijp.getRawClasspath()) ents.add(cpe);
      for (Element pe : IvyXml.children(pxml,"PATH")) {
	 updatePathElement(ents,pe);
       }

      Element ref = IvyXml.getChild(pxml,"REFERENCES");
      if (ref != null) {
	 IProject [] orefs = ip.getReferencedProjects();
	 List<IProject> projs = new ArrayList<IProject>();
	 for (Element re : IvyXml.children(ref,"PROJECT")) {
	    String rpnm = IvyXml.getText(re);
	    IProject rp = findProject(rpnm);
	    if (rp != null) {
	       projs.add(rp);
	       boolean fnd = false;
	       for (IProject orp : orefs) {
		  if (orp == rp) fnd = true;
	       }
	       if (!fnd) {
		  // IPath ppath = rp.getLocation();
		  IPath ppath = rp.getFullPath();
		  IClasspathEntry pent = JavaCore.newProjectEntry(ppath);
		  ents.add(pent);
	       }
	    }
	  }
	 IProject [] parr = projs.toArray(new IProject[projs.size()]);
	 IProjectDescription ipd = ip.getDescription();
	 ipd.setReferencedProjects(parr);
	 ip.setDescription(ipd,null);
       }

      IClasspathEntry [] enta = new IClasspathEntry[ents.size()];
      enta = ents.toArray(enta);
      BedrockProgressMonitor pm = new BedrockProgressMonitor(our_plugin,"Update Paths");
      ijp.setRawClasspath(enta,pm);
      pm.finish();
      ijp.save(null,false);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem editing project",e);
    }
}



private void updatePathElement(List<IClasspathEntry> ents,Element xml)
{
   IClasspathEntry oent = null;
   int id = IvyXml.getAttrInt(xml,"ID",0);
   if (id != 0) {
      for (IClasspathEntry ent : ents) {
	 if (ent.hashCode() == id) {
	    oent = ent;
	    break;
	  }
       }
    }

   if (IvyXml.getAttrBool(xml,"DELETE")) {
      if (oent != null) ents.remove(oent);
      return;
    }
   if (!IvyXml.getAttrBool(xml,"MODIFIED") && !IvyXml.getAttrBool(xml,"NEW")) return;

   String typ = IvyXml.getAttrString(xml,"TYPE");
   switch (typ) {
      case "LIBRARY" :
      case "SOURCE" :
	 BedrockPlugin.logD("START WROK ON ELEMENT");
	 String f = IvyXml.getTextElement(xml,"BINARY");
	 IPath bin = (f == null ? null : Path.fromOSString(f));
	 f = IvyXml.getTextElement(xml,"SOURCE");
	 IPath src = (f == null ? null : Path.fromOSString(f));
	 BedrockPlugin.logD("PATHS " + bin + " " + src);

	 boolean optfg = IvyXml.getAttrBool(xml,"OPTIONAL");
	 boolean export = IvyXml.getAttrBool(xml,"EXPORTED");
	 IAccessRule [] rls = null;
	 URI docu = null;
	 String doc = IvyXml.getTextElement(xml,"JAVADOC");
	 if (doc != null) {
	    try {
	       docu = new URI(doc);
	     }
	    catch (URISyntaxException e) { }
	    if (docu == null) {
	       try {
		  docu = new URI("file://" + doc);
		}
	       catch (URISyntaxException e) { }
	     }
	  }
	 if (oent != null) {
	    rls = oent.getAccessRules();
	  }

	 IClasspathAttribute [] xatts = null;
	 List<IClasspathAttribute> els = new ArrayList<>();
	 if (optfg) els.add(JavaCore.newClasspathAttribute(IClasspathAttribute.OPTIONAL,"true"));
	 if (docu != null) {
	    els.add(JavaCore.newClasspathAttribute(
		       IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,docu.toString()));
	  }
	 if (!els.isEmpty()) {
	    xatts = new IClasspathAttribute[els.size()];
	    xatts = els.toArray(xatts);
	  }
	 List<IPath> exclpaths = new ArrayList<>();
	 List<IPath> inclpaths = new ArrayList<>();
	 for (Element pat : IvyXml.children(xml,"EXCLUDE")) {
	    String f1 = IvyXml.getAttrString(pat,"PATH");
	    if (f1 == null) continue;
	    IPath fp = Path.fromOSString(f1);
	    exclpaths.add(fp);
	  }
	 for (Element pat : IvyXml.children(xml,"INCLUDE")) {
	    String f1 = IvyXml.getAttrString(pat,"PATH");
	    if (f1 == null) continue;
	    IPath fp = Path.fromOSString(f1);
	    inclpaths.add(fp);
	  }
	 IPath [] exclarr = new IPath[exclpaths.size()];
	 exclarr = exclpaths.toArray(exclarr);
	 IPath [] inclarr = new IPath[inclpaths.size()];
	 inclarr = exclpaths.toArray(inclarr);

	 IClasspathEntry nent = null;
	 if (bin != null && !typ.equals("SOURCE")) {
	    nent = JavaCore.newLibraryEntry(bin,src,null,
		  rls,xatts,export);
	  }
	 else {
	    nent = JavaCore.newSourceEntry(src,inclarr,exclarr,
		  null,xatts);
	  }

	 if (IvyXml.getAttrBool(xml,"MODIFIED") && oent != null) {
	    int idx = ents.indexOf(oent);
	    ents.set(idx,nent);
	  }
	 else {
	    ents.add(nent);
	  }
	 break;
    }

}




private static final class SelProvider implements ISelectionProvider, IShellProvider {

   private IStructuredSelection project_selection;
   private Shell use_shell;

   SelProvider(IProject ip,Shell sh) {
      project_selection = new StructuredSelection(ip);
      use_shell = sh;
    }

   @Override public void addSelectionChangedListener(ISelectionChangedListener listener) { }
   @Override public void removeSelectionChangedListener(ISelectionChangedListener listener) { }
   @Override public void setSelection(ISelection selection) { }

   @Override public ISelection getSelection()		{ return project_selection; }
   @Override public Shell getShell()			{ return use_shell; }


}	// end of inner class SelProvider



private static final class RunPropDialog implements Runnable {

   private SelProvider use_provider;

   RunPropDialog(SelProvider sp) {
      use_provider = sp;
    }

   @Override public void run() {
      PropertyDialogAction act = new PropertyDialogAction(use_provider,use_provider);
      try {
	 act.run();
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("BEDROCK: Problem with project property: " + t,t);
       }
    }

}	// end of inner class RunPropDialog




/********************************************************************************/
/*										*/
/*	New project setup							*/
/*										*/
/********************************************************************************/

void handleCreateProject(String pnm,File pdir,String type,Element props,IvyXmlWriter xw)
   throws BedrockException
{
   BedrockProjectCreator pc = new BedrockProjectCreator(pnm,pdir,type,props);

   try {
      if (!pc.setupProject()) return;
      importExistingProject(pnm);
    }
   catch (BedrockException e) {
      throw e;
    }
   catch (Throwable t) {
      throw new BedrockException("Problem creating project",t);
    }

   xw.begin("PROJECT");
   xw.field("NAME",pnm);
   xw.end("PROJECT");
}



/**
 * Adds a project that is already in the workspace folder but is not
 * a member of the workspace yet
 */
void importExistingProject(String name) throws Exception
{
   IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);

   //if it already exists in the workspace don't do anything
   if (p.exists()) return;

   p.create(null);
   p.open(null);
   JavaCore.create(p);
   p.refreshLocal(IResource.DEPTH_INFINITE, null);
}




/********************************************************************************/
/*										*/
/*	Command utility routines						*/
/*										*/
/********************************************************************************/

IProject findProject(String name) throws BedrockException
{
   setupProjects();

   if (name == null || name.isEmpty()) return null;
   if (ignore_projects.contains(name)) return null;

   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();
   IProject ip = wr.getProject(name);

   if (ip == null) throw new BedrockException("Project " + name + " not in workspace");

   return ip;
}



private static class JavaSourceFilter implements FileFilter {

   @Override public boolean accept(File f) {
      return f.getPath().endsWith(".java");
    }

}	// end of subclass JavaSourceFilter




static boolean useProject(String name)
{
   if (ignore_projects.contains(name)) return false;

   return true;
}




/********************************************************************************/
/*										*/
/*	Attach and detach methods						*/
/*										*/
/********************************************************************************/

private boolean attachProject(IProject p,boolean setup)
{
   if (p == null) return false;

   try {
      p.open(null);
      IJavaProject ijp = JavaCore.create(p);
      ijp.open(null);
      if (setup) {
	 SetupDefaults sdf = new SetupDefaults(ijp);
	 sdf.schedule();
	 sdf.join();
       }
//    if (setup) setupDefaults(ijp);
    }
   catch (JavaModelException e) {
      BedrockPlugin.logI("Error resolving project: " + e);
      return false;
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Error opening project: " + e,e);
      return false;
    }
   catch (Throwable e) {
      BedrockPlugin.logE("Error with project attach: " + e,e);
      return false;
    }

   if (!open_projects.contains(p)) {
      open_projects.add(p);
    }

   return true;
}




private void detachProject(IProject p)
{
   if (!open_projects.contains(p)) return;

   open_projects.remove(p);
}



private void setupProjects()
{
   if (!projects_setup) {
      BedrockApplication.getDisplay();		     // wait for setup
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot wr = ws.getRoot();
      IProject[] projs = wr.getProjects();
      for (IProject ip : projs) attachProject(ip,true);
      projects_setup = true;
    }
}



private class SetupDefaults extends WorkbenchJob {

// private IJavaProject for_project;

   SetupDefaults(IJavaProject ijp) {
      super(BedrockApplication.getDisplay(),"setupDefaults");
//    for_project = ijp;
    }

   @Override public IStatus runInUIThread(IProgressMonitor m) {
      try {
	 BedrockApplication.getDisplay();
	 IPreferenceStore ps = DebugUITools.getPreferenceStore();
	 String s = ps.getString("org.eclipse.debug.ui.switch_perspective_on_suspend");
	 if (s == null || s.equals("prompt")) {
	    ps.setValue("org.eclipse.debug.ui.switch_perspective_on_suspend","never");
	  }
	 s = ps.getString("org.eclipse.debug.ui.save_dirty_editors_before_launch");
	 if (s == null || s.equals("prompt")) {
	    ps.setValue("org.eclipse.debug.ui.save_dirty_editors_before_launch","always");
	  }
	 s = ps.getString("org.eclipse.debug.ui.cancel_launch_with_compile_errors");
	 if (s == null || !s.equals("always")) {
	    ps.setValue("org.eclipse.debug.ui.cancel_launch_with_compile_errors","always");
	    String s1 = ps.getString("org.eclipse.debug.ui.cancel_launch_with_compile_errors");
	    BedrockPlugin.logD("PREFSET " + s + " " + s1);
	  }
	 BedrockPlugin.logD("PREFVALUE " + s);
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem seting defaults",t);
       }

      return Status.OK_STATUS;
    }
}


@SuppressWarnings("unused")
private void setupDefaults(IJavaProject ijp)
{
   try {
      BedrockApplication.getDisplay();
      IPreferenceStore ps = DebugUITools.getPreferenceStore();
      String s = ps.getString("org.eclipse.debug.ui.switch_perspective_on_suspend");
      if (s == null || s.equals("prompt")) {
	 ps.setValue("org.eclipse.debug.ui.switch_perspective_on_suspend","never");
       }
      s = ps.getString("org.eclipse.debug.ui.save_dirty_editors_before_launch");
      if (s == null || s.equals("prompt")) {
	 ps.setValue("org.eclipse.debug.ui.save_dirty_editors_before_launch","always");
       }
      s = ps.getString("org.eclipse.debug.ui.cancel_launch_with_compile_errors");
      if (s == null || !s.equals("always")) {
	 ps.setValue("org.eclipse.debug.ui.cancel_launch_with_compile_errors","always");
	 String s1 = ps.getString("org.eclipse.debug.ui.cancel_launch_with_compile_errors");
	 BedrockPlugin.logD("PREFSET " + s + " " + s1);
       }
      BedrockPlugin.logD("PREFVALUE " + s);
    }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem seting defaults",t);
    }
}




/********************************************************************************/
/*										*/
/*	Project Access methods							*/
/*										*/
/********************************************************************************/

Collection<IFile> getAllSourceFiles(String p) throws BedrockException
{
   Collection<IFile> rslt = findSourceFiles(findProject(p),null);

   return rslt;
}



Collection<IProject> getOpenProjects()
{
   Collection<IProject> rslt = new ArrayList<>(open_projects);

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Package management methods						*/
/*										*/
/********************************************************************************/

void createPackage(String proj,String pkg,boolean force,IvyXmlWriter xw)
	throws BedrockException
{
   IProject ip = findProject(proj);
   IJavaProject ijp = JavaCore.create(ip);

   IPackageFragmentRoot ipfr = null;
   try {
      for (IPackageFragmentRoot pfr : ijp.getAllPackageFragmentRoots()) {
	 try {
	    if (!pfr.isExternal() && !pfr.isArchive() &&
		   pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
	       ipfr = pfr;
	       break;
	     }
	  }
	 catch (JavaModelException e) { }
       }
    }
   catch (JavaModelException e) {
      throw new BedrockException("Problem finding package roots: " + e,e);
    }

   if (ipfr == null) throw new BedrockException("Can't find source fragment root");

   IPackageFragment ifr = null;
   try {
      ifr = ipfr.createPackageFragment(pkg,force,null);
      ifr.save(null,force);
      ifr.open(null);
    }
   catch (JavaModelException e) {
      throw new BedrockException("Problem creating package: " + e,e);
    }

   xw.begin("PACKAGE");
   xw.field("NAME",ifr.getElementName());
   File f = BedrockUtil.getFileForPath(ifr.getPath(),ip);
   xw.field("PATH",f.getAbsolutePath());
   xw.end("PACKAGE");
}




void findPackage(String proj,String pkg,IvyXmlWriter xw)
	throws BedrockException
{
   IProject ip = findProject(proj);
   IPackageFragment ipf = findPackageFragment(proj,pkg);

   if (ipf == null) return;

   File f = BedrockUtil.getFileForPath(ipf.getPath(),ip);

   xw.begin("PACKAGE");
   xw.field("NAME",ipf.getElementName());
   xw.field("PATH",f.getAbsolutePath());
   BedrockUtil.outputJavaElement(ipf,xw);
   xw.end("PACKAGE");
}




IPackageFragment findPackageFragment(String proj,String pkg)
    throws BedrockException
{
   IProject ip = findProject(proj);
   IJavaProject ijp = JavaCore.create(ip);
   if (ijp == null) return null;

   try {
      for (IPackageFragmentRoot pfr : ijp.getAllPackageFragmentRoots()) {
	 try {
	    if (!pfr.isExternal() && !pfr.isArchive() &&
		   pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
	       IPackageFragment ipf = pfr.getPackageFragment(pkg);
	       if (ipf != null && ipf.isOpen()) {
		  File f = BedrockUtil.getFileForPath(ipf.getPath(),ip);
		  if (f.exists()) return ipf;
		  BedrockPlugin.logE("Fragment path doesn't exist: " + f);
		}
	     }
	  }
	 catch (JavaModelException e) { }
       }
    }
   catch (JavaModelException e) {
      e.printStackTrace();
      throw new BedrockException("Problem finding package roots: " + e,e);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Preference methods							*/
/*										*/
/********************************************************************************/

void handlePreferences(String proj,IvyXmlWriter xw)
{
   xw.begin("PREFERENCES");

   Element xml = our_plugin.getLanguageData();
   Element eopts = IvyXml.getChild(xml,"OPTIONS");
   for (Element eopt : IvyXml.children(eopts,"OPTION")) {
      String k = IvyXml.getAttrString(eopt,"NAME");
      String v = IvyXml.getAttrString(eopt,"VALUE");
      xw.begin("PREF");
      xw.field("NAME",k);
      xw.field("VALUE",v);
      xw.field("OPTS",true);
      xw.end("PREF");
    }

   Map<?,?> opts;
   if (proj == null) {
      opts = JavaCore.getOptions();
    }
   else {
      try {
	 IProject ip = findProject(proj);
	 IJavaProject ijp = JavaCore.create(ip);
	 opts = ijp.getOptions(true);
       }
      catch (BedrockException e) {
	 opts = JavaCore.getOptions();
       }
    }

   for (Map.Entry<?,?> ent : opts.entrySet()) {
      String key = (String) ent.getKey();
      String val = (String) ent.getValue();
      xw.begin("PREF");
      xw.field("NAME",key);
      xw.field("VALUE",val);
      xw.field("OPTS",true);
      xw.end("PREF");
    }

   // handle special preferences
   //  let android depend on files in project, not whether it is available in Eclipse
// try {
//    Bundle b = Platform.getBundle("com.android.ide.eclipse.adt");
//    if (b != null) {
//	 xw.begin("PREF");
//	 xw.field("NAME","bedrock.useAndroid");
//	 xw.field("VALUE",true);
//	 xw.field("OPTS",true);
//	 xw.end("PREF");
//	 use_android = true;
//     }
//  }
// catch (Throwable t) { }

   if (show_preferences) {
      try {
	 IPreferencesService ips = Platform.getPreferencesService();
	 IEclipsePreferences iep = ips.getRootNode();
	 iep.accept(new PreferenceLister());
       }
      catch (Throwable t) {
	 BedrockPlugin.logD("Problem listing preferences: " + t);
       }
    }

   xw.end("PREFERENCES");
}




void handleSetPreferences(String proj,Element xml,IvyXmlWriter xw)
{
   if (proj == null) {
      for (IProject ip : open_projects) {
	 setProjectPreferences(ip,xml);
       }
      setProjectPreferences(null,xml);
    }
   else {
      try {
	 IProject ip = findProject(proj);
	 setProjectPreferences(ip,xml);
       }
      catch (BedrockException e) { }
    }
}



private boolean setProjectPreferences(IProject ip,Element xml)
{
   Map<String,String> opts;
   IJavaProject ijp = null;

   if (ip != null) {
      ijp = JavaCore.create(ip);
      if (ijp == null) return false;
      opts = ijp.getOptions(false);
    }
   else opts = JavaCore.getOptions();

   for (Element opt : IvyXml.children(xml,"OPTION")) {
      String nm = IvyXml.getAttrString(opt,"NAME");
      String vl = IvyXml.getAttrString(opt,"VALUE");
      opts.put(nm,vl);
    }

   if (ijp != null) {
      ijp.setOptions(opts);
    }
   else {
      Hashtable<String,String> nopts = new Hashtable<>(opts);
      JavaCore.setOptions(nopts);
    }

   return true;
}


private static class PreferenceLister implements IPreferenceNodeVisitor {

   @Override public boolean visit(IEclipsePreferences node) {
      BedrockPlugin.logD("PREF " + node.name());
      try {
	 for (String k : node.keys()) {
	    BedrockPlugin.logD("PREFV " + k + " " + node.get(k,""));
	  }
       }
      catch (Throwable t) { }
      return true;
    }

}



/********************************************************************************/
/*										*/
/*	Project search methods							*/
/*										*/
/********************************************************************************/

Collection<IFile> findSourceFiles(IResource ir,Collection<IFile> rslt)
{
   if (rslt == null) rslt = new HashSet<>();

   try {
      if (ir instanceof IFile) {
	 IFile ifl = (IFile) ir;
	 if (!ifl.isHidden() && !ifl.isDerived() && !ifl.isLinked()) {
	    String nm = ifl.getName();
	    int idx = nm.lastIndexOf(".");
	    if (idx > 0) {
	       String tail = nm.substring(idx);
	       if (tail.equalsIgnoreCase(".java")) {
		  rslt.add(ifl);
		}
	     }
	  }
       }
      else if (ir instanceof IContainer) {
	 IContainer ic = (IContainer) ir;
	 IResource[] mems = ic.members();
	 for (int i = 0; i < mems.length; ++i) {
	    findSourceFiles(mems[i],rslt);
	  }
       }
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem getting source files: " + e);
    }

   return rslt;
}



Collection<File> findFilteredFiles(File basefile,FileFilter ff,Collection<File> rslt)
{
   if (rslt == null) rslt = new HashSet<>();
   if (basefile == null) return rslt;
   
   FileSystemView fsv = FileSystemView.getFileSystemView();

   if (basefile.isHidden() || fsv.isLink(basefile)) return rslt;
   
   if (basefile.isFile()) {
      if (ff == null || ff.accept(basefile)) {
         rslt.add(basefile);
       }
    }
   else if (basefile.isDirectory()) {
      for (File f : basefile.listFiles()) {
         findFilteredFiles(f,ff,rslt);
       }
    }

   return rslt;
}


Collection<File> findProjectSourceFiles(IJavaProject jp)
{
   Collection<File> rslt = new HashSet<>();

   IProject ip = jp.getProject();

   try {
      IClasspathEntry [] ents = jp.getResolvedClasspath(true);
      for (IClasspathEntry ent : ents) {
	 if (ent.getEntryKind() != IClasspathEntry.CPE_SOURCE) continue;
	 SourceFileFilter sff = new SourceFileFilter(ent);
	 IPath p = ent.getPath();
         File f1 = BedrockUtil.getFileForPath(p,ip);
	 String sp = p.toOSString();
	 IFolder pf = ip.getFolder(f1.getPath());
	 BedrockPlugin.logD("Lookup source path " + p + " " +
	       sp + " " + pf + " " + f1);
	 findFilteredFiles(f1,sff,rslt);
       }
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem resolving classpath",e);
    }


   return rslt;
}





private static final class SourceFileFilter implements FileFilter {

   private List<IvyPathPattern> include_patterns; 
   private List<IvyPathPattern> exclude_patterns;

   SourceFileFilter(IClasspathEntry ent) {
      exclude_patterns = buildPatternList(ent.getExclusionPatterns());
      include_patterns = buildPatternList(ent.getInclusionPatterns());
    }

   private List<IvyPathPattern> buildPatternList(IPath [] pats) {
      List<IvyPathPattern> rslt = new ArrayList<>();
      for (IPath p : pats) {
	 rslt.add(new IvyPathPattern(p.toOSString()));
       }
      return rslt;
    }

   @Override public boolean accept(File f) {
      if (!f.getPath().endsWith(".java")) return false;
      if (!include_patterns.isEmpty()) {
	 boolean fnd = false;
	 for (IvyPathPattern fpat : include_patterns) {
	    if (fpat.doesMatch(f)) {
	       fnd = true;
	       break;
	     }
	  }
	 if (!fnd) {
            return false;
          }
       }
      for (IvyPathPattern fpat : exclude_patterns) {
	 if (fpat.doesMatch(f)) {
	    return false;
	  }
       }
      return true;
    }

}



ICompilationUnit getCompilationUnit(String proj,String file) throws BedrockException
{
   //TODO: This should find the current working copy, not the underlying unit

   IProject ip = findProjectForFile(proj,file);
   if (ip == null) {
      BedrockPlugin.logD("No project returned for " + file);
      return null;
    }

   IJavaProject ijp = JavaCore.create(ip);
   if (ijp== null)
      BedrockPlugin.logD("Java project not created: " + ip);
   ICompilationUnit icu = checkFilePrefix(ijp,null,file);
   if (icu != null) return icu;
   if (ijp == null) return null;

   String cfile = file;
   try {
      File f1 = new File(file);
      cfile = f1.getCanonicalPath();
    }
   catch (IOException e) { }
   if (cfile != null && cfile.equals(file)) cfile = null;

   try {
      IClasspathEntry[] ents = ijp.getResolvedClasspath(true);
      for (int i = 0; i < ents.length; ++i) {
	 if (ents[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
	    IPath p = ents[i].getPath();
	    File f = BedrockUtil.getFileForPath(p,ip);
	    if (!f.exists()) continue;
	    File f1 = IvyFile.getCanonical(f);
	    icu = checkFilePrefix(ijp,f.getAbsolutePath(),file);
	    if (icu != null) return icu;
	    if (f1 != null && !f.equals(f1)) {
	       icu = checkFilePrefix(ijp,f1.getAbsolutePath(),file);
	       if (icu != null) return icu;
	     }
	    if (cfile != null) {
	       icu = checkFilePrefix(ijp,f.getAbsolutePath(),cfile);
	       if (icu != null) return icu;
	       if (f1 != null && !f.equals(f1)) {
		  icu = checkFilePrefix(ijp,f1.getAbsolutePath(),cfile);
		  if (icu != null) return icu;
		}
	     }
	  }
       }

      BedrockPlugin.logD("Can't find resolved entry for " + file + " " + ijp.getPath());
    }
   catch (JavaModelException e) {
      BedrockPlugin.logE("Problem getting compilation unit: " + e);
    }

   return null;
}






private ICompilationUnit checkFilePrefix(IJavaProject ijp,String pfx,String file)
{
   if (ijp == null) return null;

   if (pfx != null) {
      if (!file.startsWith(pfx)) return null;
      int ln = pfx.length();
      if (file.charAt(ln) == File.separatorChar || file.charAt(ln) == '/') ++ln;
      file = file.substring(ln);
    }

   try {
      IPath fp = new Path(file);
      IJavaElement je = ijp.findElement(fp);
      if (je != null && je instanceof ICompilationUnit) {
	 return (ICompilationUnit) je;
       }
      BedrockPlugin.logD("File not found: " + file + " " + fp + " " + ijp);
    }
   catch (JavaModelException e) { }

   file = file.replace('\\','/');

   try {
      IPath fp = new Path(file);
      IJavaElement je = ijp.findElement(fp);
      if (je != null && je instanceof ICompilationUnit) {
	 return (ICompilationUnit) je;
       }
    }
   catch (JavaModelException e) { }

   return null;
}




IFile getProjectFile(String proj,String file) throws BedrockException
{
   if (proj == null) {
      for (IProject ip : open_projects) {
	 IFile ifl = findProjectFile(ip,file,null);
	 if (ifl != null) return ifl;
       }
      return null;
    }

   IProject ip = findProject(proj);

   return findProjectFile(ip,file,null);
}



IProject findProjectForFile(String proj,String file) throws BedrockException
{
   if (proj == null && file != null) {
      for (IProject ip : open_projects) {
	 IFile ifl = findProjectFile(ip,file,null);
	 if (ifl != null) return ip;
       }
      BedrockPlugin.logE("No project found for file " + file);
      return null;
    }

   return findProject(proj);
}



private IFile findProjectFile(IResource ir,String name,String fname)
{
   if (fname == null) {
      int idx = name.lastIndexOf(File.separator);
      if (idx > 0) {
	 fname = name.substring(idx+1);
       }
    }

   try {
      if (ir instanceof IFile) {
	 IFile ifl = (IFile) ir;
	 File f = ifl.getLocation().toFile();
	 File f1 = f;
	 if (fname != null && !f1.getName().equals(fname)) return null;
	 if (f.getAbsolutePath().equals(name) || f.getPath().equals(name)) return ifl;
	 f1 = IvyFile.getCanonical(f);
	 if (f.getAbsolutePath().equals(name) || f1.getAbsolutePath().equals(name)) return ifl;
       }
      else if (ir instanceof IContainer) {
	 IContainer ic = (IContainer) ir;
	 IResource[] mems = ic.members();
	 for (int i = 0; i < mems.length; ++i) {
	    IFile ifl = findProjectFile(mems[i],name,fname);
	    if (ifl != null) return ifl;
	  }
       }
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem getting source files: " + e);
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Project dumping methods 						*/
/*										*/
/********************************************************************************/

private void outputProject(IProject p,boolean fil,boolean pat,boolean cls,boolean opt,boolean imps,
			      IvyXmlWriter xw)
{
   if (p == null || p.getLocation() == null) return;

   xw.begin("PROJECT");
   xw.field("NAME",p.getName());
   xw.field("PATH",p.getLocation().toOSString());
   xw.field("WORKSPACE",p.getWorkspace().getRoot().getLocation().toOSString());
// xw.field("BEDROCKDIR",p.getWorkingLocation(BEDROCK_PLUGIN).toOSString());
   try {
      if (p.hasNature("org.eclipse.jdt.core.javanature"))
	 xw.field("ISJAVA",true);
      if (p.hasNature("com.android.ide.eclipse.adt.AndroidNature"))
	 xw.field("ISANDROID",true);
    }
   catch (CoreException e) { }

   IJavaProject jp = JavaCore.create(p);
   if (jp != null && pat) {
      xw.begin("CLASSPATH");
      addClassPaths(jp,xw,null,false);
      xw.end("CLASSPATH");
      xw.begin("CLASSPATH_VARS");
      for (String nm : JavaCore.getClasspathVariableNames()) {
	 IPath vpath = JavaCore.getClasspathVariable(nm);
	 if (vpath != null) {
	    xw.begin("VARIABLE");
	    xw.field("NAME",nm);
	    xw.field("PATH",vpath.toOSString());
	    xw.field("FILE",vpath.toFile().getAbsolutePath());
	    xw.end("VARIABLE");
	  }
       }
      xw.end("CLASSPATH_VARS");
      xw.begin("RAWPATH");
      try {
	 IClasspathEntry [] ents = jp.getRawClasspath();
	 for (IClasspathEntry ent : ents) {
	    addPath(xw,jp,ent,false);
	  }
       }
      catch (JavaModelException e) { }
      xw.end("RAWPATH");
    }

   if (fil) {
      xw.begin("FILES");
      addSourceFiles(jp,xw,null);
      xw.end("FILES");
    }

   if (jp != null && cls) {
      xw.begin("CLASSES");
      addClasses(jp,xw);
      xw.end("CLASSES");
    }

   try {
      IProject[] rp = p.getReferencedProjects();
      IProject[] up = p.getReferencingProjects();
      for (int j = 0; j < rp.length; ++j) {
	 xw.textElement("REFERENCES",rp[j].getName());
       }
      for (int j = 0; j < up.length; ++j) {
	 xw.textElement("USEDBY",up[j].getName());
       }
    }
   catch (Exception e) { }

   if (opt && jp != null) {
      Map<?,?> opts = jp.getOptions(false);
      for (Map.Entry<?,?> ent : opts.entrySet()) {
	 xw.begin("OPTION");
	 xw.field("NAME",ent.getKey().toString());
	 xw.field("VALUE",ent.getValue().toString());
	 xw.end("OPTION");
       }
      Map<?,?> allopts = jp.getOptions(true);
      for (Map.Entry<?,?> ent : allopts.entrySet()) {
	 String knm = (String) ent.getKey();
	 if (opts.containsKey(knm)) continue;
	 if (knm.startsWith("org.eclipse.jdt.core.formatter")) continue;
	 xw.begin("OPTION");
	 xw.field("DEFAULT",true);
	 xw.field("NAME",ent.getKey().toString());
	 xw.field("VALUE",ent.getValue().toString());
	 xw.end("OPTION");
       }
      try {
	 Map<?,?> pm = p.getPersistentProperties();
	 for (Map.Entry<?,?> ent : pm.entrySet()) {
	    QualifiedName qn = (QualifiedName) ent.getKey();
	    xw.begin("PROPERTY");
	    xw.field("QUAL",qn.getQualifier());
	    xw.field("NAME",qn.getLocalName());
	    xw.field("VALUE",ent.getValue().toString());
	    xw.end("PROPERTY");
	  }
       }
      catch (CoreException e) { }
    }

   if (imps && jp != null) {
      try {
	 for (IPackageFragment ipf : jp.getPackageFragments()) {
	    outputImports(xw,ipf);
	  }
       }
      catch (JavaModelException e) {}
    }

   xw.end("PROJECT");
}





private void addClassPaths(IJavaProject jp,IvyXmlWriter xw,Set<IProject> done,boolean refd)
{
   if (done == null) done = new HashSet<IProject>();
   done.add(jp.getProject());
   BedrockPlugin.logD("Getting class path for " + jp.getProject().getName());

   try {
      IClasspathEntry[] ents = jp.getResolvedClasspath(true);
      for (IClasspathEntry ent : ents) {
	 addPath(xw,jp,ent,refd);
       }
      IPath op = jp.getOutputLocation();
      if (op != null) {
	 xw.begin("PATH");
	 xw.field("TYPE","BINARY");
	 xw.field("TARGET",true);
	 File f = BedrockUtil.getFileForPath(op,jp.getProject());
	 if (f.exists()) xw.textElement("BINARY",f.getAbsolutePath());
	 xw.end("PATH");
       }
      for (IProject rp : jp.getProject().getReferencedProjects()) {
	 if (done.contains(rp)) continue;
	 IJavaProject jrp = JavaCore.create(rp);
	 addClassPaths(jrp,xw,done,true);
       }
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem resolving classpath",e);
    }
}




private void addPath(IvyXmlWriter xw,IJavaProject jp,IClasspathEntry ent,boolean refd)
{
   IPath p = ent.getPath();
   IPath op = ent.getOutputLocation();
   IPath sp = ent.getSourceAttachmentPath();
   IProject ip = jp.getProject();

// BedrockPlugin.logD("ADD PATH " + ent + " " + p + " " + op + " " + sp);

   String jdp = null;
   boolean opt = false;
   IClasspathAttribute [] atts = ent.getExtraAttributes();
   for (IClasspathAttribute att : atts) {
      if (att.getName().equals(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME))
	 jdp = att.getValue();
      else if (att.getName().equals(IClasspathAttribute.OPTIONAL)) {
	 String v = att.getValue();
	 if (v.equals("true")) opt = true;
       }
    }

   if (p == null && op == null) return;
   File f1 = null;
   if (p != null) {
      f1 = BedrockUtil.getFileForPath(p,ip);
      if (!f1.exists()) {
	 BedrockPlugin.logD("Path file " + p + " not found as " + f1);
	 // f1 = null;
       }
    }
   File f2 = null;
   if (op != null) {
      f2 = BedrockUtil.getFileForPath(op,ip);
      if (!f2.exists()) {
	 BedrockPlugin.logD("Path file " + op + " not found");
	 f2 = null;
       }
    }
   File f3 = null;
   if (sp != null) {
      f3 = BedrockUtil.getFileForPath(sp,ip);
      if (!f3.exists()) {
	 BedrockPlugin.logD("Path file " + sp + " not found");
	 f3 = null;
       }
    }

   if (ent.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
      try {
	 for (IPackageFragmentRoot pfr : jp.findPackageFragmentRoots(ent)) {
	    String nm = pfr.toString();
	    if (nm.startsWith("<module:")) {
	       IClasspathEntry xent = pfr.getResolvedClasspathEntry();
	       IPath entp = xent.getPath();
	       File entf = BedrockUtil.getFileForPath(entp,ip);
	       File entf1 = entf.getParentFile();
	       File entf2 = entf1.getParentFile();
	       File entf3 = new File(entf2,"jmods");
	       int idx = nm.indexOf(">");
	       String nm1 = nm.substring(8,idx);
	       File entf4 = new File(entf3,nm1 + ".jmod");
	       // BedrockPlugin.logD("Add Module path " + entf4);
	       xw.begin("PATH");
	       if (refd) xw.field("INDIRECT",true);
	       xw.field("TYPE","BINARY");
	       xw.field("MODULE",true);
	       xw.field("BINARY",entf4.getAbsolutePath());
	       xw.field("SYSTEM",true);
	       if (f3 != null) xw.textElement("SOURCE",f3.getAbsolutePath());
	       if (jdp != null) xw.textElement("JAVADOC",jdp);
	       xw.end("PATH");
	     }
	  }
	 return;
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logD("Problem getting roots: " + e);
       }
    }

   if (f1 == null && f2 == null) {
      BedrockPlugin.logW("Ignore PATH " + p + " " + op + " " + sp);
      return;
    }

   // references to nested projects are handled in addClassPaths
   if (ent.getEntryKind() == IClasspathEntry.CPE_PROJECT) return;

   xw.begin("PATH");
   xw.field("ID",ent.hashCode());
   if (refd) xw.field("INDIRECT","TRUE");

   switch (ent.getEntryKind()) {
      case IClasspathEntry.CPE_SOURCE :
	 xw.field("TYPE","SOURCE");
	 xw.field("SUBDIRS",true);
	 f3 = f1;
	 f1 = null;
	 break;
      case IClasspathEntry.CPE_PROJECT :
	 xw.field("TYPE","BINARY");
	 break;
      case IClasspathEntry.CPE_LIBRARY :
	 xw.field("TYPE","LIBRARY");
	 break;
    }
   if (ent.isExported()) xw.field("EXPORTED",true);
   if (opt) xw.field("OPTIONAL",true);

   if (f1 != null) xw.textElement("BINARY",f1.getAbsolutePath());
   if (f2 != null) xw.textElement("OUTPUT",f2.getAbsolutePath());
   if (f3 != null) xw.textElement("SOURCE",f3.getAbsolutePath());
   if (jdp != null) xw.textElement("JAVADOC",jdp);

   for (IPath path : ent.getExclusionPatterns()) {
      xw.begin("EXCLUDE");
      xw.field("PATH",path.toOSString());
      xw.end("EXCLUDE");
    }
   for (IPath path : ent.getInclusionPatterns()) {
      xw.begin("INCLUDE");
      xw.field("PATH",path.toOSString());
      xw.end("INCLUDE");
    }
   IAccessRule [] rls = ent.getAccessRules();
   for (IAccessRule ar : rls) {
      xw.begin("ACCESS");
      xw.field("KIND",ar.getKind());
      xw.field("PATTERN",ar.getPattern().toString());
      xw.field("IGNOREIFBETTER",ar.ignoreIfBetter());
      xw.end("ACCESS");
    }

   xw.end("PATH");
}



private void addSourceFiles(IJavaProject jp,IvyXmlWriter xw,FileFilter ff)
{
   Collection<IFile> fls = new HashSet<>();
   Collection<File> ffls = new HashSet<>();
   IProject ir = jp.getProject();
   
   if (ff != null) {
      fls = findSourceFiles(ir,fls);
      for (IFile ifl : fls) {
         IPath ip = ifl.getLocation();
         File f = ip.toFile();
         ffls.add(f);
       }
    }
   else {
      ffls = findProjectSourceFiles(jp);
      for (File f : ffls) {
         IFile ifl = ir.getFile(f.getPath());
         fls.add(ifl);
       }
    }

   BedrockEditManager bem = our_plugin.getEditManager();

   for (File f : ffls) {
      String cid = "unknown";
      if (f.getName().endsWith(".java")) cid = "javaSource";
      else if (f.getName().endsWith(".class")) cid = "javaClass";
      if (ff != null && !ff.accept(f)) continue;
      if (f.exists()) {
	 f = IvyFile.getCanonical(f);
	 xw.begin("FILE");
	 if (cid.endsWith("javaSource")) xw.field("SOURCE",true);
	 else if (cid.endsWith("javaClass")) xw.field("BINARY",true);
	 else xw.field("TYPENAME",cid);
	 xw.field("NAME",f.getName());
	 if (!f.canWrite()) xw.field("READONLY",true);
// 	 if (!ifl.isSynchronized(IResource.DEPTH_ONE)) xw.field("SYNC",false);
// 	 xw.field("PROJPATH",ipr.toOSString());
	 String path = f.getAbsolutePath();
	 xw.field("PATH",path);
	 if (bem != null) xw.field("ISOPEN",bem.isFileOpen(path));
	 xw.text(f.getPath());
	 xw.end("FILE");
         
       }
    }
}




private void addClasses(IJavaProject jp,IvyXmlWriter xw)
{
   try {
      IClasspathEntry[] ents = jp.getResolvedClasspath(true);
      for (int k = 0; k < ents.length; ++k) {
	 if (ents[k].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
	    IPackageFragmentRoot[] rts = jp.findPackageFragmentRoots(ents[k]);
	    for (int l = 0; l < rts.length; ++l) {
	       IJavaElement[] elts = rts[l].getChildren();
	       for (int m = 0; m < elts.length; ++m) {
		  if (elts[m] instanceof IPackageFragment) {
		     IPackageFragment frag = (IPackageFragment) elts[m];
		     xw.textElement("PACKAGE",frag.getElementName());
		     for (ICompilationUnit icu : frag.getCompilationUnits()) {
			for (IType typ : icu.getTypes()) {
			   outputType(typ,jp,xw);
			 }
		      }
		   }
		}
	     }
	  }
       }
    }
   catch (JavaModelException e) {
      BedrockPlugin.logE("Problem getting class list: " + e);
    }
}



private void outputType(IType typ,IJavaProject jp,IvyXmlWriter xw) throws JavaModelException
{
   xw.begin("TYPE");
   xw.field("NAME",typ.getFullyQualifiedName());
   IPath ip = typ.getPath();
   File f = BedrockUtil.getFileForPath(ip,jp.getProject());
   if (f.exists()) xw.field("SOURCE",f.getAbsolutePath());
   File bf = findBinaryFor(jp,typ);
   if (bf != null) xw.field("BINARY",bf.getAbsolutePath());
   xw.end("TYPE");

   for (IType ntyp : typ.getTypes()) {
      outputType(ntyp,jp,xw);
    }
}



private void outputImports(IvyXmlWriter xw,IPackageFragment ipf)
{
   try {
      boolean havepkg = false;
      for (ICompilationUnit icu : ipf.getCompilationUnits()) {
	 for (IImportDeclaration imp : icu.getImports()) {
	    xw.begin("IMPORT");
	    if (imp.isOnDemand()) xw.field("DEMAND",true);
	    if (Modifier.isStatic(imp.getFlags())) xw.field("STATIC",true);
	    if (!havepkg) {
	       xw.field("PACKAGE",ipf.getElementName());
	       havepkg = true;
	     }
	    xw.text(imp.getElementName());
	    xw.end("IMPORT");
	 }
       }
    }
   catch (JavaModelException e) { }
}



private File findBinaryFor(IJavaProject jp,IType typ)
{
   IPath op = jp.readOutputLocation();
   // op might not be correct if there is an output directory associated with
   // the source directory

   String tnm = typ.getFullyQualifiedName();
   tnm = tnm.replace('.',File.separatorChar);
   IPath cp = op.append(tnm + ".class");
   File f= BedrockUtil.getFileForPath(cp,jp.getProject());
   return f;
}



/********************************************************************************/
/*										*/
/*	Build methods								*/
/*										*/
/********************************************************************************/

private void handleBuild(IProject p,boolean clean,boolean full,boolean refresh) throws BedrockException
{
   if (use_android) BedrockApplication.getDisplay();

   try {
      if (refresh) {
	 String desc = "Refreshing project " + p.getName();
	 BedrockProgressMonitor wait = new BuildMonitor(p.getName(),our_plugin,desc);
	 p.refreshLocal(IResource.DEPTH_INFINITE,wait);
	 wait.finish();
       }
      int kind = IncrementalProjectBuilder.INCREMENTAL_BUILD;
      String knm = "";
      if (clean) {
	 kind = IncrementalProjectBuilder.CLEAN_BUILD;
	 knm = "(clean)";
       }
      else if (full) {
	 kind = IncrementalProjectBuilder.FULL_BUILD;
	 knm = "(full)";
       }
      String desc = "Building " + knm + " project " + p.getName();
      BedrockProgressMonitor pm = new BedrockProgressMonitor(our_plugin,desc);
      p.build(kind,pm);
      pm.finish();
    }
   catch (Throwable t) {
      throw new BedrockException("Build error: " + t,t);
    }
}



private class BuildMonitor extends BedrockProgressMonitor {

   private String for_project;

   BuildMonitor(String proj,BedrockPlugin bp,String nm) {
      super(bp,nm);
      for_project = proj;
    }

   @Override public void done() {
      our_plugin.getEditManager().updateFiles(for_project);
      super.done();
    }

}	// end of inner class BuildMonitor




/********************************************************************************/
/*										*/
/*	Event handler for workspace resources					*/
/*										*/
/********************************************************************************/

@Override public void resourceChanged(IResourceChangeEvent evt)
{
   if (show_events) {
      BedrockPlugin.logD("Resource Change: " + evt.getBuildKind() + " " + evt.getType() + " " +
			    evt.getSource() + " " + evt.getResource());
      IvyXmlWriter dxw = new IvyXmlWriter();
      dxw.begin("RESOURCECHANGE");
      IResourceDelta drd = evt.getDelta();
      dumpDelta(0,drd);
      BedrockUtil.outputResource(drd,dxw);
      dxw.end();
      BedrockPlugin.logD("Resource: " + dxw.toString());
    }
   else {
      BedrockPlugin.logD("Resource Change: " + evt.getBuildKind() + " " + evt.getType() + " " +
			    evt.getSource() + " " + evt.getResource());
    }

   if (evt.getType() == IResourceChangeEvent.POST_CHANGE) {
      try {
	 IvyXmlWriter xw = our_plugin.beginMessage("RESOURCE");
	 IResourceDelta rd = evt.getDelta();
	 int ctr = BedrockUtil.outputResource(rd,xw);
	 if (ctr == 0) return;				   // nothing output
	 our_plugin.finishMessage(xw);
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem with resource: " + t);
	 t.printStackTrace();
       }
    }
   else if (evt.getType() == IResourceChangeEvent.POST_BUILD) {
//    try {
//	 IvyXmlWriter xw = our_plugin.beginMessage("BUILDDONE");
//	 IResourceDelta rd = evt.getDelta();
//	 int ctr = BedrockUtil.outputResource(rd,xw);
//	 if (ctr != 0) our_plugin.finishMessage(xw);
//	 else {
//	    checkForProjectOpen(rd);
//	  }
//     }
//    catch (Throwable t) {
//	 BedrockPlugin.logE("Problem with resource: " + t);
//	 t.printStackTrace();
//     }
    }
}




private void dumpDelta(int lvl,IResourceDelta drd)
{
   if (drd == null) return;
   BedrockPlugin.logD("Resource " + lvl + " Delta: " + drd + " " + drd.getFullPath() + " " +
			 drd.getFlags() + " " + drd.getKind() + " " + drd.getResource());
   for (IResourceDelta xrd : drd.getAffectedChildren()) {
      dumpDelta(lvl+1,xrd);
    }
}



// private void checkForProjectOpen(IResourceDelta rd)
// {
// IResource ir = rd.getResource();
// if (rd.getFlags() == IResourceDelta.OPEN) {
//    if (ir != null && ir.getProject() != null && ir.getType() == IResource.PROJECT) {
//	 IvyXmlWriter xw = our_plugin.beginMessage("PROJECTOPEN");
//	 xw.field("PROJECT",ir.getProject().getName());
//	 our_plugin.finishMessage(xw);
//     }
//  }
// else if (ir.getType() == IResource.ROOT) {
//    for (IResourceDelta crd : rd.getAffectedChildren()) {
//	 checkForProjectOpen(crd);
//     }
//  }
// }




/********************************************************************************/
/*										*/
/*	Project editing methods 						*/
/*										*/
/********************************************************************************/

void setProjectClassPath(String proj,Element desc)
{
   // List<IClasspathEntry> class_paths = new ArrayList<>();
}


void setProjectOutputPath(String proj,String path)
{
}






void setProjectDescription(String proj,Element desc)
{
}






}	// end of class BedrockProject




/* end of BedrockProject.java */






































































































































































































































































































































































































































