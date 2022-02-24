/********************************************************************************/
/*										*/
/*		PybaseSearch.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.Found;
import edu.brown.cs.bubbles.pybase.symbols.GenAndTok;
import edu.brown.cs.bubbles.pybase.symbols.NodeUtils;
import edu.brown.cs.bubbles.pybase.symbols.SourceToken;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.expr_contextType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


class PybaseSearch implements PybaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private PybaseMain pybase_main;

private static Map<SearchFor,Set<TokenType>> search_types;

static {
   search_types = new EnumMap<SearchFor,Set<TokenType>>(SearchFor.class);
   search_types.put(SearchFor.NONE,EnumSet.allOf(TokenType.class));
   search_types.put(SearchFor.ANNOTATION,EnumSet.of(TokenType.EPYDOC));
   search_types.put(SearchFor.CONSTRUCTOR,EnumSet.of(TokenType.FUNCTION));
   search_types.put(SearchFor.METHOD,EnumSet.of(TokenType.FUNCTION));
   search_types.put(SearchFor.FIELD,EnumSet.of(TokenType.ATTR));
   search_types.put(SearchFor.TYPE,EnumSet.of(TokenType.CLASS));
   search_types.put(SearchFor.PACKAGE,EnumSet.of(TokenType.PACKAGE));
   search_types.put(SearchFor.CLASS,EnumSet.of(TokenType.CLASS));
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseSearch(PybaseMain pm)
{
   pybase_main = pm;
}



/********************************************************************************/
/*										*/
/*	Pattern search methods							*/
/*										*/
/********************************************************************************/

void handlePatternSearch(String proj,String pat,SearchFor sf,
      boolean defs,boolean refs,boolean sys,IvyXmlWriter xw)
	throws PybaseException
{
   List<ISemanticData> sds = pybase_main.getProjectManager().getAllSemanticData(proj);
   SearchPattern sp = new SearchPattern(pat,sf,defs,refs);
   for (ISemanticData sd : sds) {
      PybaseScopeItems scp = sd.getGlobalScope();
      List<SourceToken> rslt = getMatches(sd,scp,sp);
      if (rslt == null || rslt.size() == 0) continue;
      Collections.sort(rslt,new TokenComparer());
      for (SourceToken t : rslt) {
	 PybaseUtil.outputSearchMatch(sd,null,t,xw);
       }
    }
}



private List<SourceToken> getMatches(ISemanticData sd,PybaseScopeItems scp,SearchPattern sp)
{
   if (scp == null) return null;

   List<SourceToken> rslt = new ArrayList<SourceToken>();
   for (Found f : scp.getAllSymbols()) {
      if (sp.match(sd,f)) {
	 AbstractToken g = f.getSingle().getGenerator();
	 if (sp.getDoDefs()) {
	    if (g instanceof SourceToken) {
	       SourceToken sg = (SourceToken) g;
	       SimpleNode sgn = sg.getAst();
	       boolean isok = true;
	       switch (sp.getSearchFor()) {
		  case FIELD :
		     if (!(sgn instanceof Name) || sgn.parent == null || !(sgn.parent instanceof Assign)) isok = false;
		     break;
		  default :
		     break;
		}
	       if (isok) {
		  rslt.add(sg);
		}
	     }
	  }
	 if (sp.getDoRefs()) {
	    for (GenAndTok gat : f.getAll()) {
	       for (AbstractToken t1 : gat.getReferences()) {
		  if (t1 instanceof SourceToken) rslt.add((SourceToken) t1);
		}
	     }
	  }
       }
    }

   if (scp.getChildren() != null) {
      for (PybaseScopeItems cscp : scp.getChildren()) {
	 if (cscp.getScopeType() != ScopeType.CLASS) continue;
	 List<SourceToken> xrslt = getMatches(sd,cscp,sp);
	 if (xrslt != null) rslt.addAll(xrslt);
       }
    }

   return rslt;
}


void handleKeySearch(String proj,String file,String key,IvyXmlWriter xw) throws PybaseException
{
   SearchFor sf = SearchFor.FIELD;
   if (key.endsWith("()")) sf = SearchFor.METHOD;
   handlePatternSearch(proj,key,sf,true,false,false,xw);
}




private static class TokenComparer implements Comparator<SourceToken> {

   @Override public int compare(SourceToken t1,SourceToken t2) {
      if (t1 == t2) return 0;
      SimpleNode s1 = t1.getNameOrNameTokAst();
      SimpleNode s2 = t2.getNameOrNameTokAst();
      if (s1.beginLine < s2.beginLine) return -1;
      if (s1.beginLine > s2.beginLine) return 1;
      if (s1.beginColumn < s2.beginColumn) return -1;
      if (s1.beginColumn > s2.beginColumn) return 1;
      return t1.getRepresentation().compareTo(t2.getRepresentation());
    }

}


