/********************************************************************************/
/*										*/
/*		BrepairFaultBubble.java 					*/
/*										*/
/*	Bubbles allowing user to interact with bug repair process		*/
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



package edu.brown.cs.bubbles.brepair;

import java.awt.Color;
import java.awt.Component;
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
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleAnnotation;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.batt.BattConstants.BattTest;
import edu.brown.cs.bubbles.bicex.BicexFactory;
import edu.brown.cs.bubbles.bicex.BicexConstants.BicexCountData;
import edu.brown.cs.bubbles.bicex.BicexConstants.BicexResultContext;
import edu.brown.cs.bubbles.bicex.BicexConstants.BicexRunner;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.swing.SwingGridPanel;


class BrepairFaultBubble extends BudaBubble implements BrepairConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BattTest		failed_test;
private BrepairCountData	count_data;
private BrepairMethodTreeModel	tree_model;
private BrepairSeedeManager	seede_manager;
private FaultTree		fault_tree;
private JComponent		execution_bubble;
private Set<BicexResultContext> done_contexts;
private List<RepairAnnotation>	active_annotations;
private JButton 		correct_button;
private JButton 		incorrect_button;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BrepairFaultBubble(BattTest test,BrepairCountData cd)
{
   failed_test = test;
   count_data = cd;

   Set<File> files = cd.getRelevantFiles();

   tree_model = new BrepairMethodTreeModel(count_data);
   seede_manager = new BrepairSeedeManager(test,files);
   seede_manager.setup();
   execution_bubble = null;
   done_contexts = new HashSet<>();
   active_annotations = new ArrayList<>();

   JComponent cnts = setupPanel();
   setContentPane(cnts);
}


