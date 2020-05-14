/********************************************************************************/
/*										*/
/*		ImportChecker.java						*/
/*										*/
/*	Python Bubbles Base import checking code				*/
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
 * Created on 21/08/2005
 */


package edu.brown.cs.bubbles.pybase.symbols;


import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseSemanticVisitor;

import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.structure.CompletionRecursionException;


/**
 * The import checker not only generates information on errors for unresolved modules, but also gathers
 * dependency information so that we can do incremental building of dependent modules.
 *
 * @author Fabio
 */

public final class ImportChecker {



/********************************************************************************/
/*										*/
/*	Import information holder						*/
/*										*/
/********************************************************************************/

public static class ImportInfo {

   private AbstractModule import_module;
   private AbstractToken  import_token;
   private String  import_rep;
   private boolean was_resolved;

   public ImportInfo(AbstractModule mod,String rep,AbstractToken token,boolean wasResolved) {
      import_module = mod;
      import_rep = rep;
      import_token = token;
      was_resolved = wasResolved;
    }

   public AbstractModule getModule() { return import_module; }
   public AbstractToken getToken() { return import_token; }
   public String getRepresentation() { return import_rep; }
   public boolean wasResolved() { return was_resolved; }

   @Override public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("ImportInfo(");
      buffer.append(" Resolved:");
      buffer.append(was_resolved);
      if (was_resolved) {
	 buffer.append(" Rep:");
	 buffer.append(import_rep);
	 buffer.append(" Mod:");
	 buffer.append(import_module.getName());
       }
      buffer.append(")");
      return buffer.toString();
    }

   public Definition getModuleDefinitionFromImportInfo(PybaseNature nature,
							  CompletionCache completionCache) {
      try {
	 Definition[] definitions = import_module.findDefinition(
	    CompletionStateFactory.getEmptyCompletionState(import_rep, nature,
							      completionCache), -1, -1, nature);

	 for (Definition d : definitions) {
	    if (d.getModule() != null && d.getValueName().length() == 0 && d.getAssignment() == null) {
	       return d;
	    }
	  }
       }
      catch (Exception e) {
	 throw new RuntimeException(e);
       }
      return null;
    }

}	// end of public inner class ImportInfo



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybaseNature the_nature;
private String	module_name;
private PybaseSemanticVisitor the_visitor;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ImportChecker(PybaseSemanticVisitor visitor,PybaseNature nature,String modulename)
{
   the_nature = nature;
   module_name = modulename;
   the_visitor = visitor;
}



/********************************************************************************/
/*										*/
/*	Visitation entry points 						*/
/*										*/
/********************************************************************************/

/**
 * @param import_token MUST be an import import_token
 * @param reportUndefinedImports
 *
 * @return the module where the import_token was found and a String representing the way it was found
 * in the module.
 *
 * Note: it may return information even if the import_token was not found in the representation required. This is useful
 * to get dependency info, because it is actually dependent on the module, event though it does not have the
 * import_token we were looking for.
 */

public ImportInfo visitImportToken(AbstractToken token,boolean reportUndefinedImports,
				      ICompletionCache completionCache)
{
   // try to find it as a relative import
   boolean wasResolved = false;
   Tuple3<AbstractModule, String, AbstractToken> modTok = null;
   String checkForToken = "";
   if (token instanceof SourceToken) {
      AbstractASTManager astManager = the_nature.getAstManager();
      CompletionState state = CompletionStateFactory.getEmptyCompletionState(
	 token.getRepresentation(), the_nature, completionCache);

      try {
	 modTok = astManager.findOnImportedMods(new AbstractToken[] { token }, state, module_name);
       }
      catch (CompletionRecursionException e1) {
	 modTok = null;// unable to resolve it
       }
      if (modTok != null && modTok.o1 != null) {
	 checkForToken = modTok.o2;
	 if (modTok.o2.length() == 0) {
	    wasResolved = true;
	  }
	 else {
	    try {
	       checkForToken = AbstractASTManager.getTokToSearchInOtherModule(modTok);
	       if (astManager.getRepInModule(modTok.o1, checkForToken, the_nature) != null) {
		  wasResolved = true;
		}
	     }
	    catch (CompletionRecursionException e) {
	       // not resolved...
	     }
	  }
       }


      // if it got here, it was not resolved
      if (!wasResolved && reportUndefinedImports) {
	 String or = token.getOriginalRep();
	 String mr = token.getParentPackage();
	 if (mr != null && or != null && mr.equals(or)) {
	    // handle import self
	 }
	 else the_visitor.onAddUnresolvedImport(token);
       }

    }

   // might still return a modTok, even if the import_token we were looking for was not
   // found.
   if (modTok != null) {
      return new ImportInfo(modTok.o1,checkForToken,modTok.o3,wasResolved);
    }
   else {
      return new ImportInfo(null,null,null,wasResolved);
    }
}




}	// end of class ImportChecker




/* end of ImportChecker.java */
