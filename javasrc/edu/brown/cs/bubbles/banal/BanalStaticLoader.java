/********************************************************************************/
/*										*/
/*		BanalStaticLoader.java						*/
/*										*/
/*	Bubbles ANALysis package class data loader using ASM			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.bubbles.org.objectweb.asm.AnnotationVisitor;
import edu.brown.cs.bubbles.org.objectweb.asm.Attribute;
import edu.brown.cs.bubbles.org.objectweb.asm.ClassReader;
import edu.brown.cs.bubbles.org.objectweb.asm.ClassVisitor;
import edu.brown.cs.bubbles.org.objectweb.asm.FieldVisitor;
import edu.brown.cs.bubbles.org.objectweb.asm.Label;
import edu.brown.cs.bubbles.org.objectweb.asm.MethodVisitor;
import edu.brown.cs.bubbles.org.objectweb.asm.Opcodes;
import edu.brown.cs.bubbles.org.objectweb.asm.Type;

import edu.brown.cs.ivy.file.IvyFormat;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


class BanalStaticLoader implements BanalConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BanalProjectManager	project_manager;
private BanalVisitor		user_visitor;
private Map<String,AsmClass>	class_map;
private Map<String,AsmField>	field_map;
private Map<String,AsmMethod>	method_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BanalStaticLoader(BanalProjectManager bpm,BanalVisitor vis)
{
   project_manager = bpm;
   user_visitor = vis;
   class_map = new HashMap<String,AsmClass>();
   field_map = new HashMap<String,AsmField>();
   method_map = new HashMap<String,AsmMethod>();
}




/********************************************************************************/
/*										*/
/*	Asm Processing								*/
/*										*/
/********************************************************************************/

void process()
{
   user_visitor.begin();

   for (BanalClassData bcd : project_manager.getClassData()) {
      if (!user_visitor.checkUseProject(bcd.getProject())) continue;
      if (!user_visitor.checkUseClass(bcd.getName())) continue;
      processClass(bcd);
    }
					
   user_visitor.finish();
}



private void processClass(BanalClassData cd)
{
   InputStream ins = cd.getClassStream();
   if (ins == null) return;

   AsmClassVisitor acv = new AsmClassVisitor(cd.getProject());

   try {
      ClassReader cr = new ClassReader(ins);
      cr.accept(acv,0);
    }
   catch (IOException e) {
      System.err.println("BANAL: Problem reading class file: " + e);
    }
   finally {
      try {
	 ins.close();
       }
      catch (IOException e) { }
    }
}



/********************************************************************************/
/*										*/
/*	Class Access methods							*/
/*										*/
/********************************************************************************/

private AsmClass findClass(String id,boolean descript)
{
   AsmClass ac = class_map.get(id);
   if (ac == null) {
      ac = new AsmClass(id,descript);
      class_map.put(ac.getInternalName(),ac);
      class_map.put(ac.getJavaName(),ac);
    }
   return ac;
}



private AsmClass findInternalClass(String id,boolean descript)
{
   if (!id.endsWith(";")) id = "L" + id + ";";

   return findClass(id,true);
}



private AsmField findField(AsmClass cls,String nm)
{
   String key = cls.getJavaName() + "@" + nm;

   AsmField af = field_map.get(key);
   if (af == null) {
      af = new AsmField(cls,nm);
      field_map.put(key,af);
    }

   return af;
}



private AsmMethod findMethod(AsmClass cls,String nm,AsmClass mcls)
{
   String key = cls.getJavaName() + "@" + nm + " @" + mcls.getJavaName();

   AsmMethod am = method_map.get(key);
   if (am == null) {
      am = new AsmMethod(cls,nm,mcls);
      method_map.put(key,am);
    }

   return am;
}




/********************************************************************************/
/*										*/
/*	Asm processing								*/
/*										*/
/********************************************************************************/

private class AsmClassVisitor extends ClassVisitor {

   private AsmClass asm_data;

   AsmClassVisitor(String proj) {
      super(ASM_API);
      asm_data = null;
    }

   @Override public void visit(int version,int access,String name,String sign,String sup,
				  String [] ifcs) {
      asm_data = findInternalClass(name,false);
      user_visitor.visitClass(asm_data,sign,access);
      if (sup != null) user_visitor.visitSuper(asm_data,findInternalClass(sup,false),false);
      if (ifcs != null) {
	 for (String xf : ifcs) {
	    user_visitor.visitSuper(asm_data,findInternalClass(xf,false),true);
	  }
       }
    }

   @Override public AnnotationVisitor visitAnnotation(String dsc,boolean vis) {
      AsmClass acls = findClass(dsc,true);
      user_visitor.visitClassAnnotation(asm_data,acls,vis);
      return new AsmAnnotationVisitor(acls,asm_data,null,null);
    }

