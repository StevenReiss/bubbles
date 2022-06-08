/********************************************************************************/
/*										*/
/*		BaleElementBuffer.java						*/
/*										*/
/*	Bubble Annotated Language Editor text buffer maintaining elements	*/
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

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import org.w3c.dom.Element;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;



class BaleElementBuffer implements BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleElement.Branch		root_element;
private Map<BumpProblem,ProblemData>	problem_map;
private BaleDocument			base_document;
private Map<BaleRegion,BaleElement>	elide_regions;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleElementBuffer(BaleElement.Branch root,BaleDocument doc)
{
   root_element = root;
   problem_map = new HashMap<BumpProblem,ProblemData>();
   base_document = doc;
   elide_regions = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BaleElement getRootElement()			{ return root_element; }




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

synchronized BaleElementEvent setup()
{
   if (root_element == null) return null;

   BaleElementEvent evt = replaceParent(root_element,false);

   // BoardLog.logD("BALE","Initial ELEMENT buffer = " + this);

   return evt;
}



/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

synchronized BaleElementEvent insertString(int off,int len)
{
   if (len == 0) return null;

   off = base_document.getFragmentOffset(off);
   if (off < 0) return null;

   // note that the edit has already been done

   BaleDocument doc = root_element.getBaleDocument();
   BaleElement.Branch par = doc.getActualParagraphElement(off+len);
   if (par == null) return null;

   while (off < par.getStartOffset() || off+len >= par.getEndOffset() ||
	     par.isInsideLine() || isInElided(par)) {
      if (par == root_element) break;
      par = (BaleElement.Branch) par.getParentElement();
    }

   return replaceParent(par,true);
}



synchronized BaleElementEvent remove(int off,int len)
{
   if (len == 0) return null;
   // note that the edit has already been done

   int eoff = base_document.getFragmentOffset(off);
   if (eoff < 0) return null;
   int roff = (eoff == 0 ? 0 : eoff-1);

   BaleDocument doc = root_element.getBaleDocument();
   BaleElement.Branch par = doc.getActualParagraphElement(roff);
   if (par == null) return null;

   BaleElement lf1 = doc.getActualCharacterElement(roff);
   if (lf1 != null) {
      BaleElement lf2 = lf1.getPreviousCharacterElement();
      if (lf2 != null) {
	 if (lf2.getEndOffset() < lf2.getStartOffset()) {
	    if (par != root_element) par = (BaleElement.Branch) par.getParentElement();
	 }
      }
   }

   while (par.getStartOffset() == roff || eoff >= par.getEndOffset() || par.isInsideLine() ||
	    par.getEndOffset() - par.getStartOffset() <= 0 || eoff < 0 || isInElided(par)) {
      if (par == root_element) break;
      par = (BaleElement.Branch) par.getParentElement();
      if (par == null) return null;
    }

   return replaceParent(par,true);
}



private boolean isInElided(BaleElement be)
{
   for (BaleElement par = be; par != null; par = par.getBaleParent()) {
      if (par.isElided()) return true;
   }
   return false;
}



/********************************************************************************/
/*										*/
/*	Fixup methods								*/
/*										*/
/********************************************************************************/

synchronized BaleElementEvent updateAst(List<BaleAstNode> nodes)
{
   if (root_element == null) return null;

   clearProblems();
   saveElisions();

   BaleAstNode bn = null;
   if (nodes.size() == 1) {
      bn = nodes.get(0);
      bn = findSubnode(bn);
    }
   else bn = new BaleAstNode(nodes);

   BaleElementBuilder b = new BaleElementBuilder(root_element,bn);
   b.addChild(root_element);
   BaleElementEvent ee = b.fixup();

   updateProblems();
   restoreElisions();

   // BoardLog.logD("BALE","Updated ELEMENT buffer = " + this);

   return ee;
}


private BaleAstNode findSubnode(BaleAstNode bn)
{
   int len = base_document.getLength();
   if (len <= 0) return null;

   Segment s = new Segment();
   try {
      base_document.getText(0,len,s);
    }
   catch (BadLocationException e) {
      return bn;
    }
   int soff = 0;
   int eoff = s.length();
   while (soff < eoff) {
      if (Character.isWhitespace(s.charAt(soff))) ++soff;
      else break;
   }
   while (eoff > soff) {
      if (Character.isWhitespace(s.charAt(eoff-1))) --eoff;
      else break;
   }
   soff = base_document.getDocumentOffset(soff);
   eoff = base_document.getDocumentOffset(eoff)-1;
   BaleAstNode cn = bn.getNode(soff,eoff);
   while (cn != null) {
      BaleAstNode pn = null;
      switch (cn.getNodeType()) {
	 case CLASS :
	 case METHOD :
	 case INITIALIZER :
	    break;
	 case FILE :
	 case SET :
	    return bn;
	 default :
	    pn = cn.getParent();
	    break;
      }
      if (pn == null) break;
      cn = pn;
   }

   if (cn != null) return cn;

   return bn;
}




/********************************************************************************/
/*										*/
/*	Problem methods 							*/
/*										*/
/********************************************************************************/

void updateProblems()
{
   Set<BumpProblem> found = new HashSet<BumpProblem>();
   BaleDocument doc = root_element.getBaleDocument();
   int numadd = 0;
   int numrem = 0;
   int numerr = 0;

   synchronized (problem_map) {
      for (BumpProblem bp : doc.getProblems()) {
	 found.add(bp);
	 if (isError(bp)) ++numerr;
	 ProblemData pd = problem_map.get(bp);
	 if (pd == null) {
	    if (isError(bp)) ++numadd;
	    pd = new ProblemData(bp,doc);
	    problem_map.put(bp,pd);
	  }
       }
      for (Iterator<Map.Entry<BumpProblem,ProblemData>> it = problem_map.entrySet().iterator(); it.hasNext(); ) {
	 Map.Entry<BumpProblem,ProblemData> ent = it.next();
	 BumpProblem bp = ent.getKey();
	 if (!found.contains(bp)) {
	    if (isError(bp)) ++numrem;
	    ent.getValue().clear(false);
	    it.remove();
	  }
       }

      for (ProblemData pd : problem_map.values()) {
	 pd.setup(root_element,base_document,false);
       }
    }

   if ((numerr != 0 && numadd == numerr) || (numerr == 0 && numrem > 0)) {
      int off = root_element.getStartOffset();
      int len = root_element.getEndOffset() - root_element.getStartOffset();
      if (len <= 0) return;
      doc.reportEvent(doc,off,len,DocumentEvent.EventType.CHANGE,null,null);
    }
}



private static boolean isError(BumpProblem bp)
{
   return bp.getErrorType() == BumpErrorType.FATAL ||
      bp.getErrorType() == BumpErrorType.ERROR;
}



List<BumpProblem> getProblemsAtLocation(int pos)
{
   List<BumpProblem> rslt = null;

   synchronized (problem_map) {
      for (Map.Entry<BumpProblem,ProblemData> ent : problem_map.entrySet()) {
	 ProblemData pd = ent.getValue();
	 if (pd.containsPosition(pos)) {
	    if (rslt == null) rslt = new ArrayList<BumpProblem>();
	    rslt.add(ent.getKey());
	  }
       }
    }

   return rslt;
}




private void clearProblems()
{
   synchronized (problem_map) {
      for (ProblemData pd : problem_map.values()) {
	 pd.clear(false);
       }
    }
}


/********************************************************************************/
/*										*/
/*	Elision methods 							*/
/*										*/
/********************************************************************************/

void saveElisions()
{
   if (elide_regions != null) return;

   addElidedRegions(root_element);
}


private void addElidedRegions(BaleElement be)
{
   if (be.isElided()) {
      if (elide_regions == null) elide_regions = new HashMap<>();
      BalePosition spos = base_document.savePosition(be.getStartPosition());
      BalePosition epos = base_document.savePosition(be.getEndPosition());
      BaleRegion br = new BaleRegion(spos,epos);
      elide_regions.put(br,be);
    }
   else {
      int n = be.getChildCount();
      for (int i = 0; i < n; ++i) addElidedRegions(be.getBaleElement(i));
    }
}



void restoreElisions()
{
   if (elide_regions == null) return;

   checkElidedRegions(root_element);

   elide_regions = null;
}



private void checkElidedRegions(BaleElement be)
{
   if (shouldBeElided(be) != null && be.canElide()) {
      be.setElided(true);
    }
   else {
      be.setElided(false);
      int n = be.getChildCount();
      for (int i = 0; i < n; ++i) checkElidedRegions(be.getBaleElement(i));
    }
}



BaleElement shouldBeElided(BaleElement be)
{
   if (elide_regions == null) return null;

   int soff = be.getStartOffset();
   int eoff = be.getEndOffset();

   for (Map.Entry<BaleRegion,BaleElement> ent : elide_regions.entrySet()) {
      BaleRegion br = ent.getKey();
      int esoff = br.getStart();
      int eeoff = br.getEnd();
      if (soff >= esoff && eoff <= eeoff) return ent.getValue();
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Scanning methods							*/
/*										*/
/********************************************************************************/

synchronized private BaleElementEvent replaceParent(BaleElement.Branch par,boolean elide)
{
   int soff,eoff;
   String text;
   BaleDocument doc = par.getBaleDocument();
   BaleTokenState sstate = par.getStartTokenState();
   BaleTokenState estate = par.getEndTokenState();

   if (par == root_element) {
      soff = 0;
      eoff = doc.getLength();
    }
   else {
      soff = par.getStartOffset();
      eoff = par.getEndOffset();
    }

   try {
      text = doc.getText(soff,eoff-soff);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Problem getting parent text for elements: " + e);
      return null;
    }

   clearProblems();

   if (elide) saveElisions();
   else elide_regions = null;
   IdentifierMap idmap = new IdentifierMap(par);

   List<BaleElement> reps = scanText(text,soff,sstate,estate,idmap,(par == root_element));
   if (reps == null) {
      return replaceParent(root_element,elide);
    }

   fixupElisions(reps);
   fixupHints(reps,par);

   BaleElementEvent ee = null;

   if (par == root_element) {
      // BoardLog.logD("BALE","ELEMENT REPLACE ROOT " + root_element.getChildCount() + " " + reps.size());

      ee = new BaleElementEvent(root_element,reps);
      root_element.clear();
      for (BaleElement r : reps) root_element.add(r);
      // BoardLog.logD("BALE","Result: " + root_element);
    }
   else {
      // BoardLog.logD("BALE","ELEMENT REPLACE " + par.getName() + " " + reps.size() + " " + par + " " + reps.get(0));
      BaleElement.Branch npar = (BaleElement.Branch) par.getParentElement();
      if (npar == null) return null;
      ee = new BaleElementEvent(npar,par,reps);
      npar.replace(par,reps);
      // BoardLog.logD("BALE","Result: " + root_element);
    }

   updateProblems();

   return ee;
}



private List<BaleElement> scanText(String text,int baseoffset,
				      BaleTokenState sstate,BaleTokenState estate,
				      IdentifierMap idmap,
				      boolean top)
{
   BaleTokenizer toks = BaleTokenizer.create(text,sstate,base_document.getLanguage());
   BaleDocument doc = root_element.getBaleDocument();

   List<BaleElement> rslt = new ArrayList<BaleElement>();
   BaleElement.UnknownNode cur = new BaleElement.UnknownNode(doc,root_element);
   cur.setStartTokenState(sstate);
   rslt.add(cur);
   int ctr = 0;
   BaleTokenState nstate = sstate;
   BaleElement xelt = null;

   for (BaleToken bt : toks.scan()) {
      int soff = bt.getStartOffset() + baseoffset;
      int eoff = soff + bt.getLength();
      BaleElement nelt = null;
      nstate = BaleTokenState.NORMAL;
      switch (bt.getType()) {
	 case EOL :
	    nelt = new BaleElement.Eol(doc,cur,soff,eoff);
	    break;
	 case SPACE :
	    if (xelt == null || xelt.isEndOfLine())
	       nelt = new BaleElement.Indent(doc,cur,soff,eoff);
	    else
	       nelt = new BaleElement.Space(doc,cur,soff,eoff);
	    break;
	 case LINECOMMENT :
	    nelt = new BaleElement.LineComment(doc,cur,soff,eoff);
	    break;
	 case EOLFORMALCOMMENT :
	    nelt = new BaleElement.JavaDocComment(doc,cur,soff,eoff,true,bt.getType());
	    nstate = BaleTokenState.IN_FORMAL_COMMENT;
	    break;
	 case ENDFORMALCOMMENT :
	    nelt = new BaleElement.JavaDocComment(doc,cur,soff,eoff,false,bt.getType());
	    break;
	 case EOLCOMMENT :
	    nelt = new BaleElement.Comment(doc,cur,soff,eoff,true,bt.getType());
	    nstate = BaleTokenState.IN_COMMENT;
	    break;
	 case ENDCOMMENT :
	    nelt = new BaleElement.Comment(doc,cur,soff,eoff,false,bt.getType());
	    break;
         case EOLSTRING :
            nelt = new BaleElement.Literal(doc,cur,soff,eoff,bt.getType());
            nstate = BaleTokenState.IN_MULTILINE_STRING;
            break;
	 case KEYWORD :
	 case IF :
	 case DO :
	 case FOR :
	 case TRY :
	 case NEW :
	 case CASE :
	 case ELSE :
	 case ENUM :
	 case GOTO :
	 case BREAK :
	 case CATCH :
	 case CLASS :
	 case WHILE :
	 case STATIC :
	 case SWITCH :
	 case DEFAULT :
	 case FINALLY :
	 case INTERFACE :
	 case SYNCHRONIZED :
	 case TYPEKEY :
	 case CONTINUE :
	 case PASS :
	 case RAISE :
	 case IMPORT :
	 case PACKAGE :
         case FUNCTION :
	    nelt = new BaleElement.Keyword(doc,cur,soff,eoff,bt.getType());
	    break;
	 case RETURN :
	    nelt = new BaleElement.Return(doc,cur,soff,eoff);
	    break;
	 case BADNUMBER :
	 case NUMBER :
	    nelt = new BaleElement.Number(doc,cur,soff,eoff);
	    break;
	 case BADSTRING :
	 case BADCHARLIT :
	 case CHARLITERAL :
	 case STRING :
	 case LONGSTRING :
	    nelt = new BaleElement.Literal(doc,cur,soff,eoff,bt.getType());
	    break;
	 case IDENTIFIER :
	    nelt = idmap.getElement(soff);
	    if (nelt == null) nelt = new BaleElement.Identifier(doc,cur,soff,eoff);
	    else {
	       BaleElement.Leaf lelt = (BaleElement.Leaf) nelt;
	       lelt.setPosition(soff,eoff);
	     }
	    break;
	 case LPAREN :
	 case RPAREN :
	 case LBRACKET :
	 case RBRACKET :
	 case SEMICOLON :
	 case COMMA :
	 case COLON :
	 case EQUAL :
	 case DOT :
	 case AT :
	 case OP :
	 case QUESTIONMARK :
	 case LANGLE :
	 case RANGLE :
	 case OTHER :
	 case BACKSLASH :
	    nelt = new BaleElement.Token(doc,cur,soff,eoff,bt.getType());
	    break;
	 case LBRACE :
	 case RBRACE :
	    nelt = new BaleElement.Brace(doc,cur,soff,eoff,bt.getType());
	    break;
	 default :
	    BoardLog.logW("BALE","Unknown token returned: " + bt.getType() + " " + soff + " " +
			     eoff + " " + text.substring(soff-baseoffset,eoff-baseoffset));
       }

      if (nelt != null) {
	 if (nelt.isEndOfLine()) {
	    cur.setEndOfLine();
	    cur.add(nelt);
	    cur.setEndTokenState(nstate);
	    cur = new BaleElement.UnknownNode(doc,root_element);
	    cur.setStartTokenState(nstate);
	    rslt.add(cur);
	    ctr = 0;
	  }
	 else {
	    cur.add(nelt);
	    ++ctr;
	  }
	 xelt = nelt;
       }
    }

   if (ctr == 0) rslt.remove(cur);

   if (!top && nstate != estate) return null;

   return rslt;
}



private void fixupElisions(List<BaleElement> lelts)
{
   if (elide_regions == null) return;

   for (int i = 0; i < lelts.size(); ++i) {
      BaleElement lelt = lelts.get(i);
      BaleElement eelt = shouldBeElided(lelt);
      if (eelt != null && lelt.isEndOfLine()) {
	 int epos = eelt.getEndOffset();
	 int spos = eelt.getStartOffset();
	 int slpos = lelt.getStartOffset();
	 if (spos < slpos) continue;

	 if (!fixPositions(lelts,i,eelt)) {
	    // BoardLog.logE("BALE","Unable to fix line positions for " + lelt + " and " + eelt);
	  }
	 epos = eelt.getEndOffset();

	 lelts.set(i,eelt);
	 boolean issimple = true;
	 while (i+1 < lelts.size()) {
	    lelt = lelts.get(i+1);
	    if (lelt.getEndOffset() > epos) break;
	    for (int j = 0; j < lelt.getChildCount(); ++j) {
	       BaleElement celt = lelt.getBaleElement(j);
	       if (!celt.isComment() && !celt.isEndOfLine() && !celt.isEmpty()) issimple = false;
	    }
	    // TODO: This code is a bit mysterious
	    // BoardLog.logD("BALE","Elision extra line " + lelt.getStartOffset() + " " + lelt.getEndOffset());
	    BaleElement xelt = lelts.remove(i+1);
	    BaleElement.Branch beb = (BaleElement.Branch) eelt;
	    issimple = false;
	    if (issimple) beb.add(xelt);
	  }

	 eelt.fixParents();	// identifiers inside old elided region might have new parents -- restore
       }
    }
}



private void fixupHints(List<BaleElement> nelts,BaleElement.Branch par)
{
   List<BaleElement> newflat = flatten(nelts,null);
   List<BaleElement> oldflat = flatten(par,null);
   int chk = Math.min(newflat.size(),oldflat.size());
   
   for (int i = 0; i < chk; ++i) {
      BaleElement oelt = oldflat.get(i);
      BaleElement nelt = newflat.get(i);
      if (oelt.getTokenType() != nelt.getTokenType()) break;
      if (!oelt.getName().equals(nelt.getName())) break;
      if (nelt.isEmpty()) continue;
      Element ohint = null;
      if (oelt.getAstNode() != null) ohint = oelt.getAstNode().getHintData();
      else ohint = oelt.getOldHintData();
      if (ohint != null) {
         nelt.setOldHintData(ohint);
       }
    }
   
}


private List<BaleElement> flatten(List<BaleElement> elts,List<BaleElement> rslt)
{
   for (BaleElement be : elts) {
      rslt = flatten(be,rslt);
    }
   
   return rslt;
}


private List<BaleElement> flatten(BaleElement be,List<BaleElement> rslt)
{
   if (rslt == null) rslt = new ArrayList<>();
   if (be == null) return rslt;
   
   if (be.isLeaf()) rslt.add(be);
   else {
      for (int i = 0; i < be.getElementCount(); ++i) {
         flatten(be.getBaleElement(i),rslt);
       }
    }
   
   return rslt;
}



private boolean fixPositions(List<BaleElement> nelts,int idx,BaleElement oelt)
{
   NewLeafIter nli = new NewLeafIter(nelts,idx);

   return fixElementPosition(nli,oelt);
}


private boolean fixElementPosition(NewLeafIter nli,BaleElement oelt)
{
   if (oelt.isLeaf()) {
      if (!nli.hasNext()) return false;
      BaleElement nelt = nli.next();
      if (!nelt.getName().equals(oelt.getName())) {
	 if (!nelt.isIdentifier() && !oelt.isIdentifier()) {
	    return false;
	  }
       }
      if (oelt != nelt) {
	 BaleElement.Leaf le = (BaleElement.Leaf) oelt;
	 le.setPosition(nelt.getStartOffset(),nelt.getEndOffset());
       }
      return true;
    }
   for (int i = 0; i < oelt.getChildCount(); ++i) {
      BaleElement celt = oelt.getBaleElement(i);
      if (!fixElementPosition(nli,celt)) return false;
    }

   return true;
}


private static class NewLeafIter implements Iterator<BaleElement> {

   List<BaleElement> element_list;
   int cur_index;
   boolean is_checked;
   BaleElement cur_element;

   NewLeafIter(List<BaleElement> lst,int idx) {
      element_list = lst;
      cur_index = idx;
      cur_element = null;
      BaleElement be = lst.get(idx);
      int spos = be.getStartOffset();
      cur_element = be.getBaleChildAtPosition(spos);
      is_checked = true;
    }

   @Override public boolean hasNext() {
      if (!is_checked) {
	 is_checked = true;
	 BaleElement par = null;
	 BaleElement chld = cur_element;
	 int idx = -1;
	 for ( ; ; ) {
	    par = chld.getBaleParent();
	    idx = par.getChildIndex(chld);
	    if (idx+1 < par.getElementCount()) {
	       cur_element = par.getBaleElement(idx+1);
	       break;
	     }
	    if (par == element_list.get(cur_index)) {
	       ++cur_index;
	       if (cur_index >= element_list.size()) {
		  cur_element = null;
		  break;
		}
	       par = element_list.get(cur_index);
	       int spos = par.getStartOffset();
	       cur_element = par.getBaleChildAtPosition(spos);
	       break;
	     }
	    chld = par;
	  }
       }
      return cur_element != null;
    }

   @Override public BaleElement next() {
      if (!is_checked) hasNext();
      is_checked = false;
      return cur_element;
    }

   @Override public void remove() {
      throw new UnsupportedOperationException();
    }

}	// end of inner classs NewLeafIter




/********************************************************************************/
/*										*/
/*	Holder for problem information						*/
/*										*/
/********************************************************************************/

private static class ProblemData {

   private List<BaleElement>	use_elements;
   private BoardHighlightStyle	hilite_style;
   private Color		hilite_color;
   private BaleRegion		problem_region;

   ProblemData(BumpProblem bp,BaleDocument doc) {
      use_elements = null;
      hilite_style = getHighlightStyle(bp);
      hilite_color = getHighlightColor(bp);
      problem_region = doc.getRegionFromEclipse(bp.getStart(),bp.getEnd());
    }

   void clear(boolean chng) {
      if (use_elements != null) {
	 for (BaleElement be : use_elements) {
	    be.removeAttribute(BOARD_ATTR_HIGHLIGHT_STYLE);
	    be.removeAttribute(BOARD_ATTR_HIGHLIGHT_COLOR);
	    if (chng) generateChange(be);
	  }
	 use_elements = null;
       }
    }

   boolean containsPosition(int pos) {
      int spos = problem_region.getStart();
      int epos = problem_region.getEnd();
      if (epos == spos) ++epos;
      if (spos <= pos &&  epos > pos) return true;
      return false;
    }

   void setup(BaleElement.Branch root,BaleDocument doc,boolean chng) {
      if (use_elements != null) return; 	// already setup
      if (hilite_style == null || hilite_style == BoardHighlightStyle.NONE) return;
      if (problem_region == null) return;

      int pstart = problem_region.getStart();
      int pend = problem_region.getEnd();
      if (pstart < 0) return;

      use_elements = new ArrayList<BaleElement>();
      BaleElement base = root.getBaleChildAtPosition(pstart);
      if (base != null) {
	 for (BaleElement pe = base.getBaleParent(); pe != null; pe = pe.getBaleParent()) {
	    if (pe.getStartOffset() < pstart || pe.getEndOffset() > pend+1) break;
	    base = pe;
	  }
	 BaleElement par = base.getBaleParent();
	 if (par == null) return;
	 for (int i = 0; i < par.getElementCount(); ++i) {
	    BaleElement ce = par.getBaleElement(i);
	    if (ce.getStartOffset() >= pstart && ce.getEndOffset() <= pend+1) {
	       use_elements.add(ce);
	     }
	  }
       }

      if (use_elements.isEmpty()) {
	 use_elements = null;
	 return;
       }

      for (BaleElement be : use_elements) {
	 be.addAttribute(BOARD_ATTR_HIGHLIGHT_STYLE,hilite_style);
	 be.addAttribute(BOARD_ATTR_HIGHLIGHT_COLOR,hilite_color);
	 if (chng) generateChange(be);
       }
    }

   private void generateChange(BaleElement elt) {
      BaleDocument doc = elt.getBaleDocument();
      int off = elt.getStartOffset();
      int len = elt.getEndOffset() - elt.getStartOffset();
      if (len <= 0) return;
      doc.reportEvent(doc,off,len,DocumentEvent.EventType.CHANGE,null,null);
    }

}	// end of inner class ProblemData



private static BoardHighlightStyle getHighlightStyle(BumpProblem bp)
{
   if (bp.getErrorType() == BumpErrorType.ERROR) return BoardHighlightStyle.SQUIGGLE;
   else if (bp.getErrorType() == BumpErrorType.WARNING) return BoardHighlightStyle.SQUIGGLE;

   return BoardHighlightStyle.NONE;
}


private static Color getHighlightColor(BumpProblem bp)
{
   if (bp.getErrorType() == BumpErrorType.ERROR) return BoardColors.getColor(BALE_ERROR_COLOR_PROP);
   else if (bp.getErrorType() == BumpErrorType.WARNING) return BoardColors.getColor(BALE_WARNING_COLOR_PROP);

   return BoardColors.getColor("Bale.DefaultHighlightColor");
}



/********************************************************************************/
/*										*/
/*	Methods for preserving identifier types 				*/
/*										*/
/********************************************************************************/

private static class IdentifierMap {

   private Map<Integer,BaleElement> id_map;

   IdentifierMap(BaleElement be) {
      id_map = new HashMap<>();
      setupMap(be);
    }

   private void setupMap(BaleElement be) {
      if (be.isIdentifier()) {
	 id_map.put(be.getStartOffset(),be);
       }
      else {
	 int n = be.getChildCount();
	 for (int i = 0; i < n; ++i) {
	    BaleElement sbe = be.getBaleElement(i);
	    setupMap(sbe);
	  }
       }
    }

   BaleElement getElement(int off) {
      BaleElement be = id_map.get(off);
      if (be == null) return null;
      // Should we clone be at this point?
      return be;
    }

}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return root_element.toString();
}




}	// end of class BaleElementBuffer



/* end of BaleElementBuffer.java */
