/********************************************************************************/
/*                                                                              */
/*              BicexDataViewPanel.java                                         */
/*                                                                              */
/*      Show a point map of data writes                                         */
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.*;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.brown.cs.bubbles.board.BoardColors;


class BicexDataViewPanel extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private DataPanel               data_panel;
private List<VarData>           data_map;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BicexDataViewPanel(BicexEvaluationViewer ev)
{
   super(ev);
   data_map = null;
}




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override protected JComponent setupPanel()
{
   data_panel = new DataPanel();
   
   return data_panel;
}



@Override void update()
{
   data_map = null;
   if (data_panel.isVisible()) data_panel.repaint();
}


@Override void updateTime()
{
   if (data_panel.isVisible()) data_panel.repaint();
}



@Override long getRelevantTime(MouseEvent evt)
{
   if (data_panel == null) return super.getRelevantTime(evt);
   
   double x = evt.getX();
   BicexEvaluationContext base = getRootContext();
   double t0 = base.getStartTime();
   double t1 = base.getEndTime();
   double wd = data_panel.getWidth();
   double time = (x/wd)*(t1-t0) + t0;
   long itime = (long)(time + 0.5);
   return itime;
}


/********************************************************************************/
/*                                                                              */
/*      Computation methods                                                     */
/*                                                                              */
/********************************************************************************/

private synchronized List<VarData> getCurrentData()
{
   List<VarData> rslt = data_map;
   if (rslt != null) return rslt;
   
   Map<String,VarData> dmap = new HashMap<>();
   
   BicexEvaluationContext ctx = getRootContext();
   if (ctx == null) return null;
   addContextValues(dmap,ctx);
   removeEmpties(dmap);
   
   Set<VarData> srslt = new TreeSet<VarData>(dmap.values());
   rslt = new ArrayList<>(srslt);
   data_map = rslt;
   
   return rslt;
}



private void addContextValues(Map<String,VarData> dmap,BicexEvaluationContext ctx)
{
   Set<BicexValue> done = new HashSet<>();
   List<VarData> pardata = new ArrayList<>();
   for (Map.Entry<String,BicexValue> ent : ctx.getValues().entrySet()) {
      BicexValue bv = ent.getValue();
      String nm = ent.getKey();
      if (nm.startsWith("*")) continue;
      addContextValue(dmap,ctx,nm,bv,pardata,done);
    }
   
   if (ctx.getInnerContexts() != null) {
      for (BicexEvaluationContext sctx : ctx.getInnerContexts()) {
         addContextValues(dmap,sctx);
       }
    }
}


private void addContextValue(Map<String,VarData> dmap,BicexEvaluationContext ctx,
      String name,BicexValue bv,List<VarData> pardata,Set<BicexValue> done)
{
   if (!done.add(bv)) return;
   
   String nm = ctx.getMethod() + "::" + name;
   VarData vd = dmap.get(nm);
   if (vd == null) {
      vd = new VarData(ctx.getMethod(),name);
      dmap.put(nm,vd);
    }
   for (Integer time : bv.getTimeChanges()) {
      if (time != 0) {
         if (!vd.addTime(time)) break;          // there can only be one set per time, so a duplicate indicates reuse
       }
      if (bv.hasChildren(time)) {
         List<VarData> ndata = new ArrayList<VarData>(pardata);
         ndata.add(vd);
         for (Map.Entry<String,BicexValue> cent : bv.getChildren(time).entrySet()) {
            String cnam = cent.getKey();
            BicexValue cval = cent.getValue();
            if (cnam.startsWith("[")) {
               addContextValue(dmap,ctx,name,cval,pardata,done);
             }
            else {
               String fnm = name + "." + cnam;
               addContextValue(dmap,ctx,fnm,cval,ndata,done);
             }
          }
       }
    }
   
}



private void removeEmpties(Map<String,VarData> dmap)
{
   for (Iterator<VarData> it = dmap.values().iterator(); it.hasNext(); ) {
      VarData vd = it.next();
      if (vd.isEmpty()) it.remove();
    }
}



/********************************************************************************/
/*                                                                              */
/*       Point Map Data Panel                                                   */
/*                                                                              */
/********************************************************************************/

private class DataPanel extends JPanel {
   
   private static final long serialVersionUID = 1;
   
