/********************************************************************************/
/*										*/
/*		BanalConstants.java						*/
/*										*/
/*	Bubbles ANALysis package constants					*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


public interface BanalConstants {


/********************************************************************************/
/*										*/
/*	Definitions for package graph data					*/
/*										*/
/********************************************************************************/

enum PackageRelationType {
   NONE,			// no relation
   SUPERCLASS,			// subclass - superclass relationship
   IMPLEMENTS,			// implements an interface
   EXTENDS,			// extends an interface
   INNERCLASS,			// inner class (nested)
   ALLOCATES,			// allocates an instance of
   CALLS,			// calls a method in
   CATCHES,			// catches an exeception of
   ACCESSES,			// accesses a field of
   WRITES,			// writes a field of
   CONSTANT,			// access a constant from (enum or final static)
   FIELD,			// has a field of
   LOCAL,			// has a local of
   PACKAGE,			// in package
   CLASSMETHOD, 		// method of a class
   OVERRIDES,			// method overriding method of superclass
}




enum ClassType {
   PUBLIC,
   PRIVATE,
   PROTECTED,
   PACKAGE_PROTECTED,
   INNER,
   ENUM,
   INTERFACE,
   CLASS,
   ANNOTATION,
   STATIC,
   ABSTRACT,
   FINAL,
   THROWABLE,
   METHOD
}




interface BanalPackageNode {

   String getName();
   int getModifiers();
   Collection<BanalPackageLink> getInLinks();
   Collection<BanalPackageLink> getOutLinks();
   Set<ClassType> getTypes();

   String getMethodName();
   String getClassName();
   String getPackageName();
   String getProjectName();
   
   void outputXml(IvyXmlWriter xw);
}	// end of inner class BanalPackageNode



interface BanalPackageClass extends BanalPackageNode {


}	// end of inner class BanalPackageClass


interface BanalPackageMethod  extends BanalPackageNode {

}	// end of inner class BanalPackageMethod


interface BanalPackageLink {

   BanalPackageNode getFromNode();
   BanalPackageNode getToNode();
   Map<PackageRelationType,Integer> getTypes();

}



/********************************************************************************/
/*										*/
/*	Hierarchy interfaces							*/
/*										*/
/********************************************************************************/

interface BanalHierarchyNode {

   String getName();
   int getLevel();
   int getCycle();
   
}	// end of inner class BanalHierarchyNode



/********************************************************************************/
/*										*/
/*	Database constants							*/
/*										*/
/********************************************************************************/

String FILE_DIRECTORY = System.getProperty("java.io.tmpdir");

String []  TABLE_NAMES = new String [] {
   "SrcClass",
   "SrcInterface",
   "SrcField",
   "SrcMethod",
   "SrcMethodParam",
   "SrcCall",
   "SrcLines",
   "SrcAlloc"
};




/********************************************************************************/
/*										*/
/*	Class Repository							*/
/*										*/
/********************************************************************************/

interface BanalClassData {

   String getName();
   String getSourceFile();
   String getProject();
   InputStream getClassStream();

}	// end of inner interface BanalClassData




/********************************************************************************/
/*										*/
/*	Visitor for Static Analysis						*/
/*										*/
/********************************************************************************/


interface BanalClass {

   String getInternalName();		// Lxxx/xxx/xxx$xxx;
   String getJavaName();		// xxx.xxx.xxx$xxx

}	// end of inner interface BanalClass



interface BanalMethod {

   BanalClass getOwnerClass();
   String getName();			// simple name
   BanalClass [] getArgumentTypes();	// set of argument types
   BanalClass getReturnType();
   String getFullName();

}      // end of inner interface BanalMethod


interface BanalField {

   BanalClass getOwnerClass();
   String getName();

}	// end of inner interface BanalField



interface BanalVisitor {

   void begin();

   boolean checkUseProject(String proj);
   boolean checkUseClass(String cls);

   void visitClass(BanalClass bc,String signature,int access);
   void visitSuper(BanalClass cls,BanalClass sup,boolean isiface);
   void visitClassAnnotation(BanalClass bc,BanalClass annot,boolean visible);
   void visitInnerClass(BanalClass ocls,BanalClass icls,int access);

   void visitClassField(BanalField bf,BanalClass typ,String gen,int acc,Object value);
   void visitFieldAnnotation(BanalField bm,BanalClass annot,boolean visible);

   void visitClassMethod(BanalMethod bm,String signature,int access,BanalClass [] excepts);
   void visitMethodAnnotation(BanalMethod bm,BanalClass annot,boolean visible);
   void visitRemoteFieldAccess(BanalMethod bm,BanalField bf);
   void visitRemoteTypeAccess(BanalMethod bm,BanalClass bc);
   void visitLocalVariable(BanalMethod bm,BanalClass type,String signature,boolean isparam);
   void visitCall(BanalMethod bm,BanalMethod called);
   void visitAlloc(BanalMethod bm,BanalClass allocd);
   void visitCatch(BanalMethod bm,BanalClass caught);

   void visitMethodEnd(BanalMethod bm);

   void visitAnnotationValue(BanalClass bc,BanalClass annot,String id,Object val);
   void visitAnnotationValue(BanalField bf,BanalClass annot,String id,Object val);
   void visitAnnotationValue(BanalMethod bm,BanalClass annot,String id,Object val);

   void visitClassEnd(BanalClass bc);

   void finish();
}



}	// end of interface BanalConstants



/* end of BanalConstants.java */


