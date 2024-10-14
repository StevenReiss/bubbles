/********************************************************************************/
/*										*/
/*		BaleTest.java							*/
/*										*/
/*	Bubble Annotated Language Editor test program				*/
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


package edu.brown.cs.bubbles.bale;


import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import org.junit.Test;

import javax.swing.SwingUtilities;



public final class BaleTest implements BaleConstants {




/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BaleTest bt = new BaleTest();
   bt.test();
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BaleTest()
{
}



/********************************************************************************/
/*										*/
/*	Test code								*/
/*										*/
/********************************************************************************/

private void test()
{
   BumpClient.getBump();

   BudaRoot root = new BudaRoot("Editor Test");
   root.pack();

   root.setVisible(true);

   SwingUtilities.invokeLater(new CreateEditor(root));
}


@Test public void testJoin() {
   double [] a = new double [] { -2, -1, 7, 0 };
   a.hashCode();
};


private static class CreateEditor implements Runnable {

   private BudaRoot for_root;

   CreateEditor(BudaRoot br) {
      for_root = br;
    }

   @Override public void run() {
      BaleFactory bf = BaleFactory.getFactory();
      BudaBubble bb = bf.createMethodBubble("ivy","edu.brown.cs.ivy.xml.IvyXml.getText");
      if (bb == null) {
         System.err.println("TEST METHOD NOT FOUND");
         System.exit(1);
       }
      for_root.add(bb,new BudaConstraint(100,300));
    }

}	// end of inner class CreateEditor




}	// end of class BaleTest




/* end of BaleTest.java */

