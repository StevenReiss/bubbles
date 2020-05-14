/********************************************************************************/
/*										*/
/*		RebaseJavaContext.java						*/
/*										*/
/*	Class to handle represent a Java user context				*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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


package edu.brown.cs.bubbles.rebase.java;


import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;


class RebaseJavaContext implements RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/**************************\*****************************************************/

private Map<String,AsmClass>	known_types;
private Map<String,RebaseJavaType>    special_types;
private String			context_package;
private String			context_class;
private List<String>		context_imports;
private List<JarFile>		base_files;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaContext(String javahome)
{
   known_types = new HashMap<String,AsmClass>();
   special_types = new HashMap<String,RebaseJavaType>();

   context_package = null;
   context_class = null;
   context_imports = new ArrayList<String>();

   computeBasePath(javahome);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getContextPackage()			{ return context_package; }
String getContextClass()			{ return context_class; }
Collection<String> getContextImports()		{ return context_imports; }



/********************************************************************************/
/*										*/
/*	Language-specific context methods					*/
/*										*/
/********************************************************************************/

RebaseJavaType defineKnownType(RebaseJavaTyper typer,String name)
{
   if (name == null) return null;

   AsmClass ac = findKnownType(typer,name);

   if (ac == null) return null;

   return ac.getRebaseJavaType(typer);
}




RebaseJavaSymbol defineKnownField(RebaseJavaTyper typer,String cls,String id)
{
   AsmClass ac = findKnownType(typer,cls);
   if (ac == null) return null;

   AsmField af = ac.findField(typer,id);
   if (af == null) return null;

   return af.createField(typer);
}



RebaseJavaSymbol defineKnownMethod(RebaseJavaTyper typer,String cls,String id,RebaseJavaType argtype,RebaseJavaType ctyp)
{
   AsmClass ac = findKnownType(typer,cls);
   if (ac == null) return null;

   AsmMethod am = ac.findMethod(typer,id,argtype,ctyp);
   if (am == null) return null;

   return am.createMethod(typer,argtype,ctyp);
}


List<RebaseJavaSymbol> findKnownMethods(RebaseJavaTyper typer,String cls)
{
   AsmClass ac = findKnownType(typer,cls);
   if (ac == null) return null;
   List<RebaseJavaSymbol> rslt = new ArrayList<RebaseJavaSymbol>();
   for (AsmMethod am : ac.getMethods()) {
      RebaseJavaSymbol js = am.createMethod(typer,null,null);
      if (js == null) return null;
      rslt.add(js);
    }
   return rslt;
}



void defineAll(RebaseJavaTyper typer,String cls,RebaseJavaScope scp)
{
   if (scp == null) return;
   AsmClass ac = findKnownType(typer,cls);
   if (ac == null) return;

   ac.defineAll(typer,scp);
}




/********************************************************************************/
/*										*/
/*	Type definition code							*/
/*										*/
/********************************************************************************/

private AsmClass findKnownType(RebaseJavaTyper typer,String name)
{
   AsmClass ac = known_types.get(name);
   if (ac != null) return ac;

   synchronized (this) {
      if (known_types.containsKey(name)) ac = known_types.get(name);
      else {
	 ac = findKnownClassType(name);
	 if (ac == null) {
	    int idx1 = name.lastIndexOf('$');
	    int idx = name.lastIndexOf('.');
	    if (idx1 < 0 && idx >= 0) {
	       String newnm = name.substring(0,idx) + "$" + name.substring(idx+1);
	       ac = findKnownType(typer,newnm);
	     }
	  }
	 known_types.put(name,ac);
	 if (ac != null) {
	    known_types.put(ac.getInternalName(),ac);
	    known_types.put(ac.getRebaseJavaName(),ac);
	    known_types.put(ac.getAccessName(),ac);
	  }
       }
    }

   return ac;
}




private AsmClass findKnownClassType(String name)
{
   String fnm = name.replace('.','/') + ".class";

   InputStream ins = getInputStream(fnm);
   if (ins == null) return null;

   KnownClassVisitor kcv = new KnownClassVisitor();

   try {
      ClassReader cr = new ClassReader(ins);
      cr.accept(kcv,ClassReader.SKIP_CODE);
    }
   catch (IOException e) {
      System.err.println("S6: CONTEXT: Problem reading class file: " + e);
    }
   finally {
      try {
	 ins.close();
       }
      catch (IOException e) { }
    }

   return kcv.getAsmClass();
}



private class KnownClassVisitor extends ClassVisitor {

   private AsmClass asm_data;

   KnownClassVisitor() {
      super(Opcodes.ASM4);
      asm_data = null;
    }

   AsmClass getAsmClass()		{ return asm_data; }


   @Override public void visit(int version,int access,String name,String sign,String sup,String [] ifcs) {
      asm_data = new AsmClass(name,access,sign,sup,ifcs);
    }

   @Override public AnnotationVisitor visitAnnotation(String dsc,boolean vis)	{ return null; }
   @Override public void visitAttribute(Attribute attr)				{ }
   @Override public void visitEnd()						{ }
   @Override public void visitInnerClass(String n,String o,String i,int acc)	{ }
   @Override public void visitOuterClass(String own,String nam,String d)		{ }
   @Override public void visitSource(String src,String dbg)			{ }

   @Override public FieldVisitor visitField(int access,String name,String desc,String sign,Object val) {
      asm_data.addField(name,access,sign,desc);
      return null;
    }

   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sign,String [] excs) {
      asm_data.addMethod(name,access,sign,desc,excs);
      return null;
    }


}	// end of class KnownClassVisitor




