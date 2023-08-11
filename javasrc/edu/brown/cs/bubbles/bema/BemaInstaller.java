/********************************************************************************/
/*										*/
/*		BemaInstaller.java						*/
/*										*/
/*	Installation program for code bubbles					*/
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bema;

import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class BemaInstaller
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BemaInstaller inst = new BemaInstaller();
   inst.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum PanelType {
   SPLASH, DIRECTORY, ECLIPSE, PROGRESS, FINISH, EXIT 
}



private File			eclipse_directory;
private String                  eclipse_version;
private File			install_directory;
private JFrame			outer_frame;
private Map<String,String>      eclipse_versions;

private Map<PanelType,InstallerPanel>	installer_panels;
private PanelType			current_panel;


private static final String BROWN_IMAGE_URL = "http://www.cs.brown.edu/people/spr/bubbles/images/brown.png";
private static final String BUBBLES_IMAGE_URL = "http://www.cs.brown.edu/people/spr/bubbles/images/bubbles.png";
private static final String BROWN_IMAGE_PATH = "/images/brown.png";
private static final String BUBBLES_IMAGE_PATH = "/images/bubbles.png";

private static final int SPLASH_WIDTH = 450;
private static final int SPLASH_HEIGHT = 300;

private static final int ECLIPSE_WIDTH = 500;
private static final int ECLIPSE_HEIGHT = 400;

private static final String [] ECLIPSE_START = new String [] {
      "eclipse", "eclipse.exe", "Eclipse.app",
      "STS.exe", "STS", "STS.app",
      "sts.exe", "stS", "sts.app",
      "myeclipse", "myeclipse.exe", "myeclipse.app"
};


private static final String BOARD_ECLIPSE_PLUGINS = "plugins";
private static final String BOARD_ECLIPSE_DROPINS = "dropins";
private static final String BOARD_ECLIPSE_MAC_DROPIN = "Eclipse.app/Contents/Eclipse/dropins";

private static final String ECLIPSE_DOWNLOAD = "http://ftp.osuosl.org/pub/eclipse/technology/epp/downloads/release/";

private static final int DIRECTORY_WIDTH = 500;
private static final int DIRECTORY_HEIGHT = 400;

private static final int PROGRESS_WIDTH = 500;
private static final int PROGRESS_HEIGHT = 400;
private static final String BUBBLES_URL = "http://www.cs.brown.edu/people/spr/bubbles/bubbles.jar";
private static final long BUBBLES_LENGTH = 44*1024*1024;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BemaInstaller()
{
   eclipse_directory = null;
   install_directory = null;
   eclipse_version = null;

   installer_panels = new HashMap<>();
   installer_panels.put(PanelType.SPLASH,new SplashPanel());
   installer_panels.put(PanelType.DIRECTORY,new DirectoryPanel());
   installer_panels.put(PanelType.ECLIPSE,new EclipsePanel());
   installer_panels.put(PanelType.PROGRESS,new ProgressPanel());
   installer_panels.put(PanelType.FINISH,new FinishPanel());
   current_panel = PanelType.SPLASH;
   
   getEclipseVersions();
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   outer_frame = new JFrame();
   outer_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

   setPanel();
}



/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

private void setPanel()
{
   if (current_panel  == null) current_panel = PanelType.SPLASH;
   if (current_panel == PanelType.EXIT) {
      System.exit(0);
    }
   
   PanelType pt = current_panel;

   InstallerPanel ip = installer_panels.get(current_panel);
   if (ip == null) {
      if (current_panel != pt) {
         setPanel();
       }
      return;
    }
   JPanel pnl = ip.getPanel();
   
   outer_frame.setContentPane(pnl);
   outer_frame.pack();
   outer_frame.setVisible(true);
   if (current_panel == PanelType.SPLASH) {
      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension ssz = tk.getScreenSize();
      Dimension wsz = outer_frame.getSize();
      int xpos = ssz.width/2 - wsz.width/2;
      int ypos = ssz.height/2 - wsz.height/2;
      outer_frame.setLocation(xpos,ypos);
    }

   ip.recheck();
}


private void moveToPanel(PanelType pnl)
{
   current_panel = pnl;
   setPanel();
}



