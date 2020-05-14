/********************************************************************************/
/*										*/
/*		BeamProblemBubble.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items problem bubble		*/
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



package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

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
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;



class BeamProblemBubble extends BudaBubble implements BeamConstants, BumpConstants,
	BumpConstants.BumpProblemHandler
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpClient		bump_client;
private List<BumpProblem>	active_problems;
private ProblemTable		problem_table;
private Set<BumpErrorType>	allow_types;
private boolean 		for_tasks;
private Color			top_color;
private Color			bottom_color;
private Color			overview_color;
private Font			base_font;
private int			base_height;

private static BoardProperties	beam_properties = BoardProperties.getProperties("Beam");


private static String [] col_names = new String[] {
   "?","Description","Resource","Line"
};

private static Class<?> [] col_types = new Class<?>[] {
   String.class, String.class, String.class, Integer.class
};


private static int [] col_sizes = new int [] {
   16, 200, 60, 50
};


private static int [] col_max_size = new int [] {
   32, 0, 0, 50
};

private static int [] col_min_size = new int [] {
   12, 20, 20, 20
};


private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamProblemBubble(String typs,boolean task)
{
   bump_client = BumpClient.getBump();
   active_problems = new ArrayList<>();
   for_tasks = task;
   base_font = beam_properties.getFont("Beam.problem.font");

   if (typs == null) {
      if (for_tasks) allow_types = EnumSet.of(BumpErrorType.NOTICE);
      else allow_types = EnumSet.of(BumpErrorType.FATAL,BumpErrorType.ERROR,BumpErrorType.WARNING);
    }
   else {
      allow_types = EnumSet.noneOf(BumpErrorType.class);
      StringTokenizer tok = new StringTokenizer(typs);
      while (tok.hasMoreTokens()) {
	 String s = tok.nextToken();
	 try {
	    BumpErrorType bet = BumpErrorType.valueOf(s);
	    allow_types.add(bet);
	  }
	 catch (IllegalArgumentException e) { }
       }
    }

   if (for_tasks) {
      top_color = BoardColors.getColor(TASK_TOP_COLOR_PROP);
      bottom_color = BoardColors.getColor(TASK_BOTTOM_COLOR_PROP);
      overview_color = BoardColors.getColor(TASK_OVERVIEW_COLOR_PROP);
    }
   else {
      top_color = BoardColors.getColor(PROBLEM_TOP_COLOR_PROP);
      bottom_color = BoardColors.getColor(PROBLEM_BOTTOM_COLOR_PROP);
      overview_color = BoardColors.getColor(PROBLEM_OVERVIEW_COLOR_PROP);
    }

   for (BumpProblem bp : bump_client.getAllProblems()) {
      if (allow_types.contains(bp.getErrorType())) active_problems.add(bp);
    }

   bump_client.addProblemHandler(null,this);

   problem_table = new ProblemTable();
   base_height = problem_table.getRowHeight();

   JScrollPane sp = new JScrollPane(problem_table);
   sp.setSize(new Dimension(beam_properties.getInt(PROBLEM_WIDTH),beam_properties.getInt(PROBLEM_HEIGHT)));

   setContentPane(sp,null);
}



@Override protected void localDispose()
{
   bump_client.removeProblemHandler(this);
}



/********************************************************************************/
/*										*/
/*	Problem update methods							*/
/*										*/
/********************************************************************************/

@Override public void handleProblemAdded(BumpProblem bp)
{
   if (!allow_types.contains(bp.getErrorType())) return;

   synchronized (active_problems) {
      active_problems.add(bp);
    }
}


@Override public void handleProblemRemoved(BumpProblem bp)
{
   if (!allow_types.contains(bp.getErrorType())) return;

   synchronized (active_problems) {
      active_problems.remove(bp);
    }
}


@Override public void handleClearProblems()
{
   active_problems.clear();
}


@Override public void handleProblemsDone()
{
   problem_table.modelUpdated();
}




/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());
   // TODO: provide rebuild, clean, etc. options

   menu.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();

   g.setColor(overview_color);
   g.fillRect(0,0,sz.width,sz.height);
}



/********************************************************************************/
/*										*/
/*	BumpProblem decoding methods						*/
/*										*/
/********************************************************************************/

boolean matchTypes(String typs,boolean task)
{
   if (task != for_tasks) return false;

   EnumSet<BumpErrorType> es = EnumSet.noneOf(BumpErrorType.class);
   StringTokenizer tok = new StringTokenizer(typs);
   while (tok.hasMoreTokens()) {
      String s = tok.nextToken();
      try {
	 BumpErrorType bet = BumpErrorType.valueOf(s);
	 es.add(bet);
       }
      catch (IllegalArgumentException e) { }
    }

   return es.equals(allow_types);
}



