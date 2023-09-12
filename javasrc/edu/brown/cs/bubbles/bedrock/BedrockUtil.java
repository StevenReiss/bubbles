/********************************************************************************/
/*										*/
/*		BedrockUtil.java						*/
/*										*/
/*	Utility methods for Bubbles - Eclipse interface 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.core.CompletionFlags;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaClassObject;
import org.eclipse.jdt.debug.core.IJavaClassPrepareBreakpoint;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodEntryBreakpoint;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaTargetPatternBreakpoint;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.UndoTextFileChange;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.core.refactoring.resource.MoveResourceChange;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;
import org.eclipse.text.edits.CopySourceEdit;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.CopyingRangeMarker;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BedrockUtil implements BedrockConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static Set<String>	sources_sent;
private static Map<ILaunchConfiguration,String>  launch_ids;

private static Map<String,Integer> resource_types;
private static Map<String,Integer> delta_kinds;
private static Map<String,Integer> delta_flags;
private static Map<String,Integer> completion_types;
private static Map<String,Integer> accessibility_types;
private static Map<String,Integer> access_flags;

private static int edit_counter = 1;

private static Random		random_gen = new Random();


static {
   sources_sent = new HashSet<String>();
   launch_ids = new HashMap<ILaunchConfiguration,String>();

   resource_types = new HashMap<String,Integer>();
   resource_types.put("FILE",1);
   resource_types.put("FOLDER",2);
   resource_types.put("PROJECT",4);
   resource_types.put("ROOT",8);

   delta_kinds = new HashMap<String,Integer>();
   delta_kinds.put("ADDED",1);
   delta_kinds.put("REMOVED",2);
   delta_kinds.put("CHANGED",4);
   delta_kinds.put("ADDED_PHANTOM",8);
   delta_kinds.put("REMOVED_PHANTOM",16);

   delta_flags = new HashMap<String,Integer>();
   delta_flags.put("CONTENT",256);
   delta_flags.put("COPIED_FROM",2048);
   delta_flags.put("MOVED_FROM",4096);
   delta_flags.put("MOVED_TO",8192);
   delta_flags.put("OPEN",16384);
   delta_flags.put("TYPE",32768);
   delta_flags.put("SYNC",65536);
   delta_flags.put("MARKERS",131072);
   delta_flags.put("REPLACED",262144);
   delta_flags.put("DESCRIPTION",524288);
   delta_flags.put("ENCODING",1048576);
   delta_flags.put("LOCAL_CHANGED",2097152);

   completion_types = new HashMap<String,Integer>();
   completion_types.put("ANNOTATION_ATTRIBUTE_REF",13);
   completion_types.put("ANONYMOUS_CLASS_DECLARATION",1);
   completion_types.put("FIELD_IMPORT",21);
   completion_types.put("FIELD_REF",2);
   completion_types.put("FIELD_REF_WITH_CASTED_RECEIVER",25);
   completion_types.put("JAVADOC_BLOCK_TAG",19);
   completion_types.put("JAVADOC_FIELD_REF",14);
   completion_types.put("JAVADOC_INLINE_TAG",20);
   completion_types.put("JAVADOC_METHOD_REF",15);
   completion_types.put("JAVADOC_PARAM_REF",18);
   completion_types.put("JAVADOC_TYPE_REF",16);
   completion_types.put("JAVADOC_VALUE_REF",17);
   completion_types.put("KEYWORD",3);
   completion_types.put("LABEL_REF",4);
   completion_types.put("LOCAL_VARIABLE_REF",5);
   completion_types.put("METHOD_DECLARATION",7);
   completion_types.put("METHOD_IMPORT",22);
   completion_types.put("METHOD_NAME_REFERENCE",12);
   completion_types.put("METHOD_REF",6);
   completion_types.put("METHOD_REF_WITH_CASTED_RECEIVER",24);
   completion_types.put("PACKAGE_REF",8);
   completion_types.put("POTENTIAL_METHOD_DECLARATION",11);
   completion_types.put("TYPE_IMPORT",23);
   completion_types.put("TYPE_REF",9);
   completion_types.put("VARIABLE_DECLARATION",10);

   accessibility_types = new HashMap<String,Integer>();
   accessibility_types.put("ACCESSIBLE",0);
   accessibility_types.put("DISCOURAGED",1);
   accessibility_types.put("NON_ACCESSIBLE",2);

   access_flags = new HashMap<String,Integer>();
   access_flags.put("AccAbstract",1024);
   access_flags.put("AccAnnotation",8192);
   access_flags.put("AccBridge",64);
   access_flags.put("AccDeprecated",1048576);
   access_flags.put("AccEnum",16384);
   access_flags.put("AccFinal",16);
   access_flags.put("AccInterface",512);
   access_flags.put("AccNative",256);
   access_flags.put("AccPrivate",2);
   access_flags.put("AccProtected",4);
   access_flags.put("AccPublic",1);
   access_flags.put("AccStatic",8);
   access_flags.put("AccStrictfp",2048);
   access_flags.put("AccSuper",32);
   access_flags.put("AccSynchronized",32);
   access_flags.put("AccSynthetic",4096);
   access_flags.put("AccTransient",128);
   access_flags.put("AccVarargs",128);
   access_flags.put("AccVolatile",64);
}


private static final int	MAX_PROBLEM = 4096;
private static final int	MAX_VALUE_SIZE = 40960;




/********************************************************************************/
/*										*/
/*	Methods to convert ints to strings for output				*/
/*										*/
/********************************************************************************/

static void fieldValue(IvyXmlWriter xw,String nm,int val,Map<String,Integer> vals)
{
   String r = null;

   for (Map.Entry<String,Integer> ent : vals.entrySet()) {
      if (ent.getValue() == val) {
	 r = ent.getKey();
	 break;
       }
    }
   if (r == null && val == 0) return;
   if (r == null) xw.field(nm,val);
   else xw.field(nm,r);
}



static void fieldFlags(IvyXmlWriter xw,String nm,int val,Map<String,Integer> vals)
{
   String r = null;

   for (Map.Entry<String,Integer> ent : vals.entrySet()) {
      if ((ent.getValue() & val) != 0) {
	 if (r == null) r = ent.getKey();
	 else r += "," + ent.getKey();
       }
    }
   if (r == null && val == 0) return;
   if (r == null) xw.field(nm,val);
   else xw.field(nm,r);
}



/********************************************************************************/
/*										*/
/*	Output methods for IProblem						*/
/*										*/
/********************************************************************************/

static void outputProblem(IProject proj,IProblem ip,IvyXmlWriter xw)
{
   xw.begin("PROBLEM");

   if (ip instanceof IMarker) {
      IMarker xmk = (IMarker) ip;
      xw.field("ID",xmk.getId());
    }

   xw.field("MSGID",ip.getID());
   xw.field("MESSAGE",ip.getMessage());
   char [] filc = ip.getOriginatingFileName();
   if (filc != null) {
      File fnm = new File(new String(filc));
      fnm = getFileForPath(fnm,proj);
      xw.field("FILE",fnm.getAbsolutePath());
    }
   xw.field("LINE",ip.getSourceLineNumber());
   xw.field("START",ip.getSourceStart());
   xw.field("END",ip.getSourceEnd());
   if (proj != null) xw.field("PROJECT",proj.getName());
   if (ip.isError()) xw.field("ERROR",true);
   else {
      switch (ip.getID()) {
	 case IProblem.Task :
	    break;
	 default :
	    xw.field("WARNING",true);
	    break;
       }
    }

   for (String s : ip.getArguments()) { xw.textElement("ARG",s); }

   BedrockPlugin.getPlugin().addFixes(ip,xw);

   xw.end("PROBLEM");
}



static void outputMarkers(IProject proj,IMarker [] mrks,IvyXmlWriter xw)
{
   int ctr = 0;

   //TODO:  if mrks.length > MAX_PROBLEM then prioritize to do errors before warnings

   Set<IMarker> done = new HashSet<IMarker>();
   for (IMarker mrk : mrks) {
      if (done.contains(mrk)) continue;
      done.add(mrk);
      IResource irc = mrk.getResource();
      // group by file
      if (irc == null || !(irc instanceof IFile)) continue;
      IFile f = (IFile) irc;
      File fil = f.getFullPath().toFile();
      fil = getFileForPath(fil,proj);

      for (IMarker xmk : mrks) {
	 if (xmk != mrk && done.contains(xmk)) continue;
	 if (xmk.getResource() != irc) continue;
	 done.add(xmk);
	 outputMarker(xmk,fil,xw);
	 ++ctr;
       }
      if (ctr > MAX_PROBLEM) break;
    }
}



