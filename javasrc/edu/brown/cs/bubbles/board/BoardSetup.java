/********************************************************************************/
/*										*/
/*		BoardSetup.java 						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingSetup;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 *	This class handles ensuring that bubbles is set up correctly.  If it isn't
 *	(i.e. bubbles is being run for the first time), then it interactively queries
 *	the user for the proper directories and does the appropriate setup.  It can
 *	be run either stand-alone or directly from any bubbles application.
 *
 **/

public class BoardSetup implements BoardConstants, MintConstants {




/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

/**
 *	Entry point for running bubbles setup stand-alone.
 **/

public static void main(String [] args)
{
   new SwingSetup();

   BoardSetup bs = new BoardSetup(args);
   bs.doSetup();

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties system_properties;
private boolean 	force_setup;
private boolean 	force_metrics;
private boolean 	ask_workspace;
private boolean 	workspace_given;
private boolean 	run_foreground;
private boolean 	has_changed;
private String		install_path;
private boolean 	install_jar;
private String		jar_file;
private String		jar_directory;
private String		eclipse_directory;
private String		default_workspace;
private boolean 	create_workspace;
private boolean 	auto_update;
private boolean 	do_uninstall;
private int		setup_count;
private boolean 	update_setup;
private boolean 	must_restart;
private boolean 	show_splash;
private boolean 	allow_debug;
private boolean 	use_lila;
private List<String>	java_args;
private BoardSplash	splash_screen;
private long		run_size;
private String		update_proxy;
private RunMode 	run_mode;
private String		mint_name;
private MintControl	mint_control;
private String		course_name;
private String		course_assignment;
private File		library_dir;
private BoardLanguage	board_language;
private List<String>	recent_workspaces;
private boolean 	palette_set;


private static BoardSetup	board_setup = null;

private static final long MIN_MEMORY = 1024*1024*1024;


private static String [] ivy_props = new String [] {
   "IVY", "edu.brown.cs.IVY", "edu.brown.cs.ivy.IVY", "BROWN_IVY_IVY"
};

private static String [] ivy_env = new String [] {
   "IVY", "BROWN_IVY_IVY", "BROWN_IVY"
};

private static final String RECENT_HEADER = "<< Select Recent Workspace >>";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Constructor for setting up an instance of the setup module with default values.
 *	This is the setup that should be done for various bubbles applications.
 **/

public static synchronized BoardSetup getSetup()
{
   if (board_setup == null) {
      board_setup = new BoardSetup();
    }
   return board_setup;
}



private BoardSetup()
{
   system_properties = BoardProperties.getProperties("System");

   force_setup = false;
   force_metrics = false;

   install_path = system_properties.getProperty(BOARD_PROP_INSTALL_DIR);
   String isp = System.getProperty("edu.brown.cs.bubbles.INSTALLDIR");
   if (isp != null) install_path = isp;

   auto_update = (System.getProperty("edu.brown.cs.bubbles.NO_UPDATE") == null);
   auto_update = system_properties.getBoolean(BOARD_PROP_AUTO_UPDATE,auto_update);
   install_jar = false;
   jar_directory = null;
   jar_file = null;
   do_uninstall = false;
   must_restart = false;
   update_setup = false;
   show_splash = true;
   splash_screen = null;
   allow_debug = false;
   use_lila = false;
   run_mode = RunMode.NORMAL;
   mint_name = null;
   mint_control = null;
   course_name = null;
   course_assignment = null;
   library_dir = null;
   board_language = BoardLanguage.JAVA;
   palette_set = false;

   eclipse_directory = system_properties.getProperty(BOARD_PROP_ECLIPSE_DIR);
   if (eclipse_directory == null) eclipse_directory = System.getProperty("edu.brown.cs.bubbles.eclipse");
   if (eclipse_directory == null) eclipse_directory = System.getenv("BUBBLES_ECLIPSE");

   default_workspace = system_properties.getProperty(BOARD_PROP_ECLIPSE_WS);
   create_workspace = false;
   ask_workspace = system_properties.getBoolean(BOARD_PROP_ECLIPSE_ASK_WS,true);
   workspace_given = false;
   run_foreground = system_properties.getBoolean(BOARD_PROP_ECLIPSE_FOREGROUND,false);
   run_foreground = false;		// force it to be false for now

   recent_workspaces = new ArrayList<String>();
   String oldws = system_properties.getProperty(BOARD_PROP_RECENT_WS);
   if (oldws != null) {
      StringTokenizer tok = new StringTokenizer(oldws,";");
      while (tok.hasMoreTokens()) {
	 recent_workspaces.add(tok.nextToken());
       }
    }

   BoardProperties bp = BoardProperties.getProperties("Board");
   update_proxy = bp.getProperty("Board.update.proxy");
   if (update_proxy != null && update_proxy.length() < 2) update_proxy = null;

   if (!checkWorkspace()) default_workspace = null;

   run_size = Runtime.getRuntime().maxMemory();
   run_size = system_properties.getLong(BOARD_PROP_JAVA_VM_SIZE,run_size);
   if (run_size < MIN_MEMORY) run_size = MIN_MEMORY;

   java_args = null;

   has_changed = false;

   String cnm = null;
   if (cnm == null) cnm = System.getProperty("edu.brown.cs.bubbles.COURSE");
   if (cnm == null) cnm = System.getenv("BUBBLES_COURSE");
   if (cnm != null) setCourseName(cnm);

   setup_count = 0;
}



private BoardSetup(String [] args)
{
   this();

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   List<String> javaargs = null;

   for (int i = 0; i < args.length; ++i) {
      if (javaargs != null) {
	 javaargs.add(args[i]);
       }
      else if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-f")) {                                // -force
	    force_setup = true;
	    force_metrics = true;
	  }
	 else if (args[i].startsWith("-course") && i+1 < args.length) { // -course <name[@assign]>
	    setCourseName(args[++i]);
	  }
	 else if (args[i].startsWith("-c")) {                           // -collect
	    force_metrics = true;
	  }
	 else if (args[i].startsWith("-E") && i+1 < args.length) {      // -Eclipse <eclipse install directory>
	    eclipse_directory = args[++i];
	    has_changed = true;
	  }
	 else if (args[i].startsWith("-B") && i+1 < args.length) {      // -Bubbles <bubbles install directory>
	    install_path = args[++i];
	    has_changed = true;
	  }
	 else if (args[i].startsWith("-w") && i+1 < args.length) {      // -workspace <workspace>
	    default_workspace = args[++i];
	    has_changed = true;
	  }
	 else if (args[i].startsWith("-u")) {                           // -update
	    update_setup = true;
	  }
	 else if (args[i].startsWith("-U")) {                           // -Uninstall
	    do_uninstall = true;
	  }
	 else if (args[i].startsWith("-nosp")) {                        // -nosplash
	    show_splash = false;
	  }
	 else if (args[i].startsWith("-java")) {                        // -java
	    setLanguage(BoardLanguage.JAVA);
	  }
	 else if (args[i].startsWith("-py")) {                          // -python
	    setLanguage(BoardLanguage.PYTHON);
	  }
	 else if (args[i].startsWith("-node")) {                        // -node
	    setLanguage(BoardLanguage.JS);
	  }
	 else if (args[i].startsWith("-js")) {                          // -js
	    setLanguage(BoardLanguage.JS);
	  }
	 else if (args[i].startsWith("-JS")) {                          // -JS
	    setLanguage(BoardLanguage.JS);
	  }
	 else if (args[i].startsWith("-rebus")) {                       // -rebus
	    setLanguage(BoardLanguage.REBUS);
	  }
	 else if (args[i].startsWith("-p") && i+1 < args.length) {      // -prop <propdir>
	    BoardProperties.setPropertyDirectory(args[++i]);
	  }
	 else if (args[i].startsWith("-C")) {                           // -Client
	    run_mode = RunMode.CLIENT;
	  }
	 else if (args[i].startsWith("-S")) {                           // -Server
	    run_mode = RunMode.SERVER;
	  }
	 else if (args[i].startsWith("-X")) {                           // -X <run args...>
	    javaargs = new ArrayList<String>();
	  }
	 else badArgs();
       }
      else badArgs();
    }

   if (javaargs != null) setJavaArgs(javaargs);
}




