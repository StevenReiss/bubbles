/********************************************************************************/
/*										*/
/*		BoardConstants.java						*/
/*										*/
/*	Bubbles attribute and property management constant definitions		*/
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

import java.awt.Color;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;



/**
 *	This class defines constants used either exclusively by Board or shared
 *	by board with all other modules in bubbles.
 *
 **/

public interface BoardConstants {





/********************************************************************************/
/*										*/
/*	Attribute values							*/
/*										*/
/********************************************************************************/

/**
 *	This defines the style for highlighting.  These styles can be user for
 *	defining different properties associated with text highlighting.  The
 *	color of the highlighting is controlled separately.
 *
 **/

enum BoardHighlightStyle {
   NONE,
   LINE,
   SQUIGGLE
}



/**
 *	Define the current language
 **/

enum BoardLanguage {
   JAVA("java","Eclipse",".bubbles","bubbles","bedrock","-java",null,true,
	 ".java.","bubbles.jar","Eclipse Workspace"),
   JAVA_IDEA("idea","IDEA",".bibbles","bibbles","bubjet","-idea",null,true
	 ,".java.","bibbles.jar","Idea Project Directory"),
   JS("js","JavaScript",".nobbles","nobbles","nobase","-js","-Xmx1536m",false,
	 ".js.","nobbles.jar","Node/JS Workspace"),
   DART("dart","Dart",".dartbubbles","dartbub","dartbase","-dart",null,false,
	 ".dart.","dartbubbles.jar","Dart Workspace"),
   TS("ts","TypeScript/JavaScript",".tybbles","tybbles","tsbase","-ts",null,false,
         ".ts.js.","tsbubbles.jar","TS/JS Workspace");

   private String language_name;
   private String language_title;
   private String property_directory;
   private String log_name;
   private String backend_log_name;
   private String bubbles_arg;
   private String vm_options;
   private boolean show_class_file;
   private Set<String> language_extensions;
   private String jar_runner;
   private String workspace_label;

   BoardLanguage(String name,String ttl,String dir,String log,String backlog,String arg,
	 String vm,boolean showclass,String exts,String jar,String ws) {
      language_name = name;
      language_title = ttl;
      property_directory = dir;
      log_name = log;
      backend_log_name = backlog;
      bubbles_arg = arg;
      vm_options = vm;
      show_class_file = showclass;
      language_extensions = new HashSet<>();
      StringTokenizer tok = new StringTokenizer(exts,".");
      while (tok.hasMoreTokens()) {
	 language_extensions.add("." + tok.nextToken());
       }
      jar_runner = jar;
      workspace_label = ws;
    }

   public String getName()			{ return language_name; }
   public String getTitle()			{ return language_title; }
   public String getLogName()			{ return log_name; }
   public String getBackendLogName()		{ return backend_log_name; }
   public String getBubblesArg()		{ return bubbles_arg; }
   public String getVMOptions() 		{ return vm_options; }
   public boolean getShowClassFile()		{ return show_class_file; }
   public String getJarRunner() 		{ return jar_runner; }
   public String getWorkspaceLabel()		{ return workspace_label; }

   public File getPropertyDirectory() {
      File f1 = new File(System.getProperty("user.home"));
      File f2 = new File(f1,property_directory);
      return f2;
    }

