/********************************************************************************/
/*										*/
/*		BassTreeModelBase.java						*/
/*										*/
/*	Bubble Augmented Search Strategies tree model for a repository		*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bass.BassTreeModel.BassTreeBase;
import edu.brown.cs.bubbles.bass.BassTreeModel.BassTreeNode;
import edu.brown.cs.bubbles.bass.BassTreeModel.BassTreeUpdateEvent;
import edu.brown.cs.bubbles.bass.BassTreeModel.BassTreeUpdateListener;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardThreadPool;

import edu.brown.cs.ivy.swing.SwingEventListenerList;

import javax.swing.Icon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


class BassTreeModelBase implements BassConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BassRepository	for_repository;
private Branch		root_node;
private int		leaf_count;
private int		max_childcount;
private ReadWriteLock	tree_lock;
private SwingEventListenerList<BassTreeUpdateListener> listener_set;
private Rebuilder	cur_rebuilder;

private static long REBUILD_DELAY = bass_properties.getLong("Bass.rebuild.delay",500);



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassTreeModelBase(BassRepository br)
{
   for_repository = br;

   root_node = new Branch("ALL",null);

   leaf_count = 0;
   max_childcount = 0;
   tree_lock = new ReentrantReadWriteLock();
   listener_set = new SwingEventListenerList<>(BassTreeUpdateListener.class);
   cur_rebuilder = null;

   setupInitial();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BassTreeBase getRoot()				{ return root_node; }

int getLeafCount()				{ return leaf_count; }
int getMaxChildCount()				{ return max_childcount; }

void addUpdateListener(BassTreeUpdateListener ul)
{
   listener_set.add(ul);
}

void removeUpdateListener(BassTreeUpdateListener ul)
{
   listener_set.remove(ul);
}



void readLock()
{
   Lock lr = tree_lock.readLock();
   lr.lock();
}

void readUnlock()
{
   Lock lr = tree_lock.readLock();
   lr.unlock();
}


void writeLock()
{
   Lock lw = tree_lock.writeLock();
   lw.lock();
}


void writeUnlock()
{
   Lock lw = tree_lock.writeLock();
   lw.unlock();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupInitial()
{
   List<BassName> items = new ArrayList<BassName>();
   for (BassName bn : for_repository.getAllNames()) items.add(bn);
   Collections.sort(items,new TreeSorter());

   TreeLeaf last = null;
   for (BassName nm : items) {
      last = insertNode(nm,last);
    }

   root_node.collapseSingletons();
   max_childcount = root_node.getMaxCount();
}



private TreeLeaf insertNode(BassName nm,TreeLeaf last)
{
   String [] comps = nm.getNameComponents();
   int ncomp = comps.length;

   int idx = ncomp-2;			// ignore actual name

   Branch p = null;

   if (last == null) idx = -1;
   if (idx >= 0 && last != null) {
      String plast = last.getBassName().getProject();
      String pthis = nm.getProject();
      if (plast == null && pthis == null) ;
      else if (plast == null || pthis == null) idx = -1;
      else if (!plast.equals(pthis)) idx = -1;
    }
   if (idx >= 0 && last != null) {
      p = last.getBassParent();
      for ( ; ; ) {
	 if (p.getLocalName().equals(comps[idx])) break;
	 --idx;
	 if (idx < 0) break;
	 p = p.getBassParent();
	 if (p == null) break;
       }
    }
   if (idx > 0 && p != null) {
      Branch q = p.getBassParent();
      for (int nidx = idx-1; nidx >= 0; --nidx) {
	 if (q == null || !q.getLocalName().equals(comps[nidx])) {
	    idx = -1;
	    break;
	 }
	 q = q.getBassParent();
      }
   }
   if (idx < 0 || p == null) {
      p = root_node;
      idx = -1;
    }

   for (int i = idx+1; i < ncomp-1; ++i) {
      int n = p.getChildCount();
      Branch cn = null;
      if (n > 0) {
	 BassTreeImpl bti = p.getChildAt(n-1);
	 if (!bti.isLeaf() && bti.getLocalName().equals(comps[i])) {
	    cn = (Branch) bti;
	  }
       }
      if (cn == null) {
	 cn = new Branch(comps[i],p);
	 p.addChild(cn);
       }
      p = cn;
    }
   if (ncomp == 1 && nm.getNameType() == BassNameType.PROJECT) {
      Branch cn = new Branch(comps[0],p);
      p.addChild(cn);
      return null;
    }

   computeBranchType(nm,p);

   TreeLeaf tl = new TreeLeaf(nm,p);
   p.addChild(tl);
   ++leaf_count;

   return tl;
}



private static void computeBranchType(BassName nm,Branch p)
{
   switch (nm.getNameType()) {
      case INTERFACE :
	 p.setBranchType(BranchNodeType.INTERFACE);
	 break;
      case ENUM :
	 p.setBranchType(BranchNodeType.ENUM);
	 break;
      case THROWABLE :
	 p.setBranchType(BranchNodeType.THROWABLE);
	 break;
      case PACKAGE :
	 p.setBranchType(BranchNodeType.PACKAGE);
	 break;
      case PROJECT :
	 p.setBranchType(BranchNodeType.PROJECT);
	 break;
      case METHOD :
         p.setBranchType(BranchNodeType.METHOD);
         break;
      case ANNOTATION :
         p.setBranchType(BranchNodeType.ANNOTATION);
         break;
      default :
	 p.setBranchType(BranchNodeType.CLASS);
	 break;
    }
}



private static class TreeSorter implements Comparator<BassName> {

   @Override public int compare(BassName b1,BassName b2) {
      String b1pfx = b1.getProject();
      String b1sfx = b1.getNameHead();
      if (b1pfx != null && b1sfx != null) b1pfx += ":" + b1sfx;
      else if (b1sfx != null) b1pfx = b1sfx;
      String b2pfx = b2.getProject();
      String b2sfx = b2.getNameHead();
      if (b2pfx != null && b2sfx != null) b2pfx += ":" + b2sfx;
      else if (b2sfx != null) b2pfx = b2sfx;
      if (b1pfx == null && b2pfx != null) return -1;
      if (b1pfx != null && b2pfx == null) return 1;
      else if (b1pfx != null && b2pfx != null) {
	 int pd = b1pfx.compareTo(b2pfx);
	 if (pd != 0) return pd;
      }

      int d = b1.getSortPriority() - b2.getSortPriority();
      if (d != 0) return d;

      return b1.getFullName().compareTo(b2.getFullName());
    }

}	// end of inner class TreeSorter




/********************************************************************************/
/*										*/
/*	Rebuild request methods 						*/
/*										*/
/********************************************************************************/



