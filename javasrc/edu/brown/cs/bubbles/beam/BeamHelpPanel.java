/********************************************************************************/
/*										*/
/*		BeamHelpPanel.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing help panel			*/
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
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.URI;


class BeamHelpPanel implements BeamConstants, BudaConstants {



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

BeamHelpPanel(BudaRoot br)
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
   JMenuBar mbar = new JMenuBar();
   BoardColors.setColors(mbar,BUTTON_PANEL_TOP_COLOR_PROP);
   mbar.setBackground(BoardColors.transparent());
   mbar.setMargin(new Insets(0,0,0,0));
   mbar.setOpaque(false);

   JMenu btn = new JMenu("HELP");
   btn.setHorizontalTextPosition(SwingConstants.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   btn.setForeground(mbar.getForeground());
   btn.setMargin(new Insets(0,0,0,0));
   btn.setFont(ft);
   btn.setOpaque(false);
   btn.add(new HelpHomeAction());
   btn.add(new HelpMailAction());
   btn.add(new HelpVideoAction());
   btn.add(new HelpWikiAction());
   btn.add(new HelpTutorialAction());
   btn.add(new HelpTodoAction());
   btn.add(new HelpKeyAction());
   btn.add(new HelpMouseAction());

   mbar.add(btn);

   btn.setToolTipText("Get help on Code Bubbles");
   for_root.addButtonPanelButton(mbar);
}


/********************************************************************************/
/*										*/
/*	Help methods								*/
/*										*/
/********************************************************************************/

void showHelpVideo()
{
   new HelpVideoAction().actionPerformed(null);
}



void showHelpHome()
{
   new HelpHomeAction().actionPerformed(null);
}


void showHelpWiki()
{
   new HelpWikiAction().actionPerformed(null);
}



void showHelpTutorial()
{
   new HelpTutorialAction().actionPerformed(null);
}



/********************************************************************************/
/*										*/
/*	Help action methods							*/
/*										*/
/********************************************************************************/

private abstract class HelpUrlAction extends AbstractAction {

   private String url_name;
   
   private static final long serialVersionUID = 1;

   HelpUrlAction(String nm,String key,String dflt) {
      super(nm);
      BoardProperties bp = BoardProperties.getProperties("Beam");
      url_name = bp.getProperty(key,dflt);
    }

   @Override public void actionPerformed(ActionEvent e) {
      try {
         url_name = url_name.replace("conifer.cs.brown.edu","conifer2.cs.brown.edu");
         URI u = new URI(url_name);
         BeamFactory.showBrowser(u);
       }
      catch (Throwable t) {
         BoardLog.logE("BEAM","Problem showing help url " + url_name,t);
       }
    }

}	// end of inner class HelpVideoAction




private class HelpVideoAction extends HelpUrlAction {

   private static final long serialVersionUID = 1;

   HelpVideoAction() {
      super("Show Help Video",HELP_VIDEO_KEY,HELP_VIDEO_URL);
    }

}	// end of inner class HelpVideoAction




private class HelpHomeAction extends HelpUrlAction {

   private static final long serialVersionUID = 1;

   HelpHomeAction() {
      super("Show Home Page",HELP_HOME_KEY,HELP_HOME_URL);
    }

}	// end of inner class HelpHomeAction




private class HelpWikiAction extends HelpUrlAction {

   private static final long serialVersionUID = 1;

   HelpWikiAction() {
      super("Show Code Bubbles Wiki",HELP_WIKI_KEY,HELP_WIKI_URL);
    }

}	// end of inner class HelpWikiAction



private class HelpTodoAction extends HelpUrlAction {

   private static final long serialVersionUID = 1;

   HelpTodoAction() {
      super("Show How-To Page",HELP_TODO_KEY,HELP_TODO_URL);
    }

}	// end of inner class HelpTodoAction





private class HelpTutorialAction extends HelpUrlAction {

   private static final long serialVersionUID = 1;

   HelpTutorialAction() {
      super("Show Tutorial",HELP_TUTORIAL_KEY,HELP_TUTORIAL_URL);
    }

}	// end of inner class HelpTutorialAction



private class HelpKeyAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   HelpKeyAction() {
      super("Show Keyboard Help");
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (for_root == null) return;
      BudaBubble bb = new BeamKeyBubble();
      BudaBubbleArea bba = for_root.getCurrentBubbleArea();
      if (bba == null) return;
      bba.addBubble(bb,null,null,BudaConstants.PLACEMENT_USER|PLACEMENT_MOVETO);
      bb.grabFocus();
    }

}	// end of inner class HelpKeyAction




private class HelpMouseAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   HelpMouseAction() {
      super("Show Mouse Usage Help");
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (for_root == null) return;
      BudaBubble bb = new BeamMouseBubble();
      BudaBubbleArea bba = for_root.getCurrentBubbleArea();
      if (bba == null) return;
      bba.addBubble(bb,null,null,BudaConstants.PLACEMENT_USER|PLACEMENT_MOVETO);
      bb.grabFocus();
    }

}	// end of inner class HelpMouseAction





/********************************************************************************/
/*										*/
/*	Mail question								*/
/*										*/
/********************************************************************************/

private class HelpMailAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   HelpMailAction() {
      super("Ask a Question");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardProperties bp = BoardProperties.getProperties("Beam");
      String addr = bp.getProperty(HELP_MAIL_KEY,HELP_MAIL_URL);
      int idx = addr.indexOf("@");
      addr = addr.substring(0,idx) + "+bubbles" + addr.substring(idx);
      BeamFactory.sendMail(addr,"Bubbles Question","Your Question Here");
    }

}	// end of inner class HelpMailAction




}	// end of class BeamHelpPanel



/* end of BeamHelpPanel.java */

