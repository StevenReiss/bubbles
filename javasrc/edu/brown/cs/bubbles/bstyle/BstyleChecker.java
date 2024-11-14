/********************************************************************************/
/*										*/
/*		BstyleChecker.java						*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bstyle;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.ThreadModeSettings;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.Violation;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BstyleChecker implements BstyleConstants, AuditListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BstyleMain			bstyle_main;
private Map<String,ConfigData>	        project_configs;
private ConfigData    			default_config;
private Map<String,ProjectChecker>      project_checkers;
private Map<String,Set<Violation>>      all_errors;        

private static final long               CHANGE_TIME = 50;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BstyleChecker(BstyleMain bm)
{
   bstyle_main = bm;
   project_configs = new HashMap<>();
   default_config = null;
   project_checkers = new HashMap<>();
   all_errors = new HashMap<>();

   BoardProperties bp = BoardProperties.getProperties("Bstyle");

   for (String nm : bp.stringPropertyNames()) {
      if (nm.startsWith("Bstyle.config.file.")) {
	 int idx = nm.lastIndexOf(".");
	 String proj = nm.substring(idx+1);
         String cfile = bp.getProperty(nm);
         project_configs.put(proj,new ConfigData(cfile));
         IvyLog.logD("BSTYLE","Use configuration " + cfile + " for " + proj);
       }
      else if (nm.equals("Bstyle.config.file")) {
         String cfile = bp.getProperty(nm);
         IvyLog.logD("BSTYLE","Default configuration " + cfile);
	 default_config = new ConfigData(cfile);
       }
    }
   
   if (default_config == null) {
      default_config = new ConfigData("sun_checks.xml");
    }
}



/********************************************************************************/
/*										*/
/*     Processing methods							*/
/*										*/
/********************************************************************************/

void processProject(String proj,Collection<BstyleFile> files)
{
   ProjectChecker pc = project_checkers.get(proj);
   if (pc == null) {
      pc = new ProjectChecker(proj);
      project_checkers.put(proj,pc);
      pc.start();
    }
   
   pc.processFiles(files);
}



void runCheckerOnProject(String proj,List<BstyleFile> files)
{
   if (files == null || files.isEmpty()) return;

   ConfigData cfdata = project_configs.get(proj);
   if (cfdata == null) cfdata = default_config;
   
   Configuration cfg = cfdata.getConfiguration(proj);
   if (cfg == null) return;

   ClassLoader mcl = Checker.class.getClassLoader();
   BstyleCheckRunner root = null;
   try {
      root = new BstyleCheckRunner(bstyle_main);
      root.setModuleClassLoader(mcl);
      root.configure(cfg);
      root.addListener(this);
    }
   catch (CheckstyleException e) {
      e.printStackTrace();
      IvyLog.logE("BSTYLE","Problem processing files",e);
      return;
    }

   all_errors.clear();
   List<File> base = new ArrayList<>();
   for (BstyleFile bf : files) {
      base.add(bf.getFile());
    }
   try {
      int ct = root.process(base);
      IvyLog.logD("BSTYLE","Found " + ct + " errors/warnings");
    }
   catch (CheckstyleException e) {
      IvyLog.logE("BSTYLE","Problem processing files",e);
    }
   
   Map<String,Set<Violation>> errs  = null;
   synchronized (all_errors) {
      errs = new HashMap<>(all_errors);
      all_errors.clear();
    }
   
   IvyLog.logD("BSTYLE","Handle results with " + errs.size() + " files");
   
   BstyleFileManager bfm = bstyle_main.getFileManager();
   
   for (Map.Entry<String,Set<Violation>> ent : errs.entrySet()) {
      IvyXmlWriter xw = bstyle_main.beginMessage("FILEERROR");
      xw.field("CATEGORY","BSTYLE");
      xw.field("PROJECT",proj);
      String fnm = ent.getKey();
      BstyleFile bf = bfm.findFile(fnm);
      xw.field("FILE",bf.getUserFile());
      xw.begin("MESSAGES");
      for (Violation v : ent.getValue()) {
         outputViolation(v,bf,xw);
       }
      xw.end("MESSAGES");
      bstyle_main.finishMessage(xw);
    }
}



private void outputViolation(Violation v,BstyleFile bf,IvyXmlWriter xw)
{
   String fnm = v.getSourceName();
   int idx = fnm.lastIndexOf(".");
   if (idx > 0) fnm = fnm.substring(idx+1);
   
   Point pos = bf.getStartAndEndPosition(v.getLineNo(),v.getColumnCharIndex()); 
   String msg = "Style: " + v.getViolation();
   
   xw.begin("PROBLEM");
   xw.field("CATEGORY","BSTYLE");
   xw.field("MSGID",Integer.toString(v.hashCode()));
   xw.field("DATA",v.getModuleId());
   xw.field("MESSAGE",msg);
   xw.field("FILE",bf.getUserFile().getPath()); 
   xw.field("LINE",v.getLineNo());
   switch (v.getSeverityLevel()) {
      case ERROR : 
         xw.field("ERROR",true);
         break;
      case WARNING :
         xw.field("WARNING",true);
         break;
      default :
         xw.field("INFO",true);
         break;
    }
   xw.field("START",pos.x);
   xw.field("END",pos.y);
   xw.field("COLUMN",v.getColumnNo());
   xw.field("COLIDX",v.getColumnCharIndex());
   xw.end("PROBLEM");
}



