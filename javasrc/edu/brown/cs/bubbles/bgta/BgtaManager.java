/********************************************************************************/
/*										*/
/*		BgtaManager.java						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
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

import edu.brown.cs.bubbles.bgta.BgtaConstants.BgtaRoster;
import edu.brown.cs.bubbles.bgta.BgtaConstants.BgtaRosterEntry;
import edu.brown.cs.bubbles.bgta.BgtaConstants.ChatServer;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.text.Document;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;






public class BgtaManager implements PacketListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private XMPPConnection		   the_connection;
private static XMPPConnection	   stat_con;
private RosterListener		   roster_listener;
private BgtaRepository		   the_repository;

protected String		   user_name;
protected String		   user_password;
protected ChatServer		   user_server;
protected boolean		   being_saved;
protected Vector<BgtaBubble>	   existing_bubbles;
protected Map<String, BgtaChat>    existing_chats;
protected Map<String, Document>    existing_docs;
protected BgtaRoster		   the_roster;




/********************************************************************************/
/*										*/
/*	Factory Method								*/
/*										*/
/********************************************************************************/

/**
 * Returns the proper manager type depending on the ChatServer.
 */
static BgtaManager getManager(String username,String password,ChatServer server,BgtaRepository repo)
{
   if (server.equals(ChatServer.AIM))
       return new BgtaAimManager(username,password,server);
   
   return new BgtaManager(username,password,server,repo);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaManager(String username,String password,ChatServer
server,BgtaRepository repo)
{
   this(username,password,server);
   the_repository = repo;
}

BgtaManager(String username,String password,ChatServer server)
{
   user_name = username;
   user_password = password;
   user_server = server;
   if (user_server == null)
      user_server = ChatServer.GMAIL;
   existing_bubbles = new Vector<BgtaBubble>();
   existing_chats = new HashMap<String, BgtaChat>();
   existing_docs = new HashMap<String, Document>();
   being_saved = false;
   roster_listener = null;
}



/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

BgtaRoster getRoster()			   { return the_roster; }

public String getUsername()				   { return user_name; }

String getPassword()				   { return user_password; }

ChatServer getServer()				   { return user_server; }

boolean isBeingSaved()		 { return being_saved; }

void setBeingSaved(boolean bs)	 { being_saved = bs; }

boolean isLoggedIn()
{
   if (the_connection == null)
       return false;
   return the_connection.isConnected() && the_connection.isAuthenticated();
}



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

boolean propertiesMatch(String un,String se)
{
   return un.equals(user_name) && se.equals(user_server.server());
}



@Override public boolean equals(Object o)
{
   if (!(o instanceof BgtaManager)) return false;
   BgtaManager man = (BgtaManager) o;

   return user_name.equals(man.getUsername()) && user_password.equals(man.getPassword()) &&
         user_server.equals(man.getServer());
}



@Override public int hashCode()
{
   return user_name.hashCode() + user_password.hashCode() + user_server.hashCode();
}



@Override public String toString()
{
   return user_name + ", " + user_server;
}



/********************************************************************************/
/*										*/
/*	Presence methods							*/
/*										*/
/********************************************************************************/

void addPresenceListener(PacketListener p)
{
   Presence pr = new Presence(Presence.Type.available);
   the_connection.addPacketListener(p, new PacketTypeFilter(pr.getClass()));
}

public void subscribeToUser(String jid)
{
   Packet p = new Presence(Presence.Type.subscribe);
   p.setTo(jid);
   the_connection.sendPacket(p);
}

/********************************************************************************/
/*										*/
/*	Connection methods							*/
/*										*/
/********************************************************************************/

void login() throws XMPPException
{
   login(user_name, user_password, user_server);
//   login("codebubbles4tester@gmail.com", "bubbles4");
}


void login(String username,String password,ChatServer server) throws XMPPException
{
   BoardLog.logD("BGTA", "Starting login process for " + username + " on server: " + server.server());
   String serv = server.server();
   String host = server.host();

   // Set up extra security for Facebook.
   ConnectionConfiguration config = null;
   if (serv.equals(ChatServer.FACEBOOK.server())) {
      SASLAuthentication.registerSASLMechanism("DIGEST-MD5",
						  BgtaSASLDigestMD5Mechanism.class);
      config = new ConnectionConfiguration(serv,5222);
      config.setSASLAuthenticationEnabled(true);
    }
   else {
      config = new ConnectionConfiguration(host,5222,serv);
      config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
    }
   the_connection = new XMPPConnection(config);
   stat_con = the_connection;

   // Make a connection and try to login.
   // Disconnect if login fails.
   try {
	the_connection.connect();
	the_connection.login(username, password);
    }
catch (XMPPException e) {
       try {
	  if (the_connection.isConnected())
	     the_connection.disconnect();
	}
catch (Exception ex) {
	    BoardLog.logE("BGTA", "Error disconnecting: " + ex.getMessage());
	}
	BoardLog.logE("BGTA","Error connecting to " + server.server() + ": " + e.getMessage());
	throw new XMPPException("Could not login to " + server.server() + ". Please try again.");
    }
    if (!the_connection.isAuthenticated()) {
       throw new XMPPException("Could not login to " + server.server() + ". Please try again.");
     }

   // Add a listener for messages as well as for roster updates.
   Message m = new Message();
   the_connection.addPacketListener(this, new PacketTypeFilter(m.getClass()));
   roster_listener = new BgtaRosterListener();
   the_connection.getRoster().addRosterListener(roster_listener);
   the_roster = new BgtaXMPPRoster(the_connection.getRoster());
   BoardLog.logD("BGTA","Successfully logged into " + server.server() + " with username: " + username + ".");
}


void disconnect()
{
    BoardLog.logD("BGTA","Starting logout process for " + user_name + " on " + user_server.server());
    the_connection.disconnect();
    for (BgtaChat ch : existing_chats.values()) {
	ch.close();
     }
    existing_chats.clear();
    roster_listener = null;
    BoardLog.logD("BGTA","Successful logout for " + user_name + " on " + user_server.server());
}



/********************************************************************************/
/*										*/
/*	Bubble, Chat, & Doc Managers						*/
/*										*/
/********************************************************************************/

void addDuplicateBubble(BgtaBubble dup)
{
   existing_bubbles.add(dup);
}


boolean hasBubble(String username)
{
   for (BgtaBubble bb : existing_bubbles) {
	  if (bb.getUsername().equals(username))
		 return true;
    }
   return false;
}


BgtaBubble getExistingBubble(String username)
{
   for (BgtaBubble bb : existing_bubbles) {
	  if (bb.getUsername().equals(username))
		 return bb;
    }
   return null;
}


List<BgtaBubble> getExistingBubbles(String username)
{
   List<BgtaBubble> bubbles = new Vector<BgtaBubble>();
   for (BgtaBubble bb : existing_bubbles) {
	  if (bb.getUsername().equals(username))
		 bubbles.add(bb);
    }
   if (bubbles.size() == 0)
	  return null;
   return bubbles;
}


void removeBubble(BgtaBubble bub)
{
   existing_bubbles.remove(bub);
   removeChat(bub.getUsername());
}


BgtaChat startChat(String username,BgtaBubble using)
{
    BgtaChat chat = null;
    if (!hasChat(username)) {
	Chat ch = the_connection.getChatManager().createChat(username,null);
	String name = the_connection.getRoster().getEntry(ch.getParticipant()).getName();
	chat = new BgtaChat(user_name,username,name,user_server,ch,getExistingDoc(username));
	existing_chats.put(username,chat);
	existing_docs.put(username,chat.getDocument());
     }
    existing_bubbles.add(using);
    return chat;
}

BgtaChat startChat(String username)
{
   BgtaChat chat = null;
   if (!hasChat(username)) {
       Chat ch = the_connection.getChatManager().createChat(username,null);
       String name = username;
       if (the_connection.getRoster().getEntry(ch.getParticipant()) != null)
	  name = the_connection.getRoster().getEntry(ch.getParticipant()).getName();
       chat = new BgtaChat(user_name,username,name,user_server,ch,getExistingDoc(username));
       existing_chats.put(username,chat);
       existing_docs.put(username,chat.getDocument());
    }
   else {
         return existing_chats.get(username);
   }
   return chat;
}

boolean hasChat(String username)
{
   if (existing_chats.get(username) == null)
      return false;
   return true;
}


BgtaChat getExistingChat(String username)
{
   return existing_chats.get(username);
}


void removeChat(String username)
{
   if (!hasBubble(username)) {
      BgtaChat chat = getExistingChat(username);
      if (chat != null)
	 chat.close();
      existing_chats.remove(username);
    }
}


boolean hasExistingDoc(String username)
{
   if (existing_docs.get(username) == null)
      return false;
   return true;
}


Document getExistingDoc(String username)
{
   return existing_docs.get(username);
}



/********************************************************************************/
/*										*/
/*	Presence methods							*/
/*										*/
/********************************************************************************/

public static Presence getPresence(String conname)
{
   if (stat_con != null)
      return stat_con.getRoster().getPresence(conname);
     
   return null;
}

/**
 * Returns the proper Icon depending on the type of the Presence.
 *
 * @param pres A Presence
 *
 * @return an Icon dependent on the type of the Presence
 */
public static Icon iconFor(Presence pres)
{
   if (pres == null) return new ImageIcon();
   if (pres.getType() == Presence.Type.available) {
      if (pres.getMode() == null || pres.getMode() == Presence.Mode.available) return BoardImage
	 .getIcon("greenled");
      switch (pres.getMode()) {
	 case away:
	 case xa:
	    return BoardImage.getIcon("yahoo_idle");
	 case dnd:
	    return BoardImage.getIcon("mix_record");
	 case chat:
	    return BoardImage.getIcon("greenled");
	 default:
	    return BoardImage.getIcon("greenled");
       }
    }
   else if (pres.getType() == Presence.Type.unavailable) {
      return BoardImage.getIcon("mini_circle");
    }
   else {
      return new ImageIcon();
    }
}



/********************************************************************************/
/*										*/
/*	Packet Listener 							*/
/*										*/
/********************************************************************************/

@Override public void processPacket(Packet pack)
{
   if (pack instanceof Message) {
      if (((Message) pack).getBody() == null)
	   return;
      if (((Message) pack).getBody().equals(""))
	   return;
      String from = pack.getFrom();
      if (from.lastIndexOf("/") != -1) from = from.substring(0, from.lastIndexOf("/"));
      if (from.equals(user_name)) return;
      BgtaChat chat = getExistingChat(from);
      if (chat != null) {
	 chat.messageReceived(pack);
	 return;
       }
      else {
	 BgtaFactory.createReceivedChatBubble(from, this);
	 chat = getExistingChat(from);
	 if (chat != null)
	    chat.messageReceived(pack);
	 return;
       }
    }
}


private final class BgtaRosterListener implements RosterListener {

   @Override public void presenceChanged(Presence pres) { }

   @Override public void entriesAdded(Collection<String> arg0) {
      the_repository.removeManager(BgtaManager.this);
      the_repository.addNewRep(new BgtaBuddyRepository(BgtaManager.this));
    }

   @Override public void entriesDeleted(Collection<String> arg0) {
      the_repository.removeManager(BgtaManager.this);
      the_repository.addNewRep(new BgtaBuddyRepository(BgtaManager.this));
    }

   @Override public void entriesUpdated(Collection<String> arg0) { }

}	// end of inner class BgtaRosterListener



private class BgtaXMPPRoster implements BgtaRoster {

   private Roster	our_roster;

   BgtaXMPPRoster(Roster roster) {
      this.our_roster = roster;
    }

   @Override public BgtaRosterEntry getEntry(String username) {
      return new BgtaXMPPRosterEntry(this.our_roster.getEntry(username));
    }

   @Override public Collection<BgtaXMPPRosterEntry> getEntries() {
	  Collection<RosterEntry> entries = null;
	  Collection<RosterGroup> groups = this.our_roster.getGroups();
	  List<BgtaXMPPRosterEntry> toreturn = new Vector<BgtaXMPPRosterEntry>();
	  if (!groups.isEmpty()) {
		  for (RosterGroup group : groups) {
			entries = group.getEntries();
			for (RosterEntry entry : entries) {
				toreturn.add(new BgtaXMPPRosterEntry(entry));
			 }
		   }
	   }
	  else {
		 entries = this.our_roster.getEntries();
		 for (RosterEntry entry : entries) {
			toreturn.add(new BgtaXMPPRosterEntry(entry));
		  }
	   }
      return toreturn.subList(0, toreturn.size());
    }

   @Override public Presence getPresence(String username) {
       return this.our_roster.getPresence(username);
    }

}   // end of inner class BgtaXMPPRoster



private static class BgtaXMPPRosterEntry implements BgtaRosterEntry {

   private RosterEntry	the_entry;

   BgtaXMPPRosterEntry(RosterEntry entry) {
      the_entry = entry;
    }

   @Override public String getName() {
      String name = null;
	  if (the_entry.getName() != null)
	 name = the_entry.getName();
	  else
	     name = the_entry.getUser();
      int idx = name.indexOf("@");
      if (idx > 0)
	  name = name.substring(0, idx);
      while (name.indexOf(".") != -1) {
	 if (name.charAt(name.indexOf(".") + 1) != ' ') {
	    String back = name.substring(name.indexOf(".") + 1);
		String front = name.substring(0, name.indexOf("."));
		name = front + " " + back;
	  }
	 else {
	    String back = name.substring(name.indexOf(".") + 1);
	    String front = name.substring(0, name.indexOf("."));
	    name = front + back;
	  }
       }
      return name;
    }

   @Override public String getUser() {
      return the_entry.getUser();
    }

}   // end of inner class BgtaXMPPRosterEntry


}   // end of class BgtaManager




/* end of BgtaManager.java */
