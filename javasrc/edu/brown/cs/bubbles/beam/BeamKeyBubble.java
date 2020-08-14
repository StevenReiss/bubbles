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
import java.util.Vector;


class BeamKeyBubble extends BudaBubble implements BeamConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static TableModel	table_model = null;

private static final long serialVersionUID = 1;



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
   BufferedReader br = null;
   
   String menu = "control";
   String xalt = "alt";
   int mask = SwingText.getMenuShortcutKeyMaskEx();
   if (mask == InputEvent.META_DOWN_MASK) {
      menu = "command";
      xalt = "control";   
    }

   try {
      br = new BufferedReader(new FileReader(kfilnm));

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
      table_model = mdl;
      br.close();
    }
   catch (IOException e) { }
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
