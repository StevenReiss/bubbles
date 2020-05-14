/********************************************************************************/
/*										*/
/*		BudaHover.java							*/
/*										*/
/*	BUblles Display Area abstract class for handling hovers 		*/
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

import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Timer;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Map;
import java.util.WeakHashMap;



/**
 *	This class provides extended support for hovering.  Whereas tool tips
 *	restrict you to a JToolTip class being popped up, this class provides
 *	a more general mechanism.
 *
 *	It is used by implementing a subclass and providing the underlying
 *	component for which hover will occur in the constructor.
 *
 **/

public abstract class BudaHover implements ActionListener, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private MouseEvent	last_mouse;
private Timer		delay_timer;
private int		hover_time;
private JViewport	use_viewport;
private Point		view_point;
private boolean 	doing_hover;


private static Map<BudaHover,Boolean> all_hovers = new WeakHashMap<BudaHover,Boolean>();
private static int	num_hover = 0;
private static boolean	hovers_enabled = true;

private static final int HOVER_TIME = 500;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Start a hovering mechanism for the given component.
 **/

protected BudaHover(Component c)
{
   last_mouse = null;
   delay_timer = new Timer(0,this);
   delay_timer.setRepeats(false);
   hover_time = HOVER_TIME;
   use_viewport = null;
   view_point = null;
   doing_hover = false;

   Mouser m = new Mouser();
   c.addMouseMotionListener(m);
   c.addMouseListener(m);
   c.addKeyListener(new Keyer());

   Comper cc = new Comper();
   c.addComponentListener(cc);
   c.addHierarchyListener(cc);

   boolean havescroll = false;
   for (Component p = c; p != null; p = p.getParent()) {
      if (p instanceof JViewport) use_viewport = (JViewport) p;
      else if (p instanceof JScrollPane) {
	 havescroll = true;
	 break;
       }
    }

   if (!havescroll) c.addMouseWheelListener(m);
   
   all_hovers.put(this, Boolean.TRUE);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 *	Set the time (in ms) before hover should occur.
 **/

public void setHoverTime(int t) 		{ hover_time = t; }



/**
 *	Return the time (in ms) before hover should occur.
 **/

public int getHoverTime()			{ return hover_time; }




/********************************************************************************/
/*										*/
/*	User callbacks								*/
/*										*/
/********************************************************************************/

/**
 *	This routine is invoked when the user has hovered for the designated
 *	amount of time over the given component.  The passed in event is the
 *	last mouse event that occurred.
 **/

public abstract void handleHover(MouseEvent e);


/**	This routine is invoked when the user moves the mouse again after a hover.
 *
 **/

public abstract void endHover(MouseEvent e);



/********************************************************************************/
/*										*/
/*	Handle callback from the timer						*/
/*										*/
/********************************************************************************/

void simulateHover(MouseEvent e)
{
   if (!hovers_enabled) return;
   
   try {
      handleHover(e);
      doing_hover = true;
      last_mouse = null;
    }
   catch (Throwable t) {
      BoardLog.logE("BUDA","Problem handling hover",t);
    }
}



@Override public void actionPerformed(ActionEvent e)
{

   if (use_viewport != null && !use_viewport.getViewPosition().equals(view_point)) {
      last_mouse = null;
    }

   if (last_mouse == null || !hovers_enabled) return;

   long now = System.currentTimeMillis();

   if (now - last_mouse.getWhen() >= hover_time) {
      try {
	 handleHover(last_mouse);
       }
      catch (Throwable t) {
	 BoardLog.logE("BUDA","Problem handling hover",t);
       }
      ++num_hover;
      doing_hover = true;
      last_mouse = null;
    }
   else {
      int delta = hover_time - ((int) (now - last_mouse.getWhen()));

      delay_timer.setInitialDelay(delta);
      delay_timer.restart();
    }
}


protected void clearHover(MouseEvent e)
{
   last_mouse = null;

   if (!doing_hover) return;

   try {
      endHover(e);
    }
   catch (Throwable t) {
      BoardLog.logE("BUDA","Problem handling hover end",t);
    }
   doing_hover = false;
   --num_hover;
}



/********************************************************************************/
/*										*/
/*	Handle mouse events							*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      clearHover(e);
    }

   @Override public void mouseDragged(MouseEvent e) {
      clearHover(e);
    }

   @Override public void mouseEntered(MouseEvent e) {
      clearHover(e);
    }

   @Override public void mouseExited(MouseEvent e) {
      clearHover(e);
    }

   @Override public void mouseMoved(MouseEvent e) {
      clearHover(e);
      last_mouse = e;
      if (!delay_timer.isRunning()) {
	 delay_timer.setInitialDelay(hover_time);
	 delay_timer.start();
       }
      if (use_viewport != null) {
	 view_point = use_viewport.getViewPosition();
       }
    }

   @Override public void mousePressed(MouseEvent e) {
      clearHover(e);
    }

   @Override public void mouseReleased(MouseEvent e) {
      clearHover(e);
    }

   @Override public void mouseWheelMoved(MouseWheelEvent e) {
      clearHover(e);
    }

}	// end of inner class Mouser



private class Keyer extends KeyAdapter {

   @Override public void keyPressed(KeyEvent e) {
      clearHover(null);
    }

}	// end of inner class Keyer



private class Comper extends ComponentAdapter implements HierarchyListener {

   @Override public void componentHidden(ComponentEvent e) {
      clearHover(null);
    }

   @Override public void hierarchyChanged(HierarchyEvent e) {
      if (!e.getComponent().isShowing() && doing_hover)
	 clearHover(null);
   }

}	// end of inner class Comper


public static void removeHovers()
{
   if (num_hover > 0) {
      for (BudaHover bh : all_hovers.keySet()) {
	 bh.clearHover(null);
       }
    }
}

public static void enableHovers(boolean fg)
{
   if (!fg) removeHovers();
   
   hovers_enabled = fg;
}



}	// end of class BudaHover




/* end of BudaHover.java */
