/********************************************************************************/
/*										*/
/*		BicexStackViewPanel.java					*/
/*										*/
/*	Show a grpahic representation of the stack over time			*/
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.ivy.swing.SwingText;

class BicexStackViewPanel extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private StackPanel	stack_panel;
private boolean 	grow_down;


private static final int   LAYER_HEIGHT = 16;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexStackViewPanel(BicexEvaluationViewer ev)
{
   super(ev);
   stack_panel = null;
   grow_down = false;
}


/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override protected JComponent setupPanel()
{
   stack_panel = new StackPanel();

   return stack_panel;
}




@Override void update()
{
   stack_panel.repaint();
}


@Override void updateTime()
{
  stack_panel.repaint();
}



@Override long getRelevantTime(MouseEvent evt)
{
   if (stack_panel == null) return super.getRelevantTime(evt);

   double x = evt.getX();
   BicexEvaluationContext base = getRootContext();
   double t0 = base.getStartTime();
   double t1 = base.getEndTime();
   double wd = stack_panel.getWidth();
   double time = (x/wd)*(t1-t0) + t0;

   return (long) (time + 0.5);
}




/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private BicexEvaluationContext getContextForTime()
{
   return eval_viewer.getContextForTime(getRootContext(),getExecution().getCurrentTime());
}


private BicexEvaluationContext getContextForPosition(Point pt)
{
   if (stack_panel == null) return null;

   double x = pt.getX();
   BicexEvaluationContext base = getRootContext();
   if (base == null) return null;

   double t0 = base.getStartTime();
   double t1 = base.getEndTime();
   double wd = stack_panel.getWidth();
   double time = (x/wd)*(t1-t0) + t0;

   double y = pt.getY();
   if (!grow_down) {
      y = stack_panel.getHeight() - y;
    }
   int row = (int)(y/LAYER_HEIGHT);
   BicexEvaluationContext cur = eval_viewer.getContextForTime(base,(long) time,row);
   return cur;
}






/********************************************************************************/
/*										*/
/*	Stack panel							       */
/*										*/
/********************************************************************************/

private class StackPanel extends JPanel {

   private BicexEvaluationContext time_context;
   private Stroke highlight_stroke;

   private static final long serialVersionUID = 1;

   StackPanel() {
      time_context = null;
      highlight_stroke = new BasicStroke(2);
      setToolTipText("Stack Panel");
      addMouseListener(new ClickHandler());
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;

      g2.setColor(BoardColors.getColor("Bicex.StackViewBackground"));
      g2.fillRect(0,0,getWidth(),getHeight());

      time_context = getContextForTime();
      BicexEvaluationContext base = getRootContext();
      if (base == null) return;
      long t0 = base.getStartTime();
      long t1 = base.getEndTime();
      paintContext(g2,base,t0,t1,0);
    }

   private void paintContext(Graphics2D g,BicexEvaluationContext ctx,long t0,long t1,int lvl) {
      Color c = getColorForMethod(ctx.getMethod());
      String txt = ctx.getShortName();
      double wd = getWidth();
      double x0 = getXPosition(ctx.getStartTime(),t0,t1)*wd;
      double x1 = getXPosition(ctx.getEndTime(),t0,t1)*wd;
      double y0 = lvl * LAYER_HEIGHT;
      double y1 = y0 + LAYER_HEIGHT;
      if (!grow_down) {
	 double yx = getHeight() -y1;
	 y1 = getHeight() - y0;
	 y0 = yx;
       }
      Rectangle2D r2 = new Rectangle2D.Double(x0,y0,x1-x0,LAYER_HEIGHT);
      g.setColor(c);
      g.fill(r2);
      if (ctx == time_context) {
	 g.setColor(BoardColors.getColor("Bicex.StackViewOutline"));
	 g.setStroke(highlight_stroke);
	 g.draw(r2);
       }

      Color c1 = BoardColors.getTextColor(c);
      g.setColor(c1);
      SwingText.drawText(txt,g,r2);

      if (ctx.getInnerContexts() != null) {
	 for (BicexEvaluationContext sctx : ctx.getInnerContexts()) {
	    paintContext(g,sctx,t0,t1,lvl+1);
	  }
       }
    }

   private double getXPosition(double t,double t0,double t1) {
      return (t-t0)/(t1-t0);
    }

   @Override public String getToolTipText(MouseEvent evt) {
      BicexEvaluationContext cur = getContextForPosition(evt.getPoint());
      if (cur != null) return cur.getMethod();
      return "";
    }

}	// end of inner class StackPanel



/********************************************************************************/
/*										*/
/*	ClickHandler -- handle clicks in stack view				*/
/*										*/
/********************************************************************************/

private class ClickHandler extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent evt) {
      if (evt.getClickCount() > 1) return;
      if (evt.getButton() != MouseEvent.BUTTON1) return;
      BicexEvaluationContext cur = getContextForPosition(evt.getPoint());
      if (cur != null) getExecution().setCurrentContext(cur);
    }
}


}	// end of class BicexStackViewPanel




/* end of BicexStackViewPanel.java */

