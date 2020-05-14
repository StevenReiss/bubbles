/********************************************************************************/
/*										*/
/*		BconTest.java							*/
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



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.SwingUtilities;

import java.awt.Point;
import java.io.File;



public class BconTest implements BconConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BconTest bt = new BconTest(args);

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

private BconTest(String [] args)
{
   BoardSetup brd = BoardSetup.getSetup();
   brd.setSkipSplash();
   brd.setDefaultWorkspace("/home/spr/Eclipse/bubblesx");

   BconFactory.setup();
}


/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

private void runTest()
{
   BudaRoot root = new BudaRoot("Context Test");
   root.pack();
   root.setVisible(true);
   SwingUtilities.invokeLater(new CreateBcon(root));
}



private static class CreateBcon implements Runnable {

   private BudaRoot for_root;

   CreateBcon(BudaRoot br) {
      for_root = br;
    }

   @Override public void run() {
      try {
         File f = new File("/pro/ivy/javasrc/edu/brown/cs/ivy/xml/IvyXmlWriter.java");
         BconFactory bcf = BconFactory.getFactory();
         BaleFactory bale = BaleFactory.getFactory();
   
         BudaBubble bb = bale.createMethodBubble("ivy","edu.brown.cs.ivy.xml.IvyXmlWriter.text");
         for_root.add(bb,new BudaConstraint(100,300));
         BudaBubble nbb = bcf.createOverviewBubble(bb,new Point(100,300));
         for_root.add(nbb,new BudaConstraint(500,300));
         BudaBubble cbb = bcf.createClassBubble(bb,"ivy",f,"edu.brown.cs.ivy.xml.IvyXmlWriter",false);
         for_root.add(cbb,new BudaConstraint(850,300));
         BudaBubble pbb = bcf.createPackageBubble(bb,"bubbles","edu.brown.cs.bubbles.bcon");
         for_root.add(pbb,new BudaConstraint(100,600));
       }
      catch (Throwable t) {
         System.err.println("Error building database: " + t);
         t.printStackTrace();
       }
    }

}	// end of inner class createBcon



}	// end of class BconTest




/* end of BconTest.java */