private String getErrorType(BumpProblem bp)
{
   switch (bp.getErrorType()) {
      case FATAL :
	 return "F";
      case ERROR :
	 return "E";
      case WARNING :
	 return "W";
      case NOTICE :
	 return "N";
    }

   return null;
}



private String getDescription(BumpProblem bp)
{
   return bp.getMessage();
}



private String getResource(BumpProblem bp)
{
   File f = bp.getFile();
   if (f == null) return null;
   return f.getName();
}



private Integer getLine(BumpProblem bp)
{
   int ln = bp.getLine();
   if (ln == 0) return null;
   return Integer.valueOf(ln);
}



private String getToolTip(BumpProblem bp)
{
   return bp.getErrorType().toString() + ": " + bp.getMessage();
}

@Override protected void setScaleFactor(double sf)
{
   Font ft = base_font;
   int ht = base_height;
   if (sf != 1) {
      float fsz = base_font.getSize();
      fsz = ((float)(fsz * sf));
      ft = base_font.deriveFont(fsz);
      ht = (int)(base_height * sf + 0.5);
    }
   problem_table.setFont(ft);
   problem_table.setRowHeight(ht);
}




/********************************************************************************/
/*										*/
/*	New bubble methods							*/
/*										*/
/********************************************************************************/

private class BubbleShower implements Runnable {

   private File for_file;
   private int	at_line;
   private BassName bass_name;

   BubbleShower(File f,int ln) {
      for_file = f;
      at_line = ln;
      bass_name = null;
    }

   @Override public void run() {
      if (bass_name == null) {
	 BaleFactory bf = BaleFactory.getFactory();
	 BaleConstants.BaleFileOverview bfo = bf.getFileOverview(null,for_file);
	 if (bfo == null) return;
	 int loff = bfo.findLineOffset(at_line);
	 int eoff = bfo.mapOffsetToEclipse(loff);

	 BassFactory bsf = BassFactory.getFactory();
	 bass_name = bsf.findBubbleName(for_file,eoff);
	 if (bass_name == null) return;

	 SwingUtilities.invokeLater(this);
       }
      else {		// in Swing thread
	 BudaBubble bb = bass_name.createBubble();
	 if (bb == null) return;
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BeamProblemBubble.this);
	 if (bba != null) {
	    bba.addBubble(bb,BeamProblemBubble.this,null,PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
	  }
       }
    }

}	// end of inner class BubbleShower




/********************************************************************************/
/*										*/
/*	Table for problems							*/
/*										*/
/********************************************************************************/

private class ProblemTable extends JTable implements MouseListener,
		BudaConstants.BudaBubbleOutputer
{
   private ErrorRenderer [] error_renderer;
   private WarningRenderer [] warning_renderer;
   private NoticeRenderer [] notice_renderer;

   private static final long serialVersionUID = 1;

   ProblemTable() {
      super(new ProblemModel());
      setAutoCreateRowSorter(true);
      fixColumnSizes();
      setIntercellSpacing(new Dimension(2,1));
      setToolTipText("");
      addMouseListener(this);
      setOpaque(false);
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
	 TableColumn tc = e.nextElement();
	 tc.setHeaderRenderer(new HeaderRenderer(getTableHeader().getDefaultRenderer()));
       }
      error_renderer = new ErrorRenderer[col_names.length];
      warning_renderer = new WarningRenderer[col_names.length];
      notice_renderer = new NoticeRenderer[col_names.length];
    }

   void modelUpdated() {
      ((ProblemModel) getModel()).fireTableDataChanged();
    }

   @Override public TableCellRenderer getCellRenderer(int row,int col) {
      BumpProblem bp = getActualProblem(row);
      BumpErrorType et = BumpErrorType.NOTICE;
      if (bp != null) et = bp.getErrorType();
      switch (et) {
	 case WARNING :
	    if (warning_renderer[col] == null) {
	       warning_renderer[col] = new WarningRenderer(super.getCellRenderer(row,col));
	     }
	    return warning_renderer[col];
	 case ERROR :
	 case FATAL :
	    if (error_renderer[col] == null) {
	       error_renderer[col] = new ErrorRenderer(super.getCellRenderer(row,col));
	     }
	    return error_renderer[col];
	 default :
	 case NOTICE :
	    if (notice_renderer[col] == null) {
	       notice_renderer[col] = new NoticeRenderer(super.getCellRenderer(row,col));
	     }
	    return notice_renderer[col];
       }
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

   @Override public void mouseClicked(MouseEvent e) {
      int cct = beam_properties.getInt("Beam.problem.click.count",1);
      if (e.getClickCount() != cct) return;
      int row = rowAtPoint(e.getPoint());
      BumpProblem bp = getActualProblem(row);
      if (bp == null) return;
      File f = bp.getFile();
      int ln = bp.getLine();
      BoardThreadPool.start(new BubbleShower(f,ln));
      // showBubble(f,ln);
    }

   @Override public void mouseEntered(MouseEvent _e)			{ }
   @Override public void mouseExited(MouseEvent _e)			{ }
   @Override public void mouseReleased(MouseEvent e)			{ }
   @Override public void mousePressed(MouseEvent e)			{ }

   @Override public String getToolTipText(MouseEvent e) {
      int r = rowAtPoint(e.getPoint());
      if (r < 0) return null;
      BumpProblem bp = getActualProblem(r);
      return getToolTip(bp);
    }

   // @Override public Point getToolTipLocation(MouseEvent e) {
      // return BudaRoot.computeToolTipLocation(e);
    // }

   // @Override public Point getLocationOnScreen() {
      // return BudaRoot.computeLocationOnScreen(this);
    // }

   @Override protected void paintComponent(Graphics g) {
      synchronized (active_problems) {
	 if (top_color.getRGB() != bottom_color.getRGB()) {
	    Graphics2D g2 = (Graphics2D) g.create();
	    Dimension sz = getSize();
	    Paint p = new GradientPaint(0f,0f,top_color,0f,sz.height,bottom_color);
	    Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	    g2.setPaint(p);
	    g2.fill(r);
	 }
	 try {
	    super.paintComponent(g);
	  }
	 catch (Throwable t) { }
       }
    }

   @Override public String getConfigurator()			{ return "BEAM"; }
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","PROBLEMS");
      StringBuffer buf = new StringBuffer();
      for (BumpErrorType bet : allow_types) buf.append(bet.toString() + " ");
      xw.field("ERRORTYPES",buf.toString());
      xw.field("FORTASKS",for_tasks);
    }

}	// end of inner class ProblemTable




