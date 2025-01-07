/********************************************************************************/
/*										*/
/*		BassSearchBox.java						*/
/*										*/
/*	Bubble Augmented Search Strategies search area				*/
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



package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bass.BassTreeModel.BassTreeBase;
import edu.brown.cs.bubbles.bass.BassTreeModel.BassTreeNode;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleGroup;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaHover;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingKey;
import edu.brown.cs.ivy.swing.SwingTextField;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.Keymap;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class BassSearchBox extends SwingGridPanel implements BassConstants, CaretListener,
	TreeExpansionListener, ActionListener, BudaConstants.BudaBubbleOutputer,
	BudaConstants, BudaConstants.Scalable
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JTextField	input_field;
private JTree		active_options;
private transient BassTreeModel tree_model;
private boolean 	is_static;
private boolean 	is_common;
private transient Reseter cur_reseter;
private transient Set<String> local_expands;

private JScrollPane scroll_pane;
private BassSearchBox our_self;

private String		old_text;

private static String	search_text = "";
private static Set<String> expanded_nodes = new HashSet<String>();

private static Icon people_expand_image = null;
private static Icon people_collapse_image = null;
private static Icon docs_expand_image = null;
private static Icon docs_collapse_image = null;
private static Icon config_expand_image = null;
private static Icon config_collapse_image = null;
private static Icon process_expand_image = null;
private static Icon process_collapse_image = null;
private static Icon courses_expand_image = null;
private static Icon courses_collapse_image = null;


