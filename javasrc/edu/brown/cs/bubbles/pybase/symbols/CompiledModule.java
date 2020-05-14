/********************************************************************************/
/*										*/
/*		CompiledModule.java						*/
/*										*/
/*	Python Bubbles Base compiled module representation			*/
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
import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseNature;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.cache.LRUCache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author Fabio Zadrozny
 */
public class CompiledModule extends AbstractModule implements PybaseConstants {



/********************************************************************************/
/*										*/
/*	Constant definitions							*/
/*										*/
/********************************************************************************/

public static boolean		    COMPILED_MODULES_ENABLED = true;

public static final boolean	      TRACE_COMPILED_MODULES   = false;

private static final Definition[]	EMPTY_DEFINITION	 = new Definition[0];

private static final Map<String, String> BUILTIN_REPLACEMENTS	  = new HashMap<String, String>();
static {
   BUILTIN_REPLACEMENTS.put("open", "file");
   BUILTIN_REPLACEMENTS.put("dir", "list");
   BUILTIN_REPLACEMENTS.put("filter", "list");
   BUILTIN_REPLACEMENTS.put("raw_input", "str");
   BUILTIN_REPLACEMENTS.put("input", "str");
   BUILTIN_REPLACEMENTS.put("locals", "dict");
   BUILTIN_REPLACEMENTS.put("map", "list");
   BUILTIN_REPLACEMENTS.put("range", "list");
   BUILTIN_REPLACEMENTS.put("repr", "str");
   BUILTIN_REPLACEMENTS.put("reversed", "list");
   BUILTIN_REPLACEMENTS.put("sorted", "list");
   BUILTIN_REPLACEMENTS.put("zip", "list");

   BUILTIN_REPLACEMENTS.put("str.capitalize", "str");
   BUILTIN_REPLACEMENTS.put("str.center", "str");
   BUILTIN_REPLACEMENTS.put("str.decode", "str");
   BUILTIN_REPLACEMENTS.put("str.encode", "str");
   BUILTIN_REPLACEMENTS.put("str.expandtabs", "str");
   BUILTIN_REPLACEMENTS.put("str.format", "str");
   BUILTIN_REPLACEMENTS.put("str.join", "str");
   BUILTIN_REPLACEMENTS.put("str.ljust", "str");
   BUILTIN_REPLACEMENTS.put("str.lower", "str");
   BUILTIN_REPLACEMENTS.put("str.lstrip", "str");
   BUILTIN_REPLACEMENTS.put("str.partition", "tuple");
   BUILTIN_REPLACEMENTS.put("str.replace", "str");
   BUILTIN_REPLACEMENTS.put("str.rjust", "str");
   BUILTIN_REPLACEMENTS.put("str.rpartition", "tuple");
   BUILTIN_REPLACEMENTS.put("str.rsplit", "list");
   BUILTIN_REPLACEMENTS.put("str.rstrip", "str");
   BUILTIN_REPLACEMENTS.put("str.split", "list");
   BUILTIN_REPLACEMENTS.put("str.splitlines", "list");
   BUILTIN_REPLACEMENTS.put("str.strip", "str");
   BUILTIN_REPLACEMENTS.put("str.swapcase", "str");
   BUILTIN_REPLACEMENTS.put("str.title", "str");
   BUILTIN_REPLACEMENTS.put("str.translate", "str");
   BUILTIN_REPLACEMENTS.put("str.upper", "str");
   BUILTIN_REPLACEMENTS.put("str.zfill", "str");
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String, Map<String, AbstractToken>> module_cache = new HashMap<String, Map<String, AbstractToken>>();

/**
 * These are the tokens the compiled module has.
 */
private Map<String, AbstractToken> module_tokens = null;

/**
 * A map with the definitions that have already been found for this compiled module.
 */
private LRUCache<String, Definition[]>	 definitions_found_cache = new LRUCache<String, Definition[]>( 30);

private File		module_file;

private final boolean	is_python_builtin;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CompiledModule(String name,ModulesManager manager)
{
   this(name,TokenType.BUILTIN,manager);
}



private CompiledModule(String name,TokenType tokentypes,ModulesManager manager)
{
   super(name);
   is_python_builtin = ("__builtin__".equals(name) || "builtins".equals(name));
   if (COMPILED_MODULES_ENABLED) {
      try {
	 setTokens(name, manager);
       }
      catch (Exception e) {
	 // ok, something went wrong... let's give it another shot...
	 synchronized (this) {
	    try {
	       wait(10);
	     }
	    catch (InterruptedException e1) {
	       // empty block
	     } // just wait a little before a retry...
	  }

	 try {
	    AbstractShell shell = AbstractShell.getServerShell(manager.getNature(),
								  AbstractShell.COMPLETION_SHELL);
	    synchronized (shell) {
	       shell.clearSocket();
	     }
	    setTokens(name, manager);
	  }
	 catch (Exception e2) {
	    module_tokens = new HashMap<String, AbstractToken>();
	    PybaseMain.logE("Problem creating compiled module",e2);
	  }
       }
    }
   else {
      // not used if not enabled.
      module_tokens = new HashMap<String, AbstractToken>();
    }
}




/********************************************************************************/
/*										*/
/*	Set up methods								*/
/*										*/
/********************************************************************************/

private void setTokens(String name,ModulesManager manager) throws IOException, Exception, PybaseException
{
   if (TRACE_COMPILED_MODULES) {
      PybaseMain.logI("Compiled modules: getting infor for " + name);
    }
   final PybaseNature nature = manager.getNature();
   AbstractShell shell = AbstractShell.getServerShell(nature,
							 AbstractShell.COMPLETION_SHELL);
   synchronized (shell) {
      Tuple<String, List<String[]>> completions = shell.getImportCompletions(
	 name,
	 manager.getCompletePythonPath(nature.getProjectInterpreter(),
					  nature.getRelatedInterpreterManager())); // default

      if (TRACE_COMPILED_MODULES) {
	 PybaseMain.logI("Compiled modules: " + name + " file: " + completions.o1
			    + " found: " + completions.o2.size() + " completions.");
       }
      String fPath = completions.o1;
      if (fPath != null) {
	 if (!fPath.equals("None")) {
	    module_file = new File(fPath);
	  }

	 String f = fPath;
	 if (f.toLowerCase().endsWith(".pyc")) {
	    f = f.substring(0, f.length() - 1); // remove the c from pyc
	    File f2 = new File(f);
	    if (f2.exists()) {
	       module_file = f2;
	     }
	  }
       }
      ArrayList<AbstractToken> array = new ArrayList<AbstractToken>();

      for (String[] element : completions.o2) {
	 // let's make this less error-prone.
	 try {
	    String o1 = element[0]; // this one is really, really needed
	    String o2 = "";
	    String o3 = "";

	    if (element.length > 0) {
	       o2 = element[1];
	     }

	    if (element.length > 0) {
	       o3 = element[2];
	     }

	    AbstractToken t;
	    if (element.length > 0) {
	       t = new CompiledToken(o1,o2,o3,name,convertToTokenType(element[3]));
	     }
	    else {
	       t = new CompiledToken(o1,o2,o3,name,TokenType.BUILTIN);
	     }

	    array.add(t);
	  }
	 catch (Exception e) {
	    String received = "";
	    for (int i = 0; i < element.length; i++) {
	       received += element[i];
	       received += "  ";
	     }

	    PybaseMain.logE("Error getting completions for compiled module "
			       + name + " received = '" + received + "'", e);
	  }
       }

      // as we will use it for code completion on sources that map to modules, the
      // __file__ should also be added...
      if (array.size() > 0 && (name.equals("__builtin__") || name.equals("builtins"))) {
	 array.add(new CompiledToken("__file__","","",name,TokenType.BUILTIN));
	 array.add(new CompiledToken("__name__","","",name,TokenType.BUILTIN));
	 array.add(new CompiledToken("__builtins__","","",name,TokenType.BUILTIN));
	 array.add(new CompiledToken("__dict__","","",name,TokenType.BUILTIN));
       }

      addTokens(array);
    }
}


private TokenType convertToTokenType(String s)
{
   switch (Integer.parseInt(s)) {
      default :
      case -1 :
	 return TokenType.UNKNOWN;
      case 0 :
	 return TokenType.IMPORT;
      case 1 :
	 return TokenType.CLASS;
      case 2 :
	 return TokenType.FUNCTION;
      case 3 :
	 return TokenType.ATTR;
      case 4 :
	 return TokenType.BUILTIN;
      case 5 :
	 return TokenType.PARAM;
      case 6 :
	 return TokenType.PACKAGE;
      case 7 :
	 return TokenType.RELATIVE_IMPORT;
      case 8 :
	 return TokenType.EPYDOC;
      case 9 :
	 return TokenType.LOCAL;
      case 10 :
	 return TokenType.OBJECT_FOUND_INTERFACE;
    }
}



/**
 * Adds module_tokens to the internal HashMap
 *
 * @param array The array of module_tokens to be added (maps representation -> token), so, existing module_tokens with the
 * same representation will be replaced.
 */
public synchronized void addTokens(List<AbstractToken> array)
{
   if (module_tokens == null) {
      module_tokens = new HashMap<String, AbstractToken>();
    }

   for (AbstractToken token : array) {
      module_tokens.put(token.getRepresentation(), token);
    }
}


/**
 * Compiled modules do not have imports to be seen
 */
@Override public AbstractToken[] getWildImportedModules()
{
   return new AbstractToken[0];
}

/**
 * Compiled modules do not have imports to be seen
 */
@Override public AbstractToken[] getTokenImportedModules()
{
   return new AbstractToken[0];
}



@Override public AbstractToken[] getGlobalTokens()
{
   if (module_tokens == null) {
      return new AbstractToken[0];
    }

   Collection<AbstractToken> values = module_tokens.values();
   return values.toArray(new AbstractToken[values.size()]);
}



@Override public String getDocString()
{
   return "compiled extension";
}


@Override public File getFile()
{
   return module_file;
}



@Override public AbstractToken[] getGlobalTokens(CompletionState state,AbstractASTManager manager)
{
   String activationToken = state.getActivationToken();
   if (activationToken.length() == 0) {
      return getGlobalTokens();
    }

   Map<String, AbstractToken> v = module_cache.get(activationToken);
   if (v != null) {
      Collection<AbstractToken> values = v.values();
      return values.toArray(new AbstractToken[values.size()]);
    }

   AbstractToken[] toks = new AbstractToken[0];

   if (COMPILED_MODULES_ENABLED) {
      try {
	 final PybaseNature nature = manager.getNature();

	 final AbstractShell shell;
	 try {
	    shell = AbstractShell.getServerShell(nature, AbstractShell.COMPLETION_SHELL);
	  }
	 catch (Exception e) {
	    throw new RuntimeException("Unable to create shell for CompiledModule: "
					  + module_name,e);
	  }
	 synchronized (shell) {
	    String act = module_name + '.' + activationToken;
	    String tokenToCompletion = act;
	    if (is_python_builtin) {
	       String replacement = BUILTIN_REPLACEMENTS.get(activationToken);
	       if (replacement != null) {
		  tokenToCompletion = module_name + '.' + replacement;
		}
	     }

	    List<String[]> completions = shell.getImportCompletions(
	       tokenToCompletion,
	       manager.getModulesManager().getCompletePythonPath(
		  nature.getProjectInterpreter(),
		     nature.getRelatedInterpreterManager())).o2;

	    ArrayList<AbstractToken> array = new ArrayList<AbstractToken>();

	    for (Iterator<String[]> iter = completions.iterator(); iter.hasNext();) {
	       String[] element = iter.next();
	       if (element.length >= 4) {// it might be a server error
		  AbstractToken t = new CompiledToken(element[0],element[1],element[2],act,
							 convertToTokenType(element[3]));
		  array.add(t);
		}

	     }
	    toks = array.toArray(new CompiledToken[0]);
	    HashMap<String, AbstractToken> map = new HashMap<String, AbstractToken>();
	    for (AbstractToken token : toks) {
	       map.put(token.getRepresentation(), token);
	     }
	    module_cache.put(activationToken, map);
	  }
       }
      catch (Exception e) {
	 PybaseMain.logE("Error while getting info for module:" + module_name + ". Project: "
			    + manager.getNature().getProject(), e);
       }
    }
   return toks;
}

@Override public boolean isInDirectGlobalTokens(String tok,
						   ICompletionCache completionCache)
{
   if (module_tokens != null) {
      return module_tokens.containsKey(tok);
    }
   return false;
}

@Override public boolean isInGlobalTokens(String tok,PybaseNature nature,
					     ICompletionCache completionCache)
{
   // we have to override because there is no way to check if it is in some import from
   // some other place if it has dots on the tok...


   if (tok.indexOf('.') == -1) {
      return isInDirectGlobalTokens(tok, completionCache);
    }
   else {
      CompletionState state = CompletionStateFactory.getEmptyCompletionState(nature,
										completionCache);
      String[] headAndTail = FullRepIterable.headAndTail(tok);
      state.setActivationToken(headAndTail[0]);
      String head = headAndTail[1];
      AbstractToken[] globalTokens = getGlobalTokens(state, nature.getAstManager());
      for (AbstractToken token : globalTokens) {
	 if (token.getRepresentation().equals(head)) {
	    return true;
	  }
       }
    }
   return false;

}

/**
 * @param findInfo
 */
@Override public Definition[] findDefinition(CompletionState state,int line,int col,
				      PybaseNature nature) throws Exception
{
   String token = state.getActivationToken();

   if (TRACE_COMPILED_MODULES) {
      System.out.println("CompiledModule.findDefinition:" + token);
    }
   Definition[] found = definitions_found_cache.getObj(token);
   if (found != null) {
      if (TRACE_COMPILED_MODULES) {
	 System.out.println("CompiledModule.findDefinition: found in module_cache.");
       }
      return found;
    }


   AbstractShell shell = AbstractShell.getServerShell(nature,
							 AbstractShell.COMPLETION_SHELL);
   synchronized (shell) {
      Tuple<String[], int[]> def = shell.getLineCol(
	 module_name,
	 token,
	 nature.getAstManager()
	 .getModulesManager()
	 .getCompletePythonPath(nature.getProjectInterpreter(),
				   nature.getRelatedInterpreterManager())); // default
      if (def == null) {
	 if (TRACE_COMPILED_MODULES) {
	    System.out.println("CompiledModule.findDefinition:" + token + " = empty");
	  }
	 definitions_found_cache.add(token, EMPTY_DEFINITION);
	 return EMPTY_DEFINITION;
       }
      String fPath = def.o1[0];
      if (fPath.equals("None")) {
	 if (TRACE_COMPILED_MODULES) {
	    System.out.println("CompiledModule.findDefinition:" + token + " = None");
	  }
	 Definition[] definition = new Definition[] { new Definition(def.o2[0],def.o2[1],
									token,null,null,this) };
	 definitions_found_cache.add(token, definition);
	 return definition;
       }
      File f = new File(fPath);
      String foundModName = nature.resolveModule(f);
      String foundAs = def.o1[1];

      AbstractModule mod;
      if (foundModName == null) {
	 // this can happen in a case where we have a definition that's found from a
	 // compiled file which actually
	 // maps to a file that's outside of the pythonpath known by Pydev.
	 String n = FullRepIterable.getFirstPart(f.getName());
	 mod = AbstractModule.createModule(n, f, nature, -1);
       }
      else {
	 mod = nature.getAstManager().getModule(foundModName, nature, true);
       }

      if (TRACE_COMPILED_MODULES) {
	 System.out.println("CompiledModule.findDefinition: found at:" + mod.getName());
       }
      int foundLine = def.o2[0];
      if (foundLine == 0 && foundAs != null && foundAs.length() > 0 && mod != null
	     && state.canStillCheckFindSourceFromCompiled(mod, foundAs)) {
	 // TODO: The nature (and so the grammar to be used) must be defined by the file
	 // we'll parse
	 // (so, we need to know the system modules manager that actually created it to
	 // know the actual nature)
	 AbstractModule sourceMod = AbstractModule.createModuleFromDoc(mod.getName(), f,
									  new Document(PybaseFileSystem.getFileContents(f)), nature, 0);
	 if (sourceMod instanceof SourceModule) {
	    Definition[] definitions = sourceMod.findDefinition(
	       state.getCopyWithActTok(foundAs), -1, -1, nature);
	    if (definitions.length > 0) {
	       definitions_found_cache.add(token, definitions);
	       return definitions;
	     }
	  }
       }
      if (mod == null) {
	 mod = this;
       }
      int foundCol = def.o2[1];
      if (foundCol < 0) {
	 foundCol = 0;
       }
      if (TRACE_COMPILED_MODULES) {
	 System.out.println("CompiledModule.findDefinition: found compiled at:"
			       + mod.getName());
       }
      Definition[] definitions = new Definition[] { new Definition(foundLine + 1,
								      foundCol + 1,token,null,null,mod) };
      definitions_found_cache.add(token, definitions);
      return definitions;
    }
}



} // end of class CompiledModule


/* end of CompiledModule.java */