/********************************************************************************/
/*                                                                              */
/*      Eclipse checking methods                                                */
/*                                                                              */
/********************************************************************************/

private boolean checkEclipseDirectory(File ed) 
{
   if (!ed.exists() || !ed.isDirectory()) return false;
   
   boolean execfnd = false;
   for (String s : ECLIPSE_START) {
      File binf = new File(ed,s);
      if (binf.exists() && binf.canExecute()) {
         execfnd = true;
         break;
       }
    }
   if (!execfnd) return false;
   
   File pdf = getPluginDirectory(ed);
   if (pdf == null || !pdf.exists() || !pdf.isDirectory() || !pdf.canWrite()) return false;
   
   if (checkEclipseVersion(pdf)) return true;
   File g1 = new File(pdf.getParentFile(),BOARD_ECLIPSE_PLUGINS);
   if (checkEclipseVersion(g1)) return true;
   
   File f1 = ed.getParentFile();
   if (f1 != null) f1 = f1.getParentFile();
   if (f1 != null) f1 = f1.getParentFile();
   if (f1 != null) {
      f1 = new File(f1,".p2");
      f1 = new File(f1,"pool");
      f1 = new File(f1,"plugins");
      if (checkEclipseVersion(f1)) return true;
    }
   
   File f2 = new File(System.getProperty("user.home"),".p2");
   f2 = new File(f2,"pool");
   f2 = new File(f2,"plugins");
   if (checkEclipseVersion(f2)) return true;
   
   return true;
}



private boolean checkEclipseVersion(File pdf) 
{
   if (!pdf.exists() || !pdf.isDirectory()) return false;
   if (pdf.list() == null) return false;
   
   boolean havejava = false;
   for (String fnm : pdf.list()) {
      if (fnm.startsWith("org.eclipse.platform_")) {
         Pattern p = Pattern.compile("(\\d+\\.\\d+)\\.\\d\\.");
         Matcher m = p.matcher(fnm);
         if (m.find()) {
            String ver = m.group(1);
            if (ver != null) {
               double d = Double.parseDouble(ver);
               if (d < 4.0) {
                  return false;			   // illegal Eclipse version
                }
             }
          }
       }
      if (fnm.startsWith("org.eclipse.jdt.core")) havejava = true;
    }
   if (!havejava) return false;
   
   // check for proper architecture as well
   
   return true;
}



private File getPluginDirectory(File edf) {
   File pdf = new File(edf,BOARD_ECLIPSE_DROPINS);
   
   if (!pdf.exists() || !pdf.isDirectory() || !pdf.canWrite()) {
      File ddf = new File(edf,BOARD_ECLIPSE_PLUGINS);
      if (ddf.exists() && ddf.isDirectory() && ddf.canWrite()) pdf = ddf;
    }
   
   if (!pdf.exists() || !pdf.isDirectory() || !pdf.canWrite()) {
      File ddf = new File(edf,BOARD_ECLIPSE_MAC_DROPIN);
      if (ddf.exists() && ddf.isDirectory() && ddf.canWrite()) pdf = ddf;
    }
   
   return pdf;
}




/********************************************************************************/
/*                                                                              */
/*      Get Eclipse versions                                                    */
/*                                                                              */
/********************************************************************************/

private void getEclipseVersions()
{
   eclipse_versions = new LinkedHashMap<>();
   try {
      Connection conn = HttpConnection.connect(ECLIPSE_DOWNLOAD);
      Document doc = conn.get(); 
      Elements elts = doc.select("table tr td a");
      for (Element elt : elts) {
         String link = elt.attr("href");
         String name = elt.text();
         if (link == null || !link.endsWith("/") || 
               name.contains("Parent ") || name.length() < 3 ||
               name.contains("osuosl.org")) continue;
         link = link.substring(0,link.length()-1);
         String linkurl = checkValidVersion(link);
         if (linkurl == null) continue;
         eclipse_versions.put(link,linkurl);
       }
    }
   catch (IOException e) { 
       System.err.println("Problem getting Eclipse versions");
       System.exit(1);
    }
}



