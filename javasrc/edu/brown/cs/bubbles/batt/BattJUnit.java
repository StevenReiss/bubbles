/********************************************************************************/
/*										*/
/*		BattJUnit.java							*/
/*										*/
/*	Bubble Automated Testing Tool junit main program			*/
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


import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestClass;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class BattJUnit implements BattConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BattJUnit bj = new BattJUnit(args);

   bj.process();
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	list_only;
private String		result_file;
private OutputStream	result_stream;
private Class<?> []	class_set;
private String		single_test;
private Map<Description,JunitTest> test_cases;

private static final JunitTestStatus STATUS_RUNNING;
private static final JunitTestStatus STATUS_UNKNOWN;
private static final JunitTestStatus STATUS_SUCCESS;
private static final JunitTestStatus STATUS_IGNORED;
private static final JunitTestStatus STATUS_LISTING;
private static final JunitTestStatus STATUS_FAILURE;


enum StatusType {
   UNKNOWN,
   LISTING,
   IGNORED,
   RUNNING,
   FAILURE,
   SUCCESS
}

static {
   STATUS_RUNNING = new JunitTestStatus(StatusType.RUNNING);
   STATUS_UNKNOWN = new JunitTestStatus(StatusType.UNKNOWN);
   STATUS_LISTING = new JunitTestStatus(StatusType.LISTING);
   STATUS_SUCCESS = new JunitTestStatus(StatusType.SUCCESS);
   STATUS_IGNORED = new JunitTestStatus(StatusType.IGNORED);
   STATUS_FAILURE = new JunitTestStatus(StatusType.FAILURE);
}


private static Set<String> bad_messages;

static {
   bad_messages = new HashSet<String>();
   bad_messages.add("No runnable methods");
   bad_messages.add("Test class should have exactly one public constructor");
   bad_messages.add("Test class can only have one constructor");
   bad_messages.add("Test class should have exactly one public zero-argument constructor");
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BattJUnit(String [] args)
{
   list_only = false;
   class_set = null;
   result_file = "batt.out";
   result_stream = null;
   test_cases = new HashMap<>();
   single_test = null;

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   List<String> clsstr = new ArrayList<String>();
   Set<String> tststr = new LinkedHashSet<String>();
   List<Class<?>> clss = new ArrayList<Class<?>>();

   boolean havecls = false;
   boolean useall = false;
   ExecutorService service = Executors.newFixedThreadPool(1);

   for (int i = 0; i < args.length; ++i) {
      if (!havecls && args[i].startsWith("-")) {
	 if (args[i].startsWith("-l")) {                                // -list
	    list_only = true;
	  }
	 else if (args[i].startsWith("-o") && i+1 < args.length) {      // -o <output>
	    result_file = args[++i];
	  }
	 else if (args[i].startsWith("-s") && i+1 < args.length) {      // -s port@host
	    result_file = null;
	    setupSocket(args[++i]);
	  }
	 else if (args[i].startsWith("-a")) {                           // -all
	    useall = true;
	  }
	 else if (args[i].startsWith("-test") && i+1 < args.length) {   // -test <testname>
	    single_test = args[++i];
	  }
	 else badArgs();
       }
      else if (args[i].startsWith("-test") && i+1 < args.length) {   // -test <testname>
	 single_test = args[++i];
       }
      else {
	 havecls = true;
	 String clsnm = args[i];
	 if (clsnm.startsWith("@")) {
	    clsnm = clsnm.substring(1);
	    tststr.add(clsnm);
	  }
	 else if (useall) tststr.add(clsnm);

	 clsstr.add(clsnm);
       }
    }

   long time0 = System.currentTimeMillis();
   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleUserClasses",String [].class);
      String [] strarr = new String[clsstr.size()];
      strarr = clsstr.toArray(strarr);
      mac.invoke(null,(Object) strarr);
    }
   catch (ClassNotFoundException e) { 
      System.err.println("BATTJ: No agent found");
   }
   catch (Throwable t) {
      System.err.println("BATTJ: Problem with agent: " + t);
      t.printStackTrace();
    }
   
   long time1 = System.currentTimeMillis();
   
   if (!list_only) {
      for (String s : clsstr) {
         if (!tststr.contains(s)) {
            setupClass(s,service);
          }
       }
    }
   
   
   for (String cnm : tststr) {
      Class<?> c1 = setupClass(cnm,service);
      addClass(c1,clss);
    }
   
   long time2 = System.currentTimeMillis();

   class_set = new Class<?>[clss.size()];
   class_set = clss.toArray(class_set);

   if (result_stream == null && result_file != null) {
      try {
	 result_stream = new FileOutputStream(result_file);
       }
      catch (IOException e) {
	 System.err.println("BATTJ: Couldn't open output file: " + e);
	 System.exit(1);
       }
    }
   

   if (!list_only) {
      try {
	 Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
	 Method mac = ac.getMethod("doneLoad");
	 mac.invoke(null);
       }
      catch (Throwable e1) { 
	 System.err.println("BATTJ: problem with doneLoad: " + e1);
       }
    }
   
   System.err.println("BATTJ: Timings " + (time1-time0) + " " + (time2 - time1) + " " + tststr.size() + " " +
	clss.size() + " " + clsstr.size());
}


