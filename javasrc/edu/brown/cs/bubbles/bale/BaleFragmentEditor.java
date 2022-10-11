/********************************************************************************/
/*										*/
/*		BaleFragmentEditor.java 					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor widget 		*/
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
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubblePosition;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.Position;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


class BaleFragmentEditor extends SwingGridPanel implements CaretListener, BaleConstants,
	BudaConstants.BudaBubbleOutputer, BumpConstants.BumpProblemHandler,
	BumpConstants.BumpBreakpointHandler, BaleConstants.BaleAnnotationListener,
	BudaConstants.Scalable, BaleConstants.BaleWindow
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private EditorPane	editor_pane;
private BaleViewport	editor_viewport;
private BaleAnnotationArea annot_area;
private BaleCrumbBar	crumb_bar;
private transient BaleFinder find_bar;
private BaleDocumentIde base_document;
private BaleFragmentType fragment_type;
private transient List<BaleRegion> fragment_regions;
private String		fragment_name;
private int		start_cline;
private int		end_cline;
private transient Map<BumpProblem,ProblemAnnot> problem_annotations;
private transient Map<BumpBreakpoint,BreakpointAnnot> breakpoint_annotations;
private transient Set<BaleAnnotation> document_annotations;
private boolean 	check_annotations;
private double		scale_factor;


private static final long serialVersionUID = 1;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleFragmentEditor(String proj,File file,String name,BaleDocumentIde fdoc,BaleFragmentType typ,
		      List<BaleRegion> regions)
{
   base_document = fdoc;
   fdoc.checkProjectName(proj);
   fragment_type = typ;
   fragment_regions = new ArrayList<>(regions);
   fragment_name = name;
   check_annotations = true;
   scale_factor = 1;

   setInsets(0);

   editor_pane = new EditorPane();
   editor_pane.addMouseListener(new BudaConstants.FocusOnEntry());

   crumb_bar = new BaleCrumbBar(editor_pane, fragment_name);
   annot_area = new BaleAnnotationArea(editor_pane);
   editor_viewport = new BaleViewport(editor_pane,annot_area);

   find_bar = new BaleFindReplaceBar(editor_pane,false);
   find_bar.getComponent().setVisible(false);

   problem_annotations = new HashMap<>();
   breakpoint_annotations = new HashMap<>();
   document_annotations = new HashSet<>();
   Icon help = BoardImage.getIcon(BALE_CRUMB_HELP_ICON);
   JLabel hlbl = new JLabel(help);
   hlbl.addMouseListener(new HelpMouser());
   BoardColors.setColors(hlbl,BALE_EDITOR_TOP_COLOR_PROP);
   hlbl.setOpaque(true);

   addGBComponent(crumb_bar,0,0,2,1,1,0);
   addGBComponent(hlbl,2,0,1,1,0,0);
   addGBComponent(editor_viewport,1,1,2,1,10,10);

   setComponentZOrder(find_bar.getComponent(),0);

   editor_pane.addCaretListener(this);

   BumpClient.getBump().addProblemHandler(fdoc.getFile(),this);
   BumpClient.getBump().addBreakpointHandler(fdoc.getFile(),this);
   BaleFactory.getFactory().addAnnotationListener(this);

   // new BaleCorrector(this,BALE_PROPERTIES.getBoolean("Bale.correct.spelling"));

   for (BumpProblem bp : BumpClient.getBump().getProblems(fdoc.getFile())) {
      handleProblemAdded(bp);
    }

   for (BumpBreakpoint bb : BumpClient.getBump().getBreakpoints(fdoc.getFile())) {
      handleBreakpointAdded(bb);
    }

   for (BaleAnnotation ba : BaleFactory.getFactory().getAnnotations(getDocument())) {
      annotationAdded(ba);
    }
}