private String checkValidVersion(String ver)
{
   String os = System.getProperty("os.name").toLowerCase();
   String ar = System.getProperty("os.arch").toLowerCase();
   String id = null;
   if (os.contains("linux")) {
      if (ar.contains("64")) id = "linux-gtk-x86_64";
      else id = "linux-gtk";
    }
   else if (os.contains("win")) {
      if (ar.contains("64")) id = "win32-x86_64";
      else id = "win32";
    }
   else if (os.contains("mac")) {
      if (ar.contains("64")) id = "cocoa-x86_64";
    }
   if (id == null) return null;
   
   String url = checkVersionExists(ver,"R","-R-",id);
   if (url != null) return url;
   return null;
}



private String checkVersionExists(String ver,String k1,String k2,String id)
{
   String url = ECLIPSE_DOWNLOAD;
   String v = ver.toLowerCase();
   url += v + "/" + k1 + "/eclipse-dsl-" + v + k2 + id + ".tar.gz";
   try {
      URI u = new URI(url);
      URLConnection uc = u.toURL().openConnection();
      uc.getDate();
      long lm = uc.getLastModified();
      int len = uc.getContentLength();
      if (lm > 0 && len > 102400) return url;
      return null;
    }
   catch (Exception e) {
      return null;
    }
}



/********************************************************************************/
/*										*/
/*	Generic panel								*/
/*										*/
/********************************************************************************/

private abstract class InstallerPanel {

   protected JButton continue_button;

   protected InstallerPanel() {
      continue_button = null;
    }

   void recheck() {
      if (continue_button != null) {
         boolean fg = validate();
         continue_button.setEnabled(fg);
       }
    }

   protected void exit() {
      System.exit(0);
    }

   abstract JPanel getPanel();

   protected abstract boolean validate();

}	// end of inner class InstallerPanel




/********************************************************************************/
/*										*/
/*	Splash panel								*/
/*										*/
/********************************************************************************/

private class SplashPanel extends InstallerPanel implements ActionListener {

   @Override JPanel getPanel() {
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
   
      SplashImage spnl = new SplashImage();
      pnl.addLabellessRawComponent("SPLASH",spnl);
   
      pnl.addBottomButton("Exit","EXIT",this);
      continue_button = pnl.addBottomButton("Continue","CONTINUE",this);
      pnl.addBottomButtons();
   
      return pnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      switch (cmd) {
	 case "EXIT" :
	    exit();
	    break;
	 case "CONTINUE" :
	    moveToPanel(PanelType.DIRECTORY);
	    break;
       }
    }

   @Override protected boolean validate()		{ return true; }

}	// end of inner class SplashPanel




/********************************************************************************/
/*										*/
/*	Directory Panel 							*/
/*										*/
/********************************************************************************/

private class DirectoryPanel extends InstallerPanel implements ActionListener, UndoableEditListener
{
   private JTextField directory_field;
   private JCheckBox create_button;
   
   DirectoryPanel() {
      directory_field = null;
      create_button = null;
    }
   
   @Override protected JPanel getPanel() {
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Specify Installation Location");
      
      String explstr = "<html><p>";
      explstr += "Select a directory into which Code Bubbles should be installed. ";
      explstr += "This can either be a new empty directory (choose the create option ";
      explstr += "if appropriate) or a location where Code Bubbles was previously ";
      explstr += "installed.";
      
      JLabel expl = new JLabel(explstr);
      
      pnl.addLabellessRawComponent("EXPLANATION",expl);
      directory_field = pnl.addFileField("Bubbles Home",(File) null,JFileChooser.DIRECTORIES_ONLY,this,this);
      create_button = pnl.addBoolean("Create directory",false,this);
      
      pnl.addBottomButton("BACK","BACK",this);
      continue_button = pnl.addBottomButton("Install Code Bubbles","CONTINUE",this);
      continue_button.setEnabled(false);
      pnl.addBottomButtons();
      
      Dimension sz = new Dimension(DIRECTORY_WIDTH,DIRECTORY_HEIGHT);
      pnl.setSize(sz);
      pnl.setPreferredSize(sz);
      
      return pnl;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand().toUpperCase();
      switch (cmd) {
	 case "CONTINUE" :
	    moveToPanel(PanelType.ECLIPSE);
	    break;
	 case "BACK" :
	    moveToPanel(PanelType.SPLASH);
	    break;
	 default :
	    recheck();
	    break;
       }
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent e) {
      recheck();
    }
   
