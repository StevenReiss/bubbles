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

import java.util.List;

import javax.swing.JOptionPane;



/**
 *	This class provides an interface between Code Bubbles and the back-end
 *	IDE.  This particular implementation works for ECLIPSE.
 *
 *	At some point, this should be converted into an interface that can then
 *	be implemented by an appropriate class for each back end IDE.
 *
 **/

abstract class BumpClientJava extends BumpClient
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpClientJava()
{ }



/********************************************************************************/
/*                                                                              */
/*      Backend startup helper methods                                          */
/*                                                                              */
/********************************************************************************/

protected boolean checkIfRunning()
{
   for ( ; ; ) {
      String r = getStringReply("PING",null,null,null,60000);
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
      else return true;
    }
   
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BoardLog.logE("BUMP","Client mode with no backend IDE found");
      JOptionPane.showMessageDialog(null,
            "Server must be running and accessible before client can be run",
				       "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
   
   return false;
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
