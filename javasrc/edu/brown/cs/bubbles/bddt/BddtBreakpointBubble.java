/********************************************************************************/
/*										*/
/*		BddtBreakpointBubble.java					*/
/*										*/
/*	Bubble Environment break point bubble					*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook, Steven P. Reiss	*/
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
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubbleOutputer;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpBreakpointHandler;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Iterator;
import java.util.List;


class BddtBreakpointBubble extends BudaBubble implements BddtConstants, BudaConstants,
	BumpConstants, BumpBreakpointHandler, MouseListener, ActionListener, BudaBubbleOutputer {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpClient		bump_client;
private BreakpointTable 	breakpoint_table;
private BreakpointTableModel	table_model;
private BreakpointToolBar	tool_bar;
private Timer			update_timer;
private BumpBreakModel		break_model;

private static final long serialVersionUID = 1;


// TODO add options for exception breakpoints
// TODO add method entry and exit breakpoints
// TODO add conditions for breakpoints



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtBreakpointBubble()
{
   super(null,BudaBorder.RECTANGLE);

   table_model = new BreakpointTableModel();
   bump_client = BumpClient.getBump();
   break_model = bump_client.getBreakModel();

   breakpoint_table = new BreakpointTable(table_model);
   breakpoint_table.setPreferredScrollableViewportSize(BDDT_BREAKPOINT_INITIAL_SIZE);
   breakpoint_table.setOpaque(false);
   breakpoint_table.addMouseListener(this);

   tool_bar = new BreakpointToolBar();
   tool_bar.setVisible(true);
   tool_bar.setFloatable(false);

   JScrollPane scroll = new JScrollPane(breakpoint_table);
   breakpoint_table.setFillsViewportHeight(true);
   BudaCursorManager.setCursor(scroll,Cursor.getDefaultCursor());

   JPanel mainpanel = new JPanel(new BorderLayout());
   mainpanel.add(tool_bar,BorderLayout.SOUTH);
   mainpanel.add(scroll,BorderLayout.CENTER);
   setContentPane(mainpanel);
   mainpanel.addMouseListener(this);
   BumpClient.getBump().addBreakpointHandler(null, this);

   mainpanel.addMouseListener(new FocusOnEntry());

   update_timer = new Timer(300,this);
   update_timer.setRepeats(false);
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();
   g.setColor(BoardColors.getColor(BDDT_BREAKPOINT_OVERVIEW_COLOR_PROP));
   g.fillRect(0, 0, sz.width, sz.height);
}




/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

private String getToolTip(BumpBreakpoint bp)
{
   // TODO: should use bp.toString()

   StringBuffer buf = new StringBuffer();
   String typ = bp.getProperty("TYPE");
   buf.append(typ);
   buf.append(" Breakpoint");
   if (typ.equals("EXCEPTION")) {
      buf.append(" for ");
      buf.append(bp.getProperty("EXCEPTION"));
      if (bp.getBoolProperty("CAUGHT") && bp.getBoolProperty("UNCAUGHT")) ;
      else if (bp.getBoolProperty("CAUGHT")) buf.append(" if caught");
      else if (bp.getBoolProperty("UNCAUGHT")) buf.append(" if uncaught");
      else if (bp.getBoolProperty("SUBCLASS")) buf.append(" w/ subclasses");
    }
   else if (typ.equals("LINE")) {
      buf.append(" at ");
      buf.append(bp.getLineNumber());
      buf.append(" in ");
      buf.append(bp.getProperty("CLASS"));
    }

   if (!bp.getBoolProperty("ENABLED")) {
      buf.append(" (disabled)");
    }

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Mouse Listener methods							*/
/*										*/
/********************************************************************************/

/**
 * Opens up the associated bubble on a double click
 */

@Override public void mouseClicked(MouseEvent e)
{
   if (e.getSource() == breakpoint_table && e.getClickCount() > 1) {
      int row = breakpoint_table.rowAtPoint(e.getPoint());
      BumpBreakpoint bp = breakpoint_table.getActualBreakpoint(row);
      if (bp == null) return;
      File f = bp.getFile();
      int ln = bp.getLineNumber();
      if (f != null && ln > 0) showBubble(f,ln);
   }
}

@Override public void mouseEntered(MouseEvent e)
{
   if (e.getSource() == breakpoint_table) {
      // tool_bar.setVisible(true);
   }
}


@Override public void mouseExited(MouseEvent e)
{
   if (e.getSource() == this) {
      // tool_bar.setVisible(false);
   }
}

@Override public void mousePressed(MouseEvent e)
{}

@Override public void mouseReleased(MouseEvent e)
{}


@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();

   BumpBreakpoint bp = null;
   Point p0 = SwingUtilities.convertPoint(this,e.getPoint(),breakpoint_table);
   int row = breakpoint_table.rowAtPoint(p0);
   if (row >= 0) bp = breakpoint_table.getActualBreakpoint(row);

   if (bp != null) {
      breakpoint_table.setRowSelectionInterval(row,row);
      String typ = bp.getProperty("TYPE");
      if (typ.equals("LINE")) {
	 popup.add(new GotoSourceAction(bp));
       }
      else if (typ.equals("EXCEPTION")) {
	 popup.add(new ExceptionAction(bp,bp.getBoolProperty("CAUGHT"),true));
	 popup.add(new ExceptionAction(bp,bp.getBoolProperty("UNCAUGHT"),false));
         popup.add(new ExceptionSubclassAction(bp,bp.getBoolProperty("SUBCLASSES")));
       }
      popup.add(new EnableAction(bp,bp.getBoolProperty("ENABLED")));
      popup.add(new RemoveAction(bp));
      // popup.add("Edit Breakpoint Properties");
    }

   popup.add(new NewExceptionAction());
   popup.add(new NewFieldWatchAction());

   popup.addSeparator();
   popup.add(getFloatBubbleAction());

   popup.show(this,p0.x,p0.y-5);
}




/********************************************************************************/
/*										*/
/*	Breakpoint Listener methods						*/
/*										*/
/********************************************************************************/

@Override public void handleBreakpointAdded(BumpBreakpoint bp)
{
   SwingUtilities.invokeLater(new DataChanger());
}

@Override public void handleBreakpointChanged(BumpBreakpoint bp)
{
   SwingUtilities.invokeLater(new DataChanger());
}

@Override public void handleBreakpointRemoved(BumpBreakpoint bp)
{
   SwingUtilities.invokeLater(new DataChanger());
}

@Override public void actionPerformed(ActionEvent e)
{
   SwingUtilities.invokeLater(new DataChanger());
}


private void showBubble(File f,int line)
{
   if (f == null) return;

   BaleFactory bf = BaleFactory.getFactory();
   BaleConstants.BaleFileOverview bfo = bf.getFileOverview(null,f);
   if (bfo == null) return;
   int loff = bfo.findLineOffset(line);
   int eoff = bfo.mapOffsetToEclipse(loff);

   BassFactory bsf = BassFactory.getFactory();
   BassName bn = bsf.findBubbleName(f, eoff);
   if (bn == null) return;

   BudaBubble bb = bn.createBubble();
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   if (bba != null) {
      bba.addBubble(bb,this,null,PLACEMENT_ABOVE|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
    }
}


private class DataChanger implements Runnable {

   @Override public void run() {
      table_model.fireTableDataChanged();
    }

}	// end of inner class DataChanger





/********************************************************************************/
/*										*/
/*	Breakpoint tool bar							*/
/*										*/
/********************************************************************************/

private class BreakpointToolBar extends JToolBar implements ActionListener {

   private JButton enableAll;
   private JButton disableAll;
   private JButton remove;

   private static final long serialVersionUID = 1;

   BreakpointToolBar() {
      enableAll = new JButton("Enable All",new ImageIcon(BoardImage
							    .getImage("checkedbox.png")));
      disableAll = new JButton("Disable All",new ImageIcon(BoardImage
							      .getImage("emptybox.png")));
      remove = new JButton("Remove Selected",new ImageIcon(BoardImage.getImage("no.png")));
      enableAll.setText("Enable All");
      disableAll.setText("Disable All");
      remove.setText("Remove Selected");
      enableAll.setMargin(BDDT_BREAKPOINT_BUTTON_MARGIN);
      disableAll.setMargin(BDDT_BREAKPOINT_BUTTON_MARGIN);
      remove.setMargin(BDDT_BREAKPOINT_BUTTON_MARGIN);
      enableAll.addActionListener(this);
      disableAll.addActionListener(this);
      remove.addActionListener(this);
      add(enableAll);
      add(disableAll);
      add(remove);
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (e.getSource() == enableAll) {
	 BumpClient client = BumpClient.getBump();
	 Iterator<BumpBreakpoint> it = client.getAllBreakpoints().iterator();
	 while (it.hasNext()) {
	    BumpBreakpoint bp = it.next();
	    break_model.enableBreakpoint(bp.getFile(), bp.getLineNumber());
	  }
	 //break_model.enableAllBreakpoints(null);
       }
      else if (e.getSource() == disableAll) {
	 BumpClient client = BumpClient.getBump();
	 Iterator<BumpBreakpoint> it = client.getAllBreakpoints().iterator();
	 while (it.hasNext()) {
	    BumpBreakpoint bp = it.next();
	    break_model.disableBreakpoint(bp.getFile(), bp.getLineNumber());
	  }
	 //break_model.disableAllBreakpoints(null);
       }
      else if (e.getSource() == remove) {
	 int[] rows = breakpoint_table.getSelectedRows();
	 BumpBreakpoint[] bps = new BumpBreakpoint[rows.length];
	 for (int i = 0; i < rows.length; i++) {
	    bps[i] = breakpoint_table.getActualBreakpoint(rows[i]);
	  }
	 for (int i = 0; i < bps.length; i++) {
	    break_model.removeBreakpoint(bps[i].getBreakId());
	  }
       }
      update_timer.restart();
    }

}	// end of inner class BreakpointToolBar



/********************************************************************************/
/*										*/
/*	Breakpoint Table methods						*/
/*										*/
/********************************************************************************/

private class BreakpointTable extends JTable {

   private static final long serialVersionUID = 1;


   BreakpointTable(BreakpointTableModel model) {
      super(model);
      setDefaultRenderer(String.class, new BreakpointTableRenderer(getDefaultRenderer(String.class)));
      setDefaultRenderer(Integer.class, new BreakpointTableRenderer(getDefaultRenderer(Integer.class)));
      setDefaultRenderer(Boolean.class, new BreakpointTableRenderer(getDefaultRenderer(Boolean.class)));
      getColumnModel().getColumn(0).setPreferredWidth(BDDT_BREAKPOINT_COLUMN_WIDTHS[0]);
      getColumnModel().getColumn(1).setPreferredWidth(BDDT_BREAKPOINT_COLUMN_WIDTHS[1]);
      getColumnModel().getColumn(2).setPreferredWidth(BDDT_BREAKPOINT_COLUMN_WIDTHS[2]);
      getColumnModel().getColumn(3).setPreferredWidth(BDDT_BREAKPOINT_COLUMN_WIDTHS[3]);
      setOpaque(false);
      setToolTipText("");
      setAutoCreateRowSorter(true);
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Color tc = BoardColors.getColor(BDDT_BREAKPOINT_TOP_COLOR_PROP);
      Color bc = BoardColors.getColor(BDDT_BREAKPOINT_BOTTOM_COLOR_PROP);
      Paint p = new GradientPaint(0f,0f,tc,0f,sz.height,bc);
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setPaint(p);
      g2.fill(r);
      super.paintComponent(g);
    }

   BumpBreakpoint getActualBreakpoint(int row) {
      RowSorter<?> rs = getRowSorter();
      if (rs != null) row = rs.convertRowIndexToModel(row);
      return bump_client.getAllBreakpoints().get(row);
    }

   @Override public String getToolTipText(MouseEvent e) {
      int r = rowAtPoint(e.getPoint());
      if (r < 0) return null;
      BumpBreakpoint bpt = getActualBreakpoint(r);
      return getToolTip(bpt);
    }

}	// end of inner class BreakpointTable





/********************************************************************************/
/*										*/
/*	Cell rendering								*/
/*										*/
/********************************************************************************/

private class BreakpointTableRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   BreakpointTableRenderer(TableCellRenderer renderer) {
      default_renderer = renderer;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
        						       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t, v, sel,
        									     foc, r, c);
      if (breakpoint_table.isRowSelected(r)) {
         cmp.setOpaque(true);
         BoardColors.setColors(cmp,BDDT_BREAKPOINT_SELECTION_COLOR_PROP);
       }
      else {
         cmp.setOpaque(false);
       }
      return cmp;
    }

}	// end of inner class BreakpointTableRenderer




