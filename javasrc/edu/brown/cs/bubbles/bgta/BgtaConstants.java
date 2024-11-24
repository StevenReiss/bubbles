/********************************************************************************/
/*                                                                              */
/*              BgtaConstants.java                                              */
/*                                                                              */
/*      Bubbles attribute and property management main setup routine            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2009 Brown University -- Ian Strickman                      */
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

import edu.brown.cs.bubbles.bass.BassConstants;

import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;




interface BgtaConstants extends BassConstants {



/********************************************************************************/
/*                                                                              */
/*      Constants for gradient background                                       */
/*                                                                              */
/********************************************************************************/

String BGTA_BUBBLE_TOP_COLOR_PROP = "Bgta.BubbleTopColor"; 

String BGTA_BUBBLE_BOTTOM_COLOR_PROP = "Bgta.BubbleBottomColor";

String BGTA_ALT_BUBBLE_TOP_COLOR_PROP = "Bgta.AltBubbleTopColor"; 

String BGTA_ALT_BUBBLE_BOTTOM_COLOR_PROP = "Bgta.AltBubbleBottomColor";




/********************************************************************************/
/*                                                                              */
/*      Constants for properties                                                */
/*                                                                              */
/********************************************************************************/

String  BGTA_NUM_ACCOUNTS           = "Bgta.mans.num";

String  BGTA_USERNAME_PREFIX     = "Bgta.mans.username.";

String  BGTA_PASSWORD_PREFIX     = "Bgta.mans.password.";

String  BGTA_SERVER_PREFIX         = "Bgta.mans.server.";

String  BGTA_ALT_COLOR_UPON_RECEIVE  = "Bgta.altcolor";



/********************************************************************************/
/*                                                                              */
/*      Constants for sort priority                                             */
/*                                                                              */
/********************************************************************************/

int     BGTA_CHATTY_PRIORITY     = BASS_DEFAULT_SORT_PRIORITY - 8;

int     BGTA_AVAIL_PRIORITY       = BASS_DEFAULT_SORT_PRIORITY - 7;

int     BGTA_IDLE_PRIORITY         = BASS_DEFAULT_SORT_PRIORITY - 6;

int     BGTA_XA_PRIORITY             = BASS_DEFAULT_SORT_PRIORITY - 5;

int     BGTA_DND_PRIORITY           = BASS_DEFAULT_SORT_PRIORITY - 4;

int     BGTA_OFFLINE_PRIORITY   = BASS_DEFAULT_SORT_PRIORITY - 3;

int     BGTA_SPEC_ACCOUNT_PRIORITY   = BASS_DEFAULT_SORT_PRIORITY - 2;

int     BGTA_GEN_ACCOUNT_PRIORITY    = BASS_DEFAULT_SORT_PRIORITY - 1;



/********************************************************************************/
/*                                                                              */
/*      Constants for remembering account information                           */
/*                                                                              */
/********************************************************************************/

boolean BGTA_INITIAL_REM_SETTING     = false;



/********************************************************************************/
/*                                                                              */
/*      Constants for Buddy List Prefices                                       */
/*                                                                              */
/********************************************************************************/

String  BGTA_BUDDY_PREFIX           = "@people.";



/********************************************************************************/
/*                                                                              */
/*      Constants for Logging area                                              */
/*                                                                              */
/********************************************************************************/

int     BGTA_LOG_WIDTH         = 275;

int     BGTA_LOG_HEIGHT       = (int) (0.75 * BGTA_LOG_WIDTH);

int     BGTA_DATA_BUTTON_WIDTH       = 110;

int     BGTA_DATA_BUTTON_HEIGHT      = 15;



/********************************************************************************/
/*                                                                              */
/*      Constants for task loading                                              */
/*                                                                              */
/********************************************************************************/

String  BGTA_TASK_DESCRIPTION   =
   "To open the new data, right click on the top bar and choose it from the list of tasks.\n";



/********************************************************************************/
/*                                                                              */
/*      Enum for chat_server values                                             */
/*                                                                              */
/********************************************************************************/

enum ChatServer {

   GMAIL("Gmail", "gmail.com", "@gmail.com", "talk.google.com", true),
   BROWN("Brown Gmail", "gmail.com", "@brown.edu", "talk.google.com", true),
   FACEBOOK("Facebook", "chat.facebook.com", " ", "", false),
   JABBER("Jabber", "jabber.org", "@jabber.org", "", false),
   AIM("AIM", "aim", " ", "", false);

   private String chat_selector;
   private String chat_server;
   private String chat_display;
   private String chat_host;
   private boolean chat_ending;

   ChatServer(String selector,String server,String display,String host, boolean ending) {
      chat_selector = selector;
      chat_server = server;
      if (display.equals(""))
         chat_display = server;
      else
         chat_display = display;
      if (host.equals(""))
         chat_host = server;
      else
         chat_host = host;
      chat_ending = ending;
    }

   String selector()                            { return chat_selector; }
   String server()                              { return chat_server; }
   String display()                             { return chat_display; }
   String host()                                { return chat_host; }

   boolean hasEnding()                          { return chat_ending; }

   String ending() {
      if (chat_ending) return chat_display;
      return "";
    }

   public static ChatServer fromServer(String server) {
      for (ChatServer s : values()) {
         if (server.equals(s.server())) return s;
       }
      return null;
    }

   @Override public String toString() { return chat_selector + " - " + chat_display; }

}       // end of enum ChatServer


/********************************************************************************/
/*                                                                              */
/*      Interfaces for XMPP and OSCAR compatibility                             */
/*                                                                              */
/********************************************************************************/

interface BgtaRoster {

   BgtaRosterEntry getEntry(String username);
   Collection<? extends BgtaRosterEntry> getEntries();
   Presence getPresence(String username);

}   // end of inner interface BgtaRoster



interface BgtaRosterEntry {

   String getName();
   String getUser();

}   // end of inner interface BgtaRosterEntry



}       // end of inner interface BgtaConstants




/* end of BgtaConstants.java */
