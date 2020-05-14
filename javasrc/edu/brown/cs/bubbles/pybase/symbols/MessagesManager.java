/********************************************************************************/
/*										*/
/*		MessagesManager 						*/
/*										*/
/*	Python Bubbles Base error message management				*/
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
 * Created on 24/07/2005
 */


package edu.brown.cs.bubbles.pybase.symbols;


import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseMessage;
import edu.brown.cs.bubbles.pybase.PybasePreferences;
import edu.brown.cs.bubbles.pybase.PybaseScopeItems;
import edu.brown.cs.bubbles.pybase.symbols.AbstractVisitor.ImportPartSourceToken;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.jython.ast.name_contextType;
import org.python.pydev.parser.jython.ast.stmtType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class MessagesManager implements PybaseConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybasePreferences    analysis_prefs;
private Map<AbstractToken, List<PybaseMessage>> message_list;
private List<PybaseMessage>	   independent_messages;
private String		      module_name;
private IDocument		   for_document;
private Set<String>		 names_to_ignore_cache;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public MessagesManager(PybasePreferences prefs,String moduleName,IDocument doc)
{
   analysis_prefs = prefs;
   module_name = moduleName;
   for_document = doc;
   message_list = new HashMap<AbstractToken, List<PybaseMessage>>();
   independent_messages = new ArrayList<PybaseMessage>();
   names_to_ignore_cache = null;
}


/********************************************************************************/
/*										*/
/*	Message interface							*/
/*										*/
/********************************************************************************/

/**
 * @return whether we should add an unused import message to the module being analyzed
 */
public boolean shouldAddUnusedImportMessage()
{
   if (module_name == null) {
      return true;
   }
   String onlyModName = FullRepIterable.headAndTail(module_name, true)[1];
   Set<String> patternsToBeIgnored = analysis_prefs.getModuleNamePatternsToBeIgnored();
   for (String pattern : patternsToBeIgnored) {
      if (onlyModName.matches(pattern)) {
	 return false;
      }
   }
   return true;
}


/**
 * adds a message of some type given its formatting params
 */
public void addMessage(ErrorType type,AbstractToken generator,Object... objects)
{
   if (isUnusedImportMessage(type)) {
      if (!shouldAddUnusedImportMessage()) {
	 return;
      }
   }
   doAddMessage(independent_messages, type, objects, generator);
}


/**
 * adds a message of some type for a given token
 */
public void addMessage(ErrorType type,AbstractToken token)
{
   List<PybaseMessage> msgs = getMsgsList(token);
   doAddMessage(msgs, type, token.getRepresentation(), token);
}

/**
 * adds a message of some type for some Found instance
 */
public void addMessage(ErrorType type,AbstractToken generator,AbstractToken tok)
{
   addMessage(type, generator, tok, tok.getRepresentation());
}

/**
 * adds a message of some type for some Found instance
 */
public void addMessage(ErrorType type,AbstractToken generator,AbstractToken tok,String rep)
{
   List<PybaseMessage> msgs = getMsgsList(generator);
   doAddMessage(msgs, type, rep, generator);
}

public void addUndefinedMessage(AbstractToken token)
{
   addUndefinedMessage(token, null);
}

/**
 * @param token adds a message saying that a token is not defined
 */
public void addUndefinedMessage(AbstractToken token,String rep)
{
   Tuple<Boolean, String> undef = isActuallyUndefined(token, rep);
   if (undef.o1) {
      addMessage(ErrorType.UNDEFINED_VARIABLE, token, undef.o2);
   }
}

/**
 * @param token adds a message saying that a token gathered from an import is not defined
 */
public void addUndefinedVarInImportMessage(AbstractToken token,String rep)
{
   Tuple<Boolean, String> undef = isActuallyUndefined(token, rep);
   if (undef.o1) {
      addMessage(ErrorType.UNDEFINED_IMPORT_VARIABLE, token, undef.o2);
   }
}

/**
 * @param token adds a message saying that a token gathered from assignment is a reserved keyword
 */
public void onAddAssignmentToBuiltinMessage(AbstractToken token,String rep)
{
   addMessage(ErrorType.ASSIGNMENT_TO_BUILT_IN_SYMBOL, token);
}


/**
 * adds a message for something that was not used
 *
 * @param node the node representing the scope being closed when adding the
 *		   unused message
 */
