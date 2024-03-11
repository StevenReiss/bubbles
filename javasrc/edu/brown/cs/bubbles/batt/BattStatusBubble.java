/********************************************************************************/
/*										*/
/*		BattStatusBubble.java						*/
/*										*/
/*	Bubbles Automated Testing Tool status display bubble			*/
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



package edu.brown.cs.bubbles.batt;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bddt.BddtFactory;
import edu.brown.cs.bubbles.beam.BeamFactory;
import edu.brown.cs.bubbles.beam.BeamNoteBubble;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaToolTip;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class BattStatusBubble extends BudaBubble implements BattConstants, ActionListener,
	BattConstants.BattModelListener, BudaConstants.BudaBubbleOutputer, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BattModeler	batt_model;
private SwingGridPanel	display_panel;
private DisplayTable	display_table;
private JPanel		display_bar;
private TestMode	current_mode;
private RunType 	current_runtype;
private JButton         run_selected;

private Set<DisplayMode> ALL_MODE = EnumSet.of(DisplayMode.ALL);
private Set<DisplayMode> FAIL_MODE = EnumSet.of(DisplayMode.FAIL);
private Set<DisplayMode> PENDING_MODE = EnumSet.of(DisplayMode.PENDING,DisplayMode.NEEDED);
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattStatusBubble(BattModeler bm)
{
   batt_model = bm;
   setupPanel();
   current_mode = null;
   current_runtype = RunType.ALL;

   setContentPane(display_panel);
   bm.addBattModelListener(this);

   BoardThreadPool.start(new SetupBatt());
}



@Override protected void localDispose()
{
   batt_model.removeBattModelListener(this);
}