   @Override protected boolean validate() {
      install_directory = null;
      String dirf = directory_field.getText();
      if (dirf == null || dirf.length() == 0) return false;
      
      File ind = new File(dirf);
      if (ind.exists() && ind.isDirectory() && ind.canWrite()) {
	 boolean isempty = true;
	 boolean isvalid = false;
	 for (String cnt : ind.list()) {
	    if (cnt.startsWith(".")) continue;
	    if (cnt.equals("bubbles.jar")) isvalid = true;
	    else isempty = false;
	  }
	 if (isempty || isvalid) {
	    install_directory = ind;
	    return true;
	  }
	 return false;
       }
      if (create_button.isSelected()) {
	 for (File f = ind.getParentFile(); f != null; f = f.getParentFile()) {
	    if (f.exists() && f.canWrite()) {
	       install_directory = ind;
	       return true;
	     }
	  }
       }
      return false;
    }
   
   
}	// end of inner class DirectoryPanel




/********************************************************************************/
/*										*/
/*	Eclipse panel -- panel for checking eclipse installation		*/
/*										*/
/********************************************************************************/

private class EclipsePanel extends InstallerPanel implements ActionListener, UndoableEditListener {

   private JCheckBox  ignore_button;
   private JTextField eclipse_field;
   private SwingComboBox<String> version_choice;
   
   EclipsePanel() {
      ignore_button = null;
      eclipse_field = null;
      version_choice = null;
    }

   @Override JPanel getPanel() {
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Locate Eclipse Installation");
      
      String pnm = System.getenv("ECLIPSEROOT");
      if (pnm != null) {
         if (!checkEclipseDirectory(new File(pnm))) pnm = null; 
       }
      
      File pfl = null;
      if (pnm == null) {
         pfl = new File(install_directory,"eclipse");
       }
      else {
         pfl = new File(pnm);
       }
   
      String explstr = "<html><p>";
      explstr += "Code Bubbles requires a writable Eclipse installation ";
      explstr += "in order to work for Java programs.  It is not needed if ";
      explstr += "all you are going to do is Code Bubbles for Node.JS or ";
      explstr += "Code Bubbles for Python.";
      explstr += "<p>You can a) specify the path of your current Eclipse ";
      explstr += "installation into which the Code Bubbles plugin will be installed; ";
      explstr += "request a new Eclipse installation just for Code Bubbles; or skip ";
      explstr += "this step if you want to install your own Eclispe or aren't going ";
      explstr += "to be working with Java.";
   
      JLabel expl = new JLabel(explstr);
   
      pnl.addLabellessRawComponent("EXPLANATION",expl);
      eclipse_field = pnl.addFileField("Eclipse Home",pfl,JFileChooser.DIRECTORIES_ONLY,this,this);
      version_choice = pnl.addChoice("Install Eclipse Version",eclipse_versions.keySet(),0,this);
      ignore_button = pnl.addBoolean("Ignore for now",false,this);
   
      pnl.addBottomButton("BACK","BACK",this);
      continue_button = pnl.addBottomButton("CONTINUE","CONTINUE",this);
      continue_button.setEnabled(false);
      pnl.addBottomButtons();
   
      Dimension sz = new Dimension(ECLIPSE_WIDTH,ECLIPSE_HEIGHT);
      pnl.setSize(sz);
      pnl.setPreferredSize(sz);
   
      return pnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand().toUpperCase();
      switch (cmd) {
         case "CONTINUE" :
            moveToPanel(PanelType.PROGRESS);
            break;
         case "BACK" :
            moveToPanel(PanelType.DIRECTORY);
            break;
         default :
            recheck();
            break;
       }
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      recheck();
    }