private void addClass(Class<?> c,List<Class<?>> clss)
{
   if (c == null) return;
   TestClass tcls = new TestClass(c);
   tcls.getOnlyConstructor();
   boolean valid = tcls.isPublic();
   if (tcls.isANonStaticInnerClass()) valid = false;
   if (valid) {
      clss.add(c);
   }
}



private Class<?> setupClass(String cnm,ExecutorService service) 
{
   System.err.println("BATTJ: SET UP CLASS " + cnm);
   System.err.flush();
   
   Class<?> rslt = null;
   
   boolean retry = true;
   while (retry && rslt == null) {
      retry = false;
      try {
	 // TODO: if this can be done without actually calling the static initializer
	 // it would be better
	 // An infinite loop or problem in the static initializer creates problems here
	 Class<?> c = null;
	 Future<Class<?>> future = service.submit(new FindClass(cnm));
	 try {
	    c = future.get(2000,TimeUnit.MILLISECONDS);
          }
	 catch (ExecutionException e) {
	    System.err.println("BATTJ: Find class " + cnm + " threw " + e);
	    throw e.getCause();
          }
	 catch (TimeoutException | InterruptedException e) {
	    // System.err.println("Initialization problem with " + cnm);
	    future.cancel(true);
          }
	 if (c == null) return null;
	 System.err.println("BATTJ: Preload test class " + cnm);
         
	 TestClass tcls = new TestClass(c);
	 tcls.getOnlyConstructor();
	 boolean valid = tcls.isPublic();
	 if (tcls.isANonStaticInnerClass()) valid = false;
	 if (valid) {
	    rslt = c;
          }
       }
      catch (AssertionError e) {
	 System.err.println("Assertion error: " + e);
       }
      catch (IllegalArgumentException e) {
	 // System.err.println("Argument exception: " + e);
	 // e.printStackTrace();
       }
      catch (ExceptionInInitializerError e) {
	 // System.err.println("Initialization exception: " + e);
	 // e.printStackTrace();
       }
      // catch (NoSuchMethodException e) {
      // System.err.println("No constructor: " + e);
      // }
      catch (NoClassDefFoundError e) {
	 System.err.println("BATTJ: Class " + cnm + " not found");
       }
      catch (ClassNotFoundException e) {
	 System.err.println("BATTJ: Class " + cnm + " not found");
       }
      catch (LinkageError e) {
	 retry = true;
       }
      catch (Throwable t) {
	 System.err.println("BATTJ: Class " + cnm + " can't be loaded: " + t);
       }
    }

   System.err.println("BATTJ: DONE: " + cnm);
   
   return rslt;
}



private static class FindClass implements Callable<Class<?>> {

   private String class_name;

   FindClass(String nm) {
      class_name = nm;
    }

   @Override public Class<?> call() {
      try {
	 return Class.forName(class_name);
       }
      catch (Throwable t) {
	 throw new Error(t);
       }
    }

}	// end of inner class FindClass



