/********************************************************************************/
/*										*/
/*		BoardPluginManager.java 					*/
/*										*/
/*	Plugin dialog and management						*/
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

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingListSet;
import edu.brown.cs.ivy.xml.IvyXml;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.net.ssl.SSLException;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.text.View;

import org.w3c.dom.Element;

public class BoardPluginManager implements BoardConstants, ActionListener
{



/********************************************************************************/
/*										*/
/*	Main program -- run standalone						*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BoardPluginManager bpm = new BoardPluginManager();
   bpm.managePlugins();
   
   for ( ; ; ) {
      JDialog dlg = bpm.working_dialog;
      if (!dlg.isVisible()) System.exit(0);
    }
}






/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SwingListSet<PluginData>	avail_set;
private SwingListSet<PluginData>	install_set;
private JList<PluginData>		avail_list;
private JList<PluginData>		install_list;
private JDialog 			working_dialog;

private static Set<PluginData> all_plugins = null;

private static boolean                  use_http = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BoardPluginManager()
{
   getPluginData();
}



/********************************************************************************/
/*										*/
/*	Manage Plugin Dialog							*/
/*										*/
/********************************************************************************/

public void managePlugins()
{
   getPluginData();
   JPanel pnl = setupPanel();
   working_dialog = new JDialog((JFrame) null,"Bubbles Plugin Manager",false);
   working_dialog.setContentPane(pnl);
   working_dialog.pack();
   working_dialog.setLocationRelativeTo(null);
   working_dialog.setVisible(true);
}



/********************************************************************************/
/*										*/
/*	User interaction panel							*/
/*										*/
/********************************************************************************/

private JPanel setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   BoardColors.setColors(pnl,WORKSPACE_DIALOG_COLOR);
   pnl.setOpaque(true);

   int y = 0;

   JLabel lbl = new JLabel("Code Bubbles Plugin Manager");
   lbl.setHorizontalAlignment(JLabel.CENTER);
   Font fnt = lbl.getFont();
   lbl.setFont(fnt.deriveFont(fnt.getSize2D()+2));
   pnl.addGBComponent(lbl,0,y++,0,1,10,0);
   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   lbl = new JLabel("Available");
   lbl.setHorizontalAlignment(JLabel.CENTER);
   pnl.addGBComponent(lbl,0,y,1,1,1,0);
   lbl = new JLabel("Installed");
   lbl.setHorizontalAlignment(JLabel.CENTER);
   pnl.addGBComponent(lbl,2,y++,1,1,1,0);

   avail_set = new SwingListSet<>();
   install_set = new SwingListSet<>();
   for (PluginData pd : all_plugins) {
      if (pd.isInstalled()) {
	 install_set.addElement(pd);
       }
      else {
	 avail_set.addElement(pd);
       }
    }
   avail_list = new PluginList(avail_set);
   install_list = new PluginList(install_set);
   pnl.addGBComponent(avail_list,0,y,1,4,1,1);
   pnl.addGBComponent(install_list,2,y,1,4,1,1);
   pnl.addGBComponent(new JLabel(),1,y++,1,1,0,1);
   Icon icn = BoardImage.getIcon("next");
   JButton btn = new JButton(icn);
   btn.addActionListener(this);
   btn.setActionCommand("INSTALL");
   pnl.addGBComponent(btn,1,y++,1,1,0,0);
   icn = BoardImage.getIcon("back");
   btn = new JButton(icn);
   btn.addActionListener(this);
   btn.setActionCommand("REMOVE");
   pnl.addGBComponent(btn,1,y++,1,1,0,0);
   pnl.addGBComponent(new JLabel(),1,y++,1,1,0,1);
   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   lbl = new JLabel("Installation will take effect on next restart");
   pnl.addGBComponent(lbl,0,y++,0,1,10,0);

   pnl.addBottomButton("OK","OK",this);
   pnl.addBottomButton("CANCEL","CANCEL",this);
   pnl.addBottomButtons(y++);

   return pnl;
}





private class PluginList extends JList<PluginData> {

   private static final long serialVersionUID = 1;

