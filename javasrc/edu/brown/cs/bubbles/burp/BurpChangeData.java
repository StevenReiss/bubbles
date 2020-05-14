/********************************************************************************/
/*										*/
/*		BurpChangeData.java						*/
/*										*/
/*	Bubble Undo/Redo Processor information for a particular edit		*/
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

import javax.swing.text.Document;
import javax.swing.undo.UndoableEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



class BurpChangeData implements BurpConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BurpHistory for_history;
private UndoableEdit base_edit;
private BurpChangeData next_global;
private BurpChangeData prior_global;
private Map<BurpEditorData,BurpChangeData> next_editor;
private Map<BurpEditorData,BurpChangeData> prior_editor;
private List<BurpChangeData> depend_upons;
private boolean is_significant;
private ChangeType change_type;
private BurpChangeData link_back;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BurpChangeData(BurpHistory bh,UndoableEdit ue,BurpChangeData prior)
{
   for_history = bh;
   base_edit = ue;
   if (prior == null) next_global = null;
   else next_global = prior.next_global;
   prior_global = prior;
   if (prior_global != null) prior_global.next_global = this;
   next_editor = new HashMap<BurpEditorData,BurpChangeData>(2);
   prior_editor = new HashMap<BurpEditorData,BurpChangeData>(2);
   depend_upons = null;
   is_significant = ue.isSignificant();
   change_type = ChangeType.EDIT;
   link_back = null;
}


BurpChangeData(BurpHistory bh,ChangeType cty,BurpChangeData prior,BurpChangeData link)
{
   for_history = bh;
   base_edit = null;
   if (prior == null) next_global = null;
   else next_global = prior.next_global;
   prior_global = prior;
   if (prior_global != null) prior_global.next_global = this;
   next_editor = new HashMap<>(2);
   prior_editor = new HashMap<>(2);
   depend_upons = null;
   change_type = cty;
   switch (cty) {
      case START_UNDO :
      case START_REDO :
	 is_significant = true;
	 break;
      default :
	 is_significant = false;
	 break;
    }
   link_back = link;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BurpChangeData getPriorChange()
{
   return prior_global;
}



Document getBaseEditDocument()
{
   if (base_edit instanceof BurpSharedEdit) {
      BurpSharedEdit bde = (BurpSharedEdit) base_edit;
      return bde.getBaseEditDocument();
    }

   return null;
}

BurpPlayableEdit getPlayableEdit()
{
   if (base_edit != null && base_edit instanceof BurpPlayableEdit) {
      return ((BurpPlayableEdit) base_edit);
    }
   return null;
}



/********************************************************************************/
/*										*/
/*	Methods for getting next edit and associated editors			*/
/*										*/
/********************************************************************************/

BurpChangeData getNext(BurpEditorData ed)
{
   if (ed == null) return next_global;
   if (next_editor == null) return null;
   return next_editor.get(ed);
}


BurpChangeData getPrior(BurpEditorData ed)
{
   if (ed == null) return prior_global;
   if (prior_editor == null) return null;
   return prior_editor.get(ed);
}


void addEditor(BurpEditorData ed)
{
   BurpChangeData cd = ed.getCurrentChange();
   if (cd != null && cd.next_editor != null) cd.next_editor.put(ed,this);
   next_editor.put(ed,null);
   prior_editor.put(ed,cd);
}



boolean removeEditor(BurpEditorData ed)
{
   if (next_editor == null) return false;
   if (!next_editor.containsKey(ed)) return false;
   next_editor.remove(ed);
   prior_editor.remove(ed);
   if (next_editor.size() == 0) return true;	     // should be removed globally
   return false;
}



void removeGlobal()
{
   if (prior_global != null) prior_global.next_global = next_global;
   if (next_global != null) next_global.prior_global = prior_global;
   next_editor = null;
   prior_editor = null;
   base_edit = null;
}



/********************************************************************************/
/*										*/
/*	Methods for managing dependencies					*/
/*										*/
/********************************************************************************/

void addDependencies(List<BurpChangeData> dep0,List<BurpChangeData> done)
{
   depend_upons = null;
   if (dep0 != null && !dep0.isEmpty()) {
      depend_upons = new ArrayList<BurpChangeData>(dep0);
      if (done == null) done = new ArrayList<BurpChangeData>();
      done.addAll(dep0);
    }
   Set<BurpEditorData> eddeps = new HashSet<BurpEditorData>();

   if (next_editor != null) {
      for (BurpEditorData ed : next_editor.keySet()) {
	 if (ed.getCurrentChange() != this) eddeps.add(ed);
       }
    }

   // THIS CODE NEEDS TO BE MODIFIED SO THTAT DEPENDENCIES ARE NEVER ADDED MORE
   // THAN ONCE

   for (BurpChangeData cd = next_global;
	 cd != null && cd != this && !eddeps.isEmpty();
	 cd = cd.next_global) {
      if (done != null && done.contains(cd)) continue;
      Set<BurpEditorData> rem = new HashSet<BurpEditorData>();
      for (BurpEditorData ed : eddeps) {
	 if (cd.next_editor.containsKey(ed)) rem.add(ed);
       }
      boolean usefg = !rem.isEmpty();

      if (usefg) {
	 eddeps.removeAll(rem);
	 if (depend_upons == null) depend_upons = new ArrayList<BurpChangeData>();
	 if (!depend_upons.contains(cd)) depend_upons.add(cd);
	 if (done == null) done = new ArrayList<BurpChangeData>();
	 done.add(cd);
	 cd.addDependencies(null,done);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Significant event determination 					*/
/*										*/
/********************************************************************************/

void setSignificant(boolean fg) 		{ is_significant = fg; }

boolean isSignificant()
{
   if (is_significant) return true;
   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (cd.isSignificant()) return true;
       }
    }
   return false;
}

ChangeType getChangeType()			{ return change_type; }

BurpChangeData getBackLink()			{ return link_back; }
void setBackLink(BurpChangeData bcd)		{ link_back = bcd; }


UndoableEdit getRootEdit()
{
   if (change_type != ChangeType.EDIT) return null;

   if (link_back != null) {
      UndoableEdit ue = link_back.getRootEdit();
      if (ue != link_back.base_edit) 
         System.err.println("DOuble link");
      return ue;
   }

   return base_edit;
}



/********************************************************************************/
/*										*/
/*	Methods to handle undo/redo						*/
/*										*/
/********************************************************************************/

boolean canUndo()
{
   if (base_edit == null) return true;
   if (!base_edit.canUndo()) return false;
   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (!cd.canUndo()) return false;
       }
    }
   return true;
}



