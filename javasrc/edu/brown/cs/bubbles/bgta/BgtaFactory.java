/********************************************************************************/
/*										*/
/*		BgtaFactory.java						*/
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

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardUpload;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;

import org.jivesoftware.smack.XMPPException;
import org.w3c.dom.Element;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.ToolTipManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * This class sets up the chat interface and provides calls to define chat
 * bubbles
 **/

public class BgtaFactory implements BgtaConstants
{

/********************************************************************************/
/*										*/
/* Private storage */
/*										*/
/********************************************************************************/

private static BgtaFactory		the_factory = null;
private static Vector<BgtaManager>	chat_managers;
private static BgtaRepository		buddy_list;
private static BoardProperties		login_properties;
private static BudaRoot 		my_buda_root;
private static boolean			rec_dif_back;
private static JMenu			metadata_menu;

static {
   chat_managers = new Vector<BgtaManager>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 * Sets up a new repository and configurator. Also creates and logs into
 * any chat accounts for which the user has saved login information.
 */
public static void setup()
{
   login_properties = BoardProperties.getProperties("Bgta");

   // TODO: Needs to be altered to match spr's guidelines for property storage
   String username = null;
   ChatServer server = null;
   try {
      for (int i = 1; i <= login_properties.getInt(BGTA_NUM_ACCOUNTS); ++i) {
	 username = login_properties.getProperty(BGTA_USERNAME_PREFIX + i);
	 String password = login_properties.getProperty(BGTA_PASSWORD_PREFIX + i);
	 server = ChatServer.fromServer(login_properties.getProperty(BGTA_SERVER_PREFIX + i));
	 BgtaManager man = BgtaManager.getManager(username,password,server,buddy_list);
	 man.setBeingSaved(true);
	 man.login();
	 chat_managers.add(man);
       }
    }
   catch (XMPPException e) {
      BoardLog.logE("BGTA","Couldn't load account for " + username + ":" + e.getMessage());
    }
   buddy_list = new BgtaRepository(chat_managers);
   rec_dif_back = login_properties.getBoolean(BGTA_ALT_COLOR_UPON_RECEIVE);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_PEOPLE, buddy_list);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER, buddy_list);
   BudaRoot.addBubbleConfigurator("BGTA", new BgtaConfigurator());

}

/**
 * Always returns the same instance of this class.
 *
 * @return a singleton instance of BgtaFactory
 */
public static synchronized BgtaFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BgtaFactory();
    }
   return the_factory;
}

/**
 * Initializes this package.
 *
 * @param br A BudaRoot
 */
