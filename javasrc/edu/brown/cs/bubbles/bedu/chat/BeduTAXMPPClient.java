/********************************************************************************/
/*										*/
/*		BeduTAXMPPClient.java						*/
/*    Bubbles for Education							*/
/*										*/
/* This class implements an XMPP chat bot that is used to			*/
/*    facilitate help requests and chatting between students			*/
/*    and TAs in a course							*/
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

import edu.brown.cs.bubbles.bgta.BgtaChat;
import edu.brown.cs.bubbles.bgta.BgtaUtil;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class implements the bulk of the system of ticket handling
 * and chatting with students.
 *
 * TAs all login to the same XMPP account, but they all use different
 * resource names, currently the resource name is the hostname of the machine
 * (this means it is problematic to for someone to log into the same course
 * account as a TA on multiple instances on the same machine).
 *
 * Once logged in TA clients implement the following protocol:
 * ACCEPTING:<string hash>			Sent to other TAs to alert them that the given client is
 *						accepting the ticket with the given hash of its text string.
 *						Other clients are expected to remove the ticket from their
 *						lists of open tickets
 *
 * REQUEST-TICKETS				Send to another TA to request all of its tickets. This is
 *						used upon login to find out about all the tickets that
 *						have been submitted in the time before the client logged in
 *
 * TICKET-FORWARD:<student jid>:<ticket text>	Sent to another TA to alert that client about a ticket that
 *						the given client knows about. This is called every time a client
 *						receives a ticket from a student (because the priorities are arranged
 *						such that only the longest logged in TA will receive the 
 *                                              ticket messages).
 *						TAs who receive this should add the ticket to their lists.
 *
 * TICKET:<ticket text> 			Sent by students to the TA with the highest priority. This TA client
 *						should add the ticket to its list and forward the ticket to 
 *                                              the other TAs
 *
 * @author akovacs
 *
 */

