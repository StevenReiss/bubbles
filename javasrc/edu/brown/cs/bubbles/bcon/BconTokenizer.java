/********************************************************************************/
/*										*/
/*		BconTokenizer.java						*/
/*										*/
/*	Bubbles Environment Context Viewer file scanning methods		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import javax.swing.text.Segment;



class BconTokenizer implements BconConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Segment 	file_text;
private int		cur_offset;
private State		cur_state;



enum State {
   NORMAL,
   IN_STRING,
   IN_CHAR,
   IN_LINE_CMMT,
   IN_BLOCK_CMMT,
   IN_DOC_CMMT,
   IN_OTHER
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconTokenizer(Segment s)
{
   file_text = s;
   cur_offset = 0;
   cur_state = State.NORMAL;
}



/********************************************************************************/
/*										*/
/*	Next token method							*/
/*										*/
/********************************************************************************/

BconToken getNextToken()
{
   int start = cur_offset;

   while (cur_offset < file_text.length()) {
      char c = file_text.charAt(cur_offset++);
      switch (cur_state) {
	 case NORMAL :
	    switch (c) {
	       case '\n' :
		  return newToken(BconTokenType.EOL,start);
	       case '/' :
		  char c1 = file_text.charAt(cur_offset);
		  switch (c1) {
		     case '/' :
			cur_state = State.IN_LINE_CMMT;
			++cur_offset;
			break;
		     case '*' :
			if (file_text.charAt(cur_offset+1) == '*' &&
			       file_text.charAt(cur_offset+2) != '/')
			   cur_state = State.IN_DOC_CMMT;
			else cur_state = State.IN_BLOCK_CMMT;
			++cur_offset;
			break;
		     default :
			cur_state = State.IN_OTHER;
			break;
		   }
		  break;
	       case '"' :
		  cur_state = State.IN_STRING;
		  break;
	       case '\'' :
		  cur_state = State.IN_CHAR;
		  break;
	       case ' ' :
	       case '\t' :
	       case '\f' :
		  start = cur_offset;
		  break;
	       default :
		  cur_state = State.IN_OTHER;
		  break;
	     }
	    break;
	  case IN_OTHER :
	     switch (c) {
		case '\n' :
		   --cur_offset;
		   cur_state = State.NORMAL;
		   return newToken(BconTokenType.OTHER,start);
		case '"' :
		   cur_state = State.IN_STRING;
		   break;
		case '\'' :
		   cur_state = State.IN_CHAR;
		   break;
		case '/' :
		   char c1 = file_text.charAt(cur_offset);
		   if (c1 == '*' || c1 == '/') {
		      --cur_offset;
		      cur_state = State.NORMAL;
		      return newToken(BconTokenType.OTHER,start);
		    }
	      }
	     break;
	  case IN_STRING :
	     switch (c) {
		case '\\' :
		   if (file_text.charAt(cur_offset) != '\n') ++cur_offset;
		   break;
		case '"' :
		   cur_state = State.IN_OTHER;
		   break;
		case '\n' :
		   --cur_offset;
		   cur_state = State.IN_OTHER;
		   break;
	      }
	     break;
	  case IN_CHAR :
	     switch (c) {
		case '\\' :
		   if (file_text.charAt(cur_offset) != '\n') ++cur_offset;
		   break;
		case '\'' :
		   cur_state = State.IN_OTHER;
		   break;
		case '\n' :
		   --cur_offset;
		   cur_state = State.IN_OTHER;
		   break;
	      }
	     break;
	  case IN_LINE_CMMT :
	     switch (c) {
		case '\n' :
		   --cur_offset;
		   cur_state = State.NORMAL;
		   return newToken(BconTokenType.LINE_CMMT,start);
	      }
	     break;
	  case IN_BLOCK_CMMT :
	     switch (c) {
		case '\n' :
		   if (cur_offset - start == 1) return newToken(BconTokenType.EOL,start);
		   --cur_offset;
		   return newToken(BconTokenType.BLOCK_CMMT,start);
		case '*' :
		   if (file_text.charAt(cur_offset) == '/') {
		      ++cur_offset;
		      cur_state = State.NORMAL;
		      return newToken(BconTokenType.BLOCK_CMMT,start);
		    }
		   break;
	      }
	     break;
	  case IN_DOC_CMMT :
	     switch (c) {
		case '\n' :
		   if (cur_offset - start == 1) return newToken(BconTokenType.EOL,start);
		   --cur_offset;
		   return newToken(BconTokenType.DOC_CMMT,start);
		case '*' :
		   if (file_text.charAt(cur_offset) == '/') {
		      ++cur_offset;
		      cur_state = State.NORMAL;
		      return newToken(BconTokenType.DOC_CMMT,start);
		    }
		   break;
	      }
	     break;
       }
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Token creation methods							*/
/*										*/
/********************************************************************************/

private TokenImpl newToken(BconTokenType typ,int start)
{
   return new TokenImpl(typ,start,cur_offset-start);
}




private static class TokenImpl implements BconToken {

   private BconTokenType token_type;
   private int start_offset;
   private int token_length;

   TokenImpl(BconTokenType tt,int off,int len) {
      token_type = tt;
      start_offset = off;
      token_length = len;
    }

   @Override public int getStart()			{ return start_offset; }
   @Override public int getLength()			{ return token_length; }
   @Override public BconTokenType getTokenType()	{ return token_type; }

}	// end of inner class TokenImpl



}	// end of class BconTokenizer




/* end of BconTokenizer.java */
