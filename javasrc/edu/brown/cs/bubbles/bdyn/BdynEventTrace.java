/********************************************************************************/
/*										*/
/*		BdynEventTrace.java						*/
/*										*/
/*	Hold event trace for current run					*/
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



package edu.brown.cs.bubbles.bdyn;


import edu.brown.cs.ivy.swing.SwingEventListenerList;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;


class BdynEventTrace implements BdynConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpProcess	for_process;
private ThreadData	current_thread;
private PriorityQueue<TraceEntry> pending_entries;
private List<TraceEntry> thread_entries;
private Map<Integer,ThreadData> thread_map;
private long		next_time;
private Boolean 	cpu_time;
private int		thread_counter;
private int		task_counter;
private BdynFactory	bdyn_factory;
private Map<Integer,OutputTask> object_tasks;
private OutputTask	dummy_task;
private SortedSet<OutputEntry> output_set;
private long		end_time;
private long		max_delta;
private SortedSet<ThreadData> active_threads;
private SwingEventListenerList<BdynEventUpdater> update_listeners;
private SortedSet<Long> time_marks;


private static PrintWriter     trace_writer = null;
static {
   try {
      trace_writer = new PrintWriter(new FileWriter("/vol/tmp/bdyntrace.out"));
      // trace_writer = null;
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdynEventTrace(BumpProcess bp)
{
   for_process = bp;
   current_thread = null;
   pending_entries = new PriorityQueue<>(100,new EntryComparator());
   thread_entries = null;
   thread_map = new HashMap<>();
   next_time = 0;
   end_time = 0;
   cpu_time = null;
   thread_counter = 0;
   task_counter = 0;
   bdyn_factory = BdynFactory.getFactory();
   object_tasks = new HashMap<>();
   dummy_task = new OutputTask(0,null);
   output_set = new ConcurrentSkipListSet<>();
   active_threads = new TreeSet<>();
   time_marks = new ConcurrentSkipListSet<>();
   max_delta = 1;
   update_listeners = new SwingEventListenerList<>(BdynEventUpdater.class);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addUpdateListener(BdynEventUpdater up)
{
   update_listeners.add(up);
}


void removeUpdateListener(BdynEventUpdater up)
{
   update_listeners.remove(up);
}


long getStartTime()
{
   if (output_set.isEmpty()) return 0;
   return output_set.first().getStartTime();
}


long getEndTime()
{
   return end_time;
}

int getActiveThreadCount()
{
   return active_threads.size();
}


List<BdynEntryThread> getActiveThreads()
{
   return new ArrayList<BdynEntryThread>(active_threads);
}


BumpProcess getProcess()		{ return for_process; }


Set<Long> getTimeMarks()
{
   return time_marks;
}


void clear()
{
   pending_entries.clear();
   output_set.clear();
   object_tasks.clear();
   active_threads.clear();
   time_marks.clear();
   thread_entries = null;
   next_time = 0;
   end_time = 0;
   current_thread = null;
   thread_map.clear();
   cpu_time = null;
   thread_counter = 0;
   task_counter = 0;
   max_delta = 1;
}


void addTimeMark(long when)
{
   time_marks.add(when);
}


Iterator<Long> getTimeMarkIterator()		{ return time_marks.iterator(); }



/********************************************************************************/
/*										*/
/*	Trace creation methods							*/
/*										*/
/********************************************************************************/

void addEntry(String s)
{
   // System.err.println("TRACX: " + s);
   if (trace_writer != null) trace_writer.println(s);
   
   char s0 = s.charAt(0);
   if (s0 == 'T') {                             // THREAD
      if (thread_entries != null) {
	 for (TraceEntry te : thread_entries) pending_entries.add(te);
	 thread_entries = null;
       }
      StringTokenizer tok = new StringTokenizer(s);
      tok.nextToken();
      int id = Integer.parseInt(tok.nextToken());
      current_thread = thread_map.get(id);
      if (current_thread == null) {
	 int tid = Integer.parseInt(tok.nextToken());
	 String tnm = tok.nextToken("\n");
	 current_thread = new ThreadData(++thread_counter,tid,tnm);
	 thread_map.put(id,current_thread);
       }
      thread_entries = new ArrayList<TraceEntry>();
    }
   else if (s0 == 'D') {                        // DONE
      if (trace_writer != null) trace_writer.flush();
      if (thread_entries != null) {
	 for (TraceEntry te : thread_entries) pending_entries.add(te);
	 thread_entries = null;
       }
      StringTokenizer tok = new StringTokenizer(s);
      tok.nextToken();
      long time = Long.parseLong(tok.nextToken());
      if (next_time != 0) {
	 int ct = 0;
	 // System.err.println("TRACE: Pending size = " + pending_entries.size());
	 while (!pending_entries.isEmpty() && pending_entries.peek().getTime() < next_time) {
	    TraceEntry te = pending_entries.remove();
	    ++ct;
	    outputEntry(te);
	  }
	 if (ct > 0) {
	    for (BdynEventUpdater eu : update_listeners) {
	       eu.eventsAdded();
	     }
	  }
	 end_time = Math.max(next_time,end_time);
       }
      next_time = time;
    }
   else if (s0 == 'S') {
      if (cpu_time == null) {
	 StringTokenizer tok = new StringTokenizer(s);
	 tok.nextToken();
	 cpu_time = Boolean.parseBoolean(tok.nextToken());
       }
    }
   else if (cpu_time != null) {
      TraceEntry te = new TraceEntry(s,current_thread,cpu_time);
      if (thread_entries == null) pending_entries.add(te);
      else {
	 thread_entries.add(te);
	 if (te.isExit()) {
	    int sz = thread_entries.size();
	    int loc = te.getEntryLocation();
	    if (sz >= 4) {
	       TraceEntry t3 = thread_entries.get(sz-2);
	       if (!t3.isExit() && t3.getEntryLocation() == loc) {
		  TraceEntry t2 = thread_entries.get(sz-3);
		  if (t2.isExit() && t2.getEntryLocation() == loc) {
		     TraceEntry t1 = thread_entries.get(sz-4);
		     if (!t1.isExit() && t1.getEntryLocation() == loc &&
			   (te.getTime() - t1.getTime()) < MERGE_TIME) {
			t1.merge(t2.getTime(),t3.getTime(),te.getTime());
			thread_entries.remove(sz-2);
			thread_entries.remove(sz-3);
		      }
		   }
		}
	     }
	  }
       }
    }
}



private void outputEntry(TraceEntry te)
{
   ThreadData td = te.getThread();
   if (td == null) return;

   // System.err.println("TRACE: " + te);

   BdynCallback cb = bdyn_factory.getCallback(te.getEntryLocation());
   if (cb == null) return;

   if (cb.getCallbackType() == CallbackType.CONSTRUCTOR) {
      OutputTask ot = td.getCurrentTransaction();
      if (ot == null) return;
      if (ot.isMainTask() && !BdynFactory.getOptions().useMainTask()) return;
      int i0 = te.getObject1();
      if (i0 != 0) {
	 // System.err.println("ASSOC TASK " + i0 + " " + te.getObject2() + " " + ot.getTaskRoot().getDisplayName());
	 OutputTask ot1 = object_tasks.get(i0);
	 if (ot1 == null) object_tasks.put(i0,ot);
	 else if (ot1 != dummy_task && ot1 != ot) object_tasks.put(i0,dummy_task);
       }
      return;
    }

   td.beginTask(te);
   end_time = Math.max(end_time,te.getTime());
}



/********************************************************************************/
/*										*/
/*	Methods for accessing output data					*/
/*										*/
/********************************************************************************/

BdynRangeSet getRange(long t0,long t1)
{
   BdynRangeSet rslt = addToRange(t0-max_delta,t0,t1,null);

   return rslt;
}


BdynRangeSet updateRange(BdynRangeSet rslt,long t0,long t1)
{
   rslt = pruneRange(rslt,t0,t1);
   rslt = addToRange(t0,t0,t1,rslt);
   return rslt;
}



private BdynRangeSet pruneRange(BdynRangeSet rslt,long t0,long t1)
{
   if (rslt == null) return null;

   for (Iterator<Set<BdynEntry>> it = rslt.values().iterator(); it.hasNext(); ) {
      Set<BdynEntry> vals = it.next();
      int ct = 0;
      for (Iterator<BdynEntry> it1 = vals.iterator(); it1.hasNext(); ) {
	 BdynEntry oe = it1.next();
	 if (oe.getEndTime(t1) < t0) 
	    it1.remove();
	 else ++ct;
       }
      if (ct == 0) it.remove();
    }

   if (rslt.size() == 0) return null;

   return rslt;
}



private BdynRangeSet addToRange(long start,long t0,long t1,BdynRangeSet rslt)
{
   OutputEntry timee = new OutputEntry(start);
   SortedSet<OutputEntry> ss = output_set.tailSet(timee);
   for (OutputEntry e1 : ss) {
      if (e1.getStartTime() > t1) break;
      if (e1.getEndTime(t1) >= t0) {
	 ThreadData td = e1.getThread();
	 if (rslt == null) rslt = new BdynRangeSet();
	 Set<BdynEntry> r1 = rslt.get(td);
	 if (r1 == null) {
	    r1 = new HashSet<>();
	    rslt.put(td,r1);
	  }
	 r1.add(e1);
       }
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Thread information							*/
/*										*/
/********************************************************************************/

private class ThreadData implements Comparable<ThreadData>, BdynEntryThread {

   private int	output_id;
   private String thread_name;
   private OutputTask current_transaction;
   private int	nest_level;
   private Stack<OutputEntry> output_stack;
   private OutputEntry last_entry;

   ThreadData(int oid,int tid,String tnm) {
      output_id = oid;
      thread_name = tnm;
      current_transaction = null;
      nest_level = 0;
      output_stack = new Stack<>();
      last_entry = null;
    }

   @Override public String getThreadName() { return thread_name; }
   int getOutputId()			{ return output_id; }
   OutputTask getCurrentTransaction()	{ return current_transaction; }

   void beginTask(TraceEntry te) {
      BdynCallback cb = bdyn_factory.getCallback(te.getEntryLocation());
      if (cb == null) return;
      
      if (trace_writer != null) trace_writer.println("PROCESS " + te.getEntryLocation() + " " + te.isExit() + " " + te.getTime());
   
      if (current_transaction == null) {
         OutputTask ot = null;
         if (te.isExit()) return;
         int i0 = te.getObject1();
         int i1 = te.getObject2();
         if (i0 != 0) {
            ot = object_tasks.get(i0);
            if (ot == dummy_task) ot = null;
          }
         if (ot == null) {
            if (i1!= 0) {
               ot = object_tasks.get(i1);
               if (ot == dummy_task) ot = null;
             }
          }
         if (ot == null) {
            ot = new OutputTask(++task_counter,cb);
          }
         current_transaction = ot;
         nest_level = 0;
       }
      if (!te.isExit()) {
         OutputEntry oe = null;
         if (nest_level == 0) {
            oe = createOutputEntry(te);
          }
         else if (cb.getCallbackType() == CallbackType.KEY && BdynFactory.getOptions().useKeyCallback()) {
            for (int i = output_stack.size()-1; i >= 0; --i) {
               OutputEntry poe = output_stack.get(i);
               if (poe != null) {
        	  poe.finishAt(te.getTime());
        	  if (trace_writer != null) trace_writer.println("FINISHINT " + poe.getEntryTask().getId() + " " + nest_level + " " + poe.getTotalTime(0) + " " + poe.hashCode());
        	  break;
        	}
             }
            oe = createOutputEntry(te);
          }
         else {
            if (trace_writer != null) trace_writer.println("IGNORE " + cb.getId() + " " + nest_level);
            // System.err.println("TRACE: Ignore " + cb.getDisplayName() + " " + nest_level);
          }
         output_stack.push(oe);
         active_threads.add(this);
         ++nest_level;
       }
      else {
         if (nest_level <= 0) {
            return;
          }
         --nest_level;
         OutputEntry oe = output_stack.pop();
         if (oe != null) {
            // TODO: max_delta doesn't take into account calls that haven't ended yet
            max_delta = Math.max(max_delta,te.getTime() - oe.getStartTime());
            oe.finishAt(te.getTime());
            if (trace_writer != null) trace_writer.println("FINISH " + oe.getEntryTask().getId() + " " + nest_level + " " + oe.getTotalTime(0) + " " + oe.hashCode());
            if (!oe.isRelevant()) {
               output_set.remove(oe);
               if (trace_writer != null) trace_writer.println("REMOVE KEY " + oe.getEntryTask().getId() + " " + nest_level);
               // System.err.println("REMOVE KEY " + oe.getEntryTask().getDisplayName());
               oe = null;
               for (int i = output_stack.size()-1; i >= 0; --i) {
                  OutputEntry poe = output_stack.get(i);
                  if (poe != null) {
                     poe.finishAt(0);
                     if (trace_writer != null) trace_writer.println("NO END " + poe.getEntryTask().getId() + " " + nest_level + " " + poe.hashCode());
                     break;
                   }
                }
             }
          }
   
         if (last_entry != null && oe != null &&
               last_entry.getEntryTask() == oe.getEntryTask() &&
               (te.getTime() - last_entry.getStartTime()) < MERGE_TIME) {
            last_entry.mergeWith(oe);
            output_set.remove(oe);
            if (trace_writer != null) trace_writer.println("MERGE KEY " + oe.getEntryTask().getId() + " " + nest_level);
            // System.err.println("REMOVE " + oe.getEntryTask().getDisplayName());
          }
   
         if (oe != null) last_entry = oe;
   
         if (nest_level == 0) endTask();
         else if (oe != null) {
            for (int i = output_stack.size()-1; i >= 0; --i) {
               OutputEntry poe = output_stack.get(i);
               if (poe != null) {
        	  if (!poe.isSignificant()) {
        	     output_set.remove(poe);
        	   }
        	  if (trace_writer != null) trace_writer.println("NEST KEY " + poe.getEntryTask().getId() + " " + nest_level + " " + poe.isSignificant());
        	  oe = new OutputEntry(te.getTime(),0,this,current_transaction,poe.getEntryTask());
        	  oe.setDeletable();
        	  output_set.add(oe);
        	  output_stack.set(i,oe);
        	  break;
        	}
             }
          }
       }
    }

   void endTask() {
      current_transaction = null;
      nest_level = 0;
      output_stack.clear();
    }

   OutputEntry createOutputEntry(TraceEntry te) {
      BdynCallback cb = bdyn_factory.getCallback(te.getEntryLocation());
      OutputEntry oe = new OutputEntry(te.getTime(),0,this,current_transaction,cb);
      if (te.getUseCount() > 1) oe.setUse(te.getUseCount(),te.getFractionUsed());
      output_set.add(oe);
      if (trace_writer != null) trace_writer.println("START " + cb.getId() + " " + nest_level + " " + te.getTime() + " " + oe.hashCode());
      return oe;
    }

   @Override public int compareTo(ThreadData td) {
      return getThreadName().compareTo(td.getThreadName());
    }

   @Override public String toString() {
      if (nest_level <= 0) return thread_name;
      return thread_name + "@" + nest_level;
    }

}	// end of inner class ThreadData



/********************************************************************************/
/*										*/
/*	Trace Entry								*/
/*										*/
/********************************************************************************/

private static class TraceEntry {

   private long 	entry_time;
   private ThreadData	entry_thread;
   private int		entry_loc;
   private int		entry_o1;
   private int		entry_o2;
   private boolean	is_exit;
   private int		use_count;
   private double	fraction_used;

   TraceEntry(String s,ThreadData td,boolean cputime) {
      String [] args = s.split(" ");
      is_exit = false;
      int ct = 0;
      entry_loc = Integer.parseInt(args[ct++]);
      if (entry_loc < 0) {
	 is_exit = true;
	 entry_loc = -entry_loc;
       }
      entry_time = Long.parseLong(args[ct++]);
      if (cputime && ct < args.length) Long.parseLong(args[ct++]);
      entry_thread = td;
      if (ct < args.length) entry_o1 = Integer.parseInt(args[ct++]);
      else entry_o1 = 0;
      if (ct < args.length) entry_o2 = Integer.parseInt(args[ct++]);
      else entry_o2 = 0;
      use_count = 1;
      fraction_used = 1;
    }

   long getTime()			{ return entry_time; }
   ThreadData getThread()		{ return entry_thread; }
   int getEntryLocation()		{ return entry_loc; }
   int getObject1()			{ return entry_o1; }
   int getObject2()			{ return entry_o2; }
   boolean isExit()			{ return is_exit; }
   int getUseCount()			{ return use_count; }
   double getFractionUsed()		{ return fraction_used; }


   void merge(long et0,long st1,long et1) {
      double tot = et1 - st1;
      if (use_count <= 1) tot += et0 - entry_time;
      else {
	 tot += (et0 - entry_time) * fraction_used;
       }
      ++use_count;
      fraction_used = tot / (et1 - entry_time);
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(entry_loc);
      if (is_exit) buf.append("^");
      buf.append(" ");
      buf.append(entry_time);
      buf.append(" ");
      buf.append(entry_thread.getOutputId());
      buf.append(" ");
      buf.append(entry_o1);
      buf.append(" ");
      buf.append(entry_o2);
      return buf.toString();
   }

}	// end of inner class TraceEntry


private static class EntryComparator implements Comparator<TraceEntry>
{

   @Override public int compare(TraceEntry e1,TraceEntry e2) {
      long d0 = e1.getTime() - e2.getTime();
      if (d0 < 0) return -1;
      else if (d0 > 0) return 1;
      return 0;
    }

}	// end of inner class EntryComparator


/********************************************************************************/
/*										*/
/*	OutputEntry -- entry to actually visualize				*/
/*										*/
/********************************************************************************/

private static class OutputEntry implements Comparable<OutputEntry>, BdynEntry {

   private long start_time;
   private long finish_time;
   private ThreadData entry_thread;
   private OutputTask entry_transaction;
   private BdynCallback entry_task;
   private int num_traces;
   private boolean can_delete;
   private float trace_fraction;

   OutputEntry(long startt,long endt,ThreadData td,OutputTask ot,BdynCallback tt) {
      start_time = startt;
      finish_time = (endt == 0 ? MAX_TIME : endt);
      entry_thread = td;
      entry_transaction = ot;
      entry_task = tt;
      num_traces = 1;
      trace_fraction = 1;
      can_delete = (tt.getCallbackType() == CallbackType.KEY);
    }

   OutputEntry(long time) {
      start_time = time;
      finish_time = time;
      entry_thread = null;
      entry_transaction = null;
      entry_task = null;
      num_traces = 0;
      trace_fraction = 1;
      can_delete = true;
    }

   @Override public long getStartTime() 	{ return start_time; }
   @Override public long getEndTime(long max) {
      if (max == 0) {
         if (finish_time == MAX_TIME) return 0;
         return finish_time;
       }
      return Math.min(max,finish_time);
    }
   @Override public long getTotalTime(long max) {
      long d0 = getEndTime(max) - start_time;
      if (num_traces > 0) d0 = (long) (d0 * trace_fraction);
      return d0;
    }

   void finishAt(long time) {
      if (time == 0 || time == MAX_TIME) finish_time = MAX_TIME;
      else if (finish_time == MAX_TIME || finish_time == 0) finish_time = time;
      else finish_time = Math.max(finish_time,time);
    }
   
   void setUse(int count,double fract) {
      num_traces = count;
      trace_fraction = (float) fract;
    }

   ThreadData getThread()				{ return entry_thread; }
   @Override public BdynEntryThread getEntryThread()	{ return entry_thread; }
   @Override public BdynCallback getEntryTask() 	{ return entry_task; }
   @Override public BdynCallback getEntryTransaction()	{ return entry_transaction.getTaskRoot(); }
   
   void setDeletable()                                  { can_delete = true; }

   boolean isRelevant() {
      if (finish_time == MAX_TIME) return true;
      // ignore KEY events with less than 10 ms times
      if (can_delete && getTotalTime(0) < IGNORE_TIME) return false;
      return true;
    }
   
   boolean isSignificant() {
      if (finish_time == MAX_TIME) return true;
      if (getTotalTime(0) < IGNORE_TIME) return false;
      return true;
   }
     
   void mergeWith(OutputEntry oe) {
      num_traces += oe.num_traces;
      double tot = getTotalTime(0) + oe.getTotalTime(0);
      finish_time = oe.finish_time;
      trace_fraction = ((float)(tot / (finish_time - start_time)));
    }

   @Override public int compareTo(OutputEntry e) {
      long dl = start_time - e.start_time;
      if (dl < 0) return -1;
      if (dl > 0) return 1;
      dl = finish_time - e.finish_time;
      if (dl < 0) return -1;
      if (dl > 0) return 1;
      int idl = entry_thread.getOutputId() - e.entry_thread.getOutputId();
      if (idl < 0) return -1;
      if (idl > 0) return 1;
      return 0;
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      if (entry_transaction != null && getEntryTransaction() != entry_task) {
	 buf.append(getEntryTransaction().toString());
	 buf.append("::");
       }
      if (entry_task != null) {
	 buf.append(entry_task.toString());
       }
      buf.append("@");
      buf.append(start_time);
      if (finish_time != 0 && finish_time != MAX_TIME) {
	 buf.append("-");
	 buf.append(finish_time);
       }
      return buf.toString();
    }

}	// end of inner class OutputEntry



private static class OutputTask implements BdynEntryTask {

   private BdynCallback task_root;

   OutputTask(int id,BdynCallback root) {
      task_root = root;
    }

   @Override public BdynCallback getTaskRoot()	{ return task_root; }
   
   boolean isMainTask() {
      if (task_root == null) return true;
      if (task_root.getCallbackType() == CallbackType.MAIN) return true;
      return false;
    }

}



}	// end of class BdynEventTrace




/* end of BdynEventTrace.java */

