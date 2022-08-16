/********************************************************************************/
/*										*/
/*		NobaseSearchInstance.java					*/
/*										*/
/*	Class to handle a search instance					*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.wst.jsdt.core.dom.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

class NobaseSearchInstance implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseProject	for_project;
private Set<NobaseSymbol> match_symbols;
private List<SearchResult> result_set;
private NobaseFile	 current_file;
private String          name_found;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseSearchInstance(NobaseProject proj)
{
   for_project = proj;
   match_symbols = new HashSet<NobaseSymbol>();
   result_set = new ArrayList<SearchResult>();
   current_file = null;
   name_found = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setFile(NobaseFile jf)			{ current_file = jf; }

List<SearchResult> getMatches() 		{ return result_set; }
Set<NobaseSymbol> getSymbols()			{ return match_symbols; }
String getNameFound()                           { return name_found; }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputSearchFor(IvyXmlWriter xw)
{
   String what = null;
   String nm = null;
   for (NobaseSymbol rs : match_symbols) {
      String rwhat = "Variable";
      NobaseValue nv = rs.getValue();
      if (nv != null && nv.isFunction()) {
	 rwhat = "Function";
       }
      else {
	 String qnm = rs.getBubblesName();
	 if (qnm == null) rwhat = "Local";
	 else {
	    int idx = qnm.indexOf(".");
	    if (idx >= 0) {
	       idx = qnm.indexOf(".",idx+1);
	    }
	    if (idx >= 0) rwhat = "Local";
	 }
       }

      if (nm == null) nm = rs.getName();
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
   EnumSet<NameType> kindset = EnumSet.noneOf(NameType.class);

   switch (kind) {
      case "TYPE" :
      case "CLASS" :
      case "ENUM" :
      case "INTERFACE" :
      case "CLASS&ENUM" :
      case "CLASS&INTERFACE" :
      case "METHOD" :
      case "CONSTRUCTOR" :
	 p1 = p1.replace("(...)","()");
	 kindset.add(NameType.FUNCTION);
	 break;
      case "FIELD" :
	 kindset.add(NameType.VARIABLE);
	 int idx8 = p1.indexOf(" ");
	 if (idx8 > 0) {
	    p1 = p1.substring(0,idx8);
	  }
	 break;
      case "PACKAGE" :
	 kindset.add(NameType.MODULE);
	 break;
    }

   return new FindSymbolVisitor(p1,kindset);
}




private class FindSymbolVisitor extends ASTVisitor {

   private Pattern name_pattern;
   private EnumSet<NameType> search_kind;

   FindSymbolVisitor(String np,EnumSet<NameType> kinds) {
      fixNamePattern(np);
      search_kind = kinds;
    }


   @Override public void postVisit(ASTNode n) {
      NobaseSymbol js = NobaseAst.getDefinition(n);
      if (js == null) return;
      if (match(js)) {
         match_symbols.add(js);
       }
    }

   private boolean match(NobaseSymbol js) {
      if (!search_kind.contains(js.getNameType())) return false;
      String n1 = js.getHandle();
      int idx1 = n1.indexOf(":");
      if (idx1 >= 0) n1 = n1.substring(idx1+1);
      if (matchName(n1)) return true;
   // int idx2 = n1.indexOf("()");
   // if (idx2 > 0) {
   //    String n2 = n1.substring(0,idx2);
   //    if (matchName(n2)) return true;
   //  }
      return false;
    }


   private void fixNamePattern(String pat) {
      name_pattern = null;
      pat = pat.trim();
      if (pat == null || pat.length() == 0 || pat.equals("*")) return;
      String q1 = "([A-Za-z0-9$_]+\\.)*";
      if (pat.startsWith(".")) pat = pat.substring(1);
      String q2 = pat.replace(".","\\.");
      q2 = q2.replace("*","(.*)");
      q2 = q2.replace("()","\\(\\)");
      name_pattern = Pattern.compile(q1 + q2);
    }

   private boolean matchName(String itm) {
      if (name_pattern == null) return true;
      if (itm == null) return false;
      if (name_pattern.matcher(itm).matches()) return true;
      return false;
    }



}	// end of inner class FindSymbolVisitor

/********************************************************************************/
/*										*/
/*	Class to holde results							*/
/*										*/
/********************************************************************************/


private static class Match implements SearchResult {

   private int match_start;
   private int match_length;
   private NobaseFile match_file;
   private NobaseSymbol match_symbol;
   private NobaseSymbol container_symbol;

