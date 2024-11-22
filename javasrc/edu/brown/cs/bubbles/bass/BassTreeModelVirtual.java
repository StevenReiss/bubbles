/********************************************************************************/
/*										*/
/*		BassTreeModelVirtual.java					*/
/*										*/
/*	Bubble Augmented Search Strategies tree model for a search box		*/
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

import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;



final class BassTreeModelVirtual implements BassConstants, TreeModel, BassTreeModel,
		BassTreeModel.BassTreeUpdateListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BassTreeModelBase	base_model;
private ActiveNodes		active_nodes;
private String			initial_project;
private String			initial_prefix;
private String			cur_prefix;
private int			leaf_count;
private boolean 		case_sensitive;
private Collection<TreeModelListener>  listener_set;

private static final BassTreeBase [] EMPTY = new BassTreeBase[0];



/********************************************************************************/
/*										*/
/*	Activation methods							*/
/*										*/
/********************************************************************************/

static BassTreeModelVirtual create(BassRepository br,String proj,String clspfx)
{
   BassTreeModelBase tmb = BassFactory.getModelBase(br);

   return new BassTreeModelVirtual(tmb,proj,clspfx);
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BassTreeModelVirtual(BassTreeModelBase base,String proj,String clspfx)
{
   base_model = base;
   active_nodes = new ActiveNodes();

   initial_project = proj;
   initial_prefix = clspfx;

   listener_set = new ArrayList<TreeModelListener>();

   leaf_count = base.getLeafCount();
   case_sensitive = false;
   cur_prefix = "";

   if (initial_project != null || initial_prefix != null) {
      String pfx = null;
      if (initial_project != null) pfx = "E:" + initial_project + " N:";
      if (initial_prefix != null) {
	 if (pfx == null) pfx = initial_prefix;
	 else pfx = pfx + initial_prefix;
       }
      prune(pfx,false);
    }
}



/********************************************************************************/
/*										*/
/*	TreeModel methods							*/
/*										*/
/********************************************************************************/

@Override public void addTreeModelListener(TreeModelListener l)
{
   synchronized (listener_set) {
      listener_set.add(l);
      if (listener_set.size() == 1) {
	 base_model.addUpdateListener(this);
       }
    }
}



@Override public void removeTreeModelListener(TreeModelListener l)
{
   synchronized (listener_set) {
      listener_set.remove(l);
      if (listener_set.isEmpty()) {
	 base_model.removeUpdateListener(this);
       }
    }
}



@Override public int getChildCount(Object parent)
{
   return active_nodes.getChildCount((BassTreeBase) parent);
}


@Override public Object getRoot()
{
   return base_model.getRoot();
}


@Override public boolean isLeaf(Object node)
{
   return ((BassTreeBase) node).isLeaf();
}



@Override public void valueForPathChanged(TreePath tp,Object nv)	{ }



@Override public Object getChild(Object par,int idx)
{
   return active_nodes.getChild((BassTreeBase) par,idx);
}



@Override public int getIndexOfChild(Object par,Object chld)
{
   return active_nodes.getIndexOfChild((BassTreeBase) par,(BassTreeBase) chld);
}



/********************************************************************************/
/*										*/
/*	Methods to restrict the set with further text				*/
/*										*/
/********************************************************************************/

@Override public void prune(String txt,boolean upd)
{
   if (txt == null) txt = "";
   if (cur_prefix.trim().equals(txt.trim())) return;

   base_model.readLock();
   try {
      BassNamePattern pat = new BassNamePattern(txt,case_sensitive);

      Stack<BassTreeBase> pars = new Stack<>();

      prune(pars,base_model.getRoot(),pat,upd);

      cur_prefix = txt;
    }
   finally { base_model.readUnlock(); }
}



private boolean prune(Stack<BassTreeBase> pars,BassTreeBase node,BassNamePattern pat,boolean upd)
{
   if (node.isLeaf()) {
      boolean fg = pat.match(node.getBassName()) >= 0;
      if (!fg) --leaf_count;
      return fg;
    }

   Map<BassTreeBase,Integer> removed = null;

   pars.push(node);

   int ln = active_nodes.getChildCount(node);
   for (int i = 0; i < ln; ++i) {
      BassTreeBase chld = active_nodes.getChild(node,i);
      if (!prune(pars,chld,pat,upd)) {
	 if (removed == null) removed = new HashMap<>();
	 removed.put(chld,i);
       }
    }

   pars.pop();

   if (removed == null) return true;		// nothing happened

   if (removed.size() == ln && pars.size() > 0) {			// all removed
      active_nodes.setChildren(node,EMPTY);
      return false;
    }

   BassTreeBase [] path = new BassTreeBase[pars.size() + 1];
   path = pars.toArray(path);
   path[pars.size()] = node;

   int kln = (ln - removed.size());
   BassTreeBase [] keep = new BassTreeBase[kln];
   int [] idxs = new int[removed.size()];
   Object [] itms = new Object[removed.size()];

   int j = 0;
   int k = 0;
   for (int i = 0; i < ln; ++i) {
      BassTreeBase chld = active_nodes.getChild(node,i);
      if (removed.containsKey(chld)) {
	 idxs[j] = i;
	 itms[j++] = chld;
       }
      else {
	 keep[k++] = chld;
       }
    }

   active_nodes.setChildren(node,keep);
   if (upd) {
      TreeModelEvent evt = new TreeModelEvent(this,path,idxs,itms);
      synchronized (listener_set) {
	 for (TreeModelListener tml : listener_set) {
	    tml.treeNodesRemoved(evt);
	  }
       }
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Top level methods for BASS						*/
/*										*/
/********************************************************************************/

@Override public void reset(String txt,boolean upd)
{
   if (txt == null) txt = "";
   if (cur_prefix != null && cur_prefix.trim().equals(txt.trim())) return;

   base_model.readLock();
   try {
      BassNamePattern pat = new BassNamePattern(txt,case_sensitive);

      Stack<BassTreeBase> pars = new Stack<>();

      reset(pars,base_model.getRoot(),pat,upd);

      cur_prefix = txt;

      leaf_count = active_nodes.getLeafCount(base_model.getRoot());
    }
   finally { base_model.readUnlock(); }
}




private boolean reset(Stack<BassTreeBase> pars,BassTreeBase node,BassNamePattern pat,boolean upd)
{
   if (node.isLeaf()) return pat.match(node.getBassName()) >= 0;

   Map<BassTreeBase,Integer> removed = null;
   List<BassTreeBase> keep = null;
   BassTreeBase [] path = null;

   pars.push(node);

   int aln = active_nodes.getChildCount(node);
   int ln = node.getChildCount();
   for (int i = 0; i < ln; ++i) {
      BassTreeBase chld = node.getChildAt(i);
      if (!reset(pars,chld,pat,upd)) {
	 if (removed == null) removed = new HashMap<BassTreeBase,Integer>();
	 removed.put(chld,-i-1);
       }
      else {
	 if (keep == null) keep = new ArrayList<BassTreeBase>();
	 keep.add(chld);
       }
    }

   pars.pop();

   TreeModelEvent revt = null;
   TreeModelEvent ievt = null;

   Set<BassTreeBase> cur = new HashSet<BassTreeBase>();
   int rct = 0;
   for (int i = 0; i < aln; ++i) {
      BassTreeBase chld = active_nodes.getChild(node,i);
      if (removed != null && removed.containsKey(chld)) {
	 removed.put(chld,i);
	 ++rct;
       }
      else cur.add(chld);
    }

   if (rct > 0 && removed != null) {
      if (path == null) {
	 path = new BassTreeBase[pars.size() + 1];
	 path = pars.toArray(path);
	 path[pars.size()] = node;
       }
      int [] idxs = new int[rct];
      Object [] itms = new Object[rct];
      int j = 0;
      for (int i = 0; i < aln; ++i) {
	 BassTreeBase chld = active_nodes.getChild(node,i);
	 Integer ivl = removed.get(chld);
	 if (ivl != null && ivl >= 0) {
	    idxs[j] = i;
	    itms[j++] = chld;
	  }
       }
      revt = new TreeModelEvent(this,path,idxs,itms);
    }

   BassTreeBase [] nitms = null;
   List<Integer> adds = null;

   if (keep != null) {
      nitms = new BassTreeBase[keep.size()];
      int i = 0;
      for (BassTreeBase chld : keep) {
	 if (!cur.contains(chld)) {
	    if (adds == null) adds = new ArrayList<Integer>();
	    adds.add(i);
	  }
	 nitms[i++] = chld;
       }
    }

   if (adds != null) {
      if (path == null) {
	 path = new BassTreeBase[pars.size() + 1];
	 path = pars.toArray(path);
	 path[pars.size()] = node;
       }
      int [] idxs = new int[adds.size()];
      int j = 0;
      for (Integer ivl : adds) idxs[j++] = ivl;
      ievt = new TreeModelEvent(this,path,idxs,null);
    }

   if (nitms == null) active_nodes.setChildren(node,EMPTY);
   else if (nitms.length == ln) active_nodes.setChildren(node,null);
   else active_nodes.setChildren(node,nitms);

   if (upd && nitms != null) {
      synchronized (listener_set) {
	 for (TreeModelListener tml : listener_set) {
	    if (revt != null) tml.treeNodesRemoved(revt);
	    if (ievt != null) tml.treeNodesInserted(ievt);
	    if (adds != null) {
	       BassTreeBase [] spath = new BassTreeBase[pars.size() + 2];
	       spath = pars.toArray(spath);
	       spath[pars.size()] = node;
	       for (Integer ivl : adds) {
		  if (!nitms[ivl].isLeaf()) {
		     spath[pars.size()+1] = nitms[ivl];
		     TreeModelEvent cevt = new TreeModelEvent(this,spath);
		     tml.treeStructureChanged(cevt);
		   }
		}
	     }
	  }
       }
    }

   boolean keepnode = true;
   if (nitms == null) {
      if (node.getLocalName().endsWith(":")) keepnode = true;
      else keepnode = false;
   }

   return keepnode;
}




@Override public void globalUpdate()
{
   Object [] spath = new Object[1];
   spath[0] = getRoot();
   TreeModelEvent cevt = new TreeModelEvent(this,spath);

   synchronized (listener_set) {
      for (TreeModelListener tml : listener_set) {
	 tml.treeStructureChanged(cevt);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Tree model changes							*/
/*										*/
/********************************************************************************/

@Override public void handleTreeUpdated(BassTreeUpdateEvent evt)
{
   updateAll();
}


@Override public void rebuild(BassRepository br)
{
   updateAll();
}



private void updateAll()
{
   String pfx = cur_prefix;
   cur_prefix = null;
   reset(cur_prefix,false);
   reset(pfx,false);
   globalUpdate();
}


/********************************************************************************/
/*										*/
/*	Get a singleton if one exists						*/
/*										*/
/********************************************************************************/

@Override public int getLeafCount()
{
   return leaf_count;
}



@Override public BassName getSingleton()
{
   base_model.readLock();
   try {
      for (BassTreeBase p = base_model.getRoot();
	   active_nodes.getChildCount(p) <= 1;
	   p = active_nodes.getChild(p,0)) {
	 if (p.isLeaf()) return p.getBassName();
	 if (active_nodes.getChildCount(p) == 0) break;
       }
    }
   finally { base_model.readUnlock(); }

   return null;
}



@Override public int [] getIndicesOfFirstMethod()
{
   List<Integer> list = new ArrayList<Integer>();

   getIndicesOfFirstMethod(base_model.getRoot(),list);

   int [] rslt = new int[list.size()];
   for (int i = 0; i < list.size(); ++i) {
      rslt[i] = list.get(i);
    }

   return rslt;
}



private boolean getIndicesOfFirstMethod(BassTreeBase p,List<Integer> list)
{
   int ln = active_nodes.getChildCount(p);
   for (int i = 0; i < ln; ++i) {
      BassTreeBase p0 = active_nodes.getChild(p,i);
      if (p0.isLeaf()) {
	 BassName bn = p0.getBassName();
	 if (bn.getNameType() == BassNameType.METHOD || bn.getNameType() == BassNameType.CONSTRUCTOR) {
	    list.add(i);
	    return true;
	  }
       }
    }

   for (int i = 0; i < ln; ++i) {
      BassTreeBase p0 = active_nodes.getChild(p,i);
      if (!p0.isLeaf()) {
	 if (getIndicesOfFirstMethod(p0,list)) {
	    list.add(i+1);
	    return true;
	  }
       }
    }

   list.add(ln);

   return false;
}




/********************************************************************************/
/*										*/
/*	Tree Path creation							*/
/*										*/
/********************************************************************************/

@Override public TreePath getTreePath(String nm)
{
   StringTokenizer tok = new StringTokenizer(nm,"@");
   Object [] elts = new Object[tok.countTokens()+1];
   BassTreeBase tn = base_model.getRoot();
   int ect = 0;

   elts[ect++] = tn;
   while (tn != null && tok.hasMoreTokens()) {
      String snm = tok.nextToken();
      int ct = active_nodes.getChildCount(tn);
      BassTreeBase ctn = null;
      for (int i = 0; i < ct; ++i) {
	 BassTreeBase btn = active_nodes.getChild(tn,i);
	 if (btn.getLocalName().equals(snm)) {
	    ctn = btn;
	    break;
	  }
       }
      elts[ect++] = ctn;
      tn = ctn;
    }
   if (tn == null) return null;

   return new TreePath(elts);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw)
{
   if (initial_project != null) xw.field("PROJECT",initial_project);
   if (initial_prefix != null) xw.field("PREFIX",initial_prefix);
   // TODO: output bubble type
}




/********************************************************************************/
/*										*/
/*	Holder of which nodes are active					*/
/*										*/
/********************************************************************************/

private static class ActiveNodes {

   private Map<BassTreeBase,BassTreeBase []> active_entries;

   ActiveNodes() {
      active_entries = new HashMap<BassTreeBase,BassTreeBase []>();
    }

   int getChildCount(BassTreeBase nd) {
      BassTreeBase[] cur = active_entries.get(nd);
      if (cur == null) return nd.getChildCount();
      return cur.length;
    }

   BassTreeBase getChild(BassTreeBase par,int idx) {
      BassTreeBase [] cur = active_entries.get(par);
      if (cur == null) return par.getChildAt(idx);
      if (idx < 0 || idx >= cur.length) return null;
      return cur[idx];
    }

   int getIndexOfChild(BassTreeBase par,BassTreeBase chld) {
      BassTreeBase [] cur = active_entries.get(par);
      if (cur == null) return par.getIndex(chld);
      for (int i = 0; i < cur.length; ++i) if (cur[i] == chld) return i;
      return -1;
    }

   void setChildren(BassTreeBase node,BassTreeBase [] chld) {
      if (chld == null) active_entries.remove(node);
      else active_entries.put(node,chld);
    }

   int getLeafCount(BassTreeBase nd) {
      if (nd.isLeaf()) return 1;
      int ct = 0;
      int ln = getChildCount(nd);
      for (int i = 0; i < ln; ++i) {
	 BassTreeBase chld = getChild(nd,i);
	 ct += getLeafCount(chld);
       }
      return ct;
    }


}	// end of inner class ActiveNodes




}	// end of class BassTreeModelVirtual




/* end of BassTreeModelVirtual.java */








