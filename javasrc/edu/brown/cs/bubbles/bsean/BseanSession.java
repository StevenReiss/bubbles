/********************************************************************************/
/*                                                                              */
/*              BseanSession.java                                               */
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


package edu.brown.cs.bubbles.bsean;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;

class BseanSession implements BseanConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          session_id;
private Set<File>       added_files;

private static AtomicInteger id_counter = new AtomicInteger((int)(Math.random()*256000.0));



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BseanSession() throws BseanException
{
   session_id = "BSEAN_" + IvyExecQuery.getProcessId() + "_" + id_counter.incrementAndGet();
   added_files = new HashSet<>();
   
   Element rslt = sendFaitMessage("BEGIN",null,null);
   if (!IvyXml.isElement(rslt,"RESULT")) throw new BseanException("Failed to create session");
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getSessionId()                           { return session_id; }




/********************************************************************************/
/*                                                                              */
/*      Maintenance methods                                                     */
/*                                                                              */
/********************************************************************************/

void begin() throws BseanException
{
   Element rslt = sendFaitMessage("BEGIN",null,null);
   if (!IvyXml.isElement(rslt,"RESULT")) 
      throw new BseanException("BEGIN for session failed");
}


void startAnalysis() throws BseanException 
{
   Element rslt = sendFaitMessage("ANALYZE",null,null);
   if (!IvyXml.isElement(rslt,"RESULT")) 
      throw new BseanException("ANALYZE for session failed");
}



void handleEditorAdded(File f)
{
   if (f == null) return;
   
   StringBuffer buf = new StringBuffer();
   int ct = 0;
   if (added_files.add(f)) {
      buf.append("<FILE NAME='");
      buf.append(f.getAbsolutePath());
      buf.append("' />");
      ++ct;
    }
   if (ct > 0) {
      sendFaitMessage("ADDFILE",null,buf.toString());
    }
}

/********************************************************************************/
/*                                                                              */
/*      Messaging methods                                                       */
/*                                                                              */
/********************************************************************************/

Element sendFaitMessage(String cmd,CommandArgs args,String cnts)
{
   BseanFactory bf = BseanFactory.getFactory();
   return bf.sendFaitMessage(session_id,cmd,args,cnts);
}


}       // end of class BseanSession




/* end of BseanSession.java */

