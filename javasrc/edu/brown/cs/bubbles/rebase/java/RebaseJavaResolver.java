/********************************************************************************/
/*										*/
/*		RebaseJavaResolver.java 					*/
/*										*/
/*	Class to handle name resolution for Java files				*/
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



import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
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
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;


class RebaseJavaResolver implements RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseJavaTyper       type_data;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseJavaResolver(RebaseJavaTyper typer)
{
   type_data = typer;
}



/********************************************************************************/
/*										*/
/*	Top level methods							*/
/*										*/
/********************************************************************************/

void resolveNames(ASTNode n)
{
   DefPass dp = new DefPass();

   n.accept(dp);

   RefPass rp = new RefPass();

   n.accept(rp);
}




void resolveNames(RebaseJavaRoot root)
{
   for (CompilationUnit cu : root.getTrees()) {
      DefPass dp = new DefPass();
      cu.accept(dp);
    }
   
   for (CompilationUnit cu : root.getTrees()) {
      RefPass rp = new RefPass();
      cu.accept(rp);
    }
}




/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private RebaseJavaType findType(String nm)
{
   return type_data.findType(nm);
}



private RebaseJavaType findArrayType(RebaseJavaType jt)
{
   return type_data.findArrayType(jt);
}



private RebaseJavaType findNumberType(String n)
{
   boolean ishex = false;
   boolean isreal = false;
   String type = null;

   for (int i = 0; i < n.length(); ++i) {
      switch (n.charAt(i)) {
	 case '.' :
	 case 'E' :
	 case 'e' :
	    if (!ishex) isreal = true;
	    break;
	 case 'l' :
	 case 'L' :
	    type = "long";
	    break;
	 case 'f' :
	 case 'F' :
	    if (!ishex) type = "float";
	    break;
	 case 'd' :
	 case 'D' :
	    if (!ishex) type = "double";
	    break;
	 case 'X' :
	 case 'x' :
	    ishex = true;
	    break;
       }
    }

   if (type == null) {
      if (isreal) type = "double";
      else type = "int";
      // MIGHT NEED SPECIAL TYPES FOR SHORTINT, BYTEINT
    }

   return type_data.findType(type);
}




/********************************************************************************/
/*										*/
/*	DefPass -- definition pass						*/
/*										*/
/********************************************************************************/

private class DefPass extends ASTVisitor {

   private RebaseJavaScope cur_scope;

   DefPass() {
      cur_scope = new RebaseJavaScopeAst(null);
    }

   public @Override boolean visit(AnnotationTypeMemberDeclaration n)	{ return false; }
   public @Override boolean visit(AnnotationTypeDeclaration n)		{ return false; }
   public @Override boolean visit(PackageDeclaration n) 		{ return false; }
   public @Override boolean visit(ImportDeclaration n)			{ return false; }

   public @Override void preVisit(ASTNode n) {
      switch (n.getNodeType()) {
         case ASTNode.TYPE_DECLARATION :
         case ASTNode.ENUM_DECLARATION :
         case ASTNode.ANONYMOUS_CLASS_DECLARATION :
         case ASTNode.INITIALIZER :
         case ASTNode.BLOCK :
         case ASTNode.CATCH_CLAUSE :
         case ASTNode.FOR_STATEMENT :
         case ASTNode.ENHANCED_FOR_STATEMENT :
         case ASTNode.SWITCH_STATEMENT :
            cur_scope = new RebaseJavaScopeAst(cur_scope);
            RebaseJavaType jt = RebaseJavaAst.getJavaType(n);
            if (jt != null) jt.setScope(cur_scope);
            RebaseJavaAst.setJavaScope(n,cur_scope);
            break;
       }
    }

   public @Override void postVisit(ASTNode n) {
      RebaseJavaScope s = RebaseJavaAst.getJavaScope(n);
      if (s != null) cur_scope = s.getParent();
    }

   public @Override boolean visit(MethodDeclaration n) {
      String nm;
      if (n.isConstructor()) nm = "<init>";
      else nm = n.getName().getIdentifier();
      RebaseJavaSymbol jm = cur_scope.defineMethod(nm,n);
      cur_scope = new RebaseJavaScopeAst(cur_scope);
      RebaseJavaAst.setJavaScope(n,cur_scope);
      RebaseJavaAst.setDefinition(n,jm);
      RebaseJavaAst.setDefinition(n.getName(),jm);
      return true;
    }

