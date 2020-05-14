/********************************************************************************/
/*                                                                              */
/*              RebaseJcompSemantics.java                                       */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.rebase.newjava;

import edu.brown.cs.bubbles.rebase.RebaseConstants;
import edu.brown.cs.bubbles.rebase.RebaseConstants.RebaseSemanticData;
import edu.brown.cs.bubbles.rebase.RebaseException;
import edu.brown.cs.bubbles.rebase.RebaseFile;
import edu.brown.cs.bubbles.rebase.RebaseMessage;
import edu.brown.cs.bubbles.rebase.RebaseUtil;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompMessage;
import edu.brown.cs.ivy.jcomp.JcompSemantics;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


class RebaseJcompSemantics implements RebaseSemanticData, RebaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcompSemantics  base_semantics;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RebaseJcompSemantics(JcompSemantics js) 
{
   base_semantics = js;
}



/********************************************************************************/
/*                                                                              */
/*      Public interface for RebaseSeamntics                                    */
/*                                                                              */
/********************************************************************************/

@Override public RebaseElider createElider() 
{
   return  new RebaseJavaElider();
}

@Override public List<RebaseMessage> getMessages()
{
   List<RebaseMessage> rslt = new ArrayList<RebaseMessage>();
   
   for (JcompMessage jm : base_semantics.getMessages()) {
      RebaseJcompSource src = (RebaseJcompSource) jm.getSource();
      MessageSeverity ms = MessageSeverity.valueOf(jm.getSeverity().toString());
      
      RebaseMessage rm = new RebaseMessage(src.getRebaseFile(),ms,jm.getMessageId(),
            jm.getText(),jm.getLineNumber(),jm.getStartOffset(),jm.getEndOffset());
      rslt.add(rm);
    }
   
   return rslt;      
}



@Override public RebaseFile getFile() 
{
   RebaseJcompSource src = (RebaseJcompSource) base_semantics.getFile();
   
   return src.getRebaseFile();
}


@Override public void reparse() 
{
   base_semantics.reparse();
}


@Override public boolean definesClass(String cls) 
{
   return base_semantics.definesClass(cls);
}



@Override public Set<String> getRelatedPackages() 
{
   return base_semantics.getRelatedPackages();
}




/********************************************************************************/
/*                                                                              */
/*      Compute text regions for Bubbles                                        */
/*                                                                              */
/********************************************************************************/

@Override public void getTextRegions(String text,String cls,boolean pfx,boolean statics,
      boolean compunit,boolean imports,boolean pkgfg,boolean topdecls,
      boolean fields,boolean all,IvyXmlWriter xw) throws RebaseException 
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
	 JcompSymbol elt = null;
	 switch (an.getNodeType()) {
	    case ASTNode.ANNOTATION_TYPE_DECLARATION :
	    case ASTNode.ENUM_DECLARATION :
	    case ASTNode.TYPE_DECLARATION :
	       elt = JcompAst.getDefinition(an);
	       break;
	    case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
	       break;
	    case ASTNode.ENUM_CONSTANT_DECLARATION :
	       elt = JcompAst.getDefinition(an);
	       break;
	    case ASTNode.FIELD_DECLARATION :
	       FieldDeclaration fdecl = (FieldDeclaration) an;
	       for (Iterator<?> it = fdecl.fragments().iterator(); it.hasNext(); ) {
		  VariableDeclarationFragment vdf = (VariableDeclarationFragment) it.next();
		  JcompSymbol velt = JcompAst.getDefinition(vdf);
		  if (velt != null) RebaseJcompProject.outputNameData(getFile(),velt,xw);
		}
	       break;
	    case ASTNode.INITIALIZER :
	       break;
	    case ASTNode.METHOD_DECLARATION :
	       elt = JcompAst.getDefinition(an);
	       break;
	    default :
	       break;
	  }
	 if (elt != null) RebaseJcompProject.outputNameData(getFile(),elt,xw);
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
	 JcompType jt = JcompAst.getJavaType(d);
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
/*                                                                              */
/*      Formatting methods                                                      */
/*                                                                              */
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
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

CompilationUnit getAstNode()
{
   return base_semantics.getRootNode();
}


}       // end of class RebaseJcompSemantics




/* end of RebaseJcompSemantics.java */