private void setupSocket(String ph)
{
   int idx = ph.indexOf('@');
   String host = "127.0.0.1";
   int port = -1;
   if (idx > 0) {
      host = ph.substring(idx+1);
      host = fixHost(host);
      ph = ph.substring(0,idx);
    }
   try {
      port = Integer.parseInt(ph);
    }
   catch (NumberFormatException e) { }

   try {
      @SuppressWarnings("resource")
      Socket s = new Socket(host,port);
      result_stream = s.getOutputStream();
    }
   catch (IOException e) {
      System.err.println("BATTJ: Problem connecting to socket " + port + " @" + host + ": " + e);
      System.exit(1);
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




private void badArgs()
{
   System.err.println("BATTJ: battjunit [-list] [-o output] class...");
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   Request rq = null;

   if (single_test != null) {
      int idx1 = single_test.indexOf("(");
      int idx2 = single_test.indexOf(")");
      String mnm = single_test.substring(0,idx1);
      String cnm = single_test.substring(idx1+1,idx2);
      System.err.println("BATTJ: WORK ON TEST " + single_test + " " + cnm + " " + mnm);
      try {
	 Class<?> clz = Class.forName(cnm);
	 rq = Request.method(clz,mnm);
       }
      catch (Exception e) {
	 System.err.println("BATTJ: Class for test not found: " + e);
       }
     }

   if (rq == null) {
      rq = Request.classes(class_set);
    }

   JUnitCore juc = new JUnitCore();

   if (list_only) {
      rq = rq.filterWith(new ListFilter());
    }

   TestListener ll = new TestListener();
   juc.addListener(ll);

   long time0 = System.currentTimeMillis();
   System.err.println("BATTJ: START RUN: " + list_only + " " + rq);

   juc.run(rq);

   long time1 = System.currentTimeMillis();
   System.err.println("BATTJ: FINISH RUN: " + list_only + " " + (time1-time0));

   if (result_stream != null) {
      try {
	 result_stream.close();
       }
      catch (IOException e) { }
    }

// System.exit(0);
   Runtime.getRuntime().halt(0);
}



/********************************************************************************/
/*										*/
/*	Test case maintenance methods						*/
/*										*/
/********************************************************************************/

synchronized JunitTest addTestCase(Description d,JunitTestStatus sts)
{
   JunitTest btc = test_cases.get(d);
   if (btc == null) {
      btc = new JunitTest(d);
      System.err.println("BATTJ: Create new test case for " + d + " " + test_cases.size());
      test_cases.put(d,btc);
    }
   btc.setStatus(sts);

   for (Description d1 : d.getChildren()) {
      addTestCase(d1,sts);
    }

   return btc;
}



synchronized void removeTestCase(Description d)
{
   System.err.println("BATTJ: Remove test case " + d);

   test_cases.remove(d);
}


void setTestStatus(Description d,JunitTestStatus sts)
{
   JunitTest btc = test_cases.get(d);
   if (btc != null) btc.setStatus(sts);
}



JunitTestStatus getTestStatus(Description d)
{
   JunitTest btc = test_cases.get(d);
   if (btc == null) return STATUS_UNKNOWN;
   return btc.getStatus();
}



void noteStart(Description d)
{
   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleStartTest",String.class);
      mac.invoke(null,d.toString());
    }
   catch (ClassNotFoundException e) { }
   catch (Throwable t) {
      System.err.println("BATTJ: Problem with agent: " + t);
      t.printStackTrace();
    }
}



void noteFinish(Description d)
{
   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleFinishTest",String.class);
      mac.invoke(null,d.toString());
    }
   catch (ClassNotFoundException e) { }
   catch (Throwable t) {
      System.err.println("BATTJ: Problem with agent: " + t);
      t.printStackTrace();
    }
}



void noteDone()
{
   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleFinishRun");
      mac.invoke(null);
    }
   catch (ClassNotFoundException e) { }
   catch (Throwable t) {
      System.err.println("BATTJ: Problem with agent: " + t);
      t.printStackTrace();
    }
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputSingleTest(JunitTest jt)
{
   if (result_stream ==  null) return;
   
   synchronized (result_stream) {
      XMLStreamWriter xw = null;
      try {
	 XMLOutputFactory xof = XMLOutputFactory.newInstance();
	 xw = xof.createXMLStreamWriter(result_stream);
	 outputTestCase(jt,xw);
	 xw.flush();
	 System.err.println("BATTJ: Finish writing test case " + jt.getDescription() + " " + jt.getStatus());
      }
      catch (XMLStreamException e) {
	 System.err.println("BATTJ: Problem writing output file " + result_file + ": " + e);
	 System.exit(1);
      }
   }
}