   public @Override void endVisit(MethodDeclaration n) {
      cur_scope = cur_scope.getParent();
    }

   public @Override boolean visit(SingleVariableDeclaration n) {
      RebaseJavaSymbol js = RebaseJavaSymbol.createSymbol(n,type_data);
      cur_scope.defineVar(js);
      RebaseJavaAst.setDefinition(n,js);
      RebaseJavaAst.setDefinition(n.getName(),js);
      return true;
    }

   public @Override boolean visit(VariableDeclarationFragment n) {
      RebaseJavaSymbol js = RebaseJavaSymbol.createSymbol(n,type_data);
      cur_scope.defineVar(js);
      RebaseJavaAst.setDefinition(n,js);
      RebaseJavaAst.setDefinition(n.getName(),js);
      return true;
    }

   public @Override boolean visit(EnumConstantDeclaration n) {
      RebaseJavaSymbol js = RebaseJavaSymbol.createSymbol(n);
      cur_scope.defineVar(js);
      RebaseJavaAst.setDefinition(n,js);
      return true;
    }

   public @Override boolean visit(LabeledStatement n) {
      RebaseJavaSymbol js = RebaseJavaSymbol.createSymbol(n);
      cur_scope.defineVar(js);
      RebaseJavaAst.setDefinition(n,js);
      return true;
    }

   public @Override void endVisit(TypeDeclaration n) {
      RebaseJavaType jt = RebaseJavaAst.getJavaType(n);
      RebaseJavaAst.setJavaType(n.getName(),jt);
      RebaseJavaSymbol js = jt.getDefinition();
      if (js == null) js = RebaseJavaSymbol.createSymbol(n);
      cur_scope.getParent().defineVar(js);
      RebaseJavaAst.setDefinition(n,js);
      RebaseJavaAst.setDefinition(n.getName(),js);
    }

   public @Override void endVisit(EnumDeclaration n) {
      RebaseJavaType jt = RebaseJavaAst.getJavaType(n);
      RebaseJavaAst.setJavaType(n.getName(),jt);
      RebaseJavaSymbol js = jt.getDefinition();
      cur_scope.getParent().defineVar(js);
      RebaseJavaAst.setDefinition(n,js);
      RebaseJavaAst.setDefinition(n.getName(),js);
    }

   public @Override void endVisit(Initializer n) {
      // TODO: create static initializer name
    }

}	// end of subclass DefPass





/********************************************************************************/
/*										*/
/*	RefPass -- handle references						*/
/*										*/
/********************************************************************************/

private class RefPass extends ASTVisitor {

   private RebaseJavaScope cur_scope;
   private RebaseJavaType cur_type;
   private Stack<RebaseJavaType> outer_types;

   RefPass() {
      cur_scope = null;
      cur_type = null;
      outer_types = new Stack<>();
    }

   public @Override boolean visit(AnnotationTypeMemberDeclaration n)	{ return false; }
   public @Override boolean visit(AnnotationTypeDeclaration n)		{ return false; }
   public @Override boolean visit(PackageDeclaration n) 		{ return false; }
   public @Override boolean visit(ImportDeclaration n)			{ return false; }
   public @Override boolean visit(ArrayType n)				{ return true; }
   public @Override boolean visit(ParameterizedType n)			{ return false; }
   public @Override boolean visit(PrimitiveType n)			{ return false; }
   public @Override boolean visit(QualifiedType n)			{ return true; }
   public @Override boolean visit(SimpleType n) 			{ return true; }
   public @Override boolean visit(WildcardType n)			{ return false; }
   public @Override boolean visit(MarkerAnnotation n)                   { return false; }
   public @Override boolean visit(NormalAnnotation n)                   { return false; }
   public @Override boolean visit(SingleMemberAnnotation n)             { return false; }

   public @Override void preVisit(ASTNode n) {
      RebaseJavaScope s = RebaseJavaAst.getJavaScope(n);
      if (s != null) cur_scope = s;
    }

   public @Override void postVisit(ASTNode n) {
      RebaseJavaScope s = RebaseJavaAst.getJavaScope(n);
      if (s != null) cur_scope = s.getParent();
    }

   public @Override void endVisit(BooleanLiteral n) {
      RebaseJavaAst.setExprType(n,findType("boolean"));
    }

   public @Override void endVisit(CharacterLiteral n) {
      RebaseJavaAst.setExprType(n,findType("char"));
    }

