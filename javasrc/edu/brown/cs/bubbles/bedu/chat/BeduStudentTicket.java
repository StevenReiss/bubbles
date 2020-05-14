/********************************************************************************/
/*										*/
/*		BeduStudentTicket.java	 	                               	*/
/*		Class defining a ticket for help submitted by  a student	*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs			*/
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

import java.util.Date;


class BeduStudentTicket {
private String ticket_text;
private Date   time_stamp;
private String student_jid;


BeduStudentTicket(String txt,Date time,String jid)
{
   ticket_text = txt;
   time_stamp = time;
   student_jid = jid;
}


/**
 * Hashcode of the text contained in the ticket
 * @return
 */
int textHash()
{
   return ticket_text.hashCode();
}


String getText()
{
   return ticket_text;
}


/**
 * The time the ticket was received 
 * @return
 */
Date getTimestamp()
{
   return time_stamp;
}


/**
 * The XMPP JID of the student
 * who submitted the ticket
 * @return
 */
String getStudentJID()
{
   return student_jid;
}


@Override public int hashCode()
{
   final int prime = 31;
   int result = 1;
   result = prime * result + ((ticket_text == null) ? 0 : ticket_text.hashCode());
   return result;
}


@Override public boolean equals(Object obj)
{
   if (this == obj) return true;
   if (obj == null) return false;
   if (getClass() != obj.getClass()) return false;
   BeduStudentTicket other = (BeduStudentTicket) obj;
   if (ticket_text == null) {
      if (other.ticket_text != null) return false;
   }
   else if (!ticket_text.equals(other.ticket_text)) return false;
   return true;
}

}
