/********************************************************************************/
/*										*/
/*		NobaseValuePass.java						*/
/*										*/
/*	Semantics resolution pass to compute names,types, and values		*/
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



package edu.brown.cs.bubbles.nobase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.wst.jsdt.core.dom.*;


class NobaseValuePass extends DefaultASTVisitor implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseScope		global_scope;
private NobaseProject		for_project;

private NobaseScope cur_scope;
private List<NobaseMessage> error_list;
private boolean change_flag;
private NobaseValue set_lvalue;
private String enclosing_function;
private NobaseFile enclosing_file;
private Stack<String> name_stack;
private Stack<NobaseValue> function_stack;
private boolean force_define;


private static Map<String,Evaluator>   operator_evaluator;


static {
   operator_evaluator = new HashMap<String,Evaluator>();
   operator_evaluator.put("+",new EvalPlus());
   operator_evaluator.put("-",new EvalNumeric("-"));
   operator_evaluator.put("*",new EvalNumeric("*"));
   operator_evaluator.put("/",new EvalNumeric("/"));
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseValuePass(NobaseProject proj,NobaseScope glbl,NobaseFile module,
	List<NobaseMessage> errors)
{
   global_scope = glbl;
   for_project = proj;
   cur_scope = global_scope;
   error_list = errors;
   change_flag = false;
   set_lvalue = null;
   force_define = false;
   enclosing_file = module;
   enclosing_function = null;
   name_stack = new Stack<>();
   function_stack = new Stack<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean checkChanged()
{
   boolean rslt = change_flag;
   change_flag = false;
   return rslt;
}


void setForceDefine()				{ force_define = true; }



/********************************************************************************/
/*                                                                              */
/*      Error checking methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public void preVisit(ASTNode n)
{
   if (force_define) return;                    // only consider first pass
   NobaseMain.logD("Visit " + n.getClass().getName() + " " + n.properties() + " " + n.getFlags() + " " + n);
  
   if (n instanceof SimpleName) {
      switch (((SimpleName) n).getIdentifier()) {
         case "MISSING" :
//          System.err.println("CHECK MISSING " + n.getParent().getClass() + " " +
//                n.getParent().getParent().getClass());
            switch (n.getParent().getNodeType()) {
               case ASTNode.PARENTHESIZED_EXPRESSION :
                  if (n.getParent().getParent().getNodeType() == ASTNode.FOR_OF_STATEMENT) ;
                  else {
                     NobaseMessage msg = new NobaseMessage(ErrorSeverity.ERROR,
                           "Parenthesis error",
                           NobaseAst.getLineNumber(n),NobaseAst.getColumn(n),
                           NobaseAst.getEndLine(n),NobaseAst.getEndColumn(n));
                     error_list.add(msg);
                   }
                  break;
             }
            break;
         default :
            break;
       }
    }
   
// if (n instanceof Expression) {
//    Expression en = (Expression) n;
//    NobaseMain.logD("\tConstant value: " + en.resolveConstantExpressionValue());
//    NobaseMain.logD("\tType value: " + en.resolveTypeBinding());
//    if (n instanceof FunctionInvocation) {
//       FunctionInvocation fi = (FunctionInvocation) n;
//       NobaseMain.logD("\tFunction value: " + fi.resolveMethodBinding());
//     }
//    if (n instanceof Name) {
//       Name nm = (Name) n;
//       NobaseMain.logD("\tName value: " + nm.resolveBinding());
//     }
//  }
// else if (n instanceof VariableDeclaration) {
//    VariableDeclaration vd = (VariableDeclaration) n;
//    NobaseMain.logD("\tBinding: " + vd.resolveBinding());
//  }
   
   if ((n.getFlags() & ASTNode.MALFORMED) != 0) {
      NobaseMain.logD("Malformed node: " + n + " " + n.properties());
      NobaseMessage msg = new NobaseMessage(ErrorSeverity.ERROR,
            "Syntax error",
            NobaseAst.getLineNumber(n),NobaseAst.getColumn(n),
            NobaseAst.getEndLine(n),NobaseAst.getEndColumn(n));
      error_list.add(msg);
    }
}


@Override public void postVisit(ASTNode n) 
{
   if (force_define) return;                    // only consider first pass
   NobaseMain.logD("EndVisit " + n.getClass().getName() + " " + n.properties());
}


/********************************************************************************/
/*										*/
/*	Top Level visit methods 						*/
/*										*/
/********************************************************************************/

@Override public boolean visit(JavaScriptUnit b)
{
   cur_scope = NobaseAst.getScope(b);
   if (cur_scope == null) {
      cur_scope = new NobaseScope(ScopeType.FILE,global_scope);
      for_project.setupModule(enclosing_file,cur_scope);
      NobaseAst.setScope(b,cur_scope);
    }
   NobaseSymbol osym = NobaseAst.getDefinition(b);
   if (osym == null) {
      NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,b,
	    enclosing_file.getModuleName(),false);
      nsym.setBubblesName(enclosing_file.getModuleName());
      NobaseAst.setDefinition(b,nsym);
    }

   return true;
}



@Override public void endVisit(JavaScriptUnit b)
{
   for_project.finishModule(enclosing_file);
   cur_scope = cur_scope.getParent();
}



/********************************************************************************/
/*										*/
/*	Constants and literal handling						*/
/*										*/
/********************************************************************************/

@Override public void endVisit(BooleanLiteral n)
{
   setValue(n,NobaseValue.createBoolean(n.booleanValue()));
}


@Override public void endVisit(CharacterLiteral n)
{
   setValue(n,NobaseValue.createNumber(n.charValue()));
}

@Override public void endVisit(NullLiteral n)
{
   setValue(n,NobaseValue.createNull());
}



@Override public void endVisit(NumberLiteral n)
{
   String nv = n.getToken();
   Number val = NobaseUtil.convertStringToNumber(nv);

   setValue(n,NobaseValue.createNumber(val));
}


@Override public boolean visit(ObjectLiteral n)
{
   NobaseScope scp = new NobaseScope(ScopeType.OBJECT,cur_scope);
   scp.setValue(NobaseValue.createObject());
   NobaseAst.setScope(n,scp);

   cur_scope = scp;
   for (Object o : n.fields()) {
      ObjectLiteralField olf = (ObjectLiteralField) o;
      olf.accept(this);
    }

   cur_scope = scp.getParent();

   return false;
}


@Override public boolean visit(ObjectLiteralField n)
{
   Expression ex = n.getInitializer();
   NobaseValue nv = NobaseAst.getNobaseValue(ex);
   
   if (nv == null && ex instanceof SimpleName && ex.toString().equals("MISSING")) {
      nv = NobaseValue.createUndefined();
      NobaseAst.setNobaseValue(ex,nv);
    }
   else if (nv == null) {
      ex.accept(this);
      nv = NobaseAst.getNobaseValue(ex);
    }
   
   Object fnm = getIdentName(n.getFieldName(),ex);
   NobaseValue objv = cur_scope.getThisValue();
   if (objv != null && fnm != null) {
      switch (n.getKind()) {
         case INIT :
            objv.addProperty(fnm,nv);
            break;
         case GET :
            objv.addGetterProperty(fnm,nv);
            break;
         case SET :
            objv.addSetterProperty(fnm,nv);
            break;
       }
    }
   else if (objv != null) {
      objv.addProperty(null,nv);
    }
   return false;
}


@Override public void endVisit(RegularExpressionLiteral n)
{
   NobaseSymbol regex = global_scope.lookup("RegExp");
   NobaseValue nv = NobaseValue.createObject();
   if (regex != null) {
      nv.setBaseValue(regex.getValue());
    }
   setValue(n,nv);
}


@Override public void endVisit(StringLiteral n)
{
   set_lvalue = null;
   try {
      setValue(n,NobaseValue.createString(n.getLiteralValue()));
    }
   catch (IllegalArgumentException e) {
      // incomplete string or string with error
      String s0 = n.getEscapedValue();
      if (s0 != null && s0.length() > 0) {
         char c0 = s0.charAt(0);
         if (c0 == '"' || c0 == '\'' || c0 == '`') {
            s0 = s0.substring(1);
            if (s0.length() > 0) {
               char c1 = s0.charAt(s0.length()-1);
               if (c1 == c0) s0 = s0.substring(0,s0.length()-1);
             }
          }
       }
      setValue(n,NobaseValue.createString(s0));
    }
}


@Override public boolean visit(TemplateLiteral n)
{
   for (Object o : n.expressions()) {
      Expression ex = (Expression) o;
      ex.accept(this);
    }
   
   setValue(n,NobaseValue.createString());
   
   return false;
}

@Override public void endVisit(TemplateElement n) 
{
   setValue(n,NobaseValue.createString(n.getRawValue()));
}


@Override public void endVisit(UndefinedLiteral n)
{
   setValue(n,NobaseValue.createUndefined());
}



/********************************************************************************/
/*										*/
/*	Expression processing							*/
/*										*/
/********************************************************************************/

@Override public boolean visit(ArrayAccess n)
{
   NobaseValue lvl = set_lvalue;
   set_lvalue = null;
   n.getArray().accept(this);
   n.getIndex().accept(this);
   NobaseValue nvl = NobaseAst.getNobaseValue(n.getArray());
   NobaseValue ivl = NobaseAst.getNobaseValue(n.getIndex());
   if (lvl == null) {
      Object idxv = (ivl == null ? null : ivl.getKnownValue());
      NobaseValue rvl = null;
      if (nvl != null) rvl = nvl.getProperty(idxv,(lvl != null));
      if (rvl == null) rvl = NobaseValue.createUnknownValue();
      setValue(n,rvl);
    }
   else {
      Object idxv = (ivl == null ? null : ivl.getKnownValue());
      if (idxv != null && idxv instanceof String && nvl != null) {
	 if (nvl.addProperty(idxv,lvl)) change_flag = true;
       }
      else if (nvl != null) nvl.addProperty(null,lvl);
      setValue(n,lvl);
    }
   return false;
}


@Override public boolean visit(ArrayName n)
{
   NobaseValue lvl = set_lvalue;
   if (set_lvalue == null) return true;
   int idx = 0;
   for (Object o : n.elements()) {
      ASTNode ex = (ASTNode) o;
      NobaseValue idxv = NobaseValue.createNumber(idx);
      NobaseValue nlvl = lvl.getProperty(idxv,false);
      if (nlvl == null) nlvl = NobaseValue.createAnyValue();
      set_lvalue = nlvl;
      ex.accept(this);
      set_lvalue = lvl;
    }
   
   return false;
}



@Override public void endVisit(ArrayCreation n)
{
   // new Type [ dims ] ...
}


@Override public void endVisit(ArrayInitializer n)
{
   List<NobaseValue> vals = new ArrayList<NobaseValue>();
   for (Object o : n.expressions()) {
      Expression e = (Expression) o;
      NobaseValue nv = null;
      if (e instanceof EmptyExpression) nv = NobaseValue.createAnyValue();
      else nv = NobaseAst.getNobaseValue(e);
      vals.add(nv);
    }
   setValue(n,NobaseValue.createArrayValue(vals));
}


@Override public void endVisit(ArrowFunctionExpression n)
{
   // (parameters) => body|expression

   //TODO: define function with anonymous name
   setValue(n,NobaseValue.createFunction(n));
}


@Override public boolean visit(Assignment n)
{
   NobaseValue ovl = set_lvalue;
   set_lvalue = null;
   n.getRightHandSide().accept(this);
   if (n.getOperator() == Assignment.Operator.ASSIGN) {
      set_lvalue = NobaseAst.getNobaseValue(n.getRightHandSide());
      NobaseMain.logD("Assignment " + n + " = " + set_lvalue);
    }
   n.getLeftHandSide().accept(this);
   set_lvalue = ovl;
   return false;
}


@Override public void endVisit(ClassInstanceCreation n)
{
   NobaseValue nv = NobaseValue.createObject();
   NobaseValue fv = NobaseAst.getNobaseValue(n.getName());
   nv.setBaseValue(fv);
   nv.mergeProperties(fv);
   setValue(n,nv);
}



@Override public void endVisit(ConditionalExpression n)
{
   NobaseValue nv = NobaseValue.createAnyValue();
   setValue(n,nv);
}


// Visitor has no calls for EmptyExpression


@Override public boolean visit(FieldAccess n)
{
   NobaseValue lval = set_lvalue;
   set_lvalue = null;
   n.getExpression().accept(this);
   set_lvalue = lval;

   NobaseScope origscp = cur_scope;
   NobaseScope scp = NobaseAst.getScope(n);
   if (scp == null) {
      NobaseValue exval = NobaseAst.getNobaseValue(n.getExpression());
      if (exval != null) scp = exval.getAssociatedScope();
      if (scp == null) scp = new NobaseScope(ScopeType.MEMBER,cur_scope);
      NobaseAst.setScope(n,scp);
    }
   scp.setValue(NobaseAst.getNobaseValue(n.getExpression()));
   cur_scope = scp;
   if (set_lvalue != null) {
      String nm = n.getName().getIdentifier();
      cur_scope.setProperty(nm,set_lvalue);
      // n.getName().accept(this);
      // this generates an error since the name is not defined
      // previously in the current (function) scope
      setValue(n,set_lvalue);
    }
   else {
      n.getName().accept(this);
      setValue(n,NobaseAst.getNobaseValue(n.getName()));
    }
   cur_scope = origscp;
   return false;
}


@Override public void endVisit(FunctionExpression n)
{
   FunctionDeclaration fd = n.getMethod();
   setValue(n,NobaseAst.getNobaseValue(fd));
}


// expression is optional and needs to do field lookup
@Override public boolean visit(FunctionInvocation n)
{
   NobaseScope scp = null;
   if (n.getExpression() != null) {
      n.getExpression().accept(this);
      scp = NobaseAst.getScope(n);
      if (scp == null) {
	 scp = new NobaseScope(ScopeType.MEMBER,cur_scope);
	 NobaseAst.setScope(n,scp);
       }
      scp.setValue(NobaseAst.getNobaseValue(n.getExpression()));
      cur_scope = scp;
    }
   NobaseValue fv = null;
   if (n.getName() != null) {
      n.getName().accept(this);
      fv = NobaseAst.getNobaseValue(n.getName());
    }
   else if (n.getExpression() != null) {
      fv = NobaseAst.getNobaseValue(n.getExpression());
    }

   if (scp != null) cur_scope = cur_scope.getParent();
   else scp = cur_scope;

   List<NobaseValue> args = new ArrayList<NobaseValue>();
   for (Object o : n.arguments()) {
      Expression e = (Expression) o;
      e.accept(this);
      args.add(NobaseAst.getNobaseValue(e));
    }

   NobaseSymbol thissym = scp.lookup("this");
   NobaseValue thisval = null;
   if (thissym != null) thisval = thissym.getValue();

   if (fv != null) {
      NobaseValue nv = fv.evaluate(enclosing_file,args,thisval);
      setValue(n,nv);
      if (nv.isKnown()) {
         NobaseMain.logD("Function evaluation returned " + nv);
       }
    }
   else {
      NobaseMain.logD("Nothing to evaluate: " + n);
    }

   return false;
}



@Override public void endVisit(InfixExpression n)
{
   NobaseValue nv = null;
   String ops = n.getOperator().toString();
   Evaluator ev = operator_evaluator.get(ops);

   if (ev != null) {
      List<NobaseValue> args = new ArrayList<NobaseValue>();
      args.add(NobaseAst.getNobaseValue(n.getLeftOperand()));
      args.add(NobaseAst.getNobaseValue(n.getRightOperand()));
      if (n.hasExtendedOperands()) {
	 for (Object o : n.extendedOperands()) {
	    Expression e = (Expression) o;
	    args.add(NobaseAst.getNobaseValue(e));
	  }
       }
      nv = ev.evaluate(enclosing_file,args,NobaseValue.createUndefined());
    }
   else {
      switch (ops) {
	 case "+" :
	    // handled by evaluator
	    break;
	 case "*" :
	 case "/" :
	 case "%" :
	 case "-" :
	 case "<<" :
	 case ">>" :
	 case ">>>" :
	 case "^" :
	 case "&" :
	 case "|" :
	    nv = NobaseValue.createNumber();
	    break;
	 case "<" :
	 case ">" :
	 case "<=" :
	 case ">=" :
	 case "==" :
	 case "!=" :
	 case "&&" :
	 case "||" :
	 case "===" :
	 case "in" :
	    nv = NobaseValue.createBoolean();
	    break;
	 default :
	    nv = NobaseValue.createAnyValue();
	    break;
       }
    }

   setValue(n,nv);
}


@Override public void endVisit(InstanceofExpression n)
{
   setValue(n,NobaseValue.createBoolean());
}



@Override public void endVisit(ListExpression n)
{
   List<?> exprs = n.expressions();
   if (exprs.isEmpty()) return;
   Expression e = (Expression) exprs.get(exprs.size()-1);
   NobaseValue nv = NobaseAst.getNobaseValue(e);
   setValue(n,nv);
}



@Override public void endVisit(MetaProperty n)
{
   // new.target
   setValue(n,NobaseValue.createAnyValue());
}



@Override public void endVisit(ParenthesizedExpression e)
{
   NobaseValue nv = NobaseAst.getNobaseValue(e.getExpression());
   NobaseAst.setNobaseValue(e,nv);
}


@Override public void endVisit(PostfixExpression n)
{
   NobaseValue nv = null;
   // ++ --
   nv = NobaseValue.createAnyValue();

   setValue(n,nv);
}


@Override public void endVisit(PrefixExpression n)
{
   NobaseValue nv = null;
   if (n.getOperator() == PrefixExpression.Operator.DELETE) {
      nv = NobaseValue.createAnyValue();
    }
   else if (n.getOperator() == PrefixExpression.Operator.TYPE_OF) {
      nv = NobaseValue.createString();
    }
   else {
      // ~ -- ++ - ! + void
      nv = NobaseValue.createAnyValue();
    }

   setValue(n,nv);
}



@Override public void endVisit(SpreadElement n)
{
   // ... args
   setValue(n,NobaseValue.createAnyValue());
}


@Override public void endVisit(SuperFieldAccess n)
{
   // suepr.gield

   setValue(n,NobaseValue.createAnyValue());
}


@Override public void endVisit(SuperMethodInvocation n)
{
   NobaseValue nv = NobaseValue.createUnknownValue();
   NobaseSymbol thissym = cur_scope.lookup("this");
   NobaseValue thisval = thissym.getValue();

   List<NobaseValue> args = new ArrayList<NobaseValue>();
   for (Object o : n.arguments()) {
      Expression e = (Expression) o;
      args.add(NobaseAst.getNobaseValue(e));
    }
   nv = thisval.evaluate(enclosing_file,args,thisval);

   setValue(n,nv);
}



@Override public void endVisit(TypeDeclarationExpression n)
{
   // class ...
   setValue(n,NobaseValue.createUndefined());
}


@Override public void endVisit(VariableDeclarationExpression n)
{
   NobaseValue v = NobaseValue.createUndefined();
   for (Object o : n.fragments()) {
      ASTNode n1 = (ASTNode) o;
      NobaseValue v1 = NobaseAst.getNobaseValue(n1);
      if (v1 != null) v = v1;
    }
      
   // var x = ...
   if (NobaseAst.getNobaseValue(n) == null) {
      setValue(n,v);
    }
}


@Override public void endVisit(YieldExpression n)
{
   // yeield <expr>

   setValue(n,NobaseValue.createAnyValue());
}



/********************************************************************************/
/*										*/
/*	Name methods								*/
/*										*/
/********************************************************************************/

@Override public void endVisit(ArrayName n)
{
   List<NobaseValue> vals = new ArrayList<>();
   for (Object o : n.elements()) {
      ASTNode ex = (ASTNode) o;
      NobaseValue v = NobaseAst.getNobaseValue(ex);
      vals.add(v);
    }
   NobaseValue rslt = NobaseValue.createArrayValue(vals);
   NobaseAst.setNobaseValue(n,rslt);
}

@Override public void endVisit(AssignmentName n)
{
   NobaseMain.logD("Assignment Name " + n);
}

@Override public void endVisit(ObjectName n)
{ }

@Override public void endVisit(QualifiedName n)
{ 
   // Name.Identifier
}

@Override public void endVisit(RestElementName n)
{ 
   // ...Name
   
}


@Override public void endVisit(SimpleName id)
{
   handleName(id.getIdentifier(),id);
}


@Override public void endVisit(ThisExpression n)
{
   handleName("this",n);
}



private void handleName(String name,ASTNode id)
{
   NobaseSymbol ref = NobaseAst.getDefinition(id);
   if (ref == null) ref = NobaseAst.getReference(id);
   if (ref == null) {
      ref = cur_scope.lookup(name);
      NobaseValue val = cur_scope.lookupValue(name,(set_lvalue != null));
      if (ref == null && force_define && val == null &&
	    !NobaseResolver.isGeneratedName(name)) {
	 // see if we should create implicit definition
	 NobaseScope dscope = cur_scope.getDefaultScope();
         if (cur_scope.getScopeType() != ScopeType.FUNCTION) {
            switch (dscope.getScopeType()) {
               case FILE :
               case GLOBAL :
               case FUNCTION :
               case CLASS :
                  NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
                        "Implicit declaration of " + name,
                        NobaseAst.getLineNumber(id),NobaseAst.getColumn(id),
                        NobaseAst.getEndLine(id),NobaseAst.getEndColumn(id));
                  error_list.add(msg);
                  ref = new NobaseSymbol(for_project,enclosing_file,id,name,false);
                  setName(ref,name,dscope);
                  dscope.define(ref);
                  break;
               default :
                  dscope.setProperty(name,NobaseValue.createAnyValue());
                  break;
             }
          }
       }
      if (ref != null) {
	 NobaseAst.setReference(id,ref);
	 NobaseScope dscp = ref.getDefScope();
	 if (dscp != null) {
	    boolean fnd = false;
	    for (NobaseScope scp = cur_scope; scp != null; scp = scp.getParent()) {
	       if (scp == dscp) {
		  fnd = true;
		  break;
		}
	     }
	    if (!fnd) {
	       NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
		     "Possible misuse of variable " + name + " based on lexical scope ",
		     NobaseAst.getLineNumber(id),NobaseAst.getColumn(id),
		     NobaseAst.getEndLine(id),NobaseAst.getEndColumn(id));
	       error_list.add(msg);
	     }
	  }
       }
    }
   if (ref != null) {
      if (set_lvalue != null) {
	 NobaseValue nv = NobaseValue.mergeValues(set_lvalue,ref.getValue());
	 setValue(id,nv);
       }
      else {
	 NobaseAst.setNobaseValue(id,ref.getValue());
       }
    }
   else {
      NobaseValue nv = cur_scope.lookupValue(name,(set_lvalue != null));
      if (nv == null && force_define) {
         if (!NobaseResolver.isGeneratedName(name)) {
            NobaseMain.logD("No value found for " + name + " at " +
                  NobaseAst.getLineNumber(id));
          }
	 nv = NobaseValue.createUnknownValue();
       }
      NobaseAst.setNobaseValue(id,nv);
    }
}




