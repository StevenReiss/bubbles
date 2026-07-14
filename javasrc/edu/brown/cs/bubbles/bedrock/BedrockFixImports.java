/********************************************************************************/
/*										*/
/*		BedrockFixImports.java						*/
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



package edu.brown.cs.bubbles.bedrock;


import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeParameter;

import java.util.HashSet;
import java.util.Set;

class BedrockFixImports implements BedrockConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockFixImports()
{
}


/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

Set<ITypeBinding> findImports(CompilationUnit cu)
{
   ImportFinder fndr = new ImportFinder();
   cu.accept(fndr);
   return fndr.getImports();
} 


Set<IBinding> findStaticImports(CompilationUnit cu)
{
   StaticFinder fnd = new StaticFinder();
   cu.accept(fnd);
   fnd.doneTypes();
   cu.accept(fnd);
   return fnd.getImports();
}



/********************************************************************************/
/*										*/
/*	Visitor to find type imports 						*/
/*										*/
/********************************************************************************/

private class ImportFinder extends ASTVisitor {

   private Set<ITypeBinding> import_types;
   private Set<ITypeBinding> defined_types;
   private IPackageBinding package_name;

   ImportFinder() {
      import_types = new HashSet<>();
      defined_types = new HashSet<>();
    }

   Set<ITypeBinding> getImports() {
      if (defined_types != null) {
         import_types.removeAll(defined_types);
         defined_types = null;
       }
      return import_types;
    }

  @Override public void endVisit(AnnotationTypeDeclaration n) {
      defined_types.add(n.resolveBinding());
    }

   @Override public void endVisit(AnonymousClassDeclaration n) {
      defined_types.add(n.resolveBinding());
    }

   @Override public void endVisit(EnumDeclaration n) {
      defined_types.add(n.resolveBinding());
    }

   @Override public boolean visit(ImportDeclaration n) {
      return false;
    }

   @Override public void endVisit(MemberRef n) {
      noteType(n.resolveBinding());
    }

   @Override public boolean visit(PackageDeclaration n) {
      package_name = n.resolveBinding();
      return false;
    }

   @Override public void endVisit(ParameterizedType n) {
      noteType(n.resolveBinding());
    }

   @Override public boolean visit(QualifiedType n) {
      n.getQualifier().accept(this);
      return false;
    }

   @Override public boolean visit(QualifiedName n) {
      n.getQualifier().accept(this);
      return false;
    }

   @Override public void endVisit(SimpleName n) {
      noteType(n.resolveBinding());
    }

   @Override public void endVisit(TypeDeclaration n) {
      defined_types.add(n.resolveBinding());
    }

   @Override public void endVisit(TypeDeclarationStatement n) {
      defined_types.add(n.resolveBinding());
    }

   @Override public void endVisit(TypeParameter n) {
      defined_types.add(n.resolveBinding());
    }

   private void noteType(IBinding n) {
      if (n instanceof ITypeBinding) {
         noteType((ITypeBinding) n);
       }
    }

   private void noteType(ITypeBinding t) {
      if (t == null) return;
      String tnm = t.getQualifiedName(); 
      
      BedrockPlugin.logD("Check import type: " + tnm);
      if (t.isArray()) {
         t = t.getElementType();
       }
      if (t.getErasure() != null) t = t.getErasure();
      
      tnm = t.getQualifiedName();
      
      BedrockPlugin.logD("Check erasure type: " + tnm);
   
      if (t.isTypeVariable()) return;
      else if (t.isLocal()) return;
      else if (t.isNullType()) return;
      else if (t.isPrimitive()) return;
      else if (t.isRawType()) return;
      else if (t.isWildcardType()) return;
      else if (t.getPackage().equals(package_name)) {
         // need to import inner classes even if in same package
         String p1 = t.getPackage().getName();
         if (!tnm.startsWith(p1)) {
            BedrockPlugin.logD("Bad type name " + tnm + " " + p1);
          }
         else {
            String tnm1 = tnm.substring(p1.length()+1);
            if (!tnm1.contains(".") && !tnm1.contains("$")) return;
          }
       }
      else if (t.getPackage().getName().equals("java.lang")) return;
      
      BedrockPlugin.logD("Add import type: " + tnm);
      import_types.add(t);
    }

}	// end of inner class ImportFinder



