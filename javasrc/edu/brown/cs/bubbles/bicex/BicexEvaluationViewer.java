/********************************************************************************/
/*										*/
/*		BicexEvaluationBubble.java					*/
/*										*/
/*	description of class							*/
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


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingGridPanel;



class BicexEvaluationViewer extends SwingGridPanel implements BicexConstants,
	BicexConstants.BicexEvaluationUpdated, BudaConstants 
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BicexExecution	for_execution;

private BicexEvaluationContext	current_context;
private BicexDataModel		data_model;
private JLabel			breadcrumb_box;
private InnerLabel		inner_box;	
private JLabel			current_box;
private JLabel			status_box;
private BicexTimeScroller	time_scroller;
private List<BicexPanel>	viewer_panels;
private Map<String,BicexGraphicsPanel> graphics_panels;
private JTabbedPane		tab_pane;
private BicexPanel              history_panel;
private String                  user_tab;
private SwingEventListenerList<BicexPopupCallback> popup_listeners;

private static boolean auto_add_editors = true;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexEvaluationViewer(BicexExecution be)
{
   for_execution = be;
   if (be.getEvaluation() != null) {
      current_context = be.getEvaluation().getRootContext();
    }
   else current_context = null;

   data_model = new BicexDataModel(be,current_context);

   viewer_panels = new ArrayList<>();
   graphics_panels = new HashMap<>();
   history_panel = null;

   setupPanel();

   popup_listeners = new SwingEventListenerList<>(BicexPopupCallback.class);

   be.addUpdateListener(this);
}




