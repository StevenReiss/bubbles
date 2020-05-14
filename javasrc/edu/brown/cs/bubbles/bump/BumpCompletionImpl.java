/********************************************************************************/
/*										*/
/*		BumpCompletionImpl.java 					*/
/*										*/
/*	BUblles Mint Partnership completion representation			*/
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


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.lang.reflect.Modifier;



class BumpCompletionImpl implements BumpConstants.BumpCompletion, BumpConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int		access_flags;
private CompletionType	completion_type;
private String		decl_signature;
private String		signature_text;
private String		completion_name;
private String		completion_text;
private int		completion_start;
private int		completion_end;
private int		completion_relevance;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpCompletionImpl(Element e)
{
   access_flags = IvyXml.getAttrInt(e,"FLAGS");
   completion_type = IvyXml.getAttrEnum(e,"KIND",CompletionType.OTHER);
   signature_text = IvyXml.getTextElement(e,"SIGNATURE");       // actual signature
   decl_signature = IvyXml.getTextElement(e,"DECLSIGN");        // class of declaration
   completion_name = IvyXml.getTextElement(e,"NAME");
   completion_text = IvyXml.getTextElement(e,"TEXT");
   completion_start = IvyXml.getAttrInt(e,"REPLACE_START");
   completion_end = IvyXml.getAttrInt(e,"REPLACE_END");
   completion_relevance = IvyXml.getAttrInt(e,"RELEVANCE");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isPublic()		{ return Modifier.isPublic(access_flags); }
@Override public boolean isPrivate()		{ return Modifier.isPrivate(access_flags); }
@Override public boolean isProtected()		{ return Modifier.isProtected(access_flags); }

@Override public boolean isAbstract()		{ return Modifier.isAbstract(access_flags); }
@Override public boolean isFinal()		{ return Modifier.isFinal(access_flags); }
@Override public boolean isNative()		{ return Modifier.isNative(access_flags); }
@Override public boolean isStatic()		{ return Modifier.isStatic(access_flags); }
@Override public boolean isSynchronized()	{ return Modifier.isSynchronized(access_flags); }
@Override public boolean isStrict()		{ return Modifier.isStrict(access_flags); }
@Override public boolean isTransient()		{ return Modifier.isTransient(access_flags); }
@Override public boolean isVolatile()		{ return Modifier.isVolatile(access_flags); }




@Override public CompletionType getType()	{ return completion_type; }

@Override public String getCompletion() 	{ return completion_text; }
@Override public String getName()		{ return completion_name; }

@Override public int getReplaceStart()		{ return completion_start; }
@Override public int getReplaceEnd()		{ return completion_end; }

@Override public int getRelevance()		{ return completion_relevance; }




/********************************************************************************/
/*										*/
/*	Signature methods							*/
/*										*/
/********************************************************************************/

@Override public String getDeclaringType()
{
   try {
      return IvyFormat.formatTypeName(decl_signature);
    }
   catch (Throwable t) {
      BoardLog.logE("BUMP","Problem getting type name for " + decl_signature,t);
      return decl_signature;
    }
}


@Override public String getSignature()		{ return signature_text; }

//TODO: add methods to parse the signature into return type and parameter types



}	// end of class BumpCompletionImpl




/* end of BumpCompletionImpl.java */
