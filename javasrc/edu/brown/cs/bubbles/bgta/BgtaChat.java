/********************************************************************************/
/*			      */
/*    BgtaChat.java		    */
/*			      */
/* Bubbles chat management	*/
/*			      */
/********************************************************************************/
/* Copyright 2011 Brown University -- Sumner Warren	       */
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

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardUpload;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlReader;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.w3c.dom.Element;

import javax.naming.OperationNotSupportedException;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationListener;
import net.kano.joustsim.oscar.oscar.service.icbm.MessageInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.SimpleMessage;



/**
 * This class represents a chat between two users. It is a wrapper around two different
 * chat objects: one for the XMPP protocol (Chat) and one for AIM's OSCAR protocol (Conversation).
 * A chat is first created when the bubbles user receives a message from a buddy, or when the bubbles
 * user clicks on or hovers over one of the buddy names in his buddy list. A chat persists until the
 * bubbles user logs out of the account containing the chat. A chat has an associated Document which it
 * uses to log the messages that are sent between the two users. In addition, the chat also maintains a
 * history of the messages which transpire between the users, saving it to a file in the bubbles user's
 * eclipse workspace so that it can be loaded and browsed by the bubbles user at a later point in time.
 */

public class BgtaChat implements BgtaConstants {



/********************************************************************************/
/*			      */
/* Private storage			     */
/*			      */
/********************************************************************************/

// used for all chats
private String this_user;
private String this_display;
private String user_name;
private String user_display;
private ChatServer user_server;
private Document user_document;
private ChatHistory the_history;
private File history_file;
private boolean is_open;
private static final String HISTORY_TIMESTAMP_FORMAT = "MM/dd/yyyy HH:mm:ss";
private static final SimpleDateFormat date_format = new SimpleDateFormat(HISTORY_TIMESTAMP_FORMAT);

// used for XMPP chats
private Chat the_chat;
private MessageListener chat_listener;
private Message last_message;

// used for AIM (OSCAR) conversations
private Conversation the_conversation;
private ConversationListener conversation_listener;
private MessageInfo last_minfo;

// wrapper members
private boolean is_xmpp;

/********************************************************************************/
/*			      */
/* Constructors 			  */
/*			      */
/********************************************************************************/

protected BgtaChat(String username,String pUsername,String pDisplayname,ChatServer server,Object chat,Document doc)
{
   // Fix display names so they don't have the server endings.
   this_user = username;
   this_display = getName(username);
   user_name = pUsername;
   user_display = pDisplayname;
   if (user_display == null) {
      user_display = getName(user_name);
   }

   user_server = server;
   the_chat = null;
   the_conversation = null;
   is_xmpp = false;
   is_open = true;
   last_message = null;
   last_minfo = null;

   // Determine the protocol and set up members appropriately.
   if (!server.equals(ChatServer.AIM)) {
      is_xmpp = true;
      the_chat = (Chat) chat;
      chat_listener = new XMPPChatListener();
      the_chat.addMessageListener(chat_listener);
    }
   else {
      the_conversation = (Conversation) chat;
      conversation_listener = new AIMChatListener();
      the_conversation.addConversationListener(conversation_listener);
   }

   // Create a new Document for this user if one doesn't exist.
   // Otherwise, use the old one.
   user_document = doc;
   if (user_document == null) {
      user_document = new HTMLDocument();
    }

   // Create a new ChatHistory for this chat.
   the_history = new ChatHistory();
   history_file = null;
}


/********************************************************************************/
/*			      */
/* Access methods			    */
/*			      */
/********************************************************************************/

/**
 * Returns the name of the participant in this chat.
 *
 * @return the name of the participant in this chat.
 */
public String getUsername()    { return user_name; }


/**
 * Returns the name of the server hosting this chat.
 *
 * @return the name of the server hosting this chat.
 */
ChatServer getServer()	{ return user_server; }


/**
 * Returns the Document associated with this chat.
 *
 * @return the Document associated with this chat.
 public */
public Document getDocument()  { return user_document; }


/**
 * Returns whether or not this chat is over the XMPP protocol.
 *
 * @return true if this chat is over the XMPP protocol; false otherwise.
 */
boolean isXMPP()	{ return is_xmpp; }

/**
 * Returns the last message logged in this chat.
 *
 * @return the last message logged in this chat.
 */
public Object getLastMessage()
{
    if (!is_xmpp)
	return last_minfo;
    return last_message;
}



/********************************************************************************/
/*			      */
/* Comparison methods				*/
/*			      */
/********************************************************************************/

@Override public boolean equals(Object o)
{
   if (!(o instanceof BgtaChat)) return false;
   BgtaChat chat = (BgtaChat) o;

   return user_name.equals(chat.getUsername()) && user_server.equals(chat.getServer()) && user_document.equals(chat.getDocument());
}

@Override public int hashCode()
{
   return user_name.hashCode() + user_server.hashCode() + user_document.hashCode();
}

@Override public String toString()
{
   return user_name + ", " + user_server + ", " + user_document.toString();
}



/********************************************************************************/
/*			      */
/* Messaging methods			       */
/*			      */
/********************************************************************************/

/**
 * Processes a received message. Creates a new bubble for this
 * chat if it there are none. Handles metadata if present. Otherwise,
 * just logs the message to all existing chat bubbles.
 *
 * @param msg The received message object to be processed. Either an instance of Message or MessageInfo, depending on the protocol in use.
 */
void messageReceived(Object msg)
{
   String message = null;
   if (is_xmpp) {
      if (msg == last_message) return;
      message = ((Message) msg).getBody();
      last_message = (Message) msg;
    }
   else {
       if (msg == last_minfo) return;
       message = ((MessageInfo) msg).getMessage().getMessageBody().replaceAll("<.*?>","");
       last_minfo = (MessageInfo) msg;
    }
   if (message == null || message.equals("")) return;

   BgtaManager man = null;
   if (man == null) {
       for (Iterator<BgtaManager> iter = BgtaFactory.getManagers(); iter.hasNext(); ) {
	   man = iter.next();
	   if (man.propertiesMatch(this_user, user_server.server()))
	      break;
	}
    }
   if (!is_open) {
       BgtaFactory.createReceivedChatBubble(user_name,man);
       is_open = true;
    }
   if (man != null && message.startsWith(BoardUpload.getUploadUrl() + "WorkingSets/") && message.endsWith(".xml")) {
       logMessage("Working set received.","Bubbles");
       Collection<BgtaBubble> bubbles = man.getExistingBubbles(user_name);
       if (bubbles != null) {
	   for (BgtaBubble bb : man.getExistingBubbles(user_name)) {
	       bb.processMetadata(message);
	    }
	}
    }
   else
      logMessage(message);
}

/**
 * Convenience method for logMessage(message,user_display).
 *
 * @param msg A String
 */
public void logMessage(String msg)
{
   logMessage(msg,user_display);
}

/**
 * Logs a message in the Chat's Document. All chat bubbles
 * which are registered a listeners of the Document will
 * receive the change.
 *
 * @param msg The text of the message
 * @param from The name of the user that sent the message
 */
void logMessage(String msg,String from)
{
   if (!from.equals(user_display) && !from.equals("Me") && !from.equals("Error") && !from.equals("Bubbles"))
      return;
   try {
       user_document.insertString(user_document.getLength(),from + ": " + msg + "\n",null);
    } catch (BadLocationException e) {
       //System.out.println("bad loc");
    }
    String to = this_display;
   if (from.equals("Me")) {
      to = user_display;
      from = this_display;
    }
   the_history.addHistoryItem(new ChatHistoryItem(from,to,msg,date_format.format(new Date())));
}

/**
 * Sends a message from the bubbles user to
 * the other participant of this chat. Logs
 * the message in the chat bubble afterward
 * so the user knows it was sent. Logs an error
 * to the bubble if the message sending failed
 * for some reason.
 *
 * @param msg The text of the message to be sent
 *
 * @return true if the message sent successfully; false otherwise.
 */
public boolean sendMessage(String msg)
{
   boolean sent = true;
   try {
      if (is_xmpp && the_chat != null)
	  the_chat.sendMessage(msg);
      if (!is_xmpp && the_conversation != null)
	  the_conversation.sendMessage(new SimpleMessage(wrapHTML(msg)));
    } catch (XMPPException e) {
      sent = false;
      BoardLog.logE("BGTA","Error sending message: " + msg + " to: " + user_name);
      logMessage("Message not sent. Please try again.", "Error");
    }
   if (sent)
       logMessage(msg, "Me");
   return sent;
}


/********************************************************************************/
/*			      */
/* Helper methods			    */
/*			      */
/********************************************************************************/

protected void setChat(Object chat)
{
   the_chat.removeMessageListener(the_chat.getListeners().iterator().next());
   if (is_xmpp) {
      the_chat = (Chat) chat;
      chat_listener = new XMPPChatListener();
      the_chat.addMessageListener(chat_listener);
    }
   else {
      the_conversation = (Conversation) chat;
      conversation_listener = new AIMChatListener();
      the_conversation.addConversationListener(conversation_listener);
   }
}

protected Chat getChat() throws OperationNotSupportedException
{
   if (is_xmpp) return the_chat;

   throw new OperationNotSupportedException("Not an XMPP BgtaChat");
}

/**
 * Creates a more displayable version of a username. This
 * is accomplished by tearing off anything after an @, and replacing periods
 * and underscores with spaces.
 *
 * @param username The username to be transformed into just a name
 *
 * @return The resulting String
 */
private String getName(String username)
{
   String name = username;
   int idx = name.indexOf("@");
   if (idx > 0)
       name = name.substring(0, idx);
   name = whiteSpaceAwareReplace(name,".");
   name = whiteSpaceAwareReplace(name,"_");
   return name;
}

/**
 * Replaces all occurrences of toreplace with a space. This method checks
 * to make sure that there isn't a space next to an occurrence of toreplace
 * already before replacing it. If there is, it simply removes the occurrence
 * of toreplace, leaving the already present space to fill the void.
 *
 * @param input A String
 * @param toreplace The String to replace
 *
 * @return The resulting String
 */
private String whiteSpaceAwareReplace(String input,String toreplace)
{
   String current = input;
   while (current.indexOf(toreplace) != -1) {
      if (current.charAt(current.indexOf(toreplace) + 1) != ' ') {
	 String back = current.substring(current.indexOf(toreplace) + 1);
	 String front = current.substring(0, current.indexOf(toreplace));
	 current = front + " " + back;
       }
      else {
	 String back = current.substring(current.indexOf(toreplace) + 1);
	 String front = current.substring(0, current.indexOf(toreplace));
	 current = front + back;
       }
    }
   return current;
}

/**
 * Wraps a string of text in html tags, and escapes several popular characters with
 * their HTML entities.
 *
 * @param text A String
 *
 * @return The resulting String
 */
private String wrapHTML(String text)
{
   String temp = text;
   temp = replace(temp,"&","&amp;");
   temp = replace(temp,"<","&lt;");
   temp = replace(temp,">","&gt;");
   temp = replace(temp,"\"","&qout;");
   temp = replace(temp,"\n","<br>");
   return "<html><body>" + temp + "</body></html>";
}

/**
 * Replaces all occurrences of toreplace with replacewith.
 * A recursive, non-regular expression alternative to the
 * builtin String replaceAll method.
 *
 * @param input A String
 * @param toreplace The String to replace
 * @param repalcewith The String to be used for replacement
 *
 * @return The resulting String
 */
private String replace(String input,String toreplace,String replacewith)
{
   String current = input;
   int pos = current.indexOf(toreplace);
   if (pos != -1) {
      current = current.substring(0,pos) + replacewith + replace(current.substring(pos + toreplace.length()),toreplace,replacewith);
    }
   return current;
}


/********************************************************************************/
/*			      */
/* History methods			     */
/*			      */
/********************************************************************************/
/**
 * Creates a file for the history to be saved in.
 * The name of the file contains the names of the
 * two users involved in the chat. The file is created
 * in the eclipse workspace that bubbles uses.
 */
private void createHistoryFile()
{
   if (history_file != null) return;

   BoardLog.logD("BGTA","Creating chat history file for " + this_display + " and " + user_display);
   File dir = BoardSetup.getBubblesWorkingDirectory();
   if (dir != null) {
      try {
	 String login_user = replace(this_display," ","").toLowerCase();
	 String other_user = replace(user_display," ","").toLowerCase();
	 for (int i = 0; i < 5; ++i) {
	    String fnm = "history_" + login_user + "_" + other_user + ".xml";
	    File f = new File(dir,fnm);
	    if (f.createNewFile() || f.exists()) {
	       history_file = f;
	       break;
	     }
	    try {
	       Thread.sleep(1000);
	     }
	    catch (InterruptedException e) { }
	  }
       }
      catch (IOException e) {
	 BoardLog.logE("BGTA","Problem creating chat history file",e);
	 return;
       }
    }
   BoardLog.logD("BGTA","Successfully created chat history file for " + this_display + " and " + user_display);
}

/**
 * Saves the history of this chat to a file.
 * This is done so that the history can be loaded
 * in a later run of bubbles so that the user
 * can browse the chat history if they wish to do so.
 */
private synchronized void saveHistory()
{
   if (!the_history.hasChanged())
      return;

   if (history_file == null) createHistoryFile();
   if (history_file == null) {
      BoardLog.logE("BGTA","Problem saving chat history");
      return;
    }

   BoardLog.logD("BGTA","Saving chat history for " + this_display + " and " + user_display + " to " + history_file.getName());
   try {
      IvyXmlWriter xw = new IvyXmlWriter(new OutputStreamWriter(new FileOutputStream(history_file,true),"UTF-8"));
      the_history.outputXML(xw);
      xw.close();
    }
   catch (IOException e) {
      BoardLog.logE("BGTA","Problem writing chat history file",e);
      history_file = null;
      return;
    }
   BoardLog.logD("BGTA","Successfully saved chat history for " + this_display + " and " + user_display + " to " + history_file.getName());
}

/**
 * Loads the history of previous chats between
 * these two participants from a file produced
 * by bubbles at an earlier date. At the user's
 * request, the history is loaded into a
 * JTextComponent in a separate portion of a chat
 * bubble.
 *
 * @param c A JTextComponent (most likely a JTextPane or JTextArea)
 */
synchronized void loadHistory(JTextComponent c)
{
   if (history_file == null) createHistoryFile();
   if (history_file == null) {
      BoardLog.logE("BGTA","Problem loading chat history: history file not found.");
      return;
    }

   BoardLog.logD("BGTA","Loading chat history for " + this_display + " and " + user_display + " from " + history_file.getName());
   Vector<Element> history = new Vector<Element>();
   try {
      IvyXmlReader xr = new IvyXmlReader(new InputStreamReader(new FileInputStream(history_file)));
      String xmlString = xr.readXml();
      Element e = null;
      while (xmlString != null && !xmlString.equals("")) {
	 e = IvyXml.convertStringToXml(xmlString);
	 history.add(e);
	 xmlString = xr.readXml();
       }
      xr.close();
    }
   catch (Exception e) {
      BoardLog.logE("BGTA","Problem loading chat history.",e);
      return;
    }

   // Iterate through history.
   Iterator<Element> sessions = history.iterator();
   while (sessions.hasNext()) {
       Element session = sessions.next();
       Iterator<Element> messages = IvyXml.getChildren(session);
       while (messages.hasNext()) {
	   Element msg = messages.next();
	   Iterator<Element> contents = IvyXml.getChildren(msg);
	   while (contents.hasNext()) {
	       String from = null;
	       String text = null;
	       String time = null;
	       while (contents.hasNext()) {
		   Element content = contents.next();
		   if (content.getTagName().equalsIgnoreCase("FROM"))
		       from = content.getTextContent();
		   if (content.getTagName().equalsIgnoreCase("TEXT"))
		       text = content.getTextContent();
		   if (content.getTagName().equalsIgnoreCase("TIME"))
		       time = content.getTextContent();
		}
	       if (from != null && !from.equals("") && text != null && !text.equals("") && time != null && !time.equals(""))
		   c.setText(c.getText() + from + ": " + text + "\n");
	    }
	}
    }
   BoardLog.logD("BGTA","Successfully loaded chat history for " + this_display + " and " + user_display + " from " + history_file.getName());
}

/********************************************************************************/
/*			      */
/* Management methods				*/
/*			      */
/********************************************************************************/
/**
 * Closes this chat and saves the chat's history so it can be loaded
 * in a later run of bubbles.
 */
void close()
{
   is_open = false;
   saveHistory();
}


/**
 * A class for processing received XMPP messages.
 */
private class XMPPChatListener implements MessageListener {

