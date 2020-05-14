/********************************************************************************/
/*										*/
/*		PybasePreferences.java						*/
/*										*/
/*	Python Bubbles Base preference holder					*/
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 11, 2004
 *
 * @author Fabio Zadrozny
 * @author atotic
 */


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class PybasePreferences implements PybaseConstants {





/********************************************************************************/
/*										*/
/*	Default Preference Definitions						*/
/*										*/
/********************************************************************************/

private final static Map<ErrorType,ErrorSeverity> DEFAULT_SEVERITY_MAP;
private final static Map<ErrorType,String> DEFAULT_IGNORE_ANNOTATION;

private final static Set<String> DEFAULT_IGNORE_UNUSED;
private final static Set<String> DEFAULT_GLOBAL_NAMES;
private final static Set<String> DEFAULT_IGNORE_MODULE;

static {
   DEFAULT_SEVERITY_MAP = new HashMap<ErrorType,ErrorSeverity>();
   DEFAULT_SEVERITY_MAP.put(ErrorType.UNUSED_IMPORT,ErrorSeverity.WARNING);
   DEFAULT_SEVERITY_MAP.put(ErrorType.UNUSED_VARIABLE,ErrorSeverity.WARNING);
   DEFAULT_SEVERITY_MAP.put(ErrorType.UNDEFINED_VARIABLE,ErrorSeverity.ERROR);
   DEFAULT_SEVERITY_MAP.put(ErrorType.DUPLICATED_SIGNATURE,ErrorSeverity.ERROR);
   DEFAULT_SEVERITY_MAP.put(ErrorType.REIMPORT,ErrorSeverity.WARNING);
   DEFAULT_SEVERITY_MAP.put(ErrorType.UNRESOLVED_IMPORT,ErrorSeverity.ERROR);
   DEFAULT_SEVERITY_MAP.put(ErrorType.NO_SELF,ErrorSeverity.ERROR);
   DEFAULT_SEVERITY_MAP.put(ErrorType.UNUSED_WILD_IMPORT,ErrorSeverity.WARNING);
   DEFAULT_SEVERITY_MAP.put(ErrorType.UNDEFINED_IMPORT_VARIABLE,ErrorSeverity.ERROR);
   DEFAULT_SEVERITY_MAP.put(ErrorType.UNUSED_PARAMETER,ErrorSeverity.INFO);
   DEFAULT_SEVERITY_MAP.put(ErrorType.NO_EFFECT_STMT,ErrorSeverity.WARNING);
   DEFAULT_SEVERITY_MAP.put(ErrorType.INDENTATION_PROBLEM,ErrorSeverity.WARNING);
   DEFAULT_SEVERITY_MAP.put(ErrorType.ASSIGNMENT_TO_BUILT_IN_SYMBOL,ErrorSeverity.WARNING);
   DEFAULT_SEVERITY_MAP.put(ErrorType.SYNTAX_ERROR,ErrorSeverity.ERROR);

   DEFAULT_IGNORE_ANNOTATION = new HashMap<ErrorType,String>();
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNUSED_IMPORT, "@UnusedImport");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNUSED_VARIABLE, "@UnusedVariable");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNUSED_PARAMETER, "@UnusedVariable");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNDEFINED_VARIABLE, "@UndefinedVariable");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.DUPLICATED_SIGNATURE, "@DuplicatedSignature");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.REIMPORT, "@Reimport");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNRESOLVED_IMPORT, "UnresolvedImport");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.NO_SELF, "@NoSelf");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNUSED_WILD_IMPORT, "@UnusedWildImport");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNDEFINED_IMPORT_VARIABLE, "@UndefinedVariable");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.UNDEFINED_VARIABLE, "@UndefinedVariable");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.NO_EFFECT_STMT, "@NoEffect");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.INDENTATION_PROBLEM, "@IndentOk");
   DEFAULT_IGNORE_ANNOTATION.put(ErrorType.ASSIGNMENT_TO_BUILT_IN_SYMBOL, "@ReservedAssignment");

   DEFAULT_IGNORE_UNUSED = new HashSet<String>();
   DEFAULT_IGNORE_UNUSED.add(PybaseUtil.convertWildcardToRegex("__init__"));
   DEFAULT_IGNORE_UNUSED.add(PybaseUtil.convertWildcardToRegex("QT"));
   DEFAULT_IGNORE_UNUSED.add(PybaseUtil.convertWildcardToRegex("_"));

   DEFAULT_IGNORE_MODULE = new HashSet<String>();
   DEFAULT_IGNORE_MODULE.add(PybaseUtil.convertWildcardToRegex("__init__"));
   DEFAULT_IGNORE_MODULE.add(PybaseUtil.convertWildcardToRegex("*QT"));

   DEFAULT_GLOBAL_NAMES = new HashSet<String>();
   DEFAULT_GLOBAL_NAMES.add(PybaseUtil.convertWildcardToRegex("_"));
   DEFAULT_GLOBAL_NAMES.add(PybaseUtil.convertWildcardToRegex("tr"));
};



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<ErrorType,ErrorSeverity> severity_map;
private Map<ErrorType,String> ignore_annotation;

private Set<String> ignore_unused_import;
private Set<String> names_considered_global;
private Set<String> module_names_ignored;

