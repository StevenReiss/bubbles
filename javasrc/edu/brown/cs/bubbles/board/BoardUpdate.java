/********************************************************************************/
/*										*/
/*		BoardUpdate.java						*/
/*										*/
/*	Bubbles attribute and property management automatic update manager	*/
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

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;



/**
 *	This class is used to check and perform automatic updates of the bubbles
 *	binary (jar file) as they become available.  It is important that this
 *	class contains no nested classes and uses nothing other than the standard
 *	Java library classes so it can easily be put in a jar file to download
 *	to do the update.
 **/

public final class BoardUpdate {


/********************************************************************************/
/*										*/
/*	Constant definitions for update 					*/
/*										*/
/********************************************************************************/


/**
 *	URL for the currently available version relative to BUBBLES_DIR
 **/
private static final String VERSION_URL = "/version.xml";


/**
 *	File path for the current version information.	This only has to work at
 *	Brown since we will be the only one releasing versions.  One might want to
 *	make this machine independent.	Running this main with -version will cause
 *	the minor version number to be updated by one in the given file.  This allows
 *	a script to generate a new version easily.
 **/

private static final String VERSION_FILE = "../../../../../../lib/version.xml";


/**
 *	File path for version description in readable English
 **/

private static final String VERSION_DESC = "../../../../../../lib/version.txt";



/**
 *	URL for the jar version of this file.  The updater will download this file
 *	if the current version of bubbles is out of date and then run it with the
 *	path of the bubbles jar as the argument. This is relative to BUBBLES_DIR.
 **/

private static final String UPDATER_URL = "/updater.jar";


/**
 *	URL for the newest version of code bubbles.  The updater will download this
 *	jar and replace the original with it and the reexecute it.  This is relative
 *	to BUBBLES_DIR.
 **/

private static final String BUBBLES_URL = "/bubbles.jar";


/**
 *	Path in the jar file of the current version file
 **/

private static final String VERSION_RESOURCE = "version.xml";



/**
 *	Arguments to File.createTempFile for updater file
 **/

private static final String	UPDATER_PREFIX = "bubblesupdater";
private static final String	UPDATER_SUFFIX = ".jar";

private static final String BUBBLES_DIR = "https://www.cs.brown.edu/people/spr/bubbles/";


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static String	bubbles_dir = BUBBLES_DIR;
private String		jar_file;
private List<String>	java_args;
private long		max_memory;

private static Proxy	update_proxy = Proxy.NO_PROXY;

private static String	version_data = "Build_" + System.getProperty("user.name");



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

/**
 *	Main entry point.  This is invoked by bubbles when an update is required.
 **/

public static void main(String [] args)
{
   if (args.length == 0) badArgs();
   if (args[0].startsWith("-v")) updateVersionFile();
   else {
      BoardUpdate bu = new BoardUpdate(args);
      bu.setupUpdate();
    }

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Static method to check if an update is needed				*/
/*										*/
/********************************************************************************/

static void checkUpdate(String jarfile,List<String> javaargs)
{
   try {
      InputStream vins = BoardUpdate.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE);
      Element ve = getVersionXml(vins);
      if (ve == null) return;
      version_data = getMajor(ve) + "." + getMinor(ve) + " @ " + getBubblesDir(ve);
      vins.close();
      bubbles_dir = getBubblesDir(ve);

      URL u = new URI(bubbles_dir + VERSION_URL).toURL();
      HttpURLConnection huc = (HttpURLConnection) u.openConnection(update_proxy);
      huc.setInstanceFollowRedirects(true);
      InputStream uins = huc.getInputStream();
      Element ue = getVersionXml(uins);
      if (ue == null) {
         System.err.println("BOARD: Can't connect to bubbles to get version: " + u);
         return;
       }
      uins.close();

      if (sameVersion(ue,ve)) return;		// we are up to date
      
      System.err.println("BOARD: VERSIONS " + getMinor(ue) + " :: " + getMinor(ve));
      
      if (System.getProperty("edu.brown.cs.bubbles.NO_UPDATE") != null) {
	 return;
       }

      int curjava = getJavaVersion(System.getProperty("java.version"));
      int updjava = getJavaVersion(ue);
      System.err.println("BOARD: JAVA VERSIONS: " + curjava + " " + updjava);
      if (curjava < updjava) return;

      forceUpdate(jarfile,javaargs);
    }
   catch (Throwable t) {
      System.err.println("BOARD: Problem checking for update: " + t);
    }
}


static void forceUpdate(String jarfile,List<String> javaargs)
{
   try {
      BoardUpdate bu = new BoardUpdate(jarfile,javaargs);
      bu.startUpdate();
    }
   catch (Throwable t) {
      System.err.println("BOARD: Problem checking for update: " + t);
    }
}



static String getVersionData()
{
   return version_data;
}



static void setVersion()
{
   try {
      InputStream vins = BoardUpdate.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE);
      Element ve = getVersionXml(vins);
      if (ve == null) return;
      version_data = getMajor(ve) + "." + getMinor(ve) + " @ " + getBubblesDir(ve);
      vins.close();
    }
   catch (Throwable t) {
      System.err.println("BOARD: Problem setting version: " + t);
    }
}



