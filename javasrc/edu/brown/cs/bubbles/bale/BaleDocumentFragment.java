/********************************************************************************/
/*										*/
/*		BaleDocumentFragment.java					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment document			*/
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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoableEdit;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;



class BaleDocumentFragment extends BaleDocument implements StyledDocument,
	BaleConstants.BaleFileOverview,
	BaleConstants.BaleFragment, BaleConstants, DocumentListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleDocumentIde 	base_document;
private BaleFragmentType	fragment_type;
private StyleContext		style_context;
private String			fragment_name;
private transient BaleRegion	cursor_region;
private boolean 		is_orphan;
private double			scale_factor;
private boolean                 is_editable;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleDocumentFragment(BaleDocumentIde base,BaleFragmentType typ,List<BaleRegion> rgns)
{
   // super(new BaleFragmentContent(base,rgns));
   super(new BaleFragmentContentIndent(base,rgns));

   BaleFragmentContent cnt = (BaleFragmentContent) getContent();

   base_document = base;
   fragment_type = typ;
   fragment_name = null;
   is_orphan = false;
   scale_factor = 1.0;
   is_editable = base.isEditable();

   style_context = BaleFactory.getFactory().getStyleContext();

   cursor_region = null;

   setupElements();

   base_document.createFragment(this,cnt.getRegions());
   base_document.redoElision();
   base_document.addDocumentListener(this);	// handles UndoableEdit and Document events

   Element e = getDefaultRootElement();
   if (e != null && e instanceof BaleElement) {
      checkForNameChange((BaleElement) getDefaultRootElement());
   }
}