/********************************************************************************/
/*										*/
/*	ASM type interface							*/
/*										*/
/********************************************************************************/

private RebaseJavaType getAsmTypeName(RebaseJavaTyper typer,String nm)
{
   nm = nm.replace('/','.');

   RebaseJavaType jt = special_types.get(nm);
   if (jt != null) return jt;

   jt = typer.findSystemType(nm);

   if (jt == null) return null;
   if (!jt.isBaseKnown()) {
      jt = defineKnownType(typer,nm);
    }
   special_types.put(nm,jt);

   return jt;
}




private RebaseJavaType getAsmType(RebaseJavaTyper typer,String desc)
{
   return getAsmType(typer,Type.getType(desc));
}


private RebaseJavaType getAsmType(RebaseJavaTyper typer,Type t)
{
   String tnm = null;
   switch (t.getSort()) {
      case Type.VOID :
	 tnm = "void";
	 break;
      case Type.BOOLEAN :
	 tnm = "boolean";
	 break;
      case Type.CHAR :
	 tnm = "char";
	 break;
      case Type.BYTE :
	 tnm = "byte";
	 break;
      case Type.SHORT :
	 tnm = "short";
	 break;
      case Type.INT :
	 tnm = "int";
	 break;
      case Type.FLOAT :
	 tnm = "float";
	 break;
      case Type.LONG :
	 tnm = "long";
	 break;
      case Type.DOUBLE :
	 tnm = "double";
	 break;
      case Type.OBJECT :
	 tnm = t.getClassName();
	 break;
      case Type.ARRAY :
	 RebaseJavaType jt = getAsmType(typer,t.getElementType());
	 for (int i = 0; i < t.getDimensions(); ++i) jt = typer.findArrayType(jt);
	 return jt;
    }

   return typer.findSystemType(tnm);
}



/********************************************************************************/
/*										*/
/*	Information about a system class					*/
/*										*/
/********************************************************************************/

private class AsmClass {

   private String class_name;
   private int access_info;
   private String generic_signature;
   private String super_name;
   private String [] iface_names;
   private List<AsmField> field_data;
   private List<AsmMethod> method_data;
   private RebaseJavaType base_type;
   private boolean all_defined;

