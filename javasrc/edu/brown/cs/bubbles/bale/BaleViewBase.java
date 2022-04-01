/********************************************************************************/
/*										*/
/*		BaleViewBase.java						*/
/*										*/
/*	Bubble Annotated Language Editor basic abstract view			*/
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



package edu.brown.cs.bubbles.bale;


import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.CompositeView;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;

import edu.brown.cs.ivy.swing.SwingText;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;



abstract class BaleViewBase extends CompositeView implements BaleConstants.BaleView, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

protected ViewData []		view_data;
protected ViewData		our_data;
protected boolean		sizes_valid;
protected boolean		layout_valid;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BaleViewBase(BaleElement e)
{
   super(e);

   layout_valid = false;
   sizes_valid = false;
   our_data = new ViewData();
   view_data = new ViewData[0];
}



/********************************************************************************/
/*										*/
/*	Composite View common methods						*/
/*										*/
/********************************************************************************/

@Override protected void childAllocation(int idx,Rectangle alloc)
{
   computeLayout();

   if (alloc == null) return;

   alloc.x += view_data[idx].xOffset();
   alloc.y += view_data[idx].yOffset();
   alloc.width = view_data[idx].xSpan();
   alloc.height = view_data[idx].ySpan();
}



/********************************************************************************/
/*										*/
/*	Layout methods								*/
/*										*/
/********************************************************************************/

protected void invalidateLayout()
{
   BaleDocument bd = (BaleDocument) getDocument();
   bd.baleReadLock();
   try {
      layout_valid = false;
      sizes_valid = false;
      View v = getParent();
      if (v != null && v instanceof BaleViewBase) {
	 BaleViewBase pv = (BaleViewBase) v;
	 if (pv.layout_valid) pv.invalidateLayout();
       }
    }
   finally { bd.baleReadUnlock(); }
}



protected abstract void computeLayout();


@Override public float getHeightAtPriority(double p,float w)
{
   return getPreferredSpan(Y_AXIS);
}


@Override public float getWidthAtPriority(double p)
{
   return getPreferredSpan(X_AXIS);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

protected BaleElement getBaleElement()
{
   return (BaleElement) getElement();
}


protected BaleElement getBaleElement(View v)
{
   return (BaleElement) v.getElement();
}


protected BaleEditorPane getBaleEditorPane()
{
   return (BaleEditorPane) getContainer();
}



/********************************************************************************/
/*										*/
/*	View Data maintenance							*/
/*										*/
/********************************************************************************/

protected void setViewSizes()
{
   int n = getViewCount();

   if (n != view_data.length) {
      view_data = Arrays.copyOf(view_data,n);
    }

   for (int i = 0; i < n; ++i) {
      if (view_data[i] == null) view_data[i] = new ViewData();
      view_data[i].setSizes(getView(i));
    }
}




/********************************************************************************/
/*										*/
/*	Cursor movement routines						*/
/*										*/
/********************************************************************************/

@Override public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a,
					int direction, Position.Bias[] biasRet)
		throws BadLocationException
{
   biasRet[0] = Position.Bias.Forward;
   int origpos = pos;

   if (pos == -1) {		// special case
      switch (direction) {
	 case WEST :
	 case NORTH :
	    pos = Math.max(0,getEndOffset()-1);
	    break;
	 case EAST :
	 case SOUTH :
	    pos = getStartOffset();
	    break;
	 default:
	    throw new IllegalArgumentException("Bad direction: " + direction);
       }
    }
   else {
      switch (direction) {
	 case WEST :
	    pos = Math.max(0, pos - 1);
	    break;
	 case EAST :
	    pos = Math.min(pos + 1, getDocument().getLength()-1);
	    break;
	 case NORTH :
	 case SOUTH :
	    JTextComponent target = (JTextComponent) getContainer();
	    Caret c = target.getCaret();
	    Point mcp = null;
	    if (c != null) mcp = c.getMagicCaretPosition();
	    int x;
	    if (mcp != null) {
	       x = mcp.x;
             }
            else {
	       Rectangle2D loc = SwingText.modelToView2D(target,pos); 
	       x = (loc == null) ? 0 : (int) loc.getX();
	     }
	    if (BALE_PROPERTIES.getBoolean(BALE_DOES_DOCUMENT_MOVEMENT)) {
	       BaleDocument bd = (BaleDocument) target.getDocument();
	       int lno = bd.findLineNumber(pos);
	       if (direction == NORTH) findPositionInLine(target,lno-1,x);
	       else pos = findPositionInLine(target,lno+1,x);
	    }
	    else {
	       if (direction == NORTH) pos = findPosition(target, pos, false);
	       else pos = findPosition(target, pos, true);
	    }
	    if (pos < 0) pos = origpos;
	    break;
	 default:
	    throw new IllegalArgumentException("Bad direction: " + direction);
       }
    }
   return pos;
}

