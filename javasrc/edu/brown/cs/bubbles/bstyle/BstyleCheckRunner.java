/********************************************************************************/
/*                                                                              */
/*              BstyleCheckRunner.java                                          */
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
import java.nio.charset.StandardCharsets;
// import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.DefaultContext;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageNamesLoader;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.BeforeExecutionFileFilter;
import com.puppycrawl.tools.checkstyle.api.BeforeExecutionFileFilterSet;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.Context;
import com.puppycrawl.tools.checkstyle.api.FileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.Filter;
import com.puppycrawl.tools.checkstyle.api.FilterSet;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;
import com.puppycrawl.tools.checkstyle.api.RootModule;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.api.SeverityLevelCounter;
import com.puppycrawl.tools.checkstyle.api.Violation;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

import edu.brown.cs.ivy.file.IvyLog;



public class BstyleCheckRunner extends AbstractAutomaticBean implements BstyleConstants, 
      RootModule, MessageDispatcher
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BstyleMain bstyle_main;
private List<AuditListener> audit_listeners;
private List<FileSetCheck> fileset_checks;
private SeverityLevelCounter error_counter;
private ClassLoader module_classloader;
private ModuleFactory module_factory;
private String char_set;
private SeverityLevel severity_level;
private String base_directory;
private int tab_width;
private Context child_context;
private BeforeExecutionFileFilterSet before_filters;
private FilterSet filter_set;
private String [] file_extensions;
private Set<FileSetCheck> inited_checks;

private static final String EXTENSION_SEPARATOR = ".";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleCheckRunner(BstyleMain bm)
{
   bstyle_main = bm;
   error_counter = new SeverityLevelCounter(SeverityLevel.WARNING);
   audit_listeners = new ArrayList<>();
   audit_listeners.add(error_counter);
   
   fileset_checks = new ArrayList<>();
   char_set = StandardCharsets.UTF_8.name();
   module_classloader = null;
   module_factory = null;
   severity_level = SeverityLevel.ERROR;
   base_directory = null;
   tab_width = CommonUtil.DEFAULT_TAB_WIDTH;
   child_context = null;
   before_filters = new BeforeExecutionFileFilterSet();
   filter_set = new FilterSet();
   file_extensions = null;
   inited_checks = new HashSet<>();
}


