/********************************************************************************/
/*										*/
/*		BaleMetrics.java						*/
/*										*/
/*	Bubble Annotated Language Editor data collection for metrics		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

import java.awt.Dimension;


class BaleMetrics implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int	num_visible;
private int	num_reflow;
private int	num_elision;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleMetrics()
{
   num_visible = 0;
   num_reflow = 0;
   num_elision = 0;
}




/********************************************************************************/
/*										*/
/*	Computation methods							*/
/*										*/
/********************************************************************************/

void computeMetrics(BaleFragmentEditor ed)
{
   BudaBubble bbl = BudaRoot.findBudaBubble(ed);
   if (bbl == null) return;
   String id = bbl.getHashId();
   if (id == null) return;

   Dimension d = ed.getSize();
   BaleDocument bd = ed.getDocument();
   Object o = bd.getDefaultRootElement();
   if (o == null || !(o instanceof BaleElement)) return;
   BaleElement be = (BaleElement) o;

   num_visible = 0;
   num_reflow = 0;
   num_elision = 0;

   countElements(be);

   BoardMetrics.noteCommand("BALE","VIS_LINES_" + id + "_" + d.height/14);
   BoardMetrics.noteCommand("BALE","CNT_LINES_" + id + "_" + num_visible);
   BoardMetrics.noteCommand("BALE","NUM_REFLOW_" + id + "_" + num_reflow);
   BoardMetrics.noteCommand("BALE","NUM_ELISION_" + id + "_" + num_elision);
}




private void countElements(BaleElement be)
{
   if (be.isElided()) {
      ++num_elision;
      return;
    }

   if (be.isLineElement() || be.isUnknown()) {
      num_reflow += be.getReflowCount();
      ++num_visible;
      return;
    }

   int ln = be.getElementCount();
   for (int i = 0; i < ln; ++i) {
      countElements(be.getBaleElement(i));
    }
}




}	// end of class BaleMetrics




/* end of BaleMetrics.java */
