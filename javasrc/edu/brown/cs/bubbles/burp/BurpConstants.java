/********************************************************************************/
/*										*/
/*		BurpConstants.java						*/
/*										*/
/*	Bubble Undo/Redo Processor constant definitions 			*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.burp;


import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import java.util.List;


/**
 *	External definitions for using the history package Burp.
 **/

public interface BurpConstants {



/********************************************************************************/
/*										*/
/*	Provide for events that can provide shared edits			*/
/*										*/
/********************************************************************************/

/**
 *	This interface provides for edits that are common between code bubbles.  This
 *	can happen when multiple bubbles share the same document.  The interface lets
 *	the history module connect the multiple edits so that they are treated as one.
 **/

interface BurpSharedEdit {

/**
 *	Return the underlying edit that is responsible for this edit.
 **/

   UndoableEdit getBaseEdit();		// return the common base edit for this edit
   Document getBaseEditDocument();	// return the base document for this edit

}


interface BurpEditorDocument {

   Position createHistoryPosition(int offset) throws BadLocationException;

}



/********************************************************************************/
/*										*/
/*	Provide for UNDO by adding new commands 				*/
/*										*/
/********************************************************************************/

interface BurpPlayableEdit {

   void playUndo(Document doc) throws CannotUndoException;

   // returns true if edit is done in the range
   boolean playUndo(Document doc,BurpRange range) throws CannotUndoException;

   void updatePosition(Object edit,int pos,int delta);
   
   List<BurpEditDelta> getDeltas();
   
}	// end of interface BurpPlayableEdit

static class BurpEditDelta {
   
   private int delta_offset;
   private int delta_delta;
   
   public BurpEditDelta(int offset,int delta) {
      delta_offset = offset;
      delta_delta = delta;
    }
   
   public int getOffset()                       { return delta_offset; }
   public int getDelta()                        { return delta_delta; }
   
}



interface BurpRange {
   int getStartPosition();
   int getEndPosition();
}	// end of interface BurpRange


enum ChangeType {
   EDIT,
   START_UNDO,
   END_UNDO,
   START_REDO,
   END_REDO
}



}	// end of interface BurpConstants




/* end of BurpConstants.java */
