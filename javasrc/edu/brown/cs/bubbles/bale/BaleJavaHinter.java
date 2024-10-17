/********************************************************************************/
/*										*/
/*		BaleJavaHinter.java						*/
/*										*/
/*	Find hints for a particular element for Java				*/
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



package edu.brown.cs.bubbles.bale;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;

class BaleJavaHinter implements BaleConstants.BaleHinter, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleJavaHinter()
{
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getPreHint(BaleElement e)
{
   if (!isRelevant(e)) return null;

   return null;
}


@Override public String getPostHint(BaleElement e)
{
   if (!BALE_PROPERTIES.getBoolean("Bale.show.hints")) return null;
   if (!isRelevant(e)) return null;
   BaleAstNode an = e.getAstNode();
   Element hint = null;
   if (an == null) {
      hint = e.getOldHintData();
    }
   else {
      hint = an.getHintData();
    }
   if (hint == null) return null;

   String kind = IvyXml.getAttrString(hint,"KIND");
   if (kind.equals("METHOD")) {
      int np = IvyXml.getAttrInt(hint,"NUMPARAM");
      if (np < 2) return null;
      int argno = getArgNumber(e);
      if (argno >= 0) {
	 Element pe = null;
	 int ct = 0;
	 for (Element chld : IvyXml.children(hint,"PARAMETER")) {
	    if (ct++ == argno) {
	       pe = chld;
	       break;
	     }
	  }
	 if (pe != null) {
	    String nm = IvyXml.getAttrString(pe,"NAME");
	    if (nm == null || nm.equals("arg" + argno)) {
	       String tnm = IvyXml.getAttrString(pe,"TYPE");
	       String tnm1 = IvyFormat.formatTypeName(tnm);
	       int idx = tnm1.lastIndexOf(".");
	       if (idx > 0) nm = tnm1.substring(idx+1);
	       else nm = tnm1;
	     }
	    return " " + nm +": ";
	  }
       }
    }
   return null;
}


private int getArgNumber(BaleElement be)
{
   int argno = -1;

   switch (be.getTokenType()) {
      default :
	 break;
      case LPAREN :
	 return 0;
      case COMMA :
	 int dep = 0;
	 argno = 1;
	 BaleElement prev = be;
	 for (int i = 0; i < 1024; ++i) {
	    prev = prev.getPreviousCharacterElement();
	    if (prev == null) return -1;
	    switch (prev.getTokenType()) {
	       default :
		  break;
	       case RPAREN :
		  ++dep;
		  break;
	       case LPAREN :
		  if (dep <= 0) return argno;
		  else --dep;
		  break;
	       case COMMA :
		  if (dep == 0) ++argno;
		  break;
	       case SEMICOLON :
		  return -1;
	       case IF :
	       case WHILE :
	       case FOR :
	       case BREAK :
	       case CASE :
	       case CATCH :
	       case CLASS :
	       case CONTINUE :
	       case DEFAULT :
	       case ENUM :
	       case INTERFACE :
	       case FINALLY :
	       case SWITCH :
	       case TRY :
	       case SYNCHRONIZED :
		  return -1;
	     }
	  }
    }

   return -1;
}


/********************************************************************************/
/*										*/
/*	Check relevance 							*/
/*										*/
/********************************************************************************/

private boolean isRelevant(BaleElement e)
{
   return true;
}


}	// end of class BaleJavaHinter




/* end of BaleJavaHinter.java */

