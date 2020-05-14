/********************************************************************************/
/*										*/
/*		BaleViewLineRegion.java 					*/
/*										*/
/*	Bubble Annotated Language Editor abstract view for horizontal region	*/
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



package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.event.DocumentEvent;
import javax.swing.text.StyleContext;
import javax.swing.text.TabableView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;



abstract class BaleViewLineRegion extends BaleViewBase implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected BaleTabHandler tab_handler;

private Rectangle	paint_rect;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleViewLineRegion(BaleElement elem)
{
   super(elem);

   tab_handler = new BaleTabHandler();
   paint_rect = new Rectangle();
}



/********************************************************************************/
/*										*/
/*	Basic methods for CompositeView 					*/
/*										*/
/********************************************************************************/

@Override protected boolean isBefore(int x,int y,Rectangle alloc)
{
   return x < alloc.x;
}



@Override protected boolean isAfter(int x,int y,Rectangle alloc)
{
   return x > alloc.x + alloc.width;
}



@Override protected View getViewAtPoint(int x,int y,Rectangle alloc)
{
   computeLayout();

   int n = getViewCount();

   for (int i = 0; i < n; i++) {
      if (y < (alloc.y + view_data[i].yOffset() + view_data[i].ySpan())) {
	 if (y < alloc.y + view_data[i].yOffset()) {
	    int j = (i > 0 ? i-1 : i);
	    childAllocation(j,alloc);
	    return getView(j);
	  }
	 if (x < (alloc.x + view_data[i].xOffset())) {
	    int j = (i == 0 ? 0 : i-1);
	    childAllocation(j, alloc);
	    return getView(j);
	  }
       }
    }
   childAllocation(n-1, alloc);
   return getView(n-1);
}



/********************************************************************************/
/*										*/
/*	Basic methods for View							*/
/*										*/
/********************************************************************************/

@Override public float getPreferredSpan(int axis)
{
   computeSizes();

   if (axis == X_AXIS) return our_data.getActualWidth();
   else return our_data.getActualHeight();
}



@Override public float getMinimumSpan(int axis)
{
   computeSizes();

   if (axis == X_AXIS) return our_data.minWidth();
   else return our_data.minHeight();
}



@Override public float getMaximumSpan(int axis)
{
   computeSizes();

   if (axis == X_AXIS) return our_data.maxWidth();
   else return our_data.maxHeight();
}



