/********************************************************************************/
/*										*/
/*		BvcrConstants.java						*/
/*										*/
/*	Bubble Version Collaboration Repository constant definitions		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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


/* SVN: $Id$ */


package edu.brown.cs.bubbles.bvcr;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.EventListener;



public interface BvcrConstants {



/********************************************************************************/
/*										*/
/*	BvcrCollaborationEvent -- event sent between users			*/
/*										*/
/********************************************************************************/

interface BvcrCollaborationEvent {

   String getType();
   void writeXml(IvyXmlWriter xw);
   boolean isStoreAndForward();

}



interface BvcrEventHandler {

   void collaborate(BvcrCollaborationEvent evt);

}


/********************************************************************************/
/*										*/
/*	Encryption constants							*/
/*										*/
/********************************************************************************/

byte [] KEY_SALT = new byte [] {
   (byte)0xa9,(byte)0x9b,(byte)0xc8,(byte)0x32,
   (byte)0x56,(byte)0x35,(byte)0xe3,(byte)0x03
};

int KEY_COUNT = 19;

boolean KEY_COMPRESS = true;



/********************************************************************************/
/*                                                                              */
/*      File change and line sets interfaces                                                         */
/*                                                                              */
/********************************************************************************/

interface BvcrFileChange {
   
   int getSourceLine();
   int getTargetLine();
   String [] getDeletedLines();
   String [] getAddedLines();
   
}       // end of interface BvcrFileChange


interface BvcrLineChange {
   
   int getLineNumber();
   
}       // end of interface BvcrLineChange



/********************************************************************************/
/*                                                                              */
/*      File status information                                                 */
/*                                                                              */
/********************************************************************************/

enum FileState {
   UNMODIFIED,
   MODIFIED, 
   ADDED, 
   DELETED,
   RENAMED,
   COPIED,
   UNMERGED,
   UNTRACKED,
   IGNORED
}



/********************************************************************************/
/*                                                                              */
/*      Control Panel access callbacks                                          */
/*                                                                              */
/********************************************************************************/

interface BvcrProjectUpdated extends EventListener {
   
   void projectUpdated(BvcrControlPanel panel);
   
}


}	// end of interface BattConstants




/* end of BattConstants.java */

