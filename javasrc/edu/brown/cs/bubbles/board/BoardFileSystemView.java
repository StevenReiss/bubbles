/********************************************************************************/
/*										*/
/*		BoardFileSystemView.java					*/
/*										*/
/*	Provide a view of local/remote file system				*/
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



package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BoardFileSystemView extends FileSystemView implements BoardConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static MintControl	mint_control = null;
private static FileSystemView	default_view = null;
private static boolean		server_running = false;
private static FileSystemView	board_view = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static FileSystemView getFileSystemView()
{
   if (board_view == null) {
      switch (BoardSetup.getSetup().getRunMode()) {
	 case NORMAL :
	 case SERVER :
	    board_view = FileSystemView.getFileSystemView();
	    break;
	 case CLIENT :
	    if (mint_control == null) setupRemoteServer();
	    board_view = new BoardFileSystemView();
	    break;
       }
    }
   return board_view;
}



private BoardFileSystemView() { }




/********************************************************************************/
/*										*/
/*	Server methods								*/
/*										*/
/********************************************************************************/

synchronized static void setupRemoteServer()
{
   switch (BoardSetup.getSetup().getRunMode()) {
      case NORMAL :
	 return;
      case SERVER :
	 default_view = FileSystemView.getFileSystemView();
	 startServer();
	 break;
      case CLIENT :
	 mint_control = BoardSetup.getSetup().getMintControl();
	 break;
    }
}



private static void startServer()
{
   MintControl mc = BoardSetup.getSetup().getMintControl();

   if (server_running) return;

   List<String> args = new ArrayList<String>();
   args.add(IvyExecQuery.getJavaPath());
   args.add("-Xmx32m");
   args.add("-cp");
   args.add(System.getProperty("java.class.path"));
   args.add("edu.brown.cs.bubbles.board.BoardFileSystemView");
   args.add("-m");
   args.add(BoardSetup.getSetup().getMintName());

   for (int i = 0; i < 100; ++i) {
      MintDefaultReply rply = new MintDefaultReply();
      mc.send("<BOARDREMOTE CMD='PING'/>",rply,MINT_MSG_FIRST_NON_NULL);
      String rslt;
      if (i == 0) rslt = rply.waitForString(1000);
      else rslt = rply.waitForString();
      if (rslt != null) {
	 server_running = true;
	 break;
       }
      if (i == 0) {
	 try {
	    IvyExec exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
	    BoardLog.logD("BOARD","Run " + exec.getCommand());
	  }
	 catch (IOException e) {
	    break;
	  }
       }
      try {
	 Thread.sleep(2000);
       }
      catch (InterruptedException e) { }
    }
   if (!server_running) {
      BoardLog.logE("BOARD","Unable to start remote file server");
      server_running = true;
    }
}




/********************************************************************************/
/*										*/
/*	File System methods							*/
/*										*/
/********************************************************************************/

@Override public File createFileObject(File dir,String nm)
{
   if (dir instanceof RemoteFile)
      return new RemoteFile((RemoteFile) dir,nm);
   else
      return new RemoteFile(dir.getPath(),nm);
}


@Override public File createFileObject(String path)
{
   return new RemoteFile(path);
}


@Override protected File createFileSystemRoot(File f)
{
   while (f.getParent() != null) f = f.getParentFile();

   if (f instanceof RemoteFile) return f;

   return new RemoteFile(f.getPath());
}


@Override public File createNewFolder(File dir) throws IOException
{
   Element xml = createDirectory(dir,false,true);

   String fnm = IvyXml.getAttrString(xml,"FILE");
   if (fnm == null) throw new IOException("Failed to create new folder");

   return new RemoteFile(fnm);
}


@Override public File getChild(File parent,String nm)
{
   return new RemoteFile((RemoteFile) parent,nm);
}


@Override public File getDefaultDirectory()
{
   Element xml = getSystemInfo();

   return new RemoteFile(IvyXml.getAttrString(xml,"DEFAULT"));
}


@Override public File [] getFiles(File dir,boolean usehiding)
{
   if (usehiding) return dir.listFiles(new HideFilter());
   else return dir.listFiles();
}


private static class HideFilter implements java.io.FileFilter {