/********************************************************************************/
/*										*/
/*	Static method to update the version file				*/
/*										*/
/********************************************************************************/

static void updateVersionFile()
{
   try {
      InputStream ins = new FileInputStream(VERSION_FILE);
      Element v = getVersionXml(ins);
      if (v == null) return;
      ins.close();
      int mj = getMajor(v);
      int mn = getMinor(v);
      String newminor = Integer.toString(mn+1);
      v.setAttribute("MINOR",newminor);

      String javaver = System.getProperty("java.version");
      v.setAttribute("JAVA",javaver);

      saveVersionXml(VERSION_FILE,v);

      PrintWriter pw = new PrintWriter(new FileWriter(VERSION_DESC));
      pw.println("Version " + mj + "." + newminor + " of BUBBLES is now available");
      pw.close();
    }
   catch (Throwable t) {
      System.err.println("BOARD: Problem updating version number: " + t);
      System.exit(1);
    }
}




/********************************************************************************/
/*										*/
/*	Methods for managing the XML version file				*/
/*										*/
/********************************************************************************/

private static Element getVersionXml(InputStream ins) throws IOException
{
   if (ins == null) return null;

   try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(ins);
      return doc.getDocumentElement();
    }
   catch (ParserConfigurationException e) {
      System.err.println("BOARD: Problem creating XML parser for versioning: " + e);
      throw new IOException("Can't parse xml",e);
    }
   catch (SAXException e) {
      System.err.println("BOARD: Problem parsing XML (network inaccessible?): " + e);
      return null;			// network inaccessible
    }
}




private static void saveVersionXml(String file,Element e) throws IOException
{
   Writer w = new FileWriter(file);
   addXml(e,w);
   w.write("\n");
   w.close();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardUpdate(String [] args)
{
   // used when run to do the actual update
   jar_file = null;
   java_args = new ArrayList<String>();
   max_memory = 0;
   boolean ourargs = true;
   HttpURLConnection.setFollowRedirects(true);

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-") && ourargs) {
	 if (args[i].startsWith("-m")) {
	    try {
	       max_memory = Long.parseLong(args[i].substring(2));
	     }
	    catch (NumberFormatException e) {
	       max_memory = 0;
	     }
	  }
	 else if (args[i].startsWith("-P") && i+1 < args.length) {
	    setupProxy(args[++i]);
	  }
	 else if (args[i].startsWith("-X")) {
	    ourargs = false;
	  }
	 else {
	    System.err.println("BOARDUPDATE: Illegal argument: " + args[i]);
	  }
       }
      else if (jar_file == null) jar_file = args[i];
      else {
	 ourargs = false;
	 java_args.add(args[i]);
       }
    }

   if (jar_file == null) badArgs();
}



