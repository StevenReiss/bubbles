/********************************************************************************/
/*										*/
/*		BudaBubble.java 						*/
/*										*/
/*	BUblles Display Area bubble basic implementation			*/
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


package edu.brown.cs.bubbles.buda;


import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;

import edu.brown.cs.ivy.swing.SwingFreezePane;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.Document;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;



/**
 *	This class represents a bubble.  It is designed to be implemented by subclasses
 *	that define the internal component and set up various properties.
 *
 *	Bubbles appear on the display with a border and a content.  Various styles of
 *	border are supported.  The content can be an arbitrary Java Component.	There
 *	are various types of bubbles.  Standard bubbles exist in the overall bubble area.
 *	Fixed bubbles appear in a fixed place in the bubble area and do not take part
 *	in bubble movements.  Floating bubbles appear in a fixed location on the display
 *	and move themselves to that location when the bubble area is panned.
 *
 **/

public class BudaBubble extends JComponent implements BudaConstants,
		BudaConstants.BudaContentLocation, BudaConstants.BudaFileHandler,
		Printable
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Component	content_pane;
private Color		bubble_color;
private Color		border_color;
private Color		focus_color;
private Shape		border_shape;
private ChildManager	child_manager;
private Component	focus_pane;
private int		border_width;
private BudaBubbleGroup bubble_group;
private boolean 	is_fixed;
private boolean 	is_float;
private boolean 	is_docked;
private boolean 	is_userpos;
private BudaMovement	spacer_movement;
private boolean 	has_focus;
private BudaBubbleGroup original_group;
private boolean 	is_transient;
private Boolean 	is_resizable;
private Color		new_color;
private String		unique_id;
private long		creation_time;
private BudaBorder	border_type;
private double		scale_factor;

private BudaWorkingSet	fixed_set;
private Point		fixed_position;
private boolean 	fixed_stop;

private Stroke		border_stroke;
private Stroke		focus_stroke;
private Stroke		copy_stroke;

private static final long serialVersionUID = 1L;

protected static final Cursor default_cursor = Cursor.getDefaultCursor();

private static final Map<Object,Object> hint_map;


private static float [] DASH_BORDER = new float [] { 8,8 };

private static boolean no_freeze = BUDA_PROPERTIES.getBoolean("Buda.nofreeze");