   @Override protected boolean validate() {
      eclipse_directory = null;
      eclipse_version = null;
      if (ignore_button.isSelected()) return true;
      File dirp = new File(eclipse_field.getText());
      int vidx = version_choice.getSelectedIndex();
      if (vidx < 0) vidx = 0;
      eclipse_version = version_choice.getItemAt(vidx);
      if (eclipse_version.startsWith("NO")) {
         if (!checkEclipseDirectory(dirp)) return false;
         eclipse_directory = dirp;
       }
      else {
         if (dirp.exists()) {
            if (!dirp.isDirectory()) return false;
            File [] files = dirp.listFiles();
            if (files != null && files.length > 0) {
               if (!checkEclipseDirectory(dirp)) return false;
             }
          }
         eclipse_directory = dirp;
       }
      return true;
    }

   

   

   

}	// end of inner class EclipsePanel




/********************************************************************************/
/*										*/
/*	Eclipse downloader							*/
/*										*/
/********************************************************************************/

private class EclipseDownloader extends Thread {
 
   private ProgressPanel progress_panel;

   EclipseDownloader(ProgressPanel p) {
      super("ECLIPSE_DOWNLOADER");
      progress_panel = p;
    }

   @Override public void run() {
      File f = eclipse_directory.getParentFile();
      if (!f.exists() && !f.mkdirs()) {
         progress_panel.handleResult(false);
         return;
       }
      String urltxt = eclipse_versions.get(eclipse_version);
      if (urltxt != null) {
         try {
            URI url = new URI(urltxt);
            URLConnection uc = url.toURL().openConnection();
            InputStream ins = uc.getInputStream();
            File fd = eclipse_directory;
            if (fd.getName().equals("eclipse")) {
               fd = fd.getParentFile();
             }
            else {
               eclipse_directory = new File(fd,"eclipse");
             }
            installEclipseFromJar(ins,fd);
            ins.close();
            return;
          }
         catch (InterruptedException e) {
            try {
               IvyFile.remove(eclipse_directory);
             }
            catch (IOException ex) { }
            progress_panel.handleResult(false);
          }
         catch (IOException | URISyntaxException e) {
            System.err.println("Problem installing: " + e);
            e.printStackTrace();
            progress_panel.handleResult(false);
         }
       }
    }

   private void installEclipseFromJar(InputStream fis,File dir) 
        throws IOException, InterruptedException {
      dir.mkdir();
      GZIPInputStream gis = new GZIPInputStream(fis);
      TarArchiveInputStream tis = new TarArchiveInputStream(gis);
      TarArchiveEntry tent = null;
      while ((tent = tis.getNextTarEntry()) != null) {
         String nm = tent.getName();
         if (nm == null || !nm.startsWith("eclipse")) continue;
         File f2 = getNestedFile(dir,nm);
         int mode = tent.getMode();
         if (nm.endsWith("/")) f2.mkdir();
         else {
            IvyFile.copyFileNoClose(tis,f2);
            f2.setExecutable((mode & 0100) != 0,false);
            f2.setReadable((mode & 0400) != 0,false);
            f2.setWritable((mode & 02) != 0,false);
            f2.setWritable((mode & 0200) != 0,true);
          }
       }
    }

   private File getNestedFile(File start,String nest) {
      if (nest == null || nest.length() == 0) return start;
      int idx = nest.lastIndexOf("/");
      File f1 = start;
      if (idx > 0) f1 = getNestedFile(start,nest.substring(0,idx));
      return new File(f1,nest.substring(idx+1));
    }

   

}       // end of inner class EclipseDownloader







/********************************************************************************/
/*										*/
/*	Progress Panel								*/
/*										*/
/********************************************************************************/

private class ProgressPanel extends InstallerPanel implements ActionListener {

   private JProgressBar progress_bar;
   private JLabel	task_label;
   private Downloader	bubbles_downloader;
   private EclipseDownloader eclipse_downloader;
   private boolean	all_done;
   private boolean      do_eclipse;

   ProgressPanel() {
      progress_bar = null;
      task_label = null;
      all_done = false;
      do_eclipse = false;
      bubbles_downloader = null;
      eclipse_downloader = null;
    }

