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

   private BgtaChat chat;
   private BgtaManager man;

   public BgtaResourceSwitchingBubble(BgtaManager m, String username)
   {
      this(m.startChat(username));
      man = m;
   }

   private BgtaResourceSwitchingBubble(BgtaChat c)
   {
      super(c);
      chat = c;
   }

   @Override public void insertUpdate(DocumentEvent e)
   {
      super.insertUpdate(e);
      Message msg = (Message)chat.getLastMessage();
      if(msg == null)
      {
	 return;
      }
      try {
	 if(!msg.getFrom().equals(chat.getChat().getParticipant()))
	 {
	    chat.setChat(man.startChat(msg.getFrom()).getChat());
	 }
      } catch (OperationNotSupportedException e1) {
	 // Do nothing
      }
   }

}
