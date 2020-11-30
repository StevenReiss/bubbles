/********************************************************************************/
/*										*/
/*		BfixAdapterImports.java 					*/
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.text.BadLocationException;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BfixAdapterImports extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Map<String,ImportChecker> import_checkers;
private static Map<BfixCorrector,Set<String>> imports_added;

static {
   import_checkers = new HashMap<String,ImportChecker>();
   imports_added = new WeakHashMap<BfixCorrector,Set<String>>();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixAdapterImports()
{
   super("Import adder");
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override void addFixers(BfixCorrector corr,BumpProblem bp,boolean explict,List<BfixFixer> rslt)
{
   String fix = getImportCandidate(corr,bp);
   if (fix == null) return;

   ImportFixer fixer = new ImportFixer(corr,bp,fix);
   rslt.add(fixer);
}



@Override String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   String name = getImportCandidate(corr,bp);
   return name;
}



/********************************************************************************/
/*										*/
/*	Get candidate for import fix						*/
/*										*/
/********************************************************************************/

String getImportCandidate(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   BaleWindowDocument document = corr.getEditor().getWindowDocument();
   BoardLog.logD("BFIX","IMPORT problem " + bp.getMessage());
   int soff = document.mapOffsetToJava(bp.getStart());
   int eoff = document.mapOffsetToJava(bp.getEnd());
   if (eoff == soff && bp.getMessage().startsWith("Syntax error")) return null;

   BaleWindowElement elt = document.getCharacterElement(soff);
   // need to have an identifier to correct
   if (!elt.isIdentifier()) return null;
   // can't be working on the identifier at this point
   int elstart = elt.getStartOffset();
   int eloff = elt.getEndOffset();
   if (eoff + 1 != eloff && eoff != eloff) return null;
   if (corr.getEndOffset() > 0 && eloff + 1 >= corr.getEndOffset()) return null;

   String txt = document.getWindowText(elstart,eloff-elstart);
   return txt;
}



/********************************************************************************/
/*										*/
/*	Fixer to add imports							*/
/*										*/
/********************************************************************************/


private static class ImportFixer extends BfixFixer {

   private BaleWindowDocument for_document;
   private String for_identifier;
   private long initial_time;

   ImportFixer(BfixCorrector corr,BumpProblem bp,String txt) {
      super(corr,bp);
      for_document = corr.getEditor().getWindowDocument();
      for_identifier = txt;
      initial_time = corr.getStartTime();
    }

   @Override protected String getMemoId()	{ return for_identifier; }

   @Override protected RunnableFix findFix() {
      int soffet = for_document.mapOffsetToJava(for_problem.getStart());
      BaleWindowElement elt = for_document.getCharacterElement(soffet);
      if (!elt.isTypeIdentifier()) {
         if (for_identifier.length() == 0) return null;
         if (!Character.isUpperCase(for_identifier.charAt(0))) return null;
         // return false;
       }
   
      ImportChecker ic = getImportCheckerForProject(for_problem.getProject());
      if (ic == null) return null;
      Collection<String> types = ic.findImport(for_identifier);
      if (types == null || types.size() == 0) return null;
   
      String accept = null;
      BumpClient bc = BumpClient.getBump();
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      BoardMetrics.noteCommand("BFIX","ImportCheck_" + types.size());
      String badaccept = null;
   
      for (String type : types) {
         String pid = createPrivateBuffer(proj,filename);
         if (pid == null) return null;
         try {
            boolean isokay = true;
            BoardLog.logD("BFIX","IMPORT: using private buffer " + pid);
            Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
            if (probs == null) {
               BoardLog.logE("BFIX","SPELL: Problem getting errors for " + pid);
               return null;
             }
            int probct = getErrorCount(probs);
            if (!checkProblemPresent(for_problem,probs)) {
               BoardLog.logD("BFIX","SPELL: import Problem went away");
               return null;
             }
            int inspos = findImportLocation();
            if (inspos < 0) continue;
            String impstr = "import " + type + ";\n";
            bc.beginPrivateEdit(filename,pid);
            BoardLog.logD("BFIX","IMPORT fix:  " + type);
            bc.editPrivateFile(proj,file,pid,inspos,inspos,impstr);
            int delta = impstr.length();
            probs = bc.getPrivateProblems(filename,pid);
            if (probs == null) {
               isokay = false;
             }
            else if (getErrorCount(probs) >= probct) {
               if (getErrorCount(probs) == probct && !checkAnyProblemPresent(for_problem,probs,delta,delta)) {
        	  if (badaccept == null) badaccept = type;
        	  else badaccept = "*";
        	}
               isokay = false;
             }
            if (isokay && checkAnyProblemPresent(for_problem,probs,delta,delta)) isokay = false;
            if (isokay) {
               if (accept == null) accept = type;
               else return null;
             }
          }
         finally {
            bc.removePrivateBuffer(proj,filename,pid);
          }
       }
      if (accept == null && badaccept != null && !badaccept.equals("*")) accept = badaccept;
      if (accept == null) return null;
   
      if (for_corrector.getStartTime() != initial_time) return null;
      BoardLog.logD("BFIX","IMPORT: DO " + accept);
      BoardMetrics.noteCommand("BFIX","IMPORTFIX");
      ImportDoer id = new ImportDoer(for_corrector,for_document,for_problem,accept,initial_time);
   
      return id;
    }

   private int findImportLocation() {
      BaleFileOverview base = for_document.getBaseWindowDocument();
      String body = null;
      try {
	 body = base.getText(0,base.getLength());
       }
      catch (BadLocationException e) { }

      if (body == null || body.length() == 0) return -1;

      String pats = "\\s*((public|private|abstract|static)\\s+)*(class|interface|enum)\\s+(\\w+(<.*>)?)\\s+((extends\\s)|(implements\\s)|\\{)";
      Pattern pat = Pattern.compile(pats,Pattern.MULTILINE);
      Matcher mat = pat.matcher(body);
      if (!mat.find()) return -1;

      for (int idx = mat.start(); idx < mat.end(); ++idx) {
	 char c = body.charAt(idx);
	 if (c == '\n') {
	    int pos = idx+1;
	    return base.mapOffsetToEclipse(pos);
	  }
       }
      return 0;
    }

}	// end of class BfixAdapterSpelling




/********************************************************************************/
/*										*/
/*	Import checker								*/
/*										*/
/********************************************************************************/

synchronized static void updateImports()
{
   for (ImportChecker ic : import_checkers.values()) {
      ic.loadProjectClasses();
    }
}

private static synchronized ImportChecker getImportCheckerForProject(String proj)
{
   if (proj == null) return null;

   ImportChecker ic = import_checkers.get(proj);
   if (ic == null) {
      ic = new ImportChecker(proj);
      import_checkers.put(proj,ic);
      ic.loadProjectClasses();
    }
   return ic;
}


private static class ImportChecker {

   private Set<String> project_classes;
   private String for_project;
   private Set<String> explicit_imports;
   private Set<String> demand_imports;
   private Set<String> implicit_imports;

   ImportChecker(String proj) {
      for_project = proj;
      project_classes = new HashSet<String>();
      explicit_imports = new HashSet<String>();
      demand_imports = new HashSet<String>();
      implicit_imports = new HashSet<String>();
    }

   void loadProjectClasses() {
      BumpClient bc = BumpClient.getBump();
      Element e = bc.getProjectData(for_project,false,false,true,false,true);
      if (e == null) return;

      project_classes = new HashSet<String>();
      explicit_imports = new HashSet<String>();
      demand_imports = new HashSet<String>();
      implicit_imports = new HashSet<String>();

      Element clss = IvyXml.getChild(e,"CLASSES");
      for (Element cls : IvyXml.children(clss,"TYPE")) {
	 String nm = IvyXml.getTextElement(cls,"NAME");
	 project_classes.add(nm);
       }
      for (Element refs : IvyXml.children(e,"REFERENCES")) {
	 String rproj = IvyXml.getText(refs);
	 ImportChecker nic = getImportCheckerForProject(rproj);
	 for (String s : nic.project_classes) {
	    project_classes.add(s);
	  }
       }
      @SuppressWarnings("unused") String pkg = null;
      for (Element imps : IvyXml.children(e,"IMPORT")) {
	 String npkg = IvyXml.getAttrString(imps,"PACKAGE");
	 if (npkg !=  null) pkg = npkg;
	 if (IvyXml.getAttrBool(imps,"STATIC")) continue;
	 String imp = IvyXml.getText(imps);
	 if (IvyXml.getAttrBool(imps,"DEMAND")) {
	    int idx = imp.indexOf(".*");
	    if (idx > 0) imp = imp.substring(0,idx);
	    demand_imports.add(imp);
	  }
	 else {
	    explicit_imports.add(imp);
	    int idx = imp.lastIndexOf(".");
	    if (idx >= 0) {
	       implicit_imports.add(imp.substring(0,idx));
	     }
	  }
       }
    }

   Collection<String> findImport(String nm) {
      String pat = "." + nm;
      Set<String> match = new HashSet<String>();
      for (String s : project_classes) {
         if (s.endsWith(pat) || s.equals(nm)) {
            match.add(s.replace("$","."));
          }
       }
      if (match.size() > 0) return match;
   
      Set<String> dmatch = new HashSet<String>();
      Set<String> amatch = new HashSet<String>();
      Set<String> imatch = new HashSet<String>();
   
      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> typlocs = bc.findAllTypes(nm);
      if (typlocs == null) return null;
      for (BumpLocation bl : typlocs) {
         String tnm = bl.getSymbolName();
         int idx = tnm.indexOf("<");
         if (idx > 0) tnm = tnm.substring(0,idx).trim();
         if (explicit_imports.contains(tnm)) {
            match.add(tnm);
          }
         for (String s : demand_imports) {
            String dimp = s + "." + nm;
            if (dimp.equals(tnm)) {
               dmatch.add(tnm);
             }
          }
         for (String s : implicit_imports) {
            String dimp = s + "." + nm;
            if (dimp.equals(tnm)) {
               imatch.add(tnm);
             }
          }
         if (!tnm.contains("internal")) amatch.add(tnm);
       }
      if (match.size() > 0) return match;
      if (dmatch.size() > 0) return dmatch;
      if (imatch.size() > 0) return imatch;
      if (amatch.size() > 0) return amatch;
   
      return null;
    }

}	// end of inner class ImportChecker





/********************************************************************************/
/*										*/
/*	Runnable to fix the imports						*/
/*										*/
/********************************************************************************/

private static class ImportDoer implements RunnableFix {

   private BfixCorrector for_corrector;
   private BaleWindowDocument for_document;
   private BumpProblem for_problem;
   private String import_type;
   private long initial_time;

   ImportDoer(BfixCorrector cor,BaleWindowDocument doc,
	 BumpProblem bp,String type,long time0) {
      for_corrector = cor;
      for_document = doc;
      for_problem = bp;
      import_type = type;
      initial_time = time0;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (for_corrector.getStartTime() != initial_time) return;
      synchronized (imports_added) {
         Set<String> impset = imports_added.get(for_corrector);
         if (impset == null) {
            impset = new HashSet<String>();
            imports_added.put(for_corrector,impset);
          }
         if (!impset.add(import_type)) return;
       }
   
      BoardMetrics.noteCommand("BFIX","AddImport");
      Element edits = bc.fixImports(for_problem.getProject(),
            for_document.getFile(),null,0,0,import_type);
      if (edits != null) {
         BaleFactory.getFactory().applyEdits(for_document.getFile(),edits);
       }
      BoardMetrics.noteCommand("BFIX","DoneAddImport");
    }

   @Override public double getPriority()                { return 0; }
   
}	// end of inner class ImportDoer


}	// end of class BfixAdapterImports




/* end of BfixAdapterImports.java */
