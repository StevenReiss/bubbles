/********************************************************************************/
/*										*/
/*		BddtTest.java							*/
/*										*/
/*	Bubbles Environment Context Viewer test program 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.SwingUtilities;



public final class BddtTest implements BddtConstants, BumpConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BddtTest bt = new BddtTest(args);

   bt.runTest();
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BddtTest(String [] args)
{
   BoardSetup brd = BoardSetup.getSetup();
   brd.setSkipSplash();

   BddtFactory.setup();
}


/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

private void runTest()
{
   BddtFactory.setup();
   BudaRoot root = new BudaRoot("Context Test");
   root.pack();
   root.setVisible(true);
   BddtFactory.initialize(root);
   root.restoreConfiguration(null);

   try { 
      Thread.sleep(5000);
    } 
   catch (InterruptedException e) { }

   SwingUtilities.invokeLater(new CreateBddt(root));
}



private static class CreateBddt implements Runnable {

   CreateBddt(BudaRoot br) { }

   @Override public void run() {
      try {
	 BddtFactory bf = BddtFactory.getFactory();
	 BumpClient bc = BumpClient.getBump();
	 bc.waitForIDE();
	 BumpRunModel run = bc.getRunModel();
	 for (BumpLaunchConfig blc : run.getLaunchConfigurations()) {
	    if (blc.getConfigName().equals("buda test")) {
	       bf.newDebugger(blc);
	       break;
	     }
	  }
       }
      catch (Throwable t) {
	 System.err.println("Error testing bddt: " + t);
	 t.printStackTrace();
       }
    }

}	// end of inner class CreateBddt



}	// end of class BddtTest




/* end of BddtTest.java */

