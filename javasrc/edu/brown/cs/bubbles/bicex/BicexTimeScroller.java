/********************************************************************************/
/*										*/
/*		BicexTimeScroller.java						*/
/*										*/
/*	Scroll bar over time for views						*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bicex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;

import edu.brown.cs.bubbles.board.BoardColors;


class BicexTimeScroller extends JScrollBar implements BicexConstants, AdjustmentListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BicexEvaluationViewer	for_bubble;
private int			last_user;
private boolean 		at_end;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexTimeScroller(BicexEvaluationViewer bbl)
{
   super(HORIZONTAL);
   for_bubble = bbl;
   last_user = -1;
   at_end = true;

   setOpaque(false);
   addAdjustmentListener(this);
   setToolTipText("Time Scroller");
   setValues(0,1,0,100);
}


void localDispose()
{
   removeAdjustmentListener(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

private BicexEvaluationContext getContext()
{
   return for_bubble.getContext();
}


long getRelevantTime(MouseEvent evt)
{
   int pos = evt.getX();	      // why does this seem to work?
   int minpos = 16;
   int maxpos = getWidth() - 16;
   double relpos = (pos - minpos);
   if (relpos < 0) relpos = 0;
   relpos /= (maxpos - minpos-1);
   double timer = getMinimum() + relpos * (getMaximum() - getMinimum()) + 0.5;
   long time = (long) timer;
   if (time > getMaximum()) time = getMaximum();

   return time;
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void update()
{
   BicexEvaluationContext ctx = for_bubble.getExecution().getCurrentContext();

   if (ctx == null) {
      setValues(100,1,0,100);
      at_end = true;
      last_user = -1;
    }
   else {
      int v = getValue();
      if (last_user > 0) v = last_user;
      if (v <= 0 || at_end) v = (int) ctx.getEndTime();

      if (v < ctx.getStartTime()) v = (int) ctx.getStartTime();
      if (v > ctx.getEndTime()) v = (int) ctx.getEndTime();
      setValues(v,1, (int) ctx.getStartTime(), (int) ctx.getEndTime());
    }
}



/********************************************************************************/
/*										*/
/*	Scrolling values							*/
/*										*/
/********************************************************************************/

@Override public void adjustmentValueChanged(AdjustmentEvent e)
{
   int v = e.getValue();
   last_user = v;
   if (v >= getMaximum()) at_end = true;
   else at_end = false;

   for_bubble.getExecution().setCurrentTime(v);
}




/********************************************************************************/
/*										*/
/*	Scrolling methods							*/
/*										*/
/********************************************************************************/

@Override public int getBlockIncrement(int dir)
{
   BicexEvaluationContext ctx = for_bubble.getExecution().getCurrentContext();
   if (ctx != null) {
      BicexValue bv = ctx.getValues().get("*LINE*");
      List<Integer> times = bv.getTimeChanges();
      long now = for_bubble.getExecution().getCurrentTime();
      long prev = -1;
      long next = -1;
      long cur = -1;
      for (Integer t : times) {
	 if (t <= now) {
	    prev = cur;
	    cur = t;
	  }
	 else if (next < 0) {
	    next = t;
	    break;
	  }
       }

      if (dir < 0 && prev > 0) {
	 return (int) (now-prev);
       }
      else if (dir > 0 && next > 0) {
	 return (int) (next-now);
       }
    }

   return super.getBlockIncrement(dir);
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paintComponent(Graphics g)
{
   Dimension sz = getSize();
   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   Graphics2D g2 = (Graphics2D) g.create();
   Color top = BoardColors.getColor(BICEX_EVAL_SCROLL_COLOR_PROP);
   g2.setColor(top);
   g2.fill(r);

   Collection<BicexEvaluationContext> inners = null;
   if (getContext() != null) inners = getContext().getInnerContexts();
   if (inners != null) {
      long start0 = getContext().getStartTime();
      long end0 = getContext().getEndTime();
      g2.setColor(BoardColors.getColor(BICEX_EVAL_SCROLL_CONTEXT_COLOR_PROP));
      for (BicexEvaluationContext ctx : inners) {
	 double start = ctx.getStartTime();
	 double end = ctx.getEndTime();
	 double v0 = (start-start0)/(end0-start0)*sz.getWidth();
	 double v1 = (end - start0)/(end0-start0)*sz.getWidth();
	 Shape r1 = new Rectangle2D.Double(v0,0,v1-v0,sz.height);
	 g2.fill(r1);
       }
    }
   super.paintComponent(g);
}



/********************************************************************************/
/*										*/
/*	Menu options								*/
/*										*/
/********************************************************************************/

void handlePopupMenu(JPopupMenu menu,MouseEvent evt)
{
   long time = getRelevantTime(evt);

   BicexEvaluationContext ctx = for_bubble.getExecution().getCurrentContext();
   if (ctx == null) return;

   if (ctx.getInnerContexts() != null) {
      for (BicexEvaluationContext sctx : ctx.getInnerContexts()) {
	 if (sctx.getStartTime() <= time && sctx.getEndTime() >= time) {
	    menu.add(for_bubble.getContextAction("Go to " + sctx.getShortName(),sctx));
	    break;
	 }
      }
    }

   BicexValue bv = for_bubble.getExecution().getCurrentContext().getValues().get("*LINE*");
   List<Integer> times = bv.getTimeChanges();
   long now = for_bubble.getExecution().getCurrentTime();
   long prev = -1;
   long next = -1;
   long cur = -1;
   for (Integer t : times) {
      if (t <= now) {
	 prev = cur;
	 cur = t;
       }
      else if (next < 0) {
	 next = t;
	 break;
       }
    }

   if (prev > 0) menu.add(for_bubble.getTimeAction("Go To Previous Line",prev+1));
   if (next > 0) menu.add(for_bubble.getTimeAction("Go To Next Line",next+1));
}



/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent evt)
{
   String rslt = null;

   int pos = evt.getX();
   int minpos = getX() + 16;
   int maxpos = getX() + getWidth() - 16;
   double relpos = (pos - minpos);
   if (relpos < 0) relpos = 0;
   relpos /= (maxpos - minpos);
   double timer = getMinimum() + relpos * (getMaximum() - getMinimum()) + 0.5;
   long time = (long) timer;
   if (time > getMaximum()) time = getMaximum();

   BicexEvaluationContext ctx = for_bubble.getExecution().getCurrentContext();
   if (ctx == null) return null;

   BicexValue bv = ctx.getValues().get("*LINE*");
   List<Integer> times = bv.getTimeChanges();
   String line = null;
   for (Integer t : times) {
      if (t <= time) {
	 line = bv.getStringValue(t+1);
	}
      else break;
     }
   if (line != null) rslt = "Line " + line;

   if (ctx != null) {
      String what = "In";
      if (ctx.getInnerContexts() != null) {
	 for (BicexEvaluationContext sctx : ctx.getInnerContexts()) {
	    if (sctx.getStartTime() <= time && sctx.getEndTime() >= time) {
	       ctx = sctx;
	       what = "Calling";
	       break;
	     }
	  }
       }
      if (rslt == null) rslt = what + " " + ctx.getMethod();
      else rslt += " " + what + " " + ctx.getShortName();
    }

   return rslt;
}



}	// end of class BicexTimeScroller




/* end of BicexTimeScroller.java */
















































































