/********************************************************************************/
/*										*/
/*	Display setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   display_panel = new SwingGridPanel();
   display_panel.beginLayout();
   display_bar = new DisplayBar();
   display_panel.addLabellessRawComponent("BAR",display_bar,true,false);
   display_panel.addSeparator();
   display_table = new DisplayTable();
   JScrollPane jsp = new JScrollPane(display_table);
   display_panel.addLabellessRawComponent("TABLE",jsp,true,true);
   display_panel.addSeparator();

   display_panel.addBottomButton("SHOW ALL","ALL",this);
   display_panel.addBottomButton("SHOW PENDING","PENDING",this);
   display_panel.addBottomButton("SHOW FAIL","FAIL",this);
   run_selected = display_panel.addBottomButton("RUN SELECTED","RUN",this);
   run_selected.setEnabled(false);
   display_panel.addBottomButton("RUN ALL","RUNALL",this);
   display_panel.addBottomButtons();
   display_table.getSelectionModel().addListSelectionListener(new Selector());
}



/********************************************************************************/
/*										*/
/*	Action handlers 							*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();
   if (cmd == null) return;
   if (cmd.equals("ALL")) {
      batt_model.setDisplayMode(ALL_MODE);
    }
   else if (cmd.equals("PENDING")) {
      batt_model.setDisplayMode(PENDING_MODE);
    }
   else if (cmd.equals("FAIL")) {
      batt_model.setDisplayMode(FAIL_MODE);
    }
   else if (cmd.equals("RUN")) {
      BumpClient bc = BumpClient.getBump();
      bc.saveAll();
      int [] sel = display_table.getSelectedRows();
      if (sel.length >= 1) {
         Set<BattTestCase> cases = new TreeSet<>();
         for (int i : sel) {
            BattTestCase btc2 = display_table.getActualTestCase(i);
            if (btc2 != null) cases.add(btc2);
          }
         BattFactory.getFactory().runTests(cases);
       }
      else {
         BattFactory.getFactory().runTests(current_runtype);
       }
    }
   else if (cmd.equals("RUNALL")) {
      BumpClient bc = BumpClient.getBump();
      bc.saveAll();
      BattFactory.getFactory().runTests(current_runtype);
    }
   else if (cmd.equals("STOP")) {
      BattFactory.getFactory().stopTest();
    }
   else {
      BoardLog.logE("BATT","Unknown action " + cmd);
    }
}



@Override public void battModelUpdated(BattModeler bm)
{
   if (display_bar != null) {
      // display_bar.repaint();
      repaint();
    }
}




/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();
   
   Point pt = new Point(e.getXOnScreen(),e.getYOnScreen());
   SwingUtilities.convertPointFromScreen(pt,display_table);
   BattTestCase btc = display_table.findTestCase(pt);
   
   int [] sel = display_table.getSelectedRows();
   if (sel.length == 1) {
      BattTestCase btc1 = display_table.getActualTestCase(sel[0]);
      if (btc1 != btc) {
         menu.add(new RunTestAction(btc1));
       }
    }
   else if (sel.length > 1) {
      Set<BattTestCase> cases = new TreeSet<>();
      for (int i : sel) {
         BattTestCase btc2 = display_table.getActualTestCase(i);
         if (btc2 != null) cases.add(btc2);
       }
      menu.add(new RunSelectedAction(cases));
    }
   
   display_table.getSelectedRows();


   if (btc != null) {
      menu.add(new SourceAction(btc));
      menu.add(new DebugAction(btc));
      menu.add(new RunTestAction(btc));
      if (btc.getStatus() == TestStatus.FAILURE && btc.getFailTrace() != null) {
	 menu.add(new ShowStackAction(btc));
	 menu.add(new GotoFailureAction(btc));
       }
    }

   BattFactory.getFactory().handleExternalPopups(menu,this,btc);

   if (current_mode == null) {
      current_mode = BattFactory.getFactory().getTestMode();
    }
   JMenu m2 = new JMenu("Test Running Mode");
   m2.add(new ModeAction(TestMode.ON_DEMAND));
   m2.add(new ModeAction(TestMode.CONTINUOUS));
   menu.add(m2);

   JMenu m1 = new JMenu("Run Option");
   m1.add(new RunAction(RunType.ALL));
   m1.add(new RunAction(RunType.FAIL));
   m1.add(new RunAction(RunType.PENDING));
   menu.add(m1);

   menu.add(new StopAction());
   menu.add(new UpdateAction());

   menu.add(getFloatBubbleAction());

   // TODO: provide rerun, debug, ... options

   menu.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*                                                                              */
/*      Actions and listeners                                                   */
/*                                                                              */
/********************************************************************************/

private class Selector implements ListSelectionListener {
   
   @Override public void valueChanged(ListSelectionEvent e) {
      int ct = display_table.getSelectedRowCount();
      run_selected.setEnabled(ct > 0);
    }
   
}       // end of inner class Selector



private class DebugAction extends AbstractAction {

   private BattTestCase test_case;
   private static final long serialVersionUID = 1;
   
   DebugAction(BattTestCase tc) {
      super("Debug " + tc.getName());
      test_case = tc;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BumpLaunchConfig blc = BattFactory.getLaunchConfigurationForTest(test_case);
      if (blc == null) return;
   
      BddtFactory.getFactory().newDebugger(blc);
    }

}	// end of inner class DebugAction




/********************************************************************************/
/*										*/
/*	Outputer methods							*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BATT"; }

@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","TESTSTATUS");
}



/********************************************************************************/
/*										*/
/*	Display bar class							*/
/*										*/
/********************************************************************************/

enum BarType {
   SUCCESS,
   NEED_SUCCESS,
   RUNNING,
   PENDING,
   CANT_RUN,
   STOPPED,
   NEED_FAILURE,
   FAILURE,
   IGNORED
}

private static final int NUM_BAR_TYPES = 8;
private static final int BAR_INSET = 3;

private static final Paint [] bar_colors;

private static Pattern LOCATION_PATTERN =
   Pattern.compile("at ([a-zA-Z0-9<>$_.]+)\\(([a-zA-Z0-9_]+\\.java)\\:([0-9]+)\\)");

