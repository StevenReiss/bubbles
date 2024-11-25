/********************************************************************************/
/*										*/
/*		BurpHistory.java						*/
/*										*/
/*	Bubble Undo/Redo Processor history maintainer				*/
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

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.undo.UndoableEdit;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *	This class implements command history and undo/redo in the bubbles
 *	framework.
 *
 *	It actually supports intelligent undo/redo on a bubble-by-bubble
 *	basis.	Where commands affect multiple bubbles (which can happen
 *	for various reasons, e.g. a global name change or a simple edit
 *	where multiple bubbles share the same buffer), it ensures that all
 *	bubbles are kept consistent by undoing/redoing any dependent
 *	commands at the same time.
 *
 *	For example if I edit A, then do a global name change, then edit B,
 *	then do an undo in A, the global name change will be undone as will
 *	the edit to B.	A second undo in A will undo the initial edit to A.
 *
 **/

public final class BurpHistory implements BurpConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BurpChangeData			current_change;
private BurpChangeData			first_change;
private Map<JTextComponent,BurpEditorData>  editor_map;
private Map<UndoableEdit,BurpChangeData>    change_map;
private Map<Document,BurpChangeData>	    undo_command;

private static BurpHistory		the_history = null;
private static UndoRedoAction		undo_action = new UndoRedoAction(-1,false);
private static UndoRedoAction		redo_action = new UndoRedoAction(1,false);
private static UndoRedoAction		undo_selection_action = new UndoRedoAction(-1,true);
private static UndoRedoAction		redo_selection_action = new UndoRedoAction(1,true);


/********************************************************************************/
/*										*/
/*	Static access methods							*/
/*										*/
/********************************************************************************/

/**
 *	Return the singular instance of the history module for all bubbles.
 **/

public static synchronized BurpHistory getHistory()
{
   if (the_history == null) {
      the_history = new BurpHistory();
    }
   return the_history;
}




/**
 *	Return the singluar instance of an UNDO editor action that can be
 *	associated with key strokes or invoked on mouse action.
 **/

public static Action getUndoAction()		{ return undo_action; }


/**
 *	Return the singular instance of a REDO editor action.
 **/

public static Action getRedoAction()		{ return redo_action; }



/**
*      Return the singluar instance of an UNDO editor action that can be
*      associated with key strokes or invoked on mouse action.
**/

public static Action getUndoSelectionAction()  { return undo_selection_action; }


/**
*      Return the singular instance of a REDO editor action.
**/

public static Action getRedoSelectionAction()  { return redo_selection_action; }




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BurpHistory()
{
   current_change = null;
   editor_map = new HashMap<JTextComponent,BurpEditorData>();
   change_map = new HashMap<UndoableEdit,BurpChangeData>();
   undo_command = new HashMap<Document,BurpChangeData>();

   BumpClient.getBump().addChangeHandler(new ChangeHandler());
}



/********************************************************************************/
/*										*/
/*	Editor management methods						*/
/*										*/
/********************************************************************************/

/**
 *	Register an editor with the history mechanism.	This should be done when the
 *	editor is created.  UNDO/REDO is only supported in registered editors.
 **/

public void addEditor(JTextComponent be)
{
   BurpEditorData ed = new BurpEditorData(this,be);
   synchronized (editor_map) {
      editor_map.put(be,ed);
    }
}


/**
 *	Unregister an editor with the history mechanism.  This should be done when
 *	the editor is removed.
 *
 *	This should actually be done automatically when the editor is freed, but this
 *	can be problematic given all the links that might exist here and elsewhere to
 *	the editor.
 **/

public void removeEditor(JTextComponent be)
{
   if (be == null) return;
   
   synchronized (editor_map) {
      BurpEditorData ed = editor_map.remove(be);
      if (ed != null) ed.remove();
    }
}



/**
 *	Determine if the given registered editor is dirty, i.e. the underlying text
 *	has been edited, since the last save.
 **/

public boolean isDirty(JTextComponent be)
{
   BurpEditorData ed = editor_map.get(be);

   if (ed == null) return false;

   return ed.isDirty();
}



/**
 *	Determine if undo is possible for a component
 **/

public boolean canUndo(JTextComponent be)
{
   BurpEditorData ed = editor_map.get(be);

   if (ed == null) return false;

   BurpChangeData cd = ed.getCurrentChange();
   if (cd == null) return false;

   return cd.canUndo();
}




/**
 *	Determine if redo is possible for a component
 **/

public boolean canRedo(JTextComponent be)
{
   BurpEditorData ed = editor_map.get(be);

   if (ed == null) return false;

   BurpChangeData cd = ed.getCurrentChange();
   if (cd == null)  cd = ed.getFirstChange();
   else cd = cd.getNext(ed);
   if (cd == null) return false;

   return cd.canRedo();
}



