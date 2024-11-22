/********************************************************************************/
/*										*/
/*		BconPackagePanel.java						*/
/*										*/
/*	Bubbles Environment Context Viewer package information viewer		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.banal.BanalConstants;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingTextField;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;



class BconPackagePanel implements BconConstants, BconConstants.BconPanel, BanalConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		for_project;
private String		for_package;
private SwingGridPanel	package_panel;
private BconPackageDisplay graph_panel;
private BconPackageGraph package_graph;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconPackagePanel(String proj,String pkg)
{
   for_project = proj;
   for_package = pkg;

   package_graph = new BconPackageGraph(proj,pkg);

   Dimension sz = new Dimension(300,200);
   graph_panel = new BconPackageDisplay(package_graph);
   graph_panel.setSize(sz);
   graph_panel.setPreferredSize(sz);

   graph_panel.addComponentListener(new Sizer());

   PackagePanel pnl = new PackagePanel();
   JLabel ttl = new JLabel(for_package);
   pnl.addGBComponent(ttl,0,0,2,1,10,0);

   pnl.addGBComponent(graph_panel,0,1,1,1,10,10);
   JTextField tfld = new SwingTextField();
   tfld.addActionListener(new FilterAction());

   pnl.addGBComponent(tfld,0,2,1,1,10,0);
   JTabbedPane tabs = new JTabbedPane(SwingConstants.BOTTOM,JTabbedPane.SCROLL_TAB_LAYOUT);
   pnl.addGBComponent(tabs,1,1,1,1,0,1);
   tabs.add("Nodes",new NodeTab());
   tabs.add("Edges",new EdgeTab());
   tabs.add("Layout",new LayoutTab());

   package_panel = pnl;
}




@Override public void dispose() 			{ }




/********************************************************************************/
/*										*/
/*	Methods to build the graph model using current settings 		*/
/*										*/
/********************************************************************************/

private void resetGraph()
{
   graph_panel.updateGraph();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public JComponent getComponent()		{ return package_panel; }




/********************************************************************************/
/*										*/
/*	Menu methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   graph_panel.handlePopupMenu(e);
}




/********************************************************************************/
/*										*/
/*	Sizing methods								*/
/*										*/
/********************************************************************************/

private final class Sizer extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      resetGraph();
    }

}	// end of inner class Sizer



/********************************************************************************/
/*										*/
/*	Tab for node options							*/
/*										*/
/********************************************************************************/

private class NodeTab extends JPanel implements ActionListener {
   
   private static final long serialVersionUID = 1;
   
   NodeTab() {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
   
      addClassButton("Public",ClassType.PUBLIC,null);
      addClassButton("Proteced",ClassType.PROTECTED,null);
      addClassButton("Package",ClassType.PACKAGE_PROTECTED,null);
      addClassButton("Private",ClassType.PRIVATE,null);
      addClassButton("Inner Classes",ClassType.INNER,null);
      addClassButton("Classes",ClassType.CLASS,NodeType.CLASS);
      addClassButton("Interfaces",ClassType.INTERFACE,NodeType.INTERFACE);
      addClassButton("Static",ClassType.STATIC,null);
      addClassButton("Abstract",ClassType.ABSTRACT,null);
      addClassButton("Enumerations",ClassType.ENUM,NodeType.ENUM);
      addClassButton("Exceptions",ClassType.THROWABLE,NodeType.THROWABLE);
      addClassButton("Methods",ClassType.METHOD,NodeType.METHOD);
      // add(new JSeparator()); -- make it fixed size
      JCheckBox cbx = new JCheckBox("Show Labels",graph_panel.getShowLabels());
      cbx.setToolTipText("Show node labels");
      cbx.addActionListener(this);
      add(cbx);
    }

   private void addClassButton(String nm,ClassType cty,NodeType nty) {
      boolean fg = package_graph.getClassOption(cty);
      JCheckBox btn = new JCheckBox(nm,fg);
      btn.addActionListener(new ClassAction(cty));
      btn.setToolTipText("Show " + nm.toLowerCase() + " types");
      if (nty != null) {
	 Color c = BconPackageDisplay.getNodeColor(nty);
	 if (c != null) btn.setForeground(c);
       }
      add(btn);
    }

   @Override public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();
      AbstractButton ab = (AbstractButton) e.getSource();

      if (btn == null) ;
      else if (btn.equals("Show Labels")) {
	 graph_panel.setShowLabels(ab.isSelected());
       }
    }

}	// end of inner class NodeTab




private class ClassAction implements ActionListener {

   private ClassType class_type;

   ClassAction(ClassType cty) {
      class_type = cty;
   }

   @Override public void actionPerformed(ActionEvent e) {
      AbstractButton cbx = (AbstractButton) e.getSource();
      package_graph.setClassOption(class_type,cbx.isSelected());
      resetGraph();
   }

}	// end of inner class ClassAction






/********************************************************************************/
/*										*/
/*	Tab for edge options							*/
/*										*/
/********************************************************************************/

private class EdgeTab extends JPanel implements ActionListener {
   
   private static final long serialVersionUID = 1;
   