   @Override public void processMessage(Chat ch,Message msg) {
      if (!ch.equals(the_chat))
	 return;
      if (msg.getType() != Message.Type.chat)
	 return;
     messageReceived(msg);
    }

}  // end of inner class XMPPChatListener


/**
 * A class for processing received AIM messages.
 */
private class AIMChatListener implements ConversationListener {

   @Override public void canSendMessageChanged(Conversation arg0, boolean arg1) { }

   @Override public void conversationClosed(Conversation arg0) { }

   @Override public void conversationOpened(Conversation arg0) { }

   @Override public void gotMessage(Conversation con, MessageInfo msg) {
      if (!con.equals(the_conversation))
	  return;
      messageReceived(msg);
    }

   @Override public void gotOtherEvent(Conversation arg0, ConversationEventInfo arg1) { }

   @Override public void sentMessage(Conversation arg0, MessageInfo arg1) { }

   @Override public void sentOtherEvent(Conversation arg0, ConversationEventInfo arg1) { }

}  // end of inner class AIMChatListener


/**
 * Represents a history of a chat between two users. This history
 * only represents a single session (any number of chats during one run of bubbles).
 */
private class ChatHistory {

    private Vector<ChatHistoryItem> my_items;
    boolean has_changed;
    int previous_size;

    /**
     * Creates an empty ChatHistory.
     */
    ChatHistory() {
       my_items = new Vector<ChatHistoryItem>();
       has_changed = false;
       previous_size = 0;
    }

