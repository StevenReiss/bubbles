/********************************************************************************/
/*										*/
/*		BdynTaskPanel.java						*/
/*										*/
/*	Panel for actual visualization display					*/
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



package edu.brown.cs.bubbles.bdyn;


import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


class BdynTaskPanel extends JPanel implements BdynConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BdynTaskWindow task_win;
private long task_min_time;
private long task_max_time;
private boolean is_frozen;
private long freeze_min_time;	       // min time when frozen
private long freeze_max_time;

private Map<BdynCallback,Color> callback_set;
private int color_counter;

private double cur_width;
private double cur_mouse;
private double cur_delta;
private double cur_tdelta;

private Stroke	mark_stroke;

private static final long serialVersionUID = 1;

private double min_step = 10000000;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdynTaskPanel(BdynTaskWindow tw)
{
   task_win = tw;

   setPreferredSize(new Dimension(400,300));
   task_min_time = 0;
   task_max_time = 0;
   callback_set = null;
   color_counter = 0;
   cur_width = 0;
   cur_mouse = -1;

   Mouser mm = new Mouser(this);
   addMouseListener(mm);
   addMouseMotionListener(mm);
   addMouseWheelListener(mm);
   setFocusable(true);
   addMouseListener(new BudaConstants.FocusOnEntry());
   addKeyListener(new Keyer());

   mark_stroke = new BasicStroke(1f,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER,1f,new float [] { 4f,4f },0f);

   setToolTipText("Task Panel");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setTimes(long min,long max)
{
   if (is_frozen) {
      freeze_min_time = min;
      freeze_max_time = max;
    }
   else if (task_min_time != min || task_max_time != max) {
      task_min_time = min;
      task_max_time = max;
      repaint();
    }
}


void freeze(boolean set)
{
   if (is_frozen == set) return;

   if (is_frozen) {
      is_frozen = false;
      if (freeze_max_time != 0) {
	 setTimes(freeze_min_time,freeze_max_time);
       }
    }
   else {
      is_frozen = true;
      freeze_min_time = task_min_time;
      freeze_max_time = task_max_time;
    }
}


void clearCallbacks()
{
   callback_set = null;
   color_counter = 0;
   cur_mouse = -1;
   cur_delta = 0;
}



/********************************************************************************/
/*										*/
/*	Paint methods								*/
/*										*/
/********************************************************************************/

@Override public void paintComponent(Graphics g0)
{
   Graphics2D g = (Graphics2D) g0;
   int rows = task_win.getThreads().size();
   Dimension dim = getSize();
   g.setBackground(BoardColors.getColor("Bdyn.BackgroundColor"));
   g.clearRect(0,0,dim.width,dim.height);
   if (task_win.getEventTrace() == null || rows == 0 ||
	  task_min_time == task_max_time || task_min_time < 0)
      return;

   Long nextmark = null;
   Iterator<Long> markiter = task_win.getEventTrace().getTimeMarkIterator();
   if (markiter.hasNext()) nextmark = markiter.next();

   BdynRangeSet rset = null;
   double y0 = 0;
   double yinc = dim.getHeight() / rows;
   Rectangle2D r2 = new Rectangle2D.Double();
   cur_width = dim.width;
   long t0 = getTimeAtPosition(0);
   for (int i = 0; i < dim.width; ++i) {
      double y1 = y0;
      long t1 = getTimeAtPosition(i+1);

      if (nextmark != null && nextmark >= t0 && nextmark < t1) {
	 nextmark = null;
	 while (markiter.hasNext()) {
	    nextmark = markiter.next();
	    if (nextmark >= y1) break;
	    nextmark = null;
	 }
	 g.setColor(BoardColors.getColor("Bdyn.MarkColor"));
	 Stroke sg = g.getStroke();
	 g.setStroke(mark_stroke);
	 g.drawLine(i, 0, i, dim.height);
	 g.setStroke(sg);
      }

      if (i == 0) rset = task_win.getEventTrace().getRange(t0,t1);
      else rset = task_win.getEventTrace().updateRange(rset,t0,t1);
      if (rset == null) continue;

      for (BdynEntryThread td : task_win.getThreads()) {
	 Set<BdynEntry> out = rset.get(td);
	 Color outcol = getOutsideColor(out,t1-t0,t1);
	 Map<Color,Double> incol = getInsideColor(out,t1-t0,t1);
	 double ya = y1 + yinc * 0.10;
	 double yb = y1 + yinc * 0.25;
	 double yc = y1 + yinc * 0.75;
	 double yd = y1 + yinc * 0.90;
	 if (outcol != null) {
	    g.setColor(outcol);
	    r2.setFrame(i,ya,1,yb-ya);
	    g.fill(r2);
	    r2.setFrame(i,yc,1,yd-yc);
	    g.fill(r2);
	  }
	 if (incol != null) {
	    double ye = yc;
	    for (Map.Entry<Color,Double> ent : incol.entrySet()) {
	       Color c0 = ent.getKey();
	       double v0 = ent.getValue();
	       double yf = ye - (yc-yb)*v0;
	       r2.setFrame(i,yf,1,ye-yf);
	       g.setColor(c0);
	       g.fill(r2);
	       ye = yf;
	     }
	  }
	 y1 += yinc;
       }
      t0 = t1;
    }
   for (int i = 1; i < rows; ++i) {
      int y1 = (int) (yinc * i + 0.5);
      g.setColor(BoardColors.getColor("Bdyn.LineColor"));
      g.drawLine(0,y1,dim.width,y1);
    }
}




/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent evt)
{
   if (task_win.getEventTrace() == null) return "Task Panel";

   Dimension dim = getSize();
   int rows = task_win.getThreads().size();
   double yinc = dim.getHeight()/rows;
   int row = (int)(evt.getY()/yinc);
   if (row >= task_win.getThreads().size()) return "Task Panel";
   BdynEntryThread thr = task_win.getThreads().get(row);
   if (thr == null) return "Task Panel";

   double t0 = getTimeAtPosition(evt.getX());
   double t1 = getTimeAtPosition(evt.getX()+1);
   BdynRangeSet alldata = task_win.getEventTrace().getRange((long) t0, (long) t1);
   Set<BdynEntry> data = null;
   if (alldata != null) data = alldata.get(thr);

   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   buf.append("<p>Thread: " + thr.getThreadName());

   buf.append("<p>Time: ");
   double t2 = (t0+t1)/2 - task_min_time;
   t2 = t2 / 1000000.0; 	// convert to milliseconds
   if (t2 > 1000) {
      DecimalFormat df = new DecimalFormat("#,##0.000");
      buf.append(df.format(t2/1000.0));
      buf.append(" Seconds");
    }
   else {
      DecimalFormat df = new DecimalFormat("0.000");
      buf.append(df.format(t2));
      buf.append(" Milliseconds");
    }

   if (data != null) {
      for (BdynEntry ent : data) {
	 buf.append("<p>");
	 buf.append(getLabel(ent.getEntryTransaction()));
	 buf.append(" :: ");
	 buf.append(getLabel(ent.getEntryTask()));
       }
    }

   return buf.toString();
}



