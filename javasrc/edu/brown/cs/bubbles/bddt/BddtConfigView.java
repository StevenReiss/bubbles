/********************************************************************************/
/*										*/
/*		BddtConfigView.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool configurations viewer 	*/
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
import edu.brown.cs.bubbles.buda.BudaCursorManager;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;



class BddtConfigView extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpRunModel	run_model;
private BumpClient	bump_client;
private ConfigModel	config_model;
private ConfigTable	config_table;


private List<BumpLaunchConfig> active_configs;


private static String [] col_names = new String[] {
   "Name","Project","Main Class","Arguments"
};

private static Class<?> [] col_types = new Class[] {
   String.class, String.class, String.class, String.class
};


private static int [] col_sizes = new int [] {
   120, 80, 120, 120
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

BddtConfigView()
{
   bump_client = BumpClient.getBump();
   run_model = bump_client.getRunModel();

   active_configs = new ArrayList<BumpLaunchConfig>();
   for (BumpLaunchConfig blc : run_model.getLaunchConfigurations()) {
      if (!blc.isWorkingCopy()) active_configs.add(blc);
    }

   config_model = new ConfigModel();
   config_table = new ConfigTable();
   addMouseListener(new ConfigMouser());

   run_model.addRunEventHandler(new ModelHandler());

   JScrollPane sp = new JScrollPane(config_table);
   sp.setPreferredSize(new Dimension(BDDT_CONFIG_WIDTH,BDDT_CONFIG_HEIGHT));
   BudaCursorManager.setCursor(sp,Cursor.getDefaultCursor());

   setContentPane(sp,null);

   addMouseListener(new FocusOnEntry());
}





/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();
   Point pt = SwingUtilities.convertPoint(getContentPane().getParent(), e.getPoint(), config_table);
   pt = new Point(pt.x,pt.y-5);
   int row = config_table.rowAtPoint(pt);

   BumpLaunchConfig launch = getActualConfig(row);
   if (launch != null) {
      popup.add(new RunAction(launch));
      popup.add(new DebugAction(launch));
      popup.add(new JPopupMenu.Separator());
    }
   popup.add(new NewAction());

   popup.show(config_table, pt.x, pt.y);
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();

   g.setColor(BoardColors.getColor(BDDT_CONFIG_TOP_COLOR_PROP));
   g.fillRect(0,0,sz.width,sz.height);
}



/********************************************************************************/
/*										*/
/*	Sorting methods 							*/
/*										*/
/********************************************************************************/

BumpLaunchConfig getActualConfig(int idx)
{
   if (idx < 0 || active_configs.size() == 0) return null;

   synchronized (active_configs) {
      if (config_table != null) {
	 RowSorter<?> rs = config_table.getRowSorter();
	 if (rs != null) idx = rs.convertRowIndexToModel(idx);
      }

      return active_configs.get(idx);
    }
}



/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

private String getToolTip(BumpLaunchConfig blc)
{
   return "Launch Configuration " + blc.getConfigName();
}




/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

private static class RunAction extends AbstractAction {

   private BumpLaunchConfig the_launch;
   private static final long serialVersionUID = 1;

   RunAction(BumpLaunchConfig c) {
      super("Run [" + c.getConfigName()+"]");
      the_launch = c;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient c =  BumpClient.getBump();
      c.startRun(the_launch);
      BoardMetrics.noteCommand("BDDT","StartDebug");
    }

   @Override public boolean isEnabled() 		{ return true; }

}	// end of inner class RunAction



private static class DebugAction extends AbstractAction {

   private BumpLaunchConfig the_launch;
   private static final long serialVersionUID = 1;

   DebugAction(BumpLaunchConfig c) {
      super("Debug ["+c.getConfigName()+"]");
      the_launch = c;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient c =  BumpClient.getBump();
      //TODO:  This should create a launch control instead of doing the launch
      BoardMetrics.noteCommand("BDDT","StartDebug");
      c.startDebug(the_launch,null);
    }

   @Override public boolean isEnabled() 		{ return true; }

}	// end of inner class DebugAction




private static class NewAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   NewAction() {
      super("New configuration...");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","NewLaunchConfiguration");
    }

}	// end of inner class NewAction




/********************************************************************************/
/*										*/
/*	Model event handling							*/
/*										*/
/********************************************************************************/

private final class ModelHandler implements BumpConstants.BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt) {
      switch (evt.getEventType()) {
	 case LAUNCH_ADD :
	 case LAUNCH_CHANGE :
	    if (!evt.getLaunchConfiguration().isWorkingCopy()) {
	       active_configs.add(evt.getLaunchConfiguration());
	     }
	    break;
	 case LAUNCH_REMOVE :
	    active_configs.remove(evt.getLaunchConfiguration());
	    break;
	 default:
	    break;
       }
      config_model.fireTableDataChanged();
    }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      // might want to track number of processes for each launch configuration
    }

}	// end of inner class ModelHandler




/********************************************************************************/
/*										*/
/*	Configuration table model						*/
/*										*/
/********************************************************************************/

private class ConfigModel extends AbstractTableModel {

   private static final long serialVersionUID = 1;

   ConfigModel() { }

   @Override public int getColumnCount()		{ return col_names.length; }
   @Override public String getColumnName(int idx)	{ return col_names[idx]; }
   @Override public Class<?> getColumnClass(int idx)	{ return col_types[idx]; }
   @Override public boolean isCellEditable(int r,int c) {
      if (c==0||c==2||c==3) return true;
      return false;
   }
   @Override public int getRowCount()			{ return active_configs.size(); }

   @Override public Object getValueAt(int r,int c) {
      BumpLaunchConfig blc;
      synchronized (active_configs) {
         if (r < 0 || r >= active_configs.size()) return null;
         blc = active_configs.get(r);
       }
      switch (c) {
         case 0 :
            return blc.getConfigName();
         case 1 :
            return blc.getProject();
         case 2 :
            return blc.getMainClass();
         case 3 :
            return blc.getArguments();
       }
      return null;
    }

}	// end of inner class ConfigModel



/********************************************************************************/
/*										*/
/*	Configuration table							*/
/*										*/
/********************************************************************************/

private class ConfigTable extends JTable implements BudaConstants.BudaBubbleOutputer
{
   private CellDrawer [] cell_drawer;

   private static final long serialVersionUID = 1;

   ConfigTable() {
      super(config_model);
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

   @Override public String getToolTipText(MouseEvent e) {
      int r = rowAtPoint(e.getPoint());
      if (r < 0) return null;
      BumpLaunchConfig blc = getActualConfig(r);
      return getToolTip(blc);
    }

   @Override protected void paintComponent(Graphics g) {
      synchronized (active_configs) {
	 Dimension sz = getSize();
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 Graphics2D g2 = (Graphics2D) g.create();
	 Color tc = BoardColors.getColor(BDDT_CONFIG_TOP_COLOR_PROP);
	 Color bc = BoardColors.getColor(BDDT_CONFIG_BOTTOM_COLOR_PROP);
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
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","CONFIGS");
    }

}	// end of inner class ConfigTable


private final class ConfigMouser extends BoardMouser {
   
   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
         int row = config_table.rowAtPoint(e.getPoint());
         BumpLaunchConfig blc = getActualConfig(row);
         if (blc == null) return;
         System.err.println("START CONFIGURATION " + blc.getConfigName());
       }
    }
   
}       // end of inner class ConfigMouser





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




}	// end of class BddtConfigView




/* end of BddtConfigView.java */