protected void localDispose()
{
   if (for_execution != null) {
      for_execution.removeUpdateListener(this);
      for_execution.remove();
      BicexFactory.getFactory().removeExecution(for_execution);
      for (BicexPanel pnl : viewer_panels) {
	 pnl.removePanel();
       }
      for_execution = null;
    }
   BoardUserReport.noteReport("seede");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BicexExecution getExecution()			{ return for_execution; }

BicexDataModel getDataModel()			{ return data_model; }

BicexEvaluationContext getContext()		{ return current_context; }



void setHistoryPanel(BicexPanel pnl)
{
   if (history_panel == pnl) return;
   if (history_panel != null) {
      removePanel(history_panel);
      history_panel = null;
    } 
   if (pnl != null) {
      addPanel("Variable History",pnl);
      history_panel = pnl;
    }
}



void addPopupListener(BicexPopupCallback cb)
{
   popup_listeners.add(cb);
}

void removePopupListener(BicexPopupCallback cb)
{
   popup_listeners.remove(cb);
}


BicexEvaluationContext findContext(String id,int sline,int eline)
{
   BicexEvaluationContext root = current_context;
   if (root == null) return null;
   while (root.getParent() != null) root = root.getParent();
   BicexEvaluationContext nctx = findContext(root,id,sline,eline);
   return nctx;
}




private BicexEvaluationContext findContext(BicexEvaluationContext ctx,String id, int sline,int eline)
{
   if (ctx == null) return null;
   if (ctx.getId().equals(id) || ctx.getMethod().equals(id)) {
      if (sline <= 0) return ctx;
      BicexValue lnv = ctx.getValues().get("*LINE*");
      List<Integer> times = lnv.getTimeChanges();
      for (Integer t : times) {
	 String xv = lnv.getStringValue(t+1);
	 int line = Integer.parseInt(xv);
	 if (line == 0) continue;
	 if (line == sline) {
	    return ctx;
	  }
       }
    }
   if (ctx.getInnerContexts() != null) {
      for (BicexEvaluationContext cctx : ctx.getInnerContexts()) {
	 BicexEvaluationContext rctx = findContext(cctx,id,sline,eline);
	 if (rctx != null) return rctx;
       }
    }
   return null;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void evaluationUpdated(BicexRunner er)
{
   BicexExecution ex = (BicexExecution) er;
   current_context = ex.getCurrentContext();
   data_model.setContext(current_context);

   time_scroller.update();
   updateGraphicsPanels();

   for (BicexPanel bbl : viewer_panels) {
      bbl.update();
    }

   String parnm = null;
   for (BicexEvaluationContext pctx = getContext().getParent();
      pctx != null;
      pctx = pctx.getParent()) {
      String id = pctx.getShortName();
      if (parnm == null) parnm = id;
      else parnm = id + " > " + parnm;
    }
   if (parnm != null) parnm += " > ";
   breadcrumb_box.setText(parnm);

   current_box.setText(getContext().getFullShortName());

   updateInnerLabel();

   setStatus(getExecution().getEvaluation().getExitType());
}



private void updateGraphicsPanels()
{
   Set<BicexGraphicsPanel> done = new HashSet<>();

   BicexGraphicsModel gm = for_execution.getGraphicsModel();
   for (DisplayModel dm : gm.getActiveModels()) {
      String name = dm.getName();
      BicexGraphicsPanel bgp = graphics_panels.get(name);
      if (bgp == null) {
	 bgp = new BicexGraphicsPanel(this,dm);
	 String pname = getGraphicsPanelName(name);
	 graphics_panels.put(name,bgp);
	 addPanel(pname,bgp);
       }
      done.add(bgp);
    }

   for (Iterator<BicexGraphicsPanel> it = graphics_panels.values().iterator(); it.hasNext(); ) {
      BicexGraphicsPanel gp = it.next();
      if (!done.contains(gp)) {
         if (removePanel(gp)) {
            it.remove();
          }
       }
    }
}



private String getGraphicsPanelName(String name)
{
   if (name.startsWith("MAIN_")) {
      name = name.substring(5);
      // this is the thread name for a top-level window
    }
   else {
      int idx = name.lastIndexOf("?");
      if (idx > 0) name = name.substring(idx+1);
      int idx1 = name.indexOf("#");
      if (idx1 > 0) name = name.substring(0,idx1);
      // this is the variable name for a user-selected window
    }

   return name;
}


@Override public void evaluationReset(BicexRunner ex)
{
   setStatus(ExitType.PENDING);
}



@Override public void contextUpdated(BicexRunner bex)
{
   evaluationUpdated(bex);
}



@Override public void timeUpdated(BicexRunner run)
{
   BicexExecution bex = (BicexExecution) run;

   long t0 = time_scroller.getValue();
   long t1 = bex.getCurrentTime();
   if (t0 != t1) time_scroller.setValue((int) t1);

   for (BicexPanel bp : viewer_panels) {
      bp.updateTime();
    }

   updateInnerLabel();
}


@Override public void editorAdded(BudaBubble bbl)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(bbl);
   if (bba != bba1) return;
   File f = bbl.getContentFile();
   if (f == null) return;
   List<File> lf = Collections.singletonList(f);
   for_execution.addFiles(lf);
}




private void updateInnerLabel()
{
   long t0 = for_execution.getCurrentTime();
   BicexEvaluationContext ctx = current_context;
   if (ctx == null) return;

   while (ctx != null) {
      if (ctx.getInnerContexts() == null) break;
      BicexEvaluationContext nctx = null;
      for (BicexEvaluationContext sctx : ctx.getInnerContexts()) {
	 if (sctx.getStartTime() <= t0 && sctx.getEndTime() >= t0) {
	    nctx = sctx;
	    break;
	  }
       }
      if (nctx == null) break;
      ctx = nctx;
    }
   inner_box.setContext(ctx);
}


/********************************************************************************/
/*										*/
/*	Menu methods								*/
/*										*/
/********************************************************************************/

void addToPopupMenu(MouseEvent evt,JPopupMenu menu)
{
   BicexEvaluationContext ctx = getContext();

   if (evt.getSource() != this) {
      BudaBubble bbl = BudaRoot.findBudaBubble(this);
      MouseEvent cevt = SwingUtilities.convertMouseEvent(bbl,evt,this);
      evt = cevt;
    }
   
   if (ctx != null) {
      BicexEvaluationContext pctx = getContext().getParent();
      if (pctx != null) menu.add(getContextAction("Go to Parent Context",pctx));
    }

   Point p = new Point(evt.getXOnScreen(),evt.getYOnScreen());
   SwingUtilities.convertPointFromScreen(p,this);
   Component c = SwingUtilities.getDeepestComponentAt(this,p.x,p.y);

   check: while (c != null) {
      if (c == time_scroller) {
	 MouseEvent cevt = SwingUtilities.convertMouseEvent(this,evt,c);
	 time_scroller.handlePopupMenu(menu,cevt);
	 break;
       }
      else if (c == breadcrumb_box) {
	 break;
       }
      else if (c == inner_box) {
	 BicexEvaluationContext ictx = inner_box.getContext();
	 if (ictx != null && ictx != getContext()) {
	    menu.add(getContextAction("Go To " + ictx.getShortName(),ictx));
	  }
	 break;
       }
      else if (c == current_box) {
	 break;
       }
      else if (c == status_box) {
	 break;
       }
      else {
	 for (BicexPanel bp : viewer_panels) {
	    if (bp.getComponent() == c) {
	       MouseEvent cevt = SwingUtilities.convertMouseEvent(this,evt,c);
	       bp.handlePopupMenu(menu,cevt);
	       break check;
	     }
	  }
       }
      c = c.getParent();
    }

   BicexEvaluationContext cctx = getContext();
   BicexEvaluationContext rctx = cctx;
   while (rctx.getParent() != null) rctx = rctx.getParent();
   addContextButtons(menu,cctx,null,rctx);

   if (!auto_add_editors) menu.add(new AddOpenEditorsAction());

   if (ctx != null) {
      AbstractAction act = getSourceAction(ctx);
      if (act != null) menu.add(act);
    }

   if (ctx != null) {
      for (BicexPopupCallback cb : popup_listeners) {
	 cb.addPopupButtons(menu,ctx,ctx.getMethod(),getExecution().getCurrentTime());
       }
    }
}





private BicexEvaluationContext addContextButtons(JPopupMenu menu,
      BicexEvaluationContext ctx,
      BicexEvaluationContext prev,
      BicexEvaluationContext cur)
{
   if (cur == ctx) {
      if (prev != null) {
	 String lbl = "Go to Previous Call of " + ctx.getShortName();
	 menu.add(getContextAction(lbl,prev));
       }
      prev = cur;
    }
   else if (cur.getMethod().equals(ctx.getMethod())) {
      if (prev == ctx) {
	 String lbl = "Go to Next Call of " + ctx.getShortName();
	 menu.add(getContextAction(lbl,cur));
       }
      prev = cur;
    }
   if (cur.getInnerContexts() != null) {
      for (BicexEvaluationContext cctx : cur.getInnerContexts()) {
	 prev = addContextButtons(menu,ctx,prev,cctx);
       }
    }
   return prev;
}







/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   BicexEvaluationResult rslt = for_execution.getEvaluation();

   breadcrumb_box = new BreadcrumbLabel();
   inner_box = new InnerLabel();

   current_box = new CurrentLabel();
   status_box = new StatusLabel();
   status_box.setOpaque(true);

   time_scroller = new BicexTimeScroller(this);

   if (rslt != null) {
      setStatus(rslt.getExitType());
      if (current_context != null) {
	 current_box.setText(current_context.getFullShortName());
       }
    }
   else {
      setStatus(ExitType.PENDING);
    }


   tab_pane = new JTabbedPane(JTabbedPane.BOTTOM);
   addPanel("Variables",new BicexViewerPanel(this));
   addPanel("Line Graph",new BicexLineGraphPanel(this));
   addPanel("Call Tree",new BicexCallGraphPanel(this,true));
   addPanel("Call Graph",new BicexCallGraphPanel(this,false));
   addPanel("Stack View",new BicexStackViewPanel(this));
   addPanel("Data View",new BicexDataViewPanel(this));
   addPanel("Output",new BicexOutputPanel(this));
   addPanel(null,new BicexEvaluationAnnot(this));
   tab_pane.addChangeListener(new TabChanged());

   int y = 0;
   Box bx = Box.createHorizontalBox();
   bx.add(breadcrumb_box);
   bx.add(Box.createHorizontalGlue());
   bx.add(inner_box);

   addGBComponent(bx,0,y++,0,1,10,0);
   addGBComponent(new JSeparator(),0,y++,0,1,10,0);
   addGBComponent(current_box,0,y,1,1,10,0);
   addGBComponent(status_box,1,y++,1,1,0,0);
   addGBComponent(new JSeparator(),0,y++,0,1,10,0);

   addGBComponent(tab_pane,0,y,0,1,10,10);

   addGBComponent(new JSeparator(),0,y++,0,1,10,0);
   addGBComponent(time_scroller,0,y++,0,1,10,0);

   setPreferredSize(new Dimension(BICEX_EVAL_WIDTH,BICEX_EVAL_HEIGHT));
}



