/********************************************************************************/
/*                                                                              */
/*              BstyleChecker.java                                              */
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



package edu.brown.cs.bubbles.bstyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.ThreadModeSettings;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;
import com.puppycrawl.tools.checkstyle.api.RootModule;
import com.puppycrawl.tools.checkstyle.api.Violation;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.ivy.file.IvyLog;

class BstyleChecker implements BstyleConstants, MessageDispatcher, AuditListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,Configuration>       project_configs;
private Configuration                   default_config;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleChecker()
{
   project_configs = new HashMap<>();
   default_config = null;
   
   BoardProperties bp = BoardProperties.getProperties("Bstyle");
   
   for (String nm : bp.stringPropertyNames()) {
      if (nm.startsWith("Bstyle.config.file.")) {
         int idx = nm.lastIndexOf(".");
         String proj = nm.substring(idx+1);
         Configuration cfg = getConfiguration(proj,bp.getProperty(nm));
         project_configs.put(proj,cfg);
       }
      else if (nm.equals("Bstyle.config.file")) {
         default_config = getConfiguration(null,bp.getProperty(nm));
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*     Processing methods                                                       */
/*                                                                              */
/********************************************************************************/

void processProject(String proj,List<BstyleFile> files)
{
   Configuration cfg = project_configs.get(proj);
   if (cfg == null) cfg = default_config;
   if (cfg == null) return;
   
   ClassLoader mcl = Checker.class.getClassLoader();
   ModuleFactory fac = new PackageObjectFactory(Checker.class.getPackage().getName(),mcl);
   RootModule root = null;
   try {
      root = new BstyleCheckRunner();
      root.setModuleClassLoader(mcl);
      root.configure(cfg);
      root.addListener(this);
    }
   catch (CheckstyleException e) {
      IvyLog.logE("BSTYLE","Problem processing files",e);
      return;
    }
   
   List<File> base = new ArrayList<>();
   for (BstyleFile bf : files) {
      base.add(bf.getFile()); 
    }
   try {
      root.process(base);
    }
   catch (CheckstyleException e) {
      IvyLog.logE("BSTYLE","Problem processing files",e);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Configuration methods                                                   */
/*                                                                              */
/********************************************************************************/

private Configuration getConfiguration(String proj,String configpath)
{
   Properties props = new Properties();
   props.put("charset","UTF_8");
   String path = null;
   try {
      File tempf = File.createTempFile("bstyle",".tmp");
      tempf.deleteOnExit();
      path = tempf.getPath();
    }
   catch (IOException e) {
      IvyLog.logE("Problem creating temp file",e);
    }
   if (path != null) props.put("cacheFile",path);
   BoardProperties bp = BoardProperties.getProperties("Bstyle");
   String dfltprops = null;
   String xprops = null;
   for (String nm : bp.stringPropertyNames()) {
      if (nm.equals("Bstyle.config.properties." + proj)) {
         xprops = bp.getProperty(nm);
       }
      else if (nm.equals("Bstyle.config.properties")) {
         dfltprops = bp.getProperty(nm);
       }
    }
   if (xprops != null) xprops = dfltprops;
   if (xprops != null) {
      StringTokenizer tok = new StringTokenizer(xprops);
      while (tok.hasMoreTokens()) {
         String t = tok.nextToken();
         int idx = t.indexOf("=");
         if (idx > 0) {
            String key = t.substring(0,idx);
            String v = t.substring(idx+1);
            props.put(key,v);
          }
         else {
            props.put(t,"true");
          }
       }
    }
   PropertiesExpander res = new PropertiesExpander(props);
   ThreadModeSettings mode = new ThreadModeSettings(1,1);
  
   try {
      Configuration cfg = ConfigurationLoader.loadConfiguration(
            configpath, res, ConfigurationLoader.IgnoredModulesOptions.OMIT,
            mode);
      return cfg;
    }
   catch (CheckstyleException e) {
      IvyLog.logE("BSTYLE","Problem setting up configuration",e);
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Handle error returns                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void fireFileStarted(String file)
{
   IvyLog.logD("BSTYLE","File started " + file);
}



@Override public void fireErrors(String filename,SortedSet<Violation> errors)
{
   for (Violation v : errors) {
      AuditEvent evt = new AuditEvent(this,filename,v);
      IvyLog.logD("BSTYLE","Handle error " + evt);
    }
}



@Override public void fireFileFinished(String file)
{
   IvyLog.logD("BSTYLE","File finished " + file);
}



@Override public void auditStarted(AuditEvent e)
{ 
   IvyLog.logD("BSTYLE","Event Audit Started " + e);
}

@Override public void fileStarted(AuditEvent e)
{ 
   IvyLog.logD("BSTYLE","Event File Started " + e);
}

@Override public void addError(AuditEvent e)
{ 
   IvyLog.logD("BSTYLE","Event Add error " + e);
}

@Override public void fileFinished(AuditEvent e)
{ 
   IvyLog.logD("BSTYLE","Event File finished " + e);
}

@Override public void auditFinished(AuditEvent e)
{ 
   IvyLog.logD("BSTYLE","Event Audit finished " + e);
}

@Override public void addException(AuditEvent e,Throwable t)
{
   IvyLog.logD("BSTYLE","Event add exception " + e + " " + t);
}




}       // end of class BstyleChecker




/* end of BstyleChecker.java */

