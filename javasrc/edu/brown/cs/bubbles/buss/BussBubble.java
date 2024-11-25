/********************************************************************************/
/*										*/
/*		BussBubble.java 						*/
/*										*/
/*	BUbble Stack Strategies bubble stack bubble				*/
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


package edu.brown.cs.bubbles.buss;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaHover;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.ivy.swing.SwingEventListenerList;

import javax.swing.JLayeredPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.tree.TreePath;

import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collection;



/**
 *	This class implements a bubble stack.
 **/

public class BussBubble extends BudaBubble implements BussConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BussStackBox	stack_box;
private BussTreeModel	tree_model;

private BussBubble our_self;
private BudaBubble source_bubble;
private transient BudaConstants.LinkPort source_linkport;

private transient BussEntry selected_entry;
private BudaBubble editor_bubble;
private JViewport view_port;
private JLayeredPane layered_pane;
private BudaConstants.BudaLinkStyle link_style;
private SwingEventListenerList<BussListener> buss_listeners;
private transient Hoverer buss_hover;

private Dimension default_dim;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BussBubble(Collection<BussEntry> ents, int contentWidth)
{
   tree_model = new BussTreeModel();

   for (BussEntry ent : ents){
      tree_model.addEntry(ent);
    }
   tree_model.setup();

   stack_box = new BussStackBox(tree_model, contentWidth, this);

   layered_pane = new JLayeredPane();
   layered_pane.add(stack_box,Integer.valueOf(0), 0);
   layered_pane.setPreferredSize(stack_box.getPreferredSize());
   layered_pane.setSize(stack_box.getSize());
   layered_pane.setLayout(null);

   BussViewport vp = new BussViewport(stack_box, layered_pane);

   default_dim = (Dimension) stack_box.getPreferredSize().clone();
   stack_box.setSize(stack_box.getPreferredSize());
   link_style = BudaLinkStyle.STYLE_SOLID;

   view_port = vp.getViewport();

   setContentPane(vp, stack_box);

   stack_box.addMouseListener(new BudaConstants.FocusOnEntry());

   this.addComponentListener(new BussResizeListener());

   our_self = this;
   buss_hover = new Hoverer();
   buss_listeners = new SwingEventListenerList<>(BussListener.class);
}


/********************************************************************************/
/*										*/
/*	Public access methods							*/
/*										*/
/********************************************************************************/

/**
 *	Set the preview bubble for the bubble stack
 **/

public void setPreviewBubble(BudaBubble previewBubble)
{
   buss_hover.setPreviewBubble(previewBubble);
}



/**
 *	Remove the editor bubble from the bubble stack
 **/

public void removeEditorBubble()
{
   if (selected_entry != null) {
      default_dim.height -= selected_entry.getCompactComponent().getPreferredSize().height;
    }

   getLayeredPane().setPreferredSize(default_dim);
   setStackBoxSize(default_dim);

   setEditorBubble(null);
   setSelectedEntry(null);
}



/**
 *	Return the current editor bubble for the bubble stack
 **/

public BudaBubble getEditorBubble()
{
   return editor_bubble;
}



/**
 *	Set the style for links
 **/
public void setLinkStyle(BudaLinkStyle sty)
{
   link_style = sty;
}



/**
 *	Set the editor bubble for the bubble stack
 **/

void setEditorBubble(BudaBubble editorbubble)
{
   editor_bubble = editorbubble;
}



/**
 *	Set the current selected entry in the bubble stack.  If the passed in
 *	entry is null, the current selection is removed.
 **/

