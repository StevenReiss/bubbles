/********************************************************************************/
/*										*/
/*		BicexBubble.java						*/
/*										*/
/*	Generic bubble code for Bicex views					*/
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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.board.BoardColors;


abstract class BicexPanel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected BicexEvaluationViewer 	eval_viewer;
private   JComponent			panel_component;
private   Map<String,Color>             color_map;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BicexPanel(BicexEvaluationViewer ev)
{
   eval_viewer = ev;
   panel_component = null;
   color_map = null;

   setupPanel();
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JComponent getComponent()
{
   if (panel_component == null) {
      panel_component = setupPanel();
    }

   return panel_component;
}



protected BicexDataModel getDataModel()
{
   return eval_viewer.getDataModel();
}



protected BicexEvaluationContext getRootContext()
{
   return eval_viewer.getContext();
}

protected BicexEvaluationContext getContext()
{
   return getRootContext();
}


protected BicexExecution getExecution()
{
   return eval_viewer.getExecution();
}


long getRelevantTime(MouseEvent evt)
{
   return getExecution().getCurrentTime();
}




boolean useHeavyScroller()		{ return false; }

boolean allowMouseWheelScrolling()	{ return true; }




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

AbstractAction getContextAction(BicexEvaluationContext ctx)
{
   return getContextAction("Go To " + ctx.getShortName(),ctx);
}

AbstractAction getContextAction(String msg,BicexEvaluationContext ctx)
{
   return eval_viewer.getContextAction(msg,ctx);
}

AbstractAction getTimeAction(String msg,long time)
{
   return eval_viewer.getTimeAction(msg,time);
}


AbstractAction getContextTimeAction(String msg,BicexEvaluationContext ctx,long time)
{
   return eval_viewer.getContextTimeAction(msg,ctx,time);
}

AbstractAction getSourceAction(BicexEvaluationContext ctx)
{
   return eval_viewer.getSourceAction(ctx);
}


/********************************************************************************/
/*										*/
/*	Abstract methods							*/
/*										*/
/********************************************************************************/

protected abstract JComponent setupPanel();

abstract void update();
void updateTime()				{ }

void removePanel()				{ }



void handlePopupMenu(JPopupMenu menu,MouseEvent evt)
{

}




/********************************************************************************/
/*                                                                              */
/*      Drawing helper methods                                                  */
/*                                                                              */
/********************************************************************************/

protected Color getColorForMethod(String method)
{
   if (method == null) return BoardColors.getColor("Bicex.StackDefault");
   
   if (color_map == null) color_map = new HashMap<>();
   
   synchronized (color_map) {
      Color c = color_map.get(method);
      if (c == null) {
	 double v;
	 int ct = color_map.size();
	 if (ct == 0) v = 0;
	 else if (ct == 1) v = 1;
	 else {
	    v = 0.5;
	    int p0 = ct-1;
	    int p1 = 1;
	    for (int p = p0; p > 1; p /= 2) {
	       v /= 2.0;
	       p0 -= p1;
	       p1 *= 2;
	     }
	    if ((p0 & 1) == 0) p0 = 2*p1 - p0 + 1;
	    v = v * p0;
	  }
	 float h = (float)(v * 0.8);
	 float s = 0.7f;
	 float b = 1.0f;
	 int rgb = Color.HSBtoRGB(h,s,b);
	 rgb |= 0xc0000000;
	 c = new Color(rgb,true);
	 color_map.put(method,c);
       }
      return c;
    }
}





}	// end of class BicexBubble




/* end of BicexBubble.java */

