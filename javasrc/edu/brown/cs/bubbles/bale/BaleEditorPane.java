/********************************************************************************/
/*										*/
/*		BaleEditorPane.java						*/
/*										*/
/*	Bubble Annotated Language Editor JEditorPane extension			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Hsu-Sheng Ko      */
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

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaToolTip;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.swing.SwingEditorPane;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.accessibility.Accessible;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolTip;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.w3c.dom.Element;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;




abstract class BaleEditorPane extends SwingEditorPane implements BaleConstants.BaleEditor,
		BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	      overwrite_mode;
private transient BaleCompletionContext completion_context;
private transient BaleRenameContext rename_context;
private transient BaleHighlightContext highlight_context;
private boolean 	      fixed_size;
private transient BaleVisualizationKit	visual_kit = BaleVisualizationKit.getVisualizationKit();

private transient Map<BaleHighlightType,HighlightData> hilite_map;

private transient Collection<ActiveRegion> active_regions;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors and finalizers						*/
/*										*/
/********************************************************************************/

protected BaleEditorPane()
{
   BaleDocument bd = getBaleDocument();

   bd.baleWriteLock();
   try {
      setUI(new BaleTextUI(getUI()));
      setCaret(new BaleCaret());
      if (!bd.isEditable()) setEditable(false);
    }
   finally { bd.baleWriteUnlock(); }

   BoardColors.setColors(this,BALE_EDITOR_TOP_COLOR_PROP);
   setOpaque(false);

   setDragEnabled(true);
   setDropMode(DropMode.INSERT);

   BaleEditorKit bek = (BaleEditorKit) getEditorKit();
   bek.setupKeyMap(this);
   BurpHistory.getHistory().addEditor(this);

   overwrite_mode = false;
   completion_context = null;
   rename_context = null;
   fixed_size = false;

   hilite_map = new EnumMap<>(BaleHighlightType.class);

   highlight_context = bd.getHighlightContext();
   highlight_context.addEditor(this);

   active_regions = new ArrayList<>();

   BoardColors.setColors(this,"Bale.EditorPaneBackground");

   addMouseMotionListener(new ActiveMouser());

   setToolTipText("");
}



