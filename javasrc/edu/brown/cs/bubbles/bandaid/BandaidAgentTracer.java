/********************************************************************************/
/*										*/
/*		BandaidAgentTracer.java 					*/
/*										*/
/*	Agent to capture event trace for visualization				*/
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



package edu.brown.cs.bubbles.bandaid;

import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.ClassReader;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.ClassVisitor;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.ClassWriter;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.MethodVisitor;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.Opcodes;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.commons.CodeSizeEvaluator;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.util.CheckMethodAdapter;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.util.Textifier;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.util.TraceMethodVisitor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


public class BandaidAgentTracer extends BandaidAgent implements BandaidConstants,
	ClassFileTransformer
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,Map<String,TraceData>> trace_map;
private boolean 	trace_enabled;
private int		max_size;
private long		start_time;
private long		last_output;
private TraceSet []	trace_sets;
private int		current_set;
private ThreadMXBean	thread_bean;
private int		sequence_number;

private static BandaidAgentTracer the_tracer = null;

private static boolean	get_cpu_time = true;

private static boolean	do_debug = false;

private static final int	NUM_SETS = 3;
private static final int	ENTRY_COUNT = 10240;
private static final long	NULL_DELAY = 1000;

private static final int	ASM_API = Opcodes.ASM9;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidAgentTracer(BandaidController bc)
{
   super(bc,"Tracer");

   the_tracer = this;

   trace_enabled = true;
   max_size = ENTRY_COUNT*32;
   long mxmem = Runtime.getRuntime().maxMemory();
   max_size = Math.min(max_size,(int) (mxmem/100));

   start_time = System.nanoTime();
   last_output = 0;
   sequence_number = 0;

   trace_sets = new TraceSet[NUM_SETS];
   for (int i = 0; i < NUM_SETS; ++i) trace_sets[i] = new TraceSet();
   current_set = 0;

   thread_bean = ManagementFactory.getThreadMXBean();
   if (get_cpu_time && thread_bean.isCurrentThreadCpuTimeSupported()) {
      thread_bean.setThreadCpuTimeEnabled(true);
    }
   else get_cpu_time = false;

   trace_map = null;
}



/********************************************************************************/
/*										*/
/*     Agent interface								*/
/*										*/
/********************************************************************************/

@Override void enableMonitoring(boolean fg,long now)
{
   // System.err.println("TRACE ENABLED " + fg);
   trace_enabled = fg;
   super.enableMonitoring(fg,now);
}


@Override void generateReport(BandaidXmlWriter xw,long now)
{
   xw.begin("TRACE");
   xw.field("SEQ",++sequence_number);
   xw.field("TIME",now);
   xw.xmlText("");
   collectStatistics(xw);
   xw.end();
}




/********************************************************************************/
/*										*/
/*	Patching interface							*/
/*										*/
/********************************************************************************/

@Override ClassFileTransformer getTransformer()
{
   if (trace_enabled && trace_map == null) {
      String sf = the_control.getBaseDirectory();
      if (sf != null) {
	 trace_map = new HashMap<String,Map<String,TraceData>>();
	 File f1 = new File(sf);
	 f1 = new File(f1,TRACE_DATA_FILE);
	 loadTraceData(f1);
	 if (trace_map == null) trace_enabled = false;
       }
      else {
	 System.err.println("TRACE DISABLED");
	 trace_enabled = false;
       }
    }

   return this;
}


@Override public byte [] transform(ClassLoader ldr,String nm,Class<?> redef,ProtectionDomain dom,
      byte [] buf)
{
// System.err.println("BANDAID: TRACE " + nm + " " + ldr);
   if (trace_map == null) return null;

   Map<String,TraceData> tdm = trace_map.get(nm);

   if (tdm != null) {
      return patchClass(nm,buf,tdm);
    }

   return null;
}



private byte [] patchClass(String nm,byte [] buf,Map<String,TraceData> mthds)
{
   byte [] rsltcode = null;

   try {
      ClassReader reader = new ClassReader(buf);
      // ClassWriter writer = new ClassWriter(reader,ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
      ClassWriter writer = new BandaidClassWriter(nm,reader);
      ClassVisitor ins = new ClassTransformer(writer,mthds);
      reader.accept(ins,ClassReader.SKIP_FRAMES);
      rsltcode = writer.toByteArray();
    }
   catch (Throwable t) {
     // System.err.println("BANDAID: Problem instrumenting class: " + t);
     // t.printStackTrace();
    }

   return rsltcode;
}