   public boolean isSourceFile(File file) {
      return isSourceFile(file.getName());
    }
   public boolean isSourceFile(String name) {
      if (name == null) return false;
      int idx = name.lastIndexOf(".");
      if (idx < 0) return false;
      String ext = name.substring(idx);
      if (language_extensions.contains(ext)) return true;
      return false;
    }

}	// end of inner enum BoardLanguage



/********************************************************************************/
/*										*/
/*	Directories								*/
/*										*/
/********************************************************************************/

/**
 *	Location of the user's copies of the bubbles property files.
 **/

String BOARD_PROP_BASE = ".bubbles";





/********************************************************************************/
/*										*/
/*	System property names							*/
/*										*/
/********************************************************************************/

/**
 *	Default architecture name
 **/

String BOARD_ARCH = System.getProperty("os.arch");


/**
 *	Eclipse installation directory system property
 **/

String BOARD_PROP_ECLIPSE_DIR = "edu.brown.cs.bubbles.eclipse." + BOARD_ARCH;
String BOARD_PROP_BASE_IDE_DIR = "edu.brown.cs.bubbles.baseide." + BOARD_ARCH;


/**
 *	Eclipse workspace name system property
 **/

String BOARD_PROP_WORKSPACE = "edu.brown.cs.bubbles.workspace";


/**
 *	Recent workspaces used
 **/

String BOARD_PROP_RECENT_WS = "edu.brown.cs.bubbles.recents";



/**
 *	Always ask the user for the workspace if this system property is true
 **/

String BOARD_PROP_ECLIPSE_ASK_WS = "edu.brown.cs.bubbles.ask_workspace";


/**
 *	Run eclipse in foreground if this property is true.
 **/

String BOARD_PROP_ECLIPSE_FOREGROUND = "edu.brown.cs.bubbles.foreground";




/**
 *	Bubbles installation directory system property
 **/

String BOARD_PROP_INSTALL_DIR = "edu.brown.cs.bubbles.install";



/**
 *	Automatically update bubbles plugin if this system property is true
 **/

String BOARD_PROP_AUTO_UPDATE = "edu.brown.cs.bubbles.auto_update";



/**
 *	Memory size to use when running bubbles
 **/

String BOARD_PROP_JAVA_VM_SIZE = "edu.brown.cs.bubbles.vmsize";



/**
 *	Name of the directory containing the jar file
 **/

String BOARD_PROP_JAR_DIR = "edu.brown.cs.bubbles.jar";



/**
 *	Eclipse startup options
 **/

String BOARD_PROP_ECLIPSE_OPTIONS = "edu.brown.cs.bubbles.eclipse.options";
String BOARD_PROP_IDEA_OPTIONS = "edu.brown.cs.bubbles.idea.options";
String BOARD_PROP_BASE_IDE_OPTIONS = "edu.brown.cs.bubbles.baseide.options";



/**
 *	Eclipse vm startup options
 **/

String BOARD_PROP_ECLIPSE_VM_OPTIONS = "edu.brown.cs.bubbles.eclipse.vm.options";
String BOARD_PROP_IDEA_VM_OPTIONS = "edu.brown.cs.bubbles.idea.vm.options";



/**
 *	Eclipse clean next option
 **/

String BOARD_PROP_ECLIPSE_CLEAN = "edu.brown.cs.bubbles.eclipse.clean";



/**
 *	Mint master port number if needed
 **/

String BOARD_PROP_MINT_MASTER_PORT = "edu.brown.cs.bubbles.mint.master.port";



/**
 *	Mint server port number if needed
 **/

String BOARD_PROP_MINT_SERVER_PORT = "edu.brown.cs.bubbles.mint.server.port";




/********************************************************************************/
/*										*/
/*	Special attribute names 						*/
/*										*/
/********************************************************************************/

/**
 *	Attribute used to store parent
 **/

String BOARD_ATTR_PARENT = "parent";            // must be string-valued


/**
 *	Attribute used to store size of a tab (default 8)
 **/

String BOARD_ATTR_TAB_SIZE = "tabsize";


/**
 *	Attribute for highlight style (e.g. shadow, underline, squiggle)
 **/

String BOARD_ATTR_HIGHLIGHT_STYLE = "highlightStyle";


/**
 *	Attribute for highlight color
 **/

String BOARD_ATTR_HIGHLIGHT_COLOR = "highlightColor";


/**
 *	Attribute for warning color
 */

Color WARNING_COLOR = Color.red;
Color WORKSPACE_DIALOG_COLOR = new Color(0xff43aeff,true);



/********************************************************************************/
/*										*/
/*	Mint constants								*/
/*										*/
/********************************************************************************/

/**
 *	Name of the mint server used to communicate with the back end.
 **/
String	BOARD_MINT_NAME = "BUBBLES_" + System.getProperty("user.name").replace(" ","_") + "_@@@";



/********************************************************************************/
/*										*/
/*	Standard file names for eclipse and bubbles installation directories	*/
/*										*/
/********************************************************************************/

/**
 *	List of applications to detect in eclipse root directory
 **/

String [] BOARD_ECLIPSE_START = new String [] {
   "eclipse", "eclipse.exe", "Eclipse.app",
   "STS.exe", "STS", "STS.app",
   "sts.exe", "stS", "sts.app",
   "myeclipse", "myeclipse.exe", "myeclipse.app",
   "Eclipse JEE.app", "eclipse JEE.app",
};
String [] BOARD_IDEA_START = new String [] {
      "idea", "idea.exe", "IntelliJ IDEA CE.app",
      "MacOS/idea"
};


/**
 *	Plugins subdirectory for eclipse
 **/

String BOARD_ECLIPSE_PLUGINS = "plugins";
String BOARD_ECLIPSE_DROPINS = "dropins";
String BOARD_ECLIPSE_MAC_DROPIN = "Eclipse.app/Contents/Eclipse";
String BOARD_INTELLIJ_MAC_PLUGIN = "IntelliJ IDEA CE.app/Contents";

interface BoardPluginFilter {

