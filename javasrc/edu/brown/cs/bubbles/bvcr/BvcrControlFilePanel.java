/********************************************************************************/
/*										*/
/*		BvcrControlFilePanel.java					*/
/*										*/
/*	Bubble showing file status wrt versioning				*/
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingKey;
import edu.brown.cs.ivy.swing.SwingTreeTable;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class BvcrControlFilePanel extends BudaBubble implements BvcrConstants,
		BvcrConstants.BvcrProjectUpdated, BudaBubble.BudaBubbleOutputer
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BvcrControlPanel	control_panel;
private FileTable		output_table;
private DirectoryNode		root_node;
private FileTableModel		table_model;
private boolean 		show_untracked;
private boolean 		show_ignored;
private List<JButton>		select_buttons;
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrControlFilePanel(BvcrControlPanel pnl)
{
   control_panel = pnl;
   show_untracked = true;
   show_ignored = false;

   select_buttons = new ArrayList<JButton>();

   root_node = new DirectoryNode(pnl.getRootDirectory());
   table_model = new FileTableModel();
   output_table = new FileTable();
   SwingGridPanel fpnl = new SwingGridPanel();
   fpnl.beginLayout();
   fpnl.addLabellessRawComponent("table",new JScrollPane(output_table));
   fpnl.addSeparator();

   ShowButtons action = new ShowButtons();
   fpnl.addBottomToggle("Show Untracked","SHOWUNTRACKED",show_untracked,action);
   fpnl.addBottomToggle("Show Ingored","SHOWIGNORED",show_ignored,action);
   fpnl.addBottomButtons();

   FileStateUpdate updater = new FileStateUpdate();
   JButton b1 = fpnl.addBottomButton("Ignore","IGNORE",updater);
   b1.setEnabled(false);
   select_buttons.add(b1);
   JButton b2 = fpnl.addBottomButton("Delete","DELETE",updater);
   b2.setEnabled(false);
   select_buttons.add(b2);
   JButton b3 = fpnl.addBottomButton("Add","ADD",updater);
   b3.setEnabled(false);
   select_buttons.add(b3);
   fpnl.addBottomButton("Add All","ADDALL",updater);
   fpnl.addBottomButtons();

   SwingKey.registerKeyAction("BVCR",this,new ExpandAllAction(),"F4");
   
   setContentPane(fpnl);

   pnl.addUpdateListener(this);

   projectUpdated(pnl);
}


@Override protected void localDispose()
{
   control_panel.removeUpdateListener(this);
}



static void registerKeys()
{
   SwingKey.registerKeyAction("BVCR","Expand All","F4");
}



/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

@Override public void projectUpdated(BvcrControlPanel pnl)
{
   updateAll();
   if (table_model != null) table_model.rootUpdated();

   expandAll();

   output_table.repaint();
}



private void updateAll()
{
   Set<BvcrControlFileStatus> files = control_panel.getFiles();
   root_node.clear();

   for (BvcrControlFileStatus sts : files) {
      File fnm = new File(sts.getFileName());
      if (!show_ignored && sts.getFileState() == FileState.IGNORED) continue;
      if (!show_untracked && sts.getFileState() == FileState.UNTRACKED) continue;
      FileNode newnd = new FileNode(sts);
      root_node.insert(fnm,newnd);
    }
}



/********************************************************************************/
/*										*/
/*	Bubble actions								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();
   menu.add(getFloatBubbleAction());
   menu.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Table actions								*/
/*										*/
/********************************************************************************/





private void expandAll()
{
   JTree tr = output_table.getTree();
   for (int i = 0; i < tr.getRowCount(); ++i) {
      try {
	 tr.expandRow(i);
       }
      catch (Throwable t) {
	 BoardLog.logE("BVCR","Problem setting up bvcr control panel",t);
       }
    }

   try {
      if (tr.getRowCount() > 0) tr.expandRow(0);
    }
   catch (Throwable t) { }
}



private class ExpandAllAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ExpandAllAction() {
      super("Expand All");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BVCR","ExpandAllFiles");
      expandAll();
    }

}	// end of inner class ExpandAllAction