static {
   hint_map = new HashMap<Object,Object>();
   hint_map.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   hint_map.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
   // hint_map.put(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Convenience constructor for new BudaBubble(null,BudaBorder.ROUNDED)
 **/

protected BudaBubble()
{
   this(null,BudaBorder.ROUNDED);
}


/**
 *	Convenience constructor for new BudaBubble(Component,BudaBorder.ROUNDED)
 **/

protected BudaBubble(Component c)
{
   this(c,BudaBorder.ROUNDED);
}


/**
 *	Convenience constructor for new BudaBubble(null,border)
 **/

protected BudaBubble(BudaBorder border)
{
   this(null,border);
}


/**
 *	Create a bubble encasing the given component with a border of the type
 *	specified.  Any object added to a bubble area must be a subtype of BudaBubble.
 *	The component can be null as part of this call, in which case it is  assumed
 *	that the call setContentPane(Component) will be made before the bubble is placed
 *	in the bubble area.
 **/

protected BudaBubble(Component c,BudaBorder bdr)
{
   border_type = bdr;

   switch (bdr) {
      case NONE :
	 border_width = 0;
	 break;
      case RECTANGLE :
	 border_width = (int) (BUBBLE_FOCUS_EDGE_SIZE + 0.5);
	 break;
      case ROUNDED :
	 border_width = (int) (BUBBLE_FOCUS_EDGE_SIZE + 1.5);
	 break;
    }

   child_manager = new ChildManager();

   addComponentListener(new BubbleManager());

   content_pane = null;
   focus_pane = null;
   unique_id = null;
   creation_time = System.currentTimeMillis();
   scale_factor = 1;

   setContentPane(c);

   float f1 = BUDA_PROPERTIES.getFloat(BUDA_EDGE_SIZE_PROP,BUBBLE_EDGE_SIZE);
   float f2 = BUDA_PROPERTIES.getFloat(BUDA_FOCUS_EDGE_SIZE_PROP,BUBBLE_FOCUS_EDGE_SIZE);

   border_stroke = new BasicStroke(f1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_BEVEL);
   focus_stroke = new BasicStroke(f2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
   copy_stroke = new BasicStroke(f2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,DASH_BORDER,0f);

   border_shape = null;
   is_fixed = false;
   is_userpos = false;
   spacer_movement = BudaMovement.ANY;
   has_focus = false;
   bubble_group = null;
   original_group = null;
   is_transient = false;
   new_color = null;
   bubble_color = null;
   fixed_set = null;
   fixed_position = null;
   fixed_stop = false;

   if (content_pane != null) content_pane.setCursor(getBubbleCursor());

   setBorderColor(BoardColors.getColor(BUBBLE_BORDER_COLOR_PROP),
	 BoardColors.getColor(BUBBLE_FOCUS_COLOR_PROP));
}



static BudaBubble createInternalBubble(Component c)
{
   return new BudaBubble(c,BudaBorder.NONE);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 *	Get the color of the border drawn for the bubble.
 **/

public Color getBorderColor()			{ return border_color; }



/**
 *	Get the color of the border when the bubble has the focus
 **/

public Color getBorderFocusColor()		{ return focus_color; }



/**
 *	Set the border colors.	The first argument is for when the bubble does
 *	not have the focus; the second is for when the bubble does have the focus.
 **/

public final void setBorderColor(Color c,Color fc)
{
   if (c != null) border_color = c;
   if (fc != null) focus_color = fc;
}


/**
 *	Get the color of the interior of the bubble.  If this is null, then
 *	the interior color defaults to that of the background (e.g. either the
 *	bubble area itself or the group if the bubble belongs to a group).  This
 *	should be used if the border is rounded since otherwise there might
 *	be gaps between the border and the interior rectangular bubble.
 **/

public Color getInteriorColor() 		{ return bubble_color; }


/**
 *	Set the interior color of the bubble.
 *	@see  #getInteriorColor
 **/

public final void setInteriorColor(Color c)
{
   bubble_color = c;
}



/**
 *	Get the content pane associated with the bubble.
 **/

public Component getContentPane()
{
   if (content_pane == null) return null;
   if (content_pane instanceof SwingFreezePane) {
      SwingFreezePane fp = (SwingFreezePane) content_pane;
      if (fp.getComponentCount() == 0) return null;
      return fp.getComponent(0);
    }
   return content_pane;
}


void forceFreeze()
{
   if (content_pane instanceof SwingFreezePane) {
      SwingFreezePane sfp = (SwingFreezePane) content_pane;
      sfp.forceFreeze();
    }
}


void unfreeze()
{
   if (content_pane instanceof SwingFreezePane) {
      SwingFreezePane sfp = (SwingFreezePane) content_pane;
      sfp.unfreeze();
    }
}

void refreshFreeze()
{
   unfreeze();
   forceFreeze();
}




/**
 *	Add a new focusable interior component for this bubble.  If this component
 *	has the text focus, then the bubble will be drawn as if it has the focus.
 **/

protected void addFocusComponent(Component f)
{
   if (focus_pane == content_pane) focus_pane = f;

   f.addFocusListener(child_manager);
}



void setGroup(BudaBubbleGroup bg)
{
   if (bubble_group != null) bubble_group.removeBubble(this);
   if (bg != null) bg.addBubble(this);
   bubble_group = bg;
}


public BudaBubbleGroup getGroup()		{ return bubble_group; }


void saveGroup()				{ original_group = bubble_group; }
BudaBubbleGroup getNewGroup()
{
   if (original_group != null && original_group.isEmpty()) return original_group;

   return new BudaBubbleGroup();
}



/**
 *	Indicate whether the bubble is currently a floating bubble or not
 **/

public boolean isFloating()
{
   if (fixed_set != null) {
      Rectangle r = fixed_set.getRegion();
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      Rectangle r1 = bba.getViewport();
      if (!r1.intersects(r)) return false;
    }
   return is_float;
}


void noteFloatMoved()
{
   if (fixed_set != null) {
      Rectangle r1 = fixed_set.getRegion();
      Rectangle r2 = getBounds();
      if (r1.contains(r2) && !fixed_stop) {
	 BudaRoot br = BudaRoot.findBudaRoot(this);
	 Point p1 = SwingUtilities.convertPoint(getParent(),getLocation(),br);
	 fixed_position = new Point(p1);
       }
      else if (!fixed_stop) {
	 fixed_stop = true;
	 System.err.println("MOVE OUT OF AREA");
	 r2.x = r1.x + 10;
	 setBounds(r2);
       }
    }
}


public boolean isFixed()			{ return is_fixed; }
public boolean isDocked()			{ return is_docked; }
public boolean isUserPos()			{ return is_userpos; }
public boolean isRemovable()			{ return true; }

/**
 *	Indicate that the bubble is fixed (i.e. should not be considered by the
 *	bubble spacer.
 **/

public void setFixed(boolean fg)
{
   is_fixed = fg;
   if (fg) setGroup(null);
}

/**
 *	Indicate that the bubble is floating (i.e. should remain in the same
 *	physical location on the screen even if the user changes the viewport.
 *	Floating bubbles are also forced to be on top, ungrouped, and do not
 *	take part in the bubble spacer.
 **/

void setFloating(boolean fg)
{
   is_float = fg;
   is_userpos = false;
   if (fg) {
      setGroup(null);
    }
   else {
      fixed_set = null;
      fixed_position = null;
    }
}



/**
 *	Indicate where a bubble is docked (i.e. permanently attached to one of the sides of
 *	the viewport).
 **/

public void setDocked(boolean fg)
{
   is_docked = fg;
   if (fg) {
      setGroup(null);
    }
}


void setUserPos(boolean fg)
{
   is_userpos = fg;
}



BudaMovement getMovement()			{ return spacer_movement; }

void setMovement(BudaMovement v)		{ spacer_movement = v; }

int getId()					{ return hashCode(); }



/**
 *	Tell if a bubble is transient or not.  Transient bubbles will not be
 *	saved as part of the configuration.
 **/

public boolean isTransient()			{ return is_transient; }


/**
 *	Indicate that this bubble is transient.
 **/

public void setTransient(boolean fg)		{ is_transient = fg; }



/**
 *	Indicate whether this bubble can be resized.  This can be affected
 *	by the minimum/maximum sizes of the content pane or by calling
 *	setResizable.
 *	@see #setResizable
 **/

public boolean isResizable()
{
   if (is_resizable != null) return is_resizable.booleanValue();
   if (content_pane == null) return true;

   Component c = getContentPane();
   if (c == null) return true;

   Dimension d1 = c.getMinimumSize();
   Dimension d2 = c.getMaximumSize();

   if (d1.width == 0 || d1.height == 0) return true;
   if (d1.width == d2.width && d1.height == d2.height) return false;

   return true;
}


/**
 *	Indicate that this bubble is/is not resizabe
 **/

protected void setResizable(boolean fg) 	{ is_resizable = fg; }


/**
 *	Get the minimum bubble width for resizing.
 **/

public int getMinimumResizeWidth()
{
   if (content_pane == null) return BUBBLE_MIN_SIZE;

   Dimension d1 = content_pane.getMinimumSize();
   if (d1.width > 0 && d1.width > BUBBLE_MIN_SIZE) return d1.width;
   return BUBBLE_MIN_SIZE;
}



/**
 *	Get the minimum bubble height for resizing
 **/

public int getMinimumResizeHeight()
{
   if (content_pane == null) return BUBBLE_MIN_SIZE;

   Dimension d1 = content_pane.getMinimumSize();
   if (d1.height > 0 && d1.height > BUBBLE_MIN_SIZE) return d1.height;
   return BUBBLE_MIN_SIZE;
}



@Override public void setToolTipText(String text)
{
   if (content_pane != null && content_pane instanceof JComponent) {
      JComponent jcp = (JComponent) content_pane;
      jcp.setToolTipText(text);
      return;
   }

   BoardLog.logE("BUDA","Attempt to set bubble tool tip text");
}




/********************************************************************************/
/*										*/
/*	Content pane methods							*/
/*										*/
/********************************************************************************/

/**
 * Set the content pane associated with the bubble.  Setting the content
 * pane should be done only once, either as part of the constructor or by
 * making this call during the construction process.
 **/

protected void setContentPane(Component c)
{
   setContentPane(c,c);
}



/**
 * Set the content pane associated with the bubble.  This method also
 * sets the interior pane that should have the text focus if this bubble
 * is focused.	Only one of the setContentPane routines should be called
 * and it should be called only once.
 **/

protected void setContentPane(Component c,Component f)
{
   if (c != null && !(c instanceof NoFreeze) && !no_freeze) setRealPane(new SwingFreezePane(c),f);
   else setRealPane(c,f);
}



/**
 * Get the frozen pane, if there is one
 **/

public Component getFrozenPane()
{
   if (content_pane == null) return null;
   if (content_pane instanceof SwingFreezePane) {
      return content_pane;
    }
   return null;
}



/**
 *	Indicate where the bubble may be frozen (saved as bitmap for redraw) or now
 **/

protected void setShouldFreeze(boolean b)
{
   if (content_pane instanceof SwingFreezePane) {
      SwingFreezePane sfp = (SwingFreezePane) content_pane;
      if (b) sfp.enableFreeze();
      else sfp.disableFreeze();
    }
}



private void setRealPane(Component c,Component f)
{
   if (content_pane != null) {
      content_pane.removeComponentListener(child_manager);
      focus_pane.removeFocusListener(child_manager);
      remove(content_pane);
    }

   content_pane = c;
   focus_pane = f;
   if (focus_pane == null) focus_pane = content_pane;

   setSizeFromContent();

   if (c != null) {
      content_pane.addComponentListener(child_manager);
      focus_pane.addFocusListener(child_manager);
      BudaCursorManager.setCursor(c,default_cursor);
      add(c);
      Point p0 = new Point((int) getContentLeftOffset(),(int) getContentTopOffset());
      c.setLocation(p0);
    }

   setSizeFromContent();
}




/********************************************************************************/
/*										*/
/*	Content access methods							*/
/*										*/
/********************************************************************************/

/**
 *	Return the eclipse project associated with this bubble if any.
 **/

@Override public String getContentProject()		{ return null; }



/**
 *	Return the source file associated with this bubble if any.
 **/

@Override public File getContentFile()			{ return null; }



/**
 *	Return the name of the content associated with the bubble.  For example,
 *	for a method bubble this would be the name of the method.
 **/

@Override public String getContentName()			{ return null; }


/**
 *	Return the document associated with the bubble.
 **/

public Document getContentDocument()		{ return null; }




/**
 *	Return information on how to interpret the content name associated with
 *	this bubble.
 *	@see #getContentName
 *	@see BudaConstants.BudaContentNameType
 **/

@Override public BudaContentNameType getContentType()	{ return BudaContentNameType.NONE; }



String getContentKey()
{
   String p1,p2,p3;

   p1 = getContentProject();
   if (getContentFile() == null) p2 = "";
   else p2 = getContentFile().getPath();
   p3 = getContentName();

   if (p1 == null) p1 = "*";
   if (p3 == null) return null;

   return p1 + "@" + p2 + "@" + p3;
}



void setCreationTime(long t)
{
   if (t > 0) creation_time = t;
}



/********************************************************************************/
/*										*/
/*	Scaling methods 							*/
/*										*/
/********************************************************************************/

protected void setScaleFactor(double sf)
{
   Component c = getContentPane();

   if (c != null && c instanceof Scalable) {
      Scalable sc = (Scalable) c;
      sc.setScaleFactor(sf);
      return;
    }
   else if (c != null && c instanceof JScrollPane) {
      JScrollPane jsp = (JScrollPane) c;
      JViewport vp = jsp.getViewport();
      if (vp != null && vp.getView() != null && vp.getView() instanceof Scalable) {
	 Scalable sc = (Scalable) vp.getView();
	 sc.setScaleFactor(sf);
	 return;
       }
    }

   double delta = sf/scale_factor;
   if (delta != 1) scaleFonts(c,delta);

   scale_factor = sf;
}


private void scaleFonts(Component c,double sf)
{
   if (c instanceof Container) {
      Container cc = (Container) c;
      for (int i = 0; i < cc.getComponentCount(); ++i) {
	 Component c1 = cc.getComponent(i);
	 scaleFonts(c1,sf);
       }
    }

   if (c.isFontSet()) {
      Font ft = c.getFont();
      if (ft != null) {
	 float sz = ft.getSize2D();
	 sz *= sf;
	 Font ft1 = ft.deriveFont(sz);
	 c.setFont(ft1);
       }
    }
}

public double getScaleFactor()
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   if (bba != null) return bba.getScaleFactor();
   else return 1;
}


/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

/**
 *	User requested quit.  This should take appropriate action (e.g. save contents)
 *	before quit.  It should return true if quit is okay.  A return of false will
 *	abort the quit.
 **/

@Override public boolean handleQuitRequest()
{
   return true;
}


/**
 *	User requested save all.  This should take whatever action is appropriate
 *	for the bubble.
 **/

@Override public void handleSaveRequest()			{ }

@Override public void handleSaveDone()				{ }

/**
 *	User requested commit all.  This should take whatever action is appropriate
 *	for the bubble.
 **/

@Override public void handleCommitRequest()			{ }






/**
 *	Timer based checkpoint.  This should do an interim save that would allow
 *	later recovery of files, contents, etc.
 **/

@Override public void handleCheckpointRequest() 	{ }


/**
 *	Called when user changes properties
 **/

@Override public void handlePropertyChange()		{ }



/**
 *	Handle popup menu requests
 **/

public void handlePopupMenu(MouseEvent e)		{ }




/********************************************************************************/
/*										*/
/*	Position and sizing methods						*/
/*										*/
/********************************************************************************/

@Override public Dimension getPreferredSize()
{
   Dimension d = content_pane.getPreferredSize();

   d.width += 2 * border_width;
   d.height += 2 * border_width;

   return d;
}




@Override public Dimension getMinimumSize()
{
   Dimension d = content_pane.getMinimumSize();

   if (d == null) return null;

   d.width += 2 * border_width;
   d.height += 2 * border_width;

   return d;
}




@Override public Dimension getMaximumSize()
{
   Dimension d = content_pane.getMaximumSize();

   if (d == null) return null;

   d.width += 2 * border_width;
   d.height += 2 * border_width;

   return d;
}




/**
 *	This routine returns the actual bubble at the given location.  Normally
 *	this is the current bubble.  However, items like bubble stacks might
 *	contain nested bubbles that should be used instead.  This routine provides
 *	a bubble with the way of indicating that.
 **/

public BudaBubble getActualBubble(int x,int y, boolean moved)
{
   return this;
}



private void setContentSizeFromBubble()
{
   if (content_pane != null) {
      Dimension d = getSize();

      int w1 = (int)(d.width - getContentLeftOffset() - getContentRightOffset());
      int h1 = (int)(d.height - getContentTopOffset() - getContentBottomOffset());

      Dimension dc = content_pane.getSize();
      if (dc.width != w1 || dc.height != h1) {
	 // BoardLog.logD("BUDA","RESIZE CONTENT " + dc.width + "," + dc.height + " => " + w1 + " " + h1);
	 content_pane.setSize(w1,h1);
       }
    }

   border_shape = null;
}




private void setSizeFromContent()
{
   if (content_pane == null) {
      setSize(50,50);
    }
   else {
      Dimension sz = content_pane.getSize();
      if (sz.width == 0 || sz.height == 0) sz = content_pane.getPreferredSize();
      Dimension d1 = new Dimension();
      d1.setSize(sz.width + getContentLeftOffset() + getContentRightOffset(),
		    sz.height + getContentTopOffset() + getContentBottomOffset());
      Dimension d0 = getSize();
      if (!d0.equals(d1)) {
	 // BoardLog.logD("BUDA","RESIZE BUBBLE " + d0 + " => " + d1);
	 setSize(d1);
	 content_pane.setSize(sz);
       }
    }

   border_shape = null;
}



/********************************************************************************/
/*										*/
/*	Handle bubbles fixed to a particular working set			*/
/*										*/
/********************************************************************************/

public void setWorkingSet(BudaWorkingSet ws)
{
   if (ws == null) {
      if (fixed_position != null) setLocation(fixed_position);
      fixed_set = null;
      fixed_position = null;
      fixed_stop = false;
      setFloating(false);
    }
   else {
      Point p = getLocation();
      Rectangle r = ws.getRegion();
      if (!r.contains(p)) return;
      setFloating(true);
      fixed_set = ws;
      fixed_stop = false;
      noteFloatMoved(); 		// sets position for reset
    }

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   bba.noteWorkingSetBubble(this,ws);
}


void checkWorkingSet()
{
   if (fixed_set == null) return;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   if (bba == null) return;
   if (fixed_stop && fixed_position != null) {
      Rectangle vpt = bba.getViewport();
      Rectangle wsr = fixed_set.getRegion();
      if (vpt.intersects(wsr)) {
	 BudaRoot br = BudaRoot.findBudaRoot(bba);
	 Point p0 = SwingUtilities.convertPoint(br,fixed_position,bba);
	 if (wsr.contains(p0)) {
	    fixed_stop = false;
	    setLocation(p0);
	  }
       }
    }
   else {
      Rectangle r1 = fixed_set.getRegion();
      Rectangle r2 = getBounds();
      Rectangle r3 = bba.getViewport();
      if (!r1.contains(r2)) {
	 fixed_stop = true;
	 r2.x = r1.x + 10;
	 setBounds(r2);
       }
      if (!r3.intersects(r1)) {
	 fixed_stop = true;
       }
    }
}

public void clearWorkingSet(BudaWorkingSet ws)
{
   if (fixed_set == ws) setWorkingSet(null);
}


BudaWorkingSet getFixedWorkingSet()		{ return fixed_set; }
Point getFixedWorkingSetPosition()		{ return fixed_position; }



/********************************************************************************/
/*										*/
/*	Location methods							*/
/*										*/
/********************************************************************************/

BudaRegion correlate(int x,int y)
{
   Rectangle r = getBounds();
   int x0 = x - r.x;
   int y0 = y - r.y;
   int x1 = r.width-x0;
   int y1 = r.height-y0;

   if (x0 < 0 || x0 > r.getWidth()) return BudaRegion.NONE;
   if (y0 < 0 || y0 > r.getHeight()) return BudaRegion.NONE;

   int x2 = (int) (x0 - getContentLeftOffset());
   int y2 = (int) (y0 - getContentRightOffset());

   if (content_pane != null && content_pane.contains(x2,y2)) return BudaRegion.COMPONENT;

   if (border_type != BudaBorder.NONE) {
      if (y0 < BUBBLE_BORDER_DELTA) {
	 if (x0 < BUBBLE_BORDER_DELTA) return BudaRegion.BORDER_NW;
	 else if (x1 < BUBBLE_BORDER_DELTA) return BudaRegion.BORDER_NE;
	 else return BudaRegion.BORDER_N;
       }
      if (y1 < BUBBLE_BORDER_DELTA) {
	 if (x0 < BUBBLE_BORDER_DELTA) return BudaRegion.BORDER_SW;
	 if (x1 < BUBBLE_BORDER_DELTA) return BudaRegion.BORDER_SE;
	 return BudaRegion.BORDER_S;
       }
      if (x0 < BUBBLE_BORDER_DELTA) return BudaRegion.BORDER_W;
      if (x1 < BUBBLE_BORDER_DELTA) return BudaRegion.BORDER_E;
    }

   return BudaRegion.COMPONENT;
}




/********************************************************************************/
/*										*/
/*	New bubble methods							*/
/*										*/
/********************************************************************************/

/**
 *	Indicate that this bubble has just been created.  This will cause it to
 *	be highlighted for a short while to draw the user's attention to its location.
 **/

public void markBubbleAsNew()
{
   new_color = BoardColors.getColor(NEW_COLOR_PROP);
   setBubbleNew(true);
   Timer nt = new Timer(NEW_BUBBLE_UPDATE_TIME,new NewUpdater());
   nt.setRepeats(true);
   nt.start();
   repaint();
}



private class NewUpdater implements ActionListener {

   private long start_time;

   NewUpdater() {
      start_time = System.currentTimeMillis();
    }

   @Override public void actionPerformed(ActionEvent e) {
      long now = System.currentTimeMillis();
      long delta = now - start_time;
      if (delta >= NEW_BUBBLE_SHOW_TIME || new_color == null) {
	 setBubbleNew(false);
	 Timer nt = (Timer) e.getSource();
	 nt.stop();
	 new_color = null;
       }
      else {
	 double f = 1.0 - ((double)(delta))/NEW_BUBBLE_SHOW_TIME;
	 int alpha = (int)(f * new_color.getAlpha());
	 new_color = BoardColors.transparent(new_color,alpha);
       }

      repaint();
    }

}	// end of inner class NewUpdater



protected void setBubbleNew(boolean fg) 	{ }




/********************************************************************************/
/*										*/
/*	Connection methods							*/
/*										*/
/********************************************************************************/

/**
 *	Handle a user specified connection.  This routine returns true if the
 *	bubble handles the connection itself, false otherwise.
 **/

public boolean connectTo(BudaBubble bb,MouseEvent evt)
{
   return false;
}



/********************************************************************************/
/*										*/
/*	Drawing methods 							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   super.paint(g);

//   if (scale_factor == 1 || scale_factor == 0 || this instanceof Scalable) {
//	super.paint(g);
//    }
//   else {
//	Graphics2D g1 = (Graphics2D) g.create();
//	g1.scale(scale_factor,scale_factor);
//	g1.setClip(0,0,getWidth(),getHeight());
//	super.paint(g1);
//    }

   if (new_color != null) {
      Graphics2D g2 = (Graphics2D) g.create();
      Shape s0 = getShape();
      g2.setColor(new_color);
      g2.fill(s0);
    }
}



@Override public void paintComponent(Graphics g0)
{
   Graphics2D g02 = (Graphics2D) g0;	// set graphics hints for children
   g02.setRenderingHints(hint_map);

   Graphics2D g2 = (Graphics2D) g0.create();

   Shape s0 = getShape();

   if (bubble_color != null) {
      g2.setColor(bubble_color);
      g2.fill(s0);
    }

   paintBubbleBorder(g2);
}



protected void paintOverview(Graphics2D g)
{
   Shape s0 = getShape();

   if (bubble_color != null) {
      g.setColor(bubble_color);
      //g.fill(s0);//commented out by Ian Strickman
    }

   paintContentOverview(g,s0);

   if (border_type == BudaBorder.NONE) return;

   g.setColor(border_color);
   g.setStroke(border_stroke);
   g.draw(s0);
}



protected void paintContentOverview(Graphics2D g,Shape s)	{ }



protected void paintBubbleBorder(Graphics2D g2)
{
   if (border_type != BudaBorder.NONE) {
      if (has_focus) {
	 g2.setColor(focus_color);
	 g2.setStroke(focus_stroke);
       }
      else {
	 g2.setColor(border_color);
	 g2.setStroke(border_stroke);
       }
      if (is_userpos) {
	 Color c = BoardColors.getColor("Buda.userpos.color");
	 g2.setColor(c);
       }

      Shape s0 = getShape();
      g2.draw(s0);
      if (!has_focus && getContentKey() != null) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 if (bba != null && getContentKey().equals(bba.getFocusKey()) &&
		bba.getFocusBubble() != this) {
	    g2.setColor(focus_color);
	    g2.setStroke(copy_stroke);
	    g2.draw(s0);
	  }
       }
    }
}




Point scalePoint(Point p0)
{
   double sf = getScaleFactor();
   if (sf == 1) return p0;

   return new Point((int)(p0.x * sf),(int)(p0.y * sf));
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

final void outputBubbleXml(BudaXmlWriter xw,BudaBubbleScaler bs)
{
   xw.begin("BUBBLE");
   xw.field("ID",getId());
   xw.field("FIXED",isFixed());
   xw.field("FLOAT",isFloating());
   xw.field("DOCKED",isDocked());
   xw.field("INTERIOR",bubble_color);
   xw.field("BORDER",border_color);
   xw.field("FOCUS",focus_color);
   xw.field("TRANSIENT",isTransient());
   xw.field("CTIME",creation_time);
   Rectangle r = getBounds();
   if (bs != null) r = bs.getScaledBounds(this);
   xw.field("X",r.x);
   xw.field("Y",r.y);
   xw.field("W",r.width);
   xw.field("H",r.height);
   if (!isVisible()) xw.field("VISIBLE",false);

   BudaBubbleOutputer bbo = getBubbleOutputer();

   if (bbo != null) {
      String key = bbo.getConfigurator();
      xw.field("CONFIG",key);
      xw.begin("CONTENT");
      bbo.outputXml(xw);
      xw.end("CONTENT");
    }

   xw.end("BUBBLE");
}



BudaBubbleOutputer getBubbleOutputer()
{
   if (this instanceof BudaBubbleOutputer) return (BudaBubbleOutputer) this;

   return findBubbleOutputer(content_pane);
}



private BudaBubbleOutputer findBubbleOutputer(Component c)
{
   if (c == null) return null;

   if (c instanceof BudaBubbleOutputer) return (BudaBubbleOutputer) c;

   if (c instanceof JScrollPane) {
      JScrollPane sp = (JScrollPane) c;
      JViewport vp = sp.getViewport();
      return findBubbleOutputer(vp.getView());
    }

   if (c instanceof SwingFreezePane) {
      SwingFreezePane fp = (SwingFreezePane) c;
      if (fp.getComponentCount() > 0)
	return findBubbleOutputer(fp.getComponent(0));
    }

   return null;
}


/********************************************************************************/
/*										*/
/*	Positioning methods							*/
/*										*/
/********************************************************************************/

private double getContentLeftOffset()		{ return border_width; }

private double getContentRightOffset()		{ return border_width; }

private double getContentTopOffset()		{ return border_width; }

private double getContentBottomOffset() 	{ return border_width; }

private double getShapeLeftOffset()		{ return (BUBBLE_FOCUS_EDGE_SIZE/2); }

private double getShapeRightOffset()		{ return (BUBBLE_FOCUS_EDGE_SIZE/2); }

private double getShapeTopOffset()		{ return (BUBBLE_FOCUS_EDGE_SIZE/2); }

private double getShapeBottomOffset()		{ return (BUBBLE_FOCUS_EDGE_SIZE/2); }




/********************************************************************************/
/*										*/
/*	Shape Methods								*/
/*										*/
/********************************************************************************/

protected Shape getShape()
{
   if (border_shape == null) {
      Dimension sz = getSize();

      double x0 = getShapeLeftOffset() + 0.5;
      double y0 = getShapeTopOffset() + 0.5;
      double w0 = sz.getWidth() - getShapeLeftOffset() - getShapeRightOffset() - 1.5;
      double h0 = sz.getHeight() - getShapeTopOffset() - getShapeBottomOffset() - 1.5;

      switch (border_type) {
	 case NONE :
	 case RECTANGLE :
	    border_shape = new Rectangle2D.Double(x0,y0,w0,h0);
	    break;
	 case ROUNDED :
	    border_shape = new RoundRectangle2D.Double(x0,y0,w0,h0,BUBBLE_ARC_SIZE,BUBBLE_ARC_SIZE);
	    break;
       }
    }

   return border_shape;
}




/********************************************************************************/
/*										*/
/*	Cursor methods								*/
/*										*/
/********************************************************************************/

protected Cursor getBubbleCursor()
{
   return null; 	//default_cursor;
}




/********************************************************************************/
/*										*/
/*	Component Management Methods						*/
/*										*/
/********************************************************************************/

/**
 *	Give this bubble the focus.  This will actually pass the focus on to whatever
 *	component has been registered as the focus component
 *	@see #setContentPane(java.awt.Component,java.awt.Component)
 *	@see #addFocusComponent
 **/

@Override public void grabFocus()
{
   Component c0 = getContentPane();

   if (focus_pane != null && focus_pane instanceof JComponent) {
      JComponent c = (JComponent) focus_pane;
      c.grabFocus();
      // System.err.println("GRAB FOCUS FOR " + c);
    }
   else if (c0 != null && c0 instanceof JComponent) {
      JComponent c = (JComponent) c0;
      c.grabFocus();
      // System.err.println("GRAB FOCUS FOR " + c);
    }
   else {
      super.grabFocus();
    }
}



private class BubbleManager extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      setContentSizeFromBubble();
    }

}	// end of inner class BubbleManager




