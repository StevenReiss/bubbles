/********************************************************************************/
/*										*/
/*		BeduTATicketListBubble.java 	                               	*/
/*		This Bubble is for listing the available tickets               	*/
/*    so a TA can select them and open up chat with a student			*/
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

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;


class BeduTATicketListBubble extends BudaBubble implements BeduConstants{
private static final long serialVersionUID      = 1L;
private static Dimension  DEFAULT_DIMENSION     = new Dimension(200,200);
private BeduTAXMPPClient  ta_client;


BeduTATicketListBubble(BeduTATicketList list,BeduTAXMPPClient taclient)
{
   TicketListPanel p = new TicketListPanel(list,this);
   ta_client = taclient;
   setContentPane(p);
}

@Override public void setVisible(boolean vis)
{
   if (!vis) {
      ta_client.disconnect();
    }
}

private class TicketListPanel extends JPanel implements MouseListener {
   private static final long serialVersionUID = 1L;
   private JTable	    ticket_table;
   private BeduTATicketList  ticket_list;
   private BudaBubble	parent_bubble;
   
   
   TicketListPanel(BeduTATicketList list,BudaBubble parent) {
      super(new BorderLayout());
      parent_bubble = parent;
      
      ticket_list = list;
      setOpaque(false);
      setPreferredSize(DEFAULT_DIMENSION);
      JLabel l = new JLabel("Tickets submitted by students:");
      add(l, BorderLayout.NORTH);
      ticket_table = new JTable(list);
      
      ticket_table.getColumnModel().getColumn(1).setPreferredWidth(75);
      
      ticket_table.getColumnModel().getColumn(0).setPreferredWidth(125);
      
      ticket_table.setFillsViewportHeight(true);
      JScrollPane p = new JScrollPane(ticket_table);
      p.setPreferredSize(new Dimension(ticket_table.getPreferredSize().width,
	    ticket_table.getRowHeight() * 2));
      p.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      ticket_table.setOpaque(false);
      p.setOpaque(false);
      ticket_table.addMouseListener(this);
      add(p, BorderLayout.CENTER);
    }
   
   
   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
         if (ticket_table.rowAtPoint(e.getPoint()) != -1) {
            BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
            BudaBubble ticketviewbubble = new BeduTATicketViewBubble(
		  ticket_list.get(ticket_table.rowAtPoint(e.getPoint())),
		  ta_client);
            bba.addBubble(ticketviewbubble, parent_bubble, null, PLACEMENT_LOGICAL |
		  PLACEMENT_GROUPED);
          }
       }
    }
   
   
   @Override public void mouseEntered(MouseEvent e) { }
   @Override public void mouseExited(MouseEvent e) { }
   @Override public void mousePressed(MouseEvent e) { }
   @Override public void mouseReleased(MouseEvent e) { }
   
}       // end of inner class TicketListPanel


@Override public void paintComponent(Graphics g)
{
   Graphics2D g2 = (Graphics2D) g.create();
   Dimension sz = getSize();
   Paint p = new GradientPaint(0f,0f,TA_TICKET_LIST_TOP_COLOR,0f,sz.height,
         TA_TICKET_LIST_BOTTOM_COLOR);
   
   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   g2.setColor(Color.orange);
   g2.fill(r);
   g2.setPaint(p);
   g2.fill(r);
   
   super.paintComponent(g);
}


}       // end of class BeduTATicketListBubble