/********************************************************************************/
/*										*/
/*	File table								*/
/*										*/
/********************************************************************************/

private class FileTable extends SwingTreeTable {

   private CellDrawer [] cell_drawer;
   private static final long serialVersionUID = 1;
   
   FileTable() {
      super(table_model);
      setPreferredScrollableViewportSize(new Dimension(400,300));
      setRowSelectionAllowed(true);
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      getSelectionModel().addListSelectionListener(new TableSelectionListener());
      cell_drawer = new CellDrawer[table_model.getColumnCount()];
      JTree tr = getTree();
      tr.setCellRenderer(new TreeCellRenderer());
      setToolTipText("");
    }

   @Override public TableCellRenderer getCellRenderer(int r,int c) {
      if (cell_drawer[c] == null) {
	 cell_drawer[c] = new CellDrawer(super.getCellRenderer(r,c));
       }
      return cell_drawer[c];
    }

   @Override public void paint(Graphics g) {
      super.paint(g);
    }

   @Override protected void paintComponent(Graphics g) {
      super.paintComponent(g);
    }

   @Override public String getToolTipText(MouseEvent evt) {
      int row = rowAtPoint(evt.getPoint());
      Object v0 = getValueAt(row,-1);
      if (v0 == null) return null;
      if (v0 instanceof GenericNode) {
	 GenericNode gn = (GenericNode) v0;
	 return gn.getRelativeName();
       }
      return null;
    }



}	// end of inner class FileTable



private class TableSelectionListener implements ListSelectionListener {

   @Override public void valueChanged(ListSelectionEvent e) {
      boolean enable = false;
      for (int r : output_table.getSelectedRows()) {
         Object o = output_table.getValueAt(r,-1);
         if (o != null && o instanceof FileNode) enable = true;
       }
      for (JButton jb : select_buttons) {
         jb.setEnabled(enable);
       }
    }

}	// end of inner class TableSelectionListener





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
      Color bkg = null;
      if (value instanceof FileNode) {
	 FileNode fn = (FileNode) value;
	 switch (fn.getFileStatus()) {
	    case ADDED :
	       bkg = BoardColors.getColor("Bvcr.ControlFileAdded");
	       break;
	    case DELETED :
	       bkg = BoardColors.getColor("Bvcr.ControlFileDeleted");
	       break;
	    case IGNORED :
	       bkg = BoardColors.getColor("Bvcr.ControlFileIgnored");
	       break;
	    case MODIFIED :
	       bkg = BoardColors.getColor("Bvcr.ControlFileModified");
	       break;
	    default :
	       break;
	 }
      }
      else if (value instanceof DirectoryNode) {
      }

      if (bkg != null) setBackground(bkg);
      else setBackground(BoardColors.transparent());

      return super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasfocus);
   }

}	// end of inner class TreeCellRenderer





/********************************************************************************/
/*										*/
/*	Tree Table Model							*/
/*										*/
/********************************************************************************/

private class FileTableModel extends SwingTreeTable.AbstractTreeTableModel {

   private final String [] column_names = {
      "File","Status"
    };

   FileTableModel() {
      super(root_node);
    }

   void rootUpdated() {
      Object [] path = new Object [] { root_node };
      int nchild = root_node.getChildCount();
      Object [] chld = new Object[nchild];
      int [] cidx = new int[nchild];
      for (int i = 0; i < nchild; ++i) {
	 cidx[i] = i;
	 chld[i] = root_node.getChild(i);
       }
      fireTreeStructureChanged(BvcrControlFilePanel.this,path,cidx,chld);
    }

   @Override public Object getChild(Object par,int idx) {
      GenericNode gn = (GenericNode) par;
      return gn.getChild(idx);
    }

   @Override public int getChildCount(Object par) {
      GenericNode gn = (GenericNode) par;
      return gn.getChildCount();
    }

   @Override public int getColumnCount()		{ return column_names.length; }

   @Override public String getColumnName(int col)	{ return column_names[col]; }