/********************************************************************************/
/*										*/
/*	Visitor to find static imports 					*/
/*										*/
/********************************************************************************/

private class StaticFinder extends ASTVisitor {
   
   private Set<IBinding> static_imports;
   private Set<ITypeBinding> defined_types;
   private boolean defining_types;
   
   StaticFinder() {
      static_imports = new HashSet<>();
      defined_types = new HashSet<>();
      defining_types = true;
    }
   
   void doneTypes() {
      defining_types = false;
    }
   
   Set<IBinding> getImports() {
      return static_imports;
    }
   
   @Override public void endVisit(AnnotationTypeDeclaration n) {
      addDefinedType(n.resolveBinding());
    }
   
   @Override public void endVisit(AnonymousClassDeclaration n) {
      addDefinedType(n.resolveBinding());
    }
   
   @Override public void endVisit(EnumDeclaration n) {
      addDefinedType(n.resolveBinding());
    }
   
   @Override public void endVisit(TypeDeclaration n) {
      addDefinedType(n.resolveBinding());
    }
   
   @Override public void endVisit(TypeDeclarationStatement n) {
      addDefinedType(n.resolveBinding());
    }
   
   @Override public void endVisit(TypeParameter n) {
      addDefinedType(n.resolveBinding());
    }
   
   @Override public boolean visit(ImportDeclaration n) {
      return false;
    }
   
   @Override public boolean visit(QualifiedName n) {
      noteVariable(n.resolveBinding());
      return false;
    }
   
   @Override public void endVisit(SimpleName n) {
      noteVariable(n.resolveBinding());
    }
   
   private void noteVariable(IBinding n) {
      if (defining_types) return;
      if (n.isSynthetic()) return;
      if (n.isRecovered()) return;
      switch (n.getKind()) {
         case IBinding.METHOD :
            IMethodBinding mthd = (IMethodBinding) n;
            ITypeBinding cls = mthd.getDeclaringClass();
            if (defined_types.contains(cls)) return;
            break;
         case IBinding.VARIABLE :
            IVariableBinding var = (IVariableBinding) n;
            if (!var.isField()) return;
            ITypeBinding vtyp = var.getType();
            if (defined_types.contains(vtyp)) return;
            break;
         default :
            return;
       }
      static_imports.add(n);
    }
   
   private void addDefinedType(ITypeBinding t) {
      if (!defining_types) return;
      if (t == null) return;
      noteType(t.getSuperclass());
      for (ITypeBinding intf : t.getInterfaces()) {
         noteType(intf);
       }
      
      String tnm = t.getQualifiedName(); 
      BedrockPlugin.logD("Check import type: " + tnm);
      if (t.isArray()) {
         t = t.getElementType();
       }
      if (t.getErasure() != null) t = t.getErasure();
      
      tnm = t.getQualifiedName();
      
      BedrockPlugin.logD("Check erasure type: " + tnm);
      
      if (t.isTypeVariable()) return;
      else if (t.isLocal()) return;
      else if (t.isNullType()) return;
      else if (t.isPrimitive()) return;
      else if (t.isRawType()) return;
      else if (t.isWildcardType()) return;
      else if (t.getPackage().getName().equals("java.lang")) return;
      
      BedrockPlugin.logD("Add defined type: " + tnm);
      defined_types.add(t);  
    }
   
   private void noteType(ITypeBinding t) {
      if (t == null) return;
      
      String tnm = t.getQualifiedName(); 
      BedrockPlugin.logD("Check import type: " + tnm);
      if (t.isArray()) {
         t = t.getElementType();
       }
      if (t.getErasure() != null) t = t.getErasure();
      
      tnm = t.getQualifiedName();
      
      BedrockPlugin.logD("Check erasure type: " + tnm);
      
      if (t.isTypeVariable()) return;
      else if (t.isLocal()) return;
      else if (t.isNullType()) return;
      else if (t.isPrimitive()) return;
      else if (t.isRawType()) return;
      else if (t.isWildcardType()) return;
      else if (t.getPackage().getName().equals("java.lang")) return;
      
      BedrockPlugin.logD("Add defined type: " + tnm);
      defined_types.add(t);
    }
   
}	// end of inner class ImportFinder



}	// end of class BedrockFixImports




/* end of BedrockFixImports.java */

