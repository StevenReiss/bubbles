/********************************************************************************/
/*										*/
/*		NobaseJsdt.java 						*/
/*										*/
/*	JavaScript parsing using Eclipse jsdt package				*/
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



package edu.brown.cs.bubbles.nobase;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;


class NobaseJsdt implements NobaseConstants, NobaseConstants.IParser
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static boolean do_resolve = false;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseJsdt()
{ }



/********************************************************************************/
/*										*/
/*	Parsing methods 							*/
/*										*/
/********************************************************************************/

@Override public ISemanticData parse(NobaseProject proj,NobaseFile fd,boolean lib)
{
   ASTParser parser = ASTParser.newParser(AST.JLS3);
   parser.setSource(fd.getContents().toCharArray());
   if (do_resolve) {
      NobaseMain.logD("Parse with bindings");
      // need to call setProject() and setUnitName() here for this to work
      parser.setResolveBindings(true);
      parser.setBindingsRecovery(true);
    }
   else {
      parser.setResolveBindings(false);
    }
   parser.setStatementsRecovery(true);
   parser.setKind(ASTParser.K_COMPILATION_UNIT);
  
   Map<String,String> opts = new Hashtable<>();
   opts.put(JavaScriptCore.COMPILER_COMPLIANCE,"1.7");
   parser.setCompilerOptions(opts);
      
   JavaScriptUnit cu = (JavaScriptUnit) parser.createAST(null);
   ParseData rslt = new ParseData(proj,fd,cu,lib);

   return rslt;
}




/********************************************************************************/
/*										*/
/*	 Result Data								*/
/*										*/
/********************************************************************************/

private static class ParseData implements ISemanticData {

   private NobaseProject for_project;
   private NobaseFile for_file;
   private List<NobaseMessage> message_list;
   private JavaScriptUnit root_node;
   private boolean is_library;

   ParseData(NobaseProject proj,NobaseFile file,JavaScriptUnit b,boolean lib) {
      for_project = proj;
      for_file = file;
      is_library = lib;
      message_list = new ArrayList<NobaseMessage>();
      // copy errors from error manager
      for_project = proj;
      for_file = file;
      is_library = lib;
      root_node = b;
    }

   @Override public NobaseFile getFileData()		{ return for_file; }
   @Override public NobaseProject getProject()		{ return for_project; }
   @Override public List<NobaseMessage> getMessages()	{ return message_list; }
   @Override public JavaScriptUnit getRootNode()	{ return root_node; }

   @Override public void addMessages(List<NobaseMessage> msgs) {
      if (msgs == null || is_library) return;
      message_list.addAll(msgs);
    }

}	// end of inner class ParseData




}	// end of class NobaseJsdt




/* end of NobaseJsdt.java */