   @Override public boolean accept(File f) {
      return !f.isHidden();
    }

}	// end of inner class HideFilter


@Override public File getHomeDirectory()
{
   Element xml = getSystemInfo();

   return new RemoteFile(IvyXml.getAttrString(xml,"HOME"));
}



File getRemoteLibraryDirectory()
{
   Element xml = getSystemInfo();

   return new RemoteFile(IvyXml.getAttrString(xml,"BUBBLESLIB"));
}


@Override public File getParentDirectory(File f)
{
   File f1 = f.getParentFile();
   if (f1 instanceof RemoteFile) return f1;

   return new RemoteFile(f1.getPath());
}


@Override public File [] getRoots()
{
   Element xml = getSystemInfo();
   int rct = IvyXml.getAttrInt(xml,"ROOTCT");
   File [] rslt = new File[rct];

   int i = 0;
   for (Element re : IvyXml.children(xml,"ROOT")) {
      rslt[i++] = new RemoteFile(IvyXml.getAttrString(re,"NAME"));
    }

   return rslt;
}


@Override public String getSystemDisplayName(File f)
{
   Element xml = getFileInfo(f);

   return IvyXml.getAttrString(xml,"DISP");
}


@Override public Icon getSystemIcon(File f)
{
   return null;
}


@Override public String getSystemTypeDescription(File f)
{
   Element e = getFileInfo(f);

   return IvyXml.getAttrString(e,"DESC");
}


@Override public boolean isComputerNode(File dir)
{
   Element e = getFileInfo(dir);

   return IvyXml.getAttrBool(e,"ISCOMP");
}


@Override public boolean isDrive(File dir)
{
   Element e = getFileInfo(dir);

   return IvyXml.getAttrBool(e,"ISDRIVE");
}


@Override public boolean isFileSystem(File f)
{
   Element e = getFileInfo(f);

   return IvyXml.getAttrBool(e,"ISFILESYS");
}


@Override public boolean isFileSystemRoot(File dir)
{
   Element e = getFileInfo(dir);

   return IvyXml.getAttrBool(e,"ISFSROOT");
}


@Override public boolean isFloppyDrive(File dir)
{
   Element e = getFileInfo(dir);

   return IvyXml.getAttrBool(e,"ISFLOP");
}


@Override public boolean isHiddenFile(File f)
{
   return f.isHidden();
}


@Override public boolean isParent(File dir,File file)
{
   return dir.equals(file.getParentFile());
}


@Override public boolean isRoot(File f)
{
   Element e = getFileInfo(f);

   return IvyXml.getAttrBool(e,"ISROOT");
}


@Override public Boolean isTraversable(File f)
{
   Element e = getFileInfo(f);

   return IvyXml.getAttrBool(e,"TRAVERSE");
}



/********************************************************************************/
/*										*/
/*	Remote File								*/
/*										*/
/********************************************************************************/

private class RemoteFile extends File {

   private static final long serialVersionUID = 1;

   RemoteFile(RemoteFile par,String child) {
      super(par,child);
    }

   RemoteFile(String path) {
      super(path);
    }

   RemoteFile(String par,String child) {
      super(par,child);
    }

