/********************************************************************************/
/*										*/
/*		BicexGraphicsPanel.java 					*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bicex;

import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;


class BicexGraphicsPanel extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DisplayModel	display_model;
private DisplayPane	display_pane;

private static Paint           background_paint;

static {
   BufferedImage bim = BoardImage.getBufferedImage("empty");
   background_paint = new TexturePaint(bim,new Rectangle(0,0,bim.getWidth(),bim.getHeight())); 
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexGraphicsPanel(BicexEvaluationViewer ev,DisplayModel mdl)
{
   super(ev);

   display_model = mdl;
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

@Override protected JComponent setupPanel()
{
   display_pane = new DisplayPane();

   return display_pane;
}


@Override boolean useHeavyScroller()                    { return true; }




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override void update()
{
   if (display_pane != null) display_pane.update();
}


@Override void updateTime()
{
   if (display_model.useTime()) {
      if (display_pane != null) display_pane.repaint();
    }
}




/********************************************************************************/
/*										*/
/*	Graphics panel								*/
/*										*/
/********************************************************************************/

private class DisplayPane extends JPanel {

   private static final long serialVersionUID = 1;

   DisplayPane() {
      setSize();
    }

   void update() {
      if (display_model.getWidth() != getWidth() ||
            display_model.getHeight() != getHeight()) {
         setSize();
       }
      repaint();
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      Paint oldpt = g2.getPaint();
      g2.setPaint(background_paint);
      g2.fillRect(0,0,getWidth(),getHeight());
      g2.setPaint(oldpt);
      BoardLog.logD("BICEX","REPAINT WITH " + g2.getClipBounds());
      display_model.paintToTime(g2,getExecution().getCurrentTime());
    }

   private void setSize() {
      if (display_model == null) return;
      Dimension sz = new Dimension(display_model.getWidth(),display_model.getHeight());
      setSize(sz);
      setPreferredSize(sz);
      setMinimumSize(sz);
      setMaximumSize(sz);
    }

}	// end of inner class DisplayPane






}	// end of class BicexGraphicsPanel




/* end of BicexGraphicsPanel.java */