public void beginEditAction(JTextComponent be)
{
   if (be == null) return;
   BurpEditorData ed = editor_map.get(be);
   if (ed != null) ed.beginEditAction();
}


public void endEditAction(JTextComponent be)
{
   if (be == null) return;
   BurpEditorData ed = editor_map.get(be);
   if (ed != null) ed.endEditAction();
}



/********************************************************************************/
/*										*/
/*	Undo/Redo request methods						*/
/*										*/
/********************************************************************************/

/**
 *	Undo the last edit in the registered editor.
 **/

public void undo(JTextComponent be)
{
   BoardMetrics.noteCommand("BURP","undo");

   BurpEditorData ed = null;
   if (be != null) {
      ed = editor_map.get(be);
      if (ed == null) return;
    }

   List<BurpChangeData> deps = null;
   BurpChangeData c0 = (ed == null ? current_change : ed.getCurrentChange());
   Document d0 = null;
   if (c0 != null) d0 = c0.getBaseEditDocument();
   if (d0 != null) {
      for (BurpChangeData cd = current_change; cd != null && cd != c0; cd = cd.getPriorChange()) {
	 Document d1 = cd.getBaseEditDocument();
	 if (d1 != null && d1 == d0) {
	    if (deps == null) deps = new ArrayList<BurpChangeData>();
	    deps.add(cd);
	  }
       }
    }
   deps = null; 	// remove effect of above code to try new undo mechanism
   // null this out to force undo at the file level

   boolean havesig = false;
   while (!havesig) {
      BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
      if (cd == null) break;
      cd.addDependencies(deps,null);
      deps = null;
      if (!cd.canUndo()) break;
      cd.undo();
      if (cd.isSignificant()) havesig = true;
    }
}


void playUndo(JTextComponent be,boolean sel)
{
   BurpRange rng = null;

   BoardLog.logD("BURP","Start PLAYUNDO " + sel);

   if (sel) {
      int soff = be.getSelectionStart();
      int eoff = be.getSelectionEnd();
      if (soff != eoff) {
	 Document d = be.getDocument();
	 try {
	    rng = new UndoRedoRange(d,soff,eoff);
	  }
	 catch (BadLocationException ex) {
	    System.err.println("BAD RANGE: " + ex);
	  }
       }
      else return;              // do nothing if there is no region
    }

   BoardMetrics.noteCommand("BURP","undo");

   BurpEditorData ed = null;
   if (be != null) {
      ed = editor_map.get(be);
      if (ed == null) return;
    }

   BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
   while (cd != null && cd.getChangeType() == ChangeType.END_UNDO) {
      cd = cd.getBackLink();
    }
   if (cd == null || !cd.canUndo()) return;

   insertPlaceholder(ed,ChangeType.START_UNDO,null);
   int lvl = 0;
   for ( ; ; ) {
      if (cd == null) break;
      if (!cd.canUndo()) break;
      switch (cd.getChangeType()) {
	 case EDIT :
	    BurpChangeData pvd = undo_command.put(cd.getBaseEditDocument(),cd);
	    cd.playUndo(rng);
	    if (pvd == null) undo_command.remove(cd.getBaseEditDocument());
	    else undo_command.put(cd.getBaseEditDocument(),pvd);
	    break;
	 case END_UNDO :
	 case END_REDO :
	    ++lvl;
	    break;
	 case START_UNDO :
	 case START_REDO :
	    --lvl;
	    break;
	 default :
	    break;
       }
      if (lvl == 0 && cd.isSignificant()) break;
      cd = cd.getPrior(ed);
    }
   if (cd != null) cd = cd.getPrior(ed);
   insertPlaceholder(ed,ChangeType.END_UNDO,cd);
}