private String getLabel(BdynCallback cb)
{
   if (cb == null) return "?";
   return cb.getDisplayName();
}




/********************************************************************************/
/*										*/
/*	Context menu methods							*/
/*										*/
/********************************************************************************/

void handleContextMenu(JPopupMenu menu,Point p,MouseEvent evt)
{
   Set<BdynEntry> data = null;
   BdynEntryThread thr = null;
   Dimension dim = getSize();
   double t0 = 0;
   double t1 = 0;
   int rows = task_win.getThreads().size();
   double yinc = dim.getHeight()/rows;
   int row = (int)(p.getY()/yinc);
   if (row < task_win.getThreads().size()) {
      thr = task_win.getThreads().get(row);
      if (thr != null) {
	 t0 = getTimeAtPosition(p.getX());
	 t1 = getTimeAtPosition(p.getX()+1);
	 BdynRangeSet alldata = task_win.getEventTrace().getRange((long) t0, (long) t1);
	 if (alldata != null) data = alldata.get(thr);
       }
    }

   Set<BdynCallback> done = new HashSet<BdynCallback>();
   if (data != null && data.size() > 0) {
      for (BdynEntry be : data) {
	 BdynCallback cb1 = be.getEntryTransaction();
	 if (cb1 != null && !done.contains(cb1)) {
	    done.add(cb1);
	    menu.add(new CallbackLabeler(cb1));
	    menu.add(new CallbackColorer(cb1,null));
	  }
	 BdynCallback cb2 = be.getEntryTask();
	 if (cb2 != null && !done.contains(cb2)) {
	    done.add(cb2);
	    menu.add(new CallbackLabeler(cb2));
	    menu.add(new CallbackColorer(cb2,null));
	  }
       }
    }

   if (t0 != t1 && t0 != 0) {
      menu.add(new TimeMarker((long) t0));
      menu.add(new FreezeAction());
    }

   menu.add(new ShowOptions());
}



