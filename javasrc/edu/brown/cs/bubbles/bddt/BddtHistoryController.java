/********************************************************************************/
/*										*/
/*		BddtHistoryController.java					*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool history recording		*/
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



package edu.brown.cs.bubbles.bddt;


import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingEventListenerList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;


class BddtHistoryController implements BddtConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BddtLaunchControl,HistoryData>	history_data;
private Map<String,HistoryData> 		process_data;



/********************************************************************************/
/*										*/
/*	Constructor								*/
/*										*/
/********************************************************************************/

BddtHistoryController()
{
   history_data = new HashMap<BddtLaunchControl,HistoryData>();
   process_data = new HashMap<String,HistoryData>();

   BumpClient bc = BumpClient.getBump();
   BumpRunModel rm = bc.getRunModel();

   rm.addRunEventHandler(new HistoryHandler());
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setupHistory(BddtLaunchControl blc)
{
   getHistory(blc);
}




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

BddtHistoryBubble createHistory(BddtLaunchControl ctrl)
{
   BddtHistoryData bd = getHistory(ctrl);

   BddtHistoryBubble bb = new BddtHistoryBubble(ctrl,bd);

   return bb;
}



/********************************************************************************/
/*										*/
/*	Methods to access history information					*/
/*										*/
/********************************************************************************/

private HistoryData getHistory(BddtLaunchControl ctrl)
{
   synchronized (history_data) {
      HistoryData hd = history_data.get(ctrl);
      if (hd == null) {
	 BumpProcess bp = ctrl.getProcess();
	 if (bp != null) hd = getHistory(bp.getId(),false);
	 if (hd == null) hd = new HistoryData();
	 history_data.put(ctrl,hd);
       }
      return hd;
    }
}




private HistoryData getHistory(String pid,boolean force)
{
   synchronized (history_data) {
      HistoryData hd = process_data.get(pid);
      if (hd != null) return hd;
      for (Map.Entry<BddtLaunchControl,HistoryData> ent : history_data.entrySet()) {
	 BddtLaunchControl blc = ent.getKey();
	 BumpProcess bp = blc.getProcess();
	 if (bp != null && bp.getId().equals(pid)) {
	    hd = ent.getValue();
	    process_data.put(pid,hd);
	    return hd;
	  }
       }
      if (force) {
	 hd = new HistoryData();
	 process_data.put(pid,hd);
       }
      return hd;
    }
}



void clearHistory(BumpProcess bp)
{
   if (bp == null) return;

   HistoryData hd = getHistory(bp.getId(),false);
   if (hd != null) {
      hd.clear();
    }
}



/********************************************************************************/
/*										*/
/*	Model event handling							*/
/*										*/
/********************************************************************************/

private class HistoryHandler implements BumpConstants.BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt)	{ }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      switch (evt.getEventType()) {
	 case PROCESS_REMOVE :
	    BumpProcess bp = evt.getProcess();
	    synchronized (history_data) {
	       process_data.remove(bp.getId());
	     }
	    break;
	 default:
	    break;
       }
    }

   @Override public void handleThreadEvent(BumpRunEvent evt) {
      if (evt.getProcess() == null) return;
      String pid = evt.getProcess().getId();
      if (pid == null) return;

      HistoryData hd = getHistory(pid,true);

      switch (evt.getEventType()) {
	 case THREAD_ADD :
	    hd.startThread(evt.getThread(),evt.getWhen());
	    break;
	 case THREAD_REMOVE :
	    hd.endThread(evt.getThread(),evt.getWhen());
	    break;
	 case THREAD_CHANGE :
	    hd.add(evt.getThread(),evt.getWhen());
	    break;
	 case THREAD_TRACE :
	    hd.add(evt.getThread(),evt.getWhen());
	    break;
	 case THREAD_HISTORY :
	    break;
	 default:
	    break;
       }
    }

   @Override public void handleConsoleMessage(BumpProcess bp,boolean err,boolean eof,String msg) { }

}	// end of inner class HistoryHandler




/********************************************************************************/
/*										*/
/*	History Data structure							*/
/*										*/
/********************************************************************************/

private static class HistoryData implements BddtHistoryData {

   private ConcurrentMap<BumpThread,HistoryThread> thread_map;
   private SwingEventListenerList<BddtHistoryListener> listener_list;

