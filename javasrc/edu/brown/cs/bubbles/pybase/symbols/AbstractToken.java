/********************************************************************************/
/*										*/
/*		AbstractToken.java						*/
/*										*/
/*	Python Bubbles Base token representation				*/
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
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;


/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractToken implements PybaseConstants {



/********************************************************************************/
/*										*/
/*	Constant definitions							*/
/*										*/
/********************************************************************************/

/**
 * Constant to indicate that it was not possible to know in which line the
 * token was defined.
 */
public static final int UNDEFINED = -1;



/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

protected String token_rep;
protected String original_rep;
protected String token_doc;
protected String token_args;
protected String parent_package;
public TokenType token_type;
private boolean  original_has_rep;




public AbstractToken(String rep,String doc,String args,String parentPackage,TokenType type,
	 String originalRep,boolean originalHasRep)
{
   this(rep,doc,args,parentPackage,type);
   original_rep = originalRep;
   original_has_rep = originalHasRep;
}



public AbstractToken(String rep,String doc,String args,String parentPackage,TokenType type)
{
   if (rep != null) token_rep = rep;
   else token_rep = "";

   if (args != null) token_args = args;
   else token_args = "";

   original_rep = token_rep;

   if (doc != null) token_doc = doc;
   else token_doc = "";

   if (parentPackage != null) parent_package = parentPackage;
   else parent_package = "";

   token_type = type;
}


public String getArgs()
{
   return token_args;
}

public void setArgs(String args)
{
   token_args = args;
}

public String getRepresentation()
{
   return token_rep;
}

public void setDocStr(String docStr)
{
   token_doc = docStr;
}

public String getDocStr()
{
   return token_doc;
}

public String getParentPackage()
{
   return parent_package;
}

public TokenType getType()
{
   return token_type;
}


public SimpleNode getAst()			{ return null; }




/**
 * @see java.lang.Object#equals(java.lang.Object)
 */
@Override public boolean equals(Object obj)
{
   if (!(obj instanceof AbstractToken)) {
      return false;
   }

   AbstractToken c = (AbstractToken) obj;

   if (c.getRepresentation().equals(getRepresentation()) == false) {
      return false;
   }

   if (c.getParentPackage().equals(getParentPackage()) == false) {
      return false;
   }

   if (c.getType() != getType()) {
      return false;
   }

   return true;

}

/**
 * @see java.lang.Object#hashCode()
 */
@Override public int hashCode()
{
   return getRepresentation().hashCode() * getType().hashCode();
}


/**
 * @see java.lang.Comparable#compareTo(java.lang.Object)
 */
public int compareTo(Object o)
{
   AbstractToken comp = (AbstractToken) o;

   TokenType thisT = getType();
   TokenType otherT = comp.getType();

   if (thisT != otherT) {
      if (thisT == TokenType.PARAM || thisT == TokenType.LOCAL
	       || thisT == TokenType.OBJECT_FOUND_INTERFACE) return -1;

      if (otherT == TokenType.PARAM || otherT == TokenType.LOCAL
	       || otherT == TokenType.OBJECT_FOUND_INTERFACE) return 1;

      if (thisT == TokenType.IMPORT) return -1;

      if (otherT == TokenType.IMPORT) return 1;
   }


   int c = getRepresentation().compareTo(comp.getRepresentation());
   if (c != 0) return c;

   c = getParentPackage().compareTo(comp.getParentPackage());
   if (c != 0) return c;

   return c;
}

/**
 * @see java.lang.Object#toString()
 */
@Override public String toString()
{

   if (getParentPackage() != null && getParentPackage().length() > 0) {
      return new StringBuilder(getRepresentation()).append(" - ")
	       .append(getParentPackage()).toString();
   }
   else {
      return getRepresentation();
   }
}

private String getOriginalRep(boolean decorateWithModule)
{
   if (!decorateWithModule) {
      return original_rep;
   }

   String p = getParentPackage();
   if (p != null && p.length() > 0) {
      return p + "." + original_rep;
   }
   return original_rep;
}

/**
 * Make our complete path relative to the base module.
 */
public String getAsRelativeImport(String baseModule)
{
   String completePath = getOriginalRep(true);

   return makeRelative(baseModule, completePath);
}

public String getAsAbsoluteImport()
{
   return getAsRelativeImport(".");
}

/**
 * @param baseModule this is the 'parent package'. The path passed will be made relative to it
 * @param completePath this is the path that we want to make relative
 * @return the relative path.
 *
 * e.g.: if the baseModule is aa.xx and the completePath is aa.xx.foo.bar, this
 * funcion would return aa.foo.bar
 */
public static String makeRelative(String baseModule,String completePath)
{
   if (baseModule == null) {
      return completePath;
   }

   if (completePath.startsWith(baseModule)) {
      String relative = completePath.substring(baseModule.length());

      baseModule = FullRepIterable.headAndTail(baseModule)[0];

      if (baseModule.length() == 0) {
	 if (relative.length() > 0 && relative.charAt(0) == '.') {
	    return relative.substring(1);
	 }
      }
      if (relative.length() > 0 && relative.charAt(0) == '.') {
	 return baseModule + relative;
      }
      else {
	 return baseModule + '.' + relative;
      }
   }
   return completePath;
}

/**
 * @return the original representation (useful for imports)
 * e.g.: if it was import coilib.Exceptions as Exceptions, would return coilib.Exceptions
 */
public String getOriginalRep()
{
   return original_rep;
}

/**
 * @return the original representation without the actual representation (useful for imports, because
 * we have to look within __init__ to check if the token is defined before trying to gather modules, if
 * we have a name clash).
 *
 * e.g.: if it was from coilib.test import Exceptions, it would return coilib.test
 *
 * @note: if the rep is not a part of the original representation, this function will return an empty string.
 */
public String getOriginalWithoutRep()
{
   int i = original_rep.length() - token_rep.length() - 1;
   if (!original_has_rep) {
      return "";
   }
   return i > 0 ? original_rep.substring(0, i) : "";
}

public int getLineDefinition()
{
   return UNDEFINED;
}

public int getColDefinition()
{
   return UNDEFINED;
}

public boolean isImport()
{
   return false;
}

public boolean isImportFrom()
{
   return false;
}

public boolean isWildImport()
{
   return false;
}

public boolean isString()
{
   return false;
}

/**
 * This representation may not be accurate depending on which tokens we are dealing with.
 */
public int[] getLineColEnd()
{
   return new int[] { UNDEFINED, UNDEFINED };
}

public static boolean isClassDef(AbstractToken element)
{
   if (element instanceof SourceToken) {
      SourceToken token = (SourceToken) element;
      SimpleNode ast = token.getAst();
      if (ast instanceof ClassDef) {
	 return true;
      }
   }
   return false;
}




} // end of class AbstractToken


/* end of AbstractToekn.java */

