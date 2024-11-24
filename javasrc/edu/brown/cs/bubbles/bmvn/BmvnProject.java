/********************************************************************************/
/*                                                                              */
/*              BmvnProject.java                                                */
/*                                                                              */
/*      Maven information for a project                                         */
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BmvnProject implements BmvnConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String project_name;
private Set<File> basis_files;
private List<BmvnModel> project_models;
private Set<File> library_files;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnProject(String name,Set<File> files,Set<File> libs)
{
   project_name = name;
   basis_files = new TreeSet<>(new FileSorter());
   basis_files.addAll(files);
   library_files = libs;
   project_models = new ArrayList<>();
   for (File f : basis_files) {
      for (BmvnTool tool : BmvnTool.values()) {
         if (f.getName().equals(tool.getFileName())) {
            switch (tool) {
               case MAVEN : 
                  project_models.add(new BmvnModelMaven(this,f));
                  break;
               case GRADLE : 
                  project_models.add(new BmvnModelGradle(this,f));
                  break;
               case ANT :
                  project_models.add(new BmvnModelAnt(this,f));
                  break;
               case NPM :
                  project_models.add(new BmvnModelNpm(this,f));
                  break;
               case YAML :
                  project_models.add(new BmvnModelYaml(this,f));
                  break;
             }
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

Set<File> getLibraries()                { return library_files; }



/********************************************************************************/
/*                                                                              */
/*      Project update methods                                                  */
/*                                                                              */
/********************************************************************************/

protected void updateLibraries(Map<File,File> updated,Set<File> added,Set<File> removed)
{
   BumpClient bc = BumpClient.getBump();
   Element pdef = bc.getProjectData(project_name);
   Element cxml = IvyXml.getChild(pdef,"RAWPATH");
   if (cxml == null) cxml = pdef;
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   for (File rem : removed) {
      File rem1 = IvyFile.getCanonical(rem);
      for (Element e : IvyXml.children(cxml,"PATH")) {
         String lib = IvyXml.getTextElement(e,"BINARY");
         String typ = IvyXml.getAttrString(e,"TYPE");
         if (typ == null || !typ.equals("LIBRARY") || lib == null) continue;
         File flib = new File(lib);
         flib = IvyFile.getCanonical(flib);
         if (rem1.equals(flib)) {
            xw.begin("PATH");
            xw.field("DELETE",true);
            xw.field("ID",IvyXml.getAttrInt(e,"ID"));
            xw.end("PATH");
            break;
          }
       }
    }
   for (Map.Entry<File,File> ent : updated.entrySet()) {
      File upd1 = IvyFile.getCanonical(ent.getKey());
      for (Element e : IvyXml.children(cxml,"PATH")) {
         String lib = IvyXml.getTextElement(e,"BINARY");
         String typ = IvyXml.getAttrString(e,"TYPE");
         if (typ == null || !typ.equals("LIBRARY") || lib == null) continue;
         File flib = new File(lib);
         flib = IvyFile.getCanonical(flib);
         if (upd1.equals(flib)) {
            xw.begin("PATH");
            xw.field("ID",IvyXml.getAttrInt(e,"ID"));
            xw.field("MODIFIED",true);
            xw.field("TYPE","LIBRARY");
            xw.field("OPTIONAL",IvyXml.getAttrBool(e,"OPTIONAL"));
            xw.field("EXPORTED",IvyXml.getAttrBool(e,"EXPORTED"));
            xw.textElement("BINARY",ent.getValue().getPath());
            xw.end("PATH");
            break;
          }
       }
    }
   for (File add : added) {
      xw.begin("PATH");
      xw.field("NEW",true);
      xw.field("TYPE","LIBRARY");
      xw.textElement("BINARY",add.getPath());
      xw.end("PATH");
    }
   xw.end("PROJECT");
   bc.editProject(project_name,xw.toString());
   
   xw.close();
   
   updateLibraryFiles();
}




/********************************************************************************/
/*                                                                              */
/*      Maintain library files                                                  */
/*                                                                              */
/********************************************************************************/

protected void updateLibraryFiles()
{
   BumpClient bc = BumpClient.getBump();
   Element pdef = bc.getProjectData(project_name);
   Element cxml = IvyXml.getChild(pdef,"RAWPATH");
   if (cxml == null) cxml = pdef;
   
   Set<File> libs = new LinkedHashSet<>();
   
   for (Element e : IvyXml.children(cxml,"PATH")) {
      String typ = IvyXml.getAttrString(e,"TYPE");
      if (typ == null) continue; 
      switch (typ) {
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
         case "SOURCE" :
            break;
       }
      
    }
   
   library_files = libs;
}



/********************************************************************************/
/*                                                                              */
/*      Add buttons on project explorer menu                                    */
/*                                                                              */
/********************************************************************************/

void addButtons(String name,BudaBubble relbbl,Point where,JPopupMenu menu)
{
   List<BmvnModel> mdls = new ArrayList<>();
   for (BmvnModel mdl : project_models) {
      if (mdl.isRelevant(name)) mdls.add(mdl);
    }
   if (mdls.size() > 1) {
      for (BmvnModel mdl : mdls) {
         String lbl = mdl.getLabel() + " ...";
         JMenu menu1 = new JMenu(lbl);
         mdl.addButtons(name,relbbl,where,menu1);
         menu.add(menu1);
       }
    }
   else {
      for (BmvnModel mdl : mdls) {
         JMenu menu1 = new JMenu();
         mdl.addButtons(name,relbbl,where,menu1);
         for (int i = 0; i < menu1.getItemCount(); ++i) {
            JMenuItem mi = menu1.getItem(i);
            menu.add(mi);
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Sort files for display                                                  */
/*                                                                              */
/********************************************************************************/

private final class FileSorter implements Comparator<File> {
   
   @Override public int compare(File f1,File f2) {
      String p1 = f1.getPath();
      String p2 = f2.getPath();
      return p1.compareTo(p2);
    }
   
}       // end of inner class FileSorter




}       // end of class BmvnProject




/* end of BmvnProject.java */

