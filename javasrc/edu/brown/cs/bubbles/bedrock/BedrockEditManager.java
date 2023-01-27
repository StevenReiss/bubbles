/********************************************************************************/
/*										*/
/*		BedrockEditManager.java 					*/
/*										*/
/*	Handle editor-related commands for Bubbles (updated version)		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bedrock;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.BufferChangedEvent;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractMethodDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveMethodDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


class BedrockEditManager implements BedrockConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;

private Map<String,FileData> file_map;
private ThreadPoolExecutor thread_pool;
private BlockingQueue<Runnable> edit_queue;
private Map<String,ParamSettings> param_map;
private CodeFormatter code_formatter;
private int active_edits;
private int java_version;

private static final int	QUEUE_SIZE = 1000;
private static final int	CORE_SIZE = 1;
private static final int	MAX_SIZE = 8;
private static final long	KEEP_ALIVE = 1000l;
private static final TimeUnit	KEEP_ALIVE_UNIT = TimeUnit.SECONDS;
private static final int	DEAD_COUNT = 10;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockEditManager(BedrockPlugin bp)
{
   our_plugin = bp;

   file_map = new HashMap<>();
   edit_queue = new ArrayBlockingQueue<>(QUEUE_SIZE,true);
   param_map = new HashMap<>();
   code_formatter = null;
   thread_pool = null;
   active_edits = 0;

   Runtime.Version rv = Runtime.version();
   int v = rv.feature();

   int max = 11;
// try {
//    max = AST.JLS_Latest;
//  }
// catch (Throwable t) { }
   if (v > max) v = max;
   java_version = v;
}





/********************************************************************************/
/*										*/
/*	Worker thread pool setup						*/
/*										*/
/********************************************************************************/

void start()
{
   thread_pool = new ThreadPoolExecutor(CORE_SIZE,MAX_SIZE,KEEP_ALIVE,KEEP_ALIVE_UNIT,
					   edit_queue);
}



/********************************************************************************/
/*										*/
/*	Parameter commands							*/
/*										*/
/********************************************************************************/

void handleParameter(String bid,String name,String value) throws BedrockException
{
   if (name == null) return;
   else if (name.equals("AUTOELIDE")) {
      setAutoElide(bid,(value != null && value.length() > 0 && "tTyY1".indexOf(value.charAt(0)) >= 0));
    }
   else if (name.equals("ELIDEDELAY")) {
      try {
	 setElideDelay(bid,Long.parseLong(value));
       }
      catch (NumberFormatException e) {
	 throw new BedrockException("Bad elide delay value: " + value);
       }
    }
   else {
      throw new BedrockException("Unknown editor parameter " + name);
    }
}



/********************************************************************************/
/*										*/
/*	Basic editing commands							*/
/*										*/
/********************************************************************************/

void handleStartFile(String proj,String bid,String file,String id,boolean cnts,IvyXmlWriter xw)
		throws BedrockException
{
   FileData fd = findFile(proj,file,bid);

   if (fd == null) {
      throw new BedrockException("Compilation unit for file " + file + " not available in " + proj);
    }

   fd.getEditableUnit(bid);
   if (id != null) fd.setCurrentId(bid,id);

   BedrockPlugin.logD("OPEN file " + file + " " + fd.hasChanged() + " " + bid + " " + id);

   String lsep = fd.getLineSeparator();
   if (lsep.equals("\n")) xw.field("LINESEP","LF");
   else if (lsep.equals("\r\n")) xw.field("LINESEP","CRLF");
   else if (lsep.equals("\r")) xw.field("LINESEP","CR");

   if (fd.getProject() != null) {
      xw.field("PROJECT",fd.getProject().getName());
    }

   if (cnts || fd.hasChanged()) {
      String s = fd.getCurrentContents();
      if (s == null) xw.emptyElement("EMPTY");
      else {
	 byte [] data = s.getBytes();
	 xw.bytesElement("CONTENTS",data);
       }
    }
   else xw.emptyElement("SUCCESS");
}



void handleFinishFile(String proj,String bid,String file)
{
   FileData fd = file_map.get(file);
   if (fd == null) return;
   fd.removeUser(bid);
}



void handleEdit(String proj,String sid,String file,String id,List<EditData> edits,
		   IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,sid);

   if (fd == null) throw new BedrockException("Compilation unit for file " + file + " not available");

   TextEdit xe = null;
   boolean havemult = false;
   for (EditData eds : edits) {
      TextEdit te = null;

      if (eds.getText() != null) {
	 // TODO: this only works for insertions, not for replace
	 String s = eds.getText();
	 if (!fd.getLineSeparator().equals("\n")) s = s.replace("\n",fd.getLineSeparator());
	 BedrockPlugin.logD("EDIT REPLACE " + sid + " " + eds.getOffset() + " " +
			       eds.getLength() + " " +
			       s.length() + " " + fd.getLineSeparator().length());
	 te = new ReplaceEdit(eds.getOffset(),eds.getLength(),s);
	 fd.noteEdit(sid,eds.getOffset(),eds.getLength(),s.length());
       }
      else {
	 int delta = 0;
	 delta = fd.getLineSeparator().length() - 1;
	 int off = eds.getOffset();
	 int len = eds.getLength();

	 if (delta > 0) {			// handle crlf pairs being deleted
	    String s = fd.getContents(sid);
	    for (int i = 0; i < len; ++i) {
	       char c = s.charAt(i+off);
	       if (c == '\r') ++len;
	     }
	  }

	 BedrockPlugin.logD("EDIT DELETE " + eds.getOffset() + " " + eds.getLength() + " " +
			       len + " " + fd.getLineSeparator().length());
	 te = new DeleteEdit(off,len);
	 fd.noteEdit(sid,off,len,0);
       }
      if (xe == null) xe = te;
      else if (!havemult) {
	 MultiTextEdit mte = new MultiTextEdit();
	 mte.addChild(xe);
	 mte.addChild(te);
	 xe = mte;
	 havemult = true;
       }
    }

   fd.applyEdit(sid,xe,id);

   EditTask et = new EditTask(fd,sid,id);
   try {
      synchronized (thread_pool) {
	 thread_pool.execute(et);
	 ++active_edits;
       }
    }
   catch (RejectedExecutionException ex) {
      BedrockPlugin.logE("Edit task rejected " + ex + " " +
			    edit_queue.size() + " " + thread_pool.getActiveCount());
    }

   xw.emptyElement("SUCCESS");
}



private void doneEdit()
{
   synchronized (thread_pool) {
      --active_edits;
      if (active_edits == 0) thread_pool.notifyAll();
    }
}