   @Override protected JPanel getPanel() {
      if (eclipse_version == null || eclipse_version.startsWith("NO") || 
            eclipse_directory == null) {
         do_eclipse = false;
       }
      else {
         if (checkEclipseDirectory(eclipse_directory)) do_eclipse = false;
         else do_eclipse = true;  
       }
      
      String what = "Code Bubbles";
      if (do_eclipse) what += " and Eclipse";
      
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Installing " + what);
   
      String explstr = "<html><p>";
      explstr += "Please wait while we download and install ";
      if (do_eclipse) explstr += "Eclipse and ";
      explstr += "Code Bubbles.";
   
      JLabel expl = new JLabel(explstr);
      pnl.addLabellessRawComponent("EXPLANATION",expl);
   
      progress_bar = new JProgressBar(0,100);
      progress_bar.setStringPainted(true);
      pnl.addRawComponent("Progress",progress_bar);
   
      task_label = new JLabel("Downloading ...");
      if (do_eclipse) task_label.setText("Getting Eclipse ...");
      pnl.addRawComponent("Current Task",task_label);
   
      pnl.addBottomButton("BACK","Back",this);
      continue_button = pnl.addBottomButton("CONTINUE","Finish",this);
      continue_button.setEnabled(false);
      pnl.addBottomButtons();
   
      Dimension sz = new Dimension(PROGRESS_WIDTH,PROGRESS_HEIGHT);
      pnl.setSize(sz);
      pnl.setPreferredSize(sz);
      
      if (do_eclipse) {
         eclipse_downloader = new EclipseDownloader(this);
         eclipse_downloader.start();
       }
      else {
         bubbles_downloader = new Downloader(this);
         bubbles_downloader.start();
       }
   
      return pnl;
    }

   @Override public boolean validate() {
      return all_done;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (evt.getActionCommand()) {
	 case "BACK" :
	    stopDownload();
	    moveToPanel(PanelType.ECLIPSE);
	    break;
	 case "CONTINUE" :
	    moveToPanel(PanelType.FINISH);
	    break;
       }
    }

   private void stopDownload() {
      if (eclipse_downloader != null) eclipse_downloader.interrupt();
      if (bubbles_downloader != null) bubbles_downloader.interrupt();
    }

   void setProgress(int pct) {
      progress_bar.setValue(pct);
     // progress_bar.setString(pct + "% downloaded");
    }

   void handleResult(boolean ok) {
      if (eclipse_downloader != null) {
         eclipse_downloader = null;
         if (ok) {
            task_label.setText("Downloading bubbles...");
            bubbles_downloader = new Downloader(this);
            bubbles_downloader.start();
          }
       }
      else {
         bubbles_downloader = null;
       }
      if (!ok) moveToPanel(PanelType.ECLIPSE); 
      else initializeBubbles();
    }

   private void initializeBubbles() {
      task_label.setText("Initializing the installaction");
   
      File f = new File(install_directory,"bubbles.jar");
      String java = IvyExecQuery.getJavaPath();
      ProcessBuilder bp = new ProcessBuilder(java,"-jar",f.getPath(),"-initialize");
      Map<String,String> env = bp.environment();
      if (eclipse_directory != null) {
         env.put("BUBBLES_ECLIPSE",eclipse_directory.getPath());
       }
   
      int sts = -1;
      try {
         Process p = bp.start();
         for ( ; ; ) {
            try {
               sts = p.waitFor();
               break;
             }
            catch (InterruptedException e) { }
          }
       }
      catch (IOException e) { }
      if (sts != 0) {
         f.delete();
         handleResult(false);
         return;
       }
      all_done = true;
      recheck();
    }

}	// end of inner class ProgressPanel



private class Downloader extends Thread {

   private ProgressPanel progress_panel;

   Downloader(ProgressPanel p) {
      super("BUBBLES_DOWNLOADER");
      progress_panel = p;
    }

   @Override public void run() {
      boolean remove = false;
      File f = new File(install_directory,"bubbles.jar");
      if (!install_directory.exists() && !install_directory.mkdirs()) {
         progress_panel.handleResult(false);
         return;
       }
      try {
         URI u = new URI(BUBBLES_URL);
         URLConnection conn = u.toURL().openConnection();
         BufferedInputStream ins = new BufferedInputStream(conn.getInputStream());
         try (OutputStream ots = new FileOutputStream(f)) {
            long len = 0;
            int pct = -1;
            byte [] buf = new byte[16384];
            for ( ; ; ) {
               if (isInterrupted()) {
                  remove = true;
                  break;
                }
               int rlen = ins.read(buf);
               if (rlen <= 0) break;
               ots.write(buf,0,rlen);
               len += rlen;
               int npct = (int)(len*100 / BUBBLES_LENGTH);
               if (npct > pct) progress_panel.setProgress(npct);
               pct = npct;
             }
          }
         ins.close();
       }
      catch (Throwable e) {
         remove = true;
       }
      if (remove) {
         f.delete();
         progress_panel.handleResult(false);
       }
      else progress_panel.handleResult(true);
    }

}	// end of inner class Downloader




