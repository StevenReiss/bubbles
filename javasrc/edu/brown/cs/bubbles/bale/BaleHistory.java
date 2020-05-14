/********************************************************************************/
/*										*/
/*		BaleHistory.java						*/
/*										*/
/*	Bubble Annotated Language Editor definitions for history maintenance	*/
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

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.burp.BurpConstants.BurpEditDelta;
import edu.brown.cs.bubbles.burp.BurpConstants.BurpPlayableEdit;
import edu.brown.cs.bubbles.burp.BurpConstants.BurpRange;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.StyleConstants;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;



class BaleHistory {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final char [] empty = new char[0];




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Implementation of our content class					*/
/*										*/
/********************************************************************************/

static class BaleIdeContent extends GapContent {

   private final static long serialVersionUID = 1;

   BaleIdeContent(int len) {
      super(len);
    }

   @Override public UndoableEdit insertString(int where,String str) throws BadLocationException {
	if (where > length() || where < 0) {
	   throw new BadLocationException("Invalid insert " + where + " " + length(), where);
	 }
	char[] chars = str.toCharArray();
	replace(where, 0, chars, chars.length);
	return new BaleInsertUndo(this,where,str.length());
    }

   @Override public UndoableEdit remove(int where,int nitems) throws BadLocationException {
      if (where + nitems >= length()) {
	 throw new BadLocationException("Invalid remove", length() + 1);
       }
      String removedString = getString(where, nitems);
      UndoableEdit edit = new BaleRemoveUndo(this,where, removedString);
      replace(where, nitems, empty, 0);
      return edit;
    }

   Vector<?> getPositions(Vector<?> v,int off,int len) {
      return super.getPositionsInRange(v,off,len);
    }

   void updatePositions(Vector<?> pos,int off,int len) {
      super.updateUndoPositions(pos,off,len);
    }

}	// end of inner class BaleIdeContent



/********************************************************************************/
/*										*/
/*	Generic undo event for bale/burp					*/
/*										*/
/********************************************************************************/

static private abstract class BaleGenericUndo extends AbstractUndoableEdit
	implements BurpPlayableEdit {

   private Map<Object,Integer>	special_delta;

   private static final long serialVersionUID = 1;

   BaleGenericUndo() {
      special_delta = null;
    }

   protected int updatePositionDelta(Object edit,int offset,int delta,int opos,boolean start) {
      if (offset > opos || delta == 0) return opos;
      if (offset == opos) {
	 if (delta > 0 && edit != null && special_delta != null && special_delta.containsKey(edit)) {
	    int ndelta = special_delta.get(edit);
	    //if (ndelta != 0) BoardLog.logD("BALE","Update position special " + opos + "->" + (opos+ndelta) + " " + edit);
	    return opos + ndelta;
	 }
	 else return opos;
      }
      if (delta > 0) {			// insertion
	 if (edit != null && special_delta != null && special_delta.containsKey(edit)) {
	    int ndelta = special_delta.get(edit);
	    // if (ndelta != 0) BoardLog.logD("BALE","Update position special1 " + opos + "->" + (opos+ndelta) + " " + edit);
	    return opos + ndelta;
	  }
	 if (opos == offset && start) return opos;
       }
      else {				// deletion
	 if (offset - delta >= opos) {
	    if (edit != null) {
	       int ndelta = opos - offset;
	       if (special_delta == null) special_delta = new HashMap<Object,Integer>(2);
	       // BoardLog.logD("BALE","Special update " + opos + " " + ndelta + " " + edit);
	       Integer ov = special_delta.put(edit,ndelta);
	       if (ov != null && ov != ndelta) {
		  System.err.println("VALUE CHANGED " + this + " " + edit + " " + ndelta + " " + ov);
		}
	     }
	    delta = offset - opos;
	  }
       }
      // if (delta != 0)  BoardLog.logD("BALE","Update position " + opos + "->" + (opos+delta));

      return opos+delta;
    }

}	// end of inner class BaleGenericUndo




/********************************************************************************/
/*										*/
/*	Implementation of Insert edit						*/
/*										*/
/********************************************************************************/

static class BaleInsertUndo extends BaleGenericUndo {

