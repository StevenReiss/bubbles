/********************************************************************************/
/*										*/
/*		NobaseDebugTarget.java						*/
/*										*/
/*	Interface to a running process to debug 				*/
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
import edu.brown.cs.ivy.xml.IvyXmlWriter;
// import jdk.incubator.http.HttpClient;
// import jdk.incubator.http.WebSocket;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

class NobaseDebugTarget implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

// private WebSocket	comm_socket;
private DebugWebSocket comm_socket;
// private DebugListener	debug_reader;
private DebugWriter	debug_writer;
private DebugEventProcessor event_handler;
private int		sequence_number;
private NobaseMain	nobase_main;
private Process 	run_process;
private NobaseLaunchConfig launch_config;
private NobaseDebugManager debug_manager;

private File	debug_file;
private NobaseDebugThread thread_data;
private boolean is_disconnected;
private String	target_id;
private OutputStream console_input;

private Map<Integer,ScriptData> script_map;
private Map<String,ScriptData> file_map;
private NobaseDebugRefMap reference_map;

private Map<NobaseDebugBreakpoint,BreakData> break_map;

private static IdCounter       target_counter = new IdCounter();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseDebugTarget(NobaseDebugManager mgr,NobaseLaunchConfig cfg)
{
   launch_config = cfg;
   debug_manager = mgr;

   comm_socket = null;
   // debug_reader = null;
   debug_writer = null;
   run_process = null;

   target_id = "TARGET_" + Integer.toString(target_counter.nextValue());

   sequence_number = 0;
   nobase_main = NobaseMain.getNobaseMain();

   script_map = new HashMap<>();
   file_map = new HashMap<>();
   break_map = new HashMap<>();
   reference_map = new NobaseDebugRefMap(this);

   is_disconnected = false;
   thread_data = null;
   debug_file = null;
   console_input = null;
}




/********************************************************************************/
/*										*/
/*	Setup Methods								*/
/*										*/
/********************************************************************************/

void startDebug() throws NobaseException
{
   int port = findPort();

   thread_data = new NobaseDebugThread(this);

   try {
      List<String> cmd = launch_config.getCommandLine(debug_manager,port);
      ProcessBuilder px = new ProcessBuilder(cmd);
      File wd = launch_config.getWorkingDirectory();
      if (wd != null && wd.exists()) px.directory(wd);
      run_process = px.start();
    }
   catch (IOException e) {
      throw new NobaseException("Problem starting debug process",e);
    }

   for (int i = 0; i < 10; ++i) {
      try {
	 URI u = new URI("http://localhost:" + port + "/json/list");
	 HttpURLConnection hc = (HttpURLConnection) u.toURL().openConnection();
	 InputStream is = hc.getInputStream();
	 String cnts = IvyFile.loadFile(is);
	 JSONArray jarr = new JSONArray(cnts);
	 JSONObject jo = jarr.getJSONObject(0);
	 String wsurl = jo.getString("webSocketDebuggerUrl");
	 String id = jo.getString("id");
	 hc.disconnect();

	 URI u1 = new URI("http://localhost:" + port + "/json/version/" + id);
	 HttpURLConnection hc1 = (HttpURLConnection) u1.toURL().openConnection();
	 InputStream is1 = hc1.getInputStream();
	 cnts = IvyFile.loadFile(is1);
	 hc1.disconnect();

	 URI wsuri = new URI(wsurl);
	 comm_socket = new DebugWebSocket(this,wsuri);
	 comm_socket.connect();
	 if (!comm_socket.waitForReady()) throw new IOException("Socket setup error");

	 debug_writer = new DebugWriter(comm_socket);
	 debug_writer.start();

	 event_handler = new DebugEventProcessor();
	 event_handler.start();

	 setupClient();
	 break;
       }
      catch (Throwable e) {
	 if (i >= 5) System.err.println("Got status: " + i + ": " + e);
       }
      try {
	 Thread.sleep(50);
       }
      catch (InterruptedException e) { }
    }

   ConsoleReader cr = new ConsoleReader(run_process.getInputStream(),false);
   cr.start();
   cr = new ConsoleReader(run_process.getErrorStream(),false);
   cr.start();
   console_input = run_process.getOutputStream();


}



private int findPort()
{
   for ( ; ; ) {
      int portid = ((int)(Math.random() * 50000)) + 4000;
      try {
	 Socket s = new Socket("localhost",portid);
	 s.close();
       }
      catch (ConnectException e) {
	 return portid;
       }
      catch (IOException e) { }
    }
}