/********************************************************************************/
/*										*/
/*	Table model								*/
/*										*/
/********************************************************************************/

private class BreakpointTableModel extends AbstractTableModel {

   private String[]   column_names = { "Line#", "Class", "Type", "Enabled" };

   private static final long serialVersionUID = 1;


   BreakpointTableModel() { }

   @Override public int getColumnCount() {
      return column_names.length;
    }

   @Override public String getColumnName(int col) {
      return column_names[col];
    }

   @Override public int getRowCount() {
      return bump_client.getAllBreakpoints().size();
    }

   @Override public Object getValueAt(int row,int col) {
      List<BumpBreakpoint> bpl = bump_client.getAllBreakpoints();
      if (bpl == null || row < 0 || row >= bpl.size()) return null;
      BumpBreakpoint bp = bpl.get(row);
      switch (col) {
	 case 0:
	    return bp.getLineNumber();
	 case 1:
	    return bp.getProperty("CLASS");
	 case 2:
	    return bp.getProperty("TYPE");
	 case 3:
	    return bp.getBoolProperty("ENABLED");

	 default:
	    return null;
       }
    }

   @Override public Class<?> getColumnClass(int col) {
      switch (col) {
	 case 0:
	    return Integer.class;
	 case 1:
	    return String.class;
	 case 2:
	    return String.class;
	 case 3:
	    return Boolean.class;
	 default:
	    return null;
       }
    }

