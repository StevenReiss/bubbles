/********************************************************************************/
/*										*/
/*		BddtStackView.java						*/
/*										*/
/*	Bubbles Environment dynamic debugger tool stack/frame/value bubble	*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardMouser;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaToolTip;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.ivy.swing.SwingTextArea;
import edu.brown.cs.ivy.swing.SwingTreeTable;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;
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
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



class BddtStackView extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	launch_control;
private BumpThread		for_thread;
private boolean 		is_frozen;
private boolean 		is_extinct;
private boolean 		is_valid;
private BddtStackModel		value_model;
private ValueTable		value_component;
private RunEventHandler 	event_handler;
private Expander		tree_expander;
private LabelUpdater		label_updater;
private ValueUpdater		value_updater;
private Selector		node_selector;
private JLabel			title_bar;
private JTextArea		value_area;
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtStackView(BddtLaunchControl ctrl,BumpThread thr)
{
   launch_control = ctrl;
   for_thread = thr;
   is_frozen = false;
   is_extinct = false;
   is_valid = true;
   value_model = new BddtStackModel(thr);

   setupBubble();
}



BddtStackView(BddtLaunchControl ctrl,BumpThreadStack stk)
{
   launch_control = ctrl;
   for_thread = stk.getThread();
   is_frozen = true;
   is_extinct = false;
   is_valid = true;
   value_model = new BddtStackModel(stk);

   setupBubble();
}




BddtStackView(BddtLaunchControl ctrl,BumpStackFrame frm)
{
   launch_control = ctrl;
   for_thread = frm.getThread();
   is_frozen = true;
   is_extinct = false;
   is_valid = true;
   value_model = new BddtStackModel(frm);

   setupBubble();
}




BddtStackView(BddtLaunchControl ctrl,BumpRunValue val,boolean freeze)
{
   launch_control = ctrl;
   for_thread = null;
   is_extinct = false;
   is_valid = true;

   BddtStackModel xmdl = new BddtStackModel(val);
   ValueTreeNode xtn = (ValueTreeNode) xmdl.getRoot();

   if (!freeze && xtn != null && val != null) {
      BddtStackModel pmdl = new BddtStackModel(val.getThread());
      BddtStackModel fmdl = new BddtStackModel(val.getFrame());
      ValueTreeNode fnd = (ValueTreeNode) fmdl.getRoot();
      Object root = pmdl.getRoot();
      ValueTreeNode frame = null;
      for (int i = 0; i < pmdl.getChildCount(root); ++i) {
	 ValueTreeNode vtn = (ValueTreeNode) pmdl.getChild(root,i);
	 if (vtn.getKey().equals(fnd.getKey())) {
	    frame = vtn;
	    break;
	  }
       }
      if (frame != null) {
	 for (int i = 0; i < pmdl.getChildCount(frame); ++i) {
	    ValueTreeNode vtn = (ValueTreeNode) pmdl.getChild(frame,i);
	    if (vtn.getKey().equals(xtn.getKey())) {
	       is_frozen = false;
	       value_model = new BddtStackModel(pmdl,vtn);
	       break;
	    }
	  }
       }
    }
   else if (xtn == null || val == null) is_valid = false;

   if (value_model == null) {
      is_frozen = true;
      value_model = xmdl;
    }

   setupBubble();
}




BddtStackView(BddtStackView base,ValueTreeNode root)
{
   launch_control = base.launch_control;
   value_model = new BddtStackModel(base.value_model,root);
   for_thread = value_model.getThread();
   is_frozen = base.is_frozen;
   is_extinct = base.is_extinct;

   setupBubble();
}



private void setupBubble()
{
   BumpClient bc = BumpClient.getBump();
   BumpRunModel rm = bc.getRunModel();
   event_handler = new RunEventHandler();
   rm.addRunEventHandler(event_handler);

   value_component = new ValueTable(value_model);
   value_component.addMouseListener(new ClickHandler());

   value_area = null;
   value_updater = null;

   if (value_model.showValueArea()) {
      value_area = new SwingTextArea(new HTMLDocument());
      value_area.setEditable(false);
      value_area.setLineWrap(true);
      value_updater = new ValueUpdater();
      value_model.addTreeModelListener(value_updater);
      updateValueArea();
    }

   tree_expander = new Expander(value_component.getTree());
   value_component.addTreeExpansionListener(tree_expander);
   value_model.addTreeModelListener(tree_expander);
   label_updater = new LabelUpdater();
   value_model.addTreeModelListener(label_updater);
   node_selector = new Selector();
   value_component.getSelectionModel().addListSelectionListener(node_selector);

   JScrollPane sp = new JScrollPane(value_component);
   sp.setPreferredSize(new Dimension(BDDT_STACK_WIDTH,BDDT_STACK_HEIGHT));

   title_bar = new JLabel(value_model.getLabel(),SwingConstants.CENTER);
   BoardColors.setColors(title_bar,BDDT_STACK_BOTTOM_COLOR_PROP);

   JPanel pnl = new JPanel(new BorderLayout());
   pnl.add(title_bar,BorderLayout.NORTH);
   pnl.add(sp,BorderLayout.CENTER);

   if (value_area == null) {
      setContentPane(pnl,null);
    }
   else {
      JScrollPane sp1 = new JScrollPane(value_area);
      sp1.setPreferredSize(new Dimension(BDDT_STACK_WIDTH,BDDT_STACK_VALUE_HEIGHT));
      JSplitPane spl = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true,pnl,sp1);
      setContentPane(spl,null);
    }

   value_component.addMouseListener(new FocusOnEntry());
}


@Override protected void localDispose()
{
   detachHandler();
   if (value_model != null) {
      if (value_updater != null) value_model.removeTreeModelListener(value_updater);
      if (tree_expander != null) value_model.removeTreeModelListener(tree_expander);
      if (label_updater != null) value_model.removeTreeModelListener(label_updater);
    }
   if (value_component != null) {
      if (tree_expander != null) value_component.removeTreeExpansionListener(tree_expander);
      if (node_selector != null) value_component.getSelectionModel().removeListSelectionListener(node_selector);
    }

   if (value_model != null) value_model.dispose();
   value_model = null;
}



private void detachHandler()
{
   if (event_handler != null) {
      BumpClient bc = BumpClient.getBump();
      BumpRunModel rm = bc.getRunModel();
      rm.removeRunEventHandler(event_handler);
      event_handler = null;
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isStackValid()				{ return is_valid; }


void expandFirst()
{
   value_component.getTree().expandRow(1);
}

void expandFrame(BumpStackFrame frm)
{
   for (int i = 1; i < value_component.getRowCount(); ++i) {
      Object o = value_component.getValueAt(i,-1);
      if (o instanceof ValueTreeNode) {
	 ValueTreeNode vtn = (ValueTreeNode) o;
	 if (vtn.getFrame() == frm) {
	    value_component.getTree().expandRow(i);
	    break;
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Popup menu and mouse handler methods					*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();
   Point pt = SwingUtilities.convertPoint(getContentPane().getParent(),e.getPoint(),value_component);
   pt = new Point(pt.x,pt.y-5);
   int row = value_component.rowAtPoint(pt);
   Object v0 = value_component.getValueAt(row,-1);
   ValueTreeNode tn = null;
   if (v0 instanceof ValueTreeNode) tn = (ValueTreeNode) v0;
 
   if (tn != null) {
      if (tn.getValue() != null) {
         popup.add(new CopyAction(tn));
       }
      popup.add(new ExtractAction(tn));
      if (tn.getFrame() != null) popup.add(new SourceAction(tn));
   }

   if (!is_frozen) popup.add(new FreezeAction());
   popup.add(getFloatBubbleAction());
   popup.show(value_component,pt.x,pt.y);
}



private final class ClickHandler extends BoardMouser {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2 || e.getClickCount() == 3) {
         Point pt = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),value_component);
         pt = new Point(pt.x,pt.y-5);
         int row = value_component.rowAtPoint(pt);
         Object v0 = value_component.getValueAt(row,-1);
         ValueTreeNode tn = null;
         if (v0 instanceof ValueTreeNode) tn = (ValueTreeNode) v0;
         if (tn != null) {
            AbstractAction aa = null;
            boolean isfrm = tn.getFrame() != null && tn.getValue() == null;
            if (isfrm && e.getClickCount() == 2) {
               aa = new SourceAction(tn);
             }
            else if (isfrm && e.getClickCount() == 3) {
               aa = new ExtractAction(tn);
             }
            else if (!isfrm && e.getClickCount() == 2) {
               aa = new ExtractAction(tn);
             }
            if (aa != null) aa.actionPerformed(null);
          }
       }
    }

}	// end of inner class ClickHandler




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private void makeFrozen(int lvls)
{
   if (is_frozen || is_extinct) return;
   is_frozen = true;

   value_model.freeze(lvls);

   detachHandler();

   repaint();
}



private void makeExtinct()
{
   if (is_frozen || is_extinct) return;
   is_extinct = true;

   detachHandler();

   repaint();
}



private class FreezeAction extends AbstractAction implements Runnable {

   private boolean doing_freeze;

   private static final long serialVersionUID = 1;

   FreezeAction() {
      super("Freeze Values");
      doing_freeze = false;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (is_frozen || is_extinct || doing_freeze) return;
      doing_freeze = true;
      launch_control.startFreeze();
      BoardMetrics.noteCommand("BDDT","StackFreeze");
      BoardThreadPool.start(this);
    }

   @Override public void run() {
      BoardProperties bp = BoardProperties.getProperties("Bddt");
      int lvls = bp.getInt(BDDT_PROPERTY_FREEZE_LEVELS,2);
      makeFrozen(lvls);
      launch_control.doneFreeze();
      doing_freeze = false;
    }

}	// end of inner class FreezeAction




private class ExtractAction extends AbstractAction {

   private ValueTreeNode for_node;

   private static final long serialVersionUID = 1;

   ExtractAction(ValueTreeNode node) {
      super("Extract Value");
      for_node = node;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","StackExtract");
      BddtStackView sv = new BddtStackView(BddtStackView.this,for_node);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtStackView.this);
      bba.addBubble(sv,BddtStackView.this,null,
	    PLACEMENT_PREFER|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
   }
   
}       // end of inner class ExtractAction


private class CopyAction extends AbstractAction {

    private ValueTreeNode for_node;
    
    private static final long serialVersionUID = 1;
    
    CopyAction(ValueTreeNode node) {
       super("Copy Value");
       for_node = node;
     }
    
    @Override public void actionPerformed(ActionEvent e) {
       BoardMetrics.noteCommand("BDDT","StackCopy");
       Object v = for_node.getValue();
       if (v == null) return;
       String s = String.valueOf(v);
       StringSelection ss = new StringSelection(s);
       Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
       clipboard.setContents(ss,null);
     }
    
}       // end of inner class CopyAction


private class SourceAction extends AbstractAction {

   private ValueTreeNode for_node;

   private static final long serialVersionUID = 1;

  SourceAction(ValueTreeNode node) {
      super("Goto Source");
      for_node = node;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BumpStackFrame frm = for_node.getFrame();
      BudaBubble bb = null;
      // TODO: If frm.isSystem(), create a file bubble using the temp file and without eclipse
      // Ideally, would like a method bubble here -- can we fake that
      // need to search for method location in system code, not just project
      // so add a new Bale method createSystemMethodBubble(proj,mid,file)
   
      if (launch_control.frameFileExists(frm)) {
         String proj = frm.getThread().getLaunch().getConfiguration().getProject();
         String mid = frm.getMethod() + frm.getSignature();
         bb = BaleFactory.getFactory().createMethodBubble(proj,mid);
      }
      if (bb != null) {
         BoardMetrics.noteCommand("BDDT","StackSource");
         BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtStackView.this);
         bba.addBubble(bb,BddtStackView.this,null,
               PLACEMENT_PREFER|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
      }
   }
}




/********************************************************************************/
/*										*/
/*	Run event handler							*/
/*										*/
/********************************************************************************/

