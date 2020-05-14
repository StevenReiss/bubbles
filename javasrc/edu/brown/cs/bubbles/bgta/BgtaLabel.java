/********************************************************************************/
/*										*/
/*		BgtaLabel.java							*/
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

import edu.brown.cs.bubbles.buda.BudaCursorManager;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.Cursor;
import java.awt.Dimension;



class BgtaLabel extends JPanel implements PacketListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JLabel          my_label;
private ImageIcon	my_icon;
private JButton         my_button;
private String		user_name;
private Presence	my_presence;


private static final long serialVersionUID = 1L;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaLabel(String username,String displayname)
{
   super();
   setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
   setOpaque(false);
   my_label = new JLabel(displayname,BgtaManager.iconFor(new Presence(Presence.Type.available)),SwingConstants.LEFT);
   my_presence = new Presence(Presence.Type.available);
   my_icon = (ImageIcon) my_label.getIcon();
   if (my_presence.getType() == Presence.Type.available && my_presence.getMode() != null) {
      my_label.setToolTipText(my_presence.getMode().toString());
   }
   else if (my_presence.getType() == Presence.Type.unavailable) {
       my_label.setToolTipText("Unavailable");
   }
   user_name = username;
   my_label.setHorizontalTextPosition(SwingConstants.RIGHT);
   BudaCursorManager.setCursor(my_label,Cursor.getDefaultCursor());
   my_label.createToolTip();
   my_button = null;
   
   Dimension d = new Dimension(getSize().width,my_icon.getIconHeight());
   setPreferredSize(d);
   setSize(d);
   
   add(my_label);
}

void setButton(JButton button)
{
    removeAll();
    add(my_label);
    my_button = button;
    if (my_button != null) {
       add(Box.createHorizontalGlue());
       add(my_button);
    }
    repaint();
}

void setPresence(Presence p)
{
    my_presence = p;
    my_icon = (ImageIcon) BgtaManager.iconFor(p);
    my_label.setIcon(my_icon);
}

/********************************************************************************/
/*										*/
/*	PacketListener								*/
/*										*/
/********************************************************************************/

@Override public void processPacket(Packet p)
{
   if (p instanceof Presence) {
      Presence pr = (Presence) p;
      String fromuser = pr.getFrom().substring(0, pr.getFrom().indexOf("/"));
      if (fromuser.equals(user_name)) {
	 my_presence = pr;
	 if (pr.getType() == Presence.Type.available) {
	    my_icon = (ImageIcon) BgtaManager.iconFor(my_presence);
	    my_label.setIcon(my_icon);
            if (my_presence.getMode() != null)
                my_label.setToolTipText(my_presence.getMode().toString());
	 }
	 else if (pr.getType() == Presence.Type.unavailable) {
	    my_icon = (ImageIcon) BgtaManager.iconFor(my_presence);
	    my_label.setIcon(my_icon);
	    my_label.setToolTipText("unavailable");
	 }
      }
   }
}






}	// end of class BgtaLabel



/* end of BgtaLabel.java */
