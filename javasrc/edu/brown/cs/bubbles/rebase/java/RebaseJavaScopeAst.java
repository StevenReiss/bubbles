/********************************************************************************/
/*										*/
/*		RebaseJavaScopeAst.java 					*/
/*										*/
/*	Class to represent a scope for an AST node				*/
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



import java.util.Collection;


class RebaseJavaScopeAst extends RebaseJavaScope implements RebaseJavaConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseJavaScopeAst    parent_scope;
private RebaseJavaScopeLookup lookup_scope;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaScopeAst(RebaseJavaScope parent)
{
   parent_scope = (RebaseJavaScopeAst) parent;
   if (parent == null) lookup_scope = new RebaseJavaScopeLookup();
   else lookup_scope = parent_scope.getLookupScope();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override RebaseJavaScope getParent()		      { return parent_scope; }

private RebaseJavaScopeLookup getLookupScope()	      { return lookup_scope; }




/********************************************************************************/
/*										*/
/*	Definition methods							*/
/*										*/
/********************************************************************************/

@Override void defineVar(RebaseJavaSymbol js)		      { lookup_scope.defineVar(js,this); }
@Override RebaseJavaSymbol lookupVariable(String nm)	      { return lookup_scope.lookupVariable(nm,this); }

@Override Collection<RebaseJavaSymbol> getDefinedFields()
{
   return lookup_scope.getDefinedFields(this);
}

@Override void defineMethod(RebaseJavaSymbol js)		      { lookup_scope.defineMethod(js,this); }
@Override RebaseJavaSymbol lookupMethod(String id,RebaseJavaType aty) { return lookup_scope.lookupMethod(id,aty,this); }

@Override Collection<RebaseJavaSymbol> getDefinedMethods()
{
   return lookup_scope.getDefinedMethods(this);
}



}	// end of class RebaseJavaScopeAst



/* end of RebaseJavaScopeAst.java */