void dispose()
{
   BurpHistory.getHistory().removeEditor(this);
   if (highlight_context != null) highlight_context.removeEditor(this);
   getBaleDocument().dispose();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public BaleDocument getBaleDocument() 		{ return (BaleDocument) getDocument(); }

@Override public Color getCaretColor()				{ return BoardColors.getColor(BALE_CARET_COLOR_PROP); }
@Override public boolean getScrollableTracksViewportWidth()	{ return true; }
@Override public boolean getScrollableTracksViewportHeight()	{ return false; }

@Override public void setOverwriteMode(boolean fg)
{
   if (overwrite_mode == fg) return;
   overwrite_mode = fg;
   BaleCaret bc = (BaleCaret) getCaret();
   bc.setCaretStyle(overwrite_mode ? BaleCaretStyle.BLOCK_CARET : BaleCaretStyle.LINE_CARET);
}



@Override public boolean getOverwriteMode()			{ return overwrite_mode; }

@Override public void setCompletionContext(BaleCompletionContext c)	{ completion_context = c; }
@Override public BaleCompletionContext getCompletionContext()		{ return completion_context; }


@Override public void setRenameContext(BaleRenameContext c)	{ rename_context = c; }
@Override public BaleRenameContext getRenameContext()		{ return rename_context; }



@Override public void setDocument(Document d)
{
   if (d != null && d instanceof BaleDocument) {
      BaleDocument bd = (BaleDocument) d;
      bd.baleWriteLock();
      try {
	 super.setDocument(bd);
       }
      finally { bd.baleWriteUnlock(); }
    }
   else if (d != null) {
      super.setDocument(d);
    }
}




/********************************************************************************/
/*										*/
/*	Editing methods to increase size if appropriate 			*/
/*										*/
/********************************************************************************/

@Override public void increaseSize(int nline)
{
   if (fixed_size) return;

   int ht = (int)(getBaleDocument().getFontHeight() * nline + 0.5);
   Dimension r = getParent().getParent().getSize();	// size of viewport
   Dimension d = getSize();
   if (d.height + ht < r.height) return;	     // it fits in current window
   if (r.height >= BALE_MAX_GROW_HEIGHT) return;     // too big to auto grow
   for (Component c = this; c != null; c = c.getParent()) {
      if (c instanceof BudaBubble) {
	 Dimension br = c.getSize();
	 if (br.height > BALE_MAX_GROW_HEIGHT) return;
	 br.height += ht;
	 if (br.height > BALE_MAX_GROW_HEIGHT) br.height = BALE_MAX_GROW_HEIGHT;
	 c.setSize(br);
	 break;
       }
    }
}




void increaseSizeForElidedElement(BaleElement be)
{
   if (be == null) return;

   BaleDocument bd = getBaleDocument();
   int slno = bd.findLineNumber(be.getStartOffset());
   int elno = bd.findLineNumber(be.getEndOffset());
   int delta = elno - slno - 1;
   if (delta <= 0) return;

   Dimension d1 = getPreferredSize();
   Dimension d2 = getSize();
   if (d1.height > d2.height) return;

   delta -= fixIncreaseDelta(bd,be);
   if (delta <= 0) return;

   increaseSize(delta);
}



void checkSize()
{
   Dimension d1 = getPreferredSize();
   Dimension d2 = getSize();
   if (d2.height > d1.height) {
      int delta = d2.height - d1.height;
      for (Component c = this; c != null; c = c.getParent()) {
	 if (c instanceof BudaBubble) {
	    Dimension br = c.getSize();
	    if (br.height > BALE_MAX_GROW_HEIGHT) return;
	    br.height -= delta;
	    c.setSize(br);
	    break;
	  }
       }
    }
}



private int fixIncreaseDelta(BaleDocument bd,BaleElement be)
{
   int delta = 0;

   if (be.isElided()) {
      int slno = bd.findLineNumber(be.getStartOffset());
      int elno = bd.findLineNumber(be.getEndOffset());
      delta = elno - slno - 1;
    }
   else if (!be.isLeaf()) {
      int ct = be.getElementCount();
      for (int i = 0; i < ct; ++i) {
	 delta += fixIncreaseDelta(bd,be.getBaleElement(i));
       }
    }

   return delta;
}



/********************************************************************************/
/*										*/
/*	Abstract methods for editing						*/
/*										*/
/********************************************************************************/

@Override abstract public BaleFinder getFindBar();
@Override abstract public BaleAnnotationArea getAnnotationArea();

void toggleFindBar() {}



/********************************************************************************/
/*										*/
/*	Context menu methods							*/
/*										*/
/********************************************************************************/

private static Map<String,String> context_names;

static {
   context_names = new HashMap<String,String>();
   context_names.put("Go to Definition","GotoDefinitionAction");
   context_names.put("Find All References","GotoReferenceAction");
   context_names.put("Go to Documentation","GotoDocAction");
   context_names.put("Go to Implementation","GotoImplementationAction");
   context_names.put("Go to Search","GotoSearchAction");
   context_names.put("Go to Type","GotoTypeAction");
   context_names.put("Remove Elision","RemoveElisionAction");
   context_names.put("Remove All Elisions", "RemoveElisionAction");
   context_names.put("Open Eclipse Editor","OpenEclipseEditorAction");
   context_names.put("Rename","RenameAction");
   context_names.put("Extract Code into New Method","ExtractMethodAction");
   context_names.put("Make Read-Only","ToggleEditableAction");
   context_names.put("Make Editable","ToggleEditableAction");
}



void handleContextMenu(MouseEvent evt)
{
   int loc = SwingText.viewToModel2D(this,evt.getPoint());
   BaleDocument bd = getBaleDocument();
   BaleElement be = bd.getCharacterElement(loc);
   ContextMenuHandler hdlr = new ContextMenuHandler();

   JPopupMenu menu = new JPopupMenu();
   if (bd.isOrphan()) {
      return;
   }

   if (getSelectionStart() == getSelectionEnd() ||
	  loc < getSelectionStart() || loc > getSelectionEnd()) {
      setCaretPosition(loc);
    }

   boolean idok = be.isIdentifier() && !be.isUndefined();
   boolean elok = be.isElided();

   addButton(menu,"Go to Implementation",idok,hdlr,null);
   addButton(menu,"Go to Definition",idok,hdlr,null);
   addButton(menu,"Find All References",idok,hdlr,null);
   if (BALE_PROPERTIES.getBoolean("Bale.goto.documentation",true)) {
      addButton(menu,"Go to Documentation",idok,hdlr,null);
    }
   addButton(menu,"Go to Search",idok,hdlr,null);
   addButton(menu,"Go to Type",idok,hdlr,null);
   addButton(menu,"Remove Elision",elok,hdlr,null);
   addButton(menu,"Remove All Elisions",true,hdlr,null);
   // TODO: only allow rename of user identifiers
   addButton(menu,"Rename",idok,hdlr,null);
   boolean extr = (getSelectionStart() != getSelectionEnd());
   addButton(menu,"Extract Code into New Method",extr,hdlr,null);

   if (bd instanceof BaleDocumentFragment) {
      BaleDocumentFragment bdf = (BaleDocumentFragment) bd;
      if (bdf.isEditable()) addButton(menu,"Make Read-Only",true,hdlr,null);
      else if (bdf.canSetEditable()) addButton(menu,"Make Editable",true,hdlr,null);
    }

   BaleContextConfig bcc = new ContextData(loc,be);
   BaleFactory.getFactory().addContextMenuItems(bcc,menu);

   // addButton(menu,"Open Eclipse Editor",true,hdlr,null);
   addButton(menu,"Remove Bubble",true,new RemoveBubbleHandler(),null);

   menu.add(new FixSizeButton());

   BudaBubble bb = BudaRoot.findBudaBubble(this);
   menu.add(bb.getFloatBubbleAction());

   int ct = menu.getComponentCount();
   if (ct == 0) return;

   menu.show(this,evt.getX(),evt.getY());
}



private void addButton(JPopupMenu m,String txt,boolean enable,ActionListener hdlr,String tt)
{
   JMenuItem mi = new JMenuItem(txt);
   if (hdlr != null) mi.addActionListener(hdlr);
   mi.setEnabled(enable);
   m.add(mi);
   if (tt != null) mi.setToolTipText(tt);
}



private class ContextMenuHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      String acmd = context_names.get(cmd);
      if (acmd != null) {
	 ActionEvent nevt = new ActionEvent(BaleEditorPane.this,e.getID(),acmd,e.getWhen(),e.getModifiers());
	 Action a = BaleEditorKit.findAction(acmd);
	 if (a != null) {
	    a.actionPerformed(nevt);
	    BoardMetrics.noteCommand("BALE","CONTEXT_" + cmd);
	  }
       }
      else {
	 BoardLog.logE("BALE","CONTEXT MENU UNKOWN COMMAND " + cmd);
       }
    }

}	// end of inner class ContextMenuHandler


