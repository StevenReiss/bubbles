/********************************************************************************/
/*										*/
/*		BaleIndenter.java						*/
/*										*/
/*	Bubble Annotated Language Editor indentation computations		*/
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

/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;



abstract class BaleIndenter implements BaleConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected BumpClient		bump_client;
protected BaleDocument		bale_document;
protected Segment		doc_text;
protected int			doc_length;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BaleIndenter(BaleDocument bd)
{
   bump_client = BumpClient.getBump();
   bale_document = bd;
   doc_text = new Segment();
   doc_length = bd.getLength();

   try {
      if (doc_length >= 0) bd.getText(0,doc_length,doc_text);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Can't get text for indentation");
    }
}




/********************************************************************************/
/*										*/
/*	Methods to return the indentation for the line at the given offset	*/
/*										*/
/********************************************************************************/

abstract int getDesiredIndentation(int offset);


int getCurrentIndentation(int offset)
{
   bale_document.readLock();
   try {
      return getLeadingWhitespaceLength(offset);
    }
   finally { bale_document.readUnlock(); }
}


int getCurrentIndentationAtOffset(int offset)
{
   bale_document.readLock();
   try {
      return getLeadingWhitespaceLengthAtOffset(offset);
    }
   finally { bale_document.readUnlock(); }
}



abstract int getSplitIndentationDelta(int offset);

abstract int getUnindentSize();




/********************************************************************************/
/*										*/
/*	Text scanning methods							*/
/*										*/
/********************************************************************************/

protected int getLineIndent(int lno)
{
   int soff = bale_document.findLineOffset(lno);
   if (soff < 0) return 0;

   return getLeadingWhitespaceLength(soff);
}



protected int getLeadingWhitespaceLength(int offset)
{
   StringBuffer indent = new StringBuffer();
   int lno = bale_document.findLineNumber(offset);
   int lstart = bale_document.findLineOffset(lno);
   int lend = bale_document.findLineOffset(lno+1)-1;

   if (lstart < 0) return 0;
   if (lend >= doc_text.length()) lend = doc_text.length()-1;

   for (int i = lstart; i < lend; ++i) {
      char ch = doc_text.charAt(i);
      if (!Character.isWhitespace(ch)) break;
      indent.append(ch);
    }

   return computeVisualLength(indent);
}



protected int getLeadingWhitespaceLengthAtOffset(int offset)
{
   StringBuffer indent = new StringBuffer();
   int lno = bale_document.findLineNumber(offset);
   int lstart = bale_document.findLineOffset(lno);
   int lend = bale_document.findLineOffset(lno+1)-1;

   if (lstart < 0) return 0;
   if (offset < lend) lend = offset;
   if (lend >= doc_text.length()) lend = doc_text.length()-1;

   for (int i = lstart; i < lend; ++i) {
      char ch = doc_text.charAt(i);
      if (!Character.isWhitespace(ch)) break;
      indent.append(ch);
    }

   return computeVisualLength(indent);
}




protected int getPositionWhitespaceLength(int offset)
{
   StringBuffer indent = new StringBuffer();
   int lno = bale_document.findLineNumber(offset);
   int lstart = offset;
   int lend = bale_document.findLineOffset(lno+1);
   if (lend >= doc_text.length()) lend = doc_text.length()-1;

   for (int i = lstart; i < lend; ++i) {
      char ch = doc_text.charAt(i);
      if (!Character.isWhitespace(ch)) break;
      indent.append(ch);
    }

   return computeVisualLength(indent);
}




protected int computeVisualLength(CharSequence indent)
{
   int tabsize = getTabSize();
   int length = 0;
   for (int i = 0; i < indent.length(); i++) {
      char ch = indent.charAt(i);
      switch (ch) {
	 case '\t':
	    if (tabsize > 0) {
	       int reminder = length % tabsize;
	       length += tabsize - reminder;
	     }
	    break;
	 case ' ':
	    length++;
	    break;
       }
    }

   return length;
}



protected CharSequence getTokenContent(BaleElement elt)
{
   int soff = elt.getStartOffset();
   int eoff = elt.getEndOffset();
   return doc_text.subSequence(soff,eoff);
}



protected abstract int getTabSize();



/********************************************************************************/
/*										*/
/*	Token scanning methods							*/
/*										*/
/********************************************************************************/

protected BaleElement getNextElement(BaleElement e,boolean stopateol)
{
   if (e == null) return null;

   for (BaleElement r = e.getNextCharacterElement(); r != null; r = r.getNextCharacterElement()) {
      if (!r.isEmpty()) return r;
      if (r.isEndOfLine() && stopateol) return null;
    }

   return null;
}



protected BaleElement getPreviousElement(BaleElement e)
{
   if (e == null) return null;

   for (BaleElement r = e.getPreviousCharacterElement(); r != null; r = r.getPreviousCharacterElement()) {
      if (!r.isEmpty() && !r.isComment()) return r;
    }

   return null;
}



protected BaleElement findOpeningPeer(BaleElement e,BaleTokenType open,BaleTokenType close)
{
   int ct = 0;
   for ( ; ; ) {
      e = getPreviousElement(e);
      if (e == null) return null;
      if (e.getTokenType() == close) ++ct;
      else if (e.getTokenType() == open) {
	 --ct;
	 if (ct < 0) return e;
       }
    }
}



protected BaleTokenType getToken(BaleElement e)
{
   if (e == null) return BaleTokenType.NONE;
   return e.getTokenType();
}



}	// end of class BaleIndenter



/* end of BaleIndenter.java */


