/********************************************************************************/
/*										*/
/*		BeamFeedbackReport.java 					*/
/*										*/
/*	Bubbles Environment Auxillary & Missing items feedback report		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Yu Li			      */
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
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;



class BeamFeedbackReport implements BeamConstants, BudaConstants {



/********************************************************************************/
/*										*/
/* Private Storage								*/
/*										*/
/********************************************************************************/

private BudaRoot     for_root;

enum FeedbackType { SUGGESTION, COMMENT, QUESTION, OTHER };



/********************************************************************************/
/*										*/
/* Constructors 								*/
/*										*/
/********************************************************************************/

BeamFeedbackReport(BudaRoot br)
{
   for_root = br;
}



/********************************************************************************/
/*										*/
/*      Setup methods								*/
/*										*/
/********************************************************************************/

void addPanel()
{
   Icon chevron = BoardImage.getIcon("dropdown_chevron",BUDA_BUTTON_RESIZE_WIDTH,BUDA_BUTTON_RESIZE_HEIGHT);

   JButton btn = new JButton("<html><center>Feedback</center></html>", chevron);
   btn.setHorizontalTextPosition(SwingConstants.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   btn.setBackground(BoardColors.transparent());
   btn.setMargin(BUDA_BUTTON_INSETS);
   btn.setFont(ft);
   btn.setOpaque(false);
   btn.addActionListener(new FeedbackReportListener());
   btn.setToolTipText("Report feedback");
   for_root.addButtonPanelButton(btn);

   if (isLiLaPresent()) {
      btn = new JButton("<html><center>Too Slow</center></html>", chevron);
      btn.setHorizontalTextPosition(SwingConstants.LEADING);
      btn.setBackground(BoardColors.transparent());
      btn.setMargin(BUDA_BUTTON_INSETS);
      btn.setFont(ft);
      btn.setOpaque(false);
      btn.addActionListener(new AngryListener());
      btn.setToolTipText("Report the system running slowly");
      for_root.addButtonPanelButton(btn);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Create the feedback panel                                               */
/*                                                                              */
/********************************************************************************/

private JPanel createFeedbackPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   FeedbackChecker chk = new FeedbackChecker(pnl);
   
   pnl.beginLayout();
   pnl.addBannerLabel("Code Bubbles Feedback");
   
   pnl.addChoice("Feedback Type",FeedbackType.COMMENT,chk);
   pnl.addTextArea("Feedback",null,10,60,chk);
   
   pnl.addBoolean("Include Screenshot",false,null);
   pnl.addTextField("Reply To",null,40,null,null);
   
   pnl.addBottomButton("CANCEL","CANCEL",chk);
   pnl.addBottomButton("SEND FEEDBACK","SEND",false,chk);
   pnl.addBottomButtons();
   
   return pnl;  
}




private class FeedbackChecker implements ActionListener, UndoableEditListener {
   
   private SwingGridPanel for_panel;
   
   FeedbackChecker(SwingGridPanel pnl) {
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
            sendFeedback(for_panel);
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
      JTextArea d = (JTextArea) for_panel.getComponentForLabel("Feedback");
      JButton ok = (JButton) for_panel.getComponentForLabel("SEND");
      String dtxt = d.getText();
      if (dtxt == null || dtxt.trim().length() < 5) {
         ok.setEnabled(false);
       }
      else {
         ok.setEnabled(true);
       }
    }
   
}       // end of inner class EmailChecker




/********************************************************************************/
/*                                                                              */
/*      Seed feedback                                                           */
/*                                                                              */
/********************************************************************************/

private void sendFeedback(SwingGridPanel panel)
{
   String addr = "spr+bubblesfeedback@cs.brown.edu";
   String subj = "Bubbles bug report";
   
   StringBuffer body = new StringBuffer();
   
   JComboBox<?> cbx = (JComboBox<?>) panel.getComponentForLabel("Feedback Type");
   body.append("Feedback type: " + cbx.getSelectedItem() + "\n\n");
   
   body.append("Feedback:\n");
   JTextArea d = (JTextArea) panel.getComponentForLabel("Feedback");
   body.append(d.getText());
   body.append("\n");
   
   List<File> added = null;
   JCheckBox cb = (JCheckBox) panel.getComponentForLabel("Include Screenshot");
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



/********************************************************************************/
/*										*/
/*	Callback to gather feedback report information				*/
/*										*/
/********************************************************************************/

private class FeedbackReportListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JPanel pnl = createFeedbackPanel();
      JDialog dlg = new JDialog(for_root,"Code Bubbles Feedback",false);
      dlg.setContentPane(pnl);
      dlg.pack();
      dlg.setVisible(true);
   }

}  // end of inner class FeedbackReportListener



/********************************************************************************/
/*										*/
/*	Callback for anger reports						*/
/*										*/
/********************************************************************************/

private static boolean isLiLaPresent()
{
   return ClassLoader.getSystemResource("usi/instrumentation/LiLa.class") != null;
}




private static class AngryListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      try {
	 Class<?> c = Class.forName("usi.bubbles.FlyBy");
	 Method m = c.getMethod("dumpStack");
	 m.invoke(null);
	 BoardLog.logI("BEAM","Anger report successful");
       }
      catch (Throwable t) {
	 BoardLog.logE("BEAM","Anger report failed",t);
       }
    }

}	// end of inner class AngryListener




}	// end of class BeamFeedbackReport



/* end of BeamFeedbackReport.java */




