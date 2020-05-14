/********************************************************************************/
/*										*/
/*		BandaidAgentDeadlock.java					*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data deadlock checking		*/
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



import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;
import java.util.*;


class BandaidAgentDeadlock extends BandaidAgent implements BandaidConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<LockNode,ThreadInfo>	thread_locks;
private Map<ThreadInfo,LockNode>	thread_blocks;
private List<Deadlock>			dead_locks;
private Map<Long,Deadlock>		locked_threads;
private int				check_counter;

private static int			check_every = 10;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidAgentDeadlock(BandaidController bc)
{
   super(bc,"Deadlock");

   thread_locks = new HashMap<LockNode,ThreadInfo>();
   thread_blocks = new HashMap<ThreadInfo,LockNode>();
   dead_locks = new ArrayList<Deadlock>();
   locked_threads = new HashMap<Long,Deadlock>();
   check_counter = 0;
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   // should use ThreadMXBean.findDeadlockedThreads()

   if (check_counter % check_every != 0) return;

   LockInfo [] lis = ti.getLockedSynchronizers();
   for (LockInfo ls : lis) {
      LockNode ln = new LockNode(ls);
      thread_locks.put(ln,ti);
    }

   switch (ti.getThreadState()) {
      case BLOCKED :
	 LockInfo ll = ti.getLockInfo();
	 if (ll != null) {
	    LockNode ln = new LockNode(ll);
	    thread_blocks.put(ti,ln);
	  }
	 break;
      default:
	 break;
    }
}



@Override void handleDoneStacks(long now)
{
   if (check_counter++ % check_every != 0) return;

   analyzeLocks();

   thread_locks.clear();
   thread_blocks.clear();
}



/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

private void analyzeLocks()
{
   Set<ThreadInfo> done = new HashSet<ThreadInfo>();

   for (Map.Entry<ThreadInfo,LockNode> ent : thread_blocks.entrySet()) {
      ThreadInfo ti = ent.getKey();
      if (done.contains(ti)) continue;
      done.add(ti);
      if (locked_threads.containsKey(ti.getThreadId())) continue;

      Set<ThreadInfo> cur = new LinkedHashSet<ThreadInfo>();
      cur.add(ti);
      LockNode ln = ent.getValue();
      while (ln != null) {
	 ThreadInfo nti = thread_locks.get(ln);
	 if (nti == null) break;
	 if (cur.contains(nti)) {
	    Deadlock dl = new Deadlock(cur);
	    dead_locks.add(dl);
	    for (ThreadInfo xi : cur) {
	       locked_threads.put(xi.getThreadId(),dl);
	     }
	  }
	 else {
	    done.add(nti);
	    cur.add(nti);
	    ln = thread_blocks.get(nti);
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override void generateReport(BandaidXmlWriter xw,long now)
{
   if (dead_locks.size() == 0) return;

   xw.begin("DEADLOCKS");
   for (Deadlock dl : dead_locks) {
      dl.outputXml(xw);
    }
   xw.end();
}



/********************************************************************************/
/*										*/
/*	Lock graph								*/
/*										*/
/********************************************************************************/

private static class LockNode {

   LockInfo for_lock;

   LockNode(LockInfo li) {
      for_lock = li;
    }

   @Override public int hashCode()		{ return for_lock.getIdentityHashCode(); }
   @Override public boolean equals(Object o) {
      if (o instanceof LockNode) {
	 LockNode ln = (LockNode) o;
	 return ln.for_lock.getIdentityHashCode() == for_lock.getIdentityHashCode();
       }
      return false;
    }

}	// end of inner class LockNode



private static class Deadlock {

   private Map<ThreadInfo,String> lock_set;

   Deadlock(Set<ThreadInfo> cur) {
      lock_set = new LinkedHashMap<ThreadInfo,String>();
      for (ThreadInfo ti : cur) {
	 lock_set.put(ti,ti.getLockName());
       }
    }

   void outputXml(BandaidXmlWriter xw) {
      xw.begin("DEADLOCK");
      for (Map.Entry<ThreadInfo,String> ent : lock_set.entrySet()) {
	 xw.begin("THREAD");
	 xw.field("NAME",ent.getKey().getThreadName());
	 xw.field("ID",ent.getKey().getThreadId());
	 xw.field("LOCK",ent.getValue());
	 xw.end();
       }
      xw.end();
    }

}	// end of inner class Deadlock



}	// end of class BandaidAgentDeadlock




/* end of BandaidAgentDeadlock.java */

