/********************************************************************************/
/*                                                                              */
/*              BvcrDiffViewer.java                                             */
/*                                                                              */
/*      File difference display                                                 */
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.swing.SwingTextArea;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.ScrollPaneLayout;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

class BvcrDiffViewer implements BvcrConstants, BudaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File                    for_file;
private BudaBubble              base_bubble;
private Point                   base_point;
private BudaBubbleArea          bubble_area;
private String                  for_project;
private BvcrFactory             for_factory;
private Map<String,BvcrFileVersion> version_set;
private DiffBubble              diff_bubble;

private JLabel current_label;
private JComboBox<BvcrFileVersion> known_versions;
private BvcrFileVersion         left_version;
private BvcrFileVersion         right_version;
private BvcrDifferenceFile      right_diffs;
private BvcrFileVersion         next_left;
private BvcrFileVersion         next_right;
private BvcrDifferenceFile      next_diffs;
private boolean                 doing_update;
private Object                  version_lock;

private DiffFileView left_display;
private DiffFileView right_display;
private RevisionBar left_bar;
private RevisionBar right_bar;
private CenterPanel center_panel;

private static Dimension panel_size;
private static Dimension line_size;
private static Stroke   base_stroke;

private static int tab_size =	 BoardProperties.getProperties("Bvcr").getInt("Bvcr.tabsize",8);

static {
   JTextArea ar = new SwingTextArea();
   ar.setRows(10);
   ar.setColumns(32);
   panel_size = ar.getPreferredSize();
   ar.setRows(1);
   ar.setColumns(1);
   line_size = ar.getPreferredSize();
   
   base_stroke = new BasicStroke(1.5f);
}


enum ChangeType {
   NONE,
   INSERT, 
   DELETE,
   CHANGE
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BvcrDiffViewer(BaleContextConfig cfg)
{
   this();
   
   base_bubble = cfg.getEditor();
   base_point = null;
   bubble_area = BudaRoot.findBudaBubbleArea(base_bubble);
   for_project = base_bubble.getContentProject();
   for_file = base_bubble.getContentFile();
}



BvcrDiffViewer(BudaBubble bb,Point where,BumpLocation loc)
{
   this();
   
   base_bubble = bb;
   base_point = where;
   bubble_area = BudaRoot.findBudaBubbleArea(bb);
   for_project = loc.getProject();
   for_file = loc.getFile();
}



private BvcrDiffViewer()
{
   for_factory = BvcrFactory.getFactory();
   version_set = null;
   diff_bubble = null;
   left_version = null;
   right_version = null;
   right_diffs = null;
   version_lock = new Object();
   next_left = null;
   next_right = null;
   next_diffs = null;
   doing_update = false;
   left_display = null;
   right_display = null;
   center_panel = null;
   left_bar = null;
   right_bar = null;
}




/********************************************************************************/
/*                                                                              */
/*      Procssing methods                                                       */
/*                                                                              */
/********************************************************************************/

void process()
{
   if (for_file == null) return;
   
   diff_bubble = new DiffBubble(setupWindow());
   bubble_area.addBubble(diff_bubble,base_bubble,base_point,BudaConstants.PLACEMENT_LOGICAL);
   
   HistoryGather hg = new HistoryGather();
   BoardThreadPool.start(hg);
}



/********************************************************************************/
/*                                                                              */
/*      History setup methods -- get version set                                */
/*                                                                              */
/********************************************************************************/

private class HistoryGather implements Runnable {

   HistoryGather() { }

   @Override public void run() {
      String file = for_file.getAbsolutePath();
      version_set = new HashMap<String,BvcrFileVersion>();
      
      Element e1 = for_factory.getHistoryForFile(for_project,file);
      e1 = IvyXml.getChild(e1,"HISTORY");
      if (e1 == null) return;
      
      for (Element ve : IvyXml.children(e1,"VERSION")) {
         BvcrFileVersion bfv = new BvcrFileVersion(ve,version_set);
         version_set.put(bfv.getVersionId(),bfv);
       }
      
      historyReady();
    }

}	// end of inner class HistoryGather




/********************************************************************************/
/*                                                                              */
/*      Windowing methods                                                       */
/*                                                                              */
/********************************************************************************/

private JPanel setupWindow()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.setInsets(0);
   JLabel lbl = new JLabel("Version Differences for " + for_file);
   pnl.addGBComponent(lbl,0,0,0,1,10,0);
   