private void setupClient()
{
   is_disconnected = false;

   generateProcessEvent("CREATE");
   generateThreadEvent("CREATE",null,thread_data);

   try {
      Thread.sleep(2000);
    }
   catch (InterruptedException e) { }

   NobaseDebugCommand cmd;

   cmd = new NobaseDebugCommand.RuntimeEnable(this);
   postCommand(cmd);
   cmd = new NobaseDebugCommand.ProfilerEnable(this);
   postCommand(cmd);
   cmd = new NobaseDebugCommand.Enable(this);
   postCommand(cmd);
   cmd = new NobaseDebugCommand.RuntimeRunIfWaitingForDebugger(this);
   postCommand(cmd);
   cmd = new NobaseDebugCommand.SetBreakpointsActive(this,true);
   postCommand(cmd);
// cmd = new NobaseDebugCommand.Pause(this);
// postCommand(cmd);
// cmd.getResponse();

   for (NobaseDebugBreakpoint bpt : debug_manager.getBreakpoints()) {
      addBreakpointInRuntime(bpt);
    }

// cmd = new NobaseDebugCommand.Resume(this);
// postCommand(cmd);

   cmd.getResponse();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

synchronized int getNextSequence()
{
   sequence_number += 1;
   return sequence_number;
}



boolean canTerminate()
{
   if (thread_data == null) return false;
   return thread_data.canTerminate();
}
boolean isTerminated()					{ return thread_data.isTerminated(); }
boolean isSuspended()					{ return thread_data.isSuspended(); }

boolean canResume()
{
   return thread_data.canResume();
}


boolean canDropToFrame()
{
   return false;
}


boolean canSuspend()
{
   if (thread_data == null) return false;
   return thread_data.canSuspend();
}


File getFile()						{ return debug_file; }
String getId()						{ return target_id; }
NobaseDebugRefMap getReferenceMap()			{ return reference_map; }

int getScriptIdForFile(String file)
{
   ScriptData sd = file_map.get(file);
   if (sd == null) return 0;
   return sd.getId();
}

String getFileForScriptId(int id)
{
   ScriptData sd = script_map.get(id);
   if (sd == null) return null;
   return sd.getFile();
}


NobaseDebugThread findThreadById(String tid)
{
   if (tid == null) return thread_data;
   if (thread_data.getLocalId().equals(tid)) return thread_data;
   return null;
}


boolean canDisconnect() 		{ return !is_disconnected; }
boolean isDisconnected()		{ return is_disconnected; }
void disconnect()
{
   terminate();
}


Process getProcess()				{ return run_process; }
NobaseLaunchConfig getLaunchConfig()		{ return launch_config; }



/********************************************************************************/
/*										*/
/*	Command methods 							*/
/*										*/
/********************************************************************************/

void postCommand(NobaseDebugCommand cmd)
{
   if (comm_socket != null) {
      comm_socket.addToResponseQueue(cmd);
    }
   else {
      cmd.processResponse(null);
      return;
    }

   if (debug_writer != null && comm_socket.isOpen()) {
      debug_writer.postCommand(cmd);
    }
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void resume() throws NobaseException
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.Resume(this);
   postCommand(cmd);
}

void stepInto()
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.StepInto(this);
   thread_data.setContinue(DebugDetailValue.STEP_INTO);
   postCommand(cmd);
}


void stepOver()
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.StepOver(this);
   thread_data.setContinue(DebugDetailValue.STEP_OVER);
   postCommand(cmd);
}

void stepReturn()
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.StepOut(this);
   thread_data.setContinue(DebugDetailValue.STEP_RETURN);
   postCommand(cmd);
}


void dropToFrame(String fid) throws NobaseException
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.RestartFrame(this,fid);
   postCommand(cmd);
}


public void suspend() throws NobaseException
{
   NobaseDebugCommand cmd = new NobaseDebugCommand.Pause(this);
   thread_data.setContinue(DebugDetailValue.CLIENT_REQUEST);
   postCommand(cmd);
}


public List<NobaseDebugThread> getThreads() throws NobaseException
{
   List<NobaseDebugThread> rslt = new ArrayList<NobaseDebugThread>();
   rslt.add(thread_data);
   return rslt;
}