@Override public void destroy()
{
   audit_listeners.clear();
   fileset_checks.clear();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void setModuleClassLoader(ClassLoader mcl)
{
   module_classloader = mcl;
}


public void setBasedir(String bd)
{
   base_directory = bd;
}


public void setTabWidth(int wd)
{
   tab_width = wd;
}


public void setSeverity(String severity)
{
   severity_level = SeverityLevel.getInstance(severity);
}


public void setFileExtensions(String... extensions)
{
   if (extensions != null) {
      file_extensions = new String[extensions.length];
      for (int i = 0; i < extensions.length; ++i) {
         String ext = extensions[i];
         if (ext.startsWith(EXTENSION_SEPARATOR)) {
            file_extensions[i] = ext;
          }
         else {
            file_extensions[i] = EXTENSION_SEPARATOR + ext;
          }
       }
    }
}



private void addFileSetCheck(FileSetCheck fsc)
{
   fileset_checks.add(fsc);
}


private void addBeforeExecutionFileFilter(BeforeExecutionFileFilter filter)
{
   before_filters.addBeforeExecutionFileFilter(filter);
}


private void addFilter(Filter filter)
{
   filter_set.addFilter(filter); 
}


/********************************************************************************/
/*                                                                              */
/*      Listener management                                                     */
/*                                                                              */
/********************************************************************************/

@Override public void addListener(AuditListener al)
{
   audit_listeners.add(al);
}


public void removeListener(AuditListener al)
{
   audit_listeners.remove(al);
}


private void fireAuditStarted()
{
   final AuditEvent event = new AuditEvent(this);
   for (final AuditListener listener : audit_listeners) {
      listener.auditStarted(event);
    }
}

private void fireAuditFinished()
{
   final AuditEvent event = new AuditEvent(this);
   for (final AuditListener listener : audit_listeners) {
      listener.auditFinished(event);
    }
}

@Override
public void fireFileStarted(String filename) {
// final String stripped = CommonUtil.relativizePath(basedir, fileName);
   String stripped = filename;
   final AuditEvent event = new AuditEvent(this, stripped);
   for (final AuditListener listener : audit_listeners) {
      listener.fileStarted(event);
    }
}

@Override
public void fireErrors(String filename, SortedSet<Violation> errors) {
   final String stripped = CommonUtil.relativizePath(base_directory, filename);
   for (final Violation element : errors) {
      final AuditEvent event = new AuditEvent(this, stripped, element);
      for (final AuditListener listener : audit_listeners) {
         listener.addError(event);
       }
    }
}

/**
 * Notify all listeners about the end of a file audit.
 *
 * @param fileName
 *            the audited file
 */
@Override
public void fireFileFinished(String filename) {
   final String stripped = CommonUtil.relativizePath(base_directory, filename);
   final AuditEvent event = new AuditEvent(this, stripped);
   for (final AuditListener listener : audit_listeners) {
      listener.fileFinished(event);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public int process(List<File> files) throws CheckstyleException
{
   BstyleFileManager fm = bstyle_main.getFileManager();
   List<FileText> texts = new ArrayList<>();
   List<BstyleFile> empty = new ArrayList<>();
   for (File f : files) {
      BstyleFile bf = fm.findFile(f);
      if (bf != null) {
         FileText ft = bf.getFileText();
         if (ft != null) texts.add(ft);
         else empty.add(bf);
       }
    }
   // output FILEERROR message for empty
   int errct = processTexts(texts,empty);
   
   return errct;
}



public int processTexts(List<FileText> files,List<BstyleFile> empty) throws CheckstyleException
{
   fireAuditStarted();
   
   for (FileSetCheck fsc : fileset_checks) {
      if (inited_checks.add(fsc)) {
         fsc.beginProcessing(char_set);
       }
    }
   
   processFileTexts(files);
   
   for (BstyleFile bf : empty) {
      String fnm = bf.getFile().getAbsolutePath();
      fireFileStarted(fnm);
      fireFileFinished(fnm);
    }
   
   for (FileSetCheck fsc : fileset_checks) {
      fsc.finishProcessing();
    }
   
// for (FileSetCheck fsc : fileset_checks) {
//    fsc.destroy();
//  }
   
   int errorcount = error_counter.getCount();
   fireAuditFinished();
   
   return errorcount;
}



private void processFileTexts(List<FileText> files) throws CheckstyleException
{
   IvyLog.logD("BSTYLE","Process file texts " + files.size());
   
   for (FileText ft : files) {
      File f = ft.getFile();
      String fnm = f.getAbsolutePath();
      fireFileStarted(fnm);
      try {
         SortedSet<Violation> errs = processFileText(f,ft);
         fireErrors(fnm,errs);
       }
      catch (Throwable t) {
         IvyLog.logE("BSTYLE","Problem processing file " + f,t);
       }
      finally {
         fireFileFinished(fnm);
       }
    }
}



private SortedSet<Violation> processFileText(File f,FileText ft)
{
   SortedSet<Violation> msgs = new TreeSet<>();
   
   for (FileSetCheck fsc : fileset_checks) {
      try {
         msgs.addAll(fsc.process(f,ft));
       }
      catch (Throwable t) {
         BstyleFileManager fm = bstyle_main.getFileManager();
         BstyleFile bf = fm.findFile(f);
         bf.setHasErrors(true);
         IvyLog.logE("BSTYLE","Problem processing file " + f + " for " + fsc,t);
         // add error violation
       }
    }
   
   return msgs;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override protected void finishLocalSetup() throws CheckstyleException
{
   if (module_factory == null) {
      if (module_classloader == null) {
         throw new CheckstyleException("Module Class Loader not defined");
       }
      Set<String> pkgnames = PackageNamesLoader.getPackageNames(module_classloader);
      module_factory = new PackageObjectFactory(pkgnames,module_classloader);
    }
   
   DefaultContext ctx = new DefaultContext();
   ctx.add("charset",char_set);
   ctx.add("moduleFactory",module_factory);
   ctx.add("severity",severity_level.getName());
   ctx.add("basedir",base_directory);
   ctx.add("tabWidth",String.valueOf(tab_width));
   child_context = ctx;
}


@Override protected void setupChild(Configuration childcfg) throws CheckstyleException
{
   String name = childcfg.getName();
   Object child;
   try {
      child = module_factory.createModule(name);
      if (child instanceof AbstractAutomaticBean) {
         AbstractAutomaticBean aab = (AbstractAutomaticBean) child;
         aab.contextualize(child_context);
         aab.configure(childcfg);
       }
    }
   catch (CheckstyleException ex) {
      throw new CheckstyleException("Problem setting up child: " + ex.getMessage(),ex);
    }
   
   if (child instanceof FileSetCheck) {
      FileSetCheck fsc = (FileSetCheck) child;
      fsc.init();
      addFileSetCheck(fsc);
    }
   else if (child instanceof BeforeExecutionFileFilter) {
      BeforeExecutionFileFilter beff = (BeforeExecutionFileFilter) child;
      addBeforeExecutionFileFilter(beff);
    }
   else if (child instanceof Filter) {
      Filter filter = (Filter) child;
      addFilter(filter);
    }
   else if (child instanceof AuditListener) {
      AuditListener al = (AuditListener) child;
      addListener(al);
    }
   else {
      throw new CheckstyleException("Illegal child for setup " + child);
    }
}




}       // end of class BstyleCheckRunner




/* end of BstyleCheckRunner.java */

