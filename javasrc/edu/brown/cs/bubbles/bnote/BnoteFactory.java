/********************************************************************************/
/*										*/
/*		BnoteFactory.java						*/
/*										*/
/*	Factory for setting up programmers notebook storage			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bnote;

import edu.brown.cs.bubbles.board.BoardProperties;

import java.io.File;
import java.util.Date;
import java.util.List;


public class BnoteFactory implements BnoteConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BnoteStore		the_store;

private static BnoteFactory	the_factory = new BnoteFactory();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BnoteFactory()
{
   the_store = null;

   BoardProperties bp = BoardProperties.getProperties("Bnote");
   if (bp.getBoolean("Bnote.record",true)) {
      the_store = BnoteStore.createStore();
    }
}



public static BnoteFactory getFactory() 	{ return the_factory; }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   getFactory();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public boolean isEnabled()
{
   if (the_store == null) return false;

   return the_store.isEnabled();
}


public List<BnoteTask> getTasksForProject(String proj)
{
   if (the_store == null) return null;

   return the_store.getTasksForProject(proj);
}


public List<String> getUsersForTask(String proj,BnoteTask task)
{
   if (the_store == null) return null;

   return the_store.getUsersForTask(proj,task);
}


public List<Date> getDatesForTask(String proj,BnoteTask task)
{
   if (the_store == null) return null;

   return the_store.getDatesForTask(proj,task);
}


public List<String> getNamesForTask(String proj,BnoteTask task)
{
   if (the_store == null) return null;

   return the_store.getNamesForTask(proj,task);
}


public List<BnoteEntry> getEntriesForTask(String proj,BnoteTask task)
{
   if (the_store == null) return null;

   return the_store.getEntriesForTask(proj,task);
}



public BnoteTask findTaskById(int tid)
{
   if (the_store == null) return null;

   return the_store.findTaskById(tid);
}



public File getAttachment(String aid)
{
   if (the_store == null) return null;

   return the_store.getAttachment(aid);
}


public String getAttachmentAsString(String aid)
{
   if (the_store == null) return null;

   return the_store.getAttachmentAsString(aid);
}





}	// end of class BnoteFactory




/* end of BnoteFactory.java */