private class CallbackLabeler extends AbstractAction {

   private BdynCallback for_callback;
   private static final long serialVersionUID = 1;
   
   CallbackLabeler(BdynCallback cb) {
      super("Set Label for " + cb.getMethodName());
      for_callback = cb;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String fnm = for_callback.getClassName() + "." + for_callback.getMethodName();
      String lbl = for_callback.getDisplayName();
      if (lbl.equals(fnm)) lbl = null;
      String args = for_callback.getArgs();
      String dnm = fnm;
      if (args != null) dnm += args;
      String rslt = JOptionPane.showInputDialog(BdynTaskPanel.this,"Enter label for " + dnm,lbl);
      if (rslt == null) return;
      if (rslt.length() == 0) rslt = null;
      else if (rslt.equals(fnm)) rslt = null;
      for_callback.setLabel(rslt);
      BdynFactory.getFactory().saveCallbacks();
    }

}	// end of inner class CallbackLabeler


private class CallbackColorer extends AbstractAction {

   private BdynCallback for_callback;
   private Color	set_color;
   private JColorChooser  color_chooser;
   private JDialog	task_dialog;
   private static final long serialVersionUID = 1;
   
   CallbackColorer(BdynCallback cb,Color c) {
      super("Set Color for " + cb.getMethodName());
      for_callback = cb;
      set_color = c;
      color_chooser = null;
      task_dialog = null;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd != null && cmd.equalsIgnoreCase("OK")) {
	 set_color = color_chooser.getColor();
	 color_chooser = null;
	 if (task_dialog != null) task_dialog.setVisible(false);
	 task_dialog = null;
	 if (set_color == null) return;
       }
      else if (cmd != null && cmd.equalsIgnoreCase("Cancel")) {
	 if (task_dialog != null) task_dialog.setVisible(false);
	 task_dialog = null;
	 color_chooser = null;
	 return;
       }
      if (set_color == null) {
	 String fnm = for_callback.getClassName() + "." + for_callback.getMethodName();
	 String lbl = for_callback.getDisplayName();
	 if (lbl.equals(fnm)) lbl = null;
	 String args = for_callback.getArgs();
	 String dnm = fnm;
	 if (args != null) dnm += args;
	 color_chooser = new JColorChooser(callback_set.get(for_callback));
	 task_dialog = JColorChooser.createDialog(BdynTaskPanel.this,"Pick color for " + dnm,
	       true,color_chooser,this,this);
	 task_dialog.setVisible(true);
	 return;
       }
      if (set_color != null) {
	 for_callback.setUserColor(set_color);
	 callback_set.put(for_callback,set_color);
	 BdynFactory.getFactory().saveCallbacks();
       }
    }
}