   pnl.addGBComponent(new JLabel("Version: "),1,1,1,1,0,0);
   current_label = new JLabel("<getting data>");
   pnl.addGBComponent(current_label,2,1,1,1,10,0);
   
   JLabel spc = new JLabel("     ");
   Dimension csz = new Dimension(20,10);
   spc.setSize(csz);
   spc.setMinimumSize(csz);
   spc.setMaximumSize(csz);
   pnl.addGBComponent(spc,3,1,1,1,0,0);
   
   pnl.addGBComponent(new JLabel("Version: "),4,1,1,1,0,0);
   Vector<BvcrFileVersion> vers = new Vector<BvcrFileVersion>();
   known_versions = new JComboBox<BvcrFileVersion>(vers);
   known_versions.addActionListener(new VersionSelect());
   pnl.addGBComponent(known_versions,5,1,1,1,10,0);
   
   left_bar = new RevisionBar(true);
   pnl.addGBComponent(left_bar,0,2,1,1,0,10);
   left_display = new DiffFileView(true);
   pnl.addGBComponent(left_display.getComponent(),1,2,2,1,10,10);
   center_panel = new CenterPanel();
   center_panel.setMinimumSize(csz);
   pnl.addGBComponent(center_panel,3,2,1,1,0,10);
   right_display = new DiffFileView(false);
   pnl.addGBComponent(right_display.getComponent(),4,2,2,1,10,10);
   right_bar = new RevisionBar(false);
   pnl.addGBComponent(right_bar,6,2,1,1,0,10);
   
   synchronizeScrolling();
      
   return pnl;
}



private Color getColorForChange(ChangeType ct,boolean bkg)
{
   String col = null;
   switch (ct) {
      default :
      case NONE :
         col = "Bvcr.DiffFileNone";
         break;
      case INSERT :
         col = "Bvcr.DiffFileInsert";
         break;
      case DELETE :
         col = "Bvcr.DiffFileDelete";
         break;
      case CHANGE :
         col = "Bvcr.DiffFileChange";
         break;
    }
   
   Color color = BoardColors.getColor(col);
   
   return color;
}




private void historyReady()
{
   synchronized(version_lock) {
      left_version = null;
      current_label.setText("CURRENT FILE");
      right_version = null;
      right_diffs = null;
      next_left = null;
      next_right = null;
      next_diffs = null;
      doing_update = false;
    }
   
   if (left_version == null) current_label.setText("<Current File>");
   else current_label.setText(left_version.toString());
   
   right_version = null;
   Set<BvcrFileVersion> pvers = new HashSet<BvcrFileVersion>(version_set.values());
   Vector<BvcrFileVersion> priors = new Vector<BvcrFileVersion>(pvers);
   known_versions.removeAll();
   Collections.sort(priors,new VersionSorter());
   for (BvcrFileVersion pver : priors) {
      if (pver != left_version) known_versions.addItem(pver);
    }
   
   Dimension sz = known_versions.getPreferredSize();
   current_label.setSize(sz);
   current_label.getParent().invalidate();
}


private class VersionSorter implements Comparator<BvcrFileVersion> {
   
   @Override public int compare(BvcrFileVersion v1,BvcrFileVersion v2) {
      return v2.getVersionTime().compareTo(v1.getVersionTime());
    }
   
}       // end of inner class VersionSorter




/********************************************************************************/
/*                                                                              */
/*      Select and setup version to compare                                     */
/*                                                                              */
/********************************************************************************/

