/********************************************************************************/
/*										*/
/*		BattMain.java							*/
/*										*/
/*	Bubble Automated Testing Tool main program				*/
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


package edu.brown.cs.bubbles.batt;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlReaderThread;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public class BattMain implements BattConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BattMain bm = new BattMain(args);
   bm.process();
   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		mint_handle;
private ProcessMode	process_mode;
private TestMode	start_mode;
private TestMode	test_mode;
private BattMonitor	batt_monitor;
private String		junit_jar;
private String		bubbles_junitjar;
private String		bubbles_agentjar;
private List<String>	java_args;
private ServerSocket	server_socket;
private Map<String,BattTestCase> test_cases;
private Set<BattTestCase> run_tests;
private boolean		run_all;
private boolean 	test_request;
private boolean 	test_busy;
private boolean 	find_new;
private BattThread	server_thread;
private Set<String>	error_classes;
private IvyExec 	current_test;
private long		last_report;
private Object          message_lock;



enum	ProcessMode {
   SERVER,		// act as a server
   LIST,		// just list tests
   RUN			// just run tests
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BattMain(String [] args)
{
   mint_handle = null;
   process_mode = ProcessMode.SERVER;
   start_mode = TestMode.ON_DEMAND;
   test_mode = null;
   batt_monitor = null;
   bubbles_junitjar = null;
   bubbles_agentjar = null;
   java_args = new ArrayList<String>();
   test_cases = new HashMap<String,BattTestCase>();
   run_tests = new HashSet<BattTestCase>();
   run_all = false;
   server_thread = null;
   test_request = false;
   test_busy = false;
   find_new = false;
   error_classes = new HashSet<String>();
   current_test = null;
   last_report = 0;
   message_lock = new Object();

   junit_jar = null;
   String s = System.getProperty("java.class.path");
   StringTokenizer tok = new StringTokenizer(s,File.pathSeparator);
   while (tok.hasMoreTokens()) {
      String p = tok.nextToken();
      if (p.contains("junit") && p.endsWith(".jar")) {
	 junit_jar = p;
	 break;
       }
    }

   scanArgs(args);

   IvySetup.setup();
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {           // -m <mint handle>
	    mint_handle = args[++i];
	  }
	 else if (args[i].startsWith("-b") && i+1 < args.length) {      // -b <bubbles lib>
	    File f1 = new File(args[++i]);
	    File f2 = new File(f1,"battjunit.jar");
	    bubbles_junitjar = f2.getPath();
	    File f3 = new File(f1,"battagent.jar");
	    bubbles_agentjar = f3.getPath();
	  }
	 else if (args[i].startsWith("-u") && i+1 < args.length) {      // -u <junit jar>
	    bubbles_junitjar = args[++i];
	  }
	 else if (args[i].startsWith("-a") && i+1 < args.length) {      // -a <agent jar>
	    bubbles_agentjar = args[++i];
	  }
	 else if (args[i].startsWith("-l") && i+1 < args.length) {      // -l <junit.jar>
	    junit_jar = args[++i];
	  }
	 else if (args[i].startsWith("-b") && i+1 < args.length) {      // -j <java argument>
	    java_args.add(args[++i]);
	  }
	 else if (args[i].startsWith("-C")) {                           // -Continuous
	    process_mode = ProcessMode.SERVER;
	    start_mode = TestMode.CONTINUOUS;
	  }
	 else if (args[i].startsWith("-L")) {                           // -List
	    process_mode = ProcessMode.LIST;
	  }
	 else if (args[i].startsWith("-R")) {                           // -Run
	    process_mode = ProcessMode.RUN;
	  }
	 else if (args[i].startsWith("-S")) {                           // -Server
	    process_mode = ProcessMode.SERVER;
	  }
	 else badArgs();
       }
      else badArgs();
    }

   if (mint_handle == null) badArgs();
}



