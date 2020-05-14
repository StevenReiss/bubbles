/********************************************************************************/
/*										*/
/*		BandaidAgent.java						*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data generic agent		*/
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



package edu.brown.cs.bubbles.bandaid;



import java.lang.instrument.ClassFileTransformer;
import java.lang.management.ThreadInfo;


abstract class BandaidAgent implements BandaidConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected BandaidController the_control;

private String		agent_name;
private long		mon_total;
private long		mon_start;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BandaidAgent(BandaidController dc,String name)
{
   the_control = dc;
   agent_name = name;

   mon_total = 0;
   mon_start = 0;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 		{ return agent_name; }



/********************************************************************************/
/*										*/
/*	Monitoring and timing methods						*/
/*										*/
/********************************************************************************/

void enableMonitoring(boolean fg,long now)
{
   if (fg) {
      mon_start = now;
      handleMonitorStart(now);
    }
   else {
      if (mon_start != 0 && now > mon_start) mon_total += now-mon_start;
      mon_start = 0;
      handleMonitorStop(now);
    }
}



long getMonitoredTime(long now)
{
   long r = mon_total;
   if (mon_start > 0) r += now-mon_start;

   return r;
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

void generateReport(BandaidXmlWriter xw,long now)      { }




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc) { }

void handleDoneStacks(long now) 					{ }

protected void handleMonitorStart(long now)				{ }

protected void handleMonitorStop(long now)				{ }

void handleCommand(String cmd,String args)				{ }

ClassFileTransformer getTransformer()					{ return null; }


}	// end of abstract class BandaidAgent




/* end of BandaidAgent.java */