private final class RunEventHandler implements BumpRunEventHandler {

   @Override public void handleThreadEvent(BumpRunEvent evt) {
      if (evt.getThread() != for_thread) return;
      switch (evt.getEventType()) {
         case THREAD_REMOVE :
            makeExtinct();
            break;
         default:
            break;
       }
    }

}	// end of inner class RunEventHandler





/********************************************************************************/
/*										*/
/*	Value Table implementation						*/
/*										*/
/********************************************************************************/

private class ValueTable extends SwingTreeTable implements BudaConstants.BudaBubbleOutputer
{
   private CellDrawer [] cell_drawer;
   private static final long serialVersionUID = 1;
   
   ValueTable(TreeTableModel mdl) {
      super(mdl);
      setOpaque(false);
      BoardColors.setColors(this,BDDT_STACK_TOP_COLOR_PROP);
      setDragEnabled(true);
      setTransferHandler(new Transferer());
      BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
         TableColumn tc = e.nextElement();
         tc.setHeaderRenderer(new HeaderDrawer(getTableHeader().getDefaultRenderer()));
       }
      cell_drawer = new CellDrawer[2];
      JTree tr = getTree();
      tr.setCellRenderer(new TreeCellRenderer(this));
   
      setRowSelectionAllowed(true);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
   
