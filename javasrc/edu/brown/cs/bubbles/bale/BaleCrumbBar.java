/********************************************************************************/
/*										*/
/*		BaleCrumbBar.java						*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor crumb bar		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman, Steven P. Reiss	*/
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
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.burp.BurpHistory;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedList;


class BaleCrumbBar extends JPanel implements DocumentListener,
		UndoableEditListener, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleEditorPane		for_editor;
private BaleDocument		for_document;
private String			project_name;
private String			fragment_name;
private boolean 		is_dirty;
private LinkedList<BaleCrumbBarComponent> component_list;
private BaleCrumbBarComponent	last_name;
private boolean 		show_method_name;
private boolean 		force_rename;
private int			recent_width;
private char			arrow_char;

private static final char ARROW = '\u25B6';
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCrumbBar(BaleEditorPane be, String name)
{
   super(bcbLayoutManager());

   for_editor = be;

   for_document = be.getBaleDocument();
   project_name = for_document.getProjectName();
   for_document.addDocumentListener(this);

   is_dirty = BurpHistory.getHistory().isDirty(be);

   force_rename = false;

   arrow_char = ARROW;
   if (!BALE_PROPERTIES.getFont(BALE_CRUMB_FONT).canDisplay(ARROW)) {
      arrow_char = '>';
    }

   show_method_name = BALE_PROPERTIES.getBoolean(BALE_CRUMB_SHOW_METHOD);

   recent_width = getWidth();

   last_name = null;

   BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
   addComponentListener(new ResizeListener());

   setBackground(BoardColors.getColor(BALE_EDITOR_TOP_COLOR_PROP));
   if(name == null) setFragmentName("*.New Fragment",false);
   else setFragmentName(name, false);

   SwingUtilities.invokeLater(new CheckNames());
}



/********************************************************************************/
/*										*/
/*	Access methods (written by Ian Strickman)				*/
/*										*/
/********************************************************************************/

private static LayoutManager bcbLayoutManager()
{
   FlowLayout lay = new FlowLayout(FlowLayout.LEFT);
   lay.setHgap(0);
   lay.setVgap(0);

   return lay;
}


private void instTextField(JTextPane j)
{
   j.setFont(BALE_PROPERTIES.getFont(BALE_CRUMB_FONT));
   j.setEditable(false);
   j.setBorder(new EmptyBorder(0,0,0,0));
   j.setBackground(BoardColors.getColor(BALE_EDITOR_TOP_COLOR_PROP));
   j.setOpaque(true);
   j.setCaretPosition(0);
}



String getProjectName() 			{ return project_name; }

String getFragmentName()			{ return fragment_name; }



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   checkNames();

   if (recent_width == 0) handleResize();

   boolean smfg = BALE_PROPERTIES.getBoolean(BALE_CRUMB_SHOW_METHOD);
   if (smfg != show_method_name) {
      synchronized (this) {
	 force_rename = true;
       }
      show_method_name = smfg;
      SwingUtilities.invokeLater(new CheckNames());
      recent_width = getWidth();
    }

   BoardColors.setColors(this,BALE_EDITOR_TOP_COLOR_PROP);

   super.paint(g);
}




/********************************************************************************/
/*										*/
/*	Naming methods								*/
/*										*/
/********************************************************************************/

private void checkNames()
{
   for_document.baleReadLock();
   try {
      project_name = for_document.getProjectName();
      boolean dirty = BurpHistory.getHistory().isDirty(for_editor);

      show_method_name = BALE_PROPERTIES.getBoolean(BALE_CRUMB_SHOW_METHOD);

      if (for_document.getFragmentName() != null) setFragmentName(for_document.getFragmentName(),dirty);
      else setFragmentName(fragment_name, dirty);
    }
   finally { for_document.baleReadUnlock(); }
}


// Written by Ian Strickman

