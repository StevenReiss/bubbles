/********************************************************************************/
/*										*/
/*		BaleDocument.java						*/
/*										*/
/*	Bubble Annotated Language Editor abstract base document 		*/
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

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.burp.BurpConstants.BurpEditorDocument;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.undo.UndoableEdit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;



abstract class BaleDocument extends AbstractDocument
   implements BaleConstants, BurpEditorDocument, BaleConstants.BaleWindowDocument
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private transient BaleElementBuffer element_buffer;
private DummyElement		dummy_element;
private BaleElideMode		elide_mode;
private BaleSplitMode		split_mode;
private transient BaleTabHandler tab_handler;
private transient BaleIndenter	our_indenter;

private int			id_counter;		// count # of edits
private long			last_edit;

private transient Map<Position,String> created_text;


private static AtomicInteger	edit_counter = new AtomicInteger();
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleDocument(AbstractDocument.Content data)
{
   super(data);
   id_counter = 0;
   last_edit = 0;
   element_buffer = null;
   dummy_element = null;

   elide_mode = BaleElideMode.ELIDE_CHECK_ONCE;
   if (BALE_PROPERTIES.getBoolean(BALE_EDITOR_NO_ELISION))
      elide_mode = BaleElideMode.ELIDE_NONE;
   else if (BALE_PROPERTIES.getBoolean(BALE_EDITOR_ALWAYS_ELIDE))
      elide_mode = BaleElideMode.ELIDE_CHECK_ALWAYS;

   split_mode = BaleSplitMode.SPLIT_NORMAL;
   if (BALE_PROPERTIES.getBoolean(BALE_EDITOR_NO_REFLOW))
      split_mode = BaleSplitMode.SPLIT_NEVER;
   else if (getLength() > SPLIT_QUICK_SIZE)
      split_mode = BaleSplitMode.SPLIT_QUICK;

   tab_handler = new BaleTabHandler();
   our_indenter = null;

   created_text = new HashMap<>();
}



void dispose()
{ }


protected void handleLoaded()
{
   if (split_mode == BaleSplitMode.SPLIT_NORMAL && getLength() > SPLIT_QUICK_SIZE)
      split_mode = BaleSplitMode.SPLIT_QUICK;
}



/********************************************************************************/
/*										*/
/*	Element methods 							*/
/*										*/
/********************************************************************************/

@Override public Element getDefaultRootElement()
{
   if (element_buffer == null) {
      synchronized (this) {
	 if (dummy_element == null) dummy_element = new DummyElement(null,null,0,getLength()-1);
	 return dummy_element;
       }
    }

   return element_buffer.getRootElement();
}



protected void setRootElement(BaleElement.Branch root)
{
   if (root == null) return;

   baleWriteLock();
   try {
      element_buffer = new BaleElementBuffer(root,this);
      element_buffer.setup();
      tab_handler.setElement(root);
    }
   finally { baleWriteUnlock(); }
}



protected void resetElements()
{
   if (element_buffer != null) {
      baleWriteLock();
      try {
	 BaleElementEvent ee = element_buffer.setup();
	 if (ee != null) reportEvent(this,0,getLength(),DocumentEvent.EventType.CHANGE,null,ee);
       }
      finally { baleWriteUnlock(); }
    }
}



@Override public Element [] getRootElements()
{
   return new Element[] { getDefaultRootElement() };
}



/**
 *	Used to implement StyledDocument
 **/

public BaleElement getCharacterElement(int pos)
{
   readLock();
   try {
      Element e = null;
      for (e = getDefaultRootElement(); e != null && !e.isLeaf() && !isElided(e); ) {
	 int idx = e.getElementIndex(pos);
	 e = e.getElement(idx);
       }
      if (e instanceof BaleElement) return (BaleElement) e;
      return null;
    }
   finally { readUnlock(); }
}



BaleElement getActualCharacterElement(int pos)
{
   // readLock();
   try {
      Element e = null;
      for (e = getDefaultRootElement(); e != null && !e.isLeaf(); ) {
	 int idx = e.getElementIndex(pos);
	 e = e.getElement(idx);
       }
      if (e != null && !(e instanceof BaleElement)) return null;
      return (BaleElement) e;
    }
   finally {
      // readUnlock();
      }
}