void setSelectedEntry(BussEntry selectedentry)
{
   if (selectedentry == null) {
      if (selected_entry != null) tree_model.removeEntry(selected_entry);
    }

   selected_entry = selectedentry;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JLayeredPane getLayeredPane()
{
   return layered_pane;
}



Dimension getStackBoxDim()
{
   return default_dim;
}



void setStackBoxSize(Dimension dim)
{
   stack_box.setPreferredSize(dim);

   Dimension dimension = new Dimension(Math.max(stack_box.getPreferredSize().width, our_self.getSize().width),
				       Math.max(stack_box.getPreferredSize().height, our_self.getSize().height));

   stack_box.setSize(dimension);
}



void setViewportLocation(int diffy)
{
   view_port.setViewPosition(new Point(view_port.getViewPosition().x, view_port.getViewPosition().y + diffy));
}



BussEntry getSelectedEntry()
{
   return selected_entry;
}



/**
 *	Set the source for linking new bubbles
 **/

public void setSourceBubbleInfomation(BudaBubble sb, BudaConstants.LinkPort sl)
{
   source_bubble = sb;
   source_linkport = sl;
}


BudaBubble getSourceBubble()		{ return source_bubble; }



/********************************************************************************/
/*										*/
/*	Methods to handle bubble movement					*/
/*										*/
/********************************************************************************/

@Override public BudaBubble getActualBubble(int x, int y, boolean moved)
{
   if (getEditorBubble() != null && stack_box.getSelectionRows() != null &&
         stack_box.getSelectionRows().length > 0) {
      int row = stack_box.getSelectionRows()[0];

      Point loc = getLocation();
      Rectangle selecteditemrect = stack_box.getRowBounds(row);
      Rectangle viewportrect = view_port.getViewRect();

      int diffx = Math.max(0, selecteditemrect.x - viewportrect.x);
      int diffy = Math.max(0, selecteditemrect.y - viewportrect.y);

      int locx = diffx + loc.x + (int) BudaConstants.BUBBLE_EDGE_SIZE + 1;
      int locy = diffy + loc.y + (int) BudaConstants.BUBBLE_EDGE_SIZE + 1;

      int startx = Math.max(selecteditemrect.x, viewportrect.x);
      int endx = Math.min(selecteditemrect.x + getEditorBubble().getPreferredSize().width,
			     viewportrect.x + viewportrect.width);

      int starty = Math.max(selecteditemrect.y, viewportrect.y);
      int endy = Math.min(selecteditemrect.y + getEditorBubble().getPreferredSize().height,
			     viewportrect.y + viewportrect.height);

      int width = Math.max(0, endx - startx);
      int height = Math.max(0, endy - starty);

      if (x >= locx && x <= (locx + width) && y >= locy && y <= (locy + height)){
	 if (moved)
	    tearOutEditorBubble();

	 return getEditorBubble();
       }
    }

   return this;
}




/**
 *	Update the location of the current editor bubble associated with the bubble stack
 **/

public void updateEditorBubbleLocation()
{
   if (getEditorBubble() != null && stack_box.getSelectionRows() != null &&
       stack_box.getSelectionRows().length > 0) {

      Rectangle selectedItemRect = stack_box.getRowBounds(stack_box.getSelectionRows()[0]);
      getEditorBubble().setLocation(selectedItemRect.getLocation());
      getLayeredPane().add(getEditorBubble(), Integer.valueOf(1), 0);
    }
}



/**
 *	Return the location of the current editor bubble associated with the bubble stack.
 **/

public Point getEditorBubbleLocation()
{
   if (getEditorBubble() != null && stack_box.getSelectionRows() != null &&
	  stack_box.getSelectionRows().length > 0) {
      int row = stack_box.getSelectionRows()[0];

      Point loc = getLocation();
      Rectangle selecteditemrect = stack_box.getRowBounds(row);
      Rectangle viewportrect = view_port.getViewRect();

      int diffx = Math.max(0, selecteditemrect.x - viewportrect.x);
      int diffy = Math.max(0, selecteditemrect.y - viewportrect.y);

      return new Point(diffx + loc.x + (int) BudaConstants.BUBBLE_EDGE_SIZE + 1,
				       diffy + loc.y + (int) BudaConstants.BUBBLE_EDGE_SIZE + 1);
    }

   return null;
}



/**
 *	Update the size of the editor bubble associated witht he bubble stack.
 **/

void updateEditorBubbleSize()
{
   if (getEditorBubble() != null && stack_box.getSelectionRows() != null &&
	 stack_box.getSelectionRows().length > 0) {
      int row = stack_box.getSelectionRows()[0];

      Rectangle selecteditemrect = stack_box.getRowBounds(row);
      Rectangle viewportrect = view_port.getViewRect();

      int startx = Math.max(selecteditemrect.x, viewportrect.x);
      int endx = Math.min(selecteditemrect.x + getEditorBubble().getPreferredSize().width,
			     viewportrect.x + viewportrect.width);

      int starty = Math.max(selecteditemrect.y, viewportrect.y);
      int endy = Math.min(selecteditemrect.y + getEditorBubble().getPreferredSize().height,
			     viewportrect.y + viewportrect.height);

      getEditorBubble().setSize(Math.max(0, endx - startx), Math.max(0, endy - starty));
    }
}



void tearOutEditorBubble()
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   Rectangle loc = BudaRoot.findBudaLocation(this);
   if (bba == null || loc == null) return;

   Rectangle selecteditemrect = stack_box.getRowBounds(stack_box.getSelectionRows()[0]);
   Rectangle viewportrect = view_port.getViewRect();

   int diffx = Math.max(0, selecteditemrect.x - viewportrect.x);
   int diffy = Math.max(0, selecteditemrect.y - viewportrect.y);

   int locx = diffx + loc.x + (int) BudaConstants.BUBBLE_EDGE_SIZE + 1;
   int locy = diffy + loc.y + (int) BudaConstants.BUBBLE_EDGE_SIZE + 1;

   int zindex = JLayeredPane.getLayer(this);

   getEditorBubble().setLocation(locx, locy);

   bba.setLayer(getEditorBubble(), zindex + 1);

   for (int i = 0; i < 3; ++i) {
      try {
	 // this can fail when trying to sort bubbles on screen -- retry in that case
	 bba.add(getEditorBubble(), new BudaConstraint(BudaBubblePosition.FIXED, locx, locy));
	 break;
       }
      catch (Throwable t) { }
    }


   getEditorBubble().setFixed(false);

   addLinks(getEditorBubble());
}


