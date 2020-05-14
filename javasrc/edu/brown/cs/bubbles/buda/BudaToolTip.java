/********************************************************************************/
/*										*/
/*		BudaToolTip							*/
/*										*/
/*	BUblles Display Area tool tip with html smart sizing			*/
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



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.Scrollable;
import javax.swing.text.View;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;






public class BudaToolTip extends JToolTip implements MouseWheelListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JTextPane text_pane;
private JLabel	  text_label;


private static final int TIP_WIDTH = 800;
private static final int MAX_HEIGHT = 400;


private static final long serialVersionUID = 1;

private static boolean allow_scrollable = true;
private static boolean scroll_label = true;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BudaToolTip()
{
   if (allow_scrollable) {
      setLayout(new BorderLayout());
      text_label = new ScrollableLabel();
      text_pane = new JTextPane();
      text_pane.setEditable(false);
      text_pane.setContentType("text/html");

      Font ft = getFont();
      text_pane.setFont(ft);
      text_pane.setBackground(getBackground());
      text_pane.setForeground(getForeground());
      text_pane.setOpaque(true);

      text_label.setFont(ft);
      text_label.setBackground(getBackground());
      text_label.setForeground(getForeground());
      text_label.setOpaque(true);

      LookAndFeel.installColorsAndFont(text_pane,"ToopTip.background",
	    "ToolTip.foreground","ToolTip.font");

      JComponent comp = text_pane;
      if (scroll_label) {
	 comp = text_label;
	 text_pane = null;
      }
      else {
	 text_label = null;
      }

      JScrollPane scrollpane = new JScrollPane(comp);
      scrollpane.setBorder(null);
      // scrollpane.getViewport().setOpaque(false);
      add(scrollpane);
    }
}



/********************************************************************************/
/*										*/
/*	Event handling								*/
/*										*/
/********************************************************************************/

@Override public void addNotify()
{
   try {
      super.addNotify();
      JComponent comp = getComponent();
      if (comp != null && allow_scrollable) {
	 comp.addMouseWheelListener(this);
       }
    }
   catch (Throwable t) {
      setVisible(false);
    }

}



@Override public void removeNotify()
{
   try {
      JComponent comp = getComponent();
      if (comp != null) {
	 comp.removeMouseWheelListener(this);
       }
      super.removeNotify();
    }
   catch (Throwable t) {
    }

}


@Override public void mouseWheelMoved(MouseWheelEvent e)
{
   JComponent comp = getComponent();
   if (comp != null && text_pane != null) {
      text_pane.dispatchEvent(new MouseWheelEvent(text_pane,
	    e.getID(), e.getWhen(), e.getModifiersEx(),
	    0,0, e.getClickCount(), e.isPopupTrigger(),
	    e.getScrollType(), e.getScrollAmount(), e.getWheelRotation()));
    }
   if (comp != null && text_label != null) {
      text_label.dispatchEvent(new MouseWheelEvent(text_label,
	    e.getID(), e.getWhen(), e.getModifiersEx(),
	    0,0, e.getClickCount(), e.isPopupTrigger(),
	    e.getScrollType(), e.getScrollAmount(), e.getWheelRotation()));
    }
}


@Override public void setTipText(String tip)
{
   if (tip == null) tip = "";

   if (allow_scrollable) {
      if (text_pane != null) {
	 String old = text_pane.getText();
	 if (tip.startsWith("<html>")) text_pane.setContentType("text/html");
	 else text_pane.setContentType("text/plain");
	 text_pane.setText(tip);
	 text_pane.setCaretPosition(0);
	 firePropertyChange("tiptext",old,tip);
      }
      else if (text_label != null) {
	 text_label.setText(tip);
      }
    }
   else {
      super.setTipText(tip);
    }
}


@Override public String getTipText()
{
   if (allow_scrollable) {
      if (text_pane == null && text_label == null) return "";
      else if (text_pane == null) return text_label.getText();
      return text_pane.getText();
    }
   else return super.getTipText();
}



@Override protected String paramString()
{
   if (allow_scrollable) {
      String tts = null;
      if (text_pane != null) tts = text_pane.getText();
      else if (text_label != null) tts = text_label.getText();
      if (tts == null) tts = "";
      return super.paramString() + ",tipText=" + tts;
    }
   else return super.paramString();
}




