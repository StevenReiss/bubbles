/********************************************************************************/
/*										*/
/*		BfixConstants.java						*/
/*										*/
/*	Constants for automatic correction package				*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;

import java.util.Comparator;



public interface BfixConstants
{



/********************************************************************************/
/*										*/
/*	Ordering Constants							*/
/*										*/
/********************************************************************************/

String	ORDER_FILE = "bfixorder.xml";

enum OrderContext {
   CLASS,
   INTERFACE,
   INNER_CLASS,
   INNER_INTERFACE
};




/********************************************************************************/
/*										*/
/*	Buttons 								*/
/*										*/
/********************************************************************************/

String BFIX_CHORE_MANAGER_BUTTON = "Bubbles.Show Chore Manager";
String BFIX_MODEL_UPDATE_BUTTON = "Admin.Admin.Update Fix Models";






/********************************************************************************/
/*										*/
/*	Comparator for region sort order					*/
/*										*/
/********************************************************************************/

public class ElementComparator implements Comparator<BfixOrderElement> {

   @Override public int compare(BfixOrderElement r1,BfixOrderElement r2) {
      int v = r1.getStartOffset() - r2.getStartOffset();
      if (v != 0) return v;
      v = r1.getEndOffset() - r2.getEndOffset();
      return v;
    }

}	// end of inner class ElementComparator


/********************************************************************************/
/*										*/
/*	Token interface 							*/
/*										*/
/********************************************************************************/

enum BfixTokenType {
   EOL, 			// end of line
   BLOCK_CMMT,			// /* ... */ comment; may only be part of a
				//	 comment if split
   DOC_CMMT,			// /** ... comment for documentation
   LINE_CMMT,			// // comment
   OTHER,			// any other token
}


interface BfixToken {
   int getStart();
   int getLength();
   BfixTokenType getTokenType();
}




/********************************************************************************/
/*										*/
/*	Interface for multiple fixes						*/
/*										*/
/********************************************************************************/

interface CanFixCallback {

   void canFix(boolean fg);

}	// end of inner interface CanFixCallback



/********************************************************************************/
/*										*/
/*	Adapter to handle nested or multiple fixes				*/
/*										*/
/********************************************************************************/

interface FixAdapter {
   BumpProblem getProblem();
   void noteFixersAdded(int ct);
   void noteStatus(boolean fg);
   void noteFix(Runnable fix);
   String getPrivateBufferId();
}



}	// end of interface BfixConstants




/* end of BfixConstants.java */