void addLinks(BudaBubble bb)
{
   if (source_bubble == null || source_linkport == null) return;

   BudaRoot root = BudaRoot.findBudaRoot(source_bubble);
   if (root == null) return;
   if (!source_bubble.isShowing()) return;

   BudaConstants.LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
   BudaBubbleLink lnk = new BudaBubbleLink(source_bubble,source_linkport,bb,port1,true,link_style);
   root.addLink(lnk);
}


/********************************************************************************/
/*                                                                              */
/*      Listener methods                                                        */
/*                                                                              */
/********************************************************************************/

public void addBussListener(BussListener bl) 
{
   buss_listeners.add(bl);
}

public void removeBussListener(BussListener bl)
{
   buss_listeners.remove(bl);
}


void noteEntryExpanded(BussEntry be)
{
   for (BussListener bl : buss_listeners) {
      bl.entryExpanded(be);
    }
}


void noteEntrySelected(BussEntry be)
{
   for (BussListener bl : buss_listeners) {
      bl.entrySelected(be);
    }
}


void noteEntryHovered(BussEntry be)
{
   for (BussListener bl : buss_listeners) {
      bl.entryHovered(be);
    }
}



private final class BussResizeListener extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      Dimension dimension = new Dimension(
	 Math.max(stack_box.getPreferredSize().width, our_self.getSize().width),
	    Math.max(stack_box.getPreferredSize().height, our_self.getSize().height));

      stack_box.setSize(dimension);
    }

}	// end of inner class BussMoveListener





/********************************************************************************/
/*										*/
/*	Hoverer :: Code added by Hsu-Sheng Ko					*/
/*										*/
/********************************************************************************/

@Override public void setVisible(boolean isVisible){
   super.setVisible(isVisible);
   buss_hover.clearHover(null);
}



