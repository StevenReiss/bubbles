/********************************************************************************/
/*										*/
/*		BconOverviewPanel.java						*/
/*										*/
/*	Bubbles Environment Context Viewer overview context panel		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;



class BconOverviewPanel implements BconConstants, BudaConstants.BubbleViewCallback,
	BconConstants.BconPanel, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<File,BconOverviewClass> class_views;

private BudaBubbleArea		bubble_area;
private DrawingPanel		drawing_area;
private ScaleType		scale_type;
private Point			base_point;
private BconOverviewClass	focus_class;
private Rectangle		focus_region;
private boolean 		is_vertical;
private Set<BudaBubble> 	active_bubbles;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconOverviewPanel(BudaBubbleArea bba,Point base)
{
   bubble_area = bba;
   class_views = new TreeMap<File,BconOverviewClass>();
   base_point = new Point(base);
   focus_class = null;
   is_vertical = false;
   focus_region = null;

   BudaRoot.addBubbleViewCallback(this);

   drawing_area = new DrawingPanel();
   scale_type = ScaleType.SCALE_LINEAR;

   //TODO: set size by the number of files
   Dimension d;
   if (is_vertical) d = new Dimension(200,400);
   else d = new Dimension(400,200);
   drawing_area.setPreferredSize(d);
   active_bubbles = new HashSet<BudaBubble>();
}



@Override public void dispose()
{
   BudaRoot.removeBubbleViewCallback(this) ;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public JComponent getComponent()		{ return drawing_area; }


Collection<BudaBubble> getActiveBubbles()		{ return active_bubbles; }




/********************************************************************************/
/*										*/
/*	Menu methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   BudaBubble bbl = BudaRoot.findBudaBubble(drawing_area);
   if (bbl == null) return;
   JPopupMenu menu = new JPopupMenu();
   menu.add(bbl.getFloatBubbleAction());
   menu.show(bbl, e.getX(), e.getY());
}




/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private void addAllBubbles()
{
   for (BudaBubble bb : bubble_area.getBubblesInRegion(focus_region)) {
      if (!active_bubbles.contains(bb)) {
	 active_bubbles.add(bb);
	 checkAddFile(bb);
	 Collection<BconRegion> rgns = findBubbleRegions(bb);
	 if (rgns != null) {
	    for (BconRegion br : rgns) br.setHasBubble(true);
	 }
       }
    }
}



private void computeRegion()
{
   if (bubble_area == null) return;

   Rectangle newrgn = bubble_area.computeRegion(drawing_area);
   if (newrgn == null || newrgn.equals(focus_region)) return;

   focus_region = new Rectangle(newrgn);
   addAllBubbles();
}




/********************************************************************************/
/*										*/
/*	Callback methods for bubble updates					*/
/*										*/
/********************************************************************************/

@Override public void focusChanged(BudaBubble bb,boolean set)
{
   focus_class = null;

   if (bb == null) return;
   if (BudaRoot.findBudaBubbleArea(bb) != bubble_area) return;

   Collection<BconRegion> rgns = findBubbleRegions(bb);
   if (rgns != null && rgns.size() > 0) {
      for (BconRegion br : rgns) {
	 br.setHasFocus(set);
	 if (set) {
	    focus_class = class_views.get(bb.getContentFile());
	 }
      }
      drawing_area.repaint();
   }
}



@Override public void bubbleAdded(BudaBubble bb)
{
   if (BudaRoot.findBudaBubbleArea(bb) != bubble_area) return;

   computeRegion();

   checkAddFile(bb);

   Collection<BconRegion> rgns = findBubbleRegions(bb);
   if (rgns != null && rgns.size() > 0) {
      for (BconRegion br : rgns) {
	 br.setHasBubble(true);
      }
      drawing_area.repaint();
   }
}



