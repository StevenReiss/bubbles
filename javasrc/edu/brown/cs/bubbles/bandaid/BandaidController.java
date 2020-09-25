/********************************************************************************/
/*										*/
/*		BandaidController.java						*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data system controller		*/
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



package edu.brown.cs.bubbles.bandaid;



import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.management.*;
import java.net.*;
import java.util.*;


public final class BandaidController implements BandaidConstants {



/********************************************************************************/
/*										*/
/*	Agent entry points							*/
/*										*/
/********************************************************************************/

public static void premain(String args,Instrumentation inst)
{
   the_control = new BandaidController(args,inst);
}



public static void agentmain(String args,Instrumentation inst)
{
   if (the_control == null) the_control = new BandaidController(args,inst);
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String			host_name;
private int			port_number;
private SocketClient		socket_client;

private String			process_id;
private long			start_time;
private Thread			monitor_thread;
private BitSet			ignore_thread;
private BitSet			use_thread;
private boolean 		thread_default;
private String			base_directory;

private Map<String,ClassType>	class_map;
private Map<String,ClassType>	package_map;

private Set<BandaidAgent>	active_agents;

private ThreadMXBean	thread_bean;
private long		delay_time;
private long		disable_time;
private long		report_time;
private long		last_report;
private long		last_monitor;
private long		last_nano;
private boolean 	monitor_enabled;
private boolean 	reports_enabled;	// only used if monitoring disabled
private boolean 	need_report;

private double		report_total;
private long		num_reports;
private double		check_total;
private double		delay_total;
private long		num_checks;
private int		max_depth;
private double		last_check;

private Map<String,BandaidAgent> agent_names;

private Instrumentation 	class_inst;


private static final double	CHECK_OVERHEAD = 1.000; 	// 1 ms for timer checks
private static final double	REPORT_OVERHEAD = 1.000;	// 1 ms for timer checks

private static BandaidController the_control = null;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BandaidController(String args,Instrumentation inst)
{
   class_inst = inst;

   start_time = System.currentTimeMillis();
   host_name = "localhost";
   port_number = BANDAID_PORT;
   base_directory = null;

   setupClassTypes();

   delay_time = BANDAID_CHECK_TIME;
   disable_time = BANDAID_DISABLE_TIME;
   report_time = BANDAID_REPORT_TIME;

   monitor_enabled = false;
   reports_enabled = true;

   last_report = 0;
   last_monitor = 0;
   last_nano = 0;

   report_total = 0;
   num_reports = 0;
   check_total = 0;
   last_check = 0;
   delay_total = 0;
   num_checks = 0;

   need_report = false;
   max_depth = BANDAID_MAX_DEPTH;
   thread_bean = ManagementFactory.getThreadMXBean();

   active_agents = new HashSet<BandaidAgent>();
   agent_names = new HashMap<String,BandaidAgent>();
   defineAgent(new BandaidAgentThreadState(this));
   defineAgent(new BandaidAgentCpu(this));
   defineAgent(new BandaidAgentDeadlock(this));
   defineAgent(new BandaidAgentHistory(this));
   defineAgent(new BandaidAgentSwing(this));
   defineAgent(new BandaidAgentTrie(this));
   defineAgent(new BandaidAgentTracer(this));

   scanArgs(args);

   if (process_id == null) {
      RuntimeMXBean rmx = ManagementFactory.getRuntimeMXBean();
      process_id = rmx.getName();
      for (String s : rmx.getInputArguments()) {
	 if (s.startsWith("-agentlib:jdwp=transport")) {
	    int idx = s.indexOf("address=");
	    if (idx >= 0) {
	       s = s.substring(idx+8);
	       idx = s.indexOf(",");
	       if (idx >= 0) s = s.substring(0,idx);
	       idx = s.indexOf(":");
	       if (idx > 0) {
		  String h = s.substring(0,idx);
		  String p = s.substring(idx+1);
		  if (h.equals("localhost") || h.equals("127.0.0.1") || h.equals("0.0.0.0")) {
		     try {
			h = InetAddress.getLocalHost().getHostName();
		      }
		     catch (UnknownHostException e) {
			h = "localhost";
		      }
		   }
		  process_id = p + "@" + h;
		}
	     }
	  }
       }
    }

   if (active_agents.isEmpty()) addAllAgents();

   ignore_thread = new BitSet(BANDAID_MAX_THREADS);
   use_thread = new BitSet(BANDAID_MAX_THREADS);
   thread_default = true;				// use all unless otherwise stated

   socket_client = new SocketClient();
   if (socket_client.isValid()) socket_client.sendMessage("CONNECT " + process_id);

   monitor_thread = new ClassMonitor();
   ignore_thread.set((int) monitor_thread.getId());
   monitor_thread.start();

   if (class_inst != null) {
      for (BandaidAgent ba : active_agents) {
	 ClassFileTransformer cft = ba.getTransformer();
	 if (cft != null) {
	    class_inst.addTransformer(cft,true);
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String args)
{
   if (args == null) return;

   StringTokenizer tok = new StringTokenizer(args,";");
   while (tok.hasMoreTokens()) {
      String arg = tok.nextToken();
      String val = null;
      int idx = arg.indexOf('=');
      if (idx >= 0) {
	 val = arg.substring(idx+1);
	 arg = arg.substring(0,idx);
       }

      if (arg.equals("id") && val != null) {
	 process_id = val;
       }
      else if (arg.equals("enable")) {
	 enableMonitoring(true);
       }
      else if (arg.equals("disable")) {
	 enableMonitoring(false);
       }
      else if (arg.equals("host")) {
	 setHost(val);
       }
      else if (arg.equals("port")) {
	 try {
	    if (val != null) port_number = Integer.parseInt(val);
	  }
	 catch (NumberFormatException e) { }
       }
      else if (arg.equals("base")) {
	 base_directory = val;
       }
      else if (arg.equals("remote")) {
	 report_time *= 10;		// send fewer reports to remote process
       }
      else if (arg.equalsIgnoreCase("All")) {
	 addAllAgents();
       }
      else if (agent_names.containsKey(arg.toUpperCase())) {
	 active_agents.add(agent_names.get(arg.toUpperCase()));
       }
      else {
	 System.err.println("BANDAID: Unknown argument: " + arg);
       }
    }
}



private void addAllAgents()
{
   for (BandaidAgent agt : agent_names.values()) active_agents.add(agt);
}


private void setHost(String h)
{
   if (h == null) h = "127.0.0.1";

   try {
      String h1 = InetAddress.getLocalHost().getHostName();
      String h2 = InetAddress.getLocalHost().getHostAddress();
      String h3 = InetAddress.getLocalHost().getCanonicalHostName();

      if (h.equals(h1) || h.equals(h2) || h.equals(h3)) h = "127.0.0.1";
    }
   catch (UnknownHostException e) { }

   host_name = h;
}



private void defineAgent(BandaidAgent agt)
{
   agent_names.put(agt.getName().toUpperCase(),agt);
}




/********************************************************************************/
/*										*/
/*	Class/package setup							*/
/*										*/
/********************************************************************************/

private void setupClassTypes()
{
   package_map = new HashMap<>();
   class_map = new HashMap<>();

   package_map.put("java.",ClassType.SYSTEM);
   package_map.put("javax.",ClassType.SYSTEM);
   package_map.put("sun.",ClassType.SYSTEM);
   package_map.put("org.w3c.",ClassType.SYSTEM);
   package_map.put("org.omg.",ClassType.SYSTEM);
   package_map.put("org.xml.",ClassType.SYSTEM);
   package_map.put("com.ibm.",ClassType.SYSTEM);
   package_map.put("com.sun.",ClassType.SYSTEM);
   package_map.put("com.apple.",ClassType.SYSTEM);
   package_map.put("org.postgresql.",ClassType.SYSTEM);
   package_map.put("jrockit.",ClassType.SYSTEM);
   package_map.put("com.jogamp.",ClassType.SYSTEM);
																
   package_map.put("edu.brown.cs.dyvise.dyper.",ClassType.SYSTEM);
   package_map.put("edu.brown.cs.bubbles.bandaid.",ClassType.SYSTEM);
   package_map.put("org.eclipse.",ClassType.SYSTEM);

   package_map.put("java.io.",ClassType.SYSTEM_IO);
   package_map.put("java.net.",ClassType.SYSTEM_IO);
   package_map.put("javax.net.",ClassType.SYSTEM_IO);
   package_map.put("java.nio.",ClassType.SYSTEM_IO);
   package_map.put("sun.nio.",ClassType.SYSTEM_IO);
   package_map.put("sun.net.",ClassType.SYSTEM_IO);
   package_map.put("jrockit.net.",ClassType.SYSTEM_IO);
   package_map.put("jrockit.io.",ClassType.SYSTEM_IO);
}



/********************************************************************************/		      ;
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

long getMonitorThreadId()
{
   if (monitor_thread == null) return 0;

   return monitor_thread.getId();
}



boolean useThread(long id)
{
   int idx = (int) id;
   if (ignore_thread.get(idx)) return false;
   if (use_thread.get(idx)) return true;
   return thread_default;
}


boolean useThread(int id)
{
   if (ignore_thread.get(id)) return false;
   if (use_thread.get(id)) return true;
   return thread_default;
}



boolean useThread()
{
   return thread_default;
}


String getBaseDirectory()		{ return base_directory; }



/********************************************************************************/
/*										*/
/*	Communications methods							*/
/*										*/
/********************************************************************************/

void handleRequests(boolean wait)
{
   for ( ; ; ) {
      String cmd = socket_client.readCommand(wait);
      if (cmd == null) break;
      processRequest(cmd);
    }
}



void sendMessage(CharSequence xml)
{
   if (socket_client.isValid()) socket_client.sendMessage(xml);
}




private void processRequest(String rqst)
{
   String who = rqst;
   String cmd = null;
   String args = null;

   int idx = rqst.indexOf(" ");
   if (idx > 0) {
      who = rqst.substring(0,idx);
      cmd = rqst.substring(idx+1).trim();
      idx = cmd.indexOf(" ");
      if (idx > 0) {
	 args = cmd.substring(idx+1).trim();
	 cmd = cmd.substring(0,idx);
       }
    }

   BandaidAgent agt = agent_names.get(who.toUpperCase());
   if (agt != null && cmd != null) {
      agt.handleCommand(cmd,args);
    }
   else {
      System.err.println("BANDAID: Unknown command " + rqst);
    }
}





/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

long sendReport(long now)
{
   if (!socket_client.isValid()) return now;

   // System.err.println("BANDAID: report");

   BandaidXmlWriter xw = new BandaidXmlWriter();

   xw.begin("BANDAID");
   xw.field("REPORT",process_id);
   xw.field("TIME",now);
   generateReport(xw);
   xw.end();

   socket_client.sendMessage(xw.toString());

   return System.nanoTime();
}



/********************************************************************************/
/*										*/
/*	Variable access methods 						*/
/*										*/
/********************************************************************************/

long getStartTime()			{ return start_time; }

String getProcessId()			{ return process_id; }

long getThreadCpuTime(long id)		{ return thread_bean.getThreadCpuTime(id); }
long getThreadUserTime(long id) 	{ return thread_bean.getThreadUserTime(id); }



/********************************************************************************/
/*										*/
/*	Class type methods							*/
/*										*/
/********************************************************************************/

boolean isIOClass(String nm)
{
   return getClassType(nm).isIO();
}


boolean isSystemClass(String nm)
{
   return getClassType(nm).isSYSTEM();
}


boolean checkSystemClass(String nm)
{
   ClassType ct = class_map.get(nm);

   if (ct == null) return false;

   return ct.isSYSTEM();
}



private ClassType getClassType(String nm)
{
   ClassType ct = class_map.get(nm);

   if (ct == null) {
      ct = ClassType.NORMAL;
      int len = 0;
      for (Map.Entry<String,ClassType> ent : package_map.entrySet()) {
	 String k = ent.getKey();
	 if (k.length() > len && nm.startsWith(k)) {
	    ct = ent.getValue();
	    len = k.length();
	  }
       }
      class_map.put(nm,ct);
    }

   return ct;
}



/********************************************************************************/
/*										*/
/*	Monitoring access methods						*/
/*										*/
/********************************************************************************/

private void enableMonitoring(boolean fg)
{
   if (monitor_enabled == fg) return;

   monitor_enabled = fg;

   thread_bean.setThreadContentionMonitoringEnabled(fg);
   thread_bean.setThreadCpuTimeEnabled(fg);

   long now = System.currentTimeMillis();
   for (BandaidAgent agt : active_agents) {
      agt.enableMonitoring(fg,now);
    }
}



/********************************************************************************/
/*										*/
/*	Time methods								*/
/*										*/
/********************************************************************************/

private long  getNextDelayTime()
{
   if (monitor_enabled) {
      return (long)(-delay_time * Math.log(Math.random())) + 1;
    }
   else if (reports_enabled || need_report) return disable_time;
   else return -1;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

void generateReport(BandaidXmlWriter xw)
{
   xw.begin("MONITOR");
   xw.field("TIME",last_monitor);
   xw.field("NANO",last_nano);
   if (num_checks > 0) {
      xw.field("LASTCHECK",last_check);
      xw.field("CHECKTIME",check_total/num_checks + CHECK_OVERHEAD);
      xw.field("CHECKTOTAL",check_total);
    }
   if (num_reports > 0) xw.field("REPORTTIME",report_total/num_reports + REPORT_OVERHEAD);
   if (num_checks > 0) xw.field("DELAYAVG",delay_total/num_checks);

   for (BandaidAgent agt : active_agents) {
      try {
	 agt.generateReport(xw,last_monitor);
       }
      catch (Throwable t) {
	 // System.err.println("BANDAID: Problem generating report: " + t);
	 // t.printStackTrace();
       }
    }

   xw.end();
}



/********************************************************************************/
/*										*/
/*	Methods to handle Stack monitoring					*/
/*										*/
/********************************************************************************/

private void monitorStacks(long now,ThreadInfo [] tinfo)
{
   for (int i = 0; i < tinfo.length; ++i) {
      ThreadInfo ti = tinfo[i];
      if (ti == null) continue;
      long tid = ti.getThreadId();
      if (tid == Thread.currentThread().getId()) continue;
      if (!useThread(tid)) continue;

      StackTraceElement [] trc = ti.getStackTrace();
      for (BandaidAgent agt : active_agents) {
	 agt.handleThreadStack(now,ti,trc);
       }
    }

   for (BandaidAgent agt : active_agents) {
      agt.handleDoneStacks(now);
    }
}




/********************************************************************************/
/*										*/
/*	Methods to handle state dumps						*/
/*										*/
/********************************************************************************/

void generateStackDump(BandaidXmlWriter xw)
{
   long now = System.currentTimeMillis();
   xw.begin("THREADS");
   xw.field("TIME",now);

   long [] tids = thread_bean.getAllThreadIds();
   ThreadInfo [] tinfo = thread_bean.getThreadInfo(tids,max_depth);

   for (int i = 0; i < tinfo.length; ++i) {
      if (tinfo[i] == null) continue;
      long tid = tinfo[i].getThreadId();
      if (tid == Thread.currentThread().getId()) continue;
      StackTraceElement [] trc = tinfo[i].getStackTrace();
      dumpThreadInfo(tinfo[i],trc,xw);
    }

   xw.end();
}





private void dumpThreadInfo(ThreadInfo tinfo,StackTraceElement [] trc,BandaidXmlWriter xw)
{
   xw.begin("THREAD");
   xw.field("ID",tinfo.getThreadId());
   xw.field("NAME",tinfo.getThreadName());
   xw.field("STATE",tinfo.getThreadState().toString());
   xw.field("BLOCKCT",tinfo.getBlockedCount());
   xw.field("BLOCKTIME",tinfo.getBlockedTime());
   String lock = tinfo.getLockName();
   if (lock != null) {
      xw.field("LOCK",lock);
      xw.field("LOCKOWNER",tinfo.getLockOwnerId());
    }
   xw.field("WAITCT",tinfo.getWaitedCount());
   xw.field("WAITTIME",tinfo.getWaitedTime());
   for (int i = 0; i < trc.length; ++i) {
      xw.begin("STACK");
      xw.field("CLASS",trc[i].getClassName());
      xw.field("METHOD",trc[i].getMethodName());
      xw.field("LINE",trc[i].getLineNumber());
      xw.field("FILE",trc[i].getFileName());
      xw.end();
    }
   xw.end();
}




/********************************************************************************/
/*										*/
/*	Communication methods							*/
/*										*/
/********************************************************************************/

private class SocketClient {

   private OutputStream output_stream;
   private BufferedReader input_reader;
   private char [] char_trailer;
   private byte [] byte_buffer;

   SocketClient() {
      output_stream = null;
      String eom = BANDAID_TRAILER + "\n";
      byte_buffer = new byte[65536];
      char_trailer = new char[eom.length()];
      eom.getChars(0,eom.length(),char_trailer,0);

      try {
	 @SuppressWarnings("resource")
	 Socket cs = new Socket(host_name,port_number);
	 output_stream = cs.getOutputStream();
	 input_reader = new BufferedReader(new InputStreamReader(cs.getInputStream()));
       }
      catch (Exception e) {
	 System.err.println("BANDAID: No server connection: " + e.getMessage());
       }
    }

   boolean isValid()			{ return output_stream != null; }

   String readCommand(boolean wait) {
      if (input_reader == null) return null;
      String cmd = null;
      try {
	 if (!wait && !input_reader.ready()) return null;
	 cmd = input_reader.readLine();
       }
      catch (IOException e) {
	 System.err.println("BANDAID: Lost server connection: " + e);
       }

      if (cmd == null) {
	 input_reader = null;
	 output_stream = null;
       }

      return cmd;
    }

   void sendMessage(CharSequence msg) {
      if (output_stream == null) return;
      int slen = 0;
      int xlen = 0;

      if (msg != null) {
	 slen = msg.length();
	 if (msg.charAt(slen-1) != '\n') xlen = 1;
	 // if (char_buf.length < slen+1) char_buf = new char[slen*2+1];
	 // msg.getChars(0,slen,char_buf,0);
	 // if (!msg.endsWith("\n")) char_buf[slen++] = '\n';
       }
      if (slen + xlen + char_trailer.length > byte_buffer.length) {
	 byte_buffer = new byte[slen*2 + char_trailer.length];
       }
      if (msg != null) {
	 for (int i = 0; i < slen; ++i) {
	    // byte_buffer[i] = (byte) char_buf[i];
	    byte_buffer[i] = (byte) msg.charAt(i);
	  }
	 if (xlen > 0) {
	    byte_buffer[slen++] = '\n';
	  }
       }
      for (int i = 0; i < char_trailer.length; ++i) {
	 byte_buffer[i+slen] = (byte) char_trailer[i];
       }
      try {
	 output_stream.write(byte_buffer,0,slen+char_trailer.length);
	 output_stream.flush();
       }
      catch (IOException e) {
	 System.err.println("BANDAID: problem writing output: " + e);
	 output_stream = null;
       }
    }

}	// end of subclass SocketClient





/********************************************************************************/
/*										*/
/*	Methods for handling stack checking					*/
/*										*/
/********************************************************************************/

private class ClassMonitor extends Thread {

   ClassMonitor() {
      super(BANDAID_THREAD);
      setDaemon(true);
    }

   @Override public void run() {
      long nextcheck = 0;

      for ( ; ; ) {
	 long delay = getNextDelayTime();
	 while (delay < 0) {
	    handleRequests(true);
	    delay = getNextDelayTime();
	  }

	 if (delay > BANDAID_MAX_DELAY) {
	    if (monitor_enabled && nextcheck == 0) nextcheck = delay;
	    delay = BANDAID_MAX_DELAY;
	  }

	 if (delay != 0) {
	    synchronized (this) {
	       try {
		  wait(delay);
		}
	       catch (InterruptedException e) { }
	     }
	  }

	 long now = System.currentTimeMillis();
	 long nnow = System.nanoTime();
	 long tnow = nnow;

	 if (monitor_enabled) {
	    if (nextcheck == 0 || now - last_monitor >= nextcheck) {
	       last_monitor = now;
	       last_nano = nnow;
	       try {
		  monitorThreads();
		}
	       catch (Throwable t) {
		  System.err.println("BANDAID: Problem monitoring threads: " + t);
		  t.printStackTrace();
		}
	       tnow = System.nanoTime();
	       last_check = (tnow - nnow) / 1000000.0;
	       check_total += last_check;
	       delay_total += delay;
	       num_checks++;
	       nextcheck = 0;
	       need_report = true;
	     }
	  }

	 if (report_time > 0 && (monitor_enabled || reports_enabled || need_report)) {
	    try {
	       if (now - last_report >= report_time) {
		  tnow = System.nanoTime();
		  long rnow = tnow;
		  if (last_report != 0) rnow = sendReport(now);
		  last_report = now;
		  report_total += (rnow - tnow) / 1000000.0;
		  num_reports++;
		  need_report = false;
		}
	     }
	    catch (Throwable t) {
	       System.err.println("BANDAID: Problem generating report: " + t);
	       t.printStackTrace();
	     }
	  }

	 handleRequests(false);
       }
    }

   private void monitorThreads() {
      long [] tids = thread_bean.getAllThreadIds();
      ThreadInfo [] tinfo = thread_bean.getThreadInfo(tids,max_depth);
      try {
	 if (monitor_enabled) {
	    monitorStacks(last_monitor,tinfo);
	  }
       }
      catch (Throwable t) {
	 System.err.println("BANDAID: Problem during monitoring: " + t);
	 t.printStackTrace();
       }
    }

}	// end of subclass ClassMonitor




}	// end of class BandaidController




/* end of BandaidController.java */
