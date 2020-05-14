/********************************************************************************/
/*										*/
/*		PybaseDebuggerCommand.java					*/
/*										*/
/*	Commands from/to the remote python debugger				*/
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

import java.io.File;
import java.util.List;


/**
 * Superclass of all debugger commands.
 *
 * Debugger commands know how to interact with pydevd.py.
 * See pydevd.py for protocol information.
 *
 * Command lifecycle:
 *  cmd = new Command() // creation
 *  cmd.getSequence()	 // get the sequence number of the command
 *  cmd.getOutgoing()	 // asks command for outgoing message
 *  cmd.aboutToSend()	 // called right before we go on wire
 *			   // by default, if command needs response
 *			   // it gets posted to in the response queue
 *     if (cmd.needsResponse())
 *	   post the command to response queue, otherwise we are done
 *  when response arrives:
 *  if response is an error
 *	   cmd.processResponse()
 *     else
 *	   cmd.processErrorResponse()
 *
 */



abstract class PybaseDebugCommand implements PybaseDebugConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected PybaseDebugTarget debug_target;
protected CommandResponseListener response_listener;
private int sequence_number;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected PybaseDebugCommand(PybaseDebugTarget debugger)
{
   debug_target = debugger;
   response_listener = null;
   sequence_number = debugger.getNextSequence();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public void setCompletionListener(CommandResponseListener listener)
{
   response_listener = listener;
}


abstract public String getOutgoing();


public final int getSequence()			{ return sequence_number; }




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void aboutToSend()
{
   // if we need a response, put me on the waiting queue
   if (needResponse()) {
      debug_target.addToResponseQueue(this);
    }
}


public boolean needResponse()			{ return false; }



public final void processResponse(int cmdcode,String payload)
{
   if (cmdcode / 100  == 9) {
      processErrorResponse(cmdcode, payload);
    }
   else {
      processOKResponse(cmdcode, payload);
    }

   if (response_listener != null) {
      response_listener.commandComplete(this);
    }
}



public void processOKResponse(int cmdcode,String payload)
{
   PybaseMain.logE("Debugger command ignored response " + getClass().toString());
}



public void processErrorResponse(int cmdcode, String payload)
{
   PybaseMain.logE("Debugger command ignored error response " + getClass().toString());
}



public static String makeCommand(int code, int sequence, String payload)
{
   StringBuilder s = new StringBuilder();

   s.append(code);
   s.append("\t");
   s.append(sequence);
   s.append("\t");
   s.append(payload);

   return s.toString();
}



/********************************************************************************/
/*										*/
/*	ChangeVariable								*/
/*										*/
/********************************************************************************/

static class ChangeVariable extends PybaseDebugCommand {

   String var_locator;
   String var_expression;

   ChangeVariable(PybaseDebugTarget debugger, String locator, String expression) {
      super(debugger);
      var_locator = locator;
      var_expression = expression;
    }

   @Override public String getOutgoing() {
      return makeCommand(getCommandId(), getSequence(), var_locator+"\t"+var_expression);
    }

   protected int getCommandId() 	       { return CMD_CHANGE_VARIABLE; }

}	// end of inner class ChangeVariable



/********************************************************************************/
/*										*/
/*	EvaluateExpression							*/
/*										*/
/********************************************************************************/

static class EvaluateExpression extends PybaseDebugCommand {

   private String var_locator;
   private String var_expression;

   private boolean is_error;
   private String result_payload;
   private boolean do_exec;


   EvaluateExpression(PybaseDebugTarget debugger, String expression, String locator, boolean doexec) {
      super(debugger);
      do_exec = doexec;
      var_locator = locator;
      var_expression = var_expression.replaceAll("[\r\n]","");
    }

   @Override public String getOutgoing() {
      int cmd = (do_exec ? CMD_EXEC_EXPRESSION : CMD_EVALUATE_EXPRESSION);
      return makeCommand(cmd, getSequence(), var_locator + "\t" + var_expression);
    }

   @Override public boolean needResponse()		       { return true; }

   @Override public void processOKResponse(int cmdCode, String payload) {
      if (cmdCode == CMD_EVALUATE_EXPRESSION || cmdCode == CMD_EXEC_EXPRESSION)
	 result_payload = payload;
      else {
	 is_error = true;
	 PybaseMain.logE("Unexpected response to EvaluateExpression");
       }
    }

   @Override public void processErrorResponse(int cmdCode, String payload) {
      result_payload = payload;
      is_error = true;
    }

   public String getResponse() {
      if (is_error) return null;
      else return result_payload;
    }

}	// end of inner class EvaluateExpression




/********************************************************************************/
/*										*/
/*	GetCompletions								*/
/*										*/
/********************************************************************************/

static class GetCompletions extends PybaseDebugCommand {

   private String active_token;
   private String locator_value;
   private boolean is_error = false;
   private int response_code;
   private String pay_load;

   public GetCompletions(PybaseDebugTarget debugger, String acttok, String locator) {
      super(debugger);
      this.locator_value = locator;
      this.active_token = acttok;
    }

   @Override public String getOutgoing() {
      int cmd = CMD_GET_COMPLETIONS;
      return makeCommand(cmd, getSequence(), locator_value + "\t" + active_token);
    }

   @Override public boolean needResponse() {
      return true; //The response are the completions!
    }

   @Override public void processOKResponse(int cmdCode, String payload) {
      response_code = cmdCode;
      if (response_code == CMD_GET_COMPLETIONS)
	 pay_load = payload;
      else {
	 is_error = true;
	 PybaseMain.logE("Unexpected response to GetCompletionsCommand");
       }
    }

   @Override public void processErrorResponse(int cmdCode, String payload) {
      response_code = cmdCode;
      pay_load = payload;
      is_error = true;
    }

   public String getResponse() throws PybaseException {
      if (is_error)
	 throw new PybaseException("pydevd error:" + pay_load);
      else
	 return pay_load;
    }

}	// end of inner class GetCompletions




/********************************************************************************/
/*										*/
/*	GetVariable								*/
/*										*/
/********************************************************************************/

static class GetVariable extends PybaseDebugCommand {

   String var_locator;
   boolean is_error = false;
   int response_code;
   String pay_load;

   public GetVariable(PybaseDebugTarget debugger, String locator) {
      super(debugger);
      this.var_locator = locator;
    }

   @Override public String getOutgoing() {
      return makeCommand(getCommandId(), getSequence(), var_locator);
    }

   @Override public boolean needResponse() {
      return true;
    }

   @Override public void processOKResponse(int cmdCode, String payload) {
      response_code = cmdCode;
      if (cmdCode == getCommandId())
	 pay_load = payload;
      else {
	 is_error = true;
	 PybaseMain.logE("Unexpected response to "+this.getClass());
       }
    }

   protected int getCommandId() {
      return CMD_GET_VARIABLE;
    }

   @Override public void processErrorResponse(int cmdCode, String payload) {
      response_code = cmdCode;
      pay_load = payload;
      is_error = true;
    }

   public String getResponse() throws PybaseException {
      if (is_error)
	 throw new PybaseException("pydevd error:" + pay_load);
      else
	 return pay_load;
    }

}	// end of inner class GetVariable


/********************************************************************************/
/*										*/
/*	GetFileContents 							*/
/*										*/
/********************************************************************************/

static class GetFileContents extends GetVariable {

   public GetFileContents(PybaseDebugTarget debugger, String locator) {
      super(debugger, locator);
    }

   @Override protected int getCommandId() {
      return CMD_GET_FILE_CONTENTS;
    }

}	// end of inner class GetFileContents



/********************************************************************************/
/*										*/
/*	GetFrame								*/
/*										*/
/********************************************************************************/

static class GetFrame extends GetVariable {

   public GetFrame(PybaseDebugTarget debugger, String locator) {
      super(debugger, locator);
    }

   @Override protected int getCommandId() {
      return CMD_GET_FRAME;
    }

}	// end of inner class GetFrame




/********************************************************************************/
/*										*/
/*	ReloadCode								*/
/*										*/
/********************************************************************************/

static class ReloadCode extends PybaseDebugCommand {

   private String module_name;

   public ReloadCode(PybaseDebugTarget debugger, String modulename) {
      super(debugger);
      module_name = modulename;
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_RELOAD_CODE, getSequence(), module_name);
    }

}	// end of inner class ReloadCode