public synchronized void terminate()
{
   if (run_process != null) run_process.destroy();

   is_disconnected = true;

   if (debug_writer != null) {
      debug_writer.done();
      debug_writer = null;
    }

   generateThreadEvent("TERMINATE",null,thread_data);
   thread_data = null;

   generateProcessEvent("TERMINATE");
}


private void noteExit()
{
   comm_socket = null;
   queueDebugEvent(null);
}




/********************************************************************************/
/*										*/
/*	Evaluation commands							*/
/*										*/
/********************************************************************************/

JSONObject getObjectProperties(String id)
{
    NobaseDebugCommand cmd = new NobaseDebugCommand.RuntimeGetProperties(this,id);
    postCommand(cmd);
    NobaseDebugResponse resp = cmd.getResponse();
    return resp.getResult();
}



void evaluateExpression(String bid,String eid,String expr,int frame,boolean brk)
{
   NobaseDebugCommand.EvaluateOnCallFrame cmd =
      new NobaseDebugCommand.EvaluateOnCallFrame(this,frame,expr,brk,60000);
   EvalRunner er = new EvalRunner(bid,eid,null,cmd);
   nobase_main.startTask(er);
}



private class EvalRunner implements Runnable {

   private String bubble_id;
   private String eval_id;
   private NobaseDebugCommand eval_command;

   EvalRunner(String bid,String id,String loc,NobaseDebugCommand cmd) {
      bubble_id = bid;
      eval_id = id;
      eval_command = cmd;
    }

   @Override public void run() {
      postCommand(eval_command);
      NobaseDebugResponse resp = eval_command.getResponse();
      IvyXmlWriter xw = nobase_main.beginMessage("EVALUATION",bubble_id);
      xw.field("ID",eval_id);
      xw.begin("EVAL");
      if (resp != null && resp.isSuccess()) {
	 xw.field("STATUS","OK");
	 // Object body = resp.getBodyAny();
	 // NobaseDebugRefMap refmap = resp.getRefMap();
	 // NobaseDebugValue ndv = NobaseDebugValue.getValue(body,refmap);
	 // need to complete the value here and handle all references
	 // ndv.outputXml(null,0,xw);
       }
      else if (resp != null && resp.getError() != null) {
	 xw.field("STATUS","ERROR");
	 xw.textElement("ERROR",resp.getError());
       }
      else {
	 xw.field("STATUS","ERROR");
       }
      xw.end("EVAL");
      nobase_main.finishMessage(xw);
   }

}	// end of inner class EvalRunner





/********************************************************************************/
/*										*/
/*	Breakpoint methods							*/
/*										*/
/********************************************************************************/

void addBreakpointInRuntime(NobaseDebugBreakpoint bp)
{
   NobaseDebugCommand cmd;

   switch (bp.getType()) {
      case EXCEPTION :
	 cmd = new NobaseDebugCommand.SetPauseOnExceptions(this,
	       bp.isCaught(),bp.isUncaught());
	 postCommand(cmd);
	 break;
      case LINE :
	 String file = bp.getFile().getPath();
	 if (file_map.get(file) != null) {
	    cmd = new NobaseDebugCommand.SetLineBreakpoint(this,file,bp.getLine());
	  }
	 else {
	    cmd = new NobaseDebugCommand.SetUrlBreakpoint(this,file,bp.getLine());
	  }
	 postCommand(cmd);
	 break;
      case NONE :
	 return;
    }
}

void handleBreakpointResolved(JSONObject data)
{
   int bid = data.getInt("breakpointId");
   JSONObject loc = data.getJSONObject("location");
   int sid = loc.getInt("scriptId");
   int lno = loc.getInt("lineNumber");
   // int cno = loc.getInt("columnNumber");
   ScriptData sd = script_map.get(sid);
   File file = new File(sd.getFile());

   int delta = -1;
   NobaseDebugBreakpoint best = null;
   for (NobaseDebugBreakpoint bpt : debug_manager.getBreakpoints()) {
      BreakData bd = break_map.get(bpt);
      if (bd == null) {
	 if (bpt.getFile().equals(file)) {
	    int d = lno - bpt.getLine();
	    if (d == 0) {
	       best = bpt;
	       break;
	     }
	    else if (d > 0 && (delta < 0 || d < delta)) {
	       best = bpt;
	       delta = d;
	     }
	  }
       }
    }
   if (best != null) {
      BreakData bd = new BreakData(best);
      bd.setId(bid);
      break_map.put(best,bd);
    }
}