public void addUnusedMessage(SimpleNode node,Found f)
{
   for (GenAndTok g : f) {
      if (g.getGenerator() instanceof SourceToken) {
	 SimpleNode ast = ((SourceToken) g.getGenerator()).getAst();
	 // it can be an unused import
	 if (ast instanceof Import || ast instanceof ImportFrom) {
	    if (AbstractVisitor.isWildImport(ast)) {
	       addMessage(ErrorType.UNUSED_WILD_IMPORT, g.getGenerator(),g.getToken());
	    }
	    else if (!(g.getGenerator() instanceof ImportPartSourceToken)) {
	       addMessage(ErrorType.UNUSED_IMPORT, g.getGenerator(),g.getToken());
	    }
	    continue; // finish it...
	 }
      }

      // or unused variable
      // we have to check if this is a name we should ignore
      if (startsWithNamesToIgnore(g)) {
	 ErrorType type = ErrorType.UNUSED_VARIABLE;

	 if (g.getToken() instanceof SourceToken) {
	    SourceToken t = (SourceToken) g.getToken();
	    SimpleNode ast = t.getAst();
	    if (ast instanceof NameTok) {
	       NameTok n = (NameTok) ast;
	       if (n.ctx == name_contextType.KwArg || n.ctx == name_contextType.VarArg
			|| n.ctx == name_contextType.KeywordName) {
		  type = ErrorType.UNUSED_PARAMETER;
	       }
	    }
	    else if (ast instanceof Name) {
	       Name n = (Name) ast;
	       if (n.ctx == expr_contextType.Param || n.ctx == expr_contextType.KwOnlyParam) {
		  type = ErrorType.UNUSED_PARAMETER;
	       }
	    }
	 }
	 boolean addMessage = true;
	 if (type == ErrorType.UNUSED_PARAMETER) {
	    // just add unused parameters in methods that have some content (not only
	    // 'pass' and 'strings')

	    if (node instanceof FunctionDef) {
	       addMessage = false;
	       FunctionDef def = (FunctionDef) node;
	       for (stmtType b : def.body) {
		  if (b instanceof Pass) {
		     continue;
		  }
		  if (b instanceof Expr) {
		     Expr expr = (Expr) b;
		     if (expr.value instanceof Str) {
			continue;
		     }
		  }
		  addMessage = true;
		  break;
	       }
	    }
	 }

	 if (addMessage) {
	    addMessage(type, g.getGenerator(), g.getToken());
	 }
      }
   }
}


/**
 * adds a message for a re-import
 */
public void addReimportMessage(Found f)
{
   for (GenAndTok g : f) {
      // we don't want to add reimport messages if they are found in a wild import
      if (g.getGenerator() instanceof SourceToken
	       && !(g.getGenerator() instanceof ImportPartSourceToken)
	       && AbstractVisitor.isWildImport(g.getGenerator()) == false) {
	 addMessage(ErrorType.REIMPORT, g.getGenerator(), g.getToken());
      }
   }
}


public void setLastScope(PybaseScopeItems m)
{}


/********************************************************************************/
/*										*/
/*	Accessing messages							*/
/*										*/
/********************************************************************************/

/**
 * @return the messages associated with a token
 */
public List<PybaseMessage> getMsgsList(AbstractToken generator)
{
   List<PybaseMessage> msgs = message_list.get(generator);
   if (msgs == null) {
      msgs = new ArrayList<PybaseMessage>();
      message_list.put(generator, msgs);
   }
   return msgs;
}


/**
 * @return the generated messages.
 */
public List<PybaseMessage> getMessages()
{

   List<PybaseMessage> result = new ArrayList<PybaseMessage>();

   // let's get the messages
   for (List<PybaseMessage> l : message_list.values()) {
      if (l.size() < 1) {
	 // we need at least one message
	 continue;
      }

      Map<ErrorType,List<PybaseMessage>> messagesByType = getMessagesByType(l);
      for (ErrorType type : messagesByType.keySet()) {
	 l = messagesByType.get(type);

	 // the values are guaranteed to have size at least equal to 1
	 PybaseMessage message = l.get(0);

	 // messages are grouped by type, and the severity is set by type, so, this is
	 // ok...
	 if (message.getSeverity() == ErrorSeverity.INFO) {
	    if (doIgnoreMessageIfJustInformational(message.getType())) {
	       // ok, let's ignore it for real (and don't add it) as those are not likely
	       // to be used anyways for other actions)
	       continue;

	    }
	 }
	 // we add even ignore messages because they might be used later in actions
	 // dependent on code analysis

	 if (l.size() == 1) {
	    // don't add additional info: not being used
	    // addAdditionalInfoToUnusedWildImport(message);
	    addToResult(result, message);

	 }
	 else {
	    /**********************
	    // the generator token has many associated messages - the messages may have
	    // different types,
	    // so, we need to get them by types
	    CompositeMessage compositeMessage = new CompositeMessage(message.getType(),
		     message.getGenerator(),analysis_prefs);
	    for (PybaseMessage m : l) {
	       compositeMessage.addMessage(m);
	    }

	    // don't add additional info: not being used
	    // addAdditionalInfoToUnusedWildImport(compositeMessage);
	    addToResult(result, compositeMessage);
	    ************************/
	    addToResult(result,message);
	 }
      }
   }

   for (PybaseMessage message : independent_messages) {
      if (message.getSeverity() == ErrorSeverity.INFO) {
	 if (doIgnoreMessageIfJustInformational(message.getType())) {
	    // ok, let's ignore it for real (and don't add it) as those are not likely to
	    // be used anyways for other actions)
	    continue;
	 }
	 // otherwise keep on and add it (needed for some actions)
      }

      addToResult(result, message);
   }

   return result;
}


