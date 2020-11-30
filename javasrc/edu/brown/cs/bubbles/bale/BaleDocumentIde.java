/********************************************************************************/
/*										*/
/*		BaleDocumentIde.java						*/
/*										*/
/*	Bubble Annotated Language Editor IDE-based document			*/
/*										*/
/*	This represents a document without an editor.  It supports a set of	*/
/*	fragment documents that are informed of any edit to this document.	*/
/*	The fragment documents should handle edits by calling the appropriate	*/
/*	editing method on this document.					*/
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


package edu.brown.cs.bubbles.bale;


import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpException;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Position;
import javax.swing.text.Segment;

// import javax.swing.undo.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;



class BaleDocumentIde extends BaleDocumentBase implements BaleConstants,
	BumpConstants.BumpFileHandler, BumpConstants.BumpProblemHandler,
	BaleConstants.BaleFileOverview, BumpConstants.BumpChangeHandler
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private File		file_name;
private BoardLanguage	file_language;
private boolean 	doing_load;
private boolean 	doing_remote;
private boolean 	doing_eload;
private boolean 	is_dirty;
private int		checkpoint_counter;
private boolean 	is_readonly;
private Element 	elision_data;	// for readonly files
private int		num_import;
private BaleFragmentEditor dummy_editor;

private Map<BaleFragment,FragmentData> fragment_map;

private String			newline_string;
private int			newline_adjust;
private BaleLineOffsetsNew	line_offsets;

private Set<BumpProblem>	problem_set;

private List<BaleAstNode>	ast_nodes;
private Queue<RemoteEdit>	remote_edits;

private static final long serialVersionUID = 1;


private static BumpClient      bump_client = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleDocumentIde()
{
   synchronized (BaleDocumentIde.class) {
      if (bump_client == null) {
	 bump_client = BumpClient.getBump();
	 bump_client.waitForIDE();
	 bump_client.setEditParameter("AUTOELIDE","TRUE");
	 int edelay = BALE_PROPERTIES.getInt(BALE_TYPEIN_DELAY,250);
	 bump_client.setEditParameter("ELIDEDELAY",Integer.toString(edelay));
       }
    }

   project_name = null;
   file_name = null;
   fragment_map = new HashMap<>();
   doing_load = false;
   doing_remote = false;
   doing_eload = false;
   ast_nodes = null;
   problem_set = new HashSet<>();
   is_dirty = false;
   checkpoint_counter = -1;
   num_import = -1;
   dummy_editor = null;
   // This should not be needed as any change should trigger a remote edit
   BumpClient.getBump().addChangeHandler(this);
   remote_edits = new LinkedList<>();

   newline_string = null;
   newline_adjust = 0;
   line_offsets = null;
   addDocumentListener(new LineOffsetListener());
   is_readonly = false;
   file_language = null;
}



BaleDocumentIde(String proj,File file)
{
   this();
   setFile(proj,file);
}



BaleDocumentIde(String proj,File file,boolean local)
{
   this();
   if (local) {
      setLocalFile(proj,file);
      is_readonly = true;
      elision_data = bump_client.getElisionForFile(file_name);
   }
   else setFile(proj,file);
}



