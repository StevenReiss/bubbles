/********************************************************************************/
/*										*/
/*		BandaidAgentSwing.java						*/
/*										*/
/*	Swing debugging assistant agent 					*/
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
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.util.Textifier;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.util.TraceMethodVisitor;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;


public class BandaidAgentSwing extends BandaidAgent implements BandaidConstants,
		ClassFileTransformer
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Map<Object,StackTraceElement []>     create_map;

private static final int ASM_API = Opcodes.ASM9;

private static boolean do_debug = false;


static {
   create_map = new WeakHashMap<Object,StackTraceElement []>();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidAgentSwing(BandaidController bc)
{
   super(bc,"Swing");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override ClassFileTransformer getTransformer()
{
   return this;
}



/********************************************************************************/
/*										*/
/*	Command handling							*/
/*										*/
/********************************************************************************/

@Override void handleCommand(String cmd,String args)
{
   if (cmd.equals("WHAT")) {
      Scanner scn = new Scanner(args);
      int x = scn.nextInt();
      int y = scn.nextInt();
      BandaidXmlWriter xw = new BandaidXmlWriter();
      xw.begin("BANDAID");
      xw.field("SWING",the_control.getProcessId());
      Correlator c = new Correlator();
      if (c.correlate(xw,x,y)) {
	 xw.end();
	 the_control.sendMessage(xw.getContents());
       }
      else {
	 xw.field("FAIL",true);
	 xw.end();
	 the_control.sendMessage(xw.getContents());
       }
      scn.close();
    }
}



/********************************************************************************/
/*										*/
/*	Component allocation methods						*/
/*										*/
/********************************************************************************/

public static void handleComponent(Object c)
{
   StackTraceElement [] elts = Thread.currentThread().getStackTrace();
   // System.err.println("CREATE COMPONENT " + c.getClass());
   create_map.put(c,elts);
}




/********************************************************************************/
/*										*/
/*	Class to handle correlation						*/
/*										*/
/*	Any access to awt/swing needs to be in here				*/
/*										*/
/********************************************************************************/

private final class Correlator {

   boolean correlate(BandaidXmlWriter xw,int x,int y) {
      Window [] wins = Window.getWindows();
      Window best = null;
      Point bestpt = null;
      for (Window w : wins) {
	 Point pt = new Point(x,y);
	 SwingUtilities.convertPointFromScreen(pt,w);
	 if (pt.x < 0 || pt.x >= w.getWidth()) continue;
	 if (pt.y < 0 || pt.y >= w.getHeight()) continue;
	 if (w.getGraphics() == null) continue;
	 best = w;
	 bestpt = pt;
       }
      if (best == null || bestpt == null) return false;
      Component c = SwingUtilities.getDeepestComponentAt(best,bestpt.x,bestpt.y);
      if (c == null) return false;

      xw.begin("SWINGDATA");

      outputComponentData(xw,c);

      Rectangle where = new Rectangle(bestpt.x,bestpt.y,1,1);

      BandaidGraphics bg = new BandaidGraphics(where,best);
      try {
	 best.paint(bg);
	 String s = bg.getResult();
	 xw.write(s);
       }
      catch (Throwable t) {
	 System.err.println("BANDAID: Problem computing drawing information: " + t);
	 t.printStackTrace();
       }

      xw.end();

      return true;
    }

   private void outputComponentData(BandaidXmlWriter xw,Component c) {
      if (c == null) return;

      xw.begin("COMPONENT");
      xw.field("X",c.getX());
      xw.field("Y",c.getY());
      xw.field("W",c.getWidth());
      xw.field("H",c.getHeight());
      xw.field("CLASS",c.getClass().getName());
      if (c.getName() != null) xw.field("NAME",c.getName());
      if (c.getParent() != null) {
	 outputComponentData(xw,c.getParent());
       }

      StackTraceElement [] elts = create_map.get(c);
      if (elts != null) {
	 xw.begin("CREATE");
	 for (int i = 2; i < elts.length; ++i) {
	    xw.begin("FRAME");
	    xw.field("CLASS",elts[i].getClassName());
	    xw.field("METHOD",elts[i].getMethodName());
	    xw.field("FILE",elts[i].getFileName());
	    xw.field("LINE",elts[i].getLineNumber());
	    xw.end();
	  }
	 xw.end();
       }

      xw.end();
    }

}	// end of inner class Correlator




/********************************************************************************/
/*										*/
/*	Transformer methods							*/
/*										*/
/********************************************************************************/

@Override public byte [] transform(ClassLoader ldr,String nm,Class<?> redef,ProtectionDomain dom,
				   byte [] buf)
{
   if (nm != null && nm.equals("java/awt/Component")) return patchComponent(buf);

   return null;
}


private byte [] patchComponent(byte [] buf)
{
   byte [] rsltcode = null;
   try {
      ClassReader reader = new ClassReader(buf);
      // ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
      ClassWriter writer = new BandaidClassWriter("java/awt/Component",reader);
      ClassVisitor ins = new ComponentTransformer(writer);
      reader.accept(ins,ClassReader.SKIP_FRAMES);
      rsltcode = writer.toByteArray();
    }
   catch (Throwable t) {
      System.err.println("BANDAID: Problem instrumenting Component: " + t);
      t.printStackTrace();
    }

   return rsltcode;
}



/********************************************************************************/
/*										*/
/*	Transform for java.awt.Component					*/
/*										*/
/********************************************************************************/

private CodeSizeEvaluator	size_eval;

private class ComponentTransformer extends ClassVisitor {

   private String super_name;

   ComponentTransformer(ClassVisitor v) {
      super(ASM_API,v);
    }

   @Override public void visit(int v,int a,String nm,String sgn,String sup,String [] ifc) {
      super.visit(v,a,nm,sgn,sup,ifc);
      super_name = sup;
    }

   @Override public MethodVisitor visitMethod(int a,String nm,String d,String s,String [] exc) {
      MethodVisitor mv = super.visitMethod(a,nm,d,s,exc);
      if (!nm.equals("<init>")) return mv;
      if (do_debug)  mv = new Tracer(mv,nm + d);
      mv = new ComponentPatcher(mv,super_name);
      size_eval = new CodeSizeEvaluator(mv);
      mv = size_eval;
      return mv;
    }

}	// end of inner class ComponentTransformer


private class ComponentPatcher extends MethodVisitor {

   private String super_class;

   ComponentPatcher(MethodVisitor v,String sc) {
      super(ASM_API,v);
      super_class = sc;
    }

   @Override public void visitMethodInsn(int opc,String own,String nm,String ds,boolean itf) {
      super.visitMethodInsn(opc,own,nm,ds,itf);
      if (super_class != null && nm.equals("<init>") && own.equals(super_class)) {
	 super_class = null;
	 super.visitVarInsn(Opcodes.ALOAD,0);
	 super.visitMethodInsn(Opcodes.INVOKESTATIC,"edu/brown/cs/bubbles/bandaid/BandaidAgentSwing",
				  "handleComponent","(Ljava/lang/Object;)V",false);
       }
    }

}	// end of inner class ComponentPatcher




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

}	// end of inner class Tracer





}	// end of class BandaidAgentSwing




/* end of BandaidAgentSwing.java */




