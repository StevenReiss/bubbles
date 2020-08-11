/********************************************************************************/
/*										*/
/*		BaleDocumentEvent.java						*/
/*										*/
/*	Bubble Annotated Language Editor document event class			*/
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
import edu.brown.cs.bubbles.burp.BurpConstants.BurpSharedEdit;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import java.util.List;




class BaleDocumentEvent implements DocumentEvent, UndoableEdit, BaleConstants,
	BurpSharedEdit, BurpPlayableEdit
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleDocument base_document;
private int doc_offset;
private int edit_length;
private EventType event_type;
private UndoableEdit the_edit;
private BaleElementEvent element_event;
private Position doc_position;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleDocumentEvent(BaleDocument d,int off,int len,EventType et,UndoableEdit ed,BaleElementEvent ee)
{
   base_document = d;
   doc_offset = off;
   edit_length = len;
   event_type = et;
   the_edit = ed;
   element_event = ee;
   savePosition();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Document getDocument() 			{ return base_document; }
@Override public int getLength()				{ return edit_length; }
@Override public int getOffset()				{ return doc_offset; }
@Override public DocumentEvent.EventType getType()		{ return event_type; }
@Override public Document getBaseEditDocument()
{
   if (base_document == null) return null;

   return base_document.getBaseEditDocument();
}



UndoableEdit getEdit()
{
   if (the_edit == null) return null;

   return the_edit;
}


@Override public DocumentEvent.ElementChange getChange(Element e)
{
   if (element_event == null) return null;

   if (element_event.getElement() == e) return element_event;

   return null;
}



@Override public UndoableEdit getBaseEdit()
{
   if (the_edit == null) return null;

   if (the_edit instanceof BaleDocumentEvent) {
      BaleDocumentEvent bde = (BaleDocumentEvent) the_edit;
      return bde.getBaseEdit();
    }

   return the_edit;
}



/********************************************************************************/
/*										*/
/*	Undoable Edit methods							*/
/*										*/
/********************************************************************************/

@Override public boolean addEdit(UndoableEdit ed)
{
   if (the_edit == null) {
      the_edit = ed;
      return true;
    }
   else return the_edit.addEdit(ed);
}


@Override public boolean canRedo()
{
   if (the_edit == null) return false;
   return the_edit.canRedo();
}



@Override public boolean canUndo()
{
   if (the_edit == null) return false;
   return the_edit.canUndo();
}



@Override public void die()
{
   if (the_edit != null) {
      the_edit.die();
      the_edit = null;
    }
}


@Override public String getPresentationName()
{
   if (the_edit == null) return null;
   return the_edit.getPresentationName();
}



@Override public String getRedoPresentationName()
{
   if (the_edit == null) return null;
   return the_edit.getRedoPresentationName();
}



@Override public String getUndoPresentationName()
{
   if (the_edit == null) return null;
   return the_edit.getUndoPresentationName();
}



@Override public boolean isSignificant()
{
   if (the_edit == null) return false;
   return the_edit.isSignificant();
}



@Override public void redo()
{
   restorePosition();
   if (the_edit != null) {
      base_document.baleWriteLock();
      try {
	 the_edit.redo();
       }
      finally { base_document.baleWriteUnlock(); }
    }
}


@Override public boolean replaceEdit(UndoableEdit ed)
{
   if (the_edit == null) return false;
   return the_edit.replaceEdit(ed);
}


@Override public void undo()
{
   restorePosition();
   if (the_edit != null) {
      base_document.baleWriteLock();
      base_document.markChanged();
      try {
	 the_edit.undo();
       }
      catch (CannotUndoException e) { }
      finally { base_document.baleWriteUnlock(); }
    }
}



@Override public void playUndo(Document d)
{
   if (the_edit != null && the_edit instanceof BurpPlayableEdit) {
      restorePosition();
      BurpPlayableEdit ed = (BurpPlayableEdit) the_edit;
      base_document.baleWriteLock();
      try {
	 base_document.markChanged();
	 ed.playUndo(d);
       }
      catch (CannotUndoException e) { }
      finally { base_document.baleWriteUnlock(); }
    }
}




@Override public boolean playUndo(Document d,BurpRange rng)
{
   boolean fg = false;
   if (the_edit != null && the_edit instanceof BurpPlayableEdit) {
      restorePosition();
      BurpPlayableEdit ed = (BurpPlayableEdit) the_edit;
      base_document.baleWriteLock();
      try {
	 base_document.markChanged();
	 fg = ed.playUndo(d,rng);
       }
      catch (CannotUndoException e) { }
      finally { base_document.baleWriteUnlock(); }
    }
   return fg;
}


@Override public void updatePosition(Object edit,int pos,int len) { 
   if (the_edit != null && the_edit instanceof BurpPlayableEdit) {
      BurpPlayableEdit bpe = (BurpPlayableEdit) the_edit;
      bpe.updatePosition(edit, pos, len);
   }
   else if (the_edit != null) {
      BoardLog.logD("BALE","Update position on " + the_edit);
    }
}



@Override public List<BurpEditDelta> getDeltas() 
{
   if (the_edit != null && the_edit instanceof BurpPlayableEdit) {
      BurpPlayableEdit bpe = (BurpPlayableEdit) the_edit;
      return bpe.getDeltas();
    }
   return null;
}





/********************************************************************************/
/*										*/
/*	Position save methods for undo/redo					*/
/*										*/
/********************************************************************************/

private void savePosition()
{
   doc_position = null;
   
//    try {
//       doc_position = base_document.createPosition(doc_offset);
//     }
//    catch (BadLocationException e) { }
}



private void restorePosition()
{
   if (doc_position == null) return;
   int off = doc_position.getOffset();
   if (off != doc_offset) {
      // BoardLog.logD("BALE","Document position update " + off + doc_offset);
      doc_offset = off;
    }
}


}	// end of class BaleDocumentEvent




/* end of BaleDocumentEvent.java */
