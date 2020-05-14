/********************************************************************************/
/*										*/
/*		PybaseEditor.java						*/
/*										*/
/*	Python Bubbles Base file and editor manager				*/
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


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



class PybaseEditor implements PybaseConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybaseMain			pybase_main;
private Map<IFileData,List<FileEditData>>   monitor_map;
private Map<IFileData,String>		owner_map;
private Map<IFileData,EditHandler>	handler_map;
private Map<String,EditParameters>	edit_parameters;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseEditor(PybaseMain pm)
{
   pybase_main = pm;
   monitor_map = new HashMap<IFileData,List<FileEditData>>();
   owner_map = new HashMap<IFileData,String>();
   handler_map = new HashMap<IFileData,EditHandler>();
   edit_parameters = new HashMap<String,EditParameters>();
}


/********************************************************************************/
/*										*/
/*	EDIT PARAM command							*/
/*										*/
/********************************************************************************/

void handleParameter(String bid,String name,String value)
{
   EditParameters ep = getParameters(bid);
   ep.setParameter(name,value);
}



/********************************************************************************/
/*										*/
/*	STARTFILE command							*/
/*										*/
/********************************************************************************/

void handleStartFile(String proj,String bid,String file,String id,boolean cnts,IvyXmlWriter xw)
	throws PybaseException
{
   File f = new File(file);
   IFileData fd = PybaseFileManager.getFileManager().getFileData(f);
   if (fd == null) throw new PybaseException("File " + file + " not found");

   addMonitor(fd,bid,id);
}



/********************************************************************************/
/*										*/
/*	Elision commands							*/
/*										*/
/********************************************************************************/

void elisionSetup(String proj,String bid,String file,boolean compute,
      Collection<Element> rgns,IvyXmlWriter xw)
	throws PybaseException
{
   File f = new File(file);
   PybaseProject pp = pybase_main.getProjectManager().findProject(proj);
   IFileData fd = PybaseFileManager.getFileManager().getFileData(f);
   if (fd == null) {
      throw new PybaseException("File " + file + " not available for elision");
    }

   if (monitor_map.get(fd) == null) {
      throw new PybaseException("File " + file + " not open");
    }

   FileEditData fed = null;
   for (FileEditData fed1 : monitor_map.get(fd)) {
      if (fed1.getBaseId().equals(bid)) {
	 fed = fed1;
	 break;
       }
    }

   ISemanticData isd = pp.getParseData(fd);
   if (isd == null) throw new PybaseException("Unable to get AST for file " + file);

   PybaseElider be = null;
   if (fed != null) be = fed.getElider();
   else be = new PybaseElider();

   if (rgns != null) {
      be.clearElideData();
      for (Element r : rgns) {
	 double p = IvyXml.getAttrDouble(r,"PRIORITY",-1);
	 int soff = IvyXml.getAttrInt(r,"START");
	 int eoff = IvyXml.getAttrInt(r,"END");
	 if (soff < 0 || eoff < 0) throw new PybaseException("Missing start or end offset for elision region");
	 if (p >= 0) be.addElidePriority(soff,eoff,p);
	 else be.addElideRegion(soff,eoff);
       }
    }
   else if (compute) {
      if (fed != null) fed.checkElider();
    }
   else if (fed != null) fed.clearElider();

   if (compute) {
      xw.begin("ELISION");
      if (be != null) be.computeElision(isd,xw);
      xw.end("ELISION");
    }
   else xw.emptyElement("SUCCESS");
}



/********************************************************************************/
/*										*/
/*	EDIT FILE command							 */
/*										*/
/********************************************************************************/

void handleEdit(String proj,String bid,String file,String id,List<IEditData> edits,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseProject pp = pybase_main.getProjectManager().findProject(proj);
   IFileData fd = PybaseFileManager.getFileManager().getFileData(file);
   if (fd == null) throw new PybaseException("File " + file + " not found");

   if (bid == null) bid = "*";

   boolean chngd = fd.hasChanged();

   FileEditData fed = lockFile(fd,bid,id);
   try {
      IDocument d = fd.getDocument();
      for (IEditData ied : edits) {
	 int len = ied.getLength();
	 int off = ied.getOffset();
	 String txt = ied.getText();
	 d.replace(off,len,txt);
	 fd.markChanged();
       }
    }
   catch (BadLocationException e) {
      PybaseMain.logE("Problem doing edit on " + file,e);
    }
   finally {
      unlockFile(fd);
    }

   if (!chngd && fd.hasChanged()) {
      IvyXmlWriter mxw = pybase_main.beginMessage("FILECHANGE");
      mxw.field("FILE",file);
      pybase_main.finishMessage(mxw);
    }

   if (fed != null) {
      AutoCompile ac = new AutoCompile(pp,fd,id,fed);
      PybaseMain.getPybaseMain().startTask(ac);
    }
}