private int findPosition(JTextComponent c, int pos, boolean down) throws BadLocationException
{
   Rectangle initview = SwingText.modelToView2D(c,0);
   Rectangle posview = SwingText.modelToView2D(c,pos);
   int front, back;

   if (initview.x > 3) initview.x = 3;

   if (posview != null) {
      int dy = Math.max(posview.height,initview.height) + 2;
      if (down) {
	 front = SwingText.viewToModel2D(c,new Point(initview.x, posview.y + dy));
	 back = SwingText.viewToModel2D(c,new Point(initview.x, posview.y + 2*dy));
       }
      else {
	 // front = c.viewToModel(new Point(initview.x, posview.y - posview.height));
	 front = SwingText.viewToModel2D(c,new Point(initview.x, posview.y - posview.height));
	 back = SwingText.viewToModel2D(c,new Point(initview.x, posview.y));
       }

      if (front > back) return -1;
    }
   else return -1;

   int min = front;
   int max = back;
   int bestoff = -1;
   int bestval = 0;

   while (min <= max) {
      int mid = (min+max)/2;
      Rectangle r = SwingText.modelToView2D(c,mid);
      int span = Math.abs(r.x-posview.x);
      if (bestoff < 0 || bestval > span) {
	 bestval = span;
	 bestoff = mid;
       }
      if (r.x > posview.x) max = mid-1;
      else if (r.x+r.width < posview.x) min = mid+1;
      else break;
    }

   if (bestoff == pos) {
      if (down) bestoff = front;
      else bestoff = back;
   }

   return bestoff;
}

private int findPositionInLine(JTextComponent c,int lno,int x) throws BadLocationException
{
   BaleDocument bd = (BaleDocument) c.getDocument();
   int loff = bd.findLineOffset(lno);
   if (loff < 0) return -1;

   int noff = bd.findLineOffset(lno+1);
   int eoff = bd.getEndPosition().getOffset();
   if (noff >= eoff) noff = eoff-1;

   int min = loff;
   int max = noff;
   int bestoff = -1;
   int bestval = 0;

   while (min <= max) {
      int mid = (min+max)/2;
      Rectangle r = SwingText.modelToView2D(c,mid);
      int span = Math.abs(r.x-x);
      if (bestoff < 0 || bestval > span) {
	 bestval = span;
	 bestoff = mid;
       }
      if (r.x > x) max = mid-1;
      else if (r.x+r.width < x) min = mid+1;
      else break;
    }

   return bestoff;
}


/********************************************************************************/
/*										*/
/*	ViewData :: holder of information for a view (or ourself)		*/
/*										*/
/********************************************************************************/

protected static class ViewData {

   private float min_x;
   private float max_x;
   private float pref_x;
   private float min_y;
   private float max_y;
   private float pref_y;
   private int	 x_offset;
   private int	 y_offset;
   private float actual_height;
   private float actual_width;

   ViewData() {
      min_x = max_x = pref_x = 0;
      min_y = max_y = pref_y = 0;
      x_offset = y_offset = 0;
      actual_height = actual_width = 0;
    }

   float minWidth()			{ return min_x; }
   float maxWidth()			{ return max_x; }
   float preferredWidth()		{ return pref_x; }
   float minHeight()			{ return min_y; }
   float maxHeight()			{ return max_y; }
   float preferredHeight()		{ return pref_y; }

   float getActualWidth()		{ return actual_width; }
   float getActualHeight()		{ return actual_height; }

   int xOffset()			{ return x_offset; }
   int xSpan()				{ return (int) actual_width; }
   int yOffset()			{ return y_offset; }
   int ySpan()				{ return (int) actual_height; }

   void setSizes(View v) {
      if (v == null) return;
      try {
         min_x = v.getMinimumSpan(X_AXIS);
         min_y = v.getMinimumSpan(Y_AXIS);
         pref_x = v.getPreferredSpan(X_AXIS);
         pref_y = v.getPreferredSpan(Y_AXIS);
         max_x = v.getMaximumSpan(X_AXIS);
         max_y = v.getMaximumSpan(Y_AXIS);
       }
      catch (Throwable t) { }
    }

   void setSizeAtPriority(View v,double p,float w) {
      if (v instanceof BaleView && w > 0) {
	 BaleView bv = (BaleView) v;
	 actual_height = bv.getHeightAtPriority(p,w);
	 actual_width = w;
       }
      else {
	 actual_height = pref_y;
	 actual_width = pref_x;
       }
    }

   float setWidthAtPriority(View v,double p) {
      if (v instanceof BaleView && p > 0) {
	 BaleView bv = (BaleView) v;
	 actual_width = bv.getWidthAtPriority(p);
       }
      else actual_width = pref_x;

      return actual_width;
    }

   void setSizes(float minx,float miny,float maxx,float maxy) {
      min_x = minx;
      min_y = miny;
      max_x = maxx;
      max_y = maxy;
    }

   void setSizes(float minx,float miny,float maxx,float maxy,float prfx,float prfy) {
      setSizes(minx,miny,maxx,maxy);
      pref_x = prfx;
      pref_y = prfy;
    }

   void setPosition(int x,int y) {
      x_offset = x;
      y_offset = y;
    }

   void setActualSize(float w,float h) {
      actual_width = w;
      actual_height = h;
    }

}	// end of inner class ViewData



}	// end of class BaleViewBase




/* end of BaleViewBase.java */