void dispose()
{
   editor_pane.dispose();
   editor_pane.removeCaretListener(this);
   BumpClient.getBump().removeProblemHandler(this);
   BumpClient.getBump().removeBreakpointHandler(this);
   BaleFactory.getFactory().removeAnnotationListener(this);
   BaleFactory.getFactory().noteEditorRemoved(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BaleDocument getDocument()		{ return editor_pane.getBaleDocument(); }

@Override public BaleEditorPane getEditor()	{ return editor_pane; }

BaleFragmentType getFragmentType()	{ return fragment_type; }



/********************************************************************************/
/*										*/
/*	External BaleWindow interface						*/
/*										*/
/********************************************************************************/

@Override public BaleWindowDocument getWindowDocument()
{
   return getDocument();
}

@Override public void addCaretListener(CaretListener cl)
{
   getEditor().addCaretListener(cl);
}

@Override public void removeCaretListener(CaretListener cl)
{
   getEditor().removeCaretListener(cl);
}

@Override public BudaBubble getBudaBubble()
{
   return BudaRoot.findBudaBubble(this);
}


@Override public Collection<String> getKeywords()
{
   Collection<String> rslt =  BaleTokenizer.getKeywords(base_document.getLanguage());

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Handle initial sizing							*/
/*										*/
/********************************************************************************/

void setInitialSize(Dimension d)
{
   Dimension eps = editor_pane.getPreferredSize();
   BaleDocument bd = getDocument();

   eps.width += BALE_DELTA_EDITING_WIDTH;
   
   Dimension eps0 = new Dimension(eps);
   
   if (eps.height > d.height || eps.width > d.width) {
      // bd.recheckElisions();
      editor_pane.setSize(d);
      eps = editor_pane.getPreferredSize();
   }
   
   if (eps0.height > 400 && eps.height < 50) {
      String nm = "RemoveCodeElisionAction";    // RemoveElisionAction
      Action act = BaleEditorKit.findAction(nm);
      if (act != null) {
         ActionEvent evt = new ActionEvent(editor_pane,0,nm);
         act.actionPerformed(evt);
       }
      eps.height = Math.min(800,eps0.height);
    }

   eps.width += BALE_ANNOT_NUMBER_WIDTH;
   
   bd.baleReadLock();
   try {
      Dimension cbs = crumb_bar.getPreferredSize();
      if (cbs.width > eps.width) {
         cbs.width = eps.width;
         crumb_bar.setSize(cbs);
         crumb_bar.setPreferredSize(cbs);
       }
    }
   finally {
      bd.baleReadUnlock();
    }

   bd.baleWriteLock();
   try {
      editor_viewport.setPreferredSize(eps);
      invalidate();
      Dimension xps = getPreferredSize();
      setSize(xps);
      bd.fixElisions();
    }
   finally {
      bd.baleWriteUnlock();
    }
}



/********************************************************************************/
/*										*/
/*	Handle scaling								*/
/*										*/
/********************************************************************************/

@Override public void setScaleFactor(double sf)
{
   if (scale_factor == sf) return;
   scale_factor = sf;
   //annot_area.setScaleFactor(sf);
   //crumb_bar.setScaleFactor(sf);
   //find_bar.setScaleFactor(sf);
   editor_pane.setScaleFactor(sf);
}



/********************************************************************************/
/*										*/
/*	Handle caret events							*/
/*										*/
/********************************************************************************/

@Override public void caretUpdate(CaretEvent evt)
{
   int pos0 = evt.getDot();
   int pos1 = evt.getMark();
   if (pos0 > pos1) {
      int t = pos0;
      pos0 = pos1;
      pos1 = t;
    }

   BaleDocumentFragment bd = (BaleDocumentFragment) editor_pane.getDocument();

   int ln0 = bd.findLineNumber(pos0);
   int ln1 = bd.findLineNumber(pos1);
   if (ln0 == start_cline && ln1 == end_cline) return;

   int soff = bd.findLineOffset(ln0);
   int eoff = bd.findLineOffset(ln1+1)-1;
   bd.setCursorRegion(soff,eoff-soff);
}



/********************************************************************************/
/*										*/
/*	Handle context menu							*/
/*										*/
/********************************************************************************/

void handleContextMenu(MouseEvent e)
{
   Point p = new Point(e.getXOnScreen(),e.getYOnScreen());
   SwingUtilities.convertPointFromScreen(p,this);
   Component c = SwingUtilities.getDeepestComponentAt(this,p.x,p.y);
   while (c != null) {
      if (c == crumb_bar) {
	 convertMouseEvent(e,p,c);
	 crumb_bar.handleContextMenu(e);
	 break;
       }
      else if (c == find_bar) {
	 convertMouseEvent(e,p,c);
	 break;
       }
      else if (c == annot_area) {
	 convertMouseEvent(e,p,c);
	 annot_area.handleContextMenu(e);
	 break;
       }
      else if (c == editor_pane || c == this) {
	 convertMouseEvent(e,p,c);
	 editor_pane.handleContextMenu(e);
	 break;
       }
      c = c.getParent();
    }
}


private void convertMouseEvent(MouseEvent e,Point p,Component c)
{
   Point pt = SwingUtilities.convertPoint(this,p,c);
   e.translatePoint(pt.x - e.getX(),pt.y - e.getY());
}



private class HelpMouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent evt) {
      Component c = (Component) evt.getSource();
      int ht = c.getHeight();
      if (c.getWidth() - evt.getX() + evt.getY() <= ht) {
	 BudaRoot.showHelp(evt);
      }
   }

}	// end of inner class Mouser





/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   if (check_annotations) checkInitialAnnotations();

//   if (scale_factor == 1 || scale_factor == 0) {
//	super.paint(g);
//   }
//   else {
//	Graphics2D g1 = (Graphics2D) g.create();
//	g1.scale(scale_factor,scale_factor);
//	g1.setClip(0,0,getWidth(),getHeight());
//	super.paint(g1);
//   }

   super.paint(g);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BALE"; }


@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","FRAGMENT");
   xw.field("FRAGTYPE",fragment_type);
   xw.field("FILE",base_document.getFile().getPath());
   xw.field("PROJECT",base_document.getProjectName());
   String fnm = getDocument().getFragmentName();
   if (fnm != null) fragment_name = fnm;
   xw.field("NAME",fragment_name);
   getDocument().outputElisions(xw);
}