private FileEditData lockFile(IFileData fd,String bid,String eid)
{
   synchronized (owner_map) {
      for ( ; ; ) {
	 String o = owner_map.get(fd);
	 if (o == null) break;
	 try {
	    owner_map.wait();
	  }
	 catch (InterruptedException e) { }
       }
      owner_map.put(fd,bid);
    }

   FileEditData fed = null;
   for (FileEditData fed1 : monitor_map.get(fd)) {
      if (fed1.getBaseId().equals(bid)) {
	 fed = fed1;
	 break;
       }
    }
   if (fed != null && eid != null) fed.setEditId(eid);

   return fed;
}


private void unlockFile(IFileData fd)
{
   synchronized (owner_map) {
      owner_map.remove(fd);
      owner_map.notifyAll();
    }
}



/********************************************************************************/
/*										*/
/*	COMMIT methods								*/
/*										*/
/********************************************************************************/

void handleCommit(String proj,String bid,boolean refresh,boolean save,
      Collection<Element> files,IvyXmlWriter xw) throws PybaseException
{
   PybaseProject pp = pybase_main.getProjectManager().findProject(proj);

   xw.begin("COMMIT");
   if (files == null || files.size() == 0) {
      for (IFileData ifd : handler_map.keySet()) {
	 if (refresh || !save || ifd.hasChanged()) {
	    commitFile(pp,ifd,bid,refresh,save,xw);
	  }
       }
    }
   else {
      for (Element e : files) {
	 String fnm = IvyXml.getAttrString(e,"NAME");
	 if (fnm == null) fnm = IvyXml.getText(e);
	 IFileData ifd	= PybaseFileManager.getFileManager().getFileData(fnm);
	 if (ifd != null) {
	    boolean r = IvyXml.getAttrBool(e,"REFRESH",refresh);
	    boolean s = IvyXml.getAttrBool(e,"SAVE",save);
	    commitFile(pp,ifd,bid,r,s,xw);
	  }
       }
    }

   xw.end("COMMIT");
}


private void commitFile(PybaseProject pp,IFileData ifd,String bid,boolean refresh,boolean save,IvyXmlWriter xw)
{
   FileEditData fed = lockFile(ifd,bid,null);
   boolean upd = false;
   try {
      xw.begin("FILE");
      xw.field("NAME",ifd.getFile().getPath());
      try {
	 upd = ifd.commit(refresh,save);
       }
      catch (Throwable t) {
	 xw.field("ERROR",t.toString());
       }
      xw.end("FILE");
    }
   finally {
      unlockFile(ifd);
    }

   if (upd && fed != null) {
      AutoCompile ac = new AutoCompile(pp,ifd,null,fed);
      PybaseMain pm = PybaseMain.getPybaseMain();
      pm.startTask(ac);
      IvyXmlWriter rxw = pm.beginMessage("RESOURCE");
      rxw.begin("DELTA");
      rxw.field("KIND","CHANGED");
      rxw.begin("RESOURCE");
      rxw.field("TYPE","FILE");
      rxw.field("PROJECT",pp.getName());
      rxw.field("LOCATION",ifd.getFile().getAbsolutePath());
      rxw.end("RESOURCE");
      rxw.end("DELTA");
      pm.finishMessage(rxw);
    }
}



/********************************************************************************/
/*										*/
/*	Monitoring methods							*/
/*										*/
/********************************************************************************/

private void addMonitor(IFileData fd,String bid,String id)
{
   synchronized (monitor_map) {
      List<FileEditData> led = monitor_map.get(fd);
      if (led == null) {
	 led = new ArrayList<FileEditData>();
	 monitor_map.put(fd,led);
	 EditHandler eh = new EditHandler(fd);
	 handler_map.put(fd,eh);
	 fd.getDocument().addDocumentListener(eh);
       }
      for (FileEditData ed : led) {
	 if (ed.getBaseId().equals(bid)) {
	    ed.setEditId(id);
	    return;
	  }
       }
      led.add(new FileEditData(bid,id));
    }
}



@SuppressWarnings("unused")
private void removeMonitor(IFileData fd,String bid,String id)
{
   synchronized (monitor_map) {
      List<FileEditData> led = monitor_map.get(fd);
      if (led == null) return;
      for (Iterator<FileEditData> it = led.iterator(); it.hasNext(); ) {
	 FileEditData ed = it.next();
	 if (ed.getBaseId().equals(bid)) {
	    it.remove();
	  }
       }
      if (led.size() == 0) {
	 EditHandler eh = handler_map.get(fd);
	 if (eh != null) fd.getDocument().removeDocumentListener(eh);
	 monitor_map.remove(fd);
       }
    }
}



private class EditHandler implements IDocumentListener {

   private IFileData for_file;

   EditHandler(IFileData fd) {
      for_file = fd;
    }

