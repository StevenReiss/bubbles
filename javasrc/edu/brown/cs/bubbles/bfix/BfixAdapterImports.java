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

import edu.brown.cs.bubbles.bfix.BfixFixer.BfixBaseEdit;
import edu.brown.cs.bubbles.bfix.BfixFixer.BfixGroupEdits;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.text.BadLocationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

private static List<BfixErrorPattern> ignore_patterns;
private static Map<String,ImportChecker> import_checkers;
private static Map<BfixCorrector,Set<String>> imports_added;
private static List<BfixErrorPattern> unused_patterns;


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
   if (ignore_patterns == null) {
      ignore_patterns = new ArrayList<>();
      unused_patterns = new ArrayList<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml,"FIXES");
      for (Element cxml : IvyXml.children(fxml,"IMPORT")) {
	 if (IvyXml.getAttrBool(cxml,"IGNORE")) {
	    ignore_patterns.add(new BfixErrorPattern(cxml));
	  }
         else if (IvyXml.getAttrBool(cxml,"UNUSED")) {
            unused_patterns.add(new BfixErrorPattern(cxml));
          }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override public void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,List<BfixFixer> rslt)
{
   String fix = getImportCandidate(corr,bp);
   if (fix == null && explicit) {
      handleImportNotUsed(corr,bp,explicit,rslt);
      return;
    }

   ImportFixer fixer = new ImportFixer(corr,bp,fix);
   rslt.add(fixer);
}



@Override protected String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   String name = getImportCandidate(corr,bp);
   if (name == null) {
      for (BfixErrorPattern pat : unused_patterns) {
         if (pat.testMatch(bp.getMessage())) {
            return "Remove Import";
          }
       }
    }
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
   int soff = document.mapOffsetToJava(bp.getStart());
   int eoff = document.mapOffsetToJava(bp.getEnd());
   BoardLog.logD("BFIX","IMPORT problem " + bp.getMessage() + " " +
         soff + " " + eoff);
   if (eoff == soff) {
      for (BfixErrorPattern pat : ignore_patterns) {
	 if (pat.testMatch(bp.getMessage())) return null;
       }
    }

   BaleWindowElement elt = document.getCharacterElement(soff);
   // need to have an identifier to correct
   if (!elt.isIdentifier()) {
      BoardLog.logD("BFIX","No identifier found for import " + elt);
      return null;
    }
   // can't be working on the identifier at this point
   int elstart = elt.getStartOffset();
   int eloff = elt.getEndOffset();
   if (eoff + 1 != eloff && eoff != eloff) return null;
   if (corr.getEndOffset() > 0 && eloff + 1 >= corr.getEndOffset()) {
      BoardLog.logD("BFIX","Identifier for import being worked on " + 
            corr.getEndOffset() + " " + elstart + " " + eloff);
      return null;
    }

   String txt = document.getWindowText(elstart,eloff-elstart);
   if (txt.length() <= 2) {
      BoardLog.logD("BFIX","Identifier too short for import: " + txt);
      return null;
    }

   return txt;
}



/********************************************************************************/
/*                                                                              */
/*      Fix import not used                                                     */
/*                                                                              */
/********************************************************************************/

