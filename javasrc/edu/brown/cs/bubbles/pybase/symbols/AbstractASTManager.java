/********************************************************************************/
/*										*/
/*		AbstractASTManager.java 					*/
/*										*/
/*	Python Bubbles Base generic AST manager implementation			*/
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

import edu.brown.cs.bubbles.pybase.PybaseConstants.ISemanticData;
import edu.brown.cs.bubbles.pybase.PybaseConstants.LookForType;
import edu.brown.cs.bubbles.pybase.PybaseConstants.TokenType;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseParser;
import edu.brown.cs.bubbles.pybase.PybaseProject;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.TupleN;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.name_contextType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;


public abstract class AbstractASTManager {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected volatile ModulesManager modulesManager;

private static final AbstractToken[] EMPTY_ITOKEN_ARRAY = new AbstractToken[0];

private static final boolean  DEBUG_CACHE	= false;

private final AssignAnalysis  assign_analysis	  = new AssignAnalysis();





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected AbstractASTManager()
{}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public ModulesManager getModulesManager()	{ return modulesManager; }


/**
 * Set the nature this ast manager works with (if no project is available and a nature is).
 */
public void setNature(PybaseNature nature)
{
   getModulesManager().setPythonNature(nature);
}

public PybaseNature getNature()
{
   return getModulesManager().getNature();
}


/********************************************************************************/
/*										*/
/*	Abstract methods							*/
/*										*/
/********************************************************************************/

public abstract void setProject(PybaseProject project,PybaseNature nature,
				   boolean restoreDeltas);

public abstract void rebuildModule(File file,IDocument doc,PybaseProject project,
				      PybaseNature nature);

public abstract void removeModule(File file,PybaseProject project);


/**
 * Returns the imports that start with a given string. The comparison is not case dependent. Passes all the modules in the cache.
 *
 * @param original is the name of the import module eg. 'from toimport import ' would mean that the original is 'toimport'
 * or something like 'foo.bar' or an empty string (if only 'import').
 * @return a Set with the imports as tuples with the name, the docstring.
 * @throws CompletionRecursionException
 */
public AbstractToken[] getCompletionsForImport(ImportInfo importInfo,ICompletionRequest r,
						  boolean onlyGetDirectModules) throws CompletionRecursionException
{
   //	String original = importInfo.importsTipperStr;
   //	String afterDots = null;
   //	int level = 0; // meaning: no absolute import
   //
   //	boolean onlyDots = true;
   //	if (original.startsWith(".")) {
   //	   // if the import has leading dots, this means it is something like
   //	   // from ...bar import xxx (new way to express the relative import)
   //	   for (int i = 0; i < original.length(); i++) {
   //	    if (original.charAt(i) != '.') {
   //	       onlyDots = false;
   //	       afterDots = original.substring(i);
   //	       break;
   //	    }
   //	    // add one to the relative import level
   //	    level++;
   //	   }
   //	}
   //	ICompletionRequest request = r;
   //	PybaseNature nature = request.getNature();
   //
   //	String relative = null;
   //	String moduleName = null;
   //	if (request.getEditorFile() != null) {
   //	   moduleName = nature.getAstManager().getModulesManager()
   //		  .resolveModule(PybaseFileSystem.getFileAbsolutePath(request.getEditorFile()));
   //	   if (moduleName != null) {
   //
   //	    if (level > 0) {
   //	       // ok, it is the import added on python 2.5 (from .. import xxx)
   //	       List<String> moduleParts = StringUtils.dotSplit(moduleName);
   //	       if (moduleParts.size() > level) {
   //		  relative = FullRepIterable.joinParts(moduleParts, moduleParts.size()
   //			   - level);
   //	       }
   //
   //	       if (!onlyDots) {
   //		  // ok, we have to add the other part too, as we have more than the leading
   //// dots
   //		  // from ..bar import
   //		  relative += "." + afterDots;
   //	       }
   //
   //	    }
   //	    else {
   //	       String tail = FullRepIterable.headAndTail(moduleName)[0];
   //	       if (original.length() > 0) {
   //		  relative = tail + "." + original;
   //	       }
   //	       else {
   //		  relative = tail;
   //	       }
   //	    }
   //	   }
   //	}
   //
   //	// set to hold the completion (no duplicates allowed).
   //	Set<AbstractToken> set = new HashSet<AbstractToken>();
   //
   //	String absoluteModule = original;
   //	if (absoluteModule.endsWith(".")) {
   //	   absoluteModule = absoluteModule.substring(0, absoluteModule.length() - 1);
   //	}
   //
   //	if (level == 0) {
   //	   // first we get the imports... that complete for the token.
   //	   getAbsoluteImportTokens(absoluteModule, set, AbstractToken.TYPE_IMPORT, false, importInfo,
   //		  onlyGetDirectModules);
   //
   //	   // Now, if we have an initial module, we have to get the completions
   //	   // for it.
   //	   getTokensForModule(original, nature, absoluteModule, set);
   //	}
   //
   //	if (relative != null && relative.equals(absoluteModule) == false) {
   //	   getAbsoluteImportTokens(relative, set, AbstractToken.TYPE_RELATIVE_IMPORT, false,
   //		  importInfo, onlyGetDirectModules);
   //	   if (importInfo.hasImportSubstring) {
   //	    getTokensForModule(relative, nature, relative, set);
   //	   }
   //	}
   //
   //	if (level == 1 && moduleName != null) {
   //	   // has returned itself, so, let's remove it
   //	   String strToRemove = FullRepIterable.getLastPart(moduleName);
   //	   for (Iterator<AbstractToken> it = set.iterator(); it.hasNext();) {
   //	    AbstractToken o = it.next();
   //	    if (o.getRepresentation().equals(strToRemove)) {
   //	       it.remove();
   //	       // don't break because the token might be different, but not the
   //// representation...
   //	    }
   //	   }
   //	}
   //	return set.toArray(EMPTY_ITOKEN_ARRAY);
   return EMPTY_ITOKEN_ARRAY;
}


/**
 * @param moduleToGetTokensFrom the string that represents the token from where we are getting the imports
 * @param set the set where the tokens should be added
 * @param importInfo if null, only the 1st element of the module will be added, otherwise, it'll check the info
							    * to see if it should add only the 1st element of the module or the complete module (e.g.: add only xml or
																		    * xml.dom and other submodules too)
							    */
public void getAbsoluteImportTokens(String moduleToGetTokensFrom,Set<AbstractToken> inputOutput,
				       TokenType type,boolean onlyFilesOnSameLevel,ImportInfo importInfo,
				       boolean onlyGetDirectModules)
{

   // boolean getSubModules = false;
   // if(importInfo != null){
   // //we only want to get submodules if we're in:
   // //from xxx
   // //import xxx
   // //
   // //We do NOT want to get it on:
   // //from xxx import yyy
   // if(importInfo.hasFromSubstring != importInfo.hasImportSubstring){
   // getSubModules = true;
   // }
   // }

   HashMap<String, AbstractToken> temp = new HashMap<String, AbstractToken>();
   SortedMap<ModulesKey, ModulesKey> modulesStartingWith;
   if (onlyGetDirectModules) {
      modulesStartingWith = modulesManager
	 .getAllDirectModulesStartingWith(moduleToGetTokensFrom);
    }
   else {
      modulesStartingWith = modulesManager
	 .getAllModulesStartingWith(moduleToGetTokensFrom);
    }

   Iterator<ModulesKey> itModules = modulesStartingWith.keySet().iterator();
   while (itModules.hasNext()) {
      ModulesKey key = itModules.next();

      String element = key.getModuleName();
      // if (element.startsWith(moduleToGetTokensFrom)) { we don't check that anymore because we
      // get all the modules starting with it already
      if (onlyFilesOnSameLevel && key.getModuleFile() != null && key.getModuleFile().isDirectory()) {
	 continue; // we only want those that are in the same directory, and not in other
	 // directories...
       }
      element = element.substring(moduleToGetTokensFrom.length());

      // we just want those that are direct
      // this means that if we had initially element = testlib.unittest.anothertest
      // and element became later = .unittest.anothertest, it will be ignored (we
      // should only analyze it if it was something as testlib.unittest and became
      // .unittest
      // we only check this if we only want file modules (in
      if (onlyFilesOnSameLevel && countChars('.', element) > 1) {
	 continue;
       }

      boolean goForIt = false;
      // if initial is not empty only get those that start with a dot (submodules, not
      // modules that start with the same name).
      // e.g. we want xml.dom
      // and not xmlrpclib
      // if we have xml token (not using the qualifier here)
      if (moduleToGetTokensFrom.length() != 0) {
	 if (element.length() > 0 && element.charAt(0) == ('.')) {
	    element = element.substring(1);
	    goForIt = true;
	  }
       }
      else {
	 goForIt = true;
       }

      if (element.length() > 0 && goForIt) {
	 List<String> splitted = StringUtils.dotSplit(element);
	 if (splitted.size() > 0) {
	    String strToAdd;

	    strToAdd = splitted.get(0);
	    // if(!getSubModules){
	    // }else{
	    // if(element.endsWith(".__init__")){
	    // strToAdd = element.substring(0, element.length()-9);
	    // }else{
	    // strToAdd = element;
	    // }
	    // }
	    // this is the completion
	    temp.put(strToAdd, new ConcreteToken(strToAdd,"","",moduleToGetTokensFrom,
						    type));
	  }
       }
      // }
    }
   inputOutput.addAll(temp.values());
}

/**
 * @param original this is the initial module where the completion should happen (may have class in it too)
 * @param moduleToGetTokensFrom
 * @param set set where the tokens should be added
 * @throws CompletionRecursionException
 */
protected void getTokensForModule(String original,PybaseNature nature,
				     String moduleToGetTokensFrom,Set<AbstractToken> set)
throws CompletionRecursionException
{
   if (moduleToGetTokensFrom.length() > 0) {
      if (original.endsWith(".")) {
	 original = original.substring(0, original.length() - 1);
       }

      Tuple<AbstractModule, String> modTok = findModuleFromPath(original, nature, false, null);
      // the current module name is not used as it is not relative
      AbstractModule m = modTok.o1;
      String tok = modTok.o2;

      if (m == null) {
	 // we were unable to find it with the given path, so, there's nothing else to do here...
	 return;
       }

      AbstractToken[] globalTokens;
      if (tok != null && tok.length() > 0) {
	 CompletionState state2 = new CompletionState(-1,-1,tok,nature,"");
	 state2.setBuiltinsGotten(true); // we don't want to get builtins here
	 globalTokens = m.getGlobalTokens(state2, this);
       }
      else {
	 CompletionState state2 = new CompletionState(-1,-1,"",nature,"");
	 state2.setBuiltinsGotten(true); // we don't want to get builtins here
	 globalTokens = getCompletionsForModule(m, state2);
       }

      for (int i = 0; i < globalTokens.length; i++) {
	 AbstractToken element = globalTokens[i];
	 // this is the completion
	 set.add(element);
       }
    }
}


/**
 * @param file
 * @param doc
 * @param state
 * @return
 */
public static AbstractModule createModule(File file,IDocument doc,CompletionState state,
					     AbstractASTManager manager)
{
   PybaseNature pythonNature = state.getNature();
   int line = state.getLine();
   ModulesManager projModulesManager = manager.getModulesManager();

   return AbstractModule.createModuleFromDoc(file, doc, pythonNature, line,
						projModulesManager);
}

public AbstractToken[] getCompletionsForToken(File file,IDocument doc,CompletionState state)
throws CompletionRecursionException
{
   AbstractModule module = createModule(file, doc, state, this);
   return getCompletionsForModule(module, state, true, true);
}





public AbstractToken[] getCompletionsForToken(IDocument doc,CompletionState state)
{
   AbstractToken[] completionsForModule;
   try {
      PybaseParser pp = new PybaseParser(state.getNature().getProject(),doc);
      ISemanticData sd = pp.parseDocument(false);
      SimpleNode n = sd.getRootNode();
      // Tuple<SimpleNode, Throwable> obj = PyParser
      //      .reparseDocument(new PyParser.ParserInfo(doc,true,state.getNature(),state
      //		.getLine()));
      // SimpleNode n = obj.o1;
      AbstractModule module = AbstractModule.createModule(n);

      completionsForModule = getCompletionsForModule(module, state, true, true);

    }
   catch (Exception e) {
      String message = e.getMessage();
      if (message == null) {
	 if (e instanceof NullPointerException) {
	    PybaseMain.logE("Problem getting token completions",e);
	    message = "NullPointerException";
	  }
	 else {
	    message = "Null error message";
	  }
       }
      completionsForModule = new AbstractToken[] { new ConcreteToken(message,message,"","",
									TokenType.UNKNOWN) };
    }

   return completionsForModule;
}



/**
 * By default does not look for relative import
 */
public AbstractModule getModule(String name,PybaseNature nature,boolean dontSearchInit)
{
   return modulesManager.getModule(name, nature, dontSearchInit, false);
}




/**
 * This method returns the module that corresponds to the path passed as a parameter.
 *
 * @param name the name of the module we're looking for
 * @param lookingForRelative determines whether we're looking for a relative module (in which case we should
											* not check in other places... only in the module)
 * @return the module represented by this name
 */
public AbstractModule getModule(String name,PybaseNature nature,boolean dontSearchInit,
				   boolean lookingForRelative)
{
   if (lookingForRelative) {
      return modulesManager.getRelativeModule(name, nature);
    }
   else {
      return modulesManager.getModule(name, nature, dontSearchInit);
    }
}




/**
 * Identifies the token passed and if it maps to a builtin not 'easily recognizable', as
 * a string or list, we return it.
 *
 * @param state
 * @return
 */
protected AbstractToken[] getBuiltinsCompletions(CompletionState state)
{
   CompletionState state2 = state.getCopy();

   String act = state.getActivationToken();

   // check for the builtin types.
   state2.setActivationToken(NodeUtils.getBuiltinType(act));

   if (state2.getActivationToken() != null) {
      AbstractModule m = getBuiltinMod(state.getNature());
      if (m != null) {
	 return m.getGlobalTokens(state2, this);
       }
    }

   if (act.equals("__builtins__") || act.startsWith("__builtins__.")) {
      act = act.substring(12);
      if (act.startsWith(".")) {
	 act = act.substring(1);
       }
      AbstractModule m = getBuiltinMod(state.getNature());
      CompletionState state3 = state.getCopy();
      state3.setActivationToken(act);
      return m.getGlobalTokens(state3, this);
    }
   return null;
}



public AbstractToken[] getCompletionsForModule(AbstractModule module,CompletionState state)
throws CompletionRecursionException
{
   return getCompletionsForModule(module, state, true);
}



public AbstractToken[] getCompletionsForModule(AbstractModule module,CompletionState state,
						  boolean searchSameLevelMods) throws CompletionRecursionException
{
   return getCompletionsForModule(module, state, true, false);
}



public AbstractToken[] getCompletionsForModule(AbstractModule module,CompletionState state,
						  boolean searchSameLevelMods,boolean lookForArgumentCompletion)
throws CompletionRecursionException
{
   return getCompletionsForModule(module, state, searchSameLevelMods,
				     lookForArgumentCompletion, false);
}


/**
 * @see #getCompletionsForModule(AbstractModule, CompletionState, boolean, boolean)
 *
 * Same thing but may handle things as if it was a wild import (in which case, the tokens starting with '_' are
								   * removed and if __all__ is available, only the tokens contained in __all__ are returned)
 */
public AbstractToken[] getCompletionsForModule(AbstractModule module,CompletionState state,
						  boolean searchSameLevelMods,boolean lookForArgumentCompletion,
						  boolean handleAsWildImport) throws CompletionRecursionException
{
   String name = module.getName();
   Object key = new TupleN("getCompletionsForModule",name != null ? name : "",
			      state.getActivationToken(),searchSameLevelMods,lookForArgumentCompletion,
			      state.getBuiltinsGotten(),state.getLocalImportsGotten(),handleAsWildImport);

   AbstractToken[] ret = (AbstractToken[]) state.getObj(key);
   if (ret != null) {
      if (DEBUG_CACHE) {
	 System.out.println("Checking if cache is correct for: " + key);
	 AbstractToken[] internal = internalGenerateGetCompletionsForModule(module, state,
									       searchSameLevelMods, lookForArgumentCompletion);
	 internal = filterForWildImport(module, handleAsWildImport, internal);
	 // the new request may actually have no tokens if a completion exception occurred.
	 if (internal.length != 0 && ret.length != internal.length) {
	    throw new RuntimeException(
	       "This can't happen... it should always return the same completions!");
	  }
       }
      return ret;
    }

   AbstractToken[] completionsForModule = internalGenerateGetCompletionsForModule(module, state,
										     searchSameLevelMods, lookForArgumentCompletion);
   completionsForModule = filterForWildImport(module, handleAsWildImport,
						 completionsForModule);

   state.add(key, completionsForModule);
   return completionsForModule;
}



/**
 * Filters the tokens according to the wild import rules:
 * - the tokens starting with '_' are removed
 * - if __all__ is available, only the tokens contained in __all__ are returned)
*/
private AbstractToken[] filterForWildImport(AbstractModule module,boolean handleAsWildImport,
					       AbstractToken[] completionsForModule)
{
   if (module != null && handleAsWildImport) {
      ArrayList<AbstractToken> ret = new ArrayList<AbstractToken>();

      for (int j = 0; j < completionsForModule.length; j++) {
	 AbstractToken token = completionsForModule[j];
	 // on wild imports we don't get names that start with '_'
	 if (!token.getRepresentation().startsWith("_")) {
	    ret.add(token);
	  }
       }

      if (module instanceof SourceModule) {
	 // Support for __all__: filter things if __all__ is available.
	 SourceModule sourceModule = (SourceModule) module;
	 GlobalModelVisitor globalModelVisitorCache = sourceModule
	    .getGlobalModelVisitorCache();
	 if (globalModelVisitorCache != null) {
	    globalModelVisitorCache.filterAll(ret);
	  }
       }
      return ret.toArray(new AbstractToken[ret.size()]);
    }
   else {
      return completionsForModule;
    }
}



/**
 * This method should only be accessed from the public getCompletionsForModule (which caches the result).
 */
private AbstractToken[] internalGenerateGetCompletionsForModule(AbstractModule module,
								   CompletionState state,boolean searchSameLevelMods,
								   boolean lookForArgumentCompletion) throws CompletionRecursionException
{

   ArrayList<AbstractToken> importedModules = new ArrayList<AbstractToken>();

   LocalScope localScope = null;
   int line = state.getLine();
   int col = state.getCol();

   if (state.getLocalImportsGotten() == false) {
      // in the first analyzed module, we have to get the local imports too.
      state.setLocalImportsGotten(true);
      if (module != null && line >= 0) {
	 localScope = module.getLocalScope(line, col);
	 if (localScope != null) {
	    importedModules.addAll(localScope.getLocalImportedModules(line + 1, col + 1,
									 module.getName()));
	  }
       }
    }

   AbstractToken[] builtinsCompletions = getBuiltinsCompletions(state);
   if (builtinsCompletions != null) {
      return builtinsCompletions;
    }

   String act = state.getActivationToken();
   int parI = act.indexOf('(');
   if (parI != -1) {
      state.setFullActivationToken(act);
      act = act.substring(0, parI);
      state.setActivationToken(act);
      state.setLookingFor(LookForType.INSTANCED_VARIABLE);
    }

   if (module != null) {
      // get the tokens (global, imported and wild imported)
      AbstractToken[] globalTokens = module.getGlobalTokens();

      List<AbstractToken> tokenImportedModules = Arrays.asList(module.getTokenImportedModules());
      importedModules.addAll(tokenImportedModules);
      state.setTokenImportedModules(importedModules);
      AbstractToken[] wildImportedModules = module.getWildImportedModules();


      // now, lets check if this is actually a module that is an __init__ (if so, we have to get all
      // the other .py files as modules that are in the same level as the __init__)
      Set<AbstractToken> initial = new HashSet<AbstractToken>();
      if (searchSameLevelMods) {
	 // now, we have to ask for the module if it's a 'package' (folders that have __init__.py for python
	 // or only folders -- not classes -- in java).
	 if (module.isPackage()) {
	    HashSet<AbstractToken> gotten = new HashSet<AbstractToken>();
	    // the module also decides how to get its submodules
	    getAbsoluteImportTokens(module.getPackageFolderName(), gotten,
				       TokenType.IMPORT, true, null, false);
	    for (AbstractToken token : gotten) {
	       if (token.getRepresentation().equals("__init__") == false) {
		  initial.add(token);
		}
	     }
	  }
       }

      if (state.getActivationToken().length() == 0) {
	 List<AbstractToken> completions = getGlobalCompletions(globalTokens,
								   importedModules.toArray(EMPTY_ITOKEN_ARRAY), wildImportedModules,
								   state, module);

	 // now find the locals for the module
	 if (line >= 0) {
	    AbstractToken[] localTokens = module.getLocalTokens(line, col, localScope);
	    for (int i = 0; i < localTokens.length; i++) {
	       completions.add(localTokens[i]);
	     }
	  }
	 completions.addAll(initial); // just add all that are in the same level if it was an __init__

	 return completions.toArray(EMPTY_ITOKEN_ARRAY);
       }
      else {
	 // ok, we have a token, find it and get its completions.
	 // first check if the token is a module... if it is, get the completions for that module.
	 AbstractToken[] tokens = findTokensOnImportedMods(
	    importedModules.toArray(EMPTY_ITOKEN_ARRAY), state, module);
	 if (tokens != null && tokens.length > 0) {
	    return decorateWithLocal(tokens, localScope, state);
	  }

	 // if it is an __init__, modules on the same level are treated as local tokens
	 if (searchSameLevelMods) {
	    tokens = searchOnSameLevelMods(initial, state);
	    if (tokens != null && tokens.length > 0) {
	       return decorateWithLocal(tokens, localScope, state);
	     }
	  }

	 // for wild imports, we must get the global completions with __all__ filtered
	 // wild imports: recursively go and get those completions and see if any matches it.
	 for (int i = 0; i < wildImportedModules.length; i++) {

	    AbstractToken name = wildImportedModules[i];
	    AbstractModule mod = getModule(name.getAsRelativeImport(module.getName()),
					      state.getNature(), false);
	    // relative (for wild imports this is
	    // ok... only a module can be used in wild imports)

	    if (mod == null) {
	       mod = getModule(name.getOriginalRep(), state.getNature(), false); // absolute
	     }


	    if (mod != null) {
	       state.checkFindModuleCompletionsMemory(mod, state.getActivationToken());
	       AbstractToken[] completionsForModule = getCompletionsForModule(mod, state);
	       if (completionsForModule.length > 0) return decorateWithLocal(
		  completionsForModule, localScope, state);
	     }
	    else {
	       PybaseMain.logE("Module not found:" + name.getRepresentation());
	     }
	  }

	 // it was not a module (would have returned already), so, try to get the
	 // completions for a global token defined.
	 tokens = module.getGlobalTokens(state, this);
	 if (tokens.length > 0) {
	    return decorateWithLocal(tokens, localScope, state);
	  }

	 // If it was still not found, go to builtins.
	 AbstractModule builtinsMod = getBuiltinMod(state.getNature());
	 if (builtinsMod != null && builtinsMod != module) {
	    tokens = getCompletionsForModule(builtinsMod, state);
	    if (tokens.length > 0) {
	       if (tokens[0].getRepresentation().equals("ERROR:") == false) {
		  return decorateWithLocal(tokens, localScope, state);
		}
	     }
	  }

	 if (lookForArgumentCompletion && localScope != null) {
	    // now, if we have to look for arguments and search things in the local scope,
	    // let's also check for assert (isinstance...) in this scope with the given variable.
	    List<String> lookForClass = localScope
	       .getPossibleClassesForActivationToken(state.getActivationToken());
	    if (lookForClass.size() > 0) {
	       HashSet<AbstractToken> hashSet = new HashSet<AbstractToken>();

	       getCompletionsForClassInLocalScope(module, state, searchSameLevelMods,
						     lookForArgumentCompletion, lookForClass, hashSet);

	       if (hashSet.size() > 0) {
		  return hashSet.toArray(EMPTY_ITOKEN_ARRAY);
		}
	     }

	    // ok, didn't find in assert isinstance... keep going
	    // if there was no assert for the class, get from extensions / local scope
	    // interface
	    tokens = CompletionParticipantsHelper.getCompletionsForMethodParameter(state,
										      localScope).toArray(EMPTY_ITOKEN_ARRAY);
	    if (tokens != null && tokens.length > 0) {
	       return tokens;
	     }
	  }

	 // nothing worked so far, so, let's look for an assignment...
	 return getAssignCompletions(module, state, lookForArgumentCompletion, localScope);
       }
    }
   else {
      PybaseMain.logE("Module for internals completions is null");
    }

   return EMPTY_ITOKEN_ARRAY;
}


private AbstractToken[] decorateWithLocal(AbstractToken[] tokens,LocalScope localScope,
					     CompletionState state)
{
   if (localScope != null) {
      Collection<AbstractToken> interfaceForLocal = localScope.getInterfaceForLocal(state
										       .getActivationToken());
      if (interfaceForLocal != null && interfaceForLocal.size() > 0) {
	 AbstractToken[] ret = new AbstractToken[tokens.length + interfaceForLocal.size()];
	 Object[] array = interfaceForLocal.toArray();
	 System.arraycopy(array, 0, ret, 0, array.length);
	 System.arraycopy(tokens, 0, ret, array.length, tokens.length);
	 return ret;
       }
    }
   return tokens;
}

private AbstractToken[] getAssignCompletions(AbstractModule module,CompletionState state,
						boolean lookForArgumentCompletion,LocalScope localScope)
{
   AssignCompletionInfo assignCompletions = assign_analysis.getAssignCompletions(this, module, state);

   boolean useExtensions = assignCompletions.possible_completions.size() == 0;

   if (lookForArgumentCompletion && localScope != null
	  && assignCompletions.possible_completions.size() == 0
	  && assignCompletions.given_defs.length > 0) {
      // Now, if a definition found was available in the same scope we started on, let's add the
      // tokens that are available from that scope.
      for (Definition d : assignCompletions.given_defs) {
	 if (d.getModule().equals(module) && localScope.equals(d.getScope())) {
	    Collection<AbstractToken> interfaceForLocal =
	       localScope.getInterfaceForLocal(state.getActivationToken());
	    assignCompletions.possible_completions.addAll(interfaceForLocal);
	    break;
	  }
       }
    }

   if (useExtensions && localScope != null) {
      assignCompletions.possible_completions.addAll(CompletionParticipantsHelper
						       .getCompletionsForTokenWithUndefinedType(state, localScope));
    }

   return assignCompletions.possible_completions.toArray(EMPTY_ITOKEN_ARRAY);
}


public void getCompletionsForClassInLocalScope(AbstractModule module,CompletionState state,
						  boolean searchSameLevelMods,boolean lookForArgumentCompletion,
						  List<String> lookForClass,HashSet<AbstractToken> hashSet)
throws CompletionRecursionException
{
   AbstractToken[] tokens;
   // if found here, it's an instanced variable (force it and restore if we didn't find it here...)
   CompletionState stateCopy = state.getCopy();
   LookForType prevLookingFor = stateCopy.getLookingFor();
   // force looking for instance
   stateCopy.setLookingFor(LookForType.INSTANCED_VARIABLE, true);

   for (String classFound : lookForClass) {
      stateCopy.setLocalImportsGotten(false);
      stateCopy.setActivationToken(classFound);

      // same thing as the initial request, but with the class we could find...
      tokens = getCompletionsForModule(module, stateCopy, searchSameLevelMods,
					  lookForArgumentCompletion);
      if (tokens != null) {
	 for (AbstractToken tok : tokens) {
	    hashSet.add(tok);
	  }
       }
    }
   if (hashSet.size() == 0) {
      // force looking for what was set before...
      stateCopy.setLookingFor(prevLookingFor, true);
    }
}


/**
 * Attempt to search on modules on the same level as this one (this will only happen if we are in an __init__
								  * module (otherwise, the initial set will be empty)
								  *
								  * @param initial this is the set of tokens generated from modules in the same level
								  * @param state the current state of the completion
								  *
								  * @return a list of tokens found.
								  * @throws CompletionRecursionException
								  */
protected AbstractToken[] searchOnSameLevelMods(Set<AbstractToken> initial,CompletionState state)
throws CompletionRecursionException
{
   AbstractToken[] ret = null;
   Tuple<AbstractModule, ModulesManager> modUsed = null;
   String actTokUsed = null;

   for (AbstractToken token : initial) {
      // ok, maybe it was from the set that is in the same level as this one (this will
      // only happen if we are on an __init__ module)
      String rep = token.getRepresentation();

      if (state.getActivationToken().startsWith(rep)) {
	 String absoluteImport = token.getAsAbsoluteImport();
	 modUsed = modulesManager.getModuleAndRelatedModulesManager(absoluteImport,
								       state.getNature(), true, false);

	 AbstractModule sameLevelMod = null;
	 if (modUsed != null) {
	    sameLevelMod = modUsed.o1;
	  }

	 if (sameLevelMod == null) {
	    return null;
	  }

	 String qualifier = state.getActivationToken().substring(rep.length());


	 if (state.getActivationToken().equals(rep)) {
	    actTokUsed = "";
	  }
	 else if (qualifier.startsWith(".")) {
	    actTokUsed = qualifier.substring(1);
	  }

	 if (actTokUsed != null) {
	    CompletionState copy = state.getCopyWithActTok(actTokUsed);
	    copy.setBuiltinsGotten(true); // we don't want builtins...
	    ret = getCompletionsForModule(sameLevelMod, copy);
	    break;
	  }
       }
    }

   return ret;
}

public List<AbstractToken> getGlobalCompletions(AbstractToken[] globalTokens,AbstractToken[] importedModules,
						   AbstractToken[] wildImportedModules,CompletionState state,AbstractModule current)
{
   List<AbstractToken> completions = new ArrayList<AbstractToken>();

   // in completion with nothing, just go for what is imported and global tokens.
   for (int i = 0; i < globalTokens.length; i++) {
      completions.add(globalTokens[i]);
    }

   // now go for the token imports
   for (int i = 0; i < importedModules.length; i++) {
      completions.add(importedModules[i]);
    }

   if (!state.getBuiltinsGotten()) {
      state.setBuiltinsGotten(true);
      // last thing: get completions from module __builtin__
      getBuiltinCompletions(state, completions);
    }

   // wild imports: recursively go and get those completions. Must be done before getting
   // the builtins, because
   // when we do a wild import, we may get tokens that are filtered, and there's a chance
   // that the builtins get
   // filtered out if they are gotten from a wild import and not from the module itself.
   for (int i = 0; i < wildImportedModules.length; i++) {

      // for wild imports, we must get the global completions with __all__ filtered
      AbstractToken name = wildImportedModules[i];
      getCompletionsForWildImport(state, current, completions, name);
    }
   return completions;
}

/**
 * @return the builtin completions
 */
public List<AbstractToken> getBuiltinCompletions(CompletionState state,List<AbstractToken> completions)
{
   PybaseNature nature = state.getNature();
   AbstractToken[] builtinCompletions = getBuiltinComps(nature);
   if (builtinCompletions != null) {
      for (int i = 0; i < builtinCompletions.length; i++) {
	 completions.add(builtinCompletions[i]);
       }
    }
   return completions;

}


/**
 * @return the tokens in the builtins
 */
protected AbstractToken[] getBuiltinComps(PybaseNature nature)
{
   return nature.getBuiltinCompletions();
}

/**
 * @return the module that represents the builtins
 */
protected AbstractModule getBuiltinMod(PybaseNature nature)
{
   return nature.getBuiltinMod();
}

/**
 * Resolves a token defined with 'from module import something' statement
 * to a proper type, as defined in module.
 * @param imported the token to resolve.
 * @return the resolved token or the original token in case no additional information could be obtained.
 * @throws CompletionRecursionException
 */
public AbstractToken resolveImport(CompletionState state,AbstractToken imported)
throws CompletionRecursionException
{
   String curModName = imported.getParentPackage();
   Tuple3<AbstractModule, String, AbstractToken> modTok = findOnImportedMods(new AbstractToken[] { imported },
									     state.getCopyWithActTok(imported.getRepresentation()), curModName);
   if (modTok != null && modTok.o1 != null) {

      if (modTok.o2.length() == 0) {
	 return imported; // it's a module actually, so, no problems...

       }
      else {
	 try {
	    state.checkResolveImportMemory(modTok.o1, modTok.o2);
	  }
	 catch (CompletionRecursionException e) {
	    return imported;
	  }
	 AbstractToken repInModule = getRepInModule(modTok.o1, modTok.o2, state.getNature(),
						       state);
	 if (repInModule != null) {
	    return repInModule;
	  }
       }
    }
   return imported;

}

public AbstractToken getRepInModule(AbstractModule module,String tokName,PybaseNature nature)
throws CompletionRecursionException
{
   return getRepInModule(module, tokName, nature, null);
}

/**
 * Get the actual token representing the tokName in the passed module
 * @param module the module where we're looking
			       * @param tokName the name of the token we're looking for
 * @param nature the nature we're looking for
			       * @return the actual token in the module (or null if it was not possible to find it).
			       * @throws CompletionRecursionException
			       */
private AbstractToken getRepInModule(AbstractModule module,String tokName,PybaseNature nature,
					CompletionState state) throws CompletionRecursionException
{
   if (module != null) {
      if (tokName.startsWith(".")) {
	 tokName = tokName.substring(1);
       }

      // ok, we are getting some token from the module... let's see if it is really
      // available.
      String[] headAndTail = FullRepIterable.headAndTail(tokName);
      String actToken = headAndTail[0]; // tail (if os.path, it is os)
      String hasToBeFound = headAndTail[1]; // head (it is path)

      // if it was os.path:
      // initial would be os.path
      // foundAs would be os
      // actToken would be path

      // now, what we will do is try to do a code completion in os and see if path is found
      if (state == null) {
	 state = CompletionStateFactory.getEmptyCompletionState(actToken, nature,
								   new CompletionCache());
       }
      else {
	 state = state.getCopy();
	 state.setActivationToken(actToken);
       }
      AbstractToken[] completionsForModule = getCompletionsForModule(module, state);
      for (AbstractToken foundTok : completionsForModule) {
	 if (foundTok.getRepresentation().equals(hasToBeFound)) {
	    return foundTok;
	  }
       }
    }
   return null;
}


public boolean getCompletionsForWildImport(CompletionState state,AbstractModule current,
					      List<AbstractToken> completions,AbstractToken name)
{
   try {
      // this one is an exception... even though we are getting the name as a relative
      // import, we say it
      // is not because we want to get the module considering __init__
      AbstractModule mod = null;

      if (current != null) {
	 // we cannot get the relative path if we don't have a current module
	 mod = getModule(name.getAsRelativeImport(current.getName()), state.getNature(),
			    false);
       }

      if (mod == null) {
	 mod = getModule(name.getOriginalRep(), state.getNature(), false); // absolute import
       }

      if (mod != null) {
	 state.checkWildImportInMemory(current, mod);
	 AbstractToken[] completionsForModule = getCompletionsForModule(mod, state, true, false,
									   true);
	 for (AbstractToken token : completionsForModule) {
	    completions.add(token);
	  }
	 return true;
       }
      else {
	 PybaseMain.logI("Module not found:" + name.getRepresentation());
       }
    }
   catch (CompletionRecursionException e) {
      // probably found a recursion... let's return the tokens we have so far
    }
   return false;
}




public AbstractToken[] findTokensOnImportedMods(AbstractToken[] importedModules,CompletionState state,
						   AbstractModule current) throws CompletionRecursionException
{
   Tuple3<AbstractModule, String, AbstractToken> o = findOnImportedMods(importedModules, state,
									   current.getName());

   if (o == null) return null;

   AbstractModule mod = o.o1;
   String tok = o.o2;
   String tokForSearchInOtherModule = getTokToSearchInOtherModule(o);


   if (tok.length() == 0) {
      // the activation token corresponds to an imported module. We have to get its global
      // tokens and return them.
      CompletionState copy = state.getCopy();
      copy.setActivationToken("");
      copy.setBuiltinsGotten(true); // we don't want builtins...
      return getCompletionsForModule(mod, copy);
    }
   else if (mod != null) {
      CompletionState copy = state.getCopy();
      copy.setActivationToken(tokForSearchInOtherModule);
      copy.setCol(-1);
      copy.setLine(-1);
      copy.raiseNFindTokensOnImportedModsCalled(mod, tokForSearchInOtherModule);

      String parentPackage = o.o3.getParentPackage();
      if (parentPackage.trim().length() > 0 && parentPackage.equals(current.getName())
	     && state.getActivationToken().equals(tok)
	     && !parentPackage.endsWith("__init__")) {
	 String name = mod.getName();
	 if (name.endsWith(".__init__")) {
	    name = name.substring(0, name.length() - 9);
	  }
	 if (o.o3.getAsAbsoluteImport().startsWith(name)) {
	    if (current.isInDirectGlobalTokens(tok, state)) {
	       return null;
	     }
	  }
       }

      return getCompletionsForModule(mod, copy);
    }
   return null;
}



/**
 * When we have an import, we have one token which we used to find it and another which is the
 * one we refer to at the current module. This method will get the way it's referred at the
 * actual module and not at the current module (at the current module it's modTok.o2).
*/
public static String getTokToSearchInOtherModule(Tuple3<AbstractModule, String, AbstractToken> modTok)
{
   String tok = modTok.o2;
   String tokForSearchInOtherModule = tok;

   if (tok.length() > 0) {
      AbstractToken sourceToken = modTok.o3;
      if (sourceToken instanceof SourceToken) {
	 SourceToken sourceToken2 = (SourceToken) sourceToken;
	 if (sourceToken2.getAst() instanceof ImportFrom) {
	    ImportFrom importFrom = (ImportFrom) sourceToken2.getAst();
	    if (importFrom.names.length > 0 && importFrom.names[0].asname != null) {
	       String originalRep = sourceToken.getOriginalRep();
	       tokForSearchInOtherModule = FullRepIterable.getLastPart(originalRep);
	     }
	  }
       }
    }
   return tokForSearchInOtherModule;
}



/**
 * @param activation_token
 * @param importedModules
 * @param for_module
 * @return tuple with:
 * 0: mod
 * 1: tok
 * @throws CompletionRecursionException
 */
public Tuple3<AbstractModule, String, AbstractToken> findOnImportedMods(CompletionState state,
									   AbstractModule current) throws CompletionRecursionException
{
   AbstractToken[] importedModules = current.getTokenImportedModules();
   return findOnImportedMods(importedModules, state, current.getName());
}




/**
 * This function tries to find some activation token defined in some imported module.
 * @return tuple with: the module and the token that should be used from it.
 *
 * @param this is the activation token we have. It may be a single token or some dotted name.
 *
 * If it is a dotted name, such as testcase.TestCase, we need to match against some import
 * represented as testcase or testcase.TestCase.
 *
 * If a testcase.TestCase matches against some import named testcase, the import is returned and
 * the TestCase is put as the module
 *
 * 0: mod
 * 1: tok (string)
 * 2: actual tok
 * @throws CompletionRecursionException
 */
public Tuple3<AbstractModule, String, AbstractToken> findOnImportedMods(AbstractToken[] importedModules,
									   CompletionState state,String currentModuleName)
throws CompletionRecursionException
{

   FullRepIterable iterable = new FullRepIterable(state.getActivationToken(),true);
   for (String tok : iterable) {
      for (AbstractToken importedModule : importedModules) {

	 final String modRep = importedModule.getRepresentation(); // this is its 'real'
	 // representation (alias) on the file (if it is from xxx import a as yyy, it is yyy)

	 if (modRep.equals(tok)) {
	    String act = state.getActivationToken();
	    Tuple<AbstractModule, String> r = findOnImportedMods(importedModule, tok, state,
								    act, currentModuleName);
	    if (r != null) {
	       return new Tuple3<AbstractModule, String, AbstractToken>(r.o1,r.o2,importedModule);
	     }
	    // Note, if r==null, even though the name matched, keep on going (to handle cases of
	    // try..except ImportError, as we cannot be sure of which version will actually match).
	  }
       }
    }
   return null;
}


public Tuple<AbstractModule, String> findModule(String moduleToFind,String currentModule,
						   CompletionState state) throws CompletionRecursionException
{
   NameTok name = new NameTok(moduleToFind,name_contextType.ImportModule);
   Import impTok = new Import(new aliasType[] { new aliasType(name,null) });

   List<AbstractToken> tokens = new ArrayList<AbstractToken>();
   List<AbstractToken> imp = AbstractVisitor
      .makeImportToken(impTok, tokens, currentModule, true);
   AbstractToken importedModule = imp.get(imp.size() - 1); // get the last one (it's the one with the 'longest' representation).
   return findOnImportedMods(importedModule, "", state, "", currentModule);
}


/**
 * Checks if some module can be resolved and returns the module it is resolved to (and to which token).
 * @throws CompletionRecursionException
 *
 */
public Tuple<AbstractModule, String> findOnImportedMods(AbstractToken importedModule,String tok,
							   CompletionState state,String activationToken,String currentModuleName)
throws CompletionRecursionException
{


   Tuple<AbstractModule, String> modTok = null;
   AbstractModule mod = null;

   // ok, check if it is a token for the new import
   PybaseNature nature = state.getNature();
   if (importedModule instanceof SourceToken) {
      SourceToken token = (SourceToken) importedModule;

      if (token.isImportFrom()) {
	 ImportFrom importFrom = (ImportFrom) token.getAst();
	 int level = importFrom.level;
	 if (level > 0) {
	    // ok, it must be treated as a relative import
	    // ok, it is the import added on python 2.5 (from .. import xxx)

	    String parentPackage = token.getParentPackage();
	    List<String> moduleParts = StringUtils.dotSplit(parentPackage);
	    String relative = null;
	    if (moduleParts.size() > level) {
	       relative = FullRepIterable.joinParts(moduleParts, moduleParts.size() - level);
	     }

	    String modName = ((NameTok) importFrom.module).id;
	    if (modName.length() > 0) {
	       // ok, we have to add the other part too, as we have more than the leading dots
	       // from ..bar import
	       relative += "." + modName;
	     }

	    if (!AbstractVisitor.isWildImport(importFrom)) {
	       tok = FullRepIterable.getLastPart(token.getOriginalRep());
	       relative += "." + tok;
	     }

	    modTok = findModuleFromPath(relative, nature, false, null);
	    mod = modTok.o1;
	    if (checkValidity(currentModuleName, mod)) {
	       Tuple<AbstractModule, String> ret = fixTok(modTok, tok, activationToken);
	       return ret;
	     }
	    // ok, it is 'forced' as relative import because it has a level, so, it MUST return here
	    return null;
	  }
       }
    }


   // check as relative with complete rep
   String asRelativeImport = importedModule.getAsRelativeImport(currentModuleName);
   modTok = findModuleFromPath(asRelativeImport, nature, true, currentModuleName);
   mod = modTok.o1;
   if (checkValidity(currentModuleName, mod)) {
      Tuple<AbstractModule, String> ret = fixTok(modTok, tok, activationToken);
      return ret;
    }


   // check if the import actually represents some token in an __init__ file
   String originalWithoutRep = importedModule.getOriginalWithoutRep();
   if (originalWithoutRep.length() > 0) {
      if (!originalWithoutRep.endsWith("__init__")) {
	 originalWithoutRep = originalWithoutRep + ".__init__";
       }
      modTok = findModuleFromPath(originalWithoutRep, nature, true, null);
      mod = modTok.o1;
      if (modTok.o2.endsWith("__init__") == false
	     && checkValidity(currentModuleName, mod)) {
	 if (mod.isInGlobalTokens(importedModule.getRepresentation(), nature, false,
				     state)) {
	    // then this is the token we're looking for (otherwise, it might be a module).
	    Tuple<AbstractModule, String> ret = fixTok(modTok, tok, activationToken);
	    if (ret.o2.length() == 0) {
	       ret.o2 = importedModule.getRepresentation();
	     }
	    else {
	       ret.o2 = importedModule.getRepresentation() + "." + ret.o2;
	     }
	    return ret;
	  }
       }
    }


   // the most 'simple' case: check as absolute with original rep
   modTok = findModuleFromPath(importedModule.getOriginalRep(), nature, false, null);
   mod = modTok.o1;
   if (checkValidity(currentModuleName, mod)) {
      Tuple<AbstractModule, String> ret = fixTok(modTok, tok, activationToken);
      return ret;
    }


   // ok, one last shot, to see a relative looking in folders __init__
   modTok = findModuleFromPath(asRelativeImport, nature, false, null);
   mod = modTok.o1;
   if (checkValidity(currentModuleName, mod, true)) {
      Tuple<AbstractModule, String> ret = fixTok(modTok, tok, activationToken);
      // now let's see if what we did when we found it as a relative import is correct:

      // if we didn't find it in an __init__ module, all should be ok
      if (!mod.getName().endsWith("__init__")) {
	 return ret;
       }
      // otherwise, we have to be more cautious...
      // if the activation token is empty, then it is the module we were looking for
      // if it is not the initial token we were looking for, it is correct
      // if it is in the global tokens of the found module it is correct
      // if none of this situations was found, we probably just found the same token we
      // had when we started (unless I'm mistaken...)
      else if (activationToken.length() == 0 || ret.o2.equals(activationToken) == false
		  || mod.isInGlobalTokens(activationToken, nature, false, state)) {
	 return ret;
       }
    }

   return null;
}


protected boolean checkValidity(String currentModuleName,AbstractModule mod)
{
   return checkValidity(currentModuleName, mod, false);
}


/**
 * @param isRelative: On a relative import we have to check some more conditions...
 */
protected boolean checkValidity(String currentModuleName,AbstractModule mod,boolean isRelative)
{
   if (mod == null) {
      return false;
    }

   String modName = mod.getName();
   if (modName == null) {
      return true;
    }

   // still in the same module
   if (modName.equals(currentModuleName)) {
      return false;
    }

   if (isRelative && currentModuleName != null && modName.endsWith(".__init__")) {
      // we have to check it without the __init__

      // what happens here is that considering the structure:
      //
      // xxx.__init__
      // xxx.mod1
      //
      // we cannot have tokens from the mod1 getting __init__

      String withoutLastPart = FullRepIterable.getWithoutLastPart(modName);
      String currentWithoutLastPart = FullRepIterable
	 .getWithoutLastPart(currentModuleName);
      if (currentWithoutLastPart.equals(withoutLastPart)) {
	 return false;
       }
    }
   return true;
}


/**
 * Fixes the token if we found a module that was just a substring from the initial activation token.
 *
 * This means that if we had testcase.TestCase and found it as TestCase, the token is added with TestCase
 */
protected Tuple<AbstractModule, String> fixTok(Tuple<AbstractModule, String> modTok,String tok,
						  String activationToken)
{
   if (activationToken.length() > tok.length() && activationToken.startsWith(tok)) {
      String toAdd = activationToken.substring(tok.length() + 1);
      if (modTok.o2.length() == 0) {
	 modTok.o2 = toAdd;
       }
      else {
	 modTok.o2 += "." + toAdd;
       }
    }
   return modTok;
}


/**
 * This function receives a path (rep) and extracts a module from that path.
 * First it tries with the full path, and them removes a part of the final of
 * that path until it finds the module or the path is empty.
 *
 * @param currentModuleName this is the module name (used to check validity for relative imports) -- not used if dontSearchInit is false
 * if this parameter is not null, it means we're looking for a relative import. When checking for relative imports,
						* we should only check the modules that are directly under this project (so, we should not check the whole pythonpath for
															    * it, just direct modules)
						*
						* @return tuple with found module and the String removed from the path in
						* order to find the module.
						*/
public Tuple<AbstractModule, String> findModuleFromPath(String rep,PybaseNature nature,
							   boolean dontSearchInit,String currentModuleName)
{
   String tok = "";
   boolean lookingForRelative = currentModuleName != null;
   AbstractModule mod = getModule(rep, nature, dontSearchInit, lookingForRelative);
   String mRep = rep;
   int index;
   while (mod == null && (index = mRep.lastIndexOf('.')) != -1) {
      tok = mRep.substring(index + 1) + "." + tok;
      mRep = mRep.substring(0, index);
      if (mRep.length() > 0) {
	 mod = getModule(mRep, nature, dontSearchInit, lookingForRelative);
       }
    }
   if (tok.endsWith(".")) {
      tok = tok.substring(0, tok.length() - 1); // remove last point if found.
    }

   if (dontSearchInit && currentModuleName != null && mod != null) {
      String parentModule = FullRepIterable.getParentModule(currentModuleName);
      // if we are looking for some relative import token, it can only match if the name
      // found is not less than the parent
      // of the current module because of the way in that relative imports are meant to be written.

      // if it equal, it should not match either, as it was found as the parent module...
      // this can not happen because it must find
      // it with __init__ if it was the parent module
      if (mod.getName().length() <= parentModule.length()) {
	 return new Tuple<AbstractModule, String>(null,null);
       }
    }
   return new Tuple<AbstractModule, String>(mod,tok);
}



public abstract void changePythonPath(String path,PybaseProject p);
public abstract void saveToFile(File f);



/********************************************************************************/
/*										*/
/*	ImportInfo structure							*/
/*										*/
/********************************************************************************/

public class ImportInfo {

   public boolean hasFromSubstring;
   public boolean hasImportSubstring;
   public String importsTipperStr;

   ImportInfo(String s,boolean imp,boolean from) {
      hasFromSubstring = from;
      hasImportSubstring = imp;
      importsTipperStr = s;
    }

}	// end of inner class ImportInfo



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private static int countChars(char c, String line)
{
   int ret = 0;
   int len = line.length();
   for (int i = 0; i < len; i++) {
      if(line.charAt(i) == c){
	 ret += 1;
       }
    }
   return ret;
}




} // end of class AbstractASTManager


/* end of AbstractASTManager.java */
