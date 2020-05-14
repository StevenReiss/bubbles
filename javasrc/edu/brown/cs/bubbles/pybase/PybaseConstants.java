/********************************************************************************/
/*										*/
/*		PybaseConstants.java						*/
/*										*/
/*	Python Bubbles Base Interface constants 				*/
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


/* SVN: $Id$ */


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.Found;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;



public interface PybaseConstants
{



/********************************************************************************/
/*										*/
/*	Messaging definitions							*/
/*										*/
/********************************************************************************/

// Must Match BumpConstants.BUMP_MINT_NAME
String	PYBASE_MINT_NAME = "BUBBLES_" + System.getProperty("user.name").replace(" ","_");



/********************************************************************************/
/*										*/
/*	Logging constants							*/
/*										*/
/********************************************************************************/

enum PybaseLogLevel {
   NONE,
   ERROR,
   WARNING,
   INFO,
   DEBUG
}


/********************************************************************************/
/*										*/
/*	Interpreter information 						*/
/*										*/
/********************************************************************************/

enum PybaseVersion {
   DEFAULT,
   VERSION_2_4,
   VERSION_2_5,
   VERSION_2_6,
   VERSION_2_7,
   VERSION_3_0
}


enum PybaseInterpreterType {
   PYTHON,
   JYTHON,
   IRONPYTHON
}


interface IInterpreterSpec {
   String getName();
   PybaseVersion getPythonVersion();
   PybaseInterpreterType getPythonType();
   List<File> getPythonPath();
   List<String> getForcedLibraries();
   File getExecutable();
}



/********************************************************************************/
/*										*/
/*	Project specification elements						*/
/*										*/
/********************************************************************************/

enum FileType {
   UNKNOWN,
   PROGRAM,			// executable (w/ print statements, etc)
   USERCODE,			// PROGRAM or UTILITY (not currently known)
   UTILITY,			// definitions for an executable
   MODULE,			// library inside a package
   TEST,			// test program for a particular utility/program file
};



interface IPathSpec {
   File getFile();
   File getOSFile(File base);
   boolean isUser();
   boolean isRelative();
   void outputXml(IvyXmlWriter xw);
}


interface IModuleSpec {
   String getName();
   boolean isRequired();
   void outputXml(IvyXmlWriter xw);
}


interface IFileSpec {
   File getFile();
   FileType getFileType();
   Collection<IFileSpec> getRelatedFiles();
   void outputXml(IvyXmlWriter xw);
}


interface IFileData {
   File getFile();
   IDocument getDocument();
   String getModuleName();
   void reload();
   long getLastDateLastModified();

   boolean hasChanged();
   void markChanged();
   boolean commit(boolean refresh,boolean save);

   void clearPositions();
   void setStart(Object o,int line,int col);
   void setEnd(Object o,int line,int col);
   void setEndFromStart(Object o,int line,int col);
   void setEnd(Object o,int off);
   int getStartOffset(Object o);
   int getEndOffset(Object o);
   int getLength(Object o);
}


interface ISemanticData {
   SimpleNode getRootNode();
   List<PybaseMessage> getMessages();
   PybaseScopeItems getScope();
   PybaseScopeItems getGlobalScope();
   IFileData getFileData();
   PybaseProject getProject();
}


/********************************************************************************/
/*										*/
/*	Edit information							*/
/*										*/
/********************************************************************************/

interface IEditData {

   public int getOffset();
   public int getLength();
   public String getText();

}	// end of subinterface EditData


/********************************************************************************/
/*										*/
/*	Symbol information							*/
/*										*/
/********************************************************************************/

enum ScopeType {
   NONE,
   GLOBAL,
   METHOD,
   CLASS,
   LIST_COMP,
   LAMBDA
};


enum VisitorType {
   GLOBAL_TOKENS,
   WILD_MODULES,
   ALIAS_MODULES,
   MODULE_DOCSTRING,
   INNER_DEFS,
   ALL;

   public boolean match(VisitorType vt) {
      if (vt == this) return true;
      if (this == ALL) return vt != INNER_DEFS;
      if (vt == ALL) return this != INNER_DEFS;
      return false;
    }

}	// end of enum type VisitorType


enum FoundType {
   NOT_FOUND,
   FOUND_TOKEN,
   FOUND_BECAUSE_OF_GETATTR
};



Set<ScopeType> ACCEPTED_METHOD_SCOPES = EnumSet.of(ScopeType.GLOBAL,
      ScopeType.METHOD, ScopeType.LAMBDA, ScopeType.LIST_COMP);
Set<ScopeType> ACCEPTED_ALL_SCOPES = EnumSet.allOf(ScopeType.class);
Set<ScopeType> ACCEPTED_METHOD_AND_LAMBDA = EnumSet.of(ScopeType.METHOD,
      ScopeType.LAMBDA);




enum TokenType {
   UNKNOWN,
   IMPORT,
   CLASS,
   FUNCTION,
   ATTR,			// attribute or global variable
   BUILTIN,
   PARAM,			// parameter
   PACKAGE,
   RELATIVE_IMPORT,
   EPYDOC,			// epydoc field
   LOCAL,
   OBJECT_FOUND_INTERFACE,	// token created results as an interface for some object
}


// Completion State Look for types

enum LookForType {
   INSTANCE_UNDEFINED,
   INSTANCED_VARIABLE,
   UNBOUND_VARIABLE,
   CLASSMETHOD_VARIABLE,
   ASSIGN
}
interface SymbolVisitor {

   void visitScopeBegin(PybaseScopeItems scp);
   void visitScopeEnd(PybaseScopeItems scp);
   void visitItem(Found f);

}	// SymbolVisitor



/********************************************************************************/
/*										*/
/*	Search Information							*/
/*										*/
/********************************************************************************/

int MAX_TEXT_SEARCH_RESULTS = 128;


enum SearchFor {
   NONE,
   ANNOTATION,
   CONSTRUCTOR,
   METHOD,
   FIELD,
   TYPE,
   PACKAGE,
   CLASS,
}


/********************************************************************************/
/*										*/
/*	Thread pool information 						*/
/*										*/
/********************************************************************************/

int PYBASE_CORE_POOL_SIZE = 2;
int PYBASE_MAX_POOL_SIZE = 8;
long PYBASE_POOL_KEEP_ALIVE_TIME = 2*60*1000;



/********************************************************************************/
/*										*/
/*	Errors									*/
/*										*/
/********************************************************************************/

enum ErrorType {
   ASSIGNMENT_TO_BUILT_IN_SYMBOL,
   DUPLICATED_SIGNATURE,
   INDENTATION_PROBLEM,
   NO_EFFECT_STMT,
   NO_SELF,
   REIMPORT,
   UNDEFINED_IMPORT_VARIABLE,
   UNDEFINED_VARIABLE,
   UNRESOLVED_IMPORT,
   UNUSED_IMPORT,
   UNUSED_PARAMETER,
   UNUSED_VARIABLE,
   UNUSED_WILD_IMPORT,
   SYNTAX_ERROR,
}

enum ErrorSeverity {
   IGNORE,
   INFO,
   WARNING,
   ERROR
}



/********************************************************************************/
/*										*/
/*	Debugging constants							*/
/*										*/
/********************************************************************************/

enum PybaseDebugAction {
   NONE,
   TERMINATE,
   RESUME,
   STEP_INTO,
   STEP_OVER,
   STEP_RETURN,
   SUSPEND,
   DROP_TO_FRAME
}

}	// end of interface PybaseConstants




/* end of PybaseConstants.java */
