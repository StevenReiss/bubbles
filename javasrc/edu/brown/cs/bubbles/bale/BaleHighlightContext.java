/********************************************************************************/
/*										*/
/*		BaleHighlightContext.java					*/
/*										*/
/*	Bubble Annotated Language Editor highlighting context			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bale;


import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;



class BaleHighlightContext implements BaleConstants, CaretListener, DocumentListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Collection<BaleEditorPane>	for_editors;
private Timer				our_timer;
private TimerTask			wait_task;
private CaretEvent			caret_event;
private int				start_counter;
private BumpClient			bump_client;
private Set<BaleHighlightType>		active_types;
private long				highlight_delay;

private static boolean		split_identifier = true;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleHighlightContext()
{
   for_editors = new HashSet<>();
   our_timer = new Timer("HighlightTimer");
   wait_task = null;
   caret_event = null;
   start_counter = -1;
   bump_client = BumpClient.getBump();
   active_types = EnumSet.noneOf(BaleHighlightType.class);
   highlight_delay = BALE_PROPERTIES.getLong(BALE_HIGHLIGHT_DELAY,300);
}



/********************************************************************************/
/*										*/
/*	Editor association methods						*/
/*										*/
/********************************************************************************/

synchronized void addEditor(BaleEditorPane ed)
{
   if (for_editors.add(ed)) {
      ed.addCaretListener(this);
      ed.getBaleDocument().addDocumentListener(this);
    }
}