@Override void dispose()
{
   BumpClient.getBump().removeChangeHandler(this);
   
   if (dummy_editor != null) {
      dummy_editor.dispose();
      dummy_editor = null;
    }
   
   super.dispose();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProjectName()	{ return project_name; }

void checkProjectName(String name)
{
   if (name != null) {
      if (!name.equals(project_name)) {
	 project_name = name;
       }
    }
}

@Override public File getFile() 		{ return file_name; }
@Override public BoardLanguage getLanguage()
{
   if (file_language != null) return file_language;
   return super.getLanguage();
}

@Override boolean isEditable()		{ return !is_readonly; }
Element getReadonlyElisionData()	{ return elision_data; }

String getLineSeparator()		{ return newline_string; }

void setReadonly()
{
   if (is_readonly) return;

   is_readonly = true;

   if (elision_data == null) elision_data = bump_client.getElisionForFile(file_name);
}


private void setFile(String proj,File file)
{
   String cnts = null;
   String linesep = null;

   project_name = proj;
   file_name = file;
   setLanguage();

   // TODO: check out the file for write using current version manager

   try {
      Element xml = bump_client.startFile(project_name,file_name,false,nextEditCounter());
      if (xml == null) return;
      byte [] data = IvyXml.getBytesElement(xml,"CONTENTS");
      if (data != null) cnts = new String(data);
      linesep = IvyXml.getTextElement(xml,"LINESEP");
      if (project_name == null) project_name = IvyXml.getTextElement(xml,"PROJECT");
    }
   catch (BumpException e) {
      BoardLog.logE("BALE","Problem loading file " + file + ": " + e);
    }

   if (linesep != null) {
      if (linesep.equals("LF")) linesep = "\n";
      else if (linesep.equals("CRLF")) linesep = "\r\n";
      else if (linesep.equals("CR")) linesep = "\r";
    }

   DefaultEditorKit dek = new DefaultEditorKit();
   is_dirty = cnts != null;

   writeLock();
   try {
      doing_load = true;
      Reader in = null;
      if (cnts == null) in = new BufferedReader(new FileReader(file));
      else in = new StringReader(cnts);

      dek.read(in,this,0);

      if (cnts == null) in = new BufferedReader(new FileReader(file));
      else in = new StringReader(cnts);
      setupLineOffsets(in,linesep);
    }
   catch (FileNotFoundException e) {
      BoardLog.logI("BALE","File disappeared " + file);
    }
   catch (Exception e) {
      BoardLog.logE("BALE","Problem loading file " + file,e);
    }
   finally {
      doing_load = false;
      writeUnlock();
    }

   handleLoaded();

   addDocumentListener(new EclipseUpdater());
}



private void setLocalFile(String proj,File file)
{
   String linesep = null;

   project_name = proj;
   file_name = file;
   setLanguage();

   try (FileReader fr = new FileReader(file)) {
      for ( ; ; ) {
	 int ch = fr.read();
	 if (ch < 0) break;
	 if (ch == '\r') {
	    ch = fr.read();
	    if (ch == '\n') linesep = "\r\n";
	    else linesep = "\r";
	    break;
	  }
	 else if (ch == '\n') {
	    ch = fr.read();
	    if (ch == '\r') linesep = "\n\r";
	    else linesep = "\n";
	    break;
	  }
       }
    }
   catch (IOException e) { }

   DefaultEditorKit dek = new DefaultEditorKit();
   is_dirty = false;

   writeLock();
   try {
      doing_load = true;
      Reader in = null;
      in = new BufferedReader(new FileReader(file));

      dek.read(in,this,0);

      in = new BufferedReader(new FileReader(file));
      setupLineOffsets(in,linesep);
    }
   catch (Exception e) {
      BoardLog.logE("BALE","Problem loading local file " + file,e);
    }
   finally {
      doing_load = false;
      writeUnlock();
    }

   handleLoaded();
}



private void setLanguage()
{
   String f = file_name.getName();
   file_language = BoardLanguage.JAVA;
   if (f != null) {
      if (f.startsWith("/REBUS/")) file_language = BoardLanguage.REBUS;
      if (f.endsWith(".py") || f.endsWith(".PY")) file_language = BoardLanguage.PYTHON;
      if (f.endsWith(".java")) file_language = BoardLanguage.JAVA;
      if (f.endsWith(".js")) file_language = BoardLanguage.JS;
    }
}


@Override void revert()
{
   // TODO: Need to get eclipse and other instances of bubbles to do the same
   // Would it be sufficient to tell Eclipse to revert?

   bump_client.revertFile(project_name,file_name);

   // TODO: need to do something with history?
   // TODO: Need to detect eclipse changes
}



@Override void save()
{
   BowiFactory.startTask();
   bump_client.saveFile(project_name,file_name);

   is_dirty = false;

   BowiFactory.stopTask();
}



@Override void commit()
{
   BowiFactory.startTask();
   bump_client.commitFile(project_name,file_name);
   BowiFactory.stopTask();
}

@Override void compile()
{
   BowiFactory.startTask();
   bump_client.compileFile(project_name,file_name);
   BowiFactory.stopTask();
}





@Override void checkpoint()
{
   if (!canSave()) return;			// not dirty
   int id = getEditCounter();
   if (checkpoint_counter == id) return;	// no change since last checkpoint
   checkpoint_counter = id;

   File otf = null;
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      File dir = BoardSetup.getPropertyBase();
      File backup = new File(dir,BALE_CHECKPOINT_DIRECTORY);
      if (!backup.exists()) backup.mkdir();
      String nm = file_name.getPath();
      nm = nm.replace("/","_").replace("\\","_");
      otf = new File(backup,nm + BALE_CHECKPOINT_EXTENSION);
    }
   else {
      File dir = file_name.getParentFile();
      File backup = new File(dir,BALE_CHECKPOINT_DIRECTORY);
      if (!backup.exists()) backup.mkdir();
      otf = new File(backup,file_name.getName() + BALE_CHECKPOINT_EXTENSION);
    }

   Segment s = new Segment();
   baleReadLock();
   try {
      getText(0,getLength(),s);
      FileWriter fw = new FileWriter(otf);
      fw.write(s.toString());
      fw.close();
    }
   catch (BadLocationException e) { }
   catch (IOException e) { }
   finally { baleReadUnlock(); }
}



