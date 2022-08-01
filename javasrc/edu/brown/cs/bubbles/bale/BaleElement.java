/********************************************************************************/
/*										*/
/*		BaleElement.java						*/
/*										*/
/*	Bubble Annotated Language Editor programming language elements		*/
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
import edu.brown.cs.bubbles.bump.BumpClient;

import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.tree.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



abstract class BaleElement implements Element, MutableAttributeSet, TreeNode, Style, BaleConstants,
	BaleConstants.BaleWindowElement {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleDocument for_document;
private BaleElement.Branch parent_element;
private AttributeSet default_attrs;
private MutableAttributeSet element_attrs;
private Map<Object,Object> attr_cache;
private int num_overlap;
private BaleAstNode	ast_node;
private org.w3c.dom.Element old_hint;
private boolean 	is_elided;

private static boolean	elide_statement = BALE_PROPERTIES.getBoolean("Bale.elide.statement");
private static boolean	elide_body = BALE_PROPERTIES.getBoolean("Bale.elide.body");
private static boolean	elide_members = BALE_PROPERTIES.getBoolean("Bale.elide.members");
private static int	tab_size;

static {
   String v = BALE_PROPERTIES.getProperty("indent.tabluation.size");
   if (v == null) v = BumpClient.getBump().getOption("org.eclipse.jdt.core.formatter.tabulation.size");
   if (v == null) v = BALE_PROPERTIES.getProperty("Bale.tabsize");
   tab_size = 8;
   try {
      if (v != null) tab_size = Integer.parseInt(v);
   }
   catch (NumberFormatException e) { }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleElement(BaleDocument d,BaleElement.Branch parent,AttributeSet a)
{
   for_document = d;
   parent_element = parent;
   default_attrs = a;
   element_attrs = null;
   num_overlap = 0;
   ast_node = null;
   is_elided = false;
   attr_cache = null;
   old_hint = null;

   if (a == null && getName() != null) {
      default_attrs = BaleFactory.getFactory().getAttributes(getName());
    }
}




/********************************************************************************/
/*										*/
/*	BALE specific methods							*/
/*										*/
/********************************************************************************/

BaleDocument getBaleDocument()			{ return for_document; }

BaleAstNode getAstNode()			{ return ast_node; }
void setAstNode(BaleAstNode bn) {
   ast_node = bn; 
   old_hint = null;
}


boolean isEndOfLine()				{ return false; }

BaleElement.Indent getIndent()			{ return null; }
boolean isLineElement() 			{ return false; }
boolean isUnknown()				{ return false; }
boolean isInsideLine()				{ return false; }
boolean isOutsideLine() 			{ return false; }
boolean isEmpty()				{ return false; }
boolean isComment()				{ return false; }
@Override public boolean isIdentifier() 	{ return false; }
@Override public boolean isTypeIdentifier()	{ return false; }
boolean isUndefined()				{ return false; }
BaleFragmentType getBubbleType()		{ return BaleFragmentType.NONE; }
boolean isDeclSet()				{ return false; }
boolean isStatement()				{ return false; }
boolean isOrphan()				{ return false; }
boolean isHint()                                { return false; }
String getHintText()                                { return null; }
org.w3c.dom.Element getOldHintData()            { return old_hint; }
void setOldHintData(org.w3c.dom.Element hint)    { old_hint = hint; }

@Override public BaleElement.Branch getBaleParent()	{ return parent_element; }

BaleElement getBaleElement(int idx)		{ return null; }
BaleTokenState getStartTokenState()		{ return BaleTokenState.NORMAL; }
BaleTokenState getEndTokenState()		{ return BaleTokenState.NORMAL; }
@Override public BaleTokenType getTokenType()	{ return BaleTokenType.NONE; }
BaleElement.Leaf getFirstChild()		{ return null; }
BaleElement.Leaf getLastChild() 		{ return null; }
int getChildIndex(BaleElement e)		{ return -1; }

boolean isElided()				{ return is_elided; }
void setElided(boolean fg)			{ is_elided = fg; }
BaleElideMode getElideMode()			{ return for_document.getElideMode(); }

void setReflowCount(int ct)			{ }
int getReflowCount()				{ return 0; }

void fixParents()				{ }


boolean canElide()
{
   if (isLeaf()) return false;
   if (isInsideLine()) return false;
   if (isLineElement()) return false;
   if (isOutsideLine()) {
      if (getChildCount() > 1) {
	 if (!elide_statement && isStatement()) return false;
	 if (isComment()) return true;
	 if (!elide_body) {
	    BaleElement.Branch par = getBaleParent();
	    if (par == null || par.isDeclSet())
	       return false;
	    BaleElement.Branch gpar = par.getBaleParent();
	    if (gpar == null && for_document.getLanguage() == BoardLanguage.JS) return false;
	  }
	 if (!elide_members) {
	    if (getBubbleType() != BaleFragmentType.NONE) {
	       BaleElement.Branch par = getBaleParent();
	       if (par != null && par.getBubbleType() == BaleFragmentType.CLASS) return false;
	       if (par != null) {
		  BaleElement.Branch gpar = par.getBaleParent();
		  if (gpar != null) gpar = gpar.getBaleParent();
		  if (gpar == null) return false;
		}
	     }
	 }
	 return true;
       }
    }

   return false;
}

String getFullName()
{
   if (ast_node == null) return null;
   return ast_node.getFullName();
}


@Override public BaleElement.Leaf getNextCharacterElement()
{
   BaleElement par = null;
   BaleElement chld = this;
   int idx = -1;

   for ( ; ; ) {
      par = chld.getBaleParent();
      if (par == null) return null;
      idx = par.getChildIndex(chld);
      if (idx+1 < par.getElementCount()) break;
      chld = par;
    }
   if (par == null) return null;
   chld = par.getBaleElement(idx+1);
   return chld.getFirstChild();
}


@Override public BaleElement.Leaf getPreviousCharacterElement()
{
   BaleElement par = null;
   BaleElement chld = this;
   int idx = -1;

   for ( ; ; ) {
      par = chld.getBaleParent();
      if (par == null) return null;
      idx = par.getChildIndex(chld);
      if (idx > 0) break;
      chld = par;
    }
   if (par == null) return null;
   chld = par.getBaleElement(idx-1);
   return chld.getLastChild();
}


BaleViewType getViewType()
{
   if (isLeaf()) {
      if (isOrphan()) return BaleViewType.ORPHAN;
      else if (isHint()) return BaleViewType.HINT;
      return BaleViewType.TEXT;
    }

   if (isInsideLine()) return BaleViewType.CODE;
   if (isLineElement()) return BaleViewType.LINE;
   if (isOutsideLine()) return BaleViewType.BLOCK;
   if (isUnknown()) return BaleViewType.LINE;

   BoardLog.logE("BALE","Unknown view type for " + this);

   return BaleViewType.NONE;
}


int getDocumentStartOffset()
{
   Position p = getStartPosition();
   if (p instanceof BalePosition) return ((BalePosition) p).getDocumentOffset();
   return p.getOffset();
}

int getDocumentEndOffset()
{
   Position p = getEndPosition();
   if (p instanceof BalePosition) return ((BalePosition) p).getDocumentOffset();
   return p.getOffset();
}



BaleElement.Leaf getBaleChildAtPosition(int off)	{ return null; }



double getPriority()
{
   if (ast_node != null) return ast_node.getElidePriority();
   // basic nodes have to be rendered if parent is
   // else if (parent_element != null) return parent_element.getElidePriority();

   return 1;
}


public String getMethodName()
{
   BaleElement be = this;
   // first find containing parent method block
   for ( ; be != null; be = be.getBaleParent()) {
      if (be.getName().equals("Method") || be.getName().equals("DeclSet"))
	 break;
      if (be.getBubbleType() != BaleFragmentType.NONE) return null;
    }
   BaleElement ce = be;
   while (ce != null && !ce.isLeaf()) ce = ce.getBaleElement(0);
   while (ce != null) {
      if (ce.getName().equals("MethodDeclId")) {
	 return ce.getFullName();
       }
      if (ce.getName().equals("Block")) break;
      if (ce.getTokenType() == BaleTokenType.LPAREN) break;
      ce = ce.getNextCharacterElement();
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Attribute set management methods					*/
/*										*/
/********************************************************************************/

@Override public int getAttributeCount()
{
   if (element_attrs == null && default_attrs == null) return 0;
   if (element_attrs == null) return default_attrs.getAttributeCount();
   return element_attrs.getAttributeCount() + default_attrs.getAttributeCount() - num_overlap;
}


@Override public boolean isDefined(Object attr)
{
   if (element_attrs != null && element_attrs.isDefined(attr)) return true;
   if (default_attrs != null && default_attrs.isDefined(attr)) return true;
   return false;
}


@Override public boolean isEqual(AttributeSet a)
{
   if (a.getAttributeCount() != getAttributeCount()) return false;

   for (Enumeration<?> e = a.getAttributeNames(); e.hasMoreElements(); ) {
      Object anm = e.nextElement();
      if (!isDefined(anm)) return false;
    }

   return true;
}


@Override public AttributeSet copyAttributes()
{
   SimpleAttributeSet sas = new SimpleAttributeSet();
   for (Enumeration<?> e = getAttributeNames(); e.hasMoreElements(); ) {
      Object anm = e.nextElement();
      sas.addAttribute(anm,getAttribute(anm));
    }
   return sas;
}


@Override public Enumeration<?> getAttributeNames()
{
   if (element_attrs == null && default_attrs == null) {
      return Collections.enumeration(Collections.emptyList());
    }
   else if (element_attrs == null) return default_attrs.getAttributeNames();
   else if (default_attrs == null) return element_attrs.getAttributeNames();
   else {
      Set<Object> r = new HashSet<>();
      for (Enumeration<?> e = element_attrs.getAttributeNames(); e.hasMoreElements(); )
	 r.add(e.nextElement());
      for (Enumeration<?> e = default_attrs.getAttributeNames(); e.hasMoreElements(); )
	 r.add(e.nextElement());
      return Collections.enumeration(r);
    }
}


@Override public boolean containsAttribute(Object n,Object v)
{
   if (element_attrs != null && element_attrs.isDefined(n)) {
      return element_attrs.containsAttribute(n,v);
    }
   if (default_attrs != null) return default_attrs.containsAttribute(n,v);

   return false;
}


@Override public boolean containsAttributes(AttributeSet a)
{
   for (Enumeration<?> e = a.getAttributeNames(); e.hasMoreElements(); ) {
      Object nm = e.nextElement();
      Object vl = a.getAttribute(nm);
      if (!containsAttribute(nm,vl)) return false;
    }

   return true;
}


@Override public Object getAttribute(Object attr)
{
   Object v = null;
   if (element_attrs != null) v = element_attrs.getAttribute(attr);

   if (v == null && default_attrs != null) v = default_attrs.getAttribute(attr);

   if (v == null && parent_element != null) {
      if (inCache(attr)) return getFromCache(attr);
      AttributeSet a = parent_element.getAttributes();
      v = a.getAttribute(attr);
      addToCache(attr,v);
    }

   return v;
}


@Override public AttributeSet getResolveParent()
{
   AttributeSet a = null;

   if (element_attrs != null) a = element_attrs.getResolveParent();
   if (a == null && default_attrs != null) a = default_attrs.getResolveParent();
   if (a == null && parent_element != null) a = parent_element.getAttributes();

   return a;
}


@Override public void addAttribute(Object name,Object value)
{
   setupElementAttributes();
   element_attrs.addAttribute(name,value);
   if (getAllowsChildren()) clearCache();
}


@Override public void addAttributes(AttributeSet a)
{
   setupElementAttributes();
   element_attrs.addAttributes(a);
   if (getAllowsChildren()) clearCache();
}


@Override public void removeAttribute(Object name)
{
   if (element_attrs != null) element_attrs.removeAttribute(name);
}


@Override public void removeAttributes(Enumeration<?> names)
{
   if (element_attrs != null) element_attrs.removeAttributes(names);
}


@Override public void removeAttributes(AttributeSet a)
{
   if (element_attrs != null) element_attrs.removeAttributes(a);
}


@Override public void setResolveParent(AttributeSet par)
{
   setupElementAttributes();
   element_attrs.setResolveParent(par);
}



private synchronized void setupElementAttributes()
{
   if (element_attrs == null) {
      element_attrs = new SimpleAttributeSet();
    }
}



/********************************************************************************/
/*										*/
/*	Attribute caching methods						*/
/*										*/
/********************************************************************************/

void addToCache(Object k,Object v)
{
   if (attr_cache == null) {
      attr_cache = new HashMap<>();
    }
   attr_cache.put(k,v);
}


boolean inCache(Object k)
{
   if (attr_cache == null) return false;

   return attr_cache.containsKey(k);
}


Object getFromCache(Object k)
{
   return attr_cache.get(k);
}


void clearCache()
{
   attr_cache = null;
   int nch = getChildCount();
   for (int i = 0; i < nch; ++i) {
      getBaleElement(i).clearCache();
    }
}



void clearLocalCache()
{
   attr_cache = null;
}




/********************************************************************************/
/*										*/
/*	Element methods 							*/
/*										*/
/********************************************************************************/

@Override public Document getDocument() 	{ return for_document; }

@Override public Element getParentElement()	{ return parent_element; }

@Override public AttributeSet getAttributes()	{ return this; }

@Override public String getName()		{ return null; }

@Override public int getStartOffset()
{
   Position p = getStartPosition();
   if (p == null) return -1;
   return p.getOffset();
}

@Override public int getEndOffset()
{
   Position p = getEndPosition();
   if (p == null) return -1;
   return p.getOffset();
}

@Override public abstract Element getElement(int idx);
@Override public abstract int getElementCount();
@Override public abstract int getElementIndex(int off);
@Override public abstract boolean isLeaf();

abstract Position getStartPosition();
abstract Position getEndPosition();




/********************************************************************************/
/*										*/
/*	TreeNode interface							*/
/*										*/
/********************************************************************************/

@Override public TreeNode getChildAt(int idx)	{ return (TreeNode) getElement(idx); }

@Override public int getChildCount()		{ return getElementCount(); }

@Override public TreeNode getParent()		{ return (TreeNode) getParentElement(); }

@Override public int getIndex(TreeNode n)
{
   for (int i = getChildCount() - 1; i >= 0; --i) {
      if (getChildAt(i) == n) return i;
    }
   return -1;
}

@Override public boolean getAllowsChildren()	{ return !isLeaf(); }

@Override public abstract Enumeration<? extends TreeNode> children();



/********************************************************************************/
/*										*/
/*	Style methods								*/
/*										*/
/********************************************************************************/

@Override public void addChangeListener(ChangeListener l)	{ }

@Override public void removeChangeListener(ChangeListener l)	{ }




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();

   dumpElement(buf,0);

   return buf.toString();
}


private void dumpElement(StringBuffer buf,int indent)
{
   for (int i = 0; i < indent; ++i) buf.append("  ");

   buf.append(getName());
   if (!isLeaf() && getElementCount() == 0) {
      buf.append("[?]\n");
      return;
    }
   if (getTokenType() != null && getTokenType() != BaleTokenType.NONE) {
      buf.append(" (" + getTokenType().toString() + ")");
    }

   buf.append(" [" + getDocumentStartOffset() + ":" + getDocumentEndOffset());
   buf.append(";" + getStartOffset() + ":" + getEndOffset() + "]");
   if (ast_node != null) {
      if (parent_element == null || parent_element.getAstNode() != ast_node) {
	 buf.append(" {" + ast_node.getElidePriority());
	 buf.append("," + ast_node.getNodeType());
	 buf.append("," + ast_node.getIdType());
	 if (ast_node.getFullName() != null) buf.append(",NAME=" + ast_node.getFullName());
	 if (ast_node.getErrorFlag()) buf.append(",ERROR");
	 buf.append("}");
       }
    }

   buf.append("\n");
   for (int i = 0; i < getElementCount(); ++i) {
      BaleElement be = (BaleElement) getElement(i);
      be.dumpElement(buf,indent+1);
    }
}




/********************************************************************************/
/*										*/
/*	Branch Elements 							*/
/*										*/
/********************************************************************************/

static class Branch extends BaleElement {

   private BaleElement [] children_elts;
   private int num_children;
   private int last_index;
   private BaleTokenState start_state;
   private BaleTokenState end_state;
   private Position end_pos;
   private Position start_pos;

   Branch(BaleDocument d,BaleElement.Branch par,AttributeSet a) {
      super(d,par,a);
      children_elts = new BaleElement[1];
      num_children = 0;
      last_index = -1;
      start_state = BaleTokenState.NORMAL;
      end_state = BaleTokenState.NORMAL;
      start_pos = null;
      end_pos = null;
    }

   @Override BaleTokenState getStartTokenState()	{ return start_state; }
   @Override BaleTokenState getEndTokenState()		{ return end_state; }
   void setStartTokenState(BaleTokenState s)		{ start_state = s; }
   void setEndTokenState(BaleTokenState s)		{ end_state = s; }

   @Override public BaleTokenType getTokenType() {
      if (isElided()) return BaleTokenType.ELIDED;
      return super.getTokenType();
    }

   void replace(BaleElement old,BaleElement rep) {
      checkCycle(rep);
      for (int i = 0; i < num_children; ++i) {
	 if (children_elts[i] == old) {
	    children_elts[i] = rep;
	    rep.parent_element = this;
	    old.parent_element = null;
	    return;
	  }
       }
      if (old != null) old.clearCache();
      if (rep != null) rep.clearCache();
      clearChildPositions();
    }

   void replace(BaleElement old,List<BaleElement> rep) {
      int sz = rep.size();
      int nsz = children_elts.length;
      while (sz - 1 + num_children >= nsz) nsz *= 2;
      if (nsz != children_elts.length) {
         children_elts = Arrays.copyOf(children_elts,nsz);
       }
      int idx = -1;
      for (int i = 0; i < num_children; ++i) {
         if (children_elts[i] == old) {
            idx = i;
            break;
          }
       }
      if (idx < 0) return;
      System.arraycopy(children_elts,idx,children_elts,idx+sz-1,num_children-idx);
      for (BaleElement be : rep) {
         checkCycle(be);
         children_elts[idx++] = be;
         be.parent_element = this;
         be.clearCache();
       }
      old.parent_element = null;
      num_children += sz-1;
      old.clearCache();
      clearChildPositions();
    }

   void remove(int frm,int to) {
      int nsz = to - frm + 1;
      for (int i = frm; i < num_children - nsz; ++i) {
	 children_elts[i] = children_elts[i+nsz];
       }
      for (int i = num_children-nsz; i < num_children; ++i) children_elts[i] = null;
      num_children -= nsz;
    }

   void clear() {
      for (int i = 0; i < num_children; ++i) children_elts[i] = null;
      num_children = 0;
    }

   void add(BaleElement e) {
      checkCycle(e);
      if (num_children >= children_elts.length) {
	 children_elts = Arrays.copyOf(children_elts,children_elts.length*2);
       }
      children_elts[num_children++] = e;
      e.parent_element = this;
      e.clearCache();
      clearChildPositions();
    }

   @Override protected Position getStartPosition() {
      if (num_children == 0) return null;
      if (start_pos == null) {
         BaleElement c = children_elts[0];
         start_pos = c.getStartPosition();
       }
      return start_pos;
    }

   @Override protected Position getEndPosition() {
      if (end_pos == null) {
	 BaleElement c;
	 if (num_children > 0) c = children_elts[num_children-1];
	 else c = children_elts[0];
	 if (c == null) return null;
	 end_pos = c.getEndPosition();
       }
      return end_pos;
    }

   void setChildPosition(Position spos,Position epos,BaleElement c) {
      boolean rpt = false;
      if (num_children > 0 && c == children_elts[num_children-1]) {
	 end_pos = epos;
	 rpt = true;
       }
      if (num_children > 0 && c == children_elts[0]) {
	 start_pos = spos;
	 rpt = true;
       }
      if (rpt && getBaleParent() != null) getBaleParent().setChildPosition(start_pos,end_pos,this);
    }

   private void clearChildPositions() {
      start_pos = null;
      end_pos = null;
      if (getBaleParent() != null) getBaleParent().clearChildPositions();
    }

   @Override public Element getElement(int idx)  { return (idx < num_children ? children_elts[idx] : null); }
   @Override BaleElement getBaleElement(int idx) { return (idx < num_children ? children_elts[idx] : null); }

   @Override int getChildIndex(BaleElement c) {
      for (int i = 0; i < num_children; ++i) {
	 if (children_elts[i] == c) return i;
       }
      return -1;
    }

   @Override BaleElement.Leaf getBaleChildAtPosition(int pos) {
      int idx = getElementIndex(pos);
      BaleElement be = getBaleElement(idx);
      if (be == null) {
	 BoardLog.logE("BALE","Problem getting child for position " + pos + " " + idx);
	 return null;
       }
      return be.getBaleChildAtPosition(pos);
    }

   @Override public int getElementCount()	{ return num_children; }

   @Override public int getElementIndex(int off) {
      int index;
      int lower = 0;
      int upper = num_children - 1;
      int mid = 0;
      int p0 = getStartOffset();
      int p1 = getEndOffset();

      if (num_children == 0) return 0;
      if (off >= p1) return num_children - 1;

      if (last_index >= lower && last_index <= upper) {
	 Element last = children_elts[last_index];
	 p0 = last.getStartOffset();
	 p1 = last.getEndOffset();
	 if (off >= p0 && off < p1) return last_index;
	 if (off < p0) upper = last_index;
	 else lower = last_index;
       }

      while (lower <= upper) {
	 mid = lower + ((upper - lower) / 2);
	 Element elem = children_elts[mid];
	 p0 = elem.getStartOffset();
	 p1 = elem.getEndOffset();
	 if (off >= p0 && off < p1) {
	    index = mid;
	    last_index = index;
	    return index;
	  }
	 else if (off < p0) upper = mid-1;
	 else lower = mid+1;
       }

      if (off < p0) index = mid;
      else index = mid+1;
      last_index = index;
      return index;
   }

   @Override public boolean isLeaf()		{ return false; }

   @Override public Enumeration<BaleElement> children() {
      if (num_children == 0) return null;
      ArrayList<BaleElement> l = new ArrayList<>(num_children);
      for (int i = 0; i < num_children; ++i) l.add(children_elts[i]);
      return Collections.enumeration(l);
    }

   @Override BaleElement.Leaf getFirstChild() {
      for (BaleElement.Branch bb = this; bb != null; ) {
	 if (bb.num_children == 0) return null;
	 BaleElement be = bb.children_elts[0];
	 if (be.isLeaf()) return (BaleElement.Leaf) be;
	 bb = (BaleElement.Branch) be;
       }
      return null;
    }

   @Override BaleElement.Leaf getLastChild() {
      for (BaleElement.Branch bb = this; bb != null; ) {
	 if (bb.num_children == 0) return null;
	 BaleElement be = bb.children_elts[bb.num_children-1];
	 if (be.isLeaf()) return (BaleElement.Leaf) be;
	 bb = (BaleElement.Branch) be;
       }
      return null;
    }

   private void checkCycle(BaleElement chld) {
      for (BaleElement be = this; be != null; be = be.getBaleParent()) {
	 if (be == chld)
	    throw new IllegalArgumentException("Circular position request for " + be);
       }
    }

  @Override void fixParents() {
     for (int i = 0; i < num_children; ++i) {
	BaleElement ce = children_elts[i];
	ce.parent_element = this;
	ce.fixParents();
      }
     clearLocalCache();
     clearChildPositions();
   }

}	// end of inner class Branch





/********************************************************************************/
/*										*/
/*	Leaf Elements								*/
/*										*/
/********************************************************************************/

static class Leaf extends BaleElement {

   private Position start_pos;
   private Position end_pos;
   private BaleTokenType token_type;

   Leaf(BaleDocument d,BaleElement.Branch p,AttributeSet a,int off0,int off1) {
      this(d,p,a,off0,off1,BaleTokenType.NONE);
    }

   Leaf(BaleDocument d,BaleElement.Branch p,AttributeSet a,int off0,int off1, BaleTokenType tt) {
      super(d,p,a);
      token_type = tt;
      setPosition(off0,off1);
    }

   @Override protected Position getStartPosition()		{ return start_pos; }
   @Override protected Position getEndPosition()		{ return end_pos; }
   @Override public int getElementIndex(int pos)		{ return -1; }
   @Override public Element getElement(int idx) 		{ return null; }
   @Override public int getElementCount()			{ return 0; }
   @Override public boolean isLeaf()				{ return true; }
   @Override public Enumeration<? extends TreeNode> children()	{ return null; }

   @Override BaleElement.Leaf getBaleChildAtPosition(int off)	{ return this; }
   @Override public BaleTokenType getTokenType()		{ return token_type; }
   @Override BaleElement.Leaf getFirstChild()			{ return this; }
   @Override BaleElement.Leaf getLastChild()			{ return this; }

   void setPosition(int off0,int off1) {
      BaleDocument bd = getBaleDocument();
      try {
         start_pos = bd.createPosition(off0);
         end_pos = bd.createPosition(off1);
         if (start_pos.getOffset() > end_pos.getOffset()) {
            BoardLog.logE("BALE","Bad token: end is less than start");
            start_pos = bd.createPosition(off0);
            end_pos = bd.createPosition(off1);
            int p0 = start_pos.getOffset();
            int p1 = end_pos.getOffset();
            BoardLog.logX("BALE","Bad token: end less than start: " + p0 + " " + p1 + " " +
        		     token_type + " " +
        		     getName() + " " + bd.getFragmentName() + " " + bd.getFile() + " " +
        		     off0 + " " + off1);
   
            end_pos = start_pos;
          }
         if (getBaleParent() != null) getBaleParent().setChildPosition(start_pos,end_pos,this);
       }
      catch (BadLocationException e) {
         start_pos = end_pos = null;
         throw new Error("Can't create position references for leaf element " + start_pos + " " + end_pos);
       }
    }


}	// end of inner class Leaf




/********************************************************************************/
/*										*/
/*	Specialized Branch Elements						*/
/*										*/
/********************************************************************************/

static class DeclSet extends Branch {

   DeclSet(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "DeclSet"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override boolean isDeclSet()		{ return true; }

}	// end of inner class DeclSet



static class CompilationUnitNode extends Branch {

   CompilationUnitNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "CompilationUnit"; }
   @Override boolean isOutsideLine()		{ return true; }

}	// end of inner class CompilationUnitNode



static class ClassNode extends Branch {

   ClassNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Class"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override BaleFragmentType getBubbleType()	{ return BaleFragmentType.CLASS; }

}	// end of inner class ClassNode



static class MethodNode extends Branch {

   MethodNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Method"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override BaleFragmentType getBubbleType()	{ return BaleFragmentType.METHOD; }

}	// end of inner class MethodNode



static class FieldNode extends Branch {

   FieldNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Field"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override BaleFragmentType getBubbleType()	{ return BaleFragmentType.FIELDS; }

}	// end of inner class FieldNode


static class VariableNode extends Branch {

   // JavaScript variable declaration
   VariableNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }
   
   @Override public String getName()		{ return "Variable"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override BaleFragmentType getBubbleType()	{ return BaleFragmentType.FIELDS; }

}	// end of inner class FieldNode



static class AnnotationNode extends Branch {

   AnnotationNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Annotation"; }
   @Override boolean isOutsideLine()		{ return true; }

}	// end of inner class AnnotationNode




static class InitializerNode extends Branch {

   InitializerNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Initializer"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override BaleFragmentType getBubbleType()	{ return BaleFragmentType.STATICS; }

}	// end of inner class InitializerNode



static class LineNode extends Branch {

   private int reflow_count;

   LineNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
      reflow_count = 0;
    }

   @Override public String getName()			{ return "Line"; }

   @Override boolean isLineElement()			{ return true; }
   @Override boolean isEmpty() {
      int n = getElementCount();
      for (int i = 0; i < n; ++i) {
	 BaleElement be = getBaleElement(i);
	 if (!be.isEmpty()) return false;
       }
      return true;
    }

   @Override void setReflowCount(int ct)		{ reflow_count = ct; }
   @Override int getReflowCount()			{ return reflow_count; }

}	// end of inner class LineNode



static class StatementNode extends Branch {

   StatementNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Statement"; }
   @Override boolean isInsideLine()		{ return true; }

}	// end of inner class StatementNode



static class SplitStatementNode extends Branch {

   SplitStatementNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "SplitStatement"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override boolean isStatement()		{ return true; }

}	// end of inner class StatementNode



static class ExpressionNode extends Branch {

   ExpressionNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Expression"; }
   @Override boolean isInsideLine()		{ return true; }

}	// end of inner class ExpressionNode



static class SplitExpressionNode extends Branch {

   SplitExpressionNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "SplitExpression"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override boolean isStatement()		{ return true; }

}	// end of inner class ExpressionNode



static class BlockNode extends Branch {

   BlockNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "Block"; }
   @Override boolean isOutsideLine()		{ return true; }

}	// end of inner class BlockNode



static class SwitchBlockNode extends Branch {

   SwitchBlockNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "SwitchBlock"; }
   @Override boolean isOutsideLine()		{ return true; }

}	// end of inner class BlockNode



