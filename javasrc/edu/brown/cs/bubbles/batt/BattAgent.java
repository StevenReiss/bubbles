/********************************************************************************/
/*										*/
/*		BattAgent.java							*/
/*										*/
/*	Bubble Automated Testing Tool javaagent manager 			*/
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


import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;




public class BattAgent implements BattConstants
{



/********************************************************************************/
/*										*/
/*	Agent entry points							*/
/*										*/
/********************************************************************************/

public static void premain(String args,Instrumentation inst)
{
   the_agent = new BattAgent(args,inst);
}



public static void agentmain(String args,Instrumentation inst)
{
   if (the_agent == null) the_agent = new BattAgent(args,inst);
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Instrumentation 	class_inst;
private BattInstrument		our_instrumenter;
private int			id_counter;
private String			active_test;
private IndexTable		index_table;
private Map<Thread,ThreadData>	thread_stack;
private OutputStream		result_stream;
private boolean                 simple_stats;

private static BattAgent	the_agent;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BattAgent(String args,Instrumentation inst)
{
   class_inst = inst;
   active_test = null;
   index_table = new IndexTable();
   result_stream = null;
   thread_stack = new HashMap<>();
   simple_stats = true;

   // System.err.println("BATTAGENT: START " + args + " " + inst);

   scanArgs(args);

   if (class_inst != null) {
      try {
	 our_instrumenter = new BattInstrument(this);
	 class_inst.addTransformer(our_instrumenter,true);
       }
      catch (Throwable t) {
	 System.err.println("BATTAGENT: Problem adding instrumenter: " + t);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to scan arguments						*/
/*										*/
/********************************************************************************/

private void scanArgs(String args)
{
   if (args == null) return;

   String cntf = null;

   StringTokenizer tok = new StringTokenizer(args,":;,");
   while (tok.hasMoreTokens()) {
      String arg = tok.nextToken();
      String val = null;
      int idx = arg.indexOf("=");
      if (idx >= 0) {
	 val = arg.substring(idx+1);
	 arg = arg.substring(0,idx);
       }
      if (arg.equals("COUNTS")) {
	 cntf = val;
       }
      else if (arg.equals("FULL")) {
         simple_stats = false;
       }
      else if (arg.equals("SIMPLE")) {
         simple_stats = true;
       }
      else {
	 System.err.println("BATTAGENT: Illegal argument: " + arg);
       }
    }

   if (cntf != null) {
      try {
	 int idx = cntf.indexOf("@");              // port@host for socket
	 if (idx > 0) {
	    String host = cntf.substring(idx+1);
	    int port = Integer.parseInt(cntf.substring(0,idx));
	    host = fixHost(host);
	    @SuppressWarnings("resource")
	    Socket s = new Socket(host,port);
	    result_stream = s.getOutputStream();
	  }
	 else {
	    result_stream = new FileOutputStream(cntf);
	  }
       }
      catch (IOException e) {
	 System.err.println("BATT: Unable to open output file " + cntf + ": " + e);
       }
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




/********************************************************************************/
/*										*/
/*	Entry points from instrumentation					*/
/*										*/
/********************************************************************************/

public static void handleStartTest(String test)
{
   if (the_agent != null) the_agent.startTest(test);
}


public static void handleFinishTest(String test)
{
   if (the_agent != null) the_agent.finishTest(test);
}



public static void handleFinishRun()
{
   if (the_agent != null) the_agent.finishRun();
}



public static void handleEntry(int id)
{
   if (the_agent != null) the_agent.enterMethod(id);
}


public static void handleExit(int id)
{
   if (the_agent != null) the_agent.exitMethod(id);
}



public static void handleBlockEntry(int id)
{
   if (the_agent != null) the_agent.enterBlock(id);
}



public static void handleUserClasses(String [] clsset)
{
   if (the_agent != null) the_agent.userClasses(clsset);
}



/********************************************************************************/
/*										*/
/*	Instrumentation management methods					*/
/*										*/
/********************************************************************************/

private void startTest(String test)
{
   active_test = test;
}



private void finishTest(String test)
{
   if (active_test == null || result_stream == null) return;

   XMLOutputFactory xof = XMLOutputFactory.newInstance();
   XMLStreamWriter xw = null;
   try {
      xw = xof.createXMLStreamWriter(result_stream);
    }
   catch (XMLStreamException e) {
      return;
    }

   if (xw != null) {
      try {
	 outputTestResult(test,xw);
	 xw.flush();
	 result_stream.flush();
       }
      catch (Exception e) {
	 System.err.println("BATT: Problem with output file for test: " + e);
       }
    }

   active_test = null;
}



private void finishRun()
{
   if (result_stream != null) {
      try {
	 result_stream.close();
       }
      catch (IOException e) { }
    }
}




private void enterMethod(int id)
{
   if (active_test != null) {
      RtMethod rm = index_table.getMethod(id);
      rm.markEntry();
      if (!simple_stats) {
         ThreadData td = getThreadData();
         RtMethod fm = td.enterMethod(rm);
         if (fm != null) fm.markCall(rm);
         if (fm == null || td.getLevel() <= 2) rm.markTop();
       }
    }
}



private void exitMethod(int id)
{
   if (active_test != null) {
      if (!simple_stats) {
         ThreadData td = getThreadData();
         td.exitMethod();
       }
    }
}


private void enterBlock(int id)
{
   if (active_test != null) {
      RtBlock rb = index_table.getBlock(id);
      rb.markEntry();
      if (!simple_stats) {
         ThreadData td = getThreadData();
         RtBlock fb = td.enterBlock(rb);
         if (fb != null) fb.markBranch(rb);
       }
    }
}



private void userClasses(String [] clsset)
{
   if (our_instrumenter != null) our_instrumenter.setClasses(clsset);
}



/********************************************************************************/
/*										*/
/*	Methods for setting up id numbers					*/
/*										*/
/********************************************************************************/

int getMethodId(String clsname,String name,String desc)
{
   int mid = ++id_counter;

   RtMethod rm = new RtMethod(mid,clsname,name,desc);
   index_table.setElement(mid,rm);

   return mid;
}



int getBlockId(int mid,int offset)
{
   int bid = ++id_counter;
   RtBlock rb = new RtBlock(bid,offset);
   index_table.setElement(bid,rb);
   if (mid > 0) {
      RtMethod rm = index_table.getMethod(mid);
      rm.addBlock(rb);
    }

   return bid;
}



void noteLineNumber(int lno,int off,int mid,int bid)
{
   if (mid > 0) index_table.getItem(mid).noteLine(lno);
   if (bid > 0) index_table.getItem(bid).noteLine(lno);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputTestResult(String nm,XMLStreamWriter xw) throws XMLStreamException
{
   xw.writeStartElement("TESTCOUNTS");
   xw.writeAttribute("NAME",nm);
   index_table.outputCounts(xw);
   xw.writeEndElement();
}



/********************************************************************************/
/*										*/
/*	Index into run time elements by id					*/
/*										*/
/********************************************************************************/

private static class IndexTable {

   private RtItem []	the_table;

   IndexTable() {
      the_table = new RtItem[10240];
      Arrays.fill(the_table,null);
    }

   void setElement(int idx,RtItem itm) {
      if (idx >= the_table.length) {
	 int nln = the_table.length * 2;
	 if (idx >= nln) nln = the_table.length + idx;
	 the_table = Arrays.copyOf(the_table,nln);
       }
      the_table[idx] = itm;
    }

   RtMethod getMethod(int idx)		{ return (RtMethod) the_table[idx]; }
   RtBlock getBlock(int idx)		{ return (RtBlock) the_table[idx]; }
   RtItem getItem(int idx)		{ return the_table[idx]; }

   void outputCounts(XMLStreamWriter xw) throws XMLStreamException {
      for (int i = 0; i < the_table.length; ++i) {
	 if (the_table[i] != null && the_table[i] instanceof RtMethod) {
	    RtMethod rm = (RtMethod) the_table[i];
	    rm.outputCounts(xw);
	  }
       }
    }

}	// end of innder class IndexTable





/********************************************************************************/
/*										*/
/*	Run Time Generic Item							*/
/*										*/
/********************************************************************************/

private static abstract class RtItem {

   protected int item_index;
   protected int start_line;
   protected int end_line;

   protected RtItem(int idx) {
      item_index = idx;
      start_line = end_line = -1;
    }

   int getIndex()			{ return item_index; }

   void noteLine(int lno) {
      if (start_line < 0 || lno < start_line) start_line = lno;
      if (end_line < 0 || lno > end_line) {
         end_line = lno;
       } 
   }

}	// end of inner class RtItem




/********************************************************************************/
/*										*/
/*	Representation of a method						*/
/*										*/
/********************************************************************************/

private static class RtMethod extends RtItem {

   private String method_name;
   private String class_name;
   private String method_desc;
   private List<RtBlock> basic_blocks;
   private int entry_count;
   private int top_count;
   private Map<RtMethod,Counter> calls_count;

   RtMethod(int idx,String cnm,String nm,String dc) {
      super(idx);
      method_name = nm;
      class_name = cnm;
      method_desc = dc;
      start_line = -1;
      end_line = -1;
      basic_blocks = new ArrayList<>();
      entry_count = 0;
      top_count = 0;
      calls_count = null;
    }

   void addBlock(RtBlock rb)		{ basic_blocks.add(rb); }

   void markEntry()			{ ++entry_count; }

   void markCall(RtMethod rm) {
      if (calls_count == null) calls_count = new HashMap<RtMethod,Counter>();
      Counter c = calls_count.get(rm);
      if (c == null) {
         c = new Counter();
         calls_count.put(rm,c);
       }
      c.incr();
    }

   void markTop()			{ ++top_count; }

   void outputCounts(XMLStreamWriter xw) throws XMLStreamException {
      if (entry_count == 0) return;
      xw.writeStartElement("METHOD");
      outputFields(xw);
      xw.writeAttribute("COUNT",Integer.toString(entry_count));
      xw.writeAttribute("TOP",Integer.toString(top_count));
      if (calls_count != null) {
	 for (Map.Entry<RtMethod,Counter> ent : calls_count.entrySet()) {
	    RtMethod cm = ent.getKey();
	    Counter c = ent.getValue();
	    xw.writeStartElement("CALLS");
	    cm.outputFields(xw);
	    xw.writeAttribute("CALLCOUNT",Integer.toString(c.getValue()));
	    xw.writeEndElement();
	  }
       }
      for (RtBlock rb : basic_blocks) {
	 rb.outputCounts(xw);
       }
      xw.writeEndElement();
      entry_count = 0;
      calls_count = null;
    }

   void outputFields(XMLStreamWriter xw) throws XMLStreamException {
      xw.writeAttribute("NAME",class_name.replace('/','.') + "." + method_name);
      xw.writeAttribute("SIGNATURE",method_desc);
      if (start_line > 0) {
	 xw.writeAttribute("START",Integer.toString(start_line));
	 xw.writeAttribute("END",Integer.toString(end_line));
       }
    }

}	// end of inner class RtMethod




/********************************************************************************/
/*										*/
/*	Representation of a block						*/
/*										*/
/********************************************************************************/

private static class RtBlock extends RtItem {

   private int code_offset;
   private int use_count;
   private RtBlock next_block;
   private int next_count;
   private RtBlock alt_block;
   private int alt_count;
   private Map<RtBlock,Counter> other_blocks;

   RtBlock(int idx,int off) {
      super(idx);
      code_offset = off;
      start_line = -1;
      end_line = -1;
      use_count = 0;
      next_block = null;
      next_count = 0;
      alt_block = null;
      alt_count = 0;
      other_blocks = null;
    }

   void markEntry()				{ ++use_count; }

   void markBranch(RtBlock tb) {
      if (tb == next_block) ++next_count;
      else if (tb == alt_block) ++alt_count;
      else if (next_block == null) {
	 next_block = tb;
	 next_count = 1;
       }
      else if (alt_block == null) {
	 alt_block = tb;
	 alt_count = 1;
       }
      else {
	 if (other_blocks == null) other_blocks = new HashMap<RtBlock,Counter>();
	 Counter c = other_blocks.get(tb);
	 if (c == null) {
	    c = new Counter();
	    other_blocks.put(tb,c);
	  }
	 c.incr();
       }
    }

   void outputCounts(XMLStreamWriter xw) throws XMLStreamException {
      if (use_count == 0) return;

      xw.writeStartElement("BLOCK");
      xw.writeAttribute("INDEX",Integer.toString(getIndex()));
      xw.writeAttribute("OFFSET",Integer.toString(code_offset));
      if (start_line > 0) {
	 xw.writeAttribute("START",Integer.toString(start_line));
	 xw.writeAttribute("END",Integer.toString(end_line));
       }
      xw.writeAttribute("COUNT",Integer.toString(use_count));
      if (next_block != null) outputBranch(xw,next_block,next_count);
      if (alt_block != null) outputBranch(xw,alt_block,alt_count);
      if (other_blocks != null) {
	 for (Map.Entry<RtBlock,Counter> ent : other_blocks.entrySet()) {
	    RtBlock nb = ent.getKey();
	    Counter c = ent.getValue();
	    outputBranch(xw,nb,c.getValue());
	  }
       }
      xw.writeEndElement();

      next_block = null;
      next_count = 0;
      alt_block = null;
      alt_count = 0;
      other_blocks = null;
    }

   void outputBranch(XMLStreamWriter xw,RtBlock nb,int ct) throws XMLStreamException {
      xw.writeStartElement("BRANCH");
      xw.writeAttribute("TOBLOCK",Integer.toString(nb.getIndex()));
      xw.writeAttribute("COUNT",Integer.toString(ct));
      xw.writeEndElement();
    }

}	// end of inner class RtBlock



/********************************************************************************/
/*										*/
/*	Mutable counter 							*/
/*										*/
/********************************************************************************/

private static class Counter {

   private int count_value;

   Counter()				{ count_value = 0; }

   void incr()				{ ++count_value; }

   int getValue()			{ return count_value; }

}	// end of inner class Counter




/********************************************************************************/
/*										*/
/*	Trace information by thread						*/
/*										*/
/********************************************************************************/


private ThreadData getThreadData()
{
   Thread th = Thread.currentThread();
   ThreadData td = thread_stack.get(th);
   if (td == null) {
      synchronized (thread_stack) {
	 td = new ThreadData();
	 thread_stack.put(th,td);
       }
    }
   return td;
}



private static class ThreadData {

   private RtMethod method_id;
   private RtBlock block_id;
   private ThreadData from_data;
   private int call_level;

   ThreadData() {
      method_id = null;
      block_id = null;
      from_data = null;
      call_level = 0;
    }

   private ThreadData(RtMethod rm,RtBlock rb,ThreadData td) {
      method_id = rm;
      block_id = rb;
      from_data = td;
      call_level = 0;
    }

   int getLevel()			{ return call_level; }

   RtMethod enterMethod(RtMethod rm) {
      from_data = new ThreadData(rm,block_id,from_data);
      RtMethod orm = method_id;
      method_id = rm;
      block_id = null;
      ++call_level;
      return orm;
    }

   void exitMethod() {
      if (from_data != null) {
	 method_id = from_data.method_id;
	 block_id = from_data.block_id;
	 from_data = from_data.from_data;
	 --call_level;
       }
    }

   RtBlock enterBlock(RtBlock rb) {
      RtBlock orb = block_id;
      block_id = rb;
      return orb;
    }

}	// end of inner class ThreadData









}	// end of class BattAgent



/* end of BattAgent.java */