@Override boolean canSave() {
   if (is_dirty) return true;
   return false;
}



/********************************************************************************/
/*										*/
/*	Edit methods								*/
/*										*/
/********************************************************************************/

@Override public void insertString(int off,String st,AttributeSet a)
	throws BadLocationException
{
   writeLock();
   try {
      // fixNewLines(off,off,st);

      super.insertString(off,st,a);

      is_dirty = true;
    }
   finally { writeUnlock(); }
}



@Override public void remove(int off,int len) throws BadLocationException
{
   writeLock();
   try {
      // fixNewLines(off,off+len,null);

      super.remove(off,len);

      is_dirty = true;
    }
   finally { writeUnlock(); }
}



@Override public void markChanged()
{
   is_dirty = true;
}



/********************************************************************************/
/*										*/
/*	Fragment management methods						*/
/*										*/
/********************************************************************************/

void createFragment(BaleFragment owner,Collection<BaleRegion> regions)
{
   FragmentData fd = new FragmentData(regions);

   synchronized (fragment_map) {
      fragment_map.put(owner,fd);
      // fixupElision();
    }
}



void removeFragment(BaleFragment owner)
{
   baleReadLock();
   try {
      synchronized (fragment_map) {
	 fragment_map.remove(owner);
	 fixupElision();
       }
    }
   finally { baleReadUnlock(); }
}



void setFragmentElisionRegions(BaleFragment owner,Map<BaleRegion,Double> priors)
{
   synchronized (fragment_map) {
      FragmentData fd = fragment_map.get(owner);
      if (fd == null) return;

      fd.setPriorityRegions(priors);
      // fixupElision();
    }
}



@Override void redoElision()
{
   synchronized (fragment_map) {
      fixupElision();
    }
}



private void fixupElision()
{
   // TODO: Need to handle RO files correctly here
   if (is_readonly) {
      int id = getEditCounter();
      handleElisionData(file_name,id,elision_data);
      return;
   }

   String rgns = null;
   if (fragment_map.size() > 0) {
      IvyXmlWriter xw = new IvyXmlWriter();
      synchronized (fragment_map) {
	 for (FragmentData fd : fragment_map.values()) {
	    fd.dumpRegions(this,xw);
	  }
       }
      rgns = xw.toString();
      bump_client.removeFileHandler(this);
      bump_client.removeProblemHandler(this);
      bump_client.addFileHandler(file_name,this);
      bump_client.addProblemHandler(file_name,this);
    }
   else {
      bump_client.removeFileHandler(this);
      bump_client.removeProblemHandler(this);
    }

   int id = getEditCounter();
   Element ed = bump_client.setupElision(project_name,file_name,rgns,true);
   Element d = IvyXml.getChild(ed,"ELISION");
   if (d == null) return;

   handleElisionData(file_name,id,d);
}