static class BlockCommentNode extends Branch {

   BlockCommentNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "BlockComment"; }
   @Override boolean isOutsideLine()		{ return true; }
   @Override boolean isComment()		{ return true; }

}	// end of inner class BlockNode



static class InternalBlockNode extends Branch {

   InternalBlockNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
    }

   @Override public String getName()		{ return "InternalBlock"; }
   @Override boolean isInsideLine()		{ return true; }

}	// end of inner class BlockNode



static class UnknownNode extends Branch {

   private int reflow_count;
   boolean has_eol;

   UnknownNode(BaleDocument d,BaleElement.Branch par) {
      super(d,par,null);
      has_eol = false;
      reflow_count = 0;
    }

   void setEndOfLine()				{ has_eol = true; }

   @Override public String getName()		{ return "Unknown"; }
   @Override boolean isEndOfLine()		{ return has_eol; }
   @Override boolean isUnknown()		{ return true; }
   @Override void setReflowCount(int ct)	{ reflow_count = ct; }
   @Override int getReflowCount()		{ return reflow_count; }

}	// end of inner class Unknown




/********************************************************************************/
/*										*/
/*	Specialized Leaf Elements						*/
/*										*/
/********************************************************************************/

static class Token extends Leaf {

   Token(BaleDocument d,BaleElement.Branch p,int offs,int offe,BaleTokenType tt) {
      super(d,p,null,offs,offe,tt);
    }

