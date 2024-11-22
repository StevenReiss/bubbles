/********************************************************************************/
/*										*/
/*		BassBubble.java 						*/
/*										*/
/*	Bubble Augmented Search Strategies bubble				*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;


class BassBubble extends BudaBubble implements BassConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;
private transient BassTreeModel my_tree_model;
private BassSearchBox my_search_box;


private static Stroke overview_stroke = new BasicStroke(1f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_BEVEL);



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassBubble(BassRepository br,String proj,String pfx,boolean trans)
{
   BassSearchBox.setDefault(proj,pfx);

   BassTreeModel tm = BassTreeModelVirtual.create(br,proj,pfx);

   BassSearchBox sb = new BassSearchBox(tm,trans);

   Dimension d = sb.getPreferredSize();
   sb.setSize(d);

   setTransient(trans);
   if (!trans) sb.setStatic(true);

   setContentPane(sb,sb.getEditor());
   my_tree_model = tm;
   my_search_box = sb;
}




/********************************************************************************/
/*										*/
/*     Action methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   // pass e along to search box
   if (my_search_box.handlePopupMenu(e)) return;

   JPopupMenu menu = new JPopupMenu();

   if (isTransient()) {
      menu.add(new PermanentAction());
   }
   else {
      menu.add(getFloatBubbleAction());
   }

   menu.show(this,e.getX(),e.getY());
}



void resetTreeModel(BassRepository br)
{
   my_tree_model.rebuild(br);
}



@Override public void paintOverview(Graphics2D g)
{ 
   //to keep package explorer from appearing in overview
   if (BASS_PROPERTIES.getBoolean(BASS_PACK_IN_OVERVIEW)) super.paintOverview(g); 
   else {
      Shape s0 = getShape();
      g.setColor(getBorderColor());

      g.setStroke(overview_stroke);
      g.draw(s0);
    }
}



private class PermanentAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   PermanentAction() {
      super("Keep Search Box");
      putValue(SHORT_DESCRIPTION,"Keep this search box around after selecting items");
   }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaRoot br = BudaRoot.findBudaRoot(my_search_box);
      br.noteSearchUsed(my_search_box);
      setTransient(false);
      my_search_box.setStatic(true);
   }

}	// end of inner class PermanentAction

}	// end of class BassBubble





/* end of BassBubble.java */



