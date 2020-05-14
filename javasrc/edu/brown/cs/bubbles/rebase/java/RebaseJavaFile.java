/********************************************************************************/
/*										*/
/*		RebaseJavaFile.java						*/
/*										*/
/*	Representation of a Java file for semantic resolution			*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/


package edu.brown.cs.bubbles.rebase.java;


import edu.brown.cs.bubbles.rebase.RebaseConstants;
import edu.brown.cs.bubbles.rebase.RebaseException;
import edu.brown.cs.bubbles.rebase.RebaseFile;
import edu.brown.cs.bubbles.rebase.RebaseMain;
import edu.brown.cs.bubbles.rebase.RebaseMessage;
import edu.brown.cs.bubbles.rebase.RebaseUtil;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


class RebaseJavaFile implements RebaseConstants.RebaseSemanticData, RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseFile		for_file;
private CompilationUnit 	ast_root;
private RebaseJavaRoot		for_project;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaFile(RebaseFile rf)
{
   for_file = rf;
   ast_root = null;
   for_project = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

CompilationUnit getAstNode()
{
   if (ast_root == null) {
      String txt = RebaseMain.getFileContents(for_file);
      if (txt != null) {
	 ASTParser parser = ASTParser.newParser(AST.JLS11);
	 Map<String,String> options = JavaCore.getOptions();
	 JavaCore.setComplianceOptions(JavaCore.VERSION_1_8,options);
	 parser.setCompilerOptions(options);
	 parser.setKind(ASTParser.K_COMPILATION_UNIT);
	 parser.setSource(txt.toCharArray());
	 ast_root = (CompilationUnit) parser.createAST(null);
       }
    }
   return ast_root;
}


@Override public RebaseElider createElider()
{
   return new RebaseJavaElider();
}

@Override public List<RebaseMessage> getMessages()
{
   List<RebaseMessage> rslt = new ArrayList<RebaseMessage>();

   CompilationUnit cu = getAstNode();
   for (IProblem p : cu.getProblems()) {
      MessageSeverity sev = MessageSeverity.NOTICE;
      if (p.isError()) sev = MessageSeverity.ERROR;
      else if (p.isWarning()) sev = MessageSeverity.WARNING;
      RebaseMessage rm = new RebaseMessage(getFile(),sev,
	    p.getID(),p.getMessage(),
	    p.getSourceLineNumber(),
	    p.getSourceStart(),p.getSourceEnd());
      rslt.add(rm);
    }

   ErrorVisitor ev = new ErrorVisitor(rslt);

   cu.accept(ev);

   return rslt;
}



private class ErrorVisitor extends ASTVisitor {

   private List<RebaseMessage> message_list;
   private boolean have_error;
   private Stack<Boolean> error_stack;

   ErrorVisitor(List<RebaseMessage> msgs) {
      message_list = msgs;
      have_error = false;
      error_stack = new Stack<Boolean>();
    }

   @Override public void preVisit(ASTNode n) {
      error_stack.push(have_error);
      have_error = false;
    }

   @Override public void postVisit(ASTNode n) {
      boolean fg = error_stack.pop();
      have_error |= fg;
      if (!have_error) {
	 RebaseJavaType jt = RebaseJavaAst.getExprType(n);
	 if (jt != null && jt.isErrorType()) {
	    addError("Expression error",IProblem.InvalidOperator,n);
	    have_error = true;
	  }
       }
    }

   @Override public boolean visit(SimpleName n) {
      RebaseJavaType jt = RebaseJavaAst.getExprType(n);
      if (jt != null && jt.isErrorType()) {
	 addError("Undefined name: " + n.getIdentifier(),IProblem.UndefinedName,n);
	 have_error = true;
       }
      return true;
    }

   private void addError(String msg,int id,ASTNode n) {
      int start = n.getStartPosition();
      int end = start + n.getLength();
      int line = ast_root.getLineNumber(start);
      RebaseMessage rm = new RebaseMessage(getFile(),MessageSeverity.ERROR,
	    id,msg,line,start,end);
      message_list.add(rm);
    }

}	// end of inner class ErrorVisitor

@Override public RebaseFile getFile()
{
   return for_file;
}

void setRoot(RebaseJavaRoot root)
{
   for_project = root;
}

RebaseJavaRoot getProjectRoot()
{
   return for_project;
}


@Override public void reparse()
{
   ast_root = null;
   for_project.setResolved(false);
}





/********************************************************************************/
/*										*/
/*	Handle Text Region commands						*/
/*										*/
/********************************************************************************/

