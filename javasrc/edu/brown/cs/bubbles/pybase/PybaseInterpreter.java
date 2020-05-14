/********************************************************************************/
/*										*/
/*		PybaseInterpreter.java						*/
/*										*/
/*	Python Bubbles Base class for manaing interpreters			*/
/*										*/
/********************************************************************************/
/*	Copyright 2012 Brown University -- Steven P. Reiss		      */
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
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.InterpreterInfoBuilder;
import edu.brown.cs.bubbles.pybase.symbols.ModulesManagerWithBuild;
import edu.brown.cs.bubbles.pybase.symbols.StringUtils;
import edu.brown.cs.bubbles.pybase.symbols.SystemModulesManager;

import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipFile;


public class PybaseInterpreter implements PybaseConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

//We want to force some libraries to be analyzed as source (e.g.: django)
private static String[] LIBRARIES_TO_IGNORE_AS_FORCED_BUILTINS = new String[]{"django"};

/**
 * For jython, this is the jython.jar
 *
 * For python, this is the path to the python executable
 */
public volatile String executable_or_jar;

/**
 * Folders or zip files: they should be passed to the pythonpath
 */
public final java.util.List<String> lib_set = new ArrayList<String>();

/**
 * __builtin__, os, math, etc for python
 *
 * check sys.builtin_module_names and others that should
 * be forced to use code completion as builtins, such os, math, etc.
 */
private final Set<String> forced_libs = new TreeSet<String>();

/**
 * This is the cache for the builtins (that's the same thing as the forcedLibs, but in a different format,
 * so, whenever the forcedLibs change, this should be changed too).
 */
private String[] builtins_cache;
private Map<String, File> predefined_builtins_cache;

/**
 * module management for the system is always binded to an interpreter (binded in this class)
 *
 * The modules manager is no longer persisted. It is restored from a separate file, because we do
 * not want to keep it in the 'configuration', as a giant Base64 string.
 */
private final SystemModulesManager modules_manager;

/**
 * This callback is only used in tests, to configure the paths that should be chosen after the interpreter is selected.
 */
public static ICallback<Boolean, Tuple<List<String>, List<String>>> configure_paths_callback = null;

/**
 * This is the version for the python interpreter (it is regarded as a String with Major and Minor version
 * for python in the format '2.5' or '2.4'.
 */
private final String version_name;

/**
 * This are the environment variables that should be used when this interpreter is specified.
 * May be null if no env. variables are specified.
 */
private String[] env_variables;

private Properties string_substitution_variables;

private final Set<String> predefined_completions_path = new TreeSet<String>();

/**
 * This is the way that the interpreter should be referred. Can be null (in which case the executable is
 * used as the name)
 */
private String interpreter_name;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseInterpreter(String version, String exe, Collection<String> libs0)
{

   executable_or_jar = exe;
   version_name = version;
   SystemModulesManager modulesmanager = new SystemModulesManager(this);

   this.modules_manager = modulesmanager;
   lib_set.addAll(libs0);
}

PybaseInterpreter(String version, String exe, Collection<String> libs0, Collection<String> dlls)
{
   this(version, exe, libs0);
}

PybaseInterpreter(String version, String exe, List<String> libs0, List<String> dlls, List<String> forced)
{
   this(version, exe, libs0, dlls, forced, null, null);
}