private class RemoveBubbleHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BaleEditorPane.this);
      BudaBubble bbl = BudaRoot.findBudaBubble(BaleEditorPane.this);
      if (bbl != null && bba != null) {
	 bba.userRemoveBubble(bbl);
       }
    }

}	// end of inner class RemoveBubbleHandler



private class FixSizeButton extends AbstractAction {

   private static final long serialVersionUID = 1;

   FixSizeButton() {
      super(fixed_size ? "Allow Auto Resizing" : "Prevent Auto Resizing");
      putValue(SHORT_DESCRIPTION,"Set whether this bubble will resize automatically with typing");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      fixed_size = !fixed_size;
    }

}	// end of inner class FixSizeButton




/********************************************************************************/
/*										*/
/*	Methods for doing highlighting						*/
/*										*/
/********************************************************************************/

void removeHighlights(BaleHighlightType typ)
{
   HighlightData hd = hilite_map.get(typ);
   if (hd != null) hd.removeAll();
}


void addHighlight(BaleHighlightType typ,BaleRegion rgn)
{
   HighlightData hd = null;

   synchronized(hilite_map) {
      hd = hilite_map.get(typ);
      if (hd == null) {
	 hd = new HighlightData(typ);
	 hilite_map.put(typ,hd);
       }
    }

   try {
      Position p0 = getBaleDocument().createPosition(rgn.getStart());
      Position p1 = getBaleDocument().createPosition(rgn.getEnd());
      hd.add(p0,p1);
    }
   catch (BadLocationException e) { }
}


List<BaleSimpleRegion> getHighlights(BaleHighlightType typ)
{
   HighlightData hd = hilite_map.get(typ);
   if (hd == null) return null;

   return hd.getRegions();
}



void updateHighlights()
{
   synchronized (hilite_map) {
      for (HighlightData hd : hilite_map.values()) hd.update();
    }
}


void removeHighlights()
{
   synchronized (hilite_map) {
      for (HighlightData hd : hilite_map.values()) hd.removeAll();
    }
}



/********************************************************************************/
/*										*/
/*	Visualization help methods						*/
/*										*/
/********************************************************************************/

private String getIconIndication()
{
   return getIndication(VISUALIZATION_ICON_INDICATION);
}



