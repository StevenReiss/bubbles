/********************************************************************************/
/*										*/
/*		StudentXMPPBotTest.java 					*/
/*										*/
/*	Bubbles for education							*/
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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.brown.cs.bubbles.bedu.chat.BeduCourse.TACourse;
import edu.brown.cs.bubbles.bgta.BgtaChat;
import edu.brown.cs.bubbles.bgta.BgtaUtil;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;


public class BeduTAXMPPClientTest {
private static BeduTAXMPPClient  ta_client;
private static BeduTAXMPPClient  ta_client2;
private static BeduTAXMPPClient  ta_client3;
private static XMPPConnection	 student_conn1;
private static XMPPConnection	 student_conn2;

// login names
private static String	    ta_login	   = "codebubbles@jabber.org";
private static String	    student_login  = "codebubbles2";
private static String	    student2_login = "codebubbles3";

private static PipedOutputStream pipe_err;

// result bools for the routing test that have
// to be members so we can set them inside
// anonymous message listeners
private boolean 	  t2ToS1_received = false;
private boolean 	  t1ToS2_received = false;
private BgtaChat		 bs1_to_T2;
private BgtaChat		 bs2_to_T1;


@BeforeClass public static void setUpOnce() throws XMPPException
{
   try {
//    BeduChatFactory.DEBUG = true;
      ta_client = new BeduTAXMPPClient(new TACourse("testcourse",ta_login,"brownbears",
						       "jabber.org"));
      ta_client.connectAndLogin("TA1");

      ta_client2 = new BeduTAXMPPClient(new TACourse("testcourse",ta_login,"brownbears",
							"jabber.org"));
      // ta_client2.connectAndLogin("TA2");

      ta_client3 = new BeduTAXMPPClient(new TACourse("testcourse",ta_login,"brownbears",
							"jabber.org"));

      XMPPConnection.DEBUG_ENABLED = true;
      ConnectionConfiguration config = new ConnectionConfiguration("jabber.org",5222);
      config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
      config.setSendPresence(true);

      student_conn1 = new XMPPConnection(config);
      student_conn1.connect();
      student_conn1.login(student_login, "brownbears");
    }
   catch (Throwable e) { }
}


@AfterClass public static void staticTearDown() throws XMPPException
{
   if (student_conn1 != null) student_conn1.disconnect();
   if (ta_client != null) ta_client.disconnect();
   if (ta_client2 != null) ta_client2.disconnect();
   if (ta_client3 != null) ta_client3.disconnect();
}


/**
 * Tests the ability to receive a ticket via chat and store it as a
 * StudentTicket
 */
@Test public void testTicketReceiveAndAccept() throws XMPPException
{
   assertTrue(ta_client != null);
   assertTrue(student_conn1 != null);

   System.out.println("Testing ticket receipt");
   Chat c = student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1",
	    new MessageListener() {
	       @Override public void processMessage(Chat ch,Message m)
	       {

	       }
	    });

   assertTrue(ta_client.getTickets().size() == 0);
   try {
      Thread.sleep(2000);
   }
   catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
   }
   c.sendMessage("TICKET:this is a ticket");
   try {
      Thread.sleep(1000);
   }
   catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
   }

   assertTrue(ta_client.getTickets().size() == 1);
   BeduStudentTicket t = ta_client.getTickets().get(0);
   assertEquals(t.getText(), "this is a ticket");

   BgtaChat bc = ta_client.acceptTicketAndAlertPeers(t);
   ta_client.endChatSession(bc);
   assertTrue(ta_client.getTickets().size() == 0);
}