   public @Override void endVisit(NullLiteral n) {
      RebaseJavaAst.setExprType(n,findType(TYPE_ANY_CLASS));
    }

   public @Override void endVisit(NumberLiteral n) {
      RebaseJavaType t = findNumberType(n.getToken());
      RebaseJavaAst.setExprType(n,t);
    }

   public @Override void endVisit(StringLiteral n) {
      RebaseJavaAst.setExprType(n,findType("java.lang.String"));
    }

   public @Override void endVisit(TypeLiteral n) {
      RebaseJavaAst.setExprType(n,findType("java.lang.Class"));
    }

   public @Override boolean visit(FieldAccess n) {
      n.getExpression().accept(this);
      RebaseJavaType t = RebaseJavaAst.getExprType(n.getExpression());
      if (t == null) {
         t = RebaseJavaType.createErrorType();
         RebaseJavaAst.setExprType(n.getExpression(),t);
       }
      RebaseJavaSymbol js = null;
      if (t != null) js = t.lookupField(type_data,n.getName().getIdentifier());
      if (js == null && t != null && (t.isArrayType() || t.isErrorType()) &&
             n.getName().getIdentifier().equals("length")) {
         RebaseJavaAst.setExprType(n,findType("int"));
         return false;
       }
      if (js == null) {
         RebaseJavaAst.setExprType(n,findType(TYPE_ERROR));
       }
      else {
         RebaseJavaAst.setReference(n.getName(),js);
         RebaseJavaType jt = js.getType();
         RebaseJavaAst.setExprType(n,jt);
       }
      return false;
    }

   public @Override boolean visit(SuperFieldAccess n) {
      Name qn = n.getQualifier();
      if (qn != null) qn.accept(this);
      RebaseJavaSymbol js = null;
      if (cur_type != null) {
         RebaseJavaType jt = cur_type.getSuperType();
         if (jt != null) js = jt.lookupField(type_data,n.getName().getIdentifier());
       }
      if (js == null) {
         RebaseJavaAst.setExprType(n,findType(TYPE_ERROR));
       }
      else {
         RebaseJavaAst.setReference(n.getName(),js);
         RebaseJavaType t = js.getType();
         RebaseJavaAst.setExprType(n,t);
       }
      return false;
    }

   public @Override boolean visit(QualifiedName n) {
      if (n.getFullyQualifiedName().endsWith(".abs"))
         System.err.println("CHEKC ABS");
      RebaseJavaType nt = RebaseJavaAst.getJavaType(n);
      if (nt != null) {
         RebaseJavaAst.setExprType(n,nt);
         return false;
       }
      Name qn = n.getQualifier();
      RebaseJavaType qt = RebaseJavaAst.getJavaType(qn);
      if (qt == null) {
         qn.accept(this);
         qt = RebaseJavaAst.getExprType(qn);
       }
      RebaseJavaSymbol js = null;
      if (qt != null) js = qt.lookupField(type_data,n.getName().getIdentifier());
      if (js == null && qt != null && (qt.isArrayType() || qt.isErrorType()) &&
             n.getName().getIdentifier().equals("length")) {
         RebaseJavaAst.setExprType(n,findType("int"));
         return false;
       }
      else if (js == null) {
         RebaseJavaAst.setExprType(n,findType(TYPE_ERROR));
       }
      else {
         RebaseJavaAst.setReference(n.getName(),js);
         RebaseJavaType t = js.getType();
         RebaseJavaAst.setExprType(n,t);
       }
      return false;
    }

   public @Override void endVisit(SimpleName n) {
      RebaseJavaSymbol js = RebaseJavaAst.getReference(n);
      if (js != null) {
         RebaseJavaAst.setExprType(n,js.getType());
         return;
       }
      js = RebaseJavaAst.getDefinition(n);
      if (js != null) {
         RebaseJavaAst.setExprType(n,js.getType());
         RebaseJavaAst.setReference(n,js);
         return;
       }
      RebaseJavaType jt = RebaseJavaAst.getJavaType(n);
      if (jt != null) {
         RebaseJavaAst.setExprType(n,jt);
       }
   
      if (cur_scope != null) {
         String name = n.getIdentifier();
         RebaseJavaSymbol d = cur_scope.lookupVariable(name);
         if (d == null && cur_type != null) {
            d = cur_type.lookupField(type_data,name);
          }
         if (d == null) {
            if (jt == null) {
               RebaseJavaAst.setExprType(n,findType(TYPE_ERROR));
             }
            else {
               d = jt.getDefinition();
               if (d != null) 
        	  RebaseJavaAst.setReference(n,d);
             }
          }
         else {
            RebaseJavaAst.setReference(n,d);
            RebaseJavaType t = d.getType();
            RebaseJavaAst.setExprType(n,t);
          }
       }
    }


