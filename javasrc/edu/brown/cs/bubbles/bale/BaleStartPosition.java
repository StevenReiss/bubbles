/********************************************************************************/
/*										*/
/*		BaleStartPosition.java						*/
/*										*/
/*	Bubble Annotated Language Editor start position implementation		*/
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


import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;



class BaleStartPosition implements Position, BaleConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private	  Document	for_document;
private   Position	base_position;
private   Position	actual_position;



/********************************************************************************/
/*										*/
/*	Position creation methods						*/
/*										*/
/********************************************************************************/

static Position createStartPosition(Document d,int off) throws BadLocationException
{
   if (off == 0) return d.getStartPosition();

   return new BaleStartPosition(d,off);
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BaleStartPosition(Document d,int off) throws BadLocationException
{
   base_position = d.createPosition(off-1);
   actual_position = d.createPosition(off);
   for_document = d;
}




/********************************************************************************/
/*										*/
/*	Access methodes 							*/
/*										*/
/********************************************************************************/

@Override public int getOffset()
{
   int off = base_position.getOffset();
   int aoff = actual_position.getOffset();
   if (off == aoff) {
      off = aoff-1;
      try {
         base_position = for_document.createPosition(off);
      }
      catch (BadLocationException e) {
	 // do something
      }
   }
      
   return off+1;
}



void savePosition(BaleDocument bd)
{
   base_position = bd.savePosition(base_position);
   actual_position = bd.savePosition(actual_position);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return "[" + base_position.getOffset() + "+1]";
}



}	// end of class BalePosition




/* end of BalePosition.java */

