/********************************************************************************/
/*										*/
/*		BeamKeyBubble.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items key bindings bubble	*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.swing.SwingKey;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


class BeamKeyBubble extends BudaBubble implements BeamConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static TableModel	table_model = null;

private static Map<String,String> where_labels;

private static final long serialVersionUID = 1;

static {
   where_labels = new HashMap<>();
   where_labels.put("ROOT","General");
   where_labels.put("CODEEDIT","Code Editors");
   where_labels.put("PYTHONEDIT","Python Code Editors");
   where_labels.put("NOTE","Note Bubbles");
   where_labels.put("BUSS","Bubble Stacks");
   where_labels.put("DEBUG","Debug Area");
   where_labels.put("Debug Interaction","Read-Eval-Print Bubble");
   where_labels.put("SEARCHBOX","Search Bubbles");
   where_labels.put("TEXTEDIT","Text Editors");
   where_labels.put("SEARCHBAR","Text Search Bars");
   where_labels.put("BVCR","Version Bubbles");
}


/********************************************************************************/
/*										*/
/*	Constructor								*/
/*										*/
/********************************************************************************/

BeamKeyBubble()
{
   super(null,BudaBorder.RECTANGLE);

   setupTableModel();

   KeyTable tbl = new KeyTable();

   setContentPane(new JScrollPane(tbl),null);
}




/********************************************************************************/
/*										*/
/*	Methods to setup the table model					*/
/*										*/
/********************************************************************************/

private static synchronized void setupTableModel()
{
   if (table_model != null) return;

   String menu = "control";
   String xalt = "alt";
   int mask = SwingText.getMenuShortcutKeyMaskEx();
   if (mask == InputEvent.META_DOWN_MASK) {
      menu = "command";
      xalt = "control";   
    }

   table_model = loadFromSwingKey(menu,xalt);
}



/********************************************************************************/
/*                                                                              */
/*      Load from SwingKey                                                      */
/*                                                                              */
/********************************************************************************/

private static TableModel loadFromSwingKey(String menu,String xalt)
{
   String yalt = "alt";
   if (xalt.equals("alt")) yalt = "meta";
   Vector<String> hdrs = vector("Where","Key","Command");
   DefaultTableModel mdl = new DefaultTableModel(hdrs,0);
   Map<String,Map<String,Set<String>>> keys = SwingKey.getKeyMappings();
   for (String where : keys.keySet()) {
      Map<String,Set<String>> ckeys = keys.get(where);
      String w1 = where_labels.get(where);
      if (w1 != null) where = w1;
      for (String cmd : ckeys.keySet()) {
         Set<String> kkeys = ckeys.get(cmd);
         for (String key : kkeys) {
            String k1 = key.replace("menu",menu);
            k1 = k1.replace("xalt",xalt);
            k1 = k1.replace("ctrl","control");
            k1 = k1.replace("yalt",yalt);
            Vector<String> data = vector(where,k1,cmd);
            mdl.addRow(data);
          }
       }
    }
   
   return mdl;
}



private static Vector<String> vector(String... data)
{
   Vector<String> rslt = new Vector<>();
   for (String s : data) rslt.add(s);
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Table									*/
/*										*/
/********************************************************************************/

private static class KeyTable extends JTable {

   private static final long serialVersionUID = 1;


   KeyTable() {
      super(table_model);
      setDragEnabled(false);
      setPreferredScrollableViewportSize(new Dimension(400,300));
      setShowGrid(true);
      setAutoCreateRowSorter(true);
    }

}	// end of inner class KeyTable



}	// end of class BeamKeyBubble




/* end of BeamKeyBubble.java */