   @Override public String getName()		{ return "Token"; }
   @Override boolean isInsideLine()		{ return true; }

}	// end of inner class Token



static class Brace extends Leaf {

   Brace(BaleDocument d,BaleElement.Branch p,int offs,int offe,BaleTokenType tt) {
      super(d,p,null,offs,offe,tt);
    }

   @Override public String getName()		{ return "Brace"; }
   @Override boolean isInsideLine()		{ return true; }

}	// end of inner class Brace



static class Keyword extends Leaf {

   Keyword(BaleDocument d,BaleElement.Branch p,int offs,int offe,BaleTokenType tt) {
      super(d,p,null,offs,offe,tt);
    }

   @Override public String getName()		{ return "Keyword"; }

}	// end of inner class Keyword



static class Return extends Leaf {

   Return(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,null,offs,offe,BaleTokenType.RETURN);
    }

   @Override public String getName()		{ return "Return"; }

}	// end of inner class Return



static class Identifier extends Leaf {

   Identifier(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,null,offs,offe,BaleTokenType.IDENTIFIER);
    }

   @Override public String getName()		{ return "Identifier"; }
   @Override public boolean isIdentifier()	{ return true; }

}	// end of inner class Identifier



static class FieldId extends Identifier {

   FieldId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "FieldId"; }

}	// end of inner class FieldId