/********************************************************************************/
/*										*/
/*	Methods to handle variable searches					*/
/*										*/
/********************************************************************************/

void handleFindAll(String proj,String file,int start,int end,
      boolean defs,boolean refs,boolean impls,boolean equiv,boolean exact,
      boolean system,boolean typeof,boolean ronly,boolean wonly,
      IvyXmlWriter xw) throws PybaseException
{
   PybaseProject pp = pybase_main.getProjectManager().findProject(proj);
   if (pp == null) throw new PybaseException("Can't find project " + proj);
   ISemanticData sd = null;
   IFileData ifd = null;
   if (file != null) {
      try {
	 File f1 = new File(file);
	 file = f1.getCanonicalPath();
       }
      catch (IOException e) { }
    }

   sd = pp.getSemanticData(file);
   if (sd == null) throw new PybaseException("Can't find file " + file);
   ifd = sd.getFileData();

   FindTokenVisitor ftv = new FindTokenVisitor(ifd,start,end);
   SimpleNode ftok = null;
   try {
      sd.getRootNode().accept(ftv);
      ftok = ftv.getToken();
    }
   catch (Exception e) { }
   if (ftok == null) throw new PybaseException("No symbol at location");

   Found f = sd.getGlobalScope().findByToken(ftok);
   if (f != null) {
      AbstractToken g = f.getSingle().getGenerator();
      PybaseUtil.outputSearchFor(ftok,g,xw);
      if (defs) {
	 if (system || g instanceof SourceToken) {
	    PybaseUtil.outputSearchMatch(sd,g,g,xw);
	  }
       }
      if (refs) {
	 List<GenAndTok> xrefs = f.getAll();
	 for (GenAndTok gat : xrefs) {
	    List<AbstractToken> trefs = gat.getReferences();
	    AbstractToken g1 = gat.getGenerator();
	    if (trefs.isEmpty() && g1 != null && !(g1 instanceof SourceToken)) {
	       trefs = sd.getGlobalScope().findAllRefs(g);
	    }
	    if (g1 instanceof SourceToken && !defs && g1.getType() == TokenType.ATTR) {
	       PybaseUtil.outputSearchMatch(sd,g,g,xw);
	     }
	    for (AbstractToken t1 : trefs) {
	       if (!system && !(t1 instanceof SourceToken)) continue;
	       if (t1 instanceof SourceToken) {
		  if (!checkReadWrite(sd,g,(SourceToken) t1,ronly,wonly)) continue;
		}
	       PybaseUtil.outputSearchMatch(sd,g,t1,xw);
	     }
	  }
       }
    }
}


private boolean checkReadWrite(ISemanticData sd,AbstractToken src,SourceToken st,boolean ronly,boolean wonly)
{
   if (!ronly && !wonly) return true;
   if (ronly && wonly) return true;

   NameTok n1 = st.getNameTokAst();
   if (n1 != null && n1.id.equals(src.getRepresentation())) {
      if (wonly) return true;
      if (ronly) return false;
    }
   Name n2 = st.getNameAst();
   if (n2 == null) return false;
   switch (n2.ctx) {
      case expr_contextType.Artificial :
	 return false;
      case expr_contextType.AugLoad :
      case expr_contextType.Load :
      case expr_contextType.Param :
      case expr_contextType.KwOnlyParam :
	 if (ronly) return true;
	 break;
      case expr_contextType.AugStore :
      case expr_contextType.Del :
      case expr_contextType.Store :
	 if (wonly) return true;
    }
   return false;
}






/********************************************************************************/
/*										*/
/*	Handle finding fully qualified name					*/
/*										*/
/********************************************************************************/