   private BaleIdeContent the_content;
   private String ins_string;
   private int insert_offset;
   private int insert_length;
   protected Vector<?> pos_refs;

   private final static long serialVersionUID = 1;

   BaleInsertUndo(BaleIdeContent cnt,int offset,int length) {
      the_content = cnt;
      insert_offset = offset;
      insert_length = length;
       try {
	 ins_string = the_content.getString(offset,length);
	 // BoardLog.logD("BALE","INSERT " + offset + " " + length + " `" + ins_string + "'");
       }
      catch (BadLocationException e) { }
    }

   @Override public void undo() throws CannotUndoException {
      super.undo();
      try {
	 int start = insert_offset;
	 int len = insert_length;
	 // Get the Positions in the range being removed.
	 pos_refs = the_content.getPositions(null,start,len);
	 ins_string = the_content.getString(start,len);
	 the_content.remove(start,len);
       }
      catch (BadLocationException bl) {
	 throw new CannotUndoException();
       }
    }

   @Override public void redo() throws CannotRedoException {
      super.redo();
      try {
	 int start = insert_offset;
	 int len = insert_length;
	 the_content.insertString(start, ins_string);
	 ins_string = null;
	 // Update the Positions that were in the range removed.
	 if (pos_refs != null) {
	    the_content.updatePositions(pos_refs, start,len);
	    pos_refs = null;
	  }
       }
	 catch (BadLocationException bl) {
	    throw new CannotRedoException();
	  }
       }

   @Override public void playUndo(Document doc) throws CannotUndoException {
      try {
	 int start = insert_offset;
	 int len = insert_length;
	 // ins_string = the_content.getString(start,len);
	 doc.remove(start,len);
       }
      catch (BadLocationException bl) {
	 throw new CannotUndoException();
       }
    }

   @Override public boolean playUndo(Document doc,BurpRange rng) throws CannotUndoException {
      int offset = insert_offset;
      int eoffset = insert_offset+insert_length;
      int rstart = rng.getStartPosition();
      int rend = rng.getEndPosition();
      if (rstart > offset) offset = rstart;
      if (eoffset > rend) eoffset = rend;
      if (offset >= eoffset) return false;
      try {
	 doc.remove(offset,eoffset-offset);
       }
      catch (BadLocationException e) {
	 throw new CannotUndoException();
       }
      return true;
    }

   @Override public List<BurpEditDelta> getDeltas() {
      return Collections.singletonList(new BurpEditDelta(insert_offset,insert_length));
    }

   @Override public void updatePosition(Object edit,int pos,int delta) {
      insert_offset = updatePositionDelta(edit,pos,delta,insert_offset,true);
    }

}   // end of inner class BaleInsertUndo





/********************************************************************************/
/*										*/
/*	Implementation of Remove edit						*/
/*										*/
/********************************************************************************/

static class BaleRemoveUndo extends BaleGenericUndo {

   private BaleIdeContent the_content;
   private String rem_string;
   private int remove_offset;
   private int remove_length;
   private Vector<?> pos_refs;

   private final static long serialVersionUID = 1;

   BaleRemoveUndo(BaleIdeContent cnt,int offset, String string) {
      the_content = cnt;
      rem_string = string;
      remove_offset= offset;
      remove_length = string.length();
      pos_refs = the_content.getPositions(null,offset,remove_length);
      // BoardLog.logD("BALE","REMOVE " + offset + " `" + string + "'" + " " + string.length());
    }

   @Override public void undo() throws CannotUndoException {
      super.undo();
      try {
	 int start = remove_offset;
	 the_content.insertString(start, rem_string);
	 int len = remove_length;
	 // Update the Positions that were in the range removed.
	 if (pos_refs != null) {
	    the_content.updatePositions(pos_refs, start,len);
	    pos_refs = null;
	  }
	 rem_string = null;
       }
      catch (BadLocationException bl) {
	 throw new CannotUndoException();
       }
    }

