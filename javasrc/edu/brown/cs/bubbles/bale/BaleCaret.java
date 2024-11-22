/********************************************************************************/
/*										*/
/*		BaleCaret.java							*/
/*										*/
/*	Bubble Annotated Language Editor caret implementation			*/
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

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;



class BaleCaret extends DefaultCaret implements Caret, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleCaretStyle	caret_style;
private int		last_width;
private boolean 	active_region;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCaret()
{
   caret_style = BaleCaretStyle.LINE_CARET;
   last_width = 0;
   active_region = false;
   setBlinkRate(500);
}



/********************************************************************************/
/*										*/
/*	Style setting methods							*/
/*										*/
/********************************************************************************/

void setCaretStyle(BaleCaretStyle cs)
{
   boolean v = isVisible();
   if (v) setVisible(false);
   caret_style = cs;
   last_width = 0;

   JTextComponent c = getComponent();
   if (c != null) setStyleForComponent(c);

   if (v) setVisible(true);
}



@Override public void install(JTextComponent c)
{
   setStyleForComponent(c);

   super.install(c);
}




private void setStyleForComponent(JTextComponent c)
{
   switch (caret_style) {
      case BLOCK_CARET :
      case LINE_CARET :
	 c.putClientProperty("caretWidth",Integer.valueOf(BALE_LINE_CARET_WIDTH));
	 break;
      case THIN_LINE_CARET :
	 c.putClientProperty("caretWidth",Integer.valueOf(BALE_THIN_CARET_WIDTH));
	 break;
      case THICK_LINE_CARET :
	 c.putClientProperty("caretWidth",Integer.valueOf(BALE_THICK_CARET_WIDTH));
	 break;
    }
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   if (caret_style == BaleCaretStyle.BLOCK_CARET) paintBlock(g);
   else super.paint(g);
}


@Override protected synchronized void damage(Rectangle r)
{
   if (caret_style == BaleCaretStyle.BLOCK_CARET) damageBlock(r);
   else super.damage(r);
}



/********************************************************************************/
/*										*/
/*	Block cursor painting methods						*/
/*										*/
/********************************************************************************/

private void paintBlock(Graphics g)
{
   if (!isVisible()) return;

   JTextComponent c = getComponent();
   Color bg = c.getBackground();
   if (bg.getAlpha() == 0) {
      bg = BoardColors.getColor("Bale.CaretBgXor");     // shows up as opposite color than this
   }

   try {
      int dot = getDot();
      Rectangle r;
      Rectangle r1;
      r = SwingText.modelToView2D(c,dot);
      r1 = SwingText.modelToView2D(c,dot+1);
      if (r1.y == r.y) last_width = r1.x - r.x;
      else last_width = 6;
      x = r.x;
      y = r.y;
      width = last_width;
      height = r.height;
      g.setXORMode(bg);
      g.fillRect(r.x,r.y,last_width,r.height);
      g.setPaintMode();
    }
   catch (BadLocationException e) { }
}



private void damageBlock(Rectangle r)
{
   if (r != null) {
      getComponent().repaint(r.x,r.y,last_width,r.height);
    }
}



/********************************************************************************/
/*										*/
/*	Mouse actions								*/
/*										*/
/********************************************************************************/

@Override public void mousePressed(MouseEvent e)
{
   JTextComponent c = getComponent();
   if (c instanceof BaleEditorPane) {
      BaleEditorPane bep = (BaleEditorPane) c;
      if (bep.handleActiveClick(e)) {
	 active_region = true;
	 return;
       }
      BaleDocument bd = bep.getBaleDocument();
      if (bd.isOrphan()) return;
    }

   super.mousePressed(e);
}



@Override public void mouseDragged(MouseEvent e)
{
   if (active_region) return;

   super.mouseDragged(e);
}



@Override public void mouseReleased(MouseEvent e)
{
   if (active_region) {
      active_region = false;
      return;
   }
   super.mouseReleased(e);
}



@Override public void mouseClicked(MouseEvent e)
{
   JTextComponent c = getComponent();
   if (c instanceof BaleEditorPane) {
      BaleEditorPane bep = (BaleEditorPane) c;
      BaleDocument bd = bep.getBaleDocument();
      if (bd.isOrphan()) return;
    }

   super.mouseClicked(e);
}



/********************************************************************************/
/*										*/
/*	Methods for handling click on elision					*/
/*										*/
/********************************************************************************/

@Override protected void positionCaret(MouseEvent e)
{
   Point pt = new Point(e.getX(),e.getY());
   JTextComponent c = getComponent();

   BaleDocument bd = (BaleDocument) c.getDocument();
   bd.baleWriteLock();
   try {
      int pos = SwingText.viewToModel2D(c,pt);
      
      BaleElement be = bd.getCharacterElement(pos);
      if (be != null && be.isElided()) {
	 try {
	    Rectangle r = SwingText.modelToView2D(c,pos);
	    if (pt.x > r.x + BALE_ELLIPSES_INSIDE_DELTA &&
		   pt.x < r.x + r.width - BALE_ELLIPSES_INSIDE_DELTA) {
	       be.setElided(false);
	       bd.handleElisionChange();
	       if (c instanceof BaleEditorPane) {
		  BaleEditorPane bep = (BaleEditorPane) c;
		  bep.increaseSizeForElidedElement(be);
		}
	       BoardMetrics.noteCommand("BALE","CaretRemoveElision");
	       BaleEditorBubble.noteElision(c);
	     }
	  }
	 catch (BadLocationException ex) { }
       }

      if (pos >= 0) setDot(pos);
    }
   finally { bd.baleWriteUnlock(); }
}


/********************************************************************************/
/*										*/
/*	Keep caret in bounds							*/
/*										*/
/********************************************************************************/

@Override public void moveDot(int dot,Position.Bias bias)
{
   JTextComponent tc = getComponent();
   int ln = tc.getDocument().getLength();
   if (dot >= ln) dot = ln-1;
   if (dot < 0) dot = 0;
   super.moveDot(dot,bias);
}



@Override public void setDot(int dot,Position.Bias bias)
{
   JTextComponent tc = getComponent();
   int ln = tc.getDocument().getLength();
   if (dot >= ln) dot = ln-1;
   if (dot < 0) dot = 0;
  
   super.setDot(dot,bias);
   
   BudaBubble bb = BudaRoot.findBudaBubble(tc);
   if (bb != null) {
      BoardMetrics.noteCommand("BALE","Caret_" + dot + "_" + bb.getHashId()); 
    }
 }



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public boolean equals(Object obj)
{
   if (!super.equals(obj)) return false;
   if (obj instanceof BaleCaret) {
      BaleCaret bc = (BaleCaret) obj;
      return caret_style == bc.caret_style && width == bc.width;
    }

   return false;
}


@Override public int hashCode()
{
   int v = super.hashCode();
   v += caret_style.hashCode();
   v += width;
   return v;
}



}	// end of class BaleCaret





/* end of BaleCaret.java */