   PluginList(ListModel<PluginData> data) {
      super(data);
      setDragEnabled(true);
      setDropMode(DropMode.INSERT);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      int sz = all_plugins.size();
      setVisibleRowCount(Math.min(sz,10));
    }

   @Override public String getToolTipText(MouseEvent e) {
      int idx = locationToIndex(e.getPoint());
      if (idx < 0) return null;
      PluginData pd = getModel().getElementAt(idx);
      if (pd != null) return pd.getDescription();
      return null;
    }

}	// end of inner class PluginList





/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   String cmd = evt.getActionCommand();
   if (cmd.equals("INSTALL")) {
      List<PluginData> toadd = new ArrayList<>(avail_list.getSelectedValuesList());
      while (!toadd.isEmpty()) {
         Set<String> req = new HashSet<>();
         for (PluginData pd : toadd) {
            if (!checkCanInstall(pd)) continue;
            avail_set.removeElement(pd);
            install_set.addElement(pd);
            for (String s : pd.getRequiredPlugins()) req.add(s);
          }
         toadd.clear();
         for (String nm : req) {
            for (PluginData pd : avail_set) {
               if (pd.getName().equals(nm)) {
                  toadd.add(pd);
                  break;
                }
             }
          }
       }
    }
   else if (cmd.equals("REMOVE")) {
      for (PluginData pd : install_list.getSelectedValuesList()) {
	 install_set.removeElement(pd);
	 avail_set.addElement(pd);
       }
    }
   if (cmd.equals("CANCEL")) {
      working_dialog.setVisible(false);
    }
   else if (cmd.equals("OK")) {
      working_dialog.setVisible(false);
      handleInstalls();
    }
}