static {
   bar_colors = new Paint[BarType.values().length];
   for (BarType bt : BarType.values()) {
      Color c1 = BoardColors.getColor("Batt.BarType." + bt.toString());
      bar_colors[bt.ordinal()] = c1;
    }
}


private BarType getTestType(BattTestCase btc)
{
   switch (btc.getState()) {
      case STOPPED :
	 return BarType.STOPPED;
      case CANT_RUN :
	 return BarType.CANT_RUN;
      case EDITED :
      case NEEDS_CHECK :
	 switch (btc.getStatus()) {
	    case SUCCESS :
	       return BarType.NEED_SUCCESS;
	    case FAILURE :
	       return BarType.NEED_FAILURE;
	    default :
	       return BarType.PENDING;
	  }
      default :
      case PENDING :
	 return BarType.PENDING;
      case TO_BE_RUN :
      case RUNNING :
	 return BarType.RUNNING;
      case UP_TO_DATE :
	 switch (btc.getStatus()) {
	    case SUCCESS :
	       return BarType.SUCCESS;
	    case FAILURE :
	       return BarType.FAILURE;
	    default :
	       return BarType.PENDING;
	  }
      case IGNORED :
	 return BarType.IGNORED;
    }
}



private class DisplayBar extends JPanel {

   private static final long serialVersionUID = 1;

   DisplayBar() {
      setPreferredSize(new Dimension(300,30));
      setMinimumSize(new Dimension(100,16));
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      List<BattTestCase> tests = batt_model.getAllTests();

      int counts[] = new int[NUM_BAR_TYPES];
      Arrays.fill(counts,0);
      double total = 0;

      for (BattTestCase btc :tests) {
	 BarType bt = getTestType(btc);
	 counts[bt.ordinal()]++;
	 total++;
       }

      Dimension d = getSize();
      double y0 = BAR_INSET;
      double y1 = d.height - BAR_INSET;
      double x0 = BAR_INSET;
      double x1 = d.width - BAR_INSET;
      double x = x0;
      Rectangle2D r2 = new Rectangle2D.Double(x0,y0,x1-x0,y1-y0);
      g2.setColor(BoardColors.getColor("Batt.BarTypeNone"));
      g2.fill(r2);
      for (int i = 0; i < NUM_BAR_TYPES; ++i) {
	 if (counts[i] == 0) continue;
	 double w = (x1-x0)*counts[i]/total;
	 g2.setPaint(bar_colors[i]);
	 r2.setRect(x,y0,w,y1-y0);
	 g2.fill(r2);
	 x += w;
       }
    }

}	// end of inner class DisplayBar





/********************************************************************************/
/*										*/
/*	Table for displaying tests						*/
/*										*/
/********************************************************************************/

private class DisplayTable extends JTable implements MouseListener {

   private static final long serialVersionUID = 1;

   DisplayTable() {
      super(batt_model.getTableModel());
      setShowGrid(true);
      setPreferredScrollableViewportSize(new Dimension(350,100));
      setAutoCreateRowSorter(true);
      setColumnSelectionAllowed(false);
      setDragEnabled(false);
      setFillsViewportHeight(true);
   
      getColumnModel().getColumn(0).setMinWidth(BATT_STATUS_COL_MIN_WIDTH);
      getColumnModel().getColumn(0).setMaxWidth(BATT_STATUS_COL_MAX_WIDTH);
   
      getColumnModel().getColumn(1).setMinWidth(BATT_STATE_COL_MIN_WIDTH);
      getColumnModel().getColumn(1).setMaxWidth(BATT_STATE_COL_MAX_WIDTH);
   
      getColumnModel().getColumn(2).setPreferredWidth(BATT_CLASS_COL_PREF_WIDTH);
      getColumnModel().getColumn(3).setPreferredWidth(BATT_NAME_COL_PREF_WIDTH);
   
      setCellSelectionEnabled(false);
      setRowSelectionAllowed(true);
      setColumnSelectionAllowed(false);
      addMouseListener(this);
    }

