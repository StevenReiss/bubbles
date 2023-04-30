/********************************************************************************/
/*										*/
/*		BattTestCase.java						*/
/*										*/
/*	Bubble Automated Testing Tool representation of a test case		*/
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

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;



class BattTestCase implements BattConstants, BattConstants.BattTest, Comparable<BattTestCase>
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		test_name;
private String		class_name;
private String		method_name;
private TestStatus	test_status;
private TestState	test_state;
private String		fail_message;
private String		fail_trace;
private BattCountData	count_data;
private long		update_time;
private String		test_class;
private Set<String>	annotation_types;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattTestCase(String name)
{
   test_name = name;
   test_status = TestStatus.UNKNOWN;
   test_state = TestState.UNKNOWN;
   fail_message = null;
   fail_trace = null;
   update_time = System.currentTimeMillis();
   annotation_types = new HashSet<String>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public synchronized String getName()			{ return test_name; }
@Override public synchronized String getClassName()		{ return class_name; }
@Override public synchronized String getMethodName()		{ return method_name; }

@Override public synchronized TestStatus getStatus()		{ return test_status; }
@Override public synchronized TestState getState()		{ return test_state; }

@Override public BattCountData getCountData()			{ return count_data; }

@Override public String getFailMessage()			{ return fail_message; }
@Override public String getFailTrace()				{ return fail_trace; }

synchronized void setStatus(TestStatus sts)
{
   if (sts == test_status) return;
   update();
   test_status = sts;
}

synchronized void setState(TestState st)
{
   if (st == test_state) return;
   update();
   test_state = st;
}




/********************************************************************************/
/*										*/
/*	Methods to take data from the tester					*/
/*										*/
/********************************************************************************/

synchronized boolean handleTestState(Element e)
{
   boolean chng = false;

   class_name = IvyXml.getAttrString(e,"CLASS");
   method_name = IvyXml.getAttrString(e,"METHOD");
   test_name = IvyXml.getAttrString(e,"NAME");
   test_class = IvyXml.getAttrString(e,"TCLASS");

   TestStatus osts = test_status;
   TestState ost = test_state;

   boolean ignore = false;
   annotation_types.clear();
   for (Element ane : IvyXml.children(e,"ANNOT")) {
      String ant = IvyXml.getText(ane);
      annotation_types.add(ant);
      if (ant.startsWith("@org.junit.Ignore")) ignore = true;
    }
   if (IvyXml.getAttrBool(e,"EMPTY")) ignore = true;

   String sts = IvyXml.getAttrString(e,"STATUS");
   if (sts.equals("FAILURE")) {
      test_status = TestStatus.FAILURE;
      if (test_state == TestState.RUNNING || test_state == TestState.TO_BE_RUN ||
	    test_state == TestState.UNKNOWN)
	 test_state = TestState.UP_TO_DATE;
    }
   else if (sts.equals("SUCCESS")) {
      test_status = TestStatus.SUCCESS;
      if (test_state == TestState.RUNNING || test_state == TestState.TO_BE_RUN ||
	    test_state == TestState.UNKNOWN)
	 test_state = TestState.UP_TO_DATE;
    }
   else {
      if (ignore) test_state = TestState.IGNORED;
      test_status = TestStatus.UNKNOWN;
      count_data = null;
    }
   if (osts != test_status) chng = true;

   if (test_status == TestStatus.FAILURE) {
      String omsg = fail_message;
      fail_message = IvyXml.getTextElement(e,"EXCEPTION");
      fail_trace = IvyXml.getTextElement(e,"TRACE");
      if (fail_trace != null && fail_message == null) {
	 int idx = fail_trace.indexOf("\n");
	 if (idx < 0) fail_message = fail_trace;
	 else fail_message = fail_trace.substring(0,idx);
       }
      if (omsg == null && fail_message != null) chng = true;
      else if (omsg != null && fail_message == null) chng = true;
      else if (omsg != null && !omsg.equals(fail_message)) chng = true;
    }
   else {
      fail_message = null;
      fail_trace = null;
    }

   String st = IvyXml.getAttrString(e,"STATE");
   if (st != null) {
      try {
	 test_state = TestState.valueOf(st);
       }
      catch (IllegalArgumentException ex) { }
    }
   if (ost != test_state) chng = true;

   Element xe = IvyXml.getChild(e,"COVERAGE");
   if (xe != null) count_data = new BattCountData(xe);

   if (chng) update();

   return chng;
}



synchronized void handleTestCounts(Element e)
{
   if (e == null && count_data == null) return;

   if (e == null) count_data = null;
   else count_data = new BattCountData(e);

   update();
}



/********************************************************************************/
/*										*/
/*	Check for class change							*/
/*										*/
/********************************************************************************/

synchronized FileState usesClasses(Map<String,FileState> clsmap)
{
   FileState fs = null;

   fs = clsmap.get(class_name);
   if (count_data == null) return fs;

   return count_data.usesClasses(clsmap,fs);
}



@Override synchronized public UseMode usesMethod(String mthd)
{
   if (count_data == null) return UseMode.UNKNOWN;

   return count_data.getMethodUsage(mthd);
}


long getUpdateTime()			{ return update_time; }


private void update()
{
   update_time = System.currentTimeMillis();
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

synchronized void shortReport(IvyXmlWriter xw)
{
   xw.begin("TEST");
   xw.field("NAME",test_name);
   xw.field("STATUS",test_status);
   xw.field("STATE",test_state);
   xw.field("CLASS",class_name);
   if (test_class != null) xw.field("TCLASS",test_class);
   xw.field("METHOD",method_name);
   xw.end("TEST");
}



synchronized void longReport(IvyXmlWriter xw)
{
   xw.begin("TEST");
   xw.field("NAME",test_name);
   xw.field("STATUS",test_status);
   xw.field("STATE",test_state);
   xw.field("CLASS",class_name);
   if (test_class != null) xw.field("TCLASS",test_class);
   xw.field("METHOD",method_name);

   if (fail_message != null) {
      xw.cdataElement("EXCEPTION",fail_message);
      xw.cdataElement("TRACE",fail_trace);
    }

   if (count_data != null) count_data.report(xw);

   for (String s : annotation_types) {
      xw.textElement("ANNOT",s);
    }

   xw.end("TEST");
}




/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

synchronized String getToolTip()
{
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   buf.append("<b>TEST ");
   buf.append(test_name);
   buf.append("</b><hr />");
   buf.append("<table cellpadding=0 cellspacing=1 align=left >");
   buf.append("<tr><td>STATUS&nbsp;</td><td>");
   buf.append(test_status.toString());
   buf.append("</td></tr>");
   buf.append("<tr><td>STATE</td><td>");
   buf.append(test_state.toString());
   buf.append("</td></tr>");
   if (fail_message != null) {
      buf.append("<tr><td>ERROR</td><td>");
      buf.append(IvyXml.htmlSanitize(fail_message));
      buf.append("</td></tr>");
    }
   if (fail_trace != null) {
      StringTokenizer tok = new StringTokenizer(fail_trace,"\n\t");
      String s1 = tok.nextToken();
      buf.append("<tr><td>TRACE</td><td>" + IvyXml.htmlSanitize(s1) + "</td></tr>");
      while (tok.hasMoreTokens()) {
	 String s = tok.nextToken();
	 buf.append("<tr><td></td><td>&nbsp;&nbsp;" + IvyXml.htmlSanitize(s) + "</td></tr>");
      }

    }
   buf.append("</table>");

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public int compareTo(BattTestCase btc)
{
   return getName().compareTo(btc.getName());
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return getName();
}


}	// end of class BattTestCase




/* end of BattTestCase.java */