@Test public void testTicketForwardAndAccept() throws XMPPException, InterruptedException
{
   assertTrue(ta_client != null);
   assertTrue(ta_client2 != null);
   assertTrue(student_conn1 != null);

   System.out.println("Testing forward and accept");
   Chat c = student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", null);
   ta_client2.connectAndLogin("TA2");

   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);

   c.sendMessage("TICKET:tick");
   Thread.sleep(5000);
   assertTrue(ta_client.getTickets().size() == 1);
   assertTrue(ta_client2.getTickets().size() == 1);
   assertEquals(ta_client.getTickets().get(0), ta_client2.getTickets().get(0));

   BgtaChat bc = ta_client2.acceptTicketAndAlertPeers(ta_client2.getTickets().get(0));
   Thread.sleep(5000);
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);
   ta_client2.endChatSession(bc);

}

/**
 * Tests the forwarding of all active tickets upon the login of another TA
 * @throws XMPPException
 * @throws InterruptedException
 */
@Test public void testInitialTicketForwards() throws XMPPException, InterruptedException
{
   assertTrue(ta_client != null);
   assertTrue(ta_client3 != null);
   assertTrue(student_conn1 != null);

   System.out.println("Testing initial ticket forwarding...");

   Chat c = student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", null);

   assertEquals(0, ta_client.getTickets().size());
   assertEquals(0, ta_client3.getTickets().size());
   c.sendMessage("TICKET:1");
   c.sendMessage("TICKET:2");

   Thread.sleep(3000);
   assertEquals(2, ta_client.getTickets().size());
   assertEquals(0, ta_client3.getTickets().size());

   ta_client3.connectAndLogin("TA3");
   Thread.sleep(3000);
   assertEquals(2, ta_client.getTickets().size());
   assertEquals(2, ta_client3.getTickets().size());
   BgtaChat bc1 = ta_client.acceptTicketAndAlertPeers(ta_client.getTickets().get(0));
   BgtaChat bc3 = ta_client3.acceptTicketAndAlertPeers(ta_client.getTickets().get(0));
   Thread.sleep(3000);
   assertEquals(0, ta_client.getTickets().size());
   assertEquals(0, ta_client3.getTickets().size());
   ta_client.endChatSession(bc1);
   ta_client3.endChatSession(bc3);

}

/**
 * Tests the methodology used to make sure
 * that chats are setup appropriately
 * Note that this does not test the actual code because it's difficult to divorce from the UI so
 * manual testing is required
 * @throws Exception
 */
