/********************************************************************************/
/*										*/
/*		BfixChoreBubble.java						*/
/*										*/
/*	Bubble to display the list of minion chore      			*/
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.buda.BudaBubble;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;


class BfixChoreBubble extends BudaBubble implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BfixChoreManager 	chore_manager;
private TableButton		chore_buttons;
private ChoreTable		chore_table;
private ChoreList		chore_list;
private ChoreUpdater		chore_updater;


static private Icon yesicon = BoardImage.getIcon("accept",12,12);
static private Icon noicon = BoardImage.getIcon("no",12,12);

private static final long serialVersionUID = 0;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixChoreBubble(BfixChoreManager mgr)
{
   chore_manager = mgr;

   chore_table = new ChoreTable();
   chore_buttons = new TableButton();
   chore_list = new ChoreList();
   chore_updater = new ChoreUpdater();

   chore_manager.addListDataListener(chore_updater);

   JScrollPane jsp = new JScrollPane(chore_list);
   jsp.setPreferredSize(new Dimension(400,100));
   setContentPane(jsp);
}



@Override protected void localDispose()
{
   chore_manager.removeListDataListener(chore_updater);
}


/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();
   
   menu.add(getFloatBubbleAction());
   
   menu.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Chore List class 							*/
/*										*/
/********************************************************************************/

private class ChoreList extends JTable {

   private static final long serialVersionUID = 1;

   ChoreList() {
      super(chore_table);
      setShowVerticalLines(false);
      setTableHeader(null);
      TableColumn actioncol = getColumnModel().getColumn(0);
      TableColumn removecol = getColumnModel().getColumn(2);
      actioncol.setCellRenderer(chore_buttons);
      actioncol.setCellEditor(chore_buttons);
      actioncol.setMaxWidth(16);
      removecol.setCellRenderer(chore_buttons);
      removecol.setCellEditor(chore_buttons);
      removecol.setMaxWidth(16);
    }

}	// end of inner class ChoreList




/********************************************************************************/
/*										*/
/*	Table Model								*/
/*										*/
/********************************************************************************/

private class ChoreTable extends AbstractTableModel {

   private static final long serialVersionUID = 0;

   ChoreTable() {
    }

   @Override public int getColumnCount()		{ return 3; }

   @Override public int getRowCount() {
      return chore_manager.getSize();
    }

   @Override public Object getValueAt(int row,int col) {
      if (col == 0) return yesicon;
      else if (col == 1) return chore_manager.getElementAt(row);
      else return noicon;
    }

   @Override public boolean isCellEditable(int row,int col) {
      return (col == 0 || col == 2);
    }

}	// end of inner class ChoreTable



/********************************************************************************/
/*										*/
/*	Move changes to chore model to table model				*/
/*										*/
/********************************************************************************/

private class ChoreUpdater implements ListDataListener {


   @Override public void contentsChanged(ListDataEvent e) {
      int row0 = e.getIndex0();
      int row1 = e.getIndex1();
      chore_table.fireTableRowsUpdated(row0,row1);
    }

   @Override public void intervalAdded(ListDataEvent e) {
      int row0 = e.getIndex0();
      int row1 = e.getIndex1();
      int delta = row1 - row0;
      int sz = chore_manager.getSize();
      for (int i = sz-1; i >= row1; --i) {
	 chore_buttons.moveRow(i-delta,i);
       }
      chore_table.fireTableRowsInserted(row0,row1-1);
    }

   @Override public void intervalRemoved(ListDataEvent e) {
      int row0 = e.getIndex0();
      int row1 = e.getIndex1();
      int delta = row1 - row0 + 1;
      int sz = chore_manager.getSize();
      for (int i = row0; i <= row1; ++i) {
	 chore_buttons.removeRow(i);
       }
      for (int i = row1+1; i < sz+delta; ++i) {
	 chore_buttons.moveRow(i,i-delta);
       }
      chore_table.fireTableRowsDeleted(row0,row1);
    }

}	// end of inner class ChoreUpdater




/********************************************************************************/
/*										*/
/*	Editable Button interface						*/
/*										*/
/********************************************************************************/

private class TableButton extends AbstractCellEditor
	implements TableCellEditor, TableCellRenderer
{
   private static final long serialVersionUID = 5647725208335645741L;

   private Map<Integer,JButton> action_map;
   private Map<Integer,JButton> delete_map;

   TableButton() {
      action_map = new HashMap<Integer,JButton>();
      delete_map = new HashMap<Integer,JButton>();
    }

   void removeRow(int row) {
      action_map.remove(row);
      delete_map.remove(row);
    }

   void moveRow(int oldrow, int newrow) {
      JButton button = action_map.remove(oldrow);
      if (button != null) action_map.put(newrow,button);
      button = delete_map.remove(oldrow);
      if (button != null) delete_map.put(newrow, button);
    }

   @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, final int row, final int column) {
      return getButton(row,column);
    }

   @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
      return getButton(row,column);
    }

   private JButton getButton(int row,int col) {
      Map<Integer,JButton> map = (col == 0 ? action_map : delete_map);
      JButton btn = map.get(row);
      if (btn == null) {
	 btn = new JButton((col == 0 ? yesicon : noicon));
	 btn.setMargin(new Insets(2,2,2,2));
	 btn.addActionListener(new ButtonAction(row,col));
	 map.put(row,btn);
       }
      return btn;
    }

   @Override public Object getCellEditorValue() {
      return null;
    }

}       // end of inner class TableButton



private class ButtonAction implements ActionListener {

   private int row_number;
   private int col_number;
   private BfixChore for_chore;

   ButtonAction(int r,int c) {
      row_number = r;
      col_number = c;
      for_chore = chore_manager.getElementAt(row_number);
    }

   @Override public void actionPerformed(ActionEvent e) {
      JButton btn = (JButton) e.getSource();
      if (col_number == 0) {
	 for_chore.execute();
	 chore_manager.removeChore(for_chore);
	 chore_list.setEditingRow(-1);
	 chore_list.setCellEditor(null);
       }
      else if (col_number == 2) {
	 chore_manager.removeChore(for_chore);
	 chore_list.setEditingRow(-1);
	 chore_list.setCellEditor(null);
       }
      Container c = btn.getParent();
      if (c != null) c.remove(btn);
    }

}	// end of inner class ButtonAction





}	// end of class BfixChoreBubble




/* end of BfixChoreBubble.java */

