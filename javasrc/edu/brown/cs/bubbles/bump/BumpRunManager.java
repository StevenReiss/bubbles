/********************************************************************************/
/*										*/
/*		BumpRunManager.java						*/
/*										*/
/*	BUblles Mint Partnership run model management				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.bandaid.BandaidConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class BumpRunManager implements BumpConstants, BumpConstants.BumpRunModel, BandaidConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpClient	bump_client;
private Map<String,LaunchConfig> known_configs;
private Map<String,LaunchData> active_launches;
private Map<String,ProcessData> active_processes;
private Map<String,ProcessData> terminated_processes;
private ConcurrentMap<String,ProcessData> named_processes;
private ConcurrentMap<String,ThreadData> active_threads;
private Map<BumpThread,SwingEventListenerList<BumpThreadFilter>> thread_filters;
private Map<String,File>	source_map;
private Map<String,ProcessData> console_processes;
private BumpTrieProcessor trie_processor;
private List<BumpLaunchType> launch_types;

private boolean 	use_debug_server;
private String		server_host;
private String		server_port;
private PrintWriter	trie_writer;
private PrintWriter	perf_writer;

private SwingEventListenerList<BumpRunEventHandler> event_handlers;


enum RunEventKind {
   NONE,
   RESUME,
   SUSPEND,
   CREATE,
   TERMINATE,
   CHANGE,
   MODEL_SPECIFIC,
   HOTCODE_SUCCESS,
   HOTCODE_FAILURE,
};
enum RunEventDetail { NONE, STEP_INTO, STEP_OVER, STEP_RETURN, TERMINATE, BREAKPOINT,
			 CLIENT_REQUEST, EVALUATION, EVALUATION_IMPLICIT,
			 STATE, CONTENT };
enum RunEventType { NONE, PROCESS, THREAD, TARGET, CONSOLE };

private static Map<String,BumpThreadType> known_threads;


private static final Pattern PORT_PATTERN = Pattern.compile("port=(\\d+)[,}]");
private static final Pattern HOST_PATTERN = Pattern.compile("hostname=((\\w|.)+)[,}]");


static {
   known_threads = new HashMap<>();
   known_threads.put("AWT-Shutdown",BumpThreadType.JAVA);
   known_threads.put("AWT-XAWT",BumpThreadType.JAVA);
   known_threads.put("AWT-EventQueue-0",BumpThreadType.UI);
   known_threads.put("AWT-EventQueue-1",BumpThreadType.UI);
   known_threads.put("AWT-EventQueue-2",BumpThreadType.UI);
   known_threads.put("AWT-EventQueue-3",BumpThreadType.UI);
   known_threads.put("AWT-AppKit",BumpThreadType.UI);
   known_threads.put("Image Fetcher 0",BumpThreadType.UI);
   known_threads.put("Image Fetcher 1",BumpThreadType.UI);
   known_threads.put("Image Fetcher 2",BumpThreadType.UI);
   known_threads.put("Image Fetcher 3",BumpThreadType.UI);
   known_threads.put("Image Fetcher 4",BumpThreadType.UI);
   known_threads.put("Image Fetcher 5",BumpThreadType.UI);
   known_threads.put("Image Fetcher 6",BumpThreadType.UI);
   known_threads.put("Image Fetcher 7",BumpThreadType.UI);
   known_threads.put("Image Fetcher 8",BumpThreadType.UI);
   known_threads.put("Image Fetcher 9",BumpThreadType.UI);
   known_threads.put("Basic L&F File Loading Thread",BumpThreadType.UI);
   known_threads.put("DestroyJavaVM",BumpThreadType.SYSTEM);
   known_threads.put("process reaper",BumpThreadType.SYSTEM);
   known_threads.put("Reference Handler",BumpThreadType.SYSTEM);
   known_threads.put("Finalizer",BumpThreadType.SYSTEM);
   known_threads.put("Signal Dispatcher",BumpThreadType.SYSTEM);
   known_threads.put("(VM Periodic Task)",BumpThreadType.SYSTEM);
   known_threads.put("(Signal Handler)",BumpThreadType.SYSTEM);
   known_threads.put("(Sensor Event Thread)",BumpThreadType.SYSTEM);
   known_threads.put("(OC Main Thread)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Optimization Thread 1)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Optimization Thread 2)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Optimization Thread 3)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Optimization Thread 4)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Generation Thread 1)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Generation Thread 2)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Generation Thread 3)",BumpThreadType.SYSTEM);
   known_threads.put("(Code Generation Thread 4)",BumpThreadType.SYSTEM);
   known_threads.put("(Attach Listener)",BumpThreadType.SYSTEM);
   known_threads.put("VM JFR Buffer Thread",BumpThreadType.SYSTEM);
   known_threads.put("HandshakeCompletedNotify-Thread",BumpThreadType.SYSTEM);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpRunManager()
{
   bump_client = null;
   known_configs = new ConcurrentHashMap<>();
   active_launches = new ConcurrentHashMap<>();
   active_processes = new ConcurrentHashMap<>();
   console_processes = new ConcurrentHashMap<>();
   named_processes = new ConcurrentHashMap<>();
   active_threads = new ConcurrentHashMap<>();
   terminated_processes = new ConcurrentHashMap<>();
   event_handlers = new SwingEventListenerList<>(BumpRunEventHandler.class);
   server_host = null;
   server_port = null;
   source_map = new HashMap<>();
   trie_processor = null;
   BoardProperties bp = BoardProperties.getProperties("Bddt");
   use_debug_server = bp.getBoolean("Bddt.debug.server");

   thread_filters = new HashMap<>();

   switch (BoardSetup.getSetup().getRunMode()) {
      case CLIENT :
	 use_debug_server = false;
	 break;
      case SERVER :
	 use_debug_server = bp.getBoolean("Bddt.cloud.performance");
	 break;
      default:
	 break;
    }



  trie_writer = null;
  perf_writer = null;
  BoardProperties bpa = BoardProperties.getProperties("Bandaid");
  if (bpa.getBoolean("bandaid.record.trie")) {
     File f1 = BoardLog.getBubblesLogFile();
     if (f1 != null) {
        File f2 = f1.getParentFile();
        File f3 = new File(f2,f1.getName() + ".trie");
        try {
           trie_writer = new PrintWriter(new FileWriter(f3));
         }
        catch (IOException e) {
           BoardLog.logE("BUMP","Can't create trie output data file");
         }
      }
   }
  if (bpa.getBoolean("bandaid.record.perf")) {
     File f1 = BoardLog.getBubblesLogFile();
     if (f1 != null) {
        File f2 = f1.getParentFile();
        File f3 = new File(f2,f1.getName() + ".perf");
        try {
           perf_writer = new PrintWriter(new FileWriter(f3));
         }
        catch (IOException e) {
           BoardLog.logE("BUMP","Can't create perf output data file");
         }
      }
   }

  launch_types = new ArrayList<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Iterable<BumpProcess> getProcesses()
{
   return new ArrayList<>(active_processes.values());
}



@Override public void addRunEventHandler(BumpRunEventHandler reh)
{
   event_handlers.add(reh);
}


@Override public void removeRunEventHandler(BumpRunEventHandler reh)
{
   event_handlers.remove(reh);
}



void addThreadFilter(BumpThread bt,BumpThreadFilter btf)
{
   SwingEventListenerList<BumpThreadFilter> ls;

   synchronized (thread_filters) {
      ls = thread_filters.get(bt);
      if (ls == null) {
	 ls = new SwingEventListenerList<>(BumpThreadFilter.class);
	 thread_filters.put(bt,ls);
       }
    }

   ls.add(btf);
}



void removeThreadFilter(BumpThread bt,BumpThreadFilter btf)
{
   SwingEventListenerList<BumpThreadFilter> ls;

   synchronized (thread_filters) {
      ls = thread_filters.get(bt);
    }

   if (ls != null) ls.remove(btf);
}




void terminateAll()
{
   List<ProcessData> acts;

   acts = new ArrayList<ProcessData>(active_processes.values());

   for (ProcessData pd : acts) {
      bump_client.terminate(pd);
    }
}


@Override public List<BumpLaunchType> getLaunchTypes()
{
   return launch_types;
}


/********************************************************************************/
/*										*/
/*	Launch configuration methods						*/
/*										*/
/********************************************************************************/

@Override public Iterable<BumpLaunchConfig> getLaunchConfigurations()
{
   return new ArrayList<BumpLaunchConfig>(known_configs.values());
}



@Override public BumpLaunchConfig getLaunchConfiguration(String id)
{
   return known_configs.get(id);
}




@Override public BumpLaunchConfig createLaunchConfiguration(String name,BumpLaunchType typ)
{
   Element e = bump_client.getNewRunConfiguration(name,null,typ);

   return getLaunchResult(e);
}



