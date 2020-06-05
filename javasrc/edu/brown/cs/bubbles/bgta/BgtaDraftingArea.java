/********************************************************************************/
/*										*/
/*		BgtaDraftingArea.java						*/
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


import edu.brown.cs.ivy.swing.SwingTextArea;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;



class BgtaDraftingArea extends SwingTextArea {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BgtaChat	my_chat;
private BgtaLoggingArea my_log;
private BgtaBubble	my_bubble;

private static final long serialVersionUID = 1L;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaDraftingArea(BgtaLoggingArea bla,BgtaBubble mybub)
{
   super(1,25);
   my_log = bla;
   my_bubble = mybub;
   setLineWrap(true);
   setWrapStyleWord(true);
   setOpaque(false);
   addKeyListener(new DraftingListener());
   addFocusListener(new FocusForLogListener());
}

/********************************************************************************/
/*										*/
/*	Setters								*/
/*										*/
/********************************************************************************/
void setChat(BgtaChat chat) {
    my_chat = chat;
}

/********************************************************************************/
/*										*/
/*	Sending methods 							*/
/*										*/
/********************************************************************************/

void send()
{
   send(getText());   
}


void send(String message)
{
   boolean sent = my_chat.sendMessage(message);
   if (sent) setText("");
   else
       grabFocus();
}



/********************************************************************************/
/*										*/
/*	 Listeners for handling input						*/
/*										*/
/********************************************************************************/

private class DraftingListener extends KeyAdapter {

   @Override public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	 send();
	 // Necessary because otherwise sending doesn't look good.
	 e.setKeyCode(KeyEvent.VK_LEFT);
       }
    }

}	// end of inner class DraftingListener



private class FocusForLogListener implements FocusListener {

   @Override public void focusGained(FocusEvent e) {
      my_log.unbold();
      my_bubble.setAltColorIsOn(false);
    }

   @Override public void focusLost(FocusEvent e) {
      if (my_bubble.reloadAltColor()) {
	 my_log.bold();
       }
    }

}	// end of inner class FocusForLogListener



}	// end of class BgtaDraftingArea



/* end of BgtaDraftingArea.java */