   @Override public void redo() throws CannotRedoException {
      super.redo();
      try {
	 int start = remove_offset;
	 int len = remove_length;
	 rem_string = the_content.getString(start,len);
	 // Get the Positions in the range being removed.
	 pos_refs = the_content.getPositions(null, start,len);
	 the_content.remove(start,len);
       }
      catch (BadLocationException bl) {
	 throw new CannotRedoException();
       }
    }

   @Override public void playUndo(Document doc) throws CannotUndoException {
      try {
	 int start = remove_offset;
	 doc.insertString(start,rem_string,null);
       }
      catch (BadLocationException bl) {
	 throw new CannotUndoException();
       }
    }

   @Override public boolean playUndo(Document doc,BurpRange rng) throws CannotUndoException {
      int offset = remove_offset;
      int rstart = rng.getStartPosition();
      int rend = rng.getEndPosition();
      if (offset < rstart || offset >= rend) return false;
      try {
	 doc.insertString(offset,rem_string,null);
       }
      catch (BadLocationException e) {
	 throw new CannotUndoException();
       }
      return true;
    }

   @Override public void updatePosition(Object edit,int pos,int delta) {
      remove_offset = updatePositionDelta(edit,pos,delta,remove_offset,true);
    }

   @Override public List<BurpEditDelta> getDeltas() {
      return Collections.singletonList(new BurpEditDelta(remove_offset,-rem_string.length()));
    }

}     // end of inner class Bale.RemoveUndo



/********************************************************************************/
/*										*/
/*	Document class								*/
/*										*/
/********************************************************************************/

static abstract class BaleAbstractDocument extends BaleDocument {

   private static final long serialVersionUID = 1;

   BaleAbstractDocument() {
      super(new BaleIdeContent(1024));
    }

   @Override public void remove(int offs, int len) throws BadLocationException {
      writeLock();
      try {
	 baleHandleRemove(offs, len);
       }
      finally {
	 writeUnlock();
       }
    }

   private void baleHandleRemove(int offs, int len) throws BadLocationException {
      if (len > 0) {
	 if (offs < 0 || (offs + len) > getLength()) {
	    throw new BadLocationException("Invalid remove " + offs + " " + len + " " + getLength(), offs);
	  }
	 DefaultDocumentEvent chng = createDocumentEvent(offs,len,DocumentEvent.EventType.REMOVE);

	 boolean isComposedTextElement = false;
	 // Check whether the position of interest is the composed text
	 isComposedTextElement = isComposedTextElement(this, offs);

	 removeUpdate(chng);
	 UndoableEdit u = getContent().remove(offs, len);
	 if (u != null) {
	    chng.addEdit(u);
	  }
	 postRemoveUpdate(chng);
	 // Mark the edit as done.
	 chng.end();
	 fireRemoveUpdate(chng);
	 // only fire undo if Content implementation supports it
	 // undo for the composed text is not supported for now
	 if ((u != null) && !isComposedTextElement) {
	    fireUndoableEditUpdate(new UndoableEditEvent(this, chng));
	  }
       }
    }

   @Override public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      if ((str == null) || (str.length() == 0)) {
	 return;
       }
      writeLock();
      try {
	 baleHandleInsertString(offs, str, a);
       }
      finally {
	 writeUnlock();
       }
    }

   private void baleHandleInsertString(int offs, String str, AttributeSet a)
      throws BadLocationException {
      if ((str == null) || (str.length() == 0)) {
	 return;
       }
      UndoableEdit u = getContent().insertString(offs, str);
      DefaultDocumentEvent e =
	 createDocumentEvent(offs, str.length(), DocumentEvent.EventType.INSERT);
      if (u != null) {
	 e.addEdit(u);
       }

      insertUpdate(e, a);
      // Mark the edit as done.
      e.end();
      fireInsertUpdate(e);
      // only fire undo if Content implementation supports it
      // undo for the composed text is not supported for now
      if (u != null &&
	     (a == null || !a.isDefined(StyleConstants.ComposedTextAttribute))) {
	 fireUndoableEditUpdate(new UndoableEditEvent(this, e));
       }
    }

