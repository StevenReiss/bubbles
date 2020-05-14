/********************************************************************************/
/*										*/
/*		BconBubble.java 						*/
/*										*/
/*	Bubbles Environment Context Viewer constants				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.buda.BudaBubble;

import javax.swing.JComponent;

import java.awt.Dimension;
import java.awt.event.MouseEvent;



class BconBubble extends BudaBubble implements BconConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BconPanel	for_panel;

private static final long serialVersionUID = 1;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconBubble(BconPanel pnl)
{
   for_panel = pnl;

   JComponent cmp = pnl.getComponent();

   Dimension d1 = cmp.getPreferredSize();
   cmp.setSize(d1);

   setContentPane(cmp);
}



@Override protected void localDispose()
{
   for_panel.dispose();
}


@Override public void handlePopupMenu(MouseEvent e)
{
   for_panel.handlePopupMenu(e);
}


BconPanel getPanel()                    { return for_panel; }

}	// end of class BconBubble




/* end of BconBubble.java */
