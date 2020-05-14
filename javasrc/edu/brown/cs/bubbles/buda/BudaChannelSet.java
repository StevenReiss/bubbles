/********************************************************************************/
/*										*/
/*		BudaChannelSet.java						*/
/*										*/
/*	BUblles Display Area channel set					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.buda;


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;




/**
 *	This class represents a set of channels that can be used to represent
 *	parallel or multiple instances of bubble areas, for example multiple
 *	debugging sessions
 **/

public class BudaChannelSet implements BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot		for_root;
private JScrollPane		scroll_pane;
private ChannelPanel		channel_panel;
private List<BudaChannel>	channel_items;
private String			top_color_name;
private String			bottom_color_name;
private BudaBubbleArea		current_area;
private Element 		default_config;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BudaChannelSet(BudaRoot root,String topc,String botc,String dflt)
{
   for_root = root;
   top_color_name = topc;
   bottom_color_name = botc;
   channel_items = new ArrayList<BudaChannel>();
   current_area = null;
   default_config = null;
   if (dflt != null) default_config = IvyXml.convertStringToXml(dflt);

   channel_panel = new ChannelPanel();
   scroll_pane = new JScrollPane(channel_panel);
   scroll_pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
   scroll_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

   scroll_pane.addComponentListener(new SizeListener());

   addChannel(null);

   root.addChannelSet(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JComponent getComponent()		{ return scroll_pane; }


public BudaBubbleArea getBubbleArea()
{
   return current_area;
}


/**
 *	Return the number of channels in the channel set.
 **/

public int getNumChannels()		{ return channel_items.size(); }



/**
 *	Determine if the current channel is empty or not
 **/

public boolean isChannelEmpty()
{
   if (current_area == null) return true;

   return current_area.getComponentCount() == 0;
}



void setBubbleArea(BudaBubbleArea bba)
{
   if (bba != null && bba.getChannelSet() == this) {
      current_area = bba;
    }
}



/**
 *	Set the name of the current channel
 **/

public void setChannelName(String name)
{
   if (current_area == null) return;

   for (BudaChannel bc : channel_items) {
      if (bc.getBubbleArea() == current_area) {
	 bc.setChannelName(name);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Channel manipulation methods						*/
/*										*/
/********************************************************************************/

/**
 *	Add a new channel to the channel set
 **/

public BudaBubbleArea addChannel(String label)
{
   BudaChannel bc = new BudaChannel(for_root,default_config,this,label);
   channel_items.add(bc);
   JViewport vp = scroll_pane.getViewport();
   Rectangle r = bc.getBounds();
   vp.scrollRectToVisible(r);

   bc.getBubbleArea().setColors(top_color_name,bottom_color_name);

   channel_panel.add(bc);

   current_area = bc.getBubbleArea();

   checkSizes();

   scroll_pane.repaint();
   for (BudaChannel xbc : channel_items) {
      xbc.repaint();
      xbc.getBubbleArea().repaint();
    }

   return current_area;
}



void removeChannel(BudaChannel bc)
{
   if (channel_items.remove(bc)) {
      channel_panel.remove(bc);
      checkSizes();
    }
}


void resetSizes()
{
   checkSizes();
}



/********************************************************************************/
/*										*/
/*	Viewport methods							*/
/*										*/
/********************************************************************************/

void setViewport(int x,int y)
{
   BudaChannel bc = getCurrentChannel();
   if (bc != null) bc.setViewport(x,y);
}


void moveViewport(int dx,int dy)
{
   BudaChannel bc = getCurrentChannel();
   if (bc != null) bc.moveViewport(dx,dy);
}



Rectangle getViewport()
{
   BudaChannel bc = getCurrentChannel();
   if (bc != null) return bc.getViewport();

   return new Rectangle(0,0,0,0);
}



private BudaChannel getCurrentChannel()
{
   for (Component c = current_area; c != null; c = c.getParent()) {
      if (c instanceof BudaChannel) {
	 return (BudaChannel) c;
       }
    }

   for (BudaChannel bc : channel_items) {
      return bc;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Sizing methods								*/
/*										*/
/********************************************************************************/

private void checkSizes()
{
   JViewport vp = scroll_pane.getViewport();
   Dimension d1 = vp.getSize();

   if (d1.height == 0) return;

   int sz = d1.height;
   if (channel_items.size() > 1) sz /= 2;

   int tsz = 0;
   for (BudaChannel bc : channel_items) {
      if (bc.isFullSize()) tsz += d1.height;
      else tsz += sz;
   }

   Dimension d2 = channel_panel.getSize();
   d2.width = d1.width;
   d2.height = tsz;
   channel_panel.setPreferredSize(d2);
   channel_panel.setSize(d2);

   tsz = 0;
   for (BudaChannel bc : channel_items) {
      if (bc.isFullSize()) bc.setSize(d1.width,d1.height);
      else bc.setSize(d1.width,sz);
      bc.setLocation(0,tsz);
      tsz += bc.getHeight();
      bc.repaint();
    }

   channel_panel.repaint();
   scroll_pane.repaint();
   current_area.invalidate();
   current_area.repaint();
}




private class SizeListener extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      checkSizes();
    }

}	// end of inner class SizeListener




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("CHANNELSET");
   xw.element("TOPCOLOR",top_color_name);
   xw.element("BOTTOMCOLOR",bottom_color_name);
   for (BudaChannel bc : channel_items) {
      bc.outputXml(xw);
    }
   xw.end("CHANNELSET");
}




/********************************************************************************/
/*										*/
/*	Panel for holding items 						*/
/*										*/
/********************************************************************************/

private static class ChannelPanel extends JComponent implements Scrollable {

   private static final long serialVersionUID = 1;

   ChannelPanel() {
      // super(new GridLayout(0,1));
      // super(new GridBagLayout());
    }

   @Override public Dimension getPreferredScrollableViewportSize() {
      return new Dimension(BUBBLE_DISPLAY_WIDTH,BUBBLE_DISPLAY_HEIGHT);
    }

   @Override public int getScrollableBlockIncrement(Rectangle vr,int or,int d) {
      return 100;
    }

   @Override public boolean getScrollableTracksViewportHeight() {
      return false;
    }

   @Override public boolean getScrollableTracksViewportWidth() {
      return true;
    }

   @Override public int getScrollableUnitIncrement(Rectangle r,int or,int d) {
      return 1;
    }

}	// end of inner class ChannelPanel




}	// end of class BudaChannelSet




/* end of BudaChannelSet.java */
