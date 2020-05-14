/********************************************************************************/
/*										*/
/*		RebaseEditManager.java						*/
/*										*/
/*	REBUS rebase code for managing various file editors			*/
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



package edu.brown.cs.bubbles.rebase;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


class RebaseEditManager implements RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseMain			rebase_main;
private Map<RebaseFile,List<FileEditData>>   monitor_map;
private Map<RebaseFile,String>		owner_map;
private Map<RebaseFile,EditHandler>	handler_map;
private Map<String,EditParameters>	edit_parameters;
private Map<RebaseFile,FileEditor>	editor_map;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseEditManager(RebaseMain pm)
{
   rebase_main = pm;
   monitor_map = new HashMap<RebaseFile,List<FileEditData>>();
   owner_map = new HashMap<RebaseFile,String>();
   handler_map = new HashMap<RebaseFile,EditHandler>();
   editor_map = new HashMap<RebaseFile,FileEditor>();
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
	throws RebaseException
{
   RebaseFile fd = rebase_main.getFileByName(file);
   if (fd == null)
      throw new RebaseException("File " + file + " not found");

   addMonitor(fd,bid,id);

   String f = fd.getText();
   if (f.length() == 0) {
      xw.emptyElement("EMPTY");
    }
   else {
      xw.field("LINESEP",getLineSeparator(f));
      byte [] data = f.getBytes();
      xw.bytesElement("CONTENTS",data);
    }
}



/********************************************************************************/
/*										*/
/*	Elision commands							*/
/*										*/
/********************************************************************************/

void elisionSetup(String proj,String bid,String file,boolean compute,
      Collection<Element> rgns,IvyXmlWriter xw)
	throws RebaseException
{
   RebaseFile fd = rebase_main.getFileByName(file);
   if (fd == null) {
      RebaseMain.logE("File " + file + " not available for elision");
      return;
      // throw new RebaseException("File " + file + " not available for elision");
    }

   if (monitor_map.get(fd) == null) {
      throw new RebaseException("File " + file + " not open");
    }

   FileEditData fed = null;
   for (FileEditData fed1 : monitor_map.get(fd)) {
      if (fed1.getBaseId().equals(bid)) {
	 fed = fed1;
	 break;
       }
    }

   RebaseSemanticData isd = rebase_main.getSemanticData(fd);
   if (isd == null) throw new RebaseException("Unable to get AST for file " + file);

   RebaseElider be = null;
   if (fed != null) be = fed.getElider(isd);
   else be = isd.createElider();

   if (rgns != null) {
      be.clearElideData();
      for (Element r : rgns) {
	 double p = IvyXml.getAttrDouble(r,"PRIORITY",-1);
	 int soff = IvyXml.getAttrInt(r,"START");
	 int eoff = IvyXml.getAttrInt(r,"END");
	 if (soff < 0 || eoff < 0) throw new RebaseException("Missing start or end offset for elision region");
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
	throws RebaseException
{
   RebaseFile fd = rebase_main.getFileByName(file);
   if (fd == null) throw new RebaseException("File " + file + " not found");
   FileEditor fe = editor_map.get(fd);
   if (fe == null) throw new RebaseException("File " + file + " not opened");

   if (bid == null) bid = "*";

   boolean chng = fe.hasChanged();

   FileEditData fed = lockFile(fd,bid,id);
   try {
      IDocument d = fe.getDocument();
      for (IEditData ied : edits) {
	 int len = ied.getLength();
	 int off = ied.getOffset();
	 String txt = ied.getText();
	 d.replace(off,len,txt);
	 fe.markChanged();
	 if (fed.checkElider() != null) {
	    fed.checkElider().noteEdit(off,len,(txt == null ? 0 : txt.length()));
	  }
       }
    }
   catch (BadLocationException e) {
      RebaseMain.logE("Problem doing edit on " + file,e);
    }
   finally {
      unlockFile(fd);
    }

   if (!chng && fe.hasChanged()) {
      IvyXmlWriter mxw = rebase_main.beginMessage("FILECHANGE");
      mxw.field("FILE",file);
      rebase_main.finishMessage(mxw);
    }

   if (fed != null) {
      AutoCompile ac = new AutoCompile(fd,id,fed);
      rebase_main.startTask(ac);
    }
}


private FileEditData lockFile(RebaseFile fd,String bid,String eid)
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


private void unlockFile(RebaseFile fd)
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
      Collection<Element> files,IvyXmlWriter xw) throws RebaseException
{
   RebaseProject pp = rebase_main.getProjectManager().findProject(proj);

   xw.begin("COMMIT");
   if (files == null || files.size() == 0) {
      for (RebaseFile ifd : handler_map.keySet()) {
	 if (refresh || !save || hasChanged(ifd)) {
	    commitFile(pp,ifd,bid,refresh,save,xw);
	  }
       }
    }
   else {
      for (Element e : files) {
	 String fnm = IvyXml.getAttrString(e,"NAME");
	 if (fnm == null) fnm = IvyXml.getText(e);
	 fnm = RebaseMain.fixFileName(fnm);
	 RebaseFile ifd = rebase_main.getFileByName(fnm);
	 if (ifd != null) {
	    boolean r = IvyXml.getAttrBool(e,"REFRESH",refresh);
	    boolean s = IvyXml.getAttrBool(e,"SAVE",save);
	    commitFile(pp,ifd,bid,r,s,xw);
	  }
       }
    }

   xw.end("COMMIT");
}


private void commitFile(RebaseProject pp,RebaseFile ifd,String bid,boolean refresh,boolean save,IvyXmlWriter xw)
{
   FileEditData fed = lockFile(ifd,bid,null);
   boolean upd = false;
   try {
      xw.begin("FILE");
      xw.field("NAME",ifd.getFileName());
      try {
	 upd = commit(ifd,refresh,save);
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
      AutoCompile ac = new AutoCompile(ifd,null,fed);
      rebase_main.startTask(ac);
      IvyXmlWriter rxw = rebase_main.beginMessage("RESOURCE");
      rxw.begin("DELTA");
      rxw.field("KIND","CHANGED");
      rxw.begin("RESOURCE");
      rxw.field("TYPE","FILE");
      rxw.field("PROJECT",pp.getName());
      rxw.field("LOCATION",ifd.getFileName());
      rxw.end("RESOURCE");
      rxw.end("DELTA");
      rebase_main.finishMessage(rxw);
    }
}




/********************************************************************************/
/*										*/
/*	Handle file being deleted						*/
/*										*/
/********************************************************************************/

void removeFile(RebaseFile rf)
{
   monitor_map.remove(rf);
   owner_map.remove(rf);
   handler_map.remove(rf);
   editor_map.remove(rf);
}



/********************************************************************************/
/*										*/
/*	Text access methods							*/
/*										*/
/********************************************************************************/

String getContents(RebaseFile rf)
{
   if (rf == null) return null;

   FileEditor fe = editor_map.get(rf);
   if (fe == null) return rf.getText();
   IDocument doc = fe.getDocument();
   return doc.get();
}




/********************************************************************************/
/*										*/
/*	Monitoring methods							*/
/*										*/
/********************************************************************************/

private void addMonitor(RebaseFile fd,String bid,String id)
{
   synchronized (editor_map) {
      FileEditor fe = editor_map.get(fd);
      if (fe == null) {
	 fe = new FileEditor(fd);
	 editor_map.put(fd,fe);
       }
    }
   synchronized (monitor_map) {
      List<FileEditData> led = monitor_map.get(fd);
      if (led == null) {
	 led = new ArrayList<FileEditData>();
	 monitor_map.put(fd,led);
	 EditHandler eh = new EditHandler(fd);
	 handler_map.put(fd,eh);
	 getDocument(fd).addDocumentListener(eh);
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
private void removeMonitor(RebaseFile fd,String bid,String id)
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
	 if (eh != null) getDocument(fd).removeDocumentListener(eh);
	 monitor_map.remove(fd);
       }
    }
}



private class EditHandler implements IDocumentListener {

   private RebaseFile for_file;

   EditHandler(RebaseFile fd) {
      for_file = fd;
    }

   @Override public void documentAboutToBeChanged(DocumentEvent evt) { }
   @Override public void documentChanged(DocumentEvent evt) {
      int len = evt.getLength();
      IDocument doc = evt.getDocument();
      int off = evt.getOffset();
      String txt = evt.getText();
      RebaseMain.logD("Doc Edit " + len + " " + off + " " + (txt == null) + " " + doc.getLength());
      List<FileEditData> lfed = monitor_map.get(for_file);
      String owner = owner_map.get(for_file);
      for (FileEditData fed : lfed) {
	 if (fed.getBaseId().equals(owner)) continue;
	 IvyXmlWriter xw = rebase_main.beginMessage("EDIT",owner);
	 xw.field("FILE",for_file.getFileName());
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
	 rebase_main.finishMessage(xw);
	 RebaseMain.logD("SENDING EDIT " + xw.toString());
       }
    }

}	// end of inner class EditHandler




/********************************************************************************/
/*										*/
/*	Actual edit buffer for a file						*/
/*										*/
/********************************************************************************/

boolean hasChanged(RebaseFile rf)
{
   FileEditor fe = editor_map.get(rf);
   if (fe == null) return false;
   return fe.hasChanged();
}

boolean commit(RebaseFile rf,boolean refresh,boolean save)
{
   FileEditor fe = editor_map.get(rf);
   if (fe != null) return fe.commit(refresh,save);
   return false;
}


IDocument getDocument(RebaseFile rf)
{
   FileEditor fe = editor_map.get(rf);
   if (fe == null) return null;
   return fe.getDocument();
}




private class FileEditor {

   private RebaseFile for_file;
   private IDocument edit_document;
   private boolean has_changed;

   FileEditor(RebaseFile rf) {
      for_file = rf;
      has_changed = false;
      edit_document = new Document(rf.getText());
    }

   boolean commit(boolean refresh,boolean save) {
      if (!has_changed) return false;
      if (refresh) {
	 for_file.resetText();
	 reload();
       }
      else for_file.setText(edit_document.get());
      has_changed = false;
      return true;
    }

   IDocument getDocument()		{ return edit_document; }
   boolean hasChanged() 		{ return has_changed; }
   void markChanged()			{ has_changed = true; }

   private void reload() {
      edit_document.set(for_file.getText());
    }

}	// end of inner class FileEditor




/********************************************************************************/
/*										*/
/*	Class to hold edit information for a file/front end			*/
/*										*/
/********************************************************************************/

private class FileEditData {

   private String front_id;
   private String edit_id;
   private RebaseElider edit_elider;

   FileEditData(String bid,String id) {
      front_id = bid;
      edit_id = id;
      edit_elider = null;
    }

   String getBaseId()			{ return front_id; }
   String getEditId()			{ return edit_id; }

   void setEditId(String id)		{ edit_id = id; }

   void clearElider()			{ edit_elider = null; }
   RebaseElider checkElider()		{ return edit_elider; }
   synchronized RebaseElider getElider(RebaseSemanticData rsd) {
      if (edit_elider == null) {
	 edit_elider = rsd.createElider();
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

   private RebaseFile for_file;
   private String edit_id;
   private FileEditData edit_data;

   AutoCompile(RebaseFile ifd,String editid,FileEditData fed) {
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
      RebaseSemanticData isd = rebase_main.getSemanticData(for_file);
      if (isd == null) return;
      isd.reparse();
      List<RebaseMessage> msgs = isd.getMessages();
      if (!edit_data.getEditId().equals(edit_id)) return;
      IvyXmlWriter xw = rebase_main.beginMessage("EDITERROR");
      RebaseFile rf = isd.getFile();
      if (rf != null) {
	 RebaseProject proj = rebase_main.getProject(rf.getProjectId());
	 if (proj != null) {
	    xw.field("PROJECT",proj.getName());
	  }
       }
      xw.field("FILE",for_file.getFileName());
      xw.field("ID",edit_id);
      xw.begin("MESSAGES");
      if (msgs != null && msgs.size() > 0) {
	 for (RebaseMessage pm : msgs) {
	    pm.outputProblem(xw);
	  }
       }
      xw.end("MESSAGES");

      if (!edit_data.getEditId().equals(edit_id)) return;
      rebase_main.finishMessage(xw);

      if (ep.getAutoElide()) {
	 if (!edit_data.getEditId().equals(edit_id)) return;
	 RebaseElider pe = edit_data.checkElider();
	 if (pe != null) {
	    xw = rebase_main.beginMessage("ELISION",edit_data.getBaseId());
	    xw.field("FILE",for_file.getFileName());
	    xw.field("ID",edit_id);
	    xw.begin("ELISION");
	    if (pe.computeElision(isd,xw)) {
	       if (edit_data.getEditId().equals(edit_id)) {
		  xw.end("ELISION");
		  rebase_main.finishMessage(xw);
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





private String getLineSeparator(String txt)
{
   boolean havecr = false;
   for (int i = 0; i < txt.length(); ++i) {
      char c = txt.charAt(i);
      if (c == '\r') havecr = true;
      else if (c == '\n') {
	 if (havecr) return "CRLF";
	 else return "LF";
       }
      else if (havecr) {
	 return "CR";
       }
    }

   return "LF";
}



}	// end of class RebaseEditManager




/* end of RebaseEditManager.java */

