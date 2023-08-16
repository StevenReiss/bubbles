/********************************************************************************/
/*                                                                              */
/*              BuenoGenericProjectEditor.java                                  */
/*                                                                              */
/*      Generic project editor based on language data                           */
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



package edu.brown.cs.bubbles.bueno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BuenoGenericProjectEditor implements BuenoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         project_data;
private Set<BuenoPathEntry> library_paths;
private Set<BuenoPathEntry> source_paths;
private Set<BuenoPathEntry> initial_paths; 
private Set<String>    ref_projects;
private Set<String>     other_projects;
private Map<String,String> option_elements;
private List<BuenoGenericEditorPanel> editor_panels;
private Map<String,String> start_options;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoGenericProjectEditor(Element edata,Element projdata)
{
   project_data = projdata;
   editor_panels = new ArrayList<>();
   
   String name = IvyXml.getAttrString(project_data,"NAME");
   library_paths = new HashSet<>();
   source_paths = new HashSet<>();
   initial_paths = new HashSet<>();
   Element cxml = IvyXml.getChild(project_data,"RAWPATH");
   for (Element e : IvyXml.children(cxml,"PATH")) {
      BuenoPathEntry pe = new BuenoPathEntry(e);
      initial_paths.add(pe);
      if (!pe.isNested() && pe.getPathType() == PathType.LIBRARY) {
	 library_paths.add(pe);
       }
      else if (!pe.isNested() && pe.getPathType() == PathType.SOURCE) {
	 source_paths.add(pe);
       }
    }
   
   option_elements = new HashMap<>();
   for (Element e : IvyXml.children(project_data,"OPTION")) {
      String k = IvyXml.getAttrString(e,"NAME");
      String v = IvyXml.getAttrString(e,"VALUE");
      if (k != null && v != null) option_elements.put(k,v);
      BoardLog.logD("BUENO","Set option " + k + " = " + v);
    }
   for (Element e : IvyXml.children(project_data,"PROPERTY")) {
      String q = IvyXml.getAttrString(e,"QUAL");
      String n = IvyXml.getAttrString(e,"NAME");
      String v = IvyXml.getAttrString(e,"VALUE");
      if (q != null && n != null && v != null) option_elements.put(q + "." + n,v);
    }
   start_options = new HashMap<>(option_elements);
   
   Set<String> refby = new HashSet<>();
   ref_projects = new HashSet<>();
   for (Element e : IvyXml.children(project_data,"REFERENCES")) {
      String ref = IvyXml.getText(e);
      ref_projects.add(ref);
    }
   for (Element e : IvyXml.children(project_data,"USEDBY")) {
      String ref = IvyXml.getText(e);
      refby.add(ref);
    }
   
   other_projects = new TreeSet<String>();
   BumpClient bc = BumpClient.getBump();
   Element allproj = bc.getAllProjects();
   if (allproj != null) {
      for (Element pe : IvyXml.children(allproj,"PROJECT")) {
         String pnm = IvyXml.getAttrString(pe,"NAME");
         if (pnm.equals(name)) continue;
         if (refby.contains(pnm)) continue;
         other_projects.add(pnm);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Create project description for editing                                  */
/*                                                                              */
/********************************************************************************/

void saveProject()
{
   boolean chng = false;
   for (BuenoGenericEditorPanel pnl : editor_panels) {
      chng |= pnl.hasChanged();
    }
   if (!chng) return;
   for (BuenoGenericEditorPanel pnl : editor_panels) {
      pnl.doUpdate();
    }
   
   Set<BuenoPathEntry> dels = new HashSet<BuenoPathEntry>(initial_paths);
   
   String pnm = IvyXml.getAttrString(project_data,"NAME");
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PROJECT");
   xw.field("NAME",pnm);
   for (BuenoPathEntry pe : library_paths) {
      if (pe.resetChanged()) pe.outputXml(xw,false);
      dels.remove(pe);
    }
   for (BuenoPathEntry pe : source_paths) {
      if (pe.resetChanged()) pe.outputXml(xw,false);
      dels.remove(pe);
    }
   for (BuenoPathEntry pe : dels) {
      pe.outputXml(xw,true);
    }
   
   for (Map.Entry<String,String> ent : option_elements.entrySet()) {
      String k = ent.getKey();
      String v = ent.getValue();
      if (k == null || v == null) continue;
      if (start_options != null) {
         String ov = start_options.get(k);
         if (v.equals(ov)) continue;
       }
      chng = true;
      xw.begin("OPTION");
      xw.field("NAME",k);
      xw.field("VALUE",v);
      xw.end("OPTION");
    }
   
   xw.begin("REFERENCES");
   for (String s : ref_projects) {
      xw.textElement("PROJECT",s);
    }
   xw.end("REFERENCES");
   
   xw.end("PROJECT");
   
   BumpClient bc = BumpClient.getBump();
   
   bc.editProject(pnm,xw.toString());
   
   initial_paths.removeAll(dels);
   initial_paths.addAll(source_paths);
   initial_paths.addAll(library_paths);
}


/********************************************************************************/
/*                                                                              */
/*      Generate a panel for a particular tab                                   */
/*                                                                              */
/********************************************************************************/

BuenoGenericEditorPanel generateTabPanel(Element tabxml)
{
   BuenoGenericEditorPanel pnl = new BuenoGenericEditorPanel(this,tabxml);
   editor_panels.add(pnl);
   return pnl;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

Set<BuenoPathEntry> getLibraryPaths()
{
   return library_paths; 
}


Set<BuenoPathEntry> getSourcePaths()
{
   return source_paths;
}


Set<String> getOtherProjects()
{
   return other_projects;
}


Set<String> getReferencedProjects()
{
   return ref_projects;
}


Map<String,String> getOptions()
{
   return option_elements;
}



}       // end of class BuenoGenericProjectEditor




/* end of BuenoGenericProjectEditor.java */