private void handleImportNotUsed(BfixCorrector corr,BumpProblem bp,
      boolean explicit,List<BfixFixer> rslt)
{
   for (BfixErrorPattern pat : unused_patterns) {
      if (pat.testMatch(bp.getMessage())) {
         rslt.add(new UnusedImportFixer(corr,bp));
         break;
       }
    }
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

   @Override protected BfixRunnableFix findFix() {
      int soffset = for_document.mapOffsetToJava(for_problem.getStart());
      BaleWindowElement elt = for_document.getCharacterElement(soffset);
      if (!elt.isTypeIdentifier()) {
         if (for_identifier == null || for_identifier.length() == 0) return null;
         if (!Character.isUpperCase(for_identifier.charAt(0))) return null;
         // return false;
       }
   
      ImportChecker ic = getImportCheckerForProject(for_problem.getProject());
      if (ic == null) return null;
      Collection<String> types = ic.findImport(for_identifier,for_problem.getProject(),
            for_problem.getFile(),for_problem.getStart());
      if (types == null || types.size() == 0) return null;
   
      BoardMetrics.noteCommand("BFIX","ImportCheck_" + types.size());
      int inspos = findImportLocation();
      if (inspos < 0) return null;
      Map<BfixEdit,String> tryedits = new LinkedHashMap<>();
      int mxdel = 0;
      for (String type : types) {
         String impstr = "import " + type + ";\n";
         mxdel = Math.max(mxdel,impstr.length());
         BfixEdit edit = new BfixBaseEdit(for_corrector,inspos,inspos,impstr);
         tryedits.put(edit,type);
       }
      BfixCheckAreas pareas = new BfixCheckAreas(mxdel,mxdel);
      List<BfixEdit> rslt = findPrivateEdits(tryedits.keySet(),null,pareas);
      if (rslt == null || rslt.size() != 1) return null;
      if (for_corrector.getStartTime() != initial_time) return null;
      String accept = tryedits.get(rslt.get(0));
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
   
      String pats = "\\s*((public|private|abstract|static)\\s+)*(class|interface|enum)" + 
            "\\s+(\\w+(<.*>)?)\\s+((extends\\s)|(implements\\s)|\\{)";
      Pattern pat = Pattern.compile(pats,Pattern.MULTILINE);
      Matcher mat = pat.matcher(body);
      if (!mat.find()) return -1;
   
      for (int idx = mat.start(); idx < mat.end(); ++idx) {
         char c = body.charAt(idx);
         if (c == '\n') {
            int pos = idx+1;
            return pos;
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

static synchronized void updateImports()
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

   Collection<String> findImport(String nm,String proj,File file,int offset) {
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
      if (typlocs.size() == 0) {
	 typlocs = bc.findSystemDefinitions(proj,file,offset);
       }
      if (typlocs != null) {
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

private static class ImportDoer extends BfixFixDoer {

   private BaleWindowDocument for_document;
   private String import_type;

   ImportDoer(BfixCorrector cor,BaleWindowDocument doc,
         BumpProblem bp,String type,long time) {
      super(cor,bp,time);
      for_document = doc;
      import_type = type;
    }

   @Override public Boolean call() {
      BumpClient bc = BumpClient.getBump();
      synchronized (imports_added) {
         Set<String> impset = imports_added.get(for_corrector);
         if (impset == null) {
            impset = new HashSet<String>();
            imports_added.put(for_corrector,impset);
          }
         if (!impset.add(import_type)) {
            BoardLog.logD("BFIX","Import already in import set: " + import_type);
            return false;
          }
       }
   
      BoardMetrics.noteCommand("BFIX","AddImport");
      Element edits = bc.fixImports(for_problem.getProject(),
            for_document.getFile(),null,0,0,import_type);
      if (edits == null) {
         BoardLog.logD("BFIX","No edits to add import");
         return false;
       }
      BfixEdit bedits = new BfixGroupEdits(for_corrector,edits);
      boolean fg = testEdit(bedits,null,null);
      BoardMetrics.noteCommand("BFIX","DoneAddImport");
      return fg;
    }

   @Override public double getRegionOrder()		{ return 0; } 

}	// end of inner class ImportDoer



/********************************************************************************/
/*                                                                              */
/*      Fixers for removing an unused import                                    */
/*                                                                              */
/********************************************************************************/

private static class UnusedImportFixer extends BfixFixer {

   private long initial_time;
   
   UnusedImportFixer(BfixCorrector corr,BumpProblem bp) {
      super(corr,bp);
      initial_time = corr.getStartTime();
    }
   
   @Override protected BfixRunnableFix findFix() {
      BaleWindow win = for_corrector.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      String proj = doc.getProjectName();
      File file = doc.getFile();
      String filename = file.getAbsolutePath();
      
      int soffset = doc.mapOffsetToJava(for_problem.getStart());
      BaleWindowElement elt = doc.getCharacterElement(soffset);
      if (elt == null) return null;
      BaleWindowElement pelt = null;
      for (BaleWindowElement xelt = elt; 
                xelt != null && pelt == null;
                xelt = xelt.getPreviousCharacterElement()) {
         switch (xelt.getTokenType()) {
            case IMPORT: 
               pelt = xelt;
               break;
            case DOT :
            case IDENTIFIER :
            case SPACE :
               break;
            default :
               return null;
          }
       }
      if (pelt == null) return null;
      BaleWindowElement nelt = null;
      for (BaleWindowElement xelt = elt; 
                xelt != null && nelt == null;
                xelt = xelt.getNextCharacterElement()) {
         switch (xelt.getTokenType()) {
            case SEMICOLON : 
               nelt = xelt;
               break;
            case DOT :
            case IDENTIFIER :
            case SPACE :
               break;
            default :
               return null;
          }
       }
      if (nelt == null) return null;
      BaleWindowElement nnelt = null;
      for (BaleWindowElement xelt = nelt.getNextCharacterElement(); 
         xelt != null && nnelt == null;
         xelt = xelt.getNextCharacterElement()) {
         switch (xelt.getTokenType()) {
            case EOL : 
               nnelt = xelt;
               break;
            case SPACE :
            case EOLCOMMENT :
               break;
            default :
               nnelt = xelt.getPreviousCharacterElement();
               break;
          }
       }
      if (nnelt == null) return null;
      
      int off = pelt.getStartOffset();
      int eoff = nnelt.getEndOffset();
      off = doc.mapOffsetToEclipse(off);
      eoff = doc.mapOffsetToEclipse(eoff);
      
      String pid = createPrivateBuffer(proj,filename);
      if (pid == null) return null;
      BumpClient bc = BumpClient.getBump();
      
      try {
         Collection<BumpProblem> oprobs = bc.getPrivateProblems(filename,pid);
         if (oprobs == null) {
            BoardLog.logE("BFIX","UNUSEDIMPORT: Problem getting errors for " + pid);
            return null;
          }
         int probct = getErrorCount(oprobs,for_problem);
         if (!checkProblemPresent(for_problem,oprobs)) {
            BoardLog.logD("BFIX","UNUSEDIMPORT: Problem went away");
            return null;
          }
         bc.beginPrivateEdit(filename,pid);
         bc.editPrivateFile(proj,file,pid,off,eoff,null);
         Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
         if (checkAnyProblemPresent(for_problem,probs,0,1)) return null;
         if (checkAnyProblemPresent(probs,for_problem.getFile(),off,eoff+1)) return null; 
         if (probs == null || getErrorCount(probs) > probct) return null;
       }
      finally {
         bc.removePrivateBuffer(proj,filename,pid);
       }
      
      return new UnusedImportDoer(for_corrector,for_problem,off,eoff,initial_time);
    }
   
}	// end of class BfixUnusedImportFixer



private static class UnusedImportDoer extends BfixFixDoer {

   private int start_offset;
   private int end_offset;
   
   UnusedImportDoer(BfixCorrector corr,BumpProblem bp,int soff,int eoff,long time) {
      super(corr,bp,time);
      start_offset = soff;
      end_offset = eoff;
    }
   
   @Override public Boolean call() {
      BfixEdit edit = new BfixBaseEdit(for_corrector,start_offset,end_offset,null);
      BfixCheckAreas areas = new BfixCheckAreas(start_offset,end_offset+1);
      return testEdit(edit,areas,"RemoveImport");
    }
   
   @Override public double getRegionOrder()                { return 0; } 
}


}	// end of class BfixAdapterImports




/* end of BfixAdapterImports.java */