@Override public void localDispose()
{
   seede_manager.remove();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void handleRemove(BudaBubble bb)
{
   if (bb == execution_bubble) execution_bubble = null;
}




/********************************************************************************/
/*										*/
/*	Layout methods								*/
/*										*/
/********************************************************************************/

JComponent setupPanel()
{
   RepairPanel pnl = new RepairPanel();
   pnl.beginLayout();
   pnl.addBannerLabel("Repair Problem for " + failed_test.getName());
   pnl.addSeparator();
   fault_tree = new FaultTree();
   fault_tree.addTreeSelectionListener(new Selector());
   pnl.addLabellessRawComponent("TREE",new JScrollPane(fault_tree));

   pnl.addSeparator();
   pnl.addBottomButton("Show Evaluation","REFINE",new RefineAction());
   pnl.addBottomButton("Show Locations","SHOWALL",new ShowAllAction());
   correct_button = pnl.addBottomButton("Mark Items CORRECT","CORRECT",new LineMarker(true));
   correct_button.setEnabled(false);
   incorrect_button = pnl.addBottomButton("Mark Items INCORRECT","INCORRECT",new LineMarker(false));
   incorrect_button.setEnabled(false);
   pnl.addBottomButtons();

   pnl.addSeparator();
   BicexRunner br = seede_manager.getSeedeRunner();
   if (br != null) {
      execution_bubble = BicexFactory.getFactory().showExecution(br);
      pnl.addLabellessRawComponent("EXEC",execution_bubble);
    }

   return pnl;
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private void markContext(BicexResultContext ctx,boolean pass)
{
   BicexCountData bcd = ctx.getCountData();

   count_data.addUserFeedback(bcd,pass);

   String tnm = failed_test.getClassName() + "." + failed_test.getMethodName() + "(";
   count_data.computeSortedMethods(tnm,MAX_METHODS,CUTOFF_VALUE);
   tree_model.countsUpdated();

   // need to refresh table here
}




/********************************************************************************/
/*										*/
/*	Interaction methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent evt)
{
   JPopupMenu menu = new JPopupMenu();

   Point p = new Point(evt.getXOnScreen(),evt.getYOnScreen());
   SwingUtilities.convertPointFromScreen(p,this);
   Component c = SwingUtilities.getDeepestComponentAt(this,p.x,p.y);
   while (c != null) {
      if (c == fault_tree) {
	 addFaultTreeButtons(evt,menu);
	 break;
       }
      else if (c == execution_bubble) {
	 BicexFactory.getFactory().addToPopupMenu(execution_bubble,evt,menu);
	 BicexResultContext rctx = BicexFactory.getFactory().getCurrentContext(execution_bubble);
	 if (rctx != null && !done_contexts.contains(rctx)) {
	    menu.add(new ContextMarker(rctx,true));
	    menu.add(new ContextMarker(rctx,false));
	  }
	 break;
       }

      c = c.getParent();
    }

   menu.add(this.getFloatBubbleAction());

   menu.show(this,evt.getX(),evt.getY());
}



private void addFaultTreeButtons(MouseEvent evt,JPopupMenu menu)
{
   Point pt = new Point(evt.getXOnScreen(), evt.getYOnScreen());
   SwingUtilities.convertPointFromScreen(pt,fault_tree);
   TreePath tp = fault_tree.getPathForLocation(pt.x, pt.y);

   if (tp == null) {
      return;
    }

   String comp = (String) tp.getLastPathComponent();
   String method = null;
   int sline = 0;
   int eline = 0;
   if (comp.startsWith("Line ") || comp.startsWith("Lines ")) {
      method = (String) tp.getPathComponent(1);
      if (comp.startsWith("Line ")) {
	 sline = Integer.parseInt(comp.substring(5));
	 eline = sline;
       }
      else {
	 comp = comp.substring(6);
	 int idx = comp.indexOf("-");
	 sline = Integer.parseInt(comp.substring(0,idx));
	 eline = Integer.parseInt(comp.substring(idx+1));
       }
    }
   else {
      method = comp;
    }

   if (execution_bubble != null) {
      menu.add(new ShowMethodExecutionAction(method,sline,eline));
    }

   if (method != null) {
      menu.add(new ShowMethodAction(method));
    }

   if (sline > 0 && eline == sline) {
      menu.add(new AnnotationAction("Hightlight line " + sline,method,sline,sline));
    }
   else if (sline != eline) {
      menu.add(new AnnotationAction("Highlight lines " + sline + " to " + eline,method,sline,eline));
    }

   if (active_annotations.size() > 0) {
      // menu.add(new RemoveAnnotationAction());
    }
}



/********************************************************************************/
/*										*/
/*	Tree for displaying possibly faulty locations				*/
/*										*/
/********************************************************************************/

private class FaultTree extends JTree {

   private static final long serialVersionUID = 1;

   FaultTree() {
      super(tree_model);
      setVisibleRowCount(10);
      setRootVisible(false);
      setOpaque(false);
      setBackground(BoardColors.transparent());
      setToggleClickCount(2);
      setCellRenderer(new FaultTreeRenderer());
      TreeSelectionModel tsm = getSelectionModel();
      tsm.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
      setExpandsSelectedPaths(true);
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Color topc = BoardColors.getColor(BREPAIR_TOP_COLOR_ID);
      Color botc = BoardColors.getColor(BREPAIR_BOTTOM_COLOR_ID);
      if (topc != botc) {
	 Paint p = new GradientPaint(0f,0f,topc,0f,sz.height,botc);
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 g2.setPaint(p);
	 g2.fill(r);
       }
      else {
	 g2.setColor(topc);
	 g2.fillRect(0,0,sz.width,sz.height);
       }

      super.paintComponent(g);
    }

}	// end of inner class FaultTree



private class FaultTreeRenderer extends DefaultTreeCellRenderer {

   private static final long serialVersionUID  = 1;

   FaultTreeRenderer() {
      setBackgroundNonSelectionColor(BoardColors.transparent());
      setBackgroundSelectionColor(BoardColors.getColor("Brepair.SelectionColor"));
      setOpaque(true);
    }

   @Override public Component getTreeCellRendererComponent(JTree tree,
	 Object value,boolean sel,boolean exp,boolean leaf,int row,
	 boolean foc) {
      Component c = super.getTreeCellRendererComponent(tree,value,sel,exp,leaf,row,foc);
      JComponent jc = (JComponent) c;

      return jc;
    }

}	// end of FaultTreeRenderer



/********************************************************************************/
/*										*/
/*	Top level panel 							*/
/*										*/
/********************************************************************************/

private static class RepairPanel extends SwingGridPanel
{
   private static final long serialVersionUID = 1;

   RepairPanel() {
      setOpaque(false);
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();

      Color topc = BoardColors.getColor(BREPAIR_TOP_COLOR_ID);
      Color botc = BoardColors.getColor(BREPAIR_BOTTOM_COLOR_ID);
      if (topc != botc) {
	 Paint p = new GradientPaint(0f,0f,topc,0f,sz.height,botc);
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 g2.setColor(botc);		// solid fill first
	 g2.fill(r);
	 g2.setPaint(p);
	 g2.fill(r);
       }
      else {
	 g2.setColor(topc);
	 g2.fillRect(0,0,sz.width,sz.height);
       }

      super.paintComponent(g);
    }
}

/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private class RefineAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      if (execution_bubble == null) return;
      List<String> mthds = count_data.getSortedBlocks();
      BicexFactory bicex = BicexFactory.getFactory();
      for (String blk : mthds) {
	 String [] args = blk.split("@");
	 int sline = Integer.parseInt(args[1]);
	 int eline = Integer.parseInt(args[2]);
	 BicexResultContext ctx = bicex.findContext(execution_bubble,args[0],sline,eline);
	 if (ctx == null) continue;
	 if (done_contexts.contains(ctx)) continue;
	 bicex.gotoContext(execution_bubble,ctx);
	 break;
       }
    }

}	// end of inner class RefineAction





