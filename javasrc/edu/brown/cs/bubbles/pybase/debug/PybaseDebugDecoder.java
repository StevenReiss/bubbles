/********************************************************************************/
/*										*/
/*		PybaseDebugDecoder.java 					*/
/*										*/
/*	Decoding methods for XML from the python debugger interface		*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;

import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseFileSystem;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



class PybaseDebugDecoder implements PybaseDebugConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/




/********************************************************************************/
/*										*/
/*	Get threads from XML output						*/
/*										*/
/********************************************************************************/

static List<PybaseDebugThread> decodeThreads(PybaseDebugTarget target,String payload)
{
   Element e = IvyXml.convertStringToXml(payload);
   if (e == null) return null;

   List<PybaseDebugThread> rslt = new ArrayList<PybaseDebugThread>();
   for (Element te : IvyXml.elementsByTag(e,"thread")) {
      String nm = IvyXml.getAttrString(te,"name");
      String id = IvyXml.getAttrString(te,"id");
      rslt.add(new PybaseDebugThread(target,nm,id));
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Get variables from XML output						*/
/*										*/
/********************************************************************************/


/*
 <xml>
   <thread id="id" stop_reason="reason" />
   <frame id="id" name="functionName " file="file" line="line" />
   ...
 </xml>
*/


static ThreadReturn debugStack(PybaseDebugTarget target,String payload)
	throws PybaseException
{
   Element xml = IvyXml.convertStringToXml(payload);
   PybaseDebugThread thread = null;
   String reason = null;
   List<PybaseDebugStackFrame> frames = new ArrayList<PybaseDebugStackFrame>();

   for (Element ce : IvyXml.children(xml)) {
      if (IvyXml.isElement(ce,"thread")) {
	 String tid = IvyXml.getAttrString(ce,"id");
	 thread =  target.findThreadById(tid);
	 if (tid == null) throw new PybaseException("Thread " + tid + " not found");
	 reason = IvyXml.getAttrString(ce,"stop_reason");
       }
      else if (thread != null && IvyXml.isElement(ce,"frame")) {
	 String name = IvyXml.getAttrString(ce,"name");
	 String id = IvyXml.getAttrString(ce,"id");
	 String file = IvyXml.getAttrString(ce,"file");
	 file = PybaseFileSystem.getFileAbsolutePath(file);
	 File f = new File(file);
	 int line = IvyXml.getAttrInt(ce,"line");
	 PybaseDebugStackFrame frame = thread.findStackFrameByID(id);
	 if (frame == null) {
	    frame = new PybaseDebugStackFrame(thread,id,name,f,line,target);
	  }
	 else {
	    frame.setName(name);
	    frame.setFile(f);
	    frame.setLine(line);
	  }
	 frames.add(frame);
       }
    }

   return new ThreadReturn(thread,reason,frames);
}




/********************************************************************************/
/*										*/
/*	Handle CMD_GET_VARIABLE return						*/
/*										*/
/********************************************************************************/

static List<PybaseDebugVariable> decodeVariables(PybaseDebugTarget target,
						    String locator,String payload)
{
   Element e = IvyXml.convertStringToXml(payload);
   List<PybaseDebugVariable> vars = new ArrayList<PybaseDebugVariable>();

   for (Element ce : IvyXml.elementsByTag(e,"var")) {
      vars.add(createVariable(target,locator,ce));
    }

   return vars;
}




/********************************************************************************/
/*										*/
/*	Handle CMD_GET_COMPLETIONS						*/
/*										*/
/********************************************************************************/

static List<Object []> decodeCompletions(String payload)
{
   Element xml = IvyXml.convertStringToXml(payload);
   List<Object []> completions = new ArrayList<Object []>();

   for (Element ce : IvyXml.elementsByTag(xml,"comp")) {
      Object [] comp = new Object [] { IvyXml.getAttrString(ce,"p0"),
					  IvyXml.getAttrString(ce,"p1"),
					  IvyXml.getAttrString(ce,"p2"),
					  IvyXml.getAttrString(ce,"p3") };
      completions.add(comp);
    }

   return completions;
}




/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

/**
 * Creates a variable from XML attributes
 * <var name="self" type="ObjectType" value="<DeepThread>"/>
 */

static private PybaseDebugVariable createVariable(PybaseDebugTarget target,String locator,Element xml)
{
   PybaseDebugVariable var = null;

   String name = IvyXml.getAttrString(xml,"name");
   String type = IvyXml.getAttrString(xml,"type");
   String value = IvyXml.getAttrString(xml,"value");
   boolean contain = IvyXml.getAttrBool(xml,"isContainer");

   if (contain) {
      var = new PybaseDebugVariableCollection(target,name,type,value,locator);
    }
   else {
      var = new PybaseDebugVariable(target,name,type,value,locator);
    }

   return var;
}







}	// end of class PybaseDebugDecoder




/* end of PybaseDebugDecoder.java */
