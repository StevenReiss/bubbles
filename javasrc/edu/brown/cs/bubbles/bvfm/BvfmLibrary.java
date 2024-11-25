/********************************************************************************/
/*										*/
/*		BvfmLibrary.java						*/
/*										*/
/*	Collection of all current virtual files 				*/
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



package edu.brown.cs.bubbles.bvfm;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.bass.BassConstants.BassUpdatableRepository;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BvfmLibrary implements BvfmConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BvfmVirtualFile,File>	file_set;
private Map<String,BvfmVirtualFile>	file_names;
private File				library_directory;
private BvfmCodeRepository		virtual_code_repo;
private BvfmGroupRepository		virtual_group_repo;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvfmLibrary()
{
   file_set = new HashMap<>();
   file_names = new HashMap<>();

   File wsd = BoardSetup.getBubblesWorkingDirectory();
   library_directory = new File(wsd,"VirtualFiles");
   library_directory.mkdir();

   virtual_code_repo = new BvfmCodeRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_CODE,virtual_code_repo);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,virtual_code_repo);

   virtual_group_repo = new BvfmGroupRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,virtual_group_repo);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BvfmVirtualFile getVirtualFileForName(String nm)
{
   return file_names.get(nm);
}



/********************************************************************************/
/*										*/
/*	Update operations							*/
/*										*/
/********************************************************************************/

void addVirtualFile(BvfmVirtualFile bvf)
{
   String nm = bvf.getName();
   File f = new File(library_directory,nm + ".bvf");
   if (f.exists()) {
      for (int i = 0; ; ++i) {
	 f = new File(library_directory,nm + "_" + i + ".bvf");
	 if (!f.exists()) break;
       }
    }
   file_set.put(bvf,f);
   file_names.put(bvf.getName(),bvf);
   save(bvf);
   updateRepository();
}



void removeVirtualFile(BvfmVirtualFile bvf)
{
   File f = file_set.remove(bvf);
   if (f == null) return;
   f.delete();
   file_names.remove(bvf.getName());
   updateRepository();
}



void editVirtualFile(BvfmVirtualFile bvf)
{
   for (Iterator<BvfmVirtualFile> it = file_names.values().iterator(); it.hasNext(); ) {
      BvfmVirtualFile ovf = it.next();
      if (ovf == bvf) {
	 it.remove();
       }
    }
   file_names.put(bvf.getName(),bvf);
   save(bvf);
   updateRepository();
}



private void save(BvfmVirtualFile bvf)
{
   File f = file_set.get(bvf);
   if (f == null) return;
   try (IvyXmlWriter xw = new IvyXmlWriter(f)) {
      bvf.outputXml(xw);
    }
   catch (IOException e) {
      BoardLog.logE("BVFM","Problem saving virtual file",e);
    }
}



/********************************************************************************/
/*										*/
/*	Repository actions							*/
/*										*/
/********************************************************************************/

private void updateRepository()
{
   BassFactory.reloadRepository(virtual_code_repo);
   BassFactory.reloadRepository(virtual_group_repo);
}




/********************************************************************************/
/*										*/
/*	Load library								*/
/*										*/
/********************************************************************************/

void loadLibrary()
{
   for (File f : library_directory.listFiles(new VirtualFileFilter())) {
      Element xml = IvyXml.loadXmlFromFile(f);
      if (xml != null && IvyXml.isElement(xml,"VIRTUALFILE")) {
	 BvfmVirtualFile bvf = new BvfmVirtualFile(xml);
	 if (bvf.isEmpty() || bvf.getName() == null) continue;
	 file_set.put(bvf,f);
	 file_names.put(bvf.getName(),bvf);
       }
    }
   updateRepository();
}


private static final class VirtualFileFilter implements FileFilter {

   @Override public boolean accept(File f) {
      if (f.isDirectory()) return false;
      if (!f.canRead()) return false;
      if (f.getName().endsWith(".bvf")) return true;
      return false;
    }

}	// end of inner class VirtualFileFilter



/********************************************************************************/
/*										*/
/*	BASS Repository methods for code elements				*/
/*										*/
/********************************************************************************/

private final class BvfmCodeRepository implements BassUpdatableRepository {

   @Override public Iterable<BassName> getAllNames() {
      List<BassName> rslt = new ArrayList<>();
      for (BvfmVirtualFile vf : file_set.keySet()) {
         String s = vf.getCommonLocation();
         if (s == null) continue;
         rslt.add(new CodeName(vf,s));
       }
      return rslt;
    }
   
   @Override public boolean isEmpty() {
      for (BvfmVirtualFile vf : file_set.keySet()) {
         String s = vf.getCommonLocation();
         if (s != null) return false;
       }
      return true;
    }

   @Override public boolean includesRepository(BassRepository br) {
      if (br == this) return true;
      return false;
    }

   @Override public void reloadRepository() {
    }

}	// end of inner class BvfmCodeRepository




/********************************************************************************/
/*										*/
/*	Bass Repository methods for general groups				*/
/*										*/
/********************************************************************************/

private final class BvfmGroupRepository implements BassUpdatableRepository {

   @Override public Iterable<BassName> getAllNames() {
      List<BassName> rslt = new ArrayList<>();
      for (BvfmVirtualFile vf : file_set.keySet()) {
         rslt.add(new GroupName(vf));
       }
      return rslt;
    }
   
   
   @Override public boolean isEmpty()
   {
      return file_set.isEmpty();
   }

   @Override public boolean includesRepository(BassRepository br) {
      if (br == this) return true;
      return false;
    }

   @Override public void reloadRepository() {
    }

}	// end of inner class BvfmGroupRepository


private class GroupName extends BvfmRepoName {

   GroupName(BvfmVirtualFile vf) {
      super(vf);
      name_type = BassNameType.VIRTUAL_FILE;
    }

   @Override public String createPreviewString() {
      return "Virtual file " + virtual_file.getName();
    }

   @Override public  String getKey() {
      // might want to add common prefix to this to allow multiple groups with same name
      return "GROUP@" + virtual_file.getName();
    }

   @Override public  String getSymbolName() {
      return BVFM_GROUP_PREFIX + virtual_file.getName();
    }

}	// end of inner class GroupName

private class CodeName extends BvfmRepoName {

   private String code_prefix;
   
   CodeName(BvfmVirtualFile vf,String pfx) {
      super(vf);
      name_type = BassNameType.VIRTUAL_CODE;
      code_prefix = pfx;
    }
   
   @Override public String createPreviewString() {
      return "Virtual file " + virtual_file.getName();
    }
   
   @Override public  String getKey() {
      // might want to add common prefix to this to allow multiple groups with same name
      return code_prefix + "@GROUP@" + virtual_file.getName();
    }
   
   @Override public  String getSymbolName() {
      return code_prefix + "VirtualFile:" + virtual_file.getName();
    }
   
}	// end of inner class GroupName




}	// end of class BvfmLibrary




/* end of BvfmLibrary.java */

