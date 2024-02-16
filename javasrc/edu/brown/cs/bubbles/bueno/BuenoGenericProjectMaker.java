/********************************************************************************/
/*                                                                              */
/*              BuenoGenericProjectMaker.java                                   */
/*                                                                              */
/*      Code to create projects of various types based on language data         */
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

class BuenoGenericProjectMaker implements BuenoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         type_data;
private String		project_language;

private static final Pattern SSH_PAT = Pattern.compile("\\w+@\\w+(\\.\\w+)*\\:\\w+(\\/\\w+)*");

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoGenericProjectMaker(Element pdata,Element tdata)
{
   project_language = IvyXml.getAttrString(pdata,"LANGUAGE");
   type_data = tdata;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getName()                
{
   return IvyXml.getAttrString(type_data,"NAME");
}


@Override public String toString()
{
   return IvyXml.getAttrString(type_data,"DESCRIPTION");
}

String getLanguage()
{
   return project_language;
}


boolean checkStatus(BuenoProperties props)
{
   for (Element felt : IvyXml.children(type_data,"FIELD")) {
      String pnm = IvyXml.getAttrString(felt,"NAME");
      switch (IvyXml.getAttrString(felt,"TYPE")) {
         case "STRING" :
            String pat = IvyXml.getAttrString(felt,"PATTERN");
            String svl = props.getStringProperty(pnm);
            if (svl == null || svl.isEmpty()) return false;
            if (pat != null) {
               if (!svl.matches(pat)) return false;
             }
            break;
         case "URL" :
            String url = props.getStringProperty(pnm);
            if (url == null || url.length() == 0) return false;
            if (url.contains("@")) {
               if (!SSH_PAT.matcher(url).matches()) return false;
             }
            else if (url.startsWith("/")) {
               File f = new File(url);
               if (!f.exists() || !f.isDirectory() || !url.endsWith(".git")) return false;
             }
            else if (url.contains(":")) {
               try {
                  new URI(url);
                }
               catch (URISyntaxException e) { 
                  return false;
                }
             }
            else return false;
            break;
         case "DIRECTORY" :
            File dir = props.getFile(pnm);
            if (IvyXml.getAttrBool(felt,"EXISTS")) {
               if (dir == null) return false;
               if (!dir.exists()) return false;
               if (!dir.isDirectory()) return false;
             }
            if (IvyXml.getAttrBool(felt,"CANWRITE")) {
               if (dir == null) return false;
               if (dir.exists()) {
                  if (!dir.isDirectory()) return false;
                  if (!dir.canWrite()) return false;
                }
               else {
                  File pdir = dir.getParentFile();
                  if (!pdir.exists()) return false;
                  if (!pdir.canWrite()) return false;
                }
             }
            String cont = IvyXml.getAttrString(felt,"CONTAINS");
            if (cont != null) {
               StringTokenizer tok = new StringTokenizer(cont);
               while (tok.hasMoreTokens()) {
                  String f = tok.nextToken();
                  File nf = new File(dir,f);
                  if (!nf.exists()) return false;
                }
             }
            break; 
       }
    }
   return true;
}



void resetPanel(JPanel jpnl,BuenoProperties props)
{
   SwingGridPanel pnl = (SwingGridPanel) jpnl;
   for (Element felt : IvyXml.children(type_data,"FIELD")) {
      String pnm = IvyXml.getAttrString(felt,"NAME");
      String lbl = IvyXml.getAttrString(felt,"DESCRIPTION");
      switch (IvyXml.getAttrString(felt,"TYPE")) {
         case "STRING" :
         case "URL" :
            JTextField tfld = (JTextField) pnl.getComponentForLabel(lbl);
            if (tfld != null) tfld.setText(props.getStringProperty(pnm));
            break;
         case "DIRECTORY" :
            JTextField dfld = (JTextField) pnl.getComponentForLabel(lbl);
            if (dfld != null) {
               File fdir = props.getFile(pnm);
               if (fdir == null) dfld.setText("");  
               else dfld.setText(fdir.getPath());
             }
            break;
       }
    }
}





/********************************************************************************/
/*                                                                              */
/*      Panel methods                                                           */
/*                                                                              */
/********************************************************************************/

JPanel createPanel(BuenoGenericProject bp,BuenoProperties props)
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   for (Element felt : IvyXml.children(type_data,"FIELD")) {
      String pnm = IvyXml.getAttrString(felt,"NAME");
      switch (IvyXml.getAttrString(felt,"TYPE")) {
         case "STRING" :
         case "URL" :
            TextAction tact = new TextAction(bp,props,pnm,false);
            pnl.addTextField(
                  IvyXml.getAttrString(felt,"DESCRIPTION"),
                  props.getStringProperty(pnm),32,tact,tact); 
            break;
         case "DIRECTORY" :
            TextAction dact = new TextAction(bp,props,pnm,true);
            pnl.addFileField(
                  IvyXml.getAttrString(felt,"DESCRIPTION"),
                  props.getFile(pnm),JFileChooser.DIRECTORIES_ONLY,
                  dact,dact);
            break;
       }
    }
   
   return pnl;
}


private class TextAction implements ActionListener, UndoableEditListener {
 
   private BuenoGenericProject for_project;
   private BuenoProperties bueno_props;
   private String prop_name;
   private boolean for_file;
   
   TextAction(BuenoGenericProject bp,BuenoProperties props,String nm,boolean file) {
      for_project = bp;
      bueno_props = props;
      prop_name = nm;
      for_file = file;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      JTextField tfld = (JTextField) evt.getSource();
      setValue(tfld.getText());
      for_project.checkStatus();
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      Document d = (Document) evt.getSource();
      String txt = null;
      try {
         txt = d.getText(0,d.getLength());
       }
      catch (BadLocationException e) { }
      setValue(txt);
      for_project.checkStatus();
    }
   
   private void setValue(String txt) {
      if (txt == null || txt.isEmpty()) {
         bueno_props.put(prop_name,"");
       }
      else if (for_file) {
         bueno_props.put(prop_name,new File(txt));
       }
      else {
         bueno_props.put(prop_name,txt);
       }
    }
}



}       // end of class BuenoGenericProjectMaker




/* end of BuenoGenericProjectMaker.java */

