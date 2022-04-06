/********************************************************************************/
/*										*/
/*		BaleAnnotationArea.java 					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor annotation panel	*/
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


/* RCS: $Header$ */

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
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


class BaleAnnotationArea extends JPanel implements DocumentListener, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private transient BaleEditor	for_editor;
private BaleDocument	for_document;
private transient Set<BaleAnnotation> annot_set;
private transient LineData []	line_data;
private int		start_line;
private transient Map<Integer,Collection<BaleAnnotation>> annot_map;
private boolean 	update_needed;
private boolean 	show_line_numbers;
private transient BumpBreakModel break_model;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleAnnotationArea(BaleEditor be)
{
   for_editor = be;
   for_document = be.getBaleDocument();

   setMinimumSize(new Dimension(BALE_ANNOT_WIDTH,0));
   setMaximumSize(new Dimension(BALE_ANNOT_WIDTH,0));
   setPreferredSize(new Dimension(BALE_ANNOT_WIDTH,0));
   setOpaque(false);
   setToolTipText("Annotation area");
   BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   addMouseListener(new Mouser());
   JComponent jbe = (JComponent) be;
   jbe.addComponentListener(new CheckUpdater());

   annot_set = new HashSet<>();
   start_line = 0;
   line_data = null;
   annot_map = null;
   update_needed = true;
   show_line_numbers = false;

   if (BALE_PROPERTIES.getBoolean("Bale.show.line.numbers",false)) {
      showLineNumbers(true);
    }

   for_document.addDocumentListener(this);

   break_model = BumpClient.getBump().getBreakModel();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void showLineNumbers(boolean fg)
{
   if (show_line_numbers == fg) return;
   show_line_numbers = fg;
   update_needed = true;

   if (show_line_numbers) {
      setMinimumSize(new Dimension(BALE_ANNOT_NUMBER_WIDTH,0));
      setMaximumSize(new Dimension(BALE_ANNOT_NUMBER_WIDTH,0));
      setPreferredSize(new Dimension(BALE_ANNOT_NUMBER_WIDTH,0));
    }
   else {
      setMinimumSize(new Dimension(BALE_ANNOT_WIDTH,0));
      setMaximumSize(new Dimension(BALE_ANNOT_WIDTH,0));
      setPreferredSize(new Dimension(BALE_ANNOT_WIDTH,0));
    }
  getParent().invalidate();
}



/********************************************************************************/
/*										*/
/*	Annotation information							*/
/*										*/
/********************************************************************************/

BaleAnnotation addAnnotation(BaleAnnotation an)
{
   synchronized (annot_set) {
      annot_set.add(an);
      annot_map = null;
    }

   repaint();

   return an;
}


void removeAnnotation(BaleAnnotation ba)
{
   boolean chng = false;

   synchronized (annot_set) {
      if (annot_set.remove(ba)) {
	 chng = true;
	 annot_map = null;
       }
    }

   if (chng) {
      getParent().repaint();
    }
}





void removeAll(BalePosition start,BalePosition end)
{
   int soff = start.getOffset();
   int eoff = end.getOffset();
   boolean chng = false;

   synchronized (annot_set) {
      for (Iterator<BaleAnnotation> it = annot_set.iterator(); it.hasNext(); ) {
	 BaleAnnotation an = it.next();
	 int off = for_document.getFragmentOffset(an.getDocumentOffset());
	 if (off >= soff && off < eoff) {
	    it.remove();
	    chng = true;
	  }
       }
    }

   if (chng) repaint();
}



/********************************************************************************/
/*										*/
/*	Painting interface							*/
/*										*/
/********************************************************************************/

@Override public void paintComponent(Graphics g)
{
   Graphics2D g2 = (Graphics2D) g;
   Rectangle bnd = getBounds();
   Paint p;

   if (!BALE_PROPERTIES.getBoolean(BALE_EDITOR_DO_GRADIENT)) {
      p = BoardColors.getColor(BALE_EDITOR_TOP_COLOR_PROP);
    }
   else {
      p = new GradientPaint(0f,0f, BoardColors.getColor(BALE_EDITOR_TOP_COLOR_PROP), 0f,
	    bnd.height, BoardColors.getColor(BALE_EDITOR_BOTTOM_COLOR_PROP));
    }

   g2.setPaint(p);
   g2.fillRect(bnd.x, bnd.y, bnd.width, bnd.height);
   g2.setColor(BoardColors.getColor(BALE_ANNOT_BAR_COLOR_PROP));
   g2.fillRoundRect(bnd.x+3,bnd.y,bnd.width - 6,bnd.height, 3, 1);

   for_document.baleReadLock();
   try {
      // TODO: this should only be done when the editor has updated
      // i.e. edits done, size changed
      recheckLines();

      if (show_line_numbers) {
	 g2.setFont(BALE_PROPERTIES.getFont(BALE_ANNOT_FONT));
	 g2.setColor(BoardColors.getColor(BALE_ANNOT_LINE_NUMBER_COLOR));
	 Rectangle2D.Double r2 = new Rectangle2D.Double();
	 for (int lno = start_line; lno < start_line + line_data.length; ++lno) {
	    LineData ld = line_data[lno-start_line];
	    if (ld == null) continue;
	    int y0 = ld.getTop();
	    int y1 = ld.getBottom();
	    r2.setFrame(0,y0,BALE_ANNOT_NUMBER_WIDTH-3,y1-y0);
	    SwingText.drawTextRight(Integer.toString(lno),g2,r2);
	  }
       }

      Map<Integer,Collection<BaleAnnotation>> amap = setupAnnotationMap();

      for (Map.Entry<Integer,Collection<BaleAnnotation>> ent : amap.entrySet()) {
	 int lno = ent.getKey();
	 if (lno - start_line < 0 || lno - start_line >= line_data.length) {
	    BoardLog.logE("BALE",
		  "Annotation beyond bounds " + lno + " " + start_line + " " + line_data.length);
	    continue;
	  }
	 LineData ld = line_data[lno-start_line];
	 if (ld == null) continue;
	 int y0 = ld.getTop();
	 int y1 = ld.getBottom();
	 Collection<BaleAnnotation> lan = ent.getValue();
	 for (BaleAnnotation an : lan) {
	    Color c = an.getBackgroundColor();
	    if (c != null) {
	       g2.setColor(c);
	       g2.fillRect(bnd.x,y0,bnd.width,y1-y0);
	       break;			// only allow one color for now
	     }
	  }
	 for (BaleAnnotation an : lan) {
	    BudaBubble bbl = BudaRoot.findBudaBubble(this);
	    Icon ic = an.getIcon(bbl);
	    if (ic == null) continue;
	    int wid = ic.getIconWidth();
	    int ht = ic.getIconHeight();
	    int x = bnd.x + bnd.width/2 - wid/2;
	    int y = (y0 + y1)/2 - ht/2;
	    try {
	       ic.paintIcon(this,g,x,y);
	     }
	    catch (Throwable t) {
	       BoardLog.logE("BALE","Problem drawing annotation " + an.getToolTip() + " " + 
                     ic + " " + an.getClass(),t);
	     }
	  }
       }
    }
   finally { for_document.baleReadUnlock(); }
}



void paintEditor(Graphics2D g)
{
   // recheckLines

   Map<Integer,Collection<BaleAnnotation>> amap = setupAnnotationMap();

   BudaBubble bbl = BudaRoot.findBudaBubble(this);

   Rectangle r = g.getClipBounds();

   for (Map.Entry<Integer,Collection<BaleAnnotation>> ent : amap.entrySet()) {
      int lno = ent.getKey();
      if (lno - start_line >= line_data.length) continue;
      LineData ld = line_data[lno-start_line];
      if (ld == null) continue;
      int y0 = ld.getTop();
      int y1 = ld.getBottom();
      Collection<BaleAnnotation> lan = ent.getValue();
      for (BaleAnnotation an : lan) {
	 Color c = an.getLineColor(bbl);
	 if (c != null) {
	    g.setColor(c);
	    g.fillRect(r.x,y0,r.width,y1-y0);
	  }
       }
    }
}




private Map<Integer,Collection<BaleAnnotation>> setupAnnotationMap()
{
   if (line_data == null) recheckLines();

   synchronized (annot_set) {
      if (annot_map == null && line_data != null) {
	 annot_map = new HashMap<>();

	 for (BaleAnnotation an : annot_set) {
	    int off = for_document.getFragmentOffset(an.getDocumentOffset());
	    int lno = for_document.findLineNumber(off);
	    if (lno < start_line || lno >= start_line + line_data.length) continue;
	    Collection<BaleAnnotation> lan = annot_map.get(lno);
	    if (lan == null) {
	       lan = new TreeSet<BaleAnnotation>(new AnnotComparator());
	       annot_map.put(lno,lan);
	    }
	    lan.add(an);
	 }
       }
      else if (annot_map == null) return new HashMap<>();
      return annot_map;
   }
}



private static class AnnotComparator implements Comparator<BaleAnnotation> {

   @Override public int compare(BaleAnnotation a1,BaleAnnotation a2) {
      int d1 = a1.getPriority() - a2.getPriority();
      if (d1 != 0) return d1;
      int d2 = a1.getDocumentOffset() - a2.getDocumentOffset();
      if (d2 != 0) return d2;
      return a1.hashCode() - a2.hashCode();
    }

}	// end of inner class AnnotComparator




/********************************************************************************/
/*										*/
/*	Popup menu interface							*/
/*										*/
/********************************************************************************/

void handleContextMenu(MouseEvent evt)
{
   Map<Integer,Collection<BaleAnnotation>> amap = annot_map;

   if (line_data == null || amap == null) return;

   int lno = findLine(evt);
   if (lno < 0) return;
   Collection<BaleAnnotation> lan = amap.get(lno);

   JPopupMenu menu = new JPopupMenu();

   if (lan != null) {
      for (BaleAnnotation ba : lan) {
	 ba.addPopupButtons(((Component) for_editor),menu);
       }
    }

   AnnotationContext ctx = new AnnotationContext(lno);
   BaleFactory.getFactory().addContextMenuItems(ctx,menu);

   menu.add(new ShowLineAction());

   if (menu.getComponentCount() == 0) return;

   menu.show(this,evt.getX(),evt.getY());
}




private class AnnotationContext implements BaleContextConfig {

   private int line_number;

   AnnotationContext(int lno) {
      line_number = lno;
    }

   @Override public BudaBubble getEditor() {
      return BudaRoot.findBudaBubble(BaleAnnotationArea.this);
    }

   @Override public BaleFileOverview getDocument() {
      return (BaleFileOverview) for_document;
    }

   @Override public int getOffset() {
      return for_document.findLineOffset(line_number);
    }

   @Override public int getDocumentOffset() {
      return for_document.getDocumentOffset(getOffset());
    }

   @Override public String getToken()				{ return null; }
   @Override public BaleContextType getTokenType()		{ return BaleContextType.NONE; }
   @Override public String getMethodName()			{ return null; }
   @Override public int getLineNumber() 			{ return line_number; }
   @Override public boolean inAnnotationArea()			{ return true; }
   
   @Override public int getSelectionStart() {
      if (for_editor instanceof BaleEditorPane) {
         BaleEditorPane bep = (BaleEditorPane) for_editor;
         return bep.getSelectionStart();
       }
      return getOffset();
    }

   @Override public int getSelectionEnd() {
      if (for_editor instanceof BaleEditorPane) {
         BaleEditorPane bep = (BaleEditorPane) for_editor;
         return bep.getSelectionEnd();
       }
      return for_document.findLineOffset(line_number+1);
    }
   
}




private int findLine(MouseEvent evt)
{
   if (line_data == null) return -1;

   int y0 = evt.getY();

   for (int i = 0; i < line_data.length; ++i) {
      if (line_data[i] == null) continue;
      int yt = line_data[i].getTop();
      int yb = line_data[i].getBottom();
      if (y0 >= yt && y0 < yb) return start_line + i;
    }

   return -1;
}




/********************************************************************************/
/*										*/
/*	Tool tip interface							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent evt)
{
   Map<Integer,Collection<BaleAnnotation>> amap = annot_map;

   if (line_data == null || amap == null) return null;

   int lno = findLine(evt);
   if (lno < 0) return null;

   Collection<BaleAnnotation> lan = amap.get(lno);
   List<String> tips = new ArrayList<String>();

   if (lan != null) {
      for (BaleAnnotation an : lan) {
	 String tt = an.getToolTip();
	 if (tt != null) tips.add(tt);
       }
    }

   StringBuffer buf = new StringBuffer();
   if (tips.size() == 0) buf.append("Line " + lno);
   else if (tips.size() == 1) {
      String tt = tips.get(0);
      buf.append("<html><body>");
      buf.append("Line " + lno + ": " + tt);
    }
   else {
      buf.append("<html><body>");
      buf.append("Line " + lno + " multiple annotations:");
      for (String tt : tips) {
	 buf.append("<br>&nbsp;&nbsp;");
	 buf.append(tt);
       }
    }

   return buf.toString();
}


// @Override public Point getToolTipLocation(MouseEvent e)
// {
   // return BudaRoot.computeToolTipLocation(e);
// }


/********************************************************************************/
/*										*/
/*	Methods to find line boundaries 					*/
/*										*/
/********************************************************************************/

private void recheckLines()
{
   if (!update_needed) return;
   update_needed = false;

   boolean chng = false;

   for_document.baleReadLock();
   try {
      Element e1 = for_document.getDefaultRootElement();
      if (!(e1 instanceof BaleElement)) return;
      BaleElement root = (BaleElement) e1;
      start_line = for_document.findLineNumber(0);
      int endlno = for_document.findLineNumber(for_document.getLength());
      int len = endlno-start_line+1;
      if (len < 0) return;
      if (line_data == null || line_data.length < len) {
	 line_data = new LineData[len];
       }
      chng = checkElement(root);
    }
   finally { for_document.baleReadUnlock(); }

   if (chng) {
      synchronized (annot_set) {
	 annot_map = null;
       }
      repaint();
    }
}



private boolean checkElement(BaleElement be)
{
   boolean chng = false;
   Rectangle r;
   Rectangle r1 = null;

   if (be.isLineElement() || (be.isUnknown() && be.isEndOfLine())) {
      int off = be.getStartOffset();
      int eoff = be.getEndOffset();
      if (off < 0 || eoff < off) return true;
      int y0;
      try {
         r = SwingText.modelToView2D(for_editor,off);
	 if (r == null) return chng;
	 if (eoff < be.getDocument().getLength()-1) r1 = SwingText.modelToView2D(for_editor,eoff);
	 if (r1 == null) y0 = r.y + r.height;
	 else y0 = r1.y;
       }
      catch (BadLocationException e) { return chng; }
      int lno = for_document.findLineNumber(off)-start_line;
      if (lno < 0 || lno >= line_data.length) ;
      else if (line_data[lno] == null) {
	 line_data[lno] = new LineData(start_line+lno,r.y,y0);//-1);
	 chng = true;
       }
      else chng |= line_data[lno].set(start_line+lno,r.y,y0);//-1);
    }
   else if (be.isElided()) {
      int soff = be.getStartOffset();
      int eoff = be.getEndOffset();
      try {
         r = SwingText.modelToView2D(for_editor,soff);
	 if (r == null) return chng;
       }
      catch (BadLocationException e) { return chng; }
      int slno = for_document.findLineNumber(soff)-start_line;
      int elno = for_document.findLineNumber(eoff)-start_line;
      if (slno < 0) slno = elno+1;
      for (int lno = slno; lno <= elno; ++lno) {
	 if (line_data[lno] == null) {
	    line_data[lno] = new LineData(start_line+lno,r.y,r.y+r.height);//-1);
	    chng = true;
	  }
	 else chng |= line_data[lno].set(start_line+lno,r.y,r.y+r.height);//-1);
       }
    }
   else if (!be.isLeaf()) {
      int n = be.getChildCount();
      for (int i = 0; i < n; ++i) {
	 BaleElement ce = be.getBaleElement(i);
	 chng |= checkElement(ce);
       }
    }

   return chng;
}




/********************************************************************************/
/*										*/
/*	Document event handlers 						*/
/*										*/
/*		Note that this check is done later to ensure that the views	*/
/*	associated with the element buffer have been updated already. This	*/
/*	might be done in a separate thread after getting a read lock on the	*/
/*	document.								*/
/*										*/
/********************************************************************************/

@Override public void changedUpdate(DocumentEvent e)
{
   SwingUtilities.invokeLater(new LineChecker());
}


@Override public void insertUpdate(DocumentEvent e)
{
   update_needed = true;
   SwingUtilities.invokeLater(new LineChecker());
}


@Override public void removeUpdate(DocumentEvent e)
{
   update_needed = true;
   SwingUtilities.invokeLater(new LineChecker());
}



private class LineChecker implements Runnable {

   @Override public void run() {
      recheckLines();
    }

}	// end of inner class Runnable



/********************************************************************************/
/*										*/
/*	Editor event handlers							*/
/*										*/
/********************************************************************************/

private class CheckUpdater extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e)	{ update_needed = true; }

}	// end of inner class CheckUpdater




