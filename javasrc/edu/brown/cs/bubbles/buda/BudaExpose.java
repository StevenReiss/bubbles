/********************************************************************************/
/*										*/
/*		BudaExpose.java 						*/
/*										*/
/*	BUblles Display Area expose global action handler			*/
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

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;



class BudaExpose extends AbstractAction implements ActionListener, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot	for_root;
private BudaBubbleArea	bubble_area;
// private Rectangle	start_viewport;
// private int		center_x;
// private int		center_y;
// private int		target_x;
// private int		target_y;
private Mouser		mouse_handler;
private Keyer		key_handler;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaExpose(BudaRoot br,BudaBubbleArea ba)
{
   for_root = br;
   bubble_area = ba;
   mouse_handler = new Mouser();
   key_handler = new Keyer();
}


/********************************************************************************/
/*										*/
/*	Action handler								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   bubble_area = for_root.getCurrentBubbleArea();

  // start_viewport = for_root.getViewport();
   // center_x = start_viewport.x + start_viewport.width/2;
  //  center_y = start_viewport.y + start_viewport.height/2;
  // target_x = center_x;
  // target_y = center_y;

   JComponent glass = (JComponent) for_root.getGlassPane();
   glass.addMouseListener(mouse_handler);
   glass.addMouseMotionListener(mouse_handler);
   glass.addMouseWheelListener(mouse_handler);
   glass.addKeyListener(key_handler);
   glass.setVisible(true);
   glass.requestFocusInWindow();

   for_root.hideSearchBubble();
   //for_root.togglePackageExplorer();

   Timer t = new Timer(EXPOSE_ANIMATE_INTERVAL,new Scaler(1.0,0.5,EXPOSE_ANIMATE_DELTA));
   t.setInitialDelay(0);
   t.start();
}



private void restore(boolean aimAtBubble)
{
  // start_viewport = for_root.getViewport();
   // center_x = (int)(start_viewport.x / for_root.getScaleFactor()) + start_viewport.width;
   // center_y = (int)(start_viewport.y / for_root.getScaleFactor()) + start_viewport.height;

   if(!aimAtBubble){
      // target_x = center_x;
      // target_y = center_y;
    }

   JComponent glass = (JComponent) for_root.getGlassPane();
   glass.removeMouseListener(mouse_handler);
   glass.removeMouseMotionListener(mouse_handler);
   glass.removeMouseWheelListener(mouse_handler);
   glass.removeKeyListener(key_handler);
   glass.setVisible(false);

   Timer t = new Timer(EXPOSE_ANIMATE_INTERVAL,new Scaler(0.5,1.0,EXPOSE_ANIMATE_DELTA));
   t.setInitialDelay(0);
   t.start();

   //for_root.togglePackageExplorer();
}



/********************************************************************************/
/*										*/
/*	Correlation methods							*/
/*										*/
/********************************************************************************/

private BudaBubble findBubble(int x,int y)
{
   double sf = for_root.getScaleFactor();

   int x0 = (int)(x / sf);
   int y0 = (int)(y / sf);

   Component c = bubble_area.getComponentAt(x0,y0);
   if (c != null && c instanceof BudaBubble) return (BudaBubble) c;

   return null;
}




/********************************************************************************/
/*										*/
/*	Mouse handler								*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      Point p0 = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),
						bubble_area);
      BudaBubble bb = findBubble(p0.x,p0.y);
      if (bb != null) {
	//  target_x = bb.getX() + bb.getWidth()/2;
	// target_y = bb.getY() + bb.getHeight()/2;
	 restore(true);
       }
    }

   @Override public void mousePressed(MouseEvent e) {
      bubble_area.processMouseEvent(e);
    }

   @Override public void mouseReleased(MouseEvent e) {
      bubble_area.processMouseEvent(e);

      JComponent glass = (JComponent) for_root.getGlassPane();
      glass.requestFocusInWindow();
    }

   @Override public void mouseDragged(MouseEvent e) {
      bubble_area.processMouseMotionEvent(e);
    }

   @Override public void mouseMoved(MouseEvent e) {
      bubble_area.processMouseMotionEvent(e);
    }

   @Override public void mouseExited(MouseEvent e) {
      bubble_area.processMouseMotionEvent(e);
    }
}	// end of inner class Mouser




/********************************************************************************/
/*										*/
/*	Key handler								*/
/*										*/
/********************************************************************************/

private class Keyer extends KeyAdapter {

   @Override public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_F9) {
         restore(false);
       }
    }

}	// end of inner class Keyer



/********************************************************************************/
/*										*/
/*	Scaler: animate scaling the bubble area 				*/
/*										*/
/********************************************************************************/

private class Scaler implements ActionListener {

   private double start_scale;
   private double end_scale;
   private double cur_scale;
   private double scale_delta;

   Scaler(double start,double end,double delta) {
      start_scale = start;
      end_scale = end;
      scale_delta = Math.abs(delta);
      cur_scale = start_scale;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (start_scale < end_scale) {
         cur_scale += scale_delta;
         if (cur_scale > end_scale) cur_scale = end_scale;
       }
      else {
         cur_scale -= scale_delta;
         if (cur_scale < end_scale) cur_scale = end_scale;
       }
   
      // double d = (cur_scale - start_scale)/(end_scale - start_scale);
      // int x0 = (int)(center_x + d*(target_x - center_x));
      // int y0 = (int)(center_y + d*(target_y - center_y));
      // for_root.setScaleFactor(cur_scale,x0,y0);
      bubble_area.setScaleFactor(cur_scale);
   
      if (cur_scale == end_scale) {
         Timer t = (Timer) e.getSource();
         t.stop();
       }
    }

}	// end of inner class Scaler



}	// end of class BudaExpose




/* end of BudaExpose.java */