public void playRedo(JTextComponent be,boolean sel)
{
   BurpRange rng = null;

   BoardLog.logD("BURP","Start PLAYREDO " + sel);

   if (sel) {
      int soff = be.getSelectionStart();
      int eoff = be.getSelectionEnd();
      if (soff != eoff) {
	 Document d = be.getDocument();
	 try {
	    rng = new UndoRedoRange(d,soff,eoff);
	  }
	 catch (BadLocationException ex) {
	    System.err.println("BAD RANGE: " + ex);
	  }
       }
      else return;              // do nothing if there is no region
    }

   BoardMetrics.noteCommand("BURP","redo");

   BurpEditorData ed = null;
   if (be != null) {
      ed = editor_map.get(be);
      if (ed == null) return;
    }

   BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
   while (cd != null && cd.getChangeType() == ChangeType.END_REDO) {
      cd = cd.getBackLink();
    }
   if (cd != null && cd.getChangeType() != ChangeType.END_UNDO) return;

   insertPlaceholder(ed,ChangeType.START_REDO,cd);
   int lvl = 0;
   for ( ; ; ) {
      if (cd == null) break;
      if (!cd.canUndo()) break;
      switch (cd.getChangeType()) {
	 case EDIT :
	    BurpChangeData pvd = undo_command.put(cd.getBaseEditDocument(),cd);
	    cd.playUndo(rng);
	    if (pvd == null) undo_command.remove(cd.getBaseEditDocument());
	    else undo_command.put(cd.getBaseEditDocument(),pvd);
	    break;
	 case START_UNDO :
	 case START_REDO :
	    --lvl;
	    break;
	 case END_UNDO :
	 case END_REDO :
	    ++lvl;
	    break;
	 default :
	    break;
       }
      if (lvl == 0 && cd.isSignificant()) break;
      cd = cd.getPrior(ed);
    }
   if (cd != null) cd = cd.getPrior(ed);
   insertPlaceholder(ed,ChangeType.END_REDO,cd);
}




/**
 *	Redo the last undone edit in the registered editor if that is possible.  If an
 *	intervening command was executed, the undo can not be redone.
 **/

public void redo(JTextComponent be)
{
   BoardMetrics.noteCommand("BURP","undo");

   BurpEditorData ed = null;
   if (be != null) {
      ed = editor_map.get(be);
      if (ed == null) return;
    }

   for (int ct = 0; ; ++ct) {
      BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
      if (cd == null) {
	 if (ed == null) cd = first_change;
	 else cd = ed.getFirstChange();
       }
      else cd = cd.getNext(ed);
      if (cd == null) break;
      if (cd.isSignificant() && ct > 0) break;
      if (!cd.canRedo()) break;
      cd.redo();
    }
}




/********************************************************************************/
/*										*/
/*	Methods for maintaining the current_change				*/
/*										*/
/********************************************************************************/

void resetCurrentChange(BurpChangeData when,BurpChangeData to,boolean fwd)
{
   BoardLog.logD("BURP", "RESET CURRENT " + current_change + " " + to);
   if (current_change == when) current_change = to;
   else if (fwd && current_change == null) current_change = to;
}



/********************************************************************************/
/*										*/
/*	Internal list management methods					*/
/*										*/
/********************************************************************************/

synchronized void handleNewEdit(BurpEditorData ed,UndoableEdit ue,boolean evt,boolean sig)
{
   UndoableEdit bed = getBaseEdit(ue);

   BoardMetrics.noteCommand("BURP",getEditCommandName(ed,ue));

   BurpChangeData cd = change_map.get(bed);
   if (cd == null) {
      if (ue instanceof BurpPlayableEdit && ue instanceof BurpSharedEdit) {
         List<BurpEditDelta> delta = ((BurpPlayableEdit) ue).getDeltas();
         BurpSharedEdit bse = (BurpSharedEdit) ue;
         Document bdoc = bse.getBaseEditDocument();
         Object root = ue;
         BurpChangeData undocd = undo_command.get(bdoc);
         if (undocd != null) root = undocd.getRootEdit();
         if (delta != null) {
            applyDeltas(root,bdoc,delta);
          }
       }
      else {
         BoardLog.logD("BURP","Saving simple edit " + ue);
       }
      removeForward(null);
      cd = new BurpChangeData(this,ue,current_change);
      if (current_change == null) first_change = cd;
      current_change = cd;
      change_map.put(bed,cd);
      BurpChangeData undocd = undo_command.get(cd.getBaseEditDocument());
      if (undocd != null) cd.setBackLink(undocd);
    }
   BoardLog.logD("BURP","Record edit " + cd + " " + ue.getUndoPresentationName() + " " + ed + " " + evt + " " + sig);

   if (evt) cd.setSignificant(sig);

   if (ed != null) {
      removeForward(ed);
      ed.addChange(cd);
    }
}


void applyDeltas(Object edit,Document d,List<BurpEditDelta> ed)
{
   if (d == null) return;
   if (ed == null || ed.size() == 0) return;

   for (BurpChangeData cd = first_change; cd != null; cd = cd.getNext(null)) {
      if (cd.getBaseEditDocument() == d) {
	 BurpPlayableEdit bse = cd.getPlayableEdit();
	 if (bse != null) {
	    for (BurpEditDelta del : ed) {
	       bse.updatePosition(edit,del.getOffset(),del.getDelta());
	    }
	 }
       }
    }
}