static void outputMarker(IMarker xmk,File fil,IvyXmlWriter xw)
{
   if (xmk instanceof IProblem) {
      BedrockUtil.outputProblem(null,(IProblem) xmk,xw);
    }
   else {
      String mtyp = null;
      try {
	 mtyp = xmk.getType();
       }
      catch (CoreException e) { return; }
      if (mtyp.contains("Breakpoint")) return;

      xw.begin("PROBLEM");
      xw.field("TYPE",mtyp);
      xw.field("ID",xmk.getId());
      int sev = xmk.getAttribute(IMarker.SEVERITY,IMarker.SEVERITY_INFO);
      if (sev == IMarker.SEVERITY_ERROR) xw.field("ERROR",true);
      else if (sev == IMarker.SEVERITY_WARNING) xw.field("WARNING",true);
      int lno = xmk.getAttribute(IMarker.LINE_NUMBER,-1);
      if (lno >= 0) {
	 xw.field("LINE",lno);
	 xw.field("START",xmk.getAttribute(IMarker.CHAR_START,0));
	 xw.field("END",xmk.getAttribute(IMarker.CHAR_END,0));
       }
      xw.field("MSGID",xmk.getAttribute(IJavaModelMarker.ID,0));
      xw.field("FLAGS",xmk.getAttribute(IJavaModelMarker.FLAGS,0));
      xw.textElement("FILE",fil.getPath());
      String msg = xmk.getAttribute(IMarker.MESSAGE,"");
      msg = IvyXml.xmlSanitize(msg,false);
      xw.textElement("MESSAGE",msg);
      String args = xmk.getAttribute(IJavaModelMarker.ARGUMENTS,null);
      if (args != null) {
	 StringTokenizer tok = new StringTokenizer(args,":#");
	 if (tok.hasMoreTokens()) tok.nextToken();   // skip count
	 while (tok.hasMoreTokens()) {
	    xw.cdataElement("ARG",tok.nextToken());
	  }
       }
      BedrockPlugin.getPlugin().addFixes(xmk,xw);
      xw.end("PROBLEM");
    }
}




/********************************************************************************/
/*										*/
/*	Output methods for IBreakpoint						*/
/*										*/
/********************************************************************************/

static void outputBreakpoint(IBreakpoint xbp,IvyXmlWriter xw)
{
   if (xbp == null) return;
   if (!(xbp instanceof IJavaBreakpoint)) return;
   IJavaBreakpoint bp = (IJavaBreakpoint) xbp;
   IMarker mk = bp.getMarker();
   boolean typed = false;

   xw.begin("BREAKPOINT");
   try {
      xw.field("ID",bp.hashCode());
      xw.field("ENABLED",bp.isEnabled());

      if (mk != null && mk.exists()) {
	 if (bp.getHitCount() >= 0) xw.field("HITCOUNT",bp.getHitCount());
	 if (bp.getSuspendPolicy() == IJavaBreakpoint.SUSPEND_THREAD) xw.field("SUSPEND","THREAD");
	 else xw.field("SUSPEND","VM");
	 if (bp.getTypeName() != null) {
	    xw.field("CLASS",bp.getTypeName());
	    String fnm = findFileForClass(bp.getTypeName());
	    if (fnm != null) xw.field("FILE",fnm);
	  }
	 if (bp instanceof IJavaLineBreakpoint) {
	    IJavaLineBreakpoint lb = (IJavaLineBreakpoint) bp;
	    xw.field("LINE",lb.getLineNumber());
	    if (lb.getCharStart() >= 0) {
	       xw.field("STARTPOS",lb.getCharStart());
	       xw.field("ENDPOS",lb.getCharEnd());
	     }
	    if (mk != null && mk.getAttribute("TRACEPOINT",false)) {
	       xw.field("TRACEPOINT",true);
	     }
	  }

	 if (bp instanceof IJavaClassPrepareBreakpoint) {
	    IJavaClassPrepareBreakpoint cpb = (IJavaClassPrepareBreakpoint) bp;
	    xw.field("TYPE","CLASSPREPARE");
	    typed = true;
	    if (cpb.getMemberType() == IJavaClassPrepareBreakpoint.TYPE_INTERFACE)
	       xw.field("INTERFACE",true);
	  }
	 else if (bp instanceof IJavaExceptionBreakpoint) {
	    IJavaExceptionBreakpoint eb = (IJavaExceptionBreakpoint) bp;
	    xw.field("TYPE","EXCEPTION");
	    typed = true;
	    xw.field("ISCAUGHT",eb.isCaught());
	    xw.field("ISCHECKED",eb.isChecked());
	    xw.field("ISUNCAUGHT",eb.isUncaught());
	    xw.field("EXCEPTION",eb.getExceptionTypeName());
	    Object val = eb.getMarker().getAttribute("org.eclipse.jdt.debug.core.suspend_on_subclasses");
	    if (val != null) xw.field("ISSUBCLASSES",val);
	    else {
	       eb.getMarker().setAttribute("org.eclipse.jdt.debug.core.suspend_on_subclasses",true);
	       xw.field("ISSUBCLASSES",true);
	     }
	    for (String ef : eb.getExclusionFilters()) {
	       xw.textElement("EXCLUDE",ef);
	     }
	    for (String ef : eb.getInclusionFilters()) {
	       xw.textElement("INCLUDE",ef);
	     }
	  }
	 else if (bp instanceof IJavaMethodBreakpoint) {
	    IJavaMethodBreakpoint mp = (IJavaMethodBreakpoint) bp;
	    xw.field("TYPE","METHOD");
	    typed = true;
	    xw.field("ENTRY",mp.isEntry());
	    xw.field("EXIT",mp.isExit());
	    xw.field("NATIVE",mp.isNativeOnly());
	    xw.begin("METHOD");
	    xw.field("CLASS",mp.getTypeName());
	    xw.field("NAME",mp.getMethodName());
	    xw.text(mp.getMethodSignature());
	    xw.end("METHOD");
	  }
	 else if (bp instanceof IJavaMethodEntryBreakpoint) {
	    IJavaMethodEntryBreakpoint mp = (IJavaMethodEntryBreakpoint) bp;
	    xw.field("TYPE","METHODENTRY");
	    typed = true;
	    xw.begin("METHOD");
	    xw.field("NAME",mp.getMethodName());
	    xw.text(mp.getMethodSignature());
	    xw.end("METHOD");
	  }
	 else if (bp instanceof IJavaStratumLineBreakpoint) {
	    IJavaStratumLineBreakpoint slb = (IJavaStratumLineBreakpoint) bp;
	    xw.field("TYPE","STRATUMLINE");
	    typed = true;
	    xw.begin("SOURCE");
	    xw.field("NAME",slb.getSourceName());
	    xw.text(slb.getSourcePath());
	    xw.end("SOURCE");
	    xw.textElement("PATTERN",slb.getPattern());
	    if (slb.getStratum() != null) xw.textElement("STRATUM",slb.getStratum());
	  }
	 else if (bp instanceof IJavaTargetPatternBreakpoint) {
	    IJavaTargetPatternBreakpoint tb = (IJavaTargetPatternBreakpoint) bp;
	    xw.field("TYPE","TARGETPATTERN");
	    typed = true;
	    xw.field("SOURCE",tb.getSourceName());
	  }
	 else if (bp instanceof IJavaWatchpoint) {
	    IJavaWatchpoint wp = (IJavaWatchpoint) bp;
	    xw.field("TYPE","WATCHPOINT");
	    typed = true;
	    xw.field("FIELD",wp.getFieldName());
	  }

	 if (bp instanceof IJavaLineBreakpoint) {
	    IJavaLineBreakpoint lb = (IJavaLineBreakpoint) bp;
	    if (!typed) xw.field("TYPE","LINE");
	    if (lb.getCondition() != null) {
	       xw.begin("CONDITION");
	       xw.field("ENABLED",lb.isConditionEnabled());
	       xw.field("SUSPEND",lb.isConditionSuspendOnTrue());
	       xw.text(lb.getCondition());
	       xw.end("CONDITION");
	     }
	  }

	 if (mk != null) {
	    // TODO: Need to dump marker as components
	    // xw.textElement("MARKER",mk.toString());
	  }
	 IJavaObject [] iflt = bp.getInstanceFilters();
	 for (IJavaObject jo : iflt) {
	    xw.begin("FILTER");
	    xw.text(jo.toString());
	    xw.end();
	  }
       }
    }
   catch (Throwable e) {
      BedrockPlugin.logE("Breakpoint reporting problem: " + e,e);
    }
   finally {
      xw.end("BREAKPOINT");
    }
}