private void badArgs()
{
   System.err.println("BOARD: setup [-E eclipse_dir] [-B bubbles_dir] [-w workspace]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 *	Set the flag to force a new setup.
 **/

public void setForceSetup(boolean fg)
{
   force_setup = fg;
   if (fg) force_metrics = true;
}


public void setForceMetrics(boolean fg)
{
   force_metrics = fg;
}



/**
 *	Set the flag to skip setup.  This should only be done if the caller knows
 *	that eclipse, the workspace, and the bubbles directory are all defined
 *	correctly.  It is used primarily for restarting bubbles.
 **/

public void setSkipSetup()
{
   setup_count = -1;
}


/**
 *	Skip the splash screen.
 **/

public void setSkipSplash()
{
   show_splash = false;
}


/**
 *	Allow remote debugging of bubbles
 **/

public void setAllowDebug()
{
   allow_debug = true;
}



/**
 *	Runs with lila monitor
 **/

public void setUseLila()
{
   use_lila = true;
}


/**
 *	Set course name
 **/

public void setCourseName(String nm)
{
   course_name = null;
   course_assignment = null;
   auto_update = false;

   if (nm != null && nm.length() > 0) {
      int idx = nm.indexOf("@");
      if (idx > 1) {
	 course_assignment = nm.substring(idx+1);
	 if (course_assignment.length() == 0) course_assignment = null;
	 course_name = nm.substring(0,idx);
       }
      else {
	 course_name = nm;
       }
    }
}


/**
 *	Get course name if any is associated with the session, null otherwise.
 **/

public String getCourseName()
{
   return course_name;
}



public String getCourseAssignment()
{
   return course_assignment;
}



/**
 *	Set the default Eclipse workspace.  This is used to let the user specify
 *	the workspace on the command line
 **/

public void setDefaultWorkspace(String ws)
{
   File fws = new File(ws);
   if (fws.exists() && checkWorkspaceDirectory(fws,false)) ws = fws.getAbsolutePath();
   else if (!fws.isAbsolute()) {
      String nm = fws.getPath();
      for (String rct : recent_workspaces) {
	 if (rct.endsWith(nm)) {
	    File f4 = new File(rct);
	    ws = f4.getAbsolutePath();
	    fws = null;
	    break;
	  }
       }
      if (fws != null) {
	 for (String rct : recent_workspaces) {
	    File f1 = new File(rct);
	    File f2 = f1.getParentFile();
	    File f3 = new File(f2,ws);
	    if (f3.exists() && f3.isDirectory()) {
	       ws = f3.getAbsolutePath();
	       break;
	     }
	  }
       }
    }

   default_workspace = ws;
   if (!checkWorkspace()) default_workspace = null;
   else {
      has_changed = true;
      workspace_given = true;
    }
}


public void setCreateWorkspace(String ws)
{
   create_workspace = true;
   if (ws != null) default_workspace = ws;
   else has_changed = true;
}


public void setAutoUpdate(boolean fg)
{
   auto_update = fg;
}


/**
 * Returns the workspace this bubbles instance is using
 * @return
 */
public String getDefaultWorkspace()
{
   if (getRunMode() == RunMode.CLIENT) {
      String nm = system_properties.getString("Cloud.workspace");
      if (nm != null) return nm;
    }

   return default_workspace;
}


/**
 *	Set flat to ask about a workspace
 **/
public void setAskWorkspace(boolean fg)
{
   ask_workspace = fg;
   if (!fg) has_changed = false;
   if (fg) workspace_given = false;
}


/**
 *	Set the java arguments to be used when restarting the system.  This is used
 *	to ensure that restart uses the same arguments as the original start.
 **/

public void setJavaArgs(Collection<String> args)
{
   java_args = new ArrayList<String>();
   if (args != null) java_args.addAll(args);
}



/**
 *	Set the java arguments to be used when restarting the system.  This is used
 *	to ensure that restart uses the same arguments as the original start.
 **/

public void setJavaArgs(String [] args)
{
   java_args = new ArrayList<>();
   if (args != null) for (String s : args) java_args.add(s);
}



/**
 *	Returns the file location of the bubbles configuration file which is generally
 *	stored in the eclipse workspace.
 **/

public static File getConfigurationFile()
{
   BoardSetup bs = getSetup();

   File f1 = BoardSetup.getPropertyBase();
   String wsname = bs.default_workspace;
   if (wsname == null) wsname = bs.getDefaultWorkspace();
   wsname = wsname.replaceAll("\\W","_");
   wsname += "_";
   File f2 = new File(f1,"configurations");
   File f3 = new File(f2,wsname + BOARD_CONFIGURATION_FILE);

   File wsd = new File(bs.default_workspace);
   File f4 = new File(wsd,BOARD_CONFIGURATION_FILE);

   if (!f3.exists() && wsd.exists() && !bs.isClientMode()) return f4;
   f2.mkdirs();

   return f3;
}



/**
 *	Returns the file location of the bubbles history configuration file which is
 *	usually stored in the eclipse workspace.  This file contains history information
 *	such as prior group memberships and the task shelf.
 **/

public static File getHistoryFile()
{
   BoardSetup bs = getSetup();

   File f1 = BoardSetup.getPropertyBase();
   String wsname = bs.default_workspace;
   wsname = wsname.replaceAll("\\W","_");
   wsname += "_";
   File f2 = new File(f1,"configurations");
   File f3 = new File(f2,wsname + BOARD_HISTORY_FILE);

   File wsd = new File(bs.default_workspace);
   File f4 = new File(wsd,BOARD_HISTORY_FILE);

   if (!f3.exists() && wsd.exists()) return f4;
   f2.mkdirs();

   return f3;
}



/**
 *	Returns the file location of the bubbles documentation configruation file that is
 *	usually stored in the eclipse workspace.  This file contains history information
 *	such as prior group memberships and the task shelf.
 **/

public static File getDocumentationFile()
{
   BoardSetup bs = getSetup();

   if (bs.getRunMode() == RunMode.CLIENT) {
      File f1 = BoardSetup.getPropertyBase();
      File f2 = new File(f1,"documentation");
      f2.mkdirs();
      File f3 = new File(f2,BOARD_DOCUMENTATION_FILE);
      return f3;
    }

   File wsd = new File(bs.default_workspace);
   if (!wsd.exists()) wsd.mkdirs();

   if (!wsd.exists() || !wsd.isDirectory()) {
      return null;
    }

   return new File(wsd,BOARD_DOCUMENTATION_FILE);
}



/**
 *	Returns the file location of the bubbles plugin directory.  This is where we should
 *	be saving eclipse-specific information
 **/

public static File getBubblesPluginDirectory()
{
   BoardSetup bs = getSetup();

   File wsd = new File(bs.default_workspace);

   if (!wsd.exists() || !wsd.isDirectory()) {
      BoardLog.logX("BOARD","Bad board setup " + bs.default_workspace + " " + bs.eclipse_directory + " " +
		       bs.jar_file + " " + bs.jar_directory + " " + bs.install_path + " " +
		       bs.install_jar);
    }


   File pdir = null;

   switch (bs.board_language) {
      default:
      case JAVA :
	 File f1 = new File(wsd,".metadata");
	 File f2 = new File(f1,".plugins");
	 pdir = new File(f2,"edu.brown.cs.bubbles.bedrock");
	 break;
      case PYTHON :
	 pdir = new File(wsd,".metadata");
	 break;
      case JS :
	 pdir = new File(wsd,".metadata");
	 break;
      case REBUS :
	 pdir = new File(wsd,".metadata");
	 break;
    }

   if (!pdir.exists()) pdir.mkdirs();
   if (!pdir.exists() || !pdir.isDirectory()) {
      BoardLog.logE("BOARD","Bad plugin directory " + pdir);
    }

   return pdir;
}



/**
 *	Returns the file location of the bubbles working directory.  This is where we should
 *	be saving various files and auxilliary information.
 **/

public static File getBubblesWorkingDirectory()
{
   return getBubblesWorkingDirectory(null);
}


public static File getBubblesWorkingDirectory(File wsd)
{
   BoardSetup bs = getSetup();

   if (wsd == null && bs.default_workspace != null) wsd = new File(bs.default_workspace);

   if (wsd == null || !wsd.exists() || !wsd.isDirectory()) {
      BoardLog.logX("BOARD","Bad board setup " + bs.default_workspace + " " + bs.eclipse_directory + " " +
		       bs.jar_file + " " + bs.jar_directory + " " + bs.install_path + " " +
		       bs.install_jar);
    }

   File f3 = new File(wsd,".bubbles");

   if (!f3.exists()) f3.mkdirs();
   if (!f3.exists() || !f3.isDirectory()) {
      BoardLog.logE("BOARD","Bad plugin directory " + f3);
    }

   return f3;
}



/**
 *	Get a string describing this version of bubbles.
 **/

public static String getVersionData()
{
   return BoardUpdate.getVersionData();
}



/**
 *	Get property base directory (~/.bubbles)
 **/

public static File getPropertyBase()
{
   return BoardProperties.getPropertyDirectory();
}



void setRunSize(long sz)
{
   run_size = sz;
}



/**
 *	Return the library resource path
 **/

public String getLibraryPath(String item)
{
   File f = getLibraryDirectory();
   if (f == null) return null;

   f = new File(f,item);
   if (!f.exists()) {
      if (item.equals("eclipsejar")) return getLibraryDirectory().getPath();
    }

   return f.getPath();
}



public String getRemoteLibraryPath(String item)
{
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();

   File f = getRemoteLibraryDirectory();

   if (f == null) return null;
   f = fsv.createFileObject(f,item);
   // f = new File(f,item);

   if (!f.exists()) {
      if (item.equals("eclipsejar")) return getRemoteLibraryDirectory().getPath();
    }

   return f.getPath();
}


public String getBinaryPath(String item)
{
   File f = getLibraryDirectory();
   if (f == null) {
      checkInstall();
      f = getLibraryDirectory();
      if (f == null) return null;
    }
   File f1 = f.getParentFile();
   File f2 = new File(f1,BOARD_INSTALL_BINARY);
   File f3 = new File(f2,item);
   return f3.getPath();
}



public String getEclipsePath()
{
   StringBuffer buf = new StringBuffer();
   int fnd = 0;
   String ejp = getLibraryPath("eclipsejar");
   File ejr = new File(ejp);
   BoardLog.logD("BOARD","TRY1 ECLIPSE AT " + ejr);
   if (ejr.exists() && ejr.isDirectory()) {
      for (File nfil : ejr.listFiles()) {
	 if (nfil.getName().startsWith("org.eclipse.") && nfil.getName().endsWith(".jar")) {
	    if (fnd > 0) buf.append(File.pathSeparator);
	    buf.append(nfil.getPath());
	    ++fnd;
	  }
       }
    }
   if (fnd == 0) {
      File f1 = ejr.getParentFile().getParentFile();	// /pro/bubbles/lib --> /pro
      File f2 = new File(f1,"ivy");                      // /pro/ivy
      File f3 = new File(f2,"lib");                      // /pro/ivy/lib
      File f4 = new File(f3,"eclipsejar");
      BoardLog.logD("BOARD","TRY2 ECLIPSE AT " + f4);
      if (f4.exists() && f4.isDirectory()) {
	 for (File nfil : f4.listFiles()) {
	    if (nfil.getName().startsWith("org.eclipse.") && nfil.getName().endsWith(".jar")) {
	       if (fnd > 0) buf.append(File.pathSeparator);
	       buf.append(nfil.getPath());
	       ++fnd;
	     }
	  }
       }
    }

   if (fnd == 0) {
      File f1 = ejr.getParentFile().getParentFile();	// /pro/bubbles/lib --> /pro
      File f2 = new File(f1,"ivy");                      // /pro/ivy
      File f3 = new File(f2,"lib");                      // /pro/ivy/lib
      File f4 = new File(f3,"eclipsejar");
      BoardLog.logD("BOARD","TRY3 ECLIPSE AT " + f4);
      if (f4.exists() && f4.isDirectory()) {
	 for (File nfil : f4.listFiles()) {
	    if (nfil.getName().startsWith("org.eclipse.") && nfil.getName().endsWith(".jar")) {
	       if (fnd > 0) buf.append(File.pathSeparator);
	       buf.append(nfil.getPath());
	       ++fnd;
	     }
	  }
       }
    }

   if (fnd == 0) {
      String xcp = System.getProperty("java.class.path");
      StringTokenizer ptok = new StringTokenizer(xcp,File.pathSeparator);
      while (ptok.hasMoreTokens()) {
	 String pelt = ptok.nextToken().trim();
	 if (pelt.contains("org.eclipse.")) {
	    if (fnd > 0) buf.append(File.pathSeparator);
	    buf.append(pelt);
	    ++fnd;
	  }
       }
    }

   if (fnd == 0) return null;

   return buf.toString();
}








/**
 *	Return the directory for the current course
 **/

public File getCourseDirectory()
{
   if (course_name == null) return null;

   File f = null;

   String suds = System.getProperty("edu.brown.cs.bubbles.suds");
   if (suds == null) suds = System.getenv("BUBBLES_SUDS");
   if (suds != null) {
      f = new File(suds);
      if (!f.exists() || !f.isDirectory()) f = null;
    }

   if (f == null && install_jar && jar_directory != null) {
      f = new File(jar_directory);
    }
   else if (install_path != null) {
      f = new File(install_path);
      f = new File(f,"suds");
    }
   else return null;

   File f1 = f.getParentFile();
   File f2 = new File(f1,course_name);
   if (!f2.exists()) f2 = new File(f,course_name);
   if (!f.exists()) return null;

   return f2;
}



public File getRootDirectory()
{
   File f = null;

   if (install_jar && jar_directory != null) {
      f = new File(jar_directory);
    }
   else if (install_path != null) {
      f = new File(install_path);
    }
   else return null;

   return f;
}



public File getLibraryDirectory()
{
   if (library_dir != null) return library_dir;

   File f = null;

   if (install_jar && jar_directory != null) {
      f = new File(jar_directory);
    }
   else if (install_path != null) {
      f = new File(install_path);
    }
   else return null;

   f = new File(f,BOARD_INSTALL_LIBRARY);

   if (!f.exists()) f.mkdir();
   if (!f.exists() || !f.isDirectory()) {
      File f1 = BoardProperties.getPropertyDirectory();
      File f2 = new File(f1,BOARD_INSTALL_LIBRARY);
      f2.mkdirs();
      if (f2.exists() && f2.isDirectory()) f = f2;
    }

   library_dir = f;

   return f;
}


public File getRemoteLibraryDirectory()
{
   switch (getRunMode()) {
      case SERVER :
      case NORMAL :
	 return getLibraryDirectory();
      case CLIENT :
	 break;
    }

   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   if (fsv instanceof BoardFileSystemView) {
      BoardFileSystemView bfsv = (BoardFileSystemView) fsv;
      return bfsv.getRemoteLibraryDirectory();
    }

   return getLibraryDirectory();
}



/**
 *	Set the langauge being worked on
 **/

public void setLanguage(BoardLanguage bl)
{
   if (bl == null) return;

   board_language = bl;

   String pb = System.getProperty("edu.brown.cs.bubbles.BASE");
   if (pb == null) {
      switch (bl) {
	 case PYTHON :
	    BoardProperties.setPropertyDirectory(BOARD_PYTHON_PROP_BASE);
	    break;
	 case JAVA :
	    if (course_name != null) BoardProperties.setPropertyDirectory(BOARD_SUDS_PROP_BASE);
	    BoardProperties.setPropertyDirectory(BOARD_PROP_BASE);
	    break;
	 case JS :
	    BoardProperties.setPropertyDirectory(BOARD_NODEJS_PROP_BASE);
	    break;
	 case REBUS :
	    BoardProperties.setPropertyDirectory(BOARD_REBUS_PROP_BASE);
	    break;
       }
    }

   system_properties = BoardProperties.getProperties("System");
   auto_update = system_properties.getBoolean(BOARD_PROP_AUTO_UPDATE,auto_update);
   eclipse_directory = system_properties.getProperty(BOARD_PROP_ECLIPSE_DIR,eclipse_directory);
   default_workspace = system_properties.getProperty(BOARD_PROP_ECLIPSE_WS,default_workspace);
   ask_workspace = system_properties.getBoolean(BOARD_PROP_ECLIPSE_ASK_WS,true);
   run_foreground = system_properties.getBoolean(BOARD_PROP_ECLIPSE_FOREGROUND,false);
   run_foreground = false;		// force it to be false for now
}


/**
 *	Return the current language.
 **/

public BoardLanguage getLanguage()
{
   return board_language;
}



/********************************************************************************/
/*										*/
/*	Splash screen methods							*/
/*										*/
/********************************************************************************/

public void startSplash()
{
   if (splash_screen == null) {
      splash_screen = new BoardSplash();
      splash_screen.start();
    }
}


/**
 *	Set the current task to be displayed at the bottom of the splash dialog.
 **/

public void setSplashTask(String id)
{
   if (splash_screen != null) splash_screen.setCurrentTask(id);
}



/**
 *	Set the % done to be displayed at the bottom of the splash dialog.  This
 *	does nothing for now.
 **/

public void setSplashPercent(int v)
{
   if (splash_screen != null) splash_screen.setPercentDone(v);
}



/**
 *	Indicate that setup is complete by removing the splash dialog.
 **/

public void removeSplash()
{
   if (splash_screen != null) {
      splash_screen.remove();
      splash_screen = null;
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle client-server modes					*/
/*										*/
/********************************************************************************/

public RunMode getRunMode()			{ return run_mode; }
public boolean isServerMode()			{ return run_mode == RunMode.SERVER; }
public boolean isClientMode()			{ return run_mode == RunMode.CLIENT; }

public void setRunMode(RunMode rm)		{ run_mode = rm; }




/********************************************************************************/
/*										*/
/*	Mint access methods							*/
/*										*/
/********************************************************************************/

/**
 *	Return name of the mint connection
 **/
public String getMintName()
{
   setupMint();
   return mint_name;
}


/**
 *	Return the mint handle
 **/
public MintControl getMintControl()
{
   setupMint();
   return mint_control;
}


private synchronized void setupMint()
{
   if (mint_name != null) return;

   mint_name = System.getProperty("edu.brown.cs.bubbles.MINT");
   if (mint_name == null) mint_name = System.getProperty("edu.brown.cs.bubbles.mint");
   if (mint_name == null) mint_name = system_properties.getProperty("edu.brown.cs.bubbles.MINT");
   if (mint_name == null) mint_name = system_properties.getProperty("edu.brown.cs.bubbles.mint");
   if (mint_name == null) {
      mint_name = BOARD_MINT_NAME;
      String wsname = default_workspace;
      int idx = wsname.lastIndexOf(File.separator);
      if (idx > 0) wsname = wsname.substring(idx+1);
      if (wsname == null) wsname = "";
      else wsname = wsname.replace(" ","_");
      mint_name = mint_name.replace("@@@",wsname);
    }

   String mport = system_properties.getProperty(BOARD_PROP_MINT_MASTER_PORT);
   if (mport != null) System.setProperty("edu.brown.cs.ivy.mint.master.port",mport);
   String sport = system_properties.getProperty(BOARD_PROP_MINT_SERVER_PORT);
   if (sport != null) System.setProperty("edu.brown.cs.ivy.mint.server.port",sport);

   mint_control = MintControl.create(mint_name,MintSyncMode.ONLY_REPLIES);
}




/********************************************************************************/
/*										*/
/*	Reset methods								*/
/*										*/
/********************************************************************************/

void resetProperties()
{
   File ivv = BoardProperties.getPropertyDirectory();
   if (!ivv.exists()) ivv.mkdir();

   for (String s : BOARD_RESOURCE_PROPS) {
      File f1 = new File(ivv,s);
      f1.delete();
    }

   if (install_jar) checkJarResources();
   else if (install_path != null) checkLibResources();
}



/********************************************************************************/
/*										*/
/*	Installation methods							*/
/*										*/
/********************************************************************************/

public void doInstall()
{
   if (!checkInstall()) return;

   if (eclipse_directory == null && board_language == BoardLanguage.JAVA) {
      eclipse_directory = System.getProperty("edu.brown.cs.bubbles.eclipse");
      if (eclipse_directory == null) eclipse_directory = System.getenv("BUBBLES_ECLIPSE");
      if (!checkEclipse()) eclipse_directory = null;
      if (eclipse_directory != null) saveProperties();
    }
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Entry point for doing setup for a bubbles applications.  Any application should
 *	first consruct an instance of BoardSetup and then call this method.  Once the
 *	method returns, bubbles should be ready to use.
 **/

public boolean doSetup()
{
   if (do_uninstall) {
      uninstall();
      return false;
    }

   if (course_name != null) auto_update = false;

   boolean firsttime = checkDefaultInstallation();
   if (firsttime && eclipse_directory == null && board_language == BoardLanguage.JAVA) {
      eclipse_directory = System.getProperty("edu.brown.cs.bubbles.eclipse");
      if (eclipse_directory == null) eclipse_directory = System.getenv("BUBBLES_ECLIPSE");
      if (!checkEclipse()) eclipse_directory = null;
    }

   if (show_splash && splash_screen == null && !firsttime) {
      splash_screen = new BoardSplash();
      splash_screen.start();
    }

   boolean thru = (setup_count != 0 && default_workspace != null);
   switch (board_language) {
      case JAVA :
	 thru &= eclipse_directory != null;
	 break;
      case PYTHON :
	 break;
      case JS :
	 break;
      case REBUS :
	 break;
   }
   if (thru) {
      BoardLog.setup();
      if (setup_count < 0) {
	 BoardUpdate.setVersion();
	 setupPaths();
       }
      return false;
    }

   setup_count = 1;

   setSplashTask("Checking configuration");

   boolean askworkspace = ask_workspace;
   if (workspace_given) askworkspace = false;

   boolean needsetup = force_setup;
   switch (board_language) {
      case JAVA :
	 needsetup |= !checkEclipse();
	 needsetup |= !checkPlugin();
	 needsetup |= !checkInstall() && !install_jar;
	 break;
      case PYTHON :
	 needsetup |= !checkInstall() && !install_jar;
	 break;
      case JS :
	 needsetup |= !checkInstall() && !install_jar;
	 break;
      case REBUS :
	 needsetup |= !checkInstall() && !install_jar;
	 break;
    }
   askworkspace |= !checkWorkspace();

   if (install_jar && !update_setup) {
      setSplashTask("Checking for newer version");
      if (update_proxy != null) BoardUpdate.setupProxy(update_proxy);
      try {
	 if (auto_update) BoardUpdate.checkUpdate(jar_file,java_args);
	 else BoardUpdate.setVersion();
       }
      catch (UnsupportedClassVersionError e) {
	 reportError("Java version for Bubbles and Eclipse must be 10 or greater");
	 System.exit(1);
       }
    }
   else if (install_jar || jar_directory != null) {
      BoardUpdate.setVersion();
    }

   if (getRunMode() != RunMode.CLIENT) {
      setSplashTask("Getting configuration information");
      if (needsetup) handleSetup();
      else if (askworkspace || has_changed) {
	 WorkspaceDialog wd = new WorkspaceDialog();
	 if (askworkspace && !wd.process()) {
	    BoardLog.logE("BOARD","BUBBLES: Setup aborted by workspace dialog");
	    System.exit(1);
	  }
	 if (has_changed || wd.hasChanged() || create_workspace) {
	    if (create_workspace) {
	       createWorkspace();
	     }
	    saveProperties();
	  }
       }
      if (default_workspace == null) {
	 reportError("No workspace specified");
	 System.exit(1);
       }
      else {
	 File wf = new File(default_workspace);
	 if (!wf.exists() || !wf.isDirectory() || !wf.canWrite()) {
	    reportError("Invaid workspace directory");
	    System.exit(1);
	  }
       }

    }

   if (!checkPalette()) {
      BoardLog.logE("BOARD","BUBBLES: Setup aborted by palette dialog");
      System.exit(1);
    }

   switch (board_language) {
      case JAVA :
	 boolean needupdate = force_setup | update_setup;
	 needupdate |= !checkDates();
	 if (needupdate) {
	    setSplashTask("Updating Eclipse plugin");
	    updatePlugin();
	  }
	 break;
      case PYTHON :
	 break;
      case JS :
	 break;
      case REBUS :
	 break;
    }

   if (install_jar) {
      URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN);
      if (url != null) {
	 setSplashTask("Checking libraries");
	 String cp = System.getProperty("java.class.path");
	 for (String s : BOARD_LIBRARY_FILES) {
	    if (!s.endsWith(".jar")) continue;
	    s = s.replace('/',File.separatorChar);
	    if (!cp.contains(s)) must_restart = true;
	 }
       }
    }

   if (default_workspace != null) {
      system_properties.setProperty(BOARD_PROP_ECLIPSE_WS,default_workspace);
      recent_workspaces.remove(default_workspace);
      recent_workspaces.add(0,default_workspace);
      saveProperties();
    }

   if (must_restart) {
      restartBubbles();
    }

   BoardLog.setup();

   setSplashTask("Checking messaging configuration");
   setupIvy();

   setupProxy();

   BoardFileSystemView.setupRemoteServer();

   return must_restart;
}





private void setupIvy()
{
   File ivv = new File(System.getProperty("user.home"),".ivy");
   File f4 = new File(ivv,"Props");
   if (ivv.exists() && ivv.isDirectory() && f4.exists()) {
      if (IvySetup.setup(ivv)) {
	 File f = IvyFile.expandFile("$(IVY)");
	 if (f.exists() && f.isDirectory()) return;
       }
    }

   boolean ivyfound = false;
   for (int i = 0; !ivyfound && i < ivy_props.length; ++i) {
      if (System.getProperty(ivy_props[i]) != null) ivyfound = true;
    }
   for (int i = 0; !ivyfound && i < ivy_env.length; ++i) {
      if (System.getenv(ivy_env[i]) != null) ivyfound = true;
    }
   if (ivyfound) return;

   String ivydir = null;
   if (install_jar) ivydir = jar_directory;
   else {
      File fi = new File(install_path);
      File fi2 = new File(fi.getParentFile(),"ivy");
      if (fi2.exists() && fi2.isDirectory()) ivydir = fi2.getAbsolutePath();
      else ivydir = install_path;
    }
   System.setProperty("edu.brown.cs.IVY",ivydir);

   Properties p = new Properties();

   try {
      Registry rmireg = LocateRegistry.getRegistry("valerie");
      Object o = rmireg.lookup("edu.brown.cs.ivy.mint.registry");
      if (o != null) {
	 p.setProperty("edu.brown.cs.ivy.mint.registryhost","valerie.cs.brown.edu");
       }
    }
   catch (Exception e) { }

   p.setProperty("BROWN_IVY_IVY",ivydir);
   p.setProperty("edu.brown.cs.ivy.IVY",ivydir);

   if (!ivv.exists()) {
      if (!ivv.mkdir()) {
	 BoardLog.logE("BOARD","Unable to create directory " + ivv);
	 reportError("Unable to create directory " + ivv);
	 System.exit(1);
       }
    }

   try {
      FileOutputStream os = new FileOutputStream(f4);
      p.storeToXML(os,"SETUP on " + new Date().toString() + " by BOARD");
      os.close();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem saving ivy: " + e);
      reportError("Problem saving ivy: " + e);
      System.exit(1);
    }
}



private void uninstall()
{
   if (checkPlugin()) {
      File pins = getPluginDirectory();
      if (pins == null) return;
      File pin = new File(pins,BOARD_BUBBLES_PLUGIN);
      pin.delete();
    }

   File bdir = BoardProperties.getPropertyDirectory();
   if (bdir.exists()) deleteAll(bdir);
}



private void deleteAll(File f)
{
   if (!f.exists()) return;

   if (f.isDirectory()) {
      for (File fd : f.listFiles()) {
	 deleteAll(fd);
       }
    }

   f.delete();
}




/********************************************************************************/
/*										*/
/*	Setup Dialog								*/
/*										*/
/********************************************************************************/

private void handleSetup()
{
   if (getRunMode() == RunMode.CLIENT) return;

   SetupDialog sd = new SetupDialog();
   if (!sd.process()) {
      BoardLog.logE("BOARD","Setup aborted by user");
      System.exit(1);
    }

   WorkspaceDialog wd = new WorkspaceDialog();
   if (!wd.process()) {
      BoardLog.logE("BOARD","Setup aborted by user/workspace dialog");
      System.exit(1);
    }
   if (create_workspace) {
      createWorkspace();
   }

   saveProperties();
}



private void saveProperties()
{
   if (install_path != null) system_properties.setProperty(BOARD_PROP_INSTALL_DIR,install_path);
   system_properties.setProperty(BOARD_PROP_AUTO_UPDATE,auto_update);
   if (eclipse_directory != null) {
      system_properties.setProperty(BOARD_PROP_ECLIPSE_DIR,eclipse_directory);
    }
   if (default_workspace != null) {
      system_properties.setProperty(BOARD_PROP_ECLIPSE_WS,default_workspace);
    }
   system_properties.setProperty(BOARD_PROP_ECLIPSE_ASK_WS,ask_workspace);
   system_properties.setProperty(BOARD_PROP_ECLIPSE_FOREGROUND,run_foreground);
   String logl = system_properties.getProperty(BOARD_PROP_LOG_LEVEL);
   if (logl == null || logl.length() == 0) {
      if (install_jar) logl = "WARNING";
      else logl = "DEBUG";
      system_properties.setProperty(BOARD_PROP_LOG_LEVEL,logl);
    }

   if (jar_directory != null) system_properties.setProperty(BOARD_PROP_JAR_DIR,jar_directory);
   if (!recent_workspaces.isEmpty()) {
      StringBuffer buf = new StringBuffer();
      int ct = 0;
      for (String s : recent_workspaces) {
	 if (ct++ > 0) buf.append(";");
	 buf.append(s);
	 if (ct >= 10) break;
       }
      system_properties.setProperty(BOARD_PROP_RECENT_WS,buf.toString());
    }

   String vmargs = system_properties.getProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS);
   if (vmargs == null) {
      switch (board_language) {
	 case JAVA :
	 case PYTHON :
	 case REBUS :
//	    system_properties.setProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS,"-Xmx512m");
	    break;
	 case JS :
	    system_properties.setProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS,"-Xmx1536m");
	    break;
      }
    }

   try {
      system_properties.save();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Unable to save system properties: " + e);
      reportError("Unable to save system properties: " + e);
      System.exit(2);
    }
}



private void setupPaths()
{
   install_path = system_properties.getProperty(BOARD_PROP_INSTALL_DIR);
   jar_directory = system_properties.getProperty(BOARD_PROP_JAR_DIR);
   if (jar_directory != null) install_jar = true;
}



/********************************************************************************/
/*										*/
/*	Check if eclipse is setup correctly					*/
/*										*/
/********************************************************************************/

private boolean checkEclipse()
{
   if (eclipse_directory == null) return false;
   File ed = new File(eclipse_directory);
   if (checkEclipseDirectory(ed)) return true;

   for (String s : BOARD_ECLIPSE_START) {
      if (ed.getName().equals(s) && ed.canExecute()) {
	 File par = ed.getParentFile();
	 if (checkEclipseDirectory(par)) {
	    eclipse_directory = par.getAbsolutePath();
	    return true;
	  }
       }
    }
   return false;
}



private boolean checkPlugin()
{
   if (!checkEclipse()) return false;

   File pins = getPluginDirectory();
   if (pins == null) return false;
   File pin = new File(pins,BOARD_BUBBLES_PLUGIN);
   if (!pin.exists() || !pin.canRead()) return false;

   return true;
}



static boolean checkEclipseDirectory(File ed)
{
   if (!ed.exists() || !ed.isDirectory()) return false;

   boolean execfnd = false;
   for (String s : BOARD_ECLIPSE_START) {
      File binf = new File(ed,s);
      if (binf.exists() && binf.canExecute()) {
	 if (!binf.isDirectory() || s.endsWith(".app")) {
	    execfnd = true;
	    break;
	  }
       }
    }
   if (!execfnd) return false;

   File pdf = getPluginDirectory(ed);
   if (pdf == null || !pdf.exists() || !pdf.isDirectory() ||
	 !pdf.canWrite())
      return false;

   // NEED A BETTER WAY OF CHECKING ECLIPSE VERSION

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



private static boolean checkEclipseVersion(File pdf)
{
   // System.err.println("BOARD: Check eclipse directory " + pdf);

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
	       // System.err.println("BOARD: Version " + d);
	       if (d < 3.5) {
		  return false; 			// illegal Eclipse version
		}
	     }
	  }
       }
      if (fnm.startsWith("org.eclipse.jdt.core")) havejava = true;
    }
   // System.err.println("BOARD: Result " + havejava);
   if (!havejava) return false;

   // check for proper architecture as well

   return true;
}




