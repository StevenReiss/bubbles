/********************************************************************************/
/*										*/
/*		BaleCrumbBarComponent.java					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor crumb bar component	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman		      */
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
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingTextPane;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;



class BaleCrumbBarComponent extends SwingTextPane implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleCrumbBar			parent_component;
private int				nat_width;
private int				nat_height;
private RoundRectangle2D.Double 	draw_oval;
private boolean 			rolled_over;
private boolean                         my_search_up;
private String				my_arrow;
private String				package_name;
private String                          shown_text;
private BaleCrumbBarComponent		brother_component;
private Color				draw_color;
private boolean 			is_dirty;

private static final char ARROW = '\u25B6';
private static final int ELLIDED_WIDTH = 12; //from experimentation
private static final long serialVersionUID = 1;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCrumbBarComponent(BaleCrumbBar par, BaleCrumbBarComponent bro, String shotxt)
{
   parent_component = par;
   brother_component = bro;
   is_dirty = false;

   if (shotxt.endsWith(""+ARROW) || shotxt.endsWith(">")) {
      shown_text = shotxt.substring(0, shotxt.length()-1);
      my_arrow = shotxt.substring(shotxt.length()-1);
    }
   else {
      shown_text = shotxt;
      my_arrow = " ";
    }
   shown_text = shown_text.replace("$",".");
   

   //nat_width = getColumnWidth()*shotxt.length()/2;
   setText(shown_text);
   setEditable(false);

   Dimension d0 = getPreferredSize();
   nat_height = d0.height;
   if (nat_height == 0) {
      System.err.println("BAD HEIGHT7");
      nat_height = 14;
   }
   setSize(d0);
   
   // DefaultCaret car = (DefaultCaret) getCaret();
   // car.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
   // car.setVisible(false);
   setCaret(new CrumbCaret());

   draw_oval = new RoundRectangle2D.Double();
   rolled_over=false;
   my_search_up = false;
   package_name = null;
   draw_color = BoardColors.getColor(BALE_CRUMB_COMPONENT_COLOR_PROP);
   setForeground(draw_color);
   append(my_arrow, BoardColors.getColor("Bale.CrumbArrowColor"));
   addMouseListener(new Mouser());

   if (BudaRoot.showHelpTips()) {
      setToolTipText("Click to begin search starting here");
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

private void instOval()
{
   draw_oval.setRoundRect(0, 0, this.getWidth()-2, this.getHeight()-1, 5, 5); //makes oval correct size.
}



void setColor(Color tobe)
{
   draw_color = tobe;
   setText("");
   append(shown_text, draw_color);
   append(my_arrow, BoardColors.getColor("Bale.CrumbArrowColor"));
}

String getShownText() { return shown_text; }

void setPackageName(String pfx)
{
   package_name = pfx;
}



String getPackageName()
{
   return package_name;
}


/*
 * This method calculates the total width of the group of BaleCrumbBarComponents being used
 */
int getWidthLocation()
{
   if (brother_component == null){
      return getWidth();
   }
   else {
      return brother_component.getWidthLocation() + getWidth();
   }
}



/********************************************************************************/
/*										*/
/*	Text appending methods							*/
/*										*/
/********************************************************************************/

void append(String toAp, Color c)
{
   if (getHeight() == 0) 
      System.err.println("BAD HEIGHT5 " + getText());
   
   try {
      SimpleAttributeSet attr=new SimpleAttributeSet();
      StyleConstants.setForeground(attr,c);
      getStyledDocument().insertString(getStyledDocument().getLength(),toAp,attr);
    }
   catch (BadLocationException e) { }

   if (getHeight() == 0) 
      System.err.println("BAD HEIGHT4 " + getText());
}


void setDirty(boolean b)
{
   is_dirty = b;
   resetName();
   updateNatWidth();
   grow();
}


private void resetName()
{
   setText("");
   append(shown_text, draw_color);
   append(my_arrow, BoardColors.getColor("Bale.CrumbArrowColor"));
   if (is_dirty) append("*", BoardColors.getColor("Bale.CrumbDirtyColor"));
   if (getHeight() == 0) 
      System.err.println("BAD HEIGHT1 " + getText());
}



/********************************************************************************/
/*										*/
/*	Resizing methods							*/
/*										*/
/********************************************************************************/

void shrink()
{
   setText("");
   append(" ...", BoardColors.getColor("Bale.CrumbElidedColor"));
   setSize(ELLIDED_WIDTH, getHeight());
   setCaretPosition(0);
   if (getHeight() == 0) 
      System.err.println("BAD HEIGHT2 " + getText());
}



void grow()
{
   resetName();
   setSize(nat_width, nat_height);
   setCaretPosition(0);
   if (getHeight() == 0) 
      System.err.println("BAD HEIGHT3 " + getText());
}



void updateNatWidth()
{
   Dimension d1 = getPreferredSize();
   if (getWidth() != 0 && !getText().equals(" ...")) nat_width = d1.width;
}



int addedWidthIfGrown()
{
   return (nat_width - getWidth());
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   super.paint(g);
   
   Graphics2D brush = (Graphics2D) g;
   if (rolled_over && package_name != null){
      brush.setColor(BoardColors.getColor(BALE_CRUMB_ROLLOVER_COLOR_PROP));
      brush.fill(draw_oval);
   }
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void handleRequest(){
   BudaRoot root = BudaRoot.findBudaRoot(parent_component);
   Rectangle loc = BudaRoot.findBudaLocation(parent_component);
   if (root == null || loc == null) return;
   if (!my_search_up) {
       root.createSearchBubble(new Point(loc.x+getWidthLocation(),
					    loc.y+this.getHeight()),
				  parent_component.getProjectName(),package_name,false);
    }
   else root.hideSearchBubble();
   my_search_up = !my_search_up;
}



/********************************************************************************/
/*										*/
/*	Callback handlers							*/
/*										*/
/********************************************************************************/

private final class Mouser extends MouseAdapter
{

   @Override public void mouseClicked(MouseEvent e) {
      if (package_name!=null) handleRequest();
   }

   @Override public void mouseEntered(MouseEvent e) {
      rolled_over = true;
      instOval(); //called at this point so the textfield is instantiated and at its correct size
      repaint();
   }

   @Override public void mouseExited(MouseEvent e) {
      rolled_over = false;
      my_search_up = false;
      repaint();
   }

}	// end of inner class Mouser



/********************************************************************************/
/*										*/
/*	Dummy caret for the component						*/
/*										*/
/********************************************************************************/

private static class CrumbCaret extends DefaultCaret {

   private static final long serialVersionUID = 1;


   CrumbCaret() {
      setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
      setVisible(false);
    }

   @Override protected void adjustVisibility(Rectangle r)		{ }

}	// end of inner class CrumbCaret




}	// end of class BaleCrumbBarComponent



/* end of BaleCrumbBarComponent.java */