private BoardUpdate(String jarfile,List<String> javaargs)
{
   // used when run to setup the update
   jar_file = jarfile;
   java_args = new ArrayList<String>();
   if (javaargs != null) java_args.addAll(javaargs);
   max_memory = 0;
   HttpURLConnection.setFollowRedirects(true);
}



private static void badArgs()
{
   System.err.println("BOARDUPDATE: boardupdate [-version]");
   System.err.println("BOARDUPDATE: boardupdate <jar file>");
}



/********************************************************************************/
/*										*/
/*	Methods to start the update by downloading and starting the updater	*/
/*										*/
/********************************************************************************/

private void startUpdate() throws IOException
{
   HttpURLConnection.setFollowRedirects(true);  
   File f = File.createTempFile(UPDATER_PREFIX,UPDATER_SUFFIX);
   try (OutputStream ots = new BufferedOutputStream(new FileOutputStream(f))) {
      URL u = new URI(bubbles_dir + UPDATER_URL).toURL();
      InputStream ins = u.openConnection(update_proxy).getInputStream();
      copyFile(ins,ots);
   }
   catch (URISyntaxException e) {
      throw new IOException("BAD URI",e);
   }
   
   System.err.println("BUBBLES: Starting update");

   if (java_args == null) java_args = new ArrayList<String>();
   int ln = java_args.size();

   int idx = 0;
   java_args.add(idx++,getJavaPath());

   java_args.add(idx++,"-jar");
   java_args.add(idx++,f.getPath());

   java_args.add(idx++,jar_file);
   java_args.add(idx++,"-m" + Runtime.getRuntime().maxMemory());
   try {
      if (update_proxy != null && update_proxy != Proxy.NO_PROXY) {
	 String pstr = update_proxy.type().toString();
	 InetSocketAddress sa = (InetSocketAddress) update_proxy.address();
	 pstr += ":" + sa.getHostName() + ":" + sa.getPort();
	 java_args.add(idx++,"-P");
	 java_args.add(idx++,pstr);
       }
    }
   catch (Throwable t) {
      // not from updater
      BoardLog.logE("BOARD","Problem getting proxy info for updater",t);
    }

   if (ln > 0) java_args.add(idx++,"-X");

   System.err.print("BUBBLES:");
   for (String s : java_args) System.err.print(" " + s);
   System.err.println();

   ProcessBuilder pb = new ProcessBuilder(java_args);
   pb.start();

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Methods to actually do the update					*/
/*										*/
/********************************************************************************/

private void setupUpdate()
{
   // In case java locks the running jar file, wait for the current process
   // to go away

   JPanel pnl = new JPanel(new BorderLayout());
   pnl.setBackground(new Color(211,232,248));
   JTextField prog = new JTextField();
   prog.setEditable(false);
   pnl.add(prog,BorderLayout.SOUTH);
   JLabel lbl = new JLabel("<html><h1><c>Updating Code Bubbles</c></h1>" +
			      "<br><h2><c>(This could take some time)</c></h2>"+
			      "<br>");
   lbl.setBackground(new Color(211,232,248));
   pnl.add(lbl,BorderLayout.CENTER);
   JFrame splash = new JFrame();
   splash.setContentPane(pnl);
   splash.setUndecorated(true);
   splash.setResizable(false);
   splash.setAlwaysOnTop(false);
   splash.pack();
   Toolkit tk = Toolkit.getDefaultToolkit();
   Dimension ssz = tk.getScreenSize();
   Dimension wsz = splash.getSize();
   int xpos = ssz.width/2 - wsz.width/2;
   int ypos = ssz.height/2 - wsz.height/2;
   splash.setLocation(xpos,ypos);
   splash.setVisible(true);

   prog.setText("Checking jar file is unlocked");

   for (int i = 0; i < 10; ++i) {
      try {
	 FileInputStream fins = new FileInputStream(jar_file);
	 fins.close();
	 break;
       }
      catch (IOException e) { }
      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }

   boolean usebackup = false;
   File jf0 = new File(jar_file);
   File jf1 = new File(jar_file + ".backup");

   try {
      // first create a backup of the current jar in case we need it
      prog.setText("Saving a backup of the jar file");
      InputStream ins = new FileInputStream(jf0);
      OutputStream ots = new FileOutputStream(jf1);
      jf1.deleteOnExit();
      copyFile(ins,ots);
      usebackup = true;

      prog.setText("Downloading new version of bubbles");
      URL u = new URI(bubbles_dir + BUBBLES_URL).toURL();
      ins = u.openConnection(update_proxy).getInputStream();
      ots = new FileOutputStream(jf0);
      copyFile(ins,ots);
    }
   catch (Throwable t) {
      JOptionPane.showMessageDialog(null,"Problem doing update: " + t);
      System.err.println("BOARDUPDATE: Problem doing update: " + t);

      prog.setText("Update Problem: restoring old version");

      if (usebackup) {
	 try {
	    InputStream ins = new FileInputStream(jf1);
	    OutputStream ots = new FileOutputStream(jf0);
	    copyFile(ins,ots);
	  }
	 catch (IOException e) {
	    System.err.println("BOARDUPDATE: problem restoring backup " + jf1);
	    System.exit(1);
	  }
       }
      // This is here to prevent infinite loops in the event of update failure
      System.exit(1);
    }

   updatePlugins();

   try {
      prog.setText("Restarting bubbles");
      List<String> args = new ArrayList<String>();
      args.add(getJavaPath());
      if (max_memory > 0) args.add("-Xmx" + max_memory);
      args.add("-cp");
      args.add(jf0.getPath());
      args.add("edu.brown.cs.bubbles.board.BoardSetup");
      args.add("-update");
      if (java_args != null) {
	 for (int i = 0; i < java_args.size(); ++i) {
	    if (java_args.get(i).equals("-p")) {
	       args.add("-p");
	       args.add(java_args.get(i+1));
	     }
	  }
	 args.add("-X");
	 for (String s : java_args) args.add(s);
       }

      ProcessBuilder pb = new ProcessBuilder(args);

      Process p = pb.start();
      for ( ; ; ) {
	 try {
	    p.waitFor(2,TimeUnit.SECONDS);
	    break;
	  }
	 catch (InterruptedException e) { }
         if (p.isAlive()) {
            splash.setVisible(false);
          }
       }
    }
   catch (IOException e) {
      System.err.println("BOARDUPDATE: Problem restarting bubbles: " + e);
      System.exit(1);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Plugin updater                                                          */
/*                                                                              */
/********************************************************************************/

private void updatePlugins()
{
   Map<String,URL> plugins = getPluginData();
   
   File p1 = new File(jar_file);
   File p2 = p1.getParentFile();
   File dir = new File(p2,"dropins");
   if (!dir.exists()) dir = new File(p2,"plugins");
   if (!dir.exists()) return;
   for (File f : dir.listFiles()) {
      URL u1 = plugins.get(f.getName());
      if (u1 != null) updatePlugin(f,u1);
    }
}



private void updatePlugin(File jar,URL url)
{
   long dlm = jar.lastModified();
   URLConnection uc = null;
   try {
      uc = url.openConnection();
      if (uc instanceof HttpURLConnection) {
         HttpURLConnection hc = (HttpURLConnection) uc;
         if (hc.getResponseCode() >= 300) {
            throw new IOException("Bad url for plugiun " + jar + " " + hc.getResponseCode());
          }
       }
      long urldlm = uc.getLastModified();
      if (urldlm < dlm) return;
    }
   catch (IOException e) {
      System.err.println("BOARDUPATE: Problem updating plugin " + jar);
      e.printStackTrace();
      return;
    }
   
   File f1 = new File(jar + ".save");
   jar.renameTo(f1);
   try {
      InputStream uins = uc.getInputStream();
      copyFile(uins,jar);
      f1.delete();
    }
   catch (IOException e) {
      f1.renameTo(jar);
      System.err.println("BOARDUPDATE: Problem copying plugin update for " + jar);
      e.printStackTrace();
    }
}


private Map<String,URL> getPluginData()
{
   Map<String,URL> rslt = new HashMap<>();
   
   try {
      URL u = new URI(BUBBLES_DIR + "/plugins.xml").toURL();
      InputStream uins = u.openConnection().getInputStream();
      Element xml = loadXmlFromStream(uins);
      NodeList nl = xml.getElementsByTagName("PLUGIN");
      for (int i = 0; i < nl.getLength(); ++i) {
         Element pxml = (Element) nl.item(i);
         String urls = pxml.getAttribute("URL");
         int idx = urls.lastIndexOf("/");
         String jar = urls.substring(idx+1);
         URL pu = new URI(urls).toURL();
         rslt.put(jar,pu);
       }
    }
   catch (IOException | URISyntaxException e) {
      System.err.println("BOARDUPDATE: Problem getting plugin data");
      e.printStackTrace();
    }
   
   return rslt;
}



private void copyFile(InputStream r,File df) throws IOException
{
   FileOutputStream w = new FileOutputStream(df);
   try {
      byte [] buf = new byte[8192];
      for ( ; ; ) {
         int ln = r.read(buf);
         if (ln <= 0) break;
         w.write(buf,0,ln);
       }
    }
   finally {
      w.close();
      r.close();
    }
}


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


private String getJavaPath()
{
   String jpath = System.getProperty("java.home");
   File jhome = new File(jpath);
   File f1 = new File(jhome,"bin");
   File f2 = new File(f1,"java");
   if (f2.exists() && f2.canExecute()) return f2.getPath();
   
   File f3 = new File(jpath,"jre");
   File f4 = new File(f3,"bin");
   File f5 = new File(f4,"java");
   if (f5.exists() && f5.canExecute()) return f5.getPath();
   
   return "java";
}



/********************************************************************************/
/*										*/
/*	Version management							*/
/*										*/
/********************************************************************************/

private static int getMajor(Element e1)
{
   if (e1 == null) return -1;

   String v = e1.getAttribute("MAJOR");
   if (v == null) return -1;
   try {
      return Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }
   return -1;
}



private static int getMinor(Element e1)
{
   if (e1 == null) return -1;

   String v = e1.getAttribute("MINOR");
   if (v == null) return -1;
   try {
      return Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }
   return -1;
}


private static int getJavaVersion(Element e1)
{
   String v = e1.getAttribute("JAVA");
   return getJavaVersion(v);
}


private static int getJavaVersion(String v)
{
   if (v == null) v = "1.8.0";
   if (v.startsWith("1.")) v = v.substring(2);
   int idx = v.indexOf(".");
   if (idx > 0) v = v.substring(0,idx);

   try {
      return Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }

   return 8;
}


private static String getBubblesDir(Element e1)
{
   String v = e1.getAttribute("URL");
   if (v == null) {
      System.err.println("BOARD UPDATER: Can't find URL in version xml");
      v = BUBBLES_DIR;
    }

   return v;
}




private static boolean sameVersion(Element e1,Element e2)
{
   if (getMajor(e1) != getMajor(e2)) return false;
   if (getMinor(e1) != getMinor(e2)) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	Proxy methods								*/
/*										*/
/********************************************************************************/

static void setupProxy(String s)
{
   String [] args = s.split(":");
   Proxy.Type typ;
   InetSocketAddress addr;
   try {
      typ = Proxy.Type.valueOf(args[0]);
      int port = Integer.parseInt(args[2]);
      addr = new InetSocketAddress(args[1],port);
      update_proxy = new Proxy(typ,addr);
    }
   catch (Throwable t) { }
}




/********************************************************************************/
/*										*/
/*	XML output methods (copied from IvyXML) 				*/
/*										*/
/********************************************************************************/

private static void addXml(Node n,Writer w) throws IOException
{
   if (n instanceof Element) {
      Element e = (Element) n;
      w.write("<" + e.getNodeName());
      NamedNodeMap nnm = e.getAttributes();
      for (int i = 0; ; ++i) {
	 Node na = nnm.item(i);
	 if (na == null) break;
	 Attr a = (Attr) na;
	 if (a.getSpecified()) {
	    w.write(" " + a.getName() + "='");
	    outputXmlString(a.getValue(),w);
	    w.write("'");
	  }
       }
      if (e.getFirstChild() == null) w.write(" />");
      else {
	 w.write(">");
	 for (Node cn = n.getFirstChild(); cn != null; cn = cn.getNextSibling()) {
	    addXml(cn,w);
	  }
	 w.write("</" + e.getNodeName() + ">");
       }
    }
   else if (n instanceof CDATASection) {
      w.write("<![CDATA[");
      w.write(n.getNodeValue());
      w.write("]]>");
    }
   else if (n instanceof Text) {
      String s = n.getNodeValue();
      if (s != null) outputXmlString(s,w);
    }
}



private static void outputXmlString(String s,Writer pw)
{
   if (s == null) return;

   try {
      for (int i = 0; i < s.length(); ++i) {
	 char c = s.charAt(i);
	 switch (c) {
	    case '&' :
	       pw.write("&amp;");
	       break;
	    case '<' :
	       pw.write("&lt;");
	       break;
	    case '>' :
	       pw.write("&gt;");
	       break;
	    case '"' :
	       pw.write("&quot;");
	       break;
	    case '\'' :
	       pw.write("&apos;");
	       break;
	    default :
	       pw.write(c);
	       break;
	  }
       }
    }
   catch (IOException e) { }
}


private Element loadXmlFromStream(InputStream inf)
{
   Element rslt = null;
   XmlParser xp = new XmlParser(false);
   if (inf == null) return null;
   
   try {
      InputSource ins = new InputSource(inf);
      Document xdoc = xp.parse(ins);
      inf.close();
      rslt = xdoc.getDocumentElement();
      xp.reset();
    }
   catch (SAXException e) {
      System.err.println("Ivy XML parse error for input stream: " + e.getMessage());
    }
   catch (IOException e) {
      System.err.println("Ivy XML I/O error for input stream: " + e.getMessage());
    }
   catch (Exception e) {
      System.err.println("Ivy XML error for input stream: " + e);
      e.printStackTrace();
    }
   
   return rslt;
}


/********************************************************************************/
/*										*/
/*	Create a parser 							*/
/*										*/
/********************************************************************************/

private static class XmlParser {

   private DocumentBuilder parser_object;
   
   XmlParser(boolean ns) {
      DocumentBuilderFactory dbf = null;
      try {
         dbf = DocumentBuilderFactory.newInstance();
       }
      catch (Exception e) {
         System.err.println("BOARDUPDATE: Problem creating document builder factory: " + e);
         throw e;
       }
      dbf.setValidating(false);
      // dbf.setXIncludeAware(false);
      dbf.setNamespaceAware(ns);
      dbf.setIgnoringElementContentWhitespace(true);
      
      try {
         System.setProperty("entityExpansionLimit","100000000");
       }
      catch (Throwable e) { }
      try {
         dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
       }
      catch (Throwable e) { }
      try {
         parser_object = dbf.newDocumentBuilder();
       }
      catch (ParserConfigurationException e) {
         System.err.println("IvyXml: Problem creating java xml parser: " + e.getMessage());
         System.exit(1);
       }
    }
   
   Document parse(InputSource is) throws Exception {
      return parser_object.parse(is);
    }
   
   Document parse(String uri) throws Exception {
      return parser_object.parse(uri);
    }
   
   void reset() {
      try {
         parser_object.reset();
         parse("<END/>");
       }
      catch (Throwable e) { }
    }

}	// end of subclass XmlParser


}	// end of class BoardUpdate




/* end of BoardUpdate.java */
