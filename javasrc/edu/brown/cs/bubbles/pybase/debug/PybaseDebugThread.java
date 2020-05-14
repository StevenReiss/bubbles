/********************************************************************************/
/*										*/
/*		PybaseDebugThread.java						*/
/*										*/
/*	Handle debugging target for Bubbles from Python 			*/
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

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;


public class PybaseDebugThread implements PybaseDebugConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/


private PybaseDebugTarget debug_target;
private String thread_name;
private String remote_id;
private String thread_id;


private boolean is_pydev_thread;

private boolean is_suspended;
private boolean is_stepping;
private List<PybaseDebugStackFrame> cur_stack;

private static IdCounter thread_counter = new IdCounter();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseDebugThread(PybaseDebugTarget target,String name,String id)
{
   debug_target = target;
   thread_name = name;
   remote_id = id;
   setup();
}



PybaseDebugThread(PybaseDebugTarget tgt,Element xml)
{
   debug_target = tgt;
   thread_name = IvyXml.getAttrString(xml,"name");
   remote_id = IvyXml.getAttrString(xml,"id");
   setup();
}



static List<PybaseDebugThread> getThreadsFromXml(PybaseDebugTarget tgt,String txt)
{
   List<PybaseDebugThread> rslt = new ArrayList<PybaseDebugThread>();
   Element e = IvyXml.convertStringToXml(txt);
   if (e != null) {
      for (Element te : IvyXml.elementsByTag(e,"thread")) {
	 PybaseDebugThread t = new PybaseDebugThread(tgt,te);
	 rslt.add(t);
       }
    }
   else System.err.println("PROBLEM WITH THREAD XML: " + txt);

   return rslt;
}



private void setup()
{
   is_pydev_thread = remote_id.equals("-1");    // use a special id for pydev threads
   thread_id = "THREAD_" + Integer.toString(thread_counter.nextValue());
   is_suspended = false;
   is_stepping = false;
   cur_stack = null;
}
		


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public void setSuspended(boolean state,List<PybaseDebugStackFrame> stack)
{
   is_suspended = state;
   cur_stack = stack;
}


public String getName() 			{ return thread_name + " - " + getRemoteId(); }

public String getRemoteId()			{ return remote_id; }
public String getLocalId()			{ return thread_id; }

public boolean isPydevThread()			{ return is_pydev_thread; }

public int getPriority()			{ return 0; }

public PybaseDebugTarget getDebugTarget()	{ return debug_target; }

public boolean canTerminate()			{ return !is_pydev_thread && !isTerminated(); }

public boolean isTerminated()			{ return debug_target.isTerminated(); }

public void terminate()
{
   debug_target.terminate();
}

public boolean canResume()
{
   return !is_pydev_thread && is_suspended && !isTerminated();
}

public boolean canSuspend()
{
   return !is_pydev_thread && !is_suspended && !isTerminated();
}

public boolean isSuspended()			{ return is_suspended; }

public void resume()
{
   if (!is_pydev_thread) {
      cur_stack = null;
      is_stepping = false;
      debug_target.postCommand(new PybaseDebugCommand.ThreadRun(debug_target,remote_id));
    }
}


public void suspend()
{
   if (!is_pydev_thread) {
      cur_stack = null;
      debug_target.postCommand(new PybaseDebugCommand.ThreadSuspend(debug_target,remote_id));
    }
}

public boolean canStepInto()			{ return canResume(); }

public boolean canStepOver()			{ return canResume(); }

public boolean canStepReturn()			{ return canResume(); }

public boolean isStepping()			{ return is_stepping; }

public void stepInto()
{
   if (!is_pydev_thread) {
      is_stepping = true;
      debug_target.postCommand(new PybaseDebugCommand.Step(debug_target,CMD_STEP_INTO,remote_id));
    }
}

public void stepOver()
{
   if (!is_pydev_thread) {
      is_stepping = true;
      debug_target.postCommand(new PybaseDebugCommand.Step(debug_target,CMD_STEP_OVER,remote_id));
    }
}

public void stepReturn()
{
   if (!is_pydev_thread) {
      is_stepping = true;
      debug_target.postCommand(new PybaseDebugCommand.Step(debug_target,CMD_STEP_RETURN,remote_id));
    }
}

public void runToLine(int line, String funcName)
{
   is_stepping = true;
   debug_target.postCommand(new PybaseDebugCommand.RunToLine(debug_target,CMD_RUN_TO_LINE,remote_id, line, funcName));
}


public void setNextStatement(int line, String funcName)
{
   is_stepping = true;
   debug_target.postCommand(new PybaseDebugCommand.SetNext(debug_target,CMD_SET_NEXT_STATEMENT,remote_id, line, funcName));
}


public List<PybaseDebugStackFrame> getStackFrames()
{
   if (is_suspended && cur_stack != null) {
      return cur_stack;
    }
   return new ArrayList<PybaseDebugStackFrame>();
}

public boolean hasStackFrames()
{
   return (cur_stack != null && cur_stack.size() > 0);
}

public PybaseDebugStackFrame getTopStackFrame()
{
   if (cur_stack == null) return null;
   if (cur_stack.size() == 0) return null;
   return cur_stack.get(0);
}


public PybaseDebugStackFrame findStackFrameByID(String id)
{
   if (cur_stack != null) {
      for (PybaseDebugStackFrame frm : cur_stack) {
	 if (id.equals(frm.getId())) return frm;
       }
    }
   return null;
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
   xw.field("NAME",thread_name);
   xw.field("SYSTEM",is_pydev_thread);
   xw.field("SUSPENDED",is_suspended);
   xw.field("TERMINATED",isTerminated());
   if (is_suspended && hasStackFrames()) {
      xw.field("STACK",true);
      xw.field("FRAMES",cur_stack.size());
    }
   xw.end("THREAD");
}



}	// end of class PybaseDebugThread




/* end of PybaseDebugThread.java */
