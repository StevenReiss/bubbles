/********************************************************************************/
/*										*/
/*		BaleViewBlock.java						*/
/*										*/
/*	Bubble Annotated Language Editor view for block element 		*/
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


import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JViewport;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.util.HashMap;
import java.util.Map;


class BaleViewBlock extends BaleViewBase implements BaleConstants.BaleView, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private float		last_width;
private float		last_height;
private double		last_priority;
private Rectangle	paint_rect;
private DrawStyle	draw_style;
private boolean 	have_errors;
private ElisionTrigger	elision_trigger;
private ExtractTrigger	extract_trigger;

private static final boolean annotate_cutline = false;


private static final int CHANGE_ELISION_DELTA = 20;
private static final double MIN_DELTA_PRIORITY = 0.01;

private static final int ELLIPSES_INDENT = 10;


enum DrawStyle {
   NORMAL,
   COMMENT,
   IGNORE,
   SHRUNK_EMPTY,
   SHRUNK_COMMENT,
   SHRUNK_ELIDED
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleViewBlock(BaleElement e)
{
   super(e);
   last_width = 0;
   last_height = 0;
   last_priority = 0;
   draw_style = DrawStyle.NORMAL;
   paint_rect = new Rectangle();
   have_errors = false;
   elision_trigger = new ElisionTrigger();
   extract_trigger = new ExtractTrigger();
}




/********************************************************************************/
/*										*/
/*	Basic methods for CompositeView 					*/
/*										*/
/********************************************************************************/

@Override protected boolean isBefore(int x,int y,Rectangle alloc)
{
   return y < alloc.y;
}



@Override protected boolean isAfter(int x,int y,Rectangle alloc)
{
   return y > alloc.y + alloc.height;
}



@Override protected View getViewAtPoint(int x,int y,Rectangle alloc)
{
   computeLayout();

   int n = getViewCount();

   for (int i = 0; i < n; i++) {
      if (y < (alloc.y + view_data[i].yOffset())) {
	 int j = (i == 0 ? 0 : i-1);
	 childAllocation(j, alloc);
	 return getView(j);
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
   computeLayout();

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
/*	Model-view modifications to handle elision				*/
/*										*/
/********************************************************************************/

@Override public Shape modelToView(int pos,Shape a,Position.Bias b)
	throws BadLocationException
{
   switch (draw_style) {
      default :
      case NORMAL :
      case COMMENT :
	 // BoardLog.logD("BALE","MODEL TO VIEW " + pos);
	 try {
	    return super.modelToView(pos,a,b);
	  }
	 catch (BadLocationException e) {
	    return super.modelToView(pos,a,b);		// try again for debugging
	  }
      case SHRUNK_COMMENT :
      case SHRUNK_ELIDED :
	 Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
	 Image img;
	 if (draw_style == DrawStyle.SHRUNK_COMMENT) img = BoardImage.getImage("comment_ellipses");
	 else img = BoardImage.getImage("ellipses");
	 ImageObserver obs = getContainer();
	 int x = alloc.x + getLeftInset() + ELLIPSES_INDENT;
	 int y = alloc.y + getTopInset();
	 int ht = alloc.height - getTopInset() - getBottomInset();
	 int iwd = img.getWidth(obs);
	 int iht = img.getHeight(obs);
	 if (iht < ht) y += (ht - iht)/2;
	 return new Rectangle(x,y,iwd,iht);
      case IGNORE :
      case SHRUNK_EMPTY :
	 alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
	 return new Rectangle(alloc.x,alloc.y,0,0);
    }
}



@Override protected View getViewAtPosition(int pos,Rectangle a)
{
   switch (draw_style) {
      default :
      case NORMAL :
      case COMMENT :
	 break;
      case SHRUNK_COMMENT :
      case SHRUNK_ELIDED :
      case IGNORE :
      case SHRUNK_EMPTY:
	 return null;
    }

   return super.getViewAtPosition(pos,a);
}



@Override public int viewToModel(float x,float y,Shape a,Position.Bias[] bias)
{
   switch (draw_style) {
      default :
      case NORMAL :
      case COMMENT :
	 return super.viewToModel(x,y,a,bias);
      case SHRUNK_COMMENT :
      case SHRUNK_ELIDED :
      case IGNORE :
      case SHRUNK_EMPTY :
	 break;
    }

   Rectangle alloc = getInsideAllocation(a);
   if (isBefore((int) x, (int) y, alloc)) {
      return super.viewToModel(x,y,a,bias);
    }
   else if (isAfter((int) x, (int) y, alloc)) {
      return super.viewToModel(x,y,a,bias);
    }
   else {
      return getElement().getStartOffset();
    }
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

   computeLayout();

   if (have_errors && BALE_PROPERTIES.getBoolean(BALE_ERROR_USE_BKG,true)) {
      Color c = g.getColor();
      g.setColor(BoardColors.getColor(BALE_ERROR_BACKGROUND_PROP));
      g.fillRect(alloc.x,alloc.y,alloc.width,alloc.height);
      g.setColor(c);
    }

   switch (draw_style) {
      case NORMAL :
	 paintNormal(g,alloc);
	 break;
      case COMMENT :
	 paintComment(g,alloc);
	 break;
      case IGNORE :
	 return;
      case SHRUNK_EMPTY :
	 return;
      case SHRUNK_COMMENT :
	 paintElidedComment(g,alloc);
	 break;
      case SHRUNK_ELIDED :
	 paintElided(g,alloc);
	 break;
    }
}



private void paintNormal(Graphics g,Rectangle alloc)
{
   int n = getViewCount();
   int x = alloc.x + getLeftInset();
   int y = alloc.y + getTopInset();
   Rectangle clip = g.getClipBounds();

   for (int i = 0; i < n; i++) {
      paint_rect.x = x + view_data[i].xOffset();
      paint_rect.y = y + view_data[i].yOffset();
      paint_rect.width = view_data[i].xSpan();
      paint_rect.height = view_data[i].ySpan();

      int trx0 = paint_rect.x;
      int trx1 = trx0 + paint_rect.width;
      int try0 = paint_rect.y;
      int try1 = try0 + paint_rect.height;

      int crx0 = clip.x;
      int crx1 = crx0 + clip.width;
      int cry0 = clip.y;
      int cry1 = cry0 + clip.height;

      // check if inside or adjacent to clip area
      if ((trx1 >= crx0) && (try1 >= cry0) && (crx1 >= trx0) && (cry1 >= try0)) {
	 View v = getView(i);
	 if (v != null) v.paint(g,paint_rect);
       }
    }

   if (getBaleElement().canElide()) {
      drawElisionTrigger(g,alloc,true);
    }

   if (isBuddable()) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setColor(BoardColors.getColor(BALE_BUD_LINE_COLOR_PROP));
      g2.setStroke(BALE_BUD_LINE_STROKE);
      g2.drawLine(alloc.x,alloc.y+alloc.height-1,alloc.x+alloc.width,alloc.y+alloc.height-1);

      if (annotate_cutline) {
	 ImageIcon scissors = (ImageIcon) BoardImage.getIcon("icon_scissors_off.png",15,8);
         Color bgc = BoardColors.getColor("Bale.BudImageBackground");
         Color fgc = BoardColors.getColor("Bale.BudImageColor");
	 g2.drawImage(scissors.getImage(),(int) (alloc.x+0.80*alloc.width), alloc.y+alloc.height-5,bgc,null);
	 g2.setColor(fgc);
	 g2.setFont(g2.getFont().deriveFont(8f));
	 g2.drawString("alt + c", alloc.x+alloc.width-43, alloc.y+alloc.height-2);
	 g2.drawString("alt + x", (int) (alloc.x+0.80*alloc.width)-30, alloc.y+alloc.height-2);
       }

      Image ic = BoardImage.getImage("tearoff");
      if (ic != null) {
	 BaleEditorPane bep = getBaleEditorPane();
	 int w = ic.getWidth(bep);
	 int h = ic.getHeight(bep);
	 int x0 = alloc.x + alloc.width - 2 - w;
	 int y0 = alloc.y + alloc.height - 1 - h/2;
	 if (checkDrawn(g,x0,y0,w,h)) {
	    g.drawImage(ic,x0,y0,bep);
	    bep.addActiveRegion(x0,y0,w,h,extract_trigger);
	 }
       }
    }
}



private boolean checkDrawn(Graphics g,int x0,int y0,int w,int h)
{
   Rectangle r = g.getClipBounds();
   Rectangle r1 = new Rectangle(x0,y0,w,h);
   return r1.intersects(r);
}



private void paintComment(Graphics g,Rectangle alloc)
{
   // TODO: might want to paint a comment differently

   paintNormal(g,alloc);
}



private void paintElided(Graphics g,Rectangle alloc)
{
   // TODO: paint a SeeSoft style view of the elided lines

   BaleEditorPane bep = getBaleEditorPane();
   Image img = BoardImage.getImage("ellipses");

   int x = alloc.x + getLeftInset() + ELLIPSES_INDENT;
   int y = alloc.y + getTopInset();
   int ht = alloc.height - getTopInset() - getBottomInset();
   // int iwd = img.getWidth(bep);
   int iht = img.getHeight(bep);
   if (iht < ht) y += (ht - iht)/2;

   g.drawImage(img,x,y,bep);

   drawElisionTrigger(g,alloc,false);

   if (checkDrawn(g,x,y,img.getWidth(bep),ht)) {
      bep.addActiveRegion(x + BALE_ELLIPSES_INSIDE_DELTA,y,
	       img.getWidth(bep) - 2 * BALE_ELLIPSES_INSIDE_DELTA,ht,null);
   }
}



private void paintElidedComment(Graphics g,Rectangle alloc)
{
   BaleEditorPane bep = getBaleEditorPane();
   Image img = BoardImage.getImage("comment_ellipses");

   int x = alloc.x + getLeftInset() + ELLIPSES_INDENT;
   int y = alloc.y + getTopInset();
   int ht = alloc.height - getTopInset() - getBottomInset();
   // int iwd = img.getWidth(bep);
   int iht = img.getHeight(bep);
   if (iht < ht) y += (ht - iht)/2;

   g.drawImage(img,x,y,bep);

   drawElisionTrigger(g,alloc,false);

   if (checkDrawn(g,x,y,img.getWidth(bep),ht)) {
      bep.addActiveRegion(x + BALE_ELLIPSES_INSIDE_DELTA,y,
	       img.getWidth(bep) - 2 * BALE_ELLIPSES_INSIDE_DELTA,ht,null);
   }
}


private boolean isBuddable()
{
   BaleElement be = getBaleElement();
   if (be.getBubbleType() == BaleFragmentType.NONE) return false;
   BaleElement par = be.getBaleParent();
   // if (par.getBaleParent() != null) return false;

   boolean fnd = false;
   int n = par.getChildCount();
   for (int i = 0; i < n; ++i) {
      BaleElement cbe = par.getBaleElement(i);
      if (cbe == be) fnd = true;
      else if (fnd) {
	 if (cbe.getBubbleType() != BaleFragmentType.NONE) return true;
       }
    }

   return false;
}



private void drawElisionTrigger(Graphics g,Rectangle alloc,boolean fg)
{
   BaleEditorPane bep = getBaleEditorPane();
   Image ic;

   if (fg) ic = BoardImage.getImage("elideminus");
   else ic = BoardImage.getImage("elideplus");

   int x0 = 0;
   int y0 = alloc.y + getTopInset();
   int w = ic.getWidth(bep);
   int h = ic.getHeight(bep);
   g.drawImage(ic,x0,y0,bep);

   if (checkDrawn(g,x0,y0,w,h))
      bep.addActiveRegion(x0,y0,w,h,elision_trigger);
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void setSize(float w,float h)
{
   if (w != last_width || h != last_height) {
      last_width = w;
      last_height = h;
      if (isTopNode()) invalidateLayout();
    }
}




@Override protected void forwardUpdate(DocumentEvent.ElementChange ec,
					  DocumentEvent e, Shape a, ViewFactory f)
{
   // if (a == null) return;

   boolean wasvalid = layout_valid;

   try {
      super.forwardUpdate(ec, e, a, f);
    }
   catch (Throwable t) {
      BoardLog.logE("BALE","Problem with event updating for " + ec + " " + e + " " + e.getOffset() + " " +
		       e.getLength() + " " + getBaleElement(),t);
      return;
    }

   if (isTopNode()) {
      BaleDocument bd = (BaleDocument) getDocument();
      boolean haveerr = false;
      for (BumpProblem bp : bd.getProblems()) {
	 if (bp.getErrorType() == BumpErrorType.FATAL || bp.getErrorType() == BumpErrorType.ERROR) {
	    haveerr = true;
	  }
       }
      if (haveerr != have_errors) {
	 have_errors = haveerr;
	 Component c = getContainer();
	 c.repaint();
       }
    }

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
   // BaleDocument bd = getBaleElement().getBaleDocument();
   // BoardLog.logD("BALE","VIEW REPLACE " + getBaleElement().getName() + " " + idx + " " + len + " " + elems.length + " " + bd);
   // bd.checkWriteLock();	// might be called during initialization without lock

   super.replace(idx,len,elems);

   invalidateLayout();
}



/********************************************************************************/
/*										*/
/*	BaleView methods							*/
/*										*/
/********************************************************************************/

@Override public float getHeightAtPriority(double p,float w)
{
   computeSizes();

   tryLayout(p,w);

   last_width = our_data.getActualWidth();
   last_height = our_data.getActualHeight();
   layout_valid = true;

   return last_height;
}


@Override public float getWidthAtPriority(double p)
{
   computeSizes();

   return our_data.preferredWidth();
}



// TODO: It would be nice to have a range of elisions for a view.  This would allow
//    different levels of compaction.  Might want to do real compact if < priority/2,
//    somewhat compact if < priority (or some such scheme).




/********************************************************************************/
/*										*/
/*	Top level layout methods						*/
/*										*/
/********************************************************************************/

@Override protected void computeLayout()
{
   if (sizes_valid && layout_valid) return;

   BaleDocument bd = (BaleDocument) getDocument();
   if (isTopNode()) {
      switch (bd.getElideMode()) {
	 case ELIDE_CHECK_ALWAYS :
	 case ELIDE_CHECK_ONCE :
	    Container tc = getContainer();
	    JViewport vp = null;
	    for (int i = 0; i < 2; ++i) {
	       tc = tc.getParent();
	       if (tc == null || tc instanceof JViewport) {
		  vp = (JViewport) tc;
		  break;
		}
	     }
	    if (vp != null) {
	       Dimension d = vp.getExtentSize();
	       if (d.width != 0) {
		  Dimension dw = getContainer().getSize();
		  setSize(dw.width,d.height);
		}
	     }
	    break;
	 default:
	    break;
       }
    }

   bd.baleReadLock();
   try {
      computeSizes();
      if (!layout_valid) {
	 doLayout();
	 layout_valid = true;
       }
    }
   finally { bd.baleReadUnlock(); }
}



private void computeSizes()
{
   if (sizes_valid) return;

   setViewSizes();

   int n = getViewCount();

   float minx = BALE_MIN_WIDTH;
   float miny = 0;
   float maxx = BALE_MIN_WIDTH;
   float maxy = 0;

   for (int i = 0; i < n; ++i) {
      minx = Math.max(minx,view_data[i].minWidth());
      maxx = Math.max(maxx,view_data[i].maxWidth());
      miny += view_data[i].minHeight();
      maxy += view_data[i].maxHeight();
    }
   if (maxy > Integer.MAX_VALUE) maxy = Integer.MAX_VALUE;

   BaleElement be = getBaleElement();
   if (be.getBaleParent() != null) {
      // internal elements are collapsible
      if (be.isEmpty()) miny = BALE_EMPTY_HEIGHT;
      else if (be.isComment()) miny = BALE_COMMENT_HEIGHT;
      else miny = BALE_ELLIPSES_HEIGHT;
    }
   if (getParent() == null || !(getParent() instanceof BaleViewBlock)) {
      maxy = Math.max(BALE_MIN_HEIGHT,maxy);
    }

   our_data.setSizes(minx,miny,maxx,maxy);

   sizes_valid = true;
}



/********************************************************************************/
/*										*/
/*	Block layout methods							*/
/*										*/
/********************************************************************************/

private void doLayout()
{
   if (last_width == 0 || last_width == Integer.MAX_VALUE) {
      tryLayout(0.0,0);
      return;
    }

   tryLayout(last_priority,last_width);

   if (isTopNode() && last_height != 0 &&
	  getBaleElement().getElideMode() != BaleElideMode.ELIDE_NONE) {
      if (our_data.getActualHeight() > last_height) {
	 findPriority(last_priority,1,last_width);
       }
      else if (last_height - our_data.getActualHeight() > CHANGE_ELISION_DELTA &&
		  last_priority > 0) {
	 findPriority(0,last_priority,last_width);
       }
    }
}




private void findPriority(double p0,double p1,float w)
{
   while (p1-p0 > MIN_DELTA_PRIORITY) {
      double p2 = (p0+p1)/2;
      tryLayout(p2,w);
      if (our_data.getActualHeight() > last_height) {
	 p0 = p2;
	 if (p1-p0 < MIN_DELTA_PRIORITY) {
	    tryLayout(p1,w);		// give up and use old max
	  }
       }
      else if (last_height - our_data.getActualHeight() > CHANGE_ELISION_DELTA &&
		  last_priority < 1) {
	 p1 = p2;
       }
      else break;
    }
}



private void tryLayout(double priority,float width)
{
   int n = getViewCount();

   float wid = width;
   if (wid == 0) wid = BALE_MIN_WIDTH;
   wid -= getLeftInset() + getRightInset();

   int ht = getTopInset();

   double p0 = getPriority();

   BaleElement be = getBaleElement();

   setDrawStyle(p0 < priority);

   switch (draw_style) {
      case SHRUNK_EMPTY :
	 ht = BALE_EMPTY_HEIGHT;
	 break;
      case SHRUNK_COMMENT :
	 ht = BALE_COMMENT_HEIGHT;
	 break;
      case SHRUNK_ELIDED :
	 ht = BALE_ELLIPSES_HEIGHT;
	 break;
      case NORMAL :
      case COMMENT :
	 if (be.isComment()) draw_style = DrawStyle.COMMENT;
	 else draw_style = DrawStyle.NORMAL;
	 for (int i = 0; i < n; ++i) {
	    View v = getView(i);
	    view_data[i].setSizeAtPriority(v,priority,width);
	    view_data[i].setPosition(0,ht);
	    wid = Math.max(wid,view_data[i].getActualWidth());
	    ht += view_data[i].getActualHeight();
	    v.setSize(view_data[i].getActualWidth(),view_data[i].getActualHeight());
	  }
	 break;
      default:
	 break;
    }

   our_data.setActualSize(wid,ht);
   last_priority = priority;
   layout_valid = true;
}



private void setDrawStyle(boolean fg)
{
   BaleElement be = getBaleElement();

   if (!be.canElide()) fg = false;

   switch (be.getElideMode()) {
      case ELIDE_CHECK_NEVER :
	 fg = be.isElided();
	 break;
      case ELIDE_CHECK_ONCE :
      case ELIDE_CHECK_ALWAYS :
	 break;
      case ELIDE_NONE :
	 fg = false;
	 break;
    }
   be.setElided(fg);

   if (fg) {
      if (be.isEmpty()) draw_style = DrawStyle.SHRUNK_EMPTY;
      else if (be.isComment()) draw_style = DrawStyle.SHRUNK_COMMENT;
      else draw_style = DrawStyle.SHRUNK_ELIDED;
    }
   else {
      if (be.isComment()) draw_style = DrawStyle.COMMENT;
      else draw_style = DrawStyle.NORMAL;
    }
}



private boolean isTopNode()
{
   BaleElement be = getBaleElement();
   if (be.getBaleParent() == null) return true;

   return false;
}



/********************************************************************************/
/*										*/
/*	Priority computation							*/
/*										*/
/********************************************************************************/

private double getPriority()
{
   double p0 = 1.0;
   BaleElement be = getBaleElement();
   BaleAstNode ban = be.getAstNode();

   if (ban != null) p0 = ban.getElidePriority();

   if (be.isComment()) {
      p0 *= BALE_COMMENT_PRIORITY;
    }

   JTextComponent tc = (JTextComponent) getContainer();
   Caret c = tc.getCaret();
   int soff = c.getDot();
   Map<BaleElement,Double> pmap = new HashMap<BaleElement,Double>();
   if (soff != 0) {
      BaleDocument bd = (BaleDocument) getDocument();
      BaleElement ce = bd.getActualCharacterElement(soff);
      double px = 1.0;
      while (ce != null) {
	 pmap.put(ce,px);
	 px *= BALE_CARET_UP_PRIORITY;
	 ce = ce.getBaleParent();
       }
      double py = 1.0;
      for (ce = be; ce != null; ce = ce.getBaleParent()) {
	 Double dv = pmap.get(ce);
	 if (dv != null) {
	    double p1 = py * dv;
	    double p2 = Math.pow(0.99,Math.abs(be.getStartOffset()-ce.getStartOffset()));
	    p1 *= p2;
	    if (p1 > p0)
	       p0 = p1;
	    break;
	  }
	 py *= BALE_CARET_DOWN_PRIORITY;
       }
    }

   return p0;
}




/********************************************************************************/
/*										*/
/*	Action to handle elision events 					*/
/*										*/
/********************************************************************************/

private class ElisionTrigger implements RegionAction {

   @Override public void handleClick(MouseEvent e) {
      BaleEditorPane bep = getBaleEditorPane();
      if (bep == null) return;
      BaleDocument bd = bep.getBaleDocument();
   
      bd.baleWriteLock();
      try {
         Point pt = new Point(e.getX(),e.getY());
         int pos = SwingText.viewToModel2D(bep,pt);
         // pos = bep.viewToModel2D(pt);
         if (pos < 0) return;
   
         BaleElement be = bd.getCharacterElement(pos);
         if (!be.isElided()) {
            for ( ; be != null && !be.canElide(); be = be.getBaleParent()) ;
            if (be == null) return;
          }
         boolean fg = be.isElided();
         be.setElided(!fg);
         bd.handleElisionChange();
         if (fg) bep.increaseSizeForElidedElement(be);
         bep.setCaretPosition(be.getStartOffset());
         if (fg) BoardMetrics.noteCommand("BALE","ClickUnElision");
         else BoardMetrics.noteCommand("BALE","ClickElision");
         BaleEditorBubble.noteElision(bep);
       }
      finally { bd.baleWriteUnlock(); }
    }

   @Override public BudaBubble handleHoverBubble(MouseEvent e)	      { return null; }

}	// end of inner class ElisionTrigger



private class ExtractTrigger implements RegionAction {

   @Override public void handleClick(MouseEvent e) {
      BaleEditorPane bep = getBaleEditorPane();
      if (bep == null) return;
      Point pt = new Point(e.getX(),e.getY());
      int pos = SwingText.viewToModel2D(bep,pt);
      if (pos < 0) return;
   
      bep.setCaretPosition(pos);
      Action act = BaleEditorKit.findAction("FragmentAction");
      ActionEvent ae = new ActionEvent(bep,ActionEvent.ACTION_FIRST,"FragmentAction");
      if (act != null) act.actionPerformed(ae);
   
      // TODO: Might want to do this explicitly rather than through the action
    }

   @Override public BudaBubble handleHoverBubble(MouseEvent e) {
      BaleEditorPane bep = getBaleEditorPane();
      if (bep == null) return null;
      Point pt = new Point(e.getX(),e.getY());
      int pos = SwingText.viewToModel2D(bep,pt);
      if (pos < 0) return null;
      BaleFragmentEditor bfe = BaleBudder.findFragmentBubble(bep,pos);
      if (bfe == null) return null;
      return new BaleEditorBubble(bfe);
    }

}	// end of inner class ExtractTrigger





}	// end of class BaleViewBlock




/* end of BaleViewBlock.java */
