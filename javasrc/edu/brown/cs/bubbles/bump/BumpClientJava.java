/********************************************************************************/
/*										*/
/*		BumpClientJava.java						*/
/*										*/
/*	BUblles Mint Partnership main class for using Eclipse/Java		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Hsu-Sheng Ko	*/
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



package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;

import edu.brown.cs.ivy.exec.IvyExec;

import javax.swing.JOptionPane;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.List;



/**
 *	This class provides an interface between Code Bubbles and the back-end
 *	IDE.  This particular implementation works for ECLIPSE.
 *
 *	At some point, this should be converted into an interface that can then
 *	be implemented by an appropriate class for each back end IDE.
 *
 **/

public class BumpClientJava extends BumpClient
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	eclipse_starting;

private String [] MAC_BINARY = new String [] {
      "Contents/MacOS/eclipse", "Contents/Eclispe/eclipse"
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpClientJava()
{
   eclipse_starting = false;

   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new IDEHandler());
}



/********************************************************************************/
/*										*/
/*	Eclipse interaction methods						*/
/*										*/
/********************************************************************************/

/**
 *	Return the name of the back end.
 **/

@Override public String getName()		{ return "Eclipse"; }
@Override public String getServerName() 	{ return "BEDROCK"; }



/**
 *	Start the back end running.  This routine will return immediately.  If the user
 *	actually needs to use the backend, they should use waitForIDE.
 **/

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
   for ( ; ; ) {
      String r = getStringReply("PING",null,null,null,5000);
      if (r != null && r.startsWith("<RESULT>")) {
	 r = r.substring(8);
	 int idx = r.indexOf("<");
	 if (idx >= 0) r = r.substring(0,idx);
      }
      if (r == null) break;		// nothing there
      if (r.equals("EXIT") || r.equals("UNSET")) {
	 BoardLog.logX("BUMP","Eclipse caught during exit " + r);
	 // Eclipse is exiting; wait for that and try again
	 try {
	    Thread.sleep(3000);
	  }
	 catch (InterruptedException e) { }
       }
      else return;			// eclipse already running
    }

   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BoardLog.logE("BUMP","Client mode with no eclipse found");
      JOptionPane.showMessageDialog(null,
				       "Server must be running and accessible before client can be run",
				       "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

   String eclipsedir = board_properties.getProperty(BOARD_PROP_ECLIPSE_DIR);
   String ws = board_properties.getProperty(BOARD_PROP_ECLIPSE_WS);

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

   if (!board_properties.getBoolean(BOARD_PROP_ECLIPSE_FOREGROUND,false)) {
      cmd += " -application edu.brown.cs.bubbles.bedrock.application";
    }
   cmd += " -nosplash";
   if (ws != null) cmd += " -data '" + ws + "'";

   String eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_OPTIONS);
   if (eopt != null) cmd += " " + eopt;

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





/********************************************************************************/
/*										*/
/*	Java Search queries							*/
/*										*/
/********************************************************************************/

/**
 *	Find the loction of a method given the project and name.  If the name is
 *	ambiguous, mutliple locations may be returned.	The name can contain a
 *	parenthesized argument list, i.e. package.method(int,java.lang.String).
 **/

@Override public List<BumpLocation> findMethod(String proj,String name,boolean system)
{
   boolean cnstr = false;
   name = name.replace('$','.');
   name = removeGenerics(name);

   String nm0 = name;
   int idx2 = name.indexOf("(");
   String args = "";
   if (idx2 >= 0) {
      nm0 = name.substring(0,idx2);
      args = name.substring(idx2);
    }

   int idx0 = nm0.lastIndexOf(".");
   if (idx0 >= 0) {
      String mthd = nm0.substring(idx0+1);
      int idx1 = nm0.lastIndexOf(".",idx0-1);
      String clsn = nm0.substring(idx1+1,idx0);
      if (mthd.equals(clsn) || mthd.equals("<init>")) {
	 cnstr = true;
	 nm0 = nm0.substring(0,idx0);
	 if (idx2 > 0) nm0 += args;
	 name = nm0;
       }
    }

   if (name == null) {
      BoardLog.logI("BUMP","Empty name provided to java search");
      return null;
    }

   List<BumpLocation> locs = findMethods(proj,name,false,true,cnstr,system);

   if (locs == null || locs.isEmpty()) {
      int x1 = name.indexOf('(');
      if (cnstr && x1 >= 0) {				// check for nested constructor with extra argument
	 String mthd = name.substring(0,x1);
	 int x2 = mthd.lastIndexOf('.');
	 if (x2 >= 0) {
	    String pfx = name.substring(0,x2);
	    if (args.startsWith("(" + pfx + ",") || args.startsWith("(" + pfx + ")")) {
	       int ln = pfx.length();
	       args = "(" + args.substring(ln+2);
	       name = mthd + args;
	       locs = findMethods(proj,name,false,true,cnstr,system);
	    }
	 }
      }
   }

   return locs;
}



@Override protected String localFixupName(String nm)
{
   if (nm == null) return null;
   nm = nm.replace('$','.');
   return nm;
}


private String removeGenerics(String name)
{
   if (!name.contains("<")) return name;

   StringBuffer buf = new StringBuffer();
   boolean insideargs = false;
   boolean atdot = true;
   int lvl = 0;

   for (int i = 0; i < name.length(); ++i) {
      char c = name.charAt(i);
      if ((insideargs || !atdot) && c == '<') ++lvl;
      else if (lvl > 0 && c == '>') --lvl;
      else if (lvl == 0) {
	 buf.append(c);
	 if (c == '(') insideargs = true;
	 else if (c == '.') atdot = true;
	 else atdot = false;
      }
    }

   return buf.toString();
}



}	// end of class BumpClientJava




/* end of BumpClientJava.java */
