/********************************************************************************/
/*										*/
/*		RebaseJavaType.java						*/
/*										*/
/*	Representation of a Java type						*/
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


import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


abstract class RebaseJavaType implements RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String type_name;
private RebaseJavaScope assoc_scope;
private RebaseJavaType assoc_type;
private RebaseJavaSymbol assoc_symbol;
private boolean is_abstract;


/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

public static RebaseJavaType createPrimitiveType(PrimitiveType.Code pt,RebaseJavaType eqv)
{
   return new PrimType(pt,eqv);
}


public static RebaseJavaType createVariableType(String nm)
{
   return new VarType(nm);
}



public static RebaseJavaType createKnownType(String nm)
{
   return new KnownType(nm);
}


public static RebaseJavaType createUnknownType(String nm)
{
   return new UnknownType(nm);
}



public static RebaseJavaType createKnownInterfaceType(String nm)
{
   return new KnownInterfaceType(nm);
}


public static RebaseJavaType createUnknownInterfaceType(String nm)
{
   return new UnknownInterfaceType(nm);
}



public static RebaseJavaType createEnumType(String nm)
{
   return new EnumType(nm);
}



public static RebaseJavaType createParameterizedType(RebaseJavaType jt,List<RebaseJavaType> ptl)
{
   return new ParamType(jt,ptl);
}



public static RebaseJavaType createArrayType(RebaseJavaType jt)
{
   return new ArrayType(jt);
}



public static RebaseJavaType createMethodType(RebaseJavaType jt,List<RebaseJavaType> aty,boolean varargs)
{
   return new MethodType(jt,aty,varargs);
}



public static RebaseJavaType createAnyClassType()
{
   return new AnyClassType();
}



