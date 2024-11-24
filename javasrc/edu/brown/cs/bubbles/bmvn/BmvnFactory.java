/********************************************************************************/
/*                                                                              */
/*              BmvnFactory.java                                                */
/*                                                                              */
/*      Factory/setup class for bubbles<=>maven interface                       */
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



package edu.brown.cs.bubbles.bmvn;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPopupMenu;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassConstants.BassPopupHandler;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;

public final class BmvnFactory implements BmvnConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,BmvnProject> known_projects;
private List<BmvnTool>          available_tools;

private static BmvnFactory      the_factory = null;





/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/


public static synchronized BmvnFactory getFactory()
{
   if (the_factory == null) the_factory = new BmvnFactory();
   return the_factory;
}


private BmvnFactory()
{
   available_tools = new ArrayList<>();
   Element ld = BumpClient.getBump().getLanguageData();
   Element pd = IvyXml.getChild(ld,"PROJECT");
   Element libs = IvyXml.getChild(pd,"LIBRARIES");
   
   for (BmvnTool tool : BmvnTool.values()) {
      String nm = tool.toString();
      if (IvyXml.getAttrBool(libs,nm)) {
         available_tools.add(tool);
       }
    }
   known_projects = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
// BudaRoot.addBubbleConfigurator("BMVN",new BmvnConfigurator());
}



public static void initialize(BudaRoot br)
{
   BumpClient bc = BumpClient.getBump();
   boolean usemvn = bc.getOptionBool("bubbles.useMaven",false);
   if (!usemvn) return;
   getFactory().setupModels();
   getFactory().setupCallbacks();
}


/********************************************************************************/
/*                                                                              */
/*      MavenSetup -- class to look for maven files                             */
/*                                                                              */
/********************************************************************************/

private void setupModels()
{
   BoardThreadPool.start(new ModelSetup());
}



private final class ModelSetup implements Runnable {
   
   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      Element pelt = bc.getAllProjects();
      for (Element pe : IvyXml.children(pelt,"PROJECT")) {
         String pnm = IvyXml.getAttrString(pe,"NAME");
         Set<File> files = new HashSet<>();
         Set<File> libs = new LinkedHashSet<>();
         Element pdef = bc.getProjectData(pnm);
         Element cxml = IvyXml.getChild(pdef,"RAWPATH");
         if (cxml == null) cxml = pdef;
         for (Element e : IvyXml.children(cxml,"PATH")) {
            String typ = IvyXml.getAttrString(e,"TYPE");
            if (typ == null) continue;
            switch (typ) {
               case "SOURCE" :
                  String src = IvyXml.getTextElement(e,"SOURCE");
                  if (src != null) {
                     File f = new File(src);
                     while (f.exists() && f.isDirectory() && f.canWrite()) {
                        boolean fnd = false;
                        for (BmvnTool tool : available_tools) {
                           fnd |= addFiles(tool,f,true,files);
                         }
                        if (fnd) break;
                        f = f.getParentFile();
                      }
                   }
                  break;
               case "LIBRARY" :
                  String lib = IvyXml.getTextElement(e,"BINARY");
                  if (lib != null) {
                     File flib = new File(lib);
                     if (flib.exists() && flib.canRead()) {
                        flib = IvyFile.getCanonical(flib);
                        libs.add(flib);
                      }
                   }
                  break;
               default :
               case "BINARY" :
                  break;
             }
           
          }
         if (files.isEmpty()) continue;
         BmvnProject bp = new BmvnProject(pnm,files,libs);
         known_projects.put(pnm,bp);
       }
    }
   
   private boolean addFiles(BmvnTool tool,File dir,boolean top,Set<File> files) {
      boolean fnd = false;
      String fnm = tool.getFileName();
      File f1 = new File(dir,fnm);
      if (f1.exists() && f1.canRead() && f1.canWrite()) {
         f1 = IvyFile.getCanonical(f1);
         files.add(f1);
         fnd = true;
       }
      if (tool.useSubdirectories()) {
         for (File subd : dir.listFiles()) {
            // skip links?
            if (subd.isDirectory() && subd.canRead() && subd.canWrite()) {
               fnd |= addFiles(tool,subd,false,files);
             }
          }
       }
      return fnd;
    }
   
}       // end of inner class MavenSetup



/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                        */
/*                                                                              */
/********************************************************************************/

private void setupCallbacks()
{
   BmvnButtons btns = new BmvnButtons();
   BassFactory.getFactory().addPopupHandler(btns);
}


private final class BmvnButtons implements BassPopupHandler {
   
   @Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,
         String name,BassName forname) {
      String pnm = null;
      if (forname == null) {
         if (name.contains("@")) return;
         if (name.contains("<")) return;
         int idx = name.indexOf(":");
         if (idx > 0) {
            pnm = name.substring(0,idx);
            name = name.substring(idx+1);
            if (!name.isEmpty()) {
               List<BumpLocation> locs = BumpClient.getBump().findPackage(pnm,name);
               if (locs == null || locs.isEmpty()) return;
             }
          }
       }
      else {
         pnm = forname.getProject();
         switch (forname.getNameType()) {
            case PACKAGE :
            case PROJECT :
               name = forname.getName();
               break;
            default :
               return;
          }
       }
      if (pnm == null) return;
      BmvnProject bp = known_projects.get(pnm);
      if (bp == null) return;
      bp.addButtons(name,bb,where,menu);
    }
   
}       // end of inner class BmvnButtons



}       // end of class BmvnFactory




/* end of BmvnFactory.java */

