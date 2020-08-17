/********************************************************************************/
/*										*/
/*		CompletionState.java						*/
/*										*/
/*	Python Bubbles Base semantic completion state representation		*/
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
 * Created on Feb 2, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants.LookForType;
import edu.brown.cs.bubbles.pybase.PybaseNature;

import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.structure.CompletionRecursionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


/**
 * @author Fabio Zadrozny
 */
public class CompletionState implements ICompletionCache {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	      activation_token;
private int		 for_line			     = -1;
private int		 for_col			      = -1;
private PybaseNature	  the_nature;
private String	      for_qualifier;

private Memo<String>	state_memory			 = new Memo<>();
private Memo<Definition>    definition_memory		    = new Memo<>();
private Memo<AbstractModule>	  wild_import_memory		  = new Memo<>();
private Memo<String>	imported_mods_called		 = new Memo<>();
private Memo<String>	find_memory			  = new Memo<>();
private Memo<String>	resolve_import_memory		= new Memo<>();
private Memo<String>	find_definition_memory	       = new Memo<>();
private Memo<String>	find_local_defined_definition_memory = new Memo<String>();
private Stack<Memo<AbstractToken>> find_resolve_import_memory	   = new Stack<>();
private Memo<String>	find_module_completions_memory	     = new Memo<>();
private Memo<String>	find_source_from_compiled_memory     = new Memo<>(1);	   // max_value is 1 for this one!

private boolean      builtins_gotten		      = false;
private boolean      local_imports_gotten		 = false;
private boolean      is_in_calltip			= false;

private LookForType		 looking_for_instance		 = LookForType.INSTANCE_UNDEFINED;
private List<AbstractToken>	  token_imported_modules;
private ICompletionCache   completion_cache;
private String	     full_activation_token;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CompletionState getCopyForResolveImportWithActTok(String actTok)
{
   CompletionState state = CompletionStateFactory.getEmptyCompletionState(actTok,the_nature,completion_cache);
   state.the_nature = the_nature;
   state.find_resolve_import_memory = find_resolve_import_memory;

   return state;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public CompletionState getCopy()
{
   return new CompletionStateWrapper(this);
}


/**
 * this is a class that can act as a memo and check if something is defined more than 'n' times
 *
 * @author Fabio Zadrozny
 */
static class Memo<E> {

   private int max_value;

   public Memo()
      {
      max_value = MAX_NUMBER_OF_OCURRENCES;
    }

   public Memo(int max)
      {
      max_value = max;
    }

   /**
      * if more than this number of ocurrences is found, we are in a recursion
   */
   private static final int		MAX_NUMBER_OF_OCURRENCES = 5;

   public Map<AbstractModule, Map<E, Integer>> memo		       = new HashMap<AbstractModule, Map<E, Integer>>();

   public boolean isInRecursion(AbstractModule caller,E def)
      {
      Map<E, Integer> val;

      boolean occuredMoreThanMax = false;
      if (!memo.containsKey(caller)) {

	 // still does not exist, let's create the structure...
	 val = new HashMap<E, Integer>();
	 memo.put(caller, val);

       }
      else {
	 val = memo.get(caller);

	 if (val.containsKey(def)) { // may be a recursion
	    Integer numberOfOccurences = val.get(def);

	    // should never be null...
	    if (numberOfOccurences > max_value) {
	       occuredMoreThanMax = true; // ok, we are recursing...
	     }
	  }
       }

      // let's raise the number of ocurrences anyway
      Integer numberOfOccurences = val.get(def);
      if (numberOfOccurences == null) {
	 val.put(def, 1); // this is the first ocurrence
       }
      else {
	 val.put(def, numberOfOccurences + 1);
       }

      return occuredMoreThanMax;
    }
}

/**
 * @param line2 starting at 0
 * @param col2 starting at 0
 * @param token
 * @param qual
 * @param nature2
 */
public CompletionState(int line2,int col2,String token,PybaseNature nature2,
			  String qualifier)
{
   this(line2,col2,token,nature2,qualifier,new CompletionCache());
}

/**
 * @param line2 starting at 0
 * @param col2 starting at 0
 * @param token
 * @param qual
 * @param nature2
 */
public CompletionState(int line2,int col2,String token,PybaseNature nature2,
			  String qualifier,ICompletionCache completionCache)
{
   for_line = line2;
   for_col = col2;
   activation_token = token;
   the_nature = nature2;
   for_qualifier = qualifier;
   assert(completionCache != null);
   completion_cache = completionCache;
}

public CompletionState()
{
}


/**
 * @param for_module
 * @param base
 */
public void checkWildImportInMemory(AbstractModule caller,AbstractModule wild)
throws CompletionRecursionException
{
   if (wild_import_memory.isInRecursion(caller, wild)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error -- (caller: "
	    + caller.getName() + ", import: " + wild.getName()
	    + " ) - stopping analysis.");
    }

}

/**
 * @param module
 * @param definition
 */
public void checkDefinitionMemory(AbstractModule module,Definition definition)
throws CompletionRecursionException
{
   if (definition_memory.isInRecursion(module, definition)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error --  (module: "
	    + module.getName() + ", token: " + definition
	    + ") - stopping analysis.");
    }

}

/**
 * @param module
 */
public void checkFindMemory(AbstractModule module,String value)
throws CompletionRecursionException
{
   if (find_memory.isInRecursion(module, value)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error --  (module: "
	    + module.getName() + ", value: " + value
	    + ") - stopping analysis.");
    }

}

/**
 * @param module
 * @throws CompletionRecursionException
 */
public void checkResolveImportMemory(AbstractModule module,String value)
throws CompletionRecursionException
{
   if (resolve_import_memory.isInRecursion(module, value)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error --  (module: "
	    + module.getName() + ", value: " + value
	    + ") - stopping analysis.");
    }

}

public void checkFindDefinitionMemory(AbstractModule mod,String tok)
throws CompletionRecursionException
{
   if (find_definition_memory.isInRecursion(mod, tok)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error --  (module: "
	    + mod.getName() + ", value: " + tok + ") - stopping analysis.");
    }
}

public void checkFindLocalDefinedDefinitionMemory(AbstractModule mod,String tok)
throws CompletionRecursionException
{
   if (find_local_defined_definition_memory.isInRecursion(mod, tok)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error --  (module: "
	    + mod.getName() + ", value: " + tok + ") - stopping analysis.");
    }
}

/**
 * @param module
 * @param base
 */
public void checkMemory(AbstractModule module,String base) throws CompletionRecursionException
{
   if (state_memory.isInRecursion(module, base)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error --  (module: "
	    + module.getName() + ", token: " + base
	    + ") - stopping analysis.");
    }
}


private Set<Tuple3<Integer,Integer,AbstractModule>> found_same_definition_memory = new HashSet<>();

public boolean checkFoudSameDefinition(int line,int col,AbstractModule mod)
{
   Tuple3<Integer, Integer, AbstractModule> key = new Tuple3<Integer, Integer, AbstractModule>(line,
												  col,mod);
   if (found_same_definition_memory.contains(key)) {
      return true;
    }
   found_same_definition_memory.add(key);
   return false;
}


public boolean canStillCheckFindSourceFromCompiled(AbstractModule mod,String tok)
{
   if (!find_source_from_compiled_memory.isInRecursion(mod, tok)) {
      return true;
    }
   return false;
}

/**
 * This check is a bit different from the others because of the context it will work in...
 *
 *	This check is used when resolving things from imports, so, it may check for recursions found when in previous context, but
 *	if a recursion is found in the current context, that's ok (because it's simply trying to get the actual representation for a token)
*/
public void checkFindResolveImportMemory(AbstractToken token)
throws CompletionRecursionException
{
   Iterator<Memo<AbstractToken>> it = find_resolve_import_memory.iterator();
   while (it.hasNext()) {
      Memo<AbstractToken> memo = it.next();
      if (memo.isInRecursion(null, token)) {
	 // if(it.hasNext()){
	 throw new CompletionRecursionException(
	    "Possible recursion found -- probably programming error --  (token: "
	       + token + ") - stopping analysis.");
	 // }
       }
    }
}

public void popFindResolveImportMemoryCtx()
{
   find_resolve_import_memory.pop();
}

public void pushFindResolveImportMemoryCtx()
{
   find_resolve_import_memory.push(new Memo<AbstractToken>());
}

/**
 * @param for_module
 * @param base
 */
public void checkFindModuleCompletionsMemory(AbstractModule mod,String tok)
throws CompletionRecursionException
{
   if (find_module_completions_memory.isInRecursion(mod, tok)) {
      throw new CompletionRecursionException(
	 "Possible recursion found -- probably programming error --  (module: "
	    + mod.getName() + ", token: " + tok + ") - stopping analysis.");
    }
}

public String getActivationToken()
{
   return activation_token;
}

public PybaseNature getNature()
{
   return the_nature;
}

public void setActivationToken(String string)
{
   activation_token = string;
}

public String getFullActivationToken()
{
   return full_activation_token;
}

public void setFullActivationToken(String act)
{
   full_activation_token = act;
}

public void setBuiltinsGotten(boolean b)
{
   builtins_gotten = b;
}

/**
 * @param i: starting at 0
 */
public void setCol(int i)
{
   for_col = i;
}

/**
 * @param i: starting at 0
 */
public void setLine(int i)
{
   for_line = i;
}

public void setLocalImportsGotten(boolean b)
{
   local_imports_gotten = b;
}

public boolean getLocalImportsGotten()
{
   return local_imports_gotten;
}

public int getLine()
{
   return for_line;
}

public int getCol()
{
   return for_col;
}

public boolean getBuiltinsGotten()
{
   return builtins_gotten;
}

public void raiseNFindTokensOnImportedModsCalled(AbstractModule mod,String tok)
throws CompletionRecursionException
{
   if (imported_mods_called.isInRecursion(mod, tok)) {
      throw new CompletionRecursionException("Possible recursion found (mod: "
						+ mod.getName() + ", tok: " + tok + " ) - stopping analysis.");
    }
}

public boolean getIsInCalltip()
{
   return is_in_calltip;
}

public void setLookingFor(LookForType b)
{
   setLookingFor(b, false);
}

public void setLookingFor(LookForType b,boolean force)
{
   // the 1st is the one that counts (or it can be forced)
   if (looking_for_instance == LookForType.INSTANCE_UNDEFINED
	  || force) {
      looking_for_instance = b;
    }
}

public LookForType getLookingFor()
{
   return looking_for_instance;
}

public CompletionState getCopyWithActTok(String value)
{
   CompletionState copy = getCopy();
   copy.setActivationToken(value);
   return copy;
}

public String getQualifier()
{
   return for_qualifier;
}

public void setIsInCalltip(boolean isInCalltip)
{
   is_in_calltip = isInCalltip;
}

public void setTokenImportedModules(List<AbstractToken> tokenImportedModules)
{
   if (tokenImportedModules != null) {
      if (token_imported_modules == null) {
	 token_imported_modules = new ArrayList<AbstractToken>(tokenImportedModules); // keep
	 // a copy of it
       }
    }
}

public List<AbstractToken> getTokenImportedModules()
{
   return token_imported_modules;
}



/********************************************************************************/
/*										*/
/*	ICompletionCache interface implementation				*/
/*										*/
/********************************************************************************/

@Override public void add(Object key,Object n)
{
   completion_cache.add(key, n);
}

@Override public Object getObj(Object o)
{
   return completion_cache.getObj(o);
}

@Override public void remove(Object key)
{
   completion_cache.remove(key);
}

@Override public void removeStaleEntries()
{
   completion_cache.removeStaleEntries();
}

@Override public void clear()
{
   completion_cache.clear();
}



}	// end of class CompletionState


/* end of CompletionState.java */