   @Override public void documentAboutToBeChanged(DocumentEvent evt) { }
   @Override public void documentChanged(DocumentEvent evt) {
      int len = evt.getLength();
      IDocument doc = evt.getDocument();
      int off = evt.getOffset();
      String txt = evt.getText();
      PybaseMain.logD("Doc Edit " + len + " " + off + " " + (txt == null) + " " + doc.getLength());
      List<FileEditData> lfed = monitor_map.get(for_file);
      String owner = owner_map.get(for_file);
      for (FileEditData fed : lfed) {
	 if (fed.getBaseId().equals(owner)) continue;
	 IvyXmlWriter xw = pybase_main.beginMessage("EDIT",owner);
	 xw.field("FILE",for_file.getFile().getPath());
	 xw.field("LENGTH",len);
	 xw.field("OFFSET",off);
	 if (len == doc.getLength() && txt != null && len > 0) {
	    xw.field("COMPLETE",true);
	    byte [] data = txt.getBytes();
	    xw.bytesElement("CONTENTS",data);
	  }
	 else if (txt != null) {
	    xw.cdata(txt);
	  }
	 pybase_main.finishMessage(xw);
	 PybaseMain.logD("SENDING EDIT " + xw.toString());
       }
    }

}	// end of inner class EditHandler

/********************************************************************************/
/*										*/
/*	Class to hold edit information for a file/front end			*/
/*										*/
/********************************************************************************/

private class FileEditData {

   private String front_id;
   private String edit_id;
   private PybaseElider edit_elider;

   FileEditData(String bid,String id) {
      front_id = bid;
      edit_id = id;
      edit_elider = null;
    }

   String getBaseId()			{ return front_id; }
   String getEditId()			{ return edit_id; }

   void setEditId(String id)		{ edit_id = id; }

   void clearElider()			{ edit_elider = null; }
   PybaseElider checkElider()		{ return edit_elider; }
   synchronized PybaseElider getElider() {
      if (edit_elider == null) {
	 edit_elider = new PybaseElider();
       }
      return edit_elider;
    }

}	// end of inner class EditData



/********************************************************************************/
/*										*/
/*	Handle automatic compilations						*/
/*										*/
/********************************************************************************/

private class AutoCompile implements Runnable {

   private PybaseProject for_project;
   private IFileData for_file;
   private String edit_id;
   private FileEditData edit_data;

   AutoCompile(PybaseProject pp,IFileData ifd,String editid,FileEditData fed) {
      for_project = pp;
      for_file = ifd;
      if (editid == null) editid = edit_data.getEditId();
      edit_id = editid;
      edit_data = fed;
    }

   @Override public void run() {
      EditParameters ep = getParameters(edit_data.getBaseId());
      if (!edit_data.getEditId().equals(edit_id)) return;
      int delay = ep.getDelayTime();
      if (delay < 0) return;
      if (delay > 0) {
	 try {
	    Thread.sleep(delay);
	  }
	 catch (InterruptedException e) { }
       }
      if (!edit_data.getEditId().equals(edit_id)) return;
      ISemanticData isd = for_project.reparseFile(for_file);
      if (isd == null) return;
      List<PybaseMessage> msgs = isd.getMessages();
      if (!edit_data.getEditId().equals(edit_id)) return;
      IvyXmlWriter xw = PybaseMain.getPybaseMain().beginMessage("EDITERROR");
      if (isd.getProject() != null) {
	 xw.field("PROJECT",isd.getProject().getName());
      }
      xw.field("FILE",for_file.getFile().getPath());
      xw.field("ID",edit_id);
      xw.begin("MESSAGES");
      if (msgs != null && msgs.size() > 0) {
	 for (PybaseMessage pm : msgs) {
	    PybaseUtil.outputProblem(pm,isd,xw);
	  }
       }
      xw.end("MESSAGES");

      if (!edit_data.getEditId().equals(edit_id)) return;
      PybaseMain.getPybaseMain().finishMessage(xw);
      if (ep.getAutoElide()) {
	 if (!edit_data.getEditId().equals(edit_id)) return;
	 PybaseElider pe = edit_data.checkElider();
	 if (pe != null) {
	     xw = PybaseMain.getPybaseMain().beginMessage("ELISION",edit_data.getBaseId());
	     xw.field("FILE",for_file.getFile().getPath());
	     xw.field("ID",edit_id);
	     xw.begin("ELISION");
	     if (pe.computeElision(isd,xw)) {
		if (edit_data.getEditId().equals(edit_id)) {
		   xw.end("ELISION");
		   PybaseMain.getPybaseMain().finishMessage(xw);
		 }
	      }
	  }
       }
    }

}	// end of inner class AutoCompile




/********************************************************************************/
/*										*/
/*	Parameter holder							*/
/*										*/
/********************************************************************************/

private EditParameters getParameters(String id)
{
   synchronized (edit_parameters) {
      EditParameters ep = edit_parameters.get(id);
      if (ep == null) {
	 ep = new EditParameters();
	 edit_parameters.put(id,ep);
       }
      return ep;
    }
}



private static class EditParameters {

   private int delay_time;
   private boolean auto_elide;

   EditParameters() {
      delay_time = 250;
      auto_elide = false;
    }

   int getDelayTime()		{ return delay_time; }
   boolean getAutoElide()	{ return auto_elide; }

   void setParameter(String name,String value) {
      if (name.equals("AUTOELIDE")) {
	 auto_elide = Boolean.parseBoolean(value);
       }
      else if (name.equals("ELIDEDELAY")) {
	 delay_time = Integer.parseInt(value);
       }
    }

}	// end of inner class EditParamters




}	// end of class PybaseEditor




/* end of PybaseEditor.java */
