/********************************************************************************/
/*										*/
/*		BoardEclipse.java						*/
/*										*/
/*	Handle eclipse installation for user					*/
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



package edu.brown.cs.bubbles.board;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingEditorPane;
import edu.brown.cs.ivy.swing.SwingGridPanel;

public class BoardEclipse implements BoardConstants
{



/********************************************************************************/
/*										*/
/*	Main program for installing eclipse					*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BoardEclipse be = new BoardEclipse(new File(args[0]));
   String rslt = be.installEclipse();
   if (rslt == null) System.exit(1);
   System.out.println(rslt);
   System.exit(0);
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File			install_directory;
private Map<String,String>	eclipse_versions;
private File			eclipse_directory;
private String			eclipse_version;

private static final String ECLIPSE_URL = "https://www.eclipse.org/downloads/download.php?" +
      "file=/technology/epp/downloads/release/$(V)/" +
      "R/eclipse-java-$(V)-R-$(OS).tar.gz";



private static final int ECLIPSE_WIDTH = 600;
private static final int ECLIPSE_HEIGHT = 300;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoardEclipse(File installdir)
{
   install_directory = installdir;
   eclipse_directory = null;
   eclipse_versions = null;
   eclipse_version = null;
}



/********************************************************************************/
/*										*/
/*	Installation routine							*/
/*										*/
/********************************************************************************/

String installEclipse()
{
   BoardLog.logI("BOARD","Installing eclipse");

   try {
      // if running directly from bubbles.jar, we need to restart with libraries
      Class.forName("org.apache.commons.compress.archivers.tar.TarArchiveEntry");
    }
   catch (ClassNotFoundException e) {
      return runInstaller();
    }

   if (!getEclipseVersions()) return null;

   EclipsePanel epnl = new EclipsePanel();
   JDialog installdialog = new JDialog((JFrame) null,"Eclipse Installation",false);
   installdialog.setContentPane(epnl.getPanel());
   installdialog.pack();
   installdialog.setLocationRelativeTo(null);
   installdialog.setVisible(true);

   for ( ; ; ) {
      Boolean sts = epnl.getStatus();
      if (sts == null) continue;
      installdialog.setVisible(false);
      if (sts == Boolean.FALSE) return null;
      return eclipse_directory.getAbsolutePath();
    }
}




/********************************************************************************/
/*										*/
/*	Run as a separate process						*/
/*										*/
/********************************************************************************/

private String runInstaller()
{
   File bbljar = new File(install_directory,"bubbles.jar");
   File lib = new File(install_directory,"lib");
   String cp = bbljar.getAbsolutePath();
   File l0 = new File(lib,"ivy.jar");
   File l2 = new File(lib,"commons-compress.jar");
   cp += File.pathSeparator + l0.getAbsolutePath();
   cp += File.pathSeparator + l2.getAbsolutePath();

   List<String> argl = new ArrayList<>();
   argl.add(IvyExecQuery.getJavaPath());
   argl.add("-Xmx256m");
   argl.add("-cp");
   argl.add(cp);
   argl.add("edu.brown.cs.bubbles.board.BoardEclipse");
   argl.add(install_directory.getAbsolutePath());

   System.err.print("Run: ");
   for (String s : argl) System.err.print(" " + s);
   System.err.println();

   try {
      File f = File.createTempFile("eclipsedirectory",".txt");
      f.deleteOnExit();
      ProcessBuilder pb = new ProcessBuilder(argl);
      pb.redirectOutput(f);
      pb.redirectError(ProcessBuilder.Redirect.INHERIT);
      Process p = pb.start();
      int sts = p.waitFor();

      if (sts == 0) {
	 BufferedReader fr = new BufferedReader(new FileReader(f));
	 String rslt = fr.readLine();
	 fr.close();
	 if (rslt != null && rslt.length() > 0) return rslt;
       }
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem running eclipse installer",e);
    }
   catch (InterruptedException e) { }

   return null;
}



/********************************************************************************/
/*										*/
/*	Get Eclipse versions							*/
/*										*/
/********************************************************************************/

private boolean getEclipseVersions()
{
   eclipse_versions = new LinkedHashMap<>();
   addEclipseVersion("2021-03");
   addEclipseVersion("2020-12");
   addEclipseVersion("2020-09");
   addEclipseVersion("2020-06");
   addEclipseVersion("2020-03");

   return true;
}


private void addEclipseVersion(String v)
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
      if (ar.contains("64")) id = "macosx-cocoa-x86_64";
    }
   if (id == null) return;

   Map<String,String> info = new HashMap<>();
   info.put("V",v);
   info.put("OS",id);
   String urltxt = IvyFile.expandName(ECLIPSE_URL,info);

   try {
      URL url = new URL(urltxt);
      URLConnection uc = url.openConnection();
      long cll = uc.getContentLengthLong();
      if (cll <= 0) return;
    }
   catch (Exception e) {
      BoardLog.logE("BOARD","Problem testing eclipse url " + urltxt,e);
      return;
    }

   eclipse_versions.put(v,urltxt);
}



