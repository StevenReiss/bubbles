/********************************************************************************/
/*										*/
/*		BfixOrderOrdering.java						*/
/*										*/
/*	Representation of an ordering between elements				*/
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


abstract class BfixOrderOrdering implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static BfixOrderOrdering createOrdering(Element xml)
	throws IllegalArgumentException
{
   if (!IvyXml.isElement(xml,"ORDER"))
      throw new IllegalArgumentException(xml.getNodeName());

   BfixOrderOrdering ord = null;
   MultipleOrder mord = null;
   for (Element e : IvyXml.children(xml)) {
      BfixOrderOrdering nord = null;
      switch (e.getNodeName()) {
	 case "MODIFIER" :
	    nord = new ModifierOrder(IvyXml.getAttrString(e,"ORDER"));
	    break;
	 case "PROTECT" :
	    nord = new ProtectionOrder(IvyXml.getAttrString(e,"ORDER"));
	    break;
	 case "DEFINITION" :
	    nord = new DefinitionOrder();
	    break;
	 case "NAME" :
	    nord = new NameOrder(IvyXml.getAttrBool(e,"CASE"),
		  IvyXml.getAttrString(e,"SELECT"),
		  IvyXml.getAttrInt(e,"SELECTOR",1));
	    break;
	 case "TYPE" :
	    nord = new ElementOrder(IvyXml.getAttrString(e,"ORDER"));
	    break;
	 case "PARAMETER" :
	    // TODO: handle ordering based on parameters
	    break;
	 default :
	    throw new IllegalArgumentException(e.getNodeName());
       }
      if (nord != null) {
	 if (ord == null) ord = nord;
	 else {
	    if (mord == null) {
	       mord = new MultipleOrder(ord);
	       ord = mord;
	     }
	    mord.addOrder(nord);
	  }
       }
    }

   if (ord == null) ord = new DefinitionOrder();

   return ord;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BfixOrderOrdering()
{ }


/********************************************************************************/
/*										*/
/*	Ordering methods							*/
/*										*/
/********************************************************************************/

abstract int compareTo(BfixOrderElement be1,BfixOrderElement be2);



/********************************************************************************/
/*										*/
/*	Definition order							*/
/*										*/
/********************************************************************************/

private static class DefinitionOrder extends BfixOrderOrdering {

   DefinitionOrder() { }

   @Override int compareTo(BfixOrderElement be1,BfixOrderElement be2) {
      int off1 = be1.getStartOffset();
      int off2 = be2.getStartOffset();
      if (off1 == off2) return 0;
      if (off1 < 0) return 1;
      if (off2 < 0) return -1;
      return (off1 < off2) ? -1 : 0;
    }

}	// end of inner class DefinitionOrder



/********************************************************************************/
/*										*/
/*	Modifier Order								*/
/*										*/
/********************************************************************************/

private static class ModifierOrder extends BfixOrderOrdering {

   private List<Integer> mod_bits;

   private static final int NEGATE = 0x1000000;
   private static final int PACKAGE = 0x800000;
   private static Map<String,Integer> mod_names;

   static {
      mod_names = new HashMap<String,Integer>();
      mod_names.put("ABSTRACT",Modifier.ABSTRACT);
      mod_names.put("FINAL",Modifier.FINAL);
      mod_names.put("INTERFACE",Modifier.INTERFACE);
      mod_names.put("NATIVE",Modifier.NATIVE);
      mod_names.put("PACKAGE",PACKAGE);
      mod_names.put("PRIVATE",Modifier.PRIVATE);
      mod_names.put("PROTECTED",Modifier.PROTECTED);
      mod_names.put("PUBLIC",Modifier.PUBLIC);
      mod_names.put("STATIC",Modifier.STATIC);
      mod_names.put("STRICT",Modifier.STRICT);
      mod_names.put("SYNCHRONIZED",Modifier.SYNCHRONIZED);
      mod_names.put("TRANSIENT",Modifier.TRANSIENT);
      mod_names.put("VOLATILE",Modifier.VOLATILE);
    }


   ModifierOrder(String s) throws IllegalArgumentException {
      StringTokenizer tok = new StringTokenizer(s,",;: \t");
      mod_bits = new ArrayList<Integer>();
      while (tok.hasMoreTokens()) {
	 String t = tok.nextToken();
	 int bit = 0;
	 if (t.startsWith("NO_")) {
	    bit |= NEGATE;
	    t = t.substring(3);
	  }
	 Integer val = mod_names.get(t);
	 if (val == null) throw new IllegalArgumentException(t);
	 bit |= val;
	 mod_bits.add(bit);
       }
    }

   @Override int compareTo(BfixOrderElement be1,BfixOrderElement be2) {
      int bit1 = modifiers(be1);
      int bit2 = modifiers(be2);
      for (Integer iv : mod_bits) {
	 int chk = iv;
	 int mul = 1;
	 if ((chk & NEGATE) != 0) {
	    chk |= ~NEGATE;
	    mul = -1;
	  }
	 int v1 = (bit1 & chk) != 0 ? 1 : -1;
	 int v2 = (bit2 & chk) != 0 ? 1 : -1;
	 if (v1 == v2) continue;
	 if (v1 > v2) return mul;
	 return -mul;
       }
      return 0;
    }

   private int modifiers(BfixOrderElement be) {
      int mods = be.getModifiers();
      if ((mods & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0)
	 mods |= PACKAGE;
       return mods;
    }
}



/********************************************************************************/
/*										*/
/*	Protection-based ordering						*/
/*										*/
/********************************************************************************/

private static class ProtectionOrder extends ModifierOrder {

   ProtectionOrder(String s) {
      super(s);
    }

}	// end of inner class ProtectionOrder




/********************************************************************************/
/*										*/
/*	Name-based ordering							*/
/*										*/
/********************************************************************************/

private static class NameOrder extends BfixOrderOrdering {

   private boolean ignore_case;
   private Pattern use_regex;
   private int regex_element;

   NameOrder(boolean usecase,String pat,int patidx) {
      ignore_case = !usecase;
      if (pat == null) use_regex = null;
      else {
	 use_regex = Pattern.compile(pat);
	 regex_element = patidx;
       }
    }

   @Override int compareTo(BfixOrderElement be1,BfixOrderElement be2) {
      String nm1 = be1.getName();
      String nm2 = be2.getName();
      if (use_regex != null) {
	 Matcher m1 = use_regex.matcher(nm1);
	 if (m1.find()) {
	    nm1 = m1.group(regex_element);
	  }
	 else nm1 = null;
	 Matcher m2 = use_regex.matcher(nm2);
	 if (m2.find()) {
	    nm2 = m2.group(regex_element);
	  }
	 else nm2 = null;
       }
      if (nm1 == null && nm2 == null) return 0;
      if (nm1 == null) return 1;
      if (nm2 == null) return -1;
      if (ignore_case) {
	 nm1 = nm1.toLowerCase();
	 nm2 = nm2.toLowerCase();
       }
      return nm1.compareTo(nm2);
    }

}	// end of inner class NameOrdering




/********************************************************************************/
/*										*/
/*	Element Type Ordering							*/
/*										*/
/********************************************************************************/

private static class ElementOrder extends BfixOrderOrdering {

   private List<String> symbol_types;

   ElementOrder(String typs) throws IllegalArgumentException {
      symbol_types = new ArrayList<String>();
      StringTokenizer tok = new StringTokenizer(typs,",;: \t");
      while (tok.hasMoreTokens()) {
	 String t = tok.nextToken();
	 symbol_types.add(t);
       }
    }

   @Override int compareTo(BfixOrderElement be1,BfixOrderElement be2) {
      String bst1 = be1.getElementType();
      String bst2 = be2.getElementType();
      if (bst1.equals(bst2)) return 0;
      for (String chk : symbol_types) {
         if (bst1.equals(chk)) return -1;
         if (bst2.equals(chk)) return 1;
       }
      return 0;
    }

}	// end of inner class ElementOrder



/********************************************************************************/
/*										*/
/*	MultipleOrder -- order with multiple orderings				*/
/*										*/
/********************************************************************************/

private static class MultipleOrder extends BfixOrderOrdering {

   private List<BfixOrderOrdering> use_orders;

   MultipleOrder(BfixOrderOrdering ord) {
      use_orders = new ArrayList<BfixOrderOrdering>();
      if (ord != null) addOrder(ord);
    }

   void addOrder(BfixOrderOrdering ord) {
      use_orders.add(ord);
    }

   @Override int compareTo(BfixOrderElement be1,BfixOrderElement be2) {
      for (BfixOrderOrdering ord : use_orders) {
	 int val = ord.compareTo(be1,be2);
	 if (val != 0) return val;
       }
      return 0;
    }

}	// end of inner class MultipleOrder




}	// end of class BfixOrderOrdering




/* end of BfixOrderOrdering.java */