private void diffsReady(BvcrFileVersion fver,BvcrFileVersion tver,BvcrDifferenceFile diffs)
{
   for ( ; ; ) {
      synchronized (version_lock) {
         if (doing_update) {
            next_left = fver;
            next_right = tver;
            next_diffs = diffs;
            return;
          }
         if (left_version != fver || right_version != tver) return;
         if (right_diffs != null) return;
         right_diffs = diffs;
         next_left = null;
         next_right = null;
         next_diffs = null;
         doing_update = true;
       }
      
      DisplayData dd = new DisplayData(fver,tver,diffs);
      dd.setupDisplay();
      
      synchronized (version_lock) {
         doing_update = false;
         if (next_left != null || next_right != null || next_diffs != null) {
            fver = next_left;
            tver = next_right;
            diffs = next_diffs;
          }
         else {
            SwingUtilities.invokeLater(new FinishSetup(diffs));
            break;
          }
       }
    }
}


private class FinishSetup implements Runnable {
   
   private BvcrDifferenceFile file_diffs;
   
   FinishSetup(BvcrDifferenceFile diffs) {
      file_diffs = diffs;
    }
   
   @Override public void run() {
      left_display.afterLoad();
      right_display.afterLoad();
      center_panel.setDifferences(file_diffs);
      left_bar.setDifferences(file_diffs);
      right_bar.setDifferences(file_diffs);
    }
   
}




private class VersionSelect implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      int selidx = known_versions.getSelectedIndex();
      if (selidx < 0) return;
      BvcrFileVersion sver = known_versions.getItemAt(selidx);
      if (sver == null) return;
      DeltaGather dg = null;
      synchronized (version_lock) {
         right_version = sver;
         right_diffs = null;
         dg = new DeltaGather(right_version,left_version);
       }
      BoardThreadPool.start(dg);
   }
   
}       // end of inner class VersionSelect



private class DeltaGather implements Runnable {
   
   private BvcrFileVersion from_version;
   private BvcrFileVersion to_version;
   
   DeltaGather(BvcrFileVersion fver,BvcrFileVersion tver) {
      from_version = fver;
      to_version = tver;
    }
   
   @Override public void run() {
      String fid = (from_version == null ? null : from_version.getVersionId());
      String tid = (to_version == null ? null : to_version.getVersionId());
      Element e2 = for_factory.getFileDifferences(for_project,for_file.getAbsolutePath(),
            fid,tid);
      Element de = IvyXml.getChild(e2,"DIFFERENCES");
      Element fe = IvyXml.getChild(de,"FILE");
      BvcrDifferenceFile bdf = new BvcrDifferenceFile(fe);
      diffsReady(to_version,from_version,bdf);
    }
   
}       // end of inner class DeltaGather




/********************************************************************************/
/*                                                                              */
/*      Structure for line information                                          */
/*                                                                              */
/********************************************************************************/

private class DisplayData {
   
   private Document left_document;
   private Document right_document;
   private BufferedReader left_text;
   private BvcrDifferenceFile right_delta;
   private Highlighter left_highlighter;
   private Highlighter right_highlighter;
   
   DisplayData(BvcrFileVersion lver,BvcrFileVersion rver,BvcrDifferenceFile diffs) {
      left_document = left_display.getDocument();
      right_document = right_display.getDocument();;
      left_highlighter = left_display.getHighlighter();
      right_highlighter = right_display.getHighlighter();
      right_delta = diffs;
      left_text = null;
      if (lver == null) {
         try {
            left_text = new BufferedReader(new FileReader(for_file));
          }
         catch (Exception e) { }
       }
      else {
         // need to get text for LHS version
       }
    }
   
