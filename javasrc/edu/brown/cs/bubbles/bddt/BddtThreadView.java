/********************************************************************************/
/*										*/
/*		BddtThreadView.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool process thread bubble 	*/
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
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
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
import javax.swing.TransferHandler;
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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;




class BddtThreadView extends BudaBubble implements BddtConstants, BumpConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl launch_control;
private BumpRunModel run_model;
private BumpClient   bump_client;
private BumpProcess  bump_process;

private List<BumpThread>  bump_threads;
private Set<BumpThread>   thread_set;

private ThreadsModel  threads_model;
private ThreadsTable  threads_table;

private static String [] col_names = new String[] {
   "Thread Name","State","Daemon","Type"
};

private static Class<?> [] col_types = new Class[] {
   String.class, BumpThreadState.class, Boolean.class, BumpThreadType.class
};


private static int [] col_sizes = new int [] {
   50, 60, 20, 30
};


private static int [] col_max_size = new int [] {
   0, 0, 0, 0
};

private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.000");

private static int [] col_min_size = new int [] {
   12, 12, 12, 12
};


private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtThreadView(BddtLaunchControl ctrl)
{
   launch_control = ctrl;
   bump_client = BumpClient.getBump();
   run_model = bump_client.getRunModel();

   bump_process = ctrl.getProcess();

   thread_set = new ConcurrentSkipListSet<>(new ThreadComparator());
   if (bump_process != null) {
      for (BumpThread bt : bump_process.getThreads()) {
	 thread_set.add(bt);
      }
   }
   bump_threads = new ArrayList<>(thread_set);

   threads_model = new ThreadsModel();
   threads_table = new ThreadsTable();
   threads_table.addMouseListener(new ClickHandler());

   run_model.addRunEventHandler(new ThreadHandler());

   JScrollPane sp = new JScrollPane(threads_table);
   sp.setPreferredSize(new Dimension(BDDT_THREADS_WIDTH,BDDT_THREADS_HEIGHT));
   setContentPane(sp,null);

   threads_table.addMouseListener(new BudaConstants.FocusOnEntry());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Point getPosition(BumpThread t)
{
   return null;
}



/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();

   Point pt = SwingUtilities.convertPoint(getContentPane().getParent(),e.getPoint(),threads_table);
   pt = new Point(pt.x,pt.y-5);
   int row = threads_table.rowAtPoint(pt);
   BumpThread thread = getActualThread(row);
   if (thread != null) {
      popup.add(new ValuesAction(thread));
      if (thread.getThreadState().isRunning()) {
	 popup.add(new ThreadAction(thread,"Pause"));
	 popup.add(new ProcessAction(thread.getProcess(),"Pause"));
       }
      else if (thread.getThreadState().isStopped()) {
         BumpThreadStack stk = thread.getStack();
         if (stk != null) {
            BumpStackFrame frm = stk.getFrame(1);
            popup.add(new ThreadAction(thread,"Step Into"));
            popup.add(new ThreadAction(thread,"Step Over"));
            popup.add(new ThreadAction(thread,"Step Return"));
            popup.add(new ThreadAction(thread,"Drop to Frame"));
            if (frm != null) popup.add(new ThreadAction(thread,"Drop to Prior Frame"));
            popup.add(new ThreadAction(thread,"Resume"));
            popup.add(new ProcessAction(thread.getProcess(),"Resume"));
            popup.add(new ProcessAction(thread.getProcess(),"Pause"));
            popup.add(new HistoryAction(thread));
          }
       }
      if (thread.getThreadState().isException()) {
	 popup.add(new ExceptionAction(thread));
         if (thread.getBreakpoint() != null &&             
               thread.getBreakpoint().getProperty("TYPE").equals("EXCEPTION")) {
            popup.add(new IgnoreExceptionAction(thread));
          }
       }

      popup.add(new ProcessAction(thread.getProcess(),"Terminate"));

      BumpThreadStack stk = thread.getStack();
      if (stk != null && stk.getNumFrames() > 0) {
	 BumpStackFrame frm = stk.getFrame(0);
	 if (launch_control.frameFileExists(frm)) {
	    popup.add(new SourceAction(frm));
	  }
       }
    }

   popup.add(getFloatBubbleAction());
   popup.show(threads_table, pt.x, pt.y);
}




/********************************************************************************/
/*										*/
/*	Mouse handling methods							*/
/*										*/
/********************************************************************************/

