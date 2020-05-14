/********************************************************************************/
/*                                                                              */
/*              BrepairMethodTreeModel.java                                     */
/*                                                                              */
/*      Tree model for displaying possibly faulty methods and blocks            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.brepair;

import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import edu.brown.cs.ivy.swing.SwingEventListenerList;

class BrepairMethodTreeModel implements BrepairConstants, TreeModel     
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BrepairCountData        count_data;
private List<String>            root_methods;
private String                  root_object;
private SwingEventListenerList<TreeModelListener> model_listeners;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BrepairMethodTreeModel(BrepairCountData bcd)
{
   count_data = bcd;
   root_methods = bcd.getSortedMethods();
   root_object = "*ALL*";
   model_listeners = new SwingEventListenerList<>(TreeModelListener.class);
}



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

void countsUpdated()
{
   Object [] spath = new Object[1];
   spath[0] = root_object;
   TreeModelEvent evt = new TreeModelEvent(this,spath);
   
   root_methods = count_data.getSortedMethods();
   for (TreeModelListener tml : model_listeners) {
      tml.treeStructureChanged(evt);
    }
}



/********************************************************************************/
/*                                                                              */
/*      TreeModel methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public void addTreeModelListener(TreeModelListener l)
{
   model_listeners.add(l);
}


@Override public void removeTreeModelListener(TreeModelListener l)
{
   model_listeners.remove(l);
}


@Override public Object getRoot() 
{
   return root_object;
}


@Override public Object getChild(Object par,int idx) 
{
   if (par.equals(root_object)) {
      if (idx < 0 || idx >= root_methods.size()) return null;
      return root_methods.get(idx);
    }
   else if (root_methods.contains(par)) {
      List<String> blks = count_data.getSortedBlocks(par);
      if (blks == null || idx < 0 || idx >= blks.size()) return null;
      return blks.get(idx);
    }
   return null;
}



@Override public int getIndexOfChild(Object par,Object child)
{
   if (par.equals(root_object)) {
      return root_methods.indexOf(child);
    }
   else if (root_methods.contains(par)) {
      List<String> blks = count_data.getSortedBlocks(par);
      if (blks == null) return -1;
      return blks.indexOf(child);
    }
   return -1;
}


@Override public int getChildCount(Object par) 
{
   if (par.equals(root_object)) return root_methods.size();
   else if (root_methods.contains(par)) {
      List<String> blks = count_data.getSortedBlocks(par);
      if (blks == null) return 0;
      return blks.size();
    }
   
   return 0;
}


@Override public boolean isLeaf(Object node)
{
   if (node.equals(root_object)) return false;
   if (root_methods.contains(node)) return false;
   return true;
}


@Override public void valueForPathChanged(TreePath tp,Object newvalue)
{
}




}       // end of class BrepairMethodTreeModel





/* end of BrepairMethodTreeModel.java */