   void setupDisplay() {
      try {
         left_highlighter.removeAllHighlights();
         right_highlighter.removeAllHighlights();
         left_document.remove(0,left_document.getLength());
         right_document.remove(0,right_document.getLength());
       }
      catch (BadLocationException e) { }
      
      List<BvcrFileChange> changes = right_delta.getChanges();
      Iterator<BvcrFileChange> chngiter = changes.iterator();
      BvcrFileChange nextchange = null;
      if (chngiter.hasNext()) nextchange = chngiter.next();
      
      int lno = 0;
      int rlno = 0;
      int delct = 0;
      int delstart = -1;
      ChangeType delchng = ChangeType.NONE;
      try {
         for ( ; ; ) {
            String line = left_text.readLine();
            if (line == null) break;
            if (line.length() == 0) line = " ";
            ++lno;
            if (delct > 0) {
               addLine(left_document,line);
               --delct;
               if (delct == 0) {
                  addHighlight(left_document,delstart,-1,delchng);
                }
               continue;
             }
            if (nextchange == null || nextchange.getTargetLine() > lno) {
               addLine(left_document,line);
               addLine(right_document,line);
               ++rlno;
               continue;
             }
            String [] del = nextchange.getDeletedLines();
            String [] ins = nextchange.getAddedLines();
            delchng = ChangeType.INSERT;
            if (del != null) {
               ChangeType ct = ChangeType.DELETE;
               if (ins != null) {
                  ct = ChangeType.CHANGE;
                  delchng = ChangeType.CHANGE;
                }
               int r0 = right_document.getLength();
               for (String s : del) {
                  if (s == null || s.length() == 0) s = " ";
                  addLine(right_document,s);
                  ++rlno;
                }
               addHighlight(right_document,r0,-1,ct);
             }
            else {
               addHighlight(right_document,-1,-1,ChangeType.INSERT);
             }
            if (ins != null) {
               delstart = left_document.getLength();
               addLine(left_document,line);
               delct = ins.length-1;
               if (delct == 0) {
                  addHighlight(left_document,delstart,-1,delchng);
                }
             }
            else {
               addHighlight(left_document,-1,-1,ChangeType.DELETE);
             }
            if (chngiter.hasNext()) nextchange = chngiter.next();
            else nextchange = null;
          }
       }
      catch (IOException e) {
       }
      left_bar.setLength(lno);
      right_bar.setLength(rlno);
    }
   
   void addHighlight(Document d,int spos,int epos,ChangeType ct) {
      Highlighter hl = left_highlighter;
      if (d == right_document) hl = right_highlighter;
      if (spos < 0) spos = d.getLength()-1;
      if (epos < 0) epos = d.getLength()-1;
      if (spos < 0) spos = 0;
      if (epos < 0) epos = 0;
      
      ChangeHighlighter ch = new ChangeHighlighter(ct);
      try {
         hl.addHighlight(spos,epos,ch);
       }
      catch (BadLocationException e) { }
    }
   
   void addLine(Document d,String line) {
      int pos = d.getLength();
      try {
         d.insertString(pos,line+"\n",null);
       }
      catch (BadLocationException e) { }
    }
  
}       // end of inner class DisplayData



private class ChangeHighlighter implements Highlighter.HighlightPainter {
   
   private ChangeType change_type;
   
   ChangeHighlighter(ChangeType ct) {
      change_type = ct;
    }
   
   @Override public void paint(Graphics g,int off0,int off1,Shape bnds,JTextComponent c) {
      Graphics2D g2 = (Graphics2D) g;
      try {
         Color color = getColorForChange(change_type,true);
         Rectangle2D r0 = SwingText.modelToView2D(c,off0);
         Dimension sz = c.getSize();
         Rectangle pt;
         if (off0 == off1) {
            int nht = (int)(r0.getY() + r0.getHeight());
            pt = new Rectangle(0,nht,sz.width,1);
          }
         else {
            Rectangle2D r1 = SwingText.modelToView2D(c,off1-1);
            int nht = (int)(r1.getY() + r1.getHeight() - r0.getY());
            pt = new Rectangle(0,(int) r0.getY(),sz.width,nht);
          }
         g.setColor(color);
         g2.fill(pt);
       }
      catch (BadLocationException e) { }
      
    }
   
}       // end of inner class ChangeHighlighter