private class ClassTransformer extends ClassVisitor {

   private Map<String,TraceData> method_data;
   private String class_name;
   private String super_name;
   private int class_access;

   ClassTransformer(ClassVisitor v,Map<String,TraceData> mthds) {
      super(ASM_API,v);
      method_data = mthds;
      class_name = null;
      super_name = null;
      class_access = 0;
    }

   @Override public void visit(int v,int a,String nm,String sgn,String sup,String [] ifc) {
      super.visit(v,a,nm,sgn,sup,ifc);
      class_name = nm;
      super_name = sup;
      class_access = a;
    }

   @Override public MethodVisitor visitMethod(int a,String nm,String d,String s,String [] exc) {
      MethodVisitor mv = super.visitMethod(a,nm,d,s,exc);
      String key = nm + d;
      key = key.replace('$','/');
      TraceData td = method_data.get(key);
      // System.err.println("TRACE CHECK " + class_name + " " + key + " " + td);
      if (td == null && nm.equals("<init>") && class_name.contains("$") &&
             (class_access & Opcodes.ACC_STATIC) == 0) {
         int idx = d.indexOf(";");
         key = nm + "(" + d.substring(idx+1);
         td = method_data.get(key);
       }
      // System.err.println("TRACE CHECK1 " + class_name + " " + key + " " + td);
      if (td != null) {
         if (do_debug) mv = new Tracer(mv,class_name + "." + key);
         mv = new CheckMethodAdapter(mv);
         mv = new CodeSizeEvaluator(mv);
         mv = new TracePatcher(mv,td,super_name);
       }
      return mv;
    }

}	// end of inner class ClassTransformer



/********************************************************************************/
/*										*/
/*	Load information on what to trace					*/
/*										*/
/********************************************************************************/

private void loadTraceData(File f)
{
   try (BufferedReader fr = new BufferedReader(new FileReader(f))) {
      for ( ; ; ) {
	 String ln = fr.readLine();
	 if (ln == null) break;
	 ln = ln.trim();
	 if (ln.length() == 0) continue;
	 if (ln.startsWith("#")) continue;
	 StringTokenizer tok = new StringTokenizer(ln);
	 int id = 0;
	 int fgs = 0;
	 int cargs = 0;
	 String cls = null;
	 String mthd = null;
	 String args = null;
	 try {
	    if (tok.hasMoreTokens()) {
	       id = Integer.parseInt(tok.nextToken());
	     }
	    if (tok.hasMoreTokens()) {
	       fgs = Integer.parseInt(tok.nextToken());
	     }
	    if (tok.hasMoreTokens()) {
	       cargs = Integer.parseInt(tok.nextToken());
	     }
	    if (tok.hasMoreTokens()) {
	       cls = tok.nextToken();
	     }
	    if (tok.hasMoreTokens()) {
	       mthd = tok.nextToken();
	     }
	    if (tok.hasMoreTokens()) {
	       args = tok.nextToken();
	     }

	    if (mthd != null && mthd.equals("<init>") && fgs != 4) continue;

	    if (mthd != null && cls != null && id > 0 && fgs != 0) {
	       String mkey = mthd;
	       if (args != null) mkey += args;
	       mkey = mkey.replace('$','/');
	       TraceData td = new TraceData(id,fgs,cargs);
	       Map<String,TraceData> tm = trace_map.get(cls);
	       // System.err.println("TRACER: Add class " + cls + " " + mkey);
	       if (tm == null) {
		  tm = new HashMap<String,TraceData>();
		  trace_map.put(cls,tm);
		}
	       // System.err.println("TRACE ENTER KEY " + mkey);
	       tm.put(mkey,td);
	     }
	  }
	 catch (NumberFormatException e) { }
       }
    }
   catch (IOException e) {
      trace_map = null;
    }
}




/********************************************************************************/
/*										*/
/*	TraceData -- information for instumentation/tracing			*/
/*										*/
/********************************************************************************/

private static class TraceData {

   private int trace_id;
   private int trace_flags;
   private int trace_args;

   TraceData(int id,int fg,int arg) {
      trace_id = id;
      trace_flags = fg;
      trace_args = arg;
    }