static String findFileForClass(String cls)
{
   if (cls == null) return null;

   BedrockPlugin bp = BedrockPlugin.getPlugin();
   BedrockProject bpp = bp.getProjectManager();
   File result = null;
   for (IProject ip : bpp.getOpenProjects()) {
      IJavaProject ijp = JavaCore.create(ip);
      if (ijp == null) continue;
      try {
	 IType ity = ijp.findType(cls);

	 if (ity == null && cls.indexOf("$") > 0) {
	    ity = ijp.findType(cls.replace('$','.'));
	    if (ity == null) {
	       int idx = cls.indexOf("$");
	       ity = ijp.findType(cls.substring(0,idx));
	     }
	  }
	 if (ity != null) {
	    IPath pt = ity.getPath();
	    File f = getFileForPath(pt,ip);
	    if (f.exists()) {
	       if (result == null) result = f;
	       else if (f.getName().endsWith(".java") &&
		     result.getName().endsWith(".class")) {
		  result = f;
		}
	     }
	  }
       }
      catch (JavaModelException e) { }
    }

   if (result != null) return result.getAbsolutePath();

   return null;
}




/********************************************************************************/
/*										*/
/*	Output methods for resources						*/
/*										*/
/********************************************************************************/

static void outputResource(IResource ir,IvyXmlWriter xw)
{
   if (ir == null) return;

   xw.begin("RESOURCE");
   if (ir.getName() != null) xw.field("NAME",ir.getName());
   if (ir.getFullPath() != null) {
      File fp = getFileForPath(ir.getFullPath(),null);
      xw.field("LOCATION",fp.getPath());
    }
   if (ir.getProject() != null) xw.field("PROJECT",ir.getProject().getName());
   BedrockUtil.fieldValue(xw,"TYPE",ir.getType(),resource_types);
   xw.end();
}



static int outputResource(IResourceDelta rd,IvyXmlWriter xw)
{
   if (rd == null) return 0;

   int ctr = 0;
   boolean out = true;
   if (out && rd.getFullPath() == null) out = false;
   if (out && !rd.getFullPath().toString().endsWith(".java")) out = false;
   if (out) {
      int fgs = rd.getFlags();
      if (fgs != 0) {
	 fgs &= ~IResourceDelta.MARKERS;	  // ignore markers
	 if (fgs == 0) out = false;
       }
    }

   if (out) {
      ++ctr;
      xw.begin("DELTA");
      BedrockUtil.fieldFlags(xw,"FLAGS",rd.getFlags(),delta_flags);
      BedrockUtil.fieldValue(xw,"KIND",rd.getKind(),delta_kinds);
      File fp = getFileForPath(rd.getFullPath(),null);
      xw.field("PATH",fp.getPath());

      outputResource(rd.getResource(),xw);

      for (IMarkerDelta md : rd.getMarkerDeltas()) {
	 xw.begin("MARKER");
	 xw.field("ID",md.getId());
	 BedrockUtil.fieldValue(xw,"KIND",md.getKind(),delta_kinds);
	 xw.field("TYPE",md.getType());
	 if (md.getKind() == IResourceDelta.REMOVED) {
	    Map<?,?> attrs = md.getAttributes();
	    for (Map.Entry<?,?> ent : attrs.entrySet()) {
	       xw.begin("ATTRIBUTE");
	       xw.field("NAME",ent.getKey());
	       xw.field("VALUE",ent.getValue());
	       xw.end();
	     }
	  }
	 else {
	    outputMarker(md.getMarker(),fp,xw);
	  }
	 xw.end("MARKER");
       }

      xw.end("DELTA");
    }

   for (IResourceDelta crd : rd.getAffectedChildren()) {
      ctr += outputResource(crd,xw);
    }

   return ctr;
}



/********************************************************************************/
/*										*/
/*	Output methods for processes						*/
/*										*/
/********************************************************************************/

static void outputProcess(IProcess rp,IvyXmlWriter xw,boolean showtarget)
{
   ILaunch lnch = rp.getLaunch();

   xw.begin("PROCESS");
   xw.field("PID",rp.hashCode());
   xw.field("LABEL",rp.getLabel());
   xw.field("MODE",lnch.getLaunchMode());
   xw.field("TYPE",rp.getAttribute(IProcess.ATTR_PROCESS_TYPE));
   IDebugTarget tgt = lnch.getDebugTarget();
   if (tgt != null) {
      try {
	 xw.field("NAME",tgt.getName());
       }
      catch (DebugException e) { }
    }
   if (rp.isTerminated()) {
      try {
	 xw.field("TERMINATED",true);
	 xw.field("EXITVALUE",rp.getExitValue());
       }
      catch (DebugException e) { }
    }
   else {
      xw.field("CANTERM",rp.canTerminate());
    }

   outputLaunch(lnch,xw);

   if (showtarget && tgt != null && tgt instanceof IJavaDebugTarget) {
      outputDebugTarget((IJavaDebugTarget) tgt,xw);
    }

   xw.end();
}



/********************************************************************************/
/*										*/
/*	Output methods for debug targets					*/
/*										*/
/********************************************************************************/

static void outputDebugTarget(IJavaDebugTarget tgt,IvyXmlWriter xw)
{
   xw.begin("TARGET");
   xw.field("ID",tgt.hashCode());
   try {
      xw.field("VM",tgt.getVMName());
      xw.field("VERSION",tgt.getVersion());
      xw.field("NAME",tgt.getName());
    }
   catch (DebugException e) { }

   if (tgt.isTerminated()) xw.field("TERMINATED",true);
   else {
      xw.field("CANTERM",tgt.canTerminate());
      xw.field("CANRESUME",tgt.canResume());
      xw.field("CANSUSPEND",tgt.canSuspend());
      xw.field("SUSPENDED",tgt.isSuspended());
    }
   if (tgt.isDisconnected()) xw.field("DISCONNECTED",true);
   else xw.field("CANDISCONNECT",tgt.canDisconnect());

   if (tgt.getProcess() != null) {
      xw.field("PID",tgt.getProcess().hashCode());
   }

   if (tgt.getLaunch() != null) {
      outputLaunch(tgt.getLaunch(),xw);
    }

   xw.end("TARGET");
}




/********************************************************************************/
/*										*/
/*	Output methods for threads						*/
/*										*/
/********************************************************************************/

static void outputThread(IJavaThread trd,IvyXmlWriter xw)
{
   if (trd == null) return;

   xw.begin("THREAD");

   try {
      xw.field("NAME",trd.getName());
    }
   catch (DebugException e) {
      BedrockPlugin.logE("Problem outputing thread",e);
    }

   try {
      xw.field("GROUP",trd.getThreadGroupName());
    }
   catch (DebugException e) {
      BedrockPlugin.logE("Problem outputing thread",e);
    }

   try {
      boolean fg = trd.hasStackFrames();
      if (fg) {
	 xw.field("STACK",true);
	 xw.field("FRAMES",trd.getFrameCount());
       }
      else xw.field("STACK",false);
    }
   catch (DebugException e) {
      BedrockPlugin.logE("Problem outputing thread",e);
    }

   try {
      xw.field("SYSTEM",trd.isSystemThread());
    }
   catch (DebugException e) {
      BedrockPlugin.logE("Problem outputing thread",e);
    }

   try {
      xw.field("DAEMON",trd.isDaemon());
    }
   catch (DebugException e) {
      BedrockPlugin.logE("Problem outputing thread",e);
    }

   if (trd.isTerminated()) xw.field("TERMINATED",true);
   else {
      xw.field("CANTERM",trd.canTerminate());
    }
   if (trd.isSuspended()) {
      xw.field("CANRESUME",trd.canResume());
      xw.field("CANSTEPIN",trd.canStepInto());
      xw.field("CANSTEPOVER",trd.canStepOver());
      xw.field("CANSTEPOUT",trd.canStepReturn());
      xw.field("SUSPENDED",trd.isSuspended());
    }
   else {
      xw.field("CANSUSPEND",trd.canSuspend());
    }

   xw.field("ID",trd.hashCode());
   // xw.field("TAG",trd);

   IDebugTarget tgt = trd.getDebugTarget();
   if (tgt != null) {
      IProcess ipro = tgt.getProcess();
      if (ipro != null) xw.field("PID",ipro.hashCode());
   }

   outputLaunch(trd.getLaunch(),xw);

   for (IBreakpoint ipt : trd.getBreakpoints()) {
      if (ipt instanceof IJavaBreakpoint) {
	 BedrockUtil.outputBreakpoint(ipt,xw);
       }
    }

   xw.end();
}