void breakpointRemoved(NobaseDebugBreakpoint b)
{
   BreakData bd = getBreakData(b);

   if (bd != null && bd.getId() > 0) {
      NobaseDebugCommand cmd = new NobaseDebugCommand.RemoveBreakpoint(this,bd.getId());
      postCommand(cmd);
    }
   else if (b.getType() == BreakType.EXCEPTION) {
      NobaseDebugCommand cmd = new NobaseDebugCommand.SetPauseOnExceptions(this,false,false);
      postCommand(cmd);
    }
   break_map.remove(b);
}


public void breakpointChanged(NobaseDebugBreakpoint breakpoint)
{
   breakpointRemoved(breakpoint);
   addBreakpointInRuntime(breakpoint);
}



/********************************************************************************/
/*										*/
/*	Execution methods							*/
/*										*/
/********************************************************************************/

private void queueDebugEvent(JSONObject msg)
{
   event_handler.addEvent(msg);
}



private void handleDebugEvent(JSONObject msg)
{
   String mthd = msg.optString("method",null);
   if (mthd == null) return;
   JSONObject cnts = msg.getJSONObject("params");
   NobaseMain.logD("DEBUG EVENT: " + msg);

   switch (mthd) {
      case "Runtime.executionContextCreated" :
      case "Runtime.executionContextDestroyed" :
      case "Runtime.executionContextsCleaned" :
      case "Runtime.exceptionRevoked" :
      case "Runtime.consoleAPICalled" :
      case "Runtime.inspectRequested" :
	 break;
      case "Runtime.exceptionThrown" :
	 break;
      case "Debugger.scriptParsed" :
      case "Debugger.scriptFailedToParse" :
	 addScript(cnts);
	 break;
      case "Debugger.breakpointResolved" :
	 handleBreakpointResolved(cnts);
	 break;
      case "Debugger.paused" :
	 handleSuspended(cnts);
	 break;
      case "Debugger.resumed" :
	 break;
      case "Profiler.consoleProfileStarted" :
      case "Profiler.consoleProfileFinished" :
	 break;
      default :
	 break;

    }
}




private void handleSuspended(JSONObject payload)
{
   String sr = payload.getString("reason");
   DebugDetailValue reason = DebugDetailValue.UNSPECIFIED;
   switch (sr) {
      case "XHR" :
      case "DOM" :
      case "EventListener" :
      case "exception" :
      case "assert" :
      case "promiseRejection" :
      case "OOM" :
      case "ambiguous" :
	 reason = DebugDetailValue.UNSPECIFIED;
	 break;
      case "debugCommand" :
	 reason = DebugDetailValue.STEP_END;
	 break;
      case "other" :
	 JSONArray arr = payload.optJSONArray("hitBreakpoints");
	 if (arr != null && arr.length() > 0) reason = DebugDetailValue.BREAKPOINT;
	 else reason = DebugDetailValue.STEP_END;
	 break;
      case "Break on start" :
	 reason = DebugDetailValue.BREAKPOINT;
	 break;
      default :
	 NobaseMain.logE("Unexpected reason for suspension: " + sr);
	 reason = DebugDetailValue.UNSPECIFIED;
	 break;
    }

   generateThreadEvent("SUSPEND",reason,thread_data);

   JSONArray framedata = payload.getJSONArray("callFrames");
   thread_data.updateFrames(framedata);
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
    IvyXmlWriter xw = nobase_main.beginMessage("RUNEVENT");
    xw.field("TIME",System.currentTimeMillis());
    xw.begin("RUNEVENT");
    xw.field("TYPE","PROCESS");
    xw.field("KIND",kind);
    outputProcessXml(xw);
    xw.end("RUNEVENT");
    nobase_main.finishMessage(xw);
}


void generateThreadEvent(String kind,DebugDetailValue reason,NobaseDebugThread dt)
{
   IvyXmlWriter xw = nobase_main.beginMessage("RUNEVENT");
   xw.field("TIME",System.currentTimeMillis());
   xw.begin("RUNEVENT");
   xw.field("TYPE","THREAD");
   xw.field("KIND",kind);
   if (reason != null) xw.field("DETAIL",reason);
   if (dt != null) dt.outputXml(xw);
   else xw.field("NOTHREAD",true);
   xw.end("RUNEVENT");
   nobase_main.finishMessage(xw);
}