   int getTraceId()			{ return trace_id; }
   boolean traceOnEnter()		{ return (trace_flags & TRACE_ENTER) != 0; }
   boolean traceOnExit()		{ return (trace_flags & TRACE_EXIT) != 0; }
   boolean traceConstructor()		{ return (trace_flags & TRACE_CONSTRUCTOR) != 0; }
   int getTraceArgs()			{ return trace_args; }

}	// end of inner class TraceData



/********************************************************************************/
/*										*/
/*	Trace Patcher								*/
/*										*/
/********************************************************************************/

private class TracePatcher extends MethodVisitor {

   private TraceData trace_data;
   private String super_name;

   TracePatcher(MethodVisitor xmv,TraceData td,String sup) {
      super(ASM_API,xmv);
      trace_data = td;
      super_name = sup;
    }

   @Override public void visitCode() {
      if (trace_data.traceOnEnter()) {
	 generateTrace(false);
       }
      super.visitCode();
    }

   @Override public void visitInsn(int opc) {
      switch (opc) {
	 case Opcodes.IRETURN :
	 case Opcodes.LRETURN :
	 case Opcodes.FRETURN :
	 case Opcodes.DRETURN :
	 case Opcodes.ARETURN :
	 case Opcodes.RETURN :
	    if (trace_data.traceOnExit()) {
	       generateTrace(true);
	     }
	    break;
	 default :
	    break;
       }
      super.visitInsn(opc);
    }

   @Override public void visitMethodInsn(int opc,String own,String nm,String ds,boolean itf) {
      super.visitMethodInsn(opc,own,nm,ds,itf);
      if (super_name != null && nm.equals("<init>") && own.equals(super_name) &&
	    trace_data.traceConstructor() && opc == Opcodes.INVOKESPECIAL) {
	 generateTrace(false);
       }
    }

   private void generateTrace(boolean exit) {
      // TODO: need to handle insertion into Main so that we don't lose the initial
      //   argument
      int v = trace_data.getTraceId();
      if (exit) v = -v;
      super.visitLdcInsn(Integer.valueOf(v));
      int args = trace_data.getTraceArgs();
      // System.err.println("PATCH " + v + " " + args);
      String atyp = "(I";
      for (int i = 0; args != 0; ++i) {
         if ((args & 1) != 0) {
            super.visitVarInsn(Opcodes.ALOAD,i);
            atyp += "Ljava/lang/Object;";
          }
         args >>>= 1;
       }
      atyp += ")V";
      super.visitMethodInsn(Opcodes.INVOKESTATIC,"edu/brown/cs/bubbles/bandaid/BandaidAgentTracer",
            "traceEntry",atyp,false);
    }

}	// end of inner class TracePatcher




/********************************************************************************/
/*										*/
/*	Debugging methods and classes						*/
/*										*/
/********************************************************************************/

private static class Tracer extends MethodVisitor {

   private String method_name;

   Tracer(MethodVisitor v,String who) {
      super(ASM_API,new TraceMethodVisitor(v,new Textifier()));
      method_name = who;
    }

   @Override public void visitEnd() {
      TraceMethodVisitor tmv = (TraceMethodVisitor) this.mv;
      List<?> tx = tmv.p.getText();
      System.err.println("TRACE METHOD " + method_name);
      for (Object o : tx) {
         System.err.print(o.toString());
       }
    }

   @Override public String toString() {
      return "TRACE_METHOD " + method_name;
    }

}	// end of inner class Tracer




/********************************************************************************/
/*										*/
/*	Trace Entry Points							*/
/*										*/
/********************************************************************************/

public static void traceEntry(int id)
{
   the_tracer.addEntry(id,null,null);
   // System.err.println("TRACE " + id);
}


public static void traceEntry(int id,Object o1)
{
   the_tracer.addEntry(id,o1,null);
   // System.err.println("TRACE " + id + " " + System.identityHashCode(o1));
}


public static void traceEntry(int id,Object o1,Object o2)
{
   the_tracer.addEntry(id,o1,o2);
   // System.err.println("TRACE " + id + " " + System.identityHashCode(o1) + " " + System.identityHashCode(o2));
}



