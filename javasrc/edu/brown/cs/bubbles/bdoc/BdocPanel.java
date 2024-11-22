/********************************************************************************/
/*										*/
/*		BdocPanel.java							*/
/*										*/
/*	Bubbles Environment Documentation display panel 			*/
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


package edu.brown.cs.bubbles.bdoc;


import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.ivy.swing.SwingEditorPane;
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Scrollable;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.View;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;



class BdocPanel implements BdocConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JScrollPane	doc_region;
private DocPanel	the_panel;
private BdocReference	ref_item;
private BdocDocItem	doc_item;
private DescriptionView desc_view;
private JTree		item_tree;
private BdocCellRenderer cell_renderer;

private static int	scroll_width = 0;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocPanel(BdocReference r) throws BdocException
{
   ref_item = r;

   cell_renderer = new BdocCellRenderer(this);

   try {
      switch (r.getNameType()) {
	 case PACKAGE :
	    doc_item = new BdocDocPackage(r.getReferenceUrl());
	    break;
	 case METHOD :
	 case CONSTRUCTOR :
	    doc_item = new BdocDocMethod(r.getReferenceUrl());
	    break;
	 case FIELDS :
         case VARIABLES :
	    doc_item = new BdocDocField(r.getReferenceUrl());
	    break;
	 case CLASS :
	 case ENUM :
	 case INTERFACE :
	 case THROWABLE :
         case ANNOTATION :
	    doc_item = new BdocDocClass(r.getReferenceUrl());
	    break;
	 default :
	    throw new BdocException("No java doc available for " + r.getNameType() + " " + r);
       }
    }
   catch (IOException e) {
      throw new BdocException("Problem getting javadoc",e);
    }

   setupPanel();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JComponent getPanel()
{
   return doc_region;
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   the_panel = new DocPanel();

   JLabel ttl = new JLabel(ref_item.getDigestedName());
   //JLabel ttl = new JLabel(ref_item.getNameHead());
   ttl.setFont(CONTEXT_FONT);
   ttl.setForeground(BoardColors.getColor(CONTEXT_COLOR_PROP));
   ttl.setBackground(BoardColors.getColor(BDOC_TOP_COLOR_PROP));
   ttl.addMouseListener(new TitleMouser());
   ttl.setOpaque(true);
   the_panel.setTitleComponent(ttl);

   String desc = "<html>" + doc_item.getDescription();

   desc_view = new DescriptionView(desc);
   the_panel.setDescriptionComponent(desc_view);

   DefaultMutableTreeNode root = new DefaultMutableTreeNode();
   for (ItemRelation ir : ItemRelation.values()) {
      List<SubItem> itms = doc_item.getItems(ir);
      if (itms != null) {// get nicer name
	 DefaultMutableTreeNode relnode = new DefaultMutableTreeNode(ir,true);
	 root.add(relnode);
	 for (SubItem itm : itms) {
	    // really want a tree node that displays the item
	    MutableTreeNode itmnode = new DefaultMutableTreeNode(itm,false);
	    relnode.add(itmnode);
	 }
      }
    }

   item_tree = new ItemTree(root);
   item_tree.setRootVisible(false);
   item_tree.setCellRenderer(cell_renderer);
   item_tree.addMouseListener(new ItemMouser());
   item_tree.addTreeExpansionListener(new TreeListener());
   item_tree.setFocusable(true);

   the_panel.setItemsComponent(item_tree);

   the_panel.addComponentListener(new PanelWidthManager());

   BudaCursorManager.setCursor(the_panel,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

   the_panel.setBackground(BoardColors.getColor(BDOC_TOP_COLOR_PROP));
   the_panel.setFocusable(true);

   doc_region = new JScrollPane(the_panel);
}





private void createNewBubble(BdocReference br)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(the_panel);
   BudaBubble bb = BudaRoot.findBudaBubble(the_panel);
   if (bba == null) return;

   try {
      // This can take a while and should be done outside the UI thread, the rest should be done in UI thread
      BdocBubble nbb = new BdocBubble(br);
      bba.addBubble(nbb,the_panel,null,
	    PLACEMENT_PREFER|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
      if (bb != null) {
	 BudaBubbleLink lnk = new BudaBubbleLink(
	    bb,
	    new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true),
	    nbb,
	    new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true));
	 bba.addLink(lnk);
       }
    }
   catch (BdocException e) {
      BoardLog.logE("BDOC","Problem creating new doc bubble",e);
    }
}