/********************************************************************************/
/*										*/
/*	Statement methods							*/
/*										*/
/********************************************************************************/

@Override public boolean visit(Block n)
{
   localScopeBegin(n);
// System.err.println("CHECK BLOCK " + n.getLength() + " " + n.getFlags() + " " +
//       (n.getBodyChild() == n) + " " + n.getStartPosition() + " " +
//       n.getLength() + " " + n.statements().size());
   
   return true;
}

@Override public void endVisit(Block n)
{
   localScopeEnd();
}



@Override public void endVisit(BreakStatement n) { }


@Override public boolean visit(CatchClause n) {
   localScopeBegin(n);
   return true;
}

@Override public void endVisit(CatchClause n) {
   localScopeEnd();
}


@Override public boolean visit(ConstructorInvocation n)
{
   NobaseScope scp = new NobaseScope(ScopeType.OBJECT,cur_scope);
   NobaseValue thisval = NobaseValue.createObject();
   scp.setValue(thisval);
   NobaseAst.setScope(n,scp);
   cur_scope = scp;

   for (Object o : n.arguments()) {
      Expression e = (Expression) o;
      e.accept(this);
    }
   cur_scope = scp.getParent();

   return false;
}



@Override public void endVisit(ContinueStatement n) { }
@Override public void endVisit(DoStatement n) { }
@Override public void endVisit(EmptyStatement n) { }


