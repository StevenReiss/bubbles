/********************************************************************************/
/*										*/
/*		BemaMain.java							*/
/*										*/
/*	Bubbles Environment Main Application main program			*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bema;


import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bdoc.BdocFactory;
import edu.brown.cs.bubbles.bedu.BeduFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardConstants.BoardLanguage;
import edu.brown.cs.bubbles.board.BoardConstants.RunMode;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingKey;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;



/**
 *	Bubbles main program.
 **/

public class BemaMain implements BemaConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

/**
 *	Starting point for the bubbles environment.
 **/

public static void main(String [] args)
{
   BemaMain bm = new BemaMain(args);

   if (System.getProperty("os.name").startsWith("Mac")) {
      try {
	 UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
       }
      catch (Throwable t) {
	 System.err.println("BEMA: Problem setting l&f: " + t);
       }
    }

   bm.start();
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	restore_session;
private boolean 	force_setup;
private boolean 	force_metrics;
private boolean 	force_update;
private boolean 	skip_setup;
private boolean 	skip_splash;
private boolean 	allow_debug;
private boolean 	use_web;
private boolean 	use_cloud;
private Boolean 	auto_update;
private String		use_workspace;
private boolean 	new_workspace;
private Boolean 	ask_workspace;
private Element 	load_config;
private String []	java_args;
private RunMode 	run_mode;
private String		course_name;
private BoardLanguage	for_language;
private String		palette_name;
private boolean 	install_only;
private boolean 	no_bedrock;

private static Map<String,ClassLoader> class_loaders = new HashMap<>();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BemaMain(String [] args)
{
   restore_session = true;
   force_setup = false;
   force_metrics = false;
   force_update = false;
   skip_setup = false;
   skip_splash = false;
   allow_debug = false;
   use_workspace = null;
   new_workspace = false;
   ask_workspace = null;
   use_web = false;
   use_cloud = false;
   java_args = args;
   run_mode = RunMode.NORMAL;
   course_name = null;
   for_language = null;
   install_only = false;
   no_bedrock = false;
   auto_update = null;
   palette_name = null;
   
   System.setProperty("derby.stream.error.file","/dev/null");
   System.setProperty("derby.stream.error.field","edu.brown.cs.ivy.file.IvyDatabase.NULL_STREAM");
   
   checkDefaultLanguage();

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   int ln = args.length;

   for (int i = 0; i < ln; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-nosetup")) {                  // -nosetup
	    skip_setup = true;
	  }
	 else if (args[i].startsWith("-nor")) {                 // -norestore
	    restore_session = false;
	  }
	 else if (args[i].startsWith("-u")) {                   // -update
	    auto_update = true;
	  }
	 else if (args[i].startsWith("-nou")) {                 // -noupdate
	    auto_update = false;
	  }
	 else if (args[i].startsWith("-f")) {                   // -force
	    force_setup = true;
	  }
	 else if (args[i].startsWith("-F")) {                   // -FORCEUPDATE
	    force_update = true;
	  }
	 else if (args[i].startsWith("-course") && i+1 < ln) {  // -course <course>
	    course_name = args[++i];
	    File fa = new File(System.getProperty("user.home"));
	    fa = new File(fa,".suds" + course_name);
	    // Don't use IvyFile here since it trys to use ivy without initializing it
	    BoardProperties.setPropertyDirectory(fa.getPath());
	  }
	 else if (args[i].startsWith("-cl")) {                  // -cloud
	    useCloud();
	  }
	 else if (args[i].startsWith("-collect")) {             // -collect
	    force_metrics = true;
	  }
	 else if (args[i].startsWith("-w") && i+1 < ln) {       // -workspace <ws>
	    use_workspace = args[++i];
	  }
	 else if (args[i].startsWith("-n")) {                   // -new
	    new_workspace = true;
	    ask_workspace = true;
	  }
	 else if (args[i].startsWith("-a")) {                   // -askworkspace
	    ask_workspace = true;
	  }
	 else if (args[i].startsWith("-nosp")) {                // -nosplash
	    skip_splash = true;
	  }
	 else if (args[i].startsWith("-prop") && i+1 < ln) {    // -prop <propdir>
	    BoardProperties.setPropertyDirectory(args[++i]);
	  }
	 else if (args[i].startsWith("-Debug")) {               // -Debug
	    allow_debug = true;
	  }
	 else if (args[i].startsWith("-m") && i+1 < ln) {       // -msg <id>
	    System.setProperty("edu.brown.cs.bubbles.MINT",args[++i]);
	  }
	 else if (args[i].startsWith("-W")) {                   // -WEB
	    use_web = true;
	  }
	 else if (args[i].startsWith("-C")) {                   // -Client
	    run_mode = RunMode.CLIENT;
	  }
	 else if (args[i].startsWith("-S")) {                   // -Server
	    run_mode = RunMode.SERVER;
	    skip_splash = true;
	    restore_session = false;
	  }
	 else if (args[i].startsWith("-Dfile.encoding")) ;
	 else if (args[i].startsWith("-r")) {                   // -restore
	    restore_session = true;
	  }
	 else if (args[i].startsWith("-install")) {             // -install - force installation
	    install_only = true;
	  }
	 else if (args[i].startsWith("-insnobed")) {            // -insnobed -- install w/o bedrock
	    no_bedrock = true;
	  }
	 else if (args[i].startsWith("-pal") && i+1 < ln) {     // -palette <file>
	    palette_name = args[++i];
	  }
	 else if (args[i].startsWith("-inv")) {                 // -inverse
	    if (palette_name == null) palette_name = "inverse_bubbles.palette";
	    else if (palette_name.startsWith("inverse_")) {
	       palette_name = palette_name.substring(8);
	     }
	    else palette_name = "inverse_" + palette_name;
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
      else if (args[i].equals("")) ;
      else if (args[i].equals("edu.brown.cs.bubbles.bema.BemaMain")) ;
      else if (use_workspace == null) use_workspace = args[i];
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("BUBBLES: bubbles [-nosave] [-norestore] [-force] [-workspace <workspace>]");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Methods to actually run bubbles 					*/
/*										*/
/********************************************************************************/

private void start()
{
   // first setup the environment
   BoardSetup bs = BoardSetup.getSetup();

   if (palette_name != null) bs.setPalette(palette_name);

   if (install_only) {
      bs.doInstall();
      return;
    }

   if (for_language != null) {
      // should be done first since it can change system properties
      bs.setLanguage(for_language);
    }

   if (new_workspace) bs.setCreateWorkspace(use_workspace);
   else if (use_workspace != null) bs.setDefaultWorkspace(use_workspace);

   if (course_name != null) bs.setCourseName(course_name);

   if (skip_setup) bs.setSkipSetup();
   if (force_setup) bs.setForceSetup(true);
   if (force_update) bs.setForceUpdate(true);
   if (force_metrics) bs.setForceMetrics(true);
   if (skip_splash) bs.setSkipSplash();
   if (allow_debug) bs.setAllowDebug();
   if (ask_workspace != null) bs.setAskWorkspace(ask_workspace);
   if (run_mode != null) bs.setRunMode(run_mode);
   bs.setJavaArgs(java_args);
   if (auto_update != null) bs.setAutoUpdate(auto_update);
   if (no_bedrock) bs.setPluginRunning();

   if (bs.getCourseName() != null) {
      BeduFactory.getFactory();
    }

   BoardLog.logI("BEMA","Start setup");

   bs.doSetup();

   if (use_cloud) {
      bs.setSplashTask("Getting Cloud Information");
      BemaCloudSetup bcs = new BemaCloudSetup(bs);
      if (!bcs.doSetup()) {
	 System.exit(0);
       }
      bs.setSplashTask("Starting Cloud Server");
      if (!bcs.startServer()) {
	 JOptionPane.showMessageDialog(null,
	       "<html><p>Cloud Bubbles was not started correctly." +
	       "<p>Please try starting again.",
	       "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	 System.exit(1);
       }
    }

   bs.setSplashTask("Setting up Metrics and Logging");
   BoardMetrics.setupMetrics(force_setup);

   BoardProperties bp = BoardProperties.getProperties("Bema");

   if (use_web) {
      String url = bp.getProperty("Bema.web.url");
      if (url == null) {
	 BoardLog.logE("BEMA","Bema.web.url property not defined");
	 use_web = false;
       }
      else {
	 File f = BoardSetup.getPropertyBase();
	 File f1 = new File(f,"webkey");
	 if (!f1.exists()) {
	    BoardLog.logE("BEMA","Can't find webkey file in property directory");
	    use_web = false;
	  }
	 else {
	    try {
	       BufferedReader br = new BufferedReader(new FileReader(f1));
	       String ln = br.readLine();
	       if (ln == null) {
		  BoardLog.logE("BEMA","No key defined in webkey file");
		  use_web = false;
		}
	       else {
		  ln = ln.trim();
		  BumpClient bc = BumpClient.getBump();
		  bc.useWebMint(ln,url);
		}
	       br.close();
	     }
	    catch (IOException e) {
	       BoardLog.logE("BEMA","Unable to read webkey file");
	       use_web = false;
	     }
	    catch (Throwable e) {
	       BoardLog.logE("BEMA","Problem setting up mint",e);
	       use_web = false;
	     }
	  }
       }
    }

   // next start Messaging
   bs.setSplashTask("Setting up messaging");
   BumpClient bc = null;

   try {
      bc = BumpClient.getBump();
    }
   catch (Error e) {
      BoardLog.logE("BEMA","Problem starting bump",e);
    }

   if (bc == null) {
      JOptionPane.showMessageDialog(null,"Can't setup messaging environment ",
	    "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
      return;
    }

   // next start Eclipse
   bc.waitForIDE(bs);

   // clear temporary information
   File f1 = BoardSetup.getBubblesWorkingDirectory();
   File f2 = new File(f1,"tracedata.bandaid");
   f2.delete();

   // ensure various components are setup

   bs.setSplashTask("Initializing components");
   BaleFactory.setup();
   BassFactory.setup();
   bs.setSplashTask("Checking for current JavaDoc");
   BdocFactory.setup();

   bs.setSplashTask("Loading Project Symbols");
   BassFactory.waitForNames();

   for (String s : getSetupPackageProperties()) {
      String nm = bp.getProperty(s);
      if (nm == null || nm.equals("*") || nm.equals("")) continue;
      String ld = bp.getProperty(s + ".load");
      setupPackage(nm,ld);
    }

   bs.setSplashTask("Loading and Caching JavaDoc");
   BdocFactory.getFactory().waitForReady();

   // setup top level session from backup

   bs.setSplashTask("Restoring configuration");
   load_config = null;
   if (restore_session) {
      File cf = BoardSetup.getConfigurationFile();
      load_config = IvyXml.loadXmlFromFile(cf);
    }

   ToolTipManager ttm = ToolTipManager.sharedInstance();
   ttm.setDismissDelay(Integer.MAX_VALUE);

   // now start windows
   BudaRoot root = new BudaRoot(load_config);

   initializePackage("edu.brown.cs.bubbles.bale.BaleFactory",root);
   initializePackage("edu.brown.cs.bubbles.bass.BassFactory",root);

   for (String s : getSetupPackageProperties()) {
      String nm = bp.getProperty(s);
      initializePackage(nm,root);
    }

   loadPlugins(root);

   if (bs.getRunMode() != RunMode.SERVER) {
      root.pack();
      root.restoreConfiguration(load_config);
      root.setVisible(true);
    }

   bs.removeSplash();

   if (bs.getRunMode() == RunMode.SERVER) {
       waitForServerExit(root);
    }
   else {
      Runtime.getRuntime().addShutdownHook(new SaveConfiguration(root));
    }

   SwingKey.saveKeyDefinitions();

   BumpClient nbc = BumpClient.getBump();
   Element xe = nbc.getAllProjects(300000);
   if (IvyXml.getChild(xe,"PROJECT") == null) {
      ProjectBubbleAdder pba = new ProjectBubbleAdder(root);
      SwingUtilities.invokeLater(pba);
    }
}


private static class ProjectBubbleAdder implements Runnable {

   private BudaRoot buda_root;

   ProjectBubbleAdder(BudaRoot root) {
      buda_root = root;
    }

   @Override public void run() {
      BudaBubble bb = BuenoFactory.getFactory().getCreateProjectBubble();
      if (bb != null) {
         buda_root.waitForSetup();
         BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
         Dimension d = bb.getSize();
         Rectangle r = bba.getViewport();
         int x0 = r.x + r.width/2 - d.width/2;
         int y0 = r.y + r.height/2 - d.height/2;
         bba.addBubble(bb, x0, y0);
       }
    }

}	// end of inner class ProjectBubbleAdder




/********************************************************************************/
/*										*/
/*	Plugin management							*/
/*										*/
/********************************************************************************/

private void loadPlugins(BudaRoot buda)
{
   BoardProperties bp = BoardProperties.getProperties("Bema");
   String pinf = bp.getProperty("Bema.pluginfolder","dropins plugins");
   if (pinf != null) {
      List<String> names= new ArrayList<>();
      StringTokenizer tok = new StringTokenizer(pinf);
      while (tok.hasMoreTokens()) {
	 String pdir = tok.nextToken();
	 File dir = new File(pdir);
	 if (!dir.isAbsolute()) {
	    File root = BoardSetup.getSetup().getRootDirectory();
	    dir = new File(root,pdir);
	  }
	 if (dir.exists() && dir.isDirectory()) {
	    loadPlugins(dir,buda,names);
	  }
       }
      for (String s : names) {
	 finishPackage(s);
       }
    }
}



private void loadPlugins(File dir,BudaRoot root,List<String> names)
{
   File [] cands = dir.listFiles();
   if (cands != null) {
      for (File jfn : cands) {
	 if (jfn.isDirectory()) {
	    // assume all plugins are at top level, rather than inside directories
	    // loadPlugins(jfn,root);
	  }
	 else if (jfn.getPath().endsWith(".jar")) {
	    BoardLog.logD("BEMA","Load plugin " + jfn);
	    try (JarFile jf = new JarFile(jfn)) {
	       Manifest mf = jf.getManifest();
	       if (mf != null) {
		  Attributes at = mf.getMainAttributes();
		  String starts = at.getValue("Bubbles-start");
		  String dep = at.getValue("Bubbles-depends");
		  String palette = at.getValue("Bubbles-palette");
		  String res = at.getValue("Bubbles-resource");
		  String lib = at.getValue("Bubbles-lib");
		  String load = jfn.getAbsolutePath();
		  String basename = null;
		  if (dep != null) {
		     dep = dep.trim();
		     if (dep.length() > 0) load += ":" + dep;
		   }
		  if (res != null) {
		     StringTokenizer tok = new StringTokenizer(res);
		     while (tok.hasMoreTokens()) {
			String nm = tok.nextToken();
			setupPluginResource(jf,nm);
		      }
		   }
		  if (lib != null) {
		     StringTokenizer tok = new StringTokenizer(lib);
		     while (tok.hasMoreTokens()) {
			String nm = tok.nextToken();
			setupPluginLibrary(jf,nm);
		      }
		   }
		  if (starts != null) {
		     StringTokenizer tok = new StringTokenizer(starts);
		     while (tok.hasMoreTokens()) {
			String nm = tok.nextToken();
			if (basename == null) basename = nm;
			setupPackage(nm,load);
			initializePackage(nm,root);
			names.add(nm);
		      }
		   }
		  if (basename != null && palette != null) {
		     ClassLoader cldr = class_loaders.get(basename);
		     URL u = cldr.getResource(palette);
		     if (u != null) {
			BoardLog.logD("BEMA","Add plugin palette " + u);
			BoardColors.addPalette(u);
		      }
		   }
		}
	     }
	    catch (IOException e) {
	       BoardLog.logE("BEMA","Can't access plugin jar file " + jfn,e);
	       JOptionPane.showMessageDialog(null,
		     "Problem loading plugin " + jfn,
		     "Bubbles Plugin Problem",JOptionPane.WARNING_MESSAGE);
	     }
	  }
       }
    }
}



private void setupPluginResource(JarFile jf,String res)
{
   try {
      ZipEntry ze = jf.getEntry(res);
      if (ze == null) return;
      File resdir = BoardSetup.getSetup().getResourceDirectory();
      File tgt = new File(resdir,res);
      InputStream ins = jf.getInputStream(ze);
      IvyFile.copyFile(ins,tgt);
      BoardSetup.getSetup().checkResourceFile(res);
    }
   catch (IOException ex) {
      BoardLog.logE("BEMA","Problem loading plugin resource file " + res,ex); 
    }
}



private void setupPluginLibrary(JarFile jf,String res)
{
   try {
      ZipEntry ze = jf.getEntry(res);
      if (ze == null) return;
      File libdir = BoardSetup.getSetup().getLibraryDirectory();
      File tgt = new File(libdir,res);
      InputStream ins = jf.getInputStream(ze);
      IvyFile.copyFile(ins,tgt);
    }
   catch (IOException ex) {
      BoardLog.logE("BEMA","Problem loading plugin library file " + res,ex);
    }
}



/********************************************************************************/
/*										*/
/*	Initialization by name							*/
/*										*/
/********************************************************************************/

private void setupPackage(String nm,String load)
{
   // BoardLog.logD("BEMA","Setup " + nm);
   ClassLoader cldr = BemaMain.class.getClassLoader();

   if (load != null) {
      try {
	 List<URL> urls = new ArrayList<>();
	 StringTokenizer tok = new StringTokenizer(load,":");
	 while (tok.hasMoreTokens()) {
	    String path = tok.nextToken();
	    if (path.startsWith("/")) {
	       String pnm = "jar:file:" + path + "!/";
	       URI u = new URI(pnm);
	       urls.add(u.toURL());
	     }
	    else if (path.startsWith("lib/")) {
	       String path1 = path.substring(4);
	       String path2 = BoardSetup.getSetup().getLibraryPath(path1);
	       if (path2 != null) {
		  File p2f = new File(path2);
		  if (!p2f.exists()) path2 = null;
		}
	       if (path2 != null) {
		  String pnm = "jar:file:" + path2 + "!/";
		  URI u = new URI(pnm);
		  urls.add(u.toURL());
		}
	       else {
		  URL ustr = cldr.getResource(path1);
		  if (ustr != null) {
		     urls.add(ustr);
		     continue;
		   }
		  else {
		     BoardLog.logE("BEMA","Can't load jar file " + load);
		     return;
		   }
		}
	     }
	  }
	 URL [] urlarr = urls.toArray(new URL[urls.size()]);
	 cldr = new URLClassLoader(urlarr,cldr);
	 class_loaders.put(nm,cldr);
       }
      catch (URISyntaxException e) {
	 BoardLog.logE("BEMA","Can't load jar file " + load);
       }
      catch (MalformedURLException e) {
	 BoardLog.logE("BEMA","Can't load jar file " + load);
       }
    }

   try {
      Class<?> c = Class.forName(nm,true,cldr);
      Method m = c.getMethod("setup");
      // BoardLog.logD("BEMA","Calling " + nm + ".setup");
      m.invoke(null);
    }
   catch (ClassNotFoundException e) {
      BoardLog.logE("BEMA","Can't find initialization package " + nm);
    }
   catch (NoSuchMethodException e) {
      // BoardLog.logI("BEMA","no setup method");
    }
   catch (Throwable e) {
      BoardLog.logE("BEMA","Can't initialize package " + nm,e);
    }
}



private void initializePackage(String nm,BudaRoot br)
{
   // BoardLog.logD("BEMA","Initialize " + nm);

   try {
      ClassLoader cldr = class_loaders.get(nm);
      if (cldr == null) cldr = BemaMain.class.getClassLoader();
      Class<?> c = Class.forName(nm,true,cldr);
      Method m = c.getMethod("initialize",BudaRoot.class);
      m.invoke(null,br);
    }
   catch (ClassNotFoundException e) {
      if (nm.contains("bubbles.bicex.") ||
	    nm.contains("bubbles.brepair") ||
	    nm.contains("bubbles.bsean")) return;
      BoardLog.logE("BEMA","Package " + nm + " doesn't exist");
    }
   catch (NoSuchMethodException e) {
      // BoardLog.logI("BEMA","no initialize method");
    }
   catch (Throwable e) {
      BoardLog.logE("BEMA","Error during initialization",e);
      // missing initialize method is okay, missing class already caught
    }
}


private void finishPackage(String nm)
{
   try {
      ClassLoader cldr = class_loaders.get(nm);
      if (cldr == null) cldr = BemaMain.class.getClassLoader();
      Class<?> c = Class.forName(nm,true,cldr);
      Method m = c.getMethod("postLoad");
      m.invoke(null);
    }
   catch (ClassNotFoundException e) {
      if (nm.contains("bubbles.bicex.") ||
	    nm.contains("bubbles.brepair") ||
	    nm.contains("bubbles.bsean")) return;
      BoardLog.logE("BEMA","Package " + nm + " doesn't exist");
    }
   catch (NoSuchMethodException e) {
      // BoardLog.logI("BEMA","no initialize method");
    }
   catch (Throwable e) {
      BoardLog.logE("BEMA","Error during initialization",e);
      // missing initialize method is okay, missing class already caught
    }
}



public static Class<?> findClass(String nm)
{
   try {
      return Class.forName(nm);
    }
   catch (ClassNotFoundException e) { }

   for (ClassLoader cldr : class_loaders.values()) {
      try {
	 return Class.forName(nm,true,cldr);
       }
      catch (ClassNotFoundException e) { }
    }

   return null;
}



private Collection<String> getSetupPackageProperties()
{
   Map<Integer,String> loads = new TreeMap<Integer,String>();
   BoardProperties bp = BoardProperties.getProperties("Bema");
   BoardSetup bs = BoardSetup.getSetup();
   Set<String> done = new HashSet<String>();

   for (String s : bp.stringPropertyNames()) {
      boolean use = false;
      if (s.startsWith("Bema.plugin.") && s.lastIndexOf(".") == 11) use = true;
      switch (bs.getRunMode()) {
	 case NORMAL :
	    if (s.startsWith("Bema.plugin.normal.")) use = true;
	    break;
	 case CLIENT :
	    if (s.startsWith("Bema.plugin.client.")) use = true;
	    break;
	 case SERVER :
	    if (s.startsWith("Beam.plugin.server.")) use = true;
	    break;
       }
      if (use) {
	 int idx = s.lastIndexOf(".");
	 try {
	    int v = Integer.parseInt(s.substring(idx+1));
	    loads.put(v,s);
	  }
	 catch (NumberFormatException e) { }
       }
    }

   for (Iterator<String> it = loads.values().iterator(); it.hasNext(); ) {
      String key = it.next();
      String val = bp.getProperty(key);
      if (done.contains(val)) it.remove();
      else done.add(val);
    }

   return new LinkedHashSet<String>(loads.values());
}



private void checkDefaultLanguage()
{
   String cp = System.getProperty("java.class.path");
   if (cp == null) return;
   StringTokenizer tok = new StringTokenizer(cp,File.pathSeparator);
   while (tok.hasMoreTokens()) {
      String elt = tok.nextToken();
      int idx = elt.lastIndexOf(File.separator);
      if (idx > 0) elt = elt.substring(idx+1);
      if (elt.equals("cloudbb.jar")) {
         useCloud();
         break;
       }
      else if (elt.endsWith(".jar")) {
         boolean fnd = false;
         for (BoardLanguage bl : BoardLanguage.values()) {
            if (elt.equals(bl.getJarRunner())) {
               setLanguage(bl);
               fnd = true;
             }
          }
         if (fnd) break;
       }
    }
}


private void setLanguage(BoardLanguage bl)
{
   for_language = bl;
   File pd = bl.getPropertyDirectory();
   BoardProperties.setPropertyDirectory(pd);
}



private void useCloud()
{
   use_cloud = true;
   run_mode = RunMode.CLIENT;
   use_web = true;
   ask_workspace = false;

   String mid = System.getProperty("edu.brown.cs.bubbles.MINT");
   if (mid == null) {
      for (int i = 0; i < java_args.length-1; ++i) {
	 if (java_args[i].startsWith("-m")) {
	    mid = java_args[i+1];
	    break;
	  }
       }
    }
   if (mid == null) {
      int mid1 = (int)(Math.random()*100000);
      mid = "BUBBLES_CLOUD_" + mid1 + "_" + System.getProperty("user.name");
      System.setProperty("edu.brown.cs.bubbles.MINT",mid);

      int idx = java_args.length;
      String [] nargs = new String[idx+2];
      System.arraycopy(java_args,0,nargs,0,idx);
      nargs[idx++] = "-msg";
      nargs[idx++] = mid;
    }

   boolean havearg = false;
   for (int i = 0; i < java_args.length; ++i) {
      if (java_args[i].startsWith("-cl")) havearg = true;
    }
   if (!havearg) {
      int idx = java_args.length;
      String [] nargs = new String[idx+1];
      System.arraycopy(java_args,0,nargs,0,idx);
      nargs[idx++] = "-cloud";
    }
}




/********************************************************************************/
/*										*/
/*	Server mode dialog							*/
/*										*/
/********************************************************************************/

private void waitForServerExit(BudaRoot root)
{
   System.out.println(SERVER_READY_STRING);

   JOptionPane.showConfirmDialog(root,"Exit from Code Bubbles Server",
	    "Exit When Done",
	    JOptionPane.DEFAULT_OPTION,
	    JOptionPane.QUESTION_MESSAGE);

   root.handleSaveAllRequest();

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Methods to save session at the end					*/
/*										*/
/********************************************************************************/

private static class SaveConfiguration extends Thread {

   private BudaRoot for_root;

   SaveConfiguration(BudaRoot br) {
      super("SaveSessionAtEnd");
      for_root = br;
    }

   @Override public void run() {
      File cf = BoardSetup.getConfigurationFile();
      try {
	 // for_root.handleSaveAllRequest();
	 for_root.handleCheckpointAllRequest();
	 for_root.saveConfiguration(cf);
       }
      catch (IOException e) {
	 BoardLog.logE("BEMA","Problem saving session: " + e);
       }
    }

}	// end of inner class SaveConfiguration



}	// end of class BemaMain




/* end of BemaMain.java */
