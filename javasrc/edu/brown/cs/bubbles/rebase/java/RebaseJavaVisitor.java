/********************************************************************************/
/*                                                                              */
/*              RebaseJavaVisitor.java                                          */
/*                                                                              */
/*      Visitor that includes the ability to visit RebaseJavaRoot               */
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



package edu.brown.cs.bubbles.rebase.java;


import org.eclipse.jdt.core.dom.ASTVisitor;


class RebaseJavaVisitor extends ASTVisitor implements RebaseJavaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RebaseJavaVisitor()
{
   super();
}


RebaseJavaVisitor(boolean docs)
{
   super(docs);
}



/********************************************************************************/
/*                                                                              */
/*      Methods for RebaseJavaRoot                                              */
/*                                                                              */
/********************************************************************************/

boolean visit(RebaseJavaRoot node)                      { return true; }

void endVisit(RebaseJavaRoot node)                      { }

void preVisit(RebaseJavaRoot node)                      { }

boolean preVisit2(RebaseJavaRoot node)                  { return true; }

void postVisit(RebaseJavaRoot node)                     { }



   
}       // end of class RebaseJavaVisitor




/* end of RebaseJavaVisitor.java */