   @Override public boolean getScrollableTracksViewportWidth()		{ return true; }

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() < 2) return;
      BattTestCase btc = findTestCase(e.getPoint());
      if (btc == null) return;
      // show bubble for that test case
      SourceAction sa = new SourceAction(btc);
      sa.actionPerformed(null);
    }

   @Override public void mouseEntered(MouseEvent _e)			{ }
   @Override public void mouseExited(MouseEvent _e)			{ }
   @Override public void mouseReleased(MouseEvent e)			{ }
   @Override public void mousePressed(MouseEvent e)			{ }

   @Override public boolean isCellEditable(int r,int c) 		{ return false; }

   @Override public String getToolTipText(MouseEvent e) {
      BattTestCase btc = findTestCase(e.getPoint());
      if (btc == null) return null;
      int col = columnAtPoint(e.getPoint());
      TableColumn tc = getColumnModel().getColumn(col);
      if (tc != null) {
	 String ch = tc.getIdentifier().toString();
	 switch (ch) {
	    case "Status" :
	    case "State" :
	       break;
	    default :
	       return null;
	  }
       }
      return btc.getToolTip();
    }

   @Override public JToolTip createToolTip() {
      return new BudaToolTip();
    }

   BattTestCase getActualTestCase(int row) {
      RowSorter<?> rs = getRowSorter();
      if (rs != null) row = rs.convertRowIndexToModel(row);
      return batt_model.getTestCase(row);
    }

   BattTestCase findTestCase(Point p) {
      int row = rowAtPoint(p);
      if (row < 0) return null;
      return getActualTestCase(row);
    }

}	// end of inner class DisplayTable



/********************************************************************************/
/*										*/
/*	Runner to set up batt							*/
/*										*/
/********************************************************************************/

private static class SetupBatt implements Runnable {

   @Override public void run() {
      BattFactory.getFactory().startBattServer();
      BattFactory.getFactory().updateTests();
    }

}	// end of inner class SetupBatt



/********************************************************************************/
/*										*/
/*	Actions 								*/
/*										*/
/********************************************************************************/

private class SourceAction extends AbstractAction {

   private BattTestCase test_case;
   private static final long serialVersionUID = 1;
   