private class TimeMarker extends AbstractAction {

   private long at_time;
   private static final long serialVersionUID = 1;
   
   TimeMarker(long when) {
      super("Add Time Mark");
      at_time = when;
   }

   @Override public void actionPerformed(ActionEvent e) {
      task_win.getEventTrace().addTimeMark(at_time);
   }

}	// end of inner class TimeMarker


private class FreezeAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   FreezeAction() {
      super((is_frozen ? "Unfreeze" : "Frozen") + " Display");
    }

   @Override public void actionPerformed(ActionEvent e) {
      freeze(!is_frozen);
    }

}	// end of inner class FreezeAction


private class ShowOptions extends AbstractAction {

   private static final long serialVersionUID = 1;
   
   ShowOptions() {
      super("Show Visualization Options");
    }

   @Override public void actionPerformed(ActionEvent e) {
      OptionPanel pnl = new OptionPanel();
      JOptionPane.showMessageDialog(BdynTaskPanel.this,pnl,"Task Visualization Options",JOptionPane.PLAIN_MESSAGE);
    }

}	// end of inner class ShowOptions




/********************************************************************************/
/*										*/
/*	Time methods								*/
/*										*/
/********************************************************************************/

private long getTimeAtPosition(double pos)
{
   double w = (cur_width == 0 ? 1 : cur_width);

   if (cur_mouse < 0) {
      double ttot = task_max_time - task_min_time;
      long t0 = (long)(task_min_time + (pos * ttot/w) + 0.5);
      return t0;
    }

   double tdelta = task_max_time - task_min_time;
   if (cur_delta < 0 || cur_tdelta != tdelta) {
      cur_tdelta = tdelta;
      cur_delta = findDistance(cur_mouse,w,tdelta);
    }
   double x = findPosition(cur_delta,cur_mouse,w,tdelta,pos);
   // System.err.println("MAP " + pos + " " + x);
   return (long)(task_min_time + x + 0.5);
}



private double findDistance(double m,double w,double t)
{
   double g = min_step;

   if (g*w > t) return 0;		// no scaling needed

   double d0 = 0;
   double d1 = t;
   int mx = (int)(Math.log(t)/Math.log(2));

   for (int i = 0; i < mx; ++i) {
      double d = (d0+d1)/2.0;
      double a1 = Math.atan2(g,d);
      double a2 = Math.atan2(m/w*t,d);
      double a3 = Math.atan2((w-m)/w*t,d);
      double x = a1 / (a2 + a3);
      if (x > 1/w) d0 = d;
      else d1 = d;
    }

   return (d0 + d1) / 2.0;
}



private double findPosition(double d,double m,double w,double t,double i)
{
   double v = t * m/w;

   double x = 0;
   double a4,a5;

   if (m < 0 || d == 0) return i * t/w;

   if (i <= m) {
      double a2 = Math.atan2(m/w*t,d);
      if (m == 0) a4 = 0;
      else a4 = (m-i)/m;
      a5 = a4*a2;
      x = d*Math.tan(a5);
      // double x1 = Math.tan(a2)*d;
      // System.err.println("FIND " + i + " " + x1 + " " + x + " " + a4 + " " + a2 + " " + a5);
      x = v - x;
    }
   else {
      double a3 = Math.atan2((w-m)/w*t,d);
      if (w == m) a4 = 0;
      else a4 = (i-m)/(w-m);
      a5 = a4*a3;
      x = d*Math.tan(a5) + v;
    }

   if (x < 0) x = 0;

   return x;
}





/********************************************************************************/
/*										*/
/*	Color methods								*/
/*										*/
/********************************************************************************/

private Color getOutsideColor(Set<BdynEntry> data,long delta,long endt)
{
   if (data == null || data.isEmpty()) return null;

   AccumMap accum = new AccumMap();
   for (BdynEntry be : data) {
      BdynCallback cb = be.getEntryTransaction();
      long d0 = be.getTotalTime(endt);
      accum.addEntry(cb,d0);
    }

   SortedSet<AccumEntry> rslt = accum.getResult();
   AccumEntry ae = rslt.first();

   return getColor(ae.getCallback());
}