@Override public void bubbleRemoved(BudaBubble bb)
{
   computeRegion();

   Collection<BconRegion> rgns = findBubbleRegions(bb);
   if (rgns != null && rgns.size() > 0) {
      for (BconRegion br : rgns) br.setHasBubble(false);
      drawing_area.repaint();
    }

   // TODO: should remove the file from the set of displayed
   // files if there are no more bubbles from it
   // this involves removing it from class_files
}



/********************************************************************************/
/*										*/
/*	Methods to find region for a bubble					*/
/*										*/
/********************************************************************************/

private Collection<BconRegion> findBubbleRegions(BudaBubble bb)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   if (bba != null && bba != bubble_area) return null;
   if (bb == null) return null;
   File f = bb.getContentFile();
   if (f == null) return null;
   BconOverviewClass bcv = class_views.get(f);
   if (bcv == null) return null;

   String nm = bb.getContentName();
   if (nm == null) return null;

   Collection<BconRegion> brs = bcv.findRegions(bb.getContentType(),nm);

   return brs;
}




private boolean checkAddFile(BudaBubble bb)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);

   if (bba != bubble_area) return false;
   File f = bb.getContentFile();
   if (f == null) return false;
   BconOverviewClass bcv = class_views.get(f);
   if (bcv != null) return false;
   if (bb.getContentName() == null) return false;
   Rectangle bloc = BudaRoot.findBudaLocation(bb);
   if (bloc == null) return false;
   if (focus_region != null && !focus_region.intersects(bloc)) return false;

   bcv = new BconOverviewClass(this,bb.getContentProject(),f,getClassName(bb),is_vertical);
   class_views.put(f,bcv);

   drawing_area.repaint();

   return true;
}




/********************************************************************************/
/*										*/
/*	Auxilliary methods for bubble deciphering				*/
/*										*/
/********************************************************************************/

private String getClassName(BudaBubble bb)
{
   String nm = bb.getContentName();
   int idx;

   if (nm == null) return null;

   switch (bb.getContentType()) {
      case NONE :
	 break;
      case CLASS :
	 break;
      case FIELD :
      case CLASS_ITEM :
	 idx = nm.lastIndexOf(".");
	 if (idx >= 0) nm = nm.substring(0,idx);
	 break;
      case METHOD :
	 idx = nm.indexOf("(");
	 if (idx >= 0) nm = nm.substring(0,idx);
	 idx = nm.lastIndexOf(".");
	 if (idx >= 0) nm = nm.substring(0,idx);
	 break;
      case OVERVIEW :
      case NOTE : 
      default:
	 break;
    }

   idx = nm.indexOf("$");
   if (idx >= 0) nm = nm.substring(0,idx);

   String file = bb.getContentFile().getName();
   idx = file.lastIndexOf(".");
   if (idx >= 0) file = file.substring(0,idx);
   String nm0 = nm;
   for ( ; ; ) {
      int i0 = nm0.lastIndexOf(".");
      if (i0 < 0) break;
      if (nm0.substring(i0+1).equals(file)) {
	 nm = nm0;
	 break;
       }
      else nm0 = nm0.substring(0,i0);
    }

   return nm;
}




/********************************************************************************/
/*										*/
/*	Drawing helper methods							*/
/*										*/
/********************************************************************************/

private double getRelativeHeight(double lines,double maxlines)
{
   double ht0 = 1.0;

   switch (scale_type) {
      case SCALE_EXPAND :
	 break;
      case SCALE_LINEAR :
	 ht0 = lines / maxlines;
	 break;
      case SCALE_SQRT :
	 ht0 = Math.sqrt(lines)/Math.sqrt(maxlines);
	 break;
      case SCALE_LOG :
	 ht0 = Math.log(lines+1)/Math.log(maxlines+1);
	 break;
    }

   return ht0;
}




/********************************************************************************/
/*										*/
/*	Panel for drawing							*/
/*										*/
/********************************************************************************/

private class DrawingPanel extends JPanel implements BudaConstants.BudaBubbleOutputer {

   private static final long serialVersionUID = 1;

