/********************************************************************************/
/*										*/
/*		BpareMain.java							*/
/*										*/
/*	Bubbles Pattern-Assisted Recommendation Engine main program		*/
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



package edu.brown.cs.bubbles.bpare;

import java.util.ArrayList;
import java.util.List;



public final class BpareMain implements BpareConstants
{



/********************************************************************************/
/*										*/
/*	Main Program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BpareMain bm = new BpareMain(args);
   bm.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		mint_handle;
private BpareMonitor	the_monitor;
private ProcessMode	process_mode;
private List<BpareProject> all_projects;
private PatternType	pattern_type;


enum ProcessMode {
   SERVER,			// act as a bubbles server
   BUILDER,			// just build patterns
   MATCH,			// just match patterns
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BpareMain(String [] args)
{
   mint_handle = null;
   process_mode = ProcessMode.SERVER;
   the_monitor = null;
   all_projects = new ArrayList<BpareProject>();
   pattern_type = PatternType.STRING;

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {           // -m <mint handle>
	    mint_handle = args[++i];
	  }
	 else if (args[i].startsWith("-M")) {                           // -MATCH
	    process_mode = ProcessMode.MATCH;
	  }
	 else if (args[i].startsWith("-B")) {                           // -BUILD
	    process_mode = ProcessMode.BUILDER;
	  }
	 else if (args[i].startsWith("-S")) {                           // -SERVER
	    process_mode = ProcessMode.SERVER;
	  }
	 else if (args[i].startsWith("-t")) {                           // -trie
	    pattern_type = PatternType.TRIE;
	  }
	 else if (args[i].startsWith("-s")) {                           // -string
	    pattern_type = PatternType.STRING;
	  }
	 else badArgs();
       }
      else badArgs();
    }
   
   if (mint_handle == null) badArgs();
}



private void badArgs()
{
   System.err.println("BPARE: bparemain -m <mint>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

PatternType getPatternType()		{ return pattern_type; }

Iterable<BpareProject> getProjects()	{ return all_projects; }


BpareProject getProject(String nm)
{
   for (BpareProject bp : all_projects) {
      if (bp.getName().equals(nm)) return bp;
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   the_monitor = new BpareMonitor(this,mint_handle);

   switch (process_mode) {
      case SERVER :
	 the_monitor.server();
	 break;
      case BUILDER :
	 loadProjects();
	 for (BpareProject bp : all_projects) {
	    BpareBuilder bb = new BpareBuilder(bp,pattern_type);
	    bb.process();
	 }
	 break;
      case MATCH :
	 loadProjects();
	 break;
    }
}



/********************************************************************************/
/*										*/
/*	Project Management							*/
/*										*/
/********************************************************************************/

private void loadProjects()
{
   the_monitor.loadProjects();

   if (all_projects.isEmpty()) {
      System.err.println("BPARE: Not projects to manage");
      System.exit(0);
    }
}




void addProject(BpareProject bp)
{
   all_projects.add(bp);
}



}	// end of class BpareMain




/* end of BpareMain.java */