static class StaticFieldId extends Identifier {

   StaticFieldId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "StaticFieldId"; }

}	// end of inner class StaticFieldId



static class ClassDeclId extends Identifier {

   ClassDeclId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "ClassDeclId"; }

}	// end of inner class ClassDeclId



static class ClassDeclMemberId extends Identifier {

   ClassDeclMemberId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "ClassDeclMemberId"; }

}	// end of inner class ClassDeclMemberId



static class MethodDeclId extends Identifier {

   MethodDeclId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "MethodDeclId"; }

}	// end of inner class MethodDeclId



static class LocalDeclId extends Identifier {

   LocalDeclId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "LocalDeclId"; }

}	// end of inner class LocalDeclId




static class FieldDeclId extends Identifier {

   FieldDeclId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "FieldDeclId"; }

}	// end of inner class FieldDeclId



static class CallId extends Identifier {

   CallId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "CallId"; }

}	// end of inner class CallId



static class StaticCallId extends Identifier {

   StaticCallId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "StaticCallId"; }

}	// end of inner class StaticCallId



static class UndefCallId extends Identifier {

   UndefCallId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "UndefCallId"; }

   @Override boolean isUndefined()		{ return true; }

}	// end of inner class UndefCallId



static class AnnotationId extends Identifier {

   AnnotationId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "AnnotationId"; }

}	// end of inner class StaticCallId