private String getGradientIndication()
{
   return getIndication(VISUALIZATION_GRADIENT_INDICATION);
}



private String getIndication(String key)
{
   BaleDocument bd = getBaleDocument();
   String proj = bd.getProjectName();
   String name = bd.getFragmentName();
   return BaleVisualizationKit.getIndication(proj,name,key);
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintComponent(Graphics g0)
{
   Graphics2D g2 = (Graphics2D) g0.create();
   Dimension sz = getSize();

   // paint the background
   Paint p;
   if (!isEditable() || !getBaleDocument().isEditable()) {
      Color tc = BoardColors.getColor(BALE_READONLY_BACKGROUND_TOP);
      Color bc = BoardColors.getColor(BALE_READONLY_BACKGROUND_BOTTOM);
      if (!BALE_PROPERTIES.getBoolean(BALE_EDITOR_DO_GRADIENT)) bc = tc;
      if (tc.getRGB() == bc.getRGB()) p = tc;
      else p = new GradientPaint(0f,0f,tc,0f,sz.height,bc);
    }
   else if (BALE_PROPERTIES.getBoolean(VISUALIZATION_GRADIENT_ENABLE)) {
       p = visual_kit.getGradientPaint(sz, getGradientIndication());
   }
   else {
      Color tc = BoardColors.getColor(BALE_EDITOR_TOP_COLOR_PROP);
      Color bc = BoardColors.getColor(BALE_EDITOR_BOTTOM_COLOR_PROP);
      if (!BALE_PROPERTIES.getBoolean(BALE_EDITOR_DO_GRADIENT)) bc = tc;
      if (tc.getRGB() == bc.getRGB()) p = tc;
      else p = new GradientPaint(0f,0f,tc,0f,sz.height,bc);
   }
   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   g2.setPaint(p);
   g2.fill(r);

   //paint icons
   if (BALE_PROPERTIES.getBoolean(VISUALIZATION_ICON_ENABLE)) {
      BufferedImage icon = visual_kit.getIcon(getIconIndication());
      if (icon != null) {
	 Point location = visual_kit.getLocation(sz, new Dimension(icon.getWidth(), icon.getHeight()));
	 g2.drawImage(icon, location.x, location.y, null);
      }
   }

   getBaleDocument().baleReadLock();
   try {
      clearRegions(g2.getClipBounds());
      BaleAnnotationArea baa = getAnnotationArea();
      if (baa != null) {
	 baa.paintEditor(g2);
       }
      super.paintComponent(g0);
    }
   finally { getBaleDocument().baleReadUnlock(); }
}



/********************************************************************************/
/*										*/
/*	TextUI that does the correct bale locking				*/
/*										*/
/********************************************************************************/

private static class BaleTextUI extends TextUI {

   private Position.Bias [] discard_bias = new Position.Bias[1];
   private TextUI base_ui;

   BaleTextUI(TextUI tui) {
      base_ui = tui;
    }

   @Override public void damageRange(JTextComponent t,int p0,int p1) {
      damageRange(t,p0,p1,Position.Bias.Forward,Position.Bias.Backward);
    }

   @Override public void damageRange(JTextComponent t,int p0,int p1,
					Position.Bias fb,Position.Bias sb) {
      readLock(t);
      try {
	 base_ui.damageRange(t,p0,p1,fb,sb);
       }
      finally { readUnlock(t); }
    }

   @Override public EditorKit getEditorKit(JTextComponent t) {
      return base_ui.getEditorKit(t);
    }

   @Override public int getNextVisualPositionFrom(JTextComponent t,int pos,Position.Bias b,
						     int dir,Position.Bias [] bret)
		throws BadLocationException {
      readLock(t);
      try {
	 return base_ui.getNextVisualPositionFrom(t,pos,b,dir,bret);
       }
      finally { readUnlock(t); }
    }

   @Override public View getRootView(JTextComponent t) {
      return base_ui.getRootView(t);
    }

   @Deprecated public String getToolTipText(JTextComponent t,Point pt) {
      readLock(t);
      try {
	 return SwingText.getToolTipText2D(base_ui,t,pt);
       }
      finally { readUnlock(t); }
    }

   @Deprecated public Rectangle modelToView(JTextComponent t,int pos) throws BadLocationException {
      return modelToView(t,pos,Position.Bias.Forward);
    }

   @Deprecated public Rectangle modelToView(JTextComponent t,int pos,Position.Bias bias)
		throws BadLocationException {
      readLock(t);
      try {
	 return SwingText.modelToView2D(base_ui,t,pos,bias);
       }
      finally { readUnlock(t); }
    }

   public Rectangle2D modelToView2D(JTextComponent t,int pos,Position.Bias bias)
	    throws BadLocationException {
      readLock(t);
      try {
	 return SwingText.modelToView2D(base_ui,t,pos,bias);
      }
      finally { readUnlock(t); }
   }

   @Deprecated public int viewToModel(JTextComponent t,Point pt) {
      return viewToModel2D(t,pt,discard_bias);
    }

   @Deprecated public int viewToModel(JTextComponent t,Point pt,Position.Bias [] bret) {
      return viewToModel2D(t,pt,bret);
    }

   public int viewToModel2D(JTextComponent t,Point2D pt,Position.Bias [] bret) {
      readLock(t);
      try {
	 return SwingText.viewToModel2D(base_ui,t,pt,bret);
       }
      finally { readUnlock(t); }
    }

   @Override public void installUI(JComponent c) {
      writeLock(c);
      try {
	 base_ui.installUI(c);
       }
      finally { writeUnlock(c); }
    }

   @Override public void uninstallUI(JComponent c) {
      base_ui.uninstallUI(c);
    }

   @Override public void paint(Graphics g,JComponent c) {
      readLock(c);
      try {
	 base_ui.paint(g,c);
       }
      finally { readUnlock(c); }
    }

   @Override public void update(Graphics g,JComponent c) {
      paint(g,c);
    }

   @Override public Dimension getPreferredSize(JComponent c) {
      readLock(c);
      try {
	 return base_ui.getPreferredSize(c);
       }
      finally { readUnlock(c); }
    }

   @Override public Dimension getMinimumSize(JComponent c) {
      readLock(c);
      try {
	 return base_ui.getMinimumSize(c);
       }
      finally { readUnlock(c); }
    }

   @Override public Dimension getMaximumSize(JComponent c) {
      readLock(c);
      try {
	 return base_ui.getMaximumSize(c);
       }
      finally { readUnlock(c); }
    }

   @Override public boolean contains(JComponent c,int x,int y) {
      return base_ui.contains(c,x,y);
    }

   @Override public int getBaseline(JComponent c,int w,int h) {
      return base_ui.getBaseline(c,w,h);
    }

   @Override public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
      return base_ui.getBaselineResizeBehavior(c);
    }

   @Override public int getAccessibleChildrenCount(JComponent c) {
      return base_ui.getAccessibleChildrenCount(c);
    }

   @Override public Accessible getAccessibleChild(JComponent c,int i) {
      return base_ui.getAccessibleChild(c,i);
    }


   private void readLock(JTextComponent t) {
      if (t.getDocument() instanceof BaleDocument) {
	 BaleDocument bd = (BaleDocument) t.getDocument();
	 bd.baleReadLock();
       }
    }

   private void readUnlock(JTextComponent t) {
      if (t.getDocument() instanceof BaleDocument) {
	 BaleDocument bd = (BaleDocument) t.getDocument();
	 bd.baleReadUnlock();
       }
    }

   private void readLock(JComponent c) {
      if (c instanceof JTextComponent) readLock((JTextComponent) c);
    }

   private void readUnlock(JComponent c) {
      if (c instanceof JTextComponent) readUnlock((JTextComponent) c);
    }

   private void writeLock(JTextComponent t) {
      if (t.getDocument() instanceof BaleDocument) {
	 BaleDocument bd = (BaleDocument) t.getDocument();
	 bd.baleWriteLock();
       }
    }

   private void writeUnlock(JTextComponent t) {
      if (t.getDocument() instanceof BaleDocument) {
	 BaleDocument bd = (BaleDocument) t.getDocument();
	 bd.baleWriteUnlock();
       }
    }

   private void writeLock(JComponent c) {
      if (c instanceof JTextComponent) writeLock((JTextComponent) c);
    }

   private void writeUnlock(JComponent c) {
      if (c instanceof JTextComponent) writeUnlock((JTextComponent) c);
    }

}	// end of inner class BaleTextUI




