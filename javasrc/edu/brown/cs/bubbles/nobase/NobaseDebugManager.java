/********************************************************************************/
/*										*/
/*		NobaseDebugManager.java 					*/
/*										*/
/*	Debugger interface for node/js						*/
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class NobaseDebugManager implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseMain		nobase_main;
private Map<String,NobaseLaunchConfig> config_map;
private File			config_file;
private File			break_file;
private Map<String,NobaseDebugBreakpoint> break_map;
private Map<String,NobaseDebugTarget> target_map;
private NobaseDebugBreakpoint	exception_breakpoint;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseDebugManager(NobaseMain nm)
{
   nobase_main = nm;
   config_map = new HashMap<>();
   config_file = new File(nobase_main.getWorkSpaceDirectory(),CONFIG_FILE);
   break_map = new ConcurrentHashMap<>();
   break_file = new File(nobase_main.getWorkSpaceDirectory(),BREAKPOINT_FILE);
   target_map = new ConcurrentHashMap<>();
   exception_breakpoint = null;

   loadConfigurations();
   loadBreakpoints();
}



/********************************************************************************/
/*										*/
/*	Configuration setup methods						*/
/*										*/
/********************************************************************************/

void handleLanguageData(IvyXmlWriter xw)
{
   String nm = "resources/launches-js.xml";
   InputStream ins = NobaseDebugManager.class.getClassLoader().getResourceAsStream(nm);
   NobaseMain.logD("Language data " + nm + " " + ins + " " +
         NobaseDebugManager.class.getClassLoader().getResource(nm));
   Element xml = IvyXml.loadXmlFromStream(ins);
   if (xml == null) {
      String nm1 = "launches-java.xml";
      ins = NobaseDebugManager.class.getClassLoader().getResourceAsStream(nm1);
      NobaseMain.logD("Language data " + nm1 + " " + ins + " " +
            NobaseDebugManager.class.getClassLoader().getResource(nm1));
      xml = IvyXml.loadXmlFromStream(ins);
    }
   if (xml == null) {
      ins = NobaseDebugManager.class.getClassLoader().getResourceAsStream(nm);
      try {
	 String txt = IvyFile.loadFile(ins);
	 NobaseMain.logE("BAD language resource file:\n" + txt);
       }
      catch (IOException e) {
	 NobaseMain.logE("Bad language resource read " + e);
       }
    }
   xw.writeXml(xml);
}


void handleLaunchQuery(String proj,String query,boolean opt,IvyXmlWriter xw)
	 throws NobaseException
{
   if (query.equals("START")) {
      if (proj != null) {
	 NobaseProject np = nobase_main.getProject(proj);
	 outputProjectStarts(np,xw);
      }
      else {
	 for (NobaseProject np : nobase_main.getProjectManager().getAllProjects()) {
	    outputProjectStarts(np,xw);
	 }
      }
   }
}



private void outputProjectStarts(NobaseProject np,IvyXmlWriter xw)
{
   for (NobaseFile nf : np.getAllFiles()) {
      xw.begin("OPTION");
      xw.field("VALUE", nf.getFile().getPath());
      xw.end("OPTION");
   }
}


/********************************************************************************/
/*										*/
/*	Run configuration methods						*/
/*										*/
/********************************************************************************/

void getNewRunConfiguration(String proj,String name,String clone,IvyXmlWriter xw)
	throws NobaseException
{
   NobaseLaunchConfig plc = null;
   if (clone != null) {
      NobaseLaunchConfig orig = config_map.get(clone);
      if (orig == null)
	 throw new NobaseException("Configuration to clone not found: " + clone);
      if (name == null) name = getUniqueName(orig.getName());
      plc = new NobaseLaunchConfig(name,orig);
    }
   else {
      if (name == null) name = getUniqueName("Node Launch");
      plc = new NobaseLaunchConfig(name);
    }

   if (plc != null) {
      if (proj != null) plc.setAttribute(NobaseConfigAttribute.PROJECT_ATTR,proj);
      config_map.put(plc.getId(),plc);
      plc.outputBubbles(xw);
      handleLaunchNotify(plc,"ADD");
    }
}


void editRunConfiguration(String id,NobaseConfigAttribute prop,String value,IvyXmlWriter xw)
	throws NobaseException
{
   NobaseLaunchConfig cfg = config_map.get(id);
   if (cfg == null) throw new NobaseException("Launch configuration " + id + " not found");

   cfg = cfg.getWorkingCopy();
   if (prop == NobaseConfigAttribute.NAME) {
      cfg.setName(value);
    }
   else if (prop.equals(NobaseConfigAttribute.MAIN_TYPE)) {
      File f = new File(value);
      cfg.setFileToRun(f);
    }
   else {
      cfg.setAttribute(prop,value);
    }

   if (xw != null) cfg.outputBubbles(xw);

   handleLaunchNotify(cfg,"CHANGE");
}