private class ContextMarker extends AbstractAction {

   private BicexResultContext for_context;
   private boolean mark_pass;

   private static final long serialVersionUID = 1;

   ContextMarker(BicexResultContext ctx,boolean pass) {
      super("This call of " + ctx.getShortName() + " is " + (pass ? "CORRECT" : "INCORRECT"));
      for_context = ctx;
      mark_pass = pass;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      done_contexts.add(for_context);
      markContext(for_context,mark_pass);
    }
}



private class LineMarker extends AbstractAction {

   private boolean do_pass;

   private static final long serialVersionUID = 1;

   LineMarker(boolean pass) {
      do_pass = pass;
    }

   @Override public void actionPerformed(ActionEvent evt) {
     TreePath [] tps = fault_tree.getSelectionPaths();
     if (tps == null) return;
     for (TreePath tp : tps) {
	String comp = (String) tp.getLastPathComponent();
	String method = null;
	int sline = 0;
	int eline = 0;
	if (comp.startsWith("Line ") || comp.startsWith("Lines ")) {
	   method = (String) tp.getPathComponent(1);
	   if (comp.startsWith("Line ")) {
	      sline = Integer.parseInt(comp.substring(5));
	      eline = sline;
	    }
	   else {
	      comp = comp.substring(6);
	      int idx = comp.indexOf("-");
	      sline = Integer.parseInt(comp.substring(0,idx));
	      eline = Integer.parseInt(comp.substring(idx+1));
	    }
	 }
	else {
	   method = comp;
	 }
	count_data.addUserFeedback(method,sline,eline,do_pass);
      }

     String tnm = failed_test.getClassName() + "." + failed_test.getMethodName() + "(";
     count_data.computeSortedMethods(tnm,MAX_METHODS,CUTOFF_VALUE);
     tree_model.countsUpdated();
    }

}	// end of inner class LineMarker


private class ShowAllAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BREPAIR","GotoAllSource");
      List<BumpLocation> locs = new ArrayList<>();
      BumpClient bc = BumpClient.getBump();
      for (String mnm : count_data.getSortedMethods()) {
	 List<BumpLocation> nlocs = bc.findMethod(null,mnm,false);
	 if (nlocs != null) locs.addAll(nlocs);
       }
      if (locs.size() > 0) {
	 BaleFactory.getFactory().createBubbleStack(BrepairFaultBubble.this,null,null,false,
	       locs,BudaLinkStyle.STYLE_DASHED);
       }
    }

}	// end of inner class ShowAllAction


private class ShowMethodAction extends AbstractAction {

   private String method_name;
   private static final long serialVersionUID = 1;

   ShowMethodAction(String method) {
      super("Open Editor on " + getShortMethodName(method));
      method_name = method;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BREPAIR","GotoSource");
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BrepairFaultBubble.this);
      String prpj = null;
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(prpj,method_name);
      bba.addBubble(bb,BrepairFaultBubble.this,null,
	    PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
    }

}	// end of inner class ShowMethodAction



private static String getShortMethodName(String name)
{
   int idx = name.indexOf("(");
   if (idx > 0) name = name.substring(0,idx);
   idx = name.lastIndexOf(".");
   if (idx > 0) name = name.substring(idx+1);
   return name;
}



/********************************************************************************/
/*										*/
/*	Show method execution action						*/
/*										*/
/********************************************************************************/

private class ShowMethodExecutionAction extends AbstractAction {

   private String method_name;
   private int start_line;
   private int end_line;
   private static final long serialVersionUID = 1;