/********************************************************************************/
/*										*/
/*	RemoveBreakpoint							*/
/*										*/
/********************************************************************************/

static class RemoveBreakpoint extends PybaseDebugCommand {

   private File break_file;
   private int break_line;

   public RemoveBreakpoint(PybaseDebugTarget debugger, File file, int line) {
      super(debugger);
      break_file = file;
      break_line = line;
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_REMOVE_BREAK, getSequence(), break_file.getPath() + "\t" + break_line);
    }

}	// end of inner class RemoveBreakpoint



/********************************************************************************/
/*										*/
/*	Run									*/
/*										*/
/********************************************************************************/

static class Run extends PybaseDebugCommand {

   public Run(PybaseDebugTarget debugger) {
      super(debugger);
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_RUN, getSequence(), "");
    }

}	// end of inner class Run



/********************************************************************************/
/*										*/
/*	RunToLine								*/
/*										*/
/********************************************************************************/

static class RunToLine extends PybaseDebugCommand {

   private int command_id;
   private String thread_id;
   private String func_name;
   private int line_number;

   public RunToLine(PybaseDebugTarget debugger, int commandid, String threadid, int line, String funcname) {
      super(debugger);
      command_id = commandid;
      thread_id = threadid;
      line_number = line;
      func_name = funcname;
    }

