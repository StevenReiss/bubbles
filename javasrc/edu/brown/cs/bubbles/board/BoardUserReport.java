/********************************************************************************/
/*                                                                              */
/*              BoardUserReport.java                                            */
/*                                                                              */
/*      Handle user feedback reports with random frequency                      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BoardUserReport implements BoardConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,ReportData>  report_map;

private static Random random_gen = new Random();
private static BoardUserReport  user_report = new BoardUserReport();




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BoardUserReport()
{
   report_map = new HashMap<String,ReportData>();
   
   loadReports();
   loadCounts();
   
   user_report = this;
}




/********************************************************************************/
/*                                                                              */
/*      Triggering methods                                                      */
/*                                                                              */
/********************************************************************************/

public static void noteReport(String name)
{
   if (user_report != null) user_report.noteReportImpl(name);
}


public static void forceReport(String name)
{
   if (user_report != null) user_report.forceReportImpl(name);
}


private void noteReportImpl(String name)
{
   ReportData rd = report_map.get(name);
   if (rd == null) return;
   
   if (rd.triggerReport()) {
      showReport(rd);
    }
   
   saveCount(rd);
}



private void forceReportImpl(String name)
{
   ReportData rd = report_map.get(name);
   if (rd == null) return;
   
   showReport(rd);
   rd.setCounter(0);
   
   saveCount(rd);
}