/********************************************************************************/
/*										*/
/*	Check if bubbles is installed correctly 				*/
/*										*/
/********************************************************************************/

private boolean checkInstall()
{
   // first check if we are running from a complete jar
   boolean ok = true;
   try {
      InputStream ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN);
      if (ins == null) ok = false;
      else ins.close();
      for (String s : BOARD_RESOURCE_PROPS) {
	 if (!ok) break;
	 ins = getClass().getClassLoader().getResourceAsStream(s);
	 if (ins == null) {
	    ok = false;
	    // System.err.println("BOARD: Setup failed on " + s);
	  }
	 else ins.close();
       }
      if (ok) {
	 URL url = BoardImage.class.getClassLoader().getResource(BOARD_RESOURCE_CHECK);
	 if (url == null || !url.toString().startsWith("jar")) ok = false;
       }
      // System.err.println("BOARD: CHECK INSTALL: " + ok + " " + install_jar);
      install_jar = ok;
      if (install_jar) {
	 URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN);
	 String file = url.toString();
	 if (file.startsWith("jar:file:/")) file = file.substring(9);
	 if (file.length() >= 3 && file.charAt(0) == '/' &&
		Character.isLetter(file.charAt(1)) && file.charAt(2) == ':' &&
		File.separatorChar == '\\')
	    file = file.substring(1);
	 int idx = file.lastIndexOf('!');
	 if (idx >= 0) file = file.substring(0,idx);
	 if (File.separatorChar != '/') file = file.replace('/',File.separatorChar);
	 file = file.replace("%20"," ");
	 File f = new File(file);
	 jar_file = f.getPath();
	 jar_directory = f.getParent();
	 checkJarResources();
	 checkJarLibraries();
	 checkBinFiles();
	 File nobbles = new File(f.getParentFile(),"nobbles.jar");
	 File pybbles = new File(f.getParentFile(),"pybles.jar");
	 File cloudbb = new File(f.getParentFile(),"cloudbb.jar");
	 Path jarpath = f.toPath();
	 Path nblpath = nobbles.toPath();
	 Path pybpath = pybbles.toPath();
	 Path clbpath = cloudbb.toPath();
	 try {
	    if (!nobbles.exists()) Files.createSymbolicLink(nblpath,jarpath);
	  }
	 catch (Exception e) { }
	 try {
	    if (!pybbles.exists()) Files.createSymbolicLink(pybpath,jarpath);
	  }
	 catch (Exception e) { }
	 try {
	    if (!cloudbb.exists()) Files.createSymbolicLink(clbpath,jarpath);
	  }
	 catch (Exception e) { }
	 File dropins = new File(f.getParentFile(),"dropins");
	 if (!dropins.exists()) dropins.mkdir();
       }
    }
   catch (IOException e) {
      System.err.println("BOARD: Problem with jar setup: " + e);
      BoardLog.logE("BOARD","Problem with jar setup",e);
      install_jar = false;
    }

   if (!install_jar) {
      // otherwise check for a valid installation
      if (install_path == null) return false;
      File ind = new File(install_path);
      if (!checkInstallDirectory(ind)) return false;
      checkLibResources();
    }

   return true;
}



