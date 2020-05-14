/********************************************************************************/
/*										*/
/*		BassTextBubble.java						*/
/*										*/
/*	Bubble Augmented Search Strategies text search				*/
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

import edu.brown.cs.bubbles.buda.BudaBubble;

import java.awt.Dimension;
import java.awt.event.MouseEvent;


class BassTextBubble extends BudaBubble implements BassConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassTextBubble()
{
   this("");
}



BassTextBubble(String initialsearch)
{
   BassTextSearch sb = new BassTextSearch();

   Dimension d = sb.getPreferredSize();
   sb.setSize(d);

   setTransient(true);

   setContentPane(sb,sb.getEditor());

   sb.getEditor().setText(initialsearch);
}



/********************************************************************************/
/*										*/
/*     Action methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   // pass e along to text search box
}



}	// end of class BassTextBubble





/* end of BassTextBubble.java */

