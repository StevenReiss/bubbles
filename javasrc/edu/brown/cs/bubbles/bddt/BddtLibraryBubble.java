/********************************************************************************/
/*										*/
/*		BddtLibraryBubble.java						*/
/*										*/
/*	Bubble Environment bubble for a library routine 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss			*/
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




package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.JTextField;


class BddtLibraryBubble extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpStackFrame	current_frame;
private JTextField	method_field;
private JTextField	line_field;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtLibraryBubble(BumpStackFrame bsf)
{
   current_frame = bsf;
   setupPanel();
}




/********************************************************************************/
/*										*/
/*	Methods for changing the context					*/
/*										*/
/********************************************************************************/

void resetFrame(BumpStackFrame bsf)
{
   current_frame = bsf;
   method_field.setText(bsf.getMethod());
   String lno = Integer.toString(bsf.getLineNumber());
   line_field.setText(lno);
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.beginLayout();
   pnl.addBannerLabel("Library Routine");
   pnl.addSeparator();
   method_field = pnl.addTextField("Method",current_frame.getMethod(),null,null);
   method_field.setEditable(false);
   String lno = Integer.toString(current_frame.getLineNumber());
   line_field = pnl.addTextField("Line",lno,null,null);
   line_field.setEditable(false);

   setContentPane(pnl);

   addMouseListener(new FocusOnEntry());
}




}	// end of BddtLibraryBubble




/* end of BddtLibraryBubbel.java */
