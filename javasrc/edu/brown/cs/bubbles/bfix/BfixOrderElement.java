/********************************************************************************/
/*										*/
/*		BfixOrderElement.java						*/
/*										*/
/*	Generic element that can be part of an ordering 			*/
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

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpSymbolType;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;



abstract class BfixOrderElement implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static BfixOrderElement create(BfixCorrector corr,BumpLocation bl)
{
   return new LocationElement(corr,bl);
}


static BfixOrderElement create(BfixCorrector corr,int spos,int epos,String type,
				  CharSequence body)
{
   if (type.equals("BLANKS"))  
      return new BlankElement(corr,spos,epos,body);
   else
      return new CommentElement(corr,spos,epos,type,body);
}





/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int		start_offset;
private int		end_offset;
private String          element_type;
private BumpSymbolType	symbol_type;
private String		symbol_name;
private BfixOrderElement parent_element;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BfixOrderElement(int start,int end,String name,BumpSymbolType stype)
{
   this(start,end,name,stype.toString(),stype);
}

protected BfixOrderElement(int start,int end,String name,String type)
{
   this(start,end,name,type,null);
}


protected BfixOrderElement(int start,int end,String name,String etype,
      BumpSymbolType stype)
{
   start_offset = start;
   end_offset = end;
   symbol_type = stype;
   element_type = etype;
   symbol_name = name;
   parent_element = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getStartOffset()			{ return start_offset; }
int getEndOffset()			{ return end_offset; }
void setPosition(int start,int end)
{
   if (start >= 0) start_offset = start;
   if (end >= 0) end_offset = end;
}

int getModifiers()			{ return 0; }
String getName()			{ return symbol_name; }
BumpSymbolType getSymbolType()		{ return symbol_type; }
String getElementType()                 { return element_type; }
String getContents()                    { return null; }

Collection<BfixOrderElement> getChildren()	{ return Collections.emptyList(); }

void addChild(BfixOrderElement e)
{
   BoardLog.logE("BFIX", "Attempt to add child to leaf element");
}

BfixOrderElement getParent()		{ return parent_element; }
void setParent(BfixOrderElement p)	{ parent_element = p; }



/********************************************************************************/
/*										*/
/*	Output Methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   if (symbol_name != null) buf.append(symbol_name);
   buf.append("(");
   buf.append(element_type);
   buf.append(")@");
   buf.append(start_offset);
   buf.append(":");
   buf.append(end_offset);
   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Element based on a BumpLocation 					*/
/*										*/
/********************************************************************************/

private static class LocationElement extends BfixOrderElement {

   private Collection<BfixOrderElement> child_elements;

   LocationElement(BfixCorrector corr,BumpLocation bl) {
      super(bl.getDefinitionOffset(),bl.getDefinitionEndOffset(),
         bl.getSymbolName(),bl.getSymbolType());
      child_elements = new TreeSet<BfixOrderElement>(new ElementComparator());
    }

   @Override Collection<BfixOrderElement> getChildren() {
      return child_elements;
    }
   
   @Override void addChild(BfixOrderElement e) {
      child_elements.add(e);
      e.setParent(this);
    }

}	// end of inner class LocationElement




/********************************************************************************/
/*										*/
/*	Comment element 							*/
/*										*/
/********************************************************************************/

private static class CommentElement extends BfixOrderElement {

   @SuppressWarnings("unused") private String comment_body;
   @SuppressWarnings("unused") private int	  blank_before;
   private int	  blank_after;

   CommentElement(BfixCorrector corr,int start,int end,String type,
        	     CharSequence body) {
      super(start,end,null,type);
      blank_before = 0;
      blank_after = 0;
   
      int cstart = 0;
      int lstart = 0;
      for ( ; ; ) {
         char ch = body.charAt(cstart);
         if (Character.isWhitespace(ch)) {
            if (ch == '\n') {
               ++blank_before;
               lstart = cstart+1;
             }
            ++cstart;
          }
         else {
            cstart = lstart;
            break;
          }
       }
      int cend = body.length();
      lstart = cend;
      for ( ; ; ) {
         char ch = body.charAt(cend-1);
         if (Character.isWhitespace(ch)) {
            if (ch == '\n') {
               lstart = cend;
               ++blank_after;
             }
            --cend;
          }
         else {
            cend = lstart;
            if (blank_after > 0) --blank_after;
            break;
          }
       }
   
      comment_body = body.subSequence(cstart,cend).toString();
    }
   
}	// end of inner class LocationElement



/********************************************************************************/
/*										*/
/*	Blank element								*/
/*										*/
/********************************************************************************/

private static class BlankElement extends BfixOrderElement {

   private int num_lines;

   BlankElement(BfixCorrector corr,int start,int end,
        	   CharSequence body) {
      super(start,end,null,"BLANKS");
      num_lines = 0;
      int len = body.length();
      for (int p = 0; p < len; ++p) {
         char ch = body.charAt(p);
         if (ch == '\n') ++num_lines;
       }
      if (num_lines > 0) --num_lines;
    }

}	// end of inner class LocationElement




}	// end of class BfixOrderElement




/* end of BfixOrderElement.java */

