@Override public boolean visit(EnhancedForStatement n)
{
   localScopeBegin(n);
   return true;
}

@Override public void endVisit(EnhancedForStatement n)
{
   localScopeEnd();
}


@Override public boolean visit(ForInStatement n)
{
   localScopeBegin(n);
   
   handleAwaitedFor(n.getIterationVariable()); 
   
   return true;
}


@Override public void endVisit(ForInStatement n)
{
   localScopeEnd();
}


@Override public boolean visit(ForOfStatement n)
{
   localScopeBegin(n);
   
   handleAwaitedFor(n.getIterationVariable());
   
   return true;
}


private void handleAwaitedFor(ASTNode n)
{
   if (n.toString().equals("MISSING")) {
      NobaseSymbol sym = NobaseAst.getDefinition(n);
      if (sym != null) return;
      
      int start = n.getStartPosition();
      int len = n.getLength();
      String cnts = enclosing_file.getContents().substring(start,start+len);
      if (cnts.startsWith("(")) cnts = cnts.substring(1);
      int idx = cnts.indexOf(" ");
      if (idx < 0) return;
      String what = cnts.substring(0,idx).trim();
      String name = cnts.substring(idx).trim();
      SimpleName sn = n.getAST().newSimpleName(name);
      VariableKind vk = VariableKind.VAR;
      switch (what) {
         case "const" :
            vk = VariableKind.CONST;
            break;
         case "let" :
            vk = VariableKind.LET;
            break;
         case "var" :
            vk = VariableKind.VAR;
            break;
         default :
            NobaseMain.logE("Unknown variable kind " + what);
            break;
       }
      defineName(sn,null,null,n,null,vk);
    }
}


