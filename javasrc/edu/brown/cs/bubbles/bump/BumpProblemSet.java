/********************************************************************************/
/*										*/
/*		BumpProblemSet.java						*/
/*										*/
/*	BUblles Mint Partnership problem set maintainer 			*/
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


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


class BumpProblemSet implements BumpConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BumpProblemImpl>	current_problems;
private SwingEventListenerList<BumpProblemHandler> handler_set;
private Map<BumpProblemHandler,File>	problem_handlers;
private Map<String,Set<BumpProblemImpl>> private_problems;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpProblemSet()
{
   current_problems = new HashMap<>();
   problem_handlers = new HashMap<>();
   handler_set = new SwingEventListenerList<>(BumpProblemHandler.class);
   private_problems = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addProblemHandler(File f,BumpProblemHandler ph)
{
   handler_set.add(ph);
   synchronized (problem_handlers) {
      problem_handlers.put(ph,f);
    }
}



synchronized void removeProblemHandler(BumpProblemHandler ph)
{
   handler_set.remove(ph);
   synchronized (problem_handlers) {
      problem_handlers.remove(ph);
    }
}



/********************************************************************************/
/*										*/
/*	Set access methods							*/
/*										*/
/********************************************************************************/

List<BumpProblem> getProblems(File f)
{
   List<BumpProblem> rslt = new ArrayList<BumpProblem>();

   synchronized (current_problems) {
      for (BumpProblemImpl bp : current_problems.values()) {
	 if (fileMatch(f,bp)) {
	    rslt.add(bp);
	  }
       }
    }

   return rslt;
}

BumpErrorType getErrorType(String cat)
{
   BumpErrorType er = BumpErrorType.NOTICE;
   synchronized (current_problems) {
      for (BumpProblemImpl bp : current_problems.values()) {
	if (cat != null && bp.getCategory() != null && !cat.equals(bp.getCategory())) {
	   continue;
	 }
	BumpErrorType en = bp.getErrorType();
	if (er == null || en.ordinal() < er.ordinal()) er = en;
       }
    }
   return er;
}



/********************************************************************************/
/*										*/
/*	Maintenance methods							*/
/*										*/
/********************************************************************************/

void handleErrors(String proj,File forfile,String cat,int eid,Element ep)
{
   BoardLog.logD("BUMP","Handle errors " + forfile +  " " + cat);
   Set<BumpProblemImpl> found = new HashSet<>();
   List<BumpProblemImpl> added = null;
   List<BumpProblemImpl> deled = null;

   // first add new problems and build set of all problems provided

   synchronized (current_problems) {
      for (Element e : IvyXml.children(ep,"PROBLEM")) {
	 String pid = getProblemId(e);
	 BumpProblemImpl bp = current_problems.get(pid);
	 if (bp == null) {
	    bp = new BumpProblemImpl(e,pid,eid,proj);
	    BoardLog.logD("BUMP","Add problem " + bp);
	    current_problems.put(pid,bp);
	    if (added == null) added = new ArrayList<>();
	    added.add(bp);
	  }
	 else {
	    BoardLog.logD("BUMP","Update problem " + bp);
	    bp.setEditId(eid);
	    bp.update(e);
	  }

	 found.add(bp);
       }

      // next remove any problems that seem to have disappeared
      if (forfile != null) {
	 for (Iterator<BumpProblemImpl> it = current_problems.values().iterator(); it.hasNext(); ) {
	    BumpProblemImpl bp = it.next();
	    if (found.contains(bp)) continue;
	    if (!fileMatch(forfile,bp)) continue;
	    if (cat != null && !cat.equals(bp.getCategory())) continue;
	    if (deled == null) deled = new ArrayList<>();
	    deled.add(bp);
	    BoardLog.logD("BUMP","Remove problem " + bp);
	    it.remove();
	 }
      }
    }

   List<BumpProblemHandler> bphl = new ArrayList<>();
   synchronized (problem_handlers) {
     for (BumpProblemHandler bph : handler_set) {
        bphl.add(bph);
      }
    }
   
   for (BumpProblemHandler bph : bphl) {
      File f;
      f = problem_handlers.get(bph);
      if (f == null && !problem_handlers.containsKey(bph)) continue;
      int ct = 0;
      if (deled != null) {
         for (BumpProblemImpl bp : deled) {
            if (fileMatch(f,bp)) {
               bph.handleProblemRemoved(bp);
               ++ct;
             }
          }
       }
      if (added != null) {
         for (BumpProblemImpl bp : added) {
            if (fileMatch(f,bp)) {
               bph.handleProblemAdded(bp);
               ++ct;
             }
          }
       }
//    if (ct > 0) bph.handleProblemsDone();
      BoardLog.logD("BUMP","FINISHED WITH PROBLEMS " + ct);
      bph.handleProblemsDone();
    }
}




void clearProblems(String proj,File file,String category)
{
   BoardLog.logD("BUMP","Clear Problems " + proj);
   File fcanon = null;
   List<BumpProblemImpl> clear;
   synchronized (current_problems) {
      if (proj == null) {
	 clear = new ArrayList<>(current_problems.values());
	 current_problems.clear();
       }
      else {
	 clear = new ArrayList<>();
	 for (Iterator<BumpProblemImpl> it = current_problems.values().iterator();
	    it.hasNext(); ) {
	    BumpProblemImpl bp = it.next();
	    if (file != null) {
	       File f1 = bp.getFile();
	       if (f1 == null) continue;
	       if (!f1.equals(file) && f1.getName().equals(file.getName())) {
		  if (fcanon == null) fcanon = IvyFile.getCanonical(file);
		  File f2 = IvyFile.getCanonical(f1);
		  if (!fcanon.equals(f2)) continue;
		  BoardLog.logD("BUMP","REMOVE FILE PROBLEM " + bp);
		  clear.add(bp);
		  it.remove();
		}
	     }
	    if (bp.getProject() == null) {
	       BoardLog.logE("BUMP","Problem lacks a project " + bp.getFile() + " " +
		     bp.getMessage());
	     }
	    if (bp.getProject() != null && proj != null && bp.getProject().equals(proj)) {
	       if (category == null || bp.getCategory() == null ||
		     category.equals(bp.getCategory())) {
		  BoardLog.logD("BUMP","REMOVE PROBLEM " + bp);
		  clear.add(bp);
		  it.remove();
		}
	     }
	  }
       }
    }

   synchronized (problem_handlers) {
      if (clear.size() > 0) {
	 for (BumpProblemHandler bph : handler_set) {
	    for (BumpProblemImpl bp : clear) {
	       bph.handleProblemRemoved(bp);
	     }
	    bph.handleClearProjectProblems(proj);
	    bph.handleProblemsDone();
	  }
       }
    }
}


void fireClearAll(String proj)
{
   for (BumpProblemHandler bph : handler_set) {
      bph.handleClearProjectProblems(proj);
    }
}



/********************************************************************************/
/*										*/
/*	Private buffer problem management					*/
/*										*/
/********************************************************************************/

void clearPrivateProblems(String pid)
{
   synchronized (private_problems) {
      private_problems.remove(pid);
    }
}


void handlePrivateErrors(String proj,File forfile,String privid,boolean fail,Element ep)
{
   if (fail) {
      private_problems.put(privid,null);
      private_problems.notifyAll();
      return;
    }

   Set<BumpProblemImpl> probs = new HashSet<BumpProblemImpl>();
   for (Element e : IvyXml.children(ep,"PROBLEM")) {
      String pid = getProblemId(e);
      BumpProblemImpl bp = new BumpProblemImpl(e,pid,-1,proj);
      probs.add(bp);
    }

   synchronized (private_problems) {
      private_problems.put(privid,probs);
      private_problems.notifyAll();
    }
}


Collection<BumpProblem> getPrivateErrors(String privid)
{
   long start = System.currentTimeMillis();

   synchronized (private_problems) {
      while (!private_problems.containsKey(privid)) {
	 if ((System.currentTimeMillis() - start) > 10000) break;
	 try {
	    private_problems.wait(1000);
	  }
	 catch (InterruptedException e) {
	    BoardLog.logE("BUMP","Interrupted getting Private problems " + privid);
	  }
       }

      if (!private_problems.containsKey(privid)) {
	 BoardLog.logE("BUMP","Failed to get private problems " + privid);
       }
      if (private_problems.get(privid) == null) return null;
      return new ArrayList<BumpProblem>(private_problems.get(privid));
    }
}



/********************************************************************************/
/*										*/
/*	Problem handling methods						*/
/*										*/
/********************************************************************************/

private boolean fileMatch(File forfile,BumpProblemImpl bp)
{
   File f1 = bp.getFile();
   if (forfile == null) return true;
   if (forfile.equals(f1)) return true;
   if (!forfile.getName().equals(f1.getName())) return false;

   return IvyFile.getCanonical(forfile).equals(IvyFile.getCanonical(f1));
}



private String getProblemId(Element e)
{
   int lno = IvyXml.getAttrInt(e,"LINE",0);
   int sloc = IvyXml.getAttrInt(e,"START",0);
   int mid = IvyXml.getAttrInt(e,"MSGID",0);
   String fnm = IvyXml.getTextElement(e,"FILE");

   int id = mid + lno*2 + sloc*3;
   if (fnm != null) id ^= fnm.hashCode();

   return Integer.toString(id);
}



}	// end of class BumpProblemSet




/* end of BumpProblemSet.java */









































































