private class ChildManager extends ComponentAdapter implements FocusListener {

   @Override public void componentResized(ComponentEvent e) {
      setSizeFromContent();
    }

   @Override public void componentHidden(ComponentEvent e) {
      setVisible(false);
    }

   @Override public void focusGained(FocusEvent e) {
      // System.err.println("FOCUS GAINED " + content_pane);
      has_focus = true;
      repaint();
      propogateFocus();
    }

   @Override public void focusLost(FocusEvent e) {
      // System.err.println("FOCUS LOST " + content_pane);
      has_focus = false;
      repaint();
      propogateFocus();
    }

   private void propogateFocus() {
      Component par = BudaBubble.this.getParent();
      if (par != null && par instanceof BudaBubbleArea) {
	 BudaBubbleArea bba = (BudaBubbleArea) par;
	 bba.setFocusBubble(BudaBubble.this,has_focus);
       }
    }

}	// end of inner class ChildManager




/********************************************************************************/
/*										*/
/*	Removal methods 							*/
/*										*/
/********************************************************************************/

/**
 *	Call when one is done with a bubble.  This will ensure that its resources
 *	are released.
 **/

public void disposeBubble()
{
   if (focus_pane != null) focus_pane.removeFocusListener(child_manager);
   if (content_pane != null) content_pane.removeComponentListener(child_manager);

   localDispose();

   if (content_pane instanceof SwingFreezePane) {
      SwingFreezePane sfp = (SwingFreezePane) content_pane;
      sfp.dispose();
    }
}