/********************************************************************************/
/*										*/
/*	Output methods for possible completions 				*/
/*										*/
/********************************************************************************/

static void outputCompletion(CompletionProposal cp,IvyXmlWriter xw)
{
   xw.begin("COMPLETION");
   fieldValue(xw,"ACCESSIBILITY",cp.getAccessibility(),accessibility_types);
   if (cp.isConstructor()) xw.field("CONSTRUCTOR",true);
   xw.field("TEXT",cp.getCompletion());
   xw.field("INDEX",cp.getCompletionLocation());
   xw.field("DECLKEY",cp.getDeclarationKey());
   switch (cp.getKind()) {
      case CompletionProposal.ANNOTATION_ATTRIBUTE_REF :
      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION :
      case CompletionProposal.FIELD_IMPORT :
      case CompletionProposal.FIELD_REF :
      case CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER :
      case CompletionProposal.METHOD_IMPORT :
      case CompletionProposal.METHOD_REF :
      case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER :
      case CompletionProposal.METHOD_DECLARATION :
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION :
	 xw.field("DECLSIGN",cp.getDeclarationSignature());
	 break;
      case CompletionProposal.PACKAGE_REF :
      case CompletionProposal.TYPE_IMPORT :
      case CompletionProposal.TYPE_REF :
	 xw.field("DOTNAME",cp.getDeclarationSignature());
	 break;
    }
   fieldFlags(xw,"ACCESS",cp.getFlags(),access_flags);
   xw.field("FLAGS",cp.getFlags());
   xw.field("KEY",cp.getKey());
   xw.field("NAME",cp.getName());
   xw.field("RELEVANCE",cp.getRelevance());
   xw.field("REPLACE_START",cp.getReplaceStart());
   xw.field("REPLACE_END",cp.getReplaceEnd());
   xw.field("SIGNATURE",cp.getSignature());
   xw.field("TOKEN_START",cp.getTokenStart());
   xw.field("TOKEN_END",cp.getTokenEnd());
   fieldValue(xw,"KIND",cp.getKind(),completion_types);
   if (cp instanceof ICompletionProposalExtension4) {
      ICompletionProposalExtension4 icp4 = (ICompletionProposalExtension4) cp;
      xw.field("AUTO",icp4.isAutoInsertable());
    }

   if (CompletionFlags.isStaticImport(cp.getAdditionalFlags())) xw.field("STATICIMPORT",true);

   if (cp.getKind() == CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER ||
	  cp.getKind() == CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER) {
      xw.field("RECEIVER_SIGN",cp.getReceiverSignature());
      xw.field("RECEIVER_START",cp.getReceiverStart());
      xw.field("RECEIVER_END",cp.getReceiverEnd());
    }
   xw.field("RCVR",cp.getReceiverSignature());

// xw.cdataElement("DESCRIPTION",cp.toString());

   CompletionProposal [] rq = cp.getRequiredProposals();
   if (rq != null) {
      xw.begin("REQUIRED");
      for (CompletionProposal xcp : rq) outputCompletion(xcp,xw);
      xw.end("REQUIRED");
    }

   xw.end("COMPLETION");
}




/********************************************************************************/
/*										*/
/*	Output methods for text edits						*/
/*										*/
/********************************************************************************/

static void outputTextEdit(TextEdit te,IvyXmlWriter xw)
{
   xw.begin("EDIT");
   xw.field("OFFSET",te.getOffset());
   xw.field("LENGTH",te.getLength());
   xw.field("INCEND",te.getInclusiveEnd());
   xw.field("EXCEND",te.getExclusiveEnd());
   xw.field("ID",te.hashCode());
   xw.field("COUNTER",++edit_counter);

   if (te instanceof CopyingRangeMarker) {
      xw.field("TYPE","COPYRANGE");
    }
   else if (te instanceof CopySourceEdit) {
      CopySourceEdit cse = (CopySourceEdit) te;
      xw.field("TYPE","COPYSOURCE");
      xw.field("TARGET",cse.getTargetEdit().hashCode());
    }
   else if (te instanceof CopyTargetEdit) {
      CopyTargetEdit cte = (CopyTargetEdit) te;
      xw.field("TYPE","COPYTARGET");
      xw.field("SOURCE",cte.getSourceEdit().hashCode());
      xw.field("SOURCEOFF",cte.getSourceEdit().getOffset());
      xw.field("SOURCELEN",cte.getSourceEdit().getLength());
    }
   else if (te instanceof DeleteEdit) {
      xw.field("TYPE","DELETE");
    }
   else if (te instanceof InsertEdit) {
      InsertEdit ite = (InsertEdit) te;
      xw.field("TYPE","INSERT");
      xw.cdataElement("TEXT",ite.getText());
    }
   else if (te instanceof MoveSourceEdit) {
      MoveSourceEdit mse = (MoveSourceEdit) te;
      xw.field("TYPE","MOVESOURCE");
      xw.field("TARGET",mse.getTargetEdit().hashCode());
    }
   else if (te instanceof MoveTargetEdit) {
      xw.field("TYPE","MOVETARGET");
    }
   else if (te instanceof MultiTextEdit) {
      xw.field("TYPE","MULTI");
    }
   else if (te instanceof RangeMarker) {
      xw.field("TYPE","RANGEMARKER");
    }
   else if (te instanceof ReplaceEdit) {
      ReplaceEdit rte = (ReplaceEdit) te;
      xw.field("TYPE","REPLACE");
      xw.cdataElement("TEXT",rte.getText());
    }
   else if (te instanceof UndoEdit) {
      xw.field("TYPE","UNDO");
    }

   if (te.hasChildren()) {
      for (TextEdit cte : te.getChildren()) {
	 outputTextEdit(cte,xw);
       }
    }
   xw.end("EDIT");
}




/********************************************************************************/
/*										*/
/*	Symbol output methods							*/
/*										*/
/********************************************************************************/

static void outputJavaElement(IJavaElement elt,IvyXmlWriter xw)
{
   outputJavaElement(elt,null,true,xw);
}



static void outputJavaElement(IJavaElement elt,Set<String> files,IvyXmlWriter xw)
{
   outputJavaElement(elt,files,true,xw);
}



static void outputJavaElement(IJavaElement elt,boolean children,IvyXmlWriter xw)
{
   outputJavaElement(elt,null,children,xw);
}



static void outputJavaElement(IJavaElement elt,Set<String> files,boolean children,IvyXmlWriter xw)
{
   outputJavaElementImpl(elt,files,children,xw);
}




