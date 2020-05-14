/********************************************************************************/
/*										*/
/*		RebaseJavaRoot.java						*/
/*										*/
/*	AST node for a set of project files					*/
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
import edu.brown.cs.bubbles.rebase.RebaseMain;
import edu.brown.cs.bubbles.rebase.RebaseMessage;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class RebaseJavaRoot implements RebaseConstants.RebaseProjectSemantics,
	RebaseJavaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<RebaseJavaFile>   file_nodes;
private RebaseJavaContext      base_context;
private boolean 	       is_resolved;
private Set<RebaseJavaType>    all_types;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaRoot(RebaseJavaContext ctx)
{
   file_nodes = new ArrayList<RebaseJavaFile>();
   base_context = ctx;
   is_resolved = false;
   all_types = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addFile(RebaseJavaFile jf)
{
   if (file_nodes.contains(jf)) return;

   file_nodes.add(jf);
   jf.setRoot(this);
}

Collection<RebaseJavaFile> getFiles()	       { return file_nodes; }
Collection<CompilationUnit> getTrees()
{
   List<CompilationUnit> rslt= new ArrayList<CompilationUnit>();
   for (RebaseJavaFile rf : file_nodes) {
      CompilationUnit cu = rf.getAstNode();
      if (cu != null) rslt.add(cu);
    }
   return rslt;
}


Set<RebaseJavaType> getAllTypes()
{
   return all_types;
}



void setResolved(boolean fg)
{
   is_resolved = fg;
   if (!fg) all_types = null;
}

boolean isResolved()				{ return is_resolved; }




/********************************************************************************/
/*										*/
/*	Compilation methods							*/
/*										*/
/********************************************************************************/

@Override synchronized public void resolve()
{
   if (isResolved()) return;

   RebaseMain.logD("START RESOLVE");
   for (RebaseJavaFile jf : file_nodes) {
      RebaseMain.logD("FILE: " + jf.getFile().getFileName());
    }

   clearResolve();

   RebaseJavaTyper jt = new RebaseJavaTyper(base_context);
   RebaseJavaResolver jr = new RebaseJavaResolver(jt);
   jt.assignTypes(this);
   jr.resolveNames(this);

   all_types = new HashSet<RebaseJavaType>(jt.getAllTypes());

   setResolved(true);
}



@Override public List<RebaseMessage> getMessages()
{
   List<RebaseMessage> rslt = new ArrayList<RebaseMessage>();
   for (RebaseJavaFile jf : file_nodes) {
      List<RebaseMessage> nmsg = jf.getMessages();
      if (nmsg != null) rslt.addAll(nmsg);
    }
   return rslt;
}



private void clearResolve()
{
   ClearVisitor cv = new ClearVisitor();

   for (RebaseJavaFile jf : file_nodes) {
      CompilationUnit cu = jf.getAstNode();
      if (cu != null) cu.accept(cv);
    }
}



private static class ClearVisitor extends ASTVisitor {

   @Override public void postVisit(ASTNode n) {
      RebaseJavaAst.clearAll(n);
   }

}	// end of inner class ClearVisitor




/********************************************************************************/
/*										*/
/*	Name Output methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAllNames(Set<String> files,IvyXmlWriter xw)
{
   resolve();

   Set<String> done = new HashSet<String>();


   for (RebaseJavaFile jf : file_nodes) {
      // output package element
      if (files != null && !files.contains(jf.getFile().getFileName())) continue;

      RebaseFile rf = jf.getFile();
      String pkg = rf.getPackageName();
      if (pkg != null && pkg.length() > 0 && !done.contains(pkg)) {
	 done.add(pkg);
	 xw.begin("ITEM");
	 xw.field("HANDLE",rf.getPackageName() + "/" + pkg);
	 xw.field("NAME",pkg);
	 File f1 = new File(rf.getFileName());
	 xw.field("PATH",f1.getParent());
	 xw.field("PROJECT",rf.getProjectName());
	 xw.field("SOURCE","USERSOURCE");
	 xw.field("TYPE","Package");
	 xw.end("ITEM");
       }

      xw.begin("FILE");
      xw.textElement("PATH",jf.getFile().getFileName());

      OutputVisitor ov = new OutputVisitor(jf,xw);
      jf.getAstNode().accept(ov);

      xw.end("FILE");
    }
}



/********************************************************************************/
/*										*/
/*	Visitation methods							*/
/*										*/
/********************************************************************************/

void accept(RebaseJavaVisitor v)
{
   v.preVisit(this);
   if (!v.preVisit2(this)) return;

   if (v.visit(this)) {
      for (CompilationUnit cu : getTrees()) {
	 cu.accept(v);
       }
    }

   v.postVisit(this);
}



/********************************************************************************/
/*										*/
/*	Symbol Location methods 						*/
/*										*/
/********************************************************************************/

@Override public RebaseSearcher findSymbols(String pattern,String kind)
{
   RebaseJavaSearch search = new RebaseJavaSearch(this);

   findSymbols(search,pattern,kind);

   return search;
}


private void findSymbols(RebaseJavaSearch search,String pattern,String kind)
{
   resolve();

   ASTVisitor av = search.getFindSymbolsVisitor(pattern,kind);

   for (RebaseJavaFile jf : file_nodes) {
      search.setFile(jf);
      jf.getAstNode().accept(av);
    }
}


@Override public RebaseSearcher findSymbolAt(String file,int soff,int eoff)
{
   resolve();

   RebaseJavaSearch search = new RebaseJavaSearch(this);
   ASTVisitor av = search.getFindLocationVisitor(soff,eoff);

   for (RebaseJavaFile jf : file_nodes) {
      if (!jf.getFile().getFileName().equals(file)) continue;
      search.setFile(jf);
      jf.getAstNode().accept(av);
      break;
    }

   return search;
}


@Override public RebaseSearcher findSymbolByKey(String proj,String file,String key)
{
   resolve();

   RebaseJavaSearch search = new RebaseJavaSearch(this);
   ASTVisitor av = search.getFindByKeyVisitor(key);

   for (RebaseJavaFile jf : file_nodes) {
      if (!jf.getFile().getFileName().equals(file)) continue;
      search.setFile(jf);
      jf.getAstNode().accept(av);
    }

   return search;
}


@Override public RebaseSearcher findTypes(RebaseSearcher rs)
{
   RebaseJavaSearch rjs = (RebaseJavaSearch) rs;
   Set<RebaseSymbol> syms = rjs.getSymbols();
   if (syms == null || syms.isEmpty()) return rs;
   List<RebaseJavaType> typs = new ArrayList<RebaseJavaType>();
   boolean isok = false;
   for (RebaseSymbol rsym : syms) {
      RebaseJavaSymbol sym = (RebaseJavaSymbol) rsym;
      if (!sym.isTypeSymbol()) {
	 isok = false;
	 RebaseJavaType jt = sym.getType();
	 if (!jt.isUndefined() && jt.isUnknown()) typs.add(jt);
       }
    }
   if (isok) return rs;

   RebaseJavaSearch nrjs = new RebaseJavaSearch(this);
   for (RebaseJavaType jt : typs) {
      findSymbols(nrjs,jt.getName(),"TYPE");
    }

   return nrjs;
}


@Override public void outputLocations(RebaseSearcher rs,boolean def,boolean ref,
      boolean impl,boolean ronly,boolean wonly,IvyXmlWriter xw)
{
   RebaseJavaSearch search = (RebaseJavaSearch) rs;

   ASTVisitor av = search.getLocationsVisitor(def,ref,impl,ronly,wonly);

   for (RebaseJavaFile jf : file_nodes) {
      search.setFile(jf);
      jf.getAstNode().accept(av);
    }

   List<SearchResult> rslt = search.getMatches();
   if (rslt == null) return;
   for (SearchResult mtch : rslt) {
      xw.begin("MATCH");
      xw.field("OFFSET",mtch.getOffset());
      xw.field("LENGTH",mtch.getLength());
      xw.field("STARTOFFSET",mtch.getOffset());
      xw.field("ENDOFFSET",mtch.getOffset() + mtch.getLength());
      xw.field("FILE",mtch.getFile().getFileName());
      RebaseJavaSymbol sym = mtch.getContainer();
      if (sym != null) {
	 sym.outputNameData(mtch.getFile(),xw);
       }
      xw.end("MATCH");
    }
}


@Override public void outputFullName(RebaseSearcher rs,IvyXmlWriter xw)
{
   RebaseJavaSearch search = (RebaseJavaSearch) rs;
   Set<RebaseSymbol> syms = search.getSymbols();
   if (syms == null) return;
   for (RebaseSymbol sym : syms) {
      RebaseJavaSymbol js = (RebaseJavaSymbol) sym;
      js.outputFullName(xw);
    }
}




/********************************************************************************/
/*										*/
/*	Symbol output visitor							*/
/*										*/
/********************************************************************************/

private void outputSymbol(RebaseFile rf,RebaseJavaSymbol rs,IvyXmlWriter xw)
{
   rs.outputNameData(rf,xw);
}



private class OutputVisitor extends RebaseJavaVisitor {

   private RebaseFile java_file;
   private IvyXmlWriter xml_writer;
   private Set<RebaseJavaSymbol> symbols_done;
   private int init_counter;

   OutputVisitor(RebaseJavaFile jf,IvyXmlWriter xw) {
      java_file = jf.getFile();
      xml_writer = xw;
      symbols_done = new HashSet<RebaseJavaSymbol>();
      init_counter = 0;
    }

   @Override public void preVisit(ASTNode n) {
      RebaseJavaSymbol rs = RebaseJavaAst.getDefinition(n);
      if (rs != null && !symbols_done.contains(rs)) {
	 if (rs.getDefinitionNode() != null) {
	    symbols_done.add(rs);
	    outputSymbol(java_file,rs,xml_writer);
	  }
       }
    }

   @Override public void endVisit(Initializer it) {
      RebaseJavaSymbol js = null;

      if (it.getParent() != null) {
	 // find class for the initializer
	 js = RebaseJavaAst.getDefinition(it.getParent());
       }

      if (js == null) return;
      xml_writer.begin("ITEM");
      xml_writer.field("PROJECT",java_file.getProjectName());
      xml_writer.field("PATH",java_file.getFileName());
      xml_writer.field("TYPE","StaticInitializer");
      xml_writer.field("NAME","<clinit>");
      xml_writer.field("STARTOFFSET",it.getStartPosition());
      xml_writer.field("ENDOFFFSET",it.getStartPosition() + it.getLength());
      xml_writer.field("LENGTH",it.getLength());
      xml_writer.field("QNAME",js.getFullName() + ".<clinit>");
      xml_writer.field("HANDLE",js.getFullName() + "@INIT@" + (++init_counter));
      xml_writer.field("FLAGS",8);
      xml_writer.end("ITEM");
    }

}




/********************************************************************************/
/*										*/
/*	Handle outputing container for locations				*/
/*										*/
/********************************************************************************/

@Override public void outputContainer(RebaseFile rf,int soff,int eoff,IvyXmlWriter xw)
{
   resolve();

   RebaseJavaSymbol sym = null;

   for (RebaseJavaFile jf : file_nodes) {
      if (jf.getFile() != rf) continue;
      sym = getContainer(jf.getAstNode(),soff,eoff);
      break;
    }

   if (sym == null) return;
   sym.outputNameData(rf,xw);
}



private RebaseJavaSymbol getContainer(CompilationUnit cu,int soff,int eoff)
{
   for (Object o : cu.types()) {
      AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
      int spos = cu.getExtendedStartPosition(atd);
      if (spos <= soff && spos + cu.getExtendedLength(atd) >= eoff) {
	 return getContainer(cu,atd,soff,eoff);
       }
    }

   return null;
}


private RebaseJavaSymbol getContainer(CompilationUnit cu,AbstractTypeDeclaration atd,
      int soff,int eoff)
{
   RebaseJavaSymbol sym = RebaseJavaAst.getDefinition(atd);

   for (Object o : atd.bodyDeclarations()) {
      BodyDeclaration bd = (BodyDeclaration) o;
      int spos = cu.getExtendedStartPosition(bd);
      if (spos <= soff && spos + cu.getExtendedLength(bd) >= eoff) {
	 switch (bd.getNodeType()) {
	    case ASTNode.TYPE_DECLARATION :
	    case ASTNode.ANNOTATION_TYPE_DECLARATION :
	    case ASTNode.ENUM_DECLARATION :
	       return getContainer(cu,(AbstractTypeDeclaration) bd,soff,eoff);
	    case ASTNode.ENUM_CONSTANT_DECLARATION :
	    case ASTNode.FIELD_DECLARATION :
	    case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
	    case ASTNode.INITIALIZER :
	    case ASTNode.METHOD_DECLARATION :
	       return sym;
	  }
       }
    }

   return sym;
}




}	// end of class RebaseJavaRoot




/* end of RebaseJavaRoot.java */

