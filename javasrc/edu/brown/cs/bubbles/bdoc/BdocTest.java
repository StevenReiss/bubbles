/********************************************************************************/
/*										*/
/*		BdocTest.java							*/
/*										*/
/*	Bubbles Environment Documentation test program				*/
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


package edu.brown.cs.bubbles.bdoc;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.SwingUtilities;


public class BdocTest implements BdocConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BdocTest bt = new BdocTest(args);

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

private BdocTest(String [] args)
{
   BdocFactory.setup();
}


/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

/*******************
private void runTest0()
{
   try {
      BdocRepository bt = new BdocRepository();
      bt.addJavadoc(new URL("file:///pro/java/linux/jdk1.6.0/docs/api/index.html"));
      bt.addJavadoc(new URL("file:///pro/clime/doc/index.html"));
      bt.addJavadoc(new URL("http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/api/index.html"));
      int ct = 0;
      for (BassConstants.BassName br : bt.getAllNames()) {
	 ++ct;
       }
      System.err.println("FOUND " + ct + " items");
    }
   catch (Throwable t) {
      System.err.println("Error building database: " + t);
    }
}
***********************/


private void runTest()
{
   BudaRoot root = new BudaRoot("Javadoc Test");
   root.pack();
   root.setVisible(true);
   SwingUtilities.invokeLater(new CreateJSearch(root));
}



private static class CreateJSearch implements Runnable {

   private BudaRoot for_root;

   CreateJSearch(BudaRoot br) {
      for_root = br;
    }

   @Override public void run() {
      try {
         BassFactory bf = BassFactory.getFactory();
         BudaBubble bb = bf.createSearch(BudaConstants.SearchType.SEARCH_DOC,null,null);
         for_root.add(bb,new BudaConstraint(100,400));
       }
      catch (Throwable t) {
         System.err.println("Error building database: " + t);
       }
    }

}	// end of inner class createJSearch



}	// end of class BdocTest




/* end of BdocTest.java */