private static boolean bk_reset = true;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassSearchBox(BassTreeModel mdl,boolean common)
{
   tree_model = mdl;

   is_static = false;
   is_common = common;
   cur_reseter = null;
   local_expands = new HashSet<String>();

   setInsets(4);
   setOpaque(false);

   Font ft = BASS_PROPERTIES.getFontOption(BASS_TEXT_FONT_PROP,BASS_TEXT_FONT);
   Font ftb = ft.deriveFont(Font.BOLD);

   loadImages();

   input_field = new SwingTextField(36);
   input_field.setFont(ft);
   input_field.setOpaque(true);
   Color bptc = BoardColors.getColor(BASS_PANEL_TOP_COLOR_PROP);
   Color ifc = new Color(bptc.getRGB(),false);
   BoardColors.setColors(input_field,ifc);
   input_field.setBorder(null);

   Keymap kmp1 = input_field.getKeymap();
   Keymap kmp = JTextField.addKeymap("BASS*",kmp1);
   input_field.setKeymap(kmp);
   SwingKey.registerKeyAction("SEARCHBOX",input_field,new AbortAction(),"ESCAPE");
   SwingKey.registerKeyAction("SEARCHBOX",input_field,new ExpandAllAction(),"F4");
   SwingKey.registerKeyAction("SEARCHBOX",input_field,new CompactAction(),"shift F4");
   SwingKey.registerKeyAction("SEARCHBOX",input_field,new UpSelectionAction(),"UP");
   SwingKey.registerKeyAction("SEARCHBOX",input_field,new DownSelectionAction(),"DOWN");
   SwingKey.registerKeyAction("SEARCHBOX",this,new ExpandAllAction(),"F4");
   SwingKey.registerKeyAction("SEARCHBOX",this,new CompactAction(),"shift F4");
   SwingKey.registerKeyAction("SEARCHBOX",this,new TextSearchAction(),"shift ENTER");
   SwingKey.registerKeyAction("SEARCHBOX",input_field,new ClearInputAction(),"meta X");

   old_text = null;
   addGBComponent(input_field,0,0,1,1,1,0);

   active_options = new GradientTree(mdl);
   TreeSelectionModel smdl = active_options.getSelectionModel();
   smdl.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
   active_options.setRootVisible(false);
   active_options.setScrollsOnExpand(true);
   active_options.setShowsRootHandles(false);
   active_options.setVisibleRowCount(15);
   active_options.setFont(ftb);
   active_options.setDragEnabled(true);
   active_options.addMouseListener(new Mouser());
   active_options.setTransferHandler(new Transferer());
   BudaCursorManager.setCursor(active_options,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   active_options.setCellRenderer(new BassCellRenderer());
   active_options.setToggleClickCount(1);
   active_options.addTreeExpansionListener(this);

   SearchBoxCellRenderer renderer = new SearchBoxCellRenderer();
   try {
      active_options.setCellRenderer(renderer);
    }
   catch (Exception e) {
      // can fail due to timing issues
      active_options.setCellRenderer(renderer);
    }

   active_options.setBorder(null);

   if (is_common && search_text != null) {
      setDefaultText(search_text);
      input_field.selectAll();
      useExpandedNodes();
    }

   BasicTreeUI tui = (BasicTreeUI) active_options.getUI();

   tui.setCollapsedIcon(null);
   tui.setExpandedIcon(null);
   tui.setLeftChildIndent(INDENT_AMOUNT);
   tui.setRightChildIndent(INDENT_AMOUNT);

   scroll_pane = new JScrollPane(active_options);
   scroll_pane.setWheelScrollingEnabled(true);
   scroll_pane.setBorder(null);
   addGBComponent(scroll_pane,0,1,1,1,1,1);

   input_field.addCaretListener(this);
   input_field.addActionListener(this);

   addMouseListener(new BudaConstants.FocusOnEntry(input_field));

   new Hoverer();

   our_self = this;

   int mxl = BASS_PROPERTIES.getInt(MAX_LEAF_FOR_EXPANDALL_PROP,MAX_LEAF_FOR_EXPANDALL);
   if (tree_model.getLeafCount() <= mxl) expandAll();

   tree_model.addTreeModelListener(new UpdateHandler());
}



private static synchronized void loadImages()
{
   if (people_expand_image != null) return;

   people_expand_image = BoardImage.getIcon("add_user");
   people_collapse_image = BoardImage.getIcon("sub_user");
   docs_expand_image = BoardImage.getIcon("docexpand");
   docs_collapse_image = BoardImage.getIcon("doccollapse");
   config_expand_image = BoardImage.getIcon("docexpand");
   config_collapse_image = BoardImage.getIcon("doccollapse");
   process_expand_image = BoardImage.getIcon("docexpand");
   process_collapse_image = BoardImage.getIcon("doccollapse");
   courses_expand_image = BoardImage.getIcon("courses_expand");
   courses_collapse_image = BoardImage.getIcon("courses_collapse");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JTextField getEditor()			{ return input_field; }

void setStatic(boolean fg)		{ is_static = fg; }

static void setDefault(String proj,String pfx)
{
   if (proj == null && pfx == null) return;

   String txt = null;
   if (proj != null) txt = "E:" + proj + " N:";

   if (pfx != null) {
      if (txt == null) txt = pfx;
      else txt += pfx;
    }

   search_text = txt;
}




/********************************************************************************/
/*										*/
/*	Callbacks for text input						*/
/*										*/
/********************************************************************************/

@Override public void caretUpdate(CaretEvent e)
{
   String txt = input_field.getText();
   if (txt.equals(old_text)) return;

   BudaRoot br = BudaRoot.findBudaRoot(this);
   if (br == null) return;
   br.noteSearchUsed(this);

   if ((old_text == null || txt.startsWith(old_text)) && !txt.endsWith(":")) doPrune(txt);
   else doReset(txt);
   old_text = txt;

   if (!is_static && is_common) search_text = txt;
}





void setDefaultText(String text)
{
   if (text == null) text = "";

   input_field.setText(text);

   if (text.equals(old_text)) return;

   tree_model.reset(text,true);

   old_text = text;

   fixupDisplay(text);
}



@Override public void setScaleFactor(double sf)
{
   Font ft = BASS_PROPERTIES.getFontOption(BASS_TEXT_FONT_PROP,BASS_TEXT_FONT);
   float f = ft.getSize2D();
   if (f != 1.0) {
      f *= (float) sf;
      ft = ft.deriveFont(f);
    }
   Font ftb = ft.deriveFont(Font.BOLD);
   input_field.setFont(ft);
   active_options.setFont(ftb);
}



@Override public void actionPerformed(ActionEvent e)
{
   BoardLog.logD("BASS","SEARCH: ACTION PERFORMED");
   BoardMetrics.noteCommand("BASS","Search");

   BassName nm = null;

   TreePath tp = active_options.getSelectionPath();
   if (tp != null) {
      BassTreeNode tn = (BassTreeNode) tp.getLastPathComponent();
      nm = tn.getBassName();
    }
   if (nm == null) {
      nm = tree_model.getSingleton();
    }

   if (nm != null) {
      createBubble(nm,0);
    }

   if (!is_static) setVisible(false);
}




@Override public void grabFocus()
{
   input_field.grabFocus();
}



private void fixupDisplay(String txt)
{
   int kys = BASS_PROPERTIES.getInt(KEYSTROKES_FOR_AUTO_EXPAND_PROP,KEYSTROKES_FOR_AUTO_EXPAND);
   int mxl = BASS_PROPERTIES.getInt(MAX_LEAF_FOR_EXPANDALL_PROP,MAX_LEAF_FOR_EXPANDALL);
   int mxea = BASS_PROPERTIES.getInt(MAX_LEAF_FOR_AUTO_EXPAND_PROP,MAX_LEAF_FOR_AUTO_EXPAND);
   int mnl = BASS_PROPERTIES.getInt(MIN_LEAF_FOR_AUTO_EXPAND_PROP,MIN_LEAF_FOR_AUTO_EXPAND);

   if (txt.trim().length() >= kys) {
      if (tree_model.getLeafCount() <= mxl) expandAll();
      else {
	 int ct = active_options.getRowCount();
	 while (ct < mnl) {
	    for (int i = 0; i < ct; ++i) {
	       active_options.expandRow(i);
	     }
	    ct = active_options.getRowCount();
	 }
       }
      int[] aryIndices = tree_model.getIndicesOfFirstMethod();
      int index = 0;
      for (int i=0; i<aryIndices.length; i++) index += aryIndices[i];
      active_options.setSelectionRow(index);
    }
   else {
      if (tree_model.getLeafCount() > mxea) collapseAll();
      else expandAll();

      active_options.setSelectionRow(-1);
   }
}




private void doPrune(String txt)
{
   if (bk_reset) {
      synchronized (this) {
	 if (cur_reseter != null) {
	    if (cur_reseter.setNextText(txt)) return;
	  }
       }
    }

   tree_model.prune(txt,true);
   fixupDisplay(txt);
}



private void doReset(String txt)
{
   if (!bk_reset) {
      tree_model.reset(txt,true);
      fixupDisplay(txt);
    }
   else {
      synchronized (this) {
	 if (cur_reseter != null) {
	    if (cur_reseter.setNextText(txt)) return;
	  }
	 cur_reseter = new Reseter(txt);
	 BoardThreadPool.start(cur_reseter);
       }
    }
}




private class Reseter implements Runnable {

   private String reset_text;
   private boolean is_active;


   Reseter(String txt) {
      reset_text = txt;
      is_active = true;
    }

   synchronized boolean setNextText(String txt) {
      if (!is_active) return false;
      reset_text = txt;
      return true;
    }

   @Override public void run() {
      BowiFactory.startTask();
      try {
	 for ( ; ; ) {
	    for ( ; ; ) {
	       String t;
	       synchronized (this) {
		  t = reset_text;
		  if (t == null) break;
		  reset_text = null;
		}
	       tree_model.reset(t,false);
	     }

	    try {
	       SwingUtilities.invokeAndWait(new TreeUpdater());
	     }
	    catch (InterruptedException e) { }
	    catch (InvocationTargetException e) {
	       BoardLog.logE("BASS","Problem with global tree update",e);
	     }

	    synchronized (this) {
	       if (reset_text == null) {
		  is_active = false;
		  break;
		}
	     }
	  }
       }
      finally { BowiFactory.stopTask(); }
    }

}	// end of inner class Reseter



private final class TreeUpdater implements Runnable {

   @Override public void run() {
      tree_model.globalUpdate();
    }

}	// end of inner class TreeUpdater




/********************************************************************************/
/*										*/
/*	Method to create a new bubble						*/
/*										*/
/********************************************************************************/

private void createBubble(BassName bn,int ypos)
{
   BowiFactory.startTask();
   try {
      BudaBubble bb = bn.createBubble();
      if (bb != null) {
	 addAndLocateBubble(bb,ypos,null);
       }
      else {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 BudaBubbleGroup bbg = bn.createBubbleGroup(bba);
	 if (bbg != null) {
	    addAndLocateBubbleGroup(bbg,ypos,null);
	  }
       }
    }
   finally { BowiFactory.stopTask(); }
}




private void createTextSearchBubble()
{
   BassTextBubble btb = new BassTextBubble(input_field.getText(),null);
   addAndLocateBubble(btb,0,null);
   if (!is_static) setVisible(false);
}




void addAndLocateBubble(BudaBubble bb, int ypos,Point ploc)
{
   BudaRoot root = BudaRoot.findBudaRoot(this);
   Rectangle loc = BudaRoot.findBudaLocation(this);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   BudaBubble obb = BudaRoot.findBudaBubble(this);
   if (bba == null || root == null || obb == null) return;

   if (ypos == 0) {
      if (loc != null) ypos = loc.y;
      else if (ploc != null) ypos = ploc.y;
   }
   if (loc == null && ploc != null) loc = new Rectangle(ploc);
   if (loc == null) return;

   if (is_static && bb != null) {
      Dimension bsz = bb.getSize();
      int potx = loc.x - bsz.width - BUBBLE_CREATION_SPACE;
      if (potx < root.getCurrentViewport().x)
	 loc.x = loc.x + obb.getWidth() + BUBBLE_CREATION_SPACE;
      else loc.x = potx;
      loc.y = ypos;
   }
   //added by Ian Strickman
   else if (bb != null) {
      Iterable<BudaBubble> cbb = root.getCurrentBubbleArea().getBubbles();
      Rectangle r;
      for (BudaBubble b : cbb) {
	 r = b.getBounds();
	 if (b != obb && r.contains(loc.getLocation())) {
	    loc.x = r.x + r.width + BUBBLE_CREATION_NEAR_SPACE;
	    break;
	 }
      }
   }

   if (bb != null) {
      bba.addBubble(bb,null,new Point(loc.x,loc.y),PLACEMENT_LOGICAL|PLACEMENT_NEW);
    }
}


private void addAndLocateBubbleGroup(BudaBubbleGroup bbg, int ypos,Point ploc)
{
   BudaRoot root = BudaRoot.findBudaRoot(this);
   Rectangle loc = BudaRoot.findBudaLocation(this);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   BudaBubble obb = BudaRoot.findBudaBubble(this);
   if (bba == null || root == null || obb == null || bbg == null) return;

   if (ypos == 0) {
      if (loc != null) ypos = loc.y;
      else if (ploc != null) ypos = ploc.y;
    }
   if (loc == null && ploc != null) loc = new Rectangle(ploc);
   if (loc == null) return;
   if (is_static) {
      Rectangle bbnds = bbg.getBounds();
      int potx = loc.x - bbnds.width - BUBBLE_CREATION_SPACE;
      if (potx < root.getCurrentViewport().x)
	 loc.x = loc.x + obb.getWidth() + BUBBLE_CREATION_SPACE;
      else loc.x = potx;
      loc.y = ypos;
    }

   Iterable<BudaBubble> cbb = root.getCurrentBubbleArea().getBubbles();
   Rectangle r;
   for (BudaBubble b : cbb) {
      r = b.getBounds();
      if (b != obb && r.contains(loc.getLocation())) {
	 loc.x = r.x + r.width + BUBBLE_CREATION_NEAR_SPACE;
	 break;
       }
    }

   bba.showBubbleGroup(bbg,loc.getLocation());
}


/*
 * Added by Hsu-Sheng Ko
 */

private BudaBubble createPreviewBubble(BassName bn, int xpos, int ypos)
{
   BudaRoot root = BudaRoot.findBudaRoot(this);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   Rectangle loc = BudaRoot.findBudaLocation(this);
   if (bba == null || loc == null || root == null) return null;

   BudaBubble previewbubble = bn.createPreviewBubble();
   if (previewbubble == null) return null;

   int x0 = loc.x + xpos + 50;
   int y0 = loc.y + ypos + 45;

   Dimension bsz = previewbubble.getSize();
   Rectangle r = root.getCurrentViewport();

   if (x0 + bsz.width > r.x + r.width) x0 = r.x + r.width - bsz.width;
   if (x0 < r.x) x0 = r.x;
   if (y0 + bsz.height > r.y + r.height) {
      y0 = y0 - bsz.height - active_options.getRowBounds(0).height; 
    }
   if (y0 < r.y) y0 = r.y;

   bba.add(previewbubble, new BudaConstraint(BudaBubblePosition.HOVER,x0,y0));

   return previewbubble;
}




/********************************************************************************/
/*										*/
/*	PopupMenu method							*/
/*										*/
/********************************************************************************/

boolean handlePopupMenu(MouseEvent e)
{
   Point pt = new Point(e.getXOnScreen(), e.getYOnScreen());
   SwingUtilities.convertPointFromScreen(pt, active_options);
   TreePath tp = active_options.getPathForLocation(pt.x, pt.y);

   if (tp == null) return false;

   Rectangle rowrect = active_options.getPathBounds(tp);

   BassName forname = null;

   StringBuffer fullname = new StringBuffer();
   for (int i = 0; i < tp.getPathCount(); ++i) {
      BassTreeBase btb = (BassTreeBase) tp.getPathComponent(i);
      int ln = fullname.length();
      if (ln == 0) fullname.append(btb.getLocalName());
      else if (fullname.charAt(ln-1) == ':') fullname.append(btb.getLocalName());
      else if (fullname.charAt(ln-1) == '.') fullname.append(btb.getLocalName());
      else {
	 fullname.append(".");
	 fullname.append(btb.getLocalName());
       }
      // System.err.println("FLAGS COMPUTED");

      if (btb.getBassName() != null) forname = btb.getBassName();
    }

   Point where = new Point(rowrect.x,rowrect.y+rowrect.height);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(active_options);
   if (bba == null) return false;

   Point bbawhere = SwingUtilities.convertPoint(active_options,where,bba);
   JPopupMenu menu = new JPopupMenu();

   String fnm = fullname.toString();
   if (fnm.startsWith("ALL.")) fnm = fnm.substring(4);

   BassFactory bf = BassFactory.getFactory();
   bf.addButtons(this,bbawhere,menu,fnm,forname);

   if (menu.getComponentCount() == 0) return false;

   menu.show(active_options,where.x,where.y);

   return true;
}




/********************************************************************************/
/*										*/
/*	Tree actions								*/
/*										*/
/********************************************************************************/

private void expandAll()
{
   for (int i = 0; i < active_options.getRowCount(); ++i) {
      active_options.expandRow(i);
   }

   if (active_options.getRowCount() > 0) active_options.expandRow(0);

}



private void compact()
{
   int mxd = -1;
   for (int i = 0; i < active_options.getRowCount(); ++i) {
      TreePath tp = active_options.getPathForRow(i);
      int ct = tp.getPathCount();
      if (ct > mxd) mxd = ct;
    }
   for (int i = active_options.getRowCount() - 1; i >= 0; --i) {
      TreePath tp = active_options.getPathForRow(i);
      if (tp.getPathCount() == mxd-1) active_options.collapseRow(i);
    }
}




private void collapseAll()
{
   for (int i = active_options.getRowCount() - 1; i >= 0; --i) {
      active_options.collapseRow(i);
    }
}

private void moveUpSelection()
{
   if (active_options.getSelectionCount() > 0) {
      int index = active_options.getSelectionRows()[0];
      if (index > 0)
	 active_options.setSelectionRow(index - 1);
      active_options.scrollRectToVisible(active_options.getPathBounds(active_options.getSelectionPath()));
    }
}

private void moveDownSelection()
{
   if (active_options.getSelectionCount() > 0) {
      int index = active_options.getSelectionRows()[0];
      if (index < active_options.getRowCount() - 1)
	 active_options.setSelectionRow(index + 1);
      active_options.scrollRectToVisible(active_options.getPathBounds(active_options.getSelectionPath()));
    }
}




/********************************************************************************/
/*										*/
/*	Actions for the search							*/
/*										*/
/********************************************************************************/

private static class AbortAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   AbortAction() {
      super("AbortSearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","AbortSearch");
      BudaBubble bb = BudaRoot.findBudaBubble((Component) e.getSource());
      if (bb != null) {
	 BudaRoot br = BudaRoot.findBudaRoot(bb);
	 br.removeCurrentBubble();
   //	    BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   //	    bba.removeCurrentBubble(null);
   //	    bb.setVisible(false);
      }
    }

}	// end of inner class AbortAction




private static class UpSelectionAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   UpSelectionAction() {
      super("UpSelectionAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","UpSelection");
      for (Component c = (Component) e.getSource(); c != null; c = c.getParent()) {
	 if (c instanceof BassSearchBox) {
	    BassSearchBox bx = (BassSearchBox) c;
	    bx.moveUpSelection();
	    break;
	  }
       }
    }

}