      setToolTipText("");
    }

   @Override public TableCellRenderer getCellRenderer(int r,int c) {
      if (cell_drawer[c] == null) {
         cell_drawer[c] = new CellDrawer(super.getCellRenderer(r,c));
       }
      return cell_drawer[c];
    }

   @Override protected void paintComponent(Graphics g) {
      if (!is_frozen && !is_extinct && value_model.hasBeenFrozen())
	 is_frozen = true;

      Dimension sz = getSize();
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      Graphics2D g2 = (Graphics2D) g.create();
      Color top = BoardColors.getColor(BDDT_STACK_TOP_COLOR_PROP);
      Color bot = BoardColors.getColor(BDDT_STACK_BOTTOM_COLOR_PROP);
      if (is_frozen) {
	 top = BoardColors.getColor(BDDT_STACK_FROZEN_TOP_COLOR_PROP);
	 bot = BoardColors.getColor(BDDT_STACK_FROZEN_BOTTOM_COLOR_PROP);
       }
      else if (is_extinct) {
	 top = BoardColors.getColor(BDDT_STACK_EXTINCT_TOP_COLOR_PROP);
	 bot = BoardColors.getColor(BDDT_STACK_EXTINCT_BOTTOM_COLOR_PROP);
       }
      if (top.getRGB() != bot.getRGB()) {
	 Paint p = new GradientPaint(0f,0f,top,0f,sz.height,bot);
	 g2.setPaint(p);
       }
      else {
	 g2.setColor(top);
       }
      g2.fill(r);
      value_model.lock();
      try {
	 super.paintComponent(g);
       }
      finally { value_model.unlock(); }
    }

   @Override public String getConfigurator()		{ return "BDDT"; }

   @Override public void outputXml(BudaXmlWriter xw)	{ }

   @Override public JToolTip createToolTip() {
      BudaToolTip btt = new BudaToolTip();
      btt.setComponent(this);
      return btt;
    }

   @Override public String getToolTipText(MouseEvent evt) {
      int row = value_component.rowAtPoint(evt.getPoint());
      Object v0 = value_component.getValueAt(row,-1);
      ValueTreeNode tn = null;
      if (v0 instanceof ValueTreeNode) tn = (ValueTreeNode) v0;
      if (tn != null) {
         StringBuffer buf = new StringBuffer();
         buf.append("<html>");
         Object vobj = tn.getValue();
         String what = tn.getKey();
         if (what == null) return null;
         if (vobj == null) return what;
         buf.append(what);
         buf.append(" = ");
         buf.append(IvyXml.htmlSanitize(vobj.toString()));
         String evl = launch_control.getEvaluationString(tn.getFrame(),tn.getRunValue(),what);
         if (evl != null) {
            buf.append("<hr />");
            // buf.append(IvyXml.htmlSanitize(evl));
            buf.append(evl);
          }
         return buf.toString();
       }
      return null;
    }

}	// end of inner class ValueTable