   @Override public boolean canExecute() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrBool(e,"CANE");
    }

   @Override public boolean canRead() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrBool(e,"CANR");
    }

   @Override public boolean canWrite() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrBool(e,"CANW");
    }

   @Override public boolean createNewFile() {
      return false;
    }

   @Override public boolean delete() {
      Element e = fileDelete(this,false);
      return IvyXml.getAttrBool(e,"STATUS");
    }

   @Override public void deleteOnExit() {
      fileDelete(this,true);
    }

   @Override public boolean exists() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrBool(e,"EXIST");
    }

   @Override public File getCanonicalFile() throws IOException {
      return new RemoteFile(getCanonicalPath());
    }

   @Override public String getCanonicalPath() throws IOException {
      Element xml = fileCanonical(this);
      String s = IvyXml.getAttrString(xml,"FILE");
      if (!IvyXml.getAttrBool(xml,"STATUS")) {
	 throw new IOException(s);
       }

      return IvyXml.getAttrString(xml,"FILE");
    }

   @Override public boolean isDirectory() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrBool(e,"ISD");
    }

   @Override public boolean isFile() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrBool(e,"ISF");
    }

   @Override public boolean isHidden() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrBool(e,"ISH");
    }

   @Override public long lastModified() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrLong(e,"DLM");
    }

   @Override public long length() {
      Element e = getFileInfo(this);
      return IvyXml.getAttrLong(e,"LEN");
    }

   @Override public String [] list() {
      return list(null);
    }

   @Override public String [] list(FilenameFilter filter) {
      File [] fls = listFiles(filter);
      if (fls == null) return null;
      String [] rslt = new String[fls.length];
      for (int i = 0; i < fls.length; ++i) rslt[i] = fls[i].getName();
      return rslt;
    }

   @Override public File [] listFiles() {
      return listFiles(null,null);
    }

   @Override public File [] listFiles(java.io.FileFilter f) {
      return listFiles(f,null);
    }

   @Override public File [] listFiles(FilenameFilter f) {
      return listFiles(null,f);
    }

   private File [] listFiles(java.io.FileFilter flt1,FilenameFilter flt2) {
      List<File> rslt = new ArrayList<File>();
      Element xml = getFiles(this);
      int ct = IvyXml.getAttrInt(xml,"COUNT",-1);
      if (ct < 0) return null;
      for (Element fe : IvyXml.children(xml,"FILE")) {
	 String nm = IvyXml.getAttrString(fe,"NAME");
	 if (flt2 != null && !flt2.accept(this,nm)) continue;
	 File f1 = new RemoteFile(this,nm);
	 if (flt1 != null && !flt1.accept(f1)) continue;
	 rslt.add(f1);
       }
      File [] rf = new File[rslt.size()];
      rf = rslt.toArray(rf);
      return rf;
    }

   @Override public boolean mkdir() {
      Element e = createDirectory(this,false,false);
      return IvyXml.getAttrBool(e,"STATUS");
    }

   @Override public boolean mkdirs() {
      Element e = createDirectory(this,true,false);
      return IvyXml.getAttrBool(e,"STATUS");
    }

   @Override public boolean renameTo(File f) {
      Element xml = fileRename(this,f);
      return IvyXml.getAttrBool(xml,"STATUS");
    }

   @Override public boolean setExecutable(boolean ex) {
      return false;
    }

   @Override public boolean setExecutable(boolean ex,boolean owner) {
      return false;
    }

   @Override public boolean setLastModified(long t) {
      return false;
    }

   @Override public boolean setReadable(boolean r) {
      return false;
    }

   @Override public boolean setReadable(boolean r,boolean owner) {
      return false;
    }

   @Override public boolean setReadOnly() {
      return false;
    }

   @Override public boolean setWritable(boolean w) {
      return false;
    }

   @Override public boolean setWritable(boolean w,boolean owner) {
      return false;
    }

}	// end of inner class RemoteFile




/********************************************************************************/
/*										*/
/*	Calls to get remote info						*/
/*										*/
/********************************************************************************/

private static Element getFileInfo(File f)
{
   return sendMessage("FILEINFO",f,null,null);
}


private static Element getSystemInfo()
{
   return sendMessage("SYSINFO",null,null,null);
}



private static Element createDirectory(File f,boolean dirs,boolean newf)
{
   String x = "DIRS='" + Boolean.toString(dirs) + "'";
   x += " NEW='" + Boolean.toString(newf) + "'";

   return sendMessage("MKDIR",f,x,null);
}


private static Element getFiles(File f)
{
   return sendMessage("LIST",f,null,null);
}


private static Element fileDelete(File f,boolean exit)
{
   String x = "ONEXIT='" + Boolean.toString(exit) + "'";

   return sendMessage("DELETE",f,x,null);
}


private static Element fileCanonical(File f)
{
   return sendMessage("CANON",f,null,null);
}


private static Element fileRename(File f,File dest)
{
   String x = "TONAME='" + dest.getAbsolutePath() + "'";

   return sendMessage("RENAME",f,x,null);
}



