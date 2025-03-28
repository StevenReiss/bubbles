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
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingKey;
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
import javax.swing.event.HyperlinkListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import java.awt.Color;
import java.awt.Dimension;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

public final class BoardSetup implements BoardConstants, MintConstants {




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
private boolean 	force_update;
private boolean 	force_metrics;
private boolean 	ask_workspace;
private boolean 	workspace_given;
private boolean 	run_foreground;
private boolean 	has_changed;
private String		install_path;
private boolean 	install_jar;
private String		jar_file;
private String		jar_directory;
private String		baseide_directory;
private String		default_workspace;
private boolean 	create_workspace;
private boolean 	auto_update;
private boolean 	do_uninstall;
private int		setup_count;
private boolean 	update_setup;
private boolean 	must_restart;
private boolean 	show_splash;
private boolean 	allow_debug;
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
private File		resource_dir;
private BoardLanguage	board_language;
private List<String>	recent_workspaces;
private boolean 	palette_set;
private boolean 	plugin_running;

private static Map<String,HyperlinkListener>	hyperlink_config = new HashMap<>();


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
   run_mode = RunMode.NORMAL;
   mint_name = null;
   mint_control = null;
   course_name = null;
   course_assignment = null;
   library_dir = null;
   resource_dir = null;
   board_language = BoardLanguage.JAVA;
   palette_set = false;
   plugin_running = false;
   force_update = false;

   baseide_directory = system_properties.getProperty(BOARD_PROP_BASE_IDE_DIR);
   if (baseide_directory == null) baseide_directory = system_properties.getProperty(BOARD_PROP_ECLIPSE_DIR);
   if (baseide_directory == null) baseide_directory = System.getProperty("edu.brown.cs.bubbles.eclipse");
   if (baseide_directory == null) baseide_directory = System.getenv("BUBBLES_ECLIPSE");