/********************************************************************************/
/*										*/
/*	Eclipse panel -- panel for checking eclipse installation		*/
/*										*/
/********************************************************************************/

private class EclipsePanel implements ActionListener, UndoableEditListener {

   private JTextField eclipse_field;
   private SwingComboBox<String> version_choice;
   private JButton install_button;
   private Boolean result_status;

   EclipsePanel() {
      eclipse_field = null;
      version_choice = null;
      install_button = null;
      result_status = null;
    }

   JPanel getPanel() {
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.setBackground(BoardColors.getColor("Buda.Bubbles.Color"));
      pnl.setOpaque(true);
      pnl.beginLayout();
      pnl.addBannerLabel("Install Eclipse");
   
      File pfl = new File(install_directory,"eclipse");
   
      String explstr = "<html><p>";
      explstr += "Code Bubbles requires a writable Eclipse installation ";
      explstr += "in order to work for Java programs.  It is not needed if ";
      explstr += "all you are going to do is Code Bubbles for Node.JS or ";
      explstr += "Code Bubbles for Python.";
      explstr += "<p><p>You can a) specify the path of your current Eclipse ";
      explstr += "installation into which the Code Bubbles plugin will be installed; ";
      explstr += "request a new Eclipse installation just for Code Bubbles; or skip ";
      explstr += "this step if you want to install your own Eclispe or aren't going ";
      explstr += "to be working with Java.";
      explstr += "<p><p>All Eclipse downloads are provided under the terms and conditions  ";
      explstr += "of the <a href='https://www.eclipse.org/legal/epl/notice.php'>";
      explstr += "Eclipse Foundation Software User Agreement</a>.";
      explstr += "<p><p>";
   
      JEditorPane expl = new SwingEditorPane("text/html",explstr);
      expl.setEditable(false);
      expl.addHyperlinkListener(new HyperListener());
      expl.setBackground(BoardColors.getColor("Buda.Bubbles.Color"));
      pnl.addLabellessRawComponent("EXPLANATION",expl);
      eclipse_field = pnl.addFileField("Eclipse Home",pfl,JFileChooser.DIRECTORIES_ONLY,this,this);
      version_choice = pnl.addChoice("Install Eclipse Version",eclipse_versions.keySet(),0,this);
   
      pnl.addBottomButton("CANCEL","CANCEL",this);
      install_button = pnl.addBottomButton("INSTALL","INSTALL",this);
      install_button.setEnabled(false);
      pnl.addBottomButtons();
   
      Dimension sz = new Dimension(ECLIPSE_WIDTH,ECLIPSE_HEIGHT);
      pnl.setSize(sz);
      pnl.setPreferredSize(sz);
   
      recheck();
   
      return pnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand().toUpperCase();
      switch (cmd) {
	 case "INSTALL" :
	    EclipseDownloader ecl = new EclipseDownloader(this);
	    ecl.start();
	    break;
	 case "CANCEL" :
	    handleResult(false);
	    break;
	 default :
	    recheck();
	    break;
       }
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      recheck();
    }

   synchronized Boolean getStatus() {
      if (result_status == null) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
      return result_status;
    }

   synchronized void handleResult(boolean sts) {
      if (!sts) {
	 eclipse_directory = null;
       }
      result_status = sts;
      notifyAll();
    }

