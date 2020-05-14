/********************************************************************************/
/*										*/
/*		NobaseScope.java						*/
/*										*/
/*	Representation of a javascript scope					*/
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



package edu.brown.cs.bubbles.nobase;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


class NobaseScope implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,NobaseSymbol>	defined_names;
private Map<String,Set<NobaseSymbol>>	all_names;
private ScopeType			scope_type;
private NobaseScope			parent_scope;
private NobaseValue			object_value;
private int				temp_counter;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseScope(ScopeType typ,NobaseScope par)
{
   defined_names = new ConcurrentHashMap<String,NobaseSymbol>();
   scope_type = typ;
   parent_scope = par;
   object_value = null;
   temp_counter = 0;
   all_names = null;
   if (typ == ScopeType.FILE) all_names = new HashMap<String,Set<NobaseSymbol>>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

ScopeType getScopeType()		{ return scope_type; }

NobaseScope getParent() 		{ return parent_scope; }

NobaseValue getThisValue()
{
   return object_value;
}

int getNextTemp()			{ return ++temp_counter; }

void setValue(NobaseValue nv)
{
   if (nv == null || object_value == nv) return;
   if (object_value != null) {
      object_value.mergeProperties(nv);
    }
   object_value = nv;
}

NobaseSymbol define(NobaseSymbol sym)
{
   if (sym == null) return null;
   
   if (scope_type == ScopeType.LOCAL && sym.getSymbolType() == SymbolType.VAR) {
      return parent_scope.define(sym);
    }

   NobaseMain.logD("Define " + sym + " in scope " + this);

   NobaseSymbol osym = defined_names.get(sym.getName());
   if (osym != null) return osym;
   defined_names.put(sym.getName(),sym);
   defineAll(sym);
   return sym;
}


Collection<NobaseSymbol> findAll(String name)
{
   if (all_names == null) {
      if (parent_scope == null) return null;
      return parent_scope.findAll(name);
    }
   return all_names.get(name);
}



void defineAll(NobaseSymbol sym)
{
  defineAll(sym,sym.getName());
}



void defineAll(NobaseSymbol sym,String nm)
{
   if (all_names == null) {
      if (parent_scope != null) parent_scope.defineAll(sym,nm);
    }
   else {
      switch (sym.getNameType()) {
	 case FUNCTION :
	 case VARIABLE :
	    break;
	 case MODULE :
	 case LOCAL :
	    return;
       }
      if (nm == null) return;
      int idx = nm.lastIndexOf(".");
      if (idx > 0) nm = nm.substring(idx+1);
      if (NobaseResolver.isGeneratedName(nm)) return;
      Set<NobaseSymbol> syms = all_names.get(nm);
      if (syms == null) {
	 syms = new HashSet<NobaseSymbol>(2);
	 all_names.put(nm,syms);
       }
      syms.add(sym);
    }
}



void setProperty(Object name,NobaseValue nv)
{
   if (object_value == null) return;
   object_value.addProperty(name,nv);
}



NobaseSymbol lookup(String name)
{
   NobaseSymbol sym = defined_names.get(name);
   if (sym != null) return sym;
   if (parent_scope == null) return null;
   if (scope_type == ScopeType.MEMBER) return null;
   return parent_scope.lookup(name);
}


NobaseValue lookupValue(String name,boolean lhs)
{
   NobaseSymbol ns = lookup(name);

   if (name.equals("this") && object_value != null) {
      if (ns == null) return getThisValue();
      return getThisValue();
    }

   switch (scope_type) {
      case MEMBER :
      case WITH :
	 if (object_value != null) {
	    NobaseValue nv = object_value.getProperty(name,lhs);
	    if (nv != null) return nv;
	  }
	 break;
      default :
	 break;
    }

   if (ns == null) return null;
   return ns.getValue();
}

Collection<NobaseSymbol> getDefinedNames()
{
   return defined_names.values();
}

NobaseScope getDefaultScope()
{
   if (parent_scope == null) return this;
   switch (scope_type) {
      case CATCH :
      case OBJECT :
      case LOCAL :
	return parent_scope.getDefaultScope();
      case FILE :
      case GLOBAL :
      case FUNCTION :
         break;
      case MEMBER :
      case WITH :
	 break;
    }
   return this;
}

}	// end of class NobaseScope




/* end of NobaseSymbol.java */

