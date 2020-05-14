/********************************************************************************/
/*										*/
/*		BedrockProgressMonitor.java					*/
/*										*/
/*	Progress monitor for Bubbles - Eclipse interface			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */


/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


class BedrockProgressMonitor extends NullProgressMonitor {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin	for_plugin;
private String		task_name;
private String		subtask_name;
private double		total_work;
private double		work_done;
private boolean 	is_done;
private String		task_id;

private static AtomicInteger task_counter = new AtomicInteger(1);
private static AtomicLong    serial_number = new AtomicLong(1);



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockProgressMonitor(BedrockPlugin bp,String nm)
{
   for_plugin = bp;
   task_name = nm;
   subtask_name = null;
   total_work = UNKNOWN;
   work_done = 0;
   is_done = false;

   task_id = Integer.toString(task_counter.incrementAndGet());

   BedrockPlugin.log("Progress Monitor " + nm + " " + task_id);
}



/********************************************************************************/
/*										*/
/*	Monitor methods 							*/
/*										*/
/********************************************************************************/

@Override public void beginTask(String name,int total)
{
   super.beginTask(name,total);

   if (name != null && name.length() > 0) task_name = name;
   subtask_name = null;
   total_work = total;
   work_done = 0;
   if (task_name == null) return;

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","BEGIN");
   xw.field("TASK",task_name);
   xw.field("ID",task_id);
   xw.field("S",serial_number.incrementAndGet());
   for_plugin.finishMessage(xw);
}



public void finish()
{
   // this is called to ensure that a done() call is invoked.  Eclipse
   // made that optional as of 4.7, but outside sources might depend
   // on it

   if (!is_done) done();
}



@Override public void done()
{
   super.done();

   if (task_name == null) return;

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","DONE");
   xw.field("TASK",task_name);
   xw.field("ID",task_id);
   xw.field("S",serial_number.incrementAndGet());
   for_plugin.finishMessage(xw);

   task_name = null;
   subtask_name = null;

   synchronized (this) {
      is_done = true;
      notifyAll();
    }
}



@Override public void setCanceled(boolean v)
{
   super.setCanceled(v);

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   if (v) xw.field("KIND","CANCEL");
   else xw.field("KIND","UNCANCEL");
   xw.field("TASK",task_name);
   xw.field("SUBTASK",subtask_name);
   xw.field("ID",task_id);
   xw.field("S",serial_number.incrementAndGet());
   for_plugin.finishMessage(xw);
}




@Override public void setTaskName(String name)
{
   super.setTaskName(name);

   subtask_name = null;

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","ENDSUBTASK");
   xw.field("TASK",task_name);
   xw.field("ID",task_id);
   xw.field("S",serial_number.incrementAndGet());
   for_plugin.finishMessage(xw);
}




@Override public void subTask(String name)
{
   super.subTask(name);

   subtask_name = name;

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","SUBTASK");
   xw.field("TASK",task_name);
   xw.field("SUBTASK",name);
   xw.field("ID",task_id);
   xw.field("S",serial_number.incrementAndGet());
   for_plugin.finishMessage(xw);
}




@Override public void worked(int w)
{
   super.worked(w);

   if (total_work == UNKNOWN) return;

   work_done += w;

   double v = work_done / total_work;
   double v0 = (v*100);
   if (v0 < 0) v0 = 0;
   if (v0 > 100) v0 = 100;

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","WORKED");
   xw.field("TASK",task_name);
   xw.field("SUBTASK",subtask_name);
   xw.field("ID",task_id);
   xw.field("WORK",v0);
   xw.field("S",serial_number.incrementAndGet());
   for_plugin.finishMessage(xw);
}



/********************************************************************************/
/*										*/
/*	Waiting methods 							*/
/*										*/
/********************************************************************************/

synchronized void waitFor()
{
   while (!is_done) {
      try {
	 wait();
       }
      catch (InterruptedException e) { }
    }
}



}	// end of class BedrockProgressMonitor




/* end of BedrockProgressMonitor.java */
