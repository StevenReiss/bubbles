/********************************************************************************/
/*										*/
/*		BedrockRuntime.java						*/
/*										*/
/*	Runtime manager for Bubbles - Eclipse interface 			*/
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


/* SVN: $Id$ */




package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaHotCodeReplaceListener;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.ICompiledExpression;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;



class BedrockRuntime implements BedrockConstants, IDebugEventSetListener,
	ILaunchConfigurationListener, IJavaHotCodeReplaceListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;
private DebugPlugin debug_plugin;

private ConsoleThread		console_thread;
private Map<Integer,ConsoleData> console_map;
private Set<ILaunchConfiguration> working_configs;
private Map<IStackFrame,Map<String,IValue>> outside_variables;

private static Set<IJavaThread> variable_threads;
private static Map<String,CallFormatter> format_map;

private static Map<String,String> prop_map;

static {
   prop_map = new HashMap<String,String>();
   variable_threads = new HashSet<>();

   prop_map.put("PROJECT_ATTR","org.eclipse.jdt.launching.PROJECT_ATTR");
   prop_map.put("MAIN_TYPE","org.eclipse.jdt.launching.MAIN_TYPE");
   prop_map.put("PROGRAM_ARGUMENTS","org.eclipse.jdt.launching.PROGRAM_ARGUMENTS");
   prop_map.put("VM_ARGUMENTS","org.eclipse.jdt.launching.VM_ARGUMENTS");
   prop_map.put("TESTNAME","org.eclipse.jdt.junit.TESTNAME");
   prop_map.put("TEST_KIND","org.eclipse.jdt.junit.TEST_KIND");
   prop_map.put("CONTRACTS","edu.brown.cs.bubbles.bedrock.CONTRACTS");
   prop_map.put("ASSERTIONS","edu.brown.cs.bubbles.bedrock.ASSERTIONS");
   prop_map.put("CONNECT_MAP","org.eclipse.jdt.launching.CONNECT_MAP");
   prop_map.put("STOP_IN_MAIN","org.eclipse.jdt.launching.STOP_IN_MAIN");
   prop_map.put("CAPTURE_IN_FILE","org.eclipse.debug.ui.ATTR_CAPTURE_IN_FILE");
   prop_map.put("WORKING_DIRECTORY","org.eclipse.jdt.launching.WORKING_DIRECTORY");

   format_map = new HashMap<String,CallFormatter>();
   CallFormatter xmlfmt = new CallFormatter("edu.brown.cs.ivy.xml.IvyXml.convertXmlToString",
	 "(Lorg/w3c/dom/Node;)Ljava/lang/String;",null);
   format_map.put("org.apache.xerces.dom.DeferredElementImpl",xmlfmt);
   format_map.put("org.apache.xerces.dom.DeferredElementNSImpl",xmlfmt);
   format_map.put("org.apache.xerces.dom.ElementImpl",xmlfmt);
   format_map.put("org.apache.xerces.dom.ElementNSImpl",xmlfmt);
   format_map.put("com.sun.apache.xerces.internal.dom.DeferredElementImpl",xmlfmt);
   format_map.put("com.sun.apache.xerces.internal.dom.DeferredElementNSImpl",xmlfmt);
   format_map.put("com.sun.org.apache.xerces.internal.dom.DeferredElementImpl",xmlfmt);
   format_map.put("com.sun.org.apache.xerces.internal.dom.DeferredElementNSImpl",xmlfmt);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockRuntime(BedrockPlugin bp)
{
   our_plugin = bp;
   debug_plugin = DebugPlugin.getDefault();
   console_thread = null;
   console_map = new LinkedHashMap<>();
   working_configs = new HashSet<>();
   outside_variables = new WeakHashMap<>();
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

void start()
{
   debug_plugin.addDebugEventListener(this);
   JDIDebugModel.addHotCodeReplaceListener(this);

   ILaunchManager lm = debug_plugin.getLaunchManager();
   lm.addLaunchConfigurationListener(this);
}



/********************************************************************************/
/*										*/
/*	Launch Query command						       */
/*										*/
/********************************************************************************/

void handleLaunchQuery(String proj,String query,boolean option,IvyXmlWriter xw)
   throws BedrockException
{
   if (query.equals("START")) {
       findJavaMainMethods(proj,option,xw);
    }
}



private void findJavaMainMethods(String proj,boolean library,IvyXmlWriter xw)
   throws BedrockException
{
   SearchPattern pat = SearchPattern.createPattern("main(String []) void",
	 IJavaSearchConstants.METHOD,
	 IJavaSearchConstants.DECLARATIONS,
	 SearchPattern.R_EXACT_MATCH);

   IJavaProject ijp = null;
   try {
      IProject ip = our_plugin.getProjectManager().findProject(proj);
      if (ip != null) ijp = JavaCore.create(ip);
    }
   catch (BedrockException e) { }

   int fg = IJavaSearchScope.SOURCES;
   IJavaElement [] pelt;
   if (ijp != null) {
      pelt = new IJavaElement [] { ijp };
    }
   else {
      pelt = BedrockJava.getAllProjects();
    }

   if (library) {
      fg |= IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.APPLICATION_LIBRARIES;
    }
   IJavaSearchScope scp = SearchEngine.createJavaSearchScope(pelt,fg);

   SearchEngine se = new SearchEngine();
   SearchParticipant [] parts = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
   MainHandler fh = new MainHandler(xw,library);
   try {
      se.search(pat,parts,scp,fh,null);
    }
   catch (Throwable e) {
      throw new BedrockException("Problem doing search for main",e);
    }
}



private static class MainHandler extends SearchRequestor {

   private boolean use_library;
   private IvyXmlWriter xml_writer;

   MainHandler(IvyXmlWriter xw,boolean library) {
      use_library = library;
      xml_writer = xw;
    }

   @Override public void acceptSearchMatch(SearchMatch mat) {
      BedrockPlugin.logD("FOUND MAIN METHOD " + mat);
      Object elt = mat.getElement();
      if (elt instanceof IMethod) {
         IMethod im = (IMethod) elt;
         try {
            if (im.isMainMethod()) {
               IResource irc = mat.getResource();
               if (!use_library) {
                  if (irc.getType() != IResource.FILE) {
                     return;
                   }
                }
               IType typ = (IType) im.getParent();
               xml_writer.begin("OPTION");
               xml_writer.field("VALUE",typ.getFullyQualifiedName());
               xml_writer.end("OPTION");
             }
            else {
               BedrockPlugin.logD("Note a main method");
             }
          }
         catch (JavaModelException e) { }
       }
      else {
         BedrockPlugin.logD("Note a method " + elt.getClass());
       }
   }

}	// end of inner class MainHandler



/********************************************************************************/
/*										*/
/*	Get the run configurations for a project				*/
/*										*/
/********************************************************************************/

void getRunConfigurations(IvyXmlWriter xw) throws BedrockException
{
   ILaunchManager lm = debug_plugin.getLaunchManager();

   try {
      ILaunchConfiguration [] cnfg = lm.getLaunchConfigurations();

      for (int i = 0; i < cnfg.length; ++i) {
	 BedrockUtil.outputLaunch(cnfg[i],xw);
       }

      IProcess [] prcs = lm.getProcesses();
      for (int i = 0; i < prcs.length; ++i) {
	 BedrockUtil.outputProcess(prcs[i],xw,true);
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem getting configurations",e);
    }
   catch (Throwable t) {
      throw new BedrockException("Unknown problem getting config",t);
      // eclispe sometimes fails for no reason here
    }
}




void getNewRunConfiguration(String proj,String name,String clone,String typ,IvyXmlWriter xw)
	throws BedrockException
{
   ILaunchConfiguration cln = findLaunchConfig(clone);
   ILaunchConfigurationWorkingCopy config = null;

   if (typ == null) typ = "Java Application";

   try {
      if (cln != null) {
	 if (name == null) {
	    if (cln.isWorkingCopy()) config = (ILaunchConfigurationWorkingCopy) cln;
	    else config = cln.getWorkingCopy();
	  }
	 else config = cln.copy(name);
       }
      else {
	 if (name == null) {
	    Random r = new Random();
	    name = "NewLaunch_" + r.nextInt(1024);
	  }
	 String ltid = null;
	 ILaunchManager lm = debug_plugin.getLaunchManager();
	 ILaunchConfigurationType [] typs = lm.getLaunchConfigurationTypes();
	 String tnm1 = typ;
	 if (typ.equals("JUnit")) tnm1 = "JUnit Test";
	 else if (typ.equals("JUnit Test")) tnm1 = "JUnit";
	 for (ILaunchConfigurationType lct : typs) {
	    BedrockPlugin.logD("CHECK CONFIG " + lct.getName() + " " + typ);
	    if (lct.getName().equals(typ) || lct.getName().equals(tnm1)) {
	       ltid = lct.getIdentifier();
	       break;
	     }
	  }
	 ILaunchConfigurationType lct = lm.getLaunchConfigurationType(ltid);
	 if (lct != null) {
	    IProject ip = our_plugin.getProjectManager().findProject(proj);
	    config = lct.newInstance(ip,name);
	  }
	 else {
	    BedrockPlugin.logE("Can't create launch config " + ltid + " " + name);
	 }
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem creating launch config working copy",e);
    }

   if (config != null) {
      config.setAttribute(BEDROCK_LAUNCH_ID_PROP,"L_" + System.identityHashCode(config));
      config.removeAttribute(BEDROCK_LAUNCH_ORIGID_PROP);
      if (cln == null) {
	 config.setAttribute("org.eclipse.jdt.launching.STOP_IN_MAIN",true);
	 config.setAttribute("org.eclipse.jdt.launching.VM_ARGUMENTS","-ea");
       }
      working_configs.add(config);
      BedrockUtil.outputLaunch(config,xw);
    }
}




void editRunConfiguration(String lnch,String prop,String val,IvyXmlWriter xw)
	throws BedrockException
{
   if (lnch == null) return;
   ILaunchConfigurationWorkingCopy wc = findWorkingLaunchConfig(lnch);

   String pnm = prop_map.get(prop);
   if (pnm == null) pnm = prop;

   try {
      if ((prop.endsWith("_MAP") || prop.endsWith("launchers")) &&
	     val != null) {
	 StringTokenizer tok = new StringTokenizer(val," {},");
	 HashMap<String,String> map = new HashMap<>();
	 while (tok.hasMoreTokens()) {
	    String s = tok.nextToken();
	    int idx = s.indexOf("=");
	    if (idx < 0) continue;
	    String nm = s.substring(0,idx);
	    String vl = s.substring(idx+1);
	    map.put(nm,vl);
	  }
	 wc.setAttribute(pnm,map);
       }
      else if (prop.equals("NAME")) {
	 wc.rename(val);
       }
      else if (prop.contains("STOP_IN_MAIN")) {
	 boolean b = Boolean.valueOf(val);
	 wc.setAttribute(pnm,b);
       }
      else {
	 wc.setAttribute(pnm,val);
       }
    }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem with setAttribute for " + wc.getClass() + " with " + val);
    }

   BedrockUtil.outputLaunch(wc,xw);
}



void saveRunConfiguration(String lnch,IvyXmlWriter xw) throws BedrockException
{
   if (lnch == null) return;
   ILaunchConfigurationWorkingCopy wc = findWorkingLaunchConfig(lnch);
   if (wc == null) return;

   ILaunchConfiguration cln = null;
   try {
      String xid = BedrockUtil.getId(wc) + "X";
      xid = wc.getAttribute(BEDROCK_LAUNCH_ORIGID_PROP,xid);
      wc.setAttribute(BEDROCK_LAUNCH_ID_PROP,xid);
      cln = wc.doSave();
      working_configs.remove(wc);

    }
   catch (CoreException e) {
      throw new BedrockException("Problem saving launch configuration",e);
    }

   BedrockUtil.outputLaunch(cln,xw);
}



void deleteRunConfiguration(String lnch,IvyXmlWriter xw) throws BedrockException
{
   if (lnch == null) return;
   ILaunchConfiguration lc = findLaunchConfig(lnch);
   if (lc == null) return;
   try {
      lc.delete();
    }
   catch (CoreException e) {
      throw new BedrockException("Problem deleting launch configuration",e);
    }
}




private ILaunchConfigurationWorkingCopy findWorkingLaunchConfig(String id) throws BedrockException
{
   ILaunchConfiguration cln = findLaunchConfig(id);
   if (cln == null) return null;

   ILaunchConfigurationWorkingCopy wc = null;
   if (cln.isWorkingCopy()) wc = (ILaunchConfigurationWorkingCopy) cln;
   else {
      try {
	 wc = cln.getWorkingCopy();
	 if (!wc.hasAttribute(BEDROCK_LAUNCH_ORIGID_PROP)) {
	    String xid = BedrockUtil.getId(cln);
	    wc.setAttribute(BEDROCK_LAUNCH_ORIGID_PROP,xid);
	  }
	 String nid = "L_" + System.identityHashCode(wc);
	 wc.setAttribute(BEDROCK_LAUNCH_ID_PROP,nid);
       }
      catch (CoreException e) {
	 throw new BedrockException("Problem creating working copy",e);
       }
      working_configs.add(wc);
    }

   return wc;
}



private ILaunchConfiguration findLaunchConfig(String id) throws BedrockException
{
   if (id == null) return null;

   try {
      ILaunchManager lm = debug_plugin.getLaunchManager();
      for (ILaunchConfiguration cfg : lm.getLaunchConfigurations()) {
	 if (matchLaunchConfiguration(id,cfg)) return cfg;
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem looking up launch configuration " + id,e);
    }

   for (ILaunchConfiguration cfg : working_configs) {
      if (matchLaunchConfiguration(id,cfg)) return cfg;
    }

   throw new BedrockException("Unknown launch configuration " + id);
}



static String getExternalPropertyName(String p)
{
   if (p == null) return null;

   for (Map.Entry<String,String> ent : prop_map.entrySet()) {
      if (p.equals(ent.getValue())) return ent.getKey();
    }

   return p;
}



/********************************************************************************/
/*										*/
/*	Handle running a launch configuration					*/
/*										*/
/********************************************************************************/

void runProject(String cfg,String mode,boolean build,boolean reg,String vmarg,String id,IvyXmlWriter xw)
	throws BedrockException
{
   try {
      ILaunchConfiguration cnf = findLaunchConfig(cfg);
      if (cnf == null) throw new BedrockException("Launch configuration " + cfg + " not found");

      if (vmarg != null) {
	 ILaunchConfigurationWorkingCopy ccnf = cnf.getWorkingCopy();
	 String vmatt = prop_map.get("VM_ARGUMENTS");
	 String ja = ccnf.getAttribute(vmatt,(String) null);
	 if (ja == null || ja.length() == 0) ja = vmarg;
	 else ja = ja + " " + vmarg;
	 ccnf.setAttribute(BEDROCK_LAUNCH_IGNORE_PROP,"true");
	 ccnf.setAttribute(vmatt,ja);
	 cnf = ccnf;
       }

      ILaunch lnch = cnf.launch(mode,null,build,reg);
      if (lnch != null) {
	 xw.begin("LAUNCH");
	 xw.field("MODE",lnch.getLaunchMode());
	 xw.field("TAG",lnch.toString());
	 xw.field("ID",lnch.hashCode());
	 IDebugTarget tgt = lnch.getDebugTarget();
	 if (tgt != null) {		// will be null if doing a run rather than debug
	    xw.field("TARGET",tgt.hashCode());
	    xw.field("TARGETTAG",tgt.toString());
	    xw.field("NAME",tgt.getName());
	    IProcess ip = tgt.getProcess();
	    if (ip != null) {
	       xw.field("PROCESSTAG",ip.getLabel());
	       xw.field("PROCESS",ip.hashCode());
	       setupConsole(ip);
	     }
	    if (tgt instanceof IJavaDebugTarget) {
	       IJavaDebugTarget jdt = (IJavaDebugTarget) tgt;
	       jdt.addHotCodeReplaceListener(this);
	     }
	  }
	 xw.end("LAUNCH");
         BedrockPlugin.logD("Launch successful");
       }
    }
   catch (Throwable e) {
      throw new BedrockException("Launch failed: " + e,e);
    }
}



/********************************************************************************/
/*										*/
/*	Handle debug actions							*/
/*										*/
/********************************************************************************/

void debugAction(String lname,String gname,String pname,String tname,String fname,
		    BedrockDebugAction act,
		    IvyXmlWriter xw) throws BedrockException
{
   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      if (!matchLaunch(lname,launch)) continue;
      if (gname == null && pname == null && tname == null) {
	 if (doAction(launch,act)) {
	    xw.textElement("LAUNCH",act.toString());
	    continue;
	  }
       }
      try {
	 for (IDebugTarget dt : launch.getDebugTargets()) {
	    if (!matchDebugTarget(gname,dt)) continue;
	    if (pname != null && !matchProcess(pname,dt.getProcess())) continue;
	    if (tname == null) {
	       if (doAction(dt,act)) {
		  xw.textElement("TARGET",act.toString());
		  continue;
		}
	     }
	    for (IThread th : dt.getThreads()) {
	       if (!matchThread(tname,th)) continue;
	       if (!th.isSuspended() && act != BedrockDebugAction.SUSPEND) continue;
	       doAction(th,fname,act);
	       xw.textElement("THREAD", act.toString());
	     }
	  }
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem getting launch information: " + e);
       }
    }
}



private boolean doAction(ILaunch il,BedrockDebugAction act) throws BedrockException
{
   try {
      switch (act) {
	 case NONE :
	    break;
	 case TERMINATE :
	    if (il.canTerminate()) il.terminate();
	    else return false;
	    break;
	 default :
	    return false;
       }
    }
   catch (DebugException e) {
      throw new BedrockException("Problem setting launch status",e);
    }

   return true;
}



private boolean doAction(IDebugTarget dt,BedrockDebugAction act) throws BedrockException
{
   try {
      switch (act) {
	 case NONE :
	    break;
	 case TERMINATE :
	    if (dt.canTerminate()) dt.terminate();
	    else return false;
	    break;
	 case SUSPEND :
	    if (dt.canSuspend()) dt.suspend();
	    else return false;
	    break;
	 case RESUME :
	    if (dt.canResume()) dt.resume();
	    else return false;
	    break;
	 default :
	    return false;
       }
    }
   catch (DebugException e) {
      throw new BedrockException("Problem setting debug target status",e);
    }

   return true;
}


private boolean doAction(IThread thrd,String fname,BedrockDebugAction act) throws BedrockException
{
   try {
      switch (act) {
	 case NONE :
	    return false;
	 case TERMINATE :
	    if (thrd.canTerminate()) thrd.terminate();
	    else return false;
	    break;
	 case RESUME :
	    if (thrd.canResume()) thrd.resume();
	    else return false;
	    break;
	 case SUSPEND :
	    if (thrd.canSuspend()) thrd.suspend();
	    else return false;
	    break;
	 case STEP_INTO :
	    if (thrd.canStepInto()) thrd.stepInto();
	    else return false;
	    break;
	 case STEP_OVER :
	    if (thrd.canStepOver()) thrd.stepOver();
	    else return false;
	    break;
	 case DROP_TO_FRAME :
	    IStackFrame topfrm = thrd.getTopStackFrame();
	    if (fname != null) {
	       for (IStackFrame frame: thrd.getStackFrames()) {
		  if (matchFrame(fname,frame)) topfrm = frame;
		}
	     }
	    if (topfrm instanceof IDropToFrame) {
	       IDropToFrame idtf = (IDropToFrame) topfrm;
	       if (idtf.canDropToFrame()) idtf.dropToFrame();
	       else {
		  BedrockPlugin.logD("Drop to frame failed for " +
					topfrm.getThread() + " " +
					topfrm.getName() + " " +
					topfrm.getLineNumber());
		  for (IStackFrame frame : thrd.getStackFrames()) {
		     BedrockPlugin.logD("Current frames include: " +
					   frame.getThread() + " " +
					   frame.getName() + " " +
					   frame.getLineNumber());
		   }
		  return false;
		}
	     }
	    else {
	       BedrockPlugin.logD("No support for drop to frame " + thrd);
	       return false;
	     }
	    break;
	 case STEP_RETURN :
	    if (thrd.canStepReturn()) thrd.stepReturn();
	    else return false;
	    break;
       }
    }
   catch (DebugException e) {
      BedrockPlugin.log(BedrockLogLevel.INFO,"Problem with debug action",e);
      throw new BedrockException("Problem setting thread status: " + e,e);
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Stack access methods							*/
/*										*/
/********************************************************************************/

void getStackFrames(String lname,String tname,int count,int vdepth,int arraysz,
      IvyXmlWriter xw) throws BedrockException
{
   ILaunch[] launches = debug_plugin.getLaunchManager().getLaunches();
   xw.begin("STACKFRAMES");

   try {
      for (ILaunch launch: launches) {
	 if (!matchLaunch(lname,launch)) continue;
	 IDebugTarget dt = launch.getDebugTarget();
	 if (tname != null) {
	    boolean fnd = false;
	    for (IThread th : dt.getThreads()) {
	       if (matchThread(tname,th)) {
		  fnd = true;
		  break;
		}
	     }
	    if (!fnd) continue;
	  }
	 for (IThread th : dt.getThreads()) {
	    if (matchThread(tname,th)) {
	       dumpFrames(th,count,vdepth,arraysz,xw);
	     }
	  }
       }
    }
   catch (DebugException e) {
      throw new BedrockException("Problem getting stack frames: " + e,e);
    }

   xw.end("STACKFRAMES");
}




private void dumpFrames(IThread thread,int count,int vdepth,int arraysz,IvyXmlWriter xw) throws DebugException
{
   xw.begin("THREAD");
   xw.field("NAME",thread.getName());
   xw.field("ID",thread.hashCode());
   xw.field("TAG",thread.toString());

   int ctr = 0;
   for (IStackFrame frame: thread.getStackFrames()) {
      if (frame == null) continue;
      IJavaStackFrame jsf = (IJavaStackFrame) frame;

      BedrockUtil.outputStackFrame(jsf,ctr,vdepth,arraysz,xw);
      if (count > 0 && ctr > count) break;
      ++ctr;
    }

   if (thread instanceof IJavaThread) {
      IJavaThread jt = (IJavaThread) thread;
      if (jt.isSuspended()) {
	 try {
	    IJavaObject [] mons = jt.getOwnedMonitors();
	    if (mons != null && mons.length > 0) {
	       xw.begin("OWNS");
	       for (IJavaObject jo : mons) {
		  BedrockUtil.outputValue(jo,null,null,0,arraysz,xw);
		}
	       xw.end("OWNS");
	     }
	  }
	 catch (DebugException e) { }
       }
    }

   xw.end("THREAD");
}




/********************************************************************************/
/*										*/
/*	Methods to access variables						*/
/*										*/
/********************************************************************************/

// CHECKSTYLE:OFF
void getVariableValue(String tname,String frid,String vname,int lvls,int arraymax,IvyXmlWriter xw)
		throws BedrockException
// CHECKSTYLE:ON
{
   IThread thrd = null;
   IStackFrame sfrm = null;
   IVariable var = null;

   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      IJavaDebugTarget tgt = (IJavaDebugTarget) launch.getDebugTarget();
      try {
	 for (IThread thread : tgt.getThreads()) {
	    if (matchThread(tname,thread)) {
	       if (thread.isSuspended()) {
		  thrd = thread;
		  break;
		}
	       else if (tname != null) {
		  BedrockPlugin.logI("Thread " + tname + " not suspended");
		  continue;
		}
	     }
	  }

	 if (thrd == null) continue;		// not in this launch

	 for (IStackFrame frame: thrd.getStackFrames()) {
	    if (matchFrame(frid,frame)) {
	       sfrm = frame;
	       break;
	     }
	  }

	 if (sfrm == null) {
	    BedrockPlugin.logI("Stack frame " + frid + " doesn't exist");
	    continue;
	  }

	 StringTokenizer tok = new StringTokenizer(vname,"?");
	 if (!tok.hasMoreTokens()) throw new BedrockException("No variable specified");

	 IValue val = null;
	 String vhead = tok.nextToken();
	 if (vhead.startsWith("*")) {
	    synchronized (outside_variables) {
	       Map<String,IValue> vals = outside_variables.get(sfrm);
	       BedrockPlugin.logD("VAR FIND " + vhead + " " + vals);
	       if (vals != null) {
		  val = vals.get(vhead);
		  if (val == null) val = vals.get(vhead.substring(1));
		}
	     }
	    if (val == null) {
	       BedrockPlugin.logD("VAR FAIL " + vhead + " " + sfrm.hashCode() + " " +
		     thrd.hashCode() + " " + tname + " " + frid + " " + vname);
	       throw new BedrockException("Save variable " + vhead + " not found");
	     }
	  }
	 else {
	    for (IVariable variable : sfrm.getVariables()) {
	       if (variable.getName().equals(vhead)) var = variable;
	     }
	    if (var == null) throw new BedrockException("Variable " + vhead + " not found");
	    val = var.getValue();
	    BedrockPlugin.logD("VAR START " + vhead + " " + var.getName() + " " + val + " " + val.hasVariables());
	  }

	 while (tok.hasMoreTokens()) {
	    boolean found = false;
	    var = null;
	    String next = tok.nextToken();
	    if (next.startsWith("@")) {
	       if (next.equals("@hashCode")) {
		  IJavaThread jthrd = (IJavaThread) thrd;
		  IJavaType [] typs = tgt.getJavaTypes("java.lang.System");
		  IJavaClassType systemtype = null;
		  if (typs != null && typs.length > 0) systemtype = (IJavaClassType) typs[0];
		  if (systemtype == null) throw new BedrockException("Type java.lang.System not found");
		  IJavaValue [] args = new IJavaValue[1];
		  args[0] = (IJavaValue) val;
		  String tsg = "(Ljava/lang/Object;)I";
		  try {
		     val = varEval(systemtype,"identityHashCode",tsg,args,jthrd);
		   }
		  catch (Throwable t) {
		     BedrockPlugin.logE("Problem getting system hash code",t);
		     throw new BedrockException("Problem getting hash code");
		   }
		}
	       continue;
	     }
	    if (val.hasVariables()) {
	       for (IVariable t: val.getVariables()) {
		  // BedrockPlugin.logD("VAR LOOKUP " + t + " " + next);
		  if (matchVariable(next,t)) {
		     found = true;
		     val = t.getValue();
		     BedrockPlugin.logD("VAR FOUND " + next + " " + val + " " + val.hasVariables());
		     break;
		   }
		}
	     }
	    else if (val instanceof IJavaArray) {
	       IJavaArray arr = (IJavaArray) val;
	       int idx0 = next.indexOf("[");
	       if (idx0 >= 0) next = next.substring(idx0+1);
	       idx0 = next.indexOf("]");
	       if (idx0 >= 0) next = next.substring(0,idx0);
	       try {
		  int sub = Integer.parseInt(next);
		  val = arr.getValue(sub);
		  found = true;
		}
	       catch (NumberFormatException e) {
		  throw new BedrockException("Index expected");
		}
	     }
	    if (!found) {
	       val = null;
	       break;
	     }
	  }

	 if (val == null || tok.hasMoreTokens()) throw new BedrockException("Variable doesn't exists: " + vname);

	 if (lvls < 0 && thrd instanceof IJavaThread) {
	    IJavaThread jthrd = (IJavaThread) thrd;
	    if (val instanceof IJavaArray) {
	       IJavaType [] typs = tgt.getJavaTypes("java.util.Arrays");
	       IJavaClassType arrays = null;
	       if (typs != null && typs.length > 0) arrays = (IJavaClassType) typs[0];
	       if (arrays == null) throw new BedrockException("Type java.util.Arrays not found");
	       IJavaArray avl = (IJavaArray) val;
	       IJavaType typ = avl.getJavaType();
	       String tsg = typ.getSignature();
	       if (tsg.startsWith("[[") || tsg.contains(";")) tsg = "[Ljava/lang/Object;";
	       tsg = "(" + tsg + ")Ljava/lang/String;";
	       IJavaValue [] args = new IJavaValue[1];
	       args[0] = avl;
	       try {
		  val = varEval(arrays,"toString",tsg,args,jthrd);
		}
	       catch (Throwable t) {
		  BedrockPlugin.logE("Problem getting array value: " + tsg,t);
		  val = varEval(avl,"toString","()Ljava/lang/String;",null,jthrd,false);
		}
	     }
	    else {
	       val = handleSpecialCases(val,thrd);
	     }
	  }

	 BedrockUtil.outputValue(val,(IJavaVariable) var,vname,lvls,arraymax,xw);
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem getting variable: " + e,e);
	 throw new BedrockException("Problem accessing variable: " + e,e);
       }
    }
}


private IValue handleSpecialCases(IValue val,IThread thrd)
{
   if (!(val instanceof IJavaObject)) return val;
   if (!(thrd instanceof IJavaThread)) return val;

   IJavaObject ovl = (IJavaObject) val;
   IJavaThread jthrd = (IJavaThread) thrd;

   try {
      if (!ovl.isNull()) {
	 IJavaType jt = ovl.getJavaType();
	 IJavaValue xvl = null;
	 CallFormatter cfmt = format_map.get(jt.getName());
	 if (cfmt != null) {
	    xvl = cfmt.convertValue(ovl,jthrd);
	  }
	 if (xvl == null) {
	    if (jt.getName().equals("org.apache.xerces.dom.DeferredElementImpl")) {
	       xvl = convertXml(jthrd,ovl);
	     }
	    else if (jt.getName().equals("com.sun.apache.xerces.internal.dom.DeferredElementImpl")) {
	       xvl = convertXml(jthrd,ovl);
	     }
	  }
	 if (xvl == null) {
	    val = varEval(ovl,"toString","()Ljava/lang/String;",null,jthrd,false);
	  }
	 else val = xvl;
       }
    }
   catch (DebugException e) {
      BedrockPlugin.logE("Problem handling variable values",e);
    }

   return val;
}


IJavaValue convertXml(IJavaThread thrd,IJavaValue xml)
{
   IJavaDebugTarget tgt = (IJavaDebugTarget) thrd.getDebugTarget();
   try {
      IJavaType [] typs = tgt.getJavaTypes("edu.brown.cs.ivy.xml.IvyXml");
      if (typs == null) return null;
      IJavaClassType ivyxml = (IJavaClassType) typs[0];
      IJavaValue [] args = new IJavaValue[] { xml };
      IJavaValue rslt = varEval(ivyxml,"convertXmlToString","(Lorg/w3c/dom/Node;)Ljava/lang/String;",
	    args,thrd);
      return rslt;
   }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem converting XML",t);
   }

   return null;
}



private static class CallFormatter {

   private String static_class;
   private String method_name;
   private String method_signature;
   private List<Object> arg_values;

   CallFormatter(String method,String sign,Iterable<Object> args) {
      int idx = method.lastIndexOf(".");
      if (idx > 0) {
	 static_class = method.substring(0,idx);
	 method_name = method.substring(idx+1);
       }
      else {
	 static_class = null;
	 method_name = method;
       }
      method_signature = sign;
      arg_values = null;
      if (args != null) {
	 for (Object o : args) {
	    arg_values.add(o);
	  }
       }
    }

   IJavaValue convertValue(IJavaObject v,IJavaThread thrd) {
      IJavaValue rslt = null;
      try {
	 IJavaDebugTarget tgt = (IJavaDebugTarget) thrd.getDebugTarget();
	 IJavaType [] typs = tgt.getJavaTypes(static_class);
	 if (static_class == null) {
	    // method call on v
	    IJavaValue [] args = setupArgs(tgt,null);
	    rslt = varEval(v,method_name,method_signature,args,thrd,false);
	  }
	 else {
	    // static method call
	    if (typs == null) return null;
	    IJavaClassType clstyp = (IJavaClassType) typs[0];
	    IJavaValue [] args = setupArgs(tgt,v);
	    rslt = varEval(clstyp,method_name,method_signature,args,thrd);
	  }
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem handling value conversion",e);
       }
      return rslt;
    }

   IJavaValue [] setupArgs(IJavaDebugTarget tgt,IJavaValue arg0) {
      int ct = (arg_values == null ? 0 : arg_values.size());
      if (arg0 != null) ++ct;
      if (ct == 0) return null;
      IJavaValue [] args = new IJavaValue[ct];
      int idx = 0;
      if (arg0 != null) args[idx++] = arg0;
      if (arg_values != null) {
	 for (Object o : arg_values) {
	    IJavaValue v = null;
	    if (o == null) v = tgt.nullValue();
	    else if (o instanceof Boolean) v = tgt.newValue(((Boolean) o).booleanValue());
	    else if (o instanceof Byte) v = tgt.newValue(((Byte) o).byteValue());
	    else if (o instanceof Character) v = tgt.newValue(((Character) o).charValue());
	    else if (o instanceof Double) v = tgt.newValue(((Double) o).doubleValue());
	    else if (o instanceof Float) v = tgt.newValue(((Float) o).floatValue());
	    else if (o instanceof Integer) v = tgt.newValue(((Integer) o).intValue());
	    else if (o instanceof Long) v = tgt.newValue(((Long) o).longValue());
	    else if (o instanceof Short) v = tgt.newValue(((Short) o).shortValue());
	    else if (o instanceof String) v = tgt.newValue(((String) o));
	    else {
	       BedrockPlugin.logE("Unknown type for conversion args" + o);
	       v = tgt.nullValue();
	     }
	    args[idx++] = v;
	  }
       }

      return args;
    }

}	// end of inner class CallFormatter




/********************************************************************************/
/*										*/
/*	Evaluate an expression in the context of the given thread		*/
/*										*/
/********************************************************************************/

// CHECKSTYLE:OFF
void evaluateExpression(String proj,String bid,String expr,String tname,String frid,boolean impl,
			   boolean bkpt,String eid,int lvl,int arraysz,String saveid,boolean allframes,
			   IvyXmlWriter xw) throws BedrockException
// CHECKSTYLE:ON
{
   IProject ip = our_plugin.getProjectManager().findProject(proj);
   IJavaProject jproj = JavaCore.create(ip);

   IThread thrd = null;
   IStackFrame sfrm = null;
   boolean evaldone = false;

   BedrockPlugin.logD("Evaluate " + tname + " " + frid + " " + eid + " " + expr);

   int detail = (impl ? DebugEvent.EVALUATION_IMPLICIT : DebugEvent.EVALUATION);

   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      try {
	 IJavaDebugTarget tgt = (IJavaDebugTarget) launch.getDebugTarget();
	 if (tgt == null) throw new BedrockException("No target to evaluate in");
	 for (IThread thread : tgt.getThreads()) {
	    if (matchThread(tname,thread)) {
	       if (thread.isSuspended()) {
		  thrd = thread;
		  break;
		}
	       else if (tname != null) {
		  throw new BedrockException("Thread " + tname + " not suspended");
		}
	     }
	  }
	 if (thrd == null) {
	    BedrockPlugin.logD("Evaluation thread " + tname + " not found in launch " + launch);
	    continue;		// not in this launch
	  }
	 for (IStackFrame frame: thrd.getStackFrames()) {
	    if (matchFrame(frid,frame)) {
	       sfrm = frame;
	       break;
	     }
	  }
	 if (sfrm == null) {
	    BedrockPlugin.logD("Stack frame " + frid + " doesn't exist");
	    return;
	  }
	 if (!(sfrm instanceof IJavaStackFrame)) {
	    throw new BedrockException("Stack frame " + frid + " not java frame");
	  }
	 IJavaStackFrame jsf = (IJavaStackFrame) sfrm;

	 if (jproj == null) {
	    BedrockProject bp = BedrockPlugin.getPlugin().getProjectManager();
	    String pnm = launch.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR");
	    if (pnm != null) {
	       IProject ip1 = bp.findProject(pnm);
	       if (ip1 != null) jproj = JavaCore.create(ip1);
	     }
	    if (jproj == null) {
	       IProject ip1 = bp.findProjectForFile(null,jsf.getSourcePath());
	       if (ip1 != null) jproj = JavaCore.create(ip1);
	     }
	    if (jproj == null) {
	       BedrockPlugin.logD("Can't find project for frame " + jsf.getSourcePath() + " " +
		     jsf.getSourceName());
	     }
	  }

	 BedrockPlugin.logD("COMPILE EXPRESSION " + expr + " FOR " + proj + " " + tgt.getName());
	 IAstEvaluationEngine eeng = EvaluationManager.newAstEvaluationEngine(jproj,tgt);
	 ICompiledExpression eexp = compileExpression(eeng,expr,jsf);
	 IStackFrame origframe = jsf;
	 if (eexp == null || eexp.hasErrors() && allframes) {
	    if (eexp != null) {
	       String [] errs = eexp.getErrorMessages();
	       for (String err : errs) {
		  BedrockPlugin.logD("COMPILER ERRORS FOUND: " + err + " " + jsf + " " + frid + " " + thrd);
		}
	     }

	    HashSet<String> done = new HashSet<>();
	    for (IStackFrame frame: thrd.getStackFrames()) {
	       if (frame instanceof IJavaStackFrame) {
		  IJavaStackFrame njsf = (IJavaStackFrame) frame;
		  String key = njsf.getSourceName() + njsf.getMethodName() + njsf.getLineNumber();
		  if (!done.add(key)) continue;
		  if (njsf == jsf) continue;
		  eexp = compileExpression(eeng,expr,njsf);
		  if (eexp == null || eexp.hasErrors()) {
		     BedrockPlugin.logD("COMPILER ERRORS FOUND: " + njsf);
		     continue;
		   }
		  BedrockPlugin.logD("COMPILED OKAY");
		  jsf = njsf;
		  break;
		}
	     }
	  }
	 eeng.evaluateExpression(eexp,jsf,new EvalListener(origframe,bid,eid,saveid,lvl,arraysz),detail,bkpt);
	 BedrockPlugin.logD("START EVALUATION OF " + expr + " " + bid + " " + eid + " " +
	       jsf.hashCode() + " " + thrd.hashCode());
	 evaldone = true;
       }
      catch (Throwable e) {
	 BedrockPlugin.logE("Problem evaluating expression: " + expr + ": " + e,e);
	 throw new BedrockException("Problem evaluating expression: " + e,e);
       }
    }

   if (!evaldone) {
      throw new BedrockException("No evaluation to do");
    }
}



private ICompiledExpression compileExpression(IAstEvaluationEngine eeng,String exp,IJavaStackFrame jsf)
{
   try {
      return eeng.getCompiledExpression(exp,jsf);
    }
   catch (Throwable t) {
      BedrockPlugin.logD("Problem compiling expression " + t);
    }
   return null;
}




private class EvalListener implements IEvaluationListener {

   private String for_id;
   private String reply_id;
   private String save_id;
   private IStackFrame for_frame;
   private int output_level;
   private int array_size;

   EvalListener(IStackFrame frm,String bid,String eid,String sid,int lvl,int arraysz) {
      for_id = bid;
      reply_id = eid;
      save_id = sid;
      for_frame = frm;
      output_level = lvl;
      array_size = arraysz;
    }

   @Override public void evaluationComplete(IEvaluationResult rslt) {
      BedrockPlugin.logD("FINISH EVALUTAION OF " + for_id + " " + reply_id + " " + save_id + " " + output_level);
      if (save_id != null) {
	 try {
	    synchronized (outside_variables) {
	       Map<String,IValue> vals = outside_variables.get(for_frame);
	       if (vals == null) {
		  vals = new HashMap<>();
		  outside_variables.put(for_frame,vals);
		}
	       vals.put(save_id,rslt.getValue());
	     }
	  }
	 catch (Throwable t) {
	    BedrockPlugin.logE("Problem with saving value",t);
	  }
       }
      BedrockPlugin.logD("START EVAL MESSAGE OUT");
      IvyXmlWriter xw = our_plugin.beginMessage("EVALUATION",for_id);
      xw.field("ID",reply_id);
      if (save_id != null) xw.field("SAVEID",save_id);
      try {
	 BedrockUtil.outputValue(rslt,output_level,array_size,xw);
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem with eval output",t);
	 xw.textElement("ERROR",t.toString());
       }
      BedrockPlugin.logD("EVAL: " + xw.toString());
      our_plugin.finishMessage(xw);
    }

}	// end of inner class EvalListener




/********************************************************************************/
/*										*/
/*	Get the details of a value						*/
/*										*/
/********************************************************************************/

void getVariableDetails(String tname,String frid,String vname,IvyXmlWriter xw)
		throws BedrockException
{
   throw new BedrockException("Not implemented yet");
}

/****************
	// IValueDetailListener requires org.eclipse.debug.ui
	//    which in turn requires that the workbench be
	//    running before this is initialized

void newGetVaiableDetails(String tname,String frid,String vname,IvyXmlWriter xw)
		throws BedrockException
{
   IThread thrd = null;
   IStackFrame sfrm = null;
   IVariable var = null;

   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      try {
	 for (IThread thread : launch.getDebugTarget().getThreads()) {
	    if (matchThread(tname,thread)) {
	       if (thread.isSuspended()) {
		  thrd = thread;
		  break;
		}
	       else if (tname != null) {
		  throw new BedrockException("Thread " + tname + " not suspended");
		}
	     }
	  }

	 if (thrd == null) continue;		// not in this launch

	 for (IStackFrame frame: thrd.getStackFrames()) {
	    if (matchFrame(frid,frame)) sfrm = frame;
	  }

	 if (sfrm == null) throw new BedrockException("Stack frame " + frid + " doesn't exist");

	 StringTokenizer tok = new StringTokenizer(vname,"?");
	 if (!tok.hasMoreTokens()) throw new BedrockException("No variable specified");

	 String vhead = tok.nextToken();

	 for (IVariable variable : sfrm.getVariables()) {
	    if (variable.getName().equals(vhead)) var = variable;
	  }

	 if (var == null) throw new BedrockException("Variable " + vhead + " not found");
	 IValue val = var.getValue();
	 BedrockPlugin.logD("VARD START " + vhead + " " + var + " " + val + " " + val.hasVariables());

	 while (tok.hasMoreTokens()) {
	    boolean found = false;
	    var = null;
	    String next = tok.nextToken();
	    if (val.hasVariables()) {
	       for (IVariable t: val.getVariables()) {
		  BedrockPlugin.logD("VARD LOOKUP " + t + " " + next);
		  if (matchVariable(next,t)) {
		     found = true;
		     val = t.getValue();
		     BedrockPlugin.logD("VARD FOUND " + val + " " + val.hasVariables());
		     break;
		   }
		}
	     }
	    else if (val instanceof IJavaArray) {
	       IJavaArray arr = (IJavaArray) val;
	       int idx0 = next.indexOf("[");
	       if (idx0 >= 0) next = next.substring(idx0+1);
	       idx0 = next.indexOf("]");
	       if (idx0 >= 0) next = next.substring(0,idx0);
	       try {
		  int sub = Integer.parseInt(next);
		  val = arr.getValue(sub);
		  found = true;
		}
	       catch (NumberFormatException e) {
		  throw new BedrockException("Index expected");
		}
	     }
	    if (!found) {
	       val = null;
	       break;
	     }
	  }

	 IDebugTarget tgt = launch.getDebugTarget();
	 String id = tgt.getModelIdentifier();
	 try {
	    IWorkbench wb = PlatformUI.getWorkbench();
	    IWorkbenchPage page = wb.getActiveWorkbenchWindow().getActivePage();
	    IDebugView idv = (IDebugView) page.findView(IDebugUIConstants.ID_VARIABLE_VIEW);
	    IDebugModelPresentation dmp = idv.getPresentation(id);
	    DetailListener dl = new DetailListener();
	    dmp.computeDetail(val,dl);
	    String rslt = dl.waitFor();
	    xw.textElement("DETAIL",rslt);
	  }
	 catch (Throwable t) {
	    BedrockPlugin.logE("Problem getting detail data: " + t,t);
	  }
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem getting variable: " + e,e);
	 throw new BedrockException("Problem accessing variable: " + e,e);
       }
    }
}




private class DetailListener implements IValueDetailListener {

   private boolean is_done;
   private String detail_result;

   DetailListener() {
      is_done = false;
      detail_result = null;
    }

   String waitFor() {
      synchronized (this) {
	 while (!is_done) {
	    try {
	       wait();
	     }
	    catch (InterruptedException e) { }
	  }
       }
      return detail_result;
    }

   @Override public void detailComputed(IValue val,String rslt) {
      BedrockPlugin.logD("Detail computed: " + rslt);
      synchronized (this) {
	 detail_result = rslt;
	 is_done = true;
	 notifyAll();
       }
    }

}	// end of inner class DetailListener

******************/





/********************************************************************************/
/*										*/
/*	Event handler for debug events						*/
/*										*/
/********************************************************************************/

private static Map<String,Integer> kind_values;
private static Map<String,Integer> detail_values;

static {
   kind_values = new HashMap<String,Integer>();
   kind_values.put("RESUME",1);
   kind_values.put("SUSPEND",2);
   kind_values.put("CREATE",4);
   kind_values.put("TERMINATE",8);
   kind_values.put("CHANGE",16);
   kind_values.put("MODEL_SPECIFIC",32);

   detail_values = new HashMap<String,Integer>();
   detail_values.put("STEP_INTO",1);
   detail_values.put("STEP_OVER",2);
   detail_values.put("STEP_RETURN",4);
   detail_values.put("STEP_END",8);
   detail_values.put("BREAKPOINT",16);
   detail_values.put("CLIENT_REQUEST",32);
   detail_values.put("EVALUATION",64);
   detail_values.put("EVALUATION_IMPLICIT",128);
   detail_values.put("STATE",256);
   detail_values.put("CONTENT",512);
}



@Override public void handleDebugEvents(DebugEvent[] events)
{
   if (events.length <= 0) return;
   
   BedrockPlugin.logD("Handle debug events: " + events);

   IvyXmlWriter xw = our_plugin.beginMessage("RUNEVENT");
   xw.field("TIME",System.currentTimeMillis());
   int ct = 0;

   for (DebugEvent event: events) {
      IJavaThread ijt = null;
      if (event.getSource() instanceof IJavaThread) {
	 ijt = (IJavaThread) event.getSource();
	 try {
	    if (ijt.getName() != null && ijt.getName().equals("garbage collected")) continue;
	  }
	 catch (DebugException e) { }
       }
      if (handleInternalEvent(event)) continue;

      ++ct;
      xw.begin("RUNEVENT");
      if (event.getData() != null) xw.field("DATA",event.getData());
      BedrockUtil.fieldValue(xw,"KIND",event.getKind(),kind_values);
      BedrockUtil.fieldValue(xw,"DETAIL",event.getDetail(),detail_values);
      xw.field("EVAL",event.isEvaluation());
      xw.field("STEPSTART",event.isStepStart());

      if (event.getSource() instanceof IProcess) {
	 xw.field("TYPE","PROCESS");
	 BedrockUtil.outputProcess((IProcess) event.getSource(),xw,false);
	 if (event.getKind() == 8) {
	    queueConsole(event.getSource().hashCode(),null,false,true);
	  }
       }
      else if (ijt != null) {
	 xw.field("TYPE","THREAD");
	 BedrockUtil.outputThread(ijt,xw);
       }
      else if (event.getSource() instanceof IJavaDebugTarget) {
	 xw.field("TYPE","TARGET");
	 BedrockUtil.outputDebugTarget((IJavaDebugTarget) event.getSource(),xw);
       }
      else xw.field("SOURCE",event.getSource());
      xw.end("RUNEVENT");
    }

   if (ct == 0) return;

   BedrockPlugin.logD("RUNEVENT: " + xw.toString());

   our_plugin.finishMessageWait(xw);
   
   BedrockPlugin.logD("FINISHED RUNEVENT");
}



/********************************************************************************/
/*										*/
/*	Handle events during variable evaluations				*/
/*										*/
/********************************************************************************/

private static IJavaValue varEval(IJavaObject ovl,String method,String sign,IJavaValue [] args,
      IJavaThread th,boolean sup) throws DebugException
{
   startThreadEval(th);
   try {
      return ovl.sendMessage(method,sign,args,th,sup);
    }
   finally {
      endThreadEval(th);
    }
}



private static IJavaValue varEval(IJavaClassType cty,String method,String sign,IJavaValue [] args,
      IJavaThread th) throws DebugException
{
   startThreadEval(th);
   try {
      return cty.sendMessage(method,sign,args,th);
    }
   finally {
      endThreadEval(th);
    }
}



private static void startThreadEval(IJavaThread th)
{
   if (th == null) return;
   synchronized (variable_threads) {
      variable_threads.add(th);
    }
}


private static void endThreadEval(IJavaThread th)
{
   if (th == null) return;
   synchronized (variable_threads) {
      variable_threads.remove(th);
    }
}



private boolean handleInternalEvent(DebugEvent event)
{
   if (event.getSource() instanceof IJavaThread) {
      IJavaThread ijt = (IJavaThread) event.getSource();
      synchronized (variable_threads) {
	 if (!variable_threads.contains(ijt)) return false;
       }
      if (event.getKind() == DebugEvent.SUSPEND &&
	    event.getDetail() == DebugEvent.BREAKPOINT) {
	 try {
	    ijt.resume();
	  }
	 catch (DebugException e) {
	    BedrockPlugin.logE("Problem resuming variable thread",e);
	  }
	 return true;
       }
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Event handler for launch configuration events				*/
/*										*/
/********************************************************************************/

@Override public void launchConfigurationAdded(ILaunchConfiguration cfg)
{
   IvyXmlWriter xw = our_plugin.beginMessage("LAUNCHCONFIGEVENT");

   xw.begin("LAUNCH");
   xw.field("REASON","ADD");
   BedrockUtil.outputLaunch(cfg,xw);
   xw.end("LAUNCH");

   our_plugin.finishMessage(xw);
}



@Override public void launchConfigurationChanged(ILaunchConfiguration cfg)
{
   IvyXmlWriter xw = our_plugin.beginMessage("LAUNCHCONFIGEVENT");

   xw.begin("LAUNCH");
   xw.field("REASON","CHANGE");
   BedrockUtil.outputLaunch(cfg,xw);
   xw.end("LAUNCH");

   our_plugin.finishMessage(xw);
}



@Override public void launchConfigurationRemoved(ILaunchConfiguration cfg)
{
   IvyXmlWriter xw = our_plugin.beginMessage("LAUNCHCONFIGEVENT");

   // need to make sure it is realy removed here.  If it was just renamed, then
   // we are using the same ID and it hasn't been removed
   ILaunchManager lm = debug_plugin.getLaunchManager();

   String rid = BedrockUtil.getId(cfg);

   try {
      ILaunchConfiguration [] cnfg = lm.getLaunchConfigurations();

      for (int i = 0; i < cnfg.length; ++i) {
	 if (cnfg[i] == cfg) continue;
	 String id = BedrockUtil.getId(cnfg[i]);
	 if (id.equals(rid)) return;
       }
    }
   catch (Throwable t) { }

   xw.begin("LAUNCH");
   xw.field("REASON","REMOVE");
   BedrockUtil.outputLaunch(cfg,xw);
   xw.end("LAUNCH");

   our_plugin.finishMessage(xw);
}




/********************************************************************************/
/*										*/
/*	Event handler for hot code replace					*/
/*										*/
/********************************************************************************/

@Override public void hotCodeReplaceFailed(IJavaDebugTarget tgt,DebugException e)
{
   BedrockPlugin.logD("HOT CODE replace failed " + tgt + " " + e);

   sendHotCodeMessage(tgt,false,e);
}


@Override public void hotCodeReplaceSucceeded(IJavaDebugTarget tgt)
{
   BedrockPlugin.logD("HOT CODE replace success " + tgt);

   sendHotCodeMessage(tgt,true,null);
}


private void sendHotCodeMessage(IJavaDebugTarget tgt,boolean succ,DebugException e)
{
   IvyXmlWriter xw = our_plugin.beginMessage("RUNEVENT");
   xw.field("TIME",System.currentTimeMillis());
   xw.begin("RUNEVENT");
   xw.field("TYPE","TARGET");
   xw.field("KIND",(succ ? "HOTCODE_SUCCESS" : "HOTCODE_FAILURE"));
   if (e != null) xw.field("ERROR",e.toString());
   BedrockUtil.outputDebugTarget(tgt,xw);
   xw.end("RUNEVENT");
   BedrockPlugin.logD("RUNEVENT: " + xw.toString());
   our_plugin.finishMessageWait(xw);
}


@Override public void obsoleteMethods(IJavaDebugTarget tgt)
{
   BedrockPlugin.logD("HOT CODE replace obsolete methods " + tgt);
}




/********************************************************************************/
/*										*/
/*	Console input methods							*/
/*										*/
/********************************************************************************/

void consoleInput(String lname,String input) throws BedrockException
{
   String in1 = IvyXml.decodeXmlString(input);
   
   BedrockPlugin.logD("CONSOLE INPUT " + input + " " + in1);
   
   if (input == null) {
      BedrockPlugin.logE("EMPTY CONSOLE INPUT");
      return;
    }

   ILaunch[] launches = debug_plugin.getLaunchManager().getLaunches();

   try {
      for (ILaunch launch: launches) {
	 if (!matchLaunch(lname,launch)) continue;
	 IDebugTarget dt = launch.getDebugTarget();
	 IProcess ip = dt.getProcess();
	 IStreamsProxy isp = ip.getStreamsProxy();
	 if (isp == null) throw new BedrockException("CONSOLE Streams proxy not supported");
	 isp.write(input);
	 BedrockPlugin.logD("Send to console: " + input);
	 break;
       }
    }
   catch (IOException e) {
      throw new BedrockException("I/O error writing to console",e);
    }
}




/********************************************************************************/
/*										*/
/*	Console management							*/
/*										*/
/********************************************************************************/

private void setupConsole(IProcess ip)
{
   IStreamsProxy isp = ip.getStreamsProxy();
   if (isp == null) {
      BedrockPlugin.logD("CONSOLE Streams proxy not supported");
      return;
    }

   IStreamMonitor ism = isp.getOutputStreamMonitor();
   ism.addListener(new ConsoleListener(ip,false));
   ism = isp.getErrorStreamMonitor();
   ism.addListener(new ConsoleListener(ip,true));
}



void writeToProcess(String pid,String text)
{
   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      try {
	 for (IDebugTarget dt : launch.getDebugTargets()) {
	    IProcess ip = dt.getProcess();
	    if (ip == null) continue;
	    if (pid != null && !matchProcess(pid,ip)) continue;
	    IStreamsProxy isp = ip.getStreamsProxy();
	    isp.write(text);
	  }
       }
      catch (IOException e) { }
    }
}



private class ConsoleListener implements IStreamListener {

   private int process_id;
   private boolean is_stderr;

   ConsoleListener(IProcess ip,boolean err) {
      process_id = ip.hashCode();
      is_stderr = err;
    }

   @Override public void streamAppended(String txt,IStreamMonitor mon) {
      BedrockPlugin.logD("Console output: " + process_id + " " + txt);
      queueConsole(process_id,txt,is_stderr,false);
    }

}	// end of inner class ConsoleListener




private void queueConsole(int pid,String txt,boolean err,boolean eof)
{
   synchronized (console_map) {
      if (console_thread == null) {
	 console_thread = new ConsoleThread();
	 console_thread.start();
       }

      ConsoleData cd = console_map.get(pid);
      if (cd != null) {
	 BedrockPlugin.logD("Console append " + pid + " " + txt.length());
	 cd.addWrite(txt,err,eof);
       }
      else {
	 BedrockPlugin.logD("Console newapp " + pid + " " + (txt == null ? 0 : txt.length()));
	 cd = new ConsoleData();
	 cd.addWrite(txt,err,eof);
	 console_map.put(pid,cd);
	 console_map.notifyAll();
       }
    }
}



private static class ConsoleWrite {

   private String write_text;
   private boolean is_stderr;
   private boolean is_eof;

   ConsoleWrite(String txt,boolean err,boolean eof) {
      write_text = txt;
      is_stderr = err;
      is_eof = eof;
    }

   String getText()			{ return write_text; }
   boolean isStdErr()			{ return is_stderr; }
   boolean isEof()			{ return is_eof; }

}	// end of inner class ConsoleWrite



private class ConsoleData {

   private List<ConsoleWrite> pending_writes;

   ConsoleData() {
      pending_writes = new ArrayList<ConsoleWrite>();
    }

   synchronized void addWrite(String txt,boolean err,boolean eof) {
      pending_writes.add(new ConsoleWrite(txt,err,eof));
    }

   List<ConsoleWrite> getWrites()		{ return pending_writes; }

}	// end of inner class ConsoleData



private class ConsoleThread extends Thread {

   ConsoleThread() {
      super("BedrockConsoleMonitor");
    }

   @Override public void run() {
      for ( ; ; ) {
	 try {
	    ConsoleData cd = null;
	    int pid = 0;
	    synchronized (console_map) {
	       while (console_map.isEmpty()) {
		  try {
		     console_map.wait(10000);
		   }
		  catch (InterruptedException e) { }
		}
	       for (Iterator<Map.Entry<Integer,ConsoleData>> it = console_map.entrySet().iterator(); it.hasNext(); ) {
		  Map.Entry<Integer,ConsoleData> ent = it.next();
		  pid = ent.getKey();
		  cd = ent.getValue();
		  BedrockPlugin.logD("Console thread data " + pid + " " + cd);
		  it.remove();
		  if (cd != null) break;
		}
	     }
	    if (cd != null) processConsoleData(pid,cd);
	  }
	 catch (Throwable t) {
	    BedrockPlugin.logE("Problem with console thread: " + t,t);
	  }
       }
    }

   private void processConsoleData(int pid,ConsoleData cd) {
      StringBuffer buf = null;
      boolean iserr = false;
      for (ConsoleWrite cw : cd.getWrites()) {
	 if (cw.isEof()) {
	    if (buf != null) flushConsole(pid,buf,iserr);
	    buf = null;
	    eofConsole(pid);
	    continue;
	  }
	 if (buf == null) {
	    if (cw.getText() != null) {
	       buf = new StringBuffer();
	       iserr = cw.isStdErr();
	       buf.append(cw.getText());
	     }
	  }
	 else if (iserr == cw.isStdErr()) {
	    if (cw.getText() != null) buf.append(cw.getText());
	  }
	 else {
	    flushConsole(pid,buf,iserr);
	    buf = null;
	    if (cw.getText() != null) {
	       buf = new StringBuffer();
	       iserr = cw.isStdErr();
	       buf.append(cw.getText());
	     }
	  }
	 if (buf != null && buf.length() > 32768) {
	    flushConsole(pid,buf,iserr);
	    buf = null;
	  }
       }
      if (buf != null) flushConsole(pid,buf,iserr);
    }

   private void flushConsole(int pid,StringBuffer buf,boolean iserr) {
      IvyXmlWriter xw = our_plugin.beginMessage("CONSOLE");
      xw.field("PID",pid);
      xw.field("STDERR",iserr);
      //TODO: fix this correctly
      String txt = buf.toString();
      // txt = txt.replace("]]>","] ]>");
      txt = txt.replace("\010"," ");
      if (txt.length() == 0) return;
      xw.cdataElement("TEXT",txt);
      our_plugin.finishMessageWait(xw);
      BedrockPlugin.logD("Console write " + txt.length());
    }

   private void eofConsole(int pid) {
      IvyXmlWriter xw = our_plugin.beginMessage("CONSOLE");
      xw.field("PID",pid);
      xw.field("EOF",true);
      our_plugin.finishMessageWait(xw);
    }

}	// end of innerclass ConsoleThread



/********************************************************************************/
/*										*/
/*	Matching utilities							*/
/*										*/
/********************************************************************************/

private boolean matchThread(String id,IThread ith)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(ith.toString())) return true;
   if (id.equals(Integer.toString(ith.hashCode()))) return true;

   return false;
}


private boolean matchLaunch(String id,ILaunch iln)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(iln.toString())) return true;
   if (id.equals(Integer.toString(iln.hashCode()))) return true;

   return false;
}



