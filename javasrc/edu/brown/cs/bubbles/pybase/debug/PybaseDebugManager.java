/********************************************************************************/
/*										*/
/*		PybaseDebugManager.java 					*/
/*										*/
/*	Manager for debugger configurations and information			*/
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PybaseDebugManager implements PybaseDebugConstants, PybaseConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private PybaseMain				pybase_main;
private Map<String,PybaseLaunchConfig>		config_map;
private File					config_file;
private Map<String,PybaseDebugBreakpoint>	break_map;
private File					break_file;
private Map<String,PybaseDebugger>		debug_map;

private static PybaseDebugManager		the_manager;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static PybaseDebugManager getManager()
{
   if (the_manager == null) {
      the_manager = new PybaseDebugManager();
    }
   return the_manager;
}


private PybaseDebugManager()
{
   pybase_main = PybaseMain.getPybaseMain();

   config_map = new ConcurrentHashMap<String,PybaseLaunchConfig>();
   config_file = new File(pybase_main.getWorkSpaceDirectory(),CONFIG_FILE);
   break_map = new ConcurrentHashMap<String,PybaseDebugBreakpoint>();
   break_file = new File(pybase_main.getWorkSpaceDirectory(),BREAKPOINT_FILE);
   debug_map = new ConcurrentHashMap<String,PybaseDebugger>();

   loadConfigurations();
   loadBreakpoints();
}




/********************************************************************************/
/*										*/
/*	Methods to load/store configurations					*/
/*										*/
/********************************************************************************/

private void loadConfigurations()
{
   Element xml = IvyXml.loadXmlFromFile(config_file);
   for (Element le : IvyXml.children(xml,"LAUNCH")) {
      PybaseLaunchConfig plc = new PybaseLaunchConfig(le);
      config_map.put(plc.getId(),plc);
    }
}


private void saveConfigurations()
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(config_file);
      xw.begin("CONFIGS");
      for (PybaseLaunchConfig plc : config_map.values()) {
	 if (plc.isSaved()) {
	    plc.outputSaveXml(xw);
	  }
       }
      xw.end("CONFIGS");
      xw.close();
    }
   catch (IOException e) {
      PybaseMain.logE("Problem writing out configurations",e);
    }
}



/********************************************************************************/
/*										*/
/*	Configuration management methods					*/
/*										*/
/********************************************************************************/

public void getRunConfigurations(IvyXmlWriter xw)
{
   for (PybaseLaunchConfig plc : config_map.values()) {
      plc.outputBubbles(xw);
    }
}


public void getNewRunConfiguration(String proj,String nm,String clone,IvyXmlWriter xw)
   throws PybaseException
{
   PybaseLaunchConfig plc = null;
   if (clone != null) {
      PybaseLaunchConfig orig = config_map.get(clone);
      if (orig == null) throw new PybaseException("Configuration to clone not found: " + clone);
      if (nm == null) nm = getUniqueName(orig.getName());
      plc = new PybaseLaunchConfig(nm,orig);
    }
   else {
      if (nm == null) nm = getUniqueName("Python Launch");
      plc = new PybaseLaunchConfig(nm);
    }

   if (plc != null) {
      if (proj != null) plc.setAttribute(ATTR_PROJECT,proj);
      config_map.put(plc.getId(),plc);
      plc.outputBubbles(xw);
      handleLaunchNotify(plc,"ADD");
    }
}



private String getUniqueName(String nm)
{
   int idx = nm.lastIndexOf("(");
   if (idx >= 0) nm = nm.substring(0,idx).trim();
   for (int i = 1; ; ++i) {
      String nnm = nm + " (" + i + ")";
      boolean fnd = false;
      for (PybaseLaunchConfig plc : config_map.values()) {
	 if (plc.getName().equals(nnm)) fnd = true;
       }
      if (!fnd) return nnm;
    }
}



public void editRunConfiguration(String lid,String prop,String val,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseLaunchConfig cfg = config_map.get(lid);
   if (cfg == null) throw new PybaseException("Launch configuration " + lid + " not found");

   cfg = cfg.getWorkingCopy();
   if (prop.equals("NAME")) {
      cfg.setName(val);
    }
   else if (prop.equals("FILE")) {
      File f = new File(val);
      cfg.setFileToRun(f);
    }
   else {
      cfg.setAttribute(prop,val);
    }

   if (xw != null) cfg.outputBubbles(xw);

   handleLaunchNotify(cfg,"CHANGE");
}



public void saveRunConfiguration(String lid,IvyXmlWriter xw) throws PybaseException
{
   PybaseLaunchConfig cfg = config_map.get(lid);
   if (cfg == null) throw new PybaseException("Launch configuration " + lid + " not found");

   cfg.commitWorkingCopy();

   saveConfigurations();

   if (xw != null) cfg.outputBubbles(xw);

   handleLaunchNotify(cfg,"CHANGE");
}


public void deleteRunConfiguration(String lid,IvyXmlWriter xw) throws PybaseException
{
   PybaseLaunchConfig cfg = config_map.remove(lid);
   if (cfg == null) return;
   cfg.setSaved(false);
   saveConfigurations();

   handleLaunchNotify(cfg,"REMOVE");
}