   SourceAction(BattTestCase btc) {
      super("Open " + btc.getMethodName());
      test_case = btc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String nm = test_case.getClassName() + "." + test_case.getMethodName();
      BaleFactory bf = BaleFactory.getFactory();
      BudaBubble bb = bf.createMethodBubble(null,nm);
      if (bb == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BattStatusBubble.this);
      if (bba != null) {
	 bba.addBubble(bb,BattStatusBubble.this,null,PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class SourceAction


private class RunTestAction extends AbstractAction {

   private BattTestCase test_case;
   private static final long serialVersionUID = 1;
   
   RunTestAction(BattTestCase btc) {
      super("Run Test " + btc.getName());
      test_case = btc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BattFactory.getFactory().runTest(test_case);
    }

}	// end of inner class RunTestAction



private class RunSelectedAction extends AbstractAction {
   
   private List<BattTestCase> test_cases;
   private static final long serialVersionUID = 1;
   
   RunSelectedAction(Collection<BattTestCase> cases) {
      super("Run Selected Tests");
      test_cases = new ArrayList<>(cases);
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      BattFactory.getFactory().runTests(test_cases);
    }
   
}       // end of inner class RunSelectedAction



private class ShowStackAction extends AbstractAction {

   private BattTestCase test_case;
   private static final long serialVersionUID = 1;
   
   ShowStackAction(BattTestCase btc) {
      super("Show Failure for " + btc.getMethodName());
      test_case = btc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String result = test_case.getToolTip();
      BeamFactory bf = BeamFactory.getFactory();
      BeamNoteBubble bb = bf.createNoteBubble(result);
      if (bb == null) return;
      bb.setEditable(false);
      bb.setNoteColor(BoardColors.getColor("Batt.Status.Top"),
            BoardColors.getColor("Batt.Status.Bottom"));
      
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BattStatusBubble.this);
      if (bba != null) {
         bba.addBubble(bb,BattStatusBubble.this,null,PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class ShowStackAction



private class GotoFailureAction extends AbstractAction {

   private BattTestCase test_case;
   private static final long serialVersionUID = 1;
   
   GotoFailureAction(BattTestCase btc) {
      super("Goto Failure for " + btc.getMethodName());
      test_case = btc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String trace = test_case.getFailTrace();
      StringTokenizer tok = new StringTokenizer(trace,"\n\r\t");
      while (tok.hasMoreTokens()) {
         String line = tok.nextToken();
         if (line.length() == 0) continue;
         Matcher m = LOCATION_PATTERN.matcher(line);
         if (m.find()) {
            int lno = Integer.parseInt(m.group(3));
            String mthd = m.group(1);
            int idx = mthd.lastIndexOf(".");
            if (idx < 0) continue;
            String cls = mthd.substring(0,idx).replace("$",".");
            mthd = mthd.substring(idx+1);
            String nmthd = null;
            boolean iscnstr = false;
            if (mthd.equals("<init>")) {
               idx = cls.lastIndexOf(".");
               if (idx >= 0) mthd = cls.substring(idx+1);
               else mthd = cls;
               iscnstr = true;
               nmthd = cls;
             }
            else {
               nmthd = cls + "." + mthd;
             }
            BumpClient bc = BumpClient.getBump();
            List<BumpLocation> locs = bc.findMethods(null,nmthd,false,true,iscnstr,false);
            if (locs == null || locs.isEmpty()) continue;
            BumpLocation bl0 = locs.get(0);
            File f = bl0.getFile();
            if (!f.exists()) continue;
            BaleFactory bf = BaleFactory.getFactory();
            if (locs.size() > 1) {
               BaleConstants.BaleFileOverview bfo = bf.getFileOverview(null,f);
               if (bfo == null) continue;
               int loff = bfo.findLineOffset(lno);
               for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
        	  BumpLocation bl1 = it.next();
        	  if (bl1.getOffset() > loff || bl1.getEndOffset() < loff) it.remove();
        	}
               if (locs.size() == 0) continue;
             }
            bf.createBubbleStack(BattStatusBubble.this,null,null,false,locs,null);
            break;
          }
       }
    }

}	// end of inner class GotoFailureAction



private class ModeAction extends JRadioButtonMenuItem implements ActionListener {

   private TestMode test_mode;
   private static final long serialVersionUID = 1;
   
   ModeAction(TestMode md) {
      super(md.toString(),(md == current_mode));
      addActionListener(this);
      test_mode = md;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BattFactory.getFactory().setTestMode(test_mode);
      current_mode = test_mode;
    }

}	// end of inner class ModeAction



private class UpdateAction extends AbstractAction {

   private static final long serialVersionUID = 1;
   
   UpdateAction() {
      super("Update Test Set");
    }

   @Override public void actionPerformed(ActionEvent e) {
      batt_model.updateTestModel(null,true);
      BattFactory.getFactory().findNewTests();
    }

}	// end of inner class ModeAction



private class RunAction extends JRadioButtonMenuItem implements ActionListener {

   private RunType run_type;
   private static final long serialVersionUID = 1;
   
   RunAction(RunType typ) {
      super(typ.toString(),(typ == current_runtype));
      addActionListener(this);
      run_type = typ;
    }

   @Override public void actionPerformed(ActionEvent e) {
      current_runtype = run_type;
    }

}	// end of inner class RunAction




private static class StopAction extends AbstractAction {

   private static final long serialVersionUID = 1;
   
   StopAction() {
      super("Stop current test");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BattFactory.getFactory().stopTest();
    }

}	// end of inner class RunAction





}	// end of class BattStatusBubble




/* end of BattStatusBubble.java */

