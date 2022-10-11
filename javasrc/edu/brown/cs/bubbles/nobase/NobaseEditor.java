/********************************************************************************/
/*										*/
/*		NobaseEditor.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


class NobaseEditor implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseMain		nobase_main;
private Map<NobaseFile,List<FileEditData>>   monitor_map;
private Map<NobaseFile,String>		owner_map;
private Map<NobaseFile,EditHandler>	handler_map;
private Map<String,EditParameters>	edit_parameters;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseEditor(NobaseMain pm)
{
   nobase_main = pm;
   monitor_map = new HashMap<NobaseFile,List<FileEditData>>();
   owner_map = new HashMap<NobaseFile,String>();
   handler_map = new HashMap<NobaseFile,EditHandler>();
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
throws NobaseException
{
   File f = new File(file);
   NobaseFile fd = nobase_main.getFileManager().getFileData(f);
   if (fd == null) throw new NobaseException("File " + file + " not found");

   addMonitor(fd,bid,id);
}



/********************************************************************************/
/*										*/
/*	Elision commands							*/
/*										*/
/********************************************************************************/

void elisionSetup(String proj,String bid,String file,boolean compute,
      Collection<Element> rgns,IvyXmlWriter xw) throws NobaseException
{
   File f = new File(file);
   NobaseProject pp = nobase_main.getProjectManager().findProject(proj);
   NobaseFile fd = nobase_main.getFileManager().getFileData(f);
   if (fd == null) {
      throw new NobaseException("File " + file + " not available for elision");
    }
   if (monitor_map.get(fd) == null) {
      throw new NobaseException("File " + file + " not open");
    }

   FileEditData fed = null;
   for (FileEditData fed1 : monitor_map.get(fd)) {
      if (fed1.getBaseId().equals(bid)) {
	 fed = fed1;
	 break;
       }
    }

   if (pp == null) pp = fd.getProject();
   if (pp == null) throw new NobaseException("Project not found for " + file);

   ISemanticData isd = pp.getParseData(fd);
   if (isd == null) throw new NobaseException("Unable to get AST for file " + file);

   NobaseElider be = null;
   if (fed != null) be = fed.getElider();
   else be = new NobaseElider();

   if (rgns != null) {
      be.clearElideData();
      for (Element r : rgns) {
	 double p = IvyXml.getAttrDouble(r,"PRIORITY",-1);
	 int soff = IvyXml.getAttrInt(r,"START");
	 int eoff = IvyXml.getAttrInt(r,"END");
	 if (soff < 0 || eoff < 0) throw new NobaseException("Missing start or end offset for elision region");
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
	throws NobaseException
{
   NobaseProject pp = nobase_main.getProjectManager().findProject(proj);
   NobaseFile fd = nobase_main.getFileManager().getFileData(file);
   if (fd == null) throw new NobaseException("File " + file + " not found");

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
      NobaseMain.logE("Problem doing edit on " + file,e);
    }
   finally {
      unlockFile(fd);
    }

   if (!chngd && fd.hasChanged()) {
      IvyXmlWriter mxw = nobase_main.beginMessage("FILECHANGE");
      mxw.field("FILE",file);
      nobase_main.finishMessage(mxw);
    }

   if (fed != null) {
      AutoCompile ac = new AutoCompile(pp,fd,id,fed);
      NobaseMain.getNobaseMain().startTask(ac);
    }
}


private FileEditData lockFile(NobaseFile fd,String bid,String eid)
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


private void unlockFile(NobaseFile fd)
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
      Collection<Element> files,IvyXmlWriter xw) throws NobaseException
{
   NobaseProject pp = nobase_main.getProjectManager().findProject(proj);

   xw.begin("COMMIT");
   if (files == null || files.size() == 0) {
      for (NobaseFile ifd : handler_map.keySet()) {
	 if (refresh || !save || ifd.hasChanged()) {
	    commitFile(pp,ifd,bid,refresh,save,xw);
	  }
       }
    }
   else {
      for (Element e : files) {
	 String fnm = IvyXml.getAttrString(e,"NAME");
	 if (fnm == null) fnm = IvyXml.getText(e);
	 NobaseFile ifd = nobase_main.getFileManager().getFileData(fnm);
	 if (ifd != null) {
	    boolean r = IvyXml.getAttrBool(e,"REFRESH",refresh);
	    boolean s = IvyXml.getAttrBool(e,"SAVE",save);
	    commitFile(pp,ifd,bid,r,s,xw);
	  }
       }
    }

   xw.end("COMMIT");
}


private void commitFile(NobaseProject pp,NobaseFile ifd,String bid,boolean refresh,boolean save,IvyXmlWriter xw)
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

   if (upd && fed != null && pp != null) {
      AutoCompile ac = new AutoCompile(pp,ifd,null,fed);
      NobaseMain pm = NobaseMain.getNobaseMain();
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
/*	RENAME command							       */
/*										*/
/********************************************************************************/

void handleRename(String proj,String bid,String file,int start,int end,String name,String handle,
      String newname,
      boolean refs,
      boolean doedit,
      String filespat,IvyXmlWriter xw)
	throws NobaseException
{
   NobaseProject pp = nobase_main.getProjectManager().findProject(proj);
   NobaseFile fd = nobase_main.getFileManager().getFileData(file);
   if (fd == null) throw new NobaseException("File " + file + " not found");
   if (pp == null) pp = fd.getProject();
   if (pp == null) {
      throw new NobaseException("No project found for " + proj + " and " + file);
    }
   ISemanticData isd = pp.getParseData(fd);
   if (isd == null || isd.getRootNode() == null) throw new NobaseException("Unable to get AST for file " + file);
   NobaseSearchInstance search = new NobaseSearchInstance(pp);
   ASTVisitor av = search.getFindLocationVisitor(start,end);
   isd.getRootNode().accept(av);
   ASTVisitor av1 = search.getLocationsVisitor(true,true,true,false,false);
   for (NobaseFile ifd : pp.getAllFiles()) {
      search.setFile(ifd);
      ISemanticData isd1 = pp.getParseData(ifd);
      if (isd1 != null && isd1.getRootNode() != null) isd1.getRootNode().accept(av1);
    }
   for (SearchResult mtch : search.getMatches()) {
      NobaseMain.logD("Create edit " + mtch.getFile().getFile().getPath() + " " +
	    mtch.getOffset() + " " + mtch.getLength() + " " + name + " " + newname);
    }
}


/********************************************************************************/
/*										*/
/*	Monitoring methods							*/
/*										*/
/********************************************************************************/

private void addMonitor(NobaseFile fd,String bid,String id)
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
private void removeMonitor(NobaseFile fd,String bid,String id)
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

   private NobaseFile for_file;

   EditHandler(NobaseFile fd) {
      for_file = fd;
    }

   @Override public void documentAboutToBeChanged(DocumentEvent evt) { }
   @Override public void documentChanged(DocumentEvent evt) {
      int len = evt.getLength();
      IDocument doc = evt.getDocument();
      int off = evt.getOffset();
      String txt = evt.getText();
      NobaseMain.logD("Doc Edit " + len + " " + off + " " + (txt == null) + " " + doc.getLength());
      List<FileEditData> lfed = monitor_map.get(for_file);
      String owner = owner_map.get(for_file);
      for (FileEditData fed : lfed) {
	 if (fed.getBaseId().equals(owner)) continue;
	 IvyXmlWriter xw = nobase_main.beginMessage("EDIT",owner);
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
	 nobase_main.finishMessage(xw);
	 NobaseMain.logD("SENDING EDIT " + xw.toString());
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
   private NobaseElider edit_elider;

   FileEditData(String bid,String id) {
      front_id = bid;
      edit_id = id;
      edit_elider = null;
    }

   String getBaseId()			{ return front_id; }
   String getEditId()			{ return edit_id; }

   void setEditId(String id)		{ edit_id = id; }

   void clearElider()			{ edit_elider = null; }
   NobaseElider checkElider()		{ return edit_elider; }
   synchronized NobaseElider getElider() {
      if (edit_elider == null) {
	 edit_elider = new NobaseElider();
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

   private NobaseProject for_project;
   private NobaseFile for_file;
   private String edit_id;
   private FileEditData edit_data;

   AutoCompile(NobaseProject pp,NobaseFile ifd,String editid,FileEditData fed) {
      for_project = pp;
      for_file = ifd;
      if (editid == null) editid = fed.getEditId();
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
      List<NobaseMessage> msgs = isd.getMessages();
      if (!edit_data.getEditId().equals(edit_id)) return;
      IvyXmlWriter xw = NobaseMain.getNobaseMain().beginMessage("EDITERROR");
      if (isd.getProject() != null) {
	 xw.field("PROJECT",isd.getProject().getName());
       }
      xw.field("FILE",for_file.getFile().getPath());
      xw.field("ID",edit_id);
      xw.begin("MESSAGES");
      if (msgs != null && msgs.size() > 0) {
	 for (NobaseMessage pm : msgs) {
	    NobaseUtil.outputProblem(pm,isd,xw);
	  }
       }
      xw.end("MESSAGES");

      if (!edit_data.getEditId().equals(edit_id)) return;
      NobaseMain.getNobaseMain().finishMessage(xw);
      if (ep.getAutoElide()) {
	 if (!edit_data.getEditId().equals(edit_id)) return;
	 NobaseElider pe = edit_data.checkElider();
	 if (pe != null) {
	    xw = NobaseMain.getNobaseMain().beginMessage("ELISION",edit_data.getBaseId());
	    xw.field("FILE",for_file.getFile().getPath());
	    xw.field("ID",edit_id);
	    xw.begin("ELISION");
	    if (pe.computeElision(isd,xw)) {
	       if (edit_data.getEditId().equals(edit_id)) {
		  xw.end("ELISION");
		  NobaseMain.getNobaseMain().finishMessage(xw);
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




}	// end of class NobaseEditor




/* end of NobaseEditor.java */

