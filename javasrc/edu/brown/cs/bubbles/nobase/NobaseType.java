/********************************************************************************/
/*										*/
/*		NobaseType.java 						*/
/*										*/
/*	description of class							*/
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class NobaseType implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected String	type_name;

private static NobaseType undef_type = new UndefinedType();
private static NobaseType unknown_type = new UnknownType();
private static NobaseType null_type = new NullType();
private static NobaseType bool_type = new BooleanType();
private static NobaseType number_type = new NumberType();
private static NobaseType string_type = new StringType();
private static NobaseType any_type = new AnyType();




/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static NobaseType createUndefined()		{ return undef_type; }

static NobaseType createUnknown()		{ return unknown_type; }

static NobaseType createNull()			{ return null_type; }

static NobaseType createBoolean()		{ return bool_type; }

static NobaseType createNumber()		{ return number_type; }

static NobaseType createString()		{ return string_type; }

static NobaseType createObject()		{ return new ObjectType(); }

static NobaseType createClass() 		{ return new ClassType(); }

static NobaseType createList()			{ return new ListType(); }

static NobaseType createFunction()		{ return new CompletionType(); }

static NobaseType createAnyType()		{ return any_type; }

static NobaseType createUnion(NobaseType... typs)
{
   return new UnionType(typs);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private NobaseType(String nm)
{
   type_name = nm;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()				{ return type_name; }

boolean isAnyType()                             { return false; }




/********************************************************************************/
/*                                                                              */
/*      Type Operations                                                         */
/*                                                                              */
/********************************************************************************/

NobaseType mergeWith(NobaseType t)
{
   if (t == null || t == this) return this;
   if (isAnyType()) return this;
   if (t.isAnyType()) return t;
   
   return createAnyType();
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return "Type:" + type_name;
}


/********************************************************************************/
/*										*/
/*	Undefined type								*/
/*										*/
/********************************************************************************/

private static class UndefinedType extends NobaseType {

   UndefinedType() {
      super("undefined");
    }

}	// end of inner class UndefinedType



/********************************************************************************/
/*										*/
/*	Null type								*/
/*										*/
/********************************************************************************/

private static class NullType extends NobaseType {

   NullType() {
      super("null");
    }

}	// end of inner class NullType



/********************************************************************************/
/*                                                                              */
/*      Unknown type                                                            */
/*                                                                              */
/********************************************************************************/

private static class UnknownType extends NobaseType {
   
   UnknownType() {
      super("*UNKNOWN*"); 
    }
   
}       // end of inner class UnknownType



/********************************************************************************/
/*										*/
/*	Boolean type								*/
/*										*/
/********************************************************************************/

private static class BooleanType extends NobaseType {

   BooleanType() {
      super("boolean");
    }

}	// end of inner class BooleanType



/********************************************************************************/
/*										*/
/*	String Type								*/
/*										*/
/********************************************************************************/

private static class StringType extends NobaseType {

   StringType() {
      super("string");
    }

}	// end of inner class StringType


/********************************************************************************/
/*										*/
/*	Numeric Types								*/
/*										*/
/********************************************************************************/

private static class NumberType extends NobaseType {

   NumberType() {
      super("number");
    }

}	// end of inner class NumberType



/********************************************************************************/
/*										*/
/*	Object Types								*/
/*										*/
/********************************************************************************/

private static class ObjectType extends NobaseType {

   ObjectType() {
      super("object");
    }

}	// end of inner class ObjectType



/********************************************************************************/
/*										*/
/*	Class Types								*/
/*										*/
/********************************************************************************/

private static class ClassType extends NobaseType {

   ClassType() {
      super("class");
    }
   
   @Override public String toString() {         
      return "Type:class";

    }
}



/********************************************************************************/
/*										*/
/*	List Type								*/
/*										*/
/********************************************************************************/

private static class ListType extends NobaseType {

   ListType() {
      super("list");
    }

}	// end of inner class ListType




/********************************************************************************/
/*										*/
/*	Completion type 							*/
/*										*/
/********************************************************************************/

private static class CompletionType extends NobaseType {

   CompletionType() {
      super("function");
    }

}	// end of inner class CompletionType





/********************************************************************************/
/*										*/
/*	Any Type								*/
/*										*/
/********************************************************************************/

private static class AnyType extends NobaseType {

   AnyType() {
      super("*ANY*");
    }

   boolean isAnyType()                  { return true; }
   
}	// end of inner class AnyType



/********************************************************************************/
/*                                                                              */
/*      Union type                                                              */
/*                                                                              */
/********************************************************************************/

private static class UnionType extends NobaseType {
   
   private List<NobaseType> base_types;
   
   UnionType(NobaseType... typs) {
      super("*UNION*");
      setupBaseTypes(typs);
    }
   
   private void setupBaseTypes(NobaseType [] typs) {
      Set<NobaseType> rslt = new HashSet<>();
      boolean haveunknown = false;
      for (NobaseType t0 : typs) {
         if (t0 instanceof UnionType) {
            UnionType ut0 = (UnionType) t0;
            for (NobaseType t1 : ut0.base_types) {
               rslt.add(t1);
             }
          }
         else if (t0 instanceof UnknownType) {
            haveunknown = true;
          }
         else rslt.add(t0);
       }
      if (rslt.isEmpty() && haveunknown) {
         rslt.add(createUnknown());
       }
      base_types = new ArrayList<>(rslt);
      Collections.sort(base_types,new TypeComparator());
      StringBuffer buf = new StringBuffer();
      for (NobaseType t0 : base_types) {
         buf.append("+");
         buf.append(t0.getName());
       }
      type_name = buf.toString();
    }
   
}       // end of inner class UnionType

  
private static final class TypeComparator implements Comparator<NobaseType> {
   
   @Override public int compare(NobaseType t0,NobaseType t1) {
      return t0.getName().compareTo(t1.getName());
    }
   
}       // end of inner class TypeComparator

}	// end of class NobaseType




/* end of NobaseType.java */