private synchronized void setFragmentName(String name,boolean dirty)
{
   if (name == null) return;

   String nodollarname;
   if (fragment_name != null) nodollarname = fragment_name.replaceAll("[$]", ".");
   else nodollarname = "";

   if (!name.equals(nodollarname) && !name.equals(fragment_name)) force_rename = true;

   if (dirty == is_dirty && !force_rename) return;

   fragment_name = name;
   is_dirty = dirty;

   if (!is_dirty || force_rename) {
      force_rename = false;
      int parenidx = fragment_name.lastIndexOf("(");
      int idx;
      if(parenidx < 0) idx = fragment_name.lastIndexOf(".");
      else idx = fragment_name.lastIndexOf(".", parenidx);
      String packclass;
      if (idx < 0) packclass = "";
      else packclass = fragment_name.substring(0, idx);
      int numcomps = 0;
      int mobidx = 0;
      while (mobidx < packclass.length()) {
	 ++numcomps;
	 mobidx = packclass.indexOf(".", mobidx + 1);
	 if (mobidx < 0) break;
       }
      removeAll();
      component_list = new LinkedList<BaleCrumbBarComponent>();
      mobidx = -1;
      int lastidx;
      for (int i = 0; i < numcomps; i++) {
	 lastidx = mobidx + 1;
	 mobidx = packclass.indexOf(".", lastidx);
	 if (mobidx < 0) mobidx = packclass.length();
	 if (i == 0) {
	    String txt = " " + packclass.substring(lastidx, mobidx) + arrow_char;
	    component_list.addLast(new BaleCrumbBarComponent(this,null,txt));
	  }
	 else {
	    String txt = packclass.substring(lastidx, mobidx) + arrow_char;
	    component_list.addLast(new BaleCrumbBarComponent(this,component_list.getLast(),txt));
	  }
	 instTextField(component_list.getLast());
	 nodollarname = packclass.substring(0, mobidx).replaceAll("[$]", ".");
	 component_list.getLast().setPackageName(nodollarname);
	 add(component_list.getLast());
       }

      String lastcomp = fragment_name.substring(idx+1);
      int parenloc = lastcomp.indexOf("(");
      boolean stoplast = false;
      if (parenloc >= 0) {
	 if (show_method_name) lastcomp = lastcomp.substring(0, parenloc + 1) + "...)";
	 else stoplast = true;
       }
      if (!stoplast) {
	 if (parenloc < 0 && lastcomp.indexOf("<") < 0) {
	    if (component_list.size() > 0) {
	       last_name = new BaleCrumbBarComponent(this, component_list.getLast(), lastcomp + arrow_char);
	       last_name.setPackageName(fragment_name);
	     }
	    else {
	       last_name = new BaleCrumbBarComponent(this,null,lastcomp);
	     }
	  }
	 else if (component_list.size() > 0) {
	    last_name = new BaleCrumbBarComponent(this, component_list.getLast(), lastcomp);
	  }

	 if (last_name != null) {
	    instTextField(last_name);
	    component_list.addLast(last_name);
	    add(last_name);
	  }
       }
      int size = component_list.size();
      if (size >= 3) {
	 component_list.get(size-3).setColor(BoardColors.getColor(BALE_CRUMB_PACKAGE_COLOR_PROP));
	 component_list.get(size-2).setColor(BoardColors.getColor(BALE_CRUMB_CLASS_COLOR_PROP));
	 component_list.getLast().setColor(BoardColors.getColor(BALE_CRUMB_METHOD_COLOR_PROP));
	 component_list.getLast().setFont(BALE_PROPERTIES.getFont(BALE_CRUMB_METHOD_FONT));
       }
      else if (size == 2) {
	 String s = component_list.getLast().getShownText();
	 if (!s.contains("(") && !s.contains("<")) {
	    component_list.get(size-2).setColor(BoardColors.getColor(BALE_CRUMB_PACKAGE_COLOR_PROP));
	    component_list.getLast().setColor(BoardColors.getColor(BALE_CRUMB_CLASS_COLOR_PROP));
	  }
	 else {
	    component_list.get(size-2).setColor(BoardColors.getColor(BALE_CRUMB_CLASS_COLOR_PROP));
	    component_list.getLast().setColor(BoardColors.getColor(BALE_CRUMB_METHOD_COLOR_PROP));
	    component_list.getLast().setFont(BALE_PROPERTIES.getFont(BALE_CRUMB_METHOD_FONT));
	  }
       }
      else if (size == 1) {
	 component_list.getLast().setColor(BoardColors.getColor(BALE_CRUMB_PACKAGE_COLOR_PROP));
       }
      repaint();
    }
   else if (is_dirty && component_list.size() > 0) {
      component_list.getLast().setDirty(true);
   }
}