   public @Override void endVisit(SimpleType t) {
      Name n = t.getName();
      RebaseJavaType jt = RebaseJavaAst.getExprType(n);
      if (jt == null)
         jt = findType(TYPE_ERROR);
   
      RebaseJavaAst.setExprType(t,jt);
   }

   public @Override boolean visit(MethodInvocation n) {
      RebaseJavaType bt = cur_type;
      Expression e = n.getExpression();
      if (e != null) {
	 e.accept(this);
	 bt = RebaseJavaAst.getJavaType(e);
	 if (bt == null) bt = RebaseJavaAst.getExprType(e);
       }

      List<RebaseJavaType> atyp = buildArgumentList(n.arguments());

      lookupMethod(bt,atyp,n,n.getName(),null);

      return false;
    }

   public @Override boolean visit(SuperMethodInvocation n) {
      RebaseJavaType bt = null;
      if (cur_type != null) bt = cur_type.getSuperType();
      Name nn = n.getQualifier();
      if (nn != null) {
	 nn.accept(this);
	 bt = RebaseJavaAst.getJavaType(nn);
	 if (bt == null) bt = RebaseJavaAst.getExprType(nn);
	 if (bt != null) bt = bt.getSuperType();
       }

      List<RebaseJavaType> atyp = buildArgumentList(n.arguments());

      lookupMethod(bt,atyp,n,n.getName(),null);

      return false;
    }

   public @Override void endVisit(ClassInstanceCreation n) {
      RebaseJavaType xt = null;
      Expression e = n.getExpression();
      if (e != null) {
	 xt = RebaseJavaAst.getJavaType(e);
	 if (xt == null) xt = RebaseJavaAst.getExprType(e);
       }

      RebaseJavaType bt = RebaseJavaAst.getJavaType(n.getType());
      if (bt == null) {
	 bt = RebaseJavaType.createErrorType();
	 RebaseJavaAst.setJavaType(n.getType(),bt);
       }
      List<RebaseJavaType> atys = buildArgumentList(n.arguments());
      RebaseJavaAst.setExprType(n,bt);		      // set default type
      lookupMethod(bt,atys,n,null,"<init>");    // this can reset the type
      RebaseJavaType rt = RebaseJavaAst.getExprType(n);
      if (rt.isErrorType() && atys.size() == 0) {
	 boolean havecnst = false;
	 if (bt.getScope() != null) {
	    for (RebaseJavaSymbol xjs : bt.getScope().getDefinedMethods()) {
	       if (xjs.getName().equals("<init>")) havecnst = true;
	     }
	  }
	 if (!havecnst) {
	    RebaseJavaAst.setExprType(n,bt);	 // handle default constructor
	  }
       }
   }

   public @Override void endVisit(ConstructorInvocation n) {
      List<RebaseJavaType> atys = buildArgumentList(n.arguments());
      RebaseJavaAst.setExprType(n,cur_type);	 // set type, will be reset on error
      lookupMethod(cur_type,atys,n,null,"<init>");
    }

   public @Override void endVisit(SuperConstructorInvocation n) {
      List<RebaseJavaType> atys = buildArgumentList(n.arguments());
      RebaseJavaType  bt = null;
      if (cur_type != null) bt = cur_type.getSuperType();
      lookupMethod(bt,atys,n,null,"<init>");
      if (RebaseJavaAst.getReference(n) != null)
	 RebaseJavaAst.setExprType(n,cur_type);
      else
	 RebaseJavaAst.setExprType(n,findType(TYPE_ERROR));
    }

   public @Override void endVisit(ArrayAccess n) {
      RebaseJavaType t = RebaseJavaAst.getExprType(n.getArray());
      if (t != null && !t.isErrorType()) t = t.getBaseType();
      if (t == null) t = findType(TYPE_ERROR);
      RebaseJavaAst.setExprType(n,t);
    }

   public @Override void endVisit(ArrayCreation n) {
      RebaseJavaType bt = RebaseJavaAst.getJavaType(n.getType());
      RebaseJavaAst.setExprType(n,bt);
    }

