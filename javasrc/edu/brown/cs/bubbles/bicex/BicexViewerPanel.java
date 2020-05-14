/********************************************************************************/
/*										*/
/*		BicexViewerBubble.java						*/
/*										*/
/*	Variable value viewer bubble						*/
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


import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.ivy.swing.SwingTreeTable;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;

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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


class BicexViewerPanel extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BicexEvaluationContext	display_context;
private ValueTable		value_table;
private Expander		expand_listener;
private UserSelection           selection_listener;
private JTextArea               tostring_area;
private JSplitPane              split_pane;
private boolean                 show_tostring;






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexViewerPanel(BicexEvaluationViewer ev)
{
   super(ev);
   display_context = ev.getContext();
   show_tostring = false;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override protected BicexEvaluationContext getContext()
{
   return display_context;
}




/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

@Override protected JComponent setupPanel()
{
   value_table = new ValueTable();
   expand_listener = new Expander(value_table.getTree());
   selection_listener = new UserSelection();
   value_table.addTreeExpansionListener(expand_listener);
   getDataModel().addTreeModelListener(expand_listener);
   value_table.getSelectionModel().addListSelectionListener(selection_listener);
   
   tostring_area = new JTextArea(3,40);  

   split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true,value_table,
      (show_tostring ? tostring_area : null));
   split_pane.setResizeWeight((show_tostring ? 0.75 : 1.0));
   
   // return value_table;
   
   return split_pane;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override void update()
{
   if (display_context == null) {
      display_context = getRootContext();
   }
   else {
      // need to find proper new context
   }
}


@Override void updateTime()
{
   getDataModel().noteChange();
}



private void setToString(boolean fg)
{
   if (fg == show_tostring) return;
   show_tostring = fg;
   CommandArgs args = new CommandArgs("VALUE",show_tostring);
   BicexExecution exec = getExecution();
   Element rslt = exec.sendSeedeMessage("TOSTRING",args,null);
   BoardLog.logD("BICEX","RESULT OF TOSTRING IS " + IvyXml.convertXmlToString(rslt));
   
   if (show_tostring) split_pane.setRightComponent(tostring_area);
   else split_pane.setRightComponent(null);
   split_pane.setResizeWeight((show_tostring ? 0.75 : 1.0));
}

/********************************************************************************/
/*										*/
/*	Interaction methods							*/
/*										*/
/********************************************************************************/

@Override void handlePopupMenu(JPopupMenu menu,MouseEvent evt)
{
   Point pt = evt.getPoint();
   pt = new Point(pt.x,pt.y);
   int row = value_table.rowAtPoint(pt);
   Object v0 = value_table.getValueAt(row,-1);
   if (v0 == null) return;
   BicexDataModel.AbstractNode an = (BicexDataModel.AbstractNode) v0;
   String name = an.getName();
   if (name == null || name.startsWith("*")) return;
   BicexValue bv = an.getBicexValue();
   if (bv == null) return;
   long now = getExecution().getCurrentTime();
   long prev = -1;
   long next = -1;
   long first = -1;
   for (Integer t : bv.getTimeChanges()) {
      if (first < 0) first = t;
      if (t < now-1) prev = t;
      else if (t > now && next < 0) next = t;
    }
   if (prev > 0) menu.add(getContextTimeAction("Go To Previous Set of " + name,null,prev+1));
   if (next > 0) menu.add(getContextTimeAction("Go To Next Set of " + name,null,next+1));

   boolean prim = an.getChildCount() == 0;
   if (prev > 0 && !bv.isInitializable() && prim) {
      menu.add(new TraceVariableAction(name,an));
    }

   if (first == 0 && !name.contains("@") && bv.isInitializable()) {
      menu.add(new SetInitialValueAction(name,an));
    }
   if (bv.isComponent(now)) {
      menu.add(new CreateGraphicsAction(name,an));
    }
   
   if (!an.isLeaf()) menu.add(new ExpandVariableAction(name,an));
   
   menu.add(new ShowToStringAction());
}



private String getNodeName(BicexDataModel.AbstractNode nd)
{
   String par = null;
   if (nd.getBicexParent() != null) {
      par = getNodeName(nd.getBicexParent());
    }
   String nm = nd.getName();
   if (nd.getContextId() != null) nm = nm + "#" + nd.getContextId();
   if (par != null) nm = par + "?" + nm;

   return nm;
}




/********************************************************************************/
/*										*/
/*	Tree expansion updating (handle dynamic model building) 		*/
/*										*/
/********************************************************************************/

void checkExpand()
{
   JTree tree = value_table.getTree();
   checkExpandNode(null,(TreeNode) tree.getModel().getRoot());
}



private void checkExpandNode(TreePath tp,TreeNode vn)
{
   boolean exp = false;
   JTree tree = value_table.getTree();

   if (tp == null) exp = true;
   else if (vn.getParent() == null) exp = true;
   else if (tree.isExpanded(tp)) exp = true;

   if (exp) {
      if (tp == null) tp = new TreePath(vn);
      else tp = tp.pathByAddingChild(vn);
      for (int i = 0; i < vn.getChildCount(); ++i) {
	 checkExpandNode(tp,vn.getChildAt(i));
       }
    }
}



/********************************************************************************/
/*										*/
/*	Value table								*/
/*										*/
/********************************************************************************/

private class ValueTable extends SwingTreeTable {

   private CellDrawer [] cell_drawer;

   private static final long serialVersionUID = 1;

   ValueTable() {
      super(getDataModel());
      setOpaque(false);
      BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
         TableColumn tc = e.nextElement();
         tc.setHeaderRenderer(new HeaderDrawer(getTableHeader().getDefaultRenderer()));
       }
      cell_drawer = new CellDrawer[2];
      JTree tr = getTree();
      tr.setCellRenderer(new TreeCellRenderer());
   
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

   @Override public void paintComponent(Graphics g) {
      Dimension sz = getSize();
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      Graphics2D g2 = (Graphics2D) g.create();
      Color top = BoardColors.getColor(BICEX_EVAL_TOP_COLOR_PROP);
      Color bot = BoardColors.getColor(BICEX_EVAL_BOTTOM_COLOR_PROP);
      if (top.getRGB() != bot.getRGB()) {
	 Paint p = new GradientPaint(0f,0f,top,0f,sz.height,bot);
	 g2.setPaint(p);
       }
      else {
	 g2.setColor(top);
       }
      g2.fill(r);
      super.paintComponent(g);
    }


   @Override public String getToolTipText(MouseEvent evt) {
      int row = rowAtPoint(evt.getPoint());
      Object v0 = getValueAt(row,-1);
      if (v0 == null) return null;
      BicexDataModel.AbstractNode node = (BicexDataModel.AbstractNode) v0;
      String nm = node.getName();
      if (nm == null) return null;
      BicexValue vl = node.getBicexValue();
      if (vl == null) return null;
      return nm + " = " + vl.getTooltipValue(getExecution().getCurrentTime());
    }

}	// end of inner class ValueTable



/********************************************************************************/
/*										*/
/*	Cell drawing for value table						*/
/*										*/
/********************************************************************************/

private static class CellDrawer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   CellDrawer(TableCellRenderer dflt) {
      default_renderer = dflt;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
         boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      
      cmp.setOpaque(false);
      
      if (v instanceof BicexTreeNode) {
         BicexTreeNode btn = (BicexTreeNode) v;
         if (btn.isUpdatedCurrently()) {
            cmp.setOpaque(true);
          }
       }
      
      return cmp;
    }

}	// end of innerclass CellRenderer



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