private void handleLaunchNotify(PybaseLaunchConfig plc,String reason)
{
   IvyXmlWriter xw = pybase_main.beginMessage("LAUNCHCONFIGEVENT");
   xw.begin("LAUNCH");
   xw.field("REASON",reason);
   xw.field("ID",plc.getId());
   if (!reason.equals("REMOVE")) plc.outputBubbles(xw);
   xw.end("LAUNCH");
   pybase_main.finishMessage(xw);
}



/********************************************************************************/
/*										*/
/*	Load and save breakpoints						*/
/*										*/
/********************************************************************************/

private void loadBreakpoints()
{
   Element xml = IvyXml.loadXmlFromFile(config_file);
   if (xml == null) {
      PybaseDebugBreakpoint bp;
      bp = PybaseDebugBreakpoint.createExceptionBreakpoint("Exception",true,true);
      break_map.put(bp.getId(),bp);
    }
   else {
      for (Element be : IvyXml.children(xml,"BREAKPOINT")) {
	 try {
	    PybaseDebugBreakpoint pb = PybaseDebugBreakpoint.createBreakpoint(be);
	    break_map.put(pb.getId(),pb);
	  }
	 catch (PybaseException e) {
	    PybaseMain.logE("Breakpoint not found: " + IvyXml.convertXmlToString(xml),e);
	  }
       }
    }
}


