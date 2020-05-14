/********************************************************************************/
/*										*/
/*		BrepairFactory.java						*/
/*										*/
/*	Controller class for automatic bug repair in code bubbles		*/
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

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.batt.BattConstants;
import edu.brown.cs.bubbles.batt.BattFactory;
import edu.brown.cs.bubbles.batt.BattConstants.BattTest;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaConstants.BubbleViewCallback;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaWorkingSet;

public class BrepairFactory implements BrepairConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BrepairFactory the_factory = new BrepairFactory();

private static Set<BrepairFaultBubble> fault_bubbles = new HashSet<>();



/********************************************************************************/
/*										*/
/*	Bubbles setup methods							*/
/*										*/
/********************************************************************************/

public static void setup()			{ }


public static void initialize(BudaRoot br)
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JS :
      case PYTHON :
      case REBUS :
	 return;
      case JAVA :
	 break;
    }

   BattFactory batt = BattFactory.getFactory();
   batt.addPopupHandler(new TestPopupHandler());
   
   BudaRoot.addBubbleViewCallback(new BubbleTracker());
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static BrepairFactory getFactory()
{
   return the_factory;
}



private BrepairFactory()
{

}



/********************************************************************************/
/*										*/
/*	Handle test case popup menu						*/
/*										*/
/********************************************************************************/

private static class TestPopupHandler implements BattConstants.BattPopupHandler {

   @Override public void handlePopupMenu(BattTest test,BudaBubble bbl,JPopupMenu menu) {
      if (test == null) return;
      if (test.getStatus() != BattConstants.TestStatus.FAILURE) return;
      menu.add(new FixFailureAction(bbl,test));
    }

}	// end of inner class TestPopupHandler



private static class FixFailureAction extends AbstractAction {

   private BudaBubble near_bubble;
   private BattTest test_case;

   private static final long serialVersionUID = 1;

   FixFailureAction(BudaBubble bbl,BattTest test) {
      super("Work on Failure for " + test.getMethodName());
      near_bubble = bbl;
      test_case = test;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BrepairTestFixer fixer = new BrepairTestFixer(near_bubble,test_case);
      BoardThreadPool.start(fixer);
    }

}	// end of inner class FixFailureAction



/********************************************************************************/
/*                                                                              */
/*      Track bubbles                                                           */
/*                                                                              */
/********************************************************************************/

private static class BubbleTracker implements BubbleViewCallback {
   
   @Override public void doneConfiguration()                            { }
   @Override public void focusChanged(BudaBubble bb,boolean set)        { }
   @Override public boolean bubbleActionDone(BudaBubble bb)             { return false; }
   @Override public void workingSetAdded(BudaWorkingSet ws)             { }
   @Override public void workingSetRemoved(BudaWorkingSet ws)           { }
   @Override public void copyFromTo(BudaBubble f,BudaBubble t)          { }
   
   @Override public void bubbleAdded(BudaBubble bb) {
      if (bb instanceof BrepairFaultBubble) {
         BrepairFaultBubble fb = (BrepairFaultBubble) bb;
         fault_bubbles.add(fb);
       }
    }
   @Override public void bubbleRemoved(BudaBubble bb)  {
      if (bb instanceof BrepairFaultBubble) {
         BrepairFaultBubble fb = (BrepairFaultBubble) bb;
         fault_bubbles.remove(fb);
       }
      else if (bb.getClass().getName().contains("Bicex")) {
         for (BrepairFaultBubble fb : fault_bubbles) {
            fb.handleRemove(bb);
          }
       }
    }
   
   
}       // end of inner class BubbleTracker




}	// end of class BrepairFactory




/* end of BrepairFactory.java */