   @Override public String getOutgoing() {
      return makeCommand(command_id, getSequence(), thread_id+"\t"+line_number+"\t"+func_name);
    }

}	// end of inner class RunToLine



/********************************************************************************/
/*										*/
/*	SendPyException 							*/
/*										*/
/********************************************************************************/

static class SendPyException extends PybaseDebugCommand {

   private String for_exception;
   private boolean is_caught;
   private boolean is_uncaught;

   public SendPyException(PybaseDebugTarget debugger,String exc,boolean caught,boolean unc) {
      super(debugger);
      for_exception = exc;
      is_caught = caught;
      is_uncaught = unc;
    }

   @Override public String getOutgoing() {
      String c = (is_caught ? "true" : "false");
      String u = (is_uncaught ? "true" : "false");
      String cmd = u + ";" + c + ";" + for_exception;
      return makeCommand(PybaseDebugConstants.CMD_SET_PY_EXCEPTION,getSequence(),cmd);
    }

}	// end of inner class SendPyException



/********************************************************************************/
/*										*/
/*	SetBreakpoint								*/
/*										*/
/********************************************************************************/

static class SetBreakpoint extends PybaseDebugCommand {

   public File break_file;
   public int break_line;
   public String break_condition;
   private String function_name;

   public SetBreakpoint(PybaseDebugTarget debugger, File file, int line, String condition, String functionname) {
      super(debugger);
      break_file = file;
      break_line = line;
      if (condition == null) {
	 break_condition = "None";
       }
      else {
	 break_condition = condition;
       }
      function_name = functionname;
    }

   @Override public String getOutgoing() {
      StringBuffer cmd = new StringBuffer().
	 append(break_file).append("\t").append(break_line);

      if (function_name != null) {
	 String last = function_name;
	 int idx = last.lastIndexOf(".");
	 if (idx > 0) last = last.substring(idx+1);
	 cmd.append("\t**FUNC**").append(last.trim());
       }

      cmd.append("\t").append(break_condition);

      return makeCommand(CMD_SET_BREAK, getSequence(), cmd.toString());
    }

}	// end of inner class SetBreakpoint



/********************************************************************************/
/*										*/
/*	SetNext 								*/
/*										*/
/********************************************************************************/

static class SetNext extends PybaseDebugCommand {


   int command_id;
   String thread_id;
   String func_name;
   int line_no;

   public SetNext(PybaseDebugTarget debugger, int commandid, String threadid, int line, String funcname) {
      super(debugger);
      command_id = commandid;
      thread_id = threadid;
      line_no = line;
      func_name = funcname;
    }

