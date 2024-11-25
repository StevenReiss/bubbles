/********************************************************************************/
/*                                                                              */
/*              BvcrControlLogPanel.java                                        */
/*                                                                              */
/*      description of class                                                    */
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
import edu.brown.cs.bubbles.bvcr.BvcrConstants.BvcrProjectUpdated;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.JEditorPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


class BvcrControlLogPanel extends BudaBubble implements BvcrConstants, BvcrProjectUpdated
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BvcrControlPanel        control_panel;
private LogViewer               log_viewer;
private static final long serialVersionUID = 1;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BvcrControlLogPanel(BvcrControlPanel pnl)
{
   control_panel = pnl;
   
   SwingGridPanel tpnl = new SwingGridPanel();
   tpnl.beginLayout();
   log_viewer = new LogViewer();
   tpnl.addLabellessRawComponent("LOG",new JScrollPane(log_viewer));
   
   setContentPane(tpnl);
   
   pnl.addUpdateListener(this); 
   updateLog();
}



@Override protected void localDispose()
{
   control_panel.removeUpdateListener(this);
}




/********************************************************************************/
/*                                                                              */
/*      Bubble actions                                                         */
/*                                                                              */
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e) 
{
   JPopupMenu menu = new JPopupMenu();
   menu.add(getFloatBubbleAction());
   menu.show(this,e.getX(),e.getY());
}



/********************************************************************************/
/*                                                                              */
/*      Callback methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void projectUpdated(BvcrControlPanel pnl)
{
   updateLog();
}




/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

private void updateLog()
{
   Map<String,BvcrControlVersion> vermap = control_panel.getVersionMap();
   BvcrControlVersion head = vermap.get("HEAD");
   if (head == null) {
      for (BvcrControlVersion bcv : vermap.values()) {
         if (head == null) head = bcv;
         else if (bcv.getDate().compareTo(head.getDate()) < 0) head = bcv;
       }
    }
   if (head == null) return;
   
   Set<BvcrControlVersion> vers = new HashSet<BvcrControlVersion>();
   addVersions(head,vers,vermap);  
   Set<BvcrControlVersion> nvers = new TreeSet<BvcrControlVersion>(new VersionComparer());
   nvers.addAll(vers);
   
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   for (BvcrControlVersion bcv : nvers) {
      buf.append(bcv.getHtmlDescription());
      buf.append("<hl>");
    }
   log_viewer.setText(buf.toString());
}



private void addVersions(BvcrControlVersion ver,Set<BvcrControlVersion> rslt,
      Map<String,BvcrControlVersion> vermap)
{
   if (!rslt.add(ver)) return;
   
   for (String s : ver.getPriorIds()) {
      BvcrControlVersion pver = vermap.get(s);
      addVersions(pver,rslt,vermap);
    }
}



private final class VersionComparer implements Comparator<BvcrControlVersion> {
   
   @Override public int compare(BvcrControlVersion v1,BvcrControlVersion v2) {
      return v2.getDate().compareTo(v1.getDate());
    }
   
}       // end of inner class VersionComparer




/********************************************************************************/
/*                                                                              */
/*      Editor class to hold log view                                           */
/*                                                                              */
/********************************************************************************/

private static class LogViewer extends JEditorPane {
   
   private static final long serialVersionUID = 1;
   
   LogViewer() {
      super("text/html",null);
      setEditable(false);
      setPreferredSize(new Dimension(400,200));
    }
   
}       // end of inner class LogViewer




}       // end of class BvcrControlLogPanel




/* end of BvcrControlLogPanel.java */

