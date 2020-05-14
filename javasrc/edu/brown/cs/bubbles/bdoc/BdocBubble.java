/********************************************************************************/
/*										*/
/*		BdocBubble.java 						*/
/*										*/
/*	Bubbles Environment Documentation javadoc bubble			*/
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

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;

import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import java.awt.Dimension;


class BdocBubble extends BudaBubble implements BdocConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BdocReference for_ref;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocBubble(BdocReference br) throws BdocException
{
   for_ref = br;
   BdocPanel pnl = new BdocPanel(br);
   JComponent cmp = pnl.getPanel();
   cmp.setBorder(new EmptyBorder(0,0,0,0));

   Dimension d1 = cmp.getPreferredSize();
   if (d1.height > BUBBLE_HEIGHT) d1.height = BUBBLE_HEIGHT;
   cmp.setPreferredSize(d1);

   cmp.setFocusable(true);
   cmp.addMouseListener(new BudaConstants.FocusOnEntry());

   setContentPane(cmp);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BdocReference getReference()            { return for_ref; }

}	// end of class BdocBubble




/* end of BdocBubble.java */
