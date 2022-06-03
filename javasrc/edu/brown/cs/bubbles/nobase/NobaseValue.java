/********************************************************************************/
/*										*/
/*		NobaseValue.java						*/
/*										*/
/*	Representation of a javascript value					*/
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



package edu.brown.cs.bubbles.nobase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.wst.jsdt.core.dom.*;



class NobaseValue implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseType	value_type;
protected Object	known_value;


private static Map<Object,NobaseValue>	value_map = new WeakHashMap<Object,NobaseValue>();
private static Map<NobaseType,NobaseValue> any_map = new WeakHashMap<NobaseType,NobaseValue>();



private static NobaseValue undef_value = new UndefinedValue();
private static NobaseValue null_value = new NullValue();
private static NobaseValue bool_value = new BooleanValue();
private static NobaseValue true_value = new BooleanValue(true);
private static NobaseValue false_value = new BooleanValue(false);
private static NobaseValue number_value = new NumberValue();
private static NobaseValue string_value = new StringValue();
private static NobaseValue function_value = new FunctionValue(null);
private static NobaseValue class_value = new ClassValue(null);

private static NobaseValue unknown_value = new UnknownValue();




/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static NobaseValue createUndefined()		{ return undef_value; }

static NobaseValue createNull() 		{ return null_value; }

static NobaseValue createBoolean()		{ return bool_value; }

static NobaseValue createBoolean(boolean fg)
{
   return (fg ? true_value : false_value);
}

static NobaseValue createNumber()		{ return number_value; }

synchronized static NobaseValue createNumber(Number n)
{
   if (n == null) return createNumber();

   NobaseValue nv = value_map.get(n);
   if (nv == null) {
      nv = new NumberValue(n);
      value_map.put(n,nv);
    }
   return nv;
}


static NobaseValue createNumber(Character c)
{
   if (c == null) return createNumber();

   Long l = Long.valueOf(c.charValue());
   return createNumber(l);
}

static NobaseValue createString()		{ return string_value; }

synchronized static NobaseValue createString(String s)
{
   if (s == null) return createString();

   NobaseValue nv = value_map.get(s);
   if (nv == null) {
      nv = new StringValue(s);
      value_map.put(s,nv);
    }
   return nv;
}


static NobaseValue createObject()		{ return new ObjectValue(); }

static NobaseValue createFunction()		{ return function_value; }

static NobaseValue createNewFunction()
{
   return new FunctionValue(null);
}


static NobaseValue createFunction(ASTNode fc)
{
   if (fc == null) return createFunction();
   NobaseValue nv = value_map.get(fc);
   if (nv == null) {
      nv = new FunctionValue(fc);
      value_map.put(fc,nv);
    }
   return nv;
}

static NobaseValue createClass(ASTNode fc)
{
   if (fc == null) return class_value;
   NobaseValue nv = value_map.get(fc);
   if (nv == null) {
      nv = new ClassValue(fc);
      value_map.put(fc,nv);
    }
   return nv;
}

static NobaseValue createAnyValue()
{
   return createAnyValue(NobaseType.createAnyType());
}


static NobaseValue createAnyValue(NobaseType typ)
{
   if (typ == null) typ = NobaseType.createAnyType();

   synchronized (any_map) {
      NobaseValue nv = any_map.get(typ);
      if (nv == null) {
	 nv = new AnyValue(typ);
	 any_map.put(typ,nv);
       }
      return nv;
    }
}

static NobaseValue createUnknownValue() 	{ return unknown_value; }

static NobaseValue createArrayValue()
{
   return new ArrayValue();
}

static NobaseValue createArrayValue(List<NobaseValue> v)
{
   NobaseValue nv = createArrayValue();
   for (int i = 0; i < v.size(); ++i) {
      NobaseValue idxv = createNumber(i);
      nv.addProperty(idxv,v.get(i));
    }
   return nv;
}



