/********************************************************************************/
/*										*/
/*		BaleEditorKit.java						*/
/*										*/
/*	Bubble Annotated Language Editor editor kit				*/
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


import edu.brown.cs.bubbles.bale.BaleElement.Indent;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.swing.SwingEditorPane;
import edu.brown.cs.ivy.swing.SwingKey;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.ParagraphView;
import javax.swing.text.Position;
import javax.swing.text.TextAction;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;



class BaleEditorKit extends DefaultEditorKit implements BaleConstants, ViewFactory,
		BudaConstants, BuenoConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;

private static transient BaleLanguageKit language_kit;
private transient Action []	  bale_actions;

// CHECKSTYLE:OFF
private static final Action undo_action = BurpHistory.getUndoAction();
private static final Action redo_action = BurpHistory.getRedoAction();
private static final Action undo_selection_action = BurpHistory.getUndoSelectionAction();
private static final Action redo_selection_action = BurpHistory.getRedoSelectionAction();
private static final Action tab_action = new TabAction();
private static final Action newline_action = new NewlineAction();
private static final Action default_key_action = new DefaultKeyAction();
private static final Action backspace_action = new BackspaceAction();
private static final Action toggle_insert_action = new ToggleInsertAction();
private static final Action find_action = new FindAction(false);
private static final Action find_next_action = new FindNextAction(1);
private static final Action find_prev_action = new FindNextAction(-1);
private static final Action replace_action = new FindAction(true);
private static final Action delete_line_action = new DeleteLineAction();
private static final Action delete_to_eol_action = new DeleteToEolAction();
private static final Action insert_line_above_action = new InsertLineAboveAction();
private static final Action insert_line_below_action = new InsertLineBelowAction();
private static final Action indent_lines_action = new IndentLinesAction();
private static final Action join_lines_action = new JoinLinesAction();
private static final Action duplicate_action = new DuplicateAction();
private static final Action comment_lines_action = new CommentLinesAction();
private static final Action smart_paste_action = new SmartPasteAction();
private static final Action move_lines_down_action = new MoveLinesAction(1);
private static final Action move_lines_up_action = new MoveLinesAction(-1);
private static final Action start_line_action = new StartLineAction(false);
private static final Action start_line_select_action = new StartLineAction(true);
private static final Action begin_line_action = new BeginLineAction(false);
private static final Action begin_line_select_action = new BeginLineAction(true);
private static final Action end_line_action = new EndLineAction(false);
private static final Action end_line_select_action = new EndLineAction(true);
private static final Action next_brace_action = new NextItemAction(BaleTokenType.RBRACE);
private static final Action select_line_action = new SelectLineAction();
private static final Action select_word_action = new SelectWordAction();
private static final Action select_paragraph_action = new SelectParagraphAction();
private static final Action save_action = new SaveAction();
private static final Action save_all_action = new SaveAllAction();
private static final Action revert_action = new RevertAction();
private static final Action force_save_action = new ForceSaveAction();
private static final Action explicit_elision_action = new ExplicitElisionAction();
private static final Action redo_elision_action = new RedoElisionAction();
private static final Action remove_elision_action = new RemoveElisionAction(false);
private static final Action remove_code_elision_action = new RemoveElisionAction(true);
private static final Action autocomplete_action = new AutoCompleteAction();
private static final Action rename_action = new RenameAction();
private static final Action extract_method_action = new ExtractMethodAction();
private static final Action format_action = new FormatAction();
private static final Action fixindents_action = new FixIndentsAction();
private static final Action expand_action = new ExpandAction();
private static final Action expandxy_action = new ExpandXYAction(false);
private static final Action expandxy_code_action = new ExpandXYAction(true);
private static final Action toggle_editable_action = new ToggleEditableAction();

private static final Action goto_definition_action = new GotoDefinitionAction();
private static final Action goto_implementation_action = new GotoImplementationAction();
private static final Action goto_reference_action = new GotoReferenceAction();
private static final Action goto_doc_action = new GotoDocAction();
private static final Action goto_search_action = new GotoSearchAction();
private static final Action goto_type_action = new GotoTypeAction();
private static final Action class_search_action = new ClassSearchAction();
private static final Action bud_action;
private static final Action fragment_action;
private static final Action quick_fix_action = new QuickFixAction();
private static final Action marquis_comment_action = new CommentAction("MarquisComment",
      BuenoType.NEW_MARQUIS_COMMENT);
private static final Action block_comment_action = new CommentAction("BlockComment",
      BuenoType.NEW_BLOCK_COMMENT);
private static final Action javadoc_comment_action = new CommentAction("JavadocComment",
      BuenoType.NEW_JAVADOC_COMMENT);
private static final Action fix_errors_action = new FixErrorsAction();
private static final Action  goto_next_error_action = new GotoNextErrorAction();
private static final Action finish_action = new FinishAction();
private static final Action infer_decl_action = new InferDeclarationAction();
private static final Action smart_delete_next_character_action = new SmartDeleteNextCharacterAction();
public static final Action python_unindent_action = new PythonUnindentAction();
public static final Action python_backspace_action = new PythonBackspaceAction();
// CHECKSTYLE:ON



static {
   BaleBudder bb = new BaleBudder();
   bud_action = bb.getBuddingAction();
   fragment_action = bb.getFragmentAction();
}


private static Action [] local_actions = {
   undo_action,
   redo_action,
   undo_selection_action,
   redo_selection_action,
   tab_action,
   newline_action,
   default_key_action,
   backspace_action,
   toggle_insert_action,
   find_action,
   find_next_action,
   find_prev_action,
   replace_action,
   delete_line_action,
   delete_to_eol_action,
   insert_line_above_action,
   insert_line_below_action,
   indent_lines_action,
   join_lines_action,
   duplicate_action,
   comment_lines_action,
   start_line_action,
   start_line_select_action,
   begin_line_action,
   begin_line_select_action,
   end_line_action,
   end_line_select_action,
   next_brace_action,
   select_line_action,
   select_word_action,
   select_paragraph_action,
   save_action,
   save_all_action,
   revert_action,
   force_save_action,
   explicit_elision_action,
   redo_elision_action,
   remove_elision_action,
   remove_code_elision_action,
   smart_paste_action,
   move_lines_up_action,
   move_lines_down_action,
   autocomplete_action,
   finish_action,
   rename_action,
   extract_method_action,
   format_action,
   fixindents_action,
   expand_action,
   expandxy_action,
   expandxy_code_action,
   smart_delete_next_character_action,
   toggle_editable_action,

   goto_definition_action,
   goto_implementation_action,
   goto_reference_action,
   goto_doc_action,
   goto_search_action,
   goto_type_action,
   class_search_action,
   bud_action,
   fragment_action,

   quick_fix_action,
   marquis_comment_action,
   block_comment_action,
   javadoc_comment_action,
   fix_errors_action,
   goto_next_error_action,
   infer_decl_action
};


private static SwingKey [] skey_defs = new SwingKey[] {
   new SwingKey("CODEEDIT",null,findDefaultAction(copyAction),"menu C"),
   new SwingKey("CODEEDIT",null,findDefaultAction(cutAction),"menu X"),
   new SwingKey("CODEEDIT",null,findDefaultAction(selectAllAction),"menu A"),
   new SwingKey("CODEEDIT",null,undo_action,"menu Z"),
   new SwingKey("CODEEDIT",null,redo_action,"menu Y"),
   new SwingKey("CODEEDIT",null,undo_selection_action,"menu shift Z"),
   new SwingKey("CODEEDIT",null,redo_selection_action,"menu shift Y"),
   new SwingKey("CODEEDIT",null,backspace_action,"BACK_SPACE"),
   new SwingKey("CODEEDIT",null,tab_action,"TAB","shift TAB"),
   new SwingKey("CODEEDIT",null,newline_action,"ENTER"),
   new SwingKey("CODEEDIT",null,toggle_insert_action,"INSERT", "xalt I","F14"),
   new SwingKey("CODEEDIT",null,find_action,"menu F"),
   new SwingKey("CODEEDIT",null,replace_action,"menu R"),
   new SwingKey("CODEEDIT",null,find_next_action,"menu K"),
   new SwingKey("CODEEDIT",null,find_prev_action,"xalt K"),
   new SwingKey("CODEEDIT",null,delete_line_action,"menu D","menu shift K"),
   new SwingKey("CODEEDIT",null,delete_to_eol_action,"menu shift D"),
   new SwingKey("CODEEDIT",null,duplicate_action,"menu alt D"),
   new SwingKey("CODEEDIT",null,insert_line_above_action,"menu shift ENTER"),
   new SwingKey("CODEEDIT",null,insert_line_below_action,"shift ENTER"),
   new SwingKey("CODEEDIT",null,indent_lines_action,"menu I"),
   new SwingKey("CODEEDIT",null,join_lines_action,"menu alt J"),
   new SwingKey("CODEEDIT",null,comment_lines_action,"menu SLASH"),
   new SwingKey("CODEEDIT",null,start_line_action,"HOME"),
   new SwingKey("CODEEDIT",null,start_line_select_action,"shift HOME"),
   new SwingKey("CODEEDIT",null,end_line_action,"END"),
   new SwingKey("CODEEDIT",null,end_line_select_action,"shift END"),
   new SwingKey("CODEEDIT",null,next_brace_action,"menu CLOSE_BRACKET"),
   new SwingKey("CODEEDIT",null,save_all_action,"menu S"),
   new SwingKey("CODEEDIT",null,save_action,"menu shift S"),
   new SwingKey("CODEEDIT",null,revert_action,"xalt shift S"),
   new SwingKey("CODEEDIT",null,force_save_action,"alt menu shift S"),
   new SwingKey("CODEEDIT",null,explicit_elision_action,"menu E"),
   new SwingKey("CODEEDIT",null,redo_elision_action,"menu shift E"),
   new SwingKey("CODEEDIT",null,remove_elision_action,"menu alt E"),
   new SwingKey("CODEEDIT",null,smart_paste_action,"menu V"),
   new SwingKey("CODEEDIT",null,findDefaultAction(pasteAction),"menu shift V"),
   new SwingKey("CODEEDIT",null,move_lines_down_action,"xalt DOWN"),
   new SwingKey("CODEEDIT",null,move_lines_up_action,"xalt UP"),
   new SwingKey("CODEEDIT",null,rename_action,"xalt shift R"),
   new SwingKey("CODEEDIT",null,extract_method_action,"xalt shift M"),
   new SwingKey("CODEEDIT",null,select_word_action,"xalt W"),
   new SwingKey("CODEEDIT",null,select_line_action,"xalt shift W"),
   new SwingKey("CODEEDIT",null,select_paragraph_action,"menu alt B"),
   new SwingKey("CODEEDIT",null,format_action,"menu shift F"),
   new SwingKey("CODEEDIT",null,fixindents_action,"menu shift I"),
   new SwingKey("CODEEDIT",null,expand_action,"xalt B"),
   new SwingKey("CODEEDIT",null,expandxy_action,"xalt shift B","menu shift B"),
   new SwingKey("CODEEDIT",null,expandxy_code_action,"menu ctrl shift B"),
   new SwingKey("CODEEDIT",null,fix_errors_action,"menu shift P", "xalt shift P"),
   new SwingKey("CODEEDIT",null,autocomplete_action,"ctrl SPACE"),
   new SwingKey("CODEEDIT",null,finish_action,"yalt SPACE"),
   new SwingKey("CODEEDIT",null,goto_implementation_action,"F3"),
   new SwingKey("CODEEDIT",null,goto_type_action,"menu F3","menu shift H"),
   new SwingKey("CODEEDIT",null,goto_definition_action,"menu shift F3"),
   new SwingKey("CODEEDIT",null,goto_doc_action,"shift F3"),
   new SwingKey("CODEEDIT",null,goto_search_action,"menu F4", "shift F4"),
   new SwingKey("CODEEDIT",null,goto_reference_action,"F4", "menu shift G"),
   new SwingKey("CODEEDIT",null,class_search_action,"menu shift T"),
   new SwingKey("CODEEDIT",null,bud_action,"menu shift X","xalt X"),
   new SwingKey("CODEEDIT",null,fragment_action,"menu shift C","xalt C"),
   new SwingKey("CODEEDIT",null,block_comment_action,"xalt shift C"),
   new SwingKey("CODEEDIT",null,marquis_comment_action,"xalt shift Q"),
   new SwingKey("CODEEDIT",null,marquis_comment_action,"menu shift Q"),
   new SwingKey("CODEEDIT",null,javadoc_comment_action,"xalt shift J"),
   new SwingKey("CODEEDIT",null,quick_fix_action,"menu 1","alt 1"),
   new SwingKey("CODEEDIT",null,infer_decl_action,"menu shift W"),
   new SwingKey("CODEEDIT",null,smart_delete_next_character_action,"shift DELETE"),
   new SwingKey("CODEEDIT",null,toggle_editable_action,"menu F15"),
   new SwingKey("CODEEDIT",null,goto_next_error_action,"menu shift N"),
};


