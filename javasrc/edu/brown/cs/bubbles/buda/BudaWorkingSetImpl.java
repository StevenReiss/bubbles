/********************************************************************************/
/*										*/
/*		BudaWorkingSetImpl.java 					*/
/*										*/
/*	BUblles Display Area working set implementation 			*/
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
import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMail;
import edu.brown.cs.bubbles.board.BoardMailMessage;
import edu.brown.cs.bubbles.board.BoardUpload;

import javax.swing.JOptionPane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



class BudaWorkingSetImpl implements BudaConstants, BudaConstants.BudaWorkingSet, BoardConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubbleArea bubble_area;
private String	set_label;
private Rectangle set_region;
private int	preferred_y;
private Color	border_color;
private Color	top_color;
private Color	bottom_color;
private Color	text_color;
private boolean being_changed;
private long	create_time;
private boolean is_shared;
private WorkingSetGrowth grow_type;
private GrowCallback grow_callback;
private Map<String,Object> property_map;

private static int MIN_GROW_SPACE = 100;
private static int GROW_EXTRA = 20;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaWorkingSetImpl(BudaBubbleArea bba,String lbl,Rectangle rgn,int y,WorkingSetGrowth g)
{
   bubble_area = bba;
   set_label = lbl;
   set_region = new Rectangle(rgn);
   preferred_y = y;
   being_changed = false;
   create_time = System.currentTimeMillis();
   is_shared = false;
   property_map = new HashMap<>();
  
   if (g == null)
      g = BUDA_PROPERTIES.getEnum("Buda.workingset.growth",null,WorkingSetGrowth.NONE);
   grow_type = WorkingSetGrowth.NONE;
   grow_callback = null;
   setGrowth(g);

   setColor(BoardColors.randomColor(1.0));
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getLabel()			{ return set_label; }

@Override public void setLabel(String s)
{
   set_label = s;
}



@Override public Rectangle getRegion()
{
   Dimension r = bubble_area.getSize();
   set_region.height = r.height;

   return new Rectangle(set_region);
}


@Override public BudaBubbleArea getBubbleArea()
{
   return bubble_area;
}


void setRegion(Rectangle r)
{
   set_region = new Rectangle(r);
   BudaRoot br = BudaRoot.findBudaRoot(bubble_area);
   if (br != null) br.repaint();
}


void setGrowth(WorkingSetGrowth g)
{
   if (grow_type == g) return;
   grow_type = g;
   if (g == WorkingSetGrowth.NONE) {
      if (grow_callback != null) {
         BudaRoot.removeBubbleViewCallback(grow_callback);
         grow_callback = null;
       }
    }
   else {
      if (grow_callback == null) {
         grow_callback = new GrowCallback();
         BudaRoot.addBubbleViewCallback(grow_callback);
       }
    }
}


Color getTopColor()			{ return top_color; }
Color getBottomColor()			{ return bottom_color; }
Color getBorderColor()
{
   if (being_changed) return BoardColors.getColor("Buda.WorkingsetChangedColor");
   return border_color;
}


Color getTextColor()			{ return text_color; }


void setColor(Color c)
{
   if (c == null) c = BoardColors.getColor("Buda.WorkingsetDefaultColor");

   border_color = c;
   top_color = BoardColors.getPaleColor(c,0.5);
   bottom_color = BoardColors.getPaleColor(top_color);
   text_color = BoardColors.getTextColor(c);
}



void setBeingChanged(boolean fg)	{ being_changed = fg; }
boolean isBeingChanged()		{ return being_changed; }

void setCreateTime(long when)		{ create_time = when; }

boolean isShared()			{ return is_shared; }
void setShared(boolean fg)		{ is_shared = fg; }

@Override public boolean isSticky()
{
   return grow_type == WorkingSetGrowth.GROW;
}

@Override public Object getProperty(String p)
{
   return property_map.get(p);
}


@Override public void setProperty(String prop,Object v)
{
   if (v == null) property_map.remove(prop);
   else property_map.put(prop,v);
}



/********************************************************************************/
/*										*/
/*	Bubble methods								*/
/*										*/
/********************************************************************************/

void removeBubbles()
{
   for (BudaBubble bb : bubble_area.getBubblesInRegion(set_region)) {
      if (!bb.isFloating()) bubble_area.userRemoveBubble(bb);
    }
}



/********************************************************************************/
/*										*/
/*	Save methods								*/
/*										*/
/********************************************************************************/

@Override public File getDescription() throws IOException
{
   File f = File.createTempFile("BudaWorkingSet",".xml");
   BudaXmlWriter xw = new BudaXmlWriter(f);
   createTask(xw);
   xw.close();

   return f;
}




void saveAs(File result) throws IOException
{
   BudaXmlWriter xw = new BudaXmlWriter(result);

   createTask(xw);

   xw.close();
}



BudaTask createTask()
{
   if (set_label == null) return null;

   BudaXmlWriter xw = new BudaXmlWriter();
   createTask(xw);

   return new BudaTask(set_label,xw.toString());
}



void sendMail(String to)
{
   try {
      File f = File.createTempFile("BudaWorkingSet",".xml");
      BudaXmlWriter xw = new BudaXmlWriter(f);
      createTask(xw);
      xw.close();
      BoardUpload bup = new BoardUpload(f);

      String msg = "Here is a working set to share:\n\n" + bup.getFileURL() + "\n";
      BoardMailMessage bmm = BoardMail.createMessage(to);
      if (bmm != null) {
	 bmm.setSubject("Bubbles working set to share");
	 bmm.addBodyText(msg);
	 bmm.send();
       }
      else {
	 JOptionPane.showMessageDialog(bubble_area,"Java Desktop Mail Interface not supported");
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUDA","Problem emailing working set",e);
    }
}




void sendPDF(String to)
{
   try {
      File f = File.createTempFile("BudaWorkingSet",".pdf");
      BudaRoot br = BudaRoot.findBudaRoot(bubble_area);
      br.exportAsPdf(f,getRegion());

      BoardUpload bup = new BoardUpload(f);

      String msg = "Here is the working set image:\n\n" + bup.getFileURL() + "\n";
      BoardMailMessage bmm = BoardMail.createMessage(to);
      if (bmm != null) {
	 bmm.setSubject("Bubbles working set image");
	 bmm.addBodyText(msg);
	 bmm.send();
       }
      else {
	 JOptionPane.showMessageDialog(bubble_area,"Java Desktop Mail Interface not supported");
       }
    }
   catch (Exception e) {
      BoardLog.logE("BUDA","Problem emailing working set image",e);
    }
}




void createTask(BudaXmlWriter xw)
{
   xw.begin("TASK");
   xw.field("TIME",System.currentTimeMillis());
   if (set_label != null) xw.field("NAME",set_label);
   else xw.field("NAME","Unnamed Task");

   outputXml(xw);

   BudaBubbleScaler bsc = bubble_area.getUnscaler();
   Set<BudaBubble> bbls = new HashSet<BudaBubble>(bubble_area.getBubblesInRegion(set_region));
   Set<BudaBubbleGroup> grps = new HashSet<BudaBubbleGroup>();

   xw.begin("BUBBLES");
   for (BudaBubble bb : bbls) {
      if (bb.isFloating()) continue;
      bb.outputBubbleXml(xw,bsc);
      BudaBubbleGroup bbg = bb.getGroup();
      if (bbg != null && bbg.getTitle() != null) grps.add(bbg);
    }
   xw.end("BUBBLES");

   xw.begin("GROUPS");
   for (BudaBubbleGroup bg : grps) {
      bg.outputXml(xw);
    }
   xw.end("GROUPS");

   xw.begin("LINKS");
   for (BudaBubbleLink bl : bubble_area.getLinks(bbls)) {
      bl.outputXml(xw);
    }
   xw.end("LINKS");

   xw.end("TASK");
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("WORKINGSET");
   xw.field("TOPCOLOR",top_color);
   xw.field("BOTTOMCOLOR",bottom_color);
   xw.field("BORDERCOLOR",border_color);
   xw.field("YLOC",preferred_y);
   xw.field("CREATE",create_time);
   xw.field("GROWTH",grow_type);
   xw.element("REGION",set_region);
   if (set_label != null) xw.element("NAME",set_label);
   xw.end("WORKINGSET");
}



/********************************************************************************/
/*                                                                              */
/*      Handle automatic growth                                                 */
/*                                                                              */
/********************************************************************************/

private class GrowCallback implements BubbleViewCallback {
   
   @Override public void doneConfiguration()                            { }
   @Override public void bubbleRemoved(BudaBubble bb)                   { }
   @Override public void focusChanged(BudaBubble bb,boolean set)        { }
   @Override public void workingSetAdded(BudaWorkingSet ws)             { }
   @Override public void workingSetRemoved(BudaWorkingSet ws)           { }
   @Override public void copyFromTo(BudaBubble from,BudaBubble to)      { }
   
   @Override public void bubbleAdded(BudaBubble bb) {
      checkBubble(bb);
    }
   
   @Override public boolean bubbleActionDone(BudaBubble bb) {
      checkBubble(bb);
      return false;
    }
   
   private void checkBubble(BudaBubble bb) {
      Rectangle r = BudaRoot.findBudaLocation(bb);
      if (set_region.contains(r)) return;
      if (!set_region.intersects(r)) {
         int delta = set_region.x - (r.x + r.width);
         if (delta < 0) delta = r.x - (set_region.x + set_region.width);
         if (delta > MIN_GROW_SPACE) return;
       }
      int leftdelta = 0;
      int rightdelta = 0;
      if (set_region.x > r.x) {
         leftdelta = set_region.x - r.x + GROW_EXTRA;
       }
      if (set_region.x + set_region.width < r.x + r.width) {
         rightdelta = r.x + r.width - set_region.x - set_region.width + GROW_EXTRA;
       }
      if (leftdelta == 0 && rightdelta == 0) return;
      
      switch (grow_type) {
         case NONE:
            return;
         case EXPAND :
            Rectangle nr = new Rectangle(set_region.x - leftdelta,set_region.y,
                  set_region.width + leftdelta + rightdelta,set_region.height);
            setRegion(nr);
            break;
         case GROW :
            bubble_area.expandArea(set_region.x,set_region.width,leftdelta,rightdelta);
            nr = new Rectangle(set_region.x,set_region.y,
                  set_region.width + leftdelta + rightdelta,set_region.height);
            setRegion(nr);
            break;
       }
    }
   
}       // end of inner class GrowCallback




}	// end of class BudaWorkingSetImpl




/* end of BudaWorkingSetImpl.java */
