/********************************************************************************/
/*										*/
/*		BdynProcess.java						*/
/*										*/
/*	Maintain dynamic information for a single process			*/
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

import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.StringTokenizer;


class BdynProcess implements BdynConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpTrieData	bump_trie_data;
private BdynEventTrace	event_trace;

private int		trace_seq;

private PriorityQueue<Element> trace_events;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdynProcess(BumpProcess bp)
{
   trace_seq = 0;
   trace_events = new PriorityQueue<Element>(10,new EventComparator());

   event_trace = new BdynEventTrace(bp);

   bump_trie_data = BumpClient.getBump().getTrieData(bp);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpTrieNode getTrieRoot()		{ return bump_trie_data.getRoot(); }

double getBaseSamples() 		{ return bump_trie_data.getBaseSamples(); }
double getTotalSamples()		{ return bump_trie_data.getTotalSamples(); }
double getBaseTime()			{ return bump_trie_data.getBaseTime(); }
BdynEventTrace getEventTrace()		{ return event_trace; }


void finish()
{
   event_trace = null;
}



/********************************************************************************/
/*										*/
/*	Event Handlers								*/
/*										*/
/********************************************************************************/

synchronized void handleTraceEvent(Element xml)
{
   int seqid = IvyXml.getAttrInt(xml,"SEQ");
   if (seqid == trace_seq+1) {
      processTraceEvent(xml);
      while (!trace_events.isEmpty()) {
	 Element e1 = trace_events.element();
	 int sq = IvyXml.getAttrInt(e1,"SEQ");
	 if (sq != trace_seq+1) break;
	 e1 = trace_events.remove();
	 processTraceEvent(e1);
       }
    }
   else {
      trace_events.add(xml);
    }
}



private static class EventComparator implements Comparator<Element> {

   @Override public int compare(Element e1,Element e2) {
      int i1 = IvyXml.getAttrInt(e1,"SEQ");
      int i2 = IvyXml.getAttrInt(e2,"SEQ");
      if (i1 < i2) return -1;
      if (i1 > i2) return 1;
      return 0;
    }

}	// end of inner class EventComparator




/********************************************************************************/
/*										*/
/*	Trace event processing							*/
/*										*/
/********************************************************************************/

private void processTraceEvent(Element xml)
{
   trace_seq = IvyXml.getAttrInt(xml,"SEQ",trace_seq+1);
   String trace = IvyXml.getText(xml);
   if (trace == null) return;
   StringTokenizer tok = new StringTokenizer(trace,"\r\n");
   while (tok.hasMoreTokens()) {
      String ln = tok.nextToken();
      processTraceData(ln);
    }
}


private void processTraceData(String s)
{
   // BoardLog.logD("BDYN","TRACE: " + s);

   event_trace.addEntry(s);
}



}	// end of class BdynProcess




/* end of BdynProcess.java */