private void handleInstalls()
{
   for (PluginData pd : avail_set) {
      if (pd.isInstalled()) pd.uninstall();
    }
   for (PluginData pd : install_set) {
      try {
	 if (!pd.isInstalled()) pd.install();
       }
      catch (IOException e) {
	 JOptionPane.showMessageDialog(null,"Problem installing " + pd.getName() + ": " +
	       e,"Bubbles Plugin Problem",JOptionPane.ERROR_MESSAGE);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Handle bubbles update							*/
/*										*/
/********************************************************************************/

public void updatePlugins()
{
   System.err.println("BOARDUPDATE: Update Plugins " + all_plugins.size());
   for (PluginData pd : all_plugins) {
      System.err.println("BOARDUPDATE: Update plugin " + pd.getName() + " " + pd.isInstalled());
      if (pd.isInstalled()) {
	 pd.update();
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Check if user lets us install a plugin                                  */
/*                                                                              */
/********************************************************************************/

private boolean checkCanInstall(PluginData pd)
{
   if (pd.getWarning() != null) {
      String warn = pd.getWarning();
      JLabel lbl = new JLabel(warn);
      View view = (View) lbl.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
      view.setSize(400,0);
      float w = view.getPreferredSpan(View.X_AXIS);
      float h = view.getPreferredSpan(View.Y_AXIS);
      Dimension sz = new Dimension((int) Math.ceil(w),(int) Math.ceil(h));
      lbl.setPreferredSize(sz);
      lbl.setSize(sz);
     
      int sts = JOptionPane.showConfirmDialog(working_dialog,lbl,
            "Confirm Plugin " + pd.getName(),
            JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
      return sts == JOptionPane.YES_OPTION;
    }
   return true;
}



/********************************************************************************/
/*										*/
/*	Handle resources							*/
/*										*/
/********************************************************************************/

public static File installResources(Class<?> c,String dir,BoardPluginFilter fltr)
{
   File jarf = IvyFile.getJarFile(c);
   if (jarf == null) return null;
   File fp = jarf.getParentFile();
   File dirf = fp;
   if (dir != null) dirf = new File(fp,dir);
   if (!dirf.exists()) dirf.mkdir();
   if (dirf.exists()) {
      try {
	 JarFile jf = new JarFile(jarf);
	 for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
	    JarEntry je = e.nextElement();
	    String nm = je.getName();
	    if (fltr.accept(nm)) {
	       File usedir = dirf;
	       String pnm = nm;
	       while (pnm.contains("/")) {
		  int idx = pnm.indexOf("/");
		  usedir = new File(usedir,pnm.substring(0,idx));
		  if (!usedir.exists()) usedir.mkdir();
		  pnm = pnm.substring(idx+1);
		}
	       File tgt = new File(usedir,pnm);
	       if (!tgt.exists()) {
		  InputStream ins = jf.getInputStream(je);
		  IvyFile.copyFile(ins,tgt);
		}
	     }
	  }
	 jf.close();
       }
      catch (IOException e) { }
    }

   return dirf;
}




/********************************************************************************/
/*										*/
/*	Find available plugins							*/
/*										*/
/********************************************************************************/

private static synchronized void getPluginData()
{
   if (all_plugins != null) return;
   HttpURLConnection.setFollowRedirects(true);

   all_plugins = new TreeSet<>();
   
   String utxt = BUBBLES_DIR + BOARD_PLUGIN_DESCRIPTION_URL;
   for (int i = 0; i < 2; ++i) {
      try {
         if (use_http) utxt = utxt.replace("https","http");
         URL u = new URI(utxt).toURL();
         HttpURLConnection hc = (HttpURLConnection) u.openConnection();
         int sts = hc.getResponseCode();
         if (sts > 300) {
            BoardLog.logE("BOARD","Bad URL connection for plugin: " + sts + " " + u);
          }
         InputStream uins = hc.getInputStream();
         Element e = IvyXml.loadXmlFromStream(uins);
         for (Element pe : IvyXml.children(e,"PLUGIN")) {
            all_plugins.add(new PluginData(pe));
          }
       }
      catch (SSLException e) {
         if (!use_http) {
            use_http = true;
            continue;
          }
         BoardLog.logE("BOARD","HTTPS Problem getting plugin data: " + e);
         // try again with http (different plugin file -- it also must use http ???
       }
      catch (IOException | URISyntaxException e) { 
         BoardLog.logE("BOARD","Problem getting plugin data: " + e);
         e.printStackTrace();
       }
      break;
    }
}



/********************************************************************************/
/*										*/
/*	Plugin information							*/
/*										*/
/********************************************************************************/

private static class PluginData implements Comparable<PluginData> {

   private String plugin_name;
   private String plugin_description;
   private String plugin_url;
   private String plugin_warning;
   private List<String> plugin_mains;
   private List<String> plugin_resources;
   private List<String> plugin_requires;
   private File plugin_file;
   private boolean plugin_installed;

   PluginData(Element xml) {
      plugin_name = IvyXml.getAttrString(xml,"NAME");
      plugin_description = IvyXml.getAttrString(xml,"DESCRIPTION");
      plugin_url = IvyXml.getAttrString(xml,"URL");
      plugin_warning = IvyXml.getTextElement(xml,"WARNING");
      if (plugin_warning != null) {
         plugin_warning = plugin_warning.trim();
         if (plugin_warning.length() == 0) plugin_warning = null;
       }
      plugin_mains = new ArrayList<>();
      for (Element e : IvyXml.children(xml,"MAIN")) {
         plugin_mains.add(IvyXml.getText(e).trim());
       }
      String s = IvyXml.getAttrString(xml,"MAIN");
      if (s != null) plugin_mains.add(s);
   
      plugin_resources = new ArrayList<>();
      for (Element e : IvyXml.children(xml,"RESOURCE")) {
         plugin_resources.add(IvyXml.getText(e).trim());
       }
      String s1 = IvyXml.getAttrString(xml,"RESOURCE");
      if (s1 != null) plugin_resources.add(s1);
   
      plugin_requires = new ArrayList<>();
      for (Element e : IvyXml.children(xml,"REQUIRES")) {
         plugin_requires.add(IvyXml.getText(e).trim());
       }
      String s2 = IvyXml.getAttrString(xml,"REQUIRES");
      if (s2 != null) plugin_requires.add(s2);
      
      plugin_file = null;
      checkInstalled();
    }

   boolean isInstalled()		{ return plugin_installed; }
   String getName()			{ return plugin_name; }
   String getDescription()		{ return plugin_description; }
   List<String> getRequiredPlugins()    { return plugin_requires; }
   String getWarning()                  { return plugin_warning; }

   @Override public String toString()	{ return plugin_name; }

   void install() throws IOException {
      if (plugin_installed) return;
      if (use_http) plugin_url = plugin_url.replace("https","http");
      int idx = plugin_url.lastIndexOf("/");
      String file = plugin_url.substring(idx+1);
      File root = BoardSetup.getSetup().getRootDirectory();
      File plugins = new File(root,BOARD_ECLIPSE_DROPINS);
      if (!plugins.exists()) plugins.mkdir();
      File p1 = new File(plugins,file);
      if (p1.exists()) {
         File p2 = new File(plugins,file + ".save");
         p1.renameTo(p2);
         p2.deleteOnExit();
       }
   
      URL u = null;
      try { 
	 u = new URI(plugin_url).toURL();
      }
      catch (URISyntaxException e) {
	 throw new IOException("BAD URI",e);
      }
      InputStream uins = u.openConnection().getInputStream();
      IvyFile.copyFile(uins,p1);
      plugin_file = p1;
      installResource();		// should no longer be needed
      plugin_installed = true;
    }

   private void installResource() {
      for (String res : plugin_resources) {
         try {
            JarFile jf = new JarFile(plugin_file);
            ZipEntry ze = jf.getEntry(res);
            if (ze == null) {
               BoardLog.logE("BOARD","Missing plugin resource file " + res);
               continue;
             }
            File libdir = BoardSetup.getSetup().getResourceDirectory();
            File tgt = new File(libdir,res);
            InputStream ins = jf.getInputStream(ze);
            IvyFile.copyFile(ins,tgt);
            jf.close();
            BoardSetup.getSetup().checkResourceFile(res);
          }
         catch (IOException _ex) {
          }
       }
    }

   void uninstall() {
      if (!plugin_installed) return;
      if (plugin_file == null) return;
      plugin_file.deleteOnExit();
      for (String main : plugin_mains) {
	 try {
	    Class<?> c = Class.forName(main);
	    Method m = c.getMethod("remove");
	    m.invoke(null);
	  }
	 catch (Exception e) { }
       }
      uninstallResource();
      plugin_installed = false;
    }

   void uninstallResource() {
      File ivv = BoardProperties.getPropertyDirectory();
      for (String res : plugin_resources) {
	 File f1 = new File(ivv,res);
	 f1.delete();
       }
    }

   void update() {
      if (!plugin_installed) return;
      if (use_http) plugin_url = plugin_url.replace("https","http");   
      long dlm = plugin_file.lastModified();
      URLConnection uc = null;
      try {
         URL u = new URI(plugin_url).toURL();
         uc = u.openConnection();
         long urldlm = uc.getLastModified();
         if (urldlm < dlm) return;
       }
      catch (Exception e) {
         return;
       }
   
      File f1 = new File(plugin_file.getPath() + ".save");
      plugin_file.renameTo(f1);
      try {
         InputStream uins = uc.getInputStream();
         IvyFile.copyFile(uins,plugin_file);
         f1.delete();
       }
      catch (IOException e) {
         f1.renameTo(plugin_file);
       }
    }

   private void checkInstalled() {
      if (use_http) plugin_url = plugin_url.replace("https","http");
      int idx = plugin_url.lastIndexOf("/");
      String file = plugin_url.substring(idx+1);
      File root = BoardSetup.getSetup().getRootDirectory();
      File plugins = new File(root,BOARD_ECLIPSE_DROPINS);
      File p1 = new File(plugins,file);
      System.err.println("BOARDUPDATE: CHECK plugin at " + p1);
      if (p1.exists()) {
         plugin_file = p1;
         plugin_installed = true;
         return;
       }
      plugins = new File(root,BOARD_ECLIPSE_PLUGINS);
      p1 = new File(plugins,file);
      System.err.println("BOARDUPDATE: CHECK plugin at " + p1);   if (p1.exists()) {
         plugin_file = p1;
         plugin_installed = true;
         return;
       }
      plugin_installed = false;
    }



   @Override public int compareTo(PluginData pd) { 
      return plugin_name.compareTo(pd.plugin_name);
    }

}	// end of inner class PluginData



}	// end of class BoardPluginManager




/* end of BoardPluginManager.java */

