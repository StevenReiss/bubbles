/********************************************************************************/
/*                                                                              */
/*              BfixFixer.java                                                  */
/*                                                                              */
/*      Generic form of a routine to find a fix                                 */
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
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;

import javax.swing.SwingUtilities;

abstract class BfixFixer implements Runnable, BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected BfixCorrector         for_corrector;
protected BumpProblem           for_problem;
private FixAdapter              subfix_data;
private BfixMemo                fix_memo;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BfixFixer(BfixCorrector bc,BumpProblem bp)       
{
   for_corrector = bc;
   for_problem = bp;
   subfix_data = null;
   fix_memo = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void setSubFixData(FixAdapter sd)
{
   subfix_data = sd;
}


BfixMemo getMemo()
{
   if (fix_memo == null) {
      fix_memo = new BfixMemo(for_problem,getClass(),getMemoId());
    }
   
   return fix_memo;
}


protected String getMemoId()
{
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Running methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public final void run()
{
   for_corrector.removePending(getMemo());
   
   RunnableFix r = findFix();
   if (subfix_data == null) {
      if (r != null) SwingUtilities.invokeLater(r);
    }
   else{
      subfix_data.noteFix(r);
    }
}



abstract protected RunnableFix findFix();



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected String createPrivateBuffer(String proj,String filename)
{
   BumpClient bc = BumpClient.getBump();
   String pid = null;
   if (subfix_data == null) {
      pid = bc.createPrivateBuffer(proj,filename,null);
    }
   else {
      pid = bc.createPrivateBuffer(proj,filename,null,subfix_data.getPrivateBufferId());
    }
   
   return pid;
}




/********************************************************************************/
/*                                                                              */
/*      Handle multiple fix attempts                                            */
/*                                                                              */
/********************************************************************************/

protected void checkForFurtherFix(BfixCorrector cor,Runnable okfix,String pid,
      BumpProblem bp)
{
   FutureCallback fc = new FutureCallback(okfix);
   BfixSubFix nsf = new BfixSubFix(cor,bp,pid,fc);
   cor.checkProblemFixable(nsf);
}




private class FutureCallback implements CanFixCallback
{
   private Runnable fix_to_make;
   
   FutureCallback(Runnable r) {
      fix_to_make = r;
    }
   
   @Override public void canFix(boolean fg) {
      if (subfix_data != null) subfix_data.noteStatus(fg);
      else if (fg) {
         SwingUtilities.invokeLater(fix_to_make);
       }
    }
   
}       // end of inner class FutureCallback



}       // end of class BfixFixer




/* end of BfixFixer.java */

