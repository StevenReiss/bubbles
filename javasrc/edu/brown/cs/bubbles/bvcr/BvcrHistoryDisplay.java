/********************************************************************************/
/*										*/
/*		BvcrHistoryDisplay.java 					*/
/*										*/
/*	Display history for a file						*/
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

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.petal.PetalArcDefault;
import edu.brown.cs.ivy.petal.PetalEditor;
import edu.brown.cs.ivy.petal.PetalLayoutMethod;
import edu.brown.cs.ivy.petal.PetalLevelLayout;
import edu.brown.cs.ivy.petal.PetalModelDefault;
import edu.brown.cs.ivy.petal.PetalNode;
import edu.brown.cs.ivy.petal.PetalNodeDefault;
import edu.brown.cs.ivy.petal.PetalUndoSupport;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;



class BvcrHistoryDisplay implements BvcrConstants, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubble		base_bubble;
private Point			base_point;
private BudaBubbleArea		bubble_area;
private String			for_project;
private File			for_file;

private BvcrFactory		for_factory;
private Map<String,BvcrFileVersion> version_set;
private List<BvcrFileVersion>	root_versions;
private Map<HistoryNode,Integer> node_order;
private Map<HistoryNode,Color>	 node_color;
private ColorMode		color_by;
private HistoryBubble		history_bubble;
private Component		author_display;
private List<Region>		file_regions;

private static final int	MAX_WIDTH = 90;
private static final int	MAX_SPACES = 3;
private static int tab_size =	 BoardProperties.getProperties("Bvcr").getInt("Bvcr.tabsize",8);


private static final int	LINE_HEIGHT = 1;
private static final int	CHAR_WIDTH = 1;

