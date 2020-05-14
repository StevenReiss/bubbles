/********************************************************************************/
/*										*/
/*		PybaseScopeItems.java						*/
/*										*/
/*	Python Bubbles Base scope items set representation			*/
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
 * Created on 27/07/2005
 */


package edu.brown.cs.bubbles.pybase;



import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.Found;
import edu.brown.cs.bubbles.pybase.symbols.NodeUtils;
import edu.brown.cs.bubbles.pybase.symbols.Scope;
import edu.brown.cs.bubbles.pybase.symbols.SourceToken;

import org.python.pydev.core.Tuple;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.excepthandlerType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;



public final class PybaseScopeItems implements PybaseConstants {


/********************************************************************************/
/*										*/
/*	Exposed inner class for Try--Except information 			*/
/*										*/
/********************************************************************************/

/**
 * This is the class that is used to wrap the try..except node (so that we can add additional info to it).
 */
public static class TryExceptInfo {
   private TryExcept		   try_except;
   private Map<String, List<Found>> imports_map_in_try_except = new HashMap<String, List<Found>>();

   public TryExceptInfo(TryExcept except)
      {
      try_except = except;
    }

   /**
      * When we add a new import found within a try..except ImportError, we mark the previous import
   * with the same name as used (as this one will redefine it in an expected way).
   */
   public void addFoundImportToTryExcept(Found found) {
      if (!found.isImport()) {
	 return;
       }
      String rep = found.getSingle().getGenerator().getRepresentation();
      List<Found> importsListInTryExcept = imports_map_in_try_except.get(rep);
      if (importsListInTryExcept == null) {
	 importsListInTryExcept = new ArrayList<Found>();
	 imports_map_in_try_except.put(rep, importsListInTryExcept);

       }
      else if (importsListInTryExcept.size() > 0) {
	 importsListInTryExcept.get(importsListInTryExcept.size() - 1).setUsed(true);
       }

      importsListInTryExcept.add(found);
    }

} // end of inner public inner class TryExceptInfo




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String, List<Found>>	  scope_map;
private Map<String, Tuple<AbstractToken, Found>> names_to_ignore;

private int			       if_sub_scope;
private FastStack<TryExceptInfo>	  try_except_sub_scope;
private int			       scope_id;
private ScopeType		       scope_type;

private PybaseScopeItems		parent_scope;
private List<PybaseScopeItems>		child_scopes;

private Map<SimpleNode,Found>		ast_map;
private Map<AbstractToken,List<AbstractToken>> builtin_map;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseScopeItems(int scopeId,ScopeType scopeType,PybaseScopeItems par)
{
   parent_scope = par;
   child_scopes = null;
   if (par != null) {
      if (par.child_scopes == null) par.child_scopes = new ArrayList<PybaseScopeItems>();
      par.child_scopes.add(this);
    }

   scope_id = scopeId;
   scope_type = scopeType;
   scope_map = new HashMap<String, List<Found>>();
   names_to_ignore = new HashMap<String, Tuple<AbstractToken, Found>>();
   if_sub_scope = 0;
   try_except_sub_scope = new FastStack<TryExceptInfo>();
   ast_map = new IdentityHashMap<SimpleNode,Found>();
   builtin_map = new HashMap<AbstractToken,List<AbstractToken>>();
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Map<String, Tuple<AbstractToken, Found>> getNamesToIgnore()
{
   return names_to_ignore;
}


/**
 * @return the TryExcept from a try..except ImportError if we are currently within such a scope
 * (otherwise will return null;.
				  */
public PybaseScopeItems.TryExceptInfo getTryExceptImportError()
{
   PybaseScopeItems.TryExceptInfo dflt = null;

   for (PybaseScopeItems.TryExceptInfo except : getCurrTryExceptNodes()) {
      for (excepthandlerType handler : except.try_except.handlers) {
	 if (handler.type != null) {
	    String rep = NodeUtils.getRepresentationString(handler.type);
	    if (rep != null && rep.equals("ImportError")) {
	       return except;
	    }
	 }
	 else dflt = except;
      }
   }
   return dflt;
}


public Found getLastAppearance(String rep)
{
   List<Found> foundItems = scope_map.get(rep);
   if (foundItems == null || foundItems.size() == 0) {
      return null;
   }
   return foundItems.get(foundItems.size() - 1);
}


/**
 * @return a list with all the found items in this scope
 */
public List<Found> getAll()
{
   List<Found> found = new ArrayList<Found>();
   Collection<List<Found>> values = scope_map.values();
   for (List<Found> list : values) {
      found.addAll(list);
   }
   return found;
}



List<Found> getAllSymbols()
{
   List<Found> rslt = getAll();

   for (Tuple<AbstractToken,Found> tup : getNamesToIgnore().values()) {
      Found fnd = tup.o2;
      AbstractToken tok = tup.o1;
      if (tok instanceof SourceToken) {
	 rslt.add(fnd);
       }
    }

   return rslt;
}



/**
 * @return all the found items that match the given representation.
 */
public List<Found> getAll(String rep)
{
   List<Found> r = scope_map.get(rep);
   if (r == null) {
      return new ArrayList<Found>(0);
   }
   return r;
}


Found findByToken(SimpleNode sn)
{
   return ast_map.get(sn);
}

List<AbstractToken> findAllRefs(AbstractToken t)
{
   List<AbstractToken> rslt = builtin_map.get(t);
   if (rslt != null) return rslt;

   return new ArrayList<AbstractToken>();
}



public void put(String rep,Found found)
{
   List<Found> foundItems = scope_map.get(rep);
   if (foundItems == null) {
      foundItems = new ArrayList<Found>();
      scope_map.put(rep, foundItems);
   }

   foundItems.add(found);
}


public void putRef(SimpleNode n,Found f)
{
   if (n == null || f == null) return;
   if (parent_scope != null) {
      parent_scope.putRef(n,f);
    }
   else {
      if (ast_map.get(n) != null && ast_map.get(n) != f) {
	 PybaseMain.logE("Duplicate mapping for " + n + " : " + ast_map.get(n) + " : " + f);
      }
      ast_map.put(n,f);
      SimpleNode n1 = SourceToken.getNameOrNameTokAst(n);
      if (n1 != null && n1 != n) putRef(n1,f);
      if (n instanceof Attribute) {
	 Attribute at = (Attribute) n;
	 putRef(at.attr,f);
	 // SimpleNode n2 = SourceToken.getNameOrNameTokAst(at.value);
	 // if (n2 != null) putRef(n2,f);
       }
      else if (n instanceof FunctionDef) {
	 putRef(((FunctionDef) n).name,f);
       }
    }
}

public void putRef(AbstractToken t,Found f)
{
   if (t == null || f == null) return;
   if (t instanceof SourceToken) {
      SourceToken st = (SourceToken) t;
      putRef(st.getAst(),f);
    }
}

public void putRef(AbstractToken t,AbstractToken t1)
{
   if (t == null || t1 == null) return;
   if (parent_scope != null) {
      parent_scope.putRef(t,t1);
   }
   else {
      List<AbstractToken> r = builtin_map.get(t);
      if (r == null) {
	 r = new ArrayList<AbstractToken>();
	 builtin_map.put(t,r);
      }
      r.add(t1);
   }
}



public Collection<Found> values()
{
   ArrayList<Found> ret = new ArrayList<Found>();
   for (List<Found> foundItems : scope_map.values()) {
      ret.addAll(foundItems);
   }
   return ret;
}


public void addIfSubScope()
{
   if_sub_scope++;
}


public void removeIfSubScope()
{
   if_sub_scope--;
}


public void addTryExceptSubScope(TryExcept node)
{
   try_except_sub_scope.push(new TryExceptInfo(node));
}


public void removeTryExceptSubScope()
{
   try_except_sub_scope.pop();
}


public FastStack<TryExceptInfo> getCurrTryExceptNodes()
{
   return try_except_sub_scope;
}


public boolean getIsInSubSubScope()
{
   return if_sub_scope != 0 || try_except_sub_scope.size() != 0;
}


public boolean getIsInIfSubScope()
{
   return if_sub_scope != 0;
}


/**
 * @return Returns the scopeId.
 */
public int getScopeId()
{
   return scope_id;
}


/**
 * @return Returns the scopeType.
 */
public ScopeType getScopeType()
{
   return scope_type;
}


/**
 * @return all the used items
 */
public List<Tuple<String, Found>> getUsedItems()
{
   ArrayList<Tuple<String, Found>> found = new ArrayList<Tuple<String, Found>>();
   for (Map.Entry<String, List<Found>> entry : scope_map.entrySet()) {
      for (Found f : entry.getValue()) {
	 if (f.isUsed()) {
	    found.add(new Tuple<String, Found>(entry.getKey(),f));
	 }
      }
   }
   return found;
}


PybaseScopeItems getParent()				{ return parent_scope; }
Collection<PybaseScopeItems> getChildren()		{ return child_scopes; }



/********************************************************************************/
/*										*/
/*	Visitation methods							*/
/*										*/
/********************************************************************************/

void accept(SymbolVisitor sv)
{
   sv.visitScopeBegin(this);

   if (scope_map != null) {
      for (List<Found> lf : scope_map.values()) {
	 for (Found f : lf) sv.visitItem(f);
       }
    }
   if (child_scopes != null) {
      for (PybaseScopeItems ps : child_scopes) {
	 ps.accept(sv);
       }
    }
   sv.visitScopeEnd(this);
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuilder buffer = new StringBuilder();
   buffer.append("ScopeItem (type:");
   buffer.append(Scope.getScopeTypeStr(scope_type));
   buffer.append(")\n");
   for (Map.Entry<String, List<Found>> entry : scope_map.entrySet()) {
      buffer.append(entry.getKey());
      buffer.append(": contains ");
      buffer.append(entry.getValue().toString());
      buffer.append("\n");
   }
   return buffer.toString();
}


} // end of class PybaseScopeItems


/* end of PybaseScopeItems.java */
