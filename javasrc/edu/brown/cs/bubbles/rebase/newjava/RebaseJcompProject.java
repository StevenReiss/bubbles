/********************************************************************************/
/*                                                                              */
/*              RebaseJcompProject.java                                         */
/*                                                                              */
/*      Project-level semantic operations                                       */
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



package edu.brown.cs.bubbles.rebase.newjava;

import edu.brown.cs.bubbles.rebase.RebaseConstants;
import edu.brown.cs.bubbles.rebase.RebaseConstants.RebaseProjectSemantics;
import edu.brown.cs.bubbles.rebase.RebaseFile;
import edu.brown.cs.bubbles.rebase.RebaseMessage;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompMessage;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSearcher;
import edu.brown.cs.ivy.jcomp.JcompSemantics;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



class RebaseJcompProject implements RebaseProjectSemantics, RebaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcompProject    base_project;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RebaseJcompProject(JcompProject jp)
{
   base_project = jp;
}



/********************************************************************************/
/*                                                                              */
/*      Direct access methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public void resolve()
{
   base_project.resolve();
}


@Override public RebaseSearcher findSymbols(String pat,String kind) 
{
   return new SearcherMapper(base_project.findSymbols(pat,kind));
}


@Override public RebaseSearcher findSymbolAt(String file,int soff,int eoff)
{
   return new SearcherMapper(base_project.findSymbolAt(file,soff,eoff));
}


@Override public RebaseSearcher findSymbolByKey(String proj,String file,String key)
{
   return new SearcherMapper(base_project.findSymbolByKey(proj,file,key));
}


@Override public RebaseSearcher findTypes(RebaseSearcher rs)
{
   SearcherMapper sm = (SearcherMapper) rs;
   return new SearcherMapper(base_project.findTypes(sm.getSearchResult()));
}


@Override public List<RebaseMessage> getMessages()
{
   List<RebaseMessage> rslt = new ArrayList<RebaseMessage>();
   
   for (JcompMessage jm : base_project.getMessages()) {
      RebaseJcompSource src = (RebaseJcompSource) jm.getSource();
      MessageSeverity ms = MessageSeverity.valueOf(jm.getSeverity().toString());
      
      RebaseMessage rm = new RebaseMessage(src.getRebaseFile(),ms,jm.getMessageId(),
            jm.getText(),jm.getLineNumber(),jm.getStartOffset(),jm.getEndOffset());
      rslt.add(rm);
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void outputLocations(RebaseSearcher rs,boolean def,boolean ref,
      boolean impl,boolean ronly,boolean wonly,IvyXmlWriter xw)
{
   SearcherMapper sm = (SearcherMapper) rs;
   JcompSearcher js = base_project.findLocations(sm.getSearchResult(),
         def,ref,impl,ronly,wonly);
   
   List<JcompSearcher.SearchResult> rslt = js.getMatches();
   if (rslt == null) return;
   for (JcompSearcher.SearchResult mtch : rslt) {
      xw.begin("MATCH");
      xw.field("OFFSET",mtch.getOffset());
      xw.field("LENGTH",mtch.getLength());
      xw.field("STARTOFFSET",mtch.getOffset());
      xw.field("ENDOFFSET",mtch.getOffset() + mtch.getLength());
      xw.field("FILE",mtch.getFile().getFileName());
      JcompSymbol sym = mtch.getContainer();
      if (sym != null) {
         RebaseJcompSource rjs = (RebaseJcompSource) mtch.getFile();
	 outputNameData(rjs.getRebaseFile(),sym,xw);
       }
      xw.end("MATCH");
    }
}


@Override public void outputFullName(RebaseSearcher rs,IvyXmlWriter xw)
{
   SearcherMapper sm = (SearcherMapper) rs;
   JcompSearcher js = sm.getSearchResult();
   Set<JcompSymbol> syms = js.getSymbols();
   if (syms == null) return;
   for (JcompSymbol sym : syms) {
      outputFullSymbolName(sym,xw);
    }
   
}


@Override public void outputContainer(RebaseFile file,int soff,int eoff,IvyXmlWriter xw)
{
   base_project.resolve();
  
   
   JcompSymbol sym = null;
   for (JcompSemantics jsem : base_project.getSources()) {
      RebaseJcompSource src = (RebaseJcompSource) jsem.getFile();
      if (src.getRebaseFile() != file) continue;
      sym = base_project.getContainer(jsem,soff,eoff);
    }
   
   if (sym == null) return;
   
   outputNameData(file,sym,xw);
}




@Override public void outputAllNames(Set<String> files,IvyXmlWriter xw)
{
   base_project.resolve();
   Set<String> done = new HashSet<String>();
   
   for (JcompSemantics jsem : base_project.getSources()) {
      if (files != null &&
            !files.contains(jsem.getFile().getFileName())) continue;
      RebaseJcompSource src = (RebaseJcompSource) jsem.getFile();
      RebaseFile rf = src.getRebaseFile();
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
      xw.textElement("PATH",jsem.getFile().getFileName());
      
      OutputVisitor ov = new OutputVisitor(rf,xw);
      jsem.getAstNode().accept(ov);
      
      xw.end("FILE");  
    }
}




static void outputNameData(RebaseFile rf,JcompSymbol js,IvyXmlWriter xw)
{
   ASTNode an = js.getDefinitionNode();
   if (an == null) return;
   
   xw.begin("ITEM");
   xw.field("PROJECT",rf.getProjectName());
   xw.field("PATH",rf.getFileName());
   CompilationUnit cu = (CompilationUnit) an.getRoot();
   xw.field("QNAME",js.getFullReportName());
   int spos = cu.getExtendedStartPosition(an);
   int len = cu.getExtendedLength(an);
   xw.field("STARTOFFSET",spos);
   xw.field("LENGTH",len);
   xw.field("ENDOFFSET",spos + len);
   String hdl = js.getHandle(rf.getProjectName());
   if (hdl != null) xw.field("HANDLE",hdl);
   
   if (rf.getSource() != null) { 
      String s6 = rf.getSource().getS6Source();
      if (s6 != null) xw.field("S6",s6);
    }
   
   switch (js.getSymbolKind()) {
      case FIELD :
         if (js.isEnumSymbol()) {
            xw.field("NAME",js.getName());
            xw.field("TYPE","EnumConstants");
            xw.field("FLAGS",js.getModifiers());
          }
         break;
      case CONSTRUCTOR :
         xw.field("NAME",js.getReportName());
         xw.field("TYPE","Constructor");
         xw.field("FLAGS",js.getModifiers());
         break;
      case METHOD :
         xw.field("NAME",js.getReportName());
         xw.field("TYPE","Function");
         xw.field("FLAGS",js.getModifiers());
         break;
      case CLASS :
      case INTERFACE :
      case ENUM :
         String typ = "Class";
         if (js.getType() == null) typ = "Class";
         else if (js.getType().isInterfaceType()) typ = "Interface";
         else if (js.getType().isEnumType()) typ = "Enum";
         else if (js.getType().isThrowable()) typ = "Exception";
         xw.field("NAME",js.getFullName());
         xw.field("TYPE",typ);
         xw.field("FLAGS",js.getModifiers());
         break;
      case ANNOTATION :
      case ANNOTATION_MEMBER :
      case LOCAL :
      case NONE :
      case PACKAGE :
         break;
    }
   xw.end("ITEM"); 
}



void outputFullSymbolName(JcompSymbol js,IvyXmlWriter xw)
{
   xw.begin("FULLYQUALIFIEDNAME");
   xw.field("NAME",js.getFullReportName());
   JcompType jt = js.getType();
   if (jt != null) xw.field("TYPE",jt.getName());
   xw.end("FULLYQUALIFIEDNAME");
}



private class OutputVisitor extends ASTVisitor {
   
   private RebaseFile java_file;
   private IvyXmlWriter xml_writer;
   private Set<JcompSymbol> symbols_done;
   private int init_counter;
   
   OutputVisitor(RebaseFile jf,IvyXmlWriter xw) {
      java_file = jf;
      xml_writer = xw;
      symbols_done = new HashSet<JcompSymbol>();
      init_counter = 0;
    }
   
   @Override public void preVisit(ASTNode n) {
      JcompSymbol rs = JcompAst.getDefinition(n);
      if (rs != null && !symbols_done.contains(rs)) {
	 if (rs.getDefinitionNode() != null) {
	    symbols_done.add(rs);
            outputNameData(java_file,rs,xml_writer);
	  }
       }
    }
   
   @Override public void endVisit(Initializer it) {
      JcompSymbol js = null;
      
      if (it.getParent() != null) {
	 // find class for the initializer
	 js = JcompAst.getDefinition(it.getParent());
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
   
}       // end of inner class OutputVisitor




/********************************************************************************/
/*                                                                              */
/*      SearcherMapper -- handle mapping JcompSearcher to RebaseSearcher        */
/*                                                                              */
/********************************************************************************/

private static class SearcherMapper implements RebaseSearcher
{
   private static JcompSearcher search_result;
   
   SearcherMapper(JcompSearcher js) {
      search_result = js;
    }
   
   JcompSearcher getSearchResult()              { return search_result; }
   
   @Override public void outputSearchFor(IvyXmlWriter xw) {
      String what = null;
      String nm = null;
      for (JcompSymbol rjs : search_result.getSymbols()) {
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
   
}       // end of inner class SearchMapper




}       // end of class RebaseJcompProject




/* end of RebaseJcompProject.java */

