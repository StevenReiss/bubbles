/********************************************************************************/
/*										*/
/*		PybaseMessage.java						*/
/*										*/
/*	Python Bubbles Base the_message container				*/
/*										*/
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
 * Created on 19/07/2005
 */

package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.AbstractVisitor;
import edu.brown.cs.bubbles.pybase.symbols.SourceToken;
import edu.brown.cs.bubbles.pybase.symbols.StringUtils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PybaseMessage implements PybaseConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Object		base_message;
private String		the_message;
private ErrorType	msg_type;
private ErrorSeverity	msg_severity;
private AbstractToken	msg_generator;
private List<String>	additional_info;
private int		start_col;
private int		start_line;
private int		end_line;
private int		end_col;


private static final Map<ErrorType,String> message_set = new HashMap<ErrorType,String>();

static {
   message_set.put(ErrorType.UNUSED_IMPORT,"Unused import: %s");
   message_set.put(ErrorType.UNUSED_WILD_IMPORT,"Unused in wild import: %s");
   message_set.put(ErrorType.UNUSED_VARIABLE,"Unused variable: %s");
   message_set.put(ErrorType.UNUSED_PARAMETER,"Unused parameter: %s");
   message_set.put(ErrorType.UNDEFINED_VARIABLE,"Undefined variable: %s");
   message_set.put(ErrorType.DUPLICATED_SIGNATURE,"Duplicated signature: %s");
   message_set.put(ErrorType.REIMPORT,"Import redefinition: %s");
   message_set.put(ErrorType.UNRESOLVED_IMPORT,"Unresolved import: %s");
   message_set.put(ErrorType.NO_SELF,"Method '%s' should have %s as first parameter");
   message_set.put(ErrorType.UNDEFINED_IMPORT_VARIABLE,"Undefined variable from import: %s");
   message_set.put(ErrorType.NO_EFFECT_STMT,"Statement apppears to have no effect");
   message_set.put(ErrorType.INDENTATION_PROBLEM, "%s");
   message_set.put(ErrorType.ASSIGNMENT_TO_BUILT_IN_SYMBOL,"Assignment to reserved built-in symbol: %s");
   message_set.put(ErrorType.SYNTAX_ERROR,"Syntax error");
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseMessage(ErrorType type,Object message,int startLine,int endLine,int startCol,
			int endCol,PybasePreferences prefs)
{
   msg_severity = prefs.getSeverityForType(type);
   msg_type = type;
   base_message = message;
   start_line = startLine;
   start_col = startCol;
   end_line = endLine;
   end_col = endCol;
   the_message = null;
}



public PybaseMessage(ErrorType type,Object message,AbstractToken generator,PybasePreferences prefs)
{
   msg_severity = prefs.getSeverityForType(type);
   msg_type = type;
   msg_generator = generator;
   base_message = message;
   the_message = null;
   start_col = -1;
   start_line = -1;
   end_line = -1;
   end_col = -1;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Object getShortMessage()
{
   return base_message;
}


private String getTypeStr()
{
   return message_set.get(getType());
}

public ErrorSeverity getSeverity()
{
   return msg_severity;
}

public ErrorType getType()
{
   return msg_type;
}


public int getStartLine(IDocument doc)
{
   if (start_line < 0) {
      start_line = getStartLine(msg_generator, doc);
    }
   return start_line;
}

/**
 * @return the starting line fro the given token (starting at 1)
 */
public static int getStartLine(AbstractToken generator,IDocument doc)
{
   if (generator == null) return 0;
   
   return getStartLine(generator, doc, generator.getRepresentation());
}

public static int getStartLine(AbstractToken generator,IDocument doc,String shortMessage)
{
   return getStartLine(generator, doc, shortMessage, false);

}

public static int getStartLine(AbstractToken generator,IDocument doc,String shortMessage,
				  boolean returnAsName)
{
   if (!generator.isImport()) {
      return generator.getLineDefinition();
    }

   // ok, it is an import... (can only be a source token)
   SourceToken s = (SourceToken) generator;

   SimpleNode ast = s.getAst();
   if (ast instanceof ImportFrom) {
      ImportFrom i = (ImportFrom) ast;
      // if it is a wild import, it starts on the module name
      if (AbstractVisitor.isWildImport(i)) {
	 return i.module.beginLine;
       }
      else {
	 // no wild import, let's check the 'as name'
	 return getNameForRepresentation(i, shortMessage, returnAsName).beginLine;
       }

    }
   else if (ast instanceof Import) {
      return getNameForRepresentation(ast, shortMessage, returnAsName).beginLine;

    }
   else {
      throw new RuntimeException("It is not an import");
    }
}


/**
 * gets the start col of the the_message (starting at 1)
 */
public int getStartCol(IDocument doc)
{
   if (start_col >= 0) {
      return start_col;
    }
   if (msg_generator == null) return 0;
   
   start_col = getStartCol(msg_generator, doc, getShortMessageStr());
   return start_col;

}


private String getShortMessageStr()
{
   Object msg = getShortMessage();
   if (msg instanceof Object[]) {
      Object[] msgs = (Object[]) msg;
      StringBuilder buffer = new StringBuilder();
      for (Object o : msgs) {
	 buffer.append(o.toString());
       }
      return buffer.toString();
    }
   else {
      return msg.toString();
    }
}

/**
 * @return the starting column for the given token (1-based)
 */
public static int getStartCol(AbstractToken generator,IDocument doc)
{
   return getStartCol(generator, doc, generator.getRepresentation());
}

/**
 * @return the starting column for the given token (1-based)
 */
public static int getStartCol(AbstractToken generator,IDocument doc,String shortMessage)
{
   return getStartCol(generator, doc, shortMessage, false);

}

/**
 * @return the starting column for the given token (1-based)
 */
public static int getStartCol(AbstractToken generator,IDocument doc,String shortMessage,
				 boolean returnAsName)
{
   if (generator == null) return 0;
   
   // not import...
   if (!generator.isImport()) {
      return generator.getColDefinition();
    }

   // ok, it is an import... (can only be a source token)
   SourceToken s = (SourceToken) generator;

   SimpleNode ast = s.getAst();
   if (ast instanceof ImportFrom) {
      ImportFrom i = (ImportFrom) ast;
      // if it is a wild import, it starts on the module name
      if (AbstractVisitor.isWildImport(i)) {
	 return i.module.beginColumn;
       }
      else {
	 // no wild import, let's check the 'as name'
	 return getNameForRepresentation(i, shortMessage, returnAsName).beginColumn;
       }

    }
   else if (ast instanceof Import) {
      return getNameForRepresentation(ast, shortMessage, returnAsName).beginColumn;

    }
   else {
      throw new RuntimeException("It is not an import");
    }
}

/**
 * @param imp this is the import ast
 * @param fullRep this is the representation we are looking for
 * @param returnAsName defines if we should return the asname or only the name (depending on what we are
										   * analyzing -- the start or the end of the representation).
 *
 * @return the name tok for the representation in a given import
 */
private static NameTok getNameForRepresentation(SimpleNode imp,String rep,
						   boolean returnAsName)
{

   aliasType[] names;
   if (imp instanceof Import) {
      names = ((Import) imp).names;
    }
   else if (imp instanceof ImportFrom) {
      names = ((ImportFrom) imp).names;
    }
   else {
      throw new RuntimeException("import expected");
    }

   for (aliasType alias : names) {
      if (alias.asname != null) {
	 if (((NameTok) alias.asname).id.equals(rep)) {
	    if (returnAsName) {
	       return (NameTok) alias.asname;
	     }
	    else {
	       return (NameTok) alias.name;
	     }
	  }
       }
      else { // let's check for the name

	 String fullRepNameId = ((NameTok) alias.name).id;

	 // we have to get all representations, since an import such as import os.path
	 // would
	 // have to match os and os.path
	 for (String repId : new FullRepIterable(fullRepNameId)) {

	    if (repId.equals(rep)) {
	       return (NameTok) alias.name;
	     }
	  }
       }
    }
   return null;
}


public int getEndLine(IDocument doc)
{
   return getEndLine(doc, true);
}

public int getEndLine(IDocument doc,boolean getOnlyToFirstDot)
{
   if (end_line < 0) {
      end_line = getEndLine(msg_generator, doc, getOnlyToFirstDot);
    }
   return end_line;

}

public static int getEndLine(AbstractToken generator,IDocument doc,boolean getOnlyToFirstDot)
{
   if (generator instanceof SourceToken) {
      if (!generator.isImport()) {
	 return ((SourceToken) generator).getLineEnd(getOnlyToFirstDot);
       }
      return getStartLine(generator, doc); // for an import, the endline == startline

    }
   else {
      return -1;
    }
}


public int getEndCol(IDocument doc)
{
   return getEndCol(doc, true);
}

public int getEndCol(IDocument doc,boolean getOnlyToFirstDot)
{
   if (end_col >= 0) {
      return end_col;
    }
   end_col = getEndCol(msg_generator, doc, getShortMessageStr(), getOnlyToFirstDot);
   return end_col;

}

/**
 * @param msg_generator is the token that generated this the_message
 * @param doc is the document where this the_message will be put
 * @param shortMessage is used when it is an import ( = foundTok.getRepresentation())
 *
 * @return the end column for this the_message
 */
public static int getEndCol(AbstractToken generator,IDocument doc,String shortMessage,
			       boolean getOnlyToFirstDot)
{
   int endCol = -1;
   if (generator.isImport()) {
      // ok, it is an import... (can only be a source token)
      SourceToken s = (SourceToken) generator;

      SimpleNode ast = s.getAst();

      if (ast instanceof ImportFrom) {
	 ImportFrom i = (ImportFrom) ast;
	 // ok, now, this depends on the name
	 NameTok it = getNameForRepresentation(i, shortMessage, true);
	 if (it != null) {
	    endCol = it.beginColumn + shortMessage.length();
	    return endCol;
	  }

	 // if still not returned, it is a wild import... find the '*'
	 try {
	    IRegion lineInformation = doc.getLineInformation(i.module.beginLine - 1);
	    // ok, we have the line... now, let's find the absolute offset
	    int absolute = lineInformation.getOffset() + i.module.beginColumn - 1;
	    while (doc.getChar(absolute) != '*') {
	       absolute++;
	     }
	    int absoluteCol = absolute + 1; // 1 for the *
	    IRegion region = doc.getLineInformationOfOffset(absoluteCol);
	    endCol = absoluteCol - region.getOffset() + 1; // 1 because we should return
	    // as if starting in 1 and not 0
	    return endCol;
	  }
	 catch (BadLocationException e) {
	    throw new RuntimeException(e);
	  }

       }
      else if (ast instanceof Import) {
	 NameTok it = getNameForRepresentation(ast, shortMessage, true);
	 endCol = it.beginColumn + shortMessage.length();
	 return endCol;
       }
      else {
	 throw new RuntimeException("It is not an import");
       }
    }

   // no import... make it regular
   if (generator instanceof SourceToken) {
      return ((SourceToken) generator).getColEnd(getOnlyToFirstDot);
    }
   return -1;
}


@Override public String toString()
{
   return getMessage();
}

public List<String> getAdditionalInfo()
{
   return additional_info;
}

public void addAdditionalInfo(String info)
{
   if (this.additional_info == null) {
      this.additional_info = new ArrayList<String>();
    }
   this.additional_info.add(info);
}

public String getMessage()
{
   if (the_message != null) {
      return the_message;
    }

   String typeStr = getTypeStr();
   if (typeStr == null) {
      throw new AssertionError("Unable to get the_message for msg_type: " + getType());
    }
   Object shortMessage = getShortMessage();
   if (shortMessage == null) {
      throw new AssertionError("Unable to get shortMessage (" + typeStr + ")");
    }
   if (shortMessage instanceof Object[]) {
      Object[] o = (Object[]) shortMessage;

      // if we have the same number of %s as objects in the array, make the format
      int countPercS = StringUtils.countPercS(typeStr);
      if (countPercS == o.length) {
	 return StringUtils.format(typeStr, o);

       }
      else if (countPercS == 1) {
	 // if we have only 1, all parameters should be concatenated in a single string
	 StringBuilder buf = new StringBuilder();
	 for (int i = 0; i < o.length; i++) {
	    buf.append(o[i].toString());
	    if (i != o.length - 1) {
	       buf.append(" ");
	     }
	  }
	 shortMessage = buf.toString();

       }
      else {
	 throw new AssertionError(
	    "The number of %s is not the number of passed parameters nor 1");
       }
    }
   the_message = StringUtils.format(typeStr, shortMessage);
   return the_message;
}

public AbstractToken getGenerator()
{
   return msg_generator;
}


/********************************************************************************/
/*										*/
/*	Equals and hashCode to equate messages					*/
/*										*/
/********************************************************************************/

@Override public boolean equals(Object obj)
{
   if (!(obj instanceof PybaseMessage)) {
      return false;
    }
   PybaseMessage m = (PybaseMessage) obj;
   return this.getType() == m.getType() && this.the_message.equals(m.the_message);
}



@Override public int hashCode()
{
   return getMessage().hashCode();
}



}	// end of class PybaseMessage



/* end of PybaseMessage.java */