   @Override public void visitInnerClass(String n,String o,String i,int acc) {
      String nm = asm_data.getJavaName();
      if (o != null) o = o.replace('/','.');
      if (o == null || nm.equals(o)) {
	 user_visitor.visitInnerClass(asm_data,findInternalClass(n,false),acc);
       }
    }

   @Override public void visitOuterClass(String n,String o,String i)	{ }
   @Override public void visitAttribute(Attribute a)			{ }
   @Override public void visitEnd()					{ }
   @Override public void visitSource(String src,String dbg)		{ }

   @Override public FieldVisitor visitField(int access,String name,String desc,String sign,
					       Object val) {
      AsmField af = new AsmField(asm_data,name);
      user_visitor.visitClassField(af,findClass(desc,true),sign,access,val);
      return new AsmFieldVisitor(af);
    }

   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sign,
						 String [] excs) {
      AsmClass mcls = findClass(desc,true);
      AsmMethod mthd = findMethod(asm_data,name,mcls);
      AsmClass [] ecls;
      if (excs == null) ecls = new AsmClass[0];
      else {
	 ecls = new AsmClass[excs.length];
	 for (int i = 0; i < excs.length; ++i) {
	    ecls[i] = findInternalClass(excs[i],false);
	  }
       }
      user_visitor.visitClassMethod(mthd,sign,access,ecls);
      return new AsmMethodVisitor(mthd);
    }

}	// end of AsmClassVisitor




private class AsmAnnotationVisitor extends AnnotationVisitor {

   private AsmClass annot_class;
   private AsmClass base_class;
   private AsmField base_field;
   private AsmMethod base_method;

   AsmAnnotationVisitor(AsmClass annot,AsmClass base,AsmField fld,AsmMethod mthd) {
      super(ASM_API);
      base_class = base;
      base_field = fld;
      base_method = mthd;
      annot_class = annot;
    }

   @Override public void visit(String name,Object value) {
      if (base_class != null)
	 user_visitor.visitAnnotationValue(base_class,annot_class,name,value);
      else if (base_field != null)
	 user_visitor.visitAnnotationValue(base_field,annot_class,name,value);
      else if (base_method != null)
	 user_visitor.visitAnnotationValue(base_method,annot_class,name,value);
    }

   @Override public AnnotationVisitor visitAnnotation(String name,String desc)	{ return null; }
   @Override public AnnotationVisitor visitArray(String name)			{ return null; }

   @Override public void visitEnd()	{ }
   @Override public void visitEnum(String name,String desc,String value) {
      visit(name,value);
    }

}	// end of inner class AsmAnnotationVisitor




private class AsmFieldVisitor extends FieldVisitor {

   private AsmField for_field;

   AsmFieldVisitor(AsmField fld) {
      super(ASM_API);
      for_field = fld;
    }

   @Override public AnnotationVisitor visitAnnotation(String desc,boolean vis) {
      AsmClass acls = findClass(desc,true);
      user_visitor.visitFieldAnnotation(for_field,acls,vis);
      return new AsmAnnotationVisitor(acls,null,for_field,null);
    }

   @Override public void visitAttribute(Attribute a)			{ }
   @Override public void visitEnd()					{ }

}	// end of inner class AsmFieldVisitor





private class AsmMethodVisitor extends MethodVisitor {

   private AsmMethod for_method;

   AsmMethodVisitor(AsmMethod mthd) {
      super(ASM_API);
      for_method = mthd;
    }

   @Override public AnnotationVisitor visitAnnotation(String dsc,boolean vis) {
      AsmClass acls = findClass(dsc,true);
      user_visitor.visitMethodAnnotation(for_method,acls,vis);
      return new AsmAnnotationVisitor(acls,null,null,for_method);
    }

   @Override public void visitFieldInsn(int op,String owner,String name,String desc) {
      AsmClass fcls = findInternalClass(owner,false);
      if (fcls != for_method.getOwnerClass()) {
	 BanalField fld = findField(fcls,name);
	 user_visitor.visitRemoteFieldAccess(for_method,fld);
       }
    }

   @Override public void visitLocalVariable(String name,String desc,String sgn,Label st,
					       Label en,int idx) {
      AsmClass fcls = findClass(desc,true);
      if (!fcls.isPrimitive() && fcls != for_method.getOwnerClass()) {
	 user_visitor.visitRemoteTypeAccess(for_method,fcls);
       }
    }

   @Override public void visitMethodInsn(int op,String own,String nam,String desc,boolean itf) {
      AsmClass ocls = findInternalClass(own,false);
      AsmClass mcls = findClass(desc,true);
      AsmMethod mthd = findMethod(ocls,nam,mcls);
      user_visitor.visitCall(for_method,mthd);
    }


