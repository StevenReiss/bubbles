/********************************************************************************/
/*										*/
/*		BconPackageDisplay.java 					*/
/*										*/
/*	The actual graph display panel						*/
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



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.banal.BanalConstants;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.petal.PetalArc;
import edu.brown.cs.ivy.petal.PetalArcDefault;
import edu.brown.cs.ivy.petal.PetalArcEndDefault;
import edu.brown.cs.ivy.petal.PetalCircleLayout;
import edu.brown.cs.ivy.petal.PetalEditor;
import edu.brown.cs.ivy.petal.PetalHelper;
import edu.brown.cs.ivy.petal.PetalLayoutMethod;
import edu.brown.cs.ivy.petal.PetalLevelLayout;
import edu.brown.cs.ivy.petal.PetalModelBase;
import edu.brown.cs.ivy.petal.PetalNode;
import edu.brown.cs.ivy.petal.PetalNodeDefault;
import edu.brown.cs.ivy.petal.PetalRelaxLayout;
import edu.brown.cs.ivy.petal.PetalUndoSupport;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;



class BconPackageDisplay extends JPanel implements BconConstants, BanalConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BconPackageGraph	package_graph;
private boolean 		show_labels;
private boolean 		arc_labels;
private boolean 		reset_graphics;
private LayoutType		layout_type;
private ComputeGraph		compute_graph;

private boolean 		layout_needed;
private boolean 		freeze_layout;
private double			user_scale;
private double			prior_scale;

private PetalEditor		petal_editor;
private GraphModel		petal_model;
private PetalLayoutMethod	layout_method;

private Map<BconGraphNode,BconPetalNode> node_map;
private Map<BconGraphArc,BconPetalArc>	 arc_map;

private static Map<NodeType,CompShape> shape_map;


enum CompShape {
   SQUARE,
   TRIANGLE,
   CIRCLE,
   DIAMOND,
   PENTAGON
}

