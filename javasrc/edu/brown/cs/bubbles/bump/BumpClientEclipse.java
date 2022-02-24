/********************************************************************************/
/*                                                                              */
/*              BumpClientEclipse.java                                          */
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



package edu.brown.cs.bubbles.bump;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JOptionPane;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.exec.IvyExec;

class BumpClientEclipse extends BumpClientJava
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/


private boolean 	eclipse_starting;

private String [] MAC_BINARY = new String [] {
      "Contents/MacOS/eclipse", "Contents/Eclispe/eclipse"
};


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BumpClientEclipse()
{
   eclipse_starting = false;
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new IDEHandler());
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/


@Override public String getName()		{ return "Eclipse"; }

@Override public String getServerName() 	{ return "BEDROCK"; }



/********************************************************************************/
/*                                                                              */
/*      Eclipse startup methods                                                 */
/*                                                                              */
/********************************************************************************/

@Override void localStartIDE()
{
   synchronized (this) {
      if (eclipse_starting) return;
      eclipse_starting = true;
    }
   
   ensureRunning();
}




private void ensureRunning()
{
   if (checkIfRunning()) return;
   
   String eclipsedir = board_properties.getProperty(BOARD_PROP_BASE_IDE_DIR);
   String ws = board_properties.getProperty(BOARD_PROP_WORKSPACE);
   
   File ef = new File(eclipsedir);
   File ef1 = null;
   for (String s : BOARD_ECLIPSE_START) {
      ef1 = new File(ef,s);
      if (ef1.exists() && ef1.canExecute()) break;
    }
   if (ef1 != null && ef1.isDirectory()) {
      for (String s : MAC_BINARY) {
	 File ef2 = new File(ef1,s);
	 if (ef2.exists() && ef2.canExecute()) {
	    ef1 = ef2;
	    break;
	  }
       }
    }
   
   if (ef1 == null || !ef1.exists() || !ef1.canExecute()) ef1 = new File(ef,"eclipse");
   String efp = ef1.getPath();
   if (efp.endsWith(".app") || efp.endsWith(".exe")) efp = efp.substring(0,efp.length()-4);
   String cmd = "'" + efp + "'";
   
   cmd += " -application edu.brown.cs.bubbles.bedrock.application";
   
   cmd += " -nosplash";
   if (ws != null) cmd += " -data '" + ws + "'";
   
   String eopt = board_properties.getProperty(BOARD_PROP_BASE_IDE_OPTIONS);
   eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_OPTIONS,eopt);
   if (eopt != null) cmd += " " + eopt;
   
   if (board_properties.getBoolean(BOARD_PROP_ECLIPSE_FOREGROUND,false)) {
      cmd += " -bdisplay";
    }
   
   boolean clean = board_properties.getBoolean(BOARD_PROP_ECLIPSE_CLEAN);
   if (ws != null) {
      File wf = new File(ws);
      File cf = new File(wf,".clean");
      if (cf.exists()) {
	 clean = true;
	 cf.delete();
       }
    }
   
   if (clean) {
      if (!cmd.contains("-clean")) cmd += " -clean";
      board_properties.remove(BOARD_PROP_ECLIPSE_CLEAN);
      try {
	 board_properties.save();
       }
      catch (IOException e) { }
    }
   
   cmd += " -vmargs '-Dedu.brown.cs.bubbles.MINT=" + mint_name + "'";
   cmd += " -Dorg.eclipse.jdt.ui.codeAssistTimeout=30000";
   
   eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS);
   if (eopt != null) {
      int idx = eopt.indexOf("-Xrunjdwp");
      if (idx >= 0) {
	 int idx1 = eopt.indexOf("address=",idx);
	 if (idx1 >= 0) {
	    int idx2 = idx1+8;
	    int idx3 = idx2;
	    while (idx3 < eopt.length()) {
	       char c = eopt.charAt(idx3);
	       if (!Character.isDigit(c)) break;
	       ++idx3;
	     }
	    String port = eopt.substring(idx2,idx3);
	    try {
	       int portno = Integer.parseInt(port);
	       for ( ; ; ) {
		  try {
		     Socket s = new Socket((String) null,portno);
		     s.close();
		     ++portno;
		     continue;
		   }
		  catch (IOException e) {
		     break;
		   }
		}
	       eopt = eopt.substring(0,idx2) + (portno) + eopt.substring(idx3);
	     }
	    catch (NumberFormatException e) { }
	  }
       }
      cmd += " " + eopt;
    }
   
   BoardLog.logD("BUMP","Start Eclipse: " + cmd);
   
   // remove snapshots because we are going to refresh anyway
   File f1 = new File(ws);
   File f2 = new File(f1,".metadata");
   File f3 = new File(f2,".plugins");
   File f4 = new File(f3,"org.eclipse.core.resources");
   File f5 = new File(f4,".snap");
   if (f5.exists()) f5.delete();
   
   try {
      IvyExec ex = new IvyExec(cmd);
      boolean eok = false;
      for (int i = 0; i < 700; ++i) {
	 synchronized (this) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
	 if (tryPing()) {
	    BoardLog.logI("BUMP","Eclipse started successfully");
	    eok = true;
	    break;
	  }
	 if (!ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting eclipse");
	    if (BoardSetup.getSetup().getRunMode() != RunMode.SERVER) {
	       JOptionPane.showMessageDialog(null,
		     "Eclipse could not be started. Check the eclipse log",
		     "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	     }
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","Eclipse doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running eclipse: " + e);
      System.exit(1);
    }
}




}       // end of class BumpClientEclipse




/* end of BumpClientEclipse.java */