   @Override public void visitTypeInsn(int op,String typ) {
      if (op == Opcodes.NEW) {
	 AsmClass acls = findInternalClass(typ,false);
	 user_visitor.visitAlloc(for_method,acls);
       }
    }


   @Override public void visitTryCatchBlock(Label s,Label e,Label h,String typ) {
      if (typ == null) return;
      AsmClass acls = findInternalClass(typ,false);
      user_visitor.visitCatch(for_method,acls);
    }

   @Override public AnnotationVisitor visitParameterAnnotation(int p,String d,boolean v) {
      return null;
    }
   @Override public void visitEnd()					{ }
   @Override public void visitAttribute(Attribute a)			{ }
   @Override public void visitMultiANewArrayInsn(String desc,int dims)	{ }
   @Override public void visitLineNumber(int l,Label s) 		{ }
   @Override public AnnotationVisitor visitAnnotationDefault()		{ return null; }
   @Override public void visitCode()					{ }
   @Override public void visitFrame(int t,int n,Object [] l,int ns,Object [] s) { }
   @Override public void visitIincInsn(int v,int i)			{ }
   @Override public void visitInsn(int o)				{ }
   @Override public void visitIntInsn(int o,int v)			{ }
   @Override public void visitJumpInsn(int o,Label l)			{ }
   @Override public void visitLabel(Label l)				{ }
   @Override public void visitLdcInsn(Object v) 			{ }
   @Override public void visitLookupSwitchInsn(Label d,int [] k,Label [] l)	{ }
   @Override public void visitMaxs(int m,int ml)			{ }
   @Override public void visitTableSwitchInsn(int mn,int mx,Label d,Label... l) { }
   @Override public void visitVarInsn(int o,int v)			{ }

}	// end of AsmMethodVisitor







/********************************************************************************/
/*										*/
/*	Representation of a class						*/
/*										*/
/********************************************************************************/

private static class AsmClass implements BanalClass {

   private String class_name;
   private String java_name;

   AsmClass(String nm,boolean descript) {
      if (descript) {
	 class_name = nm;
	 java_name = IvyFormat.formatTypeName(nm,true);
       }
      else {
	 java_name = nm.replace('/','.');
	 class_name = "L" + nm.replace('.','/') + ";";
       }
    }

   @Override public String getInternalName()		{ return class_name; }
   @Override public String getJavaName()		{ return java_name; }

   boolean isPrimitive() {
      if (class_name.length() == 1) return true;
      for (int i = 0; i < class_name.length(); ++i) {
	 if (class_name.charAt(i) != '[') {
	    if (i+1 == class_name.length()) return true;
	  }
       }
      return false;
    }

}	// end of innerclass AsmClass





/********************************************************************************/
/*										*/
/*	Representation of a field						*/
/*										*/
/********************************************************************************/

private static class AsmField implements BanalField {

   private AsmClass for_class;
   private String   field_name;

   AsmField(AsmClass cls,String nm) {
      for_class = cls;
      field_name = nm;
    }

   @Override public BanalClass getOwnerClass()		{ return for_class; }
   @Override public String getName()			{ return field_name; }

}	// end of inner class AsmField




/********************************************************************************/
/*										graph*/
/*	Representation of a method						*/
/*										*/
/********************************************************************************/

private class AsmMethod implements BanalMethod {

   private AsmClass for_class;
   private String method_name;
   private String full_name;
   private AsmClass [] arg_types;
   private AsmClass return_type;

   AsmMethod(AsmClass fc,String nm,AsmClass mcls) {
      for_class = fc;
      method_name = nm;
      String mdesc = mcls.getInternalName();
      Type [] args = Type.getArgumentTypes(mdesc);
      arg_types = new AsmClass[args.length];
      for (int i = 0; i < args.length; ++i) {
	 arg_types[i] = findClass(args[i].getDescriptor(),true);
       }
      return_type = findClass(Type.getReturnType(mdesc).getDescriptor(),true);
      String astr = mcls.getJavaName();
      int idx1 = astr.indexOf("(");
      full_name = for_class.getJavaName() + "." + nm + astr.substring(idx1);
    }

   @Override public BanalClass getOwnerClass()		{ return for_class; }
   @Override public String getName()			{ return method_name; }
   @Override public BanalClass [] getArgumentTypes()	{ return arg_types; }
   @Override public BanalClass getReturnType()		{ return return_type; }
   @Override public String getFullName()		{ return full_name; }



}	// end of inner class AsmMethod






}	// end of class BanalStaticLoader




/* end of BanalStaticLoader.java */
