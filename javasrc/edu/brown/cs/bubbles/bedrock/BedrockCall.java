/********************************************************************************/
/*										*/
/*		BedrockCall.java						*/
/*										*/
/*	Handle callgraph-related commands for Bubbles				*/
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
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


class BedrockCall implements BedrockConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;

private static final int	MAX_LEVELS = 5;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockCall(BedrockPlugin bp)
{
   our_plugin = bp;
}



/********************************************************************************/
/*										*/
/*	Search for call path between methods					*/
/*										*/
/********************************************************************************/

void getCallPath(String proj,String src,String tgt,boolean shortest,int lvls,IvyXmlWriter xw)
	throws BedrockException
{
   IProject ip = our_plugin.getProjectManager().findProject(proj);
   IJavaProject ijp = JavaCore.create(ip);
   if (lvls < 0) lvls = MAX_LEVELS;

   IJavaElement [] pelt = new IJavaElement[] { ijp };
   int incl = IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES |
      IJavaSearchScope.REFERENCED_PROJECTS;
   IJavaSearchScope scp = SearchEngine.createJavaSearchScope(pelt,incl);

   SearchEngine se = new SearchEngine();
   SearchParticipant [] parts = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };

   SearchPattern p1 = SearchPattern.createPattern(src,IJavaSearchConstants.METHOD,
						     IJavaSearchConstants.DECLARATIONS,
						     SearchPattern.R_PATTERN_MATCH);
   SearchPattern p1a = SearchPattern.createPattern(fixConstructor(src),IJavaSearchConstants.CONSTRUCTOR,
						      IJavaSearchConstants.DECLARATIONS,
						      SearchPattern.R_PATTERN_MATCH);
   if (p1 == null || p1a == null) throw new BedrockException("Illegal source pattern " + src);

   SearchPattern p2 = SearchPattern.createPattern(tgt,IJavaSearchConstants.METHOD,
						     IJavaSearchConstants.DECLARATIONS,
						     SearchPattern.R_PATTERN_MATCH);
   SearchPattern p2a = SearchPattern.createPattern(fixConstructor(tgt),IJavaSearchConstants.CONSTRUCTOR,
						     IJavaSearchConstants.DECLARATIONS,
						     SearchPattern.R_PATTERN_MATCH);
   if (p2 == null || p2a == null) throw new BedrockException("Illegal target pattern " + tgt);

   SetHandler sh = new SetHandler();
   try {
      se.search(p1,parts,scp,sh,null);
      BedrockPlugin.logD("CALL: Source A: " + sh.getSize() + " " + p1);
      if (sh.isEmpty()) se.search(p1a,parts,scp,sh,null);
      BedrockPlugin.logD("CALL: Source B: " + sh.getSize() + " " + p1a);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem doing call search 1: " + e,e);
    }

   SetHandler th = new SetHandler();
   try {
      se.search(p2,parts,scp,th,null);
      BedrockPlugin.logD("CALL: Target A: " + th.getSize() + " " + p2);
      if (th.isEmpty()) se.search(p2a,parts,scp,th,null);
      BedrockPlugin.logD("CALL: Target B: " + th.getSize() + " " + p2a);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem doing call search 2: " + e,e);
    }

   Map<IMethod,CallNode> nodes = new HashMap<>();
   Queue<IMethod> workqueue = new LinkedList<>();
   for (IMethod je : th.getElements()) {
      CallNode cn = new CallNode(je,0);
      cn.setTarget();
      nodes.put(je,cn);
      workqueue.add(je);
    }

   while (!workqueue.isEmpty()) {
      IMethod je = workqueue.remove();
      CallNode cn = nodes.get(je);
      if (cn.isDone()) continue;
      cn.markDone();

      BedrockPlugin.logD("CALL: WORK ON " + je.getKey() + " " + cn.getLevel() + " " + sh.contains(je));

      if (shortest && sh.contains(je)) break;
      int lvl = cn.getLevel() + 1;
      if (lvl > lvls) continue;

      String nm = je.getElementName();
      if (nm == null) continue;
      String cnm = je.getDeclaringType().getFullyQualifiedName();
      if (cnm != null) nm = cnm.replace("$",".") + "." + nm;
      nm += "(";
      String [] ptyps = je.getParameterTypes();
      for (int i = 0; i < ptyps.length; ++i) {
	 if (i > 0) nm += ",";
	 nm += IvyFormat.formatTypeName(ptyps[i]);
       }
      nm += ")";


      SearchPattern p3;
      try {
	 BedrockPlugin.logD("CALL: Search for: " + nm + " " + je.isConstructor());
	 if (je.isConstructor()) {
	    String nm1 = fixConstructor(nm);
	    p3 = SearchPattern.createPattern(nm1,IJavaSearchConstants.CONSTRUCTOR,
						IJavaSearchConstants.REFERENCES,
						SearchPattern.R_EXACT_MATCH);
	  }
	 else {
	    p3 = SearchPattern.createPattern(nm,IJavaSearchConstants.METHOD,
						IJavaSearchConstants.REFERENCES,
						SearchPattern.R_EXACT_MATCH);
	  }

	 CallHandler ch = new CallHandler(je,workqueue,nodes,lvl);

	 se.search(p3,parts,scp,ch,null);
       }
      catch (CoreException e) {
	 throw new BedrockException("Problem doing call search e: " + e,e);
       }
    }

   // TODO: restrict to single path if shortest is set

   xw.begin("PATH");
   for (IMethod je : sh.getElements()) {
      CallNode cn = nodes.get(je);
      if (cn == null) continue;
      Set<IMethod> done = new HashSet<>();
      cn.output(xw,done,nodes);
    }
   xw.end("PATH");
}



