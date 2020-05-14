/********************************************************************************/
/*										*/
/*		Scope.java							*/
/*										*/
/*	Python Bubbles Base scope representation				*/
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
 * Created on 27/07/2005
 */


package edu.brown.cs.bubbles.pybase.symbols;


import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseScopeItems;
import edu.brown.cs.bubbles.pybase.PybaseSemanticVisitor;
import edu.brown.cs.bubbles.pybase.symbols.ImportChecker.ImportInfo;

import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.ast.TryExcept;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Class used to handle scopes while we're walking through the AST.
 *
 * @author Fabio
 */
public final class Scope implements Iterable<PybaseScopeItems>, PybaseConstants {



/********************************************************************************/
/*										*/
/*	Public static interface 						*/
/*										*/
/********************************************************************************/

/**
 * @param scopeType
 * @return a string representing the scope type
 */
public static String getScopeTypeStr(ScopeType scopetype)
{
   switch (scopetype) {
      case GLOBAL:
	 return "Global Scope";
      case CLASS:
	 return "Class Scope";
      case METHOD:
	 return "Method Scope";
      case LAMBDA:
	 return "Lambda Scope";
      case LIST_COMP:
	 return "List Comp Scope";
      default:
	 break;
   }
   return null;
}


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean        is_in_method_definition = false;
private ImportChecker	 import_checker;
private FastStack<PybaseScopeItems> scope_stack      = new FastStack<PybaseScopeItems>();
private FastStack<Integer>    scope_id		= new FastStack<Integer>();
private int		   scope_unique     = 0;
private PybaseSemanticVisitor	 the_visitor;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public Scope(PybaseSemanticVisitor visitor,PybaseNature nature,String moduleName)
{
   the_visitor = visitor;
   import_checker = new ImportChecker(visitor,nature,moduleName);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/


public boolean isInMethodDefinitiion()
{
   return is_in_method_definition;
}

public void setIsInMethodDefinition(boolean fg)
{
   is_in_method_definition = fg;
}

public PybaseScopeItems currentScope()
{
   if (scope_stack.size() == 0) {
      return null;
   }
   return scope_stack.peek();
}


public PybaseScopeItems getGlobalScope()
{
   return scope_stack.getFirst();
}

@Override public Iterator<PybaseScopeItems> iterator()
{
   return scope_stack.iterator();
}


/********************************************************************************/
/*										*/
/*	Add items to the scope							*/
/*										*/
/********************************************************************************/

/**
 * Adds many tokens at once. (created by the same token)
 * Adding more than one ONLY happens for:
 * - wild imports (kind of obvious)
 * - imports such as import os.path (one token is created for os and one for os.path)
 */
public void addImportTokens(List<AbstractToken> list,AbstractToken generator,
	 ICompletionCache completionCache)
{
   PybaseScopeItems.TryExceptInfo withinExceptNode = scope_stack.peek()
	    .getTryExceptImportError();

   // only report undefined imports if we're not inside a try..except ImportError.
   boolean reportUndefinedImports = (withinExceptNode == null);

   boolean requireTokensToBeImports = false;
   ImportInfo importInfo = null;
   if (generator != null) {
      // it will only enter here if it is a wild import (for other imports, the generator
      // is equal to the
      // import)
      if (!generator.isImport()) {
	 throw new RuntimeException(
		  "Only imports should generate multiple tokens "
			   + "(it may be null for imports in the form import foo.bar, but then all its tokens must be imports).");
      }
      importInfo = import_checker.visitImportToken(generator, reportUndefinedImports,
	       completionCache);

   }
   else {
      requireTokensToBeImports = true;
   }

   PybaseScopeItems m = scope_stack.peek();
   for (Iterator<AbstractToken> iter = list.iterator(); iter.hasNext();) {
      AbstractToken o = iter.next();
      // System.out.println("adding: "+o.getRepresentation());
      Found found = addToken(generator, m, o, o.getRepresentation());
      if (withinExceptNode != null) {
	 withinExceptNode.addFoundImportToTryExcept(found); // may mark previous as
	 // used...
      }

      // the token that we find here is either an import (in the case of some from xxx
      // import yyy or import aa.bb)
      // or a Name, ClassDef, MethodDef, etc. (in the case of wild imports)
      if (requireTokensToBeImports) {
	 if (!o.isImport()) {
	    throw new RuntimeException("Expecting import token");
	 }
	 importInfo = import_checker.visitImportToken(o, reportUndefinedImports,
		  completionCache);
      }
      // can be either the one resolved in the wild import or in this token (if it is not
      // a wild import)
      found.setImportInfo(importInfo);
      the_visitor.onImportInfoSetOnFound(found);
   }
}


public Found addToken(AbstractToken generator,AbstractToken o)
{
   return addToken(generator, o, o.getRepresentation());

}


public Found addToken(AbstractToken generator,AbstractToken o,String rep)
{
   PybaseScopeItems m = scope_stack.peek();
   Found f = addToken(generator, m, o, rep);
   m.putRef(generator,f);

   return f;
}


/**
 * Adds a token to the global scope
 */
public Found addTokenToGlobalScope(AbstractToken generator)
{
   PybaseScopeItems globalScope = getGlobalScope();
   return addToken(generator, globalScope, generator, generator.getRepresentation());
}


/**
 * when adding a token, we also have to check if there is not a token with the same representation
 * added, because if there is, the previous token might not be used at all...
 *
 * @param generator that's the token that generated this representation
 * @param m the current scope items
 * @param o the generator token
 * @param rep the representation of the token (o)
 * @return
 */
public Found addToken(AbstractToken generator,PybaseScopeItems m,AbstractToken o,String rep)
{
   if (generator == null) {
      generator = o;
   }

   Found found = findFirst(rep, false);


   boolean isReimport = false;
   if (!is_in_method_definition && found != null) { // it will be removed from the scope
      if (found.isImport() && generator.isImport()) {
	 isReimport = true;
	 // keep on going, as it still might be used or unused
      }
      else {
	 if (!found.isUsed() && !m.getIsInSubSubScope()) { // it was not used, and we're
	    // not in an if scope...

	    // this kind of unused message should only happen if we are at the same
	    // scope...
	    if (found.getSingle().getScopeFound().getScopeId() == getCurrScopeId()) {

	       // we don't get unused at the global scope or class definition scope unless
	       // it's an import
	       if (ACCEPTED_METHOD_AND_LAMBDA.contains(found.getSingle().getScopeFound().getScopeType()) ||
		     found.isImport()) {
		  the_visitor.onAddUnusedMessage(null, found);
	       }
	    }
	 }
	 else if (!(ACCEPTED_METHOD_AND_LAMBDA.contains(m.getScopeType()) &&
	       found.getSingle().getScopeFound().getScopeType() == ScopeType.CLASS)) {
	    // if it was found but in a class scope (and we're now in a method scope), we
	    // will have to create a new Found.

	    // found... may have been or not used, (if we're in an if scope, that does not
	    // matter, because
	    // we have to group things together for generating messages for all the
	    // occurrences in the if)
	    found.addGeneratorToFound(generator, o, getCurrScopeId(), getCurrScopeItems());

	    // ok, it was added, so, let's call this over because we've appended it to
	    // another found,
	    // no reason to re-add it again.
	    return found;
	 }
      }
   }


   Found newFound = new Found(o,generator,m.getScopeId(),m);
   if (isReimport) {
      if (m.getTryExceptImportError() == null) {
	 // we don't want to add reimport messages if we're within a try..except
	 the_visitor.onAddReimportMessage(newFound);
      }
   }
   m.put(rep, newFound);
   return newFound;
}


/********************************************************************************/
/*										*/
/*	Access items in the scope						*/
/*										*/
/********************************************************************************/

public PybaseScopeItems getCurrScopeItems()
{
   return scope_stack.peek();
}


/**
 * initializes a new scope
 */
public void startScope(ScopeType scopeType)
{
   int newId = getNewId();
   PybaseScopeItems par = null;
   if (scope_stack.size() > 0) par = scope_stack.peek();
   PybaseScopeItems itms = new PybaseScopeItems(newId,scopeType,par);
   scope_stack.push(itms);
   scope_id.push(newId);

}

public int getCurrScopeId()
{
   return scope_id.peek();
}

public PybaseScopeItems endScope()
{
   scope_id.pop();
   return scope_stack.pop();
}

public int size()
{
   return scope_stack.size();
}

/**
 *
 * @param name the name to search for
 * @param setUsed indicates if the found tokens should be marked used
 * @return true if a given name was found in any of the scopes we have so far
 */
public boolean find(String name,boolean setUsed)
{
   return findInScopes(name, setUsed).size() > 0;
}


public List<Found> findInScopes(String name,boolean setUsed)
{
   List<Found> ret = new ArrayList<Found>();
   for (PybaseScopeItems m : scope_stack) {

      Found f = m.getLastAppearance(name);
      if (f != null) {
	 if (setUsed) {
	    f.setUsed(true);
	 }
	 ret.add(f);
      }
   }
   return ret;
}


public Found findFirst(String name,boolean setUsed)
{
   return findFirst(name, setUsed, ACCEPTED_ALL_SCOPES);
}


public Found findFirst(String name,boolean setUsed,Set<ScopeType> acceptedscopes)
{
   Iterator<PybaseScopeItems> topDown = scope_stack.topDownIterator();
   while (topDown.hasNext()) {
      PybaseScopeItems m = topDown.next();
      if (acceptedscopes.contains(m.getScopeType())) {
	 Found f = m.getLastAppearance(name);
	 if (f != null) {
	    if (setUsed) {
	       f.setUsed(true);
	    }
	    return f;
	 }
      }
   }
   return null;
}


/********************************************************************************/
/*										*/
/*	Conditional methods							*/
/*										*/
/********************************************************************************/

public void addIfSubScope()
{
   scope_stack.peek().addIfSubScope();
}

public boolean getIsInIfSubScope()
{
   return scope_stack.peek().getIsInIfSubScope();
}


public void removeIfSubScope()
{
   scope_stack.peek().removeIfSubScope();
}

public void addTryExceptSubScope(TryExcept node)
{
   scope_stack.peek().addTryExceptSubScope(node);
}

public void removeTryExceptSubScope()
{
   scope_stack.peek().removeTryExceptSubScope();
}

/**
 * find out if an item is in the names to ignore given its full representation
 */
public Tuple<AbstractToken, Found> findInNamesToIgnore(String fullRep,
	 Map<String, Tuple<AbstractToken, Found>> lastInStack)
{

   int i = fullRep.indexOf('.', 0);

   while (i >= 0) {
      String sub = fullRep.substring(0, i);
      i = fullRep.indexOf('.', i + 1);
      if (lastInStack.containsKey(sub)) {
	 return lastInStack.get(sub);
      }
   }

   return lastInStack.get(fullRep);
}

/**
 * checks if there is some token in the names that are defined (but should be ignored)
 */
public Tuple<AbstractToken, Found> isInNamesToIgnore(String rep)
{
   ScopeType currscopetype = getCurrScopeItems().getScopeType();

   for (PybaseScopeItems s : scope_stack) {
      // ok, if we are in a scope method, we may not get things that were defined in a
      // class scope.
      if (ACCEPTED_METHOD_AND_LAMBDA.contains(currscopetype) && s.getScopeType() == ScopeType.CLASS) {
	 continue;
      }

      Map<String, Tuple<AbstractToken, Found>> m = s.getNamesToIgnore();
      Tuple<AbstractToken, Found> found = findInNamesToIgnore(rep, m);
      if (found != null) {
	 return found;
      }
   }
   return null;
}


public PybaseScopeItems getPrevScopeItems()
{
   if (scope_stack.size() <= 1) {
      return null;
   }
   return scope_stack.get(scope_stack.size() - 2);
}


/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private int getNewId()
{
   scope_unique++;
   return scope_unique;
}


/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuilder buffer = new StringBuilder();
   buffer.append("Scope: ");
   for (PybaseScopeItems item : scope_stack) {
      buffer.append("\n");
      buffer.append(item);

   }
   return buffer.toString();
}


} // end of class Scope


/* end of Scope.java */
