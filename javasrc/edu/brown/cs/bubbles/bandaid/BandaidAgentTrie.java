/********************************************************************************/
/*										*/
/*		BandaidAgentTrie.java						*/
/*										*/
/*	Bandaid Agent to maintain a trie for runtime visualization		*/
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



package edu.brown.cs.bubbles.bandaid;


import java.lang.management.ThreadInfo;
import java.util.*;


class BandaidAgentTrie extends BandaidAgent implements BandaidConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private TrieNode	root_node;
private long		last_sample;
private long		sample_count;
private long		tsample_count;
private long [] 	total_counts;
private int		max_level;

private long		active_samples;
private long		total_active;
private boolean 	have_active;
private long		last_delta;

private Map<Long,ThreadData> thread_map;
private List<ThreadData> thread_list;

private static int	node_counter = 0;
private static int	thread_counter = 0;
private static int	seq_counter = 0;

private static final int	OP_RUN = 0;
private static final int	OP_WAIT = 1;
private static final int	OP_IO = 2;
private static final int	CHANGE = 3;
private static final int	OP_CT = 4;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidAgentTrie(BandaidController bc)
{
   super(bc,"TrieBuilder");

   last_sample = 0;
   sample_count = 0;
   tsample_count = 0;
   total_counts = new long[OP_CT];
   for (int i = 0; i < OP_CT; ++i) total_counts[i] = 0;

   have_active = false;
   active_samples = 0;
   total_active = 0;
   last_delta = 0;

   max_level = Integer.MAX_VALUE;

   root_node = new TrieNode();

   thread_map = new WeakHashMap<Long,ThreadData>();
   thread_list = new ArrayList<ThreadData>();
}



/********************************************************************************/
/*										*/
/*	Processing Methods							*/
/*										*/
/********************************************************************************/

@Override public void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   ThreadData td = thread_map.get(ti.getThreadId());
   if (td == null) {
      td = new ThreadData(ti);
      thread_map.put(ti.getThreadId(),td);
      thread_list.add(td);
    }

   if (last_sample != now) {
      ++sample_count;
      if (last_sample == 0) last_delta = 0;
      else last_delta = now - last_sample;
      last_sample = now;
      have_active = false;
    }

   if (ti.getThreadState() == Thread.State.RUNNABLE) {
      ++tsample_count;
    }

   // check for I/O
   StackTraceElement ioc = null;
   int startidx = -1;

   if (ti.getThreadState() == Thread.State.RUNNABLE) {
      // check if doing I/O and if so find first user call for that I/O
      for (int j = 0; j < trc.length; ++j) {
	 StackTraceElement te = trc[j];
	 String nm = te.getClassName();
	 if (j == 0 && the_control.isIOClass(nm)) ioc = te;
	 else if (j == 0) break;
	 else if (startidx < 0 && the_control.isSystemClass(nm)) ioc = te;
	 else if (startidx < 0) startidx = j;
       }

      if (ioc != null && startidx >= 0) {
	 addIoInstance(trc,startidx,td);
       }
      else {
	 // not I/O: Find first user class on the stack if there is one
	 int frst = -1;
	 for (int j = 0; j < trc.length; ++j) {
	    StackTraceElement te = trc[j];
	    String nm = te.getClassName();
	    if (!the_control.isSystemClass(nm)) {
	       frst = j;
	       break;
	     }
	  }
	 if (frst >= 0) {
	    if (!have_active) { 	// only once per check
	       have_active = true;
	       total_active += last_delta;
	       ++active_samples;
	     }
	    addRunInstance(trc,frst,td);
	  }
       }
    }

   if (ti.getThreadState() == Thread.State.WAITING ||
	 ti.getThreadState() == Thread.State.TIMED_WAITING) {
      startidx = -1;
      for (int j = 0; j < trc.length; ++j) {
	 StackTraceElement te = trc[j];
	 String nm = te.getClassName();
	 if (startidx < 0 && the_control.isSystemClass(nm)) ;
	 else if (startidx < 0) startidx = j;
       }
      if (startidx >= 0) {
	 addWaitInstance(trc,startidx,td);
       }
    }
}



@Override void handleDoneStacks(long now)
{ }



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override public void generateReport(BandaidXmlWriter xw,long now)
{
   xw.begin("TRIE");
   xw.field("SEQ",++seq_counter);
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("TSAMPLES",tsample_count);
   xw.field("ACTIVE",active_samples);
   xw.field("TIME",total_active);
   xw.field("LAST",last_sample);
   xw.field("TOTRUN",total_counts[OP_RUN]);
   xw.field("TOTIO",total_counts[OP_IO]);
   xw.field("TOTWAIT",total_counts[OP_WAIT]);

   for (ThreadData td : thread_list) {
      td.report(xw);
    }
   thread_list.clear();

   root_node.report(xw,null);

   xw.end();
}



/********************************************************************************/
/*										*/
/*	Stack snapshot checking for I/O and Wait processing			*/
/*										*/
/********************************************************************************/

void addIoInstance(StackTraceElement [] trc,int idx,ThreadData td)
{
   root_node.insert(trc,idx,OP_IO,td);
}



void addWaitInstance(StackTraceElement [] trc,int idx,ThreadData td)
{
   root_node.insert(trc,idx,OP_WAIT,td);
}



