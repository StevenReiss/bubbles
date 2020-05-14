/********************************************************************************/
/*										*/
/*		PybaseLaunchConfig.java 					*/
/*										*/
/*	Launch configuration representation for Python Bubbles			*/
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

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseProject;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class PybaseLaunchConfig implements PybaseDebugConstants, PybaseConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	config_name;
private String	config_id;
private int	config_number;
private File	base_file;
private Map<String,String> config_attrs;
private PybaseLaunchConfig original_config;
private PybaseLaunchConfig working_copy;
private boolean is_saved;

private static IdCounter launch_counter = new IdCounter();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseLaunchConfig(String nm)
{
   config_name = nm;
   config_number = launch_counter.nextValue();
   config_id = "LAUNCH_" + Integer.toString(config_number);
   config_attrs = new HashMap<String,String>();
   base_file = null;
   is_saved = false;
   working_copy = null;
   original_config = null;
}


PybaseLaunchConfig(Element xml)
{
   config_name = IvyXml.getAttrString(xml,"NAME");
   config_number = IvyXml.getAttrInt(xml,"ID");
   launch_counter.noteValue(config_number);
   config_id = "LAUNCH+" + Integer.toString(config_number);

   config_attrs = new HashMap<String,String>();
   for (Element ae : IvyXml.children(xml,"ATTR")) {
      config_attrs.put(IvyXml.getAttrString(ae,"KEY"),IvyXml.getAttrString(ae,"VALUE"));
    }
   String fn = IvyXml.getTextElement(xml,"FILE");
   if (fn == null) base_file = null;
   else base_file = new File(fn);
   is_saved = true;
   working_copy = null;
   original_config = null;
}



PybaseLaunchConfig(String nm,PybaseLaunchConfig orig)
{
   config_name = nm;
   config_number = launch_counter.nextValue();
   config_id = "LAUNCH_" + Integer.toString(config_number);
   config_attrs = new HashMap<String,String>(orig.config_attrs);
   base_file = orig.base_file;
   is_saved = false;
   working_copy = null;
   original_config = null;
}



// for creating a working copy
private PybaseLaunchConfig(PybaseLaunchConfig orig)
{
   config_name = orig.config_name;
   config_number = orig.config_number;
   config_id = orig.config_id;
   config_attrs = new HashMap<String,String>(orig.config_attrs);
   base_file = orig.base_file;
   is_saved = false;
   working_copy = null;
   original_config = orig;
}



/********************************************************************************/
/*										*/
/*	Working copy methods							*/
/*										*/
/********************************************************************************/

PybaseLaunchConfig getWorkingCopy()
{
   if (!is_saved) return this;
   if (working_copy == null) {
      working_copy = new PybaseLaunchConfig(this);
    }
   return working_copy;
}


void commitWorkingCopy()
{
   if (!is_saved) {
      if (original_config != null) {
	 original_config.commitWorkingCopy();
       }
      else is_saved = true;
    }
   else if (working_copy != null) {
      config_name = working_copy.config_name;
      base_file = working_copy.base_file;
      config_attrs = new HashMap<String,String>(working_copy.config_attrs);
      working_copy = null;
    }
}






/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return config_name; }
String getId()					{ return config_id; }
void setName(String nm) 			{ config_name = nm; }

public File getFileToRun()			{ return base_file; }
public void setFileToRun(File f)		{ base_file = f; }


void setAttribute(String k,String v)
{
   if (k == null) return;

   // if k == ATTR_MODULE, then find the corresponding file and call setFileToRun

   if (v == null) config_attrs.remove(k);
   else config_attrs.put(k,v);
}

boolean isSaved()				{ return is_saved; }
void setSaved(boolean fg)			{ is_saved = fg; }

public String [] getEnvironment()		{ return null; }

public File getWorkingDirectory()
{
   String s = config_attrs.get(ATTR_WD);
   if (s != null) {
      File f = new File(s);
      if (f.exists() && f.isDirectory()) return f;
    }

   try {
      String pnm = config_attrs.get(ATTR_PROJECT);
      if (pnm == null) return null;
      PybaseProject pp = PybaseMain.getPybaseMain().getProject(pnm);
      if (pp == null) return null;
      String files = config_attrs.get(ATTR_MODULE);
      File f2 = pp.findModuleFile(files);
      File f3 = f2.getParentFile();
      return f3;
    }
   catch (PybaseException e) { }

   return null;
}