/********************************************************************************/
/*										*/
/*	Display Bubble								*/
/*										*/
/********************************************************************************/

private class DiffBubble extends BudaBubble {

   private static final long serialVersionUID = 1;
   
   DiffBubble(JPanel pnl) {
      setContentPane(pnl);
    }

   @Override public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Dimension sz = getSize();
      g.setColor(BoardColors.getColor("Bvcr.DiffBubbleBackground"));
      g.fillRect(0,0,sz.width,sz.height);
    }

}	// end of inner class DiffBubble



private class DiffFileView {
   
   private JTextComponent  editor_pane;
   private JScrollPane  scroll_pane;
   
   DiffFileView(boolean left) {
      editor_pane = new DiffPanel();
      scroll_pane = new JScrollPane(editor_pane);
      scroll_pane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
      scroll_pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      if (left) {
         LeftScrollPaneLayout layout = new LeftScrollPaneLayout();
         scroll_pane.setLayout(layout);
         layout.syncWithScrollPane(scroll_pane);
         scroll_pane.getVerticalScrollBar().putClientProperty("JScrollBar.isFreeStanding",Boolean.TRUE);
       }
    }
    
   Component getComponent()             { return scroll_pane; }
   Document getDocument()               { return editor_pane.getDocument(); }
   Highlighter getHighlighter()         { return editor_pane.getHighlighter(); }
   JScrollBar getHorizontalBar()        { return scroll_pane.getHorizontalScrollBar(); }
   JScrollBar getVerticalBar()          { return scroll_pane.getVerticalScrollBar(); }
   
   void afterLoad() {
      Dimension sz = editor_pane.getPreferredSize();
      editor_pane.setSize(sz);
    }
   
   int getPosition(int ln0,int ln1) {
      int sz0 = (ln0-1)*line_size.height;
      int sz1 = (ln1-1)*line_size.height;
      JViewport vp = scroll_pane.getViewport();
      Point p = vp.getViewPosition();
      return (sz0+sz1)/2 - p.y;
    }
   
   int getScrollLine() {
      JViewport viewport = scroll_pane.getViewport();
      Point p = viewport.getViewPosition();
      int htoff = viewport.getSize().height / 2;
      int y = p.y + htoff;
      return y / line_size.height;
    }
   
   void scrollToLine(int line) {
      int offset = line * line_size.height + line_size.height/2;
      JViewport viewport = scroll_pane.getViewport();
      int htoff = viewport.getSize().height / 2;
      offset -= htoff;
      if (offset < 0) offset = 0;
      Rectangle vr = viewport.getViewRect();
      Dimension vs = viewport.getViewSize();
      Dimension es = viewport.getExtentSize();
      if (offset > vs.height - es.height) {
         offset = vs.height - es.height;
       }
      Point p = new Point(vr.x,offset);
      viewport.setViewPosition(p);
    }
}       // end of inner class DiffFileView


private class DiffPanel extends SwingTextArea {
   
   private static final long serialVersionUID = 1;
   
   DiffPanel() {
      super(new DefaultStyledDocument());
      setEditable(false);
      setLineWrap(false);
      setTabSize(tab_size);
    }
   
   @Override public Dimension getPreferredScrollableViewportSize() {
      return panel_size;
    }
   
}



private class CenterPanel extends JPanel implements AdjustmentListener {
   
   private BvcrDifferenceFile file_diffs;
   private static final long serialVersionUID = 1;
   
   CenterPanel() {
      file_diffs = null;
    }
   
   void setDifferences(BvcrDifferenceFile diffs) {
      file_diffs = diffs;
      repaint();
    }
   
   @Override public void adjustmentValueChanged(AdjustmentEvent e) {
      repaint();
    }
   