void addRunInstance(StackTraceElement [] stk,int idx,ThreadData td)
{
   root_node.insert(stk,idx,OP_RUN,td);
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining a method-trie					*/
/*										*/
/********************************************************************************/

private class TrieNode {

   private int node_id;
   private String file_name;
   private String class_name;
   private String method_name;
   private int line_number;
   private int [] num_counts;
   private Map<Integer,int[]> thread_counts;
   private List<TrieNode> next_nodes;
   private boolean is_system;
   private long last_update;
   private long last_output;

   TrieNode() {
      method_name = null;
      class_name = null;
      line_number = 0;
      num_counts = new int[OP_CT];
      next_nodes = new Vector<TrieNode>();
      thread_counts = null;
      is_system = true;
      node_id = ++node_counter;
      last_update = 0;
      last_output = 0;
    }

   TrieNode(StackTraceElement trc) {
      this();
      file_name = trc.getFileName();
      class_name = trc.getClassName();
      method_name = trc.getMethodName();
      line_number = trc.getLineNumber();
      is_system = the_control.isSystemClass(class_name);
      if (method_name == null) method_name = "*";
    }

   private void count(int op,int tid) {
      total_counts[op]++;
      num_counts[op]++;
      num_counts[CHANGE] = 1;
      if (tid > 0) {
	 if (thread_counts == null) thread_counts = new HashMap<Integer,int []>();
	 int [] tcts = thread_counts.get(tid);
	 if (tcts == null) {
	    tcts = new int[OP_CT];
	    thread_counts.put(tid,tcts);
	  }
	 tcts[op]++;
	 tcts[CHANGE] = 1;
       }
    }

   void insert(StackTraceElement [] trc,int idx,int op,ThreadData td) {
      insertItem(trc,trc.length-1,idx,op,0,td.getId());
    }


   private void insertItem(StackTraceElement [] trc,int idx,int base,int op,int lvl,int tid) {
      last_update = last_sample;
      if (idx < base || lvl >= max_level) {
	 if (class_name != null) count(op,tid);
	 return;
       }

      for (TrieNode tn : next_nodes) {
	 if (tn.matches(trc[idx])) {
	    tn.insertItem(trc,idx-1,base,op,lvl+1,tid);
	    return;
	  }
       }

      if (op == OP_RUN && base < idx-1) base = idx-1;

      TrieNode tn = createTrieNode(trc,idx,base,op,lvl+1,tid);
      if (tn == null) count(op,tid);
      else next_nodes.add(tn);
    }

   private TrieNode createTrieNode(StackTraceElement [] trc,int idx,int base,int op,int lvl,int tid) {
      if (idx < base || lvl >= max_level) return null;
      TrieNode cn = createTrieNode(trc,idx-1,base,op,lvl+1,tid);
      TrieNode tn = new TrieNode(trc[idx]);
      if (cn == null) {
	 tn.count(op,tid);
       }
      else {
	 tn.next_nodes.add(cn);
       }
      return tn;
    }

   private boolean matches(StackTraceElement trc) {
      if (class_name == null) return false;
      if (!class_name.equals(trc.getClassName())) return false;
      if (method_name == null && trc.getMethodName() == null) return true;
      if (method_name == null) return false;
      if (!method_name.equals(trc.getMethodName())) return false;
      if (line_number != trc.getLineNumber()) return false;
      return true;
    }

   void report(BandaidXmlWriter xw,TrieNode par) {
      if (last_update != 0 && last_update < last_output) return;
      if (last_update == 0) last_update = last_sample;

      if (last_output == 0 || num_counts[CHANGE] != 0) {
	 if (class_name != null) {
	    xw.begin("TRIENODE");
	    xw.field("ID",node_id);
	    if (is_system) xw.field("SYS",true);
	    if (last_output == 0) {
	       xw.field("PARENT",par.node_id);
	       xw.field("CLASS",class_name);
	       xw.field("METHOD",method_name);
	       xw.field("LINE",line_number);
	       xw.field("FILE",file_name);
	       file_name = null;
	     }
	    outputFields(xw,num_counts);
	    if (thread_counts != null) {
	       for (Map.Entry<Integer,int[]> ent : thread_counts.entrySet()) {
		  int [] tcts = ent.getValue();
		  if (tcts[CHANGE] != 0) {
		     xw.begin("THREAD");
		     xw.field("ID",ent.getKey());
		     outputFields(xw,tcts);
		     xw.end();
		   }
		}
	     }
	    xw.end();
	  }
	 else if (last_output == 0) {
	    xw.begin("TRIENODE");
	    xw.field("ID",node_id);
	    xw.field("ROOT",true);
	    xw.end();
	  }
       }

      for (TrieNode tn : next_nodes) {
	 tn.report(xw,this);
       }

      last_output = last_sample;
    }

   private void outputFields(BandaidXmlWriter xw,int [] cts) {
      if (cts[CHANGE] == 0) return;
      if (cts[OP_RUN] != 0) xw.field("RUN",cts[OP_RUN]);
      if (cts[OP_IO] != 0) xw.field("IO",cts[OP_IO]);
      if (cts[OP_WAIT] != 0) xw.field("WAIT",cts[OP_WAIT]);
      cts[CHANGE] = 0;
    }

}	// end of subclass TrieNode



/********************************************************************************/
/*										*/
/*	Thread information							*/
/*										*/
/********************************************************************************/

private static class ThreadData {

   private int	thread_id;
   private ThreadInfo for_thread;

   ThreadData(ThreadInfo th) {
      for_thread = th;
      thread_id = ++thread_counter;
    }

   int getId()				{ return thread_id; }

   void report(BandaidXmlWriter xw) {
      if (for_thread == null) return;

      xw.begin("THREAD");
      xw.field("NAME",for_thread.getThreadName());
      xw.field("TID",for_thread.getThreadId());
      xw.field("ID",thread_id);
      xw.end();

      for_thread = null;
    }

}	// end of inner class ThreadData



}	// end of class BandaidAgentTrie



/* end of BandaidAgentTrie.java */

