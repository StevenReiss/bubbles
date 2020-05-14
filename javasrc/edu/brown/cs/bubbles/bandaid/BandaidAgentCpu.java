/********************************************************************************/
/*										*/
/*		BandaidAgentCpu.java						*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data cpu performance agent	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bandaid;



import java.lang.management.ThreadInfo;
import java.util.HashMap;
import java.util.Map;


class BandaidAgentCpu extends BandaidAgent implements BandaidConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,PerfCounter> counter_map;
private Map<String,PerfCounter> current_map;
private long			last_sample;
private long			last_delta;
private long			sample_count;
private long			active_samples;
private boolean 		have_active;
private long			item_total;
private long			item_base;
private long			total_active;


private StackTraceElement []	sys_stack;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidAgentCpu(BandaidController bc)
{
   super(bc,"Cpu");

   last_sample = 0;
   last_delta = 0;
   sample_count = 0;
   have_active = false;
   active_samples = 0;
   item_total = 0;
   item_base = 0;
   total_active = 0;

   counter_map = new HashMap<String,PerfCounter>();
   current_map = null;

   sys_stack = new StackTraceElement[1024];
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   if (ti.getThreadState() != Thread.State.RUNNABLE) return;
   if (ti.isSuspended()) return;

   if (last_sample != now) {
      ++sample_count;
      current_map = counter_map;
      if (last_sample == 0) last_delta = 0;
      else last_delta = now - last_sample;
      last_sample = now;
      have_active = false;
    }

   int depth = 0;
   boolean haveuser = false;
   boolean isio = false;
   boolean issys = false;

   int scnt = 0;
   for (int j = 0; j < trc.length; ++j) {
      StackTraceElement te = trc[j];
      String nm = te.getClassName();
      if (j == 0 && the_control.isIOClass(nm)) {
	 isio = true;
	 continue;
       }
      if (!haveuser && te.isNativeMethod()) continue;
      if (the_control.isSystemClass(nm)) {
	 if (haveuser) sys_stack[scnt++] = te;
	 else issys = true;
	 continue;
       }
      if (!have_active && !isio) {
	 have_active = true;
	 total_active += last_delta;
	 ++active_samples;
       }
      haveuser = true;
      if (scnt > 0) {
	 for (int k = 0; k < scnt; ++k) {
	    handleStackItem(now,sys_stack[k],null,depth++,isio,issys,true);
	    sys_stack[k] = null;
	  }
	 scnt = 0;
       }
      handleStackItem(now,te,nm,depth++,isio,issys,false);
    }

   for (int k = 0; k < scnt; ++k) sys_stack[k] = null;
}




private void handleStackItem(long now,StackTraceElement te,String nm,int depth,
				boolean isio,boolean issys,boolean thissys)
{
   if (isio) return;

   if (nm == null) nm = te.getClassName();
   nm = nm + "@" + te.getMethodName();
   nm = nm + "@" + te.getLineNumber();
   String fn = te.getFileName();
   if (fn != null) nm = nm + "@" + fn;

   PerfCounter lc = current_map.get(nm);
   if (lc == null) {
      lc = new PerfCounter();
      current_map.put(nm,lc);
    }
   lc.incr(depth);

   item_total++;
   if (depth == 0) ++item_base;
}




@Override void handleDoneStacks(long now)
{ }




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override void generateReport(BandaidXmlWriter xw,long now)
{
   xw.begin("CPUPERF");
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("ACTIVE",active_samples);
   xw.field("LAST",last_sample);
   xw.field("TIME",total_active);

   xw.begin("TOTALS");
   xw.field("TOTAL",item_total);
   xw.field("BASE",item_base);
   xw.end();

   if (counter_map != null) {
      for (Map.Entry<String,PerfCounter> ent : counter_map.entrySet()) {
	 ent.getValue().report(ent.getKey(),xw);
       }
    }

   xw.end();
}



/********************************************************************************/
/*										*/
/*	PerfCounter class							*/
/*										*/
/********************************************************************************/

private static class PerfCounter {

   private long total_count;
   private long base_count;
   private boolean has_changed;

   PerfCounter() {
      total_count = 0;
      base_count = 0;
      has_changed = true;
    }

   void incr(int lvl) {
      total_count++;
      if (lvl == 0) base_count++;
      has_changed = true;
    }

   void report(String nm,BandaidXmlWriter xw) {
      if (!has_changed) return;
      has_changed = false;
      xw.begin("ITEM");
      xw.field("NAME",nm);
      xw.field("TOTAL",total_count);
      xw.field("BASE",base_count);
      xw.end();
    }

}	// end of subclass PerfCounter





}	 // end of class BandaidAgentCpu




/* end of BandaidAgentCpu.java */