void waitForEdits()
{
   synchronized (thread_pool) {
      while (active_edits > 0) {
	 try {
	    thread_pool.wait();
	  }
	 catch (InterruptedException e) { }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle code completion					*/
/*										*/
/********************************************************************************/

void handleGetCompletions(String proj,String bid,String file,int offset,IvyXmlWriter xw)
		throws BedrockException
{
   FileData fd = findFile(proj,file,bid);

   if (fd == null) throw new BedrockException("Compilation unit for file " + file + " not available");

   ICompilationUnit icu = fd.getEditableUnit(bid);

   CompletionHandler ch = new CompletionHandler(xw,bid);

   try {
      icu.codeComplete(offset,ch);
    }
   catch (JavaModelException e) {
      throw new BedrockException("Problem getting completions: ",e);
    }
}




/********************************************************************************/
/*										*/
/*	Elision commands							*/
/*										*/
/********************************************************************************/

void elisionSetup(String proj,String bid,String file,boolean compute,
		     Collection<Element> rgns,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,bid);

   if (fd == null) {
      throw new BedrockException("Compilation unit for file " + file +
				    " not available for elision");
    }

   CompilationUnit cu = fd.getAstRoot(bid);

   if (cu == null) throw new BedrockException("Unable to get AST for file " + file);

   BedrockElider be = null;

   if (rgns != null) {
      be = fd.getElider(bid);
      be.clearElideData();
      for (Element r : rgns) {
	 double p = IvyXml.getAttrDouble(r,"PRIORITY",-1);
	 int soff = IvyXml.getAttrInt(r,"START");
	 int eoff = IvyXml.getAttrInt(r,"END");
	 if (soff < 0 || eoff < 0) throw new BedrockException("Missing start or end offset for elision region");
	 if (p >= 0) be.addElidePriority(soff,eoff,p);
	 else be.addElideRegion(soff,eoff);
       }
    }
   else if (compute) {
      be = fd.checkElider(bid);
    }
   else fd.clearElider(bid);		// no regions and no compute

   if (compute) {
      xw.begin("ELISION");
      if (be != null) be.computeElision(cu,xw);
      xw.end("ELISION");
    }
   else xw.emptyElement("SUCCESS");
}




/********************************************************************************/
/*										*/
/*	Remote file editing commands						*/
/*										*/
/********************************************************************************/

void fileElide(byte [] bytes,IvyXmlWriter xw)
{
   BedrockElider be = new BedrockElider();

   if (bytes == null) return;

   // would really like to resolvie bindings here using JCOMP

   ASTParser ap = ASTParser.newParser(java_version);
   String s1 = new String(bytes);
   char [] cdata = s1.toCharArray();
   be.addElideRegion(0,cdata.length);
   ap.setSource(cdata);
   CompilationUnit cu = (CompilationUnit) ap.createAST(null);

   be.computeElision(cu,xw);
}



/********************************************************************************/
/*										*/
/*	Commitment commands							*/
/*										*/
/********************************************************************************/

synchronized void handleCommit(String proj,String bid,boolean refresh,boolean save,
      boolean compile,Collection<Element> files,IvyXmlWriter xw)
{
   if (xw != null) xw.begin("COMMIT");

   if (files == null || files.size() == 0) {
      Collection<IFile> fset = null;
      try {
	 if (proj != null) {
	    fset = our_plugin.getProjectManager().getAllSourceFiles(proj);
	 }
      }
      catch (BedrockException e) { }

      if (fset != null && !fset.isEmpty()) {
	 for (IFile f : fset) {
	    IPath fp = f.getFullPath();
	    String fnm = fp.toOSString();
	    FileData fd = file_map.get(fnm);
	    if (fd != null) {
	       commitFile(fd,refresh,save,compile,xw);
	     }
	  }
       }
      else {
	 for (FileData fd : file_map.values()) {
	    if (refresh || !save || fd.hasChanged())
	       commitFile(fd,refresh,save,compile,xw);
	  }
       }
    }
   else {
      for (Element e : files) {
	 String fnm = IvyXml.getAttrString(e,"NAME");
	 if (fnm == null) fnm = IvyXml.getText(e);
	 FileData fd = file_map.get(fnm);
	 if (fd != null) {
	    boolean r = IvyXml.getAttrBool(e,"REFRESH",refresh);
	    boolean s = IvyXml.getAttrBool(e,"SAVE",save);
	    boolean c = IvyXml.getAttrBool(e,"COMPILE",compile);
	    commitFile(fd,r,s,c,xw);
	  }
       }
    }

   if (xw != null) xw.end("COMMIT");
}


synchronized void updateSingleFile(String fnm)
{
   FileData fd = file_map.get(fnm);
   if (fd != null) {
      if (!fd.hasChanged()) return;
      commitFile(fd,false,false,false,null);
    }
}



private void commitFile(FileData fd,boolean refresh,boolean save,boolean compile,IvyXmlWriter xw)
{
   if (xw != null) {
      xw.begin("FILE");
      xw.field("NAME",fd.getFileName());
    }
   try {
      fd.commit(refresh,save,compile);
      fd.getLength();			// force setupDefaultBuffer
    }
   catch (JavaModelException e) {
      if (xw != null) xw.field("ERROR",e.toString());
    }
   if (xw != null) xw.end("FILE");
}



synchronized void updateFiles(String proj)
{
   IProject ip = null;
   if (proj != null) {
      try {
	 ip = our_plugin.getProjectManager().findProjectForFile(proj,null);
       }
      catch (BedrockException e) { }
    }
   for (FileData fd : file_map.values()) {
      if (ip != null && fd.getProject() != ip) continue;
      fd.handleUpdate();
   }
}




/********************************************************************************/
/*										*/
/*	Renaming commands							*/
/*										*/
/********************************************************************************/

void rename(String proj,String bid,String file,int start,int end,String name,String handle,
	       String newname,
	       boolean keeporig, boolean getters,boolean setters, boolean dohier,
	       boolean qual,boolean refs,boolean dosimilar,boolean textocc,
	       boolean doedit,
	       String filespat,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = file_map.get(file);
   ICompilationUnit icu;

   if (doedit) {
      // icu = fd.getDefaultUnit();
      icu = fd.getEditableUnit(bid);
    }
   else icu = fd.getEditableUnit(bid);

   IJavaElement [] elts;
   try {
      elts = icu.codeSelect(start,end-start);
    }
   catch (JavaModelException e) {
      throw new BedrockException("Bad location: " + e,e);
    }

   IJavaElement relt = null;
   for (IJavaElement ije : elts) {
      if (handle != null && !handle.equals(ije.getHandleIdentifier())) continue;
      if (name != null && !name.equals(ije.getElementName())) continue;
      relt = ije;
      break;
    }
   if (relt == null) throw new BedrockException("Item to rename not found");

   String id = null;
   switch (relt.getElementType()) {
      case IJavaElement.COMPILATION_UNIT :
	 id = IJavaRefactorings.RENAME_COMPILATION_UNIT;
	 break;
      case IJavaElement.FIELD :
	 IField ifld = (IField) relt;
	 try {
	    if (ifld.isEnumConstant()) id = IJavaRefactorings.RENAME_ENUM_CONSTANT;
	    else id = IJavaRefactorings.RENAME_FIELD;
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 id = IJavaRefactorings.RENAME_PACKAGE;
	 break;
      case IJavaElement.LOCAL_VARIABLE :
	 id = IJavaRefactorings.RENAME_LOCAL_VARIABLE;
	 break;
      case IJavaElement.TYPE :
	 id = IJavaRefactorings.RENAME_TYPE;
	 break;
      case IJavaElement.TYPE_PARAMETER :
	 id = IJavaRefactorings.RENAME_TYPE_PARAMETER;
	 break;
      case IJavaElement.METHOD :
	 id = IJavaRefactorings.RENAME_METHOD;
	 break;
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_DECLARATION :
	 break;
    }
   if (id == null) throw new BedrockException("Invalid element type to rename");

   RenameJavaElementDescriptor renamer;

   RefactoringContribution rfc = RefactoringCore.getRefactoringContribution(id);
   if (rfc == null) {
      xw.begin("FAILURE");
      xw.field("TYPE","SETUP");
      xw.textElement("ID",id);
      xw.end("FAILURE");
      renamer = new RenameJavaElementDescriptor(id);
    }
   else {
      renamer = (RenameJavaElementDescriptor) rfc.createDescriptor();
    }

   renamer.setJavaElement(relt);
   renamer.setKeepOriginal(keeporig);
   renamer.setNewName(newname);
   if (proj != null) renamer.setProject(proj);
   renamer.setRenameGetters(getters);
   renamer.setRenameSetters(setters);
   renamer.setUpdateHierarchy(dohier);
   renamer.setUpdateQualifiedNames(qual);
   renamer.setUpdateReferences(refs);
   renamer.setUpdateSimilarDeclarations(dosimilar);
   renamer.setUpdateTextualOccurrences(textocc);
   if (filespat != null) renamer.setFileNamePatterns(filespat);

   RefactoringStatus sts = renamer.validateDescriptor();
   if (!sts.isOK()) {
      xw.begin("FAILURE");
      xw.field("TYPE","VALIDATE");
      BedrockUtil.outputStatus(sts,xw);
      xw.end("FAILURE");
      return;
    }

   try {
      Refactoring refactor = renamer.createRefactoring(sts);
      if (refactor == null) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CREATE");
	 xw.textElement("RENAMER",renamer.toString());
	 xw.textElement("REFACTOR",renamer.toString());
	 xw.textElement("STATUS",sts.toString());
	 xw.end("FAILURE");
	 return;
       }

      refactor.setValidationContext(null);

      // this seems to reset files from disk (mutliple times)
      sts = refactor.checkAllConditions(new NullProgressMonitor());
      if (!sts.isOK()) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CHECK");
	 BedrockUtil.outputStatus(sts,xw);
	 xw.end("FAILURE");
	 if (sts.hasFatalError()) return;
       }
      BedrockPlugin.logD("RENAME: Refactoring checked");

      Change chng = refactor.createChange(new NullProgressMonitor());
      BedrockPlugin.logD("RENAME: Refactoring change created");

      if (doedit && chng != null) {
	 chng.perform(new NullProgressMonitor());
       }
      else if (chng != null) {
	 xw.begin("EDITS");
	 BedrockUtil.outputChange(chng,xw);
	 xw.end("EDITS");
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem creating refactoring: " + e,e);
    }

   BedrockPlugin.logD("RENAME RESULT = " + xw.toString());
}



void renameResource(String proj,String bid,String file,String newname,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = file_map.get(file);
   ICompilationUnit icu;

   icu = fd.getEditableUnit(bid);
   icu = icu.getPrimary();

   try {
      icu.rename(newname,true,new NullProgressMonitor());
      fd.noteDeleted();
      file_map.remove(fd.getFileName());
    }
   catch (JavaModelException e) {
      throw new BedrockException("Problem renaming compilation unit: " + e,e);
    }
}




/********************************************************************************/
/*										*/
/*	Move refactoring commands						*/
/*										*/
/********************************************************************************/

void moveElement(String proj,String bid,String what,
      String file,int start,int end,String name,String handle,
      String target,boolean qual,boolean refs,boolean doedit,
      IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,null);
   IJavaElement relt = null;
   if (what.equals("COMPUNIT")) {
      if (fd == null) throw new BedrockException("Invalid file");
      relt = fd.getSearchUnit();
    }
   else {
      ICompilationUnit icu = fd.getEditableUnit(bid);
      IJavaElement [] elts;
      try {
	 elts = icu.codeSelect(start,end-start);
       }
      catch (JavaModelException e) {
	 throw new BedrockException("Bad location: " + e,e);
       }

      for (IJavaElement ije : elts) {
	 if (handle != null && !handle.equals(ije.getHandleIdentifier())) continue;
	 if (name != null && !name.equals(ije.getElementName())) continue;
	 relt = ije;
	 break;
       }
    }
   if (relt == null) throw new BedrockException("Item to rename not found");

   RefactoringContribution rfc = null;
   RefactoringDescriptor rfd = null;
   IJavaElement tgt = null;

   switch (relt.getElementType()) {
      case IJavaElement.COMPILATION_UNIT :
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
      case IJavaElement.PACKAGE_FRAGMENT :
      case IJavaElement.TYPE :
	 rfc = RefactoringCore.getRefactoringContribution(IJavaRefactorings.MOVE);
	 tgt = our_plugin.getProjectManager().findPackageFragment(proj,target);
	 break;
      case IJavaElement.METHOD :
	 rfc = RefactoringCore.getRefactoringContribution(IJavaRefactorings.MOVE_METHOD);
	 break;
      default :
	 throw new BedrockException("Invalid element type to rename");
    }

   if (rfc == null) {
      xw.begin("FAILURE");
      xw.field("TYPE","SETUP");
      xw.end("FAILURE");
      return;
    }
   else {
      rfd = rfc.createDescriptor();
    }

   RefactoringStatus sts = null;
   if (rfd instanceof MoveDescriptor) {
      MoveDescriptor md = (MoveDescriptor) rfd;
      md.setDestination(tgt);
      IFile [] ifls = new IFile [0];
      IFolder [] iflds = new IFolder [0];
      ICompilationUnit [] icus = new ICompilationUnit[] { (ICompilationUnit) relt };
      md.setMoveResources(ifls,iflds,icus);
      sts = md.validateDescriptor();
      if (!sts.isOK()) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","VALIDATE");
	 BedrockUtil.outputStatus(sts,xw);
	 xw.end("FAILURE");
	 return;
       }
      }
   else if (rfd instanceof MoveMethodDescriptor) {
      MoveMethodDescriptor mmd = (MoveMethodDescriptor) rfd;
      System.err.println("HANDLE MOVE METHOD" + mmd);
    }

   try {
      Refactoring refactor = rfd.createRefactoring(sts);
      if (refactor == null) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CREATE");
	 if (sts != null) xw.textElement("STATUS",sts.toString());
	 xw.end("FAILURE");
	 return;
      }

      refactor.setValidationContext(null);

      sts = refactor.checkAllConditions(new NullProgressMonitor());
      if (!sts.isOK()) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CHECK");
	 BedrockUtil.outputStatus(sts,xw);
	 xw.end("FAILURE");
	 if (sts.hasFatalError()) return;
       }

      Change chng = refactor.createChange(new NullProgressMonitor());
      BedrockPlugin.logD("RENAME: Refactoring change created");

      if (doedit && chng != null) {
	 chng.perform(new NullProgressMonitor());
      }
      else if (chng != null) {
	 xw.begin("EDITS");
	 BedrockUtil.outputChange(chng,xw);
	 xw.end("EDITS");
      }
   }
   catch (CoreException e) {
      throw new BedrockException("Problem with move",e);
   }
}




