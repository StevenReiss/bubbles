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
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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



/********************************************************************************/
/*										*/
/*	Visitor to find imports 						*/
/*										*/
/********************************************************************************/

private class ImportFinder extends ASTVisitor {

   private Set<ITypeBinding> import_types;
   private Set<ITypeBinding> defined_types;
   private IPackageBinding package_name;

   ImportFinder() {
      import_types = new HashSet<ITypeBinding>();
      defined_types = new HashSet<ITypeBinding>();
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
      BedrockPlugin.logD("Check import type: " + t.getQualifiedName());
      if (t.isArray()) {
	 t = t.getElementType();
       }
      if (t.getErasure() != null) t = t.getErasure();

      BedrockPlugin.logD("Check erasure type: " + t.getQualifiedName());

      if (t.isTypeVariable()) return;
      else if (t.isLocal()) return;
      else if (t.isNullType()) return;
      else if (t.isPrimitive()) return;
      else if (t.isRawType()) return;
      else if (t.isWildcardType()) return;
      else if (t.getPackage().equals(package_name)) return;
      else if (t.getPackage().getName().equals("java.lang")) return;
      else {
	 BedrockPlugin.logD("Add import type: " + t.getQualifiedName());
	 import_types.add(t);
       }
    }

}	// end of inner class ImportFinder




}	// end of class BedrockFixImports




/* end of BedrockFixImports.java */