/********************************************************************************/
/*										*/
/*	Methods to handle problems relevant to this fragment			*/
/*										*/
/********************************************************************************/

@Override public void handleProblemAdded(BumpProblem bp)
{
   BaleDocument bd = getDocument();
   int soff = bd.mapOffsetToJava(bp.getStart());

   if (soff >= 0) {
      try {
	 ProblemAnnot pa = new ProblemAnnot(bp,bd,bd.createPosition(soff));
	 problem_annotations.put(bp,pa);
	 annot_area.addAnnotation(pa);
       }
      catch (BadLocationException e) { }
    }
}



@Override public void handleProblemRemoved(BumpProblem bp)
{
   ProblemAnnot pa = problem_annotations.remove(bp);
   if (pa != null) annot_area.removeAnnotation(pa);
}

@Override public void handleClearProblems()
{
   for (ProblemAnnot pa : problem_annotations.values()) {
      annot_area.removeAnnotation(pa);
    }
   problem_annotations.clear();
}

@Override public void handleProblemsDone()
{ }




/********************************************************************************/
/*										*/
/*	Methods to handle breakpoints for the fragment				*/
/*										*/
/********************************************************************************/

@Override public void handleBreakpointAdded(BumpBreakpoint bb)
{
   BaleDocument bd = getDocument();
   int lno = bb.getLineNumber();
   if (lno < 0) return;

   int soff = bd.findLineOffset(lno);

   if (soff >= 0) {
      try {
	 BreakpointAnnot ba = new BreakpointAnnot(bb,bd,bd.createPosition(soff));
	 breakpoint_annotations.put(bb,ba);
	 annot_area.addAnnotation(ba);
       }
      catch (BadLocationException e) { }
    }
}



@Override public void handleBreakpointRemoved(BumpBreakpoint bb)
{
   BreakpointAnnot ba = breakpoint_annotations.get(bb);
   if (ba != null) annot_area.removeAnnotation(ba);
}



@Override public void handleBreakpointChanged(BumpBreakpoint bb)
{
   BreakpointAnnot ba = breakpoint_annotations.get(bb);
   if (ba != null) {
      annot_area.removeAnnotation(ba);
      annot_area.addAnnotation(ba);
    }
}




/********************************************************************************/
/*										*/
/*	Editor Pane specializations						*/
/*										*/
/********************************************************************************/

private class EditorPane extends BaleEditorPane implements BaleEditor {

   private static final long serialVersionUID = 1;

   EditorPane() { }

   @Override protected EditorKit createDefaultEditorKit() {
      return new BaleEditorKit.FragmentKit(base_document,fragment_type,fragment_regions);
    }

   @Override public BaleFinder getFindBar()			{ return find_bar; }
   @Override public BaleAnnotationArea getAnnotationArea()	{ return annot_area; }

