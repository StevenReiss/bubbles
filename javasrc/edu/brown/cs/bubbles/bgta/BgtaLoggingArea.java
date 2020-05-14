/********************************************************************************/
/*										*/
/*		BgtaLoggingArea.java						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
/* Copyright 2011 Brown University -- Sumner Warren            */
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


package edu.brown.cs.bubbles.bgta;

import edu.brown.cs.bubbles.buda.BudaCursorManager;

import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import java.awt.Cursor;
import java.awt.Dimension;


class BgtaLoggingArea extends JTextPane implements BgtaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

// private AttributeSet	is_bolded;
//private AttributeSet	is_unbolded;
//private AttributeSet	in_use;
// private int		last_focused_caret_pos;

private static final long serialVersionUID = 1L;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaLoggingArea(BgtaBubble bub)
{
   // super(7, 25);
   super();
   setContentType("text/html");
   setText("");
   // last_focused_caret_pos = 0;
   Dimension d = new Dimension(BGTA_LOG_WIDTH,BGTA_LOG_HEIGHT);
   setPreferredSize(d);
   setSize(d);
   putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
   // setLineWrap(true);
   // setWrapStyleWord(true);
   setEditable(false);
   setOpaque(false);
   BudaCursorManager.setCursor(this,new Cursor(Cursor.TEXT_CURSOR));
   setBorder(new EmptyBorder(0,5,0,0));

//   StyleContext sc = StyleContext.getDefaultStyleContext();
//   is_unbolded = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Bold, false);
   // is_bolded = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Bold, true);
//   in_use = null;// is_unbolded;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean getScrollableTracksViewportWidth()
{
   return true;
}



/********************************************************************************/
/*										*/
/*	Bolding methods (currently not in use, could be added back later)	*/
/*										*/
/********************************************************************************/

void bold()
{
   // last_focused_caret_pos = getDocument().getLength();
   // in_use = is_bolded;
}


void unbold()
{
   // int len;
//   in_use = is_unbolded;
   /*
      try{
      len = getDocument().getLength() - last_focused_caret_pos;
      String tochange = getDocument().getText(last_focused_caret_pos, len);
      getDocument().remove(last_focused_caret_pos, len);
      getDocument().insertString(getDocument().getLength(), tochange, in_use);
    }catch(BadLocationException e){}*/
}



}	// end of class BgtaLoggingArea




/* end of BgtaLoggingArea.java */