   HistoryData() {
      thread_map = new ConcurrentHashMap<BumpThread,HistoryThread>();
      listener_list = new SwingEventListenerList<BddtHistoryListener>(BddtHistoryListener.class);
    }

   HistoryThread startThread(BumpThread th,long when) {
      HistoryThread ht = new HistoryThread(th,when);
      HistoryThread xht = thread_map.putIfAbsent(th,ht);
      if (xht != null) ht = xht;
      return ht;
    }

   void add(BumpThread th,long when) {
      HistoryThread ht = startThread(th,when);
      BumpThreadStack stk = th.getStack();
      if (stk == null || stk.getNumFrames() == 0) return;
      HistoryItem hi = new HistoryItem(stk,when);
      BoardLog.logD("BDDT","Add " + hi);
      ht.add(hi);
      historyUpdated();
    }

   void endThread(BumpThread th,long when) {
      HistoryThread ht = thread_map.get(th);
      if (ht != null) ht.endAt(when);
      // historyUpdated();
    }

   void clear() {
      thread_map.clear();
      historyStarted();
    }

   @Override public Iterable<BumpThread> getThreads() {
      return thread_map.keySet();
    }

   @Override public Iterable<BddtHistoryItem> getItems(BumpThread bt) {
      HistoryThread ht = thread_map.get(bt);
      if (ht == null) return Collections.emptyList();
      return ht.getItems();
    }

   @Override public void addHistoryListener(BddtHistoryListener bl) {
      listener_list.add(bl);
    }

   @Override public void removeHistoryListener(BddtHistoryListener bl) {
      listener_list.remove(bl);
    }

   private void historyStarted() {
      for (BddtHistoryListener bl : listener_list) {
         bl.handleHistoryStarted();
       }
    }

   private void historyUpdated() {
      for (BddtHistoryListener bl : listener_list) {
	 bl.handleHistoryUpdated();
       }
    }

}	// end of inner class HistoryData



private static class HistoryThread {

   private Queue<BddtHistoryItem> thread_items;

   HistoryThread(BumpThread th,long when) {
      thread_items = new ConcurrentLinkedQueue<BddtHistoryItem>();
    }

   void endAt(long when)			{ }

   void add(HistoryItem hi) {
      thread_items.add(hi);
    }

   Iterable<BddtHistoryItem> getItems() 	{ return thread_items; }

}	// end of inner class HistoryThread





/********************************************************************************/
/*										*/
/*	Single history datum							*/
/*										*/
/********************************************************************************/

private static class HistoryItem implements BddtHistoryItem {

   private BumpThreadStack for_stack;
   private long at_time;

   HistoryItem(BumpThreadStack stk,long at) {
      for_stack = stk;
      BumpStackFrame fm0 = for_stack.getFrame(0);
      if (!fm0.isStatic()) fm0.getValue("this");        // ensure defined
      at_time = at;
    }

   @Override public BumpThread getThread()	{ return for_stack.getThread(); }
   @Override public long getTime()		{ return at_time; }
   @Override public BumpThreadStack getStack()	{ return for_stack; }

   @Override public BumpRunValue getThisValue() {
      BumpStackFrame fm0 = for_stack.getFrame(0);
      if (!fm0.isStatic()) return fm0.getValue("this");
      return null;
    }

   @Override public String getClassName() {
      BumpStackFrame fm0 = for_stack.getFrame(0);
      return fm0.getFrameClass();
    }

   @Override public boolean isInside(BddtHistoryItem prior) {
      int delta = for_stack.getNumFrames() - prior.getStack().getNumFrames();
      if (delta <= 0) return false;
      for (int i = 0; i < prior.getStack().getNumFrames(); ++i) {
         BumpStackFrame pfi = prior.getStack().getFrame(i);
         BumpStackFrame fi = for_stack.getFrame(i-delta);
         if (fi == null) return false;
         if (!fi.getFrameClass().equals(pfi.getFrameClass()) ||
        	!fi.getMethod().equals(pfi.getMethod()))
            return false;
       }
      return true;
    }

   @Override public String toString() {
      return "HISTORY[" + getThread().getName() + " " + getClassName() + " " + getTime() + "]";
    }

}	// end of inner class HistoryItem




}	// end of class BddtHistoryController




/* end of BddtHistoryController.java */
