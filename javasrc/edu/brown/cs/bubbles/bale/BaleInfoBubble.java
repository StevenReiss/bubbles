/********************************************************************************/
/*										*/
/*		BaleInfoBubble.java						*/
/*										*/
/*	Bubble Annotated Language Editor information bubble			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Yu Li				*/
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

package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.text.Position;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;


/**
 * This class implements a bubble, which contains information of various "go to" actions
 **/

class BaleInfoBubble extends BudaBubble implements BaleConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;
private BaleEditorPane	  bale_editor;
private JPanel	    info_panel;

private static Map<String, String> action_names;

static {
   action_names = new HashMap<>();
   action_names.put("Go to Definition", "GotoDefinitionAction");
   action_names.put("Find All References", "GotoReferenceAction");
   action_names.put("Go to Documentation", "GotoDocAction");
   action_names.put("Go to Implementation", "GotoImplementationAction");
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static void createInfoBubble(BaleEditorPane target,String elmtype,
	 BaleInfoBubbleType type,Position sp)
{
   BaleInfoBubble bib = null;
   switch (type) {
      case DOC:
	 if (elmtype.equals("FieldId")) bib = new BaleInfoBubble(target,new String[] {
		  "Go to Definition", "Find All References" },
		  "No documentation found! Suggestions: ",BaleInfoBubbleIconType.WARNING,
		  sp);
	 else if (elmtype.equals("LocalDeclId")) bib = new BaleInfoBubble(target,
		  new String[] { "Find All References" },
		  "No documentation found! Suggestions: ",BaleInfoBubbleIconType.WARNING,
		  sp);
	 else bib = new BaleInfoBubble(target,new String[] { "Go to Implementation",
		  "Find All References" },"No documentation found! Suggestions: ",
		  BaleInfoBubbleIconType.WARNING,sp);
	 break;
      case DEFDOC:
	 if (elmtype.equals("FieldId")) bib = new BaleInfoBubble(target,new String[] {},
		  "No definition found!",BaleInfoBubbleIconType.WARNING,sp);
	 else bib = new BaleInfoBubble(target,new String[] { "Go to Implementation" },
		  "No definition found! Suggestions: ",BaleInfoBubbleIconType.WARNING,sp);
	 break;
      case IMPLDOC:
	 if (elmtype.equals("FieldId")) bib = new BaleInfoBubble(target,new String[] {},
		  "Source for implementation not found!",BaleInfoBubbleIconType.WARNING,sp);
	 else bib = new BaleInfoBubble(target,new String[] {},"Source for implementation not found!",
		  BaleInfoBubbleIconType.WARNING,sp);
	 break;
      case UNDEFINED:
	 bib = new BaleInfoBubble(target,new String[] {},
		  "Name is not defined",BaleInfoBubbleIconType.ERROR,sp);
	 break;
      case NOIDENTIFIER:
	 bib = new BaleInfoBubble(target,new String[] {},
		  "Please select a valid element name!",BaleInfoBubbleIconType.ERROR,sp);
	 break;
      case REF:
	 if (elmtype.equals("FieldId") || elmtype.equals("LocalDeclId")) bib = new BaleInfoBubble(
		  target,new String[] { "Go to Definition" },
		  "0 references! Suggestions:",BaleInfoBubbleIconType.WARNING,sp);
	 else bib = new BaleInfoBubble(target,new String[] { "Go to Implementation" },
		  "0 references! Suggestions:",BaleInfoBubbleIconType.WARNING,sp);
	 break;
    }

   if (bib == null) return;
   // Might want to use source position to compute y offset
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(target);
   if (bba == null) return;
   bba.addBubble(bib,target,null,PLACEMENT_RIGHT|PLACEMENT_GROUPED);

   // BudaRoot broot = BudaRoot.findBudaRoot(target);
   // Rectangle loc = BudaRoot.findBudaLocation(target);
   // broot.add(bib, new BudaConstraint(loc.x + loc.width + 20,loc.y));
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 * This method creates a information bubble, which will appear when "GO TO" action returns
 * nothing.
 **/

private BaleInfoBubble(BaleEditorPane target,String[] btnmsg,String bbmsg,
	 BaleInfoBubbleIconType icontype,Position sp)
{
   info_panel = new JPanel();
   info_panel.setOpaque(false);
   info_panel.setLayout(new GridLayout(0,1));
   Icon icon = null;

   /*********** these icons don't exist yet
   if (icontype == BaleInfoBubbleIconType.ERROR) icon = BoardImage.getIcon("info_error");
   else icon = BoardImage.getIcon("info_warning");
   *************/

   JLabel label;
   label = new JLabel("<html><font color=red>" + bbmsg + "</font></html>",icon,
	    SwingConstants.CENTER);
   label.addMouseListener(new QuitAction());
   info_panel.add(label);

   MouseAdapter actionhandler = new ContextActionHandler();

   if (btnmsg != null && btnmsg.length != 0) {
      JLabel lb;
      for (int i = 0; i < btnmsg.length; i++) {
	 lb = new JLabel("<html><font color=blue><u>" + btnmsg[i] + "</u></font></html>",
		  SwingConstants.CENTER);
	 lb.setToolTipText(btnmsg[i]);
	 lb.setOpaque(false);
	 lb.addMouseListener(actionhandler);
	 info_panel.add(lb);
       }
    }

   setTransient(true);
   setInteriorColor(BoardColors.getColor("Bale.InfoInteriorColor"));
   setContentPane(info_panel);

   bale_editor = target;

   if (sp != null) addBubbleLink(sp);
}



/**
 * This method to create a link between this bubble and target bubble.
 **/
private void addBubbleLink(Position p)
{
   BudaRoot root = BudaRoot.findBudaRoot(bale_editor);
   if (root == null) return;

   BudaConstants.LinkPort p0;
   if (p != null) {
      p0 = new BaleLinePort(bale_editor,p,null);
    }
   else {
      p0 = new BudaDefaultPort(BudaPortPosition.BORDER_EW,true);
    }

   BudaConstants.LinkPort p1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
   BudaBubble obbl = BudaRoot.findBudaBubble(bale_editor);
   if (obbl == null) return;
   BudaBubbleLink lnk = new BudaBubbleLink(obbl,p0,this,p1,true,BudaLinkStyle.STYLE_SOLID);
   root.addLink(lnk);
}


/********************************************************************************/
/*										*/
/*	Action Handler								*/
/*										*/
/********************************************************************************/

private class QuitAction extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      setVisible(false);
    }

}	// end of inner calss QuitAction


private class ContextActionHandler extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      JLabel lb = (JLabel) e.getSource();
      String cmd = lb.getToolTipText();
      String acmd = action_names.get(cmd);
      if (acmd != null) {
         ActionEvent nevt = new ActionEvent(bale_editor,e.getID(),acmd,e.getWhen(),
               e.getModifiersEx());
         Action a = BaleEditorKit.findAction(acmd);
         a.actionPerformed(nevt);
       }
      else {
         BoardLog.logE("BALE","CONTEXT MENU UNKOWN COMMAND " + cmd);
       }
      setVisible(false);
    }

}	// end of inner class ContextActionHandler


}	// end of class BaleInfoBubble



/* end of BaleInfoBubble.java */