private static boolean checkInstallDirectory(File ind)
{
   if (!ind.exists() || !ind.isDirectory()) return false;
   int fct = 0;
   for (String s : BOARD_BUBBLES_START) {
      File ex = new File(ind,s);
      if (ex.exists() && ex.canExecute() && !ex.isDirectory()) ++fct;
    }
   if (fct == 0) return false;

   File libb = new File(ind,BOARD_INSTALL_LIBRARY);
   if (!libb.exists() || !libb.isDirectory()) return false;
   File binb = new File(ind,BOARD_INSTALL_BINARY);
   if (!binb.exists() || !binb.isDirectory()) return false;
   File inf = new File(libb,BOARD_RESOURCE_PLUGIN);
   if (!inf.exists() || !inf.canRead()) return false;
   File elib = new File(ind,"eclipsejar");
   if (!elib.exists() || !elib.canRead()) return false;

   for (String s : BOARD_RESOURCE_PROPS) {
      inf = new File(libb,s);
      if (!inf.exists() || !inf.canRead()) return false;
    }
   for (String s : BOARD_LIBRARY_FILES) {
      s = s.replace("/",File.separator);
      inf = new File(elib,s);
      if (!inf.exists() || !inf.canRead()) inf = new File(libb,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing library file " + inf);
       }
    }
   for (String s : BOARD_LIBRARY_EXTRAS) {
      s = s.replace("/",File.separator);
      inf = new File(elib,s);
      if (!inf.exists() || !inf.canRead()) inf = new File(libb,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing library file " + inf);
       }
    }
   for (String s : BOARD_BINARY_FILES) {
      s = s.replace("/",File.separator);
      inf = new File(binb,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing binary file " + inf);
       }
    }
   File libt = new File(libb,"templates");
   for (String s : BOARD_TEMPLATES) {
      inf = new File(libt,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing template file " + inf);
       }
    }

   return true;
}