   ShowMethodExecutionAction(String method,int s,int e) {
      super("Show evaluation of " + getShortMethodName(method));
      method_name = method;
      start_line = s;
      end_line = e;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BREPAIR","MethodExec");
      if (execution_bubble == null) return;
      BicexFactory.getFactory().gotoContext(execution_bubble,method_name,start_line,end_line);
    }

}	// end of inner class ShowMethodAction




/********************************************************************************/
/*										*/
/*	Line annotations							*/
/*										*/
/********************************************************************************/

private class AnnotationAction extends AbstractAction {

   private String method_name;
   private int from_line;
   private int to_line;

   private static final long serialVersionUID = 1;

   AnnotationAction(String msg,String method,int fr,int to) {
      super(msg);
      method_name = method;
      from_line = fr;
      to_line = to;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BREPAIR","ShowLines");
      if (evt != null) removeAnnotations();
      List<BumpLocation> bl = BumpClient.getBump().findMethod(null,method_name,false);
      if (bl == null || bl.size() == 0) return;
      BumpLocation loc = bl.get(0);
      BaleFileOverview fov = BaleFactory.getFactory().getFileOverview(loc.getProject(),loc.getFile());
      for (int ln = from_line; ln <= to_line; ++ln) {
	 RepairAnnotation ra = new RepairAnnotation(loc.getFile(),fov,ln);
	 BaleFactory.getFactory().addAnnotation(ra);
	 active_annotations.add(ra);
       }
    }

}	// end of inner class AnnotationAction



@SuppressWarnings("unused")
private class RemoveAnnotationAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RemoveAnnotationAction() {
      super("Remove Line Highlights");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BREPAIR","UnShowLines");
      removeAnnotations();
    }

}	// end of inner class RemoveAnnotationAction




private void removeAnnotations()
{
   for (RepairAnnotation ra : active_annotations) {
      BaleFactory.getFactory().removeAnnotation(ra);
    }
   active_annotations.clear();
}



private class RepairAnnotation implements BaleAnnotation {

   private File for_file;
   private int file_offset;

   RepairAnnotation(File fil,BaleFileOverview fov,int line) {
      for_file = fil;
      file_offset = fov.findLineOffset(line);
    }

   @Override public File getFile()				{ return for_file; }
   @Override public int getDocumentOffset()			{ return file_offset; }
   @Override public Icon getIcon(BudaBubble bbl)		{ return null; }
   @Override public String getToolTip() 			{ return "Selected fault localization area"; }
   @Override public Color getLineColor(BudaBubble bbl) {
      return BoardColors.getColor("Brepair.RepairAnnotColor");
    }
   @Override public Color getBackgroundColor()			{ return null; }
   @Override public boolean getForceVisible(BudaBubble bbl)	{ return true; }
   @Override public int getPriority()				{ return 10; }
   @Override public void addPopupButtons(Component b,JPopupMenu m)	{ }

}	// end of inner class RepairAnnotation



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

private class Selector implements TreeSelectionListener {

   @Override public void valueChanged(TreeSelectionEvent evt) {
      removeAnnotations();
      int ct = fault_tree.getSelectionCount();
      if (ct > 0) {
	 for (TreePath tp : fault_tree.getSelectionPaths()) {
	    String comp = (String) tp.getLastPathComponent();
	    String method = null;
	    int sline = 0;
	    int eline = 0;
	    if (comp.startsWith("Line ") || comp.startsWith("Lines ")) {
	       method = (String) tp.getPathComponent(1);
	       if (comp.startsWith("Line ")) {
		  sline = Integer.parseInt(comp.substring(5));
		  eline = sline;
		}
	       else {
		  comp = comp.substring(6);
		  int idx = comp.indexOf("-");
		  sline = Integer.parseInt(comp.substring(0,idx));
		  eline = Integer.parseInt(comp.substring(idx+1));
		}
	       AnnotationAction aa = new AnnotationAction("CLICK",method,sline,eline);
	       aa.actionPerformed(null);
	     }
	  }
       }

      if (ct == 0) {
	 correct_button.setEnabled(false);
	 incorrect_button.setEnabled(false);
       }
      else {
	 correct_button.setEnabled(true);
	 incorrect_button.setEnabled(true);
       }
    }

}	// end of inner class Selector


}	// end of class BrepairFaultBubble




/* end of BrepairFaultBubble.java */