private void addPanel(String id,BicexPanel pnl)
{
   Component cmp = pnl.getComponent();
   if (cmp != null && id != null) {
      if (!pnl.useHeavyScroller()) {
	 JScrollPane jsp = new JScrollPane(cmp);
	 jsp.setWheelScrollingEnabled(pnl.allowMouseWheelScrolling());
	 tab_pane.addTab(id,new JScrollPane(cmp));
      }
      else {
	 ScrollPane spn = new ScrollPane();
	 spn.add(cmp);
	 tab_pane.addTab(id,spn);
      }
    }
   viewer_panels.add(pnl);
   if (user_tab != null && user_tab.equals(id)) {
      for (int i = 0; i < tab_pane.getTabCount(); ++i) {
         String key = tab_pane.getTitleAt(i);
         if (key != null && key.equals(id)) {
            tab_pane.setSelectedIndex(i);
            break;
          }
       }
    }
}



private boolean removePanel(BicexPanel pnl)
{
   Component cmp = pnl.getComponent();
   for (int i = 0; i < tab_pane.getTabCount(); ++i) {
      Component c1 = tab_pane.getComponentAt(i);
      if (c1 instanceof JScrollPane) {
	 JScrollPane jsp = (JScrollPane) c1;
	 c1 = jsp.getViewport().getView();
       }
      else if (c1 instanceof ScrollPane) {
	 ScrollPane sp = (ScrollPane) c1;
	 Panel p1 = (Panel) sp.getComponent(0);
	 c1 = p1.getComponent(0);
      }
      if (c1 == cmp) {
         String key = tab_pane.getTitleAt(i);
         if (key != null && key.equals(user_tab)) return false;
	 tab_pane.remove(i);
	 break;
       }
    }
   viewer_panels.remove(pnl);
   
   return true;
}



