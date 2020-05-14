/********************************************************************************/
/*										*/
/*		PybaseDebugStackFrame.java					*/
/*										*/
/*	Handle debugging stack frame for Bubbles from Python			*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class PybaseDebugStackFrame implements PybaseDebugConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String frame_name;
private PybaseDebugThread frame_thread;
private String frame_id;
private File frame_file;
private int frame_line;
private PybaseDebugVariable[] frame_variables;
private String locals_locator;
private String globals_locator;
private String frame_locator;
private PybaseDebugTarget debug_target;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseDebugStackFrame(PybaseDebugThread thread,String id,String name,File file,int line,
		PybaseDebugTarget target) {
   frame_id = id;
   frame_name = name;
   frame_file = file;
   frame_line = line;
   frame_thread = thread;
   locals_locator = frame_thread.getRemoteId() + "\t" + id + "\tLOCAL";
   frame_locator = frame_thread.getRemoteId() + "\t" + id + "\tFRAME";
   globals_locator = frame_thread.getRemoteId() + "\t" + id + "\tGLOBAL";
   debug_target = target;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public PybaseDebugTarget getTarget()			{ return debug_target; }
public String getId()					{ return frame_id; }
public String getThreadRemoteId()				{ return frame_thread.getRemoteId(); }
public String getLocalsLocator()			{ return locals_locator; }
public String getFrameLocator() 			{ return frame_locator; }
public String getGlobalLocator()			{ return globals_locator; }

public File getFile()					{ return frame_file; }
public PybaseDebugThread getThread()			{ return frame_thread; }

void setName(String n)					{ frame_name = n; }
void setFile(File f)					{ frame_file = f; }
void setLine(int l)					{ frame_line = l; }

void setVariables(PybaseDebugVariable [] locals)	{ frame_variables = locals; }

public PybaseDebugVariable [] getVariables()
{
   if (frame_variables == null) {
      frame_variables = new PybaseDebugVariable[0];
      // get variables from frame data or python
    }
   return frame_variables;
}


public Map<String,PybaseDebugVariable> getVariablesAsMap()
{
   Map<String,PybaseDebugVariable> map = new HashMap<String,PybaseDebugVariable>();
   for (PybaseDebugVariable v : frame_variables) {
      map.put(v.getName(),v);
    }
   return map;
}

public boolean hasVariables()				{ return true; }
public int getLineNumber()				{ return frame_line; }
public String getName()
{
   return frame_name + " [" + frame_file.getName() + ":" + Integer.toString(frame_line) + "]";
}

public PybaseDebugTarget getDebugTarget()		{ return frame_thread.getDebugTarget(); }





public PybaseDebugCommand.GetVariable getFrameCommand(PybaseDebugTarget dbg)
{
   return new PybaseDebugCommand.GetFrame(dbg,frame_locator);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw,int lvl)
{
   xw.begin("STACKFRAME");
   xw.field("NAME",frame_name);
   xw.field("ID",frame_id);
   xw.field("LINENO",frame_line);
   xw.field("METHOD",frame_name);
   if (lvl >= 0) xw.field("LEVEL",lvl);
   xw.field("FILE",frame_file.getAbsolutePath());
   xw.field("FILETYPE","PYTHON");
   // handle args here if there are any
   xw.end("STACKFRAME");
}



}	// end of class PybaseDebugStackFrame




/* end of PybaseDebugStackFrame.java */
