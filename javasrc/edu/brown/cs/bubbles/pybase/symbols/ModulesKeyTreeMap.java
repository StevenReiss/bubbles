/********************************************************************************/
/*										*/
/*		ModulesKeyTreeMap.java						*/
/*										*/
/*	Python Bubbles Base map from keys to moudles				*/
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

package edu.brown.cs.bubbles.pybase.symbols;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * This class is basically a TreeMap, but with the getEntry() method made public!!!
 *
 * That's because we need a way to get the 'real' key from a 'fake' one (so, with the name
 * we're able to get the file and zip path) -- another option would be adding the key to the
 * value itself, but that should not be needed just because TreeMap does not have the interface
 * that we want.
 *
 * @author Fabio
 *
 * @param <K>
 * @param <V>
 */
public class ModulesKeyTreeMap<K, V> extends TreeMap<K, V> {


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ModulesKeyTreeMap()
{}

public ModulesKeyTreeMap(Comparator<? super K> c)
{
   super(c);
}

public ModulesKeyTreeMap(SortedMap<K, ? extends V> m)
{
   super(m);
}

public ModulesKeyTreeMap(Map<? extends K, ? extends V> m)
{
   super(m);
}


/********************************************************************************/
/*										*/
/*	Method to get actual etnry						*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked") public Map.Entry<K, V> getActualEntry(K key)
{
   Map.Entry<K, V> ent = floorEntry(key);
   if (ent == null) return null;

   Comparator<? super K> c = comparator();
   if (c == null) {
      Comparable<? super K> k = (Comparable<? super K>) key;
      if (k.compareTo(ent.getKey()) == 0) return ent;
   }
   else {
      if (c.compare(key, ent.getKey()) == 0) return ent;
   }

   return null;
}


} // end of class ModulesKeyTreeMap


/* end of ModulesKeyTreeMap.java */