void createLinkBubble(String lbl,URI u)
{
   if (lbl != null) {
      BdocReference br = ref_item.findRelatedReference(lbl);
      if (br != null) {
	 createNewBubble(br);
	 return;
       }
    }

   if (u == null && lbl != null) {
      u = ref_item.getReferenceUrl().resolve(lbl);
    }

   if (u == null) {
      BoardLog.logE("BDOC","Can't create URL for " + lbl);
    }
   else {
      BdocReference br = ref_item.findRelatedReference(u);
      if (br != null) {
	 createNewBubble(br);
	 return;
       }

      // create html bubble here
      BoardLog.logI("BDOC","Hyperlink to " + u);
    }
}



/********************************************************************************/
/*										*/
/*	Handle computing editor size						*/
/*										*/
/********************************************************************************/

static Dimension computeEditorSize(JEditorPane ep,int w0)
{
   int delta = getScrollWidth();

   JLabel lbl = new JLabel();
   lbl.setFont(ep.getFont());
   lbl.setText(ep.getText());
   View v = (View) lbl.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
   if (w0 <= 0) w0 = DESCRIPTION_WIDTH;
   v.setSize(w0-delta,0);
   float w = v.getPreferredSpan(View.X_AXIS);
   float h = v.getPreferredSpan(View.Y_AXIS);
   w += delta;

   Dimension d = new Dimension((int) Math.ceil(w),(int) Math.ceil(h));

   return d;
}



static synchronized int getScrollWidth()
{
   if (scroll_width == 0) {
      JScrollBar sb = new JScrollBar(Adjustable.VERTICAL);
      Dimension dsb = sb.getPreferredSize();
      scroll_width = dsb.width + 4;
    }
   return scroll_width;
}


/********************************************************************************/
/*										*/
/*	Handle formatting text that is read in					*/
/*										*/
/********************************************************************************/

private String noPre(String d)
{
   return d;
}




/********************************************************************************/
/*										*/
/*	Panel implementation							*/
/*										*/
/********************************************************************************/

private class DocPanel extends SwingGridPanel implements Scrollable, BudaConstants.BudaBubbleOutputer {

   private static final long serialVersionUID = 1;

   DocPanel() {
      setOpaque(false);
    }

   // scrollable interface
   @Override public Dimension getPreferredScrollableViewportSize()		{ return getPreferredSize(); }
   @Override public int getScrollableBlockIncrement(Rectangle r,int o,int dir)	{ return 12; }
   @Override public boolean getScrollableTracksViewportHeight() 		{ return false; }
   @Override public boolean getScrollableTracksViewportWidth()			{ return true; }
   @Override public int getScrollableUnitIncrement(Rectangle r,int o,int d)	{ return 1; }

   @Override public String getConfigurator()					{ return "BDOC"; }
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","JAVADOC");
      xw.field("NAME",ref_item.getKey());
    }

   void setTitleComponent(JComponent ttl) {
      Dimension d = ttl.getPreferredSize();
      ttl.setMinimumSize(d);
      ttl.setMaximumSize(d);
      addGBComponent(ttl,0,0,0,1,0,0);
      //addGBComponent(new JSeparator(),0,1,0,1,1,0);
    }

   void setDescriptionComponent(JComponent desc) {
      addGBComponent(desc,0,2,0,1,1,0);
      //addGBComponent(new JSeparator(),0,3,0,1,1,0);
    }

   void setItemsComponent(JComponent itms) {
      addGBComponent(itms,0,4,0,1,1,0);
    }

   @Override protected void paintComponent(Graphics g0) {
      Graphics2D g2 = (Graphics2D) g0.create();
      Color tc = BoardColors.getColor(BDOC_TOP_COLOR_PROP);
      Color bc = BoardColors.getColor(BDOC_BOTTOM_COLOR_PROP);

      if (tc.getRGB() != bc.getRGB()) {
	 Dimension sz = getSize();
	 Paint p = new GradientPaint(0f,0f,tc,0f,sz.height,bc);
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 g2.setColor(BoardColors.getColor("Bdoc.BackgroundColor"));     // solid fill first
	 g2.fill(r);
	 g2.setPaint(p);
	 g2.fill(r);
       }

      super.paintComponent(g0);
    }

    @Override public void scrollRectToVisible(Rectangle r) {}

}	// end of inner class DocPanel