@Override public void endVisit(ForOfStatement n)
{
   localScopeEnd();
}


@Override public boolean visit(ForStatement n)
{
   localScopeBegin(n);
   return true;
}


@Override public void endVisit(ForStatement n)
{
   localScopeEnd();
}


@Override public void endVisit(FunctionDeclarationStatement n) { }



@Override public boolean visit(IfStatement n)
{
   localScopeBegin(n);
   return true;
}

@Override public void endVisit(IfStatement n)
{
   localScopeEnd();
}


@Override public void endVisit(LabeledStatement n)	{ }


@Override public void endVisit(ReturnStatement n)
{
   Expression exp = n.getExpression();
   if (exp != null && function_stack.size() > 0) {
      NobaseValue fvalue = function_stack.peek();
      fvalue.setReturnValue(NobaseAst.getNobaseValue(exp));
    }
}


@Override public void endVisit(SuperConstructorInvocation n)
{
   setValue(n,NobaseValue.createUndefined());
}


@Override public boolean visit(SwitchStatement n)
{
   localScopeBegin(n);
   return true;
}


@Override public void endVisit(SwitchStatement n)
{
   localScopeEnd();
}

@Override public void endVisit(SwitchCase n)		{ }



@Override public void endVisit(ThrowStatement n)	{ }


