/********************************************************************************/
/*										*/
/*		BvcrMain.java							*/
/*										*/
/*	Bubble Version Collaboration Repository server main program		*/
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


package edu.brown.cs.bubbles.bvcr;


import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class BvcrMain implements BvcrConstants
{




/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BvcrMain bm = new BvcrMain(args);
   bm.process();
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		mint_handle;
private ProcessMode	process_mode;
private BvcrMonitor	the_monitor;
private List<String>	update_files;
private Map<String,File> update_projects;

private Map<String,BvcrVersionManager> manager_map;
private Map<String,SecretKey> key_map;
private Map<String,String>    id_map;
private Map<String,String>    user_map;
private Map<String,BvcrDifferenceSet> diff_map;
private Map<String,BvcrChangeSet>     change_map;
private Map<String,BvcrProject> project_map;



enum ProcessMode {
   SERVER,			// act as a server
   CHANGESET,			// just list changes
   UPDATE,
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BvcrMain(String [] args)
{
   mint_handle = null;
   process_mode = ProcessMode.SERVER;
   the_monitor = null;
   update_files = null;
   update_projects = null;

   project_map = new HashMap<>();
   manager_map = new HashMap<>();
   key_map = new HashMap<>();
   id_map = new HashMap<>();
   user_map = new HashMap<>();
   diff_map = new HashMap<>();
   change_map = new HashMap<>();

   scanArgs(args);

   IvySetup.setup();
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {           // -m <mint handle>
	    mint_handle = args[++i];
	  }
	 else if (args[i].startsWith("-C")) {                           // -Changeset
	    process_mode = ProcessMode.CHANGESET;
	  }
	 else if (args[i].startsWith("-S")) {                           // -Server
	    process_mode = ProcessMode.SERVER;
	  }
	 else if (args[i].startsWith("-U") && i+1 < args.length) {     // -Update <proj@file>
	    process_mode = ProcessMode.UPDATE;
	    if (update_files == null) update_files = new ArrayList<String>();
	    update_files.add(args[++i]);
	  }
         else if (args[i].startsWith("-V") && i+1 < args.length) {      // -V <loglevel>
            String lvl = args[++i];
            switch (lvl.charAt(0)) {
               case 'E' :
                  IvyLog.setLogLevel(IvyLog.LogLevel.ERROR);
                  break;
               case 'W' :
                  IvyLog.setLogLevel(IvyLog.LogLevel.WARNING);
                  break;
               case 'I' :
                  IvyLog.setLogLevel(IvyLog.LogLevel.INFO);
                  break;
               default :
               case 'D' :
                  IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
                  break;
             }
          }
         else if (args[i].startsWith("-L") && i+1 < args.length) {      // -L <log file>
            String path = args[++i];
            IvyLog.setLogFile(path);
          }
         else if (args[i].startsWith("-E")) {                           // -E 
            IvyLog.useStdErr(true);
          }
         else if (args[i].startsWith("-p") && i+2 < args.length) {      // -p <project>
            process_mode = ProcessMode.CHANGESET;
            if (update_projects == null) {
               update_projects = new HashMap<>();
             }
            String pnm = args[++i];
            String root = args[++i];
            update_projects.put(pnm,new File(root));
          }
	 else badArgs();
       }
      else if (process_mode == ProcessMode.UPDATE) {
         update_files.add(args[i]);
       }
      else badArgs();
    }
   
   if (process_mode == ProcessMode.CHANGESET) {
      if (mint_handle == null && update_projects == null) badArgs();
    }
   else {
      if (mint_handle == null) badArgs();
    }
}



