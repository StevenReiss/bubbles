/********************************************************************************/
/*										*/
/*		SourceModule.java						*/
/*										*/
/*	Python Bubbles Base representation of a module with source code 	*/
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
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseMessage;
import edu.brown.cs.bubbles.pybase.PybaseNature;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.cache.Cache;
import org.python.pydev.core.cache.LRUCache;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;


/**
 * The module should have all the information we need for code completion, find definition, and refactoring on a module.
 *
 * Note: A module may be represented by a folder if it has an __init__.py file that represents the module or a python file.
 *
 * Any of those must be a valid python token to be recognized (from the PYTHONPATH).
 *
 * We don't reuse the ModelUtils already created as we still have to transport a lot of logic to it to make it workable, so, the attempt
 * here is to use a thin tier.
 *
 * NOTE: When using it, don't forget to use the superclass abstraction.
 *
 * @author Fabio Zadrozny
 */
public class SourceModule extends AbstractModule {

private static final AbstractToken[] EMPTY_ITOKEN_ARRAY 	  = new AbstractToken[0];

private static final boolean  DEBUG_INTERNAL_GLOBALS_CACHE = false;

public static boolean	 TESTING		      = false;

/**
 * This is the abstract syntax tree based on the jython parser output.
 */
private SimpleNode	    source_ast;

/**
 * File that originated the syntax tree.
 */
private File		  source_file;

/**
 * Is bootstrap?
 */
private Boolean        is_bootstrap;

/**
 * Path for this module within the zip file (only used if file is actually a file... otherwise it is null).
 */
public String		 zip_file_path;


/**
 * This is a parse error that was found when parsing the code that generated this module
 */
public final List<PybaseMessage>	parse_error;


@Override public String getZipFilePath()
{
   return zip_file_path;
}

/**
 * This is the time when the file was last modified.
 */
private long				      last_modified;

/**
 * The object may be a SourceToken or a List<SourceToken>
 */
private HashMap<VisitorType,TreeMap<String,Object>> tokens_cache = new HashMap<VisitorType,TreeMap<String,Object>>();

/**
 * Set when the visiting is done (can hold some metadata, such as __all__ token assign)
 */
private GlobalModelVisitor			global_model_visitor_cache = null;

/**
 *
 * @return the visitor that was used to generate the internal tokens for this module (if any).
 *
 * May be null
 */
public GlobalModelVisitor getGlobalModelVisitorCache()
{
   return global_model_visitor_cache;
}

/**
 * @return a reference to all the modules that are imported from this one in the global context as a from xxx import *
 *
 * This modules are treated specially, as we don't care which tokens were imported. When this is requested, the module is prompted for
 * its tokens.
 */
@Override public AbstractToken[] getWildImportedModules()
{
   return getTokens(VisitorType.WILD_MODULES, null, null);
}

/**
 * Searches for the following import tokens:
 *	 import xxx
 *	 import xxx as ...
 *	 from xxx import xxx
 *	 from xxx import xxx as ....
 * Note, that imports with wildcards are not collected.
 * @return an array of references to the modules that are imported from this one in the global context.
 */
@Override public AbstractToken[] getTokenImportedModules()
{
   return getTokens(VisitorType.ALIAS_MODULES, null, null);
}

/**
 *
 * @return the file this module corresponds to.
 */
@Override public File getFile()
{
   return this.source_file;
}

/**
 * @return the tokens that are present in the global scope.
 *
 * The tokens can be class definitions, method definitions and attributes.
 */
@Override public AbstractToken[] getGlobalTokens()
{
   return getTokens(VisitorType.GLOBAL_TOKENS, null, null);
}

/**
 * @return a string representing the module docstring.
 */
@Override public String getDocString()
{
   AbstractToken[] l = getTokens(VisitorType.MODULE_DOCSTRING, null, null);
   if (l.length > 0) {
      SimpleNode a = ((SourceToken) l[0]).getAst();

      return ((Str) a).s;
   }
   return "";
}


/**
 * Checks if it is in the global tokens that were created in this module
 * @param tok the token we are looking for
 * @param the_nature the nature
 * @return true if it was found and false otherwise
 */
@Override public boolean isInDirectGlobalTokens(String tok,ICompletionCache completionCache)
{
   TreeMap<String, Object> tokens = tokens_cache.get(VisitorType.GLOBAL_TOKENS);
   if (tokens == null) {
      getGlobalTokens();
   }

   boolean ret = false;
   if (tokens != null) {
      synchronized (tokens) {
	 ret = tokens.containsKey(tok);
      }
   }

   if (ret == false) {
      ret = isInDirectImportTokens(tok);
   }
   return ret;
}

public boolean isInDirectImportTokens(String tok)
{
   TreeMap<String, Object> tokens = tokens_cache.get(VisitorType.ALIAS_MODULES);
   if (tokens != null) {
      getTokenImportedModules();
   }

   boolean ret = false;
   if (tokens != null) {
      synchronized (tokens) {
	 ret = tokens.containsKey(tok);
      }
   }
   return ret;
}


/**
 * @param lookOnlyForNameStartingWith: if not null, well only get from the cache tokens starting with the given representation
 * @return a list of AbstractToken
 */
@SuppressWarnings("unchecked") private synchronized AbstractToken[] getTokens(VisitorType which,
	 CompletionState state,String lookOnlyForNameStartingWith)
{
   if (which.match(VisitorType.INNER_DEFS)) {
      throw new RuntimeException("Cannot do this one with caches");
   }
   // cache
   TreeMap<String, Object> tokens = tokens_cache.get(which);

   if (tokens != null) {
      return createArrayFromCacheValues(tokens, lookOnlyForNameStartingWith);
   }
   // end cache


   try {
      tokens_cache.put(VisitorType.ALIAS_MODULES, new TreeMap<String, Object>());
      tokens_cache.put(VisitorType.GLOBAL_TOKENS, new TreeMap<String, Object>());
      tokens_cache.put(VisitorType.WILD_MODULES, new TreeMap<String, Object>());
      tokens_cache.put(VisitorType.MODULE_DOCSTRING, new TreeMap<String, Object>());

      // we request all and put it into the cache (partitioned), because that's faster
      // than making multiple runs through it
      GlobalModelVisitor globalModelVisitor = AbstractVisitor
	       .getGlobalModuleVisitorWithTokens(source_ast, VisitorType.ALL, module_name, state,
			false);

      this.global_model_visitor_cache = globalModelVisitor;

      List<AbstractToken> ret = globalModelVisitor.getTokens();


      if (DEBUG_INTERNAL_GLOBALS_CACHE) {
	 System.out.println("\n\nLooking for:" + which);
      }
      // cache
      for (AbstractToken token : ret) {
	 VisitorType choice;
	 if (token.isWildImport()) {
	    choice = VisitorType.WILD_MODULES;
	 }
	 else if (token.isImportFrom() || token.isImport()) {
	    choice = VisitorType.ALIAS_MODULES;
	 }
	 else if (token.isString()) {
	    choice = VisitorType.MODULE_DOCSTRING;
	 }
	 else {
	    choice = VisitorType.GLOBAL_TOKENS;
	 }
	 String rep = token.getRepresentation();
	 if (DEBUG_INTERNAL_GLOBALS_CACHE) {
	    System.out.println("Adding choice:" + choice + " name:" + rep);
	    if (choice != which) {
	       System.out.println("Looking for:" + which + "found:" + choice);
	       System.out.println("here");
	    }
	 }
	 TreeMap<String, Object> treeMap = tokens_cache.get(choice);
	 SourceToken newSourceToken = (SourceToken) token;
	 Object current = treeMap.get(rep);
	 if (current == null) {
	    treeMap.put(rep, newSourceToken);
	 }
	 else {
	    // the new references (later in the module) are always added to the head of the position...
	    if (current instanceof List) {
	       ((List<SourceToken>) current).add(0, newSourceToken);

	    }
	    else if (current instanceof SourceToken) {
	       ArrayList<SourceToken> lst = new ArrayList<SourceToken>();
	       lst.add(newSourceToken);
	       lst.add((SourceToken) current);
	       treeMap.put(rep, lst);

	    }
	    else {
	       throw new RuntimeException("Unexpected class in cache:" + current);

	    }
	 }
      }
      // end cache

   }
   catch (Exception e) {
      PybaseMain.logE("Problem getting tokens",e);
   }

   // now, let's get it from the cache... (which should be filled by now)
   tokens = tokens_cache.get(which);
   return createArrayFromCacheValues(tokens, lookOnlyForNameStartingWith);
}

@SuppressWarnings("unchecked") private AbstractToken[] createArrayFromCacheValues(
	 TreeMap<String, Object> tokens,String lookOnlyForNameStartingWith)
{
   List<SourceToken> ret = new ArrayList<SourceToken>();

   Collection<Object> lookIn;
   if (lookOnlyForNameStartingWith == null) {
      lookIn = tokens.values();
   }
   else {
      lookIn = tokens.subMap(lookOnlyForNameStartingWith,
	       lookOnlyForNameStartingWith + "z").values();
   }


   for (Object o : lookIn) {
      if (o instanceof SourceToken) {
	 ret.add((SourceToken) o);
      }
      else if (o instanceof List) {
	 ret.addAll((List<SourceToken>) o);
      }
      else {
	 throw new RuntimeException("Unexpected class in cache:" + o);
      }
   }
   return ret.toArray(new SourceToken[ret.size()]);
}

/**
 *
 * @param name
 * @param f
 * @param n
 */
public SourceModule(String name,File f,SimpleNode n,List<PybaseMessage> parseError)
{
   super(name);
   this.source_ast = n;
   this.source_file = f;
   this.parse_error = parseError;
   if (f != null) {
      this.last_modified = f.lastModified();
   }
}


@Override public AbstractToken[] getGlobalTokens(CompletionState initialState,
	 AbstractASTManager manager)
{
   String activationToken = initialState.getActivationToken();
   final int activationTokenLen = activationToken.length();
   final List<String> actToks = StringUtils.dotSplit(activationToken);
   final int actToksLen = actToks.size();

   String goFor = null;
   if (actToksLen > 0) {
      goFor = actToks.get(0);
   }
   AbstractToken[] t = getTokens(VisitorType.GLOBAL_TOKENS, null, goFor);

   for (int i = 0; i < t.length; i++) {
      SourceToken token = (SourceToken) t[i];
      String rep = token.getRepresentation();

      SimpleNode ast = token.getAst();

      if (activationTokenLen > rep.length() && activationToken.startsWith(rep)) {
	 // we need this thing to work correctly for nested modules...
	 // some tests are available at:
// PythonCompletionTestWithoutBuiltins.testDeepNestedXXX

	 int iActTok = 0;
	 if (actToks.get(iActTok).equals(rep)) {
	    // System.out.println("Now we have to find act..."+activationToken+"(which is a definition of:"+rep+")");
	    try {
	       Definition[] definitions;
	       String value = activationToken;
	       String initialValue = null;
	       while (true) {
		  if (value.equals(initialValue)) {
		     break;
		  }
		  initialValue = value;
		  if (iActTok > actToksLen) {
		     break; // unable to find it
		  }

		  // If we have C1.f.x
		  // At this point we'll find the C1 definition...

		  definitions = findDefinition(initialState.getCopyWithActTok(value),
			   token.getLineDefinition(), token.getColDefinition() + 1,
			   manager.getNature());
		  if (definitions.length == 1) {
		     Definition d = definitions[0];
		     if (d.getAssignment() instanceof Assign) {
			Assign assign = (Assign) d.getAssignment();
			value = NodeUtils.getRepresentationString(assign.value);
			definitions = findDefinition(
				 initialState.getCopyWithActTok(value),d.getLine(),
				 d.getCol(),manager.getNature());
		     }
		     else if (d.getAssignment() instanceof ClassDef) {
			AbstractToken[] toks = ((SourceModule) d.getModule()).getClassToks(
				 initialState, manager,d.getAssignment()).toArray(
				 EMPTY_ITOKEN_ARRAY);
			if (iActTok == actToksLen - 1) {
			   return toks;
			}
			value = d.getValueName();

		     }
		     else if (d.getAssignment() instanceof Name) {
			ClassDef classDef = d.getScope().getClassDef();
			if (classDef != null) {
			   FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(
				    actToks.get(actToksLen - 1),d.getLine(),
				    d.getCol(),d.getModule());
			   try {
			      classDef.accept(visitor);
			   }
			   catch (StopVisitingException e) {
			      // expected exception
			   }
			   if (visitor.definition_set.size() == 0) {
			      return EMPTY_ITOKEN_ARRAY;
			   }
			   d = visitor.definition_set.get(0);
			   value = d.getValueName();
			   if (d instanceof AssignDefinition) {
			      // Yes, at this point we really are looking for an assign!
			      // E.g.:
			      //
			      // import my.module
			      //
			      // class A:
			      // objects = my.module.Class()
			      //
			      // This happens when completing on A.objects.
			      initialState.setLookingFor(
				       LookForType.ASSIGN, true);
			      return getValueCompletions(initialState, manager, value,
				       d.getModule());
			   }
			}
			else {
			   if (d.getModule() instanceof SourceModule) {
			      SourceModule m = (SourceModule) d.getModule();
			      String joined = FullRepIterable.joinFirstParts(actToks);
			      Definition[] definitions2 = m.findDefinition(
				       initialState.getCopyWithActTok(joined),
				       d.getLine(),d.getCol(), manager.getNature());
			      if (definitions2.length == 0) {
				 return EMPTY_ITOKEN_ARRAY;
			      }
			      d = definitions2[0];
			      value = d.getValueName() + "." + actToks.get(actToksLen - 1);
			      if (d instanceof AssignDefinition) {
				 return ((SourceModule) d.getModule())
					  .getValueCompletions(initialState, manager,
						   value, d.getModule());
			      }
			   }
			}

		     }
		     else if ((d.getAssignment() == null && d.getModule() != null)
			      || d.getAssignment() instanceof ImportFrom) {
			return getValueCompletions(initialState, manager, value,
				 d.getModule());

		     }
		     else {
			break;
		     }
		  }
		  else {
		     return getValueCompletions(initialState, manager, value, this);
		  }
		  iActTok++;
	       }
	    }
	    catch (CompletionRecursionException e) {}
	    catch (Exception e) {
	       PybaseMain.logE("Problem getting global tokens",e);
	    }
	 }
      }
      else if (rep.equals(activationToken)) {
	 if (ast instanceof ClassDef) {
	    initialState.setLookingFor(LookForType.UNBOUND_VARIABLE);
	 }
	 List<AbstractToken> classToks = getClassToks(initialState, manager, ast);
	 if (classToks.size() == 0) {
	    if (initialState.getLookingFor() == LookForType.ASSIGN) {
	       continue;
	    }
	    // otherwise, return it empty anyway...
	    return EMPTY_ITOKEN_ARRAY;
	 }
	 return classToks.toArray(EMPTY_ITOKEN_ARRAY);
      }
   }
   return EMPTY_ITOKEN_ARRAY;
}

/**
 * @param initialState
 * @param manager
 * @param value
 * @return
 * @throws CompletionRecursionException
 */
private AbstractToken[] getValueCompletions(CompletionState initialState,
	 AbstractASTManager manager,String value,AbstractModule module)
	 throws CompletionRecursionException
{
   initialState.checkFindMemory(this, value);
   CompletionState copy = initialState.getCopy();
   copy.setActivationToken(value);
   AbstractToken[] completionsForModule = manager.getCompletionsForModule(module, copy);
   return completionsForModule;
}

/**
 * @param initialState
 * @param manager
 * @param ast
 * @return
 */
public List<AbstractToken> getClassToks(CompletionState initialState,
	 AbstractASTManager manager,SimpleNode ast)
{
   List<AbstractToken> modToks = AbstractVisitor.getTokens(ast,
	    VisitorType.INNER_DEFS, module_name, initialState, false);// name =
// moduleName

   try {
      // COMPLETION: get the completions for the whole hierarchy if this is a class!!
      CompletionState state;
      if (ast instanceof ClassDef) {
	 ClassDef c = (ClassDef) ast;
	 for (int j = 0; j < c.bases.length; j++) {
	    if (c.bases[j] instanceof Name) {
	       Name n = (Name) c.bases[j];
	       String base = n.id;
	       // An error in the programming might result in an error.
	       //
	       // e.g. The case below results in a loop.
	       //
	       // class A(B):
	       //
	       // def a(self):
	       // pass
	       //
	       // class B(A):
	       //
	       // def b(self):
	       // pass
	       state = initialState.getCopy();
	       state.setActivationToken(base);

	       state.checkMemory(this, base);

	       final AbstractToken[] comps = manager.getCompletionsForModule(this, state);
	       modToks.addAll(Arrays.asList(comps));
	    }
	    else if (c.bases[j] instanceof Attribute) {
	       Attribute attr = (Attribute) c.bases[j];
	       String s = NodeUtils.getFullRepresentationString(attr);

	       state = initialState.getCopy();
	       state.setActivationToken(s);
	       final AbstractToken[] comps = manager.getCompletionsForModule(this, state);
	       modToks.addAll(Arrays.asList(comps));
	    }
	 }

      }
   }
   catch (CompletionRecursionException e) {
      // let's return what we have so far...
   }
   return modToks;
}


/**
 * Caches to hold scope visitors.
 */
private Cache<Object, FindScopeVisitor>    scope_visitor_cache	   = new LRUCache<Object, FindScopeVisitor>(
											 10);
private Cache<Object, FindDefinitionModelVisitor> find_definition_visitor_cache = new LRUCache<Object, FindDefinitionModelVisitor>(
											 10);

/**
 * @return a scope visitor that has already passed through the visiting step for the given line/col.
 *
 * @note we don't have to worry about the ast, as it won't change after we create the source module with it.
 */
private FindScopeVisitor getScopeVisitor(int line,int col) throws Exception
{
   Tuple<?, ?> key = new Tuple<Integer, Integer>(line,col);
   FindScopeVisitor scopeVisitor = this.scope_visitor_cache.getObj(key);
   if (scopeVisitor == null) {
      scopeVisitor = new FindScopeVisitor(line,col);
      if (source_ast != null) {
	 source_ast.accept(scopeVisitor);
      }
      this.scope_visitor_cache.add(key, scopeVisitor);
   }
   return scopeVisitor;
}

/**
 * @return a find definition scope visitor that has already found some definition
 */
private FindDefinitionModelVisitor getFindDefinitionsScopeVisitor(String rep,int line,
	 int col) throws Exception
{
   Tuple3<?, ?, ?> key = new Tuple3<String, Integer, Integer>(rep,line,col);
   FindDefinitionModelVisitor visitor = this.find_definition_visitor_cache.getObj(key);
   if (visitor == null) {
      visitor = new FindDefinitionModelVisitor(rep,line,col,this);
      if (source_ast != null) {
	 try {
	    source_ast.accept(visitor);
	 }
	 catch (StopVisitingException e) {
	    // expected exception
	 }
      }
      this.find_definition_visitor_cache.add(key, visitor);
   }
   return visitor;
}

/**
 * @param line: starts at 1
 * @param col: starts at 1
 */
@Override public Definition[] findDefinition(CompletionState state,int line,int col,
	 final PybaseNature nature) throws Exception
{
   String rep = state.getActivationToken();
   // the line passed in starts at 1 and the lines for the visitor start at 0
   ArrayList<Definition> toRet = new ArrayList<Definition>();

   // first thing is finding its scope
   FindScopeVisitor scopeVisitor = getScopeVisitor(line, col);

   // this visitor checks for assigns for the token
   FindDefinitionModelVisitor visitor = getFindDefinitionsScopeVisitor(rep, line, col);

   if (visitor.definition_set.size() > 0) {
      // ok, it is an assign, so, let's get it

      for (Iterator<?> iter = visitor.definition_set.iterator(); iter.hasNext();) {
	 Object next = iter.next();
	 if (next instanceof AssignDefinition) {
	    AssignDefinition element = (AssignDefinition) next;
	    if (element.target_name.startsWith("self") == false) {
	       if (element.getScope().isOuterOrSameScope(scopeVisitor.getScope())
			|| element.found_as_global) {
		  toRet.add(element);
	       }
	    }
	    else {
	       toRet.add(element);
	    }
	 }
	 else {
	    toRet.add((Definition) next);
	 }
      }
      if (toRet.size() > 0) {
	 return toRet.toArray(new Definition[0]);
      }
   }


   // now, check for locals
   AbstractToken[] localTokens = scopeVisitor.getScope().getAllLocalTokens();
   for (AbstractToken tok : localTokens) {
      String tokenRep = tok.getRepresentation();
      if (tokenRep.equals(rep)) {
	 return new Definition[] { new Definition(tok,scopeVisitor.getScope(),this,true) };
      }
      else if (rep.startsWith(tokenRep + ".") && !rep.startsWith("self.")) {
	 // this means we have a declaration in the local scope and we're accessing a part
// of it
	 // e.g.:
	 // class B:
	 // def met2(self):
	 // c = C()
	 // c.met1
	 state.checkFindLocalDefinedDefinitionMemory(this, tokenRep);
	 CompletionState copyWithActTok = state.getCopyWithActTok(tokenRep);

	 Definition[] definitions = this.findDefinition(copyWithActTok,
		  tok.getLineDefinition(), tok.getColDefinition(), nature);
	 ArrayList<Definition> ret = new ArrayList<Definition>();
	 for (Definition definition : definitions) {
	    if (definition.getModule() != null) {
	       String checkFor = definition.getValueName() + rep.substring(tokenRep.length());
	       if (checkFor.equals(rep) && definition.getModule().equals(this)) {
		  // no point in finding the starting point
		  continue;
	       }
	       // Note: I couldn't really reproduce this case, so, this fix is just a theoretical
	       // workaround. Hopefully sometime someone will provide some code to reproduce this.
	 // see: http://sourceforge.net/tracker/?func=detail&aid=2992629&group_id=85796&atid=577329
	       if (StringUtils.count(checkFor, '.') > 30) {
		  throw new CompletionRecursionException(
			   "Trying to go to deep to find definition.\n"
				    + "We probably started entering a recursion.\n"
				    + "Module: " + definition.getModule().getName() + "\n"
				    + "Token: " + checkFor);
	       }

	       Definition[] realDefinitions = definition.getModule()
			.findDefinition(state.getCopyWithActTok(checkFor),
				 definition.getLine(), definition.getCol(), nature);
	       for (Definition realDefinition : realDefinitions) {
		  ret.add(realDefinition);
	       }
	    }
	 }
	 return ret.toArray(new Definition[ret.size()]);
      }
   }

   // not found... check as local imports
   List<AbstractToken> localImportedModules = scopeVisitor.getScope().getLocalImportedModules(
	    line, col, this.module_name);
   AbstractASTManager astManager = nature.getAstManager();
   for (AbstractToken tok : localImportedModules) {
      String importRep = tok.getRepresentation();
      if (importRep.equals(rep) || rep.startsWith(importRep + ".")) {
	 Tuple3<AbstractModule, String, AbstractToken> o = astManager.findOnImportedMods(
		  new AbstractToken[] { tok }, state.getCopyWithActTok(rep), this.getName());
	 if (o != null && o.o1 instanceof SourceModule) {
	    CompletionState copy = state.getCopy();
	    copy.setActivationToken(o.o2);

	    findDefinitionsFromModAndTok(nature, toRet, null, (SourceModule) o.o1, copy);
	 }
	 if (toRet.size() > 0) {
	    return toRet.toArray(new Definition[0]);
	 }
      }
   }


   // ok, not assign nor import, let's check if it is some self (we do not check for only
// 'self' because that would map to a
   // local (which has already been covered).
   if (rep.startsWith("self.")) {
      // ok, it is some self, now, that is only valid if we are in some class definition
      ClassDef classDef = scopeVisitor.getScope().getClassDef();
      if (classDef != null) {
	 // ok, we are in a class, so, let's get the self completions
	 String classRep = NodeUtils.getRepresentationString(classDef);
	 AbstractToken[] globalTokens = getGlobalTokens(new CompletionState(line - 1,col - 1,
		  classRep,nature,"",state), // use the old state as the cache
		  astManager);

	 String withoutSelf = rep.substring(5);
	 for (AbstractToken token : globalTokens) {
	    if (token.getRepresentation().equals(withoutSelf)) {
	       String parentPackage = token.getParentPackage();
	       AbstractModule module = astManager.getModule(parentPackage, nature, true);

	       if (token instanceof SourceToken
			&& (module != null || this.module_name.equals(parentPackage))) {
		  if (module == null) {
		     module = this;
		  }

		  SimpleNode ast2 = ((SourceToken) token).getAst();
		  Tuple<Integer, Integer> def = getLineColForDefinition(ast2);
		  FastStack<SimpleNode> stack = new FastStack<SimpleNode>();
		  if (module instanceof SourceModule) {
		     stack.push(((SourceModule) module).getAst());
		  }
		  stack.push(classDef);
		  LocalScope scope = new LocalScope(stack);
		  return new Definition[] { new Definition(def.o1,def.o2,
			   token.getRepresentation(),ast2,scope,module) };

	       }
	       else {
		  return new Definition[0];
	       }
	    }
	 }
      }
   }


   // ok, it is not an assign, so, let's search the global tokens (and imports)
   String tok = rep;
   SourceModule mod = this;

   Tuple3<AbstractModule, String, AbstractToken> o = astManager.findOnImportedMods(
	    state.getCopyWithActTok(rep), this);

   if (o != null) {
      if (o.o1 instanceof SourceModule) {
	 mod = (SourceModule) o.o1;
	 tok = o.o2;

      }
      else if (o.o1 instanceof CompiledModule) {
	 // ok, we have to check the compiled module
	 tok = o.o2;
	 if (tok == null || tok.length() == 0) {
	    return new Definition[] { new Definition(1,1,"",null,null,o.o1) };
	 }
	 else {
	    state.checkFindDefinitionMemory(o.o1, tok);
	    return o.o1.findDefinition(state.getCopyWithActTok(tok), -1, -1, nature);
	 }

      }
      else {
	 throw new RuntimeException("Unexpected module found in imports: " + o);
      }
   }

   // mod == this if we are now checking the globals (or maybe not)...heheheh
   CompletionState copy = state.getCopy();
   copy.setActivationToken(tok);
   try {
      state.checkFindDefinitionMemory(mod, tok);
      findDefinitionsFromModAndTok(nature, toRet, visitor.module_imported, mod, copy);
   }
   catch (CompletionRecursionException e) {
      // ignore (will return what we've got so far)
// e.printStackTrace();
   }

   return toRet.toArray(new Definition[0]);
}

/**
 * Finds the definitions for some module and a token from that module
 * @throws Exception
 */
private void findDefinitionsFromModAndTok(PybaseNature nature,
	 ArrayList<Definition> toRet,String moduleImported,SourceModule mod,
	 CompletionState state) throws Exception
{
   String tok = state.getActivationToken();
   if (tok != null) {
      if (tok.length() > 0) {
	 Definition d = mod.findGlobalTokDef(state.getCopyWithActTok(tok), nature);
	 if (d != null) {
	    toRet.add(d);

	 }
	 else if (moduleImported != null) {
	    // if it was found as some import (and is already stored as a dotted name), we
// must check for
	    // multiple representations in the absolute form:
	    // as a relative import
	    // as absolute import
	    getModuleDefinition(nature, toRet, mod, moduleImported);
	 }

      }
      else {
	 // we found it, but it is an empty tok (which means that what we found is the
// actual module).
	 toRet.add(new Definition(1,1,"",null,null,mod));
      }
   }
}

private Definition getModuleDefinition(PybaseNature nature,ArrayList<Definition> toRet,
	 SourceModule mod,String moduleImported)
{
   String rel = AbstractToken.makeRelative(mod.getName(), moduleImported);
   AbstractModule modFound = nature.getAstManager().getModule(rel, nature, false);
   if (modFound == null) {
      modFound = nature.getAstManager().getModule(moduleImported, nature, false);
   }
   if (modFound != null) {
      // ok, found it
      Definition definition = new Definition(1,1,"",null,null,modFound);
      if (toRet != null) {
	 toRet.add(definition);
      }
      return definition;
   }
   return null;
}


/**
 * @param tok
 * @param nature
 * @return
 * @throws Exception
 */
public Definition findGlobalTokDef(CompletionState state,PybaseNature nature)
	 throws Exception
{
   String tok = state.getActivationToken();
   String[] headAndTail = FullRepIterable.headAndTail(tok);
   String firstPart = headAndTail[0];
   String rep = headAndTail[1];

   AbstractToken[] tokens = null;
   if (nature != null) {
      tokens = nature.getAstManager().getCompletionsForModule(this,
	       state.getCopyWithActTok(firstPart), true);
   }
   else {
      tokens = getGlobalTokens();
   }
   for (AbstractToken token : tokens) {
      boolean sameRep = token.getRepresentation().equals(rep);
      if (sameRep) {
	 if (token instanceof SourceToken) {
	    if (((SourceToken) token).getType() == TokenType.OBJECT_FOUND_INTERFACE) {
	       // just having it extracted from the interface from an object does not mean
	       // that it's actual definition was found
	       continue;
	    }
	    // ok, we found it
	    SimpleNode a = ((SourceToken) token).getAst();
	    Tuple<Integer, Integer> def = getLineColForDefinition(a);

	    String parentPackage = token.getParentPackage();
	    AbstractModule module = this;
	    if (nature != null) {
	       AbstractModule mod = nature.getAstManager()
			.getModule(parentPackage, nature, true);
	       if (mod != null) {
		  module = mod;
	       }
	    }


	    if (module instanceof SourceModule) {
	       // this is just to get its scope...
	       SourceModule m = (SourceModule) module;
	       FindScopeVisitor scopeVisitor = m.getScopeVisitor(a.beginLine,
			a.beginColumn);
	       return new Definition(def.o1,def.o2,rep,a,scopeVisitor.getScope(),module);
	    }
	    else {
	       // line, col
	       return new Definition(def.o1,def.o2,rep,a,new LocalScope(
			new FastStack<SimpleNode>()),module);
	    }
	 }
	 else if (token instanceof ConcreteToken && nature != null) {
	    // a contrete token represents a module
	    String modName = token.getParentPackage();
	    if (modName.length() > 0) {
	       modName += ".";
	    }
	    modName += token.getRepresentation();
	    AbstractModule module = nature.getAstManager().getModule(modName, nature, true);
	    if (module == null) {
	       return null;
	    }
	    else {
	       return new Definition(0 + 1,0 + 1,"",null,null,module); // it is the module
// itself
	    }

	 }
	 else if (token instanceof CompiledToken && nature != null) {
	    String parentPackage = token.getParentPackage();
	    FullRepIterable iterable = new FullRepIterable(parentPackage,true);

	    AbstractModule module = null;
	    for (String modName : iterable) {
	       module = nature.getAstManager().getModule(modName, nature, true);
	       if (module != null) {
		  break;
	       }
	    }
	    if (module == null) {
	       return null;
	    }

	    int length = module.getName().length();
	    String finalRep = "";
	    if (parentPackage.length() > length) {
	       finalRep = parentPackage.substring(length + 1) + '.';
	    }
	    finalRep += token.getRepresentation();

	    try {
	       Definition[] definitions = module.findDefinition(
			state.getCopyWithActTok(finalRep), -1, -1, nature);
	       if (definitions.length > 0) {
		  return definitions[0];
	       }
	    }
	    catch (Exception e) {
	       throw new RuntimeException(e);
	    }
	 }
	 else {
	    throw new RuntimeException("Unexpected token:" + token.getClass());
	 }
      }
   }

   return null;
}

public Tuple<Integer, Integer> getLineColForDefinition(SimpleNode a)
{
   int line = a.beginLine;
   int col = a.beginColumn;

   if (a instanceof ClassDef) {
      ClassDef c = (ClassDef) a;
      line = c.name.beginLine;
      col = c.name.beginColumn;

   }
   else if (a instanceof FunctionDef) {
      FunctionDef c = (FunctionDef) a;
      line = c.name.beginLine;
      col = c.name.beginColumn;
   }

   return new Tuple<Integer, Integer>(line,col);
}

/**
 * @param line: at 0
 * @param col: at 0
 */
@Override public AbstractToken[] getLocalTokens(int line,int col,LocalScope scope)
{
   try {
      if (scope == null) {
	 FindScopeVisitor scopeVisitor = getScopeVisitor(line, col);
	 scope = scopeVisitor.getScope();
      }

      return scope.getLocalTokens(line, col, false);
   }
   catch (Exception e) {
      PybaseMain.logE("Problem getting local tokens",e);
      return EMPTY_ITOKEN_ARRAY;
   }
}

/**
 * @param line: at 0
 * @param col: at 0
 */
@Override public LocalScope getLocalScope(int line,int col)
{
   try {
      FindScopeVisitor scopeVisitor = getScopeVisitor(line, col);

      return scopeVisitor.getScope();
   }
   catch (Exception e) {
      PybaseMain.logE("Problem getting local scope",e);
      return null;
   }
}

/**
 * @return if the file we have is the same file in the cache.
 */
public boolean isSynched()
{
   if (this.source_file == null && TESTING) {
      return true; // when testing we can have a source module without a file
   }
   return this.source_file.lastModified() == this.last_modified;
}

public SimpleNode getAst()
{
   return source_ast;
}


/**
 * @return the line that ends a given scope (or -1 if not found)
 */
public int findAstEnd(SimpleNode node)
{
   try {
      FindScopeVisitor scopeVisitor = getScopeVisitor(node.beginLine, node.beginColumn);

      return scopeVisitor.getScope().getScopeEndLine();
   }
   catch (Exception e) {
      PybaseMain.logE("Problem getting ast end",e);
      return -1;
   }
}

/**
 * @return the main line (or -1 if not found)
 */
public int findIfMain()
{
   try {
      FindScopeVisitor scopeVisitor = getScopeVisitor(-1, -1);

      return scopeVisitor.getScope().getIfMainLine();
   }
   catch (Exception e) {
      PybaseMain.logE("Problem getting if main",e);
      return -1;
   }
}

@Override public boolean equals(Object obj)
{
   if (!(obj instanceof SourceModule)) {
      return false;
   }
   SourceModule m = (SourceModule) obj;
   if (source_file == null || m.source_file == null) {
      if (source_file != null) {
	 return false;
      }
      if (m.source_file != null) {
	 return false;
      }
      if (this.module_name == null) {
	 return this.module_name == m.module_name;
      }
      return this.module_name.equals(m.module_name);
   }

   return PybaseFileSystem.getFileAbsolutePath(source_file).equals(
	    PybaseFileSystem.getFileAbsolutePath(m.source_file))
	    && this.module_name.equals(m.module_name);
}

@Override public int hashCode()
{
   int h = 0;
   if (source_file != null) {
      String s = PybaseFileSystem.getFileAbsolutePath(source_file);
      h += s.hashCode();
   }
   if (module_name != null) h += module_name.hashCode();
   return h;
}


public void setName(String n)
{
   this.module_name = n;
}

/**
 * @return true if this is a bootstrap module (i.e.: a module that's only used to load a compiled module with the
 * same name -- that used in eggs)
 *
 * A bootstrapped module is the way that egg handles pyd files:
 * it'll create a file with the same name of the dll (e.g.:
 *
 * for having a umath.pyd, it'll create a umath.py file with the contents below
 *
 * File for boostrap
 * def __bootstrap__():
 *	  global __bootstrap__, __loader__, __file__
 *	  import sys, pkg_resources, imp
 *	  __file__ = pkg_resources.resource_filename(__name__,'umath.pyd')
 *	  del __bootstrap__, __loader__
 *	  imp.load_dynamic(__name__,__file__)
 * __bootstrap__()
 *
 */
public boolean isBootstrapModule()
{
   if (is_bootstrap == null) {
      AbstractToken[] ret = getGlobalTokens();
      if (ret != null && (ret.length == 1 || ret.length == 2 || ret.length == 3)
	     && this.source_file != null) { // also checking 2 or 3 tokens because of
	 // __file__ and __name__
	 for (AbstractToken tok : ret) {
	    if ("__bootstrap__".equals(tok.getRepresentation())) {
	       // if we get here, we already know that it defined a __bootstrap__, so,
	       // let's see if it was also called
	       SimpleNode ast = this.getAst();
	       if (ast instanceof Module) {
		  Module module = (Module) ast;
		  if (module.body != null && module.body.length > 0) {
		     ast = module.body[module.body.length - 1];
		     if (ast instanceof Expr) {
			Expr expr = (Expr) ast;
			ast = expr.value;
			if (ast instanceof Call) {
			   Call call = (Call) ast;
			   String callRep = NodeUtils.getRepresentationString(call);
			   if (callRep != null && callRep.equals("__bootstrap__")) {
			      // ok, and now , the last thing is checking if there's a dll
			      // with the same name...
			      final String modName = FullRepIterable.getLastPart(this
										    .getName());

			      File folder = source_file.getParentFile();
			      File[] validBootsrappedDlls = folder .listFiles(new FilenameFilter() {
				 @Override public boolean accept(File dir,String name) {
				    int i = name.lastIndexOf('.');
				    if (i > 0) {
				       String namePart = name.substring(0, i);
				       if (namePart.equals(modName)) {
					  String extension = name
					     .substring(i + 1);
					  if (extension.length() > 0
						 && PybaseFileSystem.isValidDllExtension(extension)) {
					     return true;
					   }
					}
				     }
				    return false;
				  }
									       });

			      if (validBootsrappedDlls.length > 0) {
				 is_bootstrap = Boolean.TRUE;
				 break;
			       }
			    }
			 }
		      }
		   }
		}
	     }
	  }
       }
      if (is_bootstrap == null) {
	 // if still not set, it's not a bootstrap.
	 is_bootstrap = Boolean.FALSE;
       }
    }

   return is_bootstrap;
}

/**
 * @return
 */
public ModulesKey getModulesKey()
{
   if (zip_file_path != null && zip_file_path.length() > 0) {
      return new ModulesKeyForZip(module_name,source_file,zip_file_path,true);
   }
   return new ModulesKey(module_name,source_file);
}

}