boolean canRedo()
{
   if (!base_edit.canRedo()) return false;
   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (!cd.canRedo()) return false;
       }
    }
   return true;
}



void undo()
{
   BoardLog.logD("BURP","UNDO " + this + " " + base_edit.getUndoPresentationName() + " " + depend_upons);

   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (cd.canUndo()) cd.undo();
	 else {
	    BoardLog.logD("BURP","Can't undo dependency " + cd + " " + cd.depend_upons);
	  }
       }
    }

   if (base_edit.canUndo()) {
      base_edit.undo();
      BoardLog.logD("BURP","Finished UNDO " + this);
    }
   else BoardLog.logD("BURP","Can't UNDO " + this);

   for_history.resetCurrentChange(this,prior_global,false);
   for (Map.Entry<BurpEditorData,BurpChangeData> ent : prior_editor.entrySet()) {
      BurpEditorData ed = ent.getKey();
      BurpChangeData cd = ent.getValue();
      ed.setCurrentChange(cd);
    }
}



void playUndo(BurpRange rng)
{
   if (base_edit == null) return;

   BoardLog.logD("BURP","UNDO " + this + " " + base_edit.getUndoPresentationName() + " " + depend_upons);

   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (cd.canUndo()) cd.playUndo(rng);
	 else {
	    BoardLog.logD("BURP","Can't undo dependency " + cd + " " + cd.depend_upons);
	  }
       }
    }

   if (base_edit.canUndo() && base_edit instanceof BurpPlayableEdit) {
      BurpPlayableEdit bpe = (BurpPlayableEdit) base_edit;
      if (rng != null) bpe.playUndo(getBaseEditDocument(),rng);
      else bpe.playUndo(getBaseEditDocument());
      BoardLog.logD("BURP","Finished UNDO " + this);
    }
   else BoardLog.logD("BURP","Can't UNDO " + this);
}



void redo()
{
   base_edit.redo();

   for_history.resetCurrentChange(prior_global,this,true);
   for (Map.Entry<BurpEditorData,BurpChangeData> ent : next_editor.entrySet()) {
      BurpEditorData ed = ent.getKey();
      ed.setCurrentChange(this);
    }

   if (depend_upons != null) {
      for (int i = depend_upons.size()-1; i >= 0; --i) {
	 BurpChangeData cd = depend_upons.get(i);
	 if (cd.canRedo()) cd.redo();
       }
    }
}



}	// end of class BurpChangeData




/* end of BurpChangeData.java */
