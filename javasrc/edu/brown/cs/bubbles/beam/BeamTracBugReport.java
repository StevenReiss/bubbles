/********************************************************************************/
/*										*/
/*		BeamTracBugReport.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items bug report panel		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Yu Li	      */
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
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.exec.IvyExecQuery;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;


class BeamTracBugReport implements BeamConstants, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot		for_root;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamTracBugReport(BudaRoot br)
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
   JButton btn = new JButton("<html><center>Trac&nbsp;Bug</center></html>", chevron);
   btn.setHorizontalTextPosition(SwingConstants.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   Color c = BoardColors.getColor(BUDA_BUTTON_PANEL_COLOR_PROP);
   btn.setBackground(c);
   btn.setMargin(BUDA_BUTTON_INSETS);
   btn.setFont(ft);
   btn.setOpaque(false);

   btn.addActionListener(new BugReportListener());
   btn.setToolTipText("Report a bubbles bug");
   for_root.addButtonPanelButton(btn);
}




/********************************************************************************/
/*										*/
/*	Callback to gather bug report information				*/
/*										*/
/********************************************************************************/

private class BugReportListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String url = null;
   
      if (Desktop.isDesktopSupported()) {
         JOptionPane.showMessageDialog(for_root,"<html><p>This will take you to our bug-reporting web site." +
        			    "<p>Please log in as yourself or anonymously as " +
        			    "codebubbles (no password).");
       }
   
      try {
         BoardProperties bp = BoardProperties.getProperties("Beam");
   
         url = bp.getProperty("Beam.tracbug.url");
         if (url == null || url.length() == 0) return;
         if (!url.endsWith("/")) url += "/";
         url += "newticket";
   
         String uid = System.getProperty("user.name") + "@" + IvyExecQuery.getHostName();
         uid = bp.getString("Beam.tracbug.uid",uid);
         url += "?owner=" + URLEncoder.encode(uid,"UTF-8");
   
         String vn = BoardSetup.getVersionData();
         String tv = null;
         if (vn.startsWith("Build")) tv = "self-built";
         else {
            if (vn.contains(".ext")) tv = "external_release";
            else tv = "current_release";
          }
         url += "&version=" + URLEncoder.encode(tv,"UTF-8");
         url += "&bubbles_version=" + URLEncoder.encode(vn,"UTF-8");
   
         File lf0 = null;
         if (bp.getBoolean("Beam.tracbug.screeenshot")) {
            BufferedImage bi;
            Dimension sz = for_root.getSize();
            bi = new BufferedImage(sz.width,sz.height,BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bi.createGraphics();
            for_root.paint(g2);
            try {
               lf0 = File.createTempFile("BoardMetrics_SCREEN_",".png");
               ImageIO.write(bi,"png",lf0);
               lf0.deleteOnExit();
             }
            catch (IOException e) {
               lf0 = null;
             }
          }
   
         String desc = "";
         File lf1 = BoardLog.getBubblesLogFile();
         if (lf1 != null && lf1.length() <= 0) lf1 = null;
         File lf2 = BoardLog.getBedrockLogFile();
         if (lf2 != null && lf2.length() <= 0) lf2 = null;
         if (lf1 != null) {
            desc += "Please attach: " + lf1 + "\n";
            if (lf2 != null) desc += "   and attach: " + lf2 + "\n";
          }
         else if (lf2 != null) {
            desc += "Please attach: " + lf2 + "\n";
          }
         if (lf0 != null) {
            desc += "Screen dump:   " + lf0 + "\n";
          }
   
         if (desc.length() > 0) {
            desc += "\n--------------------------\n\n";
            url += "&description=" + URLEncoder.encode(desc,"UTF-8");
          }
   
         BeamFactory.showBrowser(new URI(url));
       }
      catch (Throwable t) {
         // TODO: handle unsupported operation exception where desktop api not supported.
         BoardLog.logE("BEAM","Problem with bug report",t);
         if (url != null) {
            JOptionPane.showMessageDialog(for_root,"<html><p>This version of Java does not support" +
        				     " the desktop API.  Please use your browser to navigate" +
        				     " to " + url + "<p>Log in as user 'codebubbles' (no" +
        				     " password).");
          }
       }
   }

}	// end of inner class BugReportListener




}	// end of class BeamTracBugReport



/* end of BeamTracBugReport.java */