private Map<Color,Double> getInsideColor(Set<BdynEntry> data,long delta,long endt)
{
   if (data == null || data.isEmpty()) return null;

   AccumMap accum = new AccumMap();
   for (BdynEntry be : data) {
      BdynCallback cb = be.getEntryTask();
      long d0 = be.getTotalTime(endt);
      accum.addEntry(cb,d0);
    }
   long d1 = accum.getTotalTime();
   if (d1 > delta) delta = d1;

   Map<Color,Double> rslt = new LinkedHashMap<Color,Double>();
   for (AccumEntry ae : accum.getResult()) {
      Color c0 = getColor(ae.getCallback());
      double f = ((double) ae.getTime())/((double) delta);
      rslt.put(c0,f);
    }

   return rslt;
}



private Color getColor(BdynCallback cb)
{
   if (callback_set == null) {
      callback_set = new HashMap<>();
    }

   Color c = callback_set.get(cb);
   if (c != null) return c;
   c = cb.getUserColor();
   if (c != null) {
      callback_set.put(cb, c);
      return c;
    }

   Set<Color> known = BdynFactory.getFactory().getKnownColors();
   // known.addAll(callback_set.values());

   float h = 0;
   for ( ; ; ) {
      int ct = color_counter++;
      if (ct == 0) h = 0;
      else if (ct == 1) h = 0.5f;
      else {
	 int n0 = 2;
	 float n = ct - 2;
	 float incr = 0.5f;
	 for ( ; ; ) {
	    if (n < n0) {
	       h = incr/2.0f + n*incr;
	       break;
	     }
	    n -= n0;
	    n0 *= 2;
	    incr /= 2.0;
	  }
       }
      c = new Color(Color.HSBtoRGB(h,1f,1f));
      boolean match = false;
      for (Color kc : known) {
	 int delta = Math.abs(c.getRed() - kc.getRed()) +
		Math.abs(c.getGreen() - kc.getGreen()) +
		Math.abs(c.getBlue() - kc.getBlue());
	 if (delta <= 48) {
	    match = true;
	    break;
	  }
       }
      if (!match) break;
    }

   callback_set.put(cb,c);
   // System.err.println("ASSIGN COLOR " + c + " " + cb + " " + color_counter);

   return c;
}



/********************************************************************************/
/*										*/
/*	Classes for handling graphics						*/
/*										*/
/********************************************************************************/

private static class AccumEntry implements Comparable<AccumEntry> {

   private BdynCallback call_back;
   private long total_time;

   AccumEntry(BdynCallback cb,long tot) {
      call_back = cb;
      total_time = tot;
    }

   BdynCallback getCallback()		{ return call_back; }
   long getTime()			{ return total_time; }

   @Override public int compareTo(AccumEntry e) {
      long d = e.total_time - total_time;
      if (d < 0) return -1;
      if (d > 0) return 1;
      return 0;
    }

}	// end of inner class AccumEntry




private static class AccumMap extends HashMap<BdynCallback,long []> {

   private long total_time;
   private static final long serialVersionUID = 1;
   
   AccumMap() {
      total_time = 0;
    }

   void addEntry(BdynCallback cb,long delta) {
      long [] v = get(cb);
      if (v == null) {
	 v = new long[1];
	 v[0] = 0;
	 put(cb,v);
       }
      v[0] += delta;
      total_time += delta;
    }

   long getTotalTime()			{ return total_time; }

   SortedSet<AccumEntry> getResult() {
      SortedSet<AccumEntry> rslt = new TreeSet<AccumEntry>();
      for (Map.Entry<BdynCallback,long []> ent : entrySet()) {
	 AccumEntry ae = new AccumEntry(ent.getKey(),ent.getValue()[0]);
	 rslt.add(ae);
       }
      return rslt;
    }

}	// end of inner class AccumMap



