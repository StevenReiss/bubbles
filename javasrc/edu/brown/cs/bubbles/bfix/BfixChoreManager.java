/********************************************************************************/
/*                                                                              */
/*              BfixChoreManager.java                                           */
/*                                                                              */
/*      Manage the current set of chores                                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;

import edu.brown.cs.ivy.swing.SwingEventListenerList;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


class BfixChoreManager implements BfixConstants, ListModel<BfixChore>
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<BfixChore>          chore_set;
private SwingEventListenerList<ListDataListener> list_listeners;

private static final int MAX_CHORES = 20;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixChoreManager()
{
   chore_set = new LinkedList<>();
   list_listeners = new SwingEventListenerList<>(ListDataListener.class);
   
   BumpClient.getBump().addProblemHandler(null,new ProblemHandler());
   BumpClient.getBump().addChangeHandler(new ChangeHandler());
}



/********************************************************************************/
/*                                                                              */
/*      Methods to add and remove chores                                        */
/*                                                                              */
/********************************************************************************/

void addChore(BfixChore chore)
{
   synchronized (chore_set) {
      removeChore(chore);
      chore_set.add(0,chore);
      
      ListDataEvent evt = new ListDataEvent(this,ListDataEvent.INTERVAL_ADDED,0,1);
      for (ListDataListener ldl : list_listeners) {
         ldl.intervalAdded(evt);
       }
      
      if (MAX_CHORES > 0) {
         while (chore_set.size() > MAX_CHORES) {
            BfixChore bt = chore_set.get(MAX_CHORES);
            removeChore(bt);
          }
       }
    }
}
      

void removeChore(BfixChore chore)
{
   synchronized (chore_set) {
      int idx = chore_set.indexOf(chore);
      if (idx < 0) return;
      chore_set.remove(idx);
      ListDataEvent evt = new ListDataEvent(this,ListDataEvent.INTERVAL_REMOVED,idx,idx);
      for (ListDataListener ldl : list_listeners) {
         ldl.intervalRemoved(evt);
       }
    }
}


void removeCorrector(BfixCorrector corr)
{
   synchronized (chore_set) {
      List<BfixChore> dels = new ArrayList<>();
      for (BfixChore chore : chore_set) {
         if (chore.getCorrector() == corr) dels.add(chore);
       }
      for (BfixChore chore : dels) {
         removeChore(chore);
       }
    }
}



void validate(File f,boolean force)
{
   List<BfixChore> dels = new ArrayList<BfixChore>();
   List<BfixChore> vals = new ArrayList<BfixChore>();

   synchronized (chore_set) {
      for (BfixChore chore : chore_set) {
         if (f != null && !f.equals(chore.getFile())) continue;
         if (chore.getCorrector() == null) dels.add(chore);
         else vals.add(chore);
      }
   }
   for (BfixChore ch : vals) {
      if (!ch.validate(force)) dels.add(ch);
   }

   synchronized (chore_set) {
      for (BfixChore chore : dels) {
         removeChore(chore);
      }
   }
}




/********************************************************************************/
/*                                                                              */
/*      List Model methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void addListDataListener(ListDataListener ldl)
{
   list_listeners.add(ldl);
}


@Override public void removeListDataListener(ListDataListener ldl)
{
   list_listeners.remove(ldl);
}


@Override public BfixChore getElementAt(int idx)
{
   if (idx < 0 || idx >= chore_set.size()) return null;
   return chore_set.get(idx);
}


@Override public int getSize()
{
   return chore_set.size();
}



/********************************************************************************/
/*                                                                              */
/*      Problem handler                                                         */
/*                                                                              */
/********************************************************************************/

private final class ProblemHandler implements BumpConstants.BumpProblemHandler {
   
   @Override public void handleProblemAdded(BumpProblem bp) { }
   
   @Override public void handleProblemRemoved(BumpProblem bp) { 
      File f = bp.getFile();
      if (f != null) validate(f,false);
    }
   
   @Override public void handleClearProblems() { }
   
   @Override public void handleProblemsDone() {
      // doing this on each change might be too often
      // validate();
    }

}       // end of inner class ProblemHandler


/********************************************************************************/
/*                                                                              */
/*      File Change handler                                                     */
/*                                                                              */
/********************************************************************************/

private final class ChangeHandler implements BumpConstants.BumpChangeHandler {
   
   
   
   @Override public void handleFileChanged(String proj,String file) {
      File f = new File(file);
      validate(f,true);
    }
   
   
   
   
   
   
   
   
}


}       // end of class BfixChoreManager



/* end of BfixChoreManager.java */