   private void recheck() {
      if (install_button != null) {
	 boolean fg = validate();
	 install_button.setEnabled(fg);
       }
    }

   private boolean validate() {
      eclipse_directory = null;
      eclipse_version = null;
      File dirp = new File(eclipse_field.getText());
      int vidx = version_choice.getSelectedIndex();
      if (vidx < 0) vidx = 0;
      eclipse_version = version_choice.getItemAt(vidx);
      if (dirp.exists()) {
	 if (!dirp.isDirectory()) return false;
	 File [] files = dirp.listFiles();
	 if (files != null && files.length > 0) {
	    if (!BoardSetup.checkEclipseDirectory(dirp)) return false;
	  }
       }
      eclipse_directory = dirp;
      return true;
    }

}	// end of inner class EclipsePanel




/********************************************************************************/
/*										*/
/*	Eclipse downloader							*/
/*										*/
/********************************************************************************/

private class EclipseDownloader extends Thread {

   private EclipsePanel for_panel;

   EclipseDownloader(EclipsePanel pnl) {
      super("ECLIPSE_DOWNLOADER");
      for_panel = pnl;
    }

   @Override public void run() {

      File f = eclipse_directory.getParentFile();
      if (!f.exists() && !f.mkdirs()) {
	 JOptionPane.showMessageDialog(null,"Can't create installation directory " + f,
	       "Eclipse Installation Problem",JOptionPane.ERROR_MESSAGE);
	 for_panel.handleResult(false);
	 return;
       }

      JOptionPane prog = new JOptionPane("Installing Eclipse " + eclipse_version + " into " + eclipse_directory,
	    JOptionPane.INFORMATION_MESSAGE,JOptionPane.DEFAULT_OPTION);
      prog.setBackground(BoardColors.getColor("Buda.Bubbles.Color"));
      JDialog dlg = prog.createDialog(null,"Installing Eclipse...");
      dlg.setModal(false);
      BoardColors.setColors(dlg,"Buda.Bubbles.Color");
      dlg.setVisible(true);

      String urltxt = eclipse_versions.get(eclipse_version);

      System.err.println("BOARD: Installing eclipse from " + urltxt);

      if (urltxt != null) {
	 try {
	    URL url = new URL(urltxt);
	    URLConnection uc = url.openConnection();
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
	    dlg.setVisible(false);
	    for_panel.handleResult(true);
	    return;
	  }
	 catch (InterruptedException e) {
	    dlg.setVisible(false);
	    BoardLog.logE("BOARD","Problem installing",e);
	    JOptionPane.showMessageDialog(null,e.getMessage(),
		  "Eclipse Installation Problem",JOptionPane.ERROR_MESSAGE);
	    try {
	       IvyFile.remove(eclipse_directory);
	     }
	    catch (IOException ex) { }
	  }
	 catch (IOException e) {
	    dlg.setVisible(false);
	    BoardLog.logE("BOARD","Problem installing from " + urltxt,e);
	    JOptionPane.showMessageDialog(null,e.getMessage(),
		  "Eclipse Installation Problem",JOptionPane.ERROR_MESSAGE);
	  }
       }
      dlg.setVisible(false);
      for_panel.handleResult(false);
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

}	// end of inner class EclipseDownloader



/********************************************************************************/
/*										*/
/*	Hyper link listener							*/
/*										*/
/********************************************************************************/

private class HyperListener implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	 URL u = e.getURL();
	 if (u == null) {
	    String d = e.getDescription();
	    int idx = d.indexOf(":");
	    if (idx < 0) return;
	    String proto = d.substring(0,idx);
	    HyperlinkListener hl = BudaRoot.getListenerForProtocol(proto);
	    if (hl != null) {
	       hl.hyperlinkUpdate(e);
	     }
	    return;
	  }

	 try {
	    Desktop.getDesktop().browse(u.toURI());
	  }
	 catch (IOException ex) { }
	 catch (URISyntaxException ex) { }
       }
    }

}	// end of inner class HyperListener


}	// end of class BoardEclipse




/* end of BoardEclipse.java */