private void badArgs()
{
   System.err.println("BATTM: battmain -m <mint> [-List] [-Run]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   batt_monitor = new BattMonitor(this,mint_handle);
   batt_monitor.loadProjects();

   switch (process_mode) {
      case LIST :
	 processRun(true,null);
	 break;
      case RUN :
	 setupSocket();
	 processRun(false,null);
	 break;
      case SERVER :
	 setupSocket();
	 processRun(true,null); 	// find test cases
	 reportTestStatus(true);
	 setMode(start_mode);
	 batt_monitor.server();
	 break;
    }
}




/********************************************************************************/
/*										*/
/*	Command handling methods for running tests				*/
/*										*/
/********************************************************************************/

TestMode getMode()
{
   if (test_mode != null) return test_mode;

   return start_mode;
}



void setMode(TestMode tm)
{
   if (test_mode == tm) return;

   test_mode = tm;

   synchronized (run_tests) {
      if (test_mode == TestMode.ON_DEMAND) {
	 if (server_thread != null) server_thread.interrupt();
       }
      else {
	 server_thread = new BattThread();
	 server_thread.start();
	 runSelectedTests(findPendingTests());
       }
    }
}




void runAllTests()
{
   Collection<BattTestCase> all = null;

   synchronized (this) {
      all = test_cases.values();
    }

   addTestCases(all,true);
}



void runSelectedTests(Collection<BattTestCase> tests)
{
   if (tests == null || tests.size() == 0) return;

   addTestCases(tests,false);
}



void doTests()
{
   if (server_thread != null) return;

   synchronized (run_tests) {
      if (test_busy) {
	 test_request = true;
	 return;
       }
    }

   try {
      processTests();
    }
   catch (InterruptedException e) { }
}



synchronized void addErrors(Set<String> clss)
{
   for (String s : clss) System.err.println("BATTM: Note error in " + s);
   error_classes.addAll(clss);
}


synchronized void removeErrors(Set<String> clss)
{
   for (String s : clss) System.err.println("BATTM: Remove error in " + s);

   error_classes.removeAll(clss);
   synchronized (run_tests) {
      run_tests.notifyAll();
    }
}


void stopAllTests()
{
   if (test_mode != TestMode.ON_DEMAND) setMode(TestMode.ON_DEMAND);

   synchronized (run_tests) {
      run_tests.clear();
      stopTests();
    }
}




void stopTests()
{
   // if (test_mode != TestMode.ON_DEMAND) setMode(TestMode.ON_DEMAND);

   if (current_test != null) {
      for (BattTestCase btc : test_cases.values()) {
	 if (btc.getState() == TestState.RUNNING || btc.getState() == TestState.TO_BE_RUN) {
	    btc.setStatus(TestStatus.UNKNOWN);
	    btc.setState(TestState.STOPPED);
	    btc.handleTestCounts(null);
	  }
       }
      current_test.destroy();
    }
}


void updateTests()
{
   batt_monitor.reloadProjects();

   synchronized (run_tests) {
      run_tests.clear();
      test_cases.clear();

      find_new = true;
    }
   doTests();
}




/********************************************************************************/
/*										*/
/*	Command methods for displaying tests					*/
/*										*/
/********************************************************************************/

String showAllTests()
{
   Collection<BattTestCase> rpt;

   synchronized (this) {
      rpt = new ArrayList<>(test_cases.values());
    }

   IvyXmlWriter xw = new IvyXmlWriter();
   for (BattTestCase btc : rpt) {
      // btc.shortReport(xw);
      btc.longReport(xw);
    }

   return xw.toString();
}



String showSelectedTests(Collection<BattTestCase> tests)
{
   if (tests == null || tests.size() == 0) return null;

   IvyXmlWriter xw = new IvyXmlWriter();
   for (BattTestCase btc : tests) {
      btc.longReport(xw);
    }

   return xw.toString();
}




/********************************************************************************/
/*										*/
/*	Test processing methods 						*/
/*										*/
/********************************************************************************/

void addTestCases(Collection<BattTestCase> tests,boolean all)
{
   if (tests == null || tests.size() == 0) return;

   synchronized (run_tests) {
      run_tests.addAll(tests);
      if (all) run_all = true;
      if (server_thread != null) run_tests.notifyAll();
    }
}



void processTests() throws InterruptedException
{
   Set<String> testclasses = null;
   Set<BattTestCase> testcases = null;
   boolean rpt = true;

   while (rpt) {
      rpt = false;
      boolean listonly = false;
      synchronized (run_tests) {
	 System.err.println("BATTM: Process tests " + run_tests.size() + " " + run_all + " " + find_new);
	 if (run_tests.size() == 0 && server_thread == null && !find_new) return;
	 int ct = 0;
	 while (!canRunAny(run_tests)) {
	    run_tests.wait(10000);
	    if (++ct > 5) return;
	  }
	 if (find_new) {
	    testclasses = null;
	    find_new = false;
	    if (run_tests.size() != 0) rpt = true;
	    listonly = true;
	  }
	 else {
	    testclasses = new HashSet<>();
	    testcases = new HashSet<>();
	    for (BattTestCase btc : run_tests) {
	       String cnm = btc.getClassName();
	       if (cnm != null) testclasses.add(cnm);
	       testcases.add(btc);
	     }
	  }
	 test_busy = true;
	 test_request = false;
       }

      processRun(listonly,testclasses,testcases);

      synchronized (run_tests) {
	 test_busy = false;
	 rpt |= test_request;
	 test_request = false;
       }
    }
}



void doneProcessing()
{
   synchronized (run_tests) {
      server_thread = null;
    }
}



private boolean canRunAny(Collection<BattTestCase> tests)
{
   if (find_new) return true;

   Set<String> testclass = new HashSet<String>();
   for (BattTestCase btc : tests) {
      testclass.add(btc.getClassName());
    }

   // ensure that a project can be run and that there is a test case for that project

   for (BattProject bp : batt_monitor.getProjects()) {
      boolean err = false;
      boolean use = false;
      for (String cnm : bp.getClassNames()) {
	 if (error_classes.contains(cnm)) {
	    err = true;
	    System.err.println("BATTM: HOLD for errors in " + cnm);
	  }
	 if (testclass.contains(cnm)) {
	    use = true;
	    System.err.println("BATTM: USE tests for " + cnm);
	  }
       }
      if (use && !err) return true;
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Run as set of test cases						*/
/*										*/
/********************************************************************************/

private void processRun(boolean listonly,Set<String> testclss,Set<BattTestCase> cases)
{
   if (cases != null && !listonly && testclss != null && !run_all) {
      synchronized (run_tests) {
	 for (BattTestCase btc : cases) {
	    run_tests.remove(btc);
	    btc.setStatus(TestStatus.UNKNOWN);
	    btc.setState(TestState.RUNNING);
	    btc.handleTestCounts(null);
	  }
       }
      for (BattTestCase btc : cases) {
	 runOneTest(btc);
       }
    }
   else {
      processRun(listonly,testclss);
    }
}



private void processRun(boolean listonly,Set<String> testclss)
{
   if (testclss !=  null) 
      System.err.println("BATTM: Run all tests in " + testclss.size() + " classes");
   else
      System.err.println("BATTM: Run all tests in all classes");
   
   if (!listonly) {
      synchronized (run_tests) {
	 for (BattTestCase btc : test_cases.values()) {
	    btc.setStatus(TestStatus.UNKNOWN);
	    btc.setState(TestState.RUNNING);
	    btc.handleTestCounts(null); 
	 }
	 run_tests.clear();
	 run_all = false; 
      }
   }
   
   for (BattProject bp : batt_monitor.getProjects()) {
      boolean err = false;
      boolean use = false;
      Set<String> run = new HashSet<>();
      for (String s : bp.getClassNames()) {
	 if (error_classes.contains(s)) err = true;
	 if (testclss == null || testclss.contains(s)) use = true;
	 run.add(s);
       }
      if (err) {
	 synchronized (this) {
	    for (BattTestCase btc : test_cases.values()) {
	       if (run.contains(btc.getClassName())) {
		  btc.setStatus(TestStatus.UNKNOWN);
		  btc.setState(TestState.CANT_RUN);
		}
	     }
	  }
	 continue;
       }
      if (!use) continue;

      String fnm = "BATT_" + bp.getName() + ".xml";
      String cntfnm = "BATT_" + bp.getName() + "_counts" + ".xml";
      if (server_socket != null) {
	 String nm = Integer.toString(server_socket.getLocalPort());
	 String host = "127.0.0.1";
	 try {
	    host = InetAddress.getLocalHost().getHostName();
	  }
	 catch (UnknownHostException e) { }
	 nm += "@" + host;
	 fnm = cntfnm = nm;
       }

      StringBuffer buf = new StringBuffer();
      buf.append(junit_jar);
      for (String p : bp.getClassPath()) {
	 buf.append(File.pathSeparator);
	 buf.append(p);
       }
      buf.append(File.pathSeparator);
      buf.append(bubbles_junitjar);

      List<String> args = new ArrayList<String>();
      args.add(IvyExecQuery.getJavaPath());
      if (!listonly && bubbles_agentjar != null) {
	 String agent = "-javaagent:" + bubbles_agentjar;
	 agent += "=COUNTS=" + cntfnm;
	 // agent += ";FULL"; or agent += ";SIMPLE";
	 args.add(agent);
       }
      for (String s : java_args) {
	 args.add(s);
       }

      args.add("-cp");
      args.add(buf.toString());
//    args.add("-verbose");
      args.add("edu.brown.cs.bubbles.batt.BattJUnit");
      if (testclss == null) args.add("-all");
      if (listonly) args.add("-list");
      if (server_socket != null) args.add("-socket");
      else args.add("-output");
      args.add(fnm);

      run = new HashSet<String>();
      for (String s : bp.getClassNames()) {
	 if (testclss != null && testclss.contains(s)) {
	    run.add(s);
	    s = "@" + s;
	  }
	 else if (testclss == null) {
	    run.add(s);
	  }
	 args.add(s);
       }

      List<String> testnames = new ArrayList<String>();
      synchronized (run_tests) {
	 for (Iterator<BattTestCase> it = run_tests.iterator(); it.hasNext(); ) {
	    BattTestCase btc = it.next();
	    if (run.contains(btc.getClassName())) {
	       it.remove();
	       testnames.add(btc.getName());
	     }
	  }
       }
      synchronized (this) {
	 for (BattTestCase btc : test_cases.values()) {
	    if (run.contains(btc.getClassName())) {
	       if (testnames.size() == 1 && !testnames.contains(btc.getName())) continue;
	       btc.setStatus(TestStatus.UNKNOWN);
	       btc.setState(TestState.RUNNING);
	       btc.handleTestCounts(null);
	     }
	  }
       }

      if (testnames.size() == 1) {
	 args.add("-test");
	 for (String s : testnames) args.add(s);
       }

      reportTestStatus(false);

      System.err.print("BATTM: RUN" );
      for (String s : args) {
	 System.err.print(" " + s);
       }
      System.err.println();

      try {
	 String [] argv = new String[args.size()];
	 argv = args.toArray(argv);
	 IvyExec ex = new IvyExec(argv,null,0);
	 current_test = ex;
	 // should handle input and output here
	 int sts = ex.waitFor();
	 System.err.println("BATTM: Test status " + sts);
	 current_test = null;
       }
      catch (IOException e) {
	 System.err.println("BATTM: Problem running junit java: " + e);
       }
    }

   reportTestStatus(false);
}



private boolean runOneTest(BattTestCase testcase)
{
   String testclss = testcase.getClassName();
   if (testclss == null) return false;

   System.err.println("BATTM: Start running test " + testcase.getName() +
	 " in " + testclss);

   for (BattProject bp : batt_monitor.getProjects()) {
      boolean err = false;
      boolean use = false;
      Set<String> run = new HashSet<String>();
      for (String s : bp.getClassNames()) {
	 if (error_classes.contains(s)) err = true;
	 if (s.equals(testclss)) use = true;
	 run.add(s);
       }
      if (err) {
	 synchronized (this) {
	    testcase.setStatus(TestStatus.UNKNOWN);
	    testcase.setState(TestState.CANT_RUN);
	  }
	 continue;
       }
      if (!use) continue;

      String fnm = "BATT_" + bp.getName() + ".xml";
      String cntfnm = "BATT_" + bp.getName() + "_counts" + ".xml";
      if (server_socket != null) {
	 String nm = Integer.toString(server_socket.getLocalPort());
	 String host = "127.0.0.1";
	 try {
	    host = InetAddress.getLocalHost().getHostName();
	  }
	 catch (UnknownHostException e) { }
	 nm += "@" + host;
	 fnm = cntfnm = nm;
       }

      StringBuffer buf = new StringBuffer();
      buf.append(junit_jar);
      for (String p : bp.getClassPath()) {
	 buf.append(File.pathSeparator);
	 buf.append(p);
       }
      buf.append(File.pathSeparator);
      buf.append(bubbles_junitjar);

      List<String> args = new ArrayList<String>();
      args.add(IvyExecQuery.getJavaPath());
      if (bubbles_agentjar != null) {
	 String agent = "-javaagent:" + bubbles_agentjar;
	 agent += "=COUNTS=" + cntfnm;
	 // agent += ";FULL"; or agent += ";SIMPLE";
	 args.add(agent);
       }
      for (String s : java_args) {
	 args.add(s);
       }

      args.add("-cp");
      args.add(buf.toString());
//      args.add("-verbose");
      args.add("edu.brown.cs.bubbles.batt.BattJUnit");
      if (server_socket != null) args.add("-socket");
      else args.add("-output");
      args.add(fnm);

      run = new HashSet<String>();
      for (String s : bp.getClassNames()) {
	 if (testclss != null && testclss.contains(s)) {
	    run.add(s);
	    s = "@" + s;
	  }
	 else if (testclss == null) {
	    run.add(s);
	  }
	 args.add(s);
       }

      synchronized (this) {
	 testcase.setStatus(TestStatus.UNKNOWN);
	 testcase.setState(TestState.RUNNING);
	 testcase.handleTestCounts(null);
       }

      args.add("-test");
      args.add(testcase.getName());

      reportTestStatus(false);

      System.err.print("BATTM: RUN" );
      for (String s : args) {
	 System.err.print(" " + s);
       }
      System.err.println();

      try {
	 String [] argv = new String[args.size()];
	 argv = args.toArray(argv);
	 IvyExec ex = new IvyExec(argv,null,0);
	 current_test = ex;
	 // should handle input and output here
	 int sts = ex.waitFor();
	 System.err.println("BATTM: Test status " + sts);
	 current_test = null;
       }
      catch (IOException e) {
	 System.err.println("BATTM: Problem running junit java: " + e);
       }
    }

   reportTestStatus(false);

   return true;
}



/********************************************************************************/
/*										*/
/*	Manage test cases							*/
/*										*/
/********************************************************************************/

synchronized BattTestCase findTestCase(String nm)
{
   BattTestCase btc = test_cases.get(nm);
   if (btc == null) {
      btc = new BattTestCase(nm);
      test_cases.put(nm,btc);
      System.err.println("BATTM: TEST CASES " + test_cases.size() + " " + nm);
    }

   return btc;
}



synchronized Collection<BattTestCase> findFailingTests()
{
   Set<BattTestCase> rslt = new HashSet<BattTestCase>();
   for (BattTestCase btc : test_cases.values()) {
      switch (btc.getStatus()) {
	 case FAILURE :
	    rslt.add(btc);
	    break;
	 default:
	    break;
      }
    }
   return rslt;
}




synchronized Collection<BattTestCase> findPendingTests()
{
   Set<BattTestCase> rslt = new HashSet<BattTestCase>();
   for (BattTestCase btc : test_cases.values()) {
      boolean use = false;
      switch (btc.getState()) {
	 case NEEDS_CHECK :
	 case PENDING :
	 case STOPPED :
	    use = true;
	    break;
	 default:
	    break;
       }
      switch (btc.getStatus()) {
	 case UNKNOWN :
	    use = true;
	    break;
	 default:
	    break;
       }
      if (use) rslt.add(btc);
    }
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Methods to handle updating test states					*/
/*										*/
/********************************************************************************/

void updateTestsForClasses(Map<String,FileState> chng)
{
   boolean upd = false;

   Set<BattTestCase> rslt = new HashSet<BattTestCase>();
   synchronized (this) {
      for (BattTestCase btc : test_cases.values()) {
	 FileState fs = btc.usesClasses(chng);
	 System.err.println("BATTM: TEST UPDATE: " + btc.getName() + " " + fs + " " + btc.getState());
	 if (fs != null) {
	    switch (fs) {
	       case STABLE :
		  break;
	       case EDITED :
		  if (btc.getState() == TestState.UP_TO_DATE ||
			 btc.getState() == TestState.STOPPED ||
			 btc.getState() == TestState.TO_BE_RUN ||
			 btc.getState() == TestState.RUNNING) {
		     btc.setState(TestState.EDITED);
		     upd = true;
		   }
		  break;
	       case CHANGED :
		  if (btc.getState() == TestState.UP_TO_DATE ||
			 btc.getState() == TestState.EDITED ||
			 btc.getState() == TestState.STOPPED ||
			 btc.getState() == TestState.TO_BE_RUN ||
			 btc.getState() == TestState.RUNNING) {
		     btc.setState(TestState.NEEDS_CHECK);
		     upd = true;
		     rslt.add(btc);
		   }
		  break;
	       case ERRORS :
		  if (btc.getState() != TestState.CANT_RUN) {
		     btc.setState(TestState.CANT_RUN);
		     upd = true;
		   }
		  break;
	     }
	  }
       }
    }

   if (server_thread != null && !rslt.isEmpty()) addTestCases(rslt,false);
   if (upd) reportTestStatus(false);
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

void reportTestStatus(boolean force)
{
   Collection<BattTestCase> rpt;
   long now;

   synchronized (this) {
      rpt = new ArrayList<>(test_cases.values());
      now = System.currentTimeMillis();
    }

   int ctr = 0;
   IvyXmlWriter xw = new IvyXmlWriter();
   for (BattTestCase btc : rpt) {
      if (force || btc.getUpdateTime() >= last_report) {
	 // btc.shortReport(xw);
	 // System.err.println("BATTM: WORK ON TEST CASE " + btc.getName());
	 btc.longReport(xw);
	 ++ctr;
       }
    }

   if (ctr > 0) {
      batt_monitor.sendMessageAndWait("STATUS",xw.toString());
    }

   last_report = now;
}





/********************************************************************************/
/*										*/
/*	Socket for listening to test results					*/
/*										*/
/********************************************************************************/

private void setupSocket()
{
   try {
      server_socket = new ServerSocket(0);
    }
   catch (IOException e) {
      System.err.println("BATTM: Problem creating test socket: " + e);
    }

   System.err.println("BATTM: Listening on " + server_socket.getInetAddress());

   SocketListener sl = new SocketListener();
   sl.start();
}



private void createClient(Socket s)
{
   try {
      Client c = new Client(s);
      c.start();
    }
   catch (IOException e) {
      System.err.println("BATTM: Problem starting server client: " + e);
    }
}




private class SocketListener extends Thread {

   SocketListener() {
      super("BATT_test_socket_acceptor");
      setDaemon(true);
    }

   @Override public void run() {
      try {
         for ( ; ; ) {
            Socket s = server_socket.accept();
            if (s != null) createClient(s);
          }
       }
      catch (IOException e) {
         System.err.println("BATTM: Problem with server socket accept: " + e);
       }
    }

}	// end of inner class SocketListener




private class Client extends IvyXmlReaderThread {

   private Socket client_socket;

   Client(Socket s) throws IOException {
      super("BATT_client_" + s.getRemoteSocketAddress(),new InputStreamReader(s.getInputStream()));
      client_socket = s;
    }

   @Override protected void processXmlMessage(String msg) {
      Element e = IvyXml.convertStringToXml(msg);
   
      synchronized(message_lock) {
         System.err.println("BATTM: CLIENT MSG: " + client_socket.getLocalPort() + " " + msg);
         
         if (IvyXml.isElement(e,"TESTCASE")) {
            String nm = IvyXml.getAttrString(e,"NAME");
            BattTestCase btc = findTestCase(nm);
            btc.handleTestState(e);
            reportTestStatus(false);
          }
         else if (IvyXml.isElement(e,"TESTCOUNTS")) {
            String nm = IvyXml.getAttrString(e,"NAME");
            BattTestCase btc = findTestCase(nm);
            btc.handleTestCounts(e);
          }
         
         System.err.println("BATTM: FINISH CLIENT MSG");
       }
   }




   @Override protected void processDone() {
      if (client_socket == null) return;
      try {
	 client_socket.close();
	 client_socket = null;
       }
      catch (IOException e) { }
    }

}	// end of inner class Client




/********************************************************************************/
/*										*/
/*	Background processor for running tests					*/
/*										*/
/********************************************************************************/

private class BattThread extends Thread {

   BattThread() {
      super("BattTestRunner");
    }

   @Override public void run() {
      try {
         for ( ; ; ) {
            processTests();
          }
       }
      catch (InterruptedException e) { }
      doneProcessing();
    }

}	// end of inner class BattThread


}	// end of class BattMain




/* end of BattMain.java */

