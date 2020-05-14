/********************************************************************************/
/*										*/
/*		BaleElementEvent.java						*/
/*										*/
/*	Bubble Annotated Language Editor document element change events 	*/
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
import javax.swing.text.Element;
import javax.swing.undo.AbstractUndoableEdit;

import java.util.List;



class BaleElementEvent extends AbstractUndoableEdit implements DocumentEvent.ElementChange,
		BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Element base_element;
private Element [] children_added;
private Element [] children_removed;
private int element_index;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/


BaleElementEvent(BaleElement par,List<BaleElement> rep)
{
   // replace all children of par with elements in rep

   base_element = par;
   children_removed = new Element[par.getElementCount()];
   for (int i = 0; i < children_removed.length; ++i) children_removed[i] = par.getElement(i);
   children_added = new Element[rep.size()];
   int j = 0;
   for (BaleElement be : rep) children_added[j++] = be;
   element_index = 0;

   // BoardLog.logD("BALE","ELEVENT " + element_index + " " + children_removed.length + " " + children_added.length);
}




BaleElementEvent(BaleElement par,BaleElement old,List<BaleElement> rep)
{
   // replace old in par with elements in rep

   if (par == null) return;

   base_element = par;
   children_removed = new Element[1];
   children_removed[0] = old;
   for (int i = 0; i < par.getElementCount(); ++i) {
      if (par.getElement(i) == old) {
	 element_index = i;
	 break;
       }
    }
   children_added = new Element[rep.size()];
   int j = 0;
   for (BaleElement be : rep) children_added[j++] = be;

   BoardLog.logD("BALE","ELEVENT " + element_index + " " + children_removed.length + " " +
		    children_added.length);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Element [] getChildrenAdded()		{ return children_added; }
@Override public Element [] getChildrenRemoved()	{ return children_removed; }
@Override public Element getElement()			{ return base_element; }
@Override public int getIndex() 			{ return element_index; }





}	// end of class BaleElementEvent




/* end of BaleElementEvent.java */
