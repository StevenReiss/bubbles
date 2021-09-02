/********************************************************************************/
/*										*/
/*		BddtProcessView.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool process bubble table		*/
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

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardMouser;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.Color;
import java.awt.Component;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;



class BddtProcessView extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpRunModel run_model;
private BumpClient   bump_client;
private BddtProcessView the_view;

private List<BumpProcess> active_processes;

private ProcessModel  process_model;
private ProcessTable  process_table;

private static String [] col_names = new String[] {
   "Project","Config Name","Debug","Terminated"
};

private static Class<?> [] col_types = new Class[] {
   String.class, String.class, String.class, String.class
};


private static int [] col_sizes = new int [] {
   50, 60, 20, 30
};


private static int [] col_max_size = new int [] {
   0, 0, 0, 0
};

private static int [] col_min_size = new int [] {
   12, 12, 12, 12
};


private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtProcessView()
{
   bump_client = BumpClient.getBump();
   run_model = bump_client.getRunModel();
   the_view=this;

   active_processes = new ArrayList<BumpProcess>();
   for (BumpProcess blp : run_model.getProcesses()) {
      active_processes.add(blp);
    }
   process_model = new ProcessModel();
   process_table = new ProcessTable();
   addMouseListener(new ProcessMouser());

   run_model.addRunEventHandler(new ProcessHandler());

   JScrollPane sp = new JScrollPane(process_table);
   sp.setPreferredSize(new Dimension(BDDT_PROCESS_WIDTH,BDDT_PROCESS_HEIGHT));
   setContentPane(sp,null);

   addMouseListener(new FocusOnEntry());
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<BumpProcess> getProcesses()
{
   return active_processes;
}




/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();
   Point pt = SwingUtilities.convertPoint(getContentPane().getParent(), e.getPoint(), process_table);
   pt = new Point(pt.x,pt.y-5);
   int row = process_table.rowAtPoint(pt);
   BumpProcess launch = getActualProcess(row);
   if (launch != null) {
      popup.add(new StopAction(launch));
      popup.add(new ConsoleAction(launch));
      popup.show(process_table, pt.x, pt.y);
    }
}




/********************************************************************************/
/*										*/
/*	Popup menu handlng classes						*/
/*										*/
/********************************************************************************/

private static class StopAction extends AbstractAction {

   private BumpProcess the_launch;
   private static final long serialVersionUID = 1;

   StopAction(BumpProcess c) {
      super("Terminate");
      the_launch = c;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","ProcessTerminate");
      BumpClient c =  BumpClient.getBump();
      c.terminate(the_launch);
    }

   @Override public boolean isEnabled() {
      return the_launch.isRunning();
    }

}	// end of inner class StopAction



private class ConsoleAction extends AbstractAction {

   private BumpProcess the_launch;
   private static final long serialVersionUID = 1;

   ConsoleAction(BumpProcess c) {
      super("Open Console");
      the_launch = c;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","ProcessConsole");
      BddtFactory.getFactory().makeConsoleBubble(the_view,the_launch);
    }

   @Override public boolean isEnabled() {
      return true;
    }

}	// end of inner class ConsoleAction





/********************************************************************************/
/*										*/
/*	Model event handling							*/
/*										*/
/********************************************************************************/

private class ProcessHandler implements BumpRunEventHandler
{
   @Override public void handleLaunchEvent(BumpRunEvent evt)				{ }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      BumpProcess blp;
      switch (evt.getEventType()) {
	 case PROCESS_ADD :
	    blp = evt.getProcess();
	    if (blp != null) {
	       active_processes.add(blp);
	     }
	    break;
	 case PROCESS_REMOVE :
	    blp = evt.getProcess();
	    if (blp != null) {
	       active_processes.remove(blp);
	     }
	    break;
	 case PROCESS_CHANGE :
	    //TODO: Update process
	    break;
	 case PROCESS_PERFORMANCE :
	 case PROCESS_SWING :
	 case PROCESS_TRIE :
	 case PROCESS_TRACE :
	    break;
	 default:
	    break;
       }
      process_model.fireTableDataChanged();
   }

   @Override public void handleThreadEvent(BumpRunEvent evt)				{ }
   @Override public void handleConsoleMessage(BumpProcess bp,boolean e,boolean f,String msg)	{ }

}	// end of inner class ProcessHandler





/********************************************************************************/
/*										*/
/*	Methods to handle table sorting 					*/
/*										*/
/********************************************************************************/

