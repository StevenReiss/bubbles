/********************************************************************************/
/*                                                                              */
/*              RebaseMessage.java                                              */
/*                                                                              */
/*      description of class                                                    */
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


public class RebaseMessage implements RebaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RebaseFile              for_file;
private MessageSeverity         message_severity;
private int                     message_id;
private String                  message_text;
private int                     line_number;
private int                     start_offset;
private int                     end_offset;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RebaseMessage(RebaseFile rf,MessageSeverity lvl,int id,String txt,
      int line,int start,int end)
{
   for_file = rf;
   message_severity = lvl;
   message_id = id;
   message_text = txt;
   line_number = line;
   start_offset = start;
   end_offset = end;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputProblem(IvyXmlWriter xw)
{
   xw.begin("PROBLEM");
   xw.field("MSGID",message_id);
   if (for_file != null) xw.field("FILE",for_file.getFileName());
   xw.field("LINE",line_number);
   xw.field("START",start_offset);
   xw.field("END",end_offset);
   xw.field(message_severity.toString(),true);
   xw.textElement("MESSAGE",message_text);
   xw.end("PROBLEM");
}



}       // end of class RebaseMessage




/* end of RebaseMessage.java */