   @Override public Object getValueAt(Object node,int col) {
      GenericNode gn = (GenericNode) node;
      if (gn == null) return null;
      switch (col) {
	 case -1 :
	    return gn;
	 case 0 :
	    return gn.getRelativeName();
	 case 1 :
	    if (gn.getFileStatus() == null) return null;
	    String sts = gn.getFileStatus().toString();
	    if (gn.getPushStatus() == Boolean.TRUE) sts += "*";
	    return sts;
       }
      return null;
    }


}	// end of inner class FileTableModel




/********************************************************************************/
/*										*/
/*	Generic node (file/directory)						*/
/*										*/
/********************************************************************************/

private static abstract class GenericNode {

   private DirectoryNode	   parent_node;

   GenericNode() {
      parent_node = null;
    }

   protected void setParent(DirectoryNode dn)	{ parent_node = dn; }

   abstract File getFile();
   GenericNode insert(File f,GenericNode gn)	{ return null; }

   GenericNode getChild(int idx)		{ return null; }
   int getChildCount()				{ return 0; }

   String getRelativeName() {
      if (parent_node != null) {
	 return IvyFile.getRelativePath(parent_node.getFile(),getFile());
       }
      return getFile().getPath();
    }

   FileState getFileStatus()			{ return null; }
   Boolean getPushStatus()			{ return null; }

   @Override public String toString() {
      return getRelativeName();
    }

}	// end of inner class GenericNode






/********************************************************************************/
/*										*/
/*	FileNode -- leaf node for a particular file				*/
/*										*/
/********************************************************************************/

private static class FileNode extends GenericNode {

   private BvcrControlFileStatus file_status;
   private File 		 file_name;

   FileNode(BvcrControlFileStatus sts) {
      file_status = sts;
      file_name = new File(file_status.getFileName());
    }

   @Override File getFile()			{ return file_name; }
   FileState getFileStatus()			{ return file_status.getFileState(); }
   Boolean getPushStatus()			{ return file_status.getUnpushed(); }

}	// end of inner class FileNode



/********************************************************************************/
/*										*/
/*	DirectoryNode -- internal node for a set of files			*/
/*										*/
/********************************************************************************/

private static class DirectoryNode extends GenericNode {

   private File 	      directory_name;
   private SortedSet<GenericNode>  child_nodes;
   private List<GenericNode>	child_list;

   DirectoryNode(File nm) {
      directory_name = nm;
      child_nodes = new TreeSet<GenericNode>(new TreeComparator(directory_name));
      child_list = null;
    }

   @Override File getFile()			{ return directory_name; }

   synchronized GenericNode insert(File f,GenericNode add) {
      child_list = null;
      for (GenericNode gn : child_nodes) {
	 File child = gn.getFile();
	 if (f.equals(child)) return gn;
	 File f2 = IvyFile.getCommonParent(f,child);
	 if (f2 == null) continue;
	 if (f2.equals(child)) return gn.insert(f,add);
	 if (f2.equals(directory_name)) continue;
	 if (f2.getPath().length() < child.getPath().length()) {
	    GenericNode newpar = new DirectoryNode(f2);
	    child_nodes.remove(gn);
	    child_nodes.add(newpar);
	    newpar.setParent(this);
	    newpar.insert(child,gn);
	    return newpar.insert(f,add);
	  }
       }
      child_nodes.add(add);
      add.setParent(this);
      return add;
    }

   synchronized void clear() {
      child_list = null;
      child_nodes.clear();
    }

   synchronized int getChildCount() {
      setupChildList();
      return child_list.size();
   }

   synchronized GenericNode getChild(int idx) {
      setupChildList();
      if (idx < 0 || idx >= child_list.size()) return null;
      return child_list.get(idx);
    }

   private void setupChildList() {
      if (child_list == null) {
	 child_list = new ArrayList<GenericNode>(child_nodes);
      }
   }

}	// end of inner class DirectoryNode



private static class TreeComparator implements Comparator<GenericNode> {

   private File 	base_file;

   TreeComparator(File base) {
      base_file = base;
    }

