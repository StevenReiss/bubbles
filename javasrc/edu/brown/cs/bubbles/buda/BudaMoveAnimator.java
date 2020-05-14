/********************************************************************************/
/*										*/
/*		BudaAnimator.java						*/
/*										*/
/*	BUblles Display Area animations 					*/
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

import javax.swing.Timer;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


class BudaMoveAnimator implements ActionListener, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BudaBubbleArea		bubble_area;
private Collection<Movement>	move_bubbles;
private Timer			move_timer;
private List<BudaBubble>  refresh_set;

private static final int	FRAME_DELAY = 10;
private static final int	FRAME_MOVE = 16;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaMoveAnimator()
{
   move_timer = new Timer(FRAME_DELAY,null);
   bubble_area = null;

   move_bubbles = new ArrayList<Movement>();
   refresh_set = null;

   move_timer.addActionListener(this);
   move_timer.setActionCommand("BUDAANIMATE");
   move_timer.setRepeats(true);

   move_timer.setInitialDelay(0);
}



/********************************************************************************/
/*										*/
/*	Set up methods								*/
/*										*/
/********************************************************************************/

synchronized void moveBubble(BudaBubble m,Point target,boolean update)
{
   move_bubbles.add(new Movement(m,target));

   if (move_bubbles.size() == 1) {
      refresh_set = null;
      if (bubble_area == null) bubble_area = BudaRoot.findBudaBubbleArea(m);
      if (bubble_area == null) return;
      move_timer.start();
    }

   if (update) {
      if (refresh_set == null) refresh_set = new ArrayList<BudaBubble>();
      refresh_set.add(m);
    }
}



synchronized boolean isActive()
{
   return move_timer.isRunning();
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   boolean stop = false;
   List<BudaBubble> upds = null;

   synchronized (this) {
      for (Iterator<Movement> it = move_bubbles.iterator(); it.hasNext(); ) {
	 Movement mb = it.next();
	 if (mb.nextMove()) it.remove();
       }
      if (move_bubbles.size() == 0) {
	 stop = true;
	 upds = refresh_set;
	 refresh_set = null;
       }
    }

   if (stop) {
      move_timer.stop();
      bubble_area.checkAreaDimensions();
    }

   if (upds != null && upds.size() > 0) {
      for (BudaBubble xbb : upds) {
	 bubble_area.fixupBubble(xbb);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Movement animator							*/
/*										*/
/********************************************************************************/

private static class Movement
{
   private BudaBubble	for_bubble;
   private Point	target_point;
   private Point	start_point;
   private double	total_distance;
   private double	move_count;

   Movement(BudaBubble m,Point target) {
      for_bubble = m;
      target_point = target;
      start_point = m.getLocation();
      total_distance = start_point.distance(target_point);
      move_count = 0;
    }

   boolean nextMove() {
      if (total_distance == 0) return true;

      move_count += FRAME_MOVE;
      if (move_count > total_distance) move_count = total_distance;

      double xd = target_point.x - start_point.x;
      double yd = target_point.y - start_point.y;
      double x1 = start_point.x + xd * move_count / total_distance;
      double y1 = start_point.y + yd * move_count / total_distance;

      for_bubble.setLocation((int) x1,(int) y1);

      return move_count == total_distance;
    }

}	// end of inner class Movement




}	// end of class BudaMoveAnimator




/* end of BudaMoveAnimator.java */
