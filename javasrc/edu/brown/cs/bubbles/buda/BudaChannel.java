/********************************************************************************/
/*										*/
/*		BudaChannel.java						*/
/*										*/
/*	BUblles Display Area channel with overview bar, display area, panel	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingTextField;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Date;



class BudaChannel extends SwingGridPanel implements BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot		for_root;
private BudaBubbleArea		bubble_area;
private BudaOverviewBar 	bubble_overview;
private BudaViewport		bubble_view;
private JLabel			date_label;
private JTextField		name_label;
private boolean 		full_size;

private static final long	serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaChannel(BudaRoot br,Element e,BudaChannelSet cs,String label)
{
   for_root = br;
   full_size = false;

   setInsets(1);

   Element areacfg = IvyXml.getChild(e,"BUBBLEAREA");
   bubble_area = new BudaBubbleArea(for_root,areacfg,cs);
   bubble_area.addBubbleAreaCallback(new ViewportCallback());
   bubble_area.addMouseMotionListener(br.getShadeMotionAdapter());
   
   Element viewcfg = IvyXml.getChild(e,"VIEWAREA");
   bubble_view = new BudaViewport(bubble_area,viewcfg,0);
   bubble_view.addChangeListener(new ViewportHandler());
   addGBComponent(bubble_view,0,2,0,1,10,10);

   bubble_overview = new BudaOverviewBar(bubble_area,bubble_view,for_root);
   Dimension sz = new Dimension(0,BUBBLE_CHANNEL_OVERVIEW_HEIGHT);
   bubble_overview.setMinimumSize(sz);
   bubble_overview.setPreferredSize(sz);
   MouseMotionAdapter mma = br.getShadeMotionAdapter();
   if (mma != null) bubble_overview.addMouseMotionListener(mma);   addGBComponent(bubble_overview,3,0,1,2,10,0);

   JButton close = new JButton(new DeleteAction());
   addGBComponent(close,0,1,1,1,0,0);
   JButton expand = new JButton(new ExpandAction());
   addGBComponent(expand,0,0,1,1,0,0);

   date_label = new JLabel(new Date().toString());
   Font ft = date_label.getFont();
   ft = ft.deriveFont(11f);
   BoardColors.setColors(date_label,"Bddt.ChannelTopColor");
   date_label.setFont(ft);
   date_label.setBorder(new EmptyBorder(2,5,2,5));
   date_label.setOpaque(true);
   addGBComponent(date_label,1,0,1,1,0,0);

   if (label == null) label = "<Name goes here>";
   name_label = new SwingTextField(label);
   BoardColors.setColors(name_label,"Bddt.ChannelTopColor");
   name_label.setFont(ft);
   name_label.setBorder(new EmptyBorder(2,5,2,5));
   addGBComponent(name_label,1,1,1,1,0,0);

   JToolBar tools = new JToolBar();
   tools.setFloatable(false);
   addGBComponent(tools,4,0,1,0,0,0);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BudaBubbleArea getBubbleArea()		{ return bubble_area; }

BudaOverviewBar getOverviewBar()	{ return bubble_overview; }

void setChannelName(String label) {
   name_label.setText(label);
   date_label.setText(new Date().toString());
}

BudaChannelSet getChannelSet()		{ return bubble_area.getChannelSet(); }

boolean isFullSize()			{ return full_size; }



/********************************************************************************/
/*										*/
/*	Viewport methods							*/
/*										*/
/********************************************************************************/

void setViewport(int x,int y)
{
   Rectangle v = bubble_view.getBounds();
   Rectangle a = bubble_area.getBounds();

   if (x < 0) x = 0;
   if (x + v.width >= a.width) x = a.width - v.width;
   if (y < 0) y = 0;
   if (y + v.height >= a.height) y = a.height - v.height;

   bubble_view.setViewPosition(new Point(x,y));
}



void moveViewport(int dx,int dy)
{
   Point p = bubble_view.getViewPosition();
   setViewport(p.x + dx, p.y + dy);
}


Rectangle getViewport()
{
   return bubble_view.getViewRect();
}


private final class ViewportHandler implements ChangeListener {

   @Override public void stateChanged(ChangeEvent e) {
      Rectangle vr = bubble_view.getViewRect();
      bubble_area.setViewPosition(vr);
      bubble_overview.setViewPosition(vr);
    }

}	// end of inner class ViewportHandler



private final class ViewportCallback implements BubbleAreaCallback {

   @Override public void updateOverview()		{ }

   @Override public void moveDelta(int dx,int dy) {
      Point p = bubble_view.getViewPosition();
      setViewport(p.x + dx,p.y + dy);
    }

}	// end of inner class ViewportCallback




/********************************************************************************/
/*										*/
/*	Input/Output methods							*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("CHANNEL");
   xw.element("SHAPE",getBounds());
   bubble_overview.outputXml(xw);
   bubble_view.outputXml(xw);
   bubble_area.outputXml(xw);
   xw.end("CHANNEL");
}



/********************************************************************************/
/*										*/
/*	Panel actions								*/
/*										*/
/********************************************************************************/

private class DeleteAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   DeleteAction() {
      super(null,BoardImage.getIcon("debug/close.png"));
      putValue(SHORT_DESCRIPTION,"Close this channel");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      getChannelSet().removeChannel(BudaChannel.this);
    }

}	// end of inner class DeleteAction



private class ExpandAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ExpandAction() {
      super(null,BoardImage.getIcon("debug/fullscreen.png"));
      putValue(SHORT_DESCRIPTION,"Expand/contract this channel");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      full_size = !full_size;
      getChannelSet().resetSizes();
    }

}	// end of inner class ExpandAction



}	// end of class BudaChannel






/* end of BudaChannel.java */