private LaunchConfig getLaunchResult(Element x)
{
   if (IvyXml.isElement(x,"RESULT")) {
      Element lc = IvyXml.getChild(x,"CONFIGURATION");
      if (lc != null) {
	 String id = IvyXml.getAttrString(lc,"ID");
	 LaunchConfig xlc = known_configs.get(id);
	 if (xlc == null) xlc = new LaunchConfig(lc);
	 else xlc.update(lc);
	 return xlc;
       }
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Debug monitor setup methods						*/
/*										*/
/********************************************************************************/

String startDebugArgs(String id)
{
   startDebugServer();

   if (server_host == null) return null;

   String p = BoardSetup.getSetup().getLibraryPath("bandaid.jar");
   if (p == null) return null;

   String args = "-javaagent:" + p + "=";
   args += "host=" + server_host;
   args += ";port=" + server_port;
   if (id != null) args += ";id=" + id;
   args += ";enable";
   switch (BoardSetup.getSetup().getRunMode()) {
      case CLIENT :
      case SERVER :
	 args += ";remote";
	 break;
      default :
	 break;
    }
   File f = BoardSetup.getBubblesWorkingDirectory();
   args += ";base=" + f.getPath();

   BoardProperties bp = BoardProperties.getProperties("Bandaid");
   String agts = bp.getProperty("bandaid.agents");
   if (agts == null) agts = "All";
   agts = agts.replace(",",";");
   agts = agts.replace(" ",";");
   agts = agts.replace(":",";");
   args += ";" + agts;

   return args;
}




private void startDebugServer()
{
   if (!use_debug_server) return;

   synchronized (this) {
      server_host = null;

      List<String> args = new ArrayList<String>();
      args.add(IvyExecQuery.getJavaPath());
      args.add("-cp");
      args.add(System.getProperty("java.class.path"));
      args.add("edu.brown.cs.bubbles.bump.BumpDebugServer");
      args.add("-M");
      args.add(BoardSetup.getSetup().getMintName());

      MintControl mc = BoardSetup.getSetup().getMintControl();

      for (int i = 0; i < 5; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BDDT CMD='PORT' />",rply,MINT_MSG_FIRST_NON_NULL);
	 Element rslt = rply.waitForXml(10000);
	 if (rslt != null && IvyXml.isElement(rslt,"SOCKET")) {
	    server_host = fixHost(IvyXml.getAttrString(rslt,"HOST"));
	    server_port = IvyXml.getAttrString(rslt,"PORT");
	    break;
	  }
	 if (i == 0) {
	    try {
	       new IvyExec(args,null,0);
	     }
	    catch (IOException e) {
	       break;
	     }
	  }
	 try {
	    wait(1000);
	  }
	 catch (InterruptedException e) { }
       }

      if (server_host == null) use_debug_server = false;
    }
}




private static String fixHost(String h)
{
   if (h == null) return null;

   try {
      String h1 = InetAddress.getLocalHost().getHostName();
      String h2 = InetAddress.getLocalHost().getHostAddress();
      String h3 = InetAddress.getLocalHost().getCanonicalHostName();

      if (h.equals(h1) || h.equals(h2) || h.equals(h3)) {
	 return "127.0.0.1";
       }
   }
   catch (UnknownHostException e) { }

   return h;
}


BumpTrieData getTrieData(BumpProcess bp)
{
   if (trie_processor == null) {
      synchronized (this) {
	 if (trie_processor == null) {
	    trie_processor = new BumpTrieProcessor();
	    addRunEventHandler(trie_processor);
	  }
       }
    }
   if (bp == null) return null;

   return trie_processor.getTrieDataForProcess(bp);
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

void setup()
{
   // called once eclipse is running

   bump_client = BumpClient.getBump();

   Element xml1 = bump_client.getLanguageData();
   xml1 = IvyXml.getChild(xml1,"LAUNCHES");
   for (Element launch : IvyXml.children(xml1,"LAUNCH")) {
      LaunchType lt = new LaunchType(launch);
      launch_types.add(lt);
   }

   Element xml = bump_client.getRunConfigurations();
   for (Element cnf : IvyXml.children(xml,"CONFIGURATION")) {
      LaunchConfig lc = new LaunchConfig(cnf);
      if (lc.getId() != null) {
	 known_configs.put(lc.getId(),lc);
       }
    }

   for (Element prc : IvyXml.children(xml,"PROCESS")) {
      ProcessData pd = new ProcessData(prc);
      active_processes.put(pd.getId(),pd);
      console_processes.put(pd.getId(),pd);
    }

   BoardSetup bs = BoardSetup.getSetup();
   bs.getMintControl().register("<BANDAID REPORT='_VAR_0' TIME='_VAR_1'><_VAR_2 /></BANDAID>",
				   new BandaidHandler());
   bs.getMintControl().register("<BANDAID HISTORY='_VAR_0' THREAD='_VAR_1'><_VAR_2 /></BANDAID>",
				   new BandaidHistoryHandler());
   bs.getMintControl().register("<BANDAID SWING='_VAR_0'><_VAR_1 /></BANDAID>",
				   new BandaidSwingHandler());
}





/********************************************************************************/
/*										*/
/*	Handle special commands not directly supported by Eclipse		*/
/*										*/
/********************************************************************************/

void stepUser(BumpThread bt)
{
   StepUserFilter suf = new StepUserFilter(bt);
   addThreadFilter(bt,suf);
   bump_client.stepInto(bt);
}



/********************************************************************************/
/*										*/
/*	Base class for run events						*/
/*										*/
/********************************************************************************/

private abstract static class BaseEvent implements BumpRunEvent {

   @Override public abstract BumpRunEventType getEventType();

   @Override public BumpLaunchConfig getLaunchConfiguration()	{ return null; }
   @Override public BumpLaunch getLaunch()			{ return null; }
   @Override public BumpProcess getProcess()			{ return null; }
   @Override public BumpThread getThread()			{ return null; }
   @Override public long getWhen()				{ return 0; }
   @Override public Object getEventData()			{ return null; }

}	// end of inner class BaseEvent




/********************************************************************************/
/*										*/
/*	Launch management							*/
/*										*/
/********************************************************************************/

void handleLaunchEvent(Element xml)
{
   String reason = IvyXml.getAttrString(xml,"REASON");
   Element cnf = IvyXml.getChild(xml,"CONFIGURATION");
   String id = IvyXml.getAttrString(cnf,"ID");
   if (id == null) {
      BoardLog.logE("BUMP","Launch configuration without an ID: " + IvyXml.convertXmlToString(xml));
      return;
    }
   boolean ignore = IvyXml.getAttrBool(cnf,"IGNORE");
   if (ignore) return;

   ConfigEvent evt = null;

   if (reason.equals("REMOVE")) {
      LaunchConfig lc = known_configs.remove(id);
      if (lc != null) evt = new ConfigEvent(BumpRunEventType.LAUNCH_REMOVE,lc);
    }
   else {
      LaunchConfig lc = known_configs.get(id);
      if (lc == null) {
	 lc = new LaunchConfig(cnf);
	 known_configs.put(lc.getId(),lc);
	 evt = new ConfigEvent(BumpRunEventType.LAUNCH_ADD,lc);
       }
      else {
	 lc.update(cnf);
	 evt = new ConfigEvent(BumpRunEventType.LAUNCH_CHANGE,lc);
       }
    }

   if (evt != null) {
      for (BumpRunEventHandler reh : event_handlers) {
	 try {
	    reh.handleLaunchEvent(evt);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BUMP","Problem handling launch event",t);
	  }
       }
    }
}



private static class ConfigEvent extends BaseEvent {

   private LaunchConfig for_launch;
   private BumpRunEventType event_type;

   ConfigEvent(BumpRunEventType et,LaunchConfig lc) {
      event_type = et;
      for_launch = lc;
    }

   @Override public BumpRunEventType getEventType()		{ return event_type; }
   @Override public BumpLaunchConfig getLaunchConfiguration()	{ return for_launch; }

}	// end of inner class ConfigEvent




/********************************************************************************/
/*										*/
/*	Run event distribution methods						*/
/*										*/
/********************************************************************************/

synchronized void handleRunEvent(Element xml,long when)
{
   RunEventType type = IvyXml.getAttrEnum(xml,"TYPE",RunEventType.NONE);

   switch (type) {
      default :
      case NONE :
	 return;
      case PROCESS :
	 handleProcessEvent(xml,when);
	 break;
      case THREAD :
	 handleThreadEvent(xml,when);
	 break;
      case TARGET :
	 handleTargetEvent(xml,when);
	 break;
    }
}



/********************************************************************************/
/*										*/
/*	Console event distribution methods					*/
/*										*/
/********************************************************************************/

void handleConsoleEvent(Element xml)
{
   String id = IvyXml.getAttrString(xml,"PID");
   ProcessData bp = findProcess(xml);
   if (bp == null) {
      bp = console_processes.get(id);
    }
   if (bp == null) return;

   String message = IvyXml.getTextElement(xml,"TEXT");
   BumpConsoleMode mode = BumpConsoleMode.STDOUT;
   if (IvyXml.getAttrBool(xml,"STDERR")) mode = BumpConsoleMode.STDERR;
   else if (IvyXml.getAttrBool(xml,"SYSTEM")) mode = BumpConsoleMode.SYSTEM;
   boolean iseof = IvyXml.getAttrBool(xml,"EOF");

   for (BumpRunEventHandler reh : event_handlers) {
      try {
	 reh.handleConsoleMessage(bp,mode,iseof,message);
       }
      catch (Throwable t) {
	 BoardLog.logE("BUMP","Problem handling console event",t);
       }
    }

   if (iseof) {
      console_processes.remove(id);
    }
}




/********************************************************************************/
/*										*/
/*	Process event methods							*/
/*										*/
/********************************************************************************/

private void handleProcessEvent(Element xml,long when)
{
   RunEventKind kind = IvyXml.getAttrEnum(xml,"KIND",RunEventKind.NONE);
   Element proc = IvyXml.getChild(xml,"PROCESS");
   if (proc == null) return;
   String id = IvyXml.getAttrString(proc,"PID");
   ProcessData pd;

   if (IvyXml.getAttrBool(proc,"TERMINATED")) {
      if (kind == RunEventKind.CHANGE) kind = RunEventKind.TERMINATE;
    }

   ProcessEvent evt = null;

   switch (kind) {
      case TERMINATE :
	 evt = terminateProcess(id,when);
	 break;
      case CREATE :
      case CHANGE :
	 synchronized (active_processes) {
	    pd = active_processes.get(id);
            boolean term = IvyXml.getAttrBool(proc,"TERMINATED");
	    if (pd == null && !term) {
	       pd = new ProcessData(proc);
	       active_processes.put(id,pd);
	       console_processes.put(id,pd);
	       evt = new ProcessEvent(BumpRunEventType.PROCESS_ADD,pd);
	     }
            else if (term) {
               evt = terminateProcess(id,when);
             }
	    else {
	       pd.updateProcess(proc);
	       evt = new ProcessEvent(BumpRunEventType.PROCESS_CHANGE,pd);
	     }
	  }
	 break;
      default :
	 BoardLog.logW("BUMP","Unexpeced process event for Process " +
			  IvyXml.convertXmlToString(proc));
	 break;
    }

   if (evt != null) {
      for (BumpRunEventHandler reh : event_handlers) {
	 try {
	    reh.handleProcessEvent(evt);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BUMP","Problem handling process event",t);
	  }
       }
    }
}


private ProcessEvent terminateProcess(String id,long when)
{
   if (terminated_processes.remove(id) != null) {
      return null;
    }
   ProcessEvent evt = null;
   ProcessData pd = active_processes.remove(id);
   if (pd != null) {
      terminated_processes.put(id,pd);
      if (pd.getName() != null) named_processes.remove(pd.getName());
      evt = new ProcessEvent(BumpRunEventType.PROCESS_REMOVE,pd);
      for (Iterator<ThreadData> it = active_threads.values().iterator(); it.hasNext(); ) {
	 ThreadData td = it.next();
	 if (td.getProcess() == pd) {
	    handleTargetThreadState(td,BumpThreadState.DEAD,
		  BumpThreadStateDetail.NONE,when);
	    it.remove();
	  }
       }
    }
   else {
      BoardLog.logE("BUMP","Terminate process not found " + id + " " +
	    active_processes);
    }
   return evt;
}




private static class ProcessEvent extends BaseEvent {

   private ProcessData for_process;
   private BumpRunEventType event_type;

   ProcessEvent(BumpRunEventType et,ProcessData pd) {
      event_type = et;
      for_process = pd;
    }

   @Override public BumpRunEventType getEventType()		{ return event_type; }
   @Override public BumpProcess getProcess()			{ return for_process; }
   @Override public BumpLaunch getLaunch() {
      return for_process.getLaunch();
    }
   @Override public BumpLaunchConfig getLaunchConfiguration() {
      BumpLaunch bl = for_process.getLaunch();
      if (bl == null) return null;
      return bl.getConfiguration();
    }

}	// end of inner class ProcessEvent




/********************************************************************************/
/*										*/
/*	Thread event management methods 					*/
/*										*/
/********************************************************************************/

private void handleThreadEvent(Element xml,long when)
{
   RunEventKind kind = IvyXml.getAttrEnum(xml,"KIND",RunEventKind.NONE);
   BumpThreadStateDetail dtl = IvyXml.getAttrEnum(xml,"DETAIL",BumpThreadStateDetail.NONE);
   boolean iseval = IvyXml.getAttrBool(xml,"EVAL");

   Element thrd = IvyXml.getChild(xml,"THREAD");
   if (thrd == null) return;
   String id = IvyXml.getAttrString(thrd,"ID");
   ThreadData td;

   ThreadEvent evt = null;

   BumpThreadState ost = null;
   td = active_threads.get(id);
   if (td == null) {
      td = new ThreadData(thrd);
      String pid = IvyXml.getAttrString(thrd,"PID");
      if (pid != null && terminated_processes.containsKey(pid)) return;
      if (td.getProcess() == null) return;
      td.updateThread(thrd);
      active_threads.put(id,td);
      evt = new ThreadEvent(BumpRunEventType.THREAD_ADD,td,when);
      ost = td.getThreadState();
    }
   else {
      ost = td.getThreadState();
      td.updateThread(thrd);
      evt = new ThreadEvent(BumpRunEventType.THREAD_CHANGE,td,when);
    }

   // TODO: the set thread states below somewhat duplicate the updateThread call above

   switch (kind) {
      case CREATE :
	 switch (td.getThreadState()) {
	    case NONE :
	    case NEW :
	       td.setThreadState(BumpThreadState.RUNNING);
	       break;
	    default:
	       break;
	  }
	 break;
      case CHANGE :
	 break;
      case RESUME :
	 if (dtl == BumpThreadStateDetail.EVALUATION_IMPLICIT) return;
	 td.setThreadState(ost.getRunState(),dtl);
	 break;
      case SUSPEND :
	 if (dtl == BumpThreadStateDetail.EVALUATION_IMPLICIT && iseval) return;
	 else if (checkException(td,thrd)) {
	    td.setThreadState(ost.getExceptionState(),dtl);
	  }
	 else if (!td.getThreadState().isStopped()) {
	    td.setThreadState(ost.getStopState(),dtl);
	  }
	 else if (dtl == BumpThreadStateDetail.BREAKPOINT) {
	    td.setThreadState(BumpThreadState.STOPPED,dtl);
	  }
	 else if (dtl == BumpThreadStateDetail.EVALUATION_IMPLICIT) return;
	 else if (td.getThreadState().isStopped()) {
	    if (dtl != null) td.setThreadState(ost.getStopState(),dtl);
	 }
	 break;
      case TERMINATE :
	 td.setThreadState(BumpThreadState.DEAD);
	 evt = new ThreadEvent(BumpRunEventType.THREAD_REMOVE,td,when);
	 active_threads.remove(id);
	 thread_filters.remove(td);
	 break;
      default :
	 BoardLog.logW("BUMP","Unexpeced process event for Thread " +
			  IvyXml.convertXmlToString(xml));
	 evt = null;
	 break;
    }

   BumpRunEvent revt = evt;
   SwingEventListenerList<BumpThreadFilter> tll = thread_filters.get(td);
   if (tll != null) {
      for (BumpThreadFilter btf : tll) {
	 revt = btf.handleThreadEvent(td,revt);
       }
    }
   if (td.isInternal()) revt = null;
   td.setBreakpoint(null);
   if (revt != null && td.getThreadState().isStopped() &&
	 td.getThreadDetails() == BumpThreadStateDetail.BREAKPOINT) {
      BumpBreakModel bbm = bump_client.getBreakModel();
      BumpBreakpoint bbpt = null;
      Element bpt = IvyXml.getChild(thrd,"BREAKPOINT");
      if (bpt != null) {
	 bbpt = bbm.findBreakpoint(bpt);
	 if (bbpt != null && bbpt.getBoolProperty("TRACEPOINT")) {
	    BoardLog.logD("BUMP","Trace point reached");
	    revt = new ThreadEvent(BumpRunEventType.THREAD_TRACE,td,revt.getWhen());
	    bump_client.resume(td);
	  }
	 else td.setBreakpoint(bbpt);
       }
      else {
	 String file = IvyXml.getAttrString(thrd,"LOCFILE");
	 if (file != null) {
	    int lno = IvyXml.getAttrInt(thrd,"LOCLINE");
	    bbpt = bbm.findBreakpoint(new File(file),lno);
	  }
       }
      if (bbpt != null && bbpt.getBoolProperty("TRACEPOINT")) {
	 BoardLog.logD("BUMP","Trace point reached");
	 revt = new ThreadEvent(BumpRunEventType.THREAD_TRACE,td,revt.getWhen());
	 bump_client.resume(td);
       }
      else td.setBreakpoint(bbpt);
    }

   if (revt != null) {
      for (BumpRunEventHandler reh : event_handlers) {
	 try {
	    reh.handleThreadEvent(revt);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BUMP","Problem handling thread event",t);
	  }
       }
    }
}



private boolean checkException(ThreadData td,Element thrd)
{
   boolean fnd = false;
   td.setException(null);

   String exc = IvyXml.getAttrString(thrd,"EXCEPTION");
   if (exc != null) {
      td.setException(exc);
      return true;
    }

   for (Element bpt : IvyXml.children(thrd,"BREAKPOINT")) {
      String btyp = IvyXml.getAttrString(bpt,"TYPE");
      if (btyp != null && btyp.equals("EXCEPTION")) {
	 td.setException(IvyXml.getAttrString(bpt,"EXCEPTION"));
	 fnd = true;
       }
    }

   return fnd;
}




private static class ThreadEvent extends BaseEvent {

   private ThreadData for_thread;
   private BumpRunEventType event_type;
   private long event_time;

   ThreadEvent(BumpRunEventType et,ThreadData td,long when) {
      event_type = et;
      for_thread = td;
      event_time = when;
    }

   @Override public BumpRunEventType getEventType()		{ return event_type; }
   @Override public BumpThread getThread()			{ return for_thread; }
   @Override public BumpProcess getProcess() {
      return for_thread.getProcess();
    }
   @Override public BumpLaunch getLaunch() {
      return for_thread.getLaunch();
    }
   @Override public BumpLaunchConfig getLaunchConfiguration() {
      BumpLaunch bl = for_thread.getLaunch();
      if (bl == null) return null;
      return bl.getConfiguration();
    }
   @Override public long getWhen()				{ return event_time; }

}	// end of inner class ThreadEvent




/********************************************************************************/
/*										*/
/*	Target event management methods 					*/
/*										*/
/********************************************************************************/

private void handleTargetEvent(Element xml,long when)
{
   RunEventKind kind = IvyXml.getAttrEnum(xml,"KIND",RunEventKind.NONE);
   BumpThreadStateDetail dtl = IvyXml.getAttrEnum(xml,"DETAIL",BumpThreadStateDetail.NONE);
   Element tgt = IvyXml.getChild(xml,"TARGET");
   ProcessData pd = findProcess(tgt);
   if (pd == null) {
      Element lnch = IvyXml.getChild(tgt,"LAUNCH");
      LaunchData ld = findLaunch(lnch);
      if (ld != null && kind != RunEventKind.TERMINATE) pd = ld.getDefaultProcess();
    }
   if (pd == null) return;

   String nm = IvyXml.getAttrString(tgt,"NAME");
   if (nm != null) pd.setProcessName(nm);

   if (dtl == BumpThreadStateDetail.CONTENT) return;

   switch (kind) {
      case HOTCODE_SUCCESS :
	 ProcessEvent evts = new ProcessEvent(BumpRunEventType.HOTCODE_SUCCESS,pd);
	 sendProcessEvent(evts);
	 return;
      case HOTCODE_FAILURE :
	 ProcessEvent evtf = new ProcessEvent(BumpRunEventType.HOTCODE_FAILURE,pd);
	 sendProcessEvent(evtf);
	 return;
      default :
	 break;
    }

   for (BumpThread bt : pd.getThreads()) {
      ThreadData td = (ThreadData) bt;
      BumpThreadState ost = td.getThreadState();
      switch (kind) {
	 case SUSPEND :
	    handleTargetThreadState(td,ost.getStopState(),dtl,when);
	    break;
	 case RESUME :
	    handleTargetThreadState(td,ost.getRunState(),dtl,when);
	    break;
	 default:
	    break;
       }
    }

   if (kind == RunEventKind.TERMINATE) {
      ProcessEvent evt = terminateProcess(pd.getId(),when);
      if (evt != null) sendProcessEvent(evt);
    }
}


private void sendProcessEvent(ProcessEvent evt)
{
   for (BumpRunEventHandler reh : event_handlers) {
      try {
	 reh.handleProcessEvent(evt);
       }
      catch (Throwable t) {
	 BoardLog.logE("BUMP","Problem handling process event",t);
       }
    }
}






private void handleTargetThreadState(ThreadData td,BumpThreadState st,BumpThreadStateDetail dtl,
					long when)
{
   if (td.getThreadState() == st) return;
   BumpThreadState ost = td.getThreadState();

   if (ost == BumpThreadState.DEADLOCKED && st == BumpThreadState.BLOCKED) return;

   td.setThreadState(st,dtl);
   if (st.isStopped() && !ost.isStopped()) td.resetStack();

   ThreadEvent evt;
   if (st == BumpThreadState.DEAD)
      evt = new ThreadEvent(BumpRunEventType.THREAD_REMOVE,td,when);
   else
      evt = new ThreadEvent(BumpRunEventType.THREAD_CHANGE,td,when);

   for (BumpRunEventHandler reh : event_handlers) {
      try {
	 reh.handleThreadEvent(evt);
       }
      catch (Throwable t) {
	 BoardLog.logE("BUMP","Problem handling Thread target event",t);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Evaluation management							*/
/*										*/
/********************************************************************************/

void handleEvaluationResult(BumpStackFrame frm,Element xml,BumpEvaluationHandler hdlr)
{
   String eid = IvyXml.getAttrString(xml,"ID");
   String expr = IvyXml.getTextElement(xml,"EXPR");
   Element ex = IvyXml.getChild(xml,"EVAL");
   String saveid = IvyXml.getAttrString(xml,"SAVEID");
   if (ex == null) return;

   String sts = IvyXml.getAttrString(ex,"STATUS");
   if (sts.equals("EXCEPTION")) {
      String exc = IvyXml.getTextElement(ex,"EXCEPTION");
      hdlr.evaluationError(eid,expr,exc);
    }
   else if (sts.equals("ERROR")) {
      StringBuffer buf = new StringBuffer();
      for (Element er : IvyXml.children(ex,"ERROR")) {
	 buf.append(IvyXml.getText(er));
	 buf.append("\n");
       }
      hdlr.evaluationError(eid,expr,buf.toString());
    }
   else {
      Element val = IvyXml.getChild(ex,"VALUE");
      ValueData vd = null;
      if (val != null) vd = new ValueData((StackFrame) frm,val,saveid);
      hdlr.evaluationResult(eid,expr,vd);
    }
}




/********************************************************************************/
/*										*/
/*	Launch types								*/
/*										*/
/********************************************************************************/

private class LaunchType implements BumpLaunchType {

   private String type_name;
   private String type_description;
   private List<BumpLaunchConfigField> launch_fields;
   private boolean use_debug_args;
   private boolean test_case;

   LaunchType(Element xml) {
      type_name = IvyXml.getAttrString(xml, "NAME");
      type_description = IvyXml.getAttrString(xml, "DESCRIPTION");
      use_debug_args = IvyXml.getAttrBool(xml,"DEBUGARGS");
      test_case = IvyXml.getAttrBool(xml,"TESTCASE");
      launch_fields = new ArrayList<>();
      for (Element fld : IvyXml.children(xml,"ATTRIBUTE")) {
         launch_fields.add(new LaunchTypeField(fld));
      }
   }

   @Override public String getName()		{ return type_name; }
   @Override public String getDescription()	{ return type_description; }
   @Override public List<BumpLaunchConfigField> getFields() {
      return launch_fields;
   }
   @Override public boolean useDebugArgs()	{ return use_debug_args; }
   @Override public boolean isTestCase()	{ return test_case; }

}	// end of inner class LaunchType


private class LaunchTypeField implements BumpLaunchConfigField {

   private String field_name;
   private String field_description;
   private BumpLaunchConfigFieldType field_type;
   private String field_eval;
   private String field_option;
   private int num_rows;
   private int min_value;
   private int max_value;
   private String default_value;

   LaunchTypeField(Element xml) {
      field_name = IvyXml.getAttrString(xml, "NAME");
      field_description = IvyXml.getAttrString(xml, "DESCRIPTION",field_name);
      field_type = IvyXml.getAttrEnum(xml, "TYPE", BumpLaunchConfigFieldType.UNKNOWN);
      field_eval = IvyXml.getAttrString(xml, "EVAL");
      field_option = IvyXml.getAttrString(xml,"ARG");
      num_rows = IvyXml.getAttrInt(xml,"ROWS",1);
      min_value = IvyXml.getAttrInt(xml, "MIN",0);
      max_value = IvyXml.getAttrInt(xml, "MAX",min_value);
      default_value = IvyXml.getAttrString(xml,"VALUE");
   }

   @Override public String getFieldName()		{ return field_name; }
   @Override public String getDescription()		{ return field_description; }
   @Override public BumpLaunchConfigFieldType getType() { return field_type; }
   @Override public String getEvaluate()		{ return field_eval; }
   @Override public String getArgField()		{ return field_option; }
   @Override public int getNumRows()			{ return num_rows; }
   @Override public int getMin()			{ return min_value; }
   @Override public int getMax()			{ return max_value; }
   @Override public String getDefaultValue()		{ return default_value; }

}	// end of inner class LaunchTypeField



/********************************************************************************/
/*										*/
/*	Launch configuration information					*/
/*										*/
/********************************************************************************/

private class LaunchConfig implements BumpLaunchConfig {

   private String launch_id;
   private BumpLaunchType launch_type;
   private Element launch_xml;

   LaunchConfig(Element xml) {
      launch_id = IvyXml.getAttrString(xml,"ID");
      update(xml);
    }

   void update(Element xml) {
      BoardLog.logD("BUMP","Found Launch Config " + IvyXml.convertXmlToString(xml));
   
      launch_xml = xml;
   
      String tnm = IvyXml.getAttrString(xml, "TYPE");
      Element type = IvyXml.getChild(xml,"TYPE");
      if (type != null) {
         tnm = IvyXml.getAttrString(type,"NAME");
       }
      String tnm1 = tnm;
      if (tnm.equals("JUnit")) tnm1 = "JUnit Test";
      launch_type = null;
      BumpLaunchType dflttype = null;
      for (BumpLaunchType blt : launch_types) {
         if (blt.getName().equals(tnm) || blt.getDescription().equals(tnm) ||
               blt.getName().equals(tnm1) || blt.getDescription().equals(tnm1)) {
            launch_type = blt;
            break;
          }
         else if (dflttype == null) dflttype = blt;
       }
      if (launch_type == null) {
         BoardLog.logD("BUMP","Can't find launch type " + tnm + " " + tnm1);
         launch_type = dflttype;
       }
    }

   @Override public String getConfigName() {
      return IvyXml.getAttrString(launch_xml,"NAME");
    }
   @Override public String getProject() {
      return getAttribute(launch_xml,"PROJECT_ATTR");
    }
   @Override public String getMainClass() {
      return getAttribute(launch_xml,"MAIN_TYPE");
    }
   @Override public String getArguments() {
      return getAttribute(launch_xml,"PROGRAM_ARGUMENTS");
    }
   @Override public String getVMArguments() {
      return getAttribute(launch_xml,"VM_ARGUMENTS");
    }
   @Override public String getTestName() {
      return getAttribute(launch_xml,"TESTNAME");
    }
   @Override public String getLogFile() {
      return getAttribute(launch_xml,"CAPTURE_IN_FILE");
    }
   @Override public String getWorkingDirectory() {
      return getAttribute(launch_xml,"WORKING_DIRECTORY");
    }
   @Override public String getRemoteHost() {
      String h = getAttribute(launch_xml,"REMOTE_HOST");
      if (h != null) return h;
      String hmap = getAttribute(launch_xml,"CONNECT_MAP");
      if (hmap != null) {
	 Matcher m1 = HOST_PATTERN.matcher(hmap);
	 Matcher m2 = PORT_PATTERN.matcher(hmap);
	 if (m1.find() && m2.find()) {
	    return m1.group(1);
	  }
       }
      return null;
    }
   @Override public int getRemotePort() {
      Integer p = getInteger(launch_xml,"REMOTE_PORT");
      if (p != null) return p;
      String hmap = getAttribute(launch_xml,"CONNECT_MAP");
      if (hmap != null) {
	 Matcher m1 = HOST_PATTERN.matcher(hmap);
	 Matcher m2 = PORT_PATTERN.matcher(hmap);
	 if (m1.find() && m2.find()) {
	    return Integer.parseInt(m2.group(1));
	  }
       }
      return 8000;
    }
   @Override public boolean isWorkingCopy() {
      return IvyXml.getAttrBool(launch_xml,"WORKING");
    }
   @Override public boolean getStopInMain() {
      return getBoolean(launch_xml,"STOP_IN_MAIN",false);
    }

   boolean useContracts() {
      return getBoolean(launch_xml,"CONTRACTS",true);
    }

   boolean useAssertions() {
      return getBoolean(launch_xml,"ASSERTIONS",true);
    }

   @Override public String getId()				{ return launch_id; }
   @Override public BumpLaunchType getLaunchType()		{ return launch_type; }

   @Override public String getAttribute(String nm) {
      return getAttribute(launch_xml,nm);
   }

   @Override public boolean getBoolAttribute(String nm) {
      return getBoolean(launch_xml,nm,false);
   }


   @Override public String getContractArgs() {
      String args = null;

      BumpContractType bct = bump_client.getContractType(getProject());
      if (bct == null) return null;

      if (useContracts() && bct.useContractsForJava()) {
	 String libf = BoardSetup.getSetup().getLibraryPath("cofoja.jar");
	 args = "-javaagent:" + libf;
       }

      if (useAssertions() && bct.enableAssertions()) {
	 if (args == null) args = "-ea";
	 else args += " -ea";
       }

      return null;
    }

   @Override public BumpLaunchConfig clone(String name) {
      Element x = bump_client.getNewRunConfiguration(name,getId(),getLaunchType());
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig save() {
      Element x = bump_client.saveRunConfiguration(getId());
      LaunchConfig lc = getLaunchResult(x);
      if (lc != null) known_configs.put(lc.getId(),lc);
      return lc;
    }

   @Override public void delete() {
      bump_client.deleteRunConfiguration(getId());
    }

   @Override public BumpLaunchConfig setConfigName(String nm) {
      Element x = bump_client.editRunConfiguration(getId(),"NAME",nm);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setProject(String pnm) {
      if (pnm == null) pnm = "";
      Element x = bump_client.editRunConfiguration(getId(),"PROJECT_ATTR",pnm);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setMainClass(String cnm) {
      if (cnm == null) cnm = "";
      Element x = bump_client.editRunConfiguration(getId(),"MAIN_TYPE",cnm);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setArguments(String arg) {
      if (arg == null) arg = "";
      Element x = bump_client.editRunConfiguration(getId(),"PROGRAM_ARGUMENTS",arg);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setVMArguments(String arg) {
      if (arg == null) arg = "";
      Element x = bump_client.editRunConfiguration(getId(),"VM_ARGUMENTS",arg);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setTestName(String name) {
      if (name == null) name = "";
      Element x = bump_client.editRunConfiguration(getId(),"TESTNAME",name);
      return getLaunchResult(x);
    }



   @Override public BumpLaunchConfig setRemoteHostPort(String host,int port) {
      String val = "{port=" + port + ", hostname=" + host + "}";
      Element x = bump_client.editRunConfiguration(getId(),"CONNECT_MAP",val);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setRemoteHost(String host) {
      Element x = bump_client.editRunConfiguration(getId(),"REMOTE_HOST",host);
      BumpLaunchConfig blc = getLaunchResult(x);
      return blc.setRemoteHostPort(host,getRemotePort());
   }

   @Override public BumpLaunchConfig setRemotePort(int port) {
      Element x = bump_client.editRunConfiguration(getId(),"REMOTE_PORT",Integer.toString(port));
      BumpLaunchConfig blc = getLaunchResult(x);
      return blc.setRemoteHostPort(getRemoteHost(),port);
   }

   @Override public BumpLaunchConfig setLogFile(String arg) {
      Element x = bump_client.editRunConfiguration(getId(),"CAPTURE_IN_FILE",arg);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setWorkingDirectory(String arg) {
      Element x = bump_client.editRunConfiguration(getId(),"WORKING_DIRECTORY",arg);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setStopInMain(boolean fg) {
      String val = Boolean.toString(fg);
      Element x = bump_client.editRunConfiguration(getId(),"STOP_IN_MAIN",val);
      return getLaunchResult(x);
    }

   @Override public BumpLaunchConfig setAttribute(String attr,String arg) {
      if (attr.equals("REMOTE_HOST")) return setRemoteHost(arg);
      else if (attr.equals("REMOTE_PORT")) return setRemotePort(Integer.parseInt(arg));

      Element x = bump_client.editRunConfiguration(getId(),attr,arg);
      return getLaunchResult(x);
    }

   private String getAttribute(Element xml,String id) {
      for (Element ae : IvyXml.children(xml,"ATTRIBUTE")) {
	 String anm = IvyXml.getAttrString(ae,"NAME");
	 if (id.equals(anm)) {
	    return IvyXml.getText(ae);
	  }
       }
      return null;
    }

   private boolean getBoolean(Element xml,String id,boolean dflt) {
      String s = getAttribute(xml,id);
      if (s == null || s.length() == 0) return dflt;
      if ("tT1yY".indexOf(s.charAt(0)) >= 0) return true;
      return false;
    }

   private Integer getInteger(Element xml,String id) {
      String s = getAttribute(xml,id);
      if (s == null || s.isEmpty()) return null;
      try {
	 return Integer.parseInt(s);
       }
      catch (NumberFormatException e) { }
      return null;
    }

}	// end of inner class LanuchConfig



/********************************************************************************/
/*										*/
/*	Launch									*/
/*										*/
/********************************************************************************/

LaunchData findLaunch(Element xml)
{
   if (xml == null) return null;

   String id = IvyXml.getAttrString(xml,"ID");

   LaunchData ld = new LaunchData(xml);
   LaunchData xld = active_launches.putIfAbsent(id,ld);
   if (xld != null) ld = xld;

   return ld;
}



BumpProcess findDefaultProcess(Element xml)
{
   LaunchData ld = findLaunch(xml);
   if (ld == null) return null;
   return ld.getDefaultProcess();
}



private ProcessData createDefaultProcess(LaunchData ld)
{
   ProcessData pd = new ProcessData(ld);

   ProcessEvent evt = new ProcessEvent(BumpRunEventType.PROCESS_ADD,pd);
   for (BumpRunEventHandler reh : event_handlers) {
      try {
	 reh.handleProcessEvent(evt);
       }
      catch (Throwable t) {
	 BoardLog.logE("BUMP","Problem handling process event",t);
       }
    }

   return pd;
}



private class LaunchData implements BumpLaunch {

   private String launch_id;
   private LaunchConfig for_config;
   private boolean is_debug;
   private ProcessData default_process;

   LaunchData(Element xml) {
      launch_id = IvyXml.getAttrString(xml,"ID");
      String cid = IvyXml.getAttrString(xml,"CID");
      if (cid != null) for_config = known_configs.get(cid);
      else for_config = null;
      is_debug = IvyXml.getAttrString(xml,"MODE").equals("debug");
    }

   @Override public BumpLaunchConfig getConfiguration() 	{ return for_config; }
   @Override public boolean isDebug()				{ return is_debug; }

   @Override public String getId()				{ return launch_id; }

   synchronized ProcessData getDefaultProcess() {
      if (default_process == null) {
         default_process = createDefaultProcess(this);
       }
      return default_process;
    }

}	// end of inner class LaunchData




/********************************************************************************/
/*										*/
/*	Process 								*/
/*										*/
/********************************************************************************/

ProcessData findProcess(Element xml)
{
   if (xml == null) return null;

   String id = IvyXml.getAttrString(xml,"PID");
   if (id == null) id = IvyXml.getAttrString(xml,"PROCESS");
   if (id == null) return null;

   return active_processes.get(id);
}



void setProcessName(BumpProcess bp,String id)
{
   if (bp == null || id == null) return;
   ProcessData pd = (ProcessData) bp;
   pd.setName(id);
}



private class ProcessData implements BumpProcess {

   private String process_id;
   private String process_name;
   private boolean is_running;
   private LaunchData for_launch;

   ProcessData(Element xml) {
      process_id = IvyXml.getAttrString(xml,"PID");
      process_name = null;
      is_running = true;
      updateProcess(xml);
      if (perf_writer != null) perf_writer.println("START " + process_id);
      if (trie_writer != null) trie_writer.println("START " + process_id);
    }

   ProcessData(LaunchData ld) {
      process_id = ld.getId();
      process_name = "Default";
      is_running = true;
      for_launch = ld;
    }

   @Override public Iterable<BumpThread> getThreads() {
      List<BumpThread> rslt = new ArrayList<BumpThread>();
      for (ThreadData td : active_threads.values()) {
         if (td.getProcess() == this && !td.isInternal()) rslt.add(td);
       }
      return rslt;
    }

   @Override public BumpLaunch getLaunch()		{ return for_launch; }

   @Override public String getId()			{ return process_id; }

   @Override public boolean isDummy() {
      if (for_launch == null) return false;
      return process_id.equals(for_launch.getId());
    }

   @Override public synchronized String getName()	{ return process_name; }

   synchronized void setName(String id) {
      if (process_name != null) named_processes.remove(process_name);
      process_name = id;
      if (id != null) named_processes.put(id,this);
    }

   @Override public boolean isRunning() 		{ return is_running; }

   @Override public void requestSwingData(int x,int y) {
      String cmd = "SWING WHAT " + x + " " + y;
      MintControl mc = BoardSetup.getSetup().getMintControl();
      if (getName() == null) return;
      mc.send("<BANDAID CMD='" + cmd + "' ID='" + getName() + "' />");
    }

   void updateProcess(Element xml) {
      if (is_running && IvyXml.getAttrBool(xml,"TERMINATED")) is_running = false;
      for_launch = findLaunch(IvyXml.getChild(xml,"LAUNCH"));
      if (process_name == null) {
         String nm = IvyXml.getAttrString(xml,"NAME");
         if (nm != null) setProcessName(nm);
       }
    }

   synchronized void setProcessName(String nm) {
      if (process_name != null || nm == null) return;
      int idx = nm.lastIndexOf(" at ");
      if (idx < 0) return;
      // main_class = nm.substring(0,idx).trim();
      nm = nm.substring(idx+4).trim();
      idx = nm.lastIndexOf(":");
      if (idx < 0) return;
      String host = nm.substring(0,idx);
      String port = nm.substring(idx+1);
      if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0.0.0.0")) {
	 try {
	    host = InetAddress.getLocalHost().getHostName();
	  }
	 catch (UnknownHostException e) {
	    host = "localhost";
	  }
       }
      process_name = port + "@" + host;
      named_processes.put(process_name,this);
    }

   void handleBandaidData(long when,Element xml) {
      Map<String,ThreadData> ths = new HashMap<String,ThreadData>();
      for (ThreadData td : active_threads.values()) {
         // management thread id and eclipse thread id don't match -- need to use names
         // this has problems when there are threads with identical names
         if (td.getProcess() == this) ths.put(td.getName(),td);
       }
   
      Element x = IvyXml.getChild(xml,"STATES");
      for (Element tc : IvyXml.children(x,"THREAD")) {
         String id = IvyXml.getAttrString(tc,"NAME");
         ThreadData td = ths.get(id);
         if (td != null && td.handleBandaidData(tc)) {
            ThreadEvent evt = new ThreadEvent(BumpRunEventType.THREAD_CHANGE,td,when);
            for (BumpRunEventHandler reh : event_handlers) {
               try {
        	  reh.handleThreadEvent(evt);
        	}
               catch (Throwable t) {
        	  BoardLog.logE("BUMP","Problem handling state event",t);
        	}
             }
          }
         else if (td == null) {
   //       BoardLog.logD("BUMP","Can't find thread " + id + " " + IvyXml.convertXmlToString(tc));
          }
       }
   
      Element dx = IvyXml.getChild(xml,"DEADLOCKS");
      if (dx != null) {
         for (Element de : IvyXml.children(dx,"DEADLOCK")) {
            for (Element te : IvyXml.children(de,"THREAD")) {
               String id = IvyXml.getAttrString(te,"NAME");
               ThreadData td = ths.get(id);
               if (td != null && td.handleBandaidDeadlock()) {
        	  ThreadEvent evt = new ThreadEvent(BumpRunEventType.THREAD_CHANGE,td,when);
        	  for (BumpRunEventHandler reh : event_handlers) {
        	     try {
        		reh.handleThreadEvent(evt);
        	      }
        	     catch (Throwable t) {
        		BoardLog.logE("BUMP","Problem handling deadlock state event",t);
        	      }
        	   }
        	}
             }
          }
       }
   
      Element px = IvyXml.getChild(xml,"CPUPERF");
      if (px != null) {
         if (perf_writer != null) {
            perf_writer.println(IvyXml.convertXmlToString(px));
            perf_writer.flush();
          }
         ProcessPerfEvent ppe = new ProcessPerfEvent(this,px);
         for (BumpRunEventHandler reh : event_handlers) {
            try {
               reh.handleProcessEvent(ppe);
             }
            catch (Throwable t) {
               BoardLog.logE("BUMP","Problem handling performance event",t);
             }
          }
       }
      Element tx = IvyXml.getChild(xml,"TRIE");
      if (tx != null) {
         if (trie_writer != null) {
            trie_writer.println(IvyXml.convertXmlToString(tx));
            trie_writer.flush();
          }
         ProcessTrieEvent pte = new ProcessTrieEvent(this,tx);
         for (BumpRunEventHandler reh : event_handlers) {
            try {
               reh.handleProcessEvent(pte);
             }
            catch (Throwable t) {
               BoardLog.logE("BUMP","Problem handling trie event",t);
             }
          }
       }
      Element rx = IvyXml.getChild(xml,"TRACE");
      if (rx != null) {
         // BoardLog.logD("BUMP","TRACE DATA: " + IvyXml.convertXmlToString(rx));
         ProcessTraceEvent pre = new ProcessTraceEvent(this,rx);
         for (BumpRunEventHandler reh : event_handlers) {
            try {
               reh.handleProcessEvent(pre);
             }
            catch (Throwable t) {
               BoardLog.logE("BUMP","Problem handling trace event",t);
             }
          }
       }
   }

}	// end of inner class ProcessData




/********************************************************************************/
/*										*/
/*	Thread representation							*/
/*										*/
/********************************************************************************/

private ThreadData findThread(String id)
{
   return active_threads.get(id);
}



private class ThreadData implements BumpThread {

   private String thread_id;
   private String thread_name;
   private String thread_group;
   private BumpThreadType thread_type;
   private boolean is_daemon;
   private int	   num_frames;
   private BumpThreadState thread_state;
   private BumpThreadStateDetail thread_detail;
   private LaunchData for_launch;
   private ProcessData for_process;
   private StackData stack_data;
   private long cpu_time;
   private long user_time;
   private long block_time;
   private long wait_time;
   private int block_count;
   private int wait_count;
   private String exception_type;
   private BumpBreakpoint current_breakpoint;

   ThreadData(Element xml) {
      thread_id = IvyXml.getAttrString(xml,"ID");
      thread_state = BumpThreadState.NONE;
      thread_type = BumpThreadType.UNKNOWN;
      thread_detail = BumpThreadStateDetail.NONE;
      updateThread(xml);
      stack_data = null;
      cpu_time = -1;
      user_time = -1;
      block_time = -1;
      wait_time = -1;
      block_count = -1;
      wait_count = -1;
      exception_type = null;
      current_breakpoint = null;
    }

   void updateThread(Element xml) {
      if (!IvyXml.isElement(xml,"THREAD")) xml = IvyXml.getChild(xml,"THREAD");
      String val = IvyXml.getAttrString(xml,"NAME");
      if (val != null) thread_name = val;
      val = IvyXml.getAttrString(xml,"GROUP");
      if (val != null) thread_group = val;
   
      if (IvyXml.getAttrBool(xml,"SYSTEM")) thread_type = BumpThreadType.SYSTEM;
      else {
         BumpThreadType btt = null;
         if (thread_name != null) btt = known_threads.get(thread_name);
         if (btt == null) btt = BumpThreadType.USER;
         thread_type = btt;		// don't allow thread_type to be null, even temporarily
       }
   
      is_daemon = IvyXml.getAttrBool(xml,"DAEMON");
      if (IvyXml.getAttrBool(xml,"STACK")) num_frames = IvyXml.getAttrInt(xml,"FRAMES",1);
      else num_frames = -1;
      if (IvyXml.getAttrBool(xml,"TERMINATED")) thread_state = BumpThreadState.DEAD;
      else if (IvyXml.getAttrBool(xml,"SUSPENDED")) thread_state = thread_state.getStopState();
      for_launch = findLaunch(IvyXml.getChild(xml,"LAUNCH"));
      for_process = findProcess(xml);
      if (for_process == null && !IvyXml.getAttrPresent(xml,"PID") && for_launch != null) {
         for_process = for_launch.getDefaultProcess();
       }
      exception_type = null;
      current_breakpoint = null;
    }

   void setThreadState(BumpThreadState ts) {
      setThreadState(ts,BumpThreadStateDetail.NONE);
    }

   synchronized void setThreadState(BumpThreadState ts,BumpThreadStateDetail dtl) {
      BoardLog.logD("BUMP","SET STATE OF " + thread_name + " TO " + ts);
      thread_state = ts;
      stack_data = null;
      thread_detail = dtl;
    }


   @Override public String getName()				{ return thread_name; }
   @Override public String getGroupName()			{ return thread_group; }
   @Override public BumpThreadState getThreadState()		{ return thread_state; }
   @Override public BumpThreadStateDetail getThreadDetails()	{ return thread_detail; }
   @Override public BumpThreadType getThreadType()		{ return thread_type; }
   @Override public boolean isDaemonThread()			{ return is_daemon; }
   @Override public BumpLaunch getLaunch()			{ return for_launch; }
   @Override public BumpProcess getProcess()			{ return for_process; }
   @Override public String getId()				{ return thread_id; }
   @Override public long getCpuTime()				{ return cpu_time; }
   @Override public long getUserTime()				{ return user_time; }
   @Override public long getBlockTime() 			{ return block_time; }
   @Override public long getWaitTime()				{ return wait_time; }
   @Override public int getBlockCount() 			{ return block_count; }
   @Override public int getWaitCount()				{ return wait_count; }
   @Override public String getExceptionType()			{ return exception_type; }
   @Override public BumpBreakpoint getBreakpoint()		{ return current_breakpoint; }

   void resetStack() {
      Element xml = bump_client.getThreadStack(this);
      if (xml == null) {
	 stack_data = null;
	 num_frames = -1;
       }
      else {
	 stack_data = new StackData(xml,thread_id);
	 num_frames = stack_data.getNumFrames();
       }
      current_breakpoint = null;
    }

   @Override public BumpThreadStack getStack() {
      if (num_frames <= 0) return null;
      if (stack_data == null) {
	 Element xml = bump_client.getThreadStack(this);
	 if (xml != null) {
	    stack_data = new StackData(xml,thread_id);
	    num_frames = stack_data.getNumFrames();
	  }
       }

      return stack_data;
    }

   boolean isInternal() {
      if (thread_name == null) return false;
      return thread_name.equals(BandaidConstants.BANDAID_THREAD);
    }

   synchronized boolean handleBandaidData(Element xml) {
      boolean chng = false;
      BumpThreadState state = IvyXml.getAttrEnum(xml,"STATE",thread_state);
      if (state == BumpThreadState.BLOCKED &&
	    thread_state == BumpThreadState.DEADLOCKED)
	 state = thread_state;
      else if (state == BumpThreadState.STOPPED_BLOCKED &&
	    thread_state == BumpThreadState.STOPPED_DEADLOCK)
	 state = thread_state;

      if (state != thread_state) {
	 if (state.isRunning() == thread_state.isRunning() && !thread_state.isException()) {
	    chng = true;
	    thread_state = state;
	    // System.err.println("SET BANDAID STATE OF " + thread_name + " TO " + state);
	 }
       }
      cpu_time = IvyXml.getAttrLong(xml,"CPUTM");
      user_time = IvyXml.getAttrLong(xml,"USERTM");
      wait_time = IvyXml.getAttrLong(xml,"WAITTM");
      wait_count = IvyXml.getAttrInt(xml,"WAITCT");
      block_time = IvyXml.getAttrLong(xml,"BLOCKTM");
      block_count = IvyXml.getAttrInt(xml,"BLOCKCT");
      return chng;
    }


   boolean handleBandaidDeadlock()
   {
      BumpThreadState ts = BumpThreadState.STOPPED_DEADLOCK;
      if (thread_state.isRunning()) ts = BumpThreadState.DEADLOCKED;
      if (ts == thread_state) return false;
      thread_state = ts;
      return true;
   }


   @Override public void requestHistory() {
      String cmd = "HISTORY DUMP " + getName();
      MintControl mc = BoardSetup.getSetup().getMintControl();
      if (for_process.getName() == null) return;

      mc.send("<BANDAID CMD='" + cmd + "' ID='" + for_process.getName() + "' />");
    }

   void setException(String typ)			{ exception_type = typ; }
   void setBreakpoint(BumpBreakpoint bp)		{ current_breakpoint = bp; }

}	// end of inner class ThreadData



/********************************************************************************/
/*										*/
/*	Stack representation							*/
/*										*/
/********************************************************************************/

private class StackData implements BumpThreadStack {

   private ThreadData for_thread;
   private List<StackFrame> stack_frames;

   StackData(Element xml,String tid) {
      if (!IvyXml.isElement(xml,"STACKFRAMES")) xml = IvyXml.getChild(xml,"STACKFRAMES");
      stack_frames = new ArrayList<>();
      for (Element telt : IvyXml.children(xml,"THREAD")) {
	 String teid = IvyXml.getAttrString(telt,"ID");
	 if (tid.equals(teid)) {
	    for_thread = findThread(tid);
	    for (Element e : IvyXml.children(telt,"STACKFRAME")) {
	       stack_frames.add(new StackFrame(for_thread,e,stack_frames.size()));
	     }
	    break;
	  }
       }
    }

   @Override public BumpThread getThread()	{ return for_thread; }

   @Override public int getNumFrames()		{ return stack_frames.size(); }
   @Override public BumpStackFrame getFrame(int i) {
      if (i < 0 || i >= stack_frames.size()) return null;
      return stack_frames.get(i);
    }

}	// end of inner class StackData




private class StackFrame implements BumpStackFrame {

   private ThreadData for_thread;
   private String frame_id;
   private String method_name;
   private String class_name;
   private String raw_signature;
   private String method_signature;
   private File for_file;
   private int line_number;
   private int frame_level;
   private boolean is_static;
   private boolean is_classfile;
   private boolean is_synthetic;
   private Map<String,ValueData> variable_map;

   StackFrame(ThreadData thrd,Element xml,int lvl) {
      for_thread = thrd;
      frame_id = IvyXml.getAttrString(xml,"ID");
      class_name = IvyXml.getAttrString(xml,"RECEIVER");
      method_name = IvyXml.getAttrString(xml,"METHOD");
      String fnm = IvyXml.getAttrString(xml,"FILE");
      if (fnm == null) {
         for_file = null;
         is_classfile = true;
       }
      else if (IvyXml.getAttrString(xml,"FILETYPE").equals("CLASSFILE")) {
         is_classfile = true;
         for_file = null;
         int soff = IvyXml.getAttrInt(xml,"SOURCEOFF",-1);
         int slen = IvyXml.getAttrInt(xml,"SOURCELEN",-1);
         if (soff >= 0 && slen >= 0) {
            synchronized (source_map) {
               for_file = source_map.get(fnm);
               if (for_file == null) {
        	  try {
        	     String xnm = fnm;
        	     int idx = xnm.indexOf("<");
        	     if (idx >= 0) xnm = xnm.substring(0,idx);
        	     for_file = File.createTempFile("BUBBLES_" + xnm,".java");
        	     source_map.put(fnm,for_file);
        	     byte [] data = IvyXml.stringToByteArray(IvyXml.getTextElement(xml,"SOURCE"));
        	     if (data == null) for_file = null;
        	     else {
        		FileOutputStream fos = new FileOutputStream(for_file);
        		fos.write(data);
        		fos.close();
        	      }
        	   }
        	  catch (IOException e) {
        	     BoardLog.logE("BUMP","Problem writing source file: " + e,e);
        	   }
        	  if (for_file != null) for_file.deleteOnExit();
        	}
             }
          }
       }
      else {
         for_file = new File(fnm);
         is_classfile = false;
       }
   
      line_number = IvyXml.getAttrInt(xml,"LINENO");
      is_static = IvyXml.getAttrBool(xml,"STATIC");
      is_synthetic = IvyXml.getAttrBool(xml,"SYNTHETIC");
      String sgn = IvyXml.getAttrString(xml,"SIGNATURE");
      if (sgn != null) {
         raw_signature = sgn;
         int sidx = sgn.lastIndexOf(")");
         if (sidx > 0) sgn = sgn.substring(0,sidx+1);
         method_signature = IvyFormat.formatTypeName(sgn);
      }
      frame_level = lvl;
   
      variable_map = new HashMap<String,ValueData>();
      for (Element e : IvyXml.children(xml,"VALUE")) {
         ValueData vd = new ValueData(this,e,null);
         variable_map.put(vd.getName(),vd);
       }
    }

   @Override public BumpThread getThread()		{ return for_thread; }
   @Override public String getFrameClass()		{ return class_name; }
   @Override public String getMethod()			{ return method_name; }

   @Override public String getSignature()		{ return method_signature; }
   @Override public String getRawSignature()		{ return raw_signature; }
   @Override public File getFile()			{ return for_file; }
   @Override public int getLineNumber() 		{ return line_number; }
   @Override public String getId()			{ return frame_id; }
   @Override public int getLevel()			{ return frame_level; }
   @Override public boolean isStatic()			{ return is_static; }
   @Override public boolean isSystem()			{ return is_classfile; }
   @Override public boolean isSynthetic()		{ return is_synthetic; }

   @Override public Collection<String> getVariables() {
      return new ArrayList<String>(variable_map.keySet());
    }
   @Override public BumpRunValue getValue(String var) {
      return variable_map.get(var);
    }

   @Override public boolean evaluate(String expr,BumpEvaluationHandler hdlr) {
      return bump_client.evaluateExpression(this,expr,false,true,hdlr);
    }

   @Override public boolean evaluateInternal(String expr,String saveid,BumpEvaluationHandler hdlr) {
      return bump_client.evaluateExpression(this,expr,true,false,saveid,hdlr);
    }

   @Override public boolean match(BumpStackFrame frm) {
      if (frm == null) return false;
      if (getThread() != frm.getThread()) return false;
      if (getMethod() != null) {
	 if (!getMethod().equals(frm.getMethod())) return false;
       }
      else if (frm.getMethod() != null) return false;

      if (getSignature() != null) {
	 if (!getSignature().equals(frm.getSignature())) return false;
       }
      else if (frm.getSignature() != null) return false;
      if (getLineNumber() != frm.getLineNumber()) return false;

      return true;
    }

   @Override public boolean sameFrame(BumpStackFrame frm) {
      if (frm == null) return false;
      if (frame_id.equals(frm.getId())) return true;
      return false;
    }

   @Override public String getDisplayString() {
      String mnm = getMethod();
      String cnm = null;
      int idx = mnm.lastIndexOf(".");
      if (idx > 0) {
	 cnm = mnm.substring(0,idx);
	 mnm = mnm.substring(idx+1);
       }
      StringBuffer buf = new StringBuffer();
      buf.append(getLineNumber());
      buf.append(" @ ");
      buf.append(mnm);
      if (cnm != null) {
	 buf.append(" / ");
	 buf.append(cnm);
       }
      return buf.toString();
    }


}	// end of inner class StackFrame



private class ValueData implements BumpRunValue {

   private StackFrame for_frame;
   private BumpValueKind val_kind;
   private String save_id;
   private String val_name;
   private String val_type;
   private String val_value;
   private String decl_type;
   private boolean has_values;
   private boolean is_local;
   private boolean is_static;
   private int array_length;
   private Map<String,ValueData> sub_values;
   private String var_detail;

   ValueData(StackFrame frm,Element xml,String saveid) {
      for_frame = frm;
      val_name = IvyXml.getAttrString(xml,"NAME");
      save_id = saveid;
      initialize(xml);
    }

   ValueData(ValueData par,Element xml) {
      for_frame = par.for_frame;
      val_name = par.val_name + "?" + IvyXml.getAttrString(xml,"NAME");
      initialize(xml);
    }

   private void initialize(Element xml) {
      val_kind = IvyXml.getAttrEnum(xml,"KIND",BumpValueKind.UNKNOWN);
      val_type = IvyXml.getAttrString(xml,"TYPE");
      val_value = IvyXml.getTextElement(xml,"DESCRIPTION");
      if (val_value == null) val_value = "";
      has_values = IvyXml.getAttrBool(xml,"HASVARS");
      is_local = IvyXml.getAttrBool(xml,"LOCAL");
      is_static = IvyXml.getAttrBool(xml,"STATIC");
      decl_type = IvyXml.getAttrString(xml,"DECLTYPE");
      array_length = IvyXml.getAttrInt(xml,"LENGTH",0);
      if (save_id == null) save_id = IvyXml.getAttrString(xml, "SAVEID");
      sub_values = null;
      var_detail = null;
      if (IvyXml.getAttrBool(xml,"CHARS")) {
         int len = IvyXml.getAttrInt(xml,"LENGTH");
         val_value = IvyXml.decodeCharacters(val_value,len);
       }
      addValues(xml);
    }

   @Override public BumpValueKind getKind()	{ return val_kind; }
   @Override public String getName()		{ return val_name; }
   @Override public String getType()		{ return val_type; }
   @Override public String getValue()		{ return val_value; }
   @Override public String getDeclaredType()	{ return decl_type; }
   @Override public String getActualType()	{ return null; }
   @Override public boolean hasContents()	{ return has_values; }
   @Override public boolean isLocal()		{ return is_local; }
   @Override public boolean isStatic()		{ return is_static; }
   @Override public BumpStackFrame getFrame()	{ return for_frame; }
   @Override public BumpThread getThread()	{ return for_frame.getThread(); }
   @Override public int getLength()		{ return array_length; }

   @Override public Collection<String> getVariables() {
      computeValues();
      if (sub_values == null) return Collections.emptyList();
      return new ArrayList<String>(sub_values.keySet());
    }

   @Override public BumpRunValue getValue(String var) {
      computeValues();
      if (sub_values == null) return null;
      return sub_values.get(var);
    }

   private String getShortName() {
      if (save_id != null) return "*" + save_id;
      return val_name;
    }


   private void computeValues() {
      if (!has_values || sub_values != null) return;
      Element xml = bump_client.getVariableValue(for_frame,val_name,save_id,1);
      if (IvyXml.isElement(xml,"RESULT")) {
         Element root = IvyXml.getChild(xml,"VALUE");
         addValues(root);
      }
    }

   private void addValues(Element xml) {
      if (xml == null) return;
      for (Element e : IvyXml.children(xml,"VALUE")) {
	 if (sub_values == null) sub_values = new HashMap<>();
	 ValueData vd = new ValueData(this,e);
	 sub_values.put(vd.getName(),vd);
       }
    }

   @Override public String getDetail() {
      if (var_detail == null) {
	 var_detail = bump_client.getVariableDetail(for_frame,getShortName());
	 if (var_detail == null) var_detail = val_value;
       }

      return var_detail;
    }

}	// end of inner class ValueData





/********************************************************************************/
/*										*/
/*	Handle step user commands						*/
/*										*/
/********************************************************************************/

private class StepUserFilter implements BumpThreadFilter {

   private BumpThreadStack initial_stack;

   StepUserFilter(BumpThread bt) {
      initial_stack = bt.getStack();
    }

   @Override public BumpRunEvent handleThreadEvent(BumpThread bt,BumpRunEvent evt) {
      if (evt == null || evt.getEventType() != BumpRunEventType.THREAD_CHANGE) return evt;
      if (!bt.getThreadState().isStopped()) return evt;
      BumpThreadStack stk = bt.getStack();
      if (stk == null || stk.getNumFrames() == 0 || initial_stack == null ||
            stk.getNumFrames() < initial_stack.getNumFrames()) {
         removeThreadFilter(bt,this);
         return evt;
       }
      BumpStackFrame frm = stk.getFrame(0);
      BumpStackFrame frm0 = initial_stack.getFrame(0);
   
      BoardLog.logD("BUMP","STEP FILTER " + frm.getMethod());
   
      if (stk.getNumFrames() == initial_stack.getNumFrames()) {
         if (frm.getMethod().equals(frm0.getMethod()) &&
        	(frm.getLineNumber() == frm0.getLineNumber() || frm.getLineNumber() == 1) &&
        	frm.getClass().equals(frm0.getClass()) &&
           bt.getExceptionType() == null)  {
            bump_client.stepInto(bt);
            return null;
          }
         removeThreadFilter(bt,this);
         return evt;
       }
   
      File f = frm.getFile();
      if (f != null) {
         boolean ex = f.exists();
         if (BoardSetup.getSetup().getRunMode() == BoardSetup.RunMode.CLIENT) ex = true;
         if (ex && frm.getLineNumber() > 1 && !isTempFile(f) && !isIgnore(frm)) {
            String mnm = frm.getMethod();
            int idx = mnm.lastIndexOf(".");
            if (idx >= 0) mnm = mnm.substring(idx+1);
            if (!mnm.contains("$") || mnm.startsWith("lambda$")) {
               removeThreadFilter(bt,this);
               return evt;
             }
          }
       }
      if (bt.getThreadDetails() == BumpThreadStateDetail.BREAKPOINT) {
         removeThreadFilter(bt,this);
         return evt;
       }
   
      if (frm0 != null) {
         BoardLog.logD("BUMP","USER_STEP " + frm.getMethod() + " " + stk.getNumFrames() + " " +
               initial_stack.getNumFrames() + " " + frm0.getMethod() + " " +
               frm.getLineNumber() + " " + frm0.getLineNumber() + " " +
               frm.getFrameClass() + " " + frm0.getFrameClass());
       }
      else {
         BoardLog.logD("BUMP","USER_STEP_NOPREV " + frm.getMethod() + " " + stk.getNumFrames() + " " +
               initial_stack.getNumFrames() + " " +
               frm.getLineNumber() + " " +
               frm.getFrameClass());
       }
   
      if (frm.getFrameClass().startsWith("java.lang.invoke")) bump_client.stepInto(bt);
      else if (frm.getMethod().contains("access$")) bump_client.stepInto(bt);
      else if (frm.getLineNumber() == 1) bump_client.stepInto(bt);
      else bump_client.stepReturn(bt);
      return null;
    }

}	// end of inner class StepUserFilter



private boolean isTempFile(File f)
{
   String d = System.getProperty("java.io.tmpdir");
   if (f.getParent().equals(d)) return true;
   if (f.getName().startsWith("BUBBLES_")) return true;
   return false;
}


private boolean isIgnore(BumpStackFrame frm)
{
   if (frm == null || frm.getFrameClass() == null) return false;
   if (frm.getFrameClass().startsWith("edu.brown.cs.bubbles.bandaid.")) return true;
   return false;
}



/********************************************************************************/
/*										*/
/*	Handlers for debugging messages from Bandaid				*/
/*										*/
/********************************************************************************/

private final class BandaidHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String pid = args.getArgument(0);
      long now = args.getLongArgument(1);
      Element xml = args.getXmlArgument(2);
      ProcessData pd = named_processes.get(pid);

      if (pd == null) return;
      pd.handleBandaidData(now,xml);
    }

}	// end of inner class BandaidHandler




private final class BandaidHistoryHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String pid = args.getArgument(0);
      String tnm = args.getArgument(1);
      Element xml = msg.getXml();

      ProcessData pd = named_processes.get(pid);
      if (pd == null) return;
      for (ThreadData td : active_threads.values()) {
	 if (td.getProcess() == pd && !td.isInternal() && td.getName().equals(tnm)) {
	    ThreadHistoryEvent the = new ThreadHistoryEvent(td,xml);
	    for (BumpRunEventHandler reh : event_handlers) {
	       try {
		  reh.handleThreadEvent(the);
		}
	       catch (Throwable t) {
		  BoardLog.logE("BUMP","Problem handling history event",t);
		}
	     }
	    break;
	  }
       }
    }

}	// end of inner class BandaidHandler




private static class ThreadHistoryEvent extends BaseEvent {

   private ThreadData for_thread;
   private Element stop_data;

   ThreadHistoryEvent(ThreadData td,Element stopdata) {
      for_thread = td;
      stop_data = stopdata;
    }

   @Override public BumpRunEventType getEventType()	{ return BumpRunEventType.THREAD_HISTORY; }
   @Override public BumpThread getThread()		{ return for_thread; }
   @Override public BumpProcess getProcess()		{ return for_thread.getProcess(); }
   @Override public BumpLaunch getLaunch()		{ return for_thread.getLaunch(); }
   @Override public BumpLaunchConfig getLaunchConfiguration() {
      BumpLaunch bl = for_thread.getLaunch();
      if (bl == null) return null;
      return bl.getConfiguration();
    }
   @Override public long getWhen()			{ return 0; }
   @Override public Object getEventData()		{ return stop_data; }

}	// end of inner class ThreadHistoryEvent




/********************************************************************************/
/*										*/
/*	Performance management							*/
/*										*/
/********************************************************************************/

private static class ProcessPerfEvent extends BaseEvent {

   private ProcessData for_process;
   private Element cpu_data;

   ProcessPerfEvent(ProcessData pd,Element cpudata) {
      for_process = pd;
      cpu_data = cpudata;
    }

   @Override public BumpRunEventType getEventType()	{ return BumpRunEventType.PROCESS_PERFORMANCE; }
   @Override public BumpProcess getProcess()		{ return for_process; }
   @Override public BumpLaunch getLaunch()		{ return for_process.getLaunch(); }
   @Override public BumpLaunchConfig getLaunchConfiguration() {
      BumpLaunch bl = for_process.getLaunch();
      if (bl == null) return null;
      return bl.getConfiguration();
    }
   @Override public long getWhen()			{ return 0; }
   @Override public Object getEventData()		{ return cpu_data; }

}	// end of inner class ProcessPerfEvent



/********************************************************************************/
/*										*/
/*	Trie management 						*/
/*										*/
/********************************************************************************/

private static class ProcessTrieEvent extends BaseEvent {

   private ProcessData for_process;
   private Element trie_data;

   ProcessTrieEvent(ProcessData pd,Element cpudata) {
      for_process = pd;
      trie_data = cpudata;
    }

   @Override public BumpRunEventType getEventType()	{ return BumpRunEventType.PROCESS_TRIE; }
   @Override public BumpProcess getProcess()		{ return for_process; }
   @Override public BumpLaunch getLaunch()		{ return for_process.getLaunch(); }
   @Override public BumpLaunchConfig getLaunchConfiguration() {
      BumpLaunch bl = for_process.getLaunch();
      if (bl == null) return null;
      return bl.getConfiguration();
    }
   @Override public long getWhen()			{ return 0; }
   @Override public Object getEventData()		{ return trie_data; }

}	// end of inner class ProcessPerfEvent



/********************************************************************************/
/*										*/
/*	Trace management							*/
/*										*/
/********************************************************************************/

private static class ProcessTraceEvent extends BaseEvent {

   private ProcessData for_process;
   private Element trace_data;

   ProcessTraceEvent(ProcessData pd,Element tracedata) {
      for_process = pd;
      trace_data = tracedata;
    }

   @Override public BumpRunEventType getEventType()	{ return BumpRunEventType.PROCESS_TRACE; }
   @Override public BumpProcess getProcess()		{ return for_process; }
   @Override public BumpLaunch getLaunch()		{ return for_process.getLaunch(); }
   @Override public BumpLaunchConfig getLaunchConfiguration() {
      BumpLaunch bl = for_process.getLaunch();
      if (bl == null) return null;
      return bl.getConfiguration();
    }
   @Override public long getWhen()			{ return 0; }
   @Override public Object getEventData()		{ return trace_data; }

}	// end of inner class ProcessPerfEvent




/********************************************************************************/
/*										*/
/*	Swing debugging management						*/
/*										*/
/********************************************************************************/

private final class BandaidSwingHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String pid = args.getArgument(0);
      ProcessData pd = named_processes.get(pid);
      if (pd == null) return;
      BoardLog.logD("BUMP","Swing return: " + msg.getText());

      Element xml = args.getXmlArgument(1);
      SwingEvent se = new SwingEvent(pd,xml);
      for (BumpRunEventHandler reh : event_handlers) {
	 try {
	    reh.handleProcessEvent(se);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BUMP","Problem handling swing event",t);
	  }
       }
    }

}	// end of inner class BandaidHandler



private static class SwingEvent extends BaseEvent {

   private ProcessData for_process;
   private Element swing_data;

   SwingEvent(ProcessData pd,Element data) {
      for_process = pd;
      swing_data = data;
    }

   @Override public BumpRunEventType getEventType()	{ return BumpRunEventType.PROCESS_SWING; }
   @Override public BumpProcess getProcess()		{ return for_process; }
   @Override public BumpLaunch getLaunch()		{ return for_process.getLaunch(); }
   @Override public BumpLaunchConfig getLaunchConfiguration() {
      BumpLaunch bl = for_process.getLaunch();
      if (bl == null) return null;
      return bl.getConfiguration();
    }
   @Override public long getWhen()			{ return 0; }
   @Override public Object getEventData()		{ return swing_data; }

}	// end of inner class ThreadHistoryEvent





}	// end of class BumpRunManager




/* end of BumpRunManager.java */
