/********************************************************************************/
/*										*/
/*		BeduTATicketListBubble.java					*/
/*		This Bubble is a TA can view a ticket and respond		*/
/*    by opening a chat session with the student				*/
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
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingTextArea;

import javax.swing.JButton;
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


class BeduTATicketViewBubble extends BudaBubble implements BeduConstants{
private static final long serialVersionUID	= 1L;
private static Dimension  DEFAULT_DIMENSION	= new Dimension(250,200);

private BeduStudentTicket student_ticket;


BeduTATicketViewBubble(BeduStudentTicket t,BeduTAXMPPClient client)
{
   student_ticket = t;
   setContentPane(new TicketViewPanel(t,this,new ChatStartListener(this,client)));
}




private static final class TicketViewPanel extends JPanel {

   private static final long serialVersionUID = 1L;

   private TicketViewPanel(BeduStudentTicket t,BeduTATicketViewBubble bubble,
        		      ChatStartListener listener) {
      setOpaque(false);
   
      setPreferredSize(DEFAULT_DIMENSION);
      setLayout(new GridBagLayout());
   
      GridBagConstraints c = new GridBagConstraints();
   
      JLabel ticketarelabel = new JLabel("Problem description: ");
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      c.gridheight = 1;
      c.weightx = 0;
      c.anchor = GridBagConstraints.PAGE_START;
      c.insets = new Insets(0,0,10,0);
      // c.fill = GridBagConstraints.HORIZONTAL;
      add(ticketarelabel, c);
      c.insets = new Insets(0,0,0,0);
   
      JTextArea ticketpane = new SwingTextArea();
      ticketpane.setOpaque(false);
      ticketpane.setText(t.getText());
      ticketpane.setLineWrap(true);
      JScrollPane scroll = new JScrollPane(ticketpane);
      scroll.setOpaque(false);
      scroll.getViewport().setOpaque(false);
      // scroll.setBorder(null);
      c.anchor = GridBagConstraints.PAGE_START;
      c.gridx = 0;
      c.gridy = 1;
      c.gridwidth = 2;
      c.gridheight = 1;
      c.fill = GridBagConstraints.BOTH;
      c.weightx = 1;
      c.weighty = 1;
      add(scroll, c);
   
      JButton submitbutton = new JButton("Chat with student");
      submitbutton.addActionListener(listener);
      c.anchor = GridBagConstraints.PAGE_END;
      c.gridx = 1;
      c.gridy = 2;
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
      Paint p = new GradientPaint(0f,0f,TA_TICKET_VIEW_TOP_COLOR,0f,sz.height,
				     TA_TICKET_VIEW_BOTTOM_COLOR);

      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setPaint(p);
      g2.fill(r);

      super.paintComponent(g);
    }

}	// end of inner class TicketViewPanel'


private static final class ChatStartListener implements ActionListener {
   private BeduTATicketViewBubble parent_bubble;
   private BeduTAXMPPClient	  t_client;


   private ChatStartListener(BeduTATicketViewBubble bubble,BeduTAXMPPClient client) {
      parent_bubble = bubble;
      t_client = client;
    }


   @Override public void actionPerformed(ActionEvent e) {
      BgtaChat c = t_client.acceptTicketAndAlertPeers(parent_bubble.student_ticket);
      BudaBubble chatbub = new BeduTAChatBubble(t_client,c);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(parent_bubble);
      bba.addBubble(chatbub, parent_bubble, null, PLACEMENT_LOGICAL | PLACEMENT_GROUPED);
    }

}	// end of inner class ChatStartListener



}	// end of class BeduTATicketViewBubble



/* end of BeduTATicketViewBubble.java */