   Match(NobaseFile jf,ASTNode n,NobaseSymbol js,NobaseSymbol cntr) {
      match_start = n.getStartPosition();
      match_length = n.getLength();
      match_file = jf;
      match_symbol = js;
      container_symbol = cntr;
    }

   @Override public int getOffset()			{ return match_start; }
   @Override public int getLength()			{ return match_length; }
   @Override public NobaseSymbol getSymbol()		{ return match_symbol; }
   @Override public NobaseSymbol getContainer() 	{ return container_symbol; }
   @Override public NobaseFile getFile()		{ return match_file; }

}	// end of inner class Match




/********************************************************************************/
/*										*/
/*	Visitor to find a location						*/
/*										*/
/********************************************************************************/

ASTVisitor getFindLocationVisitor(int soff,int eoff)
{
   return new FindLocationVisitor(soff,eoff);
}



private class FindLocationVisitor extends DefaultASTVisitor {

   private int start_offset;
   private int end_offset;

   FindLocationVisitor(int soff,int eoff) {
      start_offset = soff;
      end_offset = eoff;
    }

   @Override public boolean visitNode(ASTNode n) {
      return inScope(n);
    }

   @Override public boolean visit(SimpleName n) {
      if (!inScope(n)) return false;
      NobaseSymbol js = NobaseAst.getDefinition(n);
      if (js == null) js = NobaseAst.getReference(n);
      if (js == null) {
         js = NobaseAst.getDefinition(n.getParent());
         if (js == null) js = NobaseAst.getReference(n.getParent());
       }
      if (js == null) {
         name_found = n.getIdentifier();
         NobaseValue nv = NobaseAst.getNobaseValue(n);
         if (nv != null) {
            Collection<NobaseSymbol> defs = nv.getDefinitions();
            if (defs.size() > 0) {
               match_symbols.addAll(defs);
               return false;
            }
         }
      }
      if (js == null) {
         ASTNode par = n.getParent();
         if (par instanceof QualifiedName || par instanceof FieldAccess) {
            match_symbols.addAll(findAllSymbols(n.getIdentifier()));
          }
         else NobaseMain.logE("Unknown name selected in " + par.getClass() + " " + par);
       }
      if (js != null) {
         match_symbols.add(js);
       }
      return false;
   }
   
   private boolean inScope(ASTNode n) {   
      int soff = n.getStartPosition();
      int eoff = soff + n.getLength();
      if (eoff < start_offset) return false;
      if (soff > end_offset) return false;
      return true;
    }

}	// end of inner class FindLocationVisitor




/********************************************************************************/
/*										*/
/*	Hnadle find by key							*/
/*										*/
/********************************************************************************/

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
      NobaseSymbol js = NobaseAst.getDefinition(n);
      if (js != null && current_file != null) {
         String hdl = js.getHandle();
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
   // follow prototype chains and add parents
}




private class LocationVisitor extends ASTVisitor {

   private boolean use_defs;
   private boolean use_refs;
   private boolean read_only;
   private boolean write_only;
   private NobaseSymbol cur_symbol;
   private NobaseSymbol default_def;

   LocationVisitor(boolean def,boolean ref,boolean r,boolean w) {
      use_defs = def;
      use_refs = ref;
      read_only = r;
      write_only = w;
      cur_symbol = null;
      default_def = null;
      for (NobaseSymbol ns : match_symbols) {
         default_def = ns;
         if (name_found == null) name_found = ns.getName();
         break;
       }
    }

   @Override public void preVisit(ASTNode n) {
      NobaseSymbol s = getRelevantSymbol(n);
      if (s != null) cur_symbol = s;
    }

   @Override public void postVisit(ASTNode n) {
      if (cur_symbol != null && cur_symbol == getRelevantSymbol(n)) {
         if (checkReadWrite(n)) {
            result_set.add(new Match(current_file,n,cur_symbol,getContainerSymbol(n)));
          }
         cur_symbol = null;
       }
    }

   private NobaseSymbol getRelevantSymbol(ASTNode n) {
      NobaseSymbol js = null;
      if (use_defs) {
         js = NobaseAst.getDefinition(n);
         if (js != null && match_symbols.contains(js))
            return js;
       }
      if (use_refs) {
         js = NobaseAst.getReference(n);
         if (js != null && match_symbols.contains(js)) return js;
         NobaseValue nv = NobaseAst.getNobaseValue(n);
         if (nv != null) {
            Collection<NobaseSymbol> refs = nv.getDefinitions();
            if (refs != null && refs.size() > 0) {
               for (NobaseSymbol ns : refs) {
                  if (match_symbols.contains(ns))
                     return ns;
                }
             }
          }
         if (js == null && n instanceof SimpleName && default_def != null && name_found != null) {
            SimpleName sn = (SimpleName) n;
            if (sn.getIdentifier().equals(name_found)) return default_def;
          }
       }
      return null;
    }