private void addEntry(int id,Object o1,Object o2)
{
   // System.err.println("TRACE " + id + " " + current_set + " " + trace_enabled);

   if (!trace_enabled) return;

   try {
      long time = System.nanoTime() - start_time;
      Thread th = Thread.currentThread();
      int m1 = (o1 == null ? 0 : System.identityHashCode(o1));
      int m2 = (o2 == null ? 0 : System.identityHashCode(o2));

      trace_sets[current_set].addEntry(th,id,m1,m2,time);
    }
   catch (Throwable t) {
      // System.err.println("TRACE error " + t);
      // t.printStackTrace();
    }
}



private void collectStatistics(Writer w)
{
   long now = System.nanoTime() - start_time;
   int next = (current_set + 1) % NUM_SETS;
   int prior = (current_set + NUM_SETS - 1) % NUM_SETS;
   trace_sets[next].reset();
   TraceSet eval = trace_sets[prior];
   current_set = next;

   try {
      if (w != null) {
	 boolean out = eval.output(w);
	 // System.err.println("TRACEOUT " + prior + " " + out);
	 if (out || now - last_output > NULL_DELAY * 1000000) {
	    w.append("DONE ");
	    w.append(Long.toString(now));
	    w.append("\n");
	    last_output = now;
	  }
       }
    }
   catch (IOException e) { }
}




/********************************************************************************/
/*										*/
/*	Trace Set -- top level holder of current trace information		*/
/*										*/
/********************************************************************************/

private class TraceSet {

   private HashMap<Thread,ThreadSet> thread_map;
   private long last_cputime;

   TraceSet() {
      thread_map = new HashMap<>();
      last_cputime = 0;
    }

   void addEntry(Thread th,int id,int mid,int mid2,long time) {
      StringBuilder buf = new StringBuilder();
      buf.append(id);
      buf.append(" ");
      buf.append(time);
      if (get_cpu_time) {
	 long tcp = thread_bean.getCurrentThreadCpuTime();
	 tcp -= last_cputime;
	 last_cputime += tcp;
	 buf.append(" ");
	 buf.append(tcp);
       }
      if (mid != 0) {
	 buf.append(" ");
	 buf.append(mid);
       }
      if (mid2 != 0) {
	 buf.append(" ");
	 buf.append(mid2);
       }
      getThreadSet(th).addEntry(buf);
    }

   void reset() {
      for (Iterator<Map.Entry<Thread,ThreadSet>> it = thread_map.entrySet().iterator(); it.hasNext(); ) {
	 Map.Entry<Thread,ThreadSet> ent = it.next();
	 if (ent.getKey().isAlive()) ent.getValue().reset();
	 else it.remove();
       }
    }

   boolean output(Writer w) throws IOException {
      boolean fg = false;
      for (Map.Entry<Thread,ThreadSet> ent : thread_map.entrySet()) {
         ThreadSet ts = ent.getValue();
         if (ts.containsData()) {
            if (!fg) {
               w.append("START " + get_cpu_time + "\n");
             }
            Thread th = ent.getKey();
            w.append("THREAD " + System.identityHashCode(th) + " " +
        		BandaidController.getThreadId(th) + " " +
        	   th.getName() + "\n");
            fg = true;
            ts.output(w);
          }
       }
      return fg;
    }

   ThreadSet getThreadSet(Thread th) {
      ThreadSet ts = thread_map.get(th);
      if (ts == null) {
	 ts = new ThreadSet();
	 synchronized (this) {
	    thread_map.put(th,ts);
	  }
       }
      return ts;
    }

}	// end of inner class TraceSet




/********************************************************************************/
/*										*/
/*	Thread Set -- holder of trace entries for a thread			*/
/*										*/
/********************************************************************************/

private class ThreadSet {

   private StringBuilder output_data;

   ThreadSet() {
      output_data = new StringBuilder(max_size);
    }

   void addEntry(CharSequence s) {
      if (output_data.length() > max_size - 32) return;
      output_data.append(s);
      output_data.append('\n');
    }

   boolean containsData() {
      return output_data.length() > 0;
    }

   void reset() {
      int ln = output_data.length();
      output_data.delete(0,ln);
      if (ln+10 > max_size) max_size = ln+10;
    }

   void output(Writer w) throws IOException {
      w.append(output_data);
    }

}	// end of inner class ThreadSet



}	// end of class BandaidAgentTracer




/* end of BandaidAgentTracer.java */