class BeduTAXMPPClient {


private ConnectionConfiguration xmpp_config;
private XMPPConnection	  xmpp_conn;

private String		  resource_name;
private BeduCourse.TACourse	my_course;
private Map<String, BeduTAChat> chats_map;	 // maps jids to Chat objects
private Set<String>	     permitted_jids;

private BeduTATicketList	ticket_list;


BeduTAXMPPClient(BeduCourse.TACourse course)
{
   if (BeduChatFactory.DEBUG) XMPPConnection.DEBUG_ENABLED = true;
   chats_map = new HashMap<String, BeduTAChat>();
   permitted_jids = new HashSet<String>();
   my_course = course;
   xmpp_config = new ConnectionConfiguration(my_course.getXMPPServer());
   xmpp_config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
   xmpp_config.setSendPresence(true);
   ticket_list = my_course.getTicketList();
   
   xmpp_conn = new XMPPConnection(xmpp_config);
}


BeduCourse getCourse()
{
   return my_course;
}


/**
 * Connect the bot to the xmpp service
 * with the given name as the resource name
 * @throws XMPPException
 */
void connectAndLogin(String name) throws XMPPException
{
   resource_name = name;
   xmpp_conn.connect();
   xmpp_conn.login(my_course.getTAJID().split("@")[0], my_course.getXMPPPassword(), resource_name);
   
   
   xmpp_conn.getChatManager().addChatListener(new ChatManagerListener() {
      @Override public void chatCreated(Chat c,boolean createdLocally) {
	 if (BeduChatFactory.DEBUG) System.out.println("Chat created with: " +
               c.getParticipant());
	 c.addMessageListener(new StudentXMPPBotMessageListener());
       }
    });
   
   
   xmpp_conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
   try {
      Thread.sleep(1000);
    }
   catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
   ArrayList<String> myfulljids = new ArrayList<>();
   
   int minPriority = 128;
   for (Iterator<Presence> it = xmpp_conn.getRoster().getPresences(getMyBareJID()); it
         .hasNext(); ) {
      Presence p = it.next();
      myfulljids.add(p.getFrom());
      if (p.getPriority() < minPriority && p.getPriority() >= -128) {
	 minPriority = p.getPriority();
       }
    }
   
   Presence availp = new Presence(Presence.Type.available,"Answering questions",
         minPriority - 1,Presence.Mode.available);
   xmpp_conn.sendPacket(availp);
   
   if (!xmpp_conn.getRoster().contains(getMyBareJID())) {
      Packet p = new Presence(Presence.Type.subscribe);
      p.setTo(getMyBareJID());
      xmpp_conn.sendPacket(p);
    }
   
   boolean foundFull = false;
   for (String fulljid : myfulljids) {
      if (!StringUtils.parseResource(fulljid).equals(name)) {
	 xmpp_conn.getChatManager().createChat(fulljid, new StudentXMPPBotMessageListener())
            .sendMessage("REQUEST-TICKETS");
	 foundFull = true;
	 break;
       }
    }
   
   if (!foundFull) {
      xmpp_conn.getChatManager().createChat(getMyBareJID(),
            new StudentXMPPBotMessageListener()).sendMessage("REQUEST-TICKETS");
    }
   
}


boolean isLoggedIn()
{
   return xmpp_conn.isAuthenticated();
}


void disconnect()
{
   xmpp_conn.disconnect();
}


/**
 * End the current chat session with a student and ignore further messages from
 * the student until another ticket is accepted
 */
void endChatSession(BgtaChat c)
{
   permitted_jids.remove(c.getUsername());
}

/**
 * Opens a chat sesion with the student who subbmitted the
 * given ticket and returns a BgtaChat with the student
 * @param t
 * @return
 */
BgtaChat acceptTicketAndAlertPeers(BeduStudentTicket t)
{
   sendMessageToOtherResources("ACCEPTING:" + t.textHash());
   ticket_list.remove(t);
   BeduTAChat c = new BeduTAChat(xmpp_conn,xmpp_conn.getChatManager().createChat(t.getStudentJID(),
         null));
   chats_map.put(t.getStudentJID(), c);
   permitted_jids.add(t.getStudentJID());
   return c;
}


BeduTATicketList getTickets()
{
   return ticket_list;
}


XMPPConnection getConnection()
{
   return xmpp_conn;
}

/**
 * Sends a message to the other TAs (other resources on the same account)
 * @param msg
 */
private void sendMessageToOtherResources(String msg)
{
   for (String fulljid : BgtaUtil.getFullJIDsForRosterEntry(xmpp_conn.getRoster(),
         getMyBareJID())) {
      if (StringUtils.parseResource(fulljid).equals(resource_name)) continue;
      Chat othertchat = xmpp_conn.getChatManager().createChat(fulljid,
            new MessageListener() {
         @Override public void processMessage(Chat c,Message m)
      {
            // do nothing
          }
       });
      
      try {
	 othertchat.sendMessage(msg);
       }
      catch (XMPPException e) {
	 // TODO Auto-generated catch block
	 e.printStackTrace();
       }
    }
}


private String getMyBareJID()
{
   return StringUtils.parseBareAddress(xmpp_conn.getUser());
}

private final class StudentXMPPBotMessageListener implements MessageListener {
   @Override public void processMessage(Chat c,Message m)
{
      if (BeduChatFactory.DEBUG) System.out.println(xmpp_conn.getUser() + "  received message: " +
	    m.getBody() + " from " + c.getParticipant());
      
      String[] chatargs = m.getBody().split(":");
      String cmd = chatargs[0];
      
      if (cmd.equals("TICKET")) {
         // comes in the form "TICKET:<message>"
         BeduStudentTicket t = new BeduStudentTicket(chatargs[1],new Date(System
	       .currentTimeMillis()),m.getFrom());
         ticket_list.add(t);
         try {
            c.sendMessage("Ticket received. A TA will respond soon.");
          }
         catch (XMPPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
         sendMessageToOtherResources("TICKET-FORWARD:" + m.getFrom() + ":" + chatargs[1]);
       }
      
      else if (cmd.equals("TICKET-FORWARD")) {
         // comes in the form "TICKET-FORWARD:<student-jid>:<message>"
         BeduStudentTicket t = new BeduStudentTicket(chatargs[2],new Date(System
	       .currentTimeMillis()),chatargs[1]);
         if (!ticket_list.contains(t)) ticket_list.add(t);
       }
      
      else if (cmd.equals("ACCEPTING")) {
         // form: "ACCEPTING:<string hash>"
         
         int hash = Integer.valueOf(chatargs[1]);
         for (BeduStudentTicket t : ticket_list) {
            if (t.textHash() == hash) {
               ticket_list.remove(t);
               break;
             }
          }
       }
      
      else if (cmd.equals("REQUEST-TICKETS")) {
         for (BeduStudentTicket t : ticket_list) {
            Chat tc = xmpp_conn.getChatManager().createChat(m.getFrom(),
		  new StudentXMPPBotMessageListener());
            try {
               tc.sendMessage("TICKET-FORWARD:" + t.getStudentJID() + ":" + t.getText());
               try {
                  Thread.sleep(100);
                }
               catch (InterruptedException e) {
                  e.printStackTrace();
                }
             }
            catch (XMPPException e) {
               // other guy probably logged off so it doesn't matter
               // otherwise we did, there should be another alert if we did
             }
          }
       }
      else if (permitted_jids.contains(c.getParticipant())) {
         chats_map.get(c.getParticipant()).logOutsideMessage(m.getBody());
         if (BeduChatFactory.DEBUG) System.err.println("BEDU:" + xmpp_conn.getUser() +
	       ":Student message:" + c.getParticipant() + ":" + m.getBody());
       }
      else if (m.getBody().equals("Please submit a ticket to chat with a TA")) {
         System.err.println("BEDU: Error TA msg bounced back");
       }
      else {
         try {
            c.sendMessage("Please submit a ticket to chat with a TA");
          }
         catch (XMPPException e) {
            // this exception doesn't really matter because this
            // person shouldn't be chatting with us anyway
          }
       }
    }
}

private static class BeduTAChat extends BgtaChat {
   
   private String newest_msg;
   
   BeduTAChat(XMPPConnection conn,Chat c) {
      super(conn.getUser(),c.getParticipant(),null,ChatServer.fromServer(
            conn.getServiceName()),c,null);
      newest_msg = "";
    }
   
   void logOutsideMessage(String msg) {
      if (!msg.equals(newest_msg)) logMessage(msg);
      
      newest_msg = msg;
    }
   
}	// end of inner class BeduTAChat



}	// end of class BeduTAXMPPClient



/* end of BedutTAXMPPClient.java */