static {
   shape_map = new HashMap<NodeType,CompShape>();
   shape_map.put(NodeType.CLASS,CompShape.DIAMOND);
   shape_map.put(NodeType.INTERFACE,CompShape.TRIANGLE);
   shape_map.put(NodeType.ENUM,CompShape.CIRCLE);
   shape_map.put(NodeType.THROWABLE,CompShape.PENTAGON);
   shape_map.put(NodeType.ANNOTATION,CompShape.PENTAGON);
   shape_map.put(NodeType.PACKAGE,CompShape.SQUARE);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconPackageDisplay(BconPackageGraph pg)
{
   super(new BorderLayout());

   package_graph = pg;
   show_labels = false;
   arc_labels = false;
   layout_needed = true;
   freeze_layout = false;
   reset_graphics = false;
   compute_graph = new ComputeGraph();

   node_map = new HashMap<BconGraphNode,BconPetalNode>();
   arc_map = new HashMap<BconGraphArc,BconPetalArc>();

   // setupGraphModel();
   BoardThreadPool.start(compute_graph);
   PetalUndoSupport undo = PetalUndoSupport.getSupport();
   undo.blockCommands();

   petal_model = new GraphModel();
   petal_editor = new PetalEditor(petal_model);
   setLayoutType(LayoutType.GENERAL);


   JScrollPane jsp = new JScrollPane(petal_editor);

   petal_editor.addZoomWheeler();

   add(jsp,BorderLayout.CENTER);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean getShowLabels() 			{ return show_labels; }
void setShowLabels(boolean fg)
{
   if (show_labels == fg) return;
   show_labels = fg;
   reset_graphics = true;
   relayout();
}




boolean getShowArcLabels()			{ return arc_labels; }
void setShowArcLabels(boolean fg)		{ arc_labels = fg; }

LayoutType getLayoutType()			{ return layout_type; }

void setLayoutType(LayoutType t)
{
   if (layout_type == t) return;

   layout_type = t;

   switch (layout_type) {
      case FORCE :
	 PetalRelaxLayout prl = new PetalRelaxLayout(petal_editor);
	 layout_method = prl;
	 break;
      case SPRING :
	 prl = new PetalRelaxLayout(petal_editor);
	 prl.setArcLength(50);
	 prl.setDistance(100);
	 layout_method = prl;
	 break;
      default :
      case GENERAL :
	 PetalLevelLayout pll = new PetalLevelLayout(petal_editor);
	 pll.setOptimizeLevels(true);
	 pll.setWhiteFraction(1.0);
	 layout_method = pll;
	 break;
      case TREE :
	 pll = new PetalLevelLayout(petal_editor);
	 pll.setWhiteFraction(1.0);
	 layout_method = pll;
	 break;
      case MAP :
	 pll = new PetalLevelLayout(petal_editor);
	 pll.setTwoWay(true);
	 pll.setWhiteFraction(1.0);
	 layout_method = pll;
	 break;
      case CIRCLE :
	 PetalCircleLayout pcl = new PetalCircleLayout(petal_editor);
	 layout_method = pcl;
	 break;
   }

   petal_editor.commandLayout(layout_method);
}





void addSelection(BconGraphNode nd)
{
   for (PetalNode pn : petal_model.getNodes()) {
      if (pn instanceof BconPetalNode) {
	 BconPetalNode bpn = (BconPetalNode) pn;
	 if (bpn.getGraphNode() == nd) {
	    petal_model.select(pn);
	  }
       }
    }
}

void removeSelections()
{
   petal_model.deselectAll();
}


List<BconGraphNode> getSelections()
{
   List<BconGraphNode> rslt = new ArrayList<BconGraphNode>();
   for (PetalNode pn : petal_model.getSelectedNodes()) {
      if (pn instanceof BconPetalNode) {
	 BconPetalNode bpn = (BconPetalNode) pn;
	 BconGraphNode gn = bpn.getGraphNode();
	 if (gn != null) rslt.add(gn);
       }
    }


   return rslt;
}

void relayout()
{
   layout_needed = true;
   BoardThreadPool.start(compute_graph);
   // setupGraphModel();
   // repaint();
}



void updateGraph()
{
   BoardThreadPool.start(compute_graph);
   // setupGraphModel();
   // repaint();
}



void handlePopupMenu(MouseEvent e)
{
   Point p0 = e.getLocationOnScreen();
   SwingUtilities.convertPointFromScreen(p0,petal_editor);
   PetalNode pn0 = petal_editor.findNode(p0);
   if (pn0 != null) {
      BconPetalNode bpn = (BconPetalNode) pn0;
      bpn.handlePopupMenu(e);
      return;
    }

   JPopupMenu m = new JPopupMenu();
   int ct = 0;
   if (package_graph.getStartNode() != null) {
      m.add(new HomeAction());
      ++ct;
    }
   Collection<BconGraphNode> sels = getSelections();
   if (sels != null && sels.size() > 0) {
      m.add(new RemoveSelectAction(sels));
      m.add(new RestrictSelectAction(sels));
      ct += 2;
    }


   if (ct == 0) return;

   m.show(this,p0.x,p0.y);
}




/********************************************************************************/
/*										*/
/*	Graph model setup							*/
/*										*/
/********************************************************************************/

private void setupGraphModel()
{
   Collection<BconGraphNode> nodes = package_graph.getNodes();
   boolean chng = false;

   Set<BconGraphNode> nodeset = new HashSet<BconGraphNode>(nodes);
   for (Iterator<Map.Entry<BconGraphNode,BconPetalNode>> it = node_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<BconGraphNode,BconPetalNode> ent = it.next();
      if (!nodeset.contains(ent.getKey())) {
	 it.remove();
	 chng = true;
       }
      else if (reset_graphics) {
	 ent.getValue().resetGraphics();
      }
    }
   reset_graphics = false;

   for (BconGraphNode gn : nodes) {
      BconPetalNode pn = node_map.get(gn);
      if (pn != null) {
	 pn.update();
       }
      else {
	 pn = new BconPetalNode(gn);
	 node_map.put(gn,pn);
	 chng = true;
	 layout_needed = true;
       }
    }

   Set<BconGraphArc> arcs = new HashSet<BconGraphArc>();
   for (BconGraphNode gn : nodes) {
      if (gn.getOutArcs() != null)
	 arcs.addAll(gn.getOutArcs());
    }
   for (Iterator<Map.Entry<BconGraphArc,BconPetalArc>> it = arc_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<BconGraphArc,BconPetalArc> ent = it.next();
      if (!arcs.contains(ent.getKey())) {
	 it.remove();
	 chng = true;
       }
    }
   for (BconGraphArc ga : arcs) {
      BconPetalArc pa = arc_map.get(ga);
      if (pa != null) {
	 pa.update();
       }
      else {
	 pa = new BconPetalArc(ga);
	 arc_map.put(ga,pa);
	 chng = true;
       }
    }

   if (chng && !freeze_layout) layout_needed = true;

   if (petal_model != null) {
      petal_model.invalidate();
   }

   if (layout_needed && petal_editor != null) {
      petal_editor.commandDeselectAll();
      petal_editor.commandLayout(layout_method);
      layout_needed = false;
    }

   if (petal_editor != null) {
      Dimension d1 = petal_editor.getPreferredSize();
      d1 = petal_editor.getExtent();
      Dimension d2 = getSize();
      if (d1.width != 0 && d2.width != 0 && d1.height != 0 && d2.height != 0) {
	 double dx = d2.getWidth() / d1.getWidth();
	 double dy = d2.getHeight() / d1.getHeight();
	 double da = Math.min(dx,dy);
	 double db = petal_editor.getScaleFactor() / prior_scale;
	 petal_editor.setScaleFactor(da*db*0.95*user_scale);
	 petal_editor.setScaleFactor(da * 0.95);
	 prior_scale = user_scale;
      }
   }

   // scale as needed
}



/********************************************************************************/
/*										*/
/*	Petal node and arc implementations					*/
/*										*/
/********************************************************************************/

private Component getDisplayComponent(BconGraphNode gn)
{
   JComponent comp = null;

   if (show_labels) {
      String nm = gn.getLabelName();
      JLabel lbl = new JLabel(nm);
      Font ft = lbl.getFont();
      ft = ft.deriveFont(9f);
      lbl.setFont(ft);
      lbl.setBorder(new LineBorder(BoardColors.getColor("Bcon.PackageBorderColor"),2));
      lbl.setMinimumSize(lbl.getPreferredSize());
      lbl.setSize(lbl.getPreferredSize());
      lbl.setMaximumSize(new Dimension(400,400));
      lbl.setOpaque(true);
      comp = lbl;
   }
   else {
      CompShape cs = shape_map.get(gn.getNodeType());
      GraphComponent gc = new GraphComponent(cs);
      comp = gc;
   }

   Color ncol = getNodeColor(gn.getNodeType());
   if (ncol == null) ncol = comp.getForeground();
   if (show_labels) ncol = ncol.darker();
   comp.setForeground(ncol);

   return comp;
}



static Color getNodeColor(NodeType nt)
{
   Color ncol = null;

   switch (nt) {
      case CLASS :
	 ncol = BoardColors.getColor("Bcon.PackageClassNode");
	 break;
      case INTERFACE :
	 ncol = BoardColors.getColor("Bcon.PackageInterfaceNode");
	 break;
      case ENUM :
	 ncol = BoardColors.getColor("Bcon.PackageEnumNode");
	 break;
      case THROWABLE :
	 ncol = BoardColors.getColor("Bcon.PackageThrowableNode");
	 break;
      case ANNOTATION :
	 ncol = BoardColors.getColor("Bcon.PackageAnnotationNode");
	 break;
      case PACKAGE :
	 ncol = BoardColors.getColor("Bcon.PackagePackageNode");
	 break;
      default:
	 break;
    }

   return ncol;
}



private class BconPetalNode extends PetalNodeDefault {

   private BconGraphNode graph_node;

   BconPetalNode(BconGraphNode gn) {
      graph_node = gn;
      setComponent(getDisplayComponent(graph_node));
    }

   @Override public String getToolTip(Point at) {
      String nm = graph_node.getFullName();
      switch (graph_node.getNodeType()) {
	 case CLASS :
	    return "Class " + nm;
	 case INTERFACE :
	    return "Interface " + nm;
	 case ENUM :
	    return "Enum " + nm;
	 case ANNOTATION :
	    return "Annotation " + nm;
	 case PACKAGE :
	    return "Package " + nm;
	 case THROWABLE :
	    return "Throwable " + nm;
	 case METHOD :
	    return "Method " + nm;
      }
      return null;
   }

   @Override public boolean handleMouseClick(MouseEvent e)	{ return false; }
   @Override public boolean handleKeyInput(KeyEvent e)		{ return false; }

   void update()				{ }

   String getSortName() 			{ return graph_node.getFullName(); }
   int getArcCount()				{ return graph_node.getArcCount(); }
   BconGraphNode getGraphNode() 		{ return graph_node; }

   void resetGraphics() {
      setComponent(getDisplayComponent(graph_node));
   }

   void handlePopupMenu(MouseEvent evt) {
      Point p0 = evt.getLocationOnScreen();
      SwingUtilities.convertPointFromScreen(p0,BconPackageDisplay.this);
      JPopupMenu m = new JPopupMenu();
      if (package_graph.getCollapsedType(graph_node) != ArcType.NONE) {
	 m.add(new ExpandAction(graph_node));
       }
      String pnm = graph_node.getFullName();
      if (graph_node.isInnerClass()) {
	 m.add(new CompactAction(graph_node,ArcType.INNERCLASS));
	 int idx = pnm.lastIndexOf(".");
	 if (idx >= 0) pnm = pnm.substring(0,idx);
       }
      if (graph_node.getClassType().contains(ClassType.METHOD)) {
	 m.add(new CompactAction(graph_node,ArcType.MEMBER_OF));
       }
      if (pnm.contains(".")) {
	 m.add(new CompactAction(graph_node,ArcType.PACKAGE));
       }
      if (graph_node.isSubclass()) {
	 m.add(new CompactAction(graph_node,ArcType.SUBCLASS,
	       ArcType.IMPLEMENTED_BY,ArcType.EXTENDED_BY));
       }
      m.add(new InducedAction(graph_node));
      m.add(new ExcludeAction(graph_node));

      if (graph_node.getClassType().contains(ClassType.METHOD)) {
	 m.add(new SourceAction(graph_node));
       }
      else {
	 m.add(new SearchAction(graph_node));
       }

      m.show(BconPackageDisplay.this,p0.x,p0.y);
   }

   @Override public Point findPortPoint(Point at,Point from) {
      if (getComponent() instanceof GraphComponent) {
	 GraphComponent gc = (GraphComponent) getComponent();
	 return gc.findPortPoint(at,from);
       }

      return super.findPortPoint(at,from);
    }

}	// end of inner class BconPetalNode



private class BconPetalArc extends PetalArcDefault {

   BconGraphArc for_arc;

   BconPetalArc(BconGraphArc ga) {
      super(node_map.get(ga.getFromNode()),node_map.get(ga.getToNode()));
      for_arc = ga;

      update();
    }

   @Override public boolean handleMouseClick(MouseEvent evt)	{ return false; }

   @Override public String getToolTip() {
      return for_arc.getLabel();
    }

   void update() {
      for_arc.update();
      int mxn = for_arc.getRelationTypes().getRelationshipCount();
      ArcType prt = for_arc.getRelationTypes().getPrimaryRelationship();
      if (mxn > 0) {
         float wd = (float)(1 + Math.log(mxn));
         Stroke s = new BasicStroke(wd);
         setStroke(s);
      }
   
      Color c = getArcColor(prt);
      if (c != null) setColor(c);
   
      if (for_arc.useTargetArrow())
         setTargetEnd(new PetalArcEndDefault(PETAL_ARC_END_ARROW,4,c));
      else setTargetEnd(null);
   
      if (for_arc.useSourceArrow())
         setSourceEnd(new PetalArcEndDefault(PETAL_ARC_END_ARROW,4,c));
      else setSourceEnd(null);
    }


}	// end of inner clas BconPetalArc



static Color getArcColor(ArcType prt)
{
   Color c = null;
   switch (prt) {
      default :
	 c = BoardColors.getColor("Bcon.PackageArc");
	 break;
      case CALLS :
	 c = BoardColors.getColor("Bcon.PackageCallsArc");
	 break;
      case SUBCLASS :
      case IMPLEMENTED_BY :
      case EXTENDED_BY :
      case MEMBER_OF :
	 c = BoardColors.getColor("Bcon.PackageClassArc");
	 break;
      case INNERCLASS :
	 c = BoardColors.getColor("Bcon.PackageInnerArc");
	 break;
    }

   return c;
}



/********************************************************************************/
/*										*/
/*	Graph Components							*/
/*										*/
/********************************************************************************/

private static class GraphComponent extends JPanel {

   private CompShape	use_shape;
   private Polygon	poly_shape;

   GraphComponent(CompShape sh) {
      Dimension d = new Dimension(20,20);
      setSize(d);
      setPreferredSize(d);
      setMinimumSize(d);
      if (sh == null) sh = CompShape.DIAMOND;
      use_shape = sh;
      setupShape();
   }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(getForeground());
      Rectangle r = getBounds();

      Shape s = null;
      switch (use_shape) {
	 case SQUARE :
	 default :
	    r.x = r.y = 0;
	    s = r;
	    break;
	 case TRIANGLE :
	 case DIAMOND :
	 case PENTAGON :
	    s = poly_shape;
	    break;
	 case CIRCLE :
	    Ellipse2D el = new Ellipse2D.Double();
	    el.setFrame(0,0,r.width,r.height);
	    s = el;
	    break;
       }

      if (s != null) {
	 g2.fill(s);
       }
    }

   private void setupShape() {
      Rectangle r = getBounds();
      switch (use_shape) {
	 case SQUARE :
	 case CIRCLE :
	 default :
	    break;
	 case TRIANGLE :
	    poly_shape = new Polygon();
	    poly_shape.addPoint(r.width/2,0);
	    poly_shape.addPoint(0,r.height);
	    poly_shape.addPoint(r.width,r.height);
	    break;
	 case DIAMOND :
	    poly_shape = new Polygon();
	    poly_shape.addPoint(r.width/2,0);
	    poly_shape.addPoint(0,r.height/2);
	    poly_shape.addPoint(r.width/2,r.height);
	    poly_shape.addPoint(r.width,r.height/2);
	    break;
	 case PENTAGON :
	    poly_shape = new Polygon();
	    double a1 = Math.tan(Math.toRadians(54));
	    int h1 = (int)(r.width / 2.0 / a1);
	    double a2 = Math.tan(Math.toRadians(18));
	    int h2 = (int)(a2 * (r.height - h1));
	    poly_shape.addPoint(r.width/2,0);
	    poly_shape.addPoint(r.width,h1);
	    poly_shape.addPoint(r.width - h2,r.height);
	    poly_shape.addPoint(h2,r.height);
	    poly_shape.addPoint(0,h1);
	    break;
       }
   }

   Point  findPortPoint(Point at,Point from) {
      switch (use_shape) {
	 case SQUARE :
	    return PetalHelper.findPortPoint(getBounds(),at,from);
	 case CIRCLE :
	    return PetalHelper.findOvalPortPoint(getBounds(),at,from);
	 case DIAMOND :
	 case TRIANGLE :
	 case PENTAGON :
	    if (poly_shape != null)
	       return PetalHelper.findShapePortPoint(this,poly_shape,at,from);

       }

      return PetalHelper.findPortPoint(getBounds(),at,from);
    }

}	// end of inner class GraphComponent



/********************************************************************************/
/*										*/
/*	PetalModel definitions							*/
/*										*/
/********************************************************************************/

private class GraphModel extends PetalModelBase {

   private PetalNode [] node_array;
   private PetalArc []	arc_array;

   GraphModel() {
      node_array = null;
      arc_array = null;
    }

   void invalidate() {
      node_array = null;
      arc_array = null;
    }

   @Override public PetalNode [] getNodes() {
      if (node_array == null) {
	 node_array = new PetalNode[node_map.size()];
	 node_array = node_map.values().toArray(node_array);
	 Arrays.sort(node_array,new NodeSorter());
       }
      return node_array;
    }

   @Override public PetalArc [] getArcs() {
      if (arc_array == null) {
	 arc_array = new PetalArc[arc_map.size()];
	 arc_array = arc_map.values().toArray(arc_array);
       }
      return arc_array;
    }

   // disable editing
   @Override public void createArc(PetalNode frm,PetalNode to)			{ }
   @Override public boolean dropNode(Object o,Point p,PetalNode pn,PetalArc pa) { return false; }
   @Override public void removeArc(PetalArc pa) 				{ }
   @Override public void removeNode(PetalNode pn)				{ }

}	// end of inner class GraphModel



private static class NodeSorter implements Comparator<PetalNode> {

   @Override public int compare(PetalNode p1,PetalNode p2) {
      int d1 = ((BconPetalNode) p1).getArcCount() - ((BconPetalNode) p2).getArcCount();
      if (d1 != 0) return -d1;
      String s1 = ((BconPetalNode) p1).getSortName();
      String s2 = ((BconPetalNode) p2).getSortName();
      return s1.compareTo(s2);
    }

}	// end of inner class NodeSorter




/********************************************************************************/
/*										*/
/*	Key/button actions							*/
/*										*/
/********************************************************************************/

private class ExpandAction extends AbstractAction  {

   private BconGraphNode for_node;

   ExpandAction(BconGraphNode gn) {
      super("Expand node");
      for_node = gn;
   }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.expandNode(for_node);
      node_map.clear();
      relayout();
   }

}	// end of inner class ExapandAction




