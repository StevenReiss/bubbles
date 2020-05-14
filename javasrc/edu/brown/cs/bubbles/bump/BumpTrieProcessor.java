/********************************************************************************/
/*										*/
/*		BumpTrieProcessor.java						*/
/*										*/
/*	Build the performance trie from a debugger session			*/
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



package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunEventHandler;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;

class BumpTrieProcessor implements BumpConstants, BumpRunEventHandler
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BumpProcess,TrieDataImpl> process_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpTrieProcessor()
{
   process_data = new WeakHashMap<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpTrieData getTrieDataForProcess(BumpProcess bp)
{
   if (bp == null) return null;

   BumpTrieData bd = process_data.get(bp);
   if (bd == null) {
      bd = setupProcess(bp);
    }

   return bd;
}




/********************************************************************************/
/*										*/
/*	Setup a new process							*/
/*										*/
/********************************************************************************/

private synchronized TrieDataImpl setupProcess(BumpProcess bp)
{
   TrieDataImpl td = process_data.get(bp);
   if (td != null) return td;
   td = new TrieDataImpl(bp);
   process_data.put(bp,td);
   return td;
}




/********************************************************************************/
/*										*/
/*	Run Events								*/
/*										*/
/********************************************************************************/

@Override public void handleLaunchEvent(BumpRunEvent evt)		{ }

@Override public void handleThreadEvent(BumpRunEvent evt)		{ }

@Override public void handleConsoleMessage(BumpProcess p,boolean err,boolean eof,String msg)
{ }

@Override public synchronized void handleProcessEvent(BumpRunEvent evt)
{
   BumpProcess proc = evt.getProcess();
   if (proc == null) return;
   TrieDataImpl bp = process_data.get(proc);

   switch (evt.getEventType()) {
      case PROCESS_ADD :
	 if (bp == null) setupProcess(evt.getProcess());
	 break;
      case PROCESS_REMOVE :
	 if (bp != null) {
	    process_data.remove(proc);
	  }
	 break;
      case PROCESS_CHANGE :
	 break;
      case PROCESS_PERFORMANCE :
	 break;
      case PROCESS_SWING :
	 break;
      case PROCESS_TRIE :
	 if (bp != null) {
	    Element xml = (Element) evt.getEventData();
	    bp.handleTrieEvent(xml);
	  }
	 break;
      case PROCESS_TRACE :
	 break;
      default :
	 break;
    }
}


/********************************************************************************/
/*										*/
/*	Container of data for a process 					*/
/*										*/
/********************************************************************************/

private static class TrieDataImpl implements BumpTrieData {

   private BumpProcess for_process;
   private TrieNodeImpl root_node;
   private Map<Integer,TrieNodeImpl> trie_data;
   private Map<Integer,BumpThread> thread_data;
   private PriorityQueue<Element> queued_events;
   private int event_seq;
   private double base_samples;
   private double total_samples;
   private double base_time;

   TrieDataImpl(BumpProcess bp) {
      for_process = bp;
      trie_data = new HashMap<>();
      thread_data = new HashMap<>();
      queued_events = new PriorityQueue<>(10,new EventComparator());
      root_node = null;
      event_seq = 0;
      base_samples = 0;
      base_time = 0;
    }

   @Override public BumpTrieNode getRoot()		{ return root_node; }
   @Override public double getBaseSamples()		{ return base_samples; }
   @Override public double getTotalSamples()		{ return total_samples; }
   @Override public double getBaseTime()		{ return base_time; }

   void handleTrieEvent(Element xml) {
      int seqid = IvyXml.getAttrInt(xml,"SEQ");
      if (seqid == event_seq+1) {
	 processTrieEvent(xml);
	 while (!queued_events.isEmpty()) {
	    Element e1 = queued_events.element();
	    int sq = IvyXml.getAttrInt(e1,"SEQ");
	    if (sq != event_seq+1) break;
	    e1 = queued_events.remove();
	    processTrieEvent(e1);
	  }
       }
      else {
	 queued_events.add(xml);
       }
    }

   BumpThread getThreadFromId(int id) {
      return thread_data.get(id);
    }

   TrieNodeImpl getTrieNodeFromId(int id) {
      return trie_data.get(id);
    }

   private void processTrieEvent(Element xml) {
      base_samples = IvyXml.getAttrDouble(xml,"ACTIVE",0);
      total_samples = IvyXml.getAttrDouble(xml,"SAMPLES",0);
      base_time = IvyXml.getAttrDouble(xml,"TIME",0);
      event_seq = IvyXml.getAttrInt(xml,"SEQ",event_seq+1);

      for (Element thel : IvyXml.children(xml,"THREAD")) {
	 handleThreadData(thel);
       }
      for (Element trel : IvyXml.children(xml,"TRIENODE")) {
	 handleTrieNode(trel);
       }
    }

   private void handleThreadData(Element xml) {
      int id = IvyXml.getAttrInt(xml,"ID");
      String nm = IvyXml.getAttrString(xml,"NAME");
      String tid = IvyXml.getAttrString(xml,"TID");
      for (BumpThread th : for_process.getThreads()) {
	 if (th.getId().equals(tid) && th.getName().equals(nm)) {
	    thread_data.put(id,th);
	    break;
	  }
       }
    }

   private void handleTrieNode(Element xml) {
      int id = IvyXml.getAttrInt(xml,"ID");
      TrieNodeImpl tn = trie_data.get(id);
      if (tn == null) {
	 tn = new TrieNodeImpl(this,xml);
	 trie_data.put(id,tn);
	 if (IvyXml.getAttrBool(xml,"ROOT")) {
	    root_node = tn;
	  }
       }
      else tn.update(xml);
    }

}	// end of inner class BumpTrieData




/********************************************************************************/
/*										*/
/*	Trie Node implementation						*/
/*										*/
/********************************************************************************/

private static class TrieNodeImpl implements BumpTrieNode {

   private TrieDataImpl base_data;
   private TrieNodeImpl parent_node;
   private List<TrieNodeImpl> child_nodes;
   private String class_name;
   private String method_name;
   private int	  line_number;
   private String file_name;
   private int [] count_data;
   private int [] total_data;
   private Map<BumpThread,int []> thread_counts;

   TrieNodeImpl(TrieDataImpl bd,Element xml) {
      base_data = bd;
      parent_node = null;
      child_nodes = null;
      class_name = null;
      method_name = null;
      line_number = 0;
      file_name = null;
      count_data = null;
      total_data = null;
      thread_counts = null;
      setValues(xml);
      updateCounts(xml);
    }

   @Override public BumpTrieNode getParent()		{ return parent_node; }
   @Override public Collection<BumpTrieNode> getChildren() {
      List<BumpTrieNode> rslt = new ArrayList<>();
      if (child_nodes != null) rslt.addAll(child_nodes);
      return rslt;
    }
   @Override public int [] getCounts()		{ return count_data; }
   @Override public Collection<BumpThread> getThreads() {
      List<BumpThread> rslt = new ArrayList<>();
      if (thread_counts != null) {
	 rslt.addAll(thread_counts.keySet());
       }
      return rslt;
    }
   @Override public int [] getThreadCounts(BumpThread th) {
      return thread_counts.get(th);
    }
   @Override public String getClassName()		{ return class_name; }
   @Override public String getMethodName()		{ return method_name; }
   @Override public int getLineNumber() 		{ return line_number; }
   @Override public String getFileName()		{ return file_name; }

   @Override public int [] getTotals()			{ return total_data; }

   void update(Element xml) {
      if (parent_node == null && IvyXml.getAttrPresent(xml,"PARENT")) {
	 setValues(xml);
       }
      updateCounts(xml);
    }

   private void setValues(Element xml) {
      if (parent_node == null) {
	 int pid = IvyXml.getAttrInt(xml,"PARENT");
	 if (pid > 0) {
	    parent_node = base_data.getTrieNodeFromId(pid);;
	    if (parent_node != null) parent_node.addChild(this);
	  }
       }
      class_name = IvyXml.getAttrString(xml,"CLASS",class_name);
      method_name = IvyXml.getAttrString(xml,"METHOD",method_name);
      line_number = IvyXml.getAttrInt(xml,"LINE",line_number);
      file_name = IvyXml.getAttrString(xml,"FILE",file_name);
    }

   private void addChild(TrieNodeImpl ch) {
      if (child_nodes == null) child_nodes = new ArrayList<TrieNodeImpl>(4);
      child_nodes.add(ch);
    }

   private void updateCounts(Element xml) {
      count_data = getCounts(xml,count_data);
      for (Element th : IvyXml.children(xml,"THREAD")) {
	 int tid = IvyXml.getAttrInt(th,"ID");
	 BumpThread bt = base_data.getThreadFromId(tid);
	 if (bt != null) {
	    if (thread_counts == null) thread_counts = new HashMap<>();
	    int [] cts = thread_counts.get(bt);
	    if (cts == null) {
	       cts = getCounts(th,cts);
	       if (cts != null) thread_counts.put(bt,cts);
	     }
	    else getCounts(th,cts);
	  }
       }
    }

   private int [] getCounts(Element xml,int [] cts) {
      int rct = IvyXml.getAttrInt(xml,"RUN",0);
      int ict = IvyXml.getAttrInt(xml,"IO",0);
      int wct = IvyXml.getAttrInt(xml,"WAIT",0);
      if (rct > 0 || ict > 0 || wct > 0) {
	 if (cts == null) cts = new int[BUMP_TRIE_OP_COUNT];
	 cts[BUMP_TRIE_OP_RUN] = rct;
	 cts[BUMP_TRIE_OP_IO] = ict;
	 cts[BUMP_TRIE_OP_WAIT] = wct;
       }
      return cts;
    }

   @Override public void computeTotals() {
      if (count_data == null && child_nodes == null) {
	 total_data = null;
       }
      else {
	 boolean ok = false;
	 total_data = new int[BUMP_TRIE_OP_COUNT];
	 if (count_data != null) {
	    ok = true;
	    for (int i = 0; i < BUMP_TRIE_OP_COUNT; ++i) total_data[i] += count_data[i];
	  }
	 if (child_nodes != null) {
	    for (TrieNodeImpl tni : child_nodes) {
	       tni.computeTotals();
	       int [] tots = tni.total_data;
	       if (tots != null) {
		  ok = true;
		  for (int i = 0; i < BUMP_TRIE_OP_COUNT; ++i) total_data[i] += tots[i];
		}
	     }
	  }
	 if (!ok) total_data = null;
       }
    }

   @Override public String toString() {
      String s = "<";
      if (getClassName() == null) s += "^";
      else {
	 s += getClassName() + "." + getMethodName() + "@" + getLineNumber();
       }
      if (count_data != null) {
	 s += " " + count_data;
       }
      s += ">";
      return s;
    }

}	// end of inner class TrieNodeImpl



/********************************************************************************/
/*										*/
/*	Comparator for events in priority queue 				*/
/*										*/
/********************************************************************************/

private static class EventComparator implements Comparator<Element> {

   @Override public int compare(Element e1,Element e2) {
      int i1 = IvyXml.getAttrInt(e1,"SEQ");
      int i2 = IvyXml.getAttrInt(e2,"SEQ");
      if (i1 < i2) return -1;
      if (i1 > i2) return 1;
      return 0;
    }

}	// end of inner class EventComparator







}	// end of class BumpTrieProcessor




/* end of BumpTrieProcessor.java */

