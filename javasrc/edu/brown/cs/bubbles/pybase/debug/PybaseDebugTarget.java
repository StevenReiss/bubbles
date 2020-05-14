/********************************************************************************/
/*										*/
/*		PybaseDebugTarget.java						*/
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

import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class PybaseDebugTarget implements PybaseDebugConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Socket	comm_socket;
private DebugReader debug_reader;
private DebugWriter debug_writer;
private int	sequence_number;
private PybaseMain pybase_main;
private Process  run_process;

private File	debug_file;
private List<PybaseDebugThread> thread_data;
private boolean is_disconnected;
private PybaseValueModificationChecker modification_checker;
private PybaseDebugger remote_debugger;
private String	target_id;
private OutputStream console_input;

private static IdCounter       target_counter = new IdCounter();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseDebugTarget(PybaseDebugger p,Process px)
{
   comm_socket = null;
   debug_reader = null;
   debug_writer = null;
   remote_debugger = p;
   run_process = px;

   System.err.println("WORKING ON PROCESS " + px);

   target_id = "TARGET_" + Integer.toString(target_counter.nextValue());

   sequence_number = -1;
   pybase_main = PybaseMain.getPybaseMain();

   is_disconnected = false;
   modification_checker = new PybaseValueModificationChecker();
   thread_data = null;
   debug_file = null;

   ConsoleReader cr = new ConsoleReader(px.getInputStream(),false);
   cr.start();
   cr = new ConsoleReader(px.getErrorStream(),false);
   cr.start();
   console_input = px.getOutputStream();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getNextSequence()
{
   sequence_number += 2;
   return sequence_number;
}



PybaseValueModificationChecker getMofificationChecker() { return modification_checker; }



public boolean canTerminate()					{ return true; }
public boolean isTerminated()					{ return false; }
public boolean isSuspended()					{ return false; }

public boolean canResume()
{
   for (PybaseDebugThread t : thread_data) {
      if (t.canResume()) return true;
    }
   return false;
}


public boolean canSuspend()
{
   for (PybaseDebugThread t : thread_data) {
      if (t.canSuspend()) return true;
    }
   return false;
}

public boolean hasThreads()			{ return true; }




public PybaseDebugger getDegugger()				{ return remote_debugger; }
public File getFile()						{ return debug_file; }
public String getId()						{ return target_id; }


public PybaseDebugThread findThreadById(String tid)
{
   for (PybaseDebugThread t : thread_data) {
      if (tid.equals(t.getRemoteId())) return t;
    }
   return null;
}


public boolean canDisconnect()			{ return !is_disconnected; }
public boolean isDisconnected() 		{ return is_disconnected; }
public void disconnect()
{
   terminate();
   modification_checker = null;
}


Process getProcess()				{ return run_process; }



/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

public void addToResponseQueue(PybaseDebugCommand cmd)
{
   if (debug_reader != null) {
      debug_reader.addToResponseQueue(cmd);
    }
}



public void postCommand(PybaseDebugCommand cmd)
{
   if (debug_writer != null) {
      debug_writer.postCommand(cmd);
    }
}



public void startTransmission(Socket s) throws IOException
{
   comm_socket = s;
   debug_reader = new DebugReader(s,this);
   debug_writer = new DebugWriter(s);
   debug_reader.start();
   debug_writer.start();
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

public void initialize()
{
   generateProcessEvent("CREATE");

   // we post version command just for fun
   // it establishes the connection
   postCommand(new PybaseDebugCommand.Version(this));

   // ADD ALL BREAKPOINTS
   PybaseDebugManager pm = PybaseDebugManager.getManager();
   for (PybaseDebugBreakpoint db : pm.getBreakpoints()) {
      breakpointAdded(db);
    }

   // Send the run command, and we are off
   PybaseDebugCommand.Run run = new PybaseDebugCommand.Run(this);
   postCommand(run);
}



public void processCommand(String scode,String seq,String payload)
{
   PybaseMain.logD("DEBUG Command: " + scode + " " + seq + " " + payload);

   try {
      int cmdcode = Integer.parseInt(scode);

      switch (cmdcode) {
	 case CMD_THREAD_CREATED :
	    processThreadCreated(payload);
	    break;
	 case CMD_THREAD_KILL :
	    processThreadKilled(payload);
	    break;
	 case CMD_THREAD_SUSPEND :
	    processThreadSuspended(payload);
	    break;
	 case CMD_THREAD_RUN :
	    processThreadRun(payload);
	    break;
	 default :
	    PybaseMain.logW("Unexpected debugger command " + scode + " " + seq + " " + payload);
	    break;
       }
    }
   catch (Exception e) {
      PybaseMain.logE("Error processing: " + scode + " payload: "+ payload, e);
    }
}





public void resume() throws PybaseException
{
   for (PybaseDebugThread t : thread_data) {
      t.resume();
    }
}


public void suspend() throws PybaseException
{
   for (PybaseDebugThread t : thread_data) {
      t.suspend();
    }
}


public List<PybaseDebugThread> getThreads() throws PybaseException
{
   if (remote_debugger == null) return null;

   if (thread_data == null) {
      PybaseDebugCommand.ThreadList cmd = new PybaseDebugCommand.ThreadList(this);
      postCommand(cmd);
      try {
	 cmd.waitUntilDone(1000);
	 thread_data = cmd.getThreads();
       }
      catch (InterruptedException e) {
	 thread_data = new ArrayList<PybaseDebugThread>();
       }
    }

   return thread_data;
}



public void terminate()
{
   if (comm_socket != null) {
      try {
	 comm_socket.shutdownInput(); // trying to make my pydevd notice that the socket is gone
       }
      catch (Exception e) { }
      try {
	 comm_socket.shutdownOutput();
       }
      catch (Exception e) { }
      try {
	 comm_socket.close();
       }
      catch (Exception e) { }
    }
   comm_socket = null;
   is_disconnected = true;

   if (debug_writer != null) {
      debug_writer.done();
      debug_writer = null;
    }
   if (debug_reader != null) {
      debug_reader.done();
      debug_reader = null;
    }

   thread_data = new ArrayList<PybaseDebugThread>();

   generateProcessEvent("TERMINATE");
}




/********************************************************************************/
/*										*/
/*	Evaluation commands							*/
/*										*/
/********************************************************************************/

void evaluateExpression(String bid,String eid,String expr,String loc)
{
   PybaseDebugCommand.EvaluateExpression cmd = new PybaseDebugCommand.EvaluateExpression(
	 this,expr,loc,true);
   EvalRunner er = new EvalRunner(bid,eid,loc,cmd);
   pybase_main.startTask(er);
}



private class EvalRunner implements Runnable, CommandResponseListener {

   private String bubble_id;
   private String eval_id;
   private String locator_id;
   private PybaseDebugCommand.EvaluateExpression eval_command;

   EvalRunner(String bid,String id,String loc,PybaseDebugCommand.EvaluateExpression cmd) {
      bubble_id = bid;
      eval_id = id;
      locator_id = loc;
      eval_command = cmd;
    }

   @Override public void run() {
      eval_command.setCompletionListener(this);
      postCommand(eval_command);
    }

   @Override public void commandComplete(PybaseDebugCommand cmd) {
       String r = eval_command.getResponse();
       if (r != null) {
	  IvyXmlWriter xw = pybase_main.beginMessage("EVALUATION",bubble_id);
	  xw.field("ID",eval_id);
	  xw.field("STATUS","OK");
	  Element e = IvyXml.convertStringToXml(r);
	  List<PybaseDebugVariable> vars = new ArrayList<PybaseDebugVariable>();
	  for (Element ce : IvyXml.elementsByTag(e,"var")) {
	     vars.add(createVariable(locator_id,ce));
	   }
	  // output vars
	  // pybase_main.finishMessage(xw);
	}
       else {
	  // handle error
	}
    }

}	// end of inner class EvalRunner




private PybaseDebugVariable createVariable(String locator,Element xml)
{
   PybaseDebugVariable var = null;

   String name = IvyXml.getAttrString(xml,"name");
   String type = IvyXml.getAttrString(xml,"type");
   String value = IvyXml.getAttrString(xml,"value");
   boolean contain = IvyXml.getAttrBool(xml,"isContainer");

   if (contain) {
      var = new PybaseDebugVariableCollection(this,name,type,value,locator);
    }
   else {
      var = new PybaseDebugVariable(this,name,type,value,locator);
    }

   return var;
}



/********************************************************************************/
/*										*/
/*	Breakpoint methods							*/
/*										*/
/********************************************************************************/

public void breakpointAdded(PybaseDebugBreakpoint b)
{
   if (b.isEnabled()) {
      switch (b.getType()) {
	 case NONE :
	    break;
	 case LINE :
	    String condition = b.getCondition();
	    if(condition != null){
	       condition = condition.replaceAll("\n", "@_@NEW_LINE_CHAR@_@");
	       condition = condition.replaceAll("\t", "@_@TAB_CHAR@_@");
	    }
	    String fct = "None";                // b.getFunctionName()
	    PybaseDebugCommand.SetBreakpoint cmd = new PybaseDebugCommand.SetBreakpoint(
		     this, b.getFile(), b.getLine(), condition, fct);
	    this.postCommand(cmd);
	    break;
	 case EXCEPTION :
	    PybaseDebugCommand.SendPyException ecmd = new PybaseDebugCommand.SendPyException(
	       this,b.getException(),b.isCaught(),b.isUncaught());
	    this.postCommand(ecmd);
	    break;
      }
    }
}



public void breakpointRemoved(PybaseDebugBreakpoint b)
{
   switch (b.getType()) {
      case NONE :
	 break;
      case LINE :
	 PybaseDebugCommand.RemoveBreakpoint cmd = new PybaseDebugCommand.RemoveBreakpoint(this, b.getFile(), b.getLine());
	 this.postCommand(cmd);
	 break;
      case EXCEPTION :
	 // remove exception breakpoint
   }
}


public void breakpointChanged(PybaseDebugBreakpoint breakpoint)
{
   breakpointRemoved(breakpoint);
   breakpointAdded(breakpoint);
}



/********************************************************************************/
/*										*/
/*	Thread methods								*/
/*										*/
/********************************************************************************/

private void processThreadCreated(String payload)
{
   List<PybaseDebugThread> newthreads = PybaseDebugThread.getThreadsFromXml(this, payload);

   for (Iterator<PybaseDebugThread> it = newthreads.iterator(); it.hasNext(); ) {
      PybaseDebugThread t = it.next();
      if (t.isPydevThread()) it.remove();
    }

   // add threads to the thread list, and fire event
   if (thread_data == null) {
      thread_data = newthreads;
    }
   else {
      thread_data.addAll(newthreads);
    }

   // Now notify debugger that new threads were added
   for (PybaseDebugThread thrd : newthreads) {
      generateThreadEvent("CREATE",null,thrd);
    }
}



private void processThreadKilled(String thread_id)
{
   PybaseDebugThread threadtodelete = findThreadById(thread_id);
   if (threadtodelete != null) {
      thread_data.remove(threadtodelete);
      generateThreadEvent("TERMINATE",null,threadtodelete);
    }
}



private void processThreadSuspended(String payload)
{
   Element e = IvyXml.convertStringToXml(payload);
   Element te = IvyXml.getElementByTag(e,"thread");
   String tid = IvyXml.getAttrString(te,"id");
   PybaseDebugThread t = findThreadById(tid);
   if (t == null) {
      PybaseMain.logE("Problem reading thread suspended data: " + payload);
      return;
    }
   int sr = IvyXml.getAttrInt(te,"stop_reason");
   DebugReason reason = DebugReason.UNSPECIFIED;
   switch (sr) {
      case CMD_STEP_OVER :
      case CMD_STEP_INTO :
      case CMD_STEP_RETURN :
      case CMD_RUN_TO_LINE :
      case CMD_SET_NEXT_STATEMENT :
	 reason = DebugReason.STEP_END;
	 break;
      case CMD_THREAD_SUSPEND :
	 reason = DebugReason.CLIENT_REQUEST;
	 break;
      case CMD_SET_BREAK :
	 reason = DebugReason.BREAKPOINT;
	 break;
      default :
	 PybaseMain.logE("Unexpected reason for suspension: " + sr);
	 reason = DebugReason.UNSPECIFIED;
	 break;
    }

   if (t != null) {
      modification_checker.onlyLeaveThreads(thread_data);
      List<PybaseDebugStackFrame> frms = new ArrayList<PybaseDebugStackFrame>();
      for (Element fe : IvyXml.children(te,"frame")) {
	 String fid = IvyXml.getAttrString(fe,"id");
	 String fnm = IvyXml.getAttrString(fe,"name");
	 String fil = IvyXml.getAttrString(fe,"file");
	 int lno = IvyXml.getAttrInt(fe,"line");
	 File file = null;
	 if (fil != null) {
	    try {
	       fil = URLDecoder.decode(fil,"UTF-8");
	     }
	    catch (UnsupportedEncodingException ex) { }
	    file = new File(fil);
	    if (file.exists()) file = file.getAbsoluteFile();
	  }
	 PybaseDebugStackFrame sf = t.findStackFrameByID(fid);
	 if (sf == null) {
	    sf = new PybaseDebugStackFrame(t,fid,fnm,file,lno,this);
	  }
	 else {
	    sf.setName(fnm);
	    sf.setFile(file);
	    sf.setLine(lno);
	  }
	 frms.add(sf);
       }
      t.setSuspended(true,frms);
      generateThreadEvent("SUSPEND",reason,t);
    }
}



public static String [] getThreadIdAndReason(String payload) throws PybaseException
{
   String [] split = payload.trim().split("\t");
   if (split.length != 2) {
      String msg = "Unexpected threadRun payload " + payload + "(unable to match)";
      throw new PybaseException(msg);
    }

   return split;
}



private void processThreadRun(String payload)
{
   try {
      String [] threadIdAndReason = getThreadIdAndReason(payload);
      DebugReason resumereason = DebugReason.UNSPECIFIED;
      try {
	 int raw_reason = Integer.parseInt(threadIdAndReason[1]);
	 switch (raw_reason) {
	    case CMD_STEP_OVER :
	       resumereason = DebugReason.STEP_OVER;
	       break;
	    case CMD_STEP_RETURN :
	       resumereason = DebugReason.STEP_RETURN;
	       break;
	    case CMD_STEP_INTO :
	       resumereason = DebugReason.STEP_INTO;
	       break;
	    case CMD_RUN_TO_LINE :
	       resumereason = DebugReason.UNSPECIFIED;
	       break;
	    case CMD_SET_NEXT_STATEMENT :
	       resumereason = DebugReason.UNSPECIFIED;
	       break;
	    case CMD_THREAD_RUN :
	       resumereason = DebugReason.CLIENT_REQUEST;
	       break;
	    default :
	       PybaseMain.logE("Unexpected resume reason code " + resumereason);
	       resumereason = DebugReason.UNSPECIFIED;
	    }
       }
      catch (NumberFormatException e) {
	 // expected, when pydevd reports "None"
	 resumereason = DebugReason.UNSPECIFIED;
       }

      String threadID = threadIdAndReason[0];
      PybaseDebugThread t = findThreadById(threadID);
      if (t != null) {
	 t.setSuspended(false, null);
	 generateThreadEvent("RESUME",resumereason,t);
       }
      else {
	 PybaseMain.logE("Unable to find thread " + threadID);
       }
    }
   catch (Exception e1) {
      PybaseMain.logE("Problem processing thread run",e1);
    }
}




/********************************************************************************/
/*										*/
/*	Console methods 							*/
/*										*/
/********************************************************************************/

public void addConsoleInputListener()
{
}


void consoleInput(String txt) throws IOException
{
   console_input.write(txt.getBytes());
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void generateProcessEvent(String kind)
{
    IvyXmlWriter xw = pybase_main.beginMessage("RUNEVENT");
    xw.field("TIME",System.currentTimeMillis());
    xw.begin("RUNEVENT");
    xw.field("TYPE","PROCESS");
    xw.field("KIND",kind);
    outputProcessXml(xw);
    xw.end("RUNEVENT");
    pybase_main.finishMessage(xw);
}


private void generateThreadEvent(String kind,DebugReason reason,PybaseDebugThread dt)
{
   IvyXmlWriter xw = pybase_main.beginMessage("RUNEVENT");
   xw.field("TIME",System.currentTimeMillis());
   xw.begin("RUNEVENT");
   xw.field("TYPE","THREAD");
   xw.field("KIND",kind);
   if (reason != null) xw.field("DETAIL",reason);
   dt.outputXml(xw);
   xw.end("RUNEVENT");
   pybase_main.finishMessage(xw);
}


private void outputProcessXml(IvyXmlWriter xw)
{
   xw.begin("PROCESS");
   xw.field("PID",target_id);
   xw.field("TERMINATED",(comm_socket == null));
   remote_debugger.outputXml(xw);
   xw.end("PROCESS");
}



/********************************************************************************/
/*										*/
/*	DebugReader implementation						*/
/*										*/
/********************************************************************************/

private static class DebugReader extends Thread {

    private Socket read_socket;
    private volatile boolean is_done;
    private Map<Integer,PybaseDebugCommand> response_queue;
    private BufferedReader in_reader;
    private PybaseDebugTarget remote_target;

    DebugReader(Socket s,PybaseDebugTarget r) throws IOException {
       super("PybaseDebugReader_" + s.toString());
       remote_target = r;
       read_socket = s;
       is_done = false;
       response_queue = new HashMap<Integer,PybaseDebugCommand>();
       InputStream sin = read_socket.getInputStream();
       in_reader = new BufferedReader(new InputStreamReader(sin));
     }

    void done() 			{ is_done = true; }

    void addToResponseQueue(PybaseDebugCommand cmd) {
       int sequence = cmd.getSequence();
       synchronized (response_queue) {
	  response_queue.put(Integer.valueOf(sequence),cmd);
	}
     }

    private void processCommand(String cmdline) {
       try {
	  PybaseMain.logD("DEBUG RESPONSE: " + cmdline);

	  String[] cmdparsed = cmdline.split("\t", 3);
	  int cmdcode = Integer.parseInt(cmdparsed[0]);
	  int seqcode = Integer.parseInt(cmdparsed[1]);
	  String payload = URLDecoder.decode(cmdparsed[2], "UTF-8");

	  PybaseDebugCommand cmd;
	  synchronized (response_queue) {
	     cmd = response_queue.remove(Integer.valueOf(seqcode));
	   }

	  if (cmd == null) {
	     if (remote_target != null) {
		remote_target.processCommand(cmdparsed[0],cmdparsed[1],payload);
	      }
	     else {
		PybaseMain.logE("Debug error: command received no target");
	      }
	   }
	  else {
	     cmd.processResponse(cmdcode,payload);
	   }
	}
       catch (Exception e) {
	  PybaseMain.logE("Error processing debug command",e);
	  throw new RuntimeException(e);
	}
     }

    @Override public void run() {
       while (!is_done) {
	  try {
	     String cmdline = in_reader.readLine();
	     if (cmdline == null) {
		is_done = true;
		break;
	      }
	     else if(cmdline.trim().length() > 0) {
		processCommand(cmdline);
	      }
	   }
	  catch (IOException e) {
	     is_done = true;
	   }
	  // there was a 50ms delay here.  why?
	}

       if (is_done || read_socket == null || !read_socket.isConnected() ) {
	  PybaseDebugTarget target = remote_target;
	  if (target != null) {
	     target.terminate();
	   }
	  is_done = true;
	}
     }

}	// end of inner class DebugReader




/********************************************************************************/
/*										*/
/*	DebugWriter implementation						*/
/*										*/
/********************************************************************************/

private static class DebugWriter extends Thread {

   private Socket write_socket;
   private List<PybaseDebugCommand> cmd_queue;
   private OutputStreamWriter out_writer;
   private volatile boolean is_done;

   DebugWriter(Socket s) throws IOException {
      write_socket = s;
      cmd_queue = new ArrayList<PybaseDebugCommand>();
      out_writer = new OutputStreamWriter(s.getOutputStream());
      is_done = false;
    }

   void postCommand(PybaseDebugCommand cmd) {
      synchronized (cmd_queue) {
	 cmd_queue.add(cmd);
	 cmd_queue.notifyAll();
       }
    }

   public void done() {
      synchronized (cmd_queue) {
	 is_done = true;
	 cmd_queue.notifyAll();
       }
    }

   @Override public void run() {
      while (!is_done) {
	 PybaseDebugCommand cmd = null;
	 synchronized (cmd_queue) {
	    while (cmd_queue.size() == 0 && !is_done) {
	       try {
		  cmd_queue.wait();
		}
	       catch (InterruptedException e) { }
	     }
	    if (is_done) break;
	    cmd = cmd_queue.remove(0);
	  }
	 try {
	    if (cmd != null) {
	       cmd.aboutToSend();
	       String c = cmd.getOutgoing();
	       PybaseMain.logD("DEBUG COMMAND " + cmd + " " + c);
	       if (c != null) {
		  out_writer.write(c);
		  out_writer.write("\n");
		  out_writer.flush();
		}
	     }
	  }
	 catch (IOException e1) {
	    is_done = true;
	  }
	 if ((write_socket == null) || !write_socket.isConnected()) {
	    is_done = true;
	  }
       }
   }

}	// end of inner class DebugWriter







/********************************************************************************/
/*										*/
/*	Handle console I/O							*/
/*										*/
/********************************************************************************/

class ConsoleReader extends Thread {

   private Reader input_stream;
   private boolean is_stderr;

   ConsoleReader(InputStream is,boolean isstderr) {
      super("Console_" + (isstderr ? "Err" : "Out") + "_" + target_id);
      input_stream = new InputStreamReader(is);
      is_stderr = isstderr;
    }

   @Override public void run() {
      char [] buf = new char[4096];
      try {
	 for ( ; ; ) {
	    int ln = input_stream.read(buf);
	    if (ln < 0) break;
	    String txt = new String(buf,0,ln);
	    PybaseMain.logD("CONSOLE WRITE: " + is_stderr + " " + txt);
	    IvyXmlWriter xw = pybase_main.beginMessage("CONSOLE");
	    xw.field("PID",target_id);
	    xw.field("STDERR",is_stderr);
	    xw.cdataElement("TEXT",txt);
	    pybase_main.finishMessage(xw);
	  }
       }
      catch (IOException e) {
	 PybaseMain.logD("Error reading from process: " + e);
       }
      IvyXmlWriter xw = pybase_main.beginMessage("CONSOLE");
      xw.field("PID",target_id);
      xw.field("STDERR",is_stderr);
      xw.field("EOF",true);
      pybase_main.finishMessage(xw);
    }
}

}	// end of class PybaseDebugTarget




/* end of PybaseDebugTarget.java */