/********************************************************************************/
/*										*/
/*	Class for holding hilite information					*/
/*										*/
/********************************************************************************/

private class HighlightData {

   private Highlighter.HighlightPainter our_painter;
   private Set<HighlightArea> active_hilites;

   HighlightData(BaleHighlightType typ) {
      our_painter = BaleHighlightContext.getPainter(typ);
      active_hilites = new HashSet<HighlightArea>();
      hilite_map.put(typ,this);
    }

   synchronized void add(Position p0,Position p1) {
      HighlightArea ha = new HighlightArea(this,p0,p1);
      active_hilites.add(ha);
      ha.add();
    }

   synchronized void removeAll() {
      for (HighlightArea ha : active_hilites) {
	 ha.remove();
       }
      active_hilites.clear();
    }

   synchronized void update() {
      for (HighlightArea ha : active_hilites) {
	 ha.update();
       }
    }

   Highlighter.HighlightPainter getPainter()		{ return our_painter; }

   synchronized List<BaleSimpleRegion> getRegions() {
      List<BaleSimpleRegion> rslt = new ArrayList<BaleSimpleRegion>();
      for (HighlightArea ha : active_hilites) {
	 BaleSimpleRegion bsr = ha.getSimpleRegion();
	 if (bsr != null) rslt.add(bsr);
       }
      if (rslt.size() == 0) return null;
      return rslt;
    }

}	// end of inner class HighlightData