private static class DownSelectionAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   DownSelectionAction() {
      super("DownSelectionAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","DownSelection");
      for (Component c = (Component) e.getSource(); c != null; c = c.getParent()) {
	 if (c instanceof BassSearchBox) {
	    BassSearchBox bx = (BassSearchBox) c;
	    bx.moveDownSelection();
	    break;
	  }
       }
    }

}

private static class ExpandAllAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ExpandAllAction() {
      super("ExpandAllAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","ExpandAll");
      for (Component c = (Component) e.getSource(); c != null; c = c.getParent()) {
	 if (c instanceof BassSearchBox) {
	    BassSearchBox bx = (BassSearchBox) c;
	    bx.expandAll();
	    break;
	  }
       }
    }

}	// end of inner class ExpandAllAction



private static class CompactAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   CompactAction() {
      super("CompactAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","Compact");
      for (Component c = (Component) e.getSource(); c != null; c = c.getParent()) {
	 if (c instanceof BassSearchBox) {
	    BassSearchBox bx = (BassSearchBox) c;
	    bx.compact();
	  }
       }
    }

}	// end of inner class CompactAction




private static class TextSearchAction extends AbstractAction {

   private static final long serialVersionUID = 1L;

   TextSearchAction() {
      super("TextSearchAction");
   }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","TextSearch");
      for (Component c = (Component) e.getSource(); c != null; c = c.getParent()) {
	 if (c instanceof BassSearchBox) {
	    ((BassSearchBox) c).createTextSearchBubble();
	    break;
	 }
      }
   }

}	// end of inner class TextSearchAction



