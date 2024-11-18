/********************************************************************************/
/*                                                                              */
/*              BstyleFixAdapter.java                                           */
/*                                                                              */
/*      BFIX adapter to fix style problems automatically or on demand           */
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



package edu.brown.cs.bubbles.bstyle;

import java.util.List;

import edu.brown.cs.bubbles.bfix.BfixAdapter;
import edu.brown.cs.bubbles.bfix.BfixConstants;
import edu.brown.cs.bubbles.bfix.BfixCorrector;
import edu.brown.cs.bubbles.bfix.BfixFixer;
import edu.brown.cs.bubbles.board.BoardLog;

public class BstyleFixAdapter extends BfixAdapter implements BstyleConstants, BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<BstyleFixer> fixer_set;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BstyleFixAdapter()
{
   super("StyleFixer");
   
   fixer_set = BstyleFixer.getStyleFixers(); 
}


/********************************************************************************/
/*                                                                              */
/*      Handle fixes that might need checking                                   */
/*                                                                              */
/********************************************************************************/

@Override 
public void addFixers(BfixCorrector bc,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslts)
{
   // check if this is a style problem that needs double checking
   // if so return a fixer using bstylefixer
}


/********************************************************************************/
/*                                                                              */
/*      Handle style problems                                                   */
/*                                                                              */
/********************************************************************************/

public BfixRunnableFix findStyleFixer(BfixCorrector bc,BumpProblem bp,boolean explicit)
{
   if (!bp.getCategory().equals("BSTYLE")) return null;
   
   BoardLog.logD("BSTYLE","Work on problem " + bp.getData() + " " + bp.getMessage());
   
   for (BstyleFixer bf : fixer_set) {
      BfixRunnableFix rf = bf.findFix(bc,bp,explicit);  
      if (rf != null) return rf;
    }
   
   
   return null;
}



}       // end of class BstyleFixAdapter




/* end of BstyleFixAdapter.java */

