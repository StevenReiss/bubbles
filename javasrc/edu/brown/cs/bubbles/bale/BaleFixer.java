/********************************************************************************/
/*										*/
/*		BaleFixer.java							*/
/*										*/
/*	Bubble Annotated Language Editor quick fix manager			*/
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

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.burp.BurpHistory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.StringTokenizer;


class BaleFixer extends AbstractAction implements BaleConstants, BumpConstants,
	Comparable<BaleFixer>
{



/********************************************************************************/
/*										*/
/*	Local interfaces							*/
/*										*/
/********************************************************************************/

interface Fixup {

   void perform(ActionEvent evt);
   String getLabel();
   double getRelevance();

}

/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BumpProblem	for_problem;
private Fixup		using_code;

private static BaleFactory bale_factory = BaleFactory.getFactory();

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleFixer(BumpProblem bp,BumpFix bf)
{
   for_problem = bp;
   using_code = null;

   switch (bf.getType()) {
      case NONE :
	 break;
      case NEW_METHOD :
	 using_code = new NewMethodFixup(this,bf);
	 break;
      case EDIT_FIX :
	 using_code = new EditBasedFixup(this,bf);
	 break;
    }

   if (using_code != null) {
      String lbl = using_code.getLabel();
      if (lbl == null) using_code = null;
      else putValue(Action.NAME,lbl);
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isValid()
{
   if (using_code == null) return false;

   return true;
}


Position getPosition(BaleDocument doc)
{
   if (for_problem == null) return null;

   if (doc == null) {
      doc = bale_factory.getDocument(null,for_problem.getFile());
      if (doc == null) return null;
    }

   int start = for_problem.getStart();
   int offset = doc.mapOffsetToJava(start);

   try {
      return doc.createPosition(offset);
    }
   catch (BadLocationException e) { }

   return null;
}



String getProject()			{ return for_problem.getProject(); }
int getStartPosition()			{ return for_problem.getStart(); }
int getEndPosition()			{ return for_problem.getEnd(); }
File getFile()				{ return for_problem.getFile(); }

double getRelevance()			{ return using_code.getRelevance(); }

@Override public int compareTo(BaleFixer bf)
{
   double v = bf.getRelevance() - getRelevance();
   if (v < 0) return -1;
   if (v > 0) return 1;
   return toString().compareTo(bf.toString());
}



@Override public String toString()
{
   return using_code.getLabel();
}




/********************************************************************************/
/*										*/
/*	Entries for handling fixups						*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   BoardLog.logD("BALE","DOING QUICK FIX " + getValue(Action.NAME));

   if (using_code != null) {
      using_code.perform(evt);
   }
}





/********************************************************************************/
/*										*/
/*	New Method fixups							*/
/*										*/
/********************************************************************************/

private static class NewMethodFixup implements Fixup {

   private BaleFixer for_fix;
   private String class_name;
   private String method_name;
   private String param_types;
   private String return_type;

   NewMethodFixup(BaleFixer fix,BumpFix bf) {
      for_fix = fix;
      class_name = bf.getParameter("CLASS");
      method_name = bf.getParameter("NAME");
      param_types = bf.getParameter("PARAMS");
      return_type = bf.getParameter("RETURN");
    }

   @Override public String getLabel() {
      String typ = class_name;
      int idx = typ.lastIndexOf(".");
      if (idx >= 0) typ = typ.substring(idx+1);
      typ = typ.replace('$','.');
      return "Create New Method " + method_name + " in " + typ;
    }

   @Override public void perform(ActionEvent evt) {
      Component c = (Component) evt.getSource();
      BaleDocument doc = null;
      Position pos = null;
      String proj = null;
      String after = null;
      int mods = 0;
      for (Component xc = c; xc != null; xc = xc.getParent()) {
	 if (xc instanceof BaleEditorPane) {
	    c = xc;
	    break;
	 }
	 else if (xc instanceof JPopupMenu) {
	    c = ((JPopupMenu) xc).getInvoker();
	    break;
	 }
      }

      if (c instanceof BaleEditorPane) {
	 BaleEditorPane bep = (BaleEditorPane) c;
	 doc = bep.getBaleDocument();
	 pos = for_fix.getPosition(doc);
	 proj = doc.getProjectName();
	 // TODO: if class is same as that of bep, then set after to current method
	 //    and set mods to private.  If class is not in same project, set mods
	 //    to public
       }

      String params = param_types;
      if (params != null) {
	 StringBuffer plist = new StringBuffer();
	 StringTokenizer tok = new StringTokenizer(param_types,",");
	 int ct = 0;
	 while (tok.hasMoreTokens()) {
	    if (ct > 0) plist.append(", ");
	    plist.append(tok.nextToken().trim());
	    plist.append(" a" + ct);
	    ++ct;
	  }
	 params = plist.toString();
       }

      bale_factory.createNewMethod(proj,class_name + "." + method_name,
				      params,return_type,mods,true,after,
				      c,pos,true,true);
    }

   @Override public double getRelevance()		{ return 50; }

}






private static class EditBasedFixup implements Fixup {

   private BaleFixer for_fix;
   private BumpFix edit_fix;

   EditBasedFixup(BaleFixer fix,BumpFix bf) {
      for_fix = fix;
      edit_fix = bf;
    }

   @Override public String getLabel() {
      return edit_fix.getParameter("DISPLAY");
    }

   @Override public void perform(ActionEvent evt) {
      String proj = for_fix.getProject();
      File file = for_fix.getFile();
      BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(proj,file);
      BaleDocumentIde bd = (BaleDocumentIde) bfo;
   
      BurpHistory bh = null;
      BaleEditorPane htxt = null;
      if (evt.getSource() instanceof BaleEditorPane) {
         bh = BurpHistory.getHistory();
         htxt = (BaleEditorPane) evt.getSource();
         bh.beginEditAction(htxt);
       }
   
      try {
         BaleApplyEdits app = new BaleApplyEdits(bd);
         app.applyEdits(edit_fix.getEdits());
       }
      finally {
         if (bh != null) bh.endEditAction(htxt);
       }
    }

   @Override public double getRelevance() {
      double v = edit_fix.getRelevance();
      v -= BaleApplyEdits.getEditSize(edit_fix.getEdits())/5.0;
      return v;
    }
}	// end of inner class EditBasedFixup




}	// end of class BaleFixer




/* end of BaleFixer.java */
