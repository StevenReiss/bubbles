/********************************************************************************/
/*										*/
/*		BfixSmartInsert.java						*/
/*										*/
/*	Algorithm to use ordering information to do a smart insertion		*/
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

import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;

import java.util.Collection;


class BfixSmartInsert implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BfixFileOrder	file_order;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixSmartInsert(BfixCorrector corr)
{
   file_order = new BfixFileOrder(corr);
}


/********************************************************************************/
/*										*/
/*	Change methods								*/
/*										*/
/********************************************************************************/

void noteChange()
{
   file_order.noteChange();
}



/********************************************************************************/
/*										*/
/*	Worker methods								*/
/*										*/
/********************************************************************************/

void smartInsertSetup(BfixOrderNewElement newelt)
{
   BfixOrderElement root = findProperRoot(newelt);
   if (root == null) return;
   
   BfixOrderSet useorder = findOrder(root);
   BfixOrderElement prior = findPriorElement(newelt,root,useorder);
   BfixOrderElement next = findNextElement(prior,root);
   updateNewElement(useorder,prior,next,newelt);
}


void smartInsertInsert(BfixOrderNewElement newelt)
{
   BfixCorrector corr = file_order.getCorrector();
   BaleFileOverview fov = corr.getEditor().getWindowDocument().getBaseWindowDocument();
   BaleWindowDocument windoc = corr.getEditor().getWindowDocument();
   int pos0 = newelt.getInsertPosition();
   int pos1 = fov.mapOffsetToEclipse(pos0);
   int pos2 = windoc.mapOffsetToJava(pos1); 
   windoc.replace(pos2,0,newelt.getContents(),true,true);
}



private BfixOrderElement findProperRoot(BfixOrderElement newelt)
{
   Collection<BfixOrderElement> curorder = file_order.getRoots();
   
   String nm = newelt.getName();
   BfixOrderElement root = null;
   for (BfixOrderElement boe : curorder) {
      String tnm = boe.getName();
      if (nm.startsWith(tnm)) {
         root = boe;
         break;
       }
    }
   if (root == null) {
      System.err.println("Can't find root element");
      for (BfixOrderElement boe : curorder) {
         root = boe;
         break;
       }
      if (root == null) return null;
    }  
   boolean check = true;
   while (check) {
      check = false;
      for (BfixOrderElement boe : root.getChildren()) {
         String tnm = boe.getName();
         if (tnm != null && nm.startsWith(tnm)) {
            if (boe.getChildren().size() > 0) {
               root = boe;
               check = true;
               break;
             }
          }
       }
    }
   
   return root;
}



private BfixOrderSet findOrder(BfixOrderElement boe)
{
   return BfixFactory.getFactory().getBaseOrder().findSetForElement(boe);
}



private BfixOrderElement findPriorElement(BfixOrderElement newelt,BfixOrderElement root,
      BfixOrderSet ordering)
{
   BfixOrderElement use = null;
   BfixOrderElement last = null;
   int usect = 0;
   
   for (BfixOrderElement child : root.getChildren()) {
      if (child.getSymbolType() == null) continue;
      int ord = ordering.compareTo(child,newelt);
      if (ord > 0) {
         if (use == null) use = last;
         usect = 0;
       }     
      else if (ord < 0) {
         if (usect++ > 1) use = null; 
       }
      last = child;
    }
   
   if (use == null) use = last;
   
   return use;
}


private BfixOrderElement findNextElement(BfixOrderElement prior,BfixOrderElement root)
{
   boolean next = (prior == null);
   for (BfixOrderElement child : root.getChildren()) {
      if (next && child.getSymbolType() != null) return child;
      if (child == prior) next = true;
    }
   return null;
}



private void updateNewElement(BfixOrderSet ordering,
      BfixOrderElement prior,BfixOrderElement next,BfixOrderNewElement newelt)
{
   // BfixOrderItem pitm = ordering.findOrderItem(prior);
   // BfixOrderItem nitm = ordering.findOrderItem(next);
   // BfixOrderItem citm = ordering.findOrderItem(newelt);
   // BfixOrderItem insertprior = citm.getPriorItem(pitm,nitm);
   // BfixOrderItem insertnext = citm.getNextItem(pitm,nitm);
   // 
   // if (nitm == citm) {
      // newelt.setInsertPosition(next.getStartOffset());
      // while (insertnext != null) {
         // String txt = insertnext.getAddedText(newelt);
         // if (txt != null) newelt.addSuffix(txt);
         // insertnext = insertnext.getNextItem(pitm,nitm);
       // }
    // }           
   // else if (pitm == citm) {
      // newelt.setInsertPosition(prior.getEndOffset());
      // while (insertprior != null) {
         // String txt = insertprior.getAddedText(newelt);
         // if (txt != null) newelt.addPrefix(txt);
         // insertprior = insertprior.getPriorItem(pitm,nitm);
       // }
    // }
   // else {
      // while (insertprior != null) {
         // String txt = insertprior.getAddedText(newelt);
         // if (txt != null) newelt.addPrefix(txt);
         // insertprior = insertprior.getPriorItem(pitm,nitm);
       // }
      // while (insertnext != null) {
         // String txt = insertnext.getAddedText(newelt);
         // if (txt != null) newelt.addSuffix(txt);
         // insertnext = insertnext.getNextItem(pitm,nitm);
       // }
      // if (prior != null) newelt.setInsertPosition(prior.getEndOffset());
      // else if (next != null) newelt.setInsertPosition(next.getStartOffset());
    // }
}







}	// end of class BfixSmartInsert




/* end of BfixSmartInsert.java */

