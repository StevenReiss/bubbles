/********************************************************************************/
/*										*/
/*		BedrockElider.java						*/
/*										*/
/*	Handle elision computation for Bubbles					*/
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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class BedrockElider implements BedrockConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<ElidePriority> elide_pdata;
private List<ElideRegion> elide_rdata;

//TODO: replace up_map, scaleUp, and merge with Prioritizer methods
private static Map<StructuralPropertyDescriptor,Double>      up_map;

private static Prioritizer down_priority;

private static final double	UP_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_COUNT = 0.95;
private static final double	DOWN_DEFAULT_ITEM  = 0.99;
private static final double	SWITCH_BLOCK_SCALE = 0.90;


static {
   up_map = new HashMap<StructuralPropertyDescriptor,Double>();

   Prioritizer dflt = new DefaultPrioritizer(DOWN_DEFAULT_SCALE,DOWN_DEFAULT_COUNT,DOWN_DEFAULT_ITEM);
   Prioritizer same = new DefaultPrioritizer(1.0,1.0,1.0);

   StructuralPrioritizer p0 = new StructuralPrioritizer(dflt);

   // ELSE IF ... should have same priority as outer IF
   NodePrioritizer p1 = new NodePrioritizer(dflt);
   p1.addPriority(ASTNode.IF_STATEMENT,same);
   p0.addPriority(IfStatement.ELSE_STATEMENT_PROPERTY,p1);

   down_priority = p0;
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockElider()
{
   elide_pdata = new ArrayList<>();
   elide_rdata = new ArrayList<>();
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining elision information				*/
/*										*/
/********************************************************************************/


void clearElideData()
{
   elide_pdata.clear();
   elide_rdata.clear();
}



void addElidePriority(int soff,int eoff,double pri)
{
   ElidePriority ed = new ElidePriority(soff,eoff,pri);
   elide_pdata.add(ed);
}


void addElideRegion(int soff,int eoff)
{
   ElideRegion er = new ElideRegion(soff,eoff);
   elide_rdata.add(er);
}



void noteEdit(int soff,int len,int rlen)
{
   for (Iterator<ElidePriority> it = elide_pdata.iterator(); it.hasNext(); ) {
      ElidePriority ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }

   for (Iterator<ElideRegion> it = elide_rdata.iterator(); it.hasNext(); ) {
      ElideRegion ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }
}



/********************************************************************************/
/*										*/
/*	Elision computaton methods						*/
/*										*/
/********************************************************************************/

boolean computeElision(CompilationUnit cu,IvyXmlWriter xw)
{
   if (cu == null || elide_rdata.isEmpty()) return false;

   ElidePass1 ep1 = null;
   if (!elide_pdata.isEmpty()) {
      ep1 = new ElidePass1();
      cu.accept(ep1);
    }

   ElidePass2 ep2 = new ElidePass2(ep1,xw);
   cu.accept(ep2);

   return true;
}




/********************************************************************************/
/*										*/
/*	Access methods for elision information					*/
/*										*/
/********************************************************************************/

private double getElidePriority(ASTNode n)
{
   for (ElidePriority ep : elide_pdata) {
      if (ep.useForPriority(n)) return ep.getPriority();
    }

   return 0;
}



private boolean isActiveRegion(int soff,int len)
{
   for (ElideRegion er : elide_rdata) {
      if (er.overlaps(soff,len)) return true;
    }

   return false;
}



private boolean isRootRegion(int soff,int len)
{
   for (ElideRegion er : elide_rdata) {
      if (er.contains(soff,len)) return true;
    }

   return false;
}



private double scaleUp(ASTNode n)
{
   Double v = up_map.get(n.getLocationInParent());
   if (v != null) return v.doubleValue();
   return UP_DEFAULT_SCALE;
}



/********************************************************************************/
/*										*/
/*	Main priority function							*/
/*										*/
/********************************************************************************/

private double computePriority(double parprior,ASTNode base,double pass1prior)
{
   double p = down_priority.getPriority(parprior,base);

   if (pass1prior > p) p = pass1prior;
   
   return p;
}



/********************************************************************************/
/*										*/
/*	Formatting type function						*/
/*										*/
/********************************************************************************/

private String getFormatType(ASTNode n)
{
   String typ = null;

   if (n instanceof Name) {
      ASTNode p = n.getParent();
      ASTNode pp = p.getParent();
      StructuralPropertyDescriptor spd = n.getLocationInParent();
      switch (p.getNodeType()) {
	 case ASTNode.METHOD_INVOCATION :
	    if (n.getLocationInParent() == MethodInvocation.NAME_PROPERTY) {
	       typ = "CALL" + getMethodType((Name) n);
	     }
	    break;
	 case ASTNode.SIMPLE_TYPE :
	 case ASTNode.QUALIFIED_TYPE :
	 case ASTNode.TYPE_PARAMETER :
	    typ = "TYPE";
	    break;
	 case ASTNode.METHOD_DECLARATION :
	    if (spd == MethodDeclaration.NAME_PROPERTY) {
	       typ = "METHODDECL" + getMethodType((Name) n);
	     }
	    else if (spd == MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) typ = "TYPE";
	    break;
	 case ASTNode.SINGLE_VARIABLE_DECLARATION :
	    if (pp.getNodeType() == ASTNode.CATCH_CLAUSE) typ = "EXCEPTIONDECL";
	    else typ = "PARAMDECL";
	    break;
	 case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
	    if (pp.getNodeType() == ASTNode.FIELD_DECLARATION) typ = "FIELDDECL";
	    else typ = "LOCALDECL";
	    break;
	 case ASTNode.ENUM_DECLARATION :
	 case ASTNode.TYPE_DECLARATION :
	 case ASTNode.ANNOTATION_TYPE_DECLARATION :
	    typ = "CLASSDECL" + getClassType((Name) n);
	    break;
	 case ASTNode.MARKER_ANNOTATION :
	 case ASTNode.NORMAL_ANNOTATION :
	 case ASTNode.SINGLE_MEMBER_ANNOTATION :
	    typ = "ANNOT";
	    break;
       }
    }

   if (typ == null) {
      if (n instanceof SimpleName) {
	 IBinding ib = ((Name) n).resolveBinding();
	 // BedrockPlugin.logD("BINDING FOR " + ((SimpleName) n).getIdentifier() + " = " + ib);
	 if (ib != null && ib.getKind() == IBinding.VARIABLE) {
	    IVariableBinding vb = (IVariableBinding) ib;
	    if (vb.isEnumConstant()) typ = "ENUMC";
	    else if (vb.isField()) {
	       typ = "FIELD" + getVariableType((Name) n);
	     }
	  }
	 else if (ib != null && ib.getKind() == IBinding.METHOD) {
	    typ = "CALL" + getMethodType((Name) n);
	  }
	 else if (ib != null && ib.getKind() == IBinding.ANNOTATION) {
	    typ = "ANNOT";
	  }
	 else if (ib != null && ib.getKind() == IBinding.TYPE) {
	    typ = "TYPE";
	  }
	 else if (ib == null) typ = "UNDEF";
       }
    }

   // TODO: indicate whether the name is a user or system name

   return typ;
}



private String getMethodType(Name n)
{
   IMethodBinding mb = (IMethodBinding) n.resolveBinding();

   if (mb == null) return "U";

   String typ = "";

   if (mb.isDeprecated()) typ = "D";

   if (Modifier.isAbstract(mb.getModifiers())) typ = "A";
   else if (Modifier.isStatic(mb.getModifiers())) typ = "S";

   return typ;
}



private String getVariableType(Name n)
{
   IVariableBinding vb = (IVariableBinding) n.resolveBinding();

   if (vb == null) return "U";

   String typ = "";

   if (Modifier.isStatic(vb.getModifiers())) {
      if (Modifier.isFinal(vb.getModifiers())) typ = "C";
      else typ = "S";
    }

   return typ;
}



private String getClassType(Name n)
{
   ITypeBinding vb = (ITypeBinding) n.resolveBinding();

   if (vb == null) return "";

   String typ = "";

   if (vb.isMember()) typ = "M";
   else if (vb.isLocal()) typ = "L";

   return typ;
}



private String getNodeType(ASTNode n)
{
   String typ = null;

   if (n instanceof Block) {
      typ = "BLOCK";
    }
   else if (n instanceof SwitchCase) {
      typ = "CASE";
    }
   else if (n instanceof Statement) {
      typ = "STMT";
    }
   else if (n instanceof CatchClause) {
      typ = "CATCH";
    }
   else if (n instanceof SimpleName) ;
   else if (n instanceof MethodInvocation) {
      typ = "CALL";
      ASTNode p = n.getParent();
      if (!(p instanceof Expression) && !(p instanceof Type)) typ = "CALLEXPR";
    }
   else if (n instanceof Expression) {
      ASTNode p = n.getParent();
      if (!(p instanceof Expression) && !(p instanceof Type)) typ = "EXPR";
    }
   else if (n instanceof AnnotationTypeDeclaration) {
      typ = "ATYPE";
    }
   else if (n instanceof EnumDeclaration) {
      typ = "ENUM";
    }
   else if (n instanceof TypeDeclaration) {
      typ = "CLASS";
    }
   else if (n instanceof EnumConstantDeclaration) {
      typ = "ENUMC";
    }
   else if (n instanceof FieldDeclaration) {
      typ = "FIELD";
    }
   else if (n instanceof Initializer) {
      typ = "INITIALIZER";
    }
   else if (n instanceof MethodDeclaration) {
      typ = "METHOD";
    }
   else if (n instanceof AnonymousClassDeclaration) {
      typ = "ACLASS";
    }
   else if (n instanceof AnnotationTypeMemberDeclaration) {
      typ = "ANNOT";
    }
   else if (n instanceof CompilationUnit) {
      typ = "COMPUNIT";
    }
   else if (n instanceof ImportDeclaration) {
      typ = "IMPORT";
    }

   return typ;
}



/********************************************************************************/
/*										*/
/*	Output information for hints						*/
/*										*/
/********************************************************************************/

private void outputHintData(ASTNode n,IvyXmlWriter xw)
{
   IMethodBinding imb = null;
   if (n instanceof MethodInvocation) {
      MethodInvocation mi = (MethodInvocation) n;
      if (mi.arguments().size() >= 2) {
	 imb = mi.resolveMethodBinding();
       }
    }
   else if (n instanceof ClassInstanceCreation) {
      ClassInstanceCreation cic = (ClassInstanceCreation) n;
      if (cic.arguments().size() >= 2) {
	 imb = cic.resolveConstructorBinding();
       }
    }
   else if (n instanceof SuperConstructorInvocation) {
      SuperConstructorInvocation sci = (SuperConstructorInvocation) n;
      if (sci.arguments().size() >= 2) {
	 imb = sci.resolveConstructorBinding();
       }
    }
   else if (n instanceof ConstructorInvocation) {
      ConstructorInvocation cci = (ConstructorInvocation) n;
      if (cci.arguments().size() >= 2) {
	 imb = cci.resolveConstructorBinding();
       }
    }

   if (imb != null) {
      IJavaElement ije = imb.getJavaElement();
      if (ije == null || !(ije instanceof IMethod)) return;
      IMethod mthd = (IMethod) ije;
      String rettyp;
      String [] pnames;
      String [] ptypes;
      try {
	 rettyp = mthd.getReturnType();
	 pnames = mthd.getParameterNames();
	 ptypes = mthd.getParameterTypes();
       }
      catch (JavaModelException e) {
	 return;
       } 
      xw.begin("HINT");
      xw.field("KIND","METHOD");
      if (n instanceof ClassInstanceCreation) xw.field("CONSTRUCTOR",true);
      else xw.field("RETURNS",rettyp);
      int nparam = mthd.getNumberOfParameters();
      if (nparam != pnames.length) {
	 BedrockPlugin.logE("Method mismatch: " + mthd + " " + n + " " + nparam + " " + pnames.length);
       }
      xw.field("NUMPARAM",nparam);
      for (int i = 0; i < mthd.getNumberOfParameters(); ++i) {
	 int j = Math.min(i,pnames.length);
	 xw.begin("PARAMETER");
	 xw.field("NAME",pnames[j]);
	 xw.field("TYPE",ptypes[j]);
	 xw.end("PARAMETER");
       }
      xw.end("HINT");
    }
}



/********************************************************************************/
/*										*/
/*	Tree walk for setting initial priorities				*/
/*										*/
/********************************************************************************/

private class ElidePass1 extends ASTVisitor {

   private Map<ASTNode,Double> result_value;
   private int inside_count;
   private CompilationUnit tree_root;

   ElidePass1() {
      result_value = new HashMap<ASTNode,Double>();
      inside_count = 0;
      tree_root = null;
    }

   @Override public void preVisit(ASTNode n) {
      if (tree_root == null && n instanceof CompilationUnit) tree_root = (CompilationUnit) n;
      if (inside_count > 0) {
	 ++inside_count;
	 return;
       }

      double p = getElidePriority(n);
      if (p != 0) {
	 result_value.put(n,p);
	 ++inside_count;
       }
    }

   @Override public void postVisit(ASTNode n) {
      if (inside_count > 0) {
	 --inside_count;
	 return;
       }
      List<?> l = n.structuralPropertiesForType();
      double p = 0;
      for (Iterator<?> it = l.iterator(); it.hasNext(); ) {
	 StructuralPropertyDescriptor sp = (StructuralPropertyDescriptor) it.next();
	 if (sp.isChildProperty()) {
	    ASTNode cn = (ASTNode) n.getStructuralProperty(sp);
	    p = merge(p,cn);
	  }
	 else if (sp.isChildListProperty()) {
	    List<?> cl = (List<?>) n.getStructuralProperty(sp);
	    for (Iterator<?> it1 = cl.iterator(); it1.hasNext(); ) {
	       ASTNode cn = (ASTNode) it1.next();
	       p = merge(p,cn);
	     }
	  }
       }
      if (p > 0) {
	 p *= scaleUp(n);
	 result_value.put(n,p);
       }
    }

   @Override public boolean visit(MethodDeclaration n) {
      return isActiveRegion(n.getStartPosition(),n.getLength());
    }
   @Override public boolean visit(TypeDeclaration n) {
      return isActiveRegion(n.getStartPosition(),n.getLength());
    }
   @Override public boolean visit(Initializer n) {
      return isActiveRegion(n.getStartPosition(),n.getLength());
    }

   double getPriority(ASTNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }

   private double merge(double p,ASTNode n) {
      Double dv = result_value.get(n);
      if (dv == null) return p;
      double q = 1 - (1-p)*(1-dv.doubleValue());
      return q;
    }

}	// end of innerclass ElidePass1




/********************************************************************************/
/*										*/
/*	Tree walk for setting final priorities					*/
/*										*/
/********************************************************************************/

private class ElidePass2 extends ASTVisitor {

   private ElidePass1 up_values;
   private Map<ASTNode,Double> result_value;
   private CompilationUnit tree_root;
   private ASTNode active_node;
   private IvyXmlWriter xml_writer;
   private boolean last_case;
   private Stack<ASTNode> switch_stack;

   ElidePass2(ElidePass1 pass1,IvyXmlWriter xw) {
      up_values = pass1;
      xml_writer = xw;
      result_value = new HashMap<>();
      tree_root = null;
      active_node = null;
      last_case = false;
      switch_stack = new Stack<>();
    }

   @Override public void preVisit(ASTNode n) {
      if (tree_root == null && n instanceof CompilationUnit) tree_root = (CompilationUnit) n;
      if (active_node == null) {
	 if (isRootRegion(n.getStartPosition(),n.getLength())) {
	    active_node = n;
	    result_value.put(n,1.0);
	    // BedrockPlugin.logD("PRIORITY TOP " + n.getStartPosition() + " " + getNodeType(n) + " : " + n);
	    outputXmlStart(n);
	  }
	 return;
       }
      double v = getPriority(n.getParent());
      double v0 = 0;
      if (up_values != null) v0 = up_values.getPriority(n);
      double p = computePriority(v,n,v0);
      // BedrockPlugin.logD("PRIORITY " + p + " " + n.getStartPosition() + " " + getNodeType(n) + " : " + n);
      if (p != 0) {
	 result_value.put(n,p);
	 checkSwitchBlock(n);
	 outputXmlStart(n);
       }
    }

   @Override public void postVisit(ASTNode n) {
      if (active_node == n) active_node = null;
      if (xml_writer != null && result_value.get(n) != null && result_value.get(n) > 0) {
	 outputHintData(n,xml_writer);
	 xml_writer.end("ELIDE");
       }
      checkEndSwitchBlock(n);
    }

   @Override public boolean visit(MethodDeclaration n) {
      return isActiveRegion(n.getStartPosition(),n.getLength());
    }
   @Override public boolean visit(TypeDeclaration n) {
      return isActiveRegion(n.getStartPosition(),n.getLength());
    }
   @Override public boolean visit(Initializer n) {
      return isActiveRegion(n.getStartPosition(),n.getLength());
    }

   double getPriority(ASTNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }

   private void outputXmlStart(ASTNode n) {
      if (xml_writer != null) {
	 xml_writer.begin("ELIDE");
	 int sp = n.getStartPosition();
	 int esp = tree_root.getExtendedStartPosition(n);
	 int ln = n.getLength();
	 int eln = tree_root.getExtendedLength(n);
	 xml_writer.field("START",sp);
	 if (esp != sp) xml_writer.field("ESTART",esp);
	 xml_writer.field("LENGTH",ln);
	 if (eln != ln) xml_writer.field("ELENGTH",eln);
	 double p = result_value.get(n);
	 for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
	 xml_writer.field("PRIORITY",p);
	 String typ = getFormatType(n);
	 if (typ != null) {
	    xml_writer.field("TYPE",typ);
	    if (typ.startsWith("METHODDECL") || typ.startsWith("FIELDDECL") ||
		   typ.startsWith("CLASSDECL")) {
	       outputDeclInfo((Name) n);
	     }
	  }
	 String ttyp = getNodeType(n);
	 if (ttyp != null) xml_writer.field("NODE",ttyp);
	 if ((n.getFlags() & ASTNode.MALFORMED) != 0) xml_writer.field("ERROR",true);
	 if ((n.getFlags() & ASTNode.RECOVERED) != 0) xml_writer.field("RECOV",true);
       }
    }

   private void outputDeclInfo(Name name) {
      IBinding bnd = name.resolveBinding();
      if (bnd == null) return;

      StringBuffer buf;

      switch (bnd.getKind()) {
	 case IBinding.ANNOTATION :
	    buf = new StringBuffer();
	    IAnnotationBinding iab = (IAnnotationBinding) bnd;
	    buf.append(iab.getAnnotationType().getQualifiedName());
	    buf.append(".");
	    buf.append(iab.getName());
	    xml_writer.field("FULLNAME",buf.toString());
	    break;
	 case IBinding.MEMBER_VALUE_PAIR :
	 case IBinding.PACKAGE :
	    break;
	 case IBinding.METHOD :
	    buf = new StringBuffer();
	    IMethodBinding imb = (IMethodBinding) bnd;
	    buf.append(imb.getDeclaringClass().getQualifiedName());
	    buf.append(".");
	    buf.append(imb.getName());
	    buf.append("(");
	    int ct = 0;
	    for (ITypeBinding tb : imb.getParameterTypes()) {
	       if (ct++ > 0) buf.append(",");
	       buf.append(tb.getName());
	     }
	    buf.append(")");
	    xml_writer.field("FULLNAME",buf.toString());
	    break;
	 case IBinding.VARIABLE :
	    buf = new StringBuffer();
	    IVariableBinding ivb = (IVariableBinding) bnd;
	    if (ivb.isField()) {
	       if (ivb.getDeclaringClass() != null) {
		  buf.append(ivb.getDeclaringClass().getQualifiedName());
		  buf.append(".");
		  buf.append(ivb.getName());
		  xml_writer.field("FULLNAME",buf.toString());
		}
	     }
	    break;
	 case IBinding.TYPE :
	    ITypeBinding itb = (ITypeBinding) bnd;
	    xml_writer.field("FULLNAME",itb.getQualifiedName());
	    xml_writer.field("LOCAL",itb.isLocal());
	    xml_writer.field("MEMBER",itb.isMember());
	    break;
       }
    }

   private void checkSwitchBlock(ASTNode n) {
      if (!last_case || xml_writer == null) return;
      // BedrockPlugin.logD("SWITCH BLOCK CHECK " + result_value.get(n) + " " + n);
      last_case = false;
      if (result_value.get(n) == null) return;
      if (n instanceof SwitchCase) return;
      ASTNode last = null;
      if (n instanceof Statement) {
	 ASTNode pn = n.getParent();
	 if (pn.getNodeType() != ASTNode.SWITCH_STATEMENT) return;
	 List<?> l = (List<?>) pn.getStructuralProperty(SwitchStatement.STATEMENTS_PROPERTY);
	 int idx = l.indexOf(n);
	 if (idx < 0) return;
	 int lidx = idx;
	 while (lidx+1 < l.size()) {
	    if (l.get(lidx+1) instanceof SwitchCase) break;
	    else if (l.get(lidx+1) instanceof Statement) ++lidx;
	    else return;
	  }
	 if (lidx - idx >= 2) last = (ASTNode) l.get(lidx);
       }
      if (last == null) return;
      xml_writer.begin("ELIDE");
      int sp = n.getStartPosition();
      int esp = tree_root.getExtendedStartPosition(n);
      int ep = last.getStartPosition() + last.getLength();
      int eep = tree_root.getExtendedStartPosition(last) + tree_root.getExtendedLength(last);
      int ln = ep - sp;
      int eln = eep - esp;
      xml_writer.field("START",sp);
      if (esp != sp) xml_writer.field("ESTART",esp);
      xml_writer.field("LENGTH",ln);
      if (eln != ln) xml_writer.field("ELENGTH",eln);
      double p = result_value.get(n);
      for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
      xml_writer.field("PRIORITY",p);
      xml_writer.field("NODE","SWBLOCK");
      switch_stack.push(last);
    }

   private void checkEndSwitchBlock(ASTNode n) {
      while (!switch_stack.isEmpty() && n == switch_stack.peek()) {
	 switch_stack.pop();
	 xml_writer.end("ELIDE");
       }
      last_case = (n instanceof SwitchCase);
    }

}	// end of innerclass ElidePass2




/********************************************************************************/
/*										*/
/*	Classes for elision region and priorities				*/
/*										*/
/********************************************************************************/

private abstract class ElideData {

   private int start_offset;
   private int end_offset;

   ElideData(int soff,int eoff) {
      start_offset = soff;
      end_offset = eoff;
    }

   boolean contains(int soff,int len) { 				
      return (start_offset <= soff && end_offset >= soff+len-1);
    }

   boolean useForPriority(ASTNode n) {
      int sp = n.getStartPosition();
      int ln = n.getLength();
      if (start_offset != end_offset) return contains(sp,ln);
      if (!overlaps(sp,ln)) return false;
      // check if any child overlaps, use child if so
      for (Iterator<?> it = n.structuralPropertiesForType().iterator(); it.hasNext(); ) {
	 StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) it.next();
	 if (spd.isSimpleProperty()) ;
	 else if (spd.isChildProperty()) {
	    ASTNode cn = (ASTNode) n.getStructuralProperty(spd);
	    if (cn != null) {
	       if (overlaps(cn.getStartPosition(),cn.getLength())) return false;
	     }
	  }
	 else {
	    List<?> lcn = (List<?>) n.getStructuralProperty(spd);
	    for (Iterator<?> it1 = lcn.iterator(); it1.hasNext(); ) {
	       ASTNode cn = (ASTNode) it1.next();
	       if (overlaps(cn.getStartPosition(),cn.getLength())) return false;
	     }
	  }
       }
      return true;
    }

   boolean overlaps(int soff,int len) {
      if (start_offset >= soff+len-1) return false;
      if (end_offset <= soff) return false;
      return true;
    }

   boolean noteEdit(int soff,int len,int rlen) {
      if (end_offset <= soff) ; 			// before the change
      else if (start_offset > soff + len - 1) { 	// after the change
	 start_offset += rlen - len;
	 end_offset += rlen - len;
       }
      else if (start_offset <= soff && end_offset >= soff+len-1) {	// containing the change
	 end_offset += rlen -len;
       }
      else return false;				     // in the edit -- remove it
      return true;
    }

}	// end of inner abstract class ElideData




private class ElideRegion extends ElideData {

   ElideRegion(int soff,int eoff) {
      super(soff,eoff);
    }

}	// end of innerclass ElideData




private class ElidePriority extends ElideData {

   private double elide_priority;

   ElidePriority(int soff,int eoff,double pri) {
      super(soff,eoff);
      elide_priority = pri;
    }

   double getPriority() 			{ return elide_priority; }

}	// end of innerclass ElideData




/********************************************************************************/
/*										*/
/*	Priority computation classes						*/
/*										*/
/********************************************************************************/

private abstract static class Prioritizer {

   abstract double getPriority(double ppar,ASTNode base);

}


private static class DefaultPrioritizer extends Prioritizer {

   private double base_value;
   private double count_scale;
   private double item_scale;

   DefaultPrioritizer(double v,double scl,double iscl) {
      base_value = v;
      count_scale = scl;
      item_scale = iscl;
    }

   @Override double getPriority(double ppar,ASTNode base) {
      StructuralPropertyDescriptor spd = base.getLocationInParent();
      double dv = base_value;

      if (base.getParent() == null) return ppar * dv;

      if (item_scale != 1) {
	 List<?> pl = base.getParent().structuralPropertiesForType();
	 int ct = pl.size();
	 for (int i = 0; i < ct; ++i) {
	    if (pl.get(i) == spd) break;
	    dv *= item_scale;
	  }
       }

      if (count_scale != 1 && spd.isChildListProperty()) {
	 List<?> cl = (List<?>) base.getParent().getStructuralProperty(spd);
	 int ct = cl.size();
	 boolean fnd = false;
	 for (int i = 0; i < ct; ++i) {
	    if (cl.get(i) == base) fnd = true;
	    dv *= count_scale;
	    if (!fnd) dv *= item_scale;
	  }
       }

      return ppar * dv;
    }

}	// end of innerclass DefaultPrioritizer



private static class StructuralPrioritizer extends Prioritizer {

   private Prioritizer base_prioritizer;
   private Map<StructuralPropertyDescriptor,Prioritizer> priority_map;

   StructuralPrioritizer(Prioritizer base) {
      base_prioritizer = base;
      priority_map = new HashMap<StructuralPropertyDescriptor,Prioritizer>();
    }

   void addPriority(StructuralPropertyDescriptor spd,Prioritizer p) {
      priority_map.put(spd,p);
    }

   @Override double getPriority(double ppar,ASTNode base) {
      StructuralPropertyDescriptor spd = base.getLocationInParent();
      Prioritizer p = priority_map.get(spd);
      if (p == null) p = base_prioritizer;
      return p.getPriority(ppar,base);
    }

}	// end of innerclass StructuralPrioritizer




private static class NodePrioritizer extends Prioritizer {

   private Prioritizer base_prioritizer;
   private Map<Integer,Prioritizer> priority_map;

   NodePrioritizer(Prioritizer base) {
      base_prioritizer = base;
      priority_map = new HashMap<Integer,Prioritizer>();
    }

   void addPriority(int asttype,Prioritizer p) {
      priority_map.put(asttype,p);
    }

   @Override double getPriority(double ppar,ASTNode base) {
      Prioritizer p = priority_map.get(base.getNodeType());
      if (p == null) p = base_prioritizer;
      return p.getPriority(ppar,base);
    }

}	// end of innerclass NodePrioritizer



}	 // end of class BedrockElider




/* end of BedrockElider.java */




