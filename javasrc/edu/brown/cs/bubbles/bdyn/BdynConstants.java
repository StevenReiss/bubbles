/********************************************************************************/
/*										*/
/*		BdynConstants.java						*/
/*										*/
/*	Constants for dynamic visualizations in Bubbles 			*/
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



package edu.brown.cs.bubbles.bdyn;

import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.awt.Color;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Set;


public interface BdynConstants extends BumpConstants
{


/********************************************************************************/
/*										*/
/*	Trie Node access							*/
/*										*/
/********************************************************************************/

interface TrieNode {
   
   TrieNode getParent();
   Collection<TrieNode> getChildren();
   int [] getCounts();
   Collection<BumpThread> getThreads();
   int [] getThreadCounts(BumpThread th);

   String getClassName();
   String getMethodName();
   int getLineNumber();
   String getFileName();
   
   int [] getTotals();
   void computeTotals();
   
}



/********************************************************************************/
/*										*/
/*     Array elements for counts						*/
/*										*/
/********************************************************************************/

int OP_RUN = 0;
int OP_IO = 1;
int OP_WAIT = 2;
int OP_COUNT = 3;



/********************************************************************************/
/*										*/
/*	Callback data								*/
/*										*/
/********************************************************************************/


enum CallbackType {
   UNKNOWN,
   EVENT,		// event handler
   CONSTRUCTOR, 	// constructor for event recognition
   KEY,                 // key routine (significant time spent here
   MAIN,                // main program
};



long    MAX_TIME        = Long.MAX_VALUE;
long    MERGE_TIME      = 100000000;    // 100 ms 
long    IGNORE_TIME     = 10000000;     // 10 ms



interface BdynCallback {
   String getClassName();
   String getMethodName();
   String getArgs();
   String getDisplayName();
   int getId();
   CallbackType getCallbackType();
   void setLabel(String lbl);
   void setUserColor(Color c);
   Color getUserColor();
}



/********************************************************************************/
/*										*/
/*	Graph data								*/
/*										*/
/********************************************************************************/

interface BdynEntry {
   long getStartTime();
   long getEndTime(long max);
   BdynEntryThread getEntryThread();
   BdynCallback getEntryTask();
   BdynCallback getEntryTransaction();
   long getTotalTime(long max);
   
}

interface BdynEntryThread {
   String getThreadName();
}       // end of interface BdynEntryThread

interface BdynEntryTask {
   BdynCallback getTaskRoot();
}       // end of interface BdynEntryTask


class BdynRangeSet extends HashMap<BdynEntryThread,Set<BdynEntry>> { }


interface BdynEventUpdater extends EventListener {
   
   void eventsAdded();
   
}       // end of interface BdynEventUpdater


/********************************************************************************/
/*                                                                              */
/*      Options                                                                 */
/*                                                                              */
/********************************************************************************/

interface BdynOptions { 
   
   boolean useKeyCallback();
   boolean useMainCallback();
   boolean useMainTask();
   
   void setUseKeyCallback(boolean fg);
   void setUseMainCallback(boolean fg);
   void setUseMainTask(boolean fg);
   
   void save(IvyXmlWriter xw);
   void load(Element elt);
   
}       // end of interface BdynOptions



/********************************************************************************/
/*										*/
/*	Files									*/
/*										*/
/********************************************************************************/

String BDYN_CALLBACK_FILE = "callbacks.xml";
String BDYN_BANDAID_FILE = "tracedata.bandaid";


}	// end of interface BdynConstants




/* end of BdynConstants.java */