private static class TreeCellRenderer extends DefaultTreeCellRenderer {

   private static final long serialVersionUID = 1;

   TreeCellRenderer() {
      setBackgroundNonSelectionColor(null);
      setBackground(BoardColors.transparent());
    }

   @Override public Component getTreeCellRendererComponent(JTree tree,
	 Object value,
	 boolean sel,
	 boolean expanded,
	 boolean leaf,
	 int row,
	 boolean hasfocus) {
      return super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasfocus);
    }

}	// end of inner class TreeCellRenderer








/********************************************************************************/
/*										*/
/*	Handle tree node expand/contract					*/
/*										*/
/********************************************************************************/

private class Expander implements TreeExpansionListener, TreeModelListener, Runnable {

   private Set<String>	expand_set;
   private JTree for_tree;

   Expander(JTree tr) {
      expand_set = new HashSet<String>();
      for_tree = tr;
    }

   @Override public void treeCollapsed(TreeExpansionEvent evt)	{
      BoardMetrics.noteCommand("BICEX","ValueCollapse");
      BicexDataModel.AbstractNode tn = (BicexDataModel.AbstractNode) evt.getPath().getLastPathComponent();
      expand_set.remove(tn.getName());
    }

   @Override public void treeExpanded(TreeExpansionEvent evt)	{
      BoardMetrics.noteCommand("BICEX","ValueExpand");
      BicexDataModel.AbstractNode tn = (BicexDataModel.AbstractNode) evt.getPath().getLastPathComponent();
      expand_set.add(tn.getName());
    }

   @Override public void treeNodesChanged(TreeModelEvent e)	{ }
   @Override public void treeNodesInserted(TreeModelEvent e)	{ }
   @Override public void treeNodesRemoved(TreeModelEvent e)	{ }
   @Override public void treeStructureChanged(TreeModelEvent e) {
      SwingUtilities.invokeLater(this);
    }

   @Override public void run() {
      checkExpand();
    }

   private void checkExpand() {
      checkExpandNode(null,(BicexDataModel.AbstractNode) for_tree.getModel().getRoot());
    }

   private void checkExpandNode(TreePath tp,BicexDataModel.AbstractNode vn) {
      boolean exp = false;

      if (tp == null) exp = true;
      else if (vn.getParent() == null) exp = true;
      else if (expand_set.contains(vn.getName())) exp = true;

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
	    checkExpandNode(tp,(BicexDataModel.AbstractNode) vn.getChildAt(i));
	  }
       }
    }

}	// end of inner class Expander



/********************************************************************************/
/*                                                                              */
/*      Handle selections                                                       */
/*                                                                              */
/********************************************************************************/