private void setStatus(ExitType sts)
{
   status_box.setText(sts.toString());
   Color bkg = BoardColors.getColor("Bicex.StatusDefault");
   switch (sts) {
      case NONE :
	 break;
      case ERROR :
      case EXCEPTION :
	 bkg = BoardColors.getColor("Bicex.StatusError");
	 break;
      case TIMEOUT :
      case COMPILER_ERROR :
	 bkg = BoardColors.getColor("Bicex.StatusTimeout");
	 break;
      case RETURN :
      case HALTED :
      case WAIT :
	 bkg = BoardColors.getColor("Bicex.StatusNormal");
	 break;
      case PENDING :
	 bkg = BoardColors.getColor("Bicex.StatusPending");
	 break;
    }
   status_box.setBackground(bkg);
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

BicexEvaluationContext getContextForTime(BicexEvaluationContext ctx,long time)
{
   if (ctx == null) return null;
   if (ctx.getStartTime() > time || ctx.getEndTime() < time) return null;
   if (ctx.getInnerContexts() == null) return ctx;

   for (BicexEvaluationContext ictx : ctx.getInnerContexts()) {
      BicexEvaluationContext xctx = getContextForTime(ictx,time);
      if (xctx != null) return xctx;
    }
   return ctx;
}



BicexEvaluationContext getContextForTime(long time)
{
   BicexEvaluationContext ctx = getContext();
   if (ctx == null) return null;
   while (ctx.getParent() != null) ctx = ctx.getParent();

   ctx = getContextForTime(ctx,time,-1);

   return ctx;
}


BicexEvaluationContext getContextForTime(BicexEvaluationContext ctx,long time,int lvl)
{
   if (ctx == null) return null;
   if (ctx.getStartTime() > time || ctx.getEndTime() < time) return null;
   if (lvl == 0) return ctx;
   if (ctx.getInnerContexts() == null) {
      if (lvl < 0) return ctx;
      return null;
    }

   for (BicexEvaluationContext ictx : ctx.getInnerContexts()) {
      BicexEvaluationContext xctx = getContextForTime(ictx,time,lvl-1);
      if (xctx != null) return xctx;
    }

   if (lvl < 0) return ctx;
   return null;
}



/********************************************************************************/
/*										*/
/*	Input handling methods							*/
/*										*/
/********************************************************************************/

@Override public String inputRequest(BicexRunner bex,String file)
{
   String input = JOptionPane.showInputDialog(this,"Input requested from " + file);
   return input;
}



/********************************************************************************/
/*										*/
/*	Initial value requests							*/
/*										*/
/********************************************************************************/

@Override public String valueRequest(BicexRunner bex,String what)
{
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Painting methods                                                        */
/*                                                                              */
/********************************************************************************/
   
@Override public void paintComponent(Graphics g) 
{
   Graphics2D g2 = (Graphics2D) g;
   Dimension sz = getSize();
   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   Color c1 = BoardColors.getColor("Bicex.PanelTopColor");
   Color c2 = BoardColors.getColor("Bicex.PanelBottomColor");
   if (c1 != c2) {
      Paint p = new GradientPaint(0f,0f,c1,0f,sz.height,c2);
      g2.setPaint(p);
    }
   else {
      g2.setColor(c1);
    }
   g2.fill(r);
   
   super.paintComponent(g);
}




/********************************************************************************/
/*										*/
/*	Labels with tool tips							*/
/*										*/
/********************************************************************************/

private class StatusLabel extends JLabel {

   private static final long serialVersionUID = 1;

   StatusLabel() {
      setOpaque(true);
      setToolTipText("");
    }

   @Override public String getToolTipText(MouseEvent evt) {
      if (getExecution().getEvaluation() == null ||
            getExecution().getEvaluation().getExitType() == null)
         return "Waiting for initial execution to complete";
   
      switch (getExecution().getEvaluation().getExitType()) {
         case COMPILER_ERROR :
            return "Execution stopped at compiler error";
         case ERROR :
            return "Execution stopped because: " +
            getExecution().getEvaluation().getExitMessage();
         case EXCEPTION :
            return "Execution stopped with uncaught: " +
            getExecution().getEvaluation().getExitMessage();
         case HALTED :
            return "Execution stopped because of System.exit";
         case WAIT :
            return "Execution stopped because Object.wait called";
         case NONE :
            break;
         case PENDING :
            return "Exectuion being computed";
         case RETURN :
            return "Execution stopped when routine returned";
         case TIMEOUT :
            return "Execution stopped due to time out";
       }
      return "";
    }

}	// end of inner class StatusLabel



private class CurrentLabel extends JLabel {

   private static final long serialVersionUID = 1;

   CurrentLabel() {
      super("Current Function Pending");
      setToolTipText("");
    }

   @Override public String getToolTipText(MouseEvent evt) {
      if (getContext() == null) return "Waiting for initial execution";
      return getContext().getMethod();
    }

}	// end of inner class CurrentLabel


private interface ClickLabel {

   void handleClick(int x);

}	// end of inner class ClickLabel




private class BreadcrumbLabel extends JLabel implements ClickLabel {

   private static final long serialVersionUID = 1;

   BreadcrumbLabel() {
      setToolTipText("");
      Font ft = getFont();
      ft = ft.deriveFont(ft.getSize2D()*0.75f);
      setFont(ft);
      addMouseListener(new LabelMouser());
    }

   @Override public String getToolTipText(MouseEvent evt) {
      if (getContext() == null) return "Waiting for initial execution";
      String rslt = "";
      for (BicexEvaluationContext ctx = getContext(); ctx != null; ctx = ctx.getParent()) {
	 String elt = "<LI>" + ctx.getFullShortName() + "</LI>";
	 rslt = elt + rslt;
       }
      rslt = "<HTML><UL>" + rslt + "</UL>";
      return rslt;
    }

   @Override public void handleClick(int x) {
      AffineTransform atx = new AffineTransform();
      FontRenderContext ctx = new FontRenderContext(atx,false,false);
      String s = getText();
      if (s == null) return;
      int pv = 0;
      int ct = 0;
      for ( ; ; ) {
         int idx = s.indexOf(">",pv);
         if (idx < 0) break;
         Rectangle2D r2 = getFont().getStringBounds(s,0,idx-1,ctx);
         if (r2.getWidth() >= x) break;
         ++ct;
         pv = idx+1;
       }
      String [] itms = s.split(">");
      int back = itms.length - ct -1;
      BicexEvaluationContext ectx = current_context;
      for (int i = 0; i < back; ++i) {
         ectx = ectx.getParent();
       }
      if (ectx != null) for_execution.setCurrentContext(ectx);
    }

}	// end of inner class BreadcrumbLabel



private class InnerLabel extends JLabel implements ClickLabel {

   private BicexEvaluationContext for_context;

   private static final long serialVersionUID = 1;

   InnerLabel() {
      setToolTipText("");
      Font ft = getFont();
      ft = ft.deriveFont(ft.getSize2D()*0.75f);
      setFont(ft);
      addMouseListener(new LabelMouser());
      for_context = null;
    }

   void setContext(BicexEvaluationContext ctx) {
      for_context = ctx;
      setText(ctx.getShortName());
    }

   BicexEvaluationContext getContext()		{ return for_context; }

   @Override public String getToolTipText(MouseEvent evt) {
      if (getContext() == null) return "Waiting for initial execution";
      String rslt = "";
      return rslt;
    }

   @Override public void handleClick(int x) {
      if (for_context == null) return;
      if (for_context == current_context) return;
      for_execution.setCurrentContext(for_context);
    }

}	// end of inner class InnerLabel



private class LabelMouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent evt) {
      if (evt.getClickCount() > 1) return;
      if (evt.getButton() != MouseEvent.BUTTON1) return;

      if (evt.getSource() instanceof ClickLabel) {
	 ClickLabel cl = (ClickLabel) evt.getSource();
	 cl.handleClick(evt.getX());
       }
    }

}	// end of inner class Mouser




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