private boolean matchDebugTarget(String id,IDebugTarget iln)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(iln.toString())) return true;
   if (id.equals(Integer.toString(iln.hashCode()))) return true;

   return false;
}



private boolean matchProcess(String id,IProcess ipr)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(ipr.toString())) return true;
   if (id.equals(Integer.toString(ipr.hashCode()))) return true;

   return false;
}



private boolean matchFrame(String id,IStackFrame ifr)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(ifr.toString())) return true;
   if (id.equals(Integer.toString(ifr.hashCode()))) return true;

   return false;
}



private boolean matchLaunchConfiguration(String id,ILaunchConfiguration il)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(il.toString())) return true;
   if (id.equals(Integer.toString(System.identityHashCode(il)))) return true;
   if (id.equals(BedrockUtil.getId(il))) return true;
   if (id.equals(il.getName())) return true;

   return false;
}



private boolean matchVariable(String id,IVariable v)
{
   int idx = id.lastIndexOf(".");
   if (idx >= 0) {
      if (v instanceof IJavaFieldVariable) {
	 try {
	    if (id.substring(idx+1).equals(v.getName())) {
	       IJavaFieldVariable jfv = (IJavaFieldVariable) v;
	       String f0 = id.substring(0,idx);
	       String f1 = jfv.getDeclaringType().getName();
	       String f2 = f1;
	       if (f1 != null) f2 = f1.replace("$",".");
	       if (f0.equals(f1) || f0.equals(f2)) return true;
	     }
	  }
	 catch (DebugException e) { }
       }
      return false;
    }

   try {
      if (id.equals(v.getName())) {
	 if (v instanceof IJavaVariable) {
	    IJavaVariable ijv = (IJavaVariable) v;
	    try {
	       if (ijv.isLocal()) return true;
	     }
	    catch (DebugException e) {
	       return false; 
	    }
	  }
	 return true;
       }
    }
   catch (DebugException e) { }

   return false;
}




}	// end of class BedrockRuntime




/* end of BedrockRuntime.java */