   @Override public boolean isCellEditable(int row,int col) {
      switch (col) {
	 case 3:
	    return true;
	 default:
	    return false;
       }
    }

   @Override public void setValueAt(Object value,int row,int col) {
      if (col == 3) {
	 toggleEnablement(bump_client.getAllBreakpoints().get(row));
	 update_timer.restart();
       }
    }

   private void toggleEnablement(BumpBreakpoint bp) {
      if (bp.getBoolProperty("ENABLED")) {
	 break_model.disableBreakpoint(bp.getFile(), bp.getLineNumber());
       }
      else {
	 break_model.enableBreakpoint(bp.getFile(), bp.getLineNumber());
       }

    }

}	// end of inner class BreakpointTableModel



/********************************************************************************/
/*										*/
/*	BudaBubbleOutputer methods						*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()
{
   return "BDDT";
}



@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","BREAKPOINTBUBBLE");
}



/********************************************************************************/
/*										*/
/*	Actions for popup menu							*/
/*										*/
/********************************************************************************/

private class GotoSourceAction extends AbstractAction {

   private BumpBreakpoint for_breakpoint;

   GotoSourceAction(BumpBreakpoint bp) {
      super("Go to Source");
      for_breakpoint = bp;
   }

   @Override public void actionPerformed(ActionEvent e) {
      File f = for_breakpoint.getFile();
      int ln = for_breakpoint.getLineNumber();
      BoardMetrics.noteCommand("BDDT","ShowBreakpointSource");
      showBubble(f,ln);
   }

}	// end of inner class GotoSourceAction