private class CheckNames implements Runnable {

   @Override public void run() {
      checkNames();
   }

}	// end of inner class CheckNames




/********************************************************************************/
/*										*/
/*	Sizing methods								*/
/*										*/
/********************************************************************************/

private synchronized void handleResize()
{
   int newwidth = getWidth();
   int maxwidth = newwidth - 20;
   if (component_list == null) return;
   BaleCrumbBarComponent lastcomp;

   if (last_name != null) lastcomp = last_name;
   else if (component_list.size() == 0) return;
   else lastcomp = component_list.getLast();

   if (newwidth >= recent_width) {
      for (int i = 0; i < component_list.size(); i++){
	 component_list.get(i).updateNatWidth();
       }
      lastcomp.updateNatWidth();
      if (lastcomp.getWidthLocation() + lastcomp.addedWidthIfGrown() <= maxwidth) {
	 lastcomp.grow();
       }

      int unelliding = component_list.size()-1;
      while (maxwidth > lastcomp.getWidthLocation() && unelliding >= 0) {
	 if (lastcomp.getWidthLocation() + component_list.get(unelliding).addedWidthIfGrown() <= maxwidth) {
	    component_list.get(unelliding).grow();
	    unelliding--;
	  }
	 else break;
       }
    }

   if (newwidth < recent_width || recent_width == 0) {
      int elliding = 0;
      while (lastcomp.getWidthLocation() > maxwidth && elliding < component_list.size()) {
	 component_list.get(elliding).shrink();
	 elliding++;
       }
      if (lastcomp.getWidthLocation() > maxwidth && elliding == component_list.size()) {
	 lastcomp.shrink();
       }
    }

   recent_width = newwidth;
}


/********************************************************************************/
/*										*/
/*	Methods to handle resizing						*/
/*										*/
/********************************************************************************/

private class ResizeListener extends ComponentAdapter {

   /**
      * This method alters the way in which the components break down as the bubble gets smaller or larger
      *
      * Written by Ian Strickman
      **/

   @Override public void componentResized(ComponentEvent evt) {
      handleResize();
    }

   @Override public void componentShown(ComponentEvent evt) {
      handleResize();
   }

}	// end of inner class ResizeListener



/********************************************************************************/
/*										*/
/*	Popup menu handlers							*/
/*										*/
/********************************************************************************/

void handleContextMenu(MouseEvent evt)
{
   for (BaleCrumbBarComponent bcc : component_list) {
      Rectangle r = bcc.getBounds();
      int x = evt.getX();
      if (x >= r.x && x < r.x + r.width) {
	 bcc.handleRequest();
      }
   }
}



/********************************************************************************/
/*										*/
/*	Callback handlers							*/
/*										*/
/********************************************************************************/


@Override public void changedUpdate(DocumentEvent e)
{
   SwingUtilities.invokeLater(new CheckNames());
}


@Override public void insertUpdate(DocumentEvent e)
{
   SwingUtilities.invokeLater(new CheckNames());
}


@Override public void removeUpdate(DocumentEvent e)
{
   SwingUtilities.invokeLater(new CheckNames());
}



@Override public void undoableEditHappened(UndoableEditEvent e)
{
   SwingUtilities.invokeLater(new CheckNames());
}



}	// end of class BaleCrumbBar





/* end of BaleCrumbBar.java */
