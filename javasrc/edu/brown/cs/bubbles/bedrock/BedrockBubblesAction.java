/********************************************************************************/
/*                                                                              */
/*              BedrockBubblesAction.java                                       */
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



package edu.brown.cs.bubbles.bedrock;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class BedrockBubblesAction implements IWorkbenchWindowActionDelegate, BedrockConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IWorkbenchWindow        active_window;
private IAction                 our_action;
private File                    bubbles_dir;
private boolean                 bubbles_running;
private File                    props_file;
private Properties              system_props;
private boolean                 bubbles_install;
private boolean                 auto_start;
private Process                 bubbles_process;
private String                  prop_base;

private static final String BUBBLES_URL =
   "https://www.cs.brown.edu/people/spr/bubbles/bubbles.jar";
// private static final long BUBBLES_LENGTH = 110*1024*1024;

private final static String BUBBLES_DIR = ".bubbles";
// private static final String BUBBLES_DIR = ".bubbles.test";


private final String [] ECLIPSE_START = new String [] {
      "eclipse", "eclipse.exe", "Eclipse.app",
      "STS.exe", "STS", "STS.app",
      "sts.exe", "stS", "sts.app",
      "myeclipse", "myeclipse.exe", "myeclipse.app",
      "Eclipse JEE.app", "eclipse JEE.app",
};




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BedrockBubblesAction()
{
   active_window = null;
   our_action = null;
   bubbles_running = false;
   props_file = null;
   system_props = null;
   bubbles_install = false;
   auto_start = false;
   bubbles_process = null;
   
 
}


/********************************************************************************/
/*                                                                              */
/*      Eclipse tie-in methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public void init(IWorkbenchWindow window)
{
   active_window = window;
   
   setupDirectories();
   
   if (system_props != null) {
      String lvl = system_props.getProperty("edu.brown.cs.bubbles.log_level");
      if (lvl != null && lvl.startsWith("D")) BedrockPlugin.setLogLevel(BedrockLogLevel.DEBUG);
    }
}



@Override public void dispose() 
{
   shutdownBubbles();
}


@Override public void selectionChanged(IAction proxy,ISelection sel)
{
   setupAction(proxy);
}


/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void run(IAction proxy)
{
   if (active_window == null) return;
   
   setupAction(proxy);
   
   if (!installValid()) {
      installBubbles();
    }
   else {
      startBubbles();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void setupAction(IAction proxy)
{
   if (our_action != null) return;
   bubbles_running = isBubblesRunning();
   
   our_action = proxy;
   
   if (!installValid()) {
      our_action.setText("Install Code Bubbles");
    } 
   else if (!bubbles_running) {
      our_action.setText("Start Code Bubbles");
    }
   else {
      our_action.setText("Start Code Bubbles");
      our_action.setEnabled(false);
    }
   
   if (auto_start) {
      startBubbles();
    }
}


private void setupDirectories()
{
   String ehome = System.getProperty("eclipse.home.location");
   if (ehome.startsWith("file:")) ehome = ehome.substring(5);
   File edir = new File(ehome);
// File drop = new File(edir,"dropins");
   bubbles_dir = new File(edir,"bubbles");
   
   system_props = null;
   prop_base = System.getProperty("edu.brown.cs.bubbles.BEDROCKBASE");
   if (prop_base == null) prop_base = System.getenv("BUBBLES_BEDROCKBASE");
   if (prop_base == null) prop_base = BUBBLES_DIR; 
   File bhome = new File(prop_base);
   if (!bhome.isAbsolute()) {
      File home = new File(System.getProperty("user.home"));
      bhome = new File(home,prop_base);
    }
   bhome.mkdirs();
   props_file = new File(bhome,"System.props");
   if (props_file.exists() && props_file.canRead()) {
      try (InputStream ins = new FileInputStream(props_file)) {
         system_props = new Properties();
         system_props.loadFromXML(ins);
       }
      catch (IOException e) {
         system_props = null;
       }
    }
   if (system_props != null) {
      String s1 = system_props.getProperty("edu.brown.cs.bubbles.jar");
      if (s1 != null) {
         File f1 = new File(s1);
         if (f1.exists() && f1.isDirectory()) bubbles_dir = f1;
       }
      String s2 = system_props.getProperty("edu.brown.cs.bubbles.install");
      if (s2 != null) {
         File f2 = new File(s2);
         if (f2.exists() && f2.isDirectory()) {
            bubbles_dir = f2;
            bubbles_install = true;
          }      
       }
      auto_start = getBooleanProp("edu.brown.cs.bubbles.autostart",false);
    }
}



private boolean installValid()
{
   if (bubbles_dir == null) return false;
   if (!bubbles_dir.exists()) return false;
   if (bubbles_install) return true;
   File f1 = new File(bubbles_dir,"bubbles.jar");
   File f2 = new File(bubbles_dir,"lib");
   if (!f1.exists()|| !f1.canRead() || !f2.exists() || !f2.isDirectory()) return false;
   
   return true;
}



private boolean getBooleanProp(String name,boolean dflt)
{
   boolean rslt = dflt;
   if (system_props != null) {
      String p = system_props.getProperty(name);
      if (p != null && p.length() >= 1) {
         char c = p.charAt(0);
         if (c == 'f' || c == 'F' || c == '0' || c == 'n' || c == 'N') rslt = false;
         else rslt = true;
       }
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Methods to install code bubbles                                         */
/*                                                                              */
/********************************************************************************/

