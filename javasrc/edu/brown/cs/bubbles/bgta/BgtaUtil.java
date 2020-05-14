/********************************************************************************/
/*										                                                  */
/*		BgtaUtil.java							                                         */
/*										                                                  */
/*	Chat utility functions                                  		                 */
/*										                                                  */
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs      		                 */
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
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bgta.BgtaConstants.ChatServer;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BgtaUtil implements BassConstants{

   /**
    * Returns a list of XMPP resources that are logged into the bare JID
    * at the given RosterEntry
    */
   public static List<String> getFullJIDsForRosterEntry(Roster r, String bare_jid)
   {
      ArrayList<String> jids = new ArrayList<String>();
      Iterator<Presence> it = r.getPresences(bare_jid);
      while(it.hasNext()){
         Presence p = it.next();
         jids.add(p.getFrom());
      }
      return jids;   
   }
   
   /**
    * Provides a BgtaChat given enough Smack data 
    * @param conn
    * @param c
    * @return
    */
   public static BgtaChat bgtaChatForXMPPChat(XMPPConnection conn, Chat c)
   {
      return new BgtaChat(conn.getUser(), c.getParticipant(), null, ChatServer.fromServer(conn.getServiceName()), c, null);
   }
   
   /**
    * Returns the bubble normally accessed by clicking "Manage accounts" on the Repository
    * @return
    */
   public static BudaBubble getLoginBubble()
   {
      for(BassName n : BassFactory.getRepository(BudaConstants.SearchType.SEARCH_PEOPLE).getAllNames())
      {
         if(n.getNameType() == BassNameType.CHAT_LOGIN)
         {
            return n.createBubble();
         }
      }
      
      return null;
   }
   
   /**
    * Returns only the BgtaManagers attached to XMPP managers 
    * @return
    */
   public static Collection<BgtaManager> getXMPPManagers()
   {
      ArrayList<BgtaManager> l = new ArrayList<>();
      for (Iterator<BgtaManager> it = BgtaFactory.getManagers(); it.hasNext(); ) {
         BgtaManager man = it.next();
         if (man.getServer() != ChatServer.AIM)
            l.add(man);
       }
      
      return l;
   }
   

}
