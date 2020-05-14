/********************************************************************************/
/*										*/
/*		BassTreeModel.java						*/
/*										*/
/*	Bubble Augmented Search Strategies tree model generic definitions	*/
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

import javax.swing.Icon;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.Collection;
import java.util.EventListener;


interface BassTreeModel extends TreeModel, BassConstants {



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public int getLeafCount();

public BassName getSingleton();

public TreePath getTreePath(String nm);

public int [] getIndicesOfFirstMethod();




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

public void prune(String pat,boolean upd);

public void reset(String pat,boolean upd);

public void globalUpdate();



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public void outputXml(BudaXmlWriter xw);



public void rebuild(BassRepository br);


/********************************************************************************/
/*										*/
/*	Generic tree node							*/
/*										*/
/********************************************************************************/

interface BassTreeNode {

   BassName getBassName();
   String getLocalName();
  
}	// end of inner interface BassTreeNode



interface BassTreeBase extends BassTreeNode {

   boolean isLeaf();
   int getChildCount();
   BassTreeBase getChildAt(int idx);
   int getIndex(BassTreeBase chld);
   int getLeafCount();
   Icon getExpandIcon();
   Icon getCollapseIcon();
   String getFullName();

}	// end of inner interface BassTreeBse


interface BassTreeUpdateEvent {

   Collection<BassName> getNamesRemoved();
   Collection<BassName> getNamesAdded();

}	// end of inner interface BassTreeUpdateEvent


interface BassTreeUpdateListener extends EventListener {

   void handleTreeUpdated(BassTreeUpdateEvent evt);

}	// end of inner interface BassTreeUpdateListener



}	// end of interface BassTreeModel




/* end of BassTreeModel.java */
