/********************************************************************************/
/*                         							*/
/*    		BeduChatBubble.java                 				*/
/*                            			     	  			*/
/* 	Bubbles for Education   						*/
/* 	A chat bubble that can be constructed from basic smack			*/
/* 	classes for use in BeduChat			      			*/
/* 				               					*/
/********************************************************************************/
/* 	Copyright 2011 Brown University -- Andrew Kovacs      			*/
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

package edu.brown.cs.bubbles.bedu.chat;


import edu.brown.cs.bubbles.bgta.BgtaBubble;
import edu.brown.cs.bubbles.bgta.BgtaChat;


class BeduTAChatBubble extends BgtaBubble {
private static final long serialVersionUID = 1L;

private BeduTAXMPPClient  my_client;
private BgtaChat	  my_chat;

BeduTAChatBubble(BeduTAXMPPClient a_client,BgtaChat a_chat)
{
   super(a_chat);
   my_client = a_client;
   my_chat = a_chat;
}


@Override public void setVisible(boolean vis)
{
   // when this chat window is closed make sure to cut
   // off the student
   super.setVisible(vis);
   my_client.endChatSession(my_chat);
}
}
