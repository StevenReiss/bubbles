/********************************************************************************/
/*										*/
/*		AbstractVisitor.java						*/
/*										*/
/*	Python Bubbles Base semantic visitor abstraction			*/
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
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants.VisitorType;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.aliasType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractVisitor extends VisitorBase {




protected final List<AbstractToken> token_set  = new ArrayList<AbstractToken>();

/**
 * Module being visited.
 */
protected String	     module_name;

/**
 * Adds a token with a docstring.
 *
 * @param node
 */
protected SourceToken addToken(SimpleNode node)
{
   // add the token
   SourceToken t = makeToken(node, module_name);
   token_set.add(t);
   return t;
}


/**
 * @param node
 * @return
 */
public static SourceToken makeToken(SimpleNode node,String moduleName)
{
   return new SourceToken(node,NodeUtils.getRepresentationString(node),
			     NodeUtils.getNodeArgs(node),NodeUtils.getNodeDocString(node),moduleName);
}

/**
 * same as make token, but returns the full representation for a token, instead of just a 'partial' name
 */
public static SourceToken makeFullNameToken(SimpleNode node,String moduleName)
{
   return new SourceToken(node,NodeUtils.getFullRepresentationString(node),
			     NodeUtils.getNodeArgs(node),NodeUtils.getNodeDocString(node),moduleName);
}


/**
 * This function creates source tokens from a wild import node.
 *
 * @param node the import node
 * @param tokens OUT used to add the source token
 * @param moduleName the module name
 *
 * @return the tokens list passed in or the created one if it was null
 */
public static AbstractToken makeWildImportToken(ImportFrom node,List<AbstractToken> tokens,
						   String moduleName)
{
   if (tokens == null) {
      tokens = new ArrayList<AbstractToken>();
    }
   SourceToken sourceToken = null;
   if (isWildImport(node)) {
      sourceToken = new SourceToken(node,((NameTok) node.module).id,"","",moduleName);
      tokens.add(sourceToken);
    }
   return sourceToken;
}

public static List<AbstractToken> makeImportToken(SimpleNode node,List<AbstractToken> tokens,
						     String moduleName,boolean allowForMultiple)
{
   if (node instanceof Import) {
      return makeImportToken((Import) node, tokens, moduleName, allowForMultiple);
    }
   if (node instanceof ImportFrom) {
      ImportFrom i = (ImportFrom) node;
      if (isWildImport(i)) {
	 makeWildImportToken(i, tokens, moduleName);
	 return tokens;
       }
      return makeImportToken((ImportFrom) node, tokens, moduleName, allowForMultiple);
    }

   throw new RuntimeException("Unable to create token for the passed import (" + node
				 + ")");
}

/**
 * This function creates source tokens from an import node.
 *
 * @param node the import node
 * @param moduleName the module name where this token was found
 * @param tokens OUT used to add the source tokens (may create many from a single import)
 * @param allowForMultiple is used to indicate if an import in the format import os.path should generate one token for os
 * and another for os.path or just one for both with os.path
 *
 * @return the tokens list passed in or the created one if it was null
 */
public static List<AbstractToken> makeImportToken(Import node,List<AbstractToken> tokens,
						     String moduleName,boolean allowForMultiple)
{
   aliasType[] names = node.names;
   return makeImportToken(node, tokens, names, moduleName, "", allowForMultiple);
}

/**
 * The same as above but with ImportFrom
 */
public static List<AbstractToken> makeImportToken(ImportFrom node,List<AbstractToken> tokens,
						     String moduleName,boolean allowForMultiple)
{
   aliasType[] names = node.names;
   String importName = ((NameTok) node.module).id;

   return makeImportToken(node, tokens, names, moduleName, importName, allowForMultiple);
}

/**
 * This class is the same as a regular source token, just used to know that this
 * is a token that was created to identify a part of an import declaration.
 *
 * E.g.:
 *
 * import os.path
 *
 * Will create an 'os' part -- which is leaked to the namespace (but we must
 * identify that because we don't want to report import redefinitions nor unused
 * variables for those).
 *
 * See: https://sourceforge.net/tracker/index.php?func=detail&aid=2879058&group_id=85796&atid=577329
 * and	https://sourceforge.net/tracker/index.php?func=detail&aid=2008026&group_id=85796&atid=577329
 */

public static class ImportPartSourceToken extends SourceToken {

   public ImportPartSourceToken(SimpleNode node,String rep,String doc,String args,
				   String parentPackage,String originalRep,boolean originalHasRep)
      {
      super(node,rep,doc,args,parentPackage,originalRep,originalHasRep);
    }

}	// end of inner class ImportPartSourceToken



/**
 * The same as above
 */
private static List<AbstractToken> makeImportToken(SimpleNode node,List<AbstractToken> tokens,
						      aliasType[] names,String module,
						      String initialImportName,boolean allowForMultiple)
{
   if (tokens == null) {
      tokens = new ArrayList<AbstractToken>();
    }

   if (initialImportName.length() > 0) {
      initialImportName = initialImportName + ".";
    }

   for (int i = 0; i < names.length; i++) {
      aliasType aliasType = names[i];

      String name = null;
      String original = ((NameTok) aliasType.name).id;

      if (aliasType.asname != null) {
	 name = ((NameTok) aliasType.asname).id;
       }

      if (name == null) {
	 FullRepIterable iterator = new FullRepIterable(original);
	 Iterator<String> it = iterator.iterator();
	 while (it.hasNext()) {
	    String rep = it.next();
	    SourceToken sourceToken;
	    if (it.hasNext()) {
	       sourceToken = new ImportPartSourceToken(node,rep,"","",module,
							  initialImportName + rep,true);

	     }
	    else {
	       sourceToken = new SourceToken(node,rep,"","",module,initialImportName
						+ rep,true);
	     }
	    tokens.add(sourceToken);
	  }
       }
      else {
	 SourceToken sourceToken = new SourceToken(node,name,"","",module,
						      initialImportName + original,false);
	 tokens.add(sourceToken);
       }

    }
   return tokens;
}


public static boolean isWildImport(SimpleNode node)
{
   if (node instanceof ImportFrom) {
      ImportFrom n = (ImportFrom) node;
      return isWildImport(n);
    }
   return false;
}

public static boolean isWildImport(AbstractToken generator)
{
   if (generator instanceof SourceToken) {
      SourceToken t = (SourceToken) generator;
      return isWildImport(t.getAst());
    }
   return false;
}

public static boolean isString(SimpleNode ast)
{
   if (ast instanceof Str) {
      return true;
    }
   return false;
}


/**
 * @param node the node to analyze
 * @return whether it is a wild import
 */
public static boolean isWildImport(ImportFrom node)
{
   return node.names.length == 0;
}

/**
 * @param node the node to analyze
 * @return whether it is an alias import
 */
public static boolean isAliasImport(ImportFrom node)
{
   return node.names.length > 0;
}

public List<AbstractToken> getTokens()
{
   return token_set;
}

/**
 * This method transverses the ast and returns a list of found tokens.
 *
 * @param ast
 * @param which
 * @param state
 * @param module_name
 * @param onlyAllowTokensIn__all__: only used when checking global tokens: if true, if a token named __all__ is available,
 * only the classes that have strings that match in __all__ are available.
 * @return
 * @throws Exception
 */
public static List<AbstractToken> getTokens(SimpleNode ast,VisitorType which,String moduleName,
					       CompletionState state,boolean onlyAllowTokensIn__all__)
{
   AbstractVisitor modelVisitor;
   if (which == VisitorType.INNER_DEFS) {
      modelVisitor = new InnerModelVisitor(moduleName,state);
    }
   else {
      modelVisitor = new GlobalModelVisitor(which,moduleName,onlyAllowTokensIn__all__);
    }

   if (ast != null) {
      try {
	 ast.accept(modelVisitor);
       }
      catch (Exception e) {
	 throw new RuntimeException(e);
       }
      modelVisitor.finishVisit();
      return modelVisitor.token_set;
    }
   else {
      return new ArrayList<AbstractToken>();
    }
}

/**
 * This method traverses the ast and returns a model visitor that has the list of found tokens (and other related info, such as __all__, etc.)
 */
public static GlobalModelVisitor getGlobalModuleVisitorWithTokens(SimpleNode ast,
								     VisitorType which,String moduleName,CompletionState state,
								     boolean onlyAllowTokensIn__all__)
{
   if (which == VisitorType.INNER_DEFS) {
      throw new RuntimeException("Only globals for getting the GlobalModelVisitor");
    }
   GlobalModelVisitor modelVisitor = new GlobalModelVisitor(which,moduleName,
							       onlyAllowTokensIn__all__);

   if (ast != null) {
      try {
	 ast.accept(modelVisitor);
       }
      catch (Exception e) {
	 throw new RuntimeException(e);
       }
      modelVisitor.finishVisit();
      return modelVisitor;
    }
   else {
      return modelVisitor;
    }
}


/**
 * This method is available so that subclasses can do some post-processing before the tokens are actually
 * returned.
 */
protected void finishVisit()			{ }



} // end of class AbstractVisitor


/* end of AbstractVisitor.java */
