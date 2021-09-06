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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class BedrockBubblesAction implements IWorkbenchWindowActionDelegate
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

private static final String BUBBLES_URL =
   "https://www.cs.brown.edu/people/spr/bubbles/bubbles.jar";
// private static final long BUBBLES_LENGTH = 110*1024*1024;

private static final String BUBBLES_DIR = ".bubbles";




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
}



/********************************************************************************/
/*                                                                              */
/*      Eclipse tie-in methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public void init(IWorkbenchWindow window)
{
   System.err.println("BEDROCK ACTION INIT " + window);
   active_window = window;
   
   setupDirectories();
}



@Override public void dispose() 
{
   System.err.println("BEDROCK ACTION DISPOSE");
   
   shutdownBubbles();
}


@Override public void selectionChanged(IAction proxy,ISelection sel)
{
   setupAction(proxy);
   
   System.err.println("BEDROCK SELECTION CHANGE " + proxy + " " + sel);
   // nothing required
}


/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void run(IAction proxy)
{
   setupAction(proxy);
   
   System.err.println("BEDROCK ACTION RUN " + proxy);
   if (!installValid()) {
      installBubbles();
    }
   else {
      startBubbles();
    }
   
   Shell shell = active_window.getShell();
   MessageDialog.openInformation(shell,"Hello world.","Hello world!");
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void setupAction(IAction proxy)
{
   if (our_action != null) return;
   System.err.println("BEDROCK ACTION SETUP: " +
         proxy.getStyle() + " " + proxy.getText() + " " + proxy.getToolTipText() +
         " " + proxy.isChecked() + " " + proxy.isEnabled() + " " + proxy.isHandled() +
         " " + proxy.getImageDescriptor() + " " + proxy.getId());
   
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
   System.err.println("SETUP DIRECTORY " + ehome);
   if (ehome.startsWith("file:")) ehome = ehome.substring(5);
   File edir = new File(ehome);
   File drop = new File(edir,"dropins");
   bubbles_dir = new File(drop,"bubbles");
   
   system_props = null;
   File home = new File(System.getProperty("user.home"));
   File bhome = new File(home,BUBBLES_DIR);
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
      String auto = system_props.getProperty("edu.brown.cs.bubbles.autostart");
      if (auto != null && auto.length() >= 1) {
         char c = auto.charAt(0);
         if (c == 'f' || c == 'F' || c == '0' || c == 'n' || c == 'N') auto_start = false;
         else auto_start = true;
       }
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



/********************************************************************************/
/*                                                                              */
/*      Methods to install code bubbles                                         */
/*                                                                              */
/********************************************************************************/

private void installBubbles()
{
   System.err.println("INSTALL BUBBLES");
   bubbles_dir.mkdirs();
   Downloader dl = new Downloader(bubbles_dir);
   dl.start();
   dl.waitForDownload();
   
   String ehome = System.getProperty("eclipse.home.location");
   if (ehome.startsWith("file:")) ehome = ehome.substring(5);
   String earch = System.getProperty("os.arch");
   
         
   // exec java -jar bubbles_dir/bubbles.jar -install
   // run bubbles.jar in setup mode
   our_action.setText("Start Code Bubbles");
}



/********************************************************************************/
/*                                                                              */
/*      Routiunes to run code bubbles                                           */
/*                                                                              */
/********************************************************************************/

private void startBubbles()
{
   // ensure installed
   // check if bubbles running -- ignore if so
   
   // Set eclipse in system_props
   // Set current workspace in system_props
   // Set ask workspace = false in system_props
   // Save system props
   
   System.err.println("START BUBBLES");
   if (bubbles_install) {
      System.err.println("START BUBBLES FROM INSTALLATION");
    }
   else {
      System.err.println("START BUBBLES FROM JAR");
    }
   
   bubbles_running = true;
}


private void shutdownBubbles()
{
   if (bubbles_running) {
      System.err.println("SHUTDONW BUBBLES");
    }
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