   boolean accept(String elementname);

}	// end of inner interface BoardPluginFilter


/**
 *	The name of our plugin
 **/

String BOARD_BUBBLES_PLUGIN_ECLIPSE = "edu.brown.cs.bubbles.bedrock_1.0.0.jar";
String BOARD_BUBBLES_PLUGIN_IDEA = "bubjet-0.0.1.jar";

/**
 *	Metadata directory for checking on eclipse workspace
 **/

String BOARD_ECLIPSE_WS_DATA = ".metadata";
String BOARD_IDEA_WS_DATA = ".idea";


/**
 *	Subdirectory of install to use for our various files
 **/

String BOARD_INSTALL_LIBRARY = "lib";
String BOARD_INSTALL_RESOURCES = "resources";
String BOARD_INSTALL_BINARY = "bin";

/**
 *	Executable files to check for in bubbles root directory
 **/


String [] BOARD_BUBBLES_START = new String [] { "bubbles", "bubbles.bat", "bubbles.exe" };


/**
 *	The name of our configuration files in the eclipse workspace
 **/

String BOARD_CONFIGURATION_FILE = "Config.bubbles";


/**
 *	The name of our configuration history fie in the eclipse workspace
 **/

String BOARD_HISTORY_FILE = "History.bubbles";


/**
 *	The name of our configuration history fie in the eclipse workspace
 **/

String BOARD_DOCUMENTATION_FILE = "Documentation.bubbles";


/**
 *	The main class to use when restarting bubbles
 **/

String BOARD_RESTART_CLASS = "edu.brown.cs.bubbles.bema.BemaMain";




/********************************************************************************/
/*										*/
/*	Resource names								*/
/*										*/
/********************************************************************************/

/**
 *	The jar name of the eclipse plugin
 **/

String BOARD_RESOURCE_PLUGIN_ECLIPSE = "bedrock.jar";
String BOARD_RESOURCE_PLUGIN_IDEA = "bubjet.jar";
/**
 *	The jar name of a file to determine if the jar is a valid bubble build
 **/

String BOARD_RESOURCE_CHECK  = "resources/Bale.props";    // check to see if using jar



/**
 *	Resource files to install in ~./bubbles from the jar if not present
 **/

String [] BOARD_RESOURCE_PROPS =  new String [] {
   "Bale.x86.props", "Bale.props", "Bale.dart.props",
   "Bema.props", "Beam.props",
   "Bgta.props", "Bass.props", "Bted.props",
   "Board.x86.props", "Board.props",
   "Bddt.java.props", "Bddt.props",
   "Bcon.props",
   "Bueno.java.props", "Bueno.props",
   "Bvcr.props",
   "Batt.props", "Bedu.props", "Bnote.props", "Bbook.props",
   "Buda.js.props","Buda.dart.props","Buda.props",
   "Bandaid.props","Barr.props","Bass.props",
   "Bdoc.java.props","Bdoc.props",
   "Bhelp.props", "Bwiz.props", "Buss.props",
   "Bfix.props", "Brepair.props"
};


/**
 *	Library files to install in $BUBBLES/lib from the jar
 **/

String [] BOARD_LIBRARY_FILES = new String [] {
   "ivy.jar",
   "gnujpdf.jar",
   "jsyntaxpane.jar",
   "smack.jar",
   "smackx-debug.jar",
   "smackx-jingle.jar",
   "smackx.jar",
   "joscar-client.jar",
   "joscar-common.jar",
   "joscar-protocol.jar",
   "bubblesasm.jar",
// "wikitextcore.jar", "wikitexttrac.jar",
   "mail.jar","json.jar", "jsoup.jar", "asm.jar", "slf4j-api.jar",
   "javax.activation.jar",

   "derby.jar", "derbyclient.jar", "derbynet.jar", "mysql.jar", "postgresql.jar",
};


String [] BOARD_LIBRARY_EXTRAS = new String [] {
   "copyright.txt",
   "junit.jar",
   "battagent.jar",
   "battjunit.jar",
   "bandaid.jar",
   "annotations.jar",
   "cofoja.jar",
   "jsoup.jar",
   "jsocks-klea.jar",
   "commons-compress.jar",
/*********************
   "caja.jar",
   "nashorn.jar",
*******************/
   "websocket.jar",
   "asm.jar",
   "slf4j-api.jar",
// "com.google.guava.jar",
// "com.google.javascript.jar",
   "org.eclipse.core.commands.jar",
   "org.eclipse.core.contenttype.jar",
   "org.eclipse.core.filebuffers.jar",
   "org.eclipse.core.filesystem.jar",
   "org.eclipse.core.jobs.jar",
   "org.eclipse.core.net.jar",
   "org.eclipse.core.resources.jar",
   "org.eclipse.core.runtime.jar",
   "org.eclipse.debug.core.jar",
   "org.eclipse.debug.ui.jar",
   "org.eclipse.equinox.app.jar",
   "org.eclipse.equinox.common.jar",
   "org.eclipse.equinox.preferences.jar",
   "org.eclipse.equinox.registry.jar",
   "org.eclipse.jdt.core.jar",
   "org.eclipse.jdt.core.manipulation.jar",
   "org.eclipse.jdt.debug.jdi.jar",
   "org.eclipse.jdt.debug.jdimodel.jar",
   "org.eclipse.jdt.launching.jar",
   "org.eclipse.jdt.ui.jar",
   "org.eclipse.jface.jar",
   "org.eclipse.jface.text.jar",
   "org.eclipse.ltk.core.refactoring.jar",
   "org.eclipse.osgi.jar",
   "org.eclipse.osgi.services.jar",
   "org.eclipse.osgi.util.jar",
   "org.eclipse.search.jar",
   "org.eclipse.swt.gtk.linux.x86_64.jar",
   "org.eclipse.swt.jar",
   "org.eclipse.text.jar",
   "org.eclipse.ui.ide.jar",
   "org.eclipse.ui.workbench.jar",
   "org.eclipse.wst.jsdt.core.jar",
   "org.eclipse.wst.jsdt.debug.core.jar",
// "org.eclipse.pde.ui.jar",
// "org.eclipse.pde.api.tools.ui.jar",

// "seede.jar",
// "poppy.jar",
// "fait.jar",
// "karma.jar",
   "mail.jar",
//   "activation.jar",
   "js-scriptengine.jar",
/*********
   "speech.properties",
   "freetts/freetts.jar",
   "freetts/jsapi.jar",
   "freetts/cmu_time_awb.jar",
   "freetts/cmu_us_kal.jar",
   "freetts/cmudict04.jar",
   "freetts/cmulex.jar",
   "freetts/cmutimelex.jar",
   "freetts/en_us.jar",
   "freetts/freetts-jsapi10.jar",
   "freetts/mbrola.jar",
   "freetts/voices.txt",
*********/

   "cocker.jar",

   "marytts/marytts.jar",
   "marytts/marytts-lang-en.jar",
   "marytts/voice-cmu-sit-hsmm.jar",
};


String [] BOARD_RESOURCE_EXTRAS = new String [] {
      "bbookbkg.gif",
};


String [] BOARD_BINARY_FILES = new String [] {
      "cloudcheckssh",
      "cloudrunner",
      "cloudbb",
};



String [] BOARD_CLASSPATH_FILES = new String [] {
/***********
   "freetts/freetts.jar",
   "freetts/jsapi.jar",
   "freetts/en_us.jar",
   "freetts/freetts-jsapi10.jar",
   "freetts/mbrola.jar",
***********/
   "marytts/marytts.jar",
   "marytts/marytts-lang-en.jar",
   "marytts/voice-cmu-slt-hsmm.jar",
};


String [] BOARD_TEMPLATES = new String [] {
   "block.template",
   "javadoc.template",
   "marquis.template",
   "module.template",
   "scratch.template",
   "type.template",
   "interface.template",
   "module_js.template",
   "module_dart.template",
};



/********************************************************************************/
/*										*/
/*	Logging definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Log level
 **/

enum LogLevel {
   NONE,
   ERROR,
   WARNING,
   INFO,
   DEBUG
}


/**
 *	User property indicating log level
 **/

String BOARD_PROP_LOG_LEVEL = "edu.brown.cs.bubbles.log_level";


/**
 *	User property indicating that logs should also be sent to stderr
 **/

String BOARD_PROP_USE_STDERR = "edu.brown.cs.bubbles.use_stderr";




/********************************************************************************/
/*										*/
/*	Thread Pool Definitions 						*/
/*										*/
/********************************************************************************/

/**
 *	Number of threads to keep most of the time
 **/
int	BOARD_CORE_POOL_SIZE = 2;


/**
 *	Maximum number of threads to allow in the pool
 **/
int	BOARD_MAX_POOL_SIZE = 20;


/**
 *	Time to keep the extra threads alive in ms
 **/
long	BOARD_POOL_KEEP_ALIVE_TIME = 10*60*1000;




/********************************************************************************/
/*										*/
/*	Definitions for running front end remotely				*/
/*										*/
/********************************************************************************/

enum RunMode {
   NORMAL,			// front end and eclipse on same host
   SERVER,			// running bubbles/eclipse without a front end
   CLIENT			// running front end without eclipse/backend
}



/********************************************************************************/
/*										*/
/*	Metrics definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Property indicating URL to use for sending files for metric
 **/

String BOARD_SAVE_ADDR_PROP = "Board.save.address";
String BOARD_SAVE_LOCAL_DIR = "Board.save.local.directory";

/**
 *	Property indicating URL where uploads are saved
 **/

String BOARD_UPLOAD_URL = "Board.upload.address";



/**
 *	Property indicating random user id for metrics
 **/

String BOARD_METRIC_PROP_USERID = "edu.brown.cs.bubbles.metrics.UserId";

/**
 *	Property to auto generate user id
 **/
String BOARD_METRIC_PROP_AUTOID = "edu.brown.cs.bubbles.metrics.autoid";

/**
 *	Property indicating we can save screen dumps for metrics
 **/

String BOARD_METRIC_PROP_SCREENS = "edu.brown.cs.bubbles.metrics.screens";


/**
 *	Property indicating we can ask about user experience
 **/

String BOARD_METRIC_PROP_EXPERIENCE = "edu.brown.cs.bubbles.metrics.screens";


/**
 *	Property indicating we can send information about % active
 **/

String BOARD_METRIC_PROP_ACTIVE = "edu.brown.cs.bubbles.metrics.active";


/**
 *	Property indicating we can send command logs
 **/

String BOARD_METRIC_PROP_COMMANDS = "edu.brown.cs.bubbles.metrics.commands";


/**
 *	Property indicating we can send eclipse command logs
 **/

String BOARD_METRIC_PROP_ECLIPSE = "edu.brown.cs.bubbles.metrics.eclipse";



/**
 *	Property indicating we can send automatic bug reports
 **/

String BOARD_METRIC_PROP_DUMPS = "edu.brown.cs.bubbles.metrics.dumps";
String BOARD_METRIC_PROP_ERRORS = "edu.brown.cs.bubbles.metrics.errors";

/**
 *	Property indicating we can monitor eclipse interactions and send log file
 **/

String BOARD_METRIC_PROP_MONITOR = "edu.brown.cs.bubbles.metrics.monitor";


/**
 * Property indicating we can send option files
 **/

String BOARD_METRIC_PROP_OPTIONS = "edu.brown.cs.bubbles.metrics.options";

/**
 * Property indicating we can send workingset
 **/

String BOARD_METRIC_PROP_WORKINGSET = "edu.brown.cs.bubbles.metrics.workingset";

/**
 *	Time between screen dumps
 **/

long	SCREEN_DUMP_TIME = 5*60*1000;


/**
 *	Time between command dumps
 **/

long	COMMAND_DUMP_TIME = 15*60*1000;


/**
 * Time between options dumps
 **/

long  OPTIONS_DUMP_TIME = 30*60*1000;


/**
 *	Time between user feedback requests
 **/

long	USER_FEEDBACK_TIME = 60*60*1000;


/**
 *	Time between eclipse command dumps
 **/

long	ECLIPSE_DUMP_TIME = 30*60*1000;

/**
 *	Time between eclipse monitor log dumps
 **/
long	MONITORLOG_DUMP_TIME = 15*60*1000;

/**
 * Time between workingset dumps
 **/

long	WORKINGSET_DUMP_TIME = 5*60*1000;

/**
 *	Time between event to consider inactive
 **/

long	INACTIVE_TIME = 60*1000;



/**
 *	Time before upload file timeout
 **/

int	METRICS_UPLOAD_TIMEOUT = 5*1000;



/********************************************************************************/
/*										*/
/*	Colors definitions							*/
/*										*/
/********************************************************************************/

String	PALETTE_PROP = "edu.brown.cs.bubbles.palette";

String	DEFAULT_PALETTE = "bubbles.palette";
String	KEY_DEFINITIONS = "bubbles.keys";

/********************************************************************************/
/*										*/
/*	Updating constants							*/
/*										*/
/********************************************************************************/

/**
 *	URL prefix indicating where bubbles information is web-accessible.
 **/

String BUBBLES_DIR = "https://www.cs.brown.edu/people/spr/bubbles/";





}	// end of interface BoardConstants




/* end of BoardConstants.java */