private class ClearInputAction extends AbstractAction {

   private static final long serialVersionUID = 1L;

   ClearInputAction() {
      super("Clear Input");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","ClearInput");
      input_field.setText("");
    }

}	// end of inner class ClearInputAction


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()			{ return "BASS"; }


@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","SEARCH");
   xw.field("STATIC",is_static);
   xw.field("TEXT",old_text);
   tree_model.outputXml(xw);
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintComponent(Graphics g)
{
   super.paintComponent(g);

   Graphics2D g2 = (Graphics2D) g.create();
   Dimension sz = getSize();
   Color bptc = BoardColors.getColor(BASS_PANEL_TOP_COLOR_PROP);
   Color bpbc = BoardColors.getColor(BASS_PANEL_BOTTOM_COLOR_PROP);
   BoardColors.setColors(this,bptc);
   Paint p = new GradientPaint(0f,0f,bptc,0f,sz.height,bpbc);
   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   g2.setPaint(p);
   g2.fill(r);

}




/********************************************************************************/
/*										*/
/*	Drag/drop handler							*/
/*										*/
/********************************************************************************/

private final class Transferer extends TransferHandler {

   private static final long serialVersionUID = 1;

   @Override protected Transferable createTransferable(JComponent c) {
      BassSearchBox bsb = null;
      for (Component jc = c; jc != null; jc = jc.getParent()) {
	 if (jc instanceof BassSearchBox) {
	    bsb = (BassSearchBox) jc;
	    break;
	  }
       }
      if (bsb == null) return null;

      TreePath [] tp = bsb.active_options.getSelectionPaths();
      if (tp == null) return null;
      TransferBubble tb = new TransferBubble(tp);
      if (!tb.isValid()) return null;
      return tb;
    }