/********************************************************************************/
/*										*/
/*	Callbacks for asynchonous notifications for formatting			*/
/*										*/
/********************************************************************************/

@Override public void handleElisionData(File f,int id,Element d)
{
   if (!f.equals(file_name)) return;

   if (id != getEditCounter()) return;

   // BoardLog.logD("BALE","ELISION ID = " + id + " " + getEditCounter() + " " + IvyXml.convertXmlToString(d));

   List<BaleAstNode> nodes = new ArrayList<BaleAstNode>();
   int impct = 0;
   for (Element e : IvyXml.children(d,"ELIDE")) {
      BaleAstNode n = new BaleAstNode(e,this);
      if (n.getNodeType() == BaleAstNodeType.FILE) impct = n.countImports();
      nodes.add(n);
    }
   if (impct == 0) num_import = -1;
   else if (num_import < 0) num_import = impct;
   else if (num_import == impct) ;
   else if (num_import == impct-1) {
      BoardMetrics.noteCommand("BALE","ADDIMPORT");
      num_import = impct;
    }
   else num_import = -1;

   if (id != getEditCounter()) return;
   ast_nodes = nodes;

   List<FragmentElisionUpdater> runs = new ArrayList<FragmentElisionUpdater>();

   synchronized (fragment_map) {
      for (Map.Entry<BaleFragment,FragmentData> ent : fragment_map.entrySet()) {
	 BaleFragment bf = ent.getKey();
	 FragmentData fd = ent.getValue();
	 List<BaleAstNode> rgnnodes = null;
	 for (BaleRegion br : fd.getRegions()) {
	    for (BaleAstNode bn : ast_nodes) {
	       int pos = br.getStart();
	       BaleAstNode cn = bn.getChild(pos);
	       if (cn == null) {
		  // if the text has errors, it might not extend to include comments
		  if (bn.getStart() >= br.getStart() && bn.getEnd() <= br.getEnd()) cn = bn;
		}
	       if (cn != null) {
		  if (rgnnodes == null) rgnnodes = new ArrayList<BaleAstNode>();
		  // should this add bn or cn: bn
		  rgnnodes.add(bn);
		}
	     }
	  }
	 if (rgnnodes != null) {
	    runs.add(new FragmentElisionUpdater(bf,rgnnodes,id));
	  }
       }
    }

   if (id != getEditCounter()) return;

   for (FragmentElisionUpdater feu : runs) {
      BoardThreadPool.start(feu);
    }
}



private class FragmentElisionUpdater implements Runnable {

   private BaleFragment for_fragment;
   private List<BaleAstNode> local_nodes;
   private int edit_id;

   FragmentElisionUpdater(BaleFragment bf,List<BaleAstNode> nodes,int id) {
      for_fragment = bf;
      local_nodes = nodes;
      edit_id = id;
    }

   @Override public void run() {
      if (edit_id != getEditCounter()) return;
      for_fragment.handleAstUpdated(local_nodes);
    }

}	// end of inner class FragmentElisionUpdater




/********************************************************************************/
/*										*/
/*	Handle remote edits							*/
/*										*/
/********************************************************************************/

@Override public void handleRemoteEdit(File f,int len,int off,String txt)
{
   if (!f.equals(file_name)) return;

   int loff = mapOffsetToJava(off);

   RemoteEdit re = new RemoteEdit(len,loff,txt);

   synchronized (remote_edits) {
      boolean newrq = remote_edits.isEmpty();
      remote_edits.add(re);
      if (newrq) SwingUtilities.invokeLater(new RemoteEditor());
    }
}


void flushEdits()
{
   synchronized (remote_edits) {
      if (remote_edits.isEmpty()) return;
      RemoteEditor re = new RemoteEditor();
      re.run();
    }
}