static class BuiltinId extends Identifier {

   BuiltinId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "BuiltinId"; }

   @Override boolean isUndefined()		{ return true; }

}	// end of inner class BuiltinId



static class UndefId extends Identifier {

   UndefId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "UndefId"; }

   @Override boolean isUndefined()		{ return true; }

}	// end of inner class UndefId



static class DeprecatedCallId extends Identifier {

   DeprecatedCallId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "DeprecatedCallId"; }

}	// end of inner class DeprecatedCallId



static class TypeId extends Identifier {

   TypeId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "TypeId"; }
   @Override public boolean isTypeIdentifier()	{ return true; }

}	// end of inner class TypeId



static class ConstId extends Identifier {

   ConstId(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
    }

   @Override public String getName()		{ return "ConstId"; }

}	// end of inner class TypeId



static class Number extends Leaf {

   Number(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,null,offs,offe,BaleTokenType.NUMBER);
    }

   @Override public String getName()		{ return "Number"; }

}	// end of inner class Number



static class Literal extends Leaf {

   private boolean end_line;
   
   Literal(BaleDocument d,BaleElement.Branch p,int offs,int offe,
         boolean eol,BaleTokenType tt) {
      super(d,p,null,offs,offe,tt);
      end_line = eol;
    }

   @Override public String getName()		{ return "Literal"; }
   @Override boolean isEndOfLine()		{ return end_line; }
   @Override BaleTokenState getEndTokenState() {
      return (end_line ? BaleTokenState.IN_MULTILINE_STRING : BaleTokenState.NORMAL);
    }
   
}	// end of inner class Literal



