/********************************************************************************/
/*										*/
/*		BemaCloudSetup.java						*/
/*										*/
/*	Handle setting up cloud bubbles 					*/
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



package edu.brown.cs.bubbles.bema;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingGridPanel;

class BemaCloudSetup implements BemaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BoardSetup	board_setup;
private ServerMonitor	server_monitor;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BemaCloudSetup(BoardSetup bs)
{
   board_setup = bs;
   server_monitor = null;
}


/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

boolean doSetup()
{
   CloudDialog cd = new CloudDialog();
   return cd.process();
}



boolean startServer()
{
   // check if bubbles is already running -- send ping message

   BoardProperties bp = BoardProperties.getProperties("Bema");
   String ssh = bp.getProperty("Bema.cloud.sshhost").trim();
   String ahost = bp.getProperty("Bema.cloud.actualhost").trim();
   if (ahost == null || ahost.length() == 0) ahost = "@";
   String proj = bp.getProperty("Bema.cloud.project");
   String mid = System.getProperty("edu.brown.cs.bubbles.MINT");
   String cmd = board_setup.getBinaryPath("cloudrunner");
   cmd += " " + ssh + " " + ahost + " " + proj + " " + mid;

   server_monitor = new ServerMonitor(cmd);
   server_monitor.start();
   if (!server_monitor.waitForReady()) return false;

   return true;
}



void stopServer()
{
   if (server_monitor != null) server_monitor.stopServer();
}


/********************************************************************************/
/*										*/
/*	Web relay checking							*/
/*										*/
/********************************************************************************/