private static Keymap	bale_keymap;

static {
   Keymap dflt = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
   // this no longer works.  The input bindings are in an input map
   SwingText.fixKeyBindings(dflt);

   bale_keymap = JTextComponent.addKeymap("BALE",dflt);
   for (SwingKey sk : skey_defs) {
      sk.addToKeyMap(bale_keymap);
    }

   bale_keymap.setDefaultAction(default_key_action);
}


private static Action findDefaultAction(String name)
{
   DefaultEditorKit ek = new DefaultEditorKit();
   Action [] supact = ek.getActions();
   for (int i = 0; i < supact.length; ++i) {
      String nm = (String) supact[i].getValue(Action.NAME);
      if (nm != null && nm.equals(name)) return supact[i];
    }

   return null;
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleEditorKit(BoardLanguage lang)
{
   if (lang == null) language_kit = null;
   else {
      language_kit = BaleLanguageKitDefault.getKitForLanguage(lang);
    }

   if (language_kit != null) {
      bale_keymap = language_kit.getKeymap(bale_keymap);
    }

   bale_actions = null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public synchronized Action [] getActions()
{
   if (bale_actions == null) {
      Action [] supact = super.getActions();
      Action [] subact = getLanguageActions();
      bale_actions = new Action[supact.length + local_actions.length + subact.length];
      int ct = 0;
      for (int i = 0; i < supact.length; ++i) bale_actions[ct++] = supact[i];
      for (int j = 0; j < local_actions.length; ++j) bale_actions[ct++] = local_actions[j];
      for (int k = 0; k < subact.length; ++k) bale_actions[ct++] = subact[k];
    }

   return bale_actions;
}


Action [] getLanguageActions()
{
   Action [] langacts = null;
   if (language_kit != null) langacts = language_kit.getActions();

   if (langacts == null) return new Action[0];
   return langacts;
}




/********************************************************************************/
/*										*/
/*	Keymap actions								*/
/*										*/
/********************************************************************************/

void setupKeyMap(JTextComponent tc)
{
   tc.setKeymap(bale_keymap);
}



static Action findAction(String name)
{
   BoardSetup bs = BoardSetup.getSetup();
   return findAction(name,bs.getLanguage());
}




static Action findAction(String name,BoardLanguage lang)
{
   BaleLanguageKit lkit = null;
   if (lang != null) {
      lkit = BaleLanguageKitDefault.getKitForLanguage(lang);
    }
   if (lkit != null) {
      Action [] langacts = lkit.getActions();
      for (int i = 0; i < langacts.length; ++i) {
	 String nm = (String) langacts[i].getValue(Action.NAME);
	 if (nm != null && nm.equals(name)) return langacts[i];
       }
    }

   if (local_actions != null) {
      for (int i = 0; i < local_actions.length; ++i) {
	 String nm = (String) local_actions[i].getValue(Action.NAME);
	 if (nm != null && nm.equals(name)) return local_actions[i];
       }
    }

   DefaultEditorKit ek = new DefaultEditorKit();
   Action [] supact = ek.getActions();
   for (int i = 0; i < supact.length; ++i) {
      String nm = (String) supact[i].getValue(Action.NAME);
      if (nm != null && nm.equals(name)) return supact[i];
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	View Factory methods							*/
/*										*/
/********************************************************************************/

@Override public ViewFactory getViewFactory()	{ return this; }



@Override public View create(Element elem)
{
   if (elem == null) return null;
   if (!(elem instanceof BaleElement)) return new ParagraphView(elem);

   // BoardLog.logD("BALE","Create view for " + elem.getName());

   BaleElement be = (BaleElement) elem;
   String prehint = null;
   String posthint = null;
   BaleHinter bh = language_kit.getHinter();
   if (bh != null) {
      prehint = bh.getPreHint(be);
      posthint = bh.getPostHint(be);
    }

   View rslt = null;
   switch (be.getViewType()) {
      case NONE :
	 break;
      case TEXT :
	 rslt = new BaleViewText(be);
	 break;
      case BLOCK :
	 rslt = new BaleViewBlock(be);
	 break;
      case CODE :
	 // note: not currently used
	 rslt = new BaleViewCode(be);
	 break;
      case LINE :
	 rslt = new BaleViewLine(be);
	 break;
      case ORPHAN :
	 rslt = new BaleViewOrphan(be);
	 break;
      case HINT :
	 rslt = new BaleViewHint(be);
	 break;
    }

   if (prehint != null || posthint != null) {
      BoxView bv = new BaleViewHintBox(be);
      if (prehint != null) {
	 BaleElement.HintElement he = new BaleElement.HintElement(be,prehint,be.getStartOffset());
	 bv.append(new BaleViewHint(he));
       }
      bv.append(rslt);
      if (posthint != null) {
	 BaleElement.HintElement he = new BaleElement.HintElement(be,posthint,be.getStartOffset());
	 bv.append(new BaleViewHint(he));
       }
      rslt = bv;
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Subclass for Fragment Editors						*/
/*										*/
/********************************************************************************/

static class FragmentKit extends BaleEditorKit {

   private BaleDocumentIde base_document;
   private BaleFragmentType fragment_type;
   private transient List<BaleRegion> fragment_regions;

   private static final long serialVersionUID = 1;

   FragmentKit(BaleDocumentIde base,BaleFragmentType typ,List<BaleRegion> rgns) {
      super(base.getLanguage());
      base_document = base;
      fragment_type = typ;
      fragment_regions = rgns;
    }

   @Override public Document createDefaultDocument() {
      return new BaleDocumentFragment(base_document,fragment_type,fragment_regions);
    }

}	// end of inner class FragmentKit





/********************************************************************************/
/*										*/
/*	Utilities for action management 					*/
/*										*/
/********************************************************************************/

static BaleEditorPane getBaleEditor(ActionEvent e)
{
   if (e == null) return null;

   return (BaleEditorPane) e.getSource();
}



static boolean checkEditor(BaleEditorPane e)
{
   if (e == null) return false;
   if (!e.isEditable()) return false;
   if (!e.isEnabled()) return false;

   BaleDocument bd = e.getBaleDocument();
   if (!bd.isEditable()) return false;
   if (bd.isOrphan()) {
      bd.baleWriteLock();
      try {
	 e.setEditable(false);
       }
      finally { bd.baleWriteUnlock(); }
      return false;
    }

   return true;
}



static boolean checkReadEditor(BaleEditorPane e)
{
   if (e == null) return false;
   if (!e.isEnabled()) return false;
   BaleDocument bd = e.getBaleDocument();
   if (bd.isOrphan()) {
      bd.baleWriteLock();
      try {
	 if (e.isEditable()) e.setEditable(false);
       }
      finally { bd.baleWriteUnlock(); }
      return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Text character actions :: default keys					*/
/*										*/
/********************************************************************************/

private static class DefaultKeyAction extends TextAction {

   private static final long serialVersionUID = 1;

   DefaultKeyAction() {
      super("DefaultKeyAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.baleWriteLock();
      try {
	 Dimension d0 = target.getSize();
	 if (d0.height >= BALE_MAX_GROW_HEIGHT) d0 = null;
	 else d0 = target.getPreferredSize();

	 String content = e.getActionCommand();
	 String postcontent = null;
	 int mod = e.getModifiers();
	 if ((content != null) && (content.length() > 0) &&
	       ((mod & ActionEvent.ALT_MASK) == (mod & ActionEvent.CTRL_MASK))) {
	    char c = content.charAt(0);
	    int soff = target.getSelectionStart();
	    int eoff = target.getSelectionEnd();

	    if ((c >= 0x20) && (c != 0x7F)) {
	       boolean overwrite = target.getOverwriteMode();
	       if (soff == eoff && !overwrite) {
		  if (language_kit.checkContent(content) &&
			bd.checkTypeover(content,soff)) {
		     overwrite = true;
		   }
		  else {
		     postcontent = language_kit.getPostContent(content);
		     if (postcontent != null &&
			   BALE_PROPERTIES.getBoolean(BALE_AUTO_INSERT_SPACES_ONLY)) {
			String follow = null;
			try {
			   follow = target.getText(eoff,1);
			 }
			catch (BadLocationException ex) { }
			if (follow != null && !Character.isWhitespace(follow.charAt(0))) {
			   postcontent = null;
			 }
		      }
		   }
		}
	       if (overwrite) {
		  if (soff == target.getSelectionEnd()) {
		     String prev = null;
		     try {
			prev = target.getText(soff,1);
		      }
		     catch (BadLocationException ex) { }
		     eoff = soff+1;
		     if (prev == null || prev.equals("\n"));
		     else if (prev.equals("\t")) {
			int cpos = bd.getColumnPosition(soff);
			int npos = bd.getNextTabPosition(cpos);
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < npos-cpos; ++i) buf.append(" ");
			target.setSelectionEnd(eoff);
			target.replaceSelection(buf.toString());
			target.setSelectionStart(soff);
			target.setSelectionEnd(eoff);
		      }
		     else target.setSelectionEnd(eoff);
		   }
		}
	       if (soff != eoff) bd.handleReplaceTypeover(soff,eoff);

	       target.replaceSelection(content);
	       if (postcontent != null) {
		  int off = target.getSelectionEnd();
		  try {
		     bd.insertString(off,postcontent,null);
		   }
		  catch (BadLocationException ex) { }
		  bd.setCreatedTypeover(postcontent,off);
		  target.setSelectionStart(off);
		  target.setSelectionEnd(off);
		}

	       if (content != null && shouldAutoIndent(target,content,soff)) {
		  // TODO: check that this is the only thing on the line
		  indent_lines_action.actionPerformed(e);
		}
	       BaleCompletionContext ctx = target.getCompletionContext();
	       if (ctx == null && isCompletionTrigger(c) && !target.getOverwriteMode()) {
		  new BaleCompletionContext(target,soff,c);
		}

	       if (d0 != null) {
		  Dimension d1 = target.getPreferredSize();
		  if (d1.height > d0.height) {
		     target.increaseSize(1);
		   }
		}
	     }
	  }
       }
      finally { bd.baleWriteUnlock(); }
   }

}	// end of inner class DefaultKeyAction




private static boolean shouldAutoIndent(BaleEditorPane target, String content, int pos)
{
   // TODO: Really want to look at the line with content and check for only {, }, case, ...
   // when content is the last character of that

   if (content.equals("{")) return isFirstCharacter(target,pos);
   if (content.equals("}")) return isFirstCharacter(target,pos);
   if (content.equals("e")) return matchesText(target,pos,"case");
   if (content.equals("t")) return matchesText(target,pos,"default");
   if (content.equals("s")) return matchesText(target,pos,"throws");

   return false;
}



private static boolean isFirstCharacter(BaleEditorPane target,int pos)
{
   if (pos == 0) return true;

   int delta = Math.min(80,pos);
   String txt;
   try {
      txt = target.getText(pos-delta,delta);
    }
   catch (BadLocationException e) {
      return false;
   }

   for (int i = txt.length()-1; i >= 0; --i) {
      char ch = txt.charAt(i);
      if (ch == '\n') return true;
      if (!Character.isWhitespace(ch)) return false;
    }
   return true;
}



private static boolean matchesText(BaleEditorPane target,int pos,String match)
{
   if (pos < match.length()) return false;

   int delta = Math.min(80,pos);
   String txt;
   try {
      txt = target.getText(pos-delta,delta+1);
    }
   catch (BadLocationException e) {
      return false;
   }

   if (txt == null) return false;

   for (int i = txt.length()-1; i >= 0; --i) {
      char ch = txt.charAt(i);
      if (ch == '\n') {
	 String line = txt.substring(i+1).trim();
	 return line.equals(match);
       }
    }

   return false;
}



private static boolean isCompletionTrigger(char c)
{
   return c == '(' || c == '.';
}



/********************************************************************************/
/*										*/
/*	Text character actions :: other actions 				*/
/*										*/
/********************************************************************************/

private static class BackspaceAction extends TextAction {

   private transient Action delete_prev_action;
   private transient Action backward_action;

   private static final long serialVersionUID = 1;

   BackspaceAction() {
      super("BackspaceAction");
      delete_prev_action = findAction(deletePrevCharAction);
      backward_action = findAction(backwardAction);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (target == null) ;
      else if (target.getOverwriteMode()) backward_action.actionPerformed(e);
      else {
	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 if (soff == eoff && BALE_PROPERTIES.getBoolean(BALE_AUTO_INSERT_CLOSE)) {
	    try {
	       String del = target.getText(soff-1,1);
	       String d1 = language_kit.getPostContent(del);
	       if (d1 != null) {
		  if (bd.checkTypeover(d1,soff)) {
		     bd.remove(soff,1);
		   }
		}
	     }
	    catch (BadLocationException ex) { }

	  }
	 delete_prev_action.actionPerformed(e);
       }
    }

}	// end of inner class BackspaceAction



private static class ToggleInsertAction extends TextAction {

   private static final long serialVersionUID = 1;

   ToggleInsertAction() {
      super("ToggleInsertAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (target != null) {
	 BoardMetrics.noteCommand("BALE","ToggleInsert");
	 target.setOverwriteMode(!target.getOverwriteMode());
       }
    }

}	// end of inner class ToggleInsertAction




private static class TabAction extends TextAction {

   private static final long serialVersionUID = 1;

   TabAction() {
      super("TabAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane tgt = getBaleEditor(e);
      if (!checkEditor(tgt)) return;

      BaleDocument bd = tgt.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int soff = tgt.getSelectionStart();
	 int eoff = tgt.getSelectionEnd();
	 if (soff != eoff) {
	    int slno = bd.findLineNumber(soff);
	    int elno = bd.findLineNumber(eoff);
	    if (slno != elno) {
	       if (elno < slno) {
		  int x = elno;
		  elno = slno;
		  slno = x;
		}
	       for (int i = slno; i <= elno; ++i) {
		  bd.fixLineIndent(i);
		}
	       return;
	     }
	  }

	 int cpos = bd.getColumnPosition(soff);
	 int pos = bd.getNextTabPosition(cpos);
	 if (tgt.getOverwriteMode()) {
	    int npos = eoff;
	    int mod = e.getModifiers();
	    for (int i = 0; i < pos-cpos; ++i) {
	       ++npos;
	       if (eoff != soff || ((mod & ActionEvent.SHIFT_MASK) != 0)) {
		  tgt.setSelectionEnd(npos);
	       }
	       else tgt.setSelectionStart(npos);
	    }
	  }
	 else {
	    StringBuffer buf = new StringBuffer();
	    for (int i = cpos; i < pos; ++i) buf.append(" ");
	    tgt.replaceSelection(buf.toString());
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class TabAction




private static class NewlineAction extends TextAction {

   private static final long serialVersionUID = 1;

   NewlineAction() {
      super("NewlineAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;

      BaleDocument bd = target.getBaleDocument();
      bd.baleWriteLock();
      try {
	 String text = "\n";
	 String posttext = null;
	 int postdelta = 0;
	 int size = 1;
	 int postsize = 0;
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 BaleElement elt = bd.getCharacterElement(eoff);
	 if (elt != null && elt.isComment()) {
	    switch (elt.getEndTokenState()) {
	       case IN_COMMENT :
	       case IN_FORMAL_COMMENT :
		  String indtxt = " ";
		  BaleElement be1 = elt.getPreviousCharacterElement();
		  if (be1 != null) {
		     BaleElement.Indent bin = be1.getIndent();
		     if (bin != null) {
			int col = bin.getFirstColumn();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < col; ++i) buf.append(" ");
			indtxt = buf.toString();
		      }
		     else if (be1.isComment()) {
			try {
			   String ctxt = target.getText(elt.getStartOffset(),
				 elt.getEndOffset()-elt.getStartOffset()+1);
			   int ct = 0;
			   while (ctxt.charAt(ct) == ' ') ++ct;
			   indtxt = ctxt.substring(0,ct);
			 }
			catch (BadLocationException ex) { }
		      }
		   }
		  text += indtxt + "*";
		  break;
	       default:
		  break;
	     }
	  }

	 String tok = doAutoClose(bd,elt,soff,eoff);
	 if (tok != null) {
	    posttext= "\n" + tok;
	    postsize = 1;
	    try {
	       String txt = target.getText(eoff,100);
	       boolean havetxt = false;
	       for (int i = 0; i < txt.length(); ++i) {
		  if (txt.charAt(i) == '\n') {
		     if (havetxt) postdelta = i;
		     break;
		   }
		  else if (!Character.isWhitespace(txt.charAt(i))) havetxt = true;
		}
	     }
	    catch (BadLocationException ex) { }
	  }
	 if (tok == null) {
	    posttext = doAutoLine(bd,elt,soff,eoff);
	    postdelta = 0;
	    if (posttext != null && posttext.contains("\n")) postsize = 1;
	  }

	 boolean grow = true;
	 boolean rep = true;
	 boolean ind = true;

	 if (soff != eoff) {
	    int slno = bd.findLineNumber(soff);
	    int elno = bd.findLineNumber(eoff);
	    if (elno != slno) grow = false;
	  }
	 else {
	    if (target.getOverwriteMode()) {
	       rep = false;
	       grow = false;
	       int nlno = bd.findLineNumber(soff) + 1;
	       int pos = bd.getFirstNonspace(nlno);
	       if (pos >= 0) ind = false;
	       else pos = -pos;
	       if (pos < bd.getEndPosition().getOffset()) target.setCaretPosition(pos);
	       soff = pos-1;
	     }
	  }

	 if (rep) target.replaceSelection(text);
	 int noff = target.getSelectionStart();
	 if (posttext != null) {
	    try {
	       bd.insertString(noff+postdelta, posttext, null);
	       int nlno = bd.findLineNumber(noff+1+postdelta);
	       for (int i = 0; i < postsize; ++i) {
		  bd.fixLineIndent(nlno+i);
		}
	       size += postsize;
	     }
	    catch (BadLocationException ex) { }
	    target.setSelectionStart(noff);
	    target.setSelectionEnd(noff);
	  }
	 if (ind) {
	    int lno = bd.findLineNumber(soff+1);
	    bd.fixLineIndent(lno);
	  }

	 if (grow) {
	    target.increaseSize(size);
	  }
       }
      finally { bd.baleWriteUnlock(); }
   }

}	// end of inner class NewlineAction



private static String doAutoClose(BaleDocument bd,BaleElement elt,int soff,int eoff)
{
   if (!BALE_PROPERTIES.getBoolean("Bale.autoclose")) return null;
   if (elt == null) return null;
   BaleElement e1 = elt.getPreviousCharacterElement();
   if (e1 == null) return null;

   BaleTokenType left = e1.getTokenType();
   BaleTokenType right = null;
   String token = null;
   switch (left) {
      case LBRACE :
	 right = BaleTokenType.RBRACE;
	 token = "}";
	 break;
      case LBRACKET :
	 right = BaleTokenType.RBRACKET;
	 token = "]";
	 break;
      default :
	 break;
    }
   if (right == null) return null;

   int bct = 0;
   int act = 0;
   for (BaleElement e3 = elt; e3 != null; e3 = e3.getNextCharacterElement()) {
      if (e3.getTokenType() == left) ++bct;
      else if (e3.getTokenType() == right) --bct;
    }
   for (BaleElement e4 = elt.getPreviousCharacterElement(); e4 != null; e4 = e4.getPreviousCharacterElement()) {
      if (e4.getTokenType() == left) ++act;
      else if (e4.getTokenType() == right) --act;
    }
   // System.err.println("TOKENS : " + act + " " + bct);
   if (act+bct == 0) return null;
   if (act + bct > 0) return token;

   return null;
}

private static String doAutoLine(BaleDocument bd,BaleElement elt,int soff,int eoff)
{
   if (elt == null) return null;

   BaleTokenType look = null;
   switch (elt.getTokenType()) {
      case RBRACE :
	 look = BaleTokenType.LBRACE;
	 break;
      case RBRACKET :
	 look = BaleTokenType.LBRACKET;
	 break;
      case RPAREN :
	 look = BaleTokenType.LPAREN;
	 break;
      default :
	 return null;
    }
   BaleElement e1 = elt.getPreviousCharacterElement();
   if (e1 == null) return null;
   if (e1.getTokenType() == look) return "\n";

   return null;
}




/********************************************************************************/
/*										*/
/*	Text editing actions							*/
/*										*/
/********************************************************************************/

private static class DeleteLineAction extends TextAction {

   private static final long serialVersionUID = 1;

   DeleteLineAction() {
      super("DeleteLineAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;

      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int slno = bd.findLineNumber(soff);
	 int sdel = bd.findLineOffset(slno);
	 int eoff = be.getSelectionEnd();
	 int elno = (eoff == soff ? slno : bd.findLineNumber(eoff));
	 int edel = bd.findLineOffset(elno+1);

	 if (edel != sdel) {
	    be.setSelectionStart(sdel);
	    be.setSelectionEnd(edel);
	    be.cut();
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class DeleteLineAction


private static class SmartDeleteNextCharacterAction extends TextAction {

   private static final long serialVersionUID = 1;

   SmartDeleteNextCharacterAction() {
      super("SmartDeleteNextCharacterAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();
	 if (eoff != soff) {
	    be.cut();
	  }
	 else if (soff < bd.getLength()) {
	    int ct = 1;
	    if (soff < bd.getLength()-1) {
	       String dotchars = bd.getText(soff,2);
	       char c0 = dotchars.charAt(0);
	       char c1 = dotchars.charAt(1);
	       if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
		     c1 >= '\uDC00' && c1 <= '\uDFFF') {
		  ct = 2;
		}
	       else if (c0 == '\n') {
		  int len = 0;
		  int ext = Math.min(bd.getLength()-1-soff,120);
		  String eolchars = bd.getText(soff,ext);
		  for (int i = 0; i < eolchars.length(); ++i) {
		     char c = eolchars.charAt(i);
		     if (!Character.isWhitespace(c)) break;
		     else ++len;
		   }
		  bd.replace(soff,len," ",null);
		  return;
		}
	       bd.remove(soff,ct);
	     }
	  }
       }
      catch (BadLocationException ex) { }
      finally { bd.baleWriteUnlock(); }
    }


}





private static class DeleteToEolAction extends TextAction {

   private static final long serialVersionUID = 1;

   DeleteToEolAction() {
      super("DeleteToEolAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int sdel = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();
	 int elno = bd.findLineNumber(eoff);
	 int edel = bd.findLineOffset(elno+1) - 1;

	 try {
	    if (edel != sdel) bd.remove(sdel,edel-sdel);
	  }
	 catch (BadLocationException ex) { }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class DeleteToEolAction





private static class InsertLineAboveAction extends TextAction {

   private static final long serialVersionUID = 1;

   InsertLineAboveAction() {
      super("InsertLineAboveAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int slno = bd.findLineNumber(soff);
	 int sins = bd.findLineOffset(slno);
	 try {
	    bd.insertString(sins,"\n",null);
	  }
	 catch (BadLocationException ex) { }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class InsertLineAboveAction





private static class InsertLineBelowAction extends TextAction {

   private static final long serialVersionUID = 1;

   InsertLineBelowAction() {
      super("InsertLineBelowAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionEnd();
	 int slno = bd.findLineNumber(soff);
	 int sins = bd.findLineOffset(slno+1);
	 try {
	    bd.insertString(sins,"\n",null);
	  }
	 catch (BadLocationException ex) { }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class InsertLineBelowAction





private static class IndentLinesAction extends TextAction {

   private static final long serialVersionUID = 1;

   IndentLinesAction() {
      super("IndentLinesAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();

	 int slno = bd.findLineNumber(soff);
	 int elno = slno;
	 if (eoff != soff) elno = bd.findLineNumber(eoff);
	 if (elno < slno) {
	    int x = elno;
	    elno = slno;
	    slno = x;
	  }

	 // might want to do a few lines at a time by puttins
	 // separate tasks into Swing event queue?

	 for (int i = slno; i <= elno; ++i) {
	    bd.fixLineIndent(i);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class IndentLinesAction


private static class FixIndentsAction extends TextAction {

   private static final long serialVersionUID = 1;

   FixIndentsAction() {
      super("FixIndentsAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();
	 int dsoff = bd.mapOffsetToEclipse(soff);
	 int deoff = bd.mapOffsetToEclipse(eoff);

	 BumpClient bc = BumpClient.getBump();
	 org.w3c.dom.Element edits = bc.fixIndents(bd.getProjectName(),bd.getFile(),
	       bd.getEditCounter(),dsoff,deoff);

	 if (edits != null) {
	    BaleApplyEdits bae = new BaleApplyEdits(bd);
	    bae.applyEdits(edits);
	  }
	 else {
	    bd.nextEditCounter();
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class FixIndentsAction




private static class JoinLinesAction extends TextAction {

   private static final long serialVersionUID = 1;

   JoinLinesAction() {
      super("JoinLinesAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();

	 int slno = bd.findLineNumber(soff);
	 int elno = slno;
	 if (eoff != soff) elno = bd.findLineNumber(eoff);
	 if (elno < slno) {
	    int x = elno;
	    elno = slno;
	    slno = x;
	  }
	 if (elno == slno) elno = slno+1;

	 for (int i = slno; i < elno; ++i) {
	    joinLineWithNext(be,bd,i);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

   private void joinLineWithNext(BaleEditorPane be,BaleDocument bd,int lno) {
      int soff = bd.findLineOffset(lno+1);
      int bdel = soff-1;		// delete the newline
      int edel = bd.getFirstNonspace(lno+1);
      if (edel < 0) edel = soff;
      try {
	 bd.remove(bdel,edel-bdel);
	 bd.insertString(bdel," ",null);
       }
      catch (BadLocationException e) { }
    }

}	// end of inner class JoinLinesAction



private static class DuplicateAction extends TextAction {

   private static final long serialVersionUID = 1;

   DuplicateAction() {
       super("DuplicateAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();
	 if (soff == eoff) return;
	 String text = bd.getText(soff,eoff);
	 bd.insertString(eoff,text,null);
       }
      catch (BadLocationException ex) { }
      finally { bd.baleWriteUnlock(); }
    }

}



private static class SmartPasteAction extends TextAction {

   private static final long serialVersionUID = 1;

   SmartPasteAction() {
      super("SmartPasteAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int p0 = target.getSelectionStart();
	 int p1 = target.getSelectionEnd();
	 Position epos = null;
	 try {
	    epos = bd.createPosition(p1+1);
	 }
	 catch (BadLocationException ex) { }

	 SwingEditorPane src = target.getClipboardSource();
	 if (src != target && src != null) {
	    BudaRoot br = BudaRoot.findBudaRoot(target);
	    BudaBubble b1 = BudaRoot.findBudaBubble(src);
	    BudaBubble b2 = BudaRoot.findBudaBubble(target);
	    if (br != null && b1 != null && b2 != null) br.noteBubbleCopy(b1,b2);
	  }

	 target.paste();

	 int slno = bd.findLineNumber(p0);
	 if (epos != null) p1 = epos.getOffset();
	 else p1 = p0;
	 int elno = bd.findLineNumber(p1);
	 for (int i = slno; i <= elno; ++i) {
	    bd.fixLineIndent(i);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class SmartPasteAction




private static class MoveLinesAction extends TextAction {

   private int move_direction;

   private static final long serialVersionUID = 1;

   MoveLinesAction(int dir) {
      super((dir > 0 ? "MoveLinesDownAction" : "MoveLinesUpAction"));
      move_direction = dir;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      BoardMetrics.noteCommand("BALE","MoveLines");

      bd.baleWriteLock();
      try {
	 int spos = target.getSelectionStart();
	 int epos = target.getSelectionEnd();
	 int slno = bd.findLineNumber(spos);
	 spos = bd.findLineOffset(slno);
	 int elno = bd.findLineNumber(epos);
	 epos = bd.findLineOffset(elno+1);

	 try {
	    String txt = bd.getText(spos,epos-spos);
	    bd.remove(spos,epos-spos);
	    int nlno = slno + move_direction;
	    int ipos = bd.findLineOffset(nlno);
	    bd.insertString(ipos,txt,null);
	    target.setSelectionStart(ipos);
	    target.setSelectionEnd(ipos + txt.length());
	  }
	 catch (BadLocationException ex) {
	    return;
	  }
	 //TODO: Should we indent here?
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class MoveLinesAction




private static class CommentLinesAction extends TextAction {

   private static final long serialVersionUID = 1;


   CommentLinesAction() {
      super("CommentLinesAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      BoardMetrics.noteCommand("BALE","CommentLines");

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();

	 int slno = bd.findLineNumber(soff);
	 int elno = slno;
	 if (eoff != soff) elno = bd.findLineNumber(eoff);
	 if (elno < slno) {
	    int x = elno;
	    elno = slno;
	    slno = x;
	  }

	 int loff1 = bd.findLineOffset(slno);
	 BaleElement ce1 = bd.getCharacterElement(loff1);
	 while (ce1.isEmpty() && !ce1.isComment() && !ce1.isEndOfLine()) {
	    ce1 = ce1.getNextCharacterElement();
	  }
	 boolean remcmmt = ce1.getName().equals("LineComment");

	 LinkedList<Integer> fixups = new LinkedList<>();
	 for (int i = elno; i >= slno; --i) {
	    int loff = bd.findLineOffset(i);
	    BaleElement ce = bd.getCharacterElement(loff);
	    Indent ind = ce.getIndent();
	    while (ce.isEmpty() && !ce.isComment() && !ce.isEndOfLine()) {
	       ce = ce.getNextCharacterElement();
	     }

	    try {
	       if (ce.getName().equals("LineComment")) {
		  if (remcmmt) {
		     int noff = ce.getStartOffset();
		     bd.remove(noff,2);
		     fixups.addFirst(i);
		   }
		}
	       else if (!remcmmt) {
		  if (ind != null && ind.getFirstColumn() >= 3) {
		     int sto = ind.getStartOffset();
		     int eno = ind.getEndOffset();
		     String txt = bd.getText(sto,eno-sto);
		     if (txt.charAt(0) == '\t') {
			bd.insertString(loff,"// ",null);
		      }
		     else if (txt.charAt(1) == '\t') {
			bd.replace(loff,1,"//",null);
		      }
		     else {
			bd.replace(loff,2,"//",null);
		      }
		   }
		  else {
		     bd.insertString(loff,"// ",null);
		   }
		}
	       else {
		  // already commented -- comment again so its symmetric
		  bd.insertString(loff,"// ",null);
		}
	     }
	    catch (BadLocationException ex) {
	       return;
	     }
	  }
	 for (Integer iv : fixups) {
	    bd.fixLineIndent(iv);
	 }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class CommentLinesAction




/********************************************************************************/
/*										*/
/*	Search actions								*/
/*										*/
/********************************************************************************/

private static class FindAction extends TextAction {

   private boolean do_replace;

   private static final long serialVersionUID = 1;

   FindAction(boolean rep) {
      super((rep ? "ReplaceAction" : "FindAction"));
      do_replace = rep;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE","Find");
      BaleFinder fb = target.getFindBar();
      fb.setReplace(do_replace);
      target.toggleFindBar();
      fb.getComponent().repaint();
    }

}	// end of inner class FindAction




private static class FindNextAction extends TextAction {

   private int find_direction;

   private static final long serialVersionUID = 1;

   FindNextAction(int dir) {
      super(dir > 0 ? "FindNextAction" : "FindPrevAction");
      find_direction = dir;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      BaleFinder fb = target.getFindBar();
      fb.find(find_direction,true);
    }

}	// end of inner class FindNextAction




/********************************************************************************/
/*										*/
/*	Cursor actions								*/
/*										*/
/********************************************************************************/

private static class StartLineAction extends TextAction {

   private boolean do_select;

   private static final long serialVersionUID = 1;

   StartLineAction(boolean select) {
      super(select ? "StartLineSelectAction" : "StartLineAction");
      do_select = select;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE","StartLine");
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = Math.abs(bd.getFirstNonspace(lno));
      int boff = bd.findLineOffset(lno);
      if (off == soff && soff != boff) soff = boff;
      if (do_select) target.moveCaretPosition(soff);
      else target.setCaretPosition(soff);
    }

}	// end of inner class StartLineAction






private static class BeginLineAction extends TextAction {

   private boolean do_select;

   private static final long serialVersionUID = 1;

   BeginLineAction(boolean select) {
      super(select ? selectionBeginLineAction : beginLineAction);
      do_select = select;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = bd.findLineOffset(lno);
      if (do_select) target.moveCaretPosition(soff);
      else target.setCaretPosition(soff);
    }

}	// end of inner class BeginLineAction





private static class EndLineAction extends TextAction {

   private boolean do_select;

   private static final long serialVersionUID = 1;

   EndLineAction(boolean select) {
      super(select ? selectionEndLineAction : endLineAction);
      do_select = select;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = bd.findLineOffset(lno+1)-1;
      if (soff < 0) return;
      if (do_select) target.moveCaretPosition(soff);
      else target.setCaretPosition(soff);
    }

}	// end of inner class EndLineAction



private static class NextItemAction extends TextAction {

   private BaleTokenType look_for;

   private static final long serialVersionUID = 1;

   NextItemAction(BaleTokenType tt) {
      super("NextItemAction_" + tt.toString());
      look_for = tt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      BaleDocument bd = target.getBaleDocument();
      bd.baleReadLock();
      try {
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 BaleElement elt = bd.getCharacterElement(eoff);
	 while (elt != null) {
	    if (elt.getTokenType() == look_for) {
	       int npos = elt.getEndOffset();
	       if (soff == eoff) {
		  target.setSelectionStart(npos);
		}
	       target.setSelectionEnd(npos);
	       break;
	     }
	    elt = elt.getNextCharacterElement();
	  }
       }
      finally { bd.baleReadUnlock(); }
    }

}	// end of inner class NextItemAction




/********************************************************************************/
/*										*/
/*	Selection actions							*/
/*										*/
/********************************************************************************/

private static class SelectLineAction extends TextAction {

   private static final long serialVersionUID = 1;

   SelectLineAction() {
      super(selectLineAction);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = bd.findLineOffset(lno);
      int eoff = bd.findLineOffset(lno+1)-1;
      if (soff >= 0) {
	 target.setCaretPosition(soff);
	 target.moveCaretPosition(eoff);
       }
    }

}	// end of inner class SelectLineAction



private static class SelectWordAction extends TextAction {

   private static final long serialVersionUID = 1;

   private enum CharType { ALNUM, PUNCT, WHITE };

   SelectWordAction() {
      super(selectWordAction);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      BaleElement be = bd.getCharacterElement(soff);
      if (be == null) return;
      int esoff = be.getStartOffset();
      if (esoff < 0) return;
      int eeoff = be.getEndOffset();

      if (be.isComment() || be.getTokenType() == BaleTokenType.STRING) {
	 if (eeoff >= esoff) {
	    try {
	       String toktxt = target.getText(esoff,eeoff-esoff);
	       int pos = soff - esoff;
	       int spos = pos;
	       int epos = pos;
	       char base = toktxt.charAt(pos);
	       CharType btype = getCharType(base);
	       for ( ; spos > 0; --spos) {
		  char c = toktxt.charAt(spos-1);
		  if (getCharType(c) != btype) break;
		}
	       for ( ; epos < toktxt.length()-1; ++epos) {
		  char c = toktxt.charAt(epos+1);
		  if (getCharType(c) != btype) break;
		}
	       eeoff = esoff + epos + 1;
	       esoff = esoff + spos;
	     }
	    catch (BadLocationException ex) { }
	  }
       }

      target.setCaretPosition(esoff);
      if (eeoff < 0) return;
      target.moveCaretPosition(eeoff);
    }

   private CharType getCharType(char c) {
      if (Character.isJavaIdentifierPart(c)) return CharType.ALNUM;
      if (Character.isWhitespace(c)) return CharType.WHITE;
      if (Character.isUnicodeIdentifierPart(c)) return CharType.ALNUM;
      return CharType.PUNCT;
    }





}	// end of inner class SelectWordAction



private static class SelectParagraphAction extends TextAction {

   private static final long serialVersionUID = 1;

   SelectParagraphAction() {
      super(selectParagraphAction);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      BaleElement be = bd.getCharacterElement(soff);
      while (be != null && (be.isLeaf() || be.isLineElement())) {
	 be = be.getBaleParent();
       }
      if (be == null) return;
      target.setCaretPosition(be.getStartOffset());
      target.moveCaretPosition(be.getEndOffset());
    }

}	// end of inner class SelectParagraphAction



/********************************************************************************/
/*										*/
/*	File actions								*/
/*										*/
/********************************************************************************/

private static class SaveAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SaveAction() {
      super("SaveAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.save();
      BoardMetrics.noteCommand("BALE","Save");
    }

}	// end of inner class SaveAction





private static class RevertAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RevertAction() {
      super("RevertAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.revert();
      BoardMetrics.noteCommand("BALE","Revert");
    }

}	// end of inner class SaveAction




private static class ForceSaveAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ForceSaveAction() {
      super("ForceSaveAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (target == null) return;
      try {
	 BaleDocument bd = target.getBaleDocument();
	 BaleDocument basedoc = bd.getBaseEditDocument();
	 int len = basedoc.getLength();
	 String body = basedoc.getText(0,len);
	 basedoc.replace(0,len,body,null);
	 BoardMetrics.noteCommand("BALE","ForceSave");
       }
      catch (BadLocationException ex) { }
    }

}	// end of inner class ForceSaveAction




private static class SaveAllAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SaveAllAction() {
      super("SaveAllAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (target == null) return;
      BudaRoot br = BudaRoot.findBudaRoot(target);
      if (br == null) return;
      BoardMetrics.noteCommand("BALE","SaveAll");

      SaveAllRun sar = new SaveAllRun(br);

      // sar.run();
      BoardThreadPool.start(sar);
    }

}	// end of inner class SaveAllAction


private static class SaveAllRun implements Runnable {

   private BudaRoot for_root;

   SaveAllRun(BudaRoot br) {
      for_root = br;
    }

   @Override public void run() {
      BowiFactory.startTask();
      try {
	 // BumpClient.getBump().saveAll();
	 for_root.handleSaveAllRequest();
	 BumpClient bc = BumpClient.getBump();
	 boolean comp = bc.getOptionBool("lspbase.lsp.compileOnSave",true);
	 if (comp) {
	    BumpClient.getBump().compile(false,false,false);
	  }
	 try {
	    for_root.saveConfiguration(null);
	  }
	 catch (IOException ex) { }
       }
      finally { BowiFactory.stopTask(); }
    }

}	// end of inner class SaveAllRun




/********************************************************************************/
/*										*/
/*	Elision actions 							*/
/*										*/
/********************************************************************************/

private static class RedoElisionAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RedoElisionAction() {
      super("RedoElisionAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      BowiFactory.startTask();
      try {
	 bd.baleWriteLock();
	 try {
	    bd.redoElision();
	    bd.handleElisionChange();
	    target.getPreferredSize();
	    bd.fixElisions();
	  }
	 finally { bd.baleWriteUnlock(); }
       }
      finally { BowiFactory.stopTask(); }
      BoardMetrics.noteCommand("BALE","RedoElision");
      BaleEditorBubble.noteElision(target);
    }

}	// end of inner class RedoElisionAction




private static class RemoveElisionAction extends AbstractAction {

   private boolean code_only;
   private static final long serialVersionUID = 1;

   RemoveElisionAction(boolean code) {
      super(code ? "RemoveCodeElisionAction" : "RemoveElisionAction");
      code_only = code;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask();
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkEditor(target)) return;
	 BaleDocument bd = target.getBaleDocument();
	 bd.baleWriteLock();
	 try {
	    BaleElideMode em = bd.getElideMode();
	    if (code_only) bd.removeCodeElision();
	    else bd.removeElision();
	    bd.handleElisionChange();
	    bd.setElideMode(em);
	  }
	 finally { bd.baleWriteUnlock(); }
	 BoardMetrics.noteCommand("BALE","RemoveElision");
	 BaleEditorBubble.noteElision(target);
       }
      finally { BowiFactory.stopTask(); }
    }

}	// end of inner class RemoveElisionAction




private static class ExplicitElisionAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ExplicitElisionAction() {
      super("ExplicitElisionAction");
   }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      BowiFactory.startTask();
      try {
	 bd.baleWriteLock();
	 try {
	    int off = target.getCaretPosition();
	    BaleElement.Branch br = bd.getParagraphElement(off);
	    BaleElement ebr = null;
	    if (br.isElided()) {
	       br.setElided(false);
	       ebr = br;
	     }
	    else {
	       while (br != null && !br.canElide()) {
		  br = br.getBaleParent();
		}
	       if (br == null) return;
	       br.setElided(true);
	     }
	    bd.handleElisionChange();
	    if (ebr != null) target.increaseSizeForElidedElement(ebr);
	  }
	 finally { bd.baleWriteUnlock(); }
       }
      finally { BowiFactory.stopTask(); }
      BoardMetrics.noteCommand("BALE","ExplicitElision");
      BaleEditorBubble.noteElision(target);
    }

}	// end of inner class ExplicitElisionAction





/********************************************************************************/
/*										*/
/*	Completion actions							*/
/*										*/
/********************************************************************************/

private static class AutoCompleteAction extends TextAction  {

   private static final long serialVersionUID = 1;

   AutoCompleteAction() {
      super("AutoCompleteAction");
   }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleCompletionContext ctx = target.getCompletionContext();
      if (ctx == null) {
	 int sel = target.getSelectionStart();
	 new BaleCompletionContext(target,sel-1,'1');
	 BoardMetrics.noteCommand("BALE","AutoCompleteIt");
       }
      else {
	 ctx.handleSelected();
	 BoardMetrics.noteCommand("BALE","AutoComplete");
       }
   }
}


private static class FinishAction extends TextAction {

   private static final long serialVersionUID = 1;

   FinishAction() {
      super("FinishAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.baleWriteLock();
      // consider maintaining a stack of characters to add
      // for example '(' with parenct >= 0, adds ')'
      //
      try {
	 int eoff = target.getSelectionEnd();
	 BaleElement elt = bd.getCharacterElement(eoff);
	 int parenct = 0;
	 int bracect = 0;
	 int bracketct = 0;
	 int semict = 0;
	 int colonct = 0;
	 List<BaleTokenType> totype = new ArrayList<>();
	 loop: for (BaleElement e4 = elt.getPreviousCharacterElement();
	    e4 != null;
	    e4 = e4.getPreviousCharacterElement()) {
	    switch (e4.getTokenType()) {
	       case LBRACE :
		  if (!totype.isEmpty()) break loop;
		  if (bracect >= 0) totype.add(BaleTokenType.RBRACE);
		  ++bracect;
		  break;
	       case RBRACE :
		  --bracect;
		  break;
	       case LPAREN :
		  if (parenct >= 0) totype.add(BaleTokenType.RPAREN);
		  ++parenct;
		  break;
	       case RPAREN :
		  --parenct;
		  break;
	       case LBRACKET :
		  if (bracketct >= 0) totype.add(BaleTokenType.RBRACKET);
		  ++bracketct;
		  break;
	       case RBRACKET :
		  --bracketct;
		  break;
	       case FOR :
		  if (!totype.isEmpty()) break loop;
		  if (colonct == 0 && semict == 1) {
		     totype.add(BaleTokenType.SEMICOLON);
		     break loop;
		   }
		  break;
	       case WHILE :
	       case DO :
	       case CASE :
	       case CATCH :
	       case THROWS :
	       case KEYWORD :
	       case FINALLY :
	       case IF :
	       case ELSE :
	       case IMPORT :
	       case PACKAGE :
	       case RETURN :
	       case SWITCH :
	       case SYNCHRONIZED :
	       case TRY :
	       case BREAK :
	       case CONTINUE :
	       case FUNCTION :
		  if (!totype.isEmpty()) break loop;
		  break;
	       case SEMICOLON :
		  ++semict;
		  if (!totype.isEmpty()) break loop;
		  break;
	       case COLON :
		  ++colonct;
		  break;
	       case EOL :
		  break;
	       default :
		  break;
	     }
	  }
	 BoardLog.logD("BALE","FINISH " + parenct + " " + bracect + " " + bracketct + " " + totype);
	 BaleElement e3 = elt;
	 BaleElement start = elt;
	 boolean haveeol = false;
	 while (!totype.isEmpty()) {
	    while (e3 != null) {
	       if (e3.isEndOfLine()) haveeol = true;
	       if (e3.isComment() || e3.isEmpty()) ;
	       else break;
	       e3 = e3.getNextCharacterElement();
	     }
	    if (e3 != null && e3.getTokenType() == totype.get(0)) {
	       if (!haveeol || e3.getTokenType() == BaleTokenType.RBRACE) {
		  totype.remove(0);
		  start = e3;
		}
	       else break;
	     }
	    else break;
	  }
	 if (totype.isEmpty()) {
	    if (e3 != null && e3.getTokenType() != BaleTokenType.SEMICOLON && !haveeol) {
	       totype.add(BaleTokenType.SEMICOLON);
	     }
	  }
	 else haveeol = true;
	 if (!totype.isEmpty()) {
	    String add = "";
	    for (BaleTokenType tt : totype) {
	       switch (tt) {
		  case RBRACE :
		     add += "}";
		     break;
		  case RPAREN :
		     add += ")";
		     break;
		  case RBRACKET :
		     add += "]";
		     break;
		  case SEMICOLON :
		     add += ";";
		     if (!haveeol) add += " ";
		     break;
		  default :
		     break;
		}
	     }
	    eoff = start.getStartOffset();
	    int noff = eoff + add.length();
	    bd.insertString(eoff,add,null);
	    target.setSelectionStart(noff);
	    target.setSelectionEnd(noff);
	  }
       }
      catch (BadLocationException ex) { }
      finally { bd.baleWriteUnlock(); }
   }
}	// end of inner class FinishAction



/********************************************************************************/
/*										*/
/*	Refactoring actions							*/
/*										*/
/********************************************************************************/

private static class RenameAction extends TextAction {

   private static final long serialVersionUID = 1;

   RenameAction() {
      super("RenameAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      BaleDocument bd = target.getBaleDocument();
      if (!checkEditor(target)) return;
      int soff = target.getSelectionStart();
      int eoff = target.getSelectionEnd();

      BaleElement be = bd.getCharacterElement(soff);
      if (be == null) return;
      if (!be.isIdentifier()) return;
      if (eoff != soff) {
	 BaleElement xbe = bd.getCharacterElement(eoff-1);
	 if (xbe != be) return;
       }

      BaleRenameContext ctx = target.getRenameContext();
      if (ctx == null) {
	 int sel = target.getSelectionStart();
	 new BaleRenameContext(target,sel);
       }

      BoardMetrics.noteCommand("BALE","Rename");
    }

}	// end of inner class RenameAction



private static class ExtractMethodAction extends TextAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   ExtractMethodAction() {
      super("ExtractMethodAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      BaleDocument bd = target.getBaleDocument();
      if (!checkEditor(target)) return;

      int spos = target.getSelectionStart();
      int epos = target.getSelectionEnd();
      int slno = bd.findLineNumber(spos);
      spos = bd.findLineOffset(slno);
      int elno = bd.findLineNumber(epos);
      epos = bd.findLineOffset(elno+1)-1;
      String cnts;
      try {
	 cnts = bd.getText(spos,epos-spos);
       }
      catch (BadLocationException ex) {
	 BoardLog.logE("BALE","Problem getting extract text",ex);
	 return;
       }

      BudaBubble bbl = BudaRoot.findBudaBubble(target);
      if (bbl == null) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));

      String fnm = bd.getFragmentName();
      String aft = null;
      String cls = null;
      switch (bd.getFragmentType()) {
	 case FILE :
	 case NONE :
	    return;
	 case METHOD :
	    aft = fnm;
	    cls = fnm;
	    int idx1 = cls.indexOf("(");
	    if (idx1 >= 0) cls = cls.substring(0,idx1);
	    idx1 = cls.lastIndexOf(".");
	    if (idx1 >= 0) cls = cls.substring(0,idx1);
	    break;
	 case FIELDS :
	 case STATICS :
	 case MAIN :
	 case HEADER :
	    cls = fnm;
	    int idx2 = cls.lastIndexOf(".");
	    cls = cls.substring(0,idx2);
	    break;
	 default:
	    break;
       }

      if (cls == null) return;

      BuenoProperties props = new BuenoProperties();
      props.put(BuenoConstants.BuenoKey.KEY_CONTENTS,cnts);
      BuenoLocation loc = BuenoFactory.getFactory().createLocation(bd.getProjectName(),cls,aft,true);

      BuenoFactory.getFactory().createMethodDialog(bbl,null,props,loc,
						      "Enter Signature of Extracted Method",this);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,name);
      if (bb == null) return;
      bba.addBubble(bb,null,p,PLACEMENT_MOVETO|PLACEMENT_NEW);
   }

}	// end of inner class ExtractMethodAction




/********************************************************************************/
/*										*/
/*	Formatting Actions							*/
/*										*/
/********************************************************************************/

private static class FormatAction extends TextAction {

   private static final long serialVersionUID = 1;

   FormatAction() {
      super("FormatAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));

      bd.format(target.getSelectionStart(),target.getSelectionEnd());
    }

}	// end of inner class FormatAction




private static class ExpandAction extends TextAction {

   private static final long serialVersionUID = 1;

   ExpandAction() {
      super("ExpandAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      javax.swing.plaf.TextUI tui = target.getUI();
      View root = tui.getRootView(target);
      View v1 = root.getView(0);
      int vx = (int) (Math.ceil(v1.getMaximumSpan(View.X_AXIS)+0.5));
      Dimension d = target.getSize();
      if (vx <= d.width) return;
      BudaBubble bb = BudaRoot.findBudaBubble(target);
      if (bb == null) return;
      Dimension d1 = bb.getSize();
      d1.width += vx - d.width + 10;
      bb.setSize(d1);
    }

}	// end of inner class ExpandAction





private static class ExpandXYAction extends TextAction {

   private boolean code_only;
   private static final long serialVersionUID = 1;

   ExpandXYAction(boolean code) {
      super(code ? "ExpandXYCodeAction" : "ExpandXYAction");
      code_only = code;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      javax.swing.plaf.TextUI tui = target.getUI();
      View root = tui.getRootView(target);
      View v1 = root.getView(0);
      int vx = (int) (Math.ceil(v1.getMaximumSpan(View.X_AXIS)+0.5));
      int vy = (int) (Math.ceil(v1.getMaximumSpan(View.Y_AXIS)+0.5));
      vy = Math.min(vy,800);
      BudaBubble bb = BudaRoot.findBudaBubble(target);
      if (bb == null) return;
      Dimension d1 = bb.getSize();

      if (vx > d1.width || vy >= d1.height) {
	 d1.width += Math.max(0,vx - d1.width + 10);
	 d1.height += Math.max(0, vy - d1.height + 30);
	 bb.setSize(d1);
       }

      if (code_only) remove_code_elision_action.actionPerformed(e);
      else remove_elision_action.actionPerformed(e);
   }

}	// end of inner class ExpandAction




/********************************************************************************/
/*										*/
/*	Goto Actions								*/
/*										*/
/********************************************************************************/

private static class GotoDefinitionAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoDefinitionAction() {
      super("GotoDefinitionAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask();
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;

	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 if (eoff != soff) eoff = soff;

	 BumpClient bc = BumpClient.getBump();
	 List<BumpLocation> locs;
	 BalePosition sp;
	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findDefinition(null,			   // bd.getProjectName(),
					bd.getFile(),
					bd.mapOffsetToEclipse(soff),
					bd.mapOffsetToEclipse(soff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    goto_search_action.actionPerformed(e);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    if (e.getActionCommand() == null) {
	       e = new ActionEvent(target,e.getID(),
		     "GotoDefinitionAction",e.getWhen(),e.getModifiers());
	     }
	    goto_doc_action.actionPerformed(e);
	    return;
	  }

	 if (locs.size() == 1) {
	    BumpLocation loc0 = locs.get(0);
	    int foff = bd.mapOffsetToEclipse(soff);
	    if (foff >= loc0.getOffset() && foff <= loc0.getEndOffset()) {
	       goto_reference_action.actionPerformed(e);
	       return;
	     }
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,true,locs,BudaLinkStyle.STYLE_SOLID);
       }
      finally { BowiFactory.stopTask(); }
      BoardMetrics.noteCommand("BALE","GoToDefinition");
    }

}	// end of inner class GotoDefinitionAction




private static class GotoImplementationAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoImplementationAction() {
      super("GotoImplementationAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask();
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;
	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 if (eoff != soff) eoff = soff;

	 BumpClient bc = BumpClient.getBump();
	 List<BumpLocation> locs;
	 BalePosition sp;
	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findImplementations(null,
					     bd.getFile(),
					     bd.mapOffsetToEclipse(soff),
					     bd.mapOffsetToEclipse(eoff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    goto_search_action.actionPerformed(e);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    if (e.getActionCommand() == null) {
	       e = new ActionEvent(target,e.getID(),"GotoImplementationAction",e.getWhen(),e.getModifiers());
	     }
	    goto_doc_action.actionPerformed(e);
	    return;
	  }

	 if (locs.size() == 1) {
	    BumpLocation loc0 = locs.get(0);
	    int foff = bd.mapOffsetToEclipse(soff);
	    if (foff >= loc0.getOffset() && foff <= loc0.getEndOffset()) {
	       goto_reference_action.actionPerformed(e);
	       return;
	     }
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,true,locs,BudaLinkStyle.STYLE_SOLID);
       }
      finally { BowiFactory.stopTask(); }
      BoardMetrics.noteCommand("BALE","GoToImplementation");
    }

}	// end of inner class GotoImplementationAction




private static class GotoReferenceAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoReferenceAction() {
      super("GotoReferenceAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask();
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;
	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 if (eoff != soff) eoff = soff;
	 BaleElement be = bd.getCharacterElement(soff);

	 BumpClient bc = BumpClient.getBump();
	 Collection<BumpLocation> locs;
	 BalePosition sp;

	 String fullnm = null;

	 fullnm  = bc.getFullyQualifiedName(null,		   // bd.getProjectName(),
	       bd.getFile(),
	       bd.mapOffsetToEclipse(soff),
	       bd.mapOffsetToEclipse(eoff),60000);

	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findReferences(null,
					bd.getFile(),
					bd.mapOffsetToEclipse(soff),
					bd.mapOffsetToEclipse(eoff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (fullnm == null) {
	    if (be.isIdentifier())
	       BaleInfoBubble.createInfoBubble(target, be.getName(), BaleInfoBubbleType.UNDEFINED, sp);
	    else {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),
		     BaleInfoBubbleType.NOIDENTIFIER, sp);
	     }
	    Action act = findAction(beepAction);
	    if (act != null) act.actionPerformed(e);
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    goto_search_action.actionPerformed(e);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    BaleInfoBubble.createInfoBubble(target, be.getName(), BaleInfoBubbleType.REF, sp);
	    Action act = findAction(beepAction);
	    if (act != null) act.actionPerformed(e);
	    return;
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,false,BaleStackType.DROP_SOURCE,
	       locs,BudaLinkStyle.STYLE_FLIP_REFERENCE);
       }
      finally { BowiFactory.stopTask(); }
      BoardMetrics.noteCommand("BALE","GoToReference");
    }

}	// end of inner class GotoReferenceAction




private static class GotoDocAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoDocAction() {
      super("GotoDocAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      int eoff = target.getSelectionEnd();
      if (eoff != soff) eoff = soff;
      BumpClient bc = BumpClient.getBump();

      String fullnm = null;

      fullnm  = bc.getFullyQualifiedName(bd.getProjectName(),bd.getFile(),
					    bd.mapOffsetToEclipse(soff),
					    bd.mapOffsetToEclipse(eoff),60000);

      BalePosition sp = null;
      try {
	 sp = (BalePosition) bd.createPosition(soff);
       }
      catch (BadLocationException ex) { }

      BaleElement be = bd.getCharacterElement(soff);

      if (fullnm != null) {
	 // BoardLog.logD("BALE","FIND DOCUMENTATION FOR " + fullnm);
	 BudaBubble bb = BudaRoot.createDocumentationBubble(fullnm);
	 if (bb != null) {
	    BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(target);
	    BudaBubble obbl = BudaRoot.findBudaBubble(target);
	    if (bba == null || obbl == null) return;

	    Point lp = null;
	    BudaConstants.LinkPort port0 = null;
	    if (sp != null) {
	       port0 = new BaleLinePort(target,sp,"Find Link");
	       lp = port0.getLinkPoint(obbl,obbl.getLocation());
	     }
	    else port0 = new BudaDefaultPort(BudaPortPosition.BORDER_EW,true);

	    bba.addBubble(bb,target,lp,PLACEMENT_PREFER|PLACEMENT_MOVETO|PLACEMENT_NEW);

	    /****************
	    BudaRoot root = BudaRoot.findBudaRoot(target);
	    Rectangle loc = BudaRoot.findBudaLocation(target);
	    if (lp != null) loc.y = lp.y;
	    root.add(bb,new BudaConstraint(loc.x+loc.width+BUBBLE_CREATION_SPACE,loc.y));
	    ****************/

	    BudaConstants.LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	    BudaBubbleLink lnk = new BudaBubbleLink(obbl,port0,bb,port1);
	    bba.addLink(lnk);
	    BoardMetrics.noteCommand("BALE","GoToDocumentation");
	    return;
	  }
	 else {
	    if (e.getActionCommand() == null) {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.DOC, sp);
	     }
	    else if (e.getActionCommand().equals("GotoImplementationAction")) {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.IMPLDOC, sp);
	     }
	    else if (e.getActionCommand().equals("GotoDefinitionAction")) {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.DEFDOC, sp);
	     }
	    else {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.DOC, sp);
	     }
	  }
       }
      else {
	 if (be.isIdentifier())
	    BaleInfoBubble.createInfoBubble(target, be.getName(), BaleInfoBubbleType.UNDEFINED, sp);
	 else
	    BaleInfoBubble.createInfoBubble(target, be.getName(), BaleInfoBubbleType.NOIDENTIFIER, sp);
       }


      Action act = findAction(beepAction);
      if (act != null) act.actionPerformed(e);
      BoardMetrics.noteCommand("BALE","GoToDocumentation");
    }

}	// end of inner class GotoDocAction




private static class GotoSearchAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoSearchAction() {
      super("GotoSearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      int eoff = target.getSelectionEnd();

      BumpClient bc = BumpClient.getBump();
      Collection<BumpLocation> locs = bc.findDefinition(null,
							   bd.getFile(),
							   bd.mapOffsetToEclipse(soff),
							   bd.mapOffsetToEclipse(eoff));
      if (locs == null || locs.size() == 0) return;

      handleSearchAction(e,locs);

      BoardMetrics.noteCommand("BALE","GoToSearch");
    }

}	// end of inner class GotoSearchAction







private static class GotoTypeAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoTypeAction() {
      super("GotoTypeAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask();
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;

	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();

	 BumpClient bc = BumpClient.getBump();
	 Collection<BumpLocation> locs;
	 BalePosition sp;
	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findTypeDefinition(bd.getProjectName(),
					    bd.getFile(),
					    bd.mapOffsetToEclipse(soff),
					    bd.mapOffsetToEclipse(eoff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    handleSearchAction(e,locs);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    if (e.getActionCommand() == null) {
	       e = new ActionEvent(target,e.getID(),"GotoTypeAction",e.getWhen(),e.getModifiers());
	     }
	    goto_doc_action.actionPerformed(e);
	    return;
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,true,locs,BudaLinkStyle.STYLE_SOLID);
       }
      finally { BowiFactory.stopTask(); }
      BoardMetrics.noteCommand("BALE","GoToType");
    }

}	// end of inner class GotoTypeAction




private static class ClassSearchAction extends TextAction {

   private static final long serialVersionUID = 1;

   ClassSearchAction() {
      super("ClassSearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;

      String nm = "C:";

      BudaRoot br = BudaRoot.findBudaRoot(target);
      Rectangle r = BudaRoot.findBudaLocation(target);
      if (br != null && r != null) {
	 Point pt = new Point(r.x+r.width+BUBBLE_CREATION_SPACE,r.y);
	 br.createMergedSearchBubble(pt,null,nm);
	 BoardMetrics.noteCommand("BALE","ClassSearch");
       }
    }

}	// end of inner class ClassSearchAction




private static boolean doClassSearchAction(Collection<BumpLocation> locs)
{
   if (locs == null || locs.size() == 0) return false;

   BumpSymbolType bst = null;
   BumpLocation baseloc = null;
   File fil = null;

   for (BumpLocation bl : locs) {
      BumpSymbolType nst = bl.getSymbolType();
      if (nst != BumpSymbolType.CLASS && nst != BumpSymbolType.ENUM &&
		  nst != BumpSymbolType.THROWABLE &&
		  nst != BumpSymbolType.INTERFACE &&
		  nst != BumpSymbolType.PACKAGE) return false;
      if (bst == null) bst = nst;
      else if (bst != nst) return false;
      baseloc = bl;
      File nfil = bl.getFile();
      if (fil != null && nfil != null && !nfil.equals(fil)) return false;
      fil = nfil;
    }

   if (bst == null || baseloc == null) return false;

   if (locs.size() == 2) {
      BumpLocation fnd = null;
      for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
	 BumpLocation xl = it.next();
	 if (fnd != null) {
	    if (fnd.getKey().equals(xl.getKey())) {
	       it.remove();
	     }
	  }
	 fnd = xl;
       }
    }

   if (locs.size() > 1) return true;

   int len = baseloc.getDefinitionEndOffset() - baseloc.getDefinitionOffset();
   if (baseloc.getDefinitionOffset() < 0) return true;
   File f = baseloc.getFile();
   if (!f.exists()) return true;

   switch (bst) {
      case CLASS :
      case THROWABLE :
	 if (len < 1000) return false;
	 break;
      case PACKAGE :
	 break;
      case INTERFACE :
	 if (len < 1500) return false;
	 break;
      case ENUM :
	 if (len < 2000) return false;
	 break;
      default:
	 break;
    }

   return true;
}



private static void handleSearchAction(ActionEvent e,Collection<BumpLocation> locs)
{
   BaleEditorPane target = getBaleEditor(e);
   if (!checkReadEditor(target)) return;
   BaleDocument bd = target.getBaleDocument();

   if (locs == null || locs.size() == 0) return;

   BumpLocation bloc = null;
   for (BumpLocation bl : locs) {
      bloc = bl;
      break;
    }

   if (bloc == null) return;

   String nm = bloc.getSymbolName();
   if (nm == null) return;
   int idx = nm.lastIndexOf(".");

   switch (bloc.getSymbolType()) {
      case ENUM_CONSTANT :
      case FIELD :
      case FUNCTION :
      case CONSTRUCTOR :
      case GLOBAL :
	 nm = nm.substring(0,idx);
	 break;
      default :
	 return;
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
      case MODULE :
	 int idx2 = nm.indexOf("<");
	 if (idx2 >= 0) nm = nm.substring(0,idx2);
	 break;
      case PACKAGE :
	 break;
    }

   BudaRoot br = BudaRoot.findBudaRoot(target);
   Rectangle r = BudaRoot.findBudaLocation(target);
   if (br != null && r != null) {
      Rectangle r2 = br.getCurrentViewport();
      int searchsize = 200;
      Point pt = new Point(r.x+r.width+BUBBLE_CREATION_SPACE,r.y);
      if (pt.x + searchsize > r2.x + r2.width) pt.x = r.x - BUBBLE_CREATION_SPACE - searchsize;
      br.createMergedSearchBubble(pt,bd.getProjectName(),nm);
    }
}




/********************************************************************************/
/*										*/
/*	Error handling actions							*/
/*										*/
/********************************************************************************/

private static class QuickFixAction extends TextAction {

   private static final long serialVersionUID = 1;

   QuickFixAction() {
      super("QuickFixAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      // TODO: merge with BaleFactory.QuickFix
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      List<BumpProblem> probs = bd.getProblemsAtLocation(soff);
      if (probs == null) return;

      List<BaleFixer> fixes = new ArrayList<BaleFixer>();
      for (BumpProblem bp : probs) {
	 if (bp.getFixes() != null) {
	    for (BumpFix bf : bp.getFixes()) {
	       BaleFixer fixer = new BaleFixer(bp,bf);
	       if (fixer.isValid()) fixes.add(fixer);
	     }
	  }
       }
      if (fixes.isEmpty()) return;

      BaleFixer fix = null;
      if (fixes.size() == 1) fix = fixes.get(0);
      else {
	 Collections.sort(fixes);
	 Object [] fixalts = fixes.toArray();
	 fix = (BaleFixer) JOptionPane.showInputDialog(target,"Select Quick Fix","Quick Fix Selector",
							  JOptionPane.QUESTION_MESSAGE,
							  null,fixalts,fixes.get(0));
       }
      if (fix == null) return;

      fix.actionPerformed(e);
      BoardMetrics.noteCommand("BALE","QuickFix");
   }

}	// end of inner class QuickFixAction



private static class GotoNextErrorAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoNextErrorAction() {
      super("GotoNextError");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BumpClient bc = BumpClient.getBump();
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      int lno = bd.findLineNumber(soff);
      int slno = bd.findLineNumber(0);
      int elno = bd.findLineNumber(bd.getLength()-1);
      File f = bd.getFile();
      List<BumpProblem> probs = bc.getProblems(f);
      BumpProblem best = null;
      int blno = -1;
      for (BumpProblem bp : probs) {
	 if (bp.getLine() > lno && bp.getLine() <= elno) {
	    if (blno < 0 || blno > bp.getLine()) {
	       blno = bp.getLine();
	       best = bp;
	     }
	  }
       }
      if (best == null) {
	 for (BumpProblem bp : probs) {
	    if (bp.getLine() >= slno) {
	       if (blno < 0 || blno > bp.getLine()) {
		  blno = bp.getLine();
		  best = bp;
		}
	     }
	  }
       }
      if (best == null || blno < 0) return;
      int pos = bd.findLineOffset(blno);
      if (pos < 0) return;

      bd.baleWriteLock();
      try {
	 target.setCaretPosition(pos);
	 target.scrollRectToVisible(SwingText.modelToView2D(target,pos));
	 BaleElement cur = bd.getCharacterElement(pos);
	 // might want to set cur to first non-space
	 if (cur == null) return;
	 if (cur.isElided()) {
	    cur.setElided(false);
	    bd.handleElisionChange();
	    target.increaseSizeForElidedElement(cur);
	    BoardMetrics.noteCommand("BALE","GotoErrorUnElision");
	    BaleEditorBubble.noteElision(target);
	  }
       }
      catch (BadLocationException ex) { }
      finally {
	 bd.baleWriteUnlock();
       }
    }

}	// end of inner class GotoNextErrorAction


/********************************************************************************/
/*										*/
/*	Comment insertion actions						*/
/*										*/
/********************************************************************************/

private static class CommentAction extends TextAction {

   private BuenoType new_type;

   private static final long serialVersionUID = 1;

   CommentAction(String nm,BuenoType newtype) {
      super(nm);
      new_type = newtype;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;

      BuenoFactory bf = BuenoFactory.getFactory();
      BuenoLocation bl = new CommentLocation(be,be.getSelectionStart());
      bf.createNew(new_type,bl,null);
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
    }

}	// end of inner class CommentAction



private static class CommentLocation extends BuenoLocation {

   private BaleEditorPane editor_pane;
   private int cur_offset;

   CommentLocation(BaleEditorPane be,int offset) {
      editor_pane = be;
      cur_offset = editor_pane.getBaleDocument().getDocumentOffset(offset);
    }

   @Override public String getProject() {
      return editor_pane.getBaleDocument().getProjectName();
    }
   @Override public File getFile() {
      return editor_pane.getBaleDocument().getFile();
    }

   @Override public int getOffset()		{ return cur_offset; }

}	// end of inner class CommentLocation





/********************************************************************************/
/*										*/
/*	Error fix actions							*/
/*										*/
/********************************************************************************/

private static class FixErrorsAction extends TextAction {

   private transient Method fix_method;

   private static final long serialVersionUID = 1;

   FixErrorsAction() {
      super("FixErrorsInRegion");
      try {
	 Class<?> c = Class.forName("edu.brown.cs.bubbles.bfix.BfixFactory");
	 fix_method = c.getMethod("fixErrorsInRegion",BaleWindowDocument.class,int.class,int.class);
      }
      catch (Exception e) { }

    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      int soff = be.getSelectionStart();
      int eoff = be.getSelectionEnd();
      if (soff == eoff) {
	 BaleDocument bd = be.getBaleDocument();
	 int lno = bd.findLineNumber(soff);
	 soff = bd.findLineOffset(lno);
	 eoff = bd.findLineOffset(lno+1);
       }
      BoardMetrics.noteCommand("BALE",String.valueOf(getValue(Action.NAME)));
      if (fix_method != null) {
	 BowiFactory.startTask();
	 try {
	    fix_method.invoke(null,be.getBaleDocument(),soff,eoff);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BALE","Problem invoking fix errors",t);
	  }
	 finally {
	    BowiFactory.stopTask();
	  }
      }
    }

}	// end of inner class CommentAction




/********************************************************************************/
/*										*/
/*	Infer declaration action						*/
/*										*/
/********************************************************************************/

private static class InferDeclarationAction extends TextAction {

   private static final long serialVersionUID = 1;

   InferDeclarationAction() {
      super("InferDeclaration");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BoardMetrics.noteCommand("BALE","InferDeclaration");
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      BumpClient bc = BumpClient.getBump();
      org.w3c.dom.Element typ = bc.getExpectedType(bd.getProjectName(),bd.getFile(),lno);
      if (typ == null) return;
      String ins = IvyXml.getAttrString(typ,"NAME") + " ";
      int insoff = IvyXml.getAttrInt(typ,"OFFSET");
      int insoff1 = bd.mapOffsetToJava(insoff);
      bd.baleWriteLock();
      try {
	 bd.insertString(insoff1,ins,null);
       }
      catch (BadLocationException ex) { }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class InferDeclarationAction



/********************************************************************************/
/*										*/
/*	Python backspace action 						*/
/*										*/
/********************************************************************************/

private static class PythonBackspaceAction extends TextAction {

   private static final long serialVersionUID = 1;

   private transient Action use_backspace_action;

   PythonBackspaceAction() {
      super("PythonBackspaceAction");
      use_backspace_action = null;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (use_backspace_action == null) {
	 use_backspace_action = BaleEditorKit.findAction("BackspaceAction");
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
	 use_backspace_action.actionPerformed(e);
	 return;
       }

      BaleIndenter bind = bd.getIndenter();
      int oind = bind.getCurrentIndentationAtOffset(soff);
      int tind = bind.getDesiredIndentation(soff);

      if (tind != oind || oind == 0) {
	 use_backspace_action.actionPerformed(e);
	 return;
       }
      int delta = bind.getUnindentSize();
      if (delta > oind) delta = oind;
      for (int i = 0; i < delta; ++i) {
	 use_backspace_action.actionPerformed(e);
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

   private transient Action forward_action;

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



/********************************************************************************/
/*										*/
/*	Toggle editable <-> readonly						*/
/*										*/
/********************************************************************************/

private static class ToggleEditableAction extends TextAction {

   private static final long serialVersionUID = 1;

   ToggleEditableAction() {
      super("ToggleEditableAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = BaleEditorKit.getBaleEditor(e);
      if (!BaleEditorKit.checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      if (bd instanceof BaleDocumentFragment) {
	 BaleDocumentFragment bdf = (BaleDocumentFragment) bd;
	 if (bdf.isEditable()) bdf.setEditable(false);
	 else bdf.setEditable(true);
       }
    }

}	// end of inner class ToggleEditableAction



}	// end of class BaleEditorKit





/* end of BaleEditorKit.java */