void requestRebuild()
{
   synchronized (this) {
      if (cur_rebuilder != null) {
	 if (cur_rebuilder.requestRebuild()) return;
       }
      cur_rebuilder = new Rebuilder();
      BoardThreadPool.start(cur_rebuilder);
    }
}



private class Rebuilder implements Runnable {

   private long 	start_time;
   private long 	begin_time;
   private boolean	is_active;

   Rebuilder() {
      start_time = System.currentTimeMillis() + REBUILD_DELAY;
      BoardLog.logD("BASS","NEW REBUILD " + start_time);
      begin_time = 0;
      is_active = true;
    }

   synchronized boolean requestRebuild() {
      if (!is_active) return false;
      start_time = System.currentTimeMillis() + REBUILD_DELAY;
      BoardLog.logD("BASS","REQUEST REBUILD " + start_time);
      return true;
    }

   @Override public void run() {
      for ( ; ; ) {
	 synchronized (this) {
	    for ( ; ; ) {
	       long delta = start_time - System.currentTimeMillis();
	       if (delta <= 0) break;
	       try {
		  wait(delta);
		}
	       catch (InterruptedException e) { }
	     }
	    begin_time = start_time;
	  }
	 BoardLog.logD("BASS","RUN REBUILD " + begin_time + " " + System.currentTimeMillis());

	 rebuild();

	 BoardLog.logD("BASS","DONE REBUILD " + begin_time + " " + System.currentTimeMillis());
	 
	 synchronized (this) {
	    if (begin_time == start_time) {
	       is_active = false;
	       break;
	     }
	  }
       }
    }

}	// end of inner class Rebuilder