synchronized BurpChangeData insertPlaceholder(BurpEditorData ed,ChangeType cty,BurpChangeData link)
{
   BurpChangeData cd = new BurpChangeData(this,cty,current_change,link);
   if (current_change == null) first_change = cd;
   current_change = cd;
   BoardLog.logD("BURP","Placeholder " + cd + " " + cty + " " + ed);
   if (ed != null) {
      removeForward(ed);
      ed.addChange(cd);
    }

   return cd;
}



private UndoableEdit getBaseEdit(UndoableEdit ed)
{
   if (ed instanceof BurpSharedEdit) {
      BurpSharedEdit bde = (BurpSharedEdit) ed;
      ed = bde.getBaseEdit();
    }

   return ed;
}



private void removeForward(BurpEditorData ed)
{
   // remove any changes after cd for the given editor
   // if ed is null, remove all changes after ed
   // assumes that any forward changes have been undone

   BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
   if (cd == null) return;

   BurpChangeData next = null;
   for (BurpChangeData ncd = cd.getNext(ed); ncd != null; ncd = next) {
      next = ncd.getNext(ed);
      if (ed == null || ncd.removeEditor(ed)) ncd.removeGlobal();
    }
}



/***********************
private void removeAll(BurpEditorData ed)
{
   // remove any changes for the given editor

   BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
   if (cd == null) return;

   BurpChangeData next = null;
   for (BurpChangeData ncd = cd.getNext(ed); ncd != null; ncd = next) {
      next = ncd.getNext(ed);
      if (ed == null || ncd.removeEditor(ed)) ncd.removeGlobal();
}
**************************/



private String getEditCommandName(BurpEditorData be,UndoableEdit ed)
{
   String rslt = "edit_" + ed.getPresentationName();
   if (ed instanceof DefaultDocumentEvent) {
      // do it explicitly if possible to handle other locales
      DefaultDocumentEvent aed = (DefaultDocumentEvent) ed;
      DocumentEvent.EventType type = aed.getType();
      if (type == DocumentEvent.EventType.INSERT) rslt = "edit_addition";
      else if (type == DocumentEvent.EventType.REMOVE) rslt = "edit_deletion";
      else rslt = "edit_stylechange"; 
   }
   if (be != null) {
      String id = be.getBubbleId();
      if (id != null) rslt += "_" + id;
    }

   if (ed instanceof DocumentEvent) {
      DocumentEvent de = (DocumentEvent) ed;
      rslt += "_" + de.getLength() + "_" + de.getOffset();
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Undo/redo action for text editors					*/
/*										*/
/********************************************************************************/

private static class UndoRedoAction extends AbstractAction {

   private int history_direction;
   private boolean use_selection;

   private static final long serialVersionUID = 1;

   UndoRedoAction(int dir,boolean sel) {
      super((dir > 0 ? "Redo" : "Undo") + (sel ? "Selection" : ""));
      history_direction = dir;
      use_selection = sel;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BurpHistory hist = BurpHistory.getHistory();
      if (e.getSource() instanceof JTextComponent) {
         JTextComponent bed = (JTextComponent) e.getSource();
         if (history_direction < 0) hist.playUndo(bed,use_selection);
         else hist.playRedo(bed,use_selection);
       }
    }

}	// end of inner class UndoRedoAction



private static class UndoRedoRange implements BurpRange {

   private Position start_position;
   private Position end_position;

   UndoRedoRange(Document d,int spos,int epos) throws BadLocationException {
      if (d instanceof BurpEditorDocument) {
	 BurpEditorDocument bed = (BurpEditorDocument) d;
	 start_position = bed.createHistoryPosition(spos);
	 end_position = bed.createHistoryPosition(epos);
       }
      else {
	 start_position = d.createPosition(spos);
	 end_position = d.createPosition(epos);
       }
    }

   @Override public int getStartPosition() {
      return start_position.getOffset();
    }

   @Override public int getEndPosition() {
      return end_position.getOffset();
    }

}	// end of inner class UndoRedoRange




/********************************************************************************/
/*										*/
/*	Change handler for detecting saves					*/
/*										*/
/********************************************************************************/

private void noteSave(File file)
{
   synchronized (editor_map) {
      for (Map.Entry<JTextComponent,BurpEditorData> ent : editor_map.entrySet()) {
	 JTextComponent tc = ent.getKey();
	 BudaBubble bb = BudaRoot.findBudaBubble(tc);
	 if (bb != null) {
	    File bf = bb.getContentFile();
	    if (bf != null && bf.equals(file)) ent.getValue().noteSave();
	  }
       }
    }
}



private final class ChangeHandler implements BumpConstants.BumpChangeHandler {

   
   
   
   

   @Override public void handleFileChanged(String proj,String file) {
      noteSave(new File(file));
    }

}	// end of inner class ChangeHandler



}	// end of class BurpHistory



/* end of BurpHistory.java */