private void checkJarResources()
{
   File ivv = BoardProperties.getPropertyDirectory();
   if (!ivv.exists()) ivv.mkdir();

   File cpt = null;
   if (course_name != null) {
      File c1 = new File(jar_directory);
      cpt = new File(c1,course_name);
      if (!cpt.exists() || !cpt.isDirectory()) cpt = null;
    }

   for (String s : BOARD_RESOURCE_PROPS) {
      try {
	 File f1 = new File(ivv,s);

	 InputStream ins = null;
	 if (cpt != null) {
	    File f2 = new File(cpt,s);
	    try {
	       if (f2.exists()) ins = new FileInputStream(f2);
	     }
	    catch (IOException e) { }
	  }
	 if (ins == null) ins = BoardSetup.class.getClassLoader().getResourceAsStream(s);

	 checkJarResourceItem(s,f1,ins);
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","Problem setting up jar resource " + s + ": " + e);
	 reportError("Problem setting up jar resource " + s + ": " + e);
	 System.exit(1);
       }
    }
}


private void checkJarResourceItem(String s,File f1,InputStream ins) throws IOException
{
   if (f1.exists()) {
      ensurePropsDefined(s,ins);
    }
   else {
      FileOutputStream ots = new FileOutputStream(f1);
      copyFile(ins,ots);
    }
}




private void checkJarLibraries()
{
   if (!auto_update) return;

   File libd = getLibraryDirectory();
   if (!libd.exists()) libd.mkdir();

   for (String s : BOARD_LIBRARY_FILES) {
      must_restart |= extractLibraryResource(s,libd,update_setup);
    }

   for (String s : BOARD_LIBRARY_EXTRAS) {
      extractLibraryResource(s,libd,auto_update);
    }

   File libt = new File(libd,"templates");
   if (!libt.exists()) libt.mkdir();
   for (String s : BOARD_TEMPLATES) {
      extractLibraryResource(s,libt,auto_update);
    }

   File pyd = libd.getParentFile();
   File pyd1 = new File(pyd,"pybles");
   extractPybles(pyd1,libd,update_setup);
   extractNobbles(libd,update_setup);
}


private void checkBinFiles()
{
   if (!auto_update) return;

   File libd = getLibraryDirectory();
   File libp = libd.getParentFile();
   File bind = new File(libp,"bin");
   if (!bind.exists()) bind.mkdir();

   for (String s : BOARD_BINARY_FILES) {
      extractLibraryResource(s,bind,false);
      File f1 = new File(bind,s);
      f1.setExecutable(true,false);
    }
}






private boolean extractLibraryResource(String s,File libd,boolean force)
{
   boolean upd = false;

   String xs = s;
   int idx = s.lastIndexOf("/");
   if (idx >= 0) {
      xs = s.replace('/',File.separatorChar);
      String hd = xs.substring(0,idx);
      File fd = new File(libd,hd);
      if (!fd.exists()) fd.mkdirs();
    }

   File f1 = new File(libd,xs);
   if (!force && f1.exists()) return false;
   upd = true;

   try {
      InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream(s);
      if (ins != null) {
	 FileOutputStream ots = new FileOutputStream(f1);
	 copyFile(ins,ots);
       }
    }
   catch (IOException e) {
      if (f1.exists()) upd = false;			// use old version if necessary
      else {
	 BoardLog.logE("BOARD","Problem setting up jar lib resource " + s + ": " + e,e);
	 reportError("Problem setting up jar lib resource " + s + ": " + e);
	 System.exit(1);
       }
    }

   return upd;
}



private void extractPybles(File pyd,File libd,boolean force)
{
   String pfx1 = null;
   String pfx2 = null;
   String pyblesfiles = null;
   String libfiles = null;

   try (InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream("pyblesfiles.txt")) {
      if (ins == null) return;
      BufferedReader br = new BufferedReader(new InputStreamReader(ins));
      String s = br.readLine();
      if (s != null) {
	 StringTokenizer tok = new StringTokenizer(s);
	 pfx1 = tok.nextToken();
	 pfx2 = tok.nextToken();
       }
      pyblesfiles = br.readLine();
      libfiles = br.readLine();
    }
   catch (IOException e) {
      return;
    }
   if (pfx1 == null || pfx2 == null || pyblesfiles == null || libfiles == null) return;

   if (!pyd.exists()) pyd.mkdir();

   for (StringTokenizer tok = new StringTokenizer(pyblesfiles,":"); tok.hasMoreTokens(); ) {
      String pyf = tok.nextToken();
      if (!pyf.startsWith(pfx1)) continue;
      pyf = pyf.substring(pfx1.length());
      while (pyf.startsWith("/")) pyf = pyf.substring(1);
      File f1 = pyd;
      for (StringTokenizer t1 = new StringTokenizer(pyf,"/"); t1.hasMoreTokens(); ) {
	 String comp = t1.nextToken();
	 if (!f1.exists()) f1.mkdir();
	 f1 = new File(f1,comp);
       }
      if (!force && f1.exists()) continue;
      try {
	 InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream(pyf);
	 if (ins != null) {
	    FileOutputStream ots = new FileOutputStream(f1);
	    copyFile(ins,ots);
	  }
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","Problem setting up pybles files: " + e);
       }
    }

   for (StringTokenizer tok = new StringTokenizer(libfiles,":"); tok.hasMoreTokens(); ) {
      String pyf = tok.nextToken();
      if (!pyf.startsWith(pfx2)) continue;
      pyf = pyf.substring(pfx2.length());
      while (pyf.startsWith("/")) pyf = pyf.substring(1);
      int idx = pyf.lastIndexOf("/");
      String f1 = pyf;
      if (idx >= 0) f1 = pyf.substring(idx+1);
      File of = new File(libd,f1);
      if (!force && of.exists()) continue;
      try {
	 InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream(pyf);
	 if (ins != null) {
	    FileOutputStream ots = new FileOutputStream(of);
	    copyFile(ins,ots);
	  }
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","Problem setting up pybles library files: " + e);
       }
    }
}



private void extractNobbles(File libd,boolean force)
{
   File jslib = new File(libd,"JSFiles");
   if (!jslib.exists()) jslib.mkdir();

   try {
      InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream("jsfiles.txt");
      if (ins == null) return;
      try (BufferedReader br = new BufferedReader(new InputStreamReader(ins))) {
	 for ( ; ; ) {
	    String s = br.readLine();
	    if (s == null) break;
	    s = s.trim();
	    if (s.length() == 0) continue;
	    if (s.startsWith("#")) continue;

	    File f1 = new File(jslib,s);
	    if (!force && f1.exists()) continue;
	    try {
	       InputStream sins = BoardSetup.class.getClassLoader().getResourceAsStream(s);
	       if (sins != null) {
		  FileOutputStream ots = new FileOutputStream(f1);
		  copyFile(sins,ots);
		}
	     }
	    catch (IOException e) {
	       BoardLog.logE("BOARD","Problem setting up nobbles files: " + e);
	     }
	  }
       }
    }
   catch (IOException e) {
      return;
    }
}


private void checkLibResources()
{
   File ivv = BoardProperties.getPropertyDirectory();
   if (!ivv.exists()) ivv.mkdir();
   File lbv = new File(install_path,BOARD_INSTALL_LIBRARY);

   for (String s : BOARD_RESOURCE_PROPS) {
      try {
	 checkResourceFile(s);
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","Problem setting up lib resource " + s + " (" + lbv + "," + install_path + ")",e);
	 reportError("Problem setting up lib resource " + s + ": " + e);
	 System.exit(1);
       }
    }
}


public void checkResourceFile(String resfile) throws IOException
{
   File ivv = BoardProperties.getPropertyDirectory();
   if (!ivv.exists()) ivv.mkdir();
   File lbv = new File(install_path,BOARD_INSTALL_LIBRARY);

   File f1 = new File(ivv,resfile);
   File f2 = new File(lbv,resfile);
   InputStream ins = new FileInputStream(f2);
   if (f1.exists()) {
      ensurePropsDefined(resfile,ins);
    }
   else {
      OutputStream ots = new FileOutputStream(f1);
      copyFile(ins,ots);
    }
}



/********************************************************************************/
/*										*/
/*	Initial property definitions						*/
/*										*/
/********************************************************************************/

private void ensurePropsDefined(String nm,InputStream ins) throws IOException
{
   BoardProperties ups = BoardProperties.getProperties(nm);
   BoardProperties dps = new BoardProperties(ins);

   boolean chng = false;

   for (String pnm : dps.stringPropertyNames()) {
      if (pnm.endsWith(".PROB")) continue;
      String v = dps.getProperty(pnm);
      if (v != null && !ups.containsKey(pnm)) {
	 String xnm = pnm + ".PROB";
	 String up = ups.getProperty(xnm);
	 String vp = dps.getProperty(xnm);
	 if (vp != null && up == null) {
	    double dv = 1;
	    try {
	       dv = Double.parseDouble(vp);
	     }
	    catch (NumberFormatException e) { }
	    if (Math.random() > dv) {
	       ups.setProperty(xnm,"0");
	       chng = true;
	       continue;
	     }
	  }
	 ups.setProperty(pnm,v);
	 chng = true;
       }
    }

   if (chng) ups.save();
}