private static void outputJavaElementImpl(IJavaElement elt,Set<String> files,boolean children,
					     IvyXmlWriter xw)
{
   if (elt == null) return;

   String close = null;

   switch (elt.getElementType()) {
      case IJavaElement.CLASS_FILE :
	 return;
      case IJavaElement.PACKAGE_FRAGMENT :
	 IOpenable opn = (IOpenable) elt;
	 if (!opn.isOpen()) {
	    try {
	       opn.open(null);
	     }
	    catch (JavaModelException e) {
	       BedrockPlugin.logE("Package framgent " + elt.getElementName() + " not open");
	       return ;
	     }
	  }
	 try {
	    outputNameDetails((IPackageFragment) elt,xw);
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	 IPackageFragmentRoot pfr = (IPackageFragmentRoot) elt;
	 try {
	    if (!pfr.isOpen() && pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
	       pfr.open(null);
	     }
	  }
	 catch (JavaModelException e) {
	    return ;
	  }
	 outputNameDetails(pfr,xw);
	 break;
      case IJavaElement.JAVA_PROJECT :
	 IJavaProject ijp = (IJavaProject) elt;
	 outputNameDetails(ijp,xw);
	 break;
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.TYPE_PARAMETER :
      default :
	 break;
      case IJavaElement.COMPILATION_UNIT :
	 IProject ip = elt.getJavaProject().getProject();
	 File f = getFileForPath(elt.getPath(),ip);
	 if (files != null && !files.contains(f.getPath()) && !files.contains(f.getAbsolutePath())) {
	    return;
	  }
	 xw.begin("FILE");
	 xw.textElement("PATH",f.getAbsolutePath());
	 String root = getRootForPath(elt.getPath(),ip);
	 if (root != null) xw.textElement("PATHROOT",root);
	 close = "FILE";
	 break;
      case IJavaElement.TYPE :
	 try {
	    outputNameDetails((IType) elt,xw);
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.FIELD :
	 try {
	    outputNameDetails((IField) elt,xw);
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.METHOD :
	 try {
	    outputNameDetails((IMethod) elt,xw);
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.INITIALIZER :
	 outputNameDetails((IInitializer) elt,xw);
	 break;
      case IJavaElement.PACKAGE_DECLARATION :
	 outputNameDetails((IPackageDeclaration) elt,xw);
	 break;
      case IJavaElement.LOCAL_VARIABLE :
	 outputNameDetails((ILocalVariable) elt,xw);
	 break;
    }

   if (children && elt instanceof IParent) {
      try {
	 for (IJavaElement c : ((IParent) elt).getChildren()) {
	    outputJavaElementImpl(c,files,children,xw);
	  }
       }
      catch (JavaModelException e) { }
    }

   if (close != null) xw.end(close);
}



private static void outputNameDetails(IType typ,IvyXmlWriter xw) throws JavaModelException
{
   String tnm = "Class";
   try {
      if (typ.isInterface()) tnm = "Interface";
      else if (typ.isEnum()) tnm = "Enum";
      else {
	 boolean check = false;
	 String supnm = typ.getFullyQualifiedName();
	 if (supnm.contains("Error") || supnm.contains("Exception") || supnm.contains("Throw"))
	    check = true;
	 else {
	    supnm = typ.getSuperclassName();
	    if (supnm != null) {
	       if (supnm.contains("Error") || supnm.contains("Exception") || supnm.contains("Throw"))
		  check = true;
	     }
	  }
	 if (check) {
	    try {
	       ITypeHierarchy ith = typ.newSupertypeHierarchy(null);
	       for (IType xtyp = typ; xtyp != null; xtyp = ith.getSuperclass(xtyp)) {
		  if (xtyp.getFullyQualifiedName().equals("java.lang.Throwable")) {
		     tnm = "Throwable";
		     break;
		   }
		}
	     }
	    catch (JavaModelException ex) { }
	  }
       }
    }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem looking up type " + typ + " " + typ.getClass());
    }

   outputSymbol(typ,tnm,typ.getFullyQualifiedParameterizedName(),typ.getKey(),xw);
}



private static void outputNameDetails(IField fld,IvyXmlWriter xw) throws JavaModelException
{
   String tnm = "Field";

   if (fld.isEnumConstant()) tnm = "EnumConstant";

   outputSymbol(fld,tnm,fld.getElementName(),fld.getKey(),xw);
}



private static void outputNameDetails(IMethod mthd,IvyXmlWriter xw) throws JavaModelException
{
   String tnm = "Function";
   if (mthd.isConstructor()) tnm = "Constructor";

   outputSymbol(mthd,tnm,mthd.getElementName(),mthd.getKey(),xw);
}



private static void outputNameDetails(IInitializer init,IvyXmlWriter xw)
{
   outputSymbol(init,"StaticInitializer","<clinit>",null,xw);
}



private static void outputNameDetails(IPackageDeclaration pkg,IvyXmlWriter xw)
{
   outputSymbol(pkg,"PackageDecl",pkg.getElementName(),null,xw);
}



private static void outputNameDetails(IPackageFragment pkg,IvyXmlWriter xw) throws JavaModelException
{
   //TODO: this excludes higher level packages
   if (pkg.containsJavaResources()) {
      // BedrockPlugin.logD("PACKAGE FRAG " + pkg.getElementName());
      outputSymbol(pkg,"Package",pkg.getElementName(),null,xw);
    }
}



private static void outputNameDetails(IPackageFragmentRoot pkg,IvyXmlWriter xw)
{
}



private static void outputNameDetails(ILocalVariable lcl,IvyXmlWriter xw)
{
   outputSymbol(lcl,"Local",lcl.getElementName(),null,xw);
}



private static void outputNameDetails(IJavaProject ijp,IvyXmlWriter xw)
{
   outputSymbol(ijp,"Project",ijp.getElementName(),ijp.getElementName() + "@",xw);
}



private static void outputSymbol(IJavaElement elt,String what,String nm,String key,IvyXmlWriter xw)
{
   if (what == null || nm == null) return;

   xw.begin("ITEM");
   xw.field("TYPE",what);
   xw.field("NAME",nm);
   xw.field("HANDLE",elt.getHandleIdentifier());

   xw.field("WORKING",(elt.getPrimaryElement() != elt));
   ICompilationUnit cu = (ICompilationUnit) elt.getAncestor(IJavaElement.COMPILATION_UNIT);
   if (cu != null) {
      xw.field("CUWORKING",cu.isWorkingCopy());
    }
   try {
      xw.field("KNOWN",elt.isStructureKnown());
    }
   catch (JavaModelException e) { }

   if (elt instanceof ISourceReference) {
      try {
	 ISourceRange rng = ((ISourceReference) elt).getSourceRange();
	 if (rng != null) {
	    xw.field("STARTOFFSET",rng.getOffset());
	    xw.field("ENDOFFSET",rng.getOffset() + rng.getLength());
	    xw.field("LENGTH",rng.getLength());
	  }
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem getting source range: " + e);
       }
    }

   if (elt instanceof ILocalVariable) {
      ILocalVariable lcl = (ILocalVariable) elt;
      xw.field("SIGNATURE",lcl.getTypeSignature());
    }

   if (elt instanceof IMember) {
      try {
	 IMember mem = ((IMember) elt);
	 int fgs = mem.getFlags();
	 if (mem.getParent() instanceof IType && !(elt instanceof IType)) {
	    IType par = (IType) mem.getParent();
	    if (par.isInterface()) {
	       if (elt instanceof IMethod) {
		  if ((fgs & Flags.AccDefaultMethod) == 0) fgs |= Flags.AccAbstract;
		}
	       fgs |= Flags.AccPublic;
	    }
	    xw.field("QNAME",par.getFullyQualifiedName() + "." + nm);
	  }
	 xw.field("FLAGS",fgs);
       }
      catch (JavaModelException e) { }
    }

   if (elt instanceof IPackageFragment || elt instanceof IType) {
      try {
	 JavadocUrl ju = new JavadocUrl(elt);
	 ju.run();
	 URL u = ju.getResult();
	 if (u != null) {
	    xw.field("JAVADOC",u.toString());
	  }
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem getting javadoc element",t);
       }
    }

   xw.field("SOURCE","USERSOURCE");
   if (key != null) xw.field("KEY",key);

   boolean havepath = false;
   for (IJavaElement pe = elt.getParent(); pe != null; pe = pe.getParent()) {
      if (pe.getElementType() == IJavaElement.COMPILATION_UNIT) {
	 IProject ip = elt.getJavaProject().getProject();
	 File f = BedrockUtil.getFileForPath(elt.getPath(),ip);
	 xw.field("PATH",f.getAbsolutePath());
	 havepath = true;
	 break;
       }
    }
   IJavaProject ijp = elt.getJavaProject();
   if (ijp != null) xw.field("PROJECT",ijp.getProject().getName());
   IPath p = elt.getPath();
   if (p != null) {
      xw.field("XPATH",p);
      if (!havepath) {
	 IProject ip = elt.getJavaProject().getProject();
	 File f = getFileForPath(elt.getPath(),ip);
	 xw.field("PATH",f.getAbsolutePath());
       }
    }

   if (elt instanceof IField) {
      IField ifld = (IField) elt;
      try {
	 xw.field("RETURNTYPE",ifld.getTypeSignature());
       }
      catch (JavaModelException e) { }
    }

   if (elt instanceof IMethod) {
      IMethod m = (IMethod) elt;
      try {
	 xw.field("RESOLVED",m.isResolved());
	 ISourceRange rng = m.getNameRange();
	 if (rng != null) {
	    xw.field("NAMEOFFSET",rng.getOffset());
	    xw.field("NAMELENGTH",rng.getLength());
	  }
	 xw.field("RETURNTYPE",m.getReturnType());
	 StringBuffer pbuf = new StringBuffer();
	 pbuf.append("(");
	 String [] ptys = m.getParameterTypes();
	 for (int i = 0; i < ptys.length; ++i) {
	    pbuf.append(ptys[i]);
	  }
	 pbuf.append(")");
	 xw.field("PARAMETERS",pbuf.toString());
       }
      catch (JavaModelException e) { }
    }

   // TODO: output parameters as separate elements with type and name

   if (elt instanceof IAnnotatable) {
      IAnnotatable ann = (IAnnotatable) elt;
      try {
	 IAnnotation [] ans = ann.getAnnotations();
	 for (IAnnotation an : ans) {
	    xw.begin("ANNOTATION");
	    xw.field("NAME",an.getElementName());
	    xw.field("COUNT",an.getOccurrenceCount());
	    try {
	       for (IMemberValuePair mvp : an.getMemberValuePairs()) {
		  xw.begin("VALUE");
		  xw.field("NAME",mvp.getMemberName());
		  if (mvp.getValue() != null) xw.field("VALUE",mvp.getValue().toString());
		  xw.field("KIND",mvp.getValueKind());
		  xw.end("VALUE");
		}
	     }
	    catch (JavaModelException e) { }
	    xw.end("ANNOTATION");
	  }
       }
      catch (JavaModelException e) { }
    }

   xw.end("ITEM");
}



private static class JavadocUrl implements Runnable {

   private IJavaElement java_element;
   private URL result_url;

   JavadocUrl(IJavaElement elt) {
      java_element = elt;
      result_url = null;
    }

   URL getResult()			{ return result_url; }

   @Override public void run() {
      try {
	 result_url = JavaUI.getJavadocBaseLocation(java_element);
       }
      catch (Throwable e) { }
    }

}	// end of inner class JavadocUrl



/********************************************************************************/
/*										*/
/*	Search Match output							*/
/*										*/
/********************************************************************************/

static void outputSearchMatch(SearchMatch mat,IvyXmlWriter xw)
{
   xw.begin("MATCH");
   xw.field("OFFSET",mat.getOffset());
   xw.field("LENGTH",mat.getLength());
   xw.field("STARTOFFSET",mat.getOffset());
   xw.field("ENDOFFSET",mat.getOffset() + mat.getLength());
   IResource irc = mat.getResource();
   if (irc != null) {
      File f = mat.getResource().getLocation().toFile();
      switch (irc.getType()) {
	 case IResource.FILE:
	    xw.field("FILE",f.toString());
	    break;
	 case IResource.PROJECT :
	    xw.field("PROJECT",f.toString());
	    break;
	 case IResource.FOLDER :
	    xw.field("FOLDER",f.toString());
	    break;
	 case IResource.ROOT :
	    xw.field("ROOT",f.toString());
	    break;
       }
    }
   xw.field("ACCURACY",mat.getAccuracy());
   xw.field("EQUIV",mat.isEquivalent());
   xw.field("ERASURE",mat.isErasure());
   xw.field("EXACT",mat.isExact());
   xw.field("IMPLICIT",mat.isImplicit());
   xw.field("INDOCCMMT",mat.isInsideDocComment());
   xw.field("RAW",mat.isRaw());
   Object o = mat.getElement();
   BedrockPlugin.logD("MATCH ELEMENT " + o);
   if (o instanceof IJavaElement) {
      IJavaElement nelt = (IJavaElement) o;
      outputJavaElement(nelt,false,xw);
    }
   xw.end("MATCH");
}



/********************************************************************************/
/*										*/
/*	Methods to output a type hierarchy					*/
/*										*/
/********************************************************************************/

static void outputTypeHierarchy(ITypeHierarchy th,IvyXmlWriter xw)
{
   xw.begin("HIERARCHY");
   IType [] typs = th.getAllTypes();
   for (IType typ : typs) {
      xw.begin("TYPE");
      try {
	 xw.field("NAME",typ.getFullyQualifiedName());
	 xw.field("QNAME",typ.getTypeQualifiedName());
	 xw.field("PNAME",typ.getFullyQualifiedParameterizedName());
	 if (typ.isClass()) xw.field("KIND","CLASS");
	 else if (typ.isEnum()) xw.field("KIND","ENUM");
	 else if (typ.isInterface()) xw.field("KIND","INTERFACE");
	 xw.field("LOCAL",typ.isLocal());
	 xw.field("MEMBER",typ.isMember());
	 xw.field("KEY",typ.getKey());
	 IType [] subs = th.getAllSubtypes(typ);
	 for (IType styp : subs) {
	    xw.begin("SUBTYPE");
	    xw.field("NAME",styp.getFullyQualifiedName());
	    xw.field("KEY",styp.getKey());
	    xw.end("SUBTYPE");
	  }
	 IType [] sups = th.getAllSuperclasses(typ);
	 for (IType styp : sups) {
	    xw.begin("SUPERCLASS");
	    xw.field("NAME",styp.getFullyQualifiedName());
	    xw.field("KEY",styp.getKey());
	    xw.end("SUPERCLASS");
	  }
	 sups = th.getAllSuperInterfaces(typ);
	 for (IType styp : sups) {
	    xw.begin("SUPERIFACE");
	    xw.field("NAME",styp.getFullyQualifiedName());
	    xw.field("KEY",styp.getKey());
	    xw.end("SUPERIFACE");
	  }
	 sups = th.getAllSupertypes(typ);
	 for (IType styp : sups) {
	    xw.begin("SUPERTYPE");
	    xw.field("NAME",styp.getFullyQualifiedName());
	    xw.field("KEY",styp.getKey());
	    xw.end("SUPERTYPE");
	  }
	 sups = th.getExtendingInterfaces(typ);
	 for (IType styp : sups) {
	    xw.begin("EXTENDIFACE");
	    xw.field("NAME",styp.getFullyQualifiedName());
	    xw.field("KEY",styp.getKey());
	    xw.end("EXTENDIFACE");
	  }
	 sups = th.getImplementingClasses(typ);
	 for (IType styp : sups) {
	    xw.begin("IMPLEMENTOR");
	    xw.field("NAME",styp.getFullyQualifiedName());
	    xw.field("KEY",styp.getKey());
	    xw.end("IMPLEMENTOR");
	  }
       }
      catch (JavaModelException e) { }

      xw.end("TYPE");
    }
   xw.end("HIERARCHY");
}




/********************************************************************************/
/*										*/
/*	Methods to output launch configuration					*/
/*										*/
/********************************************************************************/

static void outputLaunch(ILaunch ln,IvyXmlWriter xw)
{
   xw.begin("LAUNCH");
   xw.field("MODE",ln.getLaunchMode());
   xw.field("ID",ln.hashCode());
   ILaunchConfiguration cfg = ln.getLaunchConfiguration();
   if (cfg != null && cfg.isWorkingCopy())
      cfg = ((ILaunchConfigurationWorkingCopy) cfg).getOriginal();
   if (cfg != null) xw.field("CID",getId(cfg));
   xw.end("LAUNCH");
}



static void outputLaunch(ILaunchConfiguration cfg,IvyXmlWriter xw)
{
   String id;
   Set<?> modes;
   Map<?,?> attrs;
   ILaunchConfigurationType typ;
   ILaunchConfiguration orig = null;

   if (cfg == null) return;
   if (cfg.isWorkingCopy()) {
      orig = ((ILaunchConfigurationWorkingCopy) cfg).getOriginal();
    }

   id = getId(cfg);

   xw.begin("CONFIGURATION");
   xw.field("ID",id);

   try {
      modes = cfg.getModes();
      attrs = cfg.getAttributes();
      typ = cfg.getType();
    }
   catch (CoreException e) {		// if deleted, these fail
      xw.end("CONFIGURATION");
      return;
    }

   xw.field("NAME",cfg.getName());

   if (cfg.isWorkingCopy()) xw.field("WORKING",true);
   if (orig != null) xw.field("ORIGID",getId(orig));

   for (Iterator<?> it = modes.iterator(); it.hasNext(); ) {
      String md = it.next().toString();
      xw.field(md,true);
    }

   for (Iterator<?> it = attrs.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<?,?> ent =  (Map.Entry<?,?>) it.next();
      if (ent.getKey().toString().equals(BEDROCK_LAUNCH_IGNORE_PROP)) {
	 xw.field("IGNORE",true);
       }
    }

   for (Iterator<?> it = attrs.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<?,?> ent =  (Map.Entry<?,?>) it.next();
      Object val = ent.getValue();
      String key = ent.getKey().toString();
      key = BedrockRuntime.getExternalPropertyName(key);
      if (key == null || val == null) continue;
      if (val instanceof ArrayList<?>) {
	 ArrayList<?> al = (ArrayList<?>) val;
	 for (int j = 0; j < al.size(); ++j) {
	    Object v1 = al.get(j);
	    xw.begin("ATTRIBUTE");
	    xw.field("NAME",key);
	    xw.field("INDEX",j+1);
	    xw.field("TYPE",v1.getClass().getName());
	    xw.cdata(v1.toString());
	    xw.end("ATTRIBUTE");
	  }
       }
      else {
	 xw.begin("ATTRIBUTE");
	 xw.field("NAME",key);
	 xw.field("TYPE",val.getClass().getName());
	 xw.cdata(val.toString());
	 xw.end("ATTRIBUTE");
       }
    }

   xw.begin("TYPE");
   xw.field("NAME",typ.getName());
   xw.field("ID",typ.getIdentifier());
   if (typ.getCategory() != null) xw.field("CATEGORY",typ.getCategory());
   xw.end("TYPE");

   xw.end("CONFIGURATION");
}



static String getId(ILaunchConfiguration li)
{
   String atr = null;
   try {
      atr = li.getAttribute(BEDROCK_LAUNCH_ID_PROP,atr);
    }
   catch (CoreException e) { }

   if (atr == null) {
      atr = launch_ids.get(li);
      if (atr == null) {
	 atr = Integer.toString(random_gen.nextInt(Integer.MAX_VALUE));
	 launch_ids.put(li,atr);
       }
    }
   else {
      launch_ids.put(li,atr);
    }

   return atr;
}



/********************************************************************************/
/*										*/
/*	Methods to output a stack frame 					*/
/*										*/
/********************************************************************************/

static void outputStackFrame(IJavaStackFrame jsf,int lvl,int vdepth,int arraysz,IvyXmlWriter xw)
	throws DebugException
{
   xw.begin("STACKFRAME");
   xw.field("NAME", jsf.getName());
   xw.field("ID", jsf.hashCode());
   xw.field("LINENO", jsf.getLineNumber());
   xw.field("CLASS",jsf.getDeclaringTypeName());
   xw.field("METHOD",jsf.getDeclaringTypeName() + "." + jsf.getMethodName());
   try {
      xw.field("RECEIVER",jsf.getReceivingTypeName());
    }
   catch (DebugException e) { }
   if (jsf.isConstructor()) xw.field("CONSTRUCTOR",true);
   if (jsf.isNative()) xw.field("NATIVE",true);
   if (jsf.isStaticInitializer()) xw.field("INITIALIZER",true);
   if (jsf.isVarArgs()) xw.field("VARARGS",true);
   if (jsf.isStatic()) xw.field("STATIC",true);
   if (jsf.isSynthetic()) xw.field("SYNTHETIC",true);
   if (!jsf.wereLocalsAvailable()) xw.field("NOLOCALS",true);
   xw.field("SIGNATURE",jsf.getSignature());
   if (lvl >= 0) xw.field("LEVEL",lvl);

   Object resource = null;
   ISourceLocator loc = jsf.getLaunch().getSourceLocator();
   resource = loc.getSourceElement(jsf);
   if (resource != null) {
      if (resource instanceof IClassFile) {
	 IClassFile cf = (IClassFile) resource;
	 xw.field("FILETYPE","CLASSFILE");
	 try {
	    String fnm = cf.findPrimaryType().getFullyQualifiedParameterizedName();
	    xw.field("FILE",fnm);
	    xw.field("FILEPATH",cf.getPath().toOSString());
	    if (cf.getSource() != null) {
	       ISourceRange rng = cf.getSourceRange();
	       byte [] data = cf.getSource().getBytes();
	       xw.field("SOURCELEN",rng.getLength());
	       xw.field("SOURCEOFF",rng.getOffset());
	       if (!sources_sent.contains(fnm)) {
		  sources_sent.add(fnm);
		  xw.bytesElement("SOURCE",data);
		}
	     }
	  }
	 catch (JavaModelException e) { }
       }
      else if (resource instanceof IFile) {
	 IFile f = (IFile) resource;
	 xw.field("FILE",f.getLocation().makeAbsolute().toOSString());
	 xw.field("FILETYPE","JAVAFILE");
       }
    }

   try {
      for (IVariable var : jsf.getVariables()) {
	 IJavaVariable ivj = (IJavaVariable) var;
	 outputValue(ivj.getValue(),ivj,null,vdepth,arraysz,xw);
       }
    }
   catch (DebugException e) { } 	// native methods, etc don't have variables

   try {
      List<?> l = jsf.getArgumentTypeNames();
      for (Iterator<?> it = l.iterator(); it.hasNext(); ) {
	 String nm = (String) it.next();
	 xw.begin("ARG");
	 xw.field("TYPE",nm);
	 xw.end("ARG");
       }
    }
   catch (DebugException e) { }
   xw.end("STACKFRAME");
}



/********************************************************************************/
/*										*/
/*	Methods to output a value						*/
/*										*/
/********************************************************************************/

static void outputValue(IValue val,IJavaVariable var,String name,int lvls,int arraysz,
      IvyXmlWriter xw)
{
   outputValue(val,var,name,lvls,arraysz,null,xw);
}


static void outputValue(IValue val,IJavaVariable var,String name,int lvls,int arraysz,
      HashSet<String> donestatics,IvyXmlWriter xw)
{
   if (donestatics == null) donestatics = new HashSet<>();

   try {
      if (name == null && var != null) {
	 name = var.getName();
	 if (name.startsWith("no method return value")) {
	    BedrockPlugin.logD("VALUE without name " + name + " :" + var + ": " + val);
	    return;
	  }
       }

      String dtyp = null;
      String typ = val.getReferenceTypeName();
      String txt = val.getValueString();
      boolean lcl = false;
      boolean stat = false;

      if (var != null && var instanceof IJavaFieldVariable) {
	 IJavaFieldVariable jfv = (IJavaFieldVariable) var;
	 dtyp = jfv.getDeclaringType().getName();
	 stat = jfv.isStatic();
       }
      if (var != null) lcl = var.isLocal();

      if (dtyp != null && donestatics.contains(dtyp) && stat) {
	 BedrockPlugin.logD("SKIP STATIC " + var);
	 return;
       }

      xw.begin("VALUE");
      if (name != null) xw.field("NAME",name);
      if (dtyp != null) xw.field("DECLTYPE",dtyp);
      if (lcl) xw.field("LOCAL",lcl);
      if (stat) xw.field("STATIC",stat);
      xw.field("TYPE",typ);

      try {
	 if (val instanceof IJavaArray) {
	    IJavaArray arr = (IJavaArray) val;
	    int len = arr.getLength();
	    xw.field("KIND","ARRAY");
	    xw.field("LENGTH",len);
	    xw.field("HASVARS",len > 0);
	    int sz = arraysz;
	    if (sz == 0) sz = 100;
	    else if (sz < 0) sz = len+1;
	    if (len <= sz && lvls > 0) {
	       for (int i = 0; i < len; ++i) {
		  try {
		     outputValue(arr.getValue(i),null,"[" + i + "]",lvls-1,arraysz,donestatics,xw);
		   }
		  catch (DebugException e) { break; }
		}
	     }
	    else if (lvls > 0) {
	       // TODO: Need to handle large arrays
	       for (int i = 0; i < sz; ++i) {
		  try {
		     outputValue(arr.getValue(i),null,"[" + i + "]",lvls-1,arraysz,donestatics,xw);
		   }
		  catch (DebugException e) { break; }
		}
	     }
	  }
	 else if (val instanceof IJavaPrimitiveValue) {
	    xw.field("KIND","PRIMITIVE");
	  }
	 else if (val instanceof IJavaClassObject) {
	    IJavaClassObject cobj = (IJavaClassObject) val;
	    IJavaType ctyp = cobj.getInstanceType();
	    xw.field("KIND","CLASS");
	    xw.field("TYPENAME",ctyp.getName());
	  }
	 else if (typ.equals("Ljava/lang/String;") || typ.equals("java.lang.String")) {
	    xw.field("KIND","STRING");
            if (txt.contains("\\")) {
               String txt0 = IvyFormat.getLiteralValue(txt);
               BedrockPlugin.logD("Convert string " + txt + " " + txt0);
               txt = txt0;
             }
	    // txt is not quite right here if the string is complex
	    // e.g \UD83D\UDE30" gets mapped to "\u07D8\u00B0"
	  }

	 else if (val instanceof IJavaObject) {
	    IJavaObject obj = (IJavaObject) val;
	    IVariable [] vars = obj.getVariables();
	    xw.field("KIND","OBJECT");
	    // if (obj.getJavaType() != null) xw.field("OBJTYPE",obj.getJavaType().getName());
	    xw.field("HASVARS",vars.length > 0);
	    if (lvls > 0) {
	       for (IVariable nvar : vars) {
		  try {
		     if (nvar instanceof IJavaVariable) {
			IJavaVariable nvj = (IJavaVariable) nvar;
			outputValue(nvj.getValue(),nvj,null,lvls-1,arraysz,donestatics,xw);
		      }
		     else outputValue(nvar.getValue(),null,nvar.getName(),lvls-1,arraysz,donestatics,xw);
		   }
		  catch (DebugException e) { }
		}
	     }
	    if (dtyp != null) donestatics.add(dtyp);
	  }
	 if (txt.length() >= MAX_VALUE_SIZE) {
	    txt = txt.substring(0,MAX_VALUE_SIZE) + "...";
	  }
	 xw.cdataElement("DESCRIPTION",txt);
       }
      finally {
	 xw.end("VALUE");
       }
    }
   catch (DebugException e) {
      BedrockPlugin.logE("Problem accessing value: " + e,e);
    }
}








static void outputValue(IEvaluationResult rslt,int lvl,int arraysz,IvyXmlWriter xw)
{
   if (lvl <= 0) lvl = 2;

   xw.begin("EVAL");

   DebugException ex = rslt.getException();

   if (ex != null) xw.field("STATUS","EXCEPTION");
   else if (rslt.hasErrors()) xw.field("STATUS","ERROR");
   else if (rslt.isTerminated()) xw.field("STATUS","TERMINATED");    // requires newer eclipse
   else xw.field("STATUS","OK");

   if (ex != null) {
      String cause = ex.toString();
      for (Throwable ex1 = ex.getCause(); ex1 != null; ex1 = ex1.getCause()) {
	 cause += " caused by " + ex1.toString();
       }
      xw.textElement("EXCEPTION",cause);
      BedrockPlugin.logE("Problem with evaluation",ex);
   }
   else if (rslt.hasErrors()) {
      for (String s : rslt.getErrorMessages()) {
	 xw.textElement("ERROR",s);
       }
    }

   IJavaValue val = rslt.getValue();
   if (val != null) {
      outputValue(val,null,rslt.getSnippet(),lvl,arraysz,xw);
    }

   xw.cdataElement("EXPR",rslt.getSnippet());

   xw.end("EVAL");
}



/********************************************************************************/
/*										*/
/*	Refactoring output methods						*/
/*										*/
/********************************************************************************/

static void outputStatus(RefactoringStatus sts,IvyXmlWriter xw)
{
   xw.begin("STATUS");
   xw.field("SEVERITY",sts.getSeverity());
   for (RefactoringStatusEntry rse : sts.getEntries()) {
      xw.begin("ENTRY");
      xw.field("CODE",rse.getCode());
      xw.field("MESSAGE",rse.getMessage());
      xw.field("SEVERITY",rse.getSeverity());
      xw.end("ENTRY");
    }
   xw.end("STATUS");
}



static void outputChange(Change chng,IvyXmlWriter xw)
{
   xw.begin("CHANGE");
   xw.field("NAME",chng.getName());

   BedrockPlugin.logD("CHANGE: " + chng + " " + chng.getClass().getName());

   if (chng instanceof CompositeChange) {
      CompositeChange cc = (CompositeChange) chng;
      xw.field("TYPE","COMPOSITE");
      for (Change c : cc.getChildren()) outputChange(c,xw);
    }
   else if (chng instanceof NullChange) {
      xw.field("TYPE","NULL");
    }
   else if (chng instanceof ResourceChange) {
      ResourceChange rc = (ResourceChange) chng;
      if (chng instanceof DeleteResourceChange) {
	 xw.field("TYPE","DELETERESOURCE");
       }
      else if (chng instanceof MoveResourceChange) {
	 xw.field("TYPE","MOVERESOURCE");
       }
      else if (chng instanceof RenameResourceChange) {
	 RenameResourceChange rrc = (RenameResourceChange) chng;
	 xw.field("TYPE","RENAMERESOURCE");
	 xw.field("NEWNAME",rrc.getNewName());
       }
      else {
	 String typ = rc.getName();
	 Pattern p1 = Pattern.compile("Rename compilation unit '([^']*)' to '([^']*)'");
	 Matcher m1 = p1.matcher(typ);
	 if (m1.matches()) {
	    xw.field("TYPE","RENAMERESOURCE");
	    xw.field("OLDNAME",m1.group(1));
	    xw.field("NEWNAME",m1.group(2));
	  }
	 else {
	    BedrockPlugin.logD("UNKNOWN RESOURCE CHANGE: " + typ + " " + chng.getClass().getName());
	  }
       }
      Object o = rc.getModifiedElement();
      if (o instanceof IResource) {
	 IResource ir = (IResource) o;
	 BedrockUtil.outputResource(ir,xw);
       }
      else if (o instanceof ICompilationUnit) {
	 IResource ir = ((ICompilationUnit) o).getResource();
	 BedrockUtil.outputResource(ir,xw);
       }
    }
   else if (chng instanceof TextEditBasedChange) {
      TextEditBasedChange tec = (TextEditBasedChange) chng;
      xw.field("TYPE","EDIT");
      xw.field("TEXTTYPE",tec.getTextType());
      for (TextEditBasedChangeGroup cg : tec.getChangeGroups()) {
	 for (TextEdit te : cg.getTextEdits()) {
	    BedrockUtil.outputTextEdit(te,xw);
	  }
       }
    }
   else if (chng instanceof UndoTextFileChange) {
      xw.field("TYPE","UNDO");
    }
   else {
      xw.field("CLASSTYPE",chng.getClass().getName());
    }

   Object [] aff = chng.getAffectedObjects();
   if (aff != null) {
      for (Object o : aff) {
	 if (o instanceof IResource) {
	    IResource ir = (IResource) o;
	    BedrockUtil.outputResource(ir,xw);
	  }
	 else if (o instanceof ICompilationUnit) {
	    IResource ir = ((ICompilationUnit) o).getResource();
	    BedrockUtil.outputResource(ir,xw);
	  }
	 else {
	    BedrockPlugin.logD("UNKNOWN CHANGE " + chng.getName() + " OBJECT " + o.getClass() + " " + o);
	  }
       }
    }

   xw.end("CHANGE");
}



/********************************************************************************/
/*										*/
/*	File management routines						*/
/*										*/
/********************************************************************************/

static File getFileForPath(IPath p,IProject proj)
{
   if (p == null) return null;

   if (!p.isAbsolute()) {
      String seg0 = p.segment(0);
      IPath pfx = JavaCore.getClasspathVariable(seg0);
      if (pfx != null) {
	 IPath p1 = pfx.append(p.removeFirstSegments(1));
	 File f1 = getFileForPath(p1.toFile(),proj);
	 if (f1.exists()) return f1;
       }
    }

   return getFileForPath(p.toFile(),proj);
}



static File getFileForPath(File f,IProject proj)
{
   if (!f.exists() && proj == null) {
      Stack<File> pars = new Stack<>();
      for (File f1 = f; f1 != null; f1 = f1.getParentFile()) pars.push(f1);
      pars.pop();		// /
      String pnm = pars.pop().getName();
      try {
	 BedrockPlugin bp = BedrockPlugin.getPlugin();
	 BedrockProject bpp = bp.getProjectManager();
	 proj = bpp.findProject(pnm);
       }
      catch (BedrockException e) { }
    }

   if (!f.exists() && proj != null && proj.getLocation() != null) {
      Stack<File> pars = new Stack<>();
      for (File f1 = f; f1 != null; f1 = f1.getParentFile()) pars.push(f1);
      if (pars.size() >= 3) {
	 pars.pop();		   // /
	 pars.pop();		   // /project
	 File f0 = proj.getLocation().toFile();
	 while (!pars.empty()) {
	    f0 = new File(f0,pars.pop().getName());
	  }
	 if (f0.exists()) f = f0;
       }
    }

   if (!f.exists() && proj != null) {
      Stack<File> pars = new Stack<>();
      for (File f1 = f; f1 != null; f1 = f1.getParentFile()) pars.push(f1);
      if (pars.size() >= 3) {
	 pars.pop();		   // /
	 pars.pop();		   // /project
	 IFolder lnk = proj.getFolder(pars.pop().getName());
	 if (lnk != null && lnk.getLocation() != null) {
	    File f0 = lnk.getLocation().toFile();
	    while (!pars.empty()) {
	       f0 = new File(f0,pars.pop().getName());
	     }
	    if (f0.exists()) f = f0;
	  }
       }
      else if (pars.size() == 2) {
	 IPath ip = proj.getLocation();
	 File f1 = ip.toFile();
	 if (f1.exists()) f = f1;
       }
    }

   if (!f.exists()) {
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot wr = ws.getRoot();
      File f0 = new File(wr.getLocation().toOSString());
      Stack<File> pars = new Stack<>();
      for (File f1 = f; f1 != null; f1 = f1.getParentFile()) pars.push(f1);
      pars.pop();		// /
      while (!pars.empty()) {
	 f0 = new File(f0,pars.pop().getName());
       }
      if (f0.exists()) f = f0;
    }

   return f;
}



static String getRootForPath(IPath p,IProject proj)
{
   if (p == null || p.toFile().exists() || proj == null) return null;
   String [] segs = p.segments();
   if (segs.length < 2) return null;
   return segs[1];
}



}	// end of class BedrockUtil



/* end of BedrockUtil.java */





















