/********************************************************************************/
/*										*/
/*		AbstractModule.java						*/
/*										*/
/*	Python Bubbles Base base module representation				*/
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
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseParser;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.TupleN;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Fabio Zadrozny
 */

public abstract class AbstractModule implements PybaseConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

protected String module_name;

private static final AbstractToken[] EMPTY_TOKEN_ARRAY		 = new AbstractToken[0];
private static String	  MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "";



/********************************************************************************/
/*										*/
/*	Constructors and creation methods					*/
/*										*/
/********************************************************************************/

protected AbstractModule(String name)
{
   module_name = name;
}


/**
 * This method creates a source module from a file.
 *
 * @param f
 * @return
 * @throws IOException
 */
public static AbstractModule createModule(String name,File f,PybaseNature nature,
					     int currLine) throws IOException
{
   String path = PybaseFileSystem.getFileAbsolutePath(f);
   if (PythonPathHelper.isValidFileMod(path)) {
      if (PythonPathHelper.isValidSourceFile(path)) {
	 return createModuleFromDoc(name, f, PybaseFileSystem.getDocFromFile(f), nature, currLine);

       }
      else { // this should be a compiled extension... we have to get completions from the
	 // python shell.
	 return new CompiledModule(name,nature.getAstManager().getModulesManager());
       }
    }

   // if we are here, return null...
   return null;
}


public static AbstractModule createModuleFromDoc(String name,File f,IDocument doc,
						    PybaseNature nature,int currLine)
{
   return createModuleFromDoc(name, f, doc, nature, currLine, true);
}

/**
 * This function creates the module given that you have a document (that will be parsed)
 */
public static AbstractModule createModuleFromDoc(String name,File f,IDocument doc,
						    PybaseNature nature,int currLine,boolean checkForPath)
{
   // for doc, we are only interested in python files.

   if (f != null) {
      if (!checkForPath || PythonPathHelper.isValidSourceFile(PybaseFileSystem.getFileAbsolutePath(f))) {
	 PybaseParser pp = new PybaseParser(nature.getProject(),doc);
	 ISemanticData sd = pp.parseDocument(false);
	 return new SourceModule(name,f,sd.getRootNode(),sd.getMessages());
	 //
	 // Tuple<SimpleNode, Throwable> obj = PyParser
	 // .reparseDocument(new PyParser.ParserInfo(doc,true,nature,currLine,name,
	 // f));
	 // return new SourceModule(name,f,obj.o1,obj.o2);
       }
    }
   else {
      PybaseParser pp = new PybaseParser(nature.getProject(),doc);
      ISemanticData sd = pp.parseDocument(false);
      return new SourceModule(name,f,sd.getRootNode(),sd.getMessages());
      // Tuple<SimpleNode, Throwable> obj = PyParser
      // .reparseDocument(new PyParser.ParserInfo(doc,true,nature,currLine,name,f));
      // return new SourceModule(name,f,obj.o1,obj.o2);
    }
   return null;
}

/**
 * This function creates a module and resolves the module name (use this function if only the file is available).
 */
public static AbstractModule createModuleFromDoc(File file,IDocument doc,
						    PybaseNature pythonNature,int line,ModulesManager projModulesManager)
{
   String moduleName = null;
   if (file != null) {
      moduleName = projModulesManager.resolveModule(PybaseFileSystem.getFileAbsolutePath(file));
    }
   if (moduleName == null) {
      moduleName = MODULE_NAME_WHEN_FILE_IS_UNDEFINED;
    }
   AbstractModule module = createModuleFromDoc(moduleName, file, doc, pythonNature, line, false);
   return module;
}


/**
 * Creates a source file generated only from an ast.
 * @param n the ast root
 * @return the module
 */
public static AbstractModule createModule(SimpleNode n)
{
   return new SourceModule(null,null,n,null);
}

/**
 * Creates a source file generated only from an ast.
 *
 * @param n the ast root
 * @param file the module file
 * @param moduleName the name of the module
 *
 * @return the module
 */
public static AbstractModule createModule(SimpleNode n,File file,String moduleName)
{
   return new SourceModule(moduleName,file,n,null);
}


/**
 * @return an empty module representing the key passed.
 */
public static AbstractModule createEmptyModule(ModulesKey key)
{
   if (key instanceof ModulesKeyForZip) {
      ModulesKeyForZip e = ((ModulesKeyForZip) key);
      return new EmptyModuleForZip(key.getModuleName(),key.getModuleFile(),e.getZipModulePath(),e.isFile());

    }
   else {
      return new EmptyModule(key.getModuleName(),key.getModuleFile());
    }
}




public abstract AbstractToken[] getWildImportedModules();

public abstract File getFile();

public abstract AbstractToken[] getTokenImportedModules();

public abstract AbstractToken[] getGlobalTokens();

/**
 * Don't deal with zip files unless specifically specified
	  */
public String getZipFilePath()
{
   return null;
}

public AbstractToken[] getLocalTokens(int line,int col,LocalScope scope)
{
   return EMPTY_TOKEN_ARRAY;
}

/**
 * Checks if it is in the global tokens that were created in this module
 * @param tok the token we are looking for
 * @return true if it was found and false otherwise
 */
