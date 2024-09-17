/********************************************************************************/
/*                                                                              */
/*              BvcrControlVersionPanel.java                                    */
/*                                                                              */
/*      Panel to show and allow user to select versions                         */
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class BvcrControlVersionPanel extends BudaBubble implements BvcrConstants,
        BvcrConstants.BvcrProjectUpdated, BudaConstants.BudaBubbleOutputer
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BvcrControlPanel        control_panel;
private JTable                  output_table;
private VersionTable            data_table;
private JButton                 change_button;
private BvcrControlVersion      current_version;
private static final long serialVersionUID = 1;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BvcrControlVersionPanel(BvcrControlPanel pnl) 
{ 
   control_panel = pnl;
   current_version = null;
   if (pnl.getVersionMap() != null) current_version = pnl.getVersionMap().get("HEAD");
    
   data_table = new VersionTable();
   JTable tbl = new VersionDisplay();
   output_table = tbl;
   SwingGridPanel tpnl = new SwingGridPanel();
   tpnl.beginLayout();
   tpnl.addLabellessRawComponent("table",new JScrollPane(tbl));
   tpnl.addSeparator();
   tpnl.addBottomButton("New Version","NEW",null);
   change_button = tpnl.addBottomButton("Change","CHANGE",new ChangeVersionAction());
   tpnl.addBottomButton("Show Log","LOG",new LogPanelAction(pnl));
   tpnl.addBottomButton("Show Graph","GRAPH",new GraphPanelAction(pnl));
   tpnl.addBottomButtons();
   change_button.setEnabled(false);
   
   setContentPane(tpnl);
   
   setCurrentSelection();
   
   pnl.addUpdateListener(this);
}



@Override protected void localDispose()
{
  control_panel.removeUpdateListener(this);
}


/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void projectUpdated(BvcrControlPanel pnl)
{
   current_version = null;
   if (pnl.getVersionMap() != null) current_version = pnl.getVersionMap().get("HEAD");
   
   data_table.update();
   setCurrentSelection();
}



private void setCurrentSelection()
{
   output_table.clearSelection();
   change_button.setEnabled(false);
   
   if (current_version != null) {
      int idx = data_table.getCurrentVersion();
      if (idx >= 0) {
         output_table.addRowSelectionInterval(idx,idx);
       }
    } 
}




/********************************************************************************/
/*                                                                              */
/*      Table widget                                                            */
/*                                                                              */
/********************************************************************************/

private class VersionDisplay extends JTable {
   
   private static final long serialVersionUID = 1;
   
   VersionDisplay() {
      super(data_table);
      setAutoCreateRowSorter(true);
      setPreferredScrollableViewportSize(new Dimension(500,150));
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      ListSelectionModel selmdl = getSelectionModel();
      selmdl.addListSelectionListener(new VersionSelector());
      setToolTipText("");
    }
   
   BvcrControlVersion getActualVersion(int row) {
      RowSorter<?> rs = getRowSorter();
      if (rs != null) row = rs.convertRowIndexToModel(row);
      return data_table.getVersion(row);
    }
   
   @Override public String getToolTipText(MouseEvent evt) {
      int r = rowAtPoint(evt.getPoint());
      if (r < 0) return null;
      BvcrControlVersion bcv = getActualVersion(r);
      if (bcv == null) return null;
      return "<html>" + bcv.getHtmlDescription();
    }
   
}       // end of class VersionDisplay



/********************************************************************************/
/*                                                                              */
/*      Table model                                                             */
/*                                                                              */
/********************************************************************************/

private class VersionTable extends AbstractTableModel {
   
   private final String [] column_names = { 
         "Id", "Name", "Date", "Author", "Parent", "Message"
    };
   
   private List<BvcrControlVersion> version_data;
   private static final long serialVersionUID = 1;
   
   VersionTable() {
      update();
    }
   
   void update() {
      if (control_panel.getVersionMap() != null) {
         version_data = new ArrayList<>(control_panel.getVersions());
         Collections.sort(version_data,new VersionSorter());
       }
      else {
         version_data = new ArrayList<>();
       }
      fireTableDataChanged();
    }
 
   int getCurrentVersion() {
      if (current_version == null || version_data == null) return -1;
      return version_data.indexOf(current_version);
    }
   
   @Override public int getColumnCount() {
      return column_names.length;
    }
   
   @Override public String getColumnName(int idx) {
      return column_names[idx];
    }
   
