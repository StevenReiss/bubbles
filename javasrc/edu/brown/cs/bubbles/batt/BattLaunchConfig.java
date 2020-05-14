/********************************************************************************/
/*										*/
/*		BattLaunchConfig.java						*/
/*										*/
/*	Bubble Automated Testing Tool test launch configuration data		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.batt;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


class BattLaunchConfig implements BattConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String launch_name;
private String vm_args;
private String prog_args;
private String main_type;
private String project_name;
private boolean use_env;
private List<String> class_path;
private boolean default_cp;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattLaunchConfig(Element xml)
{
   launch_name = IvyXml.getAttrString(xml,"NAME");
   use_env = false;
   default_cp = false;
   class_path = null;
   main_type = null;
   project_name = null;
   vm_args = null;
   prog_args = null;

   for (Element ae : IvyXml.children(xml,"ATTRIBUTE")) {
      String nm = IvyXml.getAttrString(ae,"NAME");
      String vl = IvyXml.getText(ae);

      if (nm.equals("org.eclipse.debug.core.appendEnvironmentVariables")) {
	 use_env = Boolean.valueOf(vl);
       }
      else if (nm.equals("org.eclipse.jdt.launching.CLASSPATH")) {
	 if (class_path == null) class_path = new ArrayList<String>();
	 class_path.add(vl);
       }
      else if (nm.equals("org.eclipse.jdt.launching.DEFAULT_CLASSPATH")) {
	 default_cp = Boolean.valueOf(vl);
       }
      else if (nm.equals("org.eclipse.jdt.launching.MAIN_TYPE")) {
	 main_type = vl;
       }
      else if (nm.equals("org.eclipse.jdt.launching.PROJECT_ATTR")) {
	 project_name = vl;
       }
      else if (nm.equals("org.eclipse.jdt.launching.VM_ARGUMENTS")) {
	 vm_args = vl;
       }
      else if (nm.equals("org.eclipse.jdt.launching.PROGRAM_ARGUMENTS")) {
	 prog_args = vl;
       }
    }

   if (class_path == null) default_cp = true;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()		{ return launch_name; }
String getProject()		{ return project_name; }
String getVmArgs()		{ return vm_args; }
String getStartClass()		{ return main_type; }
String getProgramArgs()		{ return prog_args; }
boolean useEnvironment()	{ return use_env; }

String getClassPath()
{
   if (default_cp || class_path == null) return null;

   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < class_path.size(); ++i) {
      String s = class_path.get(i);
      if (i > 0) buf.append(File.pathSeparator);
      buf.append(s);
    }

   return buf.toString();
}




}	// end of class BattLaunchConfig




/* end of BattLaunchConfig.java */
