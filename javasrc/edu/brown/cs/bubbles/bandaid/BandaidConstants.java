/********************************************************************************/
/*										*/
/*		BandaidConstants.java						*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data system constant definitions	*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bandaid;



public interface BandaidConstants {



/********************************************************************************/
/*										*/
/*	Messaging constants							*/
/*										*/
/********************************************************************************/

int		BANDAID_PORT = 37777;

String		BANDAID_TRAILER = "EBANDAIDMSG";
String		BANDAID_THREAD = "BandaidMonitorThread";



/********************************************************************************/
/*										*/
/*	Timer constants 							*/
/*										*/
/********************************************************************************/

long		BANDAID_CHECK_TIME = 33;
long		BANDAID_DISABLE_TIME = 1000;
long		BANDAID_REPORT_TIME = 100;

long		BANDAID_MAX_DELAY = 10000;



/********************************************************************************/
/*										*/
/*	Class types								*/
/*										*/
/********************************************************************************/

enum ClassType {
   NORMAL,
   SYSTEM,
   IO,
   SYSTEM_IO;

   public boolean isIO() {
      return this == IO || this == SYSTEM_IO;
    }

   public boolean isCOLLECTION() {
      return this == IO;
    }

   public boolean isSYSTEM() {
      return this == SYSTEM || this == SYSTEM_IO;
    }

}	// end of enum ClassType



/********************************************************************************/
/*										*/
/*	Instrumentation constants						*/
/*										*/
/********************************************************************************/

int BANDAID_MAX_THREADS = 32768;	  // max thread ID is one less than this

int BANDAID_MAX_DEPTH = 128;

String TRACE_DATA_FILE = "tracedata.bandaid";

int	TRACE_ENTER = 0x1;
int	TRACE_EXIT = 0x2;
int	TRACE_CONSTRUCTOR = 0x4;




}	// end of interface BandaidConstants




/* end of BandaidConstants.java */