private boolean installBubbles()
{
   System.err.println("INSTALLING CODE BUBBLES in " + bubbles_dir);
   bubbles_dir.mkdirs();
   Downloader dl = new Downloader(bubbles_dir);
   dl.start();
   boolean fg = dl.waitForDownload();
   if (!fg) return false;
 
   if (!updateProperties()) return false;
 
   File f = new File(bubbles_dir,"bubbles.jar");
   ProcessBuilder pb = new ProcessBuilder("java","-jar",f.getPath(),"-install","-insnobed");
   BedrockPlugin.logI("BUBBLES RUN " + pb.command());
   
   try {
      Process p = pb.start();
      try {
         int sts = p.waitFor();
         if (sts > 0) {
            System.err.println("BUBBLES: Bubbles setup failed: " + sts);
            return false;
          }
       }
      catch (InterruptedException e) { }
    }
   catch (IOException e) {
      System.err.println("BUBBLES: Problem setting up bubbles: " + e);
      return false;
    }
         
   our_action.setText("Start Code Bubbles");
   
   return true;
}




private File getEclipseDirectory()
{
   String ehome = System.getProperty("eclipse.home.location");
   if (ehome.startsWith("file:")) ehome = ehome.substring(5);
   File fehome = new File(ehome);
   boolean fnd = false;
   while (fehome != null) {
      for (String s : ECLIPSE_START) {
         File f1 = new File(fehome,s);
         if (f1.exists() && f1.canExecute() && f1.canRead() && 
               (f1.getName().endsWith(".app") || !f1.isDirectory())) {
            fnd = true; 
            break;
          }
       }
      if (fnd) break;
      fehome = fehome.getParentFile();
    }
   
   return fehome;
}