private class Mouser extends MouseAdapter {

   private JPanel for_panel;
   private boolean do_drag;

   Mouser(JPanel pnl) {
      for_panel = pnl;
      do_drag = false;
    }

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
	 setPosition(-1);
       }
    }

   @Override public void mouseDragged(MouseEvent e) {
      if (do_drag) {
	 double x = e.getX();
	 if (x < 0) x = 0;
	 if (x >= cur_width) x = cur_width-1;
	 setPosition(x);
       }
    }

   @Override public void mousePressed(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
	 setPosition(e.getX());
	 do_drag = true;
       }
    }

   @Override public void mouseReleased(MouseEvent e) {
      do_drag = false;
    }

   @Override public void mouseWheelMoved(MouseWheelEvent e) { }

   private void setPosition(double v) {
      double opos = cur_mouse;
      if (v < 0 || v >= cur_width) {
	 cur_mouse = -1;
	 cur_delta = 0;
       }
      else if (cur_mouse != v) {
	 cur_mouse = v;
	 cur_delta = -1;
       }
      if (cur_mouse != opos) for_panel.repaint();
    }

}	// end of inner class Mouser


private class Keyer extends KeyAdapter {

   Keyer() { }

   @Override public void keyPressed(KeyEvent e) {
      PointerInfo pi = MouseInfo.getPointerInfo();
      Point p1 = SwingUtilities.convertPoint(pi.getDevice().getFullScreenWindow(),pi.getLocation(),BdynTaskPanel.this);

      long time = 0;

      if (e.getKeyChar() == 't' || e.getKeyChar() == 'T') {
	 time= task_win.getEventTrace().getEndTime();
      }
      else if (e.getKeyChar() == 'm' || e.getKeyChar() == 'M') {
	 time = getTimeAtPosition(p1.x);
      }
      if (time > 0) task_win.getEventTrace().addTimeMark(time);
    }

}	// end of inner class Keyer



/********************************************************************************/
/*										*/
/*	Option panel								*/
/*										*/
/********************************************************************************/

private class OptionPanel extends SwingGridPanel implements ActionListener {

   private static final long serialVersionUID = 1;
   
   OptionPanel() {
      setupPanel();
    }

   private void setupPanel() {
      BdynOptions opts = BdynFactory.getOptions();
      beginLayout();
      addBannerLabel("Task Visualization Options");
      addBoolean("Show High-CPU Routines",opts.useKeyCallback(),this);
      addBoolean("Show Main Routine",opts.useMainCallback(),this);
      addBoolean("Allow Main as a Transaction",opts.useMainTask(),this);
      addSeparator();
      addBottomButton("Reset Callback Information","RESET",this);
      addBottomButtons();
      addSeparator();
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      boolean fg = true;
      if (evt.getSource() instanceof JCheckBox) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 fg = cbx.isSelected();
       }
      BdynOptions opts = BdynFactory.getOptions();

      switch (cmd) {
	 case "Show High-CPU Routines" :
	    opts.setUseKeyCallback(fg);
	    break;
	 case "Show Main Routine" :
	    opts.setUseMainCallback(fg);
	    break;
	 case "Allow Main as a Transaction" :
	    opts.setUseMainTask(fg);
	    break;
	 case "RESET" :
	    int opt = JOptionPane.showConfirmDialog(this,"Do you really want to reset the callback database?");
	    if (opt != JOptionPane.YES_OPTION) break;
	    BdynFactory.getFactory().resetCallbacks();
	    break;
	 case "CLOSE" :
	    setVisible(false);
	    break;
	 default :
	    BoardLog.logE("BDYN","Unknown command: " + cmd);
	    break;
       }
    }

}	// end of inner class OptionPanel


}	// end of class BdynTaskPanel




/* end of BdynTaskPanel.java */
