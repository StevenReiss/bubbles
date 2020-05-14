/********************************************************************************/
/*										*/
/*		BassCellRenderer.java						*/
/*										*/
/*	Bubble Augmented Search Strategies tree cell renderer			*/
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


import javax.swing.tree.DefaultTreeCellRenderer;

import edu.brown.cs.bubbles.board.BoardColors;



class BassCellRenderer extends DefaultTreeCellRenderer implements BassConstants
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

BassCellRenderer()
{
   BoardColors.setColors(this,BASS_PANEL_TOP_COLOR_PROP);
   setTextSelectionColor(getForeground());
   setTextNonSelectionColor(getForeground());
   setLeafIcon(null);
   setClosedIcon(null);
   setOpenIcon(null);
}



}	// end of class BassCellRenderer




/* end of BassCellRenderer.java */