private static Element sendMessage(String cmd,File f,String x,String q)
{
   String msg = "<BOARDREMOTE ";
   msg += "CMD='" + cmd + "'";
   if (f != null) {
      msg += " NAME='" + f.getName() + "' PARENT='" + f.getParent() + "'";
    }
   if (x != null) msg += " " + x;
   msg += ">";
   if (q != null) msg += q;
   msg += "</BOARDREMOTE>";

   Element rslt = null;
   for (int i = 0; i < 10; ++i) {
      MintDefaultReply mdr = new MintDefaultReply();
      mint_control.send(msg,mdr,MINT_MSG_FIRST_NON_NULL);
      rslt = mdr.waitForXml(10000 + i*2000);
      if (rslt != null) break;
    }

   IvyLog.logD("REMOTE","Send: " + msg + " => " + IvyXml.convertXmlToString(rslt));

   return rslt;
}



/********************************************************************************/
/*										*/
/*	FileServer to handle remote requests					*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   String mnm = null;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {                   // -m <mint>
	    mnm = args[++i];
	  }
       }
    }
   if (mnm == null) {
      System.err.println("-m [MINT] option required");
      System.exit(1);
    }

   File f = new File(System.getProperty("user.home"));
   File f1 = new File(f,".bubbles");
   File f2 = new File(f1,"logs");
   f2.mkdirs();
   File f3 = new File(f2,"RemoteFileSystem.log");
   IvyLog.setLogFile(f3);
   IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
   IvyLog.setupLogging("BOARD",false);

   default_view = FileSystemView.getFileSystemView();
   MintControl mc = MintControl.create(mnm,MintSyncMode.ONLY_REPLIES);
   FileServer fs = new FileServer();

   mc.register("<BOARDREMOTE CMD='_VAR_0' />",fs);
   mc.register("<BUBBLES DO='EXIT' />",new ExitHandler());

   for ( ; ; ) {
      synchronized (fs) {
	 try {
	    fs.wait();
	  }
	 catch (InterruptedException e) { }
       }
    }
}




private static class FileServer implements MintHandler {

   FileServer() {
      // MintControl mc = BoardSetup.getSetup().getMintControl();
      // mc.register("<BOARDREMOTE CMD='_VAR_0' />",this);
    }

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      Element xml = msg.getXml();
      String rslt = null;
      String fnm = IvyXml.getAttrString(xml,"NAME");
      String pnm = IvyXml.getAttrString(xml,"PARENT");
      IvyLog.logD("BOARD","FILE SYSTEM COMMAND " + cmd);

      IvyXmlWriter xw = new IvyXmlWriter();
      try {
	 File f = null;
	 if (pnm != null) {
	    f = new File(pnm);
	    if (fnm != null) f = new File(f,fnm);
	  }
	 else if (fnm != null) f = new File(fnm);

	 if (cmd == null) {
	    IvyLog.logE("BOARD","Bad Remote file message: " + msg.getText());
	  }
	 else if (cmd.equals("FILEINFO")) {
	    handleFileInfo(f,xw);
	  }
	 else if (cmd.equals("SYSINFO")) {
	    handleSysInfo(xw);
	  }
	 else if (cmd.equals("MKDIR")) {
	    handleMkdir(f,IvyXml.getAttrBool(xml,"DIRS"),IvyXml.getAttrBool(xml,"NEW"),xw);
	  }
	 else if (cmd.equals("LIST")) {
	    handleListFiles(f,xw);
	  }
	 else if (cmd.equals("DELETE")) {
	    handleDelete(f,IvyXml.getAttrBool(xml,"EXIT"),xw);
	  }
	 else if (cmd.equals("CANON")) {
	    handleCanonical(f,xw);
	  }
	 else if (cmd.equals("RENAME")) {
	    handleRename(f,IvyXml.getAttrString(xml,"TONAME"),xw);
	  }
	 else if (cmd.equals("PING")) {
	    rslt = "<PONG/>";
	  }
	 else if (cmd.equals("EXIT")) {
	    System.exit(0);
	  }

	 if (rslt == null) {
	    rslt = xw.toString();
	    if (rslt.length() == 0) rslt = null;
	  }
       }
      catch (Throwable t) {
	 IvyLog.logE("BOARD","Problem running command " + cmd,t);
	 rslt = null;
       }

      msg.replyTo(rslt);

      xw.close();
    }

}	// end of inner class FileServer



private static class ExitHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      System.exit(0);
    }

}	// end of inner class ExitHandler



private static void handleFileInfo(File f,IvyXmlWriter xw)
{
   xw.begin("RESULT");
   xw.field("CANE",f.canExecute());
   xw.field("CANR",f.canRead());
   xw.field("CANW",f.canWrite());
   xw.field("ISD",f.isDirectory());
   xw.field("ISF",f.isFile());
   xw.field("ISH",f.isHidden());
   xw.field("DLM",f.lastModified());
   xw.field("LEN",f.length());
   xw.field("EXIST",f.exists());

   if (default_view == null) {
      default_view =  FileSystemView.getFileSystemView();
    }
   xw.field("ISDRIVE",default_view.isDrive(f));
   xw.field("ISFILESYS",default_view.isFileSystem(f));
   xw.field("ISFSROOT",default_view.isFileSystemRoot(f));
   xw.field("ISFLOP",default_view.isFloppyDrive(f));
   xw.field("ISROOT",default_view.isRoot(f));
   xw.field("TRAVERSE",default_view.isTraversable(f));
   xw.field("ISCOMP",default_view.isComputerNode(f));
   xw.field("DESC",default_view.getSystemTypeDescription(f));
   xw.field("DISP",default_view.getSystemDisplayName(f));

   xw.end();
}


private static void handleSysInfo(IvyXmlWriter xw)
{
   xw.begin("RESULT");
   xw.field("HOME",default_view.getHomeDirectory().getPath());
   xw.field("DEFAULT",default_view.getDefaultDirectory().getPath());
   File [] rts = default_view.getRoots();
   xw.field("ROOTCT",rts.length);
   for (File f : rts) {
      xw.begin("ROOT");
      xw.field("NAME",f.getAbsolutePath());
      xw.end("ROOT");
    }

   File f1 = BoardSetup.getSetup().getLibraryDirectory();
   xw.field("BUBBLESLIB",f1.getAbsolutePath());

   xw.end();
}



private static void handleMkdir(File f,boolean dirs,boolean newd,IvyXmlWriter xw)
{
   boolean sts;

   if (newd) {
      try {
	 f = default_view.createNewFolder(f);
	 sts = true;
       }
      catch (IOException e) {
	 f = null;
	 sts = false;
       }
    }
   else if (dirs) sts = f.mkdirs();
   else sts = f.mkdirs();

   xw.begin("RESULT");
   xw.field("STATUS",sts);
   if (f != null) {
      xw.field("FILE",f.getAbsolutePath());
      xw.field("NAME",f.getName());
      xw.field("PARENT",f.getParent());
    }
   xw.end();
}


private static void handleListFiles(File f,IvyXmlWriter xw)
{
   File [] sub = f.listFiles();

   xw.begin("RESULT");
   if (sub == null) xw.field("COUNT",-1);
   else {
      xw.field("COUNT",sub.length);
      for (File s : sub) {
	 xw.begin("FILE");
	 xw.field("NAME",s.getAbsolutePath());
	 xw.end();
       }
    }
}



private static void handleDelete(File f,boolean onexit,IvyXmlWriter xw)
{
   boolean sts = true;

   if (onexit) f.deleteOnExit();
   else sts = f.delete();

   setResult(xw,sts);
}



private static void handleCanonical(File f,IvyXmlWriter xw)
{
   String s = null;
   boolean sts = true;

   try {
      s = f.getCanonicalPath();
    }
   catch (IOException e) {
      s = e.getMessage();
      sts = false;
    }

   xw.begin("RESULT");
   xw.field("STATUS",sts);
   xw.field("FILE",s);
   xw.end();
}




private static void handleRename(File f,String to,IvyXmlWriter xw)
{
   File tof = new File(to);
   boolean sts = f.renameTo(tof);
   setResult(xw,sts);
}



private static void setResult(IvyXmlWriter xw,boolean sts)
{
   xw.begin("RESULT");
   xw.field("STATUS",sts);
   xw.end();
}




}	// end of class BoardFileSystemView




/* end of BoardFileSystemView.java */