/********************************************************************************/
/*										*/
/*	Table model for problem table						*/
/*										*/
/********************************************************************************/

private class ProblemModel extends AbstractTableModel {

   private static final long serialVersionUID = 1;

   ProblemModel() { }

   @Override public int getColumnCount()		{ return col_names.length; }
   @Override public String getColumnName(int idx)	{ return col_names[idx]; }
   @Override public Class<?> getColumnClass(int idx)	{ return col_types[idx]; }
   @Override public boolean isCellEditable(int r,int c) { return false; }
   @Override public int getRowCount()			{ return active_problems.size(); }

   @Override public Object getValueAt(int r,int c) {
      BumpProblem bp;
      synchronized (active_problems) {
	 if (r < 0 || r >= active_problems.size()) return null;
	 bp = active_problems.get(r);
       }
      switch (c) {
	 case 0 :
	    return getErrorType(bp);
	 case 1 :
	    return getDescription(bp);
	 case 2 :
	    return getResource(bp);
	 case 3 :
	    return getLine(bp);
       }
      return null;
    }

}	// end of inner class ProblemModel



/********************************************************************************/
/*										*/
/*	Sorting methods 							*/
/*										*/
/********************************************************************************/

private BumpProblem getActualProblem(int idx)
{
   if (idx < 0) return null;

   synchronized (active_problems) {
      if (problem_table != null) {
	 RowSorter<?> rs = problem_table.getRowSorter();
	 try {
	    if (rs != null) idx = rs.convertRowIndexToModel(idx);
	  }
	 catch (ArrayIndexOutOfBoundsException e) {
	    return null;
	  }
       }

      if (idx >= active_problems.size()) return null;
       return active_problems.get(idx);
    }
}



/********************************************************************************/
/*										*/
/*	Renderers								*/
/*										*/
/********************************************************************************/

private static class HeaderRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;
   private Font bold_font;
   private Font component_font;

   HeaderRenderer(TableCellRenderer dflt) {
      default_renderer = dflt;
      bold_font = null;
      component_font = null;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);

      if (component_font != null && t.getFont() != component_font) bold_font = null;

      if (bold_font == null) {
	 component_font = t.getFont();
	 bold_font = component_font.deriveFont(Font.BOLD);
       }
      cmp.setFont(bold_font);
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of subclass HeaderRenderer




private static class ErrorRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   ErrorRenderer(TableCellRenderer dflt) {
      default_renderer = dflt;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      cmp.setForeground(BoardColors.getColor(PROBLEM_ERROR_COLOR_PROP));
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of subclass ErrorRenderer




private static class WarningRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   WarningRenderer(TableCellRenderer dflt) {
      default_renderer = dflt;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      cmp.setForeground(BoardColors.getColor(PROBLEM_WARNING_COLOR_PROP));
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of subclass HeaderRenderer




private static class NoticeRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;


   NoticeRenderer(TableCellRenderer dflt) {
      default_renderer = dflt;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      cmp.setForeground(BoardColors.getColor(PROBLEM_NOTICE_COLOR_PROP));
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of subclass NoticeRenderer




}	// end of class BeamProblemBubble




/* end of BeamProblemBubble.java */























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