/********************************************************************************/
/*										*/
/*	Sizing routines 							*/
/*										*/
/********************************************************************************/

@Override public Dimension getPreferredSize()
{
   String txt = getTipText();
   Dimension dflt;

   if (allow_scrollable && text_pane != null) {
      dflt = text_pane.getPreferredSize();
      dflt.width += 5;
      dflt.height += 2;
      // if (dflt.width > MAX_WIDTH) dflt.width = MAX_WIDTH;
      if (dflt.height > MAX_HEIGHT) {
	 dflt.width += 16;
	 dflt.height = MAX_HEIGHT;
      }
      return dflt;
    }
   else if (allow_scrollable && text_label != null) {
      dflt = text_label.getPreferredSize();
      if (dflt.width >= TIP_WIDTH && txt.startsWith("<html>")) {
	 View v = (View) text_label.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
	 int w0 = TIP_WIDTH;
	 v.setSize(w0,0);
	 float w = v.getPreferredSpan(View.X_AXIS);
	 dflt.width = ((int) w) + 10;
      }
      dflt.width += 5;
      dflt.height += 2;
      // if (dflt.width > MAX_WIDTH) dflt.width = MAX_WIDTH;
      if (dflt.height > MAX_HEIGHT) {
	 dflt.height = MAX_HEIGHT;
	 dflt.width += 16;
      }
      return dflt;
   }
   else dflt = super.getPreferredSize();

   if (txt == null || txt.length() == 0) return dflt;
   if (!txt.startsWith("<html>")) return dflt;
   // txt = txt.replace("\t","");

   JLabel lbl = new JLabel();
   lbl.setFont(getFont());
   lbl.setText(txt);
   Dimension d0 = lbl.getPreferredSize();
   if (d0.width < TIP_WIDTH) return super.getPreferredSize();

   View v = (View) lbl.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
   int w0 = TIP_WIDTH;
   v.setSize(w0,0);
   float w = v.getPreferredSpan(View.X_AXIS);
   float h = v.getPreferredSpan(View.Y_AXIS);

   float wmin = v.getMinimumSpan(View.X_AXIS);
   float wmax = w;
   while (wmax - wmin > 1) {
      w = (wmin + wmax) / 2;
      v.setSize(w,0);
      float h1 = v.getPreferredSpan(View.Y_AXIS);
      if (h1 <= h) {
	 wmax = w;
       }
      else {
	 wmin = w;
       }
    }
   w = Math.round(wmin);
   v.setSize(w,0);
   h = v.getPreferredSpan(View.Y_AXIS);

   // w = v.getMinimumSpan(View.X_AXIS)+16;

   Dimension d = new Dimension((int) Math.ceil(w+10),(int) Math.ceil(h+5));

   return d;
}



/********************************************************************************/
/*										*/
/*	Paint methods								*/
/*										*/
/********************************************************************************/

@Override public void paintComponent(Graphics g0)
{
   Insets ins = getInsets();
   if (getWidth()-ins.left-ins.right <= 6) return;

   try {
      super.paintComponent(g0);
    }
   catch (IllegalArgumentException e) {
      BoardLog.logE("BUDA","Tool tip with no width/height: " + getSize() + " :: " +
		       getTipText() + " :: " + getToolTipText() + " :: " + this,e);
    }
}



/********************************************************************************/
/*										*/
/*	Scrollable Label							*/
/*										*/
/********************************************************************************/

private static class ScrollableLabel extends JLabel implements Scrollable {

   @Override public int getScrollableBlockIncrement(Rectangle v,int o,int d) {
      return 32;
    }

   @Override public int getScrollableUnitIncrement(Rectangle v,int o,int d) {
      return 16;
    }

   @Override public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

   @Override public boolean getScrollableTracksViewportHeight() {
      return false;
    }

   @Override public boolean getScrollableTracksViewportWidth() {
      return true;
    }

}	// end of inner class ScrollableLabel





}	// end of class BudaToolTip




/* end of BudaToolTip.java */






































