private BumpProcess getActualProcess(int idx)
{
   if (idx < 0) return null;

   synchronized (active_processes) {
      if (process_table != null) {
	 RowSorter<?> rs = process_table.getRowSorter();
	 if (rs != null) idx = rs.convertRowIndexToModel(idx);
       }

      return active_processes.get(idx);
    }
}




/********************************************************************************/
/*										*/
/*	Configuration table model						*/
/*										*/
/********************************************************************************/

private class ProcessModel extends AbstractTableModel {

   private static final long serialVersionUID = 1;

   ProcessModel() { }

   @Override public int getColumnCount()		{ return col_names.length; }
   @Override public String getColumnName(int idx)	{ return col_names[idx]; }
   @Override public Class<?> getColumnClass(int idx)	{ return col_types[idx]; }
   @Override public boolean isCellEditable(int r,int c) {   return false;  }
   @Override public int getRowCount()			{ return active_processes.size(); }

   @Override public Object getValueAt(int r,int c) {
      BumpProcess blp;
      synchronized (active_processes) {
	 if (r < 0 || r >= active_processes.size()) return null;
	 blp = active_processes.get(r);
       }
      switch (c) {
	 case 0 :
	    return blp.getLaunch().getConfiguration().getProject();
	 case 1 :
	    return blp.getLaunch().getConfiguration().getConfigName();
	 case 2 :
	    return blp.getLaunch().isDebug();
	 case 3 :
	    return !blp.isRunning();
       }
      return null;
    }

}	// end of inner class ConfigModel



/********************************************************************************/
/*										*/
/*	Configuration table							*/
/*										*/
/********************************************************************************/

private class ProcessTable extends JTable implements BudaConstants.BudaBubbleOutputer
{
   private CellDrawer [] cell_drawer;

   private static final long serialVersionUID = 1;

   ProcessTable() {
      super(process_model);
      setAutoCreateRowSorter(true);
      fixColumnSizes();
      setIntercellSpacing(new Dimension(2,1));
      setToolTipText("");
      setOpaque(false);
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
	 TableColumn tc = e.nextElement();
	 tc.setHeaderRenderer(new HeaderDrawer(getTableHeader().getDefaultRenderer()));
       }
      cell_drawer = new CellDrawer[col_names.length];
    }

   @Override public TableCellRenderer getCellRenderer(int r,int c) {
      if (cell_drawer[c] == null) {
	 cell_drawer[c] = new CellDrawer(super.getCellRenderer(r,c));;
       }
      return cell_drawer[c];
    }

   private void fixColumnSizes() {
      TableColumnModel tcm = getColumnModel();
      for (int i = 0; i < col_sizes.length; ++i) {
    TableColumn tc = tcm.getColumn(i);
    tc.setPreferredWidth(col_sizes[i]);
    if (col_max_size[i] != 0) tc.setMaxWidth(col_max_size[i]);
    if (col_min_size[i] != 0) tc.setMinWidth(col_min_size[i]);
       }
    }

   @Override protected void paintComponent(Graphics g) {
      synchronized (active_processes) {
	 Dimension sz = getSize();
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 Graphics2D g2 = (Graphics2D) g.create();
         Color tc = BoardColors.getColor(BDDT_PROCESS_TOP_COLOR_PROP);
         Color bc = BoardColors.getColor(BDDT_PROCESS_BOTTOM_COLOR_PROP);
	 if (tc.getRGB() != bc.getRGB()) {
	    Paint p = new GradientPaint(0f,0f,tc,0f,sz.height,bc);
	    g2.setPaint(p);
	  }
	 else {
	    g2.setColor(tc);
	  }
	 g2.fill(r);
	 super.paintComponent(g);
       }
    }

   @Override public String getConfigurator()			{ return "BDDT"; }
   @Override public void outputXml(BudaXmlWriter xw)		{ }

}	// end of inner class ProcessTable




/********************************************************************************/
/*                                                                              */
/*      Mouse Handlers                                                          */
/*                                                                              */
/********************************************************************************/

private class ProcessMouser extends BoardMouser {
   
   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
         int row = process_table.rowAtPoint(e.getPoint());
         BumpProcess blp = getActualProcess(row);
         if (blp == null) return;
         System.err.println("START CONFIGURATION " + blp.getId());
       }
    }
}

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

}	// end of inner class ErrorRenderer



}	// end of class BddtProcessView




/* end of BddtProcessView.java */