    /**
     * Returns whether or not the history has changed since the last time
     * it was saved.
     *
     * @return true if the history has changed since it was last saved; false otherwise.
    */
    boolean hasChanged()		      { return has_changed; }

    /**
     * Adds an item to this chat history.
     *
     * @param item A ChatHistoryItem
     *
     */
    void addHistoryItem(ChatHistoryItem item) {
       my_items.add(item);
       has_changed = true;
    }

    /**
     * Writes this history to a file so it can be loaded later.
     *
     * @param xw An IvyXmlWriter
     *
     */
    void outputXML(IvyXmlWriter xw) {
       if (has_changed) {
	  xw.begin("SESSION");
	  xw.field("FROM",user_name);
	  xw.field("TO",this_user);
	  xw.field("SERVER",user_server.server());
	  for (int i = previous_size; i < my_items.size(); ++i) {
	     ChatHistoryItem item = my_items.get(i);
	     item.outputXML(xw);
	   }
	  xw.end("SESSION");
	  has_changed = false;
	  previous_size = my_items.size();
	}
    }

}  // end of inner class ChatHistory


/**
 * Represents a single item in a chat history. Right now
 * this represents a single message. I might extend this
 * to be a interface and have subclasses which represent
 * different events: messages, login, logout, metadata.
 */
private static class ChatHistoryItem {

