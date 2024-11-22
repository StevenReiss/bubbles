/********************************************************************************/
/*										*/
/*		BedrockRenamer.java						*/
/*										*/
/*	Handle editor-related commands for Bubbles				*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import java.util.ArrayList;
import java.util.List;


class BedrockRenamer implements BedrockConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin	our_plugin;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockRenamer(BedrockPlugin bp)
{
   our_plugin = bp;
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
   ICompilationUnit icu = our_plugin.getCompilationUnit(proj,file);

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

   BedrockPlugin.logD("RENAME CHECK " + relt.getElementType() + " " +
			 relt.getParent().getElementType());

   switch (relt.getElementType()) {
      case IJavaElement.COMPILATION_UNIT :
	 throw new BedrockException("Compilation unit renaming not supported yet");
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 throw new BedrockException("Package renaming not supported yet");
      case IJavaElement.FIELD :
      case IJavaElement.LOCAL_VARIABLE :
      case IJavaElement.TYPE_PARAMETER :
	 break;
      case IJavaElement.METHOD :
	 IMethod mthd = (IMethod) relt;
	 try {
	    if (mthd.isConstructor()) throw new BedrockException("Constructor renaming not supported yet");
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.TYPE :
	 IJavaElement pelt = relt.getParent();
	 if (pelt.getElementType() == IJavaElement.COMPILATION_UNIT) {
	    ITypeRoot xcu = (ITypeRoot) pelt;
	    if (relt == xcu.findPrimaryType()) {
	       throw new BedrockException("Compilation unit renaming based on type not supported yet");
	     }
	  }
	 break;
      default :
	 throw new BedrockException("Invalid element type to rename");
   }

   SearchPattern sp = SearchPattern.createPattern(relt,IJavaSearchConstants.ALL_OCCURRENCES,
						     SearchPattern.R_EXACT_MATCH);

   List<ICompilationUnit> worku = new ArrayList<ICompilationUnit>();
   for (IJavaElement je : BedrockJava.getAllProjects()) {
      our_plugin.getWorkingElements(je,worku);
    }
   ICompilationUnit [] work = new ICompilationUnit[worku.size()];
   work = worku.toArray(work);

   int fg = IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS;
   IJavaSearchScope scp = SearchEngine.createJavaSearchScope(work,fg);

   SearchEngine se = new SearchEngine(work);
   SearchParticipant [] parts = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
   FindHandler fh = new FindHandler(xw,null);

   try {
      se.search(sp,parts,scp,fh,null);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem doing find all search: " + e,e);
    }

   BedrockPlugin.logD("RENAME RESULT = " + xw.toString());
}



/********************************************************************************/
/*										*/
/*	Callbacks for Java search handling					*/
/*										*/
/********************************************************************************/

private static class FindHandler extends SearchRequestor {

   private FindFilter find_filter;
   private List<SearchMatch> all_matches;

   FindHandler(IvyXmlWriter xw,FindFilter ff) {
      find_filter = ff;
      all_matches = new ArrayList<SearchMatch>();
    }

   @Override public void acceptSearchMatch(SearchMatch mat) {
      if (reportMatch(mat)) {
	 all_matches.add(mat);
       }
    }

   private boolean reportMatch(SearchMatch mat) {
      IResource irc = mat.getResource();
      if (irc.getType() != IResource.FILE) return false;
      if (find_filter != null) return find_filter.checkMatch(mat);
      if (mat.getAccuracy() != SearchMatch.A_ACCURATE) return false;
      return true;
    }

}	// end of subclass FindHandler



private interface FindFilter {

   boolean checkMatch(SearchMatch mat);

}






}	// end of class BedrockRenamer




/* end of BedrockRenamer.java */