private boolean updateProperties()
{
   File fehome = getEclipseDirectory();
   if (fehome == null) {
      System.err.println("BUBBLES: No eclipse directory");
      return false;
    }
  
   if (system_props == null) system_props = new Properties();
   
   String earch = System.getProperty("os.arch");
   system_props.setProperty("edu.brown.cs.bubbles.eclipse." + earch,fehome.getPath());
   
   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot root = ws.getRoot();
   IPath rootpath = root.getRawLocation();
   system_props.setProperty("edu.brown.cs.bubbles.workspace",rootpath.toOSString());
   system_props.setProperty("edu.brown.cs.bubbles.ask_workspace","false");
   
   try (FileOutputStream fos = new FileOutputStream(props_file)) {
      system_props.storeToXML(fos,"Bubbles Startup");
    }
   catch (IOException e) {
      System.err.println("BUBBLES Error writing properties file: " + e);
      return false;
    }
    
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Routiunes to run code bubbles                                           */
/*                                                                              */
/********************************************************************************/

private boolean startBubbles()
{
   if (!installValid()) return false;
   if (bubbles_process != null) return true;
   
   if (!updateProperties()) return false;
   if (isBubblesRunning()) return true;
   
   String mint = BedrockPlugin.getPlugin().getMintName();
   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot root = ws.getRoot();
   IPath rootpath = root.getRawLocation();
   String wspath = rootpath.toOSString();
   
   System.err.println("STARTING CODE BUBBLES");
   
   ProcessBuilder pb = null;
   if (bubbles_install) {
      File f1 = new File(bubbles_dir,"bin");
      File f2 = new File(f1,"codebb");
      pb = new ProcessBuilder(f2.getAbsolutePath(),wspath,"-msg",mint,"-noupdate","-insnobed");
    }
   else {
      File f = new File(bubbles_dir,"bubbles.jar");
      pb = new ProcessBuilder("java","-jar",f.getPath(),
            wspath,"-msg",mint,"-noupdate","-insnobed");
    }
   
   try {
      BedrockPlugin.logD("BUBBLES RUN " + pb.command());
      bubbles_process = pb.start();
    }
   catch (IOException e) {
      return false;
    }
   
   bubbles_running = true;
   
   boolean hide = !getBooleanProp("edu.brown.cs.bubbles.foreground",true);
   if (hide) {
      BedrockApplication ba = new BedrockApplication(true);
      ba.startedBubbles(hide);
    }
  
   return true;
}


private void shutdownBubbles()
{
   if (bubbles_running) {
      System.err.println("SHUTDONW BUBBLES");
    }
}


private boolean isBubblesRunning()
{
   BedrockPlugin bp = BedrockPlugin.getPlugin();
   String cmd = "PING";
   IvyXmlWriter xw = bp.beginMessage(cmd);
   String resp = bp.finishMessageWait(xw,5000);
   if (resp != null) return true;
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Download bubbles                                                        */
/*                                                                              */
/********************************************************************************/

private class Downloader extends Thread {

   private File install_directory;
   private Boolean download_ok;
   
   Downloader(File install) {
      super("BUBBLES_DOWNLOADER");
      install_directory = install;
      download_ok = null;
    }
   
   synchronized boolean waitForDownload() {
      while (download_ok == null) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
      return download_ok;
    }
   
   @Override public void run() {
      boolean remove = false;
      boolean status = false;
      File f = new File(install_directory,"bubbles.jar");
      if (install_directory.exists() || install_directory.mkdirs()) {
         try {
            URL u = new URL(BUBBLES_URL);
            URLConnection conn = u.openConnection();
            BufferedInputStream ins = new BufferedInputStream(conn.getInputStream());
            try (OutputStream ots = new FileOutputStream(f)) {
               byte [] buf = new byte[16384];
               for ( ; ; ) {
                  if (isInterrupted()) {
                     remove = true;
                     break;
                   }
                  int rlen = ins.read(buf);
                  if (rlen <= 0) break;
                  ots.write(buf,0,rlen);
                }
             }
            ins.close();
            status = true;
          }
         catch (Throwable e) {
            System.err.println("BUBBLES DOWNLOAD ERROR: " + e);
            remove = true;
          }
         if (remove) {
            f.delete();
          }
         synchronized (this) {
            download_ok = status;
            notifyAll();
          }
       }
    }
   
}	// end of inner class Downloader


}       // end of class BedrockBubblesAction




/* end of BedrockBubblesAction.java */

