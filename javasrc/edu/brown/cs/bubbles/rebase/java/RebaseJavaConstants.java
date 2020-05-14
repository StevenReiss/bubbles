/********************************************************************************/
/*										*/
/*		RebaseJavaConstants.java					*/
/*										*/
/*	Constants for Java specific code for REBASE				*/
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

import edu.brown.cs.bubbles.rebase.RebaseConstants;
import edu.brown.cs.bubbles.rebase.RebaseFile;


interface RebaseJavaConstants extends RebaseConstants {



/********************************************************************************/
/*										*/
/*	Java attribute names							*/
/*										*/
/********************************************************************************/

String PROP_JAVA_TYPE = "REBUS$Type";
String PROP_JAVA_SCOPE = "REBUS$Scope";
String PROP_JAVA_REF = "REBUS$Ref";
String PROP_JAVA_ETYPE = "REBUS$ExprType";
String PROP_JAVA_DEF = "REBUS$Def";
String PROP_JAVA_RESOLVED = "REBUS$Resolved";
String PROP_JAVA_TYPER = "REBUS$Typer";
String PROP_JAVA_SOURCE = "REBUS$Source";
String PROP_JAVA_KEEP = "REBUS$Keep";
String PROP_JAVA_REQUEST = "REBUS$Request";




/********************************************************************************/
/*										*/
/*	Special Type names							*/
/*										*/
/********************************************************************************/

String TYPE_ANY_CLASS = "*ANY*";
String TYPE_ERROR = "*ERROR*";



/********************************************************************************/
/*                                                                              */
/*      Search Result holder                                                    */
/*                                                                              */
/********************************************************************************/

interface SearchResult {
   
   int getOffset();
   int getLength();
   RebaseFile getFile();
   RebaseJavaSymbol getSymbol();
   RebaseJavaSymbol getContainer();
   
}


}	// end of interface JavaConstants



/* end of JavaConstants.java */