   @Override public int compare(GenericNode g1,GenericNode g2) {
      String p1 = IvyFile.getRelativePath(g1.getFile(),base_file);
      if (p1 == null) return -1;
      String p2 = IvyFile.getRelativePath(g2.getFile(),base_file);
      if (p2 == null) return 1;
      return p1.compareTo(p2);
    }

}	// end of inner class TreeComparator



/********************************************************************************/
/*										*/
/*	Button action methods							*/
/*										*/
/********************************************************************************/

private class ShowButtons implements ChangeListener {

   @Override public void stateChanged(ChangeEvent e) {
      JToggleButton btn = (JToggleButton) e.getSource();
      boolean state = btn.isSelected();
      String act = btn.getActionCommand();
      if (act.equals("SHOWUNTRACKED")) {
         if (show_untracked == state) return;
         show_untracked = state;
       }
      else if (act.equals("SHOWIGNORED")) {
         if (show_ignored == state) return;
         show_ignored = state;
         control_panel.setIncludeIgnored(show_ignored);
       }
      projectUpdated(control_panel);
    }

}	// end of inner class ShowButtons



private class FileStateUpdate implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = null;
      String actcmd = evt.getActionCommand();
      List<FileNode> workon = new ArrayList<FileNode>();
      int [] rows = output_table.getSelectedRows();
      if (rows == null || rows.length == 0) {
         if (!actcmd.equals("ADDALL")) return;
         int ct = output_table.getRowCount();
         List<Integer> addrows = new ArrayList<Integer>();
         for (int r = 0; r < ct; ++r) {
            Object o = output_table.getValueAt(r,-1);
            if (o == null) continue;
            if (!(o instanceof FileNode)) continue;
            FileNode fn = (FileNode) o;
            switch (fn.getFileStatus()) {
               case UNTRACKED :
        	  addrows.add(r);
        	  break;
               default :
        	  break;
             }
          }
         rows = new int [addrows.size()];
         for (int i = 0; i < rows.length; ++i) rows[i] = addrows.get(i);
         actcmd = "ADD";
       }
      for (int r : rows) {
         Object o = output_table.getValueAt(r,-1);
         if (o == null) continue;
         if (!(o instanceof FileNode)) continue;
         FileNode fn = (FileNode) o;
         switch (actcmd) {
            case "IGNORE" :
               cmd = "FILEIGNORE";
               if (fn.getFileStatus() == FileState.IGNORED) continue;
               break;
            case "DELETE" :
               cmd = "FILERM";
               if (fn.getFileStatus() == FileState.DELETED) continue;
               break;
            case "ADD" :
               cmd = "FILEADD";
               switch (fn.getFileStatus()) {
        	  case ADDED :
        	  case COPIED :
        	  case MODIFIED :
        	  case RENAMED :
        	  case UNMERGED :
        	  case UNMODIFIED :
        	     continue;
        	  case DELETED :
        	  case UNTRACKED :
        	  case IGNORED :
        	  default :
        	     break;
        	}
               break;
          }
         workon.add(fn);
       }
      if (workon.isEmpty() || cmd == null) return;
   
      MintControl mc = BoardSetup.getSetup().getMintControl();
      MintDefaultReply rply = new MintDefaultReply();
      String xcmd = "<BVCR DO='" + cmd + "' PROJECT='" + control_panel.getProject() + "'>";
      for (FileNode fn : workon) {
         xcmd += "<FILE NAME='" + fn.getFile().getAbsolutePath() + "'/>";
       }
      xcmd += "</BVCR>";
      mc.send(xcmd,rply,MintConstants.MINT_MSG_FIRST_NON_NULL);
      String cnts = rply.waitForString();
      BoardLog.logD("BVCR","Reply from BVCR " + cmd + ": " + cnts);
      control_panel.startUpdate();
    }

}	// end of inner class FileStateUpdate




/********************************************************************************/
/*										*/
/*	Configurator methods							*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BVCR"; }

@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","FILECONTROL");
   xw.field("PROJECT",control_panel.getProject());
}




}	// end of class BvcrControlFilePanel




/* end of BvcrControlFilePanel.java */