/**
 *	This is invoked when the bubble is actually removed and any undo remove
 *	has been eliminated.  The bubble won't be made visible after this call.
 **/

protected void localDispose()			{ }




/********************************************************************************/
/*										*/
/*	Metrics methods 							*/
/*										*/
/********************************************************************************/

/**
 *	Return a hash value identifying this bubble by content
 **/

public String getHashId()
{
   if (unique_id != null) return unique_id;

   String s1 = getContentProject();
   File f2 = getContentFile();
   String s3 = getContentName();
   if (s1 == null && f2 == null && s3 == null) {
      Component c = getContentPane();
      if (c != null) return c.getClass().getName();
      return "BX" + Integer.toString(System.identityHashCode(this));
    }

   byte [] drslt;

   try {
      MessageDigest mdi = MessageDigest.getInstance("MD5");
      if (s1 != null) mdi.update(s1.getBytes());
      if (f2 != null) mdi.update(f2.getPath().getBytes());
      if (s3 != null) mdi.update(s3.getBytes());
      drslt = mdi.digest();
    }
   catch (NoSuchAlgorithmException e) {
      return null;
    }

   int rslt = 0;
   for (int i = 0; i < drslt.length; ++i) {
      int j = i % 4;
      rslt ^= (drslt[i] << (j*8));
    }
   rslt &= 0x7fffffff;

   unique_id = "B" + Integer.toString(rslt);

   return unique_id;
}



