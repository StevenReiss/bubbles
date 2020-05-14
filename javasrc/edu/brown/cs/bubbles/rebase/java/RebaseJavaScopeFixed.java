/********************************************************************************/
/*										*/
/*		RebaseJavaScopeFixed.java					*/
/*										*/
/*	Class to represent a fixed scope					*/
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




import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


class RebaseJavaScopeFixed extends RebaseJavaScope implements RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,RebaseJavaSymbol> var_names;
private Map<String,Collection<RebaseJavaSymbol>> method_names;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaScopeFixed()
{
   var_names = new HashMap<String,RebaseJavaSymbol>();
   method_names = new HashMap<String,Collection<RebaseJavaSymbol>>();
}




/********************************************************************************/
/*										*/
/*	Variable methods							*/
/*										*/
/********************************************************************************/

@Override synchronized void defineVar(RebaseJavaSymbol s)
{
   var_names.put(s.getName(),s);
}




@Override synchronized RebaseJavaSymbol lookupVariable(String nm)
{
   return var_names.get(nm);
}



@Override synchronized Collection<RebaseJavaSymbol> getDefinedFields()
{
   Collection<RebaseJavaSymbol> rslt = new ArrayList<RebaseJavaSymbol>();

   for (RebaseJavaSymbol js : var_names.values()) {
      if (js.isFieldSymbol()) rslt.add(js);
    }

   return rslt;
}





/********************************************************************************/
/*										*/
/*	Method definition methods						*/
/*										*/
/********************************************************************************/

@Override synchronized void defineMethod(RebaseJavaSymbol js)
{
   Collection<RebaseJavaSymbol> ms = method_names.get(js.getName());
   if (ms == null) {
      ms = new ArrayList<RebaseJavaSymbol>();
      method_names.put(js.getName(),ms);
    }
   ms.add(js);
}



@Override synchronized RebaseJavaSymbol lookupMethod(String id,RebaseJavaType aty)
{
   Collection<RebaseJavaSymbol> ljs = method_names.get(id);
   if (ljs != null) {
      for (RebaseJavaSymbol js : ljs) {
	 if (js == null) {
	    System.err.println("NULL SYMBOL IN METHOD LIST");
	    continue;
	  }
	 if (aty.isCompatibleWith(js.getType())) return js;
       }
    }

   return null;
}



@Override synchronized Collection<RebaseJavaSymbol> getDefinedMethods()
{
   Collection<RebaseJavaSymbol> rslt = new ArrayList<RebaseJavaSymbol>();
   for (Collection<RebaseJavaSymbol> csm : method_names.values()) {
      rslt.addAll(csm);
    }

   return rslt;
}




}	// end of class RebaseJavaScopeFixed



/* end of RebaseJavaScopeFixed.java */