/********************************************************************************/
/*										*/
/*	Method extraction commands						*/
/*										*/
/********************************************************************************/

void extractMethod(String proj,String bid,String file,int start,int end,String newname,
		      boolean replacedups,boolean cmmts,boolean exceptions,
		      IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = file_map.get(file);
   ICompilationUnit icu = fd.getEditableUnit(bid);

   String id = IJavaRefactorings.EXTRACT_METHOD;
   Map<String,String> rargs = new HashMap<String,String>();
   String sel = Integer.toString(start) + " " + Integer.toString(end-start+1);
   String hdl = icu.getHandleIdentifier();
   rargs.put("selection",sel);
   rargs.put("input",hdl);
   rargs.put("name",newname);
   rargs.put("replace",Boolean.toString(replacedups));
   rargs.put("comments",Boolean.toString(cmmts));
   rargs.put("exceptions",Boolean.toString(exceptions));

   RefactoringContribution rc1 = RefactoringCore.getRefactoringContribution(id);
   RefactoringDescriptor rd1 = rc1.createDescriptor(id,proj,"Bedrock extract method",
						       null,rargs,RefactoringDescriptor.NONE);
   ExtractMethodDescriptor emd = (ExtractMethodDescriptor) rd1;

   RefactoringStatus sts = emd.validateDescriptor();
   if (!sts.isOK()) {
      xw.begin("FAILURE");
      xw.field("TYPE","VALIDATE");
      BedrockUtil.outputStatus(sts,xw);
      xw.end("FAILURE");
      return;
    }
   try {
      Refactoring refactor = emd.createRefactoring(sts);

      sts = refactor.checkAllConditions(new NullProgressMonitor());
      if (!sts.isOK()) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CHECK");
	 BedrockUtil.outputStatus(sts,xw);
	 xw.end("FAILURE");
	 if (sts.hasFatalError()) return;
       }

      Change chng = refactor.createChange(new NullProgressMonitor());

      xw.begin("EDITS");
      BedrockUtil.outputChange(chng,xw);
      xw.end("EDITS");
    }
   catch (CoreException e) {
      throw new BedrockException("Problem creating refactoring: " + e,e);
    }
}



/********************************************************************************/
/*										*/
/*	Method extraction commands						*/
/*										*/
/********************************************************************************/

void executeRefactoring(String proj,String bid,String file,String id,Element argset,
		      IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = file_map.get(file);
   ICompilationUnit icu = fd.getEditableUnit(bid);

   String xid = id;
   if (!id.contains(".")) xid = "org.eclipse.jdt.ui." + id.replace("_",".").toLowerCase();

   Map<String,String> rargs = getArguments(argset);
   String hdl = icu.getHandleIdentifier();
   rargs.put("input",hdl);

   RefactoringContribution rc1 = RefactoringCore.getRefactoringContribution(xid);
   RefactoringDescriptor rd1 = rc1.createDescriptor(xid,proj,"Bedrock refactoring: " + id,
						       null,rargs,RefactoringDescriptor.NONE);
   JavaRefactoringDescriptor jrd = (JavaRefactoringDescriptor) rd1;
   RefactoringStatus sts = jrd.validateDescriptor();
   if (!sts.isOK()) {
      xw.begin("FAILURE");
      xw.field("TYPE","VALIDATE");
      BedrockUtil.outputStatus(sts,xw);
      xw.end("FAILURE");
      return;
    }
   try {
      Refactoring refactor = jrd.createRefactoring(sts);

      sts = refactor.checkAllConditions(new NullProgressMonitor());
      if (!sts.isOK()) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CHECK");
	 BedrockUtil.outputStatus(sts,xw);
	 xw.end("FAILURE");
	 if (sts.hasFatalError()) return;
       }

      Change chng = refactor.createChange(new NullProgressMonitor());

      xw.begin("EDITS");
      BedrockUtil.outputChange(chng,xw);
      xw.end("EDITS");
    }
   catch (CoreException e) {
      throw new BedrockException("Problem creating refactoring: " + e,e);
    }
}