protected void noteResize(int owd,int oht)
{
   Dimension d = getSize();

   BoardMetrics.noteCommand("BUDA","bubbleResized_" + getHashId() + "_" +
			       owd + "_" + oht + "_" + d.width + "_" + d.height);
}



/********************************************************************************/
/*										*/
/*     Printing methods 							*/
/*										*/
/********************************************************************************/

@Override public int print(Graphics g0,PageFormat fmt,int idx)
{
   if (idx > 0) return Printable.NO_SUCH_PAGE;

   Color basecolor = BoardColors.getColor("Buda.BubbleBaseColor");

   Dimension d1 = getSize();
   Dimension d = new Dimension((int) fmt.getWidth(),(int) fmt.getHeight());
   int res = 72;
   Dimension d0 = new Dimension(d);

   int margin = (int)(res * 0.5);
   d.width -= 2*margin;
   d.height -= 2*margin;

   double dx = d.getWidth()/d1.getWidth();
   double dy = d.getHeight()/d1.getHeight();
   double dz = Math.min(dx,dy);

   Graphics2D g = (Graphics2D) g0;

   Color cl = g.getColor();
   g.setColor(basecolor);
   g.fillRect(0,0,d0.width,d0.height);
   g.setColor(cl);
   int w = d1.width;
   int h = d1.height;
   if (w > d.width) w = d.width;
   if (h > d.height) h = d.height;
   g.clipRect(margin,margin,w,h);
   g.translate(margin,margin);
   g.scale(dz,dz);
   paint(g);

   return Printable.PAGE_EXISTS;
}


