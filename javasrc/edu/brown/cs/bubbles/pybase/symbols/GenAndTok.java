/********************************************************************************/
/*										*/
/*		GenAndTok							*/
/*										*/
/*	Python Bubbles Base generator and token representation			*/
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 16, 2006
 */



package edu.brown.cs.bubbles.pybase.symbols;


import edu.brown.cs.bubbles.pybase.PybaseScopeItems;

import java.util.ArrayList;
import java.util.List;



public final class GenAndTok {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private AbstractToken	    generator_token;
private AbstractToken	    base_token;
private List<AbstractToken> token_references;
private int	     scope_id;
private PybaseScopeItems   scope_found;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public GenAndTok(AbstractToken generator,AbstractToken tok,int scopeId,PybaseScopeItems scopeFound)
{
   generator_token = generator;
   base_token = tok;
   scope_id = scopeId;
   scope_found = scopeFound;
   token_references = new ArrayList<AbstractToken>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public AbstractToken getGenerator()		       { return generator_token; }

public AbstractToken getToken() 		       { return base_token; }

public void addReference(AbstractToken t)		{ token_references.add(t); }

public int getScopeId() 			{ return scope_id; }

public PybaseScopeItems getScopeFound() 	{ return scope_found; }


public List<AbstractToken> getReferences()
{
   return token_references;
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuilder buffer = new StringBuilder();
   buffer.append("GenAndTok [ ");

   buffer.append(generator_token.getRepresentation());
   buffer.append(" - ");
   buffer.append(base_token.getRepresentation());

   buffer.append(" (scope_id:");
   buffer.append(scope_id);
   buffer.append(") ");

   if (token_references.size() > 0) {
      buffer.append(" (token_references:");
      for (AbstractToken ref : token_references) {
	 buffer.append(ref.getRepresentation());
	 buffer.append(",");
      }
      buffer.deleteCharAt(buffer.length()-1);	// remove last comma
      buffer.append(") ");
   }

   buffer.append("]");
   return buffer.toString();
}



}	// end of class GenAndTok




/* end of GenAndTok.java */