@Override public boolean visit(TryStatement n)
{
   localScopeBegin(n);
   return true;
}

@Override public void endVisit(TryStatement n)
{
   localScopeEnd();
}


@Override public void endVisit(TypeDeclarationStatement n)
{
   setValue(n,NobaseValue.createUndefined());
}


@Override public void endVisit(VariableDeclarationStatement n)
{
}


@Override public boolean visit(WhileStatement n)
{
   localScopeBegin(n);
   return true;
}


@Override public void endVisit(WhileStatement n)
{
   localScopeEnd();
}


@Override public boolean visit(WithStatement wstmt)
{
   wstmt.getExpression().accept(this);
   NobaseValue nv = NobaseAst.getNobaseValue(wstmt);
   NobaseScope nscp = NobaseAst.getScope(wstmt);
   if (nscp == null) {
      nscp = new NobaseScope(ScopeType.WITH,cur_scope);
      NobaseAst.setScope(wstmt,nscp);
    }
   cur_scope = nscp;
   cur_scope.setValue(nv);
   wstmt.getBody().accept(this);
   cur_scope = cur_scope.getParent();
   return false;
}




/********************************************************************************/
/*										*/
/*	Type methods								*/
/*										*/
/********************************************************************************/

@Override public void endVisit(ArrayType n) { }
@Override public void endVisit(InferredType n) 
{
   if (n.getType() != null) {
      NobaseMain.logI("Inferred type: " + n.getType());
    }
}

@Override public void endVisit(Modifier n) { }
@Override public void endVisit(PrimitiveType n) { }
@Override public void endVisit(QualifiedType n) { }
@Override public void endVisit(SimpleType n) { }

@Override public void endVisit(TypeLiteral n)
{
   // type.class
   setValue(n,NobaseValue.createAnyValue());	// should be type value
}



/********************************************************************************/
/*										*/
/*	Class methods								*/
/*										*/
/********************************************************************************/

@Override public void endVisit(AnonymousClassDeclaration n) { }
@Override public void endVisit(FieldDeclaration n) { }
@Override public boolean visit(TypeDeclaration n)
{
   NobaseScope defscope = cur_scope;
   String clsname = n.getName().getIdentifier();
   NobaseValue nv = NobaseAst.getNobaseValue(n);
   if (nv == null) {
      nv = NobaseValue.createClass(n);
      setValue(n,nv);
    }
   function_stack.push(nv);
   
   NobaseSymbol osym = NobaseAst.getDefinition(n);
   if (osym == null) {
      NobaseSymbol nsym = new NobaseSymbol(for_project,
            enclosing_file,n,clsname,true);
      setName(nsym,clsname,defscope);
      nsym.setValue(nv);
      osym = defscope.define(nsym);
      if (nsym != osym) {
         duplicateDef(clsname,n);
         nsym = osym;
       }
    }
   NobaseAst.setDefinition(n,osym);
   
   NobaseScope nscp = NobaseAst.getScope(n);
   if (nscp == null) {
      NobaseValue othis = null;
      NobaseSymbol othissym = cur_scope.lookup("this");
      if (othissym != null) othis = othissym.getValue();
      nscp = new NobaseScope(ScopeType.CLASS,cur_scope);
      NobaseAst.setScope(n,nscp);
      nscp.setValue(nv);
      if (this != null) {
         nv.mergeProperties(othis);
       }
      NobaseSymbol thissym = new NobaseSymbol(for_project,null,null,"this",true);
      thissym.setValue(nv);
      nscp.define(thissym);
    }
   ASTNode n1 = n.getSuperclassExpression();
   if (n1 != null) {
      NobaseMain.logD("SUPER CLASS: " + n1);
      n1.accept(this);
      NobaseSymbol suptyp = NobaseAst.getReference(n1);
      NobaseSymbol supersym = new NobaseSymbol(for_project,null,null,"super",true);
      supersym.setValue(nv);
      nscp.define(supersym);
      if (suptyp != null) {
         NobaseScope sscp = suptyp.getDefScope();
         if (sscp != null) nscp.setSuperScope(sscp);
       }
    }  
   
   cur_scope = nscp;
   
   name_stack.push(enclosing_function);
   if (enclosing_function == null) enclosing_function = clsname;
   else enclosing_function += "." + clsname;
   
   NobaseAst.setDefinition(n.getName(),osym);
   
   return true;
}


@Override public void endVisit(TypeDeclaration n) 
{
   cur_scope = cur_scope.getParent();
   enclosing_function = name_stack.pop();
   function_stack.pop();
}
@Override public void endVisit(Initializer n) { }



/********************************************************************************/
/*										*/
/*	Module methods								*/
/*										*/
/********************************************************************************/