void saveRunConfiguration(String id,IvyXmlWriter xw) throws NobaseException
{
   NobaseLaunchConfig cfg = config_map.get(id);
   if (cfg == null) throw new NobaseException("Launch configuration " + id + " not found");

   cfg.commitWorkingCopy();

   saveConfigurations();

   if (xw != null) cfg.outputBubbles(xw);

   handleLaunchNotify(cfg,"CHANGE");
}


void deleteRunConfiguration(String id,IvyXmlWriter xw)
{
   NobaseLaunchConfig cfg = config_map.remove(id);
   if (cfg == null) return;
   cfg.setSaved(false);
   saveConfigurations();

   handleLaunchNotify(cfg,"REMOVE");
}



void getRunConfigurations(IvyXmlWriter xw)
{
   for (NobaseLaunchConfig plc : config_map.values()) {
      plc.outputBubbles(xw);
    }
}



private void loadConfigurations()
{
   Element xml = IvyXml.loadXmlFromFile(config_file);
   for (Element le : IvyXml.children(xml,"LAUNCH")) {
      NobaseLaunchConfig plc = new NobaseLaunchConfig(le);
      config_map.put(plc.getId(),plc);
    }
}




/********************************************************************************/
/*										*/
/*	Breakpoint methods							*/
/*										*/
/********************************************************************************/

Iterable<NobaseDebugBreakpoint> getBreakpoints()
{
   return break_map.values();
}




void getAllBreakpoints(IvyXmlWriter xw)
{
    for (NobaseDebugBreakpoint pb : break_map.values()) {
      pb.outputBubbles(xw);
    }
}



void setLineBreakpoint(String proj,String bid,String file,int line,boolean trace)
	throws NobaseException
{
   NobaseFile nf = nobase_main.getFileData(file);
   for (NobaseDebugBreakpoint prev : break_map.values()) {
      if (prev.getType() == BreakType.LINE &&
	    prev.getFile().equals(nf.getFile()) &&
	    prev.getLine() == line) {
	 return;
       }
    }

   NobaseDebugBreakpoint pb = NobaseDebugBreakpoint.createLineBreakpoint(nf,line);
   break_map.put(pb.getId(),pb);
   handleBreakNotify(pb,"ADD");
   addBreakpoint(pb);
   saveBreakpoints();
}


void setExceptionBreakpoint(String proj,
      boolean caught,boolean uncaught,boolean checked)
{
   if (exception_breakpoint == null) {
      exception_breakpoint = NobaseDebugBreakpoint.createExceptionBreakpoint(caught,uncaught);
      break_map.put(exception_breakpoint.getId(),exception_breakpoint);
      handleBreakNotify(exception_breakpoint,"ADD");
      addBreakpoint(exception_breakpoint);
    }
   else {
      exception_breakpoint.setProperty("CAUGHT",Boolean.toString(caught));
      exception_breakpoint.setProperty("UNCAUGHT",Boolean.toString(uncaught));
      handleBreakNotify(exception_breakpoint,"CHANGE");
      removeBreakpoint(exception_breakpoint);
      addBreakpoint(exception_breakpoint);
    }
   NobaseDebugBreakpoint pb = NobaseDebugBreakpoint.createExceptionBreakpoint(caught,uncaught);
   break_map.put(pb.getId(),pb);
   saveBreakpoints();
   handleBreakNotify(pb,"ADD");
   addBreakpoint(pb);

   saveBreakpoints();
}


void editBreakpoint(String id,String ... pv)
	throws NobaseException
{
   NobaseDebugBreakpoint bp = break_map.get(id);
   if (bp == null) throw new NobaseException("Breakpoint " + id + " not found");

   for (int i = 0; i < pv.length; i += 2) {
      String p = pv[i];
      String v = pv[i+1];
      if (p == null) continue;
      if (p.equals("CLEAR")) {
	 bp.clear();
	 break_map.remove(id);
	 if (bp == exception_breakpoint) exception_breakpoint = null;
	 removeBreakpoint(bp);
	 handleBreakNotify(bp,"REMOVE");
	 break;
       }
      else bp.setProperty(p,v);
    }

   removeBreakpoint(bp);
   addBreakpoint(bp);
   handleBreakNotify(bp,"CHANGE");
}