@Override public void getTextRegions(String text,String cls,boolean pfx,boolean statics,
      boolean compunit,boolean imports,boolean pkgfg,boolean topdecls,boolean fields,
      boolean all,IvyXmlWriter xw)
	throws RebaseException
{
   CompilationUnit cu = getAstNode();
   if (cu == null) throw new RebaseException("Can't get compilation unit for " + getFile().getFileName());

   List<?> typs = cu.types();
   AbstractTypeDeclaration atd = findTypeDecl(cls,typs);
   int start = 0;
   if (atd != null && atd != typs.get(0)) start = cu.getExtendedStartPosition(atd);


   if (compunit) {
      xw.begin("RANGE");
      xw.field("PATH",getFile().getFileName());
      xw.field("START",0);
      int ln = text.length();
      xw.field("END",ln);
      xw.end("RANGE");
    }

   if (pfx && atd != null) {
      int xpos = cu.getExtendedStartPosition(atd);
      int xlen = cu.getExtendedLength(atd);
      int spos = atd.getStartPosition();
      int len = atd.getLength();
      int epos = -1;
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 int apos = cu.getExtendedStartPosition(an);
	 if (epos < 0 || epos >= apos) epos = apos-1;
       }
      if (epos < 0) {		     // no body declarations
	 xw.begin("RANGE");
	 xw.field("PATH",getFile().getFileName());
	 xw.field("START",start);
	 xw.field("END",xpos+xlen);
	 xw.end("RANGE");
       }
      else {
	 xw.begin("RANGE");
	 xw.field("PATH",getFile().getFileName());
	 xw.field("START",start);
	 xw.field("END",epos);
	 xw.end("RANGE");
	 xw.begin("RANGE");
	 xw.field("PATH",getFile().getFileName());
	 xw.field("START",spos+len-1);
	 xw.field("END",xpos+xlen);
	 xw.end("RANGE");
       }
    }

   if (pkgfg) {
      PackageDeclaration pkg = cu.getPackage();
      if (pkg != null) {
	 outputRange(cu,pkg,xw);
       }
    }

   if (imports) {
      for (Iterator<?> it = cu.imports().iterator(); it.hasNext(); ) {
	 ImportDeclaration id = (ImportDeclaration) it.next();
	 outputRange(cu,id,xw);
       }
    }

   if (topdecls && atd != null) {
      int spos = atd.getStartPosition();
      int len = atd.getLength();
      int epos = -1;
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 int apos = cu.getExtendedStartPosition(an);
	 if (epos < 0 || epos >= apos) epos = apos-1;
       }
      if (epos < 0) {		     // no body declarations
	 xw.begin("RANGE");
	 xw.field("PATH",getFile().getFileName());
	 xw.field("START",spos);
	 xw.field("END",spos+len);
	 xw.end("RANGE");
       }
      else {
	 xw.begin("RANGE");
	 xw.field("PATH",getFile().getFileName());
	 xw.field("START",spos);
	 xw.field("END",epos);
	 xw.end("RANGE");
       }
    }

   if ((statics || all) && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 if (an.getNodeType() == ASTNode.INITIALIZER) {
	    outputRange(cu,an,xw);
	  }
       }
    }

   if (fields && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 if (an.getNodeType() == ASTNode.FIELD_DECLARATION) {
	    outputRange(cu,an,xw);
	  }
       }
    }

   if (all && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 RebaseJavaSymbol elt = null;
	 switch (an.getNodeType()) {
	    case ASTNode.ANNOTATION_TYPE_DECLARATION :
	    case ASTNode.ENUM_DECLARATION :
	    case ASTNode.TYPE_DECLARATION :
	       elt = RebaseJavaAst.getDefinition(an);
	       break;
	    case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
	       break;
	    case ASTNode.ENUM_CONSTANT_DECLARATION :
	       elt = RebaseJavaAst.getDefinition(an);
	       break;
	    case ASTNode.FIELD_DECLARATION :
	       FieldDeclaration fdecl = (FieldDeclaration) an;
	       for (Iterator<?> it = fdecl.fragments().iterator(); it.hasNext(); ) {
		  VariableDeclarationFragment vdf = (VariableDeclarationFragment) it.next();
		  RebaseJavaSymbol velt = RebaseJavaAst.getDefinition(vdf);
		  if (velt != null) velt.outputNameData(getFile(),xw);
		}
	       break;
	    case ASTNode.INITIALIZER :
	       break;
	    case ASTNode.METHOD_DECLARATION :
	       elt = RebaseJavaAst.getDefinition(an);
	       break;
	    default :
	       break;
	  }
	 if (elt != null) elt.outputNameData(getFile(),xw);
       }
    }
}