   public @Override void endVisit(ArrayInitializer n) {
      RebaseJavaType bt = findType(TYPE_ANY_CLASS);
      for (Iterator<?> it = n.expressions().iterator(); it.hasNext(); ) {
	 Expression e = (Expression) it.next();
	 RebaseJavaType xbt = RebaseJavaAst.getExprType(e);
	 if (xbt != null) {
	    bt = xbt;
	    break;
	  }
       }
      bt = findArrayType(bt);
      RebaseJavaAst.setExprType(n,bt);
    }

   public @Override void endVisit(Assignment n) {
      RebaseJavaType b = RebaseJavaAst.getExprType(n.getRightHandSide());
      RebaseJavaAst.setExprType(n,b);
    }

   public @Override void endVisit(CastExpression n) {
      RebaseJavaType jt = RebaseJavaAst.getJavaType(n.getType());
      if (jt != null && jt.isUnknown()) jt = null;
      if (jt == null) jt = RebaseJavaAst.getExprType(n.getType());
      if (jt == null) jt = findType(TYPE_ERROR);

      RebaseJavaAst.setExprType(n,RebaseJavaAst.getJavaType(n.getType()));
    }

   public @Override void endVisit(ConditionalExpression n) {
      RebaseJavaType t1 = RebaseJavaAst.getExprType(n.getThenExpression());
      RebaseJavaType t2 = RebaseJavaAst.getExprType(n.getElseExpression());
      t1 = RebaseJavaType.mergeTypes(type_data,t1,t2);
      RebaseJavaAst.setExprType(n,t1);
    }

   public @Override void endVisit(InfixExpression n) {
      RebaseJavaType t1 = RebaseJavaAst.getExprType(n.getLeftOperand());
      RebaseJavaType t2 = RebaseJavaAst.getExprType(n.getRightOperand());
      if (t1 == null) System.err.println("NULL TYPE FOR LEFT " + n);
      if (t2 == null) System.err.println("NULL TYPE FOR RIGHT " + n);
      if (n.getOperator() == InfixExpression.Operator.CONDITIONAL_AND ||
	     n.getOperator() == InfixExpression.Operator.CONDITIONAL_OR ||
	     n.getOperator() == InfixExpression.Operator.EQUALS ||
	     n.getOperator() == InfixExpression.Operator.GREATER ||
	     n.getOperator() == InfixExpression.Operator.GREATER_EQUALS ||
	     n.getOperator() == InfixExpression.Operator.LESS ||
	     n.getOperator() == InfixExpression.Operator.LESS_EQUALS ||
	     n.getOperator() == InfixExpression.Operator.NOT_EQUALS ||
	     n.getOperator() == InfixExpression.Operator.EQUALS)
	 t1 = findType("boolean");
      else if (n.getOperator() == InfixExpression.Operator.PLUS &&
		  t1 != null && t2 != null &&
		  (!t1.isNumericType() || !t2.isNumericType())) {
	 if (t1.isErrorType()) t1 = t2;
	 else if (t2.isErrorType()) ;
	 else t1 = findType("java.lang.String");
       }
      else {
	 t1 = RebaseJavaType.mergeTypes(type_data,t1,t2);
	 if (n.hasExtendedOperands()) {
	    for (Iterator<?> it = n.extendedOperands().iterator(); it.hasNext(); ) {
	       Expression e = (Expression) it.next();
	       t2 = RebaseJavaAst.getExprType(e);
	       if (t2 == null) ;
	       else if (t2.isNumericType()) t1 = RebaseJavaType.mergeTypes(type_data,t1,t2);
	       else if (t2.isErrorType()) ;
	       else if (n.getOperator() == InfixExpression.Operator.PLUS) {
		  t1 = findType("java.lang.String");
		  break;
		}
	     }
	  }
       }
      RebaseJavaAst.setExprType(n,t1);
    }

   public @Override void endVisit(InstanceofExpression n) {
      RebaseJavaAst.setExprType(n,findType("boolean"));
    }

   public @Override void endVisit(ParenthesizedExpression n) {
      RebaseJavaAst.setExprType(n,RebaseJavaAst.getExprType(n.getExpression()));
    }

   public @Override void endVisit(PostfixExpression n) {
      RebaseJavaAst.setExprType(n,RebaseJavaAst.getExprType(n.getOperand()));
    }

   public @Override void endVisit(PrefixExpression n) {
      RebaseJavaAst.setExprType(n,RebaseJavaAst.getExprType(n.getOperand()));
    }

