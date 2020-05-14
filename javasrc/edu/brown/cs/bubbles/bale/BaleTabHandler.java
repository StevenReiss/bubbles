/********************************************************************************/
/*										*/
/*		BaleTabHandler.java						*/
/*										*/
/*	Bubble Annotated Language Editor tab handler				*/
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

import edu.brown.cs.bubbles.bump.BumpClient;

import javax.swing.text.StyleContext;
import javax.swing.text.TabExpander;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;



class BaleTabHandler implements TabExpander, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleElement cur_element;
private int tab_size;
private Font last_font;
private int font_width;
private int font_height;

private static int base_tab_size;


private static final float	rounding_value = 0.95f; 	// matches AWT

static {
   String v = BALE_PROPERTIES.getProperty("indent.tabulation.size");
   if (v == null) v = BumpClient.getBump().getOption("org.eclipse.jdt.core.formatter.tabulation.size");
   if (v == null) v = BALE_PROPERTIES.getProperty("Bale.tabsize");
   base_tab_size = 8;
   try {
      if (v != null) base_tab_size = Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleTabHandler()
{
   tab_size = base_tab_size;
   last_font = null;
   font_width = 0;
   cur_element = null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setElement(BaleElement e) {
   cur_element = e;
}


int getFontHeight() {
   setFont();
   return font_height;
}

int getSpaceWidth() {
   setFont();
   return font_width;
}


int getTabSize() {
   setFont();
   return tab_size;
}


static void setBaseTabSize(int tz)
{
   base_tab_size = tz;
}


/********************************************************************************/
/*										*/
/*	Tab computation methods 						*/
/*										*/
/********************************************************************************/

@Override public float nextTabStop(float x,int offset) {
   setFont();

   //TODO: this doesn't work for variable space fonts
   int pos = (int)(x/font_width);
   int delta = ((int) x) % font_width;

   int npos = localNextTabPosition(pos);

   return font_width * npos + delta;
}



int nextTabPosition(int pos)
{
   setFont();

   return localNextTabPosition(pos);
}



private int localNextTabPosition(int pos)
{
   int npos = (pos + 1 + tab_size);
   int r = npos % tab_size;
   npos -= r;

   return npos;
}



/********************************************************************************/
/*										*/
/*	Font management methods 						*/
/*										*/
/********************************************************************************/

private void setFont()
{
   StyleContext ctx = BaleFactory.getFactory().getStyleContext();
   Font fn = ctx.getFont(cur_element.getAttributes());
   if (fn != last_font) {
      FontRenderContext frc = new FontRenderContext(null,false,false);
      Rectangle2D r = fn.getStringBounds("n",frc);
      LineMetrics lm = fn.getLineMetrics("NWMpq",frc);
      int ht = (int)(lm.getAscent() + rounding_value);
      ht += (int)(lm.getDescent() + lm.getLeading() + rounding_value);
      font_width = (int) r.getWidth();
      font_height = ht;
      last_font = fn;
      // Integer ivl = (Integer)(cur_element.getAttributes().getAttribute(BOARD_ATTR_TAB_SIZE));
      // if (ivl == null) tab_size = 0;
      // else tab_size = ivl;
      if (tab_size <= 0) tab_size = base_tab_size;
    }
}




}	// end of class BaleTabHandler




/* end of BaleTabHandler.java */