private String fixConstructor(String s)
{
   String r = s;

   int idx0 = r.indexOf("(");
   String tail = "";
   if (idx0 > 0) {
      tail = r.substring(idx0);
      r = r.substring(0,idx0);
    }

   int idx1 = r.lastIndexOf(".");
   if (idx1 <= 0) return s;
   int idx2 = r.lastIndexOf(".",idx1-1);

   String r1 = r.substring(idx2+1,idx1);
   String r2 = r.substring(idx1+1);
   if (!r1.equals(r2)) return s;

   r = r.substring(0,idx1);
   r += tail;

   return r;
}




/********************************************************************************/
/*										*/
/*	Search handler to accumulate resultant elements 			*/
/*										*/
/********************************************************************************/

private static class SetHandler extends SearchRequestor {

   private Set<IMethod> found_elements;

   SetHandler() {
      found_elements = new HashSet<>();
    }

   @Override public void acceptSearchMatch(SearchMatch mat) {
      Object o = mat.getElement();
      if (o != null && o instanceof IMethod) {
	 found_elements.add((IMethod) o);
       }
    }

   int getSize()				{ return found_elements.size(); }
   boolean isEmpty()				{ return found_elements.isEmpty(); }
   Iterable<IMethod> getElements()		{ return found_elements; }
   boolean contains(IMethod im) 		{ return found_elements.contains(im); }

}	// end of inner class SetHandler



/********************************************************************************/
/*										*/
/*	Class to represent a call path						*/
/*										*/
/********************************************************************************/

private static class CallHandler extends SearchRequestor {

   private IMethod target_element;
   private Queue<IMethod> work_queue;
   private Map<IMethod,CallNode> call_nodes;
   private int level_count;

   CallHandler(IMethod tgt,Queue<IMethod> wq,Map<IMethod,CallNode> cn,int lvl) {
      target_element = tgt;
      work_queue = wq;
      call_nodes = cn;
      level_count = lvl;
    }

   @Override public void acceptSearchMatch(SearchMatch mat) {
      BedrockPlugin.logD("CALL: found match " + mat.getElement());
      Object o = mat.getElement();
      if (o != null && o instanceof IMethod) {
	 IMethod je = (IMethod) o;
	 CallNode cn = call_nodes.get(je);
	 if (cn == null) {
	    cn = new CallNode(je,level_count);
	    call_nodes.put(je,cn);
	  }
	 cn.addCall(mat,target_element);
	 work_queue.add(je);
       }
    }

}	// end of innerclass CallNode




/********************************************************************************/
/*										*/
/*	Class representing call node						*/
/*										*/
/********************************************************************************/

private static class CallNode {

   private IMethod from_element;
   private List<CallData> to_elements;
   private boolean is_done;
   private boolean is_target;
   private int	level_count;

   CallNode(IMethod frm,int lvl) {
      from_element = frm;
      to_elements = new ArrayList<CallData>();
      is_done = false;
      is_target = false;
      level_count = lvl;
    }

   private boolean isDone()			{ return is_done; }
   private void markDone()			{ is_done = true; }
   private void setTarget()			{ is_target = true; }
   private boolean isTarget()			{ return is_target; }
   private int getLevel()			{ return level_count; }

   void addCall(SearchMatch sm,IMethod tgt) {
      CallData cd = new CallData(sm,tgt);
      to_elements.add(cd);
    }

   void output(IvyXmlWriter xw,Set<IMethod> done,Map<IMethod,CallNode> nodes) {
      BedrockUtil.outputJavaElement(from_element,xw);
      if (done.contains(from_element)) return;
      done.add(from_element);
      for (CallData cd : to_elements) {
	 xw.begin("CALL");
	 CallNode ncd = nodes.get(cd.getTarget());
	 if (ncd != null && ncd.isTarget()) xw.field("TARGET",true);
	 BedrockUtil.outputSearchMatch(cd.getMatch(),xw);
	 if (ncd != null) ncd.output(xw,done,nodes);
	 xw.end("CALL");
       }
    }

}	// end of innerclass CallNode



private static class CallData {

   private SearchMatch using_match;
   private IMethod target_element;

   CallData(SearchMatch sm,IMethod tgt) {
      using_match = sm;
      target_element = tgt;
    }

   private SearchMatch getMatch()		{ return using_match; }
   private IMethod getTarget()			{ return target_element; }

}	// end of inner class CallData




}	// end of class BedrockCall




/* end of BedrockCall.java */

