/********************************************************************************/
/*										*/
/*		RebaseJavaScope.java						*/
/*										*/
/*	Class to represent an abstract scope					*/
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



import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.Collection;


abstract class RebaseJavaScope implements RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

RebaseJavaScope getParent()			{ return null; }




/********************************************************************************/
/*										*/
/*	Variable methods							*/
/*										*/
/********************************************************************************/

abstract void defineVar(RebaseJavaSymbol s);

abstract RebaseJavaSymbol lookupVariable(String nm);



/********************************************************************************/
/*										*/
/*	Method definition methods						*/
/*										*/
/********************************************************************************/

RebaseJavaSymbol defineMethod(String nm,MethodDeclaration n)
{
   RebaseJavaSymbol js = RebaseJavaSymbol.createSymbol(n);

   defineMethod(js);

   return js;
}



abstract void defineMethod(RebaseJavaSymbol js);

abstract RebaseJavaSymbol lookupMethod(String id,RebaseJavaType aty);

Collection<RebaseJavaSymbol> getDefinedMethods()	      { return null; }
Collection<RebaseJavaSymbol> getDefinedFields() 	      { return null; }



}	// end of abstract class RebaseJavaScope



/* end of RebaseJavaScope.java */