private class ClickHandler extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
	 Point pt = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),threads_table);
	 pt = new Point(pt.x,pt.y-5);
	 int row = threads_table.rowAtPoint(pt);
	 BumpThread thread = getActualThread(row);
	 if (thread != null) {
	    ValuesAction va = new ValuesAction(thread);
	    va.actionPerformed(null);
	  }
       }
    }

}	// end of inner class ClickHandler



/********************************************************************************/
/*										*/
/*	Popup menu handling classes						*/
/*										*/
/********************************************************************************/

private class ProcessAction extends AbstractAction {

   private BumpProcess the_launch;
   private String action_name;

   private static final long serialVersionUID = 1;

   ProcessAction(BumpProcess c,String nm) {
      super(nm + " process");
      the_launch = c;
      action_name = nm;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","Thread" + action_name);
      if (action_name.equals("Terminate")) bump_client.terminate(the_launch);
      else if (action_name.equals("Pause")) bump_client.suspend(the_launch);
      else if (action_name.equals("Resume")) bump_client.resume(the_launch);
    }

}	// end of inner class ProcessAction




private class ThreadAction extends AbstractAction {

   private BumpThread the_thread;
   private String action_name;

   private static final long serialVersionUID = 1;

   ThreadAction(BumpThread th,String act) {
      super(act + " " + th.getName());
      the_thread = th;
      action_name = act;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","Thread" + action_name);
      if (action_name.equals("Pause")) bump_client.suspend(the_thread);
      else if (action_name.equals("Resume")) bump_client.resume(the_thread);
      else if (action_name.equals("Step Into")) bump_client.stepInto(the_thread);
      else if (action_name.equals("Step Over")) bump_client.stepOver(the_thread);
      else if (action_name.equals("Step Return")) bump_client.stepReturn(the_thread);
      else if (action_name.equals("Drop to Frame")) bump_client.dropToFrame(the_thread);
      else if (action_name.equals("Drop to Prior Frame")) {
         BumpThreadStack stk = the_thread.getStack();
         BumpStackFrame frm = stk.getFrame(1);
         if (frm != null) bump_client.dropToFrame(frm);
       }
      else BoardLog.logE("BDDT","Unknown thread action " + action_name);
    }

}	// end of inner class ThreadAction




private class ValuesAction extends AbstractAction {

   private BumpThread for_thread;

   private static final long serialVersionUID = 1;