   DrawingPanel() {
      Font ft =  getFont();
      ft = ft.deriveFont(Font.BOLD);
      setFont(ft);
      setToolTipText("Context panel");
      BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
      addMouseListener(new Mouser(this));
      BoardColors.setColors(this,"Bcon.file.overview.background");
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension d = getSize();
      g2.setBackground(getBackground());
      System.err.println("BACKGROUND " + getBackground());
      g2.clearRect(0,0,d.width,d.height);
   
      int ct = class_views.size();
      if (ct == 0) return;
   
      // compute size of each file region
      double wd,ht;
      Rectangle2D.Double trex;
      if (is_vertical) {
         wd = (d.width - 2 * LR_MARGIN_SPACE - (ct-1) * SEPARATION_SPACE -
        	  ct * NAME_SPACE)/ct;
         ht = d.height - 2 * TB_MARGIN_SPACE;
         trex = new Rectangle2D.Double(0,TB_MARGIN_SPACE,NAME_SPACE,ht);
       }
      else {
         wd = d.width - 2 * LR_MARGIN_SPACE;
         ht = (d.height - 2 * TB_MARGIN_SPACE - (ct-1) * SEPARATION_SPACE -
        	  ct * NAME_SPACE)/ct;
         trex = new Rectangle2D.Double(LR_MARGIN_SPACE,0,wd,NAME_SPACE);
       }
   
      int mxln = 1;
      for (BconOverviewClass cv : class_views.values()) {
         mxln = Math.max(mxln,cv.getLineCount());
       }
   
      int idx = 0;
      for (BconOverviewClass cv : class_views.values()) {
         String cnm = cv.getClassName();
         if (cv == focus_class) g2.setColor(BoardColors.getColor(LABEL_FOCUS_COLOR_PROP));
         else g2.setColor(BoardColors.getColor(LABEL_COLOR_PROP));
         int lct = cv.getLineCount();
         double sz0 = getRelativeHeight(lct,mxln);
   
         if (is_vertical) {
            double x0 = LR_MARGIN_SPACE + idx * (SEPARATION_SPACE + NAME_SPACE + wd);
            double x1 = x0 + NAME_SPACE;
            double y0 = TB_MARGIN_SPACE;
            trex.x = x0;
            SwingText.drawVerticalText(cnm,g2,trex);
            cv.paint(g2,x1,y0,wd,ht*sz0);
          }
         else {
            double x0 = LR_MARGIN_SPACE;
            double y0 = TB_MARGIN_SPACE + idx * (SEPARATION_SPACE + NAME_SPACE + ht);
            double y1 = y0 + NAME_SPACE;
            trex.y = y0;
            SwingText.drawText(cnm,g2,trex);
            cv.paint(g2,x0,y1,wd*sz0,ht);
          }
   
         ++idx;
       }
    }