void outputTestCase(JunitTest btc,XMLStreamWriter xw) throws XMLStreamException
{
   Description d = btc.getDescription();

   xw.writeStartElement("TESTCASE");
   if (d.getClassName() != null) xw.writeAttribute("CLASS",d.getClassName());
   if (d.getMethodName() != null) xw.writeAttribute("METHOD",d.getMethodName());
   if (d.getTestClass() != null) {
      Class<?> tcls = d.getTestClass();
      if (Modifier.isAbstract(tcls.getModifiers())) xw.writeAttribute("ABSTRACT","true");
      String tc = d.getTestClass().getName();
      if (!tc.equals(d.getClassName())) {
	 xw.writeAttribute("TCLASS",tc);
       }
    }
   if (d.isEmpty()) xw.writeAttribute("EMPTY","TRUE");
   if (d.isSuite()) xw.writeAttribute("SUITE","TRUE");
   if (d.isTest()) xw.writeAttribute("TEST","TRUE");
   if (d.testCount() > 1) xw.writeAttribute("COUNT",Integer.toString(d.testCount()));

   xw.writeAttribute("STATUS",btc.getStatus().getType().toString());
   xw.writeAttribute("NAME",d.getDisplayName());
   xw.writeAttribute("HASH",Integer.toString(d.hashCode()));

   Failure f = btc.getStatus().getFailure();
   if (f != null) {
      xw.writeStartElement("EXCEPTION");
      String msg = f.getMessage();
      if (msg != null) {
	 msg = msg.replace("]]>","]] >");
	 xw.writeCData(msg);
       }
      xw.writeEndElement();
      xw.writeStartElement("TRACE");
      xw.writeCData(shortenTrace(f.getTrace()));
      xw.writeEndElement();
    }

   for (Annotation an : d.getAnnotations()) {
      xw.writeStartElement("ANNOT");
      xw.writeCData(an.toString());
      xw.writeEndElement();
    }

   xw.writeEndElement();
   xw.writeCharacters("\n");
}



private String shortenTrace(String t)
{
   t = t.replace("]]>","]] >");

   if (t.length() > 10240) {
      StringBuilder buf = new StringBuilder();
      StringTokenizer tok = new StringTokenizer(t,"\n");
      for (int i = 0; i < 40 && tok.hasMoreTokens(); ++i) {
	 String ln = tok.nextToken();
	 buf.append(ln);
	 buf.append("\n");
       }
      if (tok.hasMoreTokens()) buf.append("...\n");
      t = buf.toString();
    }

   return t;
}




/********************************************************************************/
/*										*/
/*	FIlter for handing listing test classes 				*/
/*										*/
/********************************************************************************/

private class ListFilter extends Filter {

   @Override public String describe()			{ return "List test cases"; }

   @Override public boolean shouldRun(Description d) {
      System.err.println("BATTJ: Consider test: " + d.isTest() + " " + d.isEmpty() + " " + d.isSuite() + " " +
			    d.getClassName() + " " + d.getMethodName() + " " +
			    d.getChildren().size() + " " + d.getDisplayName() + " " + d);
      // if (d.isSuite() && d.getClassName().contains("$")) return false;

      if (d.isSuite()) return true;
      if (!d.isTest()) return false;
      if (d.getMethodName() != null) {
	 if (d.getClassName().startsWith("junit.") || d.getClassName().startsWith("org.junit."))
	    return false;
	 if (Modifier.isAbstract(d.getTestClass().getModifiers())) return false;
	 JunitTest jt = addTestCase(d,STATUS_LISTING);
	 outputSingleTest(jt);
	 return false;
       }

      System.err.println("BATTJ: Unknown test: " + d.isTest() + " " + d.isEmpty() + " " +
			    d.getClassName() + " " + d.isSuite() + " " + d.getChildren().size() + " " + d);
      setTestStatus(d,STATUS_UNKNOWN);
      // might want to check classes to see if they are relevant here as well
      return false;
    }

}	// end of inner class ListFilter




/********************************************************************************/
/*										*/
/*	Listener for handling testing						*/
/*										*/
/********************************************************************************/

private class TestListener extends RunListener {

   TestListener() { }

   @Override public void testStarted(Description d) {
      System.err.println("BATTJ: START " + d);
      JunitTestStatus bts = getTestStatus(d);
      JunitTest jt = addTestCase(d,STATUS_RUNNING);
      noteStart(d);
      System.err.println("BATTJ: TEST " + bts + " " + jt.getDescription() + " " + bts.getType());
      switch (bts.getType()) {
	 case FAILURE :
	 case SUCCESS :
	 case LISTING :
	    if (d.isTest()) outputSingleTest(jt);
	    break;
	 default:
	    break;
       }
    }

