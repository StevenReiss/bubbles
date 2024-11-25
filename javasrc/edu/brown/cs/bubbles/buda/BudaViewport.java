/********************************************************************************/
/*										*/
/*		BudaViewport.java						*/
/*										*/
/*	BUblles Display Area scrollable viewport				*/
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


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JViewport;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;




class BudaViewport extends JViewport implements Printable, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaViewport(BudaBubbleArea view,Element config,int delta)
{
   setView(view);
   Element shape = IvyXml.getChild(config,"SHAPE");
   int pw = (int) IvyXml.getAttrDouble(shape,"WIDTH",BUBBLE_DISPLAY_VIEW_WIDTH);
   int ph = (int) IvyXml.getAttrDouble(shape,"HEIGHT",BUBBLE_DISPLAY_VIEW_HEIGHT);
   ph -= delta;
   setPreferredSize(new Dimension(pw,ph));
   setDoubleBuffered(true);

   setScrollMode(SIMPLE_SCROLL_MODE);
   // setScrollMode(BLIT_SCROLL_MODE);		// causes problems with floating windows
   // setScrollMode(BACKINGSTORE_SCROLL_MODE);	Doesn't work correctly on macs
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("VIEWAREA");
   xw.element("SHAPE",getBounds());
   xw.element("VIEW",getViewRect());
   xw.end("VIEWAREA");
}




/********************************************************************************/
/*										*/
/*	Print methods								*/
/*										*/
/********************************************************************************/

@Override public int print(Graphics g0,PageFormat fmt,int idx)
{
   if (idx > 0) return Printable.NO_SUCH_PAGE;

   Dimension d1 = getSize();
   Dimension d = new Dimension((int) fmt.getWidth(),(int) fmt.getHeight());
   int res = 72;
   Dimension d0 = new Dimension(d);

   int margin = (int) (res * 0.5);
   d.width -= 2*margin;
   d.height -= 2*margin;

   double dx = d.getWidth()/d1.getWidth();
   double dy = d.getHeight()/d1.getHeight();
   double dz = Math.min(dx,dy);

   Graphics2D g = (Graphics2D) g0;

   Color cl = g.getColor();
   g.setColor(Color.WHITE);
   g.fillRect(0,0,d0.width,d0.height);
   g.setColor(cl);
   int w = d1.width;
   int h = d1.height;
   if (w > d.width) w = d.width;
   if (h > d.height) h = d.height;
   g.clipRect(margin,margin,w,h);
   g.translate(margin,margin);
   g.scale(dz,dz);
   paint(g);

   return Printable.PAGE_EXISTS;
}



}	// end of class BudaViewport




/* end of BudaViewport.java */

