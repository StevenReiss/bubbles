/********************************************************************************/
/*                                                                              */
/*              RebaseUtil.java                                                 */
/*                                                                              */
/*      Utility (mainly output) methods for REBASE                              */
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



package edu.brown.cs.bubbles.rebase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.text.edits.CopySourceEdit;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.CopyingRangeMarker;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;


public class RebaseUtil implements RebaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static int edit_counter = 0;   




/********************************************************************************/
/*                                                                              */
/*      Resource output                                                          */
/*                                                                              */
/********************************************************************************/

static void outputResourceDelta(String action,RebaseFile rf,IvyXmlWriter xw) 
{
   if (xw == null || rf == null) return;
   if (action == null) action = "CHANGED";
   
   xw.begin("DELTA");
   xw.field("KIND",action);
   xw.begin("RESOURCE");
   xw.field("TYPE","FILE");
   xw.field("PROJECT",rf.getProjectName());
   xw.field("LOCATION",rf.getFileName());
   xw.end("RESOURCE");
   xw.end("DELTA");
}



/********************************************************************************/
/*                                                                              */
/*      Text Edit output                                                        */
/*                                                                              */
/********************************************************************************/

static public void outputTextEdit(TextEdit te,IvyXmlWriter xw)
{
   xw.begin("EDIT");
   xw.field("OFFSET",te.getOffset());
   xw.field("LENGTH",te.getLength());
   xw.field("INCEND",te.getInclusiveEnd());
   xw.field("EXCEND",te.getExclusiveEnd());
   xw.field("ID",te.hashCode());
   xw.field("COUNTER",++edit_counter);
   
   if (te instanceof CopyingRangeMarker) {
      xw.field("TYPE","COPYRANGE");
    }
   else if (te instanceof CopySourceEdit) {
      CopySourceEdit cse = (CopySourceEdit) te;
      xw.field("TYPE","COPYSOURCE");
      xw.field("TARGET",cse.getTargetEdit().hashCode());
    }
   else if (te instanceof CopyTargetEdit) {
      xw.field("TYPE","COPYTARGET");
    }
   else if (te instanceof DeleteEdit) {
      xw.field("TYPE","DELETE");
    }
   else if (te instanceof InsertEdit) {
      InsertEdit ite = (InsertEdit) te;
      xw.field("TYPE","INSERT");
      xw.cdataElement("TEXT",ite.getText());
    }
   else if (te instanceof MoveSourceEdit) {
      MoveSourceEdit mse = (MoveSourceEdit) te;
      xw.field("TYPE","MOVESOURCE");
      xw.field("TARGET",mse.getTargetEdit().hashCode());
    }
   else if (te instanceof MoveTargetEdit) {
      xw.field("TYPE","MOVETARGET");
    }
   else if (te instanceof MultiTextEdit) {
      xw.field("TYPE","MULTI");
    }
   else if (te instanceof RangeMarker) {
      xw.field("TYPE","RANGEMARKER");
    }
   else if (te instanceof ReplaceEdit) {
      ReplaceEdit rte = (ReplaceEdit) te;
      xw.field("TYPE","REPLACE");
      xw.cdataElement("TEXT",rte.getText());
    }
   else if (te instanceof UndoEdit) {
      xw.field("TYPE","UNDO");
    }
   
   if (te.hasChildren()) {
      for (TextEdit cte : te.getChildren()) {
	 outputTextEdit(cte,xw);
       }
    }
   xw.end("EDIT");
}

}       // end of class RebaseUtil




/* end of RebaseUtil.java */