   @Override public int getSourceActions(JComponent c) {
      return COPY;
    }

}	// end of inner class Transferer



private class TransferBubble implements Transferable, BudaConstants.BudaDragBubble,
	BudaConstants.BudaDragBubbleGroup
{
   private List<BassName> tree_entries;

   TransferBubble(TreePath [] pths) {
      tree_entries = new ArrayList<>();
      for (int i = 0; i < pths.length; ++i) {
	 BassTreeNode btn = (BassTreeNode) pths[i].getLastPathComponent();
	 BassName bn = btn.getBassName();
	 if (bn != null) tree_entries.add(bn);
       }
    }

   boolean isValid()				{ return tree_entries.size() > 0; }

   @Override public Object getTransferData(DataFlavor df) {
      if (df == BudaRoot.getBubbleTransferFlavor()) return this;
      return null;
    }

   @Override public DataFlavor [] getTransferDataFlavors() {
       return new DataFlavor [] {
	     BudaRoot.getBubbleTransferFlavor(),
	     BudaRoot.getBubbleGroupTransferFlavor() };
     }

   @Override public boolean isDataFlavorSupported(DataFlavor f) {
      if (f.equals(BudaRoot.getBubbleTransferFlavor())) return true;
      return false;
    }

   @Override public BudaBubble [] createBubbles() {
      List<BudaBubble> rlst = new ArrayList<>();
      for (BassName ent : tree_entries) {
	 BudaBubble bbl = ent.createBubble();
	 if (bbl != null) rlst.add(ent.createBubble());
       }
      BudaBubble [] rslt = new BudaBubble[rlst.size()];
      rslt = rlst.toArray(rslt);
      return rslt;
    }

   @Override public BudaBubbleGroup createBubbleGroup() {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BassSearchBox.this);
      if (bba == null) return null;
      for (BassName ent : tree_entries) {
	 BudaBubbleGroup grp = ent.createBubbleGroup(bba);
	 if (grp != null) return grp;
       }
      return null;
    }

}	// end of inner class TransferBubble




