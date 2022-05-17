/********************************************************************************/
/*										*/
/*		RebaseWordSemantics.java					*/
/*										*/
/*	Get specialized words representing semantics from source file		*/
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



package edu.brown.cs.bubbles.rebase.word;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


class RebaseWordSemantics implements RebaseWordConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CompilationUnit 	ast_root;

private static String		ANON_NAME = "%";
private static String		ABSTRACT_ID = "A_";
private static String		STATIC_ID = "S_";
private static String		PRIVATE_ID = "X_";
private static String		PUBLIC_ID = "";
private static String		PROTECTED_ID = "P_";
private static String		WILDCARD_TYPE = "W";
private static String		CLASS_NAME = "$";
private static String		OUTER_NAME = "^";
private static String		OTHER_CLASS = "@";

private static Set<String>	STANDARD_CLASSES;
private static List<String>	 MAP_CLASSES;

static {
   STANDARD_CLASSES = new HashSet<String>();
   STANDARD_CLASSES.add("String");
   STANDARD_CLASSES.add("Integer");
   STANDARD_CLASSES.add("Double");
   STANDARD_CLASSES.add("Float");
   STANDARD_CLASSES.add("Character");
   STANDARD_CLASSES.add("Byte");
   STANDARD_CLASSES.add("Short");
   STANDARD_CLASSES.add("Number");
   STANDARD_CLASSES.add("Object");
   STANDARD_CLASSES.add("StringBuilder");
   STANDARD_CLASSES.add("StringBuffer");
   STANDARD_CLASSES.add("Thread");
   STANDARD_CLASSES.add("Throwable");
   STANDARD_CLASSES.add("Runnable");
   STANDARD_CLASSES.add("Iterable");
   STANDARD_CLASSES.add("CharSequence");
   STANDARD_CLASSES.add("Collection");
   STANDARD_CLASSES.add("BitSet");
   STANDARD_CLASSES.add("Class");
   STANDARD_CLASSES.add("TimerTask");

   MAP_CLASSES = new ArrayList<String>();
   MAP_CLASSES.add("Exception");
   MAP_CLASSES.add("Error");
   MAP_CLASSES.add("Reader");
   MAP_CLASSES.add("Writer");
   MAP_CLASSES.add("InputStream");
   MAP_CLASSES.add("OutputStream");
   MAP_CLASSES.add("List");
   MAP_CLASSES.add("Map");
   MAP_CLASSES.add("Set");

}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseWordSemantics(String text)
{
   ast_root = null;
   if (text != null) {
      ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
      Map<String,String> options = JavaCore.getOptions();
      JavaCore.setComplianceOptions(JavaCore.VERSION_1_8,options);
      parser.setCompilerOptions(options);
      parser.setKind(ASTParser.K_COMPILATION_UNIT);
      parser.setSource(text.toCharArray());
      ast_root = (CompilationUnit) parser.createAST(null);
    }
}



/********************************************************************************/
/*										*/
/*	Construct words for method signatures					*/
/*										*/
/********************************************************************************/

List<String> getSignatures()
{
   SignatureVisitor sv = new SignatureVisitor();

   if (ast_root != null) {
      ast_root.accept(sv);
    }

   return sv.getResult();
}



private static class SignatureVisitor extends ASTVisitor {

   private List<String> found_methods = new ArrayList<String>();
   private String class_name;
   private Stack<String> outer_classes;
   private StringBuilder structure_buf;
   private Stack<StringBuilder> structure_stack;

   SignatureVisitor() {
      found_methods = new ArrayList<>();
      class_name = null;
      outer_classes = new Stack<>();
      structure_buf = null;
      structure_stack = new Stack<>();
    }

   List<String> getResult()		{ return found_methods; }

   @Override public boolean visit(TypeDeclaration td) {
      startClass(td);
      return true;
    }

   @Override public void endVisit(TypeDeclaration td) {
      endClass();
    }

   @Override public boolean visit(EnumDeclaration td) {
      startClass(td);
      return true;
    }

   @Override public void endVisit(EnumDeclaration td) {
      endClass();
    }

   @Override public boolean visit(AnnotationTypeDeclaration td) {
      startClass(td);
      return true;
    }

   @Override public void endVisit(AnnotationTypeDeclaration td) {
      endClass();
    }

   @Override public boolean visit(AnonymousClassDeclaration td) {
      startClass(null);
      structure_stack.push(structure_buf);
      return true;
    }

   @Override public void endVisit(AnonymousClassDeclaration td) {
      endClass();
      structure_buf = structure_stack.pop();
    }

