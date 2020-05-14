/********************************************************************************/
/*                                                                              */
/*              BicexVarHistory.java                                            */
/*                                                                              */
/*      Compute variable history                                                */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bicex;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.LineBorder;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.ivy.jcomp.JcompSymbolKind;
import edu.brown.cs.ivy.mint.MintConstants;
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

class BicexVarHistory implements BicexConstants, MintConstants 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private long            start_time;
private BicexEvaluationViewer for_viewer;
private VarNode         start_node;
private VarHistoryPanel history_panel;

enum VarNodeType {
   VALUE, SET, STATEMENT, PARAMETER, CALL
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BicexVarHistory(BicexEvaluationViewer ev,BicexValue bv,String name)
{
   for_viewer = ev;
   
   BicexExecution ex = ev.getExecution();
   start_time = ex.getCurrentTime();
   
   start_node = new VarNode(VarNodeType.VALUE,ex.getCurrentContext(),start_time,name,bv);
   
   history_panel = null;
}




/********************************************************************************/
/*                                                                              */
/*      Processing method                                                       */
/*                                                                              */
/********************************************************************************/

void process()
{
   addDependentNodes(start_node);
   if (history_panel == null) {
      history_panel = new VarHistoryPanel();
      for_viewer.setHistoryPanel(history_panel);
    }
   history_panel.forceUpdate();
}


void update()
{
   if (start_node == null) return;
   
   BicexEvaluationContext octx = start_node.getContext();
   BicexEvaluationContext nctx = for_viewer.getContextForTime(start_time);
   if (nctx == null) return;
   if (!nctx.getMethod().equals(octx.getMethod())) {
      // attempt to find actual context
      return;
    }
   
   Map<String,BicexValue> vals = nctx.getValues();
   String [] nms = start_node.getName().split("?");
   BicexValue bv = vals.get(nms[1]);
   if (bv == null) return;
   for (int i = 2; i < nms.length; ++i) {
       bv = bv.getChildren(start_time).get(nms[i]);
       if (bv == null) return;
    }
   
   start_node = new VarNode(VarNodeType.VALUE,nctx,start_time,start_node.getName(),bv); 
   BoardThreadPool.start(new Updater());
}



private class Updater implements Runnable {
   