@Override public void endVisit(ExportDeclaration n) { }
@Override public void endVisit(ImportDeclaration n) { }
@Override public void endVisit(ModuleSpecifier n) { }
@Override public void endVisit(PackageDeclaration n) { }


@Override public boolean visit(ImportDeclaration n) 
{
   for (Object o : n.specifiers()) {
      ModuleSpecifier ms = (ModuleSpecifier) o;
      SimpleName sn = ms.getLocal();
      defineName(sn,null,null,sn,null,VariableKind.VAR);
    }
   return false;
}




/********************************************************************************/
/*										*/
/*	Variable Declaration methods						*/
/*										*/
/********************************************************************************/

@Override public boolean visit(SingleVariableDeclaration fp)
{
   return handleDeclaration(fp);
}



@Override public boolean visit(VariableDeclarationFragment n)
{
   return handleDeclaration(n);
}



private boolean handleDeclaration(VariableDeclaration vd)
{
   if (vd.getInitializer() != null) {
      vd.getInitializer().accept(this);
    }

   SimpleName fident = vd.getName();
   Map<SimpleName,String> names = new LinkedHashMap<>(); 
   if (fident == null) {
      ASTNode apat = null;
      if (vd instanceof VariableDeclarationFragment) {
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) vd;
         apat = vdf.getPattern();
       }
      else if (vd instanceof SingleVariableDeclaration) {
         SingleVariableDeclaration svd = (SingleVariableDeclaration) vd;
         apat = svd.getPattern();
       }
      if (apat != null) {
         if (apat instanceof ObjectName) {
            ObjectName pat = (ObjectName) apat;
            for (Object o : pat.objectProperties()) {
               ObjectLiteralField olf = (ObjectLiteralField) o;
               Expression ex = olf.getFieldName();
               Expression ex1 = olf.getInitializer();
               if (ex instanceof SimpleName) {
                  SimpleName sn = (SimpleName) ex;
                  String key = sn.getIdentifier();
                  if (ex1 != null && ex1 instanceof SimpleName) {
                     String key1 = ((SimpleName) ex1).getIdentifier();
                     if (key1 != null && !key1.equals("MISSING")) key = key1;
                   }
                  names.put(sn,key);
                }
               else NobaseMain.logE("Unknown object literal value for " + pat);
             }
          }
         else if (apat instanceof RestElementName) {
            RestElementName ren = (RestElementName) apat;
            if (ren.getArgument() instanceof SimpleName) {
               fident = (SimpleName) ren.getArgument();
             }
            else NobaseMain.logE("Unknown rest element name " + ren.getArgument());
          }
         else if (apat instanceof ArrayName) {
            ArrayName an = (ArrayName) apat;
            for (Object o : an.elements()) {
               if (o instanceof SimpleName) {
                  SimpleName sn = (SimpleName) o;
                  names.put(sn,sn.getIdentifier());
                }
             }
          }
         else NobaseMain.logE("Unknown pattern value for " + apat.getClass().getName() + " " + apat);
       }
     else NobaseMain.logI("NO NAME " + vd.getBodyChild() + " @@ " + vd);
    }
   else {
      if (fident.getIdentifier().contains("MISSING")) {
         switch (vd.getParent().getNodeType()) {
            case ASTNode.CATCH_CLAUSE :
               return false;
          }
         System.err.println("CHECK HERE");
       }
      names.put(fident,fident.getIdentifier());
      NobaseMain.logD("Declaration for " + fident + " " + vd.getBodyChild());
    }
   
   VariableKind vk = null;
   ASTNode par = vd.getParent();
   NobaseType decltype = null;
   if (par instanceof VariableDeclarationExpression) {
      VariableDeclarationExpression vde = (VariableDeclarationExpression) par;
      vk = vde.getKind();
      if (vde.getType() != null) {
	 decltype = NobaseAst.getType(vde.getType());
       }
      par = par.getParent();
    }
   else if (par instanceof VariableDeclarationStatement) {
      VariableDeclarationStatement vds = (VariableDeclarationStatement) par;
      vk = vds.getKind();
      if (vds.getType() != null) {
	 decltype = NobaseAst.getType(vds.getType());
       }
      par = par.getParent();
    }

   if (par instanceof FunctionDeclaration) {
      // parameter
      if (fident != null) {
	 String newname = fident.getIdentifier();
	 NobaseSymbol osym = NobaseAst.getDefinition(vd);
	 NobaseScope scp = cur_scope.getParent();
	 NobaseSymbol psym = scp.lookup(newname);
	 if (psym != null) {
	    NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
		  "Parameter " + newname + " hides outside variable",
		  NobaseAst.getLineNumber(vd),NobaseAst.getColumn(vd),
		  NobaseAst.getEndLine(vd),NobaseAst.getEndColumn(vd));
	    error_list.add(msg);
	  }
	 if (osym == null) {
	    NobaseSymbol sym = new NobaseSymbol(for_project,enclosing_file,vd,newname,true);
	    setName(sym,newname,cur_scope);
	    osym = cur_scope.define(sym);
	    if (osym != sym) duplicateDef(fident.getIdentifier(),vd);
	    if (enclosing_function != null) {
	       newname = enclosing_function + "." + newname;
	     }
	    NobaseAst.setDefinition(vd,osym);
	    NobaseAst.setDefinition(fident,osym);
	  }
       }
      return false;
    }

   if (par instanceof CatchClause) {
      // catch variable
      NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,vd,fident.getIdentifier(),true);
      nsym.setSymbolType(SymbolType.LET);
      if (enclosing_function != null) {
	 setName(nsym,enclosing_function + "." + fident.getIdentifier(),cur_scope);
       }
      else {
	 setName(nsym,fident.getIdentifier(),cur_scope);
       }
      NobaseSymbol sym = cur_scope.define(nsym);
      NobaseAst.setDefinition(fident,sym);
      return false;
    }

   NobaseValue initv = null;
   if (vd.getInitializer() != null) {
      initv = NobaseAst.getNobaseValue(vd.getInitializer());
    }
   
   if (fident != null) {
      defineName(fident,decltype,initv,vd,null,vk);
    }
   else {
      for (Map.Entry<SimpleName,String> ent : names.entrySet()) {
         SimpleName sn = ent.getKey();
         String use = ent.getValue();
         defineName(sn,decltype,initv,vd,use,vk);
       }
    }
   
   return false;
}