AbstractAction getContextAction(String msg,BicexEvaluationContext ctx)
{
   return new GotoContextAction(msg,ctx);
}

AbstractAction getTimeAction(String msg,long time)
{
   return new GotoTimeAction(msg,time);
}


AbstractAction getContextTimeAction(String msg,BicexEvaluationContext ctx,long time)
{
   if (ctx == null && time >= 0) ctx = getContextForTime(time);
   else if (ctx != null && time < 0) time = ctx.getEndTime();

   return new GotoContextTimeAction(msg,ctx,time);
}

AbstractAction getSourceAction(BicexEvaluationContext ctx)
{
   return new GotoSourceAction(ctx);
}


private class GotoContextAction extends AbstractAction {

   private BicexEvaluationContext target_context;

   private static final long serialVersionUID = 1;

   GotoContextAction(String msg,BicexEvaluationContext ctx) {
      super(msg);
      target_context = ctx;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (target_context == null) return;
      BoardMetrics.noteCommand("BICEX","GotoContext");
      getExecution().setCurrentContext(target_context);
    }

}	// end of inner class GotoContextAction


private class GotoTimeAction extends AbstractAction {

   private long target_time;

   private static final long serialVersionUID = 1;

   GotoTimeAction(String msg,long time) {
      super(msg);
      target_time = time;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BICEX","GotoTime");
      getExecution().setCurrentTime(target_time);
    }

}	// end of inner class GotoTimeAction




