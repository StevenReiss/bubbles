/********************************************************************************/
/*										*/
/*		BicexExecModel.java						*/
/*										*/
/*	Model to track current executions					*/
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



package edu.brown.cs.bubbles.bicex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProcess;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunEvent;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpStackFrame;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThread;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStack;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadState;

class BicexExecModel implements BicexConstants, BumpConstants.BumpRunEventHandler
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BicexDebug>		current_debuggers;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexExecModel()
{
   current_debuggers = new HashMap<>();
   BumpClient.getBump().getRunModel().addRunEventHandler(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<BumpProcess> getActiveProcesses()
{
   List<BumpProcess> rslt = new ArrayList<>();

   synchronized (current_debuggers) {
      for (BicexDebug bd : current_debuggers.values()) {
	 BumpProcess bp = bd.getProcess();
	 if (bp.isRunning() && bd.isStopped()) {
	    rslt.add(bp);
	  }
       }
    }

   return rslt;
}



List<BumpStackFrame> getActiveFrames(BumpProcess bp)
{
   BicexDebug dbg = current_debuggers.get(bp.getId());
   if (dbg == null) return null;
   List<BumpStackFrame> frms = dbg.getStops();
   if (frms.isEmpty()) return null;
   return frms;
}




/********************************************************************************/
/*										*/
/*	Handle run model callbacks						*/
/*										*/
/********************************************************************************/

@Override public void handleLaunchEvent(BumpRunEvent evt)	{ }


@Override public void handleProcessEvent(BumpRunEvent evt)
{
   switch (evt.getEventType()) {
      case PROCESS_ADD :
	 findProcess(evt.getProcess());
	 break;
      case PROCESS_REMOVE :
	 synchronized (current_debuggers) {
	    current_debuggers.remove(evt.getProcess().getId());
	  }
	 break;
      case PROCESS_CHANGE :
	 break;
      default :
	 break;
    }
}


@Override public void handleThreadEvent(BumpRunEvent evt)
{
   BicexDebug bd = findProcess(evt.getProcess());
   if (bd == null) return;

   BumpThread bt = evt.getThread();

   switch (evt.getEventType()) {
      case THREAD_REMOVE :
	 bd.removeThread(bt);
	 break;
      case THREAD_ADD :
	 bd.addThread(bt);
	 break;
      case THREAD_CHANGE :
	 bd.setThreadState(bt);
	 break;
      default :
	 break;
    }
}


@Override public void handleConsoleMessage(BumpProcess p,boolean e,boolean f,String msg)
{ }




/********************************************************************************/
/*										*/
/*	Process maintenance routines						*/
/*										*/
/********************************************************************************/

private BicexDebug findProcess(BumpProcess bp)
{
   if (bp == null) return null;

   synchronized (current_debuggers) {
      BicexDebug bd = current_debuggers.get(bp.getId());
      if (bd == null) {
	 bd = new BicexDebug(bp);
	 current_debuggers.put(bd.getId(),bd);
       }
      return bd;
    }
}



/********************************************************************************/
/*										*/
/*	BicexDebug -- information about a debugger run				*/
/*										*/
/********************************************************************************/

private static class BicexDebug {

   private Set<BumpThread>  stopped_threads;
   private BumpProcess	    bump_process;

   BicexDebug(BumpProcess bp) {
      stopped_threads = new ConcurrentSkipListSet<BumpThread>(new ThreadComparator());
      bump_process = bp;
    }

   String getId()			{ return bump_process.getId(); }
   BumpProcess getProcess()		{ return bump_process; }

   boolean isStopped() {
      return stopped_threads.size() > 0;
    }

   void addThread(BumpThread bt)	{ }

   void removeThread(BumpThread bt) {
      stopped_threads.remove(bt);
    }

   List<BumpStackFrame> getStops() {
      List<BumpStackFrame> rslt = new ArrayList<BumpStackFrame>();
      for (BumpThread bt : stopped_threads) {
	 BumpThreadStack bstk = bt.getStack();
	 if (bstk == null) continue;
	 BumpStackFrame frm = bstk.getFrame(0);
	 if (frm == null) continue;
	 rslt.add(frm);
       }
      return rslt;
    }

   void setThreadState(BumpThread bt) {
      BumpThreadState bst = bt.getThreadState();
      switch (bst) {
         case BLOCKED :
         case DEAD :
         case DEADLOCKED :
         case NEW :
         case NONE :
         case RUNNING :
         case RUNNING_IO :
         case RUNNING_SYNC :
         case RUNNING_SYSTEM :
         case UNKNOWN :
         case WAITING :
         case IDLE :
         case TIMED_WAITING :
            stopped_threads.remove(bt);
            break;
         case EXCEPTION :
         case STOPPED :
         case STOPPED_BLOCKED :
         case STOPPED_DEADLOCK :
         case STOPPED_IO :
         case STOPPED_SYNC :
         case STOPPED_SYSTEM :
         case STOPPED_TIMED :
         case STOPPED_WAITING :
         case STOPPED_IDLE :
            stopped_threads.add(bt);
            break;
       }
    }

}	// end of inner class BicexDebug



private static class ThreadComparator implements Comparator<BumpThread> {

   @Override public int compare(BumpThread bt1,BumpThread bt2) {
      return bt1.getId().compareTo(bt2.getId());
   }

}	// end of inner class ThreadComparator




}	// end of class BicexExecModel




/* end of BicexExecModel.java */