synchronized void removeEditor(BaleEditorPane ed)
{
   if (for_editors.remove(ed)) {
      ed.removeCaretListener(this);
      ed.getBaleDocument().removeDocumentListener(this);
      if (caret_event != null && caret_event.getSource() == ed) caret_event = null;
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

static Highlighter.HighlightPainter getPainter(BaleHighlightType typ)
{
   switch (typ) {
      case IDENTIFIER :
	 return new IdentifierPainter();
      case IDENTIFIER_WRITE :
	 return new IdentifierWritePainter();
      case IDENTIFIER_DEFINE :
	 return new IdentifierDefinePainter();
      case BRACKET :
	 return new BracketPainter();
      case FIND :
	 return new FindPainter();
      default :
	 break;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Caret event management							*/
/*										*/
/********************************************************************************/

@Override synchronized public void caretUpdate(CaretEvent e)
{
   if (wait_task != null) {
      caret_event = null;
      wait_task.cancel();
      wait_task = null;
    }

   BaleEditorPane edt = (BaleEditorPane) e.getSource();
   BaleDocument doc = edt.getBaleDocument();
   doc.baleReadLock();
   try {
      if (wait_task == null && isStartable(e)) {
	 caret_event = e;
	 start_counter = doc.getEditCounter();
	 wait_task = new WaitTimer(caret_event);
	 our_timer.schedule(wait_task,highlight_delay);
       }
    }
   finally { doc.baleReadUnlock(); }
}




synchronized private void handleWaitDone(CaretEvent start)
{
   if (start == caret_event) {
      DataGetter dg = new DataGetter(start);
      BoardThreadPool.start(dg);
    }
   wait_task = null;
}



private boolean isStartable(CaretEvent e)
{
   BaleEditorPane edt = (BaleEditorPane) e.getSource();
   BaleDocument doc = edt.getBaleDocument();

   BaleElement leaf = doc.getCharacterElement(e.getDot());
   if (leaf == null) return false;
   if (e.getMark() != e.getDot()) {
      BaleElement l1 = doc.getCharacterElement(e.getMark());
      if (l1 != leaf) return false;
    }
   else if (leaf.getTokenType() == BaleTokenType.SPACE) {
      BaleElement prev = leaf.getPreviousCharacterElement();
      if (prev != null && prev.getEndOffset() == e.getDot()) leaf = prev;
   }

   switch (leaf.getTokenType()) {
      case IDENTIFIER :
      case LBRACE :
      case RBRACE :
      case LPAREN :
      case RPAREN :
      case LBRACKET :
      case RBRACKET :
      case LANGLE :
      case RANGLE :
	 break;
      default :
	 return false;
    }

   return true;
}



private class WaitTimer extends TimerTask {

   private CaretEvent start_event;

   WaitTimer(CaretEvent e) {
      start_event = e;
    }

   @Override public void run() {
      handleWaitDone(start_event);
    }

}	// end of inner class WaitTimer




/********************************************************************************/
/*										*/
/*	Methods to find the set of highlights					*/
/*										*/
/********************************************************************************/

private void handleHighlight(CaretEvent start)
{
   BaleEditorPane edt = (BaleEditorPane) start.getSource();
   BaleDocument doc = edt.getBaleDocument();

   doc.baleReadLock();
   try {
      startHighlight(start);
    }
   finally { doc.baleReadUnlock(); }
}




private void startHighlight(CaretEvent start)
{
   if (start != caret_event) return;

   BaleEditorPane edt = (BaleEditorPane) start.getSource();
   BaleDocument doc = edt.getBaleDocument();
   if (doc.getEditCounter() != start_counter) return;

   BaleElement leaf = doc.getCharacterElement(start.getDot());
   if (leaf == null) return;

   int spos = doc.mapOffsetToEclipse(start.getDot());
   int epos = doc.mapOffsetToEclipse(start.getMark());
   if (epos < spos) {
      int x = epos;
      epos = spos;
      spos = x;
    }
   if (epos == spos && leaf.getTokenType() == BaleTokenType.SPACE) {
      BaleElement prev = leaf.getPreviousCharacterElement();
      if (prev.getEndOffset() == start.getDot()) leaf = prev;
   }


   switch (leaf.getTokenType()) {
      case IDENTIFIER :
	 List<BumpLocation> locsr = null;
	 Collection<BumpLocation> locsw = null;
	 Collection<BumpLocation> locsd = null;

	 if (split_identifier) {
	    locsr = bump_client.findRWReferences(doc.getProjectName(),
						    doc.getFile(),spos,epos,false,2000);
	    BumpSymbolType styp = BumpSymbolType.UNKNOWN;
	    if (locsr != null && locsr.size() > 0) {
	       BumpLocation loc = locsr.get(0);
	       styp = loc.getSourceType();
	       if (styp == BumpSymbolType.UNKNOWN) styp = loc.getSymbolType();
	     }
	    if (locsr != null && styp != BumpSymbolType.FUNCTION) removeDefs(locsr);
	    switch (styp) {
	       case UNKNOWN :
		  locsw = bump_client.findReferences(null,doc.getFile(),spos,epos,2000);
		  break;
	       case FIELD :
	       case LOCAL :
               case GLOBAL :
		  locsw = bump_client.findRWReferences(null,doc.getFile(),spos,epos,true,1000);
		  locsd = bump_client.findDefinition(null,doc.getFile(),spos,epos,1000);
		  break;
	       case CLASS :
	       case THROWABLE :
	       case ENUM :
	       case INTERFACE :
		  locsd = bump_client.findDefinition(null,doc.getFile(),spos,epos,2000);
		  removeDefs(locsd);
		  break;
               case MODULE :
                  break;
               default :
        	  break;
	     }
	  }
	 else {
	    locsr = bump_client.findReferences(null,doc.getFile(),spos,epos);
	  }
	 if (start != caret_event) return;
	 if (doc.getEditCounter() != start_counter) return;
	 if (locsr != null || locsw != null || locsd != null) {
	    HighlightCreator hc = new HighlightCreator(start,start_counter,locsr,locsw,locsd);
	    SwingUtilities.invokeLater(hc);
	  }
	 break;

      case LBRACE :
      case LPAREN :
      case LBRACKET :
	 BaleElement.Leaf me = findNextMatch(leaf);
	 if (leaf != null && me != null) {
	    BracketHighlightCreator bhc = new BracketHighlightCreator(leaf,me,start_counter);
	    SwingUtilities.invokeLater(bhc);
	 }
	 break;
      case RBRACE :
      case RPAREN :
      case RBRACKET :
	 me = findPriorMatch(leaf);
	 if (leaf != null && me != null) {
	    BracketHighlightCreator bhc = new BracketHighlightCreator(me,leaf,start_counter);
	    SwingUtilities.invokeLater(bhc);
	 }
	 break;
      case LANGLE :
      case RANGLE :
	 // TODO: handle find matching brace
	 break;
      default :
	 break;
    }
}



private void removeDefs(Collection<BumpLocation> locs)
{
   if (locs == null) return;

   for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
      BumpLocation loc = it.next();
      if (loc.getSourceType() == BumpSymbolType.UNKNOWN) it.remove();
    }
}



private class DataGetter implements Runnable {

   private CaretEvent start_event;

   DataGetter(CaretEvent ce) {
      start_event = ce;
    }

   @Override public void run() {
      handleHighlight(start_event);
    }

   @Override public String toString() {
      return "BALE_HighlightDataGetter_" + start_event.getDot() + "_" + start_event.getMark();
    }

}	// end of inner class DataGetter



private class HighlightCreator implements Runnable {

   private CaretEvent start_event;
   private int edit_counter;
   private Collection<BumpLocation> read_locs;
   private Collection<BumpLocation> write_locs;
   private Collection<BumpLocation> def_locs;

   HighlightCreator(CaretEvent start,int ctr,
		       Collection<BumpLocation> r,
		       Collection<BumpLocation> w,
		       Collection<BumpLocation> d) {
      start_event = start;
      edit_counter = ctr;
      read_locs = r;
      write_locs = w;
      def_locs = d;
    }

   @Override public void run() {
      BaleEditorPane edt = (BaleEditorPane) start_event.getSource();
      BaleDocument doc = edt.getBaleDocument();

      doc.baleReadLock();
      try {
	 if (doc.getEditCounter() != edit_counter) return;
	 if (read_locs != null)
	    createHighlights(start_event,BaleHighlightType.IDENTIFIER,read_locs);
	 if (write_locs != null)
	    createHighlights(start_event,BaleHighlightType.IDENTIFIER_WRITE,write_locs);
	 if (def_locs != null)
	    createHighlights(start_event,BaleHighlightType.IDENTIFIER_DEFINE,def_locs);
       }
      finally { doc.baleReadUnlock(); }
    }

}	// end of inner class HighlightCreator




private class BracketHighlightCreator implements Runnable {

   private BaleElement start_element;
   private BaleElement end_element;
   private int edit_counter;

   BracketHighlightCreator(BaleElement s,BaleElement e,int ctr) {
      start_element = s;
      end_element = e;
      edit_counter = ctr;
   }

   @Override public void run() {
      if (start_element == null || end_element == null) return;
      BaleDocument doc = (BaleDocument) start_element.getDocument();
      doc.baleReadLock();
      try {
	 if (doc.getEditCounter() != edit_counter) return;
	 createBracketHighlights(start_element,end_element);
      }
      finally { doc.baleReadUnlock(); }
   }

}	// end of inner class BracketHighlightCreator





/********************************************************************************/
/*										*/
/*	Bracket highlighting support						*/
/*										*/
/********************************************************************************/

private BaleElement.Leaf findNextMatch(BaleElement open)
{
   BaleTokenType rtyp = null;

   switch (open.getTokenType()) {
      case LBRACE :
	 rtyp = BaleTokenType.RBRACE;
	 break;
      case LBRACKET :
	 rtyp = BaleTokenType.RBRACKET;
	 break;
      case LPAREN :
	 rtyp = BaleTokenType.RPAREN;
	 break;
      case LANGLE :
	 rtyp = BaleTokenType.RANGLE;
	 break;
      default :
	 break;
    }

   int lvl = 0;

   for (BaleElement.Leaf nxt = open.getNextCharacterElement();
	nxt != null;
	nxt = nxt.getNextCharacterElement()) {
      switch (nxt.getTokenType()) {
	 case LBRACE :
	 case LPAREN :
	 case LBRACKET :
	    ++lvl;
	    break;
	 case RBRACE :
	 case RPAREN :
	 case RBRACKET :
	    if (lvl > 0) --lvl;
	    else if (nxt.getTokenType() == rtyp) return nxt;
	    else return null;
	    break;
	 default :
	    break;
       }
    }

   return null;
}



private BaleElement.Leaf findPriorMatch(BaleElement open)
{
   BaleTokenType rtyp = null;

   switch (open.getTokenType()) {
      case RBRACE :
	 rtyp = BaleTokenType.LBRACE;
	 break;
      case RBRACKET :
	 rtyp = BaleTokenType.LBRACKET;
	 break;
      case RPAREN :
	 rtyp = BaleTokenType.LPAREN;
	 break;
      case RANGLE :
	 rtyp = BaleTokenType.LANGLE;
	 break;
      default :
	 break;
    }

   int lvl = 0;

   for (BaleElement.Leaf nxt = open.getPreviousCharacterElement();
	nxt != null;
	nxt = nxt.getPreviousCharacterElement()) {
      switch (nxt.getTokenType()) {
	 case LBRACE :
	 case LPAREN :
	 case LBRACKET :
	    if (lvl > 0) --lvl;
	    else if (nxt.getTokenType() == rtyp) return nxt;
	    else return null;
	    break;
	 case RBRACE :
	 case RPAREN :
	 case RBRACKET :
	    ++lvl;
	    break;
	 default :
	    break;
       }
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Methods to set up highlights						*/
/*										*/
/********************************************************************************/

private synchronized void createHighlights(CaretEvent start,BaleHighlightType typ,Collection<BumpLocation> locs)
{
   removeHighlights(typ);
   removeHighlights(BaleHighlightType.BRACKET);

   if (locs != null) {
      boolean add = false;
      for (BumpLocation loc : locs) {
	 for (BaleEditorPane bp : for_editors) {
	    BaleDocument doc = bp.getBaleDocument();
	    BaleRegion rgn = doc.getRegionFromLocation(loc);
	    if (rgn != null) {
	       bp.addHighlight(typ,rgn);
	       add = true;
	     }
	  }
       }
      if (add) active_types.add(typ);
    }
}





private void removeHighlights(BaleHighlightType typ)
{
   if (active_types.remove(typ)) {
      for (BaleEditorPane bp : for_editors) {
	 bp.removeHighlights(typ);
      }
   }
}




private synchronized void createBracketHighlights(BaleElement start,BaleElement end)
{
   removeHighlights(BaleHighlightType.IDENTIFIER);
   removeHighlights(BaleHighlightType.IDENTIFIER_WRITE);
   removeHighlights(BaleHighlightType.IDENTIFIER_DEFINE);
   removeHighlights(BaleHighlightType.BRACKET);

   if (start == null || end == null) return;

   boolean add = false;
   for (BaleEditorPane bp : for_editors) {
      BaleDocument doc = bp.getBaleDocument();
      if (doc == start.getDocument()) {
	  BaleRegion rgn = new BaleRegion(start.getStartPosition(),end.getEndPosition());
	  bp.addHighlight(BaleHighlightType.BRACKET,rgn);
	 add = true;
      }
   }
   if (add) active_types.add(BaleHighlightType.BRACKET);
}






private synchronized void removeHighlights()
{
   for (BaleHighlightType typ : active_types) {
      for (BaleEditorPane bp : for_editors) {
	 bp.removeHighlights(typ);
       }
    }

   active_types.clear();
}



/********************************************************************************/
/*										*/
/*	Document listener methods						*/
/*										*/
/********************************************************************************/

@Override public void changedUpdate(DocumentEvent e)			{ }



@Override public void insertUpdate(DocumentEvent e)
{
   removeHighlights();
}


@Override public void removeUpdate(DocumentEvent e)
{
   removeHighlights();
}


/********************************************************************************/
/*										*/
/*	Painter classes for different highlighting types			*/
/*										*/
/********************************************************************************/

private static class IdentifierPainter extends DefaultHighlighter.DefaultHighlightPainter {

   IdentifierPainter() {
      super(BoardColors.getColor(BALE_IDENTIFIER_HIGHLIGHT_COLOR_PROP));
    }

}	// end of inner class IdentifierPainter





private static class IdentifierWritePainter extends DefaultHighlighter.DefaultHighlightPainter {

   IdentifierWritePainter() {
      super(BoardColors.getColor(BALE_IDENTIFIER_WRITE_HIGHLIGHT_COLOR_PROP));
    }

}	// end of inner class IdentifierWritePainter





private static class IdentifierDefinePainter extends DefaultHighlighter.DefaultHighlightPainter {

   IdentifierDefinePainter() {
      super(BoardColors.getColor(BALE_IDENTIFIER_DEFINE_HIGHLIGHT_COLOR_PROP));
    }

}	// end of inner class IdentifierDefinePainter





private static class BracketPainter extends DefaultHighlighter.DefaultHighlightPainter {

   BracketPainter() {
      super(BoardColors.getColor(BALE_BRACKET_HIGHLIGHT_COLOR_PROP));
    }

}	// end of inner class IdentifierPainter




private static class FindPainter extends DefaultHighlighter.DefaultHighlightPainter {

   FindPainter() {
      super(BoardColors.getColor(BALE_FIND_HIGHLIGHT_COLOR_PROP));
   }
}



}	// end of class BaleHighlightContext



/* end of BaleHighlightContext.java */