static NobaseValue mergeValues(NobaseValue t1,NobaseValue t2)
{
   if (t1 == t2) return t1;

   if (t1 == null) return t2;
   if (t2 == null) return t1;
   if (t1.isAnyValue() && t1.getType().isAnyType()) return t1;
   if (t2.isAnyValue() && t2.getType().isAnyType()) return t2;
   if (t1.isAnyValue() || t2.isAnyValue()) {
     //  NobaseType trslt = NobaseType.createUnion(t1.getType(),t2.getType());
      return createAnyValue();
    }
   if (t1 == unknown_value) return t2;
   if (t2 == unknown_value) return t1;
   if (t1.getType() == t2.getType()) return createAnyValue(t1.getType());

   // create proper merge

   //  NobaseType trslt = NobaseType.createUnion(t1.getType(),t2.getType());
   // return createAnyValue(trslt);

   return createAnyValue();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected NobaseValue(NobaseType typ)
{
   value_type = typ;
   known_value = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

NobaseType getType()					{ return value_type; }

long getHashValue()
{
   return hashCode();
}

void setBaseValue(NobaseValue typ)			{ }

boolean addProperty(Object prop,NobaseValue val)		{ return false; }
boolean addGetterProperty(Object prop,NobaseValue val)		{ return false; }
boolean addSetterProperty(Object prop,NobaseValue val)		{ return false; }
void setHasOtherProperties()				{ }
NobaseValue getProperty(Object name,boolean lhs)	{ return null; }
boolean mergeProperties(NobaseValue nv) 		   { return false; }

void addDefinition(ASTNode fct) 	{ }
Collection<NobaseSymbol> getDefinitions()		{ return new ArrayList<NobaseSymbol>(); }
NobaseScope getAssociatedScope()                        { return null; }
void setEvaluator(Evaluator eval)			{ }
NobaseValue evaluate(NobaseFile forfile,List<NobaseValue> args,NobaseValue thisval) {
   return createUnknownValue();
}

void setReturnValue(NobaseValue value)			{ }

Collection<Object> getKnownProperties() 		{ return new ArrayList<Object>(); }

void setConstructor()					{ }


Object getKnownValue()					{ return known_value; }
boolean isKnown()					{ return known_value != null; }
boolean isAnyValue()					{ return false; }
boolean isFunction()					{ return false; }
boolean isClass()                                       { return false; }

String toShortString()                                  { return toString(); }



/********************************************************************************/
/*										*/
/*	Any Value								*/
/*										*/
/********************************************************************************/

private static class AnyValue extends NobaseValue {

   AnyValue(NobaseType typ) {
      super(typ);
      known_value = KnownValue.ANY;
    }

   @Override boolean isAnyValue()				{ return true; }
   
   @Override public String toString()                           { return "*ANY*"; }

}	// end of inner class AnyValue





/********************************************************************************/
/*										*/
/*	Unknown Value								*/
/*										*/
/********************************************************************************/

private static class UnknownValue extends NobaseValue {

   UnknownValue() {
      super(NobaseType.createAnyType());
      known_value = KnownValue.UNKNOWN;
    }
   
   @Override public String toString()                           { return "*UNKNOWN*"; }

}	// end of inner class AnyValue





/********************************************************************************/
/*										*/
/*	Undefined type								*/
/*										*/
/********************************************************************************/

private static class UndefinedValue extends NobaseValue {

   UndefinedValue() {
      super(NobaseType.createUndefined());
      known_value = KnownValue.UNDEFINED;
    }
   
   @Override public String toString()                           { return "*UNDEFINED*"; }
   
}	// end of inner class UndefinedValue



/********************************************************************************/
/*										*/
/*	Null type								*/
/*										*/
/********************************************************************************/

private static class NullValue extends NobaseValue {

   NullValue() {
      super(NobaseType.createNull());
      known_value = KnownValue.NULL;
    }
   
   @Override public String toString()                           { return "*NULL*"; }
   
}	// end of inner class NullValue



/********************************************************************************/
/*										*/
/*	Boolean type								*/
/*										*/
/********************************************************************************/

private static class BooleanValue extends NobaseValue {

   BooleanValue() {
      super(NobaseType.createBoolean());
    }

   BooleanValue(boolean fg) {
      this();
      known_value = Boolean.valueOf(fg);
    }
   
   @Override public String toString() {
      if (known_value ==  null) return "*BOOLEAN*";
      else return "*" + known_value.toString().toUpperCase() + "*";
    }

}	// end of inner class BooleanValue



/********************************************************************************/
/*										*/
/*	String Value								*/
/*										*/
/********************************************************************************/

private static class StringValue extends NobaseValue {

   StringValue() {
      super(NobaseType.createString());
    }

   StringValue(String s) {
      this();
      known_value = s;
    }
   
   @Override public String toString() {
      if (known_value ==  null) return "*STRING*";
      else return "*::" + known_value.toString() + "::*";
    }
   
}	// end of inner class StringValue



/********************************************************************************/
/*										*/
/*	Numeric Values								*/
/*										*/
/********************************************************************************/

private static class NumberValue extends NobaseValue {

   NumberValue() {
      super(NobaseType.createNumber());
    }

   NumberValue(Number n) {
      this();
      known_value = n;
    }
   
   @Override public String toString() {
      if (known_value ==  null) return "*NUMBER*";
      else return "*::" + known_value.toString() + "::*";
    }
   
   
}	// end of inner class NumberValue



/********************************************************************************/
/*										*/
/*	Object Values								 */
/*										*/
/********************************************************************************/

private static class ObjectValue extends NobaseValue {

   private Map<Object,NobaseValue> known_properties;
   private Map<Object,NobaseValue> get_properties;
   private Map<Object,NobaseValue> set_properties;
   private ObjectValue base_value;
   private boolean has_other;
   private NobaseType content_type;

   ObjectValue() {
      super(NobaseType.createObject());
      known_properties = new HashMap<Object,NobaseValue>();
      get_properties = null;
      set_properties = null;
      base_value = null;
      has_other = false;
      content_type = null;
    }

   protected ObjectValue(NobaseType typ) {
      super(typ);
      known_properties = new HashMap<Object,NobaseValue>();
      get_properties = null;
      set_properties = null;
      base_value = null;
      has_other = false;
    }

   @Override void setBaseValue(NobaseValue typ) {
      if (typ instanceof ObjectValue) {
         base_value = (ObjectValue) typ;
       }
    }

   @Override boolean addProperty(Object name,NobaseValue val) {
      if (val != null) {
         if (content_type == null) content_type = val.getType();
         else content_type = content_type.mergeWith(val.getType());
       }
      if (name == null) {
         if (!has_other) {
            setHasOtherProperties();
            return true;
          }
       }
      else {
         NobaseValue oval = known_properties.get(name);
         NobaseValue nval = null;
         NobaseValue modval = known_properties.get("module");
         if (modval == this) {
            nval = val;
          }
         else {
           nval = mergeValues(val,oval);
          }
         known_properties.put(name,nval);
         if (nval != oval) return true;
       }
      return false;
    }

   @Override boolean addGetterProperty(Object name,NobaseValue val) {
      if (val != null) {
	 if (content_type == null) content_type = val.getType();
	 else content_type = content_type.mergeWith(val.getType());
       }
      if (name == null) {
	 if (!has_other) {
	    setHasOtherProperties();
	    return true;
	  }
       }
      else {
	 if (get_properties == null) get_properties = new HashMap<>();
	 NobaseValue oval = get_properties.get(name);
	 NobaseValue nval = mergeValues(val,oval);
	 get_properties.put(name,nval);
	 if (nval != oval) return true;
       }
      return false;
    }

   @Override boolean addSetterProperty(Object name,NobaseValue val) {
      if (val != null) {
	 if (content_type == null) content_type = val.getType();
	 else content_type = content_type.mergeWith(val.getType());
       }
      if (name == null) {
	 if (!has_other) {
	    setHasOtherProperties();
	    return true;
	  }
       }
      else {
	 if (set_properties == null) set_properties = new HashMap<>();
	 NobaseValue oval = set_properties.get(name);
	 NobaseValue nval = mergeValues(val,oval);
	 set_properties.put(name,nval);
	 if (nval != oval) return true;
       }
      return false;
    }

   @Override void setHasOtherProperties() {
      has_other = true;
    }

   @Override Collection<Object> getKnownProperties() {
      return known_properties.keySet();
    }

   @Override NobaseValue getProperty(Object name,boolean lhs) {
      NobaseValue oval = null;
      if (name != null) oval = known_properties.get(name);
      if (oval == null && lhs && set_properties != null && name != null) {
         oval = set_properties.get(name);
       }
      if (oval == null && !lhs && get_properties != null && name != null) {
         oval = get_properties.get(name);
       }
      if (oval != null) return oval;
      if (base_value != null) oval = base_value.getProperty(name,lhs);
      if (oval != null) return oval;
      if (has_other) return createAnyValue(content_type);
      return null;
    }

   @Override boolean mergeProperties(NobaseValue nv) {
      if (nv == null || nv == this) return false;
      boolean chng = false;
      if (nv instanceof ObjectValue) {
         ObjectValue ov = (ObjectValue) nv;
         for (Map.Entry<Object,NobaseValue> ent : ov.known_properties.entrySet()) {
            chng |= addProperty(ent.getKey(),ent.getValue());
          }
         if (ov.get_properties != null) {
            for (Map.Entry<Object,NobaseValue> ent : ov.get_properties.entrySet()) {
               chng |= addGetterProperty(ent.getKey(),ent.getValue());
             }
          }
         if (ov.set_properties != null) {
            for (Map.Entry<Object,NobaseValue> ent : ov.set_properties.entrySet()) {
               chng |= addSetterProperty(ent.getKey(),ent.getValue());
             }
          }
         if (ov.has_other) setHasOtherProperties();
       }
      return chng;
    }

   @Override long getHashValue() {
      // this should change if the known properties changes
      int vl = known_properties.hashCode();
      if (get_properties != null) vl += get_properties.hashCode();
      if (set_properties != null) vl += set_properties.hashCode();
      if (content_type != null) vl += content_type.hashCode();
      return vl;
    }
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("*Object: ");
      buf.append(hashCode());
      buf.append(" {");
      for (Map.Entry<Object,NobaseValue> ent : known_properties.entrySet()) {
         buf.append(ent.getKey());
         buf.append("=");
         buf.append(ent.getValue().toShortString());
         buf.append(", ");
       }
      buf.append("}*");
   
      return buf.toString();
    }
   
   @Override String toShortString() {
      StringBuffer buf = new StringBuffer();
      buf.append("*Object: ");
      buf.append(hashCode());
      buf.append("*");
      return buf.toString();
    }

}	// end of inner class ObjectValue




/********************************************************************************/
/*										*/
/*	Array value								*/
/*										*/
/********************************************************************************/

private static class ArrayValue extends ObjectValue {

   ArrayValue() {
      super(NobaseType.createList());
    }
   
   @Override public String toString() {
      return "*ARRAY*";

    }

}	// end of inner class ArrayValue




/********************************************************************************/
/*										*/
/*	Function value								*/
/*										*/
/********************************************************************************/

private static class FunctionValue extends ObjectValue {

   private Set<ASTNode> function_defs;
   private Evaluator function_evaluator;
   private List<NobaseValue> arg_values;
   private NobaseValue return_value;

   FunctionValue() {
      super(NobaseType.createFunction());
      function_defs = null;
      function_evaluator = null;
      arg_values = null;
      return_value = null;
    }

   FunctionValue(ASTNode fc) {
      this();
      if (fc != null) {
	 addDefinition(fc);
	 arg_values = new ArrayList<NobaseValue>();
       }
    }

   @Override boolean isFunction()			{ return true; }

   @Override void addDefinition(ASTNode fc) {
      if (function_defs == null) function_defs = new HashSet<>();
      function_defs.add(fc);
    }

   @Override void setEvaluator(Evaluator ev) {
      function_evaluator = ev;
    }

   @Override Collection<NobaseSymbol> getDefinitions() {
      List<NobaseSymbol> rslt = new ArrayList<>();
      if (function_defs != null) {
	 for (ASTNode fc : function_defs) {
	    NobaseSymbol ns = NobaseAst.getDefinition(fc);
	    if (ns != null) rslt.add(ns);
	 }
      }
      return rslt;
   }
  
   @Override NobaseScope getAssociatedScope() {
      if (function_defs != null) {
         for (ASTNode fc : function_defs) { 
            NobaseScope scp = NobaseAst.getScope(fc);
            if (scp != null) return scp;
          }
       }
      return null;
    }

   @Override NobaseValue evaluate(NobaseFile forfile,List<NobaseValue> args,NobaseValue thisval) {
      if (function_evaluator != null) {
         NobaseValue v = function_evaluator.evaluate(forfile,args,thisval);
         if (v != null) return v;
       }
   
      boolean chng = false;
      if (thisval != null && thisval instanceof ObjectValue) {
         chng |= mergeProperties(thisval);
       }
   
      if (arg_values != null) {
         int i = 0;
         for (NobaseValue arg : args) {
            if (arg_values.size() <= i) {
               arg_values.add(arg);
               chng = true;
             }
            else {
               NobaseValue ovalue = arg_values.get(i);
               NobaseValue nvalue = mergeValues(arg,ovalue);
               if (nvalue != null && !nvalue.equals(ovalue)) {
        	  arg_values.set(i,nvalue);
        	  chng = true;
        	}
             }
          }
       }
   
      if (!chng && return_value != null) return return_value;
      else return_value = null;
   
      return super.evaluate(forfile,args,thisval);
    }

   @Override void setReturnValue(NobaseValue v) {
      return_value = mergeValues(return_value,v);
    }
   
   @Override public String toString() {
      return "*FUNCTION*";
    }

}	// end of inner class FunctionValue



/********************************************************************************/
/*										*/
/*	Class value								*/
/*										*/
/********************************************************************************/

private static class ClassValue extends ObjectValue {
   
   private Set<ASTNode> class_defs;
   
   ClassValue() {
      super(NobaseType.createClass());
      class_defs = null;
    }
   
   ClassValue(ASTNode fc) { 
      this();
      if (fc != null) {
         addDefinition(fc);
       }
    }
   
   @Override boolean isClass()			{ return true; }
   
   @Override void addDefinition(ASTNode fc) {
      if (class_defs == null) class_defs = new HashSet<>();
      class_defs.add(fc);
    }
   
   @Override Collection<NobaseSymbol> getDefinitions() {
      List<NobaseSymbol> rslt = new ArrayList<>();
      if (class_defs != null) {
         for (ASTNode fc : class_defs) {
            NobaseSymbol ns = NobaseAst.getDefinition(fc);
            if (ns != null) rslt.add(ns);
          }
       }
      return rslt;
    }
   
   @Override NobaseScope getAssociatedScope() {
      if (class_defs != null) {
         for (ASTNode fc : class_defs) { 
            NobaseScope scp = NobaseAst.getScope(fc);
            if (scp != null) return scp;
          }
       }
      return null;
    }
   
   @Override public String toString() {
      return "*CLASS*";
    }
   
}	// end of inner class FunctionValue




}	// end of class NobaseValue




/* end of NobaseType.java */








































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