@Override void dispose()
{
   if (base_document != null) {
      base_document.removeFragment(this);
      base_document.removeDocumentListener(this);
      BaleFragmentContent bfc = (BaleFragmentContent) getContent();
      if (bfc != null) bfc.dispose();
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProjectName()	{ return base_document.getProjectName(); }

@Override public File getFile() 	        { return base_document.getFile(); }
@Override public BoardLanguage getLanguage()	{ return base_document.getLanguage(); }

@Override String getFragmentName()		{ return fragment_name; }

@Override int getEditCounter()			{ return base_document.getEditCounter(); }

@Override BaleFragmentType getFragmentType()	{ return fragment_type; }

@Override boolean isEditable()			{ return is_editable; }

boolean canSetEditable()
{
   return base_document.isEditable();
}

void setEditable(boolean fg) 
{
   if (fg && base_document.isEditable()) is_editable = true;
   else if (!fg) is_editable = false;
}


@Override BaleDocument getBaseEditDocument()
{
   return base_document.getBaseEditDocument();
}

void setScaleFactor(double sf)
{
   scale_factor = sf;
}



/********************************************************************************/
/*										*/
/*	Save and reload methods 						*/
/*										*/
/********************************************************************************/

@Override public BaleReloadData startReload()
{
   return new Reloader();
}


private class Reloader implements BaleReloadData {

   Reloader() { }

   @Override public void finishedReload() {
      redoElision();
      recomputeOffsets();
      BaleFragmentContent cnt = (BaleFragmentContent) getContent();
      cnt.reload();
      checkOrphan();
      if (!is_orphan) resetElements();
      else setupOrphanElement();
      handleElisionChange();
      fixElisions();
    }

}	// end of inner class Reloader



@Override public synchronized Position createPosition(int offs) throws BadLocationException
{
   if (is_orphan) return new OrphanPosition(offs);

   return super.createPosition(offs);
}



@Override public BalePosition savePosition(Position bp)
{
   return base_document.savePosition(bp);
}



private void recomputeOffsets()
{
   List<BaleRegion> rgns = BaleFactory.getFactory().getFragmentRegions(this);

   if (rgns == null) {
      BoardLog.logE("BALE","Reload problem: no locations found");
      base_document.removeFragment(this);
      return;
    }

   BaleFragmentContent cnt = (BaleFragmentContent) getContent();
   cnt.resetRegions(rgns);
   resetRegions();
}



/********************************************************************************/
/*										*/
/*	Command methods 							*/
/*										*/
/********************************************************************************/

@Override void save()				{ base_document.save(); }
@Override void revert() 			{ base_document.revert(); }
@Override void checkpoint()			{ base_document.checkpoint(); }

@Override boolean canSave()			{ return base_document.canSave(); }



/********************************************************************************/
/*										*/
/*	Locking methods 							*/
/*										*/
/********************************************************************************/

@Override void baleWriteLock()
{
   base_document.baleWriteLock();
   writeLock();
}


@Override void baleWriteUnlock()
{
   writeUnlock();
   base_document.baleWriteUnlock();
}



@Override void baleReadLock()
{
   base_document.readLock();
   readLock();
}


@Override void baleReadUnlock()
{
   base_document.readUnlock();
   readUnlock();
}


@Override void checkWriteLock()
{
   Thread th = getCurrentWriter();
   if (th != Thread.currentThread()) {
      base_document.checkWriteLock();
    }
}



/********************************************************************************/
/*										*/
/*	Content methods 							*/
/*										*/
/********************************************************************************/

@Override public void insertString(int off,String st,AttributeSet a) throws BadLocationException
{
   baleWriteLock();
   try {
      BaleFragmentContent cnt = (BaleFragmentContent) getContent();
      cnt.handleInsert(off,st,a);
      // TODO: if off is at end, make sure that the end pointer is adjusted
    }
   finally { baleWriteUnlock(); }
}




@Override public void remove(int off,int len) throws BadLocationException
{
   baleWriteLock();
   try {
      BaleFragmentContent cnt = (BaleFragmentContent) getContent();
      cnt.handleRemove(off,len);
    }
   finally { baleWriteUnlock(); }
}



@Override public int findLineNumber(int off)
{
   int doff = getDocumentOffset(off);

   return base_document.findLineNumber(doff);
}



@Override public int findLineOffset(int lno)
{
   int doff = base_document.findLineOffset(lno);

   if (doff < 0) return -1;

   return getFragmentOffset(doff);
}



@Override public int mapOffsetToEclipse(int off)
{
   return base_document.mapOffsetToEclipse(getDocumentOffset(off));
}



@Override public int mapOffsetToJava(int off)
{
   return getFragmentOffset(base_document.mapOffsetToJava(off));
}



@Override public int getFragmentOffset(int docoffset)
{
   BaleFragmentContent cnt = (BaleFragmentContent) getContent();

   return cnt.getFragmentOffset(docoffset);
}


@Override public int getDocumentOffset(int fragoffset)
{
   BaleFragmentContent cnt = (BaleFragmentContent) getContent();

   return cnt.getDocumentOffset(fragoffset);
}



@Override BaleSimpleRegion getFragmentRegion(int docoffset,int len)
{
   // this needs to go thru regions and find start/len that overlaps

   BaleFragmentContent cnt = (BaleFragmentContent) getContent();

   BaleSimpleRegion br = cnt.getFragmentRegion(docoffset,len);

   return br;
}


@Override BaleRegion createDocumentRegion(Position start,Position end,boolean inceol)
		throws BadLocationException
{
   BalePosition sp = (BalePosition) start;
   BalePosition ep = (BalePosition) end;

   Position dsp = base_document.createPosition(sp.getDocumentOffset());
   Position dep = base_document.createPosition(ep.getDocumentOffset());

   return new BaleRegion(dsp,dep,inceol);
}



@Override BaleRegion createDocumentRegion(int spos,int epos,boolean inceol)
		throws BadLocationException
{
   int soff = getDocumentOffset(spos);
   Position sp = BaleStartPosition.createStartPosition(base_document,soff);
   int eoff = getDocumentOffset(epos);
   Position ep = base_document.createPosition(eoff);

   return new BaleRegion(sp,ep,inceol);
}




/********************************************************************************/
/*										*/
/*	Budding methods 							*/
/*										*/
/********************************************************************************/

@Override void removeRegions(List<BaleRegion> rgns)
{
   BaleFragmentContent cnt = (BaleFragmentContent) getContent();
   if (cnt.removeRegions(rgns)) {
      resetElements();
      resetRegions();		// this does a fixup elision
      base_document.redoElision();
     }
   checkOrphan();
}


private void resetRegions()
{
   checkOrphan();
   if (is_orphan) base_document.removeFragment(this);
   else {
      BaleFragmentContent cnt = (BaleFragmentContent) getContent();
      base_document.createFragment(this,cnt.getRegions());
    }
}



/********************************************************************************/
/*										*/
/*	Style methods								*/
/*										*/
/********************************************************************************/

@Override public Style addStyle(String nm,Style par)	{ return style_context.addStyle(nm,par); }

@Override public Style getStyle(String nm)		{ return style_context.getStyle(nm); }

@Override public void removeStyle(String nm)		{ style_context.removeStyle(nm); }

@Override public Font getFont(AttributeSet attr)
{
   Font ft = style_context.getFont(attr);
   if (scale_factor == 0 || scale_factor == 1) return ft;
   float sz = ft.getSize2D();
   sz *= (float) scale_factor;
   ft = ft.deriveFont(sz);
   return ft;
}

@Override public Color getForeground(AttributeSet a)	{ return style_context.getForeground(a); }

@Override public Color getBackground(AttributeSet a)	{ return style_context.getBackground(a); }




@Override public Style getLogicalStyle(int off)
{
   Style s = null;
   Element p = getParagraphElement(off);
   if (p != null) {
      AttributeSet a = p.getAttributes();
      AttributeSet pa = a.getResolveParent();
      if (pa instanceof Style) s = (Style) pa;
    }
   return s;
}


@Override public void setLogicalStyle(int off,Style s)	{ }

@Override public void setCharacterAttributes(int off,int len,AttributeSet s,boolean rep) { }

@Override public void setParagraphAttributes(int off,int len,AttributeSet s,boolean rep) { }




/********************************************************************************/
/*										*/
/*	Element methods 							*/
/*										*/
/********************************************************************************/

private void setupElements()
{
   BaleElement.Branch root = null;

   switch (fragment_type) {
      case METHOD :
	 // root = new BaleElement.MethodNode(this,null);
	 root = new BaleElement.DeclSet(this,null);
	 break;
      case CLASS :
	 // root = new BaleElement.ClassNode(this,null);
	 root = new BaleElement.DeclSet(this,null);
	 break;
      case FILE :
      case ROFILE :
	 root = new BaleElement.CompilationUnitNode(this,null);
	 break;
      case FIELDS :
	 root = new BaleElement.DeclSet(this,null);
	 break;
      case STATICS :
      case MAIN :
      case IMPORTS :
      case EXPORTS :
      case CODE :
      case HEADER :
	 root = new BaleElement.DeclSet(this,null);
	 break;
      default :
	 BoardLog.logE("BALE","Unknown fragment type: " + fragment_type);
	 break;
    }

   if (root == null) return;

   setRootElement(root);
}



/********************************************************************************/
/*										*/
/*	Cursor methods								*/
/*										*/
/********************************************************************************/

void setCursorRegion(int soff,int len)
{
   try {
      Position pos = base_document.createPosition(getDocumentOffset(soff));
      if (cursor_region == null) {
	 cursor_region = new BaleRegion(pos,len);
       }
      else {
	 cursor_region.reset(pos,len);
       }
      if (getElideMode() == BaleElideMode.ELIDE_CHECK_ALWAYS) {
	 handleElisionChange();
       }
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Bad cursor location specified");
    }
}


/********************************************************************************/
/*										*/
/*	Problem methods 							*/
/*										*/
/********************************************************************************/

@Override Iterable<BumpProblem> getProblems()
{
   return base_document.getProblems(this);
}



/********************************************************************************/
/*										*/
/*	Mapping methods 							*/
/*										*/
/********************************************************************************/

@Override BaleRegion getRegionFromEclipse(int soff,int eoff)
{
   int xsoff = base_document.mapOffsetToJava(soff);
   int xeoff = base_document.mapOffsetToJava(eoff);

   int fsoff = getFragmentOffset(xsoff);
   int feoff = getFragmentOffset(xeoff);

   if (fsoff < 0 || feoff < 0) return null;

   try {
      Position p0 = createPosition(fsoff);
      Position p1 = createPosition(feoff);
      return new BaleRegion(p0,p1);
    }
   catch (BadLocationException e) { }

   return null;
}



/********************************************************************************/
/*										*/
/*	Naming methods: track fragment name changes				*/
/*										*/
/********************************************************************************/

@Override void noteOpen()
{
   Element e = getDefaultRootElement();
   if (e != null && e instanceof BaleElement) {
      checkForNameChange((BaleElement) getDefaultRootElement());
   }
}


private boolean checkForNameChange(BaleElement be)
{
   if (be instanceof BaleElement.MethodDeclId && fragment_type == BaleFragmentType.METHOD) {
      // check for methods inside nested class
      for (BaleElement.Branch par = be.getBaleParent(); par != null; par = par.getBaleParent()) {
	 if (par.getName().equals("Class")) {
	    for (BaleElement.Branch xpar = par.getBaleParent();
		 xpar != null;
		 xpar = xpar.getBaleParent()) {
	       if (xpar.getName().equals("Method")) return false;
	     }
	  }
       }
      String id = be.getFullName();
      if (id == null) return false;
      if(!id.contains("(")) id += "(...)";
      fragment_name = id;
      return true;
    }
   else if (be instanceof BaleElement.ClassDeclId || be instanceof BaleElement.ClassDeclMemberId) {
      if (fragment_type == BaleFragmentType.CLASS) {
	 String id = be.getFullName();
	 if (id == null) return false;
	 fragment_name = id;
	 return true;
       }
      else if (fragment_type == BaleFragmentType.HEADER) {
	 if (be.getFullName() == null) return false;
	 String id = be.getFullName() + ".<PREFIX>";
	 fragment_name = id;
	 return true;
       }
      else if (fragment_type == BaleFragmentType.FILE) {
	 if (be.getFullName() == null) return false;
	 String id = be.getFullName() + ".<FILE>";
	 fragment_name = id;
	 return true;
       }
      else if (fragment_type == BaleFragmentType.METHOD) {
	 // check for methods inside nested class
	 int ct = 0;
	 for (BaleElement.Branch par = be.getBaleParent(); par != null; par = par.getBaleParent()) {
	    if (par.getName().equals("Class")) ++ct;
	  }
	 if (ct > 1) return false;
	 String id = be.getFullName();
	 if (id == null) return false;
	 fragment_name = id;
	 return true;
       }
    }		
   else if (be instanceof BaleElement.FieldDeclId && fragment_type == BaleFragmentType.FIELDS) {
      String id = be.getFullName();
      if (id != null) {
	 int idx = id.lastIndexOf(".");
	 String pfx = "";
	 if (idx >= 0) pfx = id.substring(0,idx) + ". ";
	 pfx += "< " + BaleFactory.getFieldsName() + " >";
	 fragment_name = pfx;
	 return true;
       }
    }
   else if (be instanceof BaleElement.CompilationUnitNode && fragment_type == BaleFragmentType.FILE) {
      String id = be.getBaleDocument().getFile().getName();
      int idx = id.lastIndexOf(".");
      if (idx > 0) id = id.substring(0,idx);
      id += ".<FILE>";
      fragment_name = id;
      return true;
    }
   // need to find name for an initializer here without having a declaration element
   else if (be != null && !be.isLeaf()) {
      for (int i = 0; i < be.getElementCount(); ++i) {
	 if (checkForNameChange(be.getBaleElement(i))) return true;
       }
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Document event management						*/
/*										*/
/********************************************************************************/

@Override public void changedUpdate(DocumentEvent e)
{
   if (is_orphan) return;

   checkOrphan();

   handleEvent(e,null);
}




@Override public void insertUpdate(DocumentEvent e)
{
   if (is_orphan) return;

   BaleFragmentContent bcf = (BaleFragmentContent) getContent();
   bcf.noteBeginEdit();
   checkOrphan();

   BaleElementEvent ee = null;
   if (is_orphan) {
      ee = setupOrphanElement();
    }
   else {
      ee = elementInsertString(e.getOffset(),e.getLength());
    }

   if (ee != null) handleEvent(e,ee);	// propagate events
}




@Override public void removeUpdate(DocumentEvent e)
{
   if (is_orphan) return;

   BaleFragmentContent bcf = (BaleFragmentContent) getContent();
   bcf.noteBeginEdit();
   checkOrphan();

   BaleElementEvent ee = null;
   if (is_orphan) {
      ee = setupOrphanElement();
    }
   else {
      ee = elementRemove(e.getOffset(),e.getLength());
    }

   if (ee != null) handleEvent(e,ee);	// propagate events
}



private void handleEvent(DocumentEvent e,BaleElementEvent ee)
{
   checkWriteLock();

   if (fragment_name == null && getDefaultRootElement() instanceof BaleElement) checkForNameChange((BaleElement) getDefaultRootElement());

   int doff = e.getOffset();
   int foff = getFragmentOffset(doff);
   int eoff;

   if (e.getType() == DocumentEvent.EventType.INSERT) {
      // take indentation into account -- actual length might differ
      eoff = getFragmentOffset(doff + e.getLength());
    }
   else {
      // otherwise, since the deletion has occurred, use the actual length
      eoff = foff + e.getLength();
    }

   int flen = eoff - foff;

   if (foff >= 0) {
      // TODO: Might need to split this event into subevents
      if (e instanceof BaleDocumentEvent) {
	 BaleDocumentEvent be = (BaleDocumentEvent) e;
	 reportEvent(this,foff,flen,be.getType(),be.getEdit(),ee);
       }
      else if (e instanceof UndoableEdit) {
	 reportEvent(this,foff,flen,e.getType(),(UndoableEdit) e,ee);
       }
      else {
	 reportEvent(this,foff,flen,e.getType(),null,ee);
       }
    }
   else if (doff == 0 && e.getLength() > getDocumentOffset(0) && getLength() >= 0) {
      // special case the whole document changed: used after save
      reportEvent(this,0,getLength(),e.getType(),null,null);
    }
   else if (is_orphan) {
      reportEvent(this,0,1,e.getType(),null,ee);
    }
   else if (ee != null) {
      BoardLog.logX("BALE","Buffer changed for event outside of fragment");
    }
}


@Override protected void noteEvent(BaleDocumentEvent e)
{
   baleReadLock();
   try {
      if (getDefaultRootElement() instanceof BaleElement)
	 checkForNameChange((BaleElement) getDefaultRootElement());
    }
   finally { baleReadUnlock(); }
}



/********************************************************************************/
/*										*/
/*	Orphan management methods						*/
/*										*/
/********************************************************************************/

@Override protected void checkOrphan()
{
   if (is_orphan) return;

   int ln = getLength();
   if (ln <= 0) {
      BoardLog.logD("BALE","Making orphan");
      is_orphan = true;
    }
}


@Override boolean isOrphan()			  { return is_orphan; }



private BaleElementEvent setupOrphanElement()
{
   BaleElement oe = new BaleElement.OrphanElement(this,null,fragment_name);
   BaleElement be = null;
   if (getDefaultRootElement() instanceof BaleElement.Branch) {
      be = (BaleElement) getDefaultRootElement();
      BaleElement.Branch p = (BaleElement.Branch) be;
      List<BaleElement> rl = new ArrayList<>();
      rl.add(oe);
      BaleElementEvent ee = new BaleElementEvent(p,rl);
      p.clear();
      p.add(oe);
      return ee;
    }
   else if (getDefaultRootElement() instanceof AbstractDocument.BranchElement) {
      AbstractDocument.BranchElement b = (AbstractDocument.BranchElement) getDefaultRootElement();
      b.replace(0,b.getChildCount(),new Element [] { oe });
      BoardLog.logD("BALE","Attempt to set up orphan for dummy " + getDefaultRootElement());
      return null;
    }
   else {
      BoardLog.logD("BALE","Attempt to set up orphan for unknown " + getDefaultRootElement());
      return null;
    }

}



private static class OrphanPosition implements Position
{
   int base_position;

   OrphanPosition(int pos)			{ base_position = pos; }

   @Override public int getOffset()		{ return base_position; }

}	// end of inner class OrphanPosition




}	// end of class BaleDocumentFragment




/* end of BaleDocumentFragment.java */



