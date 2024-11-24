/********************************************************************************/
/*										*/
/*		BeduTicketSubmitBubble.java 	                               	*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs			*/
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

package edu.brown.cs.bubbles.bedu.chat;

import edu.brown.cs.bubbles.bgta.BgtaBubble;
import edu.brown.cs.bubbles.bgta.BgtaManager;
import edu.brown.cs.bubbles.bgta.BgtaResourceSwitchingBubble;
import edu.brown.cs.bubbles.bgta.BgtaUtil;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingTextArea;

import org.jivesoftware.smack.packet.Presence;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


class BeduStudentTicketSubmitBubble extends BudaBubble implements BeduConstants {
private static Dimension  DEFAULT_DIMENSION     = new Dimension(275,200);
private static final long serialVersionUID      = 1L;

private TicketPanel       content_panel;
private String	    ta_jid;


BeduStudentTicketSubmitBubble(String jid)
{
   HashMap<String, BgtaManager> chatlogins = new HashMap<>();
   
   BgtaManager man = BgtaUtil.getXMPPManagers().iterator().next();
   man.subscribeToUser(jid);
   if (BgtaManager.getPresence(jid).getType() == Presence.Type.unavailable) {
      JPanel failpanel = new JPanel();
      failpanel.add(new JLabel("TAs are currently not online. Try again later."));
      setContentPane(failpanel);
      setPreferredSize(new Dimension(150,35));
    }
   else {
      Iterator<BgtaManager> iterator = BgtaUtil.getXMPPManagers().iterator();
      for (Iterator<BgtaManager> it = iterator; it.hasNext(); ) {
	 BgtaManager man1 = it.next();
	 chatlogins.put(man1.getUsername(), man1);
       }
      
      ta_jid = jid;
      content_panel = new TicketPanel(chatlogins);
      setContentPane(content_panel);
    }
}

private final class TicketPanel extends JPanel implements ActionListener {
   
   private static final long	serialVersionUID = 1L;
   private Map<String, BgtaManager> chat_logins;
   private JComboBox<String>	login_box;
   private JTextArea		ticket_area;
   
   
   private TicketPanel(Map<String, BgtaManager> logins) {
      setOpaque(false);
      chat_logins = logins;
      setPreferredSize(DEFAULT_DIMENSION);
      setLayout(new GridBagLayout());
      
      GridBagConstraints c = new GridBagConstraints();
      JLabel l = new JLabel("Choose a login: ");
      c.gridx = 0;
      c.gridy = 0;
      c.fill = GridBagConstraints.NONE;
      c.weightx = 0.5;
      c.weighty = 0.1;
      add(l, c);
      
      login_box = new JComboBox<String>(chat_logins.keySet().toArray(new String[1]));
      
      c.gridx = 1;
      c.gridy = 0;
      c.fill = GridBagConstraints.NONE;
      c.weightx = 0.5;
      c.weighty = 0;
      add(login_box, c);
      
      JLabel ticketarealabel = new JLabel("Describe your question or problem: ");
      c.gridx = 0;
      c.gridy = 1;
      c.gridwidth = 2;
      c.gridheight = 1;
      c.weightx = 0;
      c.anchor = GridBagConstraints.PAGE_START;
      c.insets = new Insets(0,0,10,0);
      add(ticketarealabel, c);
      c.insets = new Insets(0,0,0,0);
      
      ticket_area = new SwingTextArea();
      ticket_area.setOpaque(false);
      ticket_area.setLineWrap(true);
      JScrollPane scroll = new JScrollPane(ticket_area);
      scroll.setOpaque(false);
      scroll.getViewport().setOpaque(false);
      
      c.anchor = GridBagConstraints.PAGE_START;
      c.gridx = 0;
      c.gridy = 2;
      c.gridwidth = 2;
      c.gridheight = 1;
      c.fill = GridBagConstraints.BOTH;
      c.weightx = 1;
      c.weighty = 1;
      add(scroll, c);
      
      JButton submitbutton = new JButton("Submit");
      submitbutton.addActionListener(this);
      c.anchor = GridBagConstraints.PAGE_END;
      c.gridx = 1;
      c.gridy = 3;
      c.gridwidth = 1;
      c.gridheight = 1;
      c.weightx = .4;
      c.weighty = 0;
      c.fill = GridBagConstraints.NONE;
      add(submitbutton, c);
    }
   
   
   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Paint p = new GradientPaint(0f,0f,BeduConstants.STUDENT_TICKET_SUBMIT_TOP_COLOR,0f,sz.height,
	    STUDENT_TICKET_SUBMIT_BOTTOM_COLOR);
      
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setPaint(p);
      g2.fill(r);
      
      super.paintComponent(g);
    }
   
   
   @Override public void actionPerformed(ActionEvent e) {
      BgtaManager man = content_panel.chat_logins.get(content_panel.login_box.getSelectedItem());
      
      man.subscribeToUser(ta_jid);
      BgtaBubble chatb = new BgtaResourceSwitchingBubble(man,ta_jid);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BeduStudentTicketSubmitBubble.this);
      bba.addBubble(chatb, this, null, PLACEMENT_LOGICAL);
      chatb.sendMessage("TICKET:" + content_panel.ticket_area.getText());
      
      this.setVisible(false);
    }
   
}       // end of inner class TicketPanel


}       // end of class BeduStudentTicketSubmitButton
