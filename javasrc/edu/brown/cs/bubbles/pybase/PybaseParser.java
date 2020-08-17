/********************************************************************************/
/*										*/
/*		PybaseParser.java						*/
/*										*/
/*	Python Bubbles Base parser interface					*/
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
 * Author: atotic
 * Created: Jul 25, 2003
 */
package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractModule;
import edu.brown.cs.bubbles.pybase.symbols.Scope;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.IGrammar;
import org.python.pydev.parser.grammar24.PythonGrammar24;
import org.python.pydev.parser.grammar25.PythonGrammar25;
import org.python.pydev.parser.grammar26.PythonGrammar26;
import org.python.pydev.parser.grammar27.PythonGrammar27;
import org.python.pydev.parser.grammar30.PythonGrammar30;
import org.python.pydev.parser.jython.CharStream;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.cmpopType;
import org.python.pydev.parser.jython.ast.stmtType;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;



/**
 * PyParser uses org.python.parser to parse the document (lexical analysis) It
 * is attached to PyEdit (a view), and it listens to document changes On every
 * document change, the syntax tree is regenerated The reparsing of the document
 * is done on a ParsingThread
 *
 * Clients that need to know when new parse tree has been generated should
 * register as parseListeners.
 */

public class PybaseParser implements PybaseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private PybaseProject for_project;
private IFileData file_data;
private SimpleNode root_node;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseParser(PybaseProject p,IFileData fd)
{
   for_project = p;
   file_data = fd;
   root_node = null;
}



public PybaseParser(PybaseProject p,IDocument d,File f)
{
   for_project = p;
   file_data = PybaseFileManager.getFileManager().getFileData(d,f);
   root_node = null;
}


