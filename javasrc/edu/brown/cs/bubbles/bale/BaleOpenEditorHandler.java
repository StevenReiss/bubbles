/********************************************************************************/
/*										*/
/*		BaleOpenEditorHandler.java					*/
/*										*/
/*	Bubble Annotated Language Editor factory for creating new methods	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Hsu-Sheng Ko 	      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/

package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpConstants;

import java.awt.Frame;
import java.awt.Point;



class BaleOpenEditorHandler implements BumpConstants.BumpOpenEditorBubbleHandler
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot buda_root;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleOpenEditorHandler(BudaRoot budaRoot)
{
   buda_root = budaRoot;
}



/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

@Override public void handleOpenEditorBubble(String projname, String resourcepath, String type)
{
   BudaBubble bubble = null;

   if ("Function".equals(type)) {
      bubble = BaleFactory.getFactory().createMethodBubble(projname, resourcepath);
    }
   else {
      bubble = BaleFactory.getFactory().createClassBubble(projname, resourcepath);
    }

   if (bubble == null) return;

   if (buda_root.getState()!= Frame.NORMAL) buda_root.setState(Frame.NORMAL);

   buda_root.toFront();
   buda_root.repaint();
   try {
      Thread.sleep(DELAY_TIME);
    }
   catch (InterruptedException ie) {
      ie.printStackTrace();
    }

   Point pos = buda_root.getCurrentBubbleArea().getViewPosition();

   buda_root.getCurrentBubbleArea().addBubble(bubble, pos.x, pos.y);
   bubble.markBubbleAsNew();

   bubble.requestFocus();

   BoardMetrics.noteCommand("BALE","EcipseOpen" + type);
}




}	// end of class BaleOpenEditorHandler

/* end of BaleOpenEditorHandler.java */