private void badArgs()
{
   System.err.println("BVCR: bvcrmain -m <mint> [-Changeset | -Server]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   String s = BvcrUpload.getRepoUrl();
   if (s == null) {
      IvyLog.logI("BVCR","No repository configured");
      System.exit(0);
    }

   if (update_projects == null || process_mode != ProcessMode.CHANGESET) {
      the_monitor = new BvcrMonitor(this,mint_handle);
      the_monitor.loadProjects();
      for (BvcrProject bp : project_map.values()) {
         IvyLog.logD("BVCR","Check project " + bp.getName());
         BvcrVersionManager bvm = BvcrVersionManager.createVersionManager(bp,the_monitor);
         if (bvm != null) {
            manager_map.put(bp.getName(),bvm);
          }
       }
    }
   else {
      for (Map.Entry<String,File> ent : update_projects.entrySet()) {
         String pnm = ent.getKey();
         File root = ent.getValue();
         BvcrProject bp = new BvcrProject(pnm); 
         BvcrVersionManager bvm = BvcrVersionManager.createVersionManager(bp,root);
         if (bvm != null) {
            setProject(bp);
            manager_map.put(pnm,bvm);
          }
       } 
    }

   if (manager_map.size() == 0) {
      IvyLog.logI("BVCR","No projects under version management");
      System.exit(0);
    }

   setupKeys();

   switch (process_mode) {
      case CHANGESET :
	 processChanges(true);
         System.err.println("CHANGES UPDATED");
	 break;
      case SERVER :
         processChanges(true);
	 the_monitor.server();
	 break;
      case UPDATE :
	 processUpdates();
	 break;
    }
}




/********************************************************************************/
/*										*/
/*	Project management methods						*/
/*										*/
/********************************************************************************/

void setProject(BvcrProject bp)
{
   project_map.put(bp.getName(),bp);
}



BvcrProject findProject(String id)
{
   return project_map.get(id);
}



List<BvcrProject> getProjects()
{
   return new ArrayList<BvcrProject>(project_map.values());
}



/********************************************************************************/
/*										*/
/*	Encryption key management						*/
/*										*/
/********************************************************************************/

private void setupKeys()
{
   for (Map.Entry<String,BvcrVersionManager> ent : manager_map.entrySet()) {
      BvcrVersionManager bvm = ent.getValue();
      String proj = ent.getKey();
      String id = bvm.getRepositoryId();
      SecretKey sk = null;
      if (id != null) {
	 try {
	    KeySpec ks = new PBEKeySpec(id.toCharArray(),KEY_SALT,KEY_COUNT);
	    SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
	    sk = kf.generateSecret(ks);
	    key_map.put(proj,sk);
	  }
	 catch (Exception e) {
	    IvyLog.logE("BVCR","Problem constructing key",e);
	  }
       }
      String nm = bvm.getRepositoryName();
      File f = bvm.getRootDirectory();
      f = IvyFile.getCanonical(f);              // use canonical to distinguish systems
      String uid = System.getProperty("user.name");
      uid += "@" + f.getPath();
      IvyLog.logD("BVCR","Work with repo=" + nm + ", User=" + uid);
      nm = getEncodedName(nm,"R");
      if (nm == null) continue;
      id_map.put(proj,nm);
      uid = getEncodedName(uid,"U");
      user_map.put(proj,uid);
      change_map.put(proj,new BvcrChangeSet(this,proj,nm,uid,sk));
      IvyLog.logD("BVCR","Change set initialized for " + proj);
    }
}



private String getEncodedName(String nm,String pfx)
{
   if (nm == null) return null;

   try {
      MessageDigest mdi = MessageDigest.getInstance("MD5");
      mdi.update(nm.getBytes());
      byte [] drslt = mdi.digest();
      long rslt = 0;
      for (int i = 0; i < drslt.length; ++i) {
	 int j = i % 8;
	 long x = (drslt[i] & 0xff);
	 rslt ^= (x << (j*8));
       }
      rslt &= 0x7fffffffffffffffL;
      nm = pfx + Long.toString(rslt);
    }
   catch (NoSuchAlgorithmException e) {
      IvyLog.logE("BVCR","Problem creating encrypted id",e);
      e.printStackTrace();
    }

   return nm;
}




/********************************************************************************/
/*										*/
/*	Change set processing methods						*/
/*										*/
/********************************************************************************/

private void processChanges(boolean force)
{
   IvyLog.logD("BVCR","Process changes");
   
   for (Map.Entry<String,BvcrVersionManager> ent : manager_map.entrySet()) {
      BvcrVersionManager bvm = ent.getValue();
      String id = ent.getKey();
      BvcrDifferenceSet ds = getDifferenceSet(id);
      if (ds == null) continue;
      if (force || ds.computationNeeded()) {
         IvyLog.logD("BVCR","Process changes for " + id);
	 bvm.getDifferences(ds);
	 IvyXmlWriter xw = new IvyXmlWriter();
	 ds.outputXml(xw);
	 SecretKey sk = key_map.get(id);
	 String rid = id_map.get(id);
	 String uid = user_map.get(id);
         
         IvyLog.logD("BVCR","Change set result " + uid + " " +
               rid + " " + xw.toString());
         
	 if (uid != null && rid != null &&
               !BvcrUpload.upload(xw.toString(),uid,rid,sk)) {
	    IvyLog.logE("BVCR","Upload failed");
	  }
       }
    }
}



void handleFileChanged(String proj,File file)
{
   BvcrDifferenceSet ds = getDifferenceSet(proj);
   if (ds != null) ds.handleFileChanged(file);
}


private BvcrDifferenceSet getDifferenceSet(String proj)
{
   BvcrDifferenceSet ds = null;
   synchronized (diff_map) {
      ds = diff_map.get(proj);
      if (ds == null) {
         ds = new BvcrDifferenceSet(this,findProject(proj));
         diff_map.put(proj,ds);
       }
    }
   
   return ds;
}



void handleEndUpdate()
{
   processChanges(false);
}



File getRootDirectory(String proj)
{
   BvcrVersionManager bvm = manager_map.get(proj);
   if (bvm == null) return null;
   return bvm.getRootDirectory();
}



BvcrVersionManager getManager(String proj)
{
   BvcrVersionManager bvm = manager_map.get(proj);
   return bvm;
}





/********************************************************************************/
/*										*/
/*	Methods to get the change set for a file				*/
/*										*/
/********************************************************************************/

private void processUpdates()
{
   String proj = null;

   IvyXmlWriter xw = new IvyXmlWriter();
   for (String s : update_files) {
      int idx = s.indexOf("@");
      if (idx >= 0) {
	 proj = s.substring(0,idx);
	 s = s.substring(idx+1);
       }
      File f = new File(s);
      xw.begin("FILE");
      xw.field("NAME",s);
      if (proj != null) xw.field("PROJECT",proj);
      findChanges(proj,f,xw);
      findHistory(proj,f,xw);
      xw.end("FILE");
    }

   IvyLog.logD("BVCR","CHANGES: " + xw.toString());
}




void findChanges(String proj,File file,IvyXmlWriter xw)
{
   BvcrChangeSet cs = change_map.get(proj);
   if (cs != null) cs.findChanges(file,xw);
}


void findActualChanges(String proj,File file,BvcrVersionManager bvm,IvyXmlWriter xw)
{
   BvcrChangeSet cs = change_map.get(proj);
   if (cs != null) {
      cs.findActualChanges(file,bvm,xw); 
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle information gathering 				*/
/*										*/
/********************************************************************************/

void findHistory(String proj,File file,IvyXmlWriter xw)
{
   BvcrVersionManager bvm = manager_map.get(proj);
   if (bvm == null) return;
   bvm.findHistory(file,xw);
}



void findFileDiffs(String proj,File file,String vfr,String vto,IvyXmlWriter xw)
{
   BvcrVersionManager bvm = manager_map.get(proj);
   bvm.findFileDiffs(this,file,vfr,vto,xw);
}




}	// end of class BvcrMain




/* end of BvcrMain.java */