public abstract boolean isInDirectGlobalTokens(String tok,ICompletionCache completionCache);

public boolean isInGlobalTokens(String tok,PybaseNature nature,
				   ICompletionCache completionCache) throws CompletionRecursionException
{
   return isInGlobalTokens(tok, nature, true, completionCache);
}

public boolean isInGlobalTokens(String tok,PybaseNature nature,
				   boolean searchSameLevelMods,ICompletionCache completionCache)
throws CompletionRecursionException
{
   return isInGlobalTokens(tok, nature, searchSameLevelMods, false, completionCache) != FoundType.NOT_FOUND;
}

@SuppressWarnings("unchecked")
public FoundType isInGlobalTokens(String tok,
			       PybaseNature nature,boolean searchSameLevelMods,
			       boolean ifHasGetAttributeConsiderInTokens,ICompletionCache completionCache)
	throws CompletionRecursionException
{

   // it's worth checking it if it is not dotted... (much faster as it's in a map already)
   if (tok.indexOf(".") == -1) {
      if (isInDirectGlobalTokens(tok, completionCache)) {
	 return FoundType.FOUND_TOKEN;
       }
    }

   String[] headAndTail = FullRepIterable.headAndTail(tok);
   String head = headAndTail[1];


   // now, check if it's cached in a way we can use it (we cache it not as raw tokens, but
   // as representation --> token)
   // to help in later searches.
   String name = getName();
   Object key = new TupleN("isInGlobalTokens",name != null ? name : "",tok, searchSameLevelMods);
   Map<String, AbstractToken> cachedTokens = (Map<String, AbstractToken>) completionCache.getObj(key);

   if (cachedTokens == null) {
      cachedTokens = internalGenerateCachedTokens(nature, completionCache,
						     headAndTail[0], searchSameLevelMods);
      completionCache.add(key, cachedTokens);
    }

   if (cachedTokens.containsKey(head)) {
      return FoundType.FOUND_TOKEN;
    }

   if (ifHasGetAttributeConsiderInTokens) {
      AbstractToken token = cachedTokens.get("__getattribute__");
      if (token == null || isTokenFromBuiltins(token)) {
	 token = cachedTokens.get("__getattr__");
       }
      if (token != null && !isTokenFromBuiltins(token)) {
	 return FoundType.FOUND_BECAUSE_OF_GETATTR;
       }
    }

   return FoundType.NOT_FOUND;
}

private boolean isTokenFromBuiltins(AbstractToken token)
{
   String parentPackage = token.getParentPackage();
   return parentPackage.equals("__builtin__") || parentPackage.startsWith("__builtin__.")
      || parentPackage.equals("builtins") || parentPackage.startsWith("builtins.");
}

/**
      * Generates the cached tokens in the needed structure for a 'fast' search given a token representation
      * (creates a map with the name of the token --> token).
      */
private Map<String, AbstractToken> internalGenerateCachedTokens(PybaseNature nature,
								   ICompletionCache completionCache,String activationToken,
								   boolean searchSameLevelMods) throws CompletionRecursionException
{

   Map<String, AbstractToken> cachedTokens = new HashMap<String, AbstractToken>();

   // if still not found, we have to get all the tokens, including regular and wild
   // imports
   CompletionState state = CompletionStateFactory.getEmptyCompletionState(nature,
									     completionCache);
   AbstractASTManager astManager = nature.getAstManager();
   state.setActivationToken(activationToken);

   // we don't want to gather builtins in this case.
   state.setBuiltinsGotten(true);
   AbstractToken[] globalTokens = astManager.getCompletionsForModule(this, state,
									searchSameLevelMods);
   for (AbstractToken token : globalTokens) {
      String rep = token.getRepresentation();
      AbstractToken t = cachedTokens.get(rep);
      if (t != null) {
	 // only override tokens if it's a getattr that's not defined in the builtin
	 // module
	 if (rep.equals("__getattribute__") || rep.equals("__getattr__")) {
	    if (!isTokenFromBuiltins(token)) {
	       cachedTokens.put(rep, token);
	     }
	  }
       }
      else {
	 cachedTokens.put(rep, token);
       }
    }
   return cachedTokens;
}

/********************************************************************************/
/*										*/
/*	Abstract methods							*/
/*										*/
/********************************************************************************/

public abstract Definition[] findDefinition(CompletionState state,int line,int col,
					       PybaseNature nature) throws Exception;

public abstract AbstractToken[] getGlobalTokens(CompletionState state, AbstractASTManager manager);

public abstract String getDocString();




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName()
{
   return module_name;
}


LocalScope getLocalScope(int line,int col)
{
   return null;
}



@Override public String toString()
{
   String n2 = getClass().getName();
   String n = n2.substring(n2.lastIndexOf('.') + 1);
   return getName() + " (" + n + ")";
}


/**
 * @return true if the name we have ends with .__init__ (default for packages -- others are modules)
 */
boolean isPackage()
{
   return module_name != null && module_name.endsWith(".__init__");
}



String getPackageFolderName()
{
   return FullRepIterable.getParentModule(module_name);
}



} // end of class AbstractModule


/* end of AbstractModule.java */