void clearLineBreakpoints(String proj,String file,int line)
{
   NobaseFile nf = null;
   if (file != null) nf = nobase_main.getFileData(file);

   boolean chng = false;
   for (Iterator<NobaseDebugBreakpoint> it = break_map.values().iterator(); it.hasNext(); ) {
      NobaseDebugBreakpoint bp = it.next();
      if (bp.getType() == BreakType.LINE) {
	 if (nf == null || bp.getFile().equals(nf.getFile())) {
	    if (line <= 0 || line <= bp.getLine()) {
	       it.remove();
	       removeBreakpoint(bp);
	       handleBreakNotify(bp,"REMOVE");
	       chng = true;
	     }
	  }
       }
    }
   if (chng) saveBreakpoints();
}



private void loadBreakpoints()
{
   Element xml = IvyXml.loadXmlFromFile(config_file);
   if (xml == null) {
      exception_breakpoint = NobaseDebugBreakpoint.createExceptionBreakpoint(false,true);
      break_map.put(exception_breakpoint.getId(),exception_breakpoint);
    }
   else {
      for (Element be : IvyXml.children(xml,"BREAKPOINT")) {
	 try {
	    NobaseDebugBreakpoint pb = NobaseDebugBreakpoint.createBreakpoint(be);
	    if (pb == null) continue;
	    switch (pb.getType()) {
	       case NONE :
		  break;
	       case EXCEPTION :
		  if (!pb.isCaught() && !pb.isUncaught()) {
		     if (exception_breakpoint != null) {
			break_map.remove(exception_breakpoint.getId());
			exception_breakpoint = null;
		      }
		     pb = null;
		   }
		  else if (exception_breakpoint == null) {
		     exception_breakpoint = pb;
		   }
		  else {
		     exception_breakpoint.setProperty("CAUGHT",Boolean.toString(pb.isCaught()));
		     exception_breakpoint.setProperty("UNCAUGHT",Boolean.toString(pb.isUncaught()));
		     pb = null;
		   }
		  break;
	       case LINE :
		  for (NobaseDebugBreakpoint prev : break_map.values()) {
		     if (prev.getType() == BreakType.LINE &&
			   prev.getFile().equals(pb.getFile()) &&
			   prev.getLine() == pb.getLine()) {
			 pb = null;
			 break;
		      }
		   }
		  break;
	     }
	    if (pb != null) {
	       break_map.put(pb.getId(),pb);
	     }
	  }
	 catch (NobaseException e) {
	    NobaseMain.logE("Breakpoint not found: " + IvyXml.convertXmlToString(xml),e);
	  }
       }
    }
}



private void saveBreakpoints()
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(break_file);
      xw.begin("BREAKPOINTS");
      for (NobaseDebugBreakpoint pb : break_map.values()) {
	 pb.outputXml(xw);
       }
      xw.end("BREAKPOINTS");
      xw.close();
    }
   catch (IOException e) {
      NobaseMain.logE("Problem writing out breakpoints",e);
    }
}



private void handleBreakNotify(NobaseDebugBreakpoint pb,String reason)
{
   IvyXmlWriter xw = nobase_main.beginMessage("BREAKEVENT");
   xw.begin("BREAKPOINTS");
   xw.field("REASON",reason);
   pb.outputBubbles(xw);
   xw.end("BREAKPOINTS");
   nobase_main.finishMessage(xw);
}



private void addBreakpoint(NobaseDebugBreakpoint bpt)
{
   for (NobaseDebugTarget tgt : target_map.values()) {
      tgt.addBreakpointInRuntime(bpt);
    }
}


private void removeBreakpoint(NobaseDebugBreakpoint bpt)
{
    for (NobaseDebugTarget tgt : target_map.values()) {
      tgt.breakpointRemoved(bpt);
    }
}


/********************************************************************************/
/*										*/
/*	Debugger action methods 						*/
/*										*/
/********************************************************************************/

void runProject(String configid,IvyXmlWriter xw) throws NobaseException
{
   NobaseLaunchConfig cfg = config_map.get(configid);
   if (cfg == null) throw new NobaseException("Configuration not found");

   NobaseDebugTarget tgt = new NobaseDebugTarget(this,cfg);
   target_map.put(tgt.getId(),tgt);
   tgt.startDebug();

   xw.begin("LAUNCH");
   xw.field("MODE","debug");
   xw.field("ID",tgt.getId());
   xw.field("CID",cfg.getId());
   xw.field("TARGET",tgt.getId());
   xw.field("PROCESS",tgt.getId());
   xw.end("LAUNCH");
}


