/********************************************************************************/
/*										*/
/*		RebaseJavaScopeLookup.java					*/
/*										*/
/*	Class to handle scope-based lookup for a compilation unit		*/
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
import java.util.List;
import java.util.Map;


class RebaseJavaScopeLookup implements RebaseJavaConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,List<VarElement>> var_names;
private Map<String,List<MethodElement>> method_names;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaScopeLookup()
{
   var_names = new HashMap<String,List<VarElement>>();
   method_names = new HashMap<String,List<MethodElement>>();
}



/********************************************************************************/
/*										*/
/*	Variable methods							*/
/*										*/
/********************************************************************************/

void defineVar(RebaseJavaSymbol s,RebaseJavaScope js)
{
   List<VarElement> lve = var_names.get(s.getName());
   if (lve == null) {
      lve = new ArrayList<VarElement>();
      var_names.put(s.getName(),lve);
    }
   lve.add(new VarElement(s,js));
}




RebaseJavaSymbol lookupVariable(String nm,RebaseJavaScope js)
{
   List<VarElement> lve = var_names.get(nm);
   if (lve == null) return null;
   while (js != null) {
      for (VarElement ve : lve) {
	 if (ve.getScope() == js) return ve.getSymbol();
       }
      js = js.getParent();
    }

   return null;
}



Collection<RebaseJavaSymbol> getDefinedFields(RebaseJavaScope js)
{
   Collection<RebaseJavaSymbol> rslt = new ArrayList<RebaseJavaSymbol>();

   for (List<VarElement> lve : var_names.values()) {
      for (VarElement ve : lve) {
	 if (ve.getScope() == js && ve.getSymbol().isFieldSymbol()) rslt.add(ve.getSymbol());
       }
    }

   return rslt;
}




private static class VarElement {

   private RebaseJavaSymbol for_symbol;
   private RebaseJavaScope for_scope;

   VarElement(RebaseJavaSymbol s,RebaseJavaScope scp) {
      for_symbol = s;
      for_scope = scp;
    }

   RebaseJavaScope getScope()		      { return for_scope; }
   RebaseJavaSymbol getSymbol() 	      { return for_symbol; }

}	// end of subclass VarElement





/********************************************************************************/
/*										*/
/*	Method symbol methods							*/
/*										*/
/********************************************************************************/

void defineMethod(RebaseJavaSymbol js,RebaseJavaScope scp)
{
   List<MethodElement> lme = method_names.get(js.getName());
   if (lme == null) {
      lme = new ArrayList<MethodElement>();
      method_names.put(js.getName(),lme);
    }
   MethodElement useme = null;
   for (MethodElement me : lme) {
      if (me.getScope() == scp) {
	 useme = me;
	 break;
       }
    }
   if (useme == null) {
      useme = new MethodElement(scp);
      lme.add(useme);
    }
   useme.add(js);
}




RebaseJavaSymbol lookupMethod(String id,RebaseJavaType aty,RebaseJavaScope js)
{
   List<MethodElement> lme = method_names.get(id);
   if (lme == null) {
      return null;
    }
   while (js != null) {
      for (MethodElement me : lme) {
	 if (me.getScope() == js) {
	    for (RebaseJavaSymbol ms : me.getMethods()) {
	       if (aty.isCompatibleWith(ms.getType())) return ms;
	     }
	  }
       }
      js = js.getParent();
    }

   return null;
}




Collection<RebaseJavaSymbol> getDefinedMethods(RebaseJavaScope js)
{
   Collection<RebaseJavaSymbol> rslt = new ArrayList<RebaseJavaSymbol>();

   for (List<MethodElement> lme : method_names.values()) {
      for (MethodElement me : lme) {
	 if (me.getScope() == js) rslt.addAll(me.getMethods());
       }
    }

   return rslt;
}




private static class MethodElement {

   private RebaseJavaScope for_scope;
   private Collection<RebaseJavaSymbol> for_methods;

   MethodElement(RebaseJavaScope scp) {
      for_scope = scp;
      for_methods = new ArrayList<RebaseJavaSymbol>();
    }

   void add(RebaseJavaSymbol js)		      { for_methods.add(js); }

   RebaseJavaScope getScope()			      { return for_scope; }
   Collection<RebaseJavaSymbol> getMethods()	      { return for_methods; }

}	// end of subclass MethodElement





}	// end of class RebaseJavaScopeLookup




/* end of RebaseJavaScopeLookup.java */