   AsmClass(String nm,int acc,String sgn,String sup,String [] ifc) {
      class_name = nm;
      access_info = acc;
      generic_signature = sgn;
      super_name = sup;
      iface_names = ifc;
      base_type = null;
      field_data = new ArrayList<AsmField>();
      method_data = new ArrayList<AsmMethod>();
      all_defined = false;
    }

   synchronized RebaseJavaType getRebaseJavaType(RebaseJavaTyper typer) {
      if (base_type == null) {
         String jnm = class_name.replace('/','.');
         jnm = jnm.replace('$','.');
         if ((access_info & Opcodes.ACC_INTERFACE) != 0) {
            base_type = RebaseJavaType.createKnownInterfaceType(jnm);
          }
         else {
            base_type = RebaseJavaType.createKnownType(jnm);
            if ((access_info & Opcodes.ACC_ABSTRACT) != 0) {
               base_type.setAbstract(true);
             }
          }
         base_type.setContextType(false);
         if (super_name != null) {
            RebaseJavaType sjt = getAsmTypeName(typer,super_name);
            if (!sjt.isKnownType()) {
               System.err.println("SUPER TYPE IS UNKNOWN");
             }
            if (sjt != null) base_type.setSuperType(sjt);
          }
         if (iface_names != null) {
            for (String inm : iface_names) {
               RebaseJavaType ijt = getAsmTypeName(typer,inm);
               if (ijt != null) base_type.addInterface(ijt);
             }
          }
       }
      typer.fixJavaType(base_type);
      return base_type;
    }

   String getInternalName()			{ return class_name; }
   String getRebaseJavaName() {
      String jnm = class_name.replace('/','.');
      jnm = jnm.replace('$','.');
      return jnm;
    }
   String getAccessName() {
      return class_name.replace('/','.');
    }
   boolean isStatic()				{ return (access_info&Opcodes.ACC_STATIC) != 0; }
   String getGenericSignature() 		{ return generic_signature; }
   List<AsmMethod> getMethods() 		{ return method_data; }

   void addField(String nm,int acc,String sgn,String desc) {
      AsmField af = new AsmField(this,nm,acc,sgn,desc);
      field_data.add(af);
    }

   void addMethod(String nm,int acc,String sgn,String desc,String [] exc) {
      AsmMethod am = new AsmMethod(this,nm,acc,sgn,desc,exc);
      method_data.add(am);
    }

   AsmField findField(RebaseJavaTyper typer,String id) {
      for (AsmField af : field_data) {
	 if (af.getName().equals(id)) return af;
       }
      if (super_name != null) {
	 AsmClass scl = findKnownType(typer,super_name);
	 if (scl != null) {
	    AsmField af = scl.findField(typer,id);
	    if (af != null) return af;
	  }
       }
      if (iface_names != null) {
	 for (String inm : iface_names) {
	    AsmClass icl = findKnownType(typer,inm);
	    if (icl != null) {
	       AsmField af = icl.findField(typer,id);
	       if (af != null) return af;
	     }
	  }
       }
      return null;
    }

   AsmMethod findMethod(RebaseJavaTyper typer,String id,RebaseJavaType argtyp,RebaseJavaType ctyp) {
      for (AsmMethod am : method_data) {
	 if (am.getName().equals(id) && am.isCompatibleWith(typer,argtyp)) return am;
       }
      if (super_name != null) {
	 AsmClass scl = findKnownType(typer,super_name);
	 if (scl != null) {
	    AsmMethod am = scl.findMethod(typer,id,argtyp,ctyp);
	    if (am != null) return am;
	  }
       }
      if (iface_names != null) {
	 for (String inm : iface_names) {
	    AsmClass icl = findKnownType(typer,inm);
	    if (icl != null) {
	       AsmMethod am = icl.findMethod(typer,id,argtyp,ctyp);
	       if (am != null) return am;
	     }
	  }
       }
      if ((access_info & Opcodes.ACC_INTERFACE) != 0) {
	 AsmClass jlo = findKnownType(typer,"java.lang.Object");
	 AsmMethod am = jlo.findMethod(typer,id,argtyp,ctyp);
	 if (am != null) return am;
       }
      return null;
    }