void getFullyQualifiedName(String proj,String file,int start,int end,
      IvyXmlWriter xw) throws PybaseException
{
   PybaseProject pp = pybase_main.getProjectManager().findProject(proj);
   if (pp == null) throw new PybaseException("Can't find project " + proj);
   ISemanticData sd = null;
   IFileData ifd = null;
   if (file != null) {
      try {
	 File f1 = new File(file);
	 file = f1.getCanonicalPath();
       }
      catch (IOException e) { }
    }
   for (IFileData xfd : pp.getAllFiles()) {
      if (xfd.getFile().getPath().equals(file)) {
	 ifd = xfd;
	 sd = pp.getParseData(xfd);
	 break;
       }
    }
   if (sd == null || ifd == null) throw new PybaseException("Can't find file " + file);

   FindTokenVisitor ftv = new FindTokenVisitor(ifd,start,end);
   SimpleNode ftok = null;
   try {
      sd.getRootNode().accept(ftv);
      ftok = ftv.getToken();
    }
   catch (Exception e) { }
   if (ftok == null) throw new PybaseException("No symbol at location");

   String rslt = null;

   Found f = sd.getGlobalScope().findByToken(ftok);
   if (f == null) {
      if (ftok.parent instanceof Attribute) {
	 Attribute a = (Attribute) ftok.parent;
	 if (ftok == a.attr) {
	    Found f1 = sd.getGlobalScope().findByToken(a.value);
	    GenAndTok gat1 = f1.getSingle();
	    AbstractToken t1 = gat1.getToken();
	    switch (t1.getType()) {
	       case CLASS :
	       case IMPORT :
	       case RELATIVE_IMPORT :
	       case BUILTIN :
	       case OBJECT_FOUND_INTERFACE :
		  rslt = t1.getAsAbsoluteImport();
		  if (rslt == null) rslt = ftok.getImage().toString();
		  else rslt += "." + ftok.getImage().toString();
		  break;
	       default :
		  break;
	    }
	 }
      }
   }
   else {
      GenAndTok gat = f.getSingle();
      rslt = gat.getToken().getAsAbsoluteImport();
   }

   if (rslt != null) {
      xw.begin("FULLYQUALIFIEDNAME");
      xw.field("NAME",rslt);
      xw.end();
   }
}



/********************************************************************************/
/*										*/
/*	Handle file regions (attributes, imports, ...)				*/
/*										*/
/********************************************************************************/

void getTextRegions(String proj,String bid,String file,String cls,
      boolean prefix,boolean statics,boolean compunit,boolean imports,
      boolean pkg,boolean topdecls,boolean main,boolean fields,
      boolean all,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseProject pp = pybase_main.getProjectManager().findProject(proj);
   if (pp == null) throw new PybaseException("Can't find project " + proj);

   if (file == null) {
      String mnm = cls;
      while (mnm != null) {
	 try {
	    File f1 = pp.findModuleFile(mnm);
	    if (f1 != null) {
	       file = f1.getAbsolutePath();
	       break;
	     }
	  }
	 catch (Throwable t) { }
	 int idx = mnm.lastIndexOf(".");
	 if (idx < 0) break;
	 mnm = mnm.substring(0,idx);
       }
      if (file == null) throw new PybaseException("File must be given");
    }
   try {
      File f1 = new File(file);
      file = f1.getCanonicalPath();
    }
   catch (IOException e) { }

   ISemanticData sd = pp.getSemanticData(file);
   if (sd == null) throw new PybaseException("Can't find file data for " + file);
   IFileData ifd = sd.getFileData();
   String mnm = ifd.getModuleName();
   String fnd = null;
   if (prefix) {
      if (cls == null || !cls.startsWith(mnm)) {
	 throw new PybaseException("File not correct for given module/class");
      }
      fnd = cls.substring(mnm.length());
      if (fnd.length() == 0) {
	 prefix = false;
	 imports = true;
      }
   }

   List<SimpleNode> rgns = null;
   SimpleNode root = sd.getRootNode();
   if (root == null) return;

   if (compunit) {
      ModuleClassVisitor mcv = new ModuleClassVisitor(fnd,false);
      try {
	 root.accept(mcv);
	 SimpleNode sn = mcv.getItem();
	 if (sn != null) {
	    if (rgns == null) rgns = new ArrayList<SimpleNode>();
	    rgns.add(sn);
	  }
       }
      catch (Exception e) {
	 throw new PybaseException("Problem getting regions",e);
       }
    }
   else if (prefix) {
      ModuleClassVisitor mcv = new ModuleClassVisitor(fnd,false);
      try {
	 root.accept(mcv);
	 SimpleNode sn = mcv.getItem();
	 if (sn != null) {
	    if (sn instanceof ClassDef) {
	       ClassDef cn = (ClassDef) sn;
	       if (rgns == null) rgns = new ArrayList<SimpleNode>();
	       NameTok ntt = (NameTok) cn.name.createCopy();
	       ifd.setStart(ntt,cn.beginLine,cn.beginColumn);
	       ifd.setEnd(ntt,ifd.getEndOffset(cn.name));
	       rgns.add(ntt);
	       if (cn.bases != null) for (SimpleNode xn : cn.bases) rgns.add(xn);
	       if (cn.keywords != null) for (SimpleNode xn : cn.keywords) rgns.add(xn);
	       if (cn.kwargs != null) rgns.add(cn.kwargs);
	       if (cn.starargs != null) rgns.add(cn.starargs);
	     }
	  }
       }
      catch (Exception e) {
	 throw new PybaseException("Problem getting regions",e);
       }
    }
   else {
      RegionVisitor rv = new RegionVisitor(imports,statics,main);
      try {
	 root.accept(rv);
       }
      catch (Exception e) {
	 throw new PybaseException("Problem getting regions",e);
       }
      rgns = rv.getRegions();
    }

   if (rgns == null) return;

   for (SimpleNode sn : rgns) {
      xw.begin("RANGE");
      xw.field("PATH",file);
      xw.field("START",ifd.getStartOffset(sn));
      xw.field("END",ifd.getEndOffset(sn));
      xw.end("RANGE");
    }
}




