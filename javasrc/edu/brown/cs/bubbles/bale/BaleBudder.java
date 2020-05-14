/********************************************************************************/
/*										*/
/*		BaleBudder.java 						*/
/*										*/
/*	Bubble Annotated Language Editor budding actions			*/
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


import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.TextAction;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;



class BaleBudder implements BaleConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudAction	bud_action;
private FragmentAction	fragment_action;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleBudder()
{
   bud_action = new BudAction();
   fragment_action = new FragmentAction();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

TextAction getBuddingAction()		{ return bud_action; }
TextAction getFragmentAction()		{ return fragment_action; }



/********************************************************************************/
/*										*/
/*	Editor access methods							*/
/*										*/
/********************************************************************************/

private static BaleEditorPane getBaleEditor(ActionEvent e)
{
   if (e == null) return null;

   return (BaleEditorPane) e.getSource();
}



private static boolean checkEditor(BaleEditorPane e)
{
   if (e == null) return false;
   if (!e.isEditable()) return false;
   if (!e.isEnabled()) return false;
   BaleDocument bd = e.getBaleDocument();
   if (bd.isOrphan()) {
      e.setEditable(false);
      return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Position finding methods						*/
/*										*/
/********************************************************************************/

private static int findEndPoint(Segment s,int off)
{
   int soff1 = off;
   if (soff1 >= s.length()) soff1 = s.length()-1;
   while (soff1 >= 0) {
      if (!Character.isWhitespace(s.charAt(soff1))) break;
      --soff1;
   }
   if (soff1 >= 0) {
      while (soff1 < off) {
	 if (s.charAt(soff1) == '\n') {
	    ++soff1;
	    break;
	 }
	 ++soff1;
      }
   }

   return soff1;
}




private static int findStartPoint(Segment s,int off)
{
   if (off < 0) off = 0;
   int soff2 = off;
   while (soff2 < s.length()) {
      if (!Character.isWhitespace(s.charAt(soff2))) break;
      ++soff2;
   }
   if (soff2 >= s.length()) return -1;
   while (soff2 > off) {
      if (s.charAt(soff2) == '\n') {
	 ++soff2;
	 break;
      }
      --soff2;
   }
   return soff2;
}




/********************************************************************************/
/*										*/
/*	Budding action								*/
/*										*/
/********************************************************************************/

private class BudAction extends TextAction {

   private static final long serialVersionUID = 1;

   BudAction() {
      super("BudAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      boolean nxt = budderActionPerformed(e);
      if (nxt) fragment_action.actionPerformed(e);
    }

   private boolean budderActionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return false;
      BaleDocument bd = target.getBaleDocument();
      BaleRegion r0,r1;
      Rectangle startpos = null;
      BaleFragmentType ftyp = BaleFragmentType.NONE;
   
   
   
      bd.baleWriteLock();
      try {
         int soff = target.getSelectionStart();
         if (!(bd.getDefaultRootElement() instanceof BaleElement)) return false;
   
         BaleElement root = (BaleElement) bd.getDefaultRootElement();
   
   
         // first find the fragment we are in
         BaleElement be = bd.getCharacterElement(soff);
         while (be != null && be.getParent() != null && be.getParent() != root &&
        	   be.getBubbleType() == BaleFragmentType.NONE) {
            be = be.getBaleParent();
          }
         if (be == null || be == root) return false;
         ftyp = be.getBubbleType();
   
         // next ensure that there is a second fragment
         int n = root.getChildCount();
         int split = -1;
         boolean havebody = false;
         for (int i = 0; i < n; ++i) {
            BaleElement cbe = root.getBaleElement(i);
            if (cbe == be) split = i;
            else if (cbe.getBubbleType() != BaleFragmentType.NONE) havebody = true;
          }
         if (split < 0 || !havebody) {
            return true;
          }
   
         startpos = null;
   
         Segment s = new Segment();
         try {
            bd.getText(0,bd.getLength(),s);
         }
         catch (BadLocationException ex) {
            return false;
         }
   
         int soff0 = be.getStartOffset();
         int eoff0 = be.getEndOffset();
         // first find where the old fragment should end
         int soff1 = findEndPoint(s,soff0);
         // next determine where new fragment should start
         int soff2 = findStartPoint(s,soff1);
         // next determine where the new fragment should end
         int eoff2 = findEndPoint(s,eoff0);
         // next determine where the old fragment should continue
         int eoff1 = findStartPoint(s,eoff2);
   
         try {
            r0 = bd.createDocumentRegion(soff2,eoff2,true);
            if (soff1 < 0) soff1 = 0;
            if (eoff1 < 0) eoff1 = s.length();
            r1 = bd.createDocumentRegion(soff1,eoff1,true);
            startpos = SwingText.modelToView2D(target,soff2);
            BudaBubble bbl = BudaRoot.findBudaBubble(target);
            if (bbl == null) return false;
            startpos.setLocation(SwingUtilities.convertPoint(target,startpos.getLocation(),bbl));
         }
         catch (BadLocationException ex) {
            return false;
         }
   
         // remove regions from original editor
         List<BaleRegion> rgns = new ArrayList<>();
         rgns.add(r1);
         bd.removeRegions(rgns);
   
         target.setCaretPosition(0);
         target.setSelectionStart(0);
         target.setSelectionEnd(0);
   
         target.checkSize();
       }
      finally { bd.baleWriteUnlock(); }
   
      BaleFactory bf = BaleFactory.getFactory();
      List<BaleRegion> nrgns = new ArrayList<>();
      nrgns.add(r0);
      BaleFragmentEditor bfe = bf.getEditorFromRegions(bd.getProjectName(),bd.getFile(),
        						   null,nrgns,ftyp);
      if (bfe != null) {
         BudaRoot broot = BudaRoot.findBudaRoot(target);
         Rectangle loc = BudaRoot.findBudaLocation(target);
         if (broot == null || loc == null) return false;
         BaleEditorBubble bb = new BaleEditorBubble(bfe);
         broot.add(bb,new BudaConstraint(loc.x + startpos.x,loc.y + startpos.y));
         bb.markBubbleAsNew();
         bb.grabFocus();
       }
      BoardMetrics.noteCommand("BALE","Bud");
   
      return false;
    }

}	// end of inner class BudAction





/********************************************************************************/
/*										*/
/*	Create fragment action							*/
/*										*/
/********************************************************************************/

private class FragmentAction extends TextAction {

   private static final long serialVersionUID = 1;

   FragmentAction() {
      super("FragmentAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;

      BaleDocument bd = target.getBaleDocument();
      // Position spos = null;
      BaleFragmentEditor bfe = null;
      BaleEditorBubble bb = null;

      bd.baleWriteLock();
      try {
	 int soff = target.getSelectionStart();
	 bfe = findFragmentBubble(target,soff);
       }
      finally { bd.baleWriteUnlock(); }

      if (bfe != null && bb == null) {
	 bb = new BaleEditorBubble(bfe);
       }

      if (bb != null) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(target);
	 if (bba != null) {
	    bba.addBubble(bb,target,null,
			     PLACEMENT_NEW |
			     PLACEMENT_PREFER|PLACEMENT_GROUPED|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
	  }
	 bb.grabFocus();
	 BoardMetrics.noteCommand("BALE","Fragment");
       }
    }

}	// end of inner class FragmentAction



/********************************************************************************/
/*										*/
/*	Find fragment bubbles							*/
/*										*/
/********************************************************************************/

static BaleFragmentEditor findFragmentBubble(BaleEditorPane target,int pos)
{
   BaleDocument bd = target.getBaleDocument();
   BaleFragmentEditor bfe = null;

   bd.baleWriteLock();
   try {
      int soff = pos;
      Element relt = bd.getDefaultRootElement();
      if (!(relt instanceof BaleElement)) return null;
      BaleElement root = (BaleElement) relt;
      BaleElement be = bd.getCharacterElement(soff);
      while (be != null && be.getParent() != null && be.getParent() != root &&
		be.getBubbleType() == BaleFragmentType.NONE) {
	 be = be.getBaleParent();
       }
      if (be == null || be == root) return null;

      BaleFragmentType ftyp = be.getBubbleType();
      if (ftyp == BaleFragmentType.NONE) return null;

      Segment s = new Segment();
      try {
	 bd.getText(0,bd.getLength(),s);
       }
      catch (BadLocationException ex) {
	 return null;
       }

      int soff0 = be.getStartOffset();
      int eoff0 = be.getEndOffset();
      int soff1 = findEndPoint(s,soff0);
      // next determine where new fragment should start
      int soff2 = findStartPoint(s,soff1);
      // next determine where the new fragment should end
      int eoff2 = findEndPoint(s,eoff0);

      List<BaleRegion> rgns = new ArrayList<BaleRegion>();
      try {
	 BaleRegion r0 = bd.createDocumentRegion(soff2,eoff2,true);
	 rgns.add(r0);
	 // spos = bd.createPosition(soff2);
       }
      catch (BadLocationException ex) {
	 return null;
       }

      BaleFactory bf = BaleFactory.getFactory();
      bfe =  bf.getEditorFromRegions(bd.getProjectName(),bd.getFile(),null,rgns,ftyp);
    }
   finally { bd.baleWriteUnlock(); }

   return bfe;
}



}	// end of class BaleBudder




/* end of BaleBudder.java */