/********************************************************************************/
/*										*/
/*	Handle adding messages							*/
/*										*/
/********************************************************************************/

/**
 * checks if the message should really be added and does the add.
 */
private void doAddMessage(List<PybaseMessage> msgs,ErrorType type,Object string,AbstractToken token)
{
   if (isUnusedImportMessage(type)) {
      if (!shouldAddUnusedImportMessage()) {
	 return;
       }
    }
   
   PybaseMessage messagetoadd = new PybaseMessage(type,string,token,analysis_prefs);
   
   String messageToIgnore = analysis_prefs.getRequiredMessageToIgnore(messagetoadd.getType());
   if (messageToIgnore != null) {
      int startLine = messagetoadd.getStartLine(for_document) - 1;
      String line = getLine(for_document, startLine);
      if (line.indexOf(messageToIgnore) != -1) {
	 // keep going... nothing to see here...
	 return;
       }
    }
   
   msgs.add(messagetoadd);
}


/**
 * Checks if some token is actually undefined and changes its representation if needed
 * @return a tuple indicating if it really is undefined and the representation that should be used.
 */
private Tuple<Boolean, String> isActuallyUndefined(AbstractToken token,String rep)
{
   String tokenRepresentation = token.getRepresentation();
   if (tokenRepresentation != null) {
      String firstPart = FullRepIterable.getFirstPart(tokenRepresentation);
      if (analysis_prefs.getTokensAlwaysInGlobals().contains(firstPart)) {
	 return new Tuple<Boolean, String>(false,firstPart); // ok firstPart in not really
							     // undefined...
      }
   }

   boolean isActuallyUndefined = true;
   if (rep == null) {
      rep = tokenRepresentation;
   }

   int i;
   if (rep != null && (i = rep.indexOf('.')) != -1) {
      rep = rep.substring(0, i);
   }

   String builtinType = NodeUtils.getBuiltinType(rep);
   if (builtinType != null) {
      isActuallyUndefined = false; // this is a builtin, so, it is defined after all
   }
   return new Tuple<Boolean, String>(isActuallyUndefined,rep);
}


/**
 * @param g the generater that will generate an unused variable message
 * @return true if we should not add the message
 */
private boolean startsWithNamesToIgnore(GenAndTok g)
{
   if (names_to_ignore_cache == null) {
      names_to_ignore_cache = analysis_prefs.getNamesIgnoredByUnusedVariable();
   }
   String representation = g.getToken().getRepresentation();
   if (names_to_ignore_cache.contains(representation)) return true;

   for (String str : names_to_ignore_cache) {
      if (representation.startsWith(str)) {
         names_to_ignore_cache.add(representation);
         return true;
      }
   }
   
   return false;
}


private boolean doIgnoreMessageIfJustInformational(ErrorType type)
{
   switch (type) {
      case UNUSED_PARAMETER :
      case INDENTATION_PROBLEM :
      case NO_EFFECT_STMT :
      case ASSIGNMENT_TO_BUILT_IN_SYMBOL :
	 return true;
      default :
	 return false;
    }
}


/**
 * @param result
 * @param message
 */
private void addToResult(List<PybaseMessage> result,PybaseMessage message)
{
   if (isUnusedImportMessage(message.getType())) {
      AbstractToken generator = message.getGenerator();
      if (generator instanceof SourceToken) {
	 String asAbsoluteImport = generator.getAsAbsoluteImport();
	 if (asAbsoluteImport.indexOf("__future__.") != -1
		  || asAbsoluteImport.indexOf("__metaclass__") != -1) {
	    // do not add from __future__ import xxx
	    return;
	 }
      }
   }
   result.add(message);
}


/**
 * @return a map with the messages separated by type (keys are the type)
 *
 * the values are guaranteed to have size at least equal to 1
 */
private Map<ErrorType,List<PybaseMessage>> getMessagesByType(List<PybaseMessage> l)
{
   HashMap<ErrorType,List<PybaseMessage>> messagesbytype = new HashMap<ErrorType,List<PybaseMessage>>();
   for (PybaseMessage message : l) {
      List<PybaseMessage> messages = messagesbytype.get(message.getType());
      if (messages == null) {
	 messages = new ArrayList<PybaseMessage>();
	 messagesbytype.put(message.getType(), messages);
      }
      messages.add(message);
   }
   return messagesbytype;
}


/**
 * @param type the type of the message
 * @return whether it is an unused import message
 */
private boolean isUnusedImportMessage(ErrorType type)
{
   switch (type) {
      case UNUSED_IMPORT :
      case UNUSED_WILD_IMPORT :
	 return true;
      default :
	 return false;
    }
}



/********************************************************************************/
/*										*/
/*	Utilities								*/
/*										*/
/********************************************************************************/

private static String getLine(IDocument doc, int i)
{
   try {
      IRegion lineInformation = doc.getLineInformation(i);
      return doc.get(lineInformation.getOffset(), lineInformation.getLength());
    }
   catch (Exception e) {
      return "";
    }
}




}	// end of class MessagesManager


/* end of MessagesManager.java */