private BudaBubble createHoverBubble(BussEntry entry, int xPos, int yPos)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   Rectangle loc = BudaRoot.findBudaLocation(this);
   if (bba == null || loc == null) return null;

   BudaBubble bubble = entry.getBubble();
   if (bubble == null) return null;

   bba.add(entry.getBubble(), new BudaConstraint(BudaBubblePosition.FIXED,
						   loc.x + xPos + 50,
						   loc.y + yPos));
   bubble.setVisible(true);
   bba.setLayer(bubble,JLayeredPane.PALETTE_LAYER + 1);

   return bubble;
}




private class Hoverer extends BudaHover implements ComponentListener {

   private BudaBubble preview_bubble;

   Hoverer() {
      super(stack_box);

      our_self.addComponentListener(this);

      preview_bubble = null;
    }

   @Override public void handleHover(MouseEvent e) {
      TreePath tp = stack_box.getPathForLocation(e.getX(), e.getY());

      if (tp != null) {
	 BussTreeNode tn = (BussTreeNode) tp.getLastPathComponent();
	 BussEntry entry = tn.getEntry();

	 if (entry != null) {
	    preview_bubble = createHoverBubble(entry, e.getX() - view_port.getViewPosition().x,
						  e.getY() - view_port.getViewPosition().y);
            noteEntryHovered(entry);
	  }
       }
    }

   @Override public void endHover(MouseEvent e) {
      if (preview_bubble != null){
	 preview_bubble.setVisible(false);
	 preview_bubble = null;
      }
    }

   void setPreviewBubble(BudaBubble previewBubble) {
      preview_bubble = previewBubble;
   }

   @Override protected void clearHover(MouseEvent e){
      super.clearHover(e);
   }

   @Override public void componentHidden(ComponentEvent e)	{ }

   @Override public void componentMoved(ComponentEvent e)	{ clearHover(null); }

   @Override public void componentResized(ComponentEvent e)	{ }

   @Override public void componentShown(ComponentEvent e)	{ }

}	// end of inner class Hoverer




/********************************************************************************/
/*										*/
/*	Handle clean up on removal						*/
/*										*/
/********************************************************************************/

@Override protected void localDispose()
{
   tree_model.disposeBubbles();
}



/********************************************************************************/
/*										*/
/*	BussViewport -- main bubble contents					*/
/*										*/
/********************************************************************************/

private static class BussViewport extends JScrollPane implements BudaConstants.BudaBubbleOutputer,
		MouseWheelListener
{
   private BussStackBox buss_stack;

   private static final long serialVersionUID = 1;

   BussViewport(BussStackBox bsb, JLayeredPane layeredPane) {
      buss_stack = bsb;

      JScrollBar jsb = new JScrollBar(Adjustable.VERTICAL);

      Dimension d1 = jsb.getPreferredSize();
      int xtra = d1.width;	   // space for scroll bar
      Dimension d = buss_stack.getPreferredSize();
      d.width += 4 + xtra;		// assume we will need a scroll bar
      d.height += 4;

      if (d.height >= BUSS_MAXIMUM_HEIGHT) d.height = BUSS_MAXIMUM_HEIGHT;
      else if (d.height < BUSS_MINIMUM_HEIGHT) d.height = BUSS_MINIMUM_HEIGHT;

      buss_stack.addMouseWheelListener(this);

      setViewportView(layeredPane);
      setPreferredSize(d);
      setSize(d);
      JScrollBar xjsb = getVerticalScrollBar();
      xjsb.setUnitIncrement(xjsb.getBlockIncrement());
    }

   @Override public String getConfigurator()		 { return "BUSS"; }

   @Override public void outputXml(BudaXmlWriter xw)	 { buss_stack.outputXml(xw); }

   @Override public void mouseWheelMoved(MouseWheelEvent e) {
      JScrollBar jsb = getVerticalScrollBar();
      if (jsb == null) return;
      int delta = e.getUnitsToScroll() * jsb.getBlockIncrement();
      int v = jsb.getValue();
      v += delta;
      if (v < 0) v = 0;
      jsb.setValue(v);
    }

}	// end of inner class BussViewport



}	// end of class BussBubble




/* end of BussBubble.java */
