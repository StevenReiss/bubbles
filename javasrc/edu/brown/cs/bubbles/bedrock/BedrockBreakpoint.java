/********************************************************************************/
/*										*/
/*		BedrockBreakpoint.java						*/
/*										*/
/*	Breakpoint manager for Bubbles - Eclipse interface			*/
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


/* SVN: $Id$ */




package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;



class BedrockBreakpoint implements BedrockConstants, IBreakpointsListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;
private DebugPlugin debug_plugin;

private enum LocationType {
   NOT_FOUND,
   LINE,
   METHOD,
   FIELD
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockBreakpoint(BedrockPlugin bp)
{
   our_plugin = bp;
   debug_plugin = DebugPlugin.getDefault();
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

void start()
{
   IBreakpointManager bm = debug_plugin.getBreakpointManager();
   bm.addBreakpointListener(this);

   setAllExceptionBreakpoint();
}




/********************************************************************************/
/*										*/
/*	Handle finding breakpoints						*/
/*										*/
/********************************************************************************/

void getAllBreakpoints(IvyXmlWriter xw)
{
   IBreakpointManager bm = debug_plugin.getBreakpointManager();
   IBreakpoint [] bps = bm.getBreakpoints();

   xw.begin("BREAKPOINTS");
   xw.field("REASON","LIST");
   for (int i = 0; i < bps.length; ++i) {
      BedrockUtil.outputBreakpoint(bps[i],xw);
    }
   xw.end("BREAKPOINTS");
}



/********************************************************************************/
/*										*/
/*	Breakpoint set routines 						*/
/*										*/
/********************************************************************************/

void setLineBreakpoint(String proj,String bid,String filename,String cls,int lineno,
			  boolean suspvm,boolean trace)
	throws BedrockException
{
   if (filename == null || lineno < 0)
      throw new BedrockException("Bad line breakpoint parameters");

   our_plugin.getEditManager().updateSingleFile(filename);

   IBreakpointManager bm = debug_plugin.getBreakpointManager();

   IFile file = our_plugin.getProjectManager().getProjectFile(proj,filename);
   if (file == null && proj != null) {
      proj = null;
      file = our_plugin.getProjectManager().getProjectFile(null,filename);
    }
   if (file == null) throw new BedrockException("Invalid file handle " + filename);

   ICompilationUnit icu = our_plugin.getCompilationUnit(proj,filename);

   if (cls == null || cls.length() == 0) {
      try {
	 IType [] typs = icu.getTypes();
	 if (typs.length > 0) {
	    cls = typs[0].getFullyQualifiedName();
	  }
	 else {
	    BedrockPlugin.logE("No class found for file " + filename + " ICU = " + icu + " " +
				  typs.length);
	  }
       }
      catch (JavaModelException e) { }
    }

   lineno = validateLine(bid,proj,filename,lineno);
   if (lineno < 0) throw new BedrockException("Breakpoint does not correspond to valid code");
   Map<String,Object> attrs = null;
   if (trace) {
      attrs = new HashMap<String,Object>();
      attrs.put("TRACEPOINT",Boolean.TRUE);
   }

   try {
      BedrockPlugin.logD("Create breakpoint " + filename + " " + cls + " " + lineno);
      IJavaBreakpoint bp = JDIDebugModel.createLineBreakpoint(file,cls,lineno,-1,-1,0,true,attrs);
      bp.setEnabled(true);
      if (suspvm) bp.setSuspendPolicy(IJavaBreakpoint.SUSPEND_VM);
      else bp.setSuspendPolicy(IJavaBreakpoint.SUSPEND_THREAD);
      bm.addBreakpoint(bp);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem setting line breakpoint: " + e);
    }
}



void setExceptionBreakpoint(String proj,String cls,boolean ct,boolean uct,boolean chk,
			       boolean suspvm) throws BedrockException
{
   if (cls == null) throw new BedrockException("Bad exception breakpoint parameters");

   IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
   IBreakpointManager bm = debug_plugin.getBreakpointManager();

   try {
      IJavaBreakpoint bp = JDIDebugModel.createExceptionBreakpoint(root,cls,ct,uct,chk,true,null);
      if (suspvm) bp.setSuspendPolicy(IJavaBreakpoint.SUSPEND_VM);
      else bp.setSuspendPolicy(IJavaBreakpoint.SUSPEND_THREAD);
      bm.addBreakpoint(bp);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem setting exception breakpoint: " + e);
    }
}



private void setAllExceptionBreakpoint()
{
   IBreakpointManager bm = debug_plugin.getBreakpointManager();

   for (IBreakpoint bp : bm.getBreakpoints()) {
      if (bp instanceof IJavaExceptionBreakpoint) {
	 IJavaExceptionBreakpoint bjp = (IJavaExceptionBreakpoint) bp;
	 try {
	    if (bjp.isCaught()) continue;
	    if (!bjp.getTypeName().equals("java.lang.Throwable")) continue;
	    if (!bjp.isEnabled()) continue;
	    // breakpoint already set, ignore
	    return;
	  }
	 catch (CoreException e ) { }
       }
    }

   try {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IBreakpoint bp = JDIDebugModel.createExceptionBreakpoint(root,"java.lang.Throwable",
								  false,true,false,false,null);
      bm.addBreakpoint(bp);
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem setting exception breakpoint: " + e);
    }
}




/********************************************************************************/
/*										*/
/*	Breakpoint clear routines						*/
/*										*/
/********************************************************************************/

void clearLineBreakpoints(String proj,String file,String cls,int line)
	throws BedrockException
{
   IBreakpointManager bm = debug_plugin.getBreakpointManager();

   if (cls == null && file != null) {
      ICompilationUnit icu = our_plugin.getProjectManager().getCompilationUnit(proj,file);
      if (icu != null) {
	 try {
	    IType [] typs = icu.getTypes();
	    for (IType typ : typs) {
	       clearLineBreakpoints(proj,null,typ.getFullyQualifiedName(),line);
	     }
	  }
	 catch (JavaModelException e) { }
       }

      return;
    }

   for (IBreakpoint bp : bm.getBreakpoints()) {
      if (bp instanceof IJavaLineBreakpoint) {
	 try {
	    IJavaLineBreakpoint jlbp = (IJavaLineBreakpoint) bp;
	    if (jlbp == null || jlbp.getTypeName() == null) continue;
	    if (cls != null && !jlbp.getTypeName().equals(cls)) continue;
	    if (line > 0 && jlbp.getLineNumber() != line) continue;
	    jlbp.delete();
	  }
	 catch (CoreException e) { }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Breakpoint edit routines						*/
/*										*/
/********************************************************************************/

void editBreakpoint(int id,String p0,String v0,String p1,String v1,
		       String p2,String v2) throws BedrockException
{
   IBreakpointManager bm = debug_plugin.getBreakpointManager();
   IBreakpoint [] bps = bm.getBreakpoints();
   IBreakpoint bp = null;
   for (int i = 0; i < bps.length; ++i) {
      if (bps[i].hashCode() == id) {
	 bp = bps[i];
	 break;
       }
    }

   if (bp == null) throw new BedrockException("Breakpoint " + id + " not found");

   if (p0.equals("CLEAR")) {
      try {
	 bp.delete();
       }
      catch (CoreException e) {
	 throw new BedrockException("Problem removing breakpoing",e);
       }
      return;
    }

   setBreakProperty(bp,p0,v0);
   setBreakProperty(bp,p1,v1);
   setBreakProperty(bp,p2,v2);
}



private void setBreakProperty(IBreakpoint bp,String p,String v) throws BedrockException
{
   if (p == null) return;

   try {
      if (p.equals("ENABLE") || p.equals("ENABLED")) {
	 if (v == null) bp.setEnabled(true);
	 else bp.setEnabled(Boolean.parseBoolean(v));
       }
      else if (p.equals("DISABLE") || p.equals("DISABLED")) {
	 bp.setEnabled(false);
       }
      else if (p.equals("CAUGHT") || p.equals("UNCAUGHT")) {
	 if (bp instanceof IJavaExceptionBreakpoint) {
	    IJavaExceptionBreakpoint eb = (IJavaExceptionBreakpoint) bp;
	    boolean v0 = true;
	    if (v != null) v0 = Boolean.parseBoolean(v);
	    if (p.equals("CAUGHT")) eb.setCaught(v0);
	    else eb.setUncaught(v0);
	  }
       }
      else if (p.equals("EXCLUDE")) {
	 if (bp instanceof IJavaExceptionBreakpoint) {
	    IJavaExceptionBreakpoint eb = (IJavaExceptionBreakpoint) bp;
	    String [] exc = v.split(",");
	    eb.setExclusionFilters(exc);
	  }
       }
      else if (p.equals("INCLUDE")) {
	 if (bp instanceof IJavaExceptionBreakpoint) {
	    IJavaExceptionBreakpoint eb = (IJavaExceptionBreakpoint) bp;
	    String [] inf = v.split(",");
	    eb.setExclusionFilters(inf);
	  }
       }
      // TODO: handle other properties
    }
   catch (CoreException e) {
      throw new BedrockException("Problem setting breakpoint property",e);
    }
}




/********************************************************************************/
/*										*/
/*	Event handler for breakpoint events					*/
/*										*/
/********************************************************************************/

@Override public void breakpointsAdded(IBreakpoint [] bpts)
{
   IvyXmlWriter xw = our_plugin.beginMessage("BREAKEVENT");

   xw.begin("BREAKPOINTS");
   xw.field("REASON","ADD");
   for (IBreakpoint bp : bpts) BedrockUtil.outputBreakpoint(bp,xw);
   xw.end();

   our_plugin.finishMessage(xw);
}


@Override public void breakpointsChanged(IBreakpoint [] bpts,IMarkerDelta [] deltas)
{
   IvyXmlWriter xw = our_plugin.beginMessage("BREAKEVENT");

   xw.begin("BREAKPOINTS");
   xw.field("REASON","CHANGE");
   for (IBreakpoint bp : bpts) BedrockUtil.outputBreakpoint(bp,xw);
   xw.end();

   our_plugin.finishMessage(xw);
}


@Override public void breakpointsRemoved(IBreakpoint [] bpts,IMarkerDelta [] deltas)
{
   IvyXmlWriter xw = our_plugin.beginMessage("BREAKEVENT");

   xw.begin("BREAKPOINTS");
   xw.field("REASON","REMOVE");
   for (IBreakpoint bp : bpts) BedrockUtil.outputBreakpoint(bp,xw);
   xw.end();

   our_plugin.finishMessage(xw);
}



/********************************************************************************/
/*										*/
/*	Methods for validating the line number for a breakpoint 		*/
/*										*/
/********************************************************************************/

private int validateLine(String bid,String proj,String file,int lno)
{
   CompilationUnit cu = null;

   try {
      cu = our_plugin.getAST(bid,proj,file);
    }
   catch (BedrockException e) { }

   if (cu == null) {
      BedrockPlugin.logD("BREAK LINE NO CU " + bid + " " + proj + " " + file);
      return lno;
    }

   BreakpointLocator bpl = new BreakpointLocator(cu,lno);
   cu.accept(bpl);

   switch (bpl.getLocationType()) {
      case NOT_FOUND :
	 lno = -1;
	 break;
      case LINE :
	 lno = bpl.getLineNumber();
	 break;
      case FIELD :
      case METHOD :
	 BedrockPlugin.logD("BREAK LINE " + bpl.getLocationType());
	 // TODO: create field or method entry/exit breakpoints as appropriate
	 break;
    }

   return lno;
}


private static class BreakpointLocator extends ASTVisitor {

   private LocationType location_type;
   private boolean location_found;
   private CompilationUnit comp_unit;
   private int line_number;
   private Stack<String> label_stack;

   BreakpointLocator(CompilationUnit cu,int lno) {
      location_type = LocationType.NOT_FOUND;
      location_found = false;
      line_number = lno;
      label_stack = null;
      comp_unit = cu;
    }

   LocationType getLocationType()		{ return location_type; }
   int getLineNumber()				{ return line_number; }

   @Override public boolean visit(AnnotationTypeDeclaration node) {
      if (visit(node, false)) {
	 List<?> bodyDeclaration= node.bodyDeclarations();
	 for (Iterator<?> iter= bodyDeclaration.iterator(); iter.hasNext();) {
	    ((BodyDeclaration)iter.next()).accept(this);
	  }
       }
      return false;
    }

   @Override public boolean visit(AnnotationTypeMemberDeclaration node) { return false; }

   @Override public boolean visit(AnonymousClassDeclaration node)	{ return visit(node,false); }
   @Override public boolean visit(ArrayAccess node)			{ return visit(node,true); }

   @Override public boolean visit(ArrayCreation node) {
      return visit(node,node.getInitializer() == null);
    }

   @Override public boolean visit(ArrayInitializer node)		{ return visit(node,true); }
   @Override public boolean visit(ArrayType node)			{ return false; }
   @Override public boolean visit(AssertStatement node) 		{ return visit(node,true); }

   @Override public boolean visit(Assignment node) {
      if (visit(node,false)) {
	 // if the left hand side represent a local variable, or a static field
	 // and the breakpoint was requested on a line before the line where
	 // starts the assigment, set the location to be the first executable
	 // instruction of the right hand side, as it will be the first part of
	 // this assigment to be executed
	 Expression lhs= node.getLeftHandSide();
	 if (lhs instanceof Name) {
	    int startline = getLineNumber(node.getStartPosition());
	    if (line_number < startline) {
	       IVariableBinding binding = (IVariableBinding)((Name)lhs).resolveBinding();
	       if (binding != null && (!binding.isField() || Modifier.isStatic(binding.getModifiers())))  {
		  node.getRightHandSide().accept(this);
		}
	     }
	  }
	 return true;
       }
      return false;
    }

   @Override public boolean visit(Block node) {
      if (visit(node,false)) {
	 if (node.statements().isEmpty() && node.getParent().getNodeType() == ASTNode.METHOD_DECLARATION) {
	    line_number = getLineNumber(node.getStartPosition() + node.getLength() - 1);
	    location_found = true;
	    location_type = LocationType.LINE;
	    return false;
	  }
	 return true;
       }
      return false;
    }

   @Override public boolean visit(BlockComment node)			{ return false; }
   @Override public boolean visit(BooleanLiteral node)			{ return visit(node,true); }
   @Override public boolean visit(BreakStatement node)			{ return visit(node,true); }
   @Override public boolean visit(CastExpression node)			{ return visit(node,true); }
   @Override public boolean visit(CatchClause node)			{ return visit(node,false); }
   @Override public boolean visit(CharacterLiteral node)		{ return visit(node,true); }
   @Override public boolean visit(ClassInstanceCreation node)		{ return visit(node,true); }
   @Override public boolean visit(CompilationUnit node) 		{ return visit(node,false); }
   @Override public boolean visit(ConditionalExpression node)		{ return visit(node,true); }
   @Override public boolean visit(ConstructorInvocation node)		{ return visit(node,true); }
   @Override public boolean visit(ContinueStatement node)		{ return visit(node,true); }
   @Override public boolean visit(CreationReference node)		{ return visit(node,true); }
   @Override public boolean visit(DoStatement node)			{ return visit(node,false); }
   @Override public boolean visit(EmptyStatement node)			{ return false; }

   @Override public boolean visit(EnhancedForStatement node) {
      if (visit(node,false)) {
	 node.getExpression().accept(this);
	 node.getBody().accept(this);
       }
      return false;
    }

   @Override public boolean visit(EnumConstantDeclaration node) {
      if (visit(node,false)) {
	 List<?> arguments= node.arguments();
	 for (Iterator<?> iter= arguments.iterator(); iter.hasNext();) {
	    ((Expression)iter.next()).accept(this);
	  }
	 AnonymousClassDeclaration decl= node.getAnonymousClassDeclaration();
	 if (decl != null) {
	    decl.accept(this);
	  }
       }
      return false;
    }

   @Override public boolean visit(EnumDeclaration node) {
      if (visit(node,false)) {
	 List<?> enumConstants= node.enumConstants();
	 for (Iterator<?> iter = enumConstants.iterator(); iter.hasNext();) {
	    ((EnumConstantDeclaration) iter.next()).accept(this);
	  }
	 List<?> bodyDeclaration= node.bodyDeclarations();
	 for (Iterator<?> iter= bodyDeclaration.iterator(); iter.hasNext();) {
	    ((BodyDeclaration)iter.next()).accept(this);
	  }
       }
      return false;
    }

   @Override public boolean visit(ExpressionMethodReference node)	{ return visit(node,true); }
   @Override public boolean visit(ExpressionStatement node)		{ return visit(node,false); }
   @Override public boolean visit(FieldAccess node)			{ return visit(node,false); }

   @Override public boolean visit(FieldDeclaration node) {
      if (visit(node,false)) {
	 // check if the line contains a single field declaration.
	 List<?> fragments = node.fragments();
	 // if (fragments.size() == 1) {
	    // int offset= ((VariableDeclarationFragment)fragments.get(0)).getName().getStartPosition();
	    // check if the breakpoint is to be set on the line which contains the name of the field
	    // if (getLineNumber(offset) == line_number) {
	       // location_type = LocationType.FIELD;
	       // location_found = true;
	       // return false;
	     // }
	  // }
	 for (Iterator<?> iter= fragments.iterator(); iter.hasNext();) {
	    ((VariableDeclarationFragment)iter.next()).accept(this);
	  }
       }
      return false;
    }

   @Override public boolean visit(ForStatement node) {
      // in case on a "for(;;)", the breakpoint can be set on the first token of the node.
      return visit(node, node.initializers().isEmpty() && node.getExpression() == null && node.updaters().isEmpty());
    }

   @Override public boolean visit(IfStatement node)			{ return visit(node,false); }
   @Override public boolean visit(ImportDeclaration node)		{ return false; }

   @Override public boolean visit(InfixExpression node) {
      // if the breakpoint is to be set on a constant operand, the breakpoint needs to be
      // set on the first constant operand after the previous non-constant operand
      // (or the beginning of the expression, if there is no non-constant operand before).
      // ex:   foo() +	  // previous non-constant operand
      //       1 +	  // breakpoint set here
      //       2	  // breakpoint asked to be set here
      if (visit(node,false)) {
	 Expression left = node.getLeftOperand();
	 Expression firstconstant = null;
	 if (visit(left,false)) {
	    left.accept(this);
	    return false;
	  }
	 if (isReplacedByConstantValue(left)) firstconstant = left;
	 Expression right = node.getRightOperand();
	 if (visit(right,false)) {
	    if (firstconstant == null || !isReplacedByConstantValue(right)) {
	       right.accept(this);
	       return false;
	     }
	  }
	 else {
	    if (isReplacedByConstantValue(right)) {
	       if (firstconstant == null) firstconstant= right;
	     }
	    else firstconstant= null;
	    List<?> extendedoperands= node.extendedOperands();
	    for (Iterator<?> iter= extendedoperands.iterator(); iter.hasNext();) {
	       Expression operand= (Expression) iter.next();
	       if (visit(operand,false)) {
		  if (firstconstant == null || !isReplacedByConstantValue(operand)) {
		     operand.accept(this);
		     return false;
		   }
		  break;
		}
	       if (isReplacedByConstantValue(operand)) {
		  if (firstconstant == null) firstconstant = operand;
		}
	       else firstconstant= null;
	     }
	  }
	 if (firstconstant != null) {
	    line_number = getLineNumber(firstconstant.getStartPosition());
	    location_found= true;
	    location_type= LocationType.LINE;
	  }
       }
      return false;
    }

   @Override public boolean visit(Initializer node)			{ return visit(node,false); }
   @Override public boolean visit(InstanceofExpression node)		{ return visit(node,true); }
   @Override public boolean visit(IntersectionType node)		{ return false; }
   @Override public boolean visit(Javadoc node) 			{ return false; }

   @Override public boolean visit(LabeledStatement node) {
      nestLabel(node.getLabel().getFullyQualifiedName());
      return visit(node, false);
    }
   @Override public void endVisit(LabeledStatement node) {
      popLabel();
      super.endVisit(node);
    }

   @Override public boolean visit(LambdaExpression node)		{ return visit(node,true); }
   @Override public boolean visit(LineComment node)			{ return false; }
   @Override public boolean visit(MarkerAnnotation node)		{ return false; }
   @Override public boolean visit(MemberRef node)			{ return false; }
   @Override public boolean visit(MemberValuePair node) 		{ return false; }

   @Override public boolean visit(MethodDeclaration node) {
      if (visit(node,false)) {
	 // check if we are on the line which contains the method name
	 // int nameoffset = node.getName().getStartPosition();
	 // if (getLineNumber(nameoffset) == line_number) {
	    // location_type = LocationType.METHOD;
	    // location_found = true;
	    // return false;
	  // }

	 // visit only the body
	 Block body = node.getBody();
	 if (body != null) { // body is null for abstract methods
	    body.accept(this);
	  }
       }
      return false;
    }

   @Override public boolean visit(MethodInvocation node)		{ return visit(node,true); }
   @Override public boolean visit(MethodRef node)			{ return false; }
   @Override public boolean visit(MethodRefParameter node)		{ return false; }
   @Override public boolean visit(Modifier node)			{ return false; }
   @Override public boolean visit(NormalAnnotation node)		{ return false; }
   @Override public boolean visit(NullLiteral node)			{ return visit(node,true); }
   @Override public boolean visit(NumberLiteral node)			{ return visit(node,true); }
   @Override public boolean visit(PackageDeclaration node)		{ return false; }
   @Override public boolean visit(ParameterizedType node)		{ return false; }
   @Override public boolean visit(ParenthesizedExpression node) 	{ return visit(node,false); }
   @Override public boolean visit(PostfixExpression node)		{ return visit(node,true); }

   @Override public boolean visit(PrefixExpression node) {
      if (visit(node,false)) {
	 if (isReplacedByConstantValue(node)) {
	    line_number = getLineNumber(node.getStartPosition());
	    location_found = true;
	    location_type = LocationType.LINE;
	    return false;
	  }
	 return true;
       }
      return false;
    }

   @Override public boolean visit(PrimitiveType node)			{ return false; }

   @Override public boolean visit(QualifiedName node) {
      visit(node,true);
      return false;
    }

   @Override public boolean visit(QualifiedType node)			{ return false; }
   @Override public boolean visit(ReturnStatement node) 		{ return visit(node,true); }

   @Override public boolean visit(SimpleName node) {
      // the name is only code if its not the current label (if any)
      return visit(node,!node.getFullyQualifiedName().equals(getLabel()));
    }

   @Override public boolean visit(SimpleType node)			{ return false; }
   @Override public boolean visit(SingleMemberAnnotation node)		{ return false; }
   @Override public boolean visit(SingleVariableDeclaration node)	{ return visit(node,false); }
   @Override public boolean visit(StringLiteral node)			{ return visit(node,true); }
   @Override public boolean visit(SuperConstructorInvocation node)	{ return visit(node,true); }
   @Override public boolean visit(SuperFieldAccess node)		{ return visit(node,true); }
   @Override public boolean visit(SuperMethodInvocation node)		{ return visit(node,true); }
   @Override public boolean visit(SuperMethodReference node)		{ return visit(node,true); }
   @Override public boolean visit(SwitchCase node)			{ return false; }
   @Override public boolean visit(SwitchStatement node) 		{ return visit(node,false); }
   @Override public boolean visit(SynchronizedStatement node)		{ return visit(node,false); }
   @Override public boolean visit(TagElement node)			{ return false; }
   @Override public boolean visit(TextElement node)			{ return false; }
   @Override public boolean visit(ThisExpression node)			{ return visit(node,true); }
   @Override public boolean visit(ThrowStatement node)			{ return visit(node,true); }
   @Override public boolean visit(TryStatement node)			{ return visit(node,false); }

   @Override public boolean visit(TypeDeclaration node) {
      if (visit(node,false)) {
	 // visit only the elements of the type declaration
	 List<?> bodydeclaration= node.bodyDeclarations();
	 for (Iterator<?> iter= bodydeclaration.iterator(); iter.hasNext();) {
	    ((BodyDeclaration)iter.next()).accept(this);
	  }
       }
      return false;
    }

   @Override public boolean visit(TypeDeclarationStatement node)	{ return visit(node,false); }
   @Override public boolean visit(TypeMethodReference node)		{ return visit(node,true); }
   @Override public boolean visit(TypeParameter node)			{ return false; }
   @Override public boolean visit(TypeLiteral node)			{ return false; }
   @Override public boolean visit(UnionType node)			{ return false; }
   @Override public boolean visit(VariableDeclarationExpression node)	{ return visit(node,false); }

   @Override public boolean visit(VariableDeclarationFragment node) {
      Expression initializer = node.getInitializer();
      if (visit(node,false) && initializer != null) {
	 int startline = getLineNumber(node.getName().getStartPosition());
	 if (line_number == startline) {
	    location_found = true;
	    location_type= LocationType.LINE;
	    return false;
	  }
	 initializer.accept(this);
       }
      return false;
    }

   @Override public boolean visit(WildcardType node)			{ return false; }
   @Override public boolean visit(VariableDeclarationStatement node)	{ return visit(node,false); }
   @Override public boolean visit(WhileStatement node)			{ return visit(node,false); }


   private boolean visit(ASTNode n,boolean code) {
      if (location_found) return false;
      int spos = n.getStartPosition();
      int endline = getLineNumber(spos + n.getLength() - 1);
      if (endline < line_number) return false;
      int startline = getLineNumber(spos);
      if (code && line_number <= startline) {
	 line_number = startline;
	 location_found = true;
	 location_type = LocationType.LINE;
	 return false;
       }
      return true;
    }

   private boolean isReplacedByConstantValue(Expression node) {
      switch (node.getNodeType()) {
	 case ASTNode.BOOLEAN_LITERAL:
	 case ASTNode.CHARACTER_LITERAL:
	 case ASTNode.NUMBER_LITERAL:
	 case ASTNode.STRING_LITERAL:
	    return true;
	 case ASTNode.SIMPLE_NAME:
	 case ASTNode.QUALIFIED_NAME:
	    return isReplacedByConstantValue((Name)node);
	 case ASTNode.FIELD_ACCESS:
	    return isReplacedByConstantValue((FieldAccess)node);
	 case ASTNode.SUPER_FIELD_ACCESS:
	    return isReplacedByConstantValue((SuperFieldAccess)node);
	 case ASTNode.INFIX_EXPRESSION:
	    return isReplacedByConstantValue((InfixExpression)node);
	 case ASTNode.PREFIX_EXPRESSION:
	    return isReplacedByConstantValue((PrefixExpression)node);
	 case ASTNode.CAST_EXPRESSION:
	    return isReplacedByConstantValue(((CastExpression)node).getExpression());
	 default:
	    return false;
       }
    }

   private boolean isReplacedByConstantValue(InfixExpression node) {
      if (!(isReplacedByConstantValue(node.getLeftOperand()) && isReplacedByConstantValue(node.getRightOperand()))) {
	 return false;
       }
      if (node.hasExtendedOperands()) {
	 for (Iterator<?> iter = node.extendedOperands().iterator(); iter.hasNext(); ) {
	    if (!isReplacedByConstantValue((Expression) iter.next())) {
	       return false;
	     }
	  }
       }
      return true;
    }

   private boolean isReplacedByConstantValue(PrefixExpression node) {
      // for '-', '+', '~' and '!', if the operand is a constant value,
      // the expression is replaced by a constant value
      PrefixExpression.Operator operator = node.getOperator();
      if (operator != PrefixExpression.Operator.INCREMENT && operator != PrefixExpression.Operator.DECREMENT) {
	 return isReplacedByConstantValue(node.getOperand());
       }
      return false;
    }

   private boolean isReplacedByConstantValue(Name node) {
      // if node is a variable with a constant value (static final field)
      IBinding binding= node.resolveBinding();
      if (binding != null && binding.getKind() == IBinding.VARIABLE) {
	 return ((IVariableBinding)binding).getConstantValue() != null;
       }
      return false;
    }

   private boolean isReplacedByConstantValue(FieldAccess node) {
      // if the node is 'this.<field>', and the field is static final
      Expression expression= node.getExpression();
      IVariableBinding binding= node.resolveFieldBinding();
      if (binding != null && expression.getNodeType() == ASTNode.THIS_EXPRESSION) {
	 return binding.getConstantValue() != null;
       }
      return false;
    }

   private boolean isReplacedByConstantValue(SuperFieldAccess node) {
      // if the field is static final
      IVariableBinding binding= node.resolveFieldBinding();
      if (binding != null) {
	 return binding.getConstantValue() != null;
       }
      return false;
    }

    private String getLabel() {
       if (label_stack == null || label_stack.empty()) return null;
       return label_stack.peek();
     }

    private void nestLabel(String label) {
       if (label_stack == null) label_stack = new Stack<String>();
       label_stack.push(label);
     }

    private void popLabel() {
       if (label_stack == null || label_stack.empty()) return;
       label_stack.pop();
     }

   private int getLineNumber(int offset) {
      int lno = comp_unit.getLineNumber(offset);
      return lno < 1 ? 1 : lno;
    }


}	// end of inner class BreakpointLocator



}	// end of class BedrockBreakpoint




/* end of BedrockBreakpoint.java */

