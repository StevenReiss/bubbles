/********************************************************************************/
/*										*/
/*    BgtaResourceSwitchingBubble.java						*/
/*										*/
/*    Chat bubble that always sends message to the same full JID that it	*/
/*    receives from as opposed to the bare JID or an arbitrary resource 	*/
/********************************************************************************/
/* Copyright 2011 Brown University -- Andrew Kovacs				*/
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

package edu.brown.cs.bubbles.bgta;

import org.jivesoftware.smack.packet.Message;

import javax.naming.OperationNotSupportedException;
import javax.swing.event.DocumentEvent;

public class BgtaResourceSwitchingBubble extends BgtaBubble
{
   private static final long serialVersionUID = 1L;

   private BgtaChat the_chat;
   private BgtaManager the_manager;

   public BgtaResourceSwitchingBubble(BgtaManager m, String username)
   {
      this(m.startChat(username));
      the_manager = m;
   }

   private BgtaResourceSwitchingBubble(BgtaChat c)
   {
      super(c);
      the_chat = c;
   }

   @Override public void insertUpdate(DocumentEvent e)
   {
      super.insertUpdate(e);
      Message msg = (Message) the_chat.getLastMessage();
      if (msg == null) {
      	 return;
      }
      try {
	 if (!msg.getFrom().equals(the_chat.getChat().getParticipant())) {
	 	    the_chat.setChat(the_manager.startChat(msg.getFrom()).getChat());
	 }
      }
catch (OperationNotSupportedException e1) {
	 // Do nothing
      }
   }

}