   @Override public void testIgnored(Description d) {
      System.err.println("BATTJ: IGNORED " + d);
      addTestCase(d,STATUS_IGNORED);
    }

   @Override public void testFinished(Description d) {
      System.err.println("BATTJ: FINISH " + d + " " + test_cases.containsKey(d));

      JunitTest jt = test_cases.get(d);
      if (jt == null) {
	 System.err.println("BATTJ: No test case found");
	 return;
       }
      noteFinish(d);

      JunitTestStatus bts = getTestStatus(d);
      System.err.println("BATTJ: STATUS " + bts.getType() + " " + result_stream
	       );
      
      switch (bts.getType()) {
	 case IGNORED :
	 case LISTING :
	    break;
	 case FAILURE :
	    setTestStatus(d,STATUS_FAILURE);
	    break;
	 default :
	    setTestStatus(d,STATUS_SUCCESS);
	    break;
       }

      outputSingleTest(jt);
    }

   @SuppressWarnings("removal")
   @Override public void testRunStarted(Description d) {
      System.setSecurityManager(new NoExitManager());
    }

   @SuppressWarnings("removal")
   @Override public void testRunFinished(Result r) {
      System.setSecurityManager(null);
      noteDone();
    }

   @Override public void testFailure(Failure f) {
      System.err.println("BATTJ: Test failure: " + f);
      // Throwable t = f.getException();
      // if (t != null) t.printStackTrace();
      setTestStatus(f.getDescription(),STATUS_FAILURE);
      
      if (f.getMessage() != null && bad_messages.contains(f.getMessage())) {
	 removeTestCase(f.getDescription());
       }
      else if (f.getMessage() != null &&
		  (f.getMessage().startsWith("No tests found matching List test cases from org.junit.runner.Request") ||
		      f.getMessage().startsWith("No runnable methods") ||
		      f.getMessage().startsWith("No tests found in "))) {
	 removeTestCase(f.getDescription());
       }
      else {
	 System.err.println("BATTJ: FAIL " + f.getTestHeader() + " " + f.getDescription() + " " + f.getException() + " " + f.getMessage() + "\nTRACE: " + f.getTrace());
	 addTestCase(f.getDescription(),new JunitTestStatus(f));
       }

      JunitTest jt = test_cases.get(f.getDescription());
      if (jt != null) outputSingleTest(jt);
      else {
	 System.err.println("Can't find failing test case " + f.getDescription());
      }
    }

}	// end of inner class TestListener



/********************************************************************************/
/*										*/
/*	Handle System.exit() calls in tests					*/
/*										*/
/********************************************************************************/

private static class ExitException extends SecurityException {

   ExitException(int sts) {
      super("Attempt to call System.exit");
    }

}	// end of inner class ExitException



@SuppressWarnings("all")
private static class NoExitManager extends SecurityManager {

   @Override public void checkPermission(Permission p)			{ }
   @Override public void checkPermission(Permission p,Object ctx)	{ }

   @Override public void checkExit(int sts) {
      super.checkExit(sts);
      throw new ExitException(sts);
    }

}	// end of inner class NoExitManager




/********************************************************************************/
/*										*/
/*	TestCase information							*/
/*										*/
/********************************************************************************/

private static class JunitTest {

   private Description test_info;
   private JunitTestStatus test_status;

   JunitTest(Description d) {
      test_info = d;
      test_status = STATUS_UNKNOWN;
    }

   Description getDescription() 		{ return test_info; }
   JunitTestStatus getStatus()			{ return test_status; }
   void setStatus(JunitTestStatus sts)		{ test_status = sts; }

}	// end of inner class JunitTest



private static class JunitTestStatus {

   private StatusType status_type;
   private Failure fail_data;

   JunitTestStatus(StatusType st) {
      status_type = st;
      fail_data = null;
    }

   JunitTestStatus(Failure f) {
      status_type = StatusType.FAILURE;
      fail_data = f;
    }

   StatusType getType() 			{ return status_type; }
   Failure getFailure() 			{ return fail_data; }
   
   @Override public String toString()		{ return status_type.toString(); }

}	// end of inner class JunitTestStatus




}	// end of class BattJUnit




/* end of BattJUnit.java */
