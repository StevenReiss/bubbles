/********************************************************************************/
/*										*/
/*		BurpEditorData.java						*/
/*										*/
/*	Bubble Undo/Redo Processor information for a particular editor		*/
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

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


class BurpEditorData implements UndoableEditListener, CaretListener, KeyListener, MouseListener,
		BurpConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BurpHistory for_history;
private BurpChangeData current_change;
private BurpChangeData first_change;
private JTextComponent for_editor;
private Document for_document;
private boolean next_sig;
private boolean last_key;
private boolean in_event;
private BurpChangeData save_change;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BurpEditorData(BurpHistory bh,JTextComponent be) {
   for_history = bh;
   current_change = null;
   save_change = null;
   for_editor = be;
   for_document = be.getDocument();
   for_document.addUndoableEditListener(this);
   if (for_editor != null) {
      for_editor.addCaretListener(this);
      for_editor.addKeyListener(this);
      for_editor.addMouseListener(this);
    }
   next_sig = true;
   last_key = false;
   in_event = false;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addChange(BurpChangeData cd)
{
   cd.addEditor(this);
   if (current_change == null) first_change = cd;
   current_change = cd;
}



void remove()
{
   for_document.removeUndoableEditListener(this);
   if (for_editor != null) {
      for_editor.removeCaretListener(this);
      for_editor.removeKeyListener(this);
      for_editor.removeMouseListener(this);
    }
}



BurpChangeData getCurrentChange()		{ return current_change; }
BurpChangeData getFirstChange() 		{ return first_change; }
void setCurrentChange(BurpChangeData cd)	{ current_change = cd; }

String getBubbleId()
{
   if (for_editor == null) return null;
   BudaBubble bb = BudaRoot.findBudaBubble(for_editor);
   if (bb == null) return null;

   return bb.getHashId();
}



boolean isDirty()
{
   return current_change != save_change;
}


void noteSave()
{
   save_change = current_change;
}



/********************************************************************************/
/*										*/
/*	Edit callbacks								*/
/*										*/
/********************************************************************************/

@Override public void undoableEditHappened(UndoableEditEvent evt)
{
   UndoableEdit ued = evt.getEdit();

   for_history.handleNewEdit(this,ued,in_event,next_sig);
   next_sig = false;
}




/********************************************************************************/
/*										*/
/*	Caret and key callbacks for detecting significant edits 		*/
/*										*/
/********************************************************************************/

@Override public void caretUpdate(CaretEvent e)
{
   // TODO: might want to create UndoableEditEvent to reset the mark/dot here
}



@Override public void keyPressed(KeyEvent e)
{
   if (isModifier(e)) return;
   boolean txt = isTextual(e);
   if (!last_key || !txt) next_sig = true;
   last_key = txt;
   in_event = true;
}



@Override public void keyReleased(KeyEvent e)
{
   if (isModifier(e)) return;
   in_event = false;
}


@Override public void keyTyped(KeyEvent e)		{ }


@Override public void mouseClicked(MouseEvent e)	{ }


@Override public void mousePressed(MouseEvent e)
{
   last_key = false;
   next_sig = true;
   in_event = true;
}


@Override public void mouseReleased(MouseEvent e)
{
   in_event = false;
}

@Override public void mouseEntered(MouseEvent e)	{ }
@Override public void mouseExited(MouseEvent e) 	{ }


void beginEditAction()
{
   in_event = true;
}


void endEditAction()
{
   in_event = false;
}


private boolean isModifier(KeyEvent e)
{
   switch (e.getKeyCode()) {
      case KeyEvent.VK_SHIFT :
      case KeyEvent.VK_CONTROL :
      case KeyEvent.VK_ALT :
      case KeyEvent.VK_META :
	 return true;
    }
   return false;
}



private boolean isTextual(KeyEvent e)
{
   if (last_key) {
      if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) return true;
    }
   if (e.isActionKey()) return false;
   char c = e.getKeyChar();
   return c >= 0x20 && c < 0x7f;
}




}	// end of class BurpEditorData



/* end of BurpEditorData.java */

