/********************************************************************************/
/*                                                                              */
/*              BfixSubFix.java                                                 */
/*                                                                              */
/*      Handle information for secondary fixes                                  */
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

class BfixSubFix implements BfixConstants, BfixConstants.FixAdapter
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          private_buffer;
private BfixCorrector   for_corrector;
private BumpProblem     for_problem;
private CanFixCallback  fix_callback;
private int             num_fixers;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixSubFix(BfixCorrector cor,BumpProblem bp,String pid,CanFixCallback cb)
{
   for_corrector = cor;
   private_buffer = pid;
   for_problem = bp;
   num_fixers = 0;
   fix_callback = cb;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getPrivateBufferId()    { return private_buffer; }
@Override public BumpProblem getProblem()       { return for_problem; }
BfixCorrector getCorrector()                    { return for_corrector; }

boolean isRelevant()                            { return fix_callback != null; }




/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void noteFixersAdded(int ct)
{
   if (ct <= 0) {
      fix_callback.canFix(false);
      fix_callback = null;
    }
   else num_fixers = ct;
}


@Override public void noteFix(Runnable fix)
{
   noteStatus(fix != null);
}



@Override public void noteStatus(boolean fixable)
{
   Boolean sts = null;
   CanFixCallback cb = fix_callback;
   
   synchronized (this) {
      if (fix_callback == null) return;
      if (fixable) sts = true;
      else if (--num_fixers == 0) {
         sts = false;
       }
      if (sts != null) fix_callback = null;
    }
   
   if (sts != null) {
      BumpClient bc = BumpClient.getBump();
      if (private_buffer != null) {
         bc.removePrivateBuffer(for_problem.getProject(),
               for_problem.getFile().getPath(),private_buffer);
       }
      cb.canFix(sts);
    }
}




}       // end of class BfixSubFix




/* end of BfixSubFix.java */