   ValuesAction(BumpThread th) {
      super("Show Values for " + th.getName());
      for_thread = th;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","ThreadValues");
      BddtStackView sv = new BddtStackView(launch_control,for_thread);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtThreadView.this);
      if (bba != null) {
	 bba.addBubble(sv,BddtThreadView.this,null,
			  PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class ValuesAction




private class SourceAction extends AbstractAction {

   private BumpStackFrame for_frame;

   private static final long serialVersionUID = 1;

   SourceAction(BumpStackFrame bsf) {
      super("Goto Source");
      for_frame = bsf;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubble bb = null;
      if (launch_control.frameFileExists(for_frame)) {
	 String proj = for_frame.getThread().getLaunch().getConfiguration().getProject();
	 String mid = for_frame.getMethod() + for_frame.getSignature();
	 bb = BaleFactory.getFactory().createMethodBubble(proj,mid);
       }
      if (bb != null) {
	 BoardMetrics.noteCommand("BDDT","ThreadSource");
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtThreadView.this);
	 if (bba != null) {
	    bba.addBubble(bb,BddtThreadView.this,null,
			     PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
	  }
       }
    }

}	// end of inner class SourceAction



private class ExceptionAction extends AbstractAction {

   private BumpThread for_thread;

   private static final long serialVersionUID = 1;

   ExceptionAction(BumpThread bt) {
      super("Goto Exception");
      for_thread = bt;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubble bb = null;
      String typ = for_thread.getExceptionType();
      bb = BaleFactory.getFactory().createClassBubble(null,typ);
      if (bb == null) {
	 bb = BudaRoot.createDocumentationBubble(typ);
       }
      if (bb != null) {
	 BoardMetrics.noteCommand("BDDT","ThreadSource");
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtThreadView.this);
	 if (bba != null) {
	    bba.addBubble(bb,BddtThreadView.this,null,
			     PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
	  }
       }
    }

}	// end of inner class ExceptionAction



private class IgnoreExceptionAction extends AbstractAction {
   
   private BumpThread for_thread;
   
   private static final long serialVersionUID = 1;
   
   IgnoreExceptionAction(BumpThread bt) {
      super("Ignore exception breakpoint at this point");
      for_thread = bt;
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      BumpBreakpoint bbp = for_thread.getBreakpoint();
      if (bbp == null) return;
      String where = for_thread.getStack().getFrame(0).getMethod();
      if (where == null) return;
      int idx = where.lastIndexOf(".");
      if (idx < 0) return;
      where = where.substring(0,idx);
      where = where.replace("$",".");
      String exl = bbp.getProperty("EXCLUDE");
      if (exl == null) {
         exl = where;
       }
      else exl = exl + "," + where;
      BumpClient.getBump().editBreakpoint(bbp.getBreakId(),"EXCLUDE",exl);
    }
}



private class HistoryAction extends AbstractAction {

   private BumpThread for_thread;

   private static final long serialVersionUID = 1;

   HistoryAction(BumpThread th) {
      super("Request History when " + th.getName() + " stopped");
      for_thread = th;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","ThreadHistory");
      BudaBubble bb = new BddtStopTraceBubble(launch_control,for_thread);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtThreadView.this);
      if (bba != null) {
	 bba.addBubble(bb,BddtThreadView.this,null,
			  PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class HistoryAction




/********************************************************************************/
/*										*/
/*	Model event handling							*/
/*										*/
/********************************************************************************/

private class ThreadHandler implements BumpRunEventHandler
{
   @Override public void handleLaunchEvent(BumpRunEvent evt)	{ }

   @Override public void handleProcessEvent(BumpRunEvent evt)	{ }

   @Override public void handleThreadEvent(BumpRunEvent evt) {
      BumpProcess bp = evt.getProcess();
      if (launch_control != null && launch_control.getProcess() != bp) return;
      TableUpdater tup = new TableUpdater(evt);
      SwingUtilities.invokeLater(tup);
    }

   @Override public void handleConsoleMessage(BumpProcess bp,boolean err,boolean eof,String msg)	{ }

}	// end of inner class ThreadHandler




private class TableUpdater implements Runnable {

   private BumpRunEvent run_event;

   TableUpdater(BumpRunEvent evt) {
      run_event = evt;
    }

   @Override public void run() {
      BumpThread bt;
      switch (run_event.getEventType()) {
	 case THREAD_ADD :
	    bt = run_event.getThread();
	    if (bt != null) {
	       if (!thread_set.add(bt)) {
		  // BoardLog.logD("BDDT","THREAD ALREADY IN THREADSET " + bt.getId());
		  return;
	       }
	       synchronized (bump_threads) {
		  bump_threads.clear();
		  bump_threads.addAll(thread_set);
		}
	     }
	    break;
	 case THREAD_REMOVE :
	    bt = run_event.getThread();
	    if (bt != null) {
	       thread_set.remove(bt);
	       synchronized (bump_threads) {
		  bump_threads.remove(bt);
		}
	     }
	    break;
	 case THREAD_CHANGE :
	     // BoardLog.logD("BDDT","THREAD CHANGE " + run_event.getThread().getThreadState());
	    //TODO: Update thread
	    break;
	 case THREAD_TRACE :
	 case THREAD_HISTORY :
	    return;
	 default:
	    break;
       }
      threads_model.fireTableDataChanged();
    }

}	// end of inner class TableUpdater



/********************************************************************************/
/*										*/
/*	Method to decode table sorting						*/
/*										*/
/********************************************************************************/

private BumpThread getActualThread(int idx)
{
   if (idx < 0) return null;

   synchronized (bump_threads) {
      if (threads_table != null) {
	 RowSorter<?> rs = threads_table.getRowSorter();
	 if (rs != null) idx = rs.convertRowIndexToModel(idx);
       }

      return bump_threads.get(idx);
    }
}



/********************************************************************************/
/*										*/
/*	Thread comparator							*/
/*										*/
/********************************************************************************/

private static class ThreadComparator implements Comparator<BumpThread> {

   @Override public int compare(BumpThread t1,BumpThread t2) {
      if (t1.getThreadType() != t2.getThreadType()) {
	 return t2.getThreadType().ordinal() - t1.getThreadType().ordinal();
       }
      if (t1.getName() == null && t2.getName() == null) return 0;
      else if (t1.getName() == null) return -1;
      else if (t2.getName() == null) return 1;
      int sts = t1.getName().compareTo(t2.getName());
      if (sts != 0) return sts;
      return t1.getId().compareTo(t2.getId());
    }

}	// end of inner class ThreadComparator




/********************************************************************************/
/*										*/
/*	Configuation table model						*/
/*										*/
/********************************************************************************/

private class ThreadsModel extends AbstractTableModel {

   private static final long serialVersionUID = 1;

   ThreadsModel() { }

   @Override public int getColumnCount()		{ return col_names.length; }
   @Override public String getColumnName(int idx)	{ return col_names[idx]; }
   @Override public Class<?> getColumnClass(int idx)	{ return col_types[idx]; }
   @Override public boolean isCellEditable(int r,int c) { return false;  }
   @Override public int getRowCount()			{ return bump_threads.size(); }

   @Override public Object getValueAt(int r,int c) {
      BumpThread bt;
      synchronized (bump_threads) {
	 if (r < 0 || r >= bump_threads.size()) return null;
	 bt = bump_threads.get(r);
       }
      switch (c) {
	 case 0 :
	    return bt.getName();
	 case 1 :
	    return bt.getThreadState();
	 case 2 :
	    return bt.isDaemonThread();
	 case 3 :
	    return bt.getThreadType();
	 case -1:
	    return bt;
       }

      return null;
    }

}	// end of inner class ThreadsModel



/********************************************************************************/
/*										*/
/*	Threads table implementation						*/
/*										*/
/********************************************************************************/

private class ThreadsTable extends JTable implements BudaConstants.BudaBubbleOutputer,
		MouseListener
{
   private CellDrawer [] cell_drawer;

   private static final long serialVersionUID = 1;

   ThreadsTable() {
      super(threads_model);
      setAutoCreateRowSorter(true);
      fixColumnSizes();
      setIntercellSpacing(new Dimension(2,1));
      setToolTipText("");
      addMouseListener(this);
      setDragEnabled(true);
      setTransferHandler(new Transferer());
      getTableHeader().setReorderingAllowed(false);
      BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      setSelectionBackground(BoardColors.getColor("Bddt.ThreadSelectionBackground")); 
      setSelectionForeground(BoardColors.getColor("Bddt.ThreadSelectionForeground")); 
      BoardColors.setColors(this,BDDT_THREADS_TOP_COLOR_PROP);
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

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
	 //int row = rowAtPoint(e.getPoint());
	 //BumpProcess blp = getActualThread(row);
	 //if (blp == null) return;
	 //System.err.println("START CONFIGURATION " + blp.getId());
       }
    }

   @Override public void mouseEntered(MouseEvent _e)	{ }
   @Override public void mouseExited(MouseEvent _e)	{ }
   @Override public void mouseReleased(MouseEvent e)	{ }
   @Override public void mousePressed(MouseEvent e)	{ }

   @Override protected void paintComponent(Graphics g) {
      synchronized (bump_threads) {
	 Dimension sz = getSize();
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 Graphics2D g2 = (Graphics2D) g.create();
         Color tc = BoardColors.getColor(BDDT_THREADS_TOP_COLOR_PROP);
         Color bc = BoardColors.getColor(BDDT_THREADS_BOTTOM_COLOR_PROP);
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

   @Override public String getConfigurator()		{ return "BDDT"; }

   @Override public void outputXml(BudaXmlWriter xw)	{ }

   @Override public String getToolTipText(MouseEvent e)  {
      int row = rowAtPoint(e.getPoint());
      BumpThread bt = getActualThread(row);
      if (bt == null) return null;

      StringBuffer buf = new StringBuffer();
      buf.append("<html>");
      buf.append("Thread " + bt.getName());
      buf.append("<table><tr><td>State:</td><td>");
      buf.append(bt.getThreadState().toString());
      buf.append("</td></tr>");
      BumpThreadStateDetail dtl = bt.getThreadDetails();

      if (bt.getThreadState().isException()) {
	 buf.append("<tr><td>Exception:</td><td>");
	 if (bt.getExceptionType() != null)  buf.append(bt.getExceptionType().toString());
	 buf.append("</td></tr>");
       }
      else if (dtl != null && dtl != BumpThreadStateDetail.NONE) {
	 buf.append("<tr><td>Detail:</td><td>");
	 buf.append(dtl.toString());
	 buf.append("</td></tr>");
       }

      buf.append("<tr><td>Type:</td><td>");
      buf.append(bt.getThreadType().toString());
      buf.append("</td></tr>");

      outputTime(bt.getCpuTime(),"Cpu Time",buf);
      outputTime(bt.getUserTime(),"User Time",buf);
      outputTime(bt.getWaitTime(),"Wait Time",buf);
      outputTime(bt.getBlockTime(),"Block Time",buf);
      outputCount(bt.getWaitCount(),"Number Waits",buf);
      outputCount(bt.getBlockCount(),"Number Blocks",buf);

      BumpThreadStack stk = bt.getStack();
      if (stk != null && stk.getNumFrames() > 0) {
	 BumpStackFrame frm = stk.getFrame(0);
	 buf.append("<tr><td>In Class:</td><td>");
	 buf.append(frm.getFrameClass());
	 buf.append("</td></tr>");
	 buf.append("<tr><td>In Method:</td><td>");
	 String mnm = frm.getMethod();
	 int idx1 = mnm.lastIndexOf(".");
	 if (idx1 > 0) mnm = mnm.substring(idx1+1);
	 if (mnm.equals("<clinit>")) mnm = "Static Initializer";
	 else if (mnm.equals("<init>")) {
	    String cls = frm.getFrameClass();
	    idx1 = cls.lastIndexOf(".");
	    if (idx1 >= 0) cls = cls.substring(idx1+1);
	    idx1 = cls.lastIndexOf("$");
	    if (idx1 > 0) cls = cls.substring(idx1+1);
	    mnm = cls;
	  }
	 buf.append(mnm);
	 buf.append(frm.getSignature());
	 buf.append("</td></tr>");
	 if (frm.getLineNumber() > 0) {
	    buf.append("<tr><td>At line:</td><td>");
	    buf.append(frm.getLineNumber());
	    buf.append("</td></tr>");
	 }
      }
      buf.append("</table>");

      return buf.toString();
   }

   void outputTime(long time,String id,StringBuffer buf) {
      if (time <= 0) return;
      buf.append("<tr><td>");
      buf.append(id);
      buf.append(":</td><td>");
      double v = time / 1000000000.0;
      buf.append(TIME_FORMAT.format(v));
      buf.append("</td></tr>");
    }

   void outputCount(int count,String id,StringBuffer buf) {
      if (count <= 0) return;
      buf.append("<tr><td>");
      buf.append(id);
      buf.append(":</td><td>");
      buf.append(count);
      buf.append("</td></tr>");
    }

}	// end of inner class ThreadsTable




/********************************************************************************/
/*										*/
/*	Table renderers 							*/
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




private class CellDrawer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   CellDrawer(TableCellRenderer dflt) {
      default_renderer = dflt;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
        						       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      if (v == BumpThreadState.EXCEPTION) {
         cmp.setBackground(BoardColors.getColor("Bddt.ThreadExceptionColor")); 
         cmp.setOpaque(true);
       }
      else if (c == 0) {
        int rn = t.convertRowIndexToModel(r);
        BumpThread bt = (BumpThread) t.getModel().getValueAt(rn,-1);
        if (bt.getThreadState().isStopped() && launch_control.getLastStoppedThread() == bt) {
           cmp.setBackground(BoardColors.getColor("Bddt.ActiveThreadColor"));
           cmp.setOpaque(true);
        }
        else {
           cmp.setOpaque(false);
        }
      }
      else {
         cmp.setOpaque(false);
       }
      return cmp;
    }

}	// end of innerclass ErrorRenderer



/********************************************************************************/
/*										*/
/*	Methods to pull out a stack bubble					*/
/*										*/
/********************************************************************************/

private class Transferer extends TransferHandler {

   private static final long serialVersionUID = 1;

   @Override protected Transferable createTransferable(JComponent c) {
      ThreadsTable tbl = null;
      for (Component jc = c; jc != null; jc = jc.getParent()) {
	 if (jc instanceof ThreadsTable) {
	    tbl = (ThreadsTable) jc;
	    break;
	  }
       }
      if (tbl == null) return null;

      int [] sels = tbl.getSelectedRows();
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

   private List<BumpThread> thread_entries;

   TransferBubble(int [] indices) {
      thread_entries = new ArrayList<>();
      for (int i = 0; i < indices.length; ++i) {
         BumpThread bt = getActualThread(indices[i]);
         if (bt != null) thread_entries.add(bt);
       }
    }

   boolean isValid()				{ return thread_entries.size() > 0; }

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
      BudaBubble [] rslt = new BudaBubble[thread_entries.size()];
      int i = 0;
      for (BumpThread bt : thread_entries) {
	 rslt[i++] = new BddtStackView(launch_control,bt);
       }
      return rslt;
    }

}	// end of inner class TransferBubble





}	// end of class BddtThreadView




/* end of BddtThreadView.java */
