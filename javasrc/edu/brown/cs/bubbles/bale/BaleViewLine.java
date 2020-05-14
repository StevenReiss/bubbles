/********************************************************************************/
/*										*/
/*		BaleViewLine.java						*/
/*										*/
/*	Bubble Annotated Language Editor view for lines 			*/
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


import javax.swing.text.View;

import java.util.ArrayList;
import java.util.List;


class BaleViewLine extends BaleViewLineRegion implements BaleConstants.BaleView, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private float		last_width;
private float		last_height;

private static BreakElement [] break_table = new BreakElement[] {

   new BreakElement("SEMICOLON","*","T",0.1),
   new BreakElement("SEMICOLON","*","F",0.15),
   new BreakElement("COMMA","*","T",0.15),
   new BreakElement("COMMA","*","F",0.20),
   new BreakElement("RPAREN","*","T",0.3),
   new BreakElement("RPAREN","*","*",0.8),
   new BreakElement("RBRACE","*","T",0.1),
   new BreakElement("RBRACE","*","*",0.3),
   new BreakElement("RBRACKET","*","T",0.1),
   new BreakElement("RBRACKET","*","*",0.3),
   new BreakElement("OP","*","T",0.35),
   new BreakElement("EQUAL","*","*",0.25),
   new BreakElement("LANGLE","*","T",0.35),
   new BreakElement("RANGLE","*","T",0.35),
   new BreakElement("DOT","*","F",0.9),
   new BreakElement("KEYWORD","*","*",0.8),
   new BreakElement("TYPEKEY","*","*",0.8),
   new BreakElement("RETURN","*","*",0.8),
   new BreakElement("IF","*","*",0.8),
   new BreakElement("DO","*","*",0.8),
   new BreakElement("FOR","*","*",0.8),
   new BreakElement("TRY","*","*",0.8),
   new BreakElement("CASE","*","*",0.8),
   new BreakElement("ELSE","*","*",0.8),
   new BreakElement("ENUM","*","*",0.8),
   new BreakElement("GOTO","*","*",0.8),
   new BreakElement("BREAK","*","*",0.8),
   new BreakElement("CATCH","*","*",0.8),
   new BreakElement("CLASS","*","*",0.8),
   new BreakElement("WHILE","*","*",0.8),
   new BreakElement("DEFAULT","*","*",0.8),
   new BreakElement("FINALLY","*","*",0.8),
   new BreakElement("INTERFACE","*","*",0.8),

   new BreakElement("*","*","T",0.4),
   new BreakElement("*","*","*",0.5)

};

private static int MAX_LINE_ELEMENTS = 50;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleViewLine(BaleElement e)
{
   super(e);

   last_width = 0;
   last_height = 0;
}



/********************************************************************************/
/*										*/
/*	Basic methods for View							*/
/*										*/
/********************************************************************************/