private class CompactAction extends AbstractAction {

   private BconGraphNode for_node;
   private Set<ArcType> compact_type;

   CompactAction(BconGraphNode gn,ArcType ... at ) {
      super("Compact node for " + at[0].toString());
      for_node = gn;
      compact_type = EnumSet.of(at[0]);
      for (int i = 1; i < at.length; ++i)
	 compact_type.add(at[i]);
    }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.collapseNode(for_node,compact_type);
      relayout();
   }

}	// end of inner class CompactAction


private class InducedAction extends AbstractAction {

   private BconGraphNode for_node;

   InducedAction(BconGraphNode gn) {
      super("Show Subgraph");
      for_node = gn;
    }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.removeStartNodes();
      package_graph.addStartNode(for_node.getFullName());
      relayout();
    }

}	// end of inner class InducedAtion


private class ExcludeAction extends AbstractAction {

   private BconGraphNode for_node;

   ExcludeAction(BconGraphNode gn) {
      super("Remove Node");
      for_node = gn;
    }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.addExclusion(for_node.getFullName());
      relayout();
    }

}	// end of inner class ExcludeAction




private class HomeAction extends AbstractAction {

   HomeAction() {
      super("Show Complete Graph");
    }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.showAllNodes();
      relayout();
    }

}	// end of inner class HomeAction