/********************************************************************************/
/*										*/
/*	Mouse checker for double clicks 					*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter {

   private TreePath	pressed_path;

   Mouser() {
      pressed_path = null;
    }

   @Override public void mousePressed(MouseEvent e) {
      int srow = active_options.getRowForLocation(e.getX(),e.getY());
      TreePath spath = active_options.getPathForLocation(e.getX(),e.getY());
      pressed_path = spath;
      if (srow != -1) {
	 if (e.getClickCount() == 1) {
	    active_options.setSelectionRow(srow);
	  }
      }
   }

   @Override public void mouseClicked(MouseEvent e) {
      TreePath spath = pressed_path;
      if (spath == null) spath = active_options.getPathForLocation(e.getX(),e.getY());
      if (spath != null && e.isControlDown()) {
         active_options.collapsePath(spath);
         return;
       }
      int cct = BoardProperties.getProperties("Bass").getInt("Bass.click.count",1);
      if (spath != null && e.getClickCount() == cct) {
            BassTreeNode tn = (BassTreeNode) spath.getLastPathComponent();
            BassName bn = tn.getBassName();
            BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(active_options);
            if (bn != null && bba != null) {
               Point pt = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),bba);
               createBubble(bn,pt.y);
               if (!is_static && getParent() != null) getParent().setVisible(false);
             }
          }
      input_field.grabFocus();
   }

}	// end of inner class Mouser




/********************************************************************************/
/*										*/
/*	Hover interaction over this tree :: added by Hsu-Sheng Ko		*/
/*										*/
/********************************************************************************/

private class Hoverer extends BudaHover {

   private BudaBubble preview_bubble;
   private JComponent preview_component;

   Hoverer() {
      super(active_options);
      preview_bubble = null;
      preview_component = null;
    }

   @Override public void handleHover(MouseEvent e) {
      if (!BASS_PROPERTIES.getBoolean(BASS_HOVER_OPTION_NAME)) return;
      TreePath tp = active_options.getPathForLocation(e.getX(), e.getY());
   
      if (tp != null) {
         BassTreeNode tn = (BassTreeNode) tp.getLastPathComponent();
         BassName bn = tn.getBassName();
   
         int xpos = e.getX() - scroll_pane.getViewport().getViewPosition().x;
         int ypos = e.getY() - scroll_pane.getViewport().getViewPosition().y;
   
         String fn = null;
   
         if (bn != null) {
            preview_bubble = createPreviewBubble(bn,xpos,ypos);
            if (preview_bubble == null) fn = bn.createPreviewString();
          }
         else {
            for (int i = 1; i < tp.getPathCount(); ++i) {
               Object o1 = tp.getPathComponent(i);
               if (o1 instanceof BassTreeNode) {
        	  BassTreeNode btn = (BassTreeNode) o1;
        	  if (fn == null) fn = btn.getLocalName();
        	  else fn = fn + "." + btn.getLocalName();
        	}
             }
            if (fn != null) {
               int idx = fn.indexOf("#");
               if (idx > 0) fn = fn.substring(idx+1);
             }
          }
   
         if (preview_bubble == null && fn != null) {
            JLabel tt = new JLabel(fn);
            tt.setOpaque(true);
            BoardColors.setColors(tt,"Bass.DefaultLabelColor");
            tt.setBorder(BorderFactory.createLineBorder(BoardColors.getColor("Bass.PanelBorderColor")));
            BudaRoot root = BudaRoot.findBudaRoot(BassSearchBox.this);
            BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BassSearchBox.this);
            Rectangle loc = BudaRoot.findBudaLocation(BassSearchBox.this);
            if (loc == null || bba == null || root == null) return;
            int x0 = loc.x + xpos + 50;
            int y0 = loc.y + ypos + 45;
            Dimension bsz = tt.getPreferredSize();
            Rectangle r = root.getCurrentViewport();
            if (x0 + bsz.width > r.x + r.width) x0 = r.x + r.width - bsz.width;
            if (x0 < r.x) x0 = r.x;
            if (y0 + bsz.height > r.y + r.height) y0 = y0 - bsz.height - active_options.getRowBounds(0).height;
            if (y0 < r.y) y0 = r.y;
            bba.add(tt,new BudaConstraint(BudaBubblePosition.HOVER,x0,y0));
            preview_component = tt;
          }
       }
    }

   @Override public void endHover(MouseEvent e) {
      if (preview_bubble != null){
         BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(our_self);
         if (bba != null) bba.removeBubble(preview_bubble);
         preview_bubble.disposeBubble();
         preview_bubble = null;
       }
      if (preview_component != null) {
         preview_component.setVisible(false);
         preview_component = null;
       }
    }

}	// end of inner class Hoverer



