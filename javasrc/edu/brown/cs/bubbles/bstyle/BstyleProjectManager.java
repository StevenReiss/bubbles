/********************************************************************************/
/*                                                                              */
/*              BstyleProjectManager.java                                       */
/*                                                                              */
/*      Keep track of projects and their files                                  */
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;

class BstyleProjectManager implements BstyleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BstyleMain      bstyle_main;
private BstyleFileManager file_manager;
private Set<BstyleFile> project_files;
private List<String> all_projects;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleProjectManager(BstyleMain bm,BstyleFileManager fm)
{
   bstyle_main = bm;
   file_manager = fm;
   project_files = new HashSet<>();
   all_projects = new ArrayList<>();
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

List<BstyleFile> getAllFiles(String proj)
{
   List<BstyleFile> files = new ArrayList<>();
   for (BstyleFile bf : project_files) {
      if (bf.getProject() != null && proj != null &&
            proj.equals(bf.getProject())) {
         files.add(bf); 
       }
    }
   return files;
}



BstyleFile addFile(String project,String filename)
{ 
   File f1 = new File(filename);
   f1 = IvyFile.getCanonical(f1);
   BstyleFile bf = file_manager.findFile(f1);
   if (bf == null) {
      bf = file_manager.addFile(project,filename,false);
      if (bf != null) project_files.add(bf);
    }
   else {
      bf = null;
    }
   
   return bf;
}



BstyleFile removeFile(String project,String filename)
{
   File f1 = new File(filename);
   f1 = IvyFile.getCanonical(f1);
   BstyleFile bf = file_manager.findFile(f1);
   if (bf != null) {
      file_manager.removeFile(bf); 
      project_files.remove(bf);
    }
   
   return bf;
}




/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void processAllProjects()
{
   for (String s : all_projects) {
      processProject(s);
    }
}



void processProject(String proj)
{
   List<BstyleFile> files = getAllFiles(proj);
   if (files.isEmpty()) return;
   
   bstyle_main.getStyleChecker().processProject(proj,files);
}



/********************************************************************************/
/*                                                                              */
/*     Set up methods                                                           */
/*                                                                              */
/********************************************************************************/

void setup()
{
   project_files = new HashSet<>();
   Set<File> done = new HashSet<>();
   
   Element projs = bstyle_main.sendCommandWithXmlReply("PROJECTS",
         null,null,null); 
   for (Element proj : IvyXml.children(projs,"PROJECT")) {
      String projnm = IvyXml.getAttrString(proj,"NAME");
      IvyLog.logD("BSTYLE","Setup project " + projnm);
      all_projects.add(projnm);
      CommandArgs args = new CommandArgs("CLASSES",false,
            "FILES",true,
            "OPTIONS",false);
      Element pinfo = bstyle_main.sendCommandWithXmlReply("OPENPROJECT",
            projnm,args,null);
      Element pdata = IvyXml.getChild(pinfo,"PROJECT");
      Element files = IvyXml.getChild(pdata,"FILES");
      for (Element finfo : IvyXml.children(files,"FILE")) {
         if (!IvyXml.getAttrBool(finfo,"SOURCE")) continue;
         String fpath = IvyXml.getAttrString(finfo,"PATH");
         // check for valid file extension
         File f1 = new File(fpath);
         f1 = IvyFile.getCanonical(f1);
         if (!done.add(f1)) continue;
         boolean isopen = IvyXml.getAttrBool(finfo,"ISOPEN");
         IvyLog.logD("BSTYLE","Project file " + fpath + " " + isopen);
         BstyleFile bf = file_manager.addFile(projnm,fpath,isopen);
         if (bf != null) project_files.add(bf);
       }
    }
}




}       // end of class BstyleProjectManager




/* end of BstyleProjectManager.java */

