/********************************************************************************/
/*										*/
/*		RebaseWordFactory.java						*/
/*										*/
/*	Controller for using word bags						*/
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



package edu.brown.cs.bubbles.rebase.word;

import edu.brown.cs.bubbles.rebase.RebaseMain;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;


public class RebaseWordFactory implements RebaseWordConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseWordBag		all_words;
private RebaseWordBag		loaded_words;
private RebaseWordBag		accepted_words;
private RebaseWordBag		rejected_words;
private File			base_file;
private boolean 		save_needed;

private static EnumSet<WordOptions>	word_options;
private static EnumSet<TermOptions>	term_options;
private static RebaseWordSpellCheck	spell_check = null;

private static RebaseWordFactory word_factory = null;

private static Set<String>		stop_words;
private static Map<String,String>	short_words;
private static Set<String>		dictionary_words;

private static double		ACCEPT_VALUE = 1.0;
private static double		LOADED_VALUE = 0.50;
private static double		REJECT_VALUE = 0.50;
private static double		WORD_CUTOFF = 0.33;
private static int		MAX_WORDS = 6;
private static double		MIN_DOC_FRACTION = 0.001;


static {
   word_options = EnumSet.allOf(WordOptions.class);
   word_options.remove(WordOptions.SPELLING);
   setupWordSets();
   term_options = EnumSet.of(TermOptions.TERM_LOG,TermOptions.WORDS_ONLY);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static RebaseWordFactory getFactory()
{
   if (word_factory == null) {
      word_factory = new RebaseWordFactory();
    }
   return word_factory;
}


private RebaseWordFactory()
{
   RebaseMain rm = RebaseMain.getRebase();
   File f1 = rm.getWorkspaceDirectory();
   base_file = new File(f1,"allwords.bag.zip");
   all_words = new RebaseWordBag();
   try {
      all_words.inputBag(base_file);
    }
   catch (IOException e) { }
   save_needed = false;
   loaded_words = new RebaseWordBag();
   accepted_words = new RebaseWordBag();
   rejected_words = new RebaseWordBag();
}



/********************************************************************************/
/*										*/
/*     Basic word bag update methods						*/
/*										*/
/********************************************************************************/

public void loadSource(String text,boolean cached)
{
   RebaseWordBag tmp = new RebaseWordBag(text);
   if (!cached) {
      all_words.addWords(tmp);
      save_needed = true;
    }
   loaded_words.addWords(tmp);
}


public void addAccept(String text)
{
   accepted_words.addWords(text);
}

public void removeAccept(String text)
{
   RebaseWordBag bag = new RebaseWordBag(text);

   accepted_words.removeWords(bag);
}


public void addReject(String text)
{
   rejected_words.addWords(text);
}


public void clear()
{
   loaded_words = new RebaseWordBag();
   accepted_words = new RebaseWordBag();
   rejected_words = new RebaseWordBag();
}


public void save()
{
   if (save_needed) {
      try {
	 all_words.outputBag(base_file);
       }
      catch (IOException e) { }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to use the computed word bags					*/
/*										*/
/********************************************************************************/

public List<String> getQuery()
{
   Map<String,Double> acc = accepted_words.computeTF(term_options);
   Map<String,Double> rej = rejected_words.computeTF(term_options);
   Map<String,Double> ldd = loaded_words.computeTF(term_options);
   Map<String,Double> idf = all_words.computeIDF(MIN_DOC_FRACTION);
   SortedSet<PotentialWord> wds = new TreeSet<PotentialWord>();

   double maxv = 0;
   for (Map.Entry<String,Double> ent : ldd.entrySet()) {
      String wd = ent.getKey();
      double v1 = getValue(wd,acc);
      double v2 = getValue(wd,rej);
      double v3 = ldd.get(wd);
      double v4 = getValue(wd,idf);
      double v = (v1 * ACCEPT_VALUE + v3 * LOADED_VALUE - v2 * REJECT_VALUE) * v4;
      boolean wfg = dictionary_words.contains(wd);

      RebaseMain.logD("Q: " + wd + " => " + v1 + " " + v2 + " " + v3 + " " + v4 + " " +
			 all_words.getDocumentCount(wd) + " " + all_words.getCount(wd) + " " +
			 wfg + " " + v);

      if (term_options.contains(TermOptions.WORDS_ONLY) && !wfg) continue;
      if (v <= 0) continue;
      maxv = Math.max(maxv,v);
      wds.add(new PotentialWord(wd,v));
    }

   List<String> rslt = new ArrayList<String>();
   int ct = 0;
   for (PotentialWord pw : wds) {
      if (pw.getValue() > maxv * WORD_CUTOFF && ct++ < MAX_WORDS) {
	 rslt.add(pw.getWord());
       }
      else break;
    }

   return rslt;
}



public void getQuery(IvyXmlWriter xw)
{
   List<String> rslt = getQuery();
   if (rslt == null) return;

   for (String s : rslt) {
      xw.textElement("WORD",s);
    }
}


private double getValue(String key,Map<String,Double> map)
{
   Double dv = map.get(key);
   if (dv == null) return 0;
   return dv.doubleValue();
}


private static class PotentialWord implements Comparable<PotentialWord> {

   private String word_name;
   private double word_value;

   PotentialWord(String wd,double vl) {
      word_name = wd;
      word_value = vl;
    }

   @Override public int compareTo(PotentialWord w) {
      double v = word_value - w.word_value;
      if (v < 0) return 1;
      if (v > 0) return -1;
      return 0;
    }

   String getWord()			{ return word_name; }
   double getValue()			{ return word_value; }

   @Override public String toString() {
      return word_name + "=>" + word_value;
   }

}	// end of inner class PotentialWord





/********************************************************************************/
/*										*/
/*	Word option methods							*/
/*										*/
/********************************************************************************/

EnumSet<WordOptions> getWordOptions()
{
   return EnumSet.copyOf(word_options);
}


void setWordOptions(EnumSet<WordOptions> opts)
{
   word_options = EnumSet.copyOf(opts);
}


EnumSet<TermOptions> getTermOption()
{
   return EnumSet.copyOf(term_options);
}

void setTermOption(EnumSet<TermOptions> opts)
{
   term_options = EnumSet.copyOf(opts);
}



/********************************************************************************/
/*										*/
/*	Word splitting methods							*/
/*										*/
/********************************************************************************/

static Collection<String> getCandidateWords(RebaseWordStemmer stm,String text,int off,int len)
{
   if (len < 3 || len > 32) return null;

   int [] breaks = new int[32];
   int breakct = 0;

   char prev = 0;
   for (int i = 0; i < len; ++i) {
      char ch = text.charAt(off+i);
      if (word_options.contains(WordOptions.SPLIT_CAMELCASE)) {
	 if (Character.isUpperCase(ch) && Character.isLowerCase(prev)) {
	    breaks[breakct++] = i;
	  }
       }
      if (word_options.contains(WordOptions.SPLIT_NUMBER)) {
	 if (Character.isDigit(ch) && !Character.isDigit(prev) && i > 0) {
	    breaks[breakct++] = i;
	  }
	 else if (Character.isDigit(prev) && !Character.isDigit(ch)) {
	    breaks[breakct++] = i;
	  }
       }
      if (word_options.contains(WordOptions.SPLIT_UNDERSCORE)) {
	 if (ch == '_') {
	    breaks[breakct++] = i;
	  }
       }
      prev = ch;
    }

   if (stm == null) stm = new RebaseWordStemmer();
   List<String> rslt = new ArrayList<String>();

   // first use whole word
   addCandidateWords(stm,text,off,len,rslt);

   if (breakct > 0) {
      int lbrk = 0;
      for (int i = 0; i < breakct; ++i) {
	 if (breaks[i] - lbrk >= 3) {
	    addCandidateWords(stm,text,off+lbrk,breaks[i]-lbrk,rslt);
	  }
	 lbrk = breaks[i];
       }
      addCandidateWords(stm,text,off+lbrk,len-lbrk,rslt);
    }

   return rslt;
}


private static void addCandidateWords(RebaseWordStemmer stm,String text,int off,int len,List<String> rslt)
{
   if (len < 3) return;

   String wd1 = text.substring(off,off+len);
   String wd0 = wd1.toLowerCase();
   addCandidateWord(wd0,rslt);

   String wd = wd0;
   if (word_options.contains(WordOptions.STEM) ||
	 word_options.contains(WordOptions.STEM_DICTIONARY)) {
      for (int i = 0; i < len; ++i) {
	 stm.add(text.charAt(off+i));
       }
      wd = stm.stem();	  // stem and convert to lower case
      if (word_options.contains(WordOptions.STEM_DICTIONARY)) {
	 if (!wd0.equals(wd)) {
	    if (!dictionary_words.contains(wd)) wd = wd0;
	  }
       }
      if (!wd0.equals(wd)) {
	 // System.err.println("STEM " + wd0 + " => " + wd);
	 addCandidateWord(wd,rslt);
       }
    }

   if (word_options.contains(WordOptions.PLURAL)) {
      String wdsp = RebaseWordPluralFilter.findSingular(wd0);
      if (wdsp != null && !wdsp.equals(wd0) && !wdsp.equals(wd)) {
	 addCandidateWord(wdsp,rslt);
       }
    }

   if (word_options.contains(WordOptions.SPELLING)) {
      String wd2 = getMisspelling(wd0);
      if (wd2 != null && !wd2.equals(wd0)) addCandidateWord(wd2,rslt);
    }

   if (word_options.contains(WordOptions.SPLIT_COMPOUND)) {
      if (!dictionary_words.contains(wd0) && !dictionary_words.contains(wd) &&
	    wd0.equals(wd1)) {
	 for (int i = 3; i < len-3; ++i) {
	    String s1 = wd0.substring(0,i);
	    String s2 = wd0.substring(i);
	    if (dictionary_words.contains(s1) || short_words.containsKey(s1)) {
	       if (dictionary_words.contains(s2) || short_words.containsKey(s2)) {
		  if (!s1.equals(wd)) {
		     addCandidateWord(s1,rslt);
		     addCandidateWord(s2,rslt);
		   }
		}
	     }
	  }
       }
    }
}





private static void addCandidateWord(String wd,List<String> rslt)
{
   if (stop_words.contains(wd)) return;
   if (wd.length() < 3 || wd.length() > 24) return;

   rslt.add(wd);

   if (word_options.contains(WordOptions.VOWELLESS)) {
      String nwd = short_words.get(wd);
      if (nwd != null) rslt.add(nwd);
    }
}



static String getMisspelling(String wd)
{
   if (dictionary_words.contains(wd)) return null;

   synchronized (RebaseWordFactory.class) {
      if (spell_check == null) {
	 spell_check = new RebaseWordSpellCheck();
	 for (String s : dictionary_words) {
	    spell_check.addWord(s);
	  }
       }
    }

   return spell_check.findBestSpelling(wd);
}


/********************************************************************************/
/*										*/
/*	Create programmer abbreviations of common words 			*/
/*										*/
/********************************************************************************/

private static void setupWordSets()
{
   stop_words = new HashSet<String>();
   String wds = "a,able,about,across,after,all,almost,also,am,among,an,and,any,are,as,at," +
   "be,because,been,but,by,can,cannot,could,dear,did,do,does,either,else,ever,every," +
   "for,from,get,got,had,has,have,he,her,hers,him,his,how,however,i,if,in,into,is,it,its," +
   "just,least,let,like,likely,may,me,might,most,must,my,neither,no,nor,not," +
   "of,off,often,on,only,or,other,our,own,rather,said,say,says,she,should,since,so,some," +
   "than,that,the,their,them,then,there,these,they,this,tis,to,too,twas,us," +
   "wants,was,we,were,what,when,where,which,while,who,whom,why,will,with,would," +
   "yet,you,your";

   String keys = "abstract,break,boolean,byte,case,catch,char,class,const,continue," +
   "default,do,double,else,enum,extends,false,final,finally,float,for,goto,if," +
   "implements,import,instanceof,int,interface,long,native,new,null,package,private," +
   "protected,public,return,short,static,super,switch,synchronized,this,throw,throws," +
   "true,try,void,while,java,com,org,javax";

   for (StringTokenizer tok = new StringTokenizer(wds," ,"); tok.hasMoreTokens(); ) {
      stop_words.add(tok.nextToken());
    }
   for (StringTokenizer tok = new StringTokenizer(keys," ,"); tok.hasMoreTokens(); ) {
      stop_words.add(tok.nextToken());
    }

   dictionary_words = new HashSet<String>();
   short_words = new HashMap<String,String>();
   HashSet<String> fnd = new HashSet<String>();

   String root = System.getProperty("edu.brown.cs.bubbles.rebase.ROOT");
   File f1 = new File(root);
   File f2 = new File(f1,"lib");
   File f = new File(f2,WORD_LIST_FILE);

   try (BufferedReader br = new BufferedReader(new FileReader(f))) {
      for ( ; ; ) {
	 String wd = br.readLine();
	 if (wd == null) break;
	 if (wd.contains("'") || wd.contains("-")) continue;
	 if (wd.length() < 3 || wd.length() > 24) continue;
	 wd = wd.toLowerCase();
	 dictionary_words.add(wd);
	 String nwd = wd.replaceAll("[aeiou]","");
	 if (!nwd.equals(wd) && nwd.length() >= 3) {
	    if (fnd.contains(nwd)) {
	       short_words.remove(nwd);
	     }
	    else {
	       fnd.add(nwd);
	       short_words.put(nwd,wd);
	     }
	  }
       }
    }
   catch (IOException e) {
      RebaseMain.logE("Problem reading word file",e);
    }
}



}	// end of class RebaseWordFactory




/* end of RebaseWordFactory.java */