   private DefaultDocumentEvent createDocumentEvent(int off,int len,DocumentEvent.EventType type) {
      return new BaleDefaultDocumentEvent(off,len,type);
    }

   class BaleDefaultDocumentEvent extends DefaultDocumentEvent
      implements BurpPlayableEdit {

      private int evt_offset;
      private int evt_length;

      private final static long serialVersionUID = 1;

      BaleDefaultDocumentEvent(int offs, int len, DocumentEvent.EventType type) {
	 super(offs,len,type);
	 evt_offset = offs;
	 evt_length = len;
       }

      @Override public int getOffset() {
	 return evt_offset;
       }

      @Override public int getLength() {
	 return evt_length;
       }

      public Vector<UndoableEdit> getEdits() {
	 return edits;
       }

      @Override public void playUndo(Document doc) throws CannotUndoException {
	 writeLock();
	 try {
	    // change the state
	    for (UndoableEdit ued : getEdits()) {
	       if (!(ued instanceof BurpPlayableEdit)) throw new CannotUndoException();
	     }
	    for (UndoableEdit ued : getEdits()) {
	       BurpPlayableEdit bpe = (BurpPlayableEdit) ued;
	       bpe.playUndo(doc);
	     }
	  }
	 finally {
	    writeUnlock();
	  }
       }

      @Override public boolean playUndo(Document doc,BurpRange rng) throws CannotUndoException {
	 boolean chng = false;
	 writeLock();
	 try {
	    // change the state
	    for (UndoableEdit ued : getEdits()) {
	       if (!(ued instanceof BurpPlayableEdit)) throw new CannotUndoException();
	     }
	    for (UndoableEdit ued : getEdits()) {
	       BurpPlayableEdit bpe = (BurpPlayableEdit) ued;
	       chng |= bpe.playUndo(doc,rng);
	     }
	  }
	 finally {
	    writeUnlock();
	  }
	 return chng;
      }

      @Override public void updatePosition(Object edit,int pos,int len) {
	 for (UndoableEdit ued : getEdits()) {
	    if (ued instanceof BurpPlayableEdit) {
	       BurpPlayableEdit bpe = (BurpPlayableEdit) ued;
	       bpe.updatePosition(edit,pos,len);
	     }
	    else BoardLog.logX("BALE","History event not playable: " + ued);
	 }
       }

      @Override public List<BurpEditDelta> getDeltas() {
	 List<BurpEditDelta> rslt = null;
	 for (UndoableEdit ued : getEdits()) {
	    if (ued instanceof BurpPlayableEdit) {
	       BurpPlayableEdit bpe = (BurpPlayableEdit) ued;
	       List<BurpEditDelta> deltas = bpe.getDeltas();
	       if (deltas != null) {
		  if (rslt == null) rslt = deltas;
		  else rslt.addAll(deltas);
	       }
	    }
	    else BoardLog.logX("BALE","History event not playable: " + ued);
	 }
	 return rslt;
       }

    }	    // end of inner class BaleDefaultDocumentEvent

}	// end of inner class BaleAbstractDocument



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

static boolean isComposedTextElement(Document doc, int offset)
{
   Element elem = doc.getDefaultRootElement();
   while (!elem.isLeaf()) {
      elem = elem.getElement(elem.getElementIndex(offset));
    }
   return isComposedTextElement(elem);
}

static boolean isComposedTextElement(Element elem)
{
   AttributeSet as = elem.getAttributes();
   return isComposedTextAttributeDefined(as);
}


static boolean isComposedTextAttributeDefined(AttributeSet as)
{
   return ((as != null) && (as.isDefined(StyleConstants.ComposedTextAttribute)));
}



}	// end of class BaleHistory

