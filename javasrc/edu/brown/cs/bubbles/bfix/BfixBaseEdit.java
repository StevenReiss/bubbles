/********************************************************************************/
/*                                                                              */
/*              BfixBaseEdit.java                                               */
/*                                                                              */
/*      Representation of a single replace edit                                 */
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



package edu.brown.cs.bubbles.bfix;

import java.io.File;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;
import edu.brown.cs.bubbles.bfix.BfixConstants.BfixEdit;
import edu.brown.cs.bubbles.bump.BumpClient;

public class BfixBaseEdit implements BfixConstants, BfixEdit
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BfixCorrector for_corrector;
private int start_offset;
private int end_offset;
private String insert_text;
private String undo_text;
private boolean can_undo;
private boolean use_base;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BfixBaseEdit(BfixCorrector corr,int soff,int eoff,String ins,String undo) 
{
   for_corrector = corr;
   start_offset = soff;
   end_offset = eoff;
   insert_text = ins;
   undo_text = undo;
   can_undo = true;
   use_base = false;
}



public BfixBaseEdit(BfixCorrector corr,int soff,int eoff,String ins) 
{
   for_corrector = corr;
   start_offset = soff;
   end_offset = eoff;
   insert_text = ins;
   undo_text = null;
   can_undo = false;
   use_base = false;
}



public void useFileDocument()
{
   use_base = true;
}


/********************************************************************************/
/*                                                                              */
/*     Make the actual edit                                                     */
/*                                                                              */
/********************************************************************************/

@Override public void doEdit(boolean format,boolean indent)
{
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   if (use_base) {
      BaleFileOverview fov = doc.getBaseWindowDocument();
      fov.replace(start_offset,end_offset - start_offset,insert_text,
            format,indent);
    }
   else {
      doc.replace(start_offset,end_offset - start_offset, insert_text,
            format,indent);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Make/Unmake the edit in private buffer                                  */
/*                                                                              */
/********************************************************************************/

@Override public boolean makeEdit(String pid) 
{
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   String proj = doc.getProjectName();
   File file = doc.getFile();
   int soff = mapOffset(doc,start_offset);
   int eoff = mapOffset(doc,end_offset);
   BumpClient bc = BumpClient.getBump();
   bc.editPrivateFile(proj,file,pid,soff,eoff,insert_text);
   return true;
}


@Override public boolean unmakeEdit(String pid) 
{
   if (!can_undo) return false;
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   String proj = doc.getProjectName();
   File file = doc.getFile();
   int soff = mapOffset(doc,start_offset);
   int eoff = end_offset + (end_offset - start_offset);
   if (insert_text != null) eoff += insert_text.length();
   eoff = mapOffset(doc,eoff);
   String txt = undo_text;
   if (txt.isEmpty()) txt = null;
   BumpClient bc = BumpClient.getBump();
   bc.editPrivateFile(proj,file,pid,soff,eoff,txt);
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public int getDelta()
{
   int delta = start_offset - end_offset;
   if (insert_text != null) delta += insert_text.length();
   return delta;
}


/********************************************************************************/
/*                                                                              */
/*      Mapping methods                                                         */
/*                                                                              */
/********************************************************************************/

private int mapOffset(BaleWindowDocument doc,int off)
{
   if (use_base) {
      BaleFileOverview base = doc.getBaseWindowDocument();
      return base.mapOffsetToEclipse(off);
    }
   
   return doc.mapOffsetToEclipse(off);
}


/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString() 
{
   StringBuffer buf = new StringBuffer();
   buf.append("EDIT[");
   buf.append(start_offset);
   buf.append("-");
   buf.append(end_offset);
   if (can_undo) buf.append("*");
   if (insert_text != null) {
      buf.append(":");
      buf.append(insert_text);
    }
   buf.append("]");
   return buf.toString();
}


}       // end of class BfixBaseEdit




/* end of BfixBaseEdit.java */

