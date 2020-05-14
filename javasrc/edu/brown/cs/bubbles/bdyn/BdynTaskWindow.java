/********************************************************************************/
/*										*/
/*		BdynTaskWindow.java						*/
/*										*/
/*	Window to hold transaction-task-thread visualization			*/
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

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingRangeScrollBar;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


class BdynTaskWindow extends JPanel implements BdynConstants, AdjustmentListener,
	BdynConstants.BdynEventUpdater
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpProcess for_process;
private BdynTaskPanel task_panel;
private SwingRangeScrollBar time_bar;
private long min_time;
private long max_time;
private BdynEventTrace event_trace;
private List<BdynEntryThread> active_threads;
private RunHandler process_handler;

private static final int MAX_SCROLL_TIME = 1000;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdynTaskWindow()
{
   super(new BorderLayout());
   task_panel = new BdynTaskPanel(this);
   time_bar = new SwingRangeScrollBar(Adjustable.HORIZONTAL,0,MAX_SCROLL_TIME,0,MAX_SCROLL_TIME);
   time_bar.addAdjustmentListener(this);
   min_time = -1;
   max_time = 0;
   event_trace = null;
   process_handler = new RunHandler();
   active_threads = new ArrayList<BdynEntryThread>();

   BumpClient.getBump().getRunModel().addRunEventHandler(process_handler);

   add(task_panel,BorderLayout.CENTER);
   add(time_bar,BorderLayout.SOUTH);
}



void dispose()
{
   BumpClient.getBump().getRunModel().removeRunEventHandler(process_handler);
   event_trace = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public BudaBubble getBubble()
{
   return new TaskBubble(this);
}



public void setProcess(BumpProcess bp)
{
   BdynFactory bf = BdynFactory.getFactory();
   BdynProcess dp = bf.getBdynProcess(bp);
   BdynEventTrace et = dp.getEventTrace();
   setEventTrace(et);
}



void setEventTrace(BdynEventTrace et)
{
   if (event_trace == et) return;

   if (event_trace != null) {
      event_trace.removeUpdateListener(this);
    }

   event_trace = et;
   if (et != null) {
      if (task_panel != null) task_panel.clearCallbacks();
      active_threads = new ArrayList<BdynEntryThread>();
      for_process = et.getProcess();
      event_trace.addUpdateListener(this);
      min_time = event_trace.getStartTime();
      max_time = event_trace.getEndTime();
      if (max_time == min_time) {
	 min_time = -1;
	 max_time = 0;
       }
      else {
	 time_bar.setValues(0,MAX_SCROLL_TIME,0,MAX_SCROLL_TIME);
       }
      if (et.getActiveThreadCount() != active_threads.size()) {
	 active_threads = et.getActiveThreads();
       }
    }
   else {
      min_time = -1;
      max_time = 0;
    }

   repaint();
}



List<BdynEntryThread>	getThreads()		{ return active_threads; }

BdynEventTrace getEventTrace()			{ return event_trace; }




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void eventsAdded()
{
   BdynEventTrace evt = event_trace;

   if (evt == null || active_threads == null) return;
   long mxt = evt.getEndTime();
   if (mxt == 0) return;

   if (min_time < 0) {
      min_time = evt.getStartTime();
    }

   if (mxt > max_time) {
      if (evt.getActiveThreadCount() != active_threads.size()) {
	 active_threads = evt.getActiveThreads();
       }
      long omax = max_time;
      max_time = mxt;
      
      if (omax > 0) {
	 int lv = time_bar.getLeftValue();
	 int rv = time_bar.getRightValue();
	 double nmxv = (max_time - min_time);
	 double t0 = lv * omax / MAX_SCROLL_TIME + min_time;
	 double t1 = rv * omax / MAX_SCROLL_TIME + min_time;
	 if (rv == MAX_SCROLL_TIME) t1 = max_time;
	 int it0 = (int)Math.round((t0 - min_time)/nmxv * MAX_SCROLL_TIME);
	 int it1 = (int)Math.round((t1 - min_time)/nmxv * MAX_SCROLL_TIME);
	 time_bar.setValues(it0,it1);
	 task_panel.setTimes((long) t0,(long) t1);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Handle scroll bar							*/
/*										*/
/********************************************************************************/

@Override public void adjustmentValueChanged(AdjustmentEvent ev) {
   long mint = (long)(min_time + time_bar.getLeftValue() * (max_time-min_time)/MAX_SCROLL_TIME + 0.5);
   long maxt = (long)(min_time + time_bar.getRightValue() * (max_time-min_time)/MAX_SCROLL_TIME + 0.5);
   task_panel.setTimes(mint,maxt);
}




/********************************************************************************/
/*										*/
/*	Run event handler							*/
/*										*/
/********************************************************************************/

private class RunHandler implements BumpConstants.BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt)		{ }
   @Override public void handleThreadEvent(BumpRunEvent evt)		{ }
   @Override public void handleConsoleMessage(BumpProcess bp,boolean e,boolean f,String msg) { }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      BumpProcess blp;
      switch (evt.getEventType()) {
         case PROCESS_CHANGE :
         case PROCESS_ADD :
            blp = evt.getProcess();
            if (for_process == null) {
               BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BdynTaskWindow.this);
               if (bba != null) {
        	  Object proc = bba.getProperty("Bddt.process");
        	  if (proc != null && proc == blp) {
        	     // process for this channel
        	     setProcess(blp);
        	     break;
        	   }
        	  if (proc == null && bba.getProperty("Bddt.debug") == null) {
        	     // non-debug -- use next process
        	     setProcess(blp);
        	   }
        	}
             }
            break;
         case PROCESS_REMOVE :
            blp = evt.getProcess();
            if (for_process == blp) for_process = null;
            break;
         default :
            break;
       }
    }

}	// end of inner class RunHandler




/********************************************************************************/
/*										*/
/*	Bubble for this window							*/
/*										*/
/********************************************************************************/

private class TaskBubble extends BudaBubble {

   BdynTaskWindow task_window;

   TaskBubble(BdynTaskWindow w) {
      task_window = w;
      setContentPane(w,null);
    }

   @Override protected void localDispose() {
       task_window.dispose();
    }

   @Override public void handlePopupMenu(MouseEvent e) {
      Point pt = SwingUtilities.convertPoint(this,e.getPoint(),task_panel);
      JPopupMenu menu = new JPopupMenu();
      task_panel.handleContextMenu(menu,pt,e);
      menu.add(getFloatBubbleAction());
      menu.show(this,e.getX(),e.getY());
    }

}	// end of inner class TaskBubble


}	// end of class BdynTaskWindow




/* end of BdynTaskWindow.java */