private class GotoContextTimeAction extends AbstractAction {

   private BicexEvaluationContext target_context;
   private long target_time;

   private static final long serialVersionUID = 1;

   GotoContextTimeAction(String msg,BicexEvaluationContext ctx,long time) {
      super(msg);
      target_context = ctx;
      target_time = time;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (target_context == null && target_time < 0) return;
      BoardMetrics.noteCommand("BICEX","GotoTimeContext");
      getExecution().setCurrentContext(target_context);
      getExecution().setCurrentTime(target_time);
    }

}	// end of inner class GotoContextTimeAction




private class GotoSourceAction extends AbstractAction {

   private BicexEvaluationContext target_context;

   private static final long serialVersionUID = 1;

   GotoSourceAction(BicexEvaluationContext ctx) {
      super("Open Editor for " + ctx.getFullShortName());
      target_context = ctx;;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BICEX","GotoSource");
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BicexEvaluationViewer.this);
      String proj = null;
      String mid = target_context.getMethod();
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,mid);
      if (bb == null) return;
      bba.addBubble(bb,BicexEvaluationViewer.this,null,
	    PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
    }

}	// end of inner class GotoTimeAction


/********************************************************************************/
/*										*/
/*	Add File actions							*/
/*										*/
/********************************************************************************/

private class AddOpenEditorsAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   AddOpenEditorsAction() {
      super("Add Open Editors");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BicexEvaluationViewer.this);
      Point pt = BicexEvaluationViewer.this.getLocation();
      Set<File> files = BicexFactory.computeRegionFiles(bba,pt);
      if (files.isEmpty()) return;
      BoardMetrics.noteCommand("BICEX","AddOpenEditors");
      getExecution().addFiles(files);
    }

}	// end of inner class AddOpenEditorsAction



/********************************************************************************/
/*                                                                              */
/*      Track user component choices                                            */
/*                                                                              */
/********************************************************************************/

private class TabChanged implements ChangeListener
{
   @Override public void stateChanged(ChangeEvent evt) {
      int sel = tab_pane.getSelectedIndex();
      String ttl = tab_pane.getTitleAt(sel);
      user_tab = ttl;
    }
}


}	// end of class BicexEvaluationViewer




/* end of BicexEvaluationBubble.java */