private class RegionVisitor extends Visitor {

   private boolean do_imports;
   private boolean do_evals;
   private boolean main_eval;
   private List<SimpleNode> result_nodes;

   RegionVisitor(boolean imp,boolean evls,boolean main) {
      do_imports = imp;
      do_evals = evls;
      main_eval = main;
      result_nodes = new ArrayList<SimpleNode>();
    }

   List<SimpleNode> getRegions()	{ return result_nodes; }

   @Override public void traverse(SimpleNode n) throws Exception {
      if (n instanceof Module) {
	 super.traverse(n);
       }
    }

   @Override public Object unhandled_node(SimpleNode n) throws Exception {
      if (n.parent != null && n.parent instanceof Module) {
	 if (n instanceof Import || n instanceof ImportFrom) {
	    if (do_imports) result_nodes.add(n);
	  }
	 else if (n instanceof Assign) ;
	 else if (n instanceof FunctionDef) ;
	 else if (n instanceof ClassDef) ;
	 else if (main_eval && NodeUtils.isIfMainNode(n)) {
	    result_nodes.add(n);
	  }
	 else if (!main_eval && do_evals) {
	    result_nodes.add(n);
	  }
       }
      return super.unhandled_node(n);
    }

}	// end of inner class RegionVisitor




private class ModuleClassVisitor extends Visitor {

   private String find_item;
   private String cur_item;
   private SimpleNode found_node;

   ModuleClassVisitor(String find,boolean pfx) {
      find_item = find;
      cur_item = null;
      found_node = null;
    }

   SimpleNode getItem() 		{ return found_node; }

   @Override public void traverse(SimpleNode n) throws Exception {
      if (n instanceof Module) {
	 if (find_item == null || find_item.length() == 0) {
	    found_node = n;
	  }
	 else {
	    cur_item = ".";
	    super.traverse(n);
	  }
       }
      else if (n instanceof ClassDef) {
	 String nm = ((NameTok)((ClassDef) n).name).id;
	 String fnd = cur_item + nm;
	 int ln = fnd.length();
	 if (find_item.equals(fnd)) {
	    found_node = n;
	  }
	 else if (find_item.startsWith(fnd) && find_item.charAt(ln) == '.') {
	    cur_item = fnd;
	    super.traverse(n);
	  }
       }
    }

}	// end of inner class ModuleClassVisitor





/********************************************************************************/
/*										*/
/*	Text Search commands							*/
/*										*/
/********************************************************************************/

void handleTextSearch(String proj,int fgs,String pat,int maxresult,IvyXmlWriter xw)
	throws PybaseException
{
   Pattern pp = null;
   try {
      pp = Pattern.compile(pat,fgs);
    }
   catch (PatternSyntaxException e) {
      pp = Pattern.compile(pat,fgs|Pattern.LITERAL);
    }

   Pattern filepat = null;

   List<ISemanticData> sds = pybase_main.getProjectManager().getAllSemanticData(proj);
   int rct = 0;
   for (ISemanticData sd : sds) {
      IFileData ifd = sd.getFileData();
      if (filepat != null) {
	 String fnm = ifd.getFile().getPath();
	 Matcher m = filepat.matcher(fnm);
	 if (!m.matches()) continue;
       }
      IDocument d = ifd.getDocument();
      String s = d.get();
      Matcher m = pp.matcher(s);
      while (m.find()) {
	 if (++rct > maxresult) break;
	 xw.begin("MATCH");
	 xw.field("STARTOFFSET",m.start());
	 xw.field("LENGTH",m.end() - m.start());
	 xw.field("FILE",ifd.getFile().getPath());
	 FindOuterVisitor ov = new FindOuterVisitor(ifd,m.start(),m.end());
	 SimpleNode root = sd.getRootNode();
	 try {
	    root.accept(ov);
	    SimpleNode itm = ov.getItem();
	    if (itm != null) {
	       Found f = sd.getGlobalScope().findByToken(itm);
	       if (f != null) {
		  AbstractToken g = f.getSingle().getGenerator();
		  if (g instanceof SourceToken) {
		     PybaseUtil.outputSymbol(sd.getProject(),ifd,f,xw);
		   }
		}
	     }
	  }
	 catch (Exception e) { }
	 // find method here
	 xw.end("MATCH");
       }
    }
}