/********************************************************************************/
/*										*/
/*	Renderers								*/
/*										*/
/********************************************************************************/

private static class HeaderDrawer implements TableCellRenderer {

   private TableCellRenderer default_renderer;
   private Font bold_font;

   HeaderDrawer(TableCellRenderer dflt) {
      default_renderer = dflt;
      bold_font = null;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      if (bold_font == null) {
	 bold_font = cmp.getFont();
	 bold_font = bold_font.deriveFont(Font.BOLD);
       }
      cmp.setFont(bold_font);
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of inner class HeaderRenderer




private static class CellDrawer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   CellDrawer(TableCellRenderer dflt) {
      default_renderer = dflt;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of innerclass CellRenderer



private static class TreeCellRenderer extends DefaultTreeCellRenderer {

   private static final long serialVersionUID = 1;

   TreeCellRenderer(Component t) {
      setBackgroundNonSelectionColor(null);
      BoardColors.setTransparent(this,t);
    }

   @Override public Component getTreeCellRendererComponent(JTree tree,
							      Object value,
							      boolean sel,
							      boolean expanded,
							      boolean leaf,
							      int row,
							      boolean hasfocus) {
      Component c = super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasfocus);
      return c;
   }

}	// end of inner class TreeCellRenderer




/********************************************************************************/
/*										*/
/*	Tree Update Management							*/
/*										*/
/********************************************************************************/

private final class LabelUpdater implements TreeModelListener {

   @Override public void treeNodesChanged(TreeModelEvent e) {
      if (value_model == null || title_bar == null) return;
      String txt = value_model.getLabel();
      if (title_bar.getText().equals(txt)) return;
      title_bar.setText(txt);
    }

   @Override public void treeNodesInserted(TreeModelEvent e) { }
   @Override public void treeNodesRemoved(TreeModelEvent e) { }

   @Override public void treeStructureChanged(TreeModelEvent e) {
      if (value_model == null || title_bar == null) return;
      title_bar.setText(value_model.getLabel());
    }

}	// end of inner class LabelUpdaer



private final class ValueUpdater implements TreeModelListener {

   @Override public void treeNodesChanged(TreeModelEvent e) {
      updateValueArea();
    }

   @Override public void treeNodesInserted(TreeModelEvent e) { }
   @Override public void treeNodesRemoved(TreeModelEvent e) { }

   @Override public void treeStructureChanged(TreeModelEvent e) {
      updateValueArea();
    }

}	// end of inner class ValueUpdater



private static class Expander implements TreeExpansionListener, TreeModelListener {

   private Set<String> expand_set;
   private JTree       for_tree;

   Expander(JTree tr) {
      expand_set = new HashSet<String>();
      for_tree = tr;
    }

   @Override public void treeCollapsed(TreeExpansionEvent evt) {
      BoardMetrics.noteCommand("BDDT","StackViewCollapse");
      ValueTreeNode tn = (ValueTreeNode) evt.getPath().getLastPathComponent();
      expand_set.remove(tn.getKey());
    }

   @Override public void treeExpanded(TreeExpansionEvent evt) {
      BoardMetrics.noteCommand("BDDT","StackViewExpand");
      ValueTreeNode tn = (ValueTreeNode) evt.getPath().getLastPathComponent();
      expand_set.add(tn.getKey());
    }

   @Override public void treeNodesChanged(TreeModelEvent e)	{ }
   @Override public void treeNodesInserted(TreeModelEvent e)	{ }
   @Override public void treeNodesRemoved(TreeModelEvent e)	{ }
   @Override public void treeStructureChanged(TreeModelEvent e) {
      SwingUtilities.invokeLater(new ExpandNodes(this));
    }

   private void checkNodes() {
      checkNode(null,(ValueTreeNode) for_tree.getModel().getRoot());
    }

   private void checkNode(TreePath tp,ValueTreeNode vn) {
      boolean exp = false;

      if (tp == null) exp = true;
      else if (vn.getParent() == null) exp = true;
      else if (expand_set.contains(vn.getKey())) exp = true;

      if (exp) {
	 if (tp == null) tp = new TreePath(vn);
	 else tp = tp.pathByAddingChild(vn);
	 try {
	    for_tree.expandPath(tp);
	  }
	 catch (ArrayIndexOutOfBoundsException e) {
	    return;
	  }
	 for (int i = 0; i < vn.getChildCount(); ++i) {
	    checkNode(tp,(ValueTreeNode) vn.getChildAt(i));
	  }
       }
    }

}	// end of inner class Expander



private static class ExpandNodes implements Runnable {

   private Expander for_expander;

   ExpandNodes(Expander ex) {
      for_expander = ex;
    }

   @Override public void run() {
      for_expander.checkNodes();
    }

}	// end of inner class ExpandNodes



private final class Selector implements ListSelectionListener {

   @Override public void valueChanged(ListSelectionEvent evt) {
      int row = evt.getFirstIndex();
      Object v0 = value_component.getValueAt(row,-1);
      ValueTreeNode tn = null;
      if (v0 instanceof ValueTreeNode) tn = (ValueTreeNode) v0;
      if (tn != null) {
	 BumpStackFrame frm = tn.getFrame();
	 if (frm != null) {
	    launch_control.setActiveFrame(frm);
	  }
       }
    }

}	// end of inner class Selector



/********************************************************************************/
/*										*/
/*	Handle value area management						*/
/*										*/
/********************************************************************************/

private void updateValueArea()
{
    if (value_area == null || value_model == null) return;

    ValueTreeNode vtn = (ValueTreeNode) value_model.getRoot();
    if (value_component.getSelectedRow() >= 0) {
       // use selection ???
     }

    BumpRunValue rv = vtn.getRunValue();
    BumpStackFrame frm = vtn.getFrame();
    String id = vtn.getKey();

    String txt = launch_control.getEvaluationString(frm,rv,id);
    if (txt == null) return;

    if (!txt.startsWith("<html>")) txt = "<html><body>" + txt;

    try {
       HTMLDocument doc = (HTMLDocument) value_area.getDocument();
       if (doc.getParser() == null) {
	  HTMLEditorKit.Parser p = new ParserDelegator();
	  doc.setParser(p);
	}
       int len = doc.getLength();
       doc.remove(0,len);
       doc.insertAfterStart(doc.getDefaultRootElement(),txt);
    }
    catch (Exception e) {
       System.err.println("PROBLEM INSERTING TEXT");
    }

    // value_area.setText(txt);
}




/********************************************************************************/
/*										*/
/*	Methods to pull out a stack bubble					*/
/*										*/
/********************************************************************************/

private final class Transferer extends TransferHandler {

   private static final long serialVersionUID = 1;

   @Override protected Transferable createTransferable(JComponent c) {
      ValueTable tbl = null;
      for (Component jc = c; jc != null; jc = jc.getParent()) {
         if (jc instanceof ValueTable) {
            tbl = (ValueTable) jc;
            break;
          }
       }
      if (tbl == null) return null;
   
      JTree tree = tbl.getTree();
      TreePath [] sels = tree.getSelectionPaths();
      if (sels == null || sels.length == 0) return null;
   
      TransferBubble tb = new TransferBubble(sels);
      if (!tb.isValid()) return null;
      return tb;
    }

   @Override public int getSourceActions(JComponent c) {
      return COPY;
    }

}	// end of inner class Transferer



private class TransferBubble implements Transferable, BudaConstants.BudaDragBubble {

   private List<ValueTreeNode> value_entries;

   TransferBubble(TreePath [] sels) {
      value_entries = new ArrayList<ValueTreeNode>();
      for (int i = 0; i < sels.length; ++i) {
	 ValueTreeNode tn = (ValueTreeNode) sels[i].getLastPathComponent();
	 if (tn != null) value_entries.add(tn);
       }
    }

   boolean isValid()				{ return value_entries.size() > 0; }

   @Override public Object getTransferData(DataFlavor df) {
      if (df == BudaRoot.getBubbleTransferFlavor()) return this;
      return null;
    }

   @Override public DataFlavor [] getTransferDataFlavors() {
       return new DataFlavor [] { BudaRoot.getBubbleTransferFlavor() };
     }

   @Override public boolean isDataFlavorSupported(DataFlavor f) {
      if (f.equals(BudaRoot.getBubbleTransferFlavor())) return true;
      return false;
    }

   @Override public BudaBubble [] createBubbles() {
      BudaBubble [] rslt = new BudaBubble[value_entries.size()];
      int i = 0;
      for (ValueTreeNode tn : value_entries) {
	 rslt[i++] = new BddtStackView(BddtStackView.this,tn);
       }
      return rslt;
    }

}	// end of inner class TransferBubble





}	// end of class BddtStackView




/* end of BddtStackView.java */