private final class PanelWidthManager extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      Dimension d = the_panel.getSize();
      cell_renderer.setTreeWidth(d.width-4);
      item_tree.invalidate();
      BasicTreeUI tui = (BasicTreeUI) item_tree.getUI();
      tui.setLeftChildIndent(tui.getLeftChildIndent());
      Dimension d1 = the_panel.getPreferredSize();
      Dimension d2 = the_panel.getSize();
      if (d1.height > d2.height) {
	 d2.height = d1.height + 4;
	 the_panel.setSize(d2);
       }
    }

}	// end of inner class TreeWidthManager



/********************************************************************************/
/*										*/
/*	Class to hold the description						*/
/*										*/
/********************************************************************************/

private class DescriptionView extends SwingEditorPane {

   private static final long serialVersionUID = 1;

   DescriptionView(String d) {
      super("text/html",noPre(d));
      setFont(NAME_FONT);
      setForeground(BoardColors.getColor(NAME_COLOR_PROP));
      setEditable(false);
      setOpaque(false);
      BudaCursorManager.setCursor(this,new Cursor(Cursor.TEXT_CURSOR));
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      addHyperlinkListener(new DocLinker());
    }

   @Override public boolean getScrollableTracksViewportWidth() { return true; }

   @Override public Dimension getPreferredSize() {
      return computeEditorSize(this,getWidth());
   }

}	// end of inner class DescriptionView




private final class DocLinker implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
         try {
            URL url = e.getURL();
            if (url == null) return;
            URI u = url.toURI();
            String lbl = e.getDescription();
            createLinkBubble(lbl,u);
         }
         catch (URISyntaxException ex) { }
       }
      else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
         BudaCursorManager.setTemporaryCursor(desc_view, new Cursor(Cursor.HAND_CURSOR));
      }
      else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
         BudaCursorManager.resetDefaults(desc_view);
      }
    }

}	// end of inner class DocLinker




/********************************************************************************/
/*										*/
/*	Class for tree								*/
/*										*/
/********************************************************************************/

private static class ItemTree extends JTree {

   private static final long serialVersionUID = 1;

   ItemTree(TreeNode root) {
      super(root);
      setOpaque(false);
      setToggleClickCount(1);
    }

}	// end of inner class ItemTree



/********************************************************************************/
/*										*/
/*	Tree expansion handling 						*/
/*										*/
/********************************************************************************/

private void checkExpandPanel()
{
   JScrollPane jsp = (JScrollPane) the_panel.getParent().getParent();
   Dimension osz = jsp.getSize();
   Dimension d = the_panel.getPreferredSize();
   if (d.height >= MAX_EXPAND_HEIGHT) d.height = MAX_EXPAND_HEIGHT;
   if (osz.height < d.height) {
      osz.height = d.height;
      jsp.setSize(osz);
    }
}




private final class TreeListener implements TreeExpansionListener {

   @Override public void treeCollapsed(TreeExpansionEvent e)	{ }

   @Override public void treeExpanded(TreeExpansionEvent e) {
      checkExpandPanel();
    }

}	// end of inner class TreeListener



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

private final class TitleMouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      try {
	 URI ui = ref_item.getReferenceUrl();
	 if (ui == null) return;
	 URI uin = new URI(ui.getScheme(),ui.getUserInfo(),ui.getHost(),ui.getPort(),ui.getPath(),null,null);
	 createLinkBubble(null,uin);
       }
      catch (Exception ex) {
	 BoardLog.logE("BDOC","Problem handling title click",ex);
       }
    }

}	// end of inner class TitleMouser


private final class ItemMouser extends MouseAdapter {

   @Override public void mousePressed(MouseEvent e) {
      JTree tree = (JTree) e.getSource();
      int selrow = tree.getRowForLocation(e.getX(),e.getY());
      if (selrow != -1 && e.getClickCount() == 1) {
	 TreePath selpath = tree.getPathForRow(selrow);
	 DefaultMutableTreeNode tn = (DefaultMutableTreeNode) selpath.getLastPathComponent();
	 if (tn.isLeaf()) {
	    SubItem sitm = (SubItem) tn.getUserObject();
	    createLinkBubble(sitm.getRelativeUrl(),sitm.getItemUrl());
	  }
       }
    }

}	// end of inner class ItemMouser





}	// end of class BdocPanel


/* end of BdocPanel.java */