private void defineName(SimpleName fident,NobaseType decltype,NobaseValue initv,ASTNode vd,String mult,VariableKind vk)
{
   NobaseSymbol sym = NobaseAst.getDefinition(fident);
   if (sym == null && fident != null) {
      NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,vd,fident.getIdentifier(),true);
      if (vk == VariableKind.LET) nsym.setSymbolType(SymbolType.LET);
      else if (vk == VariableKind.CONST) nsym.setSymbolType(SymbolType.CONST);
      else if (vk == VariableKind.VAR) nsym.setSymbolType(SymbolType.VAR);
      if (enclosing_function != null) {
	 setName(nsym,enclosing_function + "." + fident.getIdentifier(),cur_scope);
       }
      else {
	 setName(nsym,fident.getIdentifier(),cur_scope);
       }
      if (decltype != null) {
	 nsym.setDataType(decltype);
       }
      if (initv != null) {
         if (mult != null) {
            NobaseValue nv1 = initv.getProperty(mult,false);
            if (nv1 != null) nsym.setValue(nv1);
            else NobaseMain.logD("No value found for " + fident + " in " + initv);
          }
         else nsym.setValue(initv);
       }
      
      sym = cur_scope.define(nsym);
      if (nsym != sym) {
	 boolean dupok = false;
         if (initv != null && initv.isFunction() && initv == sym.getValue()) dupok = true;
	 if (!dupok) duplicateDef(fident.getIdentifier(),vd);
       }
      
      if (vd != null)  NobaseAst.setDefinition(vd,sym);
      NobaseAst.setDefinition(fident,sym);
    }
}



/********************************************************************************/
/*										*/
/*	Function Declaration methods						*/
/*										*/
/********************************************************************************/

@Override public boolean visit(FunctionDeclaration n)
{
   NobaseScope defscope = cur_scope;
   String fctname = getFunctionName(n);
   NobaseValue nv = NobaseAst.getNobaseValue(n);
   if (nv == null) {
      nv = NobaseValue.createFunction(n);
      setValue(n,nv);
    }
   function_stack.push(nv);
   
   if (fctname != null) {
      NobaseSymbol osym = NobaseAst.getDefinition(n);
      if (osym == null) {
	 ASTNode defnode = n;
         ASTNode pn = defnode.getParent();
         if (pn instanceof FunctionExpression) {
            ASTNode gpn = pn.getParent();
            if (gpn instanceof VariableDeclaration) defnode = gpn;
            else if (gpn instanceof Assignment) {
               Assignment assign = (Assignment) gpn;
               if (assign.getLeftHandSide().toString().startsWith("this.")) {
                  defnode = gpn;
                  NobaseSymbol thiss = cur_scope.lookup("this");
                  if (thiss != null) {
                     NobaseValue thisv = thiss.getValue();
                     if (thisv != null) defscope = thisv.getAssociatedScope();
                   }
                }
             }
          }
	 NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,defnode,fctname,true);
	 setName(nsym,fctname,defscope);
	 nsym.setValue(nv);
	 osym = defscope.define(nsym);
         if (osym != nsym) {
	    duplicateDef(fctname,n);
	    nsym = osym;
	  }
	 NobaseAst.setDefinition(n,osym);
       }
    }
	
   NobaseScope nscp = NobaseAst.getScope(n);
   if (nscp == null) {
      NobaseValue othis = null;
      if (enclosing_function != null) {
	 NobaseSymbol othissym = cur_scope.lookup("this");
	 if (othissym != null) othis = othissym.getValue();
       }
      nscp = new NobaseScope(ScopeType.FUNCTION,cur_scope);
      NobaseAst.setScope(n,nscp);
      nscp.setValue(nv);
      if (othis != null) {
	 nv.mergeProperties(othis);
       }
      NobaseSymbol thissym = new NobaseSymbol(for_project,null,null,"this",true);
      thissym.setValue(nv);
      nscp.define(thissym);
    }
   cur_scope = nscp;

   name_stack.push(enclosing_function);
   ASTNode defnd = getFunctionNode(n);
   if (fctname != null) {
      NobaseSymbol nsym = NobaseAst.getDefinition(defnd);
      if (nsym == null) nsym = NobaseAst.getDefinition(n);
      if (nsym == null) {
	 nsym = new NobaseSymbol(for_project,enclosing_file,defnd,fctname,true);
	 nsym.setValue(nv);
	 setName(nsym,fctname,defscope);
       }
      if (!fctname.equals(nsym.getName()))
	 cur_scope.defineAll(nsym,fctname);
      NobaseAst.setDefinition(defnd,nsym);
      if (enclosing_function == null) enclosing_function = fctname;
      else if (fctname.contains(".")) enclosing_function = fctname;
      else enclosing_function += "." + fctname;
      Expression id = n.getMethodName();
      if (id != null && NobaseAst.getDefinition(id) == null) {
	 NobaseAst.setDefinition(id,nsym);
       }
    }

   return true;
}

@Override public void endVisit(FunctionDeclaration n) {
   cur_scope = cur_scope.getParent();
   enclosing_function = name_stack.pop();
   function_stack.pop();
}


private String getFunctionName(FunctionDeclaration fc)
{
   if (fc.getMethodName() != null && fc.getMethodName() instanceof SimpleName) {
      SimpleName fnm = (SimpleName) fc.getMethodName();
      if (!NobaseResolver.isGeneratedName(fnm.getIdentifier()))
	 return fnm.getIdentifier();
    }
   if (fc.getParent() instanceof VariableDeclaration) {
      VariableDeclaration d = (VariableDeclaration) fc.getParent();
      return d.getName().getIdentifier();
    }
   if (fc.getParent() instanceof FunctionExpression &&
	 fc.getParent().getParent() instanceof Assignment) {
      Assignment ao = (Assignment) fc.getParent().getParent();
      if (ao.getRightHandSide() != fc.getParent()) return null;
      if (ao.getLeftHandSide() instanceof FieldAccess) {
	 FieldAccess ma = (FieldAccess) ao.getLeftHandSide();
	 String m1 = getStringIdentName(ma.getName());
	 if (m1 == null) return null;
	 String m0 = getStringIdentName(ma.getExpression());
	 if (m0 != null && m0.equals("this") && enclosing_function != null) {
	    return m1;
	  }
	 else if (m0 == null && ma.getExpression() instanceof FieldAccess) {
	    FieldAccess ma1 = (FieldAccess) ma.getExpression();
	    String k1 = getStringIdentName(ma1.getExpression());
	    String k2 = getStringIdentName(ma1.getName());
	    if (k2 != null && k2.equals("prototype") && k1 != null) {
	       return k1 + "." + m1;
	     }
	  }
	 else if (m0 != null && m0.equals("exports") && enclosing_function == null) {
	    return m1;
	  }
       }
    }
   return getStringIdentName(fc.getMethodName());
}