private void outputProcessXml(IvyXmlWriter xw)
{
   xw.begin("PROCESS");
   xw.field("PID",target_id);
   if (comm_socket == null) {
      xw.field("TERMINATED",true);
    }
   else {
      xw.field("CANTERM",true);
    }

   xw.begin("LAUNCH");
   xw.field("MODE","debug");
   xw.field("CID",launch_config.getId());
   xw.field("ID",target_id);
   xw.end("LAUNCH");

   xw.end("PROCESS");
}




/********************************************************************************/
/*										*/
/*	DebugListener implementation						*/
/*										*/
/********************************************************************************/

private static class DebugWebSocket extends WebSocketClient {

   private Map<Integer,NobaseDebugCommand> response_queue;
   private NobaseDebugTarget remote_target;
   private Boolean is_ready;
   private boolean accept_messages;

   DebugWebSocket(NobaseDebugTarget r,URI wsuri) {
      super(wsuri);
      remote_target = r;
      response_queue = new HashMap<>();
      is_ready = null;
      accept_messages = true;
    }

   void addToResponseQueue(NobaseDebugCommand cmd) {
      int sequence = cmd.getSequence();
      synchronized (response_queue) {
	 if (accept_messages)
	    response_queue.put(sequence,cmd);
	 else
	    cmd.processResponse(null);
       }
    }

   @Override public void onMessage(ByteBuffer msg) {
      String s0 = new String(msg.array());
      onMessage(s0);
    }

   @Override public void onClose(int status,String reason,boolean remote) {
      NobaseMain.logD("DEBUG Socket CLOSE " + status + " " + reason + " " + remote);
      remote_target.noteExit();
      synchronized (response_queue) {
         accept_messages = false;
         for (NobaseDebugCommand cmd : response_queue.values()) {
            cmd.processResponse(null);
          }
       }
    }

   @Override public void onError(Exception err) {
      NobaseMain.logD("DEBUG Socket ERROR " + err);
      err.printStackTrace();
      synchronized (this) {
	 is_ready = false;
	 notifyAll();
       }
    }

   @Override public void onOpen(ServerHandshake hsd) {
      NobaseMain.logD("DEBUG Socket OPEN " + hsd.getHttpStatus() + " " + hsd.getHttpStatusMessage());
      synchronized (this) {
	 is_ready = true;
	 notifyAll();
       }
    }

   @Override public void onMessage(String msg) {
      processCommand(msg);
    }

   private void processCommand(String jsonstr) {
      try {
	 JSONObject json = new JSONObject(jsonstr);
	 int seqno = json.optInt("id");
	 if (seqno != 0) {
	    NobaseMain.logD("DEBUG RESPONSE: " + jsonstr);
	    NobaseDebugCommand cmd;
	    synchronized (response_queue) {
	       cmd = response_queue.remove(Integer.valueOf(seqno));
	     }
	    if (cmd != null) {
	       cmd.processResponse(json);
	     }
	  }
	 else {
	    NobaseMain.logD("DEBUG MESSAGE: " + jsonstr);
	    remote_target.queueDebugEvent(json);
	  }
       }
      catch (Exception e) {
	 NobaseMain.logE("Error processing debug command",e);
	 e.printStackTrace();
	 throw new RuntimeException(e);
       }
    }

   synchronized boolean waitForReady() {
      while (is_ready == null) {
	 try {
	    wait(1000);
	  }
	 catch (InterruptedException e) { }
       }
      return is_ready;
    }

}	// end of inner class DebugWebSocket




/********************************************************************************/
/*										*/
/*	Event processor 							*/
/*										*/
/********************************************************************************/

private class DebugEventProcessor extends Thread {

   private List<JSONObject> command_queue;

   DebugEventProcessor() {
      super("NobaseDebugEventProcessor");
      command_queue = new LinkedList<>();
    }

   synchronized void addEvent(JSONObject o) {
      if (o != null) command_queue.add(o);
      notifyAll();
    }

   @Override public void run() {
      for ( ; ; ) {
	 JSONObject evt = null;
	 synchronized (this) {
	    while (command_queue.isEmpty()) {
	       if (comm_socket == null) break;
	       try {
		  wait(1000);
		}
	       catch (InterruptedException e) { }
	     }
	    if (comm_socket == null)
	       break;
	    evt = command_queue.remove(0);
	  }
	 try {
	    if (evt != null) handleDebugEvent(evt);
	  }
	 catch (Throwable t) {
	    NobaseMain.logE("Problem processing event",t);
	  }
       }
    }

}	// end of inner class DebugEventProcessor




