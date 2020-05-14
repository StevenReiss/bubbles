/********************************************************************************/
/*                                                                              */
/*              BfixOrderNewElement.java                                        */
/*                                                                              */
/*      Representation of an item to be inserted as an element                  */
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bump.BumpConstants.BumpSymbolType;



class BfixOrderNewElement extends BfixOrderElement
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          content_body;
private int             insert_position;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixOrderNewElement(String name,BumpSymbolType type,int mods,String cnts)
{
   super(-1,-1,name,type);
   content_body = cnts;
   insert_position = -1;
}




/********************************************************************************/
/*                                                                              */
/*      Methods to change the contents                                          */
/*                                                                              */
/********************************************************************************/

void addPrefix(String pfx)
{
   content_body = pfx + content_body;
}



void addSuffix(String sfx)
{
   content_body = content_body + sfx;
}



void addBlanks(int pct,int sct)
{
   if (pct <= 0 && sct <= 0) return;
   
   StringBuffer buf = new StringBuffer();
   if (pct > 0) {
      for (int i = 0; i < pct; ++i) buf.append("\n");
    }
   buf.append(content_body);
   if (sct > 0) {
      for (int i = 0; i < sct; ++i) buf.append("\n");
    }
   
   content_body = buf.toString();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void setInsertPosition(int pos)                 { insert_position = pos; }

int getInsertPosition()                         { return insert_position; }

String getContents()                            { return content_body; }


}       // end of class BfixOrderNewElement




/* end of BfixOrderNewElement.java */

