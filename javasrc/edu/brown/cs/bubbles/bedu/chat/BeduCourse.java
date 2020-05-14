/********************************************************************************/
/*										*/
/*		BeduCourse.java 						*/
/*										*/
/*	Bubbles for Education							*/
/*	Represents a school course						*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs			*/
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

package edu.brown.cs.bubbles.bedu.chat;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.bgta.BgtaLoginBubble;
import edu.brown.cs.bubbles.bgta.BgtaManager;
import edu.brown.cs.bubbles.bgta.BgtaUtil;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaErrorBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

import org.jivesoftware.smack.packet.Presence;

import javax.swing.Icon;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.InetAddress;


abstract class BeduCourse extends BassNameBase {

private String course_name;
private String ta_chat_jid;


protected BeduCourse(String a_course_name,String a_jid)
{
   course_name = a_course_name;
   ta_chat_jid = a_jid;
}


@Override protected String getParameters()
{
   return null;
}


@Override public String getProject()
{
   return null;
}


@Override protected String getKey()
{
   return course_name;
}

@Override public String toString()
{
   return course_name;
}


static class TACourse extends BeduCourse {
private BeduTATicketList ticket_list;
private String	   xmpp_password;
private String	   xmpp_server;
private BeduTAXMPPClient my_client;


protected TACourse(String cn,String jid,String pwd,String srv)
{
   super(cn,jid);
   ticket_list = new BeduTATicketList();
   xmpp_password = pwd;
   xmpp_server = srv;
}


BeduTATicketList getTicketList()
{
   return ticket_list;
}


@Override public BudaBubble createBubble()
{
   my_client = BeduChatFactory.getTAXMPPClientForCourse(this);

   try {
      if (!my_client.isLoggedIn()) {
	 my_client.connectAndLogin(InetAddress.getLocalHost().getHostName());
      }
   }
   catch (Exception e) {
      return new BudaErrorBubble("Error logging in: " + e.getMessage());
   }

   return new BeduTATicketListBubble(my_client.getTickets(),my_client);
}


@Override protected String getSymbolName()
{
   return BassConstants.BASS_COURSE_LIST_NAME + ".Enable " + getCourseName()
	    + " chat hours";
}


@Override public Icon getDisplayIcon()
{
   return BoardImage.getIcon("contents_view");
}


String getXMPPServer()
{
   return xmpp_server;
}


String getXMPPPassword()
{
   return xmpp_password;
}
}


static class StudentCourse extends BeduCourse {

private String ta_chat_jid;


StudentCourse(String a_course_name,String a_jid)
{
   super(a_course_name,a_jid);
   ta_chat_jid = a_jid;
}


@Override public BudaBubble createBubble()
{
   if (BgtaUtil.getXMPPManagers().size() == 0) {
      BgtaLoginBubble b = (BgtaLoginBubble) BgtaUtil.getLoginBubble();
      b.setErrorMessage("Login to GMail/Brown/Jabber");

      b.addComponentListener(new ComponentListener() {

	 @Override public void componentHidden(ComponentEvent e)
	 {
	    BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(e.getComponent());
	    // this is called when the login window has exited
	    if (BgtaUtil.getXMPPManagers().size() > 0) {
	       try {
		  Thread.sleep(1000);
	       }
	       catch (InterruptedException e1) {
		  e1.printStackTrace();
	       }
	       bba.addBubble(new BeduStudentTicketSubmitBubble(ta_chat_jid), e
			.getComponent().getX() - 100, e.getComponent().getY());
	    }
	    else {

	       bba.addBubble(createBubble(), e.getComponent().getX(), e.getComponent()
			.getY());
	    }
	 }

	 @Override public void componentMoved(ComponentEvent e)
	 {
	 // TODO Auto-generated method stub

	 }

	 @Override public void componentResized(ComponentEvent e)
	 {
	 // TODO Auto-generated method stub

	 }

	 @Override public void componentShown(ComponentEvent e)
	 {
	 // TODO Auto-generated method stub

	 }

      });

      return b;
   }

   return new BeduStudentTicketSubmitBubble(ta_chat_jid);
}


@Override protected String getSymbolName()
{
   return BassConstants.BASS_COURSE_LIST_NAME + ".Get " + getCourseName() + " help";
}


@Override public Icon getDisplayIcon()
{
   if (BgtaUtil.getXMPPManagers().size() >= 0) {
      Presence p = BgtaManager.getPresence(ta_chat_jid);
      if (p != null) {
	 if (p.getType() == Presence.Type.available) return BgtaManager
		  .iconFor(new Presence(Presence.Type.available));
	 return BgtaManager.iconFor(new Presence(Presence.Type.unavailable));
      }
   }
   return BoardImage.getIcon("question");
}
}


/**
 * Returns the JID of the TAs for this course
 * @return
 */
String getTAJID()
{
   return ta_chat_jid;
}


String getCourseName()
{
   return course_name;
}


@Override public boolean equals(Object o)
{
   if (o instanceof BeduCourse) {
      BeduCourse c = (BeduCourse) o;
      return (course_name.equals(c.course_name) && ta_chat_jid.equals(c.ta_chat_jid));
   }
   else return false;
}
}