   @Override public String getOutgoing() {
      return makeCommand(command_id, getSequence(), thread_id+"\t"+line_no+"\t"+func_name);
    }

}	// end of inner class SetNext



/********************************************************************************/
/*										*/
/*	Step									*/
/*										*/
/********************************************************************************/

static class Step extends PybaseDebugCommand {

   int command_id;
   String thread_id;

   public Step(PybaseDebugTarget debugger, int commandid, String threadid) {
      super(debugger);
      command_id = commandid;
      thread_id = threadid;
    }

   @Override public String getOutgoing() {
      return makeCommand(command_id, getSequence(), thread_id);
    }

}	// end of inner class Step



/********************************************************************************/
/*										*/
/*	ThreadKill								*/
/*										*/
/********************************************************************************/

static class ThreadKill extends PybaseDebugCommand {

   String thread_id;

   public ThreadKill(PybaseDebugTarget debugger, String threadid) {
      super(debugger);
      this.thread_id = threadid;
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_THREAD_KILL, getSequence(), thread_id);
    }

}	// end of inner class ThreadKill



/********************************************************************************/
/*										*/
/*	ThreadList								*/
/*										*/
/********************************************************************************/

static class ThreadList extends PybaseDebugCommand {

   boolean is_done;
   List<PybaseDebugThread> thread_data;

   public ThreadList(PybaseDebugTarget target) {
      super(target);
      is_done = false;
    }

   public void waitUntilDone(int timeout) throws InterruptedException {
      while (!is_done && timeout > 0) {
	 timeout -= 100;
	 synchronized (this) {
	    Thread.sleep(100);
	  }
       }
      if (timeout < 0)
	 throw new InterruptedException();
    }

   public List<PybaseDebugThread> getThreads() {
      return thread_data;
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_LIST_THREADS, getSequence(), "");
    }

   @Override public boolean needResponse() {
      return true;
    }

   @Override public void processOKResponse(int cmdCode, String payload) {
      if (cmdCode != 102) {
	 PybaseMain.logE("Unexpected response to LIST THREADS"  + payload);
	 return;
       }
      thread_data = PybaseDebugThread.getThreadsFromXml(debug_target, payload);
      is_done = true;
    }


   @Override public void processErrorResponse(int cmdCode, String payload) {
      PybaseMain.logE("LIST THREADS got an error "  + payload);
    }

}	// end of inner class ThreadList



/********************************************************************************/
/*										*/
/*	ThreadRun								*/
/*										*/
/********************************************************************************/

static class ThreadRun extends PybaseDebugCommand {

   String for_thread;

   public ThreadRun(PybaseDebugTarget debugger, String thread) {
      super(debugger);
      for_thread = thread;
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_THREAD_RUN, getSequence(),for_thread);
    }

}	// end of inner class ThreadRun



/********************************************************************************/
/*										*/
/*	ThreadSuspend								*/
/*										*/
/********************************************************************************/

static class ThreadSuspend extends PybaseDebugCommand {

   String for_thread;

   public ThreadSuspend(PybaseDebugTarget debugger, String thread) {
      super(debugger);
      for_thread = thread;
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_THREAD_SUSPEND, getSequence(),for_thread);
    }

}	// end of inner class ThreadSuspend



/********************************************************************************/
/*										*/
/*	Version 								*/
/*										*/
/********************************************************************************/

static class Version extends PybaseDebugCommand {

   static final String VERSION = "1.1";

   public Version(PybaseDebugTarget debugger) {
      super(debugger);
    }

   @Override public String getOutgoing() {
      return makeCommand(CMD_VERSION, getSequence(), VERSION);
    }

   @Override public boolean needResponse() {
      return true;
    }

   @Override public void processOKResponse(int cmdCode, String payload) {
      //  System.err.println("The version is " + payload);
      // not checking for versioning in 1.0, might come in useful later
    }

}	// end of inner class Version



}	// end of class PybaseDebugCommand




/* end of PybaseDebugCommand.java */