   @Override public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      Dimension sz = getSize();
      g.setColor(BoardColors.getColor("Bvcr.DiffCenterBackground"));
      g.fillRect(0, 0, sz.width, sz.height);
      BvcrDifferenceFile diffs = file_diffs;
      if (diffs == null) return;
      List<BvcrFileChange> chngs = diffs.getChanges();
      g2.setStroke(base_stroke);
      Line2D line = new Line2D.Double();
      for (BvcrFileChange chng : chngs) {
         String [] del = chng.getDeletedLines();
         String [] add = chng.getAddedLines();
         ChangeType ct = ChangeType.CHANGE;
         if (add == null) ct = ChangeType.DELETE;
         if (del == null) ct = ChangeType.INSERT;
         int sln0 = chng.getTargetLine();
         int sln1 = sln0;
         if (add != null) sln1 += add.length;
         int tln0 = chng.getSourceLine();
         int tln1 = tln0;
         if (del != null) tln1 += del.length;
         int y0 = left_display.getPosition(sln0,sln1);
         int y1 = right_display.getPosition(tln0,tln1);
         g2.setColor(getColorForChange(ct,false));
         line.setLine(0,y0,sz.width-1,y1);
         g2.draw(line);
          
         if (sln0 != sln1) {
            int yt = left_display.getPosition(sln0,sln0);
            int yb = left_display.getPosition(sln1,sln1);
            g2.fillRect(0,yt,5,yb-yt);
          }
         if (tln0 != tln1) {
            int yt = right_display.getPosition(tln0,tln0);
            int yb = right_display.getPosition(tln1,tln1);
            g2.fillRect(sz.width-1-5,yt,5,yb-yt);
          }
         
       }
    }
   
   int getRightLine(int left) {
      BvcrDifferenceFile diffs = file_diffs;
      if (diffs == null) return left;
      List<BvcrFileChange> chngs = diffs.getChanges();   
      int delta = 0;
      for (BvcrFileChange chng : chngs) {
         if (chng.getSourceLine() >= left) break;
         String [] del = chng.getDeletedLines();
         if (del != null) delta += del.length;
         String [] add = chng.getAddedLines();
         if (add != null) delta -= add.length;
       }
      return left+delta;
    }
   
   int getLeftLine(int right) {
      BvcrDifferenceFile diffs = file_diffs;
      if (diffs == null) return right;
      List<BvcrFileChange> chngs = diffs.getChanges();   
      int delta = 0;
      for (BvcrFileChange chng : chngs) {
         if (chng.getSourceLine() >= right) break;
         String [] del = chng.getDeletedLines();
         if (del != null) delta -= del.length;
         String [] add = chng.getAddedLines();
         if (add != null) delta += add.length;
       }
      return right+delta;
    }
   
}       // end of inner class CenterPanel




/********************************************************************************/
/*                                                                              */
/*      Revision bar                                                            */
/*                                                                              */
/********************************************************************************/

private class RevisionBar extends JPanel {
   
   private boolean is_left;
   private double file_length;
   private BvcrDifferenceFile file_diffs;
   private static final long serialVersionUID = 1;
   
   RevisionBar(boolean left) {
      is_left = left;
      file_diffs = null;
      file_length = 1;
      Dimension sz = new Dimension(12,10);
      setPreferredSize(sz);
      setMinimumSize(sz);
    }
   
   void setDifferences(BvcrDifferenceFile diffs) {
      file_diffs = diffs;
      repaint();
    }
   
   void setLength(int len) {
      if (len <= 0) len = 1;
      file_length = len;
    }
   