   private NobaseSymbol getContainerSymbol(ASTNode n) {
      while (n != null) {
         if (n instanceof FunctionExpression) {
            NobaseSymbol js = NobaseAst.getDefinition(n);
            if (js != null && !NobaseResolver.isGeneratedName(js)) return js;
          }
         else if (n instanceof VariableDeclaration) {
            NobaseSymbol js = NobaseAst.getDefinition(n);
            if (js != null && js.getNameType() == NameType.FUNCTION &&
                  !NobaseResolver.isGeneratedName(js)) return js;
            else break;
          }
         else if (n instanceof FunctionDeclaration) {
            NobaseSymbol js = NobaseAst.getDefinition(n);
            if (js != null && !NobaseResolver.isGeneratedName(js)) return js;
          }
         else if (n instanceof TypeDeclaration) {
            NobaseSymbol js = NobaseAst.getDefinition(n);
            if (js != null && !NobaseResolver.isGeneratedName(js)) return js;  
          }
         n = n.getParent();
       }
      return null;
    }


   private boolean checkReadWrite(ASTNode n) {
      if (read_only == write_only) return true;
   
      boolean write = false;
      boolean read = true;
      if (n instanceof Expression)
         if (isLeftHandSide(n)) {
            write = true;
            for (ASTNode p = n; p != null; p = p.getParent()) {
               if (p instanceof Assignment) {
        	  Assignment op = (Assignment) p;
        	  if (op.getOperator().equals(Assignment.Operator.ASSIGN)) read = false;
        	  break;
        	}
               else if (!(p instanceof Expression)) break;
             }
          }
   
      if (read && read_only) return true;
      if (write && write_only) return true;
   
      return false;
    }

   private boolean isLeftHandSide(ASTNode n) {
      ASTNode prev = n;
      for (ASTNode p = n.getParent(); p != null; p = p.getParent()) {
         if (!(p instanceof Expression)) break;
         StructuralPropertyDescriptor spd = prev.getLocationInParent();
         if (p instanceof Assignment) {
            if (spd == Assignment.LEFT_HAND_SIDE_PROPERTY) return true;
            return false;
          }
         else if (p instanceof ArrayAccess) {
            if (spd != ArrayAccess.ARRAY_PROPERTY) return false;
          }
         else if (p instanceof FieldAccess) {
            if (spd != FieldAccess.NAME_PROPERTY) return false;
          }
         else break;
         prev = p;
       }
      return false;
    }
   
}	// end of inner class LocationsVisitor



/********************************************************************************/
/*										*/
/*	Methods to find regions 						*/
/*										*/
/********************************************************************************/

public void findTextRegions(ISemanticData isd,boolean pfx,boolean statics,boolean compunit,
      boolean imports,boolean pkgfg,boolean topdecls,boolean fields,boolean all,
      IvyXmlWriter xw)
	throws NobaseException
{
   String fnm = isd.getFileData().getFile().getPath();
   ASTNode root = isd.getRootNode();
   if (root == null)
      throw new NobaseException("Can't get ast for " + fnm);

   if (compunit) {
      xw.begin("RANGE");
      xw.field("PATH",fnm);
      xw.field("START",0);
      long len = isd.getFileData().getFile().length();
      xw.field("END",len);
      xw.end("RANGE");
    }
   boolean initcmmts = false;
   boolean requires = false;
   boolean exports = false;
   boolean fctdecls = false;
   boolean computations = false;
   boolean vardecls = false;

   if (pfx) {
      initcmmts = true;
      requires = true;
      exports = true;
    }
   if (imports) {
      requires = true;
    }
   if (topdecls) {
      initcmmts = true;
      fctdecls = true;
      vardecls = true;
    }
   if (statics) {
      computations = true;
    }
   if (fields) {
      vardecls = true;
    }
   if (all) {
      fctdecls = true;
      vardecls = true;
    }

   RegionVisitor rv = new RegionVisitor(xw,initcmmts,requires,exports,
	 fctdecls,vardecls,computations);
   isd.getRootNode().accept(rv);
}


private class RegionVisitor extends DefaultASTVisitor {

   private IvyXmlWriter xml_writer;
   private boolean do_comments;
   private boolean do_requires;
   private boolean do_exports;
   private boolean do_functions;
   private boolean do_variables;
   private boolean do_computations;
   private int current_depth;