   @Override public String getConfigurator()		{ return "BCON"; }
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","OVERVIEW");
      xw.element("POINT",base_point);
    }

   @Override public String getToolTipText(MouseEvent e) {
      int ct = class_views.size();
      if (ct == 0) return null;

      Dimension d = getSize();
      double wd,ht;
      if (is_vertical) {
	 wd = (d.width - 2 * LR_MARGIN_SPACE - (ct-1) * SEPARATION_SPACE -
		  ct * NAME_SPACE)/ct;
	 ht = d.height - 2 * TB_MARGIN_SPACE;
       }
      else {
	 wd = d.width - 2 * LR_MARGIN_SPACE;
	 ht = (d.height - 2 * TB_MARGIN_SPACE - (ct-1) * SEPARATION_SPACE -
		  ct * NAME_SPACE)/ct;
       }

      int mxln = 1;
      for (BconOverviewClass cv : class_views.values()) {
	 mxln = Math.max(mxln,cv.getLineCount());
       }

      int idx = 0;
      double ht0 = ht;
      double wd0 = wd;
      for (BconOverviewClass cv : class_views.values()) {
	 int lct = cv.getLineCount();
	 double sz0 = getRelativeHeight(lct,mxln);
	 double ln0,x0,y0,x1,y1;

	 if (is_vertical) {
	    x0 = LR_MARGIN_SPACE + idx * (SEPARATION_SPACE + NAME_SPACE + wd);
	    x1 = x0 + NAME_SPACE;
	    y1 = TB_MARGIN_SPACE;
	    ht0 = ht * sz0;
	    ln0 = (e.getY() - y1)*lct/ht0;
	  }
	 else {
	    x1 = LR_MARGIN_SPACE;
	    y0 = TB_MARGIN_SPACE + idx * (SEPARATION_SPACE + NAME_SPACE + ht);
	    y1 = y0 + NAME_SPACE;
	    wd0 = wd * sz0;
	    ln0 = (e.getX() - x1)*lct/wd0;
	  }

	 if (e.getX() >= x1 && e.getX() <= x1+wd0 && e.getY() >= y1 && e.getY() <= y1+ht0) {
	    double z0 = (is_vertical ? ht0 : wd0);
	    int ln1 = (int) Math.floor(ln0 - 2*lct/z0);
	    int ln2 = (int) Math.ceil(ln0 + 2*lct/z0);
	    return cv.computeToolTip(ln1,ln2);
	  }

	 ++idx;
       }
      return null;
    }

   void handleMouseEvent(MouseEvent e) {
      int ct = class_views.size();
      if (ct == 0) return;

      Dimension d = getSize();
      double wd,ht;
      if (is_vertical) {
	 wd = (d.width - 2 * LR_MARGIN_SPACE - (ct-1) * SEPARATION_SPACE -
		  ct * NAME_SPACE)/ct;
	 ht = d.height - 2 * TB_MARGIN_SPACE;
       }
      else {
	 wd = d.width - 2 * LR_MARGIN_SPACE;
	 ht = (d.height - 2 * TB_MARGIN_SPACE - (ct-1) * SEPARATION_SPACE -
		  ct * NAME_SPACE)/ct;
       }

      int mxln = 1;
      for (BconOverviewClass cv : class_views.values()) {
	 mxln = Math.max(mxln,cv.getLineCount());
       }

      int idx = 0;
      double ht0 = ht;
      double wd0 = wd;
      for (BconOverviewClass cv : class_views.values()) {
	 int lct = cv.getLineCount();
	 double sz0 = getRelativeHeight(lct,mxln);
	 double ln0,x0,y0,x1,y1;

	 if (is_vertical) {
	    x0 = LR_MARGIN_SPACE + idx * (SEPARATION_SPACE + NAME_SPACE + wd);
	    x1 = x0 + NAME_SPACE;
	    y1 = TB_MARGIN_SPACE;
	    ht0 = ht * sz0;
	    ln0 = (e.getY() - y1)*lct/ht0;
	  }
	 else {
	    x1 = LR_MARGIN_SPACE;
	    y0 = TB_MARGIN_SPACE + idx * (SEPARATION_SPACE + NAME_SPACE + ht);
	    y1 = y0 + NAME_SPACE;
	    wd0 = wd * sz0;
	    ln0 = (e.getX() - x1)*lct/wd0;
	  }

	 if (e.getX() >= x1 && e.getX() <= x1+wd0 && e.getY() >= y1 && e.getY() <= y1+ht0) {
	    cv.handleMousePress(e,(int)(ln0 + 0.5));
	    return;
	  }

	 ++idx;
       }
    }



}	// end of inner class DrawingPanel



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

private static class Mouser extends MouseAdapter {

   private DrawingPanel for_panel;

   Mouser(DrawingPanel pnl) {
      for_panel = pnl;
    }

   @Override public void mouseClicked(MouseEvent e) {
      for_panel.handleMouseEvent(e);
    }

}	// end of inner class Mouser





}	// end of class BconOverviewPanel




/* end of BconOverviewPanel.java */