   EdgeTab() {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

      addRelationButton("Superclass",ArcType.SUBCLASS);
      addRelationButton("Implements",ArcType.IMPLEMENTED_BY);
      addRelationButton("Extends",ArcType.EXTENDED_BY);
      addRelationButton("Inner class",ArcType.INNERCLASS);
      addRelationButton("Allocates",ArcType.ALLOCATES);
      addRelationButton("Calls",ArcType.CALLS);
      addRelationButton("Catches",ArcType.CATCHES);
      addRelationButton("Accesses",ArcType.ACCESSES);
      addRelationButton("Writes",ArcType.WRITES);
      addRelationButton("Constant",ArcType.CONSTANT);
      addRelationButton("Field",ArcType.FIELD);
      addRelationButton("Local",ArcType.LOCAL);
      addRelationButton("Members",ArcType.MEMBER_OF);

      // add(new JSeparator()); -- make it fixed size
      JCheckBox cbx = new JCheckBox("Show Labels",graph_panel.getShowArcLabels());
      cbx.setToolTipText("Show edge labels");
      cbx.addActionListener(this);
      add(cbx);
    }

   private void addRelationButton(String nm,ArcType rtyp) {
      boolean fg = package_graph.getArcOption(rtyp);
      JCheckBox btn = new JCheckBox(nm,fg);
      btn.addActionListener(new RelationAction(rtyp));
      btn.setToolTipText("Show " + nm.toLowerCase() + " relationships");
      Color c = BconPackageDisplay.getArcColor(rtyp);
      if (c != null) btn.setForeground(c);
      add(btn);
    }

   @Override public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();
      AbstractButton ab = (AbstractButton) e.getSource();

      if (btn == null) ;
      else if (btn.equals("Show Labels")) {
	 graph_panel.setShowArcLabels(ab.isSelected());
       }
    }

}	// end of inner class EdgeTab




private class RelationAction implements ActionListener {

   private ArcType rel_type;

   RelationAction(ArcType rtyp) {
      rel_type = rtyp;
   }

   @Override public void actionPerformed(ActionEvent e) {
      AbstractButton cbx = (AbstractButton) e.getSource();
      package_graph.setArcOption(rel_type,cbx.isSelected());
      resetGraph();
   }
}




/********************************************************************************/
/*										*/
/*	Tab for layout options							*/
/*										*/
/********************************************************************************/

private class LayoutTab extends JPanel implements ActionListener {
   
   private static final long serialVersionUID = 1;
   
   LayoutTab() {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

      ButtonGroup bg = new ButtonGroup();
      addLayoutButton(bg,"Circle",LayoutType.CIRCLE);
      addLayoutButton(bg,"Force",LayoutType.FORCE);
      addLayoutButton(bg,"Map",LayoutType.MAP);
      addLayoutButton(bg,"General",LayoutType.GENERAL);
      addLayoutButton(bg,"Spring",LayoutType.SPRING);
      addLayoutButton(bg,"Tree",LayoutType.TREE);
      addLayoutButton(bg,"Stack",LayoutType.STACK);
      addLayoutButton(bg,"Parallel",LayoutType.PSTACK);
      addLayoutButton(bg,"EdgeLabel",LayoutType.ESTACK);
      addLayoutButton(bg,"Partition",LayoutType.PARTITION);
    }

   private void addLayoutButton(ButtonGroup grp,String nm,LayoutType lty) {
      boolean fg = (graph_panel.getLayoutType() == lty);
      JRadioButton itm = new JRadioButton(nm,fg);
      grp.add(itm);
      itm.addActionListener(new LayoutAction(lty));
      itm.setToolTipText("Use " + nm.toLowerCase() + " layout");
      add(itm);
    }

   @Override public void actionPerformed(ActionEvent e) {
    }

}	// end of inner class LayoutTab




private class LayoutAction implements ActionListener {

    private LayoutType for_type;

    LayoutAction(LayoutType typ) {
       for_type = typ;
     }

    @Override public void actionPerformed(ActionEvent e) {
       AbstractButton cbx = (AbstractButton) e.getSource();
       if (cbx.isSelected() && graph_panel.getLayoutType() != for_type) {
	  graph_panel.setLayoutType(for_type);
	  resetGraph();
	}
     }

}	// end of inner class LayoutAction





/********************************************************************************/
/*										*/
/*	Handle typein filtering 							 */
/*										*/
/********************************************************************************/

private final class FilterAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JTextField tfld = (JTextField) evt.getSource();
      String txt = tfld.getText();
      if (txt == null || txt.length() == 0) return;
      String [] args = txt.split("\\s");
      if (args.length == 0) return;
      graph_panel.removeSelections();
      for (BconGraphNode gn : package_graph.getNodes()) {
	 String nm = gn.getFullName();
	 boolean fnd = true;
	 for (String s : args) {
	    if (nm.contains(s)) fnd = false;
	  }
	 if (fnd) graph_panel.addSelection(gn);
       }
    }

}

/********************************************************************************/
/*										*/
/*     Class for bubble contents						*/
/*										*/
/********************************************************************************/

private final class PackagePanel extends SwingGridPanel
      implements BudaConstants.BudaBubbleOutputer {
   
   private static final long serialVersionUID = 1;
   
   @Override public String getConfigurator()		{ return "BCON"; }

   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","PACKAGE");
      xw.field("PROJECT",for_project);
      xw.field("PACKAGE",for_package);
   }


}




}	// end of class BconPackagePanel





/* end of BconPackagePanel.java */







