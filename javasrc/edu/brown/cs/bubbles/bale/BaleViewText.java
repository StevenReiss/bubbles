/********************************************************************************/
/*										*/
/*		BaleViewText.java						*/
/*										*/
/*	Bubble Annotated Language Editor view for text element			*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.LabelView;
import javax.swing.text.Segment;
import javax.swing.text.View;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;



class BaleViewText extends LabelView implements BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Segment 	saved_segment;
private int		saved_start;
private int		saved_end;
private int		saved_counter;

private static Map<Color,Paint> squiggle_paints = new HashMap<>();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleViewText(BaleElement e)
{
   super(e);

   saved_segment = null;
   saved_start = -1;
   saved_end = -1;
   saved_counter = -1;
}



/********************************************************************************/
/*										*/
/*	Text caching methods							*/
/*										*/
/********************************************************************************/

@Override public Segment getText(int p0,int p1)
{
   BaleDocument bd = (BaleDocument) getDocument();

   if (p1 < p0) {
      if (saved_segment == null) saved_segment = new Segment();
      saved_start = -1;
      saved_end = -1;
      saved_counter = 0;
      return saved_segment;
    }

   if (p0 != saved_start || p1 != saved_end || bd.getEditCounter() != saved_counter || saved_segment == null) {
      Segment s = new Segment();
      try {
	 bd.getText(p0,p1-p0,s);
	 saved_start = p0;
	 saved_end = p1;
	 saved_counter = bd.getEditCounter();
	 saved_segment = s;
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem with getText (" + p0 + "," + p1 + "): " + e +
			  ":\n" + getElement(),e);
	 return s;
       }
    }

   return saved_segment;
}




/********************************************************************************/
/*										*/
/*	Painting method 							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g,Shape a)
{
   super.paint(g,a);

   AttributeSet as = getAttributes();
   BoardHighlightStyle bs = (BoardHighlightStyle) as.getAttribute(BOARD_ATTR_HIGHLIGHT_STYLE);
   if (bs == null || bs == BoardHighlightStyle.NONE) return;
   Color hc = (Color) as.getAttribute(BOARD_ATTR_HIGHLIGHT_COLOR);
   if (hc == null) return;

   Graphics2D g2 = (Graphics2D) g;

   int p0 = getStartOffset();
   int p1 = getEndOffset();

   Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
   View parent = getParent();
   Segment s = null;
   if ((parent != null) && (parent.getEndOffset() == p1)) {
      // strip whitespace on end
      s = getText(p0, p1);
      while (Character.isWhitespace(s.last())) {
	 p1 -= 1;
	 s.count -= 1;
       }
    }
   if ((parent != null) && (parent.getStartOffset() == p0)) {
      // strip whitespace at start
      if (s == null) s = getText(p0,p1);
      while (Character.isWhitespace(s.first())) {
	 p0 += 1;
	 s.offset += 1;
	 s.count -= 1;
	 if (s.count <= 0) break;
       }
    }
   if (s != null && s.count <= 0) return;

   int x0 = alloc.x;
   int p = getStartOffset();
   if (p != p0) {
      x0 += (int) getGlyphPainter().getSpan(this, p, p0, getTabExpander(), x0);
    }
   int x1 = x0 + (int) getGlyphPainter().getSpan(this, p0, p1, getTabExpander(), x0);

   // calculate y coordinate
   int y = alloc.y + alloc.height - (int) getGlyphPainter().getDescent(this);

   int yTmp = y + 1;

   if (bs == BoardHighlightStyle.SQUIGGLE) {
      Paint sp = getSquigglePaint(hc);
      g2.setPaint(sp);
      g2.fillRect(x0,yTmp-1,x1-x0,3);
    }
   else if (bs == BoardHighlightStyle.LINE) {
      g2.setColor(hc);
      g2.drawLine(x0,yTmp,x1,yTmp);
    }
}



private static Paint getSquigglePaint(Color c)
{
   synchronized (squiggle_paints) {
      Paint p = squiggle_paints.get(c);
      if (p == null) {
	 BufferedImage bi = new BufferedImage(4,3,BufferedImage.TYPE_4BYTE_ABGR);
	 for (int i = 0; i < 4; ++i) {
	    for (int j = 0; j < 3; ++j) {
	       bi.setRGB(i,j,0);
	     }
	  }
	 int color = c.getRGB();
	 bi.setRGB(0,2,color);
	 bi.setRGB(1,1,color);
	 bi.setRGB(2,0,color);
	 bi.setRGB(3,1,color);
	 p = new TexturePaint(bi,new Rectangle(0,0,4,3));
	 squiggle_paints.put(c,p);
       }
      return p;
    }
}



}	// end of class BaleViewText




/* end of BaleViewText.java */