private Map<String,String> getArguments(Element xml)
{
   Map<String,String> rslt = new HashMap<>();

   for (Element arg : IvyXml.children(xml,"ARG")) {
      String key = IvyXml.getAttrString(arg,"KEY");
      if (key == null) key = IvyXml.getAttrString(arg,"NAME");
      if (key == null) continue;

      String typ = IvyXml.getAttrString(arg,"TYPE","STRING");
      typ = typ.toUpperCase();

      String val = IvyXml.getTextElement(arg,"VALUE");
      if (val == null) val = IvyXml.getText(arg);

      if (typ.equals("STRING")) ;
      // else handle conversion of other types

      if (val == null) continue;
      rslt.put(key,val);
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Formatting commands							*/
/*										*/
/********************************************************************************/

void formatCode(String proj,String bid,String file,int spos,int epos,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,bid);

   if (fd == null) throw new BedrockException("Compilation unit for file " + file +
						 " not available for formatting");

   ICompilationUnit icu = fd.getEditableUnit(bid);
   String cnts = null;
   try {
      cnts = icu.getBuffer().getContents();
    }
   catch (JavaModelException e) {
      throw new BedrockException("Unable to get compilation unit contents: " + e,e);
    }

   IRegion [] irgns = new IRegion[1];
   if (spos < 0) spos = 0;
   if (epos <= 0) epos = cnts.length();
   if (epos <= spos) throw new BedrockException("Bad region to format");
   irgns[0] = new Region(spos,epos-spos);

   if (code_formatter == null) {
      code_formatter = ToolFactory.createCodeFormatter(null);
    }

   // TODO: why doesn't K_CLASS_BODY_DECLARATIONS work here?
   // TextEdit te = code_formatter.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS,cnts,irgns,0,null);
   TextEdit te = code_formatter.format(CodeFormatter.K_UNKNOWN,cnts,irgns,0,null);

   if (te == null) throw new BedrockException("Unable to format method");

   BedrockUtil.outputTextEdit(te,xw);
}



/********************************************************************************/
/*										*/
/*	Handle FIX IMPORTS command						*/
/*										*/
/********************************************************************************/

public void handleFixImports(String proj,String bid,String file,int demand,int staticdemand,
      String order,String add,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,bid);

   if (fd == null) throw new BedrockException("Compilation unit for file " + file +
	 " not available for import fixup");

   CompilationUnit cu = fd.getDefaultRoot(bid);
   BedrockFixImports bfi = new BedrockFixImports();
   Set<ITypeBinding> imports = bfi.findImports(cu);

   try {
      IJavaProject ijp = null;
      if (proj != null) {
	 IProject ip = our_plugin.getProjectManager().findProject(proj);
	 ijp = JavaCore.create(ip);
       }
      if (order == null) {
	 order = PreferenceConstants.getPreference("org.eclipse.jdt.ui.importorder",ijp);
       }
      if (demand <= 0) {
	 String s = PreferenceConstants.getPreference("org.eclipse.jdt.ui.ondemandthreshold",ijp);
	 if (s != null) {
	    try {
	       demand = Integer.parseInt(s);
	     }
	    catch (NumberFormatException e) { }
	  }
	 if (demand <= 0) demand = 99;
       }
      if (staticdemand <= 0) {
	 String s = PreferenceConstants.getPreference("org.eclipse.jdt.ui.ondemandthreshold",ijp);
	 if (s != null) {
	    try {
	       staticdemand = Integer.parseInt(s);
	     }
	    catch (NumberFormatException e) { }
	  }
	 if (staticdemand <= 0) staticdemand = demand;
       }
    }
   catch (Throwable t) {
      BedrockPlugin.logD("Problem with fix constant: " + t);
    }
   BedrockPlugin.logD("IMPORT prefs done: " + demand + " " + staticdemand + " " + order);

   boolean keepfg = (add == null ? false : true);
   try {
      ImportRewrite imp = null;
      imp = ImportRewrite.create(cu,keepfg);

      if (demand >= 0) imp.setOnDemandImportThreshold(demand);
      if (staticdemand >= 0) imp.setStaticOnDemandImportThreshold(demand);
      if (order != null) {
	 String [] ord = order.split("[;,]");
	 imp.setImportOrder(ord);
       }
      if (add != null) imp.addImport(add);
      else {
	 for (ITypeBinding tb : imports) {
	    BedrockPlugin.logD("Add type to importrewrite: " + tb.getQualifiedName());
	    imp.addImport(tb);
	  }
       }
      TextEdit te = imp.rewriteImports(null);
      if (te != null) {
	 BedrockUtil.outputTextEdit(te,xw);
       }
    }
   catch (Exception e) {
      throw new BedrockException("Problem doing imports",e);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle GETEXPECTEDTYPE command                                          */
/*                                                                              */
/********************************************************************************/

void handleGetExpectedType(String proj,String bid,String file,int line,IvyXmlWriter xw)
   throws BedrockException
{
   FileData fd = findFile(proj,file,bid);
   
   if (fd == null) throw new BedrockException("Compilation unit for file " + file + " not available");
   
   CompilationUnit cu = fd.getAstRoot(bid);
   ASTNode n = JcompAst.findNodeAtLine(cu,line);
   if (n == null) throw new BedrockException("Nothing to find expected type for");
   Expression rhs = JcompAst.findTypeReferenceNode(n);
   if (rhs == null) throw new BedrockException("Nothing to infer type from");
   ITypeBinding itb = rhs.resolveTypeBinding();
   if (itb == null) throw new BedrockException("No inferred type");
   ASTNode par = rhs.getParent();
   xw.begin("TYPE");
   xw.field("OFFSET",par.getStartPosition());
   xw.field("NAME",itb.getName());
   xw.field("QNAME",itb.getQualifiedName());
   xw.field("BINARY",itb.getBinaryName());
   xw.field("NULL",itb.isNullType());
   xw.end("TYPE");
}








/********************************************************************************/
/*										*/
/*	Text Region extraction commands 					*/
/*										*/
/********************************************************************************/

void getTextRegions(String proj,String bid,String file,String cls,boolean pfx,
		       boolean statics,
		       boolean compunit,
		       boolean imports,
		       boolean pkgfg,
		       boolean topdecls,
		       boolean fields,
		       boolean all,
		       IvyXmlWriter xw)
	throws BedrockException
{
   if (file == null) {
      file = getFileFromClass(proj,cls);
    }

   FileData fd = findFile(proj,file,bid);
   if (fd == null) throw new BedrockException("Can't find file " + file + " in " + proj);

   CompilationUnit cu = fd.getDefaultRoot(bid);
   if (cu == null) throw new BedrockException("Can't get compilation unit for " + file);

   List<?> typs = cu.types();
   AbstractTypeDeclaration atd = findTypeDecl(cls,typs);
   int start = 0;
   if (atd != null && atd != typs.get(0)) start = cu.getExtendedStartPosition(atd);

   if (compunit) {
      xw.begin("RANGE");
      xw.field("PATH",file);
      xw.field("START",0);
      int ln = fd.getLength();
      if (ln < 0) {
	 File f = new File(file);
	 ln = (int) f.length();
       }
      xw.field("END",ln);
      xw.end("RANGE");
    }

   if (pfx && atd != null) {
      int xpos = cu.getExtendedStartPosition(atd);
      int xlen = cu.getExtendedLength(atd);
      int spos = atd.getStartPosition();
      int len = atd.getLength();
      int epos = -1;
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 int apos = cu.getExtendedStartPosition(an);
	 if (epos < 0 || epos >= apos) epos = apos-1;
       }
      if (epos < 0) {		     // no body declarations
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",start);
	 xw.field("END",xpos+xlen);
	 xw.end("RANGE");
       }
      else {
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",start);
	 xw.field("END",epos);
	 xw.end("RANGE");
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",spos+len-1);
	 xw.field("END",xpos+xlen);
	 xw.end("RANGE");
       }
    }

   if (pkgfg) {
      PackageDeclaration pkg = cu.getPackage();
      if (pkg != null) {
	 outputRange(cu,pkg,file,xw);
       }
    }

   if (imports) {
      for (Iterator<?> it = cu.imports().iterator(); it.hasNext(); ) {
	 ImportDeclaration id = (ImportDeclaration) it.next();
	 outputRange(cu,id,file,xw);
       }
    }

   if (topdecls && atd != null && !all) {
      int spos = atd.getStartPosition();
      int len = atd.getLength();
      int epos = -1;
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 int apos = cu.getExtendedStartPosition(an);
	 if (epos < 0 || epos >= apos) epos = apos-1;
       }
      if (epos < 0) {		     // no body declarations
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",spos);
	 xw.field("END",spos+len);
	 xw.end("RANGE");
       }
      else {
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",spos);
	 xw.field("END",epos);
	 xw.end("RANGE");
       }
    }

   if ((statics || all) && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 if (an.getNodeType() == ASTNode.INITIALIZER) {
	    outputRange(cu,an,file,xw);
	  }
       }
    }

   if (fields && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 switch (an.getNodeType()) {
	    case ASTNode.FIELD_DECLARATION :
	       outputRange(cu,an,file,xw);
	       break;
	    case ASTNode.ENUM_CONSTANT_DECLARATION :
	       outputRange(cu,an,file,xw);
	       break;
	  }
       }
      if (atd instanceof EnumDeclaration) {
	 for (Object o : ((EnumDeclaration) atd).enumConstants()) {
	    ASTNode an = (ASTNode) o;
	    switch (an.getNodeType()) {
	       case ASTNode.FIELD_DECLARATION :
		  outputRange(cu,an,file,xw);
		  break;
	       case ASTNode.ENUM_CONSTANT_DECLARATION :
		  outputRange(cu,an,file,xw);
		  break;
	    }
	 }
      }
    }

   if (all && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 IJavaElement elt = null;
	 switch (an.getNodeType()) {
	    case ASTNode.ANNOTATION_TYPE_DECLARATION :
	    case ASTNode.ENUM_DECLARATION :
	    case ASTNode.TYPE_DECLARATION :
	       AbstractTypeDeclaration atdecl = (AbstractTypeDeclaration) an;
	       ITypeBinding atbnd = atdecl.resolveBinding();
	       if (atbnd != null) elt = atbnd.getJavaElement();
	       break;
	    case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
	       break;
	    case ASTNode.ENUM_CONSTANT_DECLARATION :
	       EnumConstantDeclaration ecdecl = (EnumConstantDeclaration) an;
	       IVariableBinding ecbnd = ecdecl.resolveVariable();
	       if (ecbnd != null) elt = ecbnd.getJavaElement();
	       break;
	    case ASTNode.FIELD_DECLARATION :
	       FieldDeclaration fdecl = (FieldDeclaration) an;
	       for (Iterator<?> it = fdecl.fragments().iterator(); it.hasNext(); ) {
		  VariableDeclarationFragment vdf = (VariableDeclarationFragment) it.next();
		  IVariableBinding vbnd = vdf.resolveBinding();
		  if (vbnd != null) {
		     IJavaElement velt = vbnd.getJavaElement();
		     if (velt != null) BedrockUtil.outputJavaElement(velt,xw);
		   }
		}
	       break;
	    case ASTNode.INITIALIZER :
	       break;
	    case ASTNode.METHOD_DECLARATION :
	       MethodDeclaration mdecl = (MethodDeclaration) an;
	       IMethodBinding mbnd = mdecl.resolveBinding();
	       if (mbnd != null) elt = mbnd.getJavaElement();
	       break;
	    default :
	       break;
	 }
	 if (elt != null) BedrockUtil.outputJavaElement(elt,false,xw);
      }
    }

   if (all && topdecls) {
      for (Object o : cu.types()) {
	 AbstractTypeDeclaration td = (AbstractTypeDeclaration) o;
	 IJavaElement elt = null;
	 ITypeBinding atbnd = td.resolveBinding();
	 if (atbnd != null) elt = atbnd.getJavaElement();
	 if (elt != null) BedrockUtil.outputJavaElement(elt,false,xw);
       }
    }
}



private void outputRange(CompilationUnit cu,ASTNode an,String file,IvyXmlWriter xw)
{
   int xpos = cu.getExtendedStartPosition(an);
   int xlen = cu.getExtendedLength(an);
   xw.begin("RANGE");
   xw.field("PATH",file);
   xw.field("START",xpos);
   xw.field("END",xpos+xlen);
   xw.end("RANGE");
}




private AbstractTypeDeclaration findTypeDecl(String cls,List<?> typs)
{
   AbstractTypeDeclaration atd = null;
   for (int i = 0; atd == null && i < typs.size(); ++i) {
      if (!(typs.get(i) instanceof AbstractTypeDeclaration)) continue;
      AbstractTypeDeclaration d = (AbstractTypeDeclaration) typs.get(i);
      if (cls != null) {
	 ITypeBinding tb = d.resolveBinding();
	 if (tb != null && !cls.equals(tb.getQualifiedName())) {
	    if (cls.startsWith(tb.getQualifiedName() + ".")) {
	       atd = findTypeDecl(cls,d.bodyDeclarations());
	     }
	    continue;
	  }
       }
      atd = d;
    }

   return atd;
}



private String baseClassName(String s)
{
   if (s == null) return null;

   int idx = s.indexOf("<");
   if (idx < 0) return s;

   StringBuffer buf = new StringBuffer();
   int depth = 0;
   for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      if (c == '<') ++depth;
      else if (c == '>') --depth;
      else if (depth == 0) buf.append(c);
    }

   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Private buffer commands 						*/
/*										*/
/********************************************************************************/

String createPrivateBuffer(String proj,String bid,String pid,String file,String frompid,
      IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,bid);
   if (fd == null) throw new BedrockException("File not found");

   PrivateBufferData pbd = null;

   if (pid == null) {
      for (int i = 0; i < 100; ++i) {
	 int v = (int)(Math.random() * 10000000);
	 pid = "PID_" + v;
	 pbd = fd.createPrivateBuffer(pid,frompid);
	 if (pbd != null) break;
       }
    }
   else {
      pbd = fd.createPrivateBuffer(pid,frompid);
      if (pbd == null)
	 throw new BedrockException("Buffer id " + pid + " already used");
    }

   // do this now in case it fails
   ICompilationUnit icu = fd.getEditableUnit(pid);
   try {
      if (icu != null && icu.getSource() == null) icu = null;
    }
   catch (Throwable t) { icu = null; }
   if (icu == null) {
      fd.removePrivateBuffer(pid);
      throw new BedrockException("Problem creating private buffer");
    }

   OpenTask et = new OpenTask(fd,pid);
   try {
      synchronized (thread_pool) {
	 thread_pool.execute(et);
       }
    }
   catch (RejectedExecutionException ex) {
      BedrockPlugin.logE("Open task rejected " + ex + " " +
			    edit_queue.size() + " " + thread_pool.getActiveCount());
    }

   xw.text(pid);

   return pid;
}




void removePrivateBuffer(String proj,String bid,String file) throws BedrockException
{
   FileData fd = findFile(proj,file,null);
   if (fd != null) fd.removePrivateBuffer(bid);
}




/********************************************************************************/
/*										*/
/*	Delete commands 							*/
/*										*/
/********************************************************************************/

