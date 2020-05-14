/********************************************************************************/
/*										*/
/*		RebaseJavaSearch.java						*/
/*										*/
/*	Various search functions for Java programs				*/
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



package edu.brown.cs.bubbles.rebase.java;

import edu.brown.cs.bubbles.rebase.RebaseConstants;
import edu.brown.cs.bubbles.rebase.RebaseFile;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;



class RebaseJavaSearch implements RebaseConstants.RebaseSearcher, RebaseJavaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseJavaRoot		rebase_root;
private Set<RebaseSymbol>	match_symbols;
private List<SearchResult>	result_map;
private RebaseJavaFile		current_file;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaSearch(RebaseJavaRoot root)
{
   rebase_root = root;
   match_symbols = new HashSet<RebaseSymbol>();
   result_map = new ArrayList<SearchResult>();
   current_file = null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setFile(RebaseJavaFile jf) 		{ current_file = jf; }

List<SearchResult> getMatches() 		{ return result_map; }
Set<RebaseSymbol> getSymbols()			{ return match_symbols; }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputSearchFor(IvyXmlWriter xw)
{
   String what = null;
   String nm = null;
   for (RebaseSymbol rs : match_symbols) {
      RebaseJavaSymbol rjs = (RebaseJavaSymbol) rs;
      String rwhat = null;
      switch (rjs.getSymbolKind()) {
         case ANNOTATION :
         case NONE :
         case PACKAGE :
         default :
            break;
         case CLASS :
         case ENUM :
         case INTERFACE :
            rwhat = "Class";
            break;
         case CONSTRUCTOR :
         case METHOD :
            rwhat = "Function";
            break;
         case FIELD :
            rwhat = "Field";
            break;
         case LOCAL :
            rwhat = "Local";
            break;
       }
      if (rwhat == null) continue;
      if (nm == null) nm = rjs.getName();
      if (what == null) what = rwhat;
      else if (what.equals(rwhat)) continue;
      else return;
    }
   
   if (what != null) {
      xw.begin("SEARCHFOR");
      xw.field("TYPE",what);
      xw.text(nm);
      xw.end("SEARCHFOR");
    }
}




/********************************************************************************/
/*										*/
/*	Search for symbols							*/
/*										*/
/********************************************************************************/

ASTVisitor getFindSymbolsVisitor(String pat,String kind)
{
   if (pat == null) pat = "*";

   String p1 = pat;
   String p2 = null;
   String p3 = null;
   String p4 = null;
   EnumSet<RebaseSymbolKind> kindset = EnumSet.noneOf(RebaseSymbolKind.class);

   switch (kind) {
      case "TYPE" :
      case "CLASS" :
      case "ENUM" :
      case "INTERFACE" :
      case "CLASS&ENUM" :
      case "CLASS&INTERFACE" :
	 int idx1 = pat.indexOf("<");
	 int idx2 = findEndOfTypes(pat,idx1);
	 if (idx1 > 0 && idx2 > 0) {
	    p1 = pat.substring(0,idx1);
	    p2 = pat.substring(idx1,idx2+1);
	  }
	 break;
      case "METHOD" :
      case "CONSTRUCTOR" :
	int idx3 = pat.indexOf("<");
	int idx4 = pat.indexOf("(");
	if (idx3 >= 0 && (idx4 < 0 || idx3 < idx4) &&
	      (idx3 == 0 || pat.charAt(idx3-1) == '.')) {
	   int idx6 = findEndOfTypes(pat,idx3);
	   if (idx6 > 0) {
	      p2 = pat.substring(idx3,idx6+1);
	      p1 = p1.substring(idx6+1);
	      idx4 = p1.indexOf("(");
	    }
	 }
	if (idx4 >= 0) {
	   int idx5 = p1.indexOf(")");
	   if (idx5 > 0 && idx5 > idx4) {
	      p3 = p1.substring(idx4,idx5+1);
	      String rest = null;
	      if (idx5+1 < p1.length()) rest = p1.substring(idx5+1).trim();
	      if (rest != null && rest.length() == 0) rest = null;
	      p1 = p1.substring(0,idx4);
	      if (rest != null) p1 += " " + rest;
	    }
	 }
	int idx7 = p1.indexOf(" ");
	if (idx7 > 0) {
	   p4 = p1.substring(idx7+1).trim();
	   p1 = p1.substring(0,idx7);
	 }
	break;
      case "FIELD" :
	 int idx8 = p1.indexOf(" ");
	 if (idx8 > 0) {
	    p4 = p1.substring(idx8+1).trim();
	    p1 = p1.substring(0,idx8);
	  }
	 break;
      case "PACKAGE" :
	 break;
    }

   switch (kind) {
      case "TYPE" :
	 kindset.add(RebaseSymbolKind.CLASS);
	 kindset.add(RebaseSymbolKind.INTERFACE);
	 kindset.add(RebaseSymbolKind.ENUM);
	 break;
      case "CLASS" :
	 kindset.add(RebaseSymbolKind.CLASS);
	 break;
      case "ENUM" :
	 kindset.add(RebaseSymbolKind.ENUM);
	 break;
      case "INTERFACE" :
	 kindset.add(RebaseSymbolKind.INTERFACE);
	 break;
      case "CLASS&ENUM" :
	 kindset.add(RebaseSymbolKind.CLASS);
	 kindset.add(RebaseSymbolKind.ENUM);
	 break;
      case "CLASS&INTERFACE" :
	 kindset.add(RebaseSymbolKind.CLASS);
	 kindset.add(RebaseSymbolKind.INTERFACE);
	 break;
      case "METHOD" :
	 kindset.add(RebaseSymbolKind.METHOD);
	 break;
      case "CONSTRUCTOR" :
	 kindset.add(RebaseSymbolKind.CONSTRUCTOR);
	 break;
      case "FIELD" :
	 kindset.add(RebaseSymbolKind.FIELD);
	 break;
      case "PACKAGE" :
	 kindset.add(RebaseSymbolKind.PACKAGE);
	 break;
    }

   return new FindSymbolVisitor(p1,p2,p3,p4,kindset);
}



private int findEndOfTypes(String pat,int idx1)
{
   if (idx1 < 0) return -1;

   int lvl = 0;
   for (int i = idx1; i < pat.length(); ++i) {
      char c = pat.charAt(i);
      if (c == '<') ++ lvl;
      else if (c == '>') {
	 if (--lvl == 0) return i;
       }
    }
   return -1;
}


private class FindSymbolVisitor extends ASTVisitor {

   private Pattern name_pattern;
   private String generics_pattern;
   private List<String> parameters_pattern;
   private boolean extra_parameters;
   private Pattern type_pattern;
   private EnumSet<RebaseSymbolKind> search_kind;

   FindSymbolVisitor(String np,String gp,String ap,String tp,EnumSet<RebaseSymbolKind> kinds) {
      fixNamePattern(np);
      fixGenericsPattern(gp);
      fixParametersPattern(ap);
      fixTypePattern(tp);
      search_kind = kinds;
    }


   @Override public void postVisit(ASTNode n) {
      RebaseJavaSymbol js = RebaseJavaAst.getDefinition(n);
      if (js == null) return;
      if (match(js)) {
	 match_symbols.add(js);
       }
    }

   private boolean match(RebaseJavaSymbol js) {
      if (!search_kind.contains(js.getSymbolKind())) return false;
      if (js.isTypeSymbol()) {
	 RebaseJavaType jt = js.getType();
	 String n1 = jt.getName();
	 int idx1 = n1.indexOf("<");
	 if (idx1 > 0) {
	    String n2 = n1.substring(idx1);
	    n1 = n1.substring(0,idx1);
	    if (!matchGenerics(n2)) return false;
	  }
	 if (!matchName(jt.getName()))return false;
       }
      else if (js.isConstructorSymbol() || js.isMethodSymbol()) {
	 String n1 = js.getFullName();
	 if (js.isConstructorSymbol()) {
	    n1 = js.getClassType().getName();
	  }
	 int idx1 = n1.indexOf("<");
	 if (idx1 > 0) {
	    String n2 = n1.substring(idx1);
	    n1 = n1.substring(0,idx1);
	    if (!matchGenerics(n2)) return false;
	  }
	 if (!matchName(n1)) return false;
	 RebaseJavaType jt = js.getType();
	 String n3 = jt.getName();
	 int idx3 = n3.lastIndexOf(")");
	 // int idx2 = n3.indexOf("(");
	 // String n4 = n3.substring(idx2+1,idx3);
	 if (!matchParameters(jt)) return false;
	 String n5 = n3.substring(idx3+1);
	 if (!js.isConstructorSymbol() && !matchType(n5)) return false;
       }
      else if (js.isFieldSymbol()) {
	 String n1 = js.getFullName();
	 if (!matchName(n1)) return false;
	 String n2 = js.getType().getName();
	 if (!matchType(n2)) return false;
       }
      else return false;

      return true;
    }


   private void fixNamePattern(String pat) {
      name_pattern = null;
      pat = pat.trim();
      if (pat == null || pat.length() == 0 || pat.equals("*")) return;
      String q1 = "([A-Za-z0-9$_]+\\.)*";
      if (pat.startsWith(".")) pat = pat.substring(1);
      String q2 = pat.replace(".","\\.");
      q2 = q2.replace("*","(.*)");
      name_pattern = Pattern.compile(q1 + q2);
    }

   private boolean matchName(String itm) {
      if (name_pattern == null) return true;
      if (itm == null) return false;
      if (name_pattern.matcher(itm).matches()) return true;
      return false;
    }

   private void fixGenericsPattern(String pat) {
      generics_pattern = null;
      if (pat == null) return;
      pat = pat.trim();
      if (pat.length() == 0 || pat.equals("*")) return;   generics_pattern = pat;
    }

   private boolean matchGenerics(String itm) {
      if (generics_pattern == null) return true;
      if (itm == null) return false;
      // TODO : fix this
      return true;
    }

   private void fixParametersPattern(String pat) {
      parameters_pattern = null;
      if (pat == null) return;
      pat = pat.replace(" ","");
      if (pat.length() == 0) return;
      parameters_pattern = new ArrayList<String>();
      extra_parameters = false;
      int lvl = 0;
      int spos = -1;
      for (int i = 0; i < pat.length(); ++i) {
	 char c = pat.charAt(i);
	 if (c == '(') continue;
	 else if (c == ')') {
	    if (spos >= 0) parameters_pattern.add(pat.substring(spos,i));
	    spos = -1;
	    break;
	  }
	 else if (c == '*') {
	    if (spos >= 0) parameters_pattern.add(pat.substring(spos,i));
	    spos = -1;
	    extra_parameters = true;
	    break;
	  }
	 else if (c == ',') {
	    if (spos >= 0) parameters_pattern.add(pat.substring(spos,i));
	    spos = -1;
	  }
	 else if (c == '<') {
	    if (spos >= 0) parameters_pattern.add(pat.substring(spos,i));
	    ++lvl;
	    spos = -1;
	  }
	 else if (c == '>') {
	    if (lvl > 0) --lvl;
	    spos = -1;
	  }
	 else if (spos < 0 && lvl == 0) {
	    spos = i;
	  }
       }
      if (spos > 0) parameters_pattern.add(pat.substring(spos));
    }

   private boolean matchParameters(RebaseJavaType jt) {
      if (parameters_pattern == null) return true;

      List<RebaseJavaType>  ptyps = jt.getComponents();
      for (int i = 0; i < parameters_pattern.size(); ++i) {
	 if (i >= ptyps.size()) return false;
	 String patstr = parameters_pattern.get(i);
	 String actstr = ptyps.get(i).getName();
	 String act1 = actstr;
	 int idx1 = actstr.indexOf("<");
	 if (idx1 >= 0) act1 = actstr.substring(0,idx1);
	 if (!patstr.equals(actstr) && !patstr.equals(act1)) {
	    return false;
	  }
       }
      if (ptyps.size() == parameters_pattern.size() || extra_parameters) return true;

      return false;
    }

   private void fixTypePattern(String pat) {
      type_pattern = null;
      if (pat == null) return;
      pat = pat.trim();
      if (pat.length() == 0 || pat.equals("*")) return;
      int idx1 = pat.indexOf("<");
      if (idx1 == 0) return;
      if (idx1 > 0) pat = pat.substring(0,idx1);
      if (pat.startsWith(".")) pat = pat.substring(1);
      String q1 = "([A-Za-z0-9$_]+\\.)*";
      String q2 = pat.replace(".","\\.");
      q2 = q2.replace("*","(.*)");
      type_pattern = Pattern.compile(q1 + q2);
    }

   private boolean matchType(String itm) {
      if (type_pattern == null) return true;
      if (itm == null) return false;
      if (type_pattern.matcher(itm).matches()) return true;
      return false;
    }

}	// end of inner class FindSymbolVisitor








/********************************************************************************/
/*										*/
/*	Visitor to find a location						*/
/*										*/
/********************************************************************************/

ASTVisitor getFindLocationVisitor(int soff,int eoff)
{
   return new FindLocationVisitor(soff,eoff);
}



private class FindLocationVisitor extends ASTVisitor {

   private int start_offset;
   private int end_offset;

   FindLocationVisitor(int soff,int eoff) {
      start_offset = soff;
      end_offset = eoff;
    }

   @Override public boolean preVisit2(ASTNode n) {
      int soff = n.getStartPosition();
      int eoff = soff + n.getLength();
      if (eoff < start_offset) return false;
      if (soff > end_offset) return false;
      return true;
    }

   @Override public boolean visit(SimpleName n) {
      RebaseJavaSymbol js = RebaseJavaAst.getDefinition(n);
      if (js == null) js = RebaseJavaAst.getReference(n);
      if (js == null) {
	 RebaseJavaType jt = RebaseJavaAst.getJavaType(n);
	 if (jt != null) js = jt.getDefinition();
      }
      if (js != null) {
	 match_symbols.add(js);
       }
      return false;
    }

}	// end of inner class FindLocationVisitor




ASTVisitor getFindByKeyVisitor(String key)
{
   return new FindByKeyVisitor(key);
}


private class FindByKeyVisitor extends ASTVisitor {

   private String using_key;

   FindByKeyVisitor(String key) {
      using_key = key;
    }

   @Override public void postVisit(ASTNode n) {
      RebaseJavaSymbol js = RebaseJavaAst.getDefinition(n);
      if (js != null && current_file != null) {
	 String hdl = js.getHandle(current_file.getFile());
	 if (hdl != null && hdl.equals(using_key)) {
	    match_symbols.add(js);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to find nodes associated with a definition			*/
/*										*/
/********************************************************************************/

ASTVisitor getLocationsVisitor(boolean defs,boolean refs,boolean impls,
      boolean ronly,boolean wonly)
{
   if (impls) {
      expandMatchesForImplementations();
    }

   return new LocationVisitor(defs,refs,ronly,wonly);
}



private void expandMatchesForImplementations()
{
   Set<RebaseJavaSymbol> add = new HashSet<RebaseJavaSymbol>();
   for (RebaseSymbol rs : match_symbols) {
      RebaseJavaSymbol js = (RebaseJavaSymbol) rs;
      if (js.isMethodSymbol() && !js.isConstructorSymbol()) {
	 RebaseJavaType jt = js.getType();
	 String nm = js.getName();
	 RebaseJavaType deft = js.getClassType();
	 Set<RebaseJavaType> chld = getChildTypes(deft);
	 for (RebaseJavaType ct : chld) {
	    RebaseJavaScope scp = ct.getScope();
	    if (scp != null) {
	       RebaseJavaSymbol xsym = scp.lookupMethod(nm,jt);
	       if (!match_symbols.contains(xsym)) add.add(xsym);
	    }
	  }
	
       }
    }

   match_symbols.addAll(add);
}


private Set<RebaseJavaType> getChildTypes(RebaseJavaType jt)
{
   Set<RebaseJavaType> rslt = new HashSet<RebaseJavaType>();

   List<RebaseJavaType> workq = new ArrayList<RebaseJavaType>();
   workq.add(jt);
   while (!workq.isEmpty()) {
      RebaseJavaType ct = workq.remove(0);
      for (RebaseJavaType xt : rebase_root.getAllTypes()) {
	 if (rslt.contains(xt) || xt == jt) continue;
	 if (xt.isClassType()) {
	    if (xt.isCompatibleWith(ct)) {
	       workq.add(xt);
	       rslt.add(xt);
	     }
	  }
       }
    }

   return rslt;
}



private class LocationVisitor extends ASTVisitor {

   private boolean use_defs;
   private boolean use_refs;
   private boolean read_only;
   private boolean write_only;
   private RebaseJavaSymbol cur_symbol;

   LocationVisitor(boolean def,boolean ref,boolean r,boolean w) {
      use_defs = def;
      use_refs = ref;
      read_only = r;
      write_only = w;
      cur_symbol = null;
    }

   @Override public void preVisit(ASTNode n) {
      RebaseJavaSymbol s = getRelevantSymbol(n);
      if (s != null) cur_symbol = s;
    }
   
   @Override public void postVisit(ASTNode n) {
      if (cur_symbol != null && cur_symbol == getRelevantSymbol(n)) {
	 if (checkReadWrite(n)) {
	    result_map.add(new Match(current_file,n,cur_symbol,getContainerSymbol(n)));
	  }
         cur_symbol = null;
       }
      
    }

   private RebaseJavaSymbol getRelevantSymbol(ASTNode n) {
      RebaseJavaSymbol js = null;
      if (use_defs) {
         js = RebaseJavaAst.getDefinition(n);
         if (js != null && match_symbols.contains(js))
            return js;
       }
      if (use_refs) {
         js = RebaseJavaAst.getReference(n);
         if (js != null && match_symbols.contains(js)) return js;
       }
      return null;
   }

   private RebaseJavaSymbol getContainerSymbol(ASTNode n) {
      while (n != null) {
         if (n instanceof BodyDeclaration) {
            RebaseJavaSymbol js = RebaseJavaAst.getDefinition(n);
            if (js != null) return js;
            else System.err.println("BAD BODY DECL");
          }
         else if (n instanceof VariableDeclarationFragment && n.getParent() instanceof FieldDeclaration) {
            RebaseJavaSymbol js = RebaseJavaAst.getDefinition(n);
            if (js != null) return js;
          }
         n = n.getParent();
      }
      return null;
   }


   private boolean checkReadWrite(ASTNode n) {
      if (read_only == write_only) return true;
   
      boolean write = false;
      boolean read = true;
   
      StructuralPropertyDescriptor spd = null;
      for (ASTNode p = n; p != null; p = p.getParent()) {
         boolean done = true;
         switch (p.getNodeType()) {
            case ASTNode.ASSIGNMENT :
               if (spd == Assignment.LEFT_HAND_SIDE_PROPERTY) {
        	  read = false;
        	  write = true;
        	}
               break;
            case ASTNode.POSTFIX_EXPRESSION :
               read = true;
               write = true;
               break;
            case ASTNode.PREFIX_EXPRESSION :
               PrefixExpression pfx = (PrefixExpression) p;
               String op = pfx.getOperator().toString();
               if (op.equals("++") || op.equals("--")) {
        	  read = true;
        	  write = true;
        	}
               break;
            case ASTNode.ARRAY_ACCESS :
               if (spd == ArrayAccess.ARRAY_PROPERTY) done = false;
               break;
            case ASTNode.SIMPLE_NAME :
            case ASTNode.QUALIFIED_NAME :
               done = false;
               break;
            case ASTNode.METHOD_DECLARATION :
            case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
            case ASTNode.FIELD_DECLARATION :
            case ASTNode.TYPE_DECLARATION :
            case ASTNode.ENUM_DECLARATION :
               read = true;
               write = true;
               break;
            default :
               break;
          }
         spd = p.getLocationInParent();
         if (done) break;
       }
   
      if (read && read_only) return true;
      if (write && write_only) return true;
   
      return false;
    }

}	// end of inner class LocationsVisitor




/********************************************************************************/
/*										*/
/*	Search Result								*/
/*										*/
/********************************************************************************/

private static class Match implements SearchResult {

   private int match_start;
   private int match_length;
   private RebaseFile match_file;
   private RebaseJavaSymbol match_symbol;
   private RebaseJavaSymbol container_symbol;

   Match(RebaseJavaFile jf,ASTNode n,RebaseJavaSymbol js,RebaseJavaSymbol cntr) {
      match_start = n.getStartPosition();
      match_length = n.getLength();
      match_file = jf.getFile();
      match_symbol = js;
      container_symbol = cntr;
    }

   @Override public int getOffset()			{ return match_start; }
   @Override public int getLength()			{ return match_length; }
   @Override public RebaseJavaSymbol getSymbol()	{ return match_symbol; }
   @Override public RebaseJavaSymbol getContainer()	{ return container_symbol; }
   @Override public RebaseFile getFile()		{ return match_file; }

}	// end of inner class Match


}	// end of class RebaseJavaSearch




/* end of RebaseJavaSearch.java */