@Override public BaleElement.Branch getParagraphElement(int pos)
{
   readLock();

   try {
      Element e = getCharacterElement(pos);
      if (e == null) return null;
      if (isElided(e)) return (BaleElement.Branch) e;

      BaleElement.Branch p = (BaleElement.Branch) e.getParentElement();

      return p;
    }
   finally { readUnlock(); }
}


BaleElement.Branch getActualParagraphElement(int pos)
{
   readLock();

   try {
      Element e = getActualCharacterElement(pos);
      if (e == null) return null;
      if (isElided(e) && !e.isLeaf()) return (BaleElement.Branch) e;

      BaleElement.Branch p = (BaleElement.Branch) e.getParentElement();

      return p;
    }
   finally { readUnlock(); }
}


protected BaleElementEvent elementInsertString(int off,int len)
{
   if (element_buffer == null) return null;

   checkWriteLock();
   return element_buffer.insertString(off,len);
}


protected BaleElementEvent elementRemove(int off,int len)
{
   if (element_buffer == null) return null;

   checkWriteLock();
   return element_buffer.remove(off,len);
}


private boolean isElided(Element e)
{
   if (e instanceof BaleElement) {
      BaleElement be = (BaleElement) e;
      return be.isElided();
    }
   return false;
}



/********************************************************************************/
/*										*/
/*	Locking methods 							*/
/*										*/
/********************************************************************************/

void baleWriteLock()				{ writeLock(); }
void baleWriteUnlock()				{ writeUnlock(); }
void baleReadLock()				{ readLock(); }
void baleReadUnlock()				{ readUnlock(); }