@Test public void testMessageRouting() throws Exception
{
   assertTrue(ta_client != null);
   assertTrue(ta_client2 != null);
   assertTrue(student_conn1 != null);
   assertTrue(student_conn2 != null);

   /**
    * With 2 TAs and 2 student connections have the two students
    * send tickets to different TAs, have TAs accept tickets from students
    * from whom they didn't receive the intial ticket and then try to send messages
    * in both directions once the sessions are established and check if the
    * messages reach their destinations
    */
   System.out.println("Testing correct routing of messages");
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);

   ConnectionConfiguration config = new ConnectionConfiguration("jabber.org",5222);
   config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
   config.setSendPresence(true);

   student_conn2 = new XMPPConnection(config);
   student_conn2.connect();
   student_conn2.login(student2_login, "brownbears");


   Chat s1ToT2 = student_conn1.getChatManager().createChat("codebubbles@jabber.org",
	    new MessageListener() {
	       @Override public void processMessage(Chat c,Message m)
	       {
		  if (m.getBody().equals("t2ToS1")) t2ToS1_received = true;
	       }
	    });

   Chat s2ToT1 = student_conn2.getChatManager().createChat("codebubbles@jabber.org",
	    new MessageListener() {
	       @Override public void processMessage(Chat c,Message m)
	       {
		  if (m.getBody().equals("t1ToS2")) t1ToS2_received = true;
	       }
	    });

   bs1_to_T2 = BgtaUtil.bgtaChatForXMPPChat(student_conn1, s1ToT2);
   bs2_to_T1 = BgtaUtil.bgtaChatForXMPPChat(student_conn2, s2ToT1);

   // unforunately it isn't possible to test proper message routing without starting
   // the whole UI so here I'm testing the general methodology I used to make sure
   // the ideas are correct but this doesn't test the actual resource switching code
   // which is inside BgtaResourceSwitchingBubble
   // actually testing that code has to be done by hand

   bs1_to_T2.getDocument().addDocumentListener(new DocumentListener() {

      @Override public void changedUpdate(DocumentEvent e)
      {
      // TODO Auto-generated method stub

      }

      @Override public void insertUpdate(DocumentEvent e)
      {
	 String newDest = ((Message) bs1_to_T2.getLastMessage()).getFrom();
	 bs1_to_T2 = BgtaUtil.bgtaChatForXMPPChat(student_conn1, student_conn1
		  .getChatManager().createChat(newDest, null));

      }

      @Override public void removeUpdate(DocumentEvent e)
      {
      // TODO Auto-generated method stub

      }
   });

   bs2_to_T1.getDocument().addDocumentListener(new DocumentListener() {

      @Override public void changedUpdate(DocumentEvent e)
      {
      // TODO Auto-generated method stub

      }

      @Override public void insertUpdate(DocumentEvent e)
      {
	 String newDest = ((Message) bs2_to_T1.getLastMessage()).getFrom();
	 bs2_to_T1 = BgtaUtil.bgtaChatForXMPPChat(student_conn2, student_conn2
		  .getChatManager().createChat(newDest, null));

      }

      @Override public void removeUpdate(DocumentEvent e)
      {
      // TODO Auto-generated method stub

      }

   });


   student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", null)
	    .sendMessage("TICKET:s1ToT2"); // hashcode = -954750633
   Thread.sleep(1000);
   student_conn2.getChatManager().createChat("codebubbles@jabber.org/TA2", null)
	    .sendMessage("TICKET:s2ToT1"); // hash = -953827113

   Thread.sleep(3000);
   assertTrue(ta_client.getTickets().size() == 2);
   assertTrue(ta_client2.getTickets().size() == 2);

   BgtaChat t1ToS2 = ta_client.acceptTicketAndAlertPeers(new BeduStudentTicket("s2ToT1",
	    new Date(),student2_login + "@jabber.org"));
   BgtaChat t2ToS1 = ta_client2.acceptTicketAndAlertPeers(new BeduStudentTicket("s1ToT2",
	    new Date(),student_login + "@jabber.org"));

   Thread.sleep(3000);
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);

   pipe_err = new PipedOutputStream();
   PipedInputStream pipeIn = new PipedInputStream(pipe_err);
   System.setErr(new PrintStream(pipe_err));


   t2ToS1.sendMessage("t2ToS1");
   Thread.sleep(1000);
   t1ToS2.sendMessage("t1ToS2");
   Thread.sleep(1000);
   bs1_to_T2.sendMessage("s1toT2");
   Thread.sleep(2000);
   bs2_to_T1.sendMessage("s2toT1");
   Thread.sleep(1000);

   assertTrue(t2ToS1_received);
   assertTrue(t1ToS2_received);


   ta_client.endChatSession(t1ToS2);
   ta_client.disconnect();
   /**
    * Now lets have the higher priority TA log out and see if the messages to the other one still get through
    */

   byte[] errbuf = new byte[300];

   Thread.sleep(1000);
   pipeIn.read(errbuf);
   pipeIn.close();
   HashSet<String> prints = new HashSet<String>();
   String[] arprints = new String(errbuf).trim().split("\\r?\\n");
   prints.add(arprints[0]);
   prints.add(arprints[1]);

   assertTrue(prints
	    .contains("BEDU:codebubbles@jabber.org/TA2:Student message:codebubbles2@jabber.org:s1toT2"));
   assertTrue(prints
	    .contains("BEDU:codebubbles@jabber.org/TA1:Student message:codebubbles3@jabber.org:s2toT1"));

   ta_client2.endChatSession(t2ToS1);
}

}
