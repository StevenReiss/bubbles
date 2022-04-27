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

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.swing.SwingKey;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

private static boolean  use_swing = true;

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
   where_labels.put("SEARCHBAR","Text Editor Search Bars");
   where_labels.put("BVCR","Version Control Bubbles");
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

private synchronized static void setupTableModel()
{
   if (table_model != null) return;

   String kfilnm = BoardSetup.getSetup().getLibraryPath("keybindings.csv");
   
   String menu = "control";
   String xalt = "alt";
   int mask = SwingText.getMenuShortcutKeyMaskEx();
   if (mask == InputEvent.META_DOWN_MASK) {
      menu = "command";
      xalt = "control";   
    }

   if (use_swing) table_model = loadFromSwingKey(menu,xalt);
   else table_model = loadFromCSVFile(kfilnm,menu,xalt);
}



/********************************************************************************/
/*                                                                              */
/*      Load from SwingKey                                                      */
/*                                                                              */
/********************************************************************************/

private static TableModel loadFromSwingKey(String menu,String xalt)
{
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
            Vector<String> data = vector(where,k1,cmd);
            mdl.addRow(data);
          }
       }
    }
   
   return mdl;
}



private static Vector<String> vector(String ... data)
{
   Vector<String> rslt = new Vector<>();
   for (String s : data) rslt.add(s);
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Load from old key file                                                  */
/*                                                                              */
/********************************************************************************/

private static TableModel loadFromCSVFile(String fnm,String menu,String xalt)
{
   try (BufferedReader br = new BufferedReader(new FileReader(fnm)))  {
      DefaultTableModel mdl = null;
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null || ln.length() == 0) break;
	 Vector<String> strs = tokenize(ln);
         if (strs.size() != 3) continue;
         String s = strs.get(1);
         s = s.replace("menu",menu);
         s = s.replace("xalt",xalt);
         strs.set(1,s);
         
	 if (mdl == null) {
	    mdl = new DefaultTableModel(strs,0);
	    continue;
	  }
	 mdl.addRow(strs);
       }
      return mdl;
    }
   catch (IOException e) { }
   
   return null;
}




private static Vector<String> tokenize(String cmd)
{
   Vector<String> argv = new Vector<String>();

   char quote = 0;
   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < cmd.length(); ++i) {
      char c = cmd.charAt(i);
      if (quote != 0 && c == quote) {
	 quote = 0;
	 continue;
       }
      else if (quote == 0 && (c == '"' || c == '\'')) {
	 quote = c;
	 continue;
       }
      else if (quote == 0 && (c == ',' || c == '\n')) {
	 if (buf.length() > 0) {
	    argv.add(buf.toString());
	    buf = new StringBuffer();
	  }
       }
      else buf.append(c);
    }
   if (buf.length() > 0) {
      argv.add(buf.toString());
    }

   return argv;
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
