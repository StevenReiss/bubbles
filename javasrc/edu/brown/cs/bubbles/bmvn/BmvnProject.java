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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;

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
   basis_files = files;
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

void addButtons(BudaBubble relbbl,Point where,JPopupMenu menu)
{
   for (BmvnModel mdl : project_models) {
      mdl.addButtons(relbbl,where,menu);
    }
}

}       // end of class BmvnProject




/* end of BmvnProject.java */