    private String item_from;
    private String item_to;
    private String item_text;
    private String item_time;

    /**
     * Creates a ChatHistoryItem with the given users, message, and timestamp.
     *
     * @param from The name of the user that sent the message
     * @param to The name of the user that received the message
     * @param text The body of the message
     * @param time The time at which the message transpired
     */
    ChatHistoryItem(String from,String to,String text,String time) {
       item_from = from;
       item_to = to;
       item_text = text;
       item_time = time;
    }

    /**
     * Returns the name of this message's sender.
     *
     * @return the name of this message's sender.
     */
    // String getFrom() { return item_from; }

    /**
     * Returns the name of this message's recipient.
     *
     * @return the name of this message's recipient.
     */
    // public String getTo() { return item_to; }

    /**
     * Returns the body of this message.
     *
     * @return the body of this message.
     */
    // public String getText() { return item_text; }

    /**
     * Returns the time at which this message occurred.
     *
     * @return the time at which this message occurred.
     */
    // public String getTime() { return item_time; }

    /**
     * Writes this history to a file so it can be loaded later.
     *
     * @param xw An IvyXmlWriter
     *
     */
    public void outputXML(IvyXmlWriter xw) {
       xw.begin("MESSAGE");
       xw.textElement("FROM",item_from);
       xw.textElement("TO",item_to);
       xw.textElement("TEXT",item_text);
       xw.textElement("TIME",item_time);
       xw.end("MESSAGE");
    }

}  // end of inner class ChatHistoryItem


}