   @Override void toggleFindBar() {
      Component finder = find_bar.getComponent();
      BudaBubble bb = BudaRoot.findBudaBubble(finder);
      if (finder.isVisible()) {
	 if (bb != null && bb.isVisible()) {
	    finder.setVisible(false);
	    return;
	 }
       }

      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      BudaBubble bbx = BudaRoot.findBudaBubble(this);
      if (bba == null || bbx == null) return;
      Rectangle bounds = bbx.getBounds();
      int findwidth = finder.getWidth();
      bounds.x = bounds.x + (bounds.width/2) - (findwidth/2);
      bba.add(finder, new BudaConstraint(BudaBubblePosition.FIXED, bounds.x, bounds.y+bounds.height));
      if (bb != null) bba.setLayer(bb, 1);
      finder.setVisible(true);
    }

   void setScaleFactor(double sf) {
      BaleDocument bd = getBaleDocument();
      if (bd instanceof BaleDocumentFragment) {
          BaleDocumentFragment bf = (BaleDocumentFragment) bd;
          bf.setScaleFactor(sf);
       }
      bd.baleWriteLock();
      try {
         bd.reportEvent(bd, 0, bd.getLength(), DocumentEvent.EventType.CHANGE, null, null);
      }
      finally {
         bd.baleWriteUnlock();
      }
    }

}	// end of inner class EditorPane




/********************************************************************************/
/*										*/
/*	FindBar methods 							*/
/*										*/
/********************************************************************************/

void hideFindBar() {
   if (find_bar.getComponent().isVisible()) editor_pane.toggleFindBar();
}



void relocateFindBar()
{
   Component finder = find_bar.getComponent();
   if (finder.isVisible()) {
      BudaBubble bb = BudaRoot.findBudaBubble(this);
      if (bb == null) return;
      Rectangle bounds = bb.getBounds();
      int findwidth = finder.getWidth();
      bounds.x = bounds.x + (bounds.width/2) - (findwidth/2);
      BudaBubble findbubble = BudaRoot.findBudaBubble(finder);
      if (findbubble == null) return;
      findbubble.setLocation(bounds.x, bounds.y + bounds.height);
   }
}



/********************************************************************************/
/*										*/
/*	Annotation methods for outside (document) annotations			*/
/*										*/
/********************************************************************************/

@Override public void annotationAdded(BaleAnnotation ba)
{
   if (ba.getFile() == null) return;
   if (!ba.getFile().equals(getDocument().getFile())) return;

   int fragoffset = getDocument().getFragmentOffset(ba.getDocumentOffset());
   if (fragoffset < 0) return;

   synchronized (document_annotations) {
      document_annotations.add(ba);
    }
   annot_area.addAnnotation(ba);
   BudaBubble bb = BudaRoot.findBudaBubble(this);
   if (bb == null) return;

   if (ba.getForceVisible(bb)) {
      try {
	 Position p0 = getDocument().createPosition(fragoffset);
	 SwingUtilities.invokeLater(new ForceVisible(p0));
       }
      catch (BadLocationException e) { }
    }

   if (ba.getLineColor(bb) != null) repaint();
}



@Override public void annotationRemoved(BaleAnnotation ba)
{
   synchronized (document_annotations) {
      if (!document_annotations.contains(ba)) return;
    }
   annot_area.removeAnnotation(ba);
}



void checkInitialAnnotations()
{
   if (!check_annotations) return;
   check_annotations = false;

   BudaBubble bb = BudaRoot.findBudaBubble(this);
   if (bb == null) return;

   synchronized (document_annotations) {
      for (BaleAnnotation ba : document_annotations) {
	 int fragoffset = getDocument().getFragmentOffset(ba.getDocumentOffset());
	 if (fragoffset < 0) continue;
	 if (ba.getForceVisible(bb)) {
	    try {
	       Position p0 = getDocument().createPosition(fragoffset);
	       SwingUtilities.invokeLater(new ForceVisible(p0));
	     }
	    catch (BadLocationException e) { }
	  }
       }
    }
}



private class ForceVisible implements Runnable {

   private Position for_position;

   ForceVisible(Position p) {
      for_position = p;
    }