private class RemoveSelectAction extends AbstractAction {

   private Collection<BconGraphNode> select_nodes;

   RemoveSelectAction(Collection<BconGraphNode> sels) {
      super("Remove Selected Nodes");
      select_nodes = sels;
    }

   @Override public void actionPerformed(ActionEvent e) {
      for (BconGraphNode gn : select_nodes) {
	 package_graph.addExclusion(gn.getFullName());
       }
      relayout();
    }

}	// end of inner class RemoveSelectAction



private class RestrictSelectAction extends AbstractAction {

   private Collection<BconGraphNode> select_nodes;

   RestrictSelectAction(Collection<BconGraphNode> sels) {
      super("Show Selected Subgraph");
      select_nodes = sels;
    }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.removeStartNodes();
      for (BconGraphNode gn : select_nodes) {
	 package_graph.addStartNode(gn.getFullName());
       }
      relayout();
    }

}	// end of inner class RemoveSelectAction







private class SourceAction extends AbstractAction {

   private BconGraphNode for_node;

   SourceAction(BconGraphNode gn) {
      super("Go to source of " + gn.getLabelName());
      for_node = gn;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String mnm = for_node.getFullName();
      String proj = package_graph.getProject();
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,mnm);
      if (bb == null) return;
      BoardMetrics.noteCommand("BCON","GotoMethodSource");
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BconPackageDisplay.this);
      if (bba != null) {
	 bba.addBubble(bb,BconPackageDisplay.this,null,
			  PLACEMENT_PREFER|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class SourceAction




private class SearchAction extends AbstractAction {

   private BconGraphNode for_node;

   SearchAction(BconGraphNode gn) {
      super("Search in " + gn.getLabelName());
      for_node = gn;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String mnm = for_node.getFullName();
      String proj = package_graph.getProject();
      BudaBubble bb = BassFactory.getFactory().createSearch(SearchType.SEARCH_CODE,proj,mnm);
      if (bb == null) return;
      BoardMetrics.noteCommand("BCON","GotoSearch");
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BconPackageDisplay.this);
      if (bba != null) {
	 bba.addBubble(bb,BconPackageDisplay.this,null,
			  PLACEMENT_PREFER|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class SearchAction




/********************************************************************************/
/*										*/
/*     Delayed Evaluation							*/
/*										*/
/********************************************************************************/

private class ComputeGraph implements Runnable {

   private boolean need_recompute;
   private boolean doing_compute;

   ComputeGraph() {
      need_recompute = false;
      doing_compute = false;
   }

   @Override public void run() {
      synchronized (this) {
	 if (doing_compute) {
	    need_recompute = true;
	    return;
	 }
	 doing_compute = true;
      }

      for ( ; ; ) {
	 package_graph.getNodes();
	 synchronized (this) {
	    if (!need_recompute) {
	       doing_compute = false;
	       break;
	    }
	    need_recompute = false;
	 }
      }

      SwingUtilities.invokeLater(new SetupGraph());
   }

}	// end of inner class ComputeGraph



private class SetupGraph implements Runnable {

   @Override public void run() {
      setupGraphModel();
      repaint();
    }

}	// end of inner class SetupGraph




}	// end of class BconPackageDisplay




/* end of BconPackageDisplay.java */