@Override public float getPreferredSpan(int axis)
{
   computeLayout();

   return super.getPreferredSpan(axis);
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void setSize(float w,float h)
{
   super.setSize(w,h);

   if (w != 0) {
      if (w != last_width || h != last_height) {
	 last_width = w;
	 last_height = h;
	 invalidateLayout();
       }
    }
}




/********************************************************************************/
/*										*/
/*	BaleView methods							*/
/*										*/
/********************************************************************************/

@Override public float getHeightAtPriority(double p,float w)
{
   computeSizes();
   int n = getViewCount();

   getBaleElement().setReflowCount(0);

   // first see if things fit
   if (our_data.preferredWidth() <= w) return our_data.preferredHeight();

   // next check if things fit at this priority level
   tab_handler.setElement(getBaleElement());
   float ht = tab_handler.getFontHeight();
   float wd = 0;

   for (int i = 0; i < n; ++i) {
      View v = getView(i);
      ht = Math.max(ht,view_data[i].preferredHeight());
      wd += view_data[i].setWidthAtPriority(v,p);
    }
   if (wd <= w) return our_data.preferredHeight();

   // if this failed, remove any elisions
   for (int i = 0; i < n; ++i) {
      float wid = view_data[i].preferredWidth();
      float h = view_data[i].preferredHeight();
      view_data[i].setActualSize(wid,h);
      View v = getView(i);
      v.setSize(wid,h);
    }

   // otherwise we have to reflow (wrap) the line
   ht = computeBestLineSplit(p,w);

   last_width = w;
   last_height = ht;

   layout_valid = true;

   return ht;
}



private float computeBestLineSplit(double p,float w)
{
   // compute best split line data
   SplitLine spl0 = new SplitLine();
   SplitLine spl = null;

   switch (getBaleElement().getBaleDocument().getSplitMode()) {
      case SPLIT_NEVER :
	 break;
      case SPLIT_NORMAL :
	 spl = splitLine(w,spl0,100,true);
	 break;
      case SPLIT_QUICK :
	 spl = splitLine(w,spl0,100,false);
	 break;
    }

   if (spl == null) spl = spl0;

   getBaleElement().setReflowCount(spl.getLineCount()-1);

   // next assign positions based on the result
   float ht = spl.assignPositions();

   // finally set our size
   our_data.setActualSize(w,ht);

   return ht;
}


@Override public float getWidthAtPriority(double p)
{
   computeSizes();

   return our_data.preferredWidth();
}




/********************************************************************************/
/*										*/
/*	Searching implementation						*/
/*										*/
/********************************************************************************/

private SplitLine splitLine(float w,SplitLine spl,int maxlines,boolean opt)
{
   //TODO: if only entry left is a blank line, remove the leading spaces

   int n = getViewCount();
   if (n > MAX_LINE_ELEMENTS) opt = false;
   
   BaleDocument bd = (BaleDocument) getDocument();

   if (spl.getTotalWidth() <= w) return spl;	// current split works

   int lastok = -1;
   float totwd = spl.getStartPosition();
   for (int i = spl.getStartIndex(); i < n; ++i) {
      View v = getView(i);
      BaleElement be = getBaleElement(v);
      totwd += view_data[i].preferredWidth();
      if (totwd <= w || (lastok == i-1 && be.isEmpty())) lastok = i;
      else break;
    }

   if (lastok == n-1) {
      // spaces causing overflow -- ignore
      return spl;
    }

   // null is returned if splits we know there are better splits
   if (spl.getLineCount() >= maxlines) return null;

   float space = tab_handler.getSpaceWidth();

   if (spl.getStartIndex() == 0) {
      View v = getView(0);
      BaleElement be = getBaleElement(v);
      if (be.isEmpty()) {
	 float wd = view_data[0].preferredWidth();
	 float mxwd = BALE_MAX_INITIAL_REFLOW_INDENT * space;
	 if (wd > mxwd) {
	    spl.setInitialSpace(mxwd);
	    SplitLine nspl = splitLine(w,spl,maxlines,opt);
	    if (nspl == null) nspl = spl;
	    return nspl;
	  }
	 else if (lastok == 0) lastok = -1;
       }
    }


   if (lastok < 0) {
      // TODO: need to split subelement
      // this should be done by splitting the view into two views based on the
      // cut point and then trying again
      return spl;
    }

   SplitLine bestsplit = null;

   // Index i should be first token of split line
   // It should not be a space

   for (int i = lastok+1; i > spl.getStartIndex(); --i) {
      // element i is a potential candidate to split after (or in the middle of)
      View v = getView(i);
      BaleElement be = getBaleElement(v);

      if (!be.isEmpty()) {
	 int soff = be.getStartOffset();
	 if (soff < 0) continue;
	 int ind = bd.getSplitIndent(soff);
	 if (ind > BALE_MAX_REFLOW_INDENT) ind = BALE_MAX_REFLOW_INDENT;
	 float wd = ind * space;
	 SplitLine nspl = new SplitLine(spl,i,wd);
	 nspl = splitLine(w,nspl,maxlines,opt);      // find best split of the rest of line
	 if (nspl == null) break;	   // too far to left -- better splits to the right
	 if (nspl.getTotalWidth() > spl.getTotalWidth()) {
	    // earlier break moves things further to the right -- might want to reset indentation
	    continue;
	  }
	 maxlines = Math.min(maxlines,nspl.getLineCount());
	 if (nspl.isBetterThan(bestsplit)) bestsplit = nspl;
	 if (!opt) break;
       }
    }

   // if (bestsplit == null) {
   //	 // nothing found
   //	 return spl;
   //  }

   return bestsplit;
}



/********************************************************************************/
/*										*/
/*	Utilitiy methods							*/
/*										*/
/********************************************************************************/

private float computeWidth(int sidx,int eidx)
{
   if (eidx < 0) eidx = getViewCount();


   float totwd = 0;
   float xwid = 0;
   for (int i = sidx; i < eidx; ++i) {
      View v = getView(i);
      BaleElement be = getBaleElement(v);
      if (be.isEmpty()) {
	 xwid += view_data[i].preferredWidth();
       }
      else {
	 totwd += xwid + view_data[i].preferredWidth();
	 xwid = 0;
       }
    }

   return totwd;
}




private float setPositions(float y0,float x0,int sidx,int eidx)
{
   float ht = 0;
   float x = x0;

   if (eidx < 0) eidx = getViewCount();

   for (int i = sidx; i < eidx; ++i) {
      float vh = view_data[i].preferredHeight();
      float vw = view_data[i].preferredWidth();

      view_data[i].setPosition((int) x,(int) y0);

      ht = Math.max(ht,vh);
      x += vw;
    }

   adjustFontSizes(sidx,eidx);

   return ht;
}




/********************************************************************************/
/*										*/
/*	Reflow cost computation 						*/
/*										*/
/********************************************************************************/

private double computeCost(int sidx)
{
   // returns approximate cost for splitting at the given index
   // a cost of 1 is worst, a cost of 0 best
   //TODO: code this as a table

   BaleElement pbe = null;
   BaleElement nbe = null;
   boolean havespace = false;

   View v = getView(sidx);
   nbe = getBaleElement(v);
   if (nbe != null && !nbe.isLeaf()) nbe = nbe.getFirstChild();
   if (nbe != null) pbe = nbe.getPreviousCharacterElement();

   while (nbe != null && nbe.isEmpty()) {
      havespace = true;
      if (nbe.isEndOfLine()) {
	 nbe = null;
	 break;
       }
      else nbe = nbe.getNextCharacterElement();
    }

   while (pbe != null && pbe.isEmpty()) {
      havespace = true;
      if (pbe.isEndOfLine()) {
	 pbe = null;
	 break;
       }
      else pbe = pbe.getPreviousCharacterElement();
    }

   if (nbe == null || pbe == null) return 0;

   if (nbe.isComment() || pbe.isComment()) return 0;
   // BaleAstNode nast = getAstNode(nbe);
   // BaleAstNode past = getAstNode(pbe);
   // int astdelta = getAstDelta(nast,past);

   // handle cases that are bad due to faulty tokenization
   switch (pbe.getTokenType()) {
      case QUESTIONMARK :
      case OP :
      case EQUAL :
      case LANGLE :
      case RANGLE :
	 if (!havespace) {
	    switch (nbe.getTokenType()) {
	       case OP :
	       case EQUAL :
	       case LANGLE :
	       case RANGLE :
		  return 1;
	       default:
		  break;
	     }
	  }
	 break;
      default:
	 break;
    }

   for (BreakElement be : break_table) {
      if (be.match(pbe.getTokenType(),nbe.getTokenType(),havespace))
	 return be.getValue();
    }

   return 0.5;
}


@SuppressWarnings("unused")
private BaleAstNode getAstNode(BaleElement be)
{
   while (be != null) {
      BaleAstNode ast = be.getAstNode();
      if (ast != null) return ast;
      be = be.getBaleParent();
    }

   return null;
}



@SuppressWarnings("unused")
private int getAstDelta(BaleAstNode ast0,BaleAstNode ast1)
{
   while (ast0 != null && ast0.getIdType() != BaleAstIdType.NONE) {
      ast0 = ast0.getParent();
    }
   while (ast1 != null && ast1.getIdType() != BaleAstIdType.NONE) {
      ast1 = ast1.getParent();
    }

   if (ast0 == null || ast1 == null) return -1;
   if (ast0 == ast1) return 0;

   int pos1 = ast1.getStart();
   int ct0 = 0;
   while (ast0 != null) {
      if (ast0.getStart() <= pos1 && ast0.getEnd() > pos1) break;
      ++ct0;
      ast0 = ast0.getParent();
    }
   int ct1 = 0;
   while (ast1 != null && ast1 != ast0) {
      ++ct1;
      ast1 = ast1.getParent();
    }

   return Math.max(ct0,ct1);
}




/********************************************************************************/
/*										*/
/*	Class for representing a split line					*/
/*										*/
/********************************************************************************/

private class SplitLine {

   private int start_index;			// index of starting view
   private float start_position;		// starting indentation
   private float line_width;			// width of rest of views on this line
   private List<SplitData> line_splits; 	// prior splits
   private double view_cost;
   private double balance_cost;

   SplitLine() {
      start_index = 0;
      start_position = 0;
      line_splits = new ArrayList<SplitData>(4);
      line_width = computeWidth(0,-1);
      view_cost = 0;
      balance_cost = 0;
    }

   SplitLine(SplitLine spl,int start,float pos) {
      start_index = start;
      start_position = pos;
      line_splits = new ArrayList<SplitData>(spl.line_splits);
      SplitData spd = new SplitData(spl.start_index,start,spl.start_position);
      line_splits.add(spd);
      line_width = computeWidth(start,-1);
      view_cost = spl.view_cost + computeCost(start);
      computeBalanceCost();
    }

   float getTotalWidth()		{ return start_position + line_width; }
   int getStartIndex()			{ return start_index; }
   float getStartPosition()		{ return start_position; }
   int getLineCount()			{ return 1+line_splits.size(); }

   private void computeBalanceCost() {
      double w = line_width + start_position;
      balance_cost = w*w;
      for (SplitData sd : line_splits) {
	 w = sd.getWidth() + sd.getStartIndent();
	 balance_cost += w*w;
       }
    }

   boolean isBetterThan(SplitLine spl) {
      if (spl == null) return true;
      int i = line_splits.size() - spl.line_splits.size();
      if (i < 0) return true;
      else if (i > 0) return false;
      if (view_cost < spl.view_cost) return true;
      else if (view_cost > spl.view_cost) return false;
      if (balance_cost < spl.balance_cost) return true;
      if (balance_cost > spl.balance_cost) return false;
      return false;
    }

   float assignPositions() {
      float y = 0;
      for (SplitData sd : line_splits) {
	 y += setPositions(y,sd.getStartIndent(),sd.getIndex(),sd.getEndIndex());
       }
      y += setPositions(y,start_position,start_index,-1);

      return y;
    }

   void setInitialSpace(float w) {
      start_index += 1;
      start_position = w;
    }

}	// end of subclass SplitLine



private class SplitData {

   private int split_start;		// index for split
   private int split_end;		// index after end of split
   private float start_position;	// indented start for line
   private float line_width;		// width of the tokens used in this line

   SplitData(int idx,int eidx,float spos) {
      split_start = idx;
      split_end = eidx;
      start_position = spos;
      line_width = computeWidth(idx,eidx);
    }

   int getIndex()			{ return split_start; }
   int getEndIndex()			{ return split_end; }
   float getWidth()			{ return line_width; }
   float getStartIndent()		{ return start_position; }

}	// end of inner class SplitData



/********************************************************************************/
/*										*/
/*	Element for break tables						*/
/*										*/
/********************************************************************************/

private static class BreakElement {

   private BaleTokenType prev_token;
   private BaleTokenType next_token;
   private Boolean have_space;
   private double break_value;

   BreakElement(String pnm,String nnm,String snm,double v) {
      prev_token = getToken(pnm);
      next_token = getToken(nnm);
      have_space = getBool(snm);
      break_value = v;
    }

   boolean match(BaleTokenType pt,BaleTokenType nt,boolean sp) {
      if (prev_token != null && prev_token != pt) return false;
      if (next_token != null && next_token != nt) return false;
      if (have_space != null && have_space.booleanValue() != sp) return false;
      return true;
    }

   double getValue()			{ return break_value; }

   private BaleTokenType getToken(String nm) {
      try {
	 if (nm != null && !nm.equals("*")) return BaleTokenType.valueOf(nm);
       }
      catch (IllegalArgumentException e) { }
      return null;
    }

   private Boolean getBool(String nm) {
      if (nm == null || nm.equals("*") || nm.length() == 0) return null;
      if ("tT1yY".indexOf(nm.charAt(0)) >= 0) return Boolean.TRUE;
      return Boolean.FALSE;
    }

}	// end of inner class BreakElement


}	// end of class BaleViewLine




/* end of BaleViewLine.java */

