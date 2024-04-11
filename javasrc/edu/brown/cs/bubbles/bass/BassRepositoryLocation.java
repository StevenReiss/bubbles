/********************************************************************************/
/*										*/
/*		BassRepositoryLocation.java					*/
/*										*/
/*	Bubble Augmented Search Strategies store for all possible names 	*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.file.IvyFile;

import java.lang.reflect.Modifier;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BassRepositoryLocation implements BassConstants.BassUpdatingRepository,
		BassConstants, BumpConstants.BumpChangeHandler
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<BassName>	all_names;
private boolean 	is_ready;
private List<BassUpdatableRepository> update_repos;
private Map<File,List<BassName>> file_names;
private Map<String,Map<String,String>> base_map;

private Pattern 	anonclass_pattern = Pattern.compile("\\$[0-9]");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassRepositoryLocation()
{
   all_names = new HashSet<>();
   file_names = null;
   is_ready = false;
   update_repos = new ArrayList<>();
   base_map = null;

   initialize();

   BumpClient.getBump().addChangeHandler(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   waitForNames();

   synchronized (this) {
      return new ArrayList<>(all_names);
    }
}

@Override public boolean isEmpty()
{
   waitForNames();

   return all_names.isEmpty();
}



@Override public boolean includesRepository(BassRepository br)	{ return br == this; }


@Override public void addUpdateRepository(BassUpdatableRepository br)
{
   update_repos.add(br);
}

@Override public void removeUpdateRepository(BassUpdatableRepository br)
{
   update_repos.remove(br);
}


void waitForNames()
{
   synchronized (this) {
      while (!is_ready) {
	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
    }
}




BassName findBubbleName(File f,int eclipsepos)
{
   BassNameLocation best = null;
   int bestlen = 0;
   boolean inclbest = false;
   int maxdelta0 = 0;
   int maxdelta1 = 2;
   
   f = IvyFile.getCanonical(f);

   waitForNames();
   // this needs to be faster

   synchronized (this) {
      Collection<BassName> names = all_names;
      if (file_names == null && f != null) {
	 file_names = new HashMap<>();
	 for (BassName bn : names) {
	    BassNameLocation bnl = (BassNameLocation) bn;
	    File namef = bnl.getFile();
            namef = IvyFile.getCanonical(namef);
	    if (namef == null) continue;
	    List<BassName> nl = file_names.get(namef);
	    if (nl == null) {
	       nl = new ArrayList<>();
	       file_names.put(namef,nl);
	     }
	    nl.add(bn);
	  }
       }
      if (f != null && file_names != null) {
	 names = file_names.get(f);
	 if (names == null) return null;
       }

      for (BassName bn : names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
         File bnf = IvyFile.getCanonical(bnl.getFile());
	 if (bnf.equals(f)) {
	    int spos = bnl.getEclipseStartOffset();
	    int epos = bnl.getEclipseEndOffset();
	    boolean incl = (spos <= eclipsepos && epos > eclipsepos);
	    if (best != null && incl && !inclbest && best.getNameType() == bnl.getNameType()) 
               best = null;
	    if (best == null || epos - spos <= bestlen) {
	       if (best != null && epos - spos == bestlen) {
		  if (best.getNameType() == BassNameType.HEADER && 
                        bnl.getNameType() == BassNameType.CLASS) ;
		  else continue;
		}
	       if (spos-maxdelta0 <= eclipsepos && epos+maxdelta1 > eclipsepos) {	// allow for indentations
		  best = bnl;
		  bestlen = epos - spos;
		  inclbest = incl;
		}
	     }
	  }
       }
    }

   // TODO: handle fields, prefix ??

   return best;
}



List<BumpLocation> findClassMethods(String cls)
{
   List<BumpLocation> rslt = new ArrayList<BumpLocation>();
   boolean fndcls = false;

   waitForNames();

   int clsln = cls.length();

   synchronized (this) {
      for (BassName bn : all_names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
	 String fnm = bnl.getFullName();
	 if (fnm == null) continue;
	 if (fnm.equals(cls) || (fnm.startsWith(cls) && fnm.charAt(clsln) == '.')) {
	    switch (bnl.getNameType()) {
	       case CLASS :
	       case ENUM :
	       case THROWABLE :
	       case INTERFACE :
	       case ANNOTATION :
		  fndcls = true;
		  break;
	       case METHOD :
		  String cnm = bnl.getNameHead();
		  if (cls.equals(cnm)) rslt.add(bnl.getLocation());
		  break;
	       default :
		  break;
	    }

	  }
       }
    }

   if (!fndcls) return null;

   return rslt;
}


boolean checkMethodName(String proj,String nm,String args)
{
   waitForNames();

   synchronized (this) {
      for (BassName bn : all_names) {
	 String pnm = bn.getProject();
	 if (proj != null && pnm != null) {
	    if (!proj.equals(pnm)) continue;
	  }
	 BassNameLocation bnl = (BassNameLocation) bn;
	 String fnm = bnl.getName();
	 if (fnm == null || !nm.equals(fnm)) continue;
	 int mods = bnl.getModifiers();
	 if (!Modifier.isPublic(mods) && !Modifier.isProtected(mods)) continue;
	 String prms = bnl.getParameters();
	 if (prms != null && args != null) {
	    BoardLog.logD("BASS","CHECK PARAMETERS " + prms + " :: " + args);
	  }
	 return true;
       }
    }

   return false;
}



File findActualFile(File f)
{
   waitForNames();

   synchronized (this) {
      for (BassName bn : all_names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
	 if (bnl.getFile().equals(f)) return f;
	 if (bnl.getFile().getName().equals(f.getName())) return bnl.getFile();
       }
    }

   return null;
}


Set<File> findAssociatedFiles(String proj,String pfx)
{
   Set<File> rslt = new HashSet<>();

   waitForNames();

   synchronized (this) {
      for (BassName b : all_names) {
	 String pnm = b.getProject();
	 if (b.getFullName().startsWith(pfx) && pnm.equals(proj)) {
	    switch (b.getNameType()) {
	       case FILE :
	       case CLASS :
	       case ENUM :
	       case INTERFACE :
	       case ANNOTATION :
		  break;
	       default :
		  continue;
	     }
	    BumpLocation bloc = b.getLocation();
	    if (bloc == null) continue;
	    File f = bloc.getFile();
	    if (f != null) rslt.add(f);
	  }

       }
    }

   return rslt;
}


String findProjectForFile(File f)
{
   waitForNames();
   
   synchronized (this) {
      for (BassName b : all_names) {
         BassNameLocation bnl = (BassNameLocation) b;
         if (bnl.getFile().equals(f)) {
            return bnl.getProject();
          }
       }
      for (BassName b : all_names) {
         BassNameLocation bnl = (BassNameLocation) b;
         if (bnl.getFile().getName().equals(f.getName())) {
            return bnl.getProject();
          }
       }
    }
   
   return null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void initialize()
{
   synchronized (this) {
      all_names.clear();
      file_names = null;
      is_ready = false;
    }

   Searcher s = new Searcher();
   BoardThreadPool.start(s);
}



private synchronized void loadNames()
{
   File f1 = BoardSetup.getBubblesWorkingDirectory();
   File f2 = new File(f1,"bass.symbols");
   int maxsym = 0;
   // get number of names last time to provide % complete
   try (BufferedReader br = new BufferedReader(new FileReader(f2))) {
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 try {
	    maxsym = Integer.parseInt(ln);
	    break;
	  }
	 catch (NumberFormatException e) { }
       }
    }
   catch (IOException e) { }

   Map<String,BassNameLocation> usedmap = new HashMap<>();

   BumpClient bc = BumpClient.getBump();
   Collection<BumpLocation> locs = bc.findAllNames(null,null,true,maxsym);

   if (locs != null) checkBaseMap(locs);

   if (locs != null) {
      for (BumpLocation bl : locs) {
	 addLocation(bl,usedmap);
       }
    }

   try (PrintWriter pw = new PrintWriter(f2)) {
      if (locs != null) pw.print(locs.size());
    }
   catch (IOException e) { }

   is_ready = true;
   notifyAll();
}



private void checkBaseMap(Collection<BumpLocation> locs)
{
   if (locs == null) return;

   Map<String,Set<String>> projpaths = new HashMap<>();
   for (BumpLocation bl : locs) {
      switch (bl.getSymbolType()) {
	 case CLASS :
	 case ENUM :
	 case INTERFACE :
	 case ANNOTATION :
	    break;
	 default :
	    continue;
       }
      File f = bl.getFile();
      if (f == null) continue;
      String s = bl.getSymbolName();
      if (s == null) continue;
      int idx = s.lastIndexOf(".");
      if (idx < 0) continue;
      String jfnm = s.substring(idx+1) + ".java";
      if (!f.getName().equals(jfnm)) continue;
      f = f.getParentFile();
      s = s.substring(0,idx);
      for ( ; ; ) {
	 idx = s.lastIndexOf(".");
	 if (idx < 0) break;
	 s = s.substring(0,idx);
	 f = f.getParentFile();
       }
      f = f.getParentFile();
      if (!f.exists()) continue;
      String p = bl.getProject();
      if (p == null) continue;
      Set<String> paths = projpaths.get(p);
      if (paths == null) {
	 paths = new HashSet<>();
	 projpaths.put(p,paths);
       }
      paths.add(f.getPath());
    }

   if (projpaths.isEmpty()) return;
   for (Map.Entry<String,Set<String>> ent : projpaths.entrySet()) {
      Set<String> rslt = ent.getValue();
      if (rslt.size() > 1) {
	 String pfx = null;
	 String sfx = null;
	 for (String s : rslt) {
	    if (pfx == null) pfx = s;
	    else pfx = commonPrefix(pfx,s);
	    if (sfx == null) sfx = s;
	    else sfx = commonSuffix(sfx,s);
	  }
	 if (base_map == null) base_map = new HashMap<>();
	 String proj = ent.getKey();
	 Map<String,String> projmap = base_map.get(proj);
	 if (projmap == null) {
	    projmap = new HashMap<>();
	    base_map.put(proj,projmap);
	  }
	 int ln = pfx.length();
	 int sln = sfx.length();
	 for (String s : rslt) {
	    String nm = s.substring(ln);
	    if (sln > 0) {
	       int epos = nm.length() - sln;
	       nm = nm.substring(0,epos);
	     }
	    projmap.put(s,nm);
	  }
       }
    }
}



private String commonPrefix(String s1,String s2)
{
   int ln = Math.min(s1.length(),s2.length());
   for (int i = 0; i < ln; ++i) {
      if (s1.charAt(i) != s2.charAt(i)) {
	 return s1.substring(0,i);
       }
    }
   if (s1.length() == ln) return s1;
   return s2;
}


private String commonSuffix(String s1,String s2)
{
   int ln1 = s1.length();
   int ln2 = s2.length();
   int ln = Math.min(ln1,ln2);
   for (int i = 0; i < ln; ++i) {
      if (s1.charAt(ln1-i-1) != s2.charAt(ln2-i-1)) {
	 if (i == 0) return "";
	 return s1.substring(ln1-i);
       }
    }
   if (s1.length() == ln) return s1;
   return s2;
}



private void addLocation(BumpLocation bl,Map<String,BassNameLocation> usedmap)
{
   if (!isRelevant(bl)) {
//    BoardLog.logD("BASS","Ignore symbol " + bl);
      return;
    }

   String pfx = null;
   if (base_map != null && bl.getFile() != null && bl.getProject() != null) {
      Map<String,String> pmap = base_map.get(bl.getProject());
      if (pmap != null) {
	 String path = bl.getFile().getPath();
	 String best = null;
	 for (String key : pmap.keySet()) {
	    if (path.startsWith(key)) {
	       if (best == null || best.length() < key.length()) {
		  best = key;
		}
	     }
	  }
	 if (best != null) pfx = pmap.get(best);
       }
    }

   if (pfx == null) pfx = bl.getPrefix();

   BassNameLocation bn = new BassNameLocation(bl,pfx);
   String key = null;

   switch (bn.getNameType()) {
      case FIELDS :
	 key = "FIELD@@@" + bn.getNameHead();
	 BassNameLocation fbn = usedmap.get(key);
	 if (fbn != null) {
	    fbn.addLocation(bl);
	    bn = null;
	  }
	 else usedmap.put(key,bn);
	 break;
      case VARIABLES :
	 key = "VARIABLE@@@" + bn.getNameHead();
	 BassNameLocation vbn = usedmap.get(key);
	 if (vbn != null) {
	    vbn.addLocation(bl);
	    bn = null;
	  }
	 else usedmap.put(key,bn);
	 break;
      case MAIN_PROGRAM :
	 key = "MAIN@@@" + bn.getNameHead();
	 BassNameLocation mbn = usedmap.get(key);
	 if (mbn != null) {
	    mbn.addLocation(bl);
	    bn = null;
	 }
	 else usedmap.put(key,bn);
	 break;
      case STATICS :
	 key = "STATIC@@@" + bn.getNameHead();
	 BassNameLocation sbn = usedmap.get(key);
	 if (sbn != null) {
	    sbn.addLocation(bl);
	    bn = null;
	  }
	 else usedmap.put(key,bn);
	 break;
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case ANNOTATION :
	 all_names.add(bn);
	 if (showClassFile(bn)) {
	    BassNameLocation fnm = new BassNameLocation(bl,BassNameType.FILE,pfx);
	    all_names.add(fnm);
	 }
	 bn = new BassNameLocation(bl,BassNameType.HEADER,pfx);
	 break;
      case PROJECT :
	 if (all_names.size() != 0) bn = null;
	 break;
      case MODULE :
	 BassNameLocation fnm = new BassNameLocation(bl,BassNameType.FILE,pfx);
	 all_names.add(fnm);
	 BassNameLocation inm = new BassNameLocation(bl,BassNameType.HEADER,pfx);
	 all_names.add(inm);
	 break;
    }

   if (bn != null) all_names.add(bn);

   file_names = null;
}



private boolean showClassFile(BassNameLocation bn)
{
   if (bn.getKey().contains("$")) return false;
   return BoardSetup.getSetup().getLanguage().getShowClassFile();
}




private boolean isRelevant(BumpLocation bl)
{
   switch (bl.getSymbolType()) {
      case PACKAGE :
      case LOCAL :
      case UNKNOWN :
	 return false;
      default:
	 break;
    }

   if (bl.getKey() == null) return false;

   Matcher m = anonclass_pattern.matcher(bl.getKey());
   if (m.find()) return false;

   return true;
}



private class Searcher implements Runnable {

   @Override public void run() {
      loadNames();
    }

   @Override public String toString()		{ return "BASS_LocationSearcher"; }

}	// end of inner class Searcher




/********************************************************************************/
/*										*/
/*	Change detection methods						*/
/*										*/
/********************************************************************************/

