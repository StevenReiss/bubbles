/********************************************************************************/
/*										*/
/*		BassTest.java							*/
/*										*/
/*	Bubble Augmented Search Strategies tester				*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import javax.swing.SwingUtilities;

import java.awt.Point;



public class BassTest implements BassConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BassFactory.setup();
   BassTest bt = new BassTest();
   bt.test();
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BassTest()				{ }





/********************************************************************************/
/*										*/
/*	Text code								*/
/*										*/
/********************************************************************************/

private void test()
{
   BumpClient bc = BumpClient.getBump();
   bc.waitForIDE();

   Runtime.getRuntime().addShutdownHook(new CloseBedrock());

   BudaRoot root = new BudaRoot("Search Test");
   root.pack();

   root.setVisible(true);

   SwingUtilities.invokeLater(new CreateSearch(root));
}





private static class CreateSearch implements Runnable {

   private BudaRoot for_root;

   CreateSearch(BudaRoot br) {
      for_root = br;
    }

   @Override public void run() {
      for_root.createSearchBubble(new Point(100,100),null,null);
    }

}	// end of inner class CreateSearch




/********************************************************************************/
/*										*/
/*	Class to stop bedrock							*/
/*										*/
/********************************************************************************/

private static class CloseBedrock extends Thread {

   @Override public void run() {
      BumpClient.getBump().stopIDE();
    }

}	// end of inner class CloseBedrock




}	// end of class BassTest




/* end of BassTest.java */