   DataPanel() {
      setToolTipText("Data Panel");
    }
   
   @Override public void paintComponent(Graphics g) { 
      List<VarData> svd = getCurrentData();
      if (svd == null) return;
      
      Graphics2D g2 = (Graphics2D) g;
      
      g2.setColor(BoardColors.getColor("Bicex.DataViewBackground")); 
      g2.fillRect(0,0,getWidth(),getHeight());
      
      BicexEvaluationContext base = getRootContext();
      double t0 = base.getStartTime();
      double t1 = base.getEndTime();
      double nrow = svd.size();
      
      double wd = getWidth();
      double ht = getHeight();
      double rowht = ht/nrow;
      
      long now = getExecution().getCurrentTime();
      int timex = (int)((now - t0) / (t1 - t0) * wd);
      g2.setColor(BoardColors.getColor("Bicex.DataViewLine"));
      g2.drawLine(timex,0,timex,(int) ht);
      
      Rectangle2D r2 = new Rectangle2D.Double(0,0,4,4);
      
      int ct = 0;
      for (VarData vd : svd) {
         for (Integer time : vd.getTimes()) {
            if (time < t0 || time > t1) continue;
            double cx = (time - t0) / (t1-t0) * wd;
            double cy = (ct * rowht) + rowht/2;
            r2.setFrame(cx,cy,4,4);
            Color c = getColorForMethod(vd.getColorName());
            g2.setColor(c);
            g2.fill(r2);
          }
         ++ct;
       }
    }
   
   @Override public String getToolTipText(MouseEvent evt) {
      double x = evt.getX();
      BicexEvaluationContext base = getRootContext();
      double t0 = base.getStartTime();
      double t1 = base.getEndTime();
      double wd = getWidth();
      double time = (x/wd)*(t1-t0) + t0;
      int itime = (int)(time + 0.5);
      int tdelta = (int)(2 * (t1-t0)/wd);
      
      List<VarData> svd = getCurrentData();
      if (svd == null) return "";
      
      double ht = getHeight();
      double nrow = svd.size();
      double rowht = ht/nrow; 
      int row = (int)(evt.getY() / rowht);
      if (row < 0 || row >= svd.size()) return "";
      
      VarData vd = null;
      for (int i = 0; i < 15; ++i) {
          int drow = row;
          if (i > 0) {
             int delta = (i+1)/2;
             if ((i&1) != 0) drow -= delta;
             else drow += delta;
           }
          if (drow < 0 || drow >= svd.size()) continue;
          VarData vd1 = svd.get(drow);
          if (vd1.isActiveAtTime(itime,tdelta)) {
             vd = vd1;
             break;
           }
       }
      
      if (vd == null) return "";
      
      BicexEvaluationContext cur = eval_viewer.getContextForTime(base,itime);
      if (cur == null) return "";
      
      return vd.getToolTip();
    }
}       // end of inner class DataPanel



/********************************************************************************/
/*                                                                              */
/*      Representation of a variable                                            */
/*                                                                              */
/********************************************************************************/

private static class VarData implements Comparable<VarData> {
   
   private String context_name;
   private String var_name;
   private SortedSet<Integer> write_times;
   
   VarData(String ctx,String var) {
      context_name = ctx;
      var_name = var;
      write_times = new TreeSet<>();
    }
   
   boolean addTime(int when) {
      return write_times.add(when);
    }
   
   boolean isActiveAtTime(int when,int err) {
      SortedSet<Integer> tail = write_times.tailSet(when-err);
      if (tail.isEmpty()) return false;
      int next = tail.first();
      if (Math.abs(next-when) <= 2*err) return true;
      return false;
    }
   
   boolean isEmpty()                            { return write_times.isEmpty(); }
   
   Collection<Integer> getTimes()               { return write_times; }
   
   String getColorName()                        { return context_name; }
   
   String getToolTip() {
      return var_name + " IN " + context_name;
    }
   
   @Override public int compareTo(VarData vd) {
      if (vd == this) return 0;
      int t0 = write_times.first();
      int t1 = vd.write_times.first();
      if (t0 < t1) return -1;
      else if (t0 > t1) return 1;
      else return var_name.compareTo(vd.var_name);
    }
   
}       // end of inner class VarData




}       // end of class BicexDataViewPanel




/* end of BicexDataViewPanel.java */