private class RemoteEditor implements Runnable {

   RemoteEditor() { }

   @Override public void run() {
      synchronized (remote_edits) {
	 while (!remote_edits.isEmpty()) {
	    RemoteEdit re = remote_edits.remove();
	    re.run();
	  }
       }
      fixupElision();
    }

}	// end of class RemoteEditor




private class RemoteEdit implements Runnable {

   private int edit_len;
   private int edit_offset;
   private String edit_text;

   RemoteEdit(int len,int off,String txt) {
      edit_len = len;
      edit_offset = off;
      edit_text = txt;
    }

   @Override public void run() {
      BoardLog.logD("BALE","Remote edit update " + edit_offset + " " + edit_len);
      if (edit_offset == 0 && edit_len < 0)
	 reload();
      else
	 doEdit();
      BoardLog.logD("BALE","Done remote edit");
    }

   private void doEdit() {
      writeLock();
      try {
	 doing_remote = true;	     // prevent edit from going back to eclipse

	 try {
	    if (edit_len != 0) {
	       if (edit_offset+edit_len >= getLength()) {
		  edit_len = getLength() - edit_offset;
		}
	       remove(edit_offset,edit_len);
	     }
	    if (edit_text != null && edit_text.length() > 0) {
	       insertString(edit_offset,edit_text,null);
	     }
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Bad location for remote edit: " + e);
	    return;
	  }
       }
      finally {
	 doing_remote = false;
	 writeUnlock();
       }
    }

   private void reload() {
      writeLock();
      try {
	 try {
	    String txt = getText(0,getLength());
	    if (txt.equals(edit_text)) {
	       BoardLog.logD("BALE","Reload with equal text ignored");
	       return;
	     }
	    else {
	       BoardLog.logD("BALE","Full reload being done");
	       BoardMetrics.noteCommand("BALE","FullReload");
	     }
	  }
	 catch (BadLocationException e) { }

	 doing_remote = true;
	 doing_load = true;

	 List<BaleReloadData> reloads = new ArrayList<BaleReloadData>();
	 synchronized (fragment_map) {
	    for (BaleFragment bf : fragment_map.keySet()) {
	       BaleReloadData rd = bf.startReload();
	       if (rd != null) reloads.add(rd);
	     }
	  }

	 BaleSavedPositions posn = null;
	 try {
	    posn = savePositions();
	  }
	 catch (IOException e) {
	    BoardLog.logE("BALE","Problem saving positions: " + e);
	  }

	 DocumentListener [] dlisteners = getDocumentListeners();
	 UndoableEditListener [] elisteners = getUndoableEditListeners();
	 for (int i = 0; i < dlisteners.length; ++i) {
	    removeDocumentListener(dlisteners[i]);
	  }
	 for (int i = 0; i < elisteners.length; ++i) {
	    removeUndoableEditListener(elisteners[i]);
	  }

	 try {
	    remove(0,getLength());
	    String ntxt = getText(0,getLength());
	    if (ntxt != null && ntxt.length() > 0)
	       BoardLog.logD("BALE","Didn't remove all text: " + ntxt);
	    DefaultEditorKit dek = new DefaultEditorKit();
	    Reader in = new StringReader(edit_text);
	    try {
	       dek.read(in,BaleDocumentIde.this,0);
	     }
	    catch (IOException e) {
	       BoardLog.logE("BALE","I/O exception from string reader",e);
	     }
	    ntxt = getText(0,getLength());		// for debugging
	    in = new StringReader(edit_text);
	    setupLineOffsets(in,newline_string);
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Bad location for reload: " + e,e);
	  }

	 nextEditCounter();

	 if (posn != null) resetPositions(posn);

	 for (int i = 0; i < elisteners.length; ++i) {
	    addUndoableEditListener(elisteners[i]);
	  }
	 for (int i = 0; i < dlisteners.length; ++i) {
	    addDocumentListener(dlisteners[i]);
	  }

	 doing_load = false;
	 doing_remote = false;

	 for (BaleReloadData rd : reloads) {
	    rd.finishedReload();
	  }
       }
      finally {
	 doing_load = false;
	 doing_remote = false;
	 writeUnlock();
       }

      fixupElision();
    }

}	// end of inner class RemoteEdit







