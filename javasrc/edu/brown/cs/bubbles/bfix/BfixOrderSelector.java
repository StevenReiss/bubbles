/********************************************************************************/
/*                                                                              */
/*              BfixOrderSelector.java                                          */
/*                                                                              */
/*      Determine if an element is part of a set or not                         */
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bump.BumpConstants.BumpSymbolType;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;



class BfixOrderSelector implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private EnumSet<BumpSymbolType> symbol_types;
private int                     modifier_flags;
private int                     no_modifier_flags;
private int                     protect_flags;
private Pattern                 name_pattern;

private static final int PACKAGE = 0x800000;
private static Map<String,Integer> mod_names;

private static final String FACTORY_PATTERN = "(new|create)[A-Z][A-Za-z0-9]*";
private static final String MAIN_PATTERN = "main";
private static final String GETTER_PATTERN = "(get|is)[A-Z][A-Za-z0-9]*";
private static final String SETTER_PATTERN = "(set)[A-Z][A-Za-z0-9]*";
private static final String ACCESS_PATTERN = "(get|is|set)[A-Z][A-Za-z0-9]*";
private static final String OUTPUT_PATTERN = "toString";


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


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixOrderSelector(Element xml) throws IllegalArgumentException  
{ 
   symbol_types = null;
   modifier_flags = 0;
   no_modifier_flags = 0;
   protect_flags = 0;
   name_pattern = null;
   
   String s = IvyXml.getAttrString(xml,"TYPE");
   if (s != null) {
      symbol_types = EnumSet.noneOf(BumpSymbolType.class);
      StringTokenizer tok = new StringTokenizer(s,",;: \t");
      while (tok.hasMoreTokens()) {
         String t = tok.nextToken();
         BumpSymbolType bst = BumpSymbolType.valueOf(t);
         symbol_types.add(bst);
       }
    }
   
   String m = IvyXml.getAttrString(xml,"MODIFIER");
   if (m != null) {
      StringTokenizer tok = new StringTokenizer(m,",;: \t");
      while (tok.hasMoreTokens()) {
         String t = tok.nextToken();
         if (t.startsWith("NO_")) {
            t = t.substring(3);
            Integer fg = mod_names.get(t);
            if (fg == null) throw new IllegalArgumentException(t);
            no_modifier_flags |= fg;
          }
         else {
            Integer fg = mod_names.get(t);
            if (fg == null) throw new IllegalArgumentException(t);
            modifier_flags |= fg;
          }
       }
    }
   setModifier(xml,"STATIC",Modifier.STATIC);
   setModifier(xml,"ABSTRACT",Modifier.ABSTRACT);
   setModifier(xml,"FINAL",Modifier.FINAL);
   String p = IvyXml.getAttrString(xml,"PROTECT");
   if (p != null) {
      StringTokenizer tok = new StringTokenizer(p,",;: \t");
      while (tok.hasMoreTokens()) {
         String t = tok.nextToken();
         Integer fg = mod_names.get(t);
         if (fg == null) throw new IllegalArgumentException(t);
         protect_flags |= fg;
       }
    }
   
   String pat = IvyXml.getAttrString(xml,"PATTERN");
   if (pat == null) {
      if (IvyXml.getAttrBool(xml,"FACTORY")) pat = addPattern(pat,FACTORY_PATTERN);
      if (IvyXml.getAttrBool(xml,"GETTER")) pat = addPattern(pat,GETTER_PATTERN);
      if (IvyXml.getAttrBool(xml,"SETTER")) pat = addPattern(pat,SETTER_PATTERN);    
      if (IvyXml.getAttrBool(xml,"ACCESS")) pat = addPattern(pat,ACCESS_PATTERN);    
      if (IvyXml.getAttrBool(xml,"OUTPUT")) pat = addPattern(pat,OUTPUT_PATTERN);
      if (IvyXml.getAttrBool(xml,"MAIN")) pat = addPattern(pat,MAIN_PATTERN);
    }
   if (pat != null) {
      try {
         name_pattern = Pattern.compile(pat);
       }
      catch (PatternSyntaxException e) {
         throw new IllegalArgumentException(e);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Checking methods                                                        */
/*                                                                              */
/********************************************************************************/

boolean contains(BfixOrderElement be1)
{
   if (symbol_types != null) {
      BumpSymbolType bst = be1.getSymbolType();
      if (bst == null) return false;
      if (!symbol_types.contains(bst)) return false;
    }
   
   int mods = be1.getModifiers();
   if ((mods & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0) 
      mods |= PACKAGE;  
   if (modifier_flags != 0) {
      if ((mods & modifier_flags) == 0) return false;
    }
   if (no_modifier_flags != 0) {
      if ((mods & no_modifier_flags) != 0) return false;
    }
   if (protect_flags != 0) {
      if ((mods & protect_flags) == 0) return false;
    }
   
   if (name_pattern != null) {
      Matcher m = name_pattern.matcher(be1.getName());
      if (!m.matches()) return false;
    }
   
   return true;
}


double complexity()
{
   double rslt = 0;
   
   if (symbol_types != null) {
      int sz = symbol_types.size();
      int ct = BumpSymbolType.values().length;
      rslt += 1 + (ct - sz)/ct; 
    }
   
   if (modifier_flags != 0) {
      rslt += 1;
    }
   if (protect_flags != 0) {
      rslt += 1;
    }
   if (name_pattern != null) {
      rslt += 1;
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void setModifier(Element xml,String what,int mod)
{
   if (IvyXml.getAttrPresent(xml,what)) {
      boolean fg = IvyXml.getAttrBool(xml,what);
      if (fg) modifier_flags |= mod;
      else no_modifier_flags |= mod;
    }
}


private String addPattern(String pat,String add)
{
   if (pat == null) return add;
   pat = "((" + pat + ")|(" + add + "))";
   return pat;
}

}       // end of class BfixOrderSelector




/* end of BfixOrderSelector.java */