private AbstractTypeDeclaration findTypeDecl(String cls,List<?> typs)
{
   AbstractTypeDeclaration atd = null;
   for (int i = 0; atd == null && i < typs.size(); ++i) {
      if (!(typs.get(i) instanceof AbstractTypeDeclaration)) continue;
      AbstractTypeDeclaration d = (AbstractTypeDeclaration) typs.get(i);
      if (cls != null) {
	 RebaseJavaType jt = RebaseJavaAst.getJavaType(d);
	 if (jt != null && !jt.getName().equals(cls)) {
	    if (cls.startsWith(jt.getName() + ".")) {
	       atd = findTypeDecl(cls,d.bodyDeclarations());
	     }
	    continue;
	  }
       }
      atd = d;
    }

   return atd;
}



private void outputRange(CompilationUnit cu,ASTNode an,IvyXmlWriter xw)
{
   int xpos = cu.getExtendedStartPosition(an);
   int xlen = cu.getExtendedLength(an);
   xw.begin("RANGE");
   xw.field("PATH",getFile().getFileName());
   xw.field("START",xpos);
   xw.field("END",xpos+xlen);
   xw.end("RANGE");
}




/********************************************************************************/
/*										*/
/*	Handle code foramtting							*/
/*										*/
/********************************************************************************/

@Override public void formatCode(String cnts,int spos,int epos,IvyXmlWriter xw)
{
   CodeFormatter cf = ToolFactory.createCodeFormatter(null);

   if (spos < 0) spos = 0;
   if (epos <= 0) epos = cnts.length();
   if (epos <= spos) return;

   TextEdit te = cf.format(CodeFormatter.K_UNKNOWN,cnts,spos,epos-spos,0,null);

   if (te == null) return;

   RebaseUtil.outputTextEdit(te,xw);
}




/********************************************************************************/
/*										*/
/*	Determine if a class is defined in this file				*/
/*										*/
/********************************************************************************/

@Override public boolean definesClass(String cls)
{
   CompilationUnit cu = getAstNode();
   if (cu == null) return false;

   List<?> typs = cu.types();
   AbstractTypeDeclaration atd = findTypeDecl(cls,typs);

   return atd != null;
}

/********************************************************************************/
/*										*/
/*	Handle Finding related packages 					*/
/*										*/
/********************************************************************************/

@Override public Set<String> getRelatedPackages()
{
   Set<String> rslt = new HashSet<String>();
   CompilationUnit cu = getAstNode();

   PackageDeclaration pd = cu.getPackage();
   if (pd != null) {
      String nm = pd.getName().getFullyQualifiedName();
      rslt.add(nm);
    }

   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      if (id.isStatic()) continue;
      String inm = id.getName().getFullyQualifiedName();
      if (!id.isOnDemand()) {
	 int idx = inm.lastIndexOf(".");
	 if (idx < 0) continue;
	 inm = inm.substring(0,idx);
       }
      rslt.add(inm);
    }

   return rslt;
}

}	// end of class RebaseJavaFile




/* end of RebaseJavaFile.java */
