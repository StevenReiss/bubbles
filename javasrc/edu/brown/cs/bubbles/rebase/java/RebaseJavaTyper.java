/********************************************************************************/
/*										*/
/*		RebaseJavaTyper.java						*/
/*										*/
/*	Class to handle type resolution for Java files				*/
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



import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WildcardType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


class RebaseJavaTyper implements RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Instances of this class are only used by a single thread at one time;	*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,String> initial_types;
private Map<String,RebaseJavaType> type_map;

private RebaseJavaContext     type_context;

private static Map<String,RebaseJavaType> system_types = new HashMap<String,RebaseJavaType>();

private final String [] BASE_TYPES = { "byte", "short", "char", "int", "long", "float",
					  "double", "boolean", "void" };

private static Set<String> known_prefix;


static {
   known_prefix = new HashSet<String>();
   known_prefix.add("java");
   known_prefix.add("javax");
   known_prefix.add("org");
   known_prefix.add("com");
   known_prefix.add("sun");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaTyper(RebaseJavaContext ctx)
{
   type_context = ctx;

   initial_types = new HashMap<String,String>();

   type_map = new HashMap<String,RebaseJavaType>();

   findSystemType("java.lang.Object");
   findSystemType("java.lang.Enum");
   findSystemType("java.lang.String");
   findSystemType("java.lang.Class");

   definePrimitive(PrimitiveType.BYTE,"Byte");
   definePrimitive(PrimitiveType.SHORT,"Short");
   definePrimitive(PrimitiveType.CHAR,"Character");
   definePrimitive(PrimitiveType.INT,"Integer");
   definePrimitive(PrimitiveType.LONG,"Long");
   definePrimitive(PrimitiveType.FLOAT,"Float");
   definePrimitive(PrimitiveType.DOUBLE,"Double");
   definePrimitive(PrimitiveType.BOOLEAN,"Boolean");
   definePrimitive(PrimitiveType.VOID,null);

   type_map.put(TYPE_ANY_CLASS,RebaseJavaType.createAnyClassType());
   type_map.put(TYPE_ERROR,RebaseJavaType.createErrorType());

   fixJavaType(RebaseJavaType.createVariableType("?"));

   for (String s : BASE_TYPES) {
      initial_types.put(s,s);
    }
}



private void definePrimitive(PrimitiveType.Code pt,String sys)
{
   String nm = pt.toString().toLowerCase();

   synchronized (system_types) {
      RebaseJavaType ty = system_types.get(nm);
      if (ty == null) {
	 RebaseJavaType jt = null;
	 if (sys != null) {
	    jt = findSystemType("java.lang." + sys);
	  }
	 ty = RebaseJavaType.createPrimitiveType(pt,jt);
	 if (jt != null) jt.setAssociatedType(ty);
	 system_types.put(nm,ty);
       }
      type_map.put(nm,ty);
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

RebaseJavaContext getContext()			      { return type_context; }



RebaseJavaType findType(String nm)
{
   return type_map.get(nm);
}



RebaseJavaType findArrayType(RebaseJavaType base)
{
   String s = base.getName() + "[]";
   RebaseJavaType jt = findType(s);
   if (jt == null) {
      jt = RebaseJavaType.createArrayType(base);
      jt = fixJavaType(jt);
    }
   return jt;
}



RebaseJavaType findSystemType(String nm)
{
   if (nm == null) return null;

   RebaseJavaType jt = type_map.get(nm);
   if (jt != null) return jt;

   int idx = nm.indexOf("<");
   if (idx > 0) {
      String t0 = nm.substring(0,idx);
      int idx1 = nm.lastIndexOf(">");
      String t1 = nm.substring(idx+1,idx1);
      return findParameterizedSystemType(t0,t1);
    }

   idx = nm.lastIndexOf("[]");
   if (idx > 0) {
      String nm0 = nm.substring(0,idx);
      RebaseJavaType bty = findSystemType(nm0);
      if (bty == null) return null;
      return findArrayType(bty);
    }

   jt = type_context.defineKnownType(this,nm);
   if (jt == null) return null;

   jt = fixJavaType(jt);

   return jt;
}



private RebaseJavaType findParameterizedSystemType(String t0,String args)
{
   RebaseJavaType btyp = findSystemType(t0);
   if (btyp == null) return null;

   List<RebaseJavaType> argl = new ArrayList<RebaseJavaType>();

   int st = 0;
   int lvl = 0;
   for (int i = 1; i < args.length(); ++i) {
      char ch = args.charAt(i);
      if (ch == ',' && lvl == 0) {
	 String s0 = args.substring(st,i).trim();
	 RebaseJavaType aty = findSystemType(s0);
	 if (aty == null) return null;
	 argl.add(aty);
	 st = i+1;
       }
      else if (ch == '<') ++lvl;
      else if (ch == '>') --lvl;
    }
   String s0 = args.substring(st).trim();
   RebaseJavaType aty = findSystemType(s0);
   if (aty == null) return null;
   argl.add(aty);

   return RebaseJavaType.createParameterizedType(btyp,argl);
}



Collection<RebaseJavaType> getAllTypes()
{
   return type_map.values();
}



/********************************************************************************/
/*										*/
/*	RebaseJavaType and type mapping maintenance functions			*/
/*										*/
/********************************************************************************/

private void setJavaType(ASTNode n,RebaseJavaType jt)
{
   if (jt == null) return;

   RebaseJavaAst.setJavaType(n,fixJavaType(jt));
}


RebaseJavaType fixJavaType(RebaseJavaType jt)
{
   if (jt == null) return null;

   RebaseJavaType jt1 = type_map.get(jt.getName());
   if (jt1 != null) jt = jt1;
   else type_map.put(jt.getName(),jt);
   return jt;
}



RebaseJavaType defineUserType(String nm,boolean iface,boolean etype,boolean force)
{
   RebaseJavaType jt = type_map.get(nm);
   if (jt != null && jt.isKnownType() && force)
      jt = null;

   if (jt != null) return jt;

   int idx = nm.indexOf(".");
   if (idx >= 0 && !force) {
      String s = nm.substring(0,idx);
      if (known_prefix.contains(s)) {
	 jt = findSystemType(nm);
	 if (jt != null) return jt;
       }
    }
   if (nm.startsWith("edu.brown.cs.s6.runner.Runner")) {
      jt = findSystemType(nm);
      if (jt != null) return jt;
    }

   if (iface) jt = RebaseJavaType.createUnknownInterfaceType(nm);
   else if (etype) jt = RebaseJavaType.createEnumType(nm);
   else jt = RebaseJavaType.createUnknownType(nm);

   type_map.put(nm,jt);

   return jt;
}




/********************************************************************************/
/*										*/
/*	Known class lookup methods						*/
/*										*/
/********************************************************************************/

RebaseJavaSymbol lookupKnownField(String cls,String id)
{
   return type_context.defineKnownField(this,cls,id);
}




RebaseJavaSymbol lookupKnownMethod(String cls,String id,RebaseJavaType argtyp,RebaseJavaType ctyp)
{
   return type_context.defineKnownMethod(this,cls,id,argtyp,ctyp);
}


List<RebaseJavaSymbol> findKnownMethods(String cls)
{
   return type_context.findKnownMethods(this,cls);
}



void defineAll(String cls,RebaseJavaScope scp)
{
   type_context.defineAll(this,cls,scp);
}


/********************************************************************************/
/*										*/
/*	Methods to assign types to a RebaseJava AST				*/
/*										*/
/********************************************************************************/

void assignTypes(RebaseJavaRoot root)
{
   Map<CompilationUnit,Map<String,String>> specmap = new HashMap<CompilationUnit,Map<String,String>>();
   Map<CompilationUnit,List<String>> prefmap = new HashMap<CompilationUnit,List<String>>();

   for (CompilationUnit cu : root.getTrees()) {
      Map<String,String> specificnames = new HashMap<String,String>();
      specmap.put(cu,specificnames);
      List<String> prefixes = new ArrayList<String>();
      prefixes.add("java.lang.");
      prefmap.put(cu,prefixes);

      TypeFinder tf = new TypeFinder(specificnames,prefixes);
      cu.accept(tf);
    }

   for (CompilationUnit cu : root.getTrees()) {
      Map<String,String> knownnames = new HashMap<String,String>(initial_types);
      Map<String,String> specificnames = specmap.get(cu);
      List<String> prefixes = prefmap.get(cu);

      TypeSetter ts = new TypeSetter(knownnames,specificnames,prefixes);
      cu.accept(ts);
    }
}



/********************************************************************************/
/*										*/
/*	Methods for handling parameterized types				*/
/*										*/
/********************************************************************************/

RebaseJavaType getParameterizedReturnType(String ms,String cs,RebaseJavaType ptyp,RebaseJavaType atyp)
{
   int i0 = ms.indexOf(")");
   return getActualType(ms,i0+1,cs,ptyp,atyp);
}



private RebaseJavaType getActualType(String ms,int idx,String cs,RebaseJavaType ptyp,RebaseJavaType atyp)
{
   int dims = 0;

   while (ms.charAt(idx) == '[') {
      ++dims;
      ++idx;
    }

   String rslt = null;
   RebaseJavaType rtyp = null;

   switch (ms.charAt(idx)) {
      case 'B':
	 rslt = "byte";
	 break;
      case 'C':
	 rslt = "char";
	 break;
      case 'D':
	 rslt = "double";
	 break;
      case 'F':
	 rslt = "float";
	 break;
      case 'I':
	 rslt = "int";
	 break;
      case 'J':
	 rslt = "long";
	 break;
      case 'V':
	 rslt = "void";
	 break;
      case 'S':
	 rslt = "short";
	 break;
      case 'Z':
	 rslt = "boolean";
	 break;
      case 'L':
	 int i0 = findTypeEnd(ms,idx);
	 rslt = ms.substring(idx+1,i0);
	 rslt = rslt.replace('/','.');
	 break;
      case 'T' :
	 int i1 = findTypeEnd(ms,idx);
	 String var = ms.substring(idx+1,i1);
	 rtyp = getParamType(var,cs,ptyp,ms,atyp);
	 break;
    }

   if (rslt != null) rtyp = findSystemType(rslt);
   if (rtyp == null) return null;

   for (int i = 0; i < dims; ++i) rtyp = findArrayType(rtyp);

   return rtyp;
}



private RebaseJavaType getParamType(String var,String cs,RebaseJavaType ptyp,String ms,RebaseJavaType atyp)
{
   int idx = 0;
   int i0 = 1;
   while (cs.charAt(i0) != '>') {
      int i1 = cs.indexOf(":",i0);
      String v = cs.substring(i0,i1);
      if (v.equals(var)) {
	 if (ptyp.getComponents().size() >= idx+1) return ptyp.getComponents().get(idx);
       }
      i0 = findTypeEnd(cs,i0) + 1;
      ++idx;
    }

   int ndim = 0;
   idx = 0;
   i0 = ms.indexOf("(") + 1;
   while (ms.charAt(i0) != ')') {
      ndim = 0;
      while (ms.charAt(i0) == '[') {
	 ++ndim;
	 ++i0;
       }
      if (ms.charAt(i0) == 'L') i0 = findTypeEnd(ms,i0);
      else if (ms.charAt(i0) == 'T') {
	 int i1 = i0+1;
	 i0 = findTypeEnd(ms,i0);
	 String v = ms.substring(i1,i0);
	 if (v.equals(var)) break;
       }
      ++i0;
      ++idx;
    }

   if (atyp == null) return null;

   if (idx >= atyp.getComponents().size()) return null;

   RebaseJavaType jt = atyp.getComponents().get(idx);
   while (ndim > 0) {
      if (!jt.isArrayType()) return null;
      jt = jt.getBaseType();
      --ndim;
    }

   return jt;
}



private int findTypeEnd(String s,int idx)
{
   int lvl = 0;
   while (idx < s.length()) {
      char c = s.charAt(idx);
      if (c == '<') ++lvl;
      else if (c == '>') --lvl;
      else if (c == ';' && lvl == 0) break;
      ++idx;
    }

   return idx;
}




/********************************************************************************/
/*										*/
/*	Visitor for defining types						*/
/*										*/
/********************************************************************************/

private class TypeFinder extends ASTVisitor {

   private Map<String,String> specific_names;
   private List<String> prefix_set;
   private String type_prefix;
   private int anon_counter;

   TypeFinder(Map<String,String> s,List<String> p) {
      specific_names = s;
      prefix_set = p;
      type_prefix = null;
      anon_counter = 0;
    }

   @Override public boolean visit(PackageDeclaration t) {
      String pnm = t.getName().getFullyQualifiedName();
      type_prefix = pnm;
      addPrefixTypes(pnm);
      return false;
    }

   @Override public boolean visit(ImportDeclaration t) {
      String inm = t.getName().getFullyQualifiedName();
      if (t.isStatic()) ;
      else if (t.isOnDemand()) addPrefixTypes(inm);
      else addSpecificType(inm);
      return false;
    }

   @Override public boolean visit(TypeDeclaration t) {
      String nm = t.getName().getIdentifier();
      if (type_prefix != null) nm = type_prefix + "." + nm;
      addSpecificType(nm);
      type_prefix = nm;
      RebaseJavaType jt = defineUserType(nm,t.isInterface(),false,true);
      setJavaType(t,jt);
      RebaseJavaSymbol js = RebaseJavaSymbol.createSymbol(t);
      jt.setDefinition(js);
      return true;
    }

   @Override public void endVisit(TypeDeclaration t) {
      int idx = type_prefix.lastIndexOf('.');
      if (idx < 0) type_prefix = null;
      else type_prefix = type_prefix.substring(0,idx);
    }

   @Override public boolean visit(EnumDeclaration t) {
      String nm = t.getName().getIdentifier();
      if (type_prefix != null) nm = type_prefix + "." + nm;
      addSpecificType(nm);
      type_prefix = nm;
      RebaseJavaType jt = defineUserType(nm,false,true,true);
      RebaseJavaType etyp = findSystemType("java.lang.Enum");
      jt.setSuperType(etyp);
      setJavaType(t,jt);
      RebaseJavaSymbol js = RebaseJavaSymbol.createSymbol(t);
      jt.setDefinition(js);
      return true;
    }

   @Override public void endVisit(EnumDeclaration t) {
      int idx = type_prefix.lastIndexOf('.');
      if (idx < 0) type_prefix = null;
      else type_prefix = type_prefix.substring(0,idx);
    }

   @Override public boolean visit(AnonymousClassDeclaration t) {
      String anm = "$00" + (++anon_counter);
      String nm = type_prefix + "." + anm;
      addSpecificType(nm);
      type_prefix = nm;
      RebaseJavaType jt = defineUserType(nm,false,false,true);
      setJavaType(t,jt);
      return true;
    }

   @Override public void endVisit(AnonymousClassDeclaration t) {
      int idx = type_prefix.lastIndexOf('.');
      if (idx < 0) type_prefix = null;
      else type_prefix = type_prefix.substring(0,idx);
    }

   @Override public boolean visit(TypeParameter t) {
      String nm = type_prefix + "." + t.getName().getIdentifier();
      RebaseJavaType jt = RebaseJavaType.createVariableType(nm);
      fixJavaType(jt);
      setJavaType(t.getName(),jt);
      return true;
    }

   private void addPrefixTypes(String pnm) {
      pnm += ".";
      prefix_set.add(pnm);
    }

   private void addSpecificType(String nm) {
      String sfx = nm;
      int idx = nm.lastIndexOf('.');
      if (idx >= 0) sfx = nm.substring(idx+1);
      specific_names.put(sfx,nm);
    }

}	// end of subclass TypeFinder



/********************************************************************************/
/*										*/
/*	Visitor for defining and looking up types				*/
/*										*/
/********************************************************************************/

private class TypeSetter extends ASTVisitor {

   private Map<String,String> known_names;
   private Map<String,String> specific_names;
   private List<String> prefix_set;
   private RebaseJavaType outer_type;
   private String type_prefix;
   private boolean canbe_type;

   TypeSetter(Map<String,String> k,Map<String,String> s,List<String> p) {
      known_names = k;
      specific_names = s;
      prefix_set = p;
      type_prefix = null;
      outer_type = null;
      canbe_type = false;
    }

   @Override public boolean visit(PackageDeclaration t) {
      String pnm = t.getName().getFullyQualifiedName();
      type_prefix = pnm;
      return false;
    }

   @Override public boolean visit(ImportDeclaration t) {
      return false;
    }

   @Override public boolean visit(TypeDeclaration t) {
      String nm = t.getName().getIdentifier();
      if (type_prefix != null) nm = type_prefix + "." + nm;
      type_prefix = nm;
      RebaseJavaType out = outer_type;
      RebaseJavaType jt = RebaseJavaAst.getJavaType(t);
      outer_type = jt;
   
      canbe_type = true;
      visitItem(t.getSuperclassType());
      visitList(t.typeParameters());
      visitList(t.superInterfaceTypes());
      visitItem(t.getName());
      canbe_type = false;
      if (t.modifiers().contains(Modifier.ABSTRACT)) {
         jt.setAbstract(true);
       }
   
      if (type_prefix != null) nm = type_prefix + "." + nm;
      Type sty = t.getSuperclassType();
      if (sty != null && jt != null) jt.setSuperType(RebaseJavaAst.getJavaType(sty));
      for (Iterator<?> it = t.superInterfaceTypes().iterator(); it.hasNext(); ) {
         Type ity = (Type) it.next();
         if (jt != null) jt.addInterface(RebaseJavaAst.getJavaType(ity));
       }
      if (out != null && jt != null) jt.setOuterType(out);
   
      outer_type = out;
   
      int idx = type_prefix.lastIndexOf('.');
      if (idx < 0) type_prefix = null;
      else type_prefix = type_prefix.substring(0,idx);
      
      visitList(t.bodyDeclarations());
      // visitList(t.modifiers());
      
      return false;
    }

   @Override public boolean visit(EnumDeclaration t) {
      String nm = t.getName().getIdentifier();
      if (type_prefix != null) nm = type_prefix + "." + nm;
      type_prefix = nm;

      canbe_type = true;
      visitList(t.superInterfaceTypes());
      visitItem(t.getName());
      canbe_type = false;
      visitList(t.enumConstants());
      visitList(t.bodyDeclarations());
      // visitList(t.modifiers());

      RebaseJavaType jt = RebaseJavaAst.getJavaType(t);
      for (Iterator<?> it = t.superInterfaceTypes().iterator(); it.hasNext(); ) {
	 Type ity = (Type) it.next();
	 jt.addInterface(RebaseJavaAst.getJavaType(ity));
       }

      int idx = type_prefix.lastIndexOf('.');
      if (idx < 0) type_prefix = null;
      else type_prefix = type_prefix.substring(0,idx);

      return false;
    }

   @Override public boolean visit(AnonymousClassDeclaration t) {
      RebaseJavaType jt = RebaseJavaAst.getJavaType(t);
      type_prefix = jt.getName();
      return true;
    }

   @Override public void endVisit(AnonymousClassDeclaration t) {
      int idx = type_prefix.lastIndexOf('.');
      if (idx < 0) type_prefix = null;
      else type_prefix = type_prefix.substring(0,idx);
      RebaseJavaType jt = RebaseJavaAst.getJavaType(t);
      if (t.getParent() instanceof ClassInstanceCreation) {
	 ClassInstanceCreation cic = (ClassInstanceCreation) t.getParent();
	 Type sty = cic.getType();
	 RebaseJavaType xjt = RebaseJavaAst.getJavaType(sty);
	 if (xjt.isInterfaceType()) {
	    jt.setSuperType(findType("java.lang.Object"));
	    jt.addInterface(xjt);
	  }
	 else jt.setSuperType(xjt);
       }
      else if (t.getParent() instanceof EnumConstantDeclaration) {
	 // What do we do here?
       }
    }

   @Override public void endVisit(PrimitiveType t) {
      String nm = t.getPrimitiveTypeCode().toString().toLowerCase();
      RebaseJavaType jt = type_map.get(nm);
      if (jt == null)
	  System.err.println("PRIMITIVE TYPE " + nm + " NOT FOUND");
      setJavaType(t,jt);
    }

   @Override public boolean visit(ParameterizedType t) {
      canbe_type = true;
      visitItem(t.getType());
      visitList(t.typeArguments());
      canbe_type = false;

      RebaseJavaType jt0 = RebaseJavaAst.getJavaType(t.getType());
      List<RebaseJavaType> ljt = new ArrayList<RebaseJavaType>();
      for (Iterator<?> it = t.typeArguments().iterator(); it.hasNext(); ) {
	 Type t1 = (Type) it.next();
	 RebaseJavaType jt2 = RebaseJavaAst.getJavaType(t1);
	 if (jt2 == null) jt2 = RebaseJavaType.createErrorType();
	 ljt.add(jt2);
       }
      RebaseJavaType jt1 = RebaseJavaType.createParameterizedType(jt0,ljt);
      setJavaType(t,jt1);
      return false;
    }

   @Override public boolean visit(ArrayType t) {
      canbe_type = true;
      return true;
    }

   @Override public void endVisit(ArrayType t) {
      RebaseJavaType jt = RebaseJavaAst.getJavaType(t.getElementType());
      for (int i = 0; i < t.getDimensions(); ++i) {
	 jt = RebaseJavaType.createArrayType(jt);
	 jt = fixJavaType(jt);
       }
      setJavaType(t,jt);
    }

   @Override public boolean visit(QualifiedType t) {
      canbe_type = true;
      return true;
    }

   @Override public void endVisit(QualifiedType t) {
      RebaseJavaType t0 = RebaseJavaAst.getJavaType(t.getQualifier());
      String t1 = t0.getName() + "." + t.getName().getIdentifier();
      RebaseJavaType jt = lookupType(t1);
      setJavaType(t,jt);
    }

   @Override public boolean visit(SimpleType t) {
      canbe_type = true;
      visitItem(t.getName());
      canbe_type = false;
      RebaseJavaType jt = RebaseJavaAst.getJavaType(t.getName());
      if (jt == null) {
	 String qnm = t.getName().getFullyQualifiedName();
	 jt = lookupType(qnm);
       }
      setJavaType(t,jt);
      return false;
    }

   @Override public void endVisit(WildcardType t) {
      setJavaType(t,type_map.get("?"));
    }

   @Override public boolean visit(QualifiedName t) {
      boolean ocbt = canbe_type;
      canbe_type = true;
      visitItem(t.getQualifier());
      canbe_type = ocbt;
      String s = t.getFullyQualifiedName();
      RebaseJavaType jt = lookupPossibleType(s);
      if (jt != null) setJavaType(t,jt);
      return false;
    }

   @Override public void endVisit(SimpleName t) {
      String s = t.getFullyQualifiedName();
      RebaseJavaType jt = lookupPossibleType(s);
      if (jt != null) setJavaType(t,jt);
    }

   @Override public boolean visit(MethodDeclaration t) {
      canbe_type = true;
      visitItem(t.getReturnType2());
      visitList(t.thrownExceptionTypes());
      visitList(t.typeParameters());
      canbe_type = false;
      visitList(t.parameters());
      if (t.isConstructor()) canbe_type = true;
      visitItem(t.getName());
      canbe_type = false;
      visitItem(t.getBody());
      visitList(t.modifiers());
   
      RebaseJavaType jt = RebaseJavaAst.getJavaType(t.getReturnType2());
      List<RebaseJavaType> ljt = new ArrayList<RebaseJavaType>();
      for (Iterator<?> it = t.parameters().iterator(); it.hasNext(); ) {
         SingleVariableDeclaration svd = (SingleVariableDeclaration) it.next();
         RebaseJavaType pjt = RebaseJavaAst.getJavaType(svd);
         if (pjt == null) pjt = type_map.get(TYPE_ERROR);
         ljt.add(pjt);
       }
      RebaseJavaType mt = RebaseJavaType.createMethodType(jt,ljt,t.isVarargs());
      mt = fixJavaType(mt);
      setJavaType(t,mt);
   
      return false;
    }

   @Override public boolean visit(FieldDeclaration t) {
      canbe_type = true;
      visitItem(t.getType());
      canbe_type = false;
      visitList(t.fragments());
      return false;
    }

   @Override public boolean visit(VariableDeclarationStatement t) {
      canbe_type = true;
      visitItem(t.getType());
      canbe_type = false;
      visitList(t.fragments());
      visitList(t.modifiers());
      return false;
    }

   @Override public boolean visit(VariableDeclarationExpression t) {
      canbe_type = true;
      visitItem(t.getType());
      canbe_type = false;
      visitList(t.fragments());
      // visitList(t.modifiers());
      return false;
    }

   @Override public boolean visit(SingleVariableDeclaration t) {
      canbe_type = true;
      visitItem(t.getType());
      canbe_type = false;
      visitItem(t.getName());
      visitItem(t.getInitializer());
      RebaseJavaType jt = RebaseJavaAst.getJavaType(t.getType());
      for (int i = 0; i < t.getExtraDimensions(); ++i) jt = findArrayType(jt);
      RebaseJavaAst.setJavaType(t,jt);
      // visitList(t.modifiers());
      return false;
    }

   @Override public boolean visit(CastExpression t) {
      canbe_type = true;
      visitItem(t.getType());
      canbe_type = false;
      visitItem(t.getExpression());
      return false;
    }

   @Override public boolean visit(ClassInstanceCreation t) {
      canbe_type = true;
      visitItem(t.getType());
      visitList(t.typeArguments());
      canbe_type = false;
      visitItem(t.getExpression());
      visitList(t.arguments());
      visitItem(t.getAnonymousClassDeclaration());
      return false;
    }

   @Override public boolean visit(FieldAccess t) {
      canbe_type = true;
      visitItem(t.getExpression());
      canbe_type = false;
      visitItem(t.getName());
      return false;
    }

   @Override public boolean visit(InstanceofExpression t) {
      visitItem(t.getLeftOperand());
      canbe_type = true;
      visitItem(t.getRightOperand());
      canbe_type = false;
      return false;
    }

   @Override public boolean visit(MethodInvocation t) {
      canbe_type = true;
      visitItem(t.getExpression());
      visitList(t.typeArguments());
      canbe_type = false;
      visitItem(t.getName());
      visitList(t.arguments());
      return false;
    }

   @Override public boolean visit(SuperFieldAccess t) {
      canbe_type = true;
      visitItem(t.getQualifier());
      canbe_type = false;
      visitItem(t.getName());
      return false;
    }

   @Override public boolean visit(SuperMethodInvocation t) {
      canbe_type = true;
      visitItem(t.getQualifier());
      visitList(t.typeArguments());
      canbe_type = false;
      visitItem(t.getName());
      visitList(t.arguments());
      return false;
    }

   @Override public boolean visit(ThisExpression t) {
      canbe_type = true;
      visitItem(t.getQualifier());
      canbe_type = false;
      return false;
    }

   @Override public boolean visit(TypeLiteral t) {
      canbe_type = true;
      visitItem(t.getType());
      canbe_type = false;
      return false;
    }

   @Override public boolean visit(MarkerAnnotation t) {
      canbe_type = true;
      visitItem(t.getTypeName());
      canbe_type = false;
      return false;
    }

   @Override public boolean visit(NormalAnnotation t) {
      canbe_type = true;
      visitItem(t.getTypeName());
      canbe_type = false;
      visitList(t.values());
      return false;
    }

   @Override public boolean visit(SingleMemberAnnotation t) {
      canbe_type = true;
      visitItem(t.getTypeName());
      canbe_type = false;
      visitItem(t.getValue());
      return false;
    }

   private void visitItem(ASTNode n) {
      if (n != null) {
	 boolean cbt = canbe_type;
	 n.accept(this);
	 canbe_type = cbt;
       }
    }

   private void visitList(List<?> l) {
      if (l == null) return;
      boolean cbt = canbe_type;
      for (Iterator<?> it = l.iterator(); it.hasNext(); ) {
	 ASTNode n = (ASTNode) it.next();
	 n.accept(this);
	 canbe_type = cbt;
       }
    }

   private RebaseJavaType lookupType(String nm) {
      String s = findTypeName(nm,true);
      RebaseJavaType jt = type_map.get(s);
      if (jt != null) return jt;

      jt = findSystemType(s);
      jt = findSystemType(s);
      if (jt != null && !canbe_type) {
	 System.err.println("FOUND UNEXPECTED TYPE " + s);
       }

      if (jt == null) {
	 jt = fixJavaType(RebaseJavaType.createUnknownType(nm));
	 jt.setUndefined(true);
	 jt.setSuperType(type_map.get("java.lang.Object"));
       }

      return jt;
    }

   private RebaseJavaType lookupPossibleType(String nm) {
      String s = findTypeName(nm,false);
      if (s == null) return null;
      RebaseJavaType jt = type_map.get(s);
      if (jt == null && canbe_type) {
	 jt = findSystemType(s);
       }
      return jt;
    }

   private String findTypeName(String nm,boolean force) {
      String spn = specific_names.get(nm);
      if (spn != null) return spn;
      spn = known_names.get(nm);
      if (spn != null) return spn;

      if (canbe_type && findSystemType(nm) != null) {
	 known_names.put(nm,nm);
	 return nm;
       }

      int idx = nm.lastIndexOf('.');            // look for subtypes
      if (idx >= 0) {
	 String pfx = nm.substring(0,idx);
	 String s = findTypeName(pfx,false);
	 if (s != null) {
	    s += nm.substring(idx);
	    known_names.put(nm,s);
	    return s;
	  }
       }

      for (String p : prefix_set) {		// look for on-demand types
	 String t = p + nm;
	 spn = known_names.get(t);
	 if (spn != null) {
	    known_names.put(nm,spn);
	    return spn;
	  }
	 if (canbe_type && findType(t) != null) {
	    known_names.put(nm,t);
	    return t;
	  }
	 if (canbe_type && findSystemType(t) != null) {
	    known_names.put(nm,t);
	    return t;
	  }
       }

      if (!force) return null;

      if (type_prefix != null && idx < 0) {
	 spn = type_prefix + "." + nm;
       }
      else spn = nm;
      known_names.put(nm,spn);

      return spn;
    }

}	// end of subclass TypeSetter




}	// end of class RebaseJavaTyper





/* end of RebaseJavaTyper.java */