private class FindOuterVisitor extends Visitor {

   private IFileData for_file;
   private int start_offset;
   private int end_offset;
   private SimpleNode item_found;

   FindOuterVisitor(IFileData fd,int start,int end) {
      for_file = fd;
      start_offset = start;
      end_offset = end;
      item_found = null;
    }

   SimpleNode getItem() 			{ return item_found; }

   @Override public Object visitModule(Module n) throws Exception {
      Object r = unhandled_node(n);
      checkNode(n);
      return r;
    }

   @Override public Object visitFunctionDef(FunctionDef n) throws Exception {
      Object r = unhandled_node(n);
      checkNode(n);
      return r;
    }

   @Override public Object visitClassDef(ClassDef n) throws Exception {
      Object r = unhandled_node(n);
      checkNode(n);
      return r;
    }

   private void checkNode(SimpleNode n) {
      int soff = for_file.getStartOffset(n);
      int eoff = for_file.getEndOffset(n);
      if (soff < start_offset && eoff > end_offset) item_found = n;
    }

}	// end of inner class FindOuterVisitor



/********************************************************************************/
/*										*/
/*	Handle finding the right token						*/
/*										*/
/********************************************************************************/


private class FindTokenVisitor extends Visitor {

   private IFileData for_file;
   private int start_offset;
   private int end_offset;
   private SimpleNode found_token;

   FindTokenVisitor(IFileData ifd,int start,int end) {
      for_file = ifd;
      start_offset = start;
      end_offset = end;
      found_token = null;
    }

   SimpleNode getToken()		{ return found_token; }

   @Override public Object visitNameTok(NameTok node) {
      checkName(node);
      return node;
    }

   @Override public Object visitName(Name node) {
      checkName(node);
      return node;
    }

   private void checkName(SimpleNode n) {
      int soff = for_file.getStartOffset(n);
      int eoff = for_file.getEndOffset(n);
      if (soff < start_offset && eoff >= end_offset) {
	 found_token = n;
       }
    }

}	// end of inner class FindTokenVisitor




/********************************************************************************/
/*										*/
/*	Search pattern matching 						*/
/*										*/
/********************************************************************************/

private static class SearchPattern {

   private String pattern_string;
   private Pattern regex_pattern;
   private SearchFor search_for;
   private boolean do_defs;
   private boolean do_refs;

   SearchPattern(String pat,SearchFor sf,boolean defs,boolean refs) {
      pattern_string = pat;
      regex_pattern = null;
      int idx = pattern_string.indexOf("(");
      if (idx >= 0) pattern_string = pattern_string.substring(0,idx);
      if (pattern_string.contains("*")) {
         String rstr = PybaseUtil.convertWildcardToRegex(pattern_string);
         regex_pattern = Pattern.compile(rstr);
       }
      search_for = sf;
      do_defs = defs;
      do_refs = refs;
    }

   SearchFor getSearchFor()			{ return search_for; }
   boolean getDoDefs()				{ return do_defs; }
   boolean getDoRefs()				{ return do_refs; }

   boolean match(ISemanticData sd,Found fnd) {
      GenAndTok gt = fnd.getSingle();
      AbstractToken tok = gt.getToken();
      String hdl = tok.getRepresentation();
      String pkg = tok.getParentPackage();
      TokenType styp = tok.getType();
      if (styp == TokenType.UNKNOWN) return false;
      if (!search_types.get(search_for).contains(styp)) return false;

      String ctx = PybaseUtil.getContextName(tok);

      if (pkg != null) hdl = pkg + "." + ctx + hdl;
      else hdl = ctx + hdl;

      if (regex_pattern != null) {
	 Matcher m = regex_pattern.matcher(hdl);
	 if (m.find()) return true;
       }
      else {
	 if (hdl.equals(pattern_string)) return true;
       }

      return false;
    }

}	// end of inner class SearchPattern




}	// end of class PybaseSearch




/* end of PybaseSearch.java */

