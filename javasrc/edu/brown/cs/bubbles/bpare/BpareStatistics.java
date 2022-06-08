/********************************************************************************/
/*										*/
/*		BpareStatistics.java						*/
/*										*/
/*	Class holding statistical information about a pattern			*/
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



package edu.brown.cs.bubbles.bpare;

import edu.brown.cs.ivy.jcomp.JcompAst;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


class BpareStatistics implements BpareConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int		num_patterns;
private int		num_total;

private Map<StructuralPropertyDescriptor,Map<String,Double>> sprop_counts;
private double []	aid_counts;
private Map<Class<?>,Double> super_counts;
private double		expect_scale;

private static Map<Class<?>,ASTNode> struct_nodes = new HashMap<Class<?>,ASTNode>();

private static boolean	count_pats = false;

private Map<StructuralPropertyDescriptor,Map<Integer,Double>> list_totals;
private Map<StructuralPropertyDescriptor,EmptyCount> empty_totals;



/********************************************************************************/
/*										*/
/*	Ast Class information							*/
/*										*/
/********************************************************************************/

private static AST dummy_ast;
private static Map<Class<?>,Integer> astid_map = null;

private static Class<?> [] ast_classes = new Class<?> [] {
   AnnotationTypeDeclaration.class,
   AnnotationTypeMemberDeclaration.class,
   AnonymousClassDeclaration.class,
   ArrayAccess.class,
   ArrayCreation.class,
   ArrayInitializer.class,
   ArrayType.class,
   AssertStatement.class,
   Assignment.class,
   Block.class,
   BlockComment.class,
   BooleanLiteral.class,
   BreakStatement.class,
   CastExpression.class,
   CatchClause.class,
   CharacterLiteral.class,
   ClassInstanceCreation.class,
   CompilationUnit.class,
   ConditionalExpression.class,
   ConstructorInvocation.class,
   ContinueStatement.class,
   DoStatement.class,
   EmptyStatement.class,
   EnhancedForStatement.class,
   EnumConstantDeclaration.class,
   EnumDeclaration.class,
   ExpressionStatement.class,
   FieldAccess.class,
   FieldDeclaration.class,
   ForStatement.class,
   IfStatement.class,
   ImportDeclaration.class,
   InfixExpression.class,
   Initializer.class,
   InstanceofExpression.class,
   Javadoc.class,
   LabeledStatement.class,
   LineComment.class,
   MarkerAnnotation.class,
   MemberRef.class,
   MemberValuePair.class,
   MethodDeclaration.class,
   MethodInvocation.class,
   MethodRef.class,
   MethodRefParameter.class,
   Modifier.class,
   NormalAnnotation.class,
   NullLiteral.class,
   NumberLiteral.class,
   PackageDeclaration.class,
   ParameterizedType.class,
   ParenthesizedExpression.class,
   PostfixExpression.class,
   PrefixExpression.class,
   PrimitiveType.class,
   QualifiedName.class,
   QualifiedType.class,
   ReturnStatement.class,
   SimpleName.class,
   SimpleType.class,
   SingleMemberAnnotation.class,
   SingleVariableDeclaration.class,
   StringLiteral.class,
   SuperConstructorInvocation.class,
   SuperFieldAccess.class,
   SuperMethodInvocation.class,
   SwitchCase.class,
   SwitchStatement.class,
   SynchronizedStatement.class,
   TagElement.class,
   TextBlock.class,
   TextElement.class,
   ThisExpression.class,
   ThrowStatement.class,
   TryStatement.class,
   TypeDeclaration.class,
   TypeDeclarationStatement.class,
   TypeLiteral.class,
   TypeParameter.class,
   VariableDeclarationExpression.class,
   VariableDeclarationFragment.class,
   VariableDeclarationStatement.class,
   WhileStatement.class,
   WildcardType.class
};


static {
   dummy_ast = JcompAst.createNewAst();
   astid_map = new HashMap<Class<?>,Integer>();
   Class<?> [] ocls = new Class<?>[ast_classes.length+1];
   for (int i = 0; i < ast_classes.length; ++i) {
      int j = computeNodeId(ast_classes[i]);
      ocls[j] = ast_classes[i];
      astid_map.put(ast_classes[i],j);
    }
   ast_classes = ocls;
}



private static Set<StructuralPropertyDescriptor> use_props;
private static Set<StructuralPropertyDescriptor> ignore_props;
private static Set<StructuralPropertyDescriptor> list_props;
private static Set<StructuralPropertyDescriptor> noexpand_props;

