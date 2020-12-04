/********************************************************************************/
/*										*/
/*		BandaidAgentThreadState.java					*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data thread state agent		*/
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



import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.*;


class BandaidAgentThreadState extends BandaidAgent implements BandaidConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<Long,ThreadData>	thread_data;
private int			current_count;
private int			last_report;
private long			last_sample;

enum State {
   NONE,
   NEW,
   RUNNING,
   RUNNING_SYNC,
   RUNNING_IO,
   RUNNING_SYSTEM,
   BLOCKED,
   WAITING,
   TIMED_WAITING,
   IDLE,
   STOPPED,
   STOPPED_SYNC,
   STOPPED_IO,
   STOPPED_WAITING,
   STOPPED_IDLE,
   STOPPED_TIMED,
   STOPPED_SYSTEM,
   STOPPED_BLOCKED,
   DEAD
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidAgentThreadState(BandaidController bc)
{
   super(bc,"ThreadState");

   thread_data = new HashMap<Long,ThreadData>();
   current_count = 0;
   last_report = 0;
   last_sample = 0;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override void generateReport(BandaidXmlWriter xw,long now)
{
   xw.begin("STATES");
   for (Iterator<ThreadData> it = thread_data.values().iterator(); it.hasNext(); ) {
      ThreadData td = it.next();
      if (!td.output(xw)) {
	 it.remove();
       }
    }
   xw.end();

   last_report = current_count;
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   if (last_sample != now) {
      ++current_count;
      last_sample = now;
    }

   long tid = ti.getThreadId();
   ThreadData td = thread_data.get(tid);
   if (td == null) {
      td = new ThreadData(ti);
      thread_data.put(tid,td);
    }

   td.setState(ti,trc);
}




/********************************************************************************/
/*										*/
/*	Thread state Data holder						*/
/*										*/
/********************************************************************************/

private class ThreadData {

   private ThreadInfo thread_info;
   private State cur_state;
   private int last_change;
   private int last_update;

   ThreadData(ThreadInfo ti) {
      thread_info = ti;
      last_change = current_count;
      last_update = 0;
      cur_state = State.NONE;
    }

   void setState(ThreadInfo ti,StackTraceElement [] trc) {
      State st = State.NONE;
      switch (ti.getThreadState()) {
	 case NEW :
	    st = State.NEW;
	    break;
	 case RUNNABLE :
	    break;
	 case BLOCKED :
	    st = State.BLOCKED;
	    break;
	 case WAITING :
	    st = State.WAITING;
	    break;
	 case TIMED_WAITING :
	    st = State.TIMED_WAITING;
	    break;
	 case TERMINATED :
	    st = State.DEAD;
	    break;
       }

      if (ti.isInNative() || st == State.NONE) {
	 st = State.RUNNING_SYSTEM;
	 for (int j = 0; j < trc.length; ++j) {
	    StackTraceElement te = trc[j];
	    String nm = te.getClassName();
	    if (j == 0 && the_control.isIOClass(nm)) {
	       st = State.RUNNING_IO;
	       break;
	     }
	    if (!the_control.isSystemClass(nm)) {
	       st = State.RUNNING;
	       MonitorInfo [] mons = ti.getLockedMonitors();
	       if (mons != null && mons.length > 0) st = State.RUNNING_SYNC;
	       break;
	     }
	  }
       }
      if (st == State.WAITING || st == State.RUNNING) {
	 StackTraceElement te = trc[0];
	 String cnm = te.getClassName();
	 String mnm = te.getMethodName();
	 if (mnm.equals("park")) {
	    switch (cnm) {
	       case "java.util.concurrent.locks.LockSupport" :
	       case "jdk.internal.misc.Unsafe" :
		  st = State.IDLE;
		  break;
	     }
	  }

       }

      if (ti.isSuspended()) {
	 switch (st) {
	    case RUNNING :
	       st = State.STOPPED;
	       break;
	    case RUNNING_IO :
	       st = State.STOPPED_IO;
	       break;
	    case RUNNING_SYNC :
	       st = State.STOPPED_SYNC;
	       break;
	    case RUNNING_SYSTEM :
	       st = State.STOPPED_SYSTEM;
	       break;
	    case BLOCKED :
	       st = State.STOPPED_BLOCKED;
	       break;
	    case WAITING :
	       st = State.STOPPED_WAITING;
	       break;
	    case IDLE :
	       st = State.STOPPED_IDLE;
	       break;
	    case TIMED_WAITING :
	       st = State.STOPPED_TIMED;
	       break;
	    default:
	       break;
	 }
      }

      if (st != cur_state) {
	 cur_state = st;
	 last_change = current_count;
       }

      last_update = current_count;
    }

   boolean output(BandaidXmlWriter xw) {
      boolean rpt = (last_change > last_report);
      switch (cur_state) {
	 case RUNNING :
	 case RUNNING_SYNC :
	 case RUNNING_IO :
	 case RUNNING_SYSTEM :
	 case BLOCKED :
	 case WAITING :
	 case IDLE :
	 case TIMED_WAITING :
	    rpt = true;
	    break;
	 default:
	    break;
      }

      if (rpt) {
	 xw.begin("THREAD");
	 xw.field("ID",thread_info.getThreadId());
	 xw.field("NAME",thread_info.getThreadName());
	 xw.field("STATE",cur_state.toString());
	 xw.field("BLOCKCT",thread_info.getBlockedCount());
	 xw.field("BLOCKTM",thread_info.getBlockedTime());
	 xw.field("WAITCT",thread_info.getWaitedCount());
	 xw.field("WAITTM",thread_info.getWaitedTime());
	 xw.field("CPUTM",the_control.getThreadCpuTime(thread_info.getThreadId()));
	 xw.field("USERTM",the_control.getThreadUserTime(thread_info.getThreadId()));
	 xw.end();
       }

      if (current_count != last_update) return false;

      return true;
    }
}	// end of inner class ThreadData




}	// end of class BandaidAgentThreadState




/* end of BandaidAgentThreadState.java */