public PybaseParser(PybaseProject p,IDocument d)
{
   this(p,d,null);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

SimpleNode getRoot()				{ return root_node; }




/********************************************************************************/
/*										*/
/*	Parsing methods 							*/
/*										*/
/********************************************************************************/

public ISemanticData parseDocument()
{
   return parseDocument(true);
}



public ISemanticData parseDocument(boolean semantics)
{
   root_node = null;
   SemanticData sd = null;

   String txt = file_data.getDocument().get();

   if (txt.trim().length() == 0) {
      root_node = new Module(new stmtType[0]);
      return new SemanticData(for_project,file_data,root_node,null,null,null);
    }

   int length = txt.length();
   int skipAtStart = 0;
   if (txt.startsWith(PybaseFileSystem.BOM_UTF8)){
      skipAtStart = PybaseFileSystem.BOM_UTF8.length();
    }
   else if (txt.startsWith(PybaseFileSystem.BOM_UNICODE)){
      skipAtStart = PybaseFileSystem.BOM_UNICODE.length();
    }
   int addAtEnd = 0;
   if (!txt.endsWith("\n") && !txt.endsWith("\r")){
      addAtEnd = 1;
    }

   char []charArray = new char[length-skipAtStart+addAtEnd];
   txt.getChars(skipAtStart, length, charArray, 0);
   if(addAtEnd > 0){
      charArray[charArray.length-1] = '\n';
    }

   CharStream in = new FastCharStream(charArray);
   txt = null;

   Throwable err = null;
   IGrammar grammar = null;
   try {
      switch (for_project.getVersion()) {
	 case VERSION_2_4:
	    grammar = new PythonGrammar24(in);
	    break;
	 case VERSION_2_5:
	    grammar = new PythonGrammar25(in);
	    break;
	 case VERSION_2_6:
	    grammar = new PythonGrammar26(in);
	    break;
	 default:
	 case VERSION_2_7:
	 case DEFAULT :
	    grammar = new PythonGrammar27(in);
	    break;
	 case VERSION_3_0:
	    grammar = new PythonGrammar30(in);
	    break;
       }

      root_node = grammar.file_input(); // parses the file
      err = grammar.getErrorOnParsing();
      if (root_node != null) {
	 ParentSetter ps = new ParentSetter();
	 root_node.accept(ps);
	 EndSetter es = new EndSetter();
	 root_node.accept(es);
	 es.finish();
       }

      if (err == null && root_node != null && semantics) {
	 String nm = file_data.getModuleName();
	 AbstractModule sm = AbstractModule.createModule(root_node,file_data.getFile(),nm);
	 PybaseSemanticVisitor pv = new PybaseSemanticVisitor(for_project,nm,sm,
						 for_project.getPreferences(),
						 file_data.getDocument());
	 root_node.accept(pv);
	 Scope scp = pv.getScope();
	 PybaseScopeItems cur = scp.getCurrScopeItems();
	 PybaseScopeItems gbl = scp.getGlobalScope();
	 sd = new SemanticData(for_project,file_data,root_node,pv.getMessages(),cur,gbl);
       }
    }
   catch (Throwable e) {
      e.printStackTrace();
      if (grammar != null) err = grammar.getErrorOnParsing();
      if (err == null) err = e;
    }

   // if root_node is null, considerusing fast parser to create a dummy ast

   if (err != null) {
      PybaseMessage emsg = null;
      try {
	 emsg = createErrorMessage(err);
       }
      catch (BadLocationException e) {
	 String msg = err.getMessage();
	 if (msg == null) msg = "Syntax error";
	 emsg = new PybaseMessage(ErrorType.SYNTAX_ERROR,msg,0,0,0,0,for_project.getPreferences());
       }
      sd = new SemanticData(for_project,file_data,root_node,emsg);
    }

   if (sd == null) {
      sd = new SemanticData(for_project,file_data,root_node,null,null,null);
    }

   in = null;
   grammar = null;

   return sd;
}



ISemanticData parseDocumentOnly()
{
   return null;
}




private PybaseMessage createErrorMessage(Throwable error) throws BadLocationException
{
   int errorcolstart = -1;
   int errorcolend = -1;
   int errorlinestart = -1;
   int errorlineend = -1;

   String message = null;
   IDocument doc = file_data.getDocument();
   PybasePreferences pref = for_project.getPreferences();

   if (error instanceof ParseException) {
      ParseException parseErr = (ParseException) error;
      // Figure out where the error is in the document, and create a
      // marker for it
      if (parseErr.currentToken == null){
	 errorlinestart = doc.getLineOfOffset(doc.getLength());
	 errorlineend = errorlinestart;
	 errorcolstart = 0;
	 errorcolend = 0;
       }
      else {
	 Token errorToken = parseErr.currentToken.next != null ? parseErr.currentToken.next : parseErr.currentToken;
	 errorlinestart = errorToken.beginLine;
	 errorlineend = (errorToken.endLine == 0 ? errorlinestart : errorToken.endLine);
	 errorcolstart = errorToken.beginColumn;
	 errorcolend = errorToken.endColumn;
       }
      message = parseErr.getMessage();
    }
   else if (error instanceof TokenMgrError){
      TokenMgrError tokenErr = (TokenMgrError) error;
      errorlinestart = tokenErr.errorLine-1;
      errorlineend = errorlinestart;
      errorcolstart = tokenErr.errorColumn;
      errorcolend = errorcolstart;
      message = tokenErr.getMessage();
    }

   if (message != null) { // prettyprint
      message = message.replaceAll("\\r\\n", " ");
      message = message.replaceAll("\\r", " ");
      message = message.replaceAll("\\n", " ");
    }
   if (message == null) message = "Syntax Error";

   return new PybaseMessage(ErrorType.SYNTAX_ERROR,message,errorlinestart,errorlineend,errorcolstart,errorcolend,pref);
}



/********************************************************************************/
/*										*/
/*	Position helper routines						*/
/*										*/
/********************************************************************************/

private static Point getExtendedStart(SimpleNode n)
{
   int bline = n.beginLine;
   int bcol = n.beginColumn;
   if (n.specialsBefore != null) {
      for (Object o : n.specialsBefore) {
	 if (o instanceof SpecialStr) {
	    SpecialStr ss = (SpecialStr) o;
	    if (ss.beginLine > 0) {
	       if (ss.beginLine < bline || (ss.beginLine == bline && ss.beginCol < bcol)) {
		  bline = ss.beginLine;
		  bcol = ss.beginCol;
		}
	     }
	  }
	 else if (o instanceof SimpleNode) {
	    SimpleNode sn = (SimpleNode) o;
	    if (sn.beginLine > 0) {
	       if (sn.beginLine < bline || (sn.beginLine == bline && sn.beginColumn < bcol)) {
		  bline = sn.beginLine;
		  bcol = sn.beginColumn;
		}
	     }
	  }
       }
    }
   return new Point(bline,bcol);
}





/********************************************************************************/
/*										*/
/*	Semantic data holder							*/
/*										*/
/********************************************************************************/

private static class SemanticData implements ISemanticData {

   private PybaseProject for_project;
   private IFileData for_file;
   private SimpleNode root_node;
   private List<PybaseMessage> message_list;
   private PybaseScopeItems top_scope;
   private PybaseScopeItems global_scope;

   SemanticData(PybaseProject pp,IFileData fd,
		   SimpleNode sn,List<PybaseMessage> msg,PybaseScopeItems cur,PybaseScopeItems gbl) {
      root_node = sn;
      if (msg == null) message_list = new ArrayList<PybaseMessage>();
      else message_list = new ArrayList<PybaseMessage>(msg);
      for_project = pp;
      for_file = fd;

      top_scope = cur;
      global_scope = gbl;
    }

   SemanticData(PybaseProject pp,IFileData fd,SimpleNode sn,PybaseMessage msg) {
      for_project = pp;
      for_file = fd;
      root_node = sn;
      message_list = new ArrayList<PybaseMessage>();
      if (msg != null) message_list.add(msg);
      top_scope = null;
      global_scope = null;
    }

   @Override public SimpleNode getRootNode()		{ return root_node; }
   @Override public List<PybaseMessage> getMessages()	{ return message_list; }
   @Override public PybaseScopeItems getScope() 	{ return top_scope; }
   @Override public PybaseScopeItems getGlobalScope()	{ return global_scope; }
   @Override public IFileData getFileData()		{ return for_file; }
   @Override public PybaseProject getProject()		{ return for_project; }

}	// end of inner class SemanticData


/********************************************************************************/
/*										*/
/*	Parent setting visitor							*/
/*										*/
/********************************************************************************/

private class ParentSetter extends Visitor {

   public Stack<SimpleNode> parent_stack;
   private SimpleNode last_node;

   ParentSetter() {
      parent_stack = new Stack<>();
      last_node = null;
    }

   @Override protected Object unhandled_node(SimpleNode n) throws Exception {
      if (!parent_stack.empty()) {
         if (n.parent != null  && n.parent != parent_stack.peek()) {
            System.err.println("Parent already set for " + n);
         }
         n.parent = parent_stack.peek();
   
         if (n instanceof Print) {		// PRINT not done correctly in parser
            fixMissingKey(n,"print");
          }
         else if (n instanceof Import) {
            fixMissingKey(n,"import");
          }
         else if (n instanceof Compare) {
            Compare cmp = (Compare) n;
            for (int i = 0; i < cmp.comparators.length; ++i) {
               if (cmp.ops[i] == cmpopType.NotIn) {
        	  fixMissingKey(cmp.comparators[i],"not in");
        	}
             }
          }
   
   
         Point pt = getExtendedStart(n);
         int bline = pt.x;
         int bcol = pt.y;
   
         for (SimpleNode p : parent_stack) {
            if (p.beginLine > bline || p.beginLine == 0 ||
        	   (p.beginLine == bline && p.beginColumn > bcol)) {
               p.beginLine = bline;
               p.beginColumn = bcol;
             }
          }
       }
      last_node = n;
      return super.unhandled_node(n);
    }

   @Override public void traverse(SimpleNode n) throws Exception {
      parent_stack.push(last_node);
      super.traverse(n);
      parent_stack.pop();
    }

   private void fixMissingKey(SimpleNode n,String key) {
      if (n.specialsBefore == null) {
	 try {
	    int cline = n.beginLine;
	    int ccol = n.beginColumn;
	    IDocument d = file_data.getDocument();
	    int off = d.getLineOffset(cline-1) + ccol-1;
	    int len = key.length();
	    while (off > 0) {
	       if (d.get(off,len).equals(key)) break;
	       --off;
	       --ccol;
	       if (ccol == 0) {
		  --cline;
		  ccol = d.getLineLength(cline);
		}
	     }
	    SpecialStr ss = new SpecialStr(key,cline,ccol);
	    n.addSpecial(ss,false);
	  }
	 catch (BadLocationException e) {
	    PybaseMain.logE("Problem finding missing key",e);
	  }
       }
    }

}	// end of inner class ParentSetter



private class EndSetter extends Visitor {

   private Stack<SimpleNode> set_stack;

   EndSetter() {
      set_stack = new Stack<>();
    }

   void finish() {
      while (!set_stack.isEmpty()) {
	 SimpleNode sn = set_stack.pop();
	 file_data.setEnd(sn,file_data.getDocument().getLength());
       }
    }

   @Override public void traverse(SimpleNode n) throws Exception {
      try {
	 open_level(n);
	 super.traverse(n);
	 close_level(n);
       }
      catch (Exception e) {
	 PybaseMain.logE("Problem traversing tree for end finding",e);
	 throw e;
       }
    }

   @Override protected void open_level(SimpleNode n) {
      if (n.beginLine > 0) {
	 Point ps = getExtendedStart(n);
	 int bline = ps.x;
	 int bcol = ps.y;
	 file_data.setStart(n,bline,bcol);
	 int off = 0;
	 try {
	    off = file_data.getDocument().getLineOffset(bline-1) + bcol-1-1;
	 }
	 catch (BadLocationException e) {
	    PybaseMain.logE("Problem getting end of item",e);
	    return;
	 }
	 while (!set_stack.isEmpty()) {
	    SimpleNode sn = set_stack.pop();
	    try {
	       List<Object> aft = sn.specialsAfter;
	       if (aft != null && aft.size() > 0) {
		  for (Object o : aft) {
		     if (o instanceof SimpleNode) {
			SimpleNode sn1 = (SimpleNode) o;
			int off1 = file_data.getDocument().getLineOffset(sn1.beginLine-1) + sn1.beginColumn - 1 - 1;
			if (off1 < off) off = off1;
		     }
		     else if (o instanceof SpecialStr) {
			SpecialStr ss1 = (SpecialStr) o;
			int off2 = file_data.getDocument().getLineOffset(ss1.beginLine-1) + ss1.beginCol - 1 - 1;
			if (off2 < off) off = off2;
		     }
		     else {
			System.err.println("HANDLE THIS");
		     }
		  }
	       }
	       while (off > 0) {
		  char ch = file_data.getDocument().getChar(off);
		  // this still doesn't work for things like 'not in' operator
		  if (isIgnore(ch)) --off;
		  else break;
	       }
	       file_data.setEnd(sn,off);
	     }
	    catch (BadLocationException e) {
	       PybaseMain.logE("Problem getting end of item",e);
	     }
	  }
       }
      else {
	 PybaseMain.logD("NO LINE KNOWN FOR " + n);
       }
    }

   private boolean isIgnore(char ch) {
      if (Character.isWhitespace(ch)) return true;
      if (Character.isJavaIdentifierPart(ch)) return false;
      if (ch == '"' || ch == '\'') return false;
      if (ch == ';') return false;
      return true;
   }

   @Override protected void close_level(SimpleNode n) {
      set_stack.push(n);
    }

}	// end of inner class EndSetter








}	// end of class PybaseParser




/* end of PybaseParser.java */