/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void rebuild()
{
   Set<BassName> dels = new HashSet<BassName>();
   Set<BassName> adds = new HashSet<BassName>();

   writeLock();
   try {
      root_node.addAllNames(dels);

      for (BassName ba : for_repository.getAllNames()) {
	 if (dels.remove(ba)) continue;
	 adds.add(ba);
       }
      for (BassName ba : dels) {
	 removeNode(ba);
       }
      for (BassName ba : adds) {
	 root_node.addNode(ba,true);
       }

      max_childcount = root_node.getMaxCount();

      // see if this works - doesn't
      root_node.collapseSingletons();
      root_node.removeEmptyNodes();
      
      UpdateEvent evt = new UpdateEvent(adds,dels);
      for (BassTreeUpdateListener ul : listener_set) {
	 ul.handleTreeUpdated(evt);
       }
    }
   finally { writeUnlock(); }
}





private void removeNode(BassName nm)
{
   BassTreeImpl nd = root_node.addNode(nm,false);

   if (nd == null) return;

   Branch b = nd.getBassParent();

   b.removeNode(nm);
   --leaf_count;
}



private static class UpdateEvent implements BassTreeUpdateEvent {

   private Collection<BassName> names_removed;
   private Collection<BassName> names_added;

   UpdateEvent(Collection<BassName> na,Collection<BassName> nr) {
      names_added = na;
      names_removed = nr;
    }

   @Override public Collection<BassName> getNamesRemoved()	{ return names_removed; }
   @Override public Collection<BassName> getNamesAdded()	{ return names_added; }

}	// end of inner class UpdateEvent




/********************************************************************************/
/*										*/
/*	Tree node representation						*/
/*										*/
/********************************************************************************/

private static abstract class BassTreeImpl implements BassTreeBase, BassTreeNode {

   protected Branch parent_node;

   protected BassTreeImpl(Branch par) {
      parent_node = par;
    }

   Branch getBassParent()				{ return parent_node; }

   @Override abstract public int getChildCount();
   @Override abstract public BassTreeBase getChildAt(int idx);
   @Override public int getIndex(BassTreeBase chld)	{ return -1; }

   @Override abstract public String getLocalName();
   @Override abstract public boolean isLeaf();
   @Override public BassName getBassName()		{ return null; }
   @Override public Icon getExpandIcon()		{ return null; }
   @Override public Icon getCollapseIcon()		{ return null; }
   BranchNodeType getBranchType()			{ return BranchNodeType.NONE; }

   void collapseSingletons()				{ }
   boolean removeEmptyNodes()				{ return false; }

   @Override public String toString()			{ return getLocalName(); }

   abstract int getSortPriority();
   abstract void addAllNames(Collection<BassName> rslt);

   abstract int getMaxCount();
   @Override abstract public int getLeafCount();


}	// end of inner class BassTreeImpl




private static class TreeLeaf extends BassTreeImpl {

   private BassName for_name;
   private String short_name;

   TreeLeaf(BassName nm,Branch par) {
      super(par);
      for_name = nm;
      short_name = fixDisplayName();
    }

   @Override public int getChildCount() 		{ return 0; }
   @Override public BassTreeBase getChildAt(int idx)	{ return null; }
   @Override public boolean isLeaf()			{ return true; }

   @Override public BassName getBassName()		{ return for_name; }
   @Override public String getLocalName()		{ return for_name.getDisplayName(); }
   @Override public String getFullName() {
      return for_name.getFullName();
    }

   @Override public String toString()			{ return short_name; }

   @Override int getSortPriority()			{ return for_name.getSortPriority(); }

