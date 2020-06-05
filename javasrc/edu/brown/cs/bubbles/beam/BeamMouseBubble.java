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
import edu.brown.cs.ivy.swing.SwingEditorPane;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


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

   StringBuffer buf = new StringBuffer();
   BufferedReader br = null;

   try {
      InputStream ins = BoardProperties.getLibraryFile("mouseusage.html");
      if (ins == null) return;
      br = new BufferedReader(new InputStreamReader(ins));
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 buf.append(ln);
	 buf.append("\n");
       }
      ins.close();
    }
   catch (IOException e) { }

   JEditorPane edit = new SwingEditorPane("text/html",buf.toString());
   edit.setEditable(false);
   edit.setPreferredSize(new Dimension(600,670));
   JScrollPane jsp = new JScrollPane(edit);
   jsp.scrollRectToVisible(new Rectangle(0,0,10,10));

   setContentPane(jsp,null);
}




}	// end of class BeamMouseBubble




/* end of BeamMouseBubble.java */