   synchronized void defineAll(RebaseJavaTyper typer,RebaseJavaScope scp) {
      if (all_defined) return;
      all_defined = true;
      for (AsmField af : field_data) {
	 if (scp.lookupVariable(af.getName()) == null) {
	    RebaseJavaSymbol js = af.createField(typer);
	    scp.defineVar(js);
	  }
       }
      for (AsmMethod am : method_data) {
	 RebaseJavaType atyp = am.getMethodType(typer,null);
	 if (scp.lookupMethod(am.getName(),atyp) == null) {
	    RebaseJavaSymbol js = am.createMethod(typer,null,getRebaseJavaType(typer));
	    scp.defineMethod(js);
	  }
       }
    }

}	// end of innerclass AsmClass



/********************************************************************************/
/*										*/
/*	Storage for field information from ASM					*/
/*										*/
/********************************************************************************/

private class AsmField {

   private AsmClass for_class;
   private String field_name;
   private int access_info;
   private String field_description;

   AsmField(AsmClass cls,String nm,int acc,String sgn,String desc) {
      for_class = cls;
      field_name = nm;
      access_info = acc;
      field_description = desc;
    }

   String getName()			{ return field_name; }

   RebaseJavaSymbol createField(RebaseJavaTyper typer) {
      boolean stc = (access_info & Opcodes.ACC_STATIC) != 0;
      boolean fnl = (access_info & Opcodes.ACC_FINAL) != 0;
      RebaseJavaType fty = getAsmType(typer,field_description);
      return RebaseJavaSymbol.createKnownField(field_name,fty,for_class.getRebaseJavaType(typer),stc,fnl);
    }

}	// end of innerclass AsmField




/********************************************************************************/
/*										*/
/*	Storage for method information from ASM 				*/
/*										*/
/********************************************************************************/

private class AsmMethod {

   private AsmClass for_class;
   private String method_name;
   private int access_info;
   private String method_desc;
   private String generic_signature;
   private String [] exception_types;

   AsmMethod(AsmClass cls,String nm,int acc,String sgn,String desc,String [] excs) {
      for_class = cls;
      method_name = nm;
      access_info = acc;
      generic_signature = sgn;
      method_desc = desc;
      exception_types = excs;
    }

   String getName()			{ return method_name; }

   boolean isCompatibleWith(RebaseJavaTyper typer,RebaseJavaType argtyp) {
      if (argtyp == null) return true;

      List<RebaseJavaType> args = argtyp.getComponents();
      Type [] margs = Type.getArgumentTypes(method_desc);

      boolean isok = false;
      if (margs.length == args.size()) {
	 isok = true;
	 for (int i = 0; i < margs.length; ++i) {
	    RebaseJavaType jt0 = getAsmType(typer,margs[i]);
	    RebaseJavaType jt1 = args.get(i);
	    if (!jt1.isCompatibleWith(jt0)) isok = false;
	  }
       }

      if (!isok && (access_info & Opcodes.ACC_VARARGS) != 0 && args.size() >= margs.length-1) {
	 isok = true;
	 for (int i = 0; i < margs.length-1; ++i) {
	    RebaseJavaType jt0 = getAsmType(typer,margs[i]);
	    RebaseJavaType jt1 = args.get(i);
	    if (!jt1.isCompatibleWith(jt0)) isok = false;

	  }
	 RebaseJavaType rjt0 = getAsmType(typer,margs[margs.length-1]);
	 if (rjt0.isArrayType()) rjt0 = rjt0.getBaseType();
	 for (int i = margs.length-1; i < args.size(); ++i) {
	    RebaseJavaType jt1 = args.get(i);
	    if (!jt1.isCompatibleWith(rjt0)) isok = false;
	  }
       }

      if (!isok && method_name.equals("<init>") && for_class.isStatic()) {
	 String cn = for_class.getAccessName();
	 int idx = cn.lastIndexOf('$');
	 int idx1 = cn.lastIndexOf('.');
	 if (idx > 0 && idx > idx1) {
	    if (margs.length == args.size() + 1) {
	       isok = true;
	       for (int i = 0; i < args.size(); ++i) {
		  RebaseJavaType jt0 = getAsmType(typer,margs[i]);
		  RebaseJavaType jt1 = args.get(i);
		  if (!jt1.isCompatibleWith(jt0)) isok = false;
		}
	     }
	  }
       }

      return isok;
    }