static {
   use_props = new HashSet<StructuralPropertyDescriptor>();
   use_props.add(Modifier.KEYWORD_PROPERTY);
   use_props.add(TypeDeclaration.INTERFACE_PROPERTY);
   use_props.add(InfixExpression.OPERATOR_PROPERTY);
   use_props.add(Assignment.OPERATOR_PROPERTY);
   use_props.add(PostfixExpression.OPERATOR_PROPERTY);
   use_props.add(PrefixExpression.OPERATOR_PROPERTY);

   ignore_props = new HashSet<StructuralPropertyDescriptor>();
   ignore_props.add(AnnotationTypeDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(AnnotationTypeMemberDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(EnumConstantDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(EnumDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(FieldDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(Initializer.JAVADOC_PROPERTY);
   ignore_props.add(MethodDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(PackageDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(TypeDeclaration.JAVADOC_PROPERTY);
   ignore_props.add(PackageDeclaration.ANNOTATIONS_PROPERTY);
   ignore_props.add(Javadoc.TAGS_PROPERTY);

   list_props = new HashSet<StructuralPropertyDescriptor>();
   list_props.add(Block.STATEMENTS_PROPERTY);
   list_props.add(SwitchStatement.STATEMENTS_PROPERTY);

   noexpand_props = new HashSet<StructuralPropertyDescriptor>();
   noexpand_props.add(MethodInvocation.ARGUMENTS_PROPERTY);
   noexpand_props.add(ArrayInitializer.EXPRESSIONS_PROPERTY);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BpareStatistics()
{
   num_patterns = 0;
   num_total = 0;

   aid_counts = new double[ast_classes.length];
   for (int i = 0; i < aid_counts.length; ++i) aid_counts[i] = 0;
   sprop_counts = new HashMap<StructuralPropertyDescriptor,Map<String,Double>>();
   super_counts = new HashMap<Class<?>,Double>();
   list_totals = new HashMap<StructuralPropertyDescriptor,Map<Integer,Double>>();
   empty_totals = new HashMap<StructuralPropertyDescriptor,EmptyCount>();

   expect_scale = 1.0/3e2;
}



/********************************************************************************/
/*										*/
/*	Accumulation methods							*/
/*										*/
/********************************************************************************/

void addPattern(String pat,int ct,boolean isnew)
{
   num_total += ct;

   if (count_pats) {
      if (!isnew) return;
      ct = 1;
    }

   PatternScanner ps = new PatternScanner(pat,ct);
   ps.scan();

   if (isnew) ++num_patterns;
}



void fixup()
{
   double tot = 0;
   for (int i = 0; i < aid_counts.length; ++i) tot += aid_counts[i];
   if (tot > 0) {
      for (int i = 0; i < aid_counts.length; ++i) aid_counts[i] /= tot;
    }

   for (Map<String,Double> m : sprop_counts.values()) {
      tot = 0;
      for (Double d : m.values()) tot += d.doubleValue();
      for (Map.Entry<String,Double> ent : m.entrySet()) {
	 ent.setValue(ent.getValue() / tot);
       }
    }

   for (Map<Integer,Double> m : list_totals.values()) {
      tot = 0;
      for (Double d : m.values()) tot += d.doubleValue();
      for (Map.Entry<Integer,Double> ent : m.entrySet()) {
	 ent.setValue(ent.getValue() / tot);
       }
    }
}



void addAstData(int id,int ct)
{
   aid_counts[id] += ct;
}


void addPropData(String d,StructuralPropertyDescriptor spd,int ct)
{
   Map<String,Double> m = sprop_counts.get(spd);
   if (m == null) {
      m = new HashMap<String,Double>();
      sprop_counts.put(spd,m);
    }
   double c = 0;
   if (m.containsKey(d)) c = m.get(d);
   m.put(d,c+ct);
}



void addListData(StructuralPropertyDescriptor spd,int sz,double ct)
{
   if (spd == null) return;

   Map<Integer,Double> m = list_totals.get(spd);
   if (m == null) {
      m = new TreeMap<Integer,Double>();
      list_totals.put(spd,m);
    }
   double c = 0;
   if (m.containsKey(sz)) c = m.get(sz);
   m.put(sz,c+ct);
}




/********************************************************************************/
/*										*/
/*	Pattern scanning methods						*/
/*										*/
/********************************************************************************/

private class PatternScanner {

   private int pattern_multiplier;
   private String pattern_string;

   PatternScanner(String p,int ct) {
      pattern_multiplier = ct;
      pattern_string = p;
    }

   void scan() {
      try {
	 scanElement(0,null);
       }
      catch (Exception e) {
	 System.err.println("PACA: problem scanning pattern " + pattern_string + ": " + e);
	 e.printStackTrace();
	 System.exit(1);
       }
    }

   private int scanAst(int idx) throws Exception {
      int c = pattern_string.charAt(idx++);
      if (c == '(') c = pattern_string.charAt(idx++);
      int id = 0;
      while (Character.isDigit(c)) {
	 id = id*10+(c-'0');
	 c = pattern_string.charAt(idx++);
       }
      --idx;
      addAstData(id,pattern_multiplier);
      for (StructuralPropertyDescriptor spd : getStructuralProperties(ast_classes[id])) {
	 if (spd.isSimpleProperty()) {
	    idx = scanProperty(idx,spd);
	  }
	 else {
	    idx = scanElement(idx,spd);
	  }
       }
      c = pattern_string.charAt(idx++);
      if (c != ')') throw new Exception("AST ) expected");
      return idx;
    }

   private int scanProperty(int idx,StructuralPropertyDescriptor spd) throws Exception {
      char c = pattern_string.charAt(idx++);
      if (c != '(') throw new Exception("Property ( expected");
      c = pattern_string.charAt(idx++);
      if (c != '=') throw new Exception("Property = expected");
      StringBuffer buf = new StringBuffer();
      for ( ; ; ) {
	 c = pattern_string.charAt(idx++);
	 if (c == ')') break;
	 buf.append(c);
       }
      addPropData(buf.toString(),spd,pattern_multiplier);
      return idx;
    }

   private int scanElement(int idx,StructuralPropertyDescriptor spd) throws Exception {
      char c = pattern_string.charAt(idx++);
      if (c != '(') throw new Exception("Element ( expected " + idx);
      c = pattern_string.charAt(idx);
      updateEmpty(spd,(c == 'Z'));
      if (Character.isDigit(c)) return scanAst(idx);
      else if (c == '(' || c == ')') {
	 int ctr = 0;
	 while (c == '(') {             // list
	    ++ctr;
	    idx = scanElement(idx,spd);
	    c = pattern_string.charAt(idx);
	  }
	 addListData(spd,ctr,pattern_multiplier);
       }
      else if (c == '?' || c == 'Z') {
	 ++idx;
       }
      else if (c == 'N' || c == 'E' || c == 'S' || c == 'T') {
	 if (c == 'N') addAstData(ASTNode.SIMPLE_NAME,pattern_multiplier);
	 ++idx;
	 c = pattern_string.charAt(idx++);
	 if (c != '?') throw new Exception("Any ? expected");
	 c = pattern_string.charAt(idx++);
	 while (Character.isDigit(c)) c = pattern_string.charAt(idx++);
	 --idx;
       }
      c = pattern_string.charAt(idx++);
      if (c != ')') throw new Exception("Element ) expected");
      return idx;
    }

}	// end of subclass PatternScanner




/********************************************************************************/
/*										*/
/*	Methods for removing matches						*/
/*										*/
/********************************************************************************/

String removeMatches(String pat)
{
   String npat = pat.replaceAll("\\?\\d","?");
   if (npat.equals(pat)) return null;
   return npat;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

double scaleMinExpandProb()
{
   return num_patterns;
}


double getExpectation(double prob)
{
   return prob * num_total * expect_scale;
}


void setExpectationScale(double v)
{
   expect_scale = v;
}


double getAstProb(int type,StructuralPropertyDescriptor spd)
{
   double etot = 1;
   double tot = 1;
   if (spd != null && spd.isChildProperty()) {
      ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) spd;
      tot = getSuperProb(cpd.getChildType());
      etot = 1.0 - getEmptyProb(spd);
    }
   else if (spd != null && spd.isChildListProperty()) {
      ChildListPropertyDescriptor clpd = (ChildListPropertyDescriptor) spd;
      tot = getSuperProb(clpd.getElementType());
    }

   return etot * aid_counts[type]/tot;
}



double getPropProb(String prop,StructuralPropertyDescriptor spd)
{
   Map<String,Double> m = sprop_counts.get(spd);
   if (m == null) return 0;

   return m.get(prop);
}


double getSuperProb(Class<?> c)
{
   if (super_counts.containsKey(c)) {
      return super_counts.get(c);
    }

   double tot = 0;
   for (int i = 0; i < ast_classes.length; ++i) {
      Class<?> c1 = ast_classes[i];
      if (c1 == null) continue;
      if (c.isAssignableFrom(c1)) tot += aid_counts[i];
    }
   super_counts.put(c,tot);

   return tot;
}



double getListEmptyProb(StructuralPropertyDescriptor spd)
{
   Map<Integer,Double> m = list_totals.get(spd);
   if (m == null) return -1;
   Double d = m.get(0);
   if (d == null) return 0;
   return d.doubleValue();
}



double getEmptyProb(StructuralPropertyDescriptor spd)
{
   if (spd == null) return 0;
   EmptyCount ec = empty_totals.get(spd);
   if (ec == null) return 0;
   return ec.getEmpty();
}



/********************************************************************************/
/*										*/
/*	Ast methods								*/
/*										*/
/********************************************************************************/

static String getNodeName(Class<?> c)
{
   String s = c.getName();
   int idx = s.lastIndexOf(".");
   if (idx > 0) s = s.substring(idx+1);
   return s;
}


static int getNodeId(Class<?> c)
{
   return astid_map.get(c);
}


static String getNodeName(int id)
{
   return getNodeName(ast_classes[id]);
}



private static int computeNodeId(Class<?> c)
{
   ASTNode n = dummy_ast.createInstance(c);
   return n.getNodeType();
}



static boolean ignoreProperty(StructuralPropertyDescriptor spd)
{
   if (spd == null) return false;
   if (ignore_props.contains(spd)) return true;
   if (spd.isSimpleProperty()) {
      if (!use_props.contains(spd)) return true;
    }

   return false;
}



static boolean useListProperty(StructuralPropertyDescriptor spd)
{
   if (spd == null) return false;

   if (list_props.contains(spd)) return true;

   return false;
}



static boolean expandListProperty(StructuralPropertyDescriptor spd)
{
   if (spd == null) return true;

   if (noexpand_props.contains(spd)) return false;

   return true;
}



static Iterable<StructuralPropertyDescriptor> getStructuralProperties(int id)
{
   return getStructuralProperties(ast_classes[id]);
}



static Iterable<StructuralPropertyDescriptor> getStructuralProperties(Class<?> c)
{
   ASTNode n = struct_nodes.get(c);
   if (n == null) {
      n = dummy_ast.createInstance(c);
      struct_nodes.put(c,n);
    }

   return getStructuralProperties(n);
}



static Iterable<StructuralPropertyDescriptor> getStructuralProperties(ASTNode n)
{
   List<StructuralPropertyDescriptor> rslt = new ArrayList<StructuralPropertyDescriptor>();
   for (Iterator<?> it = n.structuralPropertiesForType().iterator(); it.hasNext(); ) {
      StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) it.next();
      if (!ignoreProperty(spd)) rslt.add(spd);
    }
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Methods for empty counts						*/
/*										*/
/********************************************************************************/

private void updateEmpty(StructuralPropertyDescriptor spd,boolean empty)
{
   if (spd == null || !spd.isChildProperty()) return;
   ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) spd;
   if (cpd.isMandatory()) return;
   EmptyCount ec = empty_totals.get(spd);
   if (ec == null) {
      ec = new EmptyCount();
      empty_totals.put(spd,ec);
    }
   ec.update(empty);
}



private static class EmptyCount {

   int total_count;
   double total_empty;

   EmptyCount() {
      total_count = 0;
      total_empty = 0;
    }

   void update(boolean empty) {
      total_count++;
      if (empty) ++total_empty;
    }

   int getTotal()				{ return total_count; }
   double getEmpty()				{ return total_empty / total_count; }

}	// end of subclass EmptyCount



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void dump()
{
   System.err.println("TOTALS: " + num_patterns + " " + num_total);

   System.err.println("AID COUNTS: ");
   for (int i = 0; i < aid_counts.length; ++i) {
      if (aid_counts[i] != 0) {
	 System.err.println("\t" + i + " " + getNodeName(i) + "\t= " + aid_counts[i]);
       }
    }

   System.err.println("STRUCT PROP COUNTS: ");
   for (Map.Entry<StructuralPropertyDescriptor,Map<String,Double>> ent1 : sprop_counts.entrySet()) {
      System.err.println("\t" + ent1.getKey().getId() + " @ " + ent1.getKey().getNodeClass().getName());
      for (Map.Entry<String,Double> ent : ent1.getValue().entrySet()) {
	 System.err.println("\t    " + ent.getKey() + " = " + ent.getValue());
       }
    }

   System.err.println("LIST PROP COUNTS: ");
   for (Map.Entry<StructuralPropertyDescriptor,Map<Integer,Double>> ent1 : list_totals.entrySet()) {
      System.err.println("\t" + ent1.getKey().getId() + " @ " + ent1.getKey().getNodeClass().getName());
      for (Map.Entry<Integer,Double> ent : ent1.getValue().entrySet()) {
	 System.err.println("\t    " + ent.getKey() + " = " + ent.getValue());
       }
    }

   System.err.println("EMPTY COUNTS: " );
   for (Map.Entry<StructuralPropertyDescriptor,EmptyCount> ent1 : empty_totals.entrySet()) {
      System.err.println("\t" + ent1.getKey().getId() + " @ " + ent1.getKey().getNodeClass().getName());
      System.err.println("\t    " + ent1.getValue().getTotal() + " @ " + ent1.getValue().getEmpty());
    }

   System.err.println();
}





}	// end of class BpareStatistics




/* end of BpareStatistics.java */

