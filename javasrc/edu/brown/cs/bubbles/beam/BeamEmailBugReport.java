/********************************************************************************/
/*										*/
/*		BeamEmailBugReport.java 					*/
/*										*/
/*	description of class							*/
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


class BeamEmailBugReport implements BeamConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot		for_root;

private enum SEVERITY { SEVERE, BAD, ANNOYING, QUIRK, TRIVIAL, SUGGESTION, WOW }



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamEmailBugReport(BudaRoot br)
{
   for_root = br;
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addPanel()
{
   Icon chevron = BoardImage.getIcon("dropdown_chevron",BUDA_BUTTON_RESIZE_WIDTH, BUDA_BUTTON_RESIZE_HEIGHT);
   JButton btn = new JButton("<html><center>Bug Report</center></html>", chevron);
   btn.setHorizontalTextPosition(SwingConstants.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   btn.setBackground(BoardColors.transparent());
   btn.setMargin(BUDA_BUTTON_INSETS);
   btn.setFont(ft);
   btn.setOpaque(false);

   btn.addActionListener(new BugReportListener());
   btn.setToolTipText("Report a bubbles bug");
   for_root.addButtonPanelButton(btn);
}




/********************************************************************************/
/*										*/
/*	Set up the email reply panel						*/
/*										*/
/********************************************************************************/

private JPanel emailReportPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   EmailChecker chk = new EmailChecker(pnl);
   pnl.beginLayout();
   pnl.addBannerLabel("Code Bubbles Bug Report");
   pnl.addTextArea("Description",null,10,60,chk);
   pnl.addChoice("Severity",SEVERITY.BAD,null);
   pnl.addBoolean("Include internal details",true,null);
   pnl.addBoolean("Include Screenshot",false,null);
   pnl.addTextField("Reply To","Reply To",40,null,null);
   pnl.addBottomButton("CANCEL","CANCEL",chk);
   pnl.addBottomButton("SEND REPORT","SEND",false,chk);
   pnl.addBottomButtons();

   return pnl;
}



private class EmailChecker implements ActionListener, UndoableEditListener {

   private SwingGridPanel for_panel;

   EmailChecker(SwingGridPanel pnl) {
      for_panel = pnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand().toLowerCase();
      switch (cmd) {
	 case "cancel" :
	    removePanel();
	    break;
	 case "send" :
	    JButton btn = (JButton) evt.getSource();
	    btn.setEnabled(false);
	    removePanel();
	    sendEmail(for_panel);
	    return;
       }
      updateStatus();
    }

   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      updateStatus();
    }

   private void removePanel() {
      for (Component cmp = for_panel; cmp != null && cmp != for_root; cmp = cmp.getParent()) {
	 cmp.setVisible(false);
	 if (cmp instanceof JDialog) break;
       }
    }

   private void updateStatus() {
      JTextArea d = (JTextArea) for_panel.getComponentForLabel("Description");
      JButton ok = (JButton) for_panel.getComponentForLabel("SEND");
      String dtxt = d.getText();
      if (dtxt == null || dtxt.trim().length() < 5) {
	 ok.setEnabled(false);
       }
      else {
	 ok.setEnabled(true);
       }
    }

}	// end of inner class EmailChecker




/********************************************************************************/
/*										*/
/*	Methods to send the email						*/
/*										*/
/********************************************************************************/

private void sendEmail(SwingGridPanel panel)
{
   String addr = "spr+bubblesbug@cs.brown.edu";
   String subj = "Bubbles bug report";

   StringBuffer body = new StringBuffer();

   JComboBox<?> cbx = (JComboBox<?>) panel.getComponentForLabel("Severity");
   body.append("Severity of the problem: " + cbx.getSelectedItem() + "\n\n");

   body.append("Description of the problem:\n");
   JTextArea d = (JTextArea) panel.getComponentForLabel("Description");
   body.append(d.getText());
   body.append("\n");

   JCheckBox cb = (JCheckBox) panel.getComponentForLabel("Include internal details");
   if (cb.isSelected()) {
      body.append("\n\nLog data:\n");
      File lf1 = BoardLog.getBubblesLogFile();
      addToOutput("  ",body,lf1);
      File lf2 = BoardLog.getBedrockLogFile();
      addToOutput("  ",body,lf2);
      BoardMetrics.getLastCommands("  ",body);
    }

   List<File> added = null;
   cb = (JCheckBox) panel.getComponentForLabel("Include Screenshot");
   if (cb.isSelected()) {
      File f = BoardMetrics.createScreenDump("png");
      if (f != null) {
	 added = new ArrayList<>();
	 added.add(f);
	 f.deleteOnExit();
       }
    }

   JTextField tfld = (JTextField) panel.getComponentForLabel("Reply To");
   String rply = tfld.getText().trim();
   if (rply == null || rply.length() <= 4) rply = null;
   BeamFactory.sendMailDirect(addr,subj,body.toString(),added,rply);
}




private void addToOutput(String pfx,Appendable buf,File f)
{
   if (f == null) return;
   String [] lns = new String[50];
   int ct = 0;
   try (BufferedReader br = new BufferedReader(new FileReader(f))) {
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 lns[ct%lns.length] = ln;
	 ++ct;
       }
      for (int i = 0; i < lns.length; ++i) {
	 int idx = (ct+i)%lns.length;
	 if (lns[idx] != null) {
	    if (pfx != null) buf.append(pfx);
	    buf.append(lns[idx]);
	    buf.append("\n");
	  }
       }
    }
   catch (IOException e) { }
}




/********************************************************************************/
/*										*/
/*	Callback to set up email message					*/
/*										*/
/********************************************************************************/

private class BugReportListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JPanel pnl = emailReportPanel();
      JDialog dlg = new JDialog(for_root,"Bubbles Bug Report",false);
      dlg.setContentPane(pnl);
      dlg.pack();
      dlg.setVisible(true);

      // String addr = "spr+bubblesbug@cs.brown.edu";
      // String subj = "Bubbles bug report";
      //
      // StringBuffer body = new StringBuffer();
      // body.append("Description of the problem:\n\n\n\n");
      // body.append("Severity of the problem: \n\n\n");
      // body.append("Log data:\n");
      // File lf1 = BoardLog.getBubblesLogFile();
      // addToOutput("  ",body,lf1);
      // File lf2 = BoardLog.getBedrockLogFile();
      // addToOutput("  ",body,lf2);
      // BoardMetrics.getLastCommands("  ",body);
      //
      // BeamFactory.sendMail(addr,subj,body.toString());
    }

}	// end of inner class BugReportListener




}	// end of class BeamEmailBugReport




/* end of BeamEmailBugReport.java */

