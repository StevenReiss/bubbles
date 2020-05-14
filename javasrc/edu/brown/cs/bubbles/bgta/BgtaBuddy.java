/********************************************************************************/
/*										*/
/*		BgtaBuddy.java							*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
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

import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.buda.BudaBubble;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import javax.swing.Icon;



class BgtaBuddy extends BassNameBase implements PacketListener, BgtaConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String	    user_name;
private String	    connection_name;
private BgtaManager the_manager;
private Icon	    avail_icon;
private Presence    is_available;
private int	    sort_prior;
// private boolean     has_bubble;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaBuddy(String username,BgtaManager man,boolean hasbub)
{
   super();

   name_type = BassNameType.CHAT_BUDDY;

   connection_name = username;
   BgtaRosterEntry entry = man.getRoster().getEntry(username);
   user_name = entry.getName();
   user_name = BGTA_BUDDY_PREFIX + user_name;


   the_manager = man;
   the_manager.addPresenceListener(this);

   is_available = man.getRoster().getPresence(connection_name);
   avail_icon = BgtaManager.iconFor(is_available);
   changeSortPriority();

   // has_bubble = hasbub;
}


/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble()
{
   return new BgtaBubble(connection_name,the_manager);
}


@Override public BudaBubble createPreviewBubble()
{
   return new BgtaBubble(connection_name,the_manager);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BgtaManager getManager()
{
   return the_manager;
}

String getConnectionName()
{
   return connection_name;
}

String getDigestedName()
{
   return user_name.substring(BGTA_BUDDY_PREFIX.length());
}



/********************************************************************************/
/*										*/
/*	BassNameBase methods							*/
/*										*/
/********************************************************************************/

@Override protected String getKey()
{
   return user_name;
}

@Override protected String getParameters()
{
   return null;
}

@Override public String getProject()
{
   return null;
}

@Override protected String getSymbolName()
{
   return user_name;
}

@Override public Icon getDisplayIcon()
{
   return avail_icon;
}

@Override public int getSortPriority()
{
   return sort_prior;
}



/********************************************************************************/
/*										*/
/*	Presence getting methods						*/
/*										*/
/********************************************************************************/

Presence getAvailability()
{
   return is_available;
}



private void changeSortPriority()
{
   Presence.Type ty = is_available.getType();
   Presence.Mode mo = is_available.getMode();
   if (ty == Presence.Type.available) {
      if (mo == null || mo == Presence.Mode.available) sort_prior = BGTA_AVAIL_PRIORITY;
      else {
	 switch (mo) {
	    case chat:
	       sort_prior = BGTA_CHATTY_PRIORITY;
	       break;
	    case away:
	       sort_prior = BGTA_IDLE_PRIORITY;
	       break;
	    case xa:
	       sort_prior = BGTA_XA_PRIORITY;
	       break;
	    case dnd:
	       sort_prior = BGTA_DND_PRIORITY;
	       break;
	    default:
	       sort_prior = BASS_DEFAULT_SORT_PRIORITY;
	  }
       }
    }
   else sort_prior = BGTA_OFFLINE_PRIORITY;
}



/********************************************************************************/
/*										*/
/*	Packet Listener methods 						*/
/*										*/
/********************************************************************************/

@Override public void processPacket(Packet p)
{
   if (p instanceof Presence) {
      Presence pr = (Presence) p;
      String fromuser = pr.getFrom().substring(0, pr.getFrom().indexOf("/"));
      if (fromuser.equals(connection_name)) {
	 is_available = pr;
	 if (pr.getType() == Presence.Type.available
		|| pr.getType() == Presence.Type.unavailable) 
	    avail_icon = BgtaManager.iconFor(is_available);
	 changeSortPriority();
       }
    }
}



}	// end of class BgtaBuddy



/* end of BgtaBuddy.java */
