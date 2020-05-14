/********************************************************************************/
/*                                                                              */
/*              BrepairTestFixer.java                                           */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.bubbles.brepair;

import edu.brown.cs.bubbles.batt.BattFactory;
import edu.brown.cs.bubbles.batt.BattConstants.BattTest;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

class BrepairTestFixer implements BrepairConstants, Runnable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BattTest                failed_test;
private BudaBubble              near_bubble;





/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BrepairTestFixer(BudaBubble bbl,BattTest test) 
{
   near_bubble = bbl;
   failed_test = test;
}



/********************************************************************************/
/*                                                                              */
/*      Main processing routine                                                 */
/*                                                                              */
/********************************************************************************/

@Override public void run() 
{
   BowiFactory.startTask();
   try {
      BrepairCountData counts = setupFaultLocalizationData();
      BrepairFaultBubble bbl = new BrepairFaultBubble(failed_test,counts);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(near_bubble);
      bba.addBubble(bbl,near_bubble,null,
            BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_NEW|
            BudaConstants.PLACEMENT_MOVETO|BudaConstants.PLACEMENT_USER);
    }
   finally {
      BowiFactory.stopTask();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Setup fault localization data                                           */
/*                                                                              */
/********************************************************************************/

private BrepairCountData setupFaultLocalizationData()
{
   BrepairCountData counts = new BrepairCountData(failed_test.getCountData());
   for (BattTest btc : BattFactory.getFactory().getAllTestCases()) {
      if (btc == failed_test) continue;
      if (btc.getCountData() == null) continue;
      switch (btc.getStatus()) {
         case FAILURE :
            counts.addCountData(btc.getCountData(),false);
            break;
         case SUCCESS :
            counts.addCountData(btc.getCountData(),true);
            break;
         case UNKNOWN :
            break;
       }
    }
   
   String tnm = failed_test.getClassName() + "." + failed_test.getMethodName() + "(";
   
   counts.computeSortedMethods(tnm,MAX_METHODS,CUTOFF_VALUE);
   
   return counts;
}




}       // end of class BrepairTestFixer




/* end of BrepairTestFixer.java */