   RebaseJavaSymbol createMethod(RebaseJavaTyper typer,RebaseJavaType argtyp,RebaseJavaType ctyp) {
      Type mret = Type.getReturnType(method_desc);
      RebaseJavaType rt = getAsmType(typer,mret);
      boolean gen = false;

      String csgn = for_class.getGenericSignature();
      if (generic_signature != null && csgn != null && ctyp != null && ctyp.isParameterizedType()) {
	 RebaseJavaType nrt = typer.getParameterizedReturnType(generic_signature,csgn,ctyp,argtyp);
	 if (nrt != null) {
	    rt = nrt;
	    gen = true;
	  }
       }

      RebaseJavaType mt = getMethodType(typer,rt);

      List<RebaseJavaType> excs = new ArrayList<RebaseJavaType>();
      if (exception_types != null) {
	 for (String s : exception_types) {
	    RebaseJavaType jt = getAsmTypeName(typer,s);
	    excs.add(jt);
	  }
       }

      boolean stc = (access_info & Opcodes.ACC_STATIC) != 0;

      return RebaseJavaSymbol.createKnownMethod(method_name,mt,for_class.getRebaseJavaType(typer),stc,excs,gen);
    }

   RebaseJavaType getMethodType(RebaseJavaTyper typer,RebaseJavaType rt) {
      List<RebaseJavaType> atys = new ArrayList<RebaseJavaType>();
      for (Type t : Type.getArgumentTypes(method_desc)) {
         atys.add(getAsmType(typer,t));
       }
      boolean var = (access_info & Opcodes.ACC_VARARGS) != 0;
      RebaseJavaType mt = RebaseJavaType.createMethodType(rt,atys,var);
      mt = typer.fixJavaType(mt);
      return mt;
    }


}	// end of innerclass AsmMethod




/********************************************************************************/
/*										*/
/*	Routines to setup base path						*/
/*										*/
/********************************************************************************/

private void computeBasePath(String javahome)
{
   if (javahome == null) javahome = System.getProperty("java.home");

   String dir = javahome + File.separator + "lib";

   System.err.println("BASE PATH: " + dir);

   base_files = new ArrayList<JarFile>();

   addToBasePath(new File(dir));
}


private void addToBasePath(File dir)
{
   if (!dir.exists()) ;
   else if (dir.isDirectory()) {
      File [] cnts = dir.listFiles();
      for (int i = 0; i < cnts.length; ++i) addToBasePath(cnts[i]);
    }
   else if (dir.getName().endsWith(".jar")) {
      try {
	 JarFile jf = new JarFile(dir);
	 base_files.add(jf);
       }
      catch (IOException e) {
	 System.err.println("S6: CONTEXT: Can't open system jar file " + dir);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Routines to lookup a file						*/
/*										*/
/********************************************************************************/

public synchronized boolean contains(String name)
{
   for (JarFile jf : base_files) {
      if (jf.getEntry(name) != null) return true;
    }

   return false;
}



public synchronized InputStream getInputStream(String name)
{
   for (JarFile jf : base_files) {
      ZipEntry ent = jf.getEntry(name);
      if (ent != null) {
	 try {
	    return jf.getInputStream(ent);
	  }
	 catch (ZipException e) {
	    System.err.println("S6:CONTEXT: Problem with system zip file: " + e);
	  }
	 catch (IOException e) {
	    System.err.println("S6:CONTEXT: Problem opening system jar entry: " + e);
	  }
       }
    }

   return null;
}






}




/* end of RebaseJavaContext.java */