/********************************************************************************/
/*										*/
/*	LineData class -- holder of information about a line			*/
/*										*/
/********************************************************************************/

private static class LineData {

   private int line_number;
   private int start_y;
   private int end_y;

   LineData(int lno,int y0,int y1) {
      set(lno,y0,y1);
    }

   boolean set(int lno,int y0,int y1) {
      if (lno == line_number && start_y == y0 && end_y == y1) return false;
      line_number = lno;
      start_y = y0;
      end_y = y1;
      return true;
    }

   int getTop() 		{ return start_y; }
   int getBottom()		{ return end_y; }

}	// end of inner class LineData




/********************************************************************************/
/*										*/
/*	Mouse handler								*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent evt) {
      int lno = findLine(evt);
      if (lno < 0 || !BALE_PROPERTIES.getBoolean("Bale.allow.breakpoints",true)) return;
      break_model.toggleBreakpoint(for_document.getProjectName(),for_document.getFile(),lno,BumpBreakMode.DEFAULT);
      BoardMetrics.noteCommand("BALE","ANNOT_TOGGLEBREAK");
    }

}	// end of interface Mouser



/********************************************************************************/
/*										*/
/*	Action for showing / hiding line numbers				*/
/*										*/
/********************************************************************************/

private class ShowLineAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ShowLineAction() {
      super(show_line_numbers ? "Hide Line Numbers" : "Show Line Numbers");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      showLineNumbers(!show_line_numbers);
    }

}	// end of inner class ShowLineAction




}	// end of class BaleAnnotationArea




/* end of BaleAnnotationArea.java */