   private void startClass(AbstractTypeDeclaration atd) {
      if (class_name != null) outer_classes.push(class_name);
      if (atd != null) {
	 class_name = atd.getName().getIdentifier();
       }
      else {
	 class_name = ANON_NAME;
       }
    }

   private void endClass() {
      if (outer_classes.size() > 0) {
	 class_name = outer_classes.pop();
       }
      else class_name = null;
    }

   @Override public boolean visit(MethodDeclaration mtd) {
      // structure_buf = new StringBuilder();
      return true;
    }

   @Override public void endVisit(MethodDeclaration mtd) {
      StringBuffer buf = new StringBuffer();

      int mods = mtd.getModifiers();
      if (Modifier.isAbstract(mods)) buf.append(ABSTRACT_ID);
      if (Modifier.isStatic(mods)) buf.append(STATIC_ID);
      if (Modifier.isPrivate(mods)) buf.append(PRIVATE_ID);
      if (Modifier.isProtected(mods)) buf.append(PROTECTED_ID);
      if (Modifier.isPublic(mods)) buf.append(PUBLIC_ID);

      buf.append("(");
      for (Iterator<?> it = mtd.parameters().iterator(); it.hasNext(); ) {
	 SingleVariableDeclaration svd = (SingleVariableDeclaration) it.next();
	 appendType(buf,svd.getType());
       }
      buf.append(")");
      if (!mtd.isConstructor()) {
	 appendType(buf,mtd.getReturnType2());
       }

      found_methods.add(buf.toString());

      if (structure_buf != null && structure_buf.length() > 0) {
	 found_methods.add(structure_buf.toString());
       }
      structure_buf = null;
    }

   @Override public boolean visit(ForStatement n) {
      if (structure_buf != null) structure_buf.append("F<");
      return true;
    }

   @Override public void endVisit(ForStatement n) {
      if (structure_buf != null) structure_buf.append(">");
    }

   @Override public boolean visit(WhileStatement n) {
      if (structure_buf != null) structure_buf.append("W<");
      return true;
    }

   @Override public void endVisit(WhileStatement n) {
      if (structure_buf != null) structure_buf.append(">");
    }

   @Override public boolean visit(DoStatement n) {
      if (structure_buf != null) structure_buf.append("D<");
      return true;
    }

   @Override public void endVisit(DoStatement n) {
      if (structure_buf != null) structure_buf.append(">");
    }

   private void appendType(StringBuffer buf,Type t) {
      if (t == null) return;
      String s = null;
      if (t.isPrimitiveType()) {
	 s = getPrimitiveType((PrimitiveType) t);
       }
      else if (t.isArrayType()) {
	 ArrayType at = (ArrayType) t;
	 for (int i = 0; i < at.getDimensions(); ++i) buf.append("[");
	 appendType(buf,at.getElementType());
       }
      else if (t.isParameterizedType()) {
	 ParameterizedType pt = (ParameterizedType) t;
	 appendType(buf,pt.getType());
       }
      else if (t.isQualifiedType()) {
	 QualifiedType qt = (QualifiedType) t;
	 s = getObjectType(qt.getQualifier().toString() + "." + qt.getName().getIdentifier());
       }
      else if (t.isSimpleType()) {
	 SimpleType st = (SimpleType) t;
	 s = getObjectType(st.getName().getFullyQualifiedName());
       }
      else if (t.isWildcardType()) {
	 s = WILDCARD_TYPE;
       }
      if (s != null) buf.append(s);
    }

   private String getPrimitiveType(PrimitiveType pt) {
       if (pt.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) return "Z";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.BYTE) return "B";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.CHAR) return "C";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.DOUBLE) return "D";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.FLOAT) return "F";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.INT) return "I";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.LONG) return "L";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.SHORT) return "S";
       else if (pt.getPrimitiveTypeCode() == PrimitiveType.VOID) return "V";
       return null;
    }

   private String getObjectType(String name) {
      String tnm = name;
      int idx = name.lastIndexOf(".");
      if (idx > 0) tnm = name.substring(idx+1);

      if (tnm.equals(class_name)) return CLASS_NAME;
      else if (outer_classes.contains(class_name))  return OUTER_NAME;

      if (STANDARD_CLASSES.contains(tnm)) {
	 return "Q" + tnm + ";";
       }

      for (String map : MAP_CLASSES) {
	 if (tnm.endsWith(map)) {
	    return "Q" + map + ";";
	  }
       }

      return OTHER_CLASS;
    }

}	// end of inner class SignatureVisitor




}	// end of class RebaseWordSemantics




/* end of RebaseWordSemantics.java */

