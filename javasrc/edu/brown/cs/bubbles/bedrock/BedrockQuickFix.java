/********************************************************************************/
/*										*/
/*		BedrockQuickFix.java						*/
/*										*/
/*	Tie into Eclipse's Quick Fix mechanism from bubbles                     */
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

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.progress.WorkbenchJob;
import org.w3c.dom.Element;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class BedrockQuickFix implements BedrockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin		our_plugin;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockQuickFix(BedrockPlugin bp)
{
   our_plugin = bp;
}



/********************************************************************************/
/*										*/
/*	Action routines 							*/
/*										*/
/********************************************************************************/

void handleQuickFix(String proj,String bid,String file,int off,int len,List<Element> problems,
		       IvyXmlWriter xw)
	throws BedrockException
{
   CompilationUnit cu = our_plugin.getAST(bid,proj,file);
   BedrockPlugin.logD("QUICK FIX FOR " + bid + " " + proj + " AST: " + cu);

   ICompilationUnit icu = our_plugin.getCompilationUnit(proj,file);

   List<CategorizedProblem> probs = getProblems(cu,problems);
   if (probs == null || probs.size() == 0) throw new BedrockException("Problem not found");
   if (off < 0) {
      CategorizedProblem p = probs.get(0);
      off = p.getSourceStart();
      len = 0;
    }

   FixContext fc = new FixContext(cu,icu,off,len);

   ProblemContext [] pcs = new ProblemContext[probs.size()];
   for (int i = 0; i < probs.size(); ++i) {
      pcs[i] = new ProblemContext(probs.get(i));
    }

   if (BedrockApplication.getDisplay() == null) throw new BedrockException("No display");
   BedrockPlugin.logD("QUICK FIX PROBLEMS " + pcs.length);
   
   try {
      WJob wj = new WJob(fc,pcs,xw);
      wj.schedule();
      wj.join();
      IStatus rslt = wj.getResult();
      Throwable t = rslt.getException();
      if (t != null) {
	 BedrockPlugin.logE("Problem running quick fix: " + fc + " " + pcs + t,t);
//	 throw new BedrockException("Problem running quick fix: " + t,t);
       }
    }
   catch (InterruptedException e) { }
   catch (Throwable t) {
      throw new BedrockException("Problem with quick fix",t);
    }
}


private class WJob extends WorkbenchJob {

   FixContext fix_context;
   ProblemContext [] problem_contexts;
   IvyXmlWriter xml_writer;

   WJob(FixContext fc,ProblemContext [] pcs,IvyXmlWriter xw) {
      super(BedrockApplication.getDisplay(),"quickfixer");
      fix_context = fc;
      problem_contexts = pcs;
      xml_writer = xw;
    }

   @Override public IStatus runInUIThread(IProgressMonitor m) {

      try {
	 for (IQuickFixProcessor qp : getFixers()) {
	    BedrockPlugin.logD("Quick fix proecessor " + qp);
	    try {
	       IJavaCompletionProposal [] props = qp.getCorrections(fix_context,problem_contexts);
               if (props == null) BedrockPlugin.logD("No completion proposals");
	       else BedrockPlugin.logD("Returned " + props.length + " completion proposals");
	       outputProposals(props,xml_writer);
	     }
            catch (OperationCanceledException e) {
               BedrockPlugin.logE("Operation completion canceled",e);
             }
	    catch (CoreException e) {
	       BedrockPlugin.logE("Problem running completion proposal",e);
	     }
	  }
	 for (IQuickAssistProcessor qp : getAssisters()) {
	    try {
	       IJavaCompletionProposal [] props = qp.getAssists(fix_context,problem_contexts);
               if (props == null) BedrockPlugin.logD("No quick assist proposals");
	       else BedrockPlugin.logD("Returned " + props.length + " quick assist proposals");
	       outputProposals(props,xml_writer);
	     }
            catch (OperationCanceledException e) {
               BedrockPlugin.logD("Operation quick assist canceled " + e);
             }
	    catch (CoreException e) {
	       BedrockPlugin.logE("Problem running quick assist proposal",e);
	     }
	  }
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem with quick fix: " + t,t);
//	 return new Status(IStatus.ERROR,"BEDROCK","Problem with quick fix",t);
	 return Status.OK_STATUS;
       }

      return Status.OK_STATUS;
    }
   
   private List<IQuickFixProcessor> getFixers() {
      List<IQuickFixProcessor> rslt = new ArrayList<>();
      try {
	 rslt.add(new org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor());
	 rslt.add(new org.eclipse.jdt.internal.ui.text.spelling.WordQuickFixProcessor());
       }
      catch (NoClassDefFoundError e) {
         BedrockPlugin.logE("Problem loading quick fixes",e);
       }
      return rslt;
    }
   
   private List<IQuickAssistProcessor> getAssisters() {
      List<IQuickAssistProcessor> rslt = new ArrayList<>();
      try {
	 rslt.add(new org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor());
	 rslt.add(new org.eclipse.jdt.internal.ui.text.correction.QuickTemplateProcessor());
       }
      catch (NoClassDefFoundError e) {
	 BedrockPlugin.logE("Problem loading quick assists",e);
       }
      return rslt;
    }

}	// end of inner class WJob





/********************************************************************************/
/*										*/
/*	Relevant problems							*/
/*										*/
/********************************************************************************/

