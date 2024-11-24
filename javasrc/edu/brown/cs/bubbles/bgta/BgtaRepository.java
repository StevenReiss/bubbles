/********************************************************************************/
/*                                                                              */
/*              BgtaRepository.java                                             */
/*                                                                              */
/*      Bubbles attribute and property management main setup routine            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2009 Brown University -- Ian Strickman                      */
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

import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassRepositoryMerge;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;



class BgtaRepository implements BassRepository {



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BassRepository        complete_rep;
private BassRepository        original_rep;
private Vector<BgtaBuddyRepository> buddy_reps;
private Vector<BgtaManager>   manager_list;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BgtaRepository(Vector<BgtaManager> mans)
{
   manager_list = mans;
   buddy_reps = new Vector<BgtaBuddyRepository>();
   complete_rep = new BgtaLoginInfoRepository(manager_list,this);
   original_rep = complete_rep;
   for (BgtaManager man : manager_list) {
      addNewRep(new BgtaBuddyRepository(man));
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void addNewRep(BgtaBuddyRepository bbr)
{
   buddy_reps.add(bbr);
   complete_rep = new BassRepositoryMerge(bbr,complete_rep);
   BassFactory.reloadRepository(this);
}



/********************************************************************************/
/*                                                                              */
/*      BassRepository methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   return complete_rep.getAllNames();
}


@Override public boolean isEmpty()
{
   return complete_rep.isEmpty();
}


@Override public boolean includesRepository(BassRepository br)
{
   if (br == this) return true;
   return complete_rep.includesRepository(br);
}


BgtaBuddy getBuddyInfo(String buddyname)
{
   for (BgtaBuddyRepository bbr : buddy_reps) {
      BgtaBuddy toReturn = bbr.getBuddyInfo(buddyname);
      if (toReturn != null) return toReturn;
    }
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Just Buddies name getter                                                */
/*                                                                              */
/********************************************************************************/

List<String> getAllBuddyNames()
{
   ArrayList<String> toreturn = new ArrayList<String>();
   for (BgtaBuddyRepository bbr : buddy_reps) {
      for (BassName bud : bbr.getAllNames()) {
         toreturn.add(((BgtaBuddy) bud).getConnectionName());
       }
    }
   return toreturn;
}



/********************************************************************************/
/*                                                                              */
/*      Clean up                                                                */
/*                                                                              */
/********************************************************************************/

void removeManager(BgtaManager bm)
{
   BassRepository newrep = original_rep;
   BgtaBuddyRepository toremove = null;
   for (BgtaBuddyRepository bbr : buddy_reps) {
      if (!bbr.getManager().equals(bm))
         newrep = new BassRepositoryMerge(bbr,newrep);
      else
         toremove = bbr;
    }
   buddy_reps.remove(toremove);
   complete_rep = newrep;
}



}       // end of class BgtaRepository



/* end of BgtaRepository.java */