enum ColorMode {
   AUTHOR,
   TIME
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrHistoryDisplay(BaleContextConfig cfg)
{
   this();

   base_bubble = cfg.getEditor();
   base_point = null;
   bubble_area = BudaRoot.findBudaBubbleArea(base_bubble);
   for_project = base_bubble.getContentProject();
   for_file = base_bubble.getContentFile();
}


BvcrHistoryDisplay(BudaBubble bb,Point where,BumpLocation loc)
{
   this();

   base_bubble = bb;
   base_point = where;
   bubble_area = BudaRoot.findBudaBubbleArea(bb);
   for_project = loc.getProject();
   for_file = loc.getFile();
}


private BvcrHistoryDisplay()
{
   for_factory = BvcrFactory.getFactory();
   version_set = null;
   root_versions = null;
   color_by = ColorMode.AUTHOR;
   author_display = null;
   file_regions = null;
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void process()
{
   if (for_file == null) return;

   HistoryGather hg = new HistoryGather();
   hg.addCallback(new ColorComputer());

   history_bubble = new HistoryBubble(hg);
   bubble_area.addBubble(history_bubble,base_bubble,base_point,BudaConstants.PLACEMENT_LOGICAL);

   BoardThreadPool.start(hg);
}




private BvcrFileVersion loadVersionData()
{
   String file = for_file.getAbsolutePath();
   version_set = new HashMap<>();
   root_versions = new ArrayList<>();
   BvcrFileVersion cur = null;

   Element e1 = for_factory.getHistoryForFile(for_project,file);
   e1 = IvyXml.getChild(e1,"HISTORY");
   if (e1 == null) return null;

   for (Element ve : IvyXml.children(e1,"VERSION")) {
      BvcrFileVersion bfv = new BvcrFileVersion(ve,version_set);
      version_set.put(bfv.getVersionId(),bfv);
      if (bfv.isHead()) cur = bfv;
    }
   for (BvcrFileVersion bfv : version_set.values()) {
      if (bfv.getPriorVersions(version_set).size() == 0 && !root_versions.contains(bfv))
	 root_versions.add(bfv);
    }

   return cur;
}




/********************************************************************************/
/*										*/
/*	Region computations							*/
/*										*/
/********************************************************************************/

private void setupRegions()
{
   file_regions = new ArrayList<Region>();

   List<BumpLocation> decls = BumpClient.getBump().findAllDeclarations(for_project,for_file,null,false);
   Segment s = new Segment();
   BaleFileOverview bov = BaleFactory.getFactory().getFileOverview(for_project,for_file);
   try {
      bov.getText(0,bov.getLength(),s);
    }
   catch (BadLocationException e) {
    }

   if (decls != null) {
      for (BumpLocation bl : decls) {
	 switch (bl.getSymbolType()) {
	    case CLASS :
	    case INTERFACE :
	    case ENUM :
	    case THROWABLE :
	       break;
	    case FIELD :
	    case ENUM_CONSTANT :
	    case GLOBAL :
	    case IMPORT :
	    case EXPORT :
	       continue;
	    case FUNCTION :
	    case CONSTRUCTOR :
	    case STATIC_INITIALIZER :
	    case MAIN_PROGRAM :
	    case PROGRAM :
	       break;
	    default :
	       continue;
	  }
	 int pos0 = bov.mapOffsetToJava(bl.getOffset());
	 int pos1 = bov.mapOffsetToJava(bl.getEndOffset());
	 try {
	    bov.getText(0,bov.getLength(),s);
	    int idx = pos0;
	    while (idx >= 0 && idx < s.length() && Character.isWhitespace(s.charAt(idx))) {
	       if (s.charAt(idx) == '\n') {
		  pos0 = idx;
		  break;
		}
	       --idx;
	     }
	    idx = pos1;
	    while (idx < s.length()) {
	       if (s.charAt(idx) == '\n') {
		  pos1 = idx;
		  break;
		}
	       ++idx;
	     }
	    while (idx < s.length() && Character.isWhitespace(s.charAt(idx))) {
	       if (s.charAt(idx) == '\n') pos1 = idx;
	       ++idx;
	     }
	  }
	 catch (BadLocationException e) {
	  }

	 int ln0 = bov.findLineNumber(pos0);
	 int ln1 = bov.findLineNumber(pos1);
	 Region rgn = new Region(bl,ln0,ln1);
	 file_regions.add(rgn);
       }
    }

   Collections.sort(file_regions);
}



private static class Region implements Comparable<Region> {

   private int start_line;
   private int end_line;
   private String region_name;
   private BumpLocation base_location;

   Region(BumpLocation loc,int sln,int eln) {
      base_location = loc;
      start_line = sln;
      end_line = eln;
      region_name = loc.getSymbolName();
    }

   int getStartLine()			{ return start_line; }
   int getEndLine()			{ return end_line; }
   String getName()			{ return region_name; }

   @Override public int compareTo(Region r) {
      if (start_line < r.start_line) return -1;
      if (start_line > r.start_line) return 1;
      if (end_line < r.end_line) return -1;
      if (end_line > r.end_line) return 1;
      return region_name.compareTo(r.region_name);
    }

   BudaBubble makeBubble() {
      if (base_location == null) return null;
      BudaBubble bb = null;
      BaleFactory bf = BaleFactory.getFactory();
      switch (base_location.getSymbolType()) {
	 case UNKNOWN :
	 case LOCAL :
	 case PACKAGE :
	 case MODULE :
	    break;
	 case CLASS :
	 case ENUM :
	 case INTERFACE :
	 case THROWABLE :
	    bb = bf.createClassBubble(base_location.getProject(),
					 base_location.getSymbolName());
	    break;
	 case FUNCTION :
	 case CONSTRUCTOR :
	    String prms = base_location.getParameters();
	    String mnm = base_location.getSymbolName();
	    if (prms != null) mnm += prms;
	    else mnm += "(...)";
	    bb = bf.createMethodBubble(base_location.getProject(),mnm);
	    break;
	 case STATIC_INITIALIZER :
	 case EXPORT :
	 case IMPORT :
	    String inm = base_location.getSymbolName();
	    bb = bf.createStaticsBubble(base_location.getProject(),inm,base_location.getFile());
	    break;
	 case MAIN_PROGRAM :
	 case PROGRAM :
	    inm = base_location.getSymbolName();
	    bb = bf.createMainProgramBubble(base_location.getProject(),inm,base_location.getFile());
	    break;
	 case FIELD :
	 case GLOBAL :
	 case ENUM_CONSTANT :
	    String fnm = base_location.getSymbolName();
	    int idx = fnm.lastIndexOf(".");
	    if (idx > 0) {
	       String cnm = fnm.substring(0,idx);
	       bb = bf.createFieldsBubble(base_location.getProject(),base_location.getFile(),cnm);
	     }
	    break;
	 default:
	    break;
       }

      return bb;
    }

}	// end of inner class Region







/********************************************************************************/
/*										*/
/*	Methods to load version differences					*/
/*										*/
/********************************************************************************/

private HistoryMap loadVersionDifferences(BvcrFileVersion cur)
{
   if (for_file == null) return null;
   String file = for_file.getAbsolutePath();

   HistoryMap diffmap = new HistoryMap();
   Set<BvcrFileVersion> done = new HashSet<BvcrFileVersion>();

   for (BvcrFileVersion bfv : version_set.values()) {
      if (done.contains(bfv)) continue;
      done.add(bfv);
      for (BvcrFileVersion bpv : bfv.getPriorVersions(version_set)) {
	 String from = bpv.getVersionId();
	 String to = bfv.getVersionId();
	 Element e2 = for_factory.getFileDifferences(for_project,file,from,to);
	 Element de = IvyXml.getChild(e2,"DIFFERENCES");
	 Element fe = IvyXml.getChild(de,"FILE");
	 if (fe == null) continue;
	 BvcrDifferenceFile bdf = new BvcrDifferenceFile(fe);
	 diffmap.put(bfv,bpv,bdf);
       }
    }

   Element e1 = for_factory.getFileDifferences(for_project,file,null,null);    // current changes
   Element de1 = IvyXml.getChild(e1,"DIFFERENCES");
   Element fe1 = IvyXml.getChild(de1,"FILE");
   if (fe1 != null) {
      BvcrDifferenceFile bdf = new BvcrDifferenceFile(fe1);
      BvcrFileVersion cfv = new BvcrFileVersion(for_file,null,new Date(),System.getProperty("user.name"),
	    "Current Version");
      if (cur != null) {
	 cfv.addPriorVersion(cur);
	 diffmap.put(cfv,cur,bdf);
       }
      cur = cfv;
    }

   diffmap.setCurrentVersion(cur);

   return diffmap;
}




/********************************************************************************/
/*										*/
/*	Construct the history graph for a file					*/
/*										*/
/********************************************************************************/

private HistoryNode buildHistory(HistoryMap map)
{
   if (map == null) return null;
   BvcrFileVersion cur = map.getCurrentVersion();
   if (cur == null) return null;

   Map<BvcrFileVersion,HistoryNode> done = new HashMap<BvcrFileVersion,HistoryNode>();
   HistoryNode root = new HistoryNode(cur);
   done.put(cur,root);

   addToHistory(root,cur,map,done);

   return root;
}



private void addToHistory(HistoryNode hn,BvcrFileVersion cv,HistoryMap map,Map<BvcrFileVersion,HistoryNode> done)
{
   for (BvcrFileVersion pv : cv.getPriorVersions(version_set)) {
      BvcrDifferenceFile dif = map.get(cv,pv);
      if (dif == null) addToHistory(hn,pv,map,done);
      else {
	 HistoryNode phn = done.get(pv);
	 BoardLog.logD("BVCR","History link: " + hn.getVersionId() + " " +
		  pv.getVersionId() + " " + dif + " " + (phn != null));
	 if (phn != null) {
	    hn.addPrior(phn,dif);
	  }
	 else {
	    phn = new HistoryNode(pv);
	    done.put(pv,phn);
	    hn.addPrior(phn,dif);
	    addToHistory(phn,pv,map,done);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Get line by line history						*/
/*										*/
/********************************************************************************/

private List<LineData> getLineHistory(HistoryNode root)
{
   List<LineData> rslt = new ArrayList<LineData>();

   if (root == null) return rslt;

   BaleFileOverview tfo = BaleFactory.getFactory().getFileOverview(for_project,for_file);

   int len = tfo.getLength();
   Map<Integer,LineData> linemap = new HashMap<Integer,LineData>();
   for (int i = 1; ; ++i) {
      int off = tfo.findLineOffset(i);
      if (off < 0 || off >= len) break;
      LineData ld = new LineData(i);
      linemap.put(i,ld);
      rslt.add(ld);
   }

   HistoryNode base = null;
   for (HistoryNode pr : root.getPriorNodes()) {
      HistoryNode b = computeLineDiffs(root,pr,linemap,linemap.size());
      if (b != null && base == null) base = b;
   }
   if (base == null) base = root;

   for (LineData ld : rslt) {
      if (ld.getCreationNode() == null)
	 ld.setCreation(base);
   }

   return rslt;
}


private HistoryNode computeLineDiffs(HistoryNode cur,HistoryNode prior,
      Map<Integer,LineData> lines,int maxlines)
{
   BvcrDifferenceFile dif = cur.getDifferences(prior);
   Map<Integer,LineData> newlines = new HashMap<Integer,LineData>();

   int delta = 0;
   int line = 1;

   for (BvcrFileChange bfc : dif.getChanges()) {
      int tln = bfc.getTargetLine();
      while (line < tln) {
	 LineData ld = lines.get(line);
	 if (ld != null) newlines.put(line+delta,ld);
	 ++line;
       }

      String [] dels = bfc.getDeletedLines();
      int dct = (dels == null ? 0 : dels.length);
      String [] adds = bfc.getAddedLines();
      int act = (adds == null ? 0 : adds.length);
      for (int i = 0; i < act; ++i) {
	 LineData ld = lines.get(line);
	 if (ld != null) {
	    if (i < dct) {
	       ld.addEdit(cur,line);
	       newlines.put(line + delta,ld);
	     }
	    else {
	       ld.setCreation(cur);
	     }
	  }
	 ++line;
       }
      delta += dct-act;
    }

   while (line < maxlines) {
      LineData ld = lines.get(line);
      if (ld != null) newlines.put(line+delta,ld);
      ++line;
    }
   maxlines += delta;

   if (prior.getPriorNodes() == null || prior.getPriorNodes().size() == 0) {
      for (Map.Entry<Integer,LineData> ent : newlines.entrySet()) {
	 LineData ld = ent.getValue();
	 if (ld != null) {
	    ld.setCreation(prior);
	    ld.addEdit(prior,ent.getKey());
	  }
       }
      return prior;
    }

   HistoryNode base = null;
   for (HistoryNode p : prior.getPriorNodes()) {
      HistoryNode b = computeLineDiffs(prior,p,newlines,maxlines);
      if (b != null && base == null) base = b;
    }

   return base;
}


/********************************************************************************/
/*										*/
/*	Deferred processing tasks						*/
/*										*/
/********************************************************************************/

private class HistoryGather implements Runnable {

   private List<HistoryCallback> history_callbacks;

   HistoryGather() {
      history_callbacks = new ArrayList<HistoryCallback>();
    }

   void addCallback(HistoryCallback cb) {
      history_callbacks.add(cb);
    }

   @Override public void run() {
      BvcrFileVersion cur = loadVersionData();
      setupRegions();
      if (version_set == null || version_set.isEmpty()) return;

      HistoryMap hmap = loadVersionDifferences(cur);
      HistoryNode root = buildHistory(hmap);
      List<LineData> lines = getLineHistory(root);

      for (HistoryCallback cb : history_callbacks) {
	 cb.handleFileHistory(root,lines);
       }
    }

}	// end of inner class HistoryGather



private interface HistoryCallback {

   void handleFileHistory(HistoryNode root,List<LineData> lines);

}	// end of inner interface HistoryCallback



private class HistoryMap extends HashMap<BvcrFileVersion,Map<BvcrFileVersion,BvcrDifferenceFile>>
{
   private BvcrFileVersion current_version;
   private static final long serialVersionUID = 1;
   
   HistoryMap() {
      current_version = null;
    }

   void setCurrentVersion(BvcrFileVersion bfv)		{ current_version = bfv; }
   BvcrFileVersion getCurrentVersion()			{ return current_version; }

   void put(BvcrFileVersion f1,BvcrFileVersion f2,BvcrDifferenceFile dif) {
      Map<BvcrFileVersion,BvcrDifferenceFile> m1 = get(f1);
      if (m1 == null) {
	 m1 = new HashMap<BvcrFileVersion,BvcrDifferenceFile>();
	 put(f1,m1);
       }
      m1.put(f2,dif);
    }

   BvcrDifferenceFile get(BvcrFileVersion f1,BvcrFileVersion f2) {
      Map<BvcrFileVersion,BvcrDifferenceFile> m1 = get(f1);
      if (m1 == null) return null;
      return m1.get(f2);
    }

}	// end of inner class HistoryMap




/********************************************************************************/
/*										*/
/*	History Node container for this files history				*/
/*										*/
/********************************************************************************/

private class HistoryNode {

   private BvcrFileVersion file_version;
   private Map<HistoryNode,BvcrDifferenceFile> prior_versions;

   HistoryNode(BvcrFileVersion bfv) {
      file_version = bfv;
      prior_versions = new HashMap<HistoryNode,BvcrDifferenceFile>();
    }

   BvcrFileVersion getVersion() 	{ return file_version; }
   String getVersionId()		{ return file_version.getVersionId(); }

   String getVersionName() {
      String nm = getVersionId();
      if (nm == null) nm = "*CURRENT*";
      return nm;
   }

   void addPrior(HistoryNode pn,BvcrDifferenceFile df) {
      prior_versions.put(pn,df);
    }

   Collection<HistoryNode> getPriorNodes()	{ return prior_versions.keySet(); }
   BvcrDifferenceFile getDifferences(HistoryNode p) {
      return prior_versions.get(p);
    }

   String getLabel() {
      String nm = getVersionName();
      for (String s1 : file_version.getAlternativeIds()) {
	 if (s1 != null && s1.length() > 0 && s1.length() < nm.length()) {
	    nm = s1;
	  }
       }
      return nm;
    }
}	// end of inner class HistoryNode



/********************************************************************************/
/*										*/
/*	Data for each line							*/
/*										*/
/********************************************************************************/

private class LineData {

   private int orig_number;
   private HistoryNode create_node;
   private Map<HistoryNode,Integer> edit_nodes;
   private List<HistoryNode> ordered_nodes;

   LineData(int ln) {
      orig_number = ln;
      create_node = null;
      edit_nodes = null;
      ordered_nodes = null;
    }

   List<HistoryNode> getEditNodes() {
      if (ordered_nodes == null) {
	 ordered_nodes = new ArrayList<HistoryNode>();
	 if (edit_nodes != null) {
	    ordered_nodes.addAll(edit_nodes.keySet());
	    if (create_node != null && !edit_nodes.containsKey(create_node)) ordered_nodes.add(create_node);
	 }
	 else if (create_node != null) ordered_nodes.add(create_node);
	 Collections.sort(ordered_nodes,new NodeSorter());
       }
      return ordered_nodes;
    }


   void setCreation(HistoryNode hn)		{ create_node = hn; }
   void addEdit(HistoryNode hn,int line) {
      if (edit_nodes == null) edit_nodes = new LinkedHashMap<HistoryNode,Integer>();
      edit_nodes.put(hn,line);
    }

   HistoryNode getCreationNode()		{ return create_node; }
   int getOriginalLineNumber()			{ return orig_number; }

   int getLineAtNode(HistoryNode hn) {
      if (edit_nodes == null) return 0;
      Integer v = edit_nodes.get(hn);
      if (v == null) return 0;
      return v;
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("LINE ");
      buf.append(orig_number);
      buf.append(": ");
      if (create_node == null) {
	 buf.append("*");
       }
      else {
	 buf.append(create_node.getVersionName());
       }
      if (edit_nodes != null) {
	 buf.append(" @");
	 for (HistoryNode hn : edit_nodes.keySet()) {
	    if (hn == create_node) continue;
	    buf.append(" ");
	    buf.append(hn.getVersionName());
	  }
       }
      return buf.toString();
   }

}	// end of inner class LineData


private class NodeSorter implements Comparator<HistoryNode> {

   @Override public int compare(HistoryNode n1,HistoryNode n2) {
      Integer i1 = node_order.get(n1);
      int v1 = (i1 == null ? -1 : i1);
      Integer i2 = node_order.get(n2);
      int v2 = (i2 == null ? -1 : i2);
      return v1 - v2;
    }

}	// end of inner class NodeSorder





/********************************************************************************/
/*										*/
/*	Display Bubble								*/
/*										*/
/********************************************************************************/

private class HistoryBubble extends BudaBubble {

   private ColorMode last_mode;
   private static final long serialVersionUID = 1;
   
   HistoryBubble(HistoryGather hg) {
      SwingGridPanel pnl = new SwingGridPanel();
      JLabel lbl = new JLabel("Version History for " + for_file);
      pnl.addGBComponent(lbl,0,0,0,1,10,0);
      FileDisplay fd = new FileDisplay(hg);
      pnl.addGBComponent(fd,0,1,1,1,10,10);
      AuthorWindow aw = new AuthorWindow(hg);
      author_display = new JScrollPane(aw);
      pnl.addGBComponent(author_display,0,2,1,1,10,0);
      setContentPane(pnl);
      last_mode = color_by;
    }

   @Override public void paint(Graphics g) {
      if (last_mode != color_by) {
	 last_mode = color_by;
	 recomputeColors();
       }
      super.paint(g);
    }

   @Override public void handlePopupMenu(MouseEvent e) {
      JPopupMenu menu = new JPopupMenu();

      menu.add(new ColorModeAction(ColorMode.AUTHOR));
      menu.add(new ColorModeAction(ColorMode.TIME));
      menu.add(new ShowAuthorAction());
      menu.add(getFloatBubbleAction());

      menu.show(this,e.getX(),e.getY());
    }

}	// end of inner class HistoryBubble



private class ColorModeAction extends JRadioButtonMenuItem implements ActionListener {

   private ColorMode color_mode;
   private static final long serialVersionUID = 1;
   
   ColorModeAction(ColorMode md) {
      super("Color By " + md.toString(),(md == color_by));
      addActionListener(this);
      color_mode = md;
    }

   @Override public void actionPerformed(ActionEvent e) {
      color_by = color_mode;
      history_bubble.repaint();
    }

}	// end of inner class ColorModeAction


private class ShowAuthorAction extends JRadioButtonMenuItem implements ActionListener {
   
   private static final long serialVersionUID = 1;
   
   ShowAuthorAction() {
      super("Show Authors",(author_display != null && author_display.isVisible()));
      addActionListener(this);
   }

   @Override public void actionPerformed(ActionEvent e) {
      if (author_display == null) return;
      boolean fg = author_display.isVisible();
      author_display.setVisible(!fg);
   }

}	// end of inner class ShowAuthorAction



/********************************************************************************/
/*										*/
/*	Display Window								*/
/*										*/
/********************************************************************************/

private class FileDisplay extends JSplitPane {

   private static final long serialVersionUID = 1;

   FileDisplay(HistoryGather hg) {
      super(VERTICAL_SPLIT,true,new HistoryGraph(hg),new LineInfo(hg));
    }

}	// end of inner class FileDisplay



/********************************************************************************/
/*										*/
/*	History Graph Display							*/
/*										*/
/********************************************************************************/

private class HistoryGraph extends JPanel implements HistoryCallback {

   private PetalEditor petal_editor;
   private PetalModelDefault petal_model;
   private PetalLayoutMethod layout_method;
   private static final long serialVersionUID = 1;
   
   HistoryGraph(HistoryGather hg) {
      super(new BorderLayout());

      // setMinimumSize(new Dimension(400,150));
      setPreferredSize(new Dimension(400,150));

      PetalUndoSupport.getSupport().blockCommands();
      petal_model = new Model();
      petal_editor = new PetalEditor(petal_model);
      layout_method = new PetalLevelLayout(petal_editor);

      JScrollPane jsp = new JScrollPane(petal_editor);
      petal_editor.addZoomWheeler();
      add(jsp,BorderLayout.CENTER);

      hg.addCallback(this);
    }

   @Override public void handleFileHistory(HistoryNode root,List<LineData> lines) {
      Map<HistoryNode,Node> nodes = new HashMap<HistoryNode,Node>();
      addNode(root,nodes);
      petal_model.fireModelUpdated();
      petal_editor.commandLayout(layout_method);
      repaint();
    }

   private Node addNode(HistoryNode hn,Map<HistoryNode,Node> done) {
      Node n1 = done.get(hn);
      if (hn == null || n1 != null) return n1;
      n1 = new Node(hn);
      petal_model.addNode(n1);
      done.put(hn,n1);
      for (HistoryNode hn2 : hn.getPriorNodes()) {
	 Node n2 = addNode(hn2,done);
	 Arc a1 = new Arc(n1,n2);
	 petal_model.addArc(a1);
       }
      return n1;
    }

   @Override public void paint(Graphics g) {
      for (PetalNode pn : petal_model.getNodes()) {
	 if (pn instanceof Node) {
	    Node nn = (Node) pn;
	    HistoryNode hn = nn.getNode();
	    Color c = node_color.get(hn);
	    if (c == null) c = BoardColors.getColor("Bvcr.HistoryDefaultColor");
	    Component lbl = pn.getComponent();
	    BoardColors.setColors(lbl,c);
	  }
       }

      super.paint(g);
    }

   private class Model extends PetalModelDefault {
    }	// end of inner class Model

   private class Node extends PetalNodeDefault {

      private HistoryNode for_node;
      private static final long serialVersionUID = 1;
      
      Node(HistoryNode hn) {
	 super(hn.getLabel());
	 Color c = node_color.get(hn);
	 if (c != null) getComponent().setBackground(c);
	 for_node = hn;
       }

      HistoryNode getNode()		{ return for_node; }

      @Override public String getToolTip(Point at) {
	 StringBuffer buf = new StringBuffer();
	 BvcrFileVersion fv = for_node.getVersion();
	 buf.append("<html><table>");
	 buf.append("<tr><td>File</td><td>");
	 buf.append(for_file);
	 buf.append("<tr><td>Id</td><td>");
	 buf.append(fv.getVersionId());
	 buf.append("</td></tr>");
	 buf.append("<tr><td>Date</td><td>");
	 buf.append(fv.getVersionTime());
	 buf.append("</td></tr>");
	 buf.append("<tr><td>Author</td><td>");
	 buf.append(fv.getAuthor());
	 buf.append("</td></tr>");
	 buf.append("<tr><td>Message</td><td><p>");
	 buf.append(fv.getFullMessage());
	 buf.append("</p></td></tr>");
	 buf.append("</table>");
	 return buf.toString();
      }

    }	// end of inner class Node

   private class Arc extends PetalArcDefault {
      
      private static final long serialVersionUID = 1;
      
      Arc(Node f,Node t) {
         super(f,t);
       }
   
   }	// end of inner class Arc

}	// end of inner class HistoryGraph



/********************************************************************************/
/*										*/
/*	Line display area							*/
/*										*/
/********************************************************************************/

private class LineInfo extends JPanel implements HistoryCallback {

   private LineDrawingArea line_area;
   private static final long serialVersionUID = 1;
   
   LineInfo(HistoryGather hg) {
      super(new BorderLayout());

      FileData fd = new FileData(for_file);
      node_order = null;

      line_area = new LineDrawingArea(fd);
      setPreferredSize(new Dimension(400,400));

      JScrollPane jsp = new JScrollPane(line_area);
      add(jsp,BorderLayout.CENTER);

      hg.addCallback(this);
    }

   @Override public void handleFileHistory(HistoryNode root,List<LineData> lines) {
      line_area.setLineData(lines);
      repaint();
    }

}	// end of inner class LineInfo



private static class FileData {

   private File file_name;
   private Map<Integer,List<FileLineData>> line_data;
   private Map<Integer,String> line_text;
   private int num_lines;

   FileData(File f) {
      file_name = f;
      num_lines = 1;
      line_data = new HashMap<Integer,List<FileLineData>>();
      line_text = new HashMap<Integer,String>();
      scanFile();
    }

   int getNumLines()				{ return num_lines; }
   List<FileLineData> getLineData(int ln)	{ return line_data.get(ln); }
   String getLineText(int ln)			{ return line_text.get(ln); }

   private void scanFile() {
      num_lines = 0;
      try (BufferedReader rdr = new BufferedReader(new FileReader(file_name))) {
	 for ( ; ; ) {
	    String ln = rdr.readLine();
	    if (ln == null) break;
	    ++num_lines;
	    List<FileLineData> lfd = scanLine(ln);
	    if (lfd != null) {
	       line_data.put(num_lines,lfd);
	       line_text.put(num_lines,ln);
	     }
	  }
       }
      catch (IOException e) {
	 return;
       }
    }

   private List<FileLineData> scanLine(String line) {
      if (line == null || line.length() == 0) return null;
      int ln = line.length();
      int pos = 1;
      List<FileLineData> rslt = new ArrayList<FileLineData>();
      boolean inspace = true;
      int lastspace = -1;
      int laststart = -1;
      for (int i = 0; i < ln; ++i) {
	 if (i >= MAX_WIDTH) break;
	 char c = line.charAt(i);
	 int npos = pos+1;
	 if (c == '\t') {
	    npos = ((pos+tab_size-1)/tab_size)*tab_size + 1;
	  }
	 if (Character.isWhitespace(c)) {
	    if (!inspace) {
	       if (lastspace > 0) {
		  if (pos - lastspace >= MAX_SPACES) {
		     rslt.add(new FileLineData(laststart,lastspace));
		     inspace = true;
		     laststart = -1;
		   }
		}
	       else lastspace = pos;
	     }
	  }
	 else if (inspace) {
	    laststart = pos;
	    inspace = false;
	  }
	 pos = npos;
       }
      if (laststart > 0) {
	 if (lastspace < 0) lastspace = pos;
	 rslt.add(new FileLineData(laststart,lastspace));
       }

      return rslt;
    }

}	// end of inner class FileData


private static class FileLineData {

   private int start_pos;
   private int end_pos;

   FileLineData(int s,int e) {
      start_pos = s;
      end_pos = e;
    }

   int getStartPos()		{ return start_pos; }
   int getEndPos()		{ return end_pos; }

}	// end of inner class FileLineData



/********************************************************************************/
/*										*/
/*	Line drawing area							*/
/*										*/
/********************************************************************************/


private class LineDrawingArea extends JPanel {

   private FileData	file_data;
   private List<LineData> line_data;
   private Rectangle2D	draw_rect;
   private static final long serialVersionUID = 1;
   
   LineDrawingArea(FileData fd) {
      file_data = fd;
      line_data = null;
      setPreferredSize(new Dimension(350,fd.getNumLines()*LINE_HEIGHT+1));
      draw_rect = new Rectangle2D.Double();
      setToolTipText("History of Lines in the File");
      addMouseListener(new Mouser(this));
    }

   void setLineData(List<LineData> lld) {
      line_data = lld;
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      Dimension sz = getSize();
      g.setColor(BoardColors.getColor("Bvcr.HistoryLineBackground"));
      g.fillRect(0,0,sz.width,sz.height);
      double ht = LINE_HEIGHT;
      double nln = file_data.getNumLines();
      if (nln > 0) ht = sz.height / nln;
      double x0 = (MAX_WIDTH + 1) * CHAR_WIDTH + 1;
      double narea = (node_order == null ? 0 : node_order.size());
      double xw = 0;
      if (narea > 0) xw = (sz.width - x0)/narea;

      if (file_regions != null) {
	 for (Region r : file_regions) {
	    int sln = r.getStartLine();
	    int eln = r.getEndLine();
	    if (eln <= sln) continue;
	    g2.setColor(BoardColors.getColor("Bvcr.HistoryLineFrame"));
	    draw_rect.setFrame(0,sln*ht,MAX_WIDTH,ht);
	    g2.fill(draw_rect);
	    draw_rect.setFrame(0,eln*ht,MAX_WIDTH,ht);
	    g2.fill(draw_rect);
	 }
      }

      for (int i = 0; i < nln; ++i) {
	 List<FileLineData> lfd = file_data.getLineData(i+1);
	 LineData ld = getLineData(i+1);
	 List<HistoryNode> hist = (ld == null ? null : ld.getEditNodes());
	 if (hist != null && hist.size() == 0) hist = null;
	 if (lfd != null) {
	    if (hist == null) g2.setColor(BoardColors.getColor("Bvcr.HistoryLineFrame"));
	    else {
	       Color c1 = node_color.get(hist.get(hist.size()-1));
	       if (c1 != null) g2.setColor(c1);
	       else g2.setColor(BoardColors.getColor("Bvcr.HistoryLineFrame"));
	     }
	    for (FileLineData fd : lfd) {
	       double p0 = fd.getStartPos() * CHAR_WIDTH;
	       double p1 = fd.getEndPos() * CHAR_WIDTH;
	       draw_rect.setFrame(p0,i*ht,p1-p0,ht);
	       g2.fill(draw_rect);
	     }
	  }
	 if (hist != null) {
	   HistoryNode prev = null;
	   for (HistoryNode hn : hist) {
	      int idx = node_order.get(hn);
	      if (prev != null) {
		 int idxp = node_order.get(prev);
		 colorRegion(g2,x0,xw,narea,prev,idxp+1,idx-1,false,i,ht);
	       }
	      colorRegion(g2,x0,xw,narea,hn,idx,idx,true,i,ht);
	      prev = hn;
	    }
	   if (prev != null) {
	      int idxp = node_order.get(prev);
	      colorRegion(g2,x0,xw,narea,prev,idxp+1,(int)(narea-1),false,i,ht);
	    }
	  }
       }
    }

   private void colorRegion(Graphics2D g2,double x0,double xw,double narea,
	 HistoryNode base,int i1,int i0,boolean full,int row,double ht) {
      if (i0 < i1) return;
      double xa = x0 + (narea-i1) * xw;
      double xb = x0 + (narea-1-i0) * xw;
      draw_rect.setFrame(xb,row*ht,xa-xb,ht);
      Color c = node_color.get(base);
      if (!full) {
	 float [] hsb = Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
	 c = Color.getHSBColor(hsb[0],0.25f,1.0f);
      }
      g2.setColor(c);
      g2.fill(draw_rect);
    }

   @Override public String getToolTipText(MouseEvent evt) {
      Dimension sz = getSize();
      double ht = LINE_HEIGHT;
      double nln = file_data.getNumLines();
      if (nln > 0) ht = sz.height / nln;

      double x0 = (MAX_WIDTH + 1) * CHAR_WIDTH + 1;
      double narea = (node_order == null ? 0 : node_order.size());
      double xw = 0;
      if (narea > 0) xw = (sz.width - x0)/narea;

      int lno = (int) (evt.getY()/ht) + 1;
      Region rgn = null;
      if (file_regions != null) {
	 for (Region r : file_regions) {
	    if (r.getStartLine() <= lno && r.getEndLine() > lno) {
	       rgn = r;
	       break;
	     }
	  }
       }

      if (evt.getX() < x0) {
	 int ln1 = Math.max(1,lno-5);
	 int ln2 = Math.min(lno+5,file_data.getNumLines()-1);
	 StringBuffer buf = new StringBuffer();
	 buf.append("<html><pre>");
	 for (int i = ln1; i < ln2; ++i) {
	    String ltxt = file_data.getLineText(i);
	    if (ltxt != null) {
	       if (i == lno) buf.append("-->");
	       else buf.append("   ");
	       buf.append(ltxt);
	     }
	    buf.append("\n");
	  }
	 buf.append("</pre>");
	 return buf.toString();
       }
      else {
	 int delta = (int)(narea - (int)((evt.getX() - x0)/xw) - 1);
	 HistoryNode chng = null;
	 LineData ld = getLineData(lno);
	 if (ld != null) {
	    int fnd = -1;
	    List<HistoryNode> hist = ld.getEditNodes();
	    if (hist != null && hist.size() > 0) {
	       for (HistoryNode hn : hist) {
		  int ord = node_order.get(hn);
		  if (ord <= delta && (fnd == -1 || ord > fnd)) {
		     fnd = ord;
		     chng = hn;
		   }
		}
	     }
	  }
	 if (chng != null) {
	    BvcrFileVersion fv = chng.getVersion();
	    StringBuffer buf = new StringBuffer();
	    buf.append("<html><table>");
	    buf.append("<tr><td>Line</td><td>");
	    buf.append(lno);
	    buf.append("</td></tr>");
	    buf.append("<tr><td>Date</td><td>");
	    buf.append(fv.getVersionTime());
	    buf.append("</td></tr>");
	    buf.append("<tr><td>Author</td><td>");
	    buf.append(fv.getAuthor());
	    buf.append("</td></tr>");
	    if (rgn != null) {
	       buf.append("<tr><td>Region</td><td>");
	       buf.append(rgn.getName());
	       buf.append("</td></tr>");
	     }
	    String s = getChangeDescription(ld,chng);
	    if (s != null) {
	       buf.append("<tr><td>Change</td><td><p>");
	       buf.append(s);
	       buf.append("</p></td></tr>");
	     }
	    buf.append("</table>");
	    return buf.toString();
	  }
       }

      return super.getToolTipText(evt);
    }

   private LineData getLineData(int ln) {
      if (line_data == null) return null;
      ln -= 1;
      if (ln < 0 || ln >= line_data.size()) return null;
      return line_data.get(ln);
    }

   void handleMouseEvent(MouseEvent evt) {
      Dimension sz = getSize();
      double ht = LINE_HEIGHT;
      double nln = file_data.getNumLines();
      if (nln > 0) ht = sz.height / nln;
      int lno = (int) (evt.getY()/ht) + 1;
      Region rgn = null;
      if (file_regions != null) {
	 for (Region r : file_regions) {
	    if (r.getStartLine() <= lno && r.getEndLine() > lno) {
	       rgn = r;
	       break;
	     }
	  }
       }
      if (rgn == null) return;
      BudaBubble bbl = rgn.makeBubble();
      if (bbl == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      if (bba != null) {
	 bba.addBubble(bbl,this,null,BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_NEW);
       }
    }

}	// end of inner class LineDrawingArea



private String getChangeDescription(LineData ld,HistoryNode node)
{
   int actualline = ld.getLineAtNode(node);

   if (node.getPriorNodes().size() == 0) return "Initial Check In";

   for (HistoryNode hn : node_order.keySet()) {
      BvcrDifferenceFile df = node.getDifferences(hn);
      if (df != null) {
	 for (BvcrFileChange chng : df.getChanges()) {
	    String [] del = chng.getDeletedLines();
	    String [] add = chng.getAddedLines();
	    int ndel = (del == null ? 0 : del.length);
	    int nadd = (add == null ? 0 : add.length);
	    if (actualline >= chng.getSourceLine() &&
		     actualline <= chng.getSourceLine() + nadd - ndel) {
	       System.err.println("CHECK CHANGE " + actualline + " " +
			ld.getOriginalLineNumber() + " " +
			chng.getSourceLine() + " " +
			chng.getTargetLine() + " " +
			ndel + " " + nadd);
	    }
	  }
       }
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Mouse handler for handling clicks on lines				*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter {

   private LineDrawingArea for_panel;

   Mouser(LineDrawingArea lda) {
      for_panel = lda;
    }

   @Override public void mouseClicked(MouseEvent e) {
      for_panel.handleMouseEvent(e);
    }

}	// end of inner class Mouser




/********************************************************************************/
/*										*/
/*	Author window								*/
/*										*/
/********************************************************************************/

private class AuthorWindow extends JPanel implements HistoryCallback, Scrollable {

   private static final long serialVersionUID = 1;
   
   AuthorWindow(HistoryGather hg) {
      super(new FlowLayout(FlowLayout.CENTER,10,10));
      setPreferredSize(new Dimension(300,175));
      hg.addCallback(this);
    }

   @Override public void handleFileHistory(HistoryNode root,List<LineData> lines) {
      Map<HistoryNode,Node> nodes = new HashMap<HistoryNode,Node>();
      Set<BvcrAuthor> authors = new TreeSet<BvcrAuthor>();
      addNode(root,nodes,authors);
      Dimension sz = getParent().getSize();
      sz.height = 300;
      setMaximumSize(sz);
      for (BvcrAuthor ba : authors) {
	 JLabel lbl = new JLabel(ba.getName());
	 Color c = ba.getColor();
	 lbl.setBackground(c);
	 lbl.setForeground(BoardColors.getTextColor(c));
	 lbl.setOpaque(true);
	 lbl.setBorder(new EmptyBorder(3,10,3,10));
	 add(lbl);
       }

    }

   @Override public Dimension getPreferredScrollableViewportSize() {
      return new Dimension(300,75);
   }
   @Override public int getScrollableBlockIncrement(Rectangle r,int o,int d)	{ return 10; }
   @Override public boolean getScrollableTracksViewportHeight() 		{ return false; }
   @Override public boolean getScrollableTracksViewportWidth()			{ return true; }
   @Override public int getScrollableUnitIncrement(Rectangle r,int o,int d)	{ return 1; }

   private void addNode(HistoryNode hn,Map<HistoryNode,Node> done,Set<BvcrAuthor> authors) {
      Node n1 = done.get(hn);
      if (hn == null || n1 != null) return;
      done.put(hn,n1);
      BvcrAuthor ba = BvcrAuthor.getAuthor(hn.getVersion().getAuthor());
      authors.add(ba);

      for (HistoryNode hn2 : hn.getPriorNodes()) {
	 addNode(hn2,done,authors);
       }
    }

}	// end of inner class AuthorWindow


/********************************************************************************/
/*										*/
/*	Color computations							*/
/*										*/
/********************************************************************************/

private class ColorComputer implements HistoryCallback {

   @Override public void handleFileHistory(HistoryNode root,List<LineData> lines) {
      setupColors(root);
    }

   private void setupColors(HistoryNode root) {
      SortedMap<Long,HistoryNode> sm = new TreeMap<Long,HistoryNode>();
      if (root == null) return;
      addSetup(root,sm);
      node_order = new HashMap<HistoryNode,Integer>();
      int ct = 0;
      for (HistoryNode hn : sm.values()) {
	 node_order.put(hn,ct);
	 ++ct;
       }

      recomputeColors();
    }

   private void addSetup(HistoryNode hn,SortedMap<Long,HistoryNode> sm) {
      BvcrFileVersion fv = hn.getVersion();
      Date d = fv.getVersionTime();
      long t0 = d.getTime();
      while (sm.get(t0) != null) t0 -= 1;
      sm.put(t0,hn);
      for (HistoryNode pv : hn.getPriorNodes()) {
	 addSetup(pv,sm);
       }
    }

}	// end of inner class ColorComputer



private void recomputeColors()
{
   node_color = new HashMap<HistoryNode,Color>();
   if (node_order == null) return;

   switch (color_by) {
      case AUTHOR :
	 for (HistoryNode hn : node_order.keySet()) {
	    String au = hn.getVersion().getAuthor();
	    BvcrAuthor ba = BvcrAuthor.getAuthor(au);
	    Color c = ba.getColor();
	    node_color.put(hn,c);
	  }
	 break;
      default :
      case TIME :
	 int nc = node_order.size();
	 for (HistoryNode hn : node_order.keySet()) {
	    float idx = node_order.get(hn);
	    Color c = Color.getHSBColor(0.8f*idx/nc,1f,1f);
	    node_color.put(hn,c);
	  }
	 break;
    }
}



}	// end of class BvcrHistoryDisplay




/* end of BvcrHistoryDisplay.java */
