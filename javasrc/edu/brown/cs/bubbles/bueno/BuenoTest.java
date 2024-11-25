/********************************************************************************/
/*										*/
/*		BuenoTest.java							*/
/*										*/
/*	BUbbles Environment New Objects creator tester				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.util.List;


public final class BuenoTest implements BuenoConstants, BuenoConstants.BuenoInserter
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BuenoTest bt = new BuenoTest(args);

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

private BuenoTest(String [] args)
{
   BoardSetup brd = BoardSetup.getSetup();
   brd.setSkipSplash();

   BuenoFactory.setup();

   BuenoFactory bf = BuenoFactory.getFactory();
   bf.addInsertionHandler(this);
}




/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

private void runTest()
{
   BumpClient bc = BumpClient.getBump();
   bc.waitForIDE();

   BuenoProperties bp = new BuenoProperties();
   BuenoFactory bf = BuenoFactory.getFactory();

   List<BumpLocation> locs = bc.findMethod("bubbles","edu.brown.cs.bubbles.bueno.BuenoTest.runTest()",false);
   BuenoLocation where = null;
   for (BumpLocation bloc : locs) {
      where = bf.createLocation(bloc,false);
    }

   bp.put(BuenoKey.KEY_NAME,"testMethod");
   bp.put(BuenoKey.KEY_PARAMETERS,"int x,int y");
   bp.put(BuenoKey.KEY_RETURNS,"int");
   bp.put(BuenoKey.KEY_ADD_COMMENT,Boolean.TRUE);
   bp.put(BuenoKey.KEY_INDENT,3);

   bf.createNew(BuenoType.NEW_METHOD,where,bp);
}



/********************************************************************************/
/*										*/
/*	Hnadle insertion requests						*/
/*										*/
/********************************************************************************/

@Override public boolean insertText(BuenoLocation loc,String text,boolean format)
{
   System.err.println("INSERT TEXT REQUEST:");
   System.err.println(text);
   System.err.println("END OF INSERTION");

   return true;
}




}	// end of class BuenoTest



/* end of BuenoTest.java */
