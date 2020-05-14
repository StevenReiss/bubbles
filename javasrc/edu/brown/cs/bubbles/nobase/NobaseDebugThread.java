/********************************************************************************/
/*										*/
/*		NobaseDebugThread.java						*/
/*										*/
/*	Information for the current javascript thread				*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class NobaseDebugThread implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		thread_id;
private boolean 	is_running;
private boolean 	is_terminated;
private DebugDetailValue continue_reason;
private List<NobaseDebugStackFrame> cur_stack;

private NobaseDebugTarget for_target;

private static IdCounter       thread_counter = new IdCounter();


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseDebugThread(NobaseDebugTarget tgt)
{
   for_target = tgt;
   is_running = false;
   is_terminated = false;
   continue_reason = null;
   cur_stack = null;

   thread_id = "THREAD_" + thread_counter.nextValue();
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean canResume()
{
   return !is_running && !is_terminated;
}


boolean canSuspend()
{
   return !is_terminated && is_running;
}


boolean canTerminate()
{
   return !is_terminated;
}


boolean isTerminated()			{ return is_terminated; }

boolean isSuspended()
{
   return !is_terminated && !is_running;
}

boolean isRunning()
{
   return !is_terminated && is_running;
}


String getLocalId()			{ return thread_id; }
String getName()			{ return "*MAIN*"; }


void setContinue(DebugDetailValue reason)
{
   continue_reason = reason;
}


void setRunning(boolean running)
{
   if (is_running != running) {
      is_running = running;
      if (running) {
	 for_target.generateThreadEvent("RESUME",continue_reason,this);
	 cur_stack = null;
       }
      else {
	 for_target.generateThreadEvent("SUSPEND",continue_reason,this);
	 // stack_command = new NobaseDebugCommand.Backtrace(for_target,0,1000,false);
	 // for_target.postCommand(stack_command);
       }
    }
   if (!running) continue_reason = null;
}




/********************************************************************************/
/*										*/
/*	Stack frame maintenance 						*/
/*										*/
/********************************************************************************/

boolean hasStackFrames()
{
   return (cur_stack != null && cur_stack.size() > 0);
}


List<NobaseDebugStackFrame> getStackFrames()
{
   if (cur_stack == null) return new ArrayList<>();

   return cur_stack;
}


void updateFrames(JSONArray fdata)
{
   List<NobaseDebugStackFrame> newframes = new ArrayList<>();

   NobaseDebugRefMap refmap = for_target.getReferenceMap();
   for (int i = 0; i < fdata.length(); ++i) {
      JSONObject fobj = fdata.getJSONObject(i);
      NobaseDebugStackFrame frm = new NobaseDebugStackFrame(fobj,refmap,i);
      newframes.add(frm);
    }

   cur_stack = newframes;
}

/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("THREAD");
   xw.field("ID",thread_id);
   xw.field("NAME","User Thread");
   xw.field("SYSTEM",false);
   xw.field("SUSPENDED",isSuspended());
   xw.field("TERMINATED",isTerminated());
   xw.field("PROCESS",for_target.getId());
   if (isSuspended()) {
      xw.field("STACK",true);
      if (hasStackFrames()) {
	 xw.field("FRAMES",cur_stack.size());
       }
    }
   xw.begin("LAUNCH");
   xw.field("MODE","debug");
   xw.field("ID",for_target.getId());
   xw.field("CID",for_target.getLaunchConfig().getId());
   xw.end("LAUNCH");

   xw.end("THREAD");
}





}	// end of class NobaseDebugThread




/* end of NobaseDebugThread.java */