   @Override public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Dimension sz = getSize();
      JScrollBar bar = null;
      if (left_display != null && is_left) bar = left_display.getVerticalBar();
      if (right_display != null && !is_left) bar = right_display.getVerticalBar();
      int top  = 0;
      int bottom = sz.height;
      if (bar != null) { 
         top = 15;
         bottom = bar.getHeight() - 15;
      }
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(BoardColors.getColor("Bvcr.DiffRevisionBackground"));
      g2.fillRect(0,top,sz.width,bottom-top);
      BvcrDifferenceFile diffs = file_diffs;
      if (diffs == null) return;
      Rectangle2D rect = new Rectangle2D.Double();
      for (BvcrFileChange chng : diffs.getChanges()) {
         String [] add = chng.getAddedLines();
         String [] del = chng.getDeletedLines();
         ChangeType ct = ChangeType.CHANGE;
         if (add == null) ct = ChangeType.DELETE;
         if (del == null) ct = ChangeType.INSERT;
         double sln0,sln1;
         if (is_left) {
            sln0 = chng.getTargetLine();
            sln1 = sln0;
            if (add != null) sln1 += add.length;
          }
         else {
            sln0 = chng.getSourceLine();
            sln1 = sln0;
            if (del != null) sln1 += del.length;
          }
         g2.setColor(getColorForChange(ct,false));
         double y0 = (sln0 / file_length) * (bottom-top) + top;
         double y1 = ((sln1+1) / file_length) * (bottom-top) + top;
         rect.setFrame(0,y0,sz.width,y1-y0);
         g2.fill(rect);
       }
    }
   
}       // end of inner class RevisionBar





/********************************************************************************/
/*                                                                              */
/*      Scroll synchronization                                                  */
/*                                                                              */
/********************************************************************************/

private void synchronizeScrolling() 
{
   JScrollBar lbar = left_display.getHorizontalBar();
   JScrollBar rbar = right_display.getHorizontalBar();
   HScrollSync hss = new HScrollSync();
   lbar.addAdjustmentListener(hss);
   rbar.addAdjustmentListener(hss);

   lbar = left_display.getVerticalBar();
   rbar = right_display.getVerticalBar();
   VScrollSync vss = new VScrollSync();
   lbar.addAdjustmentListener(vss);
   rbar.addAdjustmentListener(vss);
   lbar.addAdjustmentListener(center_panel);
   rbar.addAdjustmentListener(center_panel);
}



private class HScrollSync implements AdjustmentListener {
   
   private boolean inside_scroll;
   
   HScrollSync() {
      inside_scroll = false;
    }
   
   @Override public void adjustmentValueChanged(AdjustmentEvent e) {
      JScrollBar sbfrom;
      JScrollBar sbto;
      
      if (inside_scroll) return;
      if (left_display.getHorizontalBar() == e.getSource()) {
         sbfrom = left_display.getHorizontalBar();
         sbto = right_display.getHorizontalBar();
       }
      else {
         sbfrom = right_display.getHorizontalBar();
         sbto = left_display.getHorizontalBar();
       }
      inside_scroll = true;
      sbto.setValue(sbfrom.getValue());
      inside_scroll = false;
    }
   
}       // end of inner class HScrollSync



private class VScrollSync implements AdjustmentListener {
   
   private boolean inside_scroll;
  
   VScrollSync() {
      inside_scroll = false;
    }
   
   @Override public void adjustmentValueChanged(AdjustmentEvent e) {
      if (inside_scroll) return;
      boolean left = true;
      if (left_display.getVerticalBar() != e.getSource()) {
         left = false;
       }
      inside_scroll = true;
      handleScroll(left);
      inside_scroll = false;
    }
   
   private void handleScroll(boolean left) {
      DiffFileView fv1 = (left ? left_display : right_display);
      DiffFileView fv2 = (left ? right_display : left_display);
      int line = fv1.getScrollLine();
      int rline = line;
      if (left) rline = center_panel.getRightLine(line);
      else rline = center_panel.getLeftLine(line);
      fv2.scrollToLine(rline);
      System.err.println("SCROLL " + left + " " + line + " " + rline);
    }
   
}       // end of inner class VScrollSync


private class LeftScrollPaneLayout extends ScrollPaneLayout {
   
   private static final long serialVersionUID = 1;

   @Override public void layoutContainer(Container parent) {
      ComponentOrientation or;
      or = parent.getComponentOrientation();
      parent.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
      super.layoutContainer(parent);
      parent.setComponentOrientation(or);
    }
   
}       // end of inner class LeftScrollPaneLayout




}       // end of class BvcrDiffViewer




/* end of BvcrDiffViewer.java */

