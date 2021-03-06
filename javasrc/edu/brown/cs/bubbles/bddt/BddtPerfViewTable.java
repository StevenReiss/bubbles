/********************************************************************************/
/*										*/
/*		BddtPerfViewTable.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool performance bubble		*/
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

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

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
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



class BddtPerfViewTable extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	launch_control;
private PerfTable		perf_table;
private PerfModel		perf_model;
private PerfEventHandler	event_handler;
private double			base_samples;
private double			total_samples;
private double			base_time;
private Map<String,PerfNode>	node_map;

private static final String [] column_names = new String[] {
   "Package", "Class", "Method", "Line", "Base Time", "Base %", "Total Time", "Total %"
};

private static final Class<?> [] column_class = new Class<?>[] {
   String.class, String.class, String.class, Integer.class, Double.class, Double.class,
   Double.class, Double.class
};





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtPerfViewTable(BddtLaunchControl ctrl)
{
   launch_control = ctrl;
   base_samples = 1;
   total_samples = 1;
   base_time = 0;
   node_map = new HashMap<String,PerfNode>();

   setupBubble();
}



@Override protected void localDispose()
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
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupBubble()
{
   BumpClient bc = BumpClient.getBump();
   BumpRunModel rm = bc.getRunModel();
   event_handler = new PerfEventHandler();
   rm.addRunEventHandler(event_handler);

   perf_model = new PerfModel();
   perf_table = new PerfTable(perf_model);

   perf_table.addMouseListener(new ClickHandler());

   JScrollPane sp = new JScrollPane(perf_table);
   sp.setPreferredSize(new Dimension(BDDT_PERF_WIDTH,BDDT_PERF_HEIGHT));

   JPanel pnl = new JPanel(new BorderLayout());
   pnl.add(sp,BorderLayout.CENTER);

   setContentPane(pnl,null);
   perf_table.addMouseListener(new FocusOnEntry());
}




/********************************************************************************/
/*										*/
/*	Popup menu and mouse handlers						*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();

   popup.add(getFloatBubbleAction());

   popup.show(this,e.getX(),e.getY());
}



private class ClickHandler extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
	 int r = perf_table.rowAtPoint(e.getPoint());
	 if (r < 0) return;
	 PerfNode pn = perf_table.getActualNode(r);
	 if (pn == null) return;
	 String fct = pn.getPackage();
	 if (fct != null) fct += "." + pn.getClassName();
	 else fct = pn.getClassName();
	 fct += "." + pn.getMethodName();
	 int lno = pn.getLineNumber();
	 String file = pn.getFileName();

	 BudaBubble bb = null;

	 BaleFactory bf = BaleFactory.getFactory();
	 if (lno > 0 && file != null) {
            FileSystemView fsv = BoardFileSystemView.getFileSystemView();
	    File f = fsv.createFileObject(file);
	    if (launch_control.fileExists(f)) {
	       BaleConstants.BaleFileOverview bfo = bf.getFileOverview(null,f);
	       if (bfo != null) {
		  int loff = bfo.findLineOffset(lno);
		  int eoff = bfo.mapOffsetToEclipse(loff);
		  BassFactory bsf = BassFactory.getFactory();
		  BassName bn = bsf.findBubbleName(f,eoff);
		  if (bn != null) bb = bn.createBubble();
	       }
	     }
	  }
	 if (bb == null) {
	    bb = bf.createMethodBubble(launch_control.getProject(), fct);
	  }

	 if (bb == null) return;
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(perf_table);
	 if (bba != null) {
	    bba.addBubble(bb,BddtPerfViewTable.this,null,PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
	  }
       }
    }

}	// end of inner class ClickHandler




/********************************************************************************/
/*										*/
/*	Run Event Handler							*/
/*										*/
/********************************************************************************/

private class PerfEventHandler implements BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt)		{ }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      if (evt.getProcess() != null &&
	     evt.getProcess() != launch_control.getProcess())
	 return;
      else if (evt.getProcess() == null &&
		  evt.getEventType() != BumpRunEventType.PROCESS_ADD)
	 return;

      switch (evt.getEventType()) {
	 case PROCESS_ADD :
	    perf_model.clear();
	    break;
	 case PROCESS_PERFORMANCE :
	    Element xml = (Element) evt.getEventData();
	    base_samples = IvyXml.getAttrDouble(xml,"ACTIVE",0);
	    total_samples = IvyXml.getAttrDouble(xml,"SAMPLES",0);
	    base_time = IvyXml.getAttrDouble(xml,"TIME",0);
	    for (Element itm : IvyXml.children(xml,"ITEM")) {
	       PerfNode pn = findNode(IvyXml.getAttrString(itm,"NAME"));
	       pn.update(itm);
	     }
	    break;
	 case PROCESS_TRIE :
	    xml = (Element) evt.getEventData();
	    break;
	 default:
	    break;
       }
    }

   @Override public void handleThreadEvent(BumpRunEvent evt)		{ }

   @Override public void handleConsoleMessage(BumpProcess bp,boolean err,boolean eof,String msg)	{ }

}	// end of inner class PerfEventHandler