   @Override public void run() {
      int fragoffset = for_position.getOffset();
      BoardLog.logD("BALE","Force visible " + fragoffset);
      if (fragoffset < 0) return;
   
      boolean chng = false;
      for ( ; ; ) {
         BaleElement be = getDocument().getCharacterElement(fragoffset);
         if (be == null || !be.isElided()) break;
         be.setElided(false);
         BoardLog.logD("BALE","Unelide");
         chng = true;
       }
      if (chng) {
         BaleDocument bd = getDocument();
         bd.handleElisionChange();
         repaint();
         SwingUtilities.invokeLater(this);
         return;
       }
   
      try {
         Rectangle r = SwingText.modelToView2D(editor_pane,fragoffset);
         if (r != null) {
            editor_pane.scrollRectToVisible(r);
            BoardLog.logD("BALE","Scroll to visible " + r);
          }
       }
      catch (BadLocationException e) { }
      repaint();
    }

}	// end of inner class ForceVisible



/********************************************************************************/
/*										*/
/*	Debugging Routines							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return super.toString() + " " + fragment_name + " " + hashCode();
}




/********************************************************************************/
/*										*/
/*	Problem annotations							*/
/*										*/
/********************************************************************************/

private static class ProblemAnnot implements BaleAnnotation {

   private BumpProblem for_problem;
   private BaleDocument for_document;
   private Position error_pos;

   ProblemAnnot(BumpProblem bp,BaleDocument bd,Position p) {
      for_problem = bp;
      for_document = bd;
      error_pos = p;
    }

   @Override public File getFile()		{ return for_document.getFile(); }
   @Override public int getDocumentOffset() {
      return for_document.getDocumentOffset(error_pos.getOffset());
    }

   @Override public Icon getIcon(BudaBubble bbl) {
      switch (for_problem.getErrorType()) {
         case FATAL :
         case ERROR :
            return BoardImage.getIcon("error");
         case WARNING :
            return BoardImage.getIcon("warning");
         case NOTICE :
            return BoardImage.getIcon("notice");
       }
      return null;
    }

   @Override public String getToolTip() {
      return IvyXml.htmlSanitize(for_problem.getMessage());
    }

   @Override public Color getLineColor(BudaBubble bbl)		{ return null; }
   @Override public Color getBackgroundColor()			{ return null; }
   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }
   @Override public int getPriority()				{ return 10; }
   @Override public void addPopupButtons(Component c,JPopupMenu m) {
      m.add(new BaleFactory.QuickFix(c,for_problem));
   }

}	// end of inner class ProblemAnnot




/********************************************************************************/
/*										*/
/*	Breakpoint annotations							*/
/*										*/
/********************************************************************************/

private static class BreakpointAnnot implements BaleAnnotation {

   private BumpBreakpoint for_breakpoint;
   private BaleDocument for_document;
   private Position break_pos;

   BreakpointAnnot(BumpBreakpoint bb,BaleDocument bd,Position p) {
      for_breakpoint = bb;
      for_document = bd;
      break_pos = p;
    }

   @Override public File getFile()		{ return for_document.getFile(); }
   @Override public int getDocumentOffset() {
      return for_document.getDocumentOffset(break_pos.getOffset());
    }

   @Override public Icon getIcon(BudaBubble bbl) {
      // TODO: different images if enabled/disabled/conditional/...
      boolean enable = for_breakpoint.getBoolProperty("ENABLED");
      boolean trace = for_breakpoint.getBoolProperty("TRACEPOINT");
      if (!enable) return BoardImage.getIcon("breakdisable");
      else if (trace) return BoardImage.getIcon("trace");
   
      return BoardImage.getIcon("break");
    }

   @Override public String getToolTip() {
      // TODO: this should be more meaningful
      boolean enable = for_breakpoint.getBoolProperty("ENABLED");
      boolean trace = for_breakpoint.getBoolProperty("TRACEPOINT");
      int line = for_breakpoint.getLineNumber();
      String id = (trace ? "Tracepoint" : "Breakpoint");
      if (!enable) id = "Disabled " + id;
      if (line > 0) id += " at line " + line;
      return id;
    }

   @Override public Color getLineColor(BudaBubble bbl)		{ return null; }
   @Override public Color getBackgroundColor()			{ return null; }
   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }
   @Override public int getPriority()				{ return 5; }
   @Override public void addPopupButtons(Component c,JPopupMenu m) { }

}	// end of inner class BreakpointAnnot








}	// end of class BaleFragmentEditor




/* end of BaleFragmentEditor.java */



