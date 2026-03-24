/********************************************************************************/
/*                                                                              */
/*              BstyleFixerNaming.java                                          */
/*                                                                              */
/*      Handle naming conventions                                               */
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



package edu.brown.cs.bubbles.bstyle;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.JTextComponent;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bale.BaleApplyEdits;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;
import edu.brown.cs.bubbles.bfix.BfixCorrector;
import edu.brown.cs.bubbles.bfix.BfixConstants.BfixRunnableFix;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;
import edu.brown.cs.bubbles.burp.BurpHistory;

class BstyleFixerNaming extends BstyleFixer.GenericPatternFixer implements BstyleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleFixerNaming() 
{
   super("Name '($N$)' must match pattern '([^']+)'",null,true);
}


/********************************************************************************/
/*                                                                              */
/*      Find a fix for the problem                                              */
/*                                                                              */
/********************************************************************************/

@Override BfixRunnableFix findFix(BfixCorrector bcorr,BumpProblem bp,
      boolean explicit,long starttime)
{
   Matcher m0 = useFix(bp,explicit);
   if (m0 == null) {
      BoardLog.logD("BSTYLE","No match for " + this.getClass());
      return null;
    }
   
   BoardLog.logD("BSTYLE","Match error message for problem " + this.getClass());
   
// BaleWindow win = bcorr.getEditor();
   int p0 = bp.getStart();
   int p1 = bp.getEnd();
   
   String name = m0.group(1);
   String pat = m0.group(2);
   String newname = findNewName(name,pat);
   
   if (newname == null) return null;
   if (newname.equals(name)) return null;
   
   RenameDoer doer = new RenameDoer(bcorr,starttime,p0,p1,newname);
   
   return doer;
}



private String findNewName(String name,String pat)
{ 
   String n0 = normalizeName(name);
   
   Set<String> names = new LinkedHashSet<>();
   names.add(n0.toLowerCase());
   names.add(n0);
   names.add(camelCase(n0,false));
   names.add(camelCase(n0,true));
   names.add(n0.replace("_",""));
   names.add(n0.replace("_","").toLowerCase());
   names.add("_" + n0);
   names.add(n0 + "_");
   if (!n0.contains("_")) {
      names.add("the_" + n0);
    }
   
   Pattern p = Pattern.compile(pat);
   for (String n : names) {
      Matcher m = p.matcher(n);
      if (m.matches()) return n;
    }
   
   return null;
}


private String normalizeName(String name)
{
   String rslt = name;
      
   boolean allupper = true;
   boolean alllower = true;
   boolean sepus = false;
   
   for (int i = 0; i < name.length(); ++i) {
      char c = name.charAt(i);
      if (c == '_' && i != 0 && i != name.length()-1) {
         sepus = true;
       }
      else if (Character.isUpperCase(c)) alllower = false;
      else if (Character.isLowerCase(c)) allupper = false;
    }
   
   if (!sepus && (allupper || alllower)) {
      // might want to try split by known words
      rslt = rslt.toUpperCase();
    }
   else if (sepus) {
      rslt = rslt.toUpperCase();
    }
   else if (alllower || allupper) {
      rslt = rslt.toUpperCase();
    }
   else {
      StringBuffer buf = new StringBuffer();
      boolean prevlower = false;
      for (int i = 0; i < rslt.length(); ++i) {
         char c = rslt.charAt(i);
         if (Character.isLowerCase(c)) {
            prevlower = true;
          }
         else if (Character.isUpperCase(c)) {
            if (prevlower) {
               buf.append("_");
             }
            prevlower = false;
          }
         buf.append(c);
       }
      rslt = buf.toString().toUpperCase();
    }
   
   if (rslt.startsWith("_")) {
      rslt = rslt.substring(1);
    }
   else if (rslt.endsWith("_")) {
      rslt = rslt.substring(0,rslt.length()-1);
    }
   
   return rslt;
}


private String camelCase(String name,boolean firstupper) 
{
   String rslt = "";
   
   String [] subs = name.split("_");
   for (int i = 0; i < subs.length; ++i) {
      String s = subs[i].toLowerCase();
      if (firstupper || i != 0) {
         s = Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
       }
      else {
         s = s.toLowerCase();
       }
      rslt += s;
    }
   
   return rslt;
}





/********************************************************************************/
/*                                                                              */
/*      Renamer                                                                 */
/*                                                                              */
/********************************************************************************/

private class RenameDoer implements BfixRunnableFix {
   
   private BfixCorrector for_corrector;
   private int name_start;
   private int name_end;
   private String new_name;
   private long initial_time;
   private Element rename_edits;
   
   RenameDoer(BfixCorrector corr,long start,int p0,int p1,String repl) {
      name_start = p0;
      name_end = p1;
      new_name = repl;
      for_corrector = corr;
    }
   
   @Override public double getPriority()                { return 0.0; }
   
   @Override public Boolean call() {
      BumpClient bc = BumpClient.getBump();
      BaleWindow win = for_corrector.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      JTextComponent c = win.getEditor();
      BudaRoot br = BudaRoot.findBudaRoot(c);
      File f = doc.getFile();
      String proj = doc.getProjectName();
      
      if (for_corrector.getStartTime() != initial_time) return false;
      BoardLog.logD("BSTYLE","Making naming fix " + 
            name_start + " " + name_end + " " + new_name);
      
      
      BoardMetrics.noteCommand("BSTYLE","NamingCorrection_" + for_corrector.getBubbleId());
      rename_edits = bc.rename(proj,f,name_start,name_end,new_name);
      if (rename_edits == null) return false;
      
      if (br != null) br.handleSaveAllRequest();
      
      BurpHistory.getHistory().beginEditAction(c);
      try {
         BaleApplyEdits bae = new BaleApplyEdits();
         bae.applyEdits(rename_edits);
         if (br != null) {
            br.handleSaveAllRequest();
            bc.compile(false,true,true);
          }
       }
      catch (Throwable t) {
         return false;
       }
      finally {
         BurpHistory.getHistory().endEditAction(c);
       }
      
      BoardMetrics.noteCommand("BSTYLE","DoneNamingCorrection_" + for_corrector.getBubbleId());
      
      return true;
    }
   
}       // end of inner class RenameDoer


}       // end of class BstyleFixerNaming




/* end of BstyleFixerNaming.java */