private void saveCount(ReportData rd)
{
   
   BoardProperties bp = BoardProperties.getProperties("Board");
   String pnm = "Board.user.report." + rd.getName();
   bp.setProperty(pnm,rd.getCounter());
   try {
      bp.save();
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*                                                                              */
/*      Report dialog generator                                                 */
/*                                                                              */
/********************************************************************************/

private void showReport(ReportData rd)
{
   SwingGridPanel pnl = rd.getPanel();
   JDialog dlg = new JDialog();
   dlg.setTitle("User Experience Questionaire");
   dlg.setModal(false);
   dlg.setContentPane(pnl);
   dlg.pack();
   dlg.setVisible(true);
}



/********************************************************************************/
/*                                                                              */
/*      Methods to load reports                                                 */
/*                                                                              */
/********************************************************************************/

private void loadReports()
{
   BoardSetup bs = BoardSetup.getSetup();
   Element xml = IvyXml.loadXmlFromFile(bs.getLibraryPath("reports.xml"));
   if (xml == null) return;
   for (Element rpt : IvyXml.children(xml,"REPORT")) {
      ReportData rd = new ReportData(rpt);
      if (rd.getName() != null) {
         report_map.put(rd.getName(),rd);
       }
    }
}



private void loadCounts()
{
   BoardProperties bp = BoardProperties.getProperties("Board");
   
   for (Map.Entry<String,ReportData> ent : report_map.entrySet()) {
      String rnm = ent.getKey();
      String pnm = "Board.user.report." + rnm;
      int ctr = bp.getInt(pnm);
      ent.getValue().setCounter(ctr);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Data about a particular report                                          */
/*                                                                              */
/********************************************************************************/

private static class ReportData implements ActionListener {
   
   private String report_name;
   private String send_to;
   private int    min_uses;
   private int    max_uses;
   private List<ReportField> report_fields;
   private int    cur_counter;
   
   ReportData(Element xml) {
      report_name = IvyXml.getAttrString(xml,"NAME");
      min_uses = IvyXml.getAttrInt(xml,"MIN",10);
      max_uses = IvyXml.getAttrInt(xml,"MAX",100);
      cur_counter = 0;
      send_to = IvyXml.getAttrString(xml,"SENDTO");
      if (send_to == null) send_to = "spr+" + report_name + "@cs.brown.edu";
      report_fields = new ArrayList<>();
      for (Element fld : IvyXml.children(xml)) {
         ReportField rfld = null;
         if (IvyXml.isElement(fld,"LABEL")) {
            rfld = new LabelField(fld);
          }
         else if (IvyXml.isElement(fld,"SEPARATOR")) {
            rfld = new SeparatorField(fld);
          }
         else if (IvyXml.isElement(fld,"CHOICE")) {
            rfld = new ChoiceField(fld);
          }
         else if (IvyXml.isElement(fld,"TEXTAREA")) {
            rfld = new TextAreaField(fld);
          }
         if (rfld != null) report_fields.add(rfld);
       }
    }
   
   String getName()                             { return report_name; }
   
   void setCounter(int ct)                      { cur_counter = ct; }
   int getCounter()                             { return cur_counter; }
   
   boolean triggerReport() {
      int ct = ++cur_counter;
      if (ct < min_uses) return false;
      if (ct >= max_uses) {
         cur_counter = 0;
         return true;
       }
      int delta = (max_uses - min_uses);
      int d1 = (max_uses - ct);
      if (d1 < 3*delta/4) return false;
      
      if (random_gen.nextInt(delta) == 0) {
         cur_counter = 0;
         return true;
       }
      return false;
    }
   
   SwingGridPanel getPanel() {
      SwingGridPanel pnl = new SwingGridPanel();
      
      pnl.beginLayout();
      for (ReportField rfd : report_fields) {
         rfd.addToPanel(pnl);
       }
      
      pnl.addBottomButton("SUBMIT","SUBMIT",this);
      pnl.addBottomButton("CANCEL","CANCEL",this);
      pnl.addBottomButtons();
      
      return pnl;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      SwingGridPanel pnl = null;
      for (Component c = (Component) evt.getSource(); c != null; c = c.getParent()) {
         if (c instanceof SwingGridPanel) {
            pnl = (SwingGridPanel) c;
            break;
          }
       }
      
      switch (cmd) {
         case "SUBMIT" :
            removeDialog(evt);
            submitDialog(pnl);
            break;
         case "CANCEL" :
            removeDialog(evt);
            break;
       }
    }
   
   private void removeDialog(ActionEvent evt) {
      for (Component c = (Component) evt.getSource(); c != null; c = c.getParent()) {
         c.setVisible(false);
         if (c instanceof JDialog) break;
       }
    }
   
   private void submitDialog(SwingGridPanel pnl) {
      StringBuffer buf = new StringBuffer();
      boolean use = false;
      
      buf.append("REPORT DATA FOR " + getName() + "\n\n");
      for (ReportField rfd : report_fields) {
         use |= rfd.addToOutput(buf,pnl);
       }
      if (!use) return;
      
      BoardMailMessage msg = BoardMail.createMessage(send_to);
      msg.setSubject("BUBBLES USER REPORT FOR " + getName());
      msg.addBodyText(buf.toString());
      msg.send();   
    }
   
}       // end of inner class ReportData




private static abstract class ReportField {
   
   protected String field_name;
   
   ReportField(Element xml) {
      field_name = IvyXml.getAttrString(xml,"NAME");
    }
   
   abstract void addToPanel(SwingGridPanel pnl);
   abstract boolean addToOutput(StringBuffer buf,SwingGridPanel pnl);
   
}       // end of inner class ReportField



private static class LabelField extends ReportField {
   
   private String label_text;
   private boolean is_banner;
   private boolean is_section;
   
   LabelField(Element xml) {
      super(xml);
      label_text = IvyXml.getText(xml);
      is_banner = IvyXml.getAttrBool(xml,"BANNER");
      is_section = IvyXml.getAttrBool(xml,"SECTION");
    }
   
   @Override void addToPanel(SwingGridPanel pnl) {
      if (is_banner) pnl.addBannerLabel(label_text);
      else if (is_section) pnl.addSectionLabel(label_text);
      else pnl.addLabellessRawComponent(null,new JLabel(label_text));
    }
   
   @Override boolean addToOutput(StringBuffer buf,SwingGridPanel pnl) {
      return false; 
    }
   
}       // end of inner class LabelField



private static class SeparatorField extends ReportField {
   
   SeparatorField(Element xml) {
      super(xml);
    }
   
   @Override void addToPanel(SwingGridPanel pnl) {
      pnl.addSeparator();
    }
   
   @Override boolean addToOutput(StringBuffer buf,SwingGridPanel pnl) {
      return false;
    }
   
}       // end of inner class SeparatorField



private static class ChoiceField extends ReportField {
   
   private Map<String,Boolean> choice_set;
   private String first_item;
   
   ChoiceField(Element xml) {
      super(xml);
      choice_set = new LinkedHashMap<String,Boolean>();
      first_item = null;
      for (Element celt : IvyXml.children(xml,"OPTION")) {
         String txt = IvyXml.getText(celt);
         if (first_item == null) first_item = txt;
         choice_set.put(txt,IvyXml.getAttrBool(celt,"IGNORE"));
       }
    }
   
   @Override void addToPanel(SwingGridPanel pnl) {
      pnl.addChoice(field_name,choice_set.keySet(),first_item,null);
    }
   
   @Override boolean addToOutput(StringBuffer buf,SwingGridPanel pnl) {
      JComboBox<?> cbx = (JComboBox<?>) pnl.getComponentForLabel(field_name);
      if (cbx == null) return false;
      String cnt = cbx.getSelectedItem().toString();
      if (choice_set.get(cnt)) return false;
      buf.append(field_name + " :: " + cnt + "\n");
      return true;
    }
   
}       // end of inner class ChoiceField



private static class TextAreaField extends ReportField {
   
   private int num_rows;
   private int num_cols;
   
   TextAreaField(Element xml) {
      super(xml);
      num_rows = IvyXml.getAttrInt(xml,"WIOTH",40);
      num_cols = IvyXml.getAttrInt(xml,"HEIGHT",5);
    }
   
   @Override void addToPanel(SwingGridPanel pnl) {
      pnl.addTextArea(field_name,null,num_cols,num_rows,null);
    }
   
   @Override boolean addToOutput(StringBuffer buf,SwingGridPanel pnl) {
      JTextArea ta = (JTextArea) pnl.getComponentForLabel(field_name);
      if (ta == null) return false;
      String cnt = ta.getText().trim();
      if (cnt == null || cnt.length() == 0) return false;
      buf.append(field_name + ":-\n" + cnt + "\n-----------------\n");
      return true;
    }
   
}       // end of inner class TextAreaField




}       // end of class BoardUserReport




/* end of BoardUserReport.java */