public static void initialize(BudaRoot br)
{
   my_buda_root = br;
   addMetadataMenuItem();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BgtaFactory()
{
}



/********************************************************************************/
/*										*/
/*	Property adding methods 						*/
/*										*/
/********************************************************************************/

static void addManagerProperties(String username,String password,ChatServer server)
{
	int newnum = login_properties.getInt(BGTA_NUM_ACCOUNTS) + 1;
	login_properties.setProperty(BGTA_USERNAME_PREFIX + newnum,username);
	login_properties.setProperty(BGTA_PASSWORD_PREFIX + newnum,password);
	login_properties.setProperty(BGTA_SERVER_PREFIX + newnum,server.server());
	login_properties.setProperty(BGTA_NUM_ACCOUNTS,newnum);
	try {
		login_properties.save();
	} catch (IOException e) {
	}
}

static void clearManagerProperties()
{
	login_properties.clear();
	login_properties.setProperty(BGTA_NUM_ACCOUNTS,0);
	login_properties.setProperty(BGTA_ALT_COLOR_UPON_RECEIVE,rec_dif_back);
	try {
		login_properties.save();
	} catch (IOException e) {
	}
}

static void altColorUponReceive(boolean b)
{
	login_properties.setProperty(BGTA_ALT_COLOR_UPON_RECEIVE,b);
	rec_dif_back = b;
	try {
		login_properties.save();
	} catch (IOException e) {
	}
}


static void logoutAllAccounts()
{ }

static boolean logoutAccount(String username,String server)
{
   BgtaManager logout = null;
	for (BgtaManager man : chat_managers) {
		if (man.propertiesMatch(username,server)) {
			buddy_list.removeManager(man);
			man.disconnect();
			logout = man;
			BassFactory.reloadRepository(buddy_list);
			break;
		}
	}
	if (logout != null) {
	   chat_managers.remove(logout);
	   return true;
	}
	return false;
}


static BoardProperties getBgtaProperties()
{
	return login_properties;
}



/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

BudaBubble createChatBubble(String friendname,String myname,String password,
		String server)
{
	BgtaManager newman;
	BgtaBubble bb = null;
	try {
		newman = BgtaManager.getManager(myname,password,ChatServer.fromServer(server),buddy_list);
		newman.login();
		chat_managers.add(newman);
		bb = new BgtaBubble(friendname,newman);
	} catch (XMPPException e) {
	}
	return bb;
}

/**
 * Returns a List of names representing the users buddies.
 *
 * @return A List of String objects
 */
public static List<String> getChatters()
{
	return buddy_list.getAllBuddyNames();
}

/**
 * Returns an Iterator of the existing managers.
 *
 * @return an Iterator of the existing managers.
 */
public static Iterator<BgtaManager> getManagers()
{
    return chat_managers.iterator();
}

private static BudaBubble createMetadataChatBubble(String friendname,String url)
{
   boolean isknownmanager = false;
   BgtaManager man = buddy_list.getBuddyInfo(friendname).getManager();
   for (BgtaManager m : chat_managers) {
      if (man == m) {
	 isknownmanager = true;
	 break;
       }
    }
   if (!isknownmanager) return null;
   BgtaBubble bb = man.getExistingBubble(friendname);
   if (bb == null) {
      bb = new BgtaBubble(friendname,man);
      Rectangle vp = my_buda_root.getCurrentViewport();
      my_buda_root.add(bb,new BudaConstraint(vp.x,vp.y));
    }
   bb.sendMessage(url);
   return bb;
}



/**
 * Creates and returns a new BgtaBubble, with a non-standard gradient
 * if the user has selected that option.
 *
 * @return a BgtaBubble
 */
public static BgtaBubble createReceivedChatBubble(String username,BgtaManager man)
{
	BgtaBubble bb = new BgtaBubble(username,man);
	if (bb != null) {
		rec_dif_back = login_properties.getBoolean(BGTA_ALT_COLOR_UPON_RECEIVE);
		if (rec_dif_back) {
			bb.setAltColorIsOn(true);
		}
		Rectangle vp = my_buda_root.getCurrentViewport();
		my_buda_root.add(bb,new BudaConstraint(vp.x,vp.y));
	}
	return bb;
}

/********************************************************************************/
/*										*/
/*	Metadata adding methods 						*/
/*										*/
/********************************************************************************/

static void addTaskToRoot(Element xml)
{
	my_buda_root.addTask(xml);
}



private static void addMetadataMenuItem()
{
   metadata_menu = new JMenu("Send Working Set over Chat");
   metadata_menu.addMenuListener(new SendMetadataChatListener());
   my_buda_root.addTopBarMenuItem(metadata_menu,true);
}



private static void addChatButton(JMenu menu,String id,String tt)
{
   JMenuItem itm = new JMenuItem(id);
   itm.addActionListener(new ChatterListener());
   if (tt != null) {
      itm.setToolTipText(tt);
      ToolTipManager.sharedInstance().registerComponent(itm);
    }
   menu.add(itm);
}



/********************************************************************************/
/*										*/
/*	Listeners for chat							*/
/*										*/
/********************************************************************************/

private static class SendMetadataChatListener implements MenuListener
{

   @Override public void menuSelected(MenuEvent e) {
      List<String> chatters = BgtaFactory.getChatters();
      Collections.sort(chatters);
      metadata_menu.removeAll();
      for (String name : chatters) {
	 addChatButton(metadata_menu,name,null);
       }
    }

   @Override public void menuCanceled(MenuEvent e) { }

   @Override public void menuDeselected(MenuEvent e) { }


}	// end of inner class SendMetadataChatListener




private static class ChatterListener implements ActionListener
{

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      String url = "";
      try {
	 File f = my_buda_root.findCurrentWorkingSet().getDescription();
	 BoardUpload bup = new BoardUpload(f);
	 url = bup.getFileURL();
       }
      catch (IOException e1) { }
      createMetadataChatBubble(cmd,url);
    }

}	// End of inner class ChatterListener



}	// end of class BgtaFactory



/* end of BgtaFactory.java */
