/********************************************************************************/
/*										*/
/*		BaleEditorKitPython.java					*/
/*										*/
/*	Bubble Annotated Language Editor editor kit for Python-specific cmds	*/
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


package edu.brown.cs.bubbles.bale;


import javax.swing.Action;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;

import java.awt.event.ActionEvent;



class BaleEditorKitPython implements BaleConstants, BaleConstants.BaleLanguageKit
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static final Action python_unindent_action = new PythonUnindentAction();
private static final Action python_backspace_action = new PythonBackspaceAction();

private static final Action [] local_actions = {
   python_unindent_action,
   python_backspace_action,
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleEditorKitPython()
{
}



/********************************************************************************/
/*										*/
/*	Action Methods								*/
/*										*/
/********************************************************************************/

@Override public Action [] getActions()
{
   return local_actions;
}


@Override public Keymap getKeymap(Keymap base)
{
   // this should only be called once, but it is called for each editor
   // control-tab doesn't work for some reason
   BaleEditorKit.KeyItem ki;

   ki = new BaleEditorKit.KeyItem("ctrl Q",python_unindent_action);
   ki.addToKeyMap(base);
   ki = new BaleEditorKit.KeyItem("BACK_SPACE",python_backspace_action);
   ki.addToKeyMap(base);

   return base;
}


/********************************************************************************/
/*										*/
/*	Python backspace action 						*/
/*										*/
/********************************************************************************/

private static class PythonBackspaceAction extends TextAction {

   private static final long serialVersionUID = 1;

   private Action backspace_action;

   PythonBackspaceAction() {
      super("PythonBackspaceAction");
      backspace_action = null;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (backspace_action == null) {
	 backspace_action = BaleEditorKit.findAction("BackspaceAction");
       }
      BaleEditorPane target = BaleEditorKit.getBaleEditor(e);
      if (!BaleEditorKit.checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      int lno = bd.findLineNumber(soff);
      int lsoff = bd.findLineOffset(lno);
      
      boolean start = false;
      try {
	 if (lsoff == soff) start = true;
	 else {
	    String txt = bd.getText(lsoff,soff-lsoff);
	    txt = txt.trim();
	    if (txt.equals("")) start = true;
	 }
      }
      catch (BadLocationException ex) { }
      
      if (!start) {
	 backspace_action.actionPerformed(e);
	 return;
      }
      
      BaleIndenter bind = bd.getIndenter();
      int oind = bind.getCurrentIndentationAtOffset(soff);
      int tind = bind.getDesiredIndentation(soff);
      
      if (tind != oind || oind == 0) {
	 backspace_action.actionPerformed(e);
	 return;
       }
      int delta = bind.getUnindentSize();
      if (delta > oind) delta = oind;
      for (int i = 0; i < delta; ++i) {
	 backspace_action.actionPerformed(e);
       }
    }

}	// end of inner class PythonBackspaceAction


/********************************************************************************/
/*										*/
/*	Unindent action 							*/
/*										*/
/********************************************************************************/

private static class PythonUnindentAction extends TextAction {

   private static final long serialVersionUID = 1;

   private Action forward_action;

   PythonUnindentAction() {
      super("PythonUnindentAction");
      forward_action = null;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (forward_action == null)
	 forward_action = BaleEditorKit.findAction(DefaultEditorKit.forwardAction);
      BaleEditorPane target = BaleEditorKit.getBaleEditor(e);
      if (!BaleEditorKit.checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      BaleIndenter bind = bd.getIndenter();
      int slno = bd.findLineNumber(soff);
      int lpos = bd.findLineOffset(slno);
      int oind = bind.getCurrentIndentationAtOffset(soff);
      int tind = bind.getDesiredIndentation(soff);
      int delta = bind.getUnindentSize();
      int pos = tind-delta;
      if (pos < oind) {
	 for (int i = oind; i > pos; --i) {
	    forward_action.actionPerformed(e);
	 }
      }
      else {
	 try {
	    for (int i = oind; i < pos; ++i) {
	       bd.insertString(lpos," ",null);
	    }
	    target.setSelectionStart(lpos + pos);
	 }
	 catch (BadLocationException ex) { }
      }
    }

}	// end of inner class PythonUnindentAction




}	// end of class BaleEditorKitPython



/* end of BaleEditorKitPython.java */

