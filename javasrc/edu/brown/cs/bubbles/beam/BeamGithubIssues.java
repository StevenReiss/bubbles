/********************************************************************************/
/*										*/
/*		BeamGithubIssues.java						*/
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
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;


class BeamGithubIssues implements BeamConstants, BudaConstants {



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

BeamGithubIssues(BudaRoot br)
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
   if (!Desktop.isDesktopSupported()) return;
   
   Icon chevron = BoardImage.getIcon("dropdown_chevron",BUDA_BUTTON_RESIZE_WIDTH, BUDA_BUTTON_RESIZE_HEIGHT);
   JButton btn = new JButton("<html><center>View&nbsp;Issues</center></html>", chevron);
   btn.setHorizontalTextPosition(SwingConstants.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   Color c = BoardColors.getColor(BUDA_BUTTON_PANEL_COLOR_PROP);
   btn.setBackground(c);
   btn.setMargin(BUDA_BUTTON_INSETS);
   btn.setFont(ft);
   btn.setOpaque(false);

   btn.addActionListener(new BugReportListener());
   btn.setToolTipText("Go to the GitHub issues page for Bubbles to view or report an issue");
   for_root.addButtonPanelButton(btn);
}




/********************************************************************************/
/*										*/
/*	Callback to gather bug report information				*/
/*										*/
/********************************************************************************/

private final class BugReportListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String url = "https://github.com/StevenReiss/bubbles/issues";
      try {
         BeamFactory.showBrowser(new URI(url));
       }
      catch (Throwable t) {
         BoardLog.logE("BEAM","Problem with bug report",t);
       }
   }

}	// end of inner class BugReportListener




}	// end of class BeamGithubIssues



/* end of BeamGithubIssues.java */