static class Space extends Leaf {

   Space(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,null,offs,offe,BaleTokenType.SPACE);
    }

   @Override public String getName()		{ return "Space"; }

   @Override boolean isEmpty()			{ return true; }

}	// end of inner class Space




static class Indent extends Space {

   private int first_column;
   private int num_exdent;

   Indent(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,offs,offe);
      num_exdent = EXDENT_UNKNOWN;
      first_column = -1;
      try {
	 String txt = d.getText(offs,offe-offs);
	 int c = 0;
	 for (int i = 0; i < txt.length(); ++i) {
	    char ch = txt.charAt(i);
	    if (ch == ' ') ++c;
	    else if (ch == '\t') c = (c+tab_size)/tab_size*tab_size;
	    else if (Character.isWhitespace(c)) ;
	    else break;
	  }
	 first_column = c+1;
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem looking at indent text",e);
       }
    }

   @Override public String getName()		{ return "Indent"; }

   @Override Indent getIndent() 		{ return this; }

   public int getFirstColumn()			{ return first_column; }

   public int getNumExdent()			{ return num_exdent; }
   public void setNumExdent(int fg)		{ num_exdent = fg; }

}	// end of inner class Space




static class Eol extends Leaf {

   Eol(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,null,offs,offe,BaleTokenType.EOL);
    }

   @Override public String getName()		{ return "EOL"; }

   @Override boolean isEndOfLine()		{ return true; }
   @Override boolean isEmpty()			{ return true; }

}	// end of inner class Eol