/********************************************************************************/
/*										*/
/*	Table implementation							*/
/*										*/
/********************************************************************************/

private class PerfTable extends JTable implements BudaConstants.BudaBubbleOutputer {

   private CellDrawer [] cell_drawer;

   PerfTable(PerfModel mdl) {
      super(mdl);
      setOpaque(false);
      BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
	 TableColumn tc = e.nextElement();
	 tc.setHeaderRenderer(new HeaderDrawer(getTableHeader().getDefaultRenderer()));
       }
      cell_drawer = new CellDrawer[getColumnModel().getColumnCount()];
      setToolTipText("");
      setAutoCreateRowSorter(true);
    }

   @Override public TableCellRenderer getCellRenderer(int r,int c) {
      if (cell_drawer[c] == null) {
	 cell_drawer[c] = new CellDrawer(super.getCellRenderer(r,c));
       }
      return cell_drawer[c];
    }

   @Override protected void paintComponent(Graphics g) {
      Dimension sz = getSize();
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      Graphics2D g2 = (Graphics2D) g.create();
      Color top = BoardColors.getColor(BDDT_PERF_TOP_COLOR_PROP);
      Color bot = BoardColors.getColor(BDDT_PERF_BOTTOM_COLOR_PROP);
      if (top.getRGB() != bot.getRGB()) {
	 Paint p = new GradientPaint(0f,0f,top,0f,sz.height,bot);
	 g2.setPaint(p);
       }
      else {
	 g2.setColor(top);
       }
      g2.fill(r);
      perf_model.lock();
      try {
	 super.paintComponent(g);
       }
      finally { perf_model.unlock(); }
    }

   @Override public String getConfigurator()		{ return "BDDT"; }

   @Override public void outputXml(BudaXmlWriter xw)	{ }

   PerfNode getActualNode(int row) {
      RowSorter<?> rs = getRowSorter();
      if (rs != null) row = rs.convertRowIndexToModel(row);
      return perf_model.getNode(row);
    }

   @Override public String getToolTipText(MouseEvent e) {
      int r = rowAtPoint(e.getPoint());
      if (r < 0) return null;
      PerfNode pn = getActualNode(r);
      if (pn == null) return null;
      return pn.getToolTip();
   }

}	// end of inner class PerfTable




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
      Class<?> cc = column_class[c];
      if (v != null && !cc.isAssignableFrom(v.getClass())) {
	 System.err.println("Handle bad object");
       }

      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of inner class ErrorRenderer



/********************************************************************************/
/*										*/
/*	Tree model								*/
/*										*/
/********************************************************************************/

private class PerfModel extends AbstractTableModel {

   private Lock 	model_lock;
   private List<PerfNode> node_set;
   private Map<PerfNode,Integer> index_set;


   PerfModel() {
      model_lock = new ReentrantLock();
      node_set = new ArrayList<PerfNode>();
      index_set = new HashMap<PerfNode,Integer>();
    }

   void clear() {
      lock();
      int ln = node_set.size();
      node_set = new ArrayList<PerfNode>();
      index_set = new HashMap<PerfNode,Integer>();
      unlock();
      if (ln > 0) fireTableRowsDeleted(0,ln-1);
    }

   void lock()					{ model_lock.lock(); }
   void unlock()				{ model_lock.unlock(); }

   @Override public int getColumnCount() {
      return column_names.length;
    }

   @Override public int getRowCount()		{ return node_set.size(); }


   @Override public String getColumnName(int col) {
      return column_names[col];
    }

   @Override public Class<?> getColumnClass(int col) {
      return column_class[col];
    }

   @Override public Object getValueAt(int row,int col) {
      PerfNode pn = getNode(row);
      if (pn == null) return null;
      switch (col) {
	 case 0 :			// package
	    return pn.getPackage();
	 case 1 :			// class
	    return pn.getClassName();
	 case 2 :			// method
	    return pn.getMethodName();
	 case 3 :			// line
	    return pn.getLineNumber();
	 case 4 :
	    if (row == 1) System.err.println("BASE: " + pn.getBaseCount());
	    return pn.getBaseCount()* base_time/base_samples;
	 case 5 :
	    return 100 * pn.getBaseCount() / base_samples;
	 case 6 :
	    return pn.getTotalCount() * base_time/base_samples;
	 case 7 :
	    return 100 * pn.getTotalCount() / total_samples;
       }
      return null;
    }

   PerfNode getNode(int row) {
      if (row < 0 || row >= node_set.size()) return null;
      return node_set.get(row);
    }