private class HighlightArea {

   private Highlighter.HighlightPainter use_painter;
   private Object our_tag;
   private Position from_pos;
   private int from_offset;
   private Position to_pos;
   private int to_offset;

   HighlightArea(HighlightData hd,Position p0,Position p1) {
      use_painter = hd.getPainter();
      from_pos = p0;
      from_offset = -1;
      to_pos = p1;
      to_offset = -1;
      our_tag = null;
    }

   void add() {
      from_offset = from_pos.getOffset();
      to_offset = to_pos.getOffset();
      try {
	 our_tag = getHighlighter().addHighlight(from_offset,to_offset,use_painter);
       }
      catch (BadLocationException e) { }
    }

   void remove() {
      if (our_tag != null) {
	 getHighlighter().removeHighlight(our_tag);
	 our_tag = null;
       }
    }

   void update() {
      if (our_tag == null) add();
      else {
	 int p0 = from_pos.getOffset();
	 int p1 = to_pos.getOffset();
	 if (p0 != from_offset || p1 != to_offset) {
	    from_offset = p0;
	    to_offset = p1;
	    try {
	       getHighlighter().changeHighlight(our_tag,from_offset,to_offset);
	     }
	    catch (BadLocationException e) {
	       remove();
	     }
	  }
       }
    }

   BaleSimpleRegion getSimpleRegion() {
      if (from_offset < 0) return null;
      return new BaleSimpleRegion(from_offset,to_offset - from_offset);
    }

}	// end of inner class HighlightArea




/********************************************************************************/
/*										*/
/*	Hover management							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent e)
{
   BaleDocument bd = getBaleDocument();
   bd.baleReadLock();
   try {
      return getHoverText(e);
    }
   finally { bd.baleReadUnlock(); }
}




private String getHoverText(MouseEvent e)
{
   String txt = null;
   int loc = -1;

   try {
      loc = SwingText.viewToModel2D(this,e.getPoint());
      // TODO: Why might this fail?  Fails with ArrayIndexOutOfBoundsException
      //   in sun.font.FontDesignMetrics, deep inside swing.
    }
   catch (Throwable t) {
      BoardLog.logE("BALE","Problem with getting hover text: " + t);
      return null;
    }

   BaleDocument bd = getBaleDocument();
   BaleElement be = bd.getCharacterElement(loc);
   if (be != null && be.isElided()) {
      int soff = be.getStartOffset();
      int eoff = be.getEndOffset();
      try {
	 txt = bd.getText(soff,eoff-soff);
	 txt = IvyXml.xmlSanitize(txt,false,true);
	 txt = txt.replace("&apos;","'");
	 txt = txt.replace("&quot;","\"");
	 txt = "<html><pre>" + txt + "</pre></html>";
       }
      catch (BadLocationException ex) { }
    }
   else {
      BaleContextConfig bcc = new ContextData(loc,be);
      txt = BaleFactory.getFactory().getContextToolTip(bcc);
    }

   if (txt == null && be != null && be.isIdentifier() && be.getName().contains("FieldId")) {
      BumpClient bc = BumpClient.getBump();
      String fullnm = null;
      fullnm  = bc.getFullyQualifiedName(bd.getProjectName(),bd.getFile(),
					    bd.mapOffsetToEclipse(loc),
					    bd.mapOffsetToEclipse(loc),5000);
      if (fullnm != null) txt = fullnm;
    }

   if (txt == null) {
      BumpClient bc = BumpClient.getBump();
      Element rslt = bc.getHoverData(bd.getProjectName(),bd.getFile(),
	    bd.mapOffsetToEclipse(loc),
	    bd.mapOffsetToEclipse(loc),2000);
      BoardLog.logD("BALE","HOVERDATA result: " + IvyXml.convertXmlToString(rslt));
      String hovertext = IvyXml.getTextElement(rslt,"HOVER");
      if (hovertext != null && hovertext.startsWith("<") && !hovertext.startsWith("<html>")) {
	 hovertext = "<html>" + hovertext;
       }
      if (hovertext != null && !hovertext.isEmpty()) txt = hovertext;
    }

   return txt;
}



/**
 * This method returns a corresponding bubble if hover text is a method or function,
 * otherwise, returns null.
 **/