private class ExceptionAction extends AbstractAction {

   private BumpBreakpoint for_breakpoint;
   private boolean is_set;
   private boolean is_caught;

   ExceptionAction(BumpBreakpoint bp,boolean set,boolean cgt) {
      super((set ? "Ignore" : "Break") + " if " + (cgt ? "Caught" : "Uncaught"));
      for_breakpoint = bp;
      is_set = set;
      is_caught = cgt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","EditExceptionBreakpoint");
      bump_client.editBreakpoint(for_breakpoint.getBreakId(),
        			    (is_caught ? "CAUGHT" : "UNCAUGHT"),
        			    Boolean.toString(!is_set));
    }

}	// end of inner class ExceptionAction


private class ExceptionSubclassAction extends AbstractAction {

   private BumpBreakpoint for_breakpoint;
   private boolean is_set;
   
   ExceptionSubclassAction(BumpBreakpoint bp,boolean set) {
      super((set ? "Only this exception" : "Include subclasses of exception"));
      for_breakpoint = bp;
      is_set = set;
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","EditExceptionBreakpoint");
      bump_client.editBreakpoint(for_breakpoint.getBreakId(),
           "SUBCLASSES",Boolean.toString(!is_set));
    }

}	// end of inner class ExceptionSubclassAction




private class EnableAction extends AbstractAction {

   private BumpBreakpoint for_breakpoint;
   private boolean is_disable;


   EnableAction(BumpBreakpoint bp,boolean dis) {
      super((dis ? "Disable" : "Enable") + " Breakpoint");
      for_breakpoint = bp;
      is_disable = dis;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","EnableBreakpoint");
      bump_client.editBreakpoint(for_breakpoint.getBreakId(),
        			    "ENABLE",Boolean.toString(!is_disable));
    }

}	// end of inner class EnableAction




private class RemoveAction extends AbstractAction {

   private BumpBreakpoint for_breakpoint;


   RemoveAction(BumpBreakpoint bp) {
      super("Remove Breakpoint");
      for_breakpoint = bp;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","RemoveBreakpoint");
      break_model.removeBreakpoint(for_breakpoint.getBreakId());
    }

}	// end of inner class RemoveAction



private class NewExceptionAction extends AbstractAction {

   NewExceptionAction() {
      super("Create New Exception Breakpoint");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubble bb = new BddtExceptionBreakpointBubble();
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtBreakpointBubble.this);
      if (bba != null) {
	 bba.addBubble(bb,BddtBreakpointBubble.this,null,PLACEMENT_ABOVE|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class NewExceptionAction




private static class NewFieldWatchAction extends AbstractAction {

   NewFieldWatchAction() {
      super("Create New Field Watch Breakpoing");
    }

   @Override public void actionPerformed(ActionEvent e) {
    }

}	// end of inner class NewExceptionAction


}	// end of class BddtBreakpointBubble



/* end of BddtBreakpointBubble.java */