void handleDelete(String proj,String what,String path)
	throws BedrockException
{
   IResource rs = null;
   FileData fd = null;
   IProject ip = our_plugin.getProjectManager().findProject(proj);
   IJavaProject ijp = JavaCore.create(ip);

   if (what.equals("PROJECT")) {
      if (ip == null) throw new BedrockException("Can't find project to delete");
      rs = ip;
    }
   else if (what.equals("FILE")) {
      fd = file_map.get(path);
      if (fd != null) {
	 rs = fd.getSearchUnit().getResource();
       }
      if (rs == null) throw new BedrockException("Can't find file to delete");
    }
   else if (what.equals("CLASS")) {
      IType ityp = null;
      String bcls = baseClassName(path);
      String file = getFileFromClass(proj,bcls);
      fd = file_map.get(file);
      try {
	 if (ijp != null) ityp = ijp.findType(bcls);
       }
      catch (JavaModelException e) { }
      if (ityp == null) throw new BedrockException("Can't find class to delete");
      rs = ityp.getResource();
    }
   else if (what.equals("PACKAGE")) {
      IPackageFragmentRoot ipfr = null;
      try {
	 for (IPackageFragmentRoot pfr : ijp.getAllPackageFragmentRoots()) {
	    try {
	       if (!pfr.isExternal() && !pfr.isArchive() &&
		     pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
		  ipfr = pfr;
		  break;
		}
	     }
	    catch (JavaModelException e) { }
	  }
       }
      catch (JavaModelException e) {
	 throw new BedrockException("Problem finding package root: " + e,e);
       }
      if (ipfr == null) throw new BedrockException("Can't find source fragment root");
      IPackageFragment ifr = ipfr.getPackageFragment(path);
      if (ifr == null) throw new BedrockException("Can't find package to delete");
      rs = ifr.getResource();
    }

   if (rs != null) {
      BedrockPlugin.logD("Delete resource " + rs);
      try {
	 BedrockProgressMonitor pm = new BedrockProgressMonitor(our_plugin,"Deleting " + path);
	 rs.delete(IResource.FORCE|IResource.KEEP_HISTORY|IResource.ALWAYS_DELETE_PROJECT_CONTENT,pm);
	 pm.finish();
       }
      catch (CoreException e) {
	 throw new BedrockException("Problem with delete",e);
       }
    }

   if (fd != null) {
      String file = fd.getFileName();
      fd.noteDeleted();
      file_map.remove(file);
      IvyXmlWriter xw = our_plugin.beginMessage("RESOURCE");
      xw.begin("DELTA");
      xw.field("KIND","REMOVED");
      xw.field("PATH",file);
      xw.begin("RESOURCE");
      xw.field("LOCATION",file);
      xw.field("TYPE","FILE");
      xw.field("PROJECT",proj);
      xw.end("RESOURCE");
      xw.end("DELTA");
      our_plugin.finishMessage(xw);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to update a key-based item					*/
/*										*/
/********************************************************************************/

void findByKey(String proj,String bid,String key,String file,IvyXmlWriter xw)
		throws BedrockException
{
   FileData fd = findFile(proj,file,bid);
   if (fd == null) return;

   ICompilationUnit icu = fd.getEditableUnit(bid);

   IJavaElement elt1 = null;

   // elt1 = JavaCore.create(key,icu.getOwner());

   elt1 = findElementForKey(icu,key);

   if (elt1 != null) BedrockUtil.outputJavaElement(elt1,false,xw);
}


private IJavaElement findElementForKey(IJavaElement elt,String key)
{
   if (key.equals(elt.getHandleIdentifier())) return elt;

   if (elt instanceof IParent) {
      IParent ip = (IParent) elt;
      try {
	 if (ip.hasChildren()) {
	    for (IJavaElement je : ip.getChildren()) {
	       IJavaElement re = findElementForKey(je,key);
	       if (re != null) return re;
	     }
	  }
       }
      catch (JavaModelException e) { }
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Methods to get a list of active java elements				*/
/*										*/
/********************************************************************************/

// This shouldn't be needed since edits in a window should also be made in the default
// buffer and hence in the actual compilation unit that would be reported

void getActiveElements(IJavaElement root,List<IJavaElement> rslt)
{
   switch (root.getElementType()) {
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.FIELD :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.LOCAL_VARIABLE :
      case IJavaElement.METHOD :
      case IJavaElement.PACKAGE_DECLARATION :
      case IJavaElement.TYPE :
      case IJavaElement.TYPE_PARAMETER :
      default :
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	 IPackageFragmentRoot pfr = (IPackageFragmentRoot) root;
	 try {
	    if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE && pfr.hasChildren()) {
	       IJavaElement [] chld = pfr.getChildren();
	       for (IJavaElement c : chld) getActiveElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 IParent par = (IParent) root;
	 try {
	    if (par.hasChildren()) {
	       IJavaElement [] chld = par.getChildren();
	       for (IJavaElement c : chld) getActiveElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.COMPILATION_UNIT :
	 ICompilationUnit cu = (ICompilationUnit) root;
	 IProject ip = cu.getJavaProject().getProject();
	 File f = BedrockUtil.getFileForPath(cu.getPath(),ip);
	 String fnm = f.getPath();
	 FileData fd = file_map.get(fnm);
	 if (fd == null) rslt.add(cu);
	 else {
	    rslt.add(fd.getSearchUnit());
	  }
	 break;
    }
}




void getWorkingElements(IJavaElement root,List<ICompilationUnit> rslt)
{
   switch (root.getElementType()) {
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.FIELD :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.LOCAL_VARIABLE :
      case IJavaElement.METHOD :
      case IJavaElement.PACKAGE_DECLARATION :
      case IJavaElement.TYPE :
      case IJavaElement.TYPE_PARAMETER :
      default :
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	 IPackageFragmentRoot pfr = (IPackageFragmentRoot) root;
	 try {
	    if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE && pfr.hasChildren()) {
	       IJavaElement [] chld = pfr.getChildren();
	       for (IJavaElement c : chld) getWorkingElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 IParent par = (IParent) root;
	 try {
	    if (par.hasChildren()) {
	       IJavaElement [] chld = par.getChildren();
	       for (IJavaElement c : chld) getWorkingElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.COMPILATION_UNIT :
	 ICompilationUnit cu = (ICompilationUnit) root;
	 IProject ip = cu.getJavaProject().getProject();
	 File f = BedrockUtil.getFileForPath(cu.getPath(),ip);
	 String fnm = f.getPath();
	 FileData fd = file_map.get(fnm);
	 if (fd != null && fd.hasChanged()) {
	    rslt.add(fd.getEditableUnit(null));
	  }
	 break;
    }
}



void getCompilationElements(IJavaElement root,List<ICompilationUnit> rslt)
{
   switch (root.getElementType()) {
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.FIELD :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.LOCAL_VARIABLE :
      case IJavaElement.METHOD :
      case IJavaElement.PACKAGE_DECLARATION :
      case IJavaElement.TYPE :
      case IJavaElement.TYPE_PARAMETER :
      default :
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	 IPackageFragmentRoot pfr = (IPackageFragmentRoot) root;
	 try {
	    if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE && pfr.hasChildren()) {
	       IJavaElement [] chld = pfr.getChildren();
	       for (IJavaElement c : chld) getCompilationElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 IParent par = (IParent) root;
	 try {
	    if (par.hasChildren()) {
	       IJavaElement [] chld = par.getChildren();
	       for (IJavaElement c : chld) getCompilationElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.COMPILATION_UNIT :
	 ICompilationUnit cu = (ICompilationUnit) root;
	 IProject ip = cu.getJavaProject().getProject();
	 File f = BedrockUtil.getFileForPath(cu.getPath(),ip);
	 String fnm = f.getPath();
	 FileData fd = file_map.get(fnm);
	 if (fd != null) {
	    rslt.add(fd.getEditableUnit(null));
	  }
	 else rslt.add(cu);
	 break;
    }
}



/********************************************************************************/
/*										*/
/*	Methods to get the compilation unit					*/
/*										*/
/********************************************************************************/

ICompilationUnit getCompilationUnit(String proj,String file) throws BedrockException
{
   FileData fd = findFile(proj,file,null);

   return fd.getSearchUnit();
}



ICompilationUnit getBaseCompilationUnit(String proj,String file) throws BedrockException
{
   FileData fd = findFile(proj,file,null);

   return fd.getBaseUnit();
}



CompilationUnit getAST(String bid,String proj,String file) throws BedrockException
{
   FileData fd = findFile(proj,file,null);

   return fd.getAstRoot(bid);
}



private String getFileFromClass(String proj,String cls) throws BedrockException
{
   IProject ip = our_plugin.getProjectManager().findProjectForFile(proj,null);
   IType ityp = null;
   IJavaProject ijp = JavaCore.create(ip);
   String bcls = baseClassName(cls);
   try {
      if (ijp != null) ityp = ijp.findType(bcls);
    }
   catch (JavaModelException ex) { }
   if (ityp == null)
      throw new BedrockException("Class " + cls + " " + bcls + " not defined in project " + proj);

   ICompilationUnit icu = ityp.getCompilationUnit();
   if (icu == null)
      throw new BedrockException("Class " + cls + " " + bcls + " missing compilation unit");

   File f = BedrockUtil.getFileForPath(icu.getPath(),ip);
   return f.getPath();
}




/********************************************************************************/
/*										*/
/*	Methods for managing file data						*/
/*										*/
/********************************************************************************/

private synchronized FileData findFile(String proj,String file,String bid)
		throws BedrockException
{
   FileData fd = file_map.get(file);

   if (fd == null) {
      ICompilationUnit icu = null;
      icu = our_plugin.getProjectManager().getCompilationUnit(proj,file);
      if (icu == null && proj != null) {
	 icu = our_plugin.getProjectManager().getCompilationUnit(null,file);
	 if (icu != null) proj = null;
       }

      if (icu == null) return null;

      BedrockPlugin.logD("START FILE " + proj + " " + file + " " + bid + " " +
			    icu.isWorkingCopy() + " " + icu.hasResourceChanged() + " " +
			    icu.isOpen() + " " +
			    icu.getOwner() + " " +
			    System.identityHashCode(icu) + " " +
			    System.identityHashCode(icu.getPrimary()));

      icu = icu.getPrimary();

      fd = new FileData(proj,file,icu);
      file_map.put(file,fd);
    }

   if (bid != null) fd.getCurrentId(bid);		// force user for this file

   return fd;
}




/********************************************************************************/
/*										*/
/*	Background compilation and analysis task				*/
/*										*/
/********************************************************************************/

private class EditTask implements Runnable {

   private FileData file_data;
   private String bedrock_id;
   private String for_id;

   EditTask(FileData fd,String bid,String id) {
      file_data = fd;
      bedrock_id = bid;
      for_id = id;
    }

   @Override public void run() {
      try {
	 performEdit();
       }
      finally {
	 doneEdit();
       }
    }

   private void performEdit() {
      BedrockPlugin.logD("Start perform edit " + for_id + " " + bedrock_id + " " +
        		    file_data.getCurrentId(bedrock_id));
      if (file_data.getCurrentId(bedrock_id) != null &&
             !file_data.getCurrentId(bedrock_id).equals(for_id))
         return;
   
      long delay = getElideDelay(bedrock_id);
   
      if (delay > 0) {
         synchronized (this) {
            try { wait(delay); }
            catch (InterruptedException e) { }
          }
       }
   
      CompilationUnit cu = null;
      synchronized (file_data) {
         if (file_data.getCurrentId(bedrock_id) != null &&
               !file_data.getCurrentId(bedrock_id).equals(for_id))
            return;
   
         BedrockPlugin.logD("BUILD AST " + for_id + " " + bedrock_id);
         cu = file_data.getAstRoot(bedrock_id);
       }
   
      if (cu == null && for_id.startsWith("PID_")) {
         IvyXmlWriter xw = our_plugin.beginMessage("PRIVATEERROR",bedrock_id);
         xw.field("FILE",file_data.getFileName());
         xw.field("ID",for_id);
         xw.field("FAILURE",true);
         our_plugin.finishMessage(xw);
       }
   
      if (file_data.getCurrentId(bedrock_id) != null &&
             !file_data.getCurrentId(bedrock_id).equals(for_id)) {
         BedrockPlugin.logD("Discarding AST " + file_data.getCurrentId(bedrock_id) + " " + for_id);
         return;
       }
   
      if (getAutoElide(bedrock_id) && cu != null) {
         // System.err.println("BEDROCK: ELIDE " + for_id);
         BedrockElider be = file_data.checkElider(bedrock_id);
         if (be != null) {
            IvyXmlWriter xw = our_plugin.beginMessage("ELISION",bedrock_id);
            xw.field("FILE",file_data.getFileName());
            xw.field("ID",for_id);
            xw.begin("ELISION");
            if (be.computeElision(cu,xw)) {
               if (file_data.getCurrentId(bedrock_id) == null ||
        	      file_data.getCurrentId(bedrock_id).equals(for_id)) {
        	  xw.end("ELISION");
        	  our_plugin.finishMessage(xw);
        	}
             }
          }
       }
    }

}	// end of innerclass EditTask



private class OpenTask implements Runnable {

   private FileData file_data;
   private String bedrock_id;

   OpenTask(FileData fd,String pid) {
      file_data = fd;
      bedrock_id = pid;
    }

   @Override public void run() {
      BedrockPlugin.logD("RUNNING open task for " + bedrock_id);
      try {
	 file_data.getAstRoot(bedrock_id);
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem running open task",t);
       }
      BedrockPlugin.logD("FINISHED open task for " + bedrock_id);
    }

}	// end of inner class OpenTask




/********************************************************************************/
/*										*/
/*	Class for holding data about a file					*/
/*										*/
/********************************************************************************/
/********************************************************************************/
/*										*/
/*	The FileData structure represents all the information about a file.	*/
/*	It contains the basic compilation unit (the Primary unit), and the	*/
/*	default buffer (PrimaryUnit.getBuffer()).  This means that it should	*/
/*	reflect whatever changes are being done inside Eclipse.  The code here	*/
/*	should make every external (non-private) edit in all other external	*/
/*	buffers and in the default buffer as well.				*/
/*										*/
/*	Note that the default buffer can be a working copy rather than the	*/
/*	original.  This happens if the version of Eclipse has that file open	*/
/*	currently.								*/
/*										*/
/*	A BufferData structure represents a external Code Bubbles or a private	*/
/*	buffer.  It should be a working copy of the original compilation unit	*/
/*	(and should maintain that status at all times).  It also should have	*/
/*	its own IBuffer corresponding to that working copy.			*/
/*										*/
/*	All non-private buffers (default and BufferData) should be registered	*/
/*	with the FileData structure to monitor changes. 			*/
/*										*/
/********************************************************************************/

private class FileData implements IBufferChangedListener {

   private IProject for_project;
   private String file_name;
   private ICompilationUnit comp_unit;
   private IBuffer default_buffer;
   private Map<String,PrivateBufferData> buffer_map;
   private Map<String,UserData> buffer_users;
   private boolean doing_change;
   private String line_separator;
   private boolean update_on_open;
   private boolean safe_update;
   private String last_edit;
   private CompilationUnit last_ast;
   private DefaultCopyOwner copy_owner;
   private ICompilationUnit working_unit;
   private boolean is_deleted;

   FileData(String proj,String nm,ICompilationUnit cu) {
      try {
	 for_project = our_plugin.getProjectManager().findProjectForFile(proj,nm);
       }
      catch (BedrockException e) { }
      if (for_project == null) BedrockPlugin.logE("File " + nm + " has no associated project");
      file_name = nm;
      comp_unit = cu.getPrimary();
      doing_change = false;
      last_ast = null;
      buffer_map = new HashMap<>();
      buffer_users = new HashMap<>();
      last_edit = null;
      line_separator = null;
      update_on_open = false;
      safe_update = false;
      is_deleted = false;
      copy_owner = new DefaultCopyOwner(this);
      copy_owner.suppressErrors(true);
      try {
	 working_unit = comp_unit.getWorkingCopy(copy_owner,null);
	 default_buffer = working_unit.getBuffer();
	 default_buffer.addBufferChangedListener(this);
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem creating working copy: " + e,e);
       }
      catch (Throwable t) {
	 throw new Error("Problem getting editable unit: " + t,t);
       }
      finally {
	 copy_owner.suppressErrors(false);
       }
    }

   ICompilationUnit getSearchUnit() {
      return working_unit;
    }

   ICompilationUnit getBaseUnit() {
      try {
         if (!working_unit.isConsistent()) {
            BedrockPlugin.logD("Make base unit consistent");
            working_unit.commitWorkingCopy(true,null);
          }
       }
      catch (JavaModelException e) {
         BedrockPlugin.logE("Problem getting base unit",e);
       }
      
      return comp_unit; 
    }

   ICompilationUnit getEditableUnit(String sid) {
      return working_unit;
    }

   CompilationUnit getAstRoot(String bid) {
      if (buffer_map.get(bid) != null) {
         PrivateBufferData bd = buffer_map.get(bid);
         return bd.getAstRoot();
       }
      return getDefaultRoot(bid);
    }

   CompilationUnit getDefaultRoot(String bid) {
      if (last_ast != null) return last_ast;
   
      try {
         CompilationUnit cu = null;
         int fgs = ICompilationUnit.FORCE_PROBLEM_DETECTION | ICompilationUnit.ENABLE_STATEMENTS_RECOVERY |
               ICompilationUnit.ENABLE_BINDINGS_RECOVERY;
         cu = working_unit.reconcile(java_version,fgs,copy_owner,null);
         if (cu != null && bid != null) {
            last_ast = cu;
          }
         return cu;
       }
      catch (JavaModelException e) {
         BedrockPlugin.logE("Problem reconsiling working unit: " + e,e);
       }
      catch (Throwable t) {
         BedrockPlugin.logE("Problem reconsiling working unit: " + t,t);
       }
   
      return null;
    }

   void applyEdit(String bid,TextEdit xe,String id) throws BedrockException {
      boolean chngfg = hasChanged();

      if (bid != null && buffer_map.get(bid) != null) {
	 // handle private buffers
	 PrivateBufferData bd = buffer_map.get(bid);
	 bd.applyEdit(xe);
       }
      else {
	 try {
	    synchronized (this) {
	       last_edit = bid;
	       working_unit.applyTextEdit(xe,null);
	       setCurrentId(bid,id);
	       last_edit = null;
	       last_ast = null;
	     }
	  }
	 catch (JavaModelException e) {
	    throw new BedrockException("Problem applying text edit",e);
	  }
	 if (!chngfg && hasChanged()) {
	    IvyXmlWriter xw = our_plugin.beginMessage("FILECHANGE");
	    xw.field("FILE",file_name);
	    our_plugin.finishMessage(xw);
	  }
       }
    }

   String getFileName() 			{ return file_name; }
   IProject getProject()			{ return for_project; }
   String getLineSeparator() {
      if (line_separator == null) checkLineSeparator();
      return line_separator;
    }

   private boolean isPrivateId(String bid) {
      if (bid == null) return false;
      if (buffer_map.get(bid) != null) return true;
      if (bid.startsWith("PID_")) return true;
      return false;
    }

   String getCurrentId(String bid) {
      if (isPrivateId(bid)) return null;
      return getUser(bid).getCurrentId();
   }
   void setCurrentId(String bid,String id) {
      if (isPrivateId(bid)) return;
      getUser(bid).setCurrentId(id);
    }

   BedrockElider checkElider(String bid) {
      if (isPrivateId(bid)) return null;
      return getUser(bid).checkElider();
    }
   void clearElider(String bid) {
      if (isPrivateId(bid)) return;
      getUser(bid).clearElider();
    }
   BedrockElider getElider(String bid) {
      if (isPrivateId(bid)) return null;
      return getUser(bid).getElider();
    }

   ICompilationUnit getBaseCompUnit()		{ return comp_unit; }

   synchronized boolean hasChanged() {
      if (default_buffer == null) return false;
      return default_buffer.hasUnsavedChanges();
    }
   synchronized String getCurrentContents() {
      IBuffer dflt = setupDefaultBuffer();
      if (dflt == null) return null;
      return dflt.getContents();
    }
   synchronized int getLength() {
      IBuffer dflt = setupDefaultBuffer();
      return (dflt == null ? 0 : dflt.getLength());
    }

   String getContents(String bid) {
      return getCurrentContents();
    }

   void noteEdit(String bid,int soff,int len,int rlen) {
      if (bid != null && isPrivateId(bid)) return;
      getUser(bid).noteEdit(soff,len,rlen);
    }
   
   void noteDeleted() {
      is_deleted = true;
    }

   synchronized void commit(boolean refresh,boolean save,boolean compile) throws JavaModelException {
      // first ensure the default ICompilationUnit is saved/refreshed
      if (is_deleted) return;
      BedrockBreakMover brkmvr = null;
      if (compile) {
         last_ast = null;
         getDefaultRoot(last_edit);
       }
      else if (!refresh) {
         if (working_unit != null) {
            BedrockPlugin.logD("Commiting working copy for file data " + file_name);
            String proj = null;
            if (for_project != null) proj = for_project.getName();
            brkmvr = new BedrockBreakMover(proj,file_name);
            working_unit.commitWorkingCopy(true,null);
            last_ast = null;
          }
         if (save) {
            try {
               safe_update = true;
               BedrockProgressMonitor pm = new BedrockProgressMonitor(our_plugin,"Saving");
               comp_unit.save(pm,true);
               pm.finish();
               safe_update = false;
             }
            catch (ArrayIndexOutOfBoundsException e) { }
          }
         if (brkmvr != null) {
            brkmvr.restoreBreakpoints();
          }
       }
      else if (refresh) {
         comp_unit.restore();
         last_ast = null;
         // compare original and default buffer
       }
    }

   private UserData getUser(String sid) {
      if (sid == null) return null;

      synchronized (buffer_users) {
	 UserData ud = buffer_users.get(sid);
	 if (ud == null) {
	    if (buffer_map.get(sid) != null || sid.startsWith("PID_")) {
	       BedrockPlugin.logX("Attempt to get user for private buffer");
	       return null;
	     }
	    ud = new UserData(this,sid,comp_unit,false);
	    buffer_users.put(sid,ud);
	  }
	 return ud;
       }
   }

   private void removeUser(String sid)
   {
      if (sid == null) return;
      synchronized (buffer_users) {
	 buffer_users.remove(sid);
       }
      BedrockPlugin.logD("REMOVE USER " + sid + " for " + file_name);
   }

   private PrivateBufferData createPrivateBuffer(String sid,String opid) throws BedrockException {
      synchronized (buffer_map) {
	 PrivateBufferData bd = buffer_map.get(sid);
	 if (bd != null) return null;
	 ICompilationUnit cu = null;
	 if (opid == null) cu = comp_unit;
	 else {
	    PrivateBufferData obd = buffer_map.get(opid);
	    if (obd == null) return null;
	    cu = obd.getEditableUnit();
	  }
	 bd = new PrivateBufferData(this,sid,cu);
	 buffer_map.put(sid,bd);
	 return bd;
       }
    }

   private void removePrivateBuffer(String sid) {
      BedrockPlugin.logD("Remove private buffer " + sid);
      synchronized (buffer_map) {
	 PrivateBufferData bd = buffer_map.remove(sid);
	 if (bd != null) bd.free();
       }
    }

   private void handleUpdate() {
      try {
	 String cnts = comp_unit.getBuffer().getContents();
	 String ocnts = working_unit.getBuffer().getContents();
	 if (cnts.equals(ocnts)) return;
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem getting update information",e);
       }

      if (default_buffer != null) default_buffer.removeBufferChangedListener(this);
      default_buffer = null;

      setupDefaultBuffer();
      updateOnOpen();
    }


   @Override public void bufferChanged(BufferChangedEvent evt) {
      BedrockPlugin.logD("Buffer check change " + doing_change + " " +
        		    System.identityHashCode(evt.getBuffer()));
      if (doing_change) return;
      doing_change = true;
      try {
         IBuffer buf = evt.getBuffer();
         int len = evt.getLength();
         int off = evt.getOffset();
         String txt = evt.getText();
         BedrockPlugin.logD("Buffer change " + file_name + " " + len + " " + off + " " +
        		       (txt == null) + " " + (buf == default_buffer) + " " +
        		       System.identityHashCode(default_buffer) + " " +
        		       System.identityHashCode(buf) + " " +
        		       Thread.currentThread().getName());
         if (len == 0 && off == 0 && txt == null) {
            // buffer closed event
            if (buf != default_buffer && default_buffer != null) {
               BedrockPlugin.logEX("Unknown buffer for buffer switch " + buf.hashCode() + " " +
        			      System.identityHashCode(default_buffer));
               return;
             }
            if (default_buffer == null) return;
            BedrockPlugin.logEX("Buffer switch occurred for " + file_name + " " +
        			  safe_update + " " + System.identityHashCode(default_buffer));
            if (buf != default_buffer) {
               default_buffer = null;
               if (!safe_update) update_on_open = true;
             }
            return;
          }
   
         int ctr = 0;
         List<String> del = null;
         for (Map.Entry<String,UserData> ent : buffer_users.entrySet()) {
            String user = ent.getKey();
            if (last_edit == null || last_edit.equals(user)) continue;
            IvyXmlWriter xw = our_plugin.beginMessage("EDIT",user);
            BedrockPlugin.logD("START EDIT " + user + " " + len + " " + off + " " + (ctr++));
            xw.field("FILE",file_name);
            xw.field("LENGTH",len);
            xw.field("OFFSET",off);
            if (len == buf.getLength() && off == 0 && txt != null) {
               xw.field("COMPLETE",true);
               byte [] data = txt.getBytes();
               xw.bytesElement("CONTENTS",data);
             }
            else {
               xw.cdata(txt);
             }
            String rslt = our_plugin.finishMessageWait(xw,5000);
            BedrockPlugin.logD("SENDING EDIT " + xw.toString());
            UserData userdata = ent.getValue();
            if (userdata.noteAlive(rslt != null)) {
               if (del == null) del = new ArrayList<>();
               del.add(user);
             }
          }
         if (del != null) {
            for (String s : del) removeUser(s);
          }
   
         setupDefaultBuffer();
       }
      finally {
         doing_change = false;
       }
    }

   private void checkLineSeparator() {
      line_separator = null;
      if (default_buffer != null) {
	 int ln = default_buffer.getLength();
	 boolean havecr = false;
	 for (int i = 0; i < ln; ++i) {
	    char c = default_buffer.getChar(i);
	    if (c == '\r') havecr = true;
	    else if (c == '\n') {
	       if (havecr) line_separator = "\r\n";
	       else line_separator = "\n";
	       break;
	     }
	    else {
	       if (havecr) {
		  line_separator = "\r";
		  break;
		}
	     }
	  }
       }

      if (line_separator == null && comp_unit != null) {
	 try {
	    line_separator = comp_unit.findRecommendedLineSeparator();
	  }
	 catch (JavaModelException e) { }
       }

      if (line_separator == null) {
	 QualifiedName qn0 = new QualifiedName("line","separator");
	 for (IResource ir = default_buffer.getUnderlyingResource(); ir != null; ir = ir.getParent()) {
	    String ls = null;
	    try {
	       ls = ir.getPersistentProperty(qn0);
	     }
	    catch (CoreException e) {
	       BedrockPlugin.logE("EXCEPTION ON LINE SEPARATOR: " + e,e);
	     }
	    if (ls != null) {
	       BedrockPlugin.logD("LINE SEPARATOR STRING = '" + ls + "'");
	       if (ls.equals("\\n")) line_separator = "\n";
	       else if (ls.equals("\\r\\n")) line_separator = "\r\n";
	       else if (ls.equals("\\r")) line_separator = "\r";
	       else line_separator = ls;
	       break;
	     }
	  }
       }
      if (line_separator == null) line_separator = System.getProperty("line.separator");
    }

   private synchronized IBuffer setupDefaultBuffer() {
      if (default_buffer != null) {
	 if (!default_buffer.isClosed()) return default_buffer;
	 default_buffer = null;
       }

      BedrockPlugin.logD("Set up default buffer for " + file_name);

      copy_owner.suppressErrors(true);
      try {
	 if (working_unit != null) working_unit.discardWorkingCopy();
	 working_unit = comp_unit.getWorkingCopy(copy_owner,null);
	 default_buffer = working_unit.getBuffer();
	 default_buffer.addBufferChangedListener(this);
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem creating working copy: " + e,e);
       }
      catch (Throwable t) {
	 throw new Error("Problem getting editable unit: " + t,t);
       }
      finally {
	 copy_owner.suppressErrors(false);
       }

      if (update_on_open) updateOnOpen();
      if (line_separator == null) checkLineSeparator();

      return default_buffer;
    }

   private void updateOnOpen() {
      update_on_open = false;
      if (safe_update) {
	 safe_update = false;
	 return;
       }

      BedrockPlugin.logD("Update on Open " + file_name + " " + default_buffer);

      if (default_buffer != null) {
	 String nbuf = default_buffer.getText(0, default_buffer.getLength());
	 for (String user : buffer_users.keySet()) {
	    IvyXmlWriter xw = our_plugin.beginMessage("EDIT",user);
	    xw.field("FILE",file_name);
	    xw.field("LENGTH",nbuf.length());
	    xw.field("OFFSET",0);
	    xw.field("COMPLETE",true);
	    byte [] data = nbuf.getBytes();
	    xw.bytesElement("CONTENTS",data);
	    our_plugin.finishMessage(xw);
	  }
       }
      else {
	 for (String user : buffer_users.keySet()) {
	    IvyXmlWriter xw = our_plugin.beginMessage("EDIT",user);
	    xw.field("FILE",file_name);
	    xw.field("LENGTH",0);
	    xw.field("OFFSET",0);
	    xw.field("COMPLETE",true);
	    xw.field("REMOVE",true);
	    our_plugin.finishMessage(xw);
	  }
       }
    }


}	// end of innerclass FileData





private class PrivateBufferData {

   private FileData file_data;
   private String bedrock_id;
   private ICompilationUnit comp_unit;
   private boolean is_setup;
   private CopyOwner copy_owner;
   private CompilationUnit last_ast;

   PrivateBufferData(FileData fd,String bid,ICompilationUnit base) {
      BedrockPlugin.logD("Create private buffer for " + fd.getFileName() + " " + bid);
      file_data = fd;
      bedrock_id = bid;
      is_setup = false;
      last_ast = null;
      copy_owner = new CopyOwner(file_data,bedrock_id,true);
      comp_unit = base;
    }

   void free() {
      if (is_setup) {
	 if (comp_unit.isWorkingCopy()) {
	    try {
	       comp_unit.discardWorkingCopy();
	     }
	    catch (JavaModelException e) { }
	  }
	 comp_unit = null;
       }
    }

   synchronized ICompilationUnit getEditableUnit() {
      if (is_setup) return comp_unit;
      BedrockPlugin.logD("Set up " + bedrock_id + " " + file_data.getFileName());
      try {
         comp_unit = file_data.getBaseCompUnit();
         // comp_unit.getSource() being null causes NullPointerException
         if (comp_unit.getSource() != null) {
            copy_owner.suppressErrors(true);
            comp_unit = comp_unit.getWorkingCopy(copy_owner,null);
            copy_owner.suppressErrors(false);
          }
         else {
            BedrockPlugin.logE("Compilation unit lacking source for " + bedrock_id + " " +
        			  file_data.getFileName() + " " + comp_unit.isOpen() + " " + comp_unit.isWorkingCopy() +
        			  " " + comp_unit.getSource() + " " + comp_unit.getBuffer().getLength());
          }
         is_setup = true;
       }
      catch (JavaModelException e) {
         BedrockPlugin.logE("Problem creating working copy: " + e,e);
       }
      catch (Throwable t) {
         throw new Error("Problem getting editable unit: " + t,t);
       }
   
      return comp_unit;
    }

   synchronized CompilationUnit getDefaultRoot() {
      if (last_ast != null) return last_ast;
      ASTParser p = ASTParser.newParser(java_version);
      p.setKind(ASTParser.K_COMPILATION_UNIT);
      p.setResolveBindings(true);
      p.setSource(comp_unit);
      CompilationUnit cu = (CompilationUnit) p.createAST(null);
      last_ast = cu;
      return cu;
    }

   synchronized CompilationUnit getAstRoot() {
      ICompilationUnit icu = getEditableUnit();
      CompilationUnit cu = null;
      try {
	 copy_owner.suppressErrors(false);
	 cu = icu.reconcile(java_version,true,true,copy_owner,null);
	 if (cu == null) cu = last_ast;
	 else last_ast = cu;
	 if (last_ast == null) getDefaultRoot();
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem getting AST for file " +
			       file_data.getFileName() + ": " + e,e);
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem getting AST for file " +
			       file_data.getFileName() + ": " + t,t);
       }
      return last_ast;
    }

   synchronized void applyEdit(TextEdit xe) throws BedrockException {
      ICompilationUnit icu = getEditableUnit();
      try {
	 icu.applyTextEdit(xe,null);
       }
      catch (JavaModelException e) {
	 throw new BedrockException("Problem editing source file " + file_data.getFileName() +
				       " " + xe,e);
       }
      last_ast = null;
    }

}	// end of innerclass BufferData



private class UserData {

   private String current_id;
   private BedrockElider elision_data;
   private int dead_count;

   UserData(FileData fd,String bid,ICompilationUnit base,boolean pvt) {
      BedrockPlugin.logD("Create buffer for " + fd.getFileName() + " " + bid + " " + pvt);
      current_id = null;
      elision_data = null;
      dead_count = 0;
    }

   String getCurrentId()			{ return current_id; }
   void setCurrentId(String id) 		{ current_id = id; }

   BedrockElider checkElider()			{ return elision_data; }
   void clearElider()				{ elision_data = null; }
   synchronized BedrockElider getElider() {
      if (elision_data == null) {
	 elision_data = new BedrockElider();
       }
      return elision_data;
    }

   void noteEdit(int soff,int len,int rlen) {
      if (elision_data != null) elision_data.noteEdit(soff,len,rlen);
    }

   boolean noteAlive(boolean alive) {
      if (alive) {
	 dead_count = 0;
       }
      else if (++dead_count >= DEAD_COUNT) return true;
      return false;
    }

}	// end of innerclass UserData




/********************************************************************************/
/*										*/
/*	Working copy owner							*/
/*										*/
/********************************************************************************/

private class DefaultCopyOwner extends WorkingCopyOwner {

   protected FileData for_file;
   protected ProblemHandler problem_handler;
   protected boolean suppress_errors;

   DefaultCopyOwner(FileData fd) {
      for_file = fd;
      problem_handler = null;
      suppress_errors = false;
    }

   @Override public IBuffer createBuffer(ICompilationUnit cu) {
      IBuffer buf = super.createBuffer(cu);
      BedrockPlugin.logD("CREATE BUFFER " + buf.getLength() +
			    " " + for_file.getFileName() + " " + buf.hashCode());
      return buf;
    }

   @Override public IProblemRequestor getProblemRequestor(ICompilationUnit ic) {
      if (problem_handler == null) {
	 problem_handler = new ProblemHandler(for_file,null,ic,false);
	 problem_handler.setSuppressErrors(suppress_errors);
       }
      return problem_handler;
    }

   void suppressErrors(boolean fg) {
      suppress_errors = fg;
      if (problem_handler != null) problem_handler.setSuppressErrors(fg);
    }

}	// end of inner class DefaultCopyOwner



private class CopyOwner extends DefaultCopyOwner {

   private String bedrock_id;
   private boolean is_private;

   CopyOwner(FileData fd,String bid,boolean pvt) {
      super(fd);
      bedrock_id = bid;
      is_private = pvt;
    }

   @Override public IBuffer createBuffer(ICompilationUnit cu) {
      IBuffer buf = super.createBuffer(cu);
      String cnts = for_file.getContents(null);
      BedrockPlugin.logD("CREATE BUFFER1 " + bedrock_id + " " + is_private + " " + (cnts == null) +
			    " " + for_file.getFileName() + " " + buf.hashCode());
      if (cnts != null) buf.setContents(cnts);
      return buf;
    }

   @Override public IProblemRequestor getProblemRequestor(ICompilationUnit ic) {
      if (problem_handler == null) {
	 problem_handler = new ProblemHandler(for_file,bedrock_id,ic,is_private);
	 problem_handler.setSuppressErrors(suppress_errors);
       }
      return problem_handler;
    }

}	// end of innerclass CopyOwner



/********************************************************************************/
/*										*/
/*	Class to handle compilation problems					*/
/*										*/
/********************************************************************************/

private class ProblemHandler implements IProblemRequestor {

   private FileData file_data;
   private String bedrock_id;
   private List<IProblem> problem_set;
   private boolean is_private;
   private boolean suppress_errors;

   ProblemHandler(FileData fd,String bid,ICompilationUnit cu,boolean pvt) {
      file_data = fd;
      bedrock_id = bid;
      problem_set = null;
      is_private = pvt;
      suppress_errors = false;
    }

   void setSuppressErrors(boolean fg)		{ suppress_errors = fg; }

   @Override public void acceptProblem(IProblem ip) {
      BedrockPlugin.logD("Note problem for " + is_private + " " + suppress_errors);
      if (suppress_errors) return;
      if (problem_set == null) problem_set = new ArrayList<IProblem>();
      problem_set.add(ip);
    }

   @Override public void beginReporting() {
      BedrockPlugin.logD("Begin error reporting for " + is_private + " " +
			    suppress_errors);
      problem_set = null;
    }

   @Override public void endReporting() {
      BedrockPlugin.logD("End error reporting for " + is_private);
      if (suppress_errors) return;

      IvyXmlWriter xw;
      if (is_private) {
	 if (bedrock_id != null) {
	    xw = our_plugin.beginMessage("PRIVATEERROR",bedrock_id);
	    xw.field("FILE",file_data.getFileName());
	    xw.field("ID",bedrock_id);
	  }
	 else return;
       }
      else if (file_data != null) {
	 xw = our_plugin.beginMessage("FILEERROR",bedrock_id);
	 xw.field("FILE",file_data.getFileName());
       }
      else return;

      if (file_data != null && file_data.getProject() != null) {
	 xw.field("PROJECT",file_data.getProject().getName());
       }
      xw.begin("MESSAGES");
      if (problem_set != null) {
	 for (IProblem ip : problem_set) {
	    BedrockUtil.outputProblem(file_data.getProject(),ip,xw);
	  }
       }
      xw.end("MESSAGES");

      our_plugin.finishMessage(xw);

      BedrockPlugin.logD("ERROR REPORT: " + xw.toString());
      xw.close();
    }

   @Override public boolean isActive()			{ return true; }

}	// end of innerclass ProblemHandler









/********************************************************************************/
/*										*/
/*	Class to handle completion information					*/
/*										*/
/********************************************************************************/

private class CompletionHandler extends CompletionRequestor {

   private IvyXmlWriter xml_writer;
   private String bedrock_id;
   private boolean generate_message;

   CompletionHandler(IvyXmlWriter xw,String bid) {
      setRequireExtendedContext(true);
      setAllowsRequiredProposals(CompletionProposal.FIELD_REF,CompletionProposal.TYPE_REF,true);
      setAllowsRequiredProposals(CompletionProposal.FIELD_REF,CompletionProposal.TYPE_IMPORT,true);
      setAllowsRequiredProposals(CompletionProposal.FIELD_REF,CompletionProposal.FIELD_IMPORT,true);
      for (int i = 1; i <= 27; ++i) {
	 setIgnored(i,false);
       }

      bedrock_id = bid;
      xml_writer = xw;
      if (xw != null) {
	 generate_message = false;
       }
      else {
	 generate_message = true;
	 xml_writer = our_plugin.beginMessage("COMPLETIONS",bedrock_id);
       }
    }

   @Override public void beginReporting() {
      xml_writer.begin("COMPLETIONS");
    }

   @Override public void accept(CompletionProposal cp) {
      BedrockUtil.outputCompletion(cp,xml_writer);
    }

   @Override public void endReporting() {
      xml_writer.end("COMPLETIONS");
      if (generate_message) our_plugin.finishMessage(xml_writer);
    }

   @Override public void completionFailure(IProblem ip) {
      BedrockUtil.outputProblem(null,ip,xml_writer);
    }

}	// end of innerclass CompletionHandler



/********************************************************************************/
/*										*/
/*	Class to hold parameter settings for bedrock client			*/
/*										*/
/********************************************************************************/

private boolean getAutoElide(String id) { return getParameters(id).getAutoElide(); }
private long getElideDelay(String id)	{ return getParameters(id).getElideDelay(); }

private void setAutoElide(String id,boolean v)	{ getParameters(id).setAutoElide(v); }
private void setElideDelay(String id,long v)	{ getParameters(id).setElideDelay(v); }


private ParamSettings getParameters(String id)
{
   ParamSettings ps = param_map.get(id);
   if (ps == null) {
      ps = new ParamSettings();
      param_map.put(id,ps);
    }
   return ps;
}



private static class ParamSettings {

   private boolean auto_elide;
   private long    elide_delay;

   ParamSettings() {
      auto_elide = false;
      elide_delay = 0;
    }

   boolean getAutoElide()		{ return auto_elide; }
   long getElideDelay() 		{ return elide_delay; }

   void setAutoElide(boolean fg)	{ auto_elide = fg; }
   void setElideDelay(long v)		{ elide_delay = v; }

}	// end of inner class ParamSettings





}	// end of class BedrockEditManager



/* end of BedrockEditManager.java */
