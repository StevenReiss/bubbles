/********************************************************************************/
/*										*/
/*		BicexExecution.java						*/
/*										*/
/*	Container for a single execution view					*/
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



package edu.brown.cs.bubbles.bicex;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bicex.BicexConstants.BicexRunner;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpLaunch;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProcess;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThread;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStack;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadState;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadType;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;

class BicexExecution implements BicexConstants, BicexRunner
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		exec_id;
private String		project_name;
private boolean 	auto_restart;
private BicexEvaluationResult exec_result;
private BicexEvaluationContext current_context;
private BicexEvaluationContext saved_context;
private long		current_time;
private long		line_start_time;
private long		line_end_time;
private BicexOutputModel	output_model;
private BicexGraphicsModel	graphics_model;
private SwingEventListenerList<BicexEvaluationUpdated> update_listeners;
private int		last_counter;
private int		working_counter;
private Set<File>	added_files;

private static AtomicInteger id_counter = new AtomicInteger((int)(Math.random()*256000.0));



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexExecution(BumpProcess bp) throws BicexException
{
   exec_id = "BICEX_" + IvyExecQuery.getProcessId() + "_" + id_counter.incrementAndGet();

   auto_restart = false;
   exec_result = new BicexEvaluationResult(this);
   update_listeners = new SwingEventListenerList<>(BicexEvaluationUpdated.class);
   BumpLaunch bl = bp.getLaunch();
   String launchid = bl.getId();
   project_name = bl.getConfiguration().getProject();
   current_time = -1;
   line_start_time = -1;
   line_end_time = -1;
   current_context = null;
   saved_context = null;
   output_model = new BicexOutputModel();
   graphics_model = new BicexGraphicsModel();
   last_counter = 0;
   working_counter = 0;
   added_files = new HashSet<>();

   String threadid = null;
   for (BumpThread bt : bp.getThreads()) {
      if (bt.getThreadState() == BumpThreadState.STOPPED &&
	    (bt.getThreadType() == BumpThreadType.USER || bt.getThreadType() == BumpThreadType.UI)) {
	 BumpThreadStack stk = bt.getStack();
	 if (stk == null || stk.getNumFrames() == 0) continue;
	 String cls = stk.getFrame(0).getFrameClass();
	 if (cls == null || cls.startsWith("java.") || cls.startsWith("sun.") ||
	       cls.startsWith("org.w3c.")) continue;
	 if (threadid == null) threadid = bt.getId();
	 else threadid += " " + bt.getId();
       }
    }
   if (threadid == null) throw new BicexException("No thread found");

   CommandArgs args = new CommandArgs("TYPE","LAUNCH",
	 "LAUNCHID",launchid,"THREADID",threadid);

   Element rslt = sendSeedeMessage("BEGIN",args,null);
   if (!IvyXml.isElement(rslt,"RESULT")) throw new BicexException("Failed to create session");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String	getExecId()			{ return exec_id; }

@Override public BicexEvaluationResult getEvaluation()		{ return exec_result; }
BicexOutputModel getOutputModel()				{ return output_model; }
BicexGraphicsModel getGraphicsModel()				{ return graphics_model; }


@Override public void addUpdateListener(BicexEvaluationUpdated upd)
{
   update_listeners.add(upd);
}

@Override public void removeUpdateListener(BicexEvaluationUpdated upd)
{
   update_listeners.remove(upd);
}

long getCurrentTime()				{ return current_time; }
long getLineEndTime()				{ return line_end_time; }


boolean isCurrentLine(long when)
{
   if (line_start_time < 0) return false;
   if (when >= line_start_time && when < line_end_time) return true;
   return false;
}

BicexEvaluationContext getCurrentContext()	{ return current_context; }




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override public void startContinuousExecution() throws BicexException
{
   auto_restart = true; 		// CONTINUOUS supported for now

   CommandArgs args = new CommandArgs("EXECID",exec_id,"CONTINUOUS",true);
   args.put("MAXTIME",10000000);
   args.put("MAXDEPTH",750);
   Element rslt = sendSeedeMessage("EXEC",args,null);
   if (!IvyXml.isElement(rslt,"RESULT")) {
      throw new BicexException("Failed to start session: " + IvyXml.convertXmlToString(rslt));
    }
}



@Override public void startExecution() throws BicexException
{
   auto_restart = false;
   CommandArgs args = new CommandArgs("EXECID",exec_id,"CONTINUOUS",false);
   Element rslt = sendSeedeMessage("EXEC",args,null);
   if (!IvyXml.isElement(rslt,"RESULT")) throw new BicexException("Failed to start session");
}



@Override public void addFiles(Collection<File> files)
{
   if (files == null || files.isEmpty()) return;

   StringBuffer buf = new StringBuffer();
   int ct = 0;
   for (File f : files) {
      if (added_files.add(f)) {
	 buf.append("<FILE NAME='");
	 buf.append(f.getAbsolutePath());
	 buf.append("' />");
	 ++ct;
       }
    }

   if (ct > 0) {
      sendSeedeMessage("ADDFILE",null,buf.toString());
    }
}


void handleEditorAdded(BudaBubble bw)
{
   for (BicexEvaluationUpdated upd : update_listeners) {
      upd.editorAdded(bw);
    }
}


@Override public void remove()
{
   sendSeedeMessage("REMOVE",null,null);
   update_listeners = new SwingEventListenerList<>(BicexEvaluationUpdated.class);
}



/********************************************************************************/
/*										*/
/*	Result methods								*/
/*										*/
/********************************************************************************/

void handleResult(Element xml) throws BicexException
{
   synchronized (this) {
      int idx = IvyXml.getAttrInt(xml, "INDEX");
      if (idx < last_counter) return;
      last_counter = idx;
      while (working_counter > 0) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
      if (idx < last_counter) return;
      working_counter = idx;
    }

   try {
      if (IvyXml.getAttrBool(xml,"EMPTY")) {
         BoardMetrics.noteCommand("BICEX","EmptyResult");
	 if (!auto_restart) startExecution();
	 return;
       }
      BoardLog.logD("BICEX","Received execution result from seede");
      BoardLog.logD("BICEX","Execution result: " + IvyXml.convertXmlToString(xml));

      Element cnts = IvyXml.getChild(xml,"CONTENTS");
      if (cnts == null) cnts = xml;
 
      boolean errfg = IvyXml.getAttrBool(cnts,"ERROR");
      boolean complete = IvyXml.getAttrBool(cnts,"COMPLETE");
      
      BoardMetrics.noteCommand("BICEX","Result",errfg,complete,last_counter,
            IvyXml.getAttrLong(cnts,"TICKS"),
            IvyXml.getAttrLong(cnts,"EXECTIME"));
      
      exec_result.update(cnts);
      output_model.update(cnts);
      graphics_model.update(cnts,errfg,complete);

      if (current_context == null) current_context = exec_result.getRootContext();
      else {
	 if (saved_context != null) {
	    BicexEvaluationContext nctx = findMatchingContext(saved_context);
	    if (nctx == null) {
	       current_context = findMatchingContext(current_context);
	     }
	    else if (!nctx.getMethod().equals(saved_context.getMethod())) {
	       if (!errfg) 
	          saved_context = null;
	       current_context = nctx;
	     }
	    else current_context = nctx;
	  }
	 else {
	    current_context = findMatchingContext(current_context);
	  }
       }
      if (current_context == null) return;
      current_time = current_context.getEndTime();

      for (BicexEvaluationUpdated upd : update_listeners) {
	 try {
	    upd.evaluationUpdated(this);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BICEX","Problem updating execution",t);
	  }
       }
    }
   finally {
      synchronized (this) {
	 working_counter = 0;
       }
    }
}




void handleReset()
{
   exec_result.reset();
   for (BicexEvaluationUpdated upd : update_listeners) {
      upd.evaluationReset(this);
    }
}


String handleInput(String file)
{
   String rslt = null;

   for (BicexEvaluationUpdated upd : update_listeners) {
      rslt = upd.inputRequest(this,file);
      if (rslt != null) break;
    }

   return rslt;
}


String handleInitialValue(String what)
{
   // needs to return an XML-based value
   String rslt = null;

   for (BicexEvaluationUpdated upd : update_listeners) {
      rslt = upd.valueRequest(this,what);
      if (rslt != null) break;
    }

   return rslt;
}



private BicexEvaluationContext findMatchingContext(BicexEvaluationContext ctx)
{
   if (exec_result == null) return null;

   if (ctx.getParent() == null) return exec_result.getRootContext();
   BicexEvaluationContext pctx = findMatchingContext(ctx.getParent());

   String mthd = ctx.getMethod();
   int idx = 0;
   for (BicexEvaluationContext ctxt : ctx.getParent().getInnerContexts()) {
      if (ctxt == ctx) break;
      else if (ctxt.getMethod().equals(mthd)) ++idx;
    }
   if (pctx.getInnerContexts() != null) {
      for (BicexEvaluationContext nctx : pctx.getInnerContexts()) {
	 if (nctx.getMethod().equals(mthd)) {
	    if (idx-- == 0) return nctx;
	  }
       }
    }

   return pctx;
}




/********************************************************************************/
/*										*/
/*	Context update methods							*/
/*										*/
/********************************************************************************/

void setCurrentContext(BicexEvaluationContext ctx)
{
   if (current_context == ctx) return;
   current_context = ctx;
   if (ctx == null) return;
   saved_context = ctx;

   for (BicexEvaluationUpdated upd : update_listeners) {
      upd.contextUpdated(this);
    }
}



void setCurrentTime(long t)
{
   if (current_time == t) return;
   current_time = t;
   
   BoardMetrics.noteCommand("BICEX","ChangeTime"); 

   if (line_start_time < 0 || t < line_start_time || t >= line_end_time) {
      line_start_time = -1;
      line_end_time = -1;
      if (current_context != null) {
	 BicexValue bv = current_context.getValues().get("*LINE*");
	 if (bv != null) {
	    List<Integer> times = bv.getTimeChanges();
	    for (Integer t0 : times) {
	       if (t0 <= current_time) line_start_time = t0;
	       else {
		  line_end_time = t0;
		  break;
		}
	     }
	  }
       }
    }

   for (BicexEvaluationUpdated upd : update_listeners) {
      upd.timeUpdated(this);
    }
}



/********************************************************************************/
/*										*/
/*	Send messages for this execution					*/
/*										*/
/********************************************************************************/

Element sendSeedeMessage(String cmd,CommandArgs args,String cnts)
{
   if (args == null) args = new CommandArgs();
   args.put("PROJECT",project_name);

   BicexFactory bf = BicexFactory.getFactory();
   return bf.sendSeedeMessage(exec_id,cmd,args,cnts);
}




}	// end of class BicexExecution




/* end of BicexExecution.java */