private Map<String,String> pref_props;
private File base_file;




/********************************************************************************/
/*										*/
/*	Coinstructors								*/
/*										*/
/********************************************************************************/

PybasePreferences()
{
   severity_map = new HashMap<ErrorType,ErrorSeverity>(DEFAULT_SEVERITY_MAP);
   ignore_annotation = new HashMap<ErrorType,String>(DEFAULT_IGNORE_ANNOTATION);
   ignore_unused_import = new HashSet<String>(DEFAULT_IGNORE_UNUSED);
   module_names_ignored = new HashSet<String>(DEFAULT_IGNORE_MODULE);
   names_considered_global = new HashSet<String>(DEFAULT_GLOBAL_NAMES);
   pref_props = new HashMap<String,String>();
   base_file = null;
}



PybasePreferences(PybasePreferences par)
{
   severity_map = new HashMap<ErrorType,ErrorSeverity>(par.severity_map);
   ignore_annotation = new HashMap<ErrorType,String>(par.ignore_annotation);
   ignore_unused_import = new HashSet<String>(par.ignore_unused_import);
   module_names_ignored = new HashSet<String>(par.module_names_ignored);
   names_considered_global = new HashSet<String>(par.names_considered_global);
   pref_props = new HashMap<String,String>(par.pref_props);
   base_file = null;
}



PybasePreferences(File file)
{
   this();
   base_file = file;
   Element e = IvyXml.loadXmlFromFile(file);
   if (e != null) loadXml(e);
}



/********************************************************************************/
/*										*/
/*	Error severity and annotation methods					*/
/*										*/
/********************************************************************************/

public ErrorSeverity getSeverityForType(ErrorType type) {
   ErrorSeverity iv = severity_map.get(type);
   if (iv != null) return iv;
   PybaseMain.logE("Unknown error type for severity: " + type);
   return ErrorSeverity.ERROR;
}



public Set<String> getNamesIgnoredByUnusedVariable() {
   return ignore_unused_import;
}

public Set<String> getTokensAlwaysInGlobals() {
   return names_considered_global;
}


public Set<String> getModuleNamePatternsToBeIgnored() {
   return module_names_ignored;
}



public String getRequiredMessageToIgnore(ErrorType type) {
   return ignore_annotation.get(type);
}




/********************************************************************************/
/*										*/
/*	General property methods						*/
/*										*/
/********************************************************************************/

public String getProperty(String prop,String dflt)
{
   if (pref_props.containsKey(prop)) return pref_props.get(prop);

   return dflt;
}



public void setProperty(String prop,String value)
{
   if (prop.startsWith("ErrorType.")) {
      String ets = prop.substring(10);
      value = value.toUpperCase();
      try {
         ErrorType et = Enum.valueOf(ErrorType.class,ets);
         ErrorSeverity es = Enum.valueOf(ErrorSeverity.class,value);
         severity_map.put(et,es);
       }
      catch (IllegalArgumentException e) { }
    }
   else {
      pref_props.put(prop,value);
    }
}




/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw,boolean user)
{
   xw.begin("PREFERENCES");

   for (Map.Entry<ErrorType,ErrorSeverity> ent : severity_map.entrySet()) {
      xw.begin("SEVERITY");
      xw.field("TYPE",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("SEVERITY");
    }

   for (Map.Entry<String,String> ent : pref_props.entrySet()) {
      if (user && ent.getKey().equals("INTERPRETER_PATH_NEW")) continue;
      xw.begin("PROP");
      xw.field("KEY",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("PROP");
    }

   xw.end("PREFERENCES");
}



void loadXml(Element xml)
{
   Element prefs;
   if (IvyXml.isElement(xml,"PREFERENCES")) prefs = xml;
   else prefs = IvyXml.getChild(xml,"PREFERENCES");

   for (Element sev : IvyXml.children(prefs,"SEVERITY")) {
      ErrorType et = IvyXml.getAttrEnum(sev,"TYPE",ErrorType.SYNTAX_ERROR);
      ErrorSeverity ev = severity_map.get(et);
      ev = IvyXml.getAttrEnum(sev,"VALUE",ev);
      severity_map.put(et,ev);
    }

   for (Element pel : IvyXml.children(prefs,"PROP")) {
      String k = IvyXml.getAttrString(pel,"KEY");
      String v = IvyXml.getAttrString(pel,"VALUE");
      pref_props.put(k,v);
    }
}



void dumpPreferences(IvyXmlWriter xw)
{
   xw.begin("PREFERENCES");

   for (Map.Entry<ErrorType,ErrorSeverity> ent : severity_map.entrySet()) {
      xw.begin("SEVERITY");
      xw.field("TYPE",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("SEVERITY");
    }

   for (Map.Entry<String,String> ent : pref_props.entrySet()) {
      xw.begin("PROP");
      xw.field("KEY",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("PROP");
    }

   xw.end("PREFERENCES");
}



public void flush()
{
   if (base_file != null) {
      try {
	 IvyXmlWriter xw = new IvyXmlWriter(base_file);
	 dumpPreferences(xw);
	 xw.close();
       }
      catch (IOException e) {
	 PybaseMain.logE("Problem saving preferences file " + base_file,e);
       }
    }
}



}	// end of class PybasePreferences





































