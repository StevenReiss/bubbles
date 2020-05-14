/********************************************************************************/
/*										*/
/*		BnoteConstants.java						*/
/*										*/
/*	Constants for maintaining a programmers log/note book			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bnote;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.Date;
import java.util.Set;


public interface BnoteConstants
{


/********************************************************************************/
/*										*/
/*	Log Entry types 							*/
/*										*/
/********************************************************************************/

enum BnoteEntryType {
   NONE,
   TASK,		// specify task being worked on
   OPEN,		// open an editor
   CLOSE,		// close an editor
   EDIT,		// change code
   SAVE,		// save files
   NOTE,		// user note
   ATTACHMENT,		// user attachment
   COPY,		// copy from one editor to another
   NEW_TASK,
}




/********************************************************************************/
/*										*/
/*	Log Entry Keys								*/
/*										*/
/********************************************************************************/

enum BnoteKey {
   KEY_PROJECT,
   KEY_TYPE,
   KEY_TIME,
   KEY_USER,
   KEY_FILE,
   KEY_TEXT,
}



   
/********************************************************************************/
/*										*/
/*	Task Definition 							*/
/*										*/
/********************************************************************************/

interface BnoteTask {

   long getTaskId();
   String getName();
   String getProject();
   String getDescription();
   Date getFirstTime();
   Date getLastTime();
   
   void outputXml(IvyXmlWriter xw);

}	// end of interface BnoteTask



/********************************************************************************/
/*										*/
/*	Entry definition							*/
/*										*/
/********************************************************************************/

interface BnoteEntry {

   String getProject();
   BnoteTask getTask();
   BnoteEntryType getType();
   String getUser();
   Date getTime();
   String getProperty(String id);
   Set<String> getPropertyNames();

   void outputXml(IvyXmlWriter xw);
   
}	// end of interface BnoteEntry




/********************************************************************************/
/*										*/
/*	Database value definitions						*/
/*										*/
/********************************************************************************/

interface BnoteValue {

   String getDatabaseValue();

}	// end of interface BnoteValue



/********************************************************************************/
/*										*/
/*	Limits									*/
/*										*/
/********************************************************************************/

int MAX_ATTACHMENT_SIZE = 16*1024*1024;



}	// end of interface BnoteConstants




/* end of BnoteConstants.java */

