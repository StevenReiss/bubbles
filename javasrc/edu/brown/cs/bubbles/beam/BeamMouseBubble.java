/********************************************************************************/
/*										*/
/*		BeamMouseBubble.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items mouse bindings bubble	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingEditorPane;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;


class BeamMouseBubble extends BudaBubble implements BeamConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructor								*/
/*										*/
/********************************************************************************/

BeamMouseBubble()
{
   super(null,BudaBorder.RECTANGLE);

   String cnts = null;

   try {
      InputStream ins = BoardProperties.getResourceFile("mouseusage.html");
      if (ins == null) return;
      cnts = IvyFile.loadFile(ins);
      ins.close();
    }
   catch (IOException e) { }

   JEditorPane edit = new SwingEditorPane("text/html",cnts);
   edit.setEditable(false);
   edit.setPreferredSize(new Dimension(600,670));
   JScrollPane jsp = new JScrollPane(edit);
   jsp.scrollRectToVisible(new Rectangle(0,0,10,10));

   setContentPane(jsp,null);
}




}	// end of class BeamMouseBubble




/* end of BeamMouseBubble.java */
