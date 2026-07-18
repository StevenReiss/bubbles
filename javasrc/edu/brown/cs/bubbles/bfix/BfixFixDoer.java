/********************************************************************************/
/*                                                                              */
/*              BfixFixDoer.java                                                */
/*                                                                              */
/*      Runnable to actual do a fix                                             */
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



package edu.brown.cs.bubbles.bfix;

import java.awt.Point;
import java.util.List;

import javax.swing.text.JTextComponent;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;
import edu.brown.cs.bubbles.burp.BurpHistory;

public abstract class BfixFixDoer implements BfixConstants, BfixConstants.BfixRunnableFix 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected BfixCorrector for_corrector;
protected BumpProblem for_problem;
private long initial_time;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BfixFixDoer(BfixCorrector corr,BumpProblem prob,long time)
{
   for_corrector = corr;
   for_problem = prob;
   initial_time = time;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public abstract double getRegionOrder();



/********************************************************************************/
/*                                                                              */
/*      Convert Runnable to Callable                                            */
/*                                                                              */
/********************************************************************************/

@Override public void run() 
{
   // only called when result isn't needed
   try {
      call();
    }
   catch (Exception e) {
      BoardLog.logE("BFIX","Problem with fixer",e);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected boolean testEdit(BfixEdit edit,BfixCheckAreas safeareas,String cmd) 
{
   return testEdit(edit,safeareas,cmd,true);
}


protected boolean testEdit(BfixEdit edit,BfixCheckAreas safeareas,String cmd,
      boolean fmt) 
{
   BumpClient bc = BumpClient.getBump();
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   List<BumpProblem> probs = bc.getProblems(doc.getFile());
   if (for_problem != null && !BfixAdapter.checkProblemPresent(for_problem,probs)) {
      return false;
    }
   if (initial_time > 0 && for_corrector.getStartTime() != initial_time) {
      return false;
    }
   if (safeareas != null) {
      for (Point p : safeareas.getAreas()) {
         if (!BfixAdapter.checkSafePosition(for_corrector,p.x,p.y)) {
            return false;
          }
       }
    }
   
   if (edit != null) {
      BaleWindow edwin = for_corrector.getEditor();
      JTextComponent edcmp = edwin.getEditor();
      BurpHistory bh = BurpHistory.getHistory();
      if (edcmp != null) {
         bh.beginEditAction(edcmp);
       }
      try {
         if (cmd != null) {
            BoardMetrics.noteCommand("BFIX",cmd + "_" + for_corrector.getBubbleId());
          }
         edit.doEdit(fmt,fmt);
         if (cmd != null) {
            BoardMetrics.noteCommand("BFIX","Done" + 
                  cmd + "_" + for_corrector.getBubbleId());
          }
       }
      catch (Throwable t) {
         return false;
       }
      finally {
         if (edcmp != null) {
            bh.endEditAction(edcmp);
          }
       }
    }
   
   return true;
}

}       // end of class BfixFixDoer




/* end of BfixFixDoer.java */