   @Override public void run() {
      process();
    }
   
}       // end of inner class Updater




/********************************************************************************/
/*                                                                              */
/*      Methods to find dependencies                                            */
/*                                                                              */
/********************************************************************************/

private void addDependentNodes(VarNode vn)
{
   long now = vn.getTime();
   long prev = -1;
   for (Integer t : vn.getValue().getTimeChanges()) {
      if (t <= now) prev = t;
      else if (t > now) break;
    }
   
   if (prev <= 0) return;
   
   BicexEvaluationContext pctx = for_viewer.getContextForTime(prev+1);
   VarNode vn1 = new VarNode(VarNodeType.SET,pctx,prev,vn.getName(),vn.getValue());
   if (prev != vn.getTime()) {
      vn.addDependent(vn1);
      vn = vn1;
    }
   
   if (pctx == null) return;
   
   String vnm = vn.getName();
   int idx = vnm.lastIndexOf("?");
   if (idx > 0) vnm = vnm.substring(idx+1);
   
   int line = getLine(pctx,prev);
   if (line <= 0) return;
   
   Element dep = null;
   if (line == getLine(pctx,1)) {
      BicexEvaluationContext par = pctx.getParent();
      int nline = getLine(par,prev);
      // handle call node
      dep = getCallDependencies(vnm,pctx,par,line,nline,prev);
      pctx = par;
      line = nline;
    }
   else {
      dep = getVariableDependencies(vnm,pctx,line,prev);
    }
   
   List<VarNode> vns = findDependents(vn,dep,pctx,line,prev-1); 
   if (vns == null) return;
   
   for (VarNode nvn : vns) {
      vn.addDependent(nvn);
      addDependentNodes(nvn);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Ask seede to find dependencies for a particular statement               */
/*                                                                              */
/********************************************************************************/

List<VarNode> findDependents(VarNode vnorig,Element dep,BicexEvaluationContext ctx,int lno,long when)
{
   if (dep == null) return null;
   
   VarNodeType typ = IvyXml.getAttrEnum(dep,"TYPE",VarNodeType.VALUE);
   String other = null;
   switch (typ) {
      case STATEMENT :
         other = IvyXml.getTextElement(dep,"BODY");
         break;
      case PARAMETER :
         other = IvyXml.getTextElement(dep,"BODY");
         int idx1 = other.indexOf("(");
         if (idx1 > 0) other = other.substring(0,idx1);
         break;
      case CALL :
         other = IvyXml.getTextElement(dep,"INNERMETHOD");
         break;
      case SET :
      case VALUE :
         break;
    }
   vnorig.setNodeType(typ);
   if (other != null) vnorig.setOtherData(other);
   
   List<VarNode> deps = new ArrayList<VarNode>();
   Set<Element> done = new HashSet<>();
   boolean chng = true;
   List<BicexValue> comps = new ArrayList<BicexValue>();
   BicexValue thisv = ctx.getValues().get("this");
   if (thisv != null) comps.add(thisv);
   for (int i = 1; i < 10; ++i) {
      BicexValue thisnv = ctx.getValues().get("this$" + i);
      if (thisnv == null) break;
      comps.add(thisnv);
    }
   
   while (chng) {
      chng = false;
      for (Element var : IvyXml.children(dep,"VAR")) {
         if (done.contains(var)) continue;
         
         String vnm = IvyXml.getAttrString(var,"NAME");
         String vty = IvyXml.getAttrString(var,"TYPE");
         JcompSymbolKind knd = IvyXml.getAttrEnum(var,"KIND",JcompSymbolKind.NONE);
         List<BicexValue> bvs = new ArrayList<>();
         switch (knd) {
            case FIELD :
               System.err.println("FIELD " + vnm + " " + vty);
               for (BicexValue compv : comps) {
                  Map<String,BicexValue> chld = compv.getChildren(when);
                  if (chld != null && chld.get(vnm) != null) {
                     bvs.add(chld.get(vnm));
                     done.add(var);
                   }
                }
               break;
               
            case NONE :
            case CLASS :
            case INTERFACE :
            case ENUM :
            case METHOD :
            case CONSTRUCTOR :
            case PACKAGE :
            case ANNOTATION :
            case ANNOTATION_MEMBER :
               done.add(var);
               break;
               
            case LOCAL : 
               done.add(var);
               String lclnm = ctx.getValueName(vnm,lno);
               if (lclnm == null) break;
               BicexValue val = ctx.getValues().get(lclnm);
               if (val == null) break;
               if (val.getChildren(when) == null) {
                  bvs.add(val);
                }
               else {
                  comps.add(val);
                  chng = true;
                }
               break;
          }
         
         if (bvs.size() > 0) {
            for (BicexValue bv : bvs) {
               VarNode vn = new VarNode(VarNodeType.VALUE,ctx,when,vnm,bv);
               deps.add(vn);
             }
          }
       }
    }
   
   
   return deps;
}



private Element getVariableDependencies(String name,BicexEvaluationContext ctx,int lno,long when) 
{
   if (when == 0) return null;
   
   CommandArgs args = new CommandArgs("FILE",ctx.getFileName(),
         "LINE",lno,
         "TIME",when,
         "CONTEXT",ctx.getId(),
         "VARAIBLE",name);
   Element rslt = for_viewer.getExecution().sendSeedeMessage("VARHISTORY",args,null);
   if (rslt == null) return null;
   if (IvyXml.getChild(rslt,"ERROR") != null) return null;
   
   Element dep = IvyXml.getChild(rslt,"DEPEND");
   
   return dep;
}


private Element getCallDependencies(String name,BicexEvaluationContext ctx,BicexEvaluationContext cctx,
      int lno,int clno,long when)
{
   // need the file and line for the called context
   
   CommandArgs args = new CommandArgs("FILE",ctx.getFileName(),
         "LINE",lno,
         "TIME",when,
         "CONTEXT",ctx.getId(),
         "CALLEDCONTEXT",cctx.getId(),
         "CALLEDFILE",cctx.getFileName(),
         "CALLEDMETHOD",cctx.getMethod(),
         "CALLEDLINE",clno,
         "VARIABLE",name);
   
   Element rslt = for_viewer.getExecution().sendSeedeMessage("VARHISTORY",args,null);
   if (rslt == null) return null;
   if (IvyXml.getChild(rslt,"ERROR") != null) return null;
   
   Element dep = IvyXml.getChild(rslt,"DEPEND");
   
   return dep; 
}


private int getLine(BicexEvaluationContext ctx,long time)
{
   BicexValue lnv = ctx.getValues().get("*LINE*");
   if (lnv == null) return 0;
   
   String xv = lnv.getStringValue(time);
   if (xv == null) return 0;
   
   try {
      return Integer.parseInt(xv);
    }
   catch (NumberFormatException e) { }
   
   return 0;
}



/********************************************************************************/
/*                                                                              */
/*      Variable History Node                                                   */
/*                                                                              */
/********************************************************************************/

private static class VarNode {
   
   private BicexEvaluationContext in_context;
   private long at_time;
   private String var_name;
   private BicexValue var_value;
   private List<VarNode> comes_from;
   private String other_data;
   private VarNodeType node_type;
   
   VarNode(VarNodeType typ,BicexEvaluationContext ctx,long at,String var,BicexValue value) {
      node_type = typ;
      in_context = ctx;
      at_time = at;
      var_name = var;
      var_value = value;
      comes_from = null;
      other_data = null;
      BoardLog.logD("BICEX","Create DEPENDENCY " + ctx.getMethod() + " " + var + 
            " " + at + " " + value);
    }
   
   void addDependent(VarNode vn) {
      if (comes_from == null) comes_from = new ArrayList<>();
      if (!comes_from.contains(vn)) comes_from.add(vn);
    }
   
   void setOtherData(String data)       { other_data = data; }
   void setNodeType(VarNodeType vnt)    { node_type = vnt; }
   
   long getTime()                       { return at_time; }
   String getName()                     { return var_name; }
   BicexValue getValue()                { return var_value; }
   BicexEvaluationContext getContext()  { return in_context; }
   String getOtherData()                { return other_data; }
   VarNodeType getNodeType()            { return node_type; }
   
   List<VarNode> getDependents()        { return comes_from; }

}       // end of inner class VarNode



/********************************************************************************/
/*                                                                              */
/*      Variable history panel                                                  */
/*                                                                              */
/********************************************************************************/

private class VarHistoryPanel extends BicexPanel {
   
   private VarHistoryGraph graph_component;
   
   VarHistoryPanel() {
      super(for_viewer);
      graph_component = new VarHistoryGraph();
    }
   
   @Override protected JComponent setupPanel() {
      return graph_component;
    }
   
   @Override void update() {
      BoardLog.logD("BICEX","Var History Update");
    }
   
   @Override void updateTime() {
    }
   
   void forceUpdate() {
      graph_component.update();
    }
   
   @Override void handlePopupMenu(JPopupMenu menu,MouseEvent evt) {
      GraphNode gn = graph_component.findNode(evt.getPoint());
      if (gn == null) return;
      VarNode vn = gn.getVarNode();
      BicexEvaluationContext ctx = vn.getContext();
      if (ctx == null) return;
      
      long when = vn.getTime();
      if (when > 0) {
         menu.add(getContextTimeAction("Go to " + ctx.getShortName(),ctx,when+1));
       }
      menu.add(getSourceAction(ctx));
    }
   
}       // end of inner class VarHistoryPanel


private class VarHistoryGraph extends JPanel {
   
   private PetalEditor  petal_editor;
   private PetalModelDefault petal_model;
   private PetalLayoutMethod layout_method;
   
   private static final long serialVersionUID = 1;
   
   VarHistoryGraph() {
      super(new BorderLayout());
      setPreferredSize(new Dimension(300,300));
      PetalUndoSupport.getSupport().blockCommands();
      petal_model = new PetalModelDefault();
      petal_editor = new PetalEditor(petal_model);
      PetalLevelLayout levels = new PetalLevelLayout(petal_editor);
      levels.setSplineArcs(false);
      levels.setLevelX(true);
      levels.setOptimizeLevels(true);
      levels.setWhiteFraction(0);
      levels.setWhiteSpace(50);
      layout_method = levels;
      add(petal_editor,BorderLayout.CENTER);
      petal_editor.addZoomWheeler();
    }
   
   @Override public void paintComponent(Graphics g) {
      super.paintComponent(g);
    }
   
   void update() {
      Map<VarNode,GraphNode> nodes = new HashMap<>();
      petal_model.clear();
      
      addNodes(start_node,nodes);
      
      petal_model.fireModelUpdated();
      petal_editor.commandLayout(layout_method);
      Dimension dim = petal_editor.getPreferredSize();
      setSize(dim);
      setPreferredSize(dim);
      setMinimumSize(dim);
      repaint(); 
    }
   
   private GraphNode addNodes(VarNode vn,Map<VarNode,GraphNode> nodes) {
      GraphNode gn = nodes.get(vn);
      if (gn != null) return gn;
      
      gn = new GraphNode(vn);
      nodes.put(vn,gn);
      petal_model.addNode(gn);
      
      if (vn.getDependents() != null) {
         for (VarNode vn1 : vn.getDependents()) {
            GraphNode gn1 = addNodes(vn1,nodes);
            GraphArc ga = new GraphArc(gn,gn1);
            petal_model.addArc(ga);
          }
       }
      
      return gn;
    }
   
   GraphNode findNode(Point pt) {
      PetalNode pn = petal_editor.findNode(pt);
      if (pn != null) return (GraphNode) pn;
      return null;
    }
   
}       // end of inner class VarHistoryGraph





private class GraphNode extends PetalNodeDefault {
   
   private VarNode for_node;
   
   private static final long serialVersionUID = 1;
   
   GraphNode(VarNode vn) {
      for_node = vn;
      BicexEvaluationContext ctx = vn.getContext();
      String mthd = ctx.getShortName();
      int line = getLine(ctx,vn.getTime()+1);
      if (line > 0) mthd = mthd + " @ " + line;
      String nm = vn.getName();
      int idx = nm.indexOf("?");
      if (idx > 0) nm = nm.substring(idx+1);
      nm = nm.replace("?",".");
      String val = vn.getValue().getStringValue(vn.getTime()+1);
      String typ = vn.getValue().getDataType(vn.getTime()+1);
      if (typ.equals("java.lang.String")) {
         val = val.replace("\\","\\\\");
         val = val.replace("\"","\\\"");
         val = val.replace("\n","\\n");
         val = val.replace("\t","\\t");
         val = val.replace("\b","\\b");
         val = "\"" + val + "\"";
       }
      
      StringBuffer buf = new StringBuffer();
      buf.append("<html><body>");
      buf.append("<p>" + nm);
      buf.append("<hl><p>= " + val);
      buf.append("<hl><p>@ " + mthd);
      if (vn.getOtherData() != null) {
         buf.append("<hl><p>" + vn.getOtherData());
       }
      buf.append("<hl><p>&gt; " + vn.getNodeType());
      JLabel lbl = new JLabel(buf.toString());
      lbl.setOpaque(true);
      lbl.setBackground(BoardColors.getColor("Bicex.VarHistoryNode"));
      lbl.setBorder(new LineBorder(BoardColors.getColor("Bicex.VarHistoryNodeBorder"),1));
      Dimension d2 = lbl.getPreferredSize();
      lbl.setSize(d2);
      
      setComponent(lbl);
    }
   
   VarNode getVarNode()                         { return for_node; }
   
}       // end of inner class GraphNode



private class GraphArc extends PetalArcDefault  {
   
   private static final long serialVersionUID = 1;
   
   GraphArc(GraphNode f,GraphNode t) {
      super(f,t);
      setSourceEnd(new PetalArcEndDefault(PetalArcEnd.PETAL_ARC_END_ARROW));
    }
   
}

}       // end of class BicexVarHistory




/* end of BicexVarHistory.java */

