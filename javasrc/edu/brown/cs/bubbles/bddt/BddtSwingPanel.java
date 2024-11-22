/********************************************************************************/
/*										*/
/*		BddtSwingPanel.java						*/
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
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;


class BddtSwingPanel extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants,
	BumpConstants.BumpRunEventHandler {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	launch_control;
private boolean 		expecting_input;
private HierarchyTree		hierarchy_tree;
private DrawingTree		drawing_tree;
private HierarchyModel		hierarchy_model;
private DrawingModel		drawing_model;
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtSwingPanel(BddtLaunchControl ctrl)
{
   launch_control = ctrl;
   expecting_input = false;

   if (launch_control == null) return;

   setupPanel();

   BumpClient bc = BumpClient.getBump();
   BumpRunModel br = bc.getRunModel();
   br.addRunEventHandler(this);
}



@Override protected void localDispose()
{
   BumpClient bc = BumpClient.getBump();
   BumpRunModel br = bc.getRunModel();
   br.removeRunEventHandler(this);
}



/********************************************************************************/
/*										*/
/*	Methods to setup the display						*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   int y = 0;
   JLabel ttl = new JLabel("Swing Debugging",SwingConstants.CENTER);
   pnl.addGBComponent(ttl,0,y++,0,1,1,0);
   JSeparator sep = new JSeparator();
   pnl.addGBComponent(sep,0,y++,0,1,1,0);

   Box btns = Box.createHorizontalBox();
   JButton btn = new JButton("<html><c>Press to Get Info<br>Release on application</c>");
   btns.add(btn);
   btns.add(Box.createHorizontalGlue());
   pnl.addGBComponent(btns,0,y++,0,1,1,0);
   btn.addMouseListener(new InfoListener(btn));

   sep = new JSeparator();
   pnl.addGBComponent(sep,0,y++,0,1,1,0);
   JTabbedPane tbp = new JTabbedPane();
   pnl.addGBComponent(tbp,0,y++,0,1,10,10);

   hierarchy_model = new HierarchyModel();
   hierarchy_tree = new HierarchyTree(hierarchy_model);
   tbp.addTab("Hierarchy",new JScrollPane(hierarchy_tree));
   drawing_model = new DrawingModel();
   drawing_tree = new DrawingTree(drawing_model);
   tbp.addTab("Drawing",new JScrollPane(drawing_tree));

   setContentPane(pnl);
}




/********************************************************************************/
/*										*/
/*	Handle swing events reported by application				*/
/*										*/
/********************************************************************************/

@Override public void handleProcessEvent(BumpRunEvent evt)
{
   switch (evt.getEventType()) {
      case PROCESS_SWING :
	 if (!expecting_input) return;
	 BumpProcess bp = evt.getProcess();
	 if (launch_control.getProcess() != bp) return;
	 expecting_input = false;
	 Element xml = (Element) evt.getEventData();
	 if (xml == null) return;
	 handleSwingMessage(xml);
	 break;
      default :
	 break;
    }
}




private void handleSwingMessage(Element xml)
{
   hierarchy_model.clear();
   addToHierarchy(IvyXml.getChild(xml,"COMPONENT"));
   hierarchy_model.finish();

   drawing_model.clear();
   for (Element delt : IvyXml.children(xml,"DRAW")) {
      addToDrawing(delt);
    }
   drawing_model.finish();
}


private void addToHierarchy(Element xml)
{
   for (Element celt = xml; celt != null; celt = IvyXml.getChild(celt,"COMPONENT")) {
      ComponentNode cn = new ComponentNode(celt);
      hierarchy_model.add(cn);
    }
}



private void addToDrawing(Element xml)
{
   DrawNode dn = new DrawNode(xml);
   drawing_model.add(dn);
}



/********************************************************************************/
/*										*/
/*	Handle right click requests						*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   Component c = SwingUtilities.getDeepestComponentAt(this,e.getX(),e.getY());
   if (c instanceof CommonTree) {
      Point pt = SwingUtilities.convertPoint(this,e.getX(),e.getY(),c);
      CommonTree ct = (CommonTree) c;
      ct.handlePopupMenu(menu,pt);
   }

   menu.add(getFloatBubbleAction());
   menu.show(this,e.getX(),e.getY());
}



/********************************************************************************/
/*										*/
/*	Mouse listener for info buttons 					*/
/*										*/
/********************************************************************************/

private class InfoListener extends MouseAdapter {

   private JButton for_button;
   private boolean is_tracking;

   InfoListener(JButton btn) {
      for_button = btn;
      is_tracking = false;
    }

   @Override public void mousePressed(MouseEvent e) {
      is_tracking = true;
      BudaCursorManager.setTemporaryCursor(for_button,Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

   @Override public void mouseReleased(MouseEvent e) {
      if (is_tracking && launch_control.getProcess() != null) {
	 expecting_input = true;
	 launch_control.getProcess().requestSwingData(e.getXOnScreen(),e.getYOnScreen());
       }
      is_tracking = false;
      BudaCursorManager.resetDefaults(for_button);
    }

}	// end of inner class InfoListener



/********************************************************************************/
/*										*/
/*	Trees for swing panels							*/
/*										*/
/********************************************************************************/

private abstract class CommonTree extends JTree {
   
   private static final long serialVersionUID = 1;
   
   CommonTree(TreeModel md) {
      super(md);
      setEditable(false);
      setRootVisible(false);
      setVisibleRowCount(10);
      addMouseListener(new ClickHandler());
    }

   @Override public String getToolTipText(MouseEvent evt) {
      TreePath tp = getPathForLocation(evt.getX(),evt.getY());
      if (tp == null) return null;
      Object o = tp.getLastPathComponent();
      if (o == null) return null;
      if (o instanceof CommonNode) {
	 CommonNode cn = (CommonNode) o;
	 return cn.getToolTipText();
       }
      return o.toString();
    }

   void handlePopupMenu(JPopupMenu m,Point pt) {
      TreePath tp = getPathForLocation(pt.x,pt.y);
      if (tp == null) return;
      Object o = tp.getLastPathComponent();
      if (o == null) return;
      if (o instanceof StackNode) {
	 StackNode sn = (StackNode) o;
	 sn.handlePopupMenu(m);
       }
   }

   void handleClick(int x,int y) {
      TreePath tp = getPathForLocation(x,y);
      if (tp == null) return;
      Object o = tp.getLastPathComponent();
      if (o == null) return;
      if (o instanceof StackNode) {
	 StackNode sn = (StackNode) o;
	 sn.handleGoto();
       }
   }



}	// end of inner class CommonTree


private class HierarchyTree extends CommonTree {

   private static final long serialVersionUID = 1;
   
   HierarchyTree(HierarchyModel md) {
      super(md);
    }

}	// end of inner class HierarchyTree


private class DrawingTree extends CommonTree {
   
   private static final long serialVersionUID = 1;
   
   DrawingTree(DrawingModel md) {
      super(md);
    }

}	// end of inner class DrawingTree




/********************************************************************************/
/*										*/
/*	Tree Models								*/
/*										*/
/********************************************************************************/

private abstract class CommonModel extends DefaultTreeModel {

   private static final long serialVersionUID = 1;
   
   CommonModel() {
      super(new DefaultMutableTreeNode());
    }

   void clear() {
      DefaultMutableTreeNode rt = (DefaultMutableTreeNode) getRoot();
      rt.removeAllChildren();
    }

   void add(MutableTreeNode tn) {
      DefaultMutableTreeNode rt = (DefaultMutableTreeNode) getRoot();
      rt.add(tn);
    }

   void finish() {
      reload();
    }

}	// end of inner class CommonModel



private class HierarchyModel extends CommonModel {

   private static final long serialVersionUID = 1;
   
   HierarchyModel() { }

}	// end of inner class HierarchyModel



private class DrawingModel extends CommonModel {

   private static final long serialVersionUID = 1;

   DrawingModel() {
    }

}	// end of inner class DrawingModel



/********************************************************************************/
/*										*/
/*	Tree node implementations						*/
/*										*/
/********************************************************************************/

private abstract class CommonNode extends DefaultMutableTreeNode {
   
   private static final long serialVersionUID = 1;
   
   String getToolTipText()		{ return toString(); }

}	// end of inner class CommonNode


private class ComponentNode extends CommonNode {

   private int x_pos;
   private int y_pos;
   private int c_width;
   private int c_height;
   private String class_name;
   private String comp_name;
   private static final long serialVersionUID = 1;
   
   ComponentNode(Element xml) {
      x_pos = IvyXml.getAttrInt(xml,"X");
      y_pos = IvyXml.getAttrInt(xml,"Y");
      c_width = IvyXml.getAttrInt(xml,"W");
      c_height = IvyXml.getAttrInt(xml,"H");
      class_name = IvyXml.getAttrString(xml,"CLASS");
      comp_name = IvyXml.getAttrString(xml,"NAME");
      if (comp_name == null) {
	 int idx = class_name.lastIndexOf(".");
	 if (idx < 0) comp_name = class_name;
	 else comp_name = class_name.substring(idx+1);
       }

      Element stk = IvyXml.getChild(xml,"CREATE");
      if (stk != null) {
	 for (Element selt : IvyXml.children(stk,"FRAME")) {
	    StackNode sn = new StackNode(selt);
	    add(sn);
	  }
       }
    }

   @Override public String toString() {
      return comp_name + " @ [" + x_pos + "," + y_pos + ":" + c_width + "x" + c_height + "]";
    }

}	// end of inner class ComponentNode



private class DrawNode extends CommonNode {

   private String fg_color;
   private static final long serialVersionUID = 1;
   
   DrawNode(Element xml) {
      fg_color = IvyXml.getTextElement(xml,"COLOR");
      Element stk = IvyXml.getChild(xml,"STACK");
      for (Element selt : IvyXml.children(stk,"FRAME")) {
         StackNode sn = new StackNode(selt);
         add(sn);
       }
    }

   @Override public String toString() {
      String nm = "Paint";
      if (fg_color != null) nm += " " + fg_color;
      return nm;
    }

}	// end of inner class DrawNode



private class StackNode extends CommonNode {

   private String class_name;
   private String method_name;
   private int line_number;
   private File file_name;
   private static final long serialVersionUID = 1;
   
   StackNode(Element xml) {
      class_name = IvyXml.getAttrString(xml,"CLASS");
      method_name = IvyXml.getAttrString(xml,"METHOD");
      line_number = IvyXml.getAttrInt(xml,"LINE");
      String fnm = IvyXml.getAttrString(xml,"FILE");
      if (fnm == null) file_name = null;
      else file_name = new File(fnm);

      setAllowsChildren(false);
    }

   @Override public String toString() {
      String mnm = class_name + "." + method_name;
      if (line_number > 0) mnm += " @ " + line_number;
      return mnm;
    }

   void handlePopupMenu(JPopupMenu m) {
      checkFile();
      if (launch_control.fileExists(file_name) && line_number > 0) {
	 m.add(new SourceAction(this));
       }
    }

   void handleGoto() {
      checkFile();
      BudaBubble bb = null;
      if (launch_control.fileExists(file_name) && line_number > 0) {
         BaleFactory bf = BaleFactory.getFactory();
         BaleConstants.BaleFileOverview bfo = bf.getFileOverview(null,file_name);
         if (bfo != null) {
            int loff = bfo.findLineOffset(line_number);
            int eoff = bfo.mapOffsetToEclipse(loff);
            BassFactory bsf = BassFactory.getFactory();
            BassName bn = bsf.findBubbleName(file_name,eoff);
            if (bn != null) bb = bn.createBubble();
          }
         if (bb == null) {
            String fct = class_name + "." + method_name;
            bb = bf.createMethodBubble(null,fct);
          }
       }
      if (bb == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtSwingPanel.this);
      if (bba != null)
         bba.addBubble(bb,BddtSwingPanel.this,null,PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
    }

   private void checkFile() {
      if (file_name == null) return;
      if (launch_control.fileExists(file_name)) return;
      BassFactory bsf = BassFactory.getFactory();
      File f = bsf.findActualFile(file_name);
      if (launch_control.fileExists(f)) file_name = f;
      else file_name = null;
    }


}	// end of inner class StackNode



/********************************************************************************/
/*										*/
/*	Actions 								*/
/*										*/
/********************************************************************************/

private final class ClickHandler extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
         CommonTree tr = (CommonTree) e.getSource();
         tr.handleClick(e.getX(),e.getY());
       }
    }

}	// end of inner class ClickHandler




private class SourceAction extends AbstractAction {

   private StackNode for_node;
   private static final long serialVersionUID = 1;
   
   SourceAction(StackNode sn) {
      super("Goto Source");
      for_node = sn;
    }

   @Override public void actionPerformed(ActionEvent e) {
      for_node.handleGoto();
    }

}	// end of inner class SourceAction



}	// end of class BddtSwingPanel




/* end of BddtSwingPanel.java */