   @Override int getMaxCount()				{ return 0; }
   @Override public int getLeafCount()			{ return 1; }

   @Override void addAllNames(Collection<BassName> rslt) {
      if (for_name != null) rslt.add(for_name);
    }

   private String fixDisplayName() {
      String nm = getLocalName();
      if (!nm.contains(".")) return nm;
      if (nm.startsWith("<")) return nm;

      StringBuffer buf = new StringBuffer();
      StringBuffer pbuf = null;
      int prm = 0;
      for (int i = 0; i < nm.length(); ++i) {
	 char ch = nm.charAt(i);
	 if (ch == '(' || ch == '<') {
	    if (prm > 0) addParam(pbuf,buf);
	    pbuf = null;
	    buf.append(ch);
	    ++prm;
	  }
	 else if (prm > 0) {
	    if (ch == ',' || ch == ')' || ch == '>') {
	       addParam(pbuf,buf);
	       pbuf = null;
	       buf.append(ch);
	       if (ch == ')' || ch == '>') --prm;
	     }
	    else if (pbuf == null && ch == ' ') ;
	    else if (ch == ' ' && pbuf != null) {
	       addParam(pbuf,buf);
	       pbuf = new StringBuffer();
	       buf.append(ch);
	     }
	    else {
	       if (pbuf == null) pbuf = new StringBuffer();
	       pbuf.append(ch);
	     }
	  }
	 else buf.append(ch);
       }
      return buf.toString();
    }

   private void addParam(StringBuffer pbuf,StringBuffer buf) {
      if (pbuf == null) return;
      addParam(pbuf.toString(),buf);
    }

   private void addParam(String pnm,StringBuffer buf) {
      if (pnm.endsWith("...")) {
	 pnm = pnm.substring(0,pnm.length()-3);
	 addParam(pnm,buf);
	 buf.append("...");
	 return;
       }
      int idx = pnm.lastIndexOf(".");
      if (idx >= 0) pnm = pnm.substring(idx+1);
      buf.append(pnm);
    }

}	// end of inner class TreeLeaf



private static class Branch extends BassTreeImpl {

   private String local_name;
   private String display_name;
   private String orig_name;
   private String full_name;
   private Vector<BassTreeImpl> child_nodes;
   private int leaf_count;
   private BranchNodeType branch_type = BranchNodeType.PACKAGE;

   Branch(String name,Branch par) {
      super(par);
      local_name = name;
      display_name = name;
      orig_name = name;
      child_nodes = new Vector<>();
      int idx = name.indexOf("#");
      if (idx > 0) {
         display_name = name.substring(idx+1);
       }
      leaf_count = -1;
      if (par != null) {
         if (par.getBassParent() == null) full_name = local_name;
         else full_name = par.getFullName() + "." + local_name;
       }
      else full_name = local_name;
    }

   @Override public BassTreeImpl getChildAt(int idx) {
      if (idx < 0 || idx > child_nodes.size()) return null;
      return child_nodes.get(idx);
    }
   @Override public int getChildCount() 		{ return child_nodes.size(); }
   @Override public int getIndex(BassTreeBase tn)	{ return child_nodes.indexOf(tn); }
   @Override public boolean isLeaf()			{ return false; }
   @Override BranchNodeType getBranchType()			{ return branch_type; }

   void addChild(BassTreeImpl n) {
      child_nodes.add(n);
    }

   void setBranchType(BranchNodeType type) {
      if (branch_type == type) return;
      switch (branch_type) {
         case PROJECT :
         case PACKAGE :
         case METHOD :
            branch_type = type;
            break;
         case CLASS:
         case THROWABLE :
            if (type == BranchNodeType.INTERFACE) branch_type = type;
            if (type == BranchNodeType.ENUM) branch_type = type;
            if (type == BranchNodeType.THROWABLE) branch_type = type;
            if (type == BranchNodeType.ANNOTATION) branch_type = type;
            break;
         case INTERFACE :
         case ANNOTATION :
            break;
         case ENUM :
            if (type == BranchNodeType.INTERFACE) branch_type = type;
            break;
         case NONE:
            break;
       }
    }