/********************************************************************************/
/*										*/
/*	Check for a valid eclipse workspace					*/
/*										*/
/********************************************************************************/

private boolean checkWorkspace()
{
   if (default_workspace == null) return false;
   File wsd = new File(default_workspace);
   if (!checkWorkspaceDirectory(wsd,create_workspace)) return false;

   default_workspace = wsd.getAbsolutePath();
   return true;
}



private static boolean checkWorkspaceDirectory(File wsd,boolean create)
{
   if (wsd == null) return false;

   if (create) {
      if (wsd.getParentFile() == null) return false;
      if (!wsd.getParentFile().exists()) return false;
      if (!wsd.getParentFile().canWrite()) return false;
      if (wsd.exists() && !wsd.isDirectory()) return false;
      return true;
    }

   if (!wsd.exists() || !wsd.isDirectory()) return false;

   File df = new File(wsd,BOARD_ECLIPSE_WS_DATA);
   if (!df.exists() || !df.canRead()) return false;

   return true;
}



private boolean checkDates()
{
   long dlm = 0;

   if (install_jar) {
      try {
	 URL u = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN);
	 if (u == null) return true;
	 URLConnection uc = u.openConnection();
	 dlm = uc.getLastModified();
	 // TODO: this is the overall jar file, not the bedrock jar file
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","PROBLEM LOADING URL: " + e);
       }
    }
   else {
      File ind = new File(install_path);
      File libb = new File(ind,BOARD_INSTALL_LIBRARY);
      File inf = new File(libb,BOARD_RESOURCE_PLUGIN);
      dlm = inf.lastModified();
    }

   File pdf = getPluginDirectory();
   File bdf = new File(pdf,BOARD_BUBBLES_PLUGIN);
   long edlm = bdf.lastModified();

   if (dlm > 0 && edlm > 0 && edlm >= dlm) return true;
   if (edlm > 0 && !auto_update) return true;

   return false;
}



private void createWorkspace()
{
   File wf = new File(default_workspace);
   if (!wf.exists()) {
      wf.mkdirs();
      File xf = new File(wf,BOARD_ECLIPSE_WS_DATA);
      xf.mkdir();
      BoardLog.setup();     // might need to restart logging
    }
   create_workspace = false;
}



/********************************************************************************/
/*										*/
/*	File methods								*/
/*										*/
/********************************************************************************/

private void copyFile(InputStream ins,OutputStream ots) throws IOException
{
   byte [] buf = new byte[8192];

   for ( ; ; ) {
      int ct = ins.read(buf,0,buf.length);
      if (ct < 0) break;
      ots.write(buf,0,ct);
    }

   ins.close();
   ots.close();
}




/********************************************************************************/
/*										*/
/*	Handle palettes 							*/
/*										*/
/********************************************************************************/


public void setPalette(String pal)
{
   palette_set = true;
   BoardColors.setPalette(pal);
}



private boolean checkPalette()
{
   if (palette_set) return true;

   switch (getRunMode()) {
      case SERVER :
	 return true;
      case NORMAL :
      case CLIENT :
	 break;
    }

   String pal = system_properties.getProperty(PALETTE_PROP);
   if (pal != null && !pal.equals("") && !pal.equals("*")) {
      BoardColors.setPalette(pal);
      return true;
    }

   PaletteDialog pd = new PaletteDialog();
   if (!pd.process()) return false;

   pal = pd.getPalette();
   if (pal != null) BoardColors.setPalette(pal);
   pal = BoardColors.getPalette();
   if (pd.savePalette()) {
      system_properties.setProperty(PALETTE_PROP,pal);
      saveProperties();
    }

   return true;
}


private class PaletteDialog implements ActionListener {

   private JDialog working_dialog;
   private boolean result_status;
   private String  use_palette;
   private boolean save_palette;

   PaletteDialog() {
      use_palette = DEFAULT_PALETTE;
      save_palette = false;
      SwingGridPanel pnl = new SwingGridPanel();
      BoardColors.setColors(pnl,WORKSPACE_DIALOG_COLOR);
      pnl.setOpaque(true);
      pnl.beginLayout();
      pnl.addBannerLabel("Choose Bubbles Color Palette");
      pnl.addSeparator();
      ButtonGroup grp = new ButtonGroup();
      JRadioButton btn1 = new JRadioButton("Classical (dark on light)",true);
      btn1.setOpaque(false);
      btn1.addActionListener(this);
      pnl.addLabellessRawComponent("CLASSICAL",btn1);
      grp.add(btn1);
      JRadioButton btn2 = new JRadioButton("Inverted (light on dark)",false);
      btn2.setOpaque(false);
      btn2.addActionListener(this);
      pnl.addLabellessRawComponent("INVERTED",btn2);
      pnl.addBoolean("   Remember my choice",save_palette,this);
      grp.add(btn2);
      pnl.addSeparator();
      pnl.addBottomButton("OK","OK",this);
      pnl.addBottomButton("CANCEL","CANCEL",this);
      pnl.addBottomButtons();

      working_dialog = new JDialog((JFrame) null,"Bubbles Palette Setup",true);
      working_dialog.setContentPane(pnl);
      working_dialog.pack();
      working_dialog.setLocationRelativeTo(null);
      working_dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

   boolean process() {
      result_status = true;
      working_dialog.setVisible(true);
      return result_status;
    }

   String getPalette()			{ return use_palette; }
   boolean savePalette()		{ return save_palette; }

   @Override public void actionPerformed(ActionEvent evt) {
      String nm = evt.getActionCommand();
      if (nm.contains("Classical")) {
	 use_palette = DEFAULT_PALETTE;
       }
      else if (nm.contains("Inverted")) {
	 use_palette = "inverse_" + DEFAULT_PALETTE;
       }
      else if (nm.contains("Remember")) {
	 JButton btn = (JButton) evt.getSource();
	 save_palette = btn.isSelected();
       }
      else if (nm.equals("CANCEL")) {
	 result_status = false;
	 working_dialog.setVisible(false);
       }
      else if (nm.equals("OK")) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
    }

}


/********************************************************************************/
/*										*/
/*	Plugin update methods							*/
/*										*/
/********************************************************************************/

private void updatePlugin()
{
   // ideally, should check if eclipse is currently running?

   try {
      InputStream ins = null;
      if (install_jar) {
	 ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN);
	 if (ins == null) return;
       }
      else {
	 File ind = new File(install_path);
	 File libb = new File(ind,BOARD_INSTALL_LIBRARY);
	 File inf = new File(libb,BOARD_RESOURCE_PLUGIN);
	 ins = new FileInputStream(inf);
       }
      File pdf = getPluginDirectory();
      File bdf = new File(pdf,BOARD_BUBBLES_PLUGIN);

      OutputStream ots = new FileOutputStream(bdf);

      copyFile(ins,ots);

      system_properties.setProperty(BOARD_PROP_ECLIPSE_CLEAN,true);
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem updating bubble eclipse plugin: " + e,e);
      File pdf = getPluginDirectory();
      File bdf = new File(pdf,BOARD_BUBBLES_PLUGIN);
      if (pdf != null && pdf.exists() && (!pdf.canWrite() || !bdf.canWrite())) {     // user lacks permissions
	 if (bdf.exists()) return;	// leave things be if it is there already
	 reportError("Can't add plugin to your Eclipse installation: " + pdf);
       }
      else reportError("Problem updating bubble eclipse plugin: " + e);

      System.exit(3);
    }
}



private File getPluginDirectory()
{
   return getPluginDirectory(new File(eclipse_directory));
}



private static File getPluginDirectory(File edf)
{
   File ddf = new File(edf,BOARD_ECLIPSE_MAC_DROPIN);
   File pdf = checkPluginDirectory(ddf);
   if (pdf != null) return pdf;

   pdf = checkPluginDirectory(edf);
   if (pdf != null) return pdf;

   File f1 = edf.getParentFile();
   if (f1 != null) f1 = f1.getParentFile();
   if (f1 != null) f1 = f1.getParentFile();
   if (f1 != null) {
      f1 = new File(f1,".p2");
      f1 = new File(f1,"pool");
      pdf = checkPluginDirectory(f1);
      if (pdf != null) return pdf;
    }

   File f2 = new File(System.getProperty("user.home"),".p2");
   f2 = new File(f2,"pool");
   pdf = checkPluginDirectory(f2);
   if (pdf != null) return pdf;

   return null;
}



private static File checkPluginDirectory(File base)
{
   if (!base.exists() || !base.isDirectory()) return null;

   File pdf1 = new File(base,BOARD_ECLIPSE_DROPINS);
   File pdf2 = new File(base,BOARD_ECLIPSE_PLUGINS);
   if (pdf1.exists() && pdf1.isDirectory()) {
      File pin = new File(pdf1,BOARD_BUBBLES_PLUGIN);
      if (pin.exists() && pin.canRead()) return pdf1;
    }

   if (pdf2.exists() && pdf2.isDirectory()) {
      File pin = new File(pdf2,BOARD_BUBBLES_PLUGIN);
      if (pin.exists() && pin.canRead()) return pdf2;
    }

   if (pdf1.exists() && pdf1.isDirectory() && pdf1.canWrite()) return pdf1;
   if (pdf2.exists() && pdf2.isDirectory() && pdf2.canWrite()) return pdf2;
   if (pdf1.exists() && pdf1.isDirectory()) return pdf1;
   if (pdf2.exists() && pdf2.isDirectory()) return pdf2;

   return null;
}




/********************************************************************************/
/*										*/
/*	Restart methods 							*/
/*										*/
/********************************************************************************/

