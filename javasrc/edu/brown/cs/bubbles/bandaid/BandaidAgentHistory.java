/********************************************************************************/
/*										*/
/*		BandaidAgentHistory.java					*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data history agent		*/
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



import java.lang.management.ThreadInfo;
import java.util.*;


class BandaidAgentHistory extends BandaidAgent implements BandaidConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String,ThreadData>	thread_data;
private long			last_sample;
private Map<String,StopData>	stop_data;
private long [] 		sample_time;
private int			sample_count;

private final static int	BUFFER_SIZE = 50;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidAgentHistory(BandaidController bc)
{
   super(bc,"History");

   thread_data = new HashMap<String,ThreadData>();
   stop_data = new HashMap<String,StopData>();
   last_sample = 0;
   sample_time = new long [BUFFER_SIZE];
   sample_count = 0;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override void handleCommand(String cmd,String args)
{
   if (cmd.equalsIgnoreCase("DUMP")) {
      try {
	 BandaidXmlWriter xw = new BandaidXmlWriter();
	 xw.begin("BANDAID");
	 xw.field("HISTORY",the_control.getProcessId());
	 xw.field("THREAD",args);
	 if (generateHistory(xw,args)) {
	    xw.end();
	    the_control.sendMessage(xw.getContents());
	  }
       }
      catch (NumberFormatException e) { }
    }
}



boolean generateHistory(BandaidXmlWriter xw,String tnm)
{
   StopData sd = stop_data.get(tnm);
   if (sd == null) {
      System.err.println("HISTORY: Thread " + tnm + " not stopped or known");
      return false;
    }

   sd.outputXml(xw);

   return true;
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   if (last_sample != now) {
      last_sample = now;
      sample_count = (sample_count + 1) % BUFFER_SIZE;
      sample_time[sample_count] = now;
    }

   String tnm = ti.getThreadName();
   ThreadData td = thread_data.get(tnm);
   if (td == null) {
      td = new ThreadData(ti);
      thread_data.put(tnm,td);
    }

   td.setState(now,ti,trc);
}



@Override void handleDoneStacks(long now)
{
   int prior = (sample_count + BUFFER_SIZE - 1) % BUFFER_SIZE;
   long pwhen = sample_time[prior];

   StopData sd = null;

   for (Iterator<ThreadData> it = thread_data.values().iterator(); it.hasNext(); ) {
      ThreadData td = it.next();
      if (td.shouldRemove(pwhen)) {
	 it.remove();
       }
    }

   for (Iterator<Map.Entry<String,ThreadData>> it = thread_data.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String,ThreadData> ent = it.next();
      String tnm = ent.getKey();
      ThreadData td = ent.getValue();
      if (td.isStopped()) {
	 if (stop_data.get(tnm) != null) continue;
	 if (sd == null) {
	    long swhen = 0;
	    for (int i = 0; i < BUFFER_SIZE; ++i) {
	       int idx = (sample_count + i + 1) % BUFFER_SIZE;
	       if (sample_time[idx] > 0) {
		  swhen = sample_time[idx];
		  break;
		}
	     }
	    sd = new StopData(swhen,now);
	  }
	 stop_data.put(tnm,sd);
       }
      else stop_data.remove(tnm);
    }
}




/********************************************************************************/
/*										*/
/*	Thread state Data holder						*/
/*										*/
/********************************************************************************/

private static class ThreadData {

   int current_count;
   long [] stack_time;
   ThreadInfo [] thread_state;
   StackTraceElement [][] thread_stack;

   ThreadData(ThreadInfo ti) {
      current_count = 0;
      stack_time = new long[BUFFER_SIZE];
      thread_state = new ThreadInfo[BUFFER_SIZE];
      thread_stack = new StackTraceElement[BUFFER_SIZE][];
    }

   ThreadData(long min,ThreadData td) {
      current_count = 0;
      stack_time = new long[BUFFER_SIZE];
      thread_state = new ThreadInfo[BUFFER_SIZE];
      thread_stack = new StackTraceElement[BUFFER_SIZE][];
      for (int i = 0; i < BUFFER_SIZE; ++i) {
	 int j = (td.current_count + i) % BUFFER_SIZE;
	 if (td.stack_time[j] == 0 || td.stack_time[j] < min) continue;
	 stack_time[current_count] = td.stack_time[j];
	 thread_state[current_count] = td.thread_state[j];
	 thread_stack[current_count] = td.thread_stack[j];
	 current_count = (current_count + 1) % BUFFER_SIZE;
       }
    }

   void setState(long when,ThreadInfo ti,StackTraceElement [] trc) {
      stack_time[current_count] = when;
      thread_state[current_count] = ti;
      thread_stack[current_count] = trc;
      current_count = (current_count + 1) % BUFFER_SIZE;
    }

   boolean shouldRemove(long min) {
      int pct = (current_count + BUFFER_SIZE - 1) % BUFFER_SIZE;
      if (min > stack_time[pct]) return true;
      return false;
    }

   boolean isStopped() {
      int pct = (current_count + BUFFER_SIZE - 1) % BUFFER_SIZE;
      return thread_state[pct].isSuspended();
    }

   void outputXml(BandaidXmlWriter xw) {
      int otct = 0;
      for (int i = 0; i < BUFFER_SIZE; ++i) {
	 int j = (current_count + BUFFER_SIZE - 1 - i) % BUFFER_SIZE;
	 if (stack_time[j] == 0) break;
	 if (otct++ == 0) {
	    xw.begin("THREAD");
	    xw.field("ID",thread_state[j].getThreadId());
	    xw.field("NAME",thread_state[j].getThreadName());
	  }
	 xw.begin("TRACE");
	 xw.field("WHEN",stack_time[j]);
	 xw.field("STATE",thread_state[j].getThreadState().toString());
	 int k0 = 0;
	 for (int k = 0; k < thread_stack[j].length; ++k) {
	    StackTraceElement te = thread_stack[j][k];
	    if (te.getClassName() != null &&
		   te.getClassName().startsWith("edu.brown.cs.bubbles.bandaid")) {
	       k0 = k+1;
	     }
	  }
	 for (int k = k0; k < thread_stack[j].length; ++k) {
	    xw.begin("STACK");
	    xw.field("LEVEL",k);
	    StackTraceElement te = thread_stack[j][k];
	    xw.field("CLASS",te.getClassName());
	    xw.field("FILE",te.getFileName());
	    xw.field("LINE",te.getLineNumber());
	    xw.field("METHOD",te.getMethodName());
	    xw.end();
	  }
	 xw.end();
       }
      if (otct > 0) xw.end();
    }

}	// end of inner class ThreadData




/********************************************************************************/
/*										*/
/*	Stop data holder -- hold history when a thread stops			*/
/*										*/
/********************************************************************************/

private class StopData {

   List<ThreadData> thread_history;
   private long start_time;
   private long stop_time;

   StopData(long start,long now) {
      thread_history = new ArrayList<ThreadData>();
      start_time = start;
      stop_time = now;
      for (ThreadData td : thread_data.values()) {
	 ThreadData save = new ThreadData(start,td);
	 thread_history.add(save);
       }
    }

   void outputXml(BandaidXmlWriter xw) {
      xw.field("START",start_time);
      xw.field("STOP",stop_time);
      for (ThreadData td : thread_history) td.outputXml(xw);
    }

}	// end of inner class StopData




}	// end of class BandaidAgentHistory




/* end of BandaidAgentHistory.java */

