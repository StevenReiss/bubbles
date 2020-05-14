/********************************************************************************/
/*										*/
/*		LocalScope.java 						*/
/*										*/
/*	Python Bubbles Base local scope representaitons 			*/
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
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Fabio Zadrozny
 */
public class LocalScope implements PybaseConstants {

// the first node from the stack is always the module itself (if it's not there, it means
// it is a compiled module scope)
public FastStack<SimpleNode> source_scope = new FastStack<SimpleNode>();

public int		   scopeEndLine = -1;

public int		   ifMainLine	= -1;


/**
 * Used to create without an initial scope. It may be changed later by using the getScopeStack() and
 * adding tokens.
 */
public LocalScope()
{

}

public LocalScope(FastStack<SimpleNode> scope)
{
   source_scope.addAll(scope);
}

public FastStack<SimpleNode> getScopeStack()
{
   return source_scope;
}

/**
 * @see java.lang.Object#equals(java.lang.Object)
 */
@Override public boolean equals(Object obj)
{
   if (!(obj instanceof LocalScope)) {
      return false;
   }

   LocalScope s = (LocalScope) obj;

   if (source_scope.size() != s.source_scope.size()) {
      return false;
   }

   return checkIfScopesMatch(s);
}



public boolean isOuterOrSameScope(LocalScope s)
{
   if (source_scope.size() > s.getScopeStack().size()) {
      return false;
   }

   return checkIfScopesMatch(s);
}

/**
 * @param s the scope we're checking for
 * @return if the scope passed as a parameter starts with the same scope we have here. It should not be
 * called if the size of the scope we're checking is bigger than the size of 'this' scope.
 */
private boolean checkIfScopesMatch(LocalScope s)
{
   Iterator<SimpleNode> otIt = s.getScopeStack().iterator();

   for (Iterator<SimpleNode> iter = source_scope.iterator(); iter.hasNext();) {
      SimpleNode element = iter.next();
      SimpleNode otElement = otIt.next();

      if (element.beginColumn != otElement.beginColumn) return false;

      if (element.beginLine != otElement.beginLine) return false;

      if (!element.getClass().equals(otElement.getClass())) return false;

      String rep1 = NodeUtils.getFullRepresentationString(element);
      String rep2 = NodeUtils.getFullRepresentationString(otElement);
      if (rep1 == null || rep2 == null) {
	 if (rep1 != rep2) {
	    return false;
	 }

      }
      else if (!rep1.equals(rep2)) return false;

   }
   return true;
}

public AbstractToken[] getAllLocalTokens()
{
   return getLocalTokens(Integer.MAX_VALUE, Integer.MAX_VALUE, false);
}


public AbstractToken[] getLocalTokens(int endLine,int col,boolean onlyArgs)
{
   Set<SourceToken> comps = new HashSet<SourceToken>();

   for (Iterator<SimpleNode> iter = source_scope.iterator(); iter.hasNext();) {
      SimpleNode element = iter.next();

      stmtType[] body = null;
      if (element instanceof FunctionDef) {
	 FunctionDef f = (FunctionDef) element;
	 final argumentsType args = f.args;

	 for (int i = 0; i < args.args.length; i++) {
	    String s = NodeUtils.getRepresentationString(args.args[i]);
	    comps.add(new SourceToken(args.args[i],s,"","","",TokenType.PARAM));
	 }
	 if (args.vararg != null) {
	    String s = NodeUtils.getRepresentationString(args.vararg);
	    comps.add(new SourceToken(args.vararg,s,"","","",TokenType.PARAM));
	 }

	 if (args.kwarg != null) {
	    String s = NodeUtils.getRepresentationString(args.kwarg);
	    comps.add(new SourceToken(args.kwarg,s,"","","",TokenType.PARAM));
	 }
	 if (args.kwonlyargs != null) {
	    for (int i = 0; i < args.kwonlyargs.length; i++) {
	       String s = NodeUtils.getRepresentationString(args.kwonlyargs[i]);
	       comps.add(new SourceToken(args.kwonlyargs[i],s,"","","",TokenType.PARAM));
	    }
	 }

	 if (onlyArgs) {
	    continue;
	 }
	 body = f.body;
      }

      else if (element instanceof ClassDef && !iter.hasNext()) {
	 ClassDef classDef = (ClassDef) element;
	 body = classDef.body;
      }

      if (body != null) {
	 try {
	    for (int i = 0; i < body.length; i++) {
	       GlobalModelVisitor visitor = new GlobalModelVisitor(
			VisitorType.GLOBAL_TOKENS,"",false,true);
	       stmtType stmt = body[i];
	       if (stmt == null) {
		  continue;
	       }
	       stmt.accept(visitor);
	       List<AbstractToken> t = visitor.token_set;
	       for (Iterator<AbstractToken> iterator = t.iterator(); iterator.hasNext();) {
		  SourceToken tok = (SourceToken) iterator.next();

		  // if it is found here, it is a local type
		  tok.token_type = TokenType.LOCAL;
		  if (tok.getAst().beginLine <= endLine) {
		     comps.add(tok);
		  }

	       }
	    }
	 }
	 catch (Exception e) {
	    PybaseMain.logE("Problem with tokens in local scope",e);
	 }
      }
   }


   return comps.toArray(new SourceToken[0]);
}

/**
 *
 * @param argName this is the argument (cannot have dots)
 * @param activationToken this is the actual activation token we're looking for
 * (may have dots).
 *
 * Note that argName == activationToken first part before the dot (they may be equal)
 * @return a list of tokens for the local
 */
public Collection<AbstractToken> getInterfaceForLocal(String activationToken)
{
   return getInterfaceForLocal(activationToken, true, true);
}

public Collection<AbstractToken> getInterfaceForLocal(String activationToken,
	 boolean addAttributeAccess,boolean addLocalsFromHasAttr)
{
   Set<SourceToken> comps = new HashSet<SourceToken>();

   Iterator<SimpleNode> it = source_scope.topDownIterator();
   if (!it.hasNext()) {
      return new ArrayList<AbstractToken>();
   }

   SimpleNode element = it.next();

   String dottedActTok = activationToken + '.';
   // ok, that's the scope we have to analyze
   SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(element);

   ArrayList<Class<?>> classes = new ArrayList<Class<?>>(2);
   if (addAttributeAccess) {
      classes.add(Attribute.class);

   }
   if (addLocalsFromHasAttr) {
      classes.add(Call.class);
   }
   Iterator<ASTEntry> iterator = visitor.getIterator(classes.toArray(new Class[classes
	    .size()]));

   while (iterator.hasNext()) {
      ASTEntry entry = iterator.next();
      if (entry.node instanceof Attribute) {
	 String rep = NodeUtils.getFullRepresentationString(entry.node);
	 if (rep.startsWith(dottedActTok)) {
	    rep = rep.substring(dottedActTok.length());
	    if (NodeUtils.isValidNameRepresentation(rep)) { // that'd be something that
// can happen when trying to recreate the parsing
	       comps.add(new SourceToken(entry.node,FullRepIterable.getFirstPart(rep),"",
			"","",TokenType.OBJECT_FOUND_INTERFACE));
	    }
	 }
      }
      else if (entry.node instanceof Call) {
	 Call call = (Call) entry.node;
	 if ("hasattr".equals(NodeUtils.getFullRepresentationString(call.func))
		  && call.args != null && call.args.length == 2) {
	    String rep = NodeUtils.getFullRepresentationString(call.args[0]);
	    if (rep.equals(activationToken)) {
	       exprType node = call.args[1];
	       if (node instanceof Str) {
		  Str str = (Str) node;
		  String attrName = str.s;
		  if (NodeUtils.isValidNameRepresentation(attrName)) {
		     comps.add(new SourceToken(node,attrName,"","","",
			      TokenType.OBJECT_FOUND_INTERFACE));
		  }
	       }
	    }
	 }

      }
   }
   return new ArrayList<AbstractToken>(comps);
}


public List<AbstractToken> getLocalImportedModules(int line,int col,String moduleName)
{
   ArrayList<AbstractToken> importedModules = new ArrayList<AbstractToken>();
   for (Iterator<SimpleNode> iter = source_scope.iterator(); iter.hasNext();) {
      SimpleNode element = iter.next();

      if (element instanceof FunctionDef) {
	 FunctionDef f = (FunctionDef) element;
	 for (int i = 0; i < f.body.length; i++) {
	    stmtType stmt = f.body[i];
	    if (stmt != null) {
	       importedModules.addAll(AbstractVisitor.getTokens(stmt,
		     VisitorType.ALIAS_MODULES, moduleName, null, false));
	    }
	 }
      }
   }
   return importedModules;
}

public ClassDef getClassDef()
{
   for (Iterator<SimpleNode> it = source_scope.topDownIterator(); it.hasNext();) {
      SimpleNode node = it.next();
      if (node instanceof ClassDef) {
	 return (ClassDef) node;
      }
   }
   return null;
}

public boolean isLastClassDef()
{
   if (source_scope.size() > 0 && source_scope.peek() instanceof ClassDef) {
      return true;
   }
   return false;
}

public Iterator<SimpleNode> iterator()
{
   return source_scope.topDownIterator();
}

public int getIfMainLine()
{
   return ifMainLine;
}

public int getScopeEndLine()
{
   return scopeEndLine;
}

public void setIfMainLine(int original)
{
   ifMainLine = original;
}

public void setScopeEndLine(int beginLine)
{
   scopeEndLine = beginLine;
}

/**
 * Constant containing the calls that are checked for implementations.
 *
 * Couldn't find anything similar for pyprotocols.
 *
 * Zope has a different heuristic which is also checked:
 * assert Interface.implementedBy(foo)
 *
 * maps the method name to check -> index of the class in the call (or negative if class is the caller)
 *
 * TODO: This should be made public to the user...
 */
public static final Map<String, Integer> ISINSTANCE_POSSIBILITIES = new HashMap<String, Integer>();
static {
   ISINSTANCE_POSSIBILITIES.put("isinstance".toLowerCase(), 2);
   ISINSTANCE_POSSIBILITIES.put("IsImplementation".toLowerCase(), 2);
   ISINSTANCE_POSSIBILITIES.put("IsInterfaceDeclared".toLowerCase(), 2);
   ISINSTANCE_POSSIBILITIES.put("implementedBy".toLowerCase(), -1);
}

public List<String> getPossibleClassesForActivationToken(String actTok)
{
   ArrayList<String> ret = new ArrayList<String>();

   Iterator<SimpleNode> it = source_scope.topDownIterator();
   if (!it.hasNext()) {
      return ret;
   }
   SimpleNode element = it.next();

   // ok, that's the scope we have to analyze
   SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(element);
   Iterator<ASTEntry> iterator = visitor.getIterator(Assert.class);

   while (iterator.hasNext()) {
      ASTEntry entry = iterator.next();
      Assert ass = (Assert) entry.node;
      if (ass.test instanceof Call) {
	 Call call = (Call) ass.test;
	 String rep = NodeUtils.getFullRepresentationString(call.func);
	 if (rep == null) {
	    continue;
	 }
	 Integer classIndex = ISINSTANCE_POSSIBILITIES.get(FullRepIterable.getLastPart(
		  rep).toLowerCase());
	 if (classIndex != null) {
	    if (call.args != null && (call.args.length >= Math.max(classIndex, 1))) {
	       // in all cases, the instance is the 1st parameter.
	       String foundActTok = NodeUtils.getFullRepresentationString(call.args[0]);

	       if (foundActTok != null && foundActTok.equals(actTok)) {
		  if (classIndex > 0) {
		     exprType type = call.args[classIndex - 1];

		     if (type instanceof Tuple) {
			// case: isinstance(obj, (Class1,Class2))
			Tuple tuple = (Tuple) type;
			for (exprType expr : tuple.elts) {
			   addRepresentationIfPossible(ret, expr);
			}
		     }
		     else {
			// case: isinstance(obj, Class)
			addRepresentationIfPossible(ret, type);
		     }
		  }
		  else {
		     // zope case Interface.implementedBy(obj) -> Interface added
		     ret.add(FullRepIterable.getWithoutLastPart(rep));
		  }
	       }
	    }
	 }

      }
   }
   return ret;
}


/**
 * @param ret the list where the representation should be added
 * @param expr the Name or Attribute that determines the class that should be added
 */
private void addRepresentationIfPossible(ArrayList<String> ret,exprType expr)
{
   if (expr instanceof Name || expr instanceof Attribute) {
      String string = NodeUtils.getFullRepresentationString(expr);
      if (string != null) {
	 ret.add(string);
      }
   }
}


} // end of class LocalScope


/* end of LocalScope.java */