static class Comment extends Leaf {

   private boolean end_line;

   Comment(BaleDocument d,BaleElement.Branch p,int offs,int offe,boolean eol,BaleTokenType tt) {
      super(d,p,null,offs,offe,tt);
      end_line = eol;
    }

   @Override public String getName()		{ return "Comment"; }
   @Override boolean isEndOfLine()		{ return end_line; }
   @Override boolean isComment()		{ return true; }
   @Override BaleTokenState getEndTokenState() {
      return (end_line ? BaleTokenState.IN_COMMENT : BaleTokenState.NORMAL);
    }

}	// end of inner class Comment



static class JavaDocComment extends Comment {

   JavaDocComment(BaleDocument d,BaleElement.Branch p,int offs,int offe,boolean eol,
		     BaleTokenType tt) {
      super(d,p,offs,offe,eol,tt);
    }

   @Override public String getName()		{ return "JavaDocComment"; }
   @Override BaleTokenState getEndTokenState() {
      return (isEndOfLine() ? BaleTokenState.IN_FORMAL_COMMENT : BaleTokenState.NORMAL);
    }

}	// end of inner class JavaDocComment



static class LineComment extends Leaf {

   LineComment(BaleDocument d,BaleElement.Branch p,int offs,int offe) {
      super(d,p,null,offs,offe,BaleTokenType.LINECOMMENT);
    }

   @Override public String getName()		{ return "LineComment"; }
   @Override boolean isEndOfLine()		{ return false; }
   @Override boolean isComment()		{ return true; }

}	// end of inner class LineComment



/********************************************************************************/
/*										*/
/*	Special elements for orphaned windows					*/
/*										*/
/********************************************************************************/

static class OrphanElement extends Leaf {

   // private String fragment_name;
   
   OrphanElement(BaleDocument d,BaleElement.Branch p,String nm) {
      super(d,p,null,0,1);
      // fragment_name = nm;
    }

   @Override public String getName()		{ return "Orphan"; }
   @Override boolean isOrphan() 		{ return true; }

}	// end of inner class OrphanElement




static class HintElement extends Leaf {
   
   private String hint_text;
   
   HintElement(BaleElement base,String hint,int off) {
      super(base.getBaleDocument(),base.getBaleParent(),null,off,off);
      hint_text = hint;
    }
   
   @Override public String getName()            { return "Hint"; }
   @Override boolean isComment()                { return true; }
   @Override boolean isHint()                   { return true; }
   @Override String getHintText()                   { return hint_text; }

}



}	// end of interface BaleElement




/* end of BaleElement.java */
