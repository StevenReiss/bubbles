/********************************************************************************/
/*										*/
/*		BudaShareManager.java						*/
/*										*/
/*	BUblles Display Area manager for shared working sets			*/
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



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



class BudaShareManager implements BudaConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private MintControl	mint_control;
private Map<String,Share> our_shares;
private Map<BudaBubbleArea,BubbleChecker> call_backs;
private Map<String,Integer> last_update;

private static String	host_id;
private static int	id_counter = 1;
private static int	serial_number = 0;


static {
   host_id = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaShareManager()
{
   our_shares = new HashMap<String,Share>();
   call_backs = new HashMap<BudaBubbleArea,BubbleChecker>();
   last_update = new HashMap<String,Integer>();

   mint_control = BoardSetup.getSetup().getMintControl();

   mint_control.register("<BUDA SOURCE='SHARE' CMD='_VAR_0' HOST='_VAR_1' />",new ShareHandler());
}



/********************************************************************************/
/*										*/
/*	Methods to list all active shares we can connect to			*/
/*										*/
/********************************************************************************/

List<BudaShare> getAllShares()
{
   String msg = "<BUDA SOURCE='SHARE' CMD='ALLSHARES' HOST='" + host_id + "' />";
   AllShares as = new AllShares();
   mint_control.send(msg,as,MINT_MSG_ALL_REPLIES);

   Collection<Share> shs = as.waitForReply(500);
   if (shs == null || shs.size() == 0) return null;

   return new ArrayList<BudaShare>(shs);
}



private class AllShares implements MintReply {

   private Map<String,Share> all_shares;
   private boolean is_done;

   AllShares() {
      all_shares = new HashMap<String,Share>();
      is_done = false;
    }

   @Override public synchronized void handleReply(MintMessage msg,MintMessage rply) {
      Element rx = rply.getXml();
      if (IvyXml.isElement(rx,"SHARES")) {
	 for (Element sx : IvyXml.children(rx,"SHARE")) {
	    String id = IvyXml.getAttrString(sx,"ID");
	    if (our_shares.containsKey(id)) continue;
	    Share s = new Share(sx);
	    all_shares.put(s.getId(),s);
	  }
       }
    }

   @Override public synchronized void handleReplyDone(MintMessage msg) {
      is_done = true;
      notifyAll();
    }

   synchronized Collection<Share> waitForReply(long delay) {
      long end = System.currentTimeMillis() + delay;

      while (!is_done) {
	 try {
	    wait(delay);
	  }
	 catch (InterruptedException e) { }
	 if (delay > 0 && !is_done) {
	    delay = end - System.currentTimeMillis();
	    if (delay <= 0) break;
	  }
       }

      return all_shares.values();
    }

}	// end of inner class AllShares



private void generateShares(BudaXmlWriter xw)
{
   synchronized (our_shares) {
      for (Share s : our_shares.values()) {
	 s.outputXml(xw,false);
      }
    }
}



/********************************************************************************/
/*										*/
/*	Methods for creating a shared working set				*/
/*										*/
/********************************************************************************/

void createShare(BudaWorkingSetImpl ws)
{
   Share sh = null;

   synchronized (our_shares) {
      for (Share s : our_shares.values()) {
	 if (s.getWorkingSet() == ws) return;
       }
      sh = new Share(ws,null);
      ws.setShared(true);
      our_shares.put(sh.getId(),sh);
    }

   synchronized (call_backs) {
      BudaBubbleArea bba = ws.getBubbleArea();
      BubbleChecker bc = call_backs.get(bba);
      if (bc == null) {
	 bc = new BubbleChecker();
	 bba.addBubbleAreaCallback(bc);
       }
    }
}



void removeShare(BudaWorkingSetImpl ws)
{
   for (Iterator<Share> it = our_shares.values().iterator(); it.hasNext(); ) {
      Share s = it.next();
      if (s.getWorkingSet() == ws) {
	 ws.setShared(false);
	 it.remove();
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods for using a shared workspace					*/
/*										*/
/********************************************************************************/

boolean useShare(BudaShare bs,BudaBubbleArea bba,int offset)
{
   String id = bs.getId();
   if (our_shares.containsKey(id)) return false;	// don't share with self

   MintDefaultReply mdr = new MintDefaultReply();
   String msg = "<BUDA SOURCE='SHARE' CMD='SHARE' HOST='" + host_id + "' ID='" + id + "' />";
   mint_control.send(msg,mdr,MINT_MSG_FIRST_NON_NULL);
   Element xml = mdr.waitForXml();
   if (xml == null) return false;
   Element task = IvyXml.getChild(xml,"TASK");

   BudaTask bt = new BudaTask(task);
   BudaWorkingSetImpl ws = bt.loadTask(bba,offset);
   Share s = new Share(ws,id);
   ws.setShared(true);
   our_shares.put(s.getId(),s);
   BudaRoot.findBudaRoot(bba).repaint();

   return true;
}



/********************************************************************************/
/*										*/
/*	Handler for share messages						*/
/*										*/
/********************************************************************************/

private final class ShareHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      String host = args.getArgument(1);
      if (host.equals(host_id)) {	  // ignore self messages
	 msg.replyTo();
	 return;
       }
      Element xml = msg.getXml();
      String id = IvyXml.getAttrString(xml,"ID");

      BoardLog.logD("BUDA","Received share message: " + cmd);

      String rply = null;

      try {
	 if (cmd == null) ;
	 else if (cmd.equals("ALLSHARES")) {
	    BudaXmlWriter xw = new BudaXmlWriter();
	    xw.begin("SHARES");
	    generateShares(xw);
	    xw.end("SHARES");
	    rply = xw.toString();
	  }
	 else if (cmd.equals("UNSHARE")) {
	    Share s = our_shares.get(id);
	    if (s != null) {
	       our_shares.remove(id);
	     }
	  }
	 else if (cmd.equals("UPDATE")) {
	    Share s = our_shares.get(id);
	    if (s != null) {
	       Updater upd = new Updater(s,xml);
	       BoardThreadPool.start(upd);
	     }
	  }
	 else if (cmd.equals("SHARE")) {
	    Share s = our_shares.get(id);
	    if (s != null) {
	       BudaXmlWriter xw = new BudaXmlWriter();
	       s.outputXml(xw,true);
	       rply = xw.toString();
	     }
	  }
	 else {
	    BoardLog.logD("BUDA","Unknown share message " + cmd);
	  }
       }
      catch (Throwable t) {
	 BoardLog.logE("BUDA","Problem processing share message " + cmd,t);
       }

      msg.replyTo(rply);
    }

}	// end of inner class ShareHandler




private class Updater implements Runnable {

   private Element update_msg;
   private Share for_share;

   Updater(Share s,Element xml) {
      for_share = s;
      update_msg = xml;
    }

   @Override public void run() {
      String host = IvyXml.getAttrString(update_msg,"HOST");
      int sno = IvyXml.getAttrInteger(update_msg,"SERIAL");
      Element share = IvyXml.getChild(update_msg,"SHARE");
      Element task = IvyXml.getChild(share,"TASK");
      synchronized (for_share) {
	 Integer iv = last_update.get(host);
	 if (iv != null && sno < iv) return;
	 last_update.put(host,sno);
	 for_share.setDoingUpdate(true);
	 BudaTask bt = new BudaTask(task);
	 // System.err.println("UPDATE: " + IvyXml.convertXmlToString(update_msg));
	 bt.updateTask(for_share.getWorkingSet());
	 for_share.setDoingUpdate(false);
	 for_share.memoize();
       }
    }

}	// end of inner class updater




/********************************************************************************/
/*										*/
/*	Handler for bubble updates						*/
/*										*/
/********************************************************************************/

private final class BubbleChecker implements BubbleAreaCallback, BubbleViewCallback {

   @Override public void moveDelta(int dx,int dy)		{ }
   
   @Override public void updateOverview() {
      checkStates();
    }

}	// end of inner class BubbleChecker



private void checkStates()
{
   for (Share s : our_shares.values()) {
      synchronized (s) {
	 if (s.memoize()) {
	    if (!s.isDoingUpdate()) sendUpdate(s);
	  }
       }
    }
}


private void sendUpdate(Share s)
{
   BudaXmlWriter xw = new BudaXmlWriter();
   xw.begin("BUDA");
   xw.field("SOURCE","SHARE");
   xw.field("CMD","UPDATE");
   xw.field("HOST",host_id);
   xw.field("ID",s.getId());
   xw.field("SERIAL",getSerialNumber());
   s.outputXml(xw,true);
   xw.end("BUDA");
   mint_control.send(xw.toString());
}


private static synchronized int getSerialNumber()
{
   return ++serial_number;
}

/********************************************************************************/
/*										*/
/*	Representation of a shared working set or task				*/
/*										*/
/********************************************************************************/

private static class Share implements BudaShare {

   private BudaWorkingSetImpl working_set;
   private String share_id;
   private String share_name;
   private String share_host;
   private String share_user;
   private StateMemo state_memo;
   private boolean doing_update;

   Share(BudaWorkingSetImpl ws,String id) {
      working_set = ws;
      share_id = null;
      share_host = host_id;
      share_name = null;
      share_user = System.getProperty("user.name");
      share_id = (id == null ? createId(this) : id);
      state_memo = null;
      memoize();
      doing_update = false;
    }

   Share(Element xml) {
      working_set = null;
      share_id = IvyXml.getAttrString(xml,"ID");
      share_name = IvyXml.getAttrString(xml,"NAME");
      share_user = IvyXml.getAttrString(xml,"USER");
      share_host = IvyXml.getAttrString(xml,"HOST");
      state_memo = null;
      doing_update = false;
    }


   BudaWorkingSetImpl getWorkingSet()			{ return working_set; }

   @Override public String getId()			{ return share_id; }
   @Override public String getUser()			{ return share_user; }
   @Override public String getHost()			{ return share_host; }

   @Override public String getName() {
      if (share_name != null) return share_name;
      return working_set.getLabel();
    }

   void setDoingUpdate(boolean fg)			{ doing_update = fg; }
   boolean isDoingUpdate()				{ return doing_update; }

   boolean memoize() {
      StateMemo sm = new StateMemo(this);
      if (sm.isSame(state_memo)) return false;
      state_memo = sm;
      return true;
    }

   void outputXml(BudaXmlWriter xw,boolean cnts) {
      xw.begin("SHARE");
      xw.field("ID",getId());
      xw.field("NAME",getName());
      xw.field("USER",getUser());
      xw.field("HOST",getHost());
      if (cnts && working_set != null) {
	 working_set.createTask(xw);
       }
      xw.end("SHARE");
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      if (getName() != null) buf.append(getName());
      else buf.append("Unnnamed Share");
      buf.append(" from " + getUser());
      String h = getHost();
      int idx = h.indexOf("@");
      h = h.substring(idx+1);
      buf.append(" on " + h);
      return buf.toString();
    }

   @Override public int compareTo(BudaShare s) {
      String s1 = toString();
      String s2 = s.toString();
      int c = s1.compareTo(s2);
      return c;
    }

}	// end of inner class Share




/********************************************************************************/
/*										*/
/*	Share utility methods							*/
/*										*/
/********************************************************************************/

private static synchronized String createId(Share s)
{
   String id = host_id + "_" + (id_counter++);

   return id;
}


/********************************************************************************/
/*										*/
/*	Representation of working set state					*/
/*										*/
/********************************************************************************/

private static class StateMemo {

   private String share_name;
   private Map<BudaBubble,Rectangle> bubble_location;

   StateMemo(Share s) {
      bubble_location = new HashMap<>();
      share_name = s.getName();
      BudaWorkingSet ws = s.getWorkingSet();
      BudaBubbleArea bba = ws.getBubbleArea();
      Rectangle r1 = ws.getRegion();
      for (BudaBubble bb : bba.getBubblesInRegion(r1)) {
	 Rectangle r2 = BudaRoot.findBudaLocation(bb);
	 bubble_location.put(bb,r2);
       }
    }

   boolean isSame(StateMemo sm) {
      if (sm == null) return false;
      if (share_name == null) {
	 if (sm.share_name != null) return false;
       }
      else if (!share_name.equals(sm.share_name)) return false;

      if (!bubble_location.equals(sm.bubble_location)) return false;

      return true;
    }


}	// end of inner class StateMemo


}	// end of class BudaShareManager




/* end of BudaShareManager.java */