@Override public int getResizeWeight(int axis)
{
   if (our_data.minWidth() == our_data.maxWidth()) return 0;

   return 1;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override protected void forwardUpdate(DocumentEvent.ElementChange ec,
					  DocumentEvent e, Shape a, ViewFactory f)
{
   boolean wasvalid = layout_valid;

   super.forwardUpdate(ec, e, a, f);

   // determine if a repaint is needed
   if (wasvalid && !layout_valid) {
      // Repaint is needed because one of the tiled children
      // have changed their span along the major axis.	If there
      // is a hosting component and an allocated shape we repaint.
      Component c = getContainer();
      if ((a != null) && (c != null)) {
	 int pos = e.getOffset();
	 int index = getViewIndexAtPosition(pos);
	 Rectangle alloc = getInsideAllocation(a);
	 alloc.y += view_data[index].yOffset();
	 alloc.height -= view_data[index].yOffset();
	 c.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
       }
    }
}



@Override public void preferenceChanged(View child, boolean width, boolean height)
{
   invalidateLayout();

   super.preferenceChanged(child, width, height);
}




@Override public void replace(int idx,int len,View [] elems)
{
   super.replace(idx,len,elems);

   invalidateLayout();
}



/********************************************************************************/
/*										*/
/*	Size management methods 						*/
/*										*/
/********************************************************************************/

protected void computeSizes()
{
   if (sizes_valid) return;

   setViewSizes();

   int n = getViewCount();
   tab_handler.setElement(getBaleElement());

   float minx = 0;
   float miny = tab_handler.getFontHeight();
   float maxx = 0;
   float maxy = miny;
   float prfx = 0;
   float prfy = miny;
   float span1,span2,span3;

   for (int i = 0; i < n; ++i) {
      View v = getView(i);
      miny = Math.max(miny,view_data[i].minHeight());
      maxy = Math.max(maxy,view_data[i].maxHeight());
      prfy = Math.max(prfy,view_data[i].preferredHeight());
      if (v instanceof TabableView) {
	 TabableView tv = (TabableView) v;
	 span1 = tv.getTabbedSpan(minx,tab_handler);
	 if (minx == maxx) span2 = span1;
	 else span2 = tv.getTabbedSpan(maxx,tab_handler);
	 if (prfx == minx) span3 = span1;
	 else if (prfx == maxx) span3 = span2;
	 else span3 = tv.getTabbedSpan(prfx,tab_handler);
	 minx += span1;
	 maxx += span2;
	 prfx += span3;
       }
      else {
	 minx += view_data[i].minWidth();
	 maxx += view_data[i].maxWidth();
	 prfx += view_data[i].preferredWidth();
       }
    }
   if (maxy > Integer.MAX_VALUE) maxy = Integer.MAX_VALUE;

   our_data.setSizes(minx,miny,maxx,maxy,prfx,prfy);
   our_data.setActualSize(prfx,prfy);

   sizes_valid = true;
}



/********************************************************************************/
/*										*/
/*	Layout methods								*/
/*										*/
/********************************************************************************/

@Override protected void computeLayout()
{
   computeSizes();

   if (layout_valid) return;

   int n = getViewCount();

   float pos = 0;
   for (int i = 0; i < n; ++i) {
      View v = getView(i);
      float wd = 0;
      if (v instanceof TabableView) {
	 TabableView tv = (TabableView) v;
	 wd = tv.getTabbedSpan(pos,tab_handler);
       }
      else {
	 wd = view_data[i].preferredWidth();
       }
      view_data[i].setPosition((int) pos,0);
      view_data[i].setActualSize(wd,view_data[i].preferredHeight());
      v.setSize(view_data[i].getActualWidth(),view_data[i].getActualHeight());
      pos += wd;
    }

   adjustFontSizes(0,-1);

   our_data.setActualSize(pos,our_data.preferredHeight());

   layout_valid = true;
}




protected void adjustFontSizes(int sidx,int eidx)
{
   if (eidx < 0) eidx = getViewCount();

   float maxht = 0;
   int maxidx = -1;
   boolean delta = false;
   float base = 0;

   for (int i = sidx; i < eidx; ++i) {
      float ht = view_data[i].getActualHeight();
      if (maxidx < 0) {
	 maxidx = i;
	 maxht = ht;
	 base = view_data[i].yOffset();
       }
      else if (ht != maxht) {
	 delta = true;
	 if (ht > maxht) {
	    maxidx = i;
	    maxht = ht;
	    base = view_data[i].yOffset();
	  }
       }
    }

   if (!delta) return;

   Font ft0 = getFont(getView(maxidx));
   FontRenderContext frc = new FontRenderContext(null,false,false);
   LineMetrics lmn = ft0.getLineMetrics("Ap",frc);
   float fnht = lmn.getAscent();
   Font fn0 = null;
   int lastoffset = 0;

   for (int i = sidx; i < eidx; ++i) {
      Font fn = getFont(getView(i));
      if (fn == ft0) view_data[i].setPosition(view_data[i].xOffset(),(int) base);
      else {
	 if (fn != fn0) {
	    lmn = fn.getLineMetrics("Ap",frc);
	    float fht1 = lmn.getAscent();
	    lastoffset = (int)(base + fnht - fht1 + 0.5);
	    fn0 = fn;
	  }
	 view_data[i].setPosition(view_data[i].xOffset(),lastoffset);
       }
    }
}


private Font getFont(View v)
{
   StyleContext ctx = BaleFactory.getFactory().getStyleContext();
   Font fn = ctx.getFont(v.getElement().getAttributes());
   
   // need to scale the font here
   BudaBubble bb = BudaRoot.findBudaBubble(getBaleEditorPane());
   if (bb == null) return fn;
   double sf = bb.getScaleFactor();
   if (sf != 0 && sf != 1) {
      float sz = fn.getSize2D();
      sz *= sf;
      fn = fn.deriveFont(sz);
    }
 
   return fn;
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g,Shape allocation)
{
   Rectangle alloc;
   if (allocation instanceof Rectangle) alloc = (Rectangle) allocation;
   else alloc = allocation.getBounds();

   if (!layout_valid) {
      BaleViewBase root = null;
      for (root = this; root.getParent() instanceof BaleViewBase; root = (BaleViewBase) root.getParent());
      root.invalidateLayout();
      root.computeLayout();
   }

   computeLayout();

   int n = getViewCount();
   int x = alloc.x + getLeftInset();
   int y = alloc.y + getTopInset();
   Rectangle clip = g.getClipBounds();

   int crx0 = clip.x;
   int crx1 = crx0 + clip.width;
   int cry0 = clip.y;
   int cry1 = cry0 + clip.height;

   for (int i = 0; i < n; i++) {
      paint_rect.x = x + view_data[i].xOffset();
      paint_rect.y = y + view_data[i].yOffset();
      paint_rect.width = view_data[i].xSpan();
      paint_rect.height = view_data[i].ySpan();

      int trx0 = paint_rect.x;
      int trx1 = trx0 + paint_rect.width;
      int try0 = paint_rect.y;
      int try1 = try0 + paint_rect.height;

      // check if inside or adjacent to clip area
      if ((trx1 >= crx0) && (try1 >= cry0) && (crx1 >= trx0) && (cry1 >= try0)) {
	 View v = getView(i);
	 v.paint(g,paint_rect);
       }
    }
}




}	// end of class BaleViewLineRegion




/* end of BaleViewLineRegion.java */