/********************************************************************************/
/*										*/
/*	Handle asynchronous notifications for errors				*/
/*										*/
/********************************************************************************/

@Override public void handleProblemAdded(BumpProblem bp)
{
   // BoardLog.logD("BALE","ADD PROBLEM " + bp.getProblemId());

   synchronized (problem_set) {
      problem_set.add(bp);
    }

   int soff = mapOffsetToJava(bp.getStart());
   int eoff = mapOffsetToJava(bp.getEnd());

   synchronized (fragment_map) {
      for (FragmentData fd : fragment_map.values()) {
	 if (fd.overlaps(soff,eoff)) {
	    fd.setProblemsChanged(true);
	  }
       }
    }
}



@Override public void handleProblemRemoved(BumpProblem bp)
{
   // BoardLog.logD("BALE","REMOVE PROBLEM " + bp.getProblemId());

   synchronized (problem_set) {
      problem_set.remove(bp);
    }

   int soff = mapOffsetToJava(bp.getStart());
   int eoff = mapOffsetToJava(bp.getEnd());

   synchronized (fragment_map) {
      for (FragmentData fd : fragment_map.values()) {
	 if (fd.overlaps(soff,eoff)) {
	    fd.setProblemsChanged(true);
	  }
       }
    }
}


@Override public void handleClearProblems()
{
   List<BumpProblem> probl;
   synchronized (problem_set) {
      probl = new ArrayList<>(problem_set);
    }
   for (BumpProblem bp : probl) {
      handleProblemRemoved(bp);
    }
}




@Override public void handleProblemsDone()
{
   handleProblemsUpdated();

   synchronized (fragment_map) {
      for (Map.Entry<BaleFragment,FragmentData> ent : fragment_map.entrySet()) {
	 BaleFragment bf = ent.getKey();
	 FragmentData fd = ent.getValue();
	 if (fd.getProblemsChanged()) {
	    fd.setProblemsChanged(false);
	    ErrorUpdater eud = new ErrorUpdater(bf);
	    SwingUtilities.invokeLater(eud);
	  }
       }
    }
}



@Override Iterable<BumpProblem> getProblems()
{
   return getProblems(null);
}


Iterable<BumpProblem> getProblems(BaleDocument doc)
{
   readLock();
   try {
      synchronized (problem_set) {
	 ArrayList<BumpProblem> rslt = new ArrayList<BumpProblem>();
	 for (BumpProblem bp : problem_set) {
	    // BoardLog.logD("BALE","LOOK AT PROBLEM " + bp.getProblemId());
	    if (inRange(bp,doc)) {
	       // if (bp.getEditId() == getEditCounter() || bp.getEditId() < 0)
	       rslt.add(bp);
	     }
	  }
	 return rslt;
       }
    }
   finally { readUnlock(); }
}



private boolean inRange(BumpProblem bp,BaleDocument doc)
{
   int soff = mapOffsetToJava(bp.getStart());
   int eoff = mapOffsetToJava(bp.getEnd());

   if (doc == null || soff < 0) return false;
   else if (doc.getFragmentOffset(soff) < 0 || doc.getFragmentOffset(eoff) < 0) return false;

   return true;
}




private static class ErrorUpdater implements Runnable {

   private BaleFragment for_fragment;

   ErrorUpdater(BaleFragment bf) {
      for_fragment = bf;
    }

   @Override public void run() {
      // BoardLog.logD("BALE","Update errors for fragment");
      for_fragment.handleProblemsUpdated();
    }

}	// end of inner class ErrorUpdater




/********************************************************************************/
/*										*/
/*	Handle location mapping 						*/
/*										*/
/********************************************************************************/