   void addNode(PerfNode pn) {
      lock();
      int ln = node_set.size();
      node_set.add(pn);
      index_set.put(pn,ln);
      unlock();
      fireTableRowsInserted(ln,ln);
    }

   void handleChange(PerfNode pn) {
      Integer iv = index_set.get(pn);
      if (iv != null) fireTableRowsUpdated(iv,iv);
    }

}	// end of inner class PerfModel




/********************************************************************************/
/*										*/
/*	Performance nodes							*/
/*										*/
/********************************************************************************/

private PerfNode findNode(String nm)
{
   synchronized (node_map) {
      PerfNode pn = node_map.get(nm);
      if (pn == null) {
	 pn = new PerfNode(nm);
	 node_map.put(nm,pn);
	 SwingUtilities.invokeLater(new NodeAdder(pn));
	 // perf_model.addNode(pn);
       }
      return pn;
    }
}



private class NodeAdder implements Runnable {

   private PerfNode perf_node;

   NodeAdder(PerfNode pn) {
      perf_node = pn;
    }

   @Override public void run() {
      perf_model.addNode(perf_node);
    }

}	// end of inner class NodeAdder



private class PerfNode implements Comparable<PerfNode> {

   private String full_name;
   private String package_name;
   private String class_name;
   private String method_name;
   private int line_number;
   private String file_name;
   private int base_count;
   private int total_count;

   PerfNode(String nm) {
      full_name = nm;
      String [] data = nm.split("@");
      int idx = data[0].lastIndexOf(".");
      if (idx >= 0) {
	 package_name = data[0].substring(0,idx);
	 class_name = data[0].substring(idx+1);
       }
      else {
	 package_name = null;
	 class_name = data[0];
       }
      method_name = data[1];
      if (data[2].length() > 0) {
	 line_number = Integer.parseInt(data[2]);
       }
      if (data.length >= 4) file_name = data[3];
      else file_name = null;

      base_count = 0;
      total_count = 0;
    }

   String getPackage()				{ return package_name; }
   String getClassName()			{ return class_name; }
   String getMethodName()			{ return method_name; }
   int getLineNumber()				{ return line_number; }
   String getFileName() 			{ return file_name; }

   void update(Element xml) {
      int bc = IvyXml.getAttrInt(xml,"BASE",0);
      int tc = IvyXml.getAttrInt(xml,"TOTAL",0);
      synchronized (this) {
         if (bc > base_count) base_count = bc;
         if (tc > total_count) total_count = tc;
       }
      SwingUtilities.invokeLater(new ChangeHandler(this));
      // perf_model.handleChange(this);
    }

   int getBaseCount()			{ return base_count; }
   int getTotalCount()			{ return total_count; }

   @Override public int compareTo(PerfNode pn) {
       return full_name.compareTo(pn.full_name);
    }

   String getToolTip() {
      StringBuilder buf = new StringBuilder();
      buf.append("<html><table>");
      if (package_name != null) {
	 buf.append("<tr><td>Package</td><td>");
	 buf.append(package_name);
	 buf.append("</td></tr>");
       }
      buf.append("<tr><td>Class</td><td>");
      buf.append(class_name);
      buf.append("</td></tr>");
      buf.append("<tr><td>Method</td><td>");
      buf.append(method_name);
      if (line_number > 0) {
	 buf.append("</td></tr>");
	 buf.append("<tr><td>Line Number</td><td>");
	 buf.append(line_number);
	 buf.append("</td></tr>");
       }
      buf.append("</td></tr>");
      buf.append("<tr><td>Class</td><td>");
      buf.append(class_name);
      buf.append("</td></tr>");
      if (base_count > 0) {
	 buf.append("</td></tr>");
	 buf.append("<tr><td>Base Time</td><td>");
	 buf.append(IvyFormat.formatTime(base_count * base_time / base_samples));
	 buf.append(" sec (");
	 buf.append(IvyFormat.formatPercent(base_count / base_samples));
	 buf.append(")");
	 buf.append("</td></tr>");
       }
      if (total_count > 0) {
	 buf.append("</td></tr>");
	 buf.append("<tr><td>Total Time</td><td>");
	 buf.append(IvyFormat.formatTime(total_count * base_time / base_samples));
	 buf.append(" sec (");
	 buf.append(IvyFormat.formatPercent(total_count / total_samples));
	 buf.append(")");
	 buf.append("</td></tr>");
       }
      buf.append("</table>");
      return buf.toString();
   }

}	// end of inner class PerfNode



private class ChangeHandler implements Runnable {

   private PerfNode for_node;

   ChangeHandler(PerfNode pn) {
      for_node = pn;
    }

   @Override public void run() {
      perf_model.handleChange(for_node);
    }

}	// en dof inner class ChangeHandler




}	// end of class BddtPerfViewTable




/* end of BddtPerfViewTable.java */