BudaBubble getHoverBubble(MouseEvent e)
{
   BaleElement be = getHoverElement(e);
   int loc = SwingText.viewToModel2D(this,e.getPoint());
   BaleDocument bd = getBaleDocument();
   BudaBubble bb = null;
   BumpClient bc = BumpClient.getBump();

   if (be != null && be.isIdentifier() && be.getName().contains("CallId")) {
      String fullnm = null;
      fullnm  = bc.getFullyQualifiedName(bd.getProjectName(),bd.getFile(),
					    bd.mapOffsetToEclipse(loc),
					    bd.mapOffsetToEclipse(loc),5000);
      if (fullnm != null) {
	 BaleFactory bf = BaleFactory.getFactory();
	 bb = bf.createMethodBubble(bd.getProjectName(),fullnm);
       }
    }

   if (bb == null) {
      bb = handleActiveHoverBubble(e);
    }

   if (bb == null) {
      BaleContextConfig bcc = new ContextData(loc,be);
      bb = BaleFactory.getFactory().getContextHoverBubble(bcc);
    }

// if (bb == null) {
//    Element rslt = bc.getHoverData(bd.getProjectName(),bd.getFile(),
//	    bd.mapOffsetToEclipse(loc),
//	    bd.mapOffsetToEclipse(loc),4000);
//    BoardLog.logD("BALE","HOVERDATA result: " + IvyXml.convertXmlToString(rslt));
//    String text = IvyXml.getTextElement(rslt,"HOVER");
//    if (text.startsWith("<") && !text.startsWith("<html>")) text = "<html>" + text;
//  }

   return bb;
}



BaleElement getHoverElement(MouseEvent e)
{
   int loc = SwingText.viewToModel2D(this,e.getPoint());
   BaleDocument bd = getBaleDocument();
   BaleElement be = bd.getCharacterElement(loc);

   if (be != null && be.isIdentifier()) return be;

   return null;
}



/********************************************************************************/
/*										*/
/*	Active region management						*/
/*										*/
/********************************************************************************/

void clearRegions()
{
   active_regions.clear();
}


void clearRegions(Rectangle r)
{
   for (Iterator<ActiveRegion> it = active_regions.iterator(); it.hasNext(); ) {
      ActiveRegion ar = it.next();
      if (r == null || ar.intersects(r)) it.remove();
    }
}




void addActiveRegion(int x,int y,int w,int h,RegionAction act)
{
   ActiveRegion ar = new ActiveRegion(new Rectangle(x,y,w,h),act);
   active_regions.add(ar);
}


boolean handleActiveClick(MouseEvent e)
{
   for (ActiveRegion r : active_regions) {
      if (r.contains(e.getX(),e.getY())) {
	 if (r.perform(e)) return true;
       }
    }

   return false;
}



BudaBubble handleActiveHoverBubble(MouseEvent e)
{
   for (ActiveRegion r : active_regions) {
      if (r.contains(e.getX(),e.getY())) {
	 BudaBubble bb = r.hoverBubble(e);
	 if (bb != null) return bb;
       }
    }

   return null;
}



private static class ActiveRegion {

   private Rectangle region_bounds;
   private RegionAction region_action;

   ActiveRegion(Rectangle bnds,RegionAction act) {
      region_bounds = bnds;
      region_action = act;
    }

   boolean contains(int x,int y)		{ return region_bounds.contains(x,y); }
   boolean intersects(Rectangle r)		{ return region_bounds.intersects(r); }

   boolean perform(MouseEvent e) {
      if (region_action == null) return false;
      region_action.handleClick(e);
      return true;
    }

   BudaBubble hoverBubble(MouseEvent e) {
      if (region_action == null) return null;
      return region_action.handleHoverBubble(e);
    }

}	// end of inner class ActiveRegion