/********************************************************************************/
/*										*/
/*	Cell renderer for search box						*/
/*										*/
/********************************************************************************/


private static class SearchBoxCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer {

   private static final long serialVersionUID = 1;


   SearchBoxCellRenderer() {
      setBackgroundNonSelectionColor(null);
      setBackgroundSelectionColor(null);
      setBackground(BoardColors.transparent());
    }


   @Override public Component getTreeCellRendererComponent(JTree tree,
        						      Object value,
        						      boolean sel,
        						      boolean expanded,
        						      boolean leaf,
        						      int row,
        						      boolean hasfocus) {
      JLabel label = this;
      BassTreeBase btb = (BassTreeBase) value;
   
      // System.err.println("BASS RENDER " + value);
      String txt = value.toString();
      int validx = txt.indexOf("#@");
      if (validx > 0) txt = txt.substring(validx+1);
      label.setText(txt);
      label.setFont(tree.getFont());
      label.setOpaque(false);
      String vnm = value.toString();
      Icon icn = null;
   
      if (vnm.equals(BASS_BUDDY_LIST_NAME)) {
         if (expanded) icn = people_collapse_image;
         else icn = people_expand_image;
       }
      else if (vnm.equals(BASS_DOC_LIST_NAME)){
         if (expanded) icn = docs_collapse_image;
         else icn = docs_expand_image;
       }
      else if (vnm.equals(BASS_CONFIG_LIST_NAME)){
         if (expanded) icn = config_collapse_image;
         else icn = config_expand_image;
       }
      else if (vnm.equals(BASS_PROCESS_LIST_NAME)){
         if (expanded) icn = process_collapse_image;
         else icn = process_expand_image;
       }
      else if (vnm.equals(BASS_COURSE_LIST_NAME)){
         if (expanded) icn = courses_collapse_image;
         else icn = courses_expand_image;
       }
      else if (leaf) {
         BassName bn = ((BassTreeNode) value).getBassName();
         icn = bn.getDisplayIcon();
       }
      else {
         if (expanded) icn = btb.getCollapseIcon();
         else icn = btb.getExpandIcon();
       }
   
      if (sel) {
         BoardColors.setColors(label,BASS_PANEL_SELECT_BACKGROUND_PROP);
         label.setOpaque(true);
       }
      else {
         label.setBackground(BoardColors.transparent());
         label.setForeground(tree.getForeground());
       }
   
      BassFlag f = BassFactory.getFactory().getFlagForName(btb.getBassName(),btb.getFullName());
      if (f != null) {
         Icon i1 = f.getOverlayIcon();
         if (i1 != null) {
            if (icn == null) icn = i1;
            else icn = new OverlayIcon(icn,i1);
          }
       }
      
      synchronized (label) {
         label.setIcon(icn);
       }
   
      return label;
    }

}	// end of inner class SearchBoxCellRenderer




/********************************************************************************/
/*										*/
/*	Class for composting icons						*/
/*										*/
/********************************************************************************/

private static class OverlayIcon implements Icon {

   private List<Icon> icon_set;

   OverlayIcon(Icon... base) {
      icon_set = new ArrayList<Icon>();
      for (Icon ic : base) {
	 icon_set.add(ic);
       }
    }

   @Override public int getIconHeight() {
      if (icon_set.isEmpty()) return 0;
      return icon_set.get(0).getIconHeight();
    }

   @Override public int getIconWidth() {
      if (icon_set.isEmpty()) return 0;
      return icon_set.get(0).getIconWidth();
    }

