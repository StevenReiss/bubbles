/********************************************************************************/
/*										*/
/*		BrepairSeedeManager.java					*/
/*										*/
/*	Handle setting up controlled execution for bug repair			*/
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



package edu.brown.cs.bubbles.brepair;

import java.io.File;
import java.util.List;
import java.util.Set;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.batt.BattFactory;
import edu.brown.cs.bubbles.batt.BattConstants.BattTest;
import edu.brown.cs.bubbles.bicex.BicexConstants;
import edu.brown.cs.bubbles.bicex.BicexException;
import edu.brown.cs.bubbles.bicex.BicexFactory;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpBreakMode;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpBreakpoint;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpLaunchConfig;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProcess;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpStackFrame;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThread;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStack;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadState;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStateDetail;

class BrepairSeedeManager implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BattTest	test_case;
private BumpLaunchConfig launch_config;
private Boolean 	is_running;
private BumpBreakpoint	break_point;
private BumpProcess	debug_process;
private BicexRunner	seede_runner;
private SeedeListener	seede_listener;
private BicexResult	seede_result;
private Set<File>	using_files;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BrepairSeedeManager(BattTest bt,Set<File> files)
{
   test_case = bt;
   launch_config = null;
   break_point = null;
   debug_process = null;
   seede_runner = null;
   seede_listener = null;
   seede_result = null;
   using_files = files;
   is_running = null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BicexResult getSeedeResult()
{
   if (is_running == null) {
      setup();
    }

   return waitForSetup();
}


BicexRunner getSeedeRunner()
{
   if (is_running == null) setup();

   synchronized (this) {
      while (seede_runner == null) {
	 try {
	    wait(10000);
	  }
	 catch (InterruptedException e) { }
	 if (is_running == Boolean.FALSE) break;
       }
      return seede_runner;
    }
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

synchronized void setup()
{
   seede_result = null;
   is_running = true;
   SetupRunner sr = new SetupRunner();
   BoardThreadPool.start(sr);
}


void remove()
{
   BumpClient bc = BumpClient.getBump();

   removeBreakpoint();

   if (debug_process != null) {
      bc.terminate(debug_process.getLaunch());
      debug_process = null;
    }

   if (seede_listener != null) {
      if (seede_runner != null) seede_runner.removeUpdateListener(seede_listener);
      seede_listener = null;
    }

   if (seede_runner != null) {
      seede_runner.remove();
      seede_runner = null;
    }

   synchronized (this) {
      seede_result = null;
      is_running = false;
      notifyAll();
    }
}




private synchronized BicexResult waitForSetup()
{
   while (is_running == Boolean.TRUE) {
      try {
	 wait();
       }
      catch (InterruptedException e) { }
    }

   return seede_result;
}



private class SetupRunner implements Runnable {

   @Override public void run() {
      setupLaunch();
    }
}



/********************************************************************************/
/*										*/
/*	Methods to setup test case launch					*/
/*										*/
/********************************************************************************/

private void setupLaunch()
{
   launch_config = BattFactory.getLaunchConfigurationForTest(test_case);

   createBreakpoint();

   createLaunch();

   waitForBreak();

   removeBreakpoint();

   startSeede();
}




private void createBreakpoint()
{
   String cls = test_case.getClassName();
   String mthd = test_case.getMethodName();
   BumpClient bc = BumpClient.getBump();
   List<BumpLocation> locs = bc.findMethod(null,cls + "." + mthd,false);
   if (locs == null || locs.isEmpty()) return;
   BumpLocation loc = locs.get(0);
   BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(null,loc.getFile());
   int lno = bfo.findLineNumber(loc.getDefinitionOffset());
   List<BumpBreakpoint> origbpts = bc.getAllBreakpoints();
   bc.getBreakModel().addLineBreakpoint(null,loc.getFile(),null,lno,BumpBreakMode.SUSPEND_THREAD);
   List<BumpBreakpoint> newbpts = bc.getAllBreakpoints();
   newbpts.removeAll(origbpts);
   if (newbpts.size() == 1) {
      break_point = newbpts.get(0);
    }
}


private void createLaunch()
{
   BumpClient bc = BumpClient.getBump();
   debug_process = bc.startDebug(launch_config,null);
}


private void waitForBreak()
{
   BumpClient bc = BumpClient.getBump();

   for ( ; ; ) {
      boolean ready = false;
      boolean stopped = false;
      for (BumpThread bt : debug_process.getThreads()) {
	 if (bt.getThreadState() == BumpThreadState.STOPPED &&
	       bt.getThreadDetails() == BumpThreadStateDetail.BREAKPOINT) {
	    stopped = true;
	    BumpThreadStack stk = bt.getStack();
	    BumpStackFrame frm = stk.getFrame(0);
	    String nm = test_case.getClassName() + "." + test_case.getMethodName();
	    if (frm.getMethod().equals(nm)) {
	       ready = true;
	       break;
	     }
	  }
       }
      if (ready) break;
      else if (stopped) {
	 bc.resume(debug_process);
       }

      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }
}



private void removeBreakpoint()
{
   if (break_point != null) {
      BumpClient bc = BumpClient.getBump();
      bc.getBreakModel().removeBreakpoint(break_point.getBreakId());
      break_point = null;
    }
}


private void startSeede()
{
   synchronized (this) {
      seede_runner = BicexFactory.getFactory().runSeedeOnProcess(debug_process);
      notifyAll();
    }

   if (seede_runner == null) return;
   
   seede_listener = new SeedeListener();
   seede_runner.addUpdateListener(seede_listener);
   seede_result = null;

   try {
      seede_runner.addFiles(using_files);
      seede_runner.startExecution();
      seede_listener.waitForComplete();
      seede_result = seede_runner.getEvaluation();
    }
   catch (BicexException e) {
    }

   synchronized (this) {
      is_running = false;
      notifyAll();
    }
}




private class SeedeListener implements BicexEvaluationUpdated {

   private boolean is_complete;

   SeedeListener() {
      is_complete = false;
    }

   synchronized void waitForComplete() {
      while (!is_complete) {
         try {
            wait(10000);
          }
         catch (InterruptedException e) { }
       }
    }

   @Override public void evaluationUpdated(BicexRunner bex) {
      synchronized (this) {
         is_complete = true;
         notifyAll();
       }
    }

   @Override public void contextUpdated(BicexRunner bex) {
    }

   @Override public void timeUpdated(BicexRunner bex) {
    }

   @Override public void evaluationReset(BicexRunner bex) {
      synchronized (this) {
         is_complete = false;
       }
    }

   @Override public void editorAdded(BudaBubble bw) { }

   @Override public String inputRequest(BicexRunner bex,String file) {
      return null;
    }

   @Override public String valueRequest(BicexRunner bex,String var) {
      return null;
    }

}	// end of inner class SeedeListener

}	// end of class BrepairSeedeManager




/* end of BrepairSeedeManager.java */