void checkWriteLock()
{
   Thread th = getCurrentWriter();
   if (th != Thread.currentThread()) {
      try {
	 throw new Exception("Attempt to change document without write lock");
       }
      catch (Exception e) {
	 BoardLog.logE("BALE","Locking problems",e);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

@Override public void remove(int off,int len) throws BadLocationException
{
   baleWriteLock();
   try {
      super.remove(off,len);
    }
   finally { baleWriteUnlock(); }
}




@Override public void replace(int off,int len,String txt,AttributeSet attr)
	throws BadLocationException
{
   baleWriteLock();
   try {
      super.replace(off,len,txt,attr);
    }
   finally { baleWriteUnlock(); }
}





@Override public void insertString(int off,String txt,AttributeSet attr)
	throws BadLocationException
{
   baleWriteLock();
   try {
      super.insertString(off,txt,attr);
    }
   finally { baleWriteUnlock(); }
}


public void markChanged()				{ }



// for BaleFileOverview
public boolean replace(int off,int len,String text,boolean fmt,boolean ind)
{
   Position sp = null;
   Position ep = null;

   setupDummyEditor();

   baleWriteLock();
   try {
      replace(off,len,text,null);
      int soff = off;
      int eoff = off;
      if (text != null) eoff += text.length();
      sp = createPosition(soff);
      ep = createPosition(eoff);
      if (fmt) {
	 int dsoff = mapOffsetToEclipse(soff);
	 int deoff = mapOffsetToEclipse(eoff);
	 BumpClient bc = BumpClient.getBump();
	 org.w3c.dom.Element edits = bc.format(getProjectName(),getFile(),dsoff,deoff);
	 if (edits != null) {
	    BaleApplyEdits bae = new BaleApplyEdits(this);
	    bae.applyEdits(edits);
	 }
      }
      if (ind) {
	 int isoff = sp.getOffset();
	 int ieoff = ep.getOffset();
	 int slno = findLineNumber(isoff);
	 int elno = findLineNumber(ieoff);
	 for (int i = slno; i <= elno; ++i) {
	    fixLineIndent(i);
	 }
      }
   }
   catch (BadLocationException e) {
      return false;
   }
   finally { baleWriteUnlock(); }

   return false;
}




BaleDocument getBaseEditDocument()			{ return this; }


void setupDummyEditor() 				{ }



/********************************************************************************/
/*										*/
/*	Methods for external use of documents					*/
/*										*/
/********************************************************************************/

@Override public String getWindowText(int offset,int length)
{
   try {
      return getText(offset,length);
    }
   catch (BadLocationException e) { }
   return null;
}


@Override public BaleFileOverview getBaseWindowDocument()
{
   return (BaleFileOverview) getBaseEditDocument();
}



/********************************************************************************/
/*										*/
/*	Methods to handle AST information and use with elements 		*/
/*										*/
/********************************************************************************/

/**
 *	Used to implement BaleFragment.handleAstUpdated in subclasses.
 **/

public void handleAstUpdated(List<BaleAstNode> nodes)
{
   if (element_buffer == null) return;

   baleReadLock();
   try {
      checkOrphan();
      if (isOrphan()) return;
   }
   finally { baleReadUnlock(); }

   // BoardLog.logD("BALE","Update AST for " + nodes);
   BaleAstNode oast = null;

   baleWriteLock();
   try {
      BaleElement be = (BaleElement) getDefaultRootElement();
      oast = be.getAstNode();
      BaleElementEvent ee = element_buffer.updateAst(nodes);
      if (ee != null) reportEvent(this,0,getLength(),DocumentEvent.EventType.CHANGE,null,ee);
    }
   finally { baleWriteUnlock(); }

   if (oast == null) {
      synchronized (this) {		// notify that AST is now ready
	 notifyAll();
       }
    }
}



void handleElisionChange()
{
   if (element_buffer == null) return;

   baleWriteLock();
   try {
      reportEvent(this,0,getLength(),DocumentEvent.EventType.CHANGE,null,null);
    }
   finally { baleWriteUnlock(); }
}



void waitForAst()
{
   if (element_buffer == null) return;

   // TODO: This can fail, which in turns hangs bubbles.  This shouldn't happen
   // Happens possibly when the file is updated externally and refresh might be
   // needed.

   BaleElement be = (BaleElement) getDefaultRootElement();

   synchronized (this) {
      int ctr = 0;
      while (be.getAstNode() == null && ctr < 4) {
	 BoardLog.logI("BALE","AST timeout " + ctr);
	 try {
	    ++ctr;
	    wait(1000*ctr);
	  }
	 catch (InterruptedException e) { }
       }
      if (be.getAstNode() == null) {
	 BoardLog.logE("BALE","AST not found for " + be);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to handle breakpoints						*/
/*										*/
/********************************************************************************/

/**
 *	Used to implement BaleFragment.handleProblemsUpdated in subclasses.
 **/

public void handleProblemsUpdated()
{
   if (element_buffer != null) {
      baleWriteLock();
      try {
	  element_buffer.updateProblems();
       }
      finally { baleWriteUnlock(); }
    }
}


Iterable<BumpProblem> getProblems()
{
   return new ArrayList<>();
}



@Override public List<BumpProblem> getProblemsAtLocation(int pos)
{
   if (element_buffer == null) return null;

   baleReadLock();
   try {
      return element_buffer.getProblemsAtLocation(pos);
    }
   finally { baleReadUnlock(); }
}



/********************************************************************************/
/*										*/
/*	Line number methods							*/
/*										*/
/********************************************************************************/

@Override abstract public int findLineNumber(int offset);
@Override abstract public int findLineOffset(int linenumber);

@Override abstract public int mapOffsetToEclipse(int offset);
@Override abstract public int mapOffsetToJava(int offset);


int getColumnPosition(int offset)
{
   int lno = findLineNumber(offset);
   int soff = findLineOffset(lno);
   if (soff == offset) return 0;

   Segment s = new Segment();
   try {
      getText(soff,offset-soff,s);
    }
   catch (BadLocationException e) {
      return 0;
    }

   int pos = 0;
   for (int i = 0; i < s.length(); ++i) {
      char ch = s.charAt(i);
      if (ch == '\t') {
	 pos = tab_handler.nextTabPosition(pos);
       }
      else pos += 1;
    }

   return pos;
}



int getFirstNonspace(int line)
{
   int soff = findLineOffset(line);
   int eoff = findLineOffset(line+1);
   if (eoff < 0) eoff = soff;
   Segment s = new Segment();
   try {
      getText(soff,eoff-soff,s);
    }
   catch (BadLocationException e) {
      return soff;
    }

   for (int i = 0; i < s.length(); ++i) {
      char ch = s.charAt(i);
      if (!Character.isWhitespace(ch)) return soff + i;
    }

   return -soff;
}



int getNextTabPosition(int cpos)
{
   if (getContent() instanceof BaleFragmentContent) {
      BaleFragmentContent bfc = (BaleFragmentContent) getContent();
      int rslt = bfc.getNextTabPosition(cpos,tab_handler);
      if (rslt > 0) return rslt;
    }
   return tab_handler.nextTabPosition(cpos);
}


float getFontHeight()
{
   return tab_handler.getFontHeight();
}



/********************************************************************************/
/*										*/
/*	Fragment methods							*/
/*										*/
/********************************************************************************/

@Override public String getProjectName()	{ return null; }
@Override public File getFile() 	{ return null; }
String getFragmentName()			{ return null; }
BaleFragmentType getFragmentType()		{ return BaleFragmentType.NONE; }
@Override public BoardLanguage getLanguage()
{
   String f = getFile().getName();
   if (f == null) return BoardLanguage.JAVA;
   if (f.startsWith("/REBUS/")) return BoardLanguage.REBUS;
   if (f.endsWith(".py") || f.endsWith(".PY")) return BoardLanguage.PYTHON;
   if (f.endsWith(".js") || f.endsWith(".JS")) return BoardLanguage.JS;
   if (f.endsWith(".java")) return BoardLanguage.JAVA;
   return BoardLanguage.JAVA;
}


boolean isEditable()				{ return true; }


int getEditCounter()				{ return id_counter; }
long getLastEditTime()				{ return last_edit; }
protected int nextEditCounter()
{
   last_edit = System.currentTimeMillis();
   clearIndenter();
   id_counter = edit_counter.incrementAndGet();
   return id_counter;
}



BaleRegion getRegionFromLocation(BumpLocation l)
{
   if (l == null) return null;
   if (l.getProject() != null && !l.getProject().equals(getProjectName())) return null;
   if (l.getFile() != null && !l.getFile().equals(getFile())) return null;

   return getRegionFromEclipse(l.getOffset(),l.getEndOffset());
}

BaleRegion getRegionFromEclipse(int soff,int eoff)	{ return null; }

void save()					{ }
void commit()					{ }
void compile()					{ }
void revert()					{ }
void checkpoint()				{ }

boolean canSave()				{ return false; }


public int getFragmentOffset(int docoffset)	       { return docoffset; }
public int getDocumentOffset(int localoffset)	       { return localoffset; }
BaleSimpleRegion getFragmentRegion(int docoffset,int len)
{
   return new BaleSimpleRegion(getFragmentOffset(docoffset),len);
}


BaleRegion createDocumentRegion(Position start,Position end,boolean incleol)
	throws BadLocationException
{
   return new BaleRegion(savePosition(start),savePosition(end),incleol);
}


BaleRegion createDocumentRegion(int soff,int eoff,boolean inceol)
		throws BadLocationException
{
   Position sp = BaleStartPosition.createStartPosition(this,soff);
   Position ep = createPosition(eoff);

   return new BaleRegion(sp,ep,inceol);
}




void removeRegions(List<BaleRegion> rgns)		{ }


@Override public Position createHistoryPosition(int off) throws BadLocationException
{					
   return getBaseEditDocument().createPosition(getDocumentOffset(off));
}




/********************************************************************************/
/*										*/
/*	Position methods							*/
/*										*/
/********************************************************************************/

/**
 *	Ensure the position is correct across file saves
 **/

public BalePosition savePosition(Position p)
{
   BalePosition bp;

   if (p instanceof BalePosition) bp = (BalePosition) p;
   else bp = new BalePosition(p);

   return bp;
}



/********************************************************************************/
/*										*/
/*	Highlighting methods							*/
/*										*/
/********************************************************************************/

BaleHighlightContext getHighlightContext()
{
   return BaleFactory.getFactory().getGlobalHighlightContext();
}




/********************************************************************************/
/*										*/
/*	Elision methods 							*/
/*										*/
/********************************************************************************/

BaleElideMode getElideMode()		{ return elide_mode; }

void setElideMode(BaleElideMode md)	{ elide_mode = md; }

BaleSplitMode getSplitMode()		{ return split_mode; }


void recheckElisions()
{
   // called when viewport changes size

   if (elide_mode == BaleElideMode.ELIDE_CHECK_ALWAYS) return;

   handleElisionChange();
   fixElisions();
}


void fixElisions()
{
   if (elide_mode != BaleElideMode.ELIDE_CHECK_ONCE) return;

   elide_mode = BaleElideMode.ELIDE_CHECK_NEVER;
}


void redoElision()
{
   switch (elide_mode) {
      case ELIDE_CHECK_NEVER :
      case ELIDE_NONE :
      case ELIDE_COMMENTS :
	 elide_mode = BaleElideMode.ELIDE_CHECK_ONCE;
	 break;
      default:
	 break;
    }
}


void removeElision()
{
   elide_mode = BaleElideMode.ELIDE_NONE;
}

void removeCodeElision()
{
   elide_mode = BaleElideMode.ELIDE_COMMENTS;
}


void outputElisions(BudaXmlWriter xw)
{
   if (element_buffer == null) return;

   xw.begin("ELISIONS");
   BaleElement e = element_buffer.getRootElement();
   outputElisions(e,xw);
   xw.end("ELISIONS");
}


private void outputElisions(BaleElement e,BudaXmlWriter xw)
{
   if (isElided(e)) {
      xw.begin("ELISION");
      xw.field("START",e.getStartOffset());
      xw.field("END",e.getEndOffset());
      xw.field("NAME",e.getName());
      xw.end("ELISION");
    }
   else if (e != null && !e.isLeaf()) {
      int ct = e.getChildCount();
      for (int i = 0; i < ct; ++i) {
	 BaleElement e1 = e.getBaleElement(i);
	 outputElisions(e1,xw);
       }
    }
}


void applyElisions(List<BaleElisionData> elides)
{
   waitForAst();
   if (element_buffer == null) return;

   baleWriteLock();
   try {
      if (getElideMode() == BaleElideMode.ELIDE_CHECK_ONCE ||
	 getElideMode() == BaleElideMode.ELIDE_CHECK_NEVER) {
	 BaleElement e = element_buffer.getRootElement();
	 int idx = matchElisions(elides,0,e);
	 if (idx < elides.size()) return;
	 applyElisions(elides,0,e);
	 setElideMode(BaleElideMode.ELIDE_CHECK_NEVER);
       }
    }
   finally {
      baleWriteUnlock();
    }
}


private int matchElisions(List<BaleElisionData> elides,int idx,BaleElement e)
{
   if (idx >= elides.size()) return idx;
   BaleElisionData bed = elides.get(idx);
   if (e.getStartOffset() == bed.getStartOffset() &&
	 e.getEndOffset() == bed.getEndOffset() &&
	 bed.getElementName().equals(e.getName())) {
      ++idx;
    }
   else if (!e.isLeaf()) {
      int ct = e.getChildCount();
      for (int i = 0; i < ct; ++i) {
	 BaleElement e1 = e.getBaleElement(i);
	 idx = matchElisions(elides,idx,e1);
       }
    }
   return idx;
}


private int applyElisions(List<BaleElisionData> elides,int idx,BaleElement e)
{
   BaleElisionData bed = null;
   if (idx < elides.size()) bed = elides.get(idx);
   if (bed != null &&
	    e.getStartOffset() == bed.getStartOffset() &&
	    e.getEndOffset() == bed.getEndOffset() &&
	    bed.getElementName().equals(e.getName())) {
      e.setElided(true);
      ++idx;
    }
   else if (!e.isLeaf()) {
      e.setElided(false);
      int ct = e.getChildCount();
      for (int i = 0; i < ct; ++i) {
	 BaleElement e1 = e.getBaleElement(i);
	 idx = applyElisions(elides,idx,e1);
       }
    }
   return idx;
}




/********************************************************************************/
/*										*/
/*	Event methods								*/
/*										*/
/********************************************************************************/

protected void reportEvent(Document d,int off,int len,DocumentEvent.EventType et,UndoableEdit ed,
			      BaleElementEvent ee)
{
   BaleDocumentEvent be = new BaleDocumentEvent((BaleDocument) d,off,len,et,ed,ee);

   checkWriteLock();

   if (et == DocumentEvent.EventType.REMOVE) {
      clearIndenter();
      fireRemoveUpdate(be);
    }
   else if (et == DocumentEvent.EventType.INSERT) {
      clearIndenter();
      fireInsertUpdate(be);
    }
   else if (et == DocumentEvent.EventType.CHANGE) {
      fireChangedUpdate(be);
    }

   if (ed != null) {
      UndoableEditEvent uev = new UndoableEditEvent(d,be);
      fireUndoableEditUpdate(uev);
    }

   noteEvent(be);
}



protected void noteEvent(BaleDocumentEvent be)			{ }

void noteOpen() 						{ }




/********************************************************************************/
/*										*/
/*	Indentation methods							*/
/*										*/
/********************************************************************************/

int getSplitIndent(int off)
{
   BaleIndenter bind = getIndenter();

   return bind.getSplitIndentationDelta(off);
}



void fixLineIndent(int lno)
{
   int soff = findLineOffset(lno);

   BaleIndenter bind = getIndenter();

   int nind = bind.getDesiredIndentation(soff);
   int oind = bind.getCurrentIndentation(soff);

   if (nind == oind) return;

   int deleteln = 0;
   if (oind > 0) {
      int eoff = findLineOffset(lno+1);
      Segment s = new Segment();
      try {
	 getText(soff,eoff-soff-1,s);	      // ignore new line
       }
      catch (BadLocationException e) {
	 return;
       }

      for (int i = 0; i < s.length(); ++i) {
	 char ch = s.charAt(i);
	 if (!Character.isWhitespace(ch)) break;
	 ++deleteln;
       }
    }

   try {
      if (deleteln > 0) remove(soff,deleteln);
      if (nind > 0) {
	 StringBuffer buf = new StringBuffer();
	 for (int i = 0; i < nind; ++i) buf.append(" ");           // don't use tabs for now
	 insertString(soff,buf.toString(),null);
       }
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Problem setting indentation: " + e);
    }
}



BaleIndenter getIndenter()
{
   if (our_indenter == null) {
      switch (getLanguage()) {
	 case JAVA :
	 case JAVA_IDEA :
	 case REBUS :
	 default :
	    our_indenter = new BaleIndenterJava(this);
	    break;
	 case PYTHON :
	    our_indenter = new BaleIndenterPython(this);
	    break;
	 case JS :
	    our_indenter = new BaleIndenterJS(this);
	    break;
       }
    }

   return our_indenter;
}


protected void clearIndenter()
{
   our_indenter = null;
}



/********************************************************************************/
/*										*/
/*	Orphan Management							*/
/*										*/
/********************************************************************************/

protected void checkOrphan()		{ }
boolean isOrphan()			{ return false; }



/********************************************************************************/
/*										*/
/*	Auto-inserted typeover management					*/
/*										*/
/********************************************************************************/

void setCreatedTypeover(String cnt,int off)
{
   for (int i = 0; i < cnt.length(); ++i) {
      String c = cnt.substring(i,i+1);
      try {
	 Position p = createPosition(off+i);
	 created_text.put(p,c);
       }
      catch (BadLocationException e) { }
    }
}


boolean checkTypeover(String txt,int off)
{
   for (Iterator<Map.Entry<Position,String>> it = created_text.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<Position,String> ent = it.next();
      int poff = ent.getKey().getOffset();
      if (poff == off) {
	 if (txt.equals(ent.getValue())) {
	    it.remove();
	    return true;
	  }
       }
    }
   return false;
}


void handleReplaceTypeover(int soff,int eoff)
{
   for (Iterator<Map.Entry<Position,String>> it = created_text.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<Position,String> ent = it.next();
      int poff = ent.getKey().getOffset();
      if (poff >= soff && poff < eoff) it.remove();
    }
}



/********************************************************************************/
/*										*/
/*	Dummy element								*/
/*										*/
/********************************************************************************/

private class DummyElement extends AbstractDocument.LeafElement
{
   private static final long serialVersionUID = 1;

   DummyElement(Element par,AttributeSet a,int soff,int eoff) {
      super(par,a,soff,eoff);
    }

   @Override public int getEndOffset()		{ return getLength()-1; }
   @Override public int getStartOffset()	{ return 0; }

}	// end of inner class DummyElement




}	// end of class BaleDocument




/* end of BaleDocument.java */