   @Override public Icon getExpandIcon() {
      switch (branch_type) {
         case CLASS :
         case METHOD :
            return BoardImage.getIcon("class_expand");
         case THROWABLE :
            return BoardImage.getIcon("throw_expand");
         case PROJECT :
            return BoardImage.getIcon("project_expand");
         case PACKAGE :
            return BoardImage.getIcon("package_expand");
         case INTERFACE :
            return BoardImage.getIcon("interface_expand");
         case ENUM :
            return BoardImage.getIcon("enum_expand");
         case ANNOTATION :
            return BoardImage.getIcon("annot_expand");
         case NONE:
            break;
       }
      return null;
    }

   @Override public Icon getCollapseIcon() {
      switch (branch_type) {
         case CLASS :
         case METHOD :
            return BoardImage.getIcon("class_collapse");
         case THROWABLE :
            return BoardImage.getIcon("throw_collapse");
         case PACKAGE :
            return BoardImage.getIcon("package_collapse");
         case PROJECT :
            return BoardImage.getIcon("project_collapse");
         case INTERFACE :
            return BoardImage.getIcon("interface_collapse");
         case ENUM :
            return BoardImage.getIcon("enum_collapse");
         case ANNOTATION :
            return BoardImage.getIcon("annot_collapse");
         default:
            break;
       }
      return null;
    }

   @Override int getSortPriority()			{ return BASS_DEFAULT_INTERIOR_PRIORITY; }

   @Override void addAllNames(Collection<BassName> rslt) {
      for (BassTreeImpl tn : child_nodes) tn.addAllNames(rslt);
    }

   @Override public String getLocalName()		{ return local_name; }
   @Override public String toString()			{ return display_name; }
   @Override public String getFullName()		{ return full_name; }
   @Override Branch getBassParent()				{ return parent_node; }

   @Override void collapseSingletons() {
      if (parent_node != null) {
	 Branch cn = this;
	 StringBuffer buf = null;
	 while (cn.child_nodes.size() == 1) {
	    BassTreeImpl tn = cn.getChildAt(0);
	    if (tn.isLeaf()) break;
	    if (tn.getBranchType() == BranchNodeType.CLASS) break;
	    if (tn.getBranchType() == BranchNodeType.THROWABLE) break;
	    if (tn.getBranchType() == BranchNodeType.INTERFACE) break;
	    if (tn.getBranchType() == BranchNodeType.ENUM) break;
	    if (tn.getBranchType() == BranchNodeType.METHOD) break;
	    if (buf == null) {
	       buf = new StringBuffer();
	       buf.append(cn.getLocalName());
	     }
	    else {
	       if (buf.length() > 0 && buf.charAt(buf.length()-1) != ':') buf.append(".");
	       buf.append(cn.getLocalName());
	     }
	    cn = (Branch) tn;
	  }
	 if (cn != this && buf != null) {
	    int idx = parent_node.getIndex(this);
	    if (buf.charAt(buf.length()-1) != ':')
	       cn.local_name = buf.toString() + "." + cn.local_name;
	    else
	       cn.local_name = buf.toString() + cn.local_name;
	    cn.setDisplayName();
	    cn.parent_node = parent_node;
	    parent_node.child_nodes.set(idx,cn);
	    cn.collapseSingletons();
	    return;
	  }
       }
      for (BassTreeImpl ti : child_nodes) {
	 ti.collapseSingletons();
       }
    }
   
   @Override boolean removeEmptyNodes() {
      boolean chng = true;
      while (chng) {
	 chng = false;
	 for (BassTreeImpl ti : child_nodes) {
	    if (ti.removeEmptyNodes()) {
	       chng = true;
	       break;
	    }
	 }
      }
      
      if (!isLeaf() && parent_node != null && getChildCount() == 0) {
	 parent_node.child_nodes.remove(this);
	 return true;
      }
      
      return false;
   }

