/********************************************************************************/
/*										*/
/*		BgtaBuddyRepository.java					*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/




package edu.brown.cs.bubbles.bgta;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bgta.BgtaConstants.BgtaRoster;
import edu.brown.cs.bubbles.bgta.BgtaConstants.BgtaRosterEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;



class BgtaBuddyRepository implements BassConstants, BassRepository {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<BgtaBuddy>	all_names;
private boolean 	is_ready;
private BgtaManager	the_manager;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaBuddyRepository(BgtaManager man)
{
   all_names = new HashSet<BgtaBuddy>();
   is_ready = false;
   the_manager = man;
   initialize();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BgtaManager getManager()
{
   return the_manager;
}



/********************************************************************************/
/*										*/
/*	BassRepository methods							*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   synchronized (this) {
      while (!is_ready) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) {}
       }
      return new ArrayList<BassName>(all_names);
    }
}


@Override public boolean isEmpty()
{
   synchronized (this) {
      while (!is_ready) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) {}
       }
      return all_names.isEmpty();
    }
}



/********************************************************************************/
/*										*/
/*	Buddy Identification methods						*/
/*										*/
/********************************************************************************/

BgtaBuddy getBuddyInfo(String buddyname)
{
   for (BgtaBuddy bud : all_names) {
      if (bud.getConnectionName().equals(buddyname)) return bud;
    }
   return null;
}


/*******************
private boolean isEquivalent(BgtaBuddyRepository bbr)
{
   synchronized (all_names) {
      Iterable<BassName> theirnames = bbr.getAllNames();
      for (BassName bn : theirnames) {
	 if (!all_names.contains(bn)) return false;
       }
      return true;
    }
}
***********************/


/********************************************************************************/
/*										*/
/*	Name loader								*/
/*										*/
/********************************************************************************/

private void loadNames()
{
   BgtaRoster buddylist = the_manager.getRoster();
   Collection<? extends BgtaRosterEntry> entries = buddylist.getEntries();
   for (BgtaRosterEntry r : entries) {
      BgtaBuddy bud = new BgtaBuddy(r.getUser(),the_manager,the_manager.hasBubble(r.getUser()));
      all_names.add(bud);
    }

   synchronized (this) {
      is_ready = true;
      notifyAll();
    }
}



/********************************************************************************/
/*										*/
/*	Initializer								*/
/*										*/
/********************************************************************************/

private void initialize()
{
   synchronized (this) {
      all_names.clear();
      is_ready = false;
    }

   Searcher s = new Searcher();
   s.start();
}




private class Searcher extends Thread {

   Searcher() {
      super("BgtaSearcher");
    }

   @Override public void run() {
      loadNames();
    }

}	// end of inner class Searcher



}	// end of class BgtaBuddyRepository



/* end of BgtaBuddyRepository.java */