private class UserSelection implements ListSelectionListener {
   
   @Override public void valueChanged(ListSelectionEvent e) {
      if (show_tostring) {
         int row = value_table.getSelectedRow();
         String text = "";
         if (row >= 0) {
            Object v0 = value_table.getValueAt(row,-1);
            if (v0 != null) {
               BicexDataModel.AbstractNode an = (BicexDataModel.AbstractNode) v0;
               BicexValue bv = an.getBicexValue();
               long now = getExecution().getCurrentTime();
               text = bv.getStringValue(now);
               if (bv != null && bv.getChildren(now) != null) {
                  BicexValue tsv = bv.getChildren(now).get("@toString");
                  if (tsv != null) text = tsv.getStringValue(now);
                  else if (bv.getDataType(now).endsWith("[]")) {
                     StringBuffer buf = new StringBuffer();
                     buf.append("[");
                     Map<String,BicexValue> chld = bv.getChildren(now);
                     for (int i = 0; i < chld.size(); ++i) {
                        String key = "[" + i + "]";
                        BicexValue cbv = chld.get(key);
                        if (cbv != null) {
                           if (i > 0) buf.append(",");
                           buf.append(cbv.getStringValue(now));
                         }
                      }
                     buf.append("]");
                     text = buf.toString();
                   }
                }
             }
          }
         tostring_area.setText(text);
       }
    }
   
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private class SetInitialValueAction extends AbstractAction {

   private BicexDataModel.AbstractNode variable_slot;

   private static final long serialVersionUID = 1;

   SetInitialValueAction(String name,BicexDataModel.AbstractNode var) {
      super("Set Initial Value of " + name);
      variable_slot = var;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (variable_slot == null) return;
      BoardMetrics.noteCommand("BICEX","SetInitialValue");
      String threadid = getExecution().getEvaluation().getThreadForContext(getContext());
      String expr = JOptionPane.showInputDialog(BicexViewerPanel.this,"Intial value");
      if (expr == null) return;
      expr = expr.trim();
      if (expr.length() == 0) return;
      CommandArgs args = new CommandArgs("THREAD",threadid);
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.cdataElement("EXPR",expr);
      getExecution().sendSeedeMessage("SETVALUE",args,xw.toString());
      xw.close();

    }

}	// end of inner class AbstractAction



private class CreateGraphicsAction extends AbstractAction {

   private BicexDataModel.AbstractNode variable_slot;

   private static final long serialVersionUID = 1;

   CreateGraphicsAction(String name,BicexDataModel.AbstractNode var) {
      super("Create Graphical Viewer for " + name);
      variable_slot = var;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (variable_slot == null) return;
      String name = getNodeName(variable_slot);
      BicexExecution exec = getExecution();
      CommandArgs args = new CommandArgs("VARIABLE",name);
      Element rslt = exec.sendSeedeMessage("SWING",args,null);
      BoardMetrics.noteCommand("BICEX","CreateGraphics");
      BoardLog.logD("BICEX","RESULT OF SWING SETUP IS " + IvyXml.convertXmlToString(rslt));
    }

}	// end of inner class CreateGraphicsAction



private class TraceVariableAction extends AbstractAction implements Runnable {

   private BicexDataModel.AbstractNode variable_slot;
   private BicexVarHistory var_history;

   private static final long serialVersionUID = 1;

   TraceVariableAction(String name,BicexDataModel.AbstractNode var) {
      super("Trace History of " + name);
      variable_slot = var;
      var_history = null;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      var_history = new BicexVarHistory(eval_viewer,
            variable_slot.getBicexValue(),
            getNodeName(variable_slot));
      BoardThreadPool.start(this);
    }

   @Override public void run() {
      var_history.process();
      var_history = null;
    }

}	// end of inner class TraceVariableAction



private class ExpandVariableAction extends AbstractAction {
   
   private BicexDataModel.AbstractNode variable_slot;
   
   private static final long serialVersionUID = 1;
   
   ExpandVariableAction(String name,BicexDataModel.AbstractNode var) {
      super("Show Values of " + name);
      variable_slot = var;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      if (variable_slot == null) return;
      String name = getNodeName(variable_slot);
      BicexExecution exec = getExecution();
      CommandArgs args = new CommandArgs("VARIABLE",name,"TIME",getExecution().getCurrentTime(),
               "CONTEXT",getExecution().getCurrentContext().getId(),
               "FILE",getExecution().getCurrentContext().getFileName());
      Element rslt = exec.sendSeedeMessage("EXPAND",args,null);
      BoardMetrics.noteCommand("BICEX","ExpandVariable");
      BoardLog.logD("BICEX","RESULT OF EXPAND IS " + IvyXml.convertXmlToString(rslt));
    }
   
}	// end of inner class ExpandVariableAction



private class ShowToStringAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   ShowToStringAction() {
      super(show_tostring ? "Hide toString Values" : "Show toString Values");
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      setToString(!show_tostring);
      BoardMetrics.noteCommand("BICEX","ShowToString");
    }
   
}       // end of inner class ShowToStringAction



}

	// end of class BicexViewerBubble




/* end of BicexViewerBubble.java */