void debugAction(String launchid,String targetid,String frameid,
      NobaseDebugAction action,IvyXmlWriter xw)
	throws NobaseException
{
   NobaseDebugTarget tgt = null;
   if (launchid != null) tgt = target_map.get(launchid);
   if (tgt == null && targetid != null) tgt = target_map.get(targetid);
   if (tgt == null) throw new NobaseException("Target not found");

   boolean ok = true;
   switch (action) {
      case NONE :
	 break;
      case TERMINATE :
	 if (tgt.canTerminate()) tgt.terminate();
	 else ok = false;
	 break;
      case SUSPEND :
	 if (tgt.canSuspend()) tgt.suspend();
	 else ok = false;
	 break;
      case RESUME :
	 if (tgt.canResume()) tgt.resume();
	 else ok = false;
	 break;
      case STEP_INTO :
	 if (tgt.canResume()) tgt.stepInto();
	 else ok = false;
	 break;
      case STEP_OVER :
	 if (tgt.canResume()) tgt.stepOver();
	 else ok = false;
	 break;
      case STEP_RETURN :
	 if (tgt.canResume()) tgt.stepReturn();
	 else ok = false;
	 break;
      case DROP_TO_FRAME :
	 if (tgt.canDropToFrame()) tgt.dropToFrame(null);
	 else ok = false;
	 break;
    }
   if (ok) xw.textElement("TARGET",tgt.getId());
}


void consoleInput(String launch,String input)
{

}




/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

void getStackFrames(String launchid,int count,int depth,IvyXmlWriter xw)
	throws NobaseException
{
   xw.begin("STACKFRAMES");
   for (NobaseDebugTarget tgt : target_map.values()) {
      if (launchid != null && !tgt.getId().equals(launchid)) continue;
      NobaseDebugThread thrd = tgt.findThreadById(null);
      if (thrd != null) {
	 xw.begin("THREAD");
	 xw.field("NAME",thrd.getName());
	 xw.field("ID",thrd.getLocalId());
	 xw.field("TARGET",tgt.getId());
	 int ctr = 0;
	 for (NobaseDebugStackFrame frm : thrd.getStackFrames()) {
	    if (frm == null) continue;
	    frm.outputXml(xw,ctr,depth);
	    if (count > 0 && ctr > count) break;
	    ++ctr;
	  }
	 xw.end("THREAD");
       }
    }
   xw.end("STACKFRAMES");
}


void getVariableValue(String frame,String var,int depth,IvyXmlWriter xw)
{

}


void evaluateExpression(String proj,String bid,String expr,String thread,
      String frame,boolean implicit,boolean stop,String eid,
      IvyXmlWriter xw)
   throws NobaseException
{
   boolean done = false;
   int fidx = -1;
   if (frame != null) {
      fidx = Integer.parseInt(frame);
   }

   for (NobaseDebugTarget tgt : target_map.values()) {
      NobaseDebugThread thrd = tgt.findThreadById(thread);
      if (thrd != null) {
	 for (NobaseDebugStackFrame frm : thrd.getStackFrames()) {
	    if (frm == null) continue;
	    if (fidx < 0 || fidx == frm.getIndex()) {
	       tgt.evaluateExpression(bid,eid,expr,frm.getIndex(),stop);
	       done = true;
	       break;
	     }
	  }
       }
      if (done) break;
    }

  if (!done) throw new NobaseException("No evaluation to do");
}




/********************************************************************************/
/*										*/
/*	Control methods 							*/
/*										*/
/********************************************************************************/

File getNodeBinary()
{
   return new File("node");
}


int getServerPort()
{
   return 5858;
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private String getUniqueName(String nm)
{
   int idx = nm.lastIndexOf("(");
   if (idx >= 0) nm = nm.substring(0,idx).trim();
   for (int i = 1; ; ++i) {
      String nnm = nm + " (" + i + ")";
      boolean fnd = false;
      for (NobaseLaunchConfig plc : config_map.values()) {
	 if (plc.getName().equals(nnm)) fnd = true;
       }
      if (!fnd) return nnm;
    }
}



private void handleLaunchNotify(NobaseLaunchConfig plc,String reason)
{
   IvyXmlWriter xw = nobase_main.beginMessage("LAUNCHCONFIGEVENT");
   xw.begin("LAUNCH");
   xw.field("REASON",reason);
   xw.field("ID",plc.getId());
   if (plc != null) plc.outputBubbles(xw);
   xw.end("LAUNCH");
   nobase_main.finishMessage(xw);
}



private void saveConfigurations()
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(config_file);
      xw.begin("CONFIGS");
      for (NobaseLaunchConfig plc : config_map.values()) {
	 if (plc.isSaved()) {
	    plc.outputSaveXml(xw);
	  }
       }
      xw.end("CONFIGS");
      xw.close();
    }
   catch (IOException e) {
      NobaseMain.logE("Problem writing out configurations",e);
    }
}

}	// end of class NobaseDebugManager




/* end of NobaseDebugManager.java */