   private void setDisplayName() {
      if (local_name.length() < 32) {
	 display_name = local_name;
	 return;
       }
      String dnm = local_name;
      int idx2 = dnm.indexOf("#");
      if (idx2 > 0) {
	 dnm = dnm.substring(idx2+1);
       }
      int idx1 = dnm.indexOf(":");
      if (idx1 >= 0) {
	 String dnm1 = dnm.substring(0,idx1+1);
	 if (local_name.equals(dnm1+orig_name)) display_name = local_name;
	 else display_name = dnm1 + ".." + orig_name;
      }
      else display_name = ".." + orig_name;
    }

   Branch findNode(String txt,int priority) {
      int idx = 0;
      for (BassTreeImpl bt : child_nodes) {
	 int d = priority - bt.getSortPriority();
	 if (d < 0) break;
	 if (d == 0) {
	    int comp = txt.compareTo(bt.getLocalName());
	    if (comp == 0) return (Branch) bt;
	    if (comp < 0) break;
	  }
	 ++idx;
       }
      Branch b = new Branch(txt,this);
      child_nodes.insertElementAt(b,idx);
      int [] indx = new int[1];
      indx[0] = idx;
      return b;
    }

   BassTreeImpl addNode(BassName bn,boolean force) {
      // handle splitting here
      String [] comps = bn.getNameComponents();
      int cidx = 0;
      Branch parent = null;
   
      Branch p = this;
      while (p != null && cidx < comps.length) {
         parent = p;
         Branch np = null;
         for (BassTreeImpl bt : p.child_nodes) {
            if (bt.getLocalName().equals(comps[cidx])) {
               if (bt instanceof Branch) {
        	  np = (Branch) bt;
        	  cidx++;
        	  break;
        	}
               else {
        	  if (cidx == comps.length-1 && !force) return bt;
        	  BoardLog.logW("BALE","Search tree has leaf and parent with the same name: " +
        			   bt.getLocalName() + " in " + p.getLocalName());
        	  break;
        	}
             }
            else if (subsetName(bt.getLocalName(),comps[cidx])) {
               String nm = comps[cidx];
               int fnd = -1;
               for (int i = cidx+1; i < comps.length; ++i) {
        	  if (nm.endsWith(":")) nm += comps[i];
        	  else nm += "." + comps[i];
        	  if (bt.getLocalName().equals(nm)) {
        	     np = (Branch) bt;
        	     fnd = i;
        	     break;
        	   }
        	}
               if (fnd >= 0) {
        	  cidx = fnd+1;
        	  break;
        	}
               // need to split the local name
               nm = comps[cidx];
               fnd = cidx;
               String nm1 = nm;
               for (int i = cidx+1; i < comps.length; ++i) {
        	  if (nm.endsWith(":")) nm += comps[i];
        	  else nm += "." + comps[i];
        	  if (!subsetName(bt.getLocalName(),nm)) {
        	     fnd = i;
        	     break;
        	  }
        	  nm1 += "." + comps[i];
        	}
               splitNode((Branch) bt,nm1);
               np = (Branch) bt;
               cidx = fnd;
             }
          }
         if (np == null) break;
         p = np;
       }
      if (cidx == comps.length && bn.getNameType() == BassNameType.PROJECT && p != null) return p;
      if (cidx >= comps.length) return parent;
      if (cidx != comps.length -1 && !force) return null;
   
      for (int i = cidx; parent != null && i < comps.length-1; ++i) {
         parent = parent.findNode(comps[i],BASS_DEFAULT_INTERIOR_PRIORITY);
       }
   
      if (!force && parent != null) {
         String txt = bn.getNameWithParameters();
         for (BassTreeImpl bt : parent.child_nodes) {
            if (txt.equals(bt.getLocalName())) return bt;
          }
         return null;
       }
      if (parent == null) return null;
   
      if (bn.getNameType() == BassNameType.PROJECT) {
         String pat = comps[0] + ".";
         for (BassTreeImpl bti : parent.child_nodes) {
            String nm = bti.getFullName();
            if (nm.startsWith(pat)) return null;
         }
         Branch bb = new Branch(comps[0],parent);
         computeBranchType(bn,bb);
         parent.addChild(bb);
         return bb;
       }
      else if ((bn.getNameType() == BassNameType.CLASS || bn.getNameType() == BassNameType.INTERFACE ||
        	  bn.getNameType() == BassNameType.ENUM || bn.getNameType() == BassNameType.ANNOTATION)
               && bn.getClassName() != null &&
               parent.getBranchType() == BranchNodeType.PACKAGE) {
         if (parent.getFullName().endsWith(bn.getName())) {
            computeBranchType(bn,parent);
         }
       }
   
   
      TreeLeaf tl = parent.insertChild(bn);
      ++leaf_count;
   
      return tl;
    }

