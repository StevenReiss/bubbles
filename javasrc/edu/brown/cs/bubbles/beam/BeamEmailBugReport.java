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
import edu.brown.cs.bubbles.board.BoardSetup;
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
private static boolean          use_github = false;

private static final String     DESC_PROMPT = "Description";
private static final String     SEVERITY_PROMPT = "Severity";
private static final String     DETAILS_PROMPT = "Include internal details";
private static final String     SCREEN_PROMPT = "Include blurred screenshot";
private static final String     REPLY_PROMPT = "Reply to e-mail";

private static final String EMAIL_PATTERN = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
         


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
   btn.setToolTipText("Report a bubbles bug, ask for a feature, or provide feedback");
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
   pnl.addTextArea(DESC_PROMPT,null,10,60,chk);
   pnl.addChoice(SEVERITY_PROMPT,SEVERITY.BAD,null);
   pnl.addBoolean(DETAILS_PROMPT,true,null);
   pnl.addBoolean(SCREEN_PROMPT,false,null);
   JTextField tf = pnl.addTextField(REPLY_PROMPT,null,40,chk,chk);
   tf.setToolTipText("Provide your email for further information");
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
            if (use_github) sendReport(for_panel);
            else sendEmail(for_panel);
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
      boolean valid = true;
      JTextArea d = (JTextArea) for_panel.getComponentForLabel(DESC_PROMPT);
      JButton ok = (JButton) for_panel.getComponentForLabel("SEND");
      String dtxt = d.getText();
      if (dtxt == null || dtxt.trim().length() < 5) valid = false;
      JTextField r = (JTextField) for_panel.getComponentForLabel(REPLY_PROMPT);
      String raddr = r.getText();
      if (raddr == null) valid = false;
      else if (!raddr.trim().matches(EMAIL_PATTERN)) valid = false;
      
      ok.setEnabled(valid);
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

   String body = setupReportText(panel);
 
   List<File> added = null;
   JCheckBox cb = (JCheckBox) panel.getComponentForLabel(SCREEN_PROMPT );
   if (cb.isSelected()) {
      File f = BoardMetrics.createScreenDump("png");
      if (f != null) {
	 added = new ArrayList<>();
	 added.add(f);
	 f.deleteOnExit();
       }
    }

   JTextField tfld = (JTextField) panel.getComponentForLabel(REPLY_PROMPT);
   String rply = tfld.getText().trim();
   if (rply == null || rply.length() <= 4) rply = null;
   BeamFactory.sendMailDirect(addr,subj,body,added,rply);
}



private String setupReportText(SwingGridPanel panel)
{
   StringBuffer body = new StringBuffer();

   JTextField tfld = (JTextField) panel.getComponentForLabel(REPLY_PROMPT);
   String rply = tfld.getText().trim();
   body.append("Sender ID: " + rply + " :: " + System.getProperty("user.name") + "\n");
   body.append("Java Version: " + System.getProperty("java.version") + " " + 
         System.getProperty("java.vm.name") + "\n");
   body.append("OS Version: " + System.getProperty("os.name") + "," + System.getProperty("os.arch") + 
         "," + System.getProperty("os.version") + "\n");
   body.append("BUBBLES Version: " + BoardSetup.getVersionData() + "\n\n");
   
   JComboBox<?> cbx = (JComboBox<?>) panel.getComponentForLabel(SEVERITY_PROMPT);
   body.append("Severity of the problem: " + cbx.getSelectedItem() + "\n\n");
   
   body.append("Description of the problem:\n");
   JTextArea d = (JTextArea) panel.getComponentForLabel(DESC_PROMPT);
   body.append(d.getText());
   body.append("\n");
   
   JCheckBox cb = (JCheckBox) panel.getComponentForLabel(DETAILS_PROMPT);
   if (cb.isSelected()) {
      body.append("\n\nBubbles Log data:\n");
      File lf1 = BoardLog.getBubblesLogFile();
      addToOutput("  ",body,lf1,1000);
      body.append("\n\nBedrock Log data:\n");
      File lf2 = BoardLog.getBedrockLogFile();
      addToOutput("  ",body,lf2,1000);
      BoardMetrics.getLastCommands("  ",body,50);
    }
   
   return body.toString();
}



private void addToOutput(String pfx,Appendable buf,File f,int len)
{
   if (f == null) return;
   String [] lns = new String[len];
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
/*                                                                              */
/*      Add an issue to GitHub repo for bubbles                                 */
/*                                                                              */
/********************************************************************************/

private void sendReport(SwingGridPanel panel)
{
   String cnts = setupReportText(panel);
 
   JCheckBox cb = (JCheckBox) panel.getComponentForLabel(SCREEN_PROMPT);
   if (cb.isSelected()) {
      String s = BoardMetrics.saveScreenDump();
      if (s != null) cnts += "\nScreen Image: " + s + "\n";
    }
   
   BoardLog.logD("BEAM","Sending bug report as new github issue: " + cnts);
   
   // need to send to github here
}



/********************************************************************************/
/*										*/
/*	Callback to set up bug report dialog   				*/
/*										*/
/********************************************************************************/

private class BugReportListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JPanel pnl = emailReportPanel();
      JDialog dlg = new JDialog(for_root,"Bubbles Bug Report",false);
      dlg.setContentPane(pnl);
      dlg.pack();
      dlg.setVisible(true);
    }

}	// end of inner class BugReportListener




}	// end of class BeamEmailBugReport




/* end of BeamEmailBugReport.java */