   @Override public void paintIcon(Component c,Graphics g,int x,int y) {
      for (Icon ic : icon_set) {
	 ic.paintIcon(c,g,x,y);
       }
    }

}	// end of inner class OverlayIcon





/********************************************************************************/
/*										*/
/*	Tree with a gradient background 					*/
/*										*/
/********************************************************************************/

private static class GradientTree extends JTree {

   private static final long serialVersionUID = 1;

   GradientTree(TreeModel treemodel) {
      super(treemodel);
      setOpaque(false);
      BoardColors.setColors(this,BASS_PANEL_TOP_COLOR_PROP);
    }

   @Override protected void paintComponent(Graphics g) {
       Graphics2D g2 = (Graphics2D) g.create();
       Dimension sz = getSize();
       Color bptc = BoardColors.getColor(BASS_PANEL_TOP_COLOR_PROP);
       Color bpbc = BoardColors.getColor(BASS_PANEL_BOTTOM_COLOR_PROP);
       BoardColors.setColors(this,bptc);
       Paint p = new GradientPaint(0f,0f,bptc,0f,sz.height,bpbc);
       Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
       g2.setPaint(p);
       g2.fill(r);

       try {
	  super.paintComponent(g);
	}
       catch (Throwable t) {
	  // update might have occurred while trying to paint
	  try {
	     Thread.sleep(500);
	   }
	  catch (InterruptedException e) { }
	  super.paintComponent(g);
	}
    }

   @Override public void expandRow(int row) {
      super.expandRow(row);
      // input_field.grabFocus();
   }

   @Override public void collapseRow(int row) {
      super.collapseRow(row);
      // input_field.grabFocus();
   }

   @Override public Dimension getPreferredScrollableViewportSize() {
      Dimension psz = super.getPreferredScrollableViewportSize();
      if (psz.width > MAX_SEARCH_WIDTH) psz.width = MAX_SEARCH_WIDTH;
      // if (psz.height > MAX_SEARCH_HEIGHT) psz.height = MAX_SEARCH_HEIGHT;
      return psz;
    }

}	// end of inner class GradientTree




/********************************************************************************/
/*										*/
/*	Methods to track expanded nodes 					*/
/*										*/
/********************************************************************************/

private void useExpandedNodes()
{
   // Possible approach here: sort the set of expanded nodes by string length
   // before expanding, check that parent is visible

   List<String> expand = new ArrayList<String>(expanded_nodes);

   for (String nm : expand) {
      TreePath tp = tree_model.getTreePath(nm);
      // TODO: check to ensure that all parents are visible before expanding path
      if (tp != null) active_options.expandPath(tp);
    }
}

private void useLocalExpandedNodes()
{
   List<String> expand;
   synchronized (local_expands) {
      expand = new ArrayList<String>(local_expands);
    }

   for (String nm : expand) {
      TreePath tp = tree_model.getTreePath(nm);
      if (tp == null) continue;
      TreePath tp0 = new TreePath(tp.getPathComponent(0));
      boolean allow = true;
      for (int i = 1; allow && i < tp.getPathCount()-1; ++i) {
	 Object o = tp.getPathComponent(i);
	 tp0 = tp0.pathByAddingChild(o);
	 if (active_options.isCollapsed(tp0)) {
	    String what = getPathName(tp0);
	    if (!expand.contains(what))
	       allow = false;
	 }
      }
      if (allow && tp != null) active_options.expandPath(tp);
    }
}





@Override public void treeCollapsed(TreeExpansionEvent evt)
{
   TreePath tp = evt.getPath();
   String nm = getPathName(tp);
   if (is_common) expanded_nodes.remove(nm);
   synchronized (local_expands) {
      local_expands.remove(nm);
    }
}



@Override public void treeExpanded(TreeExpansionEvent evt)
{
   TreePath tp = evt.getPath();    
   String nm = getPathName(tp);
   if (is_common) expanded_nodes.add(nm);
   synchronized (local_expands) {
      local_expands.add(nm);
    }
}



private String getPathName(TreePath tp)
{
   StringBuffer buf = new StringBuffer();
   int n = tp.getPathCount();
   for (int i = 1; i < n; ++i) {
      BassTreeNode btn = (BassTreeNode) tp.getPathComponent(i);
      buf.append(btn.getLocalName());
      buf.append("@");
    }
   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Handle tree model changes						*/
/*										*/
/********************************************************************************/

private final class UpdateHandler implements TreeModelListener {

   @Override public void treeNodesChanged(TreeModelEvent e)		{ }

   @Override public void treeNodesInserted(TreeModelEvent e)		{ }

   @Override public void treeNodesRemoved(TreeModelEvent e)		{ }

   @Override public void treeStructureChanged(TreeModelEvent e) {
      int mxl = BASS_PROPERTIES.getInt(MAX_LEAF_FOR_EXPANDALL_PROP,MAX_LEAF_FOR_EXPANDALL);
      if (tree_model.getLeafCount() <= mxl) expandAll();
      else useLocalExpandedNodes();
    }

}	// end of inner class UpdateHandler



}	// end of class BassSearchBox




/* end of BassSearchBox.java */
