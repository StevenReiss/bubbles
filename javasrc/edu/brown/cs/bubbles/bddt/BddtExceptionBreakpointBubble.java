/********************************************************************************/
/*										*/
/*		BddtExceptionBreakpointBubble.java				*/
/*										*/
/*	Bubble to handle the creation of exception breakpoings			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeSet;


class BddtExceptionBreakpointBubble extends BudaBubble implements BddtConstants,
		BumpConstants, BudaConstants, BassConstants, ActionListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JComboBox<String>	exception_box;
private JCheckBox		caught_button;
private JCheckBox		uncaught_button;
private JCheckBox		suspendvm_button;
private JCheckBox               subclass_button;
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtExceptionBreakpointBubble()
{
   setupPanel();
   BoardThreadPool.start(new ExceptionSet());
}



/********************************************************************************/
/*										*/
/*	Panel layout								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.beginLayout();
   pnl.addBannerLabel("Exception Breakpoint Creator");
   pnl.addSeparator();
   exception_box = pnl.addChoice("EXCEPTION",(new String [] { "Computing List of Exceptions ..." }),null,false,null);
   caught_button = pnl.addBoolean("CAUGHT",true,null);
   uncaught_button = pnl.addBoolean("UNCAUGHT",true,null);
   suspendvm_button = pnl.addBoolean("SUSPEND VM",false,null);
   subclass_button = pnl.addBoolean("INCLUDE SUBCLASSES",true,null);

   pnl.addBottomButton("CANCEL","CANCEL",this);
   pnl.addBottomButton("APPLY","APPLY",this);
   pnl.addBottomButton("DEFINE","DEFINE",this);
   pnl.addSeparator();
   pnl.addBottomButtons();

   setContentPane(pnl);
}



/********************************************************************************/
/*										*/
/*	Action handler								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();

   if (cmd.equals("CANCEL")) {
      setVisible(false);
    }
   else if (cmd.equals("APPLY")) {
      createBreakpoint();
    }
   else if (cmd.equals("DEFINE")) {
      createBreakpoint();
      setVisible(false);
    }
}




private void createBreakpoint()
{
   String cls = (String) exception_box.getSelectedItem();
   if (cls == null || cls.length() == 0) return;
   BumpExceptionMode md = BumpExceptionMode.DEFAULT;
   boolean caught = caught_button.isSelected();
   boolean uncaught = uncaught_button.isSelected();
   if (caught & uncaught) md = BumpExceptionMode.ALL;
   else if (caught) md = BumpExceptionMode.CAUGHT;
   else if (uncaught) md = BumpExceptionMode.UNCAUGHT;
   boolean suspendvm = suspendvm_button.isSelected();
   BumpBreakMode bmd = BumpBreakMode.DEFAULT;
   if (suspendvm) bmd = BumpBreakMode.SUSPEND_VM;
   boolean subclass = subclass_button.isSelected();

   BumpClient bc = BumpClient.getBump();
   bc.getBreakModel().addExceptionBreakpoint(null,cls,md,bmd,subclass);
}




/********************************************************************************/
/*										*/
/*	Methods to get the set of exceptions					*/
/*										*/
/********************************************************************************/

private final class ExceptionSet implements Runnable {

   @Override public void run() {
      Set<String> etypes = new TreeSet<String>();
   
      // this doesn't get system classes although it should
      BumpClient bc = BumpClient.getBump();
      for (String nm : bc.findAllSubclasses("java.lang.Throwable")) {
         if (nm.startsWith("org.junit") && nm.contains(".internal.")) continue;
         if (nm.startsWith("org.eclipse") && nm.contains(".internal.")) continue;
         if (nm.contains("$")) continue;
         etypes.add(nm);
       }
      
      DefaultComboBoxModel<String> mdl = (DefaultComboBoxModel<String>) exception_box.getModel();
      mdl.removeAllElements();
      for (String s : etypes) mdl.addElement(s);
    }



}	// end of inner class ExceptionSet




}	// end of class BddtExceptionBreakpointBubble




/* end of BddtExceptionBreakpointBubble.java */

