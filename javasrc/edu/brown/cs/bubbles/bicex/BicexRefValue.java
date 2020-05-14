/********************************************************************************/
/*                                                                              */
/*              BicexRefValue.java                                              */
/*                                                                              */
/*      Set of time-dependent values                                            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bicex;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


class BicexRefValue extends BicexValue implements BicexConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SortedMap<Long,BicexBaseValue>  value_map;
private long                            last_update;
private BicexBaseValue                  last_value;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BicexRefValue(BicexBaseValue base)
{
   this(0,base);
}


BicexRefValue(long when,BicexBaseValue base)
{
   value_map = null;
   last_update = -1;
   last_value = null;
   
   if (base != null) setValueAt(when,base);
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override boolean hasChildren(long when)
{
   BicexBaseValue base = getValueAt(when);
   if (base == null) return false;
   return base.hasChildren(when);
}


@Override Map<String,BicexValue> getChildren(long when)
{
   BicexBaseValue base = getValueAt(when);
   if (base == null) return null;
   return base.getChildren(when);
}


@Override String getStringValue(long when)
{
   BicexBaseValue base = getValueAt(when);
   if (base == null) return null;
   return base.getStringValue(when);
}


@Override String getDataType(long when)
{
   BicexBaseValue base = getValueAt(when);
   if (base == null) return null;
   return base.getDataType(when);
}


@Override String getTooltipValue(long when)
{
   BicexBaseValue base = getValueAt(when);
   if (base == null) return null;
   return base.getTooltipValue(when);
}


@Override String getFullName()
{
   if (last_value == null) return null;
   
   return last_value.getFullName();
}


/********************************************************************************/
/*                                                                              */
/*      Methods to get/set value                                                */
/*                                                                              */
/********************************************************************************/

void setValueAt(long when,BicexBaseValue val) 
{
   if (val == null) return;
   
   if (last_update < 0 || (when == 0 && last_update == 0)) {
      last_update = when;
      last_value = val;
    }
   else {
      if (value_map == null) {
         value_map = new TreeMap<Long,BicexBaseValue>();
         value_map.put(last_update,last_value);
       }
      if (when >= last_update) {
         last_update = when;
         last_value = val;
       }
      value_map.put(when,val);
    }
}


BicexBaseValue getValueAt(long tv)
{
   if (last_update >= 0) {
      if (tv > last_update || tv == 0) return last_value;
    }
   
   if (value_map == null) return null;
   
   SortedMap<Long,BicexBaseValue> head = value_map.headMap(tv);
   if (head.isEmpty()) return null;
   
   return value_map.get(head.lastKey());
}


@Override List<Integer> getTimeChanges()
{
   List<Integer> rslt = new ArrayList<>();
   for (Long l : value_map.keySet()) {
      rslt.add(l.intValue());
    }
   return rslt;
}

@Override long getUpdateTime(long tv)
{
   if (tv <= 0) return -1;
   if (last_update >= 0) {
      if (tv >= last_update) return last_update;
    }
   if (value_map == null) return -1;
   SortedMap<Long,BicexBaseValue> head = value_map.headMap(tv+1);
   if (head.isEmpty()) return -1;
   return head.lastKey();
}

@Override boolean isInitializable()
{
   BicexValue bv = getValueAt(0);
   if (bv == null) return false;
   
   return bv.isInitializable();
}


@Override boolean isComponent(long when)
{
   BicexValue bv = getValueAt(when);
   if (bv == null) return false;
   return bv.isComponent(when);
}




}       // end of class BicexRefValue




/* end of BicexRefValue.java */