private void restartBubbles()
{
   if (jar_file == null) return;

   setSplashTask("Restarting with new configuration");

   saveProperties();

   File dir1 = new File(jar_directory);
   File dir = getLibraryDirectory();

   StringBuffer cp = new StringBuffer();
   cp.append(jar_file);
   for (String s : BOARD_LIBRARY_FILES) {
      if (!s.endsWith(".jar")) continue;
      s = s.replace('/',File.separatorChar);
      File f = new File(dir,s);
      cp.append(File.pathSeparator);
      cp.append(f.getPath());
    }
   for (String s : BOARD_CLASSPATH_FILES) {
      s = s.replace('/',File.separatorChar);
      File f = new File(dir,s);
      cp.append(File.pathSeparator);
      cp.append(f.getPath());
    }

   List<String> args = new ArrayList<String>();
   if (java_args != null) args.addAll(java_args);
   String jpath = IvyExecQuery.getJavaPath();

   System.err.println("BOARD: RESTART: " + jpath + " -Dedu.brown.cs.bubbles.BASE=" +
		    BoardProperties.getPropertyDirectory() +
		    " -Xmx" + run_size + " -cp " + cp.toString() + " " +
		    BOARD_RESTART_CLASS + " -nosetup");

   try {
      int idx = 0;
      args.add(idx++,jpath);
      if (use_lila) {
	 File lf = new File(dir,"LagHunter-4.jar");
	 if (lf.exists()) {
	    File f2 = new File(dir1,"LiLaConfiguration.ini");
	    File f3 = new File(dir,"LiLaConfiguration.ini");
	    if (!f2.exists() && f3.exists()) {
	       try {
		  FileInputStream fis = new FileInputStream(f3);
		  FileOutputStream fos = new FileOutputStream(f2);
		  copyFile(fis,fos);
		}
	       catch (IOException e) { }
	     }
	    String lc = "-javaagent:" + lf.getPath() + "=/useLiLaConfigurationFile";
	    args.add(idx++,lc);
//	    BoardLog.logD("BOARD","Use lila: " + lc);
	  }
       }

      File pf = BoardProperties.getPropertyDirectory();
      if (pf != null) {
	 args.add(idx++,"-Dedu.brown.cs.bubbles.BASE=" + pf.getAbsolutePath());
       }
      args.add(idx++,"-Xmx" + run_size);
      args.add(idx++,"-cp");
      args.add(idx++,cp.toString());
      if (allow_debug) {
	 args.add(idx++,"-Xdebug");
	 args.add(idx++,"-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n");
       }

      if (system_properties.getBoolean("edu.brown.cs.bubbles.verbose")) {
	 args.add(idx++,"-verbose");
       }

      args.add(idx++,BOARD_RESTART_CLASS);
      args.add(idx++,"-nosetup");
      if (force_metrics) args.add(idx++,"-collect");
      if (course_name != null) {
	 String cnm = course_name;
	 if (course_assignment != null) cnm += "@" + course_assignment;
	 args.add(idx++,"-course");
	 args.add(idx++,cnm);
       }
      switch (board_language) {
	 case PYTHON :
	    args.add(idx++,"-python");
	    break;
	 case REBUS :
	    args.add(idx++,"-rebus");
	    break;
	 case JS :
	    args.add(idx++,"-js");
	    break;
	 case JAVA :
	    break;
       }
      switch (run_mode) {
	 case CLIENT :
	    // args.add(idx++,"-CLIENT");
	    args.add(idx++,"-cloud");
	    break;
	 case SERVER :
	    args.add(idx++,"-SERVER");
	    break;
	 case NORMAL :
	    break;
       }

      ProcessBuilder pb = new ProcessBuilder(args);

      pb.inheritIO();		// requires Java 1.7 or beyond

      pb.start();
    }
   catch (IOException e) {
      BoardLog.setup();
      BoardLog.logE("BOARD","Problem restarting bubbles: " + e);
      reportError("Problem restarting bubbles: " + e);
      System.exit(1);
    }

   System.exit(0);
}


public boolean restartForNewWorkspace()
{
   List<String> args = new ArrayList<>();
   args.add(IvyExecQuery.getJavaPath());
   File pf = BoardProperties.getPropertyDirectory();
   if (pf != null) {
      args.add("-Dedu.brown.cs.bubbles.BASE=" + pf.getAbsolutePath());
    }
   args.add("-Xmx" + run_size);
   String ip = System.getProperty("edu.brown.cs.bubbles.INSTALLDIR");
   if (ip != null) {
      args.add("-Dedu.brown.cs.bubbles.INSTALLDIR=" + ip);
    }
   args.add("-cp");
   args.add(System.getProperty("java.class.path"));
   args.add(BOARD_RESTART_CLASS);
   if (force_metrics) args.add("-collect");
   switch (board_language) {
      case PYTHON :
	 args.add("-python");
	 break;
      case REBUS :
	 args.add("-rebus");
	 break;
      case JS :
	 args.add("-js");
	 break;
      case JAVA :
	 break;
    }
   args.add("-ask");

   ProcessBuilder pb = new ProcessBuilder(args);
   try {
      pb.inheritIO();
     pb.start();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem restarting bubbles: " + e);
      return false;
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Default installation checks						*/
/*										*/
/********************************************************************************/

private boolean checkDefaultInstallation()
{
   boolean firsttime = (jar_directory == null && install_path == null);
   if (!firsttime) return false;

   boolean jarok = true;
   try {
      InputStream ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN);
      if (ins == null) jarok = false;
      else ins.close();
    }
   catch (IOException e) {
      jarok = false;
    }

   File f = null;

   if (jarok) {
      URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN);
      String file = url.toString();
      if (file.startsWith("jar:file:/")) file = file.substring(9);
      if (file.length() >= 3 && file.charAt(0) == '/' &&
	       Character.isLetter(file.charAt(1)) && file.charAt(2) == ':' &&
	       File.separatorChar == '\\')
	 file = file.substring(1);
      int idx = file.lastIndexOf('!');
      if (idx >= 0) file = file.substring(0,idx);
      if (File.separatorChar != '/') file = file.replace('/',File.separatorChar);
      file = file.replace("%20"," ");
      f = new File(file);
      if (!f.exists()) return firsttime;
    }
   else {
      String pth = System.getProperty("edu.brown.cs.bubbles.jarpath");
      if (pth != null) {
	 f = new File(pth);
	 try {
	    f = f.getCanonicalFile();
	  }
	 catch (IOException e) { }
	 if (!f.exists()) f = null;
       }
    }

   if (f == null) return firsttime;

   jar_file = f.getPath();
   jar_directory = f.getParent();
   install_jar = true;

   if (eclipse_directory == null) {
      File fe = new File(jar_directory,"eclipse");
      File fe1 = null;
      if (f.getParentFile() != null) fe1 = new File(f.getParentFile().getParentFile(),"eclipse");
      if (checkEclipseDirectory(fe)) {
	 eclipse_directory = fe.getPath();
	 firsttime = false;
       }
      else if (fe1 !=  null && checkEclipseDirectory(fe1)) {
	 eclipse_directory = fe1.getPath();
	 firsttime = false;
       }
    }

   // has_changed = true;
   // ask_workspace = true;

   return firsttime;
}








/********************************************************************************/
/*										*/
/*	Setup dialog management 						*/
/*										*/
/********************************************************************************/

private class SetupDialog implements ActionListener, CaretListener, UndoableEditListener {

   private JButton accept_button;
   private JButton install_button;
   private JDialog working_dialog;
   private boolean result_status;
   private JLabel eclipse_warning;
   private JLabel bubbles_warning;
   private JTextField eclipse_field;
   private JTextField bubbles_field;
   private JButton eclipse_button;

   SetupDialog() {
      eclipse_field = null;
      bubbles_field = null;
      eclipse_button = null;
      SwingGridPanel pnl = new SwingGridPanel();

      BoardColors.setColors(pnl,"Buda.Bubbles.Color");
      pnl.setOpaque(true);

      pnl.beginLayout();
      pnl.addBannerLabel("Bubbles Environment Setup");

      pnl.addSeparator();

      switch (board_language) {
	 case JAVA :
	    eclipse_field = pnl.addFileField("Eclipse Installation Directory",eclipse_directory,
		  JFileChooser.DIRECTORIES_ONLY,
		  new EclipseDirectoryFilter(),this,this,null);

	    eclipse_warning = new JLabel("Warning!");  //edited by amc6
	    eclipse_warning.setToolTipText("<html>Not a valid <b>Eclipse for Java Developers</b> installation " +
		  "directory.<br>(This should be the directory containing the eclipse binary " +
		  "and the plugins directory.)");
	    eclipse_warning.setForeground(WARNING_COLOR);
	    pnl.add(eclipse_warning);
	    if (eclipse_directory == null && install_jar) {
	       // eclipse_button = pnl.addBottomButton("INSTALL ECLIPSE","ECLIPSE",this);
	     }
	    break;
	 case PYTHON :
	    break;
	 case REBUS :
	    break;
	 case JS :
	    break;
       }

      bubbles_warning = new JLabel("Warning!");
      bubbles_warning.setToolTipText("Not a valid Code Bubbles installation directory");
      bubbles_warning.setForeground(WARNING_COLOR);

      pnl.addSeparator();

      if (!install_jar) {
	 bubbles_field = pnl.addFileField("Bubbles Installation Directory",install_path,
		  JFileChooser.DIRECTORIES_ONLY,
		  new InstallDirectoryFilter(),this,null);
	 pnl.add(bubbles_warning);
	 pnl.addSeparator();
       }

      if (getCourseName() == null) {
	 pnl.addBoolean("Automatically Update Bubbles",auto_update,this);
       }
      else {
	 auto_update = false;
       }

      switch (board_language) {
	 case JAVA :
   //	    pnl.addBoolean("Run Eclipse in Foreground",run_foreground,this);
	    break;
	 case PYTHON :
	    break;
	 case REBUS :
	    break;
	 case JS :
	    break;
       }

      pnl.addSeparator();

      switch (board_language) {
	 case JAVA :
	    install_button = pnl.addBottomButton("INSTALL BUBBLES","INSTALL",this);
	    break;
	 case PYTHON :
	    break;
	 case REBUS :
	    break;
	 case JS :
	    break;
       }

      accept_button = pnl.addBottomButton("OK","OK",this);
      pnl.addBottomButton("CANCEL","CANCEL",this);
      pnl.addBottomButtons();

      working_dialog = new JDialog((JFrame) null,"Bubbles Environment Setup",true);
      working_dialog.setContentPane(pnl);
      working_dialog.pack();
      working_dialog.setLocationRelativeTo(null);
    }

   boolean process() {
      checkStatus();
      result_status = false;
      working_dialog.setVisible(true);
      return result_status;
    }

