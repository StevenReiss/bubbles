/********************************************************************************/
/*										*/
/*		BudaHintBubble.java						*/
/*										*/
/*	BUblles Display Area bubble for providing hints/undo/...		*/
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

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



class BudaHintBubble extends BudaBubble implements ActionListener, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JComponent	display_message;
private BudaHintActions hint_actions;
private int		display_time;
private int		delta_time;
private long		start_time;
private boolean 	fade_out;

private static final int FADE_INTERVAL = 1000;
private static final double START_TRANSPARENCY = 0.5;
private static final double END_TRANSPARENCY = 0.05;
private static final int MAX_INTERVALS = 20;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaHintBubble(String msg,BudaHintActions action,int time)
{
   super(BudaBorder.NONE);

   hint_actions = action;

   delta_time = Math.max(FADE_INTERVAL,time / MAX_INTERVALS);

   if (action == null) {
      display_message = new JLabel(msg);
      display_message.setOpaque(false);
    }
   else {
      JButton btn = new JButton(msg);
      btn.addActionListener(this);
      //btn.setOpaque(false);
      Color bkg = BoardColors.getColor("Buda.HintBackground");
      btn.setBackground(bkg);
      // btn.setBorderPainted(false);
      btn.setBorder(new EmptyBorder(0,0,0,0));
      btn.setContentAreaFilled(false);
      btn.putClientProperty("JButton.buttonType","toolbar");
      display_message = btn;
    }

   setFade(START_TRANSPARENCY);

   setContentPane(display_message);

   setShouldFreeze(false);
   setTransient(true);

   Dimension sz1 = getPreferredSize();
   setSize(sz1);
   setFixed(true);

   start_time = 0;
   display_time = time;
   if (time > 0) fade_out = true;
   else fade_out = false;
}



@Override protected void localDispose()
{
   if (hint_actions != null) hint_actions.finalAction();
   hint_actions = null;
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

void setFade(double fade)
{
   Color c = display_message.getForeground();
   int alpha = (int) (fade * 255);
   Color c1 = BoardColors.transparent(c,alpha);
   display_message.setForeground(c1);

   c = display_message.getBackground();
}


@Override public void paint(Graphics g)
{
   if (start_time == 0) {
      start_time = System.currentTimeMillis();
      Timer tt = new Timer(display_time,new RemoveAction());
      tt.setRepeats(false);
      tt.start();

      if (fade_out) {
	 tt = new Timer(delta_time,new FadeAction());
	 tt.start();
       }
    }

   super.paint(g);
}




/********************************************************************************/
/*										*/
/*	Action handler								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   if (hint_actions != null) hint_actions.clickAction();

   setVisible(false);
}




/********************************************************************************/
/*										*/
/*	Remove Action								*/
/*										*/
/********************************************************************************/

private final class RemoveAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      setVisible(false);
      if (hint_actions != null) hint_actions.finalAction();
      hint_actions = null;
    }

}	// end of inner class RemoveAction





/********************************************************************************/
/*										*/
/*	Fade Action								*/
/*										*/
/********************************************************************************/

private final class FadeAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      double delta = System.currentTimeMillis() - start_time;
      delta /= display_time;
      if (delta > 1) {
         Timer t = (Timer) e.getSource();
         t.stop();
       }
      else {
         setFade(START_TRANSPARENCY + delta * (END_TRANSPARENCY - START_TRANSPARENCY));
         // repaint();
       }
    }

}	// end of inner class FadeAction




}	// end of class BudaHintBubble




/* end of BudaHintBubble.java */
