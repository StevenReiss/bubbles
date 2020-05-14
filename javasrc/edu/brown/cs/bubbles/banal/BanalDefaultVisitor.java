/********************************************************************************/
/*										*/
/*		BanalDefaultVisitor.java					*/
/*										*/
/*	Bubbles ANALysis package null visitor for abstraction			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;




abstract class BanalDefaultVisitor implements BanalConstants, BanalConstants.BanalVisitor
{


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BanalDefaultVisitor() 				{ }



/********************************************************************************/
/*										*/
/*	Null visitors								*/
/*										*/
/********************************************************************************/

@Override public void begin()					{ }

@Override public boolean checkUseProject(String p)		{ return true; }

@Override public boolean checkUseClass(String c)		{ return true; }

@Override public void visitClass(BanalClass bc,String signature,int access)	{ }
@Override public void visitSuper(BanalClass cls,BanalClass sup,boolean isiface) 	{ }
@Override public void visitClassAnnotation(BanalClass bc,BanalClass annot,boolean visible)	{ }
@Override public void visitInnerClass(BanalClass ocls,BanalClass icls,int access)	{ }

@Override public void visitClassField(BanalField bf,BanalClass typ,String gen,int acc,Object value)	{ }
@Override public void visitFieldAnnotation(BanalField bm,BanalClass annot,boolean visible)	{ }

@Override public void visitClassMethod(BanalMethod bm,String signature,int access,BanalClass [] excepts) { }
@Override public void visitMethodAnnotation(BanalMethod bm,BanalClass annot,boolean visible)	{ }
@Override public void visitRemoteFieldAccess(BanalMethod bm,BanalField bf)	{ }
@Override public void visitRemoteTypeAccess(BanalMethod bm,BanalClass bc)	{ }
@Override public void visitLocalVariable(BanalMethod bm,BanalClass type,String signature,boolean isparam) { }
@Override public void visitCall(BanalMethod bm,BanalMethod called)	{ }
@Override public void visitAlloc(BanalMethod bm,BanalClass allocd)	{ }
@Override public void visitCatch(BanalMethod bm,BanalClass caught)	{ }

@Override public void visitMethodEnd(BanalMethod bm)	{ }

@Override public void visitAnnotationValue(BanalClass bc,BanalClass annot,String id,Object val) { }
@Override public void visitAnnotationValue(BanalField bf,BanalClass annot,String id,Object val) { }
@Override public void visitAnnotationValue(BanalMethod bm,BanalClass annot,String id,Object val) { }

@Override public void visitClassEnd(BanalClass bc)	{ }

@Override public void finish()	{ }



}	// end of class BanalPackageGraph




/* end of BanalPackageGraph.java */