/********************************************************************************/
/*										*/
/*	Configuration methods							*/
/*										*/
/********************************************************************************/

private Configuration buildConfiguration(String proj,String configpath)
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
/*										*/
/*	Handle error returns							*/
/*										*/
/********************************************************************************/


@Override public void auditStarted(AuditEvent e)
{
   IvyLog.logD("BSTYLE","Event Audit Started " + e);
   
   all_errors.clear();
}

@Override public void fileStarted(AuditEvent e)
{
   IvyLog.logD("BSTYLE","Event File Started " + e);
   String fnm = e.getFileName();
   synchronized (all_errors) {
      Set<Violation> errs = all_errors.get(fnm);
      if (errs == null) all_errors.put(fnm,new TreeSet<>()); 
    }
}

@Override public void addError(AuditEvent e)
{
   Violation v = e.getViolation();
   if (v == null) return;
   String fnm = e.getFileName();
   
   Set<Violation> vset = all_errors.get(fnm);
   if (vset == null) {
      fileStarted(e);           // this synchronizes on all_errors
      vset = all_errors.get(fnm);
    }
   if (vset == null) return;
   synchronized (vset) {
      vset.add(v);
    }
   
   IvyLog.logD("BSTYLE","Event Add error " +
      e.getFileName() + " " + e.getLine() + " " + e.getColumn() + " " +
      e.getMessage() + " " + e.getModuleId() + " " + e.getSeverityLevel());
}



@Override public void fileFinished(AuditEvent e)
{
   String fnm = e.getFileName();
   int ct = 0;
   synchronized (all_errors) {
      Set<Violation> errs = all_errors.get(fnm);
      if (errs != null) ct = errs.size();
    }
   IvyLog.logD("BSTYLE","Event File finished " + e + " " + ct);
   
}

@Override public void auditFinished(AuditEvent e)
{
   IvyLog.logD("BSTYLE","Event Audit finished " + e);
}

@Override public void addException(AuditEvent e,Throwable t)
{
   IvyLog.logD("BSTYLE","Event add exception " + e + " " + t);
}


/********************************************************************************/
/*                                                                              */
/*      Information for a Configuration                                         */
/*                                                                              */
/********************************************************************************/

private class ConfigData {
   
   private String config_file;
   private long config_dlm;
   private Configuration project_config;
   
   ConfigData(String cfg) {
      if (cfg == null || cfg.equals("*") || cfg.isEmpty()) {
         config_file = null;
         project_config = null;
         config_dlm = 0;
         return;
       }
      
      config_file = cfg;
      File cff = new File(cfg);
      if (cff.isAbsolute()) {
         config_dlm = cff.lastModified();
       }
      else {
         config_dlm = 0;
       }
      project_config = null;
      // 	 Configuration cfg = getConfiguration(proj,bp.getProperty(nm));
    }
   
   Configuration getConfiguration(String proj) {
      if (config_dlm > 0 && project_config != null) {
         File f = new File(config_file);
         if (f.lastModified() > config_dlm) {
            project_config = null;
          }
       }
      
      if (project_config == null && config_file != null) {
         project_config = buildConfiguration(proj,config_file);
       }
      
      return project_config;
    }
   
}       // end of inner class ConfigData



/********************************************************************************/
/*                                                                              */
/*      Thread and data for running checkstyle on a project                     */
/*                                                                              */
/********************************************************************************/

private class ProjectChecker extends Thread {
   
   private String project_name;
   private Set<BstyleFile> todo_files;
   private long last_change;
   
   private ProjectChecker(String proj) {
      project_name = proj;
      last_change = 0;
    }
   
   synchronized void processFiles(Collection<BstyleFile> files) {
      if (files == null) return;
      if (todo_files == null) {
         todo_files = new HashSet<>();
       }
      todo_files.addAll(files);
      last_change = System.currentTimeMillis();
      IvyLog.logD("BSTYLE","Add " + files.size() + " to process set for " + project_name);
      notifyAll();
    }
   
   public void run() {
      for ( ; ; ) {
         List<BstyleFile> todo = null;
         synchronized (this) {
            while (todo_files == null) {
               try {
                  wait(5000);
                }
               catch (InterruptedException e) { }
             }
            while ((System.currentTimeMillis() - last_change) < CHANGE_TIME) {
               try {
                  wait(CHANGE_TIME);
                }
               catch (InterruptedException e) { }
             }
            todo = new ArrayList<>(todo_files);
            todo_files = null;
          }
         if (todo != null) {
            IvyLog.logD("BSTYLE","Start running checker for " + project_name + " " +
                  todo.size());
            runCheckerOnProject(project_name,todo);
          }
       }
    }
   
}       // end of inner class ProjectChecker



}	// end of class BstyleChecker




/* end of BstyleChecker.java */

