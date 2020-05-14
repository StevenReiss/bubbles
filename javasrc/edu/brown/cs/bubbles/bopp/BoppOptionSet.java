/********************************************************************************/
/*										*/
/*		BoppOptionSet.java						*/
/*										*/
/*	Hold the various options and provide access to them			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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

package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 *
 * This class is the options panel, displaying all the preferences that can be
 * changed by the user.
 *
 **/

class BoppOptionSet implements BoppConstants, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,List<BoppOptionNew>> tab_map;
private List<BoppOptionBase> all_options;
private Map<String,String> changed_options;
private BudaRoot  buda_root;
private boolean doing_add;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppOptionSet(BudaRoot br)
{
   tab_map = new HashMap<String,List<BoppOptionNew>>();
   all_options = new ArrayList<BoppOptionBase>();
   changed_options = new LinkedHashMap<String,String>();
   buda_root = br;
   doing_add = true;

   Element xml = IvyXml.loadXmlFromStream(BoardProperties.getLibraryFile(PREFERENCES_XML_FILENAME_NEW));
   for (Element op : IvyXml.children(xml,"PACKAGE")) {
      loadXmlPackage(op);
    }

   for (BoppOptionBase op : all_options) op.setOptionSet(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Collection<String> getTabNames()
{
   return new ArrayList<String>(tab_map.keySet());
}


List<BoppOptionNew> getOptionsForTab(String tab)
{
   return tab_map.get(tab);
}


void doingAdd(boolean fg)
{
   doing_add = fg;
}



/********************************************************************************/
/*										*/
/*	Search Methods								*/
/*										*/
/********************************************************************************/

List<BoppOptionNew> search(String text)
{
   String[] words = text.split(" ");
   Pattern[] patterns = new Pattern[words.length];
   List<BoppOptionNew> rslt = new ArrayList<BoppOptionNew>();

   for (int i = 0; i < words.length; i++) {
      try {
	 patterns[i] = (Pattern.compile(words[i], Pattern.CASE_INSENSITIVE));
       }
      catch (PatternSyntaxException e) {
	 patterns[i] = null;
       }
    }
   for (BoppOptionNew opt : all_options) {
      if (opt.search(patterns)) rslt.add(opt);
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void saveOptions()
{
   Set<String> pkgs = new HashSet<String>();
   for (String k : changed_options.keySet()) {
      int idx = k.indexOf("@");
      if (idx < 0) continue;
      String pkg = k.substring(0,idx);
      pkgs.add(pkg);
    }
   for (String p : pkgs) {
      BoardProperties bp = BoardProperties.getProperties(p);
      try {
	 if (bp != null) bp.save();
       }
      catch (IOException e) {
	 BoardLog.logE("BOPP","Problem saving properties",e);
       }
    }
   changed_options.clear();
}


void revertOptions()
{
   for (Map.Entry<String,String> ent : changed_options.entrySet()) {
      String key = ent.getKey();
      int idx = key.indexOf("@");
      if (idx < 0) continue;
      String pkg = key.substring(0,idx);
      String prop = key.substring(idx+1);
      String val = ent.getValue();
      BoardProperties bp = BoardProperties.getProperties(pkg);
      if (bp == null) continue;
      if (val == null) bp.remove(prop);
      else bp.setProperty(prop,val);
    }
   changed_options.clear();

   for (BoppOptionBase opt : all_options) {
      opt.reset();
    }
}



void noteChange(String pkg,String prop)
{
   if (doing_add) return;

   String key = pkg + "@" + prop;

   if (changed_options.containsKey(key)) return;

   String val = null;
   BoardProperties bp = BoardProperties.getProperties(pkg);
   val = bp.getProperty(prop);

   changed_options.put(key,val);
}


void finishChanges()
{
   if (buda_root != null && !doing_add) {
      buda_root.handlePropertyChange();
      buda_root.repaint();
    }
}



/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

private void loadXmlPackage(Element px)
{
   String pname = IvyXml.getAttrString(px,"NAME");
   for (Element op : IvyXml.children(px,"OPT")) {
      loadXmlOption(op,pname);
    }
}



private void loadXmlOption(Element ox,String pkgname)
{
   BoppOptionBase bopt = BoppOptionBase.getOption(pkgname,ox);
   if (bopt == null) return;
   all_options.add(bopt);
   for (String tnm : bopt.getOptionTabs()) {
      List<BoppOptionNew> lopt = tab_map.get(tnm);
      if (lopt == null) {
	 lopt = new ArrayList<BoppOptionNew>();
	 tab_map.put(tnm,lopt);
       }
      lopt.add(bopt);
    }
}




}	// end of class BoppOptionSet




/* end of BoppOptionSet.java */