   RegionVisitor(IvyXmlWriter xw,boolean cmmt,boolean req,
	 boolean exp,boolean fct,boolean var,boolean c) {
      xml_writer = xw;
      do_comments = cmmt;
      do_requires = req;
      do_exports = exp;
      do_functions = fct;
      do_variables = var;
      do_computations = c;
      current_depth = 0;
    }

   @Override public boolean visit(Block n) {
      if (current_depth++ == 0) {
	 if (do_comments) {
	    n.getStartPosition();
	  }
	 return true;
       }
      return false;
    }

   @Override public void endVisit(Block n) {
      --current_depth;
    }
   
   @Override public boolean visit(VariableDeclarationFragment n) {
      NobaseSymbol nsym = NobaseAst.getDefinition(n);
      if (nsym == null) return false;
      boolean output = false;
      switch (nsym.getNameType()) {
         case FUNCTION :
            output |= do_functions;
            break;
         case LOCAL :
            break;
         case MODULE :
            break;
         case CLASS :
            break;
         case VARIABLE :
            output = do_variables;
            break;
       }
      if (output) nsym.outputNameData(current_file,xml_writer);
      return false;
    }

   @Override public boolean visit(FunctionDeclaration n) {
      NobaseSymbol nsym = NobaseAst.getDefinition(n);
      if (nsym == null) return false;
      if (do_functions) nsym.outputNameData(current_file,xml_writer);
      return false;
    }


   @Override public boolean visitNode(ASTNode n) {
      if (n instanceof VariableDeclarationStatement &&
            n.getParent() instanceof JavaScriptUnit) {
         return super.visitNode(n);
       }
      if (n instanceof Statement) {
         if (do_computations) outputRange(n);
         return false;
       }
      return super.visitNode(n);
    }

   @Override public boolean visit(ExpressionStatement n) {
      boolean doout = false;
      if (do_computations) doout = true;
      else {
         Expression exp = n.getExpression();
         if (exp instanceof Assignment) {
            Assignment aop = (Assignment) exp;
            if (aop.getOperator().equals(Assignment.Operator.ASSIGN)) {
               NobaseValue nv = NobaseAst.getNobaseValue(aop.getRightHandSide());
               if (nv != null && nv.isFunction()) {
                  doout |= do_functions;
                }
               Expression lhs = aop.getLeftHandSide();
               if (lhs instanceof FieldAccess) {
                  Expression m1 = ((FieldAccess)lhs).getExpression();
                  if (m1 instanceof SimpleName) {
                     SimpleName r1 = (SimpleName) m1;
                     if (r1.getIdentifier().equals("exports")) {
                        doout |= do_exports;
                      }
                     else if (lhs.toString().equals("module.exports")) {
                        doout |= do_exports;
                      }
                   }
                }
               Expression rhs = aop.getRightHandSide();
               if (rhs instanceof FunctionInvocation) {
                  FunctionInvocation fc = (FunctionInvocation) rhs;
                  Expression farg = fc.getExpression();
                  if (farg instanceof SimpleName) {
                     SimpleName id = (SimpleName) farg;
                     if (id.getIdentifier().equals("require")) {
                        doout |= do_requires;
                      }
                   }
                }
             }
          }
       }
   
      if (doout) outputRange(n);
   
      return false;
    }

   private void outputRange(ASTNode n) {
      int spos = NobaseAst.getExtendedStartPosition(n);
      int epos = NobaseAst.getExtendedEndPosition(n);
      xml_writer.begin("RANGE");
      xml_writer.field("PATH",current_file.getFile().getPath());
      xml_writer.field("START",spos);
      xml_writer.field("END",epos);
      xml_writer.end("RANGE");
    }

}


/********************************************************************************/
/*										*/
/*	Find all names matching a pattern					*/
/*										*/
/********************************************************************************/

private Collection<NobaseSymbol> findAllSymbols(String pat)
{
   Collection<NobaseSymbol> rslt = new ArrayList<NobaseSymbol>();

   for (NobaseFile nf : for_project.getAllFiles()) {
      ISemanticData isd = for_project.getParseData(nf);
      if (isd != null && isd.getRootNode() != null) {
	 NobaseScope scp = NobaseAst.getScope(isd.getRootNode());
	 if (scp != null) {
	    Collection<NobaseSymbol> syms = scp.findAll(pat);
	    if (syms != null) rslt.addAll(syms);
	  }
       }
    }

   return rslt;
}

}	// end of class NobaseSearchInstance




/* end of NobaseSearchInstance.java */