   private void checkStatus() {
      if (eclipse_field != null) {
	 String txt = eclipse_field.getText().trim();
	 if (txt.length() > 0) {
	    File ef = new File(txt);
	    if (!ef.getAbsolutePath().equals(eclipse_directory)) {
	       eclipse_directory = ef.getAbsolutePath();
	       has_changed = true;
	    }
	 }
      }
      if (bubbles_field != null) {
	 String txt = bubbles_field.getText().trim();
	 if (txt.length() > 0) {
	    File inf = new File(txt);
	    if (!inf.getAbsolutePath().equals(install_path)) {
	       install_path = inf.getAbsolutePath();
	       has_changed = true;
	    }
	 }
      }

      switch (board_language) {
	 case JAVA :
	    if (checkEclipse() && eclipse_button != null) {
	       eclipse_button.setEnabled(false);
	     }
	    if (checkEclipse() && checkPlugin() && (install_jar || checkInstall())) {
	       accept_button.setEnabled(true);
	     }
	    else {
	       accept_button.setEnabled(false);
	     }
	    if (checkEclipse() && !checkPlugin() && (install_jar || checkInstall())) {
	       install_button.setEnabled(true);
	     }
	    else {
	       install_button.setEnabled(false);
	     }
	    if (checkEclipse()) {
	       eclipse_warning.setVisible(false);
	     }
	    else {
	       eclipse_warning.setVisible(true);
	     }
	    break;
	 case PYTHON :
	    break;
	 case REBUS :
	    break;
	 case JS :
	    break;
       }

      if (install_jar || checkInstall()) {
	 bubbles_warning.setVisible(false);
       }
      else {
	 bubbles_warning.setVisible(true);
       }
   }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("Eclipse Installation Directory")) {
	 // will update in checkStatus()
       }
      else if (cmd.equals("Bubbles Installation Directory")) {
	 // will update in checkStatus()
       }
      else if (cmd.equals("Automatically Update Bubbles")) {
	 JCheckBox cbx = (JCheckBox) e.getSource();
	 auto_update = cbx.isSelected();
	 has_changed = true;
       }
      else if (cmd.equals("Run Eclipse in Foreground")) {
	 JCheckBox cbx = (JCheckBox) e.getSource();
	 run_foreground = cbx.isSelected();
	 has_changed = true;
       }
      else if (cmd.equals("INSTALL")) {
	 updatePlugin();
	 force_setup = false;
       }
      else if (cmd.equals("ECLIPSE")) {
	 BoardEclipse eclip = new BoardEclipse(new File(jar_directory));
	 String dir = eclip.installEclipse();
	 if (dir != null) {
	    eclipse_field.setText(dir);
	  }
       }
      else if (cmd.equals("OK")) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
      else if (cmd.equals("CANCEL")) {
	 result_status = false;
	 working_dialog.setVisible(false);
       }
      else {
	 BoardLog.logE("BOARD","Unknown SETUP DIALOG command: " + cmd);
       }
      checkStatus();
    }

   @Override public void caretUpdate(CaretEvent e) {
      checkStatus();
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      checkStatus();
    }

}	// end of inner class SetupDialog




private static class EclipseDirectoryFilter extends FileFilter {

   @Override public boolean accept(File f) {
      //return checkEclipseDirectory(f);  //edited by amc6
      return true;
    }

   @Override public String getDescription()	{ return "Eclipse Installation Directory"; }

}	// end of inner class EclipseDirectoryFilter




private static class InstallDirectoryFilter extends FileFilter {

   @Override public boolean accept(File f) {
      //return checkInstallDirectory(f);  //edited by amc6
      return true;
    }

   @Override public String getDescription()	{ return "Bubbles Installation Directory"; }

}	// end of inner class EclipseDirectoryFilter




/********************************************************************************/
/*										*/
/*	Eclipse workspace dialog management					*/
/*										*/
/********************************************************************************/

private class WorkspaceDialog implements ActionListener, KeyListener {

   private JButton accept_button;
   private JDialog working_dialog;
   private boolean result_status;
   private boolean ws_changed;
   private JLabel workspace_warning;
   private JTextField workspace_field;

   WorkspaceDialog() {
      SwingGridPanel pnl = new SwingGridPanel();

      // library might not be set up here -- can't use BoardColors
      // pnl.setBackground(BoardColors.getColor("Buda.Bubbles.Color"));
      pnl.setBackground(WORKSPACE_DIALOG_COLOR);
      pnl.setOpaque(true);

      pnl.beginLayout();
      pnl.addBannerLabel("Bubbles Workspace Setup");

      pnl.addSeparator();

      workspace_field = null;
      workspace_warning = new JLabel("Warning");//added by amc6

      switch (board_language) {
	 default:
	 case JAVA :
	    workspace_field = pnl.addFileField("Eclipse Workspace",default_workspace,
		  JFileChooser.DIRECTORIES_ONLY,
		  new WorkspaceDirectoryFilter(),this,null);
	    workspace_warning.setToolTipText("Not a vaid Eclipse Workspace");
	    break;
	 case PYTHON :
	    workspace_field = pnl.addFileField("Python Workspace",default_workspace,
		  JFileChooser.DIRECTORIES_ONLY,
		  new WorkspaceDirectoryFilter(),this,null);
	    workspace_warning.setToolTipText("Not a vaid Python Workspace");
	    break;
	 case JS :
	    workspace_field = pnl.addFileField("Node/JS Workspace",default_workspace,
		  JFileChooser.DIRECTORIES_ONLY,
		  new WorkspaceDirectoryFilter(),this,null);
	    workspace_warning.setToolTipText("Not a vaid Node/JS Workspace");
	    break;
	 case REBUS :
	    workspace_field = pnl.addFileField("Rebus Workspace",default_workspace,
		  JFileChooser.DIRECTORIES_ONLY,
		  new WorkspaceDirectoryFilter(),this,null);
	    workspace_warning.setToolTipText("Not a vaid Rebus Workspace");
	    break;
       }
      if (workspace_field != null) workspace_field.addKeyListener(this);

      workspace_warning.setForeground(WARNING_COLOR);
      pnl.add(workspace_warning);

      pnl.addSeparator();
      if (recent_workspaces.size() > 0) {
	 List<String> recents = new ArrayList<String>(recent_workspaces);
	 recents.add(0,RECENT_HEADER);
	 pnl.addChoice("Recent Workspaces",recents,0,true,this);
       }

      pnl.addBoolean("Create New Workspace",create_workspace,this);
      pnl.addBoolean("Always Ask for Workspace",ask_workspace,this);

      pnl.addSeparator();
      accept_button = pnl.addBottomButton("OK","OK",this);
      pnl.addBottomButton("CANCEL","CANCEL",this);
      pnl.addBottomButtons();

      working_dialog = new JDialog((JFrame) null,"Bubbles Workspace Setup",true);
      working_dialog.setContentPane(pnl);
      working_dialog.pack();
      working_dialog.setLocationRelativeTo(null);
    }

   boolean process() {
      ws_changed = false;
      checkStatus();
      result_status = false;
      working_dialog.setVisible(true);
      return result_status;
    }

   boolean hasChanged() 			{ return ws_changed; }

   private void checkStatus() {
      if (checkWorkspace()) {
	 accept_button.setEnabled(true);
	 workspace_warning.setVisible(false);
       }
      else {
	 accept_button.setEnabled(false);
	 workspace_warning.setVisible(true);
       }
    }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("Eclipse Workspace") || cmd.equals("Python Workspace") ||
	    cmd.equals("Rebus Workspace") || cmd.equals("Node/JS Workspace")) {
	 JTextField tf = (JTextField) e.getSource();
	 File ef = new File(tf.getText());
	 String np = ef.getPath();
	 if (!np.equals(default_workspace)) ws_changed = true;
	 default_workspace = np;
       }
      else if (cmd.equals("Always Ask for Workspace")) {
	 JCheckBox cbx = (JCheckBox) e.getSource();
	 if (ask_workspace != cbx.isSelected()) ws_changed = true;
	 ask_workspace = cbx.isSelected();
       }
      else if (cmd.equals("Create New Workspace")) {
	 JCheckBox cbx = (JCheckBox) e.getSource();
	 create_workspace = cbx.isSelected();
       }
      else if (cmd.equals("Recent Workspaces")) {
	 JComboBox<?> cbx = (JComboBox<?>) e.getSource();
	 String rslt = (String) cbx.getSelectedItem();
	 if (rslt != null && !rslt.trim().equals("") && !rslt.trim().equals(RECENT_HEADER)) {
	    if (!rslt.equals(default_workspace)) {
	       ws_changed = true;
	       default_workspace = rslt;
	       workspace_field.setText(rslt);
	     }
	  }
       }
      else if (cmd.equals("OK")) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
      else if (cmd.equals("CANCEL")) {
	 result_status = false;
	 working_dialog.setVisible(false);
       }
      else {
	 BoardLog.logE("BOARD","Unknown WORKSPACE DIALOG command: " + cmd);
       }
      checkStatus();
    }

   @Override public void keyPressed(KeyEvent e) {
      BoardLog.logD("BOARD", "KeyEvent handled: " + KeyEvent.getKeyText(e.getKeyCode()));
      if (accept_button.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
   }

   @Override public void keyTyped(KeyEvent e) { }

   @Override public void keyReleased(KeyEvent e) { }

}	// end of inner class SetupDialog



private static class WorkspaceDirectoryFilter extends FileFilter {

   @Override public boolean accept(File f) {
      return true;
    }

   @Override public String getDescription()	{ return "Eclipse Workspace Directory"; }

}	// end of inner class WorkspaceDirectoryFilter




/********************************************************************************/
/*										*/
/*	Proxy management							*/
/*										*/
/********************************************************************************/

private void setupProxy()
{
   BoardProperties bp = BoardProperties.getProperties("Board");
   String plst = bp.getProperty("Board.proxy");
   if (plst == null || plst.length() < 1) return;

   List<Proxy> proxies = new ArrayList<Proxy>();
   for (StringTokenizer tok = new StringTokenizer(plst," \t,"); tok.hasMoreTokens(); ) {
      String prx = tok.nextToken();
      Proxy px = getProxy(prx);
      if (px != null) proxies.add(px);
    }

   if (proxies.size() == 0) return;
   proxies.add(Proxy.NO_PROXY);
   ProxyManager pm = new ProxyManager(proxies);
   ProxySelector.setDefault(pm);
}



private Proxy getProxy(String d)
{
   String [] args = d.split(":");
   Proxy.Type typ;
   if (args.length < 3) {
      if (args[0].startsWith("N") || args[0].equals("*")) return Proxy.NO_PROXY;
      return null;
    }

   InetSocketAddress addr;
   try {
      typ = Proxy.Type.valueOf(args[0]);
      int port = Integer.parseInt(args[2]);
      addr = new InetSocketAddress(args[1],port);
      return new Proxy(typ,addr);
    }
   catch (Throwable t) { }

   return null;
}



private static class ProxyManager extends ProxySelector {

   List<Proxy> proxy_list;
   List<Proxy> null_list;
   Set<String> local_hosts;

   ProxyManager(List<Proxy> pl) {
      proxy_list = pl;
      null_list = new ArrayList<Proxy>();
      null_list.add(Proxy.NO_PROXY);
      local_hosts = new HashSet<String>();
      local_hosts.add("localhost");
      local_hosts.add("0.0.0.0");
      local_hosts.add("127.0.0.1");
      try {
	 InetAddress lh = InetAddress.getLocalHost();
	 local_hosts.add(lh.getHostAddress());
	 local_hosts.add(lh.getHostName());
	 local_hosts.add(lh.getCanonicalHostName());
       }
      catch (IOException e) { }
    }

   @Override public void connectFailed(URI uri,SocketAddress sa,IOException e) {
//	Proxy p = null;
//	for (Proxy px : proxy_list) {
//	 if (px.address().equals(sa)) p = px;
//	 }
    }

   @Override public List<Proxy> select(URI uri) {
      if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
	 String h = uri.getHost();
	 if (local_hosts.contains(h)) return null_list;
	 return proxy_list;
       }

      return null_list;
    }

}	// end of inner class ProxyManager




/********************************************************************************/
/*										*/
/*	Method to report an error to the user					*/
/*										*/
/********************************************************************************/

private void reportError(String msg)
{
   JOptionPane.showMessageDialog(null,msg,"Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
}



}	// end of class BoardSetup




/* end of BoardSetup.java */



