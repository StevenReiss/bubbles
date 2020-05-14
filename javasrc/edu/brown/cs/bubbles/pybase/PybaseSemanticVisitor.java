/********************************************************************************/
/*										*/
/*		PybaseSemanticVisitor.java					*/
/*										*/
/*	Python Bubbles Base semantic analysis visitor				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 19/07/2005
 */


package edu.brown.cs.bubbles.pybase;


import edu.brown.cs.bubbles.pybase.symbols.AbstractModule;
import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.AbstractVisitor;
import edu.brown.cs.bubbles.pybase.symbols.AssignDefinition;
import edu.brown.cs.bubbles.pybase.symbols.CompletionCache;
import edu.brown.cs.bubbles.pybase.symbols.CompletionState;
import edu.brown.cs.bubbles.pybase.symbols.CompletionStateFactory;
import edu.brown.cs.bubbles.pybase.symbols.Definition;
import edu.brown.cs.bubbles.pybase.symbols.DuplicationChecker;
import edu.brown.cs.bubbles.pybase.symbols.Found;
import edu.brown.cs.bubbles.pybase.symbols.GenAndTok;
import edu.brown.cs.bubbles.pybase.symbols.LocalScope;
import edu.brown.cs.bubbles.pybase.symbols.MessagesManager;
import edu.brown.cs.bubbles.pybase.symbols.NoSelfChecker;
import edu.brown.cs.bubbles.pybase.symbols.NodeUtils;
import edu.brown.cs.bubbles.pybase.symbols.Scope;
import edu.brown.cs.bubbles.pybase.symbols.SourceModule;
import edu.brown.cs.bubbles.pybase.symbols.SourceToken;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.TupleN;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.comp_contextType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.jython.ast.name_contextType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class PybaseSemanticVisitor extends Visitor implements PybaseConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybaseProject		for_project;
private String			module_name;
private DuplicationChecker	duplication_checker;
private int			is_in_test_scope;
private MessagesManager 	messages_manager;
private PybaseNature		the_nature;
private Scope			cur_scope;
private List<Found>		probably_not_defined;
private AbstractModule		current_module;
private IDocument		for_document;
private ICompletionCache	completion_cache;
private LocalScope		current_local_scope;
private Set<String>		builtin_tokens;
private NoSelfChecker		no_self_checker;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseSemanticVisitor(PybaseProject proj,String modname,AbstractModule cur,PybasePreferences prefs,
			 IDocument doc)
{
   for_project = proj;
   the_nature = for_project.getNature();
   current_local_scope = new LocalScope();
   probably_not_defined = new ArrayList<Found>();
   builtin_tokens = new HashSet<String>();

   current_module = cur;
   module_name = modname;
   for_document = doc;
   is_in_test_scope = 0;
   cur_scope = new Scope(this,the_nature,module_name);
   if (current_module instanceof SourceModule) {
      current_local_scope.getScopeStack().push(((SourceModule) current_module).getAst());
    }

   startScope(ScopeType.GLOBAL, null); // initial cur_scope - there is only one 'global'
   CompletionState completionState = CompletionStateFactory.getEmptyCompletionState(
      the_nature, new CompletionCache());
   completion_cache = completionState;

   List<AbstractToken> builtinCompletions = the_nature.getAstManager().getBuiltinCompletions(
      completionState, new ArrayList<AbstractToken>());

   if (module_name != null && module_name.endsWith("__init__")) {
      // __path__ should be added to modules that have __init__
      builtinCompletions.add(new SourceToken(new Name("__path__",expr_contextType.Load,false),
						"__path__","","",module_name));
    }

   for (AbstractToken t : builtinCompletions) {
      Found found = makeFound(t);
      org.python.pydev.core.Tuple<AbstractToken, Found> tup = new org.python.pydev.core.Tuple<AbstractToken, Found>(
	 t,found);
      addToNamesToIgnore(t, cur_scope.getCurrScopeItems(), tup);
      builtin_tokens.add(t.getRepresentation());
    }

   messages_manager = new MessagesManager(prefs,module_name,for_document);
   duplication_checker = new DuplicationChecker(this);
   no_self_checker = new NoSelfChecker(this,module_name);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Scope getScope()
{
   return cur_scope;
}

public List<PybaseMessage> getMessages()
{
   endScope(null); // have to end the cur_scope that started when we created the class.

   return messages_manager.getMessages();
}


/********************************************************************************/
/*										*/
/*	Basic visitation methods						*/
/*										*/
/********************************************************************************/

@Override public Object visitAssert(Assert node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitAssert(node);
   is_in_test_scope -= 1;
   return r;
}


@Override public Object visitAssign(Assign node) throws Exception
{
   is_in_test_scope += 1;
   unhandled_node(node);
   PybaseSemanticVisitor visitor = this;

   // in 'm = a', this is 'a'
   if (node.value != null) node.value.accept(visitor);

   // in 'm = a', this is 'm'
   if (node.targets != null) {
      for (int i = 0; i < node.targets.length; i++) {
	 if (node.targets[i] != null) node.targets[i].accept(visitor);
       }
    }
   onAfterVisitAssign(node);
   is_in_test_scope -= 1;
   return null;
}


@Override public Object visitAttribute(Attribute node) throws Exception
{
   unhandled_node(node);
   boolean doReturn = visitNeededAttributeParts(node, this);

   if (doReturn) {
      return null;
    }

   SourceToken token = AbstractVisitor.makeFullNameToken(node, module_name);
   if (token.getRepresentation().equals("")) {
      return null;
    }
   String fullRep = token.getRepresentation();

   if (node.ctx == expr_contextType.Store || node.ctx == expr_contextType.Param
	  || node.ctx == expr_contextType.KwOnlyParam || node.ctx == expr_contextType.AugStore) {
      // in a store attribute, the first part is always a load
      int i = fullRep.indexOf('.', 0);
      String sub = fullRep;
      if (i > 0) {
	 sub = fullRep.substring(0, i);
       }
      markRead(token, sub, true, false,false);
    }
   else if (node.ctx == expr_contextType.Load) {
      Iterator<String> it = new FullRepIterable(fullRep).iterator();
      boolean found = false;

      while (it.hasNext()) {
	 String sub = it.next();
	 if (it.hasNext()) {
	    if (markRead(token, sub, false, false, true)) {
	       found = true;
	     }
	  }
	 else {
	    markRead(token, fullRep, !found, true, false); // only set it to add to not defined
	    // if it was still not found
	  }
       }
    }
   return null;
}


/**
 * In this function, the visitor will transverse the value of the attribute as needed,
 * if it is a subscript, call, etc, as those things are not actually a part of the attribute,
 * but are rather 'in' the attribute.
 *
 * @param node the attribute to visit
 * @param base the visitor that should visit the elements inside the attribute
 * @return true if there's no need to keep visiting other stuff in the attribute
				  * @throws Exception
				  */
private boolean visitNeededAttributeParts(final Attribute node,VisitorBase base)
throws Exception
{
   exprType value = node.value;
   boolean valueVisited = false;
   boolean doReturn = false;
   if (value instanceof Subscript) {
      Subscript subs = (Subscript) value;
      base.traverse(subs.slice);
      if (subs.value instanceof Name) {
	 base.visitName((Name) subs.value);
       }
      else {
	 base.traverse(subs.value);
       }
      // No need to keep visiting. Reason:
      // Let's take the example:
      // print function()[0].strip()
      // function()[0] is part 1 of attribute
      //
      // and the .strip will constitute the second part of the attribute
      // and its value (from the subscript) constitutes the 'function' part,
      // so, when we visit it directly, we don't have to visit the first part anymore,
      // because it was just visited... kind of strange to think about it though.
      doReturn = true;

    }
   else if (value instanceof Call) {
      visitCallAttr((Call) value, base);
      valueVisited = true;

    }
   else if (value instanceof Tuple) {
      base.visitTuple((Tuple) value);
      valueVisited = true;

    }
   else if (value instanceof Dict) {
      base.visitDict((Dict) value);
      doReturn = true;
    }
   if (!doReturn && !valueVisited) {
      if (visitNeededValues(value, base)) {
	 doReturn = true;
       }
    }
   return doReturn;
}

private void visitCallAttr(Call c,VisitorBase base) throws Exception
{
   // now, visit all inside it but the func itself
   VisitorBase visitor = base;
   if (c.func instanceof Attribute) {
      base.visitAttribute((Attribute) c.func);
    }
   if (c.args != null) {
      for (int i = 0; i < c.args.length; i++) {
	 if (c.args[i] != null) c.args[i].accept(visitor);
       }
    }
   if (c.keywords != null) {
      for (int i = 0; i < c.keywords.length; i++) {
	 if (c.keywords[i] != null) c.keywords[i].accept(visitor);
       }
    }
   if (c.starargs != null) c.starargs.accept(visitor);
   if (c.kwargs != null) c.kwargs.accept(visitor);
}


private boolean visitNeededValues(exprType value,VisitorBase base) throws Exception
{
   if (value instanceof Name) {
      return false;
    }
   else if (value instanceof Attribute) {
      return visitNeededValues(((Attribute) value).value, base);
    }
   else {
      value.accept(base);
      return true;
    }
}


@Override public Object visitAugAssign(AugAssign node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitAugAssign(node);
   is_in_test_scope -= 1;
   return r;
}


@Override public Object visitCall(Call node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitCall(node);
   is_in_test_scope -= 1;
   return r;
}


@Override public Object visitClassDef(ClassDef node) throws Exception
{
   unhandled_node(node);

   PybaseSemanticVisitor visitor = this;

   handleDecorators(node.decs);

   // we want to visit the bases before actually starting the class cur_scope (as it's as
   // if
   // they're attribute
   // accesses).
   if (node.bases != null) {
      for (int i = 0; i < node.bases.length; i++) {
	 if (node.bases[i] != null) node.bases[i].accept(visitor);
       }
    }

   current_local_scope.getScopeStack().push(node);
   startScope(ScopeType.CLASS, node);

   if (node.name != null) {
      node.name.accept(visitor);
    }

   if (node.body != null) {
      for (int i = 0; i < node.body.length; i++) {
	 if (node.body[i] != null) node.body[i].accept(visitor);
       }
    }

   endScope(node);
   current_local_scope.getScopeStack().pop();

   // the class is only added to the names to ignore when it's cur_scope is resolved!
   addToNamesToIgnore(node, true, true);


   return null;
}


@Override public Object visitCompare(Compare node) throws Exception
{
   Object ret = super.visitCompare(node);
   if (is_in_test_scope == 0) {
      SourceToken token = AbstractVisitor.makeToken(node, module_name);
      messages_manager.addMessage(ErrorType.NO_EFFECT_STMT, token);
    }
   return ret;
}


@Override public Object visitComprehension(Comprehension node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitComprehension(node);
   is_in_test_scope -= 1;
   return r;

}


@Override public Object visitDictComp(DictComp node) throws Exception
{
   unhandled_node(node);
   if (node.generators != null) {
      for (int i = 0; i < node.generators.length; i++) {
	 if (node.generators[i] != null) {
	    node.generators[i].accept(this);
	  }
       }
    }
   if (node.key != null) {
      node.key.accept(this);
    }
   if (node.value != null) {
      node.value.accept(this);
    }
   return null;
}


@Override public Object visitFor(For node) throws Exception
{
   cur_scope.addIfSubScope();
   Object ret = super.visitFor(node);
   cur_scope.removeIfSubScope();
   return ret;
}


@Override public Object visitFunctionDef(FunctionDef node) throws Exception
{
   unhandled_node(node);
   addToNamesToIgnore(node, false, true);

   PybaseSemanticVisitor visitor = this;
   argumentsType args = node.args;

   // visit the defaults first (before starting the cur_scope, because this is where the
   // load of variables from other scopes happens)
   if (args.defaults != null) {
      for (exprType expr : args.defaults) {
	 if (expr != null) {
	    expr.accept(visitor);
	  }
       }
    }

   // then the decorators (no, still not in method cur_scope)
   handleDecorators(node.decs);

   startScope(ScopeType.METHOD, node);
   current_local_scope.getScopeStack().push(node);

   cur_scope.setIsInMethodDefinition(true);
   // visit regular args
   if (args.args != null) {
      for (exprType expr : args.args) {
	 expr.accept(visitor);
       }
    }

   // visit varargs
   if (args.vararg != null) {
      args.vararg.accept(visitor);
    }

   // visit kwargs
   if (args.kwarg != null) {
      args.kwarg.accept(visitor);
    }

   // visit keyword only args
   if (args.kwonlyargs != null) {
      for (exprType expr : args.kwonlyargs) {
	 expr.accept(visitor);
       }
    }
   cur_scope.setIsInMethodDefinition(false);

   // visit annotation

   if (args.annotation != null) {
      for (exprType expr : args.annotation) {
	 if (expr != null) {
	    expr.accept(visitor);
	  }
       }
    }

   // visit the return
   if (node.returns != null) {
      node.returns.accept(visitor);
    }

   // visit the body
   if (node.body != null) {
      for (int i = 0; i < node.body.length; i++) {
	 if (node.body[i] != null) {
	    node.body[i].accept(visitor);
	  }
       }
    }

   endScope(node); // don't report unused variables if the method is virtual
   current_local_scope.getScopeStack().pop();
   return null;
}


@Override public Object visitGlobal(Global node) throws Exception
{
   unhandled_node(node);
   for (NameTokType name : node.names) {
      Name nameAst = new Name(((NameTok) name).id,expr_contextType.Store,false);
      nameAst.beginLine = name.beginLine;
      nameAst.beginColumn = name.beginColumn;

      SourceToken token = AbstractVisitor.makeToken(nameAst, module_name);
      cur_scope.addTokenToGlobalScope(token);
      addToNamesToIgnore(nameAst, false, true); // it is global, so, ignore it...
    }
   return null;
}


@Override public Object visitIf(If node) throws Exception
{
   cur_scope.addIfSubScope();
   Object r = super.visitIf(node);
   cur_scope.removeIfSubScope();
   return r;
}


@Override public Object visitLambda(org.python.pydev.parser.jython.ast.Lambda node)
throws Exception
{
   is_in_test_scope += 1;
   unhandled_node(node);

   PybaseSemanticVisitor visitor = this;
   argumentsType args = node.args;

   // visit the defaults first (before starting the cur_scope, because this is where the
   // load
   // of variables from other scopes happens)
   if (args.defaults != null) {
      for (exprType expr : args.defaults) {
	 if (expr != null) {
	    expr.accept(visitor);
	  }
       }
    }

   startScope(ScopeType.LAMBDA, node);


   cur_scope.setIsInMethodDefinition(true);
   // visit regular args
   if (args.args != null) {
      for (exprType expr : args.args) {
	 expr.accept(visitor);
       }
    }

   // visit varargs
   if (args.vararg != null) {
      args.vararg.accept(visitor);
    }

   // visit kwargs
   if (args.kwarg != null) {
      args.kwarg.accept(visitor);
    }

   // visit keyword only args
   if (args.kwonlyargs != null) {
      for (exprType expr : args.kwonlyargs) {
	 expr.accept(visitor);
       }
    }

   cur_scope.setIsInMethodDefinition(false);

   // visit the body
   if (node.body != null) {
      node.body.accept(visitor);
    }

   endScope(node);
   is_in_test_scope -= 1;
   return null;
}


@Override public Object visitListComp(final ListComp node) throws Exception
{
   unhandled_node(node);
   if (node.ctx == comp_contextType.TupleCtx) {
      startScope(ScopeType.LIST_COMP, node);
    }
   try {
      Comprehension type = null;
      if (node.generators != null && node.generators.length > 0) {
	 type = (Comprehension) node.generators[0];
       }
      List<exprType> eltsToVisit = new ArrayList<exprType>();

      // we need to take care of 'nested list comprehensions'
      if (type != null && type.iter instanceof ListComp) {
	 // print dict((day, index) for index, daysRep in (day for day in enumeratedDays))
	 final ListComp listComp = (ListComp) type.iter;

	 // the "(day for day in enumeratedDays)" is in its own cur_scope
	 if (listComp.ctx == comp_contextType.TupleCtx) {
	    startScope(ScopeType.LIST_COMP, listComp);
	  }
	 try {
	    visitListCompGenerators(listComp, eltsToVisit);
	    for (exprType type2 : eltsToVisit) {
	       type2.accept(this);
	     }
	  }
	 finally {
	    if (listComp.ctx == comp_contextType.TupleCtx) {
	       endScope(listComp);
	     }
	  }
	 type.target.accept(this);
	 if (node.elt != null) {
	    node.elt.accept(this);
	  }


	 return null;
       }

      // then the generators...
      if (node.generators != null) {
	 for (int i = 0; i < node.generators.length; i++) {
	    if (node.generators[i] != null) {
	       node.generators[i].accept(this);
	     }
	  }
       }


      // we need to take care of 'nested list comprehensions'
      if (node.elt instanceof ListComp) {
	 // print dict((day, index) for index, daysRep in enumeratedDays for day in
	 // daysRep)
	 // note that the daysRep is actually generated and used later in the expression
	 visitListCompGenerators((ListComp) node.elt, eltsToVisit);
	 for (exprType type2 : eltsToVisit) {
	    type2.accept(this);
	  }
	 return null;
       }

      if (node.elt != null) {
	 node.elt.accept(this);
       }

      return null;
    }
   finally {
      if (node.ctx == comp_contextType.TupleCtx) {
	 endScope(node);
       }
    }
}


private void visitListCompGenerators(ListComp node,List<exprType> eltsToVisit)
throws Exception
{
   for (comprehensionType c : node.generators) {
      Comprehension comp = (Comprehension) c;
      if (node.elt instanceof ListComp) {
	 visitListCompGenerators((ListComp) node.elt, eltsToVisit);
	 comp.accept(this);
       }
      else {
	 comp.accept(this);
	 eltsToVisit.add(node.elt);
       }
    }
}


@Override public Object visitImport(Import node) throws Exception
{
   unhandled_node(node);
   List<AbstractToken> list = AbstractVisitor.makeImportToken(node, null, module_name, true);

   for (AbstractToken token : list) {
      if (builtin_tokens.contains(token.getRepresentation())) {
	 // Overriding builtin...
	 onAddAssignmentToBuiltinMessage(token, token.getRepresentation());
       }
    }
   cur_scope.addImportTokens(list, null, completion_cache);
   return null;
}


@Override public Object visitImportFrom(ImportFrom node) throws Exception
{
   unhandled_node(node);
   try {

      if (AbstractVisitor.isWildImport(node)) {
	 AbstractToken wildImport = AbstractVisitor.makeWildImportToken(node, null, module_name);

	 CompletionState state = CompletionStateFactory.getEmptyCompletionState(
	    the_nature, completion_cache);
	 state.setBuiltinsGotten(true); // we don't want any builtins
	 List<AbstractToken> completionsForWildImport = new ArrayList<AbstractToken>();
	 if (the_nature.getAstManager().getCompletionsForWildImport(state,
								       current_module, completionsForWildImport, wildImport)) {
	    cur_scope.addImportTokens(completionsForWildImport, wildImport,
					 completion_cache);
	  }
       }
      else {
	 List<AbstractToken> list = AbstractVisitor.makeImportToken(node, null, module_name,
								       true);
	 cur_scope.addImportTokens(list, null, completion_cache);
       }

    }
   catch (Exception e) {
      PybaseMain.logE("Error when analyzing module " + module_name, e);
    }
   return null;
}


@Override public Object visitName(Name node) throws Exception
{
   unhandled_node(node);
   // when visiting the global namespace, we don't go into any inner cur_scope.
   SourceToken token = AbstractVisitor.makeToken(node, module_name);
   boolean found = true;
   // on aug assign, it has to enter both, the load and the read (but first the load,
   // because it can be undefined)
   if (node.ctx == expr_contextType.Load || node.ctx == expr_contextType.Del || node.ctx == expr_contextType.AugStore) {
      found = markRead(token);
    }

   if (node.ctx == expr_contextType.Store || node.ctx == expr_contextType.Param || node.ctx == expr_contextType.KwOnlyParam
	  || (node.ctx == expr_contextType.AugStore && found)) { // if it was undefined on augstore,
      // we do not go on to creating the token
      String rep = token.getRepresentation();
      if (builtin_tokens.contains(rep)) {
	 // Overriding builtin...
	 onAddAssignmentToBuiltinMessage(token, rep);
       }
      org.python.pydev.core.Tuple<AbstractToken, Found> fnd1 = cur_scope.isInNamesToIgnore(rep);

      // if (!inNamesToIgnore) {
      if (!rep.equals("self") && !rep.equals("cls")) {
	 cur_scope.addToken(token, token);
       }
      else {
	 addToNamesToIgnore(node, false, false); // ignore self
	 if (fnd1 != null) cur_scope.getCurrScopeItems().putRef(token,fnd1.o2);
       }
      //  }
    }

   return null;
}


@Override public Object visitNameTok(NameTok nameTok) throws Exception
{
   unhandled_node(nameTok);
   if (nameTok.ctx == name_contextType.VarArg || nameTok.ctx == name_contextType.KwArg) {
      SourceToken token = AbstractVisitor.makeToken(nameTok, module_name);
      cur_scope.addToken(token, token, (nameTok).id);
      if (builtin_tokens.contains(token.getRepresentation())) {
	 // Overriding builtin...
	 onAddAssignmentToBuiltinMessage(token, token.getRepresentation());
       }
    }
   return null;
}


@Override public Object visitPrint(Print node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitPrint(node);
   is_in_test_scope -= 1;
   return r;
}


@Override public Object visitRaise(Raise node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitRaise(node);
   is_in_test_scope -= 1;
   return r;
}


@Override public Object visitReturn(Return node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitReturn(node);
   is_in_test_scope -= 1;
   return r;
}


@Override public Object visitSetComp(SetComp node) throws Exception
{
   unhandled_node(node);
   if (node.generators != null) {
      for (int i = 0; i < node.generators.length; i++) {
	 if (node.generators[i] != null) {
	    node.generators[i].accept(this);
	  }
       }
    }
   if (node.elt != null) {
      node.elt.accept(this);
    }
   return null;
}


@Override public Object visitTryExcept(TryExcept node) throws Exception
{
   cur_scope.addTryExceptSubScope(node);
   Object r = super.visitTryExcept(node);
   cur_scope.removeTryExceptSubScope();
   return r;
}


@Override public Object visitTryFinally(TryFinally node) throws Exception
{
   cur_scope.addIfSubScope();
   Object r = super.visitTryFinally(node);
   cur_scope.removeIfSubScope();
   return r;
}


@Override public Object visitTuple(org.python.pydev.parser.jython.ast.Tuple node)
throws Exception
{
   is_in_test_scope += 1;
   Object ret = super.visitTuple(node);
   is_in_test_scope -= 1;
   return ret;
}


@Override public Object visitWhile(While node) throws Exception
{
   cur_scope.addIfSubScope();
   Object r = super.visitWhile(node);
   cur_scope.removeIfSubScope();
   return r;
}


@Override public Object visitYield(Yield node) throws Exception
{
   is_in_test_scope += 1;
   Object r = super.visitYield(node);
   is_in_test_scope -= 1;
   return r;
}


/********************************************************************************/
/*										*/
/*	Visitation assistance methods						*/
/*										*/
/********************************************************************************/

@Override public void traverse(SimpleNode node) throws Exception
{
   if (node instanceof If) {
      traverse((If) node);
    }
   else if (node instanceof While) {
      traverse((While) node);
    }
   else if (node instanceof ListComp) {
      visitListComp((ListComp) node);
    }
   else {
      super.traverse(node);
    }
}


private void traverse(If node) throws Exception
{
   if (node.test != null) {
      is_in_test_scope += 1;
      node.test.accept(this);
      is_in_test_scope -= 1;
    }

   if (node.body != null) {
      for (int i = 0; i < node.body.length; i++) {
	 if (node.body[i] != null) node.body[i].accept(this);
       }
    }
   if (node.orelse != null) {
      node.orelse.accept(this);
    }
}


private void traverse(While node) throws Exception
{
   PybaseSemanticVisitor visitor = this;
   if (node.test != null) {
      is_in_test_scope += 1;
      node.test.accept(visitor);
      is_in_test_scope -= 1;
    }

   if (node.body != null) {
      for (int i = 0; i < node.body.length; i++) {
	 if (node.body[i] != null) node.body[i].accept(visitor);
       }
    }
   if (node.orelse != null) node.orelse.accept(visitor);
}


@Override protected Object unhandled_node(SimpleNode node) throws Exception
{
   return null;
}


/********************************************************************************/
/*										*/
/*	Error and reactive methods						*/
/*										*/
/********************************************************************************/

private void onAddUndefinedVarInImportMessage(AbstractToken foundTok,Found foundAs)
{
   messages_manager.addUndefinedVarInImportMessage(foundTok, foundTok.getRepresentation());
}


private void onAddAssignmentToBuiltinMessage(AbstractToken foundTok,String representation)
{
   messages_manager.onAddAssignmentToBuiltinMessage(foundTok, representation);
}


private void onAddUndefinedMessage(AbstractToken token,Found foundAs)
{
   if ("...".equals(token.getRepresentation())) {
      return; // Ellipsis -- when found in the grammar, it's added as a name, which we can
      // safely ignore at this point.
    }

   // global cur_scope, so, even if it is defined later, this is an error...
   messages_manager.addUndefinedMessage(token);
}


private void onLastScope(PybaseScopeItems m)
{
   for (Found n : probably_not_defined) {
      String rep = n.getSingle().getToken().getRepresentation();
      Map<String, org.python.pydev.core.Tuple<AbstractToken, Found>> lastInStack = m.getNamesToIgnore();
      if (cur_scope.findInNamesToIgnore(rep, lastInStack) == null) {
	 onAddUndefinedMessage(n.getSingle().getToken(), n);
       }
    }
   messages_manager.setLastScope(m);
}


private void onAfterEndScope(SimpleNode node,PybaseScopeItems m)
{
   boolean reportunused = true;
   if (node != null && node instanceof FunctionDef) {
      reportunused = !isVirtual((FunctionDef) node);
    }

   if (reportunused) {
      // so, now, we clear the unused
      ScopeType scopetype = m.getScopeType();
      for (Found f : m.values()) {
	 if (!f.isUsed()) {
	    // we don't get unused at the global cur_scope or class definition cur_scope
	    // unless it's an import
	    if (ACCEPTED_METHOD_AND_LAMBDA.contains(scopetype) || f.isImport()) {
	       // only within methods do we put things as unused
	       messages_manager.addUnusedMessage(node, f);
	     }
	  }
       }
    }
}


private void onAfterStartScope(ScopeType newscopetype,SimpleNode node)
{
   if (newscopetype == ScopeType.CLASS) {
      duplication_checker.beforeClassDef((ClassDef) node);
      no_self_checker.beforeClassDef((ClassDef) node);

    }
   else if (newscopetype == ScopeType.METHOD) {
      duplication_checker.beforeFunctionDef((FunctionDef) node); // duplication checker
      no_self_checker.beforeFunctionDef((FunctionDef) node);
    }
}

private void onBeforeEndScope(SimpleNode node)
{
   if (node instanceof ClassDef) {
      no_self_checker.afterClassDef((ClassDef) node);
      duplication_checker.afterClassDef((ClassDef) node);

    }
   else if (node instanceof FunctionDef) {
      duplication_checker.afterFunctionDef((FunctionDef) node);// duplication checker
      no_self_checker.afterFunctionDef((FunctionDef) node);
    }
}

public void onAddUnusedMessage(SimpleNode node,Found found)
{
   messages_manager.addUnusedMessage(node, found);
}

public void onAddReimportMessage(Found newFound)
{
   messages_manager.addReimportMessage(newFound);
}

public void onAddUnresolvedImport(AbstractToken token)
{
   messages_manager.addMessage(ErrorType.UNRESOLVED_IMPORT, token);
}

public void onAddDuplicatedSignature(SourceToken token,String name)
{
   messages_manager.addMessage(ErrorType.DUPLICATED_SIGNATURE,token,name);
}

public void onAddNoSelf(SourceToken token,Object[] objects)
{
   messages_manager.addMessage(ErrorType.NO_SELF, token, objects);
}


private boolean isVirtual(FunctionDef node)
{
   if (node.body != null) {
      for (SimpleNode n : node.body) {
	 if (n instanceof Raise) {
	    continue;
	  }
	 if (n instanceof Expr) {
	    if (((Expr) n).value instanceof Str) {
	       continue;
	     }
	  }
	 return false;
       }
    }
   return true;
}


/********************************************************************************/
/*										*/
/*	Name management methods 						*/
/*										*/
/********************************************************************************/

private void addToNamesToIgnore(SimpleNode node,boolean finishClassScope,
				   boolean checkBuiltins)
{
   String pkg = "";
   if (current_module != null) {
      pkg = current_module.getName();
    }
   SourceToken token = AbstractVisitor.makeToken(node, pkg);

   if (checkBuiltins) {
      String rep = token.getRepresentation();
      if (builtin_tokens.contains(rep)) {
	 // Overriding builtin...
	 onAddAssignmentToBuiltinMessage(token, rep);
       }
    }

   PybaseScopeItems currScopeItems = cur_scope.getCurrScopeItems();

   Found found = new Found(token,token,cur_scope.getCurrScopeId(),
			      cur_scope.getCurrScopeItems());
   org.python.pydev.core.Tuple<AbstractToken, Found> tup = new org.python.pydev.core.Tuple<AbstractToken, Found>(
      token,found);
   addToNamesToIgnore(token, currScopeItems, tup);
   currScopeItems.putRef(node,found);

   // after adding it to the names to ignore, let's see if there is someone waiting for
   // this declaration
   // in the 'probably not defined' stack.
   for (Iterator<Found> it = probably_not_defined.iterator(); it.hasNext();) {
      Found n = it.next();

      GenAndTok single = n.getSingle();
      ScopeType foundscopetype = single.getScopeFound().getScopeType();
      // ok, if we are in a cur_scope method, we may not get things that were defined in a
      // class cur_scope.
      if (ACCEPTED_METHOD_AND_LAMBDA.contains(foundscopetype)
	     && cur_scope.getCurrScopeItems().getScopeType() == ScopeType.CLASS) {
	 continue;
       }
      AbstractToken tok = single.getToken();
      String rep = tok.getRepresentation();
      if (rep.equals(token.getRepresentation())) {
	 // found match in names to ignore...

	 if (finishClassScope && foundscopetype == ScopeType.CLASS
		&& cur_scope.getCurrScopeId() < single.getScopeFound().getScopeId()) {
	    it.remove();
	    onAddUndefinedMessage(tok, found);
	  }
	 else {
	    it.remove();
	    onNotDefinedFoundLater(n, found);
	  }
       }
    }
}


private void addToNamesToIgnore(AbstractToken token,PybaseScopeItems currScopeItems,
				   org.python.pydev.core.Tuple<AbstractToken, Found> tup)
{
   currScopeItems.getNamesToIgnore().put(token.getRepresentation(), tup);
   // onAfterAddToNamesToIgnore(currScopeItems, tup);
}


private void handleDecorators(decoratorsType[] decs) throws Exception
{
   if (decs != null) {
      for (decoratorsType dec : decs) {
	 if (dec != null) {
	    handleDecorator(dec);
	  }
       }
    }
}


private void handleDecorator(decoratorsType dec) throws Exception
{
   is_in_test_scope += 1;
   dec.accept(this);
   is_in_test_scope -= 1;
}




/********************************************************************************/
/*										*/
/*	Scoping methods 							*/
/*										*/
/********************************************************************************/

private void startScope(ScopeType newscopetype,SimpleNode node)
{
   cur_scope.startScope(newscopetype);
   onAfterStartScope(newscopetype, node);
}


private void endScope(SimpleNode node)
{
   onBeforeEndScope(node);

   PybaseScopeItems m = cur_scope.endScope(); // clear the last cur_scope
   for (Iterator<Found> it = probably_not_defined.iterator(); it.hasNext();) {
      Found n = it.next();

      final GenAndTok probablyNotDefinedFirst = n.getSingle();
      AbstractToken tok = probablyNotDefinedFirst.getToken();
      String rep = tok.getRepresentation();
      // we also get a last pass to the unused to see if they might have been defined
      // later on the higher cur_scope

      List<Found> foundItems = find(m, rep);
      boolean setUsed = false;
      for (Found found : foundItems) {
	 // the cur_scope where it is defined must be an outer cur_scope so that we can
	 // say it was defined later...
	 final GenAndTok foundItemFirst = found.getSingle();

	 // if something was not defined in a method, if we are in the class definition,
	 // it won't be found.

	 if (ACCEPTED_METHOD_AND_LAMBDA.contains(probablyNotDefinedFirst.getScopeFound().getScopeType())
			&& m.getScopeType() != ScopeType.CLASS) {
	    if (foundItemFirst.getScopeId() < probablyNotDefinedFirst.getScopeId()) {
	       found.setUsed(true);
	       setUsed = true;
	     }
	  }
       }
      if (setUsed) {
	 it.remove();
       }
    }

   // ok, this was the last cur_scope, so, the ones probably not defined are really not
   // defined at this
   // point
   if (cur_scope.size() == 0) {
      onLastScope(m);
    }


   onAfterEndScope(node, m);
}


private List<Found> find(PybaseScopeItems m,String fullRep)
{
   ArrayList<Found> foundItems = new ArrayList<Found>();
   if (m == null) {
      return foundItems;
    }

   int i = fullRep.indexOf('.', 0);

   while (i >= 0) {
      String sub = fullRep.substring(0, i);
      i = fullRep.indexOf('.', i + 1);
      foundItems.addAll(m.getAll(sub));
    }

   foundItems.addAll(m.getAll(fullRep));
   return foundItems;
}


/********************************************************************************/
/*										*/
/*	Note uses								*/
/*										*/
/********************************************************************************/

private boolean markRead(AbstractToken token)
{
   String rep = token.getRepresentation();
   return markRead(token, rep, true, false, false);
}


private boolean markRead(AbstractToken token,String rep,boolean addToNotDefined,
			    boolean checkIfIsValidImportToken,boolean intermediate)
{
   boolean found = false;
   Found foundAs = null;
   String foundAsStr = null;

   Set<ScopeType> acceptedScopes = EnumSet.noneOf(ScopeType.class);
   PybaseScopeItems currScopeItems = cur_scope.getCurrScopeItems();

   if (ACCEPTED_METHOD_AND_LAMBDA.contains(currScopeItems.getScopeType())) {
      acceptedScopes = ACCEPTED_METHOD_SCOPES;
    }
   else {
      acceptedScopes = ACCEPTED_ALL_SCOPES;
    }

   if ("locals".equals(rep)) {
      // if locals() is accessed, all the tokens currently found are marked as 'used'
      // use case:
      //
      // def f2():
      // a = 1
      // b = 2
      // c = 3
      // f1(**locals())
      for (Found f : currScopeItems.getAll()) {
	 f.setUsed(true);
       }
      return true;
    }

   Iterator<String> it = new FullRepIterable(rep,true).iterator();
   // search for it
   while (found == false && it.hasNext()) {
      String nextTokToSearch = it.next();
      foundAs = cur_scope.findFirst(nextTokToSearch, true, acceptedScopes);
      if (foundAs != null) {
         found = true;
	 foundAsStr = nextTokToSearch;
	 foundAs.getSingle().addReference(token);
	 if (intermediate && token.getAst() instanceof Attribute) {
	    Attribute at = (Attribute) token.getAst();
	    if (at.value instanceof Name) {
	       currScopeItems.putRef(at.value,foundAs);
	     }
	  }
         else if (!intermediate && token.getAst() instanceof Attribute) {
            Attribute at = (Attribute) token.getAst();
            if (at.value instanceof Name) {
               currScopeItems.putRef(at.value,foundAs);
             }
            // currScopeItems.putRef(at.attr,foundAs);
          }
	 else if (!intermediate) {
	    currScopeItems.putRef(token,foundAs);
	  }
       }
      else found = false;
    }


   if (!found) {
      // this token might not be defined... (still, might be in names to ignore)
      int i;
      if ((i = rep.indexOf('.')) != -1) {
	 // if it is an attribute, we have to check the names to ignore just with its
	 // first part
	 rep = rep.substring(0, i);
       }
      org.python.pydev.core.Tuple<AbstractToken, Found> xfound = cur_scope.isInNamesToIgnore(rep);
      if (xfound != null) {
	 AbstractToken t1 = xfound.o1;
	 if (t1 != null && t1 instanceof SourceToken) {
	    Found ff = xfound.o2;
	    if (ff != null) {
	       ff.getSingle().addReference(token);
	     }
	  }
         else currScopeItems.putRef(t1,token);
	 // currScopeItems.putRef(token,xfound.o2);
       }

      if (addToNotDefined && xfound == null) {
	 if (cur_scope.size() > 1) { // if we're not in the global cur_scope, it might be defined later
	    Found foundForProbablyNotDefined = makeFound(token);
	    probably_not_defined.add(foundForProbablyNotDefined); // we are not in the
	    // global cur_scope, so it might be defined later...
	    // onAddToProbablyNotDefined(token, foundForProbablyNotDefined);
	  }
	 else {
	    onAddUndefinedMessage(token, makeFound(token)); // it is in the global cur_scope, so it is undefined.
	  }
       }
    }
   else if (checkIfIsValidImportToken && foundAs != null && foundAsStr != null) {
      // ok, it was found, but is it an attribute (and if so, are all the parts in the
      // import defined?)
      // if it was an attribute (say xxx and initially it was xxx.foo, we will have to
      // check if the token foo
      // really exists in xxx, if it was found as an import)
      try {
	 if (foundAs.isImport() && !rep.equals(foundAsStr) && foundAs.getImportInfo() != null
		&& foundAs.getImportInfo().wasResolved()) {
	    // the foundAsStr equals the module resolved in the Found tok

	    AbstractModule m = foundAs.getImportInfo().getModule();
	    String tokToCheck;
	    if (foundAs.isWildImport()) {
	       tokToCheck = foundAsStr;
	     }
	    else {
	       String tok = foundAs.getImportInfo().getRepresentation();
	       tokToCheck = rep.substring(foundAsStr.length() + 1);
	       if (tok.length() > 0) {
		  tokToCheck = tok + "." + tokToCheck;
		}
	     }

	    for (String repToCheck : new FullRepIterable(tokToCheck)) {
	       FoundType inGlobalTokens = m.isInGlobalTokens(repToCheck, the_nature, true,
							  true, completion_cache);

	       if (inGlobalTokens == FoundType.NOT_FOUND) {
		  if (!isDefinitionUnknown(m, repToCheck)) {
		     // Check if there's some hasattr (if there is, we'll consider that
		     // the token which
		     // had the hasattr checked will actually have it).
		     Collection<AbstractToken> interfaceForLocal = current_local_scope
			.getInterfaceForLocal(foundAsStr, false, true);
		     boolean foundInHasAttr = false;
		     for (AbstractToken iToken : interfaceForLocal) {
			if (iToken.getRepresentation().equals(repToCheck)) {
			   foundInHasAttr = true;
			   break;
			 }
		      }

		     if (!foundInHasAttr) {
			AbstractToken foundTok = findNameTok(token, repToCheck);
			onAddUndefinedVarInImportMessage(foundTok, foundAs);
		      }
		   }
		  break;// no need to keep checking once one is not defined

		}
	       else if (inGlobalTokens == FoundType.FOUND_BECAUSE_OF_GETATTR) {
		  break;
		}
	     }
	  }
	 else if (foundAs.isImport()
		     && (foundAs.getImportInfo() == null || !foundAs.getImportInfo().wasResolved())) {
	    // import was not resolved
	    onFoundUnresolvedImportPart(token, rep, foundAs);
	  }
       }
      catch (Exception e) {
	 // Log.log("Error checking for valid tokens (imports) for "+moduleName, e);
       }
    }
   return found;
}


/********************************************************************************/
/*										*/
/*	Symbol checking 							*/
/*										*/
/********************************************************************************/

/**
 * @return whether we're actually unable to identify that the representation
 * we're looking exists or not, so,
 * True is returned if we're really unable to identify if that token does
 * not exist and
 * False if we're sure it does not exist
 */

private boolean isDefinitionUnknown(AbstractModule m,String repToCheck) throws Exception
{
   String name = m.getName();
   TupleN key = new TupleN("isDefinitionUnknown",name != null ? name : "",repToCheck);
   Boolean isUnknown = (Boolean) completion_cache.getObj(key);
   if (isUnknown == null) {
      isUnknown = internalGenerateIsDefinitionUnknown(m, repToCheck);
      completion_cache.add(key, isUnknown);
    }
   return isUnknown;
}


/**
 * Actually makes the check to see if a given representation is unknown in a given module (without using caches)
 */
private boolean internalGenerateIsDefinitionUnknown(AbstractModule m,String repToCheck)
throws Exception
{
   if (!(m instanceof SourceModule)) {
      return false;
    }
   repToCheck = FullRepIterable.headAndTail(repToCheck, true)[0];
   if (repToCheck.length() == 0) {
      return false;
    }
   Definition[] definitions = m.findDefinition(CompletionStateFactory
						  .getEmptyCompletionState(repToCheck, the_nature, completion_cache), -1, -1,
						  the_nature);
   if (definitions.length == 1) {
      Definition foundDefinition = definitions[0];
      if (foundDefinition instanceof AssignDefinition) {
	 AssignDefinition d = (AssignDefinition) definitions[0];

	 // if the value is currently None, it will be set later on
	 if (d.getValueName().equals("None")) {
	    return true;
	  }

	 // ok, go to the definition of whatever is set
	 Definition[] definitions2 = d.getModule().findDefinition(CompletionStateFactory
								    .getEmptyCompletionState(d.getValueName(),the_nature,completion_cache),
								    d.getLine(),d.getCol(),the_nature);

	 if (definitions2.length == 1) {
	    // and if it is a function, we're actually unable to find
	    // out about its return value
	    Definition definition = definitions2[0];
	    if (definition.getAssignment() instanceof FunctionDef) {
	       return true;
	     }
	    else if (definition.getAssignment() instanceof ClassDef) {
	       ClassDef def = (ClassDef) definition.getAssignment();
	       if (isDynamicClass(def)) {
		  return true;
		}
	     }
	  }
       }
      else  { // not Assign definition
	 Definition definition = foundDefinition;
	 if (definition.getAssignment() instanceof ClassDef) {
	    // direct class access
	    ClassDef classDef = (ClassDef) definition.getAssignment();
	    if (isDynamicClass(classDef)) {
	       return true;
	     }
	  }

       }
    }
   return false;
}


/**
 * @return whether the passed class definition has a docstring indicating that it has dynamic
 */
private boolean isDynamicClass(ClassDef def)
{
   String docString = NodeUtils.getNodeDocString(def);
   if (docString != null) {
      if (docString.indexOf("@DynamicAttrs") != -1) {
	 // class that has things dynamically defined.
	 return true;
       }
    }
   return false;
}


private Found makeFound(AbstractToken token)
{
   return new Found(token,token,cur_scope.getCurrScopeId(),cur_scope.getCurrScopeItems());
}


private AbstractToken findNameTok(AbstractToken token,String tokToCheck)
{
   if (token instanceof SourceToken) {
      SourceToken s = (SourceToken) token;
      SimpleNode ast = s.getAst();

      String searchFor = FullRepIterable.getLastPart(tokToCheck);
      while (ast instanceof Attribute) {
	 Attribute a = (Attribute) ast;

	 if (((NameTok) a.attr).id.equals(searchFor)) {
	    return new SourceToken(a.attr,searchFor,"","",token.getParentPackage());

	  }
	 else if (a.value.toString().equals(searchFor)) {
	    return new SourceToken(a.value,searchFor,"","",token.getParentPackage());
	  }
	 ast = a.value;
       }
    }
   return token;
}


private void onAfterVisitAssign(Assign node)
{
   no_self_checker.visitAssign(node);
}



/**
 * This one is not abstract, but is provided as a hook, as the others.
 */
private void onNotDefinedFoundLater(Found foundInProbablyNotDefined,Found laterFound)
{}



/**
 * This one is not abstract, but is provided as a hook, as the others.
 */
private void onFoundUnresolvedImportPart(AbstractToken token,String rep,Found foundAs)
{}

/**
 * This one is not abstract, but is provided as a hook, as the others.
 */
public void onImportInfoSetOnFound(Found found)
{}



} // end of class PybaseSemanticVisitor


/* end of PybaseSemanticVisitor.java */