/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

/**
 *	Return an action to switch this bubble between floating and fixed
 **/

public Action getFloatBubbleAction()
{
   return new FloatAction();
}



private class FloatAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   FloatAction() {
      super(isFloating() ? "Make Floating" : "Make Fixed");
      putValue(SHORT_DESCRIPTION,"Set whether the bubble is fixed on the display or not");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      boolean fg = !isFloating();
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BudaBubble.this);
      if (bba != null) {
	 bba.setBubbleFloating(BudaBubble.this,fg);
	 if (fg) {
	    for (BudaWorkingSet bws : bba.getWorkingSets()) {
	       if (bws.isSticky()) {
		  Rectangle r = bws.getRegion();
		  if (r.contains(getLocation())) {
		     setWorkingSet(bws);
		     break;
		   }
		}
	     }
	  }
       }
    }

}	// end of FloatAction



protected Action getRemoveBubbleAction()
{
   return new RemoveAction();
}


private class RemoveAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RemoveAction() {
      super("Remove This Bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BudaBubble.this);
      if (bba != null) bba.userRemoveBubble(BudaBubble.this);
    }

}	// end of FloatAction


/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   String  r = "BUBBLE " + getBounds();
   if (content_pane != null && getContentPane() != null) {
      r += " :: " + getContentPane().getClass().toString();
    }
   return r;
}



}	// end of class BudaBubble




/* end of class BudaBubble.java */
