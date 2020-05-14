/********************************************************************************/
/*										*/
/*		BaleIndenterPython.java 					*/
/*										*/
/*	Bubble Annotated Language Editor indentation computations for Python	*/
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


/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bale;



class BaleIndenterPython extends BaleIndenter implements BaleConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int	indent_space;
private int	continue_space;
private int	paren_indent;
private int	tab_size;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleIndenterPython(BaleDocument bd)
{
   super(bd);

   loadProperties();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override protected int getTabSize()
{
   return tab_size;
}


@Override int getUnindentSize()
{
   return indent_space;
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override int getSplitIndentationDelta(int offset)
{
   bale_document.readLock();
   try {
      int cur = getCurrentIndentation(offset);
      return cur + paren_indent;
    }
   finally { bale_document.readUnlock(); }
}



@Override int getDesiredIndentation(int offset)
{
   int ind = 0;
   // if this is a new line we don't need to compute exdents
   bale_document.readLock();
   try {
      int lno = bale_document.findLineNumber(offset);
      int loff = bale_document.findLineOffset(lno);
      BaleElement elt = bale_document.getActualCharacterElement(loff);
      // computeExdents();
      ind = findLineIndentation(elt);
    }
   finally { bale_document.readUnlock(); }

   return ind;
}




/********************************************************************************/
/*										*/
/*	Python indentation code 						*/
/*										*/
/********************************************************************************/

private int findLineIndentation(BaleElement elt)
{
   int delta = 0;
   boolean continuation = false;
   int base = 0;
   BaleAstNode startast = elt.getAstNode();
   BaleAstNode lineast = null;

   for (BaleElement nelt = elt; nelt != null; nelt = getNextElement(nelt,true)) {
      if (!nelt.isEmpty()) {
	 startast = nelt.getAstNode();
	 switch (nelt.getTokenType()) {
	    case ELSE : 			// includes elif
	       delta = -1;
	       break;
	    default:
	       break;
	  }
	 break;
       }
      else if (nelt.isEndOfLine()) break;
    }

   boolean eol = elt.isEndOfLine();
//   BaleElement.Indent ind0 = elt.getIndent();
//   if (ind0 != null) {
//	int exd = ind0.getNumExdent();
//	if (exd > 0) delta = -exd;
//    }

   int pct = 0;
   int lpos = -1;
   BaleElement pelt = elt.getPreviousCharacterElement();
   for ( ; pelt != null; pelt = pelt.getPreviousCharacterElement()) {
      if (!pelt.isEmpty()) lineast = pelt.getAstNode();
      if (eol) {
	 switch (pelt.getTokenType()) {
	    case BACKSLASH :
	       continuation = true;
	       break;
	    case COLON :
	       delta += 1;
	       break;
	    default:
	       break;
	  }
       }
      switch (pelt.getTokenType()) {
	 case LBRACE :
	 case LBRACKET :
	 case LPAREN :
	    pct += 1;
	    break;
	 case RBRACE :
	 case RBRACKET :
	 case RPAREN :
	    pct -= 1;
	    break;
	 case BREAK :
	 case CONTINUE :
	 case RETURN :
	 case PASS :
	 case RAISE :
	    delta -= 1;
	    break;
	 case CLASS :
	 case ELSE :
	 // anything that can start a statement here
	    break;
	 default:
	    break;
       }
      // check for INDENT element, get information from it if so
      BaleElement.Indent ind = pelt.getIndent();
      if (ind != null) {
	 int frst = ind.getFirstColumn();
	 if (checkRelevantLine(pelt)) {
	    base = frst-1;
	    break;
	  }
       }
      if (pelt.isEndOfLine()) {
	 if (pct != 0) continuation = true;
	 else {
	    int epos = pelt.getEndOffset();
	    if (epos == lpos) {
	       base = 0;
	       break;
	    }
	 }
	 lpos = -1;
	 eol = true;
       }
      else {
	 lpos = pelt.getStartOffset();
      }
    }

   while (startast != null && startast.getNodeType() != BaleAstNodeType.BLOCK)
      startast = startast.getParent();

   if (startast != null && lineast != null) {
      int bct = 0;
      for (BaleAstNode n = lineast; n != null; n = n.getParent()) {
	 if (n == startast) {
	    delta -= bct;
	    break;
	 }
	 else if (n.getNodeType() == BaleAstNodeType.BLOCK) ++bct;
      }

   }

   if (continuation) return base + continue_space;
   if (delta >= 0) return base + delta * indent_space;
   if (base == 0 || pelt == null) return 0;

   while (delta < 0) {
      pelt = pelt.getPreviousCharacterElement();
      if (pelt == null) return 0;
      BaleElement.Indent ind = pelt.getIndent();
      if (ind != null && checkRelevantLine(pelt)) {
	 int frst = ind.getFirstColumn();
	 if (frst < base) {
	    base = frst - 1;
	    ++delta;
	  }
       }
    }

   return base;
}




private boolean checkRelevantLine(BaleElement elt)
{
   // first ensure the line is not a continuation
   BaleElement pelt = elt.getPreviousCharacterElement();
   if (pelt.isEndOfLine()) pelt = pelt.getPreviousCharacterElement();
   if (pelt.getTokenType() == BaleTokenType.BACKSLASH) return false;

   // then check that this line has content
   BaleElement nelt = elt.getNextCharacterElement();
   while (nelt != null && !nelt.isEndOfLine()) {
      if (!nelt.isEmpty()) return true;
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Property methods							*/
/*										*/
/********************************************************************************/

private void loadProperties()
{
    indent_space = BALE_PROPERTIES.getInt("Bale.python.indent",4);
    continue_space = BALE_PROPERTIES.getInt("Bale.python.continue.indent",4);
    // use_tabs = BALE_PROPERTIES.getBoolean("Bale.python.use_tabs",false);
    paren_indent = BALE_PROPERTIES.getInt("Bale.python.paren_indent",2);
    tab_size = BALE_PROPERTIES.getInt("Bale.python.tabsize",8);
}



/********************************************************************************/
/*										*/
/*	Current indentation computation 					*/
/*										*/
/********************************************************************************/

/*********************************

private static final int MIN_DELTA = 1; 	// min space difference that is significant


private void computeExdents()
{
   Stack<Integer> offsets = new Stack<Integer>();
   offsets.push(0);
   boolean eol = true;
   boolean havecnts = false;
   boolean contline = false;
   BaleElement.Indent curind = null;
   int pct = 0;

   BaleElement e0 = bale_document.getActualCharacterElement(0);
   while (e0 != null) {
      if (eol && pct == 0 && !contline) {
	 havecnts = false;
	 curind = null;
	 BaleElement.Indent in = e0.getIndent();
	 if (in != null) {
	    curind = in;
	    int wd = in.getFirstColumn() - 1;
	    if (Math.abs(wd - offsets.peek()) <= MIN_DELTA) {
	       in.setNumExdent(0);
	     }
	    else if (wd > offsets.peek()) {
	       in.setNumExdent(-1);
	       offsets.push(wd);
	     }
	    else {
	       int ect = 0;
	       while (wd < offsets.peek()) {
		  ++ect;
		  offsets.pop();
		}
	       if (wd > offsets.peek()) {
		  offsets.pop();
		  offsets.push(wd);
		}
	       in.setNumExdent(ect);
	     }
	  }
	 else if (e0.isComment()) ;
	 else if (e0.isEndOfLine()) ;
	 else {
	    while (offsets.size() > 1) offsets.pop();
	  }
       }
      else if (eol) {
	 BaleElement.Indent in = e0.getIndent();   // continuation line of some sort
	 if (in != null) {
	    in.setNumExdent(EXDENT_CONTINUE);
	  }
       }
      else if (!e0.isEmpty()) havecnts = true;
      if (e0.isEndOfLine()) {
	 if (!havecnts && curind != null) curind.setNumExdent(-2);
	 eol = true;
       }
      else eol = false;
      switch (e0.getTokenType()) {
	 case LBRACE :
	 case LBRACKET :
	 case LPAREN :
	     ++pct;
	     contline = false;
	     break;
	 case RBRACE :
	 case RBRACKET :
	 case RPAREN :
	    if (pct > 0) --pct;
	    contline = false;
	    break;
	 case BACKSLASH :
	    contline = true;
	    break;
	 default :
	    contline = false;
	    break;
       }
      e0 = e0.getNextCharacterElement();
    }
}
************************/



}	// end of class BaleIndenterPython




/* end of BaleIndenterPython.java */