private class ActiveMouser extends MouseAdapter {

   private boolean over_region;
   private Cursor base_cursor;

   ActiveMouser() {
      over_region = false;
      base_cursor = null;
    }

   @Override public void mouseMoved(MouseEvent e) {
      boolean set = false;
      for (ActiveRegion r : active_regions) {
	 if (r.contains(e.getX(),e.getY())) {
	    set = true;
	    break;
	  }
       }
      if (set == over_region) return;
      if (base_cursor == null) base_cursor = getCursor();
      over_region = set;
      if (set)
	 BudaCursorManager.setTemporaryCursor(BaleEditorPane.this,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      else
	 BudaCursorManager.setTemporaryCursor(BaleEditorPane.this,Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

}	// end of inner class ActiveMouser




/********************************************************************************/
/*										*/
/*	Context data for external users 					*/
/*										*/
/********************************************************************************/

private static final Map<String,BaleContextType> context_types;

static {
   context_types = new HashMap<String,BaleContextType>();
   context_types.put("Identifier",BaleContextType.LOCAL_ID);
   context_types.put("FieldId",BaleContextType.FIELD_ID);
   context_types.put("StaticFieldId",BaleContextType.STATIC_FIELD_ID);
   context_types.put("ClassDeclId",BaleContextType.CLASS_DECL_ID);
   context_types.put("MethodDeclId",BaleContextType.METHOD_DECL_ID);
   context_types.put("LocalDeclId",BaleContextType.LOCAL_DECL_ID);
   context_types.put("FieldDeclId",BaleContextType.FIELD_DECL_ID);
   context_types.put("CallId",BaleContextType.CALL_ID);
   context_types.put("StaticCallId",BaleContextType.STATIC_CALL_ID);
   context_types.put("UndefCallId",BaleContextType.UNDEF_CALL_ID);
   context_types.put("AnnotationId",BaleContextType.ANNOTATION_ID);
   context_types.put("DeprecatedCallId",BaleContextType.CALL_ID);
   context_types.put("TypeId",BaleContextType.TYPE_ID);
   context_types.put("ConstId",BaleContextType.CONST_ID);
   context_types.put("UndefId", BaleContextType.UNDEF_ID);
   context_types.put("UndefCallId", BaleContextType.UNDEF_CALL_ID);
}



private class ContextData implements BaleContextConfig {

   private int		editor_location;
   private BaleElement	editor_element;

   ContextData(int loc,BaleElement be) {
      editor_location = loc;
      editor_element = be;
    }

   @Override public BudaBubble getEditor() {
      return BudaRoot.findBudaBubble(BaleEditorPane.this);
    }

   @Override public BaleFileOverview getDocument() {
      return (BaleFileOverview) getBaleDocument();
    }

   @Override public int getOffset()				{ return editor_location; }
   @Override public int getDocumentOffset() {
      return getBaleDocument().getDocumentOffset(editor_location);
    }
   @Override public int getLineNumber() {
      return getBaleDocument().findLineNumber(editor_location);
    }

   @Override public String getToken() {
      if (editor_element == null || !editor_element.isIdentifier()) return null;
      try {
	 return getBaleDocument().getText(editor_element.getStartOffset(),
					     editor_element.getEndOffset()-editor_element.getStartOffset());
       }
      catch (BadLocationException e) { }
      return null;
    }

   @Override public BaleContextType getTokenType() {
      if (editor_element == null) return BaleContextType.NONE;
      String nm = editor_element.getName();
      BaleContextType t = context_types.get(nm);
      if (t != null) return t;
      return BaleContextType.NONE;
    }

   @Override public String getMethodName() {
      if (editor_element == null) {
	 BoardLog.logD("BALE","No editor element for getMethodName");
	 return null;
       }
      return editor_element.getMethodName();
    }

   @Override public boolean inAnnotationArea()		{ return false; }

   @Override public int getSelectionStart() {
      return BaleEditorPane.this.getSelectionStart();
    }

   @Override public int getSelectionEnd() {
      return BaleEditorPane.this.getSelectionEnd();
    }

}	// end of inner class ContextData



/********************************************************************************/
/*										*/
/*	Reasonably sized tool tip implementation				*/
/*										*/
/********************************************************************************/

@Override public JToolTip createToolTip()
{
   BudaToolTip btt = new BudaToolTip();
   btt.setComponent(this);
   return btt;
}



}	// end of class BaleEditorPane




/* end of BaleEditorPane.java */