private List<CategorizedProblem> getProblems(CompilationUnit cu,List<Element> xmls)
{
   IProblem [] probs = cu.getProblems();
   List<CategorizedProblem> pbs = new ArrayList<CategorizedProblem>();

   for (Element e : xmls) {
      int mid = IvyXml.getAttrInt(e,"MSGID",0);
      if (mid == 0) continue;
      int sln = IvyXml.getAttrInt(e,"START");
      if (sln < 0) continue;
      for (IProblem ip : probs) {
	 BedrockPlugin.logD("Consider problem " + ip.getID() + " " + ip.getSourceStart() + " " + ip.getClass());
	 if (!(ip instanceof CategorizedProblem)) continue;
	 if (ip.getID() != mid) continue;
	 if (Math.abs(ip.getSourceStart() - sln) > 2) continue;
	 BedrockPlugin.logD("Add problem " + ip.getMessage());
	 pbs.add((CategorizedProblem) ip);
       }
    }

   return pbs;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputProposals(IJavaCompletionProposal [] props,IvyXmlWriter xw)
{
   if (props == null) return;

   for (IJavaCompletionProposal p : props) {
      try {
	 BedrockPlugin.logD("COMPLETION: " + p.getRelevance() + " " + p.getDisplayString() + " " +
			       p.getClass());
       }
      catch (Throwable t) { }
      if (isUsable(p)) {
	 outputProposal(p,xw);
       }
    }
}


private boolean isUsable(IJavaCompletionProposal p)
{
   if (p == null) return false;

   if (p.getClass().getName().equals("org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal")) {

      return false;
    }

   try {
      Class<?> pcc = p.getClass();
      pcc.getMethod("getChange");
      return true;
   }
   catch (Throwable t) {
      BedrockPlugin.logD("UNKNOWN COMPLETION TYPE " + p.getClass() + " " + p.getClass().getSuperclass());
   }

   return false;
}



private void outputProposal(IJavaCompletionProposal p,IvyXmlWriter xw)
{
   TextEdit textedit = null;

   try {
      Class<?> ccp = p.getClass();
      Method ccm = ccp.getMethod("getChange");
      Change c = (Change) ccm.invoke(p);
      if (c == null) return;
      if (c instanceof TextChange) {
	 TextChange tc = (TextChange) c;
	 textedit = tc.getEdit();
      }
   }
   catch (Throwable e) {
      BedrockPlugin.logE("Problem gettting completion",e);
   }

   if (textedit == null) {
      BedrockPlugin.logD("COMPLETION w/o EDIT " + p.getClass() + " " + p.getClass().getSuperclass());
      return;
    }

   xw.begin("FIX");

   xw.field("RELEVANCE",p.getRelevance());
   xw.field("DISPLAY",p.getDisplayString());

   xw.field("ID",System.identityHashCode(p));

   BedrockUtil.outputTextEdit(textedit,xw);

   xw.end("FIX");
}



/********************************************************************************/
/*										*/
/*	Context for doing the quick fix 					*/
/*										*/
/********************************************************************************/

private static class FixContext implements IInvocationContext {

   private CompilationUnit ast_root;
   private ICompilationUnit comp_unit;
   private int source_offset;
   private int source_length;
   private NodeFinder node_finder;

   FixContext(CompilationUnit cu,ICompilationUnit icu,int off,int len) {
      ast_root = cu;
      comp_unit = icu;
      source_offset = off;
      source_length = len;
      node_finder = new NodeFinder(ast_root,off,len);
    }

   @Override public CompilationUnit getASTRoot()		{ return ast_root; }

   @Override public ICompilationUnit getCompilationUnit()	{ return comp_unit; }

   @Override public int getSelectionLength()		{ return source_length; }
   @Override public int getSelectionOffset()		{ return source_offset; }

   @Override public ASTNode getCoveredNode() {
      ASTNode n = node_finder.getCoveredNode();
      BedrockPlugin.logD("Fix covered node " + n);
      return n;
    }
   @Override public ASTNode getCoveringNode() { 
      ASTNode n = node_finder.getCoveringNode();
      BedrockPlugin.logD("Fix covering node " + n);
      return n; 
    }

}	// end of inner class FixContext



/********************************************************************************/
/*										*/
/*	Problem context for quick fix						*/
/*										*/
/********************************************************************************/

private static class ProblemContext implements IProblemLocation {

   private CategorizedProblem for_problem;
   private Map<CompilationUnit,NodeFinder> finder_map;

   ProblemContext(CategorizedProblem ip) {
      for_problem = ip;
      finder_map = new HashMap<CompilationUnit,NodeFinder>();
    }

   @Override public ASTNode getCoveredNode(CompilationUnit cu) {
      NodeFinder nf = null;
      synchronized (finder_map) {
	 nf = finder_map.get(cu);
	 if (nf == null) {
	    nf = new NodeFinder(cu,for_problem.getSourceStart(),getLength());
	    finder_map.put(cu,nf);
	  }
       }
      return nf.getCoveredNode();
    }

   @Override public ASTNode getCoveringNode(CompilationUnit cu) {
      NodeFinder nf = null;
      synchronized (finder_map) {
	 nf = finder_map.get(cu);
	 if (nf == null) {
	    nf = new NodeFinder(cu,for_problem.getSourceStart(),getLength());
	    finder_map.put(cu,nf);
	  }
       }
      return nf.getCoveringNode();
    }

   @Override public int getLength() {
      return for_problem.getSourceEnd() - for_problem.getSourceStart();
    }

   @Override public String getMarkerType() {
      return for_problem.getMarkerType();
    }

   @Override public int getOffset() {
      return for_problem.getSourceStart();
    }

   @Override public String [] getProblemArguments() {
      return for_problem.getArguments();
    }

   @Override public int getProblemId() {
      return for_problem.getID();
    }

   @Override public boolean isError() {
      return for_problem.isError();
    }

}	// end of inner class ProblemContext









}	// end of class BedrockQuickFix




/* end of BedrockQuicFix.java */