@Override BaleRegion getRegionFromEclipse(int soff,int eoff)
{
   soff = mapOffsetToJava(soff);
   eoff = mapOffsetToJava(eoff);

   try {
      Position p0 = createPosition(soff);
      Position p1 = createPosition(eoff);
      return new BaleRegion(p0,p1);
    }
   catch (BadLocationException e) { }

   return null;
}




/********************************************************************************/
/*										*/
/*	Methods for handling new line characters				*/
/*										*/
/*	Note that this assumes that all the new lines in a file are consistent	*/
/*	in that they are all \n or \r\n or \r.	It fails in the case where	*/
/*	the file contains different types of newlines, e.g. \r\n and \r\r\n.	*/
/*										*/
/*	Fixing this to handle the general case and still be fast is going	*/
/*	to be difficult.							*/
/*										*/
/********************************************************************************/

@Override public int mapOffsetToEclipse(int off)
{
   if (newline_adjust == 0) return off;
   if (line_offsets == null) return off;

   readLock();
   try {
      return line_offsets.findEclipseOffset(off);
    }
   finally { readUnlock(); }
}


@Override public int mapOffsetToJava(int off)
{
   if (newline_adjust == 0) return off;
   if (line_offsets == null) return off;

   readLock();
   try {
      return line_offsets.findJavaOffset(off);
    }
   finally { readUnlock(); }
}



@Override public int findLineOffset(int line)
{
   if (line_offsets == null) return 0;
   if (line < 0) return -1;

   readLock();
   try {
      return line_offsets.findOffset(line);
    }
   finally { readUnlock(); }
}



@Override public int findLineNumber(int offset)
{
   if (line_offsets == null) return 0;

   readLock();
   try {
      return line_offsets.findLine(offset);
    }
   finally { readUnlock(); }
}



void getLineAtOffset(int offset,Segment s) throws BadLocationException
{
   readLock();
   try {
      int lno = findLineNumber(offset);
      getLine(lno,s);
    }
   finally { readUnlock(); }
}


void getLine(int lno,Segment s) throws BadLocationException
{
   readLock();
   try {
      int lstart = findLineOffset(lno);
      int lend = findLineOffset(lno+1);
      getText(lstart,lend-lstart,s);
    }
  finally { readUnlock(); }
}



private void fixNewLines(int start,int end,String cnts)
{
   // the document is write-locked when we get here
   if (line_offsets == null) return;

   line_offsets.update(start,end,cnts);
}



private void setupLineOffsets(Reader r,String nl)
{
   newline_string = nl;
   if (newline_string == null) newline_string = System.getProperty("line.separator");
   newline_adjust = newline_string.length() - 1;

   Segment sg = new Segment();

   try {
      getText(0,getLength(),sg);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Problem getting initial offsets: " + e);
    }

   line_offsets = new BaleLineOffsetsNew(newline_string,sg,r);
}



private class LineOffsetListener implements DocumentListener {

   @Override public void changedUpdate(DocumentEvent e) 	{ }

   @Override public void insertUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      try {
         String txt = getText(off,len);
         fixNewLines(off,off,txt);
       }
      catch (BadLocationException ex) {
         BoardLog.logE("BALE","Problem getting insertion text",ex);
       }
    }

   @Override public void removeUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      fixNewLines(off,off+len,null);
    }

}	// end of inner class LineOffsetListener



/********************************************************************************/
/*										*/
/*	Information for a document fragment					*/
/*										*/
/********************************************************************************/

private static class FragmentData {

   private List<BaleRegion> active_regions;
   private Map<BaleRegion,Double> priority_regions;
   private boolean problems_changed;

   FragmentData(Collection<BaleRegion> regions) {
      active_regions = new ArrayList<>(regions);
      priority_regions = new HashMap<>();
      problems_changed = false;
    }

   void setPriorityRegions(Map<BaleRegion,Double> rgns) {
      priority_regions.clear();
      if (rgns != null) priority_regions.putAll(rgns);
    }