   private boolean subsetName(String nm,String base) {
      if (!nm.startsWith(base)) return false;
      int ln = base.length();
      if (nm.length() == ln) return true;
      if (base.endsWith(":")) return true;
      char ch = nm.charAt(ln);
      if (ch == '.' || ch == ':') return true;
      return false;
    }

   private void splitNode(Branch nd,String nm) {
      String nm1 = nd.getFullName();
      int ln = nm.length();
      nm1 = nm1.substring(ln+1);
      nd.full_name = nm;
      nd.local_name = nm.replace(":.", ":");
      String orignm = nd.orig_name;
      int oidx = nm.lastIndexOf(".");
      nd.orig_name = nm.substring(oidx+1);
      nd.setDisplayName();
      if (nd.local_name.endsWith(":")) nd.setBranchType(BranchNodeType.PROJECT);
      else nd.setBranchType(BranchNodeType.PACKAGE);
      Branch b1 = new Branch(nm1,nd);
      b1.orig_name = orignm;
   
      for (BassTreeImpl cn : nd.child_nodes) {
         b1.addChild(cn);
         cn.parent_node = b1;
      }
      nd.child_nodes.clear();
      nd.addChild(b1);
   }

   TreeLeaf insertChild(BassName nm) {
      String txt = nm.getNameWithParameters();
      int idx = 0;
      for (BassTreeImpl bt : child_nodes) {
	 if (txt.compareTo(bt.getLocalName()) <= 0) break;
	 ++idx;
       }
      TreeLeaf tl = new TreeLeaf(nm,this);
      child_nodes.insertElementAt(tl,idx);
      int [] idxs = new int[1];
      idxs[0] = idx;
      return tl;
    }

   boolean removeNode(BassName bn) {
      List<Integer> rem = null;
      int idx = 0;
      for (BassTreeImpl bt : child_nodes) {
	 if (bt.getLocalName().equals(bn.getDisplayName())) {
	    if (rem == null) rem = new ArrayList<Integer>();
	    rem.add(idx);
	  }
	 ++idx;
       }
      if (rem == null) return false;		// nothing changed;

      int [] chng = new int[rem.size()];
      Object [] chld = new Object[rem.size()];
      int ct = 0;
      for (Integer iv : rem) {
	 chng[ct++] = iv;
       }
      idx = 0;
      ct = 0;
      for (Iterator<BassTreeImpl> it = child_nodes.iterator(); it.hasNext(); ) {
	 BassTreeImpl tn = it.next();
	 if (ct < chng.length && idx == chng[ct]) {
	    it.remove();
	    chld[ct] = tn;
	    ++ct;
	  }
	 ++idx;
       }

      return true;
    }

   @Override int getMaxCount() {
      int ct = child_nodes.size();
      for (BassTreeImpl bti : child_nodes) {
	 ct = Math.max(ct,bti.getMaxCount());
       }
      return ct;
    }

   @Override public int getLeafCount() {
      if (leaf_count < 0) {
	 leaf_count = 0;
	 for (BassTreeImpl bti : child_nodes) leaf_count += bti.getLeafCount();
       }
      return leaf_count;
    }

}	// end of inner class Branch



}	// end of class BassTreeModelBase



/* end of BassTreeModelBase.java */
