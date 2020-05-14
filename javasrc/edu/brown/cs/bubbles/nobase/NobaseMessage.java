/********************************************************************************/
/*										*/
/*		NobaseMessage.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.nobase;

import org.eclipse.jface.text.IDocument;

import java.util.ArrayList;
import java.util.List;

class NobaseMessage implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		the_message;
private ErrorSeverity	msg_severity;
private List<String>	additional_info;
private int		start_col;
private int		start_line;
private int		end_line;
private int		end_col;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseMessage(ErrorSeverity sev,String msg,int sln,int sco,int eln,int eco)
{
   msg_severity = sev;
   the_message = msg;
   additional_info = null;
   start_col = sco;
   start_line = sln;
   end_line = eln;
   end_col = eco;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public ErrorSeverity getSeverity()
{
   return msg_severity;
}


public int getStartLine(IDocument doc)
{
   return start_line;
}


public int getStartCol(IDocument doc)
{
   return start_col;
}


public int getEndLine(IDocument doc)
{
   return end_line;
}


public int getEndCol(IDocument doc)
{
   return end_col;
}

@Override public String toString()
{
   return getMessage();
}

public List<String> getAdditionalInfo()
{
   return additional_info;
}


public void addAdditionalInfo(String info)
{
   if (this.additional_info == null) {
      this.additional_info = new ArrayList<String>();
    }
   this.additional_info.add(info);
}

public String getMessage()
{
      return the_message;
}




}	// end of class NobaseMessage




/* end of NobaseMessage.java */

