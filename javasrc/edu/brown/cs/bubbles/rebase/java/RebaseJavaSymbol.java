/********************************************************************************/
/*										*/
/*		RebaseJavaSymbol.java						*/
/*										*/
/*	Representation of a Java definition					*/
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
import edu.brown.cs.bubbles.rebase.RebaseFile;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.Collections;
import java.util.List;


abstract class RebaseJavaSymbol implements RebaseConstants.RebaseSymbol, RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

static RebaseJavaSymbol createSymbol(SingleVariableDeclaration n,RebaseJavaTyper typer)
{
   RebaseJavaType jt = RebaseJavaAst.getJavaType(n.getType());
   if (jt == null) System.err.println("NULL TYPE FOR: " + n);
   for (int i = 0; i < n.getExtraDimensions(); ++i) jt = typer.findArrayType(jt);
   if (n.isVarargs()) jt = typer.findArrayType(jt);

   return new VariableSymbol(n,jt,n.getModifiers(),null);
}



static RebaseJavaSymbol createSymbol(VariableDeclarationFragment n,RebaseJavaTyper typer)
{
   RebaseJavaType clstyp = null;
   ASTNode par = n.getParent();
   Type typ = null;
   int mods = 0;
   if (par instanceof FieldDeclaration) {
      FieldDeclaration fd = (FieldDeclaration) par;
      typ = fd.getType();
      mods = fd.getModifiers();
      clstyp = RebaseJavaAst.getJavaType(fd.getParent());
    }
   else if (par instanceof VariableDeclarationExpression) {
      VariableDeclarationExpression ve = (VariableDeclarationExpression) par;
      typ = ve.getType();
      mods = ve.getModifiers();
    }
   else if (par instanceof VariableDeclarationStatement) {
      VariableDeclarationStatement vs = (VariableDeclarationStatement) par;
      typ = vs.getType();
      mods = vs.getModifiers();
    }
   else throw new Error("Unknown parent for variable decl: " + par);

   RebaseJavaType jt = RebaseJavaAst.getJavaType(typ);
   for (int i = 0; i < n.getExtraDimensions(); ++i) {
      if (jt != null) jt = typer.findArrayType(jt);
    }

   if (jt == null)
      System.err.println("NULL TYPE for variable declaration: " + typ);

   return new VariableSymbol(n,jt,mods,clstyp);
}




static RebaseJavaSymbol createSymbol(EnumConstantDeclaration n)
{
   EnumDeclaration ed = (EnumDeclaration) n.getParent();

   return new EnumSymbol(n,RebaseJavaAst.getJavaType(ed));
}



static RebaseJavaSymbol createSymbol(LabeledStatement n)
{
   return new LabelSymbol(n);
}


static RebaseJavaSymbol createSymbol(MethodDeclaration n)
{
   return new MethodSymbol(n);
}


static RebaseJavaSymbol createSymbol(AbstractTypeDeclaration n)
{
   return new TypeSymbol(n);
}


static RebaseJavaSymbol createKnownField(String id,RebaseJavaType typ,RebaseJavaType cls,boolean stat,boolean fnl)
{
   return new KnownField(id,typ,cls,stat,fnl);
}