PybaseInterpreter(String version,
	 String exe,
	 List<String> libs0,
	 List<String> dlls,
	 List<String> forced,
	 List<String> envVars,
	 Properties stringSubstitution)
{
   this(version, exe, libs0, dlls);

   for (String s:forced) {
      if (!isForcedLibToIgnore(s)) {
	 forced_libs.add(s);
       }
    }

   if (envVars == null) {
      this.setEnvVariables(null);
    }
   else {
      this.setEnvVariables(envVars.toArray(new String[envVars.size()]));
    }

   this.setStringSubstitutionVariables(stringSubstitution);

   this.clearBuiltinsCache(); //force cache recreation
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public SystemModulesManager getModulesManager() {
   return modules_manager;
}

public String getExecutableOrJar() {
   return executable_or_jar;
}

/**
     * @return the pythonpath to be used (only the folders)
     */
public List<String> getPythonPath() {
   ArrayList<String> ret = new ArrayList<String>();
   ret.addAll(lib_set);
   return ret;
}




/********************************************************************************/
/*										*/
/*	Equality testing							*/
/*										*/
/********************************************************************************/

@Override public boolean equals(Object o) {
   if (!(o instanceof PybaseInterpreter)) {
      return false;
    }

   PybaseInterpreter info = (PybaseInterpreter) o;
   if(info.executable_or_jar.equals(this.executable_or_jar) == false) {
      return false;
    }

   if(info.lib_set.equals(this.lib_set) == false) {
      return false;
    }

   if (info.forced_libs.equals(this.forced_libs) == false) {
      return false;
    }

   if(info.predefined_completions_path.equals(this.predefined_completions_path) == false) {
      return false;
    }

   if(this.env_variables != null) {
      if(info.env_variables == null) {
	 return false;
       }
      //both not null
      if(!Arrays.equals(this.env_variables, info.env_variables)) {
	 return false;
       }
    }
   else{
      //env is null -- the other must be too
      if(info.env_variables != null) {
	 return false;
       }
    }

   if(this.string_substitution_variables != null) {
      if(info.string_substitution_variables == null) {
	 return false;
       }
      //both not null
      if(!this.string_substitution_variables.equals(info.string_substitution_variables)) {
	 return false;
       }
    }
   else{
      //ours is null -- the other must be too
      if(info.string_substitution_variables != null) {
	 return false;
       }
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

/**
 * Format we receive should be:
 *
 * Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2@PYDEV_STRING_SUBST_VARS@PropertiesObjectAsString
 *
 * or
 *
 * Version2.5Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2@PYDEV_STRING_SUBST_VARS@PropertiesObjectAsString
 * (added only when version 2.5 was added, so, if the string does not have it, it is regarded as 2.4)
 *
 * or
 *
 * Name:MyInterpreter:EndName:Version2.5Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2^envVar1|envVar2@PYDEV_STRING_SUBST_VARS@PropertiesObjectAsString
 *
 * Symbols ': @ $'
 */
public static PybaseInterpreter fromString(String received, boolean askUserInOutPath)
{
   if (received.toLowerCase().indexOf("executable") == -1) {
      throw new RuntimeException("Unable to recreate the Interpreter info (Its format changed. Please, re-create your Interpreter information).Contents found:"+received);
    }

   Tuple<String, String> predefCompsPath = StringUtils.splitOnFirst(received, "@PYDEV_PREDEF_COMPS_PATHS@");
   received = predefCompsPath.o1;

   //Note that the new lines are important for the string substitution, so, we must remove it before removing new lines
   Tuple<String, String> stringSubstitutionVarsSplit = StringUtils.splitOnFirst(received, "@PYDEV_STRING_SUBST_VARS@");
   received = stringSubstitutionVarsSplit.o1;

   received = received.replaceAll("\n", "").replaceAll("\r", "");
   String name=null;
   if (received.startsWith("Name:")) {
      int endNameIndex = received.indexOf(":EndName:");
      if (endNameIndex != -1) {
	 name = received.substring("Name:".length(), endNameIndex);
	 received = received.substring(endNameIndex+":EndName:".length());
       }
    }

   Tuple<String, String> envVarsSplit = StringUtils.splitOnFirst(received, '^');
   Tuple<String, String> forcedSplit = StringUtils.splitOnFirst(envVarsSplit.o1, '$');
   Tuple<String, String> libsSplit = StringUtils.splitOnFirst(forcedSplit.o1, '@');
   String exeAndLibs = libsSplit.o1;

   String version = "2.4"; //if not found in the string, the grammar version is regarded as 2.4

   String[] exeAndLibs1 = exeAndLibs.split("\\|");

   String exeAndVersion = exeAndLibs1[0];
   String lowerExeAndVersion = exeAndVersion.toLowerCase();
   if (lowerExeAndVersion.startsWith("version")) {
      int execut = lowerExeAndVersion.indexOf("executable");
      version = exeAndVersion.substring(0,execut).substring(7);
      exeAndVersion = exeAndVersion.substring(7+version.length());
    }
   String executable = exeAndVersion.substring(exeAndVersion.indexOf(":")+1, exeAndVersion.length());

   List<String> selection = new ArrayList<String>();
   List<String> toAsk = new ArrayList<String>();
   for (int i = 1; i < exeAndLibs1.length; i++) { //start at 1 (0 is exe)
      String trimmed = exeAndLibs1[i].trim();
      if (trimmed.length() > 0) {
	 if (trimmed.endsWith("OUT_PATH")) {
	    trimmed = trimmed.substring(0, trimmed.length()-8);
	    if (askUserInOutPath) {
	       toAsk.add(trimmed);
	     }
	    else {
	       //Change 2.0.1: if not asked, it's included by default!
	       selection.add(trimmed);
	     }

	  }
	 else if (trimmed.endsWith("INS_PATH")) {
	    trimmed = trimmed.substring(0, trimmed.length()-8);
	    if (askUserInOutPath) {
	       toAsk.add(trimmed);
	       selection.add(trimmed);
	     }
	    else {
	       selection.add(trimmed);
	     }
	  }
	 else {
	    selection.add(trimmed);
	  }
       }
    }

   if (ModulesManagerWithBuild.IN_TESTS) {
      if (PybaseInterpreter.configure_paths_callback != null) {
	 PybaseInterpreter.configure_paths_callback.call(new Tuple<List<String>, List<String>>(toAsk, selection));
       }
    }

   ArrayList<String> l1 = new ArrayList<String>();
   if (libsSplit.o2.length() > 1) {
      fillList(libsSplit, l1);
    }

   ArrayList<String> l2 = new ArrayList<String>();
   if (forcedSplit.o2.length() > 1) {
      fillList(forcedSplit, l2);
    }

   ArrayList<String> l3 = new ArrayList<String>();
   if (envVarsSplit.o2.length() > 1) {
      fillList(envVarsSplit, l3);
    }
   Properties p4 = null;
   if (stringSubstitutionVarsSplit.o2.length() > 1) {
      p4 = PropertiesHelper.createPropertiesFromString(stringSubstitutionVarsSplit.o2);
    }
   PybaseInterpreter info = new PybaseInterpreter(version, executable, selection, l1, l2, l3, p4);
   if (predefCompsPath.o2.length() > 1) {
      List<String> split = StringUtils.split(predefCompsPath.o2, '|');
      for(String s:split) {
	 s = s.trim();
	 if (s.length() > 0) {
	    info.addPredefinedCompletionsPath(s);
	  }
       }
    }
   info.setName(name);
   return info;
}


private static void fillList(Tuple<String, String> forcedSplit, ArrayList<String> l2) {
   String forcedLibs = forcedSplit.o2;
   for (String trimmed:StringUtils.splitAndRemoveEmptyTrimmed(forcedLibs, '|')) {
      trimmed = trimmed.trim();
      if (trimmed.length() > 0) {
	 l2.add(trimmed);
       }
    }
}

/**
     * @see java.lang.Object#toString()
     */
@Override public String toString() {
   StringBuilder buffer = new StringBuilder();
   if (this.interpreter_name != null) {
      buffer.append("Name:");
      buffer.append(this.interpreter_name);
      buffer.append(":EndName:");
    }
   buffer.append("Version");
   buffer.append(version_name);
   buffer.append("Executable:");
   buffer.append(executable_or_jar);
   for (Iterator<String> iter = lib_set.iterator(); iter.hasNext();) {
      buffer.append("|");
      buffer.append(iter.next().toString());
    }
   buffer.append("@");

   buffer.append("$");
   if (forced_libs.size() > 0) {
      for (Iterator<String> iter = forced_libs.iterator(); iter.hasNext();) {
	 buffer.append("|");
	 buffer.append(iter.next().toString());
       }
    }

   if (this.env_variables != null) {
      buffer.append("^");
      for(String s:env_variables) {
	 buffer.append(s);
	 buffer.append("|");
       }
    }

   if (this.string_substitution_variables != null && this.string_substitution_variables.size() > 0) {
      buffer.append("@PYDEV_STRING_SUBST_VARS@");
      buffer.append(PropertiesHelper.createStringFromProperties(this.string_substitution_variables));
    }

   if (this.predefined_completions_path.size() > 0) {
      buffer.append("@PYDEV_PREDEF_COMPS_PATHS@");
      for(String s:this.predefined_completions_path) {
	 buffer.append("|");
	 buffer.append(s);
       }
    }

   return buffer.toString();
}

/**
     * Adds the compiled libs (dlls)
     */
public void restoreCompiledLibs() {
   //the compiled with the interpreter should be already gotten.

   for(String lib:this.lib_set) {
      addForcedLibsFor(lib);
    }

   //we have it in source, but want to interpret it, source info (ast) does not give us much
   forced_libs.add("os");


   //we also need to add this submodule (because even though it's documented as such, it's not really
   //implemented that way with a separate file -- there's black magic to put it there)
   forced_libs.add("os.path");

   //as it is a set, there is no problem to add it twice
   if (this.version_name.startsWith("2") || this.version_name.startsWith("1")) {
      //don't add it for 3.0 onwards.
      forced_libs.add("__builtin__"); //jython bug: __builtin__ is not added
    }
   forced_libs.add("sys"); //jython bug: sys is not added
   forced_libs.add("email"); //email has some lazy imports that pydev cannot handle through the source
   forced_libs.add("hashlib"); //depending on the Python version, hashlib cannot find md5, so, let's always leave it there.
   forced_libs.add("pytest"); //yeap, pytest does have a structure that's pretty hard to analyze.


   PybaseInterpreterType interpreterType = getInterpreterType();
   switch(interpreterType) {
      case JYTHON:
	 //by default, we don't want to force anything to python.
	 forced_libs.add("StringIO"); //jython bug: StringIO is not added
	 forced_libs.add("re"); //re is very strange in Jython (while it's OK in Python)
	 forced_libs.add("com.ziclix.python.sql"); //bultin to jython but not reported.
	 break;

      case PYTHON:
	 //those are sources, but we want to get runtime info on them.
	 forced_libs.add("OpenGL");
	 forced_libs.add("wxPython");
	 forced_libs.add("wx");
	 forced_libs.add("numpy");
	 forced_libs.add("scipy");
	 forced_libs.add("Image"); //for PIL

	 //these are the builtins -- apparently sys.builtin_module_names is not ok in linux.
	 forced_libs.add("_ast");
	 forced_libs.add("_bisect");
	 forced_libs.add("_bytesio");
	 forced_libs.add("_codecs");
	 forced_libs.add("_codecs_cn");
	 forced_libs.add("_codecs_hk");
	 forced_libs.add("_codecs_iso2022");
	 forced_libs.add("_codecs_jp");
	 forced_libs.add("_codecs_kr");
	 forced_libs.add("_codecs_tw");
	 forced_libs.add("_collections");
	 forced_libs.add("_csv");
	 forced_libs.add("_fileio");
	 forced_libs.add("_functools");
	 forced_libs.add("_heapq");
	 forced_libs.add("_hotshot");
	 forced_libs.add("_json");
	 forced_libs.add("_locale");
	 forced_libs.add("_lsprof");
	 forced_libs.add("_md5");
	 forced_libs.add("_multibytecodec");
	 forced_libs.add("_random");
	 forced_libs.add("_sha");
	 forced_libs.add("_sha256");
	 forced_libs.add("_sha512");
	 forced_libs.add("_sre");
	 forced_libs.add("_struct");
	 forced_libs.add("_subprocess");
	 forced_libs.add("_symtable");
	 forced_libs.add("_warnings");
	 forced_libs.add("_weakref");
	 forced_libs.add("_winreg");
	 forced_libs.add("array");
	 forced_libs.add("audioop");
	 forced_libs.add("binascii");
	 forced_libs.add("cPickle");
	 forced_libs.add("cStringIO");
	 forced_libs.add("cmath");
	 forced_libs.add("datetime");
	 forced_libs.add("errno");
	 forced_libs.add("exceptions");
	 forced_libs.add("future_builtins");
	 forced_libs.add("gc");
	 forced_libs.add("imageop");
	 forced_libs.add("imp");
	 forced_libs.add("itertools");
	 forced_libs.add("marshal");
	 forced_libs.add("math");
	 forced_libs.add("mmap");
	 forced_libs.add("msvcrt");
	 forced_libs.add("nt");
	 forced_libs.add("operator");
	 forced_libs.add("parser");
	 forced_libs.add("signal");
	 forced_libs.add("socket"); //socket seems to have issues on linux
	 forced_libs.add("strop");
	 forced_libs.add("sys");
	 forced_libs.add("thread");
	 forced_libs.add("time");
	 forced_libs.add("xxsubtype");
	 forced_libs.add("zipimport");
	 forced_libs.add("zlib");




	 break;

      case IRONPYTHON:
	 //base namespaces
	 forced_libs.add("System");
	 forced_libs.add("Microsoft");
	 forced_libs.add("clr");


	 //other namespaces (from http://msdn.microsoft.com/en-us/library/ms229335.aspx)
	 forced_libs.add("IEHost.Execute");
	 forced_libs.add("Microsoft.Aspnet.Snapin");
	 forced_libs.add("Microsoft.Build.BuildEngine");
	 forced_libs.add("Microsoft.Build.Conversion");
	 forced_libs.add("Microsoft.Build.Framework");
	 forced_libs.add("Microsoft.Build.Tasks");
	 forced_libs.add("Microsoft.Build.Tasks.Deployment.Bootstrapper");
	 forced_libs.add("Microsoft.Build.Tasks.Deployment.ManifestUtilities");
	 forced_libs.add("Microsoft.Build.Tasks.Hosting");
	 forced_libs.add("Microsoft.Build.Tasks.Windows");
	 forced_libs.add("Microsoft.Build.Utilities");
	 forced_libs.add("Microsoft.CLRAdmin");
	 forced_libs.add("Microsoft.CSharp");
	 forced_libs.add("Microsoft.Data.Entity.Build.Tasks");
	 forced_libs.add("Microsoft.IE");
	 forced_libs.add("Microsoft.Ink");
	 forced_libs.add("Microsoft.Ink.TextInput");
	 forced_libs.add("Microsoft.JScript");
	 forced_libs.add("Microsoft.JScript.Vsa");
	 forced_libs.add("Microsoft.ManagementConsole");
	 forced_libs.add("Microsoft.ManagementConsole.Advanced");
	 forced_libs.add("Microsoft.ManagementConsole.Internal");
	 forced_libs.add("Microsoft.ServiceModel.Channels.Mail");
	 forced_libs.add("Microsoft.ServiceModel.Channels.Mail.ExchangeWebService");
	 forced_libs.add("Microsoft.ServiceModel.Channels.Mail.ExchangeWebService.Exchange2007");
	 forced_libs.add("Microsoft.ServiceModel.Channels.Mail.WindowsMobile");
	 forced_libs.add("Microsoft.SqlServer.Server");
	 forced_libs.add("Microsoft.StylusInput");
	 forced_libs.add("Microsoft.StylusInput.PluginData");
	 forced_libs.add("Microsoft.VisualBasic");
	 forced_libs.add("Microsoft.VisualBasic.ApplicationServices");
	 forced_libs.add("Microsoft.VisualBasic.Compatibility.VB6");
	 forced_libs.add("Microsoft.VisualBasic.CompilerServices");
	 forced_libs.add("Microsoft.VisualBasic.Devices");
	 forced_libs.add("Microsoft.VisualBasic.FileIO");
	 forced_libs.add("Microsoft.VisualBasic.Logging");
	 forced_libs.add("Microsoft.VisualBasic.MyServices");
	 forced_libs.add("Microsoft.VisualBasic.MyServices.Internal");
	 forced_libs.add("Microsoft.VisualBasic.Vsa");
	 forced_libs.add("Microsoft.VisualC");
	 forced_libs.add("Microsoft.VisualC.StlClr");
	 forced_libs.add("Microsoft.VisualC.StlClr.Generic");
	 forced_libs.add("Microsoft.Vsa");
	 forced_libs.add("Microsoft.Vsa.Vb.CodeDOM");
	 forced_libs.add("Microsoft.Win32");
	 forced_libs.add("Microsoft.Win32.SafeHandles");
	 forced_libs.add("Microsoft.Windows.Themes");
	 forced_libs.add("Microsoft.WindowsCE.Forms");
	 forced_libs.add("Microsoft.WindowsMobile.DirectX");
	 forced_libs.add("Microsoft.WindowsMobile.DirectX.Direct3D");
	 forced_libs.add("Microsoft_VsaVb");
	 forced_libs.add("RegCode");
	 forced_libs.add("System");
	 forced_libs.add("System.AddIn");
	 forced_libs.add("System.AddIn.Contract");
	 forced_libs.add("System.AddIn.Contract.Automation");
	 forced_libs.add("System.AddIn.Contract.Collections");
	 forced_libs.add("System.AddIn.Hosting");
	 forced_libs.add("System.AddIn.Pipeline");
	 forced_libs.add("System.CodeDom");
	 forced_libs.add("System.CodeDom.Compiler");
	 forced_libs.add("System.Collections");
	 forced_libs.add("System.Collections.Generic");
	 forced_libs.add("System.Collections.ObjectModel");
	 forced_libs.add("System.Collections.Specialized");
	 forced_libs.add("System.ComponentModel");
	 forced_libs.add("System.ComponentModel.DataAnnotations");
	 forced_libs.add("System.ComponentModel.Design");
	 forced_libs.add("System.ComponentModel.Design.Data");
	 forced_libs.add("System.ComponentModel.Design.Serialization");
	 forced_libs.add("System.Configuration");
	 forced_libs.add("System.Configuration.Assemblies");
	 forced_libs.add("System.Configuration.Install");
	 forced_libs.add("System.Configuration.Internal");
	 forced_libs.add("System.Configuration.Provider");
	 forced_libs.add("System.Data");
	 forced_libs.add("System.Data.Common");
	 forced_libs.add("System.Data.Common.CommandTrees");
	 forced_libs.add("System.Data.Design");
	 forced_libs.add("System.Data.Entity.Design");
	 forced_libs.add("System.Data.Entity.Design.AspNet");
	 forced_libs.add("System.Data.EntityClient");
	 forced_libs.add("System.Data.Linq");
	 forced_libs.add("System.Data.Linq.Mapping");
	 forced_libs.add("System.Data.Linq.SqlClient");
	 forced_libs.add("System.Data.Linq.SqlClient.Implementation");
	 forced_libs.add("System.Data.Mapping");
	 forced_libs.add("System.Data.Metadata.Edm");
	 forced_libs.add("System.Data.Objects");
	 forced_libs.add("System.Data.Objects.DataClasses");
	 forced_libs.add("System.Data.Odbc");
	 forced_libs.add("System.Data.OleDb");
	 forced_libs.add("System.Data.OracleClient");
	 forced_libs.add("System.Data.Services");
	 forced_libs.add("System.Data.Services.Client");
	 forced_libs.add("System.Data.Services.Common");
	 forced_libs.add("System.Data.Services.Design");
	 forced_libs.add("System.Data.Services.Internal");
	 forced_libs.add("System.Data.Sql");
	 forced_libs.add("System.Data.SqlClient");
	 forced_libs.add("System.Data.SqlTypes");
	 forced_libs.add("System.Deployment.Application");
	 forced_libs.add("System.Deployment.Internal");
	 forced_libs.add("System.Diagnostics");
	 forced_libs.add("System.Diagnostics.CodeAnalysis");
	 forced_libs.add("System.Diagnostics.Design");
	 forced_libs.add("System.Diagnostics.Eventing");
	 forced_libs.add("System.Diagnostics.Eventing.Reader");
	 forced_libs.add("System.Diagnostics.PerformanceData");
	 forced_libs.add("System.Diagnostics.SymbolStore");
	 forced_libs.add("System.DirectoryServices");
	 forced_libs.add("System.DirectoryServices.AccountManagement");
	 forced_libs.add("System.DirectoryServices.ActiveDirectory");
	 forced_libs.add("System.DirectoryServices.Protocols");
	 forced_libs.add("System.Drawing");
	 forced_libs.add("System.Drawing.Design");
	 forced_libs.add("System.Drawing.Drawing2D");
	 forced_libs.add("System.Drawing.Imaging");
	 forced_libs.add("System.Drawing.Printing");
	 forced_libs.add("System.Drawing.Text");
	 forced_libs.add("System.EnterpriseServices");
	 forced_libs.add("System.EnterpriseServices.CompensatingResourceManager");
	 forced_libs.add("System.EnterpriseServices.Internal");
	 forced_libs.add("System.Globalization");
	 forced_libs.add("System.IdentityModel.Claims");
	 forced_libs.add("System.IdentityModel.Policy");
	 forced_libs.add("System.IdentityModel.Selectors");
	 forced_libs.add("System.IdentityModel.Tokens");
	 forced_libs.add("System.IO");
	 forced_libs.add("System.IO.Compression");
	 forced_libs.add("System.IO.IsolatedStorage");
	 forced_libs.add("System.IO.Log");
	 forced_libs.add("System.IO.Packaging");
	 forced_libs.add("System.IO.Pipes");
	 forced_libs.add("System.IO.Ports");
	 forced_libs.add("System.Linq");
	 forced_libs.add("System.Linq.Expressions");
	 forced_libs.add("System.Management");
	 forced_libs.add("System.Management.Instrumentation");
	 forced_libs.add("System.Media");
	 forced_libs.add("System.Messaging");
	 forced_libs.add("System.Messaging.Design");
	 forced_libs.add("System.Net");
	 forced_libs.add("System.Net.Cache");
	 forced_libs.add("System.Net.Configuration");
	 forced_libs.add("System.Net.Mail");
	 forced_libs.add("System.Net.Mime");
	 forced_libs.add("System.Net.NetworkInformation");
	 forced_libs.add("System.Net.PeerToPeer");
	 forced_libs.add("System.Net.PeerToPeer.Collaboration");
	 forced_libs.add("System.Net.Security");
	 forced_libs.add("System.Net.Sockets");
	 forced_libs.add("System.Printing");
	 forced_libs.add("System.Printing.IndexedProperties");
	 forced_libs.add("System.Printing.Interop");
	 forced_libs.add("System.Reflection");
	 forced_libs.add("System.Reflection.Emit");
	 forced_libs.add("System.Resources");
	 forced_libs.add("System.Resources.Tools");
	 forced_libs.add("System.Runtime");
	 forced_libs.add("System.Runtime.CompilerServices");
	 forced_libs.add("System.Runtime.ConstrainedExecution");
	 forced_libs.add("System.Runtime.Hosting");
	 forced_libs.add("System.Runtime.InteropServices");
	 forced_libs.add("System.Runtime.InteropServices.ComTypes");
	 forced_libs.add("System.Runtime.InteropServices.CustomMarshalers");
	 forced_libs.add("System.Runtime.InteropServices.Expando");
	 forced_libs.add("System.Runtime.Remoting");
	 forced_libs.add("System.Runtime.Remoting.Activation");
	 forced_libs.add("System.Runtime.Remoting.Channels");
	 forced_libs.add("System.Runtime.Remoting.Channels.Http");
	 forced_libs.add("System.Runtime.Remoting.Channels.Ipc");
	 forced_libs.add("System.Runtime.Remoting.Channels.Tcp");
	 forced_libs.add("System.Runtime.Remoting.Contexts");
	 forced_libs.add("System.Runtime.Remoting.Lifetime");
	 forced_libs.add("System.Runtime.Remoting.Messaging");
	 forced_libs.add("System.Runtime.Remoting.Metadata");
	 forced_libs.add("System.Runtime.Remoting.MetadataServices");
	 forced_libs.add("System.Runtime.Remoting.Proxies");
	 forced_libs.add("System.Runtime.Remoting.Services");
	 forced_libs.add("System.Runtime.Serialization");
	 forced_libs.add("System.Runtime.Serialization.Configuration");
	 forced_libs.add("System.Runtime.Serialization.Formatters");
	 forced_libs.add("System.Runtime.Serialization.Formatters.Binary");
	 forced_libs.add("System.Runtime.Serialization.Formatters.Soap");
	 forced_libs.add("System.Runtime.Serialization.Json");
	 forced_libs.add("System.Runtime.Versioning");
	 forced_libs.add("System.Security");
	 forced_libs.add("System.Security.AccessControl");
	 forced_libs.add("System.Security.Authentication");
	 forced_libs.add("System.Security.Authentication.ExtendedProtection");
	 forced_libs.add("System.Security.Authentication.ExtendedProtection.Configuration");
	 forced_libs.add("System.Security.Cryptography");
	 forced_libs.add("System.Security.Cryptography.Pkcs");
	 forced_libs.add("System.Security.Cryptography.X509Certificates");
	 forced_libs.add("System.Security.Cryptography.Xml");
	 forced_libs.add("System.Security.Permissions");
	 forced_libs.add("System.Security.Policy");
	 forced_libs.add("System.Security.Principal");
	 forced_libs.add("System.Security.RightsManagement");
	 forced_libs.add("System.ServiceModel");
	 forced_libs.add("System.ServiceModel.Activation");
	 forced_libs.add("System.ServiceModel.Activation.Configuration");
	 forced_libs.add("System.ServiceModel.Channels");
	 forced_libs.add("System.ServiceModel.ComIntegration");
	 forced_libs.add("System.ServiceModel.Configuration");
	 forced_libs.add("System.ServiceModel.Description");
	 forced_libs.add("System.ServiceModel.Diagnostics");
	 forced_libs.add("System.ServiceModel.Dispatcher");
	 forced_libs.add("System.ServiceModel.Install.Configuration");
	 forced_libs.add("System.ServiceModel.Internal");
	 forced_libs.add("System.ServiceModel.MsmqIntegration");
	 forced_libs.add("System.ServiceModel.PeerResolvers");
	 forced_libs.add("System.ServiceModel.Persistence");
	 forced_libs.add("System.ServiceModel.Security");
	 forced_libs.add("System.ServiceModel.Security.Tokens");
	 forced_libs.add("System.ServiceModel.Syndication");
	 forced_libs.add("System.ServiceModel.Web");
	 forced_libs.add("System.ServiceProcess");
	 forced_libs.add("System.ServiceProcess.Design");
	 forced_libs.add("System.Speech.AudioFormat");
	 forced_libs.add("System.Speech.Recognition");
	 forced_libs.add("System.Speech.Recognition.SrgsGrammar");
	 forced_libs.add("System.Speech.Synthesis");
	 forced_libs.add("System.Speech.Synthesis.TtsEngine");
	 forced_libs.add("System.Text");
	 forced_libs.add("System.Text.RegularExpressions");
	 forced_libs.add("System.Threading");
	 forced_libs.add("System.Timers");
	 forced_libs.add("System.Transactions");
	 forced_libs.add("System.Transactions.Configuration");
	 forced_libs.add("System.Web");
	 forced_libs.add("System.Web.ApplicationServices");
	 forced_libs.add("System.Web.Caching");
	 forced_libs.add("System.Web.ClientServices");
	 forced_libs.add("System.Web.ClientServices.Providers");
	 forced_libs.add("System.Web.Compilation");
	 forced_libs.add("System.Web.Configuration");
	 forced_libs.add("System.Web.Configuration.Internal");
	 forced_libs.add("System.Web.DynamicData");
	 forced_libs.add("System.Web.DynamicData.Design");
	 forced_libs.add("System.Web.DynamicData.ModelProviders");
	 forced_libs.add("System.Web.Handlers");
	 forced_libs.add("System.Web.Hosting");
	 forced_libs.add("System.Web.Mail");
	 forced_libs.add("System.Web.Management");
	 forced_libs.add("System.Web.Mobile");
	 forced_libs.add("System.Web.Profile");
	 forced_libs.add("System.Web.Query.Dynamic");
	 forced_libs.add("System.Web.RegularExpressions");
	 forced_libs.add("System.Web.Routing");
	 forced_libs.add("System.Web.Script.Serialization");
	 forced_libs.add("System.Web.Script.Services");
	 forced_libs.add("System.Web.Security");
	 forced_libs.add("System.Web.Services");
	 forced_libs.add("System.Web.Services.Configuration");
	 forced_libs.add("System.Web.Services.Description");
	 forced_libs.add("System.Web.Services.Discovery");
	 forced_libs.add("System.Web.Services.Protocols");
	 forced_libs.add("System.Web.SessionState");
	 forced_libs.add("System.Web.UI");
	 forced_libs.add("System.Web.UI.Adapters");
	 forced_libs.add("System.Web.UI.Design");
	 forced_libs.add("System.Web.UI.Design.MobileControls");
	 forced_libs.add("System.Web.UI.Design.MobileControls.Converters");
	 forced_libs.add("System.Web.UI.Design.WebControls");
	 forced_libs.add("System.Web.UI.Design.WebControls.WebParts");
	 forced_libs.add("System.Web.UI.MobileControls");
	 forced_libs.add("System.Web.UI.MobileControls.Adapters");
	 forced_libs.add("System.Web.UI.MobileControls.Adapters.XhtmlAdapters");
	 forced_libs.add("System.Web.UI.WebControls");
	 forced_libs.add("System.Web.UI.WebControls.Adapters");
	 forced_libs.add("System.Web.UI.WebControls.WebParts");
	 forced_libs.add("System.Web.Util");
	 forced_libs.add("System.Windows");
	 forced_libs.add("System.Windows.Annotations");
	 forced_libs.add("System.Windows.Annotations.Storage");
	 forced_libs.add("System.Windows.Automation");
	 forced_libs.add("System.Windows.Automation.Peers");
	 forced_libs.add("System.Windows.Automation.Provider");
	 forced_libs.add("System.Windows.Automation.Text");
	 forced_libs.add("System.Windows.Controls");
	 forced_libs.add("System.Windows.Controls.Primitives");
	 forced_libs.add("System.Windows.Converters");
	 forced_libs.add("System.Windows.Data");
	 forced_libs.add("System.Windows.Documents");
	 forced_libs.add("System.Windows.Documents.Serialization");
	 forced_libs.add("System.Windows.Forms");
	 forced_libs.add("System.Windows.Forms.ComponentModel.Com2Interop");
	 forced_libs.add("System.Windows.Forms.Design");
	 forced_libs.add("System.Windows.Forms.Design.Behavior");
	 forced_libs.add("System.Windows.Forms.Integration");
	 forced_libs.add("System.Windows.Forms.Layout");
	 forced_libs.add("System.Windows.Forms.PropertyGridInternal");
	 forced_libs.add("System.Windows.Forms.VisualStyles");
	 forced_libs.add("System.Windows.Ink");
	 forced_libs.add("System.Windows.Ink.AnalysisCore");
	 forced_libs.add("System.Windows.Input");
	 forced_libs.add("System.Windows.Input.StylusPlugIns");
	 forced_libs.add("System.Windows.Interop");
	 forced_libs.add("System.Windows.Markup");
	 forced_libs.add("System.Windows.Markup.Localizer");
	 forced_libs.add("System.Windows.Markup.Primitives");
	 forced_libs.add("System.Windows.Media");
	 forced_libs.add("System.Windows.Media.Animation");
	 forced_libs.add("System.Windows.Media.Converters");
	 forced_libs.add("System.Windows.Media.Effects");
	 forced_libs.add("System.Windows.Media.Imaging");
	 forced_libs.add("System.Windows.Media.Media3D");
	 forced_libs.add("System.Windows.Media.Media3D.Converters");
	 forced_libs.add("System.Windows.Media.TextFormatting");
	 forced_libs.add("System.Windows.Navigation");
	 forced_libs.add("System.Windows.Resources");
	 forced_libs.add("System.Windows.Shapes");
	 forced_libs.add("System.Windows.Threading");
	 forced_libs.add("System.Windows.Xps");
	 forced_libs.add("System.Windows.Xps.Packaging");
	 forced_libs.add("System.Windows.Xps.Serialization");
	 forced_libs.add("System.Workflow.Activities");
	 forced_libs.add("System.Workflow.Activities.Configuration");
	 forced_libs.add("System.Workflow.Activities.Rules");
	 forced_libs.add("System.Workflow.Activities.Rules.Design");
	 forced_libs.add("System.Workflow.ComponentModel");
	 forced_libs.add("System.Workflow.ComponentModel.Compiler");
	 forced_libs.add("System.Workflow.ComponentModel.Design");
	 forced_libs.add("System.Workflow.ComponentModel.Serialization");
	 forced_libs.add("System.Workflow.Runtime");
	 forced_libs.add("System.Workflow.Runtime.Configuration");
	 forced_libs.add("System.Workflow.Runtime.DebugEngine");
	 forced_libs.add("System.Workflow.Runtime.Hosting");
	 forced_libs.add("System.Workflow.Runtime.Tracking");
	 forced_libs.add("System.Xml");
	 forced_libs.add("System.Xml.Linq");
	 forced_libs.add("System.Xml.Schema");
	 forced_libs.add("System.Xml.Serialization");
	 forced_libs.add("System.Xml.Serialization.Advanced");
	 forced_libs.add("System.Xml.Serialization.Configuration");
	 forced_libs.add("System.Xml.XPath");
	 forced_libs.add("System.Xml.Xsl");
	 forced_libs.add("System.Xml.Xsl.Runtime");
	 forced_libs.add("UIAutomationClientsideProviders");


	 break;

      default:
	 throw new RuntimeException("Don't know how to treat: "+interpreterType);
    }
   this.clearBuiltinsCache(); //force cache recreation
}


private void addForcedLibsFor(String lib) {
   //For now only adds "werkzeug", but this is meant as an extension place.
   File file = new File(lib);
   if (file.exists()) {
      if (file.isDirectory()) {
	 //check as dir (if it has a werkzeug folder)
	 File werkzeug = new File(file, "werkzeug");
	 if (werkzeug.isDirectory()) {
	    forced_libs.add("werkzeug");
	  }
       }
      else {
	 //check as zip (if it has a werkzeug entry -- note that we have to check the __init__
	 //because an entry just with the folder doesn't really exist)
	 try {
	    ZipFile zipFile = new ZipFile(file);
	    if (zipFile.getEntry("werkzeug/__init__.py") != null) {
	       forced_libs.add("werkzeug");
	     }
            zipFile.close();
	  } 
         catch (Exception e) {
            //ignore (not zip file)
          }
       }
    }
}



private void clearBuiltinsCache() {
   this.builtins_cache = null; //force cache recreation
   this.predefined_builtins_cache = null;
}

/**
 * Restores the path given non-standard libraries
 * @param path
 */
private void restorePythonpath(String path)
{
   //no managers involved here...
   getModulesManager().changePythonPath(path, null);
}

/**
 * Restores the path with the discovered libs
 * @param path
 */
public void restorePythonpath()
{
   StringBuilder buffer = new StringBuilder();
   for (Iterator<String> iter = lib_set.iterator(); iter.hasNext();) {
      String folder = iter.next();
      buffer.append(folder);
      buffer.append("|");
    }
   restorePythonpath(buffer.toString());
}


public PybaseInterpreterType getInterpreterType() {
   if (isJythonExecutable(executable_or_jar)) {
      return PybaseInterpreterType.JYTHON;

    }
   else if (isIronpythonExecutable(executable_or_jar)) {
	return PybaseInterpreterType.IRONPYTHON;
      }
   //neither one: it's python.
   return PybaseInterpreterType.PYTHON;
}


/**
 * @param executable the executable we want to know about
 * @return if the executable is the jython jar.
 */
public static boolean isJythonExecutable(String executable) {
   if (executable.endsWith("\"")) {
      return executable.endsWith(".jar\"");
    }
   return executable.endsWith(".jar");
}

/**
 * @param executable the executable we want to know about
 * @return if the executable is the ironpython executable.
 */
public static boolean isIronpythonExecutable(String executable) {
   File file = new File(executable);
   return file.getName().startsWith("ipy");
}

public static String getExeAsFileSystemValidPath(String executableOrJar) {
   return "v1_"+StringUtils.md5(executableOrJar);

}
public String getExeAsFileSystemValidPath() {
   return getExeAsFileSystemValidPath(executable_or_jar);
}

public String getVersion() {
   return version_name;
}

public int getGrammarVersion() {
   return PybaseNature.getGrammarVersionFromStr(version_name);
}



//START: Things related to the builtins (forcedLibs) ---------------------------------------------------------------
public String[] getBuiltins() {
   if (this.builtins_cache == null) {
      Set<String> set = new HashSet<String>(forced_libs);
      this.builtins_cache = set.toArray(new String[0]);
    }
   return this.builtins_cache;
}

public void addForcedLib(String forcedLib) {
   if (isForcedLibToIgnore(forcedLib)) {
      return;
    }
   this.forced_libs.add(forcedLib);
   this.clearBuiltinsCache();
}

/**
     * @return true if the passed forced lib should not be added to the forced builtins.
     */
private boolean isForcedLibToIgnore(String forcedLib) {
   if (forcedLib == null) {
      return true;
    }
   //We want django to be always analyzed as source
   for(String s:LIBRARIES_TO_IGNORE_AS_FORCED_BUILTINS) {
      if (forcedLib.equals(s) || forcedLib.startsWith(s+".")) {
	 return true;
       }
    }
   return false;
}

public void removeForcedLib(String forcedLib) {
   this.forced_libs.remove(forcedLib);
   this.clearBuiltinsCache();
}

public Iterator<String> forcedLibsIterator() {
   return forced_libs.iterator();
}
//END: Things related to the builtins (forcedLibs) -----------------------------------------------------------------


/**
 * Sets the environment variables to be kept in the interpreter info.
 *
 * Some notes:
 * - Will remove (and warn) about any PYTHONPATH env. var.
 * - Will keep the env. variables sorted internally.
 */
public void setEnvVariables(String[] env) {

   if (env != null) {
      ArrayList<String> lst = new ArrayList<String>();
      //We must make sure that the PYTHONPATH is not in the env. variables.
      for(String s: env) {
	 Tuple<String, String> sp = StringUtils.splitOnFirst(s, '=');
	 if (sp.o1.length() != 0 && sp.o2.length() != 0) {
	    if (!checkIfPythonPathEnvVarAndWarnIfIs(sp.o1)) {
	       lst.add(s);
	     }
	  }
       }
      Collections.sort(lst);
      env = lst.toArray(new String[lst.size()]);
    }

   if (env != null && env.length == 0) {
      env = null;
    }

   this.env_variables = env;
}

public String[] getEnvVariables() {
   return this.env_variables;
}

public String[] updateEnv(String[] env) {
   return updateEnv(env, null);
}

public String[] updateEnv(String[] env, Set<String> keysThatShouldNotBeUpdated) {
   if (this.env_variables == null || this.env_variables.length == 0) {
      return env; //nothing to change
    }
   //Ok, it's not null...

   if (env == null || env.length == 0) {
      //if the passed was null, just repass the ones contained here
      return this.env_variables;
    }

   //both not null, let's merge them
   HashMap<String, String> hashMap = new HashMap<String, String>();

   fillMapWithEnv(env, hashMap);
   fillMapWithEnv(env_variables, hashMap, keysThatShouldNotBeUpdated); //will override the keys already there unless they're in keysThatShouldNotBeUpdated

   String[] ret = createEnvWithMap(hashMap);

   return ret;
}

public static String[] createEnvWithMap(Map<String, String> hashMap) {
   Set<Entry<String, String>> entrySet = hashMap.entrySet();
   String[] ret = new String[entrySet.size()];
   int i=0;
   for (Entry<String, String> entry : entrySet) {
      ret[i] = entry.getKey()+"="+entry.getValue();
      i++;
    }
   return ret;
}

public static void fillMapWithEnv(String[] env, HashMap<String, String> hashMap) {
   fillMapWithEnv(env, hashMap, null);
}

public static void fillMapWithEnv(String[] env, HashMap<String, String> hashMap, Set<String> keysThatShouldNotBeUpdated) {
   if (keysThatShouldNotBeUpdated == null) {
      keysThatShouldNotBeUpdated = new HashSet<String>();
    }

   for(String s: env) {
      Tuple<String, String> sp = StringUtils.splitOnFirst(s, '=');
      if (sp.o1.length() != 0 && sp.o2.length() != 0 && !keysThatShouldNotBeUpdated.contains(sp.o1)) {
	 hashMap.put(sp.o1, sp.o2);
       }
    }
}


/**
 * This function will remove any PYTHONPATH entry from the given map (considering the case based on the system)
 * and will give a warning to the user if that's actually done.
 */
public static void removePythonPathFromEnvMapWithWarning(HashMap<String, String> map) {
   if (map == null) {
      return;
    }

   for(Iterator<Map.Entry<String, String>> it=map.entrySet().iterator();it.hasNext();) {
      Map.Entry<String, String> next = it.next();

      String key = next.getKey();

      if (checkIfPythonPathEnvVarAndWarnIfIs(key)) {
	 it.remove();
       }
    }
}

/**
 * Warns if the passed key is the PYTHONPATH env. var.
 *
 * @param key the key to check.
 * @return true if the passed key is a PYTHONPATH env. var. (considers platform)
 */
public static boolean checkIfPythonPathEnvVarAndWarnIfIs(String key) {
   boolean isPythonPath = false;
   boolean win32 = PybaseNature.isWindowsPlatform();
   if (win32) {
      key = key.toUpperCase();
    }
   final String keyPlatformDependent = key;
   if (keyPlatformDependent.equals("PYTHONPATH") || keyPlatformDependent.equals("CLASSPATH") || keyPlatformDependent.equals("JYTHONPATH") || keyPlatformDependent.equals("IRONPYTHONPATH")) {
      isPythonPath = true;
    }
   return isPythonPath;
}


/**
 * @return a new interpreter info that's a copy of the current interpreter info.
 */
public PybaseInterpreter makeCopy() {
   return fromString(toString(), false);
}

public void setName(String name) {
   this.interpreter_name = name;
}

public String getName() {
   if (this.interpreter_name != null) {
      return this.interpreter_name;
    }
   return this.executable_or_jar;
}

public String getNameForUI() {
   if (this.interpreter_name != null) {
      return this.interpreter_name+"  ("+this.executable_or_jar+")";
    }
   else {
      return this.executable_or_jar;
    }
}

public boolean matchNameBackwardCompatible(String interpreter) {
   if (this.interpreter_name != null) {
      if (interpreter.equals(this.interpreter_name)) {
	 return true;
       }
    }
   if (PybaseNature.isWindowsPlatform()) {
      return interpreter.equalsIgnoreCase(executable_or_jar);
    }
   return interpreter.equals(executable_or_jar);
}

public void setStringSubstitutionVariables(Properties stringSubstitutionOriginal) {
   if (stringSubstitutionOriginal == null) {
      this.string_substitution_variables = null;
    }
   else{
      this.string_substitution_variables = stringSubstitutionOriginal;
    }
}

public Properties getStringSubstitutionVariables() {
   return this.string_substitution_variables;
}

public void addPredefinedCompletionsPath(String path) {
   this.predefined_completions_path.add(path);
   this.clearBuiltinsCache();
}

public List<String> getPredefinedCompletionsPath() {
   return new ArrayList<String>(predefined_completions_path); //Return a copy.
}

/**
 * May return null if it doesn't exist.
 * @return the file that matches the passed module name with the predefined builtins.
 */
public File getPredefinedModule(String moduleName) {
   if (this.predefined_builtins_cache == null) {
      this.predefined_builtins_cache = new HashMap<>();
      for(String s:this.getPredefinedCompletionsPath()) {
	 File f = new File(s);
	 if (f.exists()) {
	    File[] predefs = f.listFiles(new FilenameFilter() {
					    //Only accept names ending with .pypredef in the passed dirs
					    @Override public boolean accept(File dir, String name) {
					       return name.endsWith(".pypredef");
					     }
					  });

	    for (File file : predefs) {
	       String n = file.getName();
	       String modName = n.substring(0, n.length()-(".pypredef".length()));
	       this.predefined_builtins_cache.put(modName, file);
	     }
	  }
       }
    }

   return this.predefined_builtins_cache.get(moduleName);
}

public void removePredefinedCompletionPath(String item) {
   this.predefined_completions_path.remove(item);
   this.clearBuiltinsCache();
}


private InterpreterInfoBuilder info_builder;
private final Object builder_lock = new Object();

private volatile boolean load_finished = true;


/**
 * Building so that the interpreter info is kept up to date.
 */
public void startBuilding() {
   synchronized (builder_lock) {
      if (info_builder == null) {
	 InterpreterInfoBuilder builder = new InterpreterInfoBuilder();
	 builder.setInfo(this);
	 info_builder = builder;
       }
    }
}



public void stopBuilding() {
   synchronized (builder_lock) {
      if (info_builder != null) {
	 info_builder.dispose();
	 info_builder = null;
       }
    }
}

public void setLoadFinished(boolean b) {
   this.load_finished = b;
}

public boolean getLoadFinished() {
   return this.load_finished;
}



}	// end of class PybaseInterpreter




/* end of PybaseInterpreter.java */