   @Override public Class<?> getColumnClass(int idx) {
      if (idx == 2) return Date.class;
      else return super.getColumnClass(idx);
    }
   
   @Override public int getRowCount() {
      return version_data.size();
    }
   
   BvcrControlVersion getVersion(int row) {
      return version_data.get(row);
    }
   
   @Override public Object getValueAt(int row,int col) {
      BvcrControlVersion vv = version_data.get(row);
      if (vv == null) return null;
      switch (col) {
         case 0 :
            String nm = vv.getName();
            for (String s : vv.getAlternativeIds()) {
               if (s.length() < nm.length()) nm = s;
             }
            return nm;
         case 1 :
            String r = null;
            for (String s : vv.getAlternativeNames()) {
               if (s.isEmpty()) continue;
               if (r == null) r = s;
               else r += "," + s;
             }
            return r;
         case 2: 
            return vv.getDate();
         case 3 :
            return vv.getAuthor();
         case 4 :
            String pv = null;
            for (String s : vv.getPriorIds()) {
               BvcrControlVersion pver = control_panel.getVersionMap().get(s);
               if (pver == null) continue;
               String pnm = pver.getBestName();
               if (pv == null) pv = pnm;
               else pv += "," + pnm;
             }
            return pv;
         case 5 :
            return vv.getMessage();
         case -1 :
            return vv;
       }
      return null;
    }
   
}       // end of inner class VersionTable



/********************************************************************************/
/*                                                                              */
/*      Version sorter for default order                                        */
/*                                                                              */
/********************************************************************************/

private static class VersionSorter implements Comparator<BvcrControlVersion> {
   
   @Override public int compare(BvcrControlVersion v1,BvcrControlVersion v2) {
      return v2.getDate().compareTo(v1.getDate());
    }
   
}       // end of inner class VersionSorter



/********************************************************************************/
/*                                                                              */
/*      Version Selector -- handle version selected                             */
/*                                                                              */
/********************************************************************************/

private class VersionSelector implements ListSelectionListener {
   
   @Override public void valueChanged(ListSelectionEvent evt) {
      ListSelectionModel lsm = (ListSelectionModel) evt.getSource();
      int idx = lsm.getMinSelectionIndex();
      if (idx != data_table.getCurrentVersion()) {
         change_button.setEnabled(true);
       }
      else change_button.setEnabled(false);
    }
   
}       // end of inner class VersionSelector



/********************************************************************************/
/*                                                                              */
/*      Actions                                                                 */
/*                                                                              */
/********************************************************************************/

private static class GraphPanelAction extends AbstractAction {
   
   private BvcrControlPanel control_panel;
   private static final long serialVersionUID = 1;
   
   GraphPanelAction(BvcrControlPanel pnl) {
      control_panel = pnl;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      Component btn = (Component) evt.getSource();
      BvcrControlGraphPanel pnl = new BvcrControlGraphPanel(control_panel);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(btn);
      BudaBubble bbl = BudaRoot.findBudaBubble(btn);
      if (bba == null) return;
      bba.addBubble(pnl,bbl,null,BudaConstants.PLACEMENT_LOGICAL);
    }
   
}       // end of inner class GraphPanelAction



private static class LogPanelAction extends AbstractAction {
   
   private BvcrControlPanel control_panel;
   private static final long serialVersionUID = 1;
   
   LogPanelAction(BvcrControlPanel cpnl) {
      control_panel = cpnl;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      Component btn = (Component) evt.getSource();
      BvcrControlLogPanel pnl = new BvcrControlLogPanel(control_panel);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(btn);
      BudaBubble bbl = BudaRoot.findBudaBubble(btn);
      if (bba == null) return;
      bba.addBubble(pnl,bbl,null,BudaConstants.PLACEMENT_LOGICAL);
    }
   
}       // end of inner class LogPanelAction



private class ChangeVersionAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   @Override public void actionPerformed(ActionEvent evt) {
      if (current_version == null) return;
      control_panel.setVersion(current_version);
    }
   
}       // end of inner class ChangeVersionAction


/********************************************************************************/
/*                                                                              */
/*      Configurator methods                                                    */
/*                                                                              */
/********************************************************************************/

@Override public String getConfigurator()               { return "BVCR"; }

@Override public void outputXml(BudaXmlWriter xw) 
{
   xw.field("TYPE","VERSIONCONTROL");
   xw.field("PROJECT",control_panel.getProject());
}



}       // end of class BvcrControlVersionPanel




/* end of BvcrControlVersionPanel.java */