static RebaseJavaSymbol createKnownMethod(String id,RebaseJavaType typ,RebaseJavaType cls,boolean stat,
				       List<RebaseJavaType> excs,boolean gen)
{
   return new KnownMethod(id,typ,cls,stat,excs,gen);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected RebaseJavaSymbol()
{
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

abstract String getName();

protected String getReportName()		{ return getName(); }


abstract RebaseJavaType getType();
ASTNode getDefinitionNode()			{ return null; }
ASTNode getNameNode()				{ return null; }

boolean isKnown()				{ return false; }

boolean isMethodSymbol()			{ return false; }
boolean isConstructorSymbol()			{ return false; }
boolean isTypeSymbol()				{ return false; }
boolean isEnumSymbol()				{ return false; }
boolean isFieldSymbol() 			{ return false; }
boolean isGenericReturn()			{ return false; }

RebaseSymbolKind getSymbolKind()
{
   if (isConstructorSymbol()) return RebaseSymbolKind.CONSTRUCTOR;
   else if (isMethodSymbol()) return RebaseSymbolKind.METHOD;
   else if (isFieldSymbol()) return RebaseSymbolKind.FIELD;
   else if (isEnumSymbol()) return RebaseSymbolKind.FIELD;
   else if (isTypeSymbol()) {
      RebaseJavaType jt = getType();
      if (jt.isEnumType()) return RebaseSymbolKind.ENUM;
      else if (jt.isInterfaceType()) return RebaseSymbolKind.INTERFACE;
      else if (jt.isClassType()) return RebaseSymbolKind.CLASS;
    }

   return RebaseSymbolKind.NONE;
}

boolean isUsed()				{ return true; }
void noteUsed() 				{ }

boolean isStatic()				{ return true; }
boolean isPrivate()				{ return false; }
boolean isFinal()				{ return false; }
boolean isAbstract()				{ return false; }

Iterable<RebaseJavaType> getExceptions()	      { return Collections.emptyList(); }

RebaseJavaType getClassType()			      { return null; }



String getFullName()
{
   ASTNode dn = getDefinitionNode();
   if (dn != null) {
      for (ASTNode p = dn.getParent(); p != null; p = p.getParent()) {
	 if (p instanceof AbstractTypeDeclaration) {
	    RebaseJavaType jt = RebaseJavaAst.getJavaType(p);
	    return jt.getName() + "." + getName();
	  }
       }
    }

   return getName();
}



String getFullReportName()
{
   ASTNode dn = getDefinitionNode();
   if (dn != null) {
      for (ASTNode p = dn.getParent(); p != null; p = p.getParent()) {
	 if (p instanceof AbstractTypeDeclaration) {
	    RebaseJavaType jt = RebaseJavaAst.getJavaType(p);
	    return jt.getName() + "." + getReportName();
	  }
       }
    }

   return getReportName();
}





String getHandle(RebaseFile rf)                 { return null; }



/********************************************************************************/
/*										*/
/*	AST creation methods							*/
/*										*/
/********************************************************************************/

Statement createDeclaration(AST ast)
{
   VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
   vdf.setName(RebaseJavaAst.getSimpleName(ast,getName()));
   vdf.setInitializer(getType().createDefaultValue(ast));
   VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
   vds.setType(getType().createAstNode(ast));

   return vds;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return getName();
}


void outputNameData(RebaseFile rf,IvyXmlWriter xw)
{
   ASTNode an = getDefinitionNode();
   if (an == null) return;

   xw.begin("ITEM");
   xw.field("PROJECT",rf.getProjectName());
   xw.field("PATH",rf.getFileName());
   CompilationUnit cu = (CompilationUnit) an.getRoot();
   xw.field("QNAME",getFullReportName());
   int spos = cu.getExtendedStartPosition(an);
   int len = cu.getExtendedLength(an);
   xw.field("STARTOFFSET",spos);
   xw.field("LENGTH",len);
   xw.field("ENDOFFSET",spos + len);
   String hdl = getHandle(rf);
   if (hdl != null) xw.field("HANDLE",hdl);
  
   if (rf.getSource() != null) { 
      String s6 = rf.getSource().getS6Source();
      if (s6 != null) xw.field("S6",s6);
    }

   outputLocalNameData(rf,xw);
   xw.end("ITEM");
}



void outputFullName(IvyXmlWriter xw)
{
   xw.begin("FULLYQUALIFIEDNAME");
   xw.field("NAME",getFullReportName());
   RebaseJavaType jt = getType();
   if (jt != null) xw.field("TYPE",jt.getName());
   xw.end("FULLYQUALIFIEDNAME");
}


protected void outputLocalNameData(RebaseFile rf,IvyXmlWriter xw)    { }




/********************************************************************************/
/*										*/
/*	KnownField -- field from known class					*/
/*										*/
/********************************************************************************/

private static class KnownField extends RebaseJavaSymbol {

   private RebaseJavaType class_type;
   private String field_name;
   private RebaseJavaType field_type;
   private boolean is_static;
   private boolean is_final;

   KnownField(String id,RebaseJavaType fty,RebaseJavaType cls,boolean stat,boolean fnl) {
      field_name = id;
      field_type = fty;
      class_type = cls;
      is_static = stat;
      is_final = fnl;
    }

   @Override String getName()			{ return field_name; }
   @Override RebaseJavaType getType()		      { return field_type; }
   @Override boolean isKnown()			{ return true; }
   @Override boolean isFieldSymbol()		{ return true; }
   @Override boolean isStatic()			{ return is_static; }
   @Override boolean isFinal()			{ return is_final; }
   @Override RebaseJavaType getClassType()	      { return class_type; }

}	// end of subtype KnownField




/********************************************************************************/
/*										*/
/*	VariableSymbol -- local variable					*/
/*										*/
/********************************************************************************/

private static class VariableSymbol extends RebaseJavaSymbol {

   private VariableDeclaration ast_node;
   private RebaseJavaType java_type;
   private boolean is_used;

   VariableSymbol(VariableDeclaration n,RebaseJavaType t,int mods,RebaseJavaType clstyp) {
      ast_node = n;
      java_type = t;
      is_used = false;
    }

   @Override String getName()			{ return ast_node.getName().getIdentifier(); }
   @Override RebaseJavaType getType()		{ return java_type; }
   @Override boolean isUsed()			{ return is_used; }
   @Override void noteUsed()			{ is_used = true; }

   @Override ASTNode getDefinitionNode() {
      for (ASTNode p = ast_node; p != null; p = p.getParent()) {
	 switch (p.getNodeType()) {
	    case ASTNode.FIELD_DECLARATION :
	       return p;
	  }
       }
      return null;
    }
   @Override ASTNode getNameNode()		{ return ast_node; }

   @Override boolean isFieldSymbol() {
      for (ASTNode p = ast_node; p != null; p = p.getParent()) {
	 switch (p.getNodeType()) {
	    case ASTNode.FIELD_DECLARATION :
	       return true;
	  }
       }
      return false;
    }

   @Override RebaseSymbolKind getSymbolKind() {
      if (isFieldSymbol()) return RebaseSymbolKind.FIELD;
      return RebaseSymbolKind.LOCAL;
    }

   private int getModifiers() {
      for (ASTNode p = ast_node; p != null; p = p.getParent()) {
	 switch (p.getNodeType()) {
	    case ASTNode.SINGLE_VARIABLE_DECLARATION :
	       SingleVariableDeclaration svd = (SingleVariableDeclaration) p;
	       return svd.getModifiers();
	    case ASTNode.VARIABLE_DECLARATION_STATEMENT :
	       VariableDeclarationStatement vds = (VariableDeclarationStatement) p;
	       return vds.getModifiers();
	    case ASTNode.FOR_STATEMENT :
	       return 0;
	    case ASTNode.FIELD_DECLARATION :
	       FieldDeclaration fd = (FieldDeclaration) p;
	       return fd.getModifiers();
	  }
       }
      return 0;
    }

   @Override boolean isStatic() {
      return Modifier.isStatic(getModifiers());
    }
   @Override boolean isPrivate() {
      return Modifier.isPrivate(getModifiers());
    }
   @Override boolean isFinal() {
      return Modifier.isFinal(getModifiers());
    }

   @Override protected void outputLocalNameData(RebaseFile rf,IvyXmlWriter xw) {
      String typ = (isFieldSymbol() ? "Field" : "Local");
      xw.field("NAME",getName());
      xw.field("TYPE",typ);
      xw.field("FLAGS",getModifiers());
    }

   @Override String getHandle(RebaseFile rf) {
      String pfx = rf.getProjectName() + "#";
      RebaseJavaType jt = getType();
      if (jt == null) return pfx + getName();
      return pfx + jt.getJavaTypeName() + "." + getName();
   }

}	// end of subclass VariableSymbol



/********************************************************************************/
/*										*/
/*	EnumSymbol -- enumeration constant					*/
/*										*/
/********************************************************************************/

private static class EnumSymbol extends RebaseJavaSymbol {

   private EnumConstantDeclaration ast_node;
   private RebaseJavaType java_type;

   EnumSymbol(EnumConstantDeclaration n,RebaseJavaType t) {
      ast_node = n;
      java_type = t;
    }

   @Override String getName()			{ return ast_node.getName().getIdentifier(); }
   @Override RebaseJavaType getType()		{ return java_type; }
   @Override boolean isEnumSymbol()		{ return true; }
   @Override ASTNode getNameNode()		{ return ast_node; }
   @Override RebaseSymbolKind getSymbolKind()	{ return RebaseSymbolKind.FIELD; }

   @Override protected void outputLocalNameData(RebaseFile rf,IvyXmlWriter xw) {
      xw.field("NAME",getName());
      xw.field("TYPE","EnumConstants");
      xw.field("FLAGS",ast_node.getModifiers());
    }

   @Override String getHandle(RebaseFile rf) {
      return rf.getProjectName() + "#" + getFullName() + getType().getJavaTypeName();
   }

}	// end of subclass EnumSymbol





/********************************************************************************/
/*										*/
/*	LabelSymbol -- statement label						*/
/*										*/
/********************************************************************************/

private static class LabelSymbol extends RebaseJavaSymbol {

   private LabeledStatement ast_node;

   LabelSymbol(LabeledStatement n) {
      ast_node = n;
    }

   @Override String getName()			{ return ast_node.getLabel().getIdentifier(); }
   @Override RebaseJavaType getType()		{ return null; }

}	// end of subclass LabelSymbol



/********************************************************************************/
/*										*/
/*	MethodSymbol -- method							*/
/*										*/
/********************************************************************************/

private static class MethodSymbol extends RebaseJavaSymbol {

   private MethodDeclaration ast_node;
   private boolean is_used;

   MethodSymbol(MethodDeclaration n) {
      ast_node = n;
      is_used = false;
    }

   @Override String getName() {
      if (ast_node.isConstructor()) return "<init>";
      return ast_node.getName().getIdentifier();
    }

   @Override protected String getReportName() {
      return ast_node.getName().getIdentifier();
    }

   @Override RebaseJavaType getType()	      { return RebaseJavaAst.getJavaType(ast_node); }

   @Override ASTNode getDefinitionNode()	{ return ast_node; }
   @Override ASTNode getNameNode()	{ return ast_node; }

   @Override boolean isMethodSymbol()	{ return true; }
   @Override boolean isConstructorSymbol() { return ast_node.isConstructor(); }

   @Override boolean isStatic()		{ return Modifier.isStatic(ast_node.getModifiers()); }
   @Override boolean isPrivate()		{ return Modifier.isPrivate(ast_node.getModifiers()); }
   @Override boolean isFinal()		{ return Modifier.isFinal(ast_node.getModifiers()); }
   @Override boolean isAbstract() 	{ return Modifier.isAbstract(ast_node.getModifiers()); }

   @Override boolean isUsed()	{ return is_used; }
   @Override void noteUsed()	{ is_used = true; }

   @Override RebaseJavaType getClassType() {
      return RebaseJavaAst.getJavaType(ast_node.getParent());
    }

   @Override protected void outputLocalNameData(RebaseFile rf,IvyXmlWriter xw) {
      xw.field("NAME",getReportName());
      String typ = (ast_node.isConstructor() ? "Constructor" : "Function");
      xw.field("TYPE",typ);
      xw.field("FLAGS",ast_node.getModifiers());
    }

   @Override String getHandle(RebaseFile rf) {
      return rf.getProjectName() + "#" + getFullName() + getType().getJavaTypeName();
   }

}	// end of subclass MethodSymbol



private static class KnownMethod extends RebaseJavaSymbol {

   private String method_name;
   private RebaseJavaType method_type;
   private boolean is_static;
   private List<RebaseJavaType> declared_exceptions;
   private RebaseJavaType class_type;
   private boolean is_generic;

   KnownMethod(String nm,RebaseJavaType typ,RebaseJavaType cls,boolean stat,List<RebaseJavaType> excs,boolean gen) {
      method_name = nm;
      method_type = typ;
      is_static = stat;
      declared_exceptions = excs;
      class_type = cls;
      is_generic = gen;
    }

   @Override String getName()			{ return method_name; }
   @Override String getFullName() {
      return class_type.getName() + "." + method_name;
   }
   @Override RebaseJavaType getType()		      { return method_type; }
   @Override boolean isKnown()			{ return true; }
   @Override boolean isMethodSymbol()		{ return true; }
   @Override boolean isStatic()			{ return is_static; }
   @Override Iterable<RebaseJavaType> getExceptions()   { return declared_exceptions; }
   @Override RebaseJavaType getClassType()	      { return class_type; }
   @Override boolean isGenericReturn()		{ return is_generic; }
   @Override boolean isConstructorSymbol()	{ return method_name.equals("<init>"); }

}	// end of subclass KnownMethod




/********************************************************************************/
/*										*/
/*	TypeSymbol -- type reference						*/
/*										*/
/********************************************************************************/

private static class TypeSymbol extends RebaseJavaSymbol {

   private AbstractTypeDeclaration ast_node;

   TypeSymbol(AbstractTypeDeclaration n) {
      ast_node = n;
    }

   @Override String getName()			{ return ast_node.getName().getIdentifier(); }
   @Override RebaseJavaType getType()		{ return RebaseJavaAst.getJavaType(ast_node); }

   @Override ASTNode getDefinitionNode()		{ return ast_node; }
   @Override ASTNode getNameNode()		{ return ast_node; }
   @Override boolean isTypeSymbol()		{ return true; }

   @Override String getFullName() {
      RebaseJavaType t = getType();
      if (t != null) return t.getName();
      return ast_node.getName().getIdentifier();
    }

   @Override String getFullReportName()		{ return getFullName(); }

   @Override protected void outputLocalNameData(RebaseFile rf,IvyXmlWriter xw) {
      String typ = "Class";
      if (getType() == null) typ = "Class";
      else if (getType().isInterfaceType()) typ = "Interface";
      else if (getType().isEnumType()) typ = "Enum";
      else if (getType().isThrowable()) typ = "Exception";
      xw.field("NAME",getFullName());
      xw.field("TYPE",typ);
      xw.field("FLAGS",ast_node.getModifiers());
    }

   @Override String getHandle(RebaseFile rf) {
      RebaseJavaType jt = getType();
      if (jt == null) jt = RebaseJavaType.createErrorType();
      return rf.getProjectName() + "#" + jt.getJavaTypeName();
   }

}	// end of subclass TypeSymbol







}	// end of class RebaseJavaSymbol




/* end of RebaseJavaSymbol.java */