   public @Override void endVisit(ThisExpression n) {
      Name nm = n.getQualifier();
      RebaseJavaType jt = cur_type;
      if (nm != null) {
	 jt = RebaseJavaAst.getJavaType(nm);
	 if (jt == null) jt = RebaseJavaAst.getExprType(nm);
	 if (jt == null) jt = findType(TYPE_ERROR);
       }
      RebaseJavaAst.setExprType(n,jt);
    }

   public @Override void endVisit(VariableDeclarationExpression n) {
      RebaseJavaType jt = RebaseJavaAst.getJavaType(n.getType());
      if (jt == null) RebaseJavaAst.setExprType(n,findType(TYPE_ERROR));
      else RebaseJavaAst.setExprType(n,jt);
    }

   public @Override boolean visit(TypeDeclaration n) {
      outer_types.push(cur_type);
      cur_type = RebaseJavaAst.getJavaType(n);
      return true;
    }

   public @Override void endVisit(TypeDeclaration n) {
      cur_type = outer_types.pop();
    }

   public @Override boolean visit(EnumDeclaration n) {
      outer_types.push(cur_type);
      cur_type = RebaseJavaAst.getJavaType(n);
      return true;
    }

   public @Override void endVisit(EnumDeclaration n) {
      cur_type = outer_types.pop();
    }

   public @Override boolean visit(AnonymousClassDeclaration n) {
      outer_types.push(cur_type);
      cur_type = RebaseJavaAst.getJavaType(n);
      return true;
    }

   public @Override void endVisit(AnonymousClassDeclaration n) {
      cur_type = outer_types.pop();
    }

   private void lookupMethod(RebaseJavaType bt,List<RebaseJavaType> atyp,ASTNode n,SimpleName nm,String id) {
      RebaseJavaType mtyp = null;
      try {
         mtyp = RebaseJavaType.createMethodType(null,atyp,false);
       }
      catch (Throwable t) {
         System.err.println("PROBLEM CREATING METHOD TYPE: " + t);
         System.err.println("CASE: " + bt + " " + nm + " " + n);
         t.printStackTrace();
         mtyp = findType(TYPE_ERROR);
       }
   
      if (id == null && nm != null) id = nm.getIdentifier();
   
      RebaseJavaSymbol js = null;
      if (bt != null) js = callLookupMethod(bt,id,mtyp);
      if (js != null) {
         if (nm != null) RebaseJavaAst.setReference(nm,js);
         RebaseJavaAst.setReference(n,js);
         RebaseJavaType rt = js.getType().getBaseType();
         if (rt == null) rt = findType(TYPE_ERROR);
         if (rt.isParameterizedType()) {
            boolean usesvar = false;
            for (RebaseJavaType prt : rt.getComponents()) {
               if (prt.isTypeVariable()) usesvar = true;
             }
            if (usesvar) {
               rt = rt.getBaseType();
             }
          }
         if (n instanceof ClassInstanceCreation) {
            ClassInstanceCreation cic = (ClassInstanceCreation) n;
            RebaseJavaAst.setExprType(n,RebaseJavaAst.getJavaType(cic.getType()));
          }
         else RebaseJavaAst.setExprType(n,rt);
       }
      else {
         RebaseJavaAst.setExprType(n,findType(TYPE_ERROR));
       }
    }

   private RebaseJavaSymbol callLookupMethod(RebaseJavaType bt,String id,RebaseJavaType mtyp) {
      try {
         return bt.lookupMethod(type_data,id,mtyp);
       }
      catch (Throwable t) {
         System.err.println("S6: PROBLEM LOOKING UP " + bt + " " + id + " " + mtyp);
         return null;
       }
    }

   private List<RebaseJavaType> buildArgumentList(List<?> args) {
      List<RebaseJavaType> atyp = new ArrayList<RebaseJavaType>();
      for (Iterator<?> it = args.iterator(); it.hasNext(); ) {
	 Expression e = (Expression) it.next();
	 e.accept(this);
	 RebaseJavaType ejt = RebaseJavaAst.getExprType(e);
	 if (ejt == null) {
	    System.err.println("NO EXPR TYPE FOR: " + e.getClass().getName() + " " + e.getNodeType() + " " + e);
	    ejt = findType(TYPE_ERROR);
	  }
	 atyp.add(ejt);
       }
      return atyp;
    }


}	// end of subclass RefPass



}	// end of class RebaseJavaResolver




/* end of RebaseJavaResolver.java */
