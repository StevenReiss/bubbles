/********************************************************************************/
/*										*/
/*		Found.java							*/
/*										*/
/*	Python Bubbles Base found name representation				*/
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
 * Created on 23/07/2005
 */

package edu.brown.cs.bubbles.pybase.symbols;


import edu.brown.cs.bubbles.pybase.PybaseScopeItems;
import edu.brown.cs.bubbles.pybase.symbols.ImportChecker.ImportInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public final class Found implements Iterable<GenAndTok> {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<GenAndTok> found_item = new ArrayList<GenAndTok>();
private boolean  is_used    = false;
private ImportInfo	import_info;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public Found(AbstractToken tok,AbstractToken generator,int scopeId,PybaseScopeItems scopeFound)
{
   found_item = new ArrayList<GenAndTok>();
   is_used = false;
   import_info = null;

   found_item.add(new GenAndTok(generator,tok,scopeId,scopeFound));
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public ImportInfo getImportInfo()
{
   return import_info;
}

public void setImportInfo(ImportInfo ii)
{
   import_info = ii;
}

public void setUsed(boolean used)
{
   is_used = used;
}

public boolean isUsed()
{
   return is_used;
}

@Override public Iterator<GenAndTok> iterator()
{
   return found_item.iterator();
}


public void addGeneratorToFound(AbstractToken generator2,AbstractToken tok2,int scopeId,
	 PybaseScopeItems scopeFound)
{
   found_item.add(new GenAndTok(generator2,tok2,scopeId,scopeFound));
}


public void addGeneratorsFromFound(Found found2)
{
   found_item.addAll(found2.found_item);
}


public GenAndTok getSingle()
{
   return found_item.get(found_item.size() - 1); // always returns the last (this is the
// one that is binded at the current place in the scope)
}


public List<GenAndTok> getAll()
{
   return found_item;
}

public boolean isImport()
{
   return getSingle().getGenerator().isImport();
}

public boolean isWildImport()
{
   return getSingle().getGenerator().isWildImport();
}


/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuilder buffer = new StringBuilder();
   buffer.append("Found { (is_used:");
   buffer.append(is_used);
   buffer.append(") [");

   for (GenAndTok g : found_item) {
      buffer.append(g);
      buffer.append("  ");
   }
   buffer.append(" ]}");
   return buffer.toString();
}


} // end of class Found


/* end of Found.java */