/********************************************************************************/
/*										*/
/*	DebugWriter implementation						*/
/*										*/
/********************************************************************************/

private static class DebugWriter extends Thread {

   private WebSocketClient write_socket;
   private List<NobaseDebugCommand> cmd_queue;
   private volatile boolean is_done;

   DebugWriter(WebSocketClient s) {
      super("DebugWriter_" + "_" + s);
      write_socket = s;
      cmd_queue = new ArrayList<>();
      is_done = false;
    }

   void postCommand(NobaseDebugCommand cmd) {
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
	 NobaseDebugCommand cmd = null;
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
	       String c = cmd.getOutgoing();
	       NobaseMain.logD("DEBUG SEND COMMAND " + c);
	       if (c != null) {
		  write_socket.send(c);
		}
	     }
	  }
	 catch (Throwable e1) {
	    is_done = true;
	  }
	 if ((write_socket == null) || write_socket.isClosed()) {
	    is_done = true;
	    write_socket = null;
	  }
       }
   }

}	// end of inner class DebugWriter



/********************************************************************************/
/*										*/
/*	Handle console I/O							*/
/*										*/
/********************************************************************************/

private class ConsoleReader extends Thread {

   private Reader input_stream;
   private boolean is_stderr;
   private boolean have_header;

   ConsoleReader(InputStream is,boolean isstderr) {
      super("Console_" + (isstderr ? "Err" : "Out") + "_" + target_id);
      input_stream = new InputStreamReader(is);
      is_stderr = isstderr;
      have_header = false;
    }

   @Override public void run() {
      char [] buf = new char[4096];
      try {
	 for ( ; ; ) {
	    int ln = input_stream.read(buf);
	    if (ln < 0) break;
	    String txt = new String(buf,0,ln);
	    if (!is_stderr && !have_header && txt.contains("listening")) {
	       have_header = true;
	       continue;
	     }
	    NobaseMain.logD("CONSOLE WRITE: " + is_stderr + " " + txt);
	    IvyXmlWriter xw = nobase_main.beginMessage("CONSOLE");
	    xw.field("PID",target_id);
	    xw.field("STDERR",is_stderr);
	    xw.cdataElement("TEXT",txt);
	    nobase_main.finishMessage(xw);
	  }
       }
      catch (IOException e) {
	 NobaseMain.logD("Error reading from process: " + e);
       }
      IvyXmlWriter xw = nobase_main.beginMessage("CONSOLE");
      xw.field("PID",target_id);
      xw.field("STDERR",is_stderr);
      xw.field("EOF",true);
      nobase_main.finishMessage(xw);
    }
}



/********************************************************************************/
/*										*/
/*	ScriptData representation						*/
/*										*/
/********************************************************************************/

private void addScript(JSONObject obj)
{
   if (obj == null) return;
   ScriptData sd = new ScriptData(obj);

   synchronized (script_map) {
      script_map.put(sd.getId(),sd);
      file_map.put(sd.getFile(),sd);
    }
}



private class ScriptData {

   private int script_id;
   private String script_file;

   ScriptData(JSONObject obj) {
      script_id = obj.getInt("scriptId");
      script_file = obj.getString("url");
      obj.getInt("startLine");
      obj.getInt("startColumn");
      obj.getInt("endLine");
      obj.getInt("endColumn");
      obj.getInt("executionContextId");
      obj.getString("hash");
      obj.getBoolean("hasSourceURL");
      obj.getBoolean("isModule");
      obj.getInt("length");
    }

   int getId()				{ return script_id; }
   String getFile()			{ return script_file; }

}	// end of inner class ScriptData



/********************************************************************************/
/*										*/
/*	Breakpoint Data 							*/
/*										*/
/********************************************************************************/

private BreakData getBreakData(NobaseDebugBreakpoint bp)
{
   synchronized (break_map) {
      BreakData bd = break_map.get(bp);
      if (bd == null) {
	 bd = new BreakData(bp);
	 break_map.put(bp,bd);
       }
      return bd;
    }
}



private class BreakData {

   @SuppressWarnings("unused") private NobaseDebugBreakpoint break_point;
   private int break_id;

   BreakData(NobaseDebugBreakpoint bp) {
      break_point = bp;
      break_id = 0;
    }

   int getId()						{ return break_id; }

   void setId(int id)					{ break_id = id; }

}	// end of inner class BreakData





}	// end of class NobaseDebugTarget




/* end of NobaseDebugTarget.java */