/********************************************************************************/
/*										*/
/*	FinishPanel								*/
/*										*/
/********************************************************************************/

private class FinishPanel extends InstallerPanel implements ActionListener {

   FinishPanel() { }

   @Override protected JPanel getPanel() {
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Code Bubbles has been Installed");

      String explstr = "<html><p>";
      explstr += "Thank you for installing Code Bubbles.";
      JLabel expl = new JLabel(explstr);
      pnl.addLabellessRawComponent("EXPLANATION",expl);

      continue_button = pnl.addBottomButton("CONTINUE","Exit",this);
      continue_button.setEnabled(false);
      pnl.addBottomButtons();

      Dimension sz = new Dimension(PROGRESS_WIDTH,PROGRESS_HEIGHT);
      pnl.setSize(sz);
      pnl.setPreferredSize(sz);

      return pnl;
    }

   @Override protected boolean validate()	{ return true; }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (evt.getActionCommand()) {
         case "CONTINUE" :
            moveToPanel(PanelType.EXIT);
            break;
       }
    }

}	// end of inner class FinishPanel




/********************************************************************************/
/*										*/
/*	Image panel -- panel containing an image				*/
/*										*/
/********************************************************************************/

private static class SplashImage extends JPanel {

   private Image brown_image;
   private Image bubbles_image;

   SplashImage() {
      brown_image = loadImage(BROWN_IMAGE_PATH,BROWN_IMAGE_URL,"Brown LOGO");
      bubbles_image = loadImage(BUBBLES_IMAGE_PATH,BUBBLES_IMAGE_URL,"Bubbles LOGO");

      Dimension sz = new Dimension(SPLASH_WIDTH,SPLASH_HEIGHT);
      setSize(sz);
      setMinimumSize(sz);
      setPreferredSize(sz);
    }

   @Override public void paintComponent(Graphics g) {
      String copyr = "Copyright 2010 by Brown University.  All Rights Reserved";
      Graphics2D g2 = (Graphics2D) g;
      Paint p = new GradientPaint(0,0,Color.WHITE,0,SPLASH_HEIGHT,new Color(211,232,248));
      Rectangle r1 = new Rectangle(0,0,SPLASH_WIDTH,SPLASH_HEIGHT);
      g2.setPaint(p);
      g2.fill(r1);
      g2.setColor(new Color(88,88,88));
      r1.width -= 1;
      g2.draw(r1);
      g2.drawImage(brown_image,24,24,this);
      g2.drawImage(bubbles_image,64,96,this);
      g2.setPaint(new Color(0,0,0));
      Font f2 = new Font(Font.SANS_SERIF,Font.BOLD,20);
      g2.setFont(f2);
      g2.drawString("INSTALL CODE BUBBLES",80,200);
      Font f1 = new Font(Font.SANS_SERIF,Font.PLAIN,9);
      g2.setFont(f1);
      g2.setColor(new Color(88,88,88));
      g2.drawString(copyr,24,SPLASH_HEIGHT-24);
    }

   private Image loadImage(String path,String url,String id) {
      URL u1 = BemaInstaller.class.getResource(path);
      ImageIcon icn = null;
      if (u1 != null) {
	 icn = new ImageIcon(u1,id);
       }
      else {
	 try {
	    u1 = new URI(url).toURL();
	    icn = new ImageIcon(u1,id);
	  }
	 catch (MalformedURLException | URISyntaxException e) {
	    return null;
	  }
       }

      return icn.getImage();
    }

}	// end of inner class ImagePanel




}	// end of class BemaInstaller




/* end of BemaInstaller.java */

