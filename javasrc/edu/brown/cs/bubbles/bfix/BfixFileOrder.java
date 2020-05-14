/********************************************************************************/
/*										*/
/*		BfixFileOrder.java						*/
/*										*/
/*	Representation of the current order of elements in a file		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


class BfixFileOrder implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BfixCorrector		for_corrector;
private SortedSet<BfixOrderElement> elements_set;
private boolean 		needs_update;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixFileOrder(BfixCorrector bcor)
{
   for_corrector = bcor;
   elements_set = new TreeSet<BfixOrderElement>(new ElementComparator());
   needs_update = true;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void noteChange()
{
   needs_update = true;
}



Collection<BfixOrderElement> getRoots()
{
   if (needs_update) {
      setupElements();
      needs_update = false;
    }

   return elements_set;
}


BfixCorrector getCorrector()            { return for_corrector; }



/********************************************************************************/
/*										*/
/*	Methods to set up the set of elements					*/
/*										*/
/********************************************************************************/

private void setupElements()
{
   BumpClient bump = BumpClient.getBump();
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   BaleFileOverview basedoc = doc.getBaseWindowDocument();
   File file = doc.getFile();
   String project = doc.getProjectName();

   elements_set.clear();

   int minpos = -1;
   int maxpos = 0;
   List<BumpLocation> locs = bump.findAllDeclarations(project,file,null,true);
   while (locs != null && !locs.isEmpty()) {
      List<BumpLocation> newlocs = new ArrayList<BumpLocation>();
      for (BumpLocation bl : locs) {
	 BfixOrderElement be = BfixOrderElement.create(for_corrector,bl);
	 if (minpos < 0 || be.getStartOffset() < minpos)
	    minpos = be.getStartOffset();
	 if (be.getEndOffset() > maxpos) maxpos = be.getEndOffset();
	 elements_set.add(be);
       }
      for (BumpLocation bl : locs) {
	 switch (bl.getSymbolType()) {
	    case ENUM :
	    case CLASS :
	    case INTERFACE :
	       if (bl.getDefinitionOffset() == minpos) break;
	       List<BumpLocation> nlocs;
	       nlocs = bump.findAllDeclarations(project,file,bl.getSymbolName(),false);
	       if (nlocs != null) newlocs.addAll(nlocs);
	       break;
	    default :
	       break;
	 }
      }

      locs = newlocs;
    }

   // start at first class, not start of file, end at last class, not end of file
   if (minpos >= 0) {
      Segment s = new Segment();
      try {
	 basedoc.getText(minpos,maxpos-minpos+1,s);
      }
      catch (BadLocationException e) {
	 BoardLog.logE("BFIX","Problem reading file",e);
      }

      commentScan(s,minpos);
    }

   // nest elements as needed
   BfixOrderElement prior = null;
   for (Iterator<BfixOrderElement> it = elements_set.iterator(); it.hasNext(); ) {
      BfixOrderElement elt = it.next();
      while (prior != null) {
	 if (prior.getStartOffset() <= elt.getStartOffset() &&
	       prior.getEndOffset() >= elt.getEndOffset()) {
	    it.remove();
	    prior.addChild(elt);
	    break;
	  }
	 else prior = prior.getParent();
       }
      prior = elt;
    }
}




/********************************************************************************/
/*										*/
/*	Comment scanning methods						*/
/*										*/
/********************************************************************************/

private void commentScan(Segment s,int offset)
{
   BfixOrderTokenizer btok = new BfixOrderTokenizer(s);
   BfixToken cmmtstart = null;		// current comment start
   BfixToken laststart = null;		// possible start of adjacent comments
   boolean isjavadoc = false;		// current is javadoc
   boolean lasteol = true;		// prior token was an eol
   boolean havecmmt = false;

   for ( ; ; ) {
      BfixToken tok = btok.getNextToken();
      if (tok == null) break;

      switch (tok.getTokenType()) {
	 case EOL :
	    if (lasteol && cmmtstart == null) cmmtstart = tok;
	    lasteol = true;
	    break;
	 case BLOCK_CMMT :
	 case DOC_CMMT :
	 case LINE_CMMT :
	    if (lasteol && cmmtstart == null) {
	       cmmtstart = tok;
	       laststart = null;
	       havecmmt = true;
	       isjavadoc = (tok.getTokenType() == BfixTokenType.DOC_CMMT);
	     }
	    else if (lasteol) {
	       isjavadoc &= (tok.getTokenType() == BfixTokenType.DOC_CMMT);
	       laststart = tok;
	       havecmmt = true;
	     }
	    lasteol = false;
	    break;
	 default:
	 case OTHER :
	    if (cmmtstart != null) {
	       BfixToken etok = (lasteol ? tok : laststart);
	       int eoff = etok.getStart();
	       String typ = "COMMENT";
	       if (isjavadoc) typ = "JAVADOC";
	       else if (!havecmmt) typ = "BLANKS";
	       int soff = cmmtstart.getStart();
	       outputComment(offset,cmmtstart,etok,typ,s.subSequence(soff,eoff));
	     }
	    cmmtstart = null;
	    lasteol = false;
	    havecmmt = false;
	    isjavadoc = false;
	    break;
       }
    }
}








private void outputComment(int offset,BfixToken start,BfixToken end,String type,
			      CharSequence body)
{
   int spos = start.getStart() + offset;
   int epos = end.getStart() + offset;

   for (BfixOrderElement br : elements_set) {
      if (br.getStartOffset() >= spos && epos > br.getStartOffset() &&
	     epos < br.getEndOffset()) {
	 elements_set.remove(br);
	 br.setPosition(epos,br.getEndOffset());
	 elements_set.add(br);
	 break;
       }
    }

   BfixOrderElement brc;
   brc = BfixOrderElement.create(for_corrector,spos,epos,type,body);
   elements_set.add(brc);
}



}	// end of class BfixFileOrder




/* end of BfixFileOrder.java */