   default_workspace = system_properties.getProperty(BOARD_PROP_WORKSPACE);
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
	    baseide_directory = args[++i];
	    has_changed = true;
	  }
	 else if (args[i].startsWith("-I") && i+1 < args.length) {     // -IntelliJ <eclipse install directory>
	    baseide_directory = args[++i];
	    setLanguage(BoardLanguage.JAVA_IDEA);
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
	 else {
	    boolean fnd = false;
	    if (args[i].length() >= 3) {
	       String match = args[i].toLowerCase();
	       for (BoardLanguage bl : BoardLanguage.values()) {
		  String arg = bl.getBubblesArg();
		  if (arg != null) {
		     if (arg.startsWith(match)) {
			setLanguage(bl);
			fnd = true;
			break;
		      }
		   }
		}
	     }
	    if (!fnd) badArgs();
	  }
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


/**
 *	Set the flag to force a new bubbles version
 **/

public void setForceUpdate(boolean fg)
{
   force_update = fg;
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
 *	Note that the plugin is already running (don't try to install)
 **/
public void setPluginRunning()
{
   plugin_running = true;
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
   if (args != null) {
      for (String s : args) {
         java_args.add(s);
       }
    }
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
      BoardLog.logX("BOARD","Bad board setup " + bs.default_workspace + " " + bs.baseide_directory + " " +
		       bs.jar_file + " " + bs.jar_directory + " " + bs.install_path + " " +
		       bs.install_jar);
    }

   File pdir = null;

   switch (bs.board_language) {
      case JAVA :
	 File f1 = new File(wsd,".metadata");
	 File f2 = new File(f1,".plugins");
	 pdir = new File(f2,"edu.brown.cs.bubbles.bedrock");
	 break;
      case JAVA_IDEA :
	 pdir = new File(wsd,".idea");
	 pdir = new File(pdir,"bubjet");
	 break;
      default :
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
      BoardLog.logX("BOARD","Bad board setup " + bs.default_workspace + " " + bs.baseide_directory + " " +
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
   if (f == null) {
      return null;
    }

   f = new File(f,item);
   if (!f.exists()) {
      if (item.equals("eclipsejar")) return getLibraryDirectory().getPath();
      File f1 = getLibraryDirectory();
      File f2 = f1.getParentFile();
      File f3 = new File(f2,"dropins");
      File f4 = new File(f3,item);
      if (f4.exists()) f = f4;
    }

   return f.getPath();
}


public String getResourcePath(String item)
{
   File f = getResourceDirectory();
   if (f == null) return null;

   f = new File(f,item);

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



public String getRemoteResourcePath(String item)
{
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();

   File f = getRemoteResourceDirectory();

   if (f == null) return null;
   f = fsv.createFileObject(f,item);
   // f = new File(f,item);

   if (!f.exists()) {
      if (item.equals("eclipsejar")) return getRemoteResourceDirectory().getPath();
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
   if (ejp == null) {		// might occur if we haven't check the installation
      if (!checkInstall()) {
	 BoardLog.logD("BOARD","Attempt to get eclipse path without valid installation");
       }
      ejp = getLibraryPath("eclipsejar");
    }
   File ejr = null;
   if (ejp != null) ejr = new File(ejp);
   if (ejr != null && ejr.exists() && ejr.isDirectory()) {
      for (File nfil : ejr.listFiles()) {
	 if (nfil.getName().startsWith("org.eclipse.") || nfil.getName().startsWith("com.google.")) {
	    if (nfil.getName().endsWith(".jar")) {
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
      if (f4.exists() && f4.isDirectory()) {
	 for (File nfil : f4.listFiles()) {
	    if (nfil.getName().startsWith("org.eclipse.") || nfil.getName().startsWith("com.google.")) {
	       if (nfil.getName().endsWith(".jar")) {
		  if (fnd > 0) buf.append(File.pathSeparator);
		  buf.append(nfil.getPath());
		  ++fnd;
		}
	     }
	  }
       }
    }

   if (fnd == 0) {
      File f1 = ejr.getParentFile().getParentFile();	// /pro/bubbles/lib --> /pro
      File f2 = new File(f1,"ivy");                      // /pro/ivy
      File f3 = new File(f2,"lib");                      // /pro/ivy/lib
      File f4 = new File(f3,"eclipsejar");
      if (f4.exists() && f4.isDirectory()) {
	 for (File nfil : f4.listFiles()) {
	    if (nfil.getName().startsWith("org.eclipse.") || nfil.getName().startsWith("com.google.")) {
	       if (nfil.getName().endsWith(".jar")) {
		  if (fnd > 0) buf.append(File.pathSeparator);
		  buf.append(nfil.getPath());
		  ++fnd;
		}
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

   if (jar_directory == null && install_path == null) checkInstall();

   if (install_jar && jar_directory != null) {
      f = new File(jar_directory);
    }
   else if (install_path != null) {
      f = new File(install_path);
    }
   else {
      BoardLog.logE("BOARD","No library directory found " +
	 install_jar + " " + jar_directory + " " + install_path);
      return null;
    }

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


public File getResourceDirectory()
{
   if (resource_dir != null) return resource_dir;

   File f = null;

   if (install_jar && jar_directory != null) {
      f = new File(jar_directory);
    }
   else if (install_path != null) {
      f = new File(install_path);
    }
   else return null;

   f = new File(f,BOARD_INSTALL_RESOURCES);

   if (!f.exists()) f.mkdir();
   if (!f.exists() || !f.isDirectory()) {
      File f1 = BoardProperties.getPropertyDirectory();
      File f2 = new File(f1,BOARD_INSTALL_RESOURCES);
      f2.mkdirs();
      if (f2.exists() && f2.isDirectory()) f = f2;
    }

   resource_dir = f;

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


public File getRemoteResourceDirectory()
{
   switch (getRunMode()) {
      case SERVER :
      case NORMAL :
	 return getResourceDirectory();
      case CLIENT :
	 break;
    }

   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   if (fsv instanceof BoardFileSystemView) {
      BoardFileSystemView bfsv = (BoardFileSystemView) fsv;
      return bfsv.getRemoteResourceDirectory();
    }

   return getResourceDirectory();
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
      BoardProperties.setPropertyDirectory(bl.getPropertyDirectory());
    }

   system_properties = BoardProperties.getProperties("System");
   auto_update = system_properties.getBoolean(BOARD_PROP_AUTO_UPDATE,auto_update);
   baseide_directory = system_properties.getProperty(BOARD_PROP_ECLIPSE_DIR,baseide_directory);
   baseide_directory = system_properties.getProperty(BOARD_PROP_BASE_IDE_DIR,baseide_directory);
   default_workspace = system_properties.getProperty(BOARD_PROP_WORKSPACE,default_workspace);
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

   BoardLog.logD("BOARD","Set splash task " + id);
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


public void noteNamesLoaded(int ct,int max)
{
   if (splash_screen != null) {
      String s = splash_screen.getCurrentTask();
      if (s != null && s.startsWith("Load Project Symbols")) {
	 String txt = "Load Project Symbols: " + ct;
	 if (max != 0) {
	    int val = (ct*100)/max;
	    txt = "Load Project Symbols: " + val + "%";
	  }
	 setSplashTask(txt);
       }
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
      if (wsname.endsWith(File.separator)) wsname = wsname.substring(0,wsname.length()-1);
      int idx = wsname.lastIndexOf(File.separator);
      if (idx > 0) {
	 wsname = wsname.substring(idx+1);
       }
      if (wsname == null) wsname = "";
      else wsname = wsname.replace(" ","_");
      switch (getLanguage()) {
         case JAVA :
            break;
         default :
            wsname = getLanguage().toString() + "_" + wsname;
            break;
       }
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
/*	Hyperlink methods							*/
/*										*/
/********************************************************************************/

public static void addHyperlinkListener(String protocol,HyperlinkListener hl)
{
   hyperlink_config.put(protocol,hl);
}



public static HyperlinkListener getListenerForProtocol(String protocol)
{
   return hyperlink_config.get(protocol);
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
   auto_update = true;

   if (!checkInstall()) return;

   if (baseide_directory == null && board_language == BoardLanguage.JAVA) {
      baseide_directory = System.getProperty("edu.brown.cs.bubbles.eclipse");
      if (baseide_directory == null) baseide_directory = System.getenv("BUBBLES_ECLIPSE");
      if (!checkEclipse()) baseide_directory = null;
      if (baseide_directory != null) saveProperties();
    }
   else if (board_language == BoardLanguage.JAVA_IDEA) {
      baseide_directory = System.getProperty("edu.brown.cs.bubbles.idea");
      if (baseide_directory == null) baseide_directory = System.getenv("BUBBLES_IDEA");
      if (!checkIntelliJ()) baseide_directory = null;
      if (baseide_directory != null) saveProperties();
    }
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Entry point for doing setup for a bubbles applications.  Any application should
 *	first construct an instance of BoardSetup and then call this method.  Once the
 *	method returns, bubbles should be ready to use.
 **/

// CHECKSTYLE:OFF
public boolean doSetup()
// CHECKSTYLE:ON
{
   if (do_uninstall) {
      uninstall();
      return false;
    }

   if (course_name != null) auto_update = false;

   boolean firsttime = checkDefaultInstallation();
   if (firsttime && baseide_directory == null && board_language == BoardLanguage.JAVA) {
      baseide_directory = System.getProperty("edu.brown.cs.bubbles.eclipse");
      if (baseide_directory == null) baseide_directory = System.getenv("BUBBLES_ECLIPSE");
      if (!checkEclipse()) baseide_directory = null;
    }
   if (firsttime && baseide_directory == null && board_language == BoardLanguage.JAVA_IDEA) {
      baseide_directory = System.getProperty("edu.brown.cs.bubbles.idea");
      if (baseide_directory == null) baseide_directory = System.getenv("BUBBLES_IDEA");
      if (!checkIntelliJ()) baseide_directory = null;
    }

   if (show_splash && splash_screen == null && !firsttime) {
      splash_screen = new BoardSplash();
      splash_screen.start();
    }

   boolean thru = (setup_count != 0 && default_workspace != null);
   switch (board_language) {
      case JAVA :
      case JAVA_IDEA :
	 thru &= baseide_directory != null;
	 break;
      default :
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
	 needsetup |= !checkEclipsePlugin();
	 break;
      case JAVA_IDEA :
	 needsetup |= !checkIntelliJ();
	 needsetup |= !checkIdeaPlugin();
	 break;
      default :
	 break;
    }
   needsetup |= !checkInstall() && !install_jar;

   askworkspace |= !checkWorkspace();

   BoardLog.logD("BOARD","In setup " + needsetup + " " + install_jar + " " + update_setup);

   if (install_jar && !update_setup) {
      setSplashTask("Checking for newer version");
      if (update_proxy != null) BoardUpdate.setupProxy(update_proxy);
      try {
	 if (force_update) BoardUpdate.forceUpdate(jar_file,java_args);
	 else if (auto_update) BoardUpdate.checkUpdate(jar_file,java_args);
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

   BoardLog.logD("BOARD","In setup " + default_workspace);

   if (!checkPalette()) {
      BoardLog.logE("BOARD","BUBBLES: Setup aborted by palette dialog");
      System.exit(1);
    }

   checkKeyDefs();

   switch (board_language) {
      case JAVA :
	 boolean needupdate = force_setup | update_setup;
	 needupdate |= !checkDates();
	 if (needupdate && !plugin_running) {
	    setSplashTask("Updating Eclipse plugin");
	    updateEclipsePlugin();
	  }
	 break;
      case JAVA_IDEA :
	 needupdate = force_setup | update_setup;
	 needupdate |= !checkDates();
	 if (needupdate && !plugin_running) {
	    setSplashTask("Updating Intellij plugin");
	    updateIdeaPlugin();
	  }
	 break;
      default :
	 break;
    }

   if (install_jar) {
      URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN_ECLIPSE);
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
      system_properties.setProperty(BOARD_PROP_WORKSPACE,default_workspace);
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
   if (checkEclipsePlugin()) {
      File pins = getPluginDirectory();
      if (pins == null) return;
      File pin = new File(pins,BOARD_BUBBLES_PLUGIN_ECLIPSE);
      pin.delete();
    }
   if (checkIdeaPlugin()) {
      File pins = getPluginDirectory();
      if (pins == null) return;
      File p1 = new File(pins,"bubbles");
      try {
	 IvyFile.remove(p1);
       }
      catch (IOException e) { }
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
   if (baseide_directory != null) {
      system_properties.setProperty(BOARD_PROP_BASE_IDE_DIR,baseide_directory);
      system_properties.remove(BOARD_PROP_ECLIPSE_DIR);
    }
   if (default_workspace != null) {
      system_properties.setProperty(BOARD_PROP_WORKSPACE,default_workspace);
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
      String vmo = board_language.getVMOptions();
      if (vmo != null) system_properties.setProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS,vmo);
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

private boolean checkBaseIde()
{
   switch (board_language) {
      case JAVA :
	 return checkEclipse();
      case JAVA_IDEA :
	 return checkIntelliJ();
      default :
	 break;
    }
   return true;
}



private boolean checkEclipse()
{
   if (baseide_directory == null) return false;
   File ed = new File(baseide_directory);
   if (checkEclipseDirectory(ed)) return true;

   for (String s : BOARD_ECLIPSE_START) {
      if (ed.getName().equals(s) && ed.canExecute()) {
	 File par = ed.getParentFile();
	 if (checkEclipseDirectory(par)) {
	    baseide_directory = par.getAbsolutePath();
	    return true;
	  }
       }
    }
   return false;
}


private boolean checkIntelliJ()
{
   if (baseide_directory == null) return false;
   File id = new File(baseide_directory);
   if (checkIntelliJDirectory(id)) return true;
   File f1 = new File(baseide_directory,"Contents");
   if (checkIntelliJDirectory(f1)) {
      baseide_directory = f1.getAbsolutePath();
      return true;
    }

   for (String s : BOARD_IDEA_START) {
      if (id.getName().equals(s) && id.canExecute()) {
	 File par = id.getParentFile();
	 if (checkIntelliJDirectory(par)) {
	    baseide_directory = par.getAbsolutePath();
	    return true;
	  }
       }
    }
   return false;
}



private boolean checkEclipsePlugin()
{
   if (!checkBaseIde()) return false;

   File pins = getPluginDirectory();
   if (pins == null) return false;
   File pin = new File(pins,BOARD_BUBBLES_PLUGIN_ECLIPSE);
   if (!pin.exists() || !pin.canRead()) return false;

   return true;
}


private boolean checkIdeaPlugin()
{
   if (!checkBaseIde()) return false;
   File pins = getPluginDirectory();
   File b1 = new File(pins,"bubbles");
   File b2 = new File(b1,"lib");
   File b3 = new File(b2,BOARD_BUBBLES_PLUGIN_IDEA);
   if (!b3.exists() || !b3.canRead()) return false;
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

   File pdf = getIdePluginDirectory(ed);
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



static boolean checkIntelliJDirectory(File id)
{
   if (!id.exists() || !id.isDirectory()) return false;

   boolean execfnd = false;
   File f1 = new File(id,"MacOS");
   File f2 = new File(f1,"idea");
   if (f2.exists() && f2.canExecute()) execfnd = true;
   f1 = new File(id,"bin");
   f2 = new File(f1,"idea");
   if (f2.exists() && f2.canExecute()) execfnd = true;
   if (!execfnd) return false;
   // handle windows

   File pdf = getIdeaPluginDirectory(id);
   if (pdf == null || !pdf.exists() || !pdf.isDirectory() || !pdf.canWrite())
      return false;

   // check for proper version of intellij IDEA

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

public boolean checkInstall()
{
   // first check if we are running from a complete jar

   boolean ok = true;
   try {
      InputStream ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN_ECLIPSE);
      if (ins == null) ok = false;
      else ins.close();
      for (String s : BOARD_RESOURCE_PROPS) {
	 if (!ok) break;
	 ins = getClass().getClassLoader().getResourceAsStream("resources/" + s);
//	 if (ins == null) ins = getClass().getClassLoader().getResourceAsStream(s);
	 if (ins == null) {
	    ok = false;
	    BoardLog.logD("BOARD","File not found in jar " + s);
	  }
	 else ins.close();
       }
      if (ok) {
	 URL url = BoardImage.class.getClassLoader().getResource(BOARD_RESOURCE_CHECK);
	 if (url == null || !url.toString().startsWith("jar")) {
	    BoardLog.logE("BOARD","Problem accessing resources: " + url);
	    ok = false;
	  }
       }
      install_jar = ok;

      if (install_jar) {
	 BoardLog.logI("BOARD","Running from jar file");
	 URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN_ECLIPSE);
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

	 Path jarpath = f.toPath();
	 List<String> links = new ArrayList<>();
	 links.add("cloudbb.jar");
	 for (BoardLanguage bl : BoardLanguage.values()) {
	    String jar = bl.getJarRunner();
	    if (jar != null && !jar.equals("bubbles.jar")) links.add(jar);
	  }
	 for (String link : links) {
	    File linkf = new File(f.getParentFile(),link);
	    Path linkp = linkf.toPath();
	    try {
	       if (!linkf.exists()) Files.createSymbolicLink(linkp,jarpath);
	     }
	    catch (Exception e) { }
	  }

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
   File resb = new File(ind,BOARD_INSTALL_RESOURCES);
   if (!resb.exists() || !resb.isDirectory()) return false;
   File binb = new File(ind,BOARD_INSTALL_BINARY);
   if (!binb.exists() || !binb.isDirectory()) return false;
   File inf = new File(libb,BOARD_RESOURCE_PLUGIN_ECLIPSE);
   if (!inf.exists() || !inf.canRead()) return false;
   File elib = new File(ind,"eclipsejar");
   if (!elib.exists() || !elib.canRead()) return false;

   for (String s : BOARD_RESOURCE_PROPS) {
      inf = new File(resb,s);
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
	 BoardLog.logX("BOARD","Missing extra library file " + inf);
       }
    }
   for (String s : BOARD_RESOURCE_EXTRAS) {
      s = s.replace("/",File.separator);
      inf = new File(resb,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing extra resource file " + inf);
       }
    }
   for (String s : BOARD_BINARY_FILES) {
      s = s.replace("/",File.separator);
      inf = new File(binb,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing binary file " + inf);
       }
    }
   File rest = new File(resb,"templates");
   for (String s : BOARD_TEMPLATES) {
      inf = new File(rest,s);
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
	 if (ins == null) {
	    ins = BoardSetup.class.getClassLoader().getResourceAsStream("resources/" + s);
	  }
	 if (ins == null) {
	    ins = BoardSetup.class.getClassLoader().getResourceAsStream(s);
	  }

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
   File resd = getResourceDirectory();
   if (!resd.exists()) resd.mkdir();

   for (String s : BOARD_LIBRARY_FILES) {
      must_restart |= extractLibraryResource(s,libd,update_setup);
    }

   for (String s : BOARD_LIBRARY_EXTRAS) {
      extractLibraryResource(s,libd,auto_update);
    }

   for (String s : BOARD_RESOURCE_EXTRAS) {
      extractResourceFile(s,resd,auto_update);
    }

   File rest = new File(resd,"templates");
   if (!rest.exists()) rest.mkdir();
   for (String s : BOARD_TEMPLATES) {
      extractLibraryResource(s,rest,auto_update);
    }

   extractNobbles(libd,update_setup);
}


private void checkBinFiles()
{
   if (!auto_update) return;

   File libd = getLibraryDirectory();
   File libp = libd.getParentFile();
   File bind = new File(libp,BOARD_INSTALL_BINARY);
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


private boolean extractResourceFile(String s,File resd,boolean force)
{
   boolean upd = false;

   String xs = s;
   int idx = s.lastIndexOf("/");
   if (idx >= 0) {
      xs = s.replace('/',File.separatorChar);
      String hd = xs.substring(0,idx);
      File fd = new File(resd,hd);
      if (!fd.exists()) fd.mkdirs();
    }

   File f1 = new File(resd,xs);
   if (!force && f1.exists()) return false;
   upd = true;

   try {
      InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream("resources/" + s);
      if (ins != null) {
	 FileOutputStream ots = new FileOutputStream(f1);
	 copyFile(ins,ots);
       }
    }
   catch (IOException e) {
      if (f1.exists()) upd = false;			// use old version if necessary
      else {
	 BoardLog.logE("BOARD","Problem setting up jar resource file " + s + ": " + e,e);
	 reportError("Problem setting up jar resource file " + s + ": " + e);
	 System.exit(1);
       }
    }

   return upd;
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
	 BoardLog.logE("BOARD","Problem setting up lib resource " + s + " (" +
	       lbv + "," + install_path + ")",e);
	 reportError("Problem setting up lib resource " + s + ": " + e);
	 System.exit(1);
       }
    }
}


public void checkResourceFile(String resfile) throws IOException
{
   File ivv = BoardProperties.getPropertyDirectory();
   if (!ivv.exists()) ivv.mkdir();
   File f1 = new File(ivv,resfile);

// File res1 = new File(install_path,BOARD_INSTALL_RESOURCES);
   File res = getResourceDirectory();
   File f2 = new File(res,resfile);

   if (!f2.exists() || !f2.canRead()) {
      // handle old installs
      File lbv = new File(install_path,BOARD_INSTALL_LIBRARY);
      f2 = new File(lbv,resfile);
    }
   InputStream ins = new FileInputStream(f2);
   if (f1.exists()) {
      try {
         ensurePropsDefined(resfile,ins);
       }
      catch (Exception e) {
         IvyLog.logE("BOARD","Problem loading resource file " + f2);
       }
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



private boolean checkWorkspaceDirectory(File wsd,boolean create)
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

   switch (board_language) {
      case JAVA :
	 File df = new File(wsd,BOARD_ECLIPSE_WS_DATA);
	 if (!df.exists() || !df.canRead()) return false;
	 break;
      case JAVA_IDEA :
	 df = new File(wsd,BOARD_IDEA_WS_DATA);
	 if (!df.exists() || !df.canRead()) return false;
	 break;
      default :
	 break;
    }

   return true;
}



private boolean checkDates()
{
   long dlm = 0;

   if (install_jar) {
      try {
	 URL u = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN_ECLIPSE);
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
      File inf = new File(libb,BOARD_RESOURCE_PLUGIN_ECLIPSE);
      dlm = inf.lastModified();
    }

   File pdf = getPluginDirectory();
   File bdf = new File(pdf,BOARD_BUBBLES_PLUGIN_ECLIPSE);
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



private void checkKeyDefs()
{
   if (getRunMode() == RunMode.SERVER) return;

   File props = getPropertyBase();
   File keys = new File(props,KEY_DEFINITIONS);
   try {
      SwingKey.loadKeyDefinitions(keys);
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem loading key definitions",e);
    }
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
	 JCheckBox btn = (JCheckBox) evt.getSource();
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

private void updateEclipsePlugin()
{
   if (plugin_running) return;

   File pdf = null;
   File bdf = null;

   try {
      InputStream ins = null;
      if (install_jar) {
	 ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN_ECLIPSE);
	 if (ins == null) return;
       }
      else {
	 File ind = new File(install_path);
	 File libb = new File(ind,BOARD_INSTALL_LIBRARY);
	 File inf = new File(libb,BOARD_RESOURCE_PLUGIN_ECLIPSE);
	 ins = new FileInputStream(inf);
       }
      pdf = getPluginDirectory();
      bdf = new File(pdf,BOARD_BUBBLES_PLUGIN_ECLIPSE);
      BoardLog.logI("BOARD","Updating plugin " + bdf);
      OutputStream ots = new FileOutputStream(bdf);

      copyFile(ins,ots);

      system_properties.setProperty(BOARD_PROP_ECLIPSE_CLEAN,true);
    }
   catch (IOException e) {
      String msg = "Problem updating bubble eclipse plugin: ";
      if (pdf  == null) msg += " no plugin directory";
      else if (bdf == null) msg += " no plugin file";
      else {
	 msg += pdf.canWrite() + " " + bdf.canWrite() + " " + pdf.exists() + " " +
	    pdf.canExecute() + " " + bdf.canRead();
       }
      BoardLog.logE("BOARD",msg,e);
      if (bdf != null && bdf.exists()) return;		// continue if it exists
      // otherwise exit if we can't install the plugin
      reportError("Problem updating bubble eclipse plugin: " + e);
      System.exit(3);
    }
}


private void updateIdeaPlugin()
{
   if (plugin_running) return;

   File pdir = getPluginDirectory();
   File p1 = new File(pdir,"bubbles");
   File p2 = new File(p1,"lib");
   p2.mkdirs();

   try {
      InputStream ins = null;
      InputStream iins = null;
      if (install_jar) {
	 ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN_IDEA);
	 iins = getClass().getClassLoader().getResourceAsStream("ivy.jar");
	 if (ins == null) return;
       }
      else {
	 File ind = new File(install_path);
	 File libb = new File(ind,BOARD_INSTALL_LIBRARY);
	 File inf = new File(libb,BOARD_RESOURCE_PLUGIN_IDEA);
	 File iinf = new File(libb,"ivy.jar");
	 ins = new FileInputStream(inf);
	 iins = new FileInputStream(iinf);
       }
      File bdf = new File(p2,BOARD_BUBBLES_PLUGIN_IDEA);
      BoardLog.logI("BOARD","Updating plugin " + bdf);
      OutputStream os = new FileOutputStream(bdf);
      copyFile(ins,os);
      File ibdf = new File(p2,"ivy.jar");
      OutputStream ios = new FileOutputStream(ibdf);
      copyFile(iins,ios);
      system_properties.setProperty(BOARD_PROP_ECLIPSE_CLEAN,true);
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem installing idea plugin",e);
    }

}




private File getPluginDirectory()
{
   return getIdePluginDirectory(new File(baseide_directory));
}



private static File getIdePluginDirectory(File edf)
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
	 return getEclipsePluginDirectory(edf);
      case JAVA_IDEA :
	 return getIdeaPluginDirectory(edf);
      default :
	 break;
    }

   return null;
}



private static File getEclipsePluginDirectory(File edf)
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


private static File getIdeaPluginDirectory(File edf)
{
   File iddf =	new File(edf,BOARD_INTELLIJ_MAC_PLUGIN);
   File ipdf = checkIdeaPluginDirectory(iddf);
   if (ipdf != null) return ipdf;
   ipdf = checkIdeaPluginDirectory(edf);
   if (ipdf != null) return ipdf;

   return null;
}




private static File checkPluginDirectory(File base)
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
	 return checkEclipsePluginDirectory(base);
      case JAVA_IDEA :
	 return checkIdeaPluginDirectory(base);
      default :
	 break;
    }
   return null;
}



private static File checkEclipsePluginDirectory(File base)
{
   if (!base.exists() || !base.isDirectory()) return null;

   File pdf1 = new File(base,BOARD_ECLIPSE_DROPINS);
   File pdf2 = new File(base,BOARD_ECLIPSE_PLUGINS);
   if (pdf1.exists() && pdf1.isDirectory()) {
      File pin = new File(pdf1,BOARD_BUBBLES_PLUGIN_ECLIPSE);
      if (pin.exists() && pin.canRead()) return pdf1;
    }

   if (pdf2.exists() && pdf2.isDirectory()) {
      File pin = new File(pdf2,BOARD_BUBBLES_PLUGIN_ECLIPSE);
      if (pin.exists() && pin.canRead()) return pdf2;
    }

   if (pdf1.exists() && pdf1.isDirectory() && pdf1.canWrite()) return pdf1;
   if (pdf2.exists() && pdf2.isDirectory() && pdf2.canWrite()) return pdf2;
   if (pdf1.exists() && pdf1.isDirectory()) return pdf1;
   if (pdf2.exists() && pdf2.isDirectory()) return pdf2;

   return null;
}



private static File checkIdeaPluginDirectory(File base)
{
   if (!base.exists() || !base.isDirectory()) return null;

   File pdf1 = new File(base,"plugins");
   if (pdf1.exists() && pdf1.isDirectory()) {
      File pin = new File(pdf1,"bubbles");
      File pin1 = new File(pin,"lib");
      File pin2 = new File(pin1,BOARD_BUBBLES_PLUGIN_IDEA);
      if (pin2.exists() && pin2.canRead()) return pdf1;
    }

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
   cp.append(File.pathSeparator);
   cp.append(getEclipsePath());

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
      if (plugin_running) args.add(idx++,"-insnobed");
      if (force_metrics) args.add(idx++,"-collect");
      if (course_name != null) {
	 String cnm = course_name;
	 if (course_assignment != null) cnm += "@" + course_assignment;
	 args.add(idx++,"-course");
	 args.add(idx++,cnm);
       }
      String arg = board_language.getBubblesArg();
      if (arg != null) args.add(idx++,arg);

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
   String arg = board_language.getBubblesArg();
   if (arg != null) args.add(arg);
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
      InputStream ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN_ECLIPSE);
      if (ins == null) jarok = false;
      else ins.close();
    }
   catch (IOException e) {
      jarok = false;
    }

   File f = null;

   if (jarok) {
      URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN_ECLIPSE);
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
	 f = IvyFile.getCanonical(f);
	 if (!f.exists()) f = null;
       }
    }

   if (f == null) return firsttime;

   jar_file = f.getPath();
   jar_directory = f.getParent();
   install_jar = true;

   if (baseide_directory == null) {
      File fe = new File(jar_directory,"eclipse");
      File fe1 = null;
      if (f.getParentFile() != null) fe1 = new File(f.getParentFile().getParentFile(),"eclipse");
      if (checkEclipseDirectory(fe)) {
	 baseide_directory = fe.getPath();
	 firsttime = false;
       }
      else if (fe1 !=  null && checkEclipseDirectory(fe1)) {
	 baseide_directory = fe1.getPath();
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
   private JLabel baseide_warning;
   private JLabel bubbles_warning;
   private JTextField baseide_field;
   private JTextField bubbles_field;
   private JButton baseide_button;

   SetupDialog() {
      baseide_field = null;
      bubbles_field = null;
      baseide_button = null;
      SwingGridPanel pnl = new SwingGridPanel();

   // BoardColors.setColors(pnl,"Buda.Bubbles.Color");
      BoardColors.setColors(pnl,new Color(211,232,248));
      pnl.setOpaque(true);

      pnl.beginLayout();
      pnl.addBannerLabel("Bubbles Environment Setup");

      pnl.addSeparator();

      switch (board_language) {
	 case JAVA :
	    baseide_field = pnl.addFileField("Eclipse Installation Directory",baseide_directory,
		  JFileChooser.DIRECTORIES_ONLY,
		  new BaseIDEDirectoryFilter("Eclipse"),this,this,null);

	    baseide_warning = new JLabel("Warning!");  //edited by amc6
	    baseide_warning.setToolTipText("<html>Not a valid <b>Eclipse for Java Developers</b> installation " +
		  "directory.<br>(This should be the directory containing the eclipse binary " +
		  "and the plugins directory.)");
	    baseide_warning.setForeground(WARNING_COLOR);
	    pnl.add(baseide_warning);
	    if (baseide_directory == null && install_jar) {
	       // eclipse_button = pnl.addBottomButton("INSTALL ECLIPSE","ECLIPSE",this);
	     }
	    break;
	 case JAVA_IDEA :
	    baseide_field = pnl.addFileField("IntelliJ IDEA Installation Directory",baseide_directory,
		  JFileChooser.DIRECTORIES_ONLY,
		  new BaseIDEDirectoryFilter("IntelliJ IDEA"),this,this,null);
	    baseide_warning = new JLabel("Warning!");  //edited by amc6
	    baseide_warning.setToolTipText("<html>Not a valid <b>IntelliJ IDEA</b> installation " +
		  "directory.<br>(This should be the directory containing the idea binary " +
		  "and the plugins directory.)");
	    baseide_warning.setForeground(WARNING_COLOR);
	    pnl.add(baseide_warning);
	    break;
	 default :
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

      pnl.addSeparator();

      switch (board_language) {
	 case JAVA :
	 case JAVA_IDEA :
	    install_button = pnl.addBottomButton("INSTALL BUBBLES","INSTALL",this);
	    break;
	 default :
	    break;
       }

      accept_button = pnl.addBottomButton("OK","OK",this);
      pnl.addBottomButton("CANCEL","CANCEL",this);
      pnl.addBottomButtons();
      Dimension r = pnl.getPreferredSize();
      r.width = Math.max(r.width,500);
      pnl.setPreferredSize(r);

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
      if (baseide_field != null) {
	 String txt = baseide_field.getText().trim();
	 if (txt.length() > 0) {
	    File ef = new File(txt);
	    if (!ef.getAbsolutePath().equals(baseide_directory)) {
	       baseide_directory = ef.getAbsolutePath();
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
	    if (checkEclipse() && baseide_button != null) {
	       baseide_button.setEnabled(false);
	     }
	    if (checkEclipse() && checkEclipsePlugin() && (install_jar || checkInstall())) {
	       accept_button.setEnabled(true);
	     }
	    else {
	       accept_button.setEnabled(false);
	     }
	    if (checkEclipse() && !checkEclipsePlugin() && (install_jar || checkInstall())) {
	       install_button.setEnabled(true);
	     }
	    else {
	       install_button.setEnabled(false);
	     }
	    if (checkEclipse()) {
	       baseide_warning.setVisible(false);
	     }
	    else {
	       baseide_warning.setVisible(true);
	     }
	    break;
	 case JAVA_IDEA :
	    if (checkIntelliJ() && checkIdeaPlugin() && (install_jar || checkInstall())) {
	       accept_button.setEnabled(true);
	     }
	    else {
	       accept_button.setEnabled(false);
	     }
	    if (checkIntelliJ() && !checkIdeaPlugin() && (install_jar || checkInstall())) {
	       install_button.setEnabled(true);
	     }
	    else {
	       install_button.setEnabled(false);
	     }
	    if (checkIntelliJ()) {
	       baseide_warning.setVisible(false);
	     }
	    else {
	       baseide_warning.setVisible(true);
	     }
	    break;
	 default :
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
	 switch (board_language) {
	    case JAVA_IDEA :
	       updateIdeaPlugin();
	       break;
	    case JAVA :
	       updateEclipsePlugin();
	       break;
	    default :
	       break;
	  }
	 force_setup = false;
       }
      else if (cmd.equals("ECLIPSE")) {
	 BoardEclipse eclip = new BoardEclipse(new File(jar_directory));
	 String dir = eclip.installEclipse();
	 if (dir != null) {
	    baseide_field.setText(dir);
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




private static class BaseIDEDirectoryFilter extends FileFilter {

   private String for_system;

   BaseIDEDirectoryFilter(String sys) {
      for_system = sys;
    }

   @Override public boolean accept(File f) {
      //return checkEclipseDirectory(f);  //edited by amc6
      return true;
    }

   @Override public String getDescription()	{ return for_system + " Installation Directory"; }

}	// end of inner class EclipseDirectoryFilter




private static final class InstallDirectoryFilter extends FileFilter {

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
      BoardLog.logD("BOARD","Asking for workspace");
      SwingGridPanel pnl = new SwingGridPanel();
   
      // library might not be set up here -- can't use BoardColors
      // pnl.setBackground(BoardColors.getColor("Buda.Bubbles.Color"));
      pnl.setBackground(WORKSPACE_DIALOG_COLOR);
      pnl.setOpaque(true);
   
      pnl.beginLayout();
      pnl.addBannerLabel("Bubbles Workspace Setup");
   
      pnl.addSeparator();
   
      workspace_field = null;
      workspace_warning = new JLabel("Warning");
   
      String lbl = board_language.getWorkspaceLabel();
      workspace_field = pnl.addFileField(lbl,default_workspace,
            JFileChooser.DIRECTORIES_ONLY,
            new WorkspaceDirectoryFilter(),this,null);
      workspace_field.setActionCommand("WORKSPACE");
      workspace_field.addKeyListener(this);
      workspace_warning.setToolTipText("Not a valid " + lbl);
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
      Dimension d = pnl.getPreferredSize();
      d.width = Math.max(d.width,500);
      pnl.setPreferredSize(d);
   
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
      if (cmd.equals("WORKSPACE") || cmd.equals(board_language.getWorkspaceLabel())) {
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
      if (accept_button.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
   }

   @Override public void keyTyped(KeyEvent e) { }

   @Override public void keyReleased(KeyEvent e) { }

}	// end of inner class SetupDialog



private static final class WorkspaceDirectoryFilter extends FileFilter {

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

   private List<Proxy> proxy_list;
   private List<Proxy> null_list;
   private Set<String> local_hosts;

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