   void dumpRegions(BaleDocument bd,IvyXmlWriter xw) {
      for (BaleRegion br : active_regions) {
	 xw.begin("REGION");
	 xw.field("START",bd.mapOffsetToEclipse(br.getStart()));
	 xw.field("END",bd.mapOffsetToEclipse(br.getEnd()));
	 xw.end("REGION");
       }
      for (Map.Entry<BaleRegion,Double> ent : priority_regions.entrySet()) {
	 BaleRegion br = ent.getKey();
	 double p = ent.getValue();
	 xw.begin("REGION");
	 xw.field("START",bd.mapOffsetToEclipse(br.getStart()));
	 xw.field("END",bd.mapOffsetToEclipse(br.getEnd()));
	 xw.field("PRIORITY",p);
	 xw.end("REGION");
       }
    }

   Collection<BaleRegion> getRegions()		{ return active_regions; }

   boolean overlaps(int start,int end) {
      for (BaleRegion br : active_regions) {
	 if (end >= br.getStart() && start <= br.getEnd()) return true;
       }
      return false;
    }

   void setProblemsChanged(boolean fg)		{ problems_changed = fg; }
   boolean getProblemsChanged() 		{ return problems_changed; }

}	// end of inner class FragmentData




/********************************************************************************/
/*										*/
/*	Listener for handling eclipse updates					*/
/*										*/
/********************************************************************************/

private class EclipseUpdater implements DocumentListener {

   @Override public void changedUpdate(DocumentEvent e) { }

   @Override public void insertUpdate(DocumentEvent e) {
      if (!doing_load && !doing_remote && !doing_eload) {
         int off = e.getOffset();
         int len = e.getLength();
         try {
            String txt = getText(off,len);
            int eoff = mapOffsetToEclipse(off);
            bump_client.editFile(project_name,file_name,nextEditCounter(),eoff,eoff,txt);
          }
         catch (BadLocationException ex) { }
       }
    }

   @Override public void removeUpdate(DocumentEvent e) {
      if (!doing_load && !doing_remote && !doing_eload) {
	 int off = e.getOffset();
	 int len = e.getLength();
	 // The delete has already been done.  The length might be off because things
	 // have changed -- i.e. there may be new lines in the deleted segment that don't
	 // show up here.  Length needs to be incremented by the number of newlines that
	 // were deleted.  This is now handled inside bedrock
	 int eoff1 = mapOffsetToEclipse(off);
	 int eoff2 = eoff1+len;
	 bump_client.editFile(project_name,file_name,nextEditCounter(),eoff1,eoff2,null);
       }
    }

}	// end of inner class EclipseUpdater



/********************************************************************************/
/*										*/
/*	File change handler methods						*/
/*										*/
/********************************************************************************/

@Override public void handleFileChanged(String proj,String file)
{
   File f1 = new File(file);
   if (!f1.equals(getFile())) return;

   baleWriteLock();
   try {
      reportEvent(this,0,getLength(),DocumentEvent.EventType.CHANGE,null,null);
    }
   finally { baleWriteUnlock(); }
}


@Override public void handleFileAdded(String proj,String file)		{ }



@Override public void handleFileRemoved(String proj,String file)
{
   // TODO: ensure all fragments are orphaned
}


@Override public void handleFileStarted(String proj,String file)	{ }
@Override public void handleProjectOpened(String proj)			{ }



/********************************************************************************/
/*                                                                              */
/*      Handle history if there is no editor                                    */
/*                                                                              */
/********************************************************************************/

@Override synchronized void setupDummyEditor()
{
   if (dummy_editor != null) return;
   
   List<BaleRegion> rgns = new ArrayList<>();
   
   try {
      Position spos = BaleStartPosition.createStartPosition(this,0);
      Position epos = createPosition(getLength());
      BaleRegion brgn = new BaleRegion(spos,epos);
      rgns.add(brgn);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Bad location for dummy fragment",e);
    }
   
   BaleFragmentEditor bde = new BaleFragmentEditor(getProjectName(),getFile(),"<DUMMY FILE>",
         this,BaleFragmentType.FILE,rgns);
   dummy_editor = bde;
}



}	// end of class BaleDocumentIde




/* end of BaleDocumentIde.java */
