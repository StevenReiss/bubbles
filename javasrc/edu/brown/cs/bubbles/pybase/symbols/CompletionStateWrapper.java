/********************************************************************************/
/*										*/
/*		CompletionStateWrapper						*/
/*										*/
/*	Python Bubbles Base completion state wrapper				*/
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

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants.LookForType;
import edu.brown.cs.bubbles.pybase.PybaseNature;

import org.python.pydev.core.structure.CompletionRecursionException;

import java.util.List;


public class CompletionStateWrapper extends CompletionState {

private CompletionState wrapped_state;

public CompletionStateWrapper(CompletionState state)
{
   wrapped_state = state;
   local_activation_token = state.getActivationToken();
   local_local_imports_gotten = state.getLocalImportsGotten();
}

// things that are not delegated
// ------------------------------------------------------------------------------------
private String	local_activation_token;
private int	local_for_col  = -1;
private int	local_for_line = -1;
private boolean local_local_imports_gotten;

@Override public String getActivationToken()
{
   return local_activation_token;
}

@Override public void setActivationToken(String string)
{
   local_activation_token = string;
}


@Override public String getFullActivationToken()
{
   return wrapped_state.getFullActivationToken();
}

@Override public void setFullActivationToken(String act)
{
   wrapped_state.setFullActivationToken(act);
}

@Override public boolean getLocalImportsGotten()
{
   return local_local_imports_gotten;
}

@Override public void setLocalImportsGotten(boolean b)
{
   local_local_imports_gotten = b;
}

@Override public int getCol()
{
   return local_for_col;
}

@Override public int getLine()
{
   return local_for_line;
}

@Override public void setCol(int i)
{
   local_for_col = i;
}

@Override public void setLine(int i)
{
   local_for_line = i;
}


// delegated
// --------------------------------------------------------------------------------------------------------
@Override public void checkDefinitionMemory(AbstractModule module,Definition definition)
	 throws CompletionRecursionException
{
   wrapped_state.checkDefinitionMemory(module, definition);
}

@Override public void checkFindLocalDefinedDefinitionMemory(AbstractModule mod,String tok)
	 throws CompletionRecursionException
{
   wrapped_state.checkFindLocalDefinedDefinitionMemory(mod, tok);
}

@Override public void checkFindDefinitionMemory(AbstractModule mod,String tok)
	 throws CompletionRecursionException
{
   wrapped_state.checkFindDefinitionMemory(mod, tok);
}

@Override public void checkFindMemory(AbstractModule module,String value)
	 throws CompletionRecursionException
{
   wrapped_state.checkFindMemory(module, value);
}

@Override public void checkFindModuleCompletionsMemory(AbstractModule mod,String tok)
	 throws CompletionRecursionException
{
   wrapped_state.checkFindModuleCompletionsMemory(mod, tok);
}

@Override public void checkFindResolveImportMemory(AbstractToken tok) throws CompletionRecursionException
{
   wrapped_state.checkFindResolveImportMemory(tok);
}

@Override public void checkMemory(AbstractModule module,String base) throws CompletionRecursionException
{
   wrapped_state.checkMemory(module, base);
}

@Override public void checkResolveImportMemory(AbstractModule module,String value)
	 throws CompletionRecursionException
{
   wrapped_state.checkResolveImportMemory(module, value);
}

@Override public void checkWildImportInMemory(AbstractModule current,AbstractModule mod)
	 throws CompletionRecursionException
{
   wrapped_state.checkWildImportInMemory(current, mod);
}

@Override public boolean checkFoudSameDefinition(int line,int col,AbstractModule mod)
{
   return wrapped_state.checkFoudSameDefinition(line, col, mod);
}

@Override public boolean canStillCheckFindSourceFromCompiled(AbstractModule mod,String tok)
{
   return wrapped_state.canStillCheckFindSourceFromCompiled(mod, tok);
}


@Override public boolean getBuiltinsGotten()
{
   return wrapped_state.getBuiltinsGotten();
}

@Override public CompletionState getCopy()
{
   return wrapped_state.getCopy();
}

@Override public CompletionState getCopyForResolveImportWithActTok(String representation)
{
   return wrapped_state.getCopyForResolveImportWithActTok(representation);
}

@Override public CompletionState getCopyWithActTok(String value)
{
   return wrapped_state.getCopyWithActTok(value);
}

@Override public boolean getIsInCalltip()
{
   return wrapped_state.getIsInCalltip();
}


@Override public PybaseNature getNature()
{
   return wrapped_state.getNature();
}

@Override public String getQualifier()
{
   return wrapped_state.getQualifier();
}

@Override public LookForType getLookingFor()
{
   return wrapped_state.getLookingFor();
}

@Override public void raiseNFindTokensOnImportedModsCalled(AbstractModule mod,String tok)
	 throws CompletionRecursionException
{
   wrapped_state.raiseNFindTokensOnImportedModsCalled(mod, tok);
}

@Override public void setBuiltinsGotten(boolean b)
{
   wrapped_state.setBuiltinsGotten(b);
}


@Override public void setIsInCalltip(boolean isInCalltip)
{
   wrapped_state.setIsInCalltip(isInCalltip);
}

@Override public void setLookingFor(LookForType b)
{
   wrapped_state.setLookingFor(b);
}

@Override public void setLookingFor(LookForType b,boolean force)
{
   wrapped_state.setLookingFor(b, force);
}

@Override public void popFindResolveImportMemoryCtx()
{
   wrapped_state.popFindResolveImportMemoryCtx();
}

@Override public void pushFindResolveImportMemoryCtx()
{
   wrapped_state.pushFindResolveImportMemoryCtx();
}

@Override public List<AbstractToken> getTokenImportedModules()
{
   return wrapped_state.getTokenImportedModules();
}

@Override public void setTokenImportedModules(List<AbstractToken> tokenImportedModules)
{
   wrapped_state.setTokenImportedModules(tokenImportedModules);
}

@Override public String toString()
{
   StringBuilder buf = new StringBuilder();
   buf.append("CompletionStateWrapper[ ");
   buf.append(local_activation_token);
   buf.append(" ]");
   return buf.toString();
}

@Override public void add(Object key,Object n)
{
   wrapped_state.add(key, n);
}

@Override public Object getObj(Object o)
{
   return wrapped_state.getObj(o);
}

@Override public void remove(Object key)
{
   wrapped_state.remove(key);
}

@Override public void clear()
{
   wrapped_state.clear();
}

@Override public void removeStaleEntries()
{
   wrapped_state.removeStaleEntries();
}

} // end of class CompletionStateWrapper


/* end of CompletionStateWrapper.java */