private ASTNode getFunctionNode(FunctionDeclaration fc)
{
   if (fc.getParent() instanceof FunctionDeclaration) return fc.getParent();
   if (fc.getMethodName() != null && fc.getMethodName() instanceof SimpleName) {
      SimpleName sn = (SimpleName) fc.getMethodName();
      if (!NobaseResolver.isGeneratedName(sn.getIdentifier())) return fc;
    }
   if (fc.getParent() instanceof VariableDeclaration) return fc.getParent();
   if (fc.getParent() instanceof FunctionExpression &&
	 fc.getParent().getParent() instanceof Assignment)
      return fc.getParent().getParent();
   if (fc.getMethodName() != null)
      return fc;

   return null;
}



/********************************************************************************/
/*										*/
/*	Ignored constructors							*/
/*										*/
/********************************************************************************/

// comment types
@Override public void endVisit(BlockComment n) { }
@Override public void endVisit(LineComment n) { }

// javadoc types
@Override public void endVisit(FunctionRef n) { }
@Override public void endVisit(JSdoc n) { }
@Override public void endVisit(MemberRef n) { }
@Override public void endVisit(TagElement n) { }
@Override public void endVisit(TextElement n) { }


/********************************************************************************/
/*										*/
/*	Utility functions for setting properties				*/
/*										*/
/********************************************************************************/

private void setValue(ASTNode n,NobaseValue v)
{
   if (v == null) v = NobaseValue.createUnknownValue();
   change_flag |= NobaseAst.setNobaseValue(n,v);
}



/********************************************************************************/
/*										*/
/*	Name utility methods							*/
/*										*/
/********************************************************************************/

private Object getIdentName(Expression e,Expression rhs)
{
   if (e == null) {
      if (rhs != null && rhs instanceof FunctionExpression) {
         FunctionExpression fde = (FunctionExpression) rhs;
         return getFunctionName(fde.getMethod());
       }
    }
   if (e instanceof SimpleName) {
      return ((SimpleName) e).getIdentifier();
    }
   else if (e instanceof ThisExpression) {
      return "this";
    }
   else if (e != null) {
      e.accept(this);
      NobaseValue nv = NobaseAst.getNobaseValue(e);
      if (nv != null) {
         Object ov = nv.getKnownValue();
         if (ov != null) return ov;
       }
    }
   return null;
}


private String getStringIdentName(Expression e)
{
   Object r = getIdentName(e,null);
   if (r == null || !(r instanceof String)) return null;
   return (String) r;
}



private void setName(NobaseSymbol sym,String name,NobaseScope dscope)
{
   String tnm = enclosing_file.getModuleName();
   String qnm = tnm + "." + name;
   if (enclosing_function != null) {
      qnm = tnm + "." + enclosing_function + "." + name;
    }
   if (sym != null) sym.setBubblesName(qnm);
   if (dscope == null) dscope = cur_scope;
   if (sym != null) sym.setDefScope(dscope);
}


private void duplicateDef(String nm,ASTNode n)
{
   NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
	 "Duplicate defintion of " + nm,
	 NobaseAst.getLineNumber(n),NobaseAst.getColumn(n),NobaseAst.getEndLine(n),NobaseAst.getEndColumn(n));
   error_list.add(msg);
}



/********************************************************************************/
/*										*/
/*	Scope utility methods							*/
/*										*/
/********************************************************************************/

private void localScopeBegin(ASTNode n)
{
   NobaseScope nscp = NobaseAst.getScope(n);
   if (nscp == null) {
      nscp = new NobaseScope(ScopeType.LOCAL,cur_scope);
      NobaseAst.setScope(n,nscp);
    }
   cur_scope = nscp;
}



private void localScopeEnd()
{
   cur_scope = cur_scope.getParent();
}



/********************************************************************************/
/*										*/
/*	Operator implementations						*/
/*										*/
/********************************************************************************/

private static class EvalPlus implements Evaluator {

   @Override public NobaseValue evaluate(NobaseFile forfile,List<NobaseValue> args,
         NobaseValue thisval) {
      if (args.size() == 0) return null;
      if (args.size() == 1) return args.get(0);
   
      List<Object> vals = new ArrayList<Object>();
      boolean havestring = false;
      boolean allnumber = true;
      for (NobaseValue nv : args) {
         if (nv == null) return null;
         Object o = nv.getKnownValue();
         if (o == null) return null;
         if (o == KnownValue.UNKNOWN) return null;
         if (o == KnownValue.ANY) return null;
         if (o == KnownValue.UNDEFINED) return null;
         if (o == KnownValue.NULL) o = null;
         vals.add(o);
         if (!(o instanceof Number)) allnumber = false;
         if (o instanceof String) havestring = true;
       }
      if (havestring) {
         StringBuffer buf = new StringBuffer();
         for (Object o : vals) {
            buf.append(o);
          }
         return NobaseValue.createString(buf.toString());
       }
      if (allnumber) {
         double v = 0;
         for (Object o : vals) {
            v += ((Number) o).doubleValue();
          }
         return NobaseValue.createNumber(v);
       }
      return null;
    }

}	// end of inner class EvalPlus



private static class EvalNumeric implements Evaluator {

   private String eval_op;
   
   EvalNumeric(String op) {
      eval_op = op;
    }
   
   @Override public NobaseValue evaluate(NobaseFile forfile,List<NobaseValue> args,
         NobaseValue thisval) {
      if (args.size() == 0) return null;
      
      List<Number> vals = new ArrayList<>();
      for (NobaseValue nv : args) {
         if (nv == null) return null;
         Object o = nv.getKnownValue();
         if (o instanceof Number) vals.add(((Number)o));
         else return null;
       }
      
      double v = vals.get(0).doubleValue();
      if (args.size() == 1) {
         switch (eval_op) {
            case "-" :
               v = -v;
               break;
            default :
               return null;
          }
       }
      else {
         for (int i = 1; i < vals.size(); ++i) {
            double v1 = vals.get(i).doubleValue();
            switch (eval_op) {
               case "-" :
                  v -= v1;
                  break;
               case "*" :
                  v *= v1;
                  break;
               case "/" :
                  v /= v1;
                  break;
               default :
                  return null;
             }
            
          }
       }
      return NobaseValue.createNumber(v);
    }
   
}	// end of inner class EvalNumeric




}	// end of class NobaseValuePass




/* end of NobaseValuePass.java */