private boolean checkWebRelay(String id)
{
   if (id == null || id.length() == 0) return false;
   if (!id.contains(":")) id += ":8080";
   if (!id.contains("/")) id += "/mint/mint";
   if (!id.startsWith("http")) id = "http://" + id;

   try {
      URL u = new URI(id).toURL();
      URLConnection c = u.openConnection();
      c.setConnectTimeout(5000);
      InputStream ins = c.getInputStream();
      String cnts = IvyFile.loadFile(ins);
      if (cnts.contains("PONG")) return true;
    }
   catch (Exception e) {
      System.err.println("Problm connecting to web relay " + id + ": " + e);
      return false;
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	User dialog								*/
/*										*/
/********************************************************************************/

private class CloudDialog implements ActionListener, CaretListener, UndoableEditListener {

   private JButton accept_button;
   private JTextField host_field;
   private JTextField actual_host;
   private JTextField project_name;
   private JTextField web_relay;
   private JTextField web_key;
   private boolean result_status;
   private JDialog working_dialog;
   private ServerChecker   server_checker;

   CloudDialog() {
      result_status = false;
      server_checker = null;
   
      BoardProperties bp = BoardProperties.getProperties("Bema");
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.setBackground(BoardColors.getColor("Buda.Bubbles.Color"));
      pnl.setOpaque(true);
      pnl.beginLayout();
      pnl.addBannerLabel("Cloud Bubbles Setup");
      pnl.addSeparator();
      host_field = pnl.addTextField("SSH Host",bp.getProperty("Bema.cloud.sshhost"),36,this,this);
      actual_host = pnl.addTextField("Actual Host",bp.getProperty("Bema.cloud.actualhost"),24,this,this);
      web_relay = pnl.addTextField("Web Relay",bp.getProperty("Bema.web.url"),36,this,this);
      project_name = pnl.addTextField("Project",bp.getProperty("Bema.cloud.project"),48,this,this);
   
      web_key = pnl.addTextField("Web Key",null,36,this,this);
   
      pnl.addBottomButton("Cancel","CANCEL",this);
      accept_button = pnl.addBottomButton("Accept","ACCEPT",this);
      accept_button.setEnabled(false);
      pnl.addBottomButtons();
   
      working_dialog = new JDialog((JFrame) null,"Cloud Bubbles Setup",true);
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

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("ACCEPT")) {
         result_status = true;
         working_dialog.setVisible(false);
         setProperties();
       }
      else if (cmd.equals("CANCEL")) {
         result_status = false;
         working_dialog.setVisible(false);
       }
    }

   @Override public void caretUpdate(CaretEvent e) {
      checkStatus();
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      checkStatus();
    }

   private void checkStatus() {
      boolean isokay = true;
   
      if (web_key.getText() == null || web_key.getText().trim().length() == 0) {
         File f1 = BoardSetup.getPropertyBase();
         if (f1.exists()) {
            File f2 = new File(f1,"webkey");
            try {
               String s = IvyFile.loadFile(f2);
               if (s != null) {
        	  web_key.setText(s.trim());
        	}
             }
            catch (IOException e) { }
          }
       }
      if (web_key.getText() == null || web_key.getText().trim().length() == 0) isokay = false;
   
      String weburl = web_relay.getText();
      if (isokay) {
         if (!checkWebRelay(weburl)) isokay = false;
       }
   
      String ssh = host_field.getText().trim();
      String ahost = actual_host.getText().trim();
      String ws = project_name.getText().trim();
      if (isokay) {
         checkProject(ssh,ahost,ws);
       }
      accept_button.setEnabled(false);
    }

   void setOkay() {
      accept_button.setEnabled(true);
    }

   private void setProperties() {
      BoardProperties bp = BoardProperties.getProperties("Bema");
      String host = host_field.getText().trim();
      bp.setProperty("Bema.cloud.sshhost",host);
      String ahost = actual_host.getText().trim();
      if (ahost == null) ahost = "";
      bp.setProperty("Bema.cloud.actualhost",ahost);
      String ws = project_name.getText().trim();
      bp.setProperty("Bema.cloud.project",ws);
      BoardProperties sysbp = BoardProperties.getProperties("System");
      sysbp.setProperty("Cloud.workspace",ws);
   
      String wr = web_relay.getText().trim();
      if (!wr.contains(":")) wr += ":8080";
      if (!wr.contains("/")) wr += "/mint/mint";
      if (!wr.startsWith("http")) wr = "http://" + wr;
      bp.setProperty("Bema.web.url",wr);
   
      String wk = web_key.getText().trim();
      File f1 = BoardSetup.getPropertyBase();
      File f2 = new File(f1,"webkey");
      try {
         FileWriter fw = new FileWriter(f2);
         fw.write(wk + "\n");
         fw.close();
       }
      catch (IOException e) { }
   
      try {
         bp.save();
         sysbp.save();
       }
      catch (IOException e) { }
    }

   private void checkProject(String ssh,String host,String path) {
      if (ssh == null || ssh.trim().length() == 0) return;
      if (host != null && host.trim().length() == 0) host = null;
      if (path == null || path.trim().length() == 0) return;
      
      synchronized (this) {
         if (server_checker != null) {
            server_checker.stopCheck();
            while (server_checker.isAlive()) {
               try {
                  wait(10);
                }
               catch (InterruptedException e) { }
             }
            server_checker = null;
          }
         server_checker = new ServerChecker(this,ssh,host,path);
         server_checker.start();
       }
    }

}	// end of inner class Cloud Dialog



/********************************************************************************/
/*										*/
/*	ServerChecker -- code to check server					*/
/*										*/
/********************************************************************************/

private class ServerChecker extends Thread {

   private String ssh_host;
   private String actual_host;
   private String project_id;
   private CloudDialog for_dialog;
   private IvyExec our_process;

   ServerChecker(CloudDialog cd,String s,String a,String p) {
      super("CheckSSHConnection");
      for_dialog = cd;
      ssh_host = s;
      actual_host = a;
      project_id = p;
      our_process = null;
    }

   void stopCheck() {
      interrupt();
      IvyExec ie = our_process;
      if (ie != null) ie.destroy();
      our_process = null;
    }

   @Override public void run() {
      if (checkProject()) {
         if (!isInterrupted()) {
            for_dialog.setOkay();
          }
       }
    }

   private boolean checkProject() {
      String cmd = board_setup.getBinaryPath("cloudcheckssh");
      cmd += " " + ssh_host;
      if (actual_host != null) cmd += " " + actual_host;
      else cmd += " @";
      cmd += " " + project_id;
      try {
         IvyExec exec = new IvyExec(cmd);
         our_process = exec;
         int sts = exec.waitFor();
         our_process = null;
         if (sts == 0) return true;
       }
      catch (IOException e) { }
   
      return false;
    }

}	// end of inner class ServerChecker



/********************************************************************************/
/*										*/
/*	ServerMonitor								*/
/*										*/
/********************************************************************************/

private class ServerMonitor extends Thread {

   private String server_command;
   private IvyExec server_process;
   private Boolean server_ready;

   ServerMonitor(String cmd) {
      super("CloudServerMonitor");
      server_command = cmd;
      server_process = null;
      server_ready = null;
    }

   @Override public void run() {
      try {
	 server_process = new IvyExec(server_command,IvyExec.READ_OUTPUT);
	 InputStream ins = server_process.getInputStream();
	 try (BufferedReader br = new BufferedReader(new InputStreamReader(ins))) {
	    for ( ; ; ) {
	       String ln = br.readLine();
	       if (ln == null) break;
	       synchronized (this) {
		  if (server_ready == null) {
		     if (ln.contains(SERVER_READY_STRING)) {
			server_ready = true;
			notifyAll();
			return;
		      }
		   }
		}
	       if (!server_process.isRunning()) break;
	     }
	    server_process.destroy();
	  }
       }
      catch (IOException e) {
	 if (server_process != null) server_process.destroy();
       }

      synchronized (this) {
	 server_ready = false;
	 notifyAll();
       }
   }

   boolean waitForReady() {
      synchronized (this) {
	 while (server_ready == null) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
       }
      return server_ready;
    }

   void stopServer() { }

}


}	// end of class BemaCloudSetup




/* end of BemaCloudSetup.java */