private void saveBreakpoints()
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(break_file);
      xw.begin("BREAKPOINTS");
      for (PybaseDebugBreakpoint pb : break_map.values()) {
	 pb.outputXml(xw);
       }
      xw.end("BREAKPOINTS");
      xw.close();
    }
   catch (IOException e) {
      PybaseMain.logE("Problem writing out breakpoints",e);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to manage breakpoints						*/
/*										*/
/********************************************************************************/

public void getAllBreakpoints(IvyXmlWriter xw)
{
   for (PybaseDebugBreakpoint pb : break_map.values()) {
      pb.outputBubbles(xw);
    }
}


List<PybaseDebugBreakpoint> getBreakpoints()
{
   return new ArrayList<PybaseDebugBreakpoint>(break_map.values());
}



public void setLineBreakpoint(String proj,String id,String file,int line,boolean susvm,boolean trace)
	throws PybaseException
{
   IFileData fd = pybase_main.getFileData(file);
   if (fd == null) throw new PybaseException("File " + file + " not found");
   PybaseDebugBreakpoint pb = PybaseDebugBreakpoint.createLineBreakpoint(fd,line);
   break_map.put(pb.getId(),pb);
   saveBreakpoints();
   handleBreakNotify(pb,"ADD");
}


public void setExceptionBreakpoint(String proj,String exc,boolean caught,
				      boolean uncaught,boolean checked,
				      boolean susvm)
	throws PybaseException
{
   PybaseDebugBreakpoint pb = PybaseDebugBreakpoint.createExceptionBreakpoint(exc, caught, uncaught);
   break_map.put(pb.getId(),pb);
   saveBreakpoints();
   handleBreakNotify(pb,"ADD");
}



public void editBreakpoint(String id,String p1,String v1,String p2,String v2,String p3,String v3)
{
}


public void clearLineBreakpoints(String proj,String file,int line)
{
}



private void handleBreakNotify(PybaseDebugBreakpoint pb,String reason)
{
   IvyXmlWriter xw = pybase_main.beginMessage("BREAKEVENT");
   xw.begin("BREAKPOINTS");
   xw.field("REASON",reason);
   pb.outputBubbles(xw);
   xw.end("BREAKPOINTS");
   pybase_main.finishMessage(xw);
}



/********************************************************************************/
/*										*/
/*	Methods to handle launches						*/
/*										*/
/********************************************************************************/

public void runProject(String id,IvyXmlWriter xw) throws PybaseException
{
   PybaseLaunchConfig plc = config_map.get(id);
   if (plc == null) throw new PybaseException("Launch configuration " + id + " not found");

   PybaseDebugger pd = null;

   try {
      pd = PybaseRunner.runDebug(plc);
      debug_map.put(pd.getId(),pd);
    }
   catch (IOException e) {
      throw new PybaseException("Problem starting debugger",e);
    }

   xw.begin("LAUNCH");
   xw.field("MODE","debug");
   xw.field("ID",pd.getId());
   xw.field("CID",pd.getLaunchConfig().getId());
   PybaseDebugTarget dt = pd.getTarget();
   if (dt != null) {
      xw.field("TARGET",dt.getId());
      xw.field("PROCESS",dt.getId());
    }
   xw.end("LAUNCH");
}



/********************************************************************************/
/*										*/
/*	Debugger actions							*/
/*										*/
/********************************************************************************/

public void debugAction(String lid,String tgtid,String pid,String tid,String fid,
      PybaseDebugAction act,IvyXmlWriter xw) throws PybaseException
{
   for (PybaseDebugger pd : debug_map.values()) {
      if (lid != null && !pd.getId().equals(lid)) continue;
      if (tgtid == null && pid== null && tid == null) {
	 if (doAction(pd,act)) {
	    xw.textElement("LAUNCH",act.toString());
	    continue;
	  }
       }

      PybaseDebugTarget pdt = pd.getTarget();
      if (pdt != null) {
	 if (tgtid != null && !pdt.getId().equals(tgtid)) return;
	 if (tid == null) {
	    if (doAction(pdt,act)) {
	       xw.textElement("TARGET",act.toString());
	       return;
	     }
	  }
	 try {
	    for (PybaseDebugThread thr : pdt.getThreads()) {
	       if (!thr.getLocalId().equals(tid)) continue;
	       if (!thr.isSuspended() && act != PybaseDebugAction.SUSPEND) return;
	       if (doAction(thr,fid,act)) {
		  xw.textElement("THREAD",act.toString());
		}
	     }
	  }
	 catch (PybaseException e) { }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Console input handling							*/
/*										*/
/********************************************************************************/

public void consoleInput(String lid,String txt) throws PybaseException
{
   for (PybaseDebugger pd : debug_map.values()) {
      if (lid != null && !pd.getId().equals(lid)) continue;
      PybaseDebugTarget tgt = pd.getTarget();
      if (tgt != null) {
	 try {
	    tgt.consoleInput(txt);
	  }
	 catch (IOException e) {
	    throw new PybaseException("Problem with console output",e);
	  }
       }
    }
}




private boolean doAction(PybaseDebugger pd,PybaseDebugAction act)
{
   boolean isdone = false;

   switch (act) {
      case NONE :
	 isdone = true;
	 break;
      case TERMINATE :
	 PybaseDebugTarget dt = pd.getTarget();
	 if (dt != null && dt.canTerminate()) {
	    dt.terminate();
	    isdone = true;
	  }
	 break;
      default:
	 break;
    }

   return isdone;
}


private boolean doAction(PybaseDebugTarget pdt,PybaseDebugAction act) throws PybaseException
{
   switch (act) {
      case NONE :
	 break;
      case TERMINATE :
	 if (pdt.canTerminate()) pdt.terminate();
	 else return false;
	 break;
      case SUSPEND :
	 if (pdt.canSuspend()) pdt.suspend();
	 else return false;
	 break;
      case RESUME :
	 if (pdt.canResume()) pdt.resume();
	 else return false;
	 break;
      default :
	 return false;
    }

   return true;
}


private boolean doAction(PybaseDebugThread thr,String fid,PybaseDebugAction act)
{
   switch (act) {
      case NONE :
	 return false;
      case TERMINATE :
	 if (thr.canTerminate()) thr.terminate();
	 else return false;
	 break;
      case RESUME :
	 if (thr.canResume()) thr.resume();
	 else return false;
	 break;
      case SUSPEND :
	 if (thr.canSuspend()) thr.suspend();
	 else return false;
	 break;
      case STEP_INTO :
	 if (thr.canStepInto()) thr.stepInto();
	 else return false;
	 break;
      case STEP_OVER :
	 if (thr.canStepOver()) thr.stepOver();
	 else return false;
	 break;
      case DROP_TO_FRAME :
	 PybaseDebugStackFrame frm = thr.getTopStackFrame();
	 if (fid != null) {
	    for (PybaseDebugStackFrame f1 : thr.getStackFrames()) {
	       if (f1.getId().equals(fid)) frm = f1;
	     }
	  }
	 PybaseMain.logD("No support for drop to frame for " + frm);
	 return false;
      default:
	 break;
    }

   return true;
}



public void getStackFrames(String lid,String tid,int cnt,int dep,IvyXmlWriter xw)
{
   xw.begin("STACKFRAMES");

   for (PybaseDebugger pd : debug_map.values()) {
      if (lid != null && !pd.getId().equals(lid)) continue;
      PybaseDebugTarget tgt = pd.getTarget();
      if (tgt != null) {
	 PybaseDebugThread thr = tgt.findThreadById(tid);
	 xw.begin("THREAD");
	 xw.field("NAME",thr.getName());
	 xw.field("ID",thr.getLocalId());
	 int ctr = 0;
	 for (PybaseDebugStackFrame frm : thr.getStackFrames()) {
	    if (frm == null) continue;
	    frm.outputXml(xw,ctr);
	    if (cnt > 0 && ctr > cnt) break;
	    ++ctr;
	  }
	 xw.end("THREAD");
       }
    }

   xw.end("STACKFRAMES");
}


public void getVariableValue(String tid,String fid,String var,int depth,IvyXmlWriter xw)
{}


public void evaluateExpression(String pid,String bid,String expr,String tid,String fid,
      boolean implicit,boolean dobreak,String rid,IvyXmlWriter xw)
{
   for (PybaseDebugger pd : debug_map.values()) {
      if (pid != null && !pd.getId().equals(pid)) continue;
      PybaseDebugTarget tgt = pd.getTarget();
      if (tgt != null) {
	 tgt.evaluateExpression(bid,rid,expr,null);
       }
    }
}






}	// end of class PybaseDebugManager




/* end of PybaseDebugManager.java */