public String getEncoding()
{
   return config_attrs.get(ATTR_ENCODING);
}


public String getPySrcPath()			{ return null; }

public String [] getCommandLine(PybaseDebugger dbg) throws PybaseException
{
   List<String> cmdargs = new ArrayList<String>();

   String pnm = config_attrs.get(ATTR_PROJECT);
   if (pnm == null) throw new PybaseException("No project specified");
   PybaseProject pp = PybaseMain.getPybaseMain().getProject(pnm);
   if (pp == null) throw new PybaseException("Project " + pnm + " not found");

   File f1 = pp.getBinary();
   cmdargs.add(f1.getAbsolutePath());
   cmdargs.add("-u");
   addVmArgs(cmdargs);
   addDebugArgs(pp,dbg,cmdargs);

   // should allow multiple files to be specified
   String files = config_attrs.get(ATTR_MODULE);
   File f2 = pp.findModuleFile(files);
   cmdargs.add(f2.getAbsolutePath());

   addUserArgs(cmdargs);

   String [] ret = new String[cmdargs.size()];
   ret = cmdargs.toArray(ret);

   return ret;
}


private void addVmArgs(List<String> cmdargs)
{
   String args = config_attrs.get(ATTR_PYTHON_ARGS);
   if (args == null || args.length() == 0) return;
   List<String> toks = IvyExec.tokenize(args);
   cmdargs.addAll(toks);
}


private void addDebugArgs(PybaseProject pp,PybaseDebugger dbg,List<String> cmdargs)
{
   if (pp.getNature().getInterpreterType() == PybaseInterpreterType.IRONPYTHON) {
      if (!cmdargs.contains("-X:Frames") && !cmdargs.contains("-X:FullFrames")) {
	 cmdargs.add("-X:FullFrames");
       }
    }

   cmdargs.add(getDebugScript());
   cmdargs.add("--vm_type");
   cmdargs.add("python");
   cmdargs.add("--client");
   cmdargs.add(IvyExecQuery.getHostName());
   cmdargs.add("--port");
   cmdargs.add(Integer.toString(dbg.getServerPort()));		  // shjould get actual port from listener
   // cmdargs.add("--DEBUG_RECORD_SOCKET_READS");
   cmdargs.add("--file");
}


private String getDebugScript()
{
   File f1 = PybaseMain.getPybaseMain().getRootDirectory();
   File f2 = new File(f1,"pybles");
   File f3 = new File(f2,"PyDebug");
   File f4 = new File(f3,"pydevd.py");
   return f4.getAbsolutePath();
}



private void addUserArgs(List<String> cmdargs)
{
   String args = config_attrs.get(ATTR_ARGS);
   if (args == null || args.length() == 0) return;
   List<String> toks = IvyExec.tokenize(args);
   cmdargs.addAll(toks);
}




/********************************************************************************/
/*										*/
/*	OutputMethods								*/
/*										*/
/********************************************************************************/

void outputSaveXml(IvyXmlWriter xw)
{
   xw.begin("CONFIG");
   xw.field("NAME",config_name);
   xw.field("ID",config_number);
   xw.textElement("FILE",base_file);
   for (Map.Entry<String,String> ent : config_attrs.entrySet()) {
      xw.begin("ATTR");
      xw.field("KEY",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("ATTR");
    }
   xw.end("CONFIG");
}


void outputBubbles(IvyXmlWriter xw)
{
   if (working_copy != null) {
      working_copy.outputBubbles(xw);
      return;
    }

   xw.begin("CONFIGURATION");
   xw.field("ID",config_id);
   xw.field("NAME",config_name);
   xw.field("WORKING",!is_saved);
   xw.field("DEBUG",true);
   for (Map.Entry<String,String> ent : config_attrs.entrySet()) {
      xw.begin("ATTRIBUTE");
      String k = ent.getKey();
      xw.field("NAME",k);
      xw.field("TYPE","java.lang.String");
      xw.cdata(ent.getValue());
      xw.end("ATTRIBUTE");
    }
   xw.begin("TYPE");
   xw.field("NAME","PYTHON");
   xw.end("TYPE");
   xw.end("CONFIGURATION");
}






}	// end of class PybaseLaunchConfig




/* end of PybaseLaunchConfig.java */
