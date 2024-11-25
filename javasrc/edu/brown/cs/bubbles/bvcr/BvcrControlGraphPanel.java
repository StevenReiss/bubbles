/********************************************************************************/
/*										*/
/*		BvcrControlGraphPanel.java					*/
/*										*/
/*	Graphical view of program versions					*/
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

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.petal.PetalArcDefault;
import edu.brown.cs.ivy.petal.PetalArcEnd;
import edu.brown.cs.ivy.petal.PetalArcEndDefault;
import edu.brown.cs.ivy.petal.PetalEditor;
import edu.brown.cs.ivy.petal.PetalLayoutMethod;
import edu.brown.cs.ivy.petal.PetalLevelLayout;
import edu.brown.cs.ivy.petal.PetalModelDefault;
import edu.brown.cs.ivy.petal.PetalNode;
import edu.brown.cs.ivy.petal.PetalNodeDefault;
import edu.brown.cs.ivy.petal.PetalUndoSupport;
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;



class BvcrControlGraphPanel extends BudaBubble implements BvcrConstants,
	BvcrConstants.BvcrProjectUpdated, BudaConstants.BudaBubbleOutputer
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BvcrControlPanel	control_panel;
private BvcrControlVersion	current_version;
private JButton 		change_button;
private VersionHistoryGraph	petal_graph;
private static final long serialVersionUID = 1;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrControlGraphPanel(BvcrControlPanel pnl)
{
   control_panel = pnl;
   current_version = null;
   if (pnl.getVersionMap() != null) current_version = pnl.getVersionMap().get("HEAD");

   SwingGridPanel tpnl = new SwingGridPanel();
   tpnl.beginLayout();
   petal_graph = new VersionHistoryGraph();
   tpnl.addLabellessRawComponent("GRAPH",petal_graph);
   tpnl.addSeparator();
   tpnl.addBottomButton("New Version","NEW",null);
   change_button = tpnl.addBottomButton("Change","CHANGE",new ChangeVersionAction());
   tpnl.addBottomButton("Show Table","TABLE",new ShowTableAction());
   tpnl.addBottomButtons();
   change_button.setEnabled(false);

   setContentPane(tpnl);

   setCurrentSelection();

   pnl.addUpdateListener(this);
   petal_graph.update();
}


@Override protected void localDispose()
{
   control_panel.removeUpdateListener(this);
}



/********************************************************************************/
/*                                                                              */
/*      Bubble actions                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e) 
{
   JPopupMenu menu = new JPopupMenu();
   menu.add(getFloatBubbleAction());
   menu.show(this,e.getX(),e.getY());
}



/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

@Override public void projectUpdated(BvcrControlPanel pnl)
{
   current_version = null;
   if (pnl.getVersionMap() != null) current_version = pnl.getVersionMap().get("HEAD");

   petal_graph.update();
   setCurrentSelection();
}



private void setCurrentSelection()
{
   // output_table.clearSelection();
   change_button.setEnabled(false);

   if (current_version != null) {
      // section node for version
    }
}



/********************************************************************************/
/*										*/
/*	Graph panel								*/
/*										*/
/********************************************************************************/

private class VersionHistoryGraph extends JPanel {

   private PetalEditor		petal_editor;
   private PetalModelDefault	petal_model;
   private PetalLayoutMethod	layout_method;
   private static final long serialVersionUID = 1;
   
   VersionHistoryGraph() {
      super(new BorderLayout());
      setPreferredSize(new Dimension(400,150));
      PetalUndoSupport.getSupport().blockCommands();
      petal_model = new Model();
      petal_editor = new PetalEditor(petal_model);
      layout_method = new PetalLevelLayout(petal_editor);
      JScrollPane jsp = new JScrollPane(petal_editor);
      petal_editor.addZoomWheeler();
      add(jsp,BorderLayout.CENTER);
    }

   void update() {
      Map<BvcrControlVersion,Node> nodes = new HashMap<BvcrControlVersion,Node>();
      petal_model.clear();
      for (BvcrControlVersion cv : control_panel.getVersions()) {
         Node n1 = new Node(cv);
         petal_model.addNode(n1);
         nodes.put(cv,n1);
       }
      for (BvcrControlVersion cv : control_panel.getVersions()) {
         Node n1 = nodes.get(cv);
         for (String pid : cv.getPriorIds()) {
            BvcrControlVersion pcv = control_panel.getVersionMap().get(pid);
            Node n2 = nodes.get(pcv);
            Arc a1 = new Arc(n1,n2);
            petal_model.addArc(a1);
          }
       }
      petal_model.fireModelUpdated();
      petal_editor.commandLayout(layout_method);
      repaint();
    }

   @Override public void paint(Graphics g) {
      for (PetalNode pn : petal_model.getNodes()) {
	 if (pn instanceof Node) {
            // fixup here?
	  }
       }
      super.paint(g);
    }

   private final class Model extends PetalModelDefault {
    }	// end of inner class Model

   private class Node extends PetalNodeDefault {
      
      private BvcrControlVersion for_node;
      private static final long serialVersionUID = 1;
      
      Node(BvcrControlVersion hn) {
         super(hn.getBestName());
         for_node = hn;
       }
   
      @Override public String getToolTip(Point at) {
         return "<html>" + for_node.getHtmlDescription();
       }
   
    }	// end of inner class Node

   private class Arc extends PetalArcDefault {
      
      private static final long serialVersionUID = 1;
      
      Arc(Node f,Node t) {
	 super(f,t);
	 setSourceEnd(new PetalArcEndDefault(PetalArcEnd.PETAL_ARC_END_ARROW));
       }

    }	// end of inner class Arc

}	// end of inner class VersionHistoryGraph




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private final class ChangeVersionAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   @Override public void actionPerformed(ActionEvent evt) {
      if (current_version == null) return;
      control_panel.setVersion(current_version);
    }

}	// end of inner class ChangeVersionAction


private final class ShowTableAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   @Override public void actionPerformed(ActionEvent evt) {
      Component btn = (Component) evt.getSource();
      BvcrControlVersionPanel pnl = new BvcrControlVersionPanel(control_panel);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(btn);
      BudaBubble bbl = BudaRoot.findBudaBubble(btn);
      if (bba == null) return;
      bba.addBubble(pnl,bbl,null,BudaConstants.PLACEMENT_LOGICAL);
    }

}	// end of inner class ShowTableAction



/********************************************************************************/
/*                                                                              */
/*      Configurator methods                                                    */
/*                                                                              */
/********************************************************************************/

@Override public String getConfigurator()               { return "BVCR"; }

@Override public void outputXml(BudaXmlWriter xw) 
{
   xw.field("TYPE","VERSIONGRAPHbedrock");
   xw.field("PROJECT",control_panel.getProject());
}



}	// end of class BvcrControlGraphPanel




/* end of BvcrControlGraphPanel.java */