public static RebaseJavaType createErrorType()
{
   return new ErrorType();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected RebaseJavaType(String s)
{
   type_name = s;
   assoc_scope = null;
   assoc_type = null;
   assoc_symbol = null;
   is_abstract = false;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()				{ return type_name; }

boolean isPrimitiveType()			{ return false; }
boolean isBooleanType() 			{ return false; }
boolean isNumericType() 			{ return false; }
boolean isVoidType()				{ return false; }
boolean isArrayType()				{ return false; }
boolean isParameterizedType()			{ return false; }
boolean isClassType()				{ return false; }
boolean isInterfaceType()			{ return false; }
boolean isThrowable()				{ return false; }
boolean isEnumType()				{ return false; }
boolean isTypeVariable()			{ return false; }
boolean isMethodType()				{ return false; }
boolean isErrorType()				{ return false; }
boolean isKnownType()				{ return false; }
boolean isAnyType()				{ return false; }
boolean isUnknown()				{ return false; }

boolean isAbstract()				{ return is_abstract; }
void setAbstract(boolean fg)			{ is_abstract = fg; }

boolean isUndefined()				{ return false; }
void setUndefined(boolean fg)			{ }

boolean isBaseKnown()				{ return false; }


boolean isContextType() 			{ return false; }
void setContextType(boolean fg) 		{ }

RebaseJavaType getBaseType()			      { return null; }
RebaseJavaType getSuperType()			      { return null; }
Collection<RebaseJavaType> getInterfaces()	      { return null; }

RebaseJavaSymbol getDefinition()		      { return assoc_symbol; }
void setDefinition(RebaseJavaSymbol js) 	      { assoc_symbol = js; }

List<RebaseJavaType> getComponents()		      { return null; }
RebaseJavaType getKnownType(RebaseJavaTyper typr)     { return typr.findType("java.lang.Object"); }
void defineAll(RebaseJavaTyper typer)		      { }

void addInterface(RebaseJavaType jt)		      { }
void setSuperType(RebaseJavaType jt)		      { }
void setOuterType(RebaseJavaType jt)		      { }

void setScope(RebaseJavaScope js)		      { assoc_scope = js; }
RebaseJavaScope getScope()			      { return assoc_scope; }

void setAssociatedType(RebaseJavaType jt)	      { assoc_type = jt; }
RebaseJavaType getAssociatedType()		      { return assoc_type; }


Type createAstNode(AST ast)
{
   return ast.newSimpleType(RebaseJavaAst.getQualifiedName(ast,getName()));
}


Expression createDefaultValue(AST ast)
{
   return null;
}


RebaseJavaSymbol lookupField(RebaseJavaTyper typs,String id)
{
   return lookupField(typs,id,0);
}


RebaseJavaSymbol lookupField(RebaseJavaTyper typs,String id,int lvl)
{
   if (assoc_scope != null) return assoc_scope.lookupVariable(id);

   return null;
}


final RebaseJavaSymbol lookupMethod(RebaseJavaTyper typer,String id,RebaseJavaType atyps)
{
   return lookupMethod(typer,id,atyps,this);
}



protected RebaseJavaSymbol lookupMethod(RebaseJavaTyper typer,String id,RebaseJavaType atyps,RebaseJavaType basetype)
{
   if (assoc_scope != null) return assoc_scope.lookupMethod(id,atyps);

   return null;
}



boolean isCompatibleWith(RebaseJavaType jt)
{
   if (jt == this) return true;
   if (isClassType()) {
      if (jt.getName().equals("java.lang.Object")) return true;
    }

   return false;
}


static RebaseJavaType mergeTypes(RebaseJavaTyper typr,RebaseJavaType jt1,RebaseJavaType jt2)
{
   if (jt1 == null) return jt2;
   if (jt2 == null) return jt1;
   if (jt1 == jt2) return jt1;

   if (jt1.isCompatibleWith(jt2)) return jt2;
   if (jt2.isCompatibleWith(jt1)) return jt1;

   if (!jt1.isPrimitiveType() && !jt2.isPrimitiveType()) {
      RebaseJavaType jt = findCommonParent(typr,jt1,jt2);
      if (jt != null) return jt;
    }
   else if (jt1.isPrimitiveType() && !jt2.isPrimitiveType()) {
      RebaseJavaType jt1a = jt1.getBaseType();
      if (jt1a != null) return mergeTypes(typr,jt1a,jt2);
    }
   else if (!jt1.isPrimitiveType() && jt2.isPrimitiveType()) {
      RebaseJavaType jt2a = jt2.getBaseType();
      if (jt2a != null) return mergeTypes(typr,jt1,jt2a);
    }

   System.err.println("INCOMPATIBLE MERGE: " + jt1 + " & " + jt2);

   return jt1;
}



private static RebaseJavaType findCommonParent(RebaseJavaTyper typr,RebaseJavaType jt1,RebaseJavaType jt2)
{
   jt1.getInterfaces();
   jt2.getInterfaces();
   RebaseJavaType best = typr.findSystemType("java.lang.Object");

   return best;
}




/********************************************************************************/
/*										*/
/*	Known item methods							*/
/*										*/
/********************************************************************************/

protected RebaseJavaSymbol lookupKnownField(RebaseJavaTyper typs,String id)
{
   RebaseJavaSymbol js = null;

   if (assoc_scope != null) js = assoc_scope.lookupVariable(id);

   if (js == null) {
      js = typs.lookupKnownField(getName(),id);
      if (js != null) {
	 assoc_scope.defineVar(js);
       }
    }

   return js;
}



protected RebaseJavaSymbol lookupKnownMethod(RebaseJavaTyper typs,String id,RebaseJavaType mtyp,RebaseJavaType basetype)
{
   RebaseJavaSymbol js = null;

   if (assoc_scope != null) js = assoc_scope.lookupMethod(id,mtyp);

   if (js == null) {
      js = typs.lookupKnownMethod(getName(),id,mtyp,basetype);
      if (js != null && !js.isGenericReturn()) {
	 boolean known = true;
	 for (RebaseJavaType xjt : js.getType().getComponents()) {
	    if (!xjt.isBaseKnown())
	       known = false;
	  }
	 if (!js.getType().getBaseType().isBaseKnown())
	    known = false;
	 if (known) {
	    if (basetype == this || basetype == null || basetype.getScope() == null) {
	       assoc_scope.defineMethod(js);
	     }
	    else {
	       basetype.getScope().defineMethod(js);
	     }
	 }
       }
    }

   return js;
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


abstract String getJavaTypeName();


/********************************************************************************/
/*										*/
/*	Primitive types 							*/
/*										*/
/********************************************************************************/

private static class PrimType extends RebaseJavaType {

   private PrimitiveType.Code type_code;
   private RebaseJavaType object_type;

   PrimType(PrimitiveType.Code tc,RebaseJavaType ot) {
      super(tc.toString().toLowerCase());
      type_code = tc;
      object_type = ot;
    }

   @Override boolean isPrimitiveType()			{ return true; }
   @Override boolean isBooleanType()			{ return type_code == PrimitiveType.BOOLEAN; }
   @Override boolean isNumericType() {
      if (type_code == PrimitiveType.BOOLEAN) return false;
      if (type_code == PrimitiveType.VOID) return false;
      return true;
    }
   @Override boolean isVoidType() 			{ return type_code == PrimitiveType.VOID; }
   @Override boolean isBaseKnown()			{ return true; }
   @Override RebaseJavaType getKnownType(RebaseJavaTyper t)	    { return this; }
   @Override RebaseJavaType getBaseType() 		{ return object_type; }

   @Override boolean isCompatibleWith(RebaseJavaType jt) {
      if (jt == this) return true;
      if (jt == null) return false;
      if (jt.isPrimitiveType()) {
	 PrimType pt = (PrimType) jt;
	 if (type_code == PrimitiveType.BYTE) {
	    if (pt.type_code == PrimitiveType.BYTE || pt.type_code == PrimitiveType.SHORT ||
		   pt.type_code == PrimitiveType.CHAR || pt.type_code == PrimitiveType.INT ||
		   pt.type_code == PrimitiveType.LONG ||
		   pt.type_code == PrimitiveType.FLOAT || pt.type_code == PrimitiveType.DOUBLE)
	       return true;
	  }
	 else if (type_code == PrimitiveType.SHORT || type_code == PrimitiveType.CHAR) {
	    if (pt.type_code == PrimitiveType.SHORT ||
		   pt.type_code == PrimitiveType.CHAR || pt.type_code == PrimitiveType.INT ||
		   pt.type_code == PrimitiveType.LONG ||
		   pt.type_code == PrimitiveType.FLOAT || pt.type_code == PrimitiveType.DOUBLE)
	       return true;
	  }
	 else if (type_code == PrimitiveType.INT) {
	    if (pt.type_code == PrimitiveType.INT ||
		   pt.type_code == PrimitiveType.LONG ||
		   pt.type_code == PrimitiveType.FLOAT || pt.type_code == PrimitiveType.DOUBLE)
	       return true;
	  }
	 else if (type_code == PrimitiveType.LONG) {
	    if (pt.type_code == PrimitiveType.LONG ||
		   pt.type_code == PrimitiveType.FLOAT || pt.type_code == PrimitiveType.DOUBLE)
	       return true;
	  }
	 else if (type_code == PrimitiveType.FLOAT) {
	    if (pt.type_code == PrimitiveType.FLOAT || pt.type_code == PrimitiveType.DOUBLE)
	       return true;
	  }
       }
      else if (object_type != null && jt.getName().startsWith("java.lang.")) {
	 return object_type.isCompatibleWith(jt);
       }
      return false;
    }

   @Override Type createAstNode(AST ast) {
      return ast.newPrimitiveType(type_code);
    }

   @Override Expression createDefaultValue(AST ast) {
      if (type_code == PrimitiveType.VOID) return null;
      else if (type_code == PrimitiveType.BOOLEAN) return ast.newBooleanLiteral(false);
      return RebaseJavaAst.newNumberLiteral(ast,0);
    }

   @Override String getJavaTypeName() {
      if (type_code == PrimitiveType.BYTE) return "B";
      if (type_code == PrimitiveType.CHAR) return "C";
      if (type_code == PrimitiveType.DOUBLE) return "D";
      if (type_code == PrimitiveType.FLOAT) return "F";
      if (type_code == PrimitiveType.INT) return "I";
      if (type_code == PrimitiveType.LONG) return "J";
      if (type_code == PrimitiveType.SHORT) return "S";
      if (type_code == PrimitiveType.VOID) return "V";
      if (type_code == PrimitiveType.BOOLEAN) return "Z";
      return "V";
    }

}	// end of subclass PrimType




/********************************************************************************/
/*										*/
/*	Array types								*/
/*										*/
/********************************************************************************/

private static class ArrayType extends RebaseJavaType {

   private RebaseJavaType base_type;

   ArrayType(RebaseJavaType b) {
      super(b.getName() + "[]");
      base_type = b;
    }

   @Override boolean isArrayType()		{ return true; }
   @Override RebaseJavaType getBaseType() 	      { return base_type; }
   @Override RebaseJavaType getKnownType(RebaseJavaTyper typer) {
      RebaseJavaType bt = base_type.getKnownType(typer);
      if (bt == base_type) return this;
      return typer.findArrayType(bt);
    }

   @Override boolean isBaseKnown()		{ return base_type.isBaseKnown(); }

   @Override boolean isCompatibleWith(RebaseJavaType jt) {
      if (jt == this) return true;
      if (jt.getName().equals("java.lang.Object")) return true;
      if (!jt.isArrayType()) return false;
      return getBaseType().isCompatibleWith(jt.getBaseType());
    }

   @Override protected RebaseJavaSymbol lookupMethod(RebaseJavaTyper typer,String id,RebaseJavaType atyps,RebaseJavaType basetype) {
      RebaseJavaType jt = typer.findType("java.lang.Object");
      return jt.lookupMethod(typer,id,atyps,basetype);
    }

   @Override Type createAstNode(AST ast) {
      return ast.newArrayType(base_type.createAstNode(ast));
    }

   @Override Expression createDefaultValue(AST ast) {
      Expression e1 = ast.newNullLiteral();
      if (base_type.isKnownType() || base_type.isPrimitiveType()) {
	 Type asttyp = createAstNode(ast);
	 CastExpression e2 = ast.newCastExpression();
	 e2.setExpression(e1);
	 e2.setType(asttyp);
	 e1 = e2;
       }
      return e1;
    }

   @Override String getJavaTypeName() {
      return "[" + base_type.getJavaTypeName();
    }

}	// end of subclass ArrayType




/********************************************************************************/
/*										*/
/*	Generic class/interface type						*/
/*										*/
/********************************************************************************/

private static abstract class ClassInterfaceType extends RebaseJavaType {

   private RebaseJavaType super_type;
   private RebaseJavaType outer_type;
   private List<RebaseJavaType> interface_types;
   private boolean is_context;

   protected ClassInterfaceType(String nm) {
      super(nm);
      super_type = null;
      outer_type = null;
      interface_types = null;
      is_context = false;
    }

   @Override void setSuperType(RebaseJavaType t) {
      if (t != this) super_type = t;
   }

   @Override void setOuterType(RebaseJavaType t)		{ outer_type = t; }

   @Override void addInterface(RebaseJavaType t) {
      if (t == null) return;
      if (interface_types == null) interface_types = new ArrayList<RebaseJavaType>();
      else if (interface_types.contains(t)) return;
      else if (t == this) return;
      interface_types.add(t);
    }

   @Override Collection<RebaseJavaType> getInterfaces()	{ return interface_types; }

   @Override boolean isClassType()			{ return true; }

   @Override boolean isBaseKnown()			{ return isKnownType(); }

   @Override RebaseJavaType getSuperType()		{ return super_type; }

   @Override boolean isCompatibleWith(RebaseJavaType jt) {
      if (jt == null) return false;
      if (jt == this) return true;
      if (jt.getName().equals("java.lang.Object")) return true;
      while (jt.isParameterizedType()) jt = jt.getBaseType();
      if (jt.isInterfaceType() && interface_types != null) {
	 for (RebaseJavaType ity : interface_types) {
	    if (ity.isCompatibleWith(jt)) return true;
	  }
       }
      if (super_type != null) {
	 if (super_type.isCompatibleWith(jt)) return true;
       }
      if (jt.isPrimitiveType()) {
	 RebaseJavaType at = getAssociatedType();
	 if (at != null) return at.isCompatibleWith(jt);
       }
      return false;
    }

   @Override RebaseJavaSymbol lookupField(RebaseJavaTyper typs,String id,int lvl) {
      RebaseJavaSymbol js = super.lookupField(typs,id,lvl+1);
      if (js != null) return js;
      if (lvl > 20) return null;
      if (super_type != null) js = super_type.lookupField(typs,id,lvl+1);
      if (js != null) return js;
      if (interface_types != null) {
         for (RebaseJavaType it : interface_types) {
            js = it.lookupField(typs,id,lvl+1);
            if (js != null) return js;
          }
       }
      if (outer_type != null) {
         js = outer_type.lookupField(typs,id,lvl+1);
         if (js != null) return js;
       }
      return null;
    }

   @Override protected RebaseJavaSymbol lookupMethod(RebaseJavaTyper typer,String id,RebaseJavaType atyps,RebaseJavaType basetype) {
      RebaseJavaSymbol js = super.lookupMethod(typer,id,atyps,basetype);
      if (js != null) return js;
      if (super_type != null) {
	 if (!id.equals("<init>"))
	    js = super_type.lookupMethod(typer,id,atyps,basetype);
       }
      else if (!getName().equals("java.lang.Object")) {
	 RebaseJavaType ot = typer.findType("java.lang.Object");
	 js = ot.lookupMethod(typer,id,atyps,basetype);
       }
      if (js != null) return js;
      if (interface_types != null) {
	 for (RebaseJavaType it : interface_types) {
	    js = it.lookupMethod(typer,id,atyps,basetype);
	    if (js != null) return js;
	  }
       }
      if (outer_type != null && !id.equals("<init>")) {
	 js = outer_type.lookupMethod(typer,id,atyps,basetype);
	 if (js != null) return js;
       }
      return null;
    }

   @Override Expression createDefaultValue(AST ast) {
      Expression e1 = ast.newNullLiteral();
      if (isKnownType()) {
	 String nm = getName();
	 CastExpression e2 = ast.newCastExpression();
	 e2.setExpression(e1);
	 Name tnm = RebaseJavaAst.getQualifiedName(ast,nm);
	 Type ty = ast.newSimpleType(tnm);
	 e2.setType(ty);
	 e1 = e2;
       }
      return e1;
    }

   @Override boolean isContextType()			{ return is_context; }
   @Override void setContextType(boolean fg)		{ is_context = fg; }

   @Override boolean isThrowable() {
      for (RebaseJavaType jt = this; jt != null; jt = jt.getSuperType()) {
	 if (jt.getName().equals("java.lang.Throwable")) return true;
       }
      return false;
    }

   @Override String getJavaTypeName() {
      if (outer_type == null) {
	 return "L" + getName().replace(".","/") + ";";
       }
      String s = outer_type.getJavaTypeName();
      int ln = s.length();
      s = s.substring(1,ln-1);
      String s1 = getName();
      s1 = s1.substring(s.length()).replace(".","$");
      return "L" + s + s1 + ";";
    }

}	// end of innerclass ClassInterfaceType




private static abstract class KnownClassInterfaceType extends ClassInterfaceType {

   KnownClassInterfaceType(String nm) {
      super(nm);
      setScope(new RebaseJavaScopeFixed());
    }

   @Override RebaseJavaSymbol lookupField(RebaseJavaTyper typs,String id,int lvl) {
      return lookupKnownField(typs,id);
    }

   @Override protected RebaseJavaSymbol lookupMethod(RebaseJavaTyper typer,String id,RebaseJavaType atyps,RebaseJavaType basetype) {
      return lookupKnownMethod(typer,id,atyps,basetype);
    }

   @Override RebaseJavaType getKnownType(RebaseJavaTyper typer)	    { return this; }
   @Override boolean isKnownType()			{ return true; }

   @Override void defineAll(RebaseJavaTyper typer) {
      typer.defineAll(getName(),getScope());
    }

}	// end of innerclass KnownClassInterfaceType




private static abstract class UnknownClassInterfaceType extends ClassInterfaceType {

   private Set<String> field_names;
   private Map<String,Set<RebaseJavaType>> method_names;

   UnknownClassInterfaceType(String nm) {
      super(nm);
      field_names = new HashSet<String>();
      method_names = new HashMap<String,Set<RebaseJavaType>>();
    }

   @Override RebaseJavaType getKnownType(RebaseJavaTyper typer) {
      if (getSuperType() != null) return getSuperType().getKnownType(typer);

      RebaseJavaType t0 = typer.findType(TYPE_ANY_CLASS);
      if (getInterfaces() != null) {
	 for (RebaseJavaType ity : getInterfaces()) {
	    RebaseJavaType t1 = ity.getKnownType(typer);
	    if (t1 != t0) return t1;
	  }
       }
      return t0;
    }

   @Override RebaseJavaSymbol lookupField(RebaseJavaTyper typs,String id,int lvl) {
      RebaseJavaSymbol js = super.lookupField(typs,id,lvl+1);
      if (js == null) {
	 field_names.add(id);
       }
      return js;
    }

    @Override protected RebaseJavaSymbol lookupMethod(RebaseJavaTyper typer,String id,RebaseJavaType atyps,RebaseJavaType basetype) {
      RebaseJavaSymbol js = super.lookupMethod(typer,id,atyps,basetype);
      if (js == null && basetype == this) {
	 Set<RebaseJavaType> args = method_names.get(id);
	 if (args == null) {
	    args = new HashSet<RebaseJavaType>();
	    method_names.put(id,args);
	  }
	 atyps = typer.fixJavaType(atyps);
	 args.add(atyps);
       }
      return js;
    }

   @Override boolean isUnknown()					{ return true; }

}	// end of innerclass UnknownClassInterfaceType



/********************************************************************************/
/*										*/
/*	Class types								*/
/*										*/
/********************************************************************************/

private static class UnknownType extends UnknownClassInterfaceType {

   boolean is_undefined;

   UnknownType(String nm) {
      super(nm);
    }

   @Override boolean isUndefined()			{ return is_undefined; }
   @Override void setUndefined(boolean fg)		{ is_undefined = fg; }

}	// end of subclass UnknownType



private static class KnownType extends KnownClassInterfaceType {

   KnownType(String nm) {
      super(nm);
    }

}	// end of subclass KnownType



/********************************************************************************/
/*										*/
/*	Interface types 							*/
/*										*/
/********************************************************************************/

private static class UnknownInterfaceType extends UnknownClassInterfaceType {

   UnknownInterfaceType(String nm) {
      super(nm);
    }

   @Override boolean isInterfaceType()		{ return true; }
   @Override boolean isAbstract() 		{ return true; }

}	// end of subclass UnknownInterfaceType



private static class KnownInterfaceType extends KnownClassInterfaceType {

   KnownInterfaceType(String nm) {
      super(nm);
    }

   @Override boolean isInterfaceType()		{ return true; }
   @Override boolean isAbstract() 		{ return true; }

}	// end of subclass KnownInterfaceType



/********************************************************************************/
/*										*/
/*	Enumeration type							*/
/*										*/
/********************************************************************************/

private static class EnumType extends UnknownClassInterfaceType {

   EnumType(String nm) {
      super(nm);
    }

   @Override boolean isEnumType() 		{ return true; }

}	// end of subclass EnumType



/********************************************************************************/
/*										*/
/*	Parameterized type							*/
/*										*/
/********************************************************************************/

private static class ParamType extends RebaseJavaType {

   private RebaseJavaType base_type;
   private List<RebaseJavaType> type_params;

   ParamType(RebaseJavaType b,Collection<RebaseJavaType> pl) {
      super(buildParamName(b,pl));
      base_type = b;
      type_params = new ArrayList<RebaseJavaType>(pl);
      RebaseJavaScope js = new RebaseJavaScopeFixed();
      setScope(js);
    }

   @Override boolean isParameterizedType()		{ return true; }
   @Override RebaseJavaType getBaseType() 		      { return base_type; }
   @Override boolean isBaseKnown()			{ return base_type.isBaseKnown(); }
   @Override boolean isAbstract() 			{ return base_type.isAbstract(); }

   @Override List<RebaseJavaType> getComponents() 	      { return type_params; }

   private static String buildParamName(RebaseJavaType b,Collection<RebaseJavaType> pl) {
      StringBuffer buf = new StringBuffer();
      buf.append(b.getName());
      buf.append("<");
      int ct = 0;
      for (RebaseJavaType p : pl) {
	 if (ct++ > 0) buf.append(",");
	 buf.append(p.getName());
       }
      buf.append(">");
      return buf.toString();
    }

   @Override boolean isCompatibleWith(RebaseJavaType jt) {
      if (jt.isParameterizedType()) {
	 if (!getBaseType().isCompatibleWith(jt)) return false;
	 ParamType pt = (ParamType) jt;
	 if (type_params.equals(pt.type_params)) return true;
	 return false;
       }
      return getBaseType().isCompatibleWith(jt);
    }

   @Override RebaseJavaType getKnownType(RebaseJavaTyper typer) {
      return base_type.getKnownType(typer);
    }

   @Override RebaseJavaSymbol lookupField(RebaseJavaTyper typs,String id,int lvl) {
      RebaseJavaSymbol js = super.lookupField(typs,id,lvl+1);
      if (js != null) return js;
      return base_type.lookupField(typs,id);
    }

   @Override protected RebaseJavaSymbol lookupMethod(RebaseJavaTyper typer,String id,RebaseJavaType atyps,RebaseJavaType basetype) {
      RebaseJavaSymbol js = super.lookupMethod(typer,id,atyps,basetype);
      if (js != null) return js;
      return base_type.lookupMethod(typer,id,atyps,basetype);
    }

   @Override @SuppressWarnings("unchecked")
   Type createAstNode(AST ast) {
      ParameterizedType pt = ast.newParameterizedType(base_type.createAstNode(ast));
      List<ASTNode> l = pt.typeArguments();
      for (RebaseJavaType jt : type_params) {
	 l.add(jt.createAstNode(ast));
       }
      return pt;
    }

   @Override Expression createDefaultValue(AST ast) {
      return ast.newNullLiteral();
    }

   @Override String getJavaTypeName() {
      String s = base_type.getJavaTypeName();
      s = s.substring(0,s.length()-1);
      s += "<";
      for (RebaseJavaType jt : type_params) {
	 s += jt.getJavaTypeName();
       }
      s += ">;";
      return s;
    }

}	// end of subclase ParamType



/********************************************************************************/
/*										*/
/*	Type Variable								*/
/*										*/
/********************************************************************************/

private static class VarType extends RebaseJavaType {

   VarType(String nm) {
      super(nm);
    }

   @Override boolean isTypeVariable()			{ return true; }
   @Override boolean isClassType()			{ return true; }
   @Override boolean isBaseKnown()			{ return true; }

   @Override Type createAstNode(AST ast) {
      if (getName().equals("?")) return ast.newWildcardType();
      return super.createAstNode(ast);
    }

   @Override String getJavaTypeName() {
      return "T" + getName() + ";";
    }

}	// end of subclass VarType



/********************************************************************************/
/*										*/
/*	Method types								*/
/*										*/
/********************************************************************************/

private static class MethodType extends RebaseJavaType {

   private RebaseJavaType return_type;
   private List<RebaseJavaType> param_types;
   private boolean is_varargs;

   MethodType(RebaseJavaType rt,Collection<RebaseJavaType> atyps,boolean varargs) {
      super(buildMethodTypeName(rt,atyps,varargs));
      return_type = rt;
      param_types = new ArrayList<RebaseJavaType>(atyps);
      is_varargs = varargs;
    }

   @Override boolean isMethodType()			{ return true; }
   @Override RebaseJavaType getBaseType() 		      { return return_type; }

   @Override List<RebaseJavaType> getComponents() 	      { return param_types; }

   private static String buildMethodTypeName(RebaseJavaType r,Collection<RebaseJavaType> pl,boolean varargs) {
      StringBuffer buf = new StringBuffer();
      buf.append("(");
      int ct = 0;
      if (pl != null) {
	 for (RebaseJavaType p : pl) {
	    if (ct++ > 0) buf.append(",");
	    buf.append(p.getName());
	  }
       }
      if (varargs) buf.append("...");
      buf.append(")");
      if (r != null) buf.append(r.getName());

      return buf.toString();
    }

   @Override boolean isCompatibleWith(RebaseJavaType jt) {
      if (jt == this) return true;
      if (jt == null) return false;

      boolean isok = false;
      if (jt.isMethodType()) {
	 MethodType mt = (MethodType) jt;
	 if (mt.param_types.size() == param_types.size()) {
	    isok = true;
	    for (int i = 0; i < param_types.size(); ++i) {
	       isok &= param_types.get(i).isCompatibleWith(mt.param_types.get(i));
	     }
	  }
	 if (!isok && mt.is_varargs && param_types.size() >= mt.param_types.size() -1 &&
		mt.param_types.size() > 0) {
	    isok = true;
	    for (int i = 0; i < mt.param_types.size()-1; ++i) {
	       isok &= param_types.get(i).isCompatibleWith(mt.param_types.get(i));
	     }
	    RebaseJavaType rt = mt.param_types.get(mt.param_types.size()-1);
	    if (rt.isArrayType()) rt = rt.getBaseType();
	    for (int i = mt.param_types.size()-1; i < param_types.size(); ++i) {
	       isok &= param_types.get(i).isCompatibleWith(rt);
	     }
	  }
       }
      return isok;
    }

   @Override Type createAstNode(AST ast)			{ return null; }

   @Override String getJavaTypeName() {
      String s = "(";
      for (RebaseJavaType jt : param_types) {
	 s += jt.getJavaTypeName();
       }
      s += ")";
      if (return_type != null) s += return_type.getJavaTypeName();
      return s;
    }

}	// end of subclase MethodType



/********************************************************************************/
/*										*/
/*	Special types								*/
/*										*/
/********************************************************************************/

private static class AnyClassType extends RebaseJavaType {

   protected AnyClassType() {
      super(TYPE_ANY_CLASS);
    }

   @Override boolean isClassType()			{ return true; }
   @Override boolean isInterfaceType()			{ return true; }
   @Override boolean isAnyType()				{ return true; }
   @Override boolean isBaseKnown()			{ return true; }

   @Override boolean isCompatibleWith(RebaseJavaType jt) {
      if (this == jt) return true;
      if (jt.isClassType() || jt.isInterfaceType() || jt.isEnumType() || jt.isArrayType())
	 return true;
      return false;
    }

   @Override Type createAstNode(AST ast)			{ return null; }

   @Override String getJavaTypeName()		{ return "Ljava/lang/Object;"; }


}	// end of subclass AnyClassType




private static class ErrorType extends RebaseJavaType {

   protected ErrorType() {
      super(TYPE_ERROR);
    }

   @Override boolean isErrorType()			{ return true; }
   @Override boolean isBaseKnown()			{ return true; }

   @Override boolean isCompatibleWith(RebaseJavaType jt) {
      return true;
    }

   @Override Type createAstNode(AST ast)			{ return null; }

   @Override String getJavaTypeName()		{ return "QError"; }

}	// end of subclass ErrorType



}	// end of class RebaseJavaType



/* end of RebaseJavaType.java */
