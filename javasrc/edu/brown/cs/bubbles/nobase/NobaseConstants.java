/********************************************************************************/
/*										*/
/*		NobaseConstants.java						*/
/*										*/
/*	Constants for Node Bubbles base interface						    */
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.nobase;


import java.util.HashMap;
import java.util.List;

import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

public interface NobaseConstants
{



/********************************************************************************/
/*										*/
/*	Messaging definitions							*/
/*										*/
/********************************************************************************/

// Must Match BumpConstants.BUMP_MINT_NAME
String	NOBASE_MINT_NAME = "BUBBLES_" + System.getProperty("user.name").replace(" ","_");



/********************************************************************************/
/*										*/
/*	Logging constants							*/
/*										*/
/********************************************************************************/

enum NobaseLogLevel {
   NONE,
   ERROR,
   WARNING,
   INFO,
   DEBUG
}




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



interface SearchResult {

   int getOffset();
   int getLength();
   NobaseFile getFile();
   NobaseSymbol getSymbol();
   NobaseSymbol getContainer();

}




/********************************************************************************/
/*										*/
/*	Errro Messages								*/
/*										*/
/********************************************************************************/

enum ErrorSeverity {
   IGNORE,
   INFO,
   WARNING,
   ERROR
}



/********************************************************************************/
/*										*/
/*	Thread pool information 						*/
/*										*/
/********************************************************************************/

int NOBASE_CORE_POOL_SIZE = 2;
int NOBASE_MAX_POOL_SIZE = 8;
long NOBASE_POOL_KEEP_ALIVE_TIME = 2*60*1000;




/********************************************************************************/
/*										*/
/*	Edit command information						*/
/*										*/
/********************************************************************************/

interface IEditData {

   int getOffset();
   int getLength();
   String getText();

}	// end of subinterface EditData




/********************************************************************************/
/*										*/
/*	Compiler information							*/
/*										*/
/********************************************************************************/

interface IParser {
   ISemanticData parse(NobaseProject proj,NobaseFile fd,boolean lib);
}



interface ISemanticData {

   NobaseFile getFileData();
   NobaseProject getProject();
   List<NobaseMessage> getMessages();
   void addMessages(List<NobaseMessage> msgs);

   JavaScriptUnit getRootNode();

}	// end of inner interface ISemanticData




enum ScopeType {
   GLOBAL,
   FILE,
   FUNCTION,
   CLASS,
   CATCH,
   MEMBER,
   OBJECT,
   LOCAL,
   WITH
};


String AST_PROPERTY_SCOPE = "Nobase.Scope";
String AST_PROPERTY_REF = "Nobase.Ref";
String AST_PROPERTY_DEF = "Nobase.Def";
String AST_PROPERTY_TYPE = "Nobase.Type";
String AST_PROPERTY_NAME = "Nobase.Name";
String AST_PROPERTY_VALUE = "Nobase.Value";




enum NameType {
   FUNCTION,
   CLASS,
   VARIABLE,
   LOCAL,
   MODULE,
}


enum SymbolType {
   VAR,
   CONST,
   LET
}


enum KnownValue {
   UNDEFINED,
   NULL,
   ANY,
   UNKNOWN
}


interface Evaluator {

   NobaseValue evaluate(NobaseFile nf,List<NobaseValue> arguments,
	    NobaseValue thisval);

}	// end of interface FunctionEvaluator




/********************************************************************************/
/*										*/
/*	Debugging constants							*/
/*										*/
/********************************************************************************/

enum NobaseDebugAction {
   NONE,
   TERMINATE,
   RESUME,
   STEP_INTO,
   STEP_OVER,
   STEP_RETURN,
   SUSPEND,
   DROP_TO_FRAME
}



/********************************************************************************/
/*										*/
/*	Debugging definitions							*/
/*										*/
/********************************************************************************/

enum NobaseConfigAttribute {
   NONE,
   PROJECT_ATTR,
   ARGS,
   VM_ARGS,
   MAIN_TYPE,
   WD,
   ENCODING,
   NAME,
   FILE
}

String CONFIG_FILE = ".launches";
String BREAKPOINT_FILE = ".breakpoints";

enum BreakType {
   NONE,
   LINE,
   EXCEPTION
}



class IdCounter {

   private int counter_value;

   IdCounter() {
      counter_value = 1;
    }

   public synchronized int nextValue() {
      return counter_value++;
    }

   public synchronized void noteValue(int v) {
      if (counter_value <= v) counter_value = v+1;
    }

}	// end of inner class IdCounter


class NobaseDebugRefMap extends HashMap<String,NobaseDebugValue> {

   private NobaseDebugTarget for_target;
   private static final long serialVersionUID = 1;
   
   NobaseDebugRefMap(NobaseDebugTarget tgt) {
      for_target = tgt;
    }

   NobaseDebugTarget getTarget()                { return for_target; }
   
}



interface NobaseDebugFunction {

   String getName();
   // int getOffset();
   // int getLine();
   // int getColumn();
}


interface NobaseDebugScript {

   String getName();

}

enum DebugKindValue {
   RESUME, SUSPEND, CREATE, TERMINATE, CHANGE, MODEL_SPECIFIC
}

enum DebugDetailValue {
   STEP_INTO, STEP_OVER, STEP_RETURN, STEP_END, BREAKPOINT, CLIENT_REQUEST,
      EVALUATION, EVALUATION_IMPLICIT, STATE, CONTENT, UNSPECIFIED,
      SUSPEND
}



}	// end of interface NobaseConstants




/* end of NobaseConstants.java */