@Override public void handleFileChanged(String proj,String file)
{
   addNamesForFile(proj,file,true);

   handleUpdated();
}



@Override public void handleFileAdded(String proj,String file)
{
   addNamesForFile(proj,file,false);

   handleUpdated();
}



@Override public void handleFileRemoved(String proj,String file)
{
   removeNamesForFile(proj,file);

   handleUpdated();
}


@Override public void handleProjectOpened(String proj)
{
   addNamesForFile(proj,null,true);

   handleUpdated();
}




private void removeNamesForFile(String proj,String file)
{
   synchronized (this) {
      for (Iterator<BassName> it = all_names.iterator(); it.hasNext(); ) {
	 BassName bn = it.next();
	 BumpLocation bl = bn.getLocation();
	 if (bl != null && fileMatch(file,bl.getFile()) &&
	       (proj == null || proj.equals(bl.getProject())))
	    it.remove();
       }
      file_names = null;
    }
}

private boolean fileMatch(String file,File blf)
{
   if (file == null) return true;
   if (file.equals(blf.getPath())) return true;
   if (blf.getPath().endsWith(file)) return true;

   boolean fg = false;
   int idx = file.indexOf("/",1);
   if (idx > 0) {
      String f1 = file.substring(idx);
      if (blf.getPath().endsWith(f1)) fg = true;
   }
   int idx1 = file.indexOf("/",idx+1);
   if (!fg && idx1 > 0) {
      String f2 = file.substring(idx1);
      if (blf.getPath().endsWith(f2)) fg = true;
   }
   if (!fg) return false;

   if (blf.exists()) return false;

   return true;
}



private void addNamesForFile(String proj,String file,boolean rem)
{
   Map<String,BassNameLocation> usedmap = new HashMap<>();
   List<String> fls = null;
   if (file != null) {
      fls = new ArrayList<>();
      fls.add(file);
    }

   Collection<BumpLocation> locs = BumpClient.getBump().findAllNames(proj,fls,true,0);

   synchronized (this) {
      if (rem) {
	 removeNamesForFile(proj,file);
       }

      if (locs != null) {
	 for (BumpLocation bl : locs) {
	    addLocation(bl,usedmap);
	  }
       }
    }
   // BoardLog.logD("BASS","AFTER " + proj + " " + file + " " + all_names.size());
}


private void handleUpdated()
{
   for (BassUpdatableRepository br : update_repos) {
      br.reloadRepository();
    }

   BassFactory.reloadRepository(this);
}


}	// end of class BassRepositoryLocation




/* end of BassRepositoryLocation.java */
